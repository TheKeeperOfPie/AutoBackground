package cw.kop.autobackground.sources;

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
import java.util.TreeMap;

import cw.kop.autobackground.Downloader;
import cw.kop.autobackground.LiveWallpaperService;
import cw.kop.autobackground.R;
import cw.kop.autobackground.images.LocalImageFragment;
import cw.kop.autobackground.settings.AppSettings;

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
		
		inflater.inflate(R.menu.source_actions, menu);
		
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
			case R.id.add_source:
				showSourceMenu();
				return true;
            case R.id.sort_sources:
                showSourceSortMenu();
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
                        LocalImageFragment localImageFragment = new LocalImageFragment();
                        Bundle arguments = new Bundle();
                        arguments.putBoolean("change", false);
                        arguments.putBoolean("set_path", false);
                        localImageFragment.setArguments(arguments);

                        getFragmentManager().beginTransaction()
                                .add(R.id.content_frame, localImageFragment, "image_fragment")
                                .addToBackStack(null)
                                .commit();
                        break;
                    default:
                }

            }
        });

        dialog.show();
    }

    private void showSourceSortMenu() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());

        dialog.setTitle("Sort by:");

        dialog.setItems(R.array.source_sort_menu, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
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

            }
        });

        dialog.show();
    }

    public void addFolder(String title, String path, int num) {
        if (listAdapter.addItem(FOLDER, title, path, true, "" + num)) {
            listAdapter.saveData();
        }
        else {
            Toast.makeText(context, "Error: Title in use.\nPlease use a different title.", Toast.LENGTH_SHORT).show();
        }

    }

    public void setFolder(int position, String title, String path, int num) {
        if (listAdapter.setItem(position, FOLDER, title, path, true, "" + num)) {
            listAdapter.saveData();
        }
        else {
            Toast.makeText(context, "Error: Title in use.\nPlease use a different title.", Toast.LENGTH_SHORT).show();
        }
    }

	private void showDialogForInput() {

		AlertDialog.Builder dialog = new AlertDialog.Builder(context);

        View dialogView = View.inflate(context, R.layout.add_source_dialog, null);
		
		dialog.setView(dialogView);

		final EditText sourceTitle = (EditText) dialogView.findViewById(R.id.source_title);
		final EditText sourceData = (EditText) dialogView.findViewById(R.id.source_data);
		final EditText sourceNum = (EditText) dialogView.findViewById(R.id.source_num);
        TextView dialogTitle = (TextView) dialogView.findViewById(R.id.dialog_title);

        dialogTitle.setText("Enter website:");

        dialog.setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int id) {
	        	if (!sourceData.getText().toString().equals("") && !sourceTitle.getText().toString().equals("")){
	        		
	        		if (!sourceData.getText().toString().contains("http")) {
	        			sourceData.setText("http://" + sourceData.getText().toString());
	        		}
	        		
	        		if (sourceNum.getText().toString().equals("")) {
	        			sourceNum.setText("1");
	        		}
	        		
	        		if (listAdapter.addItem(WEBSITE, sourceTitle.getText().toString(), sourceData.getText().toString(), true, sourceNum.getText().toString())) {
                        listAdapter.saveData();
                        hide(addWebsiteTutorial);
                    }
                    else {
                        Toast.makeText(context, "Error: Title in use.\nPlease use a different title.", Toast.LENGTH_SHORT).show();
                    }
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
		
		View dialogView = inflater.inflate(R.layout.add_source_dialog, null);
		
		dialog.setView(dialogView);

		final EditText sourceTitle = (EditText) dialogView.findViewById(R.id.source_title);
		final EditText sourceData = (EditText) dialogView.findViewById(R.id.source_data);
		final EditText sourceNum = (EditText) dialogView.findViewById(R.id.source_num);
        TextView dialogTitle = (TextView) dialogView.findViewById(R.id.dialog_title);

        dialogTitle.setText("Enter website:");
		
		sourceTitle.setText(clickedItem.get("title"));
		sourceData.setText(clickedItem.get("data"));
		sourceNum.setText(clickedItem.get("num"));
		
        dialog.setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int id) {
	        	if (!sourceData.getText().toString().equals("") && !sourceTitle.getText().toString().equals("")){
	        		
	        		if (!sourceData.getText().toString().contains("http")) {
	        			sourceData.setText("http://" + sourceData.getText().toString());
	        		}
	        		
	        		if (sourceNum.getText().toString().equals("")) {
	        			sourceNum.setText("1");
	        		}
	        		
	        		if (listAdapter.setItem(position, WEBSITE, sourceTitle.getText().toString(), sourceData.getText().toString(), Boolean.valueOf(clickedItem.get("use")), sourceNum.getText().toString())) {
                        listAdapter.saveData();
                    }
                    else {
                        Toast.makeText(context, "Error: Title in use.\nPlease use a different title.", Toast.LENGTH_SHORT).show();
                    }
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
                        if (listAdapter.getItem(position).get("type").equals(WEBSITE)) {
                            showDialogForChange(position);
                        }
                        else if(listAdapter.getItem(position).get("type").equals(FOLDER)) {
                            LocalImageFragment localImageFragment = new LocalImageFragment();
                            Bundle arguments = new Bundle();
                            arguments.putBoolean("change", true);
                            arguments.putBoolean("set_path", false);
                            arguments.putInt("position", position);
                            localImageFragment.setArguments(arguments);

                            getFragmentManager().beginTransaction()
                                    .add(R.id.content_frame, localImageFragment, "image_fragment")
                                    .addToBackStack(null)
                                    .commit();
                        }
						break;
					case 1:
                        if (listAdapter.getItem(position).get("type").equals(WEBSITE)) {
                            listAdapter.saveData();
                            AlertDialog.Builder deleteDialog = new AlertDialog.Builder(context);

                            deleteDialog.setTitle("Delete images associated with this source?");
                            deleteDialog.setMessage("This cannot be undone.");

                            deleteDialog.setPositiveButton(R.string.yes_button, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    Downloader.deleteBitmaps(context, AppSettings.getSourceTitle(position));
                                    Toast.makeText(context, "Deleting " + AppSettings.getSourceTitle(position) + " images", Toast.LENGTH_SHORT).show();
                                    listAdapter.removeItem(position);
                                    listAdapter.saveData();
                                }
                            });
                            deleteDialog.setNeutralButton(R.string.no_button, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    listAdapter.removeItem(position);
                                    listAdapter.saveData();
                                }
                            });
                            deleteDialog.setNegativeButton(R.string.cancel_button, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                }
                            });

                            deleteDialog.show();
                        }
                        else {
                            listAdapter.removeItem(position);
                            listAdapter.saveData();
                        }
					default:
				}
				
			}
		});
		
		dialog.show();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_websites, container, false);

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
				listAdapter.addItem(AppSettings.getSourceType(i), AppSettings.getSourceTitle(i), AppSettings.getSourceData(i), AppSettings.useSource(i), "" + AppSettings.getSourceNum(i));
                Log.i("WLF", "Added: " + AppSettings.getSourceTitle(i));
			}
		}
		setListAdapter(listAdapter);
        
		TextView emptyText = new TextView(getActivity());
		emptyText.setText("List is empty. Please add new website entry.");
		emptyText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        emptyText.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        emptyText.setGravity(Gravity.CENTER_HORIZONTAL);

		LinearLayout emptyLayout = new LinearLayout(getActivity());
        emptyLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		emptyLayout.setGravity(Gravity.TOP);
		emptyLayout.addView(emptyText);

		((ViewGroup) getListView().getParent()).addView(emptyLayout, 0);

		getListView().setEmptyView(emptyLayout);
        getListView().setDividerHeight(1);
		
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
                    .setContentTitle("Sources List")
                    .setContentText("This is a list of your sources. \n" +
                            "These can include both sources and your \n" +
                            "own image folders. You can edit them by \n" +
                            "tapping on their boxes.")
                    .setStyle(R.style.ShowcaseStyle)
                    .setOnClickListener(websiteListListener);

                if (android.os.Build.VERSION.SDK_INT < 20) {
                    websiteListBuilder.setTarget(new ActionViewTarget(getActivity(), ActionViewTarget.Type.TITLE));
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
                        .setContentText(
                                "To add a new website entry, \n" +
                                "click the plus (+) sign. \n" +
                                "\n" +
                                "Not all sources will work, \n" +
                                "so if there are no images, \n" +
                                "try a different website. \n" +
                                "\n" +
                                "Provided is a website \n" +
                                "of some landscape photos \n" +
                                "taken by Kai Lehnberg.")
                        .setStyle(R.style.ShowcaseStyle)
                        .setOnClickListener(addWebsiteListener);

                if (android.os.Build.VERSION.SDK_INT < 20) {
                    addWebsiteBuilder.setTarget(new ActionItemTarget(getActivity(), R.id.add_source));
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
        intent.setAction(LiveWallpaperService.CYCLE_IMAGE);
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
