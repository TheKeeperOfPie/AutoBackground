/*
 * Copyright (C) Winson Chiu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cw.kop.autobackground.images;

import android.app.Activity;
import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import cw.kop.autobackground.R;
import cw.kop.autobackground.settings.AppSettings;

public class LocalImageAdapter extends BaseAdapter {

    private static final int BYTE_TO_MEBIBYTE = 1048576;
	private Activity mainActivity;
	private File mainDir;
    private File startDir;
    private ArrayList<File> listFiles;
    private LayoutInflater inflater = null;
    private boolean finish;
    private int screenWidth;
    private int imageHeight;
	
    public LocalImageAdapter(Activity activity, File directory) {
		mainActivity = activity;
		listFiles = new ArrayList<File>();
		inflater = (LayoutInflater) mainActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        startDir = directory;
		mainDir = directory;
		setDirectory(mainDir);
        screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
        imageHeight = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 150, activity.getResources().getDisplayMetrics()));
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
			
			if (file.getName().contains(".png") || file.getName().contains(".jpg") || file.getName().contains(".jpeg")) {
				Picasso.with(parent.getContext())
					.load(file)
                    .resize(screenWidth, imageHeight)
                    .centerCrop()
					.into(fileImage);
			}
			else if (file.isDirectory()){
                if (AppSettings.getTheme().equals(AppSettings.APP_LIGHT_THEME)) {
                    Picasso.with(parent.getContext())
                            .load(R.drawable.ic_action_collection)
                            .into(fileImage);
                }
                else {
                    Picasso.with(parent.getContext())
                            .load(R.drawable.ic_action_collection_dark)
                            .into(fileImage);
                }
			}
			else {
                if (AppSettings.getTheme().equals(AppSettings.APP_LIGHT_THEME)) {
                    Picasso.with(parent.getContext())
                            .load(R.drawable.ic_action_view_as_list)
                            .into(fileImage);
                }
                else {
                    Picasso.with(parent.getContext())
                            .load(R.drawable.ic_action_view_as_list_dark)
                            .into(fileImage);
                }
			}
			
			fileTitle.setText(file.getName());
			fileSummary.setText("" + (file.length() / BYTE_TO_MEBIBYTE) + " MiB");
			return view;
		}
        return null;
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
			
			ArrayList<File> folders = new ArrayList<File>();
            ArrayList<File> files = new ArrayList<File>();
			
			if (selectedFile.listFiles() != null) {
				for (File file : selectedFile.listFiles()) {
					if (file != null && file.exists()) {
						if (file.isDirectory()) {
                            folders.add(file);
                        }
                        else {
                            files.add(file);
                        }
					}
				}
			}
			
			if (folders.size() > 0) {
				Collections.sort(folders, new Comparator<File>() {
                    @Override
                    public int compare(File lhs, File rhs) {
                        return lhs.getName().compareToIgnoreCase(rhs.getName());
                    }
                });
			}

            if (files.size() > 0) {
                Collections.sort(files, new Comparator<File>() {
                    @Override
                    public int compare(File lhs, File rhs) {
                        return lhs.getName().compareToIgnoreCase(rhs.getName());
                    }
                });
            }

            folders.addAll(files);

			listFiles = folders;
			notifyDataSetChanged();
		}
		
	}
	
	public void setFinished() {
		finish = true;
	}

	public Boolean backDirectory() {

        if (finish || mainDir.getAbsolutePath().equals(startDir.getAbsolutePath())) {
            return true;
        }
		
		File parentDir = mainDir.getParentFile();
		
		if (parentDir != null && parentDir.exists() && parentDir.isDirectory()) {
			setDirectory(parentDir);
			return false;
		}
		return true;
	}

    public void remove(int index) {
        listFiles.remove(index);
        notifyDataSetChanged();
    }

}
