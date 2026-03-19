package com.android.documentsui.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.FileUtils;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.OperationCanceledException;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.storage.StorageManager;
import android.provider.DocumentsContract;
import android.system.ErrnoException;
import android.system.Int64Ref;
import android.system.Os;
import android.system.OsConstants;
import android.text.format.DateUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;
import com.android.documentsui.DocumentsApplication;
import com.android.documentsui.Metrics;
import com.android.documentsui.R;
import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.base.DocumentStack;
import com.android.documentsui.base.Features;
import com.android.documentsui.base.RootInfo;
import com.android.documentsui.base.SharedMinimal;
import com.android.documentsui.clipping.UrisSupplier;
import com.android.documentsui.roots.ProvidersCache;
import com.android.documentsui.services.Job;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.SyncFailedException;
import java.text.NumberFormat;
import java.util.ArrayList;
import libcore.io.IoUtils;

class CopyJob extends ResolvedResourcesJob {
    static final boolean $assertionsDisabled = false;
    final ArrayList<DocumentInfo> convertedFiles;
    private volatile long mBytesCopied;
    private long mBytesCopiedSample;
    private long mBytesRequired;
    DocumentInfo mDstInfo;
    private final Handler mHandler;
    private final Messenger mMessenger;
    private long mRemainingTime;
    private long mSampleTime;
    private long mSpeed;
    private long mStartTime;

    CopyJob(Context context, Job.Listener listener, String str, DocumentStack documentStack, UrisSupplier urisSupplier, Messenger messenger, Features features) {
        this(context, listener, str, 1, documentStack, urisSupplier, messenger, features);
    }

    CopyJob(Context context, Job.Listener listener, String str, int i, DocumentStack documentStack, UrisSupplier urisSupplier, Messenger messenger, Features features) {
        super(context, listener, str, i, documentStack, urisSupplier, features);
        this.convertedFiles = new ArrayList<>();
        this.mHandler = new Handler(Looper.getMainLooper());
        this.mStartTime = -1L;
        this.mDstInfo = documentStack.peek();
        this.mMessenger = messenger;
    }

    @Override
    Notification.Builder createProgressBuilder() {
        return super.createProgressBuilder(this.service.getString(R.string.copy_notification_title), R.drawable.ic_menu_copy, this.service.getString(android.R.string.cancel), R.drawable.ic_cab_cancel);
    }

    @Override
    public Notification getSetupNotification() {
        return getSetupNotification(this.service.getString(R.string.copy_preparing));
    }

    Notification getProgressNotification(int i) {
        updateRemainingTimeEstimate();
        if (this.mBytesRequired >= 0) {
            double d = this.mBytesCopied / this.mBytesRequired;
            this.mProgressBuilder.setProgress(100, (int) (100.0d * d), false);
            this.mProgressBuilder.setSubText(NumberFormat.getPercentInstance().format(d));
        } else {
            this.mProgressBuilder.setProgress(0, 0, true);
        }
        if (this.mRemainingTime > 0) {
            this.mProgressBuilder.setContentText(this.service.getString(i, DateUtils.formatDuration(this.mRemainingTime)));
        } else {
            this.mProgressBuilder.setContentText(null);
        }
        return this.mProgressBuilder.build();
    }

    @Override
    public Notification getProgressNotification() {
        return getProgressNotification(R.string.copy_remaining);
    }

    void onBytesCopied(long j) {
        this.mBytesCopied += j;
    }

    @Override
    void finish() {
        try {
            this.mMessenger.send(Message.obtain(this.mHandler, 1, 0, 0));
        } catch (RemoteException e) {
        }
        super.finish();
    }

    private void updateRemainingTimeEstimate() {
        long jElapsedRealtime = SystemClock.elapsedRealtime() - this.mStartTime;
        long j = this.mBytesCopied;
        long jMax = ((j - this.mBytesCopiedSample) * 1000) / Math.max(jElapsedRealtime - this.mSampleTime, 1L);
        if (this.mSpeed == 0) {
            this.mSpeed = jMax;
        } else {
            this.mSpeed = ((3 * this.mSpeed) + jMax) / 4;
        }
        if (this.mSampleTime > 0 && this.mSpeed > 0) {
            this.mRemainingTime = ((this.mBytesRequired - j) * 1000) / this.mSpeed;
        } else {
            this.mRemainingTime = 0L;
        }
        this.mSampleTime = jElapsedRealtime;
        this.mBytesCopiedSample = j;
    }

