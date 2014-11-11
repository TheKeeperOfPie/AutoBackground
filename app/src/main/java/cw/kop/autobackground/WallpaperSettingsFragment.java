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

package cw.kop.autobackground;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import cw.kop.autobackground.settings.AppSettings;

public class WallpaperSettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {

    private static final String TAG = WallpaperSettingsFragment.class.getName();
    private static final long CONVERT_MILLES_TO_MIN = 60000;
    private SwitchPreference intervalPref;
    private Preference frameRatePref;
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

        appContext = activity.getApplicationContext();

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        intervalPref = (SwitchPreference) findPreference("use_interval");
        frameRatePref = findPreference("animation_frame_rate");
        frameRatePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                DialogFactory.InputDialogListener listener = new DialogFactory.InputDialogListener() {
                    @Override
                    public void onClickRight(View v) {
                        AppSettings.setAnimationFrameRate(getEditTextString());
                        frameRatePref.setSummary(AppSettings.getAnimationFrameRate() + " FPS");
                        dismissDialog();
                    }
                };

                DialogFactory.showInputDialog(appContext,
                        "Frame rate",
                        "FPS",
                        "" + AppSettings.getAnimationFrameRate(),
                        listener,
                        -1,
                        R.string.cancel_button,
                        R.string.ok_button,
                        InputType.TYPE_CLASS_NUMBER);

