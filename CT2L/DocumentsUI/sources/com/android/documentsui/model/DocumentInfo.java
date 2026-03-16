package com.android.documentsui.model;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.DocumentsContract;
import android.text.TextUtils;
import com.android.documentsui.DocumentsApplication;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ProtocolException;
import java.text.Collator;
import libcore.io.IoUtils;

public class DocumentInfo implements Parcelable, Durable {
    public static final Parcelable.Creator<DocumentInfo> CREATOR;
    private static final Collator sCollator = Collator.getInstance();
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

    static {
        sCollator.setStrength(1);
        CREATOR = new Parcelable.Creator<DocumentInfo>() {
            @Override
            public DocumentInfo createFromParcel(Parcel in) {
                DocumentInfo doc = new DocumentInfo();
                DurableUtils.readFromParcel(in, doc);
                return doc;
            }

            @Override
            public DocumentInfo[] newArray(int size) {
                return new DocumentInfo[size];
            }
        };
    }

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
    public void read(DataInputStream in) throws IOException {
        int version = in.readInt();
        switch (version) {
            case 1:
                throw new ProtocolException("Ignored upgrade");
            case 2:
                this.authority = DurableUtils.readNullableString(in);
                this.documentId = DurableUtils.readNullableString(in);
                this.mimeType = DurableUtils.readNullableString(in);
                this.displayName = DurableUtils.readNullableString(in);
                this.lastModified = in.readLong();
                this.flags = in.readInt();
                this.summary = DurableUtils.readNullableString(in);
                this.size = in.readLong();
                this.icon = in.readInt();
                deriveFields();
                return;
            default:
                throw new ProtocolException("Unknown version " + version);
        }
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeInt(2);
        DurableUtils.writeNullableString(out, this.authority);
        DurableUtils.writeNullableString(out, this.documentId);
        DurableUtils.writeNullableString(out, this.mimeType);
        DurableUtils.writeNullableString(out, this.displayName);
        out.writeLong(this.lastModified);
        out.writeInt(this.flags);
        DurableUtils.writeNullableString(out, this.summary);
        out.writeLong(this.size);
        out.writeInt(this.icon);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        DurableUtils.writeToParcel(dest, this);
    }

    public static DocumentInfo fromDirectoryCursor(Cursor cursor) {
        String authority = getCursorString(cursor, "android:authority");
        return fromCursor(cursor, authority);
    }

    public static DocumentInfo fromCursor(Cursor cursor, String authority) {
        DocumentInfo info = new DocumentInfo();
        info.updateFromCursor(cursor, authority);
        return info;
    }

    public void updateFromCursor(Cursor cursor, String authority) {
        this.authority = authority;
        this.documentId = getCursorString(cursor, "document_id");
        this.mimeType = getCursorString(cursor, "mime_type");
        this.documentId = getCursorString(cursor, "document_id");
        this.mimeType = getCursorString(cursor, "mime_type");
        this.displayName = getCursorString(cursor, "_display_name");
        this.lastModified = getCursorLong(cursor, "last_modified");
        this.flags = getCursorInt(cursor, "flags");
        this.summary = getCursorString(cursor, "summary");
        this.size = getCursorLong(cursor, "_size");
        this.icon = getCursorInt(cursor, "icon");
        deriveFields();
    }

    public static DocumentInfo fromUri(ContentResolver resolver, Uri uri) throws FileNotFoundException {
        DocumentInfo info = new DocumentInfo();
        info.updateFromUri(resolver, uri);
        return info;
    }

    public void updateSelf(ContentResolver resolver) throws FileNotFoundException {
        updateFromUri(resolver, this.derivedUri);
    }

    public void updateFromUri(ContentResolver resolver, Uri uri) throws FileNotFoundException {
        ContentProviderClient client = null;
        Cursor cursor = null;
        try {
            try {
                client = DocumentsApplication.acquireUnstableProviderOrThrow(resolver, uri.getAuthority());
                cursor = client.query(uri, null, null, null, null);
                if (!cursor.moveToFirst()) {
                    throw new FileNotFoundException("Missing details for " + uri);
                }
                updateFromCursor(cursor, uri.getAuthority());
            } catch (Throwable t) {
                throw asFileNotFoundException(t);
            }
        } finally {
            IoUtils.closeQuietly(cursor);
            ContentProviderClient.releaseQuietly(client);
        }
    }

    private void deriveFields() {
        this.derivedUri = DocumentsContract.buildDocumentUri(this.authority, this.documentId);
    }

    public String toString() {
        return "Document{docId=" + this.documentId + ", name=" + this.displayName + "}";
    }

    public boolean isCreateSupported() {
        return (this.flags & 8) != 0;
    }

    public boolean isDirectory() {
        return "vnd.android.document/directory".equals(this.mimeType);
    }

    public boolean isDeleteSupported() {
        return (this.flags & 4) != 0;
    }

    public boolean isGridTitlesHidden() {
        return (this.flags & 65536) != 0;
    }

    public static String getCursorString(Cursor cursor, String columnName) {
        int index = cursor.getColumnIndex(columnName);
        if (index != -1) {
            return cursor.getString(index);
        }
        return null;
    }

    public static long getCursorLong(Cursor cursor, String columnName) {
        String value;
        int index = cursor.getColumnIndex(columnName);
        if (index == -1 || (value = cursor.getString(index)) == null) {
            return -1L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return -1L;
        }
    }

    public static int getCursorInt(Cursor cursor, String columnName) {
        int index = cursor.getColumnIndex(columnName);
        if (index != -1) {
            return cursor.getInt(index);
        }
        return 0;
    }

    public static FileNotFoundException asFileNotFoundException(Throwable t) throws Throwable {
        if (t instanceof FileNotFoundException) {
            throw ((FileNotFoundException) t);
        }
        FileNotFoundException fnfe = new FileNotFoundException(t.getMessage());
        fnfe.initCause(t);
        throw fnfe;
    }

    public static int compareToIgnoreCaseNullable(String lhs, String rhs) {
        boolean leftEmpty = TextUtils.isEmpty(lhs);
        boolean rightEmpty = TextUtils.isEmpty(rhs);
        if (leftEmpty && rightEmpty) {
            return 0;
        }
        if (leftEmpty) {
            return -1;
        }
        if (rightEmpty) {
            return 1;
        }
        boolean leftDir = lhs.charAt(0) == 1;
        boolean rightDir = rhs.charAt(0) == 1;
        if (leftDir && !rightDir) {
            return -1;
        }
        if (!rightDir || leftDir) {
            return sCollator.compare(lhs, rhs);
        }
        return 1;
    }
}
