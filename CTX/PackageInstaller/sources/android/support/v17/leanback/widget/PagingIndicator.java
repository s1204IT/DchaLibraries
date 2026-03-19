package android.support.v17.leanback.widget;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.support.v17.leanback.R;
import android.util.AttributeSet;
import android.util.Property;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

public class PagingIndicator extends View {
    private static final TimeInterpolator DECELERATE_INTERPOLATOR = new DecelerateInterpolator();
    private static final Property<Dot, Float> DOT_ALPHA = new Property<Dot, Float>(Float.class, "alpha") {
        @Override
        public Float get(Dot dot) {
            return Float.valueOf(dot.getAlpha());
        }

        @Override
        public void set(Dot dot, Float value) {
            dot.setAlpha(value.floatValue());
        }
    };
    private static final Property<Dot, Float> DOT_DIAMETER = new Property<Dot, Float>(Float.class, "diameter") {
        @Override
        public Float get(Dot dot) {
            return Float.valueOf(dot.getDiameter());
        }

        @Override
        public void set(Dot dot, Float value) {
            dot.setDiameter(value.floatValue());
        }
    };
    private static final Property<Dot, Float> DOT_TRANSLATION_X = new Property<Dot, Float>(Float.class, "translation_x") {
        @Override
        public Float get(Dot dot) {
            return Float.valueOf(dot.getTranslationX());
        }

        @Override
        public void set(Dot dot, Float value) {
            dot.setTranslationX(value.floatValue());
        }
    };
    private final AnimatorSet mAnimator;
    Bitmap mArrow;
    final int mArrowDiameter;
    private final int mArrowGap;
    Paint mArrowPaint;
    final int mArrowRadius;
    final Rect mArrowRect;
    final float mArrowToBgRatio;
    final Paint mBgPaint;
    private int mCurrentPage;
    int mDotCenterY;
    final int mDotDiameter;
    int mDotFgSelectColor;
    private final int mDotGap;
    final int mDotRadius;
    private int[] mDotSelectedNextX;
    private int[] mDotSelectedPrevX;
    private int[] mDotSelectedX;
    private Dot[] mDots;
    final Paint mFgPaint;
    private final AnimatorSet mHideAnimator;
    boolean mIsLtr;
    private int mPageCount;
    private int mPreviousPage;
    private final int mShadowRadius;
    private final AnimatorSet mShowAnimator;

    public PagingIndicator(Context context) {
        this(context, null, 0);
    }

    public PagingIndicator(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PagingIndicator(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mAnimator = new AnimatorSet();
        Resources res = getResources();
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.PagingIndicator, defStyle, 0);
        this.mDotRadius = getDimensionFromTypedArray(typedArray, R.styleable.PagingIndicator_lbDotRadius, R.dimen.lb_page_indicator_dot_radius);
        this.mDotDiameter = this.mDotRadius * 2;
        this.mArrowRadius = getDimensionFromTypedArray(typedArray, R.styleable.PagingIndicator_arrowRadius, R.dimen.lb_page_indicator_arrow_radius);
        this.mArrowDiameter = this.mArrowRadius * 2;
        this.mDotGap = getDimensionFromTypedArray(typedArray, R.styleable.PagingIndicator_dotToDotGap, R.dimen.lb_page_indicator_dot_gap);
        this.mArrowGap = getDimensionFromTypedArray(typedArray, R.styleable.PagingIndicator_dotToArrowGap, R.dimen.lb_page_indicator_arrow_gap);
        int dotBgColor = getColorFromTypedArray(typedArray, R.styleable.PagingIndicator_dotBgColor, R.color.lb_page_indicator_dot);
        this.mBgPaint = new Paint(1);
        this.mBgPaint.setColor(dotBgColor);
        this.mDotFgSelectColor = getColorFromTypedArray(typedArray, R.styleable.PagingIndicator_arrowBgColor, R.color.lb_page_indicator_arrow_background);
        if (this.mArrowPaint == null && typedArray.hasValue(R.styleable.PagingIndicator_arrowColor)) {
            setArrowColor(typedArray.getColor(R.styleable.PagingIndicator_arrowColor, 0));
        }
        typedArray.recycle();
        this.mIsLtr = res.getConfiguration().getLayoutDirection() == 0;
        int shadowColor = res.getColor(R.color.lb_page_indicator_arrow_shadow);
        this.mShadowRadius = res.getDimensionPixelSize(R.dimen.lb_page_indicator_arrow_shadow_radius);
        this.mFgPaint = new Paint(1);
        int shadowOffset = res.getDimensionPixelSize(R.dimen.lb_page_indicator_arrow_shadow_offset);
        this.mFgPaint.setShadowLayer(this.mShadowRadius, shadowOffset, shadowOffset, shadowColor);
        this.mArrow = loadArrow();
        this.mArrowRect = new Rect(0, 0, this.mArrow.getWidth(), this.mArrow.getHeight());
        this.mArrowToBgRatio = this.mArrow.getWidth() / this.mArrowDiameter;
        this.mShowAnimator = new AnimatorSet();
        this.mShowAnimator.playTogether(createDotAlphaAnimator(0.0f, 1.0f), createDotDiameterAnimator(this.mDotRadius * 2, this.mArrowRadius * 2), createDotTranslationXAnimator());
        this.mHideAnimator = new AnimatorSet();
        this.mHideAnimator.playTogether(createDotAlphaAnimator(1.0f, 0.0f), createDotDiameterAnimator(this.mArrowRadius * 2, this.mDotRadius * 2), createDotTranslationXAnimator());
        this.mAnimator.playTogether(this.mShowAnimator, this.mHideAnimator);
        setLayerType(1, null);
    }

