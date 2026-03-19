package com.mediatek.gallery3d.adapter;

import com.android.gallery3d.app.GalleryAppImpl;
import com.android.gallery3d.util.ThreadPool;
import com.mediatek.gallery3d.util.Log;
import com.mediatek.gallerybasic.base.MediaData;
import com.mediatek.gallerybasic.base.Work;
import com.mediatek.gallerybasic.platform.Platform;
import com.mediatek.gallerybasic.util.DecodeSpecLimitor;

public class PlatformImpl implements Platform {
    private ThreadPoolBridge mThreadPoolBridge;

    public PlatformImpl(GalleryAppImpl galleryAppImpl) {
        this.mThreadPoolBridge = new ThreadPoolBridge(galleryAppImpl);
    }

    @Override
    public boolean isOutOfDecodeSpec(long j, int i, int i2, String str) {
        return DecodeSpecLimitor.isOutOfSpecLimit(j, i, i2, str);
    }

    @Override
    public void submitJob(Work work) {
        this.mThreadPoolBridge.submit(work);
    }

    private static class ThreadPoolBridge<T> {
        private ThreadPool mThreadPool;

        public ThreadPoolBridge(GalleryAppImpl galleryAppImpl) {
            this.mThreadPool = galleryAppImpl.getThreadPool();
            Log.d("MtkGallery2/PlatformImpl", "<ThreadPoolBridge> mThreadPool " + this.mThreadPool);
        }

        public void submit(Work work) {
            this.mThreadPool.submit(new BridgeJob(work));
        }

        private class BridgeJob implements ThreadPool.Job<T> {
            private Work mWork;

            public BridgeJob(Work work) {
                this.mWork = work;
            }

            @Override
            public T run(ThreadPool.JobContext jobContext) {
                if (this.mWork != null && !this.mWork.isCanceled()) {
                    return (T) this.mWork.run();
                }
                return null;
            }
        }
    }

    @Override
    public float getMinScaleLimit(MediaData mediaData) {
        return 4.0f;
    }
}
