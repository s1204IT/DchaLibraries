package com.android.systemui.statusbar;

import android.R;
import android.content.Context;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.service.notification.StatusBarNotification;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewStub;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;

public class ExpandableNotificationRow extends ActivatableNotificationView {
    private boolean mExpandable;
    private boolean mExpansionDisabled;
    private NotificationGuts mGuts;
    private boolean mHasUserChangedExpansion;
    private boolean mIsHeadsUp;
    private boolean mIsSystemExpanded;
    private ExpansionLogger mLogger;
    private String mLoggingKey;
    private int mMaxExpandHeight;
    private NotificationContentView mPrivateLayout;
    private NotificationContentView mPublicLayout;
    private int mRowMaxHeight;
    private int mRowMinHeight;
    private boolean mSensitive;
    private boolean mShowingPublic;
    private boolean mShowingPublicForIntrinsicHeight;
    private boolean mShowingPublicInitialized;
    private StatusBarNotification mStatusBarNotification;
    private boolean mUserExpanded;
    private boolean mUserLocked;
    private View mVetoButton;
    private boolean mWasReset;

    public interface ExpansionLogger {
        void logNotificationExpansion(String str, boolean z, boolean z2);
    }

    public void setIconAnimationRunning(boolean running) {
        setIconAnimationRunning(running, this.mPublicLayout);
        setIconAnimationRunning(running, this.mPrivateLayout);
    }

    private void setIconAnimationRunning(boolean running, NotificationContentView layout) {
        if (layout != null) {
            View contractedChild = layout.getContractedChild();
            View expandedChild = layout.getExpandedChild();
            setIconAnimationRunningForChild(running, contractedChild);
            setIconAnimationRunningForChild(running, expandedChild);
        }
    }

    private void setIconAnimationRunningForChild(boolean running, View child) {
        if (child != null) {
            ImageView icon = (ImageView) child.findViewById(R.id.icon);
            setIconRunning(icon, running);
            ImageView rightIcon = (ImageView) child.findViewById(R.id.replaceText);
            setIconRunning(rightIcon, running);
        }
    }

