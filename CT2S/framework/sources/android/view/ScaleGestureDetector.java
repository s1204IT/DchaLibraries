package android.view;

import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.os.SystemClock;
import android.util.FloatMath;
import android.view.GestureDetector;
import com.android.internal.R;

public class ScaleGestureDetector {
    private static final int DOUBLE_TAP_MODE_IN_PROGRESS = 1;
    private static final int DOUBLE_TAP_MODE_NONE = 0;
    private static final float SCALE_FACTOR = 0.5f;
    private static final String TAG = "ScaleGestureDetector";
    private static final long TOUCH_STABILIZE_TIME = 128;
    private final Context mContext;
    private float mCurrSpan;
    private float mCurrSpanX;
    private float mCurrSpanY;
    private long mCurrTime;
    private MotionEvent mDoubleTapEvent;
    private int mDoubleTapMode;
    private boolean mEventBeforeOrAboveStartingGestureEvent;
    private float mFocusX;
    private float mFocusY;
    private GestureDetector mGestureDetector;
    private final Handler mHandler;
    private boolean mInProgress;
    private float mInitialSpan;
    private final InputEventConsistencyVerifier mInputEventConsistencyVerifier;
    private final OnScaleGestureListener mListener;
    private int mMinSpan;
    private float mPrevSpan;
    private float mPrevSpanX;
    private float mPrevSpanY;
    private long mPrevTime;
    private boolean mQuickScaleEnabled;
    private int mSpanSlop;
    private int mTouchHistoryDirection;
    private float mTouchHistoryLastAccepted;
    private long mTouchHistoryLastAcceptedTime;
    private float mTouchLower;
    private int mTouchMinMajor;
    private float mTouchUpper;

    public interface OnScaleGestureListener {
        boolean onScale(ScaleGestureDetector scaleGestureDetector);

        boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector);