    private int getDimensionFromTypedArray(TypedArray typedArray, int attr, int defaultId) {
        return typedArray.getDimensionPixelOffset(attr, getResources().getDimensionPixelOffset(defaultId));
    }

    private int getColorFromTypedArray(TypedArray typedArray, int attr, int defaultId) {
        return typedArray.getColor(attr, getResources().getColor(defaultId));
    }

    private Bitmap loadArrow() {
        Bitmap arrow = BitmapFactory.decodeResource(getResources(), R.drawable.lb_ic_nav_arrow);
        if (this.mIsLtr) {
            return arrow;
        }
        Matrix matrix = new Matrix();
        matrix.preScale(-1.0f, 1.0f);
        return Bitmap.createBitmap(arrow, 0, 0, arrow.getWidth(), arrow.getHeight(), matrix, false);
    }

    public void setArrowColor(int color) {
        if (this.mArrowPaint == null) {
            this.mArrowPaint = new Paint();
        }
        this.mArrowPaint.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
    }

    private Animator createDotAlphaAnimator(float from, float to) {
        ObjectAnimator animator = ObjectAnimator.ofFloat((Object) null, DOT_ALPHA, from, to);
        animator.setDuration(167L);
        animator.setInterpolator(DECELERATE_INTERPOLATOR);
        return animator;
    }

    private Animator createDotDiameterAnimator(float from, float to) {
        ObjectAnimator animator = ObjectAnimator.ofFloat((Object) null, DOT_DIAMETER, from, to);
        animator.setDuration(417L);
        animator.setInterpolator(DECELERATE_INTERPOLATOR);
        return animator;
    }

    private Animator createDotTranslationXAnimator() {
        ObjectAnimator animator = ObjectAnimator.ofFloat((Object) null, DOT_TRANSLATION_X, (-this.mArrowGap) + this.mDotGap, 0.0f);
        animator.setDuration(417L);
        animator.setInterpolator(DECELERATE_INTERPOLATOR);
        return animator;
    }

