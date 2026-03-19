package com.android.documentsui.archives;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Point;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.MetadataReader;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.Preconditions;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;

public abstract class Archive implements Closeable {
    public static final String[] DEFAULT_PROJECTION = {"document_id", "_display_name", "mime_type", "_size", "flags"};
    final int mAccessMode;
    final Uri mArchiveUri;
    final Context mContext;
    final Uri mNotificationUri;

    @GuardedBy("mEntries")
    final Map<String, List<ZipEntry>> mTree = new HashMap();

    @GuardedBy("mEntries")
    final Map<String, ZipEntry> mEntries = new HashMap();

    Archive(Context context, Uri uri, int i, Uri uri2) {
        this.mContext = context;
        this.mArchiveUri = uri;
        this.mAccessMode = i;
        this.mNotificationUri = uri2;
    }

    public static String getEntryPath(ZipEntry zipEntry) {
        Preconditions.checkArgument(zipEntry.isDirectory() == zipEntry.getName().endsWith("/"), "Ill-formated ZIP-file.");
        if (zipEntry.getName().startsWith("/")) {
            return zipEntry.getName();
        }
        return "/" + zipEntry.getName();
    }

    public static boolean canSeek(ParcelFileDescriptor parcelFileDescriptor) {
        try {
            return Os.lseek(parcelFileDescriptor.getFileDescriptor(), 0L, OsConstants.SEEK_CUR) == 0;
        } catch (ErrnoException e) {
            return false;
        }
    }

    public Cursor queryChildDocuments(String str, String[] strArr, String str2) throws FileNotFoundException {
        ArchiveId archiveIdFromDocumentId = ArchiveId.fromDocumentId(str);
        MorePreconditions.checkArgumentEquals(this.mArchiveUri, archiveIdFromDocumentId.mArchiveUri, "Mismatching archive Uri. Expected: %s, actual: %s.");
        if (strArr == null) {
            strArr = DEFAULT_PROJECTION;
        }
        MatrixCursor matrixCursor = new MatrixCursor(strArr);
        if (this.mNotificationUri != null) {
            matrixCursor.setNotificationUri(this.mContext.getContentResolver(), this.mNotificationUri);
        }
        synchronized (this.mEntries) {
            List<ZipEntry> list = this.mTree.get(archiveIdFromDocumentId.mPath);
            if (list == null) {
                throw new FileNotFoundException();
            }
            Iterator<ZipEntry> it = list.iterator();
            while (it.hasNext()) {
                addCursorRow(matrixCursor, it.next());
            }
        }
        return matrixCursor;
    }

    public String getDocumentType(String str) throws FileNotFoundException {
        String mimeTypeForEntry;
        ArchiveId archiveIdFromDocumentId = ArchiveId.fromDocumentId(str);
        MorePreconditions.checkArgumentEquals(this.mArchiveUri, archiveIdFromDocumentId.mArchiveUri, "Mismatching archive Uri. Expected: %s, actual: %s.");
        synchronized (this.mEntries) {
            ZipEntry zipEntry = this.mEntries.get(archiveIdFromDocumentId.mPath);
            if (zipEntry == null) {
                throw new FileNotFoundException();
            }
            mimeTypeForEntry = getMimeTypeForEntry(zipEntry);
        }
        return mimeTypeForEntry;
    }

    public boolean isChildDocument(String str, String str2) {
        String entryPath;
        ArchiveId archiveIdFromDocumentId = ArchiveId.fromDocumentId(str);
        ArchiveId archiveIdFromDocumentId2 = ArchiveId.fromDocumentId(str2);
        MorePreconditions.checkArgumentEquals(this.mArchiveUri, archiveIdFromDocumentId.mArchiveUri, "Mismatching archive Uri. Expected: %s, actual: %s.");
        synchronized (this.mEntries) {
            ZipEntry zipEntry = this.mEntries.get(archiveIdFromDocumentId2.mPath);
            boolean z = false;
            if (zipEntry == null) {
                return false;
            }
            ZipEntry zipEntry2 = this.mEntries.get(archiveIdFromDocumentId.mPath);
            if (zipEntry2 != null && zipEntry2.isDirectory()) {
                if (zipEntry.isDirectory()) {
                    entryPath = getEntryPath(zipEntry);
                } else {
                    entryPath = getEntryPath(zipEntry) + "/";
                }
                if (entryPath.startsWith(archiveIdFromDocumentId.mPath) && !archiveIdFromDocumentId.mPath.equals(entryPath)) {
                    z = true;
                }
                return z;
            }
            return false;
        }
    }

