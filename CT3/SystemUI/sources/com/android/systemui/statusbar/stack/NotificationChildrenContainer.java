package com.android.systemui.statusbar.stack;

import android.app.Notification;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.service.notification.StatusBarNotification;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.NotificationHeaderView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RemoteViews;
import android.widget.TextView;
import com.android.systemui.R;
import com.android.systemui.ViewInvertHelper;
import com.android.systemui.statusbar.CrossFadeHelper;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.NotificationHeaderUtil;
import com.android.systemui.statusbar.notification.HybridGroupManager;
import com.android.systemui.statusbar.notification.HybridNotificationView;
import com.android.systemui.statusbar.notification.NotificationUtils;
import com.android.systemui.statusbar.notification.NotificationViewWrapper;
import java.util.ArrayList;
import java.util.List;

public class NotificationChildrenContainer extends ViewGroup {
    private int mActualHeight;
    private int mChildPadding;
    private final List<ExpandableNotificationRow> mChildren;
    private boolean mChildrenExpanded;
    private float mCollapsedBottompadding;
    private int mDividerHeight;
    private final List<View> mDividers;
    private ViewState mGroupOverFlowState;
    private int mHeaderHeight;
    private NotificationHeaderUtil mHeaderUtil;
    private ViewState mHeaderViewState;
    private final HybridGroupManager mHybridGroupManager;
    private int mMaxNotificationHeight;
    private boolean mNeverAppliedGroupState;
    private NotificationHeaderView mNotificationHeader;
    private int mNotificationHeaderMargin;
    private NotificationViewWrapper mNotificationHeaderWrapper;
    private ExpandableNotificationRow mNotificationParent;
    private int mNotificatonTopPadding;
    private ViewInvertHelper mOverflowInvertHelper;
    private TextView mOverflowNumber;
    private int mRealHeight;
    private boolean mUserLocked;

    public NotificationChildrenContainer(Context context) {
        this(context, null);
    }

