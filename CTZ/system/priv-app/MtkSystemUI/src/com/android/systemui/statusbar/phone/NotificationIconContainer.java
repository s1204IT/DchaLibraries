package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Icon;
import android.support.v4.util.ArrayMap;
import android.util.AttributeSet;
import android.view.View;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.statusbar.AlphaOptimizedFrameLayout;
import com.android.systemui.statusbar.StatusBarIconView;
import com.android.systemui.statusbar.stack.AnimationFilter;
import com.android.systemui.statusbar.stack.AnimationProperties;
import com.android.systemui.statusbar.stack.ViewState;
import java.util.ArrayList;
import java.util.HashMap;

/* loaded from: classes.dex */
public class NotificationIconContainer extends AlphaOptimizedFrameLayout {
    private int[] mAbsolutePosition;
    private int mActualLayoutWidth;
    private float mActualPaddingEnd;
    private float mActualPaddingStart;
    private int mAddAnimationStartIndex;
    private boolean mAnimationsEnabled;
    private int mCannedAnimationStartIndex;
    private boolean mChangingViewPositions;
    private boolean mDark;
    private boolean mDisallowNextAnimation;
    private int mDotPadding;
    private IconState mFirstVisibleIconState;
    private int mIconSize;
    private final HashMap<View, IconState> mIconStates;
    private boolean mIsStaticLayout;
    private StatusBarIconView mIsolatedIcon;
    private View mIsolatedIconForAnimation;
    private Rect mIsolatedIconLocation;
    private IconState mLastVisibleIconState;
    private int mNumDots;
    private float mOpenedAmount;
    private int mOverflowWidth;
    private ArrayMap<String, ArrayList<StatusBarIcon>> mReplacingIcons;
    private int mSpeedBumpIndex;
    private int mStaticDotDiameter;
    private int mStaticDotRadius;
    private float mVisualOverflowStart;
    private static final AnimationProperties DOT_ANIMATION_PROPERTIES = new AnimationProperties() { // from class: com.android.systemui.statusbar.phone.NotificationIconContainer.1
        private AnimationFilter mAnimationFilter = new AnimationFilter().animateX();

        @Override // com.android.systemui.statusbar.stack.AnimationProperties
        public AnimationFilter getAnimationFilter() {
            return this.mAnimationFilter;
        }
    }.setDuration(200);
    private static final AnimationProperties ICON_ANIMATION_PROPERTIES = new AnimationProperties() { // from class: com.android.systemui.statusbar.phone.NotificationIconContainer.2
        private AnimationFilter mAnimationFilter = new AnimationFilter().animateY().animateAlpha().animateScale();

        @Override // com.android.systemui.statusbar.stack.AnimationProperties
        public AnimationFilter getAnimationFilter() {
            return this.mAnimationFilter;
        }
    }.setDuration(100).setCustomInterpolator(View.TRANSLATION_Y, Interpolators.ICON_OVERSHOT);
    private static final AnimationProperties sTempProperties = new AnimationProperties() { // from class: com.android.systemui.statusbar.phone.NotificationIconContainer.3
        private AnimationFilter mAnimationFilter = new AnimationFilter();

        @Override // com.android.systemui.statusbar.stack.AnimationProperties
        public AnimationFilter getAnimationFilter() {
            return this.mAnimationFilter;
        }
    };
    private static final AnimationProperties ADD_ICON_PROPERTIES = new AnimationProperties() { // from class: com.android.systemui.statusbar.phone.NotificationIconContainer.4
        private AnimationFilter mAnimationFilter = new AnimationFilter().animateAlpha();

        @Override // com.android.systemui.statusbar.stack.AnimationProperties
        public AnimationFilter getAnimationFilter() {
            return this.mAnimationFilter;
        }
    }.setDuration(200).setDelay(50);
    private static final AnimationProperties UNISOLATION_PROPERTY_OTHERS = new AnimationProperties() { // from class: com.android.systemui.statusbar.phone.NotificationIconContainer.5
        private AnimationFilter mAnimationFilter = new AnimationFilter().animateAlpha();

        @Override // com.android.systemui.statusbar.stack.AnimationProperties
        public AnimationFilter getAnimationFilter() {
            return this.mAnimationFilter;
        }
    }.setDuration(110);
    private static final AnimationProperties UNISOLATION_PROPERTY = new AnimationProperties() { // from class: com.android.systemui.statusbar.phone.NotificationIconContainer.6
        private AnimationFilter mAnimationFilter = new AnimationFilter().animateX();

        @Override // com.android.systemui.statusbar.stack.AnimationProperties
        public AnimationFilter getAnimationFilter() {
            return this.mAnimationFilter;
        }
    }.setDuration(110);

