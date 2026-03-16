package com.android.providers.downloads;

import android.app.DownloadManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Point;
import android.net.Uri;
import android.os.Binder;
import android.os.CancellationSignal;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.DocumentsProvider;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;
import java.io.File;
import java.io.FileNotFoundException;
import java.text.NumberFormat;
import libcore.io.IoUtils;

public class DownloadStorageProvider extends DocumentsProvider {
    private DownloadManager mDm;
    private static final String[] DEFAULT_ROOT_PROJECTION = {"root_id", "flags", "icon", "title", "document_id"};
    private static final String[] DEFAULT_DOCUMENT_PROJECTION = {"document_id", "mime_type", "_display_name", "summary", "last_modified", "flags", "_size"};

    @Override
    public boolean onCreate() {
        this.mDm = (DownloadManager) getContext().getSystemService("download");
        this.mDm.setAccessAllDownloads(true);
        return true;
    }

    private static String[] resolveRootProjection(String[] projection) {
        return projection != null ? projection : DEFAULT_ROOT_PROJECTION;
    }

    private static String[] resolveDocumentProjection(String[] projection) {
        return projection != null ? projection : DEFAULT_DOCUMENT_PROJECTION;
    }

    private void copyNotificationUri(MatrixCursor result, Cursor cursor) {
        result.setNotificationUri(getContext().getContentResolver(), cursor.getNotificationUri());
    }

    static void onDownloadProviderDelete(Context context, long id) {
        Uri uri = DocumentsContract.buildDocumentUri("com.android.providers.downloads.documents", Long.toString(id));
        context.revokeUriPermission(uri, -1);
    }

    @Override
    public Cursor queryRoots(String[] projection) throws FileNotFoundException {
        MatrixCursor result = new MatrixCursor(resolveRootProjection(projection));
        MatrixCursor.RowBuilder row = result.newRow();
        row.add("root_id", "downloads");
        row.add("flags", 7);
        row.add("icon", Integer.valueOf(R.mipmap.ic_launcher_download));
        row.add("title", getContext().getString(R.string.root_downloads));
        row.add("document_id", "downloads");
        return result;
    }

