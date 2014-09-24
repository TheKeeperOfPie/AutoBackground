package cw.kop.autobackground.images;

import java.io.File;

/**
 * Created by TheKeeperOfPie on 9/22/2014.
 */
public class HistoryItem {

    private int index;
    private long time;
    private String  url;
    private File image;

    public HistoryItem(int index, long time, String url, File image) {
        this.index = index;
        this.time = time;
        this.url = url;
        this.image = image;
    }

    public int getIndex() {
        return index;
    }

    public long getTime() {
        return time;
    }


    public String getUrl() {
        return url;
    }


    public File getImage() {
        return image;
    }


}
