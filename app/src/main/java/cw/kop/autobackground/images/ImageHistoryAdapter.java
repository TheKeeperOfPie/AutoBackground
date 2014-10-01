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
import java.io.FilenameFilter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
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
        AppSettings.checkUsedLinksSize();
        Set<String> usedLinks = AppSettings.getUsedLinks();

        for (String link : usedLinks) {

            long time = 0;
            String url = "Error";

            try {
                url = link.substring(0, link.lastIndexOf("Time:"));
                time = Long.parseLong(link.substring(link.lastIndexOf("Time:") + 5));
            }
            catch (Exception e) {
            }

            historyItems.add(new HistoryItem(time, url, new File(AppSettings.getDownloadPath() + "/HistoryCache/" + time + ".png")));

        }

        Collections.sort(historyItems, new Comparator<HistoryItem>() {
            @Override
            public int compare(HistoryItem lhs, HistoryItem rhs) {
                return (int) (rhs.getTime() - lhs.getTime());
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

            File thumbnailFile = new File(AppSettings.getDownloadPath() + "/HistoryCache/"
                    + historyItems.get(position).getTime() + ".png");

            if (thumbnailFile.exists() && thumbnailFile.isFile()) {
                Picasso.with(parent.getContext())
                        .load(thumbnailFile)
                        .into(fileImage);
            }
            else {
                if (AppSettings.getTheme() == R.style.AppLightTheme) {
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

            fileTitle.setText(DateFormat.getDateTimeInstance().format(new Date(historyItems.get(position).getTime())));
            fileSummary.setText(historyItems.get(position).getUrl());
            return view;
        }
        return null;
    }

    public void clearHistory() {
        historyItems = new ArrayList<HistoryItem>();

        File historyDir = new File(AppSettings.getDownloadPath() + "/HistoryCache");

        FilenameFilter imageFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {

                if (filename.length() > 4 && filename.substring(filename.length() - 4, filename.length()).equals(".png")) {
                    return true;
                }

                return false;
            }
        };

        for (File file : historyDir.listFiles(imageFilter)) {
            if (file.exists() && file.isFile()) {
                file.delete();
            }
        }

        notifyDataSetChanged();
    }

    public void removeItem(HistoryItem item) {
        File file = new File(AppSettings.getDownloadPath() + "/HistoryCache/" + item.getTime() + ".png");
        if (file.exists() && file.isFile()) {
            file.delete();
        }

        historyItems.remove(item);

        notifyDataSetChanged();
    }

    public void saveHistory() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                HashSet<String> usedLinks = new HashSet<String>();

                for (HistoryItem item : historyItems) {
                    usedLinks.add(item.getUrl() + "Time:" + item.getTime());
                }

                AppSettings.setUsedLinks(usedLinks);

            }
        }).start();
    }
}