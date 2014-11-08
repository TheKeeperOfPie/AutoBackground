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

import android.annotation.SuppressLint;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cw.kop.autobackground.LiveWallpaperService;
import cw.kop.autobackground.R;

public class AppSettings {

    public static final String WEBSITE = "website";
    public static final String FOLDER = "folder";
    public static final String IMGUR = "imgur";
    public static final String PICASA = "picasa";
    public static final String TUMBLR_BLOG = "tumblr_blog";
    public static final String TUMBLR_TAG = "tumblr_tag";

    public static final String DATA_SPLITTER = ";break;";

    // Themes are user readable to easier set indicator using theme String
    public static final String APP_LIGHT_THEME = "Light Theme";
    public static final String APP_DARK_THEME = "Dark Theme";
    public static final String APP_TRANSPARENT_THEME = "Transparent Theme";

    private static SharedPreferences prefs = LiveWallpaperService.prefs;

    public AppSettings() {
    }

    public static void setPrefs(SharedPreferences preferences) {
        prefs = preferences;
    }

    private static boolean isFirstRun() {
        return prefs.getBoolean("first_run", true);
    }

    public static void setTutorial(boolean use, String name) {
        prefs.edit().putBoolean("tutorial_" + name, use).apply();
    }

    public static boolean useSourceTutorial() {
        return prefs.getBoolean("tutorial_source", true);
    }

    public static boolean useNotificationTutorial() {
        return prefs.getBoolean("tutorial_notification", true);
    }

    @SuppressLint("CommitPrefEdits")
    public static void initPrefs(SharedPreferences preferences, Context context) {
        prefs = preferences;
        if (isFirstRun()) {
            prefs.edit().putString("user_width",
                    "" + (WallpaperManager.getInstance(context).getDesiredMinimumWidth() / 2)).commit();
            prefs.edit().putString("user_height",
                    "" + (WallpaperManager.getInstance(context).getDesiredMinimumHeight() / 2)).commit();
            prefs.edit().putInt("num_sources", 1).commit();
            prefs.edit().putString("source_type_0", "website").commit();
            prefs.edit().putString("source_title_0", "Kai Lehnberg Photography").commit();
            prefs.edit().putString("source_data_0",
                    "http://www.reddit.com/user/Ziphius/submitted/?sort=top").commit();
            prefs.edit().putString("source_num_0", "5").commit();
            prefs.edit().putBoolean("use_source_0", true).commit();
            setNotificationOptionTitle(0, "Copy");
            setNotificationOptionTitle(1, "Cycle");
            setNotificationOptionTitle(2, "Delete");
            setNotificationOptionDrawable(0, R.drawable.ic_content_copy_white_24dp);
            setNotificationOptionDrawable(1, R.drawable.ic_refresh_white_24dp);
            setNotificationOptionDrawable(2, R.drawable.ic_delete_white_24dp);
            setTutorial(true, "source");
            setTutorial(true, "notification");
            prefs.edit().putBoolean("use_timer", true).commit();
            setTimerDuration(172800000);
            prefs.edit().putBoolean("first_run", false).commit();
        }
    }

    public static void versionUpdate() {

        if (prefs.getBoolean("version_update_1.0", true)) {

            FilenameFilter fileFilter = (new FilenameFilter() {

                @Override
                public boolean accept(File dir, String filename) {
                    return filename.endsWith(".png") || filename.endsWith(".jpg") || filename.endsWith(
                            ".jpg");
                }
            });

            String downloadCache = getDownloadPath();
            for (int i = 0; i < AppSettings.getNumSources(); i++) {
                if (AppSettings.getSourceType(i).equals(AppSettings.WEBSITE)) {
                    String title = AppSettings.getSourceTitle(i);
                    String titleTrimmed = title.replaceAll(" ", "");
                    File oldFolder = new File(downloadCache + "/" + titleTrimmed + AppSettings.getImagePrefix());
                    if (oldFolder.exists() && oldFolder.isDirectory()) {

                        File[] fileList = oldFolder.listFiles(fileFilter);

                        for (int index = 0; index < fileList.length; index++) {
                            if (fileList[index].getName().contains(title)) {
                                fileList[index].renameTo(new File(downloadCache + "/" + title + AppSettings.getImagePrefix() + "/" + title + " " + AppSettings.getImagePrefix() + index + ".png"));
                                Log.i("AS", "Renamed file");
                            }
                        }

                        oldFolder.renameTo(new File(downloadCache + "/" + title + " " + getImagePrefix()));

                        Log.i("AS", "Renamed");
                    }
                }
            }
            prefs.edit().putBoolean("version_update_1.0", false).commit();
        }
    }

