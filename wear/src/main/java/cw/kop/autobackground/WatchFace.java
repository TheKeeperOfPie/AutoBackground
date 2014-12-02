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
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;

public class WatchFace extends Activity implements DisplayManager.DisplayListener {

    public static final String LOAD_IMAGE = "cw.kop.autobackground.WatchFace.LOAD_IMAGE";
    private static final String TAG = WatchFace.class.getCanonicalName();
    private DisplayManager displayManager;
    private ImageView faceImage;
    private TextView timeText;
    private DateFormat timeFormat;

    private BroadcastReceiver timeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context arg0, Intent intent) {
            timeText.setText(timeFormat.format(new Date()));
        }
    };
    private BroadcastReceiver imageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context arg0, Intent intent) {
            faceImage.setImageBitmap(EventListenerService.getBitmap());
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        displayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        displayManager.registerDisplayListener(this, null);

        setContentView(R.layout.round_activity_main);

        timeText = (TextView) findViewById(R.id.time);
        faceImage = (ImageView) findViewById(R.id.face_image);

        timeFormat = android.text.format.DateFormat.getTimeFormat(getApplicationContext());

        IntentFilter imageFilter = new IntentFilter();
        imageFilter.addAction(LOAD_IMAGE);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(imageReceiver,
                imageFilter);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_TIME_TICK);
        intentFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        intentFilter.addAction(Intent.ACTION_TIME_CHANGED);

        registerReceiver(timeReceiver, intentFilter);

        timeText.setText(timeFormat.format(new Date()));
    }

    @Override
    protected void onDestroy() {
        try {
            LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(
                    imageReceiver);
            unregisterReceiver(timeReceiver);
        }
        catch (IllegalArgumentException e) {

        }
        displayManager.unregisterDisplayListener(this);
        super.onDestroy();
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