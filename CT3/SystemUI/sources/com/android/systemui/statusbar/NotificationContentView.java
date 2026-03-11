package com.android.systemui.statusbar;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.Context;
import android.graphics.Rect;
import android.service.notification.StatusBarNotification;
import android.util.AttributeSet;
import android.view.NotificationHeaderView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.android.internal.util.NotificationColorUtil;
import com.android.systemui.R;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.notification.HybridGroupManager;
import com.android.systemui.statusbar.notification.HybridNotificationView;
import com.android.systemui.statusbar.notification.NotificationCustomViewWrapper;
import com.android.systemui.statusbar.notification.NotificationUtils;
import com.android.systemui.statusbar.notification.NotificationViewWrapper;
import com.android.systemui.statusbar.phone.NotificationGroupManager;
import com.android.systemui.statusbar.policy.RemoteInputView;

public class NotificationContentView extends FrameLayout {
    private boolean mAnimate;
    private int mAnimationStartVisibleType;
    private boolean mBeforeN;
    private final Rect mClipBounds;
    private boolean mClipToActualHeight;
    private int mClipTopAmount;
    private ExpandableNotificationRow mContainingNotification;
    private int mContentHeight;
    private int mContentHeightAtAnimationStart;
    private View mContractedChild;
    private NotificationViewWrapper mContractedWrapper;
    private boolean mDark;
    private final ViewTreeObserver.OnPreDrawListener mEnableAnimationPredrawListener;
    private View.OnClickListener mExpandClickListener;
    private boolean mExpandable;
    private View mExpandedChild;
    private RemoteInputView mExpandedRemoteInput;
    private NotificationViewWrapper mExpandedWrapper;
    private boolean mFocusOnVisibilityChange;
    private boolean mForceSelectNextLayout;
    private NotificationGroupManager mGroupManager;
    private View mHeadsUpChild;
    private int mHeadsUpHeight;
    private RemoteInputView mHeadsUpRemoteInput;
    private NotificationViewWrapper mHeadsUpWrapper;
    private boolean mHeadsupDisappearRunning;
    private HybridGroupManager mHybridGroupManager;
    private boolean mIsChildInGroup;
    private boolean mIsHeadsUp;
    private final int mMinContractedHeight;
    private final int mNotificationContentMarginEnd;
    private int mNotificationMaxHeight;
    private PendingIntent mPreviousExpandedRemoteInputIntent;
    private PendingIntent mPreviousHeadsUpRemoteInputIntent;
    private RemoteInputController mRemoteInputController;
    private boolean mShowingLegacyBackground;
    private HybridNotificationView mSingleLineView;
    private int mSingleLineWidthIndention;
    private int mSmallHeight;
    private StatusBarNotification mStatusBarNotification;
    private int mTransformationStartVisibleType;
    private boolean mUserExpanding;
    private int mVisibleType;

