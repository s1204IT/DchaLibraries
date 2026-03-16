package com.android.providers.contacts.debug;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import java.io.File;
import java.io.FileNotFoundException;

public class DumpFileProvider extends ContentProvider {
    public static final Uri AUTHORITY_URI = Uri.parse("content://com.android.contacts.dumpfile");

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getType(Uri uri) {
        return "application/zip";
    }

    private static String extractFileName(Uri uri) {
        String path = uri.getPath();
        return path.startsWith("/") ? path.substring(1) : path;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) {
            throw new UnsupportedOperationException();
        }
        String fileName = extractFileName(uri);
        DataExporter.ensureValidFileName(fileName);
        File file = DataExporter.getOutputFile(getContext(), fileName);
        return ParcelFileDescriptor.open(file, 268435456);
    }

    @Override
    public Cursor query(Uri uri, String[] inProjection, String selection, String[] selectionArgs, String sortOrder) {
        String fileName = extractFileName(uri);
        DataExporter.ensureValidFileName(fileName);
        String[] projection = inProjection != null ? inProjection : new String[]{"_display_name", "_size"};
        MatrixCursor c = new MatrixCursor(projection);
        MatrixCursor.RowBuilder b = c.newRow();
        for (int i = 0; i < c.getColumnCount(); i++) {
            String column = projection[i];
            if ("_display_name".equals(column)) {
                b.add(fileName);
            } else if ("_size".equals(column)) {
                File file = DataExporter.getOutputFile(getContext(), fileName);
                if (file.exists()) {
                    b.add(Long.valueOf(file.length()));
                } else {
                    b.add(null);
                }
            } else {
                throw new IllegalArgumentException("Unknown column " + column);
            }
        }
        return c;
    }
}
