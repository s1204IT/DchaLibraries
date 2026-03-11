package com.android.systemui.statusbar.notification;

import android.content.Context;
import android.view.View;
import com.android.internal.widget.MessagingLayout;
import com.android.internal.widget.MessagingLinearLayout;
import com.android.systemui.R;
import com.android.systemui.statusbar.ExpandableNotificationRow;

public class NotificationMessagingTemplateViewWrapper extends NotificationTemplateViewWrapper {
    private MessagingLayout mMessagingLayout;
    private MessagingLinearLayout mMessagingLinearLayout;
    private final int mMinHeightWithActions;

    protected NotificationMessagingTemplateViewWrapper(Context context, View view, ExpandableNotificationRow expandableNotificationRow) {
        super(context, view, expandableNotificationRow);
        this.mMessagingLayout = (MessagingLayout) view;
        this.mMinHeightWithActions = NotificationUtils.getFontScaledHeight(context, R.dimen.notification_messaging_actions_min_height);
    }

    private void resolveViews() {
        this.mMessagingLinearLayout = this.mMessagingLayout.getMessagingLinearLayout();
    }

    @Override
    public void onContentUpdated(ExpandableNotificationRow expandableNotificationRow) {
        resolveViews();
        super.onContentUpdated(expandableNotificationRow);
    }

    @Override
    protected void updateTransformedTypes() {
        super.updateTransformedTypes();
        if (this.mMessagingLinearLayout != null) {
            this.mTransformationHelper.addTransformedView(this.mMessagingLinearLayout.getId(), this.mMessagingLinearLayout);
        }
    }

    @Override
    public void setRemoteInputVisible(boolean z) {
        this.mMessagingLayout.showHistoricMessages(z);
    }

    @Override
    public int getMinLayoutHeight() {
        if (this.mActionsContainer != null && this.mActionsContainer.getVisibility() != 8) {
            return this.mMinHeightWithActions;
        }
        return super.getMinLayoutHeight();
    }
}
