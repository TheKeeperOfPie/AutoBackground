package cw.kop.autobackground;

import android.content.Context;
import android.preference.SwitchPreference;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by TheKeeperOfPie on 7/15/2014.
 */

public class CustomSwitchPreference extends SwitchPreference {

    private Context context;
    private View view;

    public CustomSwitchPreference(Context context) {
        super(context, null);
        this.context = context;
    }

    public CustomSwitchPreference(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.switchPreferenceStyle);
    }

    public CustomSwitchPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        this.view = view;
    }

    @Override
    protected void notifyChanged() {
        super.notifyChanged();
        if (view != null) {
            view.setBackgroundColor(context.getResources().getColor(R.color.TRANSPARENT_BACKGROUND));
        }
    }
}
