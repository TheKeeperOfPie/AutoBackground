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
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.ListFragment;
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
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.amlcurran.showcaseview.ShowcaseView;
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
import java.util.HashMap;
import java.util.HashSet;

import cw.kop.autobackground.DialogFactory;
import cw.kop.autobackground.LiveWallpaperService;
import cw.kop.autobackground.R;
import cw.kop.autobackground.accounts.GoogleAccount;
import cw.kop.autobackground.downloader.Downloader;
import cw.kop.autobackground.images.AlbumFragment;
import cw.kop.autobackground.images.LocalImageFragment;
import cw.kop.autobackground.settings.ApiKeys;
import cw.kop.autobackground.settings.AppSettings;

public class SourceListFragment extends ListFragment {

    public static final String ADD_ENTRY = "cw.kop.autobackground.SourceListFragment.ADD_ENTRY";
    public static final String SET_ENTRY = "cw.kop.autobackground.SourceListFragment.SET_ENTRY";

    private SourceListAdapter listAdapter;
    private Context appContext;
    private Handler handler;
    private Button setButton;
    private ImageButton addButton;
    private Menu toolbarMenu;

    private ShowcaseView sourceListTutorial;
    private ShowcaseView addSourceTutorial;
    private ShowcaseView downloadTutorial;
    private ShowcaseView setTutorial;
    private ShowcaseView settingsTutorial;
    private RelativeLayout.LayoutParams buttonParams;
    private boolean setShown = false;
    private boolean tutorialShowing = false;

