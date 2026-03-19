package android.widget;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.Shape;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import com.android.internal.util.Preconditions;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.ToDoubleFunction;

final class SmartSelectSprite {
    private static final int CORNER_DURATION = 50;
    private static final int EXPAND_DURATION = 300;
    static final Comparator<RectF> RECTANGLE_COMPARATOR = Comparator.comparingDouble(new ToDoubleFunction() {
        @Override
        public final double applyAsDouble(java.lang.Object r3) {
            throw new UnsupportedOperationException("Method not decompiled: android.widget.$$Lambda$SmartSelectSprite$c8eqlh2kO_X0luLU2BexwK921WA.applyAsDouble(java.lang.Object):double");
        }
    }).thenComparingDouble(new ToDoubleFunction() {
        @Override
        public final double applyAsDouble(java.lang.Object r3) {
            throw new UnsupportedOperationException("Method not decompiled: android.widget.$$Lambda$SmartSelectSprite$mdkXIT1_UNlJQMaziE_E815aIKE.applyAsDouble(java.lang.Object):double");
        }
    });
    private final Interpolator mCornerInterpolator;
    private final Interpolator mExpandInterpolator;
    private final int mFillColor;
    private final Runnable mInvalidator;
    private Animator mActiveAnimator = null;
    private Drawable mExistingDrawable = null;
    private RectangleList mExistingRectangleList = null;

    static final class RectangleWithTextSelectionLayout {
        private final RectF mRectangle;
        private final int mTextSelectionLayout;

        RectangleWithTextSelectionLayout(RectF rectF, int i) {
            this.mRectangle = (RectF) Preconditions.checkNotNull(rectF);
            this.mTextSelectionLayout = i;
        }

        public RectF getRectangle() {
            return this.mRectangle;
        }

        public int getTextSelectionLayout() {
            return this.mTextSelectionLayout;
        }
    }

    private static final class RoundedRectangleShape extends Shape {
        private static final String PROPERTY_ROUND_RATIO = "roundRatio";
        private final RectF mBoundingRectangle;
        private final float mBoundingWidth;
        private final Path mClipPath;
        private final RectF mDrawRect;
        private final int mExpansionDirection;
        private final boolean mInverted;
        private float mLeftBoundary;
        private float mRightBoundary;
        private float mRoundRatio;

        @Retention(RetentionPolicy.SOURCE)
        private @interface ExpansionDirection {
            public static final int CENTER = 0;
            public static final int LEFT = -1;
            public static final int RIGHT = 1;
        }

        private static int invert(int i) {
            return i * (-1);
        }

        private RoundedRectangleShape(RectF rectF, int i, boolean z) {
            this.mRoundRatio = 1.0f;
            this.mDrawRect = new RectF();
            this.mClipPath = new Path();
            this.mLeftBoundary = 0.0f;
            this.mRightBoundary = 0.0f;
            this.mBoundingRectangle = new RectF(rectF);
            this.mBoundingWidth = rectF.width();
            this.mInverted = z && i != 0;
            if (z) {
                this.mExpansionDirection = invert(i);
            } else {
                this.mExpansionDirection = i;
            }
            if (rectF.height() > rectF.width()) {
                setRoundRatio(0.0f);
            } else {
                setRoundRatio(1.0f);
            }
        }

        @Override
        public void draw(Canvas canvas, Paint paint) {
            if (this.mLeftBoundary == this.mRightBoundary) {
                return;
            }
            float cornerRadius = getCornerRadius();
            float adjustedCornerRadius = getAdjustedCornerRadius();
            this.mDrawRect.set(this.mBoundingRectangle);
            float f = cornerRadius / 2.0f;
            this.mDrawRect.left = (this.mBoundingRectangle.left + this.mLeftBoundary) - f;
            this.mDrawRect.right = this.mBoundingRectangle.left + this.mRightBoundary + f;
            canvas.save();
            this.mClipPath.reset();
            this.mClipPath.addRoundRect(this.mDrawRect, adjustedCornerRadius, adjustedCornerRadius, Path.Direction.CW);
            canvas.clipPath(this.mClipPath);
            canvas.drawRect(this.mBoundingRectangle, paint);
            canvas.restore();
        }

