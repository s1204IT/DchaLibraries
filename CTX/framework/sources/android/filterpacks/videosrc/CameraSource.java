package android.filterpacks.videosrc;

import android.app.Instrumentation;
import android.filterfw.core.Filter;
import android.filterfw.core.FilterContext;
import android.filterfw.core.Frame;
import android.filterfw.core.GLFrame;
import android.filterfw.core.GenerateFieldPort;
import android.filterfw.core.GenerateFinalPort;
import android.filterfw.core.MutableFrameFormat;
import android.filterfw.core.ShaderProgram;
import android.filterfw.format.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.Matrix;
import android.util.Log;
import java.io.IOException;
import java.util.List;

public class CameraSource extends Filter {
    private static final int NEWFRAME_TIMEOUT = 100;
    private static final int NEWFRAME_TIMEOUT_REPEAT = 10;
    private static final String TAG = "CameraSource";
    private static final String mFrameShader = "#extension GL_OES_EGL_image_external : require\nprecision mediump float;\nuniform samplerExternalOES tex_sampler_0;\nvarying vec2 v_texcoord;\nvoid main() {\n  gl_FragColor = texture2D(tex_sampler_0, v_texcoord);\n}\n";
    private static final float[] mSourceCoords = {0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f};
    private Camera mCamera;
    private GLFrame mCameraFrame;

    @GenerateFieldPort(hasDefault = true, name = Instrumentation.REPORT_KEY_IDENTIFIER)
    private int mCameraId;
    private Camera.Parameters mCameraParameters;
    private float[] mCameraTransform;

    @GenerateFieldPort(hasDefault = true, name = "framerate")
    private int mFps;
    private ShaderProgram mFrameExtractor;

    @GenerateFieldPort(hasDefault = true, name = "height")
    private int mHeight;
    private final boolean mLogVerbose;
    private float[] mMappedCoords;
    private boolean mNewFrameAvailable;
    private MutableFrameFormat mOutputFormat;
    private SurfaceTexture mSurfaceTexture;

    @GenerateFinalPort(hasDefault = true, name = "waitForNewFrame")
    private boolean mWaitForNewFrame;

    @GenerateFieldPort(hasDefault = true, name = "width")
    private int mWidth;
    private SurfaceTexture.OnFrameAvailableListener onCameraFrameAvailableListener;

