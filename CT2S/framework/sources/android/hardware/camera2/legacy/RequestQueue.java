package android.hardware.camera2.legacy;

import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.utils.LongParcelable;
import android.util.Log;
import android.util.Pair;
import java.util.ArrayDeque;
import java.util.List;

public class RequestQueue {
    private static final long INVALID_FRAME = -1;
    private static final String TAG = "RequestQueue";
    private final List<Long> mJpegSurfaceIds;
    private BurstHolder mRepeatingRequest = null;
    private final ArrayDeque<BurstHolder> mRequestQueue = new ArrayDeque<>();
    private long mCurrentFrameNumber = 0;
    private long mCurrentRepeatingFrameNumber = -1;
    private int mCurrentRequestId = 0;

    public RequestQueue(List<Long> jpegSurfaceIds) {
        this.mJpegSurfaceIds = jpegSurfaceIds;
    }

    public synchronized Pair<BurstHolder, Long> getNext() {
        Pair<BurstHolder, Long> pair;
        BurstHolder next = this.mRequestQueue.poll();
        if (next == null && this.mRepeatingRequest != null) {
            next = this.mRepeatingRequest;
            this.mCurrentRepeatingFrameNumber = this.mCurrentFrameNumber + ((long) next.getNumberOfRequests());
        }
        if (next == null) {
            pair = null;
        } else {
            pair = new Pair<>(next, Long.valueOf(this.mCurrentFrameNumber));
            this.mCurrentFrameNumber += (long) next.getNumberOfRequests();
        }
        return pair;
    }

    public synchronized long stopRepeating(int requestId) {
        long ret;
        ret = -1;
        if (this.mRepeatingRequest != null && this.mRepeatingRequest.getRequestId() == requestId) {
            this.mRepeatingRequest = null;
            ret = this.mCurrentRepeatingFrameNumber == -1 ? -1L : this.mCurrentRepeatingFrameNumber - 1;
            this.mCurrentRepeatingFrameNumber = -1L;
            Log.i(TAG, "Repeating capture request cancelled.");
        } else {
            Log.e(TAG, "cancel failed: no repeating request exists for request id: " + requestId);
        }
        return ret;
    }

    public synchronized long stopRepeating() {
        long jStopRepeating;
        if (this.mRepeatingRequest == null) {
            Log.e(TAG, "cancel failed: no repeating request exists.");
            jStopRepeating = -1;
        } else {
            jStopRepeating = stopRepeating(this.mRepeatingRequest.getRequestId());
        }
        return jStopRepeating;
    }

    public synchronized int submit(List<CaptureRequest> requests, boolean repeating, LongParcelable frameNumber) {
        int requestId;
        requestId = this.mCurrentRequestId;
        this.mCurrentRequestId = requestId + 1;
        BurstHolder burst = new BurstHolder(requestId, repeating, requests, this.mJpegSurfaceIds);
        long ret = -1;
        if (burst.isRepeating()) {
            Log.i(TAG, "Repeating capture request set.");
            if (this.mRepeatingRequest != null) {
                ret = this.mCurrentRepeatingFrameNumber == -1 ? -1L : this.mCurrentRepeatingFrameNumber - 1;
            }
            this.mCurrentRepeatingFrameNumber = -1L;
            this.mRepeatingRequest = burst;
        } else {
            this.mRequestQueue.offer(burst);
            ret = calculateLastFrame(burst.getRequestId());
        }
        frameNumber.setNumber(ret);
        return requestId;
    }

    private long calculateLastFrame(int requestId) {
        long total = this.mCurrentFrameNumber;
        for (BurstHolder b : this.mRequestQueue) {
            total += (long) b.getNumberOfRequests();
            if (b.getRequestId() == requestId) {
                return total - 1;
            }
        }
        throw new IllegalStateException("At least one request must be in the queue to calculate frame number");
    }
}
