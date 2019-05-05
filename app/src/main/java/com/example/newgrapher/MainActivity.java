package com.example.newgrapher;

import android.app.ActivityManager;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.Handler;
import android.os.PersistableBundle;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

import me.aflak.bluetooth.Bluetooth;
import me.aflak.bluetooth.BluetoothCallback;
import me.aflak.bluetooth.DeviceCallback;
import me.aflak.bluetooth.DiscoveryCallback;

public class MainActivity extends AppCompatActivity {

    private LineGraphSeries<DataPoint> mSeries;

    private final Handler mHandler = new Handler();

    List<BluetoothDevice> deviceListArray;

    ArrayList<String> messageList = new ArrayList<>();
    ArrayList<Long> timeList = new ArrayList<>();

    Runnable timer;

    Button deviceButton;
    Switch btSwitch;
    GraphView graph;
    Switch followGraphSwitch;
    ListView deviceListView;

    BluetoothDevice myDevice;

    Bluetooth bluetooth = new Bluetooth(MainActivity.this);

    String chosenDevice;
    Double mes= 0.0;
    Boolean followFlag = true;

    Boolean firstMessageFlag = true;

    ArrayAdapter<String> arrayAdapter;


    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /*
    private void getDataLists(){
        Intent intent = new Intent();
        intent.setAction("getDataList");
        LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(intent);
    }
    */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);


        try
        {
            this.getSupportActionBar().hide();
        }
        catch (NullPointerException e){}
        setContentView(R.layout.activity_main);

        LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(deviceListReceiver, new IntentFilter("BluetoothDevices"));
        LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(deviceConnectedReceiver, new IntentFilter("connectedDevice"));
        LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(messageReceiver, new IntentFilter("sendMessage"));
        LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(saveFileReceiver, new IntentFilter("fileToast"));


        deviceButton = findViewById(R.id.id_deviceListButton);
        btSwitch = findViewById(R.id.id_btSwitch);
        if (isMyServiceRunning(BluetoothService.class)){
            btSwitch.setChecked(true);
        }else{
            btSwitch.setChecked(false);

        }



        graph = findViewById(R.id.id_graphView);
        followGraphSwitch = findViewById(R.id.id_followDataSwitch);
        deviceListView = findViewById(R.id.id_deviceListView);

        deviceListView.setVisibility(View.INVISIBLE);

        followGraphSwitch.setChecked(true);

        deviceListArray = new ArrayList<>();




        deviceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (deviceListView.getVisibility() == View.INVISIBLE){
                    deviceListView.setVisibility(View.VISIBLE);
                }else{
                    deviceListView.setVisibility(View.INVISIBLE);
                }

            }
        });

        deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                chosenDevice = parent.getItemAtPosition(position).toString();
                deviceListView.setVisibility(View.INVISIBLE);
                Log.d("choose", "onItemClick: ");
                chooseDevice(chosenDevice);
                arrayAdapter.clear();



            }
        });


        followGraphSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked){
                    followFlag = true;
                }else{
                    followFlag = false;
                }

            }
        });

        btSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked){
                    LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(deviceListReceiver, new IntentFilter("BluetoothDevices"));
                    LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(deviceConnectedReceiver, new IntentFilter("connectedDevice"));
                    LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(messageReceiver, new IntentFilter("sendMessage"));
                    Intent serviceIntent = new Intent(MainActivity.this, BluetoothService.class);
                    startService(serviceIntent);
                    firstMessageFlag = true;

                }else{
                    Intent serviceIntent = new Intent(MainActivity.this, BluetoothService.class);
                    stopService(serviceIntent);

                    firstMessageFlag = true;
                    mHandler.removeCallbacks(timer);
                    LocalBroadcastManager.getInstance(MainActivity.this).unregisterReceiver(deviceListReceiver);
                    LocalBroadcastManager.getInstance(MainActivity.this).unregisterReceiver(deviceConnectedReceiver);
                    LocalBroadcastManager.getInstance(MainActivity.this).unregisterReceiver(messageReceiver);



                }
            }
        });

        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(10);
        graph.getViewport().setMinY(-600);
        graph.getViewport().setMaxY(600);
        graph.getViewport().setScalableY(true);
        graph.getViewport().setScrollableY(true);



        bluetooth.enable();

    }

    private void chooseDevice(String name) {
        Log.d("choose", "chooseDeviceFUnction ");
        Intent intent = new Intent();
        intent.putExtra("deviceName", name);
        intent.setAction("chooseDevice");
        LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(intent);

    }

    /*
    private void sendGraphStartTime(Long time){
        Intent intent = new Intent();
        intent.putExtra("startTime", time);
        intent.setAction("sendGraphStartTime");
        LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(intent);
    }
    */

    @Override
    protected void onStart() {
        super.onStart();
        bluetooth.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        bluetooth.onStop();
    }

    private BroadcastReceiver deviceListReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            deviceListArray  = intent.getParcelableArrayListExtra("deviceList");

            List<String> s = new ArrayList<>();
            for (BluetoothDevice bt : deviceListArray)
                s.add(bt.getName());

            //arrayAdapter = new ArrayAdapter<>(MainActivity.this, R.layout.text_layout, s);
            arrayAdapter = new ArrayAdapter<>(MainActivity.this, R.layout.dropdown, s);
            deviceListView.setAdapter(arrayAdapter);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this,"Devices Detected!",Toast.LENGTH_SHORT).show();

                }
            });


        }
    };

    private BroadcastReceiver deviceConnectedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            myDevice  = intent.getParcelableExtra("device");


            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this,"Device Connected: "+ myDevice.getName(),Toast.LENGTH_SHORT).show();

                }
            });





        }


    };

    private BroadcastReceiver saveFileReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            final String msg = intent.getStringExtra("msg");

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this,msg ,Toast.LENGTH_SHORT).show();

                }
            });





        }


    };

    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //mes2 = intent.getDat
            mes  = intent.getDoubleExtra("message",0.0);

            if (mSeries == null){
                // If this is the first message, change firstMessageFlag and set some initial parameters
                if (firstMessageFlag){
                    firstMessageFlag = false;
                    final Long time1 = SystemClock.uptimeMillis(); // delete this later
                    //sendGraphStartTime(time1);

                    mSeries = new LineGraphSeries<>();              // initialize grapher
                    graph.removeAllSeries();
                    graph.addSeries(mSeries);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this,"Device Connected. Graphing Started",Toast.LENGTH_SHORT).show();

                        }
                    });


                    // Note: We want to replace this android system time with a data stream of timestamps paired with sensor data from the ESP32.
                    timer =  new Runnable() {
                        @Override
                        public void run() {
                            Long time2 = SystemClock.uptimeMillis(); // delete this later

                            mSeries.appendData(new DataPoint((time2 - time1)/1000.0, mes), followFlag, 1000000,false);


                            mHandler.postDelayed(this,10);


                        }
                    };

                    mHandler.postDelayed(timer,10);
                }

            }



        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        btSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });




    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(MainActivity.this).unregisterReceiver(deviceListReceiver);
        LocalBroadcastManager.getInstance(MainActivity.this).unregisterReceiver(deviceConnectedReceiver);
        LocalBroadcastManager.getInstance(MainActivity.this).unregisterReceiver(messageReceiver);
        LocalBroadcastManager.getInstance(MainActivity.this).unregisterReceiver(saveFileReceiver);




        super.onDestroy();
    }
}