        void setRoundRatio(float f) {
            this.mRoundRatio = f;
        }

        float getRoundRatio() {
            return this.mRoundRatio;
        }

        private void setStartBoundary(float f) {
            if (this.mInverted) {
                this.mRightBoundary = this.mBoundingWidth - f;
            } else {
                this.mLeftBoundary = f;
            }
        }

        private void setEndBoundary(float f) {
            if (this.mInverted) {
                this.mLeftBoundary = this.mBoundingWidth - f;
            } else {
                this.mRightBoundary = f;
            }
        }

        private float getCornerRadius() {
            return Math.min(this.mBoundingRectangle.width(), this.mBoundingRectangle.height());
        }

        private float getAdjustedCornerRadius() {
            return getCornerRadius() * this.mRoundRatio;
        }

        private float getBoundingWidth() {
            return (int) (this.mBoundingRectangle.width() + getCornerRadius());
        }
    }

    private static final class RectangleList extends Shape {
        private static final String PROPERTY_LEFT_BOUNDARY = "leftBoundary";
        private static final String PROPERTY_RIGHT_BOUNDARY = "rightBoundary";
        private int mDisplayType;
        private final Path mOutlinePolygonPath;
        private final List<RoundedRectangleShape> mRectangles;
        private final List<RoundedRectangleShape> mReversedRectangles;

        @Retention(RetentionPolicy.SOURCE)
        private @interface DisplayType {
            public static final int POLYGON = 1;
            public static final int RECTANGLES = 0;
        }

        private RectangleList(List<RoundedRectangleShape> list) {
            this.mDisplayType = 0;
            this.mRectangles = new ArrayList(list);
            this.mReversedRectangles = new ArrayList(list);
            Collections.reverse(this.mReversedRectangles);
            this.mOutlinePolygonPath = generateOutlinePolygonPath(list);
        }

        private void setLeftBoundary(float f) {
            float totalWidth = getTotalWidth();
            for (RoundedRectangleShape roundedRectangleShape : this.mReversedRectangles) {
                float boundingWidth = totalWidth - roundedRectangleShape.getBoundingWidth();
                if (f < boundingWidth) {
                    roundedRectangleShape.setStartBoundary(0.0f);
                } else if (f > totalWidth) {
                    roundedRectangleShape.setStartBoundary(roundedRectangleShape.getBoundingWidth());
                } else {
                    roundedRectangleShape.setStartBoundary((roundedRectangleShape.getBoundingWidth() - totalWidth) + f);
                }
                totalWidth = boundingWidth;
            }
        }

        private void setRightBoundary(float f) {
            float f2 = 0.0f;
            for (RoundedRectangleShape roundedRectangleShape : this.mRectangles) {
                float boundingWidth = roundedRectangleShape.getBoundingWidth() + f2;
                if (boundingWidth < f) {
                    roundedRectangleShape.setEndBoundary(roundedRectangleShape.getBoundingWidth());
                } else if (f2 > f) {
                    roundedRectangleShape.setEndBoundary(0.0f);
                } else {
                    roundedRectangleShape.setEndBoundary(f - f2);
                }
                f2 = boundingWidth;
            }
        }

        void setDisplayType(int i) {
            this.mDisplayType = i;
        }

        private int getTotalWidth() {
            Iterator<RoundedRectangleShape> it = this.mRectangles.iterator();
            int boundingWidth = 0;
            while (it.hasNext()) {
                boundingWidth = (int) (boundingWidth + it.next().getBoundingWidth());
            }
            return boundingWidth;
        }

