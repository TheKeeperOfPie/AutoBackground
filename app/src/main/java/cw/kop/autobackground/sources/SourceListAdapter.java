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

package cw.kop.autobackground.sources;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;
import android.widget.TextView;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

import cw.kop.autobackground.R;
import cw.kop.autobackground.downloader.Downloader;
import cw.kop.autobackground.settings.AppSettings;

public class SourceListAdapter extends BaseAdapter {

	private Activity mainActivity;
    private ArrayList<HashMap<String, String>> listData;
    private HashSet<String> titles;
    private LayoutInflater inflater = null;
	
	public SourceListAdapter(Activity activity) {
		mainActivity = activity;
		listData = new ArrayList<HashMap<String, String>>();
        titles = new HashSet<String>();
		inflater = (LayoutInflater) mainActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public int getCount() {
		return listData.size();
	}

	public HashMap<String, String> getItem(int position) {
		return listData.get(position);
	}
	
	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		final HashMap<String, String> listItem = listData.get(position);
		
		View view = convertView;

//        final Context contextThemeWrapper = new ContextThemeWrapper(mainActivity, AppSettings.getTheme());
//
//        LayoutInflater localInflater = inflater.cloneInContext(contextThemeWrapper);

		if (convertView == null) {
			view = inflater.inflate(R.layout.source_list_row, null);
		}
		
		TextView title = (TextView) view.findViewById(R.id.title_text);
		TextView summary = (TextView) view.findViewById(R.id.summary_text);
		TextView num = (TextView) view.findViewById(R.id.num_text);
		final Switch useBox = (Switch) view.findViewById(R.id.use_source_checkbox);
		useBox.setTag(position);
		
		useBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				
				int index = Integer.parseInt(useBox.getTag().toString());

                setActivated(index, isChecked);
				
			}
			
		});

        title.setSelected(true);
        summary.setSelected(true);
		title.setText(listItem.get("title"));
		summary.setText(listItem.get("data"));
		num.setText("# Images: " + listItem.get("num"));
		useBox.setChecked(Boolean.valueOf(listItem.get("use")));
		
		return view;
	}

    public void setActivated(int position, boolean use) {
        HashMap<String, String> changedItem = listData.get(position);
        changedItem.put("use", "" + use);
        listData.set(position, changedItem);
        notifyDataSetChanged();
        saveData();
    }

	public boolean setItem(int position, String type, String title, String data, boolean use, String num) {

        HashMap<String, String> changedItem = listData.get(position);

        if (!changedItem.get("title").replaceAll(" ", "").equals(title.replaceAll(" ", ""))) {
            if (titles.contains(title.replaceAll(" ", ""))) {
                return false;
            }
        }

        titles.remove(changedItem.get("title").replaceAll(" ", ""));
        changedItem.put("type", type);
		changedItem.put("title", title);
		changedItem.put("data", data);
		changedItem.put("num", "" + num);
		changedItem.put("use", "" + use);
		listData.set(position, changedItem);
        titles.add(title.replaceAll(" ", ""));
		notifyDataSetChanged();
        return true;
	}
	
	public boolean addItem(String type, String title, String data, boolean use, String num) {

        if (titles.contains(title.replaceAll(" ", ""))) {
            return false;
        }

        HashMap<String, String> newItem = new HashMap<String, String>();
        newItem.put("type", type);
		newItem.put("title", title);
		newItem.put("data", data);
		newItem.put("num", "" + num);
		newItem.put("use", "" + use);
		listData.add(newItem);
        titles.add(title.replaceAll(" ", ""));
		notifyDataSetChanged();

		Log.i("WLA", "listData" + listData.size());
        return true;
	}
	
	public void removeItem(int position) {
        titles.remove(listData.get(position).get("title").replaceAll(" ", ""));
		listData.remove(position);
		notifyDataSetChanged();
	}

    public void updateNum() {

        FilenameFilter filenameFilter = Downloader.getImageFileNameFilter();

        for (HashMap<String, String> hashMap : listData) {
            if (hashMap.get("type").equals(AppSettings.FOLDER)) {
                File file = new File(hashMap.get("data"));
                if (file.exists() && file.isDirectory()) {
                    hashMap.put("num", "" + file.listFiles(filenameFilter).length);
                }
            }
        }
        notifyDataSetChanged();
        saveData();
    }

    public void sortData(final String key) {

        ArrayList<HashMap<String, String>> sortList = new ArrayList<HashMap<String, String>>();
        sortList.addAll(listData);

        Collections.sort(sortList, new Comparator<HashMap<String, String>>() {
            @Override
            public int compare(HashMap<String, String> lhs, HashMap<String, String> rhs) {

                if (key.equals("use")) {
                    boolean first = Boolean.parseBoolean(lhs.get("use"));
                    boolean second = Boolean.parseBoolean(rhs.get("use"));

                    if (first && second || (!first && !second)) {
                        return lhs.get("title").compareTo(rhs.get("title"));
                    }

                    return first ? -1 : 1;

                }

                if (key.equals("num")) {
                    return Integer.parseInt(lhs.get("num")) - Integer.parseInt(rhs.get("num"));
                }

                return lhs.get(key).compareTo(rhs.get(key));
            }
        });

        if (sortList.equals(listData)) {
            Collections.reverse(sortList);
        }
        listData = sortList;

        notifyDataSetChanged();

    }

	public void saveData() {
		
		AppSettings.setSources(listData);
		
		Log.i("WLA", "SavedListData" + listData.size());
		Log.i("WLA", "Saved Data: " + AppSettings.getNumSources());
	}
	
}
