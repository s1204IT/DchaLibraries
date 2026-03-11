package com.android.systemui.statusbar.notification;

import android.content.Context;
import android.view.View;
import com.android.systemui.R;
import com.android.systemui.statusbar.ExpandableNotificationRow;

public class NotificationCustomViewWrapper extends NotificationViewWrapper {
    private boolean mIsLegacy;
    private int mLegacyColor;

    protected NotificationCustomViewWrapper(Context context, View view, ExpandableNotificationRow expandableNotificationRow) {
        super(context, view, expandableNotificationRow);
        this.mLegacyColor = expandableNotificationRow.getContext().getColor(R.color.notification_legacy_background_color);
    }

    @Override
    public void setVisible(boolean z) {
        super.setVisible(z);
        this.mView.setAlpha(z ? 1.0f : 0.0f);
    }

    @Override
    protected boolean shouldClearBackgroundOnReapply() {
        return false;
    }

    @Override
    public int getCustomBackgroundColor() {
        int customBackgroundColor = super.getCustomBackgroundColor();
        if (customBackgroundColor == 0 && this.mIsLegacy) {
            return this.mLegacyColor;
        }
        return customBackgroundColor;
    }

    @Override
    public void setLegacy(boolean z) {
        super.setLegacy(z);
        this.mIsLegacy = z;
    }

    @Override
    public boolean shouldClipToRounding(boolean z, boolean z2) {
        return true;
    }
}
