package com.android.launcher3.dragndrop;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RemoteViews;
import com.android.launcher3.BaseActivity;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.LauncherAppWidgetProviderInfo;
import com.android.launcher3.widget.WidgetCell;

/* loaded from: classes.dex */
public class LivePreviewWidgetCell extends WidgetCell {
    private RemoteViews mPreview;

    public LivePreviewWidgetCell(Context context) {
        this(context, null);
    }

    public LivePreviewWidgetCell(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public LivePreviewWidgetCell(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    public void setPreview(RemoteViews remoteViews) {
        this.mPreview = remoteViews;
    }

    @Override // com.android.launcher3.widget.WidgetCell
    public void ensurePreview() {
        Bitmap bitmapGenerateFromRemoteViews;
        if (this.mPreview != null && this.mActiveRequest == null && (bitmapGenerateFromRemoteViews = generateFromRemoteViews(this.mActivity, this.mPreview, this.mItem.widgetInfo, this.mPresetPreviewSize, new int[1])) != null) {
            applyPreview(bitmapGenerateFromRemoteViews);
        } else {
            super.ensurePreview();
        }
    }

    public static Bitmap generateFromRemoteViews(BaseActivity baseActivity, RemoteViews remoteViews, LauncherAppWidgetProviderInfo launcherAppWidgetProviderInfo, int i, int[] iArr) {
        float f;
        DeviceProfile deviceProfile = baseActivity.getDeviceProfile();
        int i2 = deviceProfile.cellWidthPx * launcherAppWidgetProviderInfo.spanX;
        int i3 = deviceProfile.cellHeightPx * launcherAppWidgetProviderInfo.spanY;
        try {
            View viewApply = remoteViews.apply(baseActivity, new FrameLayout(baseActivity));
            viewApply.measure(View.MeasureSpec.makeMeasureSpec(i2, 1073741824), View.MeasureSpec.makeMeasureSpec(i3, 1073741824));
            int measuredWidth = viewApply.getMeasuredWidth();
            int measuredHeight = viewApply.getMeasuredHeight();
            viewApply.layout(0, 0, measuredWidth, measuredHeight);
            iArr[0] = measuredWidth;
            if (measuredWidth > i) {
                f = i / measuredWidth;
                measuredHeight = (int) (measuredHeight * f);
                measuredWidth = i;
            } else {
                f = 1.0f;
            }
            Bitmap bitmapCreateBitmap = Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmapCreateBitmap);
            canvas.scale(f, f);
            viewApply.draw(canvas);
            canvas.setBitmap(null);
            return bitmapCreateBitmap;
        } catch (Exception e) {
            return null;
        }
    }
}
