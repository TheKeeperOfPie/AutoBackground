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
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import cw.kop.autowallpaper.settings.AppSettings;

public class DownloadSettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {

	private final static long CONVERT_MILLES_TO_MIN = 60000;
	private final static int REQUEST_FILE_ID = 0;
	private SwitchPreference timerPref;
	private Context context;
	private Intent intent;
	private PendingIntent pendingIntent;
	private AlarmManager alarmManager;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_download);
    }
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		timerPref = (SwitchPreference) findPreference("use_timer");

		return super.onCreateView(inflater, container, savedInstanceState);
		
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		context = getActivity();

		intent = new Intent();
		intent.setAction(LiveWallpaperService.DOWNLOAD_WALLPAPER);
		intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
		pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

		alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Log.i("DSF", "onAttach");
	}

	private void showDialogTimerMenu() {

        int themeId;

        if(AppSettings.getTheme() == R.style.AppLightTheme) {
            themeId = R.style.LightDialogTheme;
        }
        else {
            themeId = R.style.DarkDialogTheme;
        }

        AppSettings.setTimerDuration(0);

		AlertDialog.Builder dialog = new AlertDialog.Builder(context, themeId);
		
		dialog.setItems(R.array.timer_entry_menu, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				
				switch (which) {
					case 0: 
						AppSettings.setTimerDuration(AlarmManager.INTERVAL_HOUR);
						break;
					case 1: 
						AppSettings.setTimerDuration(2 * AlarmManager.INTERVAL_HOUR);
						break;
					case 2: 
						AppSettings.setTimerDuration(6 * AlarmManager.INTERVAL_HOUR);
						break;
					case 3: 
						AppSettings.setTimerDuration(AlarmManager.INTERVAL_HALF_DAY);
						break;
					case 4: 
						AppSettings.setTimerDuration(AlarmManager.INTERVAL_DAY);
						break;
					case 5: 
						AppSettings.setTimerDuration(2 * AlarmManager.INTERVAL_DAY);
						break;
					case 6: 
						AppSettings.setTimerDuration(4 * AlarmManager.INTERVAL_DAY);
						break;
					default:
				}
				
				Log.i("DSF", "Alarm Set: " + AppSettings.getTimerDuration());

		        if (AppSettings.getTimerDuration() > 0) {
		            timerPref.setSummary("Download every " + (AppSettings.getTimerDuration() / CONVERT_MILLES_TO_MIN) + " minutes");
		        }
		        
				setDownloadAlarm();
		        
			}
		});
		
		dialog.setOnDismissListener(new OnDismissListener () {

			@Override
			public void onDismiss(DialogInterface dialog) {
				if (AppSettings.getTimerDuration() <= 0) {
					timerPref.setChecked(false);
				}
			}
			
		});
		
		dialog.create();
		dialog.show();
	}

    private void showDialogTimerForInput() {

        int themeId;

        if(AppSettings.getTheme() == R.style.AppLightTheme) {
            themeId = R.style.LightDialogTheme;
        }
        else {
            themeId = R.style.DarkDialogTheme;
        }

        AppSettings.setTimerDuration(0);

        AlertDialog.Builder dialog = new AlertDialog.Builder(context, themeId);
        dialog.setMessage("Download Interval");

        View dialogView = View.inflate(new ContextThemeWrapper(context, themeId), R.layout.numeric_dialog, null);

        dialog.setView(dialogView);

        final EditText inputField = (EditText) dialogView.findViewById(R.id.input_field);

        inputField.setHint("Enter number of minutes");

        dialog.setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                if (inputField.getText().toString().equals("")) {
                    timerPref.setChecked(false);
                    return;
                }
                AppSettings.setTimerDuration(Integer.parseInt(inputField.getText().toString()) * CONVERT_MILLES_TO_MIN);
                setDownloadAlarm();
                timerPref.setSummary("Download every " + (AppSettings.getTimerDuration() / CONVERT_MILLES_TO_MIN) + " minutes");
            }
        });
        dialog.setNegativeButton(R.string.cancel_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                AppSettings.setTimerDuration(0);
                timerPref.setChecked(false);
            }
        });
        dialog.setOnDismissListener(new OnDismissListener () {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (AppSettings.getTimerDuration() <= 0) {
                    timerPref.setChecked(false);
                }
            }

        });

        dialog.show();
    }

	private void setDownloadAlarm() {
		
		if (AppSettings.useTimer() && AppSettings.getTimerDuration() > 0) {
			alarmManager.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis() + AppSettings.getTimerDuration(), AppSettings.getTimerDuration(), pendingIntent);
		}
		else {
			alarmManager.cancel(pendingIntent);
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        if (!AppSettings.useAdvanced()) {
            SwitchPreference experimentalPref = (SwitchPreference) findPreference("use_experimental_downloader_adv");
            ((PreferenceCategory) findPreference("title_download_settings")).removePreference(experimentalPref);
        }

        if (AppSettings.useTimer() && AppSettings.getTimerDuration() > 0) {
            timerPref.setSummary("Download every " + (AppSettings.getTimerDuration() / CONVERT_MILLES_TO_MIN) + " minutes");
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
			
			if (key.equals("use_timer")) {
				if (AppSettings.useTimer()) {
                    if (AppSettings.useAdvanced()) {
                        showDialogTimerForInput();
                    }
	        		else {
                        showDialogTimerMenu();
                    }
	    		}
				else {
					SwitchPreference timerPref = (SwitchPreference) pref;
					timerPref.setSummary(getString(R.string.use_timer_description));
					setDownloadAlarm();
				}
				Log.i("DSF", "Alarm Set: " + AppSettings.useTimer());
			}
		}
		
		
	}
	
}