    public NotificationIconContainer(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mIsStaticLayout = true;
        this.mIconStates = new HashMap<>();
        this.mActualLayoutWidth = Integer.MIN_VALUE;
        this.mActualPaddingEnd = -2.1474836E9f;
        this.mActualPaddingStart = -2.1474836E9f;
        this.mAddAnimationStartIndex = -1;
        this.mCannedAnimationStartIndex = -1;
        this.mSpeedBumpIndex = -1;
        this.mOpenedAmount = 0.0f;
        this.mAnimationsEnabled = true;
        this.mAbsolutePosition = new int[2];
        initDimens();
        setWillNotDraw(true);
    }

    private void initDimens() {
        this.mDotPadding = getResources().getDimensionPixelSize(R.dimen.overflow_icon_dot_padding);
        this.mStaticDotRadius = getResources().getDimensionPixelSize(R.dimen.overflow_dot_radius);
        this.mStaticDotDiameter = 2 * this.mStaticDotRadius;
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Paint paint = new Paint();
        paint.setColor(-65536);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawRect(getActualPaddingStart(), 0.0f, getLayoutEnd(), getHeight(), paint);
    }

    @Override // android.view.View
    protected void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        initDimens();
    }

    @Override // android.widget.FrameLayout, android.view.ViewGroup, android.view.View
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        float height = getHeight() / 2.0f;
        this.mIconSize = 0;
        for (int i5 = 0; i5 < getChildCount(); i5++) {
            View childAt = getChildAt(i5);
            int measuredWidth = childAt.getMeasuredWidth();
            int measuredHeight = childAt.getMeasuredHeight();
            int i6 = (int) (height - (measuredHeight / 2.0f));
            childAt.layout(0, i6, measuredWidth, measuredHeight + i6);
            if (i5 == 0) {
                setIconSize(childAt.getWidth());
            }
        }
        getLocationOnScreen(this.mAbsolutePosition);
        if (this.mIsStaticLayout) {
            updateState();
        }
    }

    private void setIconSize(int i) {
        this.mIconSize = i;
        this.mOverflowWidth = this.mIconSize + (0 * (this.mStaticDotDiameter + this.mDotPadding));
    }

    private void updateState() {
        resetViewStates();
        calculateIconTranslations();
        applyIconStates();
    }

    public void applyIconStates() {
        for (int i = 0; i < getChildCount(); i++) {
            View childAt = getChildAt(i);
            IconState iconState = this.mIconStates.get(childAt);
            if (iconState != null) {
                iconState.applyToView(childAt);
            }
        }
        this.mAddAnimationStartIndex = -1;
        this.mCannedAnimationStartIndex = -1;
        this.mDisallowNextAnimation = false;
        this.mIsolatedIconForAnimation = null;
    }

    @Override // android.view.ViewGroup
    public void onViewAdded(View view) {
        super.onViewAdded(view);
        boolean zIsReplacingIcon = isReplacingIcon(view);
        if (!this.mChangingViewPositions) {
            IconState iconState = new IconState();
            if (zIsReplacingIcon) {
                iconState.justAdded = false;
                iconState.justReplaced = true;
            }
            this.mIconStates.put(view, iconState);
        }
        int iIndexOfChild = indexOfChild(view);
        if (iIndexOfChild < getChildCount() - 1 && !zIsReplacingIcon && this.mIconStates.get(getChildAt(iIndexOfChild + 1)).iconAppearAmount > 0.0f) {
            if (this.mAddAnimationStartIndex < 0) {
                this.mAddAnimationStartIndex = iIndexOfChild;
            } else {
                this.mAddAnimationStartIndex = Math.min(this.mAddAnimationStartIndex, iIndexOfChild);
            }
        }
        if (view instanceof StatusBarIconView) {
            ((StatusBarIconView) view).setDark(this.mDark, false, 0L);
        }
    }

    private boolean isReplacingIcon(View view) {
        if (this.mReplacingIcons == null || !(view instanceof StatusBarIconView)) {
            return false;
        }
        StatusBarIconView statusBarIconView = (StatusBarIconView) view;
        Icon sourceIcon = statusBarIconView.getSourceIcon();
        ArrayList<StatusBarIcon> arrayList = this.mReplacingIcons.get(statusBarIconView.getNotification().getGroupKey());
        return arrayList != null && sourceIcon.sameAs(arrayList.get(0).icon);
    }

    @Override // android.view.ViewGroup
    public void onViewRemoved(View view) {
        super.onViewRemoved(view);
        if (view instanceof StatusBarIconView) {
            boolean zIsReplacingIcon = isReplacingIcon(view);
            final StatusBarIconView statusBarIconView = (StatusBarIconView) view;
            if (statusBarIconView.getVisibleState() != 2 && view.getVisibility() == 0 && zIsReplacingIcon) {
                int iFindFirstViewIndexAfter = findFirstViewIndexAfter(statusBarIconView.getTranslationX());
                if (this.mAddAnimationStartIndex < 0) {
                    this.mAddAnimationStartIndex = iFindFirstViewIndexAfter;
                } else {
                    this.mAddAnimationStartIndex = Math.min(this.mAddAnimationStartIndex, iFindFirstViewIndexAfter);
                }
            }
            if (!this.mChangingViewPositions) {
                this.mIconStates.remove(view);
                if (!zIsReplacingIcon) {
                    addTransientView(statusBarIconView, 0);
                    statusBarIconView.setVisibleState(2, true, new Runnable() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$NotificationIconContainer$sYOppFQ4vSNRi0SYdFbv716CxNY
                        @Override // java.lang.Runnable
                        public final void run() {
                            this.f$0.removeTransientView(statusBarIconView);
                        }
                    }, view == this.mIsolatedIcon ? 110L : 0L);
                }
            }
        }
    }

    private int findFirstViewIndexAfter(float f) {
        for (int i = 0; i < getChildCount(); i++) {
            if (getChildAt(i).getTranslationX() > f) {
                return i;
            }
        }
        return getChildCount();
    }

    public void resetViewStates() {
        for (int i = 0; i < getChildCount(); i++) {
            View childAt = getChildAt(i);
            IconState iconState = this.mIconStates.get(childAt);
            iconState.initFrom(childAt);
            iconState.alpha = (this.mIsolatedIcon == null || childAt == this.mIsolatedIcon) ? 1.0f : 0.0f;
            iconState.hidden = false;
        }
    }

    public void calculateIconTranslations() {
        int i;
        IconState iconState;
        float f;
        int i2;
        float iconScaleFullyDark;
        float f2;
        float actualPaddingStart = getActualPaddingStart();
        int childCount = getChildCount();
        if (this.mDark) {
            i = 5;
        } else {
            i = this.mIsStaticLayout ? 4 : childCount;
        }
        float layoutEnd = getLayoutEnd();
        float maxOverflowStart = getMaxOverflowStart();
        float f3 = 0.0f;
        this.mVisualOverflowStart = 0.0f;
        this.mFirstVisibleIconState = null;
        int i3 = -1;
        boolean z = this.mSpeedBumpIndex != -1 && this.mSpeedBumpIndex < getChildCount();
        float width = actualPaddingStart;
        int i4 = -1;
        int i5 = 0;
        while (i5 < childCount) {
            View childAt = getChildAt(i5);
            IconState iconState2 = this.mIconStates.get(childAt);
            iconState2.xTranslation = width;
            if (this.mFirstVisibleIconState == null) {
                this.mFirstVisibleIconState = iconState2;
            }
            boolean z2 = (this.mSpeedBumpIndex != i3 && i5 >= this.mSpeedBumpIndex && iconState2.iconAppearAmount > f3) || i5 >= i;
            boolean z3 = i5 == childCount + (-1);
            if (this.mDark && (childAt instanceof StatusBarIconView)) {
                iconScaleFullyDark = ((StatusBarIconView) childAt).getIconScaleFullyDark();
            } else {
                iconScaleFullyDark = 1.0f;
            }
            if (this.mOpenedAmount != f3) {
                z3 = (!z3 || z || z2) ? false : true;
            }
            iconState2.visibleState = 0;
            if (z3) {
                f2 = layoutEnd - this.mIconSize;
            } else {
                f2 = maxOverflowStart - this.mIconSize;
            }
            boolean z4 = width > f2;
            if (i4 == -1 && (z2 || z4)) {
                int i6 = (!z3 || z2) ? i5 : i5 - 1;
                this.mVisualOverflowStart = layoutEnd - this.mOverflowWidth;
                if (z2 || this.mIsStaticLayout) {
                    this.mVisualOverflowStart = Math.min(width, this.mVisualOverflowStart);
                }
                i4 = i6;
            }
            width += iconState2.iconAppearAmount * childAt.getWidth() * iconScaleFullyDark;
            i5++;
            f3 = 0.0f;
            i3 = -1;
        }
        this.mNumDots = 0;
        if (i4 != -1) {
            width = this.mVisualOverflowStart;
            for (int i7 = i4; i7 < childCount; i7++) {
                IconState iconState3 = this.mIconStates.get(getChildAt(i7));
                int i8 = this.mStaticDotDiameter + this.mDotPadding;
                iconState3.xTranslation = width;
                if (this.mNumDots < 1) {
                    if (this.mNumDots == 0 && iconState3.iconAppearAmount < 0.8f) {
                        iconState3.visibleState = 0;
                        i2 = 1;
                    } else {
                        i2 = 1;
                        iconState3.visibleState = 1;
                        this.mNumDots++;
                    }
                    if (this.mNumDots == i2) {
                        i8 *= i2;
                    }
                    width += i8 * iconState3.iconAppearAmount;
                    this.mLastVisibleIconState = iconState3;
                } else {
                    iconState3.visibleState = 2;
                }
            }
        } else if (childCount > 0) {
            this.mLastVisibleIconState = this.mIconStates.get(getChildAt(childCount - 1));
            this.mFirstVisibleIconState = this.mIconStates.get(getChildAt(0));
        }
        if (this.mDark && width < getLayoutEnd()) {
            if (this.mFirstVisibleIconState != null) {
                f = this.mFirstVisibleIconState.xTranslation;
            } else {
                f = 0.0f;
            }
            float layoutEnd2 = ((getLayoutEnd() - getActualPaddingStart()) - (getFinalTranslationX() - f)) / 2.0f;
            if (i4 != -1) {
                layoutEnd2 = (((getLayoutEnd() - this.mVisualOverflowStart) / 2.0f) + layoutEnd2) / 2.0f;
            }
            for (int i9 = 0; i9 < childCount; i9++) {
                this.mIconStates.get(getChildAt(i9)).xTranslation += layoutEnd2;
            }
        }
        if (isLayoutRtl()) {
            for (int i10 = 0; i10 < childCount; i10++) {
                IconState iconState4 = this.mIconStates.get(getChildAt(i10));
                iconState4.xTranslation = (getWidth() - iconState4.xTranslation) - r4.getWidth();
            }
        }
        if (this.mIsolatedIcon != null && (iconState = this.mIconStates.get(this.mIsolatedIcon)) != null) {
            iconState.xTranslation = (this.mIsolatedIconLocation.left - this.mAbsolutePosition[0]) - (((1.0f - this.mIsolatedIcon.getIconScale()) * this.mIsolatedIcon.getWidth()) / 2.0f);
            iconState.visibleState = 0;
        }
    }

    private float getLayoutEnd() {
        return getActualWidth() - getActualPaddingEnd();
    }

    private float getActualPaddingEnd() {
        if (this.mActualPaddingEnd == -2.1474836E9f) {
            return getPaddingEnd();
        }
        return this.mActualPaddingEnd;
    }

    private float getActualPaddingStart() {
        if (this.mActualPaddingStart == -2.1474836E9f) {
            return getPaddingStart();
        }
        return this.mActualPaddingStart;
    }

    public void setIsStaticLayout(boolean z) {
        this.mIsStaticLayout = z;
    }

    public void setActualLayoutWidth(int i) {
        this.mActualLayoutWidth = i;
    }

    public void setActualPaddingEnd(float f) {
        this.mActualPaddingEnd = f;
    }

    public void setActualPaddingStart(float f) {
        this.mActualPaddingStart = f;
    }

    public int getActualWidth() {
        if (this.mActualLayoutWidth == Integer.MIN_VALUE) {
            return getWidth();
        }
        return this.mActualLayoutWidth;
    }

    public int getFinalTranslationX() {
        if (this.mLastVisibleIconState == null) {
            return 0;
        }
        return Math.min(getWidth(), (int) (this.mLastVisibleIconState.xTranslation + this.mIconSize));
    }

    private float getMaxOverflowStart() {
        return getLayoutEnd() - this.mOverflowWidth;
    }

    public void setChangingViewPositions(boolean z) {
        this.mChangingViewPositions = z;
    }

    public void setDark(boolean z, boolean z2, long j) {
        this.mDark = z;
        this.mDisallowNextAnimation |= !z2;
        for (int i = 0; i < getChildCount(); i++) {
            View childAt = getChildAt(i);
            if (childAt instanceof StatusBarIconView) {
                ((StatusBarIconView) childAt).setDark(z, z2, j);
            }
        }
    }

    public IconState getIconState(StatusBarIconView statusBarIconView) {
        return this.mIconStates.get(statusBarIconView);
    }

    public void setSpeedBumpIndex(int i) {
        this.mSpeedBumpIndex = i;
    }

    public void setOpenedAmount(float f) {
        this.mOpenedAmount = f;
    }

    public boolean hasOverflow() {
        return this.mNumDots > 0;
    }

    public boolean hasPartialOverflow() {
        return this.mNumDots > 0 && this.mNumDots < 1;
    }

    public int getPartialOverflowExtraPadding() {
        if (!hasPartialOverflow()) {
            return 0;
        }
        int i = (1 - this.mNumDots) * (this.mStaticDotDiameter + this.mDotPadding);
        if (getFinalTranslationX() + i > getWidth()) {
            return getWidth() - getFinalTranslationX();
        }
        return i;
    }

    public int getNoOverflowExtraPadding() {
        if (this.mNumDots != 0) {
            return 0;
        }
        int i = this.mOverflowWidth;
        if (getFinalTranslationX() + i > getWidth()) {
            return getWidth() - getFinalTranslationX();
        }
        return i;
    }

    public void setAnimationsEnabled(boolean z) {
        if (!z && this.mAnimationsEnabled) {
            for (int i = 0; i < getChildCount(); i++) {
                View childAt = getChildAt(i);
                IconState iconState = this.mIconStates.get(childAt);
                if (iconState != null) {
                    iconState.cancelAnimations(childAt);
                    iconState.applyToView(childAt);
                }
            }
        }
        this.mAnimationsEnabled = z;
    }

    public void setReplacingIcons(ArrayMap<String, ArrayList<StatusBarIcon>> arrayMap) {
        this.mReplacingIcons = arrayMap;
    }

    public void showIconIsolated(StatusBarIconView statusBarIconView, boolean z) {
        if (z) {
            this.mIsolatedIconForAnimation = statusBarIconView != null ? statusBarIconView : this.mIsolatedIcon;
        }
        this.mIsolatedIcon = statusBarIconView;
        updateState();
    }

    public void setIsolatedIconLocation(Rect rect, boolean z) {
        this.mIsolatedIconLocation = rect;
        if (z) {
            updateState();
        }
    }

    public class IconState extends ViewState {
        public boolean isLastExpandIcon;
        private boolean justReplaced;
        public boolean needsCannedAnimation;
        public boolean noAnimations;
        public boolean translateContent;
        public boolean useFullTransitionAmount;
        public boolean useLinearTransitionAmount;
        public int visibleState;
        public float iconAppearAmount = 1.0f;
        public float clampedAppearAmount = 1.0f;
        public boolean justAdded = true;
        public int iconColor = 0;
        public int customTransformHeight = Integer.MIN_VALUE;

        public IconState() {
        }

        @Override // com.android.systemui.statusbar.stack.ViewState
        public void applyToView(View view) {
            boolean z;
            AnimationProperties animationProperties;
            AnimationProperties animationProperties2;
            boolean z2;
            if (view instanceof StatusBarIconView) {
                StatusBarIconView statusBarIconView = (StatusBarIconView) view;
                AnimationProperties animationProperties3 = null;
                boolean z3 = (!NotificationIconContainer.this.mAnimationsEnabled || NotificationIconContainer.this.mDisallowNextAnimation || this.noAnimations) ? false : true;
                if (z3) {
                    if (this.justAdded || this.justReplaced) {
                        super.applyToView(statusBarIconView);
                        if (this.justAdded && this.iconAppearAmount != 0.0f) {
                            statusBarIconView.setAlpha(0.0f);
                            statusBarIconView.setVisibleState(2, false);
                            animationProperties = NotificationIconContainer.ADD_ICON_PROPERTIES;
                            animationProperties2 = animationProperties;
                            z2 = true;
                        }
                        animationProperties2 = null;
                        z2 = false;
                    } else {
                        if (this.visibleState != statusBarIconView.getVisibleState()) {
                            animationProperties = NotificationIconContainer.DOT_ANIMATION_PROPERTIES;
                            animationProperties2 = animationProperties;
                            z2 = true;
                        }
                        animationProperties2 = null;
                        z2 = false;
                    }
                    if (!z2 && NotificationIconContainer.this.mAddAnimationStartIndex >= 0 && NotificationIconContainer.this.indexOfChild(view) >= NotificationIconContainer.this.mAddAnimationStartIndex && (statusBarIconView.getVisibleState() != 2 || this.visibleState != 2)) {
                        animationProperties2 = NotificationIconContainer.DOT_ANIMATION_PROPERTIES;
                        z2 = true;
                    }
                    if (this.needsCannedAnimation) {
                        AnimationFilter animationFilter = NotificationIconContainer.sTempProperties.getAnimationFilter();
                        animationFilter.reset();
                        animationFilter.combineFilter(NotificationIconContainer.ICON_ANIMATION_PROPERTIES.getAnimationFilter());
                        NotificationIconContainer.sTempProperties.resetCustomInterpolators();
                        NotificationIconContainer.sTempProperties.combineCustomInterpolators(NotificationIconContainer.ICON_ANIMATION_PROPERTIES);
                        if (animationProperties2 != null) {
                            animationFilter.combineFilter(animationProperties2.getAnimationFilter());
                            NotificationIconContainer.sTempProperties.combineCustomInterpolators(animationProperties2);
                        }
                        animationProperties2 = NotificationIconContainer.sTempProperties;
                        animationProperties2.setDuration(100L);
                        NotificationIconContainer.this.mCannedAnimationStartIndex = NotificationIconContainer.this.indexOfChild(view);
                        z2 = true;
                    }
                    if (z2 || NotificationIconContainer.this.mCannedAnimationStartIndex < 0 || NotificationIconContainer.this.indexOfChild(view) <= NotificationIconContainer.this.mCannedAnimationStartIndex || (statusBarIconView.getVisibleState() == 2 && this.visibleState == 2)) {
                        AnimationProperties animationProperties4 = animationProperties2;
                        z = z2;
                        animationProperties3 = animationProperties4;
                    } else {
                        AnimationFilter animationFilter2 = NotificationIconContainer.sTempProperties.getAnimationFilter();
                        animationFilter2.reset();
                        animationFilter2.animateX();
                        NotificationIconContainer.sTempProperties.resetCustomInterpolators();
                        animationProperties3 = NotificationIconContainer.sTempProperties;
                        animationProperties3.setDuration(100L);
                        z = true;
                    }
                    if (NotificationIconContainer.this.mIsolatedIconForAnimation != null) {
                        if (view == NotificationIconContainer.this.mIsolatedIconForAnimation) {
                            animationProperties3 = NotificationIconContainer.UNISOLATION_PROPERTY;
                            animationProperties3.setDelay(NotificationIconContainer.this.mIsolatedIcon != null ? 100L : 0L);
                        } else {
                            animationProperties3 = NotificationIconContainer.UNISOLATION_PROPERTY_OTHERS;
                            animationProperties3.setDelay(NotificationIconContainer.this.mIsolatedIcon == null ? 100L : 0L);
                        }
                        z = true;
                    }
                } else {
                    z = false;
                }
                statusBarIconView.setVisibleState(this.visibleState, z3);
                statusBarIconView.setIconColor(this.iconColor, this.needsCannedAnimation && z3);
                if (z) {
                    animateTo(statusBarIconView, animationProperties3);
                } else {
                    super.applyToView(view);
                }
                statusBarIconView.setIsInShelf(this.iconAppearAmount == 1.0f);
            }
            this.justAdded = false;
            this.justReplaced = false;
            this.needsCannedAnimation = false;
        }

        public boolean hasCustomTransformHeight() {
            return this.isLastExpandIcon && this.customTransformHeight != Integer.MIN_VALUE;
        }

        @Override // com.android.systemui.statusbar.stack.ViewState
        public void initFrom(View view) {
            super.initFrom(view);
            if (view instanceof StatusBarIconView) {
                this.iconColor = ((StatusBarIconView) view).getStaticDrawableColor();
            }
        }
    }
}