    public CameraSource(String str) {
        super(str);
        this.mCameraId = 0;
        this.mWidth = 320;
        this.mHeight = 240;
        this.mFps = 30;
        this.mWaitForNewFrame = true;
        this.onCameraFrameAvailableListener = new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                if (CameraSource.this.mLogVerbose) {
                    Log.v(CameraSource.TAG, "New frame from camera");
                }
                synchronized (CameraSource.this) {
                    CameraSource.this.mNewFrameAvailable = true;
                    CameraSource.this.notify();
                }
            }
        };
        this.mCameraTransform = new float[16];
        this.mMappedCoords = new float[16];
        this.mLogVerbose = Log.isLoggable(TAG, 2);
    }

    @Override
    public void setupPorts() {
        addOutputPort("video", ImageFormat.create(3, 3));
    }

    private void createFormats() {
        this.mOutputFormat = ImageFormat.create(this.mWidth, this.mHeight, 3, 3);
    }

    @Override
    public void prepare(FilterContext filterContext) {
        if (this.mLogVerbose) {
            Log.v(TAG, "Preparing");
        }
        this.mFrameExtractor = new ShaderProgram(filterContext, mFrameShader);
    }

    @Override
    public void open(FilterContext filterContext) {
        if (this.mLogVerbose) {
            Log.v(TAG, "Opening");
        }
        this.mCamera = Camera.open(this.mCameraId);
        getCameraParameters();
        this.mCamera.setParameters(this.mCameraParameters);
        createFormats();
        this.mCameraFrame = (GLFrame) filterContext.getFrameManager().newBoundFrame(this.mOutputFormat, 104, 0L);
        this.mSurfaceTexture = new SurfaceTexture(this.mCameraFrame.getTextureId());
        try {
            this.mCamera.setPreviewTexture(this.mSurfaceTexture);
            this.mSurfaceTexture.setOnFrameAvailableListener(this.onCameraFrameAvailableListener);
            this.mNewFrameAvailable = false;
            this.mCamera.startPreview();
        } catch (IOException e) {
            throw new RuntimeException("Could not bind camera surface texture: " + e.getMessage() + "!");
        }
    }

    @Override
    public void process(FilterContext filterContext) {
        if (this.mLogVerbose) {
            Log.v(TAG, "Processing new frame");
        }
        if (this.mWaitForNewFrame) {
            while (!this.mNewFrameAvailable) {
                try {
                    wait(100L);
                } catch (InterruptedException e) {
                    if (this.mLogVerbose) {
                        Log.v(TAG, "Interrupted while waiting for new frame");
                    }
                }
            }
            this.mNewFrameAvailable = false;
            if (this.mLogVerbose) {
                Log.v(TAG, "Got new frame");
            }
        }
        this.mSurfaceTexture.updateTexImage();
        if (this.mLogVerbose) {
            Log.v(TAG, "Using frame extractor in thread: " + Thread.currentThread());
        }
        this.mSurfaceTexture.getTransformMatrix(this.mCameraTransform);
        Matrix.multiplyMM(this.mMappedCoords, 0, this.mCameraTransform, 0, mSourceCoords, 0);
        this.mFrameExtractor.setSourceRegion(this.mMappedCoords[0], this.mMappedCoords[1], this.mMappedCoords[4], this.mMappedCoords[5], this.mMappedCoords[8], this.mMappedCoords[9], this.mMappedCoords[12], this.mMappedCoords[13]);
        Frame frameNewFrame = filterContext.getFrameManager().newFrame(this.mOutputFormat);
        this.mFrameExtractor.process(this.mCameraFrame, frameNewFrame);
        long timestamp = this.mSurfaceTexture.getTimestamp();
        if (this.mLogVerbose) {
            Log.v(TAG, "Timestamp: " + (timestamp / 1.0E9d) + " s");
        }
        frameNewFrame.setTimestamp(timestamp);
        pushOutput("video", frameNewFrame);
        frameNewFrame.release();
        if (this.mLogVerbose) {
            Log.v(TAG, "Done processing new frame");
        }
    }

    @Override
    public void close(FilterContext filterContext) {
        if (this.mLogVerbose) {
            Log.v(TAG, "Closing");
        }
        this.mCamera.release();
        this.mCamera = null;
        this.mSurfaceTexture.release();
        this.mSurfaceTexture = null;
    }

    @Override
    public void tearDown(FilterContext filterContext) {
        if (this.mCameraFrame != null) {
            this.mCameraFrame.release();
        }
    }

    @Override
    public void fieldPortValueUpdated(String str, FilterContext filterContext) {
        if (str.equals("framerate")) {
            getCameraParameters();
            int[] iArrFindClosestFpsRange = findClosestFpsRange(this.mFps, this.mCameraParameters);
            this.mCameraParameters.setPreviewFpsRange(iArrFindClosestFpsRange[0], iArrFindClosestFpsRange[1]);
            this.mCamera.setParameters(this.mCameraParameters);
        }
    }

    public synchronized Camera.Parameters getCameraParameters() {
        boolean z;
        if (this.mCameraParameters == null) {
            if (this.mCamera == null) {
                this.mCamera = Camera.open(this.mCameraId);
                z = true;
            } else {
                z = false;
            }
            this.mCameraParameters = this.mCamera.getParameters();
            if (z) {
                this.mCamera.release();
                this.mCamera = null;
            }
        }
        int[] iArrFindClosestSize = findClosestSize(this.mWidth, this.mHeight, this.mCameraParameters);
        this.mWidth = iArrFindClosestSize[0];
        this.mHeight = iArrFindClosestSize[1];
        this.mCameraParameters.setPreviewSize(this.mWidth, this.mHeight);
        int[] iArrFindClosestFpsRange = findClosestFpsRange(this.mFps, this.mCameraParameters);
        this.mCameraParameters.setPreviewFpsRange(iArrFindClosestFpsRange[0], iArrFindClosestFpsRange[1]);
        return this.mCameraParameters;
    }

    public synchronized void setCameraParameters(Camera.Parameters parameters) {
        parameters.setPreviewSize(this.mWidth, this.mHeight);
        this.mCameraParameters = parameters;
        if (isOpen()) {
            this.mCamera.setParameters(this.mCameraParameters);
        }
    }

    private int[] findClosestSize(int i, int i2, Camera.Parameters parameters) {
        List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
        int i3 = supportedPreviewSizes.get(0).width;
        int i4 = supportedPreviewSizes.get(0).height;
        int i5 = i4;
        int i6 = -1;
        int i7 = i3;
        int i8 = -1;
        for (Camera.Size size : supportedPreviewSizes) {
            if (size.width <= i && size.height <= i2 && size.width >= i8 && size.height >= i6) {
                i8 = size.width;
                i6 = size.height;
            }
            if (size.width < i7 && size.height < i5) {
                i7 = size.width;
                i5 = size.height;
            }
        }
        if (i8 == -1) {
            i8 = i7;
        } else {
            i5 = i6;
        }
        if (this.mLogVerbose) {
            Log.v(TAG, "Requested resolution: (" + i + ", " + i2 + "). Closest match: (" + i8 + ", " + i5 + ").");
        }
        return new int[]{i8, i5};
    }

    private int[] findClosestFpsRange(int i, Camera.Parameters parameters) {
        List<int[]> supportedPreviewFpsRange = parameters.getSupportedPreviewFpsRange();
        int[] iArr = supportedPreviewFpsRange.get(0);
        for (int[] iArr2 : supportedPreviewFpsRange) {
            int i2 = i * 1000;
            if (iArr2[0] < i2 && iArr2[1] > i2 && iArr2[0] > iArr[0] && iArr2[1] < iArr[1]) {
                iArr = iArr2;
            }
        }
        if (this.mLogVerbose) {
            Log.v(TAG, "Requested fps: " + i + ".Closest frame rate range: [" + (((double) iArr[0]) / 1000.0d) + "," + (((double) iArr[1]) / 1000.0d) + "]");
        }
        return iArr;
    }
}
