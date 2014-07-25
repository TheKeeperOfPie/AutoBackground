package cw.kop.autobackground.notification;

/**
 * Created by TheKeeperOfPie on 7/17/2014.
 */
public class NotificationOptionData {

    private String title, summary, setting;
    private int drawable;


    public NotificationOptionData(String title, String summary, int drawable, String setting) {
        this.title = title;
        this.summary = summary;
        this.drawable = drawable;
        this.setting = setting;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public int getDrawable() {
        return drawable;
    }

    public String getSetting() {
        return setting;
    }

}
