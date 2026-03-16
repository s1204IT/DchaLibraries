package com.android.printspooler.model;

import android.util.Log;
import java.io.File;
import java.io.IOException;

public final class MutexFileProvider {
    private final File mFile;
    private final Object mLock = new Object();
    private OnReleaseRequestCallback mOnReleaseRequestCallback;
    private Thread mOwnerThread;

    public interface OnReleaseRequestCallback {
        void onReleaseRequested(File file);
    }

    public MutexFileProvider(File file) throws IOException {
        this.mFile = file;
        if (file.exists()) {
            file.delete();
        }
        file.createNewFile();
    }

    public File acquireFile(OnReleaseRequestCallback callback) {
        File file;
        synchronized (this.mLock) {
            if (this.mOwnerThread == Thread.currentThread()) {
                file = this.mFile;
            } else {
                if (this.mOwnerThread != null && this.mOnReleaseRequestCallback != null) {
                    this.mOnReleaseRequestCallback.onReleaseRequested(this.mFile);
                }
                while (this.mOwnerThread != null) {
                    try {
                        this.mLock.wait();
                    } catch (InterruptedException e) {
                    }
                }
                this.mOwnerThread = Thread.currentThread();
                this.mOnReleaseRequestCallback = callback;
                Log.i("MutexFileProvider", "Acquired file: " + this.mFile + " by thread: " + this.mOwnerThread);
                file = this.mFile;
            }
        }
        return file;
    }

    public void releaseFile() {
        synchronized (this.mLock) {
            if (this.mOwnerThread == Thread.currentThread()) {
                Log.i("MutexFileProvider", "Released file: " + this.mFile + " from thread: " + this.mOwnerThread);
                this.mOwnerThread = null;
                this.mOnReleaseRequestCallback = null;
                this.mLock.notifyAll();
            }
        }
    }
}
