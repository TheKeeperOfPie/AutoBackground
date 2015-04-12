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
import android.animation.LayoutTransition;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Fragment;
import android.app.PendingIntent;
import android.app.WallpaperManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.CardView;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.Transformation;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Cache;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;

import cw.kop.autobackground.DialogFactory;
import cw.kop.autobackground.LiveWallpaperService;
import cw.kop.autobackground.MainActivity;
import cw.kop.autobackground.R;
import cw.kop.autobackground.files.FileHandler;
import cw.kop.autobackground.images.AdapterImages;
import cw.kop.autobackground.images.FolderFragment;
import cw.kop.autobackground.settings.AppSettings;

public class SourceListFragment extends Fragment implements AdapterView.OnItemClickListener, View.OnClickListener {

//    public static final String ADD_ENTRY = "cw.kop.autobackground.SourceListFragment.ADD_ENTRY";
//    public static final String SET_ENTRY = "cw.kop.autobackground.SourceListFragment.SET_ENTRY";

    private static final String TAG = SourceListFragment.class.getCanonicalName();
    private static final int SCROLL_ANIMATION_TIME = 150;
    private static final int INFO_ANIMATION_TIME = 250;
    private static final int ADD_ANIMATION_TIME = 350;
    private static final long EXIT_ANIMATION_TIME = 200l;
    private static final long EXPAND_ACTION_DURATION = 200;

