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
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
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

        TextView emptyText = new TextView(appContext);
        emptyText.setText("History is empty");
        emptyText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        emptyText.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        emptyText.setGravity(Gravity.CENTER_HORIZONTAL);

        LinearLayout emptyLayout = new LinearLayout(appContext);
        emptyLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
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

    private void showHistoryItemDialog(final HistoryItem item) {

        AlertDialog.Builder builder = new AlertDialog.Builder(appContext);

        builder.setTitle(DateFormat.getDateTimeInstance().format(new Date(item.getTime())));

        builder.setItems(R.array.history_menu, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
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
            }
        });

        builder.show();

    }

}