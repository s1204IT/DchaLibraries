package com.android.internal.widget;

import android.content.Context;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.RemotableViewMethod;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.RemoteViews;

@RemoteViews.RemoteView
public class TextProgressBar extends RelativeLayout implements Chronometer.OnChronometerTickListener {
    static final int CHRONOMETER_ID = 16908308;
    static final int PROGRESSBAR_ID = 16908301;
    public static final String TAG = "TextProgressBar";
    Chronometer mChronometer;
    boolean mChronometerFollow;
    int mChronometerGravity;
    int mDuration;
    long mDurationBase;
    ProgressBar mProgressBar;

    public TextProgressBar(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mChronometer = null;
        this.mProgressBar = null;
        this.mDurationBase = -1L;
        this.mDuration = -1;
        this.mChronometerFollow = false;
        this.mChronometerGravity = 0;
    }

    public TextProgressBar(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mChronometer = null;
        this.mProgressBar = null;
        this.mDurationBase = -1L;
        this.mDuration = -1;
        this.mChronometerFollow = false;
        this.mChronometerGravity = 0;
    }

    public TextProgressBar(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mChronometer = null;
        this.mProgressBar = null;
        this.mDurationBase = -1L;
        this.mDuration = -1;
        this.mChronometerFollow = false;
        this.mChronometerGravity = 0;
    }

    public TextProgressBar(Context context) {
        super(context);
        this.mChronometer = null;
        this.mProgressBar = null;
        this.mDurationBase = -1L;
        this.mDuration = -1;
        this.mChronometerFollow = false;
        this.mChronometerGravity = 0;
    }

    @Override
    public void addView(View view, int i, ViewGroup.LayoutParams layoutParams) {
        super.addView(view, i, layoutParams);
        int id = view.getId();
        if (id == 16908308 && (view instanceof Chronometer)) {
            this.mChronometer = (Chronometer) view;
            this.mChronometer.setOnChronometerTickListener(this);
            this.mChronometerFollow = layoutParams.width == -2;
            this.mChronometerGravity = this.mChronometer.getGravity() & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK;
            return;
        }
        if (id == 16908301 && (view instanceof ProgressBar)) {
            this.mProgressBar = (ProgressBar) view;
        }
    }

    @RemotableViewMethod
    public void setDurationBase(long j) {
        this.mDurationBase = j;
        if (this.mProgressBar == null || this.mChronometer == null) {
            throw new RuntimeException("Expecting child ProgressBar with id 'android.R.id.progress' and Chronometer id 'android.R.id.text1'");
        }
        this.mDuration = (int) (j - this.mChronometer.getBase());
        if (this.mDuration <= 0) {
            this.mDuration = 1;
        }
        this.mProgressBar.setMax(this.mDuration);
    }

    @Override
    public void onChronometerTick(Chronometer chronometer) {
        if (this.mProgressBar == null) {
            throw new RuntimeException("Expecting child ProgressBar with id 'android.R.id.progress'");
        }
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        if (jElapsedRealtime >= this.mDurationBase) {
            this.mChronometer.stop();
        }
        this.mProgressBar.setProgress(this.mDuration - ((int) (this.mDurationBase - jElapsedRealtime)));
        if (this.mChronometerFollow) {
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) this.mProgressBar.getLayoutParams();
            int width = this.mProgressBar.getWidth() - (layoutParams.leftMargin + layoutParams.rightMargin);
            int progress = ((this.mProgressBar.getProgress() * width) / this.mProgressBar.getMax()) + layoutParams.leftMargin;
            int i = 0;
            int width2 = this.mChronometer.getWidth();
            if (this.mChronometerGravity == 8388613) {
                i = -width2;
            } else if (this.mChronometerGravity == 1) {
                i = -(width2 / 2);
            }
            int i2 = progress + i;
            int i3 = (width - layoutParams.rightMargin) - width2;
            if (i2 < layoutParams.leftMargin) {
                i2 = layoutParams.leftMargin;
            } else if (i2 > i3) {
                i2 = i3;
            }
            ((RelativeLayout.LayoutParams) this.mChronometer.getLayoutParams()).leftMargin = i2;
            this.mChronometer.requestLayout();
        }
    }
}
