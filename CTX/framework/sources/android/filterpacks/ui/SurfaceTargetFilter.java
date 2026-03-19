package android.filterpacks.ui;

import android.filterfw.core.Filter;
import android.filterfw.core.FilterContext;
import android.filterfw.core.Frame;
import android.filterfw.core.GLEnvironment;
import android.filterfw.core.GLFrame;
import android.filterfw.core.GenerateFieldPort;
import android.filterfw.core.GenerateFinalPort;
import android.filterfw.core.ShaderProgram;
import android.filterfw.format.ImageFormat;
import android.util.Log;
import android.view.Surface;

public class SurfaceTargetFilter extends Filter {
    private static final String TAG = "SurfaceRenderFilter";
    private final int RENDERMODE_FILL_CROP;
    private final int RENDERMODE_FIT;
    private final int RENDERMODE_STRETCH;
    private float mAspectRatio;
    private GLEnvironment mGlEnv;
    private boolean mLogVerbose;
    private ShaderProgram mProgram;
    private int mRenderMode;

    @GenerateFieldPort(hasDefault = true, name = "renderMode")
    private String mRenderModeString;
    private GLFrame mScreen;

    @GenerateFieldPort(name = "oheight")
    private int mScreenHeight;

    @GenerateFieldPort(name = "owidth")
    private int mScreenWidth;

    @GenerateFinalPort(name = "surface")
    private Surface mSurface;
    private int mSurfaceId;

    public SurfaceTargetFilter(String str) {
        super(str);
        this.RENDERMODE_STRETCH = 0;
        this.RENDERMODE_FIT = 1;
        this.RENDERMODE_FILL_CROP = 2;
        this.mRenderMode = 1;
        this.mAspectRatio = 1.0f;
        this.mSurfaceId = -1;
        this.mLogVerbose = Log.isLoggable(TAG, 2);
    }

    @Override
    public void setupPorts() {
        if (this.mSurface == null) {
            throw new RuntimeException("NULL Surface passed to SurfaceTargetFilter");
        }
        addMaskedInputPort("frame", ImageFormat.create(3));
    }

    public void updateRenderMode() {
        if (this.mRenderModeString != null) {
            if (this.mRenderModeString.equals("stretch")) {
                this.mRenderMode = 0;
            } else if (this.mRenderModeString.equals("fit")) {
                this.mRenderMode = 1;
            } else if (this.mRenderModeString.equals("fill_crop")) {
                this.mRenderMode = 2;
            } else {
                throw new RuntimeException("Unknown render mode '" + this.mRenderModeString + "'!");
            }
        }
        updateTargetRect();
    }

    @Override
    public void prepare(FilterContext filterContext) {
        this.mGlEnv = filterContext.getGLEnvironment();
        this.mProgram = ShaderProgram.createIdentity(filterContext);
        this.mProgram.setSourceRect(0.0f, 1.0f, 1.0f, -1.0f);
        this.mProgram.setClearsOutput(true);
        this.mProgram.setClearColor(0.0f, 0.0f, 0.0f);
        this.mScreen = (GLFrame) filterContext.getFrameManager().newBoundFrame(ImageFormat.create(this.mScreenWidth, this.mScreenHeight, 3, 3), 101, 0L);
        updateRenderMode();
    }

    @Override
    public void open(FilterContext filterContext) {
        registerSurface();
    }

    @Override
    public void process(FilterContext filterContext) {
        if (this.mLogVerbose) {
            Log.v(TAG, "Starting frame processing");
        }
        Frame framePullInput = pullInput("frame");
        boolean z = false;
        float width = framePullInput.getFormat().getWidth() / framePullInput.getFormat().getHeight();
        if (width != this.mAspectRatio) {
            if (this.mLogVerbose) {
                Log.v(TAG, "New aspect ratio: " + width + ", previously: " + this.mAspectRatio);
            }
            this.mAspectRatio = width;
            updateTargetRect();
        }
        if (this.mLogVerbose) {
            Log.v(TAG, "Got input format: " + framePullInput.getFormat());
        }
        if (framePullInput.getFormat().getTarget() != 3) {
            framePullInput = filterContext.getFrameManager().duplicateFrameToTarget(framePullInput, 3);
            z = true;
        }
        this.mGlEnv.activateSurfaceWithId(this.mSurfaceId);
        this.mProgram.process(framePullInput, this.mScreen);
        this.mGlEnv.swapBuffers();
        if (z) {
            framePullInput.release();
        }
    }

    @Override
    public void fieldPortValueUpdated(String str, FilterContext filterContext) {
        this.mScreen.setViewport(0, 0, this.mScreenWidth, this.mScreenHeight);
        updateTargetRect();
    }

    @Override
    public void close(FilterContext filterContext) {
        unregisterSurface();
    }

    @Override
    public void tearDown(FilterContext filterContext) {
        if (this.mScreen != null) {
            this.mScreen.release();
        }
    }

    private void updateTargetRect() {
        if (this.mScreenWidth > 0 && this.mScreenHeight > 0 && this.mProgram != null) {
            float f = (this.mScreenWidth / this.mScreenHeight) / this.mAspectRatio;
            switch (this.mRenderMode) {
                case 0:
                    this.mProgram.setTargetRect(0.0f, 0.0f, 1.0f, 1.0f);
                    break;
                case 1:
                    if (f > 1.0f) {
                        this.mProgram.setTargetRect(0.5f - (0.5f / f), 0.0f, 1.0f / f, 1.0f);
                    } else {
                        this.mProgram.setTargetRect(0.0f, 0.5f - (0.5f * f), 1.0f, f);
                    }
                    break;
                case 2:
                    if (f > 1.0f) {
                        this.mProgram.setTargetRect(0.0f, 0.5f - (0.5f * f), 1.0f, f);
                    } else {
                        this.mProgram.setTargetRect(0.5f - (0.5f / f), 0.0f, 1.0f / f, 1.0f);
                    }
                    break;
            }
        }
    }

    private void registerSurface() {
        this.mSurfaceId = this.mGlEnv.registerSurface(this.mSurface);
        if (this.mSurfaceId < 0) {
            throw new RuntimeException("Could not register Surface: " + this.mSurface);
        }
    }

    private void unregisterSurface() {
        if (this.mSurfaceId > 0) {
            this.mGlEnv.unregisterSurfaceId(this.mSurfaceId);
        }
    }
}
