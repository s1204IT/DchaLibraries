package com.android.mtp;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriPermission;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDiskIOException;
import android.graphics.Point;
import android.media.MediaFile;
import android.mtp.MtpObjectInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.FileUtils;
import android.os.ParcelFileDescriptor;
import android.os.ProxyFileDescriptorCallback;
import android.os.storage.StorageManager;
import android.provider.DocumentsContract;
import android.provider.DocumentsProvider;
import android.provider.MetadataReader;
import android.provider.Settings;
import android.system.ErrnoException;
import android.system.OsConstants;
import android.util.Log;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import libcore.io.IoUtils;

public class MtpDocumentsProvider extends DocumentsProvider {
    private static MtpDocumentsProvider sSingleton;
    private Context mContext;
    private MtpDatabase mDatabase;
    private final Object mDeviceListLock = new Object();

    @GuardedBy("mDeviceListLock")
    private Map<Integer, DeviceToolkit> mDeviceToolkits;
    private ServiceIntentSender mIntentSender;
    private MtpManager mMtpManager;
    private ContentResolver mResolver;
    private Resources mResources;
    private RootScanner mRootScanner;
    private StorageManager mStorageManager;
    static final String[] DEFAULT_ROOT_PROJECTION = {"root_id", "flags", "icon", "title", "document_id", "available_bytes"};
    static final String[] DEFAULT_DOCUMENT_PROJECTION = {"document_id", "mime_type", "_display_name", "last_modified", "flags", "_size"};

    static MtpDocumentsProvider getInstance() {
        return sSingleton;
    }

    @Override
    public boolean onCreate() throws Exception {
        sSingleton = this;
        this.mContext = getContext();
        this.mResources = getContext().getResources();
        this.mMtpManager = new MtpManager(getContext());
        this.mResolver = getContext().getContentResolver();
        this.mDeviceToolkits = new HashMap();
        this.mDatabase = new MtpDatabase(getContext(), 0);
        this.mRootScanner = new RootScanner(this.mResolver, this.mMtpManager, this.mDatabase);
        this.mIntentSender = new ServiceIntentSender(getContext());
        this.mStorageManager = (StorageManager) getContext().getSystemService(StorageManager.class);
        try {
            int i = Settings.Global.getInt(this.mResolver, "boot_count", -1);
            int lastBootCount = this.mDatabase.getLastBootCount();
            if (i != -1 && i != lastBootCount) {
                this.mDatabase.setLastBootCount(i);
                List<UriPermission> outgoingPersistedUriPermissions = this.mResolver.getOutgoingPersistedUriPermissions();
                Uri[] uriArr = new Uri[outgoingPersistedUriPermissions.size()];
                for (int i2 = 0; i2 < outgoingPersistedUriPermissions.size(); i2++) {
                    uriArr[i2] = outgoingPersistedUriPermissions.get(i2).getUri();
                }
                this.mDatabase.cleanDatabase(uriArr);
            }
            resume();
            return true;
        } catch (SQLiteDiskIOException e) {
            Log.e("MtpDocumentsProvider", "Failed to clean database.", e);
            return false;
        }
    }

    @VisibleForTesting
    boolean onCreateForTesting(Context context, Resources resources, MtpManager mtpManager, ContentResolver contentResolver, MtpDatabase mtpDatabase, StorageManager storageManager, ServiceIntentSender serviceIntentSender) {
        this.mContext = context;
        this.mResources = resources;
        this.mMtpManager = mtpManager;
        this.mResolver = contentResolver;
        this.mDeviceToolkits = new HashMap();
        this.mDatabase = mtpDatabase;
        this.mRootScanner = new RootScanner(this.mResolver, this.mMtpManager, this.mDatabase);
        this.mIntentSender = serviceIntentSender;
        this.mStorageManager = storageManager;
        resume();
        return true;
    }

    @Override
    public Cursor queryRoots(String[] strArr) throws FileNotFoundException {
        if (strArr == null) {
            strArr = DEFAULT_ROOT_PROJECTION;
        }
        Cursor cursorQueryRoots = this.mDatabase.queryRoots(this.mResources, strArr);
        cursorQueryRoots.setNotificationUri(this.mResolver, DocumentsContract.buildRootsUri("com.android.mtp.documents"));
        return cursorQueryRoots;
    }

