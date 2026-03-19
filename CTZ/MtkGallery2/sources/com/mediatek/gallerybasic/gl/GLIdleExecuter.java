package com.mediatek.gallerybasic.gl;

import android.opengl.GLSurfaceView;
import java.util.ArrayDeque;

public class GLIdleExecuter {
    static final boolean $assertionsDisabled = false;
    private static final String TAG = "MtkGallery2/GLIdleExecuter";
    private MGLCanvas mCanvas;
    private GLSurfaceView mGLView;
    private IGLSurfaceViewStatusGetter mGLViewStatusGetter;
    private ArrayDeque<GLIdleCmd> mGLIdleCmds = new ArrayDeque<>();
    private IdleRunner mRunner = new IdleRunner();

    public interface GLIdleCmd {
        boolean onGLIdle(MGLCanvas mGLCanvas);
    }

    public interface IGLSurfaceViewStatusGetter {
        boolean isRenderRequested();

        boolean isSurfaceDestroyed();
    }

    public GLIdleExecuter(GLSurfaceView gLSurfaceView, IGLSurfaceViewStatusGetter iGLSurfaceViewStatusGetter) {
        this.mGLView = gLSurfaceView;
        this.mGLViewStatusGetter = iGLSurfaceViewStatusGetter;
    }

    public void setCanvas(MGLCanvas mGLCanvas) {
        this.mCanvas = mGLCanvas;
        synchronized (this.mGLIdleCmds) {
            if (!this.mGLViewStatusGetter.isSurfaceDestroyed()) {
                this.mRunner.enable();
            }
        }
    }

    public void addOnGLIdleCmd(GLIdleCmd gLIdleCmd) {
        synchronized (this.mGLIdleCmds) {
            this.mGLIdleCmds.addLast(gLIdleCmd);
            if (!this.mGLViewStatusGetter.isSurfaceDestroyed()) {
                this.mRunner.enable();
            }
        }
    }

    public void onRenderComplete() {
        synchronized (this.mGLIdleCmds) {
            if (!this.mGLIdleCmds.isEmpty()) {
                this.mRunner.enable();
            }
        }
    }

    private class IdleRunner implements Runnable {
        private boolean mActive;

        private IdleRunner() {
            this.mActive = false;
        }

        @Override
        public void run() {
            synchronized (GLIdleExecuter.this.mGLIdleCmds) {
                this.mActive = false;
                if (!GLIdleExecuter.this.mGLIdleCmds.isEmpty() && GLIdleExecuter.this.mCanvas != null) {
                    GLIdleCmd gLIdleCmd = (GLIdleCmd) GLIdleExecuter.this.mGLIdleCmds.removeFirst();
                    boolean zOnGLIdle = gLIdleCmd.onGLIdle(GLIdleExecuter.this.mCanvas);
                    synchronized (GLIdleExecuter.this.mGLIdleCmds) {
                        if (zOnGLIdle) {
                            try {
                                GLIdleExecuter.this.mGLIdleCmds.addLast(gLIdleCmd);
                            } finally {
                            }
                        }
                        if (!GLIdleExecuter.this.mGLViewStatusGetter.isRenderRequested() && !GLIdleExecuter.this.mGLIdleCmds.isEmpty()) {
                            enable();
                        }
                    }
                }
            }
        }

        public void enable() {
            if (this.mActive) {
                return;
            }
            this.mActive = true;
            GLIdleExecuter.this.mGLView.queueEvent(this);
        }
    }
}
