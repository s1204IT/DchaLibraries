package com.android.server.wm;

import android.view.Choreographer;

public final class $$Lambda$SurfaceAnimationRunner$9Wa9MhcrSX12liOouHtYXEkDU60 implements Choreographer.FrameCallback {
    private final SurfaceAnimationRunner f$0;

    public $$Lambda$SurfaceAnimationRunner$9Wa9MhcrSX12liOouHtYXEkDU60(SurfaceAnimationRunner surfaceAnimationRunner) {
        this.f$0 = surfaceAnimationRunner;
    }

    @Override
    public final void doFrame(long j) {
        this.f$0.startAnimations(j);
    }
}
