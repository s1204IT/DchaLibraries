package com.android.gallery3d.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import com.android.gallery3d.R;
import com.android.gallery3d.app.TimeBar;

public class TrimTimeBar extends TimeBar {
    private int mPressedThumb;
    private final Bitmap mTrimEndScrubber;
    private int mTrimEndScrubberLeft;
    private int mTrimEndScrubberTop;
    private int mTrimEndTime;
    private final Bitmap mTrimStartScrubber;
    private int mTrimStartScrubberLeft;
    private int mTrimStartScrubberTop;
    private int mTrimStartTime;

    public TrimTimeBar(Context context, TimeBar.Listener listener) {
        super(context, listener);
        this.mPressedThumb = 0;
        this.mTrimStartTime = 0;
        this.mTrimEndTime = 0;
        this.mTrimStartScrubberLeft = 0;
        this.mTrimEndScrubberLeft = 0;
        this.mTrimStartScrubberTop = 0;
        this.mTrimEndScrubberTop = 0;
        this.mTrimStartScrubber = BitmapFactory.decodeResource(getResources(), R.drawable.text_select_handle_left);
        this.mTrimEndScrubber = BitmapFactory.decodeResource(getResources(), R.drawable.text_select_handle_right);
        this.mScrubberPadding = 0;
        this.mVPaddingInPx = (this.mVPaddingInPx * 3) / 2;
    }

    private int getBarPosFromTime(int time) {
        return this.mProgressBar.left + ((int) ((((long) this.mProgressBar.width()) * ((long) time)) / ((long) this.mTotalTime)));
    }

    private int trimStartScrubberTipOffset() {
        return (this.mTrimStartScrubber.getWidth() * 3) / 4;
    }

    private int trimEndScrubberTipOffset() {
        return this.mTrimEndScrubber.getWidth() / 4;
    }

    private void updatePlayedBarAndScrubberFromTime() {
        this.mPlayedBar.set(this.mProgressBar);
        if (this.mTotalTime > 0) {
            this.mPlayedBar.left = getBarPosFromTime(this.mTrimStartTime);
            this.mPlayedBar.right = getBarPosFromTime(this.mCurrentTime);
            if (!this.mScrubbing) {
                this.mScrubberLeft = this.mPlayedBar.right - (this.mScrubber.getWidth() / 2);
                this.mTrimStartScrubberLeft = this.mPlayedBar.left - trimStartScrubberTipOffset();
                this.mTrimEndScrubberLeft = getBarPosFromTime(this.mTrimEndTime) - trimEndScrubberTipOffset();
                return;
            }
            return;
        }
        this.mPlayedBar.right = this.mProgressBar.left;
        this.mScrubberLeft = this.mProgressBar.left - (this.mScrubber.getWidth() / 2);
        this.mTrimStartScrubberLeft = this.mProgressBar.left - trimStartScrubberTipOffset();
        this.mTrimEndScrubberLeft = this.mProgressBar.right - trimEndScrubberTipOffset();
    }

    private void initTrimTimeIfNeeded() {
        if (this.mTotalTime > 0 && this.mTrimEndTime == 0) {
            this.mTrimEndTime = this.mTotalTime;
        }
    }

    private void update() {
        initTrimTimeIfNeeded();
        updatePlayedBarAndScrubberFromTime();
        invalidate();
    }

    @Override
    public void setTime(int currentTime, int totalTime, int trimStartTime, int trimEndTime) {
        if (this.mCurrentTime != currentTime || this.mTotalTime != totalTime || this.mTrimStartTime != trimStartTime || this.mTrimEndTime != trimEndTime) {
            this.mCurrentTime = currentTime;
            this.mTotalTime = totalTime;
            this.mTrimStartTime = trimStartTime;
            this.mTrimEndTime = trimEndTime;
            update();
        }
    }

    private int whichScrubber(float x, float y) {
        if (inScrubber(x, y, this.mTrimStartScrubberLeft, this.mTrimStartScrubberTop, this.mTrimStartScrubber)) {
            return 1;
        }
        if (inScrubber(x, y, this.mTrimEndScrubberLeft, this.mTrimEndScrubberTop, this.mTrimEndScrubber)) {
            return 3;
        }
        if (inScrubber(x, y, this.mScrubberLeft, this.mScrubberTop, this.mScrubber)) {
            return 2;
        }
        return 0;
    }

    private boolean inScrubber(float x, float y, int startX, int startY, Bitmap scrubber) {
        int scrubberRight = startX + scrubber.getWidth();
        int scrubberBottom = startY + scrubber.getHeight();
        return ((float) startX) < x && x < ((float) scrubberRight) && ((float) startY) < y && y < ((float) scrubberBottom);
    }

    private int clampScrubber(int scrubberLeft, int offset, int lowerBound, int upperBound) {
        int max = upperBound - offset;
        int min = lowerBound - offset;
        return Math.min(max, Math.max(min, scrubberLeft));
    }

