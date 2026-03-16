package com.android.gallery3d.filtershow.imageshow;

import android.content.Context;
import android.content.res.Resources;
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
        Resources res = context.getResources();
        this.mMinTouchDist = res.getDimensionPixelSize(R.dimen.gradcontrol_min_touch_dist);
        this.mEllipse = new GradControl(context);
        this.mEllipse.setShowReshapeHandles(false);
    }

    public ImageGrad(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mActiveHandle = -1;
        this.mToScr = new Matrix();
        this.mPointsX = new float[16];
        this.mPointsY = new float[16];
        Resources res = context.getResources();
        this.mMinTouchDist = res.getDimensionPixelSize(R.dimen.gradcontrol_min_touch_dist);
        this.mEllipse = new GradControl(context);
        this.mEllipse.setShowReshapeHandles(false);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int mask = event.getActionMasked();
        if (this.mActiveHandle == -1) {
            if (mask != 0) {
                return super.onTouchEvent(event);
            }
            if (event.getPointerCount() == 1) {
                this.mActiveHandle = this.mEllipse.getCloseHandle(event.getX(), event.getY());
                if (this.mActiveHandle == -1) {
                    float x = event.getX();
                    float y = event.getY();
                    float min_d = Float.MAX_VALUE;
                    int pos = -1;
                    for (int i = 0; i < this.mPointsX.length; i++) {
                        if (this.mPointsX[i] != -1.0f) {
                            float d = (float) Math.hypot(x - this.mPointsX[i], y - this.mPointsY[i]);
                            if (min_d > d) {
                                min_d = d;
                                pos = i;
                            }
                        }
                    }
                    if (min_d > this.mMinTouchDist) {
                        pos = -1;
                    }
                    if (pos != -1) {
                        this.mGradRep.setSelectedPoint(pos);
                        resetImageCaches(this);
                        this.mEditorGrad.updateSeekBar(this.mGradRep);
                        this.mEditorGrad.commitLocalRepresentation();
                        invalidate();
                    }
                }
            }
            if (this.mActiveHandle == -1) {
                return super.onTouchEvent(event);
            }
        } else {
            switch (mask) {
                case 1:
                    this.mActiveHandle = -1;
                    break;
            }
        }
        float x2 = event.getX();
        float y2 = event.getY();
        this.mEllipse.setScrImageInfo(getScreenToImageMatrix(true), MasterImage.getImage().getOriginalBounds());
        switch (mask) {
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

    public void setRepresentation(FilterGradRepresentation pointRep) {
        this.mGradRep = pointRep;
        Matrix toImg = getScreenToImageMatrix(false);
        toImg.invert(this.mToScr);
        float[] c1 = {this.mGradRep.getPoint1X(), this.mGradRep.getPoint1Y()};
        float[] c2 = {this.mGradRep.getPoint2X(), this.mGradRep.getPoint2Y()};
        if (c1[0] == -1.0f) {
            float cx = MasterImage.getImage().getOriginalBounds().width() / 2;
            float cy = MasterImage.getImage().getOriginalBounds().height() / 2;
            float rx = Math.min(cx, cy) * 0.4f;
            this.mGradRep.setPoint1(cx, cy - rx);
            this.mGradRep.setPoint2(cx, cy + rx);
            c1[0] = cx;
            c1[1] = cy - rx;
            this.mToScr.mapPoints(c1);
            if (getWidth() != 0) {
                this.mEllipse.setPoint1(c1[0], c1[1]);
                c2[0] = cx;
                c2[1] = cy + rx;
                this.mToScr.mapPoints(c2);
                this.mEllipse.setPoint2(c2[0], c2[1]);
            }
            this.mEditorGrad.commitLocalRepresentation();
            return;
        }
        this.mToScr.mapPoints(c1);
        this.mToScr.mapPoints(c2);
        this.mEllipse.setPoint1(c1[0], c1[1]);
        this.mEllipse.setPoint2(c2[0], c2[1]);
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
        int[] x1 = this.mGradRep.getXPos1();
        int[] y1 = this.mGradRep.getYPos1();
        int[] x2 = this.mGradRep.getXPos2();
        int[] y2 = this.mGradRep.getYPos2();
        int selected = this.mGradRep.getSelectedPoint();
        boolean[] m = this.mGradRep.getMask();
        float[] c = new float[2];
        for (int i = 0; i < m.length; i++) {
            if (selected == i || !m[i]) {
                this.mPointsX[i] = -1.0f;
            } else {
                c[0] = (x1[i] + x2[i]) / 2;
                c[1] = (y1[i] + y2[i]) / 2;
                this.mToScr.mapPoints(c);
                this.mPointsX[i] = c[0];
                this.mPointsY[i] = c[1];
            }
        }
    }

    public void setEditor(EditorGrad editorGrad) {
        this.mEditorGrad = editorGrad;
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.mGradRep != null) {
            setRepresentation(this.mGradRep);
            this.mEllipse.draw(canvas);
            drawOtherPoints(canvas);
        }
    }
}
