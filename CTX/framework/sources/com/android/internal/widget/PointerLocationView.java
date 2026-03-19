package com.android.internal.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.hardware.input.InputManager;
import android.os.SystemProperties;
import android.util.Log;
import android.util.Slog;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManagerPolicyConstants;
import java.util.ArrayList;

public class PointerLocationView extends View implements InputManager.InputDeviceListener, WindowManagerPolicyConstants.PointerEventListener {
    private static final String ALT_STRATEGY_PROPERY_KEY = "debug.velocitytracker.alt";
    private static final String TAG = "Pointer";
    private final int ESTIMATE_FUTURE_POINTS;
    private final float ESTIMATE_INTERVAL;
    private final int ESTIMATE_PAST_POINTS;
    private int mActivePointerId;
    private final VelocityTracker mAltVelocity;
    private boolean mCurDown;
    private int mCurNumPointers;
    private final Paint mCurrentPointPaint;
    private int mHeaderBottom;
    private final InputManager mIm;
    private int mMaxNumPointers;
    private final Paint mPaint;
    private final Paint mPathPaint;
    private final ArrayList<PointerState> mPointers;
    private boolean mPrintCoords;
    private RectF mReusableOvalRect;
    private final Paint mTargetPaint;
    private final MotionEvent.PointerCoords mTempCoords;
    private final FasterStringBuilder mText;
    private final Paint mTextBackgroundPaint;
    private final Paint mTextLevelPaint;
    private final Paint.FontMetricsInt mTextMetrics;
    private final Paint mTextPaint;
    private final ViewConfiguration mVC;
    private final VelocityTracker mVelocity;

    public static class PointerState {
        private float mAltXVelocity;
        private float mAltYVelocity;
        private float mBoundingBottom;
        private float mBoundingLeft;
        private float mBoundingRight;
        private float mBoundingTop;
        private boolean mCurDown;
        private boolean mHasBoundingBox;
        private int mToolType;
        private int mTraceCount;
        private float mXVelocity;
        private float mYVelocity;
        private float[] mTraceX = new float[32];
        private float[] mTraceY = new float[32];
        private boolean[] mTraceCurrent = new boolean[32];
        private MotionEvent.PointerCoords mCoords = new MotionEvent.PointerCoords();
        private VelocityTracker.Estimator mEstimator = new VelocityTracker.Estimator();
        private VelocityTracker.Estimator mAltEstimator = new VelocityTracker.Estimator();

        public void clearTrace() {
            this.mTraceCount = 0;
        }

        public void addTrace(float f, float f2, boolean z) {
            int length = this.mTraceX.length;
            if (this.mTraceCount == length) {
                int i = length * 2;
                float[] fArr = new float[i];
                System.arraycopy(this.mTraceX, 0, fArr, 0, this.mTraceCount);
                this.mTraceX = fArr;
                float[] fArr2 = new float[i];
                System.arraycopy(this.mTraceY, 0, fArr2, 0, this.mTraceCount);
                this.mTraceY = fArr2;
                boolean[] zArr = new boolean[i];
                System.arraycopy(this.mTraceCurrent, 0, zArr, 0, this.mTraceCount);
                this.mTraceCurrent = zArr;
            }
            this.mTraceX[this.mTraceCount] = f;
            this.mTraceY[this.mTraceCount] = f2;
            this.mTraceCurrent[this.mTraceCount] = z;
            this.mTraceCount++;
        }
    }

