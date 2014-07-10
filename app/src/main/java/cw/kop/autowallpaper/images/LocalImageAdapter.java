package cw.kop.autowallpaper.images;


import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import cw.kop.autowallpaper.R;
import cw.kop.autowallpaper.settings.AppSettings;

public class LocalImageAdapter extends BaseAdapter {

	private Activity mainActivity;
	private File mainDir;
    private ArrayList<File> listFiles;
    private LayoutInflater inflater = null;
    private boolean finish;
	
    public LocalImageAdapter(Activity activity, File directory) {
		mainActivity = activity;
		listFiles = new ArrayList<File>();
		inflater = (LayoutInflater) mainActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mainDir = directory;
		setDirectory(mainDir);
	}
    
	@Override
	public int getCount() {
		return listFiles.size();
	}

	@Override
	public File getItem(int position) {
		return listFiles.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		if (listFiles.size() > 0) {

			
			File file = listFiles.get(position);
			
			View view = convertView;
			
			if (convertView == null) {
				view = inflater.inflate(R.layout.image_list_cell, null);
			}
			
			TextView fileTitle = (TextView) view.findViewById(R.id.file_title);
			TextView fileSummary = (TextView) view.findViewById(R.id.file_summary);
			ImageView fileImage = (ImageView) view.findViewById(R.id.file_image);
			
			if (file.getName().contains(".jpg") || file.getName().contains(".png")) {
				Picasso.with(mainActivity.getApplicationContext())
					.load(file)
					.resize(50, 50)
					.into(fileImage);
			}
			else if (file.isDirectory()){
				Picasso.with(mainActivity.getApplicationContext())
					.load(R.drawable.ic_action_collection)
					.resize(50, 50)
					.into(fileImage);
			}
			else {
				Picasso.with(mainActivity.getApplicationContext())
					.load(R.drawable.ic_action_view_as_list)
					.resize(50, 50)
					.into(fileImage);
			}
			
			fileTitle.setText(file.getName());
			fileSummary.setText("" + file.getTotalSpace());
			return view;
		}
		else {
			return convertView;
		}
	}

	public void addItem(File file) {
		listFiles.add(file);
	}

	public File getDirectory() {
		return mainDir;
	}
	
	public void setDirectory(File selectedFile) {
		
		if (selectedFile != null && selectedFile.isDirectory()) {
			mainDir = selectedFile;
			
			ArrayList<File> tempList = new ArrayList<File>();
			
			if (selectedFile.listFiles() != null) {
				for (File file : selectedFile.listFiles()) {
					if (file != null && file.exists()) {
						tempList.add(file);
					}
				}
			}
			
			if (tempList.size() > 0) {
				Collections.sort(tempList);
			}
			
			listFiles = tempList;
			notifyDataSetChanged();
		}
		
		if (selectedFile != null && selectedFile.isDirectory() && selectedFile.list().length == 0) {
            if (AppSettings.useToast()) {
                Toast.makeText(mainActivity.getApplicationContext(), "No files in this folder", Toast.LENGTH_SHORT).show();
            }
		}
		
	}
	
	public void setFinished(boolean end) {
		finish = end;
	}

	public Boolean backDirectory() {
		
		if (finish) {
			return true;
		}
		
		File parentDir = mainDir.getParentFile();
		
		if (parentDir != null && parentDir.exists() && parentDir.isDirectory()) {
			setDirectory(parentDir);
			return false;
		}
		return true;
	}
	
}
