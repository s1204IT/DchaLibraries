package com.android.systemui.statusbar;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;
import com.android.systemui.R;
import com.android.systemui.ViewInvertHelper;

public class NotificationOverflowContainer extends ActivatableNotificationView {
    private boolean mDark;
    private NotificationOverflowIconsView mIconsView;
    private ViewInvertHelper mViewInvertHelper;

    public NotificationOverflowContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mIconsView = (NotificationOverflowIconsView) findViewById(R.id.overflow_icons_view);
        this.mIconsView.setMoreText((TextView) findViewById(R.id.more_text));
        this.mIconsView.setOverflowIndicator(findViewById(R.id.more_icon_overflow));
        this.mViewInvertHelper = new ViewInvertHelper(findViewById(R.id.content), 700L);
    }

    @Override
    public void setDark(boolean dark, boolean fade, long delay) {
        super.setDark(dark, fade, delay);
        if (this.mDark != dark) {
            this.mDark = dark;
            if (fade) {
                this.mViewInvertHelper.fade(dark, delay);
            } else {
                this.mViewInvertHelper.update(dark);
            }
        }
    }

    public NotificationOverflowIconsView getIconsView() {
        return this.mIconsView;
    }
}
