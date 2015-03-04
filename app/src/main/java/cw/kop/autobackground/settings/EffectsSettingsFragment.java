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
import android.app.Dialog;
import android.content.Context;
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
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import afzkl.development.colorpickerview.view.ColorPickerView;
import cw.kop.autobackground.CustomNumberPicker;
import cw.kop.autobackground.DialogFactory;
import cw.kop.autobackground.EffectPreference;
import cw.kop.autobackground.R;
import cw.kop.autobackground.settings.AppSettings;

public class EffectsSettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener, Preference.OnPreferenceClickListener {

    private Context appContext;

    private SwitchPreference randomPref;
    private SwitchPreference duotonePref;
    private SwitchPreference effectsPref;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

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

        effectsPref = (SwitchPreference) findPreference("use_effects");

        Preference blurRadiusPref = findPreference("blur_radius");
        blurRadiusPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                DialogFactory.SeekBarDialogListener listener = new DialogFactory.SeekBarDialogListener() {

                    @Override
                    public void onClickRight(View v) {
                        AppSettings.setBlurRadius(getValue());
                        this.dismissDialog();
                    }

                    @Override
                    public void onValueChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        setValueText("" + ((float) progress / 10));
                    }
                };

                DialogFactory.showSeekBarDialog(appContext,
                        "Blur effect",
                        "pixel radius",
                        listener,
                        250,
                        AppSettings.getBlurRadius(),
                        -1,
                        R.string.cancel_button,
                        R.string.ok_button);

                return true;
            }
        });

        if (!AppSettings.useAdvanced()) {

            PreferenceCategory preferenceCategory = (PreferenceCategory) findPreference(
                    "title_effects_settings");

            preferenceCategory.removePreference(findPreference("use_effects_override"));
            preferenceCategory.removePreference(findPreference("use_toast_effects"));
        }

        findPreference("effects_frequency").setOnPreferenceClickListener(this);
        findPreference("random_effects_frequency").setOnPreferenceClickListener(this);

        PreferenceCategory parametersCategory = (PreferenceCategory) findPreference(
                "title_opengl_effects");

        for (int i = 0; i < parametersCategory.getPreferenceCount(); i++) {
            String key = parametersCategory.getPreference(i).getKey();

            if (key != null && findPreference(key) instanceof EffectPreference) {
                EffectPreference effectPref = (EffectPreference) findPreference(key);
                effectPref.setSummary(effectPref.getTitle() + ": " + AppSettings.getEffectValue(key) + "%");
                effectPref.setOnPreferenceClickListener(this);
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

        return inflater.inflate(R.layout.fragment_list, container, false);

    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

    }

    private void showEffectDialogMenu() {

        DialogFactory.ListDialogListener clickListener = new DialogFactory.ListDialogListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String[] randomEffectsList = getResources().getStringArray(R.array.random_effects_entry_menu);
                AppSettings.setRandomEffect(randomEffectsList[position]);
                effectsPref.setChecked(true);
                this.dismissDialog();
            }

            @Override
            public void onDismiss() {
                if (AppSettings.getRandomEffect().equals("None")) {
                    randomPref.setChecked(false);
                }
                randomPref.setSummary("Effect: " + AppSettings.getRandomEffect());
            }
        };
        
        DialogFactory.showListDialog(appContext,
                "Random Effect:",
                clickListener,
                R.array.random_effects_entry_menu);
    }

    @Override
    public void onPause() {
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    private void resetEffects() {
        PreferenceCategory settingsCategory = (PreferenceCategory) findPreference(
                "title_effects_settings");

        for (int i = 0; i < settingsCategory.getPreferenceCount(); i++) {
            String key = settingsCategory.getPreference(i).getKey();

            if (key != null) {
                if (key.equals("effects_frequency")) {
                    AppSettings.setEffect(key, 100);
                }
                else if (findPreference(key) instanceof  SwitchPreference) {
                    ((SwitchPreference) findPreference(key)).setChecked(false);
                }
            }
        }

        AppSettings.setRandomEffect("None");
        randomPref.setSummary("Effect: " + AppSettings.getRandomEffect());

        PreferenceCategory parametersCategory = (PreferenceCategory) findPreference(
                "title_opengl_effects");

        for (int i = 0; i < parametersCategory.getPreferenceCount(); i++) {
            String key = parametersCategory.getPreference(i).getKey();

            if (key != null) {
                if (!key.contains("switch")) {
                    switch (key) {
                        case "effect_brightness":
                        case "effect_contrast":
                        case "effect_saturate":
                        case "effects_frequency":
                            AppSettings.setEffect(key, 100);
                            break;
                        case "effect_temperature":
                            AppSettings.setEffect(key, 50);
                            break;
                        default:
                            AppSettings.setEffect(key, 0);
                            break;
                    }

                    EffectPreference effectPref = (EffectPreference) findPreference(key);
                    effectPref.setSummary(effectPref.getTitle() + ": " + AppSettings.getEffectValue(
                            key) + "%");
                }
                else {
                    ((SwitchPreference) findPreference(key)).setChecked(false);
                }
            }

        }
    }

    private void showDuotoneDialog() {
        final Dialog dialog = AppSettings.getTheme().equals(AppSettings.APP_LIGHT_THEME) ?
                new Dialog(
                        appContext,
                        R.style.LightDialogTheme) :
                new Dialog(appContext, R.style.DarkDialogTheme);

        View dialogView = View.inflate(appContext, R.layout.duotone_dialog, null);
        dialog.setContentView(dialogView);

        TextView dialogTitle = (TextView) dialogView.findViewById(R.id.dialog_title);
        dialogTitle.setText("Choose dual tone colors:");

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

        Button positiveButton = (Button) dialogView.findViewById(R.id.dialog_positive_button);
        positiveButton.setVisibility(View.VISIBLE);
        positiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppSettings.setDuotoneColor(1, duoToneView1.getColor());
                AppSettings.setDuotoneColor(2, duoToneView2.getColor());
                dialog.dismiss();
            }
        });

        Button negativeButton = (Button) dialogView.findViewById(R.id.dialog_negative_button);
        negativeButton.setVisibility(View.VISIBLE);
        negativeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                duotonePref.setChecked(false);
                dialog.dismiss();
            }
        });

        if (AppSettings.getTheme().equals(AppSettings.APP_LIGHT_THEME)) {
            negativeButton.setTextColor(getResources().getColor(R.color.DARK_GRAY_OPAQUE));
            positiveButton.setTextColor(getResources().getColor(R.color.DARK_GRAY_OPAQUE));
        }
        else {
            negativeButton.setTextColor(getResources().getColor(R.color.LIGHT_GRAY_OPAQUE));
            positiveButton.setTextColor(getResources().getColor(R.color.LIGHT_GRAY_OPAQUE));
        }

        dialog.show();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        if (!((Activity) appContext).isFinishing()) {

            if (findPreference(key) instanceof EffectPreference) {
                EffectPreference effectPref = (EffectPreference) findPreference(key);
                effectPref.setSummary(effectPref.getTitle() + ": " + AppSettings.getEffectValue(key) + "%");
                effectsPref.setChecked(true);
            }
            else if (findPreference(key) instanceof SwitchPreference && key.contains("effect_")) {
                effectsPref.setChecked(true);
            }

            if (key.equals("use_random_effects")) {
                if (AppSettings.useRandomEffects()) {
                    showEffectDialogMenu();
                }
                else {
                    AppSettings.setRandomEffect("None");
                    randomPref.setSummary("Effect: " + AppSettings.getRandomEffect());
                }
            }

            if (key.equals("effect_duotone_switch")) {
                if (AppSettings.getDuotoneEffect()) {
                    showDuotoneDialog();
                }
            }

        }

    }

    @Override
    public boolean onPreferenceClick(Preference preference) {

        EffectPreference effectPreference = (EffectPreference) preference;
        String title = effectPreference.getTitle();
        final String key = effectPreference.getKey();
        int maxValue = effectPreference.getMaxValue();
        final int defaultValue = effectPreference.getDefaultValue();

        final Dialog dialog = AppSettings.getTheme().equals(AppSettings.APP_LIGHT_THEME) ?
                new Dialog(
                        appContext,
                        R.style.LightDialogTheme) :
                new Dialog(appContext, R.style.DarkDialogTheme);

        View dialogView = View.inflate(appContext, R.layout.effect_dialog, null);
        dialog.setContentView(dialogView);

        TextView dialogTitle = (TextView) dialogView.findViewById(R.id.dialog_title);
        dialogTitle.setText(title);

        final CustomNumberPicker valuePicker = (CustomNumberPicker) dialogView.findViewById(R.id.effect_number_picker);
        valuePicker.setMaxValue(maxValue);
        valuePicker.setValue(AppSettings.getEffectValue(key));

        TextView suffixText = (TextView) dialogView.findViewById(R.id.effect_suffix);
        suffixText.setText("%");

        Button defaultButton = (Button) dialogView.findViewById(R.id.effect_default_button);
        defaultButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                valuePicker.setValue(defaultValue);
            }
        });

        Button positiveButton = (Button) dialogView.findViewById(R.id.effect_ok_button);
        positiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppSettings.setEffect(key, valuePicker.getValue());
                dialog.dismiss();
            }
        });

        Button negativeButton = (Button) dialogView.findViewById(R.id.effect_cancel_button);
        negativeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        if (AppSettings.getTheme().equals(AppSettings.APP_LIGHT_THEME)) {
            defaultButton.setTextColor(getResources().getColor(R.color.DARK_GRAY_OPAQUE));
            negativeButton.setTextColor(getResources().getColor(R.color.DARK_GRAY_OPAQUE));
            positiveButton.setTextColor(getResources().getColor(R.color.DARK_GRAY_OPAQUE));
        }
        else {
            defaultButton.setTextColor(getResources().getColor(R.color.LIGHT_GRAY_OPAQUE));
            negativeButton.setTextColor(getResources().getColor(R.color.LIGHT_GRAY_OPAQUE));
            positiveButton.setTextColor(getResources().getColor(R.color.LIGHT_GRAY_OPAQUE));
        }

        dialog.show();

        return true;
    }
}
