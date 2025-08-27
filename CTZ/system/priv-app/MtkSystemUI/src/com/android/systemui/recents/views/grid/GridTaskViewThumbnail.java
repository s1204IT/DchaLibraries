package com.android.systemui.recents.views.grid;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Path;
import android.util.AttributeSet;
import com.android.systemui.R;
import com.android.systemui.recents.views.TaskViewThumbnail;

/* loaded from: classes.dex */
public class GridTaskViewThumbnail extends TaskViewThumbnail {
    private final Path mRestBackgroundOutline;
    private final Path mThumbnailOutline;
    private boolean mUpdateThumbnailOutline;

    public GridTaskViewThumbnail(Context context) {
        this(context, null);
    }

    public GridTaskViewThumbnail(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public GridTaskViewThumbnail(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public GridTaskViewThumbnail(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mThumbnailOutline = new Path();
        this.mRestBackgroundOutline = new Path();
        this.mUpdateThumbnailOutline = true;
        this.mCornerRadius = getResources().getDimensionPixelSize(R.dimen.recents_grid_task_view_rounded_corners_radius);
    }

    @Override // com.android.systemui.recents.views.TaskViewThumbnail
    public void onTaskViewSizeChanged(int i, int i2) {
        this.mUpdateThumbnailOutline = true;
        super.onTaskViewSizeChanged(i, i2);
    }

    @Override // com.android.systemui.recents.views.TaskViewThumbnail
    public void updateThumbnailMatrix() {
        this.mUpdateThumbnailOutline = true;
        super.updateThumbnailMatrix();
    }

    private void updateThumbnailOutline() throws Resources.NotFoundException {
        int dimensionPixelSize = getResources().getDimensionPixelSize(R.dimen.recents_grid_task_view_header_height);
        int iWidth = this.mTaskViewRect.width();
        int iHeight = this.mTaskViewRect.height() - dimensionPixelSize;
        int iMin = Math.min(iWidth, (int) (this.mThumbnailRect.width() * this.mThumbnailScale));
        int iMin2 = Math.min(iHeight, (int) (this.mThumbnailRect.height() * this.mThumbnailScale));
        if (this.mBitmapShader == null || iMin <= 0 || iMin2 <= 0) {
            createThumbnailPath(0, 0, iWidth, iHeight, this.mThumbnailOutline);
            return;
        }
        int i = 0 + iMin;
        int i2 = 0 + iMin2;
        createThumbnailPath(0, 0, i, i2, this.mThumbnailOutline);
        if (iMin < iWidth) {
            int iMax = Math.max(0, i - this.mCornerRadius);
            this.mRestBackgroundOutline.reset();
            float f = iMax;
            this.mRestBackgroundOutline.moveTo(f, 0.0f);
            float f2 = i;
            this.mRestBackgroundOutline.lineTo(f2, 0.0f);
            this.mRestBackgroundOutline.lineTo(f2, i2 - this.mCornerRadius);
            float f3 = i2;
            this.mRestBackgroundOutline.arcTo(i - (this.mCornerRadius * 2), i2 - (this.mCornerRadius * 2), f2, f3, 0.0f, 90.0f, false);
            this.mRestBackgroundOutline.lineTo(f, f3);
            this.mRestBackgroundOutline.lineTo(f, 0.0f);
            this.mRestBackgroundOutline.close();
        }
        if (iMin2 < iHeight) {
            int iMax2 = Math.max(0, iMin2 - this.mCornerRadius);
            this.mRestBackgroundOutline.reset();
            float f4 = iMax2;
            this.mRestBackgroundOutline.moveTo(0.0f, f4);
            float f5 = i;
            this.mRestBackgroundOutline.lineTo(f5, f4);
            this.mRestBackgroundOutline.lineTo(f5, i2 - this.mCornerRadius);
            float f6 = i2;
            this.mRestBackgroundOutline.arcTo(i - (this.mCornerRadius * 2), i2 - (this.mCornerRadius * 2), f5, f6, 0.0f, 90.0f, false);
            this.mRestBackgroundOutline.lineTo(this.mCornerRadius + 0, f6);
            this.mRestBackgroundOutline.arcTo(0.0f, i2 - (this.mCornerRadius * 2), 0 + (2 * this.mCornerRadius), f6, 90.0f, 90.0f, false);
            this.mRestBackgroundOutline.lineTo(0.0f, f4);
            this.mRestBackgroundOutline.close();
        }
    }

    private void createThumbnailPath(int i, int i2, int i3, int i4, Path path) {
        path.reset();
        float f = i;
        float f2 = i2;
        path.moveTo(f, f2);
        float f3 = i3;
        path.lineTo(f3, f2);
        path.lineTo(f3, i4 - this.mCornerRadius);
        float f4 = i4;
        path.arcTo(i3 - (this.mCornerRadius * 2), i4 - (this.mCornerRadius * 2), f3, f4, 0.0f, 90.0f, false);
        path.lineTo(this.mCornerRadius + i, f4);
        path.arcTo(f, i4 - (this.mCornerRadius * 2), i + (2 * this.mCornerRadius), f4, 90.0f, 90.0f, false);
        path.lineTo(f, f2);
        path.close();
    }

    @Override // com.android.systemui.recents.views.TaskViewThumbnail, android.view.View
    protected void onDraw(Canvas canvas) throws Resources.NotFoundException {
        int dimensionPixelSize = getResources().getDimensionPixelSize(R.dimen.recents_grid_task_view_header_height);
        int iWidth = this.mTaskViewRect.width();
        int iHeight = this.mTaskViewRect.height() - dimensionPixelSize;
        int iMin = Math.min(iWidth, (int) (this.mThumbnailRect.width() * this.mThumbnailScale));
        int iMin2 = Math.min(iHeight, (int) (this.mThumbnailRect.height() * this.mThumbnailScale));
        if (this.mUpdateThumbnailOutline) {
            updateThumbnailOutline();
            this.mUpdateThumbnailOutline = false;
        }
        if (this.mUserLocked) {
            canvas.drawPath(this.mThumbnailOutline, this.mLockedPaint);
            return;
        }
        if (this.mBitmapShader != null && iMin > 0 && iMin2 > 0) {
            if (iMin < iWidth) {
                canvas.drawPath(this.mRestBackgroundOutline, this.mBgFillPaint);
            }
            if (iMin2 < iHeight) {
                canvas.drawPath(this.mRestBackgroundOutline, this.mBgFillPaint);
            }
            canvas.drawPath(this.mThumbnailOutline, getDrawPaint());
            return;
        }
        canvas.drawPath(this.mThumbnailOutline, this.mBgFillPaint);
    }
}
