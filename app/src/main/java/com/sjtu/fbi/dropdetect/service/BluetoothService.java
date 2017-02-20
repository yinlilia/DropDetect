package com.sjtu.fbi.dropdetect.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by sjtu on 2015/12/21.
 */
public class BluetoothService extends Service {

  private BluetoothAdapter bluetoothAdapter;
  private BluetoothManager bluetoothManager;
  private static final UUID MY_UUID_INSECURE =
      UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

  @Override
  public void onCreate() {
    super.onCreate();
    initBluetooth();
  }

  private void initBluetooth() {
    bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
    bluetoothAdapter = bluetoothManager.getAdapter();
    if (!bluetoothAdapter.isEnabled()) {
      bluetoothAdapter.enable();
    }
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {

    switch (intent.getAction()) {
      case "connect":
        final String macAddress = intent.getStringExtra("macAddress");
        new Thread(new Runnable() {
          @Override
          public void run() {
            connect(macAddress);
          }
        }).start();
        break;
    }

    return super.onStartCommand(intent, flags, startId);
  }

  private void connect(String address) {
    BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
    BluetoothSocket bluetoothSocket = null;
    if (bluetoothDevice != null) {
      try {
        bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord (MY_UUID_INSECURE);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    if (bluetoothSocket != null) {
      try {
        bluetoothSocket.connect();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private void sendBroadcast(String action) {
    Intent intent = new Intent();
    intent.setAction(action);
    sendBroadcast(intent);
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }
}
