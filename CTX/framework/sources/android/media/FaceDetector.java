package android.media;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.util.Log;

public class FaceDetector {
    private static boolean sInitialized;
    private byte[] mBWBuffer;
    private long mDCR;
    private long mFD;
    private int mHeight;
    private int mMaxFaces;
    private long mSDK;
    private int mWidth;

    private native void fft_destroy();

    private native int fft_detect(Bitmap bitmap);

    private native void fft_get_face(Face face, int i);

    private native int fft_initialize(int i, int i2, int i3);

    private static native void nativeClassInit();

    public class Face {
        public static final float CONFIDENCE_THRESHOLD = 0.4f;
        public static final int EULER_X = 0;
        public static final int EULER_Y = 1;
        public static final int EULER_Z = 2;
        private float mConfidence;
        private float mEyesDist;
        private float mMidPointX;
        private float mMidPointY;
        private float mPoseEulerX;
        private float mPoseEulerY;
        private float mPoseEulerZ;

        public float confidence() {
            return this.mConfidence;
        }

        public void getMidPoint(PointF pointF) {
            pointF.set(this.mMidPointX, this.mMidPointY);
        }

        public float eyesDistance() {
            return this.mEyesDist;
        }

        public float pose(int i) {
            if (i == 0) {
                return this.mPoseEulerX;
            }
            if (i == 1) {
                return this.mPoseEulerY;
            }
            if (i == 2) {
                return this.mPoseEulerZ;
            }
            throw new IllegalArgumentException();
        }

        private Face() {
        }
    }

    public FaceDetector(int i, int i2, int i3) {
        if (!sInitialized) {
            return;
        }
        fft_initialize(i, i2, i3);
        this.mWidth = i;
        this.mHeight = i2;
        this.mMaxFaces = i3;
        this.mBWBuffer = new byte[i * i2];
    }

    public int findFaces(Bitmap bitmap, Face[] faceArr) {
        if (!sInitialized) {
            return 0;
        }
        if (bitmap.getWidth() != this.mWidth || bitmap.getHeight() != this.mHeight) {
            throw new IllegalArgumentException("bitmap size doesn't match initialization");
        }
        if (faceArr.length < this.mMaxFaces) {
            throw new IllegalArgumentException("faces[] smaller than maxFaces");
        }
        int iFft_detect = fft_detect(bitmap);
        if (iFft_detect >= this.mMaxFaces) {
            iFft_detect = this.mMaxFaces;
        }
        for (int i = 0; i < iFft_detect; i++) {
            if (faceArr[i] == null) {
                faceArr[i] = new Face();
            }
            fft_get_face(faceArr[i], i);
        }
        return iFft_detect;
    }

    protected void finalize() throws Throwable {
        fft_destroy();
    }

    static {
        sInitialized = false;
        try {
            System.loadLibrary("FFTEm");
            nativeClassInit();
            sInitialized = true;
        } catch (UnsatisfiedLinkError e) {
            Log.d("FFTEm", "face detection library not found!");
        }
    }
}
