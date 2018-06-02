package com.example.adityamohile.senseaware;

import android.Manifest;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.SwitchCompat;
import android.util.Base64;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.amulyakhare.textdrawable.TextDrawable;

import org.w3c.dom.Text;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import de.hdodenhof.circleimageview.CircleImageView;
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt;

import static android.util.Half.EPSILON;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor accelerometer;

    private TextView xData;
    private TextView yData;
    private TextView zData;

    private float deltaX = 0;
    private float deltaY = 0;
    private float deltaZ = 0;

    private float lastX, lastY, lastZ;

    private CircleImageView contactPhoto;
    private TextView contactName;
    private TextView contactNumber;
    private static final int RESULT_PICK_CONTACT = 2015;
    private static final int MAKE_CALL_PERMISSION_REQUEST_CODE = 1;
    private boolean alreadyDialed = false;
    private DecimalFormat decimalFormat;
    private Context mContext;

    private FloatingActionButton openFab;
    private FloatingActionButton openFabFirst;
    private CardView contactCard;
    private Button editContact;
    private Button deleteContact;
    private ImageView phoneIcon;
    private TextView supportText;
    private SwitchCompat pocketMode;
    private Boolean fabFirstVisible = true;

    private PhoneAnimationDialogFragment dialogBox;

    private final String MY_PREFS_NAME = "SenseAware Prefs";

    // Create a constant to convert nanoseconds to seconds.
    private static final float NS2S = 1.0f / 1000000000.0f;
    private final float[] deltaRotationVector = new float[4];
    private float timestamp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        decimalFormat = new DecimalFormat("#.#");

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mContext = getApplicationContext();
        contactName = (TextView) findViewById(R.id.contactName);
        contactNumber = (TextView) findViewById(R.id.contactNumber);
        contactPhoto = (CircleImageView) findViewById(R.id.contactPhoto);

//        phoneIcon = findViewById(R.id.phoneIcon);
//        phoneIcon.setVisibility(View.GONE);
//        supportText = findViewById(R.id.supportText);
//        supportText.setVisibility(View.GONE);

        openFab = findViewById(R.id.openBtn);
        openFab.setColorFilter(Color.BLACK);

        openFabFirst = findViewById(R.id.openBtnFirst);
        openFabFirst.setColorFilter(Color.BLACK);

        contactCard = findViewById(R.id.contactCard);
        contactCard.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                contactCard.setCardElevation(50);
                Log.v("longClick","Card has been longclicked!");
                return false;
            }
        });
        contactCard.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_UP:
                        contactCard.setCardElevation(1);
                        break;
                }
                return false;
            }
        });

        editContact = findViewById(R.id.editContact);
        deleteContact = findViewById(R.id.deleteContact);

//        pocketMode = findViewById(R.id.pocketMode);

        if (!checkPermission(Manifest.permission.CALL_PHONE)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, MAKE_CALL_PERMISSION_REQUEST_CODE);
        }

        if (!checkPermission(Manifest.permission.READ_CONTACTS)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, 2);
        }

        SharedPreferences prefs = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        String restoredName = prefs.getString("contactName", null);
        String restoredNumber = prefs.getString("contactNumber",null);
        String restoredPhoto = prefs.getString("contactImage",null);
        if (restoredName != null) {
            // If contact info is already stored
            contactName.setText(restoredName);
            contactNumber.setText(restoredNumber);
            openFabFirst.setVisibility(View.GONE);
        } else {
            contactCard.setVisibility(View.GONE);
            openFab.setVisibility(View.GONE);

        }
        if (restoredPhoto != "" && restoredPhoto != null) {
            byte[] imageAsBytes = Base64.decode(restoredPhoto.getBytes(), Base64.DEFAULT);
            contactPhoto.setImageBitmap(BitmapFactory.decodeByteArray(imageAsBytes, 0, imageAsBytes.length));
            Log.v("onStartup","Actual photo shown!");
        } else if (restoredPhoto == "") {
            TextDrawable drawable = TextDrawable.builder()
                    .beginConfig()
                    .width(60)
                    .height(60)
                    .endConfig()
                    .buildRect(restoredName.substring(0,1), Color.RED);
            contactPhoto.setImageDrawable(drawable);
            Log.v("onStartup","DRAWN photo shown!");
        }

        final Intent startCheckFlip = new Intent(this, CheckFlip.class);
        startService(startCheckFlip);

        openDialog(contactCard);

        new MaterialTapTargetPrompt.Builder(MainActivity.this)
                .setTarget(findViewById(R.id.openBtnFirst))
                .setPrimaryText("Welcome to SenseAware!")
                .setSecondaryText("Tap the plus to add your most called contact")
                .setBackgroundColour(Color.rgb(199,91,57))
                .setPromptStateChangeListener(new MaterialTapTargetPrompt.PromptStateChangeListener()
                {
                    @Override
                    public void onPromptStateChanged(MaterialTapTargetPrompt prompt, int state)
                    {
                        if (state == MaterialTapTargetPrompt.STATE_FOCAL_PRESSED)
                        {
                            // User has pressed the prompt target
                        }
                    }
                })
                .show();

