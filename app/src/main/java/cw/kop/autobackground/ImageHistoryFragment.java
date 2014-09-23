package cw.kop.autobackground;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cw.kop.autobackground.settings.AppSettings;

/**
 * Created by TheKeeperOfPie on 9/21/2014.
 */
public class ImageHistoryFragment extends Fragment {

    private Context appContext;
    private ListView historyListView;

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

        historyListView = new ListView(appContext);

        return historyListView;
//        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        List<String> values = new ArrayList<String>();
        values.addAll(AppSettings.getUsedLinks());

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(appContext, android.R.layout.simple_list_item_1,
                android.R.id.text1, values);

        historyListView.setAdapter(adapter);
    }
}
