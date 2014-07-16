package cw.kop.autowallpaper.settings;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

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
            prefs.edit().putInt("num_sources", 1).commit();
            prefs.edit().putString("source_type_0", "website").commit();
            prefs.edit().putString("source_title_0", "Kai Lehnberg Photography").commit();
            prefs.edit().putString("source_data_0", "http://www.reddit.com/user/Ziphius/submitted/?sort=top").commit();
            prefs.edit().putString("source_num_0", "5").commit();
            prefs.edit().putBoolean("use_source_0", true).commit();
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
		return prefs.getInt("num_stored", 0);
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

	public static boolean changeOnReturn() {
		return prefs.getBoolean("on_return", true);
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

    public static int getAnimationFrameRate() {
        return Integer.parseInt(prefs.getString("animation_frame_rate", "30"));
    }

    public static boolean useFade() {
        return prefs.getBoolean("use_fade", true);
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
        int num;
        try {
            num = Integer.parseInt(prefs.getString("source_num_" + index, "0"));
        }
        catch (NumberFormatException e) {
            num = 0;
        }
        return num;
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


    public static boolean useEffects() {
        return prefs.getBoolean("use_effects", false);
    }

    public static boolean useToastEffects() {
        return prefs.getBoolean("use_toast_effects", true);
    }

    public static boolean useRandomEffects() {
        return prefs.getBoolean("use_random_effects", false);
    }

    public static void setRandomEffect(String value) {
        prefs.edit().putString("random_effect", value).apply();
    }

    public static String getRandomEffect() {
        return prefs.getString("random_effect", null);
    }

    public static boolean useDuotoneGray() {
        return prefs.getBoolean("use_duotone_gray", false);
    }

    public static void setEffect(String key, int value) {
        prefs.edit().putInt(key, value).apply();
        Log.i("AS", "Effect set: " + key + " Value: " + value);
    }

    public static int getEffectValue(String key) {

        if (key.equals("effect_brightness") || key.equals("effect_contrast") || key.equals("effect_saturate") || key.equals("effects_frequency")) {
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
}