    @Override
    public Cursor queryDocument(String str, String[] strArr) throws Exception {
        if (strArr == null) {
            strArr = DEFAULT_DOCUMENT_PROJECTION;
        }
        Cursor cursorQueryDocument = this.mDatabase.queryDocument(str, strArr);
        int count = cursorQueryDocument.getCount();
        if (count == 0) {
            cursorQueryDocument.close();
            throw new FileNotFoundException();
        }
        if (count != 1) {
            cursorQueryDocument.close();
            Log.wtf("MtpDocumentsProvider", "Unexpected cursor size: " + count);
            return null;
        }
        if (this.mDatabase.createIdentifier(str).mDocumentType != 0) {
            return cursorQueryDocument;
        }
        String[] storageDocumentIds = this.mDatabase.getStorageDocumentIds(str);
        if (storageDocumentIds.length != 1) {
            return this.mDatabase.queryDocument(str, strArr);
        }
        try {
            try {
                str = this.mDatabase.queryDocument(storageDocumentIds[0], MtpDatabase.strings("_display_name", "flags"));
                if (!str.moveToNext()) {
                    throw new FileNotFoundException();
                }
                String string = str.getString(0);
                int i = str.getInt(1);
                if (str != 0) {
                    str.close();
                }
                cursorQueryDocument.moveToNext();
                ContentValues contentValues = new ContentValues();
                DatabaseUtils.cursorRowToContentValues(cursorQueryDocument, contentValues);
                if (contentValues.containsKey("_display_name")) {
                    contentValues.put("_display_name", this.mResources.getString(R.string.root_name, contentValues.getAsString("_display_name"), string));
                }
                contentValues.put("flags", Integer.valueOf(i));
                MatrixCursor matrixCursor = new MatrixCursor(strArr, 1);
                MtpDatabase.putValuesToCursor(contentValues, matrixCursor);
                return matrixCursor;
            } finally {
            }
        } finally {
            cursorQueryDocument.close();
        }
    }

    @Override
    public Cursor queryChildDocuments(String str, String[] strArr, String str2) throws Exception {
        if (strArr == null) {
            strArr = DEFAULT_DOCUMENT_PROJECTION;
        }
        Identifier identifierCreateIdentifier = this.mDatabase.createIdentifier(str);
        try {
            openDevice(identifierCreateIdentifier.mDeviceId);
            if (identifierCreateIdentifier.mDocumentType == 0) {
                String[] storageDocumentIds = this.mDatabase.getStorageDocumentIds(str);
                if (storageDocumentIds.length == 0) {
                    return createErrorCursor(strArr, R.string.error_locked_device);
                }
                if (storageDocumentIds.length > 1) {
                    return this.mDatabase.queryChildDocuments(strArr, str);
                }
                identifierCreateIdentifier = this.mDatabase.createIdentifier(storageDocumentIds[0]);
            }
            return getDocumentLoader(identifierCreateIdentifier).queryChildDocuments(strArr, identifierCreateIdentifier);
        } catch (BusyDeviceException e) {
            return createErrorCursor(strArr, R.string.error_busy_device);
        } catch (IOException e2) {
            Log.e("MtpDocumentsProvider", "queryChildDocuments", e2);
            throw new FileNotFoundException(e2.getMessage());
        }
    }

