package com.android.server.locksettings.recoverablekeystore.storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;
import com.android.server.locksettings.recoverablekeystore.TestOnlyInsecureCertificateHelper;
import com.android.server.locksettings.recoverablekeystore.WrappedKey;
import com.android.server.net.watchlist.WatchlistLoggingHandler;
import java.io.ByteArrayInputStream;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertPath;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.IntConsumer;

public class RecoverableKeyStoreDb {
    private static final String CERT_PATH_ENCODING = "PkiPath";
    private static final int IDLE_TIMEOUT_SECONDS = 30;
    private static final int LAST_SYNCED_AT_UNSYNCED = -1;
    private static final String TAG = "RecoverableKeyStoreDb";
    private final RecoverableKeyStoreDbHelper mKeyStoreDbHelper;
    private final TestOnlyInsecureCertificateHelper mTestOnlyInsecureCertificateHelper = new TestOnlyInsecureCertificateHelper();

    public static RecoverableKeyStoreDb newInstance(Context context) {
        RecoverableKeyStoreDbHelper recoverableKeyStoreDbHelper = new RecoverableKeyStoreDbHelper(context);
        recoverableKeyStoreDbHelper.setWriteAheadLoggingEnabled(true);
        recoverableKeyStoreDbHelper.setIdleConnectionTimeout(30L);
        return new RecoverableKeyStoreDb(recoverableKeyStoreDbHelper);
    }

    private RecoverableKeyStoreDb(RecoverableKeyStoreDbHelper recoverableKeyStoreDbHelper) {
        this.mKeyStoreDbHelper = recoverableKeyStoreDbHelper;
    }

    public long insertKey(int i, int i2, String str, WrappedKey wrappedKey) {
        SQLiteDatabase writableDatabase = this.mKeyStoreDbHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("user_id", Integer.valueOf(i));
        contentValues.put(WatchlistLoggingHandler.WatchlistEventKeys.UID, Integer.valueOf(i2));
        contentValues.put("alias", str);
        contentValues.put("nonce", wrappedKey.getNonce());
        contentValues.put("wrapped_key", wrappedKey.getKeyMaterial());
        contentValues.put("last_synced_at", (Integer) (-1));
        contentValues.put("platform_key_generation_id", Integer.valueOf(wrappedKey.getPlatformKeyGenerationId()));
        contentValues.put("recovery_status", Integer.valueOf(wrappedKey.getRecoveryStatus()));
        return writableDatabase.replace("keys", null, contentValues);
    }

    public WrappedKey getKey(int i, String str) throws Exception {
        Cursor cursorQuery = this.mKeyStoreDbHelper.getReadableDatabase().query("keys", new String[]{"_id", "nonce", "wrapped_key", "platform_key_generation_id", "recovery_status"}, "uid = ? AND alias = ?", new String[]{Integer.toString(i), str}, null, null, null);
        try {
            int count = cursorQuery.getCount();
            if (count == 0) {
                return null;
            }
            if (count > 1) {
                Log.wtf(TAG, String.format(Locale.US, "%d WrappedKey entries found for uid=%d alias='%s'. Should only ever be 0 or 1.", Integer.valueOf(count), Integer.valueOf(i), str));
                if (cursorQuery != null) {
                    $closeResource(null, cursorQuery);
                }
                return null;
            }
            cursorQuery.moveToFirst();
            WrappedKey wrappedKey = new WrappedKey(cursorQuery.getBlob(cursorQuery.getColumnIndexOrThrow("nonce")), cursorQuery.getBlob(cursorQuery.getColumnIndexOrThrow("wrapped_key")), cursorQuery.getInt(cursorQuery.getColumnIndexOrThrow("platform_key_generation_id")), cursorQuery.getInt(cursorQuery.getColumnIndexOrThrow("recovery_status")));
            if (cursorQuery != null) {
                $closeResource(null, cursorQuery);
            }
            return wrappedKey;
        } finally {
            if (cursorQuery != null) {
                $closeResource(null, cursorQuery);
            }
        }
    }

