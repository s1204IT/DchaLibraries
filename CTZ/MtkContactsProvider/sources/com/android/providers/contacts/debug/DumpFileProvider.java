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
    public Uri insert(Uri uri, ContentValues contentValues) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(Uri uri, String str, String[] strArr) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
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
    public ParcelFileDescriptor openFile(Uri uri, String str) throws FileNotFoundException {
        if (!"r".equals(str)) {
            throw new UnsupportedOperationException();
        }
        String strExtractFileName = extractFileName(uri);
        DataExporter.ensureValidFileName(strExtractFileName);
        return ParcelFileDescriptor.open(DataExporter.getOutputFile(getContext(), strExtractFileName), 268435456);
    }

    @Override
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        String strExtractFileName = extractFileName(uri);
        DataExporter.ensureValidFileName(strExtractFileName);
        if (strArr == null) {
            strArr = new String[]{"_display_name", "_size"};
        }
        MatrixCursor matrixCursor = new MatrixCursor(strArr);
        MatrixCursor.RowBuilder rowBuilderNewRow = matrixCursor.newRow();
        for (int i = 0; i < matrixCursor.getColumnCount(); i++) {
            String str3 = strArr[i];
            if ("_display_name".equals(str3)) {
                rowBuilderNewRow.add(strExtractFileName);
            } else if ("_size".equals(str3)) {
                File outputFile = DataExporter.getOutputFile(getContext(), strExtractFileName);
                if (outputFile.exists()) {
                    rowBuilderNewRow.add(Long.valueOf(outputFile.length()));
                } else {
                    rowBuilderNewRow.add(null);
                }
            } else {
                throw new IllegalArgumentException("Unknown column " + str3);
            }
        }
        return matrixCursor;
    }
}
