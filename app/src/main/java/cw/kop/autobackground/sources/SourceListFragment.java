package cw.kop.autobackground.sources;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.ListFragment;
import android.app.PendingIntent;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;

import java.util.HashMap;
import java.util.HashSet;

import cw.kop.autobackground.LiveWallpaperService;
import cw.kop.autobackground.R;
import cw.kop.autobackground.downloader.Downloader;
import cw.kop.autobackground.images.LocalImageFragment;
import cw.kop.autobackground.settings.AppSettings;

public class SourceListFragment extends ListFragment {

	private SourceListAdapter listAdapter;
    private Context context;
    private Button setButton;
    private ImageButton addButton;
    private ImageView downloadButton;
    private ImageView sortButton;

    private ShowcaseView sourceListTutorial;
    private ShowcaseView addSourceTutorial;
    private ShowcaseView downloadTutorial;
    private ShowcaseView setTutorial;
    private ShowcaseView settingsTutorial;
    private RelativeLayout.LayoutParams buttonParams;
    private boolean setShown = false;
    private boolean tutorialShowing = false;

	public SourceListFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_sources, container, false);

        buttonParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        buttonParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        buttonParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        buttonParams.setMargins(0, 0, 0, Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, context.getResources().getDisplayMetrics())));

        addButton = (ImageButton) view.findViewById(R.id.floating_button);
        addButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                showSourceMenu();
            }

        });

        setButton = (Button) view.findViewById(R.id.set_button);
        setButton.setText("Set Wallpaper");
        setButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (setTutorial != null) {
                    hide(setTutorial);
                    showTutorial(4);
                }
                setWallpaper();
            }

        });

        downloadButton = (ImageView) getActivity().getActionBar().getCustomView().findViewById(R.id.download_wallpaper);
        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startDownload();
            }
        });

        sortButton = (ImageView) getActivity().getActionBar().getCustomView().findViewById(R.id.sort_sources);
        sortButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSourceSortMenu();
            }
        });

        if (AppSettings.getTheme() != R.style.AppLightTheme) {
            sortButton.setImageResource(R.drawable.ic_action_storage_dark);
        }

        return view;
    }

    private void startDownload() {
        listAdapter.saveData();
        if (downloadTutorial != null) {
            hide(downloadTutorial);
            showTutorial(3);
        }
        if (Downloader.download(context)) {
            if (AppSettings.getTheme() == R.style.AppLightTheme) {
                downloadButton.setImageResource(R.drawable.ic_action_cancel);
            }
            else {
                downloadButton.setImageResource(R.drawable.ic_action_cancel_dark);
            }

            if (AppSettings.resetOnManualDownload() && AppSettings.useTimer() && AppSettings.getTimerDuration() > 0) {
                Intent intent = new Intent();
                intent.setAction(LiveWallpaperService.DOWNLOAD_WALLPAPER);
                intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                alarmManager.cancel(pendingIntent);
                alarmManager.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis() + AppSettings.getTimerDuration(), AppSettings.getTimerDuration(), pendingIntent);
            }

        }
        else {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);

            dialogBuilder.setTitle("Cancel download?");

            dialogBuilder.setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    Downloader.cancel(getActivity());
                    resetActionBarDownload();
                }
            });
            dialogBuilder.setNegativeButton(R.string.cancel_button, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                }
            });

            dialogBuilder.show();
        }
    }

    public void resetActionBarDownload() {
        if (AppSettings.getTheme() == R.style.AppLightTheme) {
            downloadButton.setImageResource(R.drawable.ic_action_download);
        }
        else {
            downloadButton.setImageResource(R.drawable.ic_action_download_dark);
        }
        downloadButton.postInvalidate();
    }

    public void resetDownload() {
        resetActionBarDownload();
    }

    private void showImageFragment(boolean change, boolean setPath, String viewPath, int position) {
        LocalImageFragment localImageFragment = new LocalImageFragment();
        Bundle arguments = new Bundle();
        arguments.putBoolean("change", change);
        arguments.putBoolean("set_path", setPath);
        arguments.putString("view_path", viewPath);
        arguments.putInt("position", position);
        localImageFragment.setArguments(arguments);

        getFragmentManager().beginTransaction()
                .add(R.id.content_frame, localImageFragment, "image_fragment")
                .addToBackStack(null)
                .commit();
    }

    private void showSourceMenu() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());

        dialog.setItems(R.array.source_menu, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        showInputDialog(AppSettings.WEBSITE,
                                "",
                                "",
                                "",
                                "",
                                "Enter website:",
                                -1);
                        break;
                    case 1:
                        showImageFragment(false, false, "", 0);
                        break;
                    case 2:
                        showInputDialog(AppSettings.IMGUR,
                                "",
                                "imgur.com/r/",
                                "",
                                "",
                                "Enter Imgur subreddit:",
                                -1);
                        break;
                    case 3:
                        showInputDialog(AppSettings.IMGUR,
                                "",
                                "imgur.com/a/",
                                "",
                                "",
                                "Enter Imgur album:",
                                -1);
                        break;
                    default:
                }
            }
        });

        dialog.show();
    }

    private void showSourceSortMenu() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(context);

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
        if (listAdapter.addItem(AppSettings.FOLDER, title, path, true, "" + num)) {
            listAdapter.saveData();
        }
        else {
            Toast.makeText(context, "Error: Title in use.\nPlease use a different title.", Toast.LENGTH_SHORT).show();
        }

    }

    public void setFolder(int position, String title, String path, int num) {
        if (listAdapter.setItem(position, AppSettings.FOLDER, title, path, true, "" + num)) {
            listAdapter.saveData();
        }
        else {
            Toast.makeText(context, "Error: Title in use.\nPlease use a different title.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showInputDialog(final String type, String title, final String prefix, String data, String num, String mainTitle, final int position) {

        AlertDialog.Builder dialog = new AlertDialog.Builder(context);

        View dialogView = View.inflate(context, R.layout.add_source_dialog, null);

        dialog.setView(dialogView);

        final EditText sourceTitle = (EditText) dialogView.findViewById(R.id.source_title);
        final TextView sourcePrefix = (TextView) dialogView.findViewById(R.id.source_data_prefix);
        final EditText sourceData = (EditText) dialogView.findViewById(R.id.source_data);
        final EditText sourceNum = (EditText) dialogView.findViewById(R.id.source_num);
        TextView dialogTitle = (TextView) dialogView.findViewById(R.id.dialog_title);

        dialogTitle.setText(mainTitle);
        sourceTitle.setText(title);
        sourcePrefix.setText(prefix);
        sourceData.setText(data);
        sourceNum.setText(num);


        dialog.setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (!sourceData.getText().toString().equals("") && !sourceTitle.getText().toString().equals("")){

                    if (sourceNum.getText().toString().equals("")) {
                        sourceNum.setText("1");
                    }

                    String newTitle = sourceTitle.getText().toString();
                    String data = prefix + sourceData.getText().toString();

                    if (!data.contains("http")) {
                        data = "http://" + data;
                    }

                    if (position >= 0) {
                        String previousTitle = AppSettings.getSourceTitle(position);
                        if (listAdapter.setItem(position, type, newTitle, data, AppSettings.useSource(position), sourceNum.getText().toString())) {
                            if (!previousTitle.equals(newTitle)) {
                                AppSettings.setSourceSet(newTitle, AppSettings.getSourceSet(previousTitle));
                                Downloader.renameFiles(context, previousTitle, newTitle);
                            }
                            listAdapter.saveData();
                        }
                        else {
                            Toast.makeText(context, "Error: Title in use.\nPlease use a different title.", Toast.LENGTH_SHORT).show();
                        }
                    }
                    else {
                        if (listAdapter.addItem(type, newTitle, data, true, sourceNum.getText().toString())) {
                            listAdapter.saveData();
                            AppSettings.setSourceSet(newTitle, new HashSet<String>());
                            hide(addSourceTutorial);
                        }
                        else {
                            Toast.makeText(context, "Error: Title in use.\nPlease use a different title.", Toast.LENGTH_SHORT).show();
                        }
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

        listAdapter.saveData();
        dialog.setTitle(AppSettings.getSourceTitle(position));

		dialog.setItems(R.array.source_edit_menu, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
                HashMap<String, String> item = listAdapter.getItem(position);
                String type = item.get("type");
				switch (which) {
                    case 0:
                        String directory;
                        if (type.equals(AppSettings.WEBSITE) || type.equals(AppSettings.IMGUR)) {
                            directory = AppSettings.getDownloadPath() + "/" + AppSettings.getSourceTitle(position) + " " + AppSettings.getImagePrefix();
                        }
                        else {
                            directory = AppSettings.getSourceData(position);
                        }
                        showImageFragment(false, false, directory, position);
                        break;
					case 1:
                        if (type.equals(AppSettings.WEBSITE)) {
                            showInputDialog(AppSettings.WEBSITE,
                                    AppSettings.getSourceTitle(position),
                                    "",
                                    AppSettings.getSourceData(position),
                                    "" + AppSettings.getSourceNum(position),
                                    "Enter website:",
                                    position);
                        }
                        else if (type.equals(AppSettings.IMGUR)) {
                            String prefix = "";
                            String data = AppSettings.getSourceData(position);
                            if (data.contains("imgur.com/a/")) {
                                prefix = "imgur.com/a/";
                            }
                            else if (data.contains("imgur.com/r/")) {
                                prefix = "imgur.com/r/";
                            }

                            showInputDialog(AppSettings.IMGUR,
                                    AppSettings.getSourceTitle(position),
                                    prefix,
                                    data.substring(data.indexOf(prefix) + prefix.length()),
                                    "" + AppSettings.getSourceNum(position),
                                    "Enter Imgur source:",
                                    position);
                        }
                        else if (type.equals(AppSettings.FOLDER)) {
                            showImageFragment(true, false, "", 0);
                        }
						break;
					case 2:
                        if (type.equals(AppSettings.WEBSITE) || type.equals(AppSettings.IMGUR)) {
                            listAdapter.saveData();
                            AlertDialog.Builder deleteDialog = new AlertDialog.Builder(context);

                            deleteDialog.setTitle("Delete images associated with this source?");
                            deleteDialog.setMessage("This cannot be undone.");

                            deleteDialog.setPositiveButton(R.string.yes_button, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    Downloader.deleteBitmaps(context, position);
                                    Toast.makeText(context, "Deleting " + AppSettings.getSourceTitle(position) + " images", Toast.LENGTH_SHORT).show();
                                    listAdapter.removeItem(position);
                                    listAdapter.saveData();
                                }
                            });
                            deleteDialog.setNeutralButton(R.string.no_button, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    listAdapter.removeItem(position);
                                    AppSettings.setSourceSet(AppSettings.getSourceTitle(position), new HashSet<String>());
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
                        break;
					default:
				}
				
			}
		});
		
		dialog.show();
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
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
		emptyText.setText("List is empty. Please add a new source entry.");
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

        listAdapter.updateNum();
		
	}

    private void hide(ShowcaseView view) {
        if (view != null) {
            view.hide();
        }
    }

    private void showTutorial(int page) {

        switch (page) {
            case 0:
                View.OnClickListener websiteListListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        hide(sourceListTutorial);
                        showTutorial(1);
                        sourceListTutorial = null;
                        Log.i("SLF", "Shown");
                    }
                };

                sourceListTutorial = new  ShowcaseView.Builder(getActivity())
                    .setContentTitle("Sources List")
                    .setContentText("This is a list of your sources. \n" +
                            "These can include both sources and your \n" +
                            "own image folders. You can edit them by \n" +
                            "tapping on their boxes.")
                    .setStyle(R.style.ShowcaseStyle)
                    .setOnClickListener(websiteListListener)
                    .setTarget((new ViewTarget(getActivity().getActionBar().getCustomView().findViewById(R.id.action_bar_title))))
                    .build();
                sourceListTutorial.setButtonPosition(buttonParams);
                break;
            case 1:
                View.OnClickListener addWebsiteListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        hide(addSourceTutorial);
                        showTutorial(2);
                        addSourceTutorial = null;
                    }
                };

                addSourceTutorial = new ShowcaseView.Builder(getActivity())
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
                        .setOnClickListener(addWebsiteListener)
                        .setTarget(new ViewTarget(addButton))
                        .build();
                addSourceTutorial.setButtonPosition(buttonParams);
                break;
            case 2:
                View.OnClickListener downloadListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        hide(downloadTutorial);
                        showTutorial(3);
                        downloadTutorial = null;
                    }
                };
                downloadTutorial = new ShowcaseView.Builder(getActivity())
                        .setContentTitle("Downloading Images")
                        .setContentText("Once you have a website entered, \n" +
                                "click this download button to start \n" +
                                "downloading some images. \n" +
                                "\n" +
                                "The app will only use WiFi to \n" +
                                "download as a default. If you \n" +
                                "wish to change this setting, \n" +
                                "go into the Downloader settings \n" +
                                "and enable mobile data.")
                        .setStyle(R.style.ShowcaseStyle)
                        .setTarget(new ViewTarget(downloadButton))
                        .setOnClickListener(downloadListener)
                        .build();
                downloadTutorial.setButtonPosition(buttonParams);
                break;
            case 3:
                View.OnClickListener setListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        hide(setTutorial);
                        showTutorial(4);
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
                    setTutorial.setButtonPosition(buttonParams);
                    setShown = true;
                }
                else {
                    showTutorial(4);
                }
                break;
            case 4:
                if (setShown) {
                    hide(setTutorial);
                }
                View.OnClickListener settingsListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        hide(settingsTutorial);
                        showTutorial(5);
                        settingsTutorial = null;
                    }
                };
                settingsTutorial = new ShowcaseView.Builder(getActivity())
                        .setContentTitle("Accessing Settings")
                        .setContentText("To open the other settings, \n" +
                                "click the app icon in the top left, \n" +
                                "which opens a list of settings.")
                        .setStyle(R.style.ShowcaseStyle)
                        .setOnClickListener(settingsListener)
                        .setTarget((new ViewTarget(getActivity().getActionBar().getCustomView().findViewById(R.id.drawer_indicator))))
                        .build();
                settingsTutorial.setButtonPosition(buttonParams);
                break;
            case 5:
                AppSettings.setTutorial(false, "source");
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
        if (Build.VERSION.SDK_INT >= 16) {
            i.setAction(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
            final String p = LiveWallpaperService.class.getPackage().getName();
            final String c = LiveWallpaperService.class.getCanonicalName();
            i.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, new ComponentName(p, c));
        } else {
            i.setAction(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER);
        }

        startActivityForResult(i, 0);
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
        sortButton.setVisibility(View.GONE);
        downloadButton.setVisibility(View.GONE);
	}

	@Override
	public void onResume() {
		super.onResume();
        sortButton.setVisibility(View.VISIBLE);
        downloadButton.setVisibility(View.VISIBLE);

        if (Downloader.isDownloading) {
            if (AppSettings.getTheme() == R.style.AppLightTheme) {
                downloadButton.setImageResource(R.drawable.ic_action_cancel);
            }
            else {
                downloadButton.setImageResource(R.drawable.ic_action_cancel_dark);
            }
        }
        else {
            resetActionBarDownload();
        }

        if (isServiceRunning(LiveWallpaperService.class.getName())) {
            setButton.setVisibility(View.GONE);
        }
        else {
            setButton.setVisibility(View.VISIBLE);
        }

        if (AppSettings.useSourceTutorial() && sourceListTutorial == null && !tutorialShowing) {
            Log.i("WLF", "Showing tutorial");
            tutorialShowing = true;
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);

            dialogBuilder.setMessage("Show Sources Tutorial?");

            dialogBuilder.setPositiveButton(R.string.yes_button, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    showTutorial(0);
                }
            });

            dialogBuilder.setNegativeButton(R.string.no_button, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                }
            });

            AlertDialog dialog = dialogBuilder.create();

            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    AppSettings.setTutorial(false, "source");
                    tutorialShowing = false;
                }
            });

            dialog.show();
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

}
