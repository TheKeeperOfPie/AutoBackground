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

package cw.kop.autobackground.settings;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cw.kop.autobackground.R;
import cw.kop.autobackground.files.FileHandler;
import cw.kop.autobackground.shared.WearConstants;
import cw.kop.autobackground.sources.SortData;
import cw.kop.autobackground.sources.Source;

public class AppSettings {

    public static final String WEBSITE = "Website";
    public static final String FOLDER = "Folder";
    public static final String IMGUR_SUBREDDIT = "Imgur Subreddit";
    public static final String IMGUR_ALBUM = "Imgur Album";
    public static final String GOOGLE_PLUS_ALBUM = "Google+ Album";
    public static final String GOOGLE_DRIVE_ALBUM = "Google Drive Album";
    public static final String TUMBLR_BLOG = "Tumblr Blog";
    public static final String TUMBLR_TAG = "Tumblr Tag";
    public static final String REDDIT_SUBREDDIT = "Reddit Subreddit";
    public static final String DROPBOX_FOLDER = "Dropbox Folder";

    public static final String DATA_SPLITTER = ":::";

    public static final String DIGITAL = "Digital";
    public static final String ANALOG = "Analog";

    // Themes are user readable to easier set indicator using theme String
    public static final String APP_LIGHT_THEME = "Light Theme";
    public static final String APP_DARK_THEME = "Dark Theme";

    private static final String TAG = AppSettings.class.getCanonicalName();
    private static final long DEFAULT_INTERVAL = 0;

    public static final String PNG = ".png";
    public static final String JPG = ".jpg";
    public static final String JPEG = ".jpeg";
    public static final String WEBM = ".webm";

    private static SharedPreferences prefs;

    public static boolean checkIsImage(String url) {
        String lowercase = url.toLowerCase();

        return lowercase.endsWith(PNG) || lowercase.endsWith(JPG) || lowercase.endsWith(JPEG) || lowercase.endsWith(WEBM);
    }

    private static boolean isFirstRun() {
        return prefs.getBoolean("first_run", true);
    }

    public static boolean useFabric() {
        return prefs.getBoolean("use_fabric", false);
    }

    public static void setUseFabric(boolean use) {
        prefs.edit().putBoolean("use_fabric", use).commit();
    }

    public static boolean useTutorial() {
        return prefs.getBoolean("use_tutorial", true);
    }

    public static void setUseTutorial(boolean use) {
        prefs.edit().putBoolean("use_tutorial", use).commit();
    }

    public static void initPrefs(Context context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        if (isFirstRun()) {
            prefs.edit().putString("user_width",
                    "" + (WallpaperManager.getInstance(context).getDesiredMinimumWidth() / 2)).commit();
            prefs.edit().putString("user_height",
                    "" + (WallpaperManager.getInstance(context).getDesiredMinimumHeight() / 2)).commit();
            setNotificationOptionTitle(0, "Copy");
            setNotificationOptionTitle(1, "Cycle");
            setNotificationOptionTitle(2, "Delete");
            setNotificationOptionDrawable(0, R.drawable.ic_content_copy_white_24dp);
            setNotificationOptionDrawable(1, R.drawable.ic_refresh_white_24dp);
            setNotificationOptionDrawable(2, R.drawable.ic_delete_white_24dp);
            prefs.edit().putBoolean("use_timer", true).commit();
            setTimerDuration(172800000);
            prefs.edit().putBoolean("first_run", false).commit();
        }
    }

    public static void resetVer2_00_20() {

        if (prefs.getBoolean("reset_ver_2_00_20", true)) {

            ArrayList<Source> sourceList = new ArrayList<>();

            for (int index = 0; index < AppSettings.getNumSources(); index++) {

                try {
                    Source source = new Source();

                    source.setType(AppSettings.getSourceType(index));
                    source.setTitle(AppSettings.getSourceTitle(index));
                    source.setData(AppSettings.getSourceData(index));
                    source.setNum(AppSettings.getSourceNum(index));
                    source.setUse(AppSettings.useSource(index));
                    source.setPreview(AppSettings.useSourcePreview(index));
                    source.setUseTime(AppSettings.useSourceTime(index));
                    source.setTime(AppSettings.getSourceTime(index));

                    sourceList.add(source);
                }
                catch (Throwable e) {

                }
            }
            setSources(sourceList);

            prefs.edit().putBoolean("reset_ver_2_00_20", false).commit();
        }

    }

    public static void resetVer2_00() {

        if (prefs.getBoolean("reset_ver_2_00", true)) {

            Log.i("AppSettings", "RESET VER 2_00");

            for (int i = 0; i < getNumSources(); i++) {

                String newType = getSourceType(i);
                String data = getSourceData(i);
                switch (getSourceType(i)) {
                    case "website":
                        newType = AppSettings.WEBSITE;
                        break;
                    case "imgur":
                        if (data.contains("imgur.com/r/")) {
                            newType = AppSettings.IMGUR_SUBREDDIT;
                            prefs.edit().putString("source_data_" + i, data.substring(data.indexOf("imgur.com/r/") + 12)).commit();
                        }
                        else if (data.contains("imgur.com/a/")) {
                            newType = AppSettings.IMGUR_ALBUM;
                            prefs.edit().putString("source_data_" + i, data.substring(data.indexOf("imgur.com/a/") + 12)).commit();
                        }
                        break;
                    case "folder":
                        newType = AppSettings.FOLDER;
                        break;
                    case "picasa":
                        newType = AppSettings.GOOGLE_PLUS_ALBUM;
                        break;
                    case "tumblr_blog":
                        newType = AppSettings.TUMBLR_BLOG;
                        break;
                    case "tumblr_tag":
                        newType = AppSettings.TUMBLR_TAG;
                        break;
                }

                prefs.edit().putString("source_type_" + i, newType).commit();

            }

            prefs.edit().putBoolean("reset_ver_2_00", false).commit();
        }

    }

