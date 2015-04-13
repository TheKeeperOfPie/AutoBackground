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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.text.TextUtils;
import android.util.DisplayMetrics;
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
import android.widget.LinearLayout;
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
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.ParentReference;

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
import java.util.Collections;
import java.util.List;

import cw.kop.autobackground.BuildConfig;
import cw.kop.autobackground.CustomSwitchPreference;
import cw.kop.autobackground.DialogFactory;
import cw.kop.autobackground.R;
import cw.kop.autobackground.accounts.GoogleAccount;
import cw.kop.autobackground.files.FileHandler;
import cw.kop.autobackground.images.AdapterAlbum;
import cw.kop.autobackground.images.AdapterDrive;
import cw.kop.autobackground.images.AdapterDropbox;
import cw.kop.autobackground.images.AdapterImages;
import cw.kop.autobackground.images.FolderFragment;
import cw.kop.autobackground.settings.ApiKeys;
import cw.kop.autobackground.settings.AppSettings;

/**
 * Created by TheKeeperOfPie on 11/5/2014.
 */
public class SourceInfoFragment extends PreferenceFragment {

    private static final String TAG = SourceInfoFragment.class.getCanonicalName();
    private static final int FADE_IN_TIME = 350;
    private static final int SLIDE_EXIT_TIME = 350;
    private static final int DRIVE_RESOLVE_REQUEST_CODE = 9005;
    private static final int REQUEST_DRIVE_ACCOUNT = 9005;
    private static final int REQUEST_DRIVE_AUTH = 9006;
    public static final String LAYOUT_LANDSCAPE = "layoutLandscape";

    private Activity appContext;
    private Drawable imageDrawable;

    private RelativeLayout settingsContainer;
    private LinearLayout sortContainer;
    private RelativeLayout numContainer;
    private TextView sourceSpinnerText;
    private Spinner sourceSpinner;
    private ImageView sourceImage;
    private EditText sourceTitle;
    private EditText sourcePrefix;
    private EditText sourceData;
    private EditText sourceSuffix;
    private EditText sourceNumPrefix;
    private EditText sourceNum;
    private TextView sourceSortText;
    private Spinner sourceSortSpinner;
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
    private View headerView;

    private String folderData;

