package cw.kop.autobackground.sources;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;

import cw.kop.autobackground.R;
import cw.kop.autobackground.settings.AppSettings;

public class SourceListAdapter extends BaseAdapter {

	private Activity mainActivity;
    private ArrayList<TreeMap<String, String>> listData;
    private HashSet<String> titles;
    private static LayoutInflater inflater = null;
	
	public SourceListAdapter(Activity activity) {
		mainActivity = activity;
		listData = new ArrayList<TreeMap<String, String>>();
        titles = new HashSet<String>();
		inflater = (LayoutInflater)mainActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public int getCount() {
		return listData.size();
	}

	public TreeMap<String, String> getItem(int position) {
		return listData.get(position);
	}
	
	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		final TreeMap<String, String> listItem = listData.get(position);
		
		View view = convertView;

        final Context contextThemeWrapper = new ContextThemeWrapper(mainActivity, AppSettings.getTheme());

        LayoutInflater localInflater = inflater.cloneInContext(contextThemeWrapper);

		if (convertView == null) {
			view = localInflater.inflate(R.layout.source_list_row, null);
		}
		
		TextView title = (TextView) view.findViewById(R.id.title_text);
		TextView summary = (TextView) view.findViewById(R.id.summary_text);
		TextView num = (TextView) view.findViewById(R.id.num_text);
		final Switch useBox = (Switch) view.findViewById(R.id.use_website_checkbox);
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
        TreeMap<String, String> changedItem = listData.get(position);
        changedItem.put("use", "" + use);
        listData.set(position, changedItem);
        notifyDataSetChanged();
        saveData();
    }

	public boolean setItem(int position, String type, String title, String data, boolean use, String num) {

        TreeMap<String, String> changedItem = listData.get(position);

        if (!changedItem.get("title").equals(title)) {
            if (titles.contains(title)) {
                return false;
            }
        }

        changedItem.put("type", type);
		changedItem.put("title", title);
		changedItem.put("data", data);
		changedItem.put("num", "" + num);
		changedItem.put("use", "" + use);
		listData.set(position, changedItem);
        titles.add(title);
		notifyDataSetChanged();
        return true;
	}
	
	public boolean addItem(String type, String title, String data, boolean use, String num) {

        if (titles.contains(title)) {
            return false;
        }

        TreeMap<String, String> newItem = new TreeMap<String, String>();
        newItem.put("type", type);
		newItem.put("title", title);
		newItem.put("data", data);
		newItem.put("num", "" + num);
		newItem.put("use", "" + use);
		listData.add(newItem);
        titles.add(title);
		notifyDataSetChanged();

		Log.i("WLA", "listData" + listData.size());
        return true;
	}
	
	public void removeItem(int position) {
        titles.remove(listData.get(position).get("title"));
		listData.remove(position);
		notifyDataSetChanged();
	}

    public void sortData(final String key) {

        ArrayList<TreeMap<String, String>> sortList = new ArrayList<TreeMap<String, String>>();
        sortList.addAll(listData);

        Collections.sort(sortList, new Comparator<TreeMap<String, String>>() {
            @Override
            public int compare(TreeMap<String, String> lhs, TreeMap<String, String> rhs) {

                if (key.equals("use")) {
                    boolean first = Boolean.parseBoolean(lhs.get("use"));
                    boolean second = Boolean.parseBoolean(rhs.get("use"));

                    if (first && second) {
                        return 0;
                    }
                    if (first) {
                        return -1;
                    }
                    if (second) {
                        return 1;
                    }

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
