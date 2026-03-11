package com.android.systemui.statusbar.notification;

import android.R;
import android.content.Context;
import android.view.View;
import com.android.systemui.statusbar.ExpandableNotificationRow;

public class NotificationMediaTemplateViewWrapper extends NotificationTemplateViewWrapper {
    View mActions;

    protected NotificationMediaTemplateViewWrapper(Context context, View view, ExpandableNotificationRow expandableNotificationRow) {
        super(context, view, expandableNotificationRow);
    }

    private void resolveViews() {
        this.mActions = this.mView.findViewById(R.id.fade_in);
    }

    @Override
    public void onContentUpdated(ExpandableNotificationRow expandableNotificationRow) {
        resolveViews();
        super.onContentUpdated(expandableNotificationRow);
    }

    @Override
    protected void updateTransformedTypes() {
        super.updateTransformedTypes();
        if (this.mActions != null) {
            this.mTransformationHelper.addTransformedView(5, this.mActions);
        }
    }

    @Override
    public boolean isDimmable() {
        return getCustomBackgroundColor() == 0;
    }

    @Override
    public boolean shouldClipToRounding(boolean z, boolean z2) {
        return true;
    }
}
