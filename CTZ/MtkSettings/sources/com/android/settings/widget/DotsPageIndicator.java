package com.android.settings.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Build;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import com.android.settings.R;
import java.util.Arrays;

public class DotsPageIndicator extends View implements ViewPager.OnPageChangeListener {
    public static final String TAG = DotsPageIndicator.class.getSimpleName();
    private long animDuration;
    private long animHalfDuration;
    private boolean attachedState;
    private final Path combinedUnselectedPath;
    float controlX1;
    float controlX2;
    float controlY1;
    float controlY2;
    private int currentPage;
    private float dotBottomY;
    private float[] dotCenterX;
    private float dotCenterY;
    private int dotDiameter;
    private float dotRadius;
    private float[] dotRevealFractions;
    private float dotTopY;
    float endX1;
    float endX2;
    float endY1;
    float endY2;
    private int gap;
    private float halfDotRadius;
    private final Interpolator interpolator;
    private AnimatorSet joiningAnimationSet;
    private ValueAnimator[] joiningAnimations;
    private float[] joiningFractions;
    private ValueAnimator moveAnimation;
    private ViewPager.OnPageChangeListener pageChangeListener;
    private int pageCount;
    private final RectF rectF;
    private PendingRetreatAnimator retreatAnimation;
    private float retreatingJoinX1;
    private float retreatingJoinX2;
    private PendingRevealAnimator[] revealAnimations;
    private int selectedColour;
    private boolean selectedDotInPosition;
    private float selectedDotX;
    private final Paint selectedPaint;
    private int unselectedColour;
    private final Path unselectedDotLeftPath;
    private final Path unselectedDotPath;
    private final Path unselectedDotRightPath;
    private final Paint unselectedPaint;
    private ViewPager viewPager;

    public DotsPageIndicator(Context context) {
        this(context, null, 0);
    }

