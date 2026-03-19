package com.android.server.wm;

import android.graphics.GraphicBuffer;
import android.graphics.Point;
import android.os.Binder;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.animation.Animation;
import com.android.server.job.controllers.JobStatus;
import com.android.server.wm.SurfaceAnimator;

class AppWindowThumbnail implements SurfaceAnimator.Animatable {
    private static final String TAG = "WindowManager";
    private final AppWindowToken mAppToken;
    private final int mHeight;
    private final SurfaceAnimator mSurfaceAnimator;
    private final SurfaceControl mSurfaceControl;
    private final int mWidth;

    AppWindowThumbnail(SurfaceControl.Transaction transaction, AppWindowToken appWindowToken, GraphicBuffer graphicBuffer) {
        this.mAppToken = appWindowToken;
        this.mSurfaceAnimator = new SurfaceAnimator(this, new Runnable() {
            @Override
            public final void run() {
                this.f$0.onAnimationFinished();
            }
        }, appWindowToken.mService);
        this.mWidth = graphicBuffer.getWidth();
        this.mHeight = graphicBuffer.getHeight();
        WindowState windowStateFindMainWindow = appWindowToken.findMainWindow();
        this.mSurfaceControl = appWindowToken.makeSurface().setName("thumbnail anim: " + appWindowToken.toString()).setSize(this.mWidth, this.mHeight).setFormat(-3).setMetadata(appWindowToken.windowType, windowStateFindMainWindow != null ? windowStateFindMainWindow.mOwnerUid : Binder.getCallingUid()).setBufferLayer().build();
        if (WindowManagerDebugConfig.SHOW_TRANSACTIONS) {
            Slog.i("WindowManager", "  THUMBNAIL " + this.mSurfaceControl + ": CREATE");
        }
        Surface surface = new Surface();
        surface.copyFrom(this.mSurfaceControl);
        surface.attachAndQueueBuffer(graphicBuffer);
        surface.release();
        transaction.show(this.mSurfaceControl);
        transaction.setLayer(this.mSurfaceControl, Integer.MAX_VALUE);
    }

    void startAnimation(SurfaceControl.Transaction transaction, Animation animation) {
        startAnimation(transaction, animation, null);
    }

    void startAnimation(SurfaceControl.Transaction transaction, Animation animation, Point point) {
        animation.restrictDuration(JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
        animation.scaleCurrentDuration(this.mAppToken.mService.getTransitionAnimationScaleLocked());
        this.mSurfaceAnimator.startAnimation(transaction, new LocalAnimationAdapter(new WindowAnimationSpec(animation, point, this.mAppToken.mService.mAppTransition.canSkipFirstFrame()), this.mAppToken.mService.mSurfaceAnimationRunner), false);
    }

    private void onAnimationFinished() {
    }

    void setShowing(SurfaceControl.Transaction transaction, boolean z) {
        if (z) {
            transaction.show(this.mSurfaceControl);
        } else {
            transaction.hide(this.mSurfaceControl);
        }
    }

    void destroy() {
        this.mSurfaceAnimator.cancelAnimation();
        this.mSurfaceControl.destroy();
    }

    void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        protoOutputStream.write(1120986464257L, this.mWidth);
        protoOutputStream.write(1120986464258L, this.mHeight);
        this.mSurfaceAnimator.writeToProto(protoOutputStream, 1146756268035L);
        protoOutputStream.end(jStart);
    }

    @Override
    public SurfaceControl.Transaction getPendingTransaction() {
        return this.mAppToken.getPendingTransaction();
    }

    @Override
    public void commitPendingTransaction() {
        this.mAppToken.commitPendingTransaction();
    }

    @Override
    public void onAnimationLeashCreated(SurfaceControl.Transaction transaction, SurfaceControl surfaceControl) {
        transaction.setLayer(surfaceControl, Integer.MAX_VALUE);
    }

    @Override
    public void onAnimationLeashDestroyed(SurfaceControl.Transaction transaction) {
        transaction.hide(this.mSurfaceControl);
    }

    @Override
    public SurfaceControl.Builder makeAnimationLeash() {
        return this.mAppToken.makeSurface();
    }

    @Override
    public SurfaceControl getSurfaceControl() {
        return this.mSurfaceControl;
    }

    @Override
    public SurfaceControl getAnimationLeashParent() {
        return this.mAppToken.getAppAnimationLayer();
    }

    @Override
    public SurfaceControl getParentSurfaceControl() {
        return this.mAppToken.getParentSurfaceControl();
    }

    @Override
    public int getSurfaceWidth() {
        return this.mWidth;
    }

    @Override
    public int getSurfaceHeight() {
        return this.mHeight;
    }
}