    private DropboxAPI<AndroidAuthSession> dropboxAPI;
    private SourceSortSpinnerAdapter sortAdapter;
    private Listener listener;
    private Drive drive;
    private GoogleAccountCredential driveCredential;
    private boolean needsRecycle;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_source);
        handler = new Handler();
        AppKeyPair appKeys = new AppKeyPair(ApiKeys.DROPBOX_KEY, ApiKeys.DROPBOX_SECRET);
        AndroidAuthSession session = new AndroidAuthSession(appKeys);
        dropboxAPI = new DropboxAPI<>(session);

        if (AppSettings.useDropboxAccount() && !TextUtils.isEmpty(AppSettings.getDropboxAccountToken())) {
            dropboxAPI.getSession().setOAuth2AccessToken(AppSettings.getDropboxAccountToken());
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        appContext = activity;
        listener = (Listener) activity;
    }

    @Override
    public void onDetach() {
        appContext = null;
        listener = null;
        super.onDetach();
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {

        int screenHeight = container.getHeight();
        int screenWidth = container.getWidth();

        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();

        if (screenWidth < 1 || screenHeight < 1) {
            screenWidth = displayMetrics.widthPixels;
            screenHeight = displayMetrics.heightPixels;
        }

        Bundle arguments = getArguments();
        sourcePosition = (Integer) arguments.get(Source.POSITION);
        int colorFilterInt = AppSettings.getColorFilterInt(appContext);

        int layout;

        if (screenHeight > screenWidth) {
            layout = R.layout.fragment_source_info_portrait;
        }
        else {
            layout = arguments.getInt(LAYOUT_LANDSCAPE, R.layout.fragment_source_info_portrait);
        }

        View view = inflater.inflate(layout, container, false);
        ListView listView = (ListView) view.findViewById(android.R.id.list);

        if (layout == R.layout.fragment_source_info_portrait) {
            headerView = inflater.inflate(R.layout.source_info_header, null, false);
            listView.addHeaderView(headerView);
        }
        else {
            headerView = view.findViewById(R.id.source_info_header);
            screenWidth /= 2;
        }

        sortContainer = (LinearLayout) headerView.findViewById(R.id.source_sort_container);
        numContainer = (RelativeLayout) headerView.findViewById(R.id.source_num_container);
        settingsContainer = (RelativeLayout) headerView.findViewById(R.id.source_settings_container);

        sourceImage = (ImageView) headerView.findViewById(R.id.source_image);
        sourceTitle = (EditText) headerView.findViewById(R.id.source_title);
        sourcePrefix = (EditText) headerView.findViewById(R.id.source_data_prefix);
        sourceData = (EditText) headerView.findViewById(R.id.source_data);
        sourceSuffix = (EditText) headerView.findViewById(R.id.source_data_suffix);
        sourceNumPrefix = (EditText) headerView.findViewById(R.id.source_num_prefix);
        sourceNum = (EditText) headerView.findViewById(R.id.source_num);
        sourceSortText = (TextView) headerView.findViewById(R.id.source_data_sort_text);
        sourceSortSpinner = (Spinner) headerView.findViewById(R.id.source_data_sort_spinner);

        sortAdapter = new SourceSortSpinnerAdapter(appContext, new ArrayList<SortData>());
        sourceSortSpinner.setAdapter(sortAdapter);

        ViewGroup.LayoutParams params = sourceImage.getLayoutParams();
        params.height = (int) ((headerView.getWidth() - 2f * getResources().getDimensionPixelSize(R.dimen.side_margin)) / 16f * 9);
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
                    case AppSettings.GOOGLE_PLUS_ALBUM:
                    case AppSettings.GOOGLE_DRIVE_ALBUM:
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            sourceSpinner.setPopupBackgroundResource(AppSettings.getDialogColorResource());
        }

        timePref = (CustomSwitchPreference) findPreference("source_time");
        timePref.setChecked(arguments.getBoolean(Source.USE_TIME));
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
            sourceNumPrefix.setVisibility(View.GONE);

            type = AppSettings.WEBSITE;

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
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });

        }
        else {
            sourceImage.setVisibility(View.VISIBLE);
            sourceSpinnerText.setVisibility(View.GONE);
            sourceSpinner.setVisibility(View.GONE);
            sourceSortText.setVisibility(View.VISIBLE);
            sourceSortSpinner.setVisibility(View.VISIBLE);
            sourceNumPrefix.setVisibility(View.VISIBLE);

            type = arguments.getString(Source.TYPE);
            setFocusBlocks();

            List<SortData> sortDataList = AppSettings.getSourceSortList(type);
            sortAdapter.setSortData(sortDataList);
            if (!sortDataList.isEmpty()) {
                int index = sortDataList.indexOf(new SortData(arguments.getString(Source.SORT, ""), "", ""));
                if (index >= 0) {
                    sourceSortSpinner.setSelection(index);
                }
            }

            folderData = arguments.getString(Source.DATA);
            String data = folderData;
            if (type.equals(AppSettings.FOLDER)) {
                data = Arrays.toString(folderData.split(AppSettings.DATA_SPLITTER));
            }

            sourceTitle.setText(arguments.getString(Source.TITLE));
            sourceData.setText(data);
            sourceNum.setText(getArguments().getInt(Source.NUM, -1) >= 0 ? "" + arguments.getInt(Source.NUM) : "");

            if (imageDrawable != null) {
                sourceImage.setImageDrawable(imageDrawable);
            }
            else if (arguments.containsKey(Source.IMAGE_FILE)) {
                needsRecycle = true;
                sourceImage.setImageBitmap(ThumbnailUtils.extractThumbnail(
                        BitmapFactory.decodeFile(arguments.getString(Source.IMAGE_FILE)),
                        screenWidth, (int) (screenWidth / 16f * 9f),
                        ThumbnailUtils.OPTIONS_RECYCLE_INPUT));
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

        sourceImage.getLayoutParams().height = (int) (screenWidth / 16 * 9);
        sourceImage.requestLayout();

        setDataWrappers();

        sourceUse = (Switch) headerView.findViewById(R.id.source_use_switch);
        sourceUse.setChecked(arguments.getBoolean(Source.USE));

        if (AppSettings.getTheme().equals(AppSettings.APP_LIGHT_THEME)) {
            view.setBackgroundColor(getResources().getColor(R.color.LIGHT_THEME_BACKGROUND));
        }
        else {
            view.setBackgroundColor(getResources().getColor(R.color.DARK_THEME_BACKGROUND));
        }

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
    public void onSaveInstanceState(Bundle outState) {

        outState.putString(Source.TYPE, type);
        outState.putString(Source.TITLE, String.valueOf(sourceTitle.getText()));
        outState.putString(Source.DATA, String.valueOf(sourceData.getText()));
        outState.putString(Source.NUM, String.valueOf(sourceNum.getText()));
        outState.putString(Source.SORT, sortAdapter.getCount() > 0 ?
                ((SortData) sortAdapter.getItem(
                        sourceSortSpinner.getSelectedItemPosition())).getTitle() : "");

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (dropboxAPI.getSession().authenticationSuccessful()) {
            try {
                dropboxAPI.getSession().finishAuthentication();

                if (!AppSettings.useDropboxAccount()) {
                    AppSettings.setUseDropboxAccount(true);
                    AppSettings.setDropboxAccountToken(dropboxAPI.getSession().getOAuth2AccessToken());
                    showDropboxFragment();
                }
            }
            catch (IllegalStateException e) {
                Log.i("DbAuthLog", "Error authenticating", e);
            }
        }

    }

    @Override
    public void onPause() {
        super.onPause();
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
                    sortContainer.setAlpha(interpolatedTime);
                    numContainer.setAlpha(interpolatedTime);
                    sourceUse.setAlpha(interpolatedTime);

                }
            };
        }
        else {
            animation = new Animation() {
                @Override
                protected void applyTransformation(float interpolatedTime, Transformation t) {

                    settingsContainer.setAlpha(interpolatedTime);
                    sortContainer.setAlpha(interpolatedTime);
                    numContainer.setAlpha(interpolatedTime);
                    sourceUse.setAlpha(interpolatedTime);

                }
            };
        }

        animation.setDuration(FADE_IN_TIME);
        animation.setInterpolator(new DecelerateInterpolator(3.0f));
        settingsContainer.startAnimation(animation);

    }



    @Override
    public void onStop() {

        if (needsRecycle && sourceImage.getDrawable() instanceof BitmapDrawable) {
            ((BitmapDrawable) sourceImage.getDrawable()).getBitmap().recycle();
        }

        super.onStop();
    }

    private void saveSource() {

        if (FileHandler.isDownloading()) {
            Toast.makeText(appContext,
                    "Cannot add/edit source while downloading",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String title = sourceTitle.getText().toString();
        String data = sourceData.getText().toString();

        if (type.equals(AppSettings.FOLDER)) {
            data = folderData;
        }

        if (TextUtils.isEmpty(title)) {
            Toast.makeText(appContext, "Title cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(data)) {
            Toast.makeText(appContext, "Data cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(sourceNum.getText().toString())) {
            Toast.makeText(appContext, "# of images cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        int num;
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

        Source source = new Source();
        source.setType(type);
        source.setTitle(sourceTitle.getText()
                .toString());
        source.setData(data);
        source.setNum(num);
        source.setUse(sourceUse.isChecked());
        source.setPreview(
                ((CustomSwitchPreference) findPreference("source_show_preview")).isChecked());
        source.setUseTime(timePref.isChecked());
        source.setTime(String.format("%02d:%02d - %02d:%02d",
                startHour, startMinute, endHour, endMinute));
        source.setSort(sourceSortSpinner.getCount() > 0 ?
                ((SortData) sourceSortSpinner.getSelectedItem()).getTitle() : "");
        Log.d(TAG, "Sort set to " + source.getSort());

        if (sourcePosition == -1) {
            if (!listener.addSource(source)) {
                listener.sendToast("Error: Title in use.\nPlease use a different title.");
                return;
            }
        }
        else {
            if (listener.saveSource(source, sourcePosition)) {
                if (!getArguments().getString(Source.TITLE).equals(title)) {
                    FileHandler.renameFolder(getArguments().getString(Source.TITLE), title);
                }
            }
            else {
                listener.sendToast("Error: Title in use.\nPlease use a different title.");
                return;
            }
        }

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
                    getFragmentManager().popBackStack();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });

            animation.setDuration(SLIDE_EXIT_TIME);
            getView().startAnimation(animation);
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
        this.folderData = data;

        handler.post(new Runnable() {
            @Override
            public void run() {
                sourceTitle.setText(title);
                sourceData.setText(SourceInfoFragment.this.type.equals(AppSettings.FOLDER) ?
                        Arrays.toString(folderData.split(AppSettings.DATA_SPLITTER)) : data);
                sourceNum.setText(num >= 0 ? "" + num : "");
                setDataWrappers();
            }
        });

    }

    private void setDataWrappers() {

        prefix = AppSettings.getSourceDataPrefix(type);
        hint = AppSettings.getSourceDataHint(type);
        suffix = AppSettings.getSourceDataSuffix(type);

        List<SortData> sortDataList = AppSettings.getSourceSortList(type);
        sortAdapter.setSortData(sortDataList);

        if (sortDataList.isEmpty()) {
            sourceSortText.setVisibility(View.GONE);
            sourceSortSpinner.setVisibility(View.GONE);
        }
        else {
            sourceSortText.setVisibility(View.VISIBLE);
            sourceSortSpinner.setVisibility(View.VISIBLE);
        }

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

        if (!type.equals(newType) &&
                (type.equals(AppSettings.FOLDER) ||
                type.equals(AppSettings.GOOGLE_PLUS_ALBUM) ||
                type.equals(AppSettings.GOOGLE_DRIVE_ALBUM) ||
                type.equals(AppSettings.DROPBOX_FOLDER))) {
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
                break;
            case AppSettings.IMGUR_SUBREDDIT:
                break;
            case AppSettings.IMGUR_ALBUM:
                break;
            case AppSettings.GOOGLE_PLUS_ALBUM:
                if (TextUtils.isEmpty(AppSettings.getGoogleAccountName())) {
                    startActivityForResult(GoogleAccount.getPickerIntent(),
                            GoogleAccount.GOOGLE_ACCOUNT_SIGN_IN);
                }
                else if (getFragmentManager().findFragmentByTag("folder_fragment") == null) {
                    new PicasaAlbumTask().execute();
                }
                break;
            case AppSettings.GOOGLE_DRIVE_ALBUM:
                if (drive == null) {
                    driveCredential = GoogleAccountCredential.usingOAuth2(
                            appContext,
                            Collections.singleton(DriveScopes.DRIVE));
                    drive = new Drive.Builder(AndroidHttp.newCompatibleTransport(),
                            GsonFactory.getDefaultInstance(), driveCredential)
                            .setApplicationName(appContext.getResources()
                                    .getString(R.string.app_name) + "/" + BuildConfig.VERSION_NAME)
                            .build();
                }
                if (TextUtils.isEmpty(AppSettings.getDriveAccountName())) {
                    startActivityForResult(driveCredential.newChooseAccountIntent(), REQUEST_DRIVE_ACCOUNT);
                }
                else {
                    driveCredential.setSelectedAccountName(AppSettings.getDriveAccountName());
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Log.d(TAG, "root: " + drive.about()
                                        .get()
                                        .execute()
                                        .getRootFolderId());
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        showDriveFragment();
                                    }
                                });
                            }
                            catch (UserRecoverableAuthIOException e) {
                                startActivityForResult(e.getIntent(), REQUEST_DRIVE_AUTH);
                            }
                            catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                }
                break;
            case AppSettings.TUMBLR_BLOG:
                break;
            case AppSettings.TUMBLR_TAG:
                break;
            case AppSettings.REDDIT_SUBREDDIT:
                break;
            case AppSettings.DROPBOX_FOLDER:
                if (!AppSettings.useDropboxAccount() || TextUtils.isEmpty(AppSettings.getDropboxAccountToken()) || !dropboxAPI.getSession().isLinked()) {
                    AppSettings.setUseDropboxAccount(false);
                    AppSettings.setDropboxAccountToken("");
                    dropboxAPI.getSession().startOAuth2Authentication(appContext);
                }
                else if (getFragmentManager().findFragmentByTag("folder_fragment") == null) {
                    showDropboxFragment();
                }
                break;
            default:
        }

        setFocusBlocks();
        setDataWrappers();

    }

    private void setFocusBlocks() {

        boolean focusData = true;
        boolean focusNum = true;

        switch (type) {
            case AppSettings.FOLDER:
                focusNum = false;
            case AppSettings.GOOGLE_DRIVE_ALBUM:
            case AppSettings.DROPBOX_FOLDER:
                focusData = false;
                break;
        }

        sourceData.setFocusableInTouchMode(focusData);
        sourceNum.setFocusableInTouchMode(focusNum);
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
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    showDriveFragment();
                                }
                            });
                        }
                        catch (UserRecoverableAuthIOException e) {
                            startActivityForResult(e.getIntent(), REQUEST_DRIVE_AUTH);
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
                            drive.about().get().execute().getRootFolderId();
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    showDriveFragment();
                                }
                            });
                        }
                        catch (UserRecoverableAuthIOException e) {
                            startActivityForResult(e.getIntent(), REQUEST_DRIVE_AUTH);
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        }

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
        final AdapterImages adapter = new AdapterImages(appContext, topDir, startDir, folderFragment);
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

            @Override
            public void onItemClick(int positionInList) {
                File selectedFile = adapter.getItem(positionInList);

                if (selectedFile.exists() && selectedFile.isDirectory()) {
                    adapter.setDirectory(selectedFile);
                    folderFragment.setDirectoryText(adapter.getDirectory()
                            .getAbsolutePath());
                }
            }

            private void setAppDirectory(boolean useSubdirectories) {
                final File dir = adapter.getDirectory();
                final FilenameFilter filenameFilter = FileHandler.getImageFileNameFilter();

                final StringBuilder stringBuilder = new StringBuilder();

                if (useSubdirectories) {

                    Toast.makeText(appContext, "Loading subdirectories...", Toast.LENGTH_SHORT)
                            .show();

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

                            if (isAdded()) {
                                setData(AppSettings.FOLDER,
                                        dir.getName(),
                                        stringBuilder.toString(),
                                        numImages);
                            }
                        }
                    }).start();
                }
                else {
                    setData(AppSettings.FOLDER,
                            dir.getName(),
                            dir.getAbsolutePath(),
                            dir.listFiles(filenameFilter) != null ?
                                    dir.listFiles(filenameFilter).length : 0);
                }
                adapter.setFinished();
                getActivity().onBackPressed();
            }

            private ArrayList<File> getAllDirectories(File dir) {

                ArrayList<File> directoryList = new ArrayList<>();

                File[] fileList = dir.listFiles();

                if (fileList != null) {
                    directoryList.add(dir);

                    for (File folder : fileList) {
                        if (folder.isDirectory()) {
                            directoryList.addAll(getAllDirectories(folder));
                        }
                    }
                }

                return directoryList;

            }

            @Override
            public boolean onBackPressed() {

                boolean endDirectory = adapter.backDirectory();
                folderFragment.setDirectoryText(adapter.getDirectory()
                        .getAbsolutePath());

                return endDirectory;
            }
        });

        getFragmentManager().beginTransaction()
                .setCustomAnimations(R.animator.none, R.animator.slide_to_bottom, R.animator.none, R.animator.slide_to_bottom)
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
        final AdapterAlbum adapter = new AdapterAlbum(appContext, names, images, links, folderFragment);
        folderFragment.setArguments(arguments);
        folderFragment.setAdapter(adapter);
        folderFragment.setListener(new FolderFragment.FolderEventListener() {
            @Override
            public void onUseDirectoryClick() {
                // Not implemented
            }

            @Override
            public void onItemClick(int positionInList) {
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
                .setCustomAnimations(R.animator.none, R.animator.slide_to_bottom, R.animator.none, R.animator.slide_to_bottom)
                .add(R.id.content_frame, folderFragment, "folder_fragment")
                .addToBackStack(null)
                .commit();
    }

    private void showDriveFragment() {

        if (!type.equals(AppSettings.GOOGLE_DRIVE_ALBUM)) {
            return;
        }

        final FolderFragment folderFragment = new FolderFragment();
        Bundle arguments = new Bundle();
        arguments.putBoolean(FolderFragment.SHOW_DIRECTORY_TEXT, true);
        arguments.putBoolean(FolderFragment.USE_DIRECTORY, true);

        final AdapterDrive adapter = new AdapterDrive(appContext, folderFragment);
        folderFragment.setArguments(arguments);
        folderFragment.setListener(new FolderFragment.FolderEventListener() {

            @Override
            public void onUseDirectoryClick() {
                com.google.api.services.drive.model.File file = adapter.getMainDir();
                setData(AppSettings.GOOGLE_DRIVE_ALBUM, file.getTitle(), file.getId(), -1);
                adapter.setFinished(true);
                getActivity().onBackPressed();
            }

            @Override
            public void onItemClick(int positionInList) {

                final com.google.api.services.drive.model.File file = adapter.getItem(positionInList);
                if (!file.getMimeType().equals("application/vnd.google-apps.folder")) {
                    return;
                }

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Drive.Files.List request = drive.files().list();
                            final FileList files = request.setQ(
                                    "'" + file.getId() + "' in parents and trashed=false").execute();
                            final List<com.google.api.services.drive.model.File> fileList = files.getItems();
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    adapter.setDir(file, fileList);
                                }
                            });
                        }
                        catch (IOException e) {
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
                        com.google.api.services.drive.model.File file = adapter.getMainDir();
                        try {
                            ParentReference parentReference = drive.parents().list(file.getId()).execute().getItems().get(0);
                            final com.google.api.services.drive.model.File parentFile = drive.files()
                                    .get(parentReference.getId())
                                    .execute();
                            Drive.Files.List request = drive.files().list();
                            FileList files = request.setQ(
                                    "'" + parentReference.getId() + "' in parents and trashed=false").execute();
                            final List<com.google.api.services.drive.model.File> fileList = files.getItems();
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    adapter.setDir(parentFile, fileList);
                                }
                            });
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();

                return false;
            }
        });

        Toast.makeText(appContext, "Loading Google Drive", Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    com.google.api.services.drive.model.File file = drive.files().get("root").execute();
                    Drive.Files.List request = drive.files().list();
                    FileList files = request.setQ("'root' in parents and trashed=false").execute();
                    adapter.setDirs(file, file, files.getItems());
                    folderFragment.setAdapter(adapter);
                    folderFragment.setStartingDirectoryText(file.getTitle());
                    getFragmentManager().beginTransaction()
                            .setCustomAnimations(R.animator.none, R.animator.slide_to_bottom,
                                    R.animator.none, R.animator.slide_to_bottom)
                            .add(R.id.content_frame, folderFragment,
                                    "folder_fragment")
                            .addToBackStack(null)
                            .commit();
                    Log.d(TAG, "folderFragment committed");
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void showDropboxFragment() {

        if (!type.equals(AppSettings.DROPBOX_FOLDER)) {
            return;
        }

        final FolderFragment folderFragment = new FolderFragment();
        Bundle arguments = new Bundle();
        arguments.putBoolean(FolderFragment.SHOW_DIRECTORY_TEXT, true);
        arguments.putBoolean(FolderFragment.USE_DIRECTORY, true);

        final AdapterDropbox adapter = new AdapterDropbox(appContext, folderFragment);
        folderFragment.setArguments(arguments);
        folderFragment.setListener(new FolderFragment.FolderEventListener() {

            @Override
            public void onUseDirectoryClick() {
                Entry entry = adapter.getMainDir();
                if (entry.isDir) {
                    setData(AppSettings.DROPBOX_FOLDER, entry.fileName(), entry.path, -1);
                    adapter.setFinished(true);
                    getActivity().onBackPressed();
                }
            }

            @Override
            public void onItemClick(int positionInList) {
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
                        .setCustomAnimations(R.animator.none, R.animator.slide_to_bottom, R.animator.none, R.animator.slide_to_bottom)
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
                showAlbumFragment(AppSettings.GOOGLE_PLUS_ALBUM,
                        albumNames,
                        albumImageLinks,
                        albumLinks,
                        albumNums);
            }
        }
    }

    public interface Listener {
        boolean addSource(Source source);
        boolean saveSource(Source source, int position);

        void sendToast(String message);
    }


}