//        pocketMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
//                if (b) {
//                    stopService(new Intent(getApplicationContext(), CheckFlip.class));
//                    Log.v("serviceStatus","service has been stopped");
//                } else {
//                    stopService(new Intent(getApplicationContext(), CheckFlip.class));
//                    Log.v("serviceStatus","Service has been restarted");
//                }
//            }
//        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // check whether the result is ok
        if (resultCode == RESULT_OK) {
            // Check for the request code, we might be usign multiple startActivityForReslut
            switch (requestCode) {
                case RESULT_PICK_CONTACT:
                    contactPicked(data);
            }
        } else {
            Log.e("MainActivity", "Failed to pick contact");
        }
    }
    /**
     * Query the Uri and read contact details. Handle the picked contact data.
     * @param data
     */
    private void contactPicked(Intent data) {
        Cursor cursor = null;
        if (fabFirstVisible) {
            openFabFirst.setVisibility(View.GONE);
            openFab.setVisibility(View.VISIBLE);
            contactCard.setVisibility(View.VISIBLE);
            fabFirstVisible = false;
        }
        try {
            String phoneNo = null ;
            String name = null;
            String image_uri = null;
            // getData() method will have the Content Uri of the selected contact
            Uri uri = data.getData();
            //Query the content uri
            cursor = getContentResolver().query(uri, null, null, null, null);
            cursor.moveToFirst();
            // column index of the phone number
            int  phoneIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            // column index of the contact name
            int  nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
            // column index of contact image
            phoneNo = cursor.getString(phoneIndex);
            name = cursor.getString(nameIndex);
            long contactID = getContactIDFromNumber(phoneNo,this);
            Log.v("photoStuff","Contact ID: "+contactID);
            InputStream contactPic = openPhoto(contactID);
            String encodedImage = "";
            if (contactPic != null) {
                Bitmap bitmap1 = BitmapFactory.decodeStream(contactPic);
                Log.v("photoStuff","Successfully made bitmap!");
                contactPhoto.setImageBitmap(bitmap1);
                // Convert bitmap photo to ByteArrayOutputStream
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap1.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                byte[] b = baos.toByteArray();
                encodedImage = Base64.encodeToString(b, Base64.DEFAULT);
            } else {
                TextDrawable drawable = TextDrawable.builder()
                        .beginConfig()
                        .width(60)
                        .height(60)
                        .endConfig()
                        .buildRect(name.substring(0,1), Color.RED);
                contactPhoto.setImageDrawable(drawable);
            }

            Log.v("photoStuff","Should display photo!");
            // Set the value to the textviews
            contactName.setText(name);
            contactNumber.setText(phoneNo);

            SharedPreferences.Editor editor = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();
            editor.putString("contactName", name);
            editor.putString("contactNumber", phoneNo);
            editor.putString("contactImage", encodedImage);
            editor.apply();
            Log.v("contactName Added","Added contact name: " + name);
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    // Actions to do after 10 seconds
                    new MaterialTapTargetPrompt.Builder(MainActivity.this)
                            .setTarget(findViewById(R.id.deleteContact))
                            .setPrimaryText("Remove this contact")
                            .setSecondaryText("Tap the button to no longer use this contact")
                            .setBackgroundColour(Color.rgb(199,91,57))
                            .show();

                    new MaterialTapTargetPrompt.Builder(MainActivity.this)
                            .setTarget(findViewById(R.id.editContact))
                            .setPrimaryText("Choose another contact")
                            .setSecondaryText("Tap the button to change the contact")
                            .setBackgroundColour(Color.rgb(199,91,57))
                            .show();
                }
            }, 1200);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int getContactIDFromNumber(String contactNumber,Context context)
    {
        contactNumber = Uri.encode(contactNumber);
        int phoneContactID = new Random().nextInt();
        Cursor contactLookupCursor = context.getContentResolver().query(Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,contactNumber),new String[] {ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup._ID}, null, null, null);
        while(contactLookupCursor.moveToNext()){
            phoneContactID = contactLookupCursor.getInt(contactLookupCursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup._ID));
        }
        contactLookupCursor.close();

        return phoneContactID;
    }

    public InputStream openPhoto(long contactId) {
        Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
        Uri photoUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
        Cursor cursor = getContentResolver().query(photoUri,
                new String[] {ContactsContract.Contacts.Photo.PHOTO}, null, null, null);
        if (cursor == null) {
            return null;
        }
        try {
            if (cursor.moveToFirst()) {
                byte[] data = cursor.getBlob(0);
                if (data != null) {
                    return new ByteArrayInputStream(data);
                }
            }
        } finally {
            cursor.close();
        }
        return null;
    }

    private boolean checkPermission(String permission) {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode) {
            case MAKE_CALL_PERMISSION_REQUEST_CODE :
                if (grantResults.length > 0 && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
//                    callBtn.setEnabled(true);
                    Toast.makeText(this, "SenseAware can now make calls", Toast.LENGTH_SHORT).show();
                }
                break;
            case 2:
                if (grantResults.length > 0 && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
//                    callBtn.setEnabled(true);
                    Toast.makeText(this, "You can read contacts by clicking on the FAB", Toast.LENGTH_SHORT).show();
                }
                break;
        }
        return;
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {

        // get the change of the x,y,z values of the accelerometer
        deltaX = Math.abs(lastX - event.values[0]);
        deltaY = Math.abs(lastY - event.values[1]);
        deltaZ = Math.abs(lastZ - event.values[2]);

        // set the lastknow values of x,y,z
        lastX = event.values[0];
        lastY = event.values[1];
        lastZ = event.values[2];

    }

    public void editContact(View v) {
        Intent contactPickerIntent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        startActivityForResult(contactPickerIntent, RESULT_PICK_CONTACT);
    }

    public void editContactFirst(View v) {
        Intent contactPickerIntent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        startActivityForResult(contactPickerIntent, RESULT_PICK_CONTACT);
    }

    public void deleteContact(View v) {
        contactCard.setVisibility(View.GONE);
        openFabFirst.setVisibility(View.VISIBLE);
        SharedPreferences.Editor editor = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();
        editor.remove("contactName");
        editor.remove("contactNumber");
        editor.remove("contactImage");
        editor.apply();
        Log.v("cleared","Should have deleted SharedPrefs");
        fabFirstVisible = true;
    }

    public void rotate() {
        Animation antiRotate = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.rotate_anticlockwise);
        final Animation rotate = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.rotate_clockwise);
        antiRotate.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                phoneIcon.startAnimation(rotate);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        rotate.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                phoneIcon.setVisibility(View.GONE);
                supportText.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        phoneIcon.startAnimation(antiRotate);
    }

    public void showHelpInfo(View v) {
        supportText.setVisibility(View.VISIBLE);
        phoneIcon.setVisibility(View.VISIBLE);
        rotate();
    }

    public void openDialog(View v) {
        dialogBox = new PhoneAnimationDialogFragment();
        dialogBox.show(getFragmentManager(),"hello");
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

}