    public static void resetVer1_30() {

        if (prefs.getBoolean("reset_ver_1_30", true)) {
            setNotificationOptionTitle(0, "Copy");
            setNotificationOptionTitle(1, "Cycle");
            setNotificationOptionTitle(2, "Delete");
            setNotificationOptionDrawable(0, R.drawable.ic_content_copy_white_24dp);
            setNotificationOptionDrawable(1, R.drawable.ic_refresh_white_24dp);
            setNotificationOptionDrawable(2, R.drawable.ic_delete_white_24dp);
            setTheme(R.style.AppLightTheme);

            prefs.edit().putBoolean("reset_ver_1_30", false).commit();
        }
    }

    public static void resetVer1_40() {

        for (int i = 0; i < getNumSources(); i++) {

            if (getSourceType(i).equals(TUMBLR_TAG)) {


                if (getSourceData(i).contains("Tumblr Tag:")) {

                    prefs.edit().putString("source_data_" + i,
                            getSourceData(i).substring(12)).commit();

                }

            }
            else if (getSourceType(i).equals(TUMBLR_BLOG)) {
                String data = getSourceData(i);

                if (data.contains(".tumblr.com")) {

                    int startIndex = data.contains("://") ? data.indexOf("://") + 3 : 0;

                    prefs.edit().putString("source_data_" + i,
                            getSourceData(i).substring(startIndex, data.indexOf(".tumblr.com"))).commit();

                }
            }


        }


    }

    public static void clearPrefs(Context context) {
        prefs.edit().clear().commit();
        initPrefs(context);
    }

    public static void setUrl(String key, String url) {
        prefs.edit().putString(key, url).apply();
    }

    public static String getUrl(String key) {
        return prefs.getString(key, null);
    }

    public static void clearUrl(String key) {
        prefs.edit().remove(key).apply();
    }

    public static boolean useDownloadPath() {
        return prefs.getBoolean("use_download_path", false);
    }

    public static String getDownloadPath() {

        if (useDownloadPath() && prefs.getString("download_path", null) != null) {

            File dir = new File(prefs.getString("download_path", null));

            if (dir.exists() && dir.isDirectory()) {
                return dir.getAbsolutePath();
            }
        }

        return Environment.getExternalStorageDirectory().getAbsolutePath() + "/AutoBackgroundCache";
    }

    public static void setDownloadPath(String path) {
        prefs.edit().putString("download_path", path).apply();
    }

    public static String getTheme() {
        return prefs.getString("application_theme", AppSettings.APP_LIGHT_THEME);
    }

    public static void setTheme(int theme) {
        switch (theme) {
            case R.style.AppLightTheme:
                prefs.edit().putString("application_theme", AppSettings.APP_LIGHT_THEME).commit();
                break;
            case R.style.AppDarkTheme:
                prefs.edit().putString("application_theme", AppSettings.APP_DARK_THEME).commit();
                break;
            default:
                break;
        }
    }

    public static int getDialogThemeResource() {

        switch (getTheme()) {
            default:
            case APP_LIGHT_THEME:
                return R.style.LightDialogTheme;
            case APP_DARK_THEME:
                return R.style.DarkDialogTheme;
        }
    }

    public static int getColorFilterInt(Context context) {

        if (context == null) {
            return 0xFFDEDEDE;
        }

        switch (getTheme()) {
            default:
            case APP_LIGHT_THEME:
                return context.getResources().getColor(R.color.DARK_GRAY_OPAQUE);
            case APP_DARK_THEME:
                return context.getResources().getColor(R.color.LIGHT_GRAY_OPAQUE);
        }

    }

    public static int getTransparentOverlayColor(Context context) {

        switch (getTheme()) {
            default:
            case APP_LIGHT_THEME:
                return context.getResources().getColor(R.color.LIGHT_GRAY_TRANSPARENT);
            case APP_DARK_THEME:
                return context.getResources().getColor(R.color.DARK_GRAY_TRANSPARENT);
        }

    }

    public static int getDialogColor(Context context) {

        switch (getTheme()) {
            default:
            case APP_LIGHT_THEME:
                return context.getResources().getColor(R.color.LIGHT_THEME_DIALOG);
            case APP_DARK_THEME:
                return context.getResources().getColor(R.color.DARK_THEME_DIALOG);
        }

    }
    public static int getDialogColorResource() {

        switch (getTheme()) {
            default:
            case APP_LIGHT_THEME:
                return R.color.LIGHT_THEME_DIALOG;
            case APP_DARK_THEME:
                return R.color.DARK_THEME_DIALOG;
        }

    }

    public static int getBackgroundColorResource() {

        switch (getTheme()) {
            default:
            case APP_LIGHT_THEME:
                return R.color.LIGHT_THEME_BACKGROUND;
            case APP_DARK_THEME:
                return R.color.DARK_THEME_BACKGROUND;
        }

    }

