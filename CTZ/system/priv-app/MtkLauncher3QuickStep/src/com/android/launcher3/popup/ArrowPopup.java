package com.android.launcher3.popup;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.CornerPathEffect;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.ShapeDrawable;
import android.util.AttributeSet;
import android.util.Property;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.AccelerateDecelerateInterpolator;
import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAnimUtils;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.anim.RevealOutlineAnimation;
import com.android.launcher3.anim.RoundedRectRevealOutlineProvider;
import com.android.launcher3.dragndrop.DragLayer;
import com.android.launcher3.graphics.TriangleShape;
import com.android.launcher3.util.Themes;
import com.android.launcher3.views.BaseDragLayer;
import java.util.ArrayList;
import java.util.Collections;

/* loaded from: classes.dex */
public abstract class ArrowPopup extends AbstractFloatingView {
    private final int mArrayOffset;
    private final View mArrow;
    protected boolean mDeferContainerRemoval;
    private final Rect mEndRect;
    private int mGravity;
    protected final LayoutInflater mInflater;
    protected boolean mIsAboveIcon;
    protected boolean mIsLeftAligned;
    protected final boolean mIsRtl;
    protected final Launcher mLauncher;
    protected Animator mOpenCloseAnimator;
    private final float mOutlineRadius;
    private final Rect mStartRect;
    private final Rect mTempRect;

    protected abstract void getTargetObjectLocation(Rect rect);

