package com.android.providers.downloads;

import android.app.DownloadManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Binder;
import android.os.CancellationSignal;
import android.os.Environment;
import android.os.FileObserver;
import android.os.FileUtils;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.Downloads;
import android.text.TextUtils;
import com.android.internal.content.FileSystemProvider;
import java.io.File;
import java.io.FileNotFoundException;
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.Set;
import libcore.io.IoUtils;

public class DownloadStorageProvider extends FileSystemProvider {
    static final boolean $assertionsDisabled = false;
    private DownloadManager mDm;
    private static final String[] DEFAULT_ROOT_PROJECTION = {"root_id", "flags", "icon", "title", "document_id"};
    private static final String[] DEFAULT_DOCUMENT_PROJECTION = {"document_id", "mime_type", "_display_name", "summary", "last_modified", "flags", "_size"};

    public boolean onCreate() {
        super.onCreate(DEFAULT_DOCUMENT_PROJECTION);
        this.mDm = (DownloadManager) getContext().getSystemService("download");
        this.mDm.setAccessAllDownloads(true);
        this.mDm.setAccessFilename(true);
        return true;
    }

    private static String[] resolveRootProjection(String[] strArr) {
        return strArr != null ? strArr : DEFAULT_ROOT_PROJECTION;
    }

    private static String[] resolveDocumentProjection(String[] strArr) {
        return strArr != null ? strArr : DEFAULT_DOCUMENT_PROJECTION;
    }

    private void copyNotificationUri(MatrixCursor matrixCursor, Cursor cursor) {
        matrixCursor.setNotificationUri(getContext().getContentResolver(), cursor.getNotificationUri());
    }

    static void onDownloadProviderDelete(Context context, long j) {
        context.revokeUriPermission(DocumentsContract.buildDocumentUri("com.android.providers.downloads.documents", Long.toString(j)), -1);
    }

    public Cursor queryRoots(String[] strArr) throws FileNotFoundException {
        getDownloadsDirectory().mkdirs();
        MatrixCursor matrixCursor = new MatrixCursor(resolveRootProjection(strArr));
        MatrixCursor.RowBuilder rowBuilderNewRow = matrixCursor.newRow();
        rowBuilderNewRow.add("root_id", "downloads");
        rowBuilderNewRow.add("flags", 31);
        rowBuilderNewRow.add("icon", Integer.valueOf(R.mipmap.ic_launcher_download));
        rowBuilderNewRow.add("title", getContext().getString(R.string.root_downloads));
        rowBuilderNewRow.add("document_id", "downloads");
        return matrixCursor;
    }

    public DocumentsContract.Path findDocumentPath(String str, String str2) throws FileNotFoundException {
        String str3 = str == null ? "downloads" : null;
        if (str == null) {
            str = "downloads";
        }
        return new DocumentsContract.Path(str3, findDocumentPath(getFileForDocId(str), getFileForDocId(str2)));
    }