    private static void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
        if (th == null) {
            autoCloseable.close();
            return;
        }
        try {
            autoCloseable.close();
        } catch (Throwable th2) {
            th.addSuppressed(th2);
        }
    }

    public boolean removeKey(int i, String str) {
        return this.mKeyStoreDbHelper.getWritableDatabase().delete("keys", "uid = ? AND alias = ?", new String[]{Integer.toString(i), str}) > 0;
    }

    public Map<String, Integer> getStatusForAllKeys(int i) throws Exception {
        Cursor cursorQuery = this.mKeyStoreDbHelper.getReadableDatabase().query("keys", new String[]{"_id", "alias", "recovery_status"}, "uid = ?", new String[]{Integer.toString(i)}, null, null, null);
        Throwable th = null;
        try {
            HashMap map = new HashMap();
            while (cursorQuery.moveToNext()) {
                map.put(cursorQuery.getString(cursorQuery.getColumnIndexOrThrow("alias")), Integer.valueOf(cursorQuery.getInt(cursorQuery.getColumnIndexOrThrow("recovery_status"))));
            }
            return map;
        } finally {
            if (cursorQuery != null) {
                $closeResource(th, cursorQuery);
            }
        }
    }

    public int setRecoveryStatus(int i, String str, int i2) {
        SQLiteDatabase writableDatabase = this.mKeyStoreDbHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("recovery_status", Integer.valueOf(i2));
        return writableDatabase.update("keys", contentValues, "uid = ? AND alias = ?", new String[]{String.valueOf(i), str});
    }

    public Map<String, WrappedKey> getAllKeys(int i, int i2, int i3) throws Exception {
        Cursor cursorQuery = this.mKeyStoreDbHelper.getReadableDatabase().query("keys", new String[]{"_id", "nonce", "wrapped_key", "alias", "recovery_status"}, "user_id = ? AND uid = ? AND platform_key_generation_id = ?", new String[]{Integer.toString(i), Integer.toString(i2), Integer.toString(i3)}, null, null, null);
        try {
            HashMap map = new HashMap();
            while (cursorQuery.moveToNext()) {
                map.put(cursorQuery.getString(cursorQuery.getColumnIndexOrThrow("alias")), new WrappedKey(cursorQuery.getBlob(cursorQuery.getColumnIndexOrThrow("nonce")), cursorQuery.getBlob(cursorQuery.getColumnIndexOrThrow("wrapped_key")), i3, cursorQuery.getInt(cursorQuery.getColumnIndexOrThrow("recovery_status"))));
            }
            return map;
        } finally {
            if (cursorQuery != null) {
                $closeResource(null, cursorQuery);
            }
        }
    }

    public long setPlatformKeyGenerationId(int i, int i2) {
        SQLiteDatabase writableDatabase = this.mKeyStoreDbHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("user_id", Integer.valueOf(i));
        contentValues.put("platform_key_generation_id", Integer.valueOf(i2));
        long jReplace = writableDatabase.replace("user_metadata", null, contentValues);
        if (jReplace != -1) {
            invalidateKeysWithOldGenerationId(i, i2);
        }
        return jReplace;
    }

    public void invalidateKeysWithOldGenerationId(int i, int i2) {
        SQLiteDatabase writableDatabase = this.mKeyStoreDbHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("recovery_status", (Integer) 3);
        writableDatabase.update("keys", contentValues, "user_id = ? AND platform_key_generation_id < ?", new String[]{String.valueOf(i), String.valueOf(i2)});
    }

    public void invalidateKeysForUserIdOnCustomScreenLock(int i) {
        SQLiteDatabase writableDatabase = this.mKeyStoreDbHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("recovery_status", (Integer) 3);
        writableDatabase.update("keys", contentValues, "user_id = ?", new String[]{String.valueOf(i)});
    }

    public int getPlatformKeyGenerationId(int i) throws Exception {
        Cursor cursorQuery = this.mKeyStoreDbHelper.getReadableDatabase().query("user_metadata", new String[]{"platform_key_generation_id"}, "user_id = ?", new String[]{Integer.toString(i)}, null, null, null);
        try {
            if (cursorQuery.getCount() == 0) {
                return -1;
            }
            cursorQuery.moveToFirst();
            int i2 = cursorQuery.getInt(cursorQuery.getColumnIndexOrThrow("platform_key_generation_id"));
            if (cursorQuery != null) {
                $closeResource(null, cursorQuery);
            }
            return i2;
        } finally {
            if (cursorQuery != null) {
                $closeResource(null, cursorQuery);
            }
        }
    }

    public long setRecoveryServicePublicKey(int i, int i2, PublicKey publicKey) {
        return setBytes(i, i2, "public_key", publicKey.getEncoded());
    }

    public Long getRecoveryServiceCertSerial(int i, int i2, String str) {
        return getLong(i, i2, str, "cert_serial");
    }

    public long setRecoveryServiceCertSerial(int i, int i2, String str, long j) {
        return setLong(i, i2, str, "cert_serial", j);
    }

    public CertPath getRecoveryServiceCertPath(int i, int i2, String str) throws Exception {
        byte[] bytes = getBytes(i, i2, str, "cert_path");
        if (bytes == null) {
            return null;
        }
        try {
            return decodeCertPath(bytes);
        } catch (CertificateException e) {
            Log.wtf(TAG, String.format(Locale.US, "Recovery service CertPath entry cannot be decoded for userId=%d uid=%d.", Integer.valueOf(i), Integer.valueOf(i2)), e);
            return null;
        }
    }

    public long setRecoveryServiceCertPath(int i, int i2, String str, CertPath certPath) throws CertificateEncodingException {
        if (certPath.getCertificates().size() == 0) {
            throw new CertificateEncodingException("No certificate contained in the cert path.");
        }
        return setBytes(i, i2, str, "cert_path", certPath.getEncoded(CERT_PATH_ENCODING));
    }

    public List<Integer> getRecoveryAgents(int i) throws Exception {
        Cursor cursorQuery = this.mKeyStoreDbHelper.getReadableDatabase().query("recovery_service_metadata", new String[]{WatchlistLoggingHandler.WatchlistEventKeys.UID}, "user_id = ?", new String[]{Integer.toString(i)}, null, null, null);
        Throwable th = null;
        try {
            ArrayList arrayList = new ArrayList(cursorQuery.getCount());
            while (cursorQuery.moveToNext()) {
                arrayList.add(Integer.valueOf(cursorQuery.getInt(cursorQuery.getColumnIndexOrThrow(WatchlistLoggingHandler.WatchlistEventKeys.UID))));
            }
            return arrayList;
        } finally {
            if (cursorQuery != null) {
                $closeResource(th, cursorQuery);
            }
        }
    }

    public PublicKey getRecoveryServicePublicKey(int i, int i2) throws Exception {
        byte[] bytes = getBytes(i, i2, "public_key");
        if (bytes == null) {
            return null;
        }
        try {
            return decodeX509Key(bytes);
        } catch (InvalidKeySpecException e) {
            Log.wtf(TAG, String.format(Locale.US, "Recovery service public key entry cannot be decoded for userId=%d uid=%d.", Integer.valueOf(i), Integer.valueOf(i2)));
            return null;
        }
    }

    public long setRecoverySecretTypes(int i, int i2, int[] iArr) {
        SQLiteDatabase writableDatabase = this.mKeyStoreDbHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        final StringJoiner stringJoiner = new StringJoiner(",");
        Arrays.stream(iArr).forEach(new IntConsumer() {
            @Override
            public final void accept(int i3) {
                stringJoiner.add(Integer.toString(i3));
            }
        });
        contentValues.put("secret_types", stringJoiner.toString());
        ensureRecoveryServiceMetadataEntryExists(i, i2);
        return writableDatabase.update("recovery_service_metadata", contentValues, "user_id = ? AND uid = ?", new String[]{String.valueOf(i), String.valueOf(i2)});
    }

    public int[] getRecoverySecretTypes(int i, int i2) throws Exception {
        Cursor cursorQuery = this.mKeyStoreDbHelper.getReadableDatabase().query("recovery_service_metadata", new String[]{"_id", "user_id", WatchlistLoggingHandler.WatchlistEventKeys.UID, "secret_types"}, "user_id = ? AND uid = ?", new String[]{Integer.toString(i), Integer.toString(i2)}, null, null, null);
        try {
            int count = cursorQuery.getCount();
            if (count == 0) {
                return new int[0];
            }
            if (count > 1) {
                Log.wtf(TAG, String.format(Locale.US, "%d deviceId entries found for userId=%d uid=%d. Should only ever be 0 or 1.", Integer.valueOf(count), Integer.valueOf(i), Integer.valueOf(i2)));
                int[] iArr = new int[0];
                if (cursorQuery != null) {
                    $closeResource(null, cursorQuery);
                }
                return iArr;
            }
            cursorQuery.moveToFirst();
            int columnIndexOrThrow = cursorQuery.getColumnIndexOrThrow("secret_types");
            if (cursorQuery.isNull(columnIndexOrThrow)) {
                int[] iArr2 = new int[0];
                if (cursorQuery != null) {
                    $closeResource(null, cursorQuery);
                }
                return iArr2;
            }
            String string = cursorQuery.getString(columnIndexOrThrow);
            if (TextUtils.isEmpty(string)) {
                int[] iArr3 = new int[0];
                if (cursorQuery != null) {
                    $closeResource(null, cursorQuery);
                }
                return iArr3;
            }
            String[] strArrSplit = string.split(",");
            int[] iArr4 = new int[strArrSplit.length];
            for (int i3 = 0; i3 < strArrSplit.length; i3++) {
                try {
                    iArr4[i3] = Integer.parseInt(strArrSplit[i3]);
                } catch (NumberFormatException e) {
                    Log.wtf(TAG, "String format error " + e);
                }
            }
            if (cursorQuery != null) {
                $closeResource(null, cursorQuery);
            }
            return iArr4;
        } finally {
            if (cursorQuery != null) {
                $closeResource(null, cursorQuery);
            }
        }
    }

    public long setActiveRootOfTrust(int i, int i2, String str) {
        SQLiteDatabase writableDatabase = this.mKeyStoreDbHelper.getWritableDatabase();
        new ContentValues().put("active_root_of_trust", str);
        ensureRecoveryServiceMetadataEntryExists(i, i2);
        return writableDatabase.update("recovery_service_metadata", r1, "user_id = ? AND uid = ?", new String[]{String.valueOf(i), String.valueOf(i2)});
    }

    public String getActiveRootOfTrust(int i, int i2) throws Exception {
        Cursor cursorQuery = this.mKeyStoreDbHelper.getReadableDatabase().query("recovery_service_metadata", new String[]{"_id", "user_id", WatchlistLoggingHandler.WatchlistEventKeys.UID, "active_root_of_trust"}, "user_id = ? AND uid = ?", new String[]{Integer.toString(i), Integer.toString(i2)}, null, null, null);
        try {
            int count = cursorQuery.getCount();
            if (count == 0) {
                return null;
            }
            if (count > 1) {
                Log.wtf(TAG, String.format(Locale.US, "%d deviceId entries found for userId=%d uid=%d. Should only ever be 0 or 1.", Integer.valueOf(count), Integer.valueOf(i), Integer.valueOf(i2)));
                if (cursorQuery != null) {
                    $closeResource(null, cursorQuery);
                }
                return null;
            }
            cursorQuery.moveToFirst();
            int columnIndexOrThrow = cursorQuery.getColumnIndexOrThrow("active_root_of_trust");
            if (cursorQuery.isNull(columnIndexOrThrow)) {
                if (cursorQuery != null) {
                    $closeResource(null, cursorQuery);
                }
                return null;
            }
            String string = cursorQuery.getString(columnIndexOrThrow);
            if (TextUtils.isEmpty(string)) {
                if (cursorQuery != null) {
                    $closeResource(null, cursorQuery);
                }
                return null;
            }
            if (cursorQuery != null) {
                $closeResource(null, cursorQuery);
            }
            return string;
        } finally {
            if (cursorQuery != null) {
                $closeResource(null, cursorQuery);
            }
        }
    }

    public long setCounterId(int i, int i2, long j) {
        return setLong(i, i2, "counter_id", j);
    }

    public Long getCounterId(int i, int i2) {
        return getLong(i, i2, "counter_id");
    }

    public long setServerParams(int i, int i2, byte[] bArr) {
        return setBytes(i, i2, "server_params", bArr);
    }

    public byte[] getServerParams(int i, int i2) {
        return getBytes(i, i2, "server_params");
    }

    public long setSnapshotVersion(int i, int i2, long j) {
        return setLong(i, i2, "snapshot_version", j);
    }

    public Long getSnapshotVersion(int i, int i2) {
        return getLong(i, i2, "snapshot_version");
    }

    public long setShouldCreateSnapshot(int i, int i2, boolean z) {
        return setLong(i, i2, "should_create_snapshot", z ? 1L : 0L);
    }

    public boolean getShouldCreateSnapshot(int i, int i2) throws Exception {
        Long l = getLong(i, i2, "should_create_snapshot");
        return (l == null || l.longValue() == 0) ? false : true;
    }

    private Long getLong(int i, int i2, String str) throws Exception {
        Cursor cursorQuery = this.mKeyStoreDbHelper.getReadableDatabase().query("recovery_service_metadata", new String[]{"_id", "user_id", WatchlistLoggingHandler.WatchlistEventKeys.UID, str}, "user_id = ? AND uid = ?", new String[]{Integer.toString(i), Integer.toString(i2)}, null, null, null);
        try {
            int count = cursorQuery.getCount();
            if (count == 0) {
                return null;
            }
            if (count > 1) {
                Log.wtf(TAG, String.format(Locale.US, "%d entries found for userId=%d uid=%d. Should only ever be 0 or 1.", Integer.valueOf(count), Integer.valueOf(i), Integer.valueOf(i2)));
                if (cursorQuery != null) {
                    $closeResource(null, cursorQuery);
                }
                return null;
            }
            cursorQuery.moveToFirst();
            int columnIndexOrThrow = cursorQuery.getColumnIndexOrThrow(str);
            if (cursorQuery.isNull(columnIndexOrThrow)) {
                if (cursorQuery != null) {
                    $closeResource(null, cursorQuery);
                }
                return null;
            }
            Long lValueOf = Long.valueOf(cursorQuery.getLong(columnIndexOrThrow));
            if (cursorQuery != null) {
                $closeResource(null, cursorQuery);
            }
            return lValueOf;
        } finally {
            if (cursorQuery != null) {
                $closeResource(null, cursorQuery);
            }
        }
    }

    private long setLong(int i, int i2, String str, long j) {
        SQLiteDatabase writableDatabase = this.mKeyStoreDbHelper.getWritableDatabase();
        new ContentValues().put(str, Long.valueOf(j));
        String[] strArr = {Integer.toString(i), Integer.toString(i2)};
        ensureRecoveryServiceMetadataEntryExists(i, i2);
        return writableDatabase.update("recovery_service_metadata", r1, "user_id = ? AND uid = ?", strArr);
    }

    private byte[] getBytes(int i, int i2, String str) throws Exception {
        Cursor cursorQuery = this.mKeyStoreDbHelper.getReadableDatabase().query("recovery_service_metadata", new String[]{"_id", "user_id", WatchlistLoggingHandler.WatchlistEventKeys.UID, str}, "user_id = ? AND uid = ?", new String[]{Integer.toString(i), Integer.toString(i2)}, null, null, null);
        try {
            int count = cursorQuery.getCount();
            if (count == 0) {
                return null;
            }
            if (count > 1) {
                Log.wtf(TAG, String.format(Locale.US, "%d entries found for userId=%d uid=%d. Should only ever be 0 or 1.", Integer.valueOf(count), Integer.valueOf(i), Integer.valueOf(i2)));
                if (cursorQuery != null) {
                    $closeResource(null, cursorQuery);
                }
                return null;
            }
            cursorQuery.moveToFirst();
            int columnIndexOrThrow = cursorQuery.getColumnIndexOrThrow(str);
            if (cursorQuery.isNull(columnIndexOrThrow)) {
                if (cursorQuery != null) {
                    $closeResource(null, cursorQuery);
                }
                return null;
            }
            byte[] blob = cursorQuery.getBlob(columnIndexOrThrow);
            if (cursorQuery != null) {
                $closeResource(null, cursorQuery);
            }
            return blob;
        } finally {
            if (cursorQuery != null) {
                $closeResource(null, cursorQuery);
            }
        }
    }

    private long setBytes(int i, int i2, String str, byte[] bArr) {
        SQLiteDatabase writableDatabase = this.mKeyStoreDbHelper.getWritableDatabase();
        new ContentValues().put(str, bArr);
        String[] strArr = {Integer.toString(i), Integer.toString(i2)};
        ensureRecoveryServiceMetadataEntryExists(i, i2);
        return writableDatabase.update("recovery_service_metadata", r1, "user_id = ? AND uid = ?", strArr);
    }

    private byte[] getBytes(int i, int i2, String str, String str2) throws Exception {
        Cursor cursorQuery = this.mKeyStoreDbHelper.getReadableDatabase().query("root_of_trust", new String[]{"_id", "user_id", WatchlistLoggingHandler.WatchlistEventKeys.UID, "root_alias", str2}, "user_id = ? AND uid = ? AND root_alias = ?", new String[]{Integer.toString(i), Integer.toString(i2), this.mTestOnlyInsecureCertificateHelper.getDefaultCertificateAliasIfEmpty(str)}, null, null, null);
        try {
            int count = cursorQuery.getCount();
            if (count == 0) {
                return null;
            }
            if (count > 1) {
                Log.wtf(TAG, String.format(Locale.US, "%d entries found for userId=%d uid=%d. Should only ever be 0 or 1.", Integer.valueOf(count), Integer.valueOf(i), Integer.valueOf(i2)));
                if (cursorQuery != null) {
                    $closeResource(null, cursorQuery);
                }
                return null;
            }
            cursorQuery.moveToFirst();
            int columnIndexOrThrow = cursorQuery.getColumnIndexOrThrow(str2);
            if (cursorQuery.isNull(columnIndexOrThrow)) {
                if (cursorQuery != null) {
                    $closeResource(null, cursorQuery);
                }
                return null;
            }
            byte[] blob = cursorQuery.getBlob(columnIndexOrThrow);
            if (cursorQuery != null) {
                $closeResource(null, cursorQuery);
            }
            return blob;
        } finally {
            if (cursorQuery != null) {
                $closeResource(null, cursorQuery);
            }
        }
    }

    private long setBytes(int i, int i2, String str, String str2, byte[] bArr) {
        String defaultCertificateAliasIfEmpty = this.mTestOnlyInsecureCertificateHelper.getDefaultCertificateAliasIfEmpty(str);
        SQLiteDatabase writableDatabase = this.mKeyStoreDbHelper.getWritableDatabase();
        new ContentValues().put(str2, bArr);
        String[] strArr = {Integer.toString(i), Integer.toString(i2), defaultCertificateAliasIfEmpty};
        ensureRootOfTrustEntryExists(i, i2, defaultCertificateAliasIfEmpty);
        return writableDatabase.update("root_of_trust", r1, "user_id = ? AND uid = ? AND root_alias = ?", strArr);
    }

    private Long getLong(int i, int i2, String str, String str2) throws Exception {
        Cursor cursorQuery = this.mKeyStoreDbHelper.getReadableDatabase().query("root_of_trust", new String[]{"_id", "user_id", WatchlistLoggingHandler.WatchlistEventKeys.UID, "root_alias", str2}, "user_id = ? AND uid = ? AND root_alias = ?", new String[]{Integer.toString(i), Integer.toString(i2), this.mTestOnlyInsecureCertificateHelper.getDefaultCertificateAliasIfEmpty(str)}, null, null, null);
        try {
            int count = cursorQuery.getCount();
            if (count == 0) {
                return null;
            }
            if (count > 1) {
                Log.wtf(TAG, String.format(Locale.US, "%d entries found for userId=%d uid=%d. Should only ever be 0 or 1.", Integer.valueOf(count), Integer.valueOf(i), Integer.valueOf(i2)));
                if (cursorQuery != null) {
                    $closeResource(null, cursorQuery);
                }
                return null;
            }
            cursorQuery.moveToFirst();
            int columnIndexOrThrow = cursorQuery.getColumnIndexOrThrow(str2);
            if (cursorQuery.isNull(columnIndexOrThrow)) {
                if (cursorQuery != null) {
                    $closeResource(null, cursorQuery);
                }
                return null;
            }
            Long lValueOf = Long.valueOf(cursorQuery.getLong(columnIndexOrThrow));
            if (cursorQuery != null) {
                $closeResource(null, cursorQuery);
            }
            return lValueOf;
        } finally {
            if (cursorQuery != null) {
                $closeResource(null, cursorQuery);
            }
        }
    }

    private long setLong(int i, int i2, String str, String str2, long j) {
        String defaultCertificateAliasIfEmpty = this.mTestOnlyInsecureCertificateHelper.getDefaultCertificateAliasIfEmpty(str);
        SQLiteDatabase writableDatabase = this.mKeyStoreDbHelper.getWritableDatabase();
        new ContentValues().put(str2, Long.valueOf(j));
        String[] strArr = {Integer.toString(i), Integer.toString(i2), defaultCertificateAliasIfEmpty};
        ensureRootOfTrustEntryExists(i, i2, defaultCertificateAliasIfEmpty);
        return writableDatabase.update("root_of_trust", r1, "user_id = ? AND uid = ? AND root_alias = ?", strArr);
    }

    private void ensureRecoveryServiceMetadataEntryExists(int i, int i2) {
        SQLiteDatabase writableDatabase = this.mKeyStoreDbHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("user_id", Integer.valueOf(i));
        contentValues.put(WatchlistLoggingHandler.WatchlistEventKeys.UID, Integer.valueOf(i2));
        writableDatabase.insertWithOnConflict("recovery_service_metadata", null, contentValues, 4);
    }

    private void ensureRootOfTrustEntryExists(int i, int i2, String str) {
        SQLiteDatabase writableDatabase = this.mKeyStoreDbHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("user_id", Integer.valueOf(i));
        contentValues.put(WatchlistLoggingHandler.WatchlistEventKeys.UID, Integer.valueOf(i2));
        contentValues.put("root_alias", str);
        writableDatabase.insertWithOnConflict("root_of_trust", null, contentValues, 4);
    }

    public void close() {
        this.mKeyStoreDbHelper.close();
    }

    private static PublicKey decodeX509Key(byte[] bArr) throws InvalidKeySpecException {
        try {
            return KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(bArr));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static CertPath decodeCertPath(byte[] bArr) throws CertificateException {
        try {
            return CertificateFactory.getInstance("X.509").generateCertPath(new ByteArrayInputStream(bArr), CERT_PATH_ENCODING);
        } catch (CertificateException e) {
            throw new RuntimeException(e);
        }
    }
}
