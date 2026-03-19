package com.android.gallery3d.app;

import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.PanoramaMetadataJob;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.FutureListener;
import com.android.gallery3d.util.LightCycleHelper;
import java.util.ArrayList;
import java.util.Iterator;

public class PanoramaMetadataSupport implements FutureListener<LightCycleHelper.PanoramaMetadata> {
    private ArrayList<MediaObject.PanoramaSupportCallback> mCallbacksWaiting;
    private Future<LightCycleHelper.PanoramaMetadata> mGetPanoMetadataTask;
    private Object mLock = new Object();
    private MediaObject mMediaObject;
    private LightCycleHelper.PanoramaMetadata mPanoramaMetadata;

    public PanoramaMetadataSupport(MediaObject mediaObject) {
        this.mMediaObject = mediaObject;
    }

    public void getPanoramaSupport(GalleryApp galleryApp, MediaObject.PanoramaSupportCallback panoramaSupportCallback) {
        synchronized (this.mLock) {
            if (this.mPanoramaMetadata != null) {
                panoramaSupportCallback.panoramaInfoAvailable(this.mMediaObject, this.mPanoramaMetadata.mUsePanoramaViewer, this.mPanoramaMetadata.mIsPanorama360);
            } else {
                if (this.mCallbacksWaiting == null) {
                    this.mCallbacksWaiting = new ArrayList<>();
                    this.mGetPanoMetadataTask = galleryApp.getThreadPool().submit(new PanoramaMetadataJob(galleryApp.getAndroidContext(), this.mMediaObject.getContentUri()), this);
                }
                this.mCallbacksWaiting.add(panoramaSupportCallback);
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
            Iterator<MediaObject.PanoramaSupportCallback> it = this.mCallbacksWaiting.iterator();
            while (it.hasNext()) {
                it.next().panoramaInfoAvailable(this.mMediaObject, this.mPanoramaMetadata.mUsePanoramaViewer, this.mPanoramaMetadata.mIsPanorama360);
            }
            this.mGetPanoMetadataTask = null;
            this.mCallbacksWaiting = null;
        }
    }
}