    public Cursor queryDocument(String str, String[] strArr) throws FileNotFoundException {
        MatrixCursor matrixCursor;
        ArchiveId archiveIdFromDocumentId = ArchiveId.fromDocumentId(str);
        MorePreconditions.checkArgumentEquals(this.mArchiveUri, archiveIdFromDocumentId.mArchiveUri, "Mismatching archive Uri. Expected: %s, actual: %s.");
        synchronized (this.mEntries) {
            ZipEntry zipEntry = this.mEntries.get(archiveIdFromDocumentId.mPath);
            if (zipEntry == null) {
                throw new FileNotFoundException();
            }
            if (strArr == null) {
                strArr = DEFAULT_PROJECTION;
            }
            matrixCursor = new MatrixCursor(strArr);
            if (this.mNotificationUri != null) {
                matrixCursor.setNotificationUri(this.mContext.getContentResolver(), this.mNotificationUri);
            }
            addCursorRow(matrixCursor, zipEntry);
        }
        return matrixCursor;
    }

    public String createDocument(String str, String str2, String str3) throws FileNotFoundException {
        throw new UnsupportedOperationException("Creating documents not supported.");
    }

    public ParcelFileDescriptor openDocument(String str, String str2, CancellationSignal cancellationSignal) throws FileNotFoundException {
        throw new UnsupportedOperationException("Opening not supported.");
    }

    public AssetFileDescriptor openDocumentThumbnail(String str, Point point, CancellationSignal cancellationSignal) throws FileNotFoundException {
        throw new UnsupportedOperationException("Thumbnails not supported.");
    }

    public ArchiveId createArchiveId(String str) {
        return new ArchiveId(this.mArchiveUri, this.mAccessMode, str);
    }

    void addCursorRow(MatrixCursor matrixCursor, ZipEntry zipEntry) {
        MatrixCursor.RowBuilder rowBuilderNewRow = matrixCursor.newRow();
        rowBuilderNewRow.add("document_id", createArchiveId(getEntryPath(zipEntry)).toDocumentId());
        rowBuilderNewRow.add("_display_name", new File(zipEntry.getName()).getName());
        rowBuilderNewRow.add("_size", Long.valueOf(zipEntry.getSize()));
        String mimeTypeForEntry = getMimeTypeForEntry(zipEntry);
        rowBuilderNewRow.add("mime_type", mimeTypeForEntry);
        boolean zStartsWith = mimeTypeForEntry.startsWith("image/");
        int i = zStartsWith;
        if (MetadataReader.isSupportedMimeType(mimeTypeForEntry)) {
            i = (zStartsWith ? 1 : 0) | 131072;
        }
        rowBuilderNewRow.add("flags", Integer.valueOf(i));
    }

    static String getMimeTypeForEntry(ZipEntry zipEntry) {
        if (zipEntry.isDirectory()) {
            return "vnd.android.document/directory";
        }
        int iLastIndexOf = zipEntry.getName().lastIndexOf(46);
        if (iLastIndexOf >= 0) {
            String mimeTypeFromExtension = MimeTypeMap.getSingleton().getMimeTypeFromExtension(zipEntry.getName().substring(iLastIndexOf + 1).toLowerCase(Locale.US));
            if (mimeTypeFromExtension != null) {
                return mimeTypeFromExtension;
            }
            return "application/octet-stream";
        }
        return "application/octet-stream";
    }

    public static class MorePreconditions {
        static void checkArgumentEquals(String str, String str2, String str3) {
            if (!TextUtils.equals(str, str2)) {
                throw new IllegalArgumentException(String.format(str3, String.valueOf(str), String.valueOf(str2)));
            }
        }

        static void checkArgumentEquals(Uri uri, Uri uri2, String str) {
            checkArgumentEquals(uri.toString(), uri2.toString(), str);
        }
    }
}
