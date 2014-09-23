package cw.kop.autobackground.images;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cw.kop.autobackground.R;
import cw.kop.autobackground.downloader.Downloader;
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.image_history_layout, container, false);

        historyListView = (ListView) view.findViewById(R.id.history_listview);

        Button clearHistoryButton = (Button) view.findViewById(R.id.clear_history_button);
        clearHistoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showClearHistoryDialog();
            }
        });

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
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(historyAdapter.getItem(position).getUrl()));
                appContext.startActivity(intent);
            }
        });
    }

    private void showClearHistoryDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(appContext);

        builder.setTitle("Clear History?");

        builder.setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                AppSettings.clearUsedLinks();
                historyAdapter.clearHistory();
            }
        });
        builder.setNegativeButton(R.string.cancel_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
            }
        });

        builder.show();

    }

}