    private BroadcastReceiver downloadButtonReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            resetActionBarDownload();
        }
    };

    public SourceListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handler = new Handler();
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        inflater.inflate(R.menu.source_list_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
        toolbarMenu = menu;

        int colorFilterInt = AppSettings.getColorFilterInt(appContext);
        Drawable refreshIcon = getResources().getDrawable(R.drawable.ic_action_refresh_white);
        Drawable downloadIcon = getResources().getDrawable(R.drawable.ic_action_download_white);
        Drawable storageIcon = getResources().getDrawable(R.drawable.ic_action_storage_white);
        refreshIcon.setColorFilter(colorFilterInt, PorterDuff.Mode.MULTIPLY);
        downloadIcon.setColorFilter(colorFilterInt, PorterDuff.Mode.MULTIPLY);
        storageIcon.setColorFilter(colorFilterInt, PorterDuff.Mode.MULTIPLY);

        menu.getItem(0).setIcon(refreshIcon);
        menu.getItem(1).setIcon(downloadIcon);
        menu.getItem(2).setIcon(storageIcon);
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

        View view = inflater.inflate(R.layout.fragment_sources, container, false);

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

        if (!AppSettings.getTheme().equals(AppSettings.APP_LIGHT_THEME)) {
            addButton.setBackgroundResource(R.drawable.floating_button_white);
        }

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

        return super.onOptionsItemSelected(item);
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
        if (Downloader.download(appContext)) {
            Drawable drawable = getResources().getDrawable(R.drawable.ic_action_cancel_white);
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
        else {

            DialogFactory.ActionDialogListener listener = new DialogFactory.ActionDialogListener() {

                @Override
                public void onClickRight(View v) {
                    Downloader.cancel(appContext);
                    resetActionBarDownload();
                    dismissDialog();
                }
            };

            DialogFactory.showActionDialog(appContext,
                                           "Cancel download?",
                                           "",
                                           listener,
                                           -1,
                                           R.string.cancel_button,
                                           R.string.ok_button);
        }
    }

    private void showImageFragment(boolean setPath, String viewPath, int position) {
        LocalImageFragment localImageFragment = new LocalImageFragment();
        Bundle arguments = new Bundle();
        arguments.putBoolean("set_path", setPath);
        arguments.putString("view_path", viewPath);
        arguments.putInt("position", position);
        localImageFragment.setArguments(arguments);

        getFragmentManager().beginTransaction()
                .add(R.id.content_frame, localImageFragment, "image_fragment")
                .addToBackStack(null)
                .commit();
    }

    private void showAlbumFragment(String type, int position, ArrayList<String> names,
                                   ArrayList<String> images, ArrayList<String> links,
                                   ArrayList<String> nums) {
        AlbumFragment albumFragment = new AlbumFragment();
        Bundle arguments = new Bundle();
        arguments.putString("type", type);
        arguments.putInt("position", position);
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
                            new PicasaAlbumTask(-1).execute();
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
                            new PicasaAlbumTask(-1).execute();
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
                        showImageFragment(false, "", -1);
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
                            new PicasaAlbumTask(-1).execute();
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
        if (listAdapter.addItem(type, title, data, true, num)) {
            listAdapter.saveData();
        }
        else {
            Toast.makeText(appContext,
                           "Error: Title in use.\nPlease use a different title.",
                           Toast.LENGTH_SHORT).show();
        }

    }

    public void setEntry(int position, String type, String title, String path, String num) {
        if (listAdapter.setItem(position, type, title, path, true, num)) {
            listAdapter.saveData();
        }
        else {
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
        final TextView sourcePrefix = (TextView) dialogView.findViewById(R.id.source_data_prefix);
        final EditText sourceData = (EditText) dialogView.findViewById(R.id.source_data);
        final TextView sourceSuffix = (TextView) dialogView.findViewById(R.id.source_data_suffix);
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
                                Downloader.renameFiles(appContext, previousTitle, newTitle);
                            }
                            listAdapter.saveData();
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
                            listAdapter.saveData();
                            AppSettings.setSourceSet(newTitle, new HashSet<String>());
                            hide(addSourceTutorial);
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

    private void showDialogMenu(final int index) {

        listAdapter.saveData();

        DialogFactory.ListDialogListener clickListener = new DialogFactory.ListDialogListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                HashMap<String, String> item = listAdapter.getItem(index);
                String type = item.get("type");
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
                        showImageFragment(false, directory, index);
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
                                new PicasaAlbumTask(index).execute();
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
                                showImageFragment(false, "", index);
                                break;
                        }
                        break;
                    case 2:
                        if (type.equals(AppSettings.WEBSITE) ||
                                type.equals(AppSettings.IMGUR) ||
                                type.equals(AppSettings.PICASA) ||
                                type.equals(AppSettings.TUMBLR_BLOG) ||
                                type.equals(AppSettings.TUMBLR_TAG)) {
                            listAdapter.saveData();

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
                                    listAdapter.saveData();
                                }

                                @Override
                                public void onClickRight(View v) {
                                    Downloader.deleteBitmaps(appContext, index);
                                    Toast.makeText(appContext,
                                                   "Deleting " + AppSettings.getSourceTitle(index) + " images",
                                                   Toast.LENGTH_SHORT).show();
                                    AppSettings.setSourceSet(AppSettings.getSourceTitle(index),
                                                             new HashSet<String>());
                                    listAdapter.removeItem(index);
                                    listAdapter.saveData();
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
                            listAdapter.saveData();
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

        if (listAdapter == null) {
            listAdapter = new SourceListAdapter(getActivity());
            for (int i = 0; i < AppSettings.getNumSources(); i++) {
                listAdapter.addItem(AppSettings.getSourceType(i),
                                    AppSettings.getSourceTitle(i),
                                    AppSettings.getSourceData(i),
                                    AppSettings.useSource(i),
                                    "" + AppSettings.getSourceNum(i));
                Log.i("WLF", "Added: " + AppSettings.getSourceTitle(i));
            }
        }
        setListAdapter(listAdapter);

        TextView emptyText = new TextView(getActivity());
        emptyText.setText("List is empty. Please add a new source entry.");
        emptyText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        emptyText.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                             ViewGroup.LayoutParams.MATCH_PARENT));
        emptyText.setGravity(Gravity.CENTER_HORIZONTAL);

        LinearLayout emptyLayout = new LinearLayout(getActivity());
        emptyLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                                  ViewGroup.LayoutParams.MATCH_PARENT));
        emptyLayout.setGravity(Gravity.TOP);
        emptyLayout.addView(emptyText);

        ((ViewGroup) getListView().getParent()).addView(emptyLayout, 0);

        getListView().setEmptyView(emptyLayout);
        getListView().setDividerHeight(0);

        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position,
                                           long id) {
                showDialogMenu(position);
                return true;
            }
        });

        if (AppSettings.getTheme().equals(AppSettings.APP_TRANSPARENT_THEME)) {
            getListView().setBackgroundColor(getResources().getColor(android.R.color.transparent));
        }

        listAdapter.updateNum();

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
//                                "go into the Downloader settings \n" +
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


    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        showDialogMenu(position);
    }

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
        setListAdapter(null);
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();

        LocalBroadcastManager.getInstance(appContext).registerReceiver(downloadButtonReceiver,
                                                                       new IntentFilter(Downloader.DOWNLOAD_TERMINATED));

        if (Downloader.isDownloading) {
            Drawable drawable = getResources().getDrawable(R.drawable.ic_action_cancel_white);
            drawable.setColorFilter(AppSettings.getColorFilterInt(appContext),
                                    PorterDuff.Mode.MULTIPLY);
            toolbarMenu.getItem(1).setIcon(drawable);
        }
        else {
            resetActionBarDownload();
        }

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
                                           "Show Sources Tutorial?",
                                           "",
                                           clickListener,
                                           -1,
                                           R.string.cancel_button,
                                           R.string.ok_button);

        }
    }

    @Override
    public void onPause() {
        super.onPause();
        listAdapter.saveData();

        LocalBroadcastManager.getInstance(appContext).unregisterReceiver(downloadButtonReceiver);
    }

    public void resetActionBarDownload() {
        if (toolbarMenu != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (AppSettings.getTheme().equals(AppSettings.APP_LIGHT_THEME)) {
                        toolbarMenu.getItem(1).setIcon(R.drawable.ic_action_download);
                    }
                    else {
                        toolbarMenu.getItem(1).setIcon(R.drawable.ic_action_download_white);
                    }
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

    class PicasaAlbumTask extends AsyncTask<Void, String, Void> {

        int changePosition;
        ArrayList<String> albumNames = new ArrayList<>();
        ArrayList<String> albumImageLinks = new ArrayList<>();
        ArrayList<String> albumLinks = new ArrayList<>();
        ArrayList<String> albumNums = new ArrayList<>();

        public PicasaAlbumTask(int position) {
            changePosition = position;
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
            super.onPostExecute(aVoid);
            showAlbumFragment(AppSettings.PICASA,
                              changePosition,
                              albumNames,
                              albumImageLinks,
                              albumLinks,
                              albumNums);
        }
    }

}
