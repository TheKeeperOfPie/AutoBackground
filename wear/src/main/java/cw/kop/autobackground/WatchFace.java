/*
 * Copyright (C) Winson Chiu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cw.kop.autobackground;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.Time;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.AnalogClock;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;
import java.util.Random;

public class WatchFace extends Activity implements DisplayManager.DisplayListener {

    public static final String LOAD_IMAGE = "cw.kop.autobackground.WatchFace.LOAD_IMAGE";
    public static final String LOAD_SETTINGS = "cw.kop.autobackground.WatchFace.LOAD_SETTINGS";
    private static final String TAG = WatchFace.class.getCanonicalName();
    private DisplayManager displayManager;
    private ImageView faceImage;
    private TextView timeText;
    private DateFormat timeFormat;
    private AnalogClock timeAnalog;
    private Canvas canvas;
    private SurfaceView surfaceView;
    private float centerX;
    private float centerY;
    private boolean isAnalog = false;
    private Handler handler;
    private boolean isAwake = false;
    private Random random;
    private Time time;

    private int hourColor;
    private int hourShadowColor;
    private int minuteColor;
    private int minuteShadowColor;
    private int secondColor;
    private int secondShadowColor;
    private int hourOffset;
    private int minuteOffset;
    private int secondOffset;

    private BroadcastReceiver digitalTimeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context arg0, Intent intent) {
            timeText.setText(timeFormat.format(new Date()));
        }
    };
    private BroadcastReceiver analogTimeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context arg0, Intent intent) {
            onDraw();
        }
    };
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context arg0, Intent intent) {

            switch (intent.getAction()) {
                case LOAD_IMAGE:
                    faceImage.setImageBitmap(EventListenerService.getBitmap());
                    Log.i(TAG, "faceImage bitmap set");
                    EventListenerService.recycleLast();
                    break;
                case LOAD_SETTINGS:
                    syncSettings();
                    if (isAnalog ^ WearSettings.getTimeType().equals(WearSettings.ANALOG)) {
                        recreate();
                    }
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        WearSettings.initPrefs(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));

        super.onCreate(savedInstanceState);

        displayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        displayManager.registerDisplayListener(this, null);

        centerX = getResources().getDisplayMetrics().widthPixels / 2;
        centerY = getResources().getDisplayMetrics().heightPixels / 2;

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LOAD_IMAGE);
        intentFilter.addAction(LOAD_SETTINGS);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                broadcastReceiver,
                intentFilter);

        IntentFilter timeIntentFilter = new IntentFilter();
        timeIntentFilter.addAction(Intent.ACTION_TIME_TICK);
        timeIntentFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        timeIntentFilter.addAction(Intent.ACTION_TIME_CHANGED);

        random = new Random();
        handler = new Handler(getMainLooper());
        time = new Time();
        setClock();

        switch (WearSettings.getTimeType()) {

            default:
            case WearSettings.DIGITAL:
                setContentView(R.layout.watch_face_digital);
                timeText = (TextView) findViewById(R.id.time_digital);
                timeFormat = android.text.format.DateFormat.getTimeFormat(getApplicationContext());
                timeText.setText(timeFormat.format(new Date()));
                registerReceiver(digitalTimeReceiver, timeIntentFilter);
                break;
            case WearSettings.ANALOG:
                setContentView(R.layout.watch_face_analog);

                surfaceView = (SurfaceView) findViewById(R.id.surface_view);
                surfaceView.setZOrderOnTop(true);
                SurfaceHolder holder = surfaceView.getHolder();
                holder.setFormat(PixelFormat.TRANSPARENT);
                isAnalog = true;
                registerReceiver(analogTimeReceiver, timeIntentFilter);
                break;

        }

        faceImage = (ImageView) findViewById(R.id.face_image);
        syncSettings();
    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        try {
            LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(
                    broadcastReceiver);
            unregisterReceiver(digitalTimeReceiver);
        }
        catch (IllegalArgumentException e) {

        }
        displayManager.unregisterDisplayListener(this);
        super.onDestroy();
    }

    private void syncSettings() {
        if (WearSettings.getTimeType().equals(WearSettings.DIGITAL)) {
            timeText.setTextColor(WearSettings.getTimeColor());
            timeText.setShadowLayer(5.0f, -1f, -1f, WearSettings.getTimeShadowColor());
            timeText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, WearSettings.getTimeSize());
        }
        else {
            setClock();
            onDraw();
        }
    }

    private void setClock() {
        hourColor = WearSettings.getAnalogHourColor();
        hourShadowColor = WearSettings.getAnalogHourShadowColor();
        minuteColor = WearSettings.getAnalogMinuteColor();
        minuteShadowColor = WearSettings.getAnalogMinuteShadowColor();
        secondColor = WearSettings.getAnalogSecondColor();
        secondShadowColor = WearSettings.getAnalogSecondShadowColor();
    }

    private void onDraw() {

        Log.i(TAG, "Drawing...");

        canvas = surfaceView.getHolder().lockCanvas();

        if (canvas == null) {
            return;
        }

        time.setToNow();

        float hour = time.hour + time.minute / 60;
        float minute = time.minute + time.second / 60;
        float second = time.second;

        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        Paint paint = new Paint();
        paint.setFlags(Paint.ANTI_ALIAS_FLAG);

        paint.setColor(hourShadowColor);
        paint.setStrokeWidth(7.0f);
        canvas.drawLine(centerX,
                centerY,
                (float) (centerX + centerX / 2 * Math.cos(Math.toRadians(hour % 12f * 30f - 90f))),
                (float) (centerY + centerY / 2 * Math.sin(Math.toRadians(hour % 12f * 30f - 90f))),
                paint);
        paint.setColor(hourColor);
        paint.setStrokeWidth(5.0f);
        canvas.drawLine(centerX,
                centerY,
                (float) (centerX + centerX / 2 * Math.cos(Math.toRadians(hour % 12f * 30f - 90f))),
                (float) (centerY + centerY / 2 * Math.sin(Math.toRadians(hour % 12f * 30f - 90f))),
                paint);

        paint.setColor(minuteShadowColor);
        paint.setStrokeWidth(5.0f);
        canvas.drawLine(centerX,
                centerY,
                (float) (centerX + centerX / 1.5 * Math.cos(Math.toRadians(minute * 6f - 90f))),
                (float) (centerY + centerY / 1.5 * Math.sin(Math.toRadians(minute * 6f - 90f))),
                paint);
        paint.setColor(minuteColor);
        paint.setStrokeWidth(3.0f);
        canvas.drawLine(centerX,
                centerY,
                (float) (centerX + centerX / 1.5 * Math.cos(Math.toRadians(minute * 6f - 90f))),
                (float) (centerY + centerY / 1.5 * Math.sin(Math.toRadians(minute * 6f - 90f))),
                paint);

        if (isAwake) {
            paint.setColor(secondShadowColor);
            paint.setStrokeWidth(3.0f);
            canvas.drawLine(centerX,
                    centerY,
                    (float) (centerX + centerX * Math.cos(Math.toRadians(second * 6f - 90f))),
                    (float) (centerY + centerY * Math.sin(Math.toRadians(second * 6f - 90f))),
                    paint);
            paint.setColor(secondColor);
            paint.setStrokeWidth(2.0f);
            canvas.drawLine(centerX,
                    centerY,
                    (float) (centerX + centerX * Math.cos(Math.toRadians(second * 6f - 90f))),
                    (float) (centerY + centerY * Math.sin(Math.toRadians(second * 6f - 90f))),
                    paint);
        }
        surfaceView.getHolder().unlockCanvasAndPost(canvas);

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        Log.i(TAG, "Touched");

        return super.onTouchEvent(event);
    }

    private RefreshTimeRunnable mRefreshTimeRunnable;

    public void onScreenDim() {
        isAwake = false;
        if (faceImage != null) {
            faceImage.setVisibility(View.INVISIBLE);
        }
        if (mRefreshTimeRunnable != null) {
            mRefreshTimeRunnable.stopRefresh();
            mRefreshTimeRunnable = null;
        }
        onDraw();
    }

    public void onScreenAwake() {
        isAwake = true;
        if (faceImage != null) {
            faceImage.setVisibility(View.VISIBLE);
        }
        if (isAnalog) {
            if (mRefreshTimeRunnable != null) {
                mRefreshTimeRunnable.stopRefresh();
            }
            mRefreshTimeRunnable = new RefreshTimeRunnable();
            runOnUiThread(mRefreshTimeRunnable);
        }
    }

    private class RefreshTimeRunnable implements Runnable{

        private boolean stopRefresh = false;

        public void stopRefresh(){
            stopRefresh = true;
            Log.i(TAG, "stopRefresh set to true");
        }

        @Override
        public void run() {
            if (!stopRefresh) {
                onDraw();

                Log.i(TAG, "onDraw called");
                handler.postDelayed(this, 1000);
            }
        }
    }

    @Override
    public void onDisplayAdded(int displayId) {

    }

    @Override
    public void onDisplayRemoved(int displayId) {

    }

    @Override
    public void onDisplayChanged(int displayId) {
        switch (displayManager.getDisplay(displayId).getState()) {
            case Display.STATE_DOZE:
                onScreenDim();
                break;
            default:
                onScreenAwake();
                break;
        }
    }
}