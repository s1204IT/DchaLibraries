package com.android.gallery3d.filtershow.imageshow;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.NinePatchDrawable;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.editors.EditorDraw;
import com.android.gallery3d.filtershow.filters.FilterDrawRepresentation;
import com.android.gallery3d.filtershow.filters.ImageFilterDraw;
import com.android.gallery3d.ui.Log;

public class ImageDraw extends ImageShow {
    private int DISPLAY_TIME;
    private int mBorderColor;
    private Paint mBorderPaint;
    private int mBorderShadowSize;
    private Paint mCheckerdPaint;
    private int mCurrentColor;
    private float mCurrentSize;
    private float mDisplayBorder;
    private float mDisplayRound;
    private EditorDraw mEditorDraw;
    private FilterDrawRepresentation mFRep;
    private Handler mHandler;
    private Paint mIconPaint;
    private Matrix mRotateToScreen;
    private NinePatchDrawable mShadow;
    private Paint mShadowPaint;
    private long mTimeout;
    float[] mTmpPoint;
    private FilterDrawRepresentation.StrokeData mTmpStrokData;
    private Matrix mToOrig;
    public int mTouchActionState;
    private byte mType;
    Runnable mUpdateRunnable;

    public ImageDraw(Context context) {
        super(context);
        this.mCurrentColor = -65536;
        this.mCurrentSize = 40.0f;
        this.mType = (byte) 0;
        this.mCheckerdPaint = makeCheckedPaint();
        this.mShadowPaint = new Paint();
        this.mIconPaint = new Paint();
        this.mBorderPaint = new Paint();
        this.mTmpStrokData = new FilterDrawRepresentation.StrokeData();
        this.DISPLAY_TIME = 500;
        this.mRotateToScreen = new Matrix();
        this.mUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                ImageDraw.this.invalidate();
            }
        };
        this.mTmpPoint = new float[2];
        this.mTouchActionState = 0;
        resetParameter();
        setupConstants(context);
        setupTimer();
    }

    private void setupConstants(Context context) {
        Resources resources = context.getResources();
        this.mDisplayRound = resources.getDimensionPixelSize(R.dimen.draw_rect_round);
        this.mDisplayBorder = resources.getDimensionPixelSize(R.dimen.draw_rect_border);
        this.mBorderShadowSize = resources.getDimensionPixelSize(R.dimen.draw_rect_shadow);
        float dimensionPixelSize = resources.getDimensionPixelSize(R.dimen.draw_rect_border_edge);
        this.mBorderColor = resources.getColor(R.color.draw_rect_border);
        this.mBorderPaint.setColor(this.mBorderColor);
        this.mBorderPaint.setStyle(Paint.Style.STROKE);
        this.mBorderPaint.setStrokeWidth(dimensionPixelSize);
        this.mShadowPaint.setStyle(Paint.Style.FILL);
        this.mShadowPaint.setColor(-16777216);
        this.mShadowPaint.setShadowLayer(this.mBorderShadowSize, this.mBorderShadowSize, this.mBorderShadowSize, -16777216);
        this.mShadow = (NinePatchDrawable) resources.getDrawable(R.drawable.geometry_shadow);
    }

    public void setEditor(EditorDraw editorDraw) {
        this.mEditorDraw = editorDraw;
    }

    public void setFilterDrawRepresentation(FilterDrawRepresentation filterDrawRepresentation) {
        this.mFRep = filterDrawRepresentation;
        this.mTmpStrokData = new FilterDrawRepresentation.StrokeData();
    }

    @Override
    public void resetParameter() {
        if (this.mFRep != null) {
            this.mFRep.clear();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (this.mFRep == null) {
            return true;
        }
        if (motionEvent.getPointerCount() > 1) {
            boolean zOnTouchEvent = super.onTouchEvent(motionEvent);
            if (this.mFRep.getCurrentDrawing() != null) {
                this.mFRep.clearCurrentSection();
                this.mEditorDraw.commitLocalRepresentation();
            }
            return zOnTouchEvent;
        }
        if (motionEvent.getAction() != 0 && this.mFRep.getCurrentDrawing() == null) {
            return super.onTouchEvent(motionEvent);
        }
        this.mTouchActionState = motionEvent.getAction();
        if (motionEvent.getAction() == 0) {
            calcScreenMapping();
            this.mTmpPoint[0] = motionEvent.getX();
            this.mTmpPoint[1] = motionEvent.getY();
            this.mToOrig.mapPoints(this.mTmpPoint);
            this.mFRep.startNewSection(this.mTmpPoint[0], this.mTmpPoint[1]);
        }
        if (motionEvent.getAction() == 2) {
            int historySize = motionEvent.getHistorySize();
            for (int i = 0; i < historySize; i++) {
                this.mTmpPoint[0] = motionEvent.getHistoricalX(0, i);
                this.mTmpPoint[1] = motionEvent.getHistoricalY(0, i);
                this.mToOrig.mapPoints(this.mTmpPoint);
                this.mFRep.addPoint(this.mTmpPoint[0], this.mTmpPoint[1]);
            }
        }
        if (motionEvent.getAction() == 1) {
            this.mTmpPoint[0] = motionEvent.getX();
            this.mTmpPoint[1] = motionEvent.getY();
            this.mToOrig.mapPoints(this.mTmpPoint);
            this.mFRep.endSection(this.mTmpPoint[0], this.mTmpPoint[1]);
        }
        this.mEditorDraw.commitLocalRepresentation();
        invalidate();
        return true;
    }

    private void calcScreenMapping() {
        this.mToOrig = getScreenToImageMatrix(true);
        this.mToOrig.invert(this.mRotateToScreen);
    }

    private static Paint makeCheckedPaint() {
        int[] iArr = new int[256];
        for (int i = 0; i < iArr.length; i++) {
            iArr[i] = (i / 8) % 2 == i / 128 ? -8947849 : -14540254;
        }
        BitmapShader bitmapShader = new BitmapShader(Bitmap.createBitmap(iArr, 16, 16, Bitmap.Config.ARGB_8888), Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        Paint paint = new Paint();
        paint.setShader(bitmapShader);
        return paint;
    }

    private void setupTimer() {
        this.mHandler = new Handler(getActivity().getMainLooper());
    }

    private void scheduleWakeup(int i) {
        this.mHandler.removeCallbacks(this.mUpdateRunnable);
        this.mHandler.postDelayed(this.mUpdateRunnable, i);
    }

    public void displayDrawLook() {
        if (this.mFRep == null) {
            return;
        }
        int i = this.mTmpStrokData.mColor;
        byte b = this.mTmpStrokData.mType;
        float f = this.mTmpStrokData.mRadius;
        this.mFRep.fillStrokeParameters(this.mTmpStrokData);
        if (f != this.mTmpStrokData.mRadius) {
            this.mTimeout = ((long) this.DISPLAY_TIME) + System.currentTimeMillis();
            scheduleWakeup(this.DISPLAY_TIME);
        }
    }

    public void drawLook(Canvas canvas) {
        if (this.mFRep == null) {
            return;
        }
        int width = canvas.getWidth();
        int height = canvas.getHeight() / 2;
        this.mIconPaint.setAntiAlias(true);
        this.mIconPaint.setStyle(Paint.Style.STROKE);
        float fMapRadius = this.mRotateToScreen.mapRadius(this.mTmpStrokData.mRadius);
        RectF rectF = new RectF();
        float f = width / 2;
        float f2 = height;
        rectF.set(f - fMapRadius, f2 - fMapRadius, f + fMapRadius, f2 + fMapRadius);
        this.mIconPaint.setColor(-16777216);
        this.mIconPaint.setStrokeWidth(5.0f);
        canvas.drawArc(rectF, 0.0f, 360.0f, true, this.mIconPaint);
        this.mIconPaint.setColor(-1);
        this.mIconPaint.setStrokeWidth(3.0f);
        canvas.drawArc(rectF, 0.0f, 360.0f, true, this.mIconPaint);
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        calcScreenMapping();
        if (System.currentTimeMillis() < this.mTimeout) {
            drawLook(canvas);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean z) {
        if (this.mFRep == null) {
            return;
        }
        if (!z && this.mTouchActionState == 3 && this.mFRep.getCurrentDrawing() != null) {
            this.mFRep.endSection(this.mTmpPoint[0], this.mTmpPoint[1]);
            this.mEditorDraw.commitLocalRepresentation();
        }
        Log.d("ImageDraw", "hasWindowFocus=" + z + " mTouchState = " + this.mTouchActionState);
    }

    @Override
    protected void onVisibilityChanged(View view, int i) {
        if (i == 0) {
            ImageFilterDraw.disableCache(true);
        }
    }
}
