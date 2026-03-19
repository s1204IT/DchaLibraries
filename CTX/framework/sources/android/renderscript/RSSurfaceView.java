package android.renderscript;

import android.content.Context;
import android.renderscript.RenderScriptGL;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class RSSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
    private RenderScriptGL mRS;
    private SurfaceHolder mSurfaceHolder;

    public RSSurfaceView(Context context) {
        super(context);
        init();
    }

    public RSSurfaceView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init();
    }

    private void init() {
        getHolder().addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        this.mSurfaceHolder = surfaceHolder;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        synchronized (this) {
            if (this.mRS != null) {
                this.mRS.setSurface(null, 0, 0);
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
        synchronized (this) {
            if (this.mRS != null) {
                this.mRS.setSurface(surfaceHolder, i2, i3);
            }
        }
    }

    public void pause() {
        if (this.mRS != null) {
            this.mRS.pause();
        }
    }

    public void resume() {
        if (this.mRS != null) {
            this.mRS.resume();
        }
    }

    public RenderScriptGL createRenderScriptGL(RenderScriptGL.SurfaceConfig surfaceConfig) {
        RenderScriptGL renderScriptGL = new RenderScriptGL(getContext(), surfaceConfig);
        setRenderScriptGL(renderScriptGL);
        return renderScriptGL;
    }

    public void destroyRenderScriptGL() {
        synchronized (this) {
            this.mRS.destroy();
            this.mRS = null;
        }
    }

    public void setRenderScriptGL(RenderScriptGL renderScriptGL) {
        this.mRS = renderScriptGL;
    }

    public RenderScriptGL getRenderScriptGL() {
        return this.mRS;
    }
}
