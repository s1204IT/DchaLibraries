package com.android.internal.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.hardware.input.InputManager;
import android.os.SystemProperties;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManagerPolicy;
import java.util.ArrayList;

public class PointerLocationView extends View implements InputManager.InputDeviceListener, WindowManagerPolicy.PointerEventListener {
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

        public void addTrace(float x, float y, boolean current) {
            int traceCapacity = this.mTraceX.length;
            if (this.mTraceCount == traceCapacity) {
                int traceCapacity2 = traceCapacity * 2;
                float[] newTraceX = new float[traceCapacity2];
                System.arraycopy(this.mTraceX, 0, newTraceX, 0, this.mTraceCount);
                this.mTraceX = newTraceX;
                float[] newTraceY = new float[traceCapacity2];
                System.arraycopy(this.mTraceY, 0, newTraceY, 0, this.mTraceCount);
                this.mTraceY = newTraceY;
                boolean[] newTraceCurrent = new boolean[traceCapacity2];
                System.arraycopy(this.mTraceCurrent, 0, newTraceCurrent, 0, this.mTraceCount);
                this.mTraceCurrent = newTraceCurrent;
            }
            this.mTraceX[this.mTraceCount] = x;
            this.mTraceY[this.mTraceCount] = y;
            this.mTraceCurrent[this.mTraceCount] = current;
            this.mTraceCount++;
        }
    }

    public PointerLocationView(Context c) {
        super(c);
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
        this.mIm = (InputManager) c.getSystemService("input");
        this.mVC = ViewConfiguration.get(c);
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
        PointerState ps = new PointerState();
        this.mPointers.add(ps);
        this.mActivePointerId = 0;
        this.mVelocity = VelocityTracker.obtain();
        String altStrategy = SystemProperties.get(ALT_STRATEGY_PROPERY_KEY);
        if (altStrategy.length() != 0) {
            Log.d(TAG, "Comparing default velocity tracker strategy with " + altStrategy);
            this.mAltVelocity = VelocityTracker.obtain(altStrategy);
        } else {
            this.mAltVelocity = null;
        }
    }

    public void setPrintCoords(boolean state) {
        this.mPrintCoords = state;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        this.mTextPaint.getFontMetricsInt(this.mTextMetrics);
        this.mHeaderBottom = (-this.mTextMetrics.ascent) + this.mTextMetrics.descent + 2;
    }

    private void drawOval(Canvas canvas, float x, float y, float major, float minor, float angle, Paint paint) {
        canvas.save(1);
        canvas.rotate((float) (((double) (180.0f * angle)) / 3.141592653589793d), x, y);
        this.mReusableOvalRect.left = x - (minor / 2.0f);
        this.mReusableOvalRect.right = (minor / 2.0f) + x;
        this.mReusableOvalRect.top = y - (major / 2.0f);
        this.mReusableOvalRect.bottom = (major / 2.0f) + y;
        canvas.drawOval(this.mReusableOvalRect, paint);
        canvas.restore();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int itemW = w / 7;
        int base = (-this.mTextMetrics.ascent) + 1;
        int bottom = this.mHeaderBottom;
        int NP = this.mPointers.size();
        if (this.mActivePointerId >= 0) {
            PointerState ps = this.mPointers.get(this.mActivePointerId);
            canvas.drawRect(0.0f, 0.0f, itemW - 1, bottom, this.mTextBackgroundPaint);
            canvas.drawText(this.mText.clear().append("P: ").append(this.mCurNumPointers).append(" / ").append(this.mMaxNumPointers).toString(), 1.0f, base, this.mTextPaint);
            int N = ps.mTraceCount;
            if ((!this.mCurDown || !ps.mCurDown) && N != 0) {
                float dx = ps.mTraceX[N - 1] - ps.mTraceX[0];
                float dy = ps.mTraceY[N - 1] - ps.mTraceY[0];
                canvas.drawRect(itemW, 0.0f, (itemW * 2) - 1, bottom, Math.abs(dx) < ((float) this.mVC.getScaledTouchSlop()) ? this.mTextBackgroundPaint : this.mTextLevelPaint);
                canvas.drawText(this.mText.clear().append("dX: ").append(dx, 1).toString(), itemW + 1, base, this.mTextPaint);
                canvas.drawRect(itemW * 2, 0.0f, (itemW * 3) - 1, bottom, Math.abs(dy) < ((float) this.mVC.getScaledTouchSlop()) ? this.mTextBackgroundPaint : this.mTextLevelPaint);
                canvas.drawText(this.mText.clear().append("dY: ").append(dy, 1).toString(), (itemW * 2) + 1, base, this.mTextPaint);
            } else {
                canvas.drawRect(itemW, 0.0f, (itemW * 2) - 1, bottom, this.mTextBackgroundPaint);
                canvas.drawText(this.mText.clear().append("X: ").append(ps.mCoords.x, 1).toString(), itemW + 1, base, this.mTextPaint);
                canvas.drawRect(itemW * 2, 0.0f, (itemW * 3) - 1, bottom, this.mTextBackgroundPaint);
                canvas.drawText(this.mText.clear().append("Y: ").append(ps.mCoords.y, 1).toString(), (itemW * 2) + 1, base, this.mTextPaint);
            }
            canvas.drawRect(itemW * 3, 0.0f, (itemW * 4) - 1, bottom, this.mTextBackgroundPaint);
            canvas.drawText(this.mText.clear().append("Xv: ").append(ps.mXVelocity, 3).toString(), (itemW * 3) + 1, base, this.mTextPaint);
            canvas.drawRect(itemW * 4, 0.0f, (itemW * 5) - 1, bottom, this.mTextBackgroundPaint);
            canvas.drawText(this.mText.clear().append("Yv: ").append(ps.mYVelocity, 3).toString(), (itemW * 4) + 1, base, this.mTextPaint);
            canvas.drawRect(itemW * 5, 0.0f, (itemW * 6) - 1, bottom, this.mTextBackgroundPaint);
            canvas.drawRect(itemW * 5, 0.0f, ((itemW * 5) + (ps.mCoords.pressure * itemW)) - 1.0f, bottom, this.mTextLevelPaint);
            canvas.drawText(this.mText.clear().append("Prs: ").append(ps.mCoords.pressure, 2).toString(), (itemW * 5) + 1, base, this.mTextPaint);
            canvas.drawRect(itemW * 6, 0.0f, w, bottom, this.mTextBackgroundPaint);
            canvas.drawRect(itemW * 6, 0.0f, ((itemW * 6) + (ps.mCoords.size * itemW)) - 1.0f, bottom, this.mTextLevelPaint);
            canvas.drawText(this.mText.clear().append("Size: ").append(ps.mCoords.size, 2).toString(), (itemW * 6) + 1, base, this.mTextPaint);
        }
        for (int p = 0; p < NP; p++) {
            PointerState ps2 = this.mPointers.get(p);
            int N2 = ps2.mTraceCount;
            float lastX = 0.0f;
            float lastY = 0.0f;
            boolean haveLast = false;
            boolean drawn = false;
            this.mPaint.setARGB(255, 128, 255, 255);
            for (int i = 0; i < N2; i++) {
                float x = ps2.mTraceX[i];
                float y = ps2.mTraceY[i];
                if (Float.isNaN(x)) {
                    haveLast = false;
                } else {
                    if (haveLast) {
                        canvas.drawLine(lastX, lastY, x, y, this.mPathPaint);
                        Paint paint = ps2.mTraceCurrent[i] ? this.mCurrentPointPaint : this.mPaint;
                        canvas.drawPoint(lastX, lastY, paint);
                        drawn = true;
                    }
                    lastX = x;
                    lastY = y;
                    haveLast = true;
                }
            }
            if (drawn) {
                this.mPaint.setARGB(128, 128, 0, 128);
                float lx = ps2.mEstimator.estimateX(-0.08f);
                float ly = ps2.mEstimator.estimateY(-0.08f);
                for (int i2 = -3; i2 <= 2; i2++) {
                    float x2 = ps2.mEstimator.estimateX(i2 * 0.02f);
                    float y2 = ps2.mEstimator.estimateY(i2 * 0.02f);
                    canvas.drawLine(lx, ly, x2, y2, this.mPaint);
                    lx = x2;
                    ly = y2;
                }
                this.mPaint.setARGB(255, 255, 64, 128);
                float xVel = ps2.mXVelocity * 16.0f;
                float yVel = ps2.mYVelocity * 16.0f;
                canvas.drawLine(lastX, lastY, lastX + xVel, lastY + yVel, this.mPaint);
                if (this.mAltVelocity != null) {
                    this.mPaint.setARGB(128, 0, 128, 128);
                    float lx2 = ps2.mAltEstimator.estimateX(-0.08f);
                    float ly2 = ps2.mAltEstimator.estimateY(-0.08f);
                    for (int i3 = -3; i3 <= 2; i3++) {
                        float x3 = ps2.mAltEstimator.estimateX(i3 * 0.02f);
                        float y3 = ps2.mAltEstimator.estimateY(i3 * 0.02f);
                        canvas.drawLine(lx2, ly2, x3, y3, this.mPaint);
                        lx2 = x3;
                        ly2 = y3;
                    }
                    this.mPaint.setARGB(255, 64, 255, 128);
                    float xVel2 = ps2.mAltXVelocity * 16.0f;
                    float yVel2 = ps2.mAltYVelocity * 16.0f;
                    canvas.drawLine(lastX, lastY, lastX + xVel2, lastY + yVel2, this.mPaint);
                }
            }
            if (this.mCurDown && ps2.mCurDown) {
                canvas.drawLine(0.0f, ps2.mCoords.y, getWidth(), ps2.mCoords.y, this.mTargetPaint);
                canvas.drawLine(ps2.mCoords.x, 0.0f, ps2.mCoords.x, getHeight(), this.mTargetPaint);
                int pressureLevel = (int) (ps2.mCoords.pressure * 255.0f);
                this.mPaint.setARGB(255, pressureLevel, 255, 255 - pressureLevel);
                canvas.drawPoint(ps2.mCoords.x, ps2.mCoords.y, this.mPaint);
                this.mPaint.setARGB(255, pressureLevel, 255 - pressureLevel, 128);
                drawOval(canvas, ps2.mCoords.x, ps2.mCoords.y, ps2.mCoords.touchMajor, ps2.mCoords.touchMinor, ps2.mCoords.orientation, this.mPaint);
                this.mPaint.setARGB(255, pressureLevel, 128, 255 - pressureLevel);
                drawOval(canvas, ps2.mCoords.x, ps2.mCoords.y, ps2.mCoords.toolMajor, ps2.mCoords.toolMinor, ps2.mCoords.orientation, this.mPaint);
                float arrowSize = ps2.mCoords.toolMajor * 0.7f;
                if (arrowSize < 20.0f) {
                    arrowSize = 20.0f;
                }
                this.mPaint.setARGB(255, pressureLevel, 255, 0);
                float orientationVectorX = (float) (Math.sin(ps2.mCoords.orientation) * ((double) arrowSize));
                float orientationVectorY = (float) ((-Math.cos(ps2.mCoords.orientation)) * ((double) arrowSize));
                if (ps2.mToolType == 2 || ps2.mToolType == 4) {
                    canvas.drawLine(ps2.mCoords.x, ps2.mCoords.y, ps2.mCoords.x + orientationVectorX, ps2.mCoords.y + orientationVectorY, this.mPaint);
                } else {
                    canvas.drawLine(ps2.mCoords.x - orientationVectorX, ps2.mCoords.y - orientationVectorY, ps2.mCoords.x + orientationVectorX, ps2.mCoords.y + orientationVectorY, this.mPaint);
                }
                float tiltScale = (float) Math.sin(ps2.mCoords.getAxisValue(25));
                canvas.drawCircle(ps2.mCoords.x + (orientationVectorX * tiltScale), ps2.mCoords.y + (orientationVectorY * tiltScale), 3.0f, this.mPaint);
                if (ps2.mHasBoundingBox) {
                    canvas.drawRect(ps2.mBoundingLeft, ps2.mBoundingTop, ps2.mBoundingRight, ps2.mBoundingBottom, this.mPaint);
                }
            }
        }
    }

    private void logMotionEvent(String type, MotionEvent event) {
        int action = event.getAction();
        int N = event.getHistorySize();
        int NI = event.getPointerCount();
        for (int historyPos = 0; historyPos < N; historyPos++) {
            for (int i = 0; i < NI; i++) {
                int id = event.getPointerId(i);
                event.getHistoricalPointerCoords(i, historyPos, this.mTempCoords);
                logCoords(type, action, i, this.mTempCoords, id, event);
            }
        }
        for (int i2 = 0; i2 < NI; i2++) {
            int id2 = event.getPointerId(i2);
            event.getPointerCoords(i2, this.mTempCoords);
            logCoords(type, action, i2, this.mTempCoords, id2, event);
        }
    }

    private void logCoords(String type, int action, int index, MotionEvent.PointerCoords coords, int id, MotionEvent event) {
        String prefix;
        int toolType = event.getToolType(index);
        int buttonState = event.getButtonState();
        switch (action & 255) {
            case 0:
                prefix = "DOWN";
                break;
            case 1:
                prefix = "UP";
                break;
            case 2:
                prefix = "MOVE";
                break;
            case 3:
                prefix = "CANCEL";
                break;
            case 4:
                prefix = "OUTSIDE";
                break;
            case 5:
                if (index == ((65280 & action) >> 8)) {
                    prefix = "DOWN";
                } else {
                    prefix = "MOVE";
                }
                break;
            case 6:
                if (index == ((65280 & action) >> 8)) {
                    prefix = "UP";
                } else {
                    prefix = "MOVE";
                }
                break;
            case 7:
                prefix = "HOVER MOVE";
                break;
            case 8:
                prefix = "SCROLL";
                break;
            case 9:
                prefix = "HOVER ENTER";
                break;
            case 10:
                prefix = "HOVER EXIT";
                break;
            default:
                prefix = Integer.toString(action);
                break;
        }
        Log.i(TAG, this.mText.clear().append(type).append(" id ").append(id + 1).append(": ").append(prefix).append(" (").append(coords.x, 3).append(", ").append(coords.y, 3).append(") Pressure=").append(coords.pressure, 3).append(" Size=").append(coords.size, 3).append(" TouchMajor=").append(coords.touchMajor, 3).append(" TouchMinor=").append(coords.touchMinor, 3).append(" ToolMajor=").append(coords.toolMajor, 3).append(" ToolMinor=").append(coords.toolMinor, 3).append(" Orientation=").append((float) (((double) (coords.orientation * 180.0f)) / 3.141592653589793d), 1).append("deg").append(" Tilt=").append((float) (((double) (coords.getAxisValue(25) * 180.0f)) / 3.141592653589793d), 1).append("deg").append(" Distance=").append(coords.getAxisValue(24), 1).append(" VScroll=").append(coords.getAxisValue(9), 1).append(" HScroll=").append(coords.getAxisValue(10), 1).append(" BoundingBox=[(").append(event.getAxisValue(32), 3).append(", ").append(event.getAxisValue(33), 3).append(")").append(", (").append(event.getAxisValue(34), 3).append(", ").append(event.getAxisValue(35), 3).append(")]").append(" ToolType=").append(MotionEvent.toolTypeToString(toolType)).append(" ButtonState=").append(MotionEvent.buttonStateToString(buttonState)).toString());
    }

    @Override
    public void onPointerEvent(MotionEvent event) {
        MotionEvent.PointerCoords coords;
        MotionEvent.PointerCoords coords2;
        int action = event.getAction();
        int NP = this.mPointers.size();
        if (action == 0 || (action & 255) == 5) {
            int index = (65280 & action) >> 8;
            if (action == 0) {
                for (int p = 0; p < NP; p++) {
                    PointerState ps = this.mPointers.get(p);
                    ps.clearTrace();
                    ps.mCurDown = false;
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
            int id = event.getPointerId(index);
            while (NP <= id) {
                this.mPointers.add(new PointerState());
                NP++;
            }
            if (this.mActivePointerId < 0 || !this.mPointers.get(this.mActivePointerId).mCurDown) {
                this.mActivePointerId = id;
            }
            PointerState ps2 = this.mPointers.get(id);
            ps2.mCurDown = true;
            InputDevice device = InputDevice.getDevice(event.getDeviceId());
            ps2.mHasBoundingBox = (device == null || device.getMotionRange(32) == null) ? false : true;
        }
        int NI = event.getPointerCount();
        this.mVelocity.addMovement(event);
        this.mVelocity.computeCurrentVelocity(1);
        if (this.mAltVelocity != null) {
            this.mAltVelocity.addMovement(event);
            this.mAltVelocity.computeCurrentVelocity(1);
        }
        int N = event.getHistorySize();
        for (int historyPos = 0; historyPos < N; historyPos++) {
            for (int i = 0; i < NI; i++) {
                int id2 = event.getPointerId(i);
                PointerState ps3 = this.mCurDown ? this.mPointers.get(id2) : null;
                if (ps3 != null) {
                    coords2 = ps3.mCoords;
                } else {
                    coords2 = this.mTempCoords;
                }
                event.getHistoricalPointerCoords(i, historyPos, coords2);
                if (this.mPrintCoords) {
                    logCoords(TAG, action, i, coords2, id2, event);
                }
                if (ps3 != null) {
                    ps3.addTrace(coords2.x, coords2.y, false);
                }
            }
        }
        for (int i2 = 0; i2 < NI; i2++) {
            int id3 = event.getPointerId(i2);
            PointerState ps4 = this.mCurDown ? this.mPointers.get(id3) : null;
            if (ps4 != null) {
                coords = ps4.mCoords;
            } else {
                coords = this.mTempCoords;
            }
            event.getPointerCoords(i2, coords);
            if (this.mPrintCoords) {
                logCoords(TAG, action, i2, coords, id3, event);
            }
            if (ps4 != null) {
                ps4.addTrace(coords.x, coords.y, true);
                ps4.mXVelocity = this.mVelocity.getXVelocity(id3);
                ps4.mYVelocity = this.mVelocity.getYVelocity(id3);
                this.mVelocity.getEstimator(id3, ps4.mEstimator);
                if (this.mAltVelocity != null) {
                    ps4.mAltXVelocity = this.mAltVelocity.getXVelocity(id3);
                    ps4.mAltYVelocity = this.mAltVelocity.getYVelocity(id3);
                    this.mAltVelocity.getEstimator(id3, ps4.mAltEstimator);
                }
                ps4.mToolType = event.getToolType(i2);
                if (ps4.mHasBoundingBox) {
                    ps4.mBoundingLeft = event.getAxisValue(32, i2);
                    ps4.mBoundingTop = event.getAxisValue(33, i2);
                    ps4.mBoundingRight = event.getAxisValue(34, i2);
                    ps4.mBoundingBottom = event.getAxisValue(35, i2);
                }
            }
        }
        if (action == 1 || action == 3 || (action & 255) == 6) {
            int index2 = (65280 & action) >> 8;
            int id4 = event.getPointerId(index2);
            PointerState ps5 = this.mPointers.get(id4);
            ps5.mCurDown = false;
            if (action == 1 || action == 3) {
                this.mCurDown = false;
                this.mCurNumPointers = 0;
            } else {
                this.mCurNumPointers--;
                if (this.mActivePointerId == id4) {
                    this.mActivePointerId = event.getPointerId(index2 == 0 ? 1 : 0);
                }
                ps5.addTrace(Float.NaN, Float.NaN, false);
            }
        }
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        onPointerEvent(event);
        if (event.getAction() == 0 && !isFocused()) {
            requestFocus();
            return true;
        }
        return true;
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        int source = event.getSource();
        if ((source & 2) != 0) {
            onPointerEvent(event);
            return true;
        }
        if ((source & 16) != 0) {
            logMotionEvent("Joystick", event);
            return true;
        }
        if ((source & 8) != 0) {
            logMotionEvent("Position", event);
            return true;
        }
        logMotionEvent("Generic", event);
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (shouldLogKey(keyCode)) {
            int repeatCount = event.getRepeatCount();
            if (repeatCount == 0) {
                Log.i(TAG, "Key Down: " + event);
            } else {
                Log.i(TAG, "Key Repeat #" + repeatCount + ": " + event);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (!shouldLogKey(keyCode)) {
            return super.onKeyUp(keyCode, event);
        }
        Log.i(TAG, "Key Up: " + event);
        return true;
    }

    private static boolean shouldLogKey(int keyCode) {
        switch (keyCode) {
            case 19:
            case 20:
            case 21:
            case 22:
            case 23:
                break;
            default:
                if (KeyEvent.isGamepadButton(keyCode) || KeyEvent.isModifierKey(keyCode)) {
                }
                break;
        }
        return true;
    }

    @Override
    public boolean onTrackballEvent(MotionEvent event) {
        logMotionEvent("Trackball", event);
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
    public void onInputDeviceAdded(int deviceId) {
        logInputDeviceState(deviceId, "Device Added");
    }

    @Override
    public void onInputDeviceChanged(int deviceId) {
        logInputDeviceState(deviceId, "Device Changed");
    }

    @Override
    public void onInputDeviceRemoved(int deviceId) {
        logInputDeviceState(deviceId, "Device Removed");
    }

    private void logInputDevices() {
        int[] deviceIds = InputDevice.getDeviceIds();
        for (int i : deviceIds) {
            logInputDeviceState(i, "Device Enumerated");
        }
    }

    private void logInputDeviceState(int deviceId, String state) {
        InputDevice device = this.mIm.getInputDevice(deviceId);
        if (device != null) {
            Log.i(TAG, state + ": " + device);
        } else {
            Log.i(TAG, state + ": " + deviceId);
        }
    }

    private static final class FasterStringBuilder {
        private char[] mChars = new char[64];
        private int mLength;

        public FasterStringBuilder clear() {
            this.mLength = 0;
            return this;
        }

        public FasterStringBuilder append(String value) {
            int valueLength = value.length();
            int index = reserve(valueLength);
            value.getChars(0, valueLength, this.mChars, index);
            this.mLength += valueLength;
            return this;
        }

        public FasterStringBuilder append(int value) {
            return append(value, 0);
        }

        public FasterStringBuilder append(int value, int zeroPadWidth) {
            boolean negative = value < 0;
            if (negative && (value = -value) < 0) {
                append("-2147483648");
            } else {
                int index = reserve(11);
                char[] chars = this.mChars;
                if (value == 0) {
                    int i = index + 1;
                    chars[index] = '0';
                    this.mLength++;
                } else {
                    if (negative) {
                        chars[index] = '-';
                        index++;
                    }
                    int divisor = 1000000000;
                    int numberWidth = 10;
                    int index2 = index;
                    while (value < divisor) {
                        divisor /= 10;
                        numberWidth--;
                        if (numberWidth < zeroPadWidth) {
                            chars[index2] = '0';
                            index2++;
                        }
                    }
                    do {
                        int index3 = index2;
                        int digit = value / divisor;
                        value -= digit * divisor;
                        divisor /= 10;
                        index2 = index3 + 1;
                        chars[index3] = (char) (digit + 48);
                    } while (divisor != 0);
                    this.mLength = index2;
                }
            }
            return this;
        }

        public FasterStringBuilder append(float value, int precision) {
            int scale = 1;
            for (int i = 0; i < precision; i++) {
                scale *= 10;
            }
            float value2 = (float) (Math.rint(scale * value) / ((double) scale));
            append((int) value2);
            if (precision != 0) {
                append(".");
                float value3 = Math.abs(value2);
                append((int) (scale * ((float) (((double) value3) - Math.floor(value3)))), precision);
            }
            return this;
        }

        public String toString() {
            return new String(this.mChars, 0, this.mLength);
        }

        private int reserve(int length) {
            int oldLength = this.mLength;
            int newLength = this.mLength + length;
            char[] oldChars = this.mChars;
            int oldCapacity = oldChars.length;
            if (newLength > oldCapacity) {
                int newCapacity = oldCapacity * 2;
                char[] newChars = new char[newCapacity];
                System.arraycopy(oldChars, 0, newChars, 0, oldLength);
                this.mChars = newChars;
            }
            return oldLength;
        }
    }
}