    public String createDocument(String str, String str2, String str3) throws FileNotFoundException {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            String strCreateDocument = super.createDocument(str, str2, str3);
            if (!"vnd.android.document/directory".equals(str2) && !RawDocumentsHelper.isRawDocId(str)) {
                File fileForDocId = getFileForDocId(strCreateDocument);
                strCreateDocument = Long.toString(this.mDm.addCompletedDownload(fileForDocId.getName(), fileForDocId.getName(), true, str2, fileForDocId.getAbsolutePath(), 0L, false, true));
            }
            return strCreateDocument;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void deleteDocument(String str) throws FileNotFoundException {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            if (RawDocumentsHelper.isRawDocId(str)) {
                super.deleteDocument(str);
            } else if (this.mDm.remove(Long.parseLong(str)) != 1) {
                throw new IllegalStateException("Failed to delete " + str);
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public String renameDocument(String str, String str2) throws FileNotFoundException {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            if (RawDocumentsHelper.isRawDocId(str)) {
                return super.renameDocument(str, str2);
            }
            String strBuildValidFatFilename = FileUtils.buildValidFatFilename(str2);
            if (!this.mDm.rename(getContext(), Long.parseLong(str), strBuildValidFatFilename)) {
                throw new IllegalStateException("Failed to rename to " + strBuildValidFatFilename + " in downloadsManager");
            }
            return null;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public Cursor queryDocument(String str, String[] strArr) throws Throwable {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        Cursor cursor = null;
        try {
            if (RawDocumentsHelper.isRawDocId(str)) {
                Cursor cursorQueryDocument = super.queryDocument(str, strArr);
                IoUtils.closeQuietly((AutoCloseable) null);
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                return cursorQueryDocument;
            }
            DownloadsCursor downloadsCursor = new DownloadsCursor(strArr, getContext().getContentResolver());
            if ("downloads".equals(str)) {
                includeDefaultDocument(downloadsCursor);
            } else {
                Cursor cursorQuery = this.mDm.query(new DownloadManager.Query().setFilterById(Long.parseLong(str)));
                try {
                    copyNotificationUri(downloadsCursor, cursorQuery);
                    Set<String> hashSet = new HashSet<>();
                    if (cursorQuery.moveToFirst()) {
                        includeDownloadFromCursor(downloadsCursor, cursorQuery, hashSet);
                    }
                    cursor = cursorQuery;
                } catch (Throwable th) {
                    cursor = cursorQuery;
                    th = th;
                    IoUtils.closeQuietly(cursor);
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                    throw th;
                }
            }
            downloadsCursor.start();
            IoUtils.closeQuietly(cursor);
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            return downloadsCursor;
        } catch (Throwable th2) {
            th = th2;
        }
    }

    public Cursor queryChildDocuments(String str, String[] strArr, String str2) throws FileNotFoundException {
        return queryChildDocuments(str, strArr, str2, false);
    }

    public Cursor queryChildDocumentsForManage(String str, String[] strArr, String str2) throws FileNotFoundException {
        return queryChildDocuments(str, strArr, str2, true);
    }

    private Cursor queryChildDocuments(String str, String[] strArr, String str2, boolean z) throws Throwable {
        Cursor cursorQuery;
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        Cursor cursor = null;
        try {
            if (RawDocumentsHelper.isRawDocId(str)) {
                Cursor cursorQueryChildDocuments = super.queryChildDocuments(str, strArr, str2);
                IoUtils.closeQuietly((AutoCloseable) null);
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                return cursorQueryChildDocuments;
            }
            DownloadsCursor downloadsCursor = new DownloadsCursor(strArr, getContext().getContentResolver());
            if (z) {
                cursorQuery = this.mDm.query(new DownloadManager.Query().setOnlyIncludeVisibleInDownloadsUi(true));
            } else {
                cursorQuery = this.mDm.query(new DownloadManager.Query().setOnlyIncludeVisibleInDownloadsUi(true).setFilterByStatus(8));
            }
            try {
                copyNotificationUri(downloadsCursor, cursorQuery);
                Set<String> hashSet = new HashSet<>();
                while (cursorQuery.moveToNext()) {
                    includeDownloadFromCursor(downloadsCursor, cursorQuery, hashSet);
                }
                includeFilesFromSharedStorage(downloadsCursor, hashSet, null);
                downloadsCursor.start();
                IoUtils.closeQuietly(cursorQuery);
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                return downloadsCursor;
            } catch (Throwable th) {
                th = th;
                cursor = cursorQuery;
                IoUtils.closeQuietly(cursor);
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    public Cursor queryRecentDocuments(String str, String[] strArr) throws Throwable {
        Cursor cursorQuery;
        DownloadsCursor downloadsCursor = new DownloadsCursor(strArr, getContext().getContentResolver());
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            cursorQuery = this.mDm.query(new DownloadManager.Query().setOnlyIncludeVisibleInDownloadsUi(true).setFilterByStatus(8));
            try {
                copyNotificationUri(downloadsCursor, cursorQuery);
                while (cursorQuery.moveToNext() && downloadsCursor.getCount() < 12) {
                    String string = cursorQuery.getString(cursorQuery.getColumnIndexOrThrow("media_type"));
                    String string2 = cursorQuery.getString(cursorQuery.getColumnIndexOrThrow("mediaprovider_uri"));
                    if (string == null || !string.startsWith("image/") || !TextUtils.isEmpty(string2)) {
                    }
                }
                IoUtils.closeQuietly(cursorQuery);
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                downloadsCursor.start();
                return downloadsCursor;
            } catch (Throwable th) {
                th = th;
                IoUtils.closeQuietly(cursorQuery);
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
            cursorQuery = null;
        }
    }

    public Cursor querySearchDocuments(String str, String str2, String[] strArr) throws Throwable {
        Cursor cursorQuery;
        DownloadsCursor downloadsCursor = new DownloadsCursor(strArr, getContext().getContentResolver());
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            cursorQuery = this.mDm.query(new DownloadManager.Query().setOnlyIncludeVisibleInDownloadsUi(true).setFilterByString(str2));
            try {
                copyNotificationUri(downloadsCursor, cursorQuery);
                Set<String> hashSet = new HashSet<>();
                while (cursorQuery.moveToNext()) {
                    includeDownloadFromCursor(downloadsCursor, cursorQuery, hashSet);
                }
                Cursor cursorQuerySearchDocuments = super.querySearchDocuments(getDownloadsDirectory(), str2, strArr, hashSet);
                while (cursorQuerySearchDocuments.moveToNext()) {
                    includeFileFromSharedStorage(downloadsCursor, getFileForDocId(cursorQuerySearchDocuments.getString(cursorQuerySearchDocuments.getColumnIndexOrThrow("document_id"))));
                }
                IoUtils.closeQuietly(cursorQuery);
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                downloadsCursor.start();
                return downloadsCursor;
            } catch (Throwable th) {
                th = th;
                IoUtils.closeQuietly(cursorQuery);
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
            cursorQuery = null;
        }
    }

    public String getDocumentType(String str) throws FileNotFoundException {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            if (RawDocumentsHelper.isRawDocId(str)) {
                return super.getDocumentType(str);
            }
            return getContext().getContentResolver().getType(this.mDm.getDownloadUri(Long.parseLong(str)));
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public ParcelFileDescriptor openDocument(String str, String str2, CancellationSignal cancellationSignal) throws FileNotFoundException {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            if (RawDocumentsHelper.isRawDocId(str)) {
                return super.openDocument(str, str2, cancellationSignal);
            }
            return getContext().getContentResolver().openFileDescriptor(this.mDm.getDownloadUri(Long.parseLong(str)), str2, cancellationSignal);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    protected File getFileForDocId(String str, boolean z) throws Throwable {
        if (RawDocumentsHelper.isRawDocId(str)) {
            return new File(RawDocumentsHelper.getAbsoluteFilePath(str));
        }
        if ("downloads".equals(str)) {
            return getDownloadsDirectory();
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        Cursor cursor = null;
        String string = null;
        try {
            Cursor cursorQuery = this.mDm.query(new DownloadManager.Query().setFilterById(Long.parseLong(str)));
            try {
                if (cursorQuery.moveToFirst()) {
                    string = cursorQuery.getString(cursorQuery.getColumnIndexOrThrow("local_filename"));
                }
                IoUtils.closeQuietly(cursorQuery);
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                if (string == null) {
                    throw new IllegalStateException("File has no filepath. Could not be found.");
                }
                return new File(string);
            } catch (Throwable th) {
                cursor = cursorQuery;
                th = th;
                IoUtils.closeQuietly(cursor);
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    protected String getDocIdForFile(File file) throws FileNotFoundException {
        return RawDocumentsHelper.getDocIdForFile(file);
    }

    protected Uri buildNotificationUri(String str) {
        return DocumentsContract.buildChildDocumentsUri("com.android.providers.downloads.documents", str);
    }

    private void includeDefaultDocument(MatrixCursor matrixCursor) {
        MatrixCursor.RowBuilder rowBuilderNewRow = matrixCursor.newRow();
        rowBuilderNewRow.add("document_id", "downloads");
        rowBuilderNewRow.add("_display_name", getContext().getString(R.string.root_downloads));
        rowBuilderNewRow.add("mime_type", "vnd.android.document/directory");
        rowBuilderNewRow.add("flags", 40);
    }

    private void includeDownloadFromCursor(MatrixCursor matrixCursor, Cursor cursor, Set<String> set) {
        String strValueOf = String.valueOf(cursor.getLong(cursor.getColumnIndexOrThrow("_id")));
        String string = cursor.getString(cursor.getColumnIndexOrThrow("title"));
        String string2 = cursor.getString(cursor.getColumnIndexOrThrow("description"));
        String string3 = cursor.getString(cursor.getColumnIndexOrThrow("media_type"));
        if (string3 == null) {
            string3 = "vnd.android.document/file";
        }
        Long lValueOf = Long.valueOf(cursor.getLong(cursor.getColumnIndexOrThrow("total_size")));
        if (lValueOf.longValue() == -1) {
            lValueOf = null;
        }
        String string4 = cursor.getString(cursor.getColumnIndexOrThrow("local_filename"));
        int i = 65536;
        int i2 = cursor.getInt(cursor.getColumnIndexOrThrow("status"));
        if (i2 != 4) {
            if (i2 == 8) {
                if (string4 == null || !new File(string4).exists()) {
                    return;
                } else {
                    i = 64;
                }
            } else {
                switch (i2) {
                    case 1:
                        string2 = getContext().getString(R.string.download_queued);
                        break;
                    case 2:
                        long j = cursor.getLong(cursor.getColumnIndexOrThrow("bytes_so_far"));
                        if (lValueOf != null) {
                            string2 = getContext().getString(R.string.download_running_percent, NumberFormat.getPercentInstance().format(j / lValueOf.longValue()));
                        } else {
                            string2 = getContext().getString(R.string.download_running);
                        }
                        break;
                    default:
                        string2 = getContext().getString(R.string.download_error);
                        break;
                }
            }
        } else {
            string2 = getContext().getString(R.string.download_queued);
        }
        int i3 = i | 6;
        if (string3.startsWith("image/")) {
            i3 |= 1;
        }
        if (typeSupportsMetadata(string3)) {
            i3 |= 131072;
        }
        long j2 = cursor.getLong(cursor.getColumnIndexOrThrow("last_modified_timestamp"));
        MatrixCursor.RowBuilder rowBuilderNewRow = matrixCursor.newRow();
        rowBuilderNewRow.add("document_id", strValueOf);
        rowBuilderNewRow.add("_display_name", string);
        rowBuilderNewRow.add("summary", string2);
        rowBuilderNewRow.add("_size", lValueOf);
        rowBuilderNewRow.add("mime_type", string3);
        rowBuilderNewRow.add("flags", Integer.valueOf(i3));
        if (i2 != 2) {
            rowBuilderNewRow.add("last_modified", Long.valueOf(j2));
        }
        set.add(string4);
    }

    private void includeFilesFromSharedStorage(MatrixCursor matrixCursor, Set<String> set, String str) throws FileNotFoundException {
        for (File file : getDownloadsDirectory().listFiles()) {
            boolean zContains = set.contains(file.getAbsolutePath());
            boolean z = str == null || file.getName().contains(str);
            if (!zContains && z) {
                includeFileFromSharedStorage(matrixCursor, file);
            }
        }
    }

    private void includeFileFromSharedStorage(MatrixCursor matrixCursor, File file) throws FileNotFoundException {
        includeFile(matrixCursor, null, file);
    }

    private static File getDownloadsDirectory() {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    }

    private static final class DownloadsCursor extends MatrixCursor {
        private static ContentChangedRelay mFileWatcher;
        private static final Object mLock = new Object();
        private static int mOpenCursorCount = 0;
        private final ContentResolver mResolver;

        DownloadsCursor(String[] strArr, ContentResolver contentResolver) {
            super(DownloadStorageProvider.resolveDocumentProjection(strArr));
            this.mResolver = contentResolver;
        }

        void start() {
            synchronized (mLock) {
                int i = mOpenCursorCount;
                mOpenCursorCount = i + 1;
                if (i == 0) {
                    mFileWatcher = new ContentChangedRelay(this.mResolver);
                    mFileWatcher.startWatching();
                }
            }
        }

        @Override
        public void close() {
            super.close();
            synchronized (mLock) {
                int i = mOpenCursorCount - 1;
                mOpenCursorCount = i;
                if (i == 0) {
                    mFileWatcher.stopWatching();
                    mFileWatcher = null;
                }
            }
        }
    }

    private static class ContentChangedRelay extends FileObserver {
        private static final String DOWNLOADS_PATH = DownloadStorageProvider.getDownloadsDirectory().getAbsolutePath();
        private final ContentResolver mResolver;

        public ContentChangedRelay(ContentResolver contentResolver) {
            super(DOWNLOADS_PATH, 4044);
            this.mResolver = contentResolver;
        }

        @Override
        public void startWatching() {
            super.startWatching();
        }

        @Override
        public void stopWatching() {
            super.stopWatching();
        }

        @Override
        public void onEvent(int i, String str) {
            if ((i & 4044) != 0) {
                this.mResolver.notifyChange(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, (ContentObserver) null, false);
                this.mResolver.notifyChange(Downloads.Impl.CONTENT_URI, (ContentObserver) null, false);
            }
        }
    }
}