    public DotsPageIndicator(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public DotsPageIndicator(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        int i2 = (int) context.getResources().getDisplayMetrics().scaledDensity;
        TypedArray typedArrayObtainStyledAttributes = getContext().obtainStyledAttributes(attributeSet, R.styleable.DotsPageIndicator, i, 0);
        this.dotDiameter = typedArrayObtainStyledAttributes.getDimensionPixelSize(2, 8 * i2);
        this.dotRadius = this.dotDiameter / 2;
        this.halfDotRadius = this.dotRadius / 2.0f;
        this.gap = typedArrayObtainStyledAttributes.getDimensionPixelSize(3, 12 * i2);
        this.animDuration = typedArrayObtainStyledAttributes.getInteger(0, 400);
        this.animHalfDuration = this.animDuration / 2;
        this.unselectedColour = typedArrayObtainStyledAttributes.getColor(4, -2130706433);
        this.selectedColour = typedArrayObtainStyledAttributes.getColor(1, -1);
        typedArrayObtainStyledAttributes.recycle();
        this.unselectedPaint = new Paint(1);
        this.unselectedPaint.setColor(this.unselectedColour);
        this.selectedPaint = new Paint(1);
        this.selectedPaint.setColor(this.selectedColour);
        if (Build.VERSION.SDK_INT >= 21) {
            this.interpolator = AnimationUtils.loadInterpolator(context, android.R.interpolator.fast_out_slow_in);
        } else {
            this.interpolator = AnimationUtils.loadInterpolator(context, android.R.anim.accelerate_decelerate_interpolator);
        }
        this.combinedUnselectedPath = new Path();
        this.unselectedDotPath = new Path();
        this.unselectedDotLeftPath = new Path();
        this.unselectedDotRightPath = new Path();
        this.rectF = new RectF();
        addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View view) {
                DotsPageIndicator.this.attachedState = true;
            }

            @Override
            public void onViewDetachedFromWindow(View view) {
                DotsPageIndicator.this.attachedState = false;
            }
        });
    }

    public void setViewPager(ViewPager viewPager) {
        this.viewPager = viewPager;
        viewPager.setOnPageChangeListener(this);
        setPageCount(viewPager.getAdapter().getCount());
        viewPager.getAdapter().registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                DotsPageIndicator.this.setPageCount(DotsPageIndicator.this.viewPager.getAdapter().getCount());
            }
        });
        setCurrentPageImmediate();
    }

    public void setOnPageChangeListener(ViewPager.OnPageChangeListener onPageChangeListener) {
        this.pageChangeListener = onPageChangeListener;
    }

    @Override
    public void onPageScrolled(int i, float f, int i2) {
        if (this.pageChangeListener != null) {
            this.pageChangeListener.onPageScrolled(i, f, i2);
        }
    }

    @Override
    public void onPageSelected(int i) {
        if (this.attachedState) {
            setSelectedPage(i);
        } else {
            setCurrentPageImmediate();
        }
        if (this.pageChangeListener != null) {
            this.pageChangeListener.onPageSelected(i);
        }
    }

    @Override
    public void onPageScrollStateChanged(int i) {
        if (this.pageChangeListener != null) {
            this.pageChangeListener.onPageScrollStateChanged(i);
        }
    }

    private void setPageCount(int i) {
        this.pageCount = i;
        calculateDotPositions();
        resetState();
    }

    private void calculateDotPositions() {
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        float width = paddingLeft + ((((getWidth() - getPaddingRight()) - paddingLeft) - getRequiredWidth()) / 2) + this.dotRadius;
        this.dotCenterX = new float[this.pageCount];
        for (int i = 0; i < this.pageCount; i++) {
            this.dotCenterX[i] = ((this.dotDiameter + this.gap) * i) + width;
        }
        float f = paddingTop;
        this.dotTopY = f;
        this.dotCenterY = f + this.dotRadius;
        this.dotBottomY = paddingTop + this.dotDiameter;
        setCurrentPageImmediate();
    }

    private void setCurrentPageImmediate() {
        if (this.viewPager != null) {
            this.currentPage = this.viewPager.getCurrentItem();
        } else {
            this.currentPage = 0;
        }
        if (this.pageCount > 0) {
            this.selectedDotX = this.dotCenterX[this.currentPage];
        }
    }

    private void resetState() {
        if (this.pageCount > 0) {
            this.joiningFractions = new float[this.pageCount - 1];
            Arrays.fill(this.joiningFractions, 0.0f);
            this.dotRevealFractions = new float[this.pageCount];
            Arrays.fill(this.dotRevealFractions, 0.0f);
            this.retreatingJoinX1 = -1.0f;
            this.retreatingJoinX2 = -1.0f;
            this.selectedDotInPosition = true;
        }
    }

    @Override
    protected void onMeasure(int i, int i2) {
        int desiredHeight = getDesiredHeight();
        int mode = View.MeasureSpec.getMode(i2);
        if (mode == Integer.MIN_VALUE) {
            desiredHeight = Math.min(desiredHeight, View.MeasureSpec.getSize(i2));
        } else if (mode == 1073741824) {
            desiredHeight = View.MeasureSpec.getSize(i2);
        }
        int desiredWidth = getDesiredWidth();
        int mode2 = View.MeasureSpec.getMode(i);
        if (mode2 == Integer.MIN_VALUE) {
            desiredWidth = Math.min(desiredWidth, View.MeasureSpec.getSize(i));
        } else if (mode2 == 1073741824) {
            desiredWidth = View.MeasureSpec.getSize(i);
        }
        setMeasuredDimension(desiredWidth, desiredHeight);
        calculateDotPositions();
    }

    @Override
    protected void onSizeChanged(int i, int i2, int i3, int i4) {
        setMeasuredDimension(i, i2);
        calculateDotPositions();
    }

    @Override
    public void clearAnimation() {
        super.clearAnimation();
        if (Build.VERSION.SDK_INT >= 16) {
            cancelRunningAnimations();
        }
    }

    private int getDesiredHeight() {
        return getPaddingTop() + this.dotDiameter + getPaddingBottom();
    }

    private int getRequiredWidth() {
        return (this.pageCount * this.dotDiameter) + ((this.pageCount - 1) * this.gap);
    }

    private int getDesiredWidth() {
        return getPaddingLeft() + getRequiredWidth() + getPaddingRight();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (this.viewPager == null || this.pageCount == 0) {
            return;
        }
        drawUnselected(canvas);
        drawSelected(canvas);
    }

    private void drawUnselected(Canvas canvas) {
        int i;
        this.combinedUnselectedPath.rewind();
        int i2 = 0;
        while (i2 < this.pageCount) {
            if (i2 != this.pageCount - 1) {
                i = i2 + 1;
            } else {
                i = i2;
            }
            if (Build.VERSION.SDK_INT >= 21) {
                this.combinedUnselectedPath.op(getUnselectedPath(i2, this.dotCenterX[i2], this.dotCenterX[i], i2 == this.pageCount + (-1) ? -1.0f : this.joiningFractions[i2], this.dotRevealFractions[i2]), Path.Op.UNION);
            } else {
                canvas.drawCircle(this.dotCenterX[i2], this.dotCenterY, this.dotRadius, this.unselectedPaint);
            }
            i2++;
        }
        if (this.retreatingJoinX1 != -1.0f && Build.VERSION.SDK_INT >= 21) {
            this.combinedUnselectedPath.op(getRetreatingJoinPath(), Path.Op.UNION);
        }
        canvas.drawPath(this.combinedUnselectedPath, this.unselectedPaint);
    }

    private Path getUnselectedPath(int i, float f, float f2, float f3, float f4) {
        this.unselectedDotPath.rewind();
        if ((f3 == 0.0f || f3 == -1.0f) && f4 == 0.0f && (i != this.currentPage || !this.selectedDotInPosition)) {
            this.unselectedDotPath.addCircle(this.dotCenterX[i], this.dotCenterY, this.dotRadius, Path.Direction.CW);
        }
        if (f3 > 0.0f && f3 < 0.5f && this.retreatingJoinX1 == -1.0f) {
            this.unselectedDotLeftPath.rewind();
            this.unselectedDotLeftPath.moveTo(f, this.dotBottomY);
            this.rectF.set(f - this.dotRadius, this.dotTopY, this.dotRadius + f, this.dotBottomY);
            this.unselectedDotLeftPath.arcTo(this.rectF, 90.0f, 180.0f, true);
            this.endX1 = this.dotRadius + f + (this.gap * f3);
            this.endY1 = this.dotCenterY;
            this.controlX1 = this.halfDotRadius + f;
            this.controlY1 = this.dotTopY;
            this.controlX2 = this.endX1;
            this.controlY2 = this.endY1 - this.halfDotRadius;
            this.unselectedDotLeftPath.cubicTo(this.controlX1, this.controlY1, this.controlX2, this.controlY2, this.endX1, this.endY1);
            this.endX2 = f;
            this.endY2 = this.dotBottomY;
            this.controlX1 = this.endX1;
            this.controlY1 = this.endY1 + this.halfDotRadius;
            this.controlX2 = this.halfDotRadius + f;
            this.controlY2 = this.dotBottomY;
            this.unselectedDotLeftPath.cubicTo(this.controlX1, this.controlY1, this.controlX2, this.controlY2, this.endX2, this.endY2);
            if (Build.VERSION.SDK_INT >= 21) {
                this.unselectedDotPath.op(this.unselectedDotLeftPath, Path.Op.UNION);
            }
            this.unselectedDotRightPath.rewind();
            this.unselectedDotRightPath.moveTo(f2, this.dotBottomY);
            this.rectF.set(f2 - this.dotRadius, this.dotTopY, this.dotRadius + f2, this.dotBottomY);
            this.unselectedDotRightPath.arcTo(this.rectF, 90.0f, -180.0f, true);
            this.endX1 = (f2 - this.dotRadius) - (this.gap * f3);
            this.endY1 = this.dotCenterY;
            this.controlX1 = f2 - this.halfDotRadius;
            this.controlY1 = this.dotTopY;
            this.controlX2 = this.endX1;
            this.controlY2 = this.endY1 - this.halfDotRadius;
            this.unselectedDotRightPath.cubicTo(this.controlX1, this.controlY1, this.controlX2, this.controlY2, this.endX1, this.endY1);
            this.endX2 = f2;
            this.endY2 = this.dotBottomY;
            this.controlX1 = this.endX1;
            this.controlY1 = this.endY1 + this.halfDotRadius;
            this.controlX2 = this.endX2 - this.halfDotRadius;
            this.controlY2 = this.dotBottomY;
            this.unselectedDotRightPath.cubicTo(this.controlX1, this.controlY1, this.controlX2, this.controlY2, this.endX2, this.endY2);
            if (Build.VERSION.SDK_INT >= 21) {
                this.unselectedDotPath.op(this.unselectedDotRightPath, Path.Op.UNION);
            }
        }
        if (f3 > 0.5f && f3 < 1.0f && this.retreatingJoinX1 == -1.0f) {
            this.unselectedDotPath.moveTo(f, this.dotBottomY);
            this.rectF.set(f - this.dotRadius, this.dotTopY, this.dotRadius + f, this.dotBottomY);
            this.unselectedDotPath.arcTo(this.rectF, 90.0f, 180.0f, true);
            this.endX1 = this.dotRadius + f + (this.gap / 2);
            this.endY1 = this.dotCenterY - (this.dotRadius * f3);
            this.controlX1 = this.endX1 - (this.dotRadius * f3);
            this.controlY1 = this.dotTopY;
            float f5 = 1.0f - f3;
            this.controlX2 = this.endX1 - (this.dotRadius * f5);
            this.controlY2 = this.endY1;
            this.unselectedDotPath.cubicTo(this.controlX1, this.controlY1, this.controlX2, this.controlY2, this.endX1, this.endY1);
            this.endX2 = f2;
            this.endY2 = this.dotTopY;
            this.controlX1 = this.endX1 + (this.dotRadius * f5);
            this.controlY1 = this.endY1;
            this.controlX2 = this.endX1 + (this.dotRadius * f3);
            this.controlY2 = this.dotTopY;
            this.unselectedDotPath.cubicTo(this.controlX1, this.controlY1, this.controlX2, this.controlY2, this.endX2, this.endY2);
            this.rectF.set(f2 - this.dotRadius, this.dotTopY, this.dotRadius + f2, this.dotBottomY);
            this.unselectedDotPath.arcTo(this.rectF, 270.0f, 180.0f, true);
            this.endY1 = this.dotCenterY + (this.dotRadius * f3);
            this.controlX1 = this.endX1 + (this.dotRadius * f3);
            this.controlY1 = this.dotBottomY;
            this.controlX2 = this.endX1 + (this.dotRadius * f5);
            this.controlY2 = this.endY1;
            this.unselectedDotPath.cubicTo(this.controlX1, this.controlY1, this.controlX2, this.controlY2, this.endX1, this.endY1);
            this.endX2 = f;
            this.endY2 = this.dotBottomY;
            this.controlX1 = this.endX1 - (f5 * this.dotRadius);
            this.controlY1 = this.endY1;
            this.controlX2 = this.endX1 - (this.dotRadius * f3);
            this.controlY2 = this.endY2;
            this.unselectedDotPath.cubicTo(this.controlX1, this.controlY1, this.controlX2, this.controlY2, this.endX2, this.endY2);
        }
        if (f3 == 1.0f && this.retreatingJoinX1 == -1.0f) {
            this.rectF.set(f - this.dotRadius, this.dotTopY, f2 + this.dotRadius, this.dotBottomY);
            this.unselectedDotPath.addRoundRect(this.rectF, this.dotRadius, this.dotRadius, Path.Direction.CW);
        }
        if (f4 > 1.0E-5f) {
            this.unselectedDotPath.addCircle(f, this.dotCenterY, this.dotRadius * f4, Path.Direction.CW);
        }
        return this.unselectedDotPath;
    }

    private Path getRetreatingJoinPath() {
        this.unselectedDotPath.rewind();
        this.rectF.set(this.retreatingJoinX1, this.dotTopY, this.retreatingJoinX2, this.dotBottomY);
        this.unselectedDotPath.addRoundRect(this.rectF, this.dotRadius, this.dotRadius, Path.Direction.CW);
        return this.unselectedDotPath;
    }

    private void drawSelected(Canvas canvas) {
        canvas.drawCircle(this.selectedDotX, this.dotCenterY, this.dotRadius, this.selectedPaint);
    }

    private void setSelectedPage(int i) {
        if (i == this.currentPage || this.pageCount == 0) {
            return;
        }
        int i2 = this.currentPage;
        this.currentPage = i;
        if (Build.VERSION.SDK_INT >= 16) {
            cancelRunningAnimations();
            int iAbs = Math.abs(i - i2);
            this.moveAnimation = createMoveSelectedAnimator(this.dotCenterX[i], i2, i, iAbs);
            this.joiningAnimations = new ValueAnimator[iAbs];
            for (int i3 = 0; i3 < iAbs; i3++) {
                this.joiningAnimations[i3] = createJoiningAnimator(i > i2 ? i2 + i3 : (i2 - 1) - i3, ((long) i3) * (this.animDuration / 8));
            }
            this.moveAnimation.start();
            startJoiningAnimations();
            return;
        }
        setCurrentPageImmediate();
        invalidate();
    }

    private ValueAnimator createMoveSelectedAnimator(float f, int i, int i2, int i3) {
        StartPredicate leftwardStartPredicate;
        ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(this.selectedDotX, f);
        if (i2 <= i) {
            leftwardStartPredicate = new LeftwardStartPredicate(f + ((this.selectedDotX - f) * 0.25f));
        } else {
            leftwardStartPredicate = new RightwardStartPredicate(f - ((f - this.selectedDotX) * 0.25f));
        }
        this.retreatAnimation = new PendingRetreatAnimator(i, i2, i3, leftwardStartPredicate);
        valueAnimatorOfFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                DotsPageIndicator.this.selectedDotX = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                DotsPageIndicator.this.retreatAnimation.startIfNecessary(DotsPageIndicator.this.selectedDotX);
                DotsPageIndicator.this.postInvalidateOnAnimation();
            }
        });
        valueAnimatorOfFloat.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animator) {
                DotsPageIndicator.this.selectedDotInPosition = false;
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                DotsPageIndicator.this.selectedDotInPosition = true;
            }
        });
        valueAnimatorOfFloat.setStartDelay(this.selectedDotInPosition ? this.animDuration / 4 : 0L);
        valueAnimatorOfFloat.setDuration((this.animDuration * 3) / 4);
        valueAnimatorOfFloat.setInterpolator(this.interpolator);
        return valueAnimatorOfFloat;
    }

    private ValueAnimator createJoiningAnimator(final int i, long j) {
        ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(0.0f, 1.0f);
        valueAnimatorOfFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                DotsPageIndicator.this.setJoiningFraction(i, valueAnimator.getAnimatedFraction());
            }
        });
        valueAnimatorOfFloat.setDuration(this.animHalfDuration);
        valueAnimatorOfFloat.setStartDelay(j);
        valueAnimatorOfFloat.setInterpolator(this.interpolator);
        return valueAnimatorOfFloat;
    }

    private void setJoiningFraction(int i, float f) {
        this.joiningFractions[i] = f;
        postInvalidateOnAnimation();
    }

    private void clearJoiningFractions() {
        Arrays.fill(this.joiningFractions, 0.0f);
        postInvalidateOnAnimation();
    }

    private void setDotRevealFraction(int i, float f) {
        this.dotRevealFractions[i] = f;
        postInvalidateOnAnimation();
    }

    private void cancelRunningAnimations() {
        cancelMoveAnimation();
        cancelJoiningAnimations();
        cancelRetreatAnimation();
        cancelRevealAnimations();
        resetState();
    }

    private void cancelMoveAnimation() {
        if (this.moveAnimation != null && this.moveAnimation.isRunning()) {
            this.moveAnimation.cancel();
        }
    }

    private void startJoiningAnimations() {
        this.joiningAnimationSet = new AnimatorSet();
        this.joiningAnimationSet.playTogether(this.joiningAnimations);
        this.joiningAnimationSet.start();
    }

    private void cancelJoiningAnimations() {
        if (this.joiningAnimationSet != null && this.joiningAnimationSet.isRunning()) {
            this.joiningAnimationSet.cancel();
        }
    }

    private void cancelRetreatAnimation() {
        if (this.retreatAnimation != null && this.retreatAnimation.isRunning()) {
            this.retreatAnimation.cancel();
        }
    }

    private void cancelRevealAnimations() {
        if (this.revealAnimations != null) {
            for (PendingRevealAnimator pendingRevealAnimator : this.revealAnimations) {
                pendingRevealAnimator.cancel();
            }
        }
    }

    int getUnselectedColour() {
        return this.unselectedColour;
    }

    int getSelectedColour() {
        return this.selectedColour;
    }

    float getDotCenterY() {
        return this.dotCenterY;
    }

    float getSelectedDotX() {
        return this.selectedDotX;
    }

    int getCurrentPage() {
        return this.currentPage;
    }

    public abstract class PendingStartAnimator extends ValueAnimator {
        protected boolean hasStarted = false;
        protected StartPredicate predicate;

        public PendingStartAnimator(StartPredicate startPredicate) {
            this.predicate = startPredicate;
        }

        public void startIfNecessary(float f) {
            if (!this.hasStarted && this.predicate.shouldStart(f)) {
                start();
                this.hasStarted = true;
            }
        }
    }

    public class PendingRetreatAnimator extends PendingStartAnimator {
        public PendingRetreatAnimator(int i, int i2, int i3, StartPredicate startPredicate) {
            super(startPredicate);
            setDuration(DotsPageIndicator.this.animHalfDuration);
            setInterpolator(DotsPageIndicator.this.interpolator);
            final float fMin = i2 > i ? Math.min(DotsPageIndicator.this.dotCenterX[i], DotsPageIndicator.this.selectedDotX) - DotsPageIndicator.this.dotRadius : DotsPageIndicator.this.dotCenterX[i2] - DotsPageIndicator.this.dotRadius;
            float f = i2 > i ? DotsPageIndicator.this.dotCenterX[i2] - DotsPageIndicator.this.dotRadius : DotsPageIndicator.this.dotCenterX[i2] - DotsPageIndicator.this.dotRadius;
            final float fMax = i2 > i ? DotsPageIndicator.this.dotCenterX[i2] + DotsPageIndicator.this.dotRadius : Math.max(DotsPageIndicator.this.dotCenterX[i], DotsPageIndicator.this.selectedDotX) + DotsPageIndicator.this.dotRadius;
            float f2 = i2 > i ? DotsPageIndicator.this.dotCenterX[i2] + DotsPageIndicator.this.dotRadius : DotsPageIndicator.this.dotCenterX[i2] + DotsPageIndicator.this.dotRadius;
            DotsPageIndicator.this.revealAnimations = new PendingRevealAnimator[i3];
            final int[] iArr = new int[i3];
            int i4 = 0;
            if (fMin != f) {
                setFloatValues(new float[]{fMin, f});
                while (i4 < i3) {
                    int i5 = i + i4;
                    DotsPageIndicator.this.revealAnimations[i4] = DotsPageIndicator.this.new PendingRevealAnimator(i5, DotsPageIndicator.this.new RightwardStartPredicate(DotsPageIndicator.this.dotCenterX[i5]));
                    iArr[i4] = i5;
                    i4++;
                }
                addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        DotsPageIndicator.this.retreatingJoinX1 = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                        DotsPageIndicator.this.postInvalidateOnAnimation();
                        for (PendingRevealAnimator pendingRevealAnimator : DotsPageIndicator.this.revealAnimations) {
                            pendingRevealAnimator.startIfNecessary(DotsPageIndicator.this.retreatingJoinX1);
                        }
                    }
                });
            } else {
                setFloatValues(new float[]{fMax, f2});
                while (i4 < i3) {
                    int i6 = i - i4;
                    DotsPageIndicator.this.revealAnimations[i4] = DotsPageIndicator.this.new PendingRevealAnimator(i6, DotsPageIndicator.this.new LeftwardStartPredicate(DotsPageIndicator.this.dotCenterX[i6]));
                    iArr[i4] = i6;
                    i4++;
                }
                addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        DotsPageIndicator.this.retreatingJoinX2 = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                        DotsPageIndicator.this.postInvalidateOnAnimation();
                        for (PendingRevealAnimator pendingRevealAnimator : DotsPageIndicator.this.revealAnimations) {
                            pendingRevealAnimator.startIfNecessary(DotsPageIndicator.this.retreatingJoinX2);
                        }
                    }
                });
            }
            addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animator) {
                    DotsPageIndicator.this.cancelJoiningAnimations();
                    DotsPageIndicator.this.clearJoiningFractions();
                    for (int i7 : iArr) {
                        DotsPageIndicator.this.setDotRevealFraction(i7, 1.0E-5f);
                    }
                    DotsPageIndicator.this.retreatingJoinX1 = fMin;
                    DotsPageIndicator.this.retreatingJoinX2 = fMax;
                    DotsPageIndicator.this.postInvalidateOnAnimation();
                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    DotsPageIndicator.this.retreatingJoinX1 = -1.0f;
                    DotsPageIndicator.this.retreatingJoinX2 = -1.0f;
                    DotsPageIndicator.this.postInvalidateOnAnimation();
                }
            });
        }
    }

    public class PendingRevealAnimator extends PendingStartAnimator {
        private final int dot;

        public PendingRevealAnimator(int i, StartPredicate startPredicate) {
            super(startPredicate);
            this.dot = i;
            setFloatValues(new float[]{1.0E-5f, 1.0f});
            setDuration(DotsPageIndicator.this.animHalfDuration);
            setInterpolator(DotsPageIndicator.this.interpolator);
            addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    DotsPageIndicator.this.setDotRevealFraction(PendingRevealAnimator.this.dot, ((Float) valueAnimator.getAnimatedValue()).floatValue());
                }
            });
            addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    DotsPageIndicator.this.setDotRevealFraction(PendingRevealAnimator.this.dot, 0.0f);
                    DotsPageIndicator.this.postInvalidateOnAnimation();
                }
            });
        }
    }

    public abstract class StartPredicate {
        protected float thresholdValue;

        abstract boolean shouldStart(float f);

        public StartPredicate(float f) {
            this.thresholdValue = f;
        }
    }

    public class RightwardStartPredicate extends StartPredicate {
        public RightwardStartPredicate(float f) {
            super(f);
        }

        @Override
        boolean shouldStart(float f) {
            return f > this.thresholdValue;
        }
    }

    public class LeftwardStartPredicate extends StartPredicate {
        public LeftwardStartPredicate(float f) {
            super(f);
        }

        @Override
        boolean shouldStart(float f) {
            return f < this.thresholdValue;
        }
    }
}
