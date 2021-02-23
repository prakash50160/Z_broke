package com.example.zerodhabrokeage;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.ByteBuffer;

public class imagetransformer implements ImageReader.OnImageAvailableListener{
    private final int width;
    private final int height;
    private final ImageReader imageReader;
    private final backgroundservice svc;
    private Bitmap latestBitmap=null;
    boolean buy = true;
    welcome wel = new welcome();
    Bundle savebundle = new Bundle();
    int buy_color = -12483341;
    int sell_color = -2141876;

    @SuppressLint("WrongConstant")
    imagetransformer(backgroundservice svc) {
        this.svc=svc;

        Display display=svc.getWindowManager().getDefaultDisplay();
        Point size=new Point();

        display.getRealSize(size);

        int width=size.x;
        int height=size.y;

        while (width*height > (2<<19)) {
            width=width>>1;
            height=height>>1;
        }

        this.width=width;
        this.height=height;

        imageReader=ImageReader.newInstance(width, height,
                PixelFormat.RGBA_8888, 2);
        imageReader.setOnImageAvailableListener(this, svc.getHandler());
    }

    @Override
    public void onImageAvailable(ImageReader reader) {
        final Image image=imageReader.acquireLatestImage();


        if (image!=null) {
            Image.Plane[] planes=image.getPlanes();
            ByteBuffer buffer=planes[0].getBuffer();
            int pixelStride=planes[0].getPixelStride();
            int rowStride=planes[0].getRowStride();
            int rowPadding=rowStride - pixelStride * width;
            int bitmapWidth=width + rowPadding / pixelStride;

            if (latestBitmap == null ||
                    latestBitmap.getWidth() != bitmapWidth ||
                    latestBitmap.getHeight() != height) {
                if (latestBitmap != null) {
                    latestBitmap.recycle();
                }

                latestBitmap=Bitmap.createBitmap(bitmapWidth,
                        height, Bitmap.Config.ARGB_8888);
            }

            latestBitmap.copyPixelsFromBuffer(buffer);
            image.close();

            ByteArrayOutputStream baos=new ByteArrayOutputStream();
            Bitmap cropped_quantity=Bitmap.createBitmap(latestBitmap, 0, (height/5)-20,
                    width/2, height/10);
            Bitmap cropped_price=Bitmap.createBitmap(latestBitmap, width/2, (height/5)-20,
                    width/2, height/10);

            //Bitmap cropped_sell=Bitmap.createBitmap(latestBitmap, 0, height-200,
                   // width, 200);

            int pixel = latestBitmap.getPixel(width/3,height-100);
            int pixel1 = latestBitmap.getPixel((width/2)+(width/4),(height/2)+(height/9));
            //int pixel1 = cropped_price.getPixel()

            if(pixel== sell_color || pixel1 == sell_color ){
                buy = false;
                svc.changeback(2);
            }else if(pixel == buy_color || pixel1 == buy_color) {
                buy = true;
                svc.changeback(1);
            }else svc.changeback(0);


            //Log.i("color", "value "+cropped_price.getPixel(width/3,100)+"red "+Color.red(pixel)+ "blue"+Color.blue(pixel)+"green"+Color.green(pixel)+"Color blue "+Color.BLUE);


            getText(cropped_quantity,false);
            getText(cropped_price,true);



            latestBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);

            byte[] newJpeg=baos.toByteArray();

            svc.processImage(newJpeg);
        }
    }

    Surface getSurface() {
        return(imageReader.getSurface());
    }

    int getWidth() {
        return(width);
    }

    int getHeight() {
        return(height);
    }

    void close() {
        imageReader.close();
    }

    private void getText(Bitmap bitmap,boolean trigger){
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        TextRecognizer recognizer = TextRecognition.getClient();
        recognizer.process(image).addOnSuccessListener(new OnSuccessListener<Text>() {
            @Override
            public void onSuccess(Text text) {
                int quantity = 0;
                float price = 0;
                String S_quantity = "";

                if(!trigger){
                   save_value(text.getText().trim(),true);
                }
                else{
                    try {
                        quantity = Integer.parseInt(save_value("",false));
                        price = Float.parseFloat(text.getText().trim());
                    }catch (NumberFormatException e){
                        Log.i("Error",e.toString());
                    }
                    if(quantity != 0){
                        Process_value(quantity,price);
                        Log.i("check","quantity: "+quantity+" Price: "+price);
                    }
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.i("tag","failed");

            }
        });
    }
    private String save_value(String value,boolean trigger){
        String value1 = "";
        if (trigger){
            savebundle.putString("save",value);
        }else {
            value1 = savebundle.getString("save");
        }
        return value1;
    }
    private void Process_value(int quantity,float price){
        boolean NSE = svc.left();
        float value =(quantity * price);
        float brokeage_temp =(float)((0.03/100)*value);
        float brokeage = 0;
        float STT = 0;
        float transaction = 0;
        float GST = 0;
        float SEBI = 0;
        float stamp = 0;
        float overall = 0;

        if(brokeage_temp < 20)brokeage = brokeage_temp;
        else brokeage = 20;

        if(!buy) STT = (float)((0.025/100)*value);
        else STT = 0;

        if(NSE) transaction = (float)((0.00345/100)*value);
        else transaction = (float)((0.003/100)*value);

        GST = (float)((0.18)*(brokeage+transaction));

        SEBI =(float) ((0.00005/100)*value);

        if(buy) stamp = (float)((0.003/100)*value);
        else stamp = 0;

        overall = brokeage + STT + transaction + GST + SEBI + stamp;

        float output = (float)(Math.round(overall * 100));
        svc.SetText((output/100));

        Log.i("charges", "brokeage" + brokeage+" , "+STT+" ' "+transaction+" , "+GST+" , "+SEBI+" , "+stamp +" ' "+overall);
//20+3.45+4.221+0.05+3
    }
}