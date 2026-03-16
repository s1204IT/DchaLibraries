package com.android.providers.contacts;

import android.content.ContentValues;

public interface DatabaseModifier {
    int delete(String str, String str2, String[] strArr);

    long insert(ContentValues contentValues);

    long insert(String str, String str2, ContentValues contentValues);

    int update(String str, ContentValues contentValues, String str2, String[] strArr);
}
