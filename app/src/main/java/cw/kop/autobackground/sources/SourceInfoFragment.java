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

package cw.kop.autobackground.sources;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AppKeyPair;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

import cw.kop.autobackground.CustomSwitchPreference;
import cw.kop.autobackground.DialogFactory;
import cw.kop.autobackground.R;
import cw.kop.autobackground.accounts.GoogleAccount;
import cw.kop.autobackground.files.FileHandler;
import cw.kop.autobackground.images.AlbumAdapter;
import cw.kop.autobackground.images.DropboxAdapter;
import cw.kop.autobackground.images.FolderFragment;
import cw.kop.autobackground.images.LocalImageAdapter;
import cw.kop.autobackground.settings.ApiKeys;
import cw.kop.autobackground.settings.AppSettings;

/**
 * Created by TheKeeperOfPie on 11/5/2014.
 */
public class SourceInfoFragment extends PreferenceFragment {

    private static final String TAG = SourceInfoFragment.class.getCanonicalName();
    private static final int FADE_IN_TIME = 350;
    private static final int SLIDE_EXIT_TIME = 350;

    private Context appContext;
    private Drawable imageDrawable;

    private RelativeLayout settingsContainer;
    private TextView sourceSpinnerText;
    private Spinner sourceSpinner;
    private ImageView sourceImage;
    private EditText sourceTitle;
    private EditText sourcePrefix;
    private EditText sourceData;
    private EditText sourceSuffix;
    private EditText sourceNum;
    private Switch sourceUse;
    private Button cancelButton;
    private Button saveButton;

    private int sourcePosition;
    private String oldTitle;
    private String type;
    private String hint;
    private String prefix;
    private String suffix;
    private int startHour;
    private int startMinute;
    private int endHour;
    private int endMinute;
    private CustomSwitchPreference timePref;
    private Handler handler;
    private Bundle oldState;
    private View headerView;

    private String folderData;