    public static int getImageWidth() {
        int width = 1000;

        try {
            width = Integer.parseInt(prefs.getString("user_width", "1000"));
        }
        catch (NumberFormatException e) {
            setImageWidth("" + width);
        }

        return width;
    }

    public static void setImageWidth(String width) {
        prefs.edit().putString("user_width", width).commit();
    }

    public static int getImageHeight() {
        int height = 1000;

        try {
            height = Integer.parseInt(prefs.getString("user_height", "1000"));
        }
        catch (NumberFormatException e) {
            setImageHeight("" + height);
        }

        return height;
    }

    public static void setImageHeight(String height) {
        prefs.edit().putString("user_height", height).commit();
    }

    public static boolean useFullResolution() {
        return prefs.getBoolean("full_resolution", true);
    }

    public static boolean forceDownload() {
        return prefs.getBoolean("force_download", false);
    }

    public static boolean useNotification() {
        return prefs.getBoolean("use_notification", false);
    }

    public static boolean usePinIndicator() {
        return prefs.getBoolean("use_pin_indicator", true);
    }

    public static boolean useDownloadNotification() {
        return prefs.getBoolean("use_download_notification", true);
    }

    public static Boolean useTimer() {
        return prefs.getBoolean("use_timer", false);
    }

    public static long getTimerDuration() {
        return prefs.getLong("timer_duration", 0);
    }

    public static void setTimerDuration(long timer) {
        prefs.edit().putLong("timer_duration", timer).apply();
    }

    public static int getTimerHour() {
        return prefs.getInt("timer_hour", 20);
    }

    public static void setTimerHour(int hour) {
        prefs.edit().putInt("timer_hour", hour).apply();
    }

    public static int getTimerMinute() {
        return prefs.getInt("timer_minute", 0);
    }

    public static void setTimerMinute(int minute) {
        prefs.edit().putInt("timer_minute", minute).apply();
    }

    public static boolean resetOnManualDownload() {
        return prefs.getBoolean("reset_on_manual_download", false);
    }

    public static boolean useInterval() {
        return prefs.getBoolean("use_interval", true);
    }

    public static long getIntervalDuration() {
        return prefs.getLong("interval_duration", DEFAULT_INTERVAL);
    }

    public static void setIntervalDuration(long interval) {
        prefs.edit().putLong("interval_duration", interval).apply();
    }

    public static boolean resetOnManualCycle() {
        return prefs.getBoolean("reset_on_manual_cycle", false);
    }

    public static long getPinDuration() {
        return prefs.getLong("pin_duration", 0);
    }

    public static void setPinDuration(long interval) {
        prefs.edit().putLong("pin_duration", interval).apply();
    }

    public static boolean keepImages() {
        return prefs.getBoolean("keep_images", false);
    }

    public static boolean deleteOldImages() {
        return prefs.getBoolean("delete_old_images", false);
    }

    public static boolean checkDuplicates() {
        return prefs.getBoolean("check_duplicates", true);
    }

    public static boolean shuffleImages() {
        return prefs.getBoolean("shuffle_images", true);
    }

    public static boolean showAlbumArt() {
        return prefs.getBoolean("show_album_art", false);
    }

    public static boolean useWifi() {
        return prefs.getBoolean("use_wifi", true);
    }

    public static boolean useMobile() {
        return prefs.getBoolean("use_data", false);
    }

    public static boolean useDownloadOnConnection() {
        return prefs.getBoolean("download_on_connection", false);
    }

    public static boolean preserveContext() {
        return prefs.getBoolean("preserve_context", true);
    }

    public static boolean changeWhenLocked() {
        return prefs.getBoolean("when_locked", true);
    }

    public static boolean forceInterval() {
        return prefs.getBoolean("force_interval", false);
    }

    public static boolean forceMultiPane() {
        return prefs.getBoolean("force_multi_pane", false);
    }

    public static boolean useDoubleImage() {
        return prefs.getBoolean("use_double_image", false);
    }

    public static void setUseDoubleImage(boolean use) {
        prefs.edit().putBoolean("use_double_image", use).commit();
    }

    public static boolean useAnimation() {
        return prefs.getBoolean("use_animation", true);
    }

    public static boolean useVerticalAnimation() {
        return prefs.getBoolean("use_animation_vertical", true);
    }

    public static int getAnimationSafety() {
        int buffer = 250;

        try {
            buffer = Integer.parseInt(prefs.getString("animation_safety_adv", "150"));
        }
        catch (NumberFormatException e) {
            setAnimationSafety("" + buffer);
        }

        return buffer;
    }

    public static void setAnimationSafety(String buffer) {
        prefs.edit().putString("animation_safety_adv", buffer).commit();
    }

    public static int getAnimationSpeed() {
        return prefs.getInt("animation_speed", 5);
    }

    public static void setAnimationSpeed(int speed) {
        prefs.edit().putInt("animation_speed", speed).commit();
    }

    public static int getVerticalAnimationSpeed() {
        return prefs.getInt("animation_speed_vertical", 5);
    }

    public static void setVerticalAnimationSpeed(int speed) {
        prefs.edit().putInt("animation_speed_vertical", speed).commit();
    }

    public static boolean scaleAnimationSpeed() {
        return prefs.getBoolean("scale_animation_speed", true);
    }