    @Override
    public ParcelFileDescriptor openDocument(String str, String str2, CancellationSignal cancellationSignal) throws FileNotFoundException {
        long fileSize;
        Identifier identifierCreateIdentifier = this.mDatabase.createIdentifier(str);
        try {
            try {
                openDevice(identifierCreateIdentifier.mDeviceId);
                MtpDeviceRecord mtpDeviceRecord = getDeviceToolkit(identifierCreateIdentifier.mDeviceId).mDeviceRecord;
                int mode = ParcelFileDescriptor.parseMode(str2) & (-134217729);
                if ((268435456 & mode) != 0) {
                    try {
                        fileSize = getFileSize(str);
                    } catch (UnsupportedOperationException e) {
                        fileSize = -1;
                    }
                    if (MtpDeviceRecord.isPartialReadSupported(mtpDeviceRecord.operationsSupported, fileSize)) {
                        return this.mStorageManager.openProxyFileDescriptor(mode, new MtpProxyFileDescriptorCallback(Integer.parseInt(str)));
                    }
                    return getPipeManager(identifierCreateIdentifier).readDocument(this.mMtpManager, identifierCreateIdentifier);
                }
                if ((536870912 & mode) != 0) {
                    if (MtpDeviceRecord.isWritingSupported(mtpDeviceRecord.operationsSupported)) {
                        return this.mStorageManager.openProxyFileDescriptor(mode, new MtpProxyFileDescriptorCallback(Integer.parseInt(str)));
                    }
                    throw new UnsupportedOperationException("The device does not support writing operation.");
                }
                throw new UnsupportedOperationException("The provider does not support 'rw' mode.");
            } catch (IOException e2) {
                Log.e("MtpDocumentsProvider", "openDocument", e2);
                throw new IllegalStateException(e2);
            }
        } catch (FileNotFoundException | RuntimeException e3) {
            Log.e("MtpDocumentsProvider", "openDocument", e3);
            throw e3;
        }
    }

    @Override
    public AssetFileDescriptor openDocumentThumbnail(String str, Point point, CancellationSignal cancellationSignal) throws FileNotFoundException {
        Identifier identifierCreateIdentifier = this.mDatabase.createIdentifier(str);
        try {
            openDevice(identifierCreateIdentifier.mDeviceId);
            return new AssetFileDescriptor(getPipeManager(identifierCreateIdentifier).readThumbnail(this.mMtpManager, identifierCreateIdentifier), 0L, -1L);
        } catch (IOException e) {
            Log.e("MtpDocumentsProvider", "openDocumentThumbnail", e);
            throw new FileNotFoundException(e.getMessage());
        }
    }

    @Override
    public void deleteDocument(String str) throws FileNotFoundException {
        try {
            Identifier identifierCreateIdentifier = this.mDatabase.createIdentifier(str);
            openDevice(identifierCreateIdentifier.mDeviceId);
            Identifier parentIdentifier = this.mDatabase.getParentIdentifier(str);
            this.mMtpManager.deleteDocument(identifierCreateIdentifier.mDeviceId, identifierCreateIdentifier.mObjectHandle);
            this.mDatabase.deleteDocument(str);
            getDocumentLoader(parentIdentifier).cancelTask(parentIdentifier);
            notifyChildDocumentsChange(parentIdentifier.mDocumentId);
            if (parentIdentifier.mDocumentType == 1) {
                notifyChildDocumentsChange(this.mDatabase.getParentIdentifier(parentIdentifier.mDocumentId).mDocumentId);
            }
        } catch (IOException e) {
            Log.e("MtpDocumentsProvider", "deleteDocument", e);
            throw new FileNotFoundException(e.getMessage());
        }
    }

    @Override
    public void onTrimMemory(int i) {
        synchronized (this.mDeviceListLock) {
            Iterator<DeviceToolkit> it = this.mDeviceToolkits.values().iterator();
            while (it.hasNext()) {
                it.next().mDocumentLoader.clearCompletedTasks();
            }
        }
    }

