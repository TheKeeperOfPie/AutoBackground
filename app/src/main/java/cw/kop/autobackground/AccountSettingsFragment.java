package cw.kop.autobackground;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;

import java.io.IOException;

import cw.kop.autobackground.accounts.GoogleAccount;
import cw.kop.autobackground.settings.AppSettings;

/**
 * Created by TheKeeperOfPie on 8/3/2014.
 */
public class AccountSettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private Context context;
    private SwitchPreference googlePref;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_accounts);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        googlePref = (SwitchPreference) findPreference("use_google_account");

        return super.onCreateView(inflater, container, savedInstanceState);
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
            if (key.equals("use_google_account")) {
                if (AppSettings.useGoogleAccount()) {
                    startActivityForResult(GoogleAccount.getPickerIntent(), GoogleAccount.GOOGLE_ACCOUNT_SIGN_IN);
                }
                else {
                    GoogleAccount.deleteAccount();
                    Toast.makeText(context, "Google access token has been deleted", Toast.LENGTH_SHORT).show();
                }
            }
        }

    }

    @Override
    public void onActivityResult(int requestCode, int responseCode, Intent intent) {
        if (requestCode == GoogleAccount.GOOGLE_ACCOUNT_SIGN_IN) {
            if (responseCode == Activity.RESULT_OK) {
                final String accountName = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                AppSettings.setGoogleAccountName(accountName);
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        try {
                            String authToken = GoogleAuthUtil.getToken(context, accountName, "oauth2:https://picasaweb.google.com/data/");
                            AppSettings.setGoogleAccountToken(authToken);
                            Log.i("MA", "GOOGLE_ACCOUNT_SIGN_IN Token: " + authToken);
                        } catch (IOException transientEx) {
                            return null;
                        } catch (UserRecoverableAuthException e) {
                            e.printStackTrace();
                            startActivityForResult(e.getIntent(), GoogleAccount.GOOGLE_AUTH_CODE);
                            return null;
                        } catch (GoogleAuthException authEx) {
                            return null;
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        return null;
                    }
                }.execute();
            }
            else {
                googlePref.setChecked(false);
                GoogleAccount.deleteAccount();
            }
        }
        if (requestCode == GoogleAccount.GOOGLE_AUTH_CODE) {
            if (responseCode == Activity.RESULT_OK) {
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        try {
                            String authToken = GoogleAuthUtil.getToken(context, AppSettings.getGoogleAccountName(), "oauth2:https://picasaweb.google.com/data/");
                            AppSettings.setGoogleAccountToken(authToken);
                            Log.i("MA", "GOOGLE_AUTH_CODE Token: " + authToken);
                        } catch (IOException transientEx) {
                            return null;
                        } catch (UserRecoverableAuthException e) {
                            return null;
                        } catch (GoogleAuthException authEx) {
                            return null;
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        return null;
                    }
                }.execute();
            }
            else {
                googlePref.setChecked(false);
                GoogleAccount.deleteAccount();
            }
        }
    }
}
