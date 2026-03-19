package com.mediatek.camera.common.device;

import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory;
import java.util.concurrent.locks.ReentrantLock;

public abstract class CameraDeviceManager {
    public abstract void close(String str);

    public abstract void closeSync(String str);

    public abstract void openCamera(String str, CameraStateCallback cameraStateCallback, Handler handler) throws CameraOpenException;

    public abstract void openCameraSync(String str, CameraStateCallback cameraStateCallback, Handler handler) throws CameraOpenException;

    public abstract void recycle();

    protected abstract class CameraProxyCreator {
        protected CameraDeviceManagerFactory.CameraApi mApiVersion;
        protected String mCameraId;
        protected HandlerThread mRequestThread;
        protected HandlerThread mRespondThread;
        protected ReentrantLock mThreadLock = new ReentrantLock(true);
        protected int mRetryCount = 0;

        protected CameraProxyCreator(CameraDeviceManagerFactory.CameraApi cameraApi, String str) {
            this.mApiVersion = cameraApi;
            this.mCameraId = str;
            createHandlerThreads();
        }

        protected void createHandlerThreads() {
            this.mThreadLock.lock();
            this.mRequestThread = new HandlerThread(this.mApiVersion + "-Request-" + this.mCameraId);
            this.mRespondThread = new HandlerThread(this.mApiVersion + "-Response-" + this.mCameraId);
            this.mRequestThread.start();
            this.mRespondThread.start();
            this.mThreadLock.unlock();
        }

        public void destroyHandlerThreads() {
            this.mThreadLock.lock();
            if (this.mRequestThread.isAlive()) {
                if (Build.VERSION.SDK_INT >= 18) {
                    this.mRequestThread.getLooper().quitSafely();
                } else {
                    this.mRequestThread.getLooper().quit();
                }
            }
            if (this.mRespondThread.isAlive()) {
                if (Build.VERSION.SDK_INT >= 18) {
                    this.mRespondThread.getLooper().quitSafely();
                } else {
                    this.mRespondThread.getLooper().quit();
                }
            }
            this.mThreadLock.unlock();
        }
    }
}
