package com.android.systemui.statusbar.notification;

import android.R;
import android.app.PendingIntent;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.service.notification.StatusBarNotification;
import android.util.ArraySet;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.android.internal.util.NotificationColorUtil;
import com.android.internal.widget.NotificationActionListLayout;
import com.android.systemui.Dependency;
import com.android.systemui.UiOffloadThread;
import com.android.systemui.statusbar.CrossFadeHelper;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.TransformableView;
import com.android.systemui.statusbar.ViewTransformationHelper;

/* loaded from: classes.dex */
public class NotificationTemplateViewWrapper extends NotificationHeaderViewWrapper {
    private NotificationActionListLayout mActions;
    protected View mActionsContainer;
    private ArraySet<PendingIntent> mCancelledPendingIntents;
    private int mContentHeight;
    private int mMinHeightHint;
    protected ImageView mPicture;
    private ProgressBar mProgressBar;
    private View mRemoteInputHistory;
    private ImageView mReplyAction;
    private TextView mText;
    private TextView mTitle;
    private Rect mTmpRect;
    private UiOffloadThread mUiOffloadThread;

    protected NotificationTemplateViewWrapper(Context context, View view, ExpandableNotificationRow expandableNotificationRow) {
        super(context, view, expandableNotificationRow);
        this.mTmpRect = new Rect();
        this.mCancelledPendingIntents = new ArraySet<>();
        this.mTransformationHelper.setCustomTransformation(new ViewTransformationHelper.CustomTransformation() { // from class: com.android.systemui.statusbar.notification.NotificationTemplateViewWrapper.1
            @Override // com.android.systemui.statusbar.ViewTransformationHelper.CustomTransformation
            public boolean transformTo(TransformState transformState, TransformableView transformableView, float f) {
                if (!(transformableView instanceof HybridNotificationView)) {
                    return false;
                }
                TransformState currentState = transformableView.getCurrentState(1);
                CrossFadeHelper.fadeOut(transformState.getTransformedView(), f);
                if (currentState != null) {
                    transformState.transformViewVerticalTo(currentState, this, f);
                    currentState.recycle();
                }
                return true;
            }

            @Override // com.android.systemui.statusbar.ViewTransformationHelper.CustomTransformation
            public boolean customTransformTarget(TransformState transformState, TransformState transformState2) {
                transformState.setTransformationEndY(getTransformationY(transformState, transformState2));
                return true;
            }

            @Override // com.android.systemui.statusbar.ViewTransformationHelper.CustomTransformation
            public boolean transformFrom(TransformState transformState, TransformableView transformableView, float f) {
                if (!(transformableView instanceof HybridNotificationView)) {
                    return false;
                }
                TransformState currentState = transformableView.getCurrentState(1);
                CrossFadeHelper.fadeIn(transformState.getTransformedView(), f);
                if (currentState != null) {
                    transformState.transformViewVerticalFrom(currentState, this, f);
                    currentState.recycle();
                }
                return true;
            }

            @Override // com.android.systemui.statusbar.ViewTransformationHelper.CustomTransformation
            public boolean initTransformation(TransformState transformState, TransformState transformState2) {
                transformState.setTransformationStartY(getTransformationY(transformState, transformState2));
                return true;
            }

            private float getTransformationY(TransformState transformState, TransformState transformState2) {
                return ((transformState2.getLaidOutLocationOnScreen()[1] + transformState2.getTransformedView().getHeight()) - transformState.getLaidOutLocationOnScreen()[1]) * 0.33f;
            }
        }, 2);
    }

