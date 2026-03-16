package android.content;

import android.accounts.Account;
import android.app.ActivityManagerNative;
import android.app.ActivityThread;
import android.app.backup.FullBackup;
import android.content.IContentService;
import android.content.ISyncStatusObserver;
import android.content.SyncRequest;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.CrossProcessCursorWrapper;
import android.database.Cursor;
import android.database.IContentObserver;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.DeadObjectException;
import android.os.IBinder;
import android.os.ICancellationSignal;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import dalvik.system.CloseGuard;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public abstract class ContentResolver {
    public static final String ANY_CURSOR_ITEM_TYPE = "vnd.android.cursor.item/*";
    public static final String CONTENT_SERVICE_NAME = "content";
    public static final String CURSOR_DIR_BASE_TYPE = "vnd.android.cursor.dir";
    public static final String CURSOR_ITEM_BASE_TYPE = "vnd.android.cursor.item";
    private static final boolean ENABLE_CONTENT_SAMPLE = false;
    public static final String EXTRA_SIZE = "android.content.extra.SIZE";
    public static final String SCHEME_ANDROID_RESOURCE = "android.resource";
    public static final String SCHEME_CONTENT = "content";
    public static final String SCHEME_FILE = "file";
    private static final int SLOW_THRESHOLD_MILLIS = 500;
    public static final int SYNC_ERROR_AUTHENTICATION = 2;
    public static final int SYNC_ERROR_CONFLICT = 5;
    public static final int SYNC_ERROR_INTERNAL = 8;
    public static final int SYNC_ERROR_IO = 3;
    public static final int SYNC_ERROR_PARSE = 4;
    public static final int SYNC_ERROR_SYNC_ALREADY_IN_PROGRESS = 1;
    public static final int SYNC_ERROR_TOO_MANY_DELETIONS = 6;
    public static final int SYNC_ERROR_TOO_MANY_RETRIES = 7;

    @Deprecated
    public static final String SYNC_EXTRAS_ACCOUNT = "account";
    public static final String SYNC_EXTRAS_DISALLOW_METERED = "allow_metered";
    public static final String SYNC_EXTRAS_DISCARD_LOCAL_DELETIONS = "discard_deletions";
    public static final String SYNC_EXTRAS_DO_NOT_RETRY = "do_not_retry";
    public static final String SYNC_EXTRAS_EXPECTED_DOWNLOAD = "expected_download";
    public static final String SYNC_EXTRAS_EXPECTED_UPLOAD = "expected_upload";
    public static final String SYNC_EXTRAS_EXPEDITED = "expedited";

    @Deprecated
    public static final String SYNC_EXTRAS_FORCE = "force";
    public static final String SYNC_EXTRAS_IGNORE_BACKOFF = "ignore_backoff";
    public static final String SYNC_EXTRAS_IGNORE_SETTINGS = "ignore_settings";
    public static final String SYNC_EXTRAS_INITIALIZE = "initialize";
    public static final String SYNC_EXTRAS_MANUAL = "force";
    public static final String SYNC_EXTRAS_OVERRIDE_TOO_MANY_DELETIONS = "deletions_override";
    public static final String SYNC_EXTRAS_PRIORITY = "sync_priority";
    public static final String SYNC_EXTRAS_UPLOAD = "upload";
    public static final int SYNC_OBSERVER_TYPE_ACTIVE = 4;
    public static final int SYNC_OBSERVER_TYPE_ALL = Integer.MAX_VALUE;
    public static final int SYNC_OBSERVER_TYPE_PENDING = 2;
    public static final int SYNC_OBSERVER_TYPE_SETTINGS = 1;
    public static final int SYNC_OBSERVER_TYPE_STATUS = 8;
    private static final String TAG = "ContentResolver";
    private static IContentService sContentService;
    private final Context mContext;
    final String mPackageName;
    private final Random mRandom = new Random();
    public static final Intent ACTION_SYNC_CONN_STATUS_CHANGED = new Intent("com.android.sync.SYNC_CONN_STATUS_CHANGED");
    private static final String[] SYNC_ERROR_NAMES = {"already-in-progress", "authentication-error", "io-error", "parse-error", "conflict", "too-many-deletions", "too-many-retries", "internal-error"};

    protected abstract IContentProvider acquireProvider(Context context, String str);

    protected abstract IContentProvider acquireUnstableProvider(Context context, String str);

    public abstract boolean releaseProvider(IContentProvider iContentProvider);

    public abstract boolean releaseUnstableProvider(IContentProvider iContentProvider);

    public abstract void unstableProviderDied(IContentProvider iContentProvider);

    public static String syncErrorToString(int error) {
        return (error < 1 || error > SYNC_ERROR_NAMES.length) ? String.valueOf(error) : SYNC_ERROR_NAMES[error - 1];
    }

    public static int syncErrorStringToInt(String error) {
        int n = SYNC_ERROR_NAMES.length;
        for (int i = 0; i < n; i++) {
            if (SYNC_ERROR_NAMES[i].equals(error)) {
                return i + 1;
            }
        }
        if (error != null) {
            try {
                return Integer.parseInt(error);
            } catch (NumberFormatException e) {
                Log.d(TAG, "error parsing sync error: " + error);
            }
        }
        return 0;
    }

    public ContentResolver(Context context) {
        this.mContext = context == null ? ActivityThread.currentApplication() : context;
        this.mPackageName = this.mContext.getOpPackageName();
    }

    protected IContentProvider acquireExistingProvider(Context c, String name) {
        return acquireProvider(c, name);
    }

    public void appNotRespondingViaProvider(IContentProvider icp) {
        throw new UnsupportedOperationException("appNotRespondingViaProvider");
    }

    public final String getType(Uri url) {
        String type = null;
        IContentProvider provider = acquireExistingProvider(url);
        try {
            if (provider != null) {
                try {
                    type = provider.getType(url);
                    releaseProvider(provider);
                } catch (RemoteException e) {
                    releaseProvider(provider);
                    return null;
                } catch (Exception e2) {
                    Log.w(TAG, "Failed to get type for: " + url + " (" + e2.getMessage() + ")");
                    releaseProvider(provider);
                }
                return type;
            }
            if (!"content".equals(url.getScheme())) {
                return null;
            }
            try {
                return ActivityManagerNative.getDefault().getProviderMimeType(ContentProvider.getUriWithoutUserId(url), resolveUserId(url));
            } catch (RemoteException e3) {
                return null;
            } catch (Exception e4) {
                Log.w(TAG, "Failed to get type for: " + url + " (" + e4.getMessage() + ")");
                return null;
            }
        } catch (Throwable th) {
            releaseProvider(provider);
            throw th;
        }
    }

    public String[] getStreamTypes(Uri url, String mimeTypeFilter) {
        String[] streamTypes = null;
        IContentProvider provider = acquireProvider(url);
        if (provider != null) {
            try {
                streamTypes = provider.getStreamTypes(url, mimeTypeFilter);
            } catch (RemoteException e) {
            } finally {
                releaseProvider(provider);
            }
        }
        return streamTypes;
    }

    public final Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return query(uri, projection, selection, selectionArgs, sortOrder, null);
    }

    public final Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder, CancellationSignal cancellationSignal) {
        IContentProvider unstableProvider = acquireUnstableProvider(uri);
        if (unstableProvider == null) {
            return null;
        }
        IContentProvider stableProvider = null;
        Cursor qCursor = null;
        try {
            long startTime = SystemClock.uptimeMillis();
            ICancellationSignal remoteCancellationSignal = null;
            if (cancellationSignal != null) {
                cancellationSignal.throwIfCanceled();
                remoteCancellationSignal = unstableProvider.createCancellationSignal();
                cancellationSignal.setRemote(remoteCancellationSignal);
            }
            try {
                qCursor = unstableProvider.query(this.mPackageName, uri, projection, selection, selectionArgs, sortOrder, remoteCancellationSignal);
            } catch (DeadObjectException e) {
                unstableProviderDied(unstableProvider);
                stableProvider = acquireProvider(uri);
                if (stableProvider == null) {
                    if (0 != 0) {
                        qCursor.close();
                    }
                    if (cancellationSignal != null) {
                        cancellationSignal.setRemote(null);
                    }
                    if (unstableProvider != null) {
                        releaseUnstableProvider(unstableProvider);
                    }
                    if (stableProvider == null) {
                        return null;
                    }
                    releaseProvider(stableProvider);
                    return null;
                }
                qCursor = stableProvider.query(this.mPackageName, uri, projection, selection, selectionArgs, sortOrder, remoteCancellationSignal);
            }
            if (qCursor == null) {
                if (qCursor != null) {
                    qCursor.close();
                }
                if (cancellationSignal != null) {
                    cancellationSignal.setRemote(null);
                }
                if (unstableProvider != null) {
                    releaseUnstableProvider(unstableProvider);
                }
                if (stableProvider == null) {
                    return null;
                }
                releaseProvider(stableProvider);
                return null;
            }
            qCursor.getCount();
            long durationMillis = SystemClock.uptimeMillis() - startTime;
            maybeLogQueryToEventLog(durationMillis, uri, projection, selection, sortOrder);
            CursorWrapperInner cursorWrapperInner = new CursorWrapperInner(qCursor, stableProvider != null ? stableProvider : acquireProvider(uri));
            Cursor qCursor2 = null;
            if (0 != 0) {
                qCursor2.close();
            }
            if (cancellationSignal != null) {
                cancellationSignal.setRemote(null);
            }
            if (unstableProvider != null) {
                releaseUnstableProvider(unstableProvider);
            }
            if (0 == 0) {
                return cursorWrapperInner;
            }
            releaseProvider(null);
            return cursorWrapperInner;
        } catch (RemoteException e2) {
            if (qCursor != null) {
                qCursor.close();
            }
            if (cancellationSignal != null) {
                cancellationSignal.setRemote(null);
            }
            if (unstableProvider != null) {
                releaseUnstableProvider(unstableProvider);
            }
            if (stableProvider == null) {
                return null;
            }
            releaseProvider(stableProvider);
            return null;
        } catch (Throwable th) {
            if (qCursor != null) {
                qCursor.close();
            }
            if (cancellationSignal != null) {
                cancellationSignal.setRemote(null);
            }
            if (unstableProvider != null) {
                releaseUnstableProvider(unstableProvider);
            }
            if (stableProvider != null) {
                releaseProvider(stableProvider);
            }
            throw th;
        }
    }

    public final Uri canonicalize(Uri url) {
        Uri uriCanonicalize = null;
        IContentProvider provider = acquireProvider(url);
        if (provider != null) {
            try {
                uriCanonicalize = provider.canonicalize(this.mPackageName, url);
            } catch (RemoteException e) {
            } finally {
                releaseProvider(provider);
            }
        }
        return uriCanonicalize;
    }

    public final Uri uncanonicalize(Uri url) {
        Uri uriUncanonicalize = null;
        IContentProvider provider = acquireProvider(url);
        if (provider != null) {
            try {
                uriUncanonicalize = provider.uncanonicalize(this.mPackageName, url);
            } catch (RemoteException e) {
            } finally {
                releaseProvider(provider);
            }
        }
        return uriUncanonicalize;
    }

    public final InputStream openInputStream(Uri uri) throws FileNotFoundException {
        FileInputStream fileInputStreamCreateInputStream = null;
        String scheme = uri.getScheme();
        if (SCHEME_ANDROID_RESOURCE.equals(scheme)) {
            OpenResourceIdResult r = getResourceId(uri);
            try {
                return r.r.openRawResource(r.id);
            } catch (Resources.NotFoundException e) {
                throw new FileNotFoundException("Resource does not exist: " + uri);
            }
        }
        if (SCHEME_FILE.equals(scheme)) {
            return new FileInputStream(uri.getPath());
        }
        AssetFileDescriptor fd = openAssetFileDescriptor(uri, FullBackup.ROOT_TREE_TOKEN, null);
        if (fd != null) {
            try {
                fileInputStreamCreateInputStream = fd.createInputStream();
            } catch (IOException e2) {
                throw new FileNotFoundException("Unable to create stream");
            }
        }
        return fileInputStreamCreateInputStream;
    }

    public final OutputStream openOutputStream(Uri uri) throws FileNotFoundException {
        return openOutputStream(uri, "w");
    }

    public final OutputStream openOutputStream(Uri uri, String mode) throws FileNotFoundException {
        AssetFileDescriptor fd = openAssetFileDescriptor(uri, mode, null);
        if (fd == null) {
            return null;
        }
        try {
            return fd.createOutputStream();
        } catch (IOException e) {
            throw new FileNotFoundException("Unable to create stream");
        }
    }

    public final ParcelFileDescriptor openFileDescriptor(Uri uri, String mode) throws FileNotFoundException {
        return openFileDescriptor(uri, mode, null);
    }

    public final ParcelFileDescriptor openFileDescriptor(Uri uri, String mode, CancellationSignal cancellationSignal) throws FileNotFoundException {
        AssetFileDescriptor afd = openAssetFileDescriptor(uri, mode, cancellationSignal);
        if (afd == null) {
            return null;
        }
        if (afd.getDeclaredLength() < 0) {
            return afd.getParcelFileDescriptor();
        }
        try {
            afd.close();
        } catch (IOException e) {
        }
        throw new FileNotFoundException("Not a whole file");
    }

    public final AssetFileDescriptor openAssetFileDescriptor(Uri uri, String mode) throws FileNotFoundException {
        return openAssetFileDescriptor(uri, mode, null);
    }

    public final AssetFileDescriptor openAssetFileDescriptor(Uri uri, String mode, CancellationSignal cancellationSignal) throws FileNotFoundException {
        AssetFileDescriptor fd;
        String scheme = uri.getScheme();
        if (SCHEME_ANDROID_RESOURCE.equals(scheme)) {
            if (!FullBackup.ROOT_TREE_TOKEN.equals(mode)) {
                throw new FileNotFoundException("Can't write resources: " + uri);
            }
            OpenResourceIdResult r = getResourceId(uri);
            try {
                return r.r.openRawResourceFd(r.id);
            } catch (Resources.NotFoundException e) {
                throw new FileNotFoundException("Resource does not exist: " + uri);
            }
        }
        if (SCHEME_FILE.equals(scheme)) {
            ParcelFileDescriptor pfd = ParcelFileDescriptor.open(new File(uri.getPath()), ParcelFileDescriptor.parseMode(mode));
            return new AssetFileDescriptor(pfd, 0L, -1L);
        }
        if (FullBackup.ROOT_TREE_TOKEN.equals(mode)) {
            return openTypedAssetFileDescriptor(uri, "*/*", null, cancellationSignal);
        }
        IContentProvider unstableProvider = acquireUnstableProvider(uri);
        if (unstableProvider == null) {
            throw new FileNotFoundException("No content provider: " + uri);
        }
        IContentProvider stableProvider = null;
        ICancellationSignal remoteCancellationSignal = null;
        try {
            if (cancellationSignal != null) {
                try {
                    cancellationSignal.throwIfCanceled();
                    remoteCancellationSignal = unstableProvider.createCancellationSignal();
                    cancellationSignal.setRemote(remoteCancellationSignal);
                } catch (RemoteException e2) {
                    throw new FileNotFoundException("Failed opening content provider: " + uri);
                } catch (FileNotFoundException e3) {
                    throw e3;
                }
            }
            try {
                fd = unstableProvider.openAssetFile(this.mPackageName, uri, mode, remoteCancellationSignal);
                if (fd == null) {
                }
            } catch (DeadObjectException e4) {
                unstableProviderDied(unstableProvider);
                stableProvider = acquireProvider(uri);
                if (stableProvider == null) {
                    throw new FileNotFoundException("No content provider: " + uri);
                }
                fd = stableProvider.openAssetFile(this.mPackageName, uri, mode, remoteCancellationSignal);
                if (fd == null) {
                    if (cancellationSignal != null) {
                        cancellationSignal.setRemote(null);
                    }
                    if (stableProvider != null) {
                        releaseProvider(stableProvider);
                    }
                    if (unstableProvider == null) {
                        return null;
                    }
                    releaseUnstableProvider(unstableProvider);
                    return null;
                }
            }
            if (stableProvider == null) {
                stableProvider = acquireProvider(uri);
            }
            releaseUnstableProvider(unstableProvider);
            ParcelFileDescriptor pfd2 = new ParcelFileDescriptorInner(fd.getParcelFileDescriptor(), stableProvider);
            AssetFileDescriptor assetFileDescriptor = new AssetFileDescriptor(pfd2, fd.getStartOffset(), fd.getDeclaredLength());
            if (cancellationSignal != null) {
                cancellationSignal.setRemote(null);
            }
            if (0 != 0) {
                releaseProvider(null);
            }
            if (unstableProvider == null) {
                return assetFileDescriptor;
            }
            releaseUnstableProvider(unstableProvider);
            return assetFileDescriptor;
        } finally {
            if (cancellationSignal != null) {
                cancellationSignal.setRemote(null);
            }
            if (0 != 0) {
                releaseProvider(null);
            }
            if (unstableProvider != null) {
                releaseUnstableProvider(unstableProvider);
            }
        }
    }

    public final AssetFileDescriptor openTypedAssetFileDescriptor(Uri uri, String mimeType, Bundle opts) throws FileNotFoundException {
        return openTypedAssetFileDescriptor(uri, mimeType, opts, null);
    }

    public final AssetFileDescriptor openTypedAssetFileDescriptor(Uri uri, String mimeType, Bundle opts, CancellationSignal cancellationSignal) throws FileNotFoundException {
        AssetFileDescriptor fd;
        AssetFileDescriptor assetFileDescriptor;
        IContentProvider unstableProvider = acquireUnstableProvider(uri);
        if (unstableProvider == null) {
            throw new FileNotFoundException("No content provider: " + uri);
        }
        IContentProvider stableProvider = null;
        ICancellationSignal remoteCancellationSignal = null;
        try {
            if (cancellationSignal != null) {
                try {
                    cancellationSignal.throwIfCanceled();
                    remoteCancellationSignal = unstableProvider.createCancellationSignal();
                    cancellationSignal.setRemote(remoteCancellationSignal);
                } catch (RemoteException e) {
                    throw new FileNotFoundException("Failed opening content provider: " + uri);
                } catch (FileNotFoundException e2) {
                    throw e2;
                }
            }
            try {
                fd = unstableProvider.openTypedAssetFile(this.mPackageName, uri, mimeType, opts, remoteCancellationSignal);
            } catch (DeadObjectException e3) {
                unstableProviderDied(unstableProvider);
                stableProvider = acquireProvider(uri);
                if (stableProvider == null) {
                    throw new FileNotFoundException("No content provider: " + uri);
                }
                fd = stableProvider.openTypedAssetFile(this.mPackageName, uri, mimeType, opts, remoteCancellationSignal);
                if (fd == null) {
                    assetFileDescriptor = null;
                    if (cancellationSignal != null) {
                        cancellationSignal.setRemote(null);
                    }
                    if (stableProvider != null) {
                        releaseProvider(stableProvider);
                    }
                    if (unstableProvider != null) {
                        releaseUnstableProvider(unstableProvider);
                    }
                }
            }
            if (fd == null) {
                assetFileDescriptor = null;
            } else {
                if (stableProvider == null) {
                    stableProvider = acquireProvider(uri);
                }
                releaseUnstableProvider(unstableProvider);
                ParcelFileDescriptor pfd = new ParcelFileDescriptorInner(fd.getParcelFileDescriptor(), stableProvider);
                assetFileDescriptor = new AssetFileDescriptor(pfd, fd.getStartOffset(), fd.getDeclaredLength());
                if (cancellationSignal != null) {
                    cancellationSignal.setRemote(null);
                }
                if (0 != 0) {
                    releaseProvider(null);
                }
                if (unstableProvider != null) {
                    releaseUnstableProvider(unstableProvider);
                }
            }
            return assetFileDescriptor;
        } finally {
            if (cancellationSignal != null) {
                cancellationSignal.setRemote(null);
            }
            if (0 != 0) {
                releaseProvider(null);
            }
            if (unstableProvider != null) {
                releaseUnstableProvider(unstableProvider);
            }
        }
    }

    public class OpenResourceIdResult {
        public int id;
        public Resources r;

        public OpenResourceIdResult() {
        }
    }

    public OpenResourceIdResult getResourceId(Uri uri) throws FileNotFoundException {
        int id;
        String authority = uri.getAuthority();
        if (TextUtils.isEmpty(authority)) {
            throw new FileNotFoundException("No authority: " + uri);
        }
        try {
            Resources r = this.mContext.getPackageManager().getResourcesForApplication(authority);
            List<String> path = uri.getPathSegments();
            if (path == null) {
                throw new FileNotFoundException("No path: " + uri);
            }
            int len = path.size();
            if (len == 1) {
                try {
                    id = Integer.parseInt(path.get(0));
                } catch (NumberFormatException e) {
                    throw new FileNotFoundException("Single path segment is not a resource ID: " + uri);
                }
            } else if (len == 2) {
                id = r.getIdentifier(path.get(1), path.get(0), authority);
            } else {
                throw new FileNotFoundException("More than two path segments: " + uri);
            }
            if (id == 0) {
                throw new FileNotFoundException("No resource found for: " + uri);
            }
            OpenResourceIdResult res = new OpenResourceIdResult();
            res.r = r;
            res.id = id;
            return res;
        } catch (PackageManager.NameNotFoundException e2) {
            throw new FileNotFoundException("No package found for authority: " + uri);
        }
    }

    public final Uri insert(Uri url, ContentValues values) {
        IContentProvider provider = acquireProvider(url);
        if (provider == null) {
            throw new IllegalArgumentException("Unknown URL " + url);
        }
        try {
            long startTime = SystemClock.uptimeMillis();
            Uri createdRow = provider.insert(this.mPackageName, url, values);
            long durationMillis = SystemClock.uptimeMillis() - startTime;
            maybeLogUpdateToEventLog(durationMillis, url, "insert", null);
            return createdRow;
        } catch (RemoteException e) {
            return null;
        } finally {
            releaseProvider(provider);
        }
    }

    public ContentProviderResult[] applyBatch(String authority, ArrayList<ContentProviderOperation> operations) throws RemoteException, OperationApplicationException {
        ContentProviderClient provider = acquireContentProviderClient(authority);
        if (provider == null) {
            throw new IllegalArgumentException("Unknown authority " + authority);
        }
        try {
            return provider.applyBatch(operations);
        } finally {
            provider.release();
        }
    }

    public final int bulkInsert(Uri url, ContentValues[] values) {
        IContentProvider provider = acquireProvider(url);
        if (provider == null) {
            throw new IllegalArgumentException("Unknown URL " + url);
        }
        try {
            long startTime = SystemClock.uptimeMillis();
            int rowsCreated = provider.bulkInsert(this.mPackageName, url, values);
            long durationMillis = SystemClock.uptimeMillis() - startTime;
            maybeLogUpdateToEventLog(durationMillis, url, "bulkinsert", null);
            return rowsCreated;
        } catch (RemoteException e) {
            return 0;
        } finally {
            releaseProvider(provider);
        }
    }

    public final int delete(Uri url, String where, String[] selectionArgs) {
        IContentProvider provider = acquireProvider(url);
        if (provider == null) {
            throw new IllegalArgumentException("Unknown URL " + url);
        }
        try {
            long startTime = SystemClock.uptimeMillis();
            int rowsDeleted = provider.delete(this.mPackageName, url, where, selectionArgs);
            long durationMillis = SystemClock.uptimeMillis() - startTime;
            maybeLogUpdateToEventLog(durationMillis, url, "delete", where);
            return rowsDeleted;
        } catch (RemoteException e) {
            return -1;
        } finally {
            releaseProvider(provider);
        }
    }

    public final int update(Uri uri, ContentValues values, String where, String[] selectionArgs) {
        IContentProvider provider = acquireProvider(uri);
        if (provider == null) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        try {
            long startTime = SystemClock.uptimeMillis();
            int rowsUpdated = provider.update(this.mPackageName, uri, values, where, selectionArgs);
            long durationMillis = SystemClock.uptimeMillis() - startTime;
            maybeLogUpdateToEventLog(durationMillis, uri, ConnectivityManager.DHCP_EVENT_UPDATE, where);
            return rowsUpdated;
        } catch (RemoteException e) {
            return -1;
        } finally {
            releaseProvider(provider);
        }
    }

    public final Bundle call(Uri uri, String method, String arg, Bundle extras) {
        if (uri == null) {
            throw new NullPointerException("uri == null");
        }
        if (method == null) {
            throw new NullPointerException("method == null");
        }
        IContentProvider provider = acquireProvider(uri);
        if (provider == null) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        try {
            return provider.call(this.mPackageName, method, arg, extras);
        } catch (RemoteException e) {
            return null;
        } finally {
            releaseProvider(provider);
        }
    }

    public final IContentProvider acquireProvider(Uri uri) {
        String auth;
        if ("content".equals(uri.getScheme()) && (auth = uri.getAuthority()) != null) {
            return acquireProvider(this.mContext, auth);
        }
        return null;
    }

    public final IContentProvider acquireExistingProvider(Uri uri) {
        String auth;
        if ("content".equals(uri.getScheme()) && (auth = uri.getAuthority()) != null) {
            return acquireExistingProvider(this.mContext, auth);
        }
        return null;
    }

    public final IContentProvider acquireProvider(String name) {
        if (name == null) {
            return null;
        }
        return acquireProvider(this.mContext, name);
    }

    public final IContentProvider acquireUnstableProvider(Uri uri) {
        if (!"content".equals(uri.getScheme())) {
            return null;
        }
        String auth = uri.getAuthority();
        if (auth != null) {
            return acquireUnstableProvider(this.mContext, uri.getAuthority());
        }
        return null;
    }

    public final IContentProvider acquireUnstableProvider(String name) {
        if (name == null) {
            return null;
        }
        return acquireUnstableProvider(this.mContext, name);
    }

    public final ContentProviderClient acquireContentProviderClient(Uri uri) {
        IContentProvider provider = acquireProvider(uri);
        if (provider != null) {
            return new ContentProviderClient(this, provider, true);
        }
        return null;
    }

    public final ContentProviderClient acquireContentProviderClient(String name) {
        IContentProvider provider = acquireProvider(name);
        if (provider != null) {
            return new ContentProviderClient(this, provider, true);
        }
        return null;
    }

    public final ContentProviderClient acquireUnstableContentProviderClient(Uri uri) {
        IContentProvider provider = acquireUnstableProvider(uri);
        if (provider != null) {
            return new ContentProviderClient(this, provider, false);
        }
        return null;
    }

    public final ContentProviderClient acquireUnstableContentProviderClient(String name) {
        IContentProvider provider = acquireUnstableProvider(name);
        if (provider != null) {
            return new ContentProviderClient(this, provider, false);
        }
        return null;
    }

    public final void registerContentObserver(Uri uri, boolean notifyForDescendents, ContentObserver observer) {
        registerContentObserver(uri, notifyForDescendents, observer, UserHandle.myUserId());
    }

    public final void registerContentObserver(Uri uri, boolean notifyForDescendents, ContentObserver observer, int userHandle) {
        try {
            getContentService().registerContentObserver(uri, notifyForDescendents, observer.getContentObserver(), userHandle);
        } catch (RemoteException e) {
        }
    }

    public final void unregisterContentObserver(ContentObserver observer) {
        try {
            IContentObserver contentObserver = observer.releaseContentObserver();
            if (contentObserver != null) {
                getContentService().unregisterContentObserver(contentObserver);
            }
        } catch (RemoteException e) {
        }
    }

    public void notifyChange(Uri uri, ContentObserver observer) {
        notifyChange(uri, observer, true);
    }

    public void notifyChange(Uri uri, ContentObserver observer, boolean syncToNetwork) {
        notifyChange(uri, observer, syncToNetwork, UserHandle.myUserId());
    }

    public void notifyChange(Uri uri, ContentObserver observer, boolean syncToNetwork, int userHandle) {
        try {
            getContentService().notifyChange(uri, observer == null ? null : observer.getContentObserver(), observer != null && observer.deliverSelfNotifications(), syncToNetwork, userHandle);
        } catch (RemoteException e) {
        }
    }

    public void takePersistableUriPermission(Uri uri, int modeFlags) {
        try {
            ActivityManagerNative.getDefault().takePersistableUriPermission(ContentProvider.getUriWithoutUserId(uri), modeFlags, resolveUserId(uri));
        } catch (RemoteException e) {
        }
    }

    public void releasePersistableUriPermission(Uri uri, int modeFlags) {
        try {
            ActivityManagerNative.getDefault().releasePersistableUriPermission(ContentProvider.getUriWithoutUserId(uri), modeFlags, resolveUserId(uri));
        } catch (RemoteException e) {
        }
    }

    public List<UriPermission> getPersistedUriPermissions() {
        try {
            return ActivityManagerNative.getDefault().getPersistedUriPermissions(this.mPackageName, true).getList();
        } catch (RemoteException e) {
            throw new RuntimeException("Activity manager has died", e);
        }
    }

    public List<UriPermission> getOutgoingPersistedUriPermissions() {
        try {
            return ActivityManagerNative.getDefault().getPersistedUriPermissions(this.mPackageName, false).getList();
        } catch (RemoteException e) {
            throw new RuntimeException("Activity manager has died", e);
        }
    }

    @Deprecated
    public void startSync(Uri uri, Bundle extras) {
        Account account = null;
        if (extras != null) {
            String accountName = extras.getString("account");
            if (!TextUtils.isEmpty(accountName)) {
                account = new Account(accountName, "com.google");
            }
            extras.remove("account");
        }
        requestSync(account, uri != null ? uri.getAuthority() : null, extras);
    }

    public static void requestSync(Account account, String authority, Bundle extras) {
        requestSyncAsUser(account, authority, UserHandle.myUserId(), extras);
    }

    public static void requestSyncAsUser(Account account, String authority, int userId, Bundle extras) {
        if (extras == null) {
            throw new IllegalArgumentException("Must specify extras.");
        }
        SyncRequest request = new SyncRequest.Builder().setSyncAdapter(account, authority).setExtras(extras).syncOnce().build();
        try {
            getContentService().syncAsUser(request, userId);
        } catch (RemoteException e) {
        }
    }

    public static void requestSync(SyncRequest request) {
        try {
            getContentService().sync(request);
        } catch (RemoteException e) {
        }
    }

    public static void validateSyncExtrasBundle(Bundle extras) {
        try {
            for (String key : extras.keySet()) {
                Object value = extras.get(key);
                if (value != null && !(value instanceof Long) && !(value instanceof Integer) && !(value instanceof Boolean) && !(value instanceof Float) && !(value instanceof Double) && !(value instanceof String) && !(value instanceof Account)) {
                    throw new IllegalArgumentException("unexpected value type: " + value.getClass().getName());
                }
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (RuntimeException exc) {
            throw new IllegalArgumentException("error unparceling Bundle", exc);
        }
    }

    @Deprecated
    public void cancelSync(Uri uri) {
        cancelSync(null, uri != null ? uri.getAuthority() : null);
    }

    public static void cancelSync(Account account, String authority) {
        try {
            getContentService().cancelSync(account, authority, null);
        } catch (RemoteException e) {
        }
    }

    public static void cancelSyncAsUser(Account account, String authority, int userId) {
        try {
            getContentService().cancelSyncAsUser(account, authority, null, userId);
        } catch (RemoteException e) {
        }
    }

    public static SyncAdapterType[] getSyncAdapterTypes() {
        try {
            return getContentService().getSyncAdapterTypes();
        } catch (RemoteException e) {
            throw new RuntimeException("the ContentService should always be reachable", e);
        }
    }

    public static SyncAdapterType[] getSyncAdapterTypesAsUser(int userId) {
        try {
            return getContentService().getSyncAdapterTypesAsUser(userId);
        } catch (RemoteException e) {
            throw new RuntimeException("the ContentService should always be reachable", e);
        }
    }

    public static boolean getSyncAutomatically(Account account, String authority) {
        try {
            return getContentService().getSyncAutomatically(account, authority);
        } catch (RemoteException e) {
            throw new RuntimeException("the ContentService should always be reachable", e);
        }
    }

    public static boolean getSyncAutomaticallyAsUser(Account account, String authority, int userId) {
        try {
            return getContentService().getSyncAutomaticallyAsUser(account, authority, userId);
        } catch (RemoteException e) {
            throw new RuntimeException("the ContentService should always be reachable", e);
        }
    }

    public static void setSyncAutomatically(Account account, String authority, boolean sync) {
        setSyncAutomaticallyAsUser(account, authority, sync, UserHandle.myUserId());
    }

    public static void setSyncAutomaticallyAsUser(Account account, String authority, boolean sync, int userId) {
        try {
            getContentService().setSyncAutomaticallyAsUser(account, authority, sync, userId);
        } catch (RemoteException e) {
        }
    }

    public static void addPeriodicSync(Account account, String authority, Bundle extras, long pollFrequency) {
        validateSyncExtrasBundle(extras);
        if (extras.getBoolean("force", false) || extras.getBoolean(SYNC_EXTRAS_DO_NOT_RETRY, false) || extras.getBoolean(SYNC_EXTRAS_IGNORE_BACKOFF, false) || extras.getBoolean(SYNC_EXTRAS_IGNORE_SETTINGS, false) || extras.getBoolean(SYNC_EXTRAS_INITIALIZE, false) || extras.getBoolean("force", false) || extras.getBoolean(SYNC_EXTRAS_EXPEDITED, false)) {
            throw new IllegalArgumentException("illegal extras were set");
        }
        try {
            getContentService().addPeriodicSync(account, authority, extras, pollFrequency);
        } catch (RemoteException e) {
        }
    }

    public static boolean invalidPeriodicExtras(Bundle extras) {
        return extras.getBoolean("force", false) || extras.getBoolean(SYNC_EXTRAS_DO_NOT_RETRY, false) || extras.getBoolean(SYNC_EXTRAS_IGNORE_BACKOFF, false) || extras.getBoolean(SYNC_EXTRAS_IGNORE_SETTINGS, false) || extras.getBoolean(SYNC_EXTRAS_INITIALIZE, false) || extras.getBoolean("force", false) || extras.getBoolean(SYNC_EXTRAS_EXPEDITED, false);
    }

    public static void removePeriodicSync(Account account, String authority, Bundle extras) {
        validateSyncExtrasBundle(extras);
        try {
            getContentService().removePeriodicSync(account, authority, extras);
        } catch (RemoteException e) {
            throw new RuntimeException("the ContentService should always be reachable", e);
        }
    }

    public static void cancelSync(SyncRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request cannot be null");
        }
        try {
            getContentService().cancelRequest(request);
        } catch (RemoteException e) {
        }
    }

    public static List<PeriodicSync> getPeriodicSyncs(Account account, String authority) {
        try {
            return getContentService().getPeriodicSyncs(account, authority, null);
        } catch (RemoteException e) {
            throw new RuntimeException("the ContentService should always be reachable", e);
        }
    }

    public static int getIsSyncable(Account account, String authority) {
        try {
            return getContentService().getIsSyncable(account, authority);
        } catch (RemoteException e) {
            throw new RuntimeException("the ContentService should always be reachable", e);
        }
    }

    public static int getIsSyncableAsUser(Account account, String authority, int userId) {
        try {
            return getContentService().getIsSyncableAsUser(account, authority, userId);
        } catch (RemoteException e) {
            throw new RuntimeException("the ContentService should always be reachable", e);
        }
    }

    public static void setIsSyncable(Account account, String authority, int syncable) {
        try {
            getContentService().setIsSyncable(account, authority, syncable);
        } catch (RemoteException e) {
        }
    }

    public static boolean getMasterSyncAutomatically() {
        try {
            return getContentService().getMasterSyncAutomatically();
        } catch (RemoteException e) {
            throw new RuntimeException("the ContentService should always be reachable", e);
        }
    }

    public static boolean getMasterSyncAutomaticallyAsUser(int userId) {
        try {
            return getContentService().getMasterSyncAutomaticallyAsUser(userId);
        } catch (RemoteException e) {
            throw new RuntimeException("the ContentService should always be reachable", e);
        }
    }

    public static void setMasterSyncAutomatically(boolean sync) {
        setMasterSyncAutomaticallyAsUser(sync, UserHandle.myUserId());
    }

    public static void setMasterSyncAutomaticallyAsUser(boolean sync, int userId) {
        try {
            getContentService().setMasterSyncAutomaticallyAsUser(sync, userId);
        } catch (RemoteException e) {
        }
    }

    public static boolean isSyncActive(Account account, String authority) {
        if (account == null) {
            throw new IllegalArgumentException("account must not be null");
        }
        if (authority == null) {
            throw new IllegalArgumentException("authority must not be null");
        }
        try {
            return getContentService().isSyncActive(account, authority, null);
        } catch (RemoteException e) {
            throw new RuntimeException("the ContentService should always be reachable", e);
        }
    }

    @Deprecated
    public static SyncInfo getCurrentSync() {
        try {
            List<SyncInfo> syncs = getContentService().getCurrentSyncs();
            if (syncs.isEmpty()) {
                return null;
            }
            return syncs.get(0);
        } catch (RemoteException e) {
            throw new RuntimeException("the ContentService should always be reachable", e);
        }
    }

    public static List<SyncInfo> getCurrentSyncs() {
        try {
            return getContentService().getCurrentSyncs();
        } catch (RemoteException e) {
            throw new RuntimeException("the ContentService should always be reachable", e);
        }
    }

    public static List<SyncInfo> getCurrentSyncsAsUser(int userId) {
        try {
            return getContentService().getCurrentSyncsAsUser(userId);
        } catch (RemoteException e) {
            throw new RuntimeException("the ContentService should always be reachable", e);
        }
    }

    public static SyncStatusInfo getSyncStatus(Account account, String authority) {
        try {
            return getContentService().getSyncStatus(account, authority, null);
        } catch (RemoteException e) {
            throw new RuntimeException("the ContentService should always be reachable", e);
        }
    }

    public static SyncStatusInfo getSyncStatusAsUser(Account account, String authority, int userId) {
        try {
            return getContentService().getSyncStatusAsUser(account, authority, null, userId);
        } catch (RemoteException e) {
            throw new RuntimeException("the ContentService should always be reachable", e);
        }
    }

    public static boolean isSyncPending(Account account, String authority) {
        return isSyncPendingAsUser(account, authority, UserHandle.myUserId());
    }

    public static boolean isSyncPendingAsUser(Account account, String authority, int userId) {
        try {
            return getContentService().isSyncPendingAsUser(account, authority, null, userId);
        } catch (RemoteException e) {
            throw new RuntimeException("the ContentService should always be reachable", e);
        }
    }

    public static Object addStatusChangeListener(int mask, final SyncStatusObserver callback) {
        if (callback == null) {
            throw new IllegalArgumentException("you passed in a null callback");
        }
        try {
            ISyncStatusObserver.Stub observer = new ISyncStatusObserver.Stub() {
                @Override
                public void onStatusChanged(int which) throws RemoteException {
                    callback.onStatusChanged(which);
                }
            };
            getContentService().addStatusChangeListener(mask, observer);
            return observer;
        } catch (RemoteException e) {
            throw new RuntimeException("the ContentService should always be reachable", e);
        }
    }

    public static void removeStatusChangeListener(Object handle) {
        if (handle == null) {
            throw new IllegalArgumentException("you passed in a null handle");
        }
        try {
            getContentService().removeStatusChangeListener((ISyncStatusObserver.Stub) handle);
        } catch (RemoteException e) {
        }
    }

    private int samplePercentForDuration(long durationMillis) {
        if (durationMillis >= 500) {
            return 100;
        }
        return ((int) ((100 * durationMillis) / 500)) + 1;
    }

    private void maybeLogQueryToEventLog(long durationMillis, Uri uri, String[] projection, String selection, String sortOrder) {
    }

    private void maybeLogUpdateToEventLog(long durationMillis, Uri uri, String operation, String selection) {
    }

    private final class CursorWrapperInner extends CrossProcessCursorWrapper {
        public static final String TAG = "CursorWrapperInner";
        private final CloseGuard mCloseGuard;
        private final IContentProvider mContentProvider;
        private boolean mProviderReleased;

        CursorWrapperInner(Cursor cursor, IContentProvider icp) {
            super(cursor);
            this.mCloseGuard = CloseGuard.get();
            this.mContentProvider = icp;
            this.mCloseGuard.open("close");
        }

        @Override
        public void close() {
            super.close();
            ContentResolver.this.releaseProvider(this.mContentProvider);
            this.mProviderReleased = true;
            if (this.mCloseGuard != null) {
                this.mCloseGuard.close();
            }
        }

        protected void finalize() throws Throwable {
            try {
                if (this.mCloseGuard != null) {
                    this.mCloseGuard.warnIfOpen();
                }
                if (!this.mProviderReleased && this.mContentProvider != null) {
                    Log.w(TAG, "Cursor finalized without prior close()");
                    ContentResolver.this.releaseProvider(this.mContentProvider);
                }
            } finally {
                super.finalize();
            }
        }
    }

    private final class ParcelFileDescriptorInner extends ParcelFileDescriptor {
        private final IContentProvider mContentProvider;
        private boolean mProviderReleased;

        ParcelFileDescriptorInner(ParcelFileDescriptor pfd, IContentProvider icp) {
            super(pfd);
            this.mContentProvider = icp;
        }

        @Override
        public void releaseResources() {
            if (!this.mProviderReleased) {
                ContentResolver.this.releaseProvider(this.mContentProvider);
                this.mProviderReleased = true;
            }
        }
    }

    public static IContentService getContentService() {
        if (sContentService != null) {
            return sContentService;
        }
        IBinder b = ServiceManager.getService("content");
        sContentService = IContentService.Stub.asInterface(b);
        return sContentService;
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public int resolveUserId(Uri uri) {
        return ContentProvider.getUserIdFromUri(uri, this.mContext.getUserId());
    }
}
