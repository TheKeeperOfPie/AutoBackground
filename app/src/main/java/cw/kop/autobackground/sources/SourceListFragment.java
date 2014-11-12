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

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
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
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.CardView;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Cache;
import com.squareup.picasso.Picasso;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;

import cw.kop.autobackground.DialogFactory;
import cw.kop.autobackground.LiveWallpaperService;
import cw.kop.autobackground.MainActivity;
import cw.kop.autobackground.MenuWrapper;
import cw.kop.autobackground.R;
import cw.kop.autobackground.files.FileHandler;
import cw.kop.autobackground.images.LocalImageFragment;
import cw.kop.autobackground.settings.AppSettings;

public class SourceListFragment extends Fragment implements AdapterView.OnItemClickListener, View.OnClickListener {

    public static final String ADD_ENTRY = "cw.kop.autobackground.SourceListFragment.ADD_ENTRY";
    public static final String SET_ENTRY = "cw.kop.autobackground.SourceListFragment.SET_ENTRY";

    private static final String TAG = SourceListFragment.class.getCanonicalName();
    private static final int INFO_ANIMATION_TIME = 250;
    private static final int ADD_ANIMATION_TIME = 350;

    private ListView sourceList;
    private SourceListAdapter listAdapter;
    private Context appContext;
    private Handler handler;
    private Button setButton;
    private ImageView addButtonBackground;
    private ImageView addButton;
    private Menu toolbarMenu;

    // Volatile variables to assure animations are reset properly
    private volatile boolean needsButtonReset = false;
    private volatile boolean needsListReset = false;

    // Hold screen dimensions to calculate animations
    private int screenHeight;
    private int screenWidth;

