package com.android.gallery3d.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import com.android.gallery3d.R;
import com.android.gallery3d.common.Utils;

public class TimeBar extends View {
    protected int mCurrentTime;
    protected final Listener mListener;
    protected final Rect mPlayedBar;
    protected final Paint mPlayedPaint;
    protected final Rect mProgressBar;
    protected final Paint mProgressPaint;
    protected final Bitmap mScrubber;
    protected int mScrubberCorrection;
    protected int mScrubberLeft;
    protected int mScrubberPadding;
    protected int mScrubberTop;
    protected boolean mScrubbing;
    protected boolean mShowScrubber;
    protected boolean mShowTimes;
    protected final Rect mTimeBounds;
    protected final Paint mTimeTextPaint;
    protected int mTotalTime;
    protected int mVPaddingInPx;

    public interface Listener {
        void onScrubbingEnd(int i, int i2, int i3);

        void onScrubbingMove(int i);

        void onScrubbingStart();
    }

    public TimeBar(Context context, Listener listener) {
        super(context);
        this.mListener = (Listener) Utils.checkNotNull(listener);
        this.mShowTimes = true;
        this.mShowScrubber = true;
        this.mProgressBar = new Rect();
        this.mPlayedBar = new Rect();
        this.mProgressPaint = new Paint();
        this.mProgressPaint.setColor(-8355712);
        this.mPlayedPaint = new Paint();
        this.mPlayedPaint.setColor(-1);
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        float textSizeInPx = metrics.density * 14.0f;
        this.mTimeTextPaint = new Paint(1);
        this.mTimeTextPaint.setColor(-3223858);
        this.mTimeTextPaint.setTextSize(textSizeInPx);
        this.mTimeTextPaint.setTextAlign(Paint.Align.CENTER);
        this.mTimeBounds = new Rect();
        this.mTimeTextPaint.getTextBounds("0:00:00", 0, 7, this.mTimeBounds);
        this.mScrubber = BitmapFactory.decodeResource(getResources(), R.drawable.scrubber_knob);
        this.mScrubberPadding = (int) (metrics.density * 10.0f);
        this.mVPaddingInPx = (int) (metrics.density * 30.0f);
    }

    private void update() {
        this.mPlayedBar.set(this.mProgressBar);
        if (this.mTotalTime > 0) {
            this.mPlayedBar.right = this.mPlayedBar.left + ((int) ((((long) this.mProgressBar.width()) * ((long) this.mCurrentTime)) / ((long) this.mTotalTime)));
        } else {
            this.mPlayedBar.right = this.mProgressBar.left;
        }
        if (!this.mScrubbing) {
            this.mScrubberLeft = this.mPlayedBar.right - (this.mScrubber.getWidth() / 2);
        }
        invalidate();
    }

    public int getPreferredHeight() {
        return this.mTimeBounds.height() + this.mVPaddingInPx + this.mScrubberPadding;
    }

    public int getBarHeight() {
        return this.mTimeBounds.height() + this.mVPaddingInPx;
    }

    public void setTime(int currentTime, int totalTime, int trimStartTime, int trimEndTime) {
        if (this.mCurrentTime != currentTime || this.mTotalTime != totalTime) {
            this.mCurrentTime = currentTime;
            this.mTotalTime = totalTime;
            update();
        }
    }

    private boolean inScrubber(float x, float y) {
        int scrubberRight = this.mScrubberLeft + this.mScrubber.getWidth();
        int scrubberBottom = this.mScrubberTop + this.mScrubber.getHeight();
        return ((float) (this.mScrubberLeft - this.mScrubberPadding)) < x && x < ((float) (this.mScrubberPadding + scrubberRight)) && ((float) (this.mScrubberTop - this.mScrubberPadding)) < y && y < ((float) (this.mScrubberPadding + scrubberBottom));
    }

    private void clampScrubber() {
        int half = this.mScrubber.getWidth() / 2;
        int max = this.mProgressBar.right - half;
        int min = this.mProgressBar.left - half;
        this.mScrubberLeft = Math.min(max, Math.max(min, this.mScrubberLeft));
    }

    private int getScrubberTime() {
        return (int) ((((long) ((this.mScrubberLeft + (this.mScrubber.getWidth() / 2)) - this.mProgressBar.left)) * ((long) this.mTotalTime)) / ((long) this.mProgressBar.width()));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int w = r - l;
        int h = b - t;
        if (!this.mShowTimes && !this.mShowScrubber) {
            this.mProgressBar.set(0, 0, w, h);
        } else {
            int margin = this.mScrubber.getWidth() / 3;
            if (this.mShowTimes) {
                margin += this.mTimeBounds.width();
            }
            int progressY = (this.mScrubberPadding + h) / 2;
            this.mScrubberTop = (progressY - (this.mScrubber.getHeight() / 2)) + 1;
            this.mProgressBar.set(getPaddingLeft() + margin, progressY, (w - getPaddingRight()) - margin, progressY + 4);
        }
        update();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawRect(this.mProgressBar, this.mProgressPaint);
        canvas.drawRect(this.mPlayedBar, this.mPlayedPaint);
        if (this.mShowScrubber) {
            canvas.drawBitmap(this.mScrubber, this.mScrubberLeft, this.mScrubberTop, (Paint) null);
        }
        if (this.mShowTimes) {
            canvas.drawText(stringForTime(this.mCurrentTime), (this.mTimeBounds.width() / 2) + getPaddingLeft(), this.mTimeBounds.height() + (this.mVPaddingInPx / 2) + this.mScrubberPadding + 1, this.mTimeTextPaint);
            canvas.drawText(stringForTime(this.mTotalTime), (getWidth() - getPaddingRight()) - (this.mTimeBounds.width() / 2), this.mTimeBounds.height() + (this.mVPaddingInPx / 2) + this.mScrubberPadding + 1, this.mTimeTextPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!this.mShowScrubber) {
            return false;
        }
        int x = (int) event.getX();
        int y = (int) event.getY();
        switch (event.getAction()) {
            case 0:
                this.mScrubberCorrection = inScrubber((float) x, (float) y) ? x - this.mScrubberLeft : this.mScrubber.getWidth() / 2;
                this.mScrubbing = true;
                this.mListener.onScrubbingStart();
                break;
            case 1:
            case 3:
                this.mListener.onScrubbingEnd(getScrubberTime(), 0, 0);
                this.mScrubbing = false;
                return true;
            case 2:
                break;
            default:
                return false;
        }
        this.mScrubberLeft = x - this.mScrubberCorrection;
        clampScrubber();
        this.mCurrentTime = getScrubberTime();
        this.mListener.onScrubbingMove(this.mCurrentTime);
        invalidate();
        return true;
    }

    protected String stringForTime(long millis) {
        int totalSeconds = ((int) millis) / 1000;
        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;
        return hours > 0 ? String.format("%d:%02d:%02d", Integer.valueOf(hours), Integer.valueOf(minutes), Integer.valueOf(seconds)).toString() : String.format("%02d:%02d", Integer.valueOf(minutes), Integer.valueOf(seconds)).toString();
    }

    public void setSeekable(boolean canSeek) {
        this.mShowScrubber = canSeek;
    }
}
