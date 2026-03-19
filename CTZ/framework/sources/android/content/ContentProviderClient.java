package android.content;

import android.content.res.AssetFileDescriptor;
import android.database.CrossProcessCursorWrapper;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.ICancellationSignal;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.provider.CalendarContract;
import android.util.Log;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import dalvik.system.CloseGuard;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class ContentProviderClient implements AutoCloseable {
    private static final String TAG = "ContentProviderClient";

    @GuardedBy("ContentProviderClient.class")
    private static Handler sAnrHandler;
    private NotRespondingRunnable mAnrRunnable;
    private long mAnrTimeout;
    private final IContentProvider mContentProvider;
    private final ContentResolver mContentResolver;
    private final String mPackageName;
    private final boolean mStable;
    private final AtomicBoolean mClosed = new AtomicBoolean();
    private final CloseGuard mCloseGuard = CloseGuard.get();

    @VisibleForTesting
    public ContentProviderClient(ContentResolver contentResolver, IContentProvider iContentProvider, boolean z) {
        this.mContentResolver = contentResolver;
        this.mContentProvider = iContentProvider;
        this.mPackageName = contentResolver.mPackageName;
        this.mStable = z;
        this.mCloseGuard.open("close");
    }

    public void setDetectNotResponding(long j) {
        synchronized (ContentProviderClient.class) {
            this.mAnrTimeout = j;
            if (j > 0) {
                if (this.mAnrRunnable == null) {
                    this.mAnrRunnable = new NotRespondingRunnable();
                }
                if (sAnrHandler == null) {
                    sAnrHandler = new Handler(Looper.getMainLooper(), null, true);
                }
                Binder.allowBlocking(this.mContentProvider.asBinder());
            } else {
                this.mAnrRunnable = null;
                Binder.defaultBlocking(this.mContentProvider.asBinder());
            }
        }
    }

    private void beforeRemote() {
        if (this.mAnrRunnable != null) {
            sAnrHandler.postDelayed(this.mAnrRunnable, this.mAnrTimeout);
        }
    }

    private void afterRemote() {
        if (this.mAnrRunnable != null) {
            sAnrHandler.removeCallbacks(this.mAnrRunnable);
        }
    }

    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) throws RemoteException {
        return query(uri, strArr, str, strArr2, str2, null);
    }

    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2, CancellationSignal cancellationSignal) throws RemoteException {
        return query(uri, strArr, ContentResolver.createSqlQueryBundle(str, strArr2, str2), cancellationSignal);
    }

    public Cursor query(Uri uri, String[] strArr, Bundle bundle, CancellationSignal cancellationSignal) throws RemoteException {
        ICancellationSignal iCancellationSignal;
        Preconditions.checkNotNull(uri, "url");
        beforeRemote();
        if (cancellationSignal != null) {
            try {
                try {
                    cancellationSignal.throwIfCanceled();
                    ICancellationSignal iCancellationSignalCreateCancellationSignal = this.mContentProvider.createCancellationSignal();
                    cancellationSignal.setRemote(iCancellationSignalCreateCancellationSignal);
                    iCancellationSignal = iCancellationSignalCreateCancellationSignal;
                } catch (DeadObjectException e) {
                    if (!this.mStable) {
                        this.mContentResolver.unstableProviderDied(this.mContentProvider);
                    }
                    throw e;
                }
            } catch (Throwable th) {
                afterRemote();
                throw th;
            }
        } else {
            iCancellationSignal = null;
        }
        Cursor cursorQuery = this.mContentProvider.query(this.mPackageName, uri, strArr, bundle, iCancellationSignal);
        if (cursorQuery == null) {
            afterRemote();
            return null;
        }
        CursorWrapperInner cursorWrapperInner = new CursorWrapperInner(cursorQuery);
        afterRemote();
        return cursorWrapperInner;
    }

    public String getType(Uri uri) throws RemoteException {
        Preconditions.checkNotNull(uri, "url");
        beforeRemote();
        try {
            try {
                return this.mContentProvider.getType(uri);
            } catch (DeadObjectException e) {
                if (!this.mStable) {
                    this.mContentResolver.unstableProviderDied(this.mContentProvider);
                }
                throw e;
            }
        } finally {
            afterRemote();
        }
    }

    public String[] getStreamTypes(Uri uri, String str) throws RemoteException {
        Preconditions.checkNotNull(uri, "url");
        Preconditions.checkNotNull(str, "mimeTypeFilter");
        beforeRemote();
        try {
            try {
                return this.mContentProvider.getStreamTypes(uri, str);
            } catch (DeadObjectException e) {
                if (!this.mStable) {
                    this.mContentResolver.unstableProviderDied(this.mContentProvider);
                }
                throw e;
            }
        } finally {
            afterRemote();
        }
    }

    public final Uri canonicalize(Uri uri) throws RemoteException {
        Preconditions.checkNotNull(uri, "url");
        beforeRemote();
        try {
            try {
                return this.mContentProvider.canonicalize(this.mPackageName, uri);
            } catch (DeadObjectException e) {
                if (!this.mStable) {
                    this.mContentResolver.unstableProviderDied(this.mContentProvider);
                }
                throw e;
            }
        } finally {
            afterRemote();
        }
    }

    public final Uri uncanonicalize(Uri uri) throws RemoteException {
        Preconditions.checkNotNull(uri, "url");
        beforeRemote();
        try {
            try {
                return this.mContentProvider.uncanonicalize(this.mPackageName, uri);
            } catch (DeadObjectException e) {
                if (!this.mStable) {
                    this.mContentResolver.unstableProviderDied(this.mContentProvider);
                }
                throw e;
            }
        } finally {
            afterRemote();
        }
    }

    public boolean refresh(Uri uri, Bundle bundle, CancellationSignal cancellationSignal) throws RemoteException {
        Preconditions.checkNotNull(uri, "url");
        beforeRemote();
        ICancellationSignal iCancellationSignalCreateCancellationSignal = null;
        if (cancellationSignal != null) {
            try {
                try {
                    cancellationSignal.throwIfCanceled();
                    iCancellationSignalCreateCancellationSignal = this.mContentProvider.createCancellationSignal();
                    cancellationSignal.setRemote(iCancellationSignalCreateCancellationSignal);
                } catch (DeadObjectException e) {
                    if (!this.mStable) {
                        this.mContentResolver.unstableProviderDied(this.mContentProvider);
                    }
                    throw e;
                }
            } catch (Throwable th) {
                afterRemote();
                throw th;
            }
        }
        boolean zRefresh = this.mContentProvider.refresh(this.mPackageName, uri, bundle, iCancellationSignalCreateCancellationSignal);
        afterRemote();
        return zRefresh;
    }

    public Uri insert(Uri uri, ContentValues contentValues) throws RemoteException {
        Preconditions.checkNotNull(uri, "url");
        beforeRemote();
        try {
            try {
                return this.mContentProvider.insert(this.mPackageName, uri, contentValues);
            } catch (DeadObjectException e) {
                if (!this.mStable) {
                    this.mContentResolver.unstableProviderDied(this.mContentProvider);
                }
                throw e;
            }
        } finally {
            afterRemote();
        }
    }

    public int bulkInsert(Uri uri, ContentValues[] contentValuesArr) throws RemoteException {
        Preconditions.checkNotNull(uri, "url");
        Preconditions.checkNotNull(contentValuesArr, "initialValues");
        beforeRemote();
        try {
            try {
                return this.mContentProvider.bulkInsert(this.mPackageName, uri, contentValuesArr);
            } catch (DeadObjectException e) {
                if (!this.mStable) {
                    this.mContentResolver.unstableProviderDied(this.mContentProvider);
                }
                throw e;
            }
        } finally {
            afterRemote();
        }
    }

    public int delete(Uri uri, String str, String[] strArr) throws RemoteException {
        Preconditions.checkNotNull(uri, "url");
        beforeRemote();
        try {
            try {
                return this.mContentProvider.delete(this.mPackageName, uri, str, strArr);
            } catch (DeadObjectException e) {
                if (!this.mStable) {
                    this.mContentResolver.unstableProviderDied(this.mContentProvider);
                }
                throw e;
            }
        } finally {
            afterRemote();
        }
    }

    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) throws RemoteException {
        Preconditions.checkNotNull(uri, "url");
        beforeRemote();
        try {
            try {
                return this.mContentProvider.update(this.mPackageName, uri, contentValues, str, strArr);
            } catch (DeadObjectException e) {
                if (!this.mStable) {
                    this.mContentResolver.unstableProviderDied(this.mContentProvider);
                }
                throw e;
            }
        } finally {
            afterRemote();
        }
    }

    public ParcelFileDescriptor openFile(Uri uri, String str) throws RemoteException, FileNotFoundException {
        return openFile(uri, str, null);
    }

    public ParcelFileDescriptor openFile(Uri uri, String str, CancellationSignal cancellationSignal) throws RemoteException, FileNotFoundException {
        Preconditions.checkNotNull(uri, "url");
        Preconditions.checkNotNull(str, "mode");
        beforeRemote();
        ICancellationSignal iCancellationSignalCreateCancellationSignal = null;
        if (cancellationSignal != null) {
            try {
                try {
                    cancellationSignal.throwIfCanceled();
                    iCancellationSignalCreateCancellationSignal = this.mContentProvider.createCancellationSignal();
                    cancellationSignal.setRemote(iCancellationSignalCreateCancellationSignal);
                } catch (DeadObjectException e) {
                    if (!this.mStable) {
                        this.mContentResolver.unstableProviderDied(this.mContentProvider);
                    }
                    throw e;
                }
            } catch (Throwable th) {
                afterRemote();
                throw th;
            }
        }
        ParcelFileDescriptor parcelFileDescriptorOpenFile = this.mContentProvider.openFile(this.mPackageName, uri, str, iCancellationSignalCreateCancellationSignal, null);
        afterRemote();
        return parcelFileDescriptorOpenFile;
    }

    public AssetFileDescriptor openAssetFile(Uri uri, String str) throws RemoteException, FileNotFoundException {
        return openAssetFile(uri, str, null);
    }

    public AssetFileDescriptor openAssetFile(Uri uri, String str, CancellationSignal cancellationSignal) throws RemoteException, FileNotFoundException {
        Preconditions.checkNotNull(uri, "url");
        Preconditions.checkNotNull(str, "mode");
        beforeRemote();
        ICancellationSignal iCancellationSignalCreateCancellationSignal = null;
        if (cancellationSignal != null) {
            try {
                try {
                    cancellationSignal.throwIfCanceled();
                    iCancellationSignalCreateCancellationSignal = this.mContentProvider.createCancellationSignal();
                    cancellationSignal.setRemote(iCancellationSignalCreateCancellationSignal);
                } catch (DeadObjectException e) {
                    if (!this.mStable) {
                        this.mContentResolver.unstableProviderDied(this.mContentProvider);
                    }
                    throw e;
                }
            } catch (Throwable th) {
                afterRemote();
                throw th;
            }
        }
        AssetFileDescriptor assetFileDescriptorOpenAssetFile = this.mContentProvider.openAssetFile(this.mPackageName, uri, str, iCancellationSignalCreateCancellationSignal);
        afterRemote();
        return assetFileDescriptorOpenAssetFile;
    }

    public final AssetFileDescriptor openTypedAssetFileDescriptor(Uri uri, String str, Bundle bundle) throws RemoteException, FileNotFoundException {
        return openTypedAssetFileDescriptor(uri, str, bundle, null);
    }

    public final AssetFileDescriptor openTypedAssetFileDescriptor(Uri uri, String str, Bundle bundle, CancellationSignal cancellationSignal) throws RemoteException, FileNotFoundException {
        Preconditions.checkNotNull(uri, "uri");
        Preconditions.checkNotNull(str, "mimeType");
        beforeRemote();
        ICancellationSignal iCancellationSignalCreateCancellationSignal = null;
        if (cancellationSignal != null) {
            try {
                try {
                    cancellationSignal.throwIfCanceled();
                    iCancellationSignalCreateCancellationSignal = this.mContentProvider.createCancellationSignal();
                    cancellationSignal.setRemote(iCancellationSignalCreateCancellationSignal);
                } catch (DeadObjectException e) {
                    if (!this.mStable) {
                        this.mContentResolver.unstableProviderDied(this.mContentProvider);
                    }
                    throw e;
                }
            } catch (Throwable th) {
                afterRemote();
                throw th;
            }
        }
        AssetFileDescriptor assetFileDescriptorOpenTypedAssetFile = this.mContentProvider.openTypedAssetFile(this.mPackageName, uri, str, bundle, iCancellationSignalCreateCancellationSignal);
        afterRemote();
        return assetFileDescriptorOpenTypedAssetFile;
    }

    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> arrayList) throws RemoteException, OperationApplicationException {
        Preconditions.checkNotNull(arrayList, "operations");
        beforeRemote();
        try {
            try {
                return this.mContentProvider.applyBatch(this.mPackageName, arrayList);
            } catch (DeadObjectException e) {
                if (!this.mStable) {
                    this.mContentResolver.unstableProviderDied(this.mContentProvider);
                }
                throw e;
            }
        } finally {
            afterRemote();
        }
    }

    public Bundle call(String str, String str2, Bundle bundle) throws RemoteException {
        Preconditions.checkNotNull(str, CalendarContract.RemindersColumns.METHOD);
        beforeRemote();
        try {
            try {
                return this.mContentProvider.call(this.mPackageName, str, str2, bundle);
            } catch (DeadObjectException e) {
                if (!this.mStable) {
                    this.mContentResolver.unstableProviderDied(this.mContentProvider);
                }
                throw e;
            }
        } finally {
            afterRemote();
        }
    }

    @Override
    public void close() {
        closeInternal();
    }

    @Deprecated
    public boolean release() {
        return closeInternal();
    }

    private boolean closeInternal() {
        this.mCloseGuard.close();
        if (!this.mClosed.compareAndSet(false, true)) {
            return false;
        }
        setDetectNotResponding(0L);
        if (this.mStable) {
            return this.mContentResolver.releaseProvider(this.mContentProvider);
        }
        return this.mContentResolver.releaseUnstableProvider(this.mContentProvider);
    }

    protected void finalize() throws Throwable {
        try {
            if (this.mCloseGuard != null) {
                this.mCloseGuard.warnIfOpen();
            }
            close();
        } finally {
            super.finalize();
        }
    }

    public ContentProvider getLocalContentProvider() {
        return ContentProvider.coerceToLocalContentProvider(this.mContentProvider);
    }

    public static void releaseQuietly(ContentProviderClient contentProviderClient) {
        if (contentProviderClient != null) {
            try {
                contentProviderClient.release();
            } catch (Exception e) {
            }
        }
    }

    private class NotRespondingRunnable implements Runnable {
        private NotRespondingRunnable() {
        }

        @Override
        public void run() {
            Log.w(ContentProviderClient.TAG, "Detected provider not responding: " + ContentProviderClient.this.mContentProvider);
            ContentProviderClient.this.mContentResolver.appNotRespondingViaProvider(ContentProviderClient.this.mContentProvider);
        }
    }

    private final class CursorWrapperInner extends CrossProcessCursorWrapper {
        private final CloseGuard mCloseGuard;

        CursorWrapperInner(Cursor cursor) {
            super(cursor);
            this.mCloseGuard = CloseGuard.get();
            this.mCloseGuard.open("close");
        }

        @Override
        public void close() {
            this.mCloseGuard.close();
            super.close();
        }

        protected void finalize() throws Throwable {
            try {
                if (this.mCloseGuard != null) {
                    this.mCloseGuard.warnIfOpen();
                }
                close();
            } finally {
                super.finalize();
            }
        }
    }
}
