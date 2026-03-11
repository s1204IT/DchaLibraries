package com.android.systemui.statusbar;

import android.app.Notification;
import android.content.Context;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.internal.util.NotificationColorUtil;
import com.android.systemui.R;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.phone.IconMerger;

public class NotificationOverflowIconsView extends IconMerger {
    private int mIconSize;
    private TextView mMoreText;
    private NotificationColorUtil mNotificationColorUtil;
    private int mTintColor;

    public NotificationOverflowIconsView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mNotificationColorUtil = NotificationColorUtil.getInstance(getContext());
        this.mTintColor = getResources().getColor(R.color.keyguard_overflow_content_color);
        this.mIconSize = getResources().getDimensionPixelSize(android.R.dimen.accessibility_magnification_thumbnail_container_stroke_width);
    }

    public void setMoreText(TextView moreText) {
        this.mMoreText = moreText;
    }

    public void addNotification(NotificationData.Entry notification) {
        StatusBarIconView v = new StatusBarIconView(getContext(), "", notification.notification.getNotification());
        v.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        addView(v, this.mIconSize, this.mIconSize);
        v.set(notification.icon.getStatusBarIcon());
        applyColor(notification.notification.getNotification(), v);
        updateMoreText();
    }

    private void applyColor(Notification notification, StatusBarIconView view) {
        view.setColorFilter(this.mTintColor, PorterDuff.Mode.MULTIPLY);
    }

    private void updateMoreText() {
        this.mMoreText.setText(getResources().getString(R.string.keyguard_more_overflow_text, Integer.valueOf(getChildCount())));
    }
}
