package com.android.documentsui.base;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.DocumentsContract;
import com.android.documentsui.DocumentsApplication;
import com.android.documentsui.archives.ArchivesProvider;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ProtocolException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import libcore.io.IoUtils;

public class DocumentInfo implements Parcelable, Durable {
    static final boolean $assertionsDisabled = false;
    public static final Parcelable.Creator<DocumentInfo> CREATOR = new Parcelable.Creator<DocumentInfo>() {
        @Override
        public DocumentInfo createFromParcel(Parcel parcel) {
            DocumentInfo documentInfo = new DocumentInfo();
            DurableUtils.readFromParcel(parcel, documentInfo);
            return documentInfo;
        }

        @Override
        public DocumentInfo[] newArray(int i) {
            return new DocumentInfo[i];
        }
    };
    public String authority;
    public Uri derivedUri;
    public String displayName;
    public String documentId;
    public int flags;
    public int icon;
    public long lastModified;
    public String mimeType;
    public long size;
    public String summary;
    public boolean isDrm = false;
    public int drmMethod = 0;
    public String data = null;

    public DocumentInfo() {
        reset();
    }

    @Override
    public void reset() {
        this.authority = null;
        this.documentId = null;
        this.mimeType = null;
        this.displayName = null;
        this.lastModified = -1L;
        this.flags = 0;
        this.summary = null;
        this.size = -1L;
        this.icon = 0;
        this.derivedUri = null;
    }

    @Override
    public void read(DataInputStream dataInputStream) throws IOException {
        int i = dataInputStream.readInt();
        switch (i) {
            case 1:
                throw new ProtocolException("Ignored upgrade");
            case 2:
                this.authority = DurableUtils.readNullableString(dataInputStream);
                this.documentId = DurableUtils.readNullableString(dataInputStream);
                this.mimeType = DurableUtils.readNullableString(dataInputStream);
                this.displayName = DurableUtils.readNullableString(dataInputStream);
                this.lastModified = dataInputStream.readLong();
                this.flags = dataInputStream.readInt();
                this.summary = DurableUtils.readNullableString(dataInputStream);
                this.size = dataInputStream.readLong();
                this.icon = dataInputStream.readInt();
                deriveFields();
                return;
            default:
                throw new ProtocolException("Unknown version " + i);
        }
    }

    @Override
    public void write(DataOutputStream dataOutputStream) throws IOException {
        dataOutputStream.writeInt(2);
        DurableUtils.writeNullableString(dataOutputStream, this.authority);
        DurableUtils.writeNullableString(dataOutputStream, this.documentId);
        DurableUtils.writeNullableString(dataOutputStream, this.mimeType);
        DurableUtils.writeNullableString(dataOutputStream, this.displayName);
        dataOutputStream.writeLong(this.lastModified);
        dataOutputStream.writeInt(this.flags);
        DurableUtils.writeNullableString(dataOutputStream, this.summary);
        dataOutputStream.writeLong(this.size);
        dataOutputStream.writeInt(this.icon);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        DurableUtils.writeToParcel(parcel, this);
    }

    public static DocumentInfo fromDirectoryCursor(Cursor cursor) {
        return fromCursor(cursor, getCursorString(cursor, "android:authority"));
    }

    public static DocumentInfo fromCursor(Cursor cursor, String str) {
        DocumentInfo documentInfo = new DocumentInfo();
        documentInfo.updateFromCursor(cursor, str);
        return documentInfo;
    }

    public void updateFromCursor(Cursor cursor, String str) {
        this.authority = str;
        this.documentId = getCursorString(cursor, "document_id");
        this.mimeType = getCursorString(cursor, "mime_type");
        this.displayName = getCursorString(cursor, "_display_name");
        this.lastModified = getCursorLong(cursor, "last_modified");
        this.flags = getCursorInt(cursor, "flags");
        this.summary = getCursorString(cursor, "summary");
        this.size = getCursorLong(cursor, "_size");
        this.icon = getCursorInt(cursor, "icon");
        deriveFields();
        this.isDrm = getCursorInt(cursor, "is_drm") > 0;
        this.drmMethod = getCursorInt(cursor, "drm_method");
        this.data = getCursorString(cursor, "_data");
    }

    public static DocumentInfo fromUri(ContentResolver contentResolver, Uri uri) throws Throwable {
        DocumentInfo documentInfo = new DocumentInfo();
        documentInfo.updateFromUri(contentResolver, uri);
        return documentInfo;
    }

    public void updateSelf(ContentResolver contentResolver) throws Throwable {
        updateFromUri(contentResolver, this.derivedUri);
    }

    public void updateFromUri(ContentResolver contentResolver, Uri uri) throws Throwable {
        ContentProviderClient contentProviderClientAcquireUnstableProviderOrThrow;
        Cursor cursorQuery;
        ContentProviderClient contentProviderClient = null;
        try {
            contentProviderClientAcquireUnstableProviderOrThrow = DocumentsApplication.acquireUnstableProviderOrThrow(contentResolver, uri.getAuthority());
            try {
                cursorQuery = contentProviderClientAcquireUnstableProviderOrThrow.query(uri, null, null, null, null);
                try {
                    if (cursorQuery.moveToFirst()) {
                        updateFromCursor(cursorQuery, uri.getAuthority());
                        IoUtils.closeQuietly(cursorQuery);
                        ContentProviderClient.releaseQuietly(contentProviderClientAcquireUnstableProviderOrThrow);
                    } else {
                        throw new FileNotFoundException("Missing details for " + uri);
                    }
                } catch (Throwable th) {
                    th = th;
                    contentProviderClient = contentProviderClientAcquireUnstableProviderOrThrow;
                    try {
                        throw asFileNotFoundException(th);
                    } catch (Throwable th2) {
                        th = th2;
                        contentProviderClientAcquireUnstableProviderOrThrow = contentProviderClient;
                        IoUtils.closeQuietly(cursorQuery);
                        ContentProviderClient.releaseQuietly(contentProviderClientAcquireUnstableProviderOrThrow);
                        throw th;
                    }
                }
            } catch (Throwable th3) {
                th = th3;
                cursorQuery = null;
            }
        } catch (Throwable th4) {
            th = th4;
            contentProviderClientAcquireUnstableProviderOrThrow = null;
            cursorQuery = null;
        }
    }

