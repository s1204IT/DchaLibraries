package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;
import com.android.systemui.qs.QSPanel;
import com.android.systemui.statusbar.policy.NetworkController;

public abstract class BaseStatusBarHeader extends RelativeLayout implements NetworkController.EmergencyListener {
    public abstract int getCollapsedHeight();

    public abstract void setActivityStarter(ActivityStarter activityStarter);

    public abstract void setCallback(QSPanel.Callback callback);

    public abstract void setExpanded(boolean z);

    public abstract void setExpansion(float f);

    public abstract void setListening(boolean z);

    public abstract void setQSPanel(QSPanel qSPanel);

    public abstract void updateEverything();

    public BaseStatusBarHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
}
