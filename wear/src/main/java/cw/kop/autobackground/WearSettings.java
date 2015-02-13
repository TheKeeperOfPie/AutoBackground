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

import android.content.SharedPreferences;

import cw.kop.autobackground.shared.WearConstants;

/**
 * Created by TheKeeperOfPie on 12/11/2014.
 */

public class WearSettings {

    public static final String DIGITAL = "Digital";
    public static final String ANALOG = "Analog";
    public static final float SHADOW_RADIUS = 5f;

    private static SharedPreferences prefs;

    public static void initPrefs(SharedPreferences preferences) {
        prefs = preferences;
        if (isFirstRun()) {
            prefs.edit().putBoolean("first_run", false).commit();
        }
    }

    private static boolean isFirstRun() {
        return prefs.getBoolean("first_run_wear", true);
    }

    public static void setUseTimePalette(boolean use) {
        prefs.edit().putBoolean("use_time_palette", use).commit();
    }

    public static void setTimeType(String type) {
        prefs.edit().putString(WearConstants.TIME_TYPE, type).apply();
    }

    public static String getTimeType() {
        return prefs.getString(WearConstants.TIME_TYPE, DIGITAL);
    }

    public static boolean useTimePalette() {
        return prefs.getBoolean(WearConstants.USE_TIME_PALETTE, false);
    }

    public static void setTimeOffset(long offset) {
        prefs.edit().putLong(WearConstants.TIME_OFFSET, offset).commit();
    }

    public static long getTimeOffset() {
        return prefs.getLong(WearConstants.TIME_OFFSET, 0);
    }

    public static void setTickWidth(float width) {
        prefs.edit().putFloat(WearConstants.TICK_WIDTH, width).commit();
    }

    public static float getTickWidth() {
        return prefs.getFloat(WearConstants.TICK_WIDTH, 5.0f);
    }

    public static void setHourWidth(float width) {
        prefs.edit().putFloat(WearConstants.HOUR_WIDTH, width).commit();
    }

    public static float getHourWidth() {
        return prefs.getFloat(WearConstants.HOUR_WIDTH, 5.0f);
    }

    public static void setMinuteWidth(float width) {
        prefs.edit().putFloat(WearConstants.MINUTE_WIDTH, width).commit();
    }

    public static float getMinuteWidth() {
        return prefs.getFloat(WearConstants.MINUTE_WIDTH, 3.0f);
    }

    public static void setSecondWidth(float width) {
        prefs.edit().putFloat(WearConstants.SECOND_WIDTH, width).commit();
    }

    public static float getSecondWidth() {
        return prefs.getFloat(WearConstants.SECOND_WIDTH, 2.0f);
    }

    public static void setTickLengthRatio(float length) {
        prefs.edit().putFloat(WearConstants.TICK_LENGTH_RATIO, length).commit();
    }

    public static float getTickLengthRatio() {
        return prefs.getFloat(WearConstants.TICK_LENGTH_RATIO, 80f);
    }

    public static void setHourLengthRatio(float length) {
        prefs.edit().putFloat(WearConstants.HOUR_LENGTH_RATIO, length).commit();
    }

    public static float getHourLengthRatio() {
        return prefs.getFloat(WearConstants.HOUR_LENGTH_RATIO, 50f);
    }

    public static void setMinuteLengthRatio(float length) {
        prefs.edit().putFloat(WearConstants.MINUTE_LENGTH_RATIO, length).commit();
    }

    public static float getMinuteLengthRatio() {
        return prefs.getFloat(WearConstants.MINUTE_LENGTH_RATIO, 66f);
    }

    public static void setSecondLengthRatio(float length) {
        prefs.edit().putFloat(WearConstants.SECOND_LENGTH_RATIO, length).commit();
    }

    public static float getSecondLengthRatio() {
        return prefs.getFloat(WearConstants.SECOND_LENGTH_RATIO, 100f);
    }

    public static void setSeparatorText(String text) {
        prefs.edit().putString(WearConstants.SEPARATOR_TEXT, text).commit();
    }

    public static String getSeparatorText() {
        return prefs.getString(WearConstants.SEPARATOR_TEXT, ":");
    }

    public static void setSeparatorColor(int color) {
        prefs.edit().putInt(WearConstants.SEPARATOR_COLOR, color).commit();
    }

    public static int getSeparatorColor() {
        return prefs.getInt(WearConstants.SEPARATOR_COLOR, 0xFFFFFFFF);
    }

    public static void setSeparatorShadowColor(int color) {
        prefs.edit().putInt(WearConstants.SEPARATOR_SHADOW_COLOR, color).commit();
    }

    public static int getSeparatorShadowColor() {
        return prefs.getInt(WearConstants.SEPARATOR_SHADOW_COLOR, 0xFF000000);
    }

    public static void setHourColor(int color) {
        prefs.edit().putInt(WearConstants.HOUR_COLOR, color).commit();
    }

    public static int getHourColor() {
        return prefs.getInt(WearConstants.HOUR_COLOR, 0xFFFFFFFF);
    }

    public static void setHourShadowColor(int color) {
        prefs.edit().putInt(WearConstants.HOUR_SHADOW_COLOR, color).commit();
    }

    public static int getHourShadowColor() {
        return prefs.getInt(WearConstants.HOUR_SHADOW_COLOR, 0xFF000000);
    }

    public static void setMinuteColor(int color) {
        prefs.edit().putInt(WearConstants.MINUTE_COLOR, color).commit();
    }

    public static int getMinuteColor() {
        return prefs.getInt(WearConstants.MINUTE_COLOR, 0xFFFFFFFF);
    }

    public static void setMinuteShadowColor(int color) {
        prefs.edit().putInt(WearConstants.MINUTE_SHADOW_COLOR, color).commit();
    }

    public static int getMinuteShadowColor() {
        return prefs.getInt(WearConstants.MINUTE_SHADOW_COLOR, 0xFF000000);
    }

    public static void setSecondColor(int color) {
        prefs.edit().putInt(WearConstants.SECOND_COLOR, color).commit();
    }

    public static int getSecondColor() {
        return prefs.getInt(WearConstants.SECOND_COLOR, 0xFFFFFFFF);
    }

    public static void setSecondShadowColor(int color) {
        prefs.edit().putInt(WearConstants.SECOND_SHADOW_COLOR, color).commit();
    }

    public static int getSecondShadowColor() {
        return prefs.getInt(WearConstants.SECOND_SHADOW_COLOR, 0xFF000000);
    }

}
