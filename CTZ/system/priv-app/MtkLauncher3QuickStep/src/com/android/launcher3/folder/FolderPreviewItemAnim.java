package com.android.launcher3.folder;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import com.android.launcher3.LauncherAnimUtils;

/* loaded from: classes.dex */
class FolderPreviewItemAnim {
    private static PreviewItemDrawingParams sTmpParams = new PreviewItemDrawingParams(0.0f, 0.0f, 0.0f, 0.0f);
    float finalScale;
    float finalTransX;
    float finalTransY;
    private ValueAnimator mValueAnimator;

    FolderPreviewItemAnim(PreviewItemManager previewItemManager, PreviewItemDrawingParams previewItemDrawingParams, int i, int i2, int i3, int i4, int i5, Runnable runnable) {
        previewItemManager.computePreviewItemDrawingParams(i3, i4, sTmpParams);
        this.finalScale = sTmpParams.scale;
        this.finalTransX = sTmpParams.transX;
        this.finalTransY = sTmpParams.transY;
        previewItemManager.computePreviewItemDrawingParams(i, i2, sTmpParams);
        float f = sTmpParams.scale;
        float f2 = sTmpParams.transX;
        float f3 = sTmpParams.transY;
        this.mValueAnimator = LauncherAnimUtils.ofFloat(0.0f, 1.0f);
        this.mValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.launcher3.folder.FolderPreviewItemAnim.1
            final /* synthetic */ PreviewItemDrawingParams val$params;
            final /* synthetic */ PreviewItemManager val$previewItemManager;
            final /* synthetic */ float val$scale0;
            final /* synthetic */ float val$transX0;
            final /* synthetic */ float val$transY0;

            AnonymousClass1(PreviewItemDrawingParams previewItemDrawingParams2, float f22, float f32, float f4, PreviewItemManager previewItemManager2) {
                previewItemDrawingParams = previewItemDrawingParams2;
                f = f22;
                f = f32;
                f = f4;
                previewItemManager = previewItemManager2;
            }

            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float animatedFraction = valueAnimator.getAnimatedFraction();
                previewItemDrawingParams.transX = f + ((FolderPreviewItemAnim.this.finalTransX - f) * animatedFraction);
                previewItemDrawingParams.transY = f + ((FolderPreviewItemAnim.this.finalTransY - f) * animatedFraction);
                previewItemDrawingParams.scale = f + (animatedFraction * (FolderPreviewItemAnim.this.finalScale - f));
                previewItemManager.onParamsChanged();
            }
        });
        this.mValueAnimator.addListener(new AnimatorListenerAdapter() { // from class: com.android.launcher3.folder.FolderPreviewItemAnim.2
            final /* synthetic */ Runnable val$onCompleteRunnable;
            final /* synthetic */ PreviewItemDrawingParams val$params;

            AnonymousClass2(Runnable runnable2, PreviewItemDrawingParams previewItemDrawingParams2) {
                runnable = runnable2;
                previewItemDrawingParams = previewItemDrawingParams2;
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                if (runnable != null) {
                    runnable.run();
                }
                previewItemDrawingParams.anim = null;
            }
        });
        this.mValueAnimator.setDuration(i5);
    }

    /* renamed from: com.android.launcher3.folder.FolderPreviewItemAnim$1 */
    class AnonymousClass1 implements ValueAnimator.AnimatorUpdateListener {
        final /* synthetic */ PreviewItemDrawingParams val$params;
        final /* synthetic */ PreviewItemManager val$previewItemManager;
        final /* synthetic */ float val$scale0;
        final /* synthetic */ float val$transX0;
        final /* synthetic */ float val$transY0;

        AnonymousClass1(PreviewItemDrawingParams previewItemDrawingParams2, float f22, float f32, float f4, PreviewItemManager previewItemManager2) {
            previewItemDrawingParams = previewItemDrawingParams2;
            f = f22;
            f = f32;
            f = f4;
            previewItemManager = previewItemManager2;
        }

        @Override // android.animation.ValueAnimator.AnimatorUpdateListener
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            float animatedFraction = valueAnimator.getAnimatedFraction();
            previewItemDrawingParams.transX = f + ((FolderPreviewItemAnim.this.finalTransX - f) * animatedFraction);
            previewItemDrawingParams.transY = f + ((FolderPreviewItemAnim.this.finalTransY - f) * animatedFraction);
            previewItemDrawingParams.scale = f + (animatedFraction * (FolderPreviewItemAnim.this.finalScale - f));
            previewItemManager.onParamsChanged();
        }
    }

    /* renamed from: com.android.launcher3.folder.FolderPreviewItemAnim$2 */
    class AnonymousClass2 extends AnimatorListenerAdapter {
        final /* synthetic */ Runnable val$onCompleteRunnable;
        final /* synthetic */ PreviewItemDrawingParams val$params;

        AnonymousClass2(Runnable runnable2, PreviewItemDrawingParams previewItemDrawingParams2) {
            runnable = runnable2;
            previewItemDrawingParams = previewItemDrawingParams2;
        }

        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animator) {
            if (runnable != null) {
                runnable.run();
            }
            previewItemDrawingParams.anim = null;
        }
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
