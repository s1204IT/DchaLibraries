package android.support.v17.leanback.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.v17.leanback.R;
import android.util.AttributeSet;
import android.view.View;

public final class SeekBar extends View {
    private AccessibilitySeekListener mAccessibilitySeekListener;
    private int mActiveBarHeight;
    private int mActiveRadius;
    private final Paint mBackgroundPaint;
    private final RectF mBackgroundRect;
    private int mBarHeight;
    private final Paint mKnobPaint;
    private int mKnobx;
    private int mMax;
    private int mProgress;
    private final Paint mProgressPaint;
    private final RectF mProgressRect;
    private int mSecondProgress;
    private final Paint mSecondProgressPaint;
    private final RectF mSecondProgressRect;

    public static abstract class AccessibilitySeekListener {
        public abstract boolean onAccessibilitySeekBackward();

        public abstract boolean onAccessibilitySeekForward();
    }

    public SeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mProgressRect = new RectF();
        this.mSecondProgressRect = new RectF();
        this.mBackgroundRect = new RectF();
        this.mSecondProgressPaint = new Paint(1);
        this.mProgressPaint = new Paint(1);
        this.mBackgroundPaint = new Paint(1);
        this.mKnobPaint = new Paint(1);
        setWillNotDraw(false);
        this.mBackgroundPaint.setColor(-7829368);
        this.mSecondProgressPaint.setColor(-3355444);
        this.mProgressPaint.setColor(-65536);
        this.mKnobPaint.setColor(-1);
        this.mBarHeight = context.getResources().getDimensionPixelSize(R.dimen.lb_playback_transport_progressbar_bar_height);
        this.mActiveBarHeight = context.getResources().getDimensionPixelSize(R.dimen.lb_playback_transport_progressbar_active_bar_height);
        this.mActiveRadius = context.getResources().getDimensionPixelSize(R.dimen.lb_playback_transport_progressbar_active_radius);
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        calculate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        calculate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int radius = isFocused() ? this.mActiveRadius : this.mBarHeight / 2;
        canvas.drawRoundRect(this.mBackgroundRect, radius, radius, this.mBackgroundPaint);
        canvas.drawRoundRect(this.mSecondProgressRect, radius, radius, this.mProgressPaint);
        canvas.drawRoundRect(this.mProgressRect, radius, radius, this.mProgressPaint);
        canvas.drawCircle(this.mKnobx, getHeight() / 2, radius, this.mKnobPaint);
    }

    private void calculate() {
        int barHeight = isFocused() ? this.mActiveBarHeight : this.mBarHeight;
        int width = getWidth();
        int height = getHeight();
        int verticalPadding = (height - barHeight) / 2;
        this.mBackgroundRect.set(this.mBarHeight / 2, verticalPadding, width - (this.mBarHeight / 2), height - verticalPadding);
        int radius = isFocused() ? this.mActiveRadius : this.mBarHeight / 2;
        int progressWidth = width - (radius * 2);
        float progressPixels = (this.mProgress / this.mMax) * progressWidth;
        this.mProgressRect.set(this.mBarHeight / 2, verticalPadding, (this.mBarHeight / 2) + progressPixels, height - verticalPadding);
        float secondProgressPixels = (this.mSecondProgress / this.mMax) * progressWidth;
        this.mSecondProgressRect.set(this.mBarHeight / 2, verticalPadding, (this.mBarHeight / 2) + secondProgressPixels, height - verticalPadding);
        this.mKnobx = ((int) progressPixels) + radius;
        invalidate();
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return android.widget.SeekBar.class.getName();
    }

    @Override
    public boolean performAccessibilityAction(int action, Bundle arguments) {
        if (this.mAccessibilitySeekListener != null) {
            if (action == 4096) {
                return this.mAccessibilitySeekListener.onAccessibilitySeekForward();
            }
            if (action == 8192) {
                return this.mAccessibilitySeekListener.onAccessibilitySeekBackward();
            }
        }
        return super.performAccessibilityAction(action, arguments);
    }
}
