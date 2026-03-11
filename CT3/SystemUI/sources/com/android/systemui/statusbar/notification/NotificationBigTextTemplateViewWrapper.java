package com.android.systemui.statusbar.notification;

import android.R;
import android.content.Context;
import android.service.notification.StatusBarNotification;
import android.view.View;
import com.android.internal.widget.ImageFloatingTextView;
import com.android.systemui.statusbar.ExpandableNotificationRow;

public class NotificationBigTextTemplateViewWrapper extends NotificationTemplateViewWrapper {
    private ImageFloatingTextView mBigtext;

    protected NotificationBigTextTemplateViewWrapper(Context ctx, View view, ExpandableNotificationRow row) {
        super(ctx, view, row);
    }

    private void resolveViews(StatusBarNotification notification) {
        this.mBigtext = this.mView.findViewById(R.id.launchRecognizer);
    }

    @Override
    public void notifyContentUpdated(StatusBarNotification notification) {
        resolveViews(notification);
        super.notifyContentUpdated(notification);
    }

    @Override
    protected void updateTransformedTypes() {
        super.updateTransformedTypes();
        if (this.mBigtext == null) {
            return;
        }
        this.mTransformationHelper.addTransformedView(2, this.mBigtext);
    }
}
