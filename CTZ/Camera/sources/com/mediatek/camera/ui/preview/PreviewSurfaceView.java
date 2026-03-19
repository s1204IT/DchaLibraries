package com.mediatek.camera.ui.preview;

import android.content.Context;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.Display;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

public class PreviewSurfaceView extends SurfaceView {
    private double mAspectRatio;
    private int mPreviewHeight;
    private int mPreviewWidth;

    public PreviewSurfaceView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mAspectRatio = 0.0d;
        this.mPreviewWidth = 0;
        this.mPreviewHeight = 0;
    }

    public void setAspectRatio(double d) {
        if (this.mAspectRatio != d) {
            this.mAspectRatio = d;
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int i, int i2) {
        int size = View.MeasureSpec.getSize(i);
        int size2 = View.MeasureSpec.getSize(i2);
        boolean z = size > size2;
        int iRound = z ? size : size2;
        if (z) {
            size = size2;
        }
        if (this.mAspectRatio > 0.0d) {
            if (Math.abs(this.mAspectRatio - findFullscreenRatio(getContext())) <= 0.03d) {
                double d = iRound;
                double d2 = size;
                if (d < this.mAspectRatio * d2) {
                    iRound = Math.round(((float) (d2 * this.mAspectRatio)) / 2.0f) * 2;
                } else {
                    size = Math.round(((float) (d / this.mAspectRatio)) / 2.0f) * 2;
                }
            } else {
                double d3 = iRound;
                double d4 = size;
                if (d3 > this.mAspectRatio * d4) {
                    iRound = Math.round(((float) (d4 * this.mAspectRatio)) / 2.0f) * 2;
                } else {
                    size = Math.round(((float) (d3 / this.mAspectRatio)) / 2.0f) * 2;
                }
            }
        }
        if (z) {
            int i3 = iRound;
            iRound = size;
            size = i3;
        }
        int i4 = this.mPreviewWidth;
        int i5 = this.mPreviewHeight;
        int i6 = getContext().getResources().getConfiguration().orientation;
        setMeasuredDimension(size, iRound);
    }

    protected boolean isFullScreenPreview(double d) {
        if (Math.abs(d - findFullscreenRatio(getContext())) <= 0.03d) {
            return true;
        }
        return false;
    }

    private static double findFullscreenRatio(Context context) {
        Display defaultDisplay = ((WindowManager) context.getSystemService("window")).getDefaultDisplay();
        Point point = new Point();
        defaultDisplay.getRealSize(point);
        if (point.x > point.y) {
            return ((double) point.x) / ((double) point.y);
        }
        return ((double) point.y) / ((double) point.x);
    }
}
