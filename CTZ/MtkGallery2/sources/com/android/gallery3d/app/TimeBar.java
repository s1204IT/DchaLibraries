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
    private boolean mEnableScrubbing;
    private InfoExtImpl mInfoExt;
    private int mLastLength;
    private int mLastShowTime;
    private LayoutExt mLayoutExt;
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
    private SecondaryProgressExtImpl mSecondaryProgressExt;
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
        this.mLastShowTime = -1;
        this.mLastLength = 0;
        this.mListener = (Listener) Utils.checkNotNull(listener);
        this.mShowTimes = true;
        this.mShowScrubber = true;
        this.mProgressBar = new Rect();
        this.mPlayedBar = new Rect();
        this.mProgressPaint = new Paint();
        this.mProgressPaint.setColor(-8355712);
        this.mPlayedPaint = new Paint();
        this.mPlayedPaint.setColor(-1);
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        float f = displayMetrics.density * 14.0f;
        this.mTimeTextPaint = new Paint(1);
        this.mTimeTextPaint.setColor(-3223858);
        this.mTimeTextPaint.setTextSize(f);
        this.mTimeTextPaint.setTextAlign(Paint.Align.CENTER);
        this.mTimeBounds = new Rect();
        this.mScrubber = BitmapFactory.decodeResource(getResources(), R.drawable.scrubber_knob);
        this.mScrubberPadding = (int) (displayMetrics.density * 10.0f);
        this.mVPaddingInPx = (int) (displayMetrics.density * 30.0f);
        this.mSecondaryProgressExt = new SecondaryProgressExtImpl();
        this.mInfoExt = new InfoExtImpl(f);
        this.mLayoutExt = new LayoutExt();
        int width = (this.mScrubber.getWidth() / 2) + 1;
        setPadding(width, 0, width, 0);
    }

    private void update() {
        this.mPlayedBar.set(this.mProgressBar);
        if (this.mTotalTime > 0) {
            if (this.mCurrentTime >= this.mTotalTime) {
                this.mPlayedBar.right = this.mProgressBar.right;
            } else {
                this.mPlayedBar.right = this.mPlayedBar.left + ((int) ((((long) this.mProgressBar.width()) * ((long) this.mCurrentTime)) / ((long) this.mTotalTime)));
                if (this.mPlayedBar.right > this.mProgressBar.right) {
                    this.mPlayedBar.right = this.mProgressBar.right;
                }
            }
        } else {
            this.mPlayedBar.right = this.mProgressBar.left;
        }
        if (!this.mScrubbing) {
            this.mScrubberLeft = this.mPlayedBar.right - (this.mScrubber.getWidth() / 2);
        }
        updateBounds();
        this.mInfoExt.updateVisibleText(this, this.mProgressBar, this.mTimeBounds);
        invalidate();
    }

    public int getPreferredHeight() {
        return this.mLayoutExt.getPreferredHeight(this.mTimeBounds.height() + this.mVPaddingInPx + this.mScrubberPadding);
    }

    public int getBarHeight() {
        return this.mLayoutExt.getBarHeight(this.mTimeBounds.height() + this.mVPaddingInPx);
    }

    public void setTime(int i, int i2, int i3, int i4) {
        com.mediatek.gallery3d.util.Log.v("VP_TimeBar", "setTime(" + i + ", " + i2 + ")");
        if (this.mCurrentTime == i && this.mTotalTime == i2) {
            return;
        }
        this.mCurrentTime = i;
        this.mTotalTime = Math.abs(i2);
        if (i2 <= 0) {
            setScrubbing(false);
        }
        update();
    }

    private boolean inScrubber(float f, float f2) {
        return ((float) (this.mScrubberLeft - this.mScrubberPadding)) < f && f < ((float) ((this.mScrubberLeft + this.mScrubber.getWidth()) + this.mScrubberPadding)) && ((float) (this.mScrubberTop - this.mScrubberPadding)) < f2 && f2 < ((float) ((this.mScrubberTop + this.mScrubber.getHeight()) + this.mScrubberPadding));
    }

    private void clampScrubber() {
        int width = this.mScrubber.getWidth() / 2;
        this.mScrubberLeft = Math.min(this.mProgressBar.right - width, Math.max(this.mProgressBar.left - width, this.mScrubberLeft));
    }

    private int getScrubberTime() {
        return (int) ((((long) ((this.mScrubberLeft + (this.mScrubber.getWidth() / 2)) - this.mProgressBar.left)) * ((long) this.mTotalTime)) / ((long) this.mProgressBar.width()));
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        int i5 = i3 - i;
        int i6 = i4 - i2;
        if (!this.mShowTimes && !this.mShowScrubber) {
            this.mProgressBar.set(0, 0, i5, i6);
        } else {
            int width = this.mScrubber.getWidth() / 3;
            if (this.mShowTimes) {
                width += this.mTimeBounds.width();
            }
            int progressMargin = this.mLayoutExt.getProgressMargin(width);
            int progressOffset = ((i6 + this.mScrubberPadding) / 2) + this.mLayoutExt.getProgressOffset();
            this.mScrubberTop = (progressOffset - (this.mScrubber.getHeight() / 2)) + 1;
            this.mProgressBar.set(getPaddingLeft() + progressMargin, progressOffset, (i5 - getPaddingRight()) - progressMargin, progressOffset + 4);
        }
        update();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(this.mProgressBar, this.mProgressPaint);
        this.mSecondaryProgressExt.draw(canvas, this.mProgressBar);
        canvas.drawRect(this.mPlayedBar, this.mPlayedPaint);
        if (this.mShowScrubber) {
            canvas.drawBitmap(this.mScrubber, this.mScrubberLeft, this.mScrubberTop, (Paint) null);
        }
        if (this.mShowTimes) {
            canvas.drawText(stringForTime(this.mCurrentTime), (this.mTimeBounds.width() / 2) + getPaddingLeft(), this.mTimeBounds.height() + (this.mVPaddingInPx / 2) + this.mScrubberPadding + 1 + this.mLayoutExt.getTimeOffset(), this.mTimeTextPaint);
            canvas.drawText(stringForTime(this.mTotalTime), (getWidth() - getPaddingRight()) - (this.mTimeBounds.width() / 2), this.mTimeBounds.height() + (this.mVPaddingInPx / 2) + this.mScrubberPadding + 1 + this.mLayoutExt.getTimeOffset(), this.mTimeTextPaint);
        }
        this.mInfoExt.draw(canvas, this.mLayoutExt.getInfoBounds(this, this.mTimeBounds));
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        com.mediatek.gallery3d.util.Log.v("VP_TimeBar", "onTouchEvent() showScrubber=" + this.mShowScrubber + ", enableScrubbing=" + this.mEnableScrubbing + ", totalTime=" + this.mTotalTime + ", scrubbing=" + this.mScrubbing + ", event=" + motionEvent);
        if (this.mShowScrubber && this.mEnableScrubbing && this.mTotalTime > 1) {
            int x = (int) motionEvent.getX();
            int y = (int) motionEvent.getY();
            switch (motionEvent.getAction()) {
                case 0:
                    this.mScrubberCorrection = inScrubber((float) x, (float) y) ? x - this.mScrubberLeft : this.mScrubber.getWidth() / 2;
                    this.mScrubbing = true;
                    this.mListener.onScrubbingStart();
                    this.mScrubberLeft = x - this.mScrubberCorrection;
                    clampScrubber();
                    this.mCurrentTime = getScrubberTime();
                    this.mListener.onScrubbingMove(this.mCurrentTime);
                    update();
                    invalidate();
                    break;
                case 1:
                case 3:
                    if (this.mScrubbing) {
                        this.mListener.onScrubbingEnd(getScrubberTime(), 0, 0);
                        this.mScrubbing = false;
                        update();
                    }
                    break;
                case 2:
                    this.mScrubberLeft = x - this.mScrubberCorrection;
                    clampScrubber();
                    this.mCurrentTime = getScrubberTime();
                    this.mListener.onScrubbingMove(this.mCurrentTime);
                    update();
                    invalidate();
                    break;
            }
            return true;
        }
        return true;
    }

    protected String stringForTime(long j) {
        int i = ((int) j) / 1000;
        int i2 = i % 60;
        int i3 = (i / 60) % 60;
        int i4 = i / 3600;
        if (i4 > 0) {
            return String.format("%2d:%02d:%02d", Integer.valueOf(i4), Integer.valueOf(i3), Integer.valueOf(i2)).toString();
        }
        return String.format("%02d:%02d", Integer.valueOf(i3), Integer.valueOf(i2)).toString();
    }

    private void updateBounds() {
        String strStringForTime;
        int length;
        int i = this.mTotalTime > this.mCurrentTime ? this.mTotalTime : this.mCurrentTime;
        if (this.mLastShowTime == i || this.mLastLength == (length = (strStringForTime = stringForTime(i)).length())) {
            return;
        }
        this.mTimeTextPaint.getTextBounds(strStringForTime, 0, length, this.mTimeBounds);
        this.mLastShowTime = i;
        this.mLastLength = length;
        com.mediatek.gallery3d.util.Log.v("VP_TimeBar", "updateBounds() durationText=" + strStringForTime + ", timeBounds=" + this.mTimeBounds);
    }

    public void setScrubbing(boolean z) {
        com.mediatek.gallery3d.util.Log.v("VP_TimeBar", "setScrubbing(" + z + ") scrubbing=" + this.mScrubbing);
        this.mEnableScrubbing = z;
        if (this.mScrubbing) {
            this.mListener.onScrubbingEnd(getScrubberTime(), 0, 0);
            this.mScrubbing = false;
        }
    }

    public boolean getScrubbing() {
        com.mediatek.gallery3d.util.Log.v("VP_TimeBar", "mEnableScrubbing=" + this.mEnableScrubbing);
        return this.mEnableScrubbing;
    }

    public void setInfo(String str) {
        com.mediatek.gallery3d.util.Log.v("VP_TimeBar", "setInfo(" + str + ")");
        this.mInfoExt.setInfo(str);
        this.mInfoExt.updateVisibleText(this, this.mProgressBar, this.mTimeBounds);
        invalidate();
    }

    public void setSecondaryProgress(int i) {
        com.mediatek.gallery3d.util.Log.v("VP_TimeBar", "setSecondaryProgress(" + i + ")");
        this.mSecondaryProgressExt.setSecondaryProgress(this.mProgressBar, i);
        invalidate();
    }

    private class LayoutExt {
        private LayoutExt() {
        }

        public int getPreferredHeight(int i) {
            return (i - TimeBar.this.mVPaddingInPx) + (2 * TimeBar.this.mScrubberPadding) + TimeBar.this.mScrubber.getHeight();
        }

        public int getBarHeight(int i) {
            return (i - TimeBar.this.mVPaddingInPx) + (2 * TimeBar.this.mScrubberPadding) + TimeBar.this.mScrubber.getHeight();
        }

        public int getProgressMargin(int i) {
            return 0;
        }

        public int getProgressOffset() {
            return TimeBar.this.mTimeBounds.height() / 2;
        }

        public int getTimeOffset() {
            return (-TimeBar.this.mVPaddingInPx) / 2;
        }

        public Rect getInfoBounds(View view, Rect rect) {
            return new Rect(view.getPaddingLeft(), 0, view.getWidth() - view.getPaddingRight(), (rect.height() + TimeBar.this.mScrubberPadding) * 2);
        }
    }
}
