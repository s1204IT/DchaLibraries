package com.android.server.wm;

import android.graphics.Rect;
import android.util.Log;
import android.util.proto.ProtoOutputStream;
import android.view.Surface;
import android.view.SurfaceControl;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wm.Dimmer;
import com.android.server.wm.LocalAnimationAdapter;
import com.android.server.wm.SurfaceAnimator;
import java.io.PrintWriter;

class Dimmer {
    private static final int DEFAULT_DIM_ANIM_DURATION = 200;
    private static final String TAG = "WindowManager";

    @VisibleForTesting
    DimState mDimState;
    private WindowContainer mHost;
    private WindowContainer mLastRequestedDimContainer;
    private final SurfaceAnimatorStarter mSurfaceAnimatorStarter;

    @VisibleForTesting
    interface SurfaceAnimatorStarter {
        void startAnimation(SurfaceAnimator surfaceAnimator, SurfaceControl.Transaction transaction, AnimationAdapter animationAdapter, boolean z);
    }

    private class DimAnimatable implements SurfaceAnimator.Animatable {
        private final SurfaceControl mDimLayer;

        private DimAnimatable(SurfaceControl surfaceControl) {
            this.mDimLayer = surfaceControl;
        }

        @Override
        public SurfaceControl.Transaction getPendingTransaction() {
            return Dimmer.this.mHost.getPendingTransaction();
        }

        @Override
        public void commitPendingTransaction() {
            Dimmer.this.mHost.commitPendingTransaction();
        }

        @Override
        public void onAnimationLeashCreated(SurfaceControl.Transaction transaction, SurfaceControl surfaceControl) {
        }

        @Override
        public void onAnimationLeashDestroyed(SurfaceControl.Transaction transaction) {
        }

        @Override
        public SurfaceControl.Builder makeAnimationLeash() {
            return Dimmer.this.mHost.makeAnimationLeash();
        }

        @Override
        public SurfaceControl getAnimationLeashParent() {
            return Dimmer.this.mHost.getSurfaceControl();
        }

        @Override
        public SurfaceControl getSurfaceControl() {
            return this.mDimLayer;
        }

        @Override
        public SurfaceControl getParentSurfaceControl() {
            return Dimmer.this.mHost.getSurfaceControl();
        }

        @Override
        public int getSurfaceWidth() {
            return Dimmer.this.mHost.getSurfaceWidth();
        }

        @Override
        public int getSurfaceHeight() {
            return Dimmer.this.mHost.getSurfaceHeight();
        }
    }

    @VisibleForTesting
    class DimState {
        boolean isVisible;
        SurfaceControl mDimLayer;
        boolean mDontReset;
        SurfaceAnimator mSurfaceAnimator;
        boolean mAnimateExit = true;
        boolean mDimming = true;

        DimState(SurfaceControl surfaceControl) {
            this.mDimLayer = surfaceControl;
            this.mSurfaceAnimator = new SurfaceAnimator(new DimAnimatable(surfaceControl), new Runnable() {
                @Override
                public final void run() {
                    Dimmer.DimState.lambda$new$0(this.f$0);
                }
            }, Dimmer.this.mHost.mService);
        }

        public static void lambda$new$0(DimState dimState) {
            if (!dimState.mDimming) {
                dimState.mDimLayer.destroy();
            }
        }
    }

    Dimmer(WindowContainer windowContainer) {
        this(windowContainer, new SurfaceAnimatorStarter() {
            @Override
            public final void startAnimation(SurfaceAnimator surfaceAnimator, SurfaceControl.Transaction transaction, AnimationAdapter animationAdapter, boolean z) {
                surfaceAnimator.startAnimation(transaction, animationAdapter, z);
            }
        });
    }

    Dimmer(WindowContainer windowContainer, SurfaceAnimatorStarter surfaceAnimatorStarter) {
        this.mHost = windowContainer;
        this.mSurfaceAnimatorStarter = surfaceAnimatorStarter;
    }

    private SurfaceControl makeDimLayer() {
        return this.mHost.makeChildSurface(null).setParent(this.mHost.getSurfaceControl()).setColorLayer(true).setName("Dim Layer for - " + this.mHost.getName()).build();
    }

    private DimState getDimState(WindowContainer windowContainer) {
        if (this.mDimState == null) {
            try {
                this.mDimState = new DimState(makeDimLayer());
                if (windowContainer == null) {
                    this.mDimState.mDontReset = true;
                }
            } catch (Surface.OutOfResourcesException e) {
                Log.w("WindowManager", "OutOfResourcesException creating dim surface");
            }
        }
        this.mLastRequestedDimContainer = windowContainer;
        return this.mDimState;
    }

