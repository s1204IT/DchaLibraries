package com.android.gallery3d.app;

import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.PanoramaMetadataJob;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.FutureListener;
import com.android.gallery3d.util.LightCycleHelper;
import java.util.ArrayList;

public class PanoramaMetadataSupport implements FutureListener<LightCycleHelper.PanoramaMetadata> {
    private ArrayList<MediaObject.PanoramaSupportCallback> mCallbacksWaiting;
    private Future<LightCycleHelper.PanoramaMetadata> mGetPanoMetadataTask;
    private Object mLock = new Object();
    private MediaObject mMediaObject;
    private LightCycleHelper.PanoramaMetadata mPanoramaMetadata;

    public PanoramaMetadataSupport(MediaObject mediaObject) {
        this.mMediaObject = mediaObject;
    }

    public void getPanoramaSupport(GalleryApp app, MediaObject.PanoramaSupportCallback callback) {
        synchronized (this.mLock) {
            if (this.mPanoramaMetadata != null) {
                callback.panoramaInfoAvailable(this.mMediaObject, this.mPanoramaMetadata.mUsePanoramaViewer, this.mPanoramaMetadata.mIsPanorama360);
            } else {
                if (this.mCallbacksWaiting == null) {
                    this.mCallbacksWaiting = new ArrayList<>();
                    this.mGetPanoMetadataTask = app.getThreadPool().submit(new PanoramaMetadataJob(app.getAndroidContext(), this.mMediaObject.getContentUri()), this);
                }
                this.mCallbacksWaiting.add(callback);
            }
        }
    }

    @Override
    public void onFutureDone(Future<LightCycleHelper.PanoramaMetadata> future) {
        synchronized (this.mLock) {
            this.mPanoramaMetadata = future.get();
            if (this.mPanoramaMetadata == null) {
                this.mPanoramaMetadata = LightCycleHelper.NOT_PANORAMA;
            }
            for (MediaObject.PanoramaSupportCallback cb : this.mCallbacksWaiting) {
                cb.panoramaInfoAvailable(this.mMediaObject, this.mPanoramaMetadata.mUsePanoramaViewer, this.mPanoramaMetadata.mIsPanorama360);
            }
            this.mGetPanoMetadataTask = null;
            this.mCallbacksWaiting = null;
        }
    }
}
