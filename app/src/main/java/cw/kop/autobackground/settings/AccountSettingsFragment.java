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

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AppKeyPair;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import java.io.IOException;
import java.util.Collections;

import cw.kop.autobackground.BuildConfig;
import cw.kop.autobackground.R;
import cw.kop.autobackground.accounts.GoogleAccount;

public class AccountSettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final int REQUEST_DRIVE_ACCOUNT = 9005;
    private static final int REQUEST_DRIVE_AUTH = 9006;
    private Context appContext;
    private SwitchPreference googlePref;
    private DropboxAPI<AndroidAuthSession> dropboxAPI;
    private GoogleAccountCredential driveCredential;
    private Drive drive;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        appContext = activity;
    }

    @Override
    public void onDetach() {
        appContext = null;
        super.onDetach();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_accounts);

        AppKeyPair appKeys = new AppKeyPair(ApiKeys.DROPBOX_KEY, ApiKeys.DROPBOX_SECRET);
        AndroidAuthSession session = new AndroidAuthSession(appKeys);
        dropboxAPI = new DropboxAPI<>(session);

        driveCredential = GoogleAccountCredential.usingOAuth2(
                appContext,
                Collections.singleton(DriveScopes.DRIVE));
        if (!TextUtils.isEmpty(AppSettings.getDriveAccountName())) {
            driveCredential.setSelectedAccountName(AppSettings.getDriveAccountName());
        }
        drive = new Drive.Builder(AndroidHttp.newCompatibleTransport(), GsonFactory.getDefaultInstance(), driveCredential)
                .setApplicationName(appContext.getResources().getString(R.string.app_name) + "/" + BuildConfig.VERSION_NAME)
                .build();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        googlePref = (SwitchPreference) findPreference("use_google_account");

        return inflater.inflate(R.layout.fragment_list, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        if (dropboxAPI.getSession().authenticationSuccessful()) {
            try {
                dropboxAPI.getSession().finishAuthentication();

                AppSettings.setUseDropboxAccount(true);
                AppSettings.setDropboxAccountToken(dropboxAPI.getSession().getOAuth2AccessToken());
            }
            catch (IllegalStateException e) {
                Log.i("DbAuthLog", "Error authenticating", e);
            }
        }
    }

    @Override
    public void onPause() {
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        if (!((Activity) appContext).isFinishing()) {
            switch (key) {
                case "use_google_account":
                    if (AppSettings.useGoogleAccount()) {
                        startActivityForResult(GoogleAccount.getPickerIntent(),
                                GoogleAccount.GOOGLE_ACCOUNT_SIGN_IN);
                    }
                    else {
                        GoogleAccount.deleteAccount();
                        if (AppSettings.useToast()) {
                            Toast.makeText(appContext,
                                    "Google access token has been deleted",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                    break;
                case "use_dropbox_account":
                    if (AppSettings.useDropboxAccount()) {
                        dropboxAPI.getSession().startOAuth2Authentication(appContext);
                    }
                    else {
                        AppSettings.setDropboxAccountToken("");
                        if (AppSettings.useToast()) {
                            Toast.makeText(appContext,
                                    "Dropbox access token has been deleted",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                case "use_google_drive_account":
                    if (TextUtils.isEmpty(AppSettings.getDriveAccountName())) {
                        startActivityForResult(driveCredential.newChooseAccountIntent(), REQUEST_DRIVE_ACCOUNT);
                    }
                    else {
                        AppSettings.setDriveAccountName("");
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    driveCredential.getGoogleAccountManager().invalidateAuthToken(driveCredential.getToken());
                                }
                                catch (IOException | GoogleAuthException e) {
                                    e.printStackTrace();
                                }
                            }
                        }).start();
                        if (AppSettings.useToast()) {
                            Toast.makeText(appContext,
                                    "Google Drive account deactivated",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
            }
        }

    }

    @Override
    public void onActivityResult(int requestCode, int responseCode, Intent intent) {


        if (requestCode == REQUEST_DRIVE_AUTH && responseCode == Activity.RESULT_OK) {
            String accountName = intent.getExtras().getString(AccountManager.KEY_ACCOUNT_NAME);
            if (!TextUtils.isEmpty(accountName)) {
                AppSettings.setDriveAccountName(accountName);
                AppSettings.setUseGoogleDriveAccount(true);
                driveCredential.setSelectedAccountName(accountName);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // Send an about request to check if app is authenticated
                            drive.about().get().execute();
                        }
                        catch (UserRecoverableAuthIOException e) {
                            startActivityForResult(e.getIntent(), REQUEST_DRIVE_ACCOUNT);
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        }
        else if (requestCode == REQUEST_DRIVE_ACCOUNT && responseCode == Activity.RESULT_OK) {
            String accountName = intent.getExtras().getString(AccountManager.KEY_ACCOUNT_NAME);
            if (!TextUtils.isEmpty(accountName)) {
                AppSettings.setDriveAccountName(accountName);
                AppSettings.setUseGoogleDriveAccount(true);
                driveCredential.setSelectedAccountName(accountName);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // Send an about request to check if app is authenticated
                            drive.about().get().execute();
                        }
                        catch (UserRecoverableAuthIOException e) {
                            startActivityForResult(e.getIntent(), REQUEST_DRIVE_ACCOUNT);
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        }
        else if (requestCode == GoogleAccount.GOOGLE_ACCOUNT_SIGN_IN) {
            if (responseCode == Activity.RESULT_OK) {
                final String accountName = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                AppSettings.setGoogleAccountName(accountName);
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        try {
                            String authToken = GoogleAuthUtil.getToken(appContext,
                                    accountName,
                                    "oauth2:https://picasaweb.google.com/data/");
                            AppSettings.setGoogleAccountToken(authToken);
                            Log.i("MA", "GOOGLE_ACCOUNT_SIGN_IN Token: " + authToken);
                        }
                        catch (IOException transientEx) {
                            return null;
                        }
                        catch (UserRecoverableAuthException e) {
                            e.printStackTrace();
                            if (isAdded()) {
                                startActivityForResult(e.getIntent(),
                                        GoogleAccount.GOOGLE_AUTH_CODE);
                            }
                            return null;
                        }
                        catch (GoogleAuthException authEx) {
                            return null;
                        }
                        catch (Exception e) {
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
        else if (requestCode == GoogleAccount.GOOGLE_AUTH_CODE) {
            if (responseCode == Activity.RESULT_OK) {
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        try {
                            String authToken = GoogleAuthUtil.getToken(appContext,
                                    AppSettings.getGoogleAccountName(),
                                    "oauth2:https://picasaweb.google.com/data/");
                            AppSettings.setGoogleAccountToken(authToken);
                            Log.i("MA", "GOOGLE_AUTH_CODE Token: " + authToken);
                        }
                        catch (IOException transientEx) {
                            return null;
                        }
                        catch (UserRecoverableAuthException e) {
                            return null;
                        }
                        catch (GoogleAuthException authEx) {
                            return null;
                        }
                        catch (Exception e) {
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
