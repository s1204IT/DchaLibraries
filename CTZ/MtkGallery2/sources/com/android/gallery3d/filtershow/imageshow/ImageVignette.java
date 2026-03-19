package com.android.gallery3d.filtershow.imageshow;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.MotionEvent;
import com.android.gallery3d.filtershow.editors.EditorVignette;
import com.android.gallery3d.filtershow.filters.FilterVignetteRepresentation;

public class ImageVignette extends ImageShow {
    private int mActiveHandle;
    private EditorVignette mEditorVignette;
    EclipseControl mElipse;
    private OvalSpaceAdapter mScreenOval;
    private FilterVignetteRepresentation mVignetteRep;

    public ImageVignette(Context context) {
        super(context);
        this.mScreenOval = new OvalSpaceAdapter();
        this.mActiveHandle = -1;
        this.mElipse = new EclipseControl(context);
    }

    public ImageVignette(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mScreenOval = new OvalSpaceAdapter();
        this.mActiveHandle = -1;
        this.mElipse = new EclipseControl(context);
    }

    static class OvalSpaceAdapter implements Oval {
        int mImgHeight;
        int mImgWidth;
        private Oval mOval;
        float[] mTmp = new float[2];
        float mTmpRadiusX;
        float mTmpRadiusY;
        Matrix mToImage;
        Matrix mToScr;

        OvalSpaceAdapter() {
        }

        public void setImageOval(Oval oval) {
            this.mOval = oval;
        }

        public void setTransform(Matrix matrix, Matrix matrix2, int i, int i2) {
            this.mToScr = matrix;
            this.mToImage = matrix2;
            this.mImgWidth = i;
            this.mImgHeight = i2;
            this.mTmpRadiusX = getRadiusX();
            this.mTmpRadiusY = getRadiusY();
        }

        @Override
        public void setCenter(float f, float f2) {
            this.mTmp[0] = f;
            this.mTmp[1] = f2;
            this.mToImage.mapPoints(this.mTmp);
            this.mOval.setCenter(this.mTmp[0] / this.mImgWidth, this.mTmp[1] / this.mImgHeight);
        }

        @Override
        public void setRadius(float f, float f2) {
            float[] fArr = this.mTmp;
            this.mTmpRadiusX = f;
            fArr[0] = f;
            float[] fArr2 = this.mTmp;
            this.mTmpRadiusY = f2;
            fArr2[1] = f2;
            this.mToImage.mapVectors(this.mTmp);
            this.mOval.setRadius(this.mTmp[0] / this.mImgWidth, this.mTmp[1] / this.mImgHeight);
        }

        @Override
        public float getCenterX() {
            this.mTmp[0] = this.mOval.getCenterX() * this.mImgWidth;
            this.mTmp[1] = this.mOval.getCenterY() * this.mImgHeight;
            this.mToScr.mapPoints(this.mTmp);
            return this.mTmp[0];
        }

        @Override
        public float getCenterY() {
            this.mTmp[0] = this.mOval.getCenterX() * this.mImgWidth;
            this.mTmp[1] = this.mOval.getCenterY() * this.mImgHeight;
            this.mToScr.mapPoints(this.mTmp);
            return this.mTmp[1];
        }

        @Override
        public float getRadiusX() {
            this.mTmp[0] = this.mOval.getRadiusX() * this.mImgWidth;
            this.mTmp[1] = this.mOval.getRadiusY() * this.mImgHeight;
            this.mToScr.mapVectors(this.mTmp);
            if (this.mTmp[0] == 0.0f) {
                return Math.abs(this.mOval.getRadiusX() * this.mTmp[1]);
            }
            if (this.mTmp[1] == 0.0f) {
                return Math.abs(this.mOval.getRadiusX() * this.mTmp[0]);
            }
            return Math.abs(this.mTmp[0]);
        }

        @Override
        public float getRadiusY() {
            this.mTmp[0] = this.mOval.getRadiusX() * this.mImgWidth;
            this.mTmp[1] = this.mOval.getRadiusY() * this.mImgHeight;
            this.mToScr.mapVectors(this.mTmp);
            if (this.mTmp[0] == 0.0f) {
                return Math.abs(this.mOval.getRadiusY() * this.mTmp[1]);
            }
            if (this.mTmp[1] == 0.0f) {
                return Math.abs(this.mOval.getRadiusY() * this.mTmp[0]);
            }
            return Math.abs(this.mTmp[1]);
        }