    public static int getAnimationFrameRate() {
        int rate = 60;

        try {
            rate = Integer.parseInt(prefs.getString("animation_frame_rate", "60"));
        }
        catch (NumberFormatException e) {
            setAnimationFrameRate("" + rate);
        }

        return rate;
    }

    public static void setAnimationFrameRate(String rate) {
        prefs.edit().putString("animation_frame_rate", rate).commit();
    }

    public static int getTransitionSpeed() {
        return prefs.getInt("transition_speed", 20);
    }

    public static void setTransitionSpeed(int speed) {
        prefs.edit().putInt("transition_speed", speed).commit();
    }

    public static boolean useFade() {
        return prefs.getBoolean("use_fade", true);
    }

    public static boolean useOvershoot() {
        return prefs.getBoolean("use_overshoot", false);
    }

    public static boolean reverseOvershoot() {
        return prefs.getBoolean("reverse_overshoot", false);
    }

    public static int getOvershootIntensity() {
        return prefs.getInt("overshoot_intensity", 10);
    }

    public static void setOvershootIntensity(int intensity) {
        prefs.edit().putInt("overshoot_intensity", intensity).commit();
    }

    public static boolean useVerticalOvershoot() {
        return prefs.getBoolean("use_overshoot_vertical", false);
    }

    public static boolean reverseVerticalOvershoot() {
        return prefs.getBoolean("reverse_overshoot_vertical", false);
    }

    public static int getVerticalOvershootIntensity() {
        return prefs.getInt("overshoot_intensity_vertical", 10);
    }

    public static void setVerticalOvershootIntensity(int intensity) {
        prefs.edit().putInt("overshoot_intensity_vertical", intensity).commit();
    }

    public static boolean useZoomIn() {
        return prefs.getBoolean("use_zoom_in", false);
    }

    public static boolean useZoomOut() {
        return prefs.getBoolean("use_zoom_out", false);
    }

    public static boolean useSpinIn() {
        return prefs.getBoolean("use_spin_in", false);
    }

    public static boolean reverseSpinIn() {
        return prefs.getBoolean("reverse_spin_in", false);
    }

    public static int getSpinInAngle() {
        return prefs.getInt("spin_in_angle", 2700);
    }

    public static void setSpinInAngle(int angle) {
        prefs.edit().putInt("spin_in_angle", angle).commit();
    }

    public static boolean useSpinOut() {
        return prefs.getBoolean("use_spin_out", false);
    }

    public static boolean reverseSpinOut() {
        return prefs.getBoolean("reverse_spin_out", false);
    }

    public static int getSpinOutAngle() {
        return prefs.getInt("spin_out_angle", 2700);
    }

    public static void setSpinOutAngle(int angle) {
        prefs.edit().putInt("spin_out_angle", angle).commit();
    }

    public static boolean useAdvanced() {
        return prefs.getBoolean("use_advanced", false);
    }

    public static boolean useDoubleTap() {
        return prefs.getBoolean("double_tap", true);
    }

    public static boolean useScrolling() {
        return prefs.getBoolean("use_scrolling", true);
    }

    public static boolean useExactScrolling() {
        return prefs.getBoolean("exact_scrolling", false);
    }

    public static boolean forceParallax() {
        return prefs.getBoolean("force_parallax", false);
    }

    public static boolean useDrag() {
        return prefs.getBoolean("use_drag", true);
    }

    public static boolean reverseDrag() {
        return prefs.getBoolean("reverse_drag", false);
    }

    public static boolean useScale() {
        return prefs.getBoolean("use_scale", true);
    }

    public static boolean extendScale() {
        return false; //prefs.getBoolean("extend_scale", false);
    }

    public static boolean useLongPressReset() {
        return prefs.getBoolean("use_long_press_reset", true);
    }

    public static boolean useToast() {
        return prefs.getBoolean("use_toast", true);
    }

    public static String getImagePrefix() {

        return prefs.getString("image_prefix_adv", "AutoBackground");
    }

    public static void setImagePrefix(String prefix) {

        prefs.edit().putString("image_prefix_adv", prefix).commit();
    }

    public static int getHistorySize() {
        return Integer.parseInt(prefs.getString("history_size", "15"));
    }

    public static void setHistorySize(int size) {
        prefs.edit().putString("history_size", String.valueOf(size)).apply();
    }

    public static boolean useHighResolutionNotificationIcon() {
        return prefs.getBoolean("high_resolution_notification_icon", false);
    }

    public static int getNumberSources() {
        return prefs.getInt("number_sources", 0);
    }

