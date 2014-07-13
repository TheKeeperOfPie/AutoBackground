package cw.kop.autowallpaper.websites;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ListFragment;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.amlcurran.showcaseview.OnShowcaseEventListener;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ActionItemTarget;
import com.github.amlcurran.showcaseview.targets.ActionViewTarget;
import com.github.amlcurran.showcaseview.targets.ViewTarget;

import java.util.HashMap;

import cw.kop.autowallpaper.Downloader;
import cw.kop.autowallpaper.LiveWallpaperService;
import cw.kop.autowallpaper.R;
import cw.kop.autowallpaper.images.LocalImageFragment;
import cw.kop.autowallpaper.settings.AppSettings;

public class SourceListFragment extends ListFragment {

    public static final String WEBSITE = "website";
    public static final String FOLDER = "folder";

	private SourceListAdapter listAdapter;
    private Context context;
    private Button setButton;
    private Button downloadButton;
    private WebView webView;
    private boolean isDownloading = false;
    private String baseUrl;

    private ShowcaseView tutorialPromptView;
    private ShowcaseView websiteListTutorial;
    private ShowcaseView addWebsiteTutorial;
    private ShowcaseView downloadTutorial;
    private ShowcaseView setTutorial;
    private ShowcaseView settingsTutorial;
    private boolean setShown = false;

	public SourceListFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setHasOptionsMenu(true);
		
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		
		inflater.inflate(R.menu.website_actions, menu);
		
