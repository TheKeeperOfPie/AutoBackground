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

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;

import cw.kop.autobackground.DialogFactory;
import cw.kop.autobackground.R;
import cw.kop.autobackground.settings.AppSettings;

/**
 * Created by TheKeeperOfPie on 9/21/2014.
 */
public class ImageHistoryFragment extends Fragment {

    private Context appContext;
    private ListView historyListView;
    private ImageHistoryAdapter historyAdapter;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        appContext = activity;
    }

    @Override
    public void onDetach() {
        appContext = null;
        super.onDetach();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.image_history_layout, container, false);

        historyListView = (ListView) view.findViewById(R.id.history_listview);

        TextView emptyText = new TextView(appContext);
        emptyText.setText("History is empty");
        emptyText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        emptyText.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        emptyText.setGravity(Gravity.CENTER_HORIZONTAL);

        LinearLayout emptyLayout = new LinearLayout(appContext);
        emptyLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        emptyLayout.setGravity(Gravity.TOP);
        emptyLayout.addView(emptyText);

//        if (AppSettings.getTheme() == R.style.AppLightTheme) {
//            historyListView.setBackgroundColor(getResources().getColor(R.color.WHITE_OPAQUE));
//            emptyLayout.setBackgroundColor(getResources().getColor(R.color.WHITE_OPAQUE));
//        }
//        else {
//            historyListView.setBackgroundColor(getResources().getColor(R.color.BLACK_OPAQUE));
//            emptyLayout.setBackgroundColor(getResources().getColor(R.color.BLACK_OPAQUE));
//        }

        Button clearHistoryButton = (Button) view.findViewById(R.id.clear_history_button);
        clearHistoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showClearHistoryDialog();
            }
        });

        ((ViewGroup) historyListView.getParent()).addView(emptyLayout, 0);

        historyListView.setEmptyView(emptyLayout);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (historyAdapter == null) {
            historyAdapter = new ImageHistoryAdapter(appContext);
        }

        historyListView.setAdapter(historyAdapter);
        historyListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                showHistoryItemDialog(historyAdapter.getItem(position));
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        historyAdapter.saveHistory();
        super.onPause();
    }

    private void showClearHistoryDialog() {

        DialogFactory.ActionDialogListener clickListener = new DialogFactory.ActionDialogListener() {
            @Override
            public void onClickRight(View v) {
                AppSettings.clearUsedLinks();
                historyAdapter.clearHistory();
                this.dismissDialog();
            }
        };

        DialogFactory.showActionDialog(appContext,
                "Clear History?",
                "This cannot be undone.",
                clickListener,
                -1,
                R.string.cancel_button,
                R.string.ok_button);

    }

    private void showHistoryItemDialog(final HistoryItem item) {

        DialogFactory.ListDialogListener clickListener = new DialogFactory.ListDialogListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(item.getUrl()));
                        appContext.startActivity(intent);
                        break;
                    case 1:
                        historyAdapter.removeItem(item);
                        break;
                    default:
                }
                dismissDialog();
            }
        };

        DialogFactory.showListDialog(appContext,
                DateFormat.getDateTimeInstance().format(new Date(item.getTime())),
                clickListener,
                R.array.history_menu);
    }

}