    @Override
    Notification getFailureNotification() {
        return getFailureNotification(R.plurals.copy_error_notification_title, R.drawable.ic_menu_copy);
    }

    @Override
    Notification getWarningNotification() {
        Intent intentBuildNavigateIntent = buildNavigateIntent("warning");
        intentBuildNavigateIntent.putExtra("com.android.documentsui.DIALOG_TYPE", 2);
        intentBuildNavigateIntent.putExtra("com.android.documentsui.OPERATION_TYPE", this.operationType);
        intentBuildNavigateIntent.putParcelableArrayListExtra("com.android.documentsui.FAILED_DOCS", this.convertedFiles);
        return createNotificationBuilder().setContentTitle(this.service.getResources().getString(R.string.notification_copy_files_converted_title)).setContentText(this.service.getString(R.string.notification_touch_for_details)).setContentIntent(PendingIntent.getActivity(this.appContext, 0, intentBuildNavigateIntent, 1207959552)).setCategory("err").setSmallIcon(R.drawable.ic_menu_copy).setAutoCancel(true).build();
    }

    @Override
    boolean setUp() {
        if (!super.setUp() || isCanceled()) {
            return false;
        }
        try {
            this.mBytesRequired = calculateBytesRequired();
        } catch (ResourceException e) {
            Log.w("CopyJob", "Failed to calculate total size. Copying without progress.", e);
            this.mBytesRequired = -1L;
        }
        if (isCanceled()) {
            return false;
        }
        return checkSpace();
    }

    @Override
    void start() throws Throwable {
        this.mStartTime = SystemClock.elapsedRealtime();
        for (int i = 0; i < this.mResolvedDocs.size() && !isCanceled(); i++) {
            DocumentInfo documentInfo = this.mResolvedDocs.get(i);
            if (SharedMinimal.DEBUG) {
                Log.d("CopyJob", "Copying " + documentInfo.displayName + " (" + documentInfo.derivedUri + ") to " + this.mDstInfo.displayName + " (" + this.mDstInfo.derivedUri + ")");
            }
            try {
                if (this.mDstInfo.equals(documentInfo) || isDescendentOf(documentInfo, this.mDstInfo)) {
                    if (SharedMinimal.DEBUG) {
                        Log.e("CopyJob", "Skipping recursive copy of " + documentInfo.derivedUri);
                    }
                    onFileFailed(documentInfo);
                } else {
                    processDocument(documentInfo, null, this.mDstInfo);
                }
            } catch (ResourceException e) {
                if (SharedMinimal.DEBUG) {
                    Log.e("CopyJob", "Failed to copy " + documentInfo.derivedUri, e);
                }
                onFileFailed(documentInfo);
            }
        }
        Metrics.logFileOperation(this.service, this.operationType, this.mResolvedDocs, this.mDstInfo);
    }

    boolean checkSpace() {
        return verifySpaceAvailable(this.mBytesRequired);
    }

    final boolean verifySpaceAvailable(long j) {
        boolean z = true;
        if (j >= 0) {
            ProvidersCache providersCache = DocumentsApplication.getProvidersCache(this.appContext);
            RootInfo root = this.stack.getRoot();
            RootInfo rootOneshot = providersCache.getRootOneshot(root.authority, root.rootId, true);
            if (rootOneshot.availableBytes < 0) {
                Log.w("CopyJob", rootOneshot.toString() + " doesn't provide available bytes.");
            } else if (j > rootOneshot.availableBytes) {
                z = false;
            }
        }
        if (!z) {
            this.failureCount = this.mResolvedDocs.size();
            this.failedDocs.addAll(this.mResolvedDocs);
        }
        return z;
    }

    @Override
    boolean hasWarnings() {
        return !this.convertedFiles.isEmpty();
    }

    private void makeCopyProgress(long j) {
        try {
            this.mMessenger.send(Message.obtain(this.mHandler, 0, this.mBytesRequired >= 0 ? (int) ((100.0d * this.mBytesCopied) / this.mBytesRequired) : -1, (int) this.mRemainingTime));
        } catch (RemoteException e) {
        }
        onBytesCopied(j);
    }

