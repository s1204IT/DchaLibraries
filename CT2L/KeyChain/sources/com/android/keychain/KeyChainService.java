package com.android.keychain;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ParceledListSlice;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Binder;
import android.os.IBinder;
import android.os.UserHandle;
import android.os.UserManager;
import android.security.IKeyChainService;
import android.security.KeyStore;
import android.util.Log;
import com.android.internal.util.ParcelableString;
import com.android.org.conscrypt.TrustedCertificateStore;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class KeyChainService extends IntentService {
    public DatabaseHelper mDatabaseHelper;
    private final IKeyChainService.Stub mIKeyChainService;

    public KeyChainService() {
        super(KeyChainService.class.getSimpleName());
        this.mIKeyChainService = new IKeyChainService.Stub() {
            private final KeyStore mKeyStore = KeyStore.getInstance();
            private final TrustedCertificateStore mTrustedCertificateStore = new TrustedCertificateStore();

            public String requestPrivateKey(String alias) {
                checkArgs(alias);
                String keystoreAlias = "USRPKEY_" + alias;
                int uid = Binder.getCallingUid();
                if (!this.mKeyStore.grant(keystoreAlias, uid)) {
                    return null;
                }
                int userHandle = UserHandle.getUserId(uid);
                int systemUidForUser = UserHandle.getUid(userHandle, 1000);
                return systemUidForUser + '_' + keystoreAlias;
            }

            public byte[] getCertificate(String alias) {
                checkArgs(alias);
                return this.mKeyStore.get("USRCERT_" + alias);
            }

            private void checkArgs(String alias) {
                if (alias == null) {
                    throw new NullPointerException("alias == null");
                }
                if (!this.mKeyStore.isUnlocked()) {
                    throw new IllegalStateException("keystore is " + this.mKeyStore.state().toString());
                }
                int callingUid = getCallingUid();
                if (!KeyChainService.this.hasGrantInternal(KeyChainService.this.mDatabaseHelper.getReadableDatabase(), callingUid, alias)) {
                    throw new IllegalStateException("uid " + callingUid + " doesn't have permission to access the requested alias");
                }
            }

            public void installCaCertificate(byte[] caCertificate) {
                checkCertInstallerOrSystemCaller();
                checkUserRestriction();
                try {
                    synchronized (this.mTrustedCertificateStore) {
                        this.mTrustedCertificateStore.installCertificate(parseCertificate(caCertificate));
                    }
                    KeyChainService.this.broadcastStorageChange();
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                } catch (CertificateException e2) {
                    throw new IllegalStateException(e2);
                }
            }

            public boolean installKeyPair(byte[] privateKey, byte[] userCertificate, String alias) {
                checkCertInstallerOrSystemCaller();
                if (!this.mKeyStore.importKey("USRPKEY_" + alias, privateKey, -1, 1)) {
                    Log.e("KeyChain", "Failed to import private key " + alias);
                    return false;
                }
                if (this.mKeyStore.put("USRCERT_" + alias, userCertificate, -1, 1)) {
                    KeyChainService.this.broadcastStorageChange();
                    return true;
                }
                Log.e("KeyChain", "Failed to import user certificate " + userCertificate);
                if (this.mKeyStore.delKey("USRPKEY_" + alias)) {
                    return false;
                }
                Log.e("KeyChain", "Failed to delete private key after certificate importing failed");
                return false;
            }

            private X509Certificate parseCertificate(byte[] bytes) throws CertificateException {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(bytes));
            }

            public boolean reset() {
                checkSystemCaller();
                checkUserRestriction();
                KeyChainService.this.removeAllGrants(KeyChainService.this.mDatabaseHelper.getWritableDatabase());
                boolean ok = true;
                synchronized (this.mTrustedCertificateStore) {
                    for (String alias : this.mTrustedCertificateStore.aliases()) {
                        if (TrustedCertificateStore.isUser(alias) && !deleteCertificateEntry(alias)) {
                            ok = false;
                        }
                    }
                }
                KeyChainService.this.broadcastStorageChange();
                return ok;
            }

            public boolean deleteCaCertificate(String alias) {
                boolean ok;
                checkSystemCaller();
                checkUserRestriction();
                synchronized (this.mTrustedCertificateStore) {
                    ok = deleteCertificateEntry(alias);
                }
                KeyChainService.this.broadcastStorageChange();
                return ok;
            }

            private boolean deleteCertificateEntry(String alias) {
                try {
                    this.mTrustedCertificateStore.deleteCertificateEntry(alias);
                    return true;
                } catch (IOException e) {
                    Log.w("KeyChain", "Problem removing CA certificate " + alias, e);
                    return false;
                } catch (CertificateException e2) {
                    Log.w("KeyChain", "Problem removing CA certificate " + alias, e2);
                    return false;
                }
            }

            private void checkCertInstallerOrSystemCaller() {
                String actual = checkCaller("com.android.certinstaller");
                if (actual != null) {
                    checkSystemCaller();
                }
            }

            private void checkSystemCaller() {
                String actual = checkCaller("android.uid.system:1000");
                if (actual != null) {
                    throw new IllegalStateException(actual);
                }
            }

            private void checkUserRestriction() {
                UserManager um = (UserManager) KeyChainService.this.getSystemService("user");
                if (um.hasUserRestriction("no_config_credentials")) {
                    throw new SecurityException("User cannot modify credentials");
                }
            }

            private String checkCaller(String expectedPackage) {
                String actualPackage = KeyChainService.this.getPackageManager().getNameForUid(getCallingUid());
                if (expectedPackage.equals(actualPackage)) {
                    return null;
                }
                return actualPackage;
            }

            public boolean hasGrant(int uid, String alias) {
                checkSystemCaller();
                return KeyChainService.this.hasGrantInternal(KeyChainService.this.mDatabaseHelper.getReadableDatabase(), uid, alias);
            }

            public void setGrant(int uid, String alias, boolean value) {
                checkSystemCaller();
                KeyChainService.this.setGrantInternal(KeyChainService.this.mDatabaseHelper.getWritableDatabase(), uid, alias, value);
                KeyChainService.this.broadcastStorageChange();
            }

            private ParceledListSlice<ParcelableString> makeAliasesParcelableSynchronised(Set<String> aliasSet) {
                List<ParcelableString> aliases = new ArrayList<>(aliasSet.size());
                for (String alias : aliasSet) {
                    ParcelableString parcelableString = new ParcelableString();
                    parcelableString.string = alias;
                    aliases.add(parcelableString);
                }
                return new ParceledListSlice<>(aliases);
            }

            public ParceledListSlice<ParcelableString> getUserCaAliases() {
                ParceledListSlice<ParcelableString> parceledListSliceMakeAliasesParcelableSynchronised;
                synchronized (this.mTrustedCertificateStore) {
                    Set<String> aliasSet = this.mTrustedCertificateStore.userAliases();
                    parceledListSliceMakeAliasesParcelableSynchronised = makeAliasesParcelableSynchronised(aliasSet);
                }
                return parceledListSliceMakeAliasesParcelableSynchronised;
            }

            public ParceledListSlice<ParcelableString> getSystemCaAliases() {
                ParceledListSlice<ParcelableString> parceledListSliceMakeAliasesParcelableSynchronised;
                synchronized (this.mTrustedCertificateStore) {
                    Set<String> aliasSet = this.mTrustedCertificateStore.allSystemAliases();
                    parceledListSliceMakeAliasesParcelableSynchronised = makeAliasesParcelableSynchronised(aliasSet);
                }
                return parceledListSliceMakeAliasesParcelableSynchronised;
            }

            public boolean containsCaAlias(String alias) {
                return this.mTrustedCertificateStore.containsAlias(alias);
            }

            public byte[] getEncodedCaCertificate(String alias, boolean includeDeletedSystem) {
                byte[] encoded = null;
                synchronized (this.mTrustedCertificateStore) {
                    X509Certificate certificate = (X509Certificate) this.mTrustedCertificateStore.getCertificate(alias, includeDeletedSystem);
                    if (certificate == null) {
                        Log.w("KeyChain", "Could not find CA certificate " + alias);
                    } else {
                        try {
                            encoded = certificate.getEncoded();
                        } catch (CertificateEncodingException e) {
                            Log.w("KeyChain", "Error while encoding CA certificate " + alias);
                        }
                    }
                }
                return encoded;
            }

            public List<String> getCaCertificateChainAliases(String rootAlias, boolean includeDeletedSystem) {
                List<String> aliases;
                synchronized (this.mTrustedCertificateStore) {
                    X509Certificate root = (X509Certificate) this.mTrustedCertificateStore.getCertificate(rootAlias, includeDeletedSystem);
                    try {
                        List<X509Certificate> chain = this.mTrustedCertificateStore.getCertificateChain(root);
                        aliases = new ArrayList<>(chain.size());
                        int n = chain.size();
                        for (int i = 0; i < n; i++) {
                            String alias = this.mTrustedCertificateStore.getCertificateAlias(chain.get(i), true);
                            if (alias != null) {
                                aliases.add(alias);
                            }
                        }
                    } catch (CertificateException e) {
                        Log.w("KeyChain", "Error retrieving cert chain for root " + rootAlias);
                        aliases = Collections.emptyList();
                    }
                }
                return aliases;
            }
        };
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.mDatabaseHelper = new DatabaseHelper(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.mDatabaseHelper.close();
        this.mDatabaseHelper = null;
    }

    private boolean hasGrantInternal(SQLiteDatabase db, int uid, String alias) {
        long numMatches = DatabaseUtils.longForQuery(db, "SELECT COUNT(*) FROM grants WHERE uid=? AND alias=?", new String[]{String.valueOf(uid), alias});
        return numMatches > 0;
    }

    private void setGrantInternal(SQLiteDatabase db, int uid, String alias, boolean value) {
        if (value) {
            if (!hasGrantInternal(db, uid, alias)) {
                ContentValues values = new ContentValues();
                values.put("alias", alias);
                values.put("uid", Integer.valueOf(uid));
                db.insert("grants", "alias", values);
                return;
            }
            return;
        }
        db.delete("grants", "uid=? AND alias=?", new String[]{String.valueOf(uid), alias});
    }

    private void removeAllGrants(SQLiteDatabase db) {
        db.delete("grants", null, null);
    }

    private class DatabaseHelper extends SQLiteOpenHelper {
        public DatabaseHelper(Context context) {
            super(context, "grants.db", (SQLiteDatabase.CursorFactory) null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE grants (  alias STRING NOT NULL,  uid INTEGER NOT NULL,  UNIQUE (alias,uid))");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.e("KeyChain", "upgrade from version " + oldVersion + " to version " + newVersion);
            if (oldVersion == 1) {
                int i = oldVersion + 1;
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (IKeyChainService.class.getName().equals(intent.getAction())) {
            return this.mIKeyChainService;
        }
        return null;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if ("android.intent.action.PACKAGE_REMOVED".equals(intent.getAction())) {
            purgeOldGrants();
        }
    }

    private void purgeOldGrants() {
        PackageManager packageManager = getPackageManager();
        SQLiteDatabase db = this.mDatabaseHelper.getWritableDatabase();
        Cursor cursor = null;
        db.beginTransaction();
        try {
            cursor = db.query("grants", new String[]{"uid"}, null, null, "uid", null, null);
            while (cursor.moveToNext()) {
                int uid = cursor.getInt(0);
                boolean packageExists = packageManager.getPackagesForUid(uid) != null;
                if (!packageExists) {
                    Log.d("KeyChain", "deleting grants for UID " + uid + " because its package is no longer installed");
                    db.delete("grants", "uid=?", new String[]{Integer.toString(uid)});
                }
            }
            db.setTransactionSuccessful();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.endTransaction();
        }
    }

    private void broadcastStorageChange() {
        Intent intent = new Intent("android.security.STORAGE_CHANGED");
        sendBroadcastAsUser(intent, new UserHandle(UserHandle.myUserId()));
    }
}
