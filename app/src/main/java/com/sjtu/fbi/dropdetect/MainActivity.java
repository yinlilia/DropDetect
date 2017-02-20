package com.sjtu.fbi.dropdetect;

import android.Manifest;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.sjtu.fbi.dropdetect.entity.AcceBean;
import com.sjtu.fbi.dropdetect.service.BleService;
import com.sjtu.fbi.dropdetect.service.BluetoothService;
import com.sjtu.fbi.dropdetect.service.CommunicationService;
import com.sjtu.fbi.dropdetect.util.AccUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

  private static final int CHECK_BLE_PERMISSION_CODE = 0;
  private static final int CHECK_LOCATION_PERMISSION_CODE = 1;
  @BindView(R.id.tv_ble_status) TextView bleStatus;
  @BindView(R.id.tv_scan) TextView scan;
  @BindView(R.id.chart_out) LineChart chartOut;
  @BindView(R.id.chart_in) LineChart chartIn;
  @BindView(R.id.status_still) TextView statusStill;
  @BindView(R.id.status_move) TextView statusMove;
  @BindView(R.id.status_drop) TextView statusDrop;
  @BindView(R.id.rb_out) RadioButton rbOut;
  @BindView(R.id.rb_in) RadioButton rbIn;
  @BindView(R.id.rb_group) RadioGroup radioGroup;
  private MyReceiver myReceiver;
  private List<Entry> entryList = new ArrayList<>();
  private List<Entry> innerEntryList = new ArrayList<>();
  private int outCounter = 0;
  private int innerCounter = 0;
  private SensorManager sensorManager;
  private Sensor sensor;
  private Algorithm innerAlgorithm = new Algorithm(Algorithm.TYPE_INNER);
  private Algorithm outAlgorithm = new Algorithm(Algorithm.TYPE_OUT);
  private static final int ENTRY_SIZE = 100;
  private Entry[] innerEntry = new Entry[ENTRY_SIZE];
  private Entry[] outEntry = new Entry[ENTRY_SIZE];

  private int currentInnerStatus = 0;
  private int currentOutStatus = 0;

  private static final int METHOD_INNER = 0;
  private static final int METHOD_OUT = 1;
  private int currentMethod = METHOD_INNER;

  private SoundPool sp;
  private HashMap<Integer, Integer> spMap = new HashMap<>();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    ButterKnife.bind(this);
    View view = new View(this);
    view.findViewById(R.id.chart_in);
    checkBlePermission();
    initView();
    initData();
    initEvent();
    myReceiver = new MyReceiver();
  }

  private void initData() {
    LineDataSet lineDataSet = new LineDataSet(entryList, "1");
    LineData lineData = new LineData(lineDataSet);
    chartOut.setData(lineData);

    LineDataSet innerDataSet = new LineDataSet(innerEntryList, "1");
    LineData lineData1 = new LineData(innerDataSet);
    chartIn.setData(lineData1);
    sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
    if (sensorManager != null) {
      sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }
    initSoundPool();
  }

  private void initView() {
    if (BleService.getConnectionState() == BluetoothProfile.STATE_CONNECTED) {
      bleStatus.setText(getString(R.string.connected));
      scan.setVisibility(View.GONE);
    } else {
      bleStatus.setText(getString(R.string.disconnected));
      scan.setVisibility(View.VISIBLE);
    }
  }

  private void initEvent() {
    scan.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        CommunicationService.initBluetooth(MainActivity.this);
      }
    });
    radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(RadioGroup radioGroup, int i) {
        switch (i) {
          case R.id.rb_out:
            currentMethod = METHOD_OUT;
            break;
          case R.id.rb_in:
            currentMethod = METHOD_INNER;
            break;
        }
      }
    });
  }

  private void clearStatus() {
    statusStill.setBackgroundResource(R.color.transparent);
    statusMove.setBackgroundResource(R.color.transparent);
    statusDrop.setBackgroundResource(R.color.transparent);
  }

  private void setStatus(int status) {
    clearStatus();
    switch (status) {
      case Algorithm.STATUS_STILL:
        statusStill.setBackgroundResource(R.color.green);
        break;
      case Algorithm.STATUS_MOVE:
        statusMove.setBackgroundResource(R.color.green);
        break;
      case Algorithm.STATUS_DROP:
        statusDrop.setBackgroundResource(R.color.red);
        playSound();
        break;
    }
  }

  private void playSound() {
    sp.play(spMap.get(1), 1, 1, 0, 0, 1);
  }

  private void initSoundPool() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      SoundPool.Builder spBuilder = new SoundPool.Builder();
      spBuilder.setMaxStreams(1);
      AudioAttributes.Builder attrBuilder = new AudioAttributes.Builder();
      attrBuilder.setLegacyStreamType(android.media.AudioManager.STREAM_MUSIC);
      spBuilder.setAudioAttributes(attrBuilder.build());
      sp = spBuilder.build();
    } else {
      sp = new SoundPool(1, android.media.AudioManager.STREAM_MUSIC, 5);
    }
    spMap.put(1, sp.load(this, R.raw.drop, 1));
  }


  private void checkBlePermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      if (checkSelfPermission(Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
        requestPermissions(new String[]{Manifest.permission.BLUETOOTH}, CHECK_BLE_PERMISSION_CODE);
      }
      if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager
          .PERMISSION_GRANTED) {
        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
            CHECK_LOCATION_PERMISSION_CODE);
      }
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction(BleService.ACTION_GATT_CONNECTED);
    intentFilter.addAction(BleService.ACTION_GATT_DISCONNECTED);
    intentFilter.addAction("bluetooth_connected");
    intentFilter.addAction("out_data");
    registerReceiver(myReceiver, intentFilter);
    sensorManager.registerListener(sensorEventListener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
  }

  private SensorEventListener sensorEventListener = new SensorEventListener() {
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
      if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
        float ax = sensorEvent.values[0];
        float ay = sensorEvent.values[1];
        float az = sensorEvent.values[2];
        if (innerEntry[ENTRY_SIZE - 1] != null) {
          chartIn.getLineData().removeEntry(innerEntry[0], 0);
          System.arraycopy(innerEntry, 1, innerEntry, 0, ENTRY_SIZE - 1);
          Entry entry = new Entry(innerCounter++, AccUtils.computeAllAcc(ax, ay, az));

          innerEntry[ENTRY_SIZE - 1] = entry;
          chartIn.getLineData().addEntry(innerEntry[ENTRY_SIZE - 1], 0);
          chartIn.notifyDataSetChanged();
          chartIn.moveViewToX(innerCounter);
        } else {
          Entry entry = new Entry(innerCounter++, AccUtils.computeAllAcc(ax, ay, az));
          innerEntry[innerCounter - 1] = entry;
          chartIn.getLineData().addEntry(entry, 0);
          chartIn.notifyDataSetChanged();
          chartIn.moveViewToX(innerCounter);
        }
        currentInnerStatus = innerAlgorithm.addData(AccUtils.computeAllAcc(ax, ay, az));
        if (currentMethod == METHOD_INNER) {
          setStatus(currentInnerStatus);
        }
      }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
  };

  @Override
  protected void onPause() {
    super.onPause();
    unregisterReceiver(myReceiver);
    sensorManager.unregisterListener(sensorEventListener);
  }

  class MyReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
      Toast.makeText(context, intent.getAction(), Toast.LENGTH_LONG).show();
      switch (intent.getAction()) {
        case BleService.ACTION_GATT_CONNECTED:
          bleStatus.setText(getString(R.string.connected));
          scan.setVisibility(View.GONE);
          break;
        case BleService.ACTION_GATT_DISCONNECTED:
          bleStatus.setText(getString(R.string.disconnected));
          scan.setVisibility(View.VISIBLE);
          break;
        case "out_data":
          AcceBean acceBean = (AcceBean) intent.getSerializableExtra("data");
          float aa = (float) Math.sqrt(Math.pow(acceBean.getAccX(), 2) + Math.pow(acceBean
              .getAccY(), 2) + Math.pow(acceBean.getAccZ(), 2));
          if (outEntry[ENTRY_SIZE - 1] != null) {
            chartOut.getLineData().removeEntry(outEntry[0], 0);
            System.arraycopy(outEntry, 1, outEntry, 0, ENTRY_SIZE - 1);
            Entry entry = new Entry(outCounter++, AccUtils.computeAllAcc(acceBean.getAccX(),
                acceBean.getAccY(), acceBean.getAccZ()));

            outEntry[ENTRY_SIZE - 1] = entry;
            chartOut.getLineData().addEntry(outEntry[ENTRY_SIZE - 1], 0);
            chartOut.notifyDataSetChanged();
            chartOut.moveViewToX(outCounter);
          } else {
            Entry entry = new Entry(outCounter++, AccUtils.computeAllAcc(acceBean.getAccX(),
                acceBean.getAccY(), acceBean.getAccZ()));
            outEntry[outCounter - 1] = entry;
            chartOut.getLineData().addEntry(entry, 0);
            chartOut.notifyDataSetChanged();
            chartOut.moveViewToX(outCounter);
          }
          currentOutStatus = outAlgorithm.addData(AccUtils.computeAllAcc(acceBean.getAccX(),
              acceBean.getAccY(), acceBean.getAccZ()));
          if (currentMethod == METHOD_OUT) {
            setStatus(currentOutStatus);
          }
          break;
      }
    }
  }

  @OnClick(R.id.tv_connect_bluetooth)
  public void connectBluetooth() {
    Intent intent = new Intent();
    intent.setClass(this, BluetoothService.class);
    intent.setAction("connect");
    intent.putExtra("macAddress", "A0:86:C6:F2:E3:35");
//    intent.putExtra("macAddress", "DB:EB:BD:E3:D8:EB");
    startService(intent);
  }
}
