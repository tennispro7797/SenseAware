package com.example.adityamohile.senseaware;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class CallBroadcastReceiver extends BroadcastReceiver {

    private String phoneNumber;

    @Override
    public void onReceive(Context context, Intent intent) {

        phoneNumber = intent.getStringExtra("number");
        Log.v("broadcastReceived","Number is " + phoneNumber);

        //This is used to close the notification tray
        Intent startCallActivity = new Intent(context,CallActivity.class);
        startCallActivity.putExtra("number",phoneNumber);
        startCallActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(startCallActivity);
    }

}
