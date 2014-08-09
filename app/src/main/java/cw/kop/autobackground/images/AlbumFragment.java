package cw.kop.autobackground.images;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import cw.kop.autobackground.R;
import cw.kop.autobackground.settings.AppSettings;
import cw.kop.autobackground.sources.SourceListFragment;

/**
 * Created by TheKeeperOfPie on 8/4/2014.
 */
public class AlbumFragment extends Fragment implements ListView.OnItemClickListener {

    private Context context;
    private ListView albumListView;
    private AlbumAdapter albumAdapter;

    private String type;
    private int changePosition;
    private ArrayList<String> albumNames;
    private ArrayList<String> albumImages;
    private ArrayList<String> albumLinks;
    private ArrayList<String> albumNums;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = getActivity();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();
        type = bundle.getString("type");
        changePosition = bundle.getInt("position");
        albumNames = bundle.getStringArrayList("album_names");
        albumImages = bundle.getStringArrayList("album_images");
        albumLinks = bundle.getStringArrayList("album_links");
        albumNums = bundle.getStringArrayList("album_nums");

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final ViewGroup view = (ViewGroup) inflater.inflate(R.layout.album_list_layout, null);

        albumListView = (ListView) view.findViewById(R.id.album_listview);

        TextView emptyText = new TextView(getActivity());
        emptyText.setText("No Albums");
        emptyText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        emptyText.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        emptyText.setGravity(Gravity.CENTER_HORIZONTAL);

        LinearLayout emptyLayout = new LinearLayout(getActivity());
        emptyLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        emptyLayout.setGravity(Gravity.TOP);
        emptyLayout.addView(emptyText);

        if (AppSettings.getTheme() == R.style.AppLightTheme) {
            albumListView.setBackgroundColor(getResources().getColor(R.color.WHITE_OPAQUE));
            emptyLayout.setBackgroundColor(getResources().getColor(R.color.WHITE_OPAQUE));
        }
        else {
            albumListView.setBackgroundColor(getResources().getColor(R.color.BLACK_OPAQUE));
            emptyLayout.setBackgroundColor(getResources().getColor(R.color.BLACK_OPAQUE));
        }

        ((ViewGroup) albumListView.getParent()).addView(emptyLayout, 0);

        albumListView.setEmptyView(emptyLayout);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (albumAdapter == null) {
            albumAdapter = new AlbumAdapter(context, albumNames, albumImages, albumLinks);
        }

        albumListView.setAdapter(albumAdapter);
        albumListView.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        SourceListFragment sourceListFragment = ((SourceListFragment) getFragmentManager().findFragmentByTag("source_fragment"));

        if (sourceListFragment != null) {
            if (changePosition > -1) {
                sourceListFragment.setEntry(changePosition, type, albumNames.get(position), albumLinks.get(position), albumNums.get(position));
            } else {
                sourceListFragment.addEntry(type, albumNames.get(position), albumLinks.get(position), albumNums.get(position));
            }
            getActivity().onBackPressed();
        }
    }
}