    void deriveFields() {
        this.derivedUri = DocumentsContract.buildDocumentUri(this.authority, this.documentId);
    }

    public String toString() {
        return "DocumentInfo{docId=" + this.documentId + ", name=" + this.displayName + ", mimeType=" + this.mimeType + ", isContainer=" + isContainer() + ", isDirectory=" + isDirectory() + ", isArchive=" + isArchive() + ", isInArchive=" + isInArchive() + ", isPartial=" + isPartial() + ", isVirtual=" + isVirtual() + ", isDeleteSupported=" + isDeleteSupported() + ", isCreateSupported=" + isCreateSupported() + ", isRenameSupported=" + isRenameSupported() + ", isMetadataSupported=" + isMetadataSupported() + "} @ " + this.derivedUri;
    }

    public boolean isCreateSupported() {
        return (this.flags & 8) != 0;
    }

    public boolean isDeleteSupported() {
        return (this.flags & 4) != 0;
    }

    public boolean isMetadataSupported() {
        return (this.flags & 131072) != 0;
    }

    public boolean isRemoveSupported() {
        return (this.flags & 1024) != 0;
    }

    public boolean isRenameSupported() {
        return (this.flags & 64) != 0;
    }

    public boolean isSettingsSupported() {
        return (this.flags & 2048) != 0;
    }

    public boolean isThumbnailSupported() {
        return (this.flags & 1) != 0;
    }

    public boolean isWeblinkSupported() {
        return (this.flags & 4096) != 0;
    }

    public boolean isWriteSupported() {
        return (this.flags & 2) != 0;
    }

    public boolean isDirectory() {
        return "vnd.android.document/directory".equals(this.mimeType);
    }

    public boolean isArchive() {
        return ArchivesProvider.isSupportedArchiveType(this.mimeType);
    }

    public boolean isInArchive() {
        return "com.android.documentsui.archives".equals(this.authority);
    }

    public boolean isPartial() {
        return (this.flags & 65536) != 0;
    }

    public boolean isContainer() {
        return isDirectory() || !(!isArchive() || isInArchive() || isPartial());
    }

    public boolean isVirtual() {
        return (this.flags & 512) != 0;
    }

    public boolean prefersSortByLastModified() {
        return (this.flags & 32) != 0;
    }

    public int hashCode() {
        return this.derivedUri.hashCode() + this.mimeType.hashCode();
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof DocumentInfo)) {
            return false;
        }
        DocumentInfo documentInfo = (DocumentInfo) obj;
        if (!Objects.equals(this.derivedUri, documentInfo.derivedUri) || !Objects.equals(this.mimeType, documentInfo.mimeType)) {
            return false;
        }
        return true;
    }

    public static String getCursorString(Cursor cursor, String str) {
        int columnIndex;
        if (cursor == null || (columnIndex = cursor.getColumnIndex(str)) == -1 || cursor.isClosed()) {
            return null;
        }
        return cursor.getString(columnIndex);
    }

    public static long getCursorLong(Cursor cursor, String str) {
        int columnIndex = cursor.getColumnIndex(str);
        if (columnIndex == -1) {
            return -1L;
        }
        try {
            String string = cursor.getString(columnIndex);
            if (string == null) {
                return -1L;
            }
            try {
                return Long.parseLong(string);
            } catch (NumberFormatException e) {
                return -1L;
            }
        } catch (NullPointerException e2) {
            e2.printStackTrace();
            return -1L;
        }
    }

    public static int getCursorInt(Cursor cursor, String str) {
        int columnIndex = cursor.getColumnIndex(str);
        if (columnIndex == -1 || cursor.isClosed()) {
            return 0;
        }
        return cursor.getInt(columnIndex);
    }

    public static FileNotFoundException asFileNotFoundException(Throwable th) throws FileNotFoundException {
        if (th instanceof FileNotFoundException) {
            throw ((FileNotFoundException) th);
        }
        FileNotFoundException fileNotFoundException = new FileNotFoundException(th.getMessage());
        fileNotFoundException.initCause(th);
        throw fileNotFoundException;
    }

    public static Uri getUri(Cursor cursor) {
        return DocumentsContract.buildDocumentUri(getCursorString(cursor, "android:authority"), getCursorString(cursor, "document_id"));
    }

    public static void addMimeTypes(ContentResolver contentResolver, Uri uri, Set<String> set) {
        if ("content".equals(uri.getScheme())) {
            set.add(contentResolver.getType(uri));
            String[] streamTypes = contentResolver.getStreamTypes(uri, "*/*");
            if (streamTypes != null) {
                set.addAll(Arrays.asList(streamTypes));
            }
        }
    }

    public static String debugString(DocumentInfo documentInfo) {
        if (documentInfo == null) {
            return "<null DocumentInfo>";
        }
        if (documentInfo.derivedUri == null) {
            documentInfo.deriveFields();
        }
        return documentInfo.derivedUri.toString();
    }
}
