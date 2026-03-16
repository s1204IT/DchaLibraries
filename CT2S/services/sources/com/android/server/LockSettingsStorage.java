package com.android.server;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.UserInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.os.UserManager;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Slog;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

class LockSettingsStorage {
    private static final String COLUMN_USERID = "user";
    private static final String LOCK_PASSWORD_FILE = "password.key";
    private static final String LOCK_PATTERN_FILE = "gesture.key";
    private static final String SYSTEM_DIRECTORY = "/system/";
    private static final String TABLE = "locksettings";
    private static final String TAG = "LockSettingsStorage";
    private final Context mContext;
    private final DatabaseHelper mOpenHelper;
    private static final String COLUMN_VALUE = "value";
    private static final String[] COLUMNS_FOR_QUERY = {COLUMN_VALUE};
    private static final String COLUMN_KEY = "name";
    private static final String[] COLUMNS_FOR_PREFETCH = {COLUMN_KEY, COLUMN_VALUE};
    private static final Object DEFAULT = new Object();
    private final Cache mCache = new Cache();
    private final Object mFileWriteLock = new Object();

    public interface Callback {
        void initialize(SQLiteDatabase sQLiteDatabase);
    }

    public LockSettingsStorage(Context context, Callback callback) {
        this.mContext = context;
        this.mOpenHelper = new DatabaseHelper(context, callback);
    }

    public void writeKeyValue(String key, String value, int userId) {
        writeKeyValue(this.mOpenHelper.getWritableDatabase(), key, value, userId);
    }

