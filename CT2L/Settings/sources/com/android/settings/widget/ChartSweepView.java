package com.android.settings.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.DynamicLayout;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.MathUtils;
import android.view.MotionEvent;
import android.view.View;
import com.android.internal.util.Preconditions;
import com.android.settings.R;

public class ChartSweepView extends View {
    private ChartAxis mAxis;
    private View.OnClickListener mClickListener;
    private Rect mContentOffset;
    private long mDragInterval;
    private int mFollowAxis;
    private int mLabelColor;
    private DynamicLayout mLabelLayout;
    private int mLabelMinSize;
    private float mLabelOffset;
    private float mLabelSize;
    private SpannableStringBuilder mLabelTemplate;
    private int mLabelTemplateRes;
    private long mLabelValue;
    private OnSweepListener mListener;
    private Rect mMargins;
    private float mNeighborMargin;
    private ChartSweepView[] mNeighbors;
    private Paint mOutlinePaint;
    private int mSafeRegion;
    private Drawable mSweep;
    private Point mSweepOffset;
    private Rect mSweepPadding;
    private int mTouchMode;
    private MotionEvent mTracking;
    private float mTrackingStart;
    private long mValidAfter;
    private ChartSweepView mValidAfterDynamic;
    private long mValidBefore;
    private ChartSweepView mValidBeforeDynamic;
    private long mValue;

    public interface OnSweepListener {
        void onSweep(ChartSweepView chartSweepView, boolean z);

        void requestEdit(ChartSweepView chartSweepView);
    }

    public ChartSweepView(Context context) {
        this(context, null);
    }

    public ChartSweepView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ChartSweepView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mSweepPadding = new Rect();
        this.mContentOffset = new Rect();
        this.mSweepOffset = new Point();
        this.mMargins = new Rect();
        this.mOutlinePaint = new Paint();
        this.mTouchMode = 0;
        this.mDragInterval = 1L;
        this.mNeighbors = new ChartSweepView[0];
        this.mClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ChartSweepView.this.dispatchRequestEdit();
            }
        };
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ChartSweepView, defStyle, 0);
        setSweepDrawable(a.getDrawable(0));
        setFollowAxis(a.getInt(1, -1));
        setNeighborMargin(a.getDimensionPixelSize(2, 0));
        setSafeRegion(a.getDimensionPixelSize(6, 0));
        setLabelMinSize(a.getDimensionPixelSize(3, 0));
        setLabelTemplate(a.getResourceId(4, 0));
        setLabelColor(a.getColor(5, -16776961));
        setBackgroundResource(R.drawable.data_usage_sweep_background);
        this.mOutlinePaint.setColor(-65536);
        this.mOutlinePaint.setStrokeWidth(1.0f);
        this.mOutlinePaint.setStyle(Paint.Style.STROKE);
        a.recycle();
        setClickable(true);
        setFocusable(true);
        setOnClickListener(this.mClickListener);
        setWillNotDraw(false);
    }

    void init(ChartAxis axis) {
        this.mAxis = (ChartAxis) Preconditions.checkNotNull(axis, "missing axis");
    }

    public void setNeighbors(ChartSweepView... neighbors) {
        this.mNeighbors = neighbors;
    }

    public int getFollowAxis() {
        return this.mFollowAxis;
    }

    public Rect getMargins() {
        return this.mMargins;
    }

    public void setDragInterval(long dragInterval) {
        this.mDragInterval = dragInterval;
    }

    private float getTargetInset() {
        if (this.mFollowAxis == 1) {
            float targetHeight = (this.mSweep.getIntrinsicHeight() - this.mSweepPadding.top) - this.mSweepPadding.bottom;
            return this.mSweepPadding.top + (targetHeight / 2.0f) + this.mSweepOffset.y;
        }
        float targetWidth = (this.mSweep.getIntrinsicWidth() - this.mSweepPadding.left) - this.mSweepPadding.right;
        return this.mSweepPadding.left + (targetWidth / 2.0f) + this.mSweepOffset.x;
    }

    public void addOnSweepListener(OnSweepListener listener) {
        this.mListener = listener;
    }

    private void dispatchOnSweep(boolean sweepDone) {
        if (this.mListener != null) {
            this.mListener.onSweep(this, sweepDone);
        }
    }

    private void dispatchRequestEdit() {
        if (this.mListener != null) {
            this.mListener.requestEdit(this);
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setFocusable(enabled);
        requestLayout();
    }

    public void setSweepDrawable(Drawable sweep) {
        if (this.mSweep != null) {
            this.mSweep.setCallback(null);
            unscheduleDrawable(this.mSweep);
        }
        if (sweep != null) {
            sweep.setCallback(this);
            if (sweep.isStateful()) {
                sweep.setState(getDrawableState());
            }
            sweep.setVisible(getVisibility() == 0, false);
            this.mSweep = sweep;
            sweep.getPadding(this.mSweepPadding);
        } else {
            this.mSweep = null;
        }
        invalidate();
    }

    public void setFollowAxis(int followAxis) {
        this.mFollowAxis = followAxis;
    }

    public void setLabelMinSize(int minSize) {
        this.mLabelMinSize = minSize;
        invalidateLabelTemplate();
    }

    public void setLabelTemplate(int resId) {
        this.mLabelTemplateRes = resId;
        invalidateLabelTemplate();
    }

    public void setLabelColor(int color) {
        this.mLabelColor = color;
        invalidateLabelTemplate();
    }

    private void invalidateLabelTemplate() {
        if (this.mLabelTemplateRes != 0) {
            CharSequence template = getResources().getText(this.mLabelTemplateRes);
            TextPaint paint = new TextPaint(1);
            paint.density = getResources().getDisplayMetrics().density;
            paint.setCompatibilityScaling(getResources().getCompatibilityInfo().applicationScale);
            paint.setColor(this.mLabelColor);
            this.mLabelTemplate = new SpannableStringBuilder(template);
            this.mLabelLayout = new DynamicLayout(this.mLabelTemplate, paint, 1024, Layout.Alignment.ALIGN_RIGHT, 1.0f, 0.0f, false);
            invalidateLabel();
        } else {
            this.mLabelTemplate = null;
            this.mLabelLayout = null;
        }
        invalidate();
        requestLayout();
    }

    private void invalidateLabel() {
        if (this.mLabelTemplate != null && this.mAxis != null) {
            this.mLabelValue = this.mAxis.buildLabel(getResources(), this.mLabelTemplate, this.mValue);
            setContentDescription(this.mLabelTemplate);
            invalidateLabelOffset();
            invalidate();
            return;
        }
        this.mLabelValue = this.mValue;
    }

    public void invalidateLabelOffset() {
        float labelOffset = 0.0f;
        if (this.mFollowAxis == 1) {
            if (this.mValidAfterDynamic != null) {
                this.mLabelSize = Math.max(getLabelWidth(this), getLabelWidth(this.mValidAfterDynamic));
                float margin = getLabelTop(this.mValidAfterDynamic) - getLabelBottom(this);
                if (margin < 0.0f) {
                    labelOffset = margin / 2.0f;
                }
            } else if (this.mValidBeforeDynamic != null) {
                this.mLabelSize = Math.max(getLabelWidth(this), getLabelWidth(this.mValidBeforeDynamic));
                float margin2 = getLabelTop(this) - getLabelBottom(this.mValidBeforeDynamic);
                if (margin2 < 0.0f) {
                    labelOffset = (-margin2) / 2.0f;
                }
            } else {
                this.mLabelSize = getLabelWidth(this);
            }
        }
        this.mLabelSize = Math.max(this.mLabelSize, this.mLabelMinSize);
        if (labelOffset != this.mLabelOffset) {
            this.mLabelOffset = labelOffset;
            invalidate();
            if (this.mValidAfterDynamic != null) {
                this.mValidAfterDynamic.invalidateLabelOffset();
            }
            if (this.mValidBeforeDynamic != null) {
                this.mValidBeforeDynamic.invalidateLabelOffset();
            }
        }
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        if (this.mSweep != null) {
            this.mSweep.jumpToCurrentState();
        }
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (this.mSweep != null) {
            this.mSweep.setVisible(visibility == 0, false);
        }
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return who == this.mSweep || super.verifyDrawable(who);
    }

    public void setValue(long value) {
        this.mValue = value;
        invalidateLabel();
    }

    public long getValue() {
        return this.mValue;
    }

    public long getLabelValue() {
        return this.mLabelValue;
    }

    public float getPoint() {
        if (isEnabled()) {
            return this.mAxis.convertToPoint(this.mValue);
        }
        return 0.0f;
    }

    public void setValidRange(long validAfter, long validBefore) {
        this.mValidAfter = validAfter;
        this.mValidBefore = validBefore;
    }

    public void setNeighborMargin(float neighborMargin) {
        this.mNeighborMargin = neighborMargin;
    }

    public void setSafeRegion(int safeRegion) {
        this.mSafeRegion = safeRegion;
    }

    public void setValidRangeDynamic(ChartSweepView validAfter, ChartSweepView validBefore) {
        this.mValidAfterDynamic = validAfter;
        this.mValidBeforeDynamic = validBefore;
    }

    public boolean isTouchCloserTo(MotionEvent eventInParent, ChartSweepView another) {
        float selfDist = getTouchDistanceFromTarget(eventInParent);
        float anotherDist = another.getTouchDistanceFromTarget(eventInParent);
        return anotherDist < selfDist;
    }

    private float getTouchDistanceFromTarget(MotionEvent eventInParent) {
        return this.mFollowAxis == 0 ? Math.abs(eventInParent.getX() - (getX() + getTargetInset())) : Math.abs(eventInParent.getY() - (getY() + getTargetInset()));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        long value;
        boolean acceptDrag;
        boolean acceptLabel;
        if (!isEnabled()) {
            return false;
        }
        View parent = (View) getParent();
        switch (event.getAction()) {
            case 0:
                if (this.mFollowAxis == 1) {
                    acceptDrag = event.getX() > ((float) (getWidth() - (this.mSweepPadding.right * 8)));
                    acceptLabel = this.mLabelLayout != null && event.getX() < ((float) this.mLabelLayout.getWidth());
                } else {
                    acceptDrag = event.getY() > ((float) (getHeight() - (this.mSweepPadding.bottom * 8)));
                    acceptLabel = this.mLabelLayout != null && event.getY() < ((float) this.mLabelLayout.getHeight());
                }
                MotionEvent eventInParent = event.copy();
                eventInParent.offsetLocation(getLeft(), getTop());
                ChartSweepView[] arr$ = this.mNeighbors;
                for (ChartSweepView neighbor : arr$) {
                    if (isTouchCloserTo(eventInParent, neighbor)) {
                        return false;
                    }
                }
                if (acceptDrag) {
                    if (this.mFollowAxis == 1) {
                        this.mTrackingStart = getTop() - this.mMargins.top;
                    } else {
                        this.mTrackingStart = getLeft() - this.mMargins.left;
                    }
                    this.mTracking = event.copy();
                    this.mTouchMode = 1;
                    if (!parent.isActivated()) {
                        parent.setActivated(true);
                    }
                    return true;
                }
                if (acceptLabel) {
                    this.mTouchMode = 2;
                    return true;
                }
                this.mTouchMode = 0;
                return false;
            case 1:
                if (this.mTouchMode == 2) {
                    performClick();
                } else if (this.mTouchMode == 1) {
                    this.mTrackingStart = 0.0f;
                    this.mTracking = null;
                    this.mValue = this.mLabelValue;
                    dispatchOnSweep(true);
                    setTranslationX(0.0f);
                    setTranslationY(0.0f);
                    requestLayout();
                }
                this.mTouchMode = 0;
                return true;
            case 2:
                if (this.mTouchMode == 2) {
                    return true;
                }
                getParent().requestDisallowInterceptTouchEvent(true);
                Rect parentContent = getParentContentRect();
                Rect clampRect = computeClampRect(parentContent);
                if (clampRect.isEmpty()) {
                    return true;
                }
                if (this.mFollowAxis == 1) {
                    float currentTargetY = getTop() - this.mMargins.top;
                    float requestedTargetY = this.mTrackingStart + (event.getRawY() - this.mTracking.getRawY());
                    float clampedTargetY = MathUtils.constrain(requestedTargetY, clampRect.top, clampRect.bottom);
                    setTranslationY(clampedTargetY - currentTargetY);
                    value = this.mAxis.convertToValue(clampedTargetY - parentContent.top);
                } else {
                    float currentTargetX = getLeft() - this.mMargins.left;
                    float requestedTargetX = this.mTrackingStart + (event.getRawX() - this.mTracking.getRawX());
                    float clampedTargetX = MathUtils.constrain(requestedTargetX, clampRect.left, clampRect.right);
                    setTranslationX(clampedTargetX - currentTargetX);
                    value = this.mAxis.convertToValue(clampedTargetX - parentContent.left);
                }
                setValue(value - (value % this.mDragInterval));
                dispatchOnSweep(false);
                return true;
            default:
                return false;
        }
    }

    public void updateValueFromPosition() {
        Rect parentContent = getParentContentRect();
        if (this.mFollowAxis == 1) {
            float effectiveY = (getY() - this.mMargins.top) - parentContent.top;
            setValue(this.mAxis.convertToValue(effectiveY));
        } else {
            float effectiveX = (getX() - this.mMargins.left) - parentContent.left;
            setValue(this.mAxis.convertToValue(effectiveX));
        }
    }

    public int shouldAdjustAxis() {
        return this.mAxis.shouldAdjustAxis(getValue());
    }

    private Rect getParentContentRect() {
        View parent = (View) getParent();
        return new Rect(parent.getPaddingLeft(), parent.getPaddingTop(), parent.getWidth() - parent.getPaddingRight(), parent.getHeight() - parent.getPaddingBottom());
    }

    @Override
    public void addOnLayoutChangeListener(View.OnLayoutChangeListener listener) {
    }

    @Override
    public void removeOnLayoutChangeListener(View.OnLayoutChangeListener listener) {
    }

    private long getValidAfterDynamic() {
        ChartSweepView dynamic = this.mValidAfterDynamic;
        if (dynamic == null || !dynamic.isEnabled()) {
            return Long.MIN_VALUE;
        }
        return dynamic.getValue();
    }

    private long getValidBeforeDynamic() {
        ChartSweepView dynamic = this.mValidBeforeDynamic;
        if (dynamic == null || !dynamic.isEnabled()) {
            return Long.MAX_VALUE;
        }
        return dynamic.getValue();
    }

    private Rect computeClampRect(Rect parentContent) {
        Rect rect = buildClampRect(parentContent, this.mValidAfter, this.mValidBefore, 0.0f);
        Rect dynamicRect = buildClampRect(parentContent, getValidAfterDynamic(), getValidBeforeDynamic(), this.mNeighborMargin);
        if (!rect.intersect(dynamicRect)) {
            rect.setEmpty();
        }
        return rect;
    }

    private Rect buildClampRect(Rect parentContent, long afterValue, long beforeValue, float margin) {
        if (this.mAxis instanceof InvertedChartAxis) {
            beforeValue = afterValue;
            afterValue = beforeValue;
        }
        boolean afterValid = (afterValue == Long.MIN_VALUE || afterValue == Long.MAX_VALUE) ? false : true;
        boolean beforeValid = (beforeValue == Long.MIN_VALUE || beforeValue == Long.MAX_VALUE) ? false : true;
        float afterPoint = this.mAxis.convertToPoint(afterValue) + margin;
        float beforePoint = this.mAxis.convertToPoint(beforeValue) - margin;
        Rect clampRect = new Rect(parentContent);
        if (this.mFollowAxis == 1) {
            if (beforeValid) {
                clampRect.bottom = clampRect.top + ((int) beforePoint);
            }
            if (afterValid) {
                clampRect.top = (int) (clampRect.top + afterPoint);
            }
        } else {
            if (beforeValid) {
                clampRect.right = clampRect.left + ((int) beforePoint);
            }
            if (afterValid) {
                clampRect.left = (int) (clampRect.left + afterPoint);
            }
        }
        return clampRect;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (this.mSweep.isStateful()) {
            this.mSweep.setState(getDrawableState());
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (isEnabled() && this.mLabelLayout != null) {
            int sweepHeight = this.mSweep.getIntrinsicHeight();
            int templateHeight = this.mLabelLayout.getHeight();
            this.mSweepOffset.x = 0;
            this.mSweepOffset.y = 0;
            this.mSweepOffset.y = (int) ((templateHeight / 2) - getTargetInset());
            setMeasuredDimension(this.mSweep.getIntrinsicWidth(), Math.max(sweepHeight, templateHeight));
        } else {
            this.mSweepOffset.x = 0;
            this.mSweepOffset.y = 0;
            setMeasuredDimension(this.mSweep.getIntrinsicWidth(), this.mSweep.getIntrinsicHeight());
        }
        if (this.mFollowAxis == 1) {
            int targetHeight = (this.mSweep.getIntrinsicHeight() - this.mSweepPadding.top) - this.mSweepPadding.bottom;
            this.mMargins.top = -(this.mSweepPadding.top + (targetHeight / 2));
            this.mMargins.bottom = 0;
            this.mMargins.left = -this.mSweepPadding.left;
            this.mMargins.right = this.mSweepPadding.right;
        } else {
            int targetWidth = (this.mSweep.getIntrinsicWidth() - this.mSweepPadding.left) - this.mSweepPadding.right;
            this.mMargins.left = -(this.mSweepPadding.left + (targetWidth / 2));
            this.mMargins.right = 0;
            this.mMargins.top = -this.mSweepPadding.top;
            this.mMargins.bottom = this.mSweepPadding.bottom;
        }
        this.mContentOffset.set(0, 0, 0, 0);
        int widthBefore = getMeasuredWidth();
        int heightBefore = getMeasuredHeight();
        if (this.mFollowAxis == 0) {
            int widthAfter = widthBefore * 3;
            setMeasuredDimension(widthAfter, heightBefore);
            this.mContentOffset.left = (widthAfter - widthBefore) / 2;
            int offset = this.mSweepPadding.bottom * 2;
            this.mContentOffset.bottom -= offset;
            this.mMargins.bottom += offset;
        } else {
            int heightAfter = heightBefore * 2;
            setMeasuredDimension(widthBefore, heightAfter);
            this.mContentOffset.offset(0, (heightAfter - heightBefore) / 2);
            int offset2 = this.mSweepPadding.right * 2;
            this.mContentOffset.right -= offset2;
            this.mMargins.right += offset2;
        }
        this.mSweepOffset.offset(this.mContentOffset.left, this.mContentOffset.top);
        this.mMargins.offset(-this.mSweepOffset.x, -this.mSweepOffset.y);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        invalidateLabelOffset();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int labelSize;
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        if (isEnabled() && this.mLabelLayout != null) {
            int count = canvas.save();
            float alignOffset = this.mLabelSize - 1024.0f;
            canvas.translate(this.mContentOffset.left + alignOffset, this.mContentOffset.top + this.mLabelOffset);
            this.mLabelLayout.draw(canvas);
            canvas.restoreToCount(count);
            labelSize = ((int) this.mLabelSize) + this.mSafeRegion;
        } else {
            labelSize = 0;
        }
        if (this.mFollowAxis == 1) {
            this.mSweep.setBounds(labelSize, this.mSweepOffset.y, this.mContentOffset.right + width, this.mSweepOffset.y + this.mSweep.getIntrinsicHeight());
        } else {
            this.mSweep.setBounds(this.mSweepOffset.x, labelSize, this.mSweepOffset.x + this.mSweep.getIntrinsicWidth(), this.mContentOffset.bottom + height);
        }
        this.mSweep.draw(canvas);
    }

    public static float getLabelTop(ChartSweepView view) {
        return view.getY() + view.mContentOffset.top;
    }

    public static float getLabelBottom(ChartSweepView view) {
        return getLabelTop(view) + view.mLabelLayout.getHeight();
    }

    public static float getLabelWidth(ChartSweepView view) {
        return Layout.getDesiredWidth(view.mLabelLayout.getText(), view.mLabelLayout.getPaint());
    }
}
