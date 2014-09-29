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

import cw.kop.autobackground.settings.AppSettings;

public class WallpaperSettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {

	private final static long CONVERT_MILLES_TO_MIN = 60000;
	private SwitchPreference intervalPref;
    private EditTextPreference frameRatePref;
    private Context appContext;
    private PendingIntent pendingIntent;
	private AlarmManager alarmManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_wallpaper);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        appContext = activity;
        Intent intent = new Intent();
        intent.setAction(LiveWallpaperService.UPDATE_WALLPAPER);
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        pendingIntent = PendingIntent.getBroadcast(appContext, 0, intent, 0);

        alarmManager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
    }

    @Override
    public void onDetach() {
        appContext = null;
        super.onDetach();
    }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        frameRatePref = (EditTextPreference) findPreference("animation_frame_rate");
		intervalPref = (SwitchPreference) findPreference("use_interval");

        final Context contextThemeWrapper = new ContextThemeWrapper(getActivity(), AppSettings.getTheme());

        LayoutInflater localInflater = inflater.cloneInContext(contextThemeWrapper);

        return localInflater.inflate(R.layout.fragment_list, container, false);
	}

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        frameRatePref.setSummary(AppSettings.getAnimationFrameRate() + " FPS");

        if (!AppSettings.useAdvanced()) {
            PreferenceCategory wallpaperPreferences = (PreferenceCategory) findPreference("title_wallpaper_settings");

            wallpaperPreferences.removePreference(findPreference("preserve_context"));

            PreferenceCategory intervalPreferences = (PreferenceCategory) findPreference("title_interval_settings");

            intervalPreferences.removePreference(findPreference("force_interval"));
            intervalPreferences.removePreference(findPreference("when_locked"));

            PreferenceCategory animationPreferences = (PreferenceCategory) findPreference("title_animation_settings");

            animationPreferences.removePreference(frameRatePref);
            animationPreferences.removePreference(findPreference("animation_safety_adv"));
            animationPreferences.removePreference(findPreference("scale_animation_speed"));

            PreferenceCategory transitionPreferences = (PreferenceCategory) findPreference("title_transition_settings");

            transitionPreferences.removePreference(findPreference("reverse_spin_in"));
            transitionPreferences.removePreference(findPreference("spin_in_angle"));
            transitionPreferences.removePreference(findPreference("reverse_spin_out"));
            transitionPreferences.removePreference(findPreference("spin_out_angle"));
        }

        if (AppSettings.getIntervalDuration() > 0) {
            intervalPref.setSummary("Change every " + (AppSettings.getIntervalDuration() / CONVERT_MILLES_TO_MIN) + " minutes");
        }
    }

    @Override
	public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        
    }
	
	private void showDialogIntervalMenu() {

        AppSettings.setIntervalDuration(0);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(appContext);

        dialogBuilder.setItems(R.array.interval_entry_menu, new DialogInterface.OnClickListener() {
			
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

        AlertDialog dialog = dialogBuilder.create();

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

    private void showDialogIntervalForInput() {

        AppSettings.setIntervalDuration(0);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(appContext);
        dialogBuilder.setMessage("Update Interval");

        View dialogView = View.inflate(appContext, R.layout.numeric_dialog, null);

        dialogBuilder.setView(dialogView);

        final EditText inputField = (EditText) dialogView.findViewById(R.id.input_field);

        inputField.setHint("Enter number of minutes");

        dialogBuilder.setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {
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
        dialogBuilder.setNegativeButton(R.string.cancel_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                AppSettings.setIntervalDuration(0);
                intervalPref.setChecked(false);
            }
        });

        AlertDialog dialog = dialogBuilder.create();

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
		
		if (!((Activity) appContext).isFinishing()) {
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

            if (key.equals("animation_frame_rate")) {
                frameRatePref.setSummary(AppSettings.getAnimationFrameRate() + " FPS");
            }
		}
		
	}
	
}
