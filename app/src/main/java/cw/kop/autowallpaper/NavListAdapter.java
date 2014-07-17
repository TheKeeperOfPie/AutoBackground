package cw.kop.autowallpaper;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import cw.kop.autowallpaper.settings.AppSettings;

/**
 * Created by TheKeeperOfPie on 7/9/2014.
 */
public class NavListAdapter extends BaseAdapter {

    private Activity mainActivity;
    private static LayoutInflater inflater = null;
    private ArrayList<String> fragmentList;
    private int[] lightImages = new int[] {
            R.drawable.ic_action_picture,
            R.drawable.ic_action_web_site,
            R.drawable.ic_action_download,
            R.drawable.ic_action_crop,
            R.drawable.ic_action_storage,
            R.drawable.ic_action_settings,
            R.drawable.ic_action_about};
    private int[] darkImages = new int[] {
            R.drawable.ic_action_picture_dark,
            R.drawable.ic_action_web_site_dark,
            R.drawable.ic_action_download_dark,
            R.drawable.ic_action_crop_dark,
            R.drawable.ic_action_storage_dark,
            R.drawable.ic_action_settings_dark,
            R.drawable.ic_action_about_dark};

    public NavListAdapter(Activity activity, String[] nameArray) {
        mainActivity = activity;
        fragmentList = new ArrayList<String>();
        for (String name : nameArray) {
            fragmentList.add(name);
        }
        inflater = (LayoutInflater)mainActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return fragmentList.size();
    }

    @Override
    public Object getItem(int position) {
        return fragmentList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;

        if (convertView == null) {
            view = inflater.inflate(R.layout.nav_row, null);
        }

        ImageView fragmentImage = (ImageView) view.findViewById(R.id.fragment_image);
        TextView fragmentTitle = (TextView) view.findViewById(R.id.fragment_title);

        if (AppSettings.getTheme() == R.style.AppLightTheme) {
            fragmentImage.setImageResource(lightImages[position]);
        }
        else {
            fragmentImage.setImageResource(darkImages[position]);
        }
        fragmentTitle.setText(fragmentList.get(position));

        return view;
    }
}
