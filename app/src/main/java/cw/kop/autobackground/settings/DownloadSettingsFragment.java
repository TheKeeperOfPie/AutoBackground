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

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.Environment;
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
import android.widget.TimePicker;
import android.widget.Toast;

import java.io.File;
import java.util.Calendar;

import cw.kop.autobackground.DialogFactory;
import cw.kop.autobackground.LiveWallpaperService;
import cw.kop.autobackground.R;
import cw.kop.autobackground.files.FileHandler;
import cw.kop.autobackground.images.FolderFragment;
import cw.kop.autobackground.images.LocalImageAdapter;

public class DownloadSettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {

    private final static long CONVERT_MILLES_TO_MIN = 60000;
    private final static long CONVERT_MIN_TO_DAY = 1440;
    private final static int REQUEST_FILE_ID = 0;
    private SwitchPreference timerPref;
    private Preference startTimePref;
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        timerPref = (SwitchPreference) findPreference("use_timer");
        startTimePref = findPreference("timer_time");

        Preference deletePref = findPreference("delete_images");
        deletePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                DialogFactory.ActionDialogListener clickListener = new DialogFactory.ActionDialogListener() {

                    @Override
                    public void onClickRight(View v) {
                        FileHandler.deleteAllBitmaps();
                        Toast.makeText(appContext,
                                "Deleted images with prefix\n" + AppSettings.getImagePrefix(),
                                Toast.LENGTH_SHORT).show();
                        this.dismissDialog();
                    }

                };

                DialogFactory.showActionDialog(appContext,
                        "Are you sure you want to delete all images?",
                        "This cannot be undone.",
                        clickListener,
                        -1,
                        R.string.cancel_button,
                        R.string.ok_button);