    private void calculateDotPositions() {
        int left = getPaddingLeft();
        int top = getPaddingTop();
        int right = getWidth() - getPaddingRight();
        int requiredWidth = getRequiredWidth();
        int mid = (left + right) / 2;
        this.mDotSelectedX = new int[this.mPageCount];
        this.mDotSelectedPrevX = new int[this.mPageCount];
        this.mDotSelectedNextX = new int[this.mPageCount];
        int i = 1;
        if (this.mIsLtr) {
            int startLeft = mid - (requiredWidth / 2);
            this.mDotSelectedX[0] = ((this.mDotRadius + startLeft) - this.mDotGap) + this.mArrowGap;
            this.mDotSelectedPrevX[0] = this.mDotRadius + startLeft;
            this.mDotSelectedNextX[0] = ((this.mDotRadius + startLeft) - (this.mDotGap * 2)) + (2 * this.mArrowGap);
            while (true) {
                int i2 = i;
                if (i2 >= this.mPageCount) {
                    break;
                }
                this.mDotSelectedX[i2] = this.mDotSelectedPrevX[i2 - 1] + this.mArrowGap;
                this.mDotSelectedPrevX[i2] = this.mDotSelectedPrevX[i2 - 1] + this.mDotGap;
                this.mDotSelectedNextX[i2] = this.mDotSelectedX[i2 - 1] + this.mArrowGap;
                i = i2 + 1;
            }
        } else {
            int startRight = (requiredWidth / 2) + mid;
            this.mDotSelectedX[0] = ((startRight - this.mDotRadius) + this.mDotGap) - this.mArrowGap;
            this.mDotSelectedPrevX[0] = startRight - this.mDotRadius;
            this.mDotSelectedNextX[0] = ((startRight - this.mDotRadius) + (this.mDotGap * 2)) - (2 * this.mArrowGap);
            while (true) {
                int i3 = i;
                if (i3 >= this.mPageCount) {
                    break;
                }
                this.mDotSelectedX[i3] = this.mDotSelectedPrevX[i3 - 1] - this.mArrowGap;
                this.mDotSelectedPrevX[i3] = this.mDotSelectedPrevX[i3 - 1] - this.mDotGap;
                this.mDotSelectedNextX[i3] = this.mDotSelectedX[i3 - 1] - this.mArrowGap;
                i = i3 + 1;
            }
        }
        this.mDotCenterY = this.mArrowRadius + top;
        adjustDotPosition();
    }

    int getPageCount() {
        return this.mPageCount;
    }

    int[] getDotSelectedX() {
        return this.mDotSelectedX;
    }

    int[] getDotSelectedLeftX() {
        return this.mDotSelectedPrevX;
    }

    int[] getDotSelectedRightX() {
        return this.mDotSelectedNextX;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height;
        int width;
        int desiredHeight = getDesiredHeight();
        int mode = View.MeasureSpec.getMode(heightMeasureSpec);
        if (mode == Integer.MIN_VALUE) {
            int height2 = View.MeasureSpec.getSize(heightMeasureSpec);
            height = Math.min(desiredHeight, height2);
        } else if (mode == 1073741824) {
            height = View.MeasureSpec.getSize(heightMeasureSpec);
        } else {
            height = desiredHeight;
        }
        int desiredWidth = getDesiredWidth();
        int mode2 = View.MeasureSpec.getMode(widthMeasureSpec);
        if (mode2 == Integer.MIN_VALUE) {
            int width2 = View.MeasureSpec.getSize(widthMeasureSpec);
            width = Math.min(desiredWidth, width2);
        } else if (mode2 == 1073741824) {
            width = View.MeasureSpec.getSize(widthMeasureSpec);
        } else {
            width = desiredWidth;
        }
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        setMeasuredDimension(width, height);
        calculateDotPositions();
    }

    private int getDesiredHeight() {
        return getPaddingTop() + this.mArrowDiameter + getPaddingBottom() + this.mShadowRadius;
    }

    private int getRequiredWidth() {
        return (this.mDotRadius * 2) + (2 * this.mArrowGap) + ((this.mPageCount - 3) * this.mDotGap);
    }

    private int getDesiredWidth() {
        return getPaddingLeft() + getRequiredWidth() + getPaddingRight();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        for (int i = 0; i < this.mPageCount; i++) {
            this.mDots[i].draw(canvas);
        }
    }

    private void adjustDotPosition() {
        int i = 0;
        while (true) {
            if (i >= this.mCurrentPage) {
                break;
            }
            this.mDots[i].deselect();
            Dot dot = this.mDots[i];
            if (i != this.mPreviousPage) {
                f = 1.0f;
            }
            dot.mDirection = f;
            this.mDots[i].mCenterX = this.mDotSelectedPrevX[i];
            i++;
        }
        this.mDots[this.mCurrentPage].select();
        this.mDots[this.mCurrentPage].mDirection = this.mPreviousPage >= this.mCurrentPage ? 1.0f : -1.0f;
        this.mDots[this.mCurrentPage].mCenterX = this.mDotSelectedX[this.mCurrentPage];
        int i2 = this.mCurrentPage;
        while (true) {
            i2++;
            if (i2 < this.mPageCount) {
                this.mDots[i2].deselect();
                this.mDots[i2].mDirection = 1.0f;
                this.mDots[i2].mCenterX = this.mDotSelectedNextX[i2];
            } else {
                return;
            }
        }
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);
        boolean isLtr = layoutDirection == 0;
        if (this.mIsLtr != isLtr) {
            this.mIsLtr = isLtr;
            this.mArrow = loadArrow();
            if (this.mDots != null) {
                for (Dot dot : this.mDots) {
                    dot.onRtlPropertiesChanged();
                }
            }
            calculateDotPositions();
            invalidate();
        }
    }

    public class Dot {
        float mAlpha;
        float mArrowImageRadius;
        float mCenterX;
        float mDiameter;
        float mDirection;
        int mFgColor;
        float mLayoutDirection;
        float mRadius;
        float mTranslationX;
        final PagingIndicator this$0;

        void select() {
            this.mTranslationX = 0.0f;
            this.mCenterX = 0.0f;
            this.mDiameter = this.this$0.mArrowDiameter;
            this.mRadius = this.this$0.mArrowRadius;
            this.mArrowImageRadius = this.mRadius * this.this$0.mArrowToBgRatio;
            this.mAlpha = 1.0f;
            adjustAlpha();
        }

        void deselect() {
            this.mTranslationX = 0.0f;
            this.mCenterX = 0.0f;
            this.mDiameter = this.this$0.mDotDiameter;
            this.mRadius = this.this$0.mDotRadius;
            this.mArrowImageRadius = this.mRadius * this.this$0.mArrowToBgRatio;
            this.mAlpha = 0.0f;
            adjustAlpha();
        }

        public void adjustAlpha() {
            int alpha = Math.round(255.0f * this.mAlpha);
            int red = Color.red(this.this$0.mDotFgSelectColor);
            int green = Color.green(this.this$0.mDotFgSelectColor);
            int blue = Color.blue(this.this$0.mDotFgSelectColor);
            this.mFgColor = Color.argb(alpha, red, green, blue);
        }

        public float getAlpha() {
            return this.mAlpha;
        }

        public void setAlpha(float alpha) {
            this.mAlpha = alpha;
            adjustAlpha();
            this.this$0.invalidate();
        }

        public float getTranslationX() {
            return this.mTranslationX;
        }

        public void setTranslationX(float translationX) {
            this.mTranslationX = this.mDirection * translationX * this.mLayoutDirection;
            this.this$0.invalidate();
        }

        public float getDiameter() {
            return this.mDiameter;
        }

        public void setDiameter(float diameter) {
            this.mDiameter = diameter;
            this.mRadius = diameter / 2.0f;
            this.mArrowImageRadius = (diameter / 2.0f) * this.this$0.mArrowToBgRatio;
            this.this$0.invalidate();
        }

        void draw(Canvas canvas) {
            float centerX = this.mCenterX + this.mTranslationX;
            canvas.drawCircle(centerX, this.this$0.mDotCenterY, this.mRadius, this.this$0.mBgPaint);
            if (this.mAlpha > 0.0f) {
                this.this$0.mFgPaint.setColor(this.mFgColor);
                canvas.drawCircle(centerX, this.this$0.mDotCenterY, this.mRadius, this.this$0.mFgPaint);
                canvas.drawBitmap(this.this$0.mArrow, this.this$0.mArrowRect, new Rect((int) (centerX - this.mArrowImageRadius), (int) (this.this$0.mDotCenterY - this.mArrowImageRadius), (int) (this.mArrowImageRadius + centerX), (int) (this.this$0.mDotCenterY + this.mArrowImageRadius)), this.this$0.mArrowPaint);
            }
        }

        void onRtlPropertiesChanged() {
            this.mLayoutDirection = this.this$0.mIsLtr ? 1.0f : -1.0f;
        }
    }
}
