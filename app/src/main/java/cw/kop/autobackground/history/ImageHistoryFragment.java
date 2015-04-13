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

package cw.kop.autobackground.history;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AppKeyPair;

import java.text.DateFormat;
import java.util.Date;

import cw.kop.autobackground.DialogFactory;
import cw.kop.autobackground.R;
import cw.kop.autobackground.files.DownloadThread;
import cw.kop.autobackground.images.HistoryItem;
import cw.kop.autobackground.settings.ApiKeys;
import cw.kop.autobackground.settings.AppSettings;

/**
 * Created by TheKeeperOfPie on 9/21/2014.
 */
public class ImageHistoryFragment extends Fragment {

    private Activity activity;
    private RecyclerView recyclerHistory;
    private AdapterHistory adapterHistory;
    private DropboxAPI<AndroidAuthSession> dropboxAPI;
    private Handler handler;
    private TextView emptyText;
    private RecyclerView.AdapterDataObserver adapterDataObserver;
    private RecyclerView.LayoutManager layoutManager;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = activity;
    }

    @Override
    public void onDetach() {
        activity = null;
        super.onDetach();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handler = new Handler(activity.getMainLooper());
        AppKeyPair appKeys = new AppKeyPair(ApiKeys.DROPBOX_KEY, ApiKeys.DROPBOX_SECRET);
        AndroidAuthSession session = new AndroidAuthSession(appKeys);
        dropboxAPI = new DropboxAPI<>(session);
        if (AppSettings.useDropboxAccount() && !TextUtils.isEmpty(AppSettings.getDropboxAccountToken())) {
            dropboxAPI.getSession().setOAuth2AccessToken(AppSettings.getDropboxAccountToken());
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.image_history_layout, container, false);
        view.setBackgroundResource(AppSettings.getBackgroundColorResource());

        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        if (displayMetrics.heightPixels > displayMetrics.widthPixels) {
            layoutManager = new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL,
                    false);
        }
        else {
            layoutManager = new GridLayoutManager(activity, 2, LinearLayoutManager.VERTICAL, false);
        }

        recyclerHistory = (RecyclerView) view.findViewById(R.id.recycler_history);
        recyclerHistory.setHasFixedSize(true);
        recyclerHistory.setLayoutManager(layoutManager);

        emptyText = (TextView) view.findViewById(R.id.empty_text);

//        if (AppSettings.getTheme() == R.style.AppLightTheme) {
//            recyclerHistory.setBackgroundColor(getResources().getColor(R.color.WHITE_OPAQUE));
//            emptyLayout.setBackgroundColor(getResources().getColor(R.color.WHITE_OPAQUE));
//        }
//        else {
//            recyclerHistory.setBackgroundColor(getResources().getColor(R.color.BLACK_OPAQUE));
//            emptyLayout.setBackgroundColor(getResources().getColor(R.color.BLACK_OPAQUE));
//        }

        ImageView clearHistoryButton = (ImageView) view.findViewById(R.id.clear_history_button);
        clearHistoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showClearHistoryDialog();
            }
        });
        clearHistoryButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(activity, "Clear history", Toast.LENGTH_LONG).show();
                return true;
            }
        });

        if (adapterHistory == null) {
            adapterHistory = new AdapterHistory(activity,
                    new AdapterHistory.HistoryItemClickListener() {
                        @Override
                        public void onItemClick(int position) {
                            showHistoryItemDialog(adapterHistory.getItem(position));
                        }
                    });
            adapterDataObserver = new RecyclerView.AdapterDataObserver() {
                @Override
                public void onChanged() {
                    super.onChanged();
                    emptyText.setVisibility(adapterHistory.getItemCount() == 0 ? View.VISIBLE : View.GONE);
                }
            };
        }

        recyclerHistory.setAdapter(adapterHistory);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        adapterHistory.registerAdapterDataObserver(adapterDataObserver);
        emptyText.setVisibility(adapterHistory.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onPause() {
        adapterHistory.unregisterAdapterDataObserver(adapterDataObserver);
        adapterHistory.saveHistory();
        super.onPause();
    }

    private void showClearHistoryDialog() {

        DialogFactory.ActionDialogListener clickListener = new DialogFactory.ActionDialogListener() {
            @Override
            public void onClickRight(View v) {
                AppSettings.clearUsedLinks();
                adapterHistory.clearHistory();
                this.dismissDialog();
            }
        };

        DialogFactory.showActionDialog(activity,
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

                        if (item.getUrl().contains(DownloadThread.DROPBOX_FILE_PREFIX)) {

                            if (!dropboxAPI.getSession().isLinked()) {
                                Toast.makeText(activity, "Dropbox isn't connected", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Intent dropboxIntent = new Intent(Intent.ACTION_VIEW);
                                        String url = dropboxAPI.media(item.getUrl().substring(DownloadThread.DROPBOX_FILE_PREFIX.length()), true).url;
                                        dropboxIntent.setData(Uri.parse(url));
                                        activity.startActivity(dropboxIntent);
                                    }
                                    catch (DropboxException e) {
                                        handler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                Toast.makeText(activity, "Error loading Dropbox link", Toast.LENGTH_LONG).show();
                                            }
                                        });
                                    }
                                }
                            }).start();
                        }
                        else {
                            intent.setData(Uri.parse(item.getUrl()));
                            activity.startActivity(intent);
                        }
                        break;
                    case 1:
                        adapterHistory.removeItem(item);
                        break;
                    default:
                }
                dismissDialog();
            }
        };

        DialogFactory.showListDialog(activity,
                DateFormat.getDateTimeInstance().format(new Date(item.getTime())),
                clickListener,
                R.array.history_menu);
    }

}