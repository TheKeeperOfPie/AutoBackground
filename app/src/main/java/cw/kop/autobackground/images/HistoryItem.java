package cw.kop.autobackground.images;

import java.io.File;

/**
 * Created by TheKeeperOfPie on 9/22/2014.
 */
public class HistoryItem {

    private long time;
    private String  url;
    private File image;

    public HistoryItem(long time, String url, File image) {
        this.time = time;
        this.url = url;
        this.image = image;
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
