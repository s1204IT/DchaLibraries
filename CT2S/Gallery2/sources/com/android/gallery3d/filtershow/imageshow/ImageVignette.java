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

    public ImageVignette(Context context, AttributeSet attrs) {
        super(context, attrs);
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

        public void setTransform(Matrix toScr, Matrix toImage, int imgWidth, int imgHeight) {
            this.mToScr = toScr;
            this.mToImage = toImage;
            this.mImgWidth = imgWidth;
            this.mImgHeight = imgHeight;
            this.mTmpRadiusX = getRadiusX();
            this.mTmpRadiusY = getRadiusY();
        }

        @Override
        public void setCenter(float x, float y) {
            this.mTmp[0] = x;
            this.mTmp[1] = y;
            this.mToImage.mapPoints(this.mTmp);
            this.mOval.setCenter(this.mTmp[0] / this.mImgWidth, this.mTmp[1] / this.mImgHeight);
        }

        @Override
        public void setRadius(float w, float h) {
            float[] fArr = this.mTmp;
            this.mTmpRadiusX = w;
            fArr[0] = w;
            float[] fArr2 = this.mTmp;
            this.mTmpRadiusY = h;
            fArr2[1] = h;
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
            return Math.abs(this.mTmp[0]);
        }

        @Override
        public float getRadiusY() {
            this.mTmp[0] = this.mOval.getRadiusX() * this.mImgWidth;
            this.mTmp[1] = this.mOval.getRadiusY() * this.mImgHeight;
            this.mToScr.mapVectors(this.mTmp);
            return Math.abs(this.mTmp[1]);
        }

        @Override
        public void setRadiusY(float y) {
            this.mTmp[0] = this.mTmpRadiusX;
            float[] fArr = this.mTmp;
            this.mTmpRadiusY = y;
            fArr[1] = y;
            this.mToImage.mapVectors(this.mTmp);
            this.mOval.setRadiusX(this.mTmp[0] / this.mImgWidth);
            this.mOval.setRadiusY(this.mTmp[1] / this.mImgHeight);
        }

        @Override
        public void setRadiusX(float x) {
            float[] fArr = this.mTmp;
            this.mTmpRadiusX = x;
            fArr[0] = x;
            this.mTmp[1] = this.mTmpRadiusY;
            this.mToImage.mapVectors(this.mTmp);
            this.mOval.setRadiusX(this.mTmp[0] / this.mImgWidth);
            this.mOval.setRadiusY(this.mTmp[1] / this.mImgHeight);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        MasterImage.getImage().getOriginalBounds().width();
        MasterImage.getImage().getOriginalBounds().height();
        int mask = event.getActionMasked();
        if (this.mActiveHandle == -1) {
            if (mask != 0) {
                return super.onTouchEvent(event);
            }
            if (event.getPointerCount() == 1) {
                this.mActiveHandle = this.mElipse.getCloseHandle(event.getX(), event.getY());
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
        float x = event.getX();
        float y = event.getY();
        this.mElipse.setScrImageInfo(new Matrix(), MasterImage.getImage().getOriginalBounds());
        boolean didComputeEllipses = false;
        switch (mask) {
            case 0:
                this.mElipse.actionDown(x, y, this.mScreenOval);
                break;
            case 1:
            case 2:
                this.mElipse.actionMove(this.mActiveHandle, x, y, this.mScreenOval);
                setRepresentation(this.mVignetteRep);
                didComputeEllipses = true;
                break;
        }
        if (!didComputeEllipses) {
            computeEllipses();
        }
        invalidate();
        return true;
    }

    public void setRepresentation(FilterVignetteRepresentation vignetteRep) {
        this.mVignetteRep = vignetteRep;
        this.mScreenOval.setImageOval(this.mVignetteRep);
        computeEllipses();
    }

    public void computeEllipses() {
        if (this.mVignetteRep != null) {
            float w = MasterImage.getImage().getOriginalBounds().width();
            float h = MasterImage.getImage().getOriginalBounds().height();
            Matrix toImg = getScreenToImageMatrix(false);
            Matrix toScr = new Matrix();
            toImg.invert(toScr);
            this.mScreenOval.setTransform(toScr, toImg, (int) w, (int) h);
            this.mElipse.setCenter(this.mScreenOval.getCenterX(), this.mScreenOval.getCenterY());
            this.mElipse.setRadius(this.mScreenOval.getRadiusX(), this.mScreenOval.getRadiusY());
            this.mEditorVignette.commitLocalRepresentation();
        }
    }

    public void setEditor(EditorVignette editorVignette) {
        this.mEditorVignette = editorVignette;
    }

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        computeEllipses();
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.mVignetteRep != null) {
            float w = MasterImage.getImage().getOriginalBounds().width();
            float h = MasterImage.getImage().getOriginalBounds().height();
            Matrix toImg = getScreenToImageMatrix(false);
            Matrix toScr = new Matrix();
            toImg.invert(toScr);
            this.mScreenOval.setTransform(toScr, toImg, (int) w, (int) h);
            this.mElipse.setCenter(this.mScreenOval.getCenterX(), this.mScreenOval.getCenterY());
            this.mElipse.setRadius(this.mScreenOval.getRadiusX(), this.mScreenOval.getRadiusY());
            this.mElipse.draw(canvas);
        }
    }
}
