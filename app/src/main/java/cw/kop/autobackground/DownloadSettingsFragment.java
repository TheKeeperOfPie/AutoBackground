/*
 * Copyright (C) Winson Chiu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cw.kop.autobackground;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
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
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.Calendar;
import java.util.HashSet;

import cw.kop.autobackground.downloader.Downloader;
import cw.kop.autobackground.images.LocalImageFragment;
import cw.kop.autobackground.settings.AppSettings;

public class DownloadSettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {

	private final static long CONVERT_MILLES_TO_MIN = 60000;
	private final static int REQUEST_FILE_ID = 0;
	private SwitchPreference timerPref;
	private Context appContext;
    private PendingIntent pendingIntent;
	private AlarmManager alarmManager;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_download);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        appContext = getActivity();

        Intent intent = new Intent();
        intent.setAction(LiveWallpaperService.DOWNLOAD_WALLPAPER);
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        pendingIntent = PendingIntent.getBroadcast(appContext, 0, intent, 0);

        alarmManager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);

        Log.i("DSF", "onAttach");
    }

    @Override
    public void onDetach() {
        appContext = null;
        super.onDetach();
    }

    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		timerPref = (SwitchPreference) findPreference("use_timer");

        Preference deletePref = findPreference("delete_images");
        deletePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                AlertDialog.Builder dialog = new AlertDialog.Builder(appContext);

                dialog.setTitle("Are you sure you want to delete all images?");
                dialog.setMessage("This cannot be undone.");

                dialog.setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        Downloader.deleteAllBitmaps(appContext);
                        for (int i = 0; i < AppSettings.getNumSources(); i ++) {
                            if (AppSettings.getSourceType(i).equals("website")) {
                                AppSettings.setSourceSet(AppSettings.getSourceTitle(i), new HashSet<String>());
                            }
                        }
                        Toast.makeText(appContext, "Deleted images with prefix\n" + AppSettings.getImagePrefix(), Toast.LENGTH_SHORT).show();
                    }
                });
                dialog.setNegativeButton(R.string.cancel_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });

                dialog.show();
                return true;
            }
        });

        EditTextPreference prefixPref = (EditTextPreference) findPreference("image_prefix_adv");
        prefixPref.setSummary("Prefix: " + AppSettings.getImagePrefix());

        Preference timePref = findPreference("timer_time");
        timePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                TimePickerDialog timeDialog;
                timeDialog = new TimePickerDialog(appContext, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                        AppSettings.setTimerHour(selectedHour);
                        AppSettings.setTimerMinute(selectedMinute);
                        setDownloadAlarm();
                    }
                }, AppSettings.getTimerHour(), AppSettings.getTimerMinute(), true);
                timeDialog.setTitle("Select Time");
                timeDialog.show();

                return true;
            }
        });

        final Context contextThemeWrapper = new ContextThemeWrapper(getActivity(), AppSettings.getTheme());

        LayoutInflater localInflater = inflater.cloneInContext(contextThemeWrapper);

        return localInflater.inflate(R.layout.fragment_list, container, false);
		
	}

	private void showDialogTimerMenu() {

        AppSettings.setTimerDuration(0);

		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(appContext);

        dialogBuilder.setItems(R.array.timer_entry_menu, new DialogInterface.OnClickListener() {
			
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

		        if (AppSettings.getTimerDuration() > 0) {
		            timerPref.setSummary("Download every " + (AppSettings.getTimerDuration() / CONVERT_MILLES_TO_MIN) + " minutes");
		        }
		        
				setDownloadAlarm();
		        
			}
		});

        AlertDialog dialog = dialogBuilder.create();

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

    private void showDialogTimerForInput() {

        AppSettings.setTimerDuration(0);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(appContext);
        dialogBuilder.setMessage("Download Interval");

        View dialogView = View.inflate(appContext, R.layout.numeric_dialog, null);

        dialogBuilder.setView(dialogView);

        final EditText inputField = (EditText) dialogView.findViewById(R.id.input_field);

        inputField.setHint("Enter number of minutes");

        dialogBuilder.setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {
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
        dialogBuilder.setNegativeButton(R.string.cancel_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                AppSettings.setTimerDuration(0);
                timerPref.setChecked(false);
            }
        });

        AlertDialog dialog = dialogBuilder.create();

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
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.set(Calendar.HOUR_OF_DAY, AppSettings.getTimerHour());
            calendar.set(Calendar.MINUTE, AppSettings.getTimerMinute());

            alarmManager.cancel(pendingIntent);

            if (calendar.getTimeInMillis() > System.currentTimeMillis()) {
                alarmManager.setInexactRepeating(AlarmManager.RTC, calendar.getTimeInMillis(), AppSettings.getTimerDuration(), pendingIntent);
            }
            else {
                alarmManager.setInexactRepeating(AlarmManager.RTC, calendar.getTimeInMillis() + AlarmManager.INTERVAL_DAY, AppSettings.getTimerDuration(), pendingIntent);
            }

            Log.i("DSF", "Alarm Set: " + AppSettings.getTimerDuration());
		}
		else {
			alarmManager.cancel(pendingIntent);
		}
	}

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ((PreferenceCategory) findPreference("title_download_settings")).removePreference(findPreference("use_experimental_downloader_adv"));

        if (!AppSettings.useAdvanced()) {
            PreferenceCategory preferenceCategory = (PreferenceCategory) findPreference("title_download_settings");
            preferenceCategory.removePreference(findPreference("full_resolution"));
            preferenceCategory.removePreference(findPreference("use_download_notification"));
            preferenceCategory.removePreference(findPreference("use_high_quality"));
            preferenceCategory.removePreference(findPreference("force_download"));
            preferenceCategory.removePreference(findPreference("use_download_path"));
            preferenceCategory.removePreference(findPreference("image_history_size"));
            preferenceCategory.removePreference(findPreference("delete_old_images"));
            preferenceCategory.removePreference(findPreference("check_duplicates"));
            preferenceCategory.removePreference(findPreference("image_prefix_adv"));
        }

        EditTextPreference widthPref = (EditTextPreference) findPreference("user_width");
        widthPref.setSummary("Minimum Width of Image: " + AppSettings.getWidth());
        EditTextPreference heightPref = (EditTextPreference) findPreference("user_height");
        heightPref.setSummary("Minimum Height of Image: " + AppSettings.getHeight());

        if (AppSettings.useTimer() && AppSettings.getTimerDuration() > 0) {
            timerPref.setSummary("Download every " + (AppSettings.getTimerDuration() / CONVERT_MILLES_TO_MIN) + " minutes");
        }
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

		
		if (!((Activity) appContext).isFinishing()) {
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

            if (key.equals("use_download_path") && AppSettings.useDownloadPath()) {

                LocalImageFragment localImageFragment = new LocalImageFragment();
                Bundle arguments = new Bundle();
                arguments.putBoolean("change", true);
                arguments.putBoolean("set_path", true);
                arguments.putInt("position", 0);
                localImageFragment.setArguments(arguments);

                getFragmentManager().beginTransaction()
                        .add(R.id.content_frame, localImageFragment, "image_fragment")
                        .addToBackStack(null)
                        .commit();
            }

		}
		
		
	}
	
}