    public static void setSources(List<Source> listData) {

        int index = 0;

        for (Source source : listData) {
            try {
                prefs.edit().putString("source_" + index, source.toJson().toString()).commit();
                index++;
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
        }

        prefs.edit().putInt("number_sources", listData.size()).commit();
    }

    public static List<Source> getSources() {
        List<Source> sources = new ArrayList<>();
        for (int index = 0; index < AppSettings.getNumberSources(); index++) {
            sources.add(AppSettings.getSource(index));
        }
        return sources;
    }

    public static Source getSource(int index) {

        try {
            return Source.fromJson(new JSONObject(prefs.getString("source_" + index, "{}")));
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
        return new Source();

    }

    public static int getNumSources() {
        return prefs.getInt("num_sources", 0);
    }

    public static String getSourceDataPrefix(String type) {

        switch (type) {

            default:
            case WEBSITE:
            case FOLDER:
            case GOOGLE_PLUS_ALBUM:
            case GOOGLE_DRIVE_ALBUM:
            case TUMBLR_BLOG:
                return "";
            case TUMBLR_TAG:
                return "Tag: ";
            case IMGUR_SUBREDDIT:
                return "imgur.com/r/";
            case IMGUR_ALBUM:
                return "imgur.com/a/";
            case REDDIT_SUBREDDIT:
                return "/r/";
        }
    }

    public static String getSourceDataHint(String type) {

        switch (type) {

            default:
            case FOLDER:
            case GOOGLE_PLUS_ALBUM:
            case GOOGLE_DRIVE_ALBUM:
                return "";
            case AppSettings.WEBSITE:
                return "URL";
            case AppSettings.IMGUR_SUBREDDIT:
                return "Subreddit";
            case AppSettings.IMGUR_ALBUM:
                return "Album ID";
            case AppSettings.TUMBLR_BLOG:
                return "Blog Name";
            case AppSettings.TUMBLR_TAG:
                return "Tag";
            case AppSettings.REDDIT_SUBREDDIT:
                return "Subreddit";
        }
    }

    public static String getSourceDataSuffix(String type) {

        switch (type) {
            default:
            case WEBSITE:
            case FOLDER:
            case GOOGLE_PLUS_ALBUM:
            case GOOGLE_DRIVE_ALBUM:
            case TUMBLR_TAG:
            case IMGUR_SUBREDDIT:
            case IMGUR_ALBUM:
            case REDDIT_SUBREDDIT:
                return "";
            case TUMBLR_BLOG:
                return ".tumblr.com";
        }
    }

    public static SortData getSourceSortParameter(Source source) {

        switch (source.getType()) {
            case IMGUR_SUBREDDIT:
                switch (source.getSort()) {
                    default:
                    case "New":
                        return new SortData("New", "time", "");
                    case "Top - Day":
                        return new SortData("Top - Day", "top/day", "");
                    case "Top - Week":
                        return new SortData("Top - Week", "top/week", "");
                    case "Top - Month":
                        return new SortData("Top - Month", "top/month", "");
                    case "Top - Year":
                        return new SortData("Top - Year", "top/year", "");
                    case "Top - All":
                        return new SortData("Top - All", "top/all", "");
                }
            case REDDIT_SUBREDDIT:

                switch (source.getSort()) {
                    default:
                    case "Hot":
                        return new SortData("Hot", "hot", "");
                    case "New":
                        return new SortData("New", "new", "");
                    case "Top - Hour":
                        return new SortData("Top - Hour", "top", "hour");
                    case "Top - Day":
                        return new SortData("Top - Day", "top", "day");
                    case "Top - Week":
                        return new SortData("Top - Week", "top", "week");
                    case "Top - Month":
                        return new SortData("Top - Month", "top", "month");
                    case "Top - Year":
                        return new SortData("Top - Year", "top", "year");
                    case "Top - All":
                        return new SortData("Top - All", "top", "all");
                }
            default:
        }
        return null;
    }

    public static List<SortData> getSourceSortList(String type) {

        List<SortData> sortData = new ArrayList<>();

        switch (type) {
            case IMGUR_SUBREDDIT:
                sortData.add(new SortData("New", "time", ""));
                sortData.add(new SortData("Top - Day", "top/day", ""));
                sortData.add(new SortData("Top - Week", "top/week", ""));
                sortData.add(new SortData("Top - Month", "top/month", ""));
                sortData.add(new SortData("Top - Year", "top/year", ""));
                sortData.add(new SortData("Top - All", "top/all", ""));
                break;
            case REDDIT_SUBREDDIT:
                sortData.add(new SortData("Hot", "hot", ""));
                sortData.add(new SortData("New", "new", ""));
                sortData.add(new SortData("Top - Hour", "top", "hour"));
                sortData.add(new SortData("Top - Day", "top", "day"));
                sortData.add(new SortData("Top - Week", "top", "week"));
                sortData.add(new SortData("Top - Month", "top", "month"));
                sortData.add(new SortData("Top - Year", "top", "year"));
                sortData.add(new SortData("Top - All", "top", "all"));
                break;
            default:
        }
        return sortData;
    }

    public static int getSourceNum(int index) {
        int num;
        try {
            num = Integer.parseInt(prefs.getString("source_num_" + index, "0"));
        }
        catch (NumberFormatException e) {
            num = 0;
        }
        return num;
    }

    public static boolean useSourcePreview(int index) {
        return prefs.getBoolean("source_preview_" + index, true);
    }

    public static String getSourceType(int index) {
        return prefs.getString("source_type_" + index, null);
    }

    public static boolean useSource(int index) {
        return prefs.getBoolean("use_source_" + index, false);
    }

    public static String getSourceTitle(int index) {
        return prefs.getString("source_title_" + index, null);
    }

    public static String getSourceData(int index) {
        return prefs.getString("source_data_" + index, null);
    }

    public static boolean useSourceTime(int index) {
        return prefs.getBoolean("use_source_time_" + index, false);
    }

    public static String getSourceTime(int index) {
        return prefs.getString("source_time_" + index, "00:00 - 00:00");
    }

    public static boolean useImageHistory() {
        return prefs.getBoolean("use_image_history", true);
    }

    public static int getImageHistorySize() {
        return Integer.parseInt(prefs.getString("image_history_size", "500"));
    }

    public static void setImageHistorySize(int size) {
        prefs.edit().putString("image_history_size", "" + size).apply();
    }

    public static boolean cacheThumbnails() {
        return prefs.getBoolean("use_thumbnails", true);
    }

    public static void setThumbnailSize(int size) {
        prefs.edit().putString("thumbnail_size", String.valueOf(size)).apply();
    }

    public static int getThumbnailSize() {
        return Integer.parseInt(prefs.getString("thumbnail_size", "150"));
    }

    public static HashSet<String> getUsedLinks() {
        return (HashSet<String>) prefs.getStringSet("used_history_links", new HashSet<String>());
    }

    public static void setUsedLinks(Set<String> usedLinks) {
        prefs.edit().putStringSet("used_history_links", usedLinks).commit();
    }

    public static void addUsedLink(String link, long time) {
        HashSet<String> set = getUsedLinks();
        set.add(link + "Time:" + time);

        Log.d(TAG, "addUsedLink: " + (link + "Time:" + time));

        prefs.edit().putStringSet("used_history_links", set).commit();
    }

    public static void clearUsedLinks() {
        prefs.edit().putStringSet("used_history_links", new HashSet<String>()).commit();
    }

    public static void checkUsedLinksSize() {

        HashSet<String> set = getUsedLinks();

        int iterations = set.size() - getImageHistorySize();

        if (iterations > 0) {
            List<String> linkList = new ArrayList<String>();
            linkList.addAll(set);

            Collections.sort(linkList, new Comparator<String>() {
                @Override
                public int compare(String lhs, String rhs) {

                    try {
                        long first = Long.parseLong(lhs.substring(lhs.lastIndexOf("Time:") + 5));
                        long second = Long.parseLong(rhs.substring(rhs.lastIndexOf("Time:") + 5));

                        return (int) (first - second);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }

                    return 0;
                }
            });

            Log.d(TAG, "checkUsedLinksSize");
            for (String link : linkList) {
                Log.d(TAG, "Link: " + link);
            }

            for (int i = 0; i < iterations; i++) {
                Log.d(TAG, "Remove: " + linkList.get(0));
                linkList.remove(0);
            }

            Log.d(TAG, "After remove");
            for (String link : linkList) {
                Log.d(TAG, "Link: " + link);
            }

            HashSet<String> newSet = new HashSet<String>(linkList);

            prefs.edit().putStringSet("used_history_links", newSet).commit();

        }
    }

    public static int getNotificationColor() {
        return prefs.getInt("notification_color", -15461356);
    }

    public static void setNotificationColor(int color) {
        prefs.edit().putInt("notification_color", color).apply();
    }

    public static String getNotificationTitle() {
        return prefs.getString("notification_title", "AutoBackground");
    }

    public static void setNotificationTitle(String title) {
        prefs.edit().putString("notification_title", title).apply();
    }

    public static int getNotificationTitleColor() {
        return prefs.getInt("notification_title_color", -1);
    }

    public static void setNotificationTitleColor(int color) {
        prefs.edit().putInt("notification_title_color", color).apply();
    }

    public static int getNotificationIcon() {

        String value = prefs.getString("notification_icon_string", "Image");

        switch (value) {

            case "Application":
                return R.drawable.ic_info_white_24dp;
            case "Image":
                return R.drawable.ic_photo_white_24dp;
            case "Custom":
                return R.drawable.ic_settings_white_24dp;
            case "None":
                return R.drawable.ic_check_box_outline_blank_white_24dp;

        }

        return R.drawable.ic_photo_white_24dp;
    }

    public static void setNotificationIcon(int drawable) {

        String value = "None";

        switch (drawable) {

            case R.drawable.ic_info_white_24dp:
                value = "Application";
                break;
            case R.drawable.ic_photo_white_24dp:
                value = "Image";
                break;
            case R.drawable.ic_settings_white_24dp:
                value = "Custom";
                break;

        }

        prefs.edit().putString("notification_icon_string", value)
                .apply();
    }

    public static void setUseNotificationIconFile(boolean value) {
        prefs.edit().putBoolean("use_notification_icon_file", value).apply();
    }

    public static boolean useNotificationIconFile() {
        return prefs.getBoolean("use_notification_icon_file", false);
    }

    public static String getNotificationIconFile() {

        if (prefs.getString("notification_icon_file", null) != null) {

            File image = new File(prefs.getString("notification_icon_file", null));

            if (image.exists() && image.isFile() && FileHandler.getImageFileNameFilter().accept(image.getParentFile(), image.getName())) {
                return image.getAbsolutePath();
            }
        }

        return null;
    }

    public static void setNotificationIconFile(String filePath) {
        prefs.edit().putString("notification_icon_file", filePath).apply();
    }

    public static String getNotificationIconAction() {
        return prefs.getString("notification_icon_action", "Game");
    }

    public static void setNotificationIconAction(String action) {
        prefs.edit().putString("notification_icon_action", action).apply();
    }

    public static int getNotificationIconActionDrawable() {

        String value = prefs.getString("notification_icon_action_drawable_string", "Game");

        switch (value) {
            case "Copy":
                return R.drawable.ic_content_copy_white_24dp;
            case "Cycle":
                return R.drawable.ic_refresh_white_24dp;
            case "Delete":
                return R.drawable.ic_delete_white_24dp;
            case "Open":
                return R.drawable.ic_photo_white_24dp;
            case "Pin":
                return R.drawable.ic_pin_drop_white_24dp;
            case "Previous":
                return R.drawable.ic_arrow_back_white_24dp;
            case "Share":
                return R.drawable.ic_share_white_24dp;
            case "Game":
                return R.drawable.ic_gamepad_white_24dp;
        }

        return R.color.TRANSPARENT_BACKGROUND;
    }

    public static void setNotificationIconActionDrawable(int drawable) {

        String value = "Game";

        switch (drawable) {
            case R.drawable.ic_content_copy_white_24dp:
                value = "Copy";
                break;
            case R.drawable.ic_refresh_white_24dp:
                value = "Cycle";
                break;
            case R.drawable.ic_delete_white_24dp:
                value = "Delete";
                break;
            case R.drawable.ic_photo_white_24dp:
                value = "Open";
                break;
            case R.drawable.ic_pin_drop_white_24dp:
                value = "Pin";
                break;
            case R.drawable.ic_arrow_back_white_24dp:
                value = "Previous";
                break;
            case R.drawable.ic_share_white_24dp:
                value = "Share";
                break;
            case R.drawable.ic_gamepad_white_24dp:
                value = "Game";
                break;
        }

        prefs.edit().putString("notification_icon_action_drawable_string", value).apply();
    }

    public static String getNotificationSummary() {
        return prefs.getString("notification_summary", "Location");
    }

    public static void setNotificationSummary(String summary) {
        prefs.edit().putString("notification_summary", summary).apply();
    }

    public static int getNotificationSummaryColor() {
        return prefs.getInt("notification_summary_color", -1);
    }

    public static void setNotificationSummaryColor(int color) {
        prefs.edit().putInt("notification_summary_color", color)
                .apply();
    }

    public static void setNotificationOptionTitle(int position, String title) {
        prefs.edit().putString("notification_list_title" + position, title).apply();
    }

    public static String getNotificationOptionTitle(int position) {
        return prefs.getString("notification_list_title" + position, "None");
    }

    public static void setNotificationOptionDrawable(int position, int drawable) {

        String value = "None";

        switch (drawable) {
            case R.drawable.ic_content_copy_white_24dp:
                value = "Copy";
                break;
            case R.drawable.ic_refresh_white_24dp:
                value = "Cycle";
                break;
            case R.drawable.ic_delete_white_24dp:
                value = "Delete";
                break;
            case R.drawable.ic_photo_white_24dp:
                value = "Open";
                break;
            case R.drawable.ic_pin_drop_white_24dp:
                value = "Pin";
                break;
            case R.drawable.ic_arrow_back_white_24dp:
                value = "Previous";
                break;
            case R.drawable.ic_share_white_24dp:
                value = "Share";
                break;
            case R.drawable.ic_gamepad_white_24dp:
                value = "Game";
                break;
        }

        prefs.edit().putString("notification_drawable_string" + position, value).apply();
    }

    public static int getNotificationOptionDrawable(int position) {

        String value = prefs.getString("notification_drawable_string" + position, "None");

        switch (value) {
            case "Copy":
                return R.drawable.ic_content_copy_white_24dp;
            case "Cycle":
                return R.drawable.ic_refresh_white_24dp;
            case "Delete":
                return R.drawable.ic_delete_white_24dp;
            case "Open":
                return R.drawable.ic_photo_white_24dp;
            case "Pin":
                return R.drawable.ic_pin_drop_white_24dp;
            case "Previous":
                return R.drawable.ic_arrow_back_white_24dp;
            case "Share":
                return R.drawable.ic_share_white_24dp;
            case "Game":
                return R.drawable.ic_gamepad_white_24dp;
            case "None":
                return R.drawable.ic_check_box_outline_blank_white_24dp;
        }

        return R.drawable.ic_check_box_outline_blank_white_24dp;

    }

    public static void setNotificationOptionColor(int position, int color) {
        prefs.edit().putInt("notification_option_color" + position, color).apply();
        prefs.edit().putInt("notification_option_previous_color", color).apply();
    }

    public static int getNotificationOptionColor(int position) {
        return prefs.getInt("notification_option_color" + position, -1);
    }

    public static int getNotificationOptionPreviousColor() {
        return prefs.getInt("notification_option_previous_color", -1);
    }

    public static void setUseNotificationGame(boolean use) {
        prefs.edit().putBoolean("use_notification_game", use).apply();
    }

    public static boolean useNotificationGame() {
        return prefs.getBoolean("use_notification_game", false);
    }

    public static boolean useEffects() {
        return prefs.getBoolean("use_effects", false);
    }

    public static boolean useToastEffects() {
        return prefs.getBoolean("use_toast_effects", true);
    }

    public static boolean useRandomEffects() {
        return prefs.getBoolean("use_random_effects", false);
    }

    public static String getRandomEffect() {
        return prefs.getString("random_effect", "None");
    }

    public static void setRandomEffect(String value) {
        prefs.edit().putString("random_effect", value).apply();
    }

    public static boolean useDuotoneGray() {
        return prefs.getBoolean("use_duotone_gray", false);
    }

    public static void setDuotoneColor(int num, int color) {
        prefs.edit().putInt("duotone_color_" + num, color).apply();
    }

    public static int getDuotoneColor(int num) {
        return prefs.getInt("duotone_color_" + num, -1);
    }

    public static void setEffect(String key, int value) {
        prefs.edit().putInt(key, value).apply();
        Log.i("AS", "Effect set: " + key + " Value: " + value);
    }

    public static int getEffectValue(String key) {

        if (key.equals("effect_brightness") || key.equals("effect_contrast") || key.equals(
                "effect_saturate") || key.equals("effects_frequency")) {
            return prefs.getInt(key, 100);
        }
        else if (key.equals("effect_temperature")) {
            return prefs.getInt(key, 50);
        }
        return prefs.getInt(key, 0);
    }

    public static float getEffectsFrequency() {
        return (float) prefs.getInt("effects_frequency", 100) / 100;
    }

    public static float getRandomEffectsFrequency() {
        return (float) prefs.getInt("random_effects_frequency", 100) / 100;
    }

    public static boolean useEffectsOverride() {
        return prefs.getBoolean("use_effects_override", false);
    }

    public static void setBlurRadius(int radius) {
        prefs.edit().putInt("blur_radius", radius).apply();
    }

    public static int getBlurRadius() {
        return prefs.getInt("blur_radius", 0);
    }

    public static float getAutoFixEffect() {
        return (float) prefs.getInt("effect_auto_fix", 0) / 100;
    }

    public static float getBrightnessEffect() {
        return (float) prefs.getInt("effect_brightness", 100) / 100;
    }

    public static float getContrastEffect() {
        return (float) prefs.getInt("effect_contrast", 100) / 100;
    }

    public static boolean getCrossProcessEffect() {
        return prefs.getBoolean("effect_cross_process_switch", false);
    }

    public static boolean getDocumentaryEffect() {
        return prefs.getBoolean("effect_documentary_switch", false);
    }

    public static boolean getDuotoneEffect() {
        return prefs.getBoolean("effect_duotone_switch", false);
    }

    public static float getFillLightEffect() {
        return (float) prefs.getInt("effect_fill_light", 0) / 100;
    }

    public static float getFisheyeEffect() {
        return (float) prefs.getInt("effect_fisheye", 0) / 100;
    }

    public static float getGrainEffect() {
        return (float) prefs.getInt("effect_grain", 0) / 100;
    }

    public static boolean getGrayscaleEffect() {
        return prefs.getBoolean("effect_grayscale_switch", false);
    }

    public static boolean getLomoishEffect() {
        return prefs.getBoolean("effect_lomoish_switch", false);
    }

    public static boolean getNegativeEffect() {
        return prefs.getBoolean("effect_negative_switch", false);
    }

    public static boolean getPosterizeEffect() {
        return prefs.getBoolean("effect_posterize_switch", false);
    }

    public static float getSaturateEffect() {
        return (float) prefs.getInt("effect_saturate", 100) / 100 - 1.0f;
    }

    public static boolean getSepiaEffect() {
        return prefs.getBoolean("effect_sepia_switch", false);
    }

    public static float getSharpenEffect() {
        return (float) prefs.getInt("effect_sharpen", 0) / 100;
    }

    public static float getTemperatureEffect() {
        return (float) prefs.getInt("effect_temperature", 50) / 100;
    }

    public static float getVignetteEffect() {
        return (float) prefs.getInt("effect_vignette", 0) / 100;
    }

    public static boolean useGoogleAccount() {
        return prefs.getBoolean("use_google_account", false);
    }

    public static void setUseGoogleAccount(boolean use) {
        prefs.edit().putBoolean("use_google_account", use).commit();
    }

    public static String getGoogleAccountName() {
        return prefs.getString("google_account_name", "");
    }

    public static void setGoogleAccountName(String name) {
        prefs.edit().putString("google_account_name", name).commit();
    }

    public static String getGoogleAccountToken() {
        return prefs.getString("google_account_token", "");
    }

    public static void setGoogleAccountToken(String token) {
        prefs.edit().putString("google_account_token", token).commit();
    }

    public static boolean useDropboxAccount() {
        return prefs.getBoolean("use_dropbox_account", false);
    }

    public static void setUseDropboxAccount(boolean use) {
        prefs.edit().putBoolean("use_dropbox_account", use).commit();
    }

    public static String getDropboxAccountToken() {
        return prefs.getString("dropbox_account_token", "");
    }

    public static void setDropboxAccountToken(String token) {
        prefs.edit().putString("dropbox_account_token", token).commit();
    }

    public static boolean useGoogleDriveAccount() {
        return prefs.getBoolean("use_google_drive_account", false);
    }

    public static void setUseGoogleDriveAccount(boolean use) {
        prefs.edit().putBoolean("use_google_drive_account", use).commit();
    }

    public static void setTimeType(String type) {
        prefs.edit().putString(WearConstants.TIME_TYPE, type).apply();
    }

    public static String getTimeType() {
        return prefs.getString(WearConstants.TIME_TYPE, DIGITAL);
    }

    public static boolean useSyncImage() {
        return prefs.getBoolean("use_sync_image", false);
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
        return prefs.getFloat(WearConstants.TICK_WIDTH, 1.0f);
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
        return prefs.getFloat(WearConstants.TICK_LENGTH_RATIO, 20f);
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

    public static long getLastDownloadTime() {
        return prefs.getLong("last_download_time", -1);
    }

    public static void setLastDownloadTime(long timeMillis) {
        prefs.edit().putLong("last_download_time", timeMillis).apply();
    }

    public static void setDriveAccountName(String accountName) {
        prefs.edit().putString("drive_account_name", accountName).commit();
    }

    public static String getDriveAccountName() {
        return prefs.getString("drive_account_name", "");
    }
}