    void processDocument(DocumentInfo documentInfo, DocumentInfo documentInfo2, DocumentInfo documentInfo3) throws Throwable {
        if (documentInfo.authority.equals(documentInfo3.authority) && (documentInfo.flags & 128) != 0) {
            try {
                if (DocumentsContract.copyDocument(getClient(documentInfo), documentInfo.derivedUri, documentInfo3.derivedUri) != null) {
                    Metrics.logFileOperated(this.appContext, this.operationType, 1);
                    return;
                }
            } catch (RemoteException | RuntimeException e) {
                if (SharedMinimal.DEBUG) {
                    Log.e("CopyJob", "Provider side copy failed for: " + documentInfo.derivedUri + " due to an exception.", e);
                }
                Metrics.logFileOperationFailure(this.appContext, 10, documentInfo.derivedUri);
            }
            if (SharedMinimal.DEBUG) {
                Log.d("CopyJob", "Fallback to byte-by-byte copy for: " + documentInfo.derivedUri);
            }
        }
        byteCopyDocument(documentInfo, documentInfo3);
    }

    void byteCopyDocument(DocumentInfo documentInfo, DocumentInfo documentInfo2) throws Throwable {
        String str;
        String string;
        String str2;
        if (SharedMinimal.DEBUG) {
            Log.d("CopyJob", "Doing byte copy of document: " + documentInfo);
        }
        if (documentInfo.isVirtual()) {
            try {
                String[] streamTypes = getContentResolver().getStreamTypes(documentInfo.derivedUri, "*/*");
                if (streamTypes == null || streamTypes.length <= 0) {
                    Metrics.logFileOperationFailure(this.appContext, 8, documentInfo.derivedUri);
                    throw new ResourceException("Cannot copy virtual file %s. No streamable formats available.", documentInfo.derivedUri);
                }
                str = streamTypes[0];
                String extensionFromMimeType = MimeTypeMap.getSingleton().getExtensionFromMimeType(str);
                StringBuilder sb = new StringBuilder();
                sb.append(documentInfo.displayName);
                if (extensionFromMimeType != null) {
                    str2 = "." + extensionFromMimeType;
                } else {
                    str2 = documentInfo.displayName;
                }
                sb.append(str2);
                string = sb.toString();
            } catch (RuntimeException e) {
                Metrics.logFileOperationFailure(this.appContext, 8, documentInfo.derivedUri);
                throw new ResourceException("Failed to obtain streamable types for %s due to an exception.", documentInfo.derivedUri, e);
            }
        } else {
            str = documentInfo.mimeType;
            string = documentInfo.displayName;
        }
        try {
            Uri uriCreateDocument = DocumentsContract.createDocument(getClient(documentInfo2), documentInfo2.derivedUri, str, string);
            if (uriCreateDocument == null) {
                Metrics.logFileOperationFailure(this.appContext, 5, documentInfo2.derivedUri);
                throw new ResourceException("Couldn't create destination document " + string + " in directory %s.", documentInfo2.derivedUri);
            }
            try {
                DocumentInfo documentInfoFromUri = DocumentInfo.fromUri(getContentResolver(), uriCreateDocument);
                if ("vnd.android.document/directory".equals(documentInfo.mimeType)) {
                    copyDirectoryHelper(documentInfo, documentInfoFromUri);
                } else {
                    copyFileHelper(documentInfo, documentInfoFromUri, documentInfo2, str);
                }
            } catch (FileNotFoundException | RuntimeException e2) {
                Metrics.logFileOperationFailure(this.appContext, 1, uriCreateDocument);
                throw new ResourceException("Could not load DocumentInfo for newly created file %s.", uriCreateDocument);
            }
        } catch (RemoteException | RuntimeException e3) {
            Metrics.logFileOperationFailure(this.appContext, 5, documentInfo2.derivedUri);
            throw new ResourceException("Couldn't create destination document " + string + " in directory %s due to an exception.", documentInfo2.derivedUri, e3);
        }
    }

