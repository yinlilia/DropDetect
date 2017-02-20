package com.sjtu.fbi.dropdetect.service;

import android.annotation.TargetApi;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.sjtu.fbi.dropdetect.detectoutside.DataParser;
import com.sjtu.fbi.dropdetect.entity.CustomBleDevice;
import com.sjtu.fbi.dropdetect.util.BaseLibraryGlobal;
import com.sjtu.fbi.dropdetect.util.ByteUtils;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public class BleService extends Service {

  // 设备不支持蓝牙
  public static final String ACTION_DEVICE_NOT_SUPPORTED = BaseLibraryGlobal.PACKAGE_NAME
      + ".DEVICE_NOT_SUPPORTED";
  public final static String ACTION_GATT_CONNECTED = BaseLibraryGlobal.PACKAGE_NAME
      + ".ACTION_GATT_CONNECTED";
  public final static String ACTION_GATT_DISCONNECTED = BaseLibraryGlobal.PACKAGE_NAME
      + ".ACTION_GATT_DISCONNECTED";
  public static final String BLE_GATT_RECEIVE_DATA = BaseLibraryGlobal.PACKAGE_NAME
      + ".BLE_GATT_RECEIVE_DATA";
  private static final String ACTION_CONNECT_COMMAND = BaseLibraryGlobal.PACKAGE_NAME
      + ".BLE_ACTION_CONNECT_COMMAND";
  private static final String ACTION_SEARCH_COMMAND = BaseLibraryGlobal.PACKAGE_NAME
      + ".BLE_ACTION_SEARCH_COMMAND";
  private static final long MAX_SCANNING_INTERVAL = 4000;
  private static final String SERVICE_UUID = BaseLibraryGlobal.PACKAGE_NAME
      + ".SERVICE_UUID";
  private static final String CHARACTERISTIC_UUID = BaseLibraryGlobal.PACKAGE_NAME
      + ".CHARACTERISTIC_UUID";
  private static final String BLE_GATT_SEND_DATA = BaseLibraryGlobal.PACKAGE_NAME
      + ".BLE_GATT_SEND_DATA";
  private static final String ACTION_GATT_SEND_DATA = BaseLibraryGlobal.PACKAGE_NAME
      + ".ACTION_GATT_SEND_DATA";
  private static final int MAX_SEND_SIZE = 20;
  private static int connectionState = BluetoothProfile.STATE_DISCONNECTED;
  private static int sendNumber = 0;
  private static int receiveNumber = 0;
  private BluetoothManager mBluetoothManager = null;
  private BluetoothAdapter mBluetoothAdapter = null;
  private boolean isScanning = false;
  private Handler mHandler = new Handler();
  private LeScanCallback mLeScanCallbackKitKat = null;
  private ScanCallback mScanCallbackLollipop = null;
  // 搜索到的ble设备列表
  private Map<String, CustomBleDevice> customBleDeviceMap = new HashMap<>();
  private BluetoothGatt mBluetoothGatt = null;
  Runnable stopScanAndStartConnectTask = new Runnable() {

    @Override
    public void run() {
      // 如果搜索到合适的ble设备，停止搜索，开始连接？
      if (isScanning) {
        stopBleScan();
        bleConnectWithMaxRssi();
      }
    }
  };
  private BluetoothGattCharacteristic mBluetoothGattCharacteristic = null;
  private BluetoothGattCharacteristic writeGattCharacteristic = null;
  private String serviceUUID = null;
  private String characteristicUUID = null;
  private List<byte[]> dataSendBuffer = new ArrayList<>();
  private List<byte[]> dataAlreadySend = new ArrayList<>();
  private boolean isReadySendData = false;
  private final BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {
    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                        int newState) {
      // TODO Auto-generated method stub
      if (newState == BluetoothProfile.STATE_CONNECTED) {
        mBluetoothGatt = gatt;
        gatt.discoverServices();
        connectionState = BluetoothProfile.STATE_CONNECTED;
      } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
        sendBleStateBroadCast(ACTION_GATT_DISCONNECTED);
        onClearGattConnection();
        connectionState = BluetoothProfile.STATE_DISCONNECTED;
      }
      super.onConnectionStateChange(gatt, status, newState);
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
      // TODO Auto-generated method stub
      List<BluetoothGattService> bleServices = gatt.getServices();
      Log.d("service size", "" + bleServices.size());
      boolean isConnected = false;
      for (BluetoothGattService service : bleServices) {
        Log.d("service uuid", "" + service.getUuid());
        Log.d("character size", "" + service.getCharacteristics().size());
        if (StringUtils.equalsIgnoreCase(service.getUuid().toString(),
            serviceUUID)) {
          List<BluetoothGattCharacteristic> bleCharacteristics = service
              .getCharacteristics();
          for (BluetoothGattCharacteristic characteristic : bleCharacteristics) {
            Log.d("character uuid", characteristic.getUuid() + "");
            if (StringUtils.equalsIgnoreCase(characteristic
                .getUuid().toString(), characteristicUUID)) {
              // 找到指定的charateristic
              mBluetoothGattCharacteristic = characteristic;
              mBluetoothGatt.setCharacteristicNotification(
                  mBluetoothGattCharacteristic, true);
              if (CommunicationService.getBleType() == CommunicationService.BLE_TYPE_8852) {
                BluetoothGattDescriptor bluetoothGattDescriptor = mBluetoothGattCharacteristic
                    .getDescriptor(CommunicationService.CCCD);
                bluetoothGattDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                mBluetoothGatt.writeDescriptor(bluetoothGattDescriptor);
              }
              writeGattCharacteristic = mBluetoothGatt.getService(UUID
                  .fromString(CommunicationService.uuidServiceDefault)).getCharacteristic(UUID
                  .fromString(CommunicationService.uuidCharacteristicWrite));
              sendBleStateBroadCast(ACTION_GATT_CONNECTED);
              isConnected = true;
              isReadySendData = true;
              connectionState = BluetoothProfile.STATE_CONNECTED;
              sendData();
            }
          }
        }
      }
      // 重新选择设备进行连接操作
      if (!isConnected) {
                /*incorrectAddressSet.add(gatt.getDevice().getAddress());
                if (incorrectAddressSet.size() > MAX_INCORRECT_DEVICE) {
					bleSearch();
				} else {
					customBleDeviceMap.remove(gatt.getDevice().getAddress());
					bleConnectWithMaxRssi();
				}*/
        customBleDeviceMap.remove(gatt.getDevice().getAddress());
        bleConnectWithMaxRssi();
      }
      super.onServicesDiscovered(gatt, status);
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt,
                                        BluetoothGattCharacteristic characteristic) {
      // TODO Auto-generated method stub
      //接收到数据
      byte[] data = characteristic.getValue();
      receiveNumber++;
      /*Log.d("receive-data " + receiveNumber, ByteUtils.bytesToString(data) + "        " + System
          .currentTimeMillis());*/
      onReceiveData(data, data.length, gatt.getDevice().getAddress());
      super.onCharacteristicChanged(gatt, characteristic);
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt,
                                      BluetoothGattCharacteristic characteristic, int status) {
      // TODO Auto-generated method stub
      int a=1;
      if (StringUtils.equalsIgnoreCase(writeGattCharacteristic
          .getUuid().toString(), characteristic.getUuid().toString())
          && BluetoothGatt.GATT_SUCCESS == status) {
        isReadySendData = true;
        sendData();
      }
      super.onCharacteristicWrite(gatt, characteristic, status);
    }
  };

  public static int getConnectionState() {
    return connectionState;
  }

  private void onReceiveData(byte[] data, int dataLength, String from) {
    DataParser.onGetData(data, dataLength);

  }


  private void onClearGattConnection() {
    mBluetoothGattCharacteristic = null;
    writeGattCharacteristic = null;
    writeGattCharacteristic = null;
    mBluetoothGatt = null;
    isReadySendData = false;
  }

  private void sendBleStateBroadCast(String action) {
    Intent intent = new Intent(action);
    sendBroadcast(intent);
  }

  /**
   * 检测设备蓝牙是否正常并且是否支持ble
   *
   * @return
   */
  private boolean bleInitialize() {
    if (mBluetoothManager == null) {
      mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
      if (mBluetoothManager == null) {
        return false;
      }
    }
    mBluetoothAdapter = mBluetoothManager.getAdapter();
    if (mBluetoothAdapter == null) {
      return false;
    }
    // 检测是否支持ble
    if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
      return false;
    }
    // 如果蓝牙没有开启，强制开启蓝牙
    if (!mBluetoothAdapter.isEnabled()) {
      mBluetoothAdapter.enable();
    }
    return true;
  }

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  @SuppressWarnings("deprecation")
  private void bleSearch() {
    if (isScanning) {
      return;
    }
    if (!bleInitialize()) {
      Intent intent = new Intent(ACTION_DEVICE_NOT_SUPPORTED);
      sendBroadcast(intent);
      return;
    }
    // 开始搜索设备
    customBleDeviceMap.clear();
    isScanning = true;
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
      mBluetoothAdapter.startLeScan(mLeScanCallbackKitKat);
    } else {
      mBluetoothAdapter.getBluetoothLeScanner().startScan(
          mScanCallbackLollipop);
    }
    mHandler.postDelayed(stopScanAndStartConnectTask, MAX_SCANNING_INTERVAL);
  }

  private void handleSearchDevice(BluetoothDevice device, int rssi,
                                  byte[] scanRecord) {
    // TODO Auto-generated method stub
    if ("WT-0001".equals(device.getName())) {
      customBleDeviceMap.put(device.getAddress(), new CustomBleDevice(device,
          rssi, scanRecord));
    }
  }

  private void bleStartConnect() {
    bleSearch();
  }

  /**
   * 选择信号最强的进行连接，当信号达到哪个程度可以进行连接？
   */
  private void bleConnectWithMaxRssi() {
    CustomBleDevice customBleDeviceWithMaxRssi = getCustomBleDeviceWithMaxRssi();
    // 如果没有找到最大信号的设备，重新搜索
    if (customBleDeviceWithMaxRssi == null) {
      bleSearch();
      return;
    }
    mBluetoothGatt = customBleDeviceWithMaxRssi.getBluetoothDevice()
        .connectGatt(this, true, mBluetoothGattCallback);
  }

  private CustomBleDevice getCustomBleDeviceWithMaxRssi() {
    CustomBleDevice customBleDevice = null;
    Iterator<Map.Entry<String, CustomBleDevice>> iterator = customBleDeviceMap
        .entrySet().iterator();
    if (iterator.hasNext()) {
      customBleDevice = iterator.next().getValue();
    }
    CustomBleDevice tempCustomBleDevice = null;
    while (iterator.hasNext()) {
      tempCustomBleDevice = iterator.next().getValue();
      if (tempCustomBleDevice.getRssi() > customBleDevice.getRssi()) {
        customBleDevice = tempCustomBleDevice;
      }
    }
    return customBleDevice;
  }

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  @SuppressWarnings("deprecation")
  private void stopBleScan() {
    // TODO Auto-generated method stub
    if (mBluetoothAdapter != null) {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
        mBluetoothAdapter.stopLeScan(mLeScanCallbackKitKat);
      } else {
        mBluetoothAdapter.getBluetoothLeScanner().stopScan(
            mScanCallbackLollipop);
      }
    }
    isScanning = false;
  }

  @Override
  public IBinder onBind(Intent intent) {
    // TODO Auto-generated method stub
    return null;
  }

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  @Override
  public void onCreate() {
    // TODO Auto-generated method stub
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
      mLeScanCallbackKitKat = new LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi,
                             byte[] scanRecord) {
          // TODO Auto-generated method stub
          handleSearchDevice(device, rssi, scanRecord);
        }
      };
    } else {
      mScanCallbackLollipop = new ScanCallback() {

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
          // TODO Auto-generated method stub
          handleSearchDevice(result.getDevice(), result.getRssi(), result.getScanRecord()
              .getBytes());
          super.onScanResult(callbackType, result);
        }
      };
    }
    super.onCreate();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    // TODO Auto-generated method stub
    String action = null;
    if (intent != null) {
      action = intent.getAction();
    }
    if (!StringUtils.isEmpty(action)) {
      switch (action) {
        case ACTION_CONNECT_COMMAND:
          this.serviceUUID = intent.getStringExtra(SERVICE_UUID);
          this.characteristicUUID = intent
              .getStringExtra(CHARACTERISTIC_UUID);
          bleStartConnect();
          break;
        case ACTION_SEARCH_COMMAND:
          bleSearch();
          break;
        case ACTION_GATT_SEND_DATA:
          if (intent.getByteArrayExtra(BLE_GATT_SEND_DATA) != null
              && intent.getByteArrayExtra(BLE_GATT_SEND_DATA).length > 0) {
            addSendDataToBuffer(intent.getByteArrayExtra(BLE_GATT_SEND_DATA));
          }
          break;
        default:
          break;
      }
    }
    return super.onStartCommand(intent, flags, startId);
  }

  private void addSendDataToBuffer(byte[] data) {
    // TODO Auto-generated method stub
    synchronized (dataSendBuffer) {
      int index = 0;
      while ((index + 1) * MAX_SEND_SIZE < data.length) {
        this.dataSendBuffer.add(Arrays.copyOfRange(data, index
            * MAX_SEND_SIZE, (index + 1) * MAX_SEND_SIZE));
        ++index;
      }
      this.dataSendBuffer.add(Arrays.copyOfRange(data, index
          * MAX_SEND_SIZE, data.length));
    }
    sendData();
    return;
  }


  private void sendData() {
    if (isReadySendData && connectionState == BluetoothProfile.STATE_CONNECTED) {
      synchronized (dataSendBuffer) {
        if (this.dataSendBuffer.size() > 0) {
          try {
            Thread.sleep(50);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          byte[] data = this.dataSendBuffer.get(0);
          this.dataAlreadySend.add(data);
          this.dataSendBuffer.remove(0);
          writeGattCharacteristic.setValue(data);
          Log.d("send-data " + sendNumber, ByteUtils.bytesToString(data) + "   " + System
              .currentTimeMillis());
          sendNumber++;
          if (mBluetoothGatt != null) {
            mBluetoothGatt.writeCharacteristic(writeGattCharacteristic);
          }
          this.isReadySendData = false;
        } else {
          this.isReadySendData = true;
        }
      }
    }
    return;
  }

  @Override
  public void onDestroy() {
    // TODO Auto-generated method stub
    if (mBluetoothGatt != null) {
      mBluetoothGatt.close();
    }
    super.onDestroy();
  }

  public static class BlePublicAction {
    public static void bleServiceConnectWithMaxRssi(Context context,
                                                    String serviceUUID, String characteristicUUID) {
      Intent intent = new Intent(context, BleService.class);
      intent.setAction(ACTION_CONNECT_COMMAND);
      intent.putExtra(SERVICE_UUID, serviceUUID);
      intent.putExtra(CHARACTERISTIC_UUID, characteristicUUID);
      context.startService(intent);
      return;
    }

    public static void bleSerivceSearch(Context context) {
      bleServiceAction(context, ACTION_SEARCH_COMMAND);
      return;
    }

    public static void bleSendData(Context context, byte[] data) {
      Intent intent = new Intent(context, BleService.class);
      intent.setAction(ACTION_GATT_SEND_DATA);
      intent.putExtra(BLE_GATT_SEND_DATA, data);
      context.startService(intent);
    }

    /**
     * 蓝牙命令操作的私有方法
     *
     * @param context
     * @param action
     */
    private static void bleServiceAction(Context context, String action) {
      Intent intent = new Intent(context, BleService.class);
      intent.setAction(action);
      context.startService(intent);
      return;
    }

    public static int getBleState() {
      return connectionState;
    }
  }
}
