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
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
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

        /* a time object */
        private Time time;
        private long timeOffset;

        /* device features */
        private boolean lowBitMode = false;
        private boolean burnProtectionMode = false;
        private boolean registered;

        /* graphic objects */
        private Paint bitmapPaint;
        private Paint separatorPaint;
        private Paint hourPaint;
        private Paint minutePaint;
        private Paint secondPaint;
        private Paint separatorShadowPaint;
        private Paint hourShadowPaint;
        private Paint minuteShadowPaint;
        private Paint secondShadowPaint;

        private String timeSeparator = ":";
        private float separatorWidth = 0f;
        private float xOffset = 0f;

        private Bitmap backgroundImage;
        private Palette imagePalette;
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

            separatorPaint = new Paint();
            separatorPaint.setStrokeCap(Paint.Cap.BUTT);
            separatorPaint.setTextAlign(Paint.Align.LEFT);

            hourPaint = new Paint();
            hourPaint.setStrokeCap(Paint.Cap.BUTT);
            hourPaint.setTextAlign(Paint.Align.LEFT);

            minutePaint = new Paint();
            minutePaint.setStrokeCap(Paint.Cap.BUTT);
            minutePaint.setTextAlign(Paint.Align.LEFT);

            secondPaint = new Paint();
            secondPaint.setStrokeCap(Paint.Cap.BUTT);
            secondPaint.setTextAlign(Paint.Align.LEFT);

            separatorShadowPaint = new Paint();
            separatorShadowPaint.setStrokeCap(Paint.Cap.BUTT);
            separatorShadowPaint.setTextAlign(Paint.Align.LEFT);

            hourShadowPaint = new Paint();
            hourShadowPaint.setStrokeCap(Paint.Cap.BUTT);
            hourShadowPaint.setTextAlign(Paint.Align.LEFT);

            minuteShadowPaint = new Paint();
            minuteShadowPaint.setStrokeCap(Paint.Cap.BUTT);
            minuteShadowPaint.setTextAlign(Paint.Align.LEFT);

            secondShadowPaint = new Paint();
            secondShadowPaint.setStrokeCap(Paint.Cap.BUTT);
            secondShadowPaint.setTextAlign(Paint.Align.LEFT);

            syncSettings();
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);
        }

        private void syncSettings() {
            timeOffset = WearSettings.getTimeOffset();
            timeSeparator = WearSettings.getDigitalSeparatorText();

            separatorPaint.setAntiAlias(true);
            hourPaint.setAntiAlias(true);
            minutePaint.setAntiAlias(true);
            secondPaint.setAntiAlias(true);
            separatorShadowPaint.setAntiAlias(true);
            hourShadowPaint.setAntiAlias(true);
            minuteShadowPaint.setAntiAlias(true);
            secondShadowPaint.setAntiAlias(true);

            separatorPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            hourPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            minutePaint.setStyle(Paint.Style.FILL_AND_STROKE);
            secondPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            separatorShadowPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            hourShadowPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            minuteShadowPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            secondShadowPaint.setStyle(Paint.Style.FILL_AND_STROKE);

            if (imagePalette != null) {
                separatorPaint.setColor(imagePalette.getMutedColor(WearSettings.getDigitalSeparatorColor()));

                separatorPaint.setColor(imagePalette.getVibrantColor(WearSettings.getDigitalSeparatorColor()));
                hourPaint.setColor(imagePalette.getVibrantColor(WearSettings.getDigitalHourColor()));
                minutePaint.setColor(imagePalette.getVibrantColor(WearSettings.getDigitalMinuteColor()));
                secondPaint.setColor(imagePalette.getVibrantColor(WearSettings.getDigitalSecondColor()));

                separatorShadowPaint.setColor(imagePalette.getDarkVibrantColor(WearSettings.getDigitalSeparatorShadowColor()));
                hourShadowPaint.setColor(imagePalette.getDarkVibrantColor(WearSettings.getDigitalHourShadowColor()));
                minuteShadowPaint.setColor(imagePalette.getDarkVibrantColor(WearSettings.getDigitalMinuteShadowColor()));
                secondShadowPaint.setColor(imagePalette.getDarkVibrantColor(WearSettings.getDigitalSecondShadowColor()));
            }
            else {
                separatorPaint.setColor(WearSettings.getDigitalSeparatorColor());
                hourPaint.setColor(WearSettings.getDigitalHourColor());
                minutePaint.setColor(WearSettings.getDigitalMinuteColor());
                secondPaint.setColor(WearSettings.getDigitalSecondColor());

                separatorShadowPaint.setColor(WearSettings.getDigitalSeparatorShadowColor());
                hourShadowPaint.setColor(WearSettings.getDigitalHourShadowColor());
                minuteShadowPaint.setColor(WearSettings.getDigitalMinuteShadowColor());
                secondShadowPaint.setColor(WearSettings.getDigitalSecondShadowColor());
            }

            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
            float textSize = displayMetrics.heightPixels / 4;
            float radius = displayMetrics.widthPixels / 2;
            float width = (float) Math.sqrt(Math.pow(radius, 2f) - Math.pow(textSize, 2f)) * 2f;
            float textScale = 1.0f;
            xOffset = radius - width / 2f;

            Log.i(TAG, "textSize: " + textSize);
            Log.i(TAG, "radius: " + radius);
            Log.i(TAG, "width: " + width);

            separatorPaint.setTextSize(textSize);
            hourPaint.setTextSize(textSize);
            minutePaint.setTextSize(textSize);
            secondPaint.setTextSize(textSize);
            separatorShadowPaint.setTextSize(textSize + 4f);
            hourShadowPaint.setTextSize(textSize + 4f);
            minuteShadowPaint.setTextSize(textSize + 4f);
            secondShadowPaint.setTextSize(textSize + 4f);

            while (getTimeWidth() > width) {
                textScale -= 0.05f;
                separatorPaint.setTextScaleX(textScale);
                hourPaint.setTextScaleX(textScale);
                minutePaint.setTextScaleX(textScale);
                secondPaint.setTextScaleX(textScale);
                Log.i(TAG, "Time width: " + getTimeWidth());
            }
            separatorShadowPaint.setTextScaleX(textScale);
            hourShadowPaint.setTextScaleX(textScale);
            minuteShadowPaint.setTextScaleX(textScale);
            secondShadowPaint.setTextScaleX(textScale);

            separatorWidth = separatorPaint.measureText(timeSeparator);

            invalidate();
        }

        private int getContrastingColor(int color) {

            float[] hsv = new float[3];

            hsv[2] = hsv[2] > 0.5f ? 0.2f : 0.8f;

            return Color.HSVToColor(hsv);
        }

        private float getTimeWidth() {

            float width = 0;
            width += hourPaint.measureText("00");
            width += minutePaint.measureText("00");
            width += secondPaint.measureText("00");
            width += separatorPaint.measureText(timeSeparator);
            width += separatorPaint.measureText(timeSeparator);

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
            separatorPaint.setAntiAlias(!lowBitMode);
            hourPaint.setAntiAlias(!lowBitMode);
            minutePaint.setAntiAlias(!lowBitMode);

            separatorPaint.setColor(Color.WHITE);
            hourPaint.setColor(Color.WHITE);
            minutePaint.setColor(Color.WHITE);

            separatorPaint.setShadowLayer(0.0f, 0.0f, 0.0f, Color.TRANSPARENT);
            hourPaint.setShadowLayer(0.0f, 0.0f, 0.0f, Color.TRANSPARENT);
            minutePaint.setShadowLayer(0.0f, 0.0f, 0.0f, Color.TRANSPARENT);

            if (burnProtectionMode) {
                separatorPaint.setStyle(Paint.Style.STROKE);
                hourPaint.setStyle(Paint.Style.STROKE);
                minutePaint.setStyle(Paint.Style.STROKE);
            }
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            /* draw your watch face */

            boolean isAmbient = isInAmbientMode();

            time.setToNow();
            time.set(time.toMillis(false) + timeOffset);

            if (isAmbient || backgroundImage == null) {
                canvas.drawColor(Color.BLACK);
            }
            else {
                canvas.drawBitmap(backgroundImage, 0, 0, bitmapPaint);
            }

            // Show colons for the first half of each second so the colons blink on when the time
            // updates.
            boolean drawSeparator = isAmbient || (System.currentTimeMillis() % 1000) < 500;

            // Draw the hours.
            float x = xOffset + (time.hour < 10 ?  hourPaint.measureText("0") : 0);
            float hourWidth = hourPaint.measureText("" + time.hour);
            float minuteWidth = minutePaint.measureText(String.format("%02d", time.minute));
            float yOffset = bounds.height() / 2;


            if (!isAmbient) {
                if (time.hour > 9) {
                    canvas.drawText("" + (time.hour / 10), x - 3.0f, yOffset + 2.0f, hourShadowPaint);
                    x += hourPaint.measureText("" + (time.hour / 10));
                }
                canvas.drawText("" + (time.hour % 10), x - 3.0f, yOffset + 2.0f, hourShadowPaint);
                x += hourPaint.measureText("" + (time.hour % 10));
                if (drawSeparator) {
                    canvas.drawText(timeSeparator, x - 3.0f, yOffset + 2.0f, separatorShadowPaint);
                }
                x += separatorWidth;


                canvas.drawText("" + (time.minute / 10), x - 3.0f, yOffset + 2.0f, minuteShadowPaint);
                x += minutePaint.measureText("" + (time.minute / 10));
                canvas.drawText("" + (time.minute % 10), x - 3.0f, yOffset + 2.0f, minuteShadowPaint);
                x += minutePaint.measureText("" + (time.minute % 10));

                if (drawSeparator) {
                    canvas.drawText(timeSeparator, x - 3.0f, yOffset + 2.0f, separatorShadowPaint);
                }
                x += separatorWidth;
                canvas.drawText("" + (time.second / 10), x - 3.0f, yOffset + 2.0f, secondShadowPaint);
                x += secondPaint.measureText("" + (time.second / 10));
                canvas.drawText("" + (time.second % 10), x - 3.0f, yOffset + 2.0f, secondShadowPaint);
                x -= secondPaint.measureText("" + (time.second / 10));
                canvas.drawText(String.format("%02d", time.second), x, yOffset,
                        secondPaint);
            }

            if (drawSeparator) {
                x = xOffset + (time.hour < 10 ?  hourPaint.measureText("0") : 0);
                x += hourWidth;
                canvas.drawText(timeSeparator, x, yOffset, separatorPaint);
                if (!isAmbient) {
                    x += separatorWidth;
                    x += minuteWidth;
                    canvas.drawText(timeSeparator, x, yOffset, separatorPaint);
                }
            }

            x = xOffset + (time.hour < 10 ?  hourPaint.measureText("0") : 0);
            canvas.drawText("" + time.hour, x, yOffset, hourPaint);
            x += hourWidth;
            x += separatorWidth;
            canvas.drawText(String.format("%02d", time.minute), x, yOffset, minutePaint);

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