    private void resolveTemplateViews(StatusBarNotification statusBarNotification) {
        this.mPicture = (ImageView) this.mView.findViewById(R.id.mediaProjection);
        if (this.mPicture != null) {
            this.mPicture.setTag(com.android.systemui.R.id.image_icon_tag, statusBarNotification.getNotification().getLargeIcon());
        }
        this.mTitle = (TextView) this.mView.findViewById(R.id.title);
        this.mText = (TextView) this.mView.findViewById(R.id.perm_money_icon);
        View viewFindViewById = this.mView.findViewById(R.id.progress);
        if (viewFindViewById instanceof ProgressBar) {
            this.mProgressBar = (ProgressBar) viewFindViewById;
        } else {
            this.mProgressBar = null;
        }
        this.mActionsContainer = this.mView.findViewById(R.id.KEYCODE_Y);
        this.mActions = this.mView.findViewById(R.id.KEYCODE_X);
        this.mReplyAction = (ImageView) this.mView.findViewById(R.id.marquee_forever);
        this.mRemoteInputHistory = this.mView.findViewById(R.id.icon_preferred);
        updatePendingIntentCancellations();
    }

    private void updatePendingIntentCancellations() {
        if (this.mActions != null) {
            int childCount = this.mActions.getChildCount();
            for (int i = 0; i < childCount; i++) {
                final Button button = (Button) this.mActions.getChildAt(i);
                performOnPendingIntentCancellation(button, new Runnable() { // from class: com.android.systemui.statusbar.notification.-$$Lambda$NotificationTemplateViewWrapper$oMwnxymrbNZg0qMyiDXpJrgotxQ
                    @Override // java.lang.Runnable
                    public final void run() {
                        NotificationTemplateViewWrapper.lambda$updatePendingIntentCancellations$0(this.f$0, button);
                    }
                });
            }
        }
        if (this.mReplyAction != null) {
            this.mReplyAction.setEnabled(true);
            performOnPendingIntentCancellation(this.mReplyAction, new Runnable() { // from class: com.android.systemui.statusbar.notification.-$$Lambda$NotificationTemplateViewWrapper$FtzUFjG_Gvdx031nKmSLXw7UiBg
                @Override // java.lang.Runnable
                public final void run() {
                    NotificationTemplateViewWrapper.lambda$updatePendingIntentCancellations$1(this.f$0);
                }
            });
        }
    }

    public static /* synthetic */ void lambda$updatePendingIntentCancellations$0(NotificationTemplateViewWrapper notificationTemplateViewWrapper, Button button) {
        if (button.isEnabled()) {
            button.setEnabled(false);
            ColorStateList textColors = button.getTextColors();
            int[] colors = textColors.getColors();
            int[] iArr = new int[colors.length];
            float f = notificationTemplateViewWrapper.mView.getResources().getFloat(R.dimen.date_picker_day_width);
            for (int i = 0; i < colors.length; i++) {
                iArr[i] = notificationTemplateViewWrapper.blendColorWithBackground(colors[i], f);
            }
            button.setTextColor(new ColorStateList(textColors.getStates(), iArr));
        }
    }

    public static /* synthetic */ void lambda$updatePendingIntentCancellations$1(NotificationTemplateViewWrapper notificationTemplateViewWrapper) {
        if (notificationTemplateViewWrapper.mReplyAction != null && notificationTemplateViewWrapper.mReplyAction.isEnabled()) {
            notificationTemplateViewWrapper.mReplyAction.setEnabled(false);
            Drawable drawableMutate = notificationTemplateViewWrapper.mReplyAction.getDrawable().mutate();
            PorterDuffColorFilter porterDuffColorFilter = (PorterDuffColorFilter) drawableMutate.getColorFilter();
            float f = notificationTemplateViewWrapper.mView.getResources().getFloat(R.dimen.date_picker_day_width);
            if (porterDuffColorFilter != null) {
                drawableMutate.mutate().setColorFilter(notificationTemplateViewWrapper.blendColorWithBackground(porterDuffColorFilter.getColor(), f), porterDuffColorFilter.getMode());
            } else {
                notificationTemplateViewWrapper.mReplyAction.setAlpha(f);
            }
        }
    }

    private int blendColorWithBackground(int i, float f) {
        return NotificationColorUtil.compositeColors(Color.argb((int) (f * 255.0f), Color.red(i), Color.green(i), Color.blue(i)), resolveBackgroundColor());
    }

