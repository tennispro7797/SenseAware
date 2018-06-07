package com.example.adityamohile.senseaware;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

public class CheckFlip extends Service implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor accelerometer;

    private float deltaX = 0;
    private float deltaY = 0;
    private float deltaZ = 0;

    private float mAccel; // acceleration apart from gravity
    private float mAccelCurrent; // current acceleration including gravity
    private float mAccelLast; // last acceleration including gravity

    private float lastX, lastY, lastZ;

    private final String MY_PREFS_NAME = "SenseAware Prefs";
    private static final int MAKE_CALL_PERMISSION_REQUEST_CODE = 1;

    private PowerManager pm;

    public CheckFlip() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
//        throw new UnsupportedOperationException("Not yet implemented");
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//        showNotification();
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI, new Handler());
        pm = (PowerManager)getSystemService(Context.POWER_SERVICE);

        return START_NOT_STICKY; //START_STICKY makes service run even though app is killed
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        deltaX = Math.abs(lastX - sensorEvent.values[0]);
        deltaY = Math.abs(lastY - sensorEvent.values[1]);
        deltaZ = Math.abs(lastZ - sensorEvent.values[2]);

        // set the lastknow values of x,y,z
        lastX = sensorEvent.values[0];
        lastY = sensorEvent.values[1];
        lastZ = sensorEvent.values[2];

        float x = sensorEvent.values[0];
        float y = sensorEvent.values[1];
        float z = sensorEvent.values[2];

        mAccelLast = mAccelCurrent;
        mAccelCurrent = (float) Math.sqrt((double) (x * x + y * y + z * z));
        float delta = mAccelCurrent - mAccelLast;
        mAccel = mAccel * 0.9f + delta; // perform low-cut filter

        if (deltaX >= 7 && deltaY <= 7 && deltaZ <= 1) {
//            showNotification();
            callContact();
            Log.v("successVals","Delta X: " + deltaX + " DeltaY: " + deltaY + " DeltaZ: " + deltaZ);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private boolean checkPermission(String permission) {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void callContact() {

        SharedPreferences prefs = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        String restoredName = prefs.getString("contactName", null);
        String restoredNumber = prefs.getString("contactNumber",null);

        String phoneNumber = restoredNumber;

        boolean isScreenOn = pm.isScreenOn();

        if (!TextUtils.isEmpty(phoneNumber) && !isCallActive(getApplicationContext()) && isScreenOn) {
            if (checkPermission(Manifest.permission.CALL_PHONE)) {
                String dial = "tel:" + phoneNumber;

                Intent intent = new Intent(Intent.ACTION_CALL);
                intent.setData(Uri.parse(dial));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                PackageManager packageManager = getApplicationContext().getPackageManager();
                List activities = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);

                for(int j = 0 ; j < activities.size() ; j++)
                {

                    if(activities.get(j).toString().toLowerCase().contains("com.android.phone"))
                    {
                        intent.setPackage("com.android.phone");
                    }
                    else if(activities.get(j).toString().toLowerCase().contains("call"))
                    {
                        String pack = (activities.get(j).toString().split("[ ]")[1].split("[/]")[0]);
                        intent.setPackage(pack);
                    }
                }

                intent.addFlags(Intent.FLAG_FROM_BACKGROUND);
                startActivity(intent);
            } else {
//                Toast.makeText(CheckFlip.this, "Permission Call Phone denied", Toast.LENGTH_SHORT).show();
            }
        } else if (!TextUtils.isEmpty(phoneNumber) && !isCallActive(getApplicationContext()) && !isScreenOn) {
            showNotification();
//            Toast.makeText(CheckFlip.this, "Enter a phone number", Toast.LENGTH_SHORT).show();
        } else {

        }
    }

    public boolean isCallActive(Context context){
        AudioManager manager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        if(manager.getMode()==AudioManager.MODE_IN_CALL){
            return true;
        }
        else{
            return false;
        }
    }

    private Intent getNotificationIntent() {
        Intent intent = new Intent(this,MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return intent;
    }

    private void showNotification() {
        Intent callIntent = getNotificationIntent();
        callIntent.setAction("Make Call");
        // Have to make sure the pending intent is processed so the call() method can be invoked
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(android.R.drawable.ic_input_get);
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        builder.setContentIntent(pendingIntent);
        builder.setContentTitle("Call Motion Registered!");
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
        builder.setContentText("The call motion was just registered. Do you want to follow through with this call?");
        builder.setSubText("Tap the button to call");
        builder.addAction(R.mipmap.ic_sense_aware_foreground,
                "Call",PendingIntent.getActivity(this,
                        0, callIntent, PendingIntent.FLAG_UPDATE_CURRENT));
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // Will display the notification in the notification bar
        notificationManager.notify(1, builder.build());
//        Log.v("notificationMSG","showNotifiction() called!");
//        final String CHANNEL_ID = "SOMETHING";
//        Intent intent = new Intent(this, MainActivity.class);
//        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
//        Log.v("notificationMSG","CHECKPOINT 1");
//
//        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
//                .setSmallIcon(R.drawable.ic_launcher_foreground)
//                .setContentTitle("My notification")
//                .setContentText("Much longer text that cannot fit one line...")
//                .setStyle(new NotificationCompat.BigTextStyle()
//                        .bigText("Much longer text that cannot fit one line..."))
//                .setPriority(NotificationCompat.PRIORITY_HIGH)
//                .setContentIntent(pendingIntent)
//                .setAutoCancel(true);
//        Log.v("notificationMSG","CHECKPOINT 2");
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            CharSequence name = CHANNEL_ID;
//            String description = "something good";
//            int importance = NotificationManager.IMPORTANCE_DEFAULT;
//            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
//            channel.setDescription(description);
//            // Register the channel with the system; you can't change the importance
//            // or other notification behaviors after this
//            NotificationManager notificationManager = getSystemService(NotificationManager.class);
//            notificationManager.createNotificationChannel(channel);
//            Log.v("notificationMSG","CHECKPOINT 3");
//        }
//
//        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
//
//        // notificationId is a unique int for each notification that you must define
//        notificationManager.notify(100, mBuilder.build());
//        Log.v("notificationMSG","showNotifiction() completed!");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(this,"Service has been stopped.",Toast.LENGTH_LONG).show();
    }
}