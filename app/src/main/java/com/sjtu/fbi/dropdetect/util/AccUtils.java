package com.sjtu.fbi.dropdetect.util;

/**
 * Author: FBi.
 * Email: bofu1993@163.com.
 * Date: 9/10/16
 */
public class AccUtils {

  public static float computeAllAcc(double ax, double ay, double az) {
    return (float) Math.sqrt(Math.pow(ax, 2) + Math.pow(ay, 2) + Math.pow(az, 2));
  }

  public static float computeAllAcc(float ax, float ay, float az) {
    return (float) Math.sqrt(Math.pow(ax, 2) + Math.pow(ay, 2) + Math.pow(az, 2));
  }

  public static float computeAllAccUniform(double ax, double ay, double az) {
    return (float) ((computeAllAcc(ax, ay, az) - 13.265) / 13.265);
  }

  public static float computeAllAccUniform(float ax, float ay, float az) {
    return (float) ((computeAllAcc(ax, ay, az) - 13.265) / 13.265);
  }
}
