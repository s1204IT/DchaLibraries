package com.android.launcher3.shortcuts;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;
import com.android.launcher3.Launcher;
import com.android.launcher3.Utilities;
import com.android.launcher3.graphics.DragPreviewProvider;

/* loaded from: classes.dex */
public class ShortcutDragPreviewProvider extends DragPreviewProvider {
    private final Point mPositionShift;

    public ShortcutDragPreviewProvider(View view, Point point) {
        super(view);
        this.mPositionShift = point;
    }

    @Override // com.android.launcher3.graphics.DragPreviewProvider
    public Bitmap createDragBitmap() {
        Drawable background = this.mView.getBackground();
        Rect drawableBounds = getDrawableBounds(background);
        int i = Launcher.getLauncher(this.mView.getContext()).getDeviceProfile().iconSizePx;
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(this.blurSizeOutline + i, this.blurSizeOutline + i, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmapCreateBitmap);
        canvas.translate(this.blurSizeOutline / 2, this.blurSizeOutline / 2);
        float f = i;
        canvas.scale(f / drawableBounds.width(), f / drawableBounds.height(), 0.0f, 0.0f);
        canvas.translate(drawableBounds.left, drawableBounds.top);
        background.draw(canvas);
        return bitmapCreateBitmap;
    }

    @Override // com.android.launcher3.graphics.DragPreviewProvider
    public float getScaleAndPosition(Bitmap bitmap, int[] iArr) {
        Launcher launcher = Launcher.getLauncher(this.mView.getContext());
        int iWidth = getDrawableBounds(this.mView.getBackground()).width();
        float locationInDragLayer = launcher.getDragLayer().getLocationInDragLayer(this.mView, iArr);
        int paddingStart = this.mView.getPaddingStart();
        if (Utilities.isRtl(this.mView.getResources())) {
            paddingStart = (this.mView.getWidth() - iWidth) - paddingStart;
        }
        float f = iWidth * locationInDragLayer;
        iArr[0] = iArr[0] + Math.round((paddingStart * locationInDragLayer) + ((f - bitmap.getWidth()) / 2.0f) + this.mPositionShift.x);
        iArr[1] = iArr[1] + Math.round((((locationInDragLayer * this.mView.getHeight()) - bitmap.getHeight()) / 2.0f) + this.mPositionShift.y);
        return f / launcher.getDeviceProfile().iconSizePx;
    }
}
