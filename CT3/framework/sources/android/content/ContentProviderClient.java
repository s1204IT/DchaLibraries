package android.content;

import android.app.ContextImpl;
import android.content.res.AssetFileDescriptor;
import android.database.CrossProcessCursorWrapper;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.ICancellationSignal;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.Log;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.Preconditions;
import com.mediatek.aee.ExceptionLog;
import dalvik.system.CloseGuard;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
    private final Throwable mStackTrace;
    private static final boolean PROVIDER_LEAK_DETECT = Log.isLoggable(ContextImpl.ApplicationContentResolver.QUERY_TAG, 2);
    private static final boolean IS_ENG_BUILD = SystemProperties.get("ro.build.type").equals("eng");
    private final AtomicBoolean mClosed = new AtomicBoolean();
    private final CloseGuard mCloseGuard = CloseGuard.get();

    public ContentProviderClient(ContentResolver contentResolver, IContentProvider contentProvider, boolean stable) {
        this.mContentResolver = contentResolver;
        this.mContentProvider = contentProvider;
        this.mPackageName = contentResolver.mPackageName;
        this.mStable = stable;
        this.mCloseGuard.open("close");
        if (IS_ENG_BUILD) {
            this.mStackTrace = new RuntimeException("Ensure that resources ContentProviderClient are closed after use").fillInStackTrace();
        } else {
            this.mStackTrace = null;
        }
    }

    public void setDetectNotResponding(long timeoutMillis) {
        synchronized (ContentProviderClient.class) {
            this.mAnrTimeout = timeoutMillis;
            if (timeoutMillis > 0) {
                if (this.mAnrRunnable == null) {
                    this.mAnrRunnable = new NotRespondingRunnable(this, null);
                }
                if (sAnrHandler == null) {
                    sAnrHandler = new Handler(Looper.getMainLooper(), null, true);
                }
            } else {
                this.mAnrRunnable = null;
            }
        }
    }

    private void beforeRemote() {
        if (this.mAnrRunnable == null) {
            return;
        }
        sAnrHandler.postDelayed(this.mAnrRunnable, this.mAnrTimeout);
    }

    private void afterRemote() {
        if (this.mAnrRunnable == null) {
            return;
        }
        sAnrHandler.removeCallbacks(this.mAnrRunnable);
    }

    public Cursor query(Uri url, String[] projection, String selection, String[] selectionArgs, String sortOrder) throws RemoteException {
        return query(url, projection, selection, selectionArgs, sortOrder, null);
    }

    public Cursor query(Uri url, String[] projection, String selection, String[] selectionArgs, String sortOrder, CancellationSignal cancellationSignal) throws RemoteException {
        Preconditions.checkNotNull(url, "url");
        beforeRemote();
        ICancellationSignal remoteCancellationSignal = null;
        try {
            if (cancellationSignal != null) {
                try {
                    cancellationSignal.throwIfCanceled();
                    remoteCancellationSignal = this.mContentProvider.createCancellationSignal();
                    cancellationSignal.setRemote(remoteCancellationSignal);
                } catch (DeadObjectException e) {
                    if (!this.mStable) {
                        this.mContentResolver.unstableProviderDied(this.mContentProvider);
                    }
                    throw e;
                }
            }
            Cursor cursor = this.mContentProvider.query(this.mPackageName, url, projection, selection, selectionArgs, sortOrder, remoteCancellationSignal);
            if (cursor == null) {
                return null;
            }
            return "com.google.android.gms".equals(this.mPackageName) ? cursor : new CursorWrapperInner(cursor);
        } finally {
            afterRemote();
        }
    }

    public String getType(Uri url) throws RemoteException {
        Preconditions.checkNotNull(url, "url");
        beforeRemote();
        try {
            try {
                return this.mContentProvider.getType(url);
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

    public String[] getStreamTypes(Uri url, String mimeTypeFilter) throws RemoteException {
        Preconditions.checkNotNull(url, "url");
        Preconditions.checkNotNull(mimeTypeFilter, "mimeTypeFilter");
        beforeRemote();
        try {
            try {
                return this.mContentProvider.getStreamTypes(url, mimeTypeFilter);
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

    public final Uri canonicalize(Uri url) throws RemoteException {
        Preconditions.checkNotNull(url, "url");
        beforeRemote();
        try {
            try {
                return this.mContentProvider.canonicalize(this.mPackageName, url);
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

    public final Uri uncanonicalize(Uri url) throws RemoteException {
        Preconditions.checkNotNull(url, "url");
        beforeRemote();
        try {
            try {
                return this.mContentProvider.uncanonicalize(this.mPackageName, url);
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

    public Uri insert(Uri url, ContentValues initialValues) throws RemoteException {
        Preconditions.checkNotNull(url, "url");
        beforeRemote();
        try {
            try {
                return this.mContentProvider.insert(this.mPackageName, url, initialValues);
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

    public int bulkInsert(Uri url, ContentValues[] initialValues) throws RemoteException {
        Preconditions.checkNotNull(url, "url");
        Preconditions.checkNotNull(initialValues, "initialValues");
        beforeRemote();
        try {
            try {
                return this.mContentProvider.bulkInsert(this.mPackageName, url, initialValues);
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

    public int delete(Uri url, String selection, String[] selectionArgs) throws RemoteException {
        Preconditions.checkNotNull(url, "url");
        beforeRemote();
        try {
            try {
                return this.mContentProvider.delete(this.mPackageName, url, selection, selectionArgs);
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

    public int update(Uri url, ContentValues values, String selection, String[] selectionArgs) throws RemoteException {
        Preconditions.checkNotNull(url, "url");
        beforeRemote();
        try {
            try {
                return this.mContentProvider.update(this.mPackageName, url, values, selection, selectionArgs);
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

    public ParcelFileDescriptor openFile(Uri url, String mode) throws RemoteException, FileNotFoundException {
        return openFile(url, mode, null);
    }

    public ParcelFileDescriptor openFile(Uri url, String mode, CancellationSignal signal) throws RemoteException, FileNotFoundException {
        Preconditions.checkNotNull(url, "url");
        Preconditions.checkNotNull(mode, "mode");
        beforeRemote();
        ICancellationSignal remoteSignal = null;
        if (signal != null) {
            try {
                try {
                    signal.throwIfCanceled();
                    remoteSignal = this.mContentProvider.createCancellationSignal();
                    signal.setRemote(remoteSignal);
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
        return this.mContentProvider.openFile(this.mPackageName, url, mode, remoteSignal, null);
    }

    public AssetFileDescriptor openAssetFile(Uri url, String mode) throws RemoteException, FileNotFoundException {
        return openAssetFile(url, mode, null);
    }

    public AssetFileDescriptor openAssetFile(Uri url, String mode, CancellationSignal signal) throws RemoteException, FileNotFoundException {
        Preconditions.checkNotNull(url, "url");
        Preconditions.checkNotNull(mode, "mode");
        beforeRemote();
        ICancellationSignal remoteSignal = null;
        if (signal != null) {
            try {
                try {
                    signal.throwIfCanceled();
                    remoteSignal = this.mContentProvider.createCancellationSignal();
                    signal.setRemote(remoteSignal);
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
        return this.mContentProvider.openAssetFile(this.mPackageName, url, mode, remoteSignal);
    }

    public final AssetFileDescriptor openTypedAssetFileDescriptor(Uri uri, String mimeType, Bundle opts) throws RemoteException, FileNotFoundException {
        return openTypedAssetFileDescriptor(uri, mimeType, opts, null);
    }

    public final AssetFileDescriptor openTypedAssetFileDescriptor(Uri uri, String mimeType, Bundle opts, CancellationSignal signal) throws RemoteException, FileNotFoundException {
        Preconditions.checkNotNull(uri, "uri");
        Preconditions.checkNotNull(mimeType, "mimeType");
        beforeRemote();
        ICancellationSignal remoteSignal = null;
        if (signal != null) {
            try {
                try {
                    signal.throwIfCanceled();
                    remoteSignal = this.mContentProvider.createCancellationSignal();
                    signal.setRemote(remoteSignal);
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
        return this.mContentProvider.openTypedAssetFile(this.mPackageName, uri, mimeType, opts, remoteSignal);
    }

    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations) throws RemoteException, OperationApplicationException {
        Preconditions.checkNotNull(operations, "operations");
        beforeRemote();
        try {
            try {
                return this.mContentProvider.applyBatch(this.mPackageName, operations);
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

    public Bundle call(String method, String arg, Bundle extras) throws RemoteException {
        Preconditions.checkNotNull(method, "method");
        beforeRemote();
        try {
            try {
                return this.mContentProvider.call(this.mPackageName, method, arg, extras);
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
        if (this.mStable) {
            return this.mContentResolver.releaseProvider(this.mContentProvider);
        }
        return this.mContentResolver.releaseUnstableProvider(this.mContentProvider);
    }

    protected void finalize() throws Throwable {
        try {
            this.mCloseGuard.warnIfOpen();
            close();
            super.finalize();
            if (!IS_ENG_BUILD) {
                return;
            }
            if (this.mStackTrace != null) {
                Log.v(TAG, "Ensure that resources ContentProviderClient are closed after use.", this.mStackTrace);
            }
            if (!PROVIDER_LEAK_DETECT || !checkAeeWarningList()) {
                return;
            }
            ExceptionLog exceptionLog = null;
            try {
                if (SystemProperties.get("ro.have_aee_feature").equals(WifiEnterpriseConfig.ENGINE_ENABLE)) {
                    ExceptionLog exceptionLog2 = new ExceptionLog();
                    exceptionLog = exceptionLog2;
                }
                if (exceptionLog == null) {
                    return;
                }
                exceptionLog.systemreport((byte) 0, "ContentProviderClient.java", "Ensure that resources ContentProviderClient are closed after use.", "/data/leak/traces.txt");
            } catch (Exception e) {
            }
        } catch (Throwable th) {
            super.finalize();
            throw th;
        }
    }

    private boolean checkAeeWarningList() throws Throwable {
        int uid = Process.myUid();
        InputStream inStream = null;
        try {
            InputStream inStream2 = new FileInputStream("/data/system/resmon-uid.txt");
            if (inStream2 != null) {
                try {
                    InputStreamReader inputReader = new InputStreamReader(inStream2);
                    BufferedReader buffReader = new BufferedReader(inputReader);
                    for (String line = buffReader.readLine(); line != null; line = buffReader.readLine()) {
                        if (uid == Integer.valueOf(line).intValue()) {
                            if (inStream2 != null) {
                                try {
                                    inStream2.close();
                                } catch (Exception e) {
                                }
                            }
                            return true;
                        }
                    }
                } catch (Exception e2) {
                    inStream = inStream2;
                    if (inStream != null) {
                        try {
                            inStream.close();
                        } catch (Exception e3) {
                        }
                    }
                    return false;
                } catch (Throwable th) {
                    th = th;
                    inStream = inStream2;
                    if (inStream != null) {
                        try {
                            inStream.close();
                        } catch (Exception e4) {
                        }
                    }
                    throw th;
                }
            }
            if (inStream2 != null) {
                try {
                    inStream2.close();
                } catch (Exception e5) {
                }
            }
            return false;
        } catch (Exception e6) {
        } catch (Throwable th2) {
            th = th2;
        }
    }

    public ContentProvider getLocalContentProvider() {
        return ContentProvider.coerceToLocalContentProvider(this.mContentProvider);
    }

    public static void releaseQuietly(ContentProviderClient client) {
        if (client == null) {
            return;
        }
        try {
            client.release();
        } catch (Exception e) {
        }
    }

    private class NotRespondingRunnable implements Runnable {
        NotRespondingRunnable(ContentProviderClient this$0, NotRespondingRunnable notRespondingRunnable) {
            this();
        }

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
                this.mCloseGuard.warnIfOpen();
                close();
            } finally {
                super.finalize();
            }
        }
    }
}
