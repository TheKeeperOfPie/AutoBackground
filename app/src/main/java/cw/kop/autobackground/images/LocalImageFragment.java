package cw.kop.autobackground.images;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;

import cw.kop.autobackground.Downloader;
import cw.kop.autobackground.MainPreferences;
import cw.kop.autobackground.R;
import cw.kop.autobackground.settings.AppSettings;
import cw.kop.autobackground.sources.SourceListFragment;

public class LocalImageFragment extends Fragment implements ListView.OnItemClickListener {

	private Context context;
	private LocalImageAdapter imageAdapter;
    private ListView imageListView;
	private File dir;
    private FilenameFilter filenameFilter;

    private boolean change, setPath;
    private int position;
    private String viewPath = "";

	public LocalImageFragment() {
    }

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();
        change = bundle.getBoolean("change", false);
        setPath = bundle.getBoolean("set_path", false);
        viewPath = bundle.getString("view_path", "");
        position = bundle.getInt("position", 0);

        filenameFilter = (new FilenameFilter() {

            @Override
            public boolean accept(File dir, String filename) {
                if (filename.endsWith(".jpg") || filename.endsWith(".png")) {
                    return true;
                }
                return false;
            }
        });
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		final ViewGroup view = (ViewGroup) inflater.inflate(R.layout.image_grid_layout, null);

        imageListView = (ListView) view.findViewById(R.id.image_listview);

        TextView emptyText = new TextView(getActivity());
        emptyText.setText("Directory is empty.");
        emptyText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        emptyText.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        emptyText.setGravity(Gravity.CENTER_HORIZONTAL);

        LinearLayout emptyLayout = new LinearLayout(getActivity());
        emptyLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        emptyLayout.setGravity(Gravity.TOP);
        emptyLayout.addView(emptyText);

        if (AppSettings.getTheme() == R.style.AppLightTheme) {
            imageListView.setBackgroundColor(getResources().getColor(R.color.WHITE_OPAQUE));
            emptyLayout.setBackgroundColor(getResources().getColor(R.color.WHITE_OPAQUE));
        }
        else {
            imageListView.setBackgroundColor(getResources().getColor(R.color.BLACK_OPAQUE));
            emptyLayout.setBackgroundColor(getResources().getColor(R.color.BLACK_OPAQUE));
        }

        ((ViewGroup) imageListView.getParent()).addView(emptyLayout, 0);

        imageListView.setEmptyView(emptyLayout);

		Button useDirectoryButton = (Button) view.findViewById(R.id.use_directory_button);
		useDirectoryButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				File dir = imageAdapter.getDirectory();
                if (setPath) {
                    AppSettings.setDownloadPath(dir.getAbsolutePath());
                    Toast.makeText(context, "Download path set to: \n" + AppSettings.getDownloadPath(context), Toast.LENGTH_SHORT).show();
                }
                else {
                    SourceListFragment sourceListFragment = ((MainPreferences) getActivity()).websiteFragment; //.getFragmentManager().findFragmentByTag("website_fragment");
                    if (change) {
                        sourceListFragment.setFolder(position, dir.getName(), dir.getAbsolutePath(), dir.listFiles(filenameFilter).length);
                    } else {
                        sourceListFragment.addFolder(dir.getName(), dir.getAbsolutePath(), dir.listFiles(filenameFilter).length);
                    }
                }
                imageAdapter.setFinished(true);
                getActivity().onBackPressed();
			}
			
		});

        if (!viewPath.equals("")) {
            useDirectoryButton.setVisibility(View.GONE);
        }
		
		return view;
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		context = getActivity();
	}

    @Override
    public void onResume() {
        super.onResume();

    }

    public boolean onBackPressed() {

        return imageAdapter.backDirectory();

    }

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		dir = Environment.getExternalStorageDirectory();

        if (!viewPath.equals("")) {
            dir = new File(viewPath);
        }

		if (imageAdapter == null) {
			imageAdapter = new LocalImageAdapter(getActivity(), dir);
		}

        imageListView.setAdapter(imageAdapter);
        imageListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            	
				File selectedFile = imageAdapter.getItem(position);
				
				if (selectedFile.isDirectory()) {
					imageAdapter.setDirectory(selectedFile);
				}
            }
        });

        imageListView.setOnItemClickListener(this);
	}

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        if (imageAdapter.getItem(position).getName().contains(".png") || imageAdapter.getItem(position).getName().contains(".jpg")) {

            Uri uri = FileProvider.getUriForFile(context, "cw.kop.fileprovider", imageAdapter.getItem(position));

            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.setType("image/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            shareIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            List<ResolveInfo> resInfoList = context.getPackageManager().queryIntentActivities(shareIntent, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolveInfo : resInfoList) {
                String packageName = resolveInfo.activityInfo.packageName;
                context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            shareIntent = Intent.createChooser(shareIntent, "Share Image");
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(shareIntent);
            context.revokeUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }

    }
}