    @Override
    public String createDocument(String str, String str2, String str3) throws FileNotFoundException {
        int i;
        int i2;
        int formatCode;
        int iCreateDocument;
        MtpObjectInfo mtpObjectInfoBuild;
        try {
            try {
                Identifier identifierCreateIdentifier = this.mDatabase.createIdentifier(str);
                openDevice(identifierCreateIdentifier.mDeviceId);
                MtpDeviceRecord mtpDeviceRecord = getDeviceToolkit(identifierCreateIdentifier.mDeviceId).mDeviceRecord;
                if (!MtpDeviceRecord.isWritingSupported(mtpDeviceRecord.operationsSupported)) {
                    throw new UnsupportedOperationException("Writing operation is not supported by the device.");
                }
                int i3 = 0;
                switch (identifierCreateIdentifier.mDocumentType) {
                    case 0:
                        String[] storageDocumentIds = this.mDatabase.getStorageDocumentIds(identifierCreateIdentifier.mDocumentId);
                        if (storageDocumentIds.length == 1) {
                            String strCreateDocument = createDocument(storageDocumentIds[0], str2, str3);
                            notifyChildDocumentsChange(str);
                            return strCreateDocument;
                        }
                        throw new UnsupportedOperationException("Cannot create a file under the device.");
                    case 1:
                        i = identifierCreateIdentifier.mStorageId;
                        i2 = -1;
                        break;
                    case 2:
                        i = identifierCreateIdentifier.mStorageId;
                        i2 = identifierCreateIdentifier.mObjectHandle;
                        break;
                    default:
                        throw new IllegalArgumentException("Unexpected document type.");
                }
                ParcelFileDescriptor[] parcelFileDescriptorArrCreateReliablePipe = ParcelFileDescriptor.createReliablePipe();
                try {
                    parcelFileDescriptorArrCreateReliablePipe[0].close();
                    if ("vnd.android.document/directory".equals(str2)) {
                        formatCode = 12289;
                    } else {
                        formatCode = MediaFile.getFormatCode(str3, str2);
                    }
                    MtpObjectInfo mtpObjectInfoBuild2 = new MtpObjectInfo.Builder().setStorageId(i).setParent(i2).setFormat(formatCode).setName(str3).build();
                    String[] strArrSplitFileName = FileUtils.splitFileName(str2, str3);
                    String str4 = strArrSplitFileName[0];
                    String str5 = strArrSplitFileName[1];
                    while (true) {
                        if (i3 <= 32) {
                            if (i3 != 0) {
                                String str6 = str4 + " (" + i3 + " )";
                                if (!str5.isEmpty()) {
                                    str6 = str6 + "." + str5;
                                }
                                mtpObjectInfoBuild = new MtpObjectInfo.Builder(mtpObjectInfoBuild2).setName(str6).build();
                            } else {
                                mtpObjectInfoBuild = mtpObjectInfoBuild2;
                            }
                            try {
                                iCreateDocument = this.mMtpManager.createDocument(identifierCreateIdentifier.mDeviceId, mtpObjectInfoBuild, parcelFileDescriptorArrCreateReliablePipe[1]);
                            } catch (SendObjectInfoFailure e) {
                                i3++;
                            }
                        } else {
                            iCreateDocument = -1;
                        }
                    }
                    if (iCreateDocument == -1) {
                        throw new IllegalArgumentException("The file name \"" + str3 + "\" is conflicted with existing files and the provider failed to find unique name.");
                    }
                    String strPutNewDocument = this.mDatabase.putNewDocument(identifierCreateIdentifier.mDeviceId, str, mtpDeviceRecord.operationsSupported, new MtpObjectInfo.Builder(mtpObjectInfoBuild2).setObjectHandle(iCreateDocument).build(), 0L);
                    getDocumentLoader(identifierCreateIdentifier).cancelTask(identifierCreateIdentifier);
                    notifyChildDocumentsChange(str);
                    return strPutNewDocument;
                } finally {
                    parcelFileDescriptorArrCreateReliablePipe[1].close();
                }
            } catch (FileNotFoundException | RuntimeException e2) {
                Log.e("MtpDocumentsProvider", "createDocument", e2);
                throw e2;
            }
        } catch (IOException e3) {
            Log.e("MtpDocumentsProvider", "createDocument", e3);
            throw new IllegalStateException(e3);
        }
    }