    private void dim(SurfaceControl.Transaction transaction, WindowContainer windowContainer, int i, float f) {
        DimState dimState = getDimState(windowContainer);
        if (dimState == null) {
            return;
        }
        if (windowContainer != null) {
            transaction.setRelativeLayer(dimState.mDimLayer, windowContainer.getSurfaceControl(), i);
        } else {
            transaction.setLayer(dimState.mDimLayer, Integer.MAX_VALUE);
        }
        transaction.setAlpha(dimState.mDimLayer, f);
        dimState.mDimming = true;
    }

    void stopDim(SurfaceControl.Transaction transaction) {
        if (this.mDimState != null) {
            transaction.hide(this.mDimState.mDimLayer);
            this.mDimState.isVisible = false;
            this.mDimState.mDontReset = false;
        }
    }

    void dimAbove(SurfaceControl.Transaction transaction, float f) {
        dim(transaction, null, 1, f);
    }

    void dimAbove(SurfaceControl.Transaction transaction, WindowContainer windowContainer, float f) {
        dim(transaction, windowContainer, 1, f);
    }

    void dimBelow(SurfaceControl.Transaction transaction, WindowContainer windowContainer, float f) {
        dim(transaction, windowContainer, -1, f);
    }

    void resetDimStates() {
        if (this.mDimState != null && !this.mDimState.mDontReset) {
            this.mDimState.mDimming = false;
        }
    }

    void dontAnimateExit() {
        if (this.mDimState != null) {
            this.mDimState.mAnimateExit = false;
        }
    }

    boolean updateDims(SurfaceControl.Transaction transaction, Rect rect) {
        if (this.mDimState == null) {
            return false;
        }
        if (!this.mDimState.mDimming) {
            if (!this.mDimState.mAnimateExit) {
                transaction.destroy(this.mDimState.mDimLayer);
            } else {
                startDimExit(this.mLastRequestedDimContainer, this.mDimState.mSurfaceAnimator, transaction);
            }
            this.mDimState = null;
            return false;
        }
        transaction.setSize(this.mDimState.mDimLayer, rect.width(), rect.height());
        transaction.setPosition(this.mDimState.mDimLayer, rect.left, rect.top);
        if (!this.mDimState.isVisible) {
            this.mDimState.isVisible = true;
            transaction.show(this.mDimState.mDimLayer);
            startDimEnter(this.mLastRequestedDimContainer, this.mDimState.mSurfaceAnimator, transaction);
        }
        return true;
    }

    private void startDimEnter(WindowContainer windowContainer, SurfaceAnimator surfaceAnimator, SurfaceControl.Transaction transaction) {
        startAnim(windowContainer, surfaceAnimator, transaction, 0.0f, 1.0f);
    }

    private void startDimExit(WindowContainer windowContainer, SurfaceAnimator surfaceAnimator, SurfaceControl.Transaction transaction) {
        startAnim(windowContainer, surfaceAnimator, transaction, 1.0f, 0.0f);
    }

    private void startAnim(WindowContainer windowContainer, SurfaceAnimator surfaceAnimator, SurfaceControl.Transaction transaction, float f, float f2) {
        this.mSurfaceAnimatorStarter.startAnimation(surfaceAnimator, transaction, new LocalAnimationAdapter(new AlphaAnimationSpec(f, f2, getDimDuration(windowContainer)), this.mHost.mService.mSurfaceAnimationRunner), false);
    }

    private long getDimDuration(WindowContainer windowContainer) {
        if (windowContainer == null) {
            return 0L;
        }
        AnimationAdapter animation = windowContainer.mSurfaceAnimator.getAnimation();
        if (animation == null) {
            return 200L;
        }
        return animation.getDurationHint();
    }

    private static class AlphaAnimationSpec implements LocalAnimationAdapter.AnimationSpec {
        private final long mDuration;
        private final float mFromAlpha;
        private final float mToAlpha;

        AlphaAnimationSpec(float f, float f2, long j) {
            this.mFromAlpha = f;
            this.mToAlpha = f2;
            this.mDuration = j;
        }

        @Override
        public long getDuration() {
            return this.mDuration;
        }

        @Override
        public void apply(SurfaceControl.Transaction transaction, SurfaceControl surfaceControl, long j) {
            transaction.setAlpha(surfaceControl, ((j / getDuration()) * (this.mToAlpha - this.mFromAlpha)) + this.mFromAlpha);
        }

        @Override
        public void dump(PrintWriter printWriter, String str) {
            printWriter.print(str);
            printWriter.print("from=");
            printWriter.print(this.mFromAlpha);
            printWriter.print(" to=");
            printWriter.print(this.mToAlpha);
            printWriter.print(" duration=");
            printWriter.println(this.mDuration);
        }

        @Override
        public void writeToProtoInner(ProtoOutputStream protoOutputStream) {
            long jStart = protoOutputStream.start(1146756268035L);
            protoOutputStream.write(1108101562369L, this.mFromAlpha);
            protoOutputStream.write(1108101562370L, this.mToAlpha);
            protoOutputStream.write(1112396529667L, this.mDuration);
            protoOutputStream.end(jStart);
        }
    }
}
