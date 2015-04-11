package cw.kop.autobackground.images;

import android.app.Activity;
import android.graphics.PorterDuff;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.Metadata;

import java.util.List;

import cw.kop.autobackground.R;
import cw.kop.autobackground.settings.AppSettings;

/**
 * Created by TheKeeperOfPie on 4/11/2015.
 */
public class AdapterDrive extends RecyclerView.Adapter<AdapterDrive.ViewHolder> {

    private List<Metadata> entries;
    private DriveFolder mainDir;
    private DriveFolder topDir;
    private int colorFilterInt;
    private boolean finished;
    private FolderCallback folderCallback;

    public AdapterDrive(Activity activity, FolderCallback folderCallback) {
        this.folderCallback = folderCallback;
        colorFilterInt = AppSettings.getColorFilterInt(activity);
    }

    public void setDirs(DriveFolder topDir, DriveFolder mainDir, List<Metadata> entries) {
        this.topDir = topDir;
        this.mainDir = mainDir;
        this.entries = entries;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.file_row, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Metadata metadata = entries.get(position);

        if (metadata.isFolder()) {
            holder.fileImage.setImageResource(R.drawable.ic_folder_white_24dp);
        }
        else {
            holder.fileImage.setImageResource(R.drawable.ic_insert_drive_file_white_24dp);
        }

        holder.fileTitle.setText(metadata.getTitle());
        holder.fileSummary.setText(metadata.isFolder() ? "" : "" + metadata.getFileSize());
    }

    @Override
    public int getItemCount() {
        folderCallback.setEmptyTextVisibility(entries.size() == 0 ? View.VISIBLE : View.INVISIBLE);
        return entries.size();
    }

    public DriveFolder getMainDir() {
        return mainDir;
    }

    public void setDir(DriveFolder mainDir, List<Metadata> entries) {
        this.mainDir = mainDir;
        this.entries = entries;
        notifyDataSetChanged();
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public Boolean backDirectory() {
        return finished || topDir.getDriveId().equals(mainDir.getDriveId());
    }

    public Metadata getItem(int positionInList) {
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
