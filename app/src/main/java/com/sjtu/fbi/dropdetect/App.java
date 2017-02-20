package com.sjtu.fbi.dropdetect;

import android.app.Application;
import android.content.Context;

/**
 * Author: FBi.
 * Email: bofu1993@163.com.
 * Date: 8/29/16
 */
public class App extends Application {

  private static Context context;

  @Override
  public void onCreate() {
    super.onCreate();
    context = getApplicationContext();
  }

  public static Context getContext() {
    return context;
  }
}
