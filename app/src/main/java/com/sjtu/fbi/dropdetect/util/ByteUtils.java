package com.sjtu.fbi.dropdetect.util;

/**
 * author: bo on 2016/4/7 22:05.
 * email: bofu1993@163.com
 */
public class ByteUtils {

  // byte[]按字节置0
  public static void initByteArray(byte[] data, int len) {
    int size = len > data.length ? data.length : len;
    for (int ii = 0; ii < size; ++ii) {
      data[ii] = 0;
    }
    return;
  }

  public static void initByteArray(byte[] data, int offset, int len) {
    int size = len > data.length - offset ? data.length - offset : len;
    for (int i = 0; i < size; ++i) {
      data[i + offset] = 0;
    }
    return;
  }

  public static void copyAndAddZero(byte[] source, byte[] target, int offset) {
    System.arraycopy(source, 0, target, offset, source.length);
    initByteArray(target, offset + source.length, target.length - offset - source.length);
  }

  public static byte[] appendArray(byte[] target, byte[] resource) {
    byte[] temp = new byte[target.length + resource.length];
    System.arraycopy(target, 0, temp, 0, target.length);
    System.arraycopy(resource, 0, temp, target.length, resource.length);
    return temp;
  }

  // 双字转换到网络字节序
  public static void convertUnsignedInt16ToHost(int u, byte[] d, int offset) {
    d[offset] = (byte) ((u >> 8) & 0xFF);
    d[offset + 1] = (byte) (u & 0xFF);
    return;
  }

  // 网络字节序转换到双字
  public static int convertHostToUnsignedInt16(final byte[] d, int offset) {
    return ((((int) d[offset]) & 0xFF) << 8)
        | (((int) d[offset + 1]) & 0xFF);
  }

  public static int bytesToInt(byte[] bytes) {
    return bytes[0] << 24 | (bytes[1] & 0xff) << 16 | (bytes[2] & 0xff) << 8 | (bytes[3] & 0xff);
  }

  public static String bytesToString(byte[] data) {
    String value = "";
    for (int i = 0; i < data.length; i++) {
      value += data[i]+" ";
    }
    return value;
  }

}
