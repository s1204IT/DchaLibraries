package com.android.camera.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;
import com.android.camera.debug.Log;
import com.android.camera.ui.PreviewStatusListener;
import com.android.camera.util.CameraUtil;
import com.android.camera2.R;

public class FaceView extends View implements FocusIndicator, Rotatable, PreviewStatusListener.PreviewAreaChangedListener {
    private static final int MSG_SWITCH_FACES = 1;
    private static final int SWITCH_DELAY = 70;
    private static final Log.Tag TAG = new Log.Tag("FaceView");
    private final boolean LOGV;
    private volatile boolean mBlocked;
    private int mColor;
    private int mDisplayOrientation;
    private Camera.Face[] mFaces;
    private Handler mHandler;
    private Matrix mMatrix;
    private boolean mMirror;
    private int mOrientation;
    private Paint mPaint;
    private boolean mPause;
    private Camera.Face[] mPendingFaces;
    private final RectF mPreviewArea;
    private RectF mRect;
    private boolean mStateSwitchPending;

    public FaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.LOGV = false;
        this.mMatrix = new Matrix();
        this.mRect = new RectF();
        this.mStateSwitchPending = false;
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        FaceView.this.mStateSwitchPending = false;
                        FaceView.this.mFaces = FaceView.this.mPendingFaces;
                        FaceView.this.invalidate();
                        break;
                }
            }
        };
        this.mPreviewArea = new RectF();
        Resources res = getResources();
        this.mColor = res.getColor(R.color.face_detect_start);
        this.mPaint = new Paint();
        this.mPaint.setAntiAlias(true);
        this.mPaint.setStyle(Paint.Style.STROKE);
        this.mPaint.setStrokeWidth(res.getDimension(R.dimen.face_circle_stroke));
    }

    public void setFaces(Camera.Face[] faces) {
        if (!this.mPause) {
            if (this.mFaces != null && ((faces.length > 0 && this.mFaces.length == 0) || (faces.length == 0 && this.mFaces.length > 0))) {
                this.mPendingFaces = faces;
                if (!this.mStateSwitchPending) {
                    this.mStateSwitchPending = true;
                    this.mHandler.sendEmptyMessageDelayed(1, 70L);
                    return;
                }
                return;
            }
            if (this.mStateSwitchPending) {
                this.mStateSwitchPending = false;
                this.mHandler.removeMessages(1);
            }
            this.mFaces = faces;
            invalidate();
        }
    }

    public void setDisplayOrientation(int orientation) {
        this.mDisplayOrientation = orientation;
    }

    @Override
    public void setOrientation(int orientation, boolean animation) {
        this.mOrientation = orientation;
        invalidate();
    }

    public void setMirror(boolean mirror) {
        this.mMirror = mirror;
    }

    public boolean faceExists() {
        return this.mFaces != null && this.mFaces.length > 0;
    }

    @Override
    public void showStart() {
        invalidate();
    }

    @Override
    public void showSuccess(boolean timeout) {
        invalidate();
    }

    @Override
    public void showFail(boolean timeout) {
        invalidate();
    }

    @Override
    public void clear() {
        this.mFaces = null;
        invalidate();
    }

    public void pause() {
        this.mPause = true;
    }

    public void resume() {
        this.mPause = false;
    }

    public void setBlockDraw(boolean block) {
        this.mBlocked = block;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!this.mBlocked && this.mFaces != null && this.mFaces.length > 0) {
            int rw = (int) this.mPreviewArea.width();
            int rh = (int) this.mPreviewArea.height();
            if ((rh > rw && (this.mDisplayOrientation == 0 || this.mDisplayOrientation == 180)) || (rw > rh && (this.mDisplayOrientation == 90 || this.mDisplayOrientation == 270))) {
                rw = rh;
                rh = rw;
            }
            CameraUtil.prepareMatrix(this.mMatrix, this.mMirror, this.mDisplayOrientation, rw, rh);
            canvas.save();
            this.mMatrix.postRotate(this.mOrientation);
            canvas.rotate(-this.mOrientation);
            for (int i = 0; i < this.mFaces.length; i++) {
                if (this.mFaces[i].score >= 50) {
                    this.mRect.set(this.mFaces[i].rect);
                    this.mMatrix.mapRect(this.mRect);
                    this.mPaint.setColor(this.mColor);
                    this.mRect.offset(this.mPreviewArea.left, this.mPreviewArea.top);
                    canvas.drawRect(this.mRect, this.mPaint);
                }
            }
            canvas.restore();
        }
        super.onDraw(canvas);
    }

    @Override
    public void onPreviewAreaChanged(RectF previewArea) {
        this.mPreviewArea.set(previewArea);
    }
}