    public ArrowPopup(Context context, AttributeSet attributeSet, int i) throws Resources.NotFoundException {
        super(context, attributeSet, i);
        this.mTempRect = new Rect();
        this.mStartRect = new Rect();
        this.mEndRect = new Rect();
        this.mInflater = LayoutInflater.from(context);
        this.mOutlineRadius = getResources().getDimension(R.dimen.bg_round_rect_radius);
        this.mLauncher = Launcher.getLauncher(context);
        this.mIsRtl = Utilities.isRtl(getResources());
        setClipToOutline(true);
        setOutlineProvider(new ViewOutlineProvider() { // from class: com.android.launcher3.popup.ArrowPopup.1
            AnonymousClass1() {
            }

            @Override // android.view.ViewOutlineProvider
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), ArrowPopup.this.mOutlineRadius);
            }
        });
        Resources resources = getResources();
        int dimensionPixelSize = resources.getDimensionPixelSize(R.dimen.popup_arrow_width);
        int dimensionPixelSize2 = resources.getDimensionPixelSize(R.dimen.popup_arrow_height);
        this.mArrow = new View(context);
        this.mArrow.setLayoutParams(new BaseDragLayer.LayoutParams(dimensionPixelSize, dimensionPixelSize2));
        this.mArrayOffset = resources.getDimensionPixelSize(R.dimen.popup_arrow_vertical_offset);
    }

    /* renamed from: com.android.launcher3.popup.ArrowPopup$1 */
    class AnonymousClass1 extends ViewOutlineProvider {
        AnonymousClass1() {
        }

        @Override // android.view.ViewOutlineProvider
        public void getOutline(View view, Outline outline) {
            outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), ArrowPopup.this.mOutlineRadius);
        }
    }

    public ArrowPopup(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public ArrowPopup(Context context) {
        this(context, null, 0);
    }

    @Override // com.android.launcher3.AbstractFloatingView
    protected void handleClose(boolean z) {
        if (z) {
            animateClose();
        } else {
            closeComplete();
        }
    }

    public <T extends View> T inflateAndAdd(int i, ViewGroup viewGroup) {
        T t = (T) this.mInflater.inflate(i, viewGroup, false);
        viewGroup.addView(t);
        return t;
    }

    protected void onInflationComplete(boolean z) {
    }

    protected void reorderAndShow(int i) throws Resources.NotFoundException {
        int i2;
        setVisibility(4);
        this.mIsOpen = true;
        this.mLauncher.getDragLayer().addView(this);
        orientAboutObject();
        boolean z = this.mIsAboveIcon;
        if (z) {
            int childCount = getChildCount();
            ArrayList arrayList = new ArrayList(childCount);
            for (int i3 = 0; i3 < childCount; i3++) {
                if (i3 == i) {
                    Collections.reverse(arrayList);
                }
                arrayList.add(getChildAt(i3));
            }
            Collections.reverse(arrayList);
            removeAllViews();
            for (int i4 = 0; i4 < childCount; i4++) {
                addView((View) arrayList.get(i4));
            }
            orientAboutObject();
        }
        onInflationComplete(z);
        Resources resources = getResources();
        if (isAlignedWithStart()) {
            i2 = R.dimen.popup_arrow_horizontal_center_start;
        } else {
            i2 = R.dimen.popup_arrow_horizontal_center_end;
        }
        int dimensionPixelSize = resources.getDimensionPixelSize(i2);
        int dimensionPixelSize2 = resources.getDimensionPixelSize(R.dimen.popup_arrow_width) / 2;
        this.mLauncher.getDragLayer().addView(this.mArrow);
        BaseDragLayer.LayoutParams layoutParams = (BaseDragLayer.LayoutParams) this.mArrow.getLayoutParams();
        if (this.mIsLeftAligned) {
            this.mArrow.setX((getX() + dimensionPixelSize) - dimensionPixelSize2);
        } else {
            this.mArrow.setX(((getX() + getMeasuredWidth()) - dimensionPixelSize) - dimensionPixelSize2);
        }
        if (Gravity.isVertical(this.mGravity)) {
            this.mArrow.setVisibility(4);
        } else {
            ShapeDrawable shapeDrawable = new ShapeDrawable(TriangleShape.create(layoutParams.width, layoutParams.height, true ^ this.mIsAboveIcon));
            Paint paint = shapeDrawable.getPaint();
            paint.setColor(Themes.getAttrColor(this.mLauncher, R.attr.popupColorPrimary));
            paint.setPathEffect(new CornerPathEffect(getResources().getDimensionPixelSize(R.dimen.popup_arrow_corner_radius)));
            this.mArrow.setBackground(shapeDrawable);
            this.mArrow.setElevation(getElevation());
        }
        this.mArrow.setPivotX(layoutParams.width / 2);
        this.mArrow.setPivotY(this.mIsAboveIcon ? 0.0f : layoutParams.height);
        animateOpen();
    }

    protected boolean isAlignedWithStart() {
        return (this.mIsLeftAligned && !this.mIsRtl) || (!this.mIsLeftAligned && this.mIsRtl);
    }

    protected void orientAboutObject() {
        int dimensionPixelSize;
        int i;
        measure(0, 0);
        int measuredWidth = getMeasuredWidth();
        int dimensionPixelSize2 = this.mArrow.getLayoutParams().height + this.mArrayOffset + getResources().getDimensionPixelSize(R.dimen.popup_vertical_padding);
        int measuredHeight = getMeasuredHeight() + dimensionPixelSize2;
        getTargetObjectLocation(this.mTempRect);
        DragLayer dragLayer = this.mLauncher.getDragLayer();
        Rect insets = dragLayer.getInsets();
        int i2 = this.mTempRect.left;
        int i3 = this.mTempRect.right - measuredWidth;
        int i4 = (!((i2 + measuredWidth) + insets.left < dragLayer.getRight() - insets.right) || (this.mIsRtl && (i3 > dragLayer.getLeft() + insets.left))) ? i3 : i2;
        this.mIsLeftAligned = i4 == i2;
        int iWidth = this.mTempRect.width();
        Resources resources = getResources();
        if (isAlignedWithStart()) {
            dimensionPixelSize = ((iWidth / 2) - (resources.getDimensionPixelSize(R.dimen.deep_shortcut_icon_size) / 2)) - resources.getDimensionPixelSize(R.dimen.popup_padding_start);
        } else {
            dimensionPixelSize = ((iWidth / 2) - (resources.getDimensionPixelSize(R.dimen.deep_shortcut_drag_handle_size) / 2)) - resources.getDimensionPixelSize(R.dimen.popup_padding_end);
        }
        if (!this.mIsLeftAligned) {
            dimensionPixelSize = -dimensionPixelSize;
        }
        int i5 = i4 + dimensionPixelSize;
        int iHeight = this.mTempRect.height();
        int i6 = this.mTempRect.top - measuredHeight;
        this.mIsAboveIcon = i6 > dragLayer.getTop() + insets.top;
        if (!this.mIsAboveIcon) {
            i6 = this.mTempRect.top + iHeight + dimensionPixelSize2;
        }
        if (this.mIsRtl) {
            i = i5 + insets.right;
        } else {
            i = i5 - insets.left;
        }
        int i7 = i6 - insets.top;
        this.mGravity = 0;
        if (measuredHeight + i7 > dragLayer.getBottom() - insets.bottom) {
            this.mGravity = 16;
            int i8 = (i2 + iWidth) - insets.left;
            int i9 = (i3 - iWidth) - insets.left;
            if (!this.mIsRtl) {
                if (measuredWidth + i8 < dragLayer.getRight()) {
                    this.mIsLeftAligned = true;
                    i = i8;
                } else {
                    this.mIsLeftAligned = false;
                    i = i9;
                }
            } else if (i9 > dragLayer.getLeft()) {
                this.mIsLeftAligned = false;
                i = i9;
            } else {
                this.mIsLeftAligned = true;
                i = i8;
            }
            this.mIsAboveIcon = true;
        }
        setX(i);
        if (Gravity.isVertical(this.mGravity)) {
            return;
        }
        BaseDragLayer.LayoutParams layoutParams = (BaseDragLayer.LayoutParams) getLayoutParams();
        BaseDragLayer.LayoutParams layoutParams2 = (BaseDragLayer.LayoutParams) this.mArrow.getLayoutParams();
        if (this.mIsAboveIcon) {
            layoutParams.gravity = 80;
            layoutParams2.gravity = 80;
            layoutParams.bottomMargin = ((this.mLauncher.getDragLayer().getHeight() - i7) - getMeasuredHeight()) - insets.top;
            layoutParams2.bottomMargin = ((layoutParams.bottomMargin - layoutParams2.height) - this.mArrayOffset) - insets.bottom;
            return;
        }
        layoutParams.gravity = 48;
        layoutParams2.gravity = 48;
        layoutParams.topMargin = i7 + insets.top;
        layoutParams2.topMargin = ((layoutParams.topMargin - insets.top) - layoutParams2.height) - this.mArrayOffset;
    }

    @Override // android.widget.LinearLayout, android.view.ViewGroup, android.view.View
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        DragLayer dragLayer = this.mLauncher.getDragLayer();
        if (getTranslationX() + i < 0.0f || getTranslationX() + i3 > dragLayer.getWidth()) {
            this.mGravity |= 1;
        }
        if (Gravity.isHorizontal(this.mGravity)) {
            setX((dragLayer.getWidth() / 2) - (getMeasuredWidth() / 2));
            this.mArrow.setVisibility(4);
        }
        if (Gravity.isVertical(this.mGravity)) {
            setY((dragLayer.getHeight() / 2) - (getMeasuredHeight() / 2));
        }
    }

    private void animateOpen() {
        setVisibility(0);
        AnimatorSet animatorSetCreateAnimatorSet = LauncherAnimUtils.createAnimatorSet();
        long integer = getResources().getInteger(R.integer.config_popupOpenCloseDuration);
        AccelerateDecelerateInterpolator accelerateDecelerateInterpolator = new AccelerateDecelerateInterpolator();
        ValueAnimator valueAnimatorCreateRevealAnimator = createOpenCloseOutlineProvider().createRevealAnimator(this, false);
        valueAnimatorCreateRevealAnimator.setDuration(integer);
        valueAnimatorCreateRevealAnimator.setInterpolator(accelerateDecelerateInterpolator);
        ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(this, (Property<ArrowPopup, Float>) ALPHA, 0.0f, 1.0f);
        objectAnimatorOfFloat.setDuration(integer);
        objectAnimatorOfFloat.setInterpolator(accelerateDecelerateInterpolator);
        animatorSetCreateAnimatorSet.play(objectAnimatorOfFloat);
        this.mArrow.setScaleX(0.0f);
        this.mArrow.setScaleY(0.0f);
        ObjectAnimator duration = ObjectAnimator.ofFloat(this.mArrow, LauncherAnimUtils.SCALE_PROPERTY, 1.0f).setDuration(r2.getInteger(R.integer.config_popupArrowOpenDuration));
        animatorSetCreateAnimatorSet.addListener(new AnimatorListenerAdapter() { // from class: com.android.launcher3.popup.ArrowPopup.2
            AnonymousClass2() {
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                ArrowPopup.this.announceAccessibilityChanges();
                ArrowPopup.this.mOpenCloseAnimator = null;
            }
        });
        this.mOpenCloseAnimator = animatorSetCreateAnimatorSet;
        animatorSetCreateAnimatorSet.playSequentially(valueAnimatorCreateRevealAnimator, duration);
        animatorSetCreateAnimatorSet.start();
    }

    /* renamed from: com.android.launcher3.popup.ArrowPopup$2 */
    class AnonymousClass2 extends AnimatorListenerAdapter {
        AnonymousClass2() {
        }

        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animator) {
            ArrowPopup.this.announceAccessibilityChanges();
            ArrowPopup.this.mOpenCloseAnimator = null;
        }
    }

    protected void animateClose() {
        if (!this.mIsOpen) {
            return;
        }
        this.mEndRect.setEmpty();
        if (getOutlineProvider() instanceof RevealOutlineAnimation) {
            ((RevealOutlineAnimation) getOutlineProvider()).getOutline(this.mEndRect);
        }
        if (this.mOpenCloseAnimator != null) {
            this.mOpenCloseAnimator.cancel();
        }
        this.mIsOpen = false;
        AnimatorSet animatorSetCreateAnimatorSet = LauncherAnimUtils.createAnimatorSet();
        animatorSetCreateAnimatorSet.play(ObjectAnimator.ofFloat(this.mArrow, LauncherAnimUtils.SCALE_PROPERTY, 0.0f));
        animatorSetCreateAnimatorSet.play(ObjectAnimator.ofFloat(this.mArrow, (Property<View, Float>) ALPHA, 0.0f));
        Resources resources = getResources();
        AccelerateDecelerateInterpolator accelerateDecelerateInterpolator = new AccelerateDecelerateInterpolator();
        ValueAnimator valueAnimatorCreateRevealAnimator = createOpenCloseOutlineProvider().createRevealAnimator(this, true);
        valueAnimatorCreateRevealAnimator.setInterpolator(accelerateDecelerateInterpolator);
        animatorSetCreateAnimatorSet.play(valueAnimatorCreateRevealAnimator);
        ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(this, (Property<ArrowPopup, Float>) ALPHA, 0.0f);
        objectAnimatorOfFloat.setInterpolator(accelerateDecelerateInterpolator);
        animatorSetCreateAnimatorSet.play(objectAnimatorOfFloat);
        onCreateCloseAnimation(animatorSetCreateAnimatorSet);
        animatorSetCreateAnimatorSet.setDuration(resources.getInteger(R.integer.config_popupOpenCloseDuration));
        animatorSetCreateAnimatorSet.addListener(new AnimatorListenerAdapter() { // from class: com.android.launcher3.popup.ArrowPopup.3
            AnonymousClass3() {
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                ArrowPopup.this.mOpenCloseAnimator = null;
                if (ArrowPopup.this.mDeferContainerRemoval) {
                    ArrowPopup.this.setVisibility(4);
                } else {
                    ArrowPopup.this.closeComplete();
                }
            }
        });
        this.mOpenCloseAnimator = animatorSetCreateAnimatorSet;
        animatorSetCreateAnimatorSet.start();
    }

    /* renamed from: com.android.launcher3.popup.ArrowPopup$3 */
    class AnonymousClass3 extends AnimatorListenerAdapter {
        AnonymousClass3() {
        }

        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animator) {
            ArrowPopup.this.mOpenCloseAnimator = null;
            if (ArrowPopup.this.mDeferContainerRemoval) {
                ArrowPopup.this.setVisibility(4);
            } else {
                ArrowPopup.this.closeComplete();
            }
        }
    }

    protected void onCreateCloseAnimation(AnimatorSet animatorSet) {
    }

    private RoundedRectRevealOutlineProvider createOpenCloseOutlineProvider() throws Resources.NotFoundException {
        int i;
        Resources resources = getResources();
        if (this.mIsLeftAligned ^ this.mIsRtl) {
            i = R.dimen.popup_arrow_horizontal_center_start;
        } else {
            i = R.dimen.popup_arrow_horizontal_center_end;
        }
        int dimensionPixelSize = resources.getDimensionPixelSize(i);
        if (!this.mIsLeftAligned) {
            dimensionPixelSize = getMeasuredWidth() - dimensionPixelSize;
        }
        int measuredHeight = this.mIsAboveIcon ? getMeasuredHeight() : 0;
        this.mStartRect.set(dimensionPixelSize, measuredHeight, dimensionPixelSize, measuredHeight);
        if (this.mEndRect.isEmpty()) {
            this.mEndRect.set(0, 0, getMeasuredWidth(), getMeasuredHeight());
        }
        return new RoundedRectRevealOutlineProvider(this.mOutlineRadius, this.mOutlineRadius, this.mStartRect, this.mEndRect);
    }

    protected void closeComplete() {
        if (this.mOpenCloseAnimator != null) {
            this.mOpenCloseAnimator.cancel();
            this.mOpenCloseAnimator = null;
        }
        this.mIsOpen = false;
        this.mDeferContainerRemoval = false;
        this.mLauncher.getDragLayer().removeView(this);
        this.mLauncher.getDragLayer().removeView(this.mArrow);
    }
}
