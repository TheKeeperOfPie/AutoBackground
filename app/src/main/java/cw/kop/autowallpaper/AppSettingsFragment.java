package cw.kop.autowallpaper;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import cw.kop.autowallpaper.settings.AppSettings;

public class AppSettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {

	private Context context;
    private SwitchPreference toastPref;
    private Preference themePref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_app);
    }
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        Preference clearPref = findPreference("clear_pref");
        clearPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (context != null) {
                    AppSettings.clearPrefs(context);
                    if (AppSettings.useToast()) {
                        Toast.makeText(context, "Resetting settings to default", Toast.LENGTH_SHORT).show();
                    }
                }
                return false;
            }
        });

        themePref = findPreference("change_theme");
        String themeName = getResources().getResourceName(AppSettings.getTheme());
        themePref.setSummary("Theme: " + themeName.substring(themeName.indexOf("Fragment") + 8, themeName.indexOf("Theme")));
        themePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                showThemeDialogMenu();
                return false;
            }
        });

        Preference tutorialPref = findPreference("show_tutorial");
        tutorialPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AppSettings.setTutorial(true);
                Toast.makeText(context, "Will reshow tutorial", Toast.LENGTH_SHORT).show();
                return false;
            }
        });

        toastPref = (SwitchPreference) findPreference("use_toast");

        final Context contextThemeWrapper = new ContextThemeWrapper(getActivity(), AppSettings.getTheme());

        LayoutInflater localInflater = inflater.cloneInContext(contextThemeWrapper);

        return localInflater.inflate(R.layout.fragment_list, container, false);
		
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		context = getActivity();
	}

    private void showThemeDialogMenu() {

        int themeId;

        if(AppSettings.getTheme() == R.style.FragmentLightTheme) {
            themeId = R.style.LightDialogTheme;
        }
        else {
            themeId = R.style.DarkDialogTheme;
        }

        AlertDialog.Builder dialog = new AlertDialog.Builder(context, R.style.DarkDialogTheme);

        dialog.setItems(R.array.theme_entry_menu, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                switch (which) {
                    case 0:
                        AppSettings.setTheme(R.style.FragmentLightTheme);
                        break;
                    case 1:
                        AppSettings.setTheme(R.style.FragmentDarkTheme);
                        break;
                    case 2:
                        AppSettings.setTheme(R.style.FragmentTransparentTheme);
                        break;
                    default:
                }

                ((MainPreferences) getActivity()).setAppTheme();
                String themeName = getResources().getResourceName(AppSettings.getTheme());
                themePref.setSummary("Theme: " + themeName.substring(themeName.indexOf("Fragment") + 8, themeName.indexOf("Theme")));

            }
        });

        dialog.create();
        dialog.show();
    }

    private void showToastDialog() {

        int themeId;

        if(AppSettings.getTheme() == R.style.FragmentLightTheme) {
            themeId = R.style.LightDialogTheme;
        }
        else {
            themeId = R.style.DarkDialogTheme;
        }

        AlertDialog.Builder dialog = new AlertDialog.Builder(context, themeId);

        dialog.setTitle("Are you sure you want to disable toast messages?");
        dialog.setMessage("You will not be notified of errors or info about the app.");

        dialog.setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                toastPref.setChecked(false);
            }
        });
        dialog.setNegativeButton(R.string.cancel_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                toastPref.setChecked(true);
            }
        });

        dialog.show();
    }

	@Override
	public void onResume() {
		super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}
	
	@Override
	public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

		if (!((Activity) context).isFinishing()) {
			Preference pref = findPreference(key);
			
			if (key.equals("use_notification")) {
				
				Intent intent = new Intent();
				intent.setAction(LiveWallpaperService.UPDATE_NOTIFICATION);
				intent.putExtra("use", ((SwitchPreference) pref).isChecked());
				context.sendBroadcast(intent);
				
				Log.i("WSF", "Toggle Notification");
				
			}

            if (key.equals("use_toast")) {
                if (!AppSettings.useToast()) {
                    showToastDialog();
                }
            }
		}
		
	}
	
}
