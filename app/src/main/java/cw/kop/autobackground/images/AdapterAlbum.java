package cw.kop.autobackground.images;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

import cw.kop.autobackground.R;
import cw.kop.autobackground.settings.AppSettings;

/**
 * Created by TheKeeperOfPie on 4/11/2015.
 */
public class AdapterAlbum extends RecyclerView.Adapter<AdapterAlbum.ViewHolder> {

    private int colorFilterInt;
    private Activity activity;
    private List<String> albumNames;
    private List<String> albumImages;
    private List<String> albumLinks;
    private FolderCallback folderCallback;

    public AdapterAlbum(Activity activity, List<String> names, List<String> images,
                        List<String> links, FolderCallback folderCallback) {
        this.activity = activity;
        albumNames = names;
        albumImages = images;
        albumLinks = links;
        this.folderCallback = folderCallback;
        colorFilterInt = AppSettings.getColorFilterInt(activity);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.album_list_cell, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {


        holder.name.setSelected(true);
        holder.name.setText(albumNames.get(position));

        if (Patterns.WEB_URL.matcher(albumImages.get(position)).matches()) {
            Picasso.with(activity).load(albumImages.get(position)).into(holder.icon);
        }
    }

    @Override
    public int getItemCount() {
        folderCallback.setEmptyTextVisibility(albumLinks.size() == 0 ? View.VISIBLE : View.INVISIBLE);
        return albumLinks.size();
    }

    protected class ViewHolder extends RecyclerView.ViewHolder {

        protected final ImageView icon;
        protected final TextView name;

        public ViewHolder(View itemView) {
            super(itemView);
            icon = (ImageView) itemView.findViewById(R.id.album_image);
            name = (TextView) itemView.findViewById(R.id.album_name);
            name.setTextColor(colorFilterInt);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    folderCallback.onItemClick(getPosition());
                }
            });
        }
    }

}