    @Override
    public DocumentsContract.Path findDocumentPath(String str, String str2) throws FileNotFoundException {
        LinkedList linkedList = new LinkedList();
        Identifier identifierCreateIdentifier = this.mDatabase.createIdentifier(str2);
        while (true) {
            if (identifierCreateIdentifier.mDocumentId.equals(str)) {
                linkedList.addFirst(identifierCreateIdentifier.mDocumentId);
            } else {
                switch (identifierCreateIdentifier.mDocumentType) {
                    case 0:
                        linkedList.addFirst(identifierCreateIdentifier.mDocumentId);
                        break;
                    case 1:
                        Identifier parentIdentifier = this.mDatabase.getParentIdentifier(identifierCreateIdentifier.mDocumentId);
                        if (this.mDatabase.getStorageDocumentIds(parentIdentifier.mDocumentId).length <= 1) {
                            identifierCreateIdentifier = parentIdentifier;
                        } else {
                            linkedList.addFirst(identifierCreateIdentifier.mDocumentId);
                        }
                        break;
                    case 2:
                        linkedList.addFirst(identifierCreateIdentifier.mDocumentId);
                        identifierCreateIdentifier = this.mDatabase.getParentIdentifier(identifierCreateIdentifier.mDocumentId);
                        continue;
                }
            }
        }
        if (str != null) {
            return new DocumentsContract.Path(null, linkedList);
        }
        return new DocumentsContract.Path(identifierCreateIdentifier.mDocumentId, linkedList);
    }

    @Override
    public boolean isChildDocument(String str, String str2) {
        try {
            Identifier identifierCreateIdentifier = this.mDatabase.createIdentifier(str2);
            while (!str.equals(identifierCreateIdentifier.mDocumentId)) {
                if (identifierCreateIdentifier.mDocumentType == 0) {
                    return false;
                }
                identifierCreateIdentifier = this.mDatabase.getParentIdentifier(identifierCreateIdentifier.mDocumentId);
            }
            return true;
        } catch (FileNotFoundException e) {
            return false;
        }
    }

    @Override
    public Bundle getDocumentMetadata(String str) throws Throwable {
        ParcelFileDescriptor.AutoCloseInputStream autoCloseInputStream;
        String documentType = getDocumentType(str);
        if (!MetadataReader.isSupportedMimeType(documentType)) {
            return null;
        }
        try {
            autoCloseInputStream = new ParcelFileDescriptor.AutoCloseInputStream(openDocument(str, "r", null));
            try {
                try {
                    Bundle bundle = new Bundle();
                    MetadataReader.getMetadata(bundle, autoCloseInputStream, documentType, (String[]) null);
                    IoUtils.closeQuietly(autoCloseInputStream);
                    return bundle;
                } catch (IOException e) {
                    e = e;
                    Log.e("MtpDocumentsProvider", "An error occurred retrieving the metadata", e);
                    IoUtils.closeQuietly(autoCloseInputStream);
                    return null;
                }
            } catch (Throwable th) {
                th = th;
                IoUtils.closeQuietly(autoCloseInputStream);
                throw th;
            }
        } catch (IOException e2) {
            e = e2;
            autoCloseInputStream = null;
        } catch (Throwable th2) {
            th = th2;
            autoCloseInputStream = null;
            IoUtils.closeQuietly(autoCloseInputStream);
            throw th;
        }
    }

    void openDevice(int i) throws IOException {
        synchronized (this.mDeviceListLock) {
            if (this.mDeviceToolkits.containsKey(Integer.valueOf(i))) {
                return;
            }
            DeviceToolkit deviceToolkit = new DeviceToolkit(this.mMtpManager, this.mResolver, this.mDatabase, this.mMtpManager.openDevice(i));
            this.mDeviceToolkits.put(Integer.valueOf(i), deviceToolkit);
            this.mIntentSender.sendUpdateNotificationIntent(getOpenedDeviceRecordsCache());
            try {
                this.mRootScanner.resume().await();
            } catch (InterruptedException e) {
                Log.e("MtpDocumentsProvider", "openDevice", e);
            }
            deviceToolkit.mDocumentLoader.resume();
        }
    }

    void closeDevice(int i) throws InterruptedException, IOException {
        synchronized (this.mDeviceListLock) {
            closeDeviceInternal(i);
            this.mIntentSender.sendUpdateNotificationIntent(getOpenedDeviceRecordsCache());
        }
        this.mRootScanner.resume();
    }

