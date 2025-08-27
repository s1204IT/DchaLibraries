package com.android.quickstep.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.v4.graphics.ColorUtils;
import android.util.AttributeSet;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.LauncherState;
import com.android.launcher3.R;
import com.android.launcher3.anim.Interpolators;
import com.android.launcher3.uioverrides.OverviewState;
import com.android.launcher3.util.Themes;
import com.android.launcher3.views.ScrimView;

/* loaded from: classes.dex */
public class ShelfScrimView extends ScrimView {
    private boolean mDrawingFlatColor;
    private final int mEndAlpha;
    private final float mMaxScrimAlpha;
    private int mMinSize;
    private float mMoveThreshold;
    private final Paint mPaint;
    private final float mRadius;
    private int mRemainingScreenColor;
    private final Path mRemainingScreenPath;
    private boolean mRemainingScreenPathValid;
    private float mScrimMoveFactor;
    private int mShelfColor;
    private final Path mTempPath;
    private final int mThresholdAlpha;

    public ShelfScrimView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mScrimMoveFactor = 0.0f;
        this.mTempPath = new Path();
        this.mRemainingScreenPath = new Path();
        this.mRemainingScreenPathValid = false;
        this.mMaxScrimAlpha = LauncherState.OVERVIEW.getWorkspaceScrimAlpha(this.mLauncher);
        this.mEndAlpha = Color.alpha(this.mEndScrim);
        this.mThresholdAlpha = Themes.getAttrInteger(context, R.attr.allAppsInterimScrimAlpha);
        this.mRadius = this.mLauncher.getResources().getDimension(R.dimen.shelf_surface_radius);
        this.mPaint = new Paint(1);
        this.mDrawingFlatColor = true;
    }

    @Override // android.view.View
    protected void onSizeChanged(int i, int i2, int i3, int i4) {
        super.onSizeChanged(i, i2, i3, i4);
        this.mRemainingScreenPathValid = false;
    }

    @Override // com.android.launcher3.views.ScrimView
    public void reInitUi() {
        DeviceProfile deviceProfile = this.mLauncher.getDeviceProfile();
        this.mDrawingFlatColor = deviceProfile.isVerticalBarLayout();
        if (!this.mDrawingFlatColor) {
            this.mMoveThreshold = 1.0f - (OverviewState.getDefaultSwipeHeight(this.mLauncher) / this.mLauncher.getAllAppsController().getShiftRange());
            this.mMinSize = deviceProfile.hotseatBarSizePx + deviceProfile.getInsets().bottom;
            this.mRemainingScreenPathValid = false;
            updateColors();
        }
        updateDragHandleAlpha();
        invalidate();
    }

    @Override // com.android.launcher3.views.ScrimView
    public void updateColors() {
        super.updateColors();
        if (this.mDrawingFlatColor) {
            return;
        }
        if (this.mProgress >= this.mMoveThreshold) {
            this.mScrimMoveFactor = 1.0f;
            if (this.mProgress >= 1.0f) {
                this.mShelfColor = 0;
            } else {
                this.mShelfColor = ColorUtils.setAlphaComponent(this.mEndScrim, Math.round(this.mThresholdAlpha * Interpolators.ACCEL_2.getInterpolation((1.0f - this.mProgress) / (1.0f - this.mMoveThreshold))));
            }
            this.mRemainingScreenColor = 0;
            return;
        }
        if (this.mProgress <= 0.0f) {
            this.mScrimMoveFactor = 0.0f;
            this.mShelfColor = this.mCurrentFlatColor;
            this.mRemainingScreenColor = 0;
        } else {
            this.mScrimMoveFactor = this.mProgress / this.mMoveThreshold;
            this.mRemainingScreenColor = ColorUtils.setAlphaComponent(this.mScrimColor, Math.round((1.0f - this.mScrimMoveFactor) * this.mMaxScrimAlpha * 255.0f));
            this.mShelfColor = ColorUtils.compositeColors(ColorUtils.setAlphaComponent(this.mEndScrim, this.mEndAlpha - Math.round((this.mEndAlpha - this.mThresholdAlpha) * this.mScrimMoveFactor)), this.mRemainingScreenColor);
        }
    }

    @Override // com.android.launcher3.views.ScrimView, android.view.View
    protected void onDraw(Canvas canvas) {
        float fDrawBackground = drawBackground(canvas);
        if (this.mDragHandle != null) {
            canvas.translate(0.0f, -fDrawBackground);
            this.mDragHandle.draw(canvas);
            canvas.translate(0.0f, fDrawBackground);
        }
    }

    private float drawBackground(Canvas canvas) {
        if (this.mDrawingFlatColor) {
            if (this.mCurrentFlatColor != 0) {
                canvas.drawColor(this.mCurrentFlatColor);
            }
            return 0.0f;
        }
        if (this.mShelfColor == 0) {
            return 0.0f;
        }
        if (this.mScrimMoveFactor <= 0.0f) {
            canvas.drawColor(this.mShelfColor);
            return getHeight();
        }
        float height = getHeight() - this.mMinSize;
        float f = (this.mScrimMoveFactor * height) - this.mDragHandleSize;
        if (this.mRemainingScreenColor != 0) {
            if (!this.mRemainingScreenPathValid) {
                this.mTempPath.reset();
                this.mTempPath.addRoundRect(0.0f, height, getWidth(), 10.0f + getHeight() + this.mRadius, this.mRadius, this.mRadius, Path.Direction.CW);
                this.mRemainingScreenPath.reset();
                this.mRemainingScreenPath.addRect(0.0f, 0.0f, getWidth(), getHeight(), Path.Direction.CW);
                this.mRemainingScreenPath.op(this.mTempPath, Path.Op.DIFFERENCE);
            }
            float f2 = height - f;
            canvas.translate(0.0f, -f2);
            this.mPaint.setColor(this.mRemainingScreenColor);
            canvas.drawPath(this.mRemainingScreenPath, this.mPaint);
            canvas.translate(0.0f, f2);
        }
        this.mPaint.setColor(this.mShelfColor);
        canvas.drawRoundRect(0.0f, f, getWidth(), getHeight() + this.mRadius, this.mRadius, this.mRadius, this.mPaint);
        return (height - this.mDragHandleSize) - f;
    }
}
