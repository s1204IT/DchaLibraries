package com.android.camera.one.v2;

import android.annotation.TargetApi;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Pair;
import com.android.camera.debug.Log;
import com.android.camera.util.ConcurrentSharedRingBuffer;
import com.android.camera.util.Task;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

@TargetApi(21)
public class ImageCaptureManager extends CameraCaptureSession.CaptureCallback implements ImageReader.OnImageAvailableListener {
    private static final long DEBUG_INTERFRAME_STALL_WARNING = 5;
    private static final long DEBUG_MAX_IMAGE_CALLBACK_DUR = 25;
    private static final boolean DEBUG_PRINT_OPEN_IMAGE_COUNT = false;
    private static final Log.Tag TAG = new Log.Tag("ZSLImageListener");
    private final ConcurrentSharedRingBuffer<CapturedImage> mCapturedImageBuffer;
    private final Executor mImageCaptureListenerExecutor;
    private final Handler mListenerHandler;
    private ImageCaptureListener mPendingImageCaptureCallback;
    private List<CapturedImageConstraint> mPendingImageCaptureConstraints;
    private long mDebugLastOnCaptureCompletedMillis = 0;
    private long mDebugStalledFrameCount = 0;
    private final AtomicInteger mNumOpenImages = new AtomicInteger(0);
    private final Map<CaptureResult.Key<?>, Pair<Long, Object>> mMetadata = new ConcurrentHashMap();
    private final Map<CaptureResult.Key<?>, Set<MetadataChangeListener>> mMetadataChangeListeners = new ConcurrentHashMap();

    public interface CaptureReadyListener {
        void onReadyStateChange(boolean z);
    }

    public interface CapturedImageConstraint {
        boolean satisfiesConstraint(TotalCaptureResult totalCaptureResult);
    }

    public interface ImageCaptureListener {
        void onImageCaptured(Image image, TotalCaptureResult totalCaptureResult);
    }

    public interface MetadataChangeListener {
        void onImageMetadataChange(CaptureResult.Key<?> key, Object obj, Object obj2, CaptureResult captureResult);
    }

    private class CapturedImage {
        private Image mImage;
        private TotalCaptureResult mMetadata;

        private CapturedImage() {
            this.mImage = null;
            this.mMetadata = null;
        }

        public void reset() {
            if (this.mImage != null) {
                this.mImage.close();
                ImageCaptureManager.this.mNumOpenImages.decrementAndGet();
            }
            this.mImage = null;
            this.mMetadata = null;
        }

        public boolean isComplete() {
            return (this.mImage == null || this.mMetadata == null) ? false : true;
        }

        public void addImage(Image image) {
            if (this.mImage != null) {
                throw new IllegalArgumentException("Unable to add an Image when one already exists.");
            }
            this.mImage = image;
        }

        public Image tryGetImage() {
            return this.mImage;
        }

        public void addMetadata(TotalCaptureResult metadata) {
            if (this.mMetadata != null) {
                throw new IllegalArgumentException("Unable to add a TotalCaptureResult when one already exists.");
            }
            this.mMetadata = metadata;
        }

        public TotalCaptureResult tryGetMetadata() {
            return this.mMetadata;
        }
    }

    ImageCaptureManager(int maxImages, Handler listenerHandler, Executor imageCaptureListenerExecutor) {
        this.mCapturedImageBuffer = new ConcurrentSharedRingBuffer<>(maxImages - 2);
        this.mListenerHandler = listenerHandler;
        this.mImageCaptureListenerExecutor = imageCaptureListenerExecutor;
    }

    public void setCaptureReadyListener(final CaptureReadyListener listener) {
        this.mCapturedImageBuffer.setListener(this.mListenerHandler, new ConcurrentSharedRingBuffer.PinStateListener() {
            @Override
            public void onPinStateChange(boolean pinsAvailable) {
                listener.onReadyStateChange(pinsAvailable);
            }
        });
    }

