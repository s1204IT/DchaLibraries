package com.mediatek.camera.feature.setting.exposure;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.SeekBar;
import com.mediatek.camera.R;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.widget.RotateLayout;

public class ExposureView extends RotateLayout {
    private static int sMaxEv;
    private static int sMinEv;
    private boolean mEvChangeStartNotified;
    private SeekBar.OnSeekBarChangeListener mEvSeekBarChangedListener;
    private VerticalSeekBar mEvSeekbar;
    private int mLastEv;
    private int mLastProgress;
    private ExposureViewChangedListener mListener;
    private int mOrientation;
    private static final LogUtil.Tag TAG = new LogUtil.Tag(ExposureView.class.getSimpleName());
    private static int sDeProgress = 150;
    private static int sAvailableSpace = 1;

    protected interface ExposureViewChangedListener {
        void onExposureViewChanged(int i);

        void onTrackingTouchStatusChanged(boolean z);
    }

    public ExposureView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mLastEv = 0;
        this.mEvChangeStartNotified = false;
        this.mEvSeekBarChangedListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
                int i2;
                int iRound;
                if (ExposureView.this.mListener != null && i >= 0 && i <= seekBar.getMax() && (i2 = ExposureView.sMaxEv - ExposureView.sMinEv) != 0 && (iRound = Math.round((i + ((seekBar.getMax() * ExposureView.sMinEv) / i2)) / (seekBar.getMax() / i2))) != ExposureView.this.mLastEv) {
                    LogHelper.d(ExposureView.TAG, "[onProgressChanged] mLastProgress " + ExposureView.this.mLastEv + ",progress = " + iRound + ",sMaxEv = " + ExposureView.sMaxEv + ",sMinEv = " + ExposureView.sMinEv + ",max = " + seekBar.getMax());
                    ExposureView.this.mLastEv = iRound;
                    ExposureView.this.mListener.onExposureViewChanged(ExposureView.this.mLastEv);
                    if (!ExposureView.this.mEvChangeStartNotified && ExposureView.this.mEvSeekbar.getProgressDrawable().getAlpha() != 0) {
                        ExposureView.this.onEvChanged(true);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (ExposureView.this.mEvChangeStartNotified) {
                    ExposureView.this.onEvChanged(false);
                }
            }
        };
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mEvSeekbar = (VerticalSeekBar) findViewById(R.id.ev_seekbar);
        this.mEvSeekbar.setThumb(getResources().getDrawable(R.drawable.ic_ev_scrubber));
    }

    protected void setListener(ExposureViewChangedListener exposureViewChangedListener) {
        this.mListener = exposureViewChangedListener;
    }

    protected void initExposureView(int[] iArr) {
        int i = iArr[iArr.length - 1];
        int i2 = iArr[0];
        sMinEv = i;
        sMaxEv = i2;
        int i3 = (sMaxEv - sMinEv) * 50;
        this.mEvSeekbar.setMax(i3);
        sDeProgress = (50 * (sMaxEv - sMinEv)) / 2;
        sAvailableSpace = i3 * 10;
        resetExposureView();
        this.mEvSeekbar.setOnSeekBarChangeListener(this.mEvSeekBarChangedListener);
        LogHelper.d(TAG, "[initExposureView] sDeProgress " + sDeProgress + ",max = " + this.mEvSeekbar.getMax());
    }

    protected void resetExposureView() {
        this.mEvSeekbar.setProgress(sDeProgress);
        this.mEvSeekbar.getProgressDrawable().setAlpha(0);
    }

    protected void setViewEnabled(boolean z) {
        this.mEvSeekbar.setEnabled(z);
    }

    protected void setOrientation(int i) {
        this.mOrientation = i;
    }

    protected void onTrackingTouch(boolean z) {
        if (this.mEvSeekBarChangedListener == null) {
            return;
        }
        if (z) {
            this.mEvSeekBarChangedListener.onStartTrackingTouch(this.mEvSeekbar);
        } else {
            this.mEvSeekBarChangedListener.onStopTrackingTouch(this.mEvSeekbar);
        }
    }

    protected void onVerticalScroll(MotionEvent motionEvent, float f) {
        if (motionEvent.getPointerCount() == 1) {
            updateEvProgressbar(f);
        }
    }

    private void updateEvProgressbar(float f) {
        int iExtractDeltaScale = extractDeltaScale(f, this.mEvSeekbar);
        if (this.mLastProgress == iExtractDeltaScale) {
            return;
        }
        this.mLastProgress = iExtractDeltaScale;
        this.mEvSeekbar.setProgressDrawable(new ColorDrawable(-1));
        this.mEvSeekbar.setProgress(iExtractDeltaScale);
    }

    private int extractDeltaScale(float f, SeekBar seekBar) {
        int i = (int) f;
        float progress = seekBar.getProgress();
        int max = seekBar.getMax();
        if (this.mOrientation == 0 || this.mOrientation == 90) {
            progress += (i / sAvailableSpace) * max;
        }
        if (this.mOrientation == 180 || this.mOrientation == 270) {
            progress += ((-i) / sAvailableSpace) * max;
        }
        float f2 = max;
        if (progress <= f2) {
            f2 = progress < 0.0f ? 0.0f : progress;
        }
        return (int) f2;
    }

    private void onEvChanged(boolean z) {
        LogHelper.d(TAG, "[onEvChanged] " + z);
        this.mEvChangeStartNotified = z;
        if (z) {
            this.mListener.onTrackingTouchStatusChanged(true);
            this.mEvSeekbar.getProgressDrawable().setAlpha(1);
        } else {
            this.mListener.onTrackingTouchStatusChanged(false);
            this.mEvSeekbar.getProgressDrawable().setAlpha(0);
        }
    }
}
