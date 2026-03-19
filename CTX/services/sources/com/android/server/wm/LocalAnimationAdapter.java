package com.android.server.wm;

import android.os.SystemClock;
import android.util.proto.ProtoOutputStream;
import android.view.SurfaceControl;
import com.android.server.wm.SurfaceAnimator;
import java.io.PrintWriter;

class LocalAnimationAdapter implements AnimationAdapter {
    private final SurfaceAnimationRunner mAnimator;
    private final AnimationSpec mSpec;

    LocalAnimationAdapter(AnimationSpec animationSpec, SurfaceAnimationRunner surfaceAnimationRunner) {
        this.mSpec = animationSpec;
        this.mAnimator = surfaceAnimationRunner;
    }

    @Override
    public boolean getDetachWallpaper() {
        return this.mSpec.getDetachWallpaper();
    }

    @Override
    public boolean getShowWallpaper() {
        return this.mSpec.getShowWallpaper();
    }

    @Override
    public int getBackgroundColor() {
        return this.mSpec.getBackgroundColor();
    }

    @Override
    public void startAnimation(SurfaceControl surfaceControl, SurfaceControl.Transaction transaction, final SurfaceAnimator.OnAnimationFinishedCallback onAnimationFinishedCallback) {
        this.mAnimator.startAnimation(this.mSpec, surfaceControl, transaction, new Runnable() {
            @Override
            public final void run() {
                onAnimationFinishedCallback.onAnimationFinished(this.f$0);
            }
        });
    }

    @Override
    public void onAnimationCancelled(SurfaceControl surfaceControl) {
        this.mAnimator.onAnimationCancelled(surfaceControl);
    }

    @Override
    public long getDurationHint() {
        return this.mSpec.getDuration();
    }

    @Override
    public long getStatusBarTransitionsStartTime() {
        return this.mSpec.calculateStatusBarTransitionStartTime();
    }

    @Override
    public void dump(PrintWriter printWriter, String str) {
        this.mSpec.dump(printWriter, str);
    }

    @Override
    public void writeToProto(ProtoOutputStream protoOutputStream) {
        long jStart = protoOutputStream.start(1146756268033L);
        this.mSpec.writeToProto(protoOutputStream, 1146756268033L);
        protoOutputStream.end(jStart);
    }

    interface AnimationSpec {
        void apply(SurfaceControl.Transaction transaction, SurfaceControl surfaceControl, long j);

        void dump(PrintWriter printWriter, String str);

        long getDuration();

        void writeToProtoInner(ProtoOutputStream protoOutputStream);

        default boolean getDetachWallpaper() {
            return false;
        }

        default boolean getShowWallpaper() {
            return false;
        }

        default int getBackgroundColor() {
            return 0;
        }

        default long calculateStatusBarTransitionStartTime() {
            return SystemClock.uptimeMillis();
        }

        default boolean canSkipFirstFrame() {
            return false;
        }

        default boolean needsEarlyWakeup() {
            return false;
        }

        default void writeToProto(ProtoOutputStream protoOutputStream, long j) {
            long jStart = protoOutputStream.start(j);
            writeToProtoInner(protoOutputStream);
            protoOutputStream.end(jStart);
        }
    }
}
