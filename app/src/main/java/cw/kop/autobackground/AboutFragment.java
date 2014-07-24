package cw.kop.autobackground;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import cw.kop.autobackground.settings.AppSettings;

public class AboutFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener {

    private Context context;
    private int gameCount = 0;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences_about, false);

        addPreferencesFromResource(R.xml.preferences_about);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        Preference copyrightPref = findPreference("about_copyright");
        copyrightPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(context);


                TextView textView = new TextView(context);
                textView.setText(context.getResources().getString(R.string.about_copyright_text));

                ScrollView scrollView = new ScrollView(context);
                scrollView.addView(textView);

                dialog.setView(scrollView);

                dialog.show();

                return false;
            }
        });

        Preference gamePref = findPreference("about_self_copyright");
        gamePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                gameCount++;

                if (gameCount > 4) {
                    gameCount = 0;
                    AppSettings.setUseNotificationGame(!AppSettings.useNotificationGame());
                    if (AppSettings.useNotificationGame()) {
                        Toast.makeText(context, "Game has been activated", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Toast.makeText(context, "Game has been deactivated", Toast.LENGTH_SHORT).show();
                    }
                    Intent intent = new Intent();
                    intent.setAction(LiveWallpaperService.UPDATE_NOTIFICATION);
                    intent.putExtra("use", true);
                    context.sendBroadcast(intent);
                }

                return true;
            }
        });

        findPreference("about_library_jsoup").setOnPreferenceClickListener(this);
        findPreference("about_library_showcaseview").setOnPreferenceClickListener(this);
        findPreference("about_library_picasso").setOnPreferenceClickListener(this);
        findPreference("about_library_colorpickerview").setOnPreferenceClickListener(this);

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
    public boolean onPreferenceClick(Preference preference) {

        String key = preference.getKey();
        Intent intent = null;

        if (key.equals("about_library_jsoup")) {
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("http://jsoup.org/"));
        }
        else if (key.equals("about_library_showcaseview")) {
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("http://amlcurran.github.io/ShowcaseView/"));
        }
        else if (key.equals("about_library_picasso")) {
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("http://square.github.io/picasso/"));
        }
        else if (key.equals("about_library_colorpickerview")) {
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://code.google.com/p/color-picker-view/"));
        }


        if (intent != null) {
            context.startActivity(intent);
            return true;
        }

        return false;
    }
}