    public static void setVersionUpdate(boolean update) {
        prefs.edit().putBoolean("version_update_1.0", true).apply();
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

    public static void clearPrefs(Context context) {
        prefs.edit().clear().commit();
        initPrefs(prefs, context);
    }

    public static void setUrl(String key, String url) {
        prefs.edit().putString(key, url).apply();
    }

    public static String getUrl(String key) {
        return prefs.getString(key, null);
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
            case R.style.AppTransparentTheme:
                prefs.edit().putString("application_theme",
                        AppSettings.APP_TRANSPARENT_THEME).commit();
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

        switch (getTheme()) {
            case APP_LIGHT_THEME:
                return context.getResources().getColor(R.color.DARK_GRAY_OPAQUE);
            case APP_DARK_THEME:
            case APP_TRANSPARENT_THEME:
                return context.getResources().getColor(R.color.LIGHT_GRAY_OPAQUE);
            default:
                return 0;
        }

    }

    public static int getBackgroundColorResource() {

        switch (getTheme()) {
            default:
            case APP_LIGHT_THEME:
                return R.color.LIGHT_THEME_BACKGROUND;
            case APP_DARK_THEME:
            case APP_TRANSPARENT_THEME:
                return R.color.DARK_THEME_BACKGROUND;
        }

    }

    public static int getImageWidth() {
        int width = 1000;

        try {
            width = Integer.parseInt(prefs.getString("user_width", "1000"));
        }
        catch (NumberFormatException e) {
            setImageHeight("" + width);
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
        return prefs.getBoolean("use_interval", false);
    }

    public static long getIntervalDuration() {
        return prefs.getLong("interval_duration", 0);
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

    public static boolean fillImages() {
        return prefs.getBoolean("fill_images", true);
    }

    public static boolean shuffleImages() {
        return prefs.getBoolean("shuffle_images", true);
    }

    public static boolean scaleImages() {
        return prefs.getBoolean("scale_images", true);
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

    public static boolean useHighQuality() {
        return prefs.getBoolean("use_high_quality", true);
    }

    public static boolean preserveContext() {
        return prefs.getBoolean("preserve_context", true);
    }

    public static boolean changeOnReturn() {
        return prefs.getBoolean("on_return", true);
    }

    public static boolean changeWhenLocked() {
        return prefs.getBoolean("when_locked", true);
    }

    public static boolean forceInterval() {
        return prefs.getBoolean("force_interval", false);
    }

    public static boolean forceMultipane() {
        return prefs.getBoolean("force_multipane", false);
    }

    public static boolean useAnimation() {
        return prefs.getBoolean("use_animation", true);
    }

    public static boolean useVerticalAnimation() {
        return prefs.getBoolean("use_animation_vertical", true);
    }

    public static float getAnimationSafety() {
        float buffer = 50;

        try {
            buffer = Float.parseFloat(prefs.getString("animation_safety_adv", "50"));
        }
        catch (NumberFormatException e) {
            setAnimationFrameRate("" + buffer);
        }

        return buffer;
    }

    public static void setAnimationSafety(String buffer) {
        prefs.edit().putString("animation_safety_adv", buffer).commit();
    }

    public static float getAnimationSpeed() {
        return ((float) prefs.getInt("animation_speed", 10) / 10);
    }

    public static float getVerticalAnimationSpeed() {
        return ((float) prefs.getInt("animation_speed_vertical", 10) / 10);
    }

    public static boolean scaleAnimationSpeed() {
        return prefs.getBoolean("scale_animation_speed", true);
    }

    public static int getAnimationFrameRate() {
        int rate = 30;

        try {
            rate =Integer.parseInt(prefs.getString("animation_frame_rate", "30"));
        }
        catch (NumberFormatException e) {
            setImageHeight("" + rate);
        }

        return rate;
    }

    public static void setAnimationFrameRate(String rate) {
        prefs.edit().putString("animation_frame_rate", rate).commit();
    }

    public static long getTransitionTime() {
        return (long) prefs.getInt("transition_speed", 20) * 100;
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

    public static float getOvershootIntensity() {
        return (float) prefs.getInt("overshoot_intensity", 10) / 10;
    }


    public static boolean useVerticalOvershoot() {
        return prefs.getBoolean("use_overshoot_vertical", false);
    }

    public static boolean reverseVerticalOvershoot() {
        return prefs.getBoolean("reverse_overshoot_vertical", false);
    }

    public static float getVerticalOvershootIntensity() {
        return (float) prefs.getInt("overshoot_intensity_vertical", 10) / 10;
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

    public static float getSpinInAngle() {
        return (float) prefs.getInt("spin_in_angle", 2700) / 10;
    }

    public static boolean useSpinOut() {
        return prefs.getBoolean("use_spin_out", false);
    }

    public static boolean reverseSpinOut() {
        return prefs.getBoolean("reverse_spin_out", false);
    }

    public static float getSpinOutAngle() {
        return (float) prefs.getInt("spin_out_angle", 2700) / 10;
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
        return prefs.getBoolean("extend_scale", false);
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
        return Integer.parseInt(prefs.getString("history_size", "5"));
    }

    public static boolean useHighResolutionNotificationIcon() {
        return prefs.getBoolean("high_resolution_notification_icon", false);
    }

    public static void setSources(ArrayList<HashMap<String, String>> listData) {

        prefs.edit().putInt("num_sources", listData.size()).commit();

        for (int i = 0; i < listData.size(); i++) {

            String type = listData.get(i).get("type");

            prefs.edit().putString("source_type_" + i, type).commit();
            prefs.edit().putString("source_title_" + i, listData.get(i).get("title")).commit();
            prefs.edit().putString("source_data_" + i, listData.get(i).get("data")).commit();
            prefs.edit().putString("source_num_" + i, listData.get(i).get("num")).commit();
            prefs.edit().putBoolean("use_source_" + i,
                    Boolean.valueOf(listData.get(i).get("use"))).commit();

        }
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

    public static void setSourceNumStored(int index, int num) {
        prefs.edit().putInt("source_num_stored_" + index, num).apply();
    }

    public static int getSourceNumStored(int index) {
        return prefs.getInt("source_num_stored_" + index, 0);
    }

    public static void setSourceSet(String title, HashSet<String> set) {
        prefs.edit().putStringSet("source_set_" + title, set).apply();
    }

    public static HashSet<String> getSourceSet(String title) {
        return (HashSet<String>) prefs.getStringSet("source_set_" + title, new HashSet<String>());
    }

    public static String getSourceType(int index) {
        return prefs.getString("source_type_" + index, null);
    }

    public static boolean useSource(int index) {
        return prefs.getBoolean("use_source_" + index, false);
    }

    public static int getNumSources() {
        return prefs.getInt("num_sources", 0);
    }

    public static String getSourceTitle(int index) {
        return prefs.getString("source_title_" + index, null);
    }

    public static String getSourceData(int index) {
        return prefs.getString("source_data_" + index, null);
    }

    public static boolean useImageHistory() {
        return prefs.getBoolean("use_image_history", true);
    }

    public static int getImageHistorySize() {
        return Integer.parseInt(prefs.getString("image_history_size", "250"));
    }

    public static boolean cacheThumbnails() {
        return prefs.getBoolean("use_thumbnails", true);
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
                        long first = Long.parseLong(lhs.substring(lhs.lastIndexOf("Time:")));
                        long second = Long.parseLong(rhs.substring(lhs.lastIndexOf("Time:")));

                        return (int) (first - second);
                    }
                    catch (Exception e) {

                    }

                    return 0;
                }
            });

            for (int i = 0; i < iterations; i++) {
                linkList.remove(0);
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

        prefs.edit().putString("notification_icon_string", value).apply();
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

            if (image.exists() && image.isFile() && (image.getAbsolutePath().contains(".png") || image.getAbsolutePath().contains(
                    ".jpg") || image.getAbsolutePath().contains(".jpeg"))) {
                return image.getAbsolutePath();
            }
        }

        return null;
    }

    public static void setNotificationIconFile(String filePath) {
        prefs.edit().putString("notification_icon_file", filePath).apply();
    }

    public static String getNotificationIconAction() {
        return prefs.getString("notification_icon_action", "None");
    }

    public static void setNotificationIconAction(String action) {
        prefs.edit().putString("notification_icon_action", action).apply();
    }

    public static int getNotificationIconActionDrawable() {

        String value = prefs.getString("notification_icon_action_drawable_string", "None");

        if (value.equals("Copy")) {
            return R.drawable.ic_content_copy_white_24dp;
        }
        if (value.equals("Cycle")) {
            return R.drawable.ic_refresh_white_24dp;
        }
        if (value.equals("Delete")) {
            return R.drawable.ic_delete_white_24dp;
        }
        if (value.equals("Open")) {
            return R.drawable.ic_photo_white_24dp;
        }
        if (value.equals("Pin")) {
            return R.drawable.ic_pin_drop_white_24dp;
        }
        if (value.equals("Previous")) {
            return R.drawable.ic_arrow_back_white_24dp;
        }
        if (value.equals("Share")) {
            return R.drawable.ic_share_white_24dp;
        }

        return R.color.TRANSPARENT_BACKGROUND;
    }

    public static void setNotificationIconActionDrawable(int drawable) {

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
        prefs.edit().putInt("notification_summary_color", color).apply();
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
        }

        prefs.edit().putString("notification_drawable_string" + position, value).apply();
    }

    public static int getNotificationOptionDrawable(int position) {

        String value = prefs.getString("notification_drawable_string" + position, "None");

        if (value.equals("Copy")) {
            return R.drawable.ic_content_copy_white_24dp;
        }
        if (value.equals("Cycle")) {
            return R.drawable.ic_refresh_white_24dp;
        }
        if (value.equals("Delete")) {
            return R.drawable.ic_delete_white_24dp;
        }
        if (value.equals("Open")) {
            return R.drawable.ic_photo_white_24dp;
        }
        if (value.equals("Pin")) {
            return R.drawable.ic_pin_drop_white_24dp;
        }
        if (value.equals("Previous")) {
            return R.drawable.ic_arrow_back_white_24dp;
        }
        if (value.equals("Share")) {
            return R.drawable.ic_share_white_24dp;
        }
        if (value.equals("None")) {
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
        return prefs.getString("random_effect", null);
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

    public static void setGoogleAccount(boolean use) {
        prefs.edit().putBoolean("use_google_account", true).apply();
    }

    public static String getGoogleAccountName() {
        return prefs.getString("google_account_name", "");
    }

    public static void setGoogleAccountName(String name) {
        prefs.edit().putString("google_account_name", name).apply();
    }

    public static String getGoogleAccountToken() {
        return prefs.getString("google_account_token", "");
    }

    public static void setGoogleAccountToken(String token) {
        prefs.edit().putString("google_account_token", token).apply();
    }

}
