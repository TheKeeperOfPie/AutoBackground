package cw.kop.autobackground.sources;

/**
 * Created by TheKeeperOfPie on 3/18/2015.
 */
public class SortData {

    private String title;
    private String data;
    private String query;

    public SortData(String title, String data, String query) {
        this.title = title;
        this.data = data;
        this.query = query;
    }

    public String getTitle() {
        return title;
    }

    public String getData() {
        return data;
    }

    public String getQuery() {
        return query;
    }

}
