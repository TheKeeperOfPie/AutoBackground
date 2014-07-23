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
import android.widget.Toast;

import java.util.HashSet;

import cw.kop.autobackground.images.LocalImageFragment;
import cw.kop.autobackground.settings.AppSettings;

public class DownloadSettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {

	private final static long CONVERT_MILLES_TO_MIN = 60000;
	private final static int REQUEST_FILE_ID = 0;
	private SwitchPreference timerPref;
	private Context context;
    private PendingIntent pendingIntent;
	private AlarmManager alarmManager;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_download);
    }
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		timerPref = (SwitchPreference) findPreference("use_timer");

        Preference deletePref = findPreference("delete_images");
        deletePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                AlertDialog.Builder dialog = new AlertDialog.Builder(context);

                dialog.setTitle("Are you sure you want to delete all images?");
                dialog.setMessage("This cannot be undone.");

                dialog.setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        Downloader.deleteAllBitmaps(context);
                        for (int i = 0; i < AppSettings.getNumSources(); i ++) {
                            if (AppSettings.getSourceType(i).equals("website")) {
                                AppSettings.setSourceSet(AppSettings.getSourceTitle(i), new HashSet<String>());
                            }
                        }
                        Toast.makeText(context, "Deleted images with prefix\n" + AppSettings.getImagePrefix(), Toast.LENGTH_SHORT).show();
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

        Preference pathPref = findPreference("download_path");
        pathPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
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
                return false;
            }
        });

        EditTextPreference prefixPref = (EditTextPreference) findPreference("image_prefix_adv");
        prefixPref.setSummary("Prefix: " + AppSettings.getImagePrefix());

        final Context contextThemeWrapper = new ContextThemeWrapper(getActivity(), AppSettings.getTheme());

        LayoutInflater localInflater = inflater.cloneInContext(contextThemeWrapper);

        return localInflater.inflate(R.layout.fragment_list, container, false);
		
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		context = getActivity();

        Intent intent = new Intent();
		intent.setAction(LiveWallpaperService.DOWNLOAD_WALLPAPER);
		intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
		pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

		alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Log.i("DSF", "onAttach");
	}

	private void showDialogTimerMenu() {

        AppSettings.setTimerDuration(0);

		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);

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
				
				Log.i("DSF", "Alarm Set: " + AppSettings.getTimerDuration());

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

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        dialogBuilder.setMessage("Download Interval");

        View dialogView = View.inflate(context, R.layout.numeric_dialog, null);

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
			alarmManager.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis() + AppSettings.getTimerDuration(), AppSettings.getTimerDuration(), pendingIntent);
		}
		else {
			alarmManager.cancel(pendingIntent);
		}
	}

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (!AppSettings.useAdvanced()) {
            ((PreferenceCategory) findPreference("title_download_settings")).removePreference(findPreference("use_experimental_downloader_adv"));
            ((PreferenceCategory) findPreference("title_download_settings")).removePreference(findPreference("image_prefix_adv"));
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

		
		if (!((Activity) context).isFinishing()) {
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
		}
		
		
	}
	
}