        @Override
        public void draw(Canvas canvas, Paint paint) {
            if (this.mDisplayType == 1) {
                drawPolygon(canvas, paint);
            } else {
                drawRectangles(canvas, paint);
            }
        }

        private void drawRectangles(Canvas canvas, Paint paint) {
            Iterator<RoundedRectangleShape> it = this.mRectangles.iterator();
            while (it.hasNext()) {
                it.next().draw(canvas, paint);
            }
        }

        private void drawPolygon(Canvas canvas, Paint paint) {
            canvas.drawPath(this.mOutlinePolygonPath, paint);
        }

        private static Path generateOutlinePolygonPath(List<RoundedRectangleShape> list) {
            Path path = new Path();
            for (RoundedRectangleShape roundedRectangleShape : list) {
                Path path2 = new Path();
                path2.addRect(roundedRectangleShape.mBoundingRectangle, Path.Direction.CW);
                path.op(path2, Path.Op.UNION);
            }
            return path;
        }
    }

    SmartSelectSprite(Context context, int i, Runnable runnable) {
        this.mExpandInterpolator = AnimationUtils.loadInterpolator(context, 17563661);
        this.mCornerInterpolator = AnimationUtils.loadInterpolator(context, 17563663);
        this.mFillColor = i;
        this.mInvalidator = (Runnable) Preconditions.checkNotNull(runnable);
    }