    public NotificationContentView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mClipBounds = new Rect();
        this.mVisibleType = 0;
        this.mEnableAnimationPredrawListener = new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                NotificationContentView.this.post(new Runnable() {
                    @Override
                    public void run() {
                        NotificationContentView.this.mAnimate = true;
                    }
                });
                NotificationContentView.this.getViewTreeObserver().removeOnPreDrawListener(this);
                return true;
            }
        };
        this.mClipToActualHeight = true;
        this.mAnimationStartVisibleType = -1;
        this.mForceSelectNextLayout = true;
        this.mContentHeightAtAnimationStart = -1;
        this.mHybridGroupManager = new HybridGroupManager(getContext(), this);
        this.mMinContractedHeight = getResources().getDimensionPixelSize(R.dimen.min_notification_layout_height);
        this.mNotificationContentMarginEnd = getResources().getDimensionPixelSize(android.R.dimen.aerr_padding_list_bottom);
        reset();
    }

    public void setHeights(int smallHeight, int headsUpMaxHeight, int maxHeight) {
        this.mSmallHeight = smallHeight;
        this.mHeadsUpHeight = headsUpMaxHeight;
        this.mNotificationMaxHeight = maxHeight;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int heightSpec;
        int spec;
        int heightMode = View.MeasureSpec.getMode(heightMeasureSpec);
        boolean hasFixedHeight = heightMode == 1073741824;
        boolean isHeightLimited = heightMode == Integer.MIN_VALUE;
        int maxSize = Integer.MAX_VALUE;
        int width = View.MeasureSpec.getSize(widthMeasureSpec);
        if (hasFixedHeight || isHeightLimited) {
            maxSize = View.MeasureSpec.getSize(heightMeasureSpec);
        }
        int maxChildHeight = 0;
        if (this.mExpandedChild != null) {
            int size = Math.min(maxSize, this.mNotificationMaxHeight);
            ViewGroup.LayoutParams layoutParams = this.mExpandedChild.getLayoutParams();
            if (layoutParams.height >= 0) {
                size = Math.min(maxSize, layoutParams.height);
            }
            if (size == Integer.MAX_VALUE) {
                spec = View.MeasureSpec.makeMeasureSpec(0, 0);
            } else {
                spec = View.MeasureSpec.makeMeasureSpec(size, Integer.MIN_VALUE);
            }
            this.mExpandedChild.measure(widthMeasureSpec, spec);
            maxChildHeight = Math.max(0, this.mExpandedChild.getMeasuredHeight());
        }
        if (this.mContractedChild != null) {
            int size2 = Math.min(maxSize, this.mSmallHeight);
            if (shouldContractedBeFixedSize()) {
                heightSpec = View.MeasureSpec.makeMeasureSpec(size2, 1073741824);
            } else {
                heightSpec = View.MeasureSpec.makeMeasureSpec(size2, Integer.MIN_VALUE);
            }
            this.mContractedChild.measure(widthMeasureSpec, heightSpec);
            int measuredHeight = this.mContractedChild.getMeasuredHeight();
            if (measuredHeight < this.mMinContractedHeight) {
                heightSpec = View.MeasureSpec.makeMeasureSpec(this.mMinContractedHeight, 1073741824);
                this.mContractedChild.measure(widthMeasureSpec, heightSpec);
            }
            maxChildHeight = Math.max(maxChildHeight, measuredHeight);
            if (updateContractedHeaderWidth()) {
                this.mContractedChild.measure(widthMeasureSpec, heightSpec);
            }
            if (this.mExpandedChild != null && this.mContractedChild.getMeasuredHeight() > this.mExpandedChild.getMeasuredHeight()) {
                int heightSpec2 = View.MeasureSpec.makeMeasureSpec(this.mContractedChild.getMeasuredHeight(), 1073741824);
                this.mExpandedChild.measure(widthMeasureSpec, heightSpec2);
            }
        }
        if (this.mHeadsUpChild != null) {
            int size3 = Math.min(maxSize, this.mHeadsUpHeight);
            ViewGroup.LayoutParams layoutParams2 = this.mHeadsUpChild.getLayoutParams();
            if (layoutParams2.height >= 0) {
                size3 = Math.min(size3, layoutParams2.height);
            }
            this.mHeadsUpChild.measure(widthMeasureSpec, View.MeasureSpec.makeMeasureSpec(size3, Integer.MIN_VALUE));
            maxChildHeight = Math.max(maxChildHeight, this.mHeadsUpChild.getMeasuredHeight());
        }
        if (this.mSingleLineView != null) {
            int singleLineWidthSpec = widthMeasureSpec;
            if (this.mSingleLineWidthIndention != 0 && View.MeasureSpec.getMode(widthMeasureSpec) != 0) {
                singleLineWidthSpec = View.MeasureSpec.makeMeasureSpec((width - this.mSingleLineWidthIndention) + this.mSingleLineView.getPaddingEnd(), Integer.MIN_VALUE);
            }
            this.mSingleLineView.measure(singleLineWidthSpec, View.MeasureSpec.makeMeasureSpec(maxSize, Integer.MIN_VALUE));
            maxChildHeight = Math.max(maxChildHeight, this.mSingleLineView.getMeasuredHeight());
        }
        int ownHeight = Math.min(maxChildHeight, maxSize);
        setMeasuredDimension(width, ownHeight);
    }

    private boolean updateContractedHeaderWidth() {
        NotificationHeaderView contractedHeader = this.mContractedWrapper.getNotificationHeader();
        if (contractedHeader != null) {
            if (this.mExpandedChild != null && this.mExpandedWrapper.getNotificationHeader() != null) {
                NotificationHeaderView expandedHeader = this.mExpandedWrapper.getNotificationHeader();
                int expandedSize = expandedHeader.getMeasuredWidth() - expandedHeader.getPaddingEnd();
                int collapsedSize = contractedHeader.getMeasuredWidth() - expandedHeader.getPaddingEnd();
                if (expandedSize != collapsedSize) {
                    int paddingEnd = contractedHeader.getMeasuredWidth() - expandedSize;
                    int paddingLeft = contractedHeader.isLayoutRtl() ? paddingEnd : contractedHeader.getPaddingLeft();
                    int paddingTop = contractedHeader.getPaddingTop();
                    if (contractedHeader.isLayoutRtl()) {
                        paddingEnd = contractedHeader.getPaddingLeft();
                    }
                    contractedHeader.setPadding(paddingLeft, paddingTop, paddingEnd, contractedHeader.getPaddingBottom());
                    contractedHeader.setShowWorkBadgeAtEnd(true);
                    return true;
                }
            } else {
                int paddingEnd2 = this.mNotificationContentMarginEnd;
                if (contractedHeader.getPaddingEnd() != paddingEnd2) {
                    int paddingLeft2 = contractedHeader.isLayoutRtl() ? paddingEnd2 : contractedHeader.getPaddingLeft();
                    int paddingTop2 = contractedHeader.getPaddingTop();
                    if (contractedHeader.isLayoutRtl()) {
                        paddingEnd2 = contractedHeader.getPaddingLeft();
                    }
                    contractedHeader.setPadding(paddingLeft2, paddingTop2, paddingEnd2, contractedHeader.getPaddingBottom());
                    contractedHeader.setShowWorkBadgeAtEnd(false);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean shouldContractedBeFixedSize() {
        if (this.mBeforeN) {
            return this.mContractedWrapper instanceof NotificationCustomViewWrapper;
        }
        return false;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int previousHeight = 0;
        if (this.mExpandedChild != null) {
            previousHeight = this.mExpandedChild.getHeight();
        }
        super.onLayout(changed, left, top, right, bottom);
        if (previousHeight != 0 && this.mExpandedChild.getHeight() != previousHeight) {
            this.mContentHeightAtAnimationStart = previousHeight;
        }
        updateClipping();
        invalidateOutline();
        selectLayout(false, this.mForceSelectNextLayout);
        this.mForceSelectNextLayout = false;
        updateExpandButtons(this.mExpandable);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateVisibility();
    }

    public void reset() {
        if (this.mContractedChild != null) {
            this.mContractedChild.animate().cancel();
            removeView(this.mContractedChild);
        }
        this.mPreviousExpandedRemoteInputIntent = null;
        if (this.mExpandedRemoteInput != null) {
            this.mExpandedRemoteInput.onNotificationUpdateOrReset();
            if (this.mExpandedRemoteInput.isActive()) {
                this.mPreviousExpandedRemoteInputIntent = this.mExpandedRemoteInput.getPendingIntent();
            }
        }
        if (this.mExpandedChild != null) {
            this.mExpandedChild.animate().cancel();
            removeView(this.mExpandedChild);
            this.mExpandedRemoteInput = null;
        }
        this.mPreviousHeadsUpRemoteInputIntent = null;
        if (this.mHeadsUpRemoteInput != null) {
            this.mHeadsUpRemoteInput.onNotificationUpdateOrReset();
            if (this.mHeadsUpRemoteInput.isActive()) {
                this.mPreviousHeadsUpRemoteInputIntent = this.mHeadsUpRemoteInput.getPendingIntent();
            }
        }
        if (this.mHeadsUpChild != null) {
            this.mHeadsUpChild.animate().cancel();
            removeView(this.mHeadsUpChild);
            this.mHeadsUpRemoteInput = null;
        }
        this.mContractedChild = null;
        this.mExpandedChild = null;
        this.mHeadsUpChild = null;
    }

    public View getContractedChild() {
        return this.mContractedChild;
    }

    public View getExpandedChild() {
        return this.mExpandedChild;
    }

    public View getHeadsUpChild() {
        return this.mHeadsUpChild;
    }

    public void setContractedChild(View child) {
        if (this.mContractedChild != null) {
            this.mContractedChild.animate().cancel();
            removeView(this.mContractedChild);
        }
        addView(child);
        this.mContractedChild = child;
        this.mContractedWrapper = NotificationViewWrapper.wrap(getContext(), child, this.mContainingNotification);
        this.mContractedWrapper.setDark(this.mDark, false, 0L);
    }

    public void setExpandedChild(View child) {
        if (this.mExpandedChild != null) {
            this.mExpandedChild.animate().cancel();
            removeView(this.mExpandedChild);
        }
        addView(child);
        this.mExpandedChild = child;
        this.mExpandedWrapper = NotificationViewWrapper.wrap(getContext(), child, this.mContainingNotification);
    }

    public void setHeadsUpChild(View child) {
        if (this.mHeadsUpChild != null) {
            this.mHeadsUpChild.animate().cancel();
            removeView(this.mHeadsUpChild);
        }
        addView(child);
        this.mHeadsUpChild = child;
        this.mHeadsUpWrapper = NotificationViewWrapper.wrap(getContext(), child, this.mContainingNotification);
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        updateVisibility();
    }

    private void updateVisibility() {
        setVisible(isShown());
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getViewTreeObserver().removeOnPreDrawListener(this.mEnableAnimationPredrawListener);
    }

    private void setVisible(boolean isVisible) {
        if (isVisible) {
            getViewTreeObserver().removeOnPreDrawListener(this.mEnableAnimationPredrawListener);
            getViewTreeObserver().addOnPreDrawListener(this.mEnableAnimationPredrawListener);
        } else {
            getViewTreeObserver().removeOnPreDrawListener(this.mEnableAnimationPredrawListener);
            this.mAnimate = false;
        }
    }

    private void focusExpandButtonIfNecessary() {
        ImageView expandButton;
        if (!this.mFocusOnVisibilityChange) {
            return;
        }
        NotificationHeaderView header = getVisibleNotificationHeader();
        if (header != null && (expandButton = header.getExpandButton()) != null) {
            expandButton.requestAccessibilityFocus();
        }
        this.mFocusOnVisibilityChange = false;
    }

    public void setContentHeight(int contentHeight) {
        this.mContentHeight = Math.max(Math.min(contentHeight, getHeight()), getMinHeight());
        selectLayout(this.mAnimate, false);
        int minHeightHint = getMinContentHeightHint();
        NotificationViewWrapper wrapper = getVisibleWrapper(this.mVisibleType);
        if (wrapper != null) {
            wrapper.setContentHeight(this.mContentHeight, minHeightHint);
        }
        NotificationViewWrapper wrapper2 = getVisibleWrapper(this.mTransformationStartVisibleType);
        if (wrapper2 != null) {
            wrapper2.setContentHeight(this.mContentHeight, minHeightHint);
        }
        updateClipping();
        invalidateOutline();
    }

    private int getMinContentHeightHint() {
        int hint;
        boolean zIsTransitioningFromTo;
        boolean pinned;
        if (this.mIsChildInGroup && isVisibleOrTransitioning(3)) {
            return this.mContext.getResources().getDimensionPixelSize(android.R.dimen.alertDialog_material_letter_spacing_body_1);
        }
        if (this.mHeadsUpChild != null && this.mExpandedChild != null) {
            if (isTransitioningFromTo(2, 1)) {
                zIsTransitioningFromTo = true;
            } else {
                zIsTransitioningFromTo = isTransitioningFromTo(1, 2);
            }
            if (isVisibleOrTransitioning(0)) {
                pinned = false;
            } else {
                pinned = !this.mIsHeadsUp ? this.mHeadsupDisappearRunning : true;
            }
            if (zIsTransitioningFromTo || pinned) {
                return Math.min(this.mHeadsUpChild.getHeight(), this.mExpandedChild.getHeight());
            }
        }
        if (this.mVisibleType == 1 && this.mContentHeightAtAnimationStart >= 0 && this.mExpandedChild != null) {
            return Math.min(this.mContentHeightAtAnimationStart, this.mExpandedChild.getHeight());
        }
        if (this.mHeadsUpChild != null && isVisibleOrTransitioning(2)) {
            hint = this.mHeadsUpChild.getHeight();
        } else if (this.mExpandedChild != null) {
            hint = this.mExpandedChild.getHeight();
        } else {
            hint = this.mContractedChild.getHeight() + this.mContext.getResources().getDimensionPixelSize(android.R.dimen.alertDialog_material_letter_spacing_body_1);
        }
        if (this.mExpandedChild != null && isVisibleOrTransitioning(1)) {
            return Math.min(hint, this.mExpandedChild.getHeight());
        }
        return hint;
    }

    private boolean isTransitioningFromTo(int from, int to) {
        return (this.mTransformationStartVisibleType == from || this.mAnimationStartVisibleType == from) && this.mVisibleType == to;
    }

    private boolean isVisibleOrTransitioning(int type) {
        return this.mVisibleType == type || this.mTransformationStartVisibleType == type || this.mAnimationStartVisibleType == type;
    }

    private void updateContentTransformation() {
        int visibleType = calculateVisibleType();
        if (visibleType != this.mVisibleType) {
            this.mTransformationStartVisibleType = this.mVisibleType;
            TransformableView shownView = getTransformableViewForVisibleType(visibleType);
            TransformableView hiddenView = getTransformableViewForVisibleType(this.mTransformationStartVisibleType);
            shownView.transformFrom(hiddenView, 0.0f);
            getViewForVisibleType(visibleType).setVisibility(0);
            hiddenView.transformTo(shownView, 0.0f);
            this.mVisibleType = visibleType;
            updateBackgroundColor(true);
        }
        if (this.mForceSelectNextLayout) {
            forceUpdateVisibilities();
        }
        if (this.mTransformationStartVisibleType != -1 && this.mVisibleType != this.mTransformationStartVisibleType && getViewForVisibleType(this.mTransformationStartVisibleType) != null) {
            TransformableView shownView2 = getTransformableViewForVisibleType(this.mVisibleType);
            TransformableView hiddenView2 = getTransformableViewForVisibleType(this.mTransformationStartVisibleType);
            float transformationAmount = calculateTransformationAmount();
            shownView2.transformFrom(hiddenView2, transformationAmount);
            hiddenView2.transformTo(shownView2, transformationAmount);
            updateBackgroundTransformation(transformationAmount);
            return;
        }
        updateViewVisibilities(visibleType);
        updateBackgroundColor(false);
    }

    private void updateBackgroundTransformation(float transformationAmount) {
        int endColor = getBackgroundColor(this.mVisibleType);
        int startColor = getBackgroundColor(this.mTransformationStartVisibleType);
        if (endColor != startColor) {
            if (startColor == 0) {
                startColor = this.mContainingNotification.getBackgroundColorWithoutTint();
            }
            if (endColor == 0) {
                endColor = this.mContainingNotification.getBackgroundColorWithoutTint();
            }
            endColor = NotificationUtils.interpolateColors(startColor, endColor, transformationAmount);
        }
        this.mContainingNotification.updateBackgroundAlpha(transformationAmount);
        this.mContainingNotification.setContentBackground(endColor, false, this);
    }

    private float calculateTransformationAmount() {
        int startHeight = getViewForVisibleType(this.mTransformationStartVisibleType).getHeight();
        int endHeight = getViewForVisibleType(this.mVisibleType).getHeight();
        int progress = Math.abs(this.mContentHeight - startHeight);
        int totalDistance = Math.abs(endHeight - startHeight);
        float amount = progress / totalDistance;
        return Math.min(1.0f, amount);
    }

    public int getMaxHeight() {
        if (this.mExpandedChild != null) {
            return this.mExpandedChild.getHeight();
        }
        if (this.mIsHeadsUp && this.mHeadsUpChild != null) {
            return this.mHeadsUpChild.getHeight();
        }
        return this.mContractedChild.getHeight();
    }

    public int getMinHeight() {
        return getMinHeight(false);
    }

    public int getMinHeight(boolean likeGroupExpanded) {
        if (likeGroupExpanded || !this.mIsChildInGroup || isGroupExpanded()) {
            return this.mContractedChild.getHeight();
        }
        return this.mSingleLineView.getHeight();
    }

    private boolean isGroupExpanded() {
        return this.mGroupManager.isGroupExpanded(this.mStatusBarNotification);
    }

    public void setClipTopAmount(int clipTopAmount) {
        this.mClipTopAmount = clipTopAmount;
        updateClipping();
    }

    private void updateClipping() {
        if (this.mClipToActualHeight) {
            this.mClipBounds.set(0, this.mClipTopAmount, getWidth(), this.mContentHeight);
            setClipBounds(this.mClipBounds);
        } else {
            setClipBounds(null);
        }
    }

    public void setClipToActualHeight(boolean clipToActualHeight) {
        this.mClipToActualHeight = clipToActualHeight;
        updateClipping();
    }

    private void selectLayout(boolean animate, boolean force) {
        if (this.mContractedChild == null) {
            return;
        }
        if (this.mUserExpanding) {
            updateContentTransformation();
            return;
        }
        int visibleType = calculateVisibleType();
        boolean changedType = visibleType != this.mVisibleType;
        if (!changedType && !force) {
            return;
        }
        View visibleView = getViewForVisibleType(visibleType);
        if (visibleView != null) {
            visibleView.setVisibility(0);
            transferRemoteInputFocus(visibleType);
        }
        NotificationViewWrapper visibleWrapper = getVisibleWrapper(visibleType);
        if (visibleWrapper != null) {
            visibleWrapper.setContentHeight(this.mContentHeight, getMinContentHeightHint());
        }
        if (animate && ((visibleType == 1 && this.mExpandedChild != null) || ((visibleType == 2 && this.mHeadsUpChild != null) || ((visibleType == 3 && this.mSingleLineView != null) || visibleType == 0)))) {
            animateToVisibleType(visibleType);
        } else {
            updateViewVisibilities(visibleType);
        }
        this.mVisibleType = visibleType;
        if (changedType) {
            focusExpandButtonIfNecessary();
        }
        updateBackgroundColor(animate);
    }

    private void forceUpdateVisibilities() {
        boolean contractedVisible = this.mVisibleType == 0 || this.mTransformationStartVisibleType == 0;
        boolean expandedVisible = this.mVisibleType == 1 || this.mTransformationStartVisibleType == 1;
        boolean headsUpVisible = this.mVisibleType == 2 || this.mTransformationStartVisibleType == 2;
        boolean singleLineVisible = this.mVisibleType == 3 || this.mTransformationStartVisibleType == 3;
        if (!contractedVisible) {
            this.mContractedChild.setVisibility(4);
        } else {
            this.mContractedWrapper.setVisible(true);
        }
        if (this.mExpandedChild != null) {
            if (!expandedVisible) {
                this.mExpandedChild.setVisibility(4);
            } else {
                this.mExpandedWrapper.setVisible(true);
            }
        }
        if (this.mHeadsUpChild != null) {
            if (!headsUpVisible) {
                this.mHeadsUpChild.setVisibility(4);
            } else {
                this.mHeadsUpWrapper.setVisible(true);
            }
        }
        if (this.mSingleLineView == null) {
            return;
        }
        if (!singleLineVisible) {
            this.mSingleLineView.setVisibility(4);
        } else {
            this.mSingleLineView.setVisible(true);
        }
    }

    public void updateBackgroundColor(boolean animate) {
        int customBackgroundColor = getBackgroundColor(this.mVisibleType);
        this.mContainingNotification.resetBackgroundAlpha();
        this.mContainingNotification.setContentBackground(customBackgroundColor, animate, this);
    }

    public int getVisibleType() {
        return this.mVisibleType;
    }

    public int getBackgroundColorForExpansionState() {
        int visibleType;
        if (this.mContainingNotification.isGroupExpanded() || this.mContainingNotification.isUserLocked()) {
            visibleType = calculateVisibleType();
        } else {
            visibleType = getVisibleType();
        }
        return getBackgroundColor(visibleType);
    }

    public int getBackgroundColor(int visibleType) {
        NotificationViewWrapper currentVisibleWrapper = getVisibleWrapper(visibleType);
        if (currentVisibleWrapper == null) {
            return 0;
        }
        int customBackgroundColor = currentVisibleWrapper.getCustomBackgroundColor();
        return customBackgroundColor;
    }

    private void updateViewVisibilities(int visibleType) {
        boolean contractedVisible = visibleType == 0;
        this.mContractedWrapper.setVisible(contractedVisible);
        if (this.mExpandedChild != null) {
            boolean expandedVisible = visibleType == 1;
            this.mExpandedWrapper.setVisible(expandedVisible);
        }
        if (this.mHeadsUpChild != null) {
            boolean headsUpVisible = visibleType == 2;
            this.mHeadsUpWrapper.setVisible(headsUpVisible);
        }
        if (this.mSingleLineView == null) {
            return;
        }
        boolean singleLineVisible = visibleType == 3;
        this.mSingleLineView.setVisible(singleLineVisible);
    }

    private void animateToVisibleType(int visibleType) {
        TransformableView shownView = getTransformableViewForVisibleType(visibleType);
        final TransformableView hiddenView = getTransformableViewForVisibleType(this.mVisibleType);
        if (shownView == hiddenView || hiddenView == null) {
            shownView.setVisible(true);
            return;
        }
        this.mAnimationStartVisibleType = this.mVisibleType;
        shownView.transformFrom(hiddenView);
        getViewForVisibleType(visibleType).setVisibility(0);
        hiddenView.transformTo(shownView, new Runnable() {
            @Override
            public void run() {
                if (hiddenView != NotificationContentView.this.getTransformableViewForVisibleType(NotificationContentView.this.mVisibleType)) {
                    hiddenView.setVisible(false);
                }
                NotificationContentView.this.mAnimationStartVisibleType = -1;
            }
        });
    }

    private void transferRemoteInputFocus(int visibleType) {
        if (visibleType == 2 && this.mHeadsUpRemoteInput != null && this.mExpandedRemoteInput != null && this.mExpandedRemoteInput.isActive()) {
            this.mHeadsUpRemoteInput.stealFocusFrom(this.mExpandedRemoteInput);
        }
        if (visibleType != 1 || this.mExpandedRemoteInput == null || this.mHeadsUpRemoteInput == null || !this.mHeadsUpRemoteInput.isActive()) {
            return;
        }
        this.mExpandedRemoteInput.stealFocusFrom(this.mHeadsUpRemoteInput);
    }

    public TransformableView getTransformableViewForVisibleType(int visibleType) {
        switch (visibleType) {
            case 1:
                return this.mExpandedWrapper;
            case 2:
                return this.mHeadsUpWrapper;
            case 3:
                return this.mSingleLineView;
            default:
                return this.mContractedWrapper;
        }
    }

    private View getViewForVisibleType(int visibleType) {
        switch (visibleType) {
            case 1:
                return this.mExpandedChild;
            case 2:
                return this.mHeadsUpChild;
            case 3:
                return this.mSingleLineView;
            default:
                return this.mContractedChild;
        }
    }

    private NotificationViewWrapper getVisibleWrapper(int visibleType) {
        switch (visibleType) {
            case 0:
                return this.mContractedWrapper;
            case 1:
                return this.mExpandedWrapper;
            case 2:
                return this.mHeadsUpWrapper;
            default:
                return null;
        }
    }

    public int calculateVisibleType() {
        int height;
        int collapsedVisualType;
        if (this.mUserExpanding) {
            if (!this.mIsChildInGroup || isGroupExpanded() || this.mContainingNotification.isExpanded(true)) {
                height = this.mContainingNotification.getMaxContentHeight();
            } else {
                height = this.mContainingNotification.getShowingLayout().getMinHeight();
            }
            if (height == 0) {
                height = this.mContentHeight;
            }
            int expandedVisualType = getVisualTypeForHeight(height);
            if (this.mIsChildInGroup && !isGroupExpanded()) {
                collapsedVisualType = 3;
            } else {
                collapsedVisualType = getVisualTypeForHeight(this.mContainingNotification.getCollapsedHeight());
            }
            if (this.mTransformationStartVisibleType == collapsedVisualType) {
                return expandedVisualType;
            }
            int expandedVisualType2 = collapsedVisualType;
            return expandedVisualType2;
        }
        int intrinsicHeight = this.mContainingNotification.getIntrinsicHeight();
        int viewHeight = this.mContentHeight;
        if (intrinsicHeight != 0) {
            viewHeight = Math.min(this.mContentHeight, intrinsicHeight);
        }
        return getVisualTypeForHeight(viewHeight);
    }

    private int getVisualTypeForHeight(float viewHeight) {
        boolean noExpandedChild = this.mExpandedChild == null;
        if (!noExpandedChild && viewHeight == this.mExpandedChild.getHeight()) {
            return 1;
        }
        if (this.mUserExpanding || !this.mIsChildInGroup || isGroupExpanded()) {
            return ((this.mIsHeadsUp || this.mHeadsupDisappearRunning) && this.mHeadsUpChild != null) ? (viewHeight <= ((float) this.mHeadsUpChild.getHeight()) || noExpandedChild) ? 2 : 1 : (noExpandedChild || (viewHeight <= ((float) this.mContractedChild.getHeight()) && !(this.mIsChildInGroup && !isGroupExpanded() && this.mContainingNotification.isExpanded(true)))) ? 0 : 1;
        }
        return 3;
    }

    public boolean isContentExpandable() {
        return this.mExpandedChild != null;
    }

    public void setDark(boolean dark, boolean fade, long delay) {
        if (this.mContractedChild == null) {
            return;
        }
        this.mDark = dark;
        if (this.mVisibleType == 0 || !dark) {
            this.mContractedWrapper.setDark(dark, fade, delay);
        }
        if (this.mVisibleType == 1 || (this.mExpandedChild != null && !dark)) {
            this.mExpandedWrapper.setDark(dark, fade, delay);
        }
        if (this.mVisibleType == 2 || (this.mHeadsUpChild != null && !dark)) {
            this.mHeadsUpWrapper.setDark(dark, fade, delay);
        }
        if (this.mSingleLineView != null) {
            if (this.mVisibleType != 3 && dark) {
                return;
            }
            this.mSingleLineView.setDark(dark, fade, delay);
        }
    }

    public void setHeadsUp(boolean headsUp) {
        this.mIsHeadsUp = headsUp;
        selectLayout(false, true);
        updateExpandButtons(this.mExpandable);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    public void setShowingLegacyBackground(boolean showing) {
        this.mShowingLegacyBackground = showing;
        updateShowingLegacyBackground();
    }

    private void updateShowingLegacyBackground() {
        if (this.mContractedChild != null) {
            this.mContractedWrapper.setShowingLegacyBackground(this.mShowingLegacyBackground);
        }
        if (this.mExpandedChild != null) {
            this.mExpandedWrapper.setShowingLegacyBackground(this.mShowingLegacyBackground);
        }
        if (this.mHeadsUpChild == null) {
            return;
        }
        this.mHeadsUpWrapper.setShowingLegacyBackground(this.mShowingLegacyBackground);
    }

    public void setIsChildInGroup(boolean isChildInGroup) {
        this.mIsChildInGroup = isChildInGroup;
        updateSingleLineView();
    }

    public void onNotificationUpdated(NotificationData.Entry entry) {
        this.mStatusBarNotification = entry.notification;
        this.mBeforeN = entry.targetSdk < 24;
        updateSingleLineView();
        applyRemoteInput(entry);
        if (this.mContractedChild != null) {
            this.mContractedWrapper.notifyContentUpdated(entry.notification);
        }
        if (this.mExpandedChild != null) {
            this.mExpandedWrapper.notifyContentUpdated(entry.notification);
        }
        if (this.mHeadsUpChild != null) {
            this.mHeadsUpWrapper.notifyContentUpdated(entry.notification);
        }
        updateShowingLegacyBackground();
        this.mForceSelectNextLayout = true;
        setDark(this.mDark, false, 0L);
        this.mPreviousExpandedRemoteInputIntent = null;
        this.mPreviousHeadsUpRemoteInputIntent = null;
    }

    private void updateSingleLineView() {
        if (this.mIsChildInGroup) {
            this.mSingleLineView = this.mHybridGroupManager.bindFromNotification(this.mSingleLineView, this.mStatusBarNotification.getNotification());
        } else {
            if (this.mSingleLineView == null) {
                return;
            }
            removeView(this.mSingleLineView);
            this.mSingleLineView = null;
        }
    }

    private void applyRemoteInput(NotificationData.Entry entry) {
        if (this.mRemoteInputController == null) {
            return;
        }
        boolean hasRemoteInput = false;
        Notification.Action[] actions = entry.notification.getNotification().actions;
        if (actions != null) {
            for (Notification.Action a : actions) {
                if (a.getRemoteInputs() != null) {
                    RemoteInput[] remoteInputs = a.getRemoteInputs();
                    int length = remoteInputs.length;
                    int i = 0;
                    while (true) {
                        if (i < length) {
                            RemoteInput ri = remoteInputs[i];
                            if (!ri.getAllowFreeFormInput()) {
                                i++;
                            } else {
                                hasRemoteInput = true;
                                break;
                            }
                        }
                    }
                }
            }
        }
        View bigContentView = this.mExpandedChild;
        if (bigContentView != null) {
            this.mExpandedRemoteInput = applyRemoteInput(bigContentView, entry, hasRemoteInput, this.mPreviousExpandedRemoteInputIntent);
        } else {
            this.mExpandedRemoteInput = null;
        }
        View headsUpContentView = this.mHeadsUpChild;
        if (headsUpContentView != null) {
            this.mHeadsUpRemoteInput = applyRemoteInput(headsUpContentView, entry, hasRemoteInput, this.mPreviousHeadsUpRemoteInputIntent);
        } else {
            this.mHeadsUpRemoteInput = null;
        }
    }

    private RemoteInputView applyRemoteInput(View view, NotificationData.Entry entry, boolean hasRemoteInput, PendingIntent existingPendingIntent) {
        View actionContainerCandidate = view.findViewById(android.R.id.issued_to_header);
        if (!(actionContainerCandidate instanceof FrameLayout)) {
            return null;
        }
        RemoteInputView existing = (RemoteInputView) view.findViewWithTag(RemoteInputView.VIEW_TAG);
        if (existing != null) {
            existing.onNotificationUpdateOrReset();
        }
        if (existing == null && hasRemoteInput) {
            ViewGroup actionContainer = (FrameLayout) actionContainerCandidate;
            RemoteInputView riv = RemoteInputView.inflate(this.mContext, actionContainer, entry, this.mRemoteInputController);
            riv.setVisibility(4);
            actionContainer.addView(riv, new FrameLayout.LayoutParams(-1, -1));
            existing = riv;
        }
        if (hasRemoteInput) {
            int color = entry.notification.getNotification().color;
            if (color == 0) {
                color = this.mContext.getColor(R.color.default_remote_input_background);
            }
            existing.setBackgroundColor(NotificationColorUtil.ensureTextBackgroundColor(color, this.mContext.getColor(R.color.remote_input_text_enabled), this.mContext.getColor(R.color.remote_input_hint)));
            if (existingPendingIntent != null || existing.isActive()) {
                Notification.Action[] actions = entry.notification.getNotification().actions;
                if (existingPendingIntent != null) {
                    existing.setPendingIntent(existingPendingIntent);
                }
                if (existing.updatePendingIntentFromActions(actions)) {
                    if (!existing.isActive()) {
                        existing.focus();
                    }
                } else if (existing.isActive()) {
                    existing.close();
                }
            }
        }
        return existing;
    }

    public void closeRemoteInput() {
        if (this.mHeadsUpRemoteInput != null) {
            this.mHeadsUpRemoteInput.close();
        }
        if (this.mExpandedRemoteInput == null) {
            return;
        }
        this.mExpandedRemoteInput.close();
    }

    public void setGroupManager(NotificationGroupManager groupManager) {
        this.mGroupManager = groupManager;
    }

    public void setRemoteInputController(RemoteInputController r) {
        this.mRemoteInputController = r;
    }

    public void setExpandClickListener(View.OnClickListener expandClickListener) {
        this.mExpandClickListener = expandClickListener;
    }

    public void updateExpandButtons(boolean expandable) {
        this.mExpandable = expandable;
        if (this.mExpandedChild != null && this.mExpandedChild.getHeight() != 0) {
            if (!this.mIsHeadsUp || this.mHeadsUpChild == null) {
                if (this.mExpandedChild.getHeight() == this.mContractedChild.getHeight()) {
                    expandable = false;
                }
            } else if (this.mExpandedChild.getHeight() == this.mHeadsUpChild.getHeight()) {
                expandable = false;
            }
        }
        if (this.mExpandedChild != null) {
            this.mExpandedWrapper.updateExpandability(expandable, this.mExpandClickListener);
        }
        if (this.mContractedChild != null) {
            this.mContractedWrapper.updateExpandability(expandable, this.mExpandClickListener);
        }
        if (this.mHeadsUpChild == null) {
            return;
        }
        this.mHeadsUpWrapper.updateExpandability(expandable, this.mExpandClickListener);
    }

    public NotificationHeaderView getNotificationHeader() {
        NotificationHeaderView header = null;
        if (this.mContractedChild != null) {
            header = this.mContractedWrapper.getNotificationHeader();
        }
        if (header == null && this.mExpandedChild != null) {
            header = this.mExpandedWrapper.getNotificationHeader();
        }
        if (header == null && this.mHeadsUpChild != null) {
            return this.mHeadsUpWrapper.getNotificationHeader();
        }
        return header;
    }

    public NotificationHeaderView getVisibleNotificationHeader() {
        NotificationViewWrapper wrapper = getVisibleWrapper(this.mVisibleType);
        if (wrapper == null) {
            return null;
        }
        return wrapper.getNotificationHeader();
    }

    public void setContainingNotification(ExpandableNotificationRow containingNotification) {
        this.mContainingNotification = containingNotification;
    }

    public void requestSelectLayout(boolean needsAnimation) {
        selectLayout(needsAnimation, false);
    }

    public void reInflateViews() {
        if (!this.mIsChildInGroup || this.mSingleLineView == null) {
            return;
        }
        removeView(this.mSingleLineView);
        this.mSingleLineView = null;
        updateSingleLineView();
    }

    public void setUserExpanding(boolean userExpanding) {
        this.mUserExpanding = userExpanding;
        if (userExpanding) {
            this.mTransformationStartVisibleType = this.mVisibleType;
            return;
        }
        this.mTransformationStartVisibleType = -1;
        this.mVisibleType = calculateVisibleType();
        updateViewVisibilities(this.mVisibleType);
        updateBackgroundColor(false);
    }

    public void setSingleLineWidthIndention(int singleLineWidthIndention) {
        if (singleLineWidthIndention == this.mSingleLineWidthIndention) {
            return;
        }
        this.mSingleLineWidthIndention = singleLineWidthIndention;
        this.mContainingNotification.forceLayout();
        forceLayout();
    }

    public HybridNotificationView getSingleLineView() {
        return this.mSingleLineView;
    }

    public void setRemoved() {
        if (this.mExpandedRemoteInput != null) {
            this.mExpandedRemoteInput.setRemoved();
        }
        if (this.mHeadsUpRemoteInput == null) {
            return;
        }
        this.mHeadsUpRemoteInput.setRemoved();
    }

    public void setContentHeightAnimating(boolean animating) {
        if (animating) {
            return;
        }
        this.mContentHeightAtAnimationStart = -1;
    }

    public void setHeadsupDisappearRunning(boolean headsupDisappearRunning) {
        this.mHeadsupDisappearRunning = headsupDisappearRunning;
        selectLayout(false, true);
    }

    public void setFocusOnVisibilityChange() {
        this.mFocusOnVisibilityChange = true;
    }
}
