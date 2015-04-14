package cw.kop.autobackground.sources;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.Arrays;

import cw.kop.autobackground.R;
import cw.kop.autobackground.files.FileHandler;
import cw.kop.autobackground.settings.AppSettings;

/**
 * Created by TheKeeperOfPie on 4/10/2015.
 */
public class AdapterSources extends RecyclerView.Adapter<AdapterSources.ViewHolder> {

    private static final String TAG = AdapterSources.class.getCanonicalName();
    private int baseImageHeight;
    private int overlayColorResource;
    private int colorFilterInt;
    private int colorPrimary;
    private int cardViewToInflate;
    private float sideMarginPixels;
    private AdapterSourceListener adapterSourceListener;
    private ControllerSources controllerSources;

    public AdapterSources(Activity activity, ControllerSources controllerSources, AdapterSourceListener adapterSourceListener) {
        Resources resources = activity.getResources();
        cardViewToInflate = AppSettings.getTheme().equals(AppSettings.APP_LIGHT_THEME) ?
                R.layout.source_list_card :
                R.layout.source_list_card_dark;
        colorFilterInt = AppSettings.getColorFilterInt(activity);
        colorPrimary = resources.getColor(R.color.BLUE_OPAQUE);
        overlayColorResource = AppSettings.getBackgroundColorResource();
        sideMarginPixels = resources.getDimensionPixelSize(R.dimen.side_margin);
        baseImageHeight = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                28,
                resources.getDisplayMetrics()) + TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                24,
                resources.getDisplayMetrics()));
        this.controllerSources = controllerSources;
        this.adapterSourceListener = adapterSourceListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(cardViewToInflate, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        Source source = controllerSources.get(position);

        boolean use = source.isUse();
        boolean preview = source.isPreview();

        holder.title.setText(source.getTitle());

        holder.imageOverlay.setVisibility(use ? View.INVISIBLE : View.VISIBLE);
        holder.sourceExpandContainer.setVisibility(source.isExpanded() ? View.VISIBLE : View.GONE);

        if (source.getType().equals(AppSettings.FOLDER)) {
            holder.toolbar.getMenu().findItem(R.id.item_source_download).setEnabled(false);
        }
        else {
            holder.toolbar.getMenu().findItem(R.id.item_source_download).setEnabled(true);
        }

        if (preview) {
            holder.sourceImage.getLayoutParams().height = (int) ((adapterSourceListener.getItemWidth() - 2f * sideMarginPixels) / 16f * 9);
            holder.sourceImage.setImageResource(R.drawable.ic_file_download_white_48dp);
            holder.sourceImage.setColorFilter(colorFilterInt, PorterDuff.Mode.MULTIPLY);
            holder.sourceImage.requestLayout();

            if (source.getType().equals(AppSettings.FOLDER)) {
                String[] folders = source.getData().split(AppSettings.DATA_SPLITTER);
                for (String folder : folders) {

                    File[] files = new File(folder).listFiles(
                            FileHandler.getImageFileNameFilter());

                    if (files != null && files.length > 0) {
                        File file;
                        if (files.length > 1 && adapterSourceListener.getSpanForPosition(position) == 1) {
                            file = files[1];
                        }
                        else {
                            file = files[0];
                        }
                        source.setImageFile(file);
                        holder.sourceImage.clearColorFilter();
                        Picasso.with(adapterSourceListener.getActivity())
                                .load(file)
                                .fit()
                                .centerCrop()
                                .into(
                                        holder.sourceImage);
                        break;
                    }
                    else {
                        holder.sourceImage.setImageResource(
                                R.drawable.ic_not_interested_white_48dp);
                    }
                }
            }
            else {
                File folder = new File(AppSettings.getDownloadPath() + "/" + source.getTitle() + " " + AppSettings.getImagePrefix());
                if (folder.exists() && folder.isDirectory()) {
                    File[] files = folder.listFiles(FileHandler.getImageFileNameFilter());

                    if (files != null && files.length > 0) {
                        File file;
                        if (files.length > 1 && adapterSourceListener.getSpanForPosition(position) == 1) {
                            file = files[1];
                        }
                        else {
                            file = files[0];
                        }
                        source.setImageFile(file);
                        holder.sourceImage.clearColorFilter();
                        Picasso.with(adapterSourceListener.getActivity())
                                .load(file)
                                .fit()
                                .centerCrop()
                                .into(holder.sourceImage);
                    }
                }
            }
        }
        else {
            Picasso.with(adapterSourceListener.getActivity()).load(android.R.color.transparent).into(holder.sourceImage);
            holder.sourceImage.getLayoutParams().height = baseImageHeight;
            holder.sourceImage.requestLayout();
        }

        if (!preview || !use) {
            holder.title.setTextColor(colorFilterInt);
            holder.title.setShadowLayer(0f, 0f, 0f, 0x00000000);
        }
        else {
            holder.title.setTextColor(0xFFFFFFFF);
            holder.title.setShadowLayer(5.0f, -1f, -1f, 0xFF000000);
        }

        SpannableString typePrefix = new SpannableString("Type: ");
        typePrefix.setSpan(new ForegroundColorSpan(colorPrimary), 0, typePrefix.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        SpannableString dataPrefix = new SpannableString("Data: ");
        dataPrefix.setSpan(new ForegroundColorSpan(colorPrimary), 0, dataPrefix.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        SpannableString numPrefix = new SpannableString("Number of Images: ");
        numPrefix.setSpan(new ForegroundColorSpan(colorPrimary), 0, numPrefix.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        SpannableString sortPrefix = new SpannableString("Sort By: ");
        sortPrefix.setSpan(new ForegroundColorSpan(colorPrimary), 0, sortPrefix.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        SpannableString timePrefix = new SpannableString("Active Time: ");
        timePrefix.setSpan(new ForegroundColorSpan(colorPrimary), 0, timePrefix.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        holder.sourceType.setText(typePrefix);
        holder.sourceType.append(source.getType());
        holder.sourceData.setText(dataPrefix);
        if (source.getType().equals(AppSettings.FOLDER)) {
            holder.sourceData.append(Arrays.toString(source.getData()
                    .split(AppSettings.DATA_SPLITTER)));
        }
        else {
            holder.sourceData.append(source.getData());
        }
        holder.sourceNum.setText(numPrefix);
        if (source.getType().equals(AppSettings.FOLDER)) {
            holder.sourceNum.append("" + source.getNum());
        }
        else {
            holder.sourceNum.append(source.getNumStored() + " / " + source.getNum());
        }

        holder.sourceSort.setVisibility(View.VISIBLE);
        holder.sourceSort.setText(sortPrefix);
        holder.sourceSort.append(source.getSort());

        holder.sourceTime.setText(timePrefix);
        if (source.isUseTime()) {
            holder.sourceTime.append(source.getTime());
        }
        else {
            holder.sourceTime.append("N/A");
        }

    }

    @Override
    public int getItemCount() {
        adapterSourceListener.setEmptyArrowVisibility(
                controllerSources.size() == 0 ? View.VISIBLE : View.GONE);
        return controllerSources.size();
    }

    protected class ViewHolder extends RecyclerView.ViewHolder {

        protected final CardView cardView;
        protected final EditText title;
        protected final View imageOverlay;
        protected final TextView sourceType;
        protected final TextView sourceData;
        protected final TextView sourceNum;
        protected final TextView sourceSort;
        protected final TextView sourceTime;
        protected final ImageView sourceImage;
        protected final LinearLayout sourceExpandContainer;
        protected final Toolbar toolbar;

        public ViewHolder(View itemView) {
            super(itemView);
            cardView = (CardView) itemView.findViewById(R.id.source_card);
            title = (EditText) itemView.findViewById(R.id.source_title);
            imageOverlay = itemView.findViewById(R.id.source_image_overlay);
            sourceType = (TextView) itemView.findViewById(R.id.source_type);
            sourceData = (TextView) itemView.findViewById(R.id.source_data);
            sourceNum = (TextView) itemView.findViewById(R.id.source_num);
            sourceSort = (TextView) itemView.findViewById(R.id.source_sort);
            sourceTime = (TextView) itemView.findViewById(R.id.source_time);
            sourceImage = (ImageView) itemView.findViewById(R.id.source_image);
            sourceExpandContainer = (LinearLayout) itemView.findViewById(R.id.source_expand_container);
            toolbar = (Toolbar) itemView.findViewById(R.id.toolbar_actions);

            toolbar.inflateMenu(R.menu.menu_source);
            toolbar.getMenu().findItem(R.id.item_source_download).getIcon().setColorFilter(colorFilterInt,
                    PorterDuff.Mode.MULTIPLY);
            toolbar.getMenu().findItem(R.id.item_source_delete).getIcon().setColorFilter(colorFilterInt,
                    PorterDuff.Mode.MULTIPLY);
            toolbar.getMenu().findItem(R.id.item_source_view).getIcon().setColorFilter(colorFilterInt,
                    PorterDuff.Mode.MULTIPLY);
            toolbar.getMenu().findItem(R.id.item_source_edit).getIcon().setColorFilter(colorFilterInt,
                    PorterDuff.Mode.MULTIPLY);
            toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    switch (menuItem.getItemId()) {
                        case R.id.item_source_download:
                            adapterSourceListener.onDownloadClick(ViewHolder.this.itemView,
                                    controllerSources.get(getPosition()));
                            break;
                        case R.id.item_source_delete:
                            adapterSourceListener.onDeleteClick(ViewHolder.this.itemView,
                                    getPosition());
                            break;
                        case R.id.item_source_view:
                            adapterSourceListener.onViewImageClick(ViewHolder.this.itemView,
                                    getPosition());
                            break;
                        case R.id.item_source_edit:
                            adapterSourceListener.onEditClick(ViewHolder.this.itemView,
                                    getPosition());
                            break;
                    }
                    return false;
                }
            });

            sourceImage.setMinimumHeight(baseImageHeight);
            imageOverlay.setBackgroundResource(overlayColorResource);
            title.setClickable(false);

            View.OnLongClickListener longClickListener = new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    adapterSourceListener.onLongClick(getPosition());
                    return true;
                }
            };

            title.setOnLongClickListener(longClickListener);
            cardView.setOnLongClickListener(longClickListener);

            View.OnClickListener clickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    controllerSources.get(getPosition()).setExpanded(!sourceExpandContainer.isShown());
                    sourceExpandContainer.setVisibility(sourceExpandContainer.isShown() ? View.GONE : View.VISIBLE);
                }
            };

            cardView.setOnClickListener(clickListener);
            title.setOnClickListener(clickListener);
        }
    }

    public interface AdapterSourceListener {

        void onDownloadClick(View view, Source source);
        void onDeleteClick(View view, int index);
        void onViewImageClick(View view, int index);
        void onEditClick(View view, int index);
        void onLongClick(int position);
        Context getActivity();
        void setEmptyArrowVisibility(int visibility);
        float getItemWidth();
        int getSpanForPosition(int position);
    }

}
