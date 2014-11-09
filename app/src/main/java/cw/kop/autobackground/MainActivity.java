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

package cw.kop.autobackground;

import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.squareup.picasso.Picasso;

import cw.kop.autobackground.files.FileHandler;
import cw.kop.autobackground.images.AlbumFragment;
import cw.kop.autobackground.images.ImageHistoryFragment;
import cw.kop.autobackground.images.LocalImageFragment;
import cw.kop.autobackground.notification.NotificationSettingsFragment;
import cw.kop.autobackground.settings.AppSettings;
import cw.kop.autobackground.sources.SourceInfoFragment;
import cw.kop.autobackground.sources.SourceListFragment;

public class MainActivity extends ActionBarActivity {

    public static final String LOAD_NAV_PICTURE = "cw.kop.autobackground.LOAD_NAV_PICTURE";
    private BroadcastReceiver entryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case SourceListFragment.ADD_ENTRY:
                    sourceListFragment.addEntry(
                            intent.getStringExtra("type"),
                            intent.getStringExtra("title"),
                            intent.getStringExtra("data"),
                            "" + intent.getIntExtra("num", 0));
                    break;
                case SourceListFragment.SET_ENTRY:
                    sourceListFragment.setEntry(
                            intent.getIntExtra("position", -1),
                            intent.getStringExtra("type"),
                            intent.getStringExtra("title"),
                            intent.getStringExtra("data"),
                            "" + intent.getIntExtra("num", 0),
                            intent.getBooleanExtra("use", true));
                    break;
                case LOAD_NAV_PICTURE:
                    loadNavPicture();
                    break;
            }

        }
    };
    private SourceListFragment sourceListFragment;
    private ActionBarDrawerToggle drawerToggle;
    private String[] fragmentList;
    private DrawerLayout drawerLayout;
    private LinearLayout navLayout;
    private ImageView navPicture;
    private ListView drawerList;
    private IntentFilter entryFilter;
    private int currentPosition = -1;
    private int newPosition = -1;

    public MainActivity() {
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.i("MP", "onConfigurationChanged");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i("MP", "onCreate");

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                getApplicationContext());

        AppSettings.initPrefs(prefs, getApplicationContext());

        switch (AppSettings.getTheme()) {

            case AppSettings.APP_LIGHT_THEME:
                setTheme(R.style.AppLightTheme);
                break;
            case AppSettings.APP_DARK_THEME:
                setTheme(R.style.AppDarkTheme);
                break;
            case AppSettings.APP_TRANSPARENT_THEME:
                setTheme(R.style.AppTransparentTheme);
                break;
        }

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_layout);

        fragmentList = getResources().getStringArray(R.array.fragment_titles);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, Gravity.START);

        navLayout = (LinearLayout) findViewById(R.id.navigation_drawer);
        navPicture = (ImageView) findViewById(R.id.nav_drawer_picture);

        Configuration configuration = getResources().getConfiguration();
        navLayout.getLayoutParams().width = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                configuration.screenWidthDp - 56,
                getResources().getDisplayMetrics()));

        CustomRelativeLayout navHeader = (CustomRelativeLayout) findViewById(R.id.nav_drawer_header);
        navHeader.getLayoutParams().height = Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                Math.min(320, (configuration.screenWidthDp - 56) / 16 * 9),
                getResources().getDisplayMetrics()));
        navHeader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LiveWallpaperService.OPEN_IMAGE);
                sendBroadcast(intent);
            }
        });

        drawerList = (ListView) findViewById(R.id.nav_list);
        drawerList.setAdapter(new NavListAdapter(this, fragmentList));
        drawerList.setOnItemClickListener(new DrawerItemClickListener());

        drawerList.setDividerHeight(0);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitleTextAppearance(getApplicationContext(), R.style.ToolbarTitle);

        try {
            setSupportActionBar(toolbar);
        }
        catch (Throwable t) {

        }

        if (AppSettings.getTheme().equals(AppSettings.APP_LIGHT_THEME)) {
            navLayout.setBackgroundColor(getResources().getColor(R.color.LIGHT_THEME_BACKGROUND));
            toolbar.setTitleTextColor(getResources().getColor(R.color.DARK_GRAY_OPAQUE));
            toolbar.setBackgroundColor(getResources().getColor(R.color.BLUE_OPAQUE));
        }
        else if (AppSettings.getTheme().equals(AppSettings.APP_DARK_THEME)) {
            navLayout.setBackgroundColor(getResources().getColor(R.color.DARK_THEME_BACKGROUND));
            toolbar.setTitleTextColor(getResources().getColor(R.color.LIGHT_GRAY_OPAQUE));
            toolbar.setBackgroundColor(getResources().getColor(R.color.DARK_BLUE_OPAQUE));
        }
        else if (AppSettings.getTheme().equals(AppSettings.APP_TRANSPARENT_THEME)) {
            navLayout.setBackgroundColor(getResources().getColor(R.color.TRANSPARENT_BACKGROUND));
            toolbar.setTitleTextColor(getResources().getColor(R.color.LIGHT_GRAY_OPAQUE));
            toolbar.setBackgroundColor(getResources().getColor(R.color.DARK_BLUE_OPAQUE));
        }

        drawerToggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.drawer_open,
                R.string.drawer_close) {

            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                selectItem(newPosition, true);
            }

            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                getFragmentManager().popBackStack();
            }
        };

        drawerLayout.setDrawerListener(drawerToggle);

        if (sourceListFragment == null) {
            sourceListFragment = new SourceListFragment();
        }

        Bundle bundle = getIntent().getExtras();

        String action = getIntent().getAction();

        if (bundle != null && bundle.getInt("fragment", 0) > 0) {
            int position = bundle.getInt("fragment", 0);
            selectItem(position, false);
        }
        else if (action != null && action.equals(Intent.ACTION_MANAGE_NETWORK_USAGE)) {
            selectItem(2, false);
        }
        else {
            selectItem(0, false);
        }

        loadNavPicture();

        entryFilter = new IntentFilter();
        entryFilter.addAction(SourceListFragment.ADD_ENTRY);
        entryFilter.addAction(SourceListFragment.SET_ENTRY);
        entryFilter.addAction(MainActivity.LOAD_NAV_PICTURE);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    protected void onStart() {
        super.onStart();
    }

    protected void onStop() {
        super.onStop();
    }

    private void loadNavPicture() {

        if (Build.VERSION.SDK_INT >= 16 && navPicture != null && FileHandler.getCurrentBitmapFile() != null && FileHandler.getCurrentBitmapFile().exists()) {
            Picasso.with(getApplicationContext()).load(FileHandler.getCurrentBitmapFile()).fit().centerCrop().into(
                    navPicture);
        }
    }

    public void toggleDrawer() {
        LocalImageFragment localImageFragment = (LocalImageFragment) getFragmentManager().findFragmentByTag(
                "image_fragment");
        AlbumFragment albumFragment = (AlbumFragment) getFragmentManager().findFragmentByTag(
                "album_fragment");
        if (localImageFragment != null || albumFragment != null) {
            onBackPressed();
        }

        if (drawerLayout.isDrawerOpen(navLayout)) {
            drawerLayout.closeDrawer(navLayout);
        }
        else {
            drawerLayout.openDrawer(navLayout);
        }

    }

    private void selectItem(int position, boolean slideAnimate) {

        if (position == currentPosition || position < 0) {
            return;
        }

        currentPosition = position;

        setTitle(fragmentList[position]);
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();

        if (slideAnimate) {
            fragmentTransaction.setCustomAnimations(R.animator.slide_from_left,
                    R.animator.slide_to_right);
        }
        else {
            fragmentTransaction.setCustomAnimations(android.R.animator.fade_in,
                    android.R.animator.fade_out);
        }

        switch (position) {

            case 0:
                fragmentTransaction.replace(R.id.content_frame,
                        sourceListFragment,
                        "source_fragment").commit();
                break;
            case 1:
                fragmentTransaction.replace(R.id.content_frame,
                        new WallpaperSettingsFragment()).commit();
                break;
            case 2:
                fragmentTransaction.replace(R.id.content_frame,
                        new DownloadSettingsFragment()).commit();
                break;
            case 3:
                fragmentTransaction.replace(R.id.content_frame,
                        new AccountSettingsFragment()).commit();
                break;
            case 4:
                fragmentTransaction.replace(R.id.content_frame,
                        new EffectsSettingsFragment()).commit();
                break;
            case 5:
                fragmentTransaction.replace(R.id.content_frame,
                        new NotificationSettingsFragment()).commit();
                break;
            case 6:
                fragmentTransaction.replace(R.id.content_frame, new AppSettingsFragment()).commit();
                break;
            case 7:
                fragmentTransaction.replace(R.id.content_frame,
                        new ImageHistoryFragment()).commit();
                break;
            case 8:
                fragmentTransaction.replace(R.id.content_frame, new AboutFragment()).commit();
                break;
            default:

        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        Log.i("MP", "Item pressed: " + item.getItemId());

        if (getFragmentManager().findFragmentByTag("image_fragment") == null && drawerToggle.onOptionsItemSelected(
                item)) {
            return item.getItemId() != android.R.id.home || super.onOptionsItemSelected(item);
        }
        else if (getFragmentManager().findFragmentByTag("image_fragment") != null) {
            getFragmentManager().popBackStack();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {

        if (getFragmentManager().findFragmentByTag("image_fragment") != null) {
            Log.i("MP", "Back directory");
            if (((LocalImageFragment) getFragmentManager().findFragmentByTag("image_fragment")).onBackPressed()) {
                getFragmentManager().popBackStack();
            }
        }
        else if (getFragmentManager().findFragmentByTag("source_info_fragment") != null) {
            ((SourceInfoFragment) getFragmentManager().findFragmentByTag("source_info_fragment")).onBackPressed();
        }
        else if (getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStack();
        }
        else {
            super.onBackPressed();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNavPicture();
        LocalBroadcastManager.getInstance(this).registerReceiver(entryReceiver, entryFilter);
    }

    @Override
    protected void onPause() {

        LocalBroadcastManager.getInstance(this).unregisterReceiver(entryReceiver);

        super.onPause();
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            newPosition = position;
            drawerList.setItemChecked(position, true);
            drawerLayout.closeDrawer(navLayout);
        }
    }
}
