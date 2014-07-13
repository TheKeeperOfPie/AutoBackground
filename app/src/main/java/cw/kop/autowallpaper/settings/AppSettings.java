package cw.kop.autowallpaper.settings;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import cw.kop.autowallpaper.LiveWallpaperService;
import cw.kop.autowallpaper.R;

public class AppSettings {

	public static SharedPreferences prefs = LiveWallpaperService.prefs;
	
	public AppSettings() {
	}
	
	public static void setPrefs(SharedPreferences preferences) {
		prefs = preferences;
	}

    public static boolean isFirstRun() {
        return prefs.getBoolean("first_run", true);
    }

    public static void setTutorial(boolean tutorial) {
        prefs.edit().putBoolean("tutorial", tutorial).apply();
    }

    public static boolean useTutorial() {
        return prefs.getBoolean("tutorial", false);
    }

	public static void initPrefs(SharedPreferences preferences, Context context) {
		prefs = preferences;
        if (isFirstRun()) {
            prefs.edit().putString("user_width", "" + (WallpaperManager.getInstance(context).getDesiredMinimumWidth() / 2)).commit();
            prefs.edit().putString("user_height", "" + (WallpaperManager.getInstance(context).getDesiredMinimumHeight() / 2)).commit();
            setTutorial(true);
            prefs.edit().putBoolean("first_run", false).commit();
        }
	}

    public static void clearPrefs(Context context) {
        prefs.edit().clear().commit();
        prefs.edit().putString("user_width", "" + (WallpaperManager.getInstance(context).getDesiredMinimumWidth() / 2)).commit();
        prefs.edit().putString("user_height", "" + (WallpaperManager.getInstance(context).getDesiredMinimumHeight() / 2)).commit();
    }

	public static void setUrl(String key, String url) {
		prefs.edit().putString(key, url).apply();
	}
	
	public static String getUrl(String key) {
		return prefs.getString(key, null);
	}
	
	public static void setDownloadPath(String path) {
		prefs.edit().putString("download_path", path).apply();
	}

    public static int getTheme() {
        return prefs.getInt("app_theme", R.style.FragmentDarkTheme);
    }

    public static void setTheme(int theme) {
        prefs.edit().putInt("app_theme", theme).apply();
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
	
	public static int getNumStored() {
		return prefs.getInt("num_stored", 1);
	}
	
	public static void setNumStored(int value) {
		prefs.edit().putInt("num_stored", value).apply();
	}

	public static boolean shuffleImages() {
		return prefs.getBoolean("shuffle_images", true);
	}
	
	public static boolean forceDownload() {
		return prefs.getBoolean("force_download", false);
	}

	public static boolean useNotification() {
		return prefs.getBoolean("use_notification", false);
	}
	
	public static Boolean useTimer() {
		return prefs.getBoolean("use_timer", false);
	}

	public static void setTimerDuration(long timer) {
		prefs.edit().putLong("timer_duration", timer).apply();
	}
	
	public static long getTimerDuration() {
		return prefs.getLong("timer_duration", 0);
	}

	public static boolean useInterval() {
		return prefs.getBoolean("use_interval", false);
	}
	
	public static void setIntervalDuration(long interval) {
		prefs.edit().putLong("interval_duration", interval).apply();
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

    public static boolean useFade() {
        return prefs.getBoolean("use_fade", true);
    }

    public static boolean useEffects() {
        return prefs.getBoolean("use_effects", false);
    }

    public static int getBrightnessValue() {
        return prefs.getInt("effect_brightness", 0);
    }

    public static int getContrastValue() {
        return prefs.getInt("effect_contrast", 0);
    }

    public static boolean useExperimentalDownloader() {
        return prefs.getBoolean("use_experimental_downloader_adv", false);
    }

    public static boolean useAdvanced() {
        return prefs.getBoolean("use_advanced", false);
    }

    public static boolean useDoubleTap() {
        return prefs.getBoolean("double_tap", true);
    }

    public static boolean useToast() {
        return prefs.getBoolean("use_toast", true);
    }

    public static String getImagePrefix() {
        return prefs.getString("image_prefix_adv", "AutoBackground");
    }

    public static void setSources(ArrayList<HashMap<String, String>> listData) {

        prefs.edit().putInt("num_sources", listData.size()).commit();

        for (int i = 0; i < listData.size(); i++) {

            String type = listData.get(i).get("type");

            prefs.edit().putString("source_type_" + i, type).commit();
            prefs.edit().putString("source_title_" + i, listData.get(i).get("title")).commit();
            prefs.edit().putString("source_data_" + i, listData.get(i).get("data")).commit();
            prefs.edit().putString("source_num_" + i, listData.get(i).get("num")).commit();
            prefs.edit().putBoolean("use_source_" + i, Boolean.valueOf(listData.get(i).get("use"))).commit();

        }
    }

    public static int getSourceNum(int index) {
        return Integer.parseInt(prefs.getString("source_num_" + index, "0"));
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

}
