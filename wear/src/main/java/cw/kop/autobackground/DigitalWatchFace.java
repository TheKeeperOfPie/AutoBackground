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
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
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
        private static final String TIME_SEPARATOR = ":";

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
        private Paint hourShadowPaint;
        private Paint minuteShadowPaint;
        private Paint secondShadowPaint;

        private float separatorWidth = 0f;

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

            separatorPaint.setAntiAlias(true);
            hourPaint.setAntiAlias(true);
            minutePaint.setAntiAlias(true);
            secondPaint.setAntiAlias(true);
            hourShadowPaint.setAntiAlias(true);
            minuteShadowPaint.setAntiAlias(true);
            secondShadowPaint.setAntiAlias(true);

            separatorPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            hourPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            minutePaint.setStyle(Paint.Style.FILL_AND_STROKE);
            secondPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            hourShadowPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            minuteShadowPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            secondShadowPaint.setStyle(Paint.Style.FILL_AND_STROKE);

            separatorPaint.setColor(WearSettings.getDigitalHourColor());
            hourPaint.setColor(WearSettings.getDigitalHourColor());
            minutePaint.setColor(WearSettings.getDigitalMinuteColor());
            secondPaint.setColor(WearSettings.getDigitalSecondColor());
            hourShadowPaint.setColor(WearSettings.getDigitalHourShadowColor());
            minuteShadowPaint.setColor(WearSettings.getDigitalMinuteShadowColor());
            secondShadowPaint.setColor(WearSettings.getDigitalSecondShadowColor());

            separatorPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 45, getResources().getDisplayMetrics()));
            hourPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 45, getResources().getDisplayMetrics()));
            minutePaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 45, getResources().getDisplayMetrics()));
            secondPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 45, getResources().getDisplayMetrics()));
            hourShadowPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 45, getResources().getDisplayMetrics()));
            minuteShadowPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 45, getResources().getDisplayMetrics()));
            secondShadowPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 45, getResources().getDisplayMetrics()));

            separatorWidth = separatorPaint.measureText(TIME_SEPARATOR);

            invalidate();
        }

        /**
         * Borrowed from code written by Michael Scheper
         * Sets the text size for a Paint object so a given string of text will be a
         * given width.
         *
         * @param paint
         *            the Paint to set the text size for
         * @param desiredWidth
         *            the desired width
         * @param text
         *            the text that should be that width
         */
        private void setTextSizeForWidth(Paint paint, float desiredWidth,
                String text) {

            // Pick a reasonably large value for the test. Larger values produce
            // more accurate results, but may cause problems with hardware
            // acceleration. But there are workarounds for that, too; refer to
            // http://stackoverflow.com/questions/6253528/font-size-too-large-to-fit-in-cache
            final float testTextSize = 48f;

            // Get the bounds of the text, using our testTextSize.
            paint.setTextSize(testTextSize);
            Rect bounds = new Rect();
            paint.getTextBounds(text, 0, text.length(), bounds);

            // Calculate the desired size as a proportion of our testTextSize.
            float desiredTextSize = testTextSize * desiredWidth / bounds.width();

            // Set the paint for that size.
            paint.setTextSize(desiredTextSize);
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
            separatorPaint.setAntiAlias(!lowBitMode);
            hourPaint.setAntiAlias(!lowBitMode);
            minutePaint.setAntiAlias(!lowBitMode);
            hourShadowPaint.setAntiAlias(!lowBitMode);
            minuteShadowPaint.setAntiAlias(!lowBitMode);

            separatorPaint.setColor(Color.WHITE);
            hourPaint.setColor(Color.WHITE);
            minutePaint.setColor(Color.WHITE);
            hourShadowPaint.setColor(Color.WHITE);
            minuteShadowPaint.setColor(Color.WHITE);

            separatorPaint.setStyle(Paint.Style.STROKE);
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

            // Show colons for the first half of each second so the colons blink on when the time
            // updates.
            boolean mShouldDrawColons = (System.currentTimeMillis() % 1000) < 500;

            // Draw the hours.
            float x = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    15,
                    getResources().getDisplayMetrics());
            float yOffset = bounds.height() / 2;

            canvas.drawText("" + time.hour, x - 2.0f, yOffset - 2.0f, hourShadowPaint);
            canvas.drawText("" + time.hour, x, yOffset, hourPaint);
            x += hourPaint.measureText("" + time.hour);

            // In ambient and mute modes, always draw the first colon. Otherwise, draw the
            // first colon for the first half of each second.
            if (isInAmbientMode() || mShouldDrawColons) {
                canvas.drawText(TIME_SEPARATOR, x - 2.0f, yOffset - 2.0f, separatorPaint);
                canvas.drawText(TIME_SEPARATOR, x, yOffset, separatorPaint);
            }
            x += separatorWidth;

            // Draw the minutes.
            canvas.drawText("" + time.minute, x - 2.0f, yOffset - 2.0f, minuteShadowPaint);
            canvas.drawText("" + time.minute, x, yOffset, minutePaint);
            x += minutePaint.measureText("" + time.minute);

            // In ambient and mute modes, draw AM/PM. Otherwise, draw a second blinking
            // colon followed by the seconds.
            if (!isInAmbientMode()) {
                if (mShouldDrawColons) {
                    canvas.drawText(TIME_SEPARATOR, x - 2.0f, yOffset - 2.0f, separatorPaint);
                    canvas.drawText(TIME_SEPARATOR, x, yOffset, separatorPaint);
                }
                x += separatorWidth;
                canvas.drawText(String.format("%02d", time.second), x - 2.0f, yOffset - 2.0f, secondShadowPaint);
                canvas.drawText(String.format("%02d", time.second), x, yOffset,
                        secondPaint);
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