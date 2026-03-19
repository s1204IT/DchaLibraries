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
    private boolean mIsScrubberingTrimEnd;
    private boolean mIsScrubberingTrimStart;
    private TrimLayoutExt mLayoutExt;
    private boolean mNeedUpdateTrimEnd;
    private boolean mNeedUpdateTrimStart;
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
        com.mediatek.gallery3d.util.Log.v("Gallery2/TrimTimeBar", "TrimTimeBar init");
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
        this.mLayoutExt = new TrimLayoutExt();
    }

    private int getBarPosFromTime(int i) {
        com.mediatek.gallery3d.util.Log.v("Gallery2/TrimTimeBar", "getBarPosFromTime time is " + i);
        return this.mProgressBar.left + ((int) ((((long) this.mProgressBar.width()) * ((long) i)) / ((long) this.mTotalTime)));
    }

    private int trimStartScrubberTipOffset() {
        return (this.mTrimStartScrubber.getWidth() * 3) / 4;
    }

    private int trimEndScrubberTipOffset() {
        return this.mTrimEndScrubber.getWidth() / 4;
    }

    private void updatePlayedBarAndScrubberFromTime() {
        com.mediatek.gallery3d.util.Log.v("Gallery2/TrimTimeBar", "updatePlayedBarAndScrubberFromTime(), mTrimStartTime = " + this.mTrimStartTime + ", mTrimEndTime = " + this.mTrimEndTime + ", mTotalTime = " + this.mTotalTime);
        int i = this.mPlayedBar.left;
        this.mPlayedBar.set(this.mProgressBar);
        if (this.mTotalTime > 0) {
            if (this.mIsScrubberingTrimStart || this.mNeedUpdateTrimStart) {
                this.mPlayedBar.left = getBarPosFromTime(this.mTrimStartTime);
            } else {
                this.mPlayedBar.left = i;
            }
            com.mediatek.gallery3d.util.Log.v("Gallery2/TrimTimeBar", "mIsScrubberingTrimStart = " + this.mIsScrubberingTrimStart + ", mIsScrubberingTrimEnd = " + this.mIsScrubberingTrimEnd + ", mNeedUpdateTrimStart = " + this.mNeedUpdateTrimStart + ", mNeedUpdateTrimEnd = " + this.mNeedUpdateTrimEnd + ", mPlayedBar.left = " + this.mPlayedBar.left);
            this.mPlayedBar.right = getBarPosFromTime(this.mCurrentTime);
            if (!this.mScrubbing) {
                this.mScrubberLeft = this.mPlayedBar.right - (this.mScrubber.getWidth() / 2);
                if (this.mIsScrubberingTrimStart || this.mNeedUpdateTrimStart) {
                    this.mTrimStartScrubberLeft = this.mPlayedBar.left - trimStartScrubberTipOffset();
                }
                if (this.mIsScrubberingTrimEnd || this.mNeedUpdateTrimStart) {
                    this.mTrimEndScrubberLeft = getBarPosFromTime(this.mTrimEndTime) - trimEndScrubberTipOffset();
                }
            }
            this.mNeedUpdateTrimStart = false;
            this.mNeedUpdateTrimEnd = false;
            com.mediatek.gallery3d.util.Log.v("Gallery2/TrimTimeBar", "mScrubbing = " + this.mScrubbing + ", mTrimStartScrubberLeft = " + this.mTrimStartScrubberLeft + ", mTrimEndScrubberLeft = " + this.mTrimEndScrubberLeft);
            return;
        }
        this.mPlayedBar.right = this.mProgressBar.left;
        this.mScrubberLeft = this.mProgressBar.left - (this.mScrubber.getWidth() / 2);
        this.mTrimStartScrubberLeft = this.mProgressBar.left - trimStartScrubberTipOffset();
        this.mTrimEndScrubberLeft = this.mProgressBar.right - trimEndScrubberTipOffset();
        com.mediatek.gallery3d.util.Log.v("Gallery2/TrimTimeBar", "mTrimStartScrubberLeft = " + this.mTrimStartScrubberLeft + ", mTrimEndScrubberLeft = " + this.mTrimEndScrubberLeft);
    }

    private void initTrimTimeIfNeeded() {
        if (this.mTotalTime > 0 && this.mTrimEndTime == 0) {
            this.mTrimEndTime = this.mTotalTime;
        }
    }

    private void update() {
        com.mediatek.gallery3d.util.Log.v("Gallery2/TrimTimeBar", "update()");
        initTrimTimeIfNeeded();
        updatePlayedBarAndScrubberFromTime();
        invalidate();
    }

    @Override
    public void setTime(int i, int i2, int i3, int i4) {
        com.mediatek.gallery3d.util.Log.v("Gallery2/TrimTimeBar", "setTime() currentTime " + i + ", totalTime " + i2 + ", trimStartTime " + i3 + ", trimEndTime " + i4);
        if (this.mCurrentTime == i && this.mTotalTime == i2 && this.mTrimStartTime == i3 && this.mTrimEndTime == i4) {
            return;
        }
        this.mCurrentTime = i;
        this.mTotalTime = i2;
        if (this.mTrimStartTime != i3) {
            this.mNeedUpdateTrimStart = true;
        }
        if (this.mTrimEndTime != i4) {
            this.mNeedUpdateTrimEnd = true;
        }
        this.mTrimStartTime = i3;
        this.mTrimEndTime = i4;
        update();
    }

    private int whichScrubber(float f, float f2) {
        if (inScrubber(f, f2, this.mTrimStartScrubberLeft, this.mTrimStartScrubberTop, this.mTrimStartScrubber)) {
            return 1;
        }
        if (inScrubber(f, f2, this.mTrimEndScrubberLeft, this.mTrimEndScrubberTop, this.mTrimEndScrubber)) {
            return 3;
        }
        if (inScrubber(f, f2, this.mScrubberLeft, this.mScrubberTop, this.mScrubber)) {
            return 2;
        }
        return 0;
    }

    private boolean inScrubber(float f, float f2, int i, int i2, Bitmap bitmap) {
        return ((float) i) < f && f < ((float) (bitmap.getWidth() + i)) && ((float) i2) < f2 && f2 < ((float) (bitmap.getHeight() + i2));
    }

    private int clampScrubber(int i, int i2, int i3, int i4) {
        return Math.min(i4 - i2, Math.max(i3 - i2, i));
    }

    private int getScrubberTime(int i, int i2) {
        return (int) ((((long) ((i + i2) - this.mProgressBar.left)) * ((long) this.mTotalTime)) / ((long) this.mProgressBar.width()));
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        com.mediatek.gallery3d.util.Log.v("Gallery2/TrimTimeBar", "onLayout()");
        int i5 = i3 - i;
        int i6 = i4 - i2;
        if (!this.mShowTimes && !this.mShowScrubber) {
            this.mProgressBar.set(0, 0, i5, i6);
        } else {
            int width = this.mScrubber.getWidth() / 3;
            if (this.mShowTimes) {
                width += this.mTimeBounds.width();
            }
            int progressOffset = (i6 / 4) + this.mLayoutExt.getProgressOffset(i6);
            this.mScrubberTop = (progressOffset - (this.mScrubber.getHeight() / 2)) + 1;
            this.mTrimStartScrubberTop = progressOffset;
            this.mTrimEndScrubberTop = progressOffset;
            this.mProgressBar.set(getPaddingLeft() + width, progressOffset, (i5 - getPaddingRight()) - width, progressOffset + 4);
        }
        this.mNeedUpdateTrimStart = true;
        this.mNeedUpdateTrimEnd = true;
        update();
    }

    @Override
    public int getPreferredHeight() {
        com.mediatek.gallery3d.util.Log.d("Gallery2/TrimTimeBar", "getPreferredHeight mTrimStartScrubber " + this.mTrimStartScrubber.getHeight());
        return this.mLayoutExt.getPreferredHeight(super.getPreferredHeight());
    }

    @Override
    public int getBarHeight() {
        return getPreferredHeight();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        com.mediatek.gallery3d.util.Log.v("Gallery2/TrimTimeBar", "onDraw()");
        canvas.drawRect(this.mProgressBar, this.mProgressPaint);
        canvas.drawRect(this.mPlayedBar, this.mPlayedPaint);
        if (this.mShowTimes) {
            canvas.drawText(stringForTime(this.mCurrentTime), (this.mTimeBounds.width() / 2) + (getPaddingLeft() * 3), (this.mTimeBounds.height() / 2) + this.mTrimStartScrubberTop + this.mLayoutExt.getTimeOffset(), this.mTimeTextPaint);
            canvas.drawText(stringForTime(this.mTotalTime), (getWidth() - (getPaddingRight() * 3)) - (this.mTimeBounds.width() / 2), (this.mTimeBounds.height() / 2) + this.mTrimStartScrubberTop + this.mLayoutExt.getTimeOffset(), this.mTimeTextPaint);
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
    public boolean onTouchEvent(MotionEvent motionEvent) {
        int scrubberTime;
        int scrubberTime2;
        com.mediatek.gallery3d.util.Log.v("Gallery2/TrimTimeBar", "onTouchEvent() mTotalTime = " + this.mTotalTime);
        if (this.mShowScrubber && this.mTotalTime > 0) {
            int x = (int) motionEvent.getX();
            int y = (int) motionEvent.getY();
            switch (motionEvent.getAction()) {
                case 0:
                    this.mPressedThumb = whichScrubber(x, y);
                    switch (this.mPressedThumb) {
                        case 1:
                            this.mIsScrubberingTrimStart = true;
                            this.mScrubbing = true;
                            this.mScrubberCorrection = x - this.mTrimStartScrubberLeft;
                            break;
                        case 2:
                            this.mScrubbing = true;
                            this.mScrubberCorrection = x - this.mScrubberLeft;
                            break;
                        case 3:
                            this.mIsScrubberingTrimEnd = true;
                            this.mScrubbing = true;
                            this.mScrubberCorrection = x - this.mTrimEndScrubberLeft;
                            break;
                    }
                    if (this.mScrubbing) {
                        this.mListener.onScrubbingStart();
                        return true;
                    }
                    break;
                case 1:
                case 3:
                    if (this.mScrubbing) {
                        switch (this.mPressedThumb) {
                            case 1:
                                scrubberTime = getScrubberTime(this.mTrimStartScrubberLeft, trimStartScrubberTipOffset());
                                this.mScrubberLeft = (this.mTrimStartScrubberLeft + trimStartScrubberTipOffset()) - (this.mScrubber.getWidth() / 2);
                                break;
                            case 2:
                                scrubberTime = getScrubberTime(this.mScrubberLeft, this.mScrubber.getWidth() / 2);
                                break;
                            case 3:
                                scrubberTime = getScrubberTime(this.mTrimEndScrubberLeft, trimEndScrubberTipOffset());
                                this.mScrubberLeft = (this.mTrimEndScrubberLeft + trimEndScrubberTipOffset()) - (this.mScrubber.getWidth() / 2);
                                break;
                            default:
                                scrubberTime = 0;
                                break;
                        }
                        updateTimeFromPos();
                        this.mListener.onScrubbingEnd(scrubberTime, getScrubberTime(this.mTrimStartScrubberLeft, trimStartScrubberTipOffset()), getScrubberTime(this.mTrimEndScrubberLeft, trimEndScrubberTipOffset()));
                        this.mScrubbing = false;
                        this.mIsScrubberingTrimStart = false;
                        this.mIsScrubberingTrimEnd = false;
                        this.mPressedThumb = 0;
                        update();
                        this.mIsScrubberingTrimStart = false;
                        return true;
                    }
                    break;
                case 2:
                    if (this.mScrubbing) {
                        int iTrimStartScrubberTipOffset = this.mTrimStartScrubberLeft + trimStartScrubberTipOffset();
                        int iTrimEndScrubberTipOffset = this.mTrimEndScrubberLeft + trimEndScrubberTipOffset();
                        switch (this.mPressedThumb) {
                            case 1:
                                this.mTrimStartScrubberLeft = x - this.mScrubberCorrection;
                                if (this.mTrimStartScrubberLeft > this.mTrimEndScrubberLeft) {
                                    this.mTrimStartScrubberLeft = this.mTrimEndScrubberLeft;
                                }
                                this.mTrimStartScrubberLeft = clampScrubber(this.mTrimStartScrubberLeft, trimStartScrubberTipOffset(), this.mProgressBar.left, iTrimEndScrubberTipOffset);
                                scrubberTime2 = getScrubberTime(this.mTrimStartScrubberLeft, trimStartScrubberTipOffset());
                                break;
                            case 2:
                                this.mScrubberLeft = x - this.mScrubberCorrection;
                                this.mScrubberLeft = clampScrubber(this.mScrubberLeft, this.mScrubber.getWidth() / 2, iTrimStartScrubberTipOffset, iTrimEndScrubberTipOffset);
                                scrubberTime2 = getScrubberTime(this.mScrubberLeft, this.mScrubber.getWidth() / 2);
                                break;
                            case 3:
                                this.mTrimEndScrubberLeft = x - this.mScrubberCorrection;
                                this.mTrimEndScrubberLeft = clampScrubber(this.mTrimEndScrubberLeft, trimEndScrubberTipOffset(), iTrimStartScrubberTipOffset, this.mProgressBar.right);
                                scrubberTime2 = getScrubberTime(this.mTrimEndScrubberLeft, trimEndScrubberTipOffset());
                                break;
                            default:
                                scrubberTime2 = -1;
                                break;
                        }
                        updateTimeFromPos();
                        updatePlayedBarAndScrubberFromTime();
                        if (scrubberTime2 != -1) {
                            this.mListener.onScrubbingMove(scrubberTime2);
                        }
                        invalidate();
                        return true;
                    }
                    break;
            }
        }
        return false;
    }

    private class TrimLayoutExt {
        private TrimLayoutExt() {
        }

        public int getPreferredHeight(int i) {
            return i + TrimTimeBar.this.mTrimStartScrubber.getHeight();
        }

        public int getProgressOffset(int i) {
            return (TrimTimeBar.this.mTimeBounds.height() / 2) + (i / 4);
        }

        public int getTimeOffset() {
            return (-TrimTimeBar.this.mVPaddingInPx) / 2;
        }
    }
}