    private DropboxAPI<AndroidAuthSession> dropboxAPI;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_source);
        handler = new Handler();
        setRetainInstance(true);
        AppKeyPair appKeys = new AppKeyPair(ApiKeys.DROPBOX_KEY, ApiKeys.DROPBOX_SECRET);
        AndroidAuthSession session = new AndroidAuthSession(appKeys);
        dropboxAPI = new DropboxAPI<>(session);

        if (AppSettings.useDropboxAccount() && !AppSettings.getDropboxAccountToken().equals("")) {
            dropboxAPI.getSession().setOAuth2AccessToken(AppSettings.getDropboxAccountToken());
        }
    }

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
    public View onCreateView(LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {

        Bundle arguments = getArguments();
        sourcePosition = (Integer) arguments.get(Source.POSITION);
        int colorFilterInt = AppSettings.getColorFilterInt(appContext);

        View view = inflater.inflate(R.layout.source_info_fragment, container, false);
        headerView = inflater.inflate(R.layout.source_info_header, null, false);

        settingsContainer = (RelativeLayout) headerView.findViewById(R.id.source_settings_container);

        sourceImage = (ImageView) headerView.findViewById(R.id.source_image);
        sourceTitle = (EditText) headerView.findViewById(R.id.source_title);
        sourcePrefix = (EditText) headerView.findViewById(R.id.source_data_prefix);
        sourceData = (EditText) headerView.findViewById(R.id.source_data);
        sourceSuffix = (EditText) headerView.findViewById(R.id.source_data_suffix);
        sourceNum = (EditText) headerView.findViewById(R.id.source_num);

        ViewGroup.LayoutParams params = sourceImage.getLayoutParams();
        params.height = (int) ((container.getWidth() - 2f * getResources().getDimensionPixelSize(R.dimen.side_margin)) / 16f * 9);
        sourceImage.setLayoutParams(params);

        cancelButton = (Button) view.findViewById(R.id.cancel_button);
        saveButton = (Button) view.findViewById(R.id.save_button);

        sourcePrefix.setTextColor(colorFilterInt);
        sourceSuffix.setTextColor(colorFilterInt);
        cancelButton.setTextColor(colorFilterInt);
        saveButton.setTextColor(colorFilterInt);

        // Adjust alpha to get faded hint color from regular text color
        int hintColor = Color.argb(0x88,
                Color.red(colorFilterInt),
                Color.green(colorFilterInt),
                Color.blue(colorFilterInt));

        sourceTitle.setHintTextColor(hintColor);
        sourceData.setHintTextColor(hintColor);
        sourceNum.setHintTextColor(hintColor);

        sourceData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (type) {

                    case AppSettings.FOLDER:
                    case AppSettings.GOOGLE_ALBUM:
                    case AppSettings.DROPBOX_FOLDER:
                        selectSource(type);
                        break;

                }
                Log.i(TAG, "Data launched folder fragment");
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSource();
            }
        });

        sourceSpinnerText = (TextView) headerView.findViewById(R.id.source_spinner_text);
        sourceSpinner = (Spinner) headerView.findViewById(R.id.source_spinner);

        timePref = (CustomSwitchPreference) findPreference("source_time");
        timePref.setChecked(arguments.getBoolean("use_time"));
        timePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                if (!(Boolean) newValue) {
                    return true;
                }

                DialogFactory.TimeDialogListener startTimeListener = new DialogFactory.TimeDialogListener() {



                    @Override
                    public void onTimeSet(TimePicker view, int hour, int minute) {
                        startHour = hour;
                        startMinute = minute;

                        DialogFactory.TimeDialogListener endTimeListener = new DialogFactory.TimeDialogListener() {
                            @Override
                            public void onTimeSet(TimePicker view, int hour, int minute) {
                                endHour = hour;
                                endMinute = minute;

                                timePref.setSummary(String.format(
                                        "Time active: %02d:%02d - %02d:%02d",
                                        startHour,
                                        startMinute,
                                        endHour,
                                        endMinute));

                            }
                        };

                        DialogFactory.showTimeDialog(appContext,
                                "End time?",
                                endTimeListener,
                                startHour,
                                startMinute);

                    }
                };

                DialogFactory.showTimeDialog(appContext,
                        "Start time?",
                        startTimeListener,
                        startHour,
                        startMinute);


                return true;
            }
        });

        if (sourcePosition == -1) {
            sourceImage.setVisibility(View.GONE);
            sourceSpinnerText.setVisibility(View.VISIBLE);
            sourceSpinner.setVisibility(View.VISIBLE);

            SourceSpinnerAdapter adapter = new SourceSpinnerAdapter(appContext,
                    R.layout.spinner_row,
                    Arrays.asList(getResources().getStringArray(R.array.source_menu)));
            sourceSpinner.setAdapter(adapter);
            sourceSpinner.setSelection(0);
            sourceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent,
                        View view,
                        int position,
                        long id) {

                    selectSource(getTypeFromPosition(position));
                    Log.i(TAG, "Spinner launched folder fragment");
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });

            type = AppSettings.WEBSITE;
            prefix = AppSettings.getSourceDataPrefix(type);
            hint = AppSettings.getSourceDataHint(type);
            suffix = AppSettings.getSourceDataSuffix(type);

            startHour = 0;
            startMinute = 0;
            endHour = 0;
            endMinute = 0;
        }
        else {
            sourceImage.setVisibility(View.VISIBLE);
            sourceSpinnerText.setVisibility(View.GONE);
            sourceSpinner.setVisibility(View.GONE);

            type = arguments.getString(Source.TYPE);

            folderData = arguments.getString(Source.DATA);
            String data = folderData;

            hint = AppSettings.getSourceDataHint(type);
            prefix = AppSettings.getSourceDataPrefix(type);
            suffix = AppSettings.getSourceDataSuffix(type);

            switch (type) {
                case AppSettings.GOOGLE_ALBUM:
                    sourceTitle.setFocusable(false);
                    sourceData.setFocusable(false);
                    sourceNum.setFocusable(false);
                case AppSettings.FOLDER:
                    data = Arrays.toString(folderData.split(AppSettings.DATA_SPLITTER));
                    break;

            }

            sourceTitle.setText(arguments.getString(Source.TITLE));

            if (getArguments().getInt(Source.NUM, -1) >= 0) {
                sourceNum.setText("" + arguments.getInt(Source.NUM));
            }
            sourceData.setText(data);

            if (imageDrawable != null) {
                sourceImage.setImageDrawable(imageDrawable);
            }

            boolean showPreview = arguments.getBoolean(Source.PREVIEW);
            if (showPreview) {
                sourceImage.setVisibility(View.VISIBLE);
            }

            ((CustomSwitchPreference) findPreference("source_show_preview")).setChecked(showPreview);

            String[] timeArray = arguments.getString(Source.TIME).split(":|[ -]+");

            try {
                startHour = Integer.parseInt(timeArray[0]);
                startMinute = Integer.parseInt(timeArray[1]);
                endHour = Integer.parseInt(timeArray[2]);
                endMinute = Integer.parseInt(timeArray[3]);
                timePref.setSummary(String.format("Time active: %02d:%02d - %02d:%02d",
                        startHour, startMinute, endHour, endMinute));
            }
            catch (NumberFormatException e) {
                e.printStackTrace();
                startHour = 0;
                startMinute = 0;
                endHour = 0;
                endMinute = 0;
            }

        }

        setDataWrappers();

        sourceUse = (Switch) headerView.findViewById(R.id.source_use_switch);
        sourceUse.setChecked(arguments.getBoolean(Source.USE));

        if (AppSettings.getTheme().equals(AppSettings.APP_LIGHT_THEME)) {
            view.setBackgroundColor(getResources().getColor(R.color.LIGHT_THEME_BACKGROUND));
        }
        else {
            view.setBackgroundColor(getResources().getColor(R.color.DARK_THEME_BACKGROUND));
        }

        ListView listView = (ListView) view.findViewById(android.R.id.list);
        listView.addHeaderView(headerView);

        if (savedInstanceState != null) {
            if (arguments.getString(Source.TYPE, "").length() > 0) {
                sourceSpinner.setSelection(getPositionOfType(savedInstanceState.getString(Source.TYPE,
                        AppSettings.WEBSITE)));
            }
            sourceTitle.setText(savedInstanceState.getString(Source.TITLE, ""));
            sourceData.setText(savedInstanceState.getString(Source.DATA, ""));
            sourceNum.setText(savedInstanceState.getString(Source.NUM, ""));
        }

        return view;
    }

    private int getPositionOfType(String type) {
        return Arrays.asList(getResources().getStringArray(R.array.source_menu)).indexOf(type);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        outState.putString(Source.TYPE, type);
        outState.putString(Source.TITLE, String.valueOf(sourceTitle.getText()));
        outState.putString(Source.DATA, String.valueOf(sourceData.getText()));
        outState.putString(Source.NUM, String.valueOf(sourceNum.getText()));

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (dropboxAPI.getSession().authenticationSuccessful()) {
            try {
                dropboxAPI.getSession().finishAuthentication();

                AppSettings.setUseDropboxAccount(true);
                AppSettings.setDropboxAccountToken(dropboxAPI.getSession().getOAuth2AccessToken());
                showDropboxFragment();
            }
            catch (IllegalStateException e) {
                Log.i("DbAuthLog", "Error authenticating", e);
            }
        }

    }

    @Override
    public void onStart() {
        super.onStart();
        Animation animation;
        if (sourcePosition == -1) {
            animation = new Animation() {
                @Override
                protected void applyTransformation(float interpolatedTime, Transformation t) {

                    sourceSpinnerText.setAlpha(interpolatedTime);
                    sourceSpinner.setAlpha(interpolatedTime);
                    sourceTitle.setAlpha(interpolatedTime);
                    settingsContainer.setAlpha(interpolatedTime);
                    sourceUse.setAlpha(interpolatedTime);

                }
            };
        }
        else {
            animation = new Animation() {
                @Override
                protected void applyTransformation(float interpolatedTime, Transformation t) {

                    settingsContainer.setAlpha(interpolatedTime);
                    sourceUse.setAlpha(interpolatedTime);

                }
            };
        }

        animation.setDuration(FADE_IN_TIME);
        animation.setInterpolator(new DecelerateInterpolator(3.0f));
        settingsContainer.startAnimation(animation);

    }

    private void saveSource() {

        final Intent sourceIntent = new Intent();

        String title = sourceTitle.getText().toString();
        String data = sourceData.getText().toString();

        if (type.equals(AppSettings.FOLDER)) {
            data = folderData;
        }

        if (title.equals("")) {
            Toast.makeText(appContext, "Title cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }
        if (data.equals("")) {
            Toast.makeText(appContext, "Data cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        int num = 0;
        try {
            num = Integer.parseInt(sourceNum.getText().toString());
        }
        catch (NumberFormatException e) {
            num = 1;
        }

        switch (type) {

            case AppSettings.WEBSITE:
                if (!data.contains("http")) {
                    data = "http://" + data;
                }
                break;

        }

        if (sourcePosition == -1) {

            if (FileHandler.isDownloading) {
                Toast.makeText(appContext,
                        "Cannot add source while downloading",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            sourceIntent.setAction(SourceListFragment.ADD_ENTRY);

        }
        else {

            if (!getArguments().getString(Source.TITLE).equals(title)) {
                FileHandler.renameFolder(getArguments().getString(Source.TITLE), title);
            }

            if (FileHandler.isDownloading) {
                Toast.makeText(appContext,
                        "Cannot edit while downloading",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            sourceIntent.setAction(SourceListFragment.SET_ENTRY);
        }

        sourceIntent.putExtra(Source.TYPE, type);
        sourceIntent.putExtra(Source.TITLE, sourceTitle.getText().toString());
        sourceIntent.putExtra(Source.DATA, data);
        sourceIntent.putExtra(Source.NUM, num);
        sourceIntent.putExtra(Source.POSITION, sourcePosition);
        sourceIntent.putExtra(Source.USE, sourceUse.isChecked());
        sourceIntent.putExtra(Source.PREVIEW,
                ((CustomSwitchPreference) findPreference("source_show_preview")).isChecked());
        sourceIntent.putExtra(Source.USE_TIME, timePref.isChecked());
        sourceIntent.putExtra(Source.TIME, String.format("%02d:%02d - %02d:%02d",
                startHour, startMinute, endHour, endMinute));

        try {
            InputMethodManager im = (InputMethodManager) appContext.getSystemService(Context.INPUT_METHOD_SERVICE);
            im.hideSoftInputFromWindow(getView().getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS);
        }
        catch (Exception e) {
            e.printStackTrace();
        }


        final int screenHeight = getResources().getDisplayMetrics().heightPixels;
        final View fragmentView = getView();

        if (fragmentView != null) {
            final float viewStartY = getView().getY();

            Animation animation = new Animation() {
                @Override
                protected void applyTransformation(float interpolatedTime,
                        Transformation t) {
                    fragmentView.setY((screenHeight - viewStartY) * interpolatedTime + viewStartY);
                }

                @Override
                public boolean willChangeBounds() {
                    return true;
                }
            };

            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    LocalBroadcastManager.getInstance(appContext).sendBroadcast(sourceIntent);
                    getFragmentManager().popBackStack();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });

            animation.setDuration(SLIDE_EXIT_TIME);
            getView().startAnimation(animation);
        }
        else {
            LocalBroadcastManager.getInstance(appContext).sendBroadcast(sourceIntent);
        }

    }

    public void setImageDrawable(Drawable drawable) {
        imageDrawable = drawable;
        if (sourceImage != null) {
            sourceImage.setImageDrawable(imageDrawable);
        }
    }

    public void setData(String type,
            final String title,
            final String data,
            final int num) {

        this.type = type;
        this.prefix = AppSettings.getSourceDataPrefix(type);
        this.hint = AppSettings.getSourceDataHint(type);
        this.suffix = AppSettings.getSourceDataSuffix(type);
        this.folderData = data;

        handler.post(new Runnable() {
            @Override
            public void run() {
                sourceTitle.setText(title);
                sourceData.setText(SourceInfoFragment.this.type.equals(AppSettings.FOLDER) ? Arrays.toString(folderData.split(AppSettings.DATA_SPLITTER)) : data);
                sourceNum.setText("" + num);
                setDataWrappers();
            }
        });

    }

    private void setDataWrappers() {
        sourcePrefix.setText(prefix);
        sourceSuffix.setText(suffix);
        if (prefix.length() > 0) {
            sourcePrefix.setVisibility(View.VISIBLE);
        }
        else {
            sourcePrefix.setVisibility(View.GONE);
        }
        if (suffix.length() > 0) {
            sourceSuffix.setVisibility(View.VISIBLE);
        }
        else {
            sourceSuffix.setVisibility(View.GONE);
        }
        sourceData.setHint(hint);

    }

    private String getTypeFromPosition(int position) {

        return getResources().getStringArray(R.array.source_menu)[position];

    }

    private void selectSource(String newType) {

        boolean blockTitle = false;
        boolean blockData = false;
        boolean blockNum = false;

        if (type.equals(AppSettings.FOLDER) ||
                type.equals(AppSettings.GOOGLE_ALBUM) ||
                type.equals(AppSettings.DROPBOX_FOLDER)) {
            sourceTitle.setText("");
            sourceData.setText("");
            sourceNum.setText("");
        }

        type = newType;

        switch (type) {
            case AppSettings.WEBSITE:
                break;
            case AppSettings.FOLDER:
                File externalStorageDirectory = Environment.getExternalStorageDirectory();
                if (getFragmentManager().findFragmentByTag("folder_fragment") == null) {
                    if (externalStorageDirectory.exists() && externalStorageDirectory.canRead()) {
                        showImageFragment(new File(File.separator), externalStorageDirectory);
                    }
                    else {
                        showImageFragment(new File(File.separator), new File(File.separator));
                    }
                }
                blockData = true;
                blockNum = true;
                break;
            case AppSettings.IMGUR_SUBREDDIT:
                break;
            case AppSettings.IMGUR_ALBUM:
                break;
            case AppSettings.GOOGLE_ALBUM:
                if (AppSettings.getGoogleAccountName().equals("")) {
                    startActivityForResult(GoogleAccount.getPickerIntent(),
                            GoogleAccount.GOOGLE_ACCOUNT_SIGN_IN);
                }
                else if (getFragmentManager().findFragmentByTag("folder_fragment") == null) {
                    new PicasaAlbumTask().execute();
                }
                blockData = true;
                break;
            case AppSettings.TUMBLR_BLOG:
                break;
            case AppSettings.TUMBLR_TAG:
                break;
            case AppSettings.REDDIT_SUBREDDIT:
                break;
            case AppSettings.DROPBOX_FOLDER:
                if (AppSettings.getDropboxAccountToken().equals("") || !dropboxAPI.getSession().isLinked()) {
                    dropboxAPI.getSession().startOAuth2Authentication(appContext);
                }
                else if (getFragmentManager().findFragmentByTag("folder_fragment") == null) {
                    showDropboxFragment();
                }
                blockData = true;
                break;
            default:
        }

        prefix = AppSettings.getSourceDataPrefix(type);
        hint = AppSettings.getSourceDataHint(type);
        suffix = AppSettings.getSourceDataSuffix(type);

        setDataWrappers();

        sourceTitle.setFocusable(true);
        sourceTitle.setFocusableInTouchMode(true);
        sourceData.setFocusable(true);
        sourceData.setFocusableInTouchMode(true);
        sourceNum.setFocusable(true);
        sourceNum.setFocusableInTouchMode(true);

        if (blockTitle) {
            sourceTitle.setFocusable(false);
        }
        if (blockData) {
            sourceData.setFocusable(false);
        }
        if (blockNum) {
            sourceNum.setFocusable(false);
        }

    }

    @Override
    public void onActivityResult(int requestCode, int responseCode, Intent intent) {

        if (requestCode == GoogleAccount.GOOGLE_ACCOUNT_SIGN_IN) {
            if (intent != null && responseCode == Activity.RESULT_OK) {
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
                            AppSettings.setUseGoogleAccount(true);
                            new PicasaAlbumTask().execute();
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
        }
        if (requestCode == GoogleAccount.GOOGLE_AUTH_CODE) {
            if (responseCode == Activity.RESULT_OK) {
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        try {
                            String authToken = GoogleAuthUtil.getToken(appContext,
                                    AppSettings.getGoogleAccountName(),
                                    "oauth2:https://picasaweb.google.com/data/");
                            AppSettings.setGoogleAccountToken(authToken);
                            AppSettings.setUseGoogleAccount(true);
                            new PicasaAlbumTask().execute();
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
        }
    }

    private void showImageFragment(File topDir, File startDir) {

        final FolderFragment folderFragment = new FolderFragment();
        Bundle arguments = new Bundle();
        arguments.putBoolean(FolderFragment.SHOW_DIRECTORY_TEXT, true);
        arguments.putBoolean(FolderFragment.USE_DIRECTORY, true);
        final LocalImageAdapter adapter = new LocalImageAdapter(appContext, topDir, startDir);
        folderFragment.setArguments(arguments);
        folderFragment.setAdapter(adapter);
        folderFragment.setStartingDirectoryText(startDir.getAbsolutePath());
        folderFragment.setListener(new FolderFragment.FolderEventListener() {
            @Override
            public void onUseDirectoryClick() {
                DialogFactory.ActionDialogListener listener = new DialogFactory.ActionDialogListener() {
                    @Override
                    public void onClickMiddle(View v) {
                        setAppDirectory(false);
                        this.dismissDialog();
                    }

                    @Override
                    public void onClickRight(View v) {
                        setAppDirectory(true);
                        this.dismissDialog();
                    }
                };

                DialogFactory.showActionDialog(appContext,
                        "",
                        "Include subdirectories?",
                        listener,
                        R.string.cancel_button,
                        R.string.no_button,
                        R.string.yes_button);
            }

            private void setAppDirectory(boolean useSubdirectories) {
                final File dir = adapter.getDirectory();
                final FilenameFilter filenameFilter = FileHandler.getImageFileNameFilter();

                final StringBuilder stringBuilder = new StringBuilder();

                if (useSubdirectories) {

                    Toast.makeText(appContext, "Loading subdirectories...", Toast.LENGTH_SHORT).show();

                    new Thread(new Runnable() {
                        @Override
                        public void run() {

                            int numImages = 0;

                            ArrayList<File> folderNames = getAllDirectories(dir);

                            for (File folderName : folderNames) {
                                stringBuilder.append(folderName.getAbsolutePath());
                                stringBuilder.append(AppSettings.DATA_SPLITTER);
                                numImages += folderName.list(filenameFilter).length;
                            }

                            setData(AppSettings.FOLDER,
                                    dir.getName(),
                                    stringBuilder.toString(),
                                    numImages);
                        }
                    }).start();
                }
                else {
                    setData(AppSettings.FOLDER,
                            dir.getName(),
                            dir.getAbsolutePath(),
                            dir.listFiles(filenameFilter) != null ? dir.listFiles(filenameFilter).length : 0);
                }
                adapter.setFinished();
                getActivity().onBackPressed();
            }

            private ArrayList<File> getAllDirectories(File dir) {

                ArrayList<File> directoryList = new ArrayList<>();

                File[] fileList = dir.listFiles();

                if (fileList != null) {
                    if (dir.listFiles(FileHandler.getImageFileNameFilter()).length > 0) {
                        directoryList.add(dir);
                    }

                    for (File folder : fileList) {
                        if (folder.isDirectory()) {
                            directoryList.addAll(getAllDirectories(folder));
                        }
                    }
                }

                return directoryList;

            }

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int positionInList, long id) {
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

    private void showAlbumFragment(final String type, final ArrayList<String> names,
            ArrayList<String> images, final ArrayList<String> links,
            final ArrayList<String> nums) {

        FolderFragment folderFragment = new FolderFragment();
        Bundle arguments = new Bundle();
        arguments.putBoolean(FolderFragment.USE_DIRECTORY, false);
        arguments.putBoolean(FolderFragment.SHOW_DIRECTORY_TEXT, false);
        final AlbumAdapter adapter = new AlbumAdapter(appContext, names, images, links);
        folderFragment.setArguments(arguments);
        folderFragment.setAdapter(adapter);
        folderFragment.setListener(new FolderFragment.FolderEventListener() {
            @Override
            public void onUseDirectoryClick() {
                // Not implemented
            }

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int positionInList, long id) {
                setData(type,
                        names.get(positionInList),
                        links.get(positionInList),
                        Integer.parseInt(nums.get(positionInList)));

                getActivity().onBackPressed();
            }

            @Override
            public boolean onBackPressed() {
                return true;
            }
        });

        getFragmentManager().beginTransaction()
                .add(R.id.content_frame, folderFragment, "folder_fragment")
                .addToBackStack(null)
                .commit();
    }

    private void showDropboxFragment() {

        final FolderFragment folderFragment = new FolderFragment();
        Bundle arguments = new Bundle();
        arguments.putBoolean(FolderFragment.SHOW_DIRECTORY_TEXT, true);
        arguments.putBoolean(FolderFragment.USE_DIRECTORY, true);

        final DropboxAdapter adapter = new DropboxAdapter((Activity) appContext);
        folderFragment.setArguments(arguments);
        folderFragment.setListener(new FolderFragment.FolderEventListener() {

            @Override
            public void onUseDirectoryClick() {
                Entry entry = adapter.getMainDir();
                if (entry.isDir) {
                    setData(AppSettings.DROPBOX_FOLDER, entry.fileName(), entry.path, 1);
                    adapter.setFinished(true);
                    getActivity().onBackPressed();
                }
            }

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int positionInList, long id) {
                final Entry entry = adapter.getItem(positionInList);
                if (!entry.isDir) {
                    return;
                }
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final Entry newEntry;
                        try {
                            newEntry = dropboxAPI.metadata(entry.path,
                                    0,
                                    null,
                                    true,
                                    null);
                            if (newEntry != null) {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        adapter.setDir(newEntry);
                                        folderFragment.setDirectoryText(newEntry.path);
                                    }
                                });
                            }
                        }
                        catch (DropboxException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();

            }

            @Override
            public boolean onBackPressed() {
                if (adapter.backDirectory()) {
                    return true;
                }

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final Entry parentDir;
                        try {
                            parentDir = dropboxAPI.metadata(adapter.getMainDir().parentPath(),
                                    0,
                                    null,
                                    true,
                                    null);
                            if (parentDir != null) {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        adapter.setDir(parentDir);
                                        folderFragment.setDirectoryText(parentDir.path);
                                    }
                                });
                            }
                        }
                        catch (DropboxException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();

                return false;
            }
        });

        Toast.makeText(appContext, "Loading Dropbox", Toast.LENGTH_SHORT).show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Entry startEntry;
                try {
                    startEntry = dropboxAPI.metadata("/", 0, null, true, null);
                }
                catch (DropboxException e) {
                    e.printStackTrace();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(appContext,
                                    "Error loading Dropbox",
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                    return;
                }
                adapter.setDirs(startEntry, startEntry);
                folderFragment.setAdapter(adapter);
                folderFragment.setStartingDirectoryText(startEntry.path);

                getFragmentManager().beginTransaction()
                        .add(R.id.content_frame, folderFragment, "folder_fragment")
                        .addToBackStack(null)
                        .commit();
            }
        }).start();

    }

    public void onBackPressed() {

        final int screenHeight = getResources().getDisplayMetrics().heightPixels;
        final View fragmentView = getView();

        if (fragmentView != null) {
            final float viewStartY = getView().getY();

            Animation animation = new Animation() {
                @Override
                protected void applyTransformation(float interpolatedTime, Transformation t) {
                    fragmentView.setY((screenHeight - viewStartY) * interpolatedTime + viewStartY);
                }

                @Override
                public boolean willChangeBounds() {
                    return true;
                }
            };

            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    getFragmentManager().popBackStack();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });

            animation.setDuration(SLIDE_EXIT_TIME);
            getView().startAnimation(animation);
        }
        else {
            getFragmentManager().popBackStack();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    class PicasaAlbumTask extends AsyncTask<Void, String, Void> {

        ArrayList<String> albumNames = new ArrayList<>();
        ArrayList<String> albumImageLinks = new ArrayList<>();
        ArrayList<String> albumLinks = new ArrayList<>();
        ArrayList<String> albumNums = new ArrayList<>();

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            Toast.makeText(appContext, values[0], Toast.LENGTH_SHORT).show();
        }

        @Override
        protected Void doInBackground(Void... params) {

            InputStream inputStream = null;
            BufferedReader reader = null;
            String result = null;
            try {
                publishProgress("Loading albums...");
                String authToken = null;
                authToken = GoogleAuthUtil.getToken(appContext,
                        AppSettings.getGoogleAccountName(),
                        "oauth2:https://picasaweb.google.com/data/");
                AppSettings.setGoogleAccountToken(authToken);

                HttpClient httpClient = new DefaultHttpClient();
                HttpGet httpGet = new HttpGet("https://picasaweb.google.com/data/feed/api/user/" + AppSettings.getGoogleAccountName());
                httpGet.setHeader("Authorization", "OAuth " + authToken);
                httpGet.setHeader("X-GData-Client", ApiKeys.PICASA_CLIENT_ID);
                httpGet.setHeader("GData-Version", "2");
                inputStream = httpClient.execute(httpGet).getEntity().getContent();
                reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8);
                StringBuilder stringBuilder = new StringBuilder();

                String line = null;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line + "\n");
                }
                result = stringBuilder.toString();

            }
            catch (Exception e) {
                publishProgress("Error loading albums");
            }
            finally {
                try {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                    if (reader != null) {
                        reader.close();
                    }
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }

            Document albumDoc = Jsoup.parse(result);

            for (Element link : albumDoc.select("entry")) {
                albumNames.add(link.select("title").text());
                albumImageLinks.add(link.select("media|group").select("media|content").attr("url"));
                albumLinks.add(link.select("id").text().replace("entry", "feed"));
                albumNums.add(link.select("gphoto|numphotos").text());
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {

            if (isAdded()) {
                showAlbumFragment(AppSettings.GOOGLE_ALBUM,
                        albumNames,
                        albumImageLinks,
                        albumLinks,
                        albumNums);
            }
        }
    }

}