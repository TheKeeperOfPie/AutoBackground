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

    public static String getStringPref(String key) {
        return prefs.getString(key, "Nope");
    }
}
