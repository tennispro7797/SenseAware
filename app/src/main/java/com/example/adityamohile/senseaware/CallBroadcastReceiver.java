package com.example.adityamohile.senseaware;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

/**
 * Created by Aditya Mohile on 6/7/2018.
 */

public class CallBroadcastReceiver extends BroadcastReceiver {

    private String phoneNumber;

    @Override
    public void onReceive(Context context, Intent intent) {

        //Toast.makeText(context,"recieved",Toast.LENGTH_SHORT).show();

        String action = intent.getStringExtra("action");
        phoneNumber = intent.getStringExtra("number");
        Log.v("broadcast","Number is " + phoneNumber);

        //This is used to close the notification tray
        Intent startCallActivity = new Intent(context,CallActivity.class);
        startCallActivity.putExtra("number",phoneNumber);
        startCallActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(startCallActivity);
    }

}
