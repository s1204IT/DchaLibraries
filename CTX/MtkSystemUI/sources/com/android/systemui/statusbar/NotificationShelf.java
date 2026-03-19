package com.android.systemui.statusbar;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.SystemProperties;
import android.util.AttributeSet;
import android.util.Log;
import android.util.MathUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.accessibility.AccessibilityNodeInfo;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.statusbar.notification.NotificationUtils;
import com.android.systemui.statusbar.phone.NotificationIconContainer;
import com.android.systemui.statusbar.stack.AmbientState;
import com.android.systemui.statusbar.stack.AnimationProperties;
import com.android.systemui.statusbar.stack.ExpandableViewState;
import com.android.systemui.statusbar.stack.NotificationStackScrollLayout;
import com.android.systemui.statusbar.stack.StackScrollState;
import com.android.systemui.statusbar.stack.ViewState;

public class NotificationShelf extends ActivatableNotificationView implements View.OnLayoutChangeListener {
    private AmbientState mAmbientState;
    private boolean mAnimationsEnabled;
    private Rect mClipRect;
    private NotificationIconContainer mCollapsedIcons;
    private boolean mDark;
    private float mFirstElementRoundness;
    private boolean mHasItemsInStableShelf;
    private boolean mHideBackground;
    private NotificationStackScrollLayout mHostLayout;
    private int mIconAppearTopPadding;
    private int mIconSize;
    private boolean mInteractive;
    private int mMaxLayoutHeight;
    private float mMaxShelfEnd;
    private boolean mNoAnimationsInThisFrame;
    private int mNotGoneIndex;
    private float mOpenedAmount;
    private int mPaddingBetweenElements;
    private int mRelativeOffset;
    private int mScrollFastThreshold;
    private int mShelfAppearTranslation;
    private NotificationIconContainer mShelfIcons;
    private ShelfState mShelfState;
    private boolean mShowNotificationShelf;
    private int mStatusBarHeight;
    private int mStatusBarPaddingStart;
    private int mStatusBarState;
    private int[] mTmp;
    private static final boolean USE_ANIMATIONS_WHEN_OPENING = SystemProperties.getBoolean("debug.icon_opening_animations", true);
    private static final boolean ICON_ANMATIONS_WHILE_SCROLLING = SystemProperties.getBoolean("debug.icon_scroll_animations", true);

    public NotificationShelf(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mTmp = new int[2];
        this.mAnimationsEnabled = true;
        this.mClipRect = new Rect();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mShelfIcons = (NotificationIconContainer) findViewById(R.id.content);
        this.mShelfIcons.setClipChildren(false);
        this.mShelfIcons.setClipToPadding(false);
        setClipToActualHeight(false);
        setClipChildren(false);
        setClipToPadding(false);
        this.mShelfIcons.setIsStaticLayout(false);
        this.mShelfState = new ShelfState();
        setBottomRoundness(1.0f, false);
        initDimens();
    }

    public void bind(AmbientState ambientState, NotificationStackScrollLayout notificationStackScrollLayout) {
        this.mAmbientState = ambientState;
        this.mHostLayout = notificationStackScrollLayout;
    }

