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

    public static void setTimeType(String type) {
        prefs.edit().putString("time_type", type).commit();
    }

    public static String getTimeType() {
        return prefs.getString("time_type", DIGITAL);
    }

    public static void setUseTimePalette(boolean use) {
        prefs.edit().putBoolean("use_time_palette", use).commit();
    }

    public static boolean useTimePalette() {
        return prefs.getBoolean("use_time_palette", false);
    }

    public static void setTimeOffset(long offset) {
        prefs.edit().putLong("time_offset", offset).commit();
    }

    public static long getTimeOffset() {
        return prefs.getLong("time_offset", 0);
    }

    public static void setTimeColor(int color) {
        prefs.edit().putInt("time_color", color).commit();
    }

    public static int getTimeColor() {
        return prefs.getInt("time_color", 0xFFFFFFFF);
    }

    public static void setTimeShadowColor(int color) {
        prefs.edit().putInt("time_shadow_color", color).commit();
    }

    public static int getTimeShadowColor() {
        return prefs.getInt("time_shadow_color", 0xFF000000);
    }

    public static void setTimeSize(float size) {
        prefs.edit().putFloat("time_size", size).commit();
    }

    public static float getTimeSize() {
        return prefs.getFloat("time_size", 24);
    }

    public static void setTickWidth(float width) {
        prefs.edit().putFloat("wear_tick_width", width).commit();
    }

    public static float getTickWidth() {
        return prefs.getFloat("wear_tick_width", 5.0f);
    }

    public static void setHourWidth(float width) {
        prefs.edit().putFloat("wear_hour_width", width).commit();
    }

    public static float getHourWidth() {
        return prefs.getFloat("wear_hour_width", 5.0f);
    }

    public static void setMinuteWidth(float width) {
        prefs.edit().putFloat("wear_minute_width", width).commit();
    }

    public static float getMinuteWidth() {
        return prefs.getFloat("wear_minute_width", 3.0f);
    }

    public static void setSecondWidth(float width) {
        prefs.edit().putFloat("wear_second_width", width).commit();
    }

    public static float getSecondWidth() {
        return prefs.getFloat("wear_second_width", 2.0f);
    }

    public static void setTickLength(float length) {
        prefs.edit().putFloat("wear_tick_length", length).commit();
    }

    public static float getTickLength() {
        return prefs.getFloat("wear_tick_length", 80f);
    }

    public static void setHourLength(float length) {
        prefs.edit().putFloat("wear_hour_length", length).commit();
    }

    public static float getHourLength() {
        return prefs.getFloat("wear_hour_length", 50f);
    }

    public static void setMinuteLength(float length) {
        prefs.edit().putFloat("wear_minute_length", length).commit();
    }

    public static float getMinuteLength() {
        return prefs.getFloat("wear_minute_length", 66f);
    }

    public static void setSecondLength(float length) {
        prefs.edit().putFloat("wear_second_length", length).commit();
    }

    public static float getSecondLength() {
        return prefs.getFloat("wear_second_length", 100f);
    }

    // Wear time settings

    public static void setSeparatorText(String text) {
        prefs.edit().putString("wear_separator_text", text).commit();
    }

    public static String getSeparatorText() {
        return prefs.getString("wear_separator_text", ":");
    }

    public static void setSeparatorColor(int color) {
        prefs.edit().putInt("digital_separator_color", color).commit();
    }

    public static int getSeparatorColor() {
        return prefs.getInt("wear_separator_color", 0xFFFFFFFF);
    }

    public static void setSeparatorShadowColor(int color) {
        prefs.edit().putInt("wear_separator_shadow_color", color).commit();
    }

    public static int getSeparatorShadowColor() {
        return prefs.getInt("wear_separator_shadow_color", 0xFF000000);
    }

    public static void setHourColor(int color) {
        prefs.edit().putInt("wear_hour_color", color).commit();
    }

    public static int getHourColor() {
        return prefs.getInt("wear_hour_color", 0xFFFFFFFF);
    }

    public static void setHourShadowColor(int color) {
        prefs.edit().putInt("wear_hour_shadow_color", color).commit();
    }

    public static int getHourShadowColor() {
        return prefs.getInt("wear_hour_shadow_color", 0xFF000000);
    }

    public static void setMinuteColor(int color) {
        prefs.edit().putInt("wear_minute_color", color).commit();
    }

    public static int getMinuteColor() {
        return prefs.getInt("wear_minute_color", 0xFFFFFFFF);
    }

    public static void setMinuteShadowColor(int color) {
        prefs.edit().putInt("wear_minute_shadow_color", color).commit();
    }

    public static int getMinuteShadowColor() {
        return prefs.getInt("wear_minute_shadow_color", 0xFF000000);
    }

    public static void setSecondColor(int color) {
        prefs.edit().putInt("wear_second_color", color).commit();
    }

    public static int getSecondColor() {
        return prefs.getInt("wear_second_color", 0xFFFFFFFF);
    }

    public static void setSecondShadowColor(int color) {
        prefs.edit().putInt("wear_second_shadow_color", color).commit();
    }

    public static int getSecondShadowColor() {
        return prefs.getInt("wear_second_shadow_color", 0xFF000000);
    }

    public static void setDigitalHourSize(float size) {
        prefs.edit().putFloat("digital_hour_size", size).commit();
    }

    public static float getDigitalHourSize() {
        return prefs.getFloat("digital_hour_size", 50f);
    }

    public static void setDigitalMinuteSize(float size) {
        prefs.edit().putFloat("digital_minute_size", size).commit();
    }

    public static float getDigitalMinuteSize() {
        return prefs.getFloat("digital_minute_size", 66f);
    }

    public static void setDigitalSecondSize(float size) {
        prefs.edit().putFloat("digital_second_size", size).commit();
    }

    public static float getDigitalSecondSize() {
        return prefs.getFloat("digital_second_size", 100f);
    }

}
