package com.android.server.am;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import java.io.File;
import java.io.FileNotFoundException;

public class DumpHeapProvider extends ContentProvider {
    static File sHeapDumpJavaFile;
    static final Object sLock = new Object();

    public static File getJavaFile() {
        File file;
        synchronized (sLock) {
            file = sHeapDumpJavaFile;
        }
        return file;
    }

    @Override
    public boolean onCreate() {
        synchronized (sLock) {
            File file = new File(new File(Environment.getDataDirectory(), "system"), "heapdump");
            file.mkdir();
            sHeapDumpJavaFile = new File(file, "javaheap.bin");
        }
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return "application/octet-stream";
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        return null;
    }

    @Override
    public int delete(Uri uri, String str, String[] strArr) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        return 0;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String str) throws FileNotFoundException {
        ParcelFileDescriptor parcelFileDescriptorOpen;
        synchronized (sLock) {
            if (Uri.decode(uri.getEncodedPath()).equals("/java")) {
                parcelFileDescriptorOpen = ParcelFileDescriptor.open(sHeapDumpJavaFile, 268435456);
            } else {
                throw new FileNotFoundException("Invalid path for " + uri);
            }
        }
        return parcelFileDescriptorOpen;
    }
}
