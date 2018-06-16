package com.adityamohile.jestcall.app;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

/**
 * Created by Aditya Mohile on 5/20/2018.
 */

public class PhoneAnimationDialogFragment extends DialogFragment {

    private ImageView phoneIcon;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(),R.style.CustomAlertDialog);
        LayoutInflater inflater = getActivity().getLayoutInflater();
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        final View v = inflater.inflate(R.layout.dialog, null);
        phoneIcon = v.findViewById(R.id.phoneIconDialog);
        builder.setView(v);
        phoneIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rotate();
            }
        });
        builder.setTitle("Welcome to JestCall!").setMessage("Choose a contact and spin the phone like below to call your chosen contact!")
                .setPositiveButton("Got it!", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        // Create the AlertDialog object and return it
        setStyle(DialogFragment.STYLE_NO_FRAME,R.drawable.dialog_bg);
        return builder.create();
    }

    public void rotate() {
        Animation antiRotate = AnimationUtils.loadAnimation(getActivity().getApplicationContext(),R.anim.rotate_anticlockwise);
        final Animation rotate = AnimationUtils.loadAnimation(getActivity().getApplicationContext(),R.anim.rotate_clockwise);
        antiRotate.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                phoneIcon.startAnimation(rotate);
                Log.v("animation","rotated CCW?");
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        phoneIcon.startAnimation(antiRotate);
        Log.v("animation","Supposed to be animating!");
    }
}