    private void setIconRunning(ImageView imageView, boolean running) {
        if (imageView != null) {
            Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AnimationDrawable) {
                AnimationDrawable animationDrawable = (AnimationDrawable) drawable;
                if (running) {
                    animationDrawable.start();
                    return;
                } else {
                    animationDrawable.stop();
                    return;
                }
            }
            if (drawable instanceof AnimatedVectorDrawable) {
                AnimatedVectorDrawable animationDrawable2 = (AnimatedVectorDrawable) drawable;
                if (running) {
                    animationDrawable2.start();
                } else {
                    animationDrawable2.stop();
                }
            }
        }
    }

    public void setStatusBarNotification(StatusBarNotification statusBarNotification) {
        this.mStatusBarNotification = statusBarNotification;
        updateVetoButton();
    }

    public StatusBarNotification getStatusBarNotification() {
        return this.mStatusBarNotification;
    }

    public void setHeadsUp(boolean isHeadsUp) {
        this.mIsHeadsUp = isHeadsUp;
    }

    public ExpandableNotificationRow(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void reset() {
        super.reset();
        this.mRowMinHeight = 0;
        boolean wasExpanded = isExpanded();
        this.mRowMaxHeight = 0;
        this.mExpandable = false;
        this.mHasUserChangedExpansion = false;
        this.mUserLocked = false;
        this.mShowingPublic = false;
        this.mSensitive = false;
        this.mShowingPublicInitialized = false;
        this.mIsSystemExpanded = false;
        this.mExpansionDisabled = false;
        this.mPublicLayout.reset(this.mIsHeadsUp);
        this.mPrivateLayout.reset(this.mIsHeadsUp);
        resetHeight();
        logExpansionEvent(false, wasExpanded);
    }

    public void resetHeight() {
        if (this.mIsHeadsUp) {
            resetActualHeight();
        }
        this.mMaxExpandHeight = 0;
        this.mWasReset = true;
        onHeightReset();
        requestLayout();
    }

    @Override
    protected boolean filterMotionEvent(MotionEvent event) {
        return this.mIsHeadsUp || super.filterMotionEvent(event);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mPublicLayout = (NotificationContentView) findViewById(com.android.systemui.R.id.expandedPublic);
        this.mPrivateLayout = (NotificationContentView) findViewById(com.android.systemui.R.id.expanded);
        ViewStub gutsStub = (ViewStub) findViewById(com.android.systemui.R.id.notification_guts_stub);
        gutsStub.setOnInflateListener(new ViewStub.OnInflateListener() {
            @Override
            public void onInflate(ViewStub stub, View inflated) {
                ExpandableNotificationRow.this.mGuts = (NotificationGuts) inflated;
                ExpandableNotificationRow.this.mGuts.setClipTopAmount(ExpandableNotificationRow.this.getClipTopAmount());
                ExpandableNotificationRow.this.mGuts.setActualHeight(ExpandableNotificationRow.this.getActualHeight());
            }
        });
        this.mVetoButton = findViewById(com.android.systemui.R.id.veto);
    }

    @Override
    public boolean onRequestSendAccessibilityEvent(View child, AccessibilityEvent event) {
        if (!super.onRequestSendAccessibilityEvent(child, event)) {
            return false;
        }
        AccessibilityEvent record = AccessibilityEvent.obtain();
        onInitializeAccessibilityEvent(record);
        dispatchPopulateAccessibilityEvent(record);
        event.appendRecord(record);
        return true;
    }

    @Override
    public void setDark(boolean dark, boolean fade, long delay) {
        super.setDark(dark, fade, delay);
        NotificationContentView showing = getShowingLayout();
        if (showing != null) {
            showing.setDark(dark, fade, delay);
        }
    }

    public void setHeightRange(int rowMinHeight, int rowMaxHeight) {
        this.mRowMinHeight = rowMinHeight;
        this.mRowMaxHeight = rowMaxHeight;
    }

    public boolean isExpandable() {
        return this.mExpandable;
    }

    public void setExpandable(boolean expandable) {
        this.mExpandable = expandable;
    }

    public boolean hasUserChangedExpansion() {
        return this.mHasUserChangedExpansion;
    }

    public boolean isUserExpanded() {
        return this.mUserExpanded;
    }

    public void setUserExpanded(boolean userExpanded) {
        if (!userExpanded || this.mExpandable) {
            boolean wasExpanded = isExpanded();
            this.mHasUserChangedExpansion = true;
            this.mUserExpanded = userExpanded;
            logExpansionEvent(true, wasExpanded);
        }
    }

    public void resetUserExpansion() {
        this.mHasUserChangedExpansion = false;
        this.mUserExpanded = false;
    }

    public boolean isUserLocked() {
        return this.mUserLocked;
    }

    public void setUserLocked(boolean userLocked) {
        this.mUserLocked = userLocked;
    }

    public boolean isSystemExpanded() {
        return this.mIsSystemExpanded;
    }

    public void setSystemExpanded(boolean expand) {
        if (expand != this.mIsSystemExpanded) {
            boolean wasExpanded = isExpanded();
            this.mIsSystemExpanded = expand;
            notifyHeightChanged();
            logExpansionEvent(false, wasExpanded);
        }
    }

    public void setExpansionDisabled(boolean expansionDisabled) {
        if (expansionDisabled != this.mExpansionDisabled) {
            boolean wasExpanded = isExpanded();
            this.mExpansionDisabled = expansionDisabled;
            logExpansionEvent(false, wasExpanded);
            if (wasExpanded != isExpanded()) {
                notifyHeightChanged();
            }
        }
    }

    public boolean isClearable() {
        return this.mStatusBarNotification != null && this.mStatusBarNotification.isClearable();
    }

    public void applyExpansionToLayout() {
        boolean expand = isExpanded();
        if (expand && this.mExpandable) {
            setActualHeight(this.mMaxExpandHeight);
        } else {
            setActualHeight(this.mRowMinHeight);
        }
    }

    @Override
    public int getIntrinsicHeight() {
        if (isUserLocked()) {
            return getActualHeight();
        }
        boolean inExpansionState = isExpanded();
        if (inExpansionState) {
            return this.mShowingPublicForIntrinsicHeight ? this.mRowMinHeight : getMaxExpandHeight();
        }
        return this.mRowMinHeight;
    }

    private boolean isExpanded() {
        return !this.mExpansionDisabled && ((!hasUserChangedExpansion() && isSystemExpanded()) || isUserExpanded());
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        boolean updateExpandHeight = this.mMaxExpandHeight == 0 && !this.mWasReset;
        updateMaxExpandHeight();
        if (updateExpandHeight) {
            applyExpansionToLayout();
        }
        this.mWasReset = false;
    }

    private void updateMaxExpandHeight() {
        int intrinsicBefore = getIntrinsicHeight();
        this.mMaxExpandHeight = this.mPrivateLayout.getMaxHeight();
        if (intrinsicBefore != getIntrinsicHeight()) {
            notifyHeightChanged();
        }
    }

    public void setSensitive(boolean sensitive) {
        this.mSensitive = sensitive;
    }

    @Override
    public void setHideSensitiveForIntrinsicHeight(boolean hideSensitive) {
        this.mShowingPublicForIntrinsicHeight = this.mSensitive && hideSensitive;
    }

    @Override
    public void setHideSensitive(boolean hideSensitive, boolean animated, long delay, long duration) {
        boolean oldShowingPublic = this.mShowingPublic;
        this.mShowingPublic = this.mSensitive && hideSensitive;
        if ((!this.mShowingPublicInitialized || this.mShowingPublic != oldShowingPublic) && this.mPublicLayout.getChildCount() != 0) {
            if (!animated) {
                this.mPublicLayout.animate().cancel();
                this.mPrivateLayout.animate().cancel();
                this.mPublicLayout.setAlpha(1.0f);
                this.mPrivateLayout.setAlpha(1.0f);
                this.mPublicLayout.setVisibility(this.mShowingPublic ? 0 : 4);
                this.mPrivateLayout.setVisibility(this.mShowingPublic ? 4 : 0);
            } else {
                animateShowingPublic(delay, duration);
            }
            updateVetoButton();
            this.mShowingPublicInitialized = true;
        }
    }

    private void animateShowingPublic(long delay, long duration) {
        final View source = this.mShowingPublic ? this.mPrivateLayout : this.mPublicLayout;
        View target = this.mShowingPublic ? this.mPublicLayout : this.mPrivateLayout;
        source.setVisibility(0);
        target.setVisibility(0);
        target.setAlpha(0.0f);
        source.animate().cancel();
        target.animate().cancel();
        source.animate().alpha(0.0f).setStartDelay(delay).setDuration(duration).withEndAction(new Runnable() {
            @Override
            public void run() {
                source.setVisibility(4);
            }
        });
        target.animate().alpha(1.0f).setStartDelay(delay).setDuration(duration);
    }

    private void updateVetoButton() {
        this.mVetoButton.setVisibility((!isClearable() || this.mShowingPublic) ? 8 : 0);
    }

    public int getMaxExpandHeight() {
        return this.mShowingPublicForIntrinsicHeight ? this.mRowMinHeight : this.mMaxExpandHeight;
    }

    @Override
    public boolean isContentExpandable() {
        NotificationContentView showingLayout = getShowingLayout();
        return showingLayout.isContentExpandable();
    }

    @Override
    public void setActualHeight(int height, boolean notifyListeners) {
        this.mPrivateLayout.setActualHeight(height);
        this.mPublicLayout.setActualHeight(height);
        if (this.mGuts != null) {
            this.mGuts.setActualHeight(height);
        }
        invalidate();
        super.setActualHeight(height, notifyListeners);
    }

    @Override
    public int getMaxHeight() {
        NotificationContentView showingLayout = getShowingLayout();
        return showingLayout.getMaxHeight();
    }

    @Override
    public int getMinHeight() {
        NotificationContentView showingLayout = getShowingLayout();
        return showingLayout.getMinHeight();
    }

    @Override
    public void setClipTopAmount(int clipTopAmount) {
        super.setClipTopAmount(clipTopAmount);
        this.mPrivateLayout.setClipTopAmount(clipTopAmount);
        this.mPublicLayout.setClipTopAmount(clipTopAmount);
        if (this.mGuts != null) {
            this.mGuts.setClipTopAmount(clipTopAmount);
        }
    }

    public void notifyContentUpdated() {
        this.mPublicLayout.notifyContentUpdated();
        this.mPrivateLayout.notifyContentUpdated();
    }

    public boolean isMaxExpandHeightInitialized() {
        return this.mMaxExpandHeight != 0;
    }

    private NotificationContentView getShowingLayout() {
        return this.mShowingPublic ? this.mPublicLayout : this.mPrivateLayout;
    }

    public void setExpansionLogger(ExpansionLogger logger, String key) {
        this.mLogger = logger;
        this.mLoggingKey = key;
    }

    private void logExpansionEvent(boolean userAction, boolean wasExpanded) {
        boolean nowExpanded = isExpanded();
        if (wasExpanded != nowExpanded && this.mLogger != null) {
            this.mLogger.logNotificationExpansion(this.mLoggingKey, userAction, nowExpanded);
        }
    }
}
