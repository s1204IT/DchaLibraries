package com.android.documentsui;

import android.content.ContentProviderClient;
import android.database.Cursor;
import libcore.io.IoUtils;

class DirectoryResult implements AutoCloseable {
    ContentProviderClient client;
    Cursor cursor;
    Exception exception;
    int mode = 0;
    int sortOrder = 0;

    DirectoryResult() {
    }

    @Override
    public void close() {
        IoUtils.closeQuietly(this.cursor);
        ContentProviderClient.releaseQuietly(this.client);
        this.cursor = null;
        this.client = null;
    }
}