    private void initDimens() {
        Resources resources = getResources();
        this.mIconAppearTopPadding = resources.getDimensionPixelSize(R.dimen.notification_icon_appear_padding);
        this.mStatusBarHeight = resources.getDimensionPixelOffset(R.dimen.status_bar_height);
        this.mStatusBarPaddingStart = resources.getDimensionPixelOffset(R.dimen.status_bar_padding_start);
        this.mPaddingBetweenElements = resources.getDimensionPixelSize(R.dimen.notification_divider_height);
        this.mShelfAppearTranslation = resources.getDimensionPixelSize(R.dimen.shelf_appear_translation);
        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        layoutParams.height = resources.getDimensionPixelOffset(R.dimen.notification_shelf_height);
        setLayoutParams(layoutParams);
        int dimensionPixelOffset = resources.getDimensionPixelOffset(R.dimen.shelf_icon_container_padding);
        this.mShelfIcons.setPadding(dimensionPixelOffset, 0, dimensionPixelOffset, 0);
        this.mScrollFastThreshold = resources.getDimensionPixelOffset(R.dimen.scroll_fast_threshold);
        this.mShowNotificationShelf = resources.getBoolean(R.bool.config_showNotificationShelf);
        this.mIconSize = resources.getDimensionPixelSize(android.R.dimen.handwriting_bounds_offset_left);
        if (!this.mShowNotificationShelf) {
            setVisibility(8);
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        initDimens();
    }

    @Override
    public void setDark(boolean z, boolean z2, long j) {
        super.setDark(z, z2, j);
        if (this.mDark == z) {
            return;
        }
        this.mDark = z;
        this.mShelfIcons.setDark(z, z2, j);
        updateInteractiveness();
    }

    public void fadeInTranslating() {
        float translationY = this.mShelfIcons.getTranslationY();
        this.mShelfIcons.setTranslationY(translationY - this.mShelfAppearTranslation);
        this.mShelfIcons.setAlpha(0.0f);
        this.mShelfIcons.animate().setInterpolator(Interpolators.DECELERATE_QUINT).translationY(translationY).setDuration(200L).start();
        this.mShelfIcons.animate().alpha(1.0f).setInterpolator(Interpolators.LINEAR).setDuration(200L).start();
    }

    @Override
    protected View getContentView() {
        return this.mShelfIcons;
    }

    public NotificationIconContainer getShelfIcons() {
        return this.mShelfIcons;
    }

    @Override
    public ExpandableViewState createNewViewState(StackScrollState stackScrollState) {
        return this.mShelfState;
    }

    public void updateState(StackScrollState stackScrollState, AmbientState ambientState) {
        float darkAmount;
        ActivatableNotificationView lastVisibleBackgroundChild = ambientState.getLastVisibleBackgroundChild();
        boolean z = true;
        if (this.mShowNotificationShelf && lastVisibleBackgroundChild != null) {
            float innerHeight = ambientState.getInnerHeight() + ambientState.getTopPadding() + ambientState.getStackTranslation();
            ExpandableViewState viewStateForView = stackScrollState.getViewStateForView(lastVisibleBackgroundChild);
            float f = viewStateForView.yTranslation + viewStateForView.height;
            this.mShelfState.copyFrom(viewStateForView);
            this.mShelfState.height = getIntrinsicHeight();
            float fMax = Math.max(Math.min(f, innerHeight) - this.mShelfState.height, getFullyClosedTranslation());
            float darkTopPadding = this.mAmbientState.getDarkTopPadding();
            if (!this.mAmbientState.hasPulsingNotifications()) {
                darkAmount = this.mAmbientState.getDarkAmount();
            } else {
                darkAmount = 0.0f;
            }
            this.mShelfState.yTranslation = MathUtils.lerp(fMax, darkTopPadding, darkAmount);
            this.mShelfState.zTranslation = ambientState.getBaseZHeight();
            this.mShelfState.openedAmount = Math.min(1.0f, (this.mShelfState.yTranslation - getFullyClosedTranslation()) / (getIntrinsicHeight() * 2));
            this.mShelfState.clipTopAmount = 0;
            this.mShelfState.alpha = this.mAmbientState.hasPulsingNotifications() ? 0.0f : 1.0f;
            this.mShelfState.belowSpeedBump = this.mAmbientState.getSpeedBumpIndex() == 0;
            this.mShelfState.shadowAlpha = 1.0f;
            this.mShelfState.hideSensitive = false;
            this.mShelfState.xTranslation = getTranslationX();
            if (this.mNotGoneIndex != -1) {
                this.mShelfState.notGoneIndex = Math.min(this.mShelfState.notGoneIndex, this.mNotGoneIndex);
            }
            this.mShelfState.hasItemsInStableShelf = viewStateForView.inShelf;
            ShelfState shelfState = this.mShelfState;
            if (this.mAmbientState.isShadeExpanded() && !this.mAmbientState.isQsCustomizerShowing()) {
                z = false;
            }
            shelfState.hidden = z;
            this.mShelfState.maxShelfEnd = innerHeight;
            return;
        }
        this.mShelfState.hidden = true;
        this.mShelfState.location = 64;
        this.mShelfState.hasItemsInStableShelf = false;
    }

    public void updateAppearance() {
        int i;
        int i2;
        float currentTopRoundness;
        int i3;
        int i4;
        ActivatableNotificationView activatableNotificationView;
        float f;
        boolean z;
        int i5;
        int translationY;
        float fMin;
        int i6;
        boolean z2;
        int i7;
        if (!this.mShowNotificationShelf) {
            return;
        }
        this.mShelfIcons.resetViewStates();
        float translationY2 = getTranslationY();
        ActivatableNotificationView lastVisibleBackgroundChild = this.mAmbientState.getLastVisibleBackgroundChild();
        this.mNotGoneIndex = -1;
        float intrinsicHeight = this.mMaxLayoutHeight - (getIntrinsicHeight() * 2);
        float fMin2 = translationY2 >= intrinsicHeight ? Math.min(1.0f, (translationY2 - intrinsicHeight) / getIntrinsicHeight()) : 0.0f;
        boolean z3 = this.mHideBackground && !this.mShelfState.hasItemsInStableShelf;
        float currentScrollVelocity = this.mAmbientState.getCurrentScrollVelocity();
        boolean z4 = currentScrollVelocity > ((float) this.mScrollFastThreshold) || (this.mAmbientState.isExpansionChanging() && Math.abs(this.mAmbientState.getExpandingVelocity()) > ((float) this.mScrollFastThreshold));
        boolean z5 = currentScrollVelocity > 0.0f;
        boolean z6 = this.mAmbientState.isExpansionChanging() && !this.mAmbientState.isPanelTracking();
        int baseZHeight = this.mAmbientState.getBaseZHeight();
        float f2 = 0.0f;
        float f3 = 0.0f;
        float f4 = 0.0f;
        int i8 = 0;
        int i9 = 0;
        int i10 = 0;
        int i11 = 0;
        int i12 = 0;
        int i13 = 0;
        while (i12 < this.mHostLayout.getChildCount()) {
            ExpandableView expandableView = (ExpandableView) this.mHostLayout.getChildAt(i12);
            if (!(expandableView instanceof ExpandableNotificationRow) || expandableView.getVisibility() == 8) {
                i = i8;
                i2 = i10;
                currentTopRoundness = f2;
                i3 = i12;
                i4 = baseZHeight;
                activatableNotificationView = lastVisibleBackgroundChild;
                f = fMin2;
                z = false;
                i5 = i9;
                translationY = i11;
            } else {
                ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) expandableView;
                boolean z7 = ViewState.getFinalTranslationZ(expandableNotificationRow) > ((float) baseZHeight) || expandableNotificationRow.isPinned();
                boolean z8 = expandableView == lastVisibleBackgroundChild;
                float translationY3 = expandableNotificationRow.getTranslationY();
                if ((z8 && !expandableView.isInShelf()) || z7 || z3) {
                    i6 = i8;
                    fMin = translationY2 + getIntrinsicHeight();
                } else {
                    fMin = translationY2 - this.mPaddingBetweenElements;
                    float f5 = fMin - translationY3;
                    if (expandableNotificationRow.isBelowSpeedBump()) {
                        i6 = i8;
                    } else {
                        i6 = i8;
                        if (f5 <= getNotificationMergeSize()) {
                            fMin = Math.min(translationY2, translationY3 + getNotificationMergeSize());
                        }
                    }
                }
                updateNotificationClipHeight(expandableNotificationRow, fMin);
                int i14 = i6;
                activatableNotificationView = lastVisibleBackgroundChild;
                i5 = i9;
                boolean z9 = z7;
                int i15 = i10;
                float f6 = fMin2;
                f = fMin2;
                currentTopRoundness = f2;
                translationY = i11;
                i3 = i12;
                i4 = baseZHeight;
                float fUpdateIconAppearance = updateIconAppearance(expandableNotificationRow, f6, z5, z4, z6, z8);
                f4 += fUpdateIconAppearance;
                int backgroundColorWithoutTint = expandableNotificationRow.getBackgroundColorWithoutTint();
                if (translationY3 >= translationY2 && this.mNotGoneIndex == -1) {
                    this.mNotGoneIndex = i15;
                    setTintColor(i14);
                    setOverrideTintColor(i5, f3);
                } else if (this.mNotGoneIndex == -1) {
                    f3 = fUpdateIconAppearance;
                    i5 = i14;
                }
                if (z8) {
                    int i16 = i13 == 0 ? backgroundColorWithoutTint : i13;
                    expandableNotificationRow.setOverrideTintColor(i16, fUpdateIconAppearance);
                    i7 = i16;
                    z2 = false;
                    z = false;
                } else {
                    z2 = false;
                    z = false;
                    expandableNotificationRow.setOverrideTintColor(0, 0.0f);
                    i7 = backgroundColorWithoutTint;
                }
                if (i15 != 0 || !z9) {
                    expandableNotificationRow.setAboveShelf(z2);
                }
                if (i15 == 0) {
                    NotificationIconContainer.IconState iconState = getIconState(expandableNotificationRow.getEntry().expandedIcon);
                    if (iconState != null && iconState.clampedAppearAmount == 1.0f) {
                        translationY = (int) (expandableNotificationRow.getTranslationY() - getTranslationY());
                        currentTopRoundness = expandableNotificationRow.getCurrentTopRoundness();
                    } else if (iconState == null) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("iconState is null. ExpandedIcon: ");
                        sb.append(expandableNotificationRow.getEntry().expandedIcon);
                        sb.append(expandableNotificationRow.getEntry().expandedIcon != null ? "\n icon parent: " + expandableNotificationRow.getEntry().expandedIcon.getParent() : "");
                        sb.append(" \n number of notifications: ");
                        sb.append(this.mHostLayout.getChildCount());
                        Log.wtf("NotificationShelf", sb.toString());
                    }
                }
                i = backgroundColorWithoutTint;
                i13 = i7;
                i2 = i15 + 1;
            }
            i9 = i5;
            i11 = translationY;
            f2 = currentTopRoundness;
            i12 = i3 + 1;
            i10 = i2;
            i8 = i;
            baseZHeight = i4;
            lastVisibleBackgroundChild = activatableNotificationView;
            fMin2 = f;
        }
        int i17 = i10;
        clipTransientViews();
        setBackgroundTop(i11);
        setFirstElementRoundness(f2);
        this.mShelfIcons.setSpeedBumpIndex(this.mAmbientState.getSpeedBumpIndex());
        this.mShelfIcons.calculateIconTranslations();
        this.mShelfIcons.applyIconStates();
        for (int i18 = 0; i18 < this.mHostLayout.getChildCount(); i18++) {
            View childAt = this.mHostLayout.getChildAt(i18);
            if ((childAt instanceof ExpandableNotificationRow) && childAt.getVisibility() != 8) {
                ExpandableNotificationRow expandableNotificationRow2 = (ExpandableNotificationRow) childAt;
                updateIconClipAmount(expandableNotificationRow2);
                updateContinuousClipping(expandableNotificationRow2);
            }
        }
        setHideBackground(((f4 > 1.0f ? 1 : (f4 == 1.0f ? 0 : -1)) < 0) || z3);
        if (this.mNotGoneIndex == -1) {
            this.mNotGoneIndex = i17;
        }
    }

    private void clipTransientViews() {
        for (int i = 0; i < this.mHostLayout.getTransientViewCount(); i++) {
            View transientView = this.mHostLayout.getTransientView(i);
            if (transientView instanceof ExpandableNotificationRow) {
                updateNotificationClipHeight((ExpandableNotificationRow) transientView, getTranslationY());
            } else {
                Log.e("NotificationShelf", "NotificationShelf.clipTransientViews(): Trying to clip non-row transient view");
            }
        }
    }

    private void setFirstElementRoundness(float f) {
        if (this.mFirstElementRoundness != f) {
            this.mFirstElementRoundness = f;
            setTopRoundness(f, false);
        }
    }

    private void updateIconClipAmount(ExpandableNotificationRow expandableNotificationRow) {
        float translationY = expandableNotificationRow.getTranslationY();
        StatusBarIconView statusBarIconView = expandableNotificationRow.getEntry().expandedIcon;
        float translationY2 = getTranslationY() + statusBarIconView.getTop() + statusBarIconView.getTranslationY();
        if (translationY2 < translationY && !this.mAmbientState.isDark()) {
            int i = (int) (translationY - translationY2);
            statusBarIconView.setClipBounds(new Rect(0, i, statusBarIconView.getWidth(), Math.max(i, statusBarIconView.getHeight())));
        } else {
            statusBarIconView.setClipBounds(null);
        }
    }

    private void updateContinuousClipping(final ExpandableNotificationRow expandableNotificationRow) {
        final StatusBarIconView statusBarIconView = expandableNotificationRow.getEntry().expandedIcon;
        boolean z = ViewState.isAnimatingY(statusBarIconView) && !this.mAmbientState.isDark();
        boolean z2 = statusBarIconView.getTag(R.id.continuous_clipping_tag) != null;
        if (z && !z2) {
            final ViewTreeObserver viewTreeObserver = statusBarIconView.getViewTreeObserver();
            final ViewTreeObserver.OnPreDrawListener onPreDrawListener = new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    if (ViewState.isAnimatingY(statusBarIconView)) {
                        NotificationShelf.this.updateIconClipAmount(expandableNotificationRow);
                        return true;
                    }
                    viewTreeObserver.removeOnPreDrawListener(this);
                    statusBarIconView.setTag(R.id.continuous_clipping_tag, null);
                    return true;
                }
            };
            viewTreeObserver.addOnPreDrawListener(onPreDrawListener);
            statusBarIconView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View view) {
                }

                @Override
                public void onViewDetachedFromWindow(View view) {
                    if (view == statusBarIconView) {
                        viewTreeObserver.removeOnPreDrawListener(onPreDrawListener);
                        statusBarIconView.setTag(R.id.continuous_clipping_tag, null);
                    }
                }
            });
            statusBarIconView.setTag(R.id.continuous_clipping_tag, onPreDrawListener);
        }
    }

    private void updateNotificationClipHeight(ExpandableNotificationRow expandableNotificationRow, float f) {
        float translationY = expandableNotificationRow.getTranslationY() + expandableNotificationRow.getActualHeight();
        boolean z = (expandableNotificationRow.isPinned() || expandableNotificationRow.isHeadsUpAnimatingAway()) && !this.mAmbientState.isDozingAndNotPulsing(expandableNotificationRow);
        if (translationY > f && (this.mAmbientState.isShadeExpanded() || !z)) {
            int iMin = (int) (translationY - f);
            if (z) {
                iMin = Math.min(expandableNotificationRow.getIntrinsicHeight() - expandableNotificationRow.getCollapsedHeight(), iMin);
            }
            expandableNotificationRow.setClipBottomAmount(iMin);
            return;
        }
        expandableNotificationRow.setClipBottomAmount(0);
    }

    @Override
    public void setFakeShadowIntensity(float f, float f2, int i, int i2) {
        if (!this.mHasItemsInStableShelf) {
            f = 0.0f;
        }
        super.setFakeShadowIntensity(f, f2, i, i2);
    }

    private float updateIconAppearance(ExpandableNotificationRow expandableNotificationRow, float f, boolean z, boolean z2, boolean z3, boolean z4) {
        ExpandableNotificationRow expandableNotificationRow2;
        float fMin;
        NotificationIconContainer.IconState iconState = getIconState(expandableNotificationRow.getEntry().expandedIcon);
        if (iconState == null) {
            return 0.0f;
        }
        float translationY = expandableNotificationRow.getTranslationY();
        int actualHeight = expandableNotificationRow.getActualHeight() + this.mPaddingBetweenElements;
        float f2 = 1.0f;
        float fMin2 = Math.min(getIntrinsicHeight() * 1.5f * NotificationUtils.interpolate(1.0f, 1.5f, f), actualHeight);
        if (z4) {
            actualHeight = Math.min(actualHeight, expandableNotificationRow.getMinHeight() - getIntrinsicHeight());
            fMin2 = Math.min(fMin2, expandableNotificationRow.getMinHeight() - getIntrinsicHeight());
        }
        float f3 = actualHeight + translationY;
        boolean z5 = true;
        if (z3 && this.mAmbientState.getScrollY() == 0 && !this.mAmbientState.isOnKeyguard() && !iconState.isLastExpandIcon) {
            expandableNotificationRow2 = expandableNotificationRow;
            float intrinsicPadding = this.mAmbientState.getIntrinsicPadding() + this.mHostLayout.getPositionInLinearLayout(expandableNotificationRow2);
            float intrinsicHeight = this.mMaxLayoutHeight - getIntrinsicHeight();
            if (intrinsicPadding < intrinsicHeight && expandableNotificationRow.getIntrinsicHeight() + intrinsicPadding >= intrinsicHeight && expandableNotificationRow.getTranslationY() < intrinsicPadding) {
                iconState.isLastExpandIcon = true;
                iconState.customTransformHeight = Integer.MIN_VALUE;
                if (!(((float) (this.mMaxLayoutHeight - getIntrinsicHeight())) - intrinsicPadding < ((float) getIntrinsicHeight()))) {
                    iconState.customTransformHeight = (int) ((this.mMaxLayoutHeight - getIntrinsicHeight()) - intrinsicPadding);
                }
            }
        } else {
            expandableNotificationRow2 = expandableNotificationRow;
        }
        float translationY2 = getTranslationY();
        if (iconState.hasCustomTransformHeight()) {
            actualHeight = iconState.customTransformHeight;
            fMin2 = iconState.customTransformHeight;
        }
        if (f3 < translationY2 || ((this.mAmbientState.isUnlockHintRunning() && !expandableNotificationRow.isInShelf()) || (!this.mAmbientState.isShadeExpanded() && (expandableNotificationRow.isPinned() || expandableNotificationRow.isHeadsUpAnimatingAway())))) {
            f2 = 0.0f;
            fMin = 0.0f;
        } else if (translationY < translationY2) {
            float f4 = translationY2 - translationY;
            float fMin3 = Math.min(1.0f, f4 / actualHeight);
            float fInterpolate = 1.0f - NotificationUtils.interpolate(Interpolators.ACCELERATE_DECELERATE.getInterpolation(fMin3), fMin3, f);
            fMin = 1.0f - Math.min(1.0f, f4 / fMin2);
            z5 = false;
            f2 = fInterpolate;
        } else {
            fMin = 1.0f;
        }
        if (z5 && !z3 && iconState.isLastExpandIcon) {
            iconState.isLastExpandIcon = false;
            iconState.customTransformHeight = Integer.MIN_VALUE;
        }
        updateIconPositioning(expandableNotificationRow2, fMin, f2, fMin2, z, z2, z3, z4);
        return f2;
    }

    private void updateIconPositioning(ExpandableNotificationRow expandableNotificationRow, float f, float f2, float f3, boolean z, boolean z2, boolean z3, boolean z4) {
        float f4;
        StatusBarIconView statusBarIconView = expandableNotificationRow.getEntry().expandedIcon;
        NotificationIconContainer.IconState iconState = getIconState(statusBarIconView);
        if (iconState == null) {
            return;
        }
        boolean z5 = iconState.isLastExpandIcon && !iconState.hasCustomTransformHeight();
        float f5 = f > 0.5f ? 1.0f : 0.0f;
        if (f5 == f2) {
            iconState.noAnimations = (z2 || z3) && !z5;
            iconState.useFullTransitionAmount = iconState.noAnimations || (!ICON_ANMATIONS_WHILE_SCROLLING && f2 == 0.0f && z);
            iconState.useLinearTransitionAmount = (ICON_ANMATIONS_WHILE_SCROLLING || f2 != 0.0f || this.mAmbientState.isExpansionChanging()) ? false : true;
            iconState.translateContent = (((float) this.mMaxLayoutHeight) - getTranslationY()) - ((float) getIntrinsicHeight()) > 0.0f;
        }
        if (!z5 && (z2 || (z3 && iconState.useFullTransitionAmount && !ViewState.isAnimatingY(statusBarIconView)))) {
            iconState.cancelAnimations(statusBarIconView);
            iconState.useFullTransitionAmount = true;
            iconState.noAnimations = true;
        }
        if (iconState.hasCustomTransformHeight()) {
            iconState.useFullTransitionAmount = true;
        }
        if (iconState.isLastExpandIcon) {
            iconState.translateContent = false;
        }
        if (this.mAmbientState.getDarkAmount() > 0.0f && !expandableNotificationRow.isInShelf()) {
            f4 = this.mAmbientState.isFullyDark() ? 1.0f : 0.0f;
        } else if (z4 || !USE_ANIMATIONS_WHEN_OPENING || iconState.useFullTransitionAmount || iconState.useLinearTransitionAmount) {
            f4 = f;
        } else {
            iconState.needsCannedAnimation = (iconState.clampedAppearAmount == f5 || this.mNoAnimationsInThisFrame) ? false : true;
            f4 = f5;
        }
        iconState.iconAppearAmount = (!USE_ANIMATIONS_WHEN_OPENING || iconState.useFullTransitionAmount) ? f2 : f4;
        iconState.clampedAppearAmount = f5;
        expandableNotificationRow.setContentTransformationAmount((this.mAmbientState.isAboveShelf(expandableNotificationRow) || !(z4 || iconState.translateContent)) ? 0.0f : f, z4);
        setIconTransformationAmount(expandableNotificationRow, f4, f3, f5 != f4, z4);
    }

    private void setIconTransformationAmount(ExpandableNotificationRow expandableNotificationRow, float f, float f2, boolean z, boolean z2) {
        int relativeTopPadding;
        float height;
        float f3;
        StatusBarIconView statusBarIconView = expandableNotificationRow.getEntry().expandedIcon;
        NotificationIconContainer.IconState iconState = getIconState(statusBarIconView);
        View notificationIcon = expandableNotificationRow.getNotificationIcon();
        float translationY = expandableNotificationRow.getTranslationY() + expandableNotificationRow.getContentTranslation();
        boolean z3 = expandableNotificationRow.isInShelf() && !expandableNotificationRow.isTransformingIntoShelf();
        if (z && !z3) {
            translationY = getTranslationY() - f2;
        }
        if (notificationIcon != null) {
            relativeTopPadding = expandableNotificationRow.getRelativeTopPadding(notificationIcon);
            height = notificationIcon.getHeight();
        } else {
            relativeTopPadding = this.mIconAppearTopPadding;
            height = 0.0f;
        }
        float fInterpolate = NotificationUtils.interpolate((translationY + relativeTopPadding) - ((getTranslationY() + statusBarIconView.getTop()) + ((statusBarIconView.getHeight() - (statusBarIconView.getIconScale() * this.mIconSize)) / 2.0f)), 0.0f, f);
        float iconScale = this.mIconSize * statusBarIconView.getIconScale();
        boolean z4 = !expandableNotificationRow.isShowingIcon();
        if (z4) {
            height = iconScale / 2.0f;
            f3 = f;
        } else {
            f3 = 1.0f;
        }
        float fInterpolate2 = NotificationUtils.interpolate(height, iconScale, f);
        if (iconState != null) {
            iconState.scaleX = fInterpolate2 / iconScale;
            iconState.scaleY = iconState.scaleX;
            iconState.hidden = f == 0.0f && !iconState.isAnimating(statusBarIconView);
            if (expandableNotificationRow.isDrawingAppearAnimation() && !expandableNotificationRow.isInShelf()) {
                iconState.hidden = true;
                iconState.iconAppearAmount = 0.0f;
            }
            iconState.alpha = f3;
            iconState.yTranslation = fInterpolate;
            if (z3) {
                iconState.iconAppearAmount = 1.0f;
                iconState.alpha = 1.0f;
                iconState.scaleX = 1.0f;
                iconState.scaleY = 1.0f;
                iconState.hidden = false;
            }
            if (this.mAmbientState.isAboveShelf(expandableNotificationRow) || (!expandableNotificationRow.isInShelf() && ((z2 && expandableNotificationRow.areGutsExposed()) || expandableNotificationRow.getTranslationZ() > this.mAmbientState.getBaseZHeight()))) {
                iconState.hidden = true;
            }
            int contrastedStaticDrawableColor = statusBarIconView.getContrastedStaticDrawableColor(getBackgroundColorWithoutTint());
            if (!z4 && contrastedStaticDrawableColor != 0) {
                contrastedStaticDrawableColor = NotificationUtils.interpolateColors(expandableNotificationRow.getVisibleNotificationHeader().getOriginalIconColor(), contrastedStaticDrawableColor, iconState.iconAppearAmount);
            }
            iconState.iconColor = contrastedStaticDrawableColor;
        }
    }

    private NotificationIconContainer.IconState getIconState(StatusBarIconView statusBarIconView) {
        return this.mShelfIcons.getIconState(statusBarIconView);
    }

    private float getFullyClosedTranslation() {
        return (-(getIntrinsicHeight() - this.mStatusBarHeight)) / 2;
    }

    public int getNotificationMergeSize() {
        return getIntrinsicHeight();
    }

    @Override
    public boolean hasNoContentHeight() {
        return true;
    }

    private void setHideBackground(boolean z) {
        if (this.mHideBackground != z) {
            this.mHideBackground = z;
            updateBackground();
            updateOutline();
        }
    }

    @Override
    protected boolean needsOutline() {
        return !this.mHideBackground && super.needsOutline();
    }

    @Override
    protected boolean shouldHideBackground() {
        return super.shouldHideBackground() || this.mHideBackground;
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        updateRelativeOffset();
        int i5 = getResources().getDisplayMetrics().heightPixels;
        this.mClipRect.set(0, -i5, getWidth(), i5);
        this.mShelfIcons.setClipBounds(this.mClipRect);
    }

    private void updateRelativeOffset() {
        this.mCollapsedIcons.getLocationOnScreen(this.mTmp);
        this.mRelativeOffset = this.mTmp[0];
        getLocationOnScreen(this.mTmp);
        this.mRelativeOffset -= this.mTmp[0];
    }

    private void setOpenedAmount(float f) {
        int partialOverflowExtraPadding;
        this.mNoAnimationsInThisFrame = f == 1.0f && this.mOpenedAmount == 0.0f;
        this.mOpenedAmount = f;
        if (!this.mAmbientState.isPanelFullWidth()) {
            f = 1.0f;
        }
        int width = this.mRelativeOffset;
        if (isLayoutRtl()) {
            width = (getWidth() - width) - this.mCollapsedIcons.getWidth();
        }
        this.mShelfIcons.setActualLayoutWidth((int) NotificationUtils.interpolate(this.mCollapsedIcons.getFinalTranslationX() + width, this.mShelfIcons.getWidth(), f));
        boolean zHasOverflow = this.mCollapsedIcons.hasOverflow();
        int paddingEnd = this.mCollapsedIcons.getPaddingEnd();
        if (!zHasOverflow) {
            partialOverflowExtraPadding = paddingEnd - this.mCollapsedIcons.getNoOverflowExtraPadding();
        } else {
            partialOverflowExtraPadding = paddingEnd - this.mCollapsedIcons.getPartialOverflowExtraPadding();
        }
        this.mShelfIcons.setActualPaddingEnd(NotificationUtils.interpolate(partialOverflowExtraPadding, this.mShelfIcons.getPaddingEnd(), f));
        this.mShelfIcons.setActualPaddingStart(NotificationUtils.interpolate(width, this.mShelfIcons.getPaddingStart(), f));
        this.mShelfIcons.setOpenedAmount(f);
    }

    public void setMaxLayoutHeight(int i) {
        this.mMaxLayoutHeight = i;
    }

    public int getNotGoneIndex() {
        return this.mNotGoneIndex;
    }

    private void setHasItemsInStableShelf(boolean z) {
        if (this.mHasItemsInStableShelf != z) {
            this.mHasItemsInStableShelf = z;
            updateInteractiveness();
        }
    }

    public boolean hasItemsInStableShelf() {
        return this.mHasItemsInStableShelf;
    }

    public void setCollapsedIcons(NotificationIconContainer notificationIconContainer) {
        this.mCollapsedIcons = notificationIconContainer;
        this.mCollapsedIcons.addOnLayoutChangeListener(this);
    }

    public void setStatusBarState(int i) {
        if (this.mStatusBarState != i) {
            this.mStatusBarState = i;
            updateInteractiveness();
        }
    }

    private void updateInteractiveness() {
        this.mInteractive = this.mStatusBarState == 1 && this.mHasItemsInStableShelf && !this.mDark;
        setClickable(this.mInteractive);
        setFocusable(this.mInteractive);
        setImportantForAccessibility(this.mInteractive ? 1 : 4);
    }

    @Override
    protected boolean isInteractive() {
        return this.mInteractive;
    }

    public void setMaxShelfEnd(float f) {
        this.mMaxShelfEnd = f;
    }

    public void setAnimationsEnabled(boolean z) {
        this.mAnimationsEnabled = z;
        this.mCollapsedIcons.setAnimationsEnabled(z);
        if (!z) {
            this.mShelfIcons.setAnimationsEnabled(false);
        }
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(accessibilityNodeInfo);
        if (this.mInteractive) {
            accessibilityNodeInfo.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_EXPAND);
            accessibilityNodeInfo.addAction(new AccessibilityNodeInfo.AccessibilityAction(16, getContext().getString(R.string.accessibility_overflow_action)));
        }
    }

    @Override
    public void onLayoutChange(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
        updateRelativeOffset();
    }

    private class ShelfState extends ExpandableViewState {
        private boolean hasItemsInStableShelf;
        private float maxShelfEnd;
        private float openedAmount;

        private ShelfState() {
        }

        @Override
        public void applyToView(View view) {
            if (!NotificationShelf.this.mShowNotificationShelf) {
                return;
            }
            super.applyToView(view);
            NotificationShelf.this.setMaxShelfEnd(this.maxShelfEnd);
            NotificationShelf.this.setOpenedAmount(this.openedAmount);
            NotificationShelf.this.updateAppearance();
            NotificationShelf.this.setHasItemsInStableShelf(this.hasItemsInStableShelf);
            NotificationShelf.this.mShelfIcons.setAnimationsEnabled(NotificationShelf.this.mAnimationsEnabled);
        }

        @Override
        public void animateTo(View view, AnimationProperties animationProperties) {
            if (!NotificationShelf.this.mShowNotificationShelf) {
                return;
            }
            super.animateTo(view, animationProperties);
            NotificationShelf.this.setMaxShelfEnd(this.maxShelfEnd);
            NotificationShelf.this.setOpenedAmount(this.openedAmount);
            NotificationShelf.this.updateAppearance();
            NotificationShelf.this.setHasItemsInStableShelf(this.hasItemsInStableShelf);
            NotificationShelf.this.mShelfIcons.setAnimationsEnabled(NotificationShelf.this.mAnimationsEnabled);
        }
    }
}
