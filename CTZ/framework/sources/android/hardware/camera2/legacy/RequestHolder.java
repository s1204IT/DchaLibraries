package android.hardware.camera2.legacy;

import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.legacy.LegacyExceptionUtils;
import android.util.Log;
import android.view.Surface;
import com.android.internal.util.Preconditions;
import java.util.Collection;
import java.util.Iterator;

public class RequestHolder {
    private static final String TAG = "RequestHolder";
    private volatile boolean mFailed;
    private final long mFrameNumber;
    private final Collection<Long> mJpegSurfaceIds;
    private final int mNumJpegTargets;
    private final int mNumPreviewTargets;
    private boolean mOutputAbandoned;
    private final boolean mRepeating;
    private final CaptureRequest mRequest;
    private final int mRequestId;
    private final int mSubsequeceId;

    public static final class Builder {
        private final Collection<Long> mJpegSurfaceIds;
        private final int mNumJpegTargets;
        private final int mNumPreviewTargets;
        private final boolean mRepeating;
        private final CaptureRequest mRequest;
        private final int mRequestId;
        private final int mSubsequenceId;

        public Builder(int i, int i2, CaptureRequest captureRequest, boolean z, Collection<Long> collection) {
            Preconditions.checkNotNull(captureRequest, "request must not be null");
            this.mRequestId = i;
            this.mSubsequenceId = i2;
            this.mRequest = captureRequest;
            this.mRepeating = z;
            this.mJpegSurfaceIds = collection;
            this.mNumJpegTargets = numJpegTargets(this.mRequest);
            this.mNumPreviewTargets = numPreviewTargets(this.mRequest);
        }

        private boolean jpegType(Surface surface) throws LegacyExceptionUtils.BufferQueueAbandonedException {
            return LegacyCameraDevice.containsSurfaceId(surface, this.mJpegSurfaceIds);
        }

        private boolean previewType(Surface surface) throws LegacyExceptionUtils.BufferQueueAbandonedException {
            return !jpegType(surface);
        }

        private int numJpegTargets(CaptureRequest captureRequest) {
            Iterator<Surface> it = captureRequest.getTargets().iterator();
            int i = 0;
            while (it.hasNext()) {
                try {
                    if (jpegType(it.next())) {
                        i++;
                    }
                } catch (LegacyExceptionUtils.BufferQueueAbandonedException e) {
                    Log.d(RequestHolder.TAG, "Surface abandoned, skipping...", e);
                }
            }
            return i;
        }

        private int numPreviewTargets(CaptureRequest captureRequest) {
            Iterator<Surface> it = captureRequest.getTargets().iterator();
            int i = 0;
            while (it.hasNext()) {
                try {
                    if (previewType(it.next())) {
                        i++;
                    }
                } catch (LegacyExceptionUtils.BufferQueueAbandonedException e) {
                    Log.d(RequestHolder.TAG, "Surface abandoned, skipping...", e);
                }
            }
            return i;
        }

        public RequestHolder build(long j) {
            return new RequestHolder(this.mRequestId, this.mSubsequenceId, this.mRequest, this.mRepeating, j, this.mNumJpegTargets, this.mNumPreviewTargets, this.mJpegSurfaceIds);
        }
    }

    private RequestHolder(int i, int i2, CaptureRequest captureRequest, boolean z, long j, int i3, int i4, Collection<Long> collection) {
        this.mFailed = false;
        this.mOutputAbandoned = false;
        this.mRepeating = z;
        this.mRequest = captureRequest;
        this.mRequestId = i;
        this.mSubsequeceId = i2;
        this.mFrameNumber = j;
        this.mNumJpegTargets = i3;
        this.mNumPreviewTargets = i4;
        this.mJpegSurfaceIds = collection;
    }

    public int getRequestId() {
        return this.mRequestId;
    }

    public boolean isRepeating() {
        return this.mRepeating;
    }

    public int getSubsequeceId() {
        return this.mSubsequeceId;
    }

    public long getFrameNumber() {
        return this.mFrameNumber;
    }

    public CaptureRequest getRequest() {
        return this.mRequest;
    }

    public Collection<Surface> getHolderTargets() {
        return getRequest().getTargets();
    }

    public boolean hasJpegTargets() {
        return this.mNumJpegTargets > 0;
    }

    public boolean hasPreviewTargets() {
        return this.mNumPreviewTargets > 0;
    }

    public int numJpegTargets() {
        return this.mNumJpegTargets;
    }

    public int numPreviewTargets() {
        return this.mNumPreviewTargets;
    }

    public boolean jpegType(Surface surface) throws LegacyExceptionUtils.BufferQueueAbandonedException {
        return LegacyCameraDevice.containsSurfaceId(surface, this.mJpegSurfaceIds);
    }

    public void failRequest() {
        Log.w(TAG, "Capture failed for request: " + getRequestId());
        this.mFailed = true;
    }

    public boolean requestFailed() {
        return this.mFailed;
    }

    public void setOutputAbandoned() {
        this.mOutputAbandoned = true;
    }

    public boolean isOutputAbandoned() {
        return this.mOutputAbandoned;
    }
}