    public void startAnimation(PointF pointF, List<RectangleWithTextSelectionLayout> list, Runnable runnable) {
        RectangleWithTextSelectionLayout next;
        cancelAnimation();
        ValueAnimator.AnimatorUpdateListener animatorUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                this.f$0.mInvalidator.run();
            }
        };
        int size = list.size();
        ArrayList arrayList = new ArrayList(size);
        ArrayList arrayList2 = new ArrayList(size);
        Iterator<RectangleWithTextSelectionLayout> it = list.iterator();
        int iWidth = 0;
        while (true) {
            if (it.hasNext()) {
                next = it.next();
                RectF rectangle = next.getRectangle();
                if (contains(rectangle, pointF)) {
                    break;
                } else {
                    iWidth = (int) (iWidth + rectangle.width());
                }
            } else {
                next = null;
                break;
            }
        }
        if (next == null) {
            throw new IllegalArgumentException("Center point is not inside any of the rectangles!");
        }
        int i = (int) (iWidth + (pointF.x - next.getRectangle().left));
        int[] iArrGenerateDirections = generateDirections(next, list);
        for (int i2 = 0; i2 < size; i2++) {
            RectangleWithTextSelectionLayout rectangleWithTextSelectionLayout = list.get(i2);
            RoundedRectangleShape roundedRectangleShape = new RoundedRectangleShape(rectangleWithTextSelectionLayout.getRectangle(), iArrGenerateDirections[i2], rectangleWithTextSelectionLayout.getTextSelectionLayout() == 0);
            arrayList2.add(createCornerAnimator(roundedRectangleShape, animatorUpdateListener));
            arrayList.add(roundedRectangleShape);
        }
        RectangleList rectangleList = new RectangleList(arrayList);
        ShapeDrawable shapeDrawable = new ShapeDrawable(rectangleList);
        Paint paint = shapeDrawable.getPaint();
        paint.setColor(this.mFillColor);
        paint.setStyle(Paint.Style.FILL);
        this.mExistingRectangleList = rectangleList;
        this.mExistingDrawable = shapeDrawable;
        float f = i;
        this.mActiveAnimator = createAnimator(rectangleList, f, f, arrayList2, animatorUpdateListener, runnable);
        this.mActiveAnimator.start();
    }

    public boolean isAnimationActive() {
        return this.mActiveAnimator != null && this.mActiveAnimator.isRunning();
    }

    private Animator createAnimator(RectangleList rectangleList, float f, float f2, List<Animator> list, ValueAnimator.AnimatorUpdateListener animatorUpdateListener, Runnable runnable) {
        ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(rectangleList, "rightBoundary", f2, rectangleList.getTotalWidth());
        ObjectAnimator objectAnimatorOfFloat2 = ObjectAnimator.ofFloat(rectangleList, "leftBoundary", f, 0.0f);
        objectAnimatorOfFloat.setDuration(300L);
        objectAnimatorOfFloat2.setDuration(300L);
        objectAnimatorOfFloat.addUpdateListener(animatorUpdateListener);
        objectAnimatorOfFloat2.addUpdateListener(animatorUpdateListener);
        objectAnimatorOfFloat.setInterpolator(this.mExpandInterpolator);
        objectAnimatorOfFloat2.setInterpolator(this.mExpandInterpolator);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(list);
        AnimatorSet animatorSet2 = new AnimatorSet();
        animatorSet2.playTogether(objectAnimatorOfFloat2, objectAnimatorOfFloat);
        AnimatorSet animatorSet3 = new AnimatorSet();
        animatorSet3.playSequentially(animatorSet2, animatorSet);
        setUpAnimatorListener(animatorSet3, runnable);
        return animatorSet3;
    }

    private void setUpAnimatorListener(Animator animator, final Runnable runnable) {
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator2) {
            }

            @Override
            public void onAnimationEnd(Animator animator2) {
                SmartSelectSprite.this.mExistingRectangleList.setDisplayType(1);
                SmartSelectSprite.this.mInvalidator.run();
                runnable.run();
            }

            @Override
            public void onAnimationCancel(Animator animator2) {
            }

            @Override
            public void onAnimationRepeat(Animator animator2) {
            }
        });
    }

    private ObjectAnimator createCornerAnimator(RoundedRectangleShape roundedRectangleShape, ValueAnimator.AnimatorUpdateListener animatorUpdateListener) {
        ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(roundedRectangleShape, "roundRatio", roundedRectangleShape.getRoundRatio(), 0.0f);
        objectAnimatorOfFloat.setDuration(50L);
        objectAnimatorOfFloat.addUpdateListener(animatorUpdateListener);
        objectAnimatorOfFloat.setInterpolator(this.mCornerInterpolator);
        return objectAnimatorOfFloat;
    }

    private static int[] generateDirections(RectangleWithTextSelectionLayout rectangleWithTextSelectionLayout, List<RectangleWithTextSelectionLayout> list) {
        int[] iArr = new int[list.size()];
        int iIndexOf = list.indexOf(rectangleWithTextSelectionLayout);
        for (int i = 0; i < iIndexOf - 1; i++) {
            iArr[i] = -1;
        }
        if (list.size() == 1) {
            iArr[iIndexOf] = 0;
        } else if (iIndexOf == 0) {
            iArr[iIndexOf] = -1;
        } else if (iIndexOf == list.size() - 1) {
            iArr[iIndexOf] = 1;
        } else {
            iArr[iIndexOf] = 0;
        }
        for (int i2 = iIndexOf + 1; i2 < iArr.length; i2++) {
            iArr[i2] = 1;
        }
        return iArr;
    }

    private static boolean contains(RectF rectF, PointF pointF) {
        float f = pointF.x;
        float f2 = pointF.y;
        return f >= rectF.left && f <= rectF.right && f2 >= rectF.top && f2 <= rectF.bottom;
    }

    private void removeExistingDrawables() {
        this.mExistingDrawable = null;
        this.mExistingRectangleList = null;
        this.mInvalidator.run();
    }

    public void cancelAnimation() {
        if (this.mActiveAnimator != null) {
            this.mActiveAnimator.cancel();
            this.mActiveAnimator = null;
            removeExistingDrawables();
        }
    }

    public void draw(Canvas canvas) {
        if (this.mExistingDrawable != null) {
            this.mExistingDrawable.draw(canvas);
        }
    }
}