    public PointerLocationView(Context context) {
        super(context);
        this.ESTIMATE_PAST_POINTS = 4;
        this.ESTIMATE_FUTURE_POINTS = 2;
        this.ESTIMATE_INTERVAL = 0.02f;
        this.mTextMetrics = new Paint.FontMetricsInt();
        this.mPointers = new ArrayList<>();
        this.mTempCoords = new MotionEvent.PointerCoords();
        this.mText = new FasterStringBuilder();
        this.mPrintCoords = true;
        this.mReusableOvalRect = new RectF();
        setFocusableInTouchMode(true);
        this.mIm = (InputManager) context.getSystemService(InputManager.class);
        this.mVC = ViewConfiguration.get(context);
        this.mTextPaint = new Paint();
        this.mTextPaint.setAntiAlias(true);
        this.mTextPaint.setTextSize(10.0f * getResources().getDisplayMetrics().density);
        this.mTextPaint.setARGB(255, 0, 0, 0);
        this.mTextBackgroundPaint = new Paint();
        this.mTextBackgroundPaint.setAntiAlias(false);
        this.mTextBackgroundPaint.setARGB(128, 255, 255, 255);
        this.mTextLevelPaint = new Paint();
        this.mTextLevelPaint.setAntiAlias(false);
        this.mTextLevelPaint.setARGB(192, 255, 0, 0);
        this.mPaint = new Paint();
        this.mPaint.setAntiAlias(true);
        this.mPaint.setARGB(255, 255, 255, 255);
        this.mPaint.setStyle(Paint.Style.STROKE);
        this.mPaint.setStrokeWidth(2.0f);
        this.mCurrentPointPaint = new Paint();
        this.mCurrentPointPaint.setAntiAlias(true);
        this.mCurrentPointPaint.setARGB(255, 255, 0, 0);
        this.mCurrentPointPaint.setStyle(Paint.Style.STROKE);
        this.mCurrentPointPaint.setStrokeWidth(2.0f);
        this.mTargetPaint = new Paint();
        this.mTargetPaint.setAntiAlias(false);
        this.mTargetPaint.setARGB(255, 0, 0, 192);
        this.mPathPaint = new Paint();
        this.mPathPaint.setAntiAlias(false);
        this.mPathPaint.setARGB(255, 0, 96, 255);
        this.mPaint.setStyle(Paint.Style.STROKE);
        this.mPaint.setStrokeWidth(1.0f);
        this.mPointers.add(new PointerState());
        this.mActivePointerId = 0;
        this.mVelocity = VelocityTracker.obtain();
        String str = SystemProperties.get(ALT_STRATEGY_PROPERY_KEY);
        if (str.length() != 0) {
            Log.d(TAG, "Comparing default velocity tracker strategy with " + str);
            this.mAltVelocity = VelocityTracker.obtain(str);
            return;
        }
        this.mAltVelocity = null;
    }

    public void setPrintCoords(boolean z) {
        this.mPrintCoords = z;
    }

    @Override
    protected void onMeasure(int i, int i2) {
        super.onMeasure(i, i2);
        this.mTextPaint.getFontMetricsInt(this.mTextMetrics);
        this.mHeaderBottom = (-this.mTextMetrics.ascent) + this.mTextMetrics.descent + 2;
    }

