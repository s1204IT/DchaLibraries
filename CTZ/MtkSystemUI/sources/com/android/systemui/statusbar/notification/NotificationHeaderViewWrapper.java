package com.android.systemui.statusbar.notification;

import android.app.Notification;
import android.content.Context;
import android.util.ArraySet;
import android.view.NotificationHeaderView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.internal.widget.NotificationExpandButton;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.TransformableView;
import com.android.systemui.statusbar.ViewTransformationHelper;
import java.util.Stack;

public class NotificationHeaderViewWrapper extends NotificationViewWrapper {
    private static final Interpolator LOW_PRIORITY_HEADER_CLOSE = new PathInterpolator(0.4f, 0.0f, 0.7f, 1.0f);
    protected int mColor;
    private NotificationExpandButton mExpandButton;
    private TextView mHeaderText;
    protected float mHeaderTranslation;
    private ImageView mIcon;
    private boolean mIsLowPriority;
    private NotificationHeaderView mNotificationHeader;
    private boolean mShowExpandButtonAtEnd;
    private boolean mTransformLowPriorityTitle;
    protected final ViewTransformationHelper mTransformationHelper;
    private final int mTranslationForHeader;
    private ImageView mWorkProfileImage;

    protected NotificationHeaderViewWrapper(Context context, View view, ExpandableNotificationRow expandableNotificationRow) {
        super(context, view, expandableNotificationRow);
        this.mShowExpandButtonAtEnd = context.getResources().getBoolean(R.bool.config_showNotificationExpandButtonAtEnd);
        this.mTransformationHelper = new ViewTransformationHelper();
        this.mTransformationHelper.setCustomTransformation(new CustomInterpolatorTransformation(1) {
            @Override
            public Interpolator getCustomInterpolator(int i, boolean z) {
                boolean z2 = NotificationHeaderViewWrapper.this.mView instanceof NotificationHeaderView;
                if (i == 16) {
                    if ((!z2 || z) && (z2 || !z)) {
                        return NotificationHeaderViewWrapper.LOW_PRIORITY_HEADER_CLOSE;
                    }
                    return Interpolators.LINEAR_OUT_SLOW_IN;
                }
                return null;
            }

            @Override
            protected boolean hasCustomTransformation() {
                return NotificationHeaderViewWrapper.this.mIsLowPriority && NotificationHeaderViewWrapper.this.mTransformLowPriorityTitle;
            }
        }, 1);
        resolveHeaderViews();
        addAppOpsOnClickListener(expandableNotificationRow);
        this.mTranslationForHeader = context.getResources().getDimensionPixelSize(android.R.dimen.conversation_badge_protrusion_group_expanded) - context.getResources().getDimensionPixelSize(android.R.dimen.conversation_compact_face_pile_protection_width);
    }

    protected void resolveHeaderViews() {
        this.mIcon = (ImageView) this.mView.findViewById(android.R.id.icon);
        this.mHeaderText = (TextView) this.mView.findViewById(android.R.id.collapsing);
        this.mExpandButton = this.mView.findViewById(android.R.id.autofill_sheet_scroll_view_space);
        this.mWorkProfileImage = (ImageView) this.mView.findViewById(android.R.id.insideInset);
        this.mNotificationHeader = this.mView.findViewById(android.R.id.fullUser);
        this.mNotificationHeader.setShowExpandButtonAtEnd(this.mShowExpandButtonAtEnd);
        this.mColor = this.mNotificationHeader.getOriginalIconColor();
    }

    private void addAppOpsOnClickListener(ExpandableNotificationRow expandableNotificationRow) {
        this.mNotificationHeader.setAppOpsOnClickListener(expandableNotificationRow.getAppOpsOnClickListener());
    }

