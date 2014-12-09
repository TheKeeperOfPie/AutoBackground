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

import android.app.WallpaperManager;
import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by TheKeeperOfPie on 12/4/2014.
 */
public class WearSettings {

    public static final String DIGITAL = "Digital";
    public static final String ANALOG = "Analog";

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

    public static void setAnalogHourColor(int color) {
        prefs.edit().putInt("analog_hour_color", color).commit();
    }

    public static int getAnalogHourColor() {
        return prefs.getInt("analog_hour_color", 0xFFFFFFF);
    }

    public static void setAnalogHourShadowColor(int color) {
        prefs.edit().putInt("analog_hour_shadow_color", color).commit();
    }

    public static int getAnalogHourShadowColor() {
        return prefs.getInt("analog_hour_shadow_color", 0xFF000000);
    }

    public static void setAnalogMinuteColor(int color) {
        prefs.edit().putInt("analog_minute_color", color).commit();
    }

    public static int getAnalogMinuteColor() {
        return prefs.getInt("analog_minute_color", 0xFFFFFFF);
    }

    public static void setAnalogMinuteShadowColor(int color) {
        prefs.edit().putInt("analog_minute_shadow_color", color).commit();
    }

    public static int getAnalogMinuteShadowColor() {
        return prefs.getInt("analog_minute_shadow_color", 0xFF000000);
    }

    public static void setAnalogSecondColor(int color) {
        prefs.edit().putInt("analog_second_color", color).commit();
    }

    public static int getAnalogSecondColor() {
        return prefs.getInt("analog_second_color", 0xFFFFFFF);
    }

    public static void setAnalogSecondShadowColor(int color) {
        prefs.edit().putInt("analog_second_shadow_color", color).commit();
    }

    public static int getAnalogSecondShadowColor() {
        return prefs.getInt("analog_second_shadow_color", 0xFF000000);
    }

    public static void setAnalogHourWidth(float width) {
        prefs.edit().putFloat("analog_hour_width", width).commit();
    }

    public static float getAnalogHourWidth() {
        return prefs.getFloat("analog_hour_width", 5.0f);
    }

    public static void setAnalogMinuteWidth(float width) {
        prefs.edit().putFloat("analog_minute_width", width).commit();
    }

    public static float getAnalogMinuteWidth() {
        return prefs.getFloat("analog_minute_width", 3.0f);
    }

    public static void setAnalogSecondWidth(float width) {
        prefs.edit().putFloat("analog_second_width", width).commit();
    }

    public static float getAnalogSecondWidth() {
        return prefs.getFloat("analog_second_width", 2.0f);
    }

    public static void setAnalogHourLength(float length) {
        prefs.edit().putFloat("analog_hour_length", length).commit();
    }

    public static float getAnalogHourLength() {
        return prefs.getFloat("analog_hour_length", 50f);
    }

    public static void setAnalogMinuteLength(float length) {
        prefs.edit().putFloat("analog_minute_length", length).commit();
    }

    public static float getAnalogMinuteLength() {
        return prefs.getFloat("analog_minute_length", 66f);
    }

    public static void setAnalogSecondLength(float length) {
        prefs.edit().putFloat("analog_second_length", length).commit();
    }

    public static float getAnalogSecondLength() {
        return prefs.getFloat("analog_second_length", 100f);
    }


}
