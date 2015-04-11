package cw.kop.autobackground.images;

import android.app.Activity;
import android.graphics.PorterDuff;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import cw.kop.autobackground.R;
import cw.kop.autobackground.files.FileHandler;
import cw.kop.autobackground.settings.AppSettings;

/**
 * Created by TheKeeperOfPie on 4/11/2015.
 */
public class AdapterImages extends RecyclerView.Adapter<AdapterImages.ViewHolder> {

    private static final int BYTE_TO_MEBIBYTE = 1048576;
    private File currentDir;
    private FolderCallback folderCallback;
    private File topDir;
    private Activity activity;
    private ArrayList<File> listFiles;
    private boolean finish;
    private int colorFilterInt;
    private float sideMarginPixels;

    public AdapterImages(Activity activity, File topDir, File startDir, FolderCallback folderCallback) {
        this.activity = activity;
        this.topDir = topDir;
        this.currentDir = startDir;
        this.folderCallback = folderCallback;
        listFiles = new ArrayList<>();
        setDirectory(startDir);
        colorFilterInt = AppSettings.getColorFilterInt(activity);
        sideMarginPixels = activity.getResources().getDimensionPixelSize(
                R.dimen.side_margin);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.file_row, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        File file = listFiles.get(position);

        boolean isImage = FileHandler.getImageFileNameFilter().accept(null, file.getName());

        if (isImage) {
            holder.fileImageFull.setVisibility(View.VISIBLE);
            holder.fileTitle.setVisibility(View.GONE);
            holder.fileSummary.setVisibility(View.GONE);
            holder.fileImage.setVisibility(View.GONE);

            holder.fileImage.clearColorFilter();

            holder.fileImageFull.getLayoutParams().height = (int) ((folderCallback.getItemWidth() - 2f * sideMarginPixels) / 16f * 9);
            holder.fileImageFull.requestLayout();

            Picasso.with(activity)
                    .load(file)
                    .fit()
                    .centerCrop()
                    .into(holder.fileImageFull);
        }
        else {

            holder.fileImageFull.setVisibility(View.GONE);
            holder.fileTitle.setVisibility(View.VISIBLE);
            holder.fileSummary.setVisibility(View.VISIBLE);
            holder.fileImage.setVisibility(View.VISIBLE);

            holder.fileImage.setColorFilter(colorFilterInt, PorterDuff.Mode.MULTIPLY);

            if (file.isDirectory()) {
                holder.fileImage.setImageResource(R.drawable.ic_folder_white_24dp);
            }
            else {
                holder.fileImage.setImageResource(R.drawable.ic_insert_drive_file_white_24dp);
            }

            holder.fileTitle.setText(file.getName());
            holder.fileSummary.setText(
                    (file.isDirectory() && file.list() != null) ? file.list().length + " Files" :
                            "" + (file.length() / BYTE_TO_MEBIBYTE) + " MiB");
        }
    }

    @Override
    public int getItemCount() {
        folderCallback.setEmptyTextVisibility(listFiles.size() == 0 ? View.VISIBLE : View.INVISIBLE);
        return listFiles.size();
    }

    public File getDirectory() {
        return currentDir;
    }

    public void setDirectory(File selectedFile) {

        if (selectedFile != null && selectedFile.isDirectory()) {
            currentDir = selectedFile;

            ArrayList<File> folders = new ArrayList<>();
            ArrayList<File> files = new ArrayList<>();

            if (selectedFile.listFiles() != null) {
                for (File file : selectedFile.listFiles()) {
                    if (file != null && file.exists()) {
                        if (file.isDirectory()) {
                            folders.add(file);
                        }
                        else {
                            files.add(file);
                        }
                    }
                }
            }

            if (folders.size() > 0) {
                Collections.sort(folders, new Comparator<File>() {
                    @Override
                    public int compare(File lhs, File rhs) {
                        return lhs.getName()
                                .compareToIgnoreCase(rhs.getName());
                    }
                });
            }

            if (files.size() > 0) {
                Collections.sort(files, new Comparator<File>() {
                    @Override
                    public int compare(File lhs, File rhs) {
                        return lhs.getName().compareToIgnoreCase(rhs.getName());
                    }
                });
            }

            folders.addAll(files);

            listFiles = folders;
            notifyDataSetChanged();
        }

    }

    public boolean isFinished() {
        return finish;
    }

    public void setFinished() {
        finish = true;
    }

    public Boolean backDirectory() {

        if (finish || currentDir.getAbsolutePath().equals(topDir.getAbsolutePath())) {
            return true;
        }

        File parentDir = currentDir.getParentFile();

        if (parentDir != null && parentDir.exists() && parentDir.isDirectory()) {
            setDirectory(parentDir);
            return false;
        }
        return true;
    }

    public void remove(int index) {
        listFiles.remove(index);
        notifyDataSetChanged();
    }

    public File getItem(int positionInList) {
        return listFiles.get(positionInList);
    }

    protected class ViewHolder extends RecyclerView.ViewHolder {

        protected final TextView fileTitle;
        protected final TextView fileSummary;
        protected final ImageView fileImage;
        protected final ImageView fileImageFull;

        public ViewHolder(View itemView) {
            super(itemView);

            fileTitle = (TextView) itemView.findViewById(R.id.file_title);
            fileSummary = (TextView) itemView.findViewById(R.id.file_summary);
            fileImage = (ImageView) itemView.findViewById(R.id.file_image);
            fileImageFull = (ImageView) itemView.findViewById(R.id.file_image_full);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    folderCallback.onItemClick(getPosition());
                }
            });
        }
    }

}
