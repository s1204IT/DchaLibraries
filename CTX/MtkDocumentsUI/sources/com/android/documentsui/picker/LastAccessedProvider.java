package com.android.documentsui.picker;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.UriMatcher;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.base.DocumentStack;
import com.android.documentsui.base.DurableUtils;
import com.google.android.collect.Sets;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import libcore.io.IoUtils;

public class LastAccessedProvider extends ContentProvider {
    private static final UriMatcher sMatcher = new UriMatcher(-1);
    private DatabaseHelper mHelper;

    static {
        sMatcher.addURI("com.android.documentsui.lastAccessed", "lastAccessed/*", 1);
    }

    public static Uri buildLastAccessed(String str) {
        return new Uri.Builder().scheme("content").authority("com.android.documentsui.lastAccessed").appendPath("lastAccessed").appendPath(str).build();
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {
        public DatabaseHelper(Context context) {
            super(context, "lastAccess.db", (SQLiteDatabase.CursorFactory) null, 6);
        }

        @Override
        public void onCreate(SQLiteDatabase sQLiteDatabase) {
            sQLiteDatabase.execSQL("CREATE TABLE lastAccessed (package_name TEXT NOT NULL PRIMARY KEY,stack BLOB DEFAULT NULL,timestamp INTEGER,external INTEGER NOT NULL DEFAULT 0)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
            Log.w("LastAccessedProvider", "Upgrading database; wiping app data");
            sQLiteDatabase.execSQL("DROP TABLE IF EXISTS lastAccessed");
            onCreate(sQLiteDatabase);
        }
    }

    @Deprecated
    static void setLastAccessed(ContentResolver contentResolver, String str, DocumentStack documentStack) {
        ContentValues contentValues = new ContentValues();
        byte[] bArrWriteToArrayOrNull = DurableUtils.writeToArrayOrNull(documentStack);
        contentValues.clear();
        contentValues.put("stack", bArrWriteToArrayOrNull);
        contentValues.put("external", (Integer) 0);
        contentResolver.insert(buildLastAccessed(str), contentValues);
    }

    @Override
    public boolean onCreate() {
        this.mHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        if (sMatcher.match(uri) != 1) {
            throw new UnsupportedOperationException("Unsupported Uri " + uri);
        }
        return this.mHelper.getReadableDatabase().query("lastAccessed", strArr, "package_name=?", new String[]{uri.getPathSegments().get(1)}, null, null, str2);
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        if (sMatcher.match(uri) != 1) {
            throw new UnsupportedOperationException("Unsupported Uri " + uri);
        }
        SQLiteDatabase writableDatabase = this.mHelper.getWritableDatabase();
        ContentValues contentValues2 = new ContentValues();
        contentValues.put("timestamp", Long.valueOf(System.currentTimeMillis()));
        String str = uri.getPathSegments().get(1);
        contentValues2.put("package_name", str);
        writableDatabase.insertWithOnConflict("lastAccessed", null, contentValues2, 4);
        writableDatabase.update("lastAccessed", contentValues, "package_name=?", new String[]{str});
        return uri;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        throw new UnsupportedOperationException("Unsupported Uri " + uri);
    }

    @Override
    public int delete(Uri uri, String str, String[] strArr) {
        throw new UnsupportedOperationException("Unsupported Uri " + uri);
    }

    @Override
    public Bundle call(String str, String str2, Bundle bundle) {
        if ("purge".equals(str)) {
            Intent intent = new Intent("android.content.action.DOCUMENTS_PROVIDER");
            final HashSet hashSetNewHashSet = Sets.newHashSet();
            List<ResolveInfo> listQueryIntentContentProviders = getContext().getPackageManager().queryIntentContentProviders(intent, 0);
            if (listQueryIntentContentProviders == null) {
                return null;
            }
            Iterator<ResolveInfo> it = listQueryIntentContentProviders.iterator();
            while (it.hasNext()) {
                hashSetNewHashSet.add(it.next().providerInfo.authority);
            }
            purgeByAuthority(new Predicate<String>() {
                @Override
                public boolean test(String str3) {
                    return !hashSetNewHashSet.contains(str3);
                }
            });
            return null;
        }
        if ("purgePackage".equals(str)) {
            Intent intent2 = new Intent("android.content.action.DOCUMENTS_PROVIDER");
            intent2.setPackage(str2);
            final HashSet hashSetNewHashSet2 = Sets.newHashSet();
            List<ResolveInfo> listQueryIntentContentProviders2 = getContext().getPackageManager().queryIntentContentProviders(intent2, 0);
            if (listQueryIntentContentProviders2 == null) {
                return null;
            }
            Iterator<ResolveInfo> it2 = listQueryIntentContentProviders2.iterator();
            while (it2.hasNext()) {
                hashSetNewHashSet2.add(it2.next().providerInfo.authority);
            }
            if (!hashSetNewHashSet2.isEmpty()) {
                purgeByAuthority(new Predicate<String>() {
                    @Override
                    public boolean test(String str3) {
                        return hashSetNewHashSet2.contains(str3);
                    }
                });
            }
            return null;
        }
        return super.call(str, str2, bundle);
    }

    private void purgeByAuthority(Predicate<String> predicate) {
        SQLiteDatabase writableDatabase = this.mHelper.getWritableDatabase();
        DocumentStack documentStack = new DocumentStack();
        Cursor cursorQuery = writableDatabase.query("lastAccessed", null, null, null, null, null, null);
        while (cursorQuery.moveToNext()) {
            try {
                try {
                    DurableUtils.readFromArray(cursorQuery.getBlob(cursorQuery.getColumnIndex("stack")), documentStack);
                    if (documentStack.getRoot() != null && predicate.test(documentStack.getRoot().authority)) {
                        writableDatabase.delete("lastAccessed", "package_name=?", new String[]{DocumentInfo.getCursorString(cursorQuery, "package_name")});
                    }
                } catch (IOException e) {
                }
            } finally {
                IoUtils.closeQuietly(cursorQuery);
            }
        }
    }
}
