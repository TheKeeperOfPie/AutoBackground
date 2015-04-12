package cw.kop.autobackground.history;

import android.app.Activity;
import android.graphics.PorterDuff;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FilenameFilter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import cw.kop.autobackground.R;
import cw.kop.autobackground.images.HistoryItem;
import cw.kop.autobackground.settings.AppSettings;

/**
 * Created by TheKeeperOfPie on 4/11/2015.
 */
public class AdapterHistory extends RecyclerView.Adapter<AdapterHistory.ViewHolder> {

    private static final String TAG = AdapterHistory.class.getCanonicalName();
    private Activity activity;
    private HistoryItemClickListener clickListener;
    private ArrayList<HistoryItem> historyItems;
    private int colorFilterInt;

    public AdapterHistory(Activity activity, HistoryItemClickListener clickListener) {
        this.activity = activity;
        this.clickListener = clickListener;
        colorFilterInt = AppSettings.getColorFilterInt(activity);

        historyItems = new ArrayList<>();
        AppSettings.checkUsedLinksSize();
        Set<String> usedLinks = AppSettings.getUsedLinks();

        Log.d(TAG, "usedLinks size: " + usedLinks.size());

        for (String link : usedLinks) {

            long time = 0;
            String url = "Error";

            try {
                url = link.substring(0, link.lastIndexOf("Time:"));
                time = Long.parseLong(link.substring(link.lastIndexOf("Time:") + 5));
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            historyItems.add(new HistoryItem(time,
                    url,
                    new File(AppSettings.getDownloadPath() + "/HistoryCache/" + time + ".png")));

        }

        Collections.sort(historyItems);
        Log.d(TAG, "historyItems size: " + historyItems.size());
        for (HistoryItem item : historyItems) {
            Log.d(TAG, "HistoryItem: " + item.toString());
        }

    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.file_row, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        File thumbnailFile = new File(AppSettings.getDownloadPath() + "/HistoryCache/"
                + historyItems.get(position).getTime() + ".png");

        if (thumbnailFile.exists() && thumbnailFile.isFile()) {
            holder.fileImage.clearColorFilter();
            Picasso.with(activity)
                    .load(thumbnailFile)
                    .into(holder.fileImage);
        }
        else {
            holder.fileImage.setColorFilter(colorFilterInt, PorterDuff.Mode.MULTIPLY);
            holder.fileImage.setImageResource(R.drawable.ic_insert_drive_file_white_24dp);
        }

        holder.fileTitle.setText(DateFormat.getDateTimeInstance().format(new Date(historyItems.get(
                position).getTime())));
        holder.fileSummary.setText(historyItems.get(position).getUrl());
    }

    @Override
    public int getItemCount() {
        return historyItems.size();
    }

    public HistoryItem getItem(int position) {
        return historyItems.get(position);
    }

    public void clearHistory() {
        historyItems = new ArrayList<>();

        File historyDir = new File(AppSettings.getDownloadPath() + "/HistoryCache");

        historyDir.mkdirs();

        FilenameFilter imageFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {

                return filename.endsWith(".png");

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
                HashSet<String> usedLinks = new HashSet<>();

                for (HistoryItem item : historyItems) {
                    usedLinks.add(item.getUrl() + "Time:" + item.getTime());
                }

                AppSettings.setUsedLinks(usedLinks);

            }
        }).start();
    }

    protected class ViewHolder extends RecyclerView.ViewHolder {

        protected final TextView fileTitle;
        protected final TextView fileSummary;
        protected final ImageView fileImage;

        public ViewHolder(View itemView) {
            super(itemView);

            fileTitle = (TextView) itemView.findViewById(R.id.file_title);
            fileSummary = (TextView) itemView.findViewById(R.id.file_summary);
            fileImage = (ImageView) itemView.findViewById(R.id.file_image);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    clickListener.onItemClick(getPosition());
                }
            });
        }
    }

    public interface HistoryItemClickListener {
        void onItemClick(int position);
    }

}