        @Override
        public void setRadiusY(float f) {
            this.mTmp[0] = this.mTmpRadiusX;
            float[] fArr = this.mTmp;
            this.mTmpRadiusY = f;
            fArr[1] = f;
            this.mToImage.mapVectors(this.mTmp);
            this.mOval.setRadiusX(this.mTmp[0] / this.mImgWidth);
            this.mOval.setRadiusY(this.mTmp[1] / this.mImgHeight);
        }

        @Override
        public void setRadiusX(float f) {
            float[] fArr = this.mTmp;
            this.mTmpRadiusX = f;
            fArr[0] = f;
            this.mTmp[1] = this.mTmpRadiusY;
            this.mToImage.mapVectors(this.mTmp);
            this.mOval.setRadiusX(this.mTmp[0] / this.mImgWidth);
            this.mOval.setRadiusY(this.mTmp[1] / this.mImgHeight);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        MasterImage.getImage().getOriginalBounds().width();
        MasterImage.getImage().getOriginalBounds().height();
        int actionMasked = motionEvent.getActionMasked();
        if (this.mActiveHandle == -1) {
            if (actionMasked != 0) {
                return super.onTouchEvent(motionEvent);
            }
            if (motionEvent.getPointerCount() == 1) {
                this.mActiveHandle = this.mElipse.getCloseHandle(motionEvent.getX(), motionEvent.getY());
            }
            if (this.mActiveHandle == -1) {
                return super.onTouchEvent(motionEvent);
            }
        } else if (actionMasked == 1) {
            this.mActiveHandle = -1;
        }
        float x = motionEvent.getX();
        float y = motionEvent.getY();
        this.mElipse.setScrImageInfo(new Matrix(), MasterImage.getImage().getOriginalBounds());
        this.mElipse.setMatrix(getScreenToImageMatrix(true));
        boolean z = false;
        switch (actionMasked) {
            case 0:
                this.mElipse.actionDown(x, y, this.mScreenOval);
                break;
            case 1:
            case 2:
                this.mElipse.actionMove(this.mActiveHandle, x, y, this.mScreenOval);
                setRepresentation(this.mVignetteRep);
                z = true;
                break;
        }
        if (!z) {
            computeEllipses();
        }
        invalidate();
        return true;
    }

    public void setRepresentation(FilterVignetteRepresentation filterVignetteRepresentation) {
        this.mVignetteRep = filterVignetteRepresentation;
        this.mScreenOval.setImageOval(this.mVignetteRep);
        computeEllipses();
    }

    public void computeEllipses() {
        if (this.mVignetteRep == null) {
            return;
        }
        float fWidth = MasterImage.getImage().getOriginalBounds().width();
        float fHeight = MasterImage.getImage().getOriginalBounds().height();
        Matrix screenToImageMatrix = getScreenToImageMatrix(false);
        Matrix matrix = new Matrix();
        screenToImageMatrix.invert(matrix);
        this.mScreenOval.setTransform(matrix, screenToImageMatrix, (int) fWidth, (int) fHeight);
        this.mElipse.setCenter(this.mScreenOval.getCenterX(), this.mScreenOval.getCenterY());
        this.mElipse.setRadius(this.mScreenOval.getRadiusX(), this.mScreenOval.getRadiusY());
        this.mEditorVignette.commitLocalRepresentation();
    }

    public void setEditor(EditorVignette editorVignette) {
        this.mEditorVignette = editorVignette;
        this.mVignetteRep = null;
    }

    @Override
    public void onSizeChanged(int i, int i2, int i3, int i4) {
        super.onSizeChanged(i, i2, i3, i4);
        computeEllipses();
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.mVignetteRep == null) {
            return;
        }
        float fWidth = MasterImage.getImage().getOriginalBounds().width();
        float fHeight = MasterImage.getImage().getOriginalBounds().height();
        Matrix screenToImageMatrix = getScreenToImageMatrix(false);
        Matrix matrix = new Matrix();
        screenToImageMatrix.invert(matrix);
        this.mScreenOval.setTransform(matrix, screenToImageMatrix, (int) fWidth, (int) fHeight);
        this.mElipse.setCenter(this.mScreenOval.getCenterX(), this.mScreenOval.getCenterY());
        this.mElipse.setRadius(this.mScreenOval.getRadiusX(), this.mScreenOval.getRadiusY());
        this.mElipse.draw(canvas);
    }
}