    MtpDeviceRecord[] getOpenedDeviceRecordsCache() {
        MtpDeviceRecord[] mtpDeviceRecordArr;
        synchronized (this.mDeviceListLock) {
            mtpDeviceRecordArr = new MtpDeviceRecord[this.mDeviceToolkits.size()];
            int i = 0;
            Iterator<DeviceToolkit> it = this.mDeviceToolkits.values().iterator();
            while (it.hasNext()) {
                mtpDeviceRecordArr[i] = it.next().mDeviceRecord;
                i++;
            }
        }
        return mtpDeviceRecordArr;
    }

    public String getDeviceDocumentId(int i) throws FileNotFoundException {
        return this.mDatabase.getDeviceDocumentId(i);
    }

    void resumeRootScanner() {
        this.mRootScanner.resume();
    }

    @Override
    public void shutdown() {
        synchronized (this.mDeviceListLock) {
            try {
                try {
                    for (Integer num : (Integer[]) this.mDeviceToolkits.keySet().toArray(new Integer[this.mDeviceToolkits.size()])) {
                        closeDeviceInternal(num.intValue());
                    }
                    this.mRootScanner.pause();
                } catch (IOException | InterruptedException | TimeoutException e) {
                    throw new RuntimeException(e);
                }
            } finally {
                this.mDatabase.close();
                super.shutdown();
            }
        }
    }

    private void notifyChildDocumentsChange(String str) {
        this.mResolver.notifyChange(DocumentsContract.buildChildDocumentsUri("com.android.mtp.documents", str), (ContentObserver) null, false);
    }

    private void resume() {
        synchronized (this.mDeviceListLock) {
            this.mDatabase.getMapper().clearMapping();
        }
    }

    private void closeDeviceInternal(int i) throws InterruptedException, IOException {
        if (!this.mDeviceToolkits.containsKey(Integer.valueOf(i))) {
            return;
        }
        getDeviceToolkit(i).close();
        this.mDeviceToolkits.remove(Integer.valueOf(i));
        this.mMtpManager.closeDevice(i);
    }

    private DeviceToolkit getDeviceToolkit(int i) throws FileNotFoundException {
        DeviceToolkit deviceToolkit;
        synchronized (this.mDeviceListLock) {
            deviceToolkit = this.mDeviceToolkits.get(Integer.valueOf(i));
            if (deviceToolkit == null) {
                throw new FileNotFoundException();
            }
        }
        return deviceToolkit;
    }

    private PipeManager getPipeManager(Identifier identifier) throws FileNotFoundException {
        return getDeviceToolkit(identifier.mDeviceId).mPipeManager;
    }

    private DocumentLoader getDocumentLoader(Identifier identifier) throws FileNotFoundException {
        return getDeviceToolkit(identifier.mDeviceId).mDocumentLoader;
    }

    private long getFileSize(String str) throws FileNotFoundException {
        Cursor cursorQueryDocument = this.mDatabase.queryDocument(str, MtpDatabase.strings("_size", "_display_name"));
        try {
            if (cursorQueryDocument.moveToNext()) {
                if (cursorQueryDocument.isNull(0)) {
                    throw new UnsupportedOperationException();
                }
                return cursorQueryDocument.getLong(0);
            }
            throw new FileNotFoundException();
        } finally {
            cursorQueryDocument.close();
        }
    }

    private Cursor createErrorCursor(String[] strArr, int i) {
        Bundle bundle = new Bundle();
        bundle.putString("error", this.mResources.getString(i));
        MatrixCursor matrixCursor = new MatrixCursor(strArr);
        matrixCursor.setExtras(bundle);
        return matrixCursor;
    }

    private static class DeviceToolkit implements AutoCloseable {
        public final MtpDeviceRecord mDeviceRecord;
        public final DocumentLoader mDocumentLoader;
        public final PipeManager mPipeManager;

        public DeviceToolkit(MtpManager mtpManager, ContentResolver contentResolver, MtpDatabase mtpDatabase, MtpDeviceRecord mtpDeviceRecord) {
            this.mPipeManager = new PipeManager(mtpDatabase);
            this.mDocumentLoader = new DocumentLoader(mtpDeviceRecord, mtpManager, contentResolver, mtpDatabase);
            this.mDeviceRecord = mtpDeviceRecord;
        }

