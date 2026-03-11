package com.android.launcher3.folder;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import com.android.launcher3.LauncherAnimUtils;

class FolderPreviewItemAnim {
    private static PreviewItemDrawingParams sTmpParams = new PreviewItemDrawingParams(0.0f, 0.0f, 0.0f, 0.0f);
    float finalScale;
    float finalTransX;
    float finalTransY;
    private ValueAnimator mValueAnimator;

    FolderPreviewItemAnim(final PreviewItemManager previewItemManager, final PreviewItemDrawingParams previewItemDrawingParams, int i, int i2, int i3, int i4, int i5, final Runnable runnable) {
        previewItemManager.computePreviewItemDrawingParams(i3, i4, sTmpParams);
        this.finalScale = sTmpParams.scale;
        this.finalTransX = sTmpParams.transX;
        this.finalTransY = sTmpParams.transY;
        previewItemManager.computePreviewItemDrawingParams(i, i2, sTmpParams);
        final float f = sTmpParams.scale;
        final float f2 = sTmpParams.transX;
        final float f3 = sTmpParams.transY;
        this.mValueAnimator = LauncherAnimUtils.ofFloat(0.0f, 1.0f);
        this.mValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float animatedFraction = valueAnimator.getAnimatedFraction();
                previewItemDrawingParams.transX = f2 + ((FolderPreviewItemAnim.this.finalTransX - f2) * animatedFraction);
                previewItemDrawingParams.transY = f3 + ((FolderPreviewItemAnim.this.finalTransY - f3) * animatedFraction);
                previewItemDrawingParams.scale = f + (animatedFraction * (FolderPreviewItemAnim.this.finalScale - f));
                previewItemManager.onParamsChanged();
            }
        });
        this.mValueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                if (runnable != null) {
                    runnable.run();
                }
                previewItemDrawingParams.anim = null;
            }
        });
        this.mValueAnimator.setDuration(i5);
    }

    public void start() {
        this.mValueAnimator.start();
    }

    public void cancel() {
        this.mValueAnimator.cancel();
    }

    public boolean hasEqualFinalState(FolderPreviewItemAnim folderPreviewItemAnim) {
        return this.finalTransY == folderPreviewItemAnim.finalTransY && this.finalTransX == folderPreviewItemAnim.finalTransX && this.finalScale == folderPreviewItemAnim.finalScale;
    }
}
