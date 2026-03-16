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
import android.support.v4.app.NotificationCompat;
import android.view.MotionEvent;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.editors.EditorDraw;
import com.android.gallery3d.filtershow.filters.FilterDrawRepresentation;

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
        resetParameter();
        setupConstants(context);
        setupTimer();
    }

    private void setupConstants(Context context) {
        Resources res = context.getResources();
        this.mDisplayRound = res.getDimensionPixelSize(R.dimen.draw_rect_round);
        this.mDisplayBorder = res.getDimensionPixelSize(R.dimen.draw_rect_border);
        this.mBorderShadowSize = res.getDimensionPixelSize(R.dimen.draw_rect_shadow);
        float edge = res.getDimensionPixelSize(R.dimen.draw_rect_border_edge);
        this.mBorderColor = res.getColor(R.color.draw_rect_border);
        this.mBorderPaint.setColor(this.mBorderColor);
        this.mBorderPaint.setStyle(Paint.Style.STROKE);
        this.mBorderPaint.setStrokeWidth(edge);
        this.mShadowPaint.setStyle(Paint.Style.FILL);
        this.mShadowPaint.setColor(-16777216);
        this.mShadowPaint.setShadowLayer(this.mBorderShadowSize, this.mBorderShadowSize, this.mBorderShadowSize, -16777216);
        this.mShadow = (NinePatchDrawable) res.getDrawable(R.drawable.geometry_shadow);
    }

    public void setEditor(EditorDraw editorDraw) {
        this.mEditorDraw = editorDraw;
    }

    public void setFilterDrawRepresentation(FilterDrawRepresentation fr) {
        this.mFRep = fr;
        this.mTmpStrokData = new FilterDrawRepresentation.StrokeData();
    }

    @Override
    public void resetParameter() {
        if (this.mFRep != null) {
            this.mFRep.clear();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getPointerCount() > 1) {
            boolean ret = super.onTouchEvent(event);
            if (this.mFRep.getCurrentDrawing() != null) {
                this.mFRep.clearCurrentSection();
                this.mEditorDraw.commitLocalRepresentation();
                return ret;
            }
            return ret;
        }
        if (event.getAction() != 0 && this.mFRep.getCurrentDrawing() == null) {
            return super.onTouchEvent(event);
        }
        if (event.getAction() == 0) {
            calcScreenMapping();
            this.mTmpPoint[0] = event.getX();
            this.mTmpPoint[1] = event.getY();
            this.mToOrig.mapPoints(this.mTmpPoint);
            this.mFRep.startNewSection(this.mTmpPoint[0], this.mTmpPoint[1]);
        }
        if (event.getAction() == 2) {
            int historySize = event.getHistorySize();
            for (int h = 0; h < historySize; h++) {
                this.mTmpPoint[0] = event.getHistoricalX(0, h);
                this.mTmpPoint[1] = event.getHistoricalY(0, h);
                this.mToOrig.mapPoints(this.mTmpPoint);
                this.mFRep.addPoint(this.mTmpPoint[0], this.mTmpPoint[1]);
            }
        }
        if (event.getAction() == 1) {
            this.mTmpPoint[0] = event.getX();
            this.mTmpPoint[1] = event.getY();
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
        int[] colors = new int[NotificationCompat.FLAG_LOCAL_ONLY];
        for (int i = 0; i < colors.length; i++) {
            int y = i / 128;
            int x = (i / 8) % 2;
            colors[i] = x == y ? -8947849 : -14540254;
        }
        Bitmap bitmap = Bitmap.createBitmap(colors, 16, 16, Bitmap.Config.ARGB_8888);
        BitmapShader bs = new BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        Paint p = new Paint();
        p.setShader(bs);
        return p;
    }

    private void setupTimer() {
        this.mHandler = new Handler(getActivity().getMainLooper());
    }

    private void scheduleWakeup(int delay) {
        this.mHandler.removeCallbacks(this.mUpdateRunnable);
        this.mHandler.postDelayed(this.mUpdateRunnable, delay);
    }

    public void displayDrawLook() {
        if (this.mFRep != null) {
            int i = this.mTmpStrokData.mColor;
            byte b = this.mTmpStrokData.mType;
            float radius = this.mTmpStrokData.mRadius;
            this.mFRep.fillStrokeParameters(this.mTmpStrokData);
            if (radius != this.mTmpStrokData.mRadius) {
                this.mTimeout = ((long) this.DISPLAY_TIME) + System.currentTimeMillis();
                scheduleWakeup(this.DISPLAY_TIME);
            }
        }
    }

    public void drawLook(Canvas canvas) {
        if (this.mFRep != null) {
            int cw = canvas.getWidth();
            int ch = canvas.getHeight();
            int centerx = cw / 2;
            int centery = ch / 2;
            this.mIconPaint.setAntiAlias(true);
            this.mIconPaint.setStyle(Paint.Style.STROKE);
            float rad = this.mRotateToScreen.mapRadius(this.mTmpStrokData.mRadius);
            RectF rec = new RectF();
            rec.set(centerx - rad, centery - rad, centerx + rad, centery + rad);
            this.mIconPaint.setColor(-16777216);
            this.mIconPaint.setStrokeWidth(5.0f);
            canvas.drawArc(rec, 0.0f, 360.0f, true, this.mIconPaint);
            this.mIconPaint.setColor(-1);
            this.mIconPaint.setStrokeWidth(3.0f);
            canvas.drawArc(rec, 0.0f, 360.0f, true, this.mIconPaint);
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        calcScreenMapping();
        if (System.currentTimeMillis() < this.mTimeout) {
            drawLook(canvas);
        }
    }
}
