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

package cw.kop.autobackground.sources;

import android.accounts.AccountManager;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.Fragment;
import android.app.PendingIntent;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.amlcurran.showcaseview.ShowcaseView;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.squareup.picasso.Cache;
import com.squareup.picasso.Picasso;

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
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import cw.kop.autobackground.DialogFactory;
import cw.kop.autobackground.LiveWallpaperService;
import cw.kop.autobackground.MainActivity;
import cw.kop.autobackground.MenuWrapper;
import cw.kop.autobackground.R;
import cw.kop.autobackground.accounts.GoogleAccount;
import cw.kop.autobackground.files.FileHandler;
import cw.kop.autobackground.images.AlbumFragment;
import cw.kop.autobackground.images.LocalImageFragment;
import cw.kop.autobackground.settings.ApiKeys;
import cw.kop.autobackground.settings.AppSettings;

public class SourceListFragment extends Fragment implements AdapterView.OnItemClickListener {

    public static final String ADD_ENTRY = "cw.kop.autobackground.SourceListFragment.ADD_ENTRY";
    public static final String SET_ENTRY = "cw.kop.autobackground.SourceListFragment.SET_ENTRY";

    private static final String TAG = SourceListFragment.class.getCanonicalName();
    private static final int INFO_ANIMATION_TIME = 250;

    private ListView sourceList;
    private SourceListAdapter listAdapter;
    private Context appContext;
    private Handler handler;
    private Button setButton;
    private ImageButton addButton;
    private Menu toolbarMenu;
    private TextView noImageText;
    private volatile boolean isAnimating = false;

    private int screenHeight;
    private int screenWidth;

    private ShowcaseView sourceListTutorial;
    private ShowcaseView addSourceTutorial;
    private ShowcaseView downloadTutorial;
    private ShowcaseView setTutorial;
    private ShowcaseView settingsTutorial;
    private RelativeLayout.LayoutParams buttonParams;
    private boolean setShown = false;
    private boolean tutorialShowing = false;

    private BroadcastReceiver sourceListReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            switch (action) {
                case FileHandler.DOWNLOAD_TERMINATED:
                    new ImageCountTask().execute();

            }

        }
    };

    public SourceListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);
        handler = new Handler();

        screenHeight = getResources().getDisplayMetrics().heightPixels;
        screenWidth = getResources().getDisplayMetrics().widthPixels;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        // Code by Stefan Rusek to fix possible Menu issue
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            menu = new MenuWrapper(menu) {

                private MenuItem fix(MenuItem item) {
                    try {
                        Field f = item.getClass().getDeclaredField(
                                "mEmulateProviderVisibilityOverride");
                        f.setAccessible(true);
                        f.set(item, Boolean.FALSE);
                    }
                    catch (Throwable e) {
                        e.printStackTrace();
                    }
                    return item;
                }

                @Override
                public MenuItem add(CharSequence title) {
                    return fix(super.add(title));
                }

                @Override
                public MenuItem add(int titleRes) {
                    return fix(super.add(titleRes));
                }

                @Override
                public MenuItem add(int groupId, int itemId, int order, CharSequence title) {
                    return fix(super.add(groupId, itemId, order, title));
                }

                @Override
                public MenuItem add(int groupId, int itemId, int order, int titleRes) {
                    return fix(super.add(groupId, itemId, order, titleRes));
                }
            };
        }

        inflater.inflate(R.menu.source_list_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
        toolbarMenu = menu;

        int colorFilterInt = AppSettings.getColorFilterInt(appContext);
        Drawable refreshIcon = getResources().getDrawable(R.drawable.ic_refresh_white_24dp);
        Drawable storageIcon = getResources().getDrawable(R.drawable.ic_sort_white_24dp);
        refreshIcon.setColorFilter(colorFilterInt, PorterDuff.Mode.MULTIPLY);
        storageIcon.setColorFilter(colorFilterInt, PorterDuff.Mode.MULTIPLY);

        menu.getItem(0).setIcon(refreshIcon);
        menu.getItem(2).setIcon(storageIcon);

        new ImageCountTask().execute();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        appContext = activity;
    }

    @Override
    public void onDestroy() {
        try {
            Field clearCache = Class.forName("com.squareup.picasso.Picasso").getDeclaredField(
                    "cache");
            clearCache.setAccessible(true);
            Cache cache = (Cache) clearCache.get(Picasso.with(appContext));
            cache.clear();

        }
        catch (Exception e) {
            Log.d(TAG, "" + e);
        }
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        appContext = null;
        super.onDetach();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_sources, container, false);

        sourceList = (ListView) view.findViewById(R.id.source_list);

        buttonParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        buttonParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        buttonParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        buttonParams.setMargins(0,
                0,
                0,
                Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                        100,
                        appContext.getResources().getDisplayMetrics())));

        addButton = (ImageButton) view.findViewById(R.id.floating_button);
        addButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                showSourceMenu();
            }

        });

        setButton = (Button) view.findViewById(R.id.set_button);
        setButton.setText("Set Wallpaper");
        setButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
//                if (setTutorial != null) {
//                    hide(setTutorial);
//                    showTutorial(4);
//                }
                setWallpaper();
            }

        });