    private TextView alertText;
    private RecyclerView recyclerSources;
    private RecyclerView.LayoutManager layoutManager;
    private AdapterSources adapterSources;
    private Activity appContext;
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
                    sourceListListener.getControllerSources().recount();
                    break;
                default:
            }

        }
    };
    private SourceListListener sourceListListener;
    private boolean needsUpdate;

    public SourceListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);
        handler = new Handler();

        screenHeight = getResources().getDisplayMetrics().heightPixels;
        screenWidth = getResources().getDisplayMetrics().widthPixels;

        AppSettings.resetVer1_30();
        AppSettings.resetVer1_40();
        AppSettings.resetVer2_00();
        AppSettings.resetVer2_00_20();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        // Inflate menu and hold reference in toolbarMenu
        inflater.inflate(R.menu.source_list_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
        toolbarMenu = menu;

        // Sets correct colors of toolbar icons
        int colorFilterInt = AppSettings.getColorFilterInt(appContext);
        menu.findItem(R.id.item_cycle).getIcon().setColorFilter(colorFilterInt,
                PorterDuff.Mode.MULTIPLY);
        menu.findItem(R.id.item_sort).getIcon().setColorFilter(colorFilterInt,
                PorterDuff.Mode.MULTIPLY);

        // Recounts images
        sourceListListener.getControllerSources().recount();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        appContext = activity;
        sourceListListener = (SourceListListener) activity;
        sourceListListener.getControllerSources().setListener(
                new ControllerSources.SourceListener() {
                    @Override
                    public void notifyDataSetChanged() {
                        if (adapterSources != null) {
                            recyclerSources.post(new Runnable() {
                                @Override
                                public void run() {
                                    adapterSources.notifyDataSetChanged();
                                }
                            });
                        }
                        else {
                            needsUpdate = true;
                        }
                    }

                    @Override
                    public void onChangeState() {
                        alertText.post(new Runnable() {
                            @Override
                            public void run() {
                                setAlertText();
                            }
                        });
                    }
                });
        needsUpdate = true;
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
        sourceListListener.getControllerSources().setListener(null);
        sourceListListener = null;
        super.onDetach();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_sources, container, false);

        final ImageView emptyArrow = (ImageView) view.findViewById(R.id.empty_arrow);

        alertText = (TextView) view.findViewById(R.id.source_alert_text);

        if (screenHeight > screenWidth) {
            layoutManager = new LinearLayoutManager(appContext, LinearLayoutManager.VERTICAL,
                    false);
        }
        else {
            layoutManager = new GridLayoutManager(appContext, 2, LinearLayoutManager.VERTICAL, false);
        }

        recyclerSources = (RecyclerView) view.findViewById(R.id.source_list);
        recyclerSources.setLayoutManager(layoutManager);
        recyclerSources.setHasFixedSize(true);
        addButtonBackground = (ImageView) view.findViewById(R.id.floating_button);
        addButton = (ImageView) view.findViewById(R.id.floating_button_icon);
        addButton.setOnClickListener(this);
        resetAddButtonIcon();

        setButton = (Button) view.findViewById(R.id.set_button);
        setButton.setOnClickListener(this);

        final AdapterSources.AdapterSourceListener listener = new AdapterSources.AdapterSourceListener() {

            @Override
            public void onDownloadClick(View view, Source source) {

                ArrayList<Source> sources = new ArrayList<>(1);
                sources.add(source);

                sourceListListener.getControllerSources().saveData();
                if (FileHandler.isDownloading()) {

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
                else if (FileHandler.download(appContext, sources)) {
                    toolbarMenu.findItem(R.id.item_download).setIcon(
                            R.drawable.ic_cancel_white_24dp);
                    toolbarMenu.findItem(R.id.item_download).getIcon()
                            .setColorFilter(AppSettings.getColorFilterInt(appContext),
                                    PorterDuff.Mode.MULTIPLY);

                    if (AppSettings.resetOnManualDownload() && AppSettings.useTimer()) {
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

            @Override
            public void onDeleteClick(final View view, final int index) {

                sourceListListener.getControllerSources().saveData();
                if (FileHandler.isDownloading()) {
                    Toast.makeText(appContext,
                            "Cannot delete while downloading",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                Source item = sourceListListener.getControllerSources().get(index);
                String type = item.getType();
                if (type.equals(AppSettings.FOLDER)) {

                    DialogFactory.ActionDialogListener clickListener = new DialogFactory.ActionDialogListener() {

                        @Override
                        public void onClickRight(View v) {
                            sourceListListener.getControllerSources().removeItem(index);
                            sourceListListener.getControllerSources().recount();
                            this.dismissDialog();
                        }
                    };

                    DialogFactory.showActionDialog(appContext,
                            "",
                            "Delete " + item.getTitle() + "?",
                            clickListener,
                            -1,
                            R.string.cancel_button,
                            R.string.ok_button);


                }
                else {
                    DialogFactory.ActionDialogListener clickListener = new DialogFactory.ActionDialogListener() {

                        @Override
                        public void onClickMiddle(View v) {
                            this.dismissDialog();
                            sourceListListener.getControllerSources().removeItem(index);
                            sourceListListener.getControllerSources().recount();
                        }

                        @Override
                        public void onClickRight(View v) {
                            Source source = sourceListListener.getControllerSources().get(index);
                            FileHandler.deleteBitmaps(source);
                            sendToast("Deleting " + source.getTitle() + " images");
                            sourceListListener.getControllerSources().removeItem(index);
                            sourceListListener.getControllerSources().recount();
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

            }

            @Override
            public void onViewImageClick(View view, int index) {
                showViewImageFragment(view, index);
            }

            @Override
            public void onEditClick(View view, int index) {
                showSourceEditFragment(view, index);
            }

            @Override
            public void onExpandClick(View view, int position) {
                onItemClick(null, view, position, 0);
            }

            @Override
            public void onLongClick(int position) {
                onItemLongClick(position);
            }

            @Override
            public Activity getActivity() {
                return appContext;
            }

            @Override
            public void setEmptyArrowVisibility(int visibility) {
                emptyArrow.setVisibility(visibility);
            }

            @Override
            public float getItemWidth() {
                if (layoutManager instanceof GridLayoutManager) {
                    return recyclerSources.getWidth() / ((GridLayoutManager) layoutManager).getSpanCount();
                }
                return recyclerSources.getWidth();
            }

            @Override
            public int getSpanForPosition(int position) {
                if (layoutManager instanceof GridLayoutManager) {
                    return position % 2;
                }
                return 0;
            }
        };

        if (adapterSources == null) {
            adapterSources = new AdapterSources(getActivity(),
                    sourceListListener.getControllerSources(), listener);
        }
        adapterSources.notifyDataSetChanged();
        recyclerSources.setAdapter(adapterSources);

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
                final float scale = (float) ((Math.hypot(addButtonBackground.getX(),
                        addButtonBackground.getY()) + addButtonBackground.getWidth()) / addButtonBackground.getWidth() * 2);

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
                                    circleDrawable.setColor(
                                            getResources().getColor(R.color.ACCENT_OPAQUE));
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
            case R.id.item_cycle:
                cycleWallpaper();
                sendToast("Cycling wallpaper...");
                break;
            case R.id.item_download:
                startDownload();
                break;
            case R.id.item_sort_activated:
                sourceListListener.getControllerSources().sortData(Source.USE);
                item.setEnabled(true);
                break;
            case R.id.item_sort_location:
                sourceListListener.getControllerSources().sortData(Source.DATA);
                item.setEnabled(true);
                break;
            case R.id.item_sort_name:
                sourceListListener.getControllerSources().sortData(Source.TITLE);
                item.setEnabled(true);
                break;
            case R.id.item_sort_number:
                sourceListListener.getControllerSources().sortData(Source.NUM);
                item.setEnabled(true);
                break;
        }

        return true;
    }

    /**
     * Saves data in list and sends Intent to cycle wallpaper
     */
    private void cycleWallpaper() {
        sourceListListener.getControllerSources().saveData();
        Intent intent = new Intent();
        intent.setAction(LiveWallpaperService.CYCLE_IMAGE);
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        appContext.sendBroadcast(intent);
    }

    /**
     * Starts (or stops) download and sets download icon appropriately
     */
    private void startDownload() {
        sourceListListener.getControllerSources().saveData();
        if (FileHandler.isDownloading()) {

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
            toolbarMenu.findItem(R.id.item_download).setIcon(R.drawable.ic_cancel_white_24dp);
            toolbarMenu.findItem(R.id.item_download)
                    .getIcon()
                    .setColorFilter(AppSettings.getColorFilterInt(appContext),
                            PorterDuff.Mode.MULTIPLY);

            if (AppSettings.resetOnManualDownload() && AppSettings.useTimer()) {
                Intent intent = new Intent();
                intent.setAction(LiveWallpaperService.DOWNLOAD_WALLPAPER);
                intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(appContext, 0, intent, 0);

                AlarmManager alarmManager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
                alarmManager.cancel(pendingIntent);

                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(System.currentTimeMillis());
                calendar.set(Calendar.HOUR_OF_DAY, AppSettings.getTimerHour());
                calendar.set(Calendar.MINUTE, AppSettings.getTimerMinute());

                if (calendar.getTimeInMillis() > System.currentTimeMillis()) {
                    alarmManager.setInexactRepeating(AlarmManager.RTC,
                            calendar.getTimeInMillis(),
                            AppSettings.getTimerDuration(),
                            pendingIntent);
                }
                else {
                    alarmManager.setInexactRepeating(AlarmManager.RTC,
                            calendar.getTimeInMillis() + AlarmManager.INTERVAL_DAY,
                            AppSettings.getTimerDuration(),
                            pendingIntent);
                }
            }
        }
    }

    /**
     * Shows LocalImageFragment to view images
     *
     * @param view  source card which was selected
     * @param index position of source in adapterSources
     */
    private void showViewImageFragment(final View view, final int index) {
        recyclerSources.setClickable(false);
        recyclerSources.setEnabled(false);

        sourceListListener.getControllerSources().saveData();
        Source item = sourceListListener.getControllerSources().get(index);
        String type = item.getType();
        File directory;
        if (type.equals(AppSettings.FOLDER)) {
            directory = new File(item.getData().split(AppSettings.DATA_SPLITTER)[0]);
        }
        else {
            directory = new File(AppSettings.getDownloadPath() + "/" + item.getTitle() + " " + AppSettings.getImagePrefix());
        }

        Log.i(TAG, "Directory: " + directory);

        final RelativeLayout sourceContainer = (RelativeLayout) view.findViewById(R.id.source_container);
        final ImageView sourceImage = (ImageView) view.findViewById(R.id.source_image);
        final View imageOverlay = view.findViewById(R.id.source_image_overlay);
        final EditText sourceTitle = (EditText) view.findViewById(R.id.source_title);
        final Toolbar toolbarActions = (Toolbar) view.findViewById(R.id.toolbar_actions);
        final LinearLayout sourceExpandContainer = (LinearLayout) view.findViewById(R.id.source_expand_container);

        final boolean fadeView = directory.list() == null || directory.list().length == 0;
        final float viewStartHeight = sourceContainer.getHeight();
        final float viewStartY = view.getY();
        final float overlayStartAlpha = imageOverlay.getAlpha();
        final float listHeight = recyclerSources.getHeight();
        Log.i(TAG, "listHeight: " + listHeight);
        Log.i(TAG, "viewStartHeight: " + viewStartHeight);

        final FolderFragment folderFragment = new FolderFragment();
        Bundle arguments = new Bundle();
        arguments.putBoolean(FolderFragment.SHOW_DIRECTORY_TEXT, false);
        arguments.putBoolean(FolderFragment.USE_DIRECTORY, false);
        final AdapterImages adapter = new AdapterImages(appContext, directory, directory, folderFragment);
        folderFragment.setArguments(arguments);
        folderFragment.setAdapter(adapter);
        folderFragment.setListener(new FolderFragment.FolderEventListener() {

            @Override
            public void onUseDirectoryClick() {
                // Not implemented
            }

            @Override
            public void onItemClick(final int positionInList) {

                File selectedFile = adapter.getItem(positionInList);

                if (selectedFile.exists() && selectedFile.isDirectory()) {
                    adapter.setDirectory(selectedFile);
                }
                else if (FileHandler.getImageFileNameFilter().accept(null, adapter.getItem(positionInList).getName())) {
                    DialogFactory.ListDialogListener clickListener = new DialogFactory.ListDialogListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int positionInDialog, long id) {
                            switch (positionInDialog) {
                                case 0:
                                    Intent galleryIntent = new Intent();
                                    galleryIntent.setAction(Intent.ACTION_VIEW);
                                    galleryIntent.setDataAndType(Uri.fromFile(adapter.getItem(
                                            positionInList)), "image/*");
                                    galleryIntent = Intent.createChooser(galleryIntent, "Open Image");
                                    galleryIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    appContext.startActivity(galleryIntent);
                                    break;
                                case 1:
                                    File file = adapter.getItem(positionInList);

                                    if (file.exists() && file.isFile()) {
                                        if (FileHandler.getCurrentBitmapFile() != null && file.getAbsolutePath().equals(FileHandler.getCurrentBitmapFile().getAbsolutePath())) {
                                            Intent intent = new Intent();
                                            intent.setAction(LiveWallpaperService.CYCLE_IMAGE);
                                            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                                            appContext.sendBroadcast(intent);
                                        }
                                        file.delete();
                                        adapter.remove(positionInList);
                                    }
                            }
                            dismissDialog();
                        }
                    };

                    DialogFactory.showListDialog(appContext,
                            "",
                            clickListener,
                            R.array.history_menu);
                }
            }

            @Override
            public boolean onBackPressed() {
                return adapter.backDirectory();
            }
        });

        final boolean animateSideBySide;

        final View viewAdjacent;
        final RelativeLayout sourceContainerAdjacent;
        final ImageView sourceImageAdjacent;
        final View imageOverlayAdjacent;
        final EditText sourceTitleAdjacent;
        final Toolbar toolbarActionsAdjacent;
        final LinearLayout sourceExpandContainerAdjacent;

        if (!(index % 2 == 1 && adapter.getItemCount() == index - 1) && layoutManager instanceof GridLayoutManager) {
            animateSideBySide = true;
            viewAdjacent = recyclerSources.findViewHolderForPosition(index % 2 == 0 ? index + 1 : index - 1).itemView;

            sourceContainerAdjacent = (RelativeLayout) viewAdjacent.findViewById(R.id.source_container);
            sourceImageAdjacent = (ImageView) viewAdjacent.findViewById(R.id.source_image);
            imageOverlayAdjacent = viewAdjacent.findViewById(R.id.source_image_overlay);
            sourceTitleAdjacent = (EditText) viewAdjacent.findViewById(R.id.source_title);
            toolbarActionsAdjacent = (Toolbar) viewAdjacent.findViewById(R.id.toolbar_actions);
            sourceExpandContainerAdjacent = (LinearLayout) viewAdjacent.findViewById(R.id.source_expand_container);
        }
        else {
            viewAdjacent = null;
            sourceContainerAdjacent = null;
            sourceImageAdjacent = null;
            imageOverlayAdjacent = null;
            sourceTitleAdjacent = null;
            toolbarActionsAdjacent = null;
            sourceExpandContainerAdjacent = null;
            animateSideBySide = false;
        }

        Animation animation = new Animation() {

            private boolean needsFragment = true;

            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {

                if (needsFragment && interpolatedTime >= 1) {
                    needsFragment = false;
                    getFragmentManager().beginTransaction()
                            .setCustomAnimations(R.animator.none, R.animator.slide_to_bottom, R.animator.none, R.animator.slide_to_bottom)
                            .add(R.id.content_frame, folderFragment, "folder_fragment")
                            .addToBackStack(null)
                            .commit();
                }
                sourceContainer.getLayoutParams().height = (int) (viewStartHeight + (listHeight - viewStartHeight) * interpolatedTime);
                sourceContainer.requestLayout();
                view.setY(viewStartY - interpolatedTime * viewStartY);
                toolbarActions.setAlpha(1.0f - interpolatedTime);
                sourceTitle.setAlpha(1.0f - interpolatedTime);
                imageOverlay.setAlpha(overlayStartAlpha - overlayStartAlpha * (1.0f - interpolatedTime));
                sourceExpandContainer.setAlpha(1.0f - interpolatedTime);

                if (fadeView) {
                    sourceImage.setAlpha(1.0f - interpolatedTime);
                }

                view.requestLayout();

                if (animateSideBySide) {
                    sourceContainerAdjacent.getLayoutParams().height = (int) (viewStartHeight + (listHeight - viewStartHeight) * interpolatedTime);
                    sourceContainerAdjacent.requestLayout();
                    viewAdjacent.setY(viewStartY - interpolatedTime * viewStartY);
                    toolbarActionsAdjacent.setAlpha(1.0f - interpolatedTime);
                    sourceTitleAdjacent.setAlpha(1.0f - interpolatedTime);
                    imageOverlayAdjacent.setAlpha(overlayStartAlpha - overlayStartAlpha * (1.0f - interpolatedTime));
                    sourceExpandContainerAdjacent.setAlpha(1.0f - interpolatedTime);
                    sourceImageAdjacent.setAlpha(1.0f - interpolatedTime);
                    viewAdjacent.requestLayout();
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
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (needsListReset) {
                    Parcelable state = layoutManager.onSaveInstanceState();
                    recyclerSources.setAdapter(null);
                    recyclerSources.setAdapter(adapterSources);
                    layoutManager.onRestoreInstanceState(state);
                    recyclerSources.setClickable(true);
                    recyclerSources.setEnabled(true);
                    needsListReset = false;
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        ValueAnimator cardColorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(),
                AppSettings.getDialogColor(appContext),
                getResources().getColor(AppSettings.getBackgroundColorResource()));
        cardColorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                sourceContainer.setBackgroundColor((Integer) animation.getAnimatedValue());
                if (animateSideBySide) {
                    sourceContainerAdjacent.setBackgroundColor(
                            (Integer) animation.getAnimatedValue());
                }
            }

        });

        DecelerateInterpolator decelerateInterpolator = new DecelerateInterpolator(1.5f);

        animation.setDuration(INFO_ANIMATION_TIME);
        cardColorAnimation.setDuration(INFO_ANIMATION_TIME);

        animation.setInterpolator(decelerateInterpolator);
        cardColorAnimation.setInterpolator(decelerateInterpolator);

        needsListReset = true;
        cardColorAnimation.start();
        view.startAnimation(animation);

    }

    /**
     * Show SourceInfoFragment with position -1 to add new source
     */
    private void showSourceAddFragment() {
        final SourceInfoFragment sourceInfoFragment = new SourceInfoFragment();
        Bundle arguments = new Bundle();
        arguments.putInt(Source.POSITION, -1);
        arguments.putString(Source.TYPE, AppSettings.WEBSITE);
        arguments.putString(Source.TITLE, "");
        arguments.putString(Source.DATA, "");
        arguments.putInt(Source.NUM, -1);
        arguments.putString(Source.SORT, "");
        arguments.putBoolean(Source.USE, true);
        arguments.putBoolean(Source.PREVIEW, true);
        arguments.putBoolean(Source.USE_TIME, false);
        arguments.putString(Source.TIME, "00:00 - 00:00");
        sourceInfoFragment.setArguments(arguments);

        getFragmentManager().beginTransaction()
                .add(R.id.content_frame,
                        sourceInfoFragment,
                        "sourceInfoFragment")
                .addToBackStack(null)
                .setTransition(FragmentTransaction.TRANSIT_NONE)
                .commit();

    }

    /**
     * Sets up recyclerSources behavior
     *
     * @param savedInstanceState
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    protected void setWallpaper() {

        final Intent intent = new Intent();
        if (Build.VERSION.SDK_INT >= 16) {
            intent.setAction(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
            final String packageName = LiveWallpaperService.class.getPackage().getName();
            final String className = LiveWallpaperService.class.getCanonicalName();
            intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    new ComponentName(packageName, className));
        }
        else {
            intent.setAction(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER);
        }

        try {
            startActivityForResult(intent, 0);
        }
        catch (ActivityNotFoundException e) {
            sendToast("Error loading wallpaper chooser,\n" +
                    "please set manually.");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroyView() {
        recyclerSources.setAdapter(null);
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();

        sourceListListener.getControllerSources().recount();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(FileHandler.DOWNLOAD_TERMINATED);
        LocalBroadcastManager.getInstance(appContext).registerReceiver(sourceListReceiver,
                intentFilter);

        if (isServiceRunning(LiveWallpaperService.class.getName())) {
            setButton.setVisibility(View.GONE);
        }
        else {
            setButton.setVisibility(View.VISIBLE);
        }

        if (needsUpdate) {
            adapterSources.notifyDataSetChanged();
            needsUpdate = false;
        }

    }

    @Override
    public void onPause() {
        ((MainActivity) getActivity()).getSupportActionBar().show();
        sourceListListener.getControllerSources().saveData();
        LocalBroadcastManager.getInstance(appContext).unregisterReceiver(sourceListReceiver);
        super.onPause();
    }

    public void resetAddButtonIcon() {

        if (isAdded()) {
            Drawable addDrawable = getResources().getDrawable(R.drawable.ic_add_white_24dp);
            addDrawable.setColorFilter(AppSettings.getColorFilterInt(appContext),
                    PorterDuff.Mode.MULTIPLY);
            addButton.setImageDrawable(addDrawable);
        }

    }

    public void resetActionBarDownload() {

        if (isAdded()) {
            if (toolbarMenu != null) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        toolbarMenu.findItem(R.id.item_download).setIcon(
                                R.drawable.ic_file_download_white_24dp);
                        toolbarMenu.findItem(R.id.item_download).getIcon().setColorFilter(
                                AppSettings.getColorFilterInt(appContext),
                                PorterDuff.Mode.MULTIPLY);
                    }
                });
            }
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
    public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {

        LinearLayout expandedView = (LinearLayout) view.findViewById(R.id.source_expand_container);
        expandedView.setVisibility(expandedView.isShown() ? View.GONE : View.VISIBLE);
        recyclerSources.smoothScrollToPosition(position);

//        final View expandedView = view.findViewById(R.id.source_expand_container);
//        expandedView.measure(View.MeasureSpec.AT_MOST, View.MeasureSpec.AT_MOST);
//
//        final int height = expandedView.getMeasuredHeight();
//
//        Toast.makeText(appContext, "Expand target height: " + height, Toast.LENGTH_SHORT).show();
//
//        Animation animation;
//        if (expandedView.getVisibility() == View.VISIBLE) {
//            animation = new Animation() {
//                @Override
//                protected void applyTransformation(float interpolatedTime, Transformation t) {
//                    expandedView.getLayoutParams().height = (int) (height * (1.0f - interpolatedTime));
//                    expandedView.requestLayout();
//                }
//
//                @Override
//                public boolean willChangeBounds() {
//                    return true;
//                }
//            };
//            animation.setAnimationListener(new Animation.AnimationListener() {
//                @Override
//                public void onAnimationStart(Animation animation) {
//
//                }
//
//                @Override
//                public void onAnimationEnd(Animation animation) {
//                    expandedView.setVisibility(View.GONE);
//                }
//
//                @Override
//                public void onAnimationRepeat(Animation animation) {
//
//                }
//            });
//        }
//        else {
//            animation = new Animation() {
//                @Override
//                protected void applyTransformation(float interpolatedTime, Transformation t) {
//                    expandedView.getLayoutParams().height = (int) (interpolatedTime * height);
//                    expandedView.requestLayout();
//                }
//
//                @Override
//                public boolean willChangeBounds() {
//                    return true;
//                }
//            };
//            animation.setAnimationListener(new Animation.AnimationListener() {
//                @Override
//                public void onAnimationStart(Animation animation) {
//
//                }
//
//                @Override
//                public void onAnimationEnd(Animation animation) {
//                    recyclerSources.smoothScrollToPosition(position);
//                }
//
//                @Override
//                public void onAnimationRepeat(Animation animation) {
//
//                }
//            });
//            expandedView.getLayoutParams().height = 0;
//            expandedView.requestLayout();
//            expandedView.setVisibility(View.VISIBLE);
//        }
//        animation.setDuration(EXPAND_ACTION_DURATION);
//        animation.setInterpolator(new DecelerateInterpolator());
//        expandedView.startAnimation(animation);
//        expandedView.requestLayout();

    }

    public boolean onItemLongClick(int position) {
        if (sourceListListener.getControllerSources().toggleActivated(position)) {
            if (alertText.isShown()) {
                sourceListListener.getControllerSources().recount();
            }
        }
        adapterSources.notifyItemChanged(position);

        return true;
    }

    private void showSourceEditFragment(final View view, final int position) {
        recyclerSources.setClickable(false);
        recyclerSources.setEnabled(false);
        sourceListListener.getControllerSources().saveData();

        Source dataItem = sourceListListener.getControllerSources().get(position);
        final SourceInfoFragment sourceInfoFragment = new SourceInfoFragment();
        sourceInfoFragment.setImageDrawable(((ImageView) view.findViewById(R.id.source_image)).getDrawable());
        Bundle arguments = new Bundle();
        arguments.putInt(Source.POSITION, position);
        arguments.putString(Source.TYPE, dataItem.getType());
        arguments.putString(Source.TITLE, dataItem.getTitle());
        arguments.putString(Source.DATA, dataItem.getData());
        arguments.putInt(Source.NUM, dataItem.getNum());
        arguments.putBoolean(Source.USE, dataItem.isUse());
        arguments.putString(Source.SORT, dataItem.getSort());
        arguments.putBoolean(Source.PREVIEW, dataItem.isPreview());
        if (dataItem.getImageFile() != null) {
            arguments.putString(Source.IMAGE_FILE, dataItem.getImageFile().getAbsolutePath());
        }
        else {
            arguments.putString(Source.IMAGE_FILE, "");
        }

        arguments.putBoolean(Source.USE_TIME, dataItem.isUseTime());
        arguments.putString(Source.TIME, dataItem.getTime());

        sourceInfoFragment.setArguments(arguments);

        final RelativeLayout sourceContainer = (RelativeLayout) view.findViewById(R.id.source_container);
        final CardView sourceCard = (CardView) view.findViewById(R.id.source_card);
        final View imageOverlay = view.findViewById(R.id.source_image_overlay);
        final EditText sourceTitle = (EditText) view.findViewById(R.id.source_title);
        final Toolbar toolbarActions = (Toolbar) view.findViewById(R.id.toolbar_actions);
        final LinearLayout sourceExpandContainer = (LinearLayout) view.findViewById(R.id.source_expand_container);

        final float cardStartShadow = sourceCard.getPaddingLeft();
        final float viewStartHeight = sourceContainer.getHeight();
        final float viewStartY = view.getY();
        final int viewStartPadding = view.getPaddingLeft();
        final float textStartX = sourceTitle.getX();
        final float textStartY = sourceTitle.getY();
        final float textTranslationY = sourceTitle.getHeight();

        Animation animation = new Animation() {

            private boolean needsFragment = true;

            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {

                if (needsFragment && interpolatedTime >= 1) {
                    needsFragment = false;
                    getFragmentManager().beginTransaction()
                            .add(R.id.content_frame,
                                    sourceInfoFragment,
                                    "sourceInfoFragment")
                            .addToBackStack(null)
                            .setTransition(FragmentTransaction.TRANSIT_NONE)
                            .commit();
                }
                int newPadding = Math.round(viewStartPadding * (1 - interpolatedTime));
                int newShadowPadding = (int) (cardStartShadow * (1.0f - interpolatedTime));
                sourceCard.setShadowPadding(newShadowPadding, 0, newShadowPadding, 0);
                ((LinearLayout.LayoutParams) sourceCard.getLayoutParams()).topMargin = newShadowPadding;
                ((LinearLayout.LayoutParams) sourceCard.getLayoutParams()).bottomMargin = newShadowPadding;
                ((LinearLayout.LayoutParams) sourceCard.getLayoutParams()).leftMargin = newShadowPadding;
                ((LinearLayout.LayoutParams) sourceCard.getLayoutParams()).rightMargin = newShadowPadding;
                view.setPadding(newPadding, 0, newPadding, 0);
                view.setY(viewStartY - interpolatedTime * viewStartY);
                ViewGroup.LayoutParams params = sourceContainer.getLayoutParams();
                params.height = (int) (viewStartHeight + (screenHeight - viewStartHeight) * interpolatedTime);
                sourceContainer.setLayoutParams(params);
                sourceTitle.setY(textStartY + interpolatedTime * textTranslationY);
                sourceTitle.setX(textStartX + viewStartPadding - newPadding);
                toolbarActions.setAlpha(1.0f - interpolatedTime);
                sourceExpandContainer.setAlpha(1.0f - interpolatedTime);
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
                    Parcelable state = layoutManager.onSaveInstanceState();
                    recyclerSources.setAdapter(null);
                    recyclerSources.setAdapter(adapterSources);
                    layoutManager.onRestoreInstanceState(state);
                    recyclerSources.setClickable(true);;
                    recyclerSources.setEnabled(true);
                    needsListReset = false;
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        ValueAnimator cardColorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(),
                AppSettings.getDialogColor(appContext),
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
                    Parcelable state = layoutManager.onSaveInstanceState();
                    recyclerSources.setAdapter(null);
                    recyclerSources.setAdapter(adapterSources);
                    layoutManager.onRestoreInstanceState(state);
                    recyclerSources.setClickable(true);
                    recyclerSources.setEnabled(true);
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

    private void setAlertText() {

        Log.i("SLA", "ImageCountTask onPostExecute");

        if (!isAdded()) {
            return;
        }

        String sourceState = sourceListListener.getControllerSources().getState();

        sourceListListener.getControllerSources().updateNum();

        resetAddButtonIcon();

        if (toolbarMenu != null) {
            toolbarMenu.findItem(R.id.item_download).setIcon(FileHandler.isDownloading() ?
                    R.drawable.ic_cancel_white_24dp :
                    R.drawable.ic_file_download_white_24dp);
            toolbarMenu.findItem(R.id.item_download).getIcon().setColorFilter(
                    AppSettings.getColorFilterInt(appContext),
                    PorterDuff.Mode.MULTIPLY);
        }

        alertText.setVisibility(sourceState.equals(ControllerSources.OKAY) ?
                View.GONE :
                View.VISIBLE);

        switch (sourceState) {

            case ControllerSources.NO_SOURCES:
                alertText.setText("Please add a source");
                Drawable addDrawable = getResources().getDrawable(R.drawable.floating_button_white);
                addDrawable.setColorFilter(getResources().getColor(R.color.ALERT_TEXT),
                        PorterDuff.Mode.MULTIPLY);
                addButtonBackground.setImageDrawable(addDrawable);
                break;
            case ControllerSources.NO_ACTIVE_SOURCES:
                alertText.setText("No active sources");
                break;
            case ControllerSources.NEED_DOWNLOAD:
                alertText.setText("No downloaded images");
                if (!FileHandler.isDownloading() && toolbarMenu != null) {
                    toolbarMenu.findItem(R.id.item_download).setIcon(
                            R.drawable.ic_file_download_white_24dp);
                    toolbarMenu.findItem(R.id.item_download).getIcon()
                            .setColorFilter(getResources().getColor(R.color.ALERT_TEXT),
                                    PorterDuff.Mode.MULTIPLY);
                }
                break;
            case ControllerSources.NO_IMAGES:
                alertText.setText("No images found");
                break;
            case ControllerSources.OKAY:
                break;

        }
    }

    public interface SourceListListener {
        ControllerSources getControllerSources();
    }

}