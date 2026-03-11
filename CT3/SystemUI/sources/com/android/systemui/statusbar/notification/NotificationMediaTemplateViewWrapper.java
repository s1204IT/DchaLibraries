package com.android.systemui.statusbar.notification;

import android.R;
import android.content.Context;
import android.service.notification.StatusBarNotification;
import android.view.View;
import com.android.systemui.statusbar.ExpandableNotificationRow;

public class NotificationMediaTemplateViewWrapper extends NotificationTemplateViewWrapper {
    View mActions;

    protected NotificationMediaTemplateViewWrapper(Context ctx, View view, ExpandableNotificationRow row) {
        super(ctx, view, row);
    }

    private void resolveViews(StatusBarNotification notification) {
        this.mActions = this.mView.findViewById(R.id.language_picker_item);
    }

    @Override
    public void notifyContentUpdated(StatusBarNotification notification) {
        resolveViews(notification);
        super.notifyContentUpdated(notification);
    }

    @Override
    protected void updateTransformedTypes() {
        super.updateTransformedTypes();
        if (this.mActions == null) {
            return;
        }
        this.mTransformationHelper.addTransformedView(5, this.mActions);
    }
}