    @Override
    public String createDocument(String docId, String mimeType, String displayName) throws FileNotFoundException {
        if ("vnd.android.document/directory".equals(mimeType)) {
            throw new FileNotFoundException("Directory creation not supported");
        }
        File parent = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        parent.mkdirs();
        long token = Binder.clearCallingIdentity();
        try {
            String displayName2 = removeExtension(mimeType, displayName);
            File file = new File(parent, addExtension(mimeType, displayName2));
            int n = 0;
            while (true) {
                int n2 = n;
                if (!file.exists()) {
                    break;
                }
                n = n2 + 1;
                if (n2 < 32) {
                    file = new File(parent, addExtension(mimeType, displayName2 + " (" + n + ")"));
                }
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public void deleteDocument(String docId) throws FileNotFoundException {
        long token = Binder.clearCallingIdentity();
        try {
            if (this.mDm.remove(Long.parseLong(docId)) != 1) {
                throw new IllegalStateException("Failed to delete " + docId);
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public Cursor queryDocument(String docId, String[] projection) throws FileNotFoundException {
        MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));
        if ("downloads".equals(docId)) {
            includeDefaultDocument(result);
        } else {
            long token = Binder.clearCallingIdentity();
            Cursor cursor = null;
            try {
                cursor = this.mDm.query(new DownloadManager.Query().setFilterById(Long.parseLong(docId)));
                copyNotificationUri(result, cursor);
                if (cursor.moveToFirst()) {
                    includeDownloadFromCursor(result, cursor);
                }
            } finally {
                IoUtils.closeQuietly(cursor);
                Binder.restoreCallingIdentity(token);
            }
        }
        return result;
    }

    @Override
    public Cursor queryChildDocuments(String docId, String[] projection, String sortOrder) throws FileNotFoundException {
        MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));
        long token = Binder.clearCallingIdentity();
        Cursor cursor = null;
        try {
            cursor = this.mDm.query(new DownloadManager.Query().setOnlyIncludeVisibleInDownloadsUi(true).setFilterByStatus(8));
            copyNotificationUri(result, cursor);
            while (cursor.moveToNext()) {
                includeDownloadFromCursor(result, cursor);
            }
            return result;
        } finally {
            IoUtils.closeQuietly(cursor);
            Binder.restoreCallingIdentity(token);
        }
    }

    public Cursor queryChildDocumentsForManage(String parentDocumentId, String[] projection, String sortOrder) throws FileNotFoundException {
        MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));
        long token = Binder.clearCallingIdentity();
        Cursor cursor = null;
        try {
            cursor = this.mDm.query(new DownloadManager.Query().setOnlyIncludeVisibleInDownloadsUi(true));
            copyNotificationUri(result, cursor);
            while (cursor.moveToNext()) {
                includeDownloadFromCursor(result, cursor);
            }
            return result;
        } finally {
            IoUtils.closeQuietly(cursor);
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public Cursor queryRecentDocuments(String rootId, String[] projection) throws FileNotFoundException {
        MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));
        long token = Binder.clearCallingIdentity();
        Cursor cursor = null;
        try {
            cursor = this.mDm.query(new DownloadManager.Query().setOnlyIncludeVisibleInDownloadsUi(true).setFilterByStatus(8));
            copyNotificationUri(result, cursor);
            while (cursor.moveToNext() && result.getCount() < 12) {
                String mimeType = cursor.getString(cursor.getColumnIndexOrThrow("media_type"));
                String uri = cursor.getString(cursor.getColumnIndexOrThrow("mediaprovider_uri"));
                if (mimeType != null && (!mimeType.startsWith("image/") || TextUtils.isEmpty(uri))) {
                    includeDownloadFromCursor(result, cursor);
                }
            }
            return result;
        } finally {
            IoUtils.closeQuietly(cursor);
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public ParcelFileDescriptor openDocument(String docId, String mode, CancellationSignal signal) throws FileNotFoundException {
        long token = Binder.clearCallingIdentity();
        try {
            long id = Long.parseLong(docId);
            ContentResolver resolver = getContext().getContentResolver();
            return resolver.openFileDescriptor(this.mDm.getDownloadUri(id), mode, signal);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public AssetFileDescriptor openDocumentThumbnail(String docId, Point sizeHint, CancellationSignal signal) throws FileNotFoundException {
        ParcelFileDescriptor pfd = openDocument(docId, "r", signal);
        return new AssetFileDescriptor(pfd, 0L, -1L);
    }

    private void includeDefaultDocument(MatrixCursor result) {
        MatrixCursor.RowBuilder row = result.newRow();
        row.add("document_id", "downloads");
        row.add("mime_type", "vnd.android.document/directory");
        row.add("flags", 40);
    }

    private void includeDownloadFromCursor(MatrixCursor result, Cursor cursor) {
        long id = cursor.getLong(cursor.getColumnIndexOrThrow("_id"));
        String docId = String.valueOf(id);
        String displayName = cursor.getString(cursor.getColumnIndexOrThrow("title"));
        String summary = cursor.getString(cursor.getColumnIndexOrThrow("description"));
        String mimeType = cursor.getString(cursor.getColumnIndexOrThrow("media_type"));
        if (mimeType == null) {
            mimeType = "vnd.android.document/file";
        }
        Long size = Long.valueOf(cursor.getLong(cursor.getColumnIndexOrThrow("total_size")));
        if (size.longValue() == -1) {
            size = null;
        }
        int status = cursor.getInt(cursor.getColumnIndexOrThrow("status"));
        switch (status) {
            case 1:
                summary = getContext().getString(R.string.download_queued);
                break;
            case 2:
                long progress = cursor.getLong(cursor.getColumnIndexOrThrow("bytes_so_far"));
                if (size != null) {
                    String percent = NumberFormat.getPercentInstance().format(progress / size.longValue());
                    summary = getContext().getString(R.string.download_running_percent, percent);
                } else {
                    summary = getContext().getString(R.string.download_running);
                }
                break;
            case 3:
            case 5:
            case 6:
            case 7:
            default:
                summary = getContext().getString(R.string.download_error);
                break;
            case 4:
                summary = getContext().getString(R.string.download_queued);
                break;
            case 8:
                break;
        }
        int flags = 6;
        if (mimeType != null && mimeType.startsWith("image/")) {
            flags = 6 | 1;
        }
        long lastModified = cursor.getLong(cursor.getColumnIndexOrThrow("last_modified_timestamp"));
        MatrixCursor.RowBuilder row = result.newRow();
        row.add("document_id", docId);
        row.add("_display_name", displayName);
        row.add("summary", summary);
        row.add("_size", size);
        row.add("mime_type", mimeType);
        row.add("last_modified", Long.valueOf(lastModified));
        row.add("flags", Integer.valueOf(flags));
    }

    private static String removeExtension(String mimeType, String name) {
        int lastDot = name.lastIndexOf(46);
        if (lastDot >= 0) {
            String extension = name.substring(lastDot + 1);
            String nameMime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            if (mimeType.equals(nameMime)) {
                return name.substring(0, lastDot);
            }
            return name;
        }
        return name;
    }

    private static String addExtension(String mimeType, String name) {
        String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
        if (extension != null) {
            return name + "." + extension;
        }
        return name;
    }
}