    /**
     * Receives DOWNLOAD_TERMINATED intent to reset download button icon and recount available images
     */
    private BroadcastReceiver sourceListReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            switch (action) {
                case FileHandler.DOWNLOAD_TERMINATED:
                    new ImageCountTask().execute();
                    break;
                default:
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

        // Inflate menu and hold reference in toolbarMenu
        inflater.inflate(R.menu.source_list_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
        toolbarMenu = menu;

        // Sets correct colors of toolbar icons
        int colorFilterInt = AppSettings.getColorFilterInt(appContext);
        Drawable refreshIcon = getResources().getDrawable(R.drawable.ic_refresh_white_24dp);
        Drawable storageIcon = getResources().getDrawable(R.drawable.ic_sort_white_24dp);
        refreshIcon.setColorFilter(colorFilterInt, PorterDuff.Mode.MULTIPLY);
        storageIcon.setColorFilter(colorFilterInt, PorterDuff.Mode.MULTIPLY);
        menu.getItem(0).setIcon(refreshIcon);
        menu.getItem(2).setIcon(storageIcon);

        // Recounts images
        new ImageCountTask().execute();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        appContext = activity;
    }

    @Override
    public void onDestroy() {

        // Attempt using Reflection to force clear Picasso cache to make sure previews are accurate
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
        addButtonBackground = (ImageView) view.findViewById(R.id.floating_button);
        addButton = (ImageView) view.findViewById(R.id.floating_button_icon);
        addButton.setOnClickListener(this);
        resetAddButtonIcon();

        setButton = (Button) view.findViewById(R.id.set_button);
        setButton.setOnClickListener(this);

        return view;
    }

    @Override
    public void onClick(final View v) {

        switch (v.getId()) {

            case R.id.set_button:
                setWallpaper();
                break;
            case R.id.floating_button_icon:
                final GradientDrawable circleDrawable = (GradientDrawable) getResources().getDrawable(
                        R.drawable.floating_button_circle);
                final float scale = (float) (Math.hypot(addButtonBackground.getX(),
                        addButtonBackground.getY()) / addButtonBackground.getWidth() * 2);

                Animation animation = new Animation() {

                    private boolean needsFragment = true;
                    private float pivot;

                    @Override
                    public void initialize(int width,
                            int height,
                            int parentWidth,
                            int parentHeight) {
                        super.initialize(width, height, parentWidth, parentHeight);

                        pivot = resolveSize(RELATIVE_TO_SELF, 0.5f, width, parentWidth);
                    }

                    @Override
                    protected void applyTransformation(float interpolatedTime, Transformation t) {

                        if (needsFragment && interpolatedTime >= 1) {
                            needsFragment = false;
                            showSourceAddFragment();
                        }
                        else {
                            float scaleFactor = 1.0f + ((scale - 1.0f) * interpolatedTime);
                            t.getMatrix().setScale(scaleFactor, scaleFactor, pivot, pivot);
                        }
                    }

                    @Override
                    public boolean willChangeBounds() {
                        return true;
                    }


                };

                animation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        circleDrawable.setColor(getResources().getColor(R.color.ACCENT_OPAQUE));
                        addButtonBackground.setImageDrawable(circleDrawable);
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (needsButtonReset) {
                                    addButton.setOnClickListener(SourceListFragment.this);
                                    addButtonBackground.setScaleX(1.0f);
                                    addButtonBackground.setScaleY(1.0f);
                                    addButtonBackground.clearAnimation();
                                    circleDrawable.setColor(getResources().getColor(R.color.ACCENT_OPAQUE));
                                    addButtonBackground.setImageDrawable(circleDrawable);
                                    addButton.setVisibility(View.VISIBLE);
                                }
                            }
                        }, 100);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }

                });

                ValueAnimator buttonColorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(),
                        getResources().getColor(R.color.ACCENT_OPAQUE),
                        getResources().getColor(AppSettings.getBackgroundColorResource()));
                buttonColorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        circleDrawable.setColor((Integer) animation.getAnimatedValue());
                        addButtonBackground.setImageDrawable(circleDrawable);
                    }

                });

                DecelerateInterpolator decelerateInterpolator = new DecelerateInterpolator();

                animation.setDuration(ADD_ANIMATION_TIME);
                buttonColorAnimation.setDuration((long) (ADD_ANIMATION_TIME * 0.9));
                buttonColorAnimation.setInterpolator(decelerateInterpolator);
                animation.setInterpolator(decelerateInterpolator);

                // Post a delayed Runnable to ensure reset even if animation is interrupted
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (needsButtonReset) {
                            addButtonBackground.setScaleX(1.0f);
                            addButtonBackground.setScaleY(1.0f);
                            addButtonBackground.clearAnimation();
                            circleDrawable.setColor(getResources().getColor(R.color.ACCENT_OPAQUE));
                            addButtonBackground.setImageDrawable(circleDrawable);
                            addButton.setVisibility(View.VISIBLE);
                            needsButtonReset = false;
                        }
                    }
                }, (long) (ADD_ANIMATION_TIME * 1.1f));

                needsButtonReset = true;
                addButton.setVisibility(View.GONE);
                buttonColorAnimation.start();
                addButtonBackground.startAnimation(animation);
                break;
            default:
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.cycle_wallpaper:
                cycleWallpaper();
                sendToast("Cycling wallpaper...");
                break;
            case R.id.download_wallpaper:
                startDownload();
                break;
            case R.id.sort_sources:
                if (FileHandler.isDownloading) {
                    sendToast("Cannot sort while downloading");
                }
                else {
                    showSourceSortMenu();
                }
                break;
        }

        return true;
    }

    /**
     * Saves data in list and sends Intent to cycle wallpaper
     */
    private void cycleWallpaper() {
        listAdapter.saveData();
        Intent intent = new Intent();
        intent.setAction(LiveWallpaperService.CYCLE_IMAGE);
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        appContext.sendBroadcast(intent);
    }

    /**
     * Starts (or stops) download and sets download icon appropriately
     */
    private void startDownload() {
        listAdapter.saveData();
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

    /**
     * Shows LocalImageFragment to view images
     *
     * @param setPath  whether or not to show set directory button
     * @param viewPath top level path to start file view in
     * @param position source index with which to save data from fragment
     * @param use      whether or not source will be active
     */
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

    /**
     * Show SourceInfoFragment with position -1 to add new source
     */
    private void showSourceAddFragment() {
        final SourceInfoFragment sourceInfoFragment = new SourceInfoFragment();
        Bundle arguments = new Bundle();
        arguments.putInt("position", -1);
        arguments.putString("type", AppSettings.WEBSITE);
        arguments.putString("title", "");
        arguments.putString("data", "");
        arguments.putString("num", "");
        arguments.putBoolean("use", true);
        arguments.putBoolean("preview", true);
        arguments.putBoolean("use_time", false);
        arguments.putString("time", "00:00 - 00:00");
        sourceInfoFragment.setArguments(arguments);

        getFragmentManager().beginTransaction()
                .add(R.id.content_frame,
                        sourceInfoFragment,
                        "source_info_fragment")
                .addToBackStack(null)
                .setTransition(FragmentTransaction.TRANSIT_NONE)
                .commit();

    }

    /**
     * Shows sort menu for sources in a dialog
     */
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

    /**
     * Adds source to listAdapter
     *
     * @param type    source type
     * @param title   source title
     * @param data    source data
     * @param num     number of images for source
     * @param use     whether or not source will be active
     * @param preview show preview in source list
     * @param useTime use active time setting
     * @param time    time for active time setting
     */
    public void addEntry(String type,
            String title,
            String data,
            String num,
            boolean use,
            boolean preview,
            boolean useTime,
            String time) {
        if (!listAdapter.addItem(type, title, data, use, num, preview, useTime, time)) {
            sendToast("Error: Title in use.\nPlease use a different title.");
        }

    }

    /**
     * Changes entry in listAdapter
     *
     * @param position source index
     * @param type     source type
     * @param title    source title
     * @param data     source data
     * @param num      number of images for source
     * @param use      whether or not source will be active
     * @param preview  show preview in source list
     * @param useTime  use active time setting
     * @param time     time for active time setting
     */
    public void setEntry(int position,
            String type,
            String title,
            String data,
            String num,
            boolean use,
            boolean preview, boolean useTime, String time) {
        if (!listAdapter.setItem(position, type, title, data, use, num, preview, useTime, time)) {
            sendToast("Error: Title in use.\nPlease use a different title.");
        }
    }

    /**
     * Sets up sourceList behavior
     *
     * @param savedInstanceState
     */
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
                            sendToast("Deleting " + AppSettings.getSourceTitle(index) + " images");
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
            public void onViewImageClick(int index) {
                listAdapter.saveData();
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

                Log.i(TAG, "Directory: " + directory);

                showImageFragment(false, directory, index, false);
            }

            @Override
            public void onViewClick(View view, int index) {
                onItemClick(null, view, index, -1);
            }
        };

        if (listAdapter == null) {
            listAdapter = new SourceListAdapter(getActivity(), listener);
            for (int i = 0; i < AppSettings.getNumSources(); i++) {
                listAdapter.addItem(AppSettings.getSourceType(i),
                        AppSettings.getSourceTitle(i),
                        AppSettings.getSourceData(i),
                        AppSettings.useSource(i),
                        "" + AppSettings.getSourceNum(i),
                        AppSettings.useSourcePreview(i),
                        AppSettings.useSourceTime(i),
                        AppSettings.getSourceTime(i));
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

        sourceList.setOnItemClickListener(this);
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
    }

    @Override
    public void onPause() {
        ((MainActivity) getActivity()).getSupportActionBar().show();
        listAdapter.saveData();
        LocalBroadcastManager.getInstance(appContext).unregisterReceiver(sourceListReceiver);
        super.onPause();
    }

    public void resetAddButtonIcon() {

        Drawable addDrawable = getResources().getDrawable(R.drawable.ic_add_white_24dp);
        addDrawable.setColorFilter(AppSettings.getColorFilterInt(appContext),
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
        arguments.putBoolean("preview", Boolean.parseBoolean(dataItem.get("preview")));
        String imageFileName = dataItem.get("image");
        if (imageFileName != null && imageFileName.length() > 0) {
            arguments.putString("image", imageFileName);
        }
        else {
            arguments.putString("image", "");
        }

        arguments.putBoolean("use_time", Boolean.parseBoolean(dataItem.get("use_time")));
        arguments.putString("time", dataItem.get("time"));

        sourceInfoFragment.setArguments(arguments);

        final RelativeLayout sourceContainer = (RelativeLayout) view.findViewById(R.id.source_container);
        final CardView sourceCard = (CardView) view.findViewById(R.id.source_card);
        final View imageOverlay = view.findViewById(R.id.source_image_overlay);
        final TextView sourceTitle = (TextView) view.findViewById(R.id.source_title);
        final ImageView deleteButton = (ImageView) view.findViewById(R.id.source_delete_button);
        final ImageView viewButton = (ImageView) view.findViewById(R.id.source_view_image_button);
        final ImageView editButton = (ImageView) view.findViewById(R.id.source_edit_button);

        final float cardStartShadow = sourceCard.getPaddingLeft();
        final float viewStartHeight = view.getHeight();
        final float viewStartY = view.getY();
        final int viewStartPadding = view.getPaddingLeft();
        final float textStartX = sourceTitle.getX();
        final float textStartY = sourceTitle.getY();
        final float textTranslationY = sourceTitle.getHeight() + TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
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
                int newShadowPadding = (int) (cardStartShadow * (1.0f - interpolatedTime));
                sourceCard.setShadowPadding(newShadowPadding, 0, newShadowPadding, 0);
                view.setPadding(newPadding, 0, newPadding, 0);
                view.setY(viewStartY - interpolatedTime * viewStartY);
                sourceContainer.getLayoutParams().height = (int) (viewStartHeight + screenHeight * interpolatedTime);
//                sourceTitle.setY(textStartY + interpolatedTime * textTranslationY);
                sourceTitle.setX(textStartX + viewStartPadding - newPadding);
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
                if (needsListReset) {
                    Parcelable state = sourceList.onSaveInstanceState();
                    sourceList.setAdapter(null);
                    sourceList.setAdapter(listAdapter);
                    sourceList.onRestoreInstanceState(state);
                    sourceList.setOnItemClickListener(SourceListFragment.this);
                    needsListReset = false;
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        ValueAnimator cardColorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(),
                getResources().getColor(R.color.ACCENT_OPAQUE),
                getResources().getColor(AppSettings.getBackgroundColorResource()));
        cardColorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                sourceContainer.setBackgroundColor((Integer) animation.getAnimatedValue());
            }

        });

        ValueAnimator titleColorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(),
                sourceTitle.getCurrentTextColor(),
                getResources().getColor(R.color.BLUE_OPAQUE));
        titleColorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                sourceTitle.setTextColor((Integer) animation.getAnimatedValue());
            }

        });

        ValueAnimator titleShadowAlphaAnimation = ValueAnimator.ofObject(new ArgbEvaluator(),
                AppSettings.getColorFilterInt(appContext),
                getResources().getColor(android.R.color.transparent));
        titleShadowAlphaAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                sourceTitle.setShadowLayer(4, 0, 0, (Integer) animation.getAnimatedValue());
            }
        });

        ValueAnimator imageOverlayAlphaAnimation = ValueAnimator.ofFloat(imageOverlay.getAlpha(),
                0f);
        imageOverlayAlphaAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                imageOverlay.setAlpha((Float) animation.getAnimatedValue());
            }
        });

        int transitionTime = INFO_ANIMATION_TIME;

        DecelerateInterpolator decelerateInterpolator = new DecelerateInterpolator(1.5f);

        animation.setDuration(transitionTime);
        cardColorAnimation.setDuration(transitionTime);
        titleColorAnimation.setDuration(transitionTime);
        titleShadowAlphaAnimation.setDuration(transitionTime);

        animation.setInterpolator(decelerateInterpolator);
        cardColorAnimation.setInterpolator(decelerateInterpolator);
        titleColorAnimation.setInterpolator(decelerateInterpolator);
        titleShadowAlphaAnimation.setInterpolator(decelerateInterpolator);

        if (imageOverlay.getAlpha() > 0) {
            imageOverlayAlphaAnimation.start();
        }

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (needsListReset) {
                    Parcelable state = sourceList.onSaveInstanceState();
                    sourceList.setAdapter(null);
                    sourceList.setAdapter(listAdapter);
                    sourceList.onRestoreInstanceState(state);
                    sourceList.setOnItemClickListener(SourceListFragment.this);
                    needsListReset = false;
                }
            }
        }, (long) (transitionTime * 1.1f));

        needsListReset = true;
        view.startAnimation(animation);
        cardColorAnimation.start();
        titleColorAnimation.start();
        titleShadowAlphaAnimation.start();
    }

    private void sendToast(String toast) {

        if (AppSettings.useToast()) {
            Toast.makeText(appContext, toast, Toast.LENGTH_SHORT).show();
        }

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

            resetAddButtonIcon();

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
//                    addButtonBackground.setImageDrawable(addDrawable);
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

}