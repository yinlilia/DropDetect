package com.sjtu.fbi.dropdetect;

import java.util.Arrays;

/**
 * Author: FBi.
 * Email: bofu1993@163.com.
 * Date: 9/10/16
 */
public class Algorithm {

  public static final int STATUS_STILL = 0;
  public static final int STATUS_MOVE = 1;
  public static final int STATUS_DROP = 2;
  private static final int MIN_DROP_NUM = 1;

  private float[] dataArray = new float[10];
  private int status;

  private static final float STANDARD_DATA = 9.75f;
  private static final float UPPER_THRESHOLD = 14.0F;
  private static final float LOWER_THRESHOLD = 3.0F;

  private static final float MOVE_UPPER_THRESHOLD = 10.5F;
  private static final float MOVE_LOWER_THRESHOLD = 9.0F;


  public static final int TYPE_OUT = 1;
  public static final int TYPE_INNER = 0;
  private int type;

  public Algorithm() {
    for (int i = 0; i < dataArray.length; i++) {
      dataArray[i] = STANDARD_DATA;
    }
  }

  public Algorithm(int type) {
    this.type = type;
    for (int i = 0; i < dataArray.length; i++) {
      dataArray[i] = STANDARD_DATA;
    }
  }

  public int addData(float data) {
    System.arraycopy(dataArray, 0, dataArray, 1, dataArray.length - 1);
    dataArray[0] = data;
//    Log.d("data", "" + dataArray[0] + "--" + dataArray[1] + "--" + dataArray[2]);
    return analyze();
  }

  private int analyze() {

    if (isDropping()) {
      status = STATUS_DROP;
    } else {
      if (isMoving()) {
        status = STATUS_MOVE;
      } else {
        status = STATUS_STILL;
      }
    }
    return status;
  }

  private boolean isDropping() {
    if (type == TYPE_INNER) {
      return hasLowerThreshold(LOWER_THRESHOLD);
    } else {
      return hasLowerThresholdWithNum(LOWER_THRESHOLD, MIN_DROP_NUM);
    }
  }

  private boolean isMoving() {
    int low = 0;
    int high = 0;
    for (float data : dataArray) {
      if (data < MOVE_LOWER_THRESHOLD && data > MOVE_LOWER_THRESHOLD - 2) {
        low++;
      }
      if (data > MOVE_UPPER_THRESHOLD && data < MOVE_UPPER_THRESHOLD + 2) {
        high++;
      }
    }
    if (low >= 2 && high >= 2) {
      return true;
    } else {
      return false;
    }
  }

  private boolean hasUpperThreshold(float threshold) {
    for (float data : dataArray) {
      if (data >= threshold) {
        return true;
      }
    }
    return false;
  }

  private boolean hasLowerThreshold(float threshold) {
    for (float data : dataArray) {
      if (data <= threshold) {
        return true;
      }
    }
    return false;
  }

  private boolean hasLowerThresholdWithNum(float threshold, int num) {
    int count = 0;
    for (float data : Arrays.copyOf(dataArray, 5)) {
      if (data <= threshold) {
        count++;
      } else {
        count = 0;
      }
      if (count >= num) {
        return true;
      }
    }
    return count >= num;
  }

  public int getStatus() {
    return status;
  }
}
