package com.sjtu.fbi.dropdetect.service;

import android.bluetooth.BluetoothProfile;
import android.content.Context;

import java.util.UUID;

/**
 * Created by bo on 2016/1/18.
 */
public class CommunicationService {

  public static final UUID CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
  public static final int BLE_TYPE_8852 = 1;
  public static final int BLE_TYPE_2541 = 2;
  private static final String UUID_SERVICE_2541 = "0000ffe0-0000-1000-8000-00805f9b34fb";
  private static final String UUID_CHARACTERISTIC_2541 = "0000ffe1-0000-1000-8000-00805f9b34fb";
  private static final String UUID_CHARACTERISTIC_WRITE_2541 =
      "0000ffe1-0000-1000-8000-00805f9b34fb";
  private static final String UUID_SERVICE_8852 = "00001234-0000-1000-8000-00805f9b34fb";
  private static final String UUID_CHARACTERISTIC_8852 = "00001236-0000-1000-8000-00805f9b34fb";
  private static final String UUID_CHARACTERISTIC_WRITE_8852 =
      "00001235-0000-1000-8000-00805f9b34fb";
  public static String uuidCharacteristicWrite;
  public static String uuidServiceDefault;
  public static String uuidCharacteristicDefault;
  private static int bleType = BLE_TYPE_8852;
  private static int TYPE_BLE = 1;
  private static int TYPE_BLUETOOTH = 2;
  private static int communicationType = TYPE_BLE;

  static {
    switch (bleType) {
      case BLE_TYPE_2541:
        initBleType2541();
        break;
      case BLE_TYPE_8852:
        initBleType8852();
        break;
      default:
        break;
    }
  }

  public static int getBleType() {
    return bleType;
  }

  private static void initBleType2541() {
    uuidServiceDefault = UUID_SERVICE_2541;
    uuidCharacteristicDefault = UUID_CHARACTERISTIC_2541;
    uuidCharacteristicWrite = UUID_CHARACTERISTIC_WRITE_2541;
  }

  private static void initBleType8852() {
    uuidServiceDefault = UUID_SERVICE_8852;
    uuidCharacteristicDefault = UUID_CHARACTERISTIC_8852;
    uuidCharacteristicWrite = UUID_CHARACTERISTIC_WRITE_8852;
  }


  public static void initBluetooth(Context context) {
    if (communicationType == TYPE_BLE) {
      if (BluetoothProfile.STATE_DISCONNECTED == BleService.BlePublicAction.getBleState()) {
        BleService.BlePublicAction.bleServiceConnectWithMaxRssi(context,
            uuidServiceDefault, uuidCharacteristicDefault);
      }
    }
  }
}
