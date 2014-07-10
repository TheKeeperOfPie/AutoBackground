package cw.kop.autowallpaper;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ActionViewTarget;
import com.github.amlcurran.showcaseview.targets.ViewTarget;

import java.util.List;

import cw.kop.autowallpaper.images.LocalImageFragment;
import cw.kop.autowallpaper.settings.AppSettings;
import cw.kop.autowallpaper.websites.WebsiteListFragment;

public class MainPreferences extends PreferenceActivity{

	public static SharedPreferences prefs;
	private Button setButton;
    private WebView webView;
    private boolean isDownloading = false;
    private String baseUrl;

    private ShowcaseView tutorialPromptView;

	protected boolean isValidFragment(final String fragmentName) {
		Log.i("MP", "isValidFragment Called for " + fragmentName);

		return WallpaperSettingsFragment.class.getName().equals(fragmentName)
            || WebsiteListFragment.class.getName().equals(fragmentName)
            || DownloadSettingsFragment.class.getName().equals(fragmentName)
            || ImageSettingsFragment.class.getName().equals(fragmentName)
            || LocalImageFragment.class.getName().equals(fragmentName)
            || AppSettingsFragment.class.getName().equals(fragmentName)
            || AboutFragment.class.getName().equals(fragmentName);
	}
	
	@Override
	public boolean onIsMultiPane() {
		if (AppSettings.forceMultipane()) {
			return true;
		}
		
        return super.onIsMultiPane();
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

        setTheme(AppSettings.getTheme());

//        String ua = "Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/37.0.2049.0 Safari/537.36";
//        webView = new WebView(this);
//        webView.getSettings().setUserAgentString(ua);

		Downloader.setNewTask(getApplicationContext());
		
        setButton = new Button(this) ;
        setButton.setText("Set Wallpaper");
        setButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				setWallpaper();
			}

        });

        Button downloadButton = new Button(this);
        downloadButton.setText("Download Wallpaper");
        downloadButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
//                if (!isDownloading) {
//                    Downloader.download(getApplicationContext());
//                } else {
//                    Log.i("MP", "isDownloading");
//                }
                FragmentManager fragmentManager = getFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                WebsiteListFragment fragment = new WebsiteListFragment();
                fragmentTransaction.add(fragment, "test");
                fragmentTransaction.commit();
            }

        });

        Button refreshButton = new Button(this);
        refreshButton.setText("Cycle Wallpaper");
        refreshButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                cycleWallpaper();
                if (AppSettings.useToast()) {
                    Toast.makeText(getApplicationContext(), "Cycling...", Toast.LENGTH_SHORT).show();
                }
            }

        });

        LinearLayout footerLayout = new LinearLayout(this);
        footerLayout.setOrientation(LinearLayout.VERTICAL);
        footerLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        footerLayout.setGravity(Gravity.CENTER);
        
        footerLayout.addView(setButton);
        footerLayout.addView(refreshButton);
        footerLayout.addView(downloadButton);
//        if (AppSettings.useExperimentalDownloader()) {
//            footerLayout.addView(webView);
//            ViewGroup.LayoutParams params = webView.getLayoutParams();
//            params.height = 1;
//            webView.setLayoutParams(params);
//        }

        setListFooter(footerLayout);

