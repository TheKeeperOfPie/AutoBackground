package cw.kop.autobackground;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;

import cw.kop.autobackground.files.FileHandler;
import cw.kop.autobackground.settings.AppSettings;

/**
 * Created by TheKeeperOfPie on 4/11/2015.
 */
public class AdapterNavigation extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = AdapterNavigation.class.getCanonicalName();
    private static final int HEADER = 0;
    private static final int NAVIGATION_ITEM = 1;
    private Activity activity;
    private ArrayList<String> fragmentList;
    private int[] iconImages = new int[] {
            R.drawable.ic_view_list_white_24dp,
            R.drawable.ic_now_wallpaper_white_24dp,
            R.drawable.ic_file_download_white_24dp,
            R.drawable.ic_account_circle_white_24dp,
            R.drawable.ic_filter_white_24dp,
            R.drawable.ic_notifications_white_24dp,
            R.drawable.ic_watch_white_24dp,
            R.drawable.ic_settings_white_24dp,
            R.drawable.ic_history_white_24dp,
            R.drawable.ic_info_white_24dp};
    private int colorFilterInt;
    private NavigationClickListener clickListener;
    private float headerHeight;

    public AdapterNavigation(Activity activity, String[] nameArray, NavigationClickListener clickListener) {
        this.activity = activity;
        this.clickListener = clickListener;
        Configuration configuration = activity.getResources().getConfiguration();
        headerHeight = Math.round(TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        Math.min(180, (configuration.screenWidthDp - 56) / 16f * 9),
                        activity.getResources().getDisplayMetrics()));
        fragmentList = new ArrayList<>();
        Collections.addAll(fragmentList, nameArray);
        colorFilterInt = AppSettings.getColorFilterInt(activity);
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? HEADER : NAVIGATION_ITEM;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == HEADER) {
            return new ViewHolderHeader(LayoutInflater.from(parent.getContext()).inflate(R.layout.nav_header, parent, false));
        }

        return new ViewHolderNavigation(LayoutInflater.from(parent.getContext()).inflate(R.layout.nav_row, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        if (holder instanceof ViewHolderHeader) {

            ViewHolderHeader viewHolderHeader = (ViewHolderHeader) holder;

            Picasso.with(activity).load(FileHandler.getCurrentBitmapFile()).fit().centerCrop().into(viewHolderHeader.imageHeader);
            Log.d(TAG, "Loaded header image");
        }
        else if (holder instanceof ViewHolderNavigation) {
            position--;

            ViewHolderNavigation viewHolderNavigation = (ViewHolderNavigation) holder;

            viewHolderNavigation.fragmentImage.setImageResource(iconImages[position]);
            viewHolderNavigation.fragmentTitle.setText(fragmentList.get(position));
        }

    }
    @Override
    public int getItemCount() {
        return fragmentList.size() + 1;
    }

    public void loadNavPicture() {
        notifyItemChanged(0);
    }

    protected class ViewHolderHeader extends RecyclerView.ViewHolder {

        protected final ImageView imageHeader;

        public ViewHolderHeader(View itemView) {
            super(itemView);
            imageHeader = (ImageView) itemView.findViewById(R.id.image_header);
            itemView.getLayoutParams().height = (int) headerHeight;
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(LiveWallpaperService.OPEN_IMAGE);
                    clickListener.sendIntent(intent);
                }
            });
            itemView.requestLayout();
        }
    }

    protected class ViewHolderNavigation extends RecyclerView.ViewHolder {

        protected final ImageView fragmentImage;
        protected final TextView fragmentTitle;

        public ViewHolderNavigation(View itemView) {
            super(itemView);

            fragmentImage = (ImageView) itemView.findViewById(R.id.fragment_image);
            fragmentTitle = (TextView) itemView.findViewById(R.id.fragment_title);

            fragmentImage.setColorFilter(colorFilterInt, PorterDuff.Mode.MULTIPLY);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    clickListener.onItemClick(getPosition() - 1);
                }
            });
        }
    }

    public interface NavigationClickListener {
        void onItemClick(int position);
        void sendIntent(Intent intent);
    }

}
