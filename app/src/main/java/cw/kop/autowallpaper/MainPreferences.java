package cw.kop.autowallpaper;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import cw.kop.autowallpaper.images.LocalImageFragment;
import cw.kop.autowallpaper.settings.AppSettings;
import cw.kop.autowallpaper.websites.SourceListFragment;

public class MainPreferences extends Activity {

	public static SharedPreferences prefs;

    private ActionBarDrawerToggle mDrawerToggle;
    private String[] fragmentList;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;

    private CharSequence mTitle;

    public SourceListFragment websiteFragment;

    public MainPreferences() {
    }

    @Override
    public void onConfigurationChanged (Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.i("MP", "onConfigurationChanged");
    }

    public void setAppTheme() {
        if (AppSettings.getTheme() == R.style.FragmentLightTheme) {
            setTheme(R.style.AppLightTheme);
        }
        else if (AppSettings.getTheme() == R.style.FragmentDarkTheme){
            setTheme(R.style.AppDarkTheme);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i("MP", "onCreate");

        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        AppSettings.initPrefs(prefs, getApplicationContext());

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_layout);

        setAppTheme();

        mTitle = getTitle();

        fragmentList = getResources().getStringArray(R.array.fragment_titles);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, Gravity.START);
        mDrawerLayout.setBackgroundColor(getResources().getColor(R.color.BLACK_OPAQUE));

        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mDrawerList.setAdapter(new NavListAdapter(this, fragmentList));
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
        mDrawerList.setBackgroundColor(getResources().getColor(R.color.BLACK_OPAQUE));

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        mDrawerToggle = new ActionBarDrawerToggle(
                this,
                mDrawerLayout,
                R.drawable.ic_drawer,
                R.string.drawer_open,
                R.string.drawer_close) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                getActionBar().setTitle(mTitle);
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                getActionBar().setTitle("Settings");
            }
        };

		Downloader.setNewTask(getApplicationContext());

        if (websiteFragment == null) {
            setTitle(fragmentList[0]);

            websiteFragment = new SourceListFragment();

            getFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, websiteFragment, "website_fragment")
                    .commit();

        }

        Bundle bundle = getIntent().getExtras();
        if (bundle != null && bundle.getBoolean("download")) {
            websiteFragment.getHtml();
            if (AppSettings.useToast()) {
                Toast.makeText(getApplicationContext(), "Downloading images", Toast.LENGTH_SHORT).show();
            }
            Log.i("MP", "Called getHtml()");
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
                        .replace(R.id.content_frame, new SourceListFragment(), "website_fragment")
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
                        .replace(R.id.content_frame, new ImageSettingsFragment())
                        .commit();
                break;
            case 4:
                getFragmentManager().beginTransaction()
                        .replace(R.id.content_frame, new AppSettingsFragment())
                        .commit();
                break;
            case 5:
                getFragmentManager().beginTransaction()
                        .replace(R.id.content_frame, new AboutFragment())
                        .commit();
                break;
            default:

        }

        getFragmentManager().executePendingTransactions();

        setTitle(fragmentList[position]);

        mDrawerLayout.closeDrawer(mDrawerList);
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getActionBar().setTitle(mTitle);

    }

//	@Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu items for use in the action bar
//        MenuInflater inflater = getMenuInflater();
//        inflater.inflate(R.menu.mainpreferences_activity_actions, menu);
//        return super.onCreateOptionsMenu(menu);
//    }

    @Override
	public boolean onOptionsItemSelected(MenuItem item) {

        Log.i("MP", "Item pressed: " + item.getItemId());

        if (getFragmentManager().findFragmentByTag("image_fragment") == null && mDrawerToggle.onOptionsItemSelected(item)) {
            if (item.getItemId() == android.R.id.home) {
                return super.onOptionsItemSelected(item);
            }
            return true;
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

        getActionBar().setDisplayHomeAsUpEnabled(false);

        View view = getWindow().getDecorView().findViewById(getResources().getIdentifier("action_bar_container", "id", "android"));
        if (view == null) {
            Log.i("MP", "Home null");
        }
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}


}