    public void writeKeyValue(SQLiteDatabase db, String key, String value, int userId) {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_KEY, key);
        cv.put(COLUMN_USERID, Integer.valueOf(userId));
        cv.put(COLUMN_VALUE, value);
        db.beginTransaction();
        try {
            db.delete(TABLE, "name=? AND user=?", new String[]{key, Integer.toString(userId)});
            db.insert(TABLE, null, cv);
            db.setTransactionSuccessful();
            this.mCache.putKeyValue(key, value, userId);
        } finally {
            db.endTransaction();
        }
    }

    public String readKeyValue(String key, String defaultValue, int userId) {
        Object obj;
        synchronized (this.mCache) {
            if (this.mCache.hasKeyValue(key, userId)) {
                return this.mCache.peekKeyValue(key, defaultValue, userId);
            }
            int version = this.mCache.getVersion();
            Object result = DEFAULT;
            SQLiteDatabase db = this.mOpenHelper.getReadableDatabase();
            Cursor cursor = db.query(TABLE, COLUMNS_FOR_QUERY, "user=? AND name=?", new String[]{Integer.toString(userId), key}, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    result = cursor.getString(0);
                }
                cursor.close();
                obj = result;
            } else {
                obj = result;
            }
            this.mCache.putKeyValueIfUnchanged(key, obj, userId, version);
            return obj != DEFAULT ? (String) obj : defaultValue;
        }
    }

    public void prefetchUser(int userId) throws Throwable {
        synchronized (this.mCache) {
            if (!this.mCache.isFetched(userId)) {
                this.mCache.setFetched(userId);
                int version = this.mCache.getVersion();
                SQLiteDatabase db = this.mOpenHelper.getReadableDatabase();
                Cursor cursor = db.query(TABLE, COLUMNS_FOR_PREFETCH, "user=?", new String[]{Integer.toString(userId)}, null, null, null);
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        String key = cursor.getString(0);
                        String value = cursor.getString(1);
                        this.mCache.putKeyValueIfUnchanged(key, value, userId, version);
                    }
                    cursor.close();
                }
                readPasswordHash(userId);
                readPatternHash(userId);
            }
        }
    }

    public byte[] readPasswordHash(int userId) throws Throwable {
        byte[] stored = readFile(getLockPasswordFilename(userId));
        if (stored == null || stored.length <= 0) {
            return null;
        }
        return stored;
    }

    public byte[] readPatternHash(int userId) throws Throwable {
        byte[] stored = readFile(getLockPatternFilename(userId));
        if (stored == null || stored.length <= 0) {
            return null;
        }
        return stored;
    }

    public boolean hasPassword(int userId) {
        return hasFile(getLockPasswordFilename(userId));
    }

    public boolean hasPattern(int userId) {
        return hasFile(getLockPatternFilename(userId));
    }

    private boolean hasFile(String name) throws Throwable {
        byte[] contents = readFile(name);
        return contents != null && contents.length > 0;
    }

    private byte[] readFile(String name) throws Throwable {
        byte[] stored;
        RandomAccessFile raf;
        synchronized (this.mCache) {
            if (this.mCache.hasFile(name)) {
                stored = this.mCache.peekFile(name);
            } else {
                int version = this.mCache.getVersion();
                RandomAccessFile raf2 = null;
                stored = null;
                try {
                    try {
                        raf = new RandomAccessFile(name, "r");
                    } catch (Throwable th) {
                        th = th;
                    }
                } catch (IOException e) {
                    e = e;
                }
                try {
                    stored = new byte[(int) raf.length()];
                    raf.readFully(stored, 0, stored.length);
                    raf.close();
                    if (raf != null) {
                        try {
                            raf.close();
                            raf2 = raf;
                        } catch (IOException e2) {
                            Slog.e(TAG, "Error closing file " + e2);
                            raf2 = raf;
                        }
                    } else {
                        raf2 = raf;
                    }
                } catch (IOException e3) {
                    e = e3;
                    raf2 = raf;
                    Slog.e(TAG, "Cannot read file " + e);
                    if (raf2 != null) {
                        try {
                            raf2.close();
                        } catch (IOException e4) {
                            Slog.e(TAG, "Error closing file " + e4);
                        }
                    }
                } catch (Throwable th2) {
                    th = th2;
                    raf2 = raf;
                    if (raf2 != null) {
                        try {
                            raf2.close();
                        } catch (IOException e5) {
                            Slog.e(TAG, "Error closing file " + e5);
                        }
                    }
                    throw th;
                }
                this.mCache.putFileIfUnchanged(name, stored, version);
            }
        }
        return stored;
    }

    private void writeFile(String name, byte[] hash) throws Throwable {
        RandomAccessFile raf;
        synchronized (this.mFileWriteLock) {
            RandomAccessFile raf2 = null;
            try {
                try {
                    try {
                        raf = new RandomAccessFile(name, "rw");
                    } catch (IOException e) {
                        e = e;
                    }
                    if (hash != null) {
                        try {
                            if (hash.length != 0) {
                                raf.write(hash, 0, hash.length);
                            }
                            raf.close();
                            if (raf == null) {
                                try {
                                    try {
                                        raf.close();
                                    } catch (IOException e2) {
                                        Slog.e(TAG, "Error closing file " + e2);
                                    }
                                } catch (Throwable th) {
                                    th = th;
                                    throw th;
                                }
                            }
                        } catch (IOException e3) {
                            e = e3;
                            raf2 = raf;
                            Slog.e(TAG, "Error writing to file " + e);
                            if (raf2 != null) {
                                try {
                                    raf2.close();
                                } catch (IOException e4) {
                                    Slog.e(TAG, "Error closing file " + e4);
                                }
                            }
                        } catch (Throwable th2) {
                            th = th2;
                            raf2 = raf;
                            if (raf2 != null) {
                                try {
                                    raf2.close();
                                } catch (IOException e5) {
                                    Slog.e(TAG, "Error closing file " + e5);
                                }
                            }
                            throw th;
                        }
                        this.mCache.putFile(name, hash);
                    }
                    raf.setLength(0L);
                    raf.close();
                    if (raf == null) {
                    }
                    this.mCache.putFile(name, hash);
                } catch (Throwable th3) {
                    th = th3;
                }
            } catch (Throwable th4) {
                th = th4;
                throw th;
            }
        }
    }

    public void writePatternHash(byte[] hash, int userId) throws Throwable {
        writeFile(getLockPatternFilename(userId), hash);
    }

    public void writePasswordHash(byte[] hash, int userId) throws Throwable {
        writeFile(getLockPasswordFilename(userId), hash);
    }

    String getLockPatternFilename(int userId) {
        return getLockCredentialFilePathForUser(userId, LOCK_PATTERN_FILE);
    }

    String getLockPasswordFilename(int userId) {
        return getLockCredentialFilePathForUser(userId, LOCK_PASSWORD_FILE);
    }

    private String getLockCredentialFilePathForUser(int userId, String basename) {
        int userId2 = getUserParentOrSelfId(userId);
        String dataSystemDirectory = Environment.getDataDirectory().getAbsolutePath() + SYSTEM_DIRECTORY;
        return userId2 == 0 ? dataSystemDirectory + basename : new File(Environment.getUserSystemDirectory(userId2), basename).getAbsolutePath();
    }

    private int getUserParentOrSelfId(int userId) {
        if (userId != 0) {
            UserManager um = (UserManager) this.mContext.getSystemService(COLUMN_USERID);
            UserInfo pi = um.getProfileParent(userId);
            if (pi != null) {
                return pi.id;
            }
            return userId;
        }
        return userId;
    }

    public void removeUser(int userId) {
        SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
        UserManager um = (UserManager) this.mContext.getSystemService(COLUMN_USERID);
        UserInfo parentInfo = um.getProfileParent(userId);
        synchronized (this.mFileWriteLock) {
            if (parentInfo == null) {
                String name = getLockPasswordFilename(userId);
                File file = new File(name);
                if (file.exists()) {
                    file.delete();
                    this.mCache.putFile(name, null);
                }
                String name2 = getLockPatternFilename(userId);
                File file2 = new File(name2);
                if (file2.exists()) {
                    file2.delete();
                    this.mCache.putFile(name2, null);
                }
            }
        }
        try {
            db.beginTransaction();
            db.delete(TABLE, "user='" + userId + "'", null);
            db.setTransactionSuccessful();
            this.mCache.removeUser(userId);
        } finally {
            db.endTransaction();
        }
    }

    void closeDatabase() {
        this.mOpenHelper.close();
    }

    void clearCache() {
        this.mCache.clear();
    }

    class DatabaseHelper extends SQLiteOpenHelper {
        private static final String DATABASE_NAME = "locksettings.db";
        private static final int DATABASE_VERSION = 2;
        private static final String TAG = "LockSettingsDB";
        private final Callback mCallback;

        public DatabaseHelper(Context context, Callback callback) {
            super(context, DATABASE_NAME, (SQLiteDatabase.CursorFactory) null, 2);
            setWriteAheadLoggingEnabled(true);
            this.mCallback = callback;
        }

        private void createTable(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE locksettings (_id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT,user INTEGER,value TEXT);");
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            createTable(db);
            this.mCallback.initialize(db);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int currentVersion) {
            int upgradeVersion = oldVersion;
            if (upgradeVersion == 1) {
                upgradeVersion = 2;
            }
            if (upgradeVersion != 2) {
                Log.w(TAG, "Failed to upgrade database!");
            }
        }
    }

    private static class Cache {
        private final ArrayMap<CacheKey, Object> mCache;
        private final CacheKey mCacheKey;
        private int mVersion;

        private Cache() {
            this.mCache = new ArrayMap<>();
            this.mCacheKey = new CacheKey();
            this.mVersion = 0;
        }

        String peekKeyValue(String key, String defaultValue, int userId) {
            Object cached = peek(0, key, userId);
            return cached == LockSettingsStorage.DEFAULT ? defaultValue : (String) cached;
        }

        boolean hasKeyValue(String key, int userId) {
            return contains(0, key, userId);
        }

        void putKeyValue(String key, String value, int userId) {
            put(0, key, value, userId);
        }

        void putKeyValueIfUnchanged(String key, Object value, int userId, int version) {
            putIfUnchanged(0, key, value, userId, version);
        }

        byte[] peekFile(String fileName) {
            return (byte[]) peek(1, fileName, -1);
        }

        boolean hasFile(String fileName) {
            return contains(1, fileName, -1);
        }

        void putFile(String key, byte[] value) {
            put(1, key, value, -1);
        }

        void putFileIfUnchanged(String key, byte[] value, int version) {
            putIfUnchanged(1, key, value, -1, version);
        }

        void setFetched(int userId) {
            put(2, "isFetched", "true", userId);
        }

        boolean isFetched(int userId) {
            return contains(2, "", userId);
        }

        private synchronized void put(int type, String key, Object value, int userId) {
            this.mCache.put(new CacheKey().set(type, key, userId), value);
            this.mVersion++;
        }

        private synchronized void putIfUnchanged(int type, String key, Object value, int userId, int version) {
            if (!contains(type, key, userId) && this.mVersion == version) {
                put(type, key, value, userId);
            }
        }

        private synchronized boolean contains(int type, String key, int userId) {
            return this.mCache.containsKey(this.mCacheKey.set(type, key, userId));
        }

        private synchronized Object peek(int type, String key, int userId) {
            return this.mCache.get(this.mCacheKey.set(type, key, userId));
        }

        private synchronized int getVersion() {
            return this.mVersion;
        }

        synchronized void removeUser(int userId) {
            for (int i = this.mCache.size() - 1; i >= 0; i--) {
                if (this.mCache.keyAt(i).userId == userId) {
                    this.mCache.removeAt(i);
                }
            }
            this.mVersion++;
        }

        synchronized void clear() {
            this.mCache.clear();
            this.mVersion++;
        }

        private static final class CacheKey {
            static final int TYPE_FETCHED = 2;
            static final int TYPE_FILE = 1;
            static final int TYPE_KEY_VALUE = 0;
            String key;
            int type;
            int userId;

            private CacheKey() {
            }

            public CacheKey set(int type, String key, int userId) {
                this.type = type;
                this.key = key;
                this.userId = userId;
                return this;
            }

            public boolean equals(Object obj) {
                if (!(obj instanceof CacheKey)) {
                    return false;
                }
                CacheKey o = (CacheKey) obj;
                return this.userId == o.userId && this.type == o.type && this.key.equals(o.key);
            }

            public int hashCode() {
                return (this.key.hashCode() ^ this.userId) ^ this.type;
            }
        }
    }
}
