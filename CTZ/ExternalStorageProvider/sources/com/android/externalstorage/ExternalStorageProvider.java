package com.android.externalstorage;

import android.app.usage.StorageStatsManager;
import android.content.Context;
import android.content.UriPermission;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.DiskInfo;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.DebugUtils;
import android.util.Log;
import android.util.Pair;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.content.FileSystemProvider;
import com.android.internal.util.IndentingPrintWriter;
import com.mediatek.internal.content.FileSystemProviderExt;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ExternalStorageProvider extends FileSystemProvider {
    private static FileSystemProviderExt sFileSystemProviderExt;
    private String[] mDefaultProjection;
    private StorageManager mStorageManager;
    private UserManager mUserManager;
    private static final Uri BASE_URI = new Uri.Builder().scheme("content").authority("com.android.externalstorage.documents").build();
    private static final String[] DEFAULT_ROOT_PROJECTION = {"root_id", "flags", "icon", "title", "document_id", "available_bytes"};
    private static final String[] DEFAULT_DOCUMENT_PROJECTION = {"document_id", "mime_type", "_display_name", "last_modified", "flags", "_size"};
    private final Object mRootsLock = new Object();

    @GuardedBy("mRootsLock")
    private ArrayMap<String, RootInfo> mRoots = new ArrayMap<>();

    private static class RootInfo {
        public String docId;
        public int flags;
        public File path;
        public boolean reportAvailableBytes;
        public String rootId;
        public UUID storageUuid;
        public String title;
        public File visiblePath;
        public String volumeId;

        private RootInfo() {
            this.reportAvailableBytes = true;
        }
    }

    public boolean onCreate() {
        Context context = getContext();
        FileSystemProviderExt fileSystemProviderExt = sFileSystemProviderExt;
        sFileSystemProviderExt = FileSystemProviderExt.getInstance(context);
        this.mDefaultProjection = sFileSystemProviderExt.resolveProjection(DEFAULT_DOCUMENT_PROJECTION);
        super.onCreate(this.mDefaultProjection);
        this.mStorageManager = (StorageManager) getContext().getSystemService(StorageManager.class);
        this.mUserManager = (UserManager) getContext().getSystemService(UserManager.class);
        updateVolumes();
        return true;
    }

    private void enforceShellRestrictions() {
        if (UserHandle.getCallingAppId() == 2000 && this.mUserManager.hasUserRestriction("no_usb_file_transfer")) {
            throw new SecurityException("Shell user cannot access files for user " + UserHandle.myUserId());
        }
    }

    protected int enforceReadPermissionInner(Uri uri, String str, IBinder iBinder) throws SecurityException {
        enforceShellRestrictions();
        return super.enforceReadPermissionInner(uri, str, iBinder);
    }

    protected int enforceWritePermissionInner(Uri uri, String str, IBinder iBinder) throws SecurityException {
        enforceShellRestrictions();
        return super.enforceWritePermissionInner(uri, str, iBinder);
    }

    public void updateVolumes() {
        synchronized (this.mRootsLock) {
            updateVolumesLocked();
        }
    }

    private void updateVolumesLocked() {
        String fsUuid;
        String bestVolumeDescription;
        UUID uuidConvert;
        this.mRoots.clear();
        int iMyUserId = UserHandle.myUserId();
        VolumeInfo volumeInfo = null;
        for (VolumeInfo volumeInfo2 : this.mStorageManager.getVolumes()) {
            if (volumeInfo2.isMountedReadable()) {
                if (volumeInfo2.getType() == 2) {
                    fsUuid = "primary";
                    if ("emulated".equals(volumeInfo2.getId())) {
                        String string = Settings.Global.getString(getContext().getContentResolver(), "device_name");
                        if (TextUtils.isEmpty(string)) {
                            string = getContext().getString(R.string.root_internal_storage);
                        }
                        bestVolumeDescription = string;
                        uuidConvert = StorageManager.UUID_DEFAULT;
                    } else {
                        VolumeInfo volumeInfoFindPrivateForEmulated = this.mStorageManager.findPrivateForEmulated(volumeInfo2);
                        bestVolumeDescription = this.mStorageManager.getBestVolumeDescription(volumeInfoFindPrivateForEmulated);
                        uuidConvert = StorageManager.convert(volumeInfoFindPrivateForEmulated.fsUuid);
                    }
                } else if (volumeInfo2.getType() == 0 && volumeInfo2.getMountUserId() == iMyUserId) {
                    fsUuid = volumeInfo2.getFsUuid();
                    bestVolumeDescription = this.mStorageManager.getBestVolumeDescription(volumeInfo2);
                    uuidConvert = null;
                }
                if (TextUtils.isEmpty(fsUuid)) {
                    Log.d("ExternalStorage", "Missing UUID for " + volumeInfo2.getId() + "; skipping");
                } else if (this.mRoots.containsKey(fsUuid)) {
                    Log.w("ExternalStorage", "Duplicate UUID " + fsUuid + " for " + volumeInfo2.getId() + "; skipping");
                } else {
                    RootInfo rootInfo = new RootInfo();
                    this.mRoots.put(fsUuid, rootInfo);
                    rootInfo.rootId = fsUuid;
                    rootInfo.volumeId = volumeInfo2.id;
                    rootInfo.storageUuid = uuidConvert;
                    rootInfo.flags = 26;
                    DiskInfo disk = volumeInfo2.getDisk();
                    if (disk != null && disk.isSd()) {
                        rootInfo.flags |= 524288;
                    } else if (disk != null && disk.isUsb()) {
                        rootInfo.flags |= 1048576;
                    }
                    if (!"emulated".equals(volumeInfo2.getId())) {
                        rootInfo.flags |= 32;
                    }
                    if (volumeInfo2.isPrimary()) {
                        rootInfo.flags |= 131072;
                        volumeInfo = volumeInfo2;
                    }
                    if (volumeInfo2.isMountedWritable()) {
                        rootInfo.flags |= 1;
                    }
                    rootInfo.title = bestVolumeDescription;
                    if (volumeInfo2.getType() == 0) {
                        rootInfo.flags |= 262144;
                    }
                    if (volumeInfo2.isVisibleForRead(iMyUserId)) {
                        rootInfo.visiblePath = volumeInfo2.getPathForUser(iMyUserId);
                    } else {
                        rootInfo.visiblePath = null;
                    }
                    rootInfo.path = volumeInfo2.getInternalPathForUser(iMyUserId);
                    try {
                        rootInfo.docId = getDocIdForFile(rootInfo.path);
                    } catch (FileNotFoundException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }
        }
        if (volumeInfo != null && volumeInfo.isVisible()) {
            RootInfo rootInfo2 = new RootInfo();
            rootInfo2.rootId = "home";
            this.mRoots.put(rootInfo2.rootId, rootInfo2);
            rootInfo2.title = getContext().getString(R.string.root_documents);
            rootInfo2.reportAvailableBytes = false;
            rootInfo2.flags = 26;
            if (volumeInfo.isMountedWritable()) {
                rootInfo2.flags |= 1;
            }
            rootInfo2.visiblePath = new File(volumeInfo.getPathForUser(iMyUserId), Environment.DIRECTORY_DOCUMENTS);
            rootInfo2.path = new File(volumeInfo.getInternalPathForUser(iMyUserId), Environment.DIRECTORY_DOCUMENTS);
            try {
                rootInfo2.docId = getDocIdForFile(rootInfo2.path);
            } catch (FileNotFoundException e2) {
                throw new IllegalStateException(e2);
            }
        }
        Log.d("ExternalStorage", "After updating volumes, found " + this.mRoots.size() + " active roots");
        getContext().getContentResolver().notifyChange(BASE_URI, (ContentObserver) null, false);
    }

    private static String[] resolveRootProjection(String[] strArr) {
        return strArr != null ? strArr : DEFAULT_ROOT_PROJECTION;
    }

    protected String getDocIdForFile(File file) throws FileNotFoundException {
        return getDocIdForFileMaybeCreate(file, false);
    }

    private String getDocIdForFileMaybeCreate(File file, boolean z) throws FileNotFoundException {
        String absolutePath;
        String strSubstring;
        String absolutePath2 = file.getAbsolutePath();
        boolean z2 = false;
        RootInfo mostSpecificRootForPath = getMostSpecificRootForPath(absolutePath2, false);
        if (mostSpecificRootForPath == null) {
            mostSpecificRootForPath = getMostSpecificRootForPath(absolutePath2, true);
            z2 = true;
        }
        if (mostSpecificRootForPath == null) {
            throw new FileNotFoundException("Failed to find root that contains " + absolutePath2);
        }
        if (z2) {
            absolutePath = mostSpecificRootForPath.visiblePath.getAbsolutePath();
        } else {
            absolutePath = mostSpecificRootForPath.path.getAbsolutePath();
        }
        if (absolutePath.equals(absolutePath2)) {
            strSubstring = "";
        } else if (!absolutePath.endsWith("/")) {
            strSubstring = absolutePath2.substring(absolutePath.length() + 1);
        } else {
            strSubstring = absolutePath2.substring(absolutePath.length());
        }
        if (!file.exists() && z) {
            Log.i("ExternalStorage", "Creating new directory " + file);
            if (!file.mkdir()) {
                Log.e("ExternalStorage", "Could not create directory " + file);
            }
        }
        return mostSpecificRootForPath.rootId + ':' + strSubstring;
    }

    private RootInfo getMostSpecificRootForPath(String str, boolean z) {
        RootInfo rootInfo;
        synchronized (this.mRootsLock) {
            rootInfo = null;
            String str2 = null;
            for (int i = 0; i < this.mRoots.size(); i++) {
                RootInfo rootInfoValueAt = this.mRoots.valueAt(i);
                File file = z ? rootInfoValueAt.visiblePath : rootInfoValueAt.path;
                if (file != null) {
                    String absolutePath = file.getAbsolutePath();
                    if (str.startsWith(absolutePath) && (str2 == null || absolutePath.length() > str2.length())) {
                        rootInfo = rootInfoValueAt;
                        str2 = absolutePath;
                    }
                }
            }
        }
        return rootInfo;
    }

    protected File getFileForDocId(String str, boolean z) throws FileNotFoundException {
        return getFileForDocId(str, z, true);
    }

    private File getFileForDocId(String str, boolean z, boolean z2) throws FileNotFoundException {
        return buildFile(getRootFromDocId(str), str, z, z2);
    }

    private Pair<RootInfo, File> resolveDocId(String str, boolean z) throws FileNotFoundException {
        RootInfo rootFromDocId = getRootFromDocId(str);
        return Pair.create(rootFromDocId, buildFile(rootFromDocId, str, z, true));
    }

    private RootInfo getRootFromDocId(String str) throws FileNotFoundException {
        RootInfo rootInfo;
        String strSubstring = str.substring(0, str.indexOf(58, 1));
        synchronized (this.mRootsLock) {
            rootInfo = this.mRoots.get(strSubstring);
        }
        if (rootInfo == null) {
            throw new FileNotFoundException("No root for " + strSubstring);
        }
        return rootInfo;
    }

    private File buildFile(RootInfo rootInfo, String str, boolean z, boolean z2) throws FileNotFoundException {
        String strSubstring = str.substring(str.indexOf(58, 1) + 1);
        File file = z ? rootInfo.visiblePath : rootInfo.path;
        if (file == null) {
            return null;
        }
        if (!file.exists()) {
            file.mkdirs();
        }
        File file2 = new File(file, strSubstring);
        if (z2 && !file2.exists()) {
            throw new FileNotFoundException("Missing file for " + str + " at " + file2);
        }
        return file2;
    }

    protected Uri buildNotificationUri(String str) {
        return DocumentsContract.buildChildDocumentsUri("com.android.externalstorage.documents", str);
    }

    protected void onDocIdChanged(String str) {
        try {
            File fileForDocId = getFileForDocId(str, true, false);
            if (fileForDocId != null) {
                Os.access(fileForDocId.getAbsolutePath(), OsConstants.F_OK);
            }
        } catch (ErrnoException | FileNotFoundException e) {
        }
    }

    protected void onDocIdDeleted(String str) {
        getContext().revokeUriPermission(DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", str), -1);
    }

    public Cursor queryRoots(String[] strArr) throws IOException {
        MatrixCursor matrixCursor = new MatrixCursor(resolveRootProjection(strArr));
        synchronized (this.mRootsLock) {
            for (RootInfo rootInfo : this.mRoots.values()) {
                MatrixCursor.RowBuilder rowBuilderNewRow = matrixCursor.newRow();
                rowBuilderNewRow.add("root_id", rootInfo.rootId);
                rowBuilderNewRow.add("flags", Integer.valueOf(rootInfo.flags));
                rowBuilderNewRow.add("title", rootInfo.title);
                rowBuilderNewRow.add("document_id", rootInfo.docId);
                long freeBytes = -1;
                if (rootInfo.reportAvailableBytes) {
                    if (rootInfo.storageUuid != null) {
                        try {
                            freeBytes = ((StorageStatsManager) getContext().getSystemService(StorageStatsManager.class)).getFreeBytes(rootInfo.storageUuid);
                        } catch (IOException e) {
                            Log.w("ExternalStorage", e);
                        }
                    } else {
                        freeBytes = rootInfo.path.getUsableSpace();
                    }
                }
                rowBuilderNewRow.add("available_bytes", Long.valueOf(freeBytes));
            }
        }
        return matrixCursor;
    }

    public DocumentsContract.Path findDocumentPath(String str, String str2) throws FileNotFoundException {
        File fileForDocId;
        Pair<RootInfo, File> pairResolveDocId = resolveDocId(str2, false);
        RootInfo rootInfo = (RootInfo) pairResolveDocId.first;
        File file = (File) pairResolveDocId.second;
        if (TextUtils.isEmpty(str)) {
            fileForDocId = rootInfo.path;
        } else {
            fileForDocId = getFileForDocId(str);
        }
        return new DocumentsContract.Path(str == null ? rootInfo.rootId : null, findDocumentPath(fileForDocId, file));
    }

    private Uri getDocumentUri(String str, List<UriPermission> list) throws FileNotFoundException {
        String docIdForFile = getDocIdForFile(new File(str));
        UriPermission uriPermission = null;
        UriPermission uriPermission2 = null;
        for (UriPermission uriPermission3 : list) {
            Uri uri = uriPermission3.getUri();
            if ("com.android.externalstorage.documents".equals(uri.getAuthority())) {
                boolean z = false;
                if (DocumentsContract.isTreeUri(uri)) {
                    if (isChildDocument(DocumentsContract.getTreeDocumentId(uri), docIdForFile)) {
                        uriPermission2 = uriPermission3;
                        z = true;
                    }
                } else if (Objects.equals(docIdForFile, DocumentsContract.getDocumentId(uri))) {
                    uriPermission = uriPermission3;
                    z = true;
                }
                if (z && allowsBothReadAndWrite(uriPermission3)) {
                    break;
                }
            }
        }
        if (allowsBothReadAndWrite(uriPermission2)) {
            return DocumentsContract.buildDocumentUriUsingTree(uriPermission2.getUri(), docIdForFile);
        }
        if (allowsBothReadAndWrite(uriPermission)) {
            return uriPermission.getUri();
        }
        if (uriPermission2 != null) {
            return DocumentsContract.buildDocumentUriUsingTree(uriPermission2.getUri(), docIdForFile);
        }
        if (uriPermission != null) {
            return uriPermission.getUri();
        }
        throw new SecurityException("The app is not given any access to the document under path " + str + " with permissions granted in " + list);
    }

    private static boolean allowsBothReadAndWrite(UriPermission uriPermission) {
        return uriPermission != null && uriPermission.isReadPermission() && uriPermission.isWritePermission();
    }

    public Cursor querySearchDocuments(String str, String str2, String[] strArr) throws FileNotFoundException {
        File file;
        synchronized (this.mRootsLock) {
            file = this.mRoots.get(str).path;
        }
        return querySearchDocuments(file, str2, strArr, Collections.emptySet());
    }

    public void ejectRoot(String str) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        RootInfo rootInfo = this.mRoots.get(str);
        try {
            if (rootInfo != null) {
                try {
                    this.mStorageManager.unmount(rootInfo.volumeId);
                } catch (RuntimeException e) {
                    throw new IllegalStateException(e);
                }
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        IndentingPrintWriter indentingPrintWriter = new IndentingPrintWriter(printWriter, "  ", 160);
        synchronized (this.mRootsLock) {
            for (int i = 0; i < this.mRoots.size(); i++) {
                RootInfo rootInfoValueAt = this.mRoots.valueAt(i);
                indentingPrintWriter.println("Root{" + rootInfoValueAt.rootId + "}:");
                indentingPrintWriter.increaseIndent();
                indentingPrintWriter.printPair("flags", DebugUtils.flagsToString(DocumentsContract.Root.class, "FLAG_", rootInfoValueAt.flags));
                indentingPrintWriter.println();
                indentingPrintWriter.printPair("title", rootInfoValueAt.title);
                indentingPrintWriter.printPair("docId", rootInfoValueAt.docId);
                indentingPrintWriter.println();
                indentingPrintWriter.printPair("path", rootInfoValueAt.path);
                indentingPrintWriter.printPair("visiblePath", rootInfoValueAt.visiblePath);
                indentingPrintWriter.decreaseIndent();
                indentingPrintWriter.println();
            }
        }
    }

    public Bundle call(String str, String str2, Bundle bundle) {
        Bundle bundleCall = super.call(str, str2, bundle);
        if (bundleCall == null && !TextUtils.isEmpty(str)) {
            byte b = -1;
            int iHashCode = str.hashCode();
            if (iHashCode != -1873611919) {
                if (iHashCode == -1112688756 && str.equals("getDocumentId")) {
                    b = 1;
                }
            } else if (str.equals("getDocIdForFileCreateNewDir")) {
                b = 0;
            }
            switch (b) {
                case 0:
                    getContext().enforceCallingPermission("android.permission.MANAGE_DOCUMENTS", null);
                    if (TextUtils.isEmpty(str2)) {
                        return null;
                    }
                    try {
                        String docIdForFileMaybeCreate = getDocIdForFileMaybeCreate(new File(str2), true);
                        Bundle bundle2 = new Bundle();
                        bundle2.putString("DOC_ID", docIdForFileMaybeCreate);
                        return bundle2;
                    } catch (FileNotFoundException e) {
                        Log.w("ExternalStorage", "file '" + str2 + "' not found");
                        return null;
                    }
                case 1:
                    ArrayList parcelableArrayList = bundle.getParcelableArrayList("com.android.externalstorage.documents.extra.uriPermissions");
                    try {
                        Bundle bundle3 = new Bundle();
                        bundle3.putParcelable("uri", getDocumentUri(str2, parcelableArrayList));
                        return bundle3;
                    } catch (FileNotFoundException e2) {
                        throw new IllegalStateException("File in " + str2 + " is not found.", e2);
                    }
                default:
                    Log.w("ExternalStorage", "unknown method passed to call(): " + str);
                    break;
            }
        }
        return bundleCall;
    }
}