    @Override
    public void onContentUpdated(ExpandableNotificationRow expandableNotificationRow) {
        super.onContentUpdated(expandableNotificationRow);
        this.mIsLowPriority = expandableNotificationRow.isLowPriority();
        this.mTransformLowPriorityTitle = (expandableNotificationRow.isChildInGroup() || expandableNotificationRow.isSummaryWithChildren()) ? false : true;
        ArraySet<View> allTransformingViews = this.mTransformationHelper.getAllTransformingViews();
        resolveHeaderViews();
        updateTransformedTypes();
        addRemainingTransformTypes();
        updateCropToPaddingForImageViews();
        Notification notification = expandableNotificationRow.getStatusBarNotification().getNotification();
        this.mIcon.setTag(R.id.image_icon_tag, notification.getSmallIcon());
        this.mWorkProfileImage.setTag(R.id.image_icon_tag, notification.getSmallIcon());
        ArraySet<View> allTransformingViews2 = this.mTransformationHelper.getAllTransformingViews();
        for (int i = 0; i < allTransformingViews.size(); i++) {
            View viewValueAt = allTransformingViews.valueAt(i);
            if (!allTransformingViews2.contains(viewValueAt)) {
                this.mTransformationHelper.resetTransformedView(viewValueAt);
            }
        }
    }

    private void addRemainingTransformTypes() {
        this.mTransformationHelper.addRemainingTransformTypes(this.mView);
    }

    private void updateCropToPaddingForImageViews() {
        Stack stack = new Stack();
        stack.push(this.mView);
        while (!stack.isEmpty()) {
            View view = (View) stack.pop();
            if (view instanceof ImageView) {
                ((ImageView) view).setCropToPadding(true);
            } else if (view instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) view;
                for (int i = 0; i < viewGroup.getChildCount(); i++) {
                    stack.push(viewGroup.getChildAt(i));
                }
            }
        }
    }

    protected void updateTransformedTypes() {
        this.mTransformationHelper.reset();
        this.mTransformationHelper.addTransformedView(0, this.mIcon);
        if (this.mIsLowPriority) {
            this.mTransformationHelper.addTransformedView(1, this.mHeaderText);
        }
    }

    @Override
    public void updateExpandability(boolean z, View.OnClickListener onClickListener) {
        this.mExpandButton.setVisibility(z ? 0 : 8);
        NotificationHeaderView notificationHeaderView = this.mNotificationHeader;
        if (!z) {
            onClickListener = null;
        }
        notificationHeaderView.setOnClickListener(onClickListener);
    }

    @Override
    public void setHeaderVisibleAmount(float f) {
        super.setHeaderVisibleAmount(f);
        this.mNotificationHeader.setAlpha(f);
        this.mHeaderTranslation = (1.0f - f) * this.mTranslationForHeader;
        this.mView.setTranslationY(this.mHeaderTranslation);
    }

    @Override
    public int getHeaderTranslation() {
        return (int) this.mHeaderTranslation;
    }

    @Override
    public NotificationHeaderView getNotificationHeader() {
        return this.mNotificationHeader;
    }

    @Override
    public TransformState getCurrentState(int i) {
        return this.mTransformationHelper.getCurrentState(i);
    }

    @Override
    public void transformTo(TransformableView transformableView, Runnable runnable) {
        this.mTransformationHelper.transformTo(transformableView, runnable);
    }

    @Override
    public void transformTo(TransformableView transformableView, float f) {
        this.mTransformationHelper.transformTo(transformableView, f);
    }

    @Override
    public void transformFrom(TransformableView transformableView) {
        this.mTransformationHelper.transformFrom(transformableView);
    }

    @Override
    public void transformFrom(TransformableView transformableView, float f) {
        this.mTransformationHelper.transformFrom(transformableView, f);
    }

    @Override
    public void setIsChildInGroup(boolean z) {
        super.setIsChildInGroup(z);
        this.mTransformLowPriorityTitle = !z;
    }

    @Override
    public void setVisible(boolean z) {
        super.setVisible(z);
        this.mTransformationHelper.setVisible(z);
    }
}