        void onScaleEnd(ScaleGestureDetector scaleGestureDetector);
    }

    public static class SimpleOnScaleGestureListener implements OnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            return false;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
        }
    }

    public ScaleGestureDetector(Context context, OnScaleGestureListener listener) {
        this(context, listener, null);
    }

    public ScaleGestureDetector(Context context, OnScaleGestureListener listener, Handler handler) {
        this.mDoubleTapMode = 0;
        this.mInputEventConsistencyVerifier = InputEventConsistencyVerifier.isInstrumentationEnabled() ? new InputEventConsistencyVerifier(this, 0) : null;
        this.mContext = context;
        this.mListener = listener;
        this.mSpanSlop = ViewConfiguration.get(context).getScaledTouchSlop() * 2;
        Resources res = context.getResources();
        this.mTouchMinMajor = res.getDimensionPixelSize(R.dimen.config_minScalingTouchMajor);
        this.mMinSpan = res.getDimensionPixelSize(R.dimen.config_minScalingSpan);
        this.mHandler = handler;
        if (context.getApplicationInfo().targetSdkVersion > 18) {
            setQuickScaleEnabled(true);
        }
    }

    private void addTouchHistory(MotionEvent ev) {
        float major;
        int directionSig;
        long currentTime = SystemClock.uptimeMillis();
        int count = ev.getPointerCount();
        boolean accept = currentTime - this.mTouchHistoryLastAcceptedTime >= 128;
        float total = 0.0f;
        int sampleCount = 0;
        for (int i = 0; i < count; i++) {
            boolean hasLastAccepted = !Float.isNaN(this.mTouchHistoryLastAccepted);
            int historySize = ev.getHistorySize();
            int pointerSampleCount = historySize + 1;
            int h = 0;
            while (h < pointerSampleCount) {
                if (h < historySize) {
                    major = ev.getHistoricalTouchMajor(i, h);
                } else {
                    major = ev.getTouchMajor(i);
                }
                if (major < this.mTouchMinMajor) {
                    major = this.mTouchMinMajor;
                }
                total += major;
                if (Float.isNaN(this.mTouchUpper) || major > this.mTouchUpper) {
                    this.mTouchUpper = major;
                }
                if (Float.isNaN(this.mTouchLower) || major < this.mTouchLower) {
                    this.mTouchLower = major;
                }
                if (hasLastAccepted && ((directionSig = (int) Math.signum(major - this.mTouchHistoryLastAccepted)) != this.mTouchHistoryDirection || (directionSig == 0 && this.mTouchHistoryDirection == 0))) {
                    this.mTouchHistoryDirection = directionSig;
                    long time = h < historySize ? ev.getHistoricalEventTime(h) : ev.getEventTime();
                    this.mTouchHistoryLastAcceptedTime = time;
                    accept = false;
                }
                h++;
            }
            sampleCount += pointerSampleCount;
        }
        float avg = total / sampleCount;
        if (accept) {
            float newAccepted = ((this.mTouchUpper + this.mTouchLower) + avg) / 3.0f;
            this.mTouchUpper = (this.mTouchUpper + newAccepted) / 2.0f;
            this.mTouchLower = (this.mTouchLower + newAccepted) / 2.0f;
            this.mTouchHistoryLastAccepted = newAccepted;
            this.mTouchHistoryDirection = 0;
            this.mTouchHistoryLastAcceptedTime = ev.getEventTime();
        }
    }

    private void clearTouchHistory() {
        this.mTouchUpper = Float.NaN;
        this.mTouchLower = Float.NaN;
        this.mTouchHistoryLastAccepted = Float.NaN;
        this.mTouchHistoryDirection = 0;
        this.mTouchHistoryLastAcceptedTime = 0L;
    }

    public boolean onTouchEvent(MotionEvent event) {
        float focusX;
        float focusY;
        float span;
        if (this.mInputEventConsistencyVerifier != null) {
            this.mInputEventConsistencyVerifier.onTouchEvent(event, 0);
        }
        this.mCurrTime = event.getEventTime();
        int action = event.getActionMasked();
        if (this.mQuickScaleEnabled) {
            this.mGestureDetector.onTouchEvent(event);
        }
        boolean streamComplete = action == 1 || action == 3;
        if (action == 0 || streamComplete) {
            if (this.mInProgress) {
                this.mListener.onScaleEnd(this);
                this.mInProgress = false;
                this.mInitialSpan = 0.0f;
                this.mDoubleTapMode = 0;
            } else if (this.mDoubleTapMode == 1 && streamComplete) {
                this.mInProgress = false;
                this.mInitialSpan = 0.0f;
                this.mDoubleTapMode = 0;
            }
            if (streamComplete) {
                clearTouchHistory();
                return true;
            }
        }
        boolean configChanged = action == 0 || action == 6 || action == 5;
        boolean pointerUp = action == 6;
        int skipIndex = pointerUp ? event.getActionIndex() : -1;
        float sumX = 0.0f;
        float sumY = 0.0f;
        int count = event.getPointerCount();
        int div = pointerUp ? count - 1 : count;
        if (this.mDoubleTapMode == 1) {
            focusX = this.mDoubleTapEvent.getX();
            focusY = this.mDoubleTapEvent.getY();
            if (event.getY() < focusY) {
                this.mEventBeforeOrAboveStartingGestureEvent = true;
            } else {
                this.mEventBeforeOrAboveStartingGestureEvent = false;
            }
        } else {
            for (int i = 0; i < count; i++) {
                if (skipIndex != i) {
                    sumX += event.getX(i);
                    sumY += event.getY(i);
                }
            }
            focusX = sumX / div;
            focusY = sumY / div;
        }
        addTouchHistory(event);
        float devSumX = 0.0f;
        float devSumY = 0.0f;
        for (int i2 = 0; i2 < count; i2++) {
            if (skipIndex != i2) {
                float touchSize = this.mTouchHistoryLastAccepted / 2.0f;
                devSumX += Math.abs(event.getX(i2) - focusX) + touchSize;
                devSumY += Math.abs(event.getY(i2) - focusY) + touchSize;
            }
        }
        float devX = devSumX / div;
        float devY = devSumY / div;
        float spanX = devX * 2.0f;
        float spanY = devY * 2.0f;
        if (inDoubleTapMode()) {
            span = spanY;
        } else {
            span = FloatMath.sqrt((spanX * spanX) + (spanY * spanY));
        }
        boolean wasInProgress = this.mInProgress;
        this.mFocusX = focusX;
        this.mFocusY = focusY;
        if (!inDoubleTapMode() && this.mInProgress && (span < this.mMinSpan || configChanged)) {
            this.mListener.onScaleEnd(this);
            this.mInProgress = false;
            this.mInitialSpan = span;
            this.mDoubleTapMode = 0;
        }
        if (configChanged) {
            this.mCurrSpanX = spanX;
            this.mPrevSpanX = spanX;
            this.mCurrSpanY = spanY;
            this.mPrevSpanY = spanY;
            this.mCurrSpan = span;
            this.mPrevSpan = span;
            this.mInitialSpan = span;
        }
        int minSpan = inDoubleTapMode() ? this.mSpanSlop : this.mMinSpan;
        if (!this.mInProgress && span >= minSpan && (wasInProgress || Math.abs(span - this.mInitialSpan) > this.mSpanSlop)) {
            this.mCurrSpanX = spanX;
            this.mPrevSpanX = spanX;
            this.mCurrSpanY = spanY;
            this.mPrevSpanY = spanY;
            this.mCurrSpan = span;
            this.mPrevSpan = span;
            this.mPrevTime = this.mCurrTime;
            this.mInProgress = this.mListener.onScaleBegin(this);
        }
        if (action == 2) {
            this.mCurrSpanX = spanX;
            this.mCurrSpanY = spanY;
            this.mCurrSpan = span;
            boolean updatePrev = true;
            if (this.mInProgress) {
                updatePrev = this.mListener.onScale(this);
            }
            if (updatePrev) {
                this.mPrevSpanX = this.mCurrSpanX;
                this.mPrevSpanY = this.mCurrSpanY;
                this.mPrevSpan = this.mCurrSpan;
                this.mPrevTime = this.mCurrTime;
            }
        }
        return true;
    }

    private boolean inDoubleTapMode() {
        return this.mDoubleTapMode == 1;
    }

    public void setQuickScaleEnabled(boolean scales) {
        this.mQuickScaleEnabled = scales;
        if (this.mQuickScaleEnabled && this.mGestureDetector == null) {
            GestureDetector.SimpleOnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    ScaleGestureDetector.this.mDoubleTapEvent = e;
                    ScaleGestureDetector.this.mDoubleTapMode = 1;
                    return true;
                }
            };
            this.mGestureDetector = new GestureDetector(this.mContext, gestureListener, this.mHandler);
        }
    }

    public boolean isQuickScaleEnabled() {
        return this.mQuickScaleEnabled;
    }

    public boolean isInProgress() {
        return this.mInProgress;
    }

    public float getFocusX() {
        return this.mFocusX;
    }

    public float getFocusY() {
        return this.mFocusY;
    }

    public float getCurrentSpan() {
        return this.mCurrSpan;
    }

    public float getCurrentSpanX() {
        return this.mCurrSpanX;
    }

    public float getCurrentSpanY() {
        return this.mCurrSpanY;
    }

    public float getPreviousSpan() {
        return this.mPrevSpan;
    }

    public float getPreviousSpanX() {
        return this.mPrevSpanX;
    }

    public float getPreviousSpanY() {
        return this.mPrevSpanY;
    }

    public float getScaleFactor() {
        if (!inDoubleTapMode()) {
            if (this.mPrevSpan > 0.0f) {
                return this.mCurrSpan / this.mPrevSpan;
            }
            return 1.0f;
        }
        boolean scaleUp = (this.mEventBeforeOrAboveStartingGestureEvent && this.mCurrSpan < this.mPrevSpan) || (!this.mEventBeforeOrAboveStartingGestureEvent && this.mCurrSpan > this.mPrevSpan);
        float spanDiff = Math.abs(1.0f - (this.mCurrSpan / this.mPrevSpan)) * SCALE_FACTOR;
        if (this.mPrevSpan <= 0.0f) {
            return 1.0f;
        }
        return scaleUp ? 1.0f + spanDiff : 1.0f - spanDiff;
    }

    public long getTimeDelta() {
        return this.mCurrTime - this.mPrevTime;
    }

    public long getEventTime() {
        return this.mCurrTime;
    }
}