    private void copyDirectoryHelper(DocumentInfo documentInfo, DocumentInfo documentInfo2) throws Throwable {
        RuntimeException e;
        Cursor cursorQueryChildren;
        boolean z = false;
        try {
            try {
                cursorQueryChildren = queryChildren(documentInfo, new String[]{"_display_name", "document_id", "mime_type", "_size", "flags"});
                boolean z2 = true;
                while (cursorQueryChildren.moveToNext() && !isCanceled()) {
                    try {
                        try {
                            try {
                                processDocument(DocumentInfo.fromCursor(cursorQueryChildren, documentInfo.authority), documentInfo, documentInfo2);
                            } catch (RuntimeException e2) {
                                if (SharedMinimal.DEBUG) {
                                    Log.e("CopyJob", String.format("Failed to recursively process a file %s due to an exception.", documentInfo.derivedUri.toString()), e2);
                                }
                                z2 = false;
                            }
                        } catch (RuntimeException e3) {
                            e = e3;
                            if (SharedMinimal.DEBUG) {
                                Log.e("CopyJob", String.format("Failed to copy a file %s to %s. ", documentInfo.derivedUri.toString(), documentInfo2.derivedUri.toString()), e);
                            }
                            IoUtils.closeQuietly(cursorQueryChildren);
                        }
                    } catch (Throwable th) {
                        th = th;
                        IoUtils.closeQuietly(cursorQueryChildren);
                        throw th;
                    }
                }
                IoUtils.closeQuietly(cursorQueryChildren);
                z = z2;
                if (z) {
                    throw new RuntimeException("Some files failed to copy during a recursive directory copy.");
                }
            } catch (Throwable th2) {
                th = th2;
                cursorQueryChildren = null;
                IoUtils.closeQuietly(cursorQueryChildren);
                throw th;
            }
        } catch (RemoteException | RuntimeException e4) {
            try {
                Metrics.logFileOperationFailure(this.appContext, 2, documentInfo.derivedUri);
                throw new ResourceException("Failed to query children of %s due to an exception.", documentInfo.derivedUri, e4);
            } catch (RuntimeException e5) {
                e = e5;
                cursorQueryChildren = null;
                if (SharedMinimal.DEBUG) {
                }
                IoUtils.closeQuietly(cursorQueryChildren);
                if (z) {
                }
            }
        }
    }