                return true;
            }
        });

        final Preference animationBufferPref = findPreference("animation_safety_adv");
        animationBufferPref.setSummary("Side buffer: " + AppSettings.getAnimationSafety() + " pixels");
        animationBufferPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                DialogFactory.InputDialogListener listener = new DialogFactory.InputDialogListener() {
                    @Override
                    public void onClickRight(View v) {
                        AppSettings.setAnimationSafety(getEditTextString());
                        animationBufferPref.setSummary("Side buffer: " + AppSettings.getAnimationSafety() + " pixels");
                        dismissDialog();
                    }
                };

                DialogFactory.showInputDialog(appContext,
                        "Animation Buffer",
                        "Buffer in pixels",
                        "" + AppSettings.getAnimationSafety(),
                        listener,
                        -1,
                        R.string.cancel_button,
                        R.string.ok_button,
                        InputType.TYPE_CLASS_NUMBER);


                return true;
            }
        });

        frameRatePref.setSummary(AppSettings.getAnimationFrameRate() + " FPS");

        if (!AppSettings.useAdvanced()) {
            PreferenceCategory wallpaperPreferences = (PreferenceCategory) findPreference(
                    "title_wallpaper_settings");

            wallpaperPreferences.removePreference(findPreference("fill_images"));
            wallpaperPreferences.removePreference(findPreference("preserve_context"));
            wallpaperPreferences.removePreference(findPreference("scale_images"));
            wallpaperPreferences.removePreference(findPreference("show_album_art"));

            PreferenceCategory intervalPreferences = (PreferenceCategory) findPreference(
                    "title_interval_settings");


            intervalPreferences.removePreference(findPreference("reset_on_manual_cycle"));
            intervalPreferences.removePreference(findPreference("force_interval"));
            intervalPreferences.removePreference(findPreference("when_locked"));

            PreferenceCategory transitionPreferences = (PreferenceCategory) findPreference(
                    "title_transition_settings");

            transitionPreferences.removePreference(findPreference("transition_speed"));
            transitionPreferences.removePreference(findPreference("reverse_overshoot"));
            transitionPreferences.removePreference(findPreference("overshoot_intensity"));
            transitionPreferences.removePreference(findPreference("reverse_overshoot_vertical"));
            transitionPreferences.removePreference(findPreference("overshoot_intensity_vertical"));
            transitionPreferences.removePreference(findPreference("reverse_spin_in"));
            transitionPreferences.removePreference(findPreference("spin_in_angle"));
            transitionPreferences.removePreference(findPreference("reverse_spin_out"));
            transitionPreferences.removePreference(findPreference("spin_out_angle"));

            PreferenceCategory animationPreferences = (PreferenceCategory) findPreference(
                    "title_animation_settings");

            animationPreferences.removePreference(findPreference("animation_speed"));
            animationPreferences.removePreference(findPreference("animation_speed_vertical"));
            animationPreferences.removePreference(findPreference("scale_animation_speed"));
            animationPreferences.removePreference(frameRatePref);
            animationPreferences.removePreference(findPreference("animation_safety_adv"));

            getPreferenceScreen().removePreference(findPreference("title_gesture_settings"));

        }

        if (AppSettings.useInterval()) {
            if (AppSettings.getIntervalDuration() > 0) {
                intervalPref.setSummary("Change every " + (AppSettings.getIntervalDuration() / (float) CONVERT_MILLES_TO_MIN) + " minutes");
            }
            else {
                intervalPref.setSummary("Change on return");
            }
        }

        return inflater.inflate(R.layout.fragment_list, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

    }

    private void showDialogIntervalMenu() {

        AppSettings.setIntervalDuration(0);

        DialogFactory.ListDialogListener clickListener = new DialogFactory.ListDialogListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                switch (position) {
                    case 0:
                        AppSettings.setIntervalDuration(0);
                        break;
                    case 1:
                        AppSettings.setIntervalDuration(5 * CONVERT_MILLES_TO_MIN);
                        break;
                    case 2:
                        AppSettings.setIntervalDuration(15 * CONVERT_MILLES_TO_MIN);
                        break;
                    case 3:
                        AppSettings.setIntervalDuration(30 * CONVERT_MILLES_TO_MIN);
                        break;
                    case 4:
                        AppSettings.setIntervalDuration(AlarmManager.INTERVAL_HOUR);
                        break;
                    case 5:
                        AppSettings.setIntervalDuration(2 * AlarmManager.INTERVAL_HOUR);
                        break;
                    case 6:
                        AppSettings.setIntervalDuration(6 * AlarmManager.INTERVAL_HOUR);
                        break;
                    case 7:
                        AppSettings.setIntervalDuration(AlarmManager.INTERVAL_HALF_DAY);
                        break;
                    default:
                }

                if (AppSettings.getIntervalDuration() > 0) {
                    intervalPref.setSummary("Change every " + (AppSettings.getIntervalDuration() / (float) CONVERT_MILLES_TO_MIN) + " minutes");
                }
                else {
                    intervalPref.setSummary("Change on return");
                }

                setIntervalAlarm();

                setItemSelected(true);
                dismissDialog();
            }

            @Override
            public void onDismiss() {
                if (!getItemSelected()) {
                    intervalPref.setChecked(false);
                }
                super.onDismiss();
            }
        };

        DialogFactory.showListDialog(appContext,
                "Update Interval",
                clickListener,
                R.array.interval_entry_menu);
    }


    private void showDialogIntervalForInput() {

        DialogFactory.InputDialogListener listener = new DialogFactory.InputDialogListener() {
            @Override
            public void onClickMiddle(View v) {
                intervalPref.setChecked(false);
                dismissDialog();
            }

            @Override
            public void onClickRight(View v) {
                String value = getEditTextString();
                long inputValue = Long.parseLong(value);

                if (value.equals("") || inputValue < 0) {
                    intervalPref.setChecked(false);
                    return;
                }
                if (inputValue < 3000L && inputValue > 0) {
                    inputValue = 3000L;
                }

                AppSettings.setIntervalDuration(inputValue);
                setIntervalAlarm();
                if (inputValue == 0) {
                    intervalPref.setSummary("Change on return");
                }
                else {
                    intervalPref.setSummary("Change every " + (AppSettings.getIntervalDuration() / (float) CONVERT_MILLES_TO_MIN) + " minutes");
                }
                dismissDialog();
            }
        };

        DialogFactory.showInputDialog(appContext,
                "Update Interval",
                "Number of milliseconds",
                "",
                listener,
                -1,
                R.string.cancel_button,
                R.string.ok_button,
                InputType.TYPE_CLASS_NUMBER);
    }

    private void setIntervalAlarm() {

        if (AppSettings.useInterval() && AppSettings.getIntervalDuration() > 0) {
            alarmManager.setInexactRepeating(AlarmManager.RTC,
                    System.currentTimeMillis() + AppSettings.getIntervalDuration(),
                    AppSettings.getIntervalDuration(),
                    pendingIntent);
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
                    intervalPref.setSummary("Change image after certain period");
                    setIntervalAlarm();
                }
                Log.i("WSF", "Interval Set: " + AppSettings.useInterval());
            }

            if (key.equals("show_album_art")) {
                Intent musicReceiverIntent = new Intent(appContext, MusicReceiverService.class);

                if (AppSettings.showAlbumArt()) {
                    appContext.startService(musicReceiverIntent);
                    Log.i(TAG, "Starting MusicReceiverService");
                }
                else {
                    appContext.stopService(musicReceiverIntent);
                    Log.i(TAG, "Stopping MusicReceiverService");
                }
            }
        }
    }
}