    public <T> void addMetadataChangeListener(CaptureResult.Key<T> key, MetadataChangeListener listener) {
        if (!this.mMetadataChangeListeners.containsKey(key)) {
            this.mMetadataChangeListeners.put(key, Collections.newSetFromMap(new ConcurrentHashMap()));
        }
        this.mMetadataChangeListeners.get(key).add(listener);
    }

    public <T> boolean removeMetadataChangeListener(CaptureResult.Key<T> key, MetadataChangeListener listener) {
        if (this.mMetadataChangeListeners.containsKey(key)) {
            return this.mMetadataChangeListeners.get(key).remove(listener);
        }
        return false;
    }

    @Override
    public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, final CaptureResult partialResult) {
        long frameNumber = partialResult.getFrameNumber();
        for (final CaptureResult.Key<?> key : partialResult.getKeys()) {
            Pair<Long, Object> oldEntry = this.mMetadata.get(key);
            final Object oldValue = oldEntry != null ? oldEntry.second : null;
            boolean newerValueAlreadyExists = oldEntry != null && frameNumber < ((Long) oldEntry.first).longValue();
            if (!newerValueAlreadyExists) {
                final Object newValue = partialResult.get(key);
                this.mMetadata.put(key, new Pair<>(Long.valueOf(frameNumber), newValue));
                if (oldValue != newValue && this.mMetadataChangeListeners.containsKey(key)) {
                    for (final MetadataChangeListener listener : this.mMetadataChangeListeners.get(key)) {
                        Log.v(TAG, "Dispatching to metadata change listener for key: " + key.toString());
                        this.mListenerHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                listener.onImageMetadataChange(key, oldValue, newValue, partialResult);
                            }
                        });
                    }
                }
            }
        }
    }

    @Override
    public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, final TotalCaptureResult result) {
        long timestamp = ((Long) result.get(TotalCaptureResult.SENSOR_TIMESTAMP)).longValue();
        long now = SystemClock.uptimeMillis();
        if (now - this.mDebugLastOnCaptureCompletedMillis < DEBUG_INTERFRAME_STALL_WARNING) {
            Log.Tag tag = TAG;
            StringBuilder sbAppend = new StringBuilder().append("Camera thread has stalled for ");
            long j = this.mDebugStalledFrameCount + 1;
            this.mDebugStalledFrameCount = j;
            Log.e(tag, sbAppend.append(j).append(" frames at # ").append(result.getFrameNumber()).append(".").toString());
        } else {
            this.mDebugStalledFrameCount = 0L;
        }
        this.mDebugLastOnCaptureCompletedMillis = now;
        boolean swapSuccess = this.mCapturedImageBuffer.swapLeast(timestamp, new ConcurrentSharedRingBuffer.SwapTask<CapturedImage>() {
            @Override
            public CapturedImage create() {
                CapturedImage image = new CapturedImage();
                image.addMetadata(result);
                return image;
            }

            @Override
            public CapturedImage swap(CapturedImage oldElement) {
                oldElement.reset();
                oldElement.addMetadata(result);
                return oldElement;
            }

            @Override
            public void update(CapturedImage existingElement) {
                existingElement.addMetadata(result);
            }
        });
        if (!swapSuccess) {
            Log.v(TAG, "Unable to add new image metadata to ring-buffer.");
        }
        tryExecutePendingCaptureRequest(timestamp);
    }

    @Override
    public void onImageAvailable(ImageReader reader) {
        long startTime = SystemClock.currentThreadTimeMillis();
        final Image img = reader.acquireLatestImage();
        if (img != null) {
            this.mNumOpenImages.incrementAndGet();
            boolean swapSuccess = this.mCapturedImageBuffer.swapLeast(img.getTimestamp(), new ConcurrentSharedRingBuffer.SwapTask<CapturedImage>() {
                @Override
                public CapturedImage create() {
                    CapturedImage image = new CapturedImage();
                    image.addImage(img);
                    return image;
                }

                @Override
                public CapturedImage swap(CapturedImage oldElement) {
                    oldElement.reset();
                    oldElement.addImage(img);
                    return oldElement;
                }

                @Override
                public void update(CapturedImage existingElement) {
                    existingElement.addImage(img);
                }
            });
            if (!swapSuccess) {
                img.close();
                this.mNumOpenImages.decrementAndGet();
            }
            tryExecutePendingCaptureRequest(img.getTimestamp());
            long endTime = SystemClock.currentThreadTimeMillis();
            long totTime = endTime - startTime;
            if (totTime > DEBUG_MAX_IMAGE_CALLBACK_DUR) {
                Log.v(TAG, "onImageAvailable() took " + totTime + "ms");
            }
        }
    }

    public void close() {
        try {
            this.mCapturedImageBuffer.close(new Task<CapturedImage>() {
                @Override
                public void run(CapturedImage e) {
                    e.reset();
                }
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void captureNextImage(ImageCaptureListener onImageCaptured, List<CapturedImageConstraint> constraints) {
        this.mPendingImageCaptureCallback = onImageCaptured;
        this.mPendingImageCaptureConstraints = constraints;
    }

    private void tryExecutePendingCaptureRequest(long newImageTimestamp) {
        Pair<Long, CapturedImage> pinnedImage;
        if (this.mPendingImageCaptureCallback != null && (pinnedImage = this.mCapturedImageBuffer.tryPin(newImageTimestamp)) != null) {
            CapturedImage image = (CapturedImage) pinnedImage.second;
            if (!image.isComplete()) {
                this.mCapturedImageBuffer.release(((Long) pinnedImage.first).longValue());
                return;
            }
            TotalCaptureResult captureResult = image.tryGetMetadata();
            if (this.mPendingImageCaptureConstraints != null) {
                for (CapturedImageConstraint constraint : this.mPendingImageCaptureConstraints) {
                    if (!constraint.satisfiesConstraint(captureResult)) {
                        this.mCapturedImageBuffer.release(((Long) pinnedImage.first).longValue());
                        return;
                    }
                }
            }
            if (tryExecuteCaptureOrRelease(pinnedImage, this.mPendingImageCaptureCallback)) {
                this.mPendingImageCaptureCallback = null;
                this.mPendingImageCaptureConstraints = null;
            }
        }
    }

    public boolean tryCaptureExistingImage(ImageCaptureListener onImageCaptured, final List<CapturedImageConstraint> constraints) {
        ConcurrentSharedRingBuffer.Selector<CapturedImage> selector;
        if (constraints == null || constraints.isEmpty()) {
            selector = new ConcurrentSharedRingBuffer.Selector<CapturedImage>() {
                @Override
                public boolean select(CapturedImage image) {
                    return true;
                }
            };
        } else {
            selector = new ConcurrentSharedRingBuffer.Selector<CapturedImage>() {
                @Override
                public boolean select(CapturedImage e) {
                    TotalCaptureResult captureResult = e.tryGetMetadata();
                    if (captureResult == null || e.tryGetImage() == null) {
                        return false;
                    }
                    for (CapturedImageConstraint constraint : constraints) {
                        if (!constraint.satisfiesConstraint(captureResult)) {
                            return false;
                        }
                    }
                    return true;
                }
            };
        }
        Pair<Long, CapturedImage> toCapture = this.mCapturedImageBuffer.tryPinGreatestSelected(selector);
        return tryExecuteCaptureOrRelease(toCapture, onImageCaptured);
    }

    private boolean tryExecuteCaptureOrRelease(final Pair<Long, CapturedImage> toCapture, final ImageCaptureListener callback) {
        if (toCapture == null) {
            return false;
        }
        try {
            this.mImageCaptureListenerExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        CapturedImage img = (CapturedImage) toCapture.second;
                        callback.onImageCaptured(img.tryGetImage(), img.tryGetMetadata());
                    } finally {
                        ImageCaptureManager.this.mCapturedImageBuffer.release(((Long) toCapture.first).longValue());
                    }
                }
            });
            return true;
        } catch (RejectedExecutionException e) {
            this.mCapturedImageBuffer.release(((Long) toCapture.first).longValue());
            return false;
        }
    }
}