    private void copyFileHelper(DocumentInfo documentInfo, DocumentInfo documentInfo2, DocumentInfo documentInfo3, String str) throws Throwable {
        ?? autoCloseOutputStream;
        ParcelFileDescriptor parcelFileDescriptorOpenFile;
        Throwable th;
        ParcelFileDescriptor.AutoCloseInputStream autoCloseInputStream;
        ParcelFileDescriptor parcelFileDescriptorOpenFile2;
        try {
            autoCloseOutputStream = 3;
            if (documentInfo.isVirtual()) {
                try {
                    AssetFileDescriptor assetFileDescriptorOpenTypedAssetFileDescriptor = getClient(documentInfo).openTypedAssetFileDescriptor(documentInfo.derivedUri, str, null, this.mSignal);
                    parcelFileDescriptorOpenFile2 = assetFileDescriptorOpenTypedAssetFileDescriptor.getParcelFileDescriptor();
                    try {
                        AssetFileDescriptor.AutoCloseInputStream autoCloseInputStream2 = new AssetFileDescriptor.AutoCloseInputStream(assetFileDescriptorOpenTypedAssetFileDescriptor);
                        try {
                            Metrics.logFileOperated(this.appContext, this.operationType, 2);
                            autoCloseInputStream = autoCloseInputStream2;
                        } catch (Throwable th2) {
                            autoCloseOutputStream = 0;
                            th = th2;
                            autoCloseInputStream = autoCloseInputStream2;
                            parcelFileDescriptorOpenFile = null;
                            if (parcelFileDescriptorOpenFile != null) {
                            }
                            if (SharedMinimal.DEBUG) {
                            }
                            this.mSignal.cancel();
                            try {
                                deleteDocument(documentInfo2, documentInfo3);
                            } catch (ResourceException e) {
                                if (SharedMinimal.DEBUG) {
                                    Log.w("CopyJob", "Failed to cleanup after copy error: " + documentInfo.derivedUri, e);
                                }
                            }
                            IoUtils.closeQuietly(autoCloseInputStream);
                            IoUtils.closeQuietly((AutoCloseable) autoCloseOutputStream);
                            throw th;
                        }
                    } catch (IOException e2) {
                        Metrics.logFileOperationFailure(this.appContext, 3, documentInfo.derivedUri);
                        throw new ResourceException("Failed to open a file input stream for %s due an exception.", documentInfo.derivedUri, e2);
                    }
                } catch (RemoteException | FileNotFoundException | RuntimeException e3) {
                    Metrics.logFileOperationFailure(this.appContext, 3, documentInfo.derivedUri);
                    throw new ResourceException("Failed to open a file as asset for %s due to an exception.", documentInfo.derivedUri, e3);
                }
            } else {
                try {
                    parcelFileDescriptorOpenFile2 = getClient(documentInfo).openFile(documentInfo.derivedUri, "r", this.mSignal);
                    autoCloseInputStream = new ParcelFileDescriptor.AutoCloseInputStream(parcelFileDescriptorOpenFile2);
                    try {
                        Metrics.logFileOperated(this.appContext, this.operationType, 3);
                    } catch (Throwable th3) {
                        th = th3;
                        autoCloseOutputStream = 0;
                        parcelFileDescriptorOpenFile = null;
                        th = th;
                        if (parcelFileDescriptorOpenFile != null) {
                            try {
                                parcelFileDescriptorOpenFile.closeWithError("Error copying bytes.");
                            } catch (IOException e4) {
                                Log.w("CopyJob", "Error closing destination.", e4);
                            }
                        }
                        if (SharedMinimal.DEBUG) {
                            Log.d("CopyJob", "Cleaning up failed operation leftovers.");
                        }
                        this.mSignal.cancel();
                        deleteDocument(documentInfo2, documentInfo3);
                        IoUtils.closeQuietly(autoCloseInputStream);
                        IoUtils.closeQuietly((AutoCloseable) autoCloseOutputStream);
                        throw th;
                    }
                } catch (RemoteException | FileNotFoundException | RuntimeException e5) {
                    Metrics.logFileOperationFailure(this.appContext, 3, documentInfo.derivedUri);
                    throw new ResourceException("Failed to open a file for %s due to an exception.", documentInfo.derivedUri, e5);
                }
            }
        } catch (Throwable th4) {
            autoCloseOutputStream = 0;
            parcelFileDescriptorOpenFile = null;
            th = th4;
            autoCloseInputStream = null;
        }
        try {
            parcelFileDescriptorOpenFile = getClient(documentInfo2).openFile(documentInfo2.derivedUri, "w", this.mSignal);
            try {
                try {
                    autoCloseOutputStream = new ParcelFileDescriptor.AutoCloseOutputStream(parcelFileDescriptorOpenFile);
                    try {
                        StorageManager storageManager = (StorageManager) this.service.getSystemService(StorageManager.class);
                        long statSize = parcelFileDescriptorOpenFile2.getStatSize();
                        FileDescriptor fileDescriptor = parcelFileDescriptorOpenFile.getFileDescriptor();
                        if (statSize > 0 && storageManager.isAllocationSupported(fileDescriptor)) {
                            storageManager.allocateBytes(fileDescriptor, statSize);
                        }
                        try {
                            final Int64Ref int64Ref = new Int64Ref(0L);
                            FileUtils.copy(autoCloseInputStream, autoCloseOutputStream, new FileUtils.ProgressListener() {
                                @Override
                                public final void onProgress(long j) {
                                    CopyJob.lambda$copyFileHelper$0(this.f$0, int64Ref, j);
                                }
                            }, this.mSignal);
                            try {
                                Os.fsync(parcelFileDescriptorOpenFile.getFileDescriptor());
                            } catch (ErrnoException e6) {
                                if (e6.errno != OsConstants.EROFS && e6.errno != OsConstants.EINVAL) {
                                    throw new SyncFailedException("Failed to sync bytes after copying a file.");
                                }
                            }
                            IoUtils.close(parcelFileDescriptorOpenFile.getFileDescriptor());
                            parcelFileDescriptorOpenFile2.checkError();
                            if (documentInfo.isVirtual()) {
                                this.convertedFiles.add(documentInfo);
                            }
                            IoUtils.closeQuietly(autoCloseInputStream);
                            IoUtils.closeQuietly((AutoCloseable) autoCloseOutputStream);
                        } catch (OperationCanceledException e7) {
                            if (SharedMinimal.DEBUG) {
                                Log.d("CopyJob", "Canceled copy mid-copy of: " + documentInfo.derivedUri);
                            }
                            if (parcelFileDescriptorOpenFile != null) {
                                try {
                                    parcelFileDescriptorOpenFile.closeWithError("Error copying bytes.");
                                } catch (IOException e8) {
                                    Log.w("CopyJob", "Error closing destination.", e8);
                                }
                            }
                            if (SharedMinimal.DEBUG) {
                                Log.d("CopyJob", "Cleaning up failed operation leftovers.");
                            }
                            this.mSignal.cancel();
                            try {
                                deleteDocument(documentInfo2, documentInfo3);
                            } catch (ResourceException e9) {
                                if (SharedMinimal.DEBUG) {
                                    Log.w("CopyJob", "Failed to cleanup after copy error: " + documentInfo.derivedUri, e9);
                                }
                            }
                            IoUtils.closeQuietly(autoCloseInputStream);
                            IoUtils.closeQuietly((AutoCloseable) autoCloseOutputStream);
                        }
                    } catch (IOException e10) {
                        Metrics.logFileOperationFailure(this.appContext, 6, documentInfo2.derivedUri);
                        throw new ResourceException("Failed to copy bytes from %s to %s due to an IO exception.", documentInfo.derivedUri, documentInfo2.derivedUri, e10);
                    }
                } catch (Throwable th5) {
                    th = th5;
                    autoCloseOutputStream = 0;
                    th = th;
                    if (parcelFileDescriptorOpenFile != null) {
                    }
                    if (SharedMinimal.DEBUG) {
                    }
                    this.mSignal.cancel();
                    deleteDocument(documentInfo2, documentInfo3);
                    IoUtils.closeQuietly(autoCloseInputStream);
                    IoUtils.closeQuietly((AutoCloseable) autoCloseOutputStream);
                    throw th;
                }
            } catch (Throwable th6) {
                th = th6;
                if (parcelFileDescriptorOpenFile != null) {
                }
                if (SharedMinimal.DEBUG) {
                }
                this.mSignal.cancel();
                deleteDocument(documentInfo2, documentInfo3);
                IoUtils.closeQuietly(autoCloseInputStream);
                IoUtils.closeQuietly((AutoCloseable) autoCloseOutputStream);
                throw th;
            }
        } catch (RemoteException | FileNotFoundException | RuntimeException e11) {
            Metrics.logFileOperationFailure(this.appContext, 3, documentInfo2.derivedUri);
            throw new ResourceException("Failed to open the destination file %s for writing due to an exception.", documentInfo2.derivedUri, e11);
        }
    }

