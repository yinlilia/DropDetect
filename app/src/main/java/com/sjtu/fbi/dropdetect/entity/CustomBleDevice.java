package com.sjtu.fbi.dropdetect.entity;

import android.bluetooth.BluetoothDevice;

public class CustomBleDevice {

  private BluetoothDevice bluetoothDevice;
  private int rssi;
  private byte[] scanRecord;

  public CustomBleDevice(BluetoothDevice bluetoothDevice, int rssi, byte[] scanRecord) {
    super();
    this.bluetoothDevice = bluetoothDevice;
    this.rssi = rssi;
    this.scanRecord = scanRecord;
  }

  public BluetoothDevice getBluetoothDevice() {
    return bluetoothDevice;
  }

  public int getRssi() {
    return rssi;
  }

  public void setRssi(int rssi) {
    this.rssi = rssi;
  }

  public byte[] getScanRecord() {
    return scanRecord;
  }

  public void setScanRecord(byte[] scanRecord) {
    this.scanRecord = scanRecord;
  }

  public boolean isSameBluetoothDevice(String macAddress) {
    return bluetoothDevice.getAddress().equalsIgnoreCase(macAddress);
  }

  public boolean isSameBluetoothDevice(BluetoothDevice bluetoothDevice) {
    return isSameBluetoothDevice(bluetoothDevice.getAddress());
  }
}