		super.onCreateOptionsMenu(menu, inflater);
	}

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = getActivity();
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
            case android.R.id.home:
                hide(settingsTutorial);
                showTutorial(6);
                return true;
			case R.id.add_website:
				showSourceMenu();
				return true;
			default:
			    return super.onOptionsItemSelected(item);
		}
	}

    private void showSourceMenu() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());

        dialog.setItems(R.array.source_menu, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        showDialogForInput();
                        break;
                    case 1:
                        getFragmentManager().beginTransaction()
                                .add(R.id.content_frame, new LocalImageFragment(), "image_fragment")
                                .addToBackStack(null)
                                .commit();
                        break;
                    default:
                }

            }
        });

        dialog.show();
    }

    public void addFolder(String title, String path, int num) {
        listAdapter.addItem(FOLDER, title, path, true, "" + num);
    }

	private void showDialogForInput() {

		AlertDialog.Builder dialog = new AlertDialog.Builder(context);

        View dialogView = View.inflate(context, R.layout.add_website_dialog, null);
		
		dialog.setView(dialogView);

		final EditText websiteTitle = (EditText) dialogView.findViewById(R.id.website_title);
		final EditText websiteUrl = (EditText) dialogView.findViewById(R.id.website_url);
		final EditText numImages = (EditText) dialogView.findViewById(R.id.num_images);
        TextView dialogTitle = (TextView) dialogView.findViewById(R.id.dialog_title);

        dialogTitle.setText("Enter website:");

        dialog.setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int id) {
	        	if (!websiteUrl.getText().toString().equals("") && !websiteTitle.getText().toString().equals("")){
	        		
	        		if (!websiteUrl.getText().toString().contains("http")) {
	        			websiteUrl.setText("http://" + websiteUrl.getText().toString());
	        		}
	        		
	        		if (numImages.getText().toString().equals("")) {
	        			numImages.setText("1");
	        		}
	        		
	        		listAdapter.addItem(WEBSITE, websiteTitle.getText().toString(), websiteUrl.getText().toString(), true, numImages.getText().toString());
                    listAdapter.saveData();
                    hide(addWebsiteTutorial);
	        	}
	        }
        });
        dialog.setNegativeButton(R.string.cancel_button, new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int id) {
		    }
        });
	    dialog.show();
	}
	
	private void showDialogForChange(final int position) {
		
		final HashMap<String, String> clickedItem = listAdapter.getItem(position);
		
		AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
		
		LayoutInflater inflater = getActivity().getLayoutInflater();
		
		View dialogView = inflater.inflate(R.layout.add_website_dialog, null);
		
		dialog.setView(dialogView);

		final EditText websiteTitle = (EditText) dialogView.findViewById(R.id.website_title);
		final EditText websiteUrl = (EditText) dialogView.findViewById(R.id.website_url);
		final EditText numImages = (EditText) dialogView.findViewById(R.id.num_images);
        TextView dialogTitle = (TextView) dialogView.findViewById(R.id.dialog_title);

        dialogTitle.setText("Enter website:");
		
		websiteTitle.setText(clickedItem.get("title"));
		websiteUrl.setText(clickedItem.get("url"));
		numImages.setText(clickedItem.get("num"));
		
        dialog.setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int id) {
	        	if (!websiteUrl.getText().toString().equals("") && !websiteTitle.getText().toString().equals("")){
	        		
	        		if (!websiteUrl.getText().toString().contains("http")) {
	        			websiteUrl.setText("http://" + websiteUrl.getText().toString());
	        		}
	        		
	        		if (numImages.getText().toString().equals("")) {
	        			numImages.setText("1");
	        		}
	        		
	        		listAdapter.setItem(position, WEBSITE, websiteTitle.getText().toString(), websiteUrl.getText().toString(), Boolean.valueOf(clickedItem.get("use")), numImages.getText().toString());
                    listAdapter.saveData();
	        	}
	        }
        });
        dialog.setNegativeButton(R.string.cancel_button, new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int id) {
		    }
        });
	    dialog.show();
	}
	
	private void showDialogMenu(final int position) {
		AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
		
		dialog.setItems(R.array.website_entry_menu, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which) {
					case 0: 
						showDialogForChange(position);
						break;
					case 1:	
						listAdapter.removeItem(position);
					default:
				}
				
			}
		});
		
		dialog.show();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final Context contextThemeWrapper = new ContextThemeWrapper(getActivity(), AppSettings.getTheme());

        LayoutInflater localInflater = inflater.cloneInContext(contextThemeWrapper);

        View view = localInflater.inflate(R.layout.fragment_websites, container, false);

        String ua = "Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/37.0.2049.0 Safari/537.36";
        webView = (WebView) view.findViewById(R.id.webview);
        webView.getSettings().setUserAgentString(ua);

        setButton = (Button) view.findViewById(R.id.set_button);
        setButton.setText("Set Wallpaper");
        setButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (setTutorial != null) {
                    hide(setTutorial);
                    showTutorial(5);
                }
                setWallpaper();
            }

        });

        downloadButton = (Button) view.findViewById(R.id.download_button);
        downloadButton.setText("Download Wallpaper");
        downloadButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (downloadTutorial != null) {
                    hide(downloadTutorial);
                    showTutorial(4);
                    Log.i("WLF", "Showing 4");
                }
                if (!isDownloading) {
                    listAdapter.saveData();
                    if (AppSettings.useExperimentalDownloader()){
                        getHtml();
                    }
                    else {
                        Downloader.download(context);
                    }
                } else {
                    Log.i("MP", "isDownloading");
                }
            }

        });

        Button refreshButton = (Button) view.findViewById(R.id.refresh_button);;
        refreshButton.setText("Cycle Wallpaper");
        refreshButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                cycleWallpaper();
                if (AppSettings.useToast()) {
                    Toast.makeText(context, "Cycling...", Toast.LENGTH_SHORT).show();
                }
            }

        });

		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onActivityCreated(savedInstanceState);
		
		if (listAdapter == null) {
			listAdapter = new SourceListAdapter(getActivity());
			for (int i = 0; i < AppSettings.getNumSources(); i++) {
				listAdapter.addItem(WEBSITE, AppSettings.getSourceTitle(i), AppSettings.getSourceData(i), AppSettings.useSource(i), "" + AppSettings.getSourceNum(i));
                Log.i("WLF", "Added: " + AppSettings.getSourceTitle(i));
			}
		}
		setListAdapter(listAdapter);
        
		TextView emptyText = new TextView(getActivity());
		emptyText.setText("List is empty. Please add new website entry.");
		emptyText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
		emptyText.setGravity(Gravity.CENTER_HORIZONTAL);

		LinearLayout emptyLayout = new LinearLayout(getActivity());
		emptyLayout.setOrientation(LinearLayout.VERTICAL);
		emptyLayout.setGravity(Gravity.TOP);
		emptyLayout.addView(emptyText);
		
		((ViewGroup) getListView().getParent()).addView(emptyLayout, 0);
		
		getListView().setEmptyView(emptyLayout);
        getListView().setDividerHeight(1);
		
	}

    @Override
    public void onStart() {
        super.onStart();

        Log.i("WLF", "onStart");

    }

    private void hide(ShowcaseView view) {
        if (view != null) {
            view.hide();
        }
    }

    private void showTutorial(int page) {

        if (!AppSettings.useTutorial()) {
            return;
        }

        switch (page) {
            case 0:
                View.OnClickListener tutorialPromptListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showTutorial(1);
                        hide(tutorialPromptView);
                    }
                };
                tutorialPromptView = new ShowcaseView.Builder(getActivity())
                        .setContentTitle("Welcome to AutoBackground")
                        .setContentText("If you would like to go through \n" +
                                "a quick tutorial, hit the OK button. \n" +
                                "\n" +
                                "If not, just click anywhere else. \n" +
                                "\n" +
                                "You are always able to reshow this \n" +
                                "tutorial in the Application Settings.")
                        .setStyle(R.style.ShowcaseStyle)
                        .setOnClickListener(tutorialPromptListener)
                        .hideOnTouchOutside()
                        .setShowcaseEventListener(new OnShowcaseEventListener() {
                            @Override
                            public void onShowcaseViewHide(ShowcaseView showcaseView) {
                                if (websiteListTutorial == null) {
                                    AppSettings.setTutorial(false);
                                    tutorialPromptView = null;
                                }
                            }

                            @Override
                            public void onShowcaseViewDidHide(ShowcaseView showcaseView) {

                            }

                            @Override
                            public void onShowcaseViewShow(ShowcaseView showcaseView) {

                            }
                        })
                        .build();
                break;
            case 1:
                View.OnClickListener websiteListListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        hide(websiteListTutorial);
                        showTutorial(2);
                        websiteListTutorial = null;
                    }
                };

                ShowcaseView.Builder websiteListBuilder = new  ShowcaseView.Builder(getActivity())
                    .setContentTitle("Website List")
                    .setContentText("This is a list of your websites. \n" +
                            "Here you can edit their titles, URLs, \n" +
                            "and number of images.")
                    .setStyle(R.style.ShowcaseStyle)
                    .setOnClickListener(websiteListListener);

                if (android.os.Build.VERSION.SDK_INT < 20) {
                    websiteListBuilder.setTarget(new ActionViewTarget(getActivity(), ActionViewTarget.Type.HOME));
                }

                websiteListTutorial = websiteListBuilder.build();
                break;
            case 2:
                View.OnClickListener addWebsiteListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        hide(addWebsiteTutorial);
                        showTutorial(3);
                        addWebsiteTutorial = null;
                    }
                };

                ShowcaseView.Builder addWebsiteBuilder = new ShowcaseView.Builder(getActivity())
                        .setContentTitle("Adding Websites")
                        .setContentText("To add a new website entry, \n" +
                                "click the plus (+) sign.")
                        .setStyle(R.style.ShowcaseStyle)
                        .setOnClickListener(addWebsiteListener);

                if (android.os.Build.VERSION.SDK_INT < 20) {
                    addWebsiteBuilder.setTarget(new ActionItemTarget(getActivity(), R.id.add_website));
                }

                addWebsiteTutorial = addWebsiteBuilder.build();
                break;
            case 3:
                View.OnClickListener downloadListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        hide(downloadTutorial);
                        showTutorial(4);
                        downloadTutorial = null;
                    }
                };
                downloadTutorial = new ShowcaseView.Builder(getActivity())
                        .setContentTitle("Downloading Images")
                        .setContentText("Once you have a website entered, \n" +
                                "click this download button to start \n" +
                                "downloading some images.")
                        .setStyle(R.style.ShowcaseStyle)
                        .setTarget(new ViewTarget(downloadButton))
                        .setOnClickListener(downloadListener)
                        .build();
                break;
            case 4:
                View.OnClickListener setListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        hide(setTutorial);
                        showTutorial(5);
                        setTutorial = null;
                    }
                };
                if (setButton.getVisibility() == View.VISIBLE) {
                    setTutorial = new ShowcaseView.Builder(getActivity())
                            .setContentTitle("Setting the wallpaper")
                            .setContentText("Now that it's downloading, \n" +
                                    "it's time to set the app \n" +
                                    "as your system wallpaper. \n" +
                                    "Click the set button and \n" +
                                    "hit apply on next page.")
                            .setStyle(R.style.ShowcaseStyle)
                            .setTarget(new ViewTarget(setButton))
                            .setOnClickListener(setListener)
                            .build();
                    setShown = true;
                }
                else {
                    showTutorial(5);
                }
                break;
            case 5:
                if (setShown) {
                    hide(setTutorial);
                }
                View.OnClickListener settingsListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        hide(settingsTutorial);
                        showTutorial(6);
                        settingsTutorial = null;
                    }
                };
                ShowcaseView.Builder settingsBuilder = new ShowcaseView.Builder(getActivity())
                        .setContentTitle("Accessing Settings")
                        .setContentText("To open the other settings, \n" +
                                "click the app icon in the top left, \n" +
                                "which opens a list of settings.")
                        .setStyle(R.style.ShowcaseStyle)
                        .setOnClickListener(settingsListener);

                if (android.os.Build.VERSION.SDK_INT < 20) {
                    settingsBuilder.setTarget(new ActionViewTarget(getActivity(), ActionViewTarget.Type.HOME));
                }

                settingsTutorial = settingsBuilder.build();
                break;
            case 6:
                AppSettings.setTutorial(false);
                break;
            default:
        }

    }


	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		showDialogMenu(position);
		
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

        context.sendBroadcast(intent);
    }

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		setListAdapter(null);
	}

	@Override
	public void onPause() {
		super.onPause();
		listAdapter.saveData();
	}

	@Override
	public void onResume() {
		super.onResume();

        if (isServiceRunning(LiveWallpaperService.class.getName())) {
            setButton.setVisibility(View.GONE);
        }
        else {
            setButton.setVisibility(View.VISIBLE);
        }

        if (AppSettings.useTutorial() && tutorialPromptView == null) {

            Log.i("WLF", "Showing tutorial");

            showTutorial(0);

        }
	}

    private boolean isServiceRunning(final String className) {
        final ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (final ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (className.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

	@Override
	public void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
	}

    public void getHtml() {

        isDownloading = true;

        final Handler handler = new Handler();

        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                Log.i("MP", "Test1");

                for (int i = 0; i < AppSettings.getNumSources(); i++) {

                    Log.i("MP", "Test2");

                    final int index = i;

                    if (AppSettings.useSource(i)) {

                        handler.post(new Runnable() {

                            @Override
                            public void run() {
                                Log.i("MP", "Test3");

                                String url = AppSettings.getSourceData(index);

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
                                        Downloader.setHtml(html, context.getCacheDir().getAbsolutePath(), index, getBaseUrl(), context);
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
                                    Toast.makeText(context, "Loading page", Toast.LENGTH_SHORT).show();
                                }

                                webView.getSettings().setJavaScriptEnabled(true);

                                webView.loadUrl(url);

                                Log.i("MP", "WebView finished");
                            }
                        });
                    }
                }
                isDownloading = false;
            }
        });
        thread.start();
    }

    private String getBaseUrl() {
        return baseUrl;
    }

}
