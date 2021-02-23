package com.example.zerodhabrokeage;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class welcome extends AppCompatActivity {
    Button button;
    ImageView logo;
    private static final int REQUEST_SCREENSHOT=59706;
    private MediaProjectionManager mediaProjectionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        button = findViewById(R.id.start);
        logo = findViewById(R.id.logo);

        mediaProjectionManager=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
        logo.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(),R.anim.translate));

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                button.setVisibility(View.VISIBLE);
            }
        },1500);


        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if(!Settings.canDrawOverlays(getBaseContext())){
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:"+getPackageName()));
                        startActivity(intent);
                }}
                startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(),
                        REQUEST_SCREENSHOT);
            }
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SCREENSHOT) {
            if (resultCode == RESULT_OK ) {
                Intent i = new Intent(this, backgroundservice.class)
                                .putExtra(backgroundservice.EXTRA_RESULT_CODE, resultCode)
                                .putExtra(backgroundservice.EXTRA_RESULT_INTENT, data);

                startService(i);
                finish();
            }}}}

