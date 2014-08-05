package cw.kop.autobackground.images;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.regex.Pattern;

import cw.kop.autobackground.R;

/**
 * Created by TheKeeperOfPie on 8/4/2014.
 */
public class AlbumAdapter extends BaseAdapter {

    private Context context;

    private ArrayList<String> albumNames;
    private ArrayList<String> albumImages;
    private ArrayList<String> albumLinks;


    public AlbumAdapter(Context context, ArrayList<String> names, ArrayList<String> images, ArrayList<String> links) {
        this.context = context;
        albumNames = names;
        albumImages = images;
        albumLinks = links;
    }

    @Override
    public int getCount() {
        return albumLinks.size();
    }

    @Override
    public Object getItem(int position) {
        return albumLinks.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (albumLinks.size() > 0) {

            View view = convertView;

            if (convertView == null) {
                Log.i("AA", "Try inflate");
                LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.album_list_cell, null);
                Log.i("AA", "Finished inflate");
            }

            ImageView icon = (ImageView) view.findViewById(R.id.album_image);
            TextView name = (TextView) view.findViewById(R.id.album_name);

            name.setSelected(true);
            name.setText(albumNames.get(position));

            if (Patterns.WEB_URL.matcher(albumImages.get(position)).matches()) {
                Picasso.with(context).load(albumImages.get(position)).into(icon);
            }

            return view;
        }
        return null;
    }
}
