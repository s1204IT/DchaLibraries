package com.android.camera.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.SystemClock;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import com.android.camera.debug.Log;
import com.android.camera.ui.PreviewStatusListener;
import com.android.camera2.R;
import java.util.List;

public class PreviewOverlay extends View implements PreviewStatusListener.PreviewAreaChangedListener {
    private static final Log.Tag TAG = new Log.Tag("PreviewOverlay");
    private static final long ZOOM_MINIMUM_WAIT_MILLIS = 33;
    public static final float ZOOM_MIN_RATIO = 1.0f;
    private long mDelayZoomCallUntilMillis;
    private GestureDetector mGestureDetector;
    private OnPreviewTouchedListener mOnPreviewTouchedListener;
    private final ZoomGestureDetector mScaleDetector;
    private View.OnTouchListener mTouchListener;
    private OnZoomChangedListener mZoomListener;
    private final ZoomProcessor mZoomProcessor;

    public interface OnPreviewTouchedListener {
        void onPreviewTouched(MotionEvent motionEvent);
    }

    public interface OnZoomChangedListener {
        void onZoomEnd();

        void onZoomStart();

        void onZoomValueChanged(float f);
    }

    public PreviewOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mDelayZoomCallUntilMillis = 0L;
        this.mZoomProcessor = new ZoomProcessor();
        this.mGestureDetector = null;
        this.mTouchListener = null;
        this.mZoomListener = null;
        this.mScaleDetector = new ZoomGestureDetector();
    }

    public void setupZoom(float zoomMaxRatio, float zoom, OnZoomChangedListener zoomChangeListener) {
        this.mZoomListener = zoomChangeListener;
        this.mZoomProcessor.setupZoom(zoomMaxRatio, zoom);
    }

    @Override
    public boolean onTouchEvent(MotionEvent m) {
        if (this.mGestureDetector != null) {
            this.mGestureDetector.onTouchEvent(m);
        }
        if (this.mTouchListener != null) {
            this.mTouchListener.onTouch(this, m);
        }
        this.mScaleDetector.onTouchEvent(m);
        if (this.mOnPreviewTouchedListener != null) {
            this.mOnPreviewTouchedListener.onPreviewTouched(m);
            return true;
        }
        return true;
    }

    public void setOnPreviewTouchedListener(OnPreviewTouchedListener listener) {
        this.mOnPreviewTouchedListener = listener;
    }

    @Override
    public void onPreviewAreaChanged(RectF previewArea) {
        this.mZoomProcessor.layout((int) previewArea.left, (int) previewArea.top, (int) previewArea.right, (int) previewArea.bottom);
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        this.mZoomProcessor.draw(canvas);
    }

    public void setGestureListener(GestureDetector.OnGestureListener gestureListener) {
        if (gestureListener != null) {
            this.mGestureDetector = new GestureDetector(getContext(), gestureListener);
        }
    }

    public void setTouchListener(View.OnTouchListener touchListener) {
        this.mTouchListener = touchListener;
    }

    public void reset() {
        this.mZoomListener = null;
        this.mGestureDetector = null;
        this.mTouchListener = null;
    }

    private class ZoomGestureDetector extends ScaleGestureDetector {
        private float mDeltaX;
        private float mDeltaY;

        public ZoomGestureDetector() {
            super(PreviewOverlay.this.getContext(), PreviewOverlay.this.mZoomProcessor);
        }

        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            if (PreviewOverlay.this.mZoomListener == null) {
                return false;
            }
            boolean zOnTouchEvent = super.onTouchEvent(ev);
            if (ev.getPointerCount() > 1) {
                this.mDeltaX = ev.getX(1) - ev.getX(0);
                this.mDeltaY = ev.getY(1) - ev.getY(0);
                return zOnTouchEvent;
            }
            return zOnTouchEvent;
        }

        public float getAngle() {
            return (float) Math.atan2(-this.mDeltaY, this.mDeltaX);
        }
    }

    private class ZoomProcessor implements ScaleGestureDetector.OnScaleGestureListener {
        private static final float ZOOM_UI_DONUT = 0.25f;
        private static final float ZOOM_UI_SIZE = 0.8f;
        private int mCenterX;
        private int mCenterY;
        private float mCurrentRatio;
        private double mFingerAngle;
        private float mInnerRadius;
        private float mMaxRatio;
        private float mOuterRadius;
        private final Paint mPaint;
        private List<Integer> mZoomRatios;
        private final int mZoomStroke;
        private final Log.Tag TAG = new Log.Tag("ZoomProcessor");
        private final float mMinRatio = 1.0f;
        private boolean mVisible = false;

        public ZoomProcessor() {
            Resources res = PreviewOverlay.this.getResources();
            this.mZoomStroke = res.getDimensionPixelSize(R.dimen.zoom_stroke);
            this.mPaint = new Paint();
            this.mPaint.setAntiAlias(true);
            this.mPaint.setColor(-1);
            this.mPaint.setStyle(Paint.Style.STROKE);
            this.mPaint.setStrokeWidth(this.mZoomStroke);
            this.mPaint.setStrokeCap(Paint.Cap.ROUND);
        }

        public void setZoomMax(float zoomMaxRatio) {
            this.mMaxRatio = zoomMaxRatio;
        }

        public void setZoom(float ratio) {
            this.mCurrentRatio = ratio;
        }

        public void layout(int l, int t, int r, int b) {
            this.mCenterX = (r - l) / 2;
            this.mCenterY = (b - t) / 2;
            float insetCircleDiameter = Math.min(PreviewOverlay.this.getWidth(), PreviewOverlay.this.getHeight());
            this.mOuterRadius = 0.5f * insetCircleDiameter * ZOOM_UI_SIZE;
            this.mInnerRadius = this.mOuterRadius * ZOOM_UI_DONUT;
        }

        public void draw(Canvas canvas) {
            if (this.mVisible) {
                this.mPaint.setAlpha(70);
                canvas.drawLine((this.mInnerRadius * ((float) Math.cos(this.mFingerAngle))) + this.mCenterX, this.mCenterY - (this.mInnerRadius * ((float) Math.sin(this.mFingerAngle))), (this.mOuterRadius * ((float) Math.cos(this.mFingerAngle))) + this.mCenterX, this.mCenterY - (this.mOuterRadius * ((float) Math.sin(this.mFingerAngle))), this.mPaint);
                canvas.drawLine(this.mCenterX - (this.mInnerRadius * ((float) Math.cos(this.mFingerAngle))), (this.mInnerRadius * ((float) Math.sin(this.mFingerAngle))) + this.mCenterY, this.mCenterX - (this.mOuterRadius * ((float) Math.cos(this.mFingerAngle))), (this.mOuterRadius * ((float) Math.sin(this.mFingerAngle))) + this.mCenterY, this.mPaint);
                this.mPaint.setAlpha(MotionEventCompat.ACTION_MASK);
                float fillRatio = (this.mCurrentRatio - 1.0f) / (this.mMaxRatio - 1.0f);
                float zoomRadius = this.mInnerRadius + ((this.mOuterRadius - this.mInnerRadius) * fillRatio);
                canvas.drawLine((this.mInnerRadius * ((float) Math.cos(this.mFingerAngle))) + this.mCenterX, this.mCenterY - (this.mInnerRadius * ((float) Math.sin(this.mFingerAngle))), (((float) Math.cos(this.mFingerAngle)) * zoomRadius) + this.mCenterX, this.mCenterY - (((float) Math.sin(this.mFingerAngle)) * zoomRadius), this.mPaint);
                canvas.drawLine(this.mCenterX - (this.mInnerRadius * ((float) Math.cos(this.mFingerAngle))), (this.mInnerRadius * ((float) Math.sin(this.mFingerAngle))) + this.mCenterY, this.mCenterX - (((float) Math.cos(this.mFingerAngle)) * zoomRadius), (((float) Math.sin(this.mFingerAngle)) * zoomRadius) + this.mCenterY, this.mPaint);
            }
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float sf = detector.getScaleFactor();
            this.mCurrentRatio = (((this.mCurrentRatio + 0.33f) * sf) * sf) - 0.33f;
            if (this.mCurrentRatio < 1.0f) {
                this.mCurrentRatio = 1.0f;
            }
            if (this.mCurrentRatio > this.mMaxRatio) {
                this.mCurrentRatio = this.mMaxRatio;
            }
            long now = SystemClock.uptimeMillis();
            if (now > PreviewOverlay.this.mDelayZoomCallUntilMillis) {
                if (PreviewOverlay.this.mZoomListener != null) {
                    PreviewOverlay.this.mZoomListener.onZoomValueChanged(this.mCurrentRatio);
                }
                PreviewOverlay.this.mDelayZoomCallUntilMillis = PreviewOverlay.ZOOM_MINIMUM_WAIT_MILLIS + now;
            }
            this.mFingerAngle = PreviewOverlay.this.mScaleDetector.getAngle();
            PreviewOverlay.this.invalidate();
            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            PreviewOverlay.this.mZoomProcessor.showZoomUI();
            if (PreviewOverlay.this.mZoomListener != null) {
                if (PreviewOverlay.this.mZoomListener != null) {
                    PreviewOverlay.this.mZoomListener.onZoomStart();
                }
                this.mFingerAngle = PreviewOverlay.this.mScaleDetector.getAngle();
                PreviewOverlay.this.invalidate();
                return true;
            }
            return false;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            PreviewOverlay.this.mZoomProcessor.hideZoomUI();
            if (PreviewOverlay.this.mZoomListener != null) {
                PreviewOverlay.this.mZoomListener.onZoomEnd();
            }
            PreviewOverlay.this.invalidate();
        }

        public boolean isVisible() {
            return this.mVisible;
        }

        public void showZoomUI() {
            if (PreviewOverlay.this.mZoomListener != null) {
                this.mVisible = true;
                this.mFingerAngle = PreviewOverlay.this.mScaleDetector.getAngle();
                PreviewOverlay.this.invalidate();
            }
        }

        public void hideZoomUI() {
            if (PreviewOverlay.this.mZoomListener != null) {
                this.mVisible = false;
                PreviewOverlay.this.invalidate();
            }
        }

        private void setupZoom(float zoomMax, float zoom) {
            setZoomMax(zoomMax);
            setZoom(zoom);
        }
    }
}
