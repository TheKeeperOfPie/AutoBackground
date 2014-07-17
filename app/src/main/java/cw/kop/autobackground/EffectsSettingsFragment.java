package cw.kop.autobackground;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import cw.kop.autobackground.settings.AppSettings;

/**
 * Created by TheKeeperOfPie on 7/4/2014.
 */
public class EffectsSettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {

    private Context context;

    private SwitchPreference randomPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_effects);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        Preference resetPref = findPreference("reset_effects");
        resetPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                resetEffects();
                if (AppSettings.useToast()) {
                    Toast.makeText(context, "Reset effects", Toast.LENGTH_SHORT).show();
                }
                return false;
            }
        });

        randomPref = (SwitchPreference) findPreference("use_random_effects");
        if (AppSettings.useRandomEffects()) {
            randomPref.setSummary("Effect: " + AppSettings.getRandomEffect());
        }

        final Context contextThemeWrapper = new ContextThemeWrapper(getActivity(), AppSettings.getTheme());

        LayoutInflater localInflater = inflater.cloneInContext(contextThemeWrapper);

        return localInflater.inflate(R.layout.fragment_list, container, false);

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = getActivity();

    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        PreferenceCategory preferenceCategory = (PreferenceCategory) findPreference("title_effects_parameters");

        for(int i = 0; i < preferenceCategory.getPreferenceCount(); i++) {
            String key = preferenceCategory.getPreference(i).getKey();

            if(key != null && !key.contains("switch")) {
                EffectPreference effectPref = (EffectPreference) findPreference(key);
                effectPref.setSummary(effectPref.getTitle() + ": " + AppSettings.getEffectValue(key) + "%");
            }
        }

    }

    private void showEffectDialogMenu() {

        AlertDialog.Builder dialog = new AlertDialog.Builder(context, R.style.DarkDialogTheme);

        dialog.setItems(R.array.random_effects_entry_menu, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                String[] randomEffectsList = getResources().getStringArray(R.array.random_effects_entry_menu);
                AppSettings.setRandomEffect(randomEffectsList[which]);
            }
        });


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

        dialog.create();
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

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        if (!((Activity) context).isFinishing()) {

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

        }

    }

}
