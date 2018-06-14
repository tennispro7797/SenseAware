package com.example.adityamohile.senseaware;

import android.Manifest;
import android.app.NotificationChannel;
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
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

import static android.support.v4.app.NotificationCompat.VISIBILITY_PUBLIC;

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
    private final String CHANNEL_ID = "jestcall_notifications";

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
            }
        } else if (!TextUtils.isEmpty(phoneNumber) && !isCallActive(getApplicationContext()) && !isScreenOn) {
            showNotification(phoneNumber);
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

    private Intent getNotificationIntent(String contactNumber) {
        Intent intent = new Intent(this, CallBroadcastReceiver.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("makeCall","callingyourmom");
        intent.putExtra("number",contactNumber);
        return intent;
    }

    private Intent getNotificationCancelIntent() {
        Intent intent = new Intent(this, CallBroadcastReceiver.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("makeCall","cancel");
        return intent;
    }

    private void showNotification(String contactNumber) {
        Intent callIntent = getNotificationIntent(contactNumber);
        Intent cancelIntent = getNotificationCancelIntent();
        Log.v("intentType",callIntent.getStringExtra("makeCall"));
        Log.v("intentType",cancelIntent.getStringExtra("makeCall"));
        PendingIntent callPendingIntent =
                PendingIntent.getBroadcast(this, 0, callIntent, 0);
        PendingIntent cancelPendingIntent =
                PendingIntent.getBroadcast(this,0,cancelIntent, 0);
        createNotificationChannel();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,CHANNEL_ID);
        builder.setSmallIcon(R.mipmap.ic_sense_aware_foreground);
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
        builder.setContentTitle("Call Motion Made");
        builder.setContentText("Do you want to make this call?");
        builder.setSubText("Tap the button below to call");
        builder.setVisibility(VISIBILITY_PUBLIC);
        builder.setVibrate(new long[] { 1000, 1000});
        builder.setAutoCancel(true);
        builder.addAction(R.mipmap.ic_launcher,
                "Call",callPendingIntent);
//        builder.addAction(R.mipmap.ic_launcher,
//                "Cancel", cancelPendingIntent);
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
//        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // Will display the notification in the notification bar
//        notificationManager.notify(1, builder.build());
        notificationManagerCompat.notify(1,builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "jestcall_notifications";
            String description = "include all jestcall notifications";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;

            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID,name,importance);
            notificationChannel.setDescription(description);

            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(notificationChannel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(this,"Service has been stopped.",Toast.LENGTH_LONG).show();
    }
}