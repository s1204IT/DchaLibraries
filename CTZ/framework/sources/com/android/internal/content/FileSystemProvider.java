package com.android.internal.content;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Point;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.FileObserver;
import android.os.FileUtils;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.DocumentsProvider;
import android.provider.MediaStore;
import android.provider.MetadataReader;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.webkit.MimeTypeMap;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.widget.MessagingMessage;
import com.mediatek.internal.content.FileSystemProviderExt;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import libcore.io.IoUtils;

public abstract class FileSystemProvider extends DocumentsProvider {
    static final boolean $assertionsDisabled = false;
    private static final boolean DEBUG = false;
    private static final boolean LOG_INOTIFY = false;
    private static final String MIMETYPE_JPEG = "image/jpeg";
    private static final String MIMETYPE_JPG = "image/jpg";
    private static final String MIMETYPE_OCTET_STREAM = "application/octet-stream";
    private static final String TAG = "FileSystemProvider";
    private static FileSystemProviderExt sFileSystemProviderExt;
    private String[] mDefaultProjection;
    private Handler mHandler;

    @GuardedBy("mObservers")
    private final ArrayMap<File, DirectoryObserver> mObservers = new ArrayMap<>();

    protected abstract Uri buildNotificationUri(String str);

    protected abstract String getDocIdForFile(File file) throws FileNotFoundException;

    protected abstract File getFileForDocId(String str, boolean z) throws FileNotFoundException;

    protected void onDocIdChanged(String str) {
    }

    protected void onDocIdDeleted(String str) {
    }

    @Override
    public boolean onCreate() {
        throw new UnsupportedOperationException("Subclass should override this and call onCreate(defaultDocumentProjection)");
    }

    protected void onCreate(String[] strArr) {
        this.mHandler = new Handler();
        Context context = getContext();
        FileSystemProviderExt fileSystemProviderExt = sFileSystemProviderExt;
        sFileSystemProviderExt = FileSystemProviderExt.getInstance(context);
        this.mDefaultProjection = sFileSystemProviderExt.resolveProjection(strArr);
    }

    @Override
    public boolean isChildDocument(String str, String str2) {
        try {
            return FileUtils.contains(getFileForDocId(str).getCanonicalFile(), getFileForDocId(str2).getCanonicalFile());
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to determine if " + str2 + " is child of " + str + ": " + e);
        }
    }

    @Override
    public Bundle getDocumentMetadata(String str) throws Throwable {
        FileInputStream fileInputStream;
        File fileForDocId = getFileForDocId(str);
        if (!fileForDocId.exists()) {
            throw new FileNotFoundException("Can't find the file for documentId: " + str);
        }
        FileInputStream fileInputStream2 = null;
        if (!fileForDocId.isFile()) {
            Log.w(TAG, "Can't stream non-regular file. Returning empty metadata.");
            return null;
        }
        if (!fileForDocId.canRead()) {
            Log.w(TAG, "Can't stream non-readable file. Returning empty metadata.");
            return null;
        }
        String typeForFile = getTypeForFile(fileForDocId);
        if (!MetadataReader.isSupportedMimeType(typeForFile)) {
            return null;
        }
        try {
            Bundle bundle = new Bundle();
            fileInputStream = new FileInputStream(fileForDocId.getAbsolutePath());
            try {
                try {
                    MetadataReader.getMetadata(bundle, fileInputStream, typeForFile, null);
                    IoUtils.closeQuietly(fileInputStream);
                    return bundle;
                } catch (IOException e) {
                    e = e;
                    Log.e(TAG, "An error occurred retrieving the metadata", e);
                    IoUtils.closeQuietly(fileInputStream);
                    return null;
                }
            } catch (Throwable th) {
                th = th;
                fileInputStream2 = fileInputStream;
                IoUtils.closeQuietly(fileInputStream2);
                throw th;
            }
        } catch (IOException e2) {
            e = e2;
            fileInputStream = null;
        } catch (Throwable th2) {
            th = th2;
            IoUtils.closeQuietly(fileInputStream2);
            throw th;
        }
    }

    protected final List<String> findDocumentPath(File file, File file2) throws FileNotFoundException {
        if (!file2.exists()) {
            throw new FileNotFoundException(file2 + " is not found.");
        }
        if (!FileUtils.contains(file, file2)) {
            throw new FileNotFoundException(file2 + " is not found under " + file);
        }
        LinkedList linkedList = new LinkedList();
        while (file2 != null && FileUtils.contains(file, file2)) {
            linkedList.addFirst(getDocIdForFile(file2));
            file2 = file2.getParentFile();
        }
        return linkedList;
    }

