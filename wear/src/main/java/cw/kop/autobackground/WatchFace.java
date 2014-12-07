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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
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
import java.util.Calendar;
import java.util.Date;

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
    private Calendar calendar;

    private BroadcastReceiver digitalTimeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context arg0, Intent intent) {
            timeText.setText(timeFormat.format(new Date()));
        }
    };
    private BroadcastReceiver analogTimeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context arg0, Intent intent) {

        }
    };
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context arg0, Intent intent) {

            switch (intent.getAction()) {
                case LOAD_IMAGE:
                    faceImage.setImageBitmap(EventListenerService.getBitmap());
                    EventListenerService.recycleLast();
                    break;
                case LOAD_SETTINGS:
                    syncSettings();
                    recreate();
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

        calendar = Calendar.getInstance();

        switch (WearSettings.getTimeType()) {

            default:
            case WearSettings.DIGITAL:
                setContentView(R.layout.watch_face_digital);

                timeText = (TextView) findViewById(R.id.time);
                timeFormat = android.text.format.DateFormat.getTimeFormat(getApplicationContext());
                timeText.setText(timeFormat.format(new Date()));
                registerReceiver(digitalTimeReceiver, timeIntentFilter);
                break;
            case WearSettings.ANALOG:
                setContentView(R.layout.watch_face_analog);

                surfaceView = (SurfaceView) findViewById(R.id.surface_view);
                SurfaceHolder holder = surfaceView.getHolder();
                holder.addCallback(new SurfaceHolder.Callback() {
                    @Override
                    public void surfaceCreated(SurfaceHolder holder) {
                        onDraw();
                    }

                    @Override
                    public void surfaceChanged(SurfaceHolder holder,
                            int format,
                            int width,
                            int height) {

                    }

                    @Override
                    public void surfaceDestroyed(SurfaceHolder holder) {

                    }
                });
                break;

        }

        faceImage = (ImageView) findViewById(R.id.face_image);

        syncSettings();
    }

    @Override
    protected void onResume() {
        super.onResume();
        onScreenAwake();
    }

    @Override
    protected void onPause() {
        onScreenDim();
        super.onPause();
    }

    @Override
    protected void onStop() {
        try {
            LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(
                    broadcastReceiver);
            unregisterReceiver(digitalTimeReceiver);
        }
        catch (IllegalArgumentException e) {

        }
        displayManager.unregisterDisplayListener(this);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void syncSettings() {
        if (WearSettings.getTimeType().equals(WearSettings.DIGITAL)) {
            timeText.setTextColor(WearSettings.getTimeColor());
            timeText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, WearSettings.getTimeSize());
        }
    }

    private void onDraw() {

        canvas = surfaceView.getHolder().lockCanvas();

        if (canvas == null) {
            return;
        }


        surfaceView.getHolder().unlockCanvasAndPost(canvas);

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        Log.i(TAG, "Touched");

        return super.onTouchEvent(event);
    }

    public void onScreenDim() {
        faceImage.setVisibility(View.INVISIBLE);
    }

    public void onScreenAwake() {
        faceImage.setVisibility(View.VISIBLE);
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