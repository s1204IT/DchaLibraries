package com.android.systemui.statusbar;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import com.android.settingslib.Utils;
import com.android.systemui.R;
import com.android.systemui.statusbar.notification.AnimatableProperty;
import com.android.systemui.statusbar.notification.PropertyAnimator;
import com.android.systemui.statusbar.stack.AnimationProperties;
import java.util.function.BiConsumer;
import java.util.function.Function;

public abstract class ExpandableOutlineView extends ExpandableView {
    private boolean mAlwaysRoundBothCorners;
    private int mBackgroundTop;
    private float mBottomRoundness;
    private final Path mClipPath;
    private float mCurrentBottomRoundness;
    private float mCurrentTopRoundness;
    private boolean mCustomOutline;
    private float mDistanceToTopRoundness;
    private float mExtraWidthForClipping;
    private int mMinimumHeightForClipping;
    private float mOutlineAlpha;
    protected float mOutlineRadius;
    private final Rect mOutlineRect;
    private final ViewOutlineProvider mProvider;
    protected boolean mShouldTranslateContents;
    private Path mTmpPath;
    private Path mTmpPath2;
    private boolean mTopAmountRounded;
    private float mTopRoundness;
    private static final AnimatableProperty TOP_ROUNDNESS = AnimatableProperty.from("topRoundness", new BiConsumer() {
        @Override
        public final void accept(Object obj, Object obj2) {
            ((ExpandableOutlineView) obj).setTopRoundnessInternal(((Float) obj2).floatValue());
        }
    }, new Function() {
        @Override
        public final Object apply(Object obj) {
            return Float.valueOf(((ExpandableOutlineView) obj).getCurrentTopRoundness());
        }
    }, R.id.top_roundess_animator_tag, R.id.top_roundess_animator_end_tag, R.id.top_roundess_animator_start_tag);
    private static final AnimatableProperty BOTTOM_ROUNDNESS = AnimatableProperty.from("bottomRoundness", new BiConsumer() {
        @Override
        public final void accept(Object obj, Object obj2) {
            ((ExpandableOutlineView) obj).setBottomRoundnessInternal(((Float) obj2).floatValue());
        }
    }, new Function() {
        @Override
        public final Object apply(Object obj) {
            return Float.valueOf(((ExpandableOutlineView) obj).getCurrentBottomRoundness());
        }
    }, R.id.bottom_roundess_animator_tag, R.id.bottom_roundess_animator_end_tag, R.id.bottom_roundess_animator_start_tag);
    private static final AnimationProperties ROUNDNESS_PROPERTIES = new AnimationProperties().setDuration(360);
    private static final Path EMPTY_PATH = new Path();

    private Path getClipPath() {
        return getClipPath(false, false);
    }

    protected Path getClipPath(boolean z, boolean z2) {
        int iMax;
        int i;
        int iMin;
        int iMax2;
        int translation;
        Path path = null;
        if (!this.mCustomOutline) {
            if (this.mShouldTranslateContents && !z) {
                translation = (int) getTranslation();
            } else {
                translation = 0;
            }
            iMax = Math.max(translation, 0);
            i = this.mClipTopAmount + this.mBackgroundTop;
            iMin = Math.min(translation, 0) + getWidth();
            iMax2 = Math.max(getActualHeight(), i);
            int iMax3 = Math.max(getActualHeight() - this.mClipBottomAmount, i);
            if (iMax2 != iMax3) {
                if (!z2) {
                    getRoundedRectPath(iMax, i, iMin, iMax3, 0.0f, 0.0f, this.mTmpPath2);
                    path = this.mTmpPath2;
                } else {
                    iMax2 = iMax3;
                }
            }
        } else {
            iMax = this.mOutlineRect.left;
            i = this.mOutlineRect.top;
            iMin = this.mOutlineRect.right;
            iMax2 = this.mOutlineRect.bottom;
        }
        int i2 = iMin;
        int i3 = iMax;
        int i4 = iMax2;
        int i5 = i;
        int i6 = i4 - i5;
        if (i6 == 0) {
            return EMPTY_PATH;
        }
        float currentBackgroundRadiusTop = this.mAlwaysRoundBothCorners ? this.mOutlineRadius : getCurrentBackgroundRadiusTop();
        float currentBackgroundRadiusBottom = this.mAlwaysRoundBothCorners ? this.mOutlineRadius : getCurrentBackgroundRadiusBottom();
        float f = currentBackgroundRadiusTop + currentBackgroundRadiusBottom;
        float f2 = i6;
        if (f > f2) {
            float f3 = f - f2;
            currentBackgroundRadiusTop -= (this.mCurrentTopRoundness * f3) / (this.mCurrentTopRoundness + this.mCurrentBottomRoundness);
            currentBackgroundRadiusBottom -= (f3 * this.mCurrentBottomRoundness) / (this.mCurrentTopRoundness + this.mCurrentBottomRoundness);
        }
        getRoundedRectPath(i3, i5, i2, i4, currentBackgroundRadiusTop, currentBackgroundRadiusBottom, this.mTmpPath);
        Path path2 = this.mTmpPath;
        if (path != null) {
            path2.op(path, Path.Op.INTERSECT);
        }
        return path2;
    }

    public static void getRoundedRectPath(int i, int i2, int i3, int i4, float f, float f2, Path path) {
        path.reset();
        float f3 = (i3 - i) / 2;
        float fMin = Math.min(f3, f);
        float fMin2 = Math.min(f3, f2);
        if (f > 0.0f) {
            float f4 = i;
            float f5 = i2;
            float f6 = f + f5;
            path.moveTo(f4, f6);
            path.quadTo(f4, f5, f4 + fMin, f5);
            float f7 = i3;
            path.lineTo(f7 - fMin, f5);
            path.quadTo(f7, f5, f7, f6);
        } else {
            float f8 = i2;
            path.moveTo(i, f8);
            path.lineTo(i3, f8);
        }
        if (f2 > 0.0f) {
            float f9 = i3;
            float f10 = i4;
            float f11 = f10 - f2;
            path.lineTo(f9, f11);
            path.quadTo(f9, f10, f9 - fMin2, f10);
            float f12 = i;
            path.lineTo(fMin2 + f12, f10);
            path.quadTo(f12, f10, f12, f11);
        } else {
            float f13 = i3;
            float f14 = i4;
            path.lineTo(f13, f14);
            path.lineTo(i, f14);
        }
        path.close();
    }

    public ExpandableOutlineView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mOutlineRect = new Rect();
        this.mClipPath = new Path();
        this.mOutlineAlpha = -1.0f;
        this.mTmpPath = new Path();
        this.mTmpPath2 = new Path();
        this.mDistanceToTopRoundness = -1.0f;
        this.mMinimumHeightForClipping = 0;
        this.mProvider = new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                if (ExpandableOutlineView.this.mCustomOutline || ExpandableOutlineView.this.mCurrentTopRoundness != 0.0f || ExpandableOutlineView.this.mCurrentBottomRoundness != 0.0f || ExpandableOutlineView.this.mAlwaysRoundBothCorners || ExpandableOutlineView.this.mTopAmountRounded) {
                    Path clipPath = ExpandableOutlineView.this.getClipPath();
                    if (clipPath != null && clipPath.isConvex()) {
                        outline.setConvexPath(clipPath);
                    }
                } else {
                    int translation = ExpandableOutlineView.this.mShouldTranslateContents ? (int) ExpandableOutlineView.this.getTranslation() : 0;
                    int iMax = Math.max(translation, 0);
                    int i = ExpandableOutlineView.this.mClipTopAmount + ExpandableOutlineView.this.mBackgroundTop;
                    outline.setRect(iMax, i, ExpandableOutlineView.this.getWidth() + Math.min(translation, 0), Math.max(ExpandableOutlineView.this.getActualHeight() - ExpandableOutlineView.this.mClipBottomAmount, i));
                }
                outline.setAlpha(ExpandableOutlineView.this.mOutlineAlpha);
            }
        };
        setOutlineProvider(this.mProvider);
        initDimens();
    }

    @Override
    protected boolean drawChild(Canvas canvas, View view, long j) {
        Path path;
        canvas.save();
        if (this.mTopAmountRounded && topAmountNeedsClipping()) {
            int i = (int) ((-this.mExtraWidthForClipping) / 2.0f);
            int i2 = (int) (this.mClipTopAmount - this.mDistanceToTopRoundness);
            getRoundedRectPath(i, i2, ((int) (this.mExtraWidthForClipping + i)) + getWidth(), (int) Math.max(this.mMinimumHeightForClipping, Math.max(getActualHeight() - this.mClipBottomAmount, i2 + this.mOutlineRadius)), this.mOutlineRadius, 0.0f, this.mClipPath);
            path = this.mClipPath;
        } else {
            path = null;
        }
        boolean z = false;
        if (childNeedsClipping(view)) {
            Path customClipPath = getCustomClipPath(view);
            if (customClipPath == null) {
                customClipPath = getClipPath();
            }
            if (customClipPath != null) {
                if (path != null) {
                    customClipPath.op(path, Path.Op.INTERSECT);
                }
                canvas.clipPath(customClipPath);
                z = true;
            }
        }
        if (!z && path != null) {
            canvas.clipPath(path);
        }
        boolean zDrawChild = super.drawChild(canvas, view, j);
        canvas.restore();
        return zDrawChild;
    }

    public void setExtraWidthForClipping(float f) {
        this.mExtraWidthForClipping = f;
    }

    public void setMinimumHeightForClipping(int i) {
        this.mMinimumHeightForClipping = i;
    }

    @Override
    public void setDistanceToTopRoundness(float f) {
        super.setDistanceToTopRoundness(f);
        if (f != this.mDistanceToTopRoundness) {
            this.mTopAmountRounded = f >= 0.0f;
            this.mDistanceToTopRoundness = f;
            applyRoundness();
        }
    }

    protected boolean childNeedsClipping(View view) {
        return false;
    }

    public boolean topAmountNeedsClipping() {
        return true;
    }

    protected boolean isClippingNeeded() {
        return this.mAlwaysRoundBothCorners || this.mCustomOutline || getTranslation() != 0.0f;
    }

    private void initDimens() {
        Resources resources = getResources();
        this.mShouldTranslateContents = resources.getBoolean(R.bool.config_translateNotificationContentsOnSwipe);
        this.mOutlineRadius = resources.getDimension(R.dimen.notification_shadow_radius);
        this.mAlwaysRoundBothCorners = resources.getBoolean(R.bool.config_clipNotificationsToOutline);
        if (!this.mAlwaysRoundBothCorners) {
            this.mOutlineRadius = resources.getDimensionPixelSize(Utils.getThemeAttr(this.mContext, android.R.attr.dialogCornerRadius));
        }
        setClipToOutline(this.mAlwaysRoundBothCorners);
    }

    public boolean setTopRoundness(float f, boolean z) {
        if (this.mTopRoundness != f) {
            this.mTopRoundness = f;
            PropertyAnimator.setProperty(this, TOP_ROUNDNESS, f, ROUNDNESS_PROPERTIES, z);
            return true;
        }
        return false;
    }

    protected void applyRoundness() {
        invalidateOutline();
        invalidate();
    }

    public float getCurrentBackgroundRadiusTop() {
        if (this.mTopAmountRounded) {
            return this.mOutlineRadius;
        }
        return this.mCurrentTopRoundness * this.mOutlineRadius;
    }

    public float getCurrentTopRoundness() {
        return this.mCurrentTopRoundness;
    }

    public float getCurrentBottomRoundness() {
        return this.mCurrentBottomRoundness;
    }

    protected float getCurrentBackgroundRadiusBottom() {
        return this.mCurrentBottomRoundness * this.mOutlineRadius;
    }

    public boolean setBottomRoundness(float f, boolean z) {
        if (this.mBottomRoundness != f) {
            this.mBottomRoundness = f;
            PropertyAnimator.setProperty(this, BOTTOM_ROUNDNESS, f, ROUNDNESS_PROPERTIES, z);
            return true;
        }
        return false;
    }

    protected void setBackgroundTop(int i) {
        if (this.mBackgroundTop != i) {
            this.mBackgroundTop = i;
            invalidateOutline();
        }
    }

    private void setTopRoundnessInternal(float f) {
        this.mCurrentTopRoundness = f;
        applyRoundness();
    }

    private void setBottomRoundnessInternal(float f) {
        this.mCurrentBottomRoundness = f;
        applyRoundness();
    }

    public void onDensityOrFontScaleChanged() {
        initDimens();
        applyRoundness();
    }

    @Override
    public void setActualHeight(int i, boolean z) {
        int actualHeight = getActualHeight();
        super.setActualHeight(i, z);
        if (actualHeight != i) {
            applyRoundness();
        }
    }

    @Override
    public void setClipTopAmount(int i) {
        int clipTopAmount = getClipTopAmount();
        super.setClipTopAmount(i);
        if (clipTopAmount != i) {
            applyRoundness();
        }
    }

    @Override
    public void setClipBottomAmount(int i) {
        int clipBottomAmount = getClipBottomAmount();
        super.setClipBottomAmount(i);
        if (clipBottomAmount != i) {
            applyRoundness();
        }
    }

    protected void setOutlineAlpha(float f) {
        if (f != this.mOutlineAlpha) {
            this.mOutlineAlpha = f;
            applyRoundness();
        }
    }

    @Override
    public float getOutlineAlpha() {
        return this.mOutlineAlpha;
    }

    protected void setOutlineRect(RectF rectF) {
        if (rectF != null) {
            setOutlineRect(rectF.left, rectF.top, rectF.right, rectF.bottom);
        } else {
            this.mCustomOutline = false;
            applyRoundness();
        }
    }

    @Override
    public int getOutlineTranslation() {
        return this.mCustomOutline ? this.mOutlineRect.left : (int) getTranslation();
    }

    public void updateOutline() {
        if (this.mCustomOutline) {
            return;
        }
        setOutlineProvider(needsOutline() ? this.mProvider : null);
    }

    protected boolean needsOutline() {
        if (isChildInGroup()) {
            return isGroupExpanded() && !isGroupExpansionChanging();
        }
        if (isSummaryWithChildren()) {
            return !isGroupExpanded() || isGroupExpansionChanging();
        }
        return true;
    }

    protected void setOutlineRect(float f, float f2, float f3, float f4) {
        this.mCustomOutline = true;
        this.mOutlineRect.set((int) f, (int) f2, (int) f3, (int) f4);
        this.mOutlineRect.bottom = (int) Math.max(f2, this.mOutlineRect.bottom);
        this.mOutlineRect.right = (int) Math.max(f, this.mOutlineRect.right);
        applyRoundness();
    }

    public Path getCustomClipPath(View view) {
        return null;
    }
}