        @Override
        public void close() throws InterruptedException {
            this.mPipeManager.close();
            this.mDocumentLoader.close();
        }
    }

    private class MtpProxyFileDescriptorCallback extends ProxyFileDescriptorCallback {
        private final int mInode;
        private MtpFileWriter mWriter;

        MtpProxyFileDescriptorCallback(int i) {
            this.mInode = i;
        }

        @Override
        public long onGetSize() throws ErrnoException {
            try {
                return MtpDocumentsProvider.this.getFileSize(String.valueOf(this.mInode));
            } catch (FileNotFoundException e) {
                Log.e("MtpDocumentsProvider", e.getMessage(), e);
                throw new ErrnoException("onGetSize", OsConstants.ENOENT);
            }
        }

        @Override
        public int onRead(long j, int i, byte[] bArr) throws ErrnoException {
            try {
                Identifier identifierCreateIdentifier = MtpDocumentsProvider.this.mDatabase.createIdentifier(Integer.toString(this.mInode));
                MtpDeviceRecord mtpDeviceRecord = MtpDocumentsProvider.this.getDeviceToolkit(identifierCreateIdentifier.mDeviceId).mDeviceRecord;
                if (MtpDeviceRecord.isSupported(mtpDeviceRecord.operationsSupported, 38337)) {
                    return (int) MtpDocumentsProvider.this.mMtpManager.getPartialObject64(identifierCreateIdentifier.mDeviceId, identifierCreateIdentifier.mObjectHandle, j, i, bArr);
                }
                if (0 <= j && j <= 4294967295L && MtpDeviceRecord.isSupported(mtpDeviceRecord.operationsSupported, 4123)) {
                    return (int) MtpDocumentsProvider.this.mMtpManager.getPartialObject(identifierCreateIdentifier.mDeviceId, identifierCreateIdentifier.mObjectHandle, j, i, bArr);
                }
                throw new ErrnoException("onRead", OsConstants.ENOTSUP);
            } catch (IOException e) {
                Log.e("MtpDocumentsProvider", e.getMessage(), e);
                throw new ErrnoException("onRead", OsConstants.EIO);
            }
        }

        @Override
        public int onWrite(long j, int i, byte[] bArr) throws ErrnoException {
            try {
                if (this.mWriter == null) {
                    this.mWriter = new MtpFileWriter(MtpDocumentsProvider.this.mContext, String.valueOf(this.mInode));
                }
                return this.mWriter.write(j, i, bArr);
            } catch (IOException e) {
                Log.e("MtpDocumentsProvider", e.getMessage(), e);
                throw new ErrnoException("onWrite", OsConstants.EIO);
            }
        }

        @Override
        public void onFsync() throws ErrnoException {
            tryFsync();
        }

        @Override
        public void onRelease() {
            try {
                try {
                    tryFsync();
                    if (this.mWriter == null) {
                        return;
                    }
                } catch (ErrnoException e) {
                    Log.e("MtpDocumentsProvider", "Cannot recover from the error at onRelease.", e);
                    if (this.mWriter == null) {
                        return;
                    }
                }
                IoUtils.closeQuietly(this.mWriter);
            } catch (Throwable th) {
                if (this.mWriter != null) {
                    IoUtils.closeQuietly(this.mWriter);
                }
                throw th;
            }
        }

        private void tryFsync() throws ErrnoException {
            try {
                if (this.mWriter != null) {
                    this.mWriter.flush(MtpDocumentsProvider.this.mMtpManager, MtpDocumentsProvider.this.mDatabase, MtpDocumentsProvider.this.getDeviceToolkit(MtpDocumentsProvider.this.mDatabase.createIdentifier(this.mWriter.getDocumentId()).mDeviceId).mDeviceRecord.operationsSupported);
                }
            } catch (IOException e) {
                Log.e("MtpDocumentsProvider", e.getMessage(), e);
                throw new ErrnoException("onWrite", OsConstants.EIO);
            }
        }
    }
}
