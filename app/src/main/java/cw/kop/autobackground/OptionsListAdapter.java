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

package cw.kop.autobackground;

import android.content.Context;
import android.graphics.PorterDuff;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import cw.kop.autobackground.settings.AppSettings;

public class OptionsListAdapter extends RecyclerView.Adapter<OptionsListAdapter.ViewHolder> {

    private Context appContext;
    private List<OptionData> optionsList;
    private RecyclerViewListClickListener listClickListener;
    private int optionPosition, colorFilterInt;

    public OptionsListAdapter(Context context, List<OptionData> options,
            int position,
            RecyclerViewListClickListener listener) {
        appContext = context;
        optionsList = options;
        optionPosition = position;
        listClickListener = listener;

        colorFilterInt = AppSettings.getColorFilterInt(appContext);

    }

    public void addItem(OptionData data) {
        optionsList.add(data);
        notifyItemInserted(optionsList.size() - 1);
    }

    @Override
    public OptionsListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View rowLayout = LayoutInflater.from(parent.getContext()).inflate(R.layout.notification_settings_row,
                parent,
                false);

        return new ViewHolder(rowLayout);
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, int position) {

        final OptionData optionData = optionsList.get(position);

        viewHolder.optionTitle.setText(optionData.getTitle());
        viewHolder.optionSummary.setText(optionData.getSummary());

        viewHolder.optionIcon.setImageResource(optionData.getDrawable());

        viewHolder.rowLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listClickListener.onClick(viewHolder.getPosition(), optionsList.get(viewHolder.getPosition()).getTitle(),
                        optionsList.get(viewHolder.getPosition()).getDrawable());
            }
        });
    }

    @Override
    public int getItemCount() {
        return optionsList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        protected ImageView optionIcon;
        protected TextView optionTitle;
        protected TextView optionSummary;
        protected View rowLayout;

        public ViewHolder(View rowLayout) {
            super(rowLayout);
            this.rowLayout = rowLayout;
            optionIcon = (ImageView) rowLayout.findViewById(R.id.notification_list_icon);
            optionTitle = (TextView) rowLayout.findViewById(R.id.notification_list_title);
            optionSummary = (TextView) rowLayout.findViewById(R.id.notification_list_summary);
            optionIcon.setColorFilter(colorFilterInt, PorterDuff.Mode.MULTIPLY);
        }

    }
}
