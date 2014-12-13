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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import java.util.TimeZone;

/**
 * Created by TheKeeperOfPie on 12/11/2014.
 */
public class DigitalWatchFace extends CanvasWatchFaceService {

    private static final String TAG = DigitalWatchFace.class.getCanonicalName();

    @Override
    public void onCreate() {
        super.onCreate();
        WearSettings.initPrefs(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
    }

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    /* implement service callback methods */
    private class Engine extends CanvasWatchFaceService.Engine {

        private static final int MSG_UPDATE_TIME = 0;
        private static final long INTERACTIVE_UPDATE_RATE_MS = 1000;

        /* a time object */
        private Time time;
        private long timeOffset;

        /* device features */
        private boolean lowBitMode = false;
        private boolean burnProtectionMode = false;
        private boolean registered;

        private float tickRadius = 0.80f;
        private float hourRadius = 1f;
        private float minuteRadius = 1f;
        private float secondRadius = 1f;
        private float tickWidth = 5f;
        private float hourWidth = 5f;
        private float minuteWidth = 5f;
        private float secondWidth = 5f;

        /* graphic objects */
        private Paint bitmapPaint;
        private Paint tickPaint;
        private Paint hourPaint;
        private Paint minutePaint;
        private Paint secondPaint;
        private Paint hourShadowPaint;
        private Paint minuteShadowPaint;
        private Paint secondShadowPaint;

        private Bitmap tempBackground;
        private IntentFilter localIntentFilter;
        private IntentFilter timeZoneIntentFilter;

        /* handler to update the time once a second in interactive mode */
        final Handler timeHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_UPDATE_TIME:
                        try {
                            invalidate();
                            if (shouldTimerBeRunning()) {
                                long timeMs = System.currentTimeMillis();
                                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                                timeHandler
                                        .sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                            }
                        }
                        catch (NullPointerException e) {
                            Log.e(TAG, "Null pointer");
                        }
                        break;
                }
            }
        };

        /* receiver to update the time zone */
        final BroadcastReceiver timeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                time.clear(intent.getStringExtra("time-zone"));
                time.setToNow();
            }
        };

        private BroadcastReceiver localBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent intent) {

                switch (intent.getAction()) {
                    case EventListenerService.LOAD_IMAGE:
                        tempBackground = EventListenerService.getBitmap();
                        EventListenerService.recycleLast();
                        break;
                    case EventListenerService.LOAD_SETTINGS:
                        syncSettings();
                        break;
                }
            }
        };

        @Override
        public void onCreate(SurfaceHolder holder) {
            /* initialize your watch face */

            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(DigitalWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());

            Resources resources = getResources();
            Drawable backgroundDrawable = resources.getDrawable(R.drawable.app_icon);
            tempBackground = ((BitmapDrawable) backgroundDrawable).getBitmap();

            localIntentFilter = new IntentFilter();
            localIntentFilter.addAction(EventListenerService.LOAD_IMAGE);
            localIntentFilter.addAction(EventListenerService.LOAD_SETTINGS);
            timeZoneIntentFilter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);

            time = new Time();

            bitmapPaint = new Paint();
            bitmapPaint.setAntiAlias(false);
            bitmapPaint.setDither(true);

            tickPaint = new Paint();
            tickPaint.setStrokeCap(Paint.Cap.BUTT);

            hourPaint = new Paint();
            hourPaint.setStrokeCap(Paint.Cap.BUTT);

            minutePaint = new Paint();
            minutePaint.setStrokeCap(Paint.Cap.BUTT);

            secondPaint = new Paint();
            secondPaint.setStrokeCap(Paint.Cap.BUTT);

            hourShadowPaint = new Paint();
            hourShadowPaint.setStrokeCap(Paint.Cap.BUTT);

            minuteShadowPaint = new Paint();
            minuteShadowPaint.setStrokeCap(Paint.Cap.BUTT);

            secondShadowPaint = new Paint();
            secondShadowPaint.setStrokeCap(Paint.Cap.BUTT);

            syncSettings();
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);
        }

        private void syncSettings() {
            timeOffset = WearSettings.getTimeOffset();

            tickPaint.setAntiAlias(true);
            hourPaint.setAntiAlias(true);
            minutePaint.setAntiAlias(true);
            secondPaint.setAntiAlias(true);
            hourShadowPaint.setAntiAlias(true);
            minuteShadowPaint.setAntiAlias(true);
            secondShadowPaint.setAntiAlias(true);

            tickPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            hourPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            minutePaint.setStyle(Paint.Style.FILL_AND_STROKE);
            secondPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            hourShadowPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            minuteShadowPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            secondShadowPaint.setStyle(Paint.Style.FILL_AND_STROKE);

            tickPaint.setColor(WearSettings.getAnalogTickColor());
            hourPaint.setColor(WearSettings.getAnalogHourColor());
            minutePaint.setColor(WearSettings.getAnalogMinuteColor());
            secondPaint.setColor(WearSettings.getAnalogSecondColor());
            hourShadowPaint.setColor(WearSettings.getAnalogHourShadowColor());
            minuteShadowPaint.setColor(WearSettings.getAnalogMinuteShadowColor());
            secondShadowPaint.setColor(WearSettings.getAnalogSecondShadowColor());

            tickRadius = WearSettings.getAnalogTickLength();
            hourRadius = WearSettings.getAnalogHourLength();
            minuteRadius = WearSettings.getAnalogMinuteLength();
            secondRadius = WearSettings.getAnalogSecondLength();

            tickWidth = WearSettings.getAnalogTickWidth();
            hourWidth = WearSettings.getAnalogHourWidth();
            minuteWidth = WearSettings.getAnalogMinuteWidth();
            secondWidth = WearSettings.getAnalogSecondWidth();

            invalidate();
        }

        @Override
        public void onDestroy() {
            timeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onTimeTick() {
            /* the time changed */
            super.onTimeTick();
            invalidate();
        }

        private void updateTimer() {
            timeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                timeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            boolean wasInAmbientMode = isInAmbientMode();
            super.onAmbientModeChanged(inAmbientMode);

            if (inAmbientMode != wasInAmbientMode) {
                if (isInAmbientMode()) {
                    applyAmbientMode();
                }
                else {
                    syncSettings();
                }
            }

            invalidate();
            updateTimer();
        }

        private void applyAmbientMode() {
            tickPaint.setAntiAlias(!lowBitMode);
            hourPaint.setAntiAlias(!lowBitMode);
            minutePaint.setAntiAlias(!lowBitMode);
            hourShadowPaint.setAntiAlias(!lowBitMode);
            minuteShadowPaint.setAntiAlias(!lowBitMode);

            tickPaint.setColor(Color.WHITE);
            hourPaint.setColor(Color.WHITE);
            minutePaint.setColor(Color.WHITE);
            hourShadowPaint.setColor(Color.WHITE);
            minuteShadowPaint.setColor(Color.WHITE);

            tickPaint.setStyle(Paint.Style.STROKE);
            hourPaint.setStyle(Paint.Style.STROKE);
            minutePaint.setStyle(Paint.Style.STROKE);
            hourShadowPaint.setStyle(Paint.Style.STROKE);
            minuteShadowPaint.setStyle(Paint.Style.STROKE);
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            /* draw your watch face */

            boolean isAmbient = isInAmbientMode();

            time.setToNow();
            time.set(time.toMillis(false) + timeOffset);

            if (isAmbient) {
                canvas.drawColor(Color.BLACK);
            }
            else {
                canvas.drawBitmap(tempBackground, 0, 0, bitmapPaint);
            }

            float hour = time.hour + time.minute / 60f;
            float minute = time.minute + time.second / 60f;
            float second = time.second;
            float radius = bounds.width() < bounds.height() ? bounds.width() / 2 : bounds.height() / 2;

            float centerX = bounds.exactCenterX();
            float centerY = bounds.exactCenterY();

            // Draw tick marks

            for (int i = 0; i < 12; i++) {
                canvas.drawLine(
                        (float) (centerX + (radius * tickRadius / 100) * Math.cos(Math.toRadians(i * 30f))),
                        (float) (centerY + (radius * tickRadius / 100) * Math.sin(Math.toRadians(i * 30f))),
                        (float) (centerX + (radius) * Math.cos(Math.toRadians(i * 30f))),
                        (float) (centerY + (radius) * Math.sin(Math.toRadians(i * 30f))),
                        tickPaint);
            }


            // Draw clock hands

            // Draw shadows first to prevent outline overlapping other hands
//            canvas.drawLine(centerX,
//                    centerY,
//                    (float) (centerX + (radius * hourRadius / 100 + 1.0f) * Math.cos(Math.toRadians(hour % 12f * 30f - 90f))),
//                    (float) (centerY + (radius * hourRadius / 100 + 1.0f) * Math.sin(Math.toRadians(hour % 12f * 30f - 90f))),
//                    hourShadowPaint);

            Path hourShadowPath = new Path();
            hourShadowPath.moveTo((float) (centerX + hourWidth / 1.5f * Math.cos(Math.toRadians(hour % 12 * 30f))),
                    (float) (centerY + hourWidth / 1.5f * Math.sin(Math.toRadians(hour % 12 * 30f))));
            hourShadowPath.quadTo(
                    (float) (centerX - (hourWidth / 1.5f) * Math.cos(Math.toRadians(hour % 12 * 30f - 90f))),
                    (float) (centerY - (hourWidth / 1.5f) * Math.sin(Math.toRadians(hour % 12 * 30f - 90f))),
                    (float) (centerX + hourWidth / 1.5f * Math.cos(Math.toRadians(hour % 12 * 30f + 180f))),
                    (float) (centerY + hourWidth / 1.5f * Math.sin(Math.toRadians(hour % 12 * 30f + 180f))));
            hourShadowPath.lineTo((float) (centerX + (radius * hourRadius / 100 + 2.0f) * Math.cos(Math.toRadians(
                            hour % 12 * 30f - 90f))),
                    (float) (centerY + (radius * hourRadius / 100 + 2.0f) * Math.sin(Math.toRadians(hour % 12 * 30f - 90f))));
            hourShadowPath.close();
            canvas.drawPath(hourShadowPath, hourShadowPaint);

//            canvas.drawLine(centerX,
//                    centerY,
//                    (float) (centerX + (radius * minuteRadius / 100 + 1.0f) * Math.cos(Math.toRadians(minute * 6f - 90f))),
//                    (float) (centerY + (radius * minuteRadius / 100 + 1.0f) * Math.sin(Math.toRadians(minute * 6f - 90f))),
//                    minuteShadowPaint);

            Path minuteShadowPath = new Path();
            minuteShadowPath.moveTo((float) (centerX + minuteWidth / 1.5f * Math.cos(Math.toRadians(minute * 6f))),
                    (float) (centerY + minuteWidth / 1.5f * Math.sin(Math.toRadians(minute * 6f))));
            minuteShadowPath.quadTo(
                    (float) (centerX - (minuteWidth / 1.5f) * Math.cos(Math.toRadians(minute * 6f - 90f - 90f))),
                    (float) (centerY - (minuteWidth / 1.5f) * Math.sin(Math.toRadians(minute * 6f - 90f - 90f))),
                    (float) (centerX + minuteWidth / 1.5f * Math.cos(Math.toRadians(minute * 6f - 90f + 180f))),
                    (float) (centerY + minuteWidth / 1.5f * Math.sin(Math.toRadians(minute * 6f - 90f + 180f))));
            minuteShadowPath.lineTo((float) (centerX + (radius * minuteRadius / 100 + 2.0f) * Math.cos(Math.toRadians(
                            minute * 6f - 90f))),
                    (float) (centerY + (radius * minuteRadius / 100 + 2.0f) * Math.sin(Math.toRadians(minute * 6f - 90f))));
            minuteShadowPath.close();
            canvas.drawPath(minuteShadowPath, minuteShadowPaint);

            if (!isAmbient) {
//                canvas.drawLine(centerX,
//                        centerY,
//                        (float) (centerX + (radius * secondRadius / 100 + 1.0f) * Math.cos(Math.toRadians(
//                                second * 6f - 90f))),
//                        (float) (centerY + (radius * secondRadius / 100 + 1.0f) * Math.sin(Math.toRadians(
//                                second * 6f - 90f))),
//                        secondShadowPaint);

                Path secondShadowPath = new Path();
                secondShadowPath.moveTo((float) (centerX + secondWidth / 1.5f * Math.cos(Math.toRadians(minute * 6f))),
                        (float) (centerY + secondWidth / 1.5f * Math.sin(Math.toRadians(second * 6f))));
                secondShadowPath.quadTo(
                        (float) (centerX - (secondWidth / 1.5f) * Math.cos(Math.toRadians(second * 6f - 90f - 90f))),
                        (float) (centerY - (secondWidth / 1.5f) * Math.sin(Math.toRadians(second * 6f - 90f - 90f))),
                        (float) (centerX + secondWidth / 1.5f * Math.cos(Math.toRadians(second * 6f - 90f + 180f))),
                        (float) (centerY + secondWidth / 1.5f * Math.sin(Math.toRadians(second * 6f - 90f + 180f))));
                secondShadowPath.lineTo((float) (centerX + (radius * secondRadius / 100 + 2.0f) * Math.cos(Math.toRadians(
                                second * 6f - 90f))),
                        (float) (centerY + (radius * secondRadius / 100 + 2.0f) * Math.sin(Math.toRadians(second * 6f - 90f))));
                secondShadowPath.close();
                canvas.drawPath(secondShadowPath, secondShadowPaint);
            }

            // Now draw actual hands
            Path hourPath = new Path();
            hourPath.moveTo((float) (centerX + hourWidth / 2f * Math.cos(Math.toRadians(hour % 12 * 30f))),
                    (float) (centerY + hourWidth / 2f * Math.sin(Math.toRadians(hour % 12 * 30f))));
            hourPath.quadTo(
                    (float) (centerX - (hourWidth / 2f) * Math.cos(Math.toRadians(
                            hour % 12 * 30f - 90f))),
                    (float) (centerY - (hourWidth / 2f) * Math.sin(Math.toRadians(hour % 12 * 30f - 90f))),
                    (float) (centerX + hourWidth / 2f * Math.cos(Math.toRadians(hour % 12 * 30f + 180f))),
                    (float) (centerY + hourWidth / 2f * Math.sin(Math.toRadians(hour % 12 * 30f + 180f))));
            hourPath.lineTo((float) (centerX + (radius * hourRadius / 100) * Math.cos(Math.toRadians(
                    hour % 12 * 30f - 90f))),
                    (float) (centerY + (radius * hourRadius / 100) * Math.sin(Math.toRadians(hour % 12 * 30f - 90f))));
            hourPath.close();
            canvas.drawPath(hourPath, hourPaint);

            Path minutePath = new Path();
            minutePath.moveTo((float) (centerX + minuteWidth / 2f * Math.cos(Math.toRadians(minute * 6f))),
                    (float) (centerY + minuteWidth / 2f * Math.sin(Math.toRadians(minute * 6f))));
            minutePath.quadTo(
                    (float) (centerX - (minuteWidth / 2) * Math.cos(Math.toRadians(
                            minute * 6f - 90f))),
                    (float) (centerY + (minuteWidth / 2) * Math.sin(Math.toRadians(
                            minute * 6f - 90f))),
                    (float) (centerX + (minuteWidth / 2f) * Math.cos(Math.toRadians(minute * 6f + 180f))),
                    (float) (centerY + (minuteWidth / 2f) * Math.sin(Math.toRadians(minute * 6f + 180f))));
            minutePath.lineTo((float) (centerX + (radius * minuteRadius / 100) * Math.cos(Math.toRadians(
                            minute * 6f - 90f))),
                    (float) (centerY + (radius * minuteRadius / 100) * Math.sin(Math.toRadians(
                            minute * 6f - 90f))));
            minutePath.close();
            canvas.drawPath(minutePath, minutePaint);

            if (!isAmbient) {
                Path secondPath = new Path();
                secondPath.moveTo((float) (centerX + secondWidth / 2f * Math.cos(Math.toRadians(second * 6f))),
                        (float) (centerY + secondWidth / 2f * Math.sin(Math.toRadians(second * 6f))));
                secondPath.quadTo(
                        (float) (centerX - (secondWidth / 2) * Math.cos(Math.toRadians(second * 6f - 90f))),
                        (float) (centerY - (secondWidth / 2) * Math.sin(Math.toRadians(second * 6f - 90f))),
                        (float) (centerX + (secondWidth / 2f) * Math.cos(Math.toRadians(second * 6f + 180f))),
                        (float) (centerY + (secondWidth / 2f) * Math.sin(Math.toRadians(second * 6f + 180f))));
                secondPath.lineTo(
                        (float) (centerX + (radius * secondRadius / 100) * Math.cos(Math.toRadians(
                                second * 6f - 90f))),
                        (float) (centerY + (radius * secondRadius / 100) * Math.sin(Math.toRadians(
                                second * 6f - 90f))));
                secondPath.close();
                canvas.drawPath(secondPath, secondPaint);
            }

        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            /* the watch face became visible or invisible */
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceivers();

                // Update time zone in case it changed while we weren't visible.
                time.clear(TimeZone.getDefault().getID());
                time.setToNow();
            } else {
                unregisterReceivers();
            }
            updateTimer();
        }

        private void registerReceivers() {
            if (registered) {
                return;
            }
            LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                    localBroadcastReceiver,
                    localIntentFilter);
            registerReceiver(timeZoneReceiver, timeZoneIntentFilter);
            registered = true;
        }

        private void unregisterReceivers() {
            if (!registered) {
                return;
            }
            LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(
                    localBroadcastReceiver);
            unregisterReceiver(timeZoneReceiver);
            registered = false;
        }

        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            lowBitMode = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            burnProtectionMode = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION,
                    false);
        }

    }

}