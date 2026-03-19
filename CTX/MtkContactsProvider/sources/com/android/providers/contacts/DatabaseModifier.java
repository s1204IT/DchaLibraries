package com.android.providers.contacts;

import android.content.ContentValues;
import android.net.Uri;

public interface DatabaseModifier {
    int delete(String str, String str2, String[] strArr);

    void finishBulkOperation();

    long insert(ContentValues contentValues);

    long insert(String str, String str2, ContentValues contentValues);

    void startBulkOperation();

    int update(Uri uri, String str, ContentValues contentValues, String str2, String[] strArr);

    void yieldBulkOperation();
}
