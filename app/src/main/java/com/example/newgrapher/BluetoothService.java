package com.example.newgrapher;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;


import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import me.aflak.bluetooth.Bluetooth;
import me.aflak.bluetooth.BluetoothCallback;
import me.aflak.bluetooth.DeviceCallback;
import me.aflak.bluetooth.DiscoveryCallback;


public class BluetoothService extends Service {

    List<BluetoothDevice> deviceListArray;
    BluetoothDevice myDevice;
    Bluetooth bluetooth = new Bluetooth(BluetoothService.this);
    String chosenDevice;
    Double mes= 0.0;
    LineGraphSeries mSeries;
    Long graphTime1;

    ArrayList<String> messageList = new ArrayList<>();
    ArrayList<Long> timeList = new ArrayList<>();



    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void sendDevicesBroadcast(List<BluetoothDevice> deviceList) {
        Intent intent = new Intent();
        intent.putParcelableArrayListExtra("deviceList",(ArrayList) deviceList);
        intent.setAction("BluetoothDevices");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }


    private void deviceConnectedBroadcast(BluetoothDevice device) {
        Intent intent = new Intent();
        intent.putExtra("device", device);
        intent.setAction("connectedDevice");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void updateMessageBroadcast(Double message) {
        Intent intent = new Intent();
        intent.putExtra("message", message);
        intent.setAction("sendMessage");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LocalBroadcastManager.getInstance(BluetoothService.this).registerReceiver(chooseDeviceReceiver, new IntentFilter("chooseDevice"));
        //LocalBroadcastManager.getInstance(BluetoothService.this).registerReceiver(graphTime1Receiver, new IntentFilter("sendGraphStartTime"));
        LocalBroadcastManager.getInstance(BluetoothService.this).registerReceiver(getDataListReceiver, new IntentFilter("getDataList"));

    }

    private BroadcastReceiver chooseDeviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if(intent.getAction() != null && intent.getAction().equals("chooseDevice")){
                if (!bluetooth.isConnected()){

                    chosenDevice = intent.getStringExtra("deviceName" );
                    Log.d("bt", "chose device: "+ chosenDevice);
                    bluetooth.connectToName(chosenDevice);
                    Log.d("bt", "connect to device ");

                }
            }


        }
    };

    private void sendDataLists(){

        Intent intent = new Intent();
        intent.setAction("sendDataList");
        intent.putExtra("timeList", timeList);
        intent.putExtra("messageList", messageList);


        LocalBroadcastManager.getInstance(BluetoothService.this).sendBroadcast(intent);

    }

    private void sendSaveFileToast(String e){

        Intent intent = new Intent();
        intent.setAction("fileToast");
        intent.putExtra("msg",e);
        LocalBroadcastManager.getInstance(BluetoothService.this).sendBroadcast(intent);

    }

    private BroadcastReceiver getDataListReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if(intent.getAction() != null && intent.getAction().equals("getDataList")){

                sendDataLists();

            }

        }
    };

    /*
    private BroadcastReceiver graphTime1Receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if(intent.getAction() != null && intent.getAction().equals("sendGraphStartTime")) {

                //timeList.clear();
                graphTime1 = intent.getLongExtra("startTime",0);
                Log.e("time", "timeis:" + graphTime1.toString());

            }

        }
    };
    */



    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    "1",
                    "Breathe Notification",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("BT", "onStartCommand: " );
        createNotificationChannel();
        bluetooth.onStart();

        bluetooth.enable();

        bluetooth.startScanning();




        bluetooth.setBluetoothCallback(new BluetoothCallback() {
            @Override
            public void onBluetoothTurningOn() {
                bluetooth.startScanning();

                Log.e("BT", "On BT turning On" );

            }

            @Override
            public void onBluetoothOn() {
                bluetooth.startScanning();
                Log.e("BT", "on BT on " );

            }

            @Override
            public void onBluetoothTurningOff() {

            }

            @Override
            public void onBluetoothOff() {

            }

            @Override
            public void onUserDeniedActivation() {

            }
        });


        bluetooth.setDiscoveryCallback(new DiscoveryCallback() {
            @Override
            public void onDiscoveryStarted() {

                Log.e("Service", "onDiscoveryStarted: " );

                List<BluetoothDevice> devices = bluetooth.getPairedDevices();

                sendDevicesBroadcast(devices);



            }

            @Override
            public void onDiscoveryFinished() {

                Log.d("msg", "discovery finished");


            }

            @Override
            public void onDeviceFound(BluetoothDevice device) {

                Log.d("msg", "device found");


            }

            @Override
            public void onDevicePaired(BluetoothDevice device) {
                Log.d("msg", "device paired");


            }

            @Override
            public void onDeviceUnpaired(BluetoothDevice device) {

            }

            @Override
            public void onError(String message) {

                Log.d("msg", "error:" + message);


            }
        });

        bluetooth.setDeviceCallback(new DeviceCallback() {
            @Override
            public void onDeviceConnected(BluetoothDevice device) {

                deviceConnectedBroadcast(device);
                Log.d("msg", "device connected:" + device.getName());


            }

            @Override
            public void onDeviceDisconnected(BluetoothDevice device, String message) {

            }

            @Override
            public void onMessage(String message) {

                //Log.d("msg", message);

                if (message.length() > 0){

                    mes = Double.parseDouble(message);
                    messageList.add(mes+"");
                    timeList.add(SystemClock.elapsedRealtimeNanos());
                    updateMessageBroadcast(mes);

                    //              String mes[] = message.split(",");
                }


            }

            @Override
            public void onError(String message) {

            }

            @Override
            public void onConnectError(BluetoothDevice device, String message) {

            }
        });

        Intent openAppIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, openAppIntent, 0);


        Notification notification = new NotificationCompat.Builder(this, "1")
                .setContentTitle("ProjectStorque Grapher")
                .setContentText("Tap here to go back to app and stop")
                .setContentTitle("ProjectStorque Grapher")
                .setSmallIcon(R.drawable.notification_icon)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);





        return START_REDELIVER_INTENT;

    }






    @Override
    public void onDestroy() {
        Log.e("BTService", "onDestroy: " );
        LocalBroadcastManager.getInstance(BluetoothService.this).unregisterReceiver(chooseDeviceReceiver);

        SaveFile(timeList,messageList);

        super.onDestroy();
        bluetooth.onStop();
        bluetooth.removeDiscoveryCallback();
        bluetooth.removeCommunicationCallback();
        bluetooth.removeBluetoothCallback();
        bluetooth.disconnect();



    }


    private void SaveFile(ArrayList<Long> timeList, ArrayList<String> messageList) {

        long zeroPoint = timeList.get(0);

        for(int i=0;i<timeList.size();i++){

            timeList.set(i,timeList.get(i) - zeroPoint);
        }


        ArrayList<String> totalList = new ArrayList<>();

        int totalSize = timeList.size();

        if (timeList.size() > messageList.size()){
             totalSize = messageList.size();
        }

        String timeMs = "0.0";


        for(int i=0;i<totalSize;i++){


            timeMs = String.format("%.2f",timeList.get(i) * 1e-6);

            totalList.add(timeMs + "," + messageList.get(i));
            //Log.e("file", totalList.get(i) );


        }

        //Checking the availability state of the External Storage.
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            //If it isn't mounted - we can't write into it.
            return;
        }

        String currentDateAndTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String fname = "sTorqueData-"+ currentDateAndTime +  ".txt";


        File mFolder = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS) + "/BajaSTorqueData");
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS) + "/BajaSTorqueData" , fname);



        try {
            if (!mFolder.exists()) {
                mFolder.mkdir();
            }
            if (!file.exists()) {
                file.createNewFile();
            }

            BufferedWriter bw =
                    new BufferedWriter (new FileWriter(file)) ;

            for (String line : totalList) {
                bw.write (line);
                bw.newLine();
                Log.e("bw", line );
            }

            bw.close();

            sendSaveFileToast("Saved File Successful");


        } catch (IOException e) {
            e.printStackTrace ();
            sendSaveFileToast("Failed Saving File: " + e.toString());
        }


    }



}
