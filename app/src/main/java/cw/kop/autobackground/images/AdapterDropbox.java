package cw.kop.autobackground.images;

import android.app.Activity;
import android.graphics.PorterDuff;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.dropbox.client2.DropboxAPI.Entry;

import java.util.ArrayList;
import java.util.List;

import cw.kop.autobackground.R;
import cw.kop.autobackground.settings.AppSettings;

/**
 * Created by TheKeeperOfPie on 4/11/2015.
 */
public class AdapterDropbox extends RecyclerView.Adapter<AdapterDropbox.ViewHolder> {

    private List<Entry> entries;
    private Entry topDir;
    private Entry mainDir;
    private boolean finished;
    private int colorFilterInt;
    private FolderCallback folderCallback;

    public AdapterDropbox(Activity activity, FolderCallback folderCallback) {
        this.folderCallback = folderCallback;
        this.colorFilterInt = AppSettings.getColorFilterInt(activity);
    }

    public void setDirs(Entry topDir, Entry mainDir) {
        this.topDir = topDir;
        this.mainDir = mainDir;
        this.entries = mainDir.contents == null ? new ArrayList<Entry>() : mainDir.contents;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.file_row, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        Entry entry = entries.get(position);

        if (entry.isDir) {
            holder.fileImage.setImageResource(R.drawable.ic_folder_white_24dp);
        }
        else {
            holder.fileImage.setImageResource(R.drawable.ic_insert_drive_file_white_24dp);
        }

        holder.fileTitle.setText(entry.fileName());
        holder.fileSummary.setText(entry.isDir ? "" : entry.size);
    }

    @Override
    public int getItemCount() {
        folderCallback.setEmptyTextVisibility(entries.size() == 0 ? View.VISIBLE : View.INVISIBLE);
        return entries.size();
    }

    public Entry getMainDir() {
        return mainDir;
    }

    public void setDir(Entry dir) {
        entries = dir.contents == null ? new ArrayList<Entry>() : dir.contents;
        mainDir = dir;
        notifyDataSetChanged();
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public Boolean backDirectory() {

        return finished || topDir.path.equals(mainDir.path);

    }

    public Entry getItem(int positionInList) {
        return entries.get(positionInList);
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

            fileImage.setColorFilter(colorFilterInt, PorterDuff.Mode.MULTIPLY);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    folderCallback.onItemClick(getPosition());
                }
            });
        }
    }

}
