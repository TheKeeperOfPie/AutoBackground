package cw.kop.autobackground.images;

import android.app.Activity;
import android.graphics.PorterDuff;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.api.services.drive.model.File;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import cw.kop.autobackground.R;
import cw.kop.autobackground.settings.AppSettings;

/**
 * Created by TheKeeperOfPie on 4/11/2015.
 */
public class AdapterDrive extends RecyclerView.Adapter<AdapterDrive.ViewHolder> {

    private static final int BYTE_TO_MEBIBYTE = 1048576;
    private static final String TAG = AdapterDrive.class.getCanonicalName();
    private List<File> entries;
    private File mainDir;
    private File topDir;
    private int colorFilterInt;
    private boolean finished;
    private FolderCallback folderCallback;

    public AdapterDrive(Activity activity, FolderCallback folderCallback) {
        this.folderCallback = folderCallback;
        colorFilterInt = AppSettings.getColorFilterInt(activity);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.file_row, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        File file = entries.get(position);

        if (file.getMimeType().equals("application/vnd.google-apps.folder")) {
            holder.fileImage.setImageResource(R.drawable.ic_folder_white_24dp);
            holder.fileSummary.setText(file.getDescription());
        }
        else {
            holder.fileImage.setImageResource(R.drawable.ic_insert_drive_file_white_24dp);
            holder.fileSummary.setText(file.getFileSize() != null ? String.valueOf(file.getFileSize() / BYTE_TO_MEBIBYTE) + " MiB" : null);
        }

        holder.fileTitle.setText(file.getTitle());
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    public File getMainDir() {
        return mainDir;
    }


    public void setDirs(File topDir, File mainDir, List<File> entries) {
        this.topDir = topDir;
        setDir(mainDir, entries);
    }

    public void setDir(File mainDir, List<File> entries) {
        this.mainDir = mainDir;
        this.entries = entries;
        Collections.sort(entries,
                new Comparator<File>() {
                    @Override
                    public int compare(com.google.api.services.drive.model.File lhs,
                                       com.google.api.services.drive.model.File rhs) {

                        if (lhs.getMimeType()
                                .equals("application/vnd.google-apps.folder") ^
                                rhs.getMimeType()
                                        .equals("application/vnd.google-apps.folder")) {
                            return lhs.getMimeType()
                                    .equals("application/vnd.google-apps.folder") ? -1 : 1;
                        }

                        return lhs.getTitle()
                                .compareTo(rhs.getTitle());
                    }
                });
        notifyDataSetChanged();
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public Boolean backDirectory() {
        return finished || topDir.getId().equals(mainDir.getId());
    }

    public File getItem(int positionInList) {
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
