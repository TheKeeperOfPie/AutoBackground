package cw.kop.autowallpaper;

import java.util.List;

import cw.kop.autowallpaper.images.LocalImageFragment;
import cw.kop.autowallpaper.settings.AppSettings;
import cw.kop.autowallpaper.websites.WebsiteListFragment;
import android.app.ActionBar;
import android.app.ActivityManager;
import android.app.FragmentBreadCrumbs;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

public class MainPreferences extends PreferenceActivity{

	public static SharedPreferences prefs;
	private Button setButton;
	private Button downloadButton;
	private Button refreshButton;
	private LinearLayout footerLayout;
    private WebView webView;
	
	protected boolean isValidFragment(final String fragmentName) {
		Log.i("MP", "isValidFragment Called for " + fragmentName);

		return AboutFragment.class.getName().equals(fragmentName)
				|| WallpaperSettingsFragment.class.getName().equals(fragmentName)
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
    protected void onCreate(Bundle savedInstanceState) {

        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		AppSettings.initPrefs(prefs, getApplicationContext());
		
        if (AppSettings.useTransparentTheme()) {
        	setTheme(R.style.AppTheme);
        }
        
        super.onCreate(savedInstanceState);

        String ua = "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.0.4) Gecko/20100101 Firefox/4.0";
        webView = new WebView(this);
        webView.getSettings().setUserAgentString(ua);

		Downloader.setNewTask(getApplicationContext());

        getActionBar().setDisplayHomeAsUpEnabled(true);
		
        setButton = new Button(this) ;
        setButton.setText("Set Wallpaper");
        setButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				setWallpaper();
			}
        	
        });
        
        downloadButton = new Button(this);
        downloadButton.setText("Download Wallpaper");
        downloadButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Downloader.download(getApplicationContext());
                //getHtml(AppSettings.getWebsiteUrl(0));
			}
        	
        });
        
        refreshButton = new Button(this);
        refreshButton.setText("Cycle Wallpaper");
        refreshButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
                cycleWallpaper();
                Toast.makeText(getApplicationContext(), "Cycling...", Toast.LENGTH_SHORT).show();
			}
        	
        });
        
        footerLayout = new LinearLayout(this);
        footerLayout.setOrientation(LinearLayout.VERTICAL);
        footerLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        footerLayout.setGravity(Gravity.CENTER);
        
        footerLayout.addView(setButton);
        footerLayout.addView(refreshButton);
        footerLayout.addView(downloadButton);
        footerLayout.addView(webView);

        ViewGroup.LayoutParams params = webView.getLayoutParams();
        params.height = 1;
        webView.setLayoutParams(params);

        setListFooter(footerLayout);
        
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
		if (isServiceRunning(LiveWallpaperService.class.getName())) {
        	setButton.setVisibility(View.GONE);
        }
		else {
			setButton.setVisibility(View.VISIBLE);
		}
		if (onIsMultiPane()) {
			(findViewById(android.R.id.title)).setVisibility(View.GONE);
		}

        if (android.os.Build.VERSION.SDK_INT < 20) {
            setBackgroundColorForViewTree((ViewGroup) getWindow().getDecorView(), Color.TRANSPARENT);
        }
	}
	
	private static void setBackgroundColorForViewTree(ViewGroup viewGroup, int color)
	{
		for (int i = 0; i < viewGroup.getChildCount(); i++)
		{
			View child = viewGroup.getChildAt(i);
			if (child instanceof ViewGroup) 
				setBackgroundColorForViewTree((ViewGroup)child, color);
			child.setBackgroundColor(color);
		}
		viewGroup.setBackgroundColor(color);

	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
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

    public void getHtml(final String url) {

        webView.post(new Runnable() {

            boolean test = true;

            @Override
            public void run() {

                final String[] generatedHtml = new String[1];
            /* An instance of this class will be registered as a JavaScript interface */
                class MyJavaScriptInterface
                {
                    private Context ctx;
                    public String html;

                    MyJavaScriptInterface(Context ctx) {
                        this.ctx = ctx;
                    }

                    @JavascriptInterface
                    public void processHTML(final String html) {
                        webView.post(new Runnable() {
                            @Override
                            public void run() {
                                webView.loadDataWithBaseURL(url, html, "text/html", "UTF-8", url);
                            }
                        });
                    }

                    @JavascriptInterface
                    public void showHTML(String html) {
                        Downloader.setHtml(html, getCacheDir().getAbsolutePath());
                        Log.i("MP", "Written html");
                    }
                }

                webView.getSettings().setJavaScriptEnabled(true);


                webView.addJavascriptInterface(new MyJavaScriptInterface(getApplicationContext()), "HTMLOUT");


                webView.setWebViewClient(new WebViewClient() {

                    boolean loadingFinished = true;
                    boolean redirect = false;

                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String urlNewString) {

                        if (!loadingFinished) {
                            redirect = true;
                        }

                        loadingFinished = false;
                        view.loadUrl(urlNewString);
                        return true;
                    }

                    @Override
                    public void onPageStarted(WebView view, String url, Bitmap facIcon) {
                        loadingFinished = false;
                    }

                    @Override
                    public void onPageFinished(WebView view, String url) {
                        if(!redirect){
                            loadingFinished = true;
                        }

                        if(loadingFinished && !redirect){

                            if (test) {
                                webView.loadUrl("javascript:HTMLOUT.processHTML(document.documentElement.outerHTML);");
                                test = false;
                            }
                            else {
                                webView.loadUrl("javascript:HTMLOUT.showHTML(document.documentElement.outerHTML);");
                            }

                        } else{
                            redirect = false;
                        }

                    }
                });

        /* load a web page */

                webView.getSettings().setUserAgentString("Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.0.4) Gecko/20100101 Firefox/4.0");

                Toast.makeText(getApplicationContext(), "Loading page", Toast.LENGTH_SHORT).show();

                webView.loadUrl(url);

            }
        });

    }
    
}
