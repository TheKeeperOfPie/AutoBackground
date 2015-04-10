package cw.kop.autobackground.images;

import android.app.Activity;
import android.content.Context;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.dropbox.client2.DropboxAPI;
import com.google.android.gms.common.data.DataBuffer;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.widget.DataBufferAdapter;

import java.util.ArrayList;
import java.util.List;

import cw.kop.autobackground.R;
import cw.kop.autobackground.settings.AppSettings;

/**
 * Created by TheKeeperOfPie on 4/4/2015.
 */
public class DriveAdapter extends BaseAdapter {

    private List<Metadata> entries;
    private DriveFolder mainDir;
    private DriveFolder topDir;
    private LayoutInflater inflater;
    private int colorFilterInt;
    private boolean finished;

    public DriveAdapter(Activity activity) {
        inflater = activity.getLayoutInflater();
        colorFilterInt = AppSettings.getColorFilterInt(activity);
    }

    public void setDirs(DriveFolder topDir, DriveFolder mainDir, List<Metadata> entries) {
        this.topDir = topDir;
        this.mainDir = mainDir;
        this.entries = entries;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return entries.size();
    }

    @Override
    public Object getItem(int position) {
        return entries.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Metadata metadata = entries.get(position);

        TextView fileTitle;
        TextView fileSummary;
        ImageView fileImage;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.file_row, parent, false);

            fileTitle = (TextView) convertView.findViewById(R.id.file_title);
            fileSummary = (TextView) convertView.findViewById(R.id.file_summary);
            fileImage = (ImageView) convertView.findViewById(R.id.file_image);

            fileImage.setColorFilter(colorFilterInt, PorterDuff.Mode.MULTIPLY);

            convertView.setTag(new ViewHolder(fileTitle, fileSummary, fileImage));
        }

        ViewHolder viewHolder = (ViewHolder) convertView.getTag();
        fileTitle = viewHolder.fileTitle;
        fileSummary = viewHolder.fileSummary;
        fileImage = viewHolder.fileImage;

        if (metadata.isFolder()) {
            fileImage.setImageResource(R.drawable.ic_folder_white_24dp);
        }
        else {
            fileImage.setImageResource(R.drawable.ic_insert_drive_file_white_24dp);
        }

        fileTitle.setText(metadata.getTitle());
        fileSummary.setText(metadata.isFolder() ? "" : "" + metadata.getFileSize());

        return convertView;
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

    private class ViewHolder {

        protected final TextView fileTitle;
        protected final TextView fileSummary;
        protected final ImageView fileImage;

        public ViewHolder(TextView fileTitle,
                          TextView fileSummary,
                          ImageView fileImage) {
            this.fileTitle = fileTitle;
            this.fileSummary = fileSummary;
            this.fileImage = fileImage;
        }
    }
}
