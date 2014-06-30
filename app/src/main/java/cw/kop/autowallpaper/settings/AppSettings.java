package cw.kop.autowallpaper.settings;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.SharedPreferences;

import cw.kop.autowallpaper.LiveWallpaperService;

public class AppSettings {

	public static SharedPreferences prefs = LiveWallpaperService.prefs;
	
	public AppSettings() {
	}
	
	public static void setPrefs(SharedPreferences preferences) {
		prefs = preferences;
	}

	public static void initPrefs(SharedPreferences preferences, Context context) {
		
		prefs = preferences;
		if (prefs.getBoolean("first_run", true)) {
			prefs.edit().putBoolean("first_run", false).commit();
			prefs.edit().putString("main_website", "http://reddit.com/r/aww").commit();
			prefs.edit().putString("user_width", "" + (WallpaperManager.getInstance(context).getDesiredMinimumWidth() / 2)).commit();
			prefs.edit().putString("user_height", "" + (WallpaperManager.getInstance(context).getDesiredMinimumHeight() / 2)).commit();
			
		}
		
	}
	
	public static void setUrl(String key, String url) {
		prefs.edit().putString(key, url).commit();
	}
	
	public static String getUrl(String key) {
		return prefs.getString(key, null);
	}
	
	public static boolean usePath() {
		return prefs.getBoolean("use_path", false);
	}
	
	public static void setDownloadPath(String path) {
		prefs.edit().putString("download_path", path).commit();
	}
	
	public static String getDownloadPath() {
		
		if (prefs.getString("download_path", null) != null) {

			File dir = new File(prefs.getString("download_path", null));
			
			if (dir.exists() && dir.isDirectory()){
				return dir.getAbsolutePath();
			}
		}
		
		return null;
	}
	
	public static int getWidth() {
		return Integer.parseInt(prefs.getString("user_width", "1000"));
	}
	
	public static int getHeight() {
		return Integer.parseInt(prefs.getString("user_height", "1000"));
	}
	
	public static int getNumImages(int index) {
		return Integer.parseInt(prefs.getString("num_images_" + index, "3"));
	}
//	
//	public static void setNumImages(String value) {
//		prefs.edit().putString("num_images", value).commit();
//	}
	
	public static int getNumStored() {
		return prefs.getInt("num_stored", 1);
	}
	
	public static void setNumStored(int value) {
		prefs.edit().putInt("num_stored", value).commit();
	}

	public static boolean shuffleImages() {
		return prefs.getBoolean("shuffle_images", true);
	}
	
	public static boolean forceDownload() {
		return prefs.getBoolean("force_download", false);
	}
	
	public static void setWebsites(ArrayList<HashMap<String, String>> listData) {
		
		prefs.edit().putInt("num_websites", listData.size()).commit();
		
		for (int i = 0; i < listData.size(); i++) {
			
			prefs.edit().putString("website_title_" + i, listData.get(i).get("title")).commit();
			prefs.edit().putString("website_url_" + i, listData.get(i).get("url")).commit();
			prefs.edit().putString("num_images_" + i, listData.get(i).get("num")).commit();
			prefs.edit().putBoolean("use_website_" + i, Boolean.valueOf(listData.get(i).get("use"))).commit();
			
		}
	}

	public static boolean useWebsite(int index) {
		return prefs.getBoolean("use_website_" + index, false);
	}
	
	public static int getNumWebsites() {
		return prefs.getInt("num_websites", 0);
	}
	
	public static String getWebsiteTitle(int index) {
		return prefs.getString("website_title_" + index, null);
	}
	
	public static String getWebsiteUrl(int index) {
		return prefs.getString("website_url_" + index, null);
	}

	public static boolean useNotification() {
		return prefs.getBoolean("use_notification", false);
	}
	
	public static void setUseTimer(boolean timer) {
		prefs.edit().putBoolean("use_timer", timer).commit();
	}
	
	public static Boolean useTimer() {
		return prefs.getBoolean("use_timer", false);
	}

	public static void setTimerDuration(long timer) {
		prefs.edit().putLong("timer_duration", timer).commit();
	}
	
	public static long getTimerDuration() {
		return prefs.getLong("timer_duration", 0);
	}

	public static boolean useInterval() {
		return prefs.getBoolean("use_interval", false);
	}
	
	public static void setIntervalDuration(long interval) {
		prefs.edit().putLong("interval_duration", interval).commit();
	}
	
	public static long getIntervalDuration() {
		return prefs.getLong("interval_duration", 0);
	}
	
	public static boolean keepImages() {
		return prefs.getBoolean("keep_images", true);
	}
	
	public static boolean fillImages() {
		return prefs.getBoolean("fill_images", true);
	}
	
	public static boolean useWifi() {
		return prefs.getBoolean("use_wifi", true);
	}
	
	public static boolean useMobile() {
		return prefs.getBoolean("use_mobile", false);
	}

	public static boolean useHighQuality() {
		return prefs.getBoolean("use_high_quality", true);
	}

	public static int getAffinity(int i) {
		// TODO Auto-generated method stub
		return 0;
	}

	public static boolean changeOnLeave() {
		return prefs.getBoolean("on_leave", true);
	}

	public static boolean forceInterval() {
		return prefs.getBoolean("force_interval", false);
	}
	
	public static boolean forceMultipane() {
		return prefs.getBoolean("force_multipane", false);
	}

	public static boolean useAnimation() {
		return prefs.getBoolean("use_animation", false);
	}

	public static int getAnimationSpeed() {
		return prefs.getInt("animation_speed", 1);
	}
	
	public static boolean useTransparentTheme() {
		return prefs.getBoolean("use_transparent_theme", false);
	}

}