    public static void lambda$copyFileHelper$0(CopyJob copyJob, Int64Ref int64Ref, long j) {
        long j2 = j - int64Ref.value;
        int64Ref.value = j;
        copyJob.makeCopyProgress(j2);
    }

    private long calculateBytesRequired() throws ResourceException {
        long jCalculateFileSizesRecursively = 0;
        for (DocumentInfo documentInfo : this.mResolvedDocs) {
            if (documentInfo.isDirectory()) {
                try {
                    jCalculateFileSizesRecursively += calculateFileSizesRecursively(getClient(documentInfo), documentInfo.derivedUri);
                } catch (RemoteException e) {
                    throw new ResourceException("Failed to obtain the client for %s.", documentInfo.derivedUri, e);
                }
            } else {
                jCalculateFileSizesRecursively += documentInfo.size;
            }
            if (isCanceled()) {
                return jCalculateFileSizesRecursively;
            }
        }
        return jCalculateFileSizesRecursively;
    }

    long calculateFileSizesRecursively(ContentProviderClient contentProviderClient, Uri uri) throws Throwable {
        Cursor cursorQueryChildren;
        String authority = uri.getAuthority();
        Cursor cursor = null;
        try {
            try {
                cursorQueryChildren = queryChildren(contentProviderClient, uri, new String[]{"document_id", "mime_type", "_size"});
                long jCalculateFileSizesRecursively = 0;
                while (cursorQueryChildren.moveToNext() && !isCanceled()) {
                    try {
                        if ("vnd.android.document/directory".equals(DocumentInfo.getCursorString(cursorQueryChildren, "mime_type"))) {
                            jCalculateFileSizesRecursively += calculateFileSizesRecursively(contentProviderClient, DocumentsContract.buildDocumentUri(authority, DocumentInfo.getCursorString(cursorQueryChildren, "document_id")));
                        } else {
                            long cursorLong = DocumentInfo.getCursorLong(cursorQueryChildren, "_size");
                            if (cursorLong <= 0) {
                                cursorLong = 0;
                            }
                            jCalculateFileSizesRecursively += cursorLong;
                        }
                    } catch (RemoteException | RuntimeException e) {
                        e = e;
                        cursor = cursorQueryChildren;
                        throw new ResourceException("Failed to calculate size for %s due to an exception.", uri, e);
                    } catch (Throwable th) {
                        th = th;
                        IoUtils.closeQuietly(cursorQueryChildren);
                        throw th;
                    }
                }
                IoUtils.closeQuietly(cursorQueryChildren);
                return jCalculateFileSizesRecursively;
            } catch (RemoteException | RuntimeException e2) {
                e = e2;
            }
        } catch (Throwable th2) {
            th = th2;
            cursorQueryChildren = cursor;
        }
    }

