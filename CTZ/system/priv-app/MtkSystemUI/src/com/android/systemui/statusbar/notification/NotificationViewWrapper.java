package com.android.systemui.statusbar.notification;

import android.R;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.NotificationHeaderView;
import android.view.View;
import com.android.systemui.statusbar.CrossFadeHelper;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.TransformableView;

/* loaded from: classes.dex */
public abstract class NotificationViewWrapper implements TransformableView {
    private int mBackgroundColor = 0;
    protected final ExpandableNotificationRow mRow;
    protected final View mView;

    public static NotificationViewWrapper wrap(Context context, View view, ExpandableNotificationRow expandableNotificationRow) {
        if (view.getId() == 16909350) {
            if ("bigPicture".equals(view.getTag())) {
                return new NotificationBigPictureTemplateViewWrapper(context, view, expandableNotificationRow);
            }
            if ("bigText".equals(view.getTag())) {
                return new NotificationBigTextTemplateViewWrapper(context, view, expandableNotificationRow);
            }
            if ("media".equals(view.getTag()) || "bigMediaNarrow".equals(view.getTag())) {
                return new NotificationMediaTemplateViewWrapper(context, view, expandableNotificationRow);
            }
            if ("messaging".equals(view.getTag())) {
                return new NotificationMessagingTemplateViewWrapper(context, view, expandableNotificationRow);
            }
            return new NotificationTemplateViewWrapper(context, view, expandableNotificationRow);
        }
        if (view instanceof NotificationHeaderView) {
            return new NotificationHeaderViewWrapper(context, view, expandableNotificationRow);
        }
        return new NotificationCustomViewWrapper(context, view, expandableNotificationRow);
    }

    protected NotificationViewWrapper(Context context, View view, ExpandableNotificationRow expandableNotificationRow) {
        this.mView = view;
        this.mRow = expandableNotificationRow;
        onReinflated();
    }

    public void onContentUpdated(ExpandableNotificationRow expandableNotificationRow) {
    }

    public void onReinflated() {
        if (shouldClearBackgroundOnReapply()) {
            this.mBackgroundColor = 0;
        }
        Drawable background = this.mView.getBackground();
        if (background instanceof ColorDrawable) {
            this.mBackgroundColor = ((ColorDrawable) background).getColor();
            this.mView.setBackground(null);
        }
    }

    protected boolean shouldClearBackgroundOnReapply() {
        return true;
    }

    public void updateExpandability(boolean z, View.OnClickListener onClickListener) {
    }

    public NotificationHeaderView getNotificationHeader() {
        return null;
    }

    public int getHeaderTranslation() {
        return 0;
    }

    @Override // com.android.systemui.statusbar.TransformableView
    public TransformState getCurrentState(int i) {
        return null;
    }

    @Override // com.android.systemui.statusbar.TransformableView
    public void transformTo(TransformableView transformableView, Runnable runnable) {
        CrossFadeHelper.fadeOut(this.mView, runnable);
    }

    @Override // com.android.systemui.statusbar.TransformableView
    public void transformTo(TransformableView transformableView, float f) {
        CrossFadeHelper.fadeOut(this.mView, f);
    }

    @Override // com.android.systemui.statusbar.TransformableView
    public void transformFrom(TransformableView transformableView) {
        CrossFadeHelper.fadeIn(this.mView);
    }

    @Override // com.android.systemui.statusbar.TransformableView
    public void transformFrom(TransformableView transformableView, float f) {
        CrossFadeHelper.fadeIn(this.mView, f);
    }

    @Override // com.android.systemui.statusbar.TransformableView
    public void setVisible(boolean z) {
        this.mView.animate().cancel();
        this.mView.setVisibility(z ? 0 : 4);
    }

    public int getCustomBackgroundColor() {
        if (this.mRow.isSummaryWithChildren()) {
            return 0;
        }
        return this.mBackgroundColor;
    }

    protected int resolveBackgroundColor() {
        int customBackgroundColor = getCustomBackgroundColor();
        if (customBackgroundColor != 0) {
            return customBackgroundColor;
        }
        return this.mView.getContext().getColor(R.color.accessibility_magnification_thumbnail_container_background_color);
    }

    public void setLegacy(boolean z) {
    }

    public void setContentHeight(int i, int i2) {
    }

    public void setRemoteInputVisible(boolean z) {
    }

    public void setIsChildInGroup(boolean z) {
    }

    public boolean isDimmable() {
        return true;
    }

    public boolean disallowSingleClick(float f, float f2) {
        return false;
    }

    public int getMinLayoutHeight() {
        return 0;
    }

    public boolean shouldClipToRounding(boolean z, boolean z2) {
        return false;
    }

    public void setHeaderVisibleAmount(float f) {
    }

    public int getExtraMeasureHeight() {
        return 0;
    }
}
