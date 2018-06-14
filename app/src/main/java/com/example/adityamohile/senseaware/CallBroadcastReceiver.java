package com.example.adityamohile.senseaware;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import static android.content.Context.NOTIFICATION_SERVICE;

public class CallBroadcastReceiver extends BroadcastReceiver {

    private String phoneNumber;
    private String intentType;

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.v("intentType",intent.getStringExtra("makeCall"));
        intentType = intent.getStringExtra("makeCall");
        Log.v("intentType",intentType);
        if (intentType.equals("callingyourmom")) {
            phoneNumber = intent.getStringExtra("number");
            Log.v("broadcastReceived","Number is " + phoneNumber);

            //This is used to close the notification tray
            Intent startCallActivity = new Intent(context,CallActivity.class);
            startCallActivity.putExtra("number",phoneNumber);
            startCallActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(startCallActivity);
        } else if (intentType.equals("cancel")) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
            notificationManager.cancel(1);
        }

    }

}
