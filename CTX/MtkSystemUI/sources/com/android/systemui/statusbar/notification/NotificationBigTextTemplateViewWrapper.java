package com.android.systemui.statusbar.notification;

import android.R;
import android.content.Context;
import android.service.notification.StatusBarNotification;
import android.view.View;
import com.android.internal.widget.ImageFloatingTextView;
import com.android.systemui.statusbar.ExpandableNotificationRow;

public class NotificationBigTextTemplateViewWrapper extends NotificationTemplateViewWrapper {
    private ImageFloatingTextView mBigtext;

    protected NotificationBigTextTemplateViewWrapper(Context context, View view, ExpandableNotificationRow expandableNotificationRow) {
        super(context, view, expandableNotificationRow);
    }

    private void resolveViews(StatusBarNotification statusBarNotification) {
        this.mBigtext = this.mView.findViewById(R.id.accessibility_shortcut_target_checkbox);
    }

    @Override
    public void onContentUpdated(ExpandableNotificationRow expandableNotificationRow) {
        resolveViews(expandableNotificationRow.getStatusBarNotification());
        super.onContentUpdated(expandableNotificationRow);
    }

    @Override
    protected void updateTransformedTypes() {
        super.updateTransformedTypes();
        if (this.mBigtext != null) {
            this.mTransformationHelper.addTransformedView(2, this.mBigtext);
        }
    }
}