    private void drawOval(Canvas canvas, float f, float f2, float f3, float f4, float f5, Paint paint) {
        canvas.save(1);
        canvas.rotate((float) (((double) (f5 * 180.0f)) / 3.141592653589793d), f, f2);
        float f6 = f4 / 2.0f;
        this.mReusableOvalRect.left = f - f6;
        this.mReusableOvalRect.right = f + f6;
        float f7 = f3 / 2.0f;
        this.mReusableOvalRect.top = f2 - f7;
        this.mReusableOvalRect.bottom = f2 + f7;
        canvas.drawOval(this.mReusableOvalRect, paint);
        canvas.restore();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float f;
        float f2;
        int width = getWidth();
        int i = width / 7;
        int i2 = (-this.mTextMetrics.ascent) + 1;
        int i3 = this.mHeaderBottom;
        int size = this.mPointers.size();
        if (this.mActivePointerId >= 0) {
            PointerState pointerState = this.mPointers.get(this.mActivePointerId);
            float f3 = i3;
            canvas.drawRect(0.0f, 0.0f, i - 1, f3, this.mTextBackgroundPaint);
            float f4 = i2;
            canvas.drawText(this.mText.clear().append("P: ").append(this.mCurNumPointers).append(" / ").append(this.mMaxNumPointers).toString(), 1.0f, f4, this.mTextPaint);
            int i4 = pointerState.mTraceCount;
            if ((this.mCurDown && pointerState.mCurDown) || i4 == 0) {
                f2 = 1.0f;
                canvas.drawRect(i, 0.0f, r4 - 1, f3, this.mTextBackgroundPaint);
                canvas.drawText(this.mText.clear().append("X: ").append(pointerState.mCoords.x, 1).toString(), 1 + i, f4, this.mTextPaint);
                canvas.drawRect(i * 2, 0.0f, (i * 3) - 1, f3, this.mTextBackgroundPaint);
                canvas.drawText(this.mText.clear().append("Y: ").append(pointerState.mCoords.y, 1).toString(), 1 + r4, f4, this.mTextPaint);
            } else {
                f2 = 1.0f;
                int i5 = i4 - 1;
                float f5 = pointerState.mTraceX[i5] - pointerState.mTraceX[0];
                float f6 = pointerState.mTraceY[i5] - pointerState.mTraceY[0];
                int i6 = i * 2;
                canvas.drawRect(i, 0.0f, i6 - 1, f3, Math.abs(f5) < ((float) this.mVC.getScaledTouchSlop()) ? this.mTextBackgroundPaint : this.mTextLevelPaint);
                canvas.drawText(this.mText.clear().append("dX: ").append(f5, 1).toString(), 1 + i, f4, this.mTextPaint);
                canvas.drawRect(i6, 0.0f, (i * 3) - 1, f3, Math.abs(f6) < ((float) this.mVC.getScaledTouchSlop()) ? this.mTextBackgroundPaint : this.mTextLevelPaint);
                canvas.drawText(this.mText.clear().append("dY: ").append(f6, 1).toString(), 1 + i6, f4, this.mTextPaint);
            }
            canvas.drawRect(i * 3, 0.0f, r14 - 1, f3, this.mTextBackgroundPaint);
            canvas.drawText(this.mText.clear().append("Xv: ").append(pointerState.mXVelocity, 3).toString(), r13 + 1, f4, this.mTextPaint);
            canvas.drawRect(i * 4, 0.0f, r13 - 1, f3, this.mTextBackgroundPaint);
            canvas.drawText(this.mText.clear().append("Yv: ").append(pointerState.mYVelocity, 3).toString(), 1 + r14, f4, this.mTextPaint);
            float f7 = i * 5;
            int i7 = i * 6;
            canvas.drawRect(f7, 0.0f, i7 - 1, f3, this.mTextBackgroundPaint);
            float f8 = i;
            canvas.drawRect(f7, 0.0f, ((pointerState.mCoords.pressure * f8) + f7) - f2, f3, this.mTextLevelPaint);
            canvas.drawText(this.mText.clear().append("Prs: ").append(pointerState.mCoords.pressure, 2).toString(), 1 + r13, f4, this.mTextPaint);
            float f9 = i7;
            canvas.drawRect(f9, 0.0f, width, f3, this.mTextBackgroundPaint);
            canvas.drawRect(f9, 0.0f, ((pointerState.mCoords.size * f8) + f9) - f2, f3, this.mTextLevelPaint);
            canvas.drawText(this.mText.clear().append("Size: ").append(pointerState.mCoords.size, 2).toString(), 1 + i7, f4, this.mTextPaint);
        }
        for (int i8 = 0; i8 < size; i8++) {
            PointerState pointerState2 = this.mPointers.get(i8);
            int i9 = pointerState2.mTraceCount;
            this.mPaint.setARGB(255, 128, 255, 255);
            float f10 = 0.0f;
            float f11 = 0.0f;
            boolean z = false;
            boolean z2 = false;
            for (int i10 = 0; i10 < i9; i10++) {
                float f12 = pointerState2.mTraceX[i10];
                float f13 = pointerState2.mTraceY[i10];
                if (Float.isNaN(f12)) {
                    z2 = false;
                } else {
                    if (z2) {
                        f = f12;
                        float f14 = f10;
                        float f15 = f11;
                        canvas.drawLine(f11, f10, f12, f13, this.mPathPaint);
                        canvas.drawPoint(f15, f14, pointerState2.mTraceCurrent[i10] ? this.mCurrentPointPaint : this.mPaint);
                        z = true;
                    } else {
                        f = f12;
                    }
                    f10 = f13;
                    f11 = f;
                    z2 = true;
                }
            }
            float f16 = f10;
            float f17 = f11;
            if (z) {
                this.mPaint.setARGB(128, 128, 0, 128);
                float fEstimateX = pointerState2.mEstimator.estimateX(-0.08f);
                float fEstimateY = pointerState2.mEstimator.estimateY(-0.08f);
                int i11 = -3;
                float f18 = fEstimateX;
                while (i11 <= 2) {
                    float f19 = i11 * 0.02f;
                    float fEstimateX2 = pointerState2.mEstimator.estimateX(f19);
                    float fEstimateY2 = pointerState2.mEstimator.estimateY(f19);
                    canvas.drawLine(f18, fEstimateY, fEstimateX2, fEstimateY2, this.mPaint);
                    i11++;
                    f18 = fEstimateX2;
                    fEstimateY = fEstimateY2;
                }
                this.mPaint.setARGB(255, 255, 64, 128);
                canvas.drawLine(f17, f16, f17 + (pointerState2.mXVelocity * 16.0f), f16 + (pointerState2.mYVelocity * 16.0f), this.mPaint);
                if (this.mAltVelocity != null) {
                    this.mPaint.setARGB(128, 0, 128, 128);
                    float fEstimateX3 = pointerState2.mAltEstimator.estimateX(-0.08f);
                    float fEstimateY3 = pointerState2.mAltEstimator.estimateY(-0.08f);
                    int i12 = -3;
                    float f20 = fEstimateX3;
                    while (i12 <= 2) {
                        float f21 = i12 * 0.02f;
                        float fEstimateX4 = pointerState2.mAltEstimator.estimateX(f21);
                        float fEstimateY4 = pointerState2.mAltEstimator.estimateY(f21);
                        canvas.drawLine(f20, fEstimateY3, fEstimateX4, fEstimateY4, this.mPaint);
                        i12++;
                        f20 = fEstimateX4;
                        fEstimateY3 = fEstimateY4;
                    }
                    this.mPaint.setARGB(255, 64, 255, 128);
                    canvas.drawLine(f17, f16, f17 + (pointerState2.mAltXVelocity * 16.0f), f16 + (pointerState2.mAltYVelocity * 16.0f), this.mPaint);
                }
            }
            if (this.mCurDown && pointerState2.mCurDown) {
                canvas.drawLine(0.0f, pointerState2.mCoords.y, getWidth(), pointerState2.mCoords.y, this.mTargetPaint);
                canvas.drawLine(pointerState2.mCoords.x, 0.0f, pointerState2.mCoords.x, getHeight(), this.mTargetPaint);
                int i13 = (int) (pointerState2.mCoords.pressure * 255.0f);
                int i14 = 255 - i13;
                this.mPaint.setARGB(255, i13, 255, i14);
                canvas.drawPoint(pointerState2.mCoords.x, pointerState2.mCoords.y, this.mPaint);
                this.mPaint.setARGB(255, i13, i14, 128);
                drawOval(canvas, pointerState2.mCoords.x, pointerState2.mCoords.y, pointerState2.mCoords.touchMajor, pointerState2.mCoords.touchMinor, pointerState2.mCoords.orientation, this.mPaint);
                this.mPaint.setARGB(255, i13, 128, i14);
                drawOval(canvas, pointerState2.mCoords.x, pointerState2.mCoords.y, pointerState2.mCoords.toolMajor, pointerState2.mCoords.toolMinor, pointerState2.mCoords.orientation, this.mPaint);
                float f22 = pointerState2.mCoords.toolMajor * 0.7f;
                if (f22 < 20.0f) {
                    f22 = 20.0f;
                }
                this.mPaint.setARGB(255, i13, 255, 0);
                double d = f22;
                float fSin = (float) (Math.sin(pointerState2.mCoords.orientation) * d);
                float f23 = (float) ((-Math.cos(pointerState2.mCoords.orientation)) * d);
                if (pointerState2.mToolType == 2 || pointerState2.mToolType == 4) {
                    canvas.drawLine(pointerState2.mCoords.x, pointerState2.mCoords.y, pointerState2.mCoords.x + fSin, pointerState2.mCoords.y + f23, this.mPaint);
                } else {
                    canvas.drawLine(pointerState2.mCoords.x - fSin, pointerState2.mCoords.y - f23, pointerState2.mCoords.x + fSin, pointerState2.mCoords.y + f23, this.mPaint);
                }
                float fSin2 = (float) Math.sin(pointerState2.mCoords.getAxisValue(25));
                canvas.drawCircle(pointerState2.mCoords.x + (fSin * fSin2), pointerState2.mCoords.y + (f23 * fSin2), 3.0f, this.mPaint);
                if (pointerState2.mHasBoundingBox) {
                    canvas.drawRect(pointerState2.mBoundingLeft, pointerState2.mBoundingTop, pointerState2.mBoundingRight, pointerState2.mBoundingBottom, this.mPaint);
                }
            }
        }
    }

