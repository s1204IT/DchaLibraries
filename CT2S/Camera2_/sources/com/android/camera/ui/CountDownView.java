package com.android.camera.ui;

import android.content.Context;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.android.camera.debug.Log;
import com.android.camera2.R;
import java.util.Locale;

public class CountDownView extends FrameLayout {
    private static final long ANIMATION_DURATION_MS = 800;
    private static final int SET_TIMER_TEXT = 1;
    private static final Log.Tag TAG = new Log.Tag("CountDownView");
    private final Handler mHandler;
    private OnCountDownStatusListener mListener;
    private final RectF mPreviewArea;
    private TextView mRemainingSecondsView;
    private int mRemainingSecs;

    public interface OnCountDownStatusListener {
        void onCountDownFinished();

        void onRemainingSecondsChanged(int i);
    }

    public CountDownView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mRemainingSecs = 0;
        this.mHandler = new MainHandler();
        this.mPreviewArea = new RectF();
    }

    public boolean isCountingDown() {
        return this.mRemainingSecs > 0;
    }

    public void onPreviewAreaChanged(RectF previewArea) {
        this.mPreviewArea.set(previewArea);
    }

    private void remainingSecondsChanged(int newVal) {
        this.mRemainingSecs = newVal;
        if (this.mListener != null) {
            this.mListener.onRemainingSecondsChanged(this.mRemainingSecs);
        }
        if (newVal == 0) {
            setVisibility(4);
            if (this.mListener != null) {
                this.mListener.onCountDownFinished();
                return;
            }
            return;
        }
        Locale locale = getResources().getConfiguration().locale;
        String localizedValue = String.format(locale, "%d", Integer.valueOf(newVal));
        this.mRemainingSecondsView.setText(localizedValue);
        startFadeOutAnimation();
        this.mHandler.sendEmptyMessageDelayed(1, 1000L);
    }

    private void startFadeOutAnimation() {
        int textWidth = this.mRemainingSecondsView.getMeasuredWidth();
        int textHeight = this.mRemainingSecondsView.getMeasuredHeight();
        this.mRemainingSecondsView.setScaleX(1.0f);
        this.mRemainingSecondsView.setScaleY(1.0f);
        this.mRemainingSecondsView.setTranslationX(this.mPreviewArea.centerX() - (textWidth / 2));
        this.mRemainingSecondsView.setTranslationY(this.mPreviewArea.centerY() - (textHeight / 2));
        this.mRemainingSecondsView.setPivotX(textWidth / 2);
        this.mRemainingSecondsView.setPivotY(textHeight / 2);
        this.mRemainingSecondsView.setAlpha(1.0f);
        this.mRemainingSecondsView.animate().scaleX(2.5f).scaleY(2.5f).alpha(0.0f).setDuration(ANIMATION_DURATION_MS).start();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mRemainingSecondsView = (TextView) findViewById(R.id.remaining_seconds);
    }

    public void setCountDownStatusListener(OnCountDownStatusListener listener) {
        this.mListener = listener;
    }

    public void startCountDown(int sec) {
        if (sec <= 0) {
            Log.w(TAG, "Invalid input for countdown timer: " + sec + " seconds");
            return;
        }
        if (isCountingDown()) {
            cancelCountDown();
        }
        setVisibility(0);
        remainingSecondsChanged(sec);
    }

    public void cancelCountDown() {
        if (this.mRemainingSecs > 0) {
            this.mRemainingSecs = 0;
            this.mHandler.removeMessages(1);
            setVisibility(4);
        }
    }

    private class MainHandler extends Handler {
        private MainHandler() {
        }

        @Override
        public void handleMessage(Message message) {
            if (message.what == 1) {
                CountDownView.this.remainingSecondsChanged(CountDownView.this.mRemainingSecs - 1);
            }
        }
    }
}
