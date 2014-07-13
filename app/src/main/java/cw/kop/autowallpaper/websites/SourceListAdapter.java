package cw.kop.autowallpaper.websites;

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
import java.util.HashMap;

import cw.kop.autowallpaper.R;
import cw.kop.autowallpaper.settings.AppSettings;

public class SourceListAdapter extends BaseAdapter {

	private Activity mainActivity;
    private ArrayList<HashMap<String, String>> listData;
    private static LayoutInflater inflater = null;
	
	public SourceListAdapter(Activity activity) {
		mainActivity = activity;
		listData = new ArrayList<HashMap<String, String>>();
		inflater = (LayoutInflater)mainActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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

        final Context contextThemeWrapper = new ContextThemeWrapper(mainActivity, AppSettings.getTheme());

        LayoutInflater localInflater = inflater.cloneInContext(contextThemeWrapper);

		if (convertView == null) {
			view = localInflater.inflate(R.layout.website_list_row, null);
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
				
				setItem(index, listData.get(index).get("type"), listData.get(index).get("title"), listData.get(index).get("data"), isChecked, listData.get(index).get("num"));
				notifyDataSetChanged();
				
			}
			
		});
		
		title.setText(listItem.get("title"));
		summary.setText(listItem.get("data"));
		num.setText("# Images: " + listItem.get("num"));
		useBox.setChecked(Boolean.valueOf(listItem.get("use")));
		
		return view;
	}

	public void setItem(int position, String type, String title, String data, boolean use, String num) {
		HashMap<String, String> changedItem = new HashMap<String, String>();
        changedItem.put("type", type);
		changedItem.put("title", title);
		changedItem.put("data", data);
		changedItem.put("num", "" + num);
		changedItem.put("use", "" + use);
		listData.set(position, changedItem);
		notifyDataSetChanged();
	}
	
	public void addItem(String type, String title, String data, boolean use, String num) {
		HashMap<String, String> newItem = new HashMap<String, String>();
        newItem.put("type", type);
		newItem.put("title", title);
		newItem.put("data", data);
		newItem.put("num", "" + num);
		newItem.put("use", "" + use);
		listData.add(newItem);
		notifyDataSetChanged();

		Log.i("WLA", "listData" + listData.size());
	}
	
	public void removeItem(int position) {
		listData.remove(position);
		notifyDataSetChanged();
	}
	
	public void saveData() {
		
		AppSettings.setSources(listData);
		
		Log.i("WLA", "SavedListData" + listData.size());
		Log.i("WLA", "Saved Data: " + AppSettings.getNumSources());
	}
	
}