    private void logMotionEvent(String str, MotionEvent motionEvent) {
        int action = motionEvent.getAction();
        int historySize = motionEvent.getHistorySize();
        int pointerCount = motionEvent.getPointerCount();
        for (int i = 0; i < historySize; i++) {
            for (int i2 = 0; i2 < pointerCount; i2++) {
                int pointerId = motionEvent.getPointerId(i2);
                motionEvent.getHistoricalPointerCoords(i2, i, this.mTempCoords);
                logCoords(str, action, i2, this.mTempCoords, pointerId, motionEvent);
            }
        }
        for (int i3 = 0; i3 < pointerCount; i3++) {
            int pointerId2 = motionEvent.getPointerId(i3);
            motionEvent.getPointerCoords(i3, this.mTempCoords);
            logCoords(str, action, i3, this.mTempCoords, pointerId2, motionEvent);
        }
    }

    private void logCoords(String str, int i, int i2, MotionEvent.PointerCoords pointerCoords, int i3, MotionEvent motionEvent) {
        String string;
        int toolType = motionEvent.getToolType(i2);
        int buttonState = motionEvent.getButtonState();
        switch (i & 255) {
            case 0:
                string = "DOWN";
                break;
            case 1:
                string = "UP";
                break;
            case 2:
                string = "MOVE";
                break;
            case 3:
                string = "CANCEL";
                break;
            case 4:
                string = "OUTSIDE";
                break;
            case 5:
                if (i2 == ((i & 65280) >> 8)) {
                    string = "DOWN";
                } else {
                    string = "MOVE";
                }
                break;
            case 6:
                if (i2 == ((i & 65280) >> 8)) {
                    string = "UP";
                } else {
                    string = "MOVE";
                }
                break;
            case 7:
                string = "HOVER MOVE";
                break;
            case 8:
                string = "SCROLL";
                break;
            case 9:
                string = "HOVER ENTER";
                break;
            case 10:
                string = "HOVER EXIT";
                break;
            default:
                string = Integer.toString(i);
                break;
        }
        Log.i(TAG, this.mText.clear().append(str).append(" id ").append(i3 + 1).append(": ").append(string).append(" (").append(pointerCoords.x, 3).append(", ").append(pointerCoords.y, 3).append(") Pressure=").append(pointerCoords.pressure, 3).append(" Size=").append(pointerCoords.size, 3).append(" TouchMajor=").append(pointerCoords.touchMajor, 3).append(" TouchMinor=").append(pointerCoords.touchMinor, 3).append(" ToolMajor=").append(pointerCoords.toolMajor, 3).append(" ToolMinor=").append(pointerCoords.toolMinor, 3).append(" Orientation=").append((float) (((double) (pointerCoords.orientation * 180.0f)) / 3.141592653589793d), 1).append("deg").append(" Tilt=").append((float) (((double) (pointerCoords.getAxisValue(25) * 180.0f)) / 3.141592653589793d), 1).append("deg").append(" Distance=").append(pointerCoords.getAxisValue(24), 1).append(" VScroll=").append(pointerCoords.getAxisValue(9), 1).append(" HScroll=").append(pointerCoords.getAxisValue(10), 1).append(" BoundingBox=[(").append(motionEvent.getAxisValue(32), 3).append(", ").append(motionEvent.getAxisValue(33), 3).append(")").append(", (").append(motionEvent.getAxisValue(34), 3).append(", ").append(motionEvent.getAxisValue(35), 3).append(")]").append(" ToolType=").append(MotionEvent.toolTypeToString(toolType)).append(" ButtonState=").append(MotionEvent.buttonStateToString(buttonState)).toString());
    }

