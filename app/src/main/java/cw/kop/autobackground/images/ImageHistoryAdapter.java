package cw.kop.autobackground.images;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Set;

import cw.kop.autobackground.R;
import cw.kop.autobackground.settings.AppSettings;

/**
 * Created by TheKeeperOfPie on 9/22/2014.
 */
public class ImageHistoryAdapter  extends BaseAdapter {

    private Context mainActivity;
    private LayoutInflater inflater;
    private ArrayList<HistoryItem> historyItems;

    public ImageHistoryAdapter(Context activity) {

        mainActivity = activity;
        inflater = (LayoutInflater) mainActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        historyItems = new ArrayList<HistoryItem>();
        Set<String> usedLinks = AppSettings.getUsedLinks();

        for (String link : usedLinks) {

            int index = -1;
            long time = 0;
            String url = "Possible corruption", title = "Error";

            try {
                index = Integer.parseInt("" + link.charAt(link.length() - 1));
                url = link.substring(0, link.lastIndexOf("Time:"));
                time = Long.parseLong(link.substring(link.lastIndexOf("Time:") + 5, link.lastIndexOf("Order:")));
                title = DateFormat.getDateTimeInstance().format(new Date(time));
            }
            catch (Exception e) {
            }

            historyItems.add(new HistoryItem(index, title, url, new File(AppSettings.getDownloadPath() + "/HistoryCache/" + time + ".png")));

        }

        Collections.sort(historyItems, new Comparator<HistoryItem>() {
            @Override
            public int compare(HistoryItem lhs, HistoryItem rhs) {
                return lhs.getIndex() - rhs.getIndex();
            }
        });

    }

    @Override
    public int getCount() {
        return historyItems.size();
    }

    @Override
    public HistoryItem getItem(int position) {
        return historyItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (historyItems.size() > 0) {
            View view = convertView;

            if (convertView == null) {
                view = inflater.inflate(R.layout.image_list_cell, null);
            }

            TextView fileTitle = (TextView) view.findViewById(R.id.file_title);
            TextView fileSummary = (TextView) view.findViewById(R.id.file_summary);
            ImageView fileImage = (ImageView) view.findViewById(R.id.file_image);

            Picasso.with(mainActivity.getApplicationContext())
                    .load(R.drawable.ic_action_collection)
                    .into(fileImage);

            fileTitle.setText(historyItems.get(position).getName());
            fileSummary.setText(historyItems.get(position).getUrl());
            return view;
        }
        return null;
    }

    public void clearHistory() {
        historyItems = new ArrayList<HistoryItem>();
        notifyDataSetChanged();
    }
}