    @Override
    public String createDocument(String str, String str2, String str3) throws FileNotFoundException {
        String strBuildValidFatFilename = FileUtils.buildValidFatFilename(str3);
        File fileForDocId = getFileForDocId(str);
        if (!fileForDocId.isDirectory()) {
            throw new IllegalArgumentException("Parent document isn't a directory");
        }
        File fileBuildUniqueFile = FileUtils.buildUniqueFile(fileForDocId, str2, strBuildValidFatFilename);
        if (DocumentsContract.Document.MIME_TYPE_DIR.equals(str2)) {
            if (!fileBuildUniqueFile.mkdir()) {
                throw new IllegalStateException("Failed to mkdir " + fileBuildUniqueFile);
            }
            String docIdForFile = getDocIdForFile(fileBuildUniqueFile);
            onDocIdChanged(docIdForFile);
            addFolderToMediaStore(getFileForDocId(docIdForFile, true));
            return docIdForFile;
        }
        try {
            if (!fileBuildUniqueFile.createNewFile()) {
                throw new IllegalStateException("Failed to touch " + fileBuildUniqueFile);
            }
            String docIdForFile2 = getDocIdForFile(fileBuildUniqueFile);
            onDocIdChanged(docIdForFile2);
            return docIdForFile2;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to touch " + fileBuildUniqueFile + ": " + e);
        }
    }

    private void addFolderToMediaStore(File file) {
        if (file != null) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                ContentResolver contentResolver = getContext().getContentResolver();
                Uri directoryUri = MediaStore.Files.getDirectoryUri("external");
                ContentValues contentValues = new ContentValues();
                contentValues.put("_data", file.getAbsolutePath());
                contentResolver.insert(directoryUri, contentValues);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
    }

    @Override
    public String renameDocument(String str, String str2) throws FileNotFoundException {
        String strBuildValidFatFilename = FileUtils.buildValidFatFilename(str2);
        File fileForDocId = getFileForDocId(str);
        File fileBuildUniqueFile = FileUtils.buildUniqueFile(fileForDocId.getParentFile(), strBuildValidFatFilename);
        File fileForDocId2 = getFileForDocId(str, true);
        if (!fileForDocId.renameTo(fileBuildUniqueFile)) {
            throw new IllegalStateException("Failed to rename to " + fileBuildUniqueFile);
        }
        String docIdForFile = getDocIdForFile(fileBuildUniqueFile);
        onDocIdChanged(str);
        onDocIdDeleted(str);
        onDocIdChanged(docIdForFile);
        File fileForDocId3 = getFileForDocId(docIdForFile, true);
        moveInMediaStore(fileForDocId2, fileForDocId3);
        if (!TextUtils.equals(str, docIdForFile)) {
            scanFile(fileForDocId3);
            return docIdForFile;
        }
        return null;
    }

    @Override
    public String moveDocument(String str, String str2, String str3) throws FileNotFoundException {
        File fileForDocId = getFileForDocId(str);
        File file = new File(getFileForDocId(str3), fileForDocId.getName());
        File fileForDocId2 = getFileForDocId(str, true);
        if (file.exists()) {
            throw new IllegalStateException("Already exists " + file);
        }
        if (!fileForDocId.renameTo(file)) {
            throw new IllegalStateException("Failed to move to " + file);
        }
        String docIdForFile = getDocIdForFile(file);
        onDocIdChanged(str);
        onDocIdDeleted(str);
        onDocIdChanged(docIdForFile);
        moveInMediaStore(fileForDocId2, getFileForDocId(docIdForFile, true));
        return docIdForFile;
    }

    private void moveInMediaStore(File file, File file2) {
        Uri contentUri;
        if (file != null && file2 != null) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                ContentResolver contentResolver = getContext().getContentResolver();
                if (file2.isDirectory()) {
                    contentUri = MediaStore.Files.getDirectoryUri("external");
                } else {
                    contentUri = MediaStore.Files.getContentUri("external");
                }
                ContentValues contentValues = new ContentValues();
                contentValues.put("_data", file2.getAbsolutePath());
                String absolutePath = file.getAbsolutePath();
                contentResolver.update(contentUri, contentValues, "_data LIKE ? AND lower(_data)=lower(?)", new String[]{absolutePath, absolutePath});
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
    }

    @Override
    public void deleteDocument(String str) throws FileNotFoundException {
        File fileForDocId = getFileForDocId(str);
        File fileForDocId2 = getFileForDocId(str, true);
        boolean zIsDirectory = fileForDocId.isDirectory();
        if (zIsDirectory) {
            FileUtils.deleteContents(fileForDocId);
        }
        if (!fileForDocId.delete()) {
            throw new IllegalStateException("Failed to delete " + fileForDocId);
        }
        onDocIdChanged(str);
        onDocIdDeleted(str);
        removeFromMediaStore(fileForDocId2, zIsDirectory);
    }

    private void removeFromMediaStore(File file, boolean z) throws FileNotFoundException {
        if (file != null) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                ContentResolver contentResolver = getContext().getContentResolver();
                Uri contentUri = MediaStore.Files.getContentUri("external");
                if (z) {
                    String str = file.getAbsolutePath() + "/";
                    contentResolver.delete(contentUri, "_data LIKE ?1 AND lower(substr(_data,1,?2))=lower(?3)", new String[]{str + "%", Integer.toString(str.length()), str});
                }
                String absolutePath = file.getAbsolutePath();
                contentResolver.delete(contentUri, "_data LIKE ?1 AND lower(_data)=lower(?2)", new String[]{absolutePath, absolutePath});
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
    }

    @Override
    public Cursor queryDocument(String str, String[] strArr) throws FileNotFoundException {
        MatrixCursor matrixCursor = new MatrixCursor(resolveProjection(strArr));
        includeFile(matrixCursor, str, null);
        return matrixCursor;
    }

    @Override
    public Cursor queryChildDocuments(String str, String[] strArr, String str2) throws FileNotFoundException {
        File fileForDocId = getFileForDocId(str);
        DirectoryCursor directoryCursor = new DirectoryCursor(resolveProjection(strArr), str, fileForDocId);
        File[] fileArrListFiles = fileForDocId.listFiles();
        for (File file : fileArrListFiles) {
            includeFile(directoryCursor, null, file);
        }
        return directoryCursor;
    }

    protected final Cursor querySearchDocuments(File file, String str, String[] strArr, Set<String> set) throws FileNotFoundException {
        String lowerCase = str.toLowerCase();
        MatrixCursor matrixCursor = new MatrixCursor(resolveProjection(strArr));
        LinkedList linkedList = new LinkedList();
        linkedList.add(file);
        while (!linkedList.isEmpty() && matrixCursor.getCount() < 24) {
            File file2 = (File) linkedList.removeFirst();
            if (file2.isDirectory()) {
                for (File file3 : file2.listFiles()) {
                    linkedList.add(file3);
                }
            }
            if (file2.getName().toLowerCase().contains(lowerCase) && !set.contains(file2.getAbsolutePath())) {
                includeFile(matrixCursor, null, file2);
            }
        }
        return matrixCursor;
    }

    @Override
    public String getDocumentType(String str) throws FileNotFoundException {
        return getTypeForFile(getFileForDocId(str));
    }

    @Override
    public ParcelFileDescriptor openDocument(final String str, String str2, CancellationSignal cancellationSignal) throws FileNotFoundException {
        File fileForDocId = getFileForDocId(str);
        final File fileForDocId2 = getFileForDocId(str, true);
        int mode = ParcelFileDescriptor.parseMode(str2);
        if (mode == 268435456 || fileForDocId2 == null) {
            return ParcelFileDescriptor.open(fileForDocId, mode);
        }
        try {
            return ParcelFileDescriptor.open(fileForDocId, mode, this.mHandler, new ParcelFileDescriptor.OnCloseListener() {
                @Override
                public final void onClose(IOException iOException) {
                    FileSystemProvider.lambda$openDocument$0(this.f$0, str, fileForDocId2, iOException);
                }
            });
        } catch (IOException e) {
            throw new FileNotFoundException("Failed to open for writing: " + e);
        }
    }

    public static void lambda$openDocument$0(FileSystemProvider fileSystemProvider, String str, File file, IOException iOException) {
        fileSystemProvider.onDocIdChanged(str);
        fileSystemProvider.scanFile(file);
    }

    private void scanFile(File file) {
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intent.setData(Uri.fromFile(file));
        getContext().sendBroadcast(intent);
    }

    @Override
    public AssetFileDescriptor openDocumentThumbnail(String str, Point point, CancellationSignal cancellationSignal) throws FileNotFoundException {
        return DocumentsContract.openImageThumbnail(getFileForDocId(str));
    }

    protected MatrixCursor.RowBuilder includeFile(MatrixCursor matrixCursor, String str, File file) throws FileNotFoundException {
        File fileForDocId;
        if (str == null) {
            str = getDocIdForFile(file);
        } else {
            file = getFileForDocId(str);
        }
        if (file != null && !file.isDirectory()) {
            fileForDocId = getFileForDocId(str, true);
        } else {
            fileForDocId = null;
        }
        int i = 0;
        if (file.canWrite()) {
            if (file.isDirectory()) {
                i = 332;
            } else {
                i = 326;
            }
        }
        String typeForFile = getTypeForFile(file);
        String name = file.getName();
        if (typeForFile.startsWith(MessagingMessage.IMAGE_MIME_TYPE_PREFIX)) {
            i |= 1;
        }
        if (typeSupportsMetadata(typeForFile)) {
            i |= 131072;
        }
        MatrixCursor.RowBuilder rowBuilderNewRow = matrixCursor.newRow();
        rowBuilderNewRow.add("document_id", str);
        rowBuilderNewRow.add("_display_name", name);
        rowBuilderNewRow.add("_size", Long.valueOf(file.length()));
        rowBuilderNewRow.add("mime_type", typeForFile);
        rowBuilderNewRow.add("flags", Integer.valueOf(i));
        long jLastModified = file.lastModified();
        if (jLastModified > 31536000000L) {
            rowBuilderNewRow.add("last_modified", Long.valueOf(jLastModified));
        }
        if (sFileSystemProviderExt != null) {
            FileSystemProviderExt fileSystemProviderExt = sFileSystemProviderExt;
            FileSystemProviderExt.addSupportDRMMethod(file, rowBuilderNewRow, str, typeForFile, fileForDocId);
        }
        return rowBuilderNewRow;
    }

