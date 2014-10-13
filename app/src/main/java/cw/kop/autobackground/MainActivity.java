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

import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import cw.kop.autobackground.images.AlbumFragment;
import cw.kop.autobackground.images.ImageHistoryFragment;
import cw.kop.autobackground.images.LocalImageFragment;
import cw.kop.autobackground.notification.NotificationSettingsFragment;
import cw.kop.autobackground.settings.AppSettings;
import cw.kop.autobackground.sources.SourceListFragment;

public class MainActivity extends Activity {

    public SourceListFragment sourceListFragment;
    private ActionBarDrawerToggle drawerToggle;
    private String[] fragmentList;
    private DrawerLayout drawerLayout;
    private ListView drawerList;
    private TextView actionBarTitle;
    private CharSequence mTitle;
    private BroadcastReceiver entryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            if (action.equals(SourceListFragment.ADD_ENTRY)) {
                sourceListFragment.addEntry(
                        intent.getStringExtra("type"),
                        intent.getStringExtra("title"),
                        intent.getStringExtra("data"),
                        intent.getStringExtra("num"));
            }
            if (action.equals(SourceListFragment.SET_ENTRY)) {
                sourceListFragment.setEntry(
                        intent.getIntExtra("position", -1),
                        intent.getStringExtra("type"),
                        intent.getStringExtra("title"),
                        intent.getStringExtra("data"),
                        intent.getStringExtra("num"));
            }

        }
    };

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

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        AppSettings.initPrefs(prefs, getApplicationContext());

        super.onCreate(savedInstanceState);

        int[] colors = {0, 0xFFFFFFFF, 0};

        if (AppSettings.getTheme().equals(AppSettings.APP_LIGHT_THEME)) {
            setTheme(R.style.AppLightTheme);
            colors = new int[] {0, 0xFF000000, 0};
        }
        else if (AppSettings.getTheme().equals(AppSettings.APP_DARK_THEME)) {
            setTheme(R.style.AppDarkTheme);
        }
        else if (AppSettings.getTheme().equals(AppSettings.APP_TRANSPARENT_THEME)) {
            setTheme(R.style.AppTransparentTheme);
        }

        if (AppSettings.useRightDrawer()) {
            setContentView(R.layout.activity_right_layout);
        }
        else {
            setContentView(R.layout.activity_layout);
        }

        mTitle = getTitle();

        fragmentList = getResources().getStringArray(R.array.fragment_titles);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, Gravity.START);

        drawerList = (ListView) findViewById(R.id.navigation_drawer);
        drawerList.setAdapter(new NavListAdapter(this, fragmentList));
        drawerList.setOnItemClickListener(new DrawerItemClickListener());

        if (AppSettings.getTheme().equals(AppSettings.APP_LIGHT_THEME)) {
            drawerList.setBackgroundColor(getResources().getColor(R.color.WHITE_OPAQUE));
        }
        else if (AppSettings.getTheme().equals(AppSettings.APP_DARK_THEME)) {
            drawerList.setBackgroundColor(getResources().getColor(R.color.BLACK_OPAQUE));
        }
        else if (AppSettings.getTheme().equals(AppSettings.APP_TRANSPARENT_THEME)) {
            drawerList.setBackgroundColor(getResources().getColor(R.color.TRANSPARENT_BACKGROUND));
        }

        drawerList.setDivider(new GradientDrawable(GradientDrawable.Orientation.RIGHT_LEFT, colors));
        drawerList.setDividerHeight(1);

        final ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        int drawerLayoutId = AppSettings.useRightDrawer() ? R.layout.action_bar_right_layout : R.layout.action_bar_layout;

        View actionBarView = getLayoutInflater().inflate(drawerLayoutId, null);
        actionBarTitle = (TextView) actionBarView.findViewById(R.id.action_bar_title);

        ImageView drawerIndicator = (ImageView) actionBarView.findViewById(R.id.drawer_indicator);
        drawerIndicator.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleDrawer();
            }
        });

        TextView actionBarTitle = (TextView) actionBarView.findViewById(R.id.action_bar_title);
        actionBarTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleDrawer();
            }
        });

        if (!AppSettings.getTheme().equals(AppSettings.APP_LIGHT_THEME)) {
            drawerIndicator.setImageResource(R.drawable.drawer_menu_dark);
        }

        actionBar.setCustomView(actionBarView);
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

        drawerToggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                R.drawable.drawer_menu,
                R.string.drawer_open,
                R.string.drawer_close) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                setTitle(mTitle);
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                Log.i("MP", "Title: Settings");
                setTitle("Settings");
            }
        };

        drawerLayout.setDrawerListener(drawerToggle);

        if (sourceListFragment == null) {
            sourceListFragment = new SourceListFragment();
        }

        Bundle bundle = getIntent().getExtras();

        if (bundle != null && bundle.getInt("fragment", 0) > 0) {
            selectItem(bundle.getInt("fragment", 0));
        }
        else {
            selectItem(0);
        }

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

    private void toggleDrawer() {
        LocalImageFragment localImageFragment = (LocalImageFragment) getFragmentManager().findFragmentByTag("image_fragment");
        AlbumFragment albumFragment = (AlbumFragment) getFragmentManager().findFragmentByTag("album_fragment");
        if (localImageFragment != null || albumFragment != null) {
            onBackPressed();
        }

        if (drawerLayout.isDrawerOpen(drawerList)) {
            drawerLayout.closeDrawer(drawerList);
        }
        else {
            drawerLayout.openDrawer(drawerList);
        }

    }

    private void selectItem(int position) {

        switch (position) {

            case 0:
                getFragmentManager().beginTransaction()
                        .replace(R.id.content_frame, sourceListFragment, "source_fragment")
                        .commit();
                break;
            case 1:
                getFragmentManager().beginTransaction()
                        .replace(R.id.content_frame, new WallpaperSettingsFragment())
                        .commit();
                break;
            case 2:
                getFragmentManager().beginTransaction()
                        .replace(R.id.content_frame, new DownloadSettingsFragment())
                        .commit();
                break;
            case 3:
                getFragmentManager().beginTransaction()
                        .replace(R.id.content_frame, new AccountSettingsFragment())
                        .commit();
                break;
            case 4:
                getFragmentManager().beginTransaction()
                        .replace(R.id.content_frame, new EffectsSettingsFragment())
                        .commit();
                break;
            case 5:
                getFragmentManager().beginTransaction()
                        .replace(R.id.content_frame, new NotificationSettingsFragment())
                        .commit();
                break;
            case 6:
                getFragmentManager().beginTransaction()
                        .replace(R.id.content_frame, new AppSettingsFragment())
                        .commit();
                break;
            case 7:
                getFragmentManager().beginTransaction()
                        .replace(R.id.content_frame, new ImageHistoryFragment())
                        .commit();
                break;
            case 8:
                getFragmentManager().beginTransaction()
                        .replace(R.id.content_frame, new AboutFragment())
                        .commit();
                break;
            default:

        }

        drawerList.setItemChecked(position, true);
        setTitle(fragmentList[position]);
        drawerLayout.closeDrawer(drawerList);
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        actionBarTitle.setText(title);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        Log.i("MP", "Item pressed: " + item.getItemId());

        if (getFragmentManager().findFragmentByTag("image_fragment") == null && drawerToggle.onOptionsItemSelected(item)) {
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
                super.onBackPressed();
            }
        }
        else {
            super.onBackPressed();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter entryFilter = new IntentFilter();
        entryFilter.addAction(SourceListFragment.ADD_ENTRY);
        entryFilter.addAction(SourceListFragment.SET_ENTRY);

        LocalBroadcastManager.getInstance(this).registerReceiver(entryReceiver, entryFilter);

        View view = getWindow().getDecorView().findViewById(getResources().getIdentifier("action_bar_container", "id", "android"));
        if (view == null) {
            Log.i("MP", "Home null");
        }
    }

    @Override
    protected void onPause() {

        LocalBroadcastManager.getInstance(this).unregisterReceiver(entryReceiver);

        super.onPause();
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }
}
