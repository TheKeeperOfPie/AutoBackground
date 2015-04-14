package cw.kop.autobackground.sources;

import android.util.Log;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import cw.kop.autobackground.files.FileHandler;
import cw.kop.autobackground.settings.AppSettings;

/**
 * Created by TheKeeperOfPie on 3/26/2015.
 */
public class ControllerSources {

    public static final String NO_SOURCES = "NO_SOURCE";
    public static final String NO_ACTIVE_SOURCES = "NO_ACTIVE_SOURCES";
    public static final String NEED_DOWNLOAD = "NEED_DOWNLOAD";
    public static final String NO_IMAGES = "NO_IMAGES";
    public static final String OKAY = "OKAY";

    private List<Source> listData;
    private HashSet<String> titles;
    private SourceListener listener;
    private String state;

    public ControllerSources() {
        listData = new ArrayList<>();
        titles = new HashSet<>();
        loadSources();
    }

    public void setListener(SourceListener listener) {
        this.listener = listener;
    }

    public void loadSources() {
        listData = AppSettings.getSources();
    }

    public int size() {
        return listData.size();
    }

    public Source get(int position) {
        if (position >= 0 && position < listData.size()) {
            return listData.get(position);
        }
        return null;
    }

    public boolean toggleActivated(int position) {
        Source changedItem = listData.get(position);
        changedItem.setUse(!changedItem.isUse());
        listData.set(position, changedItem);

        for (Source source : listData) {
            if (source.isUse()) {
                return true;
            }
        }

        state = NO_ACTIVE_SOURCES;
        if (listener != null) {
            listener.onChangeState();
        }

        return false;
    }

    public boolean setItem(Source source, int position) {

        Source oldSource = listData.get(position);

        if (!oldSource.getTitle().equals(source.getTitle())) {
            if (titles.contains(source.getTitle())) {
                return false;
            }
        }
        titles.remove(oldSource.getTitle());
        File folder = new File(AppSettings.getDownloadPath() + "/" + source.getTitle() + " " + AppSettings.getImagePrefix());
        if (folder.exists() && folder.isDirectory()) {
            source.setNumStored(folder.listFiles(FileHandler.getImageFileNameFilter()).length);
        }
        else {
            source.setNumStored(0);
        }
        listData.set(position, source);
        titles.add(source.getTitle());
        if (listener != null) {
            listener.notifyDataSetChanged();
        }
        saveData();
        return true;
    }

    public boolean addItem(Source source) {

        if (titles.contains(source.getTitle())) {
            return false;
        }

        File folder = new File(AppSettings.getDownloadPath() + "/" + source.getTitle() + " " + AppSettings.getImagePrefix());
        if (folder.exists() && folder.isDirectory()) {
            source.setNumStored(folder.listFiles(FileHandler.getImageFileNameFilter()).length);
        }
        else {
            source.setNumStored(0);
        }

        listData.add(source);
        titles.add(source.getTitle());
        if (listener != null) {
            listener.notifyDataSetChanged();
        }
        return true;
    }

    public void removeItem(final int position) {

        titles.remove(listData.get(position)
                .getTitle());
        listData.remove(position);
        if (listener != null) {
            listener.notifyDataSetChanged();
        }
    }

    public void updateNum() {

        FilenameFilter filenameFilter = FileHandler.getImageFileNameFilter();

        String cacheDir = AppSettings.getDownloadPath();

        if (listData != null) {
            for (Source source : listData) {
                if (source.getType().equals(AppSettings.FOLDER)) {

                    int numImages = 0;

                    for (String folderName : source.getData().split(AppSettings.DATA_SPLITTER)) {
                        File folder = new File(folderName);
                        if (folder.exists() && folder.isDirectory()) {
                            numImages += folder.listFiles(filenameFilter).length;
                        }
                    }

                    source.setNum(numImages);
                }
                else {
                    File folder = new File(cacheDir + "/" + source.getTitle() + " " + AppSettings.getImagePrefix());
                    if (folder.exists() && folder.isDirectory()) {
                        source.setNumStored(folder.listFiles(filenameFilter).length);
                    }
                }
            }
            if (listener != null) {
                listener.notifyDataSetChanged();
            }
        }
    }

    public void recount() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                state = checkSources();
                if (listener != null) {
                    listener.onChangeState();
                }
            }
        }).start();
    }

    public String checkSources() {

        if (listData.isEmpty()) {
            return NO_SOURCES;
        }

        boolean noActive = true;
        boolean needDownload = true;

        for (int index = 0; (noActive || needDownload) && index < listData.size(); index++) {

            boolean use = listData.get(index).isUse();

            if (noActive && use) {
                noActive = false;
            }

            if (use && listData.get(index).getType().equals(AppSettings.FOLDER)) {
                needDownload = false;
                Log.i("SLA", "Type: " + listData.get(index)
                        .getType());
            }

        }

        if (noActive) {
            return NO_ACTIVE_SOURCES;
        }

        boolean noImages = FileHandler.hasImages();

        if (noImages) {
            if (needDownload) {
                return NEED_DOWNLOAD;
            }
            return NO_IMAGES;
        }

        return OKAY;
    }

    public void sortData(final String key) {

        ArrayList<Source> sortList = new ArrayList<Source>();
        sortList.addAll(listData);

        Collections.sort(sortList, new Comparator<Source>() {
            @Override
            public int compare(Source lhs, Source rhs) {

                if (key.equals(Source.USE)) {
                    boolean first = lhs.isUse();
                    boolean second = rhs.isUse();

                    if (first && second || (!first && !second)) {
                        return lhs.getTitle()
                                .compareTo(rhs.getTitle());
                    }

                    return first ? -1 : 1;

                }

                if (key.equals(Source.NUM)) {
                    return lhs.getNum() - rhs.getNum();
                }

                if (key.equals(Source.TITLE)) {
                    return lhs.getTitle()
                            .compareTo(rhs.getTitle());
                }

                if (key.equals(Source.DATA)) {
                    return lhs.getData()
                            .compareTo(rhs.getData());
                }

                return lhs.getTitle()
                        .compareTo(rhs.getTitle());
            }
        });

        if (sortList.equals(listData)) {
            Collections.reverse(sortList);
        }
        listData = sortList;

        if (listener != null) {
            listener.notifyDataSetChanged();
        }

    }

    public void saveData() {

        AppSettings.setSources(listData);

        Log.i("WLA", "SavedListData" + listData.size());
        Log.i("WLA", "Saved Data: " + AppSettings.getNumberSources());
    }

    public String getState() {
        return state;
    }

    public interface SourceListener {
        void notifyDataSetChanged();
        void onChangeState();
    }

}
