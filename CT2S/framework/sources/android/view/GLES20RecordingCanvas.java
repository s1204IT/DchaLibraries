package android.view;

import android.util.Pools;

class GLES20RecordingCanvas extends GLES20Canvas {
    private static final int POOL_LIMIT = 25;
    private static final Pools.SynchronizedPool<GLES20RecordingCanvas> sPool = new Pools.SynchronizedPool<>(25);
    RenderNode mNode;

    private GLES20RecordingCanvas() {
    }

    static GLES20RecordingCanvas obtain(RenderNode node) {
        if (node == null) {
            throw new IllegalArgumentException("node cannot be null");
        }
        GLES20RecordingCanvas canvas = sPool.acquire();
        if (canvas == null) {
            canvas = new GLES20RecordingCanvas();
        }
        canvas.mNode = node;
        return canvas;
    }

    void recycle() {
        this.mNode = null;
        sPool.release(this);
    }

    long finishRecording() {
        return nFinishRecording(this.mRenderer);
    }

    @Override
    public boolean isRecordingFor(Object o) {
        return o == this.mNode;
    }
}
