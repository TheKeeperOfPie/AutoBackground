package cw.kop.autowallpaper;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import cw.kop.autowallpaper.settings.AppSettings;

public class WallpaperSettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener{

	private final static long CONVERT_MILLES_TO_MIN = 60000;
	private SwitchPreference intervalPref;
	private Context context;
    private PendingIntent pendingIntent;
	private AlarmManager alarmManager;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_wallpaper);
    }
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		intervalPref = (SwitchPreference) findPreference("use_interval");

		return super.onCreateView(inflater, container, savedInstanceState);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		context = getActivity();
        Intent intent = new Intent();
		intent.setAction(LiveWallpaperService.UPDATE_WALLPAPER);
		intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
		pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
		
		alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
	}

	
	@Override
	public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        
        onSharedPreferenceChanged(getPreferenceScreen().getSharedPreferences(), "");
        
        SharedPreferences sp = getPreferenceScreen().getSharedPreferences();
        EditTextPreference widthPref = (EditTextPreference) findPreference("user_width");
        widthPref.setSummary("Minimum Width of Image: " + sp.getString("user_width", "1000"));
        EditTextPreference heightPref = (EditTextPreference) findPreference("user_height");
        heightPref.setSummary("Minimum Height of Image: " + sp.getString("user_height", "1000"));

        if (AppSettings.getIntervalDuration() > 0) {
            intervalPref.setSummary("Change every " + (AppSettings.getIntervalDuration() / CONVERT_MILLES_TO_MIN) + " minutes");
        }
        
    }
	
	private void showDialogIntervalMenu() {

        int themeId;

        if(AppSettings.getTheme() == R.style.AppLightTheme) {
            themeId = R.style.LightDialogTheme;
        }
        else {
            themeId = R.style.DarkDialogTheme;
        }

        AppSettings.setIntervalDuration(0);

        AlertDialog.Builder dialog = new AlertDialog.Builder(context, themeId);
		
		dialog.setItems(R.array.interval_entry_menu, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				
				switch (which) {
					case 0: 
						AppSettings.setIntervalDuration(5 * CONVERT_MILLES_TO_MIN);
						break;
					case 1: 
						AppSettings.setIntervalDuration(15 * CONVERT_MILLES_TO_MIN);
						break;
					case 2: 
						AppSettings.setIntervalDuration(30 * CONVERT_MILLES_TO_MIN);
						break;
					case 3: 
						AppSettings.setIntervalDuration(AlarmManager.INTERVAL_HOUR);
						break;
					case 4: 
						AppSettings.setIntervalDuration(2 * AlarmManager.INTERVAL_HOUR);
						break;
					case 5: 
						AppSettings.setIntervalDuration(6 * AlarmManager.INTERVAL_HOUR);
						break;
					case 6: 
						AppSettings.setIntervalDuration(AlarmManager.INTERVAL_HALF_DAY);
						break;
					default:
				}

		        if (AppSettings.getIntervalDuration() > 0) {
		        	intervalPref.setSummary("Change every " + (AppSettings.getIntervalDuration() / CONVERT_MILLES_TO_MIN) + " minutes");
		        }
		        
				setIntervalAlarm();
		        
			}
		});
		
		dialog.setOnDismissListener(new OnDismissListener () {

			@Override
			public void onDismiss(DialogInterface dialog) {
				if (AppSettings.getIntervalDuration() <= 0) {
					intervalPref.setChecked(false);
				}
			}
			
		});

		dialog.create();
		dialog.show();
	}

    private void showDialogIntervalForInput() {

        int themeId;

        if(AppSettings.getTheme() == R.style.AppLightTheme) {
            themeId = R.style.LightDialogTheme;
        }
        else {
            themeId = R.style.DarkDialogTheme;
        }

        AppSettings.setIntervalDuration(0);

        AlertDialog.Builder dialog = new AlertDialog.Builder(context, themeId);
        dialog.setMessage("Update Interval");

        View dialogView = View.inflate(new ContextThemeWrapper(context, themeId), R.layout.numeric_dialog, null);

        dialog.setView(dialogView);

        final EditText inputField = (EditText) dialogView.findViewById(R.id.input_field);

        inputField.setHint("Enter number of minutes");

        dialog.setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                if (inputField.getText().toString().equals("")) {
                    intervalPref.setChecked(false);
                    return;
                }
                AppSettings.setIntervalDuration(Integer.parseInt(inputField.getText().toString()) * CONVERT_MILLES_TO_MIN);
                setIntervalAlarm();
                intervalPref.setSummary("Change every " + (AppSettings.getIntervalDuration() / CONVERT_MILLES_TO_MIN) + " minutes");
            }
        });
        dialog.setNegativeButton(R.string.cancel_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                AppSettings.setIntervalDuration(0);
                intervalPref.setChecked(false);
            }
        });
        dialog.setOnDismissListener(new OnDismissListener () {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (AppSettings.getIntervalDuration() <= 0) {
                    intervalPref.setChecked(false);
                }
            }

        });

        dialog.show();
    }

	private void setIntervalAlarm() {
		
		if (AppSettings.useInterval() && AppSettings.getIntervalDuration() > 0) {
			alarmManager.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis() + AppSettings.getIntervalDuration(), AppSettings.getIntervalDuration(), pendingIntent);
			Log.i("WSD", "Interval Set: " + AppSettings.getIntervalDuration());
		}
		else {
			alarmManager.cancel(pendingIntent);
		}
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
			
			if (pref instanceof EditTextPreference) {
				EditTextPreference editPref = (EditTextPreference) pref;
				if (editPref.getText().equals("0") || editPref.getText().equals("")) {
					editPref.setText("1");
				}
				editPref.setSummary(editPref.getText());
			}
			
			if (key.equals("use_interval")) {
				if (AppSettings.useInterval()) {
                    if (AppSettings.useAdvanced()) {
                        showDialogIntervalForInput();
                    }
                    else {
                        showDialogIntervalMenu();
                    }
	    		}
				else {
					AppSettings.setIntervalDuration(0);
			        intervalPref.setSummary("Change image after certain period");
					setIntervalAlarm();
				}
				Log.i("WSF", "Interval Set: " + AppSettings.useInterval());
			}
		}
		
	}
	
}
