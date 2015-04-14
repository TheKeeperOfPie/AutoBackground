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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SortData sortData = (SortData) o;

        return !(getTitle() != null ? !getTitle().equals(sortData.getTitle()) :
                sortData.getTitle() != null);

    }

    @Override
    public int hashCode() {
        return getTitle() != null ? getTitle().hashCode() : 0;
    }
}
