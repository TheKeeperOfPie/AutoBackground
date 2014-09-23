package cw.kop.autobackground.images;

import java.io.File;

/**
 * Created by TheKeeperOfPie on 9/22/2014.
 */
public class HistoryItem {

    private int index;
    private String name, url;
    private File image;

    public HistoryItem(int index, String name, String url, File image) {
        this.index = index;
        this.name = name;
        this.url = url;
        this.image = image;
    }

    public int getIndex() {
        return index;
    }

    public String getName() {
        return name;
    }


    public String getUrl() {
        return url;
    }


    public File getImage() {
        return image;
    }


}