//        noImageText = (TextView) view.findViewById(R.id.no_image_indicator);

        resetAddButton();

        return view;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.cycle_wallpaper:
                cycleWallpaper();
                if (AppSettings.useToast()) {
                    Toast.makeText(appContext, "Cycling wallpaper...", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.download_wallpaper:
                startDownload();
                break;
            case R.id.sort_sources:
                showSourceSortMenu();
                break;
        }

        return true;
    }

    private void cycleWallpaper() {
        Intent intent = new Intent();
        intent.setAction(LiveWallpaperService.CYCLE_IMAGE);
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        appContext.sendBroadcast(intent);
    }

    private void startDownload() {
        listAdapter.saveData();
//        if (downloadTutorial != null) {
//            hide(downloadTutorial);
//            showTutorial(3);
//        }
        if (FileHandler.isDownloading) {

            DialogFactory.ActionDialogListener listener = new DialogFactory.ActionDialogListener() {

                @Override
                public void onClickRight(View v) {
                    FileHandler.cancel(appContext);
                    resetActionBarDownload();
                    dismissDialog();
                }
            };

            DialogFactory.showActionDialog(appContext,
                    "",
                    "Cancel download?",
                    listener,
                    -1,
                    R.string.cancel_button,
                    R.string.ok_button);
        }
        else if (FileHandler.download(appContext)) {
            Drawable drawable = getResources().getDrawable(R.drawable.ic_cancel_white_24dp);
            drawable.setColorFilter(AppSettings.getColorFilterInt(appContext),
                    PorterDuff.Mode.MULTIPLY);
            toolbarMenu.getItem(1).setIcon(drawable);

            if (AppSettings.resetOnManualDownload() && AppSettings.useTimer() && AppSettings.getTimerDuration() > 0) {
                Intent intent = new Intent();
                intent.setAction(LiveWallpaperService.DOWNLOAD_WALLPAPER);
                intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(appContext, 0, intent, 0);
                AlarmManager alarmManager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
                alarmManager.cancel(pendingIntent);
                alarmManager.setInexactRepeating(AlarmManager.RTC,
                        System.currentTimeMillis() + AppSettings.getTimerDuration(),
                        AppSettings.getTimerDuration(),
                        pendingIntent);
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

        getFragmentManager().beginTransaction()
                .add(R.id.content_frame, albumFragment, "album_fragment")
                .addToBackStack(null)
                .commit();
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

    private void showSourceMenu() {

        DialogFactory.ListDialogListener clickListener = new DialogFactory.ListDialogListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                switch (position) {
                    case 0:
                        showInputDialog(AppSettings.WEBSITE,
                                "",
                                "URL",
                                "",
                                "",
                                "",
                                "",
                                "Enter website:",
                                -1);
                        break;
                    case 1:
                        showImageFragment(false, "", -1, true);
                        break;
                    case 2:
                        showInputDialog(AppSettings.IMGUR,
                                "",
                                "Subreddit",
                                "imgur.com/r/",
                                "",
                                "",
                                "",
                                "Enter Imgur subreddit:",
                                -1);
                        break;
                    case 3:
                        showInputDialog(AppSettings.IMGUR,
                                "",
                                "Album ID",
                                "imgur.com/a/",
                                "",
                                "",
                                "",
                                "Enter Imgur album:",
                                -1);
                        break;
                    case 4:
                        if (AppSettings.getGoogleAccountName().equals("")) {
                            startActivityForResult(GoogleAccount.getPickerIntent(),
                                    GoogleAccount.GOOGLE_ACCOUNT_SIGN_IN);
                        }
                        else {
                            new PicasaAlbumTask(-1, true).execute();
                        }
                        break;
                    case 5:
                        showInputDialog(AppSettings.TUMBLR_BLOG,
                                "",
                                "Blog name",
                                "",
                                "",
                                ".tumblr.com",
                                "",
                                "Enter Tumblr blog:",
                                -1);
                        break;
                    case 6:
                        showInputDialog(AppSettings.TUMBLR_TAG,
                                "",
                                "Tag",
                                "",
                                "",
                                "",
                                "",
                                "Enter Tumblr tag:",
                                -1);
                        break;
                    default:
                }
                this.dismissDialog();
            }
        };

        DialogFactory.showListDialog(appContext,
                "Source:",
                clickListener,
                R.array.source_menu);
    }

    private void showSourceSortMenu() {

        DialogFactory.ListDialogListener clickListener = new DialogFactory.ListDialogListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        listAdapter.sortData("use");
                        break;
                    case 1:
                        listAdapter.sortData("data");
                        break;
                    case 2:
                        listAdapter.sortData("title");
                        break;
                    case 3:
                        listAdapter.sortData("num");
                        break;
                    default:
                }
                dismissDialog();
            }
        };

        DialogFactory.showListDialog(appContext,
                "Sort by:",
                clickListener,
                R.array.source_sort_menu);
    }

    public void addEntry(String type, String title, String data, String num) {
        if (!listAdapter.addItem(type, title, data, true, num)) {
            Toast.makeText(appContext,
                    "Error: Title in use.\nPlease use a different title.",
                    Toast.LENGTH_SHORT).show();
        }

    }

    public void setEntry(int position,
            String type,
            String title,
            String path,
            String num,
            boolean use) {
        if (!listAdapter.setItem(position, type, title, path, use, num)) {
            Toast.makeText(appContext,
                    "Error: Title in use.\nPlease use a different title.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void showInputDialog(final String type, String title, String hint, final String prefix,
            String data, final String suffix, String num, String mainTitle,
            final int index) {

        final Dialog dialog = AppSettings.getTheme().equals(AppSettings.APP_LIGHT_THEME) ?
                new Dialog(
                        appContext,
                        R.style.LightDialogTheme) :
                new Dialog(appContext, R.style.DarkDialogTheme);

        View dialogView = View.inflate(appContext, R.layout.add_source_dialog, null);

        dialog.setContentView(dialogView);

        TextView dialogTitle = (TextView) dialogView.findViewById(R.id.dialog_title);
        final EditText sourceTitle = (EditText) dialogView.findViewById(R.id.source_title);
        final EditText sourcePrefix = (EditText) dialogView.findViewById(R.id.source_data_prefix);
        final EditText sourceData = (EditText) dialogView.findViewById(R.id.source_data);
        final EditText sourceSuffix = (EditText) dialogView.findViewById(R.id.source_data_suffix);
        final EditText sourceNum = (EditText) dialogView.findViewById(R.id.source_num);

        dialogTitle.setText(mainTitle);
        sourceTitle.setText(title);
        sourcePrefix.setText(prefix);
        sourceData.setHint(hint);
        sourceData.setText(data);
        sourceSuffix.setText(suffix);
        sourceNum.setText(num);

        Button negativeButton = (Button) dialogView.findViewById(R.id.source_negative_button);
        negativeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        Button positiveButton = (Button) dialogView.findViewById(R.id.source_positive_button);
        positiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!sourceData.getText().toString().equals("") && !sourceTitle.getText().toString().equals(
                        "")) {

                    if (sourceNum.getText().toString().equals("")) {
                        sourceNum.setText("1");
                    }

                    String newTitle = sourceTitle.getText().toString();
                    String data = prefix + sourceData.getText().toString() + suffix;

                    if ((type.equals(AppSettings.WEBSITE) ||
                            type.equals(AppSettings.IMGUR) ||
                            type.equals(AppSettings.PICASA) ||
                            type.equals(AppSettings.TUMBLR_BLOG))
                            && !data.contains("http")) {
                        data = "http://" + data;
                    }
                    else if (type.equals(AppSettings.TUMBLR_TAG)) {
                        data = "Tumblr Tag: " + data;
                    }

                    if (index >= 0) {
                        String previousTitle = AppSettings.getSourceTitle(index);
                        if (listAdapter.setItem(index,
                                type,
                                newTitle,
                                data.trim(),
                                AppSettings.useSource(index),
                                sourceNum.getText().toString())) {
                            if (!previousTitle.equals(newTitle)) {
                                AppSettings.setSourceSet(newTitle,
                                        AppSettings.getSourceSet(previousTitle));
                                FileHandler.renameFiles(appContext, previousTitle, newTitle);
                            }
                            new ImageCountTask().execute();
                            dialog.dismiss();
                        }
                        else {
                            Toast.makeText(appContext,
                                    "Error: Title in use.\nPlease use a different title.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                    else {
                        if (listAdapter.addItem(type,
                                newTitle,
                                data.trim(),
                                true,
                                sourceNum.getText().toString())) {
                            AppSettings.setSourceSet(newTitle, new HashSet<String>());
                            hide(addSourceTutorial);
                            new ImageCountTask().execute();
                            dialog.dismiss();
                        }
                        else {
                            Toast.makeText(appContext,
                                    "Error: Title in use.\nPlease use a different title.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        });


        negativeButton.setTextColor(getResources().getColor(R.color.LIGHT_BLUE_OPAQUE));
        positiveButton.setTextColor(getResources().getColor(R.color.LIGHT_BLUE_OPAQUE));

        dialog.show();

    }


    private void showDialogMenu(final int index) {

        DialogFactory.ListDialogListener clickListener = new DialogFactory.ListDialogListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                HashMap<String, String> item = listAdapter.getItem(index);
                String type = item.get("type");
                boolean use = Boolean.parseBoolean(item.get("use"));
                switch (position) {
                    case 0:
                        String directory;
                        if (type.equals(AppSettings.WEBSITE) ||
                                type.equals(AppSettings.IMGUR) ||
                                type.equals(AppSettings.PICASA) ||
                                type.equals(AppSettings.TUMBLR_BLOG) ||
                                type.equals(AppSettings.TUMBLR_TAG)) {
                            directory = AppSettings.getDownloadPath() + "/" + AppSettings.getSourceTitle(
                                    index) + " " + AppSettings.getImagePrefix();
                        }
                        else {
                            directory = AppSettings.getSourceData(index);
                        }
                        showImageFragment(false, directory, index, use);
                        break;
                    case 1:
                        switch (type) {
                            case AppSettings.WEBSITE:
                                showInputDialog(AppSettings.WEBSITE,
                                        AppSettings.getSourceTitle(index),
                                        "",
                                        "",
                                        AppSettings.getSourceData(index),
                                        "",
                                        "" + AppSettings.getSourceNum(index),
                                        "Edit website:",
                                        index);
                                break;
                            case AppSettings.IMGUR: {
                                String prefix = "", hint = "";
                                String data = AppSettings.getSourceData(index);
                                if (data.contains("imgur.com/a/")) {
                                    prefix = "imgur.com/a/";
                                    hint = "Album ID";
                                }
                                else if (data.contains("imgur.com/r/")) {
                                    prefix = "imgur.com/r/";
                                    hint = "Subreddit";
                                }

                                showInputDialog(AppSettings.IMGUR,
                                        AppSettings.getSourceTitle(index),
                                        hint,
                                        prefix,
                                        data.substring(data.indexOf(prefix) + prefix.length()),
                                        "",
                                        "" + AppSettings.getSourceNum(index),
                                        "Edit Imgur source:",
                                        index);
                                break;
                            }
                            case AppSettings.PICASA:
                                new PicasaAlbumTask(index, use).execute();
                                break;
                            case AppSettings.TUMBLR_BLOG:
                                showInputDialog(AppSettings.TUMBLR_BLOG,
                                        AppSettings.getSourceTitle(index),
                                        "Blog name",
                                        "",
                                        AppSettings.getSourceData(index),
                                        "",
                                        "" + AppSettings.getSourceNum(index),
                                        "Edit Tumblr Blog:",
                                        index);
                                break;
                            case AppSettings.TUMBLR_TAG: {
                                String data = AppSettings.getSourceData(index);

                                if (data.length() > 12) {
                                    data = data.substring(12);
                                }

                                showInputDialog(AppSettings.TUMBLR_TAG,
                                        AppSettings.getSourceTitle(index),
                                        "Tag",
                                        "",
                                        data,
                                        "",
                                        "" + AppSettings.getSourceNum(index),
                                        "Edit Tumblr Tag:",
                                        index);
                                break;
                            }
                            case AppSettings.FOLDER:
                                showImageFragment(false, "", index, use);
                                break;
                        }
                        break;
                    case 2:
                        if (type.equals(AppSettings.WEBSITE) ||
                                type.equals(AppSettings.IMGUR) ||
                                type.equals(AppSettings.PICASA) ||
                                type.equals(AppSettings.TUMBLR_BLOG) ||
                                type.equals(AppSettings.TUMBLR_TAG)) {

                            DialogFactory.ActionDialogListener clickListener = new DialogFactory.ActionDialogListener() {
                                @Override
                                public void onClickLeft(View v) {
                                    this.dismissDialog();
                                }

                                @Override
                                public void onClickMiddle(View v) {
                                    this.dismissDialog();
                                    AppSettings.setSourceSet(AppSettings.getSourceTitle(index),
                                            new HashSet<String>());
                                    listAdapter.removeItem(index);
                                    new ImageCountTask().execute();
                                }

                                @Override
                                public void onClickRight(View v) {
                                    FileHandler.deleteBitmaps(appContext, index);
                                    Toast.makeText(appContext,
                                            "Deleting " + AppSettings.getSourceTitle(index) + " images",
                                            Toast.LENGTH_SHORT).show();
                                    AppSettings.setSourceSet(AppSettings.getSourceTitle(index),
                                            new HashSet<String>());
                                    listAdapter.removeItem(index);
                                    new ImageCountTask().execute();
                                    this.dismissDialog();
                                }

                                @Override
                                public void onDismiss() {
                                    AppSettings.setTutorial(false, "source");
                                    tutorialShowing = false;
                                }
                            };

                            DialogFactory.showActionDialog(appContext,
                                    "Delete images associated with this source?",
                                    "This cannot be undone.",
                                    clickListener,
                                    R.string.cancel_button,
                                    R.string.no_button,
                                    R.string.yes_button);

                        }
                        else {
                            listAdapter.removeItem(index);
                            new ImageCountTask().execute();
                        }
                        break;
                    default:
                }
                dismissDialog();
            }
        };

        DialogFactory.showListDialog(appContext,
                AppSettings.getSourceTitle(index),
                clickListener,
                R.array.source_edit_menu);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final SourceListAdapter.CardClickListener listener = new SourceListAdapter.CardClickListener() {
            @Override
            public void onDeleteClick(final int index) {
                HashMap<String, String> item = listAdapter.getItem(index);
                String type = item.get("type");
                if (type.equals(AppSettings.WEBSITE) ||
                        type.equals(AppSettings.IMGUR) ||
                        type.equals(AppSettings.PICASA) ||
                        type.equals(AppSettings.TUMBLR_BLOG) ||
                        type.equals(AppSettings.TUMBLR_TAG)) {

                    DialogFactory.ActionDialogListener clickListener = new DialogFactory.ActionDialogListener() {

                        @Override
                        public void onClickMiddle(View v) {
                            this.dismissDialog();
                            AppSettings.setSourceSet(AppSettings.getSourceTitle(index),
                                    new HashSet<String>());
                            listAdapter.removeItem(index);
                            new ImageCountTask().execute();
                        }

                        @Override
                        public void onClickRight(View v) {
                            FileHandler.deleteBitmaps(appContext, index);
                            Toast.makeText(appContext,
                                    "Deleting " + AppSettings.getSourceTitle(index) + " images",
                                    Toast.LENGTH_SHORT).show();
                            AppSettings.setSourceSet(AppSettings.getSourceTitle(index),
                                    new HashSet<String>());
                            listAdapter.removeItem(index);
                            new ImageCountTask().execute();
                            this.dismissDialog();
                        }

                    };

                    DialogFactory.showActionDialog(appContext,
                            "Delete images along with this source?",
                            "This cannot be undone.",
                            clickListener,
                            R.string.cancel_button,
                            R.string.no_button,
                            R.string.yes_button);

                }
                else {

                    DialogFactory.ActionDialogListener clickListener = new DialogFactory.ActionDialogListener() {

                        @Override
                        public void onClickRight(View v) {
                            listAdapter.removeItem(index);
                            new ImageCountTask().execute();
                            this.dismissDialog();
                        }
                    };

                    DialogFactory.showActionDialog(appContext,
                            "",
                            "Delete " + item.get("title") + "?",
                            clickListener,
                            -1,
                            R.string.cancel_button,
                            R.string.ok_button);


                }

            }

            @Override
            public void onViewClick(int index) {
                HashMap<String, String> item = listAdapter.getItem(index);
                String type = item.get("type");
                String directory;
                if (type.equals(AppSettings.WEBSITE) ||
                        type.equals(AppSettings.IMGUR) ||
                        type.equals(AppSettings.PICASA) ||
                        type.equals(AppSettings.TUMBLR_BLOG) ||
                        type.equals(AppSettings.TUMBLR_TAG)) {
                    directory = AppSettings.getDownloadPath() + "/" + AppSettings.getSourceTitle(
                            index) + " " + AppSettings.getImagePrefix();
                }
                else {
                    directory = AppSettings.getSourceData(index);
                }
                showImageFragment(false, directory, index, false);
            }

            @Override
            public void onEditClick(int index) {
                HashMap<String, String> item = listAdapter.getItem(index);
                String type = item.get("type");
                boolean use = Boolean.parseBoolean(item.get("use"));
                switch (type) {
                    case AppSettings.WEBSITE:
                        showInputDialog(AppSettings.WEBSITE,
                                AppSettings.getSourceTitle(index),
                                "",
                                "",
                                AppSettings.getSourceData(index),
                                "",
                                "" + AppSettings.getSourceNum(index),
                                "Edit website:",
                                index);
                        break;
                    case AppSettings.IMGUR: {
                        String hint = "";
                        String prefix = "";
                        String data = AppSettings.getSourceData(index);
                        if (data.contains("imgur.com/a/")) {
                            hint = "Album ID";
                            prefix = "imgur.com/a/";
                        }
                        else if (data.contains("imgur.com/r/")) {
                            hint = "Subreddit";
                            prefix = "imgur.com/r/";
                        }

                        showInputDialog(AppSettings.IMGUR,
                                AppSettings.getSourceTitle(index),
                                hint,
                                prefix,
                                data.substring(data.indexOf(prefix) + prefix.length()),
                                "",
                                "" + AppSettings.getSourceNum(index),
                                "Edit Imgur source:",
                                index);
                        break;
                    }
                    case AppSettings.PICASA:
                        new PicasaAlbumTask(index, use).execute();
                        break;
                    case AppSettings.TUMBLR_BLOG:
                        showInputDialog(AppSettings.TUMBLR_BLOG,
                                AppSettings.getSourceTitle(index),
                                "blog name",
                                "",
                                AppSettings.getSourceData(index),
                                "",
                                "" + AppSettings.getSourceNum(index),
                                "Edit Tumblr Blog:",
                                index);
                        break;
                    case AppSettings.TUMBLR_TAG: {
                        String data = AppSettings.getSourceData(index);

                        if (data.length() > 12) {
                            data = data.substring(12);
                        }

                        showInputDialog(AppSettings.TUMBLR_TAG,
                                AppSettings.getSourceTitle(index),
                                "tag",
                                "",
                                data,
                                "",
                                "" + AppSettings.getSourceNum(index),
                                "Edit Tumblr Tag:",
                                index);
                        break;
                    }
                    case AppSettings.FOLDER:
                        showImageFragment(false, "", index, use);
                        break;
                }
            }
        };

        if (listAdapter == null) {
            listAdapter = new SourceListAdapter(getActivity(), listener);
            for (int i = 0; i < AppSettings.getNumSources(); i++) {
                listAdapter.addItem(AppSettings.getSourceType(i),
                        AppSettings.getSourceTitle(i),
                        AppSettings.getSourceData(i),
                        AppSettings.useSource(i),
                        "" + AppSettings.getSourceNum(i));
                Log.i("WLF", "Added: " + AppSettings.getSourceTitle(i));
            }
        }
        sourceList.setAdapter(listAdapter);

        sourceList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position,
                    long id) {
                listAdapter.toggleActivated(position);
                int firstVisiblePosition = sourceList.getFirstVisiblePosition();
                View childView = sourceList.getChildAt(position - firstVisiblePosition);
                sourceList.getAdapter().getView(position, childView, sourceList);
                return true;
            }
        });

        if (AppSettings.getTheme().equals(AppSettings.APP_TRANSPARENT_THEME)) {
            sourceList.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        }

        sourceList.setOnItemClickListener(this);

        // TODO: Find better implementation for action bar hide
//        sourceList.setOnScrollListener(new AbsListView.OnScrollListener() {
//
//            final static int ITEMS_THRESHOLD = 3;
//            int lastFirstVisibleItem = 0;
//
//            @Override
//            public void onScrollStateChanged(AbsListView view, int scrollState) {
//
//            }
//
//            @Override
//            public void onScroll(AbsListView view,
//                    int firstVisibleItem,
//                    int visibleItemCount,
//                    int totalItemCount) {
//
//                if (lastFirstVisibleItem < firstVisibleItem) {
//                    ActionBar actionBar = ((MainActivity) getActivity()).getSupportActionBar();
//
//                    if (actionBar.isShowing()) {
//                        actionBar.hide();
//                    }
//                }
//                else if (lastFirstVisibleItem > firstVisibleItem) {
//                    ActionBar actionBar = ((MainActivity) getActivity()).getSupportActionBar();
//                    if (!actionBar.isShowing()) {
//                        actionBar.show();
//                    }
//                }
//                lastFirstVisibleItem = firstVisibleItem;
//            }
//        });

    }

    private void hide(ShowcaseView view) {
        if (view != null) {
            view.hide();
        }
    }

    private void showTutorial() {

    }

//    private void showTutorial(int page) {
//
//        switch (page) {
//            case 0:
//                View.OnClickListener websiteListListener = new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        hide(sourceListTutorial);
//                        showTutorial(1);
//                        sourceListTutorial = null;
//                        Log.i("SLF", "Shown");
//                    }
//                };
//
//                sourceListTutorial = new ShowcaseView.Builder(getActivity())
//                        .setContentTitle("Sources List")
//                        .setContentText("This is a list of your sources. \n" +
//                                "These can include both sources and your \n" +
//                                "own image folders. You can edit them by \n" +
//                                "tapping on their boxes.")
//                        .setStyle(R.style.ShowcaseStyle)
//                        .setOnClickListener(websiteListListener)
//                        .setTarget((new ViewTarget(getActivity().findViewById(R.id.toolbar))))
//                        .build();
//                sourceListTutorial.setButtonPosition(buttonParams);
//                break;
//            case 1:
//                View.OnClickListener addWebsiteListener = new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        hide(addSourceTutorial);
//                        showTutorial(2);
//                        addSourceTutorial = null;
//                    }
//                };
//
//                addSourceTutorial = new ShowcaseView.Builder(getActivity())
//                        .setContentTitle("Adding Sources")
//                        .setContentText(
//                                "To add a new source entry, \n" +
//                                        "click the plus (+) sign. \n" +
//                                        "\n" +
//                                        "Not all sources will work, \n" +
//                                        "so if there are no images, \n" +
//                                        "try a different source. \n" +
//                                        "\n" +
//                                        "Provided is a page \n" +
//                                        "of some landscape photos \n" +
//                                        "taken by Kai Lehnberg.")
//                        .setStyle(R.style.ShowcaseStyle)
//                        .setOnClickListener(addWebsiteListener)
//                        .setTarget(new ViewTarget(addButton))
//                        .build();
//                addSourceTutorial.setButtonPosition(buttonParams);
//                break;
//            case 2:
//                View.OnClickListener downloadListener = new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        hide(downloadTutorial);
//                        showTutorial(3);
//                        downloadTutorial = null;
//                    }
//                };
//                downloadTutorial = new ShowcaseView.Builder(getActivity())
//                        .setContentTitle("Downloading Images")
//                        .setContentText("Once you have a website entered, \n" +
//                                "click this download button to start \n" +
//                                "downloading some images. \n" +
//                                "\n" +
//                                "The app will only use WiFi to \n" +
//                                "download as a default. If you \n" +
//                                "wish to change this setting, \n" +
//                                "go into the FileHandler settings \n" +
//                                "and enable mobile data.")
//                        .setStyle(R.style.ShowcaseStyle)
//                        .setTarget(new ActionItemTarget(getActivity(), toolbarMenu.getItem(1).getItemId()))
//                        .setOnClickListener(downloadListener)
//                        .build();
//                downloadTutorial.setButtonPosition(buttonParams);
//                break;
//            case 3:
//                View.OnClickListener setListener = new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        hide(setTutorial);
//                        showTutorial(4);
//                        setTutorial = null;
//                    }
//                };
//                if (setButton.getVisibility() == View.VISIBLE) {
//                    setTutorial = new ShowcaseView.Builder(getActivity())
//                            .setContentTitle("Setting the wallpaper")
//                            .setContentText("Now that it's downloading, \n" +
//                                    "it's time to set the app \n" +
//                                    "as your system wallpaper. \n" +
//                                    "Click the set button and \n" +
//                                    "hit apply on next page.")
//                            .setStyle(R.style.ShowcaseStyle)
//                            .setTarget(new ViewTarget(setButton))
//                            .setOnClickListener(setListener)
//                            .build();
//                    setTutorial.setButtonPosition(buttonParams);
//                    setShown = true;
//                }
//                else {
//                    showTutorial(4);
//                }
//                break;
//            case 4:
//                if (setShown) {
//                    hide(setTutorial);
//                }
//                View.OnClickListener settingsListener = new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        hide(settingsTutorial);
//                        showTutorial(5);
//                        settingsTutorial = null;
//                    }
//                };
//                settingsTutorial = new ShowcaseView.Builder(getActivity())
//                        .setContentTitle("Accessing Settings")
//                        .setContentText("To open the other settings, \n" +
//                                "click the entry in the top left, \n" +
//                                "which opens a list of settings.")
//                        .setStyle(R.style.ShowcaseStyle)
//                        .setOnClickListener(settingsListener)
//                        .setTarget((new ViewTarget(((Toolbar) getActivity().findViewById(R.id.toolbar)))))
//                        .build();
//                settingsTutorial.setButtonPosition(buttonParams);
//                ((MainActivity) appContext).toggleDrawer();
//                break;
//            case 5:
//                AppSettings.setTutorial(false, "source");
//                break;
//            default:
//        }
//
//    }


    protected void setWallpaper() {

        final Intent i = new Intent();
        if (Build.VERSION.SDK_INT >= 16) {
            i.setAction(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
            final String p = LiveWallpaperService.class.getPackage().getName();
            final String c = LiveWallpaperService.class.getCanonicalName();
            i.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, new ComponentName(p, c));
        }
        else {
            i.setAction(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER);
        }

        startActivityForResult(i, 0);
    }

    @Override
    public void onDestroyView() {
        sourceList.setAdapter(null);
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();

        sourceList.setOnItemClickListener(this);
        new ImageCountTask().execute();

        LocalBroadcastManager.getInstance(appContext).registerReceiver(sourceListReceiver,
                new IntentFilter(FileHandler.DOWNLOAD_TERMINATED));

        if (isServiceRunning(LiveWallpaperService.class.getName())) {
            setButton.setVisibility(View.GONE);
        }
        else {
            setButton.setVisibility(View.VISIBLE);
        }

        if (AppSettings.useSourceTutorial() && sourceListTutorial == null && !tutorialShowing) {
            Log.i("WLF", "Showing tutorial");
            tutorialShowing = true;

            DialogFactory.ActionDialogListener clickListener = new DialogFactory.ActionDialogListener() {
                @Override
                public void onClickLeft(View v) {

                }

                @Override
                public void onClickMiddle(View v) {
                    this.dismissDialog();
                }

                @Override
                public void onClickRight(View v) {
                    showTutorial();
                    this.dismissDialog();
                }

                @Override
                public void onDismiss() {
                    AppSettings.setTutorial(false, "source");
                    tutorialShowing = false;
                }
            };

            DialogFactory.showActionDialog(appContext,
                    "",
                    "Show Sources Tutorial?",
                    clickListener,
                    -1,
                    R.string.cancel_button,
                    R.string.ok_button);

        }
    }

    @Override
    public void onPause() {
        ((MainActivity) getActivity()).getSupportActionBar().show();
        listAdapter.saveData();
        LocalBroadcastManager.getInstance(appContext).unregisterReceiver(sourceListReceiver);
        super.onPause();
    }

    public void resetAddButton() {

        Drawable addDrawable = getResources().getDrawable(R.drawable.floating_button_white);
        addDrawable.setColorFilter(getResources().getColor(R.color.BLUE_OPAQUE),
                PorterDuff.Mode.MULTIPLY);
        addButton.setImageDrawable(addDrawable);

    }

    public void resetActionBarDownload() {

        Log.i("SLF", "resetActionBarDownload");

        if (toolbarMenu != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Drawable drawable = getResources().getDrawable(R.drawable.ic_file_download_white_24dp);
                    drawable.setColorFilter(AppSettings.getColorFilterInt(appContext),
                            PorterDuff.Mode.MULTIPLY);
                    toolbarMenu.getItem(1).setIcon(drawable);
                }
            });
        }
    }

    private boolean isServiceRunning(final String className) {
        final ActivityManager manager = (ActivityManager) appContext.getSystemService(Context.ACTIVITY_SERVICE);
        for (final ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (className.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
        sourceList.setOnItemClickListener(null);

        HashMap<String, String> dataItem = listAdapter.getItem(position);
        final SourceInfoFragment sourceInfoFragment = new SourceInfoFragment();
        sourceInfoFragment.setImageDrawable(((ImageView) view.findViewById(R.id.source_image)).getDrawable());
        Bundle arguments = new Bundle();
        arguments.putInt("position", position);
        arguments.putString("type", dataItem.get("type"));
        arguments.putString("title", dataItem.get("title"));
        arguments.putString("data", dataItem.get("data"));
        arguments.putString("num", dataItem.get("num"));
        arguments.putBoolean("use", Boolean.parseBoolean(dataItem.get("use")));
        String imageFileName = dataItem.get("image");
        if (imageFileName != null && imageFileName.length() > 0) {
            arguments.putString("image", imageFileName);
        }
        else {
            arguments.putString("image", "");
        }
        sourceInfoFragment.setArguments(arguments);

        final RelativeLayout sourceContainer = (RelativeLayout) view.findViewById(R.id.source_container);
        final EditText sourceTitle = (EditText) view.findViewById(R.id.source_title);
        final ImageView deleteButton = (ImageView) view.findViewById(R.id.source_delete_button);
        final ImageView viewButton = (ImageView) view.findViewById(R.id.source_view_image_button);
        final ImageView editButton = (ImageView) view.findViewById(R.id.source_edit_button);

        final float viewStartHeight = view.getHeight();
        final float viewStartY = view.getY();
        final int viewStartPadding = view.getPaddingLeft();
        final float textStartX = sourceTitle.getX();
        final float textStartY = sourceTitle.getY();
        final float textTranslationY = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                48,
                getResources().getDisplayMetrics())
                + TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                8,
                getResources().getDisplayMetrics());

        Animation animation = new Animation() {

            private boolean needsFragment = true;

            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {

                if (needsFragment && interpolatedTime >= 1) {
                    needsFragment = false;
                    getFragmentManager().beginTransaction()
                            .add(R.id.content_frame,
                                    sourceInfoFragment,
                                    "source_info_fragment")
                            .addToBackStack(null)
                            .setCustomAnimations(R.animator.slide_from_bottom,
                                    android.R.animator.fade_out)
                            .setTransition(FragmentTransaction.TRANSIT_NONE)
                            .commit();
                }
                int newPadding = Math.round(viewStartPadding * (1 - interpolatedTime));
                view.setPadding(newPadding, 0, newPadding, 0);
                view.setY(viewStartY - interpolatedTime * viewStartY);
                sourceContainer.getLayoutParams().height = (int) (viewStartHeight + screenHeight * interpolatedTime);
                sourceTitle.setY(textStartY + interpolatedTime * textTranslationY);
                sourceTitle.setX(textStartX + (viewStartPadding - newPadding));
                deleteButton.setAlpha(1.0f - interpolatedTime);
                viewButton.setAlpha(1.0f - interpolatedTime);
                editButton.setAlpha(1.0f - interpolatedTime);
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
                Parcelable state = sourceList.onSaveInstanceState();
                sourceList.setAdapter(null);
                sourceList.setAdapter(listAdapter);
                sourceList.onRestoreInstanceState(state);
                sourceList.setOnItemClickListener(SourceListFragment.this);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        ValueAnimator cardColorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(),
                getResources().getColor(R.color.DARK_BLUE_OPAQUE),
                getResources().getColor(AppSettings.getBackgroundColorResource()));
        cardColorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                sourceContainer.setBackgroundColor((Integer) animation.getAnimatedValue());
            }

        });

        ValueAnimator titleColorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(),
                sourceTitle.getCurrentTextColor(),
                AppSettings.getColorFilterInt(appContext));
        titleColorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                sourceTitle.setTextColor((Integer) animation.getAnimatedValue());
            }

        });

        ValueAnimator titleShadowAlphaAnimation = ValueAnimator.ofObject(new ArgbEvaluator(),
                getResources().getColor(R.color.BLACK_OPAQUE),
                getResources().getColor(android.R.color.transparent));
        titleShadowAlphaAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                sourceTitle.setShadowLayer(4, 0, 0, (Integer) animation.getAnimatedValue());
            }
        });

        int transitionTime = INFO_ANIMATION_TIME; //(int) Math.abs(INFO_ANIMATION_TIME * (viewStartY / screenHeight) + INFO_ANIMATION_TIME / 8);

        DecelerateInterpolator decelerateInterpolator = new DecelerateInterpolator();

        animation.setDuration(transitionTime);
        cardColorAnimation.setDuration(transitionTime);
        titleColorAnimation.setDuration(transitionTime);
        titleShadowAlphaAnimation.setDuration(transitionTime);

        animation.setInterpolator(decelerateInterpolator);
        cardColorAnimation.setInterpolator(decelerateInterpolator);
        titleColorAnimation.setInterpolator(decelerateInterpolator);
        titleShadowAlphaAnimation.setInterpolator(decelerateInterpolator);

        view.startAnimation(animation);
        cardColorAnimation.start();
        titleColorAnimation.start();
        titleShadowAlphaAnimation.start();
    }

    class ImageCountTask extends AsyncTask<Void, String, String> {
        @Override
        protected String doInBackground(Void... params) {
            return listAdapter.checkSources();
        }

        @Override
        protected void onPostExecute(String sourceState) {

            Log.i("SLA", "ImageCountTask onPostExecute");

            listAdapter.updateNum();

            resetAddButton();

            if (toolbarMenu != null) {
                Drawable drawable = FileHandler.isDownloading ?
                        getResources().getDrawable(R.drawable.ic_cancel_white_24dp) :
                        getResources().getDrawable(R.drawable.ic_file_download_white_24dp);
                drawable.setColorFilter(AppSettings.getColorFilterInt(appContext),
                        PorterDuff.Mode.MULTIPLY);
                toolbarMenu.getItem(1).setIcon(drawable);
            }


//            noImageText.setVisibility(sourceState.equals(SourceListAdapter.OKAY) ?
//                    View.GONE :
//                    View.VISIBLE);
//
//            switch (sourceState) {
//
//                case SourceListAdapter.NO_SOURCES:
//                    noImageText.setText("Please add a source");
//                    Drawable addDrawable = getResources().getDrawable(R.drawable.floating_button_white);
//                    addDrawable.setColorFilter(getResources().getColor(R.color.ALERT_TEXT),
//                            PorterDuff.Mode.MULTIPLY);
//                    addButton.setImageDrawable(addDrawable);
//                    break;
//                case SourceListAdapter.NO_ACTIVE_SOURCES:
//                    noImageText.setText("No active sources");
//                    break;
//                case SourceListAdapter.NEED_DOWNLOAD:
//                    noImageText.setText("No downloaded images");
//                    if (!FileHandler.isDownloading && toolbarMenu != null) {
//                        Drawable downloadDrawable = getResources().getDrawable(R.drawable.ic_file_download_white_24dp).mutate();
//                        downloadDrawable.setColorFilter(getResources().getColor(R.color.ALERT_TEXT),
//                                PorterDuff.Mode.MULTIPLY);
//                        toolbarMenu.getItem(1).setIcon(downloadDrawable);
//                    }
//                    break;
//                case SourceListAdapter.OKAY:
//                    break;
//
//            }

        }
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
            showAlbumFragment(AppSettings.PICASA,
                    changePosition,
                    albumNames,
                    albumImageLinks,
                    albumLinks,
                    albumNums,
                    use);
        }
    }

}