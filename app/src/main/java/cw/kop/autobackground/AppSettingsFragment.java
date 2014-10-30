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
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import cw.kop.autobackground.settings.AppSettings;

public class AppSettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {

    private Context appContext;
    private SwitchPreference toastPref;
    private Preference themePref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_app);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        appContext = getActivity();
    }

    @Override
    public void onDetach() {
        appContext = null;
        super.onDetach();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_list, container, false);

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        PreferenceCategory preferenceCategory = (PreferenceCategory) findPreference("title_app_settings");
        preferenceCategory.removePreference(findPreference("force_multipane"));


        Preference clearPref = findPreference("clear_pref");
        clearPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (appContext != null) {
                    AppSettings.clearPrefs(appContext);
                    if (AppSettings.useToast()) {
                        Toast.makeText(appContext, "Resetting settings to default", Toast.LENGTH_SHORT).show();
                    }
                }
                return false;
            }
        });

        themePref = findPreference("change_theme");
        themePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                showThemeDialogMenu();
                return false;
            }
        });

        setThemePrefSummary();

        Preference tutorialPref = findPreference("show_tutorial_source");
        tutorialPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AppSettings.setTutorial(true, "source");
                Toast.makeText(appContext, "Will reshow source tutorial", Toast.LENGTH_SHORT).show();
                return false;
            }
        });

        toastPref = (SwitchPreference) findPreference("use_toast");
    }

    private void showThemeDialogMenu() {

        final Dialog dialog = AppSettings.getTheme().equals(AppSettings.APP_LIGHT_THEME) ? new Dialog(appContext, R.style.LightDialogTheme) : new Dialog(appContext, R.style.DarkDialogTheme);

        View dialogView = View.inflate(appContext, R.layout.list_dialog, null);
        dialog.setContentView(dialogView);

        TextView dialogTitle = (TextView) dialogView.findViewById(R.id.dialog_title);
        dialogTitle.setText("Theme:");

        ListView dialogList = (ListView) dialogView.findViewById(R.id.dialog_list);
        dialogList.setAdapter(new ArrayAdapter<>(appContext, android.R.layout.simple_list_item_1, android.R.id.text1, getResources().getStringArray(R.array.theme_entry_menu)));
        dialogList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int positionInList, long id) {

                switch (positionInList) {
                    case 0:
                        AppSettings.setTheme(R.style.AppLightTheme);
                        break;
                    case 1:
                        AppSettings.setTheme(R.style.AppDarkTheme);
                        break;
                    case 2:
                        AppSettings.setTheme(R.style.AppTransparentTheme);
                        break;
                    default:
                }

                setThemePrefSummary();

                Intent intent = new Intent(appContext, MainActivity.class);
                intent.putExtra("fragment", 6);
                appContext.startActivity(intent);

                getActivity().finish();
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    private void setThemePrefSummary() {
        themePref.setSummary(AppSettings.getTheme());
    }

    private void showToastDialog() {

        AlertDialog.Builder dialog = new AlertDialog.Builder(appContext);

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

        if (!((Activity) appContext).isFinishing()) {
            Preference pref = findPreference(key);

            if (key.equals("use_right_drawer")) {
                Intent intent = new Intent(appContext, MainActivity.class);
                intent.putExtra("fragment", 6);
                appContext.startActivity(intent);
                ((Activity) appContext).finish();
            }

            if (key.equals("use_notification")) {

                Intent intent = new Intent();
                intent.setAction(LiveWallpaperService.UPDATE_NOTIFICATION);
                intent.putExtra("use", ((SwitchPreference) pref).isChecked());
                appContext.sendBroadcast(intent);

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
