package com.android.documentsui;

import android.content.ContentProviderClient;
import android.database.Cursor;
import com.android.documentsui.archives.ArchivesProvider;
import com.android.documentsui.base.DocumentInfo;
import libcore.io.IoUtils;

public class DirectoryResult implements AutoCloseable {
    ContentProviderClient client;
    public Cursor cursor;
    public DocumentInfo doc;
    public Exception exception;

    @Override
    public void close() {
        IoUtils.closeQuietly(this.cursor);
        if (this.client != null && this.doc.isInArchive()) {
            ArchivesProvider.releaseArchive(this.client, this.doc.derivedUri);
        }
        this.cursor = null;
        this.client = null;
        this.doc = null;
    }
}
