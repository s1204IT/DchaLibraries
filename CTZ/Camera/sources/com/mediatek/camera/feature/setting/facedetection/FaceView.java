package com.mediatek.camera.feature.setting.facedetection;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import com.mediatek.camera.R;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.utils.CameraUtil;
import com.mediatek.camera.common.utils.CoordinatesTransform;

public class FaceView extends View {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(FaceView.class.getSimpleName());
    private int mDisplayOrientation;
    private Drawable mFaceIndicator;
    private Face[] mFaces;
    private boolean mMirror;
    private int mPreviewHeight;
    private int mPreviewWidth;
    private boolean mReallyShown;

    public FaceView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mReallyShown = false;
        this.mFaceIndicator = context.getResources().getDrawable(R.drawable.ic_face_detection_focusing);
    }

    public boolean hasReallyShown() {
        return this.mReallyShown;
    }

    public void resetReallyShown() {
        this.mReallyShown = false;
    }

    public void setFaces(Face[] faceArr) {
        this.mFaces = faceArr;
        invalidate();
    }

    public void setDisplayOrientation(int i, int i2) {
        this.mDisplayOrientation = i;
        this.mMirror = CameraUtil.isCameraFacingFront(getContext(), i2);
    }

    public void updatePreviewSize(int i, int i2) {
        LogHelper.d(TAG, "[updatePreviewSize] width = " + i + ", height = " + i2);
        this.mPreviewWidth = i;
        this.mPreviewHeight = i2;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        this.mReallyShown = true;
        if (this.mFaces != null && this.mFaces.length > 0) {
            for (int i = 0; i < this.mFaces.length; i++) {
                Rect rectNormalizedPreviewToUi = CoordinatesTransform.normalizedPreviewToUi(this.mFaces[i].rect, this.mPreviewWidth, this.mPreviewHeight, this.mDisplayOrientation, this.mMirror);
                this.mFaceIndicator.setBounds(rectNormalizedPreviewToUi.left, rectNormalizedPreviewToUi.top, rectNormalizedPreviewToUi.right, rectNormalizedPreviewToUi.bottom);
                this.mFaceIndicator.draw(canvas);
            }
        }
        super.onDraw(canvas);
    }
}