//        Bundle bundle = getIntent().getExtras();
//        if (bundle != null && bundle.getBoolean("download")) {
//            getHtml();
//            Log.i("MP", "Called getHtml()");
//        }

        OnClickListener tutorialPromptListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                tutorialPromptView.hide();
                AppSettings.setTutorial(true);
                showTutorial();
            }
        };

        tutorialPromptView = new ShowcaseView.Builder(this)
                .setContentTitle("Welcome to AutoBackground")
                .setContentText("If you would like to go through\n" +
                        "a quick tutorial, hit the OK button.")
                .setStyle(R.style.ShowcaseStyle)
                .setOnClickListener(tutorialPromptListener)
                .build();

    }

    private void showTutorial() {
        ShowcaseView websiteTutorialView = new ShowcaseView.Builder(this)
                .setTarget(new ViewTarget(getListView().getChildAt(1)))
                .setContentTitle("Website Settings")
                .setContentText("Click here to add\n" +
                        "your first website.")
                .setStyle(R.style.ShowcaseStyle)
                .build();

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
    	
		switch (item.getItemId()) 
	    {
	        case android.R.id.home:
	            onBackPressed();
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
		
	}

	@Override
	public void onBuildHeaders(final List<Header> target) {
		loadHeadersFromResource(R.xml.preference_headers, target);
	}
    
	@Override
	protected void onResume() {
		super.onResume();

        getActionBar().setDisplayHomeAsUpEnabled(true);

		if (isServiceRunning(LiveWallpaperService.class.getName())) {
        	setButton.setVisibility(View.GONE);
        }
		else {
			setButton.setVisibility(View.VISIBLE);
		}
		if (onIsMultiPane()) {
			(findViewById(android.R.id.title)).setVisibility(View.GONE);
		}

//        if (android.os.Build.VERSION.SDK_INT < 20) {
//            setBackgroundColorForViewTree((ViewGroup) getWindow().getDecorView(), Color.TRANSPARENT);
//        }
	}
	
//	private static void setBackgroundColorForViewTree(ViewGroup viewGroup, int color)
//	{
//		for (int i = 0; i < viewGroup.getChildCount(); i++)
//		{
//			View child = viewGroup.getChildAt(i);
//			if (child instanceof ViewGroup)
//				setBackgroundColorForViewTree((ViewGroup)child, color);
//			child.setBackgroundColor(color);
//		}
//		viewGroup.setBackgroundColor(color);
//
//	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
	
	protected void setWallpaper() {
		
		final Intent i = new Intent();
		i.setAction(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
		final String p = LiveWallpaperService.class.getPackage().getName();
		final String c = LiveWallpaperService.class.getCanonicalName();
		i.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, new ComponentName(p, c));
		
		startActivityForResult(i, 0);
	}
    
    protected void cycleWallpaper() {
    	
    	Intent intent = new Intent();
		intent.setAction(LiveWallpaperService.CYCLE_WALLPAPER);
		intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);

		sendBroadcast(intent);
	}
    
    private boolean isServiceRunning(final String className) {
		final ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (final RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (className.equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

    public void getHtml() {

        isDownloading = true;

        final Handler handler = new Handler();

        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                Log.i("MP", "Test1");

                for (int i = 0; i < AppSettings.getNumWebsites(); i++) {

                    Log.i("MP", "Test2");

                    final int index = i;

                    if (AppSettings.useWebsite(i)) {

                        handler.post(new Runnable() {

                            @Override
                            public void run() {
                                Log.i("MP", "Test3");

                                String url = AppSettings.getWebsiteUrl(index);

                                if (url.contains(".com")) {
                                    baseUrl = url.substring(0, url.indexOf(".com") + 4);
                                }
                                else if (url.contains(".net")) {
                                    baseUrl = url.substring(0, url.indexOf(".net") + 4);
                                }
                                else {
                                    baseUrl = url;
                                }

                                class MyJavaScriptInterface {

                                    @JavascriptInterface
                                    public void showHTML(String html) {
                                        Downloader.setHtml(html, getCacheDir().getAbsolutePath(), index, getBaseUrl());
                                        Log.i("MP", "Written html");
                                    }
                                }


                                webView.addJavascriptInterface(new MyJavaScriptInterface(), "HTMLOUT");


                                webView.setWebViewClient(new WebViewClient() {

                                    @Override
                                    public void onPageFinished(WebView view, String url) {

                                        webView.postDelayed(new Runnable() {

                                            @Override
                                            public void run() {
                                                webView.loadUrl("javascript:HTMLOUT.showHTML(document.documentElement.outerHTML);");
                                            }
                                        }, 15000);
                                    }
                                });
                                if (AppSettings.useToast()) {
                                    Toast.makeText(getApplicationContext(), "Loading page", Toast.LENGTH_SHORT).show();
                                }

                                webView.getSettings().setJavaScriptEnabled(true);

                                webView.loadUrl(url);

                                Log.i("MP", "WebView finished");
                            }
                        });
                    }
                }
                Downloader.resetIndex();
                isDownloading = false;
            }
        });
        thread.start();
    }

    private String getBaseUrl() {
        return baseUrl;
    }

}
