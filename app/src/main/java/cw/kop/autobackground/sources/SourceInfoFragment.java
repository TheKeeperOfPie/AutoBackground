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
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.KeyEvent;
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
import android.widget.Toast;

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
import cw.kop.autobackground.images.AlbumFragment;
import cw.kop.autobackground.images.LocalImageFragment;
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_source);
        handler = new Handler();
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
        sourcePosition = (Integer) arguments.get("position");
        int colorFilterInt = AppSettings.getColorFilterInt(appContext);

        View view = inflater.inflate(R.layout.source_info_fragment, container, false);
        View headerView = inflater.inflate(R.layout.source_info_header, null, false);

        settingsContainer = (RelativeLayout) headerView.findViewById(R.id.source_settings_container);

        sourceImage = (ImageView) headerView.findViewById(R.id.source_image);
        sourceTitle = (EditText) headerView.findViewById(R.id.source_title);
        sourcePrefix = (EditText) headerView.findViewById(R.id.source_data_prefix);
        sourceData = (EditText) headerView.findViewById(R.id.source_data);
        sourceSuffix = (EditText) headerView.findViewById(R.id.source_data_suffix);
        sourceNum = (EditText) headerView.findViewById(R.id.source_num);

        ViewGroup.LayoutParams params = sourceImage.getLayoutParams();
        params.height = (int) ((container.getWidth() -  2f * getResources().getDimensionPixelSize(R.dimen.side_margin)) / 16f * 9);
        sourceImage.setLayoutParams(params);

        cancelButton = (Button) view.findViewById(R.id.cancel_button);
        saveButton = (Button) view.findViewById(R.id.save_button);

        sourcePrefix.setTextColor(colorFilterInt);
        sourceSuffix.setTextColor(colorFilterInt);
        cancelButton.setTextColor(colorFilterInt);
        saveButton.setTextColor(colorFilterInt);

        // Adjust alpha to get faded hint color from regular text color
        int hintColor = Color.argb(0x88, Color.red(colorFilterInt), Color.green(colorFilterInt), Color.blue(colorFilterInt));

        sourceTitle.setHintTextColor(hintColor);
        sourceData.setHintTextColor(hintColor);
        sourceNum.setHintTextColor(hintColor);

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

                DialogFactory.TimeDialogListener listener = new DialogFactory.TimeDialogListener() {

                    @Override
                    public void onClickRight(View v) {
                        startHour = getTimePicker().getCurrentHour();
                        startMinute = getTimePicker().getCurrentMinute();

                        DialogFactory.TimeDialogListener listener = new DialogFactory.TimeDialogListener() {

                            @Override
                            public void onClickRight(View v) {
                                endHour = getTimePicker().getCurrentHour();
                                endMinute = getTimePicker().getCurrentMinute();

                                timePref.setSummary(String.format(
                                        "Time active: %02d:%02d - %02d:%02d",
                                        startHour,
                                        startMinute,
                                        endHour,
                                        endMinute));

                                dismissDialog();
                            }

                            @Override
                            public void onDismiss() {
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
                                "",
                                listener,
                                -1,
                                R.string.cancel_button,
                                R.string.ok_button,
                                endHour,
                                endMinute);

                        dismissDialog();
                    }

                    @Override
                    public void onDismiss() {
                        timePref.setSummary(String.format(
                                "Time active: %02d:%02d - %02d:%02d",
                                startHour,
                                startMinute,
                                endHour,
                                endMinute));
                    }
                };

                DialogFactory.showTimeDialog(appContext,
                        "Start time?",
                        "",
                        listener,
                        -1,
                        R.string.cancel_button,
                        R.string.ok_button,
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
            sourceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent,
                        View view,
                        int position,
                        long id) {

                    selectSource(position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });

            sourceSpinner.setSelection(0);
            type = AppSettings.WEBSITE;
            hint = "URL";
            prefix = "";
            suffix = "";

            startHour = 0;
            startMinute = 0;
            endHour = 0;
            endMinute = 0;
        }
        else {
            sourceImage.setVisibility(View.VISIBLE);
            sourceSpinnerText.setVisibility(View.GONE);
            sourceSpinner.setVisibility(View.GONE);

            type = arguments.getString("type");

            sourceNum.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_ENTER) {
                        saveSource();
                        return true;
                    }
                    return false;
                }
            });

            String data = arguments.getString("data");

            hint = AppSettings.getSourceDataHint(type);
            prefix = AppSettings.getSourceDataPrefix(type);
            suffix = "";

            switch (type) {
                case AppSettings.TUMBLR_BLOG:
                    suffix = ".tumblr.com";
                case AppSettings.FOLDER:
                case AppSettings.GOOGLE_ALBUM:
                    sourceTitle.setFocusable(false);
                    sourceData.setFocusable(false);
                    sourceNum.setFocusable(false);

            }

            sourceTitle.setText(arguments.getString("title"));
            sourceNum.setText(arguments.getString("num"));
            sourceData.setText(data);

            if (imageDrawable != null) {
                sourceImage.setImageDrawable(imageDrawable);
            }

            boolean showPreview = arguments.getBoolean("preview");
            if (showPreview) {
                sourceImage.setVisibility(View.VISIBLE);
            }

            ((CustomSwitchPreference) findPreference("source_show_preview")).setChecked(showPreview);

            String[] timeArray = arguments.getString("time").split(":|[ -]+");

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
        sourceUse.setChecked(arguments.getBoolean("use"));

        if (AppSettings.getTheme().equals(AppSettings.APP_LIGHT_THEME)) {
            view.setBackgroundColor(getResources().getColor(R.color.LIGHT_THEME_BACKGROUND));
        }
        else {
            view.setBackgroundColor(getResources().getColor(R.color.DARK_THEME_BACKGROUND));
        }

        ListView listView = (ListView) view.findViewById(android.R.id.list);
        listView.addHeaderView(headerView);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
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

            if (FileHandler.isDownloading) {
                Toast.makeText(appContext,
                        "Cannot edit while downloading",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            sourceIntent.setAction(SourceListFragment.SET_ENTRY);
        }

        sourceIntent.putExtra("type", type);
        sourceIntent.putExtra("title", sourceTitle.getText().toString());
        sourceIntent.putExtra("data", data);
        sourceIntent.putExtra("num", num);
        sourceIntent.putExtra("position", sourcePosition);
        sourceIntent.putExtra("use", sourceUse.isChecked());
        sourceIntent.putExtra("preview",
                ((CustomSwitchPreference) findPreference("source_show_preview")).isChecked());
        sourceIntent.putExtra("use_time", timePref.isChecked());
        sourceIntent.putExtra("time", String.format("%02d:%02d - %02d:%02d",
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
            final String prefix,
            final String data,
            final String suffix,
            final int num) {

        this.type = type;
        this.prefix = prefix;
        this.suffix = suffix;

        handler.post(new Runnable() {
            @Override
            public void run() {
                sourceTitle.setText(title);
                sourceData.setText(data);
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

    private void selectSource(int position) {

        hint = "";
        prefix = "";
        suffix = "";

        boolean blockData = false;

        if (type.equals(AppSettings.GOOGLE_ALBUM) ||
                type.equals(AppSettings.FOLDER)) {
            sourceTitle.setText("");
            sourceData.setText("");
            sourceNum.setText("");
        }

        switch (position) {
            case 0:
                type = AppSettings.WEBSITE;
                break;
            case 1:
                type = AppSettings.FOLDER;
                showImageFragment(false, "", -1, true);
                blockData = true;
                break;
            case 2:
                type = AppSettings.IMGUR_SUBREDDIT;
                break;
            case 3:
                type = AppSettings.IMGUR_ALBUM;
                break;
            case 4:
                type = AppSettings.GOOGLE_ALBUM;
                if (AppSettings.getGoogleAccountName().equals("")) {
                    startActivityForResult(GoogleAccount.getPickerIntent(),
                            GoogleAccount.GOOGLE_ACCOUNT_SIGN_IN);
                }
                else {
                    new PicasaAlbumTask(-1, true).execute();
                }
                blockData = true;
                break;
            case 5:
                type = AppSettings.TUMBLR_BLOG;
                suffix = ".tumblr.com";
                break;
            case 6:
                type = AppSettings.TUMBLR_TAG;
                break;
            case 7:
                type = AppSettings.REDDIT_SUBREDDIT;
                break;
            default:
        }

        hint = AppSettings.getSourceDataHint(type);
        prefix = AppSettings.getSourceDataPrefix(type);

        setDataWrappers();

        if (blockData) {
            sourceTitle.setFocusable(false);
            sourceData.setFocusable(false);
            sourceNum.setFocusable(false);
        }
        else {
            sourceTitle.setFocusable(true);
            sourceTitle.setFocusableInTouchMode(true);
            sourceData.setFocusable(true);
            sourceData.setFocusableInTouchMode(true);
            sourceNum.setFocusable(true);
            sourceNum.setFocusableInTouchMode(true);

            Log.i(TAG, "Reset fields");
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
                            AppSettings.setGoogleAccount(true);
                            new PicasaAlbumTask(-1, true).execute();
                        }
                        catch (IOException transientEx) {
                            return null;
                        }
                        catch (UserRecoverableAuthException e) {
                            e.printStackTrace();
                            startActivityForResult(e.getIntent(), GoogleAccount.GOOGLE_AUTH_CODE);
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
                            AppSettings.setGoogleAccount(true);
                            new PicasaAlbumTask(-1, true).execute();
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

    private void showImageFragment(boolean setPath, String viewPath, int position, boolean use) {
        LocalImageFragment localImageFragment = new LocalImageFragment();
        Bundle arguments = new Bundle();
        arguments.putBoolean("set_path", setPath);
        arguments.putString("view_path", viewPath);
        arguments.putInt("position", position);
        arguments.putBoolean("use", use);
        localImageFragment.setArguments(arguments);
        localImageFragment.setTargetFragment(this, -1);

        getFragmentManager().beginTransaction()
                .add(R.id.content_frame, localImageFragment, "image_fragment")
                .addToBackStack(null)
                .commit();
    }

    private void showAlbumFragment(String type, int position, ArrayList<String> names,
            ArrayList<String> images, ArrayList<String> links,
            ArrayList<String> nums, boolean use) {
        AlbumFragment albumFragment = new AlbumFragment();
        Bundle arguments = new Bundle();
        arguments.putString("type", type);
        arguments.putInt("position", position);
        arguments.putBoolean("use", use);
        arguments.putStringArrayList("album_names", names);
        arguments.putStringArrayList("album_images", images);
        arguments.putStringArrayList("album_links", links);
        arguments.putStringArrayList("album_nums", nums);
        albumFragment.setArguments(arguments);
        albumFragment.setTargetFragment(this, -1);

        FragmentManager fragmentManager = getFragmentManager();

        if (fragmentManager != null) {
            fragmentManager.beginTransaction()
                    .add(R.id.content_frame, albumFragment, "album_fragment")
                    .addToBackStack(null)
                    .commit();
        }
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
        private int changePosition;
        private boolean use;

        public PicasaAlbumTask(int position, boolean use) {
            changePosition = position;
            this.use = use;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            Toast.makeText(appContext, values[0], Toast.LENGTH_SHORT).show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            publishProgress("Loading albums...");
            String authToken = null;
            try {
                authToken = GoogleAuthUtil.getToken(appContext,
                        AppSettings.getGoogleAccountName(),
                        "oauth2:https://picasaweb.google.com/data/");
            }
            catch (IOException e) {
                publishProgress("Error loading albums");
                return null;
            }
            catch (GoogleAuthException e) {
                publishProgress("Error loading albums");
                return null;
            }
            AppSettings.setGoogleAccountToken(authToken);

            HttpClient httpClient = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet("https://picasaweb.google.com/data/feed/api/user/" + AppSettings.getGoogleAccountName());
            httpGet.setHeader("Authorization", "OAuth " + authToken);
            httpGet.setHeader("X-GData-Client", ApiKeys.PICASA_CLIENT_ID);
            httpGet.setHeader("GData-Version", "2");

            InputStream inputStream = null;
            BufferedReader reader = null;
            String result = null;
            try {
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
            showAlbumFragment(AppSettings.GOOGLE_ALBUM,
                    changePosition,
                    albumNames,
                    albumImageLinks,
                    albumLinks,
                    albumNums,
                    use);
        }
    }

}