package com.sjtu.fbi.dropdetect.entity;

import java.io.Serializable;

/**
 * Author: FBi.
 * Email: bofu1993@163.com.
 * Date: 8/31/16
 */
public class AcceBean implements Serializable{

  private double accX;
  private double accY;
  private double accZ;


  public double getAccX() {
    return accX;
  }

  public void setAccX(double accX) {
    this.accX = accX;
  }

  public double getAccY() {
    return accY;
  }

  public void setAccY(double accY) {
    this.accY = accY;
  }

  public double getAccZ() {
    return accZ;
  }

  public void setAccZ(double accZ) {
    this.accZ = accZ;
  }
}
