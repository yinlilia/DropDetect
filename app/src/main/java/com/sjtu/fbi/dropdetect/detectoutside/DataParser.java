package com.sjtu.fbi.dropdetect.detectoutside;

import android.content.Intent;
import android.util.Log;

import com.sjtu.fbi.dropdetect.App;
import com.sjtu.fbi.dropdetect.entity.AcceBean;

/**
 * Author: FBi.
 * Email: bofu1993@163.com.
 * Date: 8/31/16
 */
public class DataParser {

  public static final double G = 9.8;

  public static void onGetData(byte[] data, int length) {
    if (data != null && data.length == 12 && data[0] == (byte) 0x69 && data[1] == (byte) 0xAB &&
        data[10] == (byte) 0xAC && data[11] == (byte) 0x71) {
      AcceBean acceBean = new AcceBean();
      acceBean.setAccX(((short) ((data[3] & 0xff) << 8 | (data[2] & 0xff))) / 32768.0 * 16 * G);
      acceBean.setAccY(((short) ((data[5] & 0xff) << 8 | (data[4] & 0xff))) / 32768.0 * 16 * G);
      acceBean.setAccZ(((short) ((data[7] & 0xff) << 8 | (data[6] & 0xff))) / 32768.0 * 16 * G -
          2 * G);
      Intent intent = new Intent();
      intent.setAction("out_data");
      intent.putExtra("data", acceBean);
      App.getContext().sendBroadcast(intent);
      double temper = ((short) ((data[9] & 0xff) << 8 | data[8] & 0xff)) / 340.0 + 36.25;
      Log.d("acc", acceBean.getAccX() + "     " + acceBean.getAccY() + "     " + acceBean
          .getAccZ() + "      " + temper);
    }
  }
}
