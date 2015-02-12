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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.graphics.Palette;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
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
        private static final long INTERACTIVE_UPDATE_RATE_MS = 500;
        private static final int NUM_DRAW_CALLS = 5;

        /* a time object */
        private Time time;
        private long timeOffset;

        /* device features */
        private boolean lowBitMode = false;
        private boolean burnProtectionMode = false;
        private boolean registered;

        /* graphic objects */
        private Paint bitmapPaint;
        private Paint indicatorPaint;
        private Paint hourPaint;
        private Paint minutePaint;
        private Paint secondPaint;

        private String timeSeparator = ":";
        private float separatorWidth = 0f;
        private float xOffset = 0f;

        private Bitmap backgroundImage;
        private Palette imagePalette;
        private IntentFilter localIntentFilter;
        private IntentFilter timeZoneIntentFilter;

        /* handler to update the time once a second in interactive mode */
        private Handler timeHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
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
                return true;
            }
        });

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
                        backgroundImage = EventListenerService.getBitmap();
                        EventListenerService.recycleLast();
                        if (WearSettings.useTimePalette()) {

                            Palette.generateAsync(backgroundImage, new Palette.PaletteAsyncListener() {
                                @Override
                                public void onGenerated(Palette palette) {

                                    imagePalette = palette;
                                    if (!isInAmbientMode()) {
                                        syncSettings();
                                    }

                                }
                            });

                        }

                        break;
                    case EventListenerService.LOAD_SETTINGS:
                        syncSettings();
                        break;
                }
            }
        };
        private boolean isDigital;
        private float tickRadius;
        private float hourRadius;
        private float minuteRadius;
        private float secondRadius;
        private float tickWidth;
        private float hourWidth;
        private float minuteWidth;
        private float secondWidth;

        @Override
        public void onCreate(SurfaceHolder holder) {
            /* initialize your watch face */

            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(DigitalWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());

            localIntentFilter = new IntentFilter();
            localIntentFilter.addAction(EventListenerService.LOAD_IMAGE);
            localIntentFilter.addAction(EventListenerService.LOAD_SETTINGS);
            timeZoneIntentFilter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);

            time = new Time();

            bitmapPaint = new Paint();
            bitmapPaint.setAntiAlias(false);
            bitmapPaint.setDither(true);

            indicatorPaint = new Paint();
            indicatorPaint.setStrokeCap(Paint.Cap.BUTT);
            indicatorPaint.setTextAlign(Paint.Align.LEFT);
            indicatorPaint.setShadowLayer(WearSettings.SHADOW_RADIUS,
                    0f,
                    0f,
                    WearSettings.getDigitalSeparatorShadowColor());

            hourPaint = new Paint();
            hourPaint.setStrokeCap(Paint.Cap.BUTT);
            hourPaint.setTextAlign(Paint.Align.LEFT);
            hourPaint.setShadowLayer(WearSettings.SHADOW_RADIUS, 0f, 0f, WearSettings.getDigitalHourShadowColor());

            minutePaint = new Paint();
            minutePaint.setStrokeCap(Paint.Cap.BUTT);
            minutePaint.setTextAlign(Paint.Align.LEFT);
            minutePaint.setShadowLayer(WearSettings.SHADOW_RADIUS, 0f, 0f, WearSettings.getDigitalMinuteShadowColor());

            secondPaint = new Paint();
            secondPaint.setStrokeCap(Paint.Cap.BUTT);
            secondPaint.setTextAlign(Paint.Align.LEFT);
            secondPaint.setShadowLayer(WearSettings.SHADOW_RADIUS,
                    0f,
                    0f,
                    WearSettings.getDigitalSecondShadowColor());

            syncSettings();

            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
            float textSize = displayMetrics.heightPixels / 4;
            float radius = displayMetrics.widthPixels / 2;
            float width = (float) Math.sqrt(Math.pow(radius, 2f) - Math.pow(textSize, 2f)) * 2f;
            float textScale = 1.0f;
            xOffset = radius - width / 2f;

            Log.i(TAG, "textSize: " + textSize);
            Log.i(TAG, "radius: " + radius);
            Log.i(TAG, "width: " + width);

            indicatorPaint.setTextSize(textSize);
            hourPaint.setTextSize(textSize);
            minutePaint.setTextSize(textSize);
            secondPaint.setTextSize(textSize);

            while (getTimeWidth() > width) {
                textScale -= 0.05f;
                indicatorPaint.setTextScaleX(textScale);
                hourPaint.setTextScaleX(textScale);
                minutePaint.setTextScaleX(textScale);
                secondPaint.setTextScaleX(textScale);
                Log.i(TAG, "Time width: " + getTimeWidth());
            }

            separatorWidth = indicatorPaint.measureText(timeSeparator);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);
        }

        private void syncSettings() {
            isDigital = WearSettings.getTimeType().equals(WearSettings.DIGITAL);

            timeOffset = WearSettings.getTimeOffset();
            timeSeparator = WearSettings.getDigitalSeparatorText();

            indicatorPaint.setAntiAlias(true);
            hourPaint.setAntiAlias(true);
            minutePaint.setAntiAlias(true);
            secondPaint.setAntiAlias(true);

            indicatorPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            hourPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            minutePaint.setStyle(Paint.Style.FILL_AND_STROKE);
            secondPaint.setStyle(Paint.Style.FILL_AND_STROKE);

            if (imagePalette != null) {
                indicatorPaint.setColor(imagePalette.getMutedColor(WearSettings.getDigitalSeparatorColor()));

                indicatorPaint.setColor(imagePalette.getVibrantColor(WearSettings.getDigitalSeparatorColor()));
                hourPaint.setColor(imagePalette.getVibrantColor(WearSettings.getDigitalHourColor()));
                minutePaint.setColor(imagePalette.getVibrantColor(WearSettings.getDigitalMinuteColor()));
                secondPaint.setColor(imagePalette.getVibrantColor(WearSettings.getDigitalSecondColor()));

                indicatorPaint.setShadowLayer(WearSettings.SHADOW_RADIUS,
                        0f,
                        0f,
                        imagePalette.getDarkMutedColor(WearSettings.getDigitalSeparatorShadowColor()));
                hourPaint.setShadowLayer(WearSettings.SHADOW_RADIUS, 0f, 0f, imagePalette.getDarkMutedColor(WearSettings.getDigitalHourShadowColor()));
                minutePaint.setShadowLayer(WearSettings.SHADOW_RADIUS, 0f, 0f, imagePalette.getDarkMutedColor(WearSettings.getDigitalMinuteShadowColor()));
                secondPaint.setShadowLayer(WearSettings.SHADOW_RADIUS, 0f, 0f, imagePalette.getDarkMutedColor(WearSettings.getDigitalSecondShadowColor()));
            }
            else {
                indicatorPaint.setColor(WearSettings.getDigitalSeparatorColor());
                hourPaint.setColor(WearSettings.getDigitalHourColor());
                minutePaint.setColor(WearSettings.getDigitalMinuteColor());
                secondPaint.setColor(WearSettings.getDigitalSecondColor());

                indicatorPaint.setShadowLayer(WearSettings.SHADOW_RADIUS,
                        0f,
                        0f,
                        WearSettings.getDigitalSeparatorShadowColor());
                hourPaint.setShadowLayer(WearSettings.SHADOW_RADIUS, 0f, 0f, WearSettings.getDigitalHourShadowColor());
                minutePaint.setShadowLayer(WearSettings.SHADOW_RADIUS, 0f, 0f, WearSettings.getDigitalMinuteShadowColor());
                secondPaint.setShadowLayer(WearSettings.SHADOW_RADIUS, 0f, 0f, WearSettings.getDigitalSecondShadowColor());
            }

            tickRadius = WearSettings.getAnalogTickLength();
            hourRadius = WearSettings.getAnalogHourLength();
            minuteRadius = WearSettings.getAnalogMinuteLength();
            secondRadius = WearSettings.getAnalogSecondLength();

            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();

            tickWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    WearSettings.getAnalogTickWidth(),
                    displayMetrics);
            hourWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    WearSettings.getAnalogHourWidth(),
                    displayMetrics);
            minuteWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    WearSettings.getAnalogMinuteWidth(),
                    displayMetrics);
            secondWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    WearSettings.getAnalogSecondWidth(),
                    displayMetrics);

            invalidate();
        }

        private float getTimeWidth() {

            float width = 0;
            width += hourPaint.measureText("00");
            width += minutePaint.measureText("00");
            width += secondPaint.measureText("00");
            width += indicatorPaint.measureText(timeSeparator);
            width += indicatorPaint.measureText(timeSeparator);

            return width;

        }

        @Override
        public void onDestroy() {
            timeHandler.removeMessages(MSG_UPDATE_TIME);
            unregisterReceivers();
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

//            if (inAmbientMode != wasInAmbientMode) {
                if (isInAmbientMode()) {
                    applyAmbientMode();
                }
                else {
                    syncSettings();
                }
//            }

            invalidate();
            updateTimer();
        }

        private void applyAmbientMode() {
            indicatorPaint.setAntiAlias(!lowBitMode);
            hourPaint.setAntiAlias(!lowBitMode);
            minutePaint.setAntiAlias(!lowBitMode);

            indicatorPaint.setColor(Color.WHITE);
            hourPaint.setColor(Color.WHITE);
            minutePaint.setColor(Color.WHITE);

            indicatorPaint.setShadowLayer(0.0f, 0.0f, 0.0f, Color.TRANSPARENT);
            hourPaint.setShadowLayer(0.0f, 0.0f, 0.0f, Color.TRANSPARENT);
            minutePaint.setShadowLayer(0.0f, 0.0f, 0.0f, Color.TRANSPARENT);

            if (burnProtectionMode) {
                indicatorPaint.setStyle(Paint.Style.STROKE);
                hourPaint.setStyle(Paint.Style.STROKE);
                minutePaint.setStyle(Paint.Style.STROKE);
            }
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            /* draw your watch face */

            time.setToNow();
            time.set(time.toMillis(false) + timeOffset);

            if (isInAmbientMode() || backgroundImage == null) {
                canvas.drawColor(Color.BLACK);
            }
            else {
                canvas.drawBitmap(backgroundImage, 0, 0, bitmapPaint);
            }

            if (isDigital) {
                drawDigital(canvas, bounds);
            }
            else {
                drawAnalog(canvas, bounds);
            }

        }

        private void drawAnalog(Canvas canvas, Rect bounds) {

            float hour = time.hour + time.minute / 60f;
            float minute = time.minute + time.second / 60f;
            float second = time.second;
            float radius = bounds.width() < bounds.height() ? bounds.width() / 2 : bounds.height() / 2;

            float centerX = bounds.exactCenterX();
            float centerY = bounds.exactCenterY();

            // Draw tick marks

            for (int position = 0; position < 12; position++) {
                for (int i = 0; i < NUM_DRAW_CALLS; i++) {
                    canvas.drawLine(
                            (float) (centerX + (radius * tickRadius / 100f) * Math.cos(Math.toRadians(
                                    position * 30f))),
                            (float) (centerY + (radius * tickRadius / 100f) * Math.sin(Math.toRadians(
                                    position * 30f))),
                            (float) (centerX + (radius) * Math.cos(Math.toRadians(position * 30f))),
                            (float) (centerY + (radius) * Math.sin(Math.toRadians(position * 30f))),
                            indicatorPaint);
                }
            }

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
            for (int i = 0; i < NUM_DRAW_CALLS; i++) {
                canvas.drawPath(hourPath, hourPaint);
            }

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
            for (int i = 0; i < NUM_DRAW_CALLS; i++) {
                canvas.drawPath(minutePath, minutePaint);
            }

            if (!isInAmbientMode()) {
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
                for (int i = 0; i < NUM_DRAW_CALLS; i++) {
                    canvas.drawPath(secondPath, secondPaint);
                }
            }
        }

        private void drawDigital(Canvas canvas, Rect bounds) {
            boolean isAmbient = isInAmbientMode();

            // Show colons for the first half of each second so the colons blink on when the time
            // updates.
            boolean drawSeparator = isAmbient || (System.currentTimeMillis() % 1000) < 500;

            // Draw the hours.
            // Each is drawn NUM_DRAW_CALL(5) times to make shadow darker
            float x = xOffset + (time.hour < 10 ?  hourPaint.measureText("0") : 0);
            float hourWidth = hourPaint.measureText("" + time.hour);
            float minuteWidth = minutePaint.measureText(String.format("%02d", time.minute));
            float yOffset = bounds.height() / 2;

            if (drawSeparator) {
                x += hourWidth;
                for (int i = 0; i < NUM_DRAW_CALLS; i++){
                    canvas.drawText(timeSeparator, x, yOffset, indicatorPaint);
                }
                if (!isAmbient) {
                    x += separatorWidth;
                    x += minuteWidth;
                    for (int i = 0; i < NUM_DRAW_CALLS; i++){
                        canvas.drawText(timeSeparator, x, yOffset, indicatorPaint);
                    }
                }
                x = xOffset + (time.hour < 10 ?  hourPaint.measureText("0") : 0);
            }

            for (int i = 0; i < NUM_DRAW_CALLS; i++) {
                canvas.drawText("" + time.hour, x, yOffset, hourPaint);
            }
            x += hourWidth;
            x += separatorWidth;
            for (int i = 0; i < NUM_DRAW_CALLS; i++) {
                canvas.drawText(String.format("%02d", time.minute), x, yOffset, minutePaint);
            }
            if (!isAmbient) {
                x += separatorWidth;
                x += minutePaint.measureText("" + (time.minute / 10));
                x += minutePaint.measureText("" + (time.minute % 10));
                for (int i = 0; i < NUM_DRAW_CALLS; i++) {
                    canvas.drawText(String.format("%02d", time.second), x, yOffset,
                            secondPaint);
                }
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