    @Override
    public void onPointerEvent(MotionEvent motionEvent) {
        MotionEvent.PointerCoords pointerCoords;
        MotionEvent.PointerCoords pointerCoords2;
        PointerState pointerState;
        int i;
        char c;
        MotionEvent.PointerCoords pointerCoords3;
        MotionEvent.PointerCoords pointerCoords4;
        PointerState pointerState2;
        int i2;
        int i3;
        int i4;
        int action = motionEvent.getAction();
        int size = this.mPointers.size();
        if (action == 0 || (action & 255) == 5) {
            int i5 = (action & 65280) >> 8;
            if (action == 0) {
                for (int i6 = 0; i6 < size; i6++) {
                    PointerState pointerState3 = this.mPointers.get(i6);
                    pointerState3.clearTrace();
                    pointerState3.mCurDown = false;
                }
                this.mCurDown = true;
                this.mCurNumPointers = 0;
                this.mMaxNumPointers = 0;
                this.mVelocity.clear();
                if (this.mAltVelocity != null) {
                    this.mAltVelocity.clear();
                }
            }
            this.mCurNumPointers++;
            if (this.mMaxNumPointers < this.mCurNumPointers) {
                this.mMaxNumPointers = this.mCurNumPointers;
            }
            int pointerId = motionEvent.getPointerId(i5);
            while (size <= pointerId) {
                this.mPointers.add(new PointerState());
                size++;
            }
            if (this.mActivePointerId < 0 || !this.mPointers.get(this.mActivePointerId).mCurDown) {
                this.mActivePointerId = pointerId;
            }
            PointerState pointerState4 = this.mPointers.get(pointerId);
            pointerState4.mCurDown = true;
            InputDevice device = InputDevice.getDevice(motionEvent.getDeviceId());
            pointerState4.mHasBoundingBox = (device == null || device.getMotionRange(32) == null) ? false : true;
        }
        int i7 = size;
        int pointerCount = motionEvent.getPointerCount();
        this.mVelocity.addMovement(motionEvent);
        this.mVelocity.computeCurrentVelocity(1);
        if (this.mAltVelocity != null) {
            this.mAltVelocity.addMovement(motionEvent);
            this.mAltVelocity.computeCurrentVelocity(1);
        }
        int historySize = motionEvent.getHistorySize();
        int i8 = 0;
        while (i8 < historySize) {
            int i9 = 0;
            while (i9 < pointerCount) {
                int pointerId2 = motionEvent.getPointerId(i9);
                PointerState pointerState5 = this.mCurDown ? this.mPointers.get(pointerId2) : null;
                if (pointerState5 != null) {
                    pointerCoords3 = pointerState5.mCoords;
                } else {
                    pointerCoords3 = this.mTempCoords;
                }
                MotionEvent.PointerCoords pointerCoords5 = pointerCoords3;
                motionEvent.getHistoricalPointerCoords(i9, i8, pointerCoords5);
                if (this.mPrintCoords) {
                    pointerCoords4 = pointerCoords5;
                    pointerState2 = pointerState5;
                    i2 = i9;
                    i3 = i8;
                    i4 = historySize;
                    logCoords(TAG, action, i9, pointerCoords4, pointerId2, motionEvent);
                } else {
                    pointerCoords4 = pointerCoords5;
                    pointerState2 = pointerState5;
                    i2 = i9;
                    i3 = i8;
                    i4 = historySize;
                }
                if (pointerState2 != null) {
                    MotionEvent.PointerCoords pointerCoords6 = pointerCoords4;
                    pointerState2.addTrace(pointerCoords6.x, pointerCoords6.y, false);
                }
                i9 = i2 + 1;
                historySize = i4;
                i8 = i3;
            }
            i8++;
        }
        for (int i10 = 0; i10 < pointerCount; i10++) {
            int pointerId3 = motionEvent.getPointerId(i10);
            PointerState pointerState6 = this.mCurDown ? this.mPointers.get(pointerId3) : null;
            if (pointerState6 != null) {
                pointerCoords = pointerState6.mCoords;
            } else {
                pointerCoords = this.mTempCoords;
            }
            MotionEvent.PointerCoords pointerCoords7 = pointerCoords;
            motionEvent.getPointerCoords(i10, pointerCoords7);
            if (this.mPrintCoords) {
                pointerCoords2 = pointerCoords7;
                pointerState = pointerState6;
                i = pointerId3;
                logCoords(TAG, action, i10, pointerCoords7, pointerId3, motionEvent);
            } else {
                pointerCoords2 = pointerCoords7;
                pointerState = pointerState6;
                i = pointerId3;
            }
            if (pointerState != null) {
                MotionEvent.PointerCoords pointerCoords8 = pointerCoords2;
                pointerState.addTrace(pointerCoords8.x, pointerCoords8.y, true);
                pointerState.mXVelocity = this.mVelocity.getXVelocity(i);
                pointerState.mYVelocity = this.mVelocity.getYVelocity(i);
                this.mVelocity.getEstimator(i, pointerState.mEstimator);
                if (this.mAltVelocity != null) {
                    pointerState.mAltXVelocity = this.mAltVelocity.getXVelocity(i);
                    pointerState.mAltYVelocity = this.mAltVelocity.getYVelocity(i);
                    this.mAltVelocity.getEstimator(i, pointerState.mAltEstimator);
                }
                pointerState.mToolType = motionEvent.getToolType(i10);
                if (!pointerState.mHasBoundingBox) {
                    c = ' ';
                } else {
                    c = ' ';
                    pointerState.mBoundingLeft = motionEvent.getAxisValue(32, i10);
                    pointerState.mBoundingTop = motionEvent.getAxisValue(33, i10);
                    pointerState.mBoundingRight = motionEvent.getAxisValue(34, i10);
                    pointerState.mBoundingBottom = motionEvent.getAxisValue(35, i10);
                }
            }
        }
        if (action == 1 || action == 3 || (action & 255) == 6) {
            int i11 = (65280 & action) >> 8;
            int pointerId4 = motionEvent.getPointerId(i11);
            if (pointerId4 >= i7) {
                Slog.wtf(TAG, "Got pointer ID out of bounds: id=" + pointerId4 + " arraysize=" + i7 + " pointerindex=" + i11 + " action=0x" + Integer.toHexString(action));
                return;
            }
            PointerState pointerState7 = this.mPointers.get(pointerId4);
            pointerState7.mCurDown = false;
            if (action == 1 || action == 3) {
                this.mCurDown = false;
                this.mCurNumPointers = 0;
            } else {
                this.mCurNumPointers--;
                if (this.mActivePointerId == pointerId4) {
                    this.mActivePointerId = motionEvent.getPointerId(i11 != 0 ? 0 : 1);
                }
                pointerState7.addTrace(Float.NaN, Float.NaN, false);
            }
        }
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        onPointerEvent(motionEvent);
        if (motionEvent.getAction() == 0 && !isFocused()) {
            requestFocus();
            return true;
        }
        return true;
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent motionEvent) {
        int source = motionEvent.getSource();
        if ((source & 2) != 0) {
            onPointerEvent(motionEvent);
            return true;
        }
        if ((source & 16) != 0) {
            logMotionEvent("Joystick", motionEvent);
            return true;
        }
        if ((source & 8) != 0) {
            logMotionEvent("Position", motionEvent);
            return true;
        }
        logMotionEvent("Generic", motionEvent);
        return true;
    }

    @Override
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        if (shouldLogKey(i)) {
            int repeatCount = keyEvent.getRepeatCount();
            if (repeatCount == 0) {
                Log.i(TAG, "Key Down: " + keyEvent);
                return true;
            }
            Log.i(TAG, "Key Repeat #" + repeatCount + ": " + keyEvent);
            return true;
        }
        return super.onKeyDown(i, keyEvent);
    }

    @Override
    public boolean onKeyUp(int i, KeyEvent keyEvent) {
        if (shouldLogKey(i)) {
            Log.i(TAG, "Key Up: " + keyEvent);
            return true;
        }
        return super.onKeyUp(i, keyEvent);
    }

    private static boolean shouldLogKey(int i) {
        switch (i) {
            case 19:
            case 20:
            case 21:
            case 22:
            case 23:
                break;
            default:
                if (KeyEvent.isGamepadButton(i) || KeyEvent.isModifierKey(i)) {
                }
                break;
        }
        return true;
    }

    @Override
    public boolean onTrackballEvent(MotionEvent motionEvent) {
        logMotionEvent("Trackball", motionEvent);
        return true;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mIm.registerInputDeviceListener(this, getHandler());
        logInputDevices();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mIm.unregisterInputDeviceListener(this);
    }

    @Override
    public void onInputDeviceAdded(int i) {
        logInputDeviceState(i, "Device Added");
    }

    @Override
    public void onInputDeviceChanged(int i) {
        logInputDeviceState(i, "Device Changed");
    }

    @Override
    public void onInputDeviceRemoved(int i) {
        logInputDeviceState(i, "Device Removed");
    }

    private void logInputDevices() {
        for (int i : InputDevice.getDeviceIds()) {
            logInputDeviceState(i, "Device Enumerated");
        }
    }

    private void logInputDeviceState(int i, String str) {
        InputDevice inputDevice = this.mIm.getInputDevice(i);
        if (inputDevice != null) {
            Log.i(TAG, str + ": " + inputDevice);
            return;
        }
        Log.i(TAG, str + ": " + i);
    }

    private static final class FasterStringBuilder {
        private char[] mChars = new char[64];
        private int mLength;

        public FasterStringBuilder clear() {
            this.mLength = 0;
            return this;
        }

        public FasterStringBuilder append(String str) {
            int length = str.length();
            str.getChars(0, length, this.mChars, reserve(length));
            this.mLength += length;
            return this;
        }

        public FasterStringBuilder append(int i) {
            return append(i, 0);
        }

        public FasterStringBuilder append(int i, int i2) {
            boolean z;
            int i3;
            if (i >= 0) {
                z = false;
            } else {
                z = true;
            }
            if (z && (i = -i) < 0) {
                append("-2147483648");
                return this;
            }
            int iReserve = reserve(11);
            char[] cArr = this.mChars;
            if (i == 0) {
                cArr[iReserve] = '0';
                this.mLength++;
                return this;
            }
            if (z) {
                i3 = iReserve + 1;
                cArr[iReserve] = '-';
            } else {
                i3 = iReserve;
            }
            int i4 = 1000000000;
            int i5 = i3;
            int i6 = 10;
            while (i < i4) {
                i4 /= 10;
                i6--;
                if (i6 < i2) {
                    cArr[i5] = '0';
                    i5++;
                }
            }
            while (true) {
                int i7 = i / i4;
                i -= i7 * i4;
                i4 /= 10;
                int i8 = i5 + 1;
                cArr[i5] = (char) (i7 + 48);
                if (i4 != 0) {
                    i5 = i8;
                } else {
                    this.mLength = i8;
                    return this;
                }
            }
        }

        public FasterStringBuilder append(float f, int i) {
            int i2 = 1;
            for (int i3 = 0; i3 < i; i3++) {
                i2 *= 10;
            }
            float f2 = i2;
            float fRint = (float) (Math.rint(f * f2) / ((double) i2));
            append((int) fRint);
            if (i != 0) {
                append(".");
                double dAbs = Math.abs(fRint);
                append((int) (((float) (dAbs - Math.floor(dAbs))) * f2), i);
            }
            return this;
        }

        public String toString() {
            return new String(this.mChars, 0, this.mLength);
        }

        private int reserve(int i) {
            int i2 = this.mLength;
            int i3 = this.mLength + i;
            char[] cArr = this.mChars;
            int length = cArr.length;
            if (i3 > length) {
                char[] cArr2 = new char[length * 2];
                System.arraycopy(cArr, 0, cArr2, 0, i2);
                this.mChars = cArr2;
            }
            return i2;
        }
    }
}