    private void performOnPendingIntentCancellation(View view, final Runnable runnable) {
        final PendingIntent pendingIntent = (PendingIntent) view.getTag(R.id.installerExemptIgnored);
        if (pendingIntent == null) {
            return;
        }
        if (this.mCancelledPendingIntents.contains(pendingIntent)) {
            runnable.run();
            return;
        }
        final PendingIntent.CancelListener cancelListener = new PendingIntent.CancelListener() { // from class: com.android.systemui.statusbar.notification.-$$Lambda$NotificationTemplateViewWrapper$CdGwp65qr_-MX4610o6pqFvZxIU
            public final void onCancelled(PendingIntent pendingIntent2) {
                NotificationTemplateViewWrapper notificationTemplateViewWrapper = this.f$0;
                notificationTemplateViewWrapper.mView.post(new Runnable() { // from class: com.android.systemui.statusbar.notification.-$$Lambda$NotificationTemplateViewWrapper$xG2GQ4HDvuPqD0L-UzmtaSPeQy4
                    @Override // java.lang.Runnable
                    public final void run() {
                        NotificationTemplateViewWrapper.lambda$performOnPendingIntentCancellation$2(notificationTemplateViewWrapper, pendingIntent, runnable);
                    }
                });
            }
        };
        if (this.mUiOffloadThread == null) {
            this.mUiOffloadThread = (UiOffloadThread) Dependency.get(UiOffloadThread.class);
        }
        if (view.isAttachedToWindow()) {
            this.mUiOffloadThread.submit(new Runnable() { // from class: com.android.systemui.statusbar.notification.-$$Lambda$NotificationTemplateViewWrapper$g3I4Kek7Jbtd7d61H8EguvhgrJw
                @Override // java.lang.Runnable
                public final void run() {
                    pendingIntent.registerCancelListener(cancelListener);
                }
            });
        }
        view.addOnAttachStateChangeListener(new AnonymousClass2(pendingIntent, cancelListener));
    }

    public static /* synthetic */ void lambda$performOnPendingIntentCancellation$2(NotificationTemplateViewWrapper notificationTemplateViewWrapper, PendingIntent pendingIntent, Runnable runnable) {
        notificationTemplateViewWrapper.mCancelledPendingIntents.add(pendingIntent);
        runnable.run();
    }

    /* renamed from: com.android.systemui.statusbar.notification.NotificationTemplateViewWrapper$2, reason: invalid class name */
    class AnonymousClass2 implements View.OnAttachStateChangeListener {
        final /* synthetic */ PendingIntent.CancelListener val$listener;
        final /* synthetic */ PendingIntent val$pendingIntent;

        AnonymousClass2(PendingIntent pendingIntent, PendingIntent.CancelListener cancelListener) {
            this.val$pendingIntent = pendingIntent;
            this.val$listener = cancelListener;
        }

        @Override // android.view.View.OnAttachStateChangeListener
        public void onViewAttachedToWindow(View view) {
            UiOffloadThread uiOffloadThread = NotificationTemplateViewWrapper.this.mUiOffloadThread;
            final PendingIntent pendingIntent = this.val$pendingIntent;
            final PendingIntent.CancelListener cancelListener = this.val$listener;
            uiOffloadThread.submit(new Runnable() { // from class: com.android.systemui.statusbar.notification.-$$Lambda$NotificationTemplateViewWrapper$2$hLjA2XXd-c0oPihadtdZw3GNclc
                @Override // java.lang.Runnable
                public final void run() {
                    pendingIntent.registerCancelListener(cancelListener);
                }
            });
        }

