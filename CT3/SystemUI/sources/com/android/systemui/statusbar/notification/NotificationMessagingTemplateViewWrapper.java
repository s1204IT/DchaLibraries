package com.android.systemui.statusbar.notification;

import android.R;
import android.content.Context;
import android.service.notification.StatusBarNotification;
import android.view.View;
import com.android.internal.widget.MessagingLinearLayout;
import com.android.systemui.statusbar.ExpandableNotificationRow;

public class NotificationMessagingTemplateViewWrapper extends NotificationTemplateViewWrapper {
    private View mContractedMessage;

    protected NotificationMessagingTemplateViewWrapper(Context ctx, View view, ExpandableNotificationRow row) {
        super(ctx, view, row);
    }

    private void resolveViews() {
        this.mContractedMessage = null;
        MessagingLinearLayout messagingLinearLayoutFindViewById = this.mView.findViewById(R.id.line);
        if (!(messagingLinearLayoutFindViewById instanceof MessagingLinearLayout) || messagingLinearLayoutFindViewById.getChildCount() <= 0) {
            return;
        }
        MessagingLinearLayout messagingContainer = messagingLinearLayoutFindViewById;
        View child = messagingContainer.getChildAt(0);
        if (child.getId() != messagingContainer.getContractedChildId()) {
            return;
        }
        this.mContractedMessage = child;
    }

    @Override
    public void notifyContentUpdated(StatusBarNotification notification) {
        resolveViews();
        super.notifyContentUpdated(notification);
    }

    @Override
    protected void updateTransformedTypes() {
        super.updateTransformedTypes();
        if (this.mContractedMessage == null) {
            return;
        }
        this.mTransformationHelper.addTransformedView(2, this.mContractedMessage);
    }
}
