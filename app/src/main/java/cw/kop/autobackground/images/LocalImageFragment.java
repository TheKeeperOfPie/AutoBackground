package cw.kop.autobackground.images;

import java.io.File;

import cw.kop.autobackground.R;

import cw.kop.autobackground.settings.AppSettings;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.Toast;

public class LocalImageFragment extends Fragment {

	private Context context;
	private LocalImageAdapter imageAdapter;
	private GridView gridView;
	private File dir;
	
	public LocalImageFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setHasOptionsMenu(true);
		
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		// TODO Auto-generated method stub
		
		super.onCreateOptionsMenu(menu, inflater);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		ViewGroup view = (ViewGroup) inflater.inflate(R.layout.image_grid_layout, null);
		
		Button useDirectoryButton = (Button) view.findViewById(R.id.use_directory_button);
		useDirectoryButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				File dir = imageAdapter.getDirectory();
				if (dir != null && dir.exists() && dir.isDirectory() && dir.canWrite()) {
					AppSettings.setDownloadPath(dir.getAbsolutePath());
					Toast.makeText(context, "Directory set to " + dir.getAbsolutePath(), Toast.LENGTH_SHORT).show();
					imageAdapter.setFinished(true);
					getActivity().onBackPressed();
				}
				else {
					Toast.makeText(context, "Invalid directory", Toast.LENGTH_SHORT).show();
				}
			}
			
		});
		
		Button resetDirectoryButton = (Button) view.findViewById(R.id.reset_directory_button);
		resetDirectoryButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				AppSettings.setDownloadPath(context.getCacheDir().getAbsolutePath());
				Toast.makeText(context, "Reset directory", Toast.LENGTH_SHORT).show();
				imageAdapter.setFinished(true);
				getActivity().onBackPressed();
			}
			
		});
		gridView = (GridView) view.findViewById(R.id.image_gridview);
		
		return view;
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		context = getActivity();
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		getView().setFocusableInTouchMode(true);
		
		dir = Environment.getExternalStorageDirectory();
		
		if (imageAdapter == null) {
			imageAdapter = new LocalImageAdapter(getActivity(), dir);
		}

		getView().setOnKeyListener( new OnKeyListener()
		{

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if(keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP)
		        {
					if (!imageAdapter.backDirectory()) {
						return true;
					}
		        }
		        return false;
			}
			
		});
		
		gridView.setAdapter(imageAdapter);
		gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            	
				File selectedFile = imageAdapter.getItem(position);
				
				if (selectedFile.isDirectory()) {
					imageAdapter.setDirectory(selectedFile);
				}
            }
        });
	}
	
}
