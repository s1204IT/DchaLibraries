package com.android.documentsui.archives;

import android.content.ContentProviderClient;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.DocumentsProvider;
import android.provider.MetadataReader;
import android.util.Log;
import com.android.documentsui.R;
import com.android.documentsui.archives.Archive;
import com.android.internal.annotations.GuardedBy;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import libcore.io.IoUtils;

public class ArchivesProvider extends DocumentsProvider {
    private static final String[] DEFAULT_ROOTS_PROJECTION = {"root_id", "document_id", "title", "flags", "icon"};
    private static final String[] ZIP_MIME_TYPES = {"application/zip", "application/x-zip", "application/x-zip-compressed"};

    @GuardedBy("mArchives")
    private final Map<Key, Loader> mArchives = new HashMap();

    @Override
    public Bundle call(String str, String str2, Bundle bundle) {
        if ("acquireArchive".equals(str)) {
            acquireArchive(str2);
            return null;
        }
        if ("releaseArchive".equals(str)) {
            releaseArchive(str2);
            return null;
        }
        return super.call(str, str2, bundle);
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor queryRoots(String[] strArr) {
        if (strArr == null) {
            strArr = DEFAULT_ROOTS_PROJECTION;
        }
        return new MatrixCursor(strArr);
    }

    @Override
    public Cursor queryChildDocuments(String str, String[] strArr, String str2) throws FileNotFoundException {
        ArchiveId archiveIdFromDocumentId = ArchiveId.fromDocumentId(str);
        Loader loaderOrThrow = getLoaderOrThrow(str);
        int status = loaderOrThrow.getStatus();
        if (status == 1) {
            return loaderOrThrow.get().queryChildDocuments(str, strArr, str2);
        }
        if (strArr == null) {
            strArr = Archive.DEFAULT_PROJECTION;
        }
        MatrixCursor matrixCursor = new MatrixCursor(strArr);
        Bundle bundle = new Bundle();
        if (status == 0) {
            bundle.putBoolean("loading", true);
        } else if (status == 2) {
            bundle.putString("error", getContext().getString(R.string.archive_loading_failed));
        }
        matrixCursor.setExtras(bundle);
        matrixCursor.setNotificationUri(getContext().getContentResolver(), buildUriForArchive(archiveIdFromDocumentId.mArchiveUri, archiveIdFromDocumentId.mAccessMode));
        return matrixCursor;
    }

    @Override
    public String getDocumentType(String str) throws FileNotFoundException {
        if (ArchiveId.fromDocumentId(str).mPath.equals("/")) {
            return "vnd.android.document/directory";
        }
        return getLoaderOrThrow(str).get().getDocumentType(str);
    }

    @Override
    public boolean isChildDocument(String str, String str2) {
        return getLoaderOrThrow(str2).get().isChildDocument(str, str2);
    }

    @Override
    public Bundle getDocumentMetadata(String str) throws Throwable {
        ParcelFileDescriptor.AutoCloseInputStream autoCloseInputStream;
        String documentType = getLoaderOrThrow(str).get().getDocumentType(str);
        ?? IsSupportedMimeType = MetadataReader.isSupportedMimeType(documentType);
        try {
            if (IsSupportedMimeType == 0) {
                return null;
            }
            try {
                autoCloseInputStream = new ParcelFileDescriptor.AutoCloseInputStream(openDocument(str, "r", null));
                try {
                    Bundle bundle = new Bundle();
                    MetadataReader.getMetadata(bundle, autoCloseInputStream, documentType, (String[]) null);
                    IoUtils.closeQuietly(autoCloseInputStream);
                    return bundle;
                } catch (IOException e) {
                    e = e;
                    Log.e("ArchivesProvider", "An error occurred retrieving the metadata.", e);
                    IoUtils.closeQuietly(autoCloseInputStream);
                    return null;
                }
            } catch (IOException e2) {
                e = e2;
                autoCloseInputStream = null;
            } catch (Throwable th) {
                th = th;
                IsSupportedMimeType = 0;
                IoUtils.closeQuietly((AutoCloseable) IsSupportedMimeType);
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    @Override
    public Cursor queryDocument(String str, String[] strArr) throws FileNotFoundException {
        ArchiveId archiveIdFromDocumentId = ArchiveId.fromDocumentId(str);
        if (archiveIdFromDocumentId.mPath.equals("/")) {
            Cursor cursorQuery = getContext().getContentResolver().query(archiveIdFromDocumentId.mArchiveUri, new String[]{"_display_name"}, null, null, null, null);
            Throwable th = null;
            try {
                if (cursorQuery != null) {
                    if (cursorQuery.moveToFirst()) {
                        String string = cursorQuery.getString(cursorQuery.getColumnIndex("_display_name"));
                        if (strArr == null) {
                            strArr = Archive.DEFAULT_PROJECTION;
                        }
                        MatrixCursor matrixCursor = new MatrixCursor(strArr);
                        MatrixCursor.RowBuilder rowBuilderNewRow = matrixCursor.newRow();
                        rowBuilderNewRow.add("document_id", str);
                        rowBuilderNewRow.add("_display_name", string);
                        rowBuilderNewRow.add("_size", 0);
                        rowBuilderNewRow.add("mime_type", "vnd.android.document/directory");
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                        return matrixCursor;
                    }
                }
                throw new FileNotFoundException("Cannot resolve display name of the archive.");
            } catch (Throwable th2) {
                if (cursorQuery != null) {
                    if (0 != 0) {
                        try {
                            cursorQuery.close();
                        } catch (Throwable th3) {
                            th.addSuppressed(th3);
                        }
                    } else {
                        cursorQuery.close();
                    }
                }
                throw th2;
            }
        }
        return getLoaderOrThrow(str).get().queryDocument(str, strArr);
    }

    @Override
    public String createDocument(String str, String str2, String str3) throws FileNotFoundException {
        return getLoaderOrThrow(str).get().createDocument(str, str2, str3);
    }

    @Override
    public ParcelFileDescriptor openDocument(String str, String str2, CancellationSignal cancellationSignal) throws FileNotFoundException {
        return getLoaderOrThrow(str).get().openDocument(str, str2, cancellationSignal);
    }

    @Override
    public AssetFileDescriptor openDocumentThumbnail(String str, Point point, CancellationSignal cancellationSignal) throws FileNotFoundException {
        return getLoaderOrThrow(str).get().openDocumentThumbnail(str, point, cancellationSignal);
    }

    public static boolean isSupportedArchiveType(String str) {
        for (String str2 : ZIP_MIME_TYPES) {
            if (str2.equals(str)) {
                return true;
            }
        }
        return false;
    }

    public static Uri buildUriForArchive(Uri uri, int i) {
        return DocumentsContract.buildDocumentUri("com.android.documentsui.archives", new ArchiveId(uri, i, "/").toDocumentId());
    }

    public static void acquireArchive(ContentProviderClient contentProviderClient, Uri uri) {
        Archive.MorePreconditions.checkArgumentEquals("com.android.documentsui.archives", uri.getAuthority(), "Mismatching authority. Expected: %s, actual: %s.");
        try {
            contentProviderClient.call("acquireArchive", DocumentsContract.getDocumentId(uri), null);
        } catch (Exception e) {
            Log.w("ArchivesProvider", "Failed to acquire archive.", e);
        }
    }

    public static void releaseArchive(ContentProviderClient contentProviderClient, Uri uri) {
        Archive.MorePreconditions.checkArgumentEquals("com.android.documentsui.archives", uri.getAuthority(), "Mismatching authority. Expected: %s, actual: %s.");
        try {
            contentProviderClient.call("releaseArchive", DocumentsContract.getDocumentId(uri), null);
        } catch (Exception e) {
            Log.w("ArchivesProvider", "Failed to release archive.", e);
        }
    }

    private void acquireArchive(String str) {
        ArchiveId archiveIdFromDocumentId = ArchiveId.fromDocumentId(str);
        synchronized (this.mArchives) {
            Key keyFromArchiveId = Key.fromArchiveId(archiveIdFromDocumentId);
            Loader loader = this.mArchives.get(keyFromArchiveId);
            if (loader == null) {
                loader = new Loader(getContext(), archiveIdFromDocumentId.mArchiveUri, archiveIdFromDocumentId.mAccessMode, null);
                this.mArchives.put(keyFromArchiveId, loader);
            }
            loader.acquire();
            this.mArchives.put(keyFromArchiveId, loader);
        }
    }

    private void releaseArchive(String str) {
        Key keyFromArchiveId = Key.fromArchiveId(ArchiveId.fromDocumentId(str));
        synchronized (this.mArchives) {
            Loader loader = this.mArchives.get(keyFromArchiveId);
            loader.release();
            int status = loader.getStatus();
            if (status == 4 || status == 3) {
                this.mArchives.remove(keyFromArchiveId);
            }
        }
    }

    private Loader getLoaderOrThrow(String str) {
        Loader loader;
        Key keyFromArchiveId = Key.fromArchiveId(ArchiveId.fromDocumentId(str));
        synchronized (this.mArchives) {
            loader = this.mArchives.get(keyFromArchiveId);
            if (loader == null) {
                throw new IllegalStateException("Archive not acquired.");
            }
        }
        return loader;
    }

    private static class Key {
        int accessMode;
        Uri archiveUri;

        public Key(Uri uri, int i) {
            this.archiveUri = uri;
            this.accessMode = i;
        }

        public static Key fromArchiveId(ArchiveId archiveId) {
            return new Key(archiveId.mArchiveUri, archiveId.mAccessMode);
        }

        public boolean equals(Object obj) {
            if (obj == null || !(obj instanceof Key)) {
                return false;
            }
            Key key = (Key) obj;
            return this.archiveUri.equals(key.archiveUri) && this.accessMode == key.accessMode;
        }

        public int hashCode() {
            return Objects.hash(this.archiveUri, Integer.valueOf(this.accessMode));
        }
    }
}
