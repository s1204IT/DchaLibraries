package com.android.server.locksettings;

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
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.Preconditions;
import com.android.server.LocalServices;
import com.android.server.PersistentDataBlockManagerInternal;
import com.android.server.backup.BackupManagerConstants;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class LockSettingsStorage {
    private static final String BASE_ZERO_LOCK_PATTERN_FILE = "gatekeeper.gesture.key";
    private static final String CHILD_PROFILE_LOCK_FILE = "gatekeeper.profile.key";
    private static final String COLUMN_KEY = "name";
    private static final String COLUMN_USERID = "user";
    private static final boolean DEBUG = false;
    private static final String LEGACY_LOCK_PASSWORD_FILE = "password.key";
    private static final String LEGACY_LOCK_PATTERN_FILE = "gesture.key";
    private static final String LOCK_PASSWORD_FILE = "gatekeeper.password.key";
    private static final String LOCK_PATTERN_FILE = "gatekeeper.pattern.key";
    private static final String SYNTHETIC_PASSWORD_DIRECTORY = "spblob/";
    private static final String SYSTEM_DIRECTORY = "/system/";
    private static final String TABLE = "locksettings";
    private static final String TAG = "LockSettingsStorage";
    private final Context mContext;
    private final DatabaseHelper mOpenHelper;
    private PersistentDataBlockManagerInternal mPersistentDataBlockManagerInternal;
    private static final String COLUMN_VALUE = "value";
    private static final String[] COLUMNS_FOR_QUERY = {COLUMN_VALUE};
    private static final String[] COLUMNS_FOR_PREFETCH = {"name", COLUMN_VALUE};
    private static final Object DEFAULT = new Object();
    private final Cache mCache = new Cache();
    private final Object mFileWriteLock = new Object();

    public interface Callback {
        void initialize(SQLiteDatabase sQLiteDatabase);
    }

    @VisibleForTesting
    public static class CredentialHash {
        static final int VERSION_GATEKEEPER = 1;
        static final int VERSION_LEGACY = 0;
        byte[] hash;
        boolean isBaseZeroPattern;
        int type;
        int version;

        private CredentialHash(byte[] bArr, int i, int i2) {
            this(bArr, i, i2, false);
        }

        private CredentialHash(byte[] bArr, int i, int i2, boolean z) {
            if (i != -1) {
                if (bArr == null) {
                    throw new RuntimeException("Empty hash for CredentialHash");
                }
            } else if (bArr != null) {
                throw new RuntimeException("None type CredentialHash should not have hash");
            }
            this.hash = bArr;
            this.type = i;
            this.version = i2;
            this.isBaseZeroPattern = z;
        }

        private static CredentialHash createBaseZeroPattern(byte[] bArr) {
            return new CredentialHash(bArr, 1, 1, true);
        }

        static CredentialHash create(byte[] bArr, int i) {
            if (i == -1) {
                throw new RuntimeException("Bad type for CredentialHash");
            }
            return new CredentialHash(bArr, i, 1);
        }

        static CredentialHash createEmptyHash() {
            return new CredentialHash(null, -1, 1);
        }

        public byte[] toBytes() {
            Preconditions.checkState(!this.isBaseZeroPattern, "base zero patterns are not serializable");
            try {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
                dataOutputStream.write(this.version);
                dataOutputStream.write(this.type);
                if (this.hash != null && this.hash.length > 0) {
                    dataOutputStream.writeInt(this.hash.length);
                    dataOutputStream.write(this.hash);
                } else {
                    dataOutputStream.writeInt(0);
                }
                dataOutputStream.close();
                return byteArrayOutputStream.toByteArray();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public static CredentialHash fromBytes(byte[] bArr) {
            try {
                DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(bArr));
                int i = dataInputStream.read();
                int i2 = dataInputStream.read();
                int i3 = dataInputStream.readInt();
                byte[] bArr2 = null;
                if (i3 > 0) {
                    bArr2 = new byte[i3];
                    dataInputStream.readFully(bArr2);
                }
                return new CredentialHash(bArr2, i2, i);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public LockSettingsStorage(Context context) {
        this.mContext = context;
        this.mOpenHelper = new DatabaseHelper(context);
    }

    public void setDatabaseOnCreateCallback(Callback callback) {
        this.mOpenHelper.setCallback(callback);
    }

    public void writeKeyValue(String str, String str2, int i) {
        writeKeyValue(this.mOpenHelper.getWritableDatabase(), str, str2, i);
    }

    public void writeKeyValue(SQLiteDatabase sQLiteDatabase, String str, String str2, int i) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("name", str);
        contentValues.put(COLUMN_USERID, Integer.valueOf(i));
        contentValues.put(COLUMN_VALUE, str2);
        sQLiteDatabase.beginTransaction();
        try {
            sQLiteDatabase.delete(TABLE, "name=? AND user=?", new String[]{str, Integer.toString(i)});
            sQLiteDatabase.insert(TABLE, null, contentValues);
            sQLiteDatabase.setTransactionSuccessful();
            this.mCache.putKeyValue(str, str2, i);
        } finally {
            sQLiteDatabase.endTransaction();
        }
    }

    public String readKeyValue(String str, String str2, int i) {
        synchronized (this.mCache) {
            if (this.mCache.hasKeyValue(str, i)) {
                return this.mCache.peekKeyValue(str, str2, i);
            }
            int version = this.mCache.getVersion();
            Object string = DEFAULT;
            Cursor cursorQuery = this.mOpenHelper.getReadableDatabase().query(TABLE, COLUMNS_FOR_QUERY, "user=? AND name=?", new String[]{Integer.toString(i), str}, null, null, null);
            if (cursorQuery != null) {
                if (cursorQuery.moveToFirst()) {
                    string = cursorQuery.getString(0);
                }
                cursorQuery.close();
            }
            this.mCache.putKeyValueIfUnchanged(str, string, i, version);
            return string == DEFAULT ? str2 : (String) string;
        }
    }

    public void prefetchUser(int i) throws Throwable {
        synchronized (this.mCache) {
            if (this.mCache.isFetched(i)) {
                return;
            }
            this.mCache.setFetched(i);
            int version = this.mCache.getVersion();
            Cursor cursorQuery = this.mOpenHelper.getReadableDatabase().query(TABLE, COLUMNS_FOR_PREFETCH, "user=?", new String[]{Integer.toString(i)}, null, null, null);
            if (cursorQuery != null) {
                while (cursorQuery.moveToNext()) {
                    this.mCache.putKeyValueIfUnchanged(cursorQuery.getString(0), cursorQuery.getString(1), i, version);
                }
                cursorQuery.close();
            }
            readCredentialHash(i);
        }
    }

    private CredentialHash readPasswordHashIfExists(int i) throws Throwable {
        byte[] file = readFile(getLockPasswordFilename(i));
        int i2 = 2;
        if (!ArrayUtils.isEmpty(file)) {
            return new CredentialHash(file, i2, 1);
        }
        byte[] file2 = readFile(getLegacyLockPasswordFilename(i));
        if (ArrayUtils.isEmpty(file2)) {
            return null;
        }
        return new CredentialHash(file2, i2, 0);
    }

    private CredentialHash readPatternHashIfExists(int i) throws Throwable {
        byte[] file = readFile(getLockPatternFilename(i));
        int i2 = 1;
        if (!ArrayUtils.isEmpty(file)) {
            return new CredentialHash(file, i2, i2);
        }
        byte[] file2 = readFile(getBaseZeroLockPatternFilename(i));
        if (!ArrayUtils.isEmpty(file2)) {
            return CredentialHash.createBaseZeroPattern(file2);
        }
        byte[] file3 = readFile(getLegacyLockPatternFilename(i));
        if (ArrayUtils.isEmpty(file3)) {
            return null;
        }
        return new CredentialHash(file3, i2, 0);
    }

    public CredentialHash readCredentialHash(int i) throws Throwable {
        CredentialHash passwordHashIfExists = readPasswordHashIfExists(i);
        CredentialHash patternHashIfExists = readPatternHashIfExists(i);
        if (passwordHashIfExists != null && patternHashIfExists != null) {
            if (passwordHashIfExists.version == 1) {
                return passwordHashIfExists;
            }
            return patternHashIfExists;
        }
        if (passwordHashIfExists != null) {
            return passwordHashIfExists;
        }
        if (patternHashIfExists != null) {
            return patternHashIfExists;
        }
        return CredentialHash.createEmptyHash();
    }

    public void removeChildProfileLock(int i) {
        try {
            deleteFile(getChildProfileLockFile(i));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void writeChildProfileLock(int i, byte[] bArr) {
        writeFile(getChildProfileLockFile(i), bArr);
    }

    public byte[] readChildProfileLock(int i) {
        return readFile(getChildProfileLockFile(i));
    }

    public boolean hasChildProfileLock(int i) {
        return hasFile(getChildProfileLockFile(i));
    }

    public boolean hasPassword(int i) {
        return hasFile(getLockPasswordFilename(i)) || hasFile(getLegacyLockPasswordFilename(i));
    }

    public boolean hasPattern(int i) {
        return hasFile(getLockPatternFilename(i)) || hasFile(getBaseZeroLockPatternFilename(i)) || hasFile(getLegacyLockPatternFilename(i));
    }

    public boolean hasCredential(int i) {
        return hasPassword(i) || hasPattern(i);
    }

    private boolean hasFile(String str) throws Throwable {
        byte[] file = readFile(str);
        return file != null && file.length > 0;
    }

    private byte[] readFile(String str) throws Throwable {
        RandomAccessFile randomAccessFile;
        byte[] bArr;
        IOException e;
        String str2;
        StringBuilder sb;
        synchronized (this.mCache) {
            if (this.mCache.hasFile(str)) {
                return this.mCache.peekFile(str);
            }
            int version = this.mCache.getVersion();
            try {
                randomAccessFile = new RandomAccessFile(str, "r");
                try {
                    try {
                        bArr = new byte[(int) randomAccessFile.length()];
                    } catch (IOException e2) {
                        bArr = null;
                        e = e2;
                    }
                    try {
                        randomAccessFile.readFully(bArr, 0, bArr.length);
                        randomAccessFile.close();
                        try {
                            randomAccessFile.close();
                        } catch (IOException e3) {
                            e = e3;
                            str2 = TAG;
                            sb = new StringBuilder();
                            sb.append("Error closing file ");
                            sb.append(e);
                            Slog.e(str2, sb.toString());
                        }
                    } catch (IOException e4) {
                        e = e4;
                        Slog.e(TAG, "Cannot read file " + e);
                        if (randomAccessFile != null) {
                            try {
                                randomAccessFile.close();
                            } catch (IOException e5) {
                                e = e5;
                                str2 = TAG;
                                sb = new StringBuilder();
                                sb.append("Error closing file ");
                                sb.append(e);
                                Slog.e(str2, sb.toString());
                            }
                        }
                    }
                } catch (Throwable th) {
                    th = th;
                    if (randomAccessFile != null) {
                        try {
                            randomAccessFile.close();
                        } catch (IOException e6) {
                            Slog.e(TAG, "Error closing file " + e6);
                        }
                    }
                    throw th;
                }
            } catch (IOException e7) {
                bArr = null;
                e = e7;
                randomAccessFile = null;
            } catch (Throwable th2) {
                th = th2;
                randomAccessFile = null;
                if (randomAccessFile != null) {
                }
                throw th;
            }
            this.mCache.putFileIfUnchanged(str, bArr, version);
            return bArr;
        }
    }

    private void writeFile(String str, byte[] bArr) {
        RandomAccessFile randomAccessFile;
        IOException e;
        String str2;
        String str3;
        synchronized (this.mFileWriteLock) {
            try {
                randomAccessFile = new RandomAccessFile(str, "rws");
                if (bArr != null) {
                    try {
                        try {
                            if (bArr.length == 0) {
                                randomAccessFile.setLength(0L);
                                randomAccessFile.close();
                                try {
                                    randomAccessFile.close();
                                } catch (IOException e2) {
                                    str2 = TAG;
                                    str3 = "Error closing file " + e2;
                                    Slog.e(str2, str3);
                                }
                            } else {
                                randomAccessFile.write(bArr, 0, bArr.length);
                                randomAccessFile.close();
                                randomAccessFile.close();
                            }
                        } catch (IOException e3) {
                            e = e3;
                            Slog.e(TAG, "Error writing to file " + e);
                            if (randomAccessFile != null) {
                                try {
                                    randomAccessFile.close();
                                } catch (IOException e4) {
                                    str2 = TAG;
                                    str3 = "Error closing file " + e4;
                                    Slog.e(str2, str3);
                                }
                            }
                        }
                    } catch (Throwable th) {
                        th = th;
                        if (randomAccessFile != null) {
                            try {
                                randomAccessFile.close();
                            } catch (IOException e5) {
                                Slog.e(TAG, "Error closing file " + e5);
                            }
                        }
                        throw th;
                    }
                } else {
                    randomAccessFile.setLength(0L);
                    randomAccessFile.close();
                    randomAccessFile.close();
                }
            } catch (IOException e6) {
                randomAccessFile = null;
                e = e6;
            } catch (Throwable th2) {
                th = th2;
                randomAccessFile = null;
                if (randomAccessFile != null) {
                }
                throw th;
            }
            this.mCache.putFile(str, bArr);
        }
    }

    private void deleteFile(String str) {
        synchronized (this.mFileWriteLock) {
            File file = new File(str);
            if (file.exists()) {
                file.delete();
                this.mCache.putFile(str, null);
            }
        }
    }

    public void writeCredentialHash(CredentialHash credentialHash, int i) {
        byte[] bArr;
        byte[] bArr2 = null;
        if (credentialHash.type == 2) {
            bArr2 = credentialHash.hash;
            bArr = null;
        } else {
            bArr = credentialHash.type == 1 ? credentialHash.hash : null;
        }
        writeFile(getLockPasswordFilename(i), bArr2);
        writeFile(getLockPatternFilename(i), bArr);
    }

    @VisibleForTesting
    String getLockPatternFilename(int i) {
        return getLockCredentialFilePathForUser(i, LOCK_PATTERN_FILE);
    }

    @VisibleForTesting
    String getLockPasswordFilename(int i) {
        return getLockCredentialFilePathForUser(i, LOCK_PASSWORD_FILE);
    }

    @VisibleForTesting
    String getLegacyLockPatternFilename(int i) {
        return getLockCredentialFilePathForUser(i, LEGACY_LOCK_PATTERN_FILE);
    }

    @VisibleForTesting
    String getLegacyLockPasswordFilename(int i) {
        return getLockCredentialFilePathForUser(i, LEGACY_LOCK_PASSWORD_FILE);
    }

    private String getBaseZeroLockPatternFilename(int i) {
        return getLockCredentialFilePathForUser(i, BASE_ZERO_LOCK_PATTERN_FILE);
    }

    @VisibleForTesting
    String getChildProfileLockFile(int i) {
        return getLockCredentialFilePathForUser(i, CHILD_PROFILE_LOCK_FILE);
    }

    private String getLockCredentialFilePathForUser(int i, String str) {
        String str2 = Environment.getDataDirectory().getAbsolutePath() + SYSTEM_DIRECTORY;
        if (i == 0) {
            return str2 + str;
        }
        return new File(Environment.getUserSystemDirectory(i), str).getAbsolutePath();
    }

    public void writeSyntheticPasswordState(int i, long j, String str, byte[] bArr) {
        ensureSyntheticPasswordDirectoryForUser(i);
        writeFile(getSynthenticPasswordStateFilePathForUser(i, j, str), bArr);
    }

    public byte[] readSyntheticPasswordState(int i, long j, String str) {
        return readFile(getSynthenticPasswordStateFilePathForUser(i, j, str));
    }

    public void deleteSyntheticPasswordState(int i, long j, String str) {
        RandomAccessFile randomAccessFile;
        Throwable th;
        Throwable th2;
        String synthenticPasswordStateFilePathForUser = getSynthenticPasswordStateFilePathForUser(i, j, str);
        File file = new File(synthenticPasswordStateFilePathForUser);
        if (file.exists()) {
            try {
                try {
                    randomAccessFile = new RandomAccessFile(synthenticPasswordStateFilePathForUser, "rws");
                } catch (Exception e) {
                    Slog.w(TAG, "Failed to zeroize " + synthenticPasswordStateFilePathForUser, e);
                }
                try {
                    randomAccessFile.write(new byte[(int) randomAccessFile.length()]);
                    randomAccessFile.close();
                    file.delete();
                    this.mCache.putFile(synthenticPasswordStateFilePathForUser, null);
                } catch (Throwable th3) {
                    try {
                        throw th3;
                    } catch (Throwable th4) {
                        th = th3;
                        th2 = th4;
                        if (th != null) {
                            randomAccessFile.close();
                            throw th2;
                        }
                        try {
                            randomAccessFile.close();
                            throw th2;
                        } catch (Throwable th5) {
                            th.addSuppressed(th5);
                            throw th2;
                        }
                    }
                }
            } catch (Throwable th6) {
                file.delete();
                throw th6;
            }
        }
    }

    public Map<Integer, List<Long>> listSyntheticPasswordHandlesForAllUsers(String str) {
        ArrayMap arrayMap = new ArrayMap();
        for (UserInfo userInfo : UserManager.get(this.mContext).getUsers(false)) {
            arrayMap.put(Integer.valueOf(userInfo.id), listSyntheticPasswordHandlesForUser(str, userInfo.id));
        }
        return arrayMap;
    }

    public List<Long> listSyntheticPasswordHandlesForUser(String str, int i) {
        File syntheticPasswordDirectoryForUser = getSyntheticPasswordDirectoryForUser(i);
        ArrayList arrayList = new ArrayList();
        File[] fileArrListFiles = syntheticPasswordDirectoryForUser.listFiles();
        if (fileArrListFiles == null) {
            return arrayList;
        }
        for (File file : fileArrListFiles) {
            String[] strArrSplit = file.getName().split("\\.");
            if (strArrSplit.length == 2 && strArrSplit[1].equals(str)) {
                try {
                    arrayList.add(Long.valueOf(Long.parseUnsignedLong(strArrSplit[0], 16)));
                } catch (NumberFormatException e) {
                    Slog.e(TAG, "Failed to parse handle " + strArrSplit[0]);
                }
            }
        }
        return arrayList;
    }

    @VisibleForTesting
    protected File getSyntheticPasswordDirectoryForUser(int i) {
        return new File(Environment.getDataSystemDeDirectory(i), SYNTHETIC_PASSWORD_DIRECTORY);
    }

    private void ensureSyntheticPasswordDirectoryForUser(int i) {
        File syntheticPasswordDirectoryForUser = getSyntheticPasswordDirectoryForUser(i);
        if (!syntheticPasswordDirectoryForUser.exists()) {
            syntheticPasswordDirectoryForUser.mkdir();
        }
    }

    @VisibleForTesting
    protected String getSynthenticPasswordStateFilePathForUser(int i, long j, String str) {
        return new File(getSyntheticPasswordDirectoryForUser(i), String.format("%016x.%s", Long.valueOf(j), str)).getAbsolutePath();
    }

    public void removeUser(int i) {
        SQLiteDatabase writableDatabase = this.mOpenHelper.getWritableDatabase();
        if (((UserManager) this.mContext.getSystemService(COLUMN_USERID)).getProfileParent(i) == null) {
            synchronized (this.mFileWriteLock) {
                String lockPasswordFilename = getLockPasswordFilename(i);
                File file = new File(lockPasswordFilename);
                if (file.exists()) {
                    file.delete();
                    this.mCache.putFile(lockPasswordFilename, null);
                }
                String lockPatternFilename = getLockPatternFilename(i);
                File file2 = new File(lockPatternFilename);
                if (file2.exists()) {
                    file2.delete();
                    this.mCache.putFile(lockPatternFilename, null);
                }
            }
        } else {
            removeChildProfileLock(i);
        }
        File syntheticPasswordDirectoryForUser = getSyntheticPasswordDirectoryForUser(i);
        try {
            writableDatabase.beginTransaction();
            writableDatabase.delete(TABLE, "user='" + i + "'", null);
            writableDatabase.setTransactionSuccessful();
            this.mCache.removeUser(i);
            this.mCache.purgePath(syntheticPasswordDirectoryForUser.getAbsolutePath());
        } finally {
            writableDatabase.endTransaction();
        }
    }

    @VisibleForTesting
    void closeDatabase() {
        this.mOpenHelper.close();
    }

    @VisibleForTesting
    void clearCache() {
        this.mCache.clear();
    }

    public PersistentDataBlockManagerInternal getPersistentDataBlock() {
        if (this.mPersistentDataBlockManagerInternal == null) {
            this.mPersistentDataBlockManagerInternal = (PersistentDataBlockManagerInternal) LocalServices.getService(PersistentDataBlockManagerInternal.class);
        }
        return this.mPersistentDataBlockManagerInternal;
    }

    public void writePersistentDataBlock(int i, int i2, int i3, byte[] bArr) {
        PersistentDataBlockManagerInternal persistentDataBlock = getPersistentDataBlock();
        if (persistentDataBlock == null) {
            return;
        }
        persistentDataBlock.setFrpCredentialHandle(PersistentData.toBytes(i, i2, i3, bArr));
    }

    public PersistentData readPersistentDataBlock() {
        PersistentDataBlockManagerInternal persistentDataBlock = getPersistentDataBlock();
        if (persistentDataBlock == null) {
            return PersistentData.NONE;
        }
        try {
            return PersistentData.fromBytes(persistentDataBlock.getFrpCredentialHandle());
        } catch (IllegalStateException e) {
            Slog.e(TAG, "Error reading persistent data block", e);
            return PersistentData.NONE;
        }
    }

    public static class PersistentData {
        public static final PersistentData NONE = new PersistentData(0, -10000, 0, null);
        public static final int TYPE_NONE = 0;
        public static final int TYPE_SP = 1;
        public static final int TYPE_SP_WEAVER = 2;
        static final byte VERSION_1 = 1;
        static final int VERSION_1_HEADER_SIZE = 10;
        final byte[] payload;
        final int qualityForUi;
        final int type;
        final int userId;

        private PersistentData(int i, int i2, int i3, byte[] bArr) {
            this.type = i;
            this.userId = i2;
            this.qualityForUi = i3;
            this.payload = bArr;
        }

        public static PersistentData fromBytes(byte[] bArr) {
            if (bArr == null || bArr.length == 0) {
                return NONE;
            }
            DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(bArr));
            try {
                byte b = dataInputStream.readByte();
                if (b == 1) {
                    int i = dataInputStream.readByte() & 255;
                    int i2 = dataInputStream.readInt();
                    int i3 = dataInputStream.readInt();
                    byte[] bArr2 = new byte[bArr.length - 10];
                    System.arraycopy(bArr, 10, bArr2, 0, bArr2.length);
                    return new PersistentData(i, i2, i3, bArr2);
                }
                Slog.wtf(LockSettingsStorage.TAG, "Unknown PersistentData version code: " + ((int) b));
                return NONE;
            } catch (IOException e) {
                Slog.wtf(LockSettingsStorage.TAG, "Could not parse PersistentData", e);
                return NONE;
            }
        }

        public static byte[] toBytes(int i, int i2, int i3, byte[] bArr) {
            if (i == 0) {
                Preconditions.checkArgument(bArr == null, "TYPE_NONE must have empty payload");
                return null;
            }
            if (bArr != null && bArr.length > 0) {
                z = true;
            }
            Preconditions.checkArgument(z, "empty payload must only be used with TYPE_NONE");
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(10 + bArr.length);
            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
            try {
                dataOutputStream.writeByte(1);
                dataOutputStream.writeByte(i);
                dataOutputStream.writeInt(i2);
                dataOutputStream.writeInt(i3);
                dataOutputStream.write(bArr);
                return byteArrayOutputStream.toByteArray();
            } catch (IOException e) {
                throw new RuntimeException("ByteArrayOutputStream cannot throw IOException");
            }
        }
    }

    static class DatabaseHelper extends SQLiteOpenHelper {
        private static final String DATABASE_NAME = "locksettings.db";
        private static final int DATABASE_VERSION = 2;
        private static final int IDLE_CONNECTION_TIMEOUT_MS = 30000;
        private static final String TAG = "LockSettingsDB";
        private Callback mCallback;

        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, (SQLiteDatabase.CursorFactory) null, 2);
            setWriteAheadLoggingEnabled(true);
            setIdleConnectionTimeout(30000L);
        }

        public void setCallback(Callback callback) {
            this.mCallback = callback;
        }

        private void createTable(SQLiteDatabase sQLiteDatabase) {
            sQLiteDatabase.execSQL("CREATE TABLE locksettings (_id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT,user INTEGER,value TEXT);");
        }

        @Override
        public void onCreate(SQLiteDatabase sQLiteDatabase) {
            createTable(sQLiteDatabase);
            if (this.mCallback != null) {
                this.mCallback.initialize(sQLiteDatabase);
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
            if (i == 1) {
                i = 2;
            }
            if (i != 2) {
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

        String peekKeyValue(String str, String str2, int i) {
            Object objPeek = peek(0, str, i);
            return objPeek == LockSettingsStorage.DEFAULT ? str2 : (String) objPeek;
        }

        boolean hasKeyValue(String str, int i) {
            return contains(0, str, i);
        }

        void putKeyValue(String str, String str2, int i) {
            put(0, str, str2, i);
        }

        void putKeyValueIfUnchanged(String str, Object obj, int i, int i2) {
            putIfUnchanged(0, str, obj, i, i2);
        }

        byte[] peekFile(String str) {
            return (byte[]) peek(1, str, -1);
        }

        boolean hasFile(String str) {
            return contains(1, str, -1);
        }

        void putFile(String str, byte[] bArr) {
            put(1, str, bArr, -1);
        }

        void putFileIfUnchanged(String str, byte[] bArr, int i) {
            putIfUnchanged(1, str, bArr, -1, i);
        }

        void setFetched(int i) {
            put(2, "isFetched", "true", i);
        }

        boolean isFetched(int i) {
            return contains(2, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, i);
        }

        private synchronized void put(int i, String str, Object obj, int i2) {
            this.mCache.put(new CacheKey().set(i, str, i2), obj);
            this.mVersion++;
        }

        private synchronized void putIfUnchanged(int i, String str, Object obj, int i2, int i3) {
            if (!contains(i, str, i2) && this.mVersion == i3) {
                put(i, str, obj, i2);
            }
        }

        private synchronized boolean contains(int i, String str, int i2) {
            return this.mCache.containsKey(this.mCacheKey.set(i, str, i2));
        }

        private synchronized Object peek(int i, String str, int i2) {
            return this.mCache.get(this.mCacheKey.set(i, str, i2));
        }

        private synchronized int getVersion() {
            return this.mVersion;
        }

        synchronized void removeUser(int i) {
            for (int size = this.mCache.size() - 1; size >= 0; size--) {
                if (this.mCache.keyAt(size).userId == i) {
                    this.mCache.removeAt(size);
                }
            }
            this.mVersion++;
        }

        synchronized void purgePath(String str) {
            for (int size = this.mCache.size() - 1; size >= 0; size--) {
                CacheKey cacheKeyKeyAt = this.mCache.keyAt(size);
                if (cacheKeyKeyAt.type == 1 && cacheKeyKeyAt.key.startsWith(str)) {
                    this.mCache.removeAt(size);
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

            public CacheKey set(int i, String str, int i2) {
                this.type = i;
                this.key = str;
                this.userId = i2;
                return this;
            }

            public boolean equals(Object obj) {
                if (!(obj instanceof CacheKey)) {
                    return false;
                }
                CacheKey cacheKey = (CacheKey) obj;
                return this.userId == cacheKey.userId && this.type == cacheKey.type && this.key.equals(cacheKey.key);
            }

            public int hashCode() {
                return (this.key.hashCode() ^ this.userId) ^ this.type;
            }
        }
    }
}
