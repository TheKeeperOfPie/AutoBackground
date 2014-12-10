/*
 * Copyright (C) Winson Chiu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cw.kop.autobackground.images;

import android.content.Context;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import cw.kop.autobackground.R;

public class AlbumAdapter extends BaseAdapter {

    private static final long ENTRY_ANIMATION_TIME = 200l;
    private static final float ENTRY_ANIMATION_Y = 500f;
    private ArrayList<String> albumNames;
    private ArrayList<String> albumImages;
    private ArrayList<String> albumLinks;
    private LayoutInflater inflater;
    private int lastPosition = -1;

    public AlbumAdapter(Context context, ArrayList<String> names, ArrayList<String> images,
            ArrayList<String> links) {
        albumNames = names;
        albumImages = images;
        albumLinks = links;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
                view = inflater.inflate(R.layout.album_list_cell, parent, false);
            }

            ImageView icon = (ImageView) view.findViewById(R.id.album_image);
            TextView name = (TextView) view.findViewById(R.id.album_name);

            name.setSelected(true);
            name.setText(albumNames.get(position));

            if (Patterns.WEB_URL.matcher(albumImages.get(position)).matches()) {
                Picasso.with(parent.getContext()).load(albumImages.get(position)).into(icon);
            }

            if (position > 0 && lastPosition <= position) {
                float initialTranslation = ENTRY_ANIMATION_Y;
                lastPosition = position;

                view.setTranslationY(initialTranslation);
                view.animate()
                        .setInterpolator(new DecelerateInterpolator(1.0f))
                        .translationY(0f)
                        .setDuration(ENTRY_ANIMATION_TIME)
                        .setListener(null);
            }

            return view;
        }
        return null;
    }
}
