package com.example.zerodhabrokeage;


import android.annotation.SuppressLint;

import android.app.PendingIntent;
import android.app.Service;

import android.content.Intent;

import android.graphics.Color;

import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.AudioManager;
import android.media.MediaScannerConnection;
import android.media.ToneGenerator;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;

import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;


import java.io.File;
import java.io.FileOutputStream;
import java.util.Calendar;

public class backgroundservice extends Service {
    private static final String CHANNEL_WHATEVER="channel_whatever";
    private static final int NOTIFY_ID=9906;
    static final String EXTRA_RESULT_CODE="resultCode";
    static final String EXTRA_RESULT_INTENT="resultIntent";
    static final String ACTION_RECORD= BuildConfig.APPLICATION_ID+".RECORD";
    static final String ACTION_SHUTDOWN= BuildConfig.APPLICATION_ID+".SHUTDOWN";
    static final int VIRT_DISPLAY_FLAGS= DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC;
    private MediaProjection projection;
    private VirtualDisplay vdisplay;
    final private HandlerThread handlerThread= new HandlerThread(getClass().getSimpleName(), android.os.Process.THREAD_PRIORITY_BACKGROUND);
    private Handler handler;
    private MediaProjectionManager mediaProjectionManager;
    private WindowManager windowManager;
    private imagetransformer it;
    private int resultCode;
    private Intent resultData;
    int height,width;
    View mFloatingView;
    int posx,posy;
    TextView broke;
    final private ToneGenerator beeper= new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("ClickableViewAccessibility")
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate() {
        super.onCreate();
        mediaProjectionManager=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);

        mFloatingView = LayoutInflater.from(this).inflate(R.layout.floating_widget,null);

        handlerThread.start();
        handler=new Handler(handlerThread.getLooper());
        broke = mFloatingView.findViewById(R.id.brokeage);

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        height = windowManager.getDefaultDisplay().getHeight();
        width = windowManager.getDefaultDisplay().getWidth();

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.RIGHT;
        params.x = 100;
        params.y = 100;

        windowManager.addView(mFloatingView, params);
        print("Service begin");
        broke.setText(R.string.zbroke);


        broke.setOnTouchListener(new View.OnTouchListener() {
            private int initialX,initialY;
            private float initialTouchX,initialTouchY;
            long startClickTime;
            final int duration = 200;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        startClickTime = Calendar.getInstance().getTimeInMillis();
                        initialX = params.x;
                        initialY = params.y;

                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_UP:
                        long clickduration = Calendar.getInstance().getTimeInMillis() - startClickTime;

                        params.x = initialX +(int)(initialTouchX - event.getRawX());
                        params.y = initialY +(int)(event.getRawY() - initialTouchY);
                        if (clickduration<duration){
                            startCapture();
                            beeper.startTone(ToneGenerator.TONE_PROP_NACK);

                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    beeper.stopTone();
                                }
                            },100);
                        }else if(params.y> height){
                            stopSelf();
                            print("Service ended");
                            windowManager.removeView(mFloatingView);
                        }
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (initialTouchX-event.getRawX() );
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                       // print(" x "+event.getRawX()+" y "+event.getRawY());
                        windowManager.updateViewLayout(mFloatingView,params);
                        posx = params.x;
                        posy = params.y;
                        return true;
                }
                return false;
            }
        });

    }
    @SuppressLint("UseCompatLoadingForDrawables")
    public void changeback(int condition){
        if (condition == 0){
            broke.setTextColor(Color.BLACK);
            broke.setBackground(getDrawable(R.drawable.normal_background));
        }else if(condition == 1){
            broke.setTextColor(Color.WHITE);
            broke.setBackground(getDrawable(R.drawable.buy_background));
        }else if(condition == 2){
            broke.setTextColor(Color.WHITE);
            broke.setBackground(getDrawable(R.drawable.sell_background));
        }
    }




    @Override
    public int onStartCommand(Intent i, int flags, int startId) {
        if (i.getAction()==null) {
            resultCode=i.getIntExtra(EXTRA_RESULT_CODE, 1337);
            resultData=i.getParcelableExtra(EXTRA_RESULT_INTENT);
            //foregroundify();
        }
        else if (ACTION_RECORD.equals(i.getAction())) {
            if (resultData!=null) {
                //startCapture();
            }
            else {
                Intent ui=
                        new Intent(this, welcome.class)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                startActivity(ui);
                Toast.makeText(getBaseContext(),"check once",Toast.LENGTH_SHORT).show();
            }
        }
        else if (ACTION_SHUTDOWN.equals(i.getAction())) {
            //beeper.startTone(ToneGenerator.TONE_CDMA_ANSWER);
            stopForeground(true);
            stopSelf();
        }

        return(START_NOT_STICKY);
    }

    @Override
    public void onDestroy() {
        stopCapture();

        super.onDestroy();
    }

    @Nullable

    WindowManager getWindowManager() {
        return(windowManager);
    }

    Handler getHandler() {
        return(handler);
    }

    void processImage(final byte[] jpeg) {
        new Thread() {
            @Override
            public void run() {
                File output=new File(getExternalFilesDir(null),
                        "screenshot.jpeg");

                try {
                    FileOutputStream fos=new FileOutputStream(output);

                    fos.write(jpeg);
                    fos.flush();
                    fos.getFD().sync();
                    fos.close();

                    MediaScannerConnection.scanFile(backgroundservice.this,
                            new String[] {output.getAbsolutePath()},
                            new String[] {"image/jpeg"},
                            null);
                }
                catch (Exception e) {
                    Log.e(getClass().getSimpleName(), "Exception writing out screenshot", e);
                }
            }
        }.start();
        stopCapture();
    }


    private void stopCapture() {
        if (projection!=null) {
            projection.stop();
            vdisplay.release();
            projection=null;
        }
    }

    private void startCapture() {
        projection=mediaProjectionManager.getMediaProjection(resultCode, resultData);
        it=new imagetransformer(this);

        MediaProjection.Callback cb=new MediaProjection.Callback() {
            @Override
            public void onStop() {
                vdisplay.release();
            }
        };

        vdisplay=projection.createVirtualDisplay("andshooter",
                it.getWidth(), it.getHeight(),
                getResources().getDisplayMetrics().densityDpi,
                VIRT_DISPLAY_FLAGS, it.getSurface(), null, handler);
        projection.registerCallback(cb, handler);
    }

    public boolean left(){
        if(posx < width/2)return true;
        else return false;
    }

    private PendingIntent buildPendingIntent(String action) {
        Intent i=new Intent(this, getClass());

        i.setAction(action);

        return(PendingIntent.getService(this, 0, i, 0));
    }
    public void SetText(float value){
        broke.setText(String.valueOf(value));
    }
    void print(String msg){
        Toast.makeText(getBaseContext(),msg,Toast.LENGTH_SHORT).show();
        Log.d("Service",msg);
    }
}
