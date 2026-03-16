package com.android.deskclock.timer;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.android.deskclock.CircleTimerView;
import com.android.deskclock.R;

public class TimerListItem extends LinearLayout {
    CircleTimerView mCircleView;
    ImageView mResetAddButton;
    long mTimerLength;
    CountingTimerView mTimerText;

    public TimerListItem(Context context) {
        this(context, null);
    }

    public TimerListItem(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mTimerText = (CountingTimerView) findViewById(R.id.timer_time_text);
        this.mCircleView = (CircleTimerView) findViewById(R.id.timer_time);
        this.mResetAddButton = (ImageView) findViewById(R.id.reset_add);
        this.mCircleView.setTimerMode(true);
    }

    public void set(long timerLength, long timeLeft, boolean drawRed) {
        if (this.mCircleView == null) {
            this.mCircleView = (CircleTimerView) findViewById(R.id.timer_time);
            this.mCircleView.setTimerMode(true);
        }
        this.mTimerLength = timerLength;
        this.mCircleView.setIntervalTime(this.mTimerLength);
        this.mCircleView.setPassedTime(timerLength - timeLeft, drawRed);
        invalidate();
    }

    public void start() {
        this.mResetAddButton.setImageResource(R.drawable.ic_plusone);
        this.mResetAddButton.setContentDescription(getResources().getString(R.string.timer_plus_one));
        this.mCircleView.startIntervalAnimation();
        this.mTimerText.setTimeStrTextColor(false, true);
        this.mTimerText.showTime(true);
        this.mCircleView.setVisibility(0);
    }

    public void pause() {
        this.mResetAddButton.setImageResource(R.drawable.ic_reset);
        this.mResetAddButton.setContentDescription(getResources().getString(R.string.timer_reset));
        this.mCircleView.pauseIntervalAnimation();
        this.mTimerText.setTimeStrTextColor(false, true);
        this.mTimerText.showTime(true);
        this.mCircleView.setVisibility(0);
    }

    public void stop() {
        this.mCircleView.stopIntervalAnimation();
        this.mTimerText.setTimeStrTextColor(false, true);
        this.mTimerText.showTime(true);
        this.mCircleView.setVisibility(0);
    }

    public void timesUp() {
        this.mCircleView.abortIntervalAnimation();
        this.mTimerText.setTimeStrTextColor(true, true);
    }

    public void done() {
        this.mCircleView.stopIntervalAnimation();
        this.mCircleView.setVisibility(0);
        this.mCircleView.invalidate();
        this.mTimerText.setTimeStrTextColor(true, false);
    }

    public void setLength(long timerLength) {
        this.mTimerLength = timerLength;
        this.mCircleView.setIntervalTime(this.mTimerLength);
        this.mCircleView.invalidate();
    }

    public void setTextBlink(boolean blink) {
        this.mTimerText.showTime(!blink);
    }

    public void setCircleBlink(boolean blink) {
        this.mCircleView.setVisibility(blink ? 4 : 0);
    }

    public void setResetAddButton(boolean isRunning, View.OnClickListener listener) {
        if (this.mResetAddButton == null) {
            this.mResetAddButton = (ImageView) findViewById(R.id.reset_add);
        }
        this.mResetAddButton.setImageResource(isRunning ? R.drawable.ic_plusone : R.drawable.ic_reset);
        this.mResetAddButton.setContentDescription(getResources().getString(isRunning ? R.string.timer_plus_one : R.string.timer_reset));
        this.mResetAddButton.setOnClickListener(listener);
    }

    public void setTime(long time, boolean forceUpdate) {
        if (this.mTimerText == null) {
            this.mTimerText = (CountingTimerView) findViewById(R.id.timer_time_text);
        }
        this.mTimerText.setTime(time, false, forceUpdate);
    }

    public void setAnimatedHeight(int height) {
        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        if (layoutParams != null) {
            layoutParams.height = height;
            requestLayout();
        }
    }
}
