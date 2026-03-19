package com.android.gallery3d.filtershow.imageshow;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.MotionEvent;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.editors.EditorGrad;
import com.android.gallery3d.filtershow.filters.FilterGradRepresentation;

public class ImageGrad extends ImageShow {
    private int mActiveHandle;
    private EditorGrad mEditorGrad;
    private GradControl mEllipse;
    private FilterGradRepresentation mGradRep;
    private float mMinTouchDist;
    float[] mPointsX;
    float[] mPointsY;
    Matrix mToScr;

    public ImageGrad(Context context) {
        super(context);
        this.mActiveHandle = -1;
        this.mToScr = new Matrix();
        this.mPointsX = new float[16];
        this.mPointsY = new float[16];
        this.mMinTouchDist = context.getResources().getDimensionPixelSize(R.dimen.gradcontrol_min_touch_dist);
        this.mEllipse = new GradControl(context);
        this.mEllipse.setShowReshapeHandles(false);
    }

    public ImageGrad(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mActiveHandle = -1;
        this.mToScr = new Matrix();
        this.mPointsX = new float[16];
        this.mPointsY = new float[16];
        this.mMinTouchDist = context.getResources().getDimensionPixelSize(R.dimen.gradcontrol_min_touch_dist);
        this.mEllipse = new GradControl(context);
        this.mEllipse.setShowReshapeHandles(false);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        int actionMasked = motionEvent.getActionMasked();
        if (this.mActiveHandle == -1) {
            if (actionMasked != 0) {
                return super.onTouchEvent(motionEvent);
            }
            if (motionEvent.getPointerCount() == 1) {
                this.mActiveHandle = this.mEllipse.getCloseHandle(motionEvent.getX(), motionEvent.getY());
                if (this.mActiveHandle == -1) {
                    float x = motionEvent.getX();
                    float y = motionEvent.getY();
                    float f = Float.MAX_VALUE;
                    int i = -1;
                    for (int i2 = 0; i2 < this.mPointsX.length; i2++) {
                        if (this.mPointsX[i2] != -1.0f) {
                            float fHypot = (float) Math.hypot(x - this.mPointsX[i2], y - this.mPointsY[i2]);
                            if (f > fHypot) {
                                i = i2;
                                f = fHypot;
                            }
                        }
                    }
                    if (f > this.mMinTouchDist) {
                        i = -1;
                    }
                    if (i != -1) {
                        this.mGradRep.setSelectedPoint(i);
                        resetImageCaches(this);
                        this.mEditorGrad.updateSeekBar(this.mGradRep);
                        this.mEditorGrad.commitLocalRepresentation();
                        invalidate();
                    }
                }
            }
            if (this.mActiveHandle == -1) {
                return super.onTouchEvent(motionEvent);
            }
        } else if (actionMasked == 1) {
            this.mActiveHandle = -1;
        }
        float x2 = motionEvent.getX();
        float y2 = motionEvent.getY();
        this.mEllipse.setScrImageInfo(getScreenToImageMatrix(true), MasterImage.getImage().getOriginalBounds());
        switch (actionMasked) {
            case 0:
                this.mEllipse.actionDown(x2, y2, this.mGradRep);
                break;
            case 1:
            case 2:
                this.mEllipse.actionMove(this.mActiveHandle, x2, y2, this.mGradRep);
                setRepresentation(this.mGradRep);
                break;
        }
        invalidate();
        this.mEditorGrad.commitLocalRepresentation();
        return true;
    }

    public void setRepresentation(FilterGradRepresentation filterGradRepresentation) {
        this.mGradRep = filterGradRepresentation;
        getScreenToImageMatrix(false).invert(this.mToScr);
        float[] fArr = {this.mGradRep.getPoint1X(), this.mGradRep.getPoint1Y()};
        float[] fArr2 = {this.mGradRep.getPoint2X(), this.mGradRep.getPoint2Y()};
        if (fArr[0] == -1.0f) {
            float fWidth = MasterImage.getImage().getOriginalBounds().width() / 2;
            float fHeight = MasterImage.getImage().getOriginalBounds().height() / 2;
            float fMin = Math.min(fWidth, fHeight) * 0.4f;
            float f = fHeight - fMin;
            this.mGradRep.setPoint1(fWidth, f);
            float f2 = fHeight + fMin;
            this.mGradRep.setPoint2(fWidth, f2);
            fArr[0] = fWidth;
            fArr[1] = f;
            this.mToScr.mapPoints(fArr);
            if (getWidth() != 0) {
                this.mEllipse.setPoint1(fArr[0], fArr[1]);
                fArr2[0] = fWidth;
                fArr2[1] = f2;
                this.mToScr.mapPoints(fArr2);
                this.mEllipse.setPoint2(fArr2[0], fArr2[1]);
            }
            this.mEditorGrad.commitLocalRepresentation();
            return;
        }
        this.mToScr.mapPoints(fArr);
        this.mToScr.mapPoints(fArr2);
        this.mEllipse.setPoint1(fArr[0], fArr[1]);
        this.mEllipse.setPoint2(fArr2[0], fArr2[1]);
    }

    public void drawOtherPoints(Canvas canvas) {
        computCenterLocations();
        for (int i = 0; i < this.mPointsX.length; i++) {
            if (this.mPointsX[i] != -1.0f) {
                this.mEllipse.paintGrayPoint(canvas, this.mPointsX[i], this.mPointsY[i]);
            }
        }
    }

    public void computCenterLocations() {
        int[] xPos1 = this.mGradRep.getXPos1();
        int[] yPos1 = this.mGradRep.getYPos1();
        int[] xPos2 = this.mGradRep.getXPos2();
        int[] yPos2 = this.mGradRep.getYPos2();
        int selectedPoint = this.mGradRep.getSelectedPoint();
        boolean[] mask = this.mGradRep.getMask();
        float[] fArr = new float[2];
        for (int i = 0; i < mask.length; i++) {
            if (selectedPoint != i && mask[i]) {
                fArr[0] = (xPos1[i] + xPos2[i]) / 2;
                fArr[1] = (yPos1[i] + yPos2[i]) / 2;
                this.mToScr.mapPoints(fArr);
                this.mPointsX[i] = fArr[0];
                this.mPointsY[i] = fArr[1];
            } else {
                this.mPointsX[i] = -1.0f;
            }
        }
    }

    public void setEditor(EditorGrad editorGrad) {
        this.mEditorGrad = editorGrad;
        this.mGradRep = null;
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.mGradRep == null) {
            return;
        }
        setRepresentation(this.mGradRep);
        this.mEllipse.draw(canvas);
        drawOtherPoints(canvas);
    }
}
