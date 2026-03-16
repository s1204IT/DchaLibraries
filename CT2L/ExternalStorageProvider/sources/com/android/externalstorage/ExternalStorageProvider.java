package com.android.externalstorage;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Point;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.FileObserver;
import android.os.FileUtils;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.DocumentsContract;
import android.provider.DocumentsProvider;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;
import com.android.internal.annotations.GuardedBy;
import com.google.android.collect.Lists;
import com.google.android.collect.Maps;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;

public class ExternalStorageProvider extends DocumentsProvider {
    private Handler mHandler;

    @GuardedBy("mRootsLock")
    private HashMap<String, File> mIdToPath;

    @GuardedBy("mRootsLock")
    private HashMap<String, RootInfo> mIdToRoot;

    @GuardedBy("mRootsLock")
    private ArrayList<RootInfo> mRoots;
    private StorageManager mStorageManager;
    private static final String[] DEFAULT_ROOT_PROJECTION = {"root_id", "flags", "icon", "title", "document_id", "available_bytes"};
    private static final String[] DEFAULT_DOCUMENT_PROJECTION = {"document_id", "mime_type", "_display_name", "last_modified", "flags", "_size"};
    private final Object mRootsLock = new Object();

    @GuardedBy("mObservers")
    private Map<File, DirectoryObserver> mObservers = Maps.newHashMap();

    private static class RootInfo {
        public String docId;
        public int flags;
        public String rootId;
        public String title;

        private RootInfo() {
        }
    }

    @Override
    public boolean onCreate() {
        this.mStorageManager = (StorageManager) getContext().getSystemService("storage");
        this.mHandler = new Handler();
        this.mRoots = Lists.newArrayList();
        this.mIdToRoot = Maps.newHashMap();
        this.mIdToPath = Maps.newHashMap();
        updateVolumes();
        return true;
    }

    public void updateVolumes() {
        synchronized (this.mRootsLock) {
            updateVolumesLocked();
        }
    }

    private void updateVolumesLocked() {
        String rootId;
        this.mRoots.clear();
        this.mIdToPath.clear();
        this.mIdToRoot.clear();
        StorageVolume[] volumes = this.mStorageManager.getVolumeList();
        for (StorageVolume volume : volumes) {
            boolean mounted = "mounted".equals(volume.getState()) || "mounted_ro".equals(volume.getState());
            if (mounted) {
                if (volume.isPrimary() && volume.isEmulated()) {
                    rootId = "primary";
                } else if (volume.getUuid() != null) {
                    rootId = volume.getUuid();
                } else {
                    Log.d("ExternalStorage", "Missing UUID for " + volume.getPath() + "; skipping");
                }
                if (this.mIdToPath.containsKey(rootId)) {
                    Log.w("ExternalStorage", "Duplicate UUID " + rootId + "; skipping");
                } else {
                    try {
                        File path = volume.getPathFile();
                        this.mIdToPath.put(rootId, path);
                        RootInfo root = new RootInfo();
                        root.rootId = rootId;
                        root.flags = 131099;
                        if ("primary".equals(rootId)) {
                            root.title = getContext().getString(R.string.root_internal_storage);
                        } else {
                            String userLabel = volume.getUserLabel();
                            if (!TextUtils.isEmpty(userLabel)) {
                                root.title = userLabel;
                            } else {
                                root.title = volume.getDescription(getContext());
                            }
                        }
                        root.docId = getDocIdForFile(path);
                        this.mRoots.add(root);
                        this.mIdToRoot.put(rootId, root);
                    } catch (FileNotFoundException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }
        }
        Log.d("ExternalStorage", "After updating volumes, found " + this.mRoots.size() + " active roots");
        getContext().getContentResolver().notifyChange(DocumentsContract.buildRootsUri("com.android.externalstorage.documents"), (ContentObserver) null, false);
    }

    private static String[] resolveRootProjection(String[] projection) {
        return projection != null ? projection : DEFAULT_ROOT_PROJECTION;
    }

    private static String[] resolveDocumentProjection(String[] projection) {
        return projection != null ? projection : DEFAULT_DOCUMENT_PROJECTION;
    }

    private String getDocIdForFile(File file) throws FileNotFoundException {
        String path;
        String path2 = file.getAbsolutePath();
        Map.Entry<String, File> mostSpecific = null;
        synchronized (this.mRootsLock) {
            for (Map.Entry<String, File> root : this.mIdToPath.entrySet()) {
                String rootPath = root.getValue().getPath();
                if (path2.startsWith(rootPath) && (mostSpecific == null || rootPath.length() > mostSpecific.getValue().getPath().length())) {
                    mostSpecific = root;
                }
            }
        }
        if (mostSpecific == null) {
            throw new FileNotFoundException("Failed to find root that contains " + path2);
        }
        String rootPath2 = mostSpecific.getValue().getPath();
        if (rootPath2.equals(path2)) {
            path = "";
        } else if (rootPath2.endsWith("/")) {
            path = path2.substring(rootPath2.length());
        } else {
            path = path2.substring(rootPath2.length() + 1);
        }
        return mostSpecific.getKey() + ':' + path;
    }

    private File getFileForDocId(String docId) throws FileNotFoundException {
        File target;
        int splitIndex = docId.indexOf(58, 1);
        String tag = docId.substring(0, splitIndex);
        String path = docId.substring(splitIndex + 1);
        synchronized (this.mRootsLock) {
            target = this.mIdToPath.get(tag);
        }
        if (target == null) {
            throw new FileNotFoundException("No root for " + tag);
        }
        if (!target.exists()) {
            target.mkdirs();
        }
        File target2 = new File(target, path);
        if (!target2.exists()) {
            throw new FileNotFoundException("Missing file for " + docId + " at " + target2);
        }
        return target2;
    }

    private void includeFile(MatrixCursor result, String docId, File file) throws FileNotFoundException {
        if (docId == null) {
            docId = getDocIdForFile(file);
        } else {
            file = getFileForDocId(docId);
        }
        int flags = 0;
        if (file.canWrite()) {
            if (file.isDirectory()) {
                int flags2 = 0 | 8;
                flags = flags2 | 4 | 64;
            } else {
                int flags3 = 0 | 2;
                flags = flags3 | 4 | 64;
            }
        }
        String displayName = file.getName();
        String mimeType = getTypeForFile(file);
        if (mimeType.startsWith("image/")) {
            flags |= 1;
        }
        MatrixCursor.RowBuilder row = result.newRow();
        row.add("document_id", docId);
        row.add("_display_name", displayName);
        row.add("_size", Long.valueOf(file.length()));
        row.add("mime_type", mimeType);
        row.add("flags", Integer.valueOf(flags));
        long lastModified = file.lastModified();
        if (lastModified > 31536000000L) {
            row.add("last_modified", Long.valueOf(lastModified));
        }
    }

    @Override
    public Cursor queryRoots(String[] projection) throws FileNotFoundException {
        MatrixCursor result = new MatrixCursor(resolveRootProjection(projection));
        synchronized (this.mRootsLock) {
            for (String rootId : this.mIdToPath.keySet()) {
                RootInfo root = this.mIdToRoot.get(rootId);
                File path = this.mIdToPath.get(rootId);
                MatrixCursor.RowBuilder row = result.newRow();
                row.add("root_id", root.rootId);
                row.add("flags", Integer.valueOf(root.flags));
                row.add("title", root.title);
                row.add("document_id", root.docId);
                row.add("available_bytes", Long.valueOf(path.getFreeSpace()));
            }
        }
        return result;
    }

    @Override
    public boolean isChildDocument(String parentDocId, String docId) {
        try {
            File parent = getFileForDocId(parentDocId).getCanonicalFile();
            File doc = getFileForDocId(docId).getCanonicalFile();
            return FileUtils.contains(parent, doc);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to determine if " + docId + " is child of " + parentDocId + ": " + e);
        }
    }

    @Override
    public String createDocument(String docId, String mimeType, String displayName) throws FileNotFoundException {
        String displayName2 = FileUtils.buildValidFatFilename(displayName);
        File parent = getFileForDocId(docId);
        if (!parent.isDirectory()) {
            throw new IllegalArgumentException("Parent document isn't a directory");
        }
        File file = buildUniqueFile(parent, mimeType, displayName2);
        if ("vnd.android.document/directory".equals(mimeType)) {
            if (!file.mkdir()) {
                throw new IllegalStateException("Failed to mkdir " + file);
            }
        } else {
            try {
                if (!file.createNewFile()) {
                    throw new IllegalStateException("Failed to touch " + file);
                }
            } catch (IOException e) {
                throw new IllegalStateException("Failed to touch " + file + ": " + e);
            }
        }
        return getDocIdForFile(file);
    }

    private static File buildFile(File parent, String name, String ext) {
        return TextUtils.isEmpty(ext) ? new File(parent, name) : new File(parent, name + "." + ext);
    }

    public static File buildUniqueFile(File parent, String mimeType, String displayName) throws FileNotFoundException {
        String name;
        String ext;
        String mimeTypeFromExt;
        if ("vnd.android.document/directory".equals(mimeType)) {
            name = displayName;
            ext = null;
        } else {
            int lastDot = displayName.lastIndexOf(46);
            if (lastDot >= 0) {
                name = displayName.substring(0, lastDot);
                ext = displayName.substring(lastDot + 1);
                mimeTypeFromExt = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.toLowerCase());
            } else {
                name = displayName;
                ext = null;
                mimeTypeFromExt = null;
            }
            if (mimeTypeFromExt == null) {
                mimeTypeFromExt = "application/octet-stream";
            }
            String extFromMimeType = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
            if (!Objects.equals(mimeType, mimeTypeFromExt) && !Objects.equals(ext, extFromMimeType)) {
                name = displayName;
                ext = extFromMimeType;
            }
        }
        File file = buildFile(parent, name, ext);
        int n = 0;
        while (file.exists()) {
            int n2 = n + 1;
            if (n >= 32) {
                throw new FileNotFoundException("Failed to create unique file");
            }
            file = buildFile(parent, name + " (" + n2 + ")", ext);
            n = n2;
        }
        return file;
    }

    @Override
    public String renameDocument(String docId, String displayName) throws FileNotFoundException {
        String displayName2 = FileUtils.buildValidFatFilename(displayName);
        File before = getFileForDocId(docId);
        File after = new File(before.getParentFile(), displayName2);
        if (after.exists()) {
            throw new IllegalStateException("Already exists " + after);
        }
        if (!before.renameTo(after)) {
            throw new IllegalStateException("Failed to rename to " + after);
        }
        String afterDocId = getDocIdForFile(after);
        if (TextUtils.equals(docId, afterDocId)) {
            return null;
        }
        return afterDocId;
    }

    @Override
    public void deleteDocument(String docId) throws FileNotFoundException {
        File file = getFileForDocId(docId);
        if (file.isDirectory()) {
            FileUtils.deleteContents(file);
        }
        if (!file.delete()) {
            throw new IllegalStateException("Failed to delete " + file);
        }
    }

    @Override
    public Cursor queryDocument(String documentId, String[] projection) throws FileNotFoundException {
        MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));
        includeFile(result, documentId, null);
        return result;
    }

    @Override
    public Cursor queryChildDocuments(String parentDocumentId, String[] projection, String sortOrder) throws FileNotFoundException {
        File parent = getFileForDocId(parentDocumentId);
        MatrixCursor result = new DirectoryCursor(resolveDocumentProjection(projection), parentDocumentId, parent);
        File[] arr$ = parent.listFiles();
        for (File file : arr$) {
            includeFile(result, null, file);
        }
        return result;
    }

    @Override
    public Cursor querySearchDocuments(String rootId, String query, String[] projection) throws FileNotFoundException {
        File parent;
        MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));
        synchronized (this.mRootsLock) {
            parent = this.mIdToPath.get(rootId);
        }
        LinkedList<File> pending = new LinkedList<>();
        pending.add(parent);
        while (!pending.isEmpty() && result.getCount() < 24) {
            File file = pending.removeFirst();
            if (file.isDirectory()) {
                File[] arr$ = file.listFiles();
                for (File child : arr$) {
                    pending.add(child);
                }
            }
            if (file.getName().toLowerCase().contains(query)) {
                includeFile(result, null, file);
            }
        }
        return result;
    }

    @Override
    public String getDocumentType(String documentId) throws FileNotFoundException {
        File file = getFileForDocId(documentId);
        return getTypeForFile(file);
    }

    @Override
    public ParcelFileDescriptor openDocument(String documentId, String mode, CancellationSignal signal) throws FileNotFoundException {
        final File file = getFileForDocId(documentId);
        int pfdMode = ParcelFileDescriptor.parseMode(mode);
        if (pfdMode == 268435456) {
            return ParcelFileDescriptor.open(file, pfdMode);
        }
        try {
            return ParcelFileDescriptor.open(file, pfdMode, this.mHandler, new ParcelFileDescriptor.OnCloseListener() {
                @Override
                public void onClose(IOException e) {
                    Intent intent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
                    intent.setData(Uri.fromFile(file));
                    ExternalStorageProvider.this.getContext().sendBroadcast(intent);
                }
            });
        } catch (IOException e) {
            throw new FileNotFoundException("Failed to open for writing: " + e);
        }
    }

    @Override
    public AssetFileDescriptor openDocumentThumbnail(String documentId, Point sizeHint, CancellationSignal signal) throws FileNotFoundException {
        File file = getFileForDocId(documentId);
        return DocumentsContract.openImageThumbnail(file);
    }

    private static String getTypeForFile(File file) {
        return file.isDirectory() ? "vnd.android.document/directory" : getTypeForName(file.getName());
    }

    private static String getTypeForName(String name) {
        int lastDot = name.lastIndexOf(46);
        if (lastDot >= 0) {
            String extension = name.substring(lastDot + 1).toLowerCase();
            String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            if (mime != null) {
                return mime;
            }
        }
        return "application/octet-stream";
    }

    private void startObserving(File file, Uri notifyUri) {
        synchronized (this.mObservers) {
            DirectoryObserver observer = this.mObservers.get(file);
            if (observer == null) {
                observer = new DirectoryObserver(file, getContext().getContentResolver(), notifyUri);
                observer.startWatching();
                this.mObservers.put(file, observer);
            }
            DirectoryObserver.access$108(observer);
        }
    }

    private void stopObserving(File file) {
        synchronized (this.mObservers) {
            DirectoryObserver observer = this.mObservers.get(file);
            if (observer != null) {
                DirectoryObserver.access$110(observer);
                if (observer.mRefCount == 0) {
                    this.mObservers.remove(file);
                    observer.stopWatching();
                }
            }
        }
    }

    private static class DirectoryObserver extends FileObserver {
        private final File mFile;
        private final Uri mNotifyUri;
        private int mRefCount;
        private final ContentResolver mResolver;

        static int access$108(DirectoryObserver x0) {
            int i = x0.mRefCount;
            x0.mRefCount = i + 1;
            return i;
        }

        static int access$110(DirectoryObserver x0) {
            int i = x0.mRefCount;
            x0.mRefCount = i - 1;
            return i;
        }

        public DirectoryObserver(File file, ContentResolver resolver, Uri notifyUri) {
            super(file.getAbsolutePath(), 4044);
            this.mRefCount = 0;
            this.mFile = file;
            this.mResolver = resolver;
            this.mNotifyUri = notifyUri;
        }

        @Override
        public void onEvent(int event, String path) {
            if ((event & 4044) != 0) {
                this.mResolver.notifyChange(this.mNotifyUri, (ContentObserver) null, false);
            }
        }

        public String toString() {
            return "DirectoryObserver{file=" + this.mFile.getAbsolutePath() + ", ref=" + this.mRefCount + "}";
        }
    }

    private class DirectoryCursor extends MatrixCursor {
        private final File mFile;

        public DirectoryCursor(String[] columnNames, String docId, File file) {
            super(columnNames);
            Uri notifyUri = DocumentsContract.buildChildDocumentsUri("com.android.externalstorage.documents", docId);
            setNotificationUri(ExternalStorageProvider.this.getContext().getContentResolver(), notifyUri);
            this.mFile = file;
            ExternalStorageProvider.this.startObserving(this.mFile, notifyUri);
        }

        @Override
        public void close() {
            super.close();
            ExternalStorageProvider.this.stopObserving(this.mFile);
        }
    }
}
