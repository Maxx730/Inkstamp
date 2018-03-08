package com.kinghorn.inkstamp.inkstamp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Xfermode;
import android.media.Image;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

public class InkStampActivity extends AppCompatActivity {
    private ImageButton fade_toggle,rotate_toggle,zoom_toggle,activity_check,activity_cancel,layer_up,layer_down,zoom_in,zoom_out;
    private LinearLayout fade_seekbar,rotate_seekbar,zoom_seekbar;
    private RelativeLayout stage;
    private SeekBar fade,rotate;
    private int SWAP_CANCEL = 1,CURRENT_LAYER = 2,BACKGROUND_ROTATION = 0,FOREGROUND_ROTATION = 0;
    private float BACKGROUND_SCALE = 1,FOREGROUND_SCALE = 1,SEEKBAR_BSCALE,SEEKBAR_FSCALE,FADE_RADIUS = 200f;
    private View inkCanvas;
    private Bitmap foreground_img,background_img;
    private Boolean DEBUG = true;
    private TextView zoom_text,layer_text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ink_stamp);

        Intent i = new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, 1);

        InitializeStage();
        InitializeToggleButtons();
        InitializeSeekbarActions();
        InitializeClickEvents();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == Activity.RESULT_OK){
            if(requestCode == 1){
                try {
                    foreground_img = MediaStore.Images.Media.getBitmap(getContentResolver(),(Uri) data.getData());
                } catch (IOException e) {
                    Toast.makeText(getApplicationContext(),"There has been an error opening file...",Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void InitializeStage(){
        stage = (RelativeLayout) findViewById(R.id.InkStampStage);
        inkCanvas = new InkStampCanvas(getApplicationContext());
        stage.addView(inkCanvas);

        inkCanvas.invalidate();
    }

    //Adds click events for the bottom toggle buttons so that when each
    //are clicked on it shows the correct tool in the above layout.
    private void InitializeToggleButtons(){
        //Grab the image buttons.
        fade_toggle = (ImageButton) findViewById(R.id.feather_toggle);
        rotate_toggle = (ImageButton) findViewById(R.id.rotate_toggle);
        zoom_toggle = (ImageButton) findViewById(R.id.zoom_toggle);
        activity_check = (ImageButton) findViewById(R.id.activityConfirm);
        activity_cancel = (ImageButton) findViewById(R.id.activityCancel);
        layer_text = (TextView) findViewById(R.id.LayerIndicator);

        //Grab layer buttons.
        layer_up = (ImageButton) findViewById(R.id.upperLayer);
        layer_down = (ImageButton) findViewById(R.id.lowerLayer);

        //Grab the layouts.
        fade_seekbar = (LinearLayout) findViewById(R.id.fade_layout);
        rotate_seekbar = (LinearLayout) findViewById(R.id.rotate_layout);
        zoom_seekbar = (LinearLayout) findViewById(R.id.zoom_layout);

        activity_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        fade_toggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rotate_seekbar.setVisibility(View.GONE);
                zoom_seekbar.setVisibility(View.GONE);
                fade_seekbar.setVisibility(View.VISIBLE);
            }
        });

        zoom_toggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rotate_seekbar.setVisibility(View.GONE);
                zoom_seekbar.setVisibility(View.VISIBLE);
                fade_seekbar.setVisibility(View.GONE);
            }
        });

        rotate_toggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rotate_seekbar.setVisibility(View.VISIBLE);
                zoom_seekbar.setVisibility(View.GONE);
                fade_seekbar.setVisibility(View.GONE);
            }
        });

        //Layer click events.
        layer_up.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                layer_down.setVisibility(View.VISIBLE);
                layer_up.setVisibility(View.GONE);
                CURRENT_LAYER = 2;
                zoom_text.setText("ZOOM "+FOREGROUND_SCALE);
                layer_text.setText("FOREGROUND");
                inkCanvas.invalidate();
                rotate.setEnabled(true);
                fade.setEnabled(true);
                rotate.setProgress(FOREGROUND_ROTATION);
            }
        });

        layer_down.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                layer_down.setVisibility(View.GONE);
                layer_up.setVisibility(View.VISIBLE);
                CURRENT_LAYER = 1;
                zoom_text.setText("ZOOM "+BACKGROUND_SCALE);
                layer_text.setText("BACKGROUND");
                inkCanvas.invalidate();
                rotate.setEnabled(false);
                fade.setEnabled(false);
                rotate.setProgress(BACKGROUND_ROTATION);
            }
        });
    }

    //Grabs the actual seekbars and sets the actions associated with them
    //based on the tool they represent.
    private void InitializeSeekbarActions(){
        rotate = (SeekBar) findViewById(R.id.RotateScalebar);
        fade = (SeekBar) findViewById(R.id.FeatherScalebar);

        rotate.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if(CURRENT_LAYER == 2){
                    FOREGROUND_ROTATION = i;
                }else{
                    BACKGROUND_ROTATION = i;
                }

                inkCanvas.invalidate();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        fade.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                FADE_RADIUS = i;
                inkCanvas.invalidate();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void InitializeClickEvents(){
        zoom_in = (ImageButton) findViewById(R.id.ZoomInBtn);
        zoom_out = (ImageButton) findViewById(R.id.ZoomOutBtn);
        zoom_text = (TextView) findViewById(R.id.ZoomIndication);

        zoom_in.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(CURRENT_LAYER == 2){
                    FOREGROUND_SCALE += .25f;
                    zoom_text.setText("SCALE "+FOREGROUND_SCALE);
                }else{
                    BACKGROUND_SCALE += .25f;
                    zoom_text.setText("SCALE "+BACKGROUND_SCALE);
                }
                inkCanvas.invalidate();
            }
        });

        zoom_out.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(CURRENT_LAYER == 2){
                    if(FOREGROUND_SCALE > .25f){
                        FOREGROUND_SCALE -= .25f;
                    }

                    zoom_text.setText("SCALE "+FOREGROUND_SCALE);
                }else{
                    if(BACKGROUND_SCALE > .25F){
                        BACKGROUND_SCALE -= .25f;
                    }

                    zoom_text.setText("SCALE "+BACKGROUND_SCALE);
                }

                inkCanvas.invalidate();
            }
        });
    }

    //Canvas class that will be used to draw the images to the canvas.
    private class InkStampCanvas extends View{

        private float posx,posy = 0;
        private Paint p,b;

        public InkStampCanvas(Context context) {
            super(context);

            b = new Paint();

            p = new Paint();
            p.setColor(Color.WHITE);
            p.setStyle(Paint.Style.FILL);
            p.setStrokeWidth(10);
            p.setTextSize(60);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            Bitmap scaledBackground = ScaleLayerImage(foreground_img,1);
            //Draw the background image.
            canvas.drawBitmap(RotateImage(scaledBackground,1),(getWidth() - RotateImage(scaledBackground,1).getWidth()) / 2,(getHeight() - RotateImage(scaledBackground,1).getHeight())/2,null);

            if(CURRENT_LAYER == 1){
                b.setAlpha(100);
            }else{
                b.setAlpha(255);
            }

            Bitmap rotated = RotateImage(foreground_img,2);
            //Draw the foreground image.
            canvas.drawBitmap(ApplyFeathering(rotated),posx - (rotated.getWidth()/2),posy - (rotated.getHeight()/2),b);
            if(DEBUG){
                canvas.drawText("Position - X: "+posx+" Y: "+posy,30,280,p);

                if(CURRENT_LAYER == 2){
                    canvas.drawText("Layer: Foreground",30,380,p);
                }else{
                    canvas.drawText("Layer: Background",30,380,p);
                }

                if(CURRENT_LAYER == 2){
                    canvas.drawText("Foreground Scale: "+FOREGROUND_SCALE,30,480,p);
                    canvas.drawText("Foreground Rotation: "+FOREGROUND_ROTATION,30,580,p);
                }else{
                    canvas.drawText("Background Scale: "+BACKGROUND_SCALE,30,480,p);
                    canvas.drawText("Background Rotation: "+BACKGROUND_ROTATION,30,580,p);
                }
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            switch(event.getAction()){
                case MotionEvent.ACTION_DOWN:
                    posx = event.getX();
                    posy = event.getY();
                    break;

                case MotionEvent.ACTION_UP:
                    break;

                case MotionEvent.ACTION_MOVE:
                    posx = event.getX();
                    posy = event.getY();
                    break;
            }

            invalidate();
            return true;
        }

        private Bitmap ScaleBackgroundLayer(Bitmap b){
            Matrix m = new Matrix();
            float scale;

            if(foreground_img.getWidth() > getWidth()){
                scale = foreground_img.getWidth() / getWidth();
            }else{
                scale = getWidth() / foreground_img.getWidth();
            }

            m.setScale(scale,scale);
            return Bitmap.createBitmap(b,0,0,b.getWidth(),b.getHeight(),m,true);
        }

        //Takes in and image the layer the image is on and applys the rotation for the given layer.
        private Bitmap RotateImage(Bitmap b,int lay){
            Matrix m = new Matrix();
            Bitmap fin;

            if(lay == 2){
                m.postRotate(FOREGROUND_ROTATION,b.getWidth() / 2,b.getHeight() / 2);
            }else{
                m.postRotate(BACKGROUND_ROTATION,b.getWidth() / 2,b.getHeight() / 2);
            }

            fin = Bitmap.createBitmap(b,0,0,b.getWidth(),b.getHeight(),m,true);
            fin.setHasAlpha(true);
            return ScaleLayerImage(fin,lay);
        }

        private Bitmap ScaleLayerImage(Bitmap b,int lay){
            Matrix m = new Matrix();
            Bitmap fin;

            if(lay == 2){
                m.setScale(FOREGROUND_SCALE,FOREGROUND_SCALE);
            }else{
                m.setScale(BACKGROUND_SCALE,BACKGROUND_SCALE);
            }

            fin = Bitmap.createBitmap(b,0,0,b.getWidth(),b.getHeight(),m,true);
            return fin;
        }

        private Bitmap ApplyFeathering(Bitmap b){
            Bitmap bcan = Bitmap.createBitmap(b.getWidth(),b.getHeight(),Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bcan);
            float[] fadeVals = {.6f,.8f,1};
            int[] colors = {Color.BLACK,Color.parseColor("#66000000"),Color.TRANSPARENT};
            RadialGradient r = new RadialGradient((float) b.getWidth() / 2,(float) b.getHeight() / 2,FADE_RADIUS * FOREGROUND_SCALE,colors,fadeVals, Shader.TileMode.CLAMP);

            Paint p = new Paint();
            p.setShader(r);
            c.drawRoundRect(new RectF(0,0,b.getWidth(),b.getHeight()),0,50,p);
            p.setAntiAlias(true);
            p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            c.drawBitmap(b,0,0,p);

            return bcan;
        }

    }
}
