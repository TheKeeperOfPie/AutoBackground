package cw.kop.autobackground;

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

import cw.kop.autobackground.images.LocalImageFragment;
import cw.kop.autobackground.settings.AppSettings;
import cw.kop.autobackground.websites.SourceListFragment;

public class MainPreferences extends Activity {

	public static SharedPreferences prefs;

    private ActionBarDrawerToggle drawerToggle;
    private String[] fragmentList;
    private DrawerLayout drawerLayout;
    private ListView drawerList;

    private CharSequence mTitle;

    public SourceListFragment websiteFragment;

    public MainPreferences() {
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


        if (AppSettings.getTheme() == R.style.AppLightTheme) {
            setTheme(R.style.AppLightTheme);
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

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        drawerToggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                R.drawable.ic_drawer_dark,
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
                Log.i("MP", "Title: Settings");
                getActionBar().setTitle("Settings");
            }
        };

        drawerLayout.setDrawerListener(drawerToggle);

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

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
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
                        .replace(R.id.content_frame, new EffectsSettingsFragment())
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
        drawerList.setItemChecked(position, true);
        setTitle(fragmentList[position]);
        drawerLayout.closeDrawer(drawerList);
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

        if (getFragmentManager().findFragmentByTag("image_fragment") == null && drawerToggle.onOptionsItemSelected(item)) {
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
