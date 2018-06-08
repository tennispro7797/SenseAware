package com.example.adityamohile.senseaware;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.util.List;

public class CallActivity extends AppCompatActivity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v("call","Started call activity!");
        Intent intent = getIntent();
        String phoneNumber = intent.getStringExtra("number");
        callContact(phoneNumber);
    }

    private boolean checkPermission(String permission) {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    public void callContact(String phoneNumber) {
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

//            intent.addFlags(Intent.FLAG);
            startActivity(intent);
        }
    }

}