        @Override // android.view.View.OnAttachStateChangeListener
        public void onViewDetachedFromWindow(View view) {
            UiOffloadThread uiOffloadThread = NotificationTemplateViewWrapper.this.mUiOffloadThread;
            final PendingIntent pendingIntent = this.val$pendingIntent;
            final PendingIntent.CancelListener cancelListener = this.val$listener;
            uiOffloadThread.submit(new Runnable() { // from class: com.android.systemui.statusbar.notification.-$$Lambda$NotificationTemplateViewWrapper$2$wS--1KV8Whw4yTApvD_Oru1vhok
                @Override // java.lang.Runnable
                public final void run() {
                    pendingIntent.unregisterCancelListener(cancelListener);
                }
            });
        }
    }

    @Override // com.android.systemui.statusbar.notification.NotificationViewWrapper
    public boolean disallowSingleClick(float f, float f2) {
        if (this.mReplyAction != null && this.mReplyAction.getVisibility() == 0 && (isOnView(this.mReplyAction, f, f2) || isOnView(this.mPicture, f, f2))) {
            return true;
        }
        return super.disallowSingleClick(f, f2);
    }

    private boolean isOnView(View view, float f, float f2) {
        for (View view2 = (View) view.getParent(); view2 != null && !(view2 instanceof ExpandableNotificationRow); view2 = (View) view2.getParent()) {
            view2.getHitRect(this.mTmpRect);
            f -= this.mTmpRect.left;
            f2 -= this.mTmpRect.top;
        }
        view.getHitRect(this.mTmpRect);
        return this.mTmpRect.contains((int) f, (int) f2);
    }

    @Override // com.android.systemui.statusbar.notification.NotificationHeaderViewWrapper, com.android.systemui.statusbar.notification.NotificationViewWrapper
    public void onContentUpdated(ExpandableNotificationRow expandableNotificationRow) {
        resolveTemplateViews(expandableNotificationRow.getStatusBarNotification());
        super.onContentUpdated(expandableNotificationRow);
    }

    @Override // com.android.systemui.statusbar.notification.NotificationHeaderViewWrapper
    protected void updateTransformedTypes() {
        super.updateTransformedTypes();
        if (this.mTitle != null) {
            this.mTransformationHelper.addTransformedView(1, this.mTitle);
        }
        if (this.mText != null) {
            this.mTransformationHelper.addTransformedView(2, this.mText);
        }
        if (this.mPicture != null) {
            this.mTransformationHelper.addTransformedView(3, this.mPicture);
        }
        if (this.mProgressBar != null) {
            this.mTransformationHelper.addTransformedView(4, this.mProgressBar);
        }
    }

    @Override // com.android.systemui.statusbar.notification.NotificationViewWrapper
    public void setContentHeight(int i, int i2) {
        super.setContentHeight(i, i2);
        this.mContentHeight = i;
        this.mMinHeightHint = i2;
        updateActionOffset();
    }

    @Override // com.android.systemui.statusbar.notification.NotificationViewWrapper
    public boolean shouldClipToRounding(boolean z, boolean z2) {
        if (super.shouldClipToRounding(z, z2)) {
            return true;
        }
        return (!z2 || this.mActionsContainer == null || this.mActionsContainer.getVisibility() == 8) ? false : true;
    }

    private void updateActionOffset() {
        if (this.mActionsContainer != null) {
            this.mActionsContainer.setTranslationY((Math.max(this.mContentHeight, this.mMinHeightHint) - this.mView.getHeight()) - getHeaderTranslation());
        }
    }

    @Override // com.android.systemui.statusbar.notification.NotificationViewWrapper
    public int getExtraMeasureHeight() {
        int dimensionPixelSize;
        if (this.mActions != null) {
            dimensionPixelSize = this.mActions.getExtraMeasureHeight();
        } else {
            dimensionPixelSize = 0;
        }
        if (this.mRemoteInputHistory != null && this.mRemoteInputHistory.getVisibility() != 8) {
            dimensionPixelSize += this.mRow.getContext().getResources().getDimensionPixelSize(com.android.systemui.R.dimen.remote_input_history_extra_height);
        }
        return dimensionPixelSize + super.getExtraMeasureHeight();
    }
}