    public NotificationChildrenContainer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NotificationChildrenContainer(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public NotificationChildrenContainer(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mDividers = new ArrayList();
        this.mChildren = new ArrayList();
        initDimens();
        this.mHybridGroupManager = new HybridGroupManager(getContext(), this);
    }

    private void initDimens() {
        this.mChildPadding = getResources().getDimensionPixelSize(R.dimen.notification_children_padding);
        this.mDividerHeight = Math.max(1, getResources().getDimensionPixelSize(R.dimen.notification_divider_height));
        this.mHeaderHeight = getResources().getDimensionPixelSize(R.dimen.notification_header_height);
        this.mMaxNotificationHeight = getResources().getDimensionPixelSize(R.dimen.notification_max_height);
        this.mNotificationHeaderMargin = getResources().getDimensionPixelSize(android.R.dimen.alertDialog_material_letter_spacing_title);
        this.mNotificatonTopPadding = getResources().getDimensionPixelSize(R.dimen.notification_children_container_top_padding);
        this.mCollapsedBottompadding = getResources().getDimensionPixelSize(android.R.dimen.alertDialog_material_line_height_body_1);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int childCount = Math.min(this.mChildren.size(), 8);
        for (int i = 0; i < childCount; i++) {
            View child = this.mChildren.get(i);
            child.layout(0, 0, child.getMeasuredWidth(), child.getMeasuredHeight());
            this.mDividers.get(i).layout(0, 0, getWidth(), this.mDividerHeight);
        }
        if (this.mOverflowNumber != null) {
            this.mOverflowNumber.layout(getWidth() - this.mOverflowNumber.getMeasuredWidth(), 0, getWidth(), this.mOverflowNumber.getMeasuredHeight());
        }
        if (this.mNotificationHeader == null) {
            return;
        }
        this.mNotificationHeader.layout(0, 0, this.mNotificationHeader.getMeasuredWidth(), this.mNotificationHeader.getMeasuredHeight());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measuredWidth;
        int ownMaxHeight = this.mMaxNotificationHeight;
        int heightMode = View.MeasureSpec.getMode(heightMeasureSpec);
        boolean hasFixedHeight = heightMode == 1073741824;
        boolean isHeightLimited = heightMode == Integer.MIN_VALUE;
        int size = View.MeasureSpec.getSize(heightMeasureSpec);
        if (hasFixedHeight || isHeightLimited) {
            ownMaxHeight = Math.min(ownMaxHeight, size);
        }
        int newHeightSpec = View.MeasureSpec.makeMeasureSpec(ownMaxHeight, Integer.MIN_VALUE);
        int width = View.MeasureSpec.getSize(widthMeasureSpec);
        if (this.mOverflowNumber != null) {
            this.mOverflowNumber.measure(View.MeasureSpec.makeMeasureSpec(width, Integer.MIN_VALUE), newHeightSpec);
        }
        int dividerHeightSpec = View.MeasureSpec.makeMeasureSpec(this.mDividerHeight, 1073741824);
        int height = this.mNotificationHeaderMargin + this.mNotificatonTopPadding;
        int childCount = Math.min(this.mChildren.size(), 8);
        int collapsedChildren = getMaxAllowedVisibleChildren(true);
        int overflowIndex = childCount > collapsedChildren ? collapsedChildren - 1 : -1;
        int i = 0;
        while (i < childCount) {
            ExpandableNotificationRow child = this.mChildren.get(i);
            boolean isOverflow = i == overflowIndex;
            if (isOverflow && this.mOverflowNumber != null) {
                measuredWidth = this.mOverflowNumber.getMeasuredWidth();
            } else {
                measuredWidth = 0;
            }
            child.setSingleLineWidthIndention(measuredWidth);
            child.measure(widthMeasureSpec, newHeightSpec);
            View divider = this.mDividers.get(i);
            divider.measure(widthMeasureSpec, dividerHeightSpec);
            if (child.getVisibility() != 8) {
                height += child.getMeasuredHeight() + this.mDividerHeight;
            }
            i++;
        }
        this.mRealHeight = height;
        if (heightMode != 0) {
            height = Math.min(height, size);
        }
        if (this.mNotificationHeader != null) {
            int headerHeightSpec = View.MeasureSpec.makeMeasureSpec(this.mHeaderHeight, 1073741824);
            this.mNotificationHeader.measure(widthMeasureSpec, headerHeightSpec);
        }
        setMeasuredDimension(width, height);
    }

    public boolean pointInView(float localX, float localY, float slop) {
        return localX >= (-slop) && localY >= (-slop) && localX < ((float) (this.mRight - this.mLeft)) + slop && localY < ((float) this.mRealHeight) + slop;
    }

    public void addNotification(ExpandableNotificationRow row, int childIndex) {
        int newIndex = childIndex < 0 ? this.mChildren.size() : childIndex;
        this.mChildren.add(newIndex, row);
        addView(row);
        row.setUserLocked(this.mUserLocked);
        View divider = inflateDivider();
        addView(divider);
        this.mDividers.add(newIndex, divider);
        updateGroupOverflow();
    }

    public void removeNotification(ExpandableNotificationRow row) {
        int childIndex = this.mChildren.indexOf(row);
        this.mChildren.remove(row);
        removeView(row);
        final View divider = this.mDividers.remove(childIndex);
        removeView(divider);
        getOverlay().add(divider);
        CrossFadeHelper.fadeOut(divider, new Runnable() {
            @Override
            public void run() {
                NotificationChildrenContainer.this.getOverlay().remove(divider);
            }
        });
        row.setSystemChildExpanded(false);
        row.setUserLocked(false);
        updateGroupOverflow();
        if (row.isRemoved()) {
            return;
        }
        this.mHeaderUtil.restoreNotificationHeader(row);
    }

    public int getNotificationChildCount() {
        return this.mChildren.size();
    }

    public void recreateNotificationHeader(View.OnClickListener listener, StatusBarNotification notification) {
        Notification.Builder builder = Notification.Builder.recoverBuilder(getContext(), this.mNotificationParent.getStatusBarNotification().getNotification());
        RemoteViews header = builder.makeNotificationHeader();
        if (this.mNotificationHeader == null) {
            this.mNotificationHeader = header.apply(getContext(), this);
            View expandButton = this.mNotificationHeader.findViewById(android.R.id.label_hour);
            expandButton.setVisibility(0);
            this.mNotificationHeader.setOnClickListener(listener);
            this.mNotificationHeaderWrapper = NotificationViewWrapper.wrap(getContext(), this.mNotificationHeader, this.mNotificationParent);
            addView((View) this.mNotificationHeader, 0);
            invalidate();
        } else {
            header.reapply(getContext(), this.mNotificationHeader);
            this.mNotificationHeaderWrapper.notifyContentUpdated(notification);
        }
        updateChildrenHeaderAppearance();
    }

    public void updateChildrenHeaderAppearance() {
        this.mHeaderUtil.updateChildrenHeaderAppearance();
    }

    public void updateGroupOverflow() {
        int childCount = this.mChildren.size();
        int maxAllowedVisibleChildren = getMaxAllowedVisibleChildren(true);
        if (childCount > maxAllowedVisibleChildren) {
            this.mOverflowNumber = this.mHybridGroupManager.bindOverflowNumber(this.mOverflowNumber, childCount - maxAllowedVisibleChildren);
            if (this.mOverflowInvertHelper == null) {
                this.mOverflowInvertHelper = new ViewInvertHelper(this.mOverflowNumber, 700L);
            }
            if (this.mGroupOverFlowState != null) {
                return;
            }
            this.mGroupOverFlowState = new ViewState();
            this.mNeverAppliedGroupState = true;
            return;
        }
        if (this.mOverflowNumber == null) {
            return;
        }
        removeView(this.mOverflowNumber);
        if (isShown()) {
            final View removedOverflowNumber = this.mOverflowNumber;
            addTransientView(removedOverflowNumber, getTransientViewCount());
            CrossFadeHelper.fadeOut(removedOverflowNumber, new Runnable() {
                @Override
                public void run() {
                    NotificationChildrenContainer.this.removeTransientView(removedOverflowNumber);
                }
            });
        }
        this.mOverflowNumber = null;
        this.mOverflowInvertHelper = null;
        this.mGroupOverFlowState = null;
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateGroupOverflow();
    }

    private View inflateDivider() {
        return LayoutInflater.from(this.mContext).inflate(R.layout.notification_children_divider, (ViewGroup) this, false);
    }

    public List<ExpandableNotificationRow> getNotificationChildren() {
        return this.mChildren;
    }

    public boolean applyChildOrder(List<ExpandableNotificationRow> childOrder) {
        if (childOrder == null) {
            return false;
        }
        boolean result = false;
        for (int i = 0; i < this.mChildren.size() && i < childOrder.size(); i++) {
            ExpandableNotificationRow child = this.mChildren.get(i);
            ExpandableNotificationRow desiredChild = childOrder.get(i);
            if (child != desiredChild) {
                this.mChildren.remove(desiredChild);
                this.mChildren.add(i, desiredChild);
                result = true;
            }
        }
        updateExpansionStates();
        return result;
    }

    private void updateExpansionStates() {
        if (this.mChildrenExpanded || this.mUserLocked) {
            return;
        }
        int size = this.mChildren.size();
        int i = 0;
        while (i < size) {
            ExpandableNotificationRow child = this.mChildren.get(i);
            child.setSystemChildExpanded(i == 0 && size == 1);
            i++;
        }
    }

    public int getIntrinsicHeight() {
        int maxAllowedVisibleChildren = getMaxAllowedVisibleChildren();
        return getIntrinsicHeight(maxAllowedVisibleChildren);
    }

    private int getIntrinsicHeight(float maxAllowedVisibleChildren) {
        int i;
        int intrinsicHeight;
        int intrinsicHeight2 = this.mNotificationHeaderMargin;
        int visibleChildren = 0;
        int childCount = this.mChildren.size();
        boolean firstChild = true;
        float expandFactor = 0.0f;
        if (this.mUserLocked) {
            expandFactor = getGroupExpandFraction();
        }
        for (int i2 = 0; i2 < childCount && visibleChildren < maxAllowedVisibleChildren; i2++) {
            if (!firstChild) {
                if (this.mUserLocked) {
                    intrinsicHeight = (int) (intrinsicHeight2 + NotificationUtils.interpolate(this.mChildPadding, this.mDividerHeight, expandFactor));
                } else {
                    intrinsicHeight = intrinsicHeight2 + (this.mChildrenExpanded ? this.mDividerHeight : this.mChildPadding);
                }
            } else {
                if (this.mUserLocked) {
                    intrinsicHeight = (int) (intrinsicHeight2 + NotificationUtils.interpolate(0.0f, this.mNotificatonTopPadding + this.mDividerHeight, expandFactor));
                } else {
                    if (this.mChildrenExpanded) {
                        i = this.mNotificatonTopPadding + this.mDividerHeight;
                    } else {
                        i = 0;
                    }
                    intrinsicHeight = intrinsicHeight2 + i;
                }
                firstChild = false;
            }
            ExpandableNotificationRow child = this.mChildren.get(i2);
            intrinsicHeight2 = intrinsicHeight + child.getIntrinsicHeight();
            visibleChildren++;
        }
        if (this.mUserLocked) {
            return (int) (intrinsicHeight2 + NotificationUtils.interpolate(this.mCollapsedBottompadding, 0.0f, expandFactor));
        }
        if (!this.mChildrenExpanded) {
            return (int) (intrinsicHeight2 + this.mCollapsedBottompadding);
        }
        return intrinsicHeight2;
    }

    public void getState(StackScrollState resultState, StackViewState parentState) {
        boolean z;
        float translationZ;
        int yPosition;
        float translationZ2;
        int childCount = this.mChildren.size();
        int yPosition2 = this.mNotificationHeaderMargin;
        boolean firstChild = true;
        int maxAllowedVisibleChildren = getMaxAllowedVisibleChildren();
        int lastVisibleIndex = maxAllowedVisibleChildren - 1;
        int firstOverflowIndex = lastVisibleIndex + 1;
        float expandFactor = 0.0f;
        if (this.mUserLocked) {
            expandFactor = getGroupExpandFraction();
            firstOverflowIndex = getMaxAllowedVisibleChildren(true);
        }
        if (this.mNotificationParent.isGroupExpansionChanging()) {
            z = false;
        } else {
            z = this.mChildrenExpanded;
        }
        int parentHeight = parentState.height;
        for (int i = 0; i < childCount; i++) {
            ExpandableNotificationRow child = this.mChildren.get(i);
            if (!firstChild) {
                if (this.mUserLocked) {
                    yPosition = (int) (yPosition2 + NotificationUtils.interpolate(this.mChildPadding, this.mDividerHeight, expandFactor));
                } else {
                    yPosition = yPosition2 + (this.mChildrenExpanded ? this.mDividerHeight : this.mChildPadding);
                }
            } else {
                if (this.mUserLocked) {
                    yPosition = (int) (yPosition2 + NotificationUtils.interpolate(0.0f, this.mNotificatonTopPadding + this.mDividerHeight, expandFactor));
                } else {
                    yPosition = yPosition2 + (this.mChildrenExpanded ? this.mNotificatonTopPadding + this.mDividerHeight : 0);
                }
                firstChild = false;
            }
            StackViewState childState = resultState.getViewStateForView(child);
            int intrinsicHeight = child.getIntrinsicHeight();
            if (z) {
                if (updateChildStateForExpandedGroup(child, parentHeight, childState, yPosition)) {
                    childState.isBottomClipped = true;
                }
            } else {
                childState.hidden = false;
                childState.height = intrinsicHeight;
                childState.isBottomClipped = false;
            }
            childState.yTranslation = yPosition;
            if (z) {
                translationZ2 = this.mNotificationParent.getTranslationZ();
            } else {
                translationZ2 = 0.0f;
            }
            childState.zTranslation = translationZ2;
            childState.dimmed = parentState.dimmed;
            childState.dark = parentState.dark;
            childState.hideSensitive = parentState.hideSensitive;
            childState.belowSpeedBump = parentState.belowSpeedBump;
            childState.clipTopAmount = 0;
            childState.alpha = 0.0f;
            if (i < firstOverflowIndex) {
                childState.alpha = 1.0f;
            } else if (expandFactor == 1.0f && i <= lastVisibleIndex) {
                childState.alpha = (this.mActualHeight - childState.yTranslation) / childState.height;
                childState.alpha = Math.max(0.0f, Math.min(1.0f, childState.alpha));
            }
            childState.location = parentState.location;
            yPosition2 = yPosition + intrinsicHeight;
        }
        if (this.mOverflowNumber != null) {
            ExpandableNotificationRow overflowView = this.mChildren.get(Math.min(getMaxAllowedVisibleChildren(true), childCount) - 1);
            this.mGroupOverFlowState.copyFrom(resultState.getViewStateForView(overflowView));
            if (!this.mChildrenExpanded) {
                if (this.mUserLocked) {
                    HybridNotificationView singleLineView = overflowView.getSingleLineView();
                    View mirrorView = singleLineView.getTextView();
                    if (mirrorView.getVisibility() == 8) {
                        mirrorView = singleLineView.getTitleView();
                    }
                    if (mirrorView.getVisibility() == 8) {
                        mirrorView = singleLineView;
                    }
                    this.mGroupOverFlowState.yTranslation += NotificationUtils.getRelativeYOffset(mirrorView, overflowView);
                    this.mGroupOverFlowState.alpha = mirrorView.getAlpha();
                }
            } else {
                this.mGroupOverFlowState.yTranslation += this.mNotificationHeaderMargin;
                this.mGroupOverFlowState.alpha = 0.0f;
            }
        }
        if (this.mNotificationHeader == null) {
            return;
        }
        if (this.mHeaderViewState == null) {
            this.mHeaderViewState = new ViewState();
        }
        this.mHeaderViewState.initFrom(this.mNotificationHeader);
        ViewState viewState = this.mHeaderViewState;
        if (z) {
            translationZ = this.mNotificationParent.getTranslationZ();
        } else {
            translationZ = 0.0f;
        }
        viewState.zTranslation = translationZ;
    }

    private boolean updateChildStateForExpandedGroup(ExpandableNotificationRow child, int parentHeight, StackViewState childState, int yPosition) {
        int top = yPosition + child.getClipTopAmount();
        int intrinsicHeight = child.getIntrinsicHeight();
        int bottom = top + intrinsicHeight;
        int newHeight = intrinsicHeight;
        if (bottom >= parentHeight) {
            newHeight = Math.max(parentHeight - top, 0);
        }
        childState.hidden = newHeight == 0;
        childState.height = newHeight;
        return (childState.height == intrinsicHeight || childState.hidden) ? false : true;
    }

    private int getMaxAllowedVisibleChildren() {
        return getMaxAllowedVisibleChildren(false);
    }

    private int getMaxAllowedVisibleChildren(boolean likeCollapsed) {
        if (!likeCollapsed && (this.mChildrenExpanded || this.mNotificationParent.isUserLocked())) {
            return 8;
        }
        if (!this.mNotificationParent.isOnKeyguard()) {
            if (this.mNotificationParent.isExpanded() || this.mNotificationParent.isHeadsUp()) {
                return 5;
            }
            return 2;
        }
        return 2;
    }

    public void applyState(StackScrollState state) {
        boolean zIsGroupExpansionChanging;
        int childCount = this.mChildren.size();
        ViewState tmpState = new ViewState();
        float expandFraction = 0.0f;
        if (this.mUserLocked) {
            expandFraction = getGroupExpandFraction();
        }
        if (this.mUserLocked) {
            zIsGroupExpansionChanging = true;
        } else {
            zIsGroupExpansionChanging = this.mNotificationParent.isGroupExpansionChanging();
        }
        for (int i = 0; i < childCount; i++) {
            ExpandableNotificationRow child = this.mChildren.get(i);
            StackViewState viewState = state.getViewStateForView(child);
            state.applyState(child, viewState);
            View divider = this.mDividers.get(i);
            tmpState.initFrom(divider);
            tmpState.yTranslation = viewState.yTranslation - this.mDividerHeight;
            float alpha = (!this.mChildrenExpanded || viewState.alpha == 0.0f) ? 0.0f : 0.5f;
            if (this.mUserLocked && viewState.alpha != 0.0f) {
                alpha = NotificationUtils.interpolate(0.0f, 0.5f, Math.min(viewState.alpha, expandFraction));
            }
            tmpState.hidden = !zIsGroupExpansionChanging;
            tmpState.alpha = alpha;
            state.applyViewState(divider, tmpState);
            child.setFakeShadowIntensity(0.0f, 0.0f, 0, 0);
        }
        if (this.mOverflowNumber != null) {
            state.applyViewState(this.mOverflowNumber, this.mGroupOverFlowState);
            this.mNeverAppliedGroupState = false;
        }
        if (this.mNotificationHeader == null) {
            return;
        }
        state.applyViewState(this.mNotificationHeader, this.mHeaderViewState);
    }

    public void prepareExpansionChanged(StackScrollState state) {
    }

    public void startAnimationToState(StackScrollState state, StackStateAnimator stateAnimator, long baseDelay, long duration) {
        boolean zIsGroupExpansionChanging;
        int childCount = this.mChildren.size();
        ViewState tmpState = new ViewState();
        float expandFraction = getGroupExpandFraction();
        if (this.mUserLocked) {
            zIsGroupExpansionChanging = true;
        } else {
            zIsGroupExpansionChanging = this.mNotificationParent.isGroupExpansionChanging();
        }
        for (int i = childCount - 1; i >= 0; i--) {
            ExpandableNotificationRow child = this.mChildren.get(i);
            StackViewState viewState = state.getViewStateForView(child);
            stateAnimator.startStackAnimations(child, viewState, state, -1, baseDelay);
            View divider = this.mDividers.get(i);
            tmpState.initFrom(divider);
            tmpState.yTranslation = viewState.yTranslation - this.mDividerHeight;
            float alpha = (!this.mChildrenExpanded || viewState.alpha == 0.0f) ? 0.0f : 0.5f;
            if (this.mUserLocked && viewState.alpha != 0.0f) {
                alpha = NotificationUtils.interpolate(0.0f, 0.5f, Math.min(viewState.alpha, expandFraction));
            }
            tmpState.hidden = !zIsGroupExpansionChanging;
            tmpState.alpha = alpha;
            stateAnimator.startViewAnimations(divider, tmpState, baseDelay, duration);
            child.setFakeShadowIntensity(0.0f, 0.0f, 0, 0);
        }
        if (this.mOverflowNumber != null) {
            if (this.mNeverAppliedGroupState) {
                float alpha2 = this.mGroupOverFlowState.alpha;
                this.mGroupOverFlowState.alpha = 0.0f;
                state.applyViewState(this.mOverflowNumber, this.mGroupOverFlowState);
                this.mGroupOverFlowState.alpha = alpha2;
                this.mNeverAppliedGroupState = false;
            }
            stateAnimator.startViewAnimations(this.mOverflowNumber, this.mGroupOverFlowState, baseDelay, duration);
        }
        if (this.mNotificationHeader == null) {
            return;
        }
        state.applyViewState(this.mNotificationHeader, this.mHeaderViewState);
    }

    public ExpandableNotificationRow getViewAtPosition(float y) {
        int count = this.mChildren.size();
        for (int childIdx = 0; childIdx < count; childIdx++) {
            ExpandableNotificationRow slidingChild = this.mChildren.get(childIdx);
            float childTop = slidingChild.getTranslationY();
            float top = childTop + slidingChild.getClipTopAmount();
            float bottom = childTop + slidingChild.getActualHeight();
            if (y >= top && y <= bottom) {
                return slidingChild;
            }
        }
        return null;
    }

    public void setChildrenExpanded(boolean childrenExpanded) {
        this.mChildrenExpanded = childrenExpanded;
        updateExpansionStates();
        if (this.mNotificationHeader != null) {
            this.mNotificationHeader.setExpanded(childrenExpanded);
        }
        int count = this.mChildren.size();
        for (int childIdx = 0; childIdx < count; childIdx++) {
            ExpandableNotificationRow child = this.mChildren.get(childIdx);
            child.setChildrenExpanded(childrenExpanded, false);
        }
    }

    public void setNotificationParent(ExpandableNotificationRow parent) {
        this.mNotificationParent = parent;
        this.mHeaderUtil = new NotificationHeaderUtil(this.mNotificationParent);
    }

    public NotificationHeaderView getHeaderView() {
        return this.mNotificationHeader;
    }

    public void updateHeaderVisibility(int visiblity) {
        if (this.mNotificationHeader == null) {
            return;
        }
        this.mNotificationHeader.setVisibility(visiblity);
    }

    public void updateHeaderForExpansion(boolean expanded) {
        if (this.mNotificationHeader == null) {
            return;
        }
        if (expanded) {
            ColorDrawable cd = new ColorDrawable();
            cd.setColor(this.mNotificationParent.calculateBgColor());
            this.mNotificationHeader.setHeaderBackgroundDrawable(cd);
            return;
        }
        this.mNotificationHeader.setHeaderBackgroundDrawable((Drawable) null);
    }

    public int getMaxContentHeight() {
        int minHeight;
        int maxContentHeight = this.mNotificationHeaderMargin + this.mNotificatonTopPadding;
        int visibleChildren = 0;
        int childCount = this.mChildren.size();
        for (int i = 0; i < childCount && visibleChildren < 8; i++) {
            ExpandableNotificationRow child = this.mChildren.get(i);
            if (child.isExpanded(true)) {
                minHeight = child.getMaxExpandHeight();
            } else {
                minHeight = child.getShowingLayout().getMinHeight(true);
            }
            float childHeight = minHeight;
            maxContentHeight = (int) (maxContentHeight + childHeight);
            visibleChildren++;
        }
        if (visibleChildren > 0) {
            return maxContentHeight + (this.mDividerHeight * visibleChildren);
        }
        return maxContentHeight;
    }

    public void setActualHeight(int actualHeight) {
        int minHeight;
        if (!this.mUserLocked) {
            return;
        }
        this.mActualHeight = actualHeight;
        float fraction = getGroupExpandFraction();
        int maxAllowedVisibleChildren = getMaxAllowedVisibleChildren(true);
        int childCount = this.mChildren.size();
        for (int i = 0; i < childCount; i++) {
            ExpandableNotificationRow child = this.mChildren.get(i);
            if (child.isExpanded(true)) {
                minHeight = child.getMaxExpandHeight();
            } else {
                minHeight = child.getShowingLayout().getMinHeight(true);
            }
            float childHeight = minHeight;
            if (i < maxAllowedVisibleChildren) {
                float singleLineHeight = child.getShowingLayout().getMinHeight(false);
                child.setActualHeight((int) NotificationUtils.interpolate(singleLineHeight, childHeight, fraction), false);
            } else {
                child.setActualHeight((int) childHeight, false);
            }
        }
    }

    public float getGroupExpandFraction() {
        int visibleChildrenExpandedHeight = getVisibleChildrenExpandHeight();
        int minExpandHeight = getCollapsedHeight();
        float factor = (this.mActualHeight - minExpandHeight) / (visibleChildrenExpandedHeight - minExpandHeight);
        return Math.max(0.0f, Math.min(1.0f, factor));
    }

    private int getVisibleChildrenExpandHeight() {
        int minHeight;
        int intrinsicHeight = this.mNotificationHeaderMargin + this.mNotificatonTopPadding + this.mDividerHeight;
        int visibleChildren = 0;
        int childCount = this.mChildren.size();
        int maxAllowedVisibleChildren = getMaxAllowedVisibleChildren(true);
        for (int i = 0; i < childCount && visibleChildren < maxAllowedVisibleChildren; i++) {
            ExpandableNotificationRow child = this.mChildren.get(i);
            if (child.isExpanded(true)) {
                minHeight = child.getMaxExpandHeight();
            } else {
                minHeight = child.getShowingLayout().getMinHeight(true);
            }
            float childHeight = minHeight;
            intrinsicHeight = (int) (intrinsicHeight + childHeight);
            visibleChildren++;
        }
        return intrinsicHeight;
    }

    public int getMinHeight() {
        return getMinHeight(2);
    }

    public int getCollapsedHeight() {
        return getMinHeight(getMaxAllowedVisibleChildren(true));
    }

    private int getMinHeight(int maxAllowedVisibleChildren) {
        int minExpandHeight = this.mNotificationHeaderMargin;
        int visibleChildren = 0;
        boolean firstChild = true;
        int childCount = this.mChildren.size();
        for (int i = 0; i < childCount && visibleChildren < maxAllowedVisibleChildren; i++) {
            if (!firstChild) {
                minExpandHeight += this.mChildPadding;
            } else {
                firstChild = false;
            }
            ExpandableNotificationRow child = this.mChildren.get(i);
            minExpandHeight += child.getSingleLineView().getHeight();
            visibleChildren++;
        }
        return (int) (minExpandHeight + this.mCollapsedBottompadding);
    }

    public void setDark(boolean dark, boolean fade, long delay) {
        if (this.mOverflowNumber != null) {
            this.mOverflowInvertHelper.setInverted(dark, fade, delay);
        }
        this.mNotificationHeaderWrapper.setDark(dark, fade, delay);
    }

    public void reInflateViews(View.OnClickListener listener, StatusBarNotification notification) {
        removeView(this.mNotificationHeader);
        this.mNotificationHeader = null;
        recreateNotificationHeader(listener, notification);
        initDimens();
        for (int i = 0; i < this.mDividers.size(); i++) {
            View prevDivider = this.mDividers.get(i);
            int index = indexOfChild(prevDivider);
            removeView(prevDivider);
            View divider = inflateDivider();
            addView(divider, index);
            this.mDividers.set(i, divider);
        }
        removeView(this.mOverflowNumber);
        this.mOverflowNumber = null;
        this.mOverflowInvertHelper = null;
        this.mGroupOverFlowState = null;
        updateGroupOverflow();
    }

    public void setUserLocked(boolean userLocked) {
        this.mUserLocked = userLocked;
        int childCount = this.mChildren.size();
        for (int i = 0; i < childCount; i++) {
            ExpandableNotificationRow child = this.mChildren.get(i);
            child.setUserLocked(userLocked);
        }
    }

    public void onNotificationUpdated() {
        this.mHybridGroupManager.setOverflowNumberColor(this.mOverflowNumber, this.mNotificationParent.getNotificationColor());
    }

    public int getPositionInLinearLayout(View childInGroup) {
        int position = this.mNotificationHeaderMargin + this.mNotificatonTopPadding;
        for (int i = 0; i < this.mChildren.size(); i++) {
            ExpandableNotificationRow child = this.mChildren.get(i);
            boolean notGone = child.getVisibility() != 8;
            if (notGone) {
                position += this.mDividerHeight;
            }
            if (child == childInGroup) {
                return position;
            }
            if (notGone) {
                position += child.getIntrinsicHeight();
            }
        }
        return 0;
    }
}