    private static String getTypeForFile(File file) {
        if (file.isDirectory()) {
            return DocumentsContract.Document.MIME_TYPE_DIR;
        }
        return getTypeForName(file);
    }

    protected boolean typeSupportsMetadata(String str) {
        return MetadataReader.isSupportedMimeType(str);
    }

    private static String getTypeForName(File file) {
        String name = file.getName();
        if (sFileSystemProviderExt != null) {
            FileSystemProviderExt fileSystemProviderExt = sFileSystemProviderExt;
            return FileSystemProviderExt.getTypeForNameMethod(file);
        }
        int iLastIndexOf = name.lastIndexOf(46);
        if (iLastIndexOf >= 0) {
            String mimeTypeFromExtension = MimeTypeMap.getSingleton().getMimeTypeFromExtension(name.substring(iLastIndexOf + 1).toLowerCase());
            if (mimeTypeFromExtension != null) {
                return mimeTypeFromExtension;
            }
            return MIMETYPE_OCTET_STREAM;
        }
        return MIMETYPE_OCTET_STREAM;
    }

    protected final File getFileForDocId(String str) throws FileNotFoundException {
        return getFileForDocId(str, false);
    }

    private String[] resolveProjection(String[] strArr) {
        return strArr == null ? this.mDefaultProjection : strArr;
    }

    private void startObserving(File file, Uri uri) {
        synchronized (this.mObservers) {
            DirectoryObserver directoryObserver = this.mObservers.get(file);
            if (directoryObserver == null) {
                directoryObserver = new DirectoryObserver(file, getContext().getContentResolver(), uri);
                directoryObserver.startWatching();
                this.mObservers.put(file, directoryObserver);
            }
            DirectoryObserver.access$008(directoryObserver);
        }
    }

    private void stopObserving(File file) {
        synchronized (this.mObservers) {
            DirectoryObserver directoryObserver = this.mObservers.get(file);
            if (directoryObserver == null) {
                return;
            }
            DirectoryObserver.access$010(directoryObserver);
            if (directoryObserver.mRefCount == 0) {
                this.mObservers.remove(file);
                directoryObserver.stopWatching();
            }
        }
    }

    private static class DirectoryObserver extends FileObserver {
        private static final int NOTIFY_EVENTS = 4044;
        private final File mFile;
        private final Uri mNotifyUri;
        private int mRefCount;
        private final ContentResolver mResolver;

        static int access$008(DirectoryObserver directoryObserver) {
            int i = directoryObserver.mRefCount;
            directoryObserver.mRefCount = i + 1;
            return i;
        }

        static int access$010(DirectoryObserver directoryObserver) {
            int i = directoryObserver.mRefCount;
            directoryObserver.mRefCount = i - 1;
            return i;
        }

        public DirectoryObserver(File file, ContentResolver contentResolver, Uri uri) {
            super(file.getAbsolutePath(), NOTIFY_EVENTS);
            this.mRefCount = 0;
            this.mFile = file;
            this.mResolver = contentResolver;
            this.mNotifyUri = uri;
        }

        @Override
        public void onEvent(int i, String str) {
            if ((i & NOTIFY_EVENTS) != 0) {
                this.mResolver.notifyChange(this.mNotifyUri, (ContentObserver) null, false);
            }
        }

        public String toString() {
            return "DirectoryObserver{file=" + this.mFile.getAbsolutePath() + ", ref=" + this.mRefCount + "}";
        }
    }

    private class DirectoryCursor extends MatrixCursor {
        private final File mFile;

        public DirectoryCursor(String[] strArr, String str, File file) {
            super(strArr);
            Uri uriBuildNotificationUri = FileSystemProvider.this.buildNotificationUri(str);
            setNotificationUri(FileSystemProvider.this.getContext().getContentResolver(), uriBuildNotificationUri);
            this.mFile = file;
            FileSystemProvider.this.startObserving(this.mFile, uriBuildNotificationUri);
        }

        @Override
        public void close() {
            super.close();
            FileSystemProvider.this.stopObserving(this.mFile);
        }
    }
}
