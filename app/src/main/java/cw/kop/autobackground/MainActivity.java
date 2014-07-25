package cw.kop.autobackground;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import cw.kop.autobackground.images.LocalImageFragment;
import cw.kop.autobackground.notification.NotificationSettingsFragment;
import cw.kop.autobackground.settings.AppSettings;
import cw.kop.autobackground.sources.SourceListFragment;

public class MainActivity extends Activity {

	private static SharedPreferences prefs;

    private ActionBarDrawerToggle drawerToggle;
    private String[] fragmentList;
    private DrawerLayout drawerLayout;
    private ListView drawerList;
    private TextView actionBarTitle;

    private CharSequence mTitle;

    public SourceListFragment websiteFragment;

    public MainActivity() {
    }

    @Override
    public void onConfigurationChanged (Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.i("MP", "onConfigurationChanged");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i("MP", "onCreate");

        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        AppSettings.initPrefs(prefs, getApplicationContext());

        super.onCreate(savedInstanceState);

        int[] colors = {0, 0xFFFFFFFF, 0};

        if (AppSettings.getTheme() == R.style.AppLightTheme) {
            setTheme(R.style.AppLightTheme);
            colors = new int[] {0, 0xFF000000, 0};
        }
        else if (AppSettings.getTheme() == R.style.AppDarkTheme){
            setTheme(R.style.AppDarkTheme);
        }
        else if (AppSettings.getTheme() == R.style.AppTransparentTheme){
            setTheme(R.style.AppTransparentTheme);
        }

        setContentView(R.layout.activity_layout);

        mTitle = getTitle();

        fragmentList = getResources().getStringArray(R.array.fragment_titles);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, Gravity.START);

        drawerList = (ListView) findViewById(R.id.left_drawer);
        drawerList.setAdapter(new NavListAdapter(this, fragmentList));
        drawerList.setOnItemClickListener(new DrawerItemClickListener());

        if (AppSettings.getTheme() == R.style.AppLightTheme) {
            drawerList.setBackgroundColor(getResources().getColor(R.color.WHITE_OPAQUE));
        }
        else if (AppSettings.getTheme() == R.style.AppDarkTheme){
            drawerList.setBackgroundColor(getResources().getColor(R.color.BLACK_OPAQUE));
        }
        else if (AppSettings.getTheme() == R.style.AppTransparentTheme){
            drawerList.setBackgroundColor(getResources().getColor(R.color.TRANSPARENT_BACKGROUND));
        }

        drawerList.setDivider(new GradientDrawable(GradientDrawable.Orientation.RIGHT_LEFT, colors));
        drawerList.setDividerHeight(1);

        final ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        View actionBarView = getLayoutInflater().inflate(R.layout.action_bar_layout, null);
        actionBarTitle = (TextView) actionBarView.findViewById(R.id.action_bar_title);

        ImageView drawerIndicator = (ImageView) actionBarView.findViewById(R.id.drawer_indicator);
        drawerIndicator.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleDrawer();
            }
        });

        ImageView actionBarIcon = (ImageView) actionBarView.findViewById(R.id.action_bar_icon);
        actionBarIcon.setOnClickListener(new View.OnClickListener() {
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

        ImageView downloadButton = (ImageView) actionBarView.findViewById(R.id.download_wallpaper);

        ImageView cycleButton = (ImageView) actionBarView.findViewById(R.id.cycle_wallpaper);
        cycleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cycleWallpaper();
                if (AppSettings.useToast()) {
                    Toast.makeText(getApplicationContext(), "Cycling wallpaper...", Toast.LENGTH_SHORT).show();
                }
            }
        });

        if (AppSettings.getTheme() != R.style.AppLightTheme) {
            downloadButton.setImageResource(R.drawable.ic_action_download_dark);
            cycleButton.setImageResource(R.drawable.ic_action_refresh_dark);
            drawerIndicator.setImageResource(R.drawable.ic_drawer_dark);
        }

        actionBar.setCustomView(actionBarView);
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

        drawerToggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                R.drawable.ic_drawer_dark,
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

        if (websiteFragment == null) {
            websiteFragment = new SourceListFragment();
        }

        Bundle bundle = getIntent().getExtras();

        if (bundle != null && bundle.getInt("fragment", 0) > 0) {
            selectItem(bundle.getInt("fragment", 0));
        }
        else {
            selectItem(0);
        }

        if (bundle != null && bundle.getBoolean("download")) {
            websiteFragment.getHtml();
            if (AppSettings.useToast()) {
                Toast.makeText(getApplicationContext(), "Downloading images", Toast.LENGTH_SHORT).show();
            }
            Log.i("MP", "Called getHtml()");
        }

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    private void toggleDrawer() {
        LocalImageFragment localImageFragment = (LocalImageFragment) getFragmentManager().findFragmentByTag("image_fragment");
        if (localImageFragment != null) {
            onBackPressed();
            Log.i("MA", "onBackPressed()");
        }
        if (drawerLayout.isDrawerOpen(drawerList)) {
            drawerLayout.closeDrawer(drawerList);
        }
        else {
            drawerLayout.openDrawer(drawerList);
        }

    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    private void selectItem(int position) {

        switch (position) {

            case 0:
                getFragmentManager().beginTransaction()
                        .replace(R.id.content_frame, websiteFragment, "website_fragment")
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
                        .replace(R.id.content_frame, new EffectsSettingsFragment())
                        .commit();
                break;
            case 4:
                getFragmentManager().beginTransaction()
                        .replace(R.id.content_frame, new NotificationSettingsFragment())
                        .commit();
                break;
            case 5:
                getFragmentManager().beginTransaction()
                        .replace(R.id.content_frame, new AppSettingsFragment())
                        .commit();
                break;
            case 6:
                getFragmentManager().beginTransaction()
                        .replace(R.id.content_frame, new AboutFragment())
                        .commit();
                break;
            default:

        }

        getFragmentManager().executePendingTransactions();
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

    private void cycleWallpaper() {
        Intent intent = new Intent();
        intent.setAction(LiveWallpaperService.CYCLE_IMAGE);
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        sendBroadcast(intent);
    }

    @Override
	protected void onResume() {
		super.onResume();

        View view = getWindow().getDecorView().findViewById(getResources().getIdentifier("action_bar_container", "id", "android"));
        if (view == null) {
            Log.i("MP", "Home null");
        }
	}

}
