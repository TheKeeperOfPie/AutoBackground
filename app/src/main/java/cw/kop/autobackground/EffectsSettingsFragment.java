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
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.media.effect.EffectFactory;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import afzkl.development.colorpickerview.view.ColorPickerView;
import cw.kop.autobackground.settings.AppSettings;

public class EffectsSettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {

    private Context appContext;

    private SwitchPreference randomPref, duotonePref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_effects);
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        Preference resetPref = findPreference("reset_effects");
        resetPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                resetEffects();
                if (AppSettings.useToast()) {
                    Toast.makeText(appContext, "Reset effects", Toast.LENGTH_SHORT).show();
                }
                return false;
            }
        });

        randomPref = (SwitchPreference) findPreference("use_random_effects");
        if (AppSettings.useRandomEffects()) {
            randomPref.setSummary("Effect: " + AppSettings.getRandomEffect());
        }

        duotonePref = (SwitchPreference) findPreference("effect_duotone_switch");

//        final Context contextThemeWrapper = new ContextThemeWrapper(getActivity(), AppSettings.getTheme());
//
//        LayoutInflater localInflater = inflater.cloneInContext(contextThemeWrapper);

        return inflater.inflate(R.layout.fragment_list, container, false);

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (!AppSettings.useAdvanced()) {

            PreferenceCategory preferenceCategory = (PreferenceCategory) findPreference("title_effects_settings");

            preferenceCategory.removePreference(findPreference("use_effects_override"));
            preferenceCategory.removePreference(findPreference("use_toast_effects"));
        }

        PreferenceCategory parametersCategory = (PreferenceCategory) findPreference("title_effects_parameters");

        for(int i = 0; i < parametersCategory.getPreferenceCount(); i++) {
            String key = parametersCategory.getPreference(i).getKey();

            if(key != null && !key.contains("switch")) {
                EffectPreference effectPref = (EffectPreference) findPreference(key);
                effectPref.setSummary(effectPref.getTitle() + ": " + AppSettings.getEffectValue(key) + "%");
            }
        }

        if (!EffectFactory.isEffectSupported(EffectFactory.EFFECT_AUTOFIX)) {
            parametersCategory.removePreference(findPreference("effect_auto_fix"));
        }
        if (!EffectFactory.isEffectSupported(EffectFactory.EFFECT_BRIGHTNESS)) {
            parametersCategory.removePreference(findPreference("effect_brightness"));
        }
        if (!EffectFactory.isEffectSupported(EffectFactory.EFFECT_CONTRAST)) {
            parametersCategory.removePreference(findPreference("effect_contrast"));
        }
        if (!EffectFactory.isEffectSupported(EffectFactory.EFFECT_CROSSPROCESS)) {
            parametersCategory.removePreference(findPreference("effect_cross_process_switch"));
        }
        if (!EffectFactory.isEffectSupported(EffectFactory.EFFECT_DOCUMENTARY)) {
            parametersCategory.removePreference(findPreference("effect_documentary_switch"));
        }
        if (!EffectFactory.isEffectSupported(EffectFactory.EFFECT_DUOTONE)) {
            parametersCategory.removePreference(findPreference("effect_duotone_switch"));
        }
        if (!EffectFactory.isEffectSupported(EffectFactory.EFFECT_FILLLIGHT)) {
            parametersCategory.removePreference(findPreference("effect_fill_light"));
        }
        if (!EffectFactory.isEffectSupported(EffectFactory.EFFECT_FISHEYE)) {
            parametersCategory.removePreference(findPreference("effect_fisheye"));
        }
        if (!EffectFactory.isEffectSupported(EffectFactory.EFFECT_GRAIN)) {
            parametersCategory.removePreference(findPreference("effect_grain"));
        }
        if (!EffectFactory.isEffectSupported(EffectFactory.EFFECT_GRAYSCALE)) {
            parametersCategory.removePreference(findPreference("effect_grayscale_switch"));
        }
        if (!EffectFactory.isEffectSupported(EffectFactory.EFFECT_LOMOISH)) {
            parametersCategory.removePreference(findPreference("effect_lomoish_switch"));
        }
        if (!EffectFactory.isEffectSupported(EffectFactory.EFFECT_NEGATIVE)) {
            parametersCategory.removePreference(findPreference("effect_negative_switch"));
        }
        if (!EffectFactory.isEffectSupported(EffectFactory.EFFECT_POSTERIZE)) {
            parametersCategory.removePreference(findPreference("effect_posterize_switch"));
        }
        if (!EffectFactory.isEffectSupported(EffectFactory.EFFECT_SATURATE)) {
            parametersCategory.removePreference(findPreference("effect_saturate"));
        }
        if (!EffectFactory.isEffectSupported(EffectFactory.EFFECT_SEPIA)) {
            parametersCategory.removePreference(findPreference("effect_sepia_switch"));
        }
        if (!EffectFactory.isEffectSupported(EffectFactory.EFFECT_SHARPEN)) {
            parametersCategory.removePreference(findPreference("effect_sharpen"));
        }
        if (!EffectFactory.isEffectSupported(EffectFactory.EFFECT_TEMPERATURE)) {
            parametersCategory.removePreference(findPreference("effect_temperature"));
        }
        if (!EffectFactory.isEffectSupported(EffectFactory.EFFECT_VIGNETTE)) {
            parametersCategory.removePreference(findPreference("effect_vignette"));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

    }

    private void showEffectDialogMenu() {

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(appContext, R.style.DarkDialogTheme);

        dialogBuilder.setItems(R.array.random_effects_entry_menu, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                String[] randomEffectsList = getResources().getStringArray(R.array.random_effects_entry_menu);
                AppSettings.setRandomEffect(randomEffectsList[which]);
            }
        });

        AlertDialog dialog = dialogBuilder.create();

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {
                if (AppSettings.getRandomEffect().equals("None")) {
                    randomPref.setChecked(false);
                }
                randomPref.setSummary("Effect: " + AppSettings.getRandomEffect());
            }

        });

        AppSettings.setRandomEffect("None");

        dialog.show();
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    private void resetEffects() {
        PreferenceCategory settingsCategory = (PreferenceCategory) findPreference("title_effects_settings");

        for(int i = 0; i < settingsCategory.getPreferenceCount(); i++) {
            String key = settingsCategory.getPreference(i).getKey();

            if (key != null) {
                if(key.equals("effects_frequency")) {
                    AppSettings.setEffect(key, 100);
                }
                else if (!key.equals("reset_effects")) {
                    ((SwitchPreference) findPreference(key)).setChecked(false);
                }
            }
        }

        AppSettings.setRandomEffect("None");
        randomPref.setSummary("Effect: " + AppSettings.getRandomEffect());

        PreferenceCategory parametersCategory = (PreferenceCategory) findPreference("title_effects_parameters");

        for(int i = 0; i < parametersCategory.getPreferenceCount(); i++) {
            String key = parametersCategory.getPreference(i).getKey();

            if (key != null) {
                if (!key.contains("switch")) {
                    if (key.equals("effect_brightness") || key.equals("effect_contrast") || key.equals("effect_saturate") || key.equals("effects_frequency")) {
                        AppSettings.setEffect(key, 100);
                    } else if (key.equals("effect_temperature")) {
                        AppSettings.setEffect(key, 50);
                    } else {
                        AppSettings.setEffect(key, 0);
                    }

                    EffectPreference effectPref = (EffectPreference) findPreference(key);
                    effectPref.setSummary(effectPref.getTitle() + ": " + AppSettings.getEffectValue(key) + "%");
                }
                else {
                    ((SwitchPreference) findPreference(key)).setChecked(false);
                }
            }

        }
    }

    private void showDuotoneDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(appContext);

        dialogBuilder.setTitle("Choose dual tone colors:");

        View dialogView = getActivity().getLayoutInflater().inflate(R.layout.duotone_dialog, null);

        final ColorPickerView duoToneView1 = (ColorPickerView) dialogView.findViewById(R.id.duotone_color_picker_one);
        final ColorPickerView duoToneView2 = (ColorPickerView) dialogView.findViewById(R.id.duotone_color_picker_two);

        duoToneView1.setColor(AppSettings.getDuotoneColor(1));
        duoToneView2.setColor(AppSettings.getDuotoneColor(2));

        Button tabOneButton = (Button) dialogView.findViewById(R.id.color_one_button);
        Button tabTwoButton = (Button) dialogView.findViewById(R.id.color_two_button);
        final View tabOneHighlight = dialogView.findViewById(R.id.color_one_button_highlight);
        final View tabTwoHighlight = dialogView.findViewById(R.id.color_two_button_highlight);

        tabOneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                duoToneView2.setVisibility(View.GONE);
                tabTwoHighlight.setVisibility(View.INVISIBLE);
                duoToneView1.setVisibility(View.VISIBLE);
                tabOneHighlight.setVisibility(View.VISIBLE);
            }
        });

        tabTwoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                duoToneView1.setVisibility(View.GONE);
                tabOneHighlight.setVisibility(View.INVISIBLE);
                duoToneView2.setVisibility(View.VISIBLE);
                tabTwoHighlight.setVisibility(View.VISIBLE);
            }
        });

        dialogBuilder.setView(dialogView);

        dialogBuilder.setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                AppSettings.setDuotoneColor(1, duoToneView1.getColor());
                AppSettings.setDuotoneColor(2, duoToneView2.getColor());
            }
        });
        dialogBuilder.setNegativeButton(R.string.cancel_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                duotonePref.setChecked(false);
            }
        });

        AlertDialog dialog = dialogBuilder.create();
        dialog.show();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        if (!((Activity) appContext).isFinishing()) {

            if (!key.contains("switch") && key.contains("effect_")) {
                EffectPreference effectPref = (EffectPreference) findPreference(key);
                effectPref.setSummary(effectPref.getTitle() + ": " + AppSettings.getEffectValue(key) + "%");
            }

            if (key.equals("use_random_effects")) {
                if (AppSettings.useRandomEffects()) {
                    showEffectDialogMenu();
                }
                else {
                    AppSettings.setRandomEffect("None");
                }
            }

            if (key.equals("effect_duotone_switch")) {
                if (AppSettings.getDuotoneEffect()) {
                    showDuotoneDialog();
                }
            }

        }

    }

}
