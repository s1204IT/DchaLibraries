package com.android.documentsui.archives;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.util.Log;
import com.android.internal.annotations.GuardedBy;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Loader {
    static final boolean $assertionsDisabled = false;
    private final int mAccessMode;
    private final Uri mArchiveUri;
    private final Context mContext;
    private final Uri mNotificationUri;
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private int mStatus = 0;

    @GuardedBy("mLock")
    private int mRefCount = 0;
    private Archive mArchive = null;

    Loader(Context context, Uri uri, int i, Uri uri2) {
        this.mContext = context;
        this.mArchiveUri = uri;
        this.mAccessMode = i;
        this.mNotificationUri = uri2;
        this.mExecutor.submit(new Callable() {
            @Override
            public final Object call() {
                return this.f$0.get();
            }
        });
    }

    synchronized Archive get() {
        synchronized (this.mLock) {
            if (this.mStatus == 1) {
                return this.mArchive;
            }
            synchronized (this.mLock) {
                if (this.mStatus != 0) {
                    throw new IllegalStateException("Trying to perform an operation on an archive which is invalidated.");
                }
            }
            try {
                try {
                    if (ReadableArchive.supportsAccessMode(this.mAccessMode)) {
                        this.mArchive = ReadableArchive.createForParcelFileDescriptor(this.mContext, this.mContext.getContentResolver().openFileDescriptor(this.mArchiveUri, "r", null), this.mArchiveUri, this.mAccessMode, this.mNotificationUri);
                    } else {
                        if (!WriteableArchive.supportsAccessMode(this.mAccessMode)) {
                            throw new IllegalStateException("Access mode not supported.");
                        }
                        this.mArchive = WriteableArchive.createForParcelFileDescriptor(this.mContext, this.mContext.getContentResolver().openFileDescriptor(this.mArchiveUri, "w", null), this.mArchiveUri, this.mAccessMode, this.mNotificationUri);
                    }
                    synchronized (this.mLock) {
                        if (this.mRefCount == 0) {
                            this.mArchive.close();
                            this.mStatus = 4;
                        } else {
                            this.mStatus = 1;
                        }
                    }
                    synchronized (this.mLock) {
                        try {
                            if (this.mRefCount > 0) {
                                this.mContext.getContentResolver().notifyChange(ArchivesProvider.buildUriForArchive(this.mArchiveUri, this.mAccessMode), (ContentObserver) null, false);
                            }
                        } catch (Throwable th) {
                            throw th;
                        }
                    }
                    return this.mArchive;
                } catch (Throwable th2) {
                    synchronized (this.mLock) {
                        if (this.mRefCount > 0) {
                            this.mContext.getContentResolver().notifyChange(ArchivesProvider.buildUriForArchive(this.mArchiveUri, this.mAccessMode), (ContentObserver) null, false);
                        }
                        throw th2;
                    }
                }
            } catch (IOException | RuntimeException e) {
                Log.e("Loader", "Failed to open the archive.", e);
                synchronized (this.mLock) {
                    this.mStatus = 2;
                    throw new IllegalStateException("Failed to open the archive.", e);
                }
            }
        }
    }

    int getStatus() {
        int i;
        synchronized (this.mLock) {
            i = this.mStatus;
        }
        return i;
    }

    void acquire() {
        synchronized (this.mLock) {
            this.mRefCount++;
        }
    }

    void release() {
        synchronized (this.mLock) {
            this.mRefCount--;
            if (this.mRefCount == 0) {
                switch (this.mStatus) {
                    case 0:
                        this.mStatus = 3;
                        break;
                    case 1:
                        try {
                            this.mArchive.close();
                            this.mStatus = 4;
                        } catch (IOException e) {
                            Log.e("Loader", "Failed to close the archive on release.", e);
                        }
                        break;
                    case 2:
                        this.mStatus = 4;
                        break;
                }
            }
        }
    }
}
