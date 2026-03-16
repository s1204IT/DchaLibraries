package com.android.documentsui;

import android.content.ContentProvider;
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
import com.android.documentsui.model.DocumentInfo;
import com.android.documentsui.model.DocumentStack;
import com.android.documentsui.model.DurableUtils;
import com.android.internal.util.Predicate;
import com.google.android.collect.Sets;
import java.io.IOException;
import java.util.Set;
import libcore.io.IoUtils;

public class RecentsProvider extends ContentProvider {
    private static final UriMatcher sMatcher = new UriMatcher(-1);
    private DatabaseHelper mHelper;

    static {
        sMatcher.addURI("com.android.documentsui.recents", "recent", 1);
        sMatcher.addURI("com.android.documentsui.recents", "state/*/*/*", 2);
        sMatcher.addURI("com.android.documentsui.recents", "resume/*", 3);
    }

    public static Uri buildRecent() {
        return new Uri.Builder().scheme("content").authority("com.android.documentsui.recents").appendPath("recent").build();
    }

    public static Uri buildState(String authority, String rootId, String documentId) {
        return new Uri.Builder().scheme("content").authority("com.android.documentsui.recents").appendPath("state").appendPath(authority).appendPath(rootId).appendPath(documentId).build();
    }

    public static Uri buildResume(String packageName) {
        return new Uri.Builder().scheme("content").authority("com.android.documentsui.recents").appendPath("resume").appendPath(packageName).build();
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {
        public DatabaseHelper(Context context) {
            super(context, "recents.db", (SQLiteDatabase.CursorFactory) null, 5);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE recent (key TEXT PRIMARY KEY ON CONFLICT REPLACE,stack BLOB DEFAULT NULL,timestamp INTEGER)");
            db.execSQL("CREATE TABLE state (authority TEXT,root_id TEXT,document_id TEXT,mode INTEGER,sortOrder INTEGER,PRIMARY KEY (authority, root_id, document_id))");
            db.execSQL("CREATE TABLE resume (package_name TEXT NOT NULL PRIMARY KEY,stack BLOB DEFAULT NULL,timestamp INTEGER,external INTEGER NOT NULL DEFAULT 0)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w("RecentsProvider", "Upgrading database; wiping app data");
            db.execSQL("DROP TABLE IF EXISTS recent");
            db.execSQL("DROP TABLE IF EXISTS state");
            db.execSQL("DROP TABLE IF EXISTS resume");
            onCreate(db);
        }
    }

    @Override
    public boolean onCreate() {
        this.mHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteDatabase db = this.mHelper.getReadableDatabase();
        switch (sMatcher.match(uri)) {
            case 1:
                long cutoff = System.currentTimeMillis() - 3888000000L;
                return db.query("recent", projection, "timestamp>" + cutoff, null, null, null, sortOrder);
            case 2:
                String authority = uri.getPathSegments().get(1);
                String rootId = uri.getPathSegments().get(2);
                String documentId = uri.getPathSegments().get(3);
                return db.query("state", projection, "authority=? AND root_id=? AND document_id=?", new String[]{authority, rootId, documentId}, null, null, sortOrder);
            case 3:
                String packageName = uri.getPathSegments().get(1);
                return db.query("resume", projection, "package_name=?", new String[]{packageName}, null, null, sortOrder);
            default:
                throw new UnsupportedOperationException("Unsupported Uri " + uri);
        }
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase db = this.mHelper.getWritableDatabase();
        ContentValues key = new ContentValues();
        switch (sMatcher.match(uri)) {
            case 1:
                values.put("timestamp", Long.valueOf(System.currentTimeMillis()));
                db.insert("recent", null, values);
                long cutoff = System.currentTimeMillis() - 3888000000L;
                db.delete("recent", "timestamp<" + cutoff, null);
                return uri;
            case 2:
                String authority = uri.getPathSegments().get(1);
                String rootId = uri.getPathSegments().get(2);
                String documentId = uri.getPathSegments().get(3);
                key.put("authority", authority);
                key.put("root_id", rootId);
                key.put("document_id", documentId);
                db.insertWithOnConflict("state", null, key, 4);
                db.update("state", values, "authority=? AND root_id=? AND document_id=?", new String[]{authority, rootId, documentId});
                return uri;
            case 3:
                values.put("timestamp", Long.valueOf(System.currentTimeMillis()));
                String packageName = uri.getPathSegments().get(1);
                key.put("package_name", packageName);
                db.insertWithOnConflict("resume", null, key, 4);
                db.update("resume", values, "package_name=?", new String[]{packageName});
                return uri;
            default:
                throw new UnsupportedOperationException("Unsupported Uri " + uri);
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Unsupported Uri " + uri);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Unsupported Uri " + uri);
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if ("purge".equals(method)) {
            Intent intent = new Intent("android.content.action.DOCUMENTS_PROVIDER");
            final Set<String> knownAuth = Sets.newHashSet();
            for (ResolveInfo info : getContext().getPackageManager().queryIntentContentProviders(intent, 0)) {
                knownAuth.add(info.providerInfo.authority);
            }
            purgeByAuthority(new Predicate<String>() {
                public boolean apply(String authority) {
                    return !knownAuth.contains(authority);
                }
            });
            return null;
        }
        if ("purgePackage".equals(method)) {
            Intent intent2 = new Intent("android.content.action.DOCUMENTS_PROVIDER");
            intent2.setPackage(arg);
            final Set<String> packageAuth = Sets.newHashSet();
            for (ResolveInfo info2 : getContext().getPackageManager().queryIntentContentProviders(intent2, 0)) {
                packageAuth.add(info2.providerInfo.authority);
            }
            if (packageAuth.isEmpty()) {
                return null;
            }
            purgeByAuthority(new Predicate<String>() {
                public boolean apply(String authority) {
                    return packageAuth.contains(authority);
                }
            });
            return null;
        }
        return super.call(method, arg, extras);
    }

    private void purgeByAuthority(Predicate<String> predicate) {
        SQLiteDatabase db = this.mHelper.getWritableDatabase();
        DocumentStack stack = new DocumentStack();
        Cursor cursor = db.query("recent", null, null, null, null, null, null);
        while (cursor.moveToNext()) {
            try {
                try {
                    byte[] rawStack = cursor.getBlob(cursor.getColumnIndex("stack"));
                    DurableUtils.readFromArray(rawStack, stack);
                    if (stack.root != null && predicate.apply(stack.root.authority)) {
                        String key = DocumentInfo.getCursorString(cursor, "key");
                        db.delete("recent", "key=?", new String[]{key});
                    }
                } catch (IOException e) {
                }
            } finally {
            }
        }
        IoUtils.closeQuietly(cursor);
        cursor = db.query("state", new String[]{"authority"}, null, null, "authority", null, null);
        while (cursor.moveToNext()) {
            try {
                String authority = DocumentInfo.getCursorString(cursor, "authority");
                if (predicate.apply(authority)) {
                    db.delete("state", "authority=?", new String[]{authority});
                    Log.d("RecentsProvider", "Purged state for " + authority);
                }
            } finally {
            }
        }
        IoUtils.closeQuietly(cursor);
        cursor = db.query("resume", null, null, null, null, null, null);
        while (cursor.moveToNext()) {
            try {
                try {
                    byte[] rawStack2 = cursor.getBlob(cursor.getColumnIndex("stack"));
                    DurableUtils.readFromArray(rawStack2, stack);
                    if (stack.root != null && predicate.apply(stack.root.authority)) {
                        String packageName = DocumentInfo.getCursorString(cursor, "package_name");
                        db.delete("resume", "package_name=?", new String[]{packageName});
                    }
                } catch (IOException e2) {
                }
            } finally {
            }
        }
    }
}
