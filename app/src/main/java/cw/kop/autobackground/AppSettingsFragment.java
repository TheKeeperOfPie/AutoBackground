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
import android.content.Context;
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
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;

import cw.kop.autobackground.settings.AppSettings;
import cw.kop.autobackground.tutorial.TutorialActivity;
import io.fabric.sdk.android.Fabric;

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

        PreferenceCategory preferenceCategory = (PreferenceCategory) findPreference(
                "title_app_settings");

        Preference clearPref = findPreference("clear_pref");
        clearPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (appContext != null) {

                    DialogFactory.ActionDialogListener listener = new DialogFactory.ActionDialogListener() {

                        @Override
                        public void onClickMiddle(View v) {
//                            AppSettings.debugVer2_00();
                            AppSettings.setUseTutorial(true);
                            this.dismissDialog();
                        }

                        @Override
                        public void onClickRight(View v) {
                            AppSettings.clearPrefs(appContext);
                            if (AppSettings.useToast()) {
                                Toast.makeText(appContext,
                                        "Resetting settings to default",
                                        Toast.LENGTH_SHORT).show();
                            }
                            this.dismissDialog();
                            restartActivity();
                        }
                    };

                    DialogFactory.showActionDialog(appContext,
                            "Reset All Settings?",
                            "This cannot be undone.",
                            listener,
                            -1,
                            R.string.cancel_button,
                            R.string.ok_button);

                }
                return true;
            }
        });

        themePref = findPreference("change_theme");
        themePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                showThemeDialogMenu();
                return true;
            }
        });

        setThemePrefSummary();

        Preference tutorialPref = findPreference("show_tutorial_source");
        tutorialPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Toast.makeText(appContext, "Showing tutorial...", Toast.LENGTH_SHORT).show();
                Intent tutorialIntent = new Intent(appContext, TutorialActivity.class);
                startActivityForResult(tutorialIntent, TutorialActivity.TUTORIAL_REQUEST);
                return true;
            }
        });

        toastPref = (SwitchPreference) findPreference("use_toast");

        if (!AppSettings.useAdvanced()) {
            preferenceCategory.removePreference(toastPref);
            preferenceCategory.removePreference(findPreference("force_multi_pane"));
        }

        return inflater.inflate(R.layout.fragment_list, container, false);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == TutorialActivity.TUTORIAL_REQUEST) {
            restartActivity();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    private void showThemeDialogMenu() {

        DialogFactory.ListDialogListener clickListener = new DialogFactory.ListDialogListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
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

                restartActivity();
                dismissDialog();
            }
        };

        DialogFactory.showListDialog(appContext,
                "Theme:",
                clickListener,
                R.array.theme_entry_menu);
    }

    private void setThemePrefSummary() {
        themePref.setSummary(AppSettings.getTheme());
    }

    private void showToastDialog() {

        DialogFactory.ActionDialogListener clickListener = new DialogFactory.ActionDialogListener() {
            @Override
            public void onClickMiddle(View v) {
                toastPref.setChecked(true);
                this.dismissDialog();
            }

            @Override
            public void onClickRight(View v) {
                toastPref.setChecked(false);
                this.dismissDialog();
            }

        };

        DialogFactory.showActionDialog(appContext,
                "Are you sure you want to disable toast messages?",
                "You will not be notified of errors or info about the app.",
                clickListener,
                -1,
                R.string.cancel_button,
                R.string.ok_button);

    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        ((CustomSwitchPreference) findPreference("use_fabric")).setChecked(AppSettings.useFabric());
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

    private void restartActivity() {
        Intent intent = new Intent(appContext, MainActivity.class);
        intent.putExtra("fragment", 6);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        appContext.startActivity(intent);
        getActivity().finish();
        getActivity().overridePendingTransition(0, 0);
    }

}