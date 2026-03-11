package com.android.keyguard;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

public class KeyguardWidgetFrame extends FrameLayout {
    private static final PorterDuffXfermode sAddBlendMode = new PorterDuffXfermode(PorterDuff.Mode.ADD);
    private float mBackgroundAlpha;
    private float mBackgroundAlphaMultiplier;
    private Drawable mBackgroundDrawable;
    private Rect mBackgroundRect;
    private Object mBgAlphaController;
    private float mContentAlpha;
    private int mForegroundAlpha;
    private LinearGradient mForegroundGradient;
    private final Rect mForegroundRect;
    private Animator mFrameFade;
    private int mFrameHeight;
    private int mFrameStrokeAdjustment;
    private int mGradientColor;
    private Paint mGradientPaint;
    private boolean mIsHoveringOverDeleteDropTarget;
    private boolean mIsSmall;
    boolean mLeftToRight;
    private LinearGradient mLeftToRightGradient;
    private CheckLongPressHelper mLongPressHelper;
    private int mMaxChallengeTop;
    private float mOverScrollAmount;
    private boolean mPerformAppWidgetSizeUpdateOnBootComplete;
    private LinearGradient mRightToLeftGradient;
    private int mSmallFrameHeight;
    private int mSmallWidgetHeight;
    private KeyguardUpdateMonitorCallback mUpdateMonitorCallbacks;
    private boolean mWidgetLockedSmall;
    private Handler mWorkerHandler;

    public KeyguardWidgetFrame(Context context) {
        this(context, null, 0);
    }

    public KeyguardWidgetFrame(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KeyguardWidgetFrame(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mGradientPaint = new Paint();
        this.mLeftToRight = true;
        this.mOverScrollAmount = 0.0f;
        this.mForegroundRect = new Rect();
        this.mForegroundAlpha = 0;
        this.mIsSmall = false;
        this.mBackgroundAlphaMultiplier = 1.0f;
        this.mBackgroundRect = new Rect();
        this.mWidgetLockedSmall = false;
        this.mMaxChallengeTop = -1;
        this.mUpdateMonitorCallbacks = new KeyguardUpdateMonitorCallback() {
            @Override
            public void onBootCompleted() {
                if (KeyguardWidgetFrame.this.mPerformAppWidgetSizeUpdateOnBootComplete) {
                    KeyguardWidgetFrame.this.performAppWidgetSizeCallbacksIfNecessary();
                    KeyguardWidgetFrame.this.mPerformAppWidgetSizeUpdateOnBootComplete = false;
                }
            }
        };
        this.mLongPressHelper = new CheckLongPressHelper(this);
        Resources res = context.getResources();
        float density = res.getDisplayMetrics().density;
        int padding = (int) (res.getDisplayMetrics().density * 8.0f);
        setPadding(padding, padding, padding, padding);
        this.mFrameStrokeAdjustment = ((int) (2.0f * density)) + 2;
        this.mSmallWidgetHeight = res.getDimensionPixelSize(R.dimen.kg_small_widget_height);
        this.mBackgroundDrawable = res.getDrawable(R.drawable.kg_widget_bg_padded);
        this.mGradientColor = res.getColor(R.color.kg_widget_pager_gradient);
        this.mGradientPaint.setXfermode(sAddBlendMode);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        cancelLongPress();
        KeyguardUpdateMonitor.getInstance(this.mContext).removeCallback(this.mUpdateMonitorCallbacks);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        KeyguardUpdateMonitor.getInstance(this.mContext).registerCallback(this.mUpdateMonitorCallbacks);
    }

    void setIsHoveringOverDeleteDropTarget(boolean isHovering) {
        if (this.mIsHoveringOverDeleteDropTarget != isHovering) {
            this.mIsHoveringOverDeleteDropTarget = isHovering;
            int resId = isHovering ? R.string.keyguard_accessibility_delete_widget_start : R.string.keyguard_accessibility_delete_widget_end;
            String text = getContext().getResources().getString(resId, getContentDescription());
            announceForAccessibility(text);
            invalidate();
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case 0:
                this.mLongPressHelper.postCheckForLongPress(ev);
                break;
            case 1:
            case 3:
            case 5:
                this.mLongPressHelper.cancelLongPress();
                break;
            case 2:
                this.mLongPressHelper.onMove(ev);
                break;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case 1:
            case 3:
            case 5:
                this.mLongPressHelper.cancelLongPress();
                break;
            case 2:
                this.mLongPressHelper.onMove(ev);
                break;
        }
        return true;
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        super.requestDisallowInterceptTouchEvent(disallowIntercept);
        cancelLongPress();
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        this.mLongPressHelper.cancelLongPress();
    }

    private void drawGradientOverlay(Canvas c) {
        this.mGradientPaint.setShader(this.mForegroundGradient);
        this.mGradientPaint.setAlpha(this.mForegroundAlpha);
        c.drawRect(this.mForegroundRect, this.mGradientPaint);
    }

    private void drawHoveringOverDeleteOverlay(Canvas c) {
        if (this.mIsHoveringOverDeleteDropTarget) {
            c.drawColor(-1711341568);
        }
    }

    protected void drawBg(Canvas canvas) {
        if (this.mBackgroundAlpha > 0.0f) {
            Drawable bg = this.mBackgroundDrawable;
            bg.setAlpha((int) (this.mBackgroundAlpha * this.mBackgroundAlphaMultiplier * 255.0f));
            bg.setBounds(this.mBackgroundRect);
            bg.draw(canvas);
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        canvas.save();
        drawBg(canvas);
        super.dispatchDraw(canvas);
        drawGradientOverlay(canvas);
        drawHoveringOverDeleteOverlay(canvas);
        canvas.restore();
    }

    public void enableHardwareLayersForContent() {
        View widget = getContent();
        if (widget != null && widget.isHardwareAccelerated()) {
            widget.setLayerType(2, null);
        }
    }

    public void disableHardwareLayersForContent() {
        View widget = getContent();
        if (widget != null) {
            widget.setLayerType(0, null);
        }
    }

    public View getContent() {
        return getChildAt(0);
    }

    public int getContentAppWidgetId() {
        View content = getContent();
        if (content instanceof AppWidgetHostView) {
            return ((AppWidgetHostView) content).getAppWidgetId();
        }
        if (content instanceof KeyguardStatusView) {
            return ((KeyguardStatusView) content).getAppWidgetId();
        }
        return 0;
    }

    public void setBackgroundAlpha(float alpha) {
        if (Float.compare(this.mBackgroundAlpha, alpha) != 0) {
            this.mBackgroundAlpha = alpha;
            invalidate();
        }
    }

    public float getContentAlpha() {
        return this.mContentAlpha;
    }

    public void setContentAlpha(float alpha) {
        this.mContentAlpha = alpha;
        View content = getContent();
        if (content != null) {
            content.setAlpha(alpha);
        }
    }

    private void setWidgetHeight(int height) {
        boolean needLayout = false;
        View widget = getContent();
        if (widget != null) {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) widget.getLayoutParams();
            if (lp.height != height) {
                needLayout = true;
                lp.height = height;
            }
        }
        if (needLayout) {
            requestLayout();
        }
    }

    public void setMaxChallengeTop(int top) {
        boolean dirty = this.mMaxChallengeTop != top;
        this.mMaxChallengeTop = top;
        this.mSmallWidgetHeight = top - getPaddingTop();
        this.mSmallFrameHeight = getPaddingBottom() + top;
        if (dirty && this.mIsSmall) {
            setWidgetHeight(this.mSmallWidgetHeight);
            setFrameHeight(this.mSmallFrameHeight);
        } else if (dirty && this.mWidgetLockedSmall) {
            setWidgetHeight(this.mSmallWidgetHeight);
        }
    }

    public boolean isSmall() {
        return this.mIsSmall;
    }

    public void adjustFrame(int challengeTop) {
        int frameHeight = challengeTop + getPaddingBottom();
        setFrameHeight(frameHeight);
    }

    public void shrinkWidget(boolean alsoShrinkFrame) {
        this.mIsSmall = true;
        setWidgetHeight(this.mSmallWidgetHeight);
        if (alsoShrinkFrame) {
            setFrameHeight(this.mSmallFrameHeight);
        }
    }

    public int getSmallFrameHeight() {
        return this.mSmallFrameHeight;
    }

    public void setWidgetLockedSmall(boolean locked) {
        if (locked) {
            setWidgetHeight(this.mSmallWidgetHeight);
        }
        this.mWidgetLockedSmall = locked;
    }

    public void resetSize() {
        this.mIsSmall = false;
        if (!this.mWidgetLockedSmall) {
            setWidgetHeight(-1);
        }
        setFrameHeight(getMeasuredHeight());
    }

    public void setFrameHeight(int height) {
        this.mFrameHeight = height;
        this.mBackgroundRect.set(0, 0, getMeasuredWidth(), Math.min(this.mFrameHeight, getMeasuredHeight()));
        this.mForegroundRect.set(this.mFrameStrokeAdjustment, this.mFrameStrokeAdjustment, getMeasuredWidth() - this.mFrameStrokeAdjustment, Math.min(getMeasuredHeight(), this.mFrameHeight) - this.mFrameStrokeAdjustment);
        updateGradient();
        invalidate();
    }

    public void hideFrame(Object caller) {
        fadeFrame(caller, false, 0.0f, 375);
    }

    public void showFrame(Object caller) {
        fadeFrame(caller, true, 0.6f, 100);
    }

    public void fadeFrame(Object caller, boolean takeControl, float alpha, int duration) {
        if (takeControl) {
            this.mBgAlphaController = caller;
        }
        if (this.mBgAlphaController == caller || this.mBgAlphaController == null) {
            if (this.mFrameFade != null) {
                this.mFrameFade.cancel();
                this.mFrameFade = null;
            }
            PropertyValuesHolder bgAlpha = PropertyValuesHolder.ofFloat("backgroundAlpha", alpha);
            this.mFrameFade = ObjectAnimator.ofPropertyValuesHolder(this, bgAlpha);
            this.mFrameFade.setDuration(duration);
            this.mFrameFade.start();
        }
    }

    private void updateGradient() {
        float x0 = this.mLeftToRight ? 0.0f : this.mForegroundRect.width();
        float x1 = this.mLeftToRight ? this.mForegroundRect.width() : 0.0f;
        this.mLeftToRightGradient = new LinearGradient(x0, 0.0f, x1, 0.0f, this.mGradientColor, 0, Shader.TileMode.CLAMP);
        this.mRightToLeftGradient = new LinearGradient(x1, 0.0f, x0, 0.0f, this.mGradientColor, 0, Shader.TileMode.CLAMP);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (!this.mIsSmall) {
            this.mFrameHeight = h;
        }
        this.mForegroundRect.set(this.mFrameStrokeAdjustment, this.mFrameStrokeAdjustment, w - this.mFrameStrokeAdjustment, Math.min(h, this.mFrameHeight) - this.mFrameStrokeAdjustment);
        this.mBackgroundRect.set(0, 0, getMeasuredWidth(), Math.min(h, this.mFrameHeight));
        updateGradient();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        performAppWidgetSizeCallbacksIfNecessary();
    }

    public void performAppWidgetSizeCallbacksIfNecessary() {
        View content = getContent();
        if (content instanceof AppWidgetHostView) {
            if (!KeyguardUpdateMonitor.getInstance(this.mContext).hasBootCompleted()) {
                this.mPerformAppWidgetSizeUpdateOnBootComplete = true;
                return;
            }
            AppWidgetHostView awhv = (AppWidgetHostView) content;
            float density = getResources().getDisplayMetrics().density;
            int width = (int) (content.getMeasuredWidth() / density);
            int height = (int) (content.getMeasuredHeight() / density);
            awhv.updateAppWidgetSize(null, width, height, width, height, true);
        }
    }

    void setOverScrollAmount(float r, boolean left) {
        if (Float.compare(this.mOverScrollAmount, r) != 0) {
            this.mOverScrollAmount = r;
            this.mForegroundGradient = left ? this.mLeftToRightGradient : this.mRightToLeftGradient;
            this.mForegroundAlpha = Math.round(0.5f * r * 255.0f);
            float bgAlpha = Math.min(0.6f + (0.39999998f * r), 1.0f);
            setBackgroundAlpha(bgAlpha);
            invalidate();
        }
    }

    public void onActive(boolean isActive) {
    }

    public boolean onUserInteraction(MotionEvent event) {
        return false;
    }

    public void onBouncerShowing(boolean showing) {
    }

    public void setWorkerHandler(Handler workerHandler) {
        this.mWorkerHandler = workerHandler;
    }

    public Handler getWorkerHandler() {
        return this.mWorkerHandler;
    }
}