    private int getScrubberTime(int scrubberLeft, int offset) {
        return (int) ((((long) ((scrubberLeft + offset) - this.mProgressBar.left)) * ((long) this.mTotalTime)) / ((long) this.mProgressBar.width()));
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
            int progressY = h / 4;
            int scrubberY = (progressY - (this.mScrubber.getHeight() / 2)) + 1;
            this.mScrubberTop = scrubberY;
            this.mTrimStartScrubberTop = progressY;
            this.mTrimEndScrubberTop = progressY;
            this.mProgressBar.set(getPaddingLeft() + margin, progressY, (w - getPaddingRight()) - margin, progressY + 4);
        }
        update();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawRect(this.mProgressBar, this.mProgressPaint);
        canvas.drawRect(this.mPlayedBar, this.mPlayedPaint);
        if (this.mShowTimes) {
            canvas.drawText(stringForTime(this.mCurrentTime), (this.mTimeBounds.width() / 2) + getPaddingLeft(), (this.mTimeBounds.height() / 2) + this.mTrimStartScrubberTop, this.mTimeTextPaint);
            canvas.drawText(stringForTime(this.mTotalTime), (getWidth() - getPaddingRight()) - (this.mTimeBounds.width() / 2), (this.mTimeBounds.height() / 2) + this.mTrimStartScrubberTop, this.mTimeTextPaint);
        }
        if (this.mShowScrubber) {
            canvas.drawBitmap(this.mScrubber, this.mScrubberLeft, this.mScrubberTop, (Paint) null);
            canvas.drawBitmap(this.mTrimStartScrubber, this.mTrimStartScrubberLeft, this.mTrimStartScrubberTop, (Paint) null);
            canvas.drawBitmap(this.mTrimEndScrubber, this.mTrimEndScrubberLeft, this.mTrimEndScrubberTop, (Paint) null);
        }
    }

    private void updateTimeFromPos() {
        this.mCurrentTime = getScrubberTime(this.mScrubberLeft, this.mScrubber.getWidth() / 2);
        this.mTrimStartTime = getScrubberTime(this.mTrimStartScrubberLeft, trimStartScrubberTipOffset());
        this.mTrimEndTime = getScrubberTime(this.mTrimEndScrubberLeft, trimEndScrubberTipOffset());
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
                this.mPressedThumb = whichScrubber(x, y);
                switch (this.mPressedThumb) {
                    case 1:
                        this.mScrubbing = true;
                        this.mScrubberCorrection = x - this.mTrimStartScrubberLeft;
                        break;
                    case 2:
                        this.mScrubbing = true;
                        this.mScrubberCorrection = x - this.mScrubberLeft;
                        break;
                    case 3:
                        this.mScrubbing = true;
                        this.mScrubberCorrection = x - this.mTrimEndScrubberLeft;
                        break;
                }
                if (!this.mScrubbing) {
                    return false;
                }
                this.mListener.onScrubbingStart();
                return true;
            case 1:
            case 3:
                if (!this.mScrubbing) {
                    return false;
                }
                int seekToTime = 0;
                switch (this.mPressedThumb) {
                    case 1:
                        seekToTime = getScrubberTime(this.mTrimStartScrubberLeft, trimStartScrubberTipOffset());
                        this.mScrubberLeft = (this.mTrimStartScrubberLeft + trimStartScrubberTipOffset()) - (this.mScrubber.getWidth() / 2);
                        break;
                    case 2:
                        seekToTime = getScrubberTime(this.mScrubberLeft, this.mScrubber.getWidth() / 2);
                        break;
                    case 3:
                        seekToTime = getScrubberTime(this.mTrimEndScrubberLeft, trimEndScrubberTipOffset());
                        this.mScrubberLeft = (this.mTrimEndScrubberLeft + trimEndScrubberTipOffset()) - (this.mScrubber.getWidth() / 2);
                        break;
                }
                updateTimeFromPos();
                this.mListener.onScrubbingEnd(seekToTime, getScrubberTime(this.mTrimStartScrubberLeft, trimStartScrubberTipOffset()), getScrubberTime(this.mTrimEndScrubberLeft, trimEndScrubberTipOffset()));
                this.mScrubbing = false;
                this.mPressedThumb = 0;
                return true;
            case 2:
                if (!this.mScrubbing) {
                    return false;
                }
                int seekToTime2 = -1;
                int lowerBound = this.mTrimStartScrubberLeft + trimStartScrubberTipOffset();
                int upperBound = this.mTrimEndScrubberLeft + trimEndScrubberTipOffset();
                switch (this.mPressedThumb) {
                    case 1:
                        this.mTrimStartScrubberLeft = x - this.mScrubberCorrection;
                        if (this.mTrimStartScrubberLeft > this.mTrimEndScrubberLeft) {
                            this.mTrimStartScrubberLeft = this.mTrimEndScrubberLeft;
                        }
                        this.mTrimStartScrubberLeft = clampScrubber(this.mTrimStartScrubberLeft, trimStartScrubberTipOffset(), this.mProgressBar.left, upperBound);
                        seekToTime2 = getScrubberTime(this.mTrimStartScrubberLeft, trimStartScrubberTipOffset());
                        break;
                    case 2:
                        this.mScrubberLeft = x - this.mScrubberCorrection;
                        this.mScrubberLeft = clampScrubber(this.mScrubberLeft, this.mScrubber.getWidth() / 2, lowerBound, upperBound);
                        seekToTime2 = getScrubberTime(this.mScrubberLeft, this.mScrubber.getWidth() / 2);
                        break;
                    case 3:
                        this.mTrimEndScrubberLeft = x - this.mScrubberCorrection;
                        this.mTrimEndScrubberLeft = clampScrubber(this.mTrimEndScrubberLeft, trimEndScrubberTipOffset(), lowerBound, this.mProgressBar.right);
                        seekToTime2 = getScrubberTime(this.mTrimEndScrubberLeft, trimEndScrubberTipOffset());
                        break;
                }
                updateTimeFromPos();
                updatePlayedBarAndScrubberFromTime();
                if (seekToTime2 != -1) {
                    this.mListener.onScrubbingMove(seekToTime2);
                }
                invalidate();
                return true;
            default:
                return false;
        }
    }
}