                return true;
            }
        });

        final Preference prefixPref = findPreference("image_prefix_adv");
        prefixPref.setSummary("Prefix: " + AppSettings.getImagePrefix());
        prefixPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                DialogFactory.InputDialogListener listener = new DialogFactory.InputDialogListener() {
                    @Override
                    public void onClickRight(View v) {
                        AppSettings.setImagePrefix(getEditTextString());
                        prefixPref.setSummary("Prefix: " + AppSettings.getImagePrefix());
                        dismissDialog();
                    }
                };

                DialogFactory.showInputDialog(appContext,
                        "Image Prefix",
                        "",
                        "" + AppSettings.getImagePrefix(),
                        listener,
                        -1,
                        R.string.cancel_button,
                        R.string.ok_button,
                        InputType.TYPE_CLASS_TEXT);


                return true;
            }
        });

        final Preference timePref = findPreference("timer_time");
        timePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                DialogFactory.TimeDialogListener listener = new DialogFactory.TimeDialogListener() {

                    @Override
                    public void onTimeSet(TimePicker view, int hour, int minute) {

                        AppSettings.setTimerHour(hour);
                        AppSettings.setTimerMinute(minute);
                        startTimePref.setSummary("Time to begin download timer: " + hour + ":" + String.format(
                                "%02d",
                                minute));
                        dismissDialog();
                        setDownloadAlarm();
                    }
                };

                DialogFactory.showTimeDialog(appContext,
                        "Time to start download:",
                        listener,
                        AppSettings.getTimerHour(),
                        AppSettings.getTimerMinute());

                return true;
            }
        });

        ((PreferenceCategory) findPreference("title_download_settings")).removePreference(
                findPreference("use_experimental_downloader_adv"));

        if (!AppSettings.useAdvanced()) {
            PreferenceCategory preferenceCategory = (PreferenceCategory) findPreference(
                    "title_download_settings");
            preferenceCategory.removePreference(findPreference("full_resolution"));
            preferenceCategory.removePreference(findPreference("reset_on_manual_download"));
            preferenceCategory.removePreference(findPreference("download_on_connection"));
            preferenceCategory.removePreference(findPreference("use_download_notification"));
            preferenceCategory.removePreference(findPreference("force_download"));
            preferenceCategory.removePreference(findPreference("use_download_path"));
            preferenceCategory.removePreference(findPreference("use_image_history"));
            preferenceCategory.removePreference(findPreference("image_history_size"));
            preferenceCategory.removePreference(findPreference("use_thumbnails"));
            preferenceCategory.removePreference(findPreference("thumbnail_size"));
            preferenceCategory.removePreference(findPreference("delete_old_images"));
            preferenceCategory.removePreference(findPreference("check_duplicates"));
            preferenceCategory.removePreference(findPreference("image_prefix_adv"));
            preferenceCategory.removePreference(findPreference("delete_images"));
        }

        return inflater.inflate(R.layout.fragment_list, container, false);

    }

    private void showDialogTimerMenu() {

        AppSettings.setTimerDuration(0);

        DialogFactory.ListDialogListener clickListener = new DialogFactory.ListDialogListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        AppSettings.setTimerDuration(AlarmManager.INTERVAL_HALF_DAY);
                        break;
                    case 1:
                        AppSettings.setTimerDuration(1 * AlarmManager.INTERVAL_DAY);
                        break;
                    case 2:
                        AppSettings.setTimerDuration(2 * AlarmManager.INTERVAL_DAY);
                        break;
                    case 3:
                        AppSettings.setTimerDuration(3 * AlarmManager.INTERVAL_DAY);
                        break;
                    case 4:
                        AppSettings.setTimerDuration(4 * AlarmManager.INTERVAL_DAY);
                        break;
                    case 5:
                        AppSettings.setTimerDuration(5 * AlarmManager.INTERVAL_DAY);
                        break;
                    case 6:
                        AppSettings.setTimerDuration(6 * AlarmManager.INTERVAL_DAY);
                        break;
                    case 7:
                        AppSettings.setTimerDuration(7 * AlarmManager.INTERVAL_DAY);
                        break;
                    default:
                }

                if (AppSettings.getTimerDuration() > 0) {
                    float days = (float) AppSettings.getTimerDuration() / CONVERT_MILLES_TO_MIN / CONVERT_MIN_TO_DAY;
                    timerPref.setSummary("Download every " + String.format("%.2f", days) + (days == 1 ? " day" : " days"));
                }

                setDownloadAlarm();
                dismissDialog();
            }

            @Override
            public void onDismiss() {
                if (AppSettings.getTimerDuration() <= 0) {
                    timerPref.setChecked(false);
                }
            }
        };

        DialogFactory.showListDialog(appContext,
                "Download Interval:",
                clickListener,
                R.array.timer_entry_menu);
    }

    private void showDialogTimerForInput() {

        AppSettings.setTimerDuration(0);

        DialogFactory.InputDialogListener listener = new DialogFactory.InputDialogListener() {

            @Override
            public void onClickMiddle(View v) {
                AppSettings.setTimerDuration(0);
                timerPref.setChecked(false);
                dismissDialog();
            }

            @Override
            public void onClickRight(View v) {

                String value = getEditTextString();

                if (value.equals("") || Long.parseLong(value) < 0) {
                    timerPref.setChecked(false);
                    dismissDialog();
                    return;
                }

                long inputValue = Long.parseLong(value);

                if (inputValue < 30L) {
                    inputValue = 30L;
                }

                AppSettings.setTimerDuration(inputValue * CONVERT_MILLES_TO_MIN);
                setDownloadAlarm();
                float days = (float) AppSettings.getTimerDuration() / CONVERT_MILLES_TO_MIN / CONVERT_MIN_TO_DAY;
                timerPref.setSummary("Download every " + String.format("%.2f", days) + (days == 1 ? " day" : " days"));
                dismissDialog();
            }

            @Override
            public void onDismiss() {
                if (AppSettings.getTimerDuration() <= 0) {
                    timerPref.setChecked(false);
                }
            }

        };

        DialogFactory.showInputDialog(appContext,
                "Download Interval",
                "Number of minutes",
                "",
                listener,
                -1,
                R.string.cancel_button,
                R.string.ok_button,
                InputType.TYPE_CLASS_NUMBER);
    }

    private void setDownloadAlarm() {

        if (AppSettings.useTimer() && AppSettings.getTimerDuration() > 0) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.set(Calendar.HOUR_OF_DAY, AppSettings.getTimerHour());
            calendar.set(Calendar.MINUTE, AppSettings.getTimerMinute());

            alarmManager.cancel(pendingIntent);

            if (calendar.getTimeInMillis() > System.currentTimeMillis()) {
                alarmManager.setInexactRepeating(AlarmManager.RTC,
                        calendar.getTimeInMillis(),
                        AppSettings.getTimerDuration(),
                        pendingIntent);
            }
            else {
                alarmManager.setInexactRepeating(AlarmManager.RTC,
                        calendar.getTimeInMillis() + AlarmManager.INTERVAL_DAY,
                        AppSettings.getTimerDuration(),
                        pendingIntent);
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

        final Preference widthPref = findPreference("user_width");
        widthPref.setSummary("Minimum Width of Image: " + AppSettings.getImageWidth());
        widthPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                DialogFactory.InputDialogListener listener = new DialogFactory.InputDialogListener() {
                    @Override
                    public void onClickRight(View v) {
                        AppSettings.setImageWidth(getEditTextString());
                        widthPref.setSummary("Minimum Width of Image: " + AppSettings.getImageWidth());
                        dismissDialog();
                    }
                };

                DialogFactory.showInputDialog(appContext,
                        "Minimum Width of Image:",
                        "Width in pixels",
                        "" + AppSettings.getImageWidth(),
                        listener,
                        -1,
                        R.string.cancel_button,
                        R.string.ok_button,
                        InputType.TYPE_CLASS_NUMBER);

                return true;
            }
        });
        final Preference heightPref = findPreference("user_height");
        heightPref.setSummary("Minimum Height of Image: " + AppSettings.getImageHeight());
        heightPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                DialogFactory.InputDialogListener listener = new DialogFactory.InputDialogListener() {
                    @Override
                    public void onClickRight(View v) {
                        AppSettings.setImageHeight(getEditTextString());
                        heightPref.setSummary("Minimum Height of Image: " + AppSettings.getImageHeight());
                        dismissDialog();
                    }
                };

                DialogFactory.showInputDialog(appContext,
                        "Minimum Height of Image:",
                        "Height in pixels",
                        "" + AppSettings.getImageHeight(),
                        listener,
                        -1,
                        R.string.cancel_button,
                        R.string.ok_button,
                        InputType.TYPE_CLASS_NUMBER);

                return true;
            }
        });


        if (AppSettings.useTimer() && AppSettings.getTimerDuration() > 0) {
            float days = (float) AppSettings.getTimerDuration() / CONVERT_MILLES_TO_MIN / CONVERT_MIN_TO_DAY;
            timerPref.setSummary("Download every " + String.format("%.2f", days) + (days == 1 ? " day" : " days"));
        }
        startTimePref.setSummary("Time to begin download timer: " + AppSettings.getTimerHour() + ":" + String.format(
                "%02d",
                AppSettings.getTimerMinute()));
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {


        if (!((Activity) appContext).isFinishing()) {

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
                    SwitchPreference timerPref = (SwitchPreference) findPreference(key);
                    timerPref.setSummary(getString(R.string.use_timer_description));
                    setDownloadAlarm();
                }
                Log.i("DSF", "Alarm Set: " + AppSettings.useTimer());
            }

            if (key.equals("use_download_path") && AppSettings.useDownloadPath()) {

                File rootDir = Environment.getExternalStorageDirectory();
                if (!rootDir.exists() || !rootDir.canRead()) {
                    rootDir = new File(File.separator);
                }
                final FolderFragment folderFragment = new FolderFragment();
                Bundle arguments = new Bundle();
                arguments.putBoolean(FolderFragment.SHOW_DIRECTORY_TEXT, true);
                arguments.putBoolean(FolderFragment.USE_DIRECTORY, true);
                final LocalImageAdapter adapter = new LocalImageAdapter(appContext, rootDir, rootDir);
                folderFragment.setArguments(arguments);
                folderFragment.setAdapter(adapter);
                folderFragment.setStartingDirectoryText(rootDir.getAbsolutePath());
                folderFragment.setListener(new FolderFragment.FolderEventListener() {
                    @Override
                    public void onUseDirectoryClick() {
                        AppSettings.setDownloadPath(adapter.getDirectory().getAbsolutePath());
                        Toast.makeText(appContext,
                                "Download path set to: \n" + AppSettings.getDownloadPath(),
                                Toast.LENGTH_SHORT).show();
                        adapter.setFinished();
                        getActivity().onBackPressed();
                    }

                    @Override
                    public void onItemClick(AdapterView<?> parent,
                            View view,
                            int positionInList,
                            long id) {
                        File selectedFile = adapter.getItem(positionInList);

                        if (selectedFile.exists() && selectedFile.isDirectory()) {
                            adapter.setDirectory(selectedFile);
                            folderFragment.setDirectoryText(adapter.getDirectory().getAbsolutePath());
                        }
                    }

                    @Override
                    public boolean onBackPressed() {
                        boolean endDirectory = adapter.backDirectory();
                        folderFragment.setDirectoryText(adapter.getDirectory().getAbsolutePath());

                        return endDirectory;
                    }
                });

                getFragmentManager().beginTransaction()
                        .add(R.id.content_frame, folderFragment, "folder_fragment")
                        .addToBackStack(null)
                        .commit();
            }

        }


    }

}