    private Cursor queryChildren(DocumentInfo documentInfo, String[] strArr) throws RemoteException {
        return queryChildren(getClient(documentInfo), documentInfo.derivedUri, strArr);
    }

    private Cursor queryChildren(ContentProviderClient contentProviderClient, Uri uri, String[] strArr) throws RemoteException {
        Uri uriBuildChildDocumentsUri = DocumentsContract.buildChildDocumentsUri(uri.getAuthority(), DocumentsContract.getDocumentId(uri));
        String str = (String) null;
        Cursor cursorQuery = contentProviderClient.query(uriBuildChildDocumentsUri, strArr, str, null, null);
        while (cursorQuery.getExtras().getBoolean("loading")) {
            cursorQuery.registerContentObserver(new DirectoryChildrenObserver(uriBuildChildDocumentsUri));
            try {
                long jCurrentTimeMillis = System.currentTimeMillis();
                synchronized (uriBuildChildDocumentsUri) {
                    uriBuildChildDocumentsUri.wait(60000L);
                }
                if (System.currentTimeMillis() - jCurrentTimeMillis > 60000) {
                    throw new RemoteException("Timed out waiting on update for " + uriBuildChildDocumentsUri);
                }
                cursorQuery = contentProviderClient.query(uriBuildChildDocumentsUri, strArr, str, null, null);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return cursorQuery;
    }

    boolean isDescendentOf(DocumentInfo documentInfo, DocumentInfo documentInfo2) throws ResourceException {
        if (documentInfo2.isDirectory() && documentInfo.authority.equals(documentInfo2.authority)) {
            try {
                return DocumentsContract.isChildDocument(getClient(documentInfo), documentInfo.derivedUri, documentInfo2.derivedUri);
            } catch (RemoteException | RuntimeException e) {
                throw new ResourceException("Failed to check if %s is a child of %s due to an exception.", documentInfo.derivedUri, documentInfo2.derivedUri, e);
            }
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("CopyJob");
        sb.append("{");
        sb.append("id=" + this.id);
        sb.append(", uris=" + this.mResourceUris);
        sb.append(", docs=" + this.mResolvedDocs);
        sb.append(", destination=" + this.stack);
        sb.append("}");
        return sb.toString();
    }

    private static class DirectoryChildrenObserver extends ContentObserver {
        static final boolean $assertionsDisabled = false;
        private final Object mNotifier;

        private DirectoryChildrenObserver(Object obj) {
            super(new Handler(Looper.getMainLooper()));
            this.mNotifier = obj;
        }

        @Override
        public void onChange(boolean z, Uri uri) {
            synchronized (this.mNotifier) {
                this.mNotifier.notify();
            }
        }
    }
}
