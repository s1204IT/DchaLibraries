package com.android.systemui.statusbar.notification;

import android.content.Context;
import android.graphics.drawable.Icon;
import android.service.notification.StatusBarNotification;
import android.view.View;
import com.android.systemui.R;
import com.android.systemui.statusbar.ExpandableNotificationRow;

public class NotificationBigPictureTemplateViewWrapper extends NotificationTemplateViewWrapper {
    protected NotificationBigPictureTemplateViewWrapper(Context context, View view, ExpandableNotificationRow expandableNotificationRow) {
        super(context, view, expandableNotificationRow);
    }

    @Override
    public void onContentUpdated(ExpandableNotificationRow expandableNotificationRow) {
        super.onContentUpdated(expandableNotificationRow);
        updateImageTag(expandableNotificationRow.getStatusBarNotification());
    }

    private void updateImageTag(StatusBarNotification statusBarNotification) {
        Icon icon = (Icon) statusBarNotification.getNotification().extras.getParcelable("android.largeIcon.big");
        if (icon != null) {
            this.mPicture.setTag(R.id.image_icon_tag, icon);
        }
    }
}
