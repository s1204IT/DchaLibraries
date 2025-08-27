package com.android.systemui.shared.system;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.ThreadedRenderer;
import android.view.View;
import android.view.ViewRootImpl;

/* loaded from: classes.dex */
public class SyncRtSurfaceTransactionApplier {
    private final Surface mTargetSurface;
    private final ViewRootImpl mTargetViewRootImpl;
    private final float[] mTmpFloat9 = new float[9];

    public SyncRtSurfaceTransactionApplier(View view) {
        this.mTargetViewRootImpl = view != null ? view.getViewRootImpl() : null;
        this.mTargetSurface = this.mTargetViewRootImpl != null ? this.mTargetViewRootImpl.mSurface : null;
    }

    public void scheduleApply(final SurfaceParams... surfaceParamsArr) {
        if (this.mTargetViewRootImpl == null) {
            return;
        }
        this.mTargetViewRootImpl.registerRtFrameCallback(new ThreadedRenderer.FrameDrawingCallback() { // from class: com.android.systemui.shared.system.-$$Lambda$SyncRtSurfaceTransactionApplier$odiZXJPwVQjUmyGhNuNxcMPZIOM
            public final void onFrameDraw(long j) {
                SyncRtSurfaceTransactionApplier.lambda$scheduleApply$0(this.f$0, surfaceParamsArr, j);
            }
        });
        this.mTargetViewRootImpl.getView().invalidate();
    }

    public static /* synthetic */ void lambda$scheduleApply$0(SyncRtSurfaceTransactionApplier syncRtSurfaceTransactionApplier, SurfaceParams[] surfaceParamsArr, long j) {
        if (syncRtSurfaceTransactionApplier.mTargetSurface == null || !syncRtSurfaceTransactionApplier.mTargetSurface.isValid()) {
            return;
        }
        SurfaceControl.Transaction transaction = new SurfaceControl.Transaction();
        for (int length = surfaceParamsArr.length - 1; length >= 0; length--) {
            SurfaceParams surfaceParams = surfaceParamsArr[length];
            transaction.deferTransactionUntilSurface(surfaceParams.surface, syncRtSurfaceTransactionApplier.mTargetSurface, j);
            applyParams(transaction, surfaceParams, syncRtSurfaceTransactionApplier.mTmpFloat9);
        }
        transaction.setEarlyWakeup();
        transaction.apply();
    }

    private static void applyParams(SurfaceControl.Transaction transaction, SurfaceParams surfaceParams, float[] fArr) {
        transaction.setMatrix(surfaceParams.surface, surfaceParams.matrix, fArr);
        transaction.setWindowCrop(surfaceParams.surface, surfaceParams.windowCrop);
        transaction.setAlpha(surfaceParams.surface, surfaceParams.alpha);
        transaction.setLayer(surfaceParams.surface, surfaceParams.layer);
        transaction.show(surfaceParams.surface);
    }

    public static class SurfaceParams {
        final float alpha;
        final int layer;
        final Matrix matrix;
        final SurfaceControl surface;
        final Rect windowCrop;

        public SurfaceParams(SurfaceControlCompat surfaceControlCompat, float f, Matrix matrix, Rect rect, int i) {
            this.surface = surfaceControlCompat.mSurfaceControl;
            this.alpha = f;
            this.matrix = new Matrix(matrix);
            this.windowCrop = new Rect(rect);
            this.layer = i;
        }
    }
}
