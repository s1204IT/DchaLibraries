package com.mediatek.camera.common.utils;

import android.os.Handler;
import android.os.Message;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import java.util.concurrent.Semaphore;

public final class AtomAccessor {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(AtomAccessor.class.getSimpleName());
    private Object mResourceAccessLock = new Object();
    private final Semaphore mSingleResource;

    public AtomAccessor() {
        LogHelper.d(TAG, "[AtomAccessor]");
        this.mSingleResource = new Semaphore(1, true);
    }

    public void sendAtomMessageAtFrontOfQueue(Handler handler, Message message) {
        if (handler == null || message == null) {
            return;
        }
        acquireResource();
        handler.sendMessageAtFrontOfQueue(message);
        releaseResource();
    }

    public void sendAtomMessage(Handler handler, Message message) {
        if (handler == null || message == null) {
            return;
        }
        acquireResource();
        handler.sendMessage(message);
        releaseResource();
    }

    public boolean sendAtomMessageAndWait(Handler handler, Message message, Runnable runnable) {
        if (handler == null || message == null) {
            return false;
        }
        acquireResource();
        handler.sendMessage(message);
        return waitDoneAndReleaseResource(handler, runnable);
    }

    public boolean sendAtomMessageAndWait(Handler handler, Message message) {
        if (handler == null || message == null) {
            return false;
        }
        acquireResource();
        handler.sendMessage(message);
        return waitDoneAndReleaseResource(handler, null);
    }

    public void acquireResource() {
        synchronized (this.mResourceAccessLock) {
            this.mSingleResource.acquireUninterruptibly();
        }
    }

    public void releaseResource() {
        this.mSingleResource.release();
    }

    public boolean waitDoneAndReleaseResource(Handler handler, Runnable runnable) {
        boolean zPost;
        if (handler == null) {
            return false;
        }
        final Object obj = new Object();
        Runnable runnable2 = new Runnable() {
            @Override
            public void run() {
                synchronized (obj) {
                    obj.notifyAll();
                }
            }
        };
        synchronized (obj) {
            zPost = handler.post(runnable2);
            if (zPost && runnable != null) {
                handler.post(runnable);
            }
            this.mSingleResource.release();
            if (zPost) {
                try {
                    obj.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return false;
                }
            }
        }
        return zPost;
    }
}
