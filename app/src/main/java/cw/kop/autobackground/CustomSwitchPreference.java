package cw.kop.autobackground;

import android.content.Context;
import android.preference.SwitchPreference;
import android.util.AttributeSet;

/**
 * Created by TheKeeperOfPie on 7/15/2014.
 */

public class CustomSwitchPreference extends SwitchPreference {

    public CustomSwitchPreference(Context context) {
        super(context, null);
    }

    public CustomSwitchPreference(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.switchPreferenceStyle);
    }

    public CustomSwitchPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

}
