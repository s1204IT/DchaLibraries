package com.android.server.accounts;

import android.R;
import android.accounts.Account;
import android.accounts.AccountAndUser;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AuthenticatorDescription;
import android.accounts.CantAddAccountActivity;
import android.accounts.GrantCredentialsPermissionActivity;
import android.accounts.IAccountAuthenticator;
import android.accounts.IAccountAuthenticatorResponse;
import android.accounts.IAccountManager;
import android.accounts.IAccountManagerResponse;
import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.app.admin.DevicePolicyManagerInternal;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.RegisteredServicesCache;
import android.content.pm.RegisteredServicesCacheListener;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.content.pm.UserInfo;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.Preconditions;
import com.android.server.FgThread;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.pm.PackageManagerService;
import com.android.server.voiceinteraction.DatabaseHelper;
import com.google.android.collect.Lists;
import com.google.android.collect.Sets;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class AccountManagerService extends IAccountManager.Stub implements RegisteredServicesCacheListener<AuthenticatorDescription> {
    private static final String ACCOUNTS_ID = "_id";
    private static final String ACCOUNTS_LAST_AUTHENTICATE_TIME_EPOCH_MILLIS = "last_password_entry_time_millis_epoch";
    private static final String ACCOUNTS_NAME = "name";
    private static final String ACCOUNTS_PASSWORD = "password";
    private static final String ACCOUNTS_PREVIOUS_NAME = "previous_name";
    private static final String ACCOUNTS_TYPE = "type";
    private static final String AUTHTOKENS_ACCOUNTS_ID = "accounts_id";
    private static final String AUTHTOKENS_AUTHTOKEN = "authtoken";
    private static final String AUTHTOKENS_ID = "_id";
    private static final String AUTHTOKENS_TYPE = "type";
    private static final String CE_DATABASE_NAME = "accounts_ce.db";
    private static final int CE_DATABASE_VERSION = 10;
    private static final String CE_DB_PREFIX = "ceDb.";
    private static final String CE_TABLE_ACCOUNTS = "ceDb.accounts";
    private static final String CE_TABLE_AUTHTOKENS = "ceDb.authtokens";
    private static final String CE_TABLE_EXTRAS = "ceDb.extras";
    private static final String[] COLUMNS_AUTHTOKENS_TYPE_AND_AUTHTOKEN;
    private static final String[] COLUMNS_EXTRAS_KEY_AND_VALUE;
    private static final String COUNT_OF_MATCHING_GRANTS = "SELECT COUNT(*) FROM grants, accounts WHERE accounts_id=_id AND uid=? AND auth_token_type=? AND name=? AND type=?";
    private static final String DATABASE_NAME = "accounts.db";
    private static final String DE_DATABASE_NAME = "accounts_de.db";
    private static final int DE_DATABASE_VERSION = 1;
    private static final Account[] EMPTY_ACCOUNT_ARRAY;
    private static final String EXTRAS_ACCOUNTS_ID = "accounts_id";
    private static final String EXTRAS_ID = "_id";
    private static final String EXTRAS_KEY = "key";
    private static final String EXTRAS_VALUE = "value";
    private static final String GRANTS_ACCOUNTS_ID = "accounts_id";
    private static final String GRANTS_AUTH_TOKEN_TYPE = "auth_token_type";
    private static final String GRANTS_GRANTEE_UID = "uid";
    private static final int MAX_DEBUG_DB_SIZE = 64;
    private static final int MESSAGE_COPY_SHARED_ACCOUNT = 4;
    private static final int MESSAGE_TIMED_OUT = 3;
    private static final String META_KEY = "key";
    private static final String META_KEY_DELIMITER = ":";
    private static final String META_KEY_FOR_AUTHENTICATOR_UID_FOR_TYPE_PREFIX = "auth_uid_for_type:";
    private static final String META_VALUE = "value";
    private static final String PRE_N_DATABASE_NAME = "accounts.db";
    private static final int PRE_N_DATABASE_VERSION = 9;
    private static final String SELECTION_AUTHTOKENS_BY_ACCOUNT = "accounts_id=(select _id FROM accounts WHERE name=? AND type=?)";
    private static final String SELECTION_META_BY_AUTHENTICATOR_TYPE = "key LIKE ?";
    private static final String SELECTION_USERDATA_BY_ACCOUNT = "accounts_id=(select _id FROM accounts WHERE name=? AND type=?)";
    private static final String SHARED_ACCOUNTS_ID = "_id";
    private static final String TABLE_ACCOUNTS = "accounts";
    private static final String TABLE_AUTHTOKENS = "authtokens";
    private static final String TABLE_EXTRAS = "extras";
    private static final String TABLE_GRANTS = "grants";
    private static final String TABLE_META = "meta";
    private static final String TABLE_SHARED_ACCOUNTS = "shared_accounts";
    private static final String TAG = "AccountManager";
    private static AtomicReference<AccountManagerService> sThis;
    private final AppOpsManager mAppOpsManager;
    private final IAccountAuthenticatorCache mAuthenticatorCache;
    private final Context mContext;
    private final SparseBooleanArray mLocalUnlockedUsers;
    private final MessageHandler mMessageHandler;
    private final AtomicInteger mNotificationIds;
    private final PackageManager mPackageManager;
    private final LinkedHashMap<String, Session> mSessions;
    private UserManager mUserManager;
    private final SparseArray<UserAccounts> mUsers;
    private static final String ACCOUNTS_TYPE_COUNT = "count(type)";
    private static final String[] ACCOUNT_TYPE_COUNT_PROJECTION = {DatabaseHelper.SoundModelContract.KEY_TYPE, ACCOUNTS_TYPE_COUNT};
    private static final Intent ACCOUNTS_CHANGED_INTENT = new Intent("android.accounts.LOGIN_ACCOUNTS_CHANGED");

    public static class Lifecycle extends SystemService {
        private AccountManagerService mService;

        public Lifecycle(Context context) {
            super(context);
        }

        @Override
        public void onStart() {
            this.mService = new AccountManagerService(getContext());
            publishBinderService("account", this.mService);
        }

        @Override
        public void onBootPhase(int phase) {
            if (phase != 550) {
                return;
            }
            this.mService.systemReady();
        }

        @Override
        public void onUnlockUser(int userHandle) {
            this.mService.onUnlockUser(userHandle);
        }
    }

    static {
        ACCOUNTS_CHANGED_INTENT.setFlags(67108864);
        COLUMNS_AUTHTOKENS_TYPE_AND_AUTHTOKEN = new String[]{DatabaseHelper.SoundModelContract.KEY_TYPE, AUTHTOKENS_AUTHTOKEN};
        COLUMNS_EXTRAS_KEY_AND_VALUE = new String[]{"key", "value"};
        sThis = new AtomicReference<>();
        EMPTY_ACCOUNT_ARRAY = new Account[0];
    }

    static class UserAccounts {
        private final DeDatabaseHelper openHelper;
        private SQLiteStatement statementForLogging;
        private final int userId;
        private final HashMap<Pair<Pair<Account, String>, Integer>, Integer> credentialsPermissionNotificationIds = new HashMap<>();
        private final HashMap<Account, Integer> signinRequiredNotificationIds = new HashMap<>();
        private final Object cacheLock = new Object();
        private final HashMap<String, Account[]> accountCache = new LinkedHashMap();
        private final HashMap<Account, HashMap<String, String>> userDataCache = new HashMap<>();
        private final HashMap<Account, HashMap<String, String>> authTokenCache = new HashMap<>();
        private final TokenCache accountTokenCaches = new TokenCache();
        private final HashMap<Account, AtomicReference<String>> previousNameCache = new HashMap<>();
        private int debugDbInsertionPoint = -1;

        UserAccounts(Context context, int userId, File preNDbFile, File deDbFile) {
            this.userId = userId;
            synchronized (this.cacheLock) {
                this.openHelper = DeDatabaseHelper.create(context, userId, preNDbFile, deDbFile);
            }
        }
    }

    public static AccountManagerService getSingleton() {
        return sThis.get();
    }

    public AccountManagerService(Context context) {
        this(context, context.getPackageManager(), new AccountAuthenticatorCache(context));
    }

    public AccountManagerService(Context context, PackageManager packageManager, IAccountAuthenticatorCache authenticatorCache) {
        this.mSessions = new LinkedHashMap<>();
        this.mNotificationIds = new AtomicInteger(1);
        this.mUsers = new SparseArray<>();
        this.mLocalUnlockedUsers = new SparseBooleanArray();
        this.mContext = context;
        this.mPackageManager = packageManager;
        this.mAppOpsManager = (AppOpsManager) this.mContext.getSystemService(AppOpsManager.class);
        this.mMessageHandler = new MessageHandler(FgThread.get().getLooper());
        this.mAuthenticatorCache = authenticatorCache;
        this.mAuthenticatorCache.setListener(this, null);
        sThis.set(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        intentFilter.addDataScheme("package");
        this.mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context1, Intent intent) {
                if (intent.getBooleanExtra("android.intent.extra.REPLACING", false)) {
                    return;
                }
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        AccountManagerService.this.purgeOldGrantsAll();
                    }
                };
                new Thread(r).start();
            }
        }, intentFilter);
        IntentFilter userFilter = new IntentFilter();
        userFilter.addAction("android.intent.action.USER_REMOVED");
        this.mContext.registerReceiverAsUser(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                if (!"android.intent.action.USER_REMOVED".equals(action)) {
                    return;
                }
                AccountManagerService.this.onUserRemoved(intent);
            }
        }, UserHandle.ALL, userFilter, null, null);
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        try {
            return super.onTransact(code, data, reply, flags);
        } catch (RuntimeException e) {
            if (!(e instanceof SecurityException)) {
                Slog.wtf(TAG, "Account Manager Crash", e);
            }
            throw e;
        }
    }

    public void systemReady() {
    }

    private UserManager getUserManager() {
        if (this.mUserManager == null) {
            this.mUserManager = UserManager.get(this.mContext);
        }
        return this.mUserManager;
    }

    public void validateAccounts(int userId) {
        UserAccounts accounts = getUserAccounts(userId);
        validateAccountsInternal(accounts, true);
    }

    private void validateAccountsInternal(UserAccounts accounts, boolean invalidateAuthenticatorCache) {
        if (Log.isLoggable(TAG, 3)) {
            Log.d(TAG, "validateAccountsInternal " + accounts.userId + " isCeDatabaseAttached=" + accounts.openHelper.isCeDatabaseAttached() + " userLocked=" + this.mLocalUnlockedUsers.get(accounts.userId));
        }
        if (invalidateAuthenticatorCache) {
            this.mAuthenticatorCache.invalidateCache(accounts.userId);
        }
        HashMap<String, Integer> knownAuth = getAuthenticatorTypeAndUIDForUser(this.mAuthenticatorCache, accounts.userId);
        boolean userUnlocked = isLocalUnlockedUser(accounts.userId);
        synchronized (accounts.cacheLock) {
            SQLiteDatabase db = accounts.openHelper.getWritableDatabase();
            boolean accountDeleted = false;
            Cursor cursor = db.query(TABLE_META, new String[]{"key", "value"}, SELECTION_META_BY_AUTHENTICATOR_TYPE, new String[]{"auth_uid_for_type:%"}, null, null, "key");
            HashSet<String> obsoleteAuthType = Sets.newHashSet();
            SparseBooleanArray uidsOfInstalledOrUpdatedPackagesAsUser = null;
            while (cursor.moveToNext()) {
                try {
                    String type = TextUtils.split(cursor.getString(0), META_KEY_DELIMITER)[1];
                    String uid = cursor.getString(1);
                    if (TextUtils.isEmpty(type) || TextUtils.isEmpty(uid)) {
                        Slog.e(TAG, "Auth type empty: " + TextUtils.isEmpty(type) + ", uid empty: " + TextUtils.isEmpty(uid));
                    } else {
                        Integer knownUid = knownAuth.get(type);
                        if (knownUid == null || !uid.equals(knownUid.toString())) {
                            if (uidsOfInstalledOrUpdatedPackagesAsUser == null) {
                                uidsOfInstalledOrUpdatedPackagesAsUser = getUidsOfInstalledOrUpdatedPackagesAsUser(accounts.userId);
                            }
                            if (!uidsOfInstalledOrUpdatedPackagesAsUser.get(Integer.parseInt(uid))) {
                                obsoleteAuthType.add(type);
                                db.delete(TABLE_META, "key=? AND value=?", new String[]{META_KEY_FOR_AUTHENTICATOR_UID_FOR_TYPE_PREFIX + type, uid});
                            }
                        } else {
                            knownAuth.remove(type);
                        }
                    }
                } finally {
                    cursor.close();
                }
            }
            cursor.close();
            for (Map.Entry<String, Integer> entry : knownAuth.entrySet()) {
                ContentValues values = new ContentValues();
                values.put("key", META_KEY_FOR_AUTHENTICATOR_UID_FOR_TYPE_PREFIX + entry.getKey());
                values.put("value", entry.getValue());
                db.insertWithOnConflict(TABLE_META, null, values, 5);
            }
            cursor = db.query(TABLE_ACCOUNTS, new String[]{"_id", DatabaseHelper.SoundModelContract.KEY_TYPE, ACCOUNTS_NAME}, null, null, null, null, "_id");
            try {
                accounts.accountCache.clear();
                HashMap<String, ArrayList<String>> accountNamesByType = new LinkedHashMap<>();
                while (cursor.moveToNext()) {
                    long accountId = cursor.getLong(0);
                    String accountType = cursor.getString(1);
                    String accountName = cursor.getString(2);
                    if (obsoleteAuthType.contains(accountType)) {
                        Slog.w(TAG, "deleting account " + accountName + " because type " + accountType + "'s registered authenticator no longer exist.");
                        db.beginTransaction();
                        try {
                            db.delete(TABLE_ACCOUNTS, "_id=" + accountId, null);
                            if (userUnlocked) {
                                db.delete(CE_TABLE_ACCOUNTS, "_id=" + accountId, null);
                            }
                            db.setTransactionSuccessful();
                            db.endTransaction();
                            accountDeleted = true;
                            logRecord(db, DebugDbHelper.ACTION_AUTHENTICATOR_REMOVE, TABLE_ACCOUNTS, accountId, accounts);
                            Account account = new Account(accountName, accountType);
                            accounts.userDataCache.remove(account);
                            accounts.authTokenCache.remove(account);
                            accounts.accountTokenCaches.remove(account);
                        } catch (Throwable th) {
                            db.endTransaction();
                            throw th;
                        }
                    } else {
                        ArrayList<String> accountNames = accountNamesByType.get(accountType);
                        if (accountNames == null) {
                            accountNames = new ArrayList<>();
                            accountNamesByType.put(accountType, accountNames);
                        }
                        accountNames.add(accountName);
                    }
                }
                for (Map.Entry<String, ArrayList<String>> cur : accountNamesByType.entrySet()) {
                    String accountType2 = cur.getKey();
                    ArrayList<String> accountNames2 = cur.getValue();
                    Account[] accountsForType = new Account[accountNames2.size()];
                    for (int i = 0; i < accountsForType.length; i++) {
                        accountsForType[i] = new Account(accountNames2.get(i), accountType2);
                    }
                    accounts.accountCache.put(accountType2, accountsForType);
                }
            } finally {
                if (accountDeleted) {
                    sendAccountsChangedBroadcast(accounts.userId);
                }
            }
        }
    }

    private SparseBooleanArray getUidsOfInstalledOrUpdatedPackagesAsUser(int userId) {
        List<PackageInfo> pkgsWithData = this.mPackageManager.getInstalledPackagesAsUser(PackageManagerService.DumpState.DUMP_PREFERRED_XML, userId);
        SparseBooleanArray knownUids = new SparseBooleanArray(pkgsWithData.size());
        for (PackageInfo pkgInfo : pkgsWithData) {
            if (pkgInfo.applicationInfo != null && (pkgInfo.applicationInfo.flags & 8388608) != 0) {
                knownUids.put(pkgInfo.applicationInfo.uid, true);
            }
        }
        return knownUids;
    }

    private static HashMap<String, Integer> getAuthenticatorTypeAndUIDForUser(Context context, int userId) {
        AccountAuthenticatorCache authCache = new AccountAuthenticatorCache(context);
        return getAuthenticatorTypeAndUIDForUser(authCache, userId);
    }

    private static HashMap<String, Integer> getAuthenticatorTypeAndUIDForUser(IAccountAuthenticatorCache authCache, int userId) {
        HashMap<String, Integer> knownAuth = new HashMap<>();
        for (RegisteredServicesCache.ServiceInfo<AuthenticatorDescription> service : authCache.getAllServices(userId)) {
            knownAuth.put(((AuthenticatorDescription) service.type).type, Integer.valueOf(service.uid));
        }
        return knownAuth;
    }

    private UserAccounts getUserAccountsForCaller() {
        return getUserAccounts(UserHandle.getCallingUserId());
    }

    protected UserAccounts getUserAccounts(int userId) {
        UserAccounts accounts;
        synchronized (this.mUsers) {
            accounts = this.mUsers.get(userId);
            boolean validateAccounts = false;
            if (accounts == null) {
                File preNDbFile = new File(getPreNDatabaseName(userId));
                File deDbFile = new File(getDeDatabaseName(userId));
                accounts = new UserAccounts(this.mContext, userId, preNDbFile, deDbFile);
                initializeDebugDbSizeAndCompileSqlStatementForLogging(accounts.openHelper.getWritableDatabase(), accounts);
                this.mUsers.append(userId, accounts);
                purgeOldGrants(accounts);
                validateAccounts = true;
            }
            if (!accounts.openHelper.isCeDatabaseAttached() && this.mLocalUnlockedUsers.get(userId)) {
                Log.i(TAG, "User " + userId + " is unlocked - opening CE database");
                synchronized (accounts.cacheLock) {
                    File preNDatabaseFile = new File(getPreNDatabaseName(userId));
                    File ceDatabaseFile = new File(getCeDatabaseName(userId));
                    CeDatabaseHelper.create(this.mContext, userId, preNDatabaseFile, ceDatabaseFile);
                    accounts.openHelper.attachCeDatabase(ceDatabaseFile);
                }
                syncDeCeAccountsLocked(accounts);
            }
            if (validateAccounts) {
                validateAccountsInternal(accounts, true);
            }
        }
        return accounts;
    }

    private void syncDeCeAccountsLocked(UserAccounts accounts) {
        Preconditions.checkState(Thread.holdsLock(this.mUsers), "mUsers lock must be held");
        SQLiteDatabase db = accounts.openHelper.getReadableDatabaseUserIsUnlocked();
        List<Account> accountsToRemove = CeDatabaseHelper.findCeAccountsNotInDe(db);
        if (accountsToRemove.isEmpty()) {
            return;
        }
        Slog.i(TAG, "Accounts " + accountsToRemove + " were previously deleted while user " + accounts.userId + " was locked. Removing accounts from CE tables");
        logRecord(accounts, DebugDbHelper.ACTION_SYNC_DE_CE_ACCOUNTS, TABLE_ACCOUNTS);
        for (Account account : accountsToRemove) {
            removeAccountInternal(accounts, account, Process.myUid());
        }
    }

    private void purgeOldGrantsAll() {
        synchronized (this.mUsers) {
            for (int i = 0; i < this.mUsers.size(); i++) {
                purgeOldGrants(this.mUsers.valueAt(i));
            }
        }
    }

    private void purgeOldGrants(UserAccounts accounts) {
        synchronized (accounts.cacheLock) {
            SQLiteDatabase db = accounts.openHelper.getWritableDatabase();
            Cursor cursor = db.query(TABLE_GRANTS, new String[]{"uid"}, null, null, "uid", null, null);
            while (cursor.moveToNext()) {
                try {
                    int uid = cursor.getInt(0);
                    boolean packageExists = this.mPackageManager.getPackagesForUid(uid) != null;
                    if (!packageExists) {
                        Log.d(TAG, "deleting grants for UID " + uid + " because its package is no longer installed");
                        db.delete(TABLE_GRANTS, "uid=?", new String[]{Integer.toString(uid)});
                    }
                } finally {
                    cursor.close();
                }
            }
        }
    }

    private void onUserRemoved(Intent intent) {
        UserAccounts accounts;
        boolean userUnlocked;
        int userId = intent.getIntExtra("android.intent.extra.user_handle", -1);
        if (userId < 1) {
            return;
        }
        synchronized (this.mUsers) {
            accounts = this.mUsers.get(userId);
            this.mUsers.remove(userId);
            userUnlocked = this.mLocalUnlockedUsers.get(userId);
            this.mLocalUnlockedUsers.delete(userId);
        }
        if (accounts != null) {
            synchronized (accounts.cacheLock) {
                accounts.openHelper.close();
            }
        }
        Log.i(TAG, "Removing database files for user " + userId);
        File dbFile = new File(getDeDatabaseName(userId));
        deleteDbFileWarnIfFailed(dbFile);
        boolean fbeEnabled = StorageManager.isFileEncryptedNativeOrEmulated();
        if (fbeEnabled && !userUnlocked) {
            return;
        }
        File ceDb = new File(getCeDatabaseName(userId));
        if (!ceDb.exists()) {
            return;
        }
        deleteDbFileWarnIfFailed(ceDb);
    }

    private static void deleteDbFileWarnIfFailed(File dbFile) {
        if (SQLiteDatabase.deleteDatabase(dbFile)) {
            return;
        }
        Log.w(TAG, "Database at " + dbFile + " was not deleted successfully");
    }

    void onUserUnlocked(Intent intent) {
        onUnlockUser(intent.getIntExtra("android.intent.extra.user_handle", -1));
    }

    void onUnlockUser(int userId) {
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "onUserUnlocked " + userId);
        }
        synchronized (this.mUsers) {
            this.mLocalUnlockedUsers.put(userId, true);
        }
        if (userId < 1) {
            return;
        }
        syncSharedAccounts(userId);
    }

    private void syncSharedAccounts(int userId) {
        Account[] sharedAccounts = getSharedAccountsAsUser(userId);
        if (sharedAccounts == null || sharedAccounts.length == 0) {
            return;
        }
        Account[] accounts = getAccountsAsUser(null, userId, this.mContext.getOpPackageName());
        int parentUserId = UserManager.isSplitSystemUser() ? getUserManager().getUserInfo(userId).restrictedProfileParentId : 0;
        if (parentUserId < 0) {
            Log.w(TAG, "User " + userId + " has shared accounts, but no parent user");
            return;
        }
        for (Account sa : sharedAccounts) {
            if (!ArrayUtils.contains(accounts, sa)) {
                copyAccountToUser(null, sa, parentUserId, userId);
            }
        }
    }

    public void onServiceChanged(AuthenticatorDescription desc, int userId, boolean removed) {
        validateAccountsInternal(getUserAccounts(userId), false);
    }

    public String getPassword(Account account) {
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "getPassword: " + account + ", caller's uid " + Binder.getCallingUid() + ", pid " + Binder.getCallingPid());
        }
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        int userId = UserHandle.getCallingUserId();
        if (!isAccountManagedByCaller(account.type, callingUid, userId)) {
            String msg = String.format("uid %s cannot get secrets for accounts of type: %s", Integer.valueOf(callingUid), account.type);
            throw new SecurityException(msg);
        }
        long identityToken = clearCallingIdentity();
        try {
            UserAccounts accounts = getUserAccounts(userId);
            return readPasswordInternal(accounts, account);
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    private String readPasswordInternal(UserAccounts accounts, Account account) {
        String strFindAccountPasswordByNameAndType;
        if (account == null) {
            return null;
        }
        if (!isLocalUnlockedUser(accounts.userId)) {
            Log.w(TAG, "Password is not available - user " + accounts.userId + " data is locked");
            return null;
        }
        synchronized (accounts.cacheLock) {
            SQLiteDatabase db = accounts.openHelper.getReadableDatabaseUserIsUnlocked();
            strFindAccountPasswordByNameAndType = CeDatabaseHelper.findAccountPasswordByNameAndType(db, account.name, account.type);
        }
        return strFindAccountPasswordByNameAndType;
    }

    public String getPreviousName(Account account) {
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "getPreviousName: " + account + ", caller's uid " + Binder.getCallingUid() + ", pid " + Binder.getCallingPid());
        }
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        int userId = UserHandle.getCallingUserId();
        long identityToken = clearCallingIdentity();
        try {
            UserAccounts accounts = getUserAccounts(userId);
            return readPreviousNameInternal(accounts, account);
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    private String readPreviousNameInternal(UserAccounts accounts, Account account) {
        if (account == null) {
            return null;
        }
        synchronized (accounts.cacheLock) {
            AtomicReference<String> previousNameRef = (AtomicReference) accounts.previousNameCache.get(account);
            if (previousNameRef == null) {
                SQLiteDatabase db = accounts.openHelper.getReadableDatabase();
                Cursor cursor = db.query(TABLE_ACCOUNTS, new String[]{ACCOUNTS_PREVIOUS_NAME}, "name=? AND type=?", new String[]{account.name, account.type}, null, null, null);
                try {
                    if (cursor.moveToNext()) {
                        String previousName = cursor.getString(0);
                        try {
                            accounts.previousNameCache.put(account, new AtomicReference<>(previousName));
                            cursor.close();
                            return previousName;
                        } catch (Throwable th) {
                            th = th;
                        }
                    } else {
                        cursor.close();
                        return null;
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
                cursor.close();
                throw th;
            }
            return previousNameRef.get();
        }
    }

    public String getUserData(Account account, String key) {
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            String msg = String.format("getUserData( account: %s, key: %s, callerUid: %s, pid: %s", account, key, Integer.valueOf(callingUid), Integer.valueOf(Binder.getCallingPid()));
            Log.v(TAG, msg);
        }
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        if (key == null) {
            throw new IllegalArgumentException("key is null");
        }
        int userId = UserHandle.getCallingUserId();
        if (!isAccountManagedByCaller(account.type, callingUid, userId)) {
            String msg2 = String.format("uid %s cannot get user data for accounts of type: %s", Integer.valueOf(callingUid), account.type);
            throw new SecurityException(msg2);
        }
        if (!isLocalUnlockedUser(userId)) {
            Log.w(TAG, "User " + userId + " data is locked. callingUid " + callingUid);
            return null;
        }
        long identityToken = clearCallingIdentity();
        try {
            UserAccounts accounts = getUserAccounts(userId);
            synchronized (accounts.cacheLock) {
                if (accountExistsCacheLocked(accounts, account)) {
                    return readUserDataInternalLocked(accounts, account, key);
                }
                return null;
            }
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public AuthenticatorDescription[] getAuthenticatorTypes(int userId) {
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "getAuthenticatorTypes: for user id " + userId + " caller's uid " + callingUid + ", pid " + Binder.getCallingPid());
        }
        if (isCrossUser(callingUid, userId)) {
            throw new SecurityException(String.format("User %s tying to get authenticator types for %s", Integer.valueOf(UserHandle.getCallingUserId()), Integer.valueOf(userId)));
        }
        long identityToken = clearCallingIdentity();
        try {
            return getAuthenticatorTypesInternal(userId);
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    private AuthenticatorDescription[] getAuthenticatorTypesInternal(int userId) {
        Collection<RegisteredServicesCache.ServiceInfo<AuthenticatorDescription>> allServices = this.mAuthenticatorCache.getAllServices(userId);
        AuthenticatorDescription[] types = new AuthenticatorDescription[allServices.size()];
        int i = 0;
        Iterator authenticator$iterator = allServices.iterator();
        while (authenticator$iterator.hasNext()) {
            RegisteredServicesCache<AuthenticatorDescription>.ServiceInfo<AuthenticatorDescription> authenticator = (RegisteredServicesCache.ServiceInfo) authenticator$iterator.next();
            types[i] = (AuthenticatorDescription) authenticator.type;
            i++;
        }
        return types;
    }

    private boolean isCrossUser(int callingUid, int userId) {
        return (userId == UserHandle.getCallingUserId() || callingUid == Process.myUid() || this.mContext.checkCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL") == 0) ? false : true;
    }

    public boolean addAccountExplicitly(Account account, String password, Bundle extras) {
        Bundle.setDefusable(extras, true);
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "addAccountExplicitly: " + account + ", caller's uid " + callingUid + ", pid " + Binder.getCallingPid());
        }
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        int userId = UserHandle.getCallingUserId();
        if (!isAccountManagedByCaller(account.type, callingUid, userId)) {
            String msg = String.format("uid %s cannot explicitly add accounts of type: %s", Integer.valueOf(callingUid), account.type);
            throw new SecurityException(msg);
        }
        long identityToken = clearCallingIdentity();
        try {
            UserAccounts accounts = getUserAccounts(userId);
            return addAccountInternal(accounts, account, password, extras, callingUid);
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public void copyAccountToUser(final IAccountManagerResponse response, final Account account, final int userFrom, int userTo) {
        int callingUid = Binder.getCallingUid();
        if (isCrossUser(callingUid, -1)) {
            throw new SecurityException("Calling copyAccountToUser requires android.permission.INTERACT_ACROSS_USERS_FULL");
        }
        UserAccounts fromAccounts = getUserAccounts(userFrom);
        final UserAccounts toAccounts = getUserAccounts(userTo);
        if (fromAccounts == null || toAccounts == null) {
            if (response != null) {
                Bundle result = new Bundle();
                result.putBoolean("booleanResult", false);
                try {
                    response.onResult(result);
                    return;
                } catch (RemoteException e) {
                    Slog.w(TAG, "Failed to report error back to the client." + e);
                    return;
                }
            }
            return;
        }
        Slog.d(TAG, "Copying account " + account.name + " from user " + userFrom + " to user " + userTo);
        long identityToken = clearCallingIdentity();
        try {
            new Session(this, fromAccounts, response, account.type, false, false, account.name, false) {
                @Override
                protected String toDebugString(long now) {
                    return super.toDebugString(now) + ", getAccountCredentialsForClone, " + account.type;
                }

                @Override
                public void run() throws RemoteException {
                    this.mAuthenticator.getAccountCredentialsForCloning(this, account);
                }

                @Override
                public void onResult(Bundle result2) {
                    Bundle.setDefusable(result2, true);
                    if (result2 != null && result2.getBoolean("booleanResult", false)) {
                        this.completeCloningAccount(response, result2, account, toAccounts, userFrom);
                    } else {
                        super.onResult(result2);
                    }
                }
            }.bind();
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public boolean accountAuthenticated(Account account) {
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            String msg = String.format("accountAuthenticated( account: %s, callerUid: %s)", account, Integer.valueOf(callingUid));
            Log.v(TAG, msg);
        }
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        int userId = UserHandle.getCallingUserId();
        if (!isAccountManagedByCaller(account.type, callingUid, userId)) {
            String msg2 = String.format("uid %s cannot notify authentication for accounts of type: %s", Integer.valueOf(callingUid), account.type);
            throw new SecurityException(msg2);
        }
        if (!canUserModifyAccounts(userId, callingUid) || !canUserModifyAccountsForType(userId, account.type, callingUid)) {
            return false;
        }
        long identityToken = clearCallingIdentity();
        try {
            getUserAccounts(userId);
            return updateLastAuthenticatedTime(account);
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    private boolean updateLastAuthenticatedTime(Account account) {
        UserAccounts accounts = getUserAccountsForCaller();
        synchronized (accounts.cacheLock) {
            ContentValues values = new ContentValues();
            values.put(ACCOUNTS_LAST_AUTHENTICATE_TIME_EPOCH_MILLIS, Long.valueOf(System.currentTimeMillis()));
            SQLiteDatabase db = accounts.openHelper.getWritableDatabase();
            int i = db.update(TABLE_ACCOUNTS, values, "name=? AND type=?", new String[]{account.name, account.type});
            return i > 0;
        }
    }

    private void completeCloningAccount(IAccountManagerResponse response, final Bundle accountCredentials, final Account account, UserAccounts targetUser, final int parentUserId) {
        Bundle.setDefusable(accountCredentials, true);
        long id = clearCallingIdentity();
        try {
            new Session(this, targetUser, response, account.type, false, false, account.name, false) {
                @Override
                protected String toDebugString(long now) {
                    return super.toDebugString(now) + ", getAccountCredentialsForClone, " + account.type;
                }

                @Override
                public void run() throws RemoteException {
                    UserAccounts owner = this.getUserAccounts(parentUserId);
                    synchronized (owner.cacheLock) {
                        Account[] accounts = this.getAccounts(parentUserId, this.mContext.getOpPackageName());
                        int i = 0;
                        int length = accounts.length;
                        while (true) {
                            if (i >= length) {
                                break;
                            }
                            Account acc = accounts[i];
                            if (acc.equals(account)) {
                                break;
                            } else {
                                i++;
                            }
                        }
                    }
                }

                @Override
                public void onResult(Bundle result) {
                    Bundle.setDefusable(result, true);
                    super.onResult(result);
                }

                @Override
                public void onError(int errorCode, String errorMessage) {
                    super.onError(errorCode, errorMessage);
                }
            }.bind();
        } finally {
            restoreCallingIdentity(id);
        }
    }

    private boolean addAccountInternal(UserAccounts accounts, Account account, String password, Bundle extras, int callingUid) {
        Bundle.setDefusable(extras, true);
        if (account == null) {
            return false;
        }
        if (!isLocalUnlockedUser(accounts.userId)) {
            Log.w(TAG, "Account " + account + " cannot be added - user " + accounts.userId + " is locked. callingUid=" + callingUid);
            return false;
        }
        synchronized (accounts.cacheLock) {
            SQLiteDatabase db = accounts.openHelper.getWritableDatabaseUserIsUnlocked();
            db.beginTransaction();
            try {
                long numMatches = DatabaseUtils.longForQuery(db, "select count(*) from ceDb.accounts WHERE name=? AND type=?", new String[]{account.name, account.type});
                if (numMatches > 0) {
                    Log.w(TAG, "insertAccountIntoDatabase: " + account + ", skipping since the account already exists");
                    return false;
                }
                ContentValues values = new ContentValues();
                values.put(ACCOUNTS_NAME, account.name);
                values.put(DatabaseHelper.SoundModelContract.KEY_TYPE, account.type);
                values.put(ACCOUNTS_PASSWORD, password);
                long accountId = db.insert(CE_TABLE_ACCOUNTS, ACCOUNTS_NAME, values);
                if (accountId < 0) {
                    Log.w(TAG, "insertAccountIntoDatabase: " + account + ", skipping the DB insert failed");
                    return false;
                }
                ContentValues values2 = new ContentValues();
                values2.put("_id", Long.valueOf(accountId));
                values2.put(ACCOUNTS_NAME, account.name);
                values2.put(DatabaseHelper.SoundModelContract.KEY_TYPE, account.type);
                values2.put(ACCOUNTS_LAST_AUTHENTICATE_TIME_EPOCH_MILLIS, Long.valueOf(System.currentTimeMillis()));
                if (db.insert(TABLE_ACCOUNTS, ACCOUNTS_NAME, values2) < 0) {
                    Log.w(TAG, "insertAccountIntoDatabase: " + account + ", skipping the DB insert failed");
                    return false;
                }
                if (extras != null) {
                    for (String key : extras.keySet()) {
                        String value = extras.getString(key);
                        if (insertExtraLocked(db, accountId, key, value) < 0) {
                            Log.w(TAG, "insertAccountIntoDatabase: " + account + ", skipping since insertExtra failed for key " + key);
                            return false;
                        }
                    }
                }
                db.setTransactionSuccessful();
                logRecord(db, DebugDbHelper.ACTION_ACCOUNT_ADD, TABLE_ACCOUNTS, accountId, accounts, callingUid);
                insertAccountIntoCacheLocked(accounts, account);
                db.endTransaction();
                sendAccountsChangedBroadcast(accounts.userId);
                if (!getUserManager().getUserInfo(accounts.userId).canHaveProfile()) {
                    return true;
                }
                addAccountToLinkedRestrictedUsers(account, accounts.userId);
                return true;
            } finally {
                db.endTransaction();
            }
        }
    }

    private boolean isLocalUnlockedUser(int userId) {
        boolean z;
        synchronized (this.mUsers) {
            z = this.mLocalUnlockedUsers.get(userId);
        }
        return z;
    }

    private void addAccountToLinkedRestrictedUsers(Account account, int parentUserId) {
        List<UserInfo> users = getUserManager().getUsers();
        for (UserInfo user : users) {
            if (user.isRestricted() && parentUserId == user.restrictedProfileParentId) {
                addSharedAccountAsUser(account, user.id);
                if (isLocalUnlockedUser(user.id)) {
                    this.mMessageHandler.sendMessage(this.mMessageHandler.obtainMessage(4, parentUserId, user.id, account));
                }
            }
        }
    }

    private long insertExtraLocked(SQLiteDatabase db, long accountId, String key, String value) {
        ContentValues values = new ContentValues();
        values.put("key", key);
        values.put("accounts_id", Long.valueOf(accountId));
        values.put("value", value);
        return db.insert(CE_TABLE_EXTRAS, "key", values);
    }

    public void hasFeatures(IAccountManagerResponse response, Account account, String[] features, String opPackageName) {
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "hasFeatures: " + account + ", response " + response + ", features " + stringArrayToString(features) + ", caller's uid " + callingUid + ", pid " + Binder.getCallingPid());
        }
        if (response == null) {
            throw new IllegalArgumentException("response is null");
        }
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        if (features == null) {
            throw new IllegalArgumentException("features is null");
        }
        int userId = UserHandle.getCallingUserId();
        checkReadAccountsPermitted(callingUid, account.type, userId, opPackageName);
        long identityToken = clearCallingIdentity();
        try {
            UserAccounts accounts = getUserAccounts(userId);
            new TestFeaturesSession(accounts, response, account, features).bind();
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    private class TestFeaturesSession extends Session {
        private final Account mAccount;
        private final String[] mFeatures;

        public TestFeaturesSession(UserAccounts accounts, IAccountManagerResponse response, Account account, String[] features) {
            super(AccountManagerService.this, accounts, response, account.type, false, true, account.name, false);
            this.mFeatures = features;
            this.mAccount = account;
        }

        @Override
        public void run() throws RemoteException {
            try {
                this.mAuthenticator.hasFeatures(this, this.mAccount, this.mFeatures);
            } catch (RemoteException e) {
                onError(1, "remote exception");
            }
        }

        @Override
        public void onResult(Bundle result) {
            Bundle.setDefusable(result, true);
            IAccountManagerResponse response = getResponseAndClose();
            if (response == null) {
                return;
            }
            try {
                if (result == null) {
                    response.onError(5, "null bundle");
                    return;
                }
                if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                    Log.v(AccountManagerService.TAG, getClass().getSimpleName() + " calling onResult() on response " + response);
                }
                Bundle newResult = new Bundle();
                newResult.putBoolean("booleanResult", result.getBoolean("booleanResult", false));
                response.onResult(newResult);
            } catch (RemoteException e) {
                if (!Log.isLoggable(AccountManagerService.TAG, 2)) {
                    return;
                }
                Log.v(AccountManagerService.TAG, "failure while notifying response", e);
            }
        }

        @Override
        protected String toDebugString(long now) {
            return super.toDebugString(now) + ", hasFeatures, " + this.mAccount + ", " + (this.mFeatures != null ? TextUtils.join(",", this.mFeatures) : null);
        }
    }

    public void renameAccount(IAccountManagerResponse response, Account accountToRename, String newName) {
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "renameAccount: " + accountToRename + " -> " + newName + ", caller's uid " + callingUid + ", pid " + Binder.getCallingPid());
        }
        if (accountToRename == null) {
            throw new IllegalArgumentException("account is null");
        }
        int userId = UserHandle.getCallingUserId();
        if (!isAccountManagedByCaller(accountToRename.type, callingUid, userId)) {
            String msg = String.format("uid %s cannot rename accounts of type: %s", Integer.valueOf(callingUid), accountToRename.type);
            throw new SecurityException(msg);
        }
        long identityToken = clearCallingIdentity();
        try {
            UserAccounts accounts = getUserAccounts(userId);
            Account resultingAccount = renameAccountInternal(accounts, accountToRename, newName);
            Bundle result = new Bundle();
            result.putString("authAccount", resultingAccount.name);
            result.putString("accountType", resultingAccount.type);
            try {
                response.onResult(result);
            } catch (RemoteException e) {
                Log.w(TAG, e.getMessage());
            }
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    private Account renameAccountInternal(UserAccounts accounts, Account accountToRename, String newName) {
        Account resultAccount = null;
        cancelNotification(getSigninRequiredNotificationId(accounts, accountToRename).intValue(), new UserHandle(accounts.userId));
        synchronized (accounts.credentialsPermissionNotificationIds) {
            for (Pair<Pair<Account, String>, Integer> pair : accounts.credentialsPermissionNotificationIds.keySet()) {
                if (accountToRename.equals(((Pair) pair.first).first)) {
                    int id = ((Integer) accounts.credentialsPermissionNotificationIds.get(pair)).intValue();
                    cancelNotification(id, new UserHandle(accounts.userId));
                }
            }
        }
        synchronized (accounts.cacheLock) {
            SQLiteDatabase db = accounts.openHelper.getWritableDatabaseUserIsUnlocked();
            db.beginTransaction();
            boolean isSuccessful = false;
            Account renamedAccount = new Account(newName, accountToRename.type);
            try {
                long accountId = getAccountIdLocked(db, accountToRename);
                if (accountId >= 0) {
                    ContentValues values = new ContentValues();
                    values.put(ACCOUNTS_NAME, newName);
                    String[] argsAccountId = {String.valueOf(accountId)};
                    db.update(CE_TABLE_ACCOUNTS, values, "_id=?", argsAccountId);
                    values.put(ACCOUNTS_PREVIOUS_NAME, accountToRename.name);
                    db.update(TABLE_ACCOUNTS, values, "_id=?", argsAccountId);
                    db.setTransactionSuccessful();
                    isSuccessful = true;
                    logRecord(db, DebugDbHelper.ACTION_ACCOUNT_RENAME, TABLE_ACCOUNTS, accountId, accounts);
                }
                db.endTransaction();
                if (isSuccessful) {
                    insertAccountIntoCacheLocked(accounts, renamedAccount);
                    HashMap<String, String> tmpData = (HashMap) accounts.userDataCache.get(accountToRename);
                    HashMap<String, String> tmpTokens = (HashMap) accounts.authTokenCache.get(accountToRename);
                    removeAccountFromCacheLocked(accounts, accountToRename);
                    accounts.userDataCache.put(renamedAccount, tmpData);
                    accounts.authTokenCache.put(renamedAccount, tmpTokens);
                    accounts.previousNameCache.put(renamedAccount, new AtomicReference(accountToRename.name));
                    resultAccount = renamedAccount;
                    int parentUserId = accounts.userId;
                    if (canHaveProfile(parentUserId)) {
                        List<UserInfo> users = getUserManager().getUsers(true);
                        for (UserInfo user : users) {
                            if (user.isRestricted() && user.restrictedProfileParentId == parentUserId) {
                                renameSharedAccountAsUser(accountToRename, newName, user.id);
                            }
                        }
                    }
                    sendAccountsChangedBroadcast(accounts.userId);
                }
            } catch (Throwable th) {
                db.endTransaction();
                if (isSuccessful) {
                    insertAccountIntoCacheLocked(accounts, renamedAccount);
                    HashMap<String, String> tmpData2 = (HashMap) accounts.userDataCache.get(accountToRename);
                    HashMap<String, String> tmpTokens2 = (HashMap) accounts.authTokenCache.get(accountToRename);
                    removeAccountFromCacheLocked(accounts, accountToRename);
                    accounts.userDataCache.put(renamedAccount, tmpData2);
                    accounts.authTokenCache.put(renamedAccount, tmpTokens2);
                    accounts.previousNameCache.put(renamedAccount, new AtomicReference(accountToRename.name));
                    int parentUserId2 = accounts.userId;
                    if (canHaveProfile(parentUserId2)) {
                        List<UserInfo> users2 = getUserManager().getUsers(true);
                        for (UserInfo user2 : users2) {
                            if (user2.isRestricted() && user2.restrictedProfileParentId == parentUserId2) {
                                renameSharedAccountAsUser(accountToRename, newName, user2.id);
                            }
                        }
                    }
                    sendAccountsChangedBroadcast(accounts.userId);
                }
                throw th;
            }
        }
        return resultAccount;
    }

    private boolean canHaveProfile(int parentUserId) {
        UserInfo userInfo = getUserManager().getUserInfo(parentUserId);
        if (userInfo != null) {
            return userInfo.canHaveProfile();
        }
        return false;
    }

    public void removeAccount(IAccountManagerResponse response, Account account, boolean expectActivityLaunch) {
        removeAccountAsUser(response, account, expectActivityLaunch, UserHandle.getCallingUserId());
    }

    public void removeAccountAsUser(IAccountManagerResponse response, Account account, boolean expectActivityLaunch, int userId) {
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "removeAccount: " + account + ", response " + response + ", caller's uid " + callingUid + ", pid " + Binder.getCallingPid() + ", for user id " + userId);
        }
        if (response == null) {
            throw new IllegalArgumentException("response is null");
        }
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        if (isCrossUser(callingUid, userId)) {
            throw new SecurityException(String.format("User %s tying remove account for %s", Integer.valueOf(UserHandle.getCallingUserId()), Integer.valueOf(userId)));
        }
        UserHandle user = UserHandle.of(userId);
        if (!isAccountManagedByCaller(account.type, callingUid, user.getIdentifier()) && !isSystemUid(callingUid)) {
            String msg = String.format("uid %s cannot remove accounts of type: %s", Integer.valueOf(callingUid), account.type);
            throw new SecurityException(msg);
        }
        if (!canUserModifyAccounts(userId, callingUid)) {
            try {
                response.onError(100, "User cannot modify accounts");
                return;
            } catch (RemoteException e) {
                return;
            }
        }
        if (!canUserModifyAccountsForType(userId, account.type, callingUid)) {
            try {
                response.onError(101, "User cannot modify accounts of this type (policy).");
                return;
            } catch (RemoteException e2) {
                return;
            }
        }
        long identityToken = clearCallingIdentity();
        UserAccounts accounts = getUserAccounts(userId);
        cancelNotification(getSigninRequiredNotificationId(accounts, account).intValue(), user);
        synchronized (accounts.credentialsPermissionNotificationIds) {
            for (Pair<Pair<Account, String>, Integer> pair : accounts.credentialsPermissionNotificationIds.keySet()) {
                if (account.equals(((Pair) pair.first).first)) {
                    int id = ((Integer) accounts.credentialsPermissionNotificationIds.get(pair)).intValue();
                    cancelNotification(id, user);
                }
            }
        }
        logRecord(accounts, DebugDbHelper.ACTION_CALLED_ACCOUNT_REMOVE, TABLE_ACCOUNTS);
        try {
            new RemoveAccountSession(accounts, response, account, expectActivityLaunch).bind();
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public boolean removeAccountExplicitly(Account account) {
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "removeAccountExplicitly: " + account + ", caller's uid " + callingUid + ", pid " + Binder.getCallingPid());
        }
        int userId = Binder.getCallingUserHandle().getIdentifier();
        if (account == null) {
            Log.e(TAG, "account is null");
            return false;
        }
        if (!isAccountManagedByCaller(account.type, callingUid, userId)) {
            String msg = String.format("uid %s cannot explicitly add accounts of type: %s", Integer.valueOf(callingUid), account.type);
            throw new SecurityException(msg);
        }
        UserAccounts accounts = getUserAccountsForCaller();
        logRecord(accounts, DebugDbHelper.ACTION_CALLED_ACCOUNT_REMOVE, TABLE_ACCOUNTS);
        long identityToken = clearCallingIdentity();
        try {
            return removeAccountInternal(accounts, account, callingUid);
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    private class RemoveAccountSession extends Session {
        final Account mAccount;

        public RemoveAccountSession(UserAccounts accounts, IAccountManagerResponse response, Account account, boolean expectActivityLaunch) {
            super(AccountManagerService.this, accounts, response, account.type, expectActivityLaunch, true, account.name, false);
            this.mAccount = account;
        }

        @Override
        protected String toDebugString(long now) {
            return super.toDebugString(now) + ", removeAccount, account " + this.mAccount;
        }

        @Override
        public void run() throws RemoteException {
            this.mAuthenticator.getAccountRemovalAllowed(this, this.mAccount);
        }

        @Override
        public void onResult(Bundle result) {
            Bundle.setDefusable(result, true);
            if (result != null && result.containsKey("booleanResult") && !result.containsKey("intent")) {
                boolean removalAllowed = result.getBoolean("booleanResult");
                if (removalAllowed) {
                    AccountManagerService.this.removeAccountInternal(this.mAccounts, this.mAccount, getCallingUid());
                }
                IAccountManagerResponse response = getResponseAndClose();
                if (response != null) {
                    if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                        Log.v(AccountManagerService.TAG, getClass().getSimpleName() + " calling onResult() on response " + response);
                    }
                    Bundle result2 = new Bundle();
                    result2.putBoolean("booleanResult", removalAllowed);
                    try {
                        response.onResult(result2);
                    } catch (RemoteException e) {
                    }
                }
            }
            super.onResult(result);
        }
    }

    protected void removeAccountInternal(Account account) {
        removeAccountInternal(getUserAccountsForCaller(), account, getCallingUid());
    }

    private boolean removeAccountInternal(UserAccounts accounts, Account account, int callingUid) {
        SQLiteDatabase db;
        int deleted;
        boolean userUnlocked = isLocalUnlockedUser(accounts.userId);
        if (!userUnlocked) {
            Slog.i(TAG, "Removing account " + account + " while user " + accounts.userId + " is still locked. CE data will be removed later");
        }
        synchronized (accounts.cacheLock) {
            if (userUnlocked) {
                db = accounts.openHelper.getWritableDatabaseUserIsUnlocked();
            } else {
                db = accounts.openHelper.getWritableDatabase();
            }
            long accountId = getAccountIdLocked(db, account);
            db.beginTransaction();
            try {
                deleted = db.delete(TABLE_ACCOUNTS, "name=? AND type=?", new String[]{account.name, account.type});
                if (userUnlocked) {
                    deleted = db.delete(CE_TABLE_ACCOUNTS, "name=? AND type=?", new String[]{account.name, account.type});
                }
                db.setTransactionSuccessful();
                db.endTransaction();
                removeAccountFromCacheLocked(accounts, account);
                sendAccountsChangedBroadcast(accounts.userId);
                String action = userUnlocked ? DebugDbHelper.ACTION_ACCOUNT_REMOVE : DebugDbHelper.ACTION_ACCOUNT_REMOVE_DE;
                logRecord(db, action, TABLE_ACCOUNTS, accountId, accounts);
            } catch (Throwable th) {
                db.endTransaction();
                throw th;
            }
        }
        long id = Binder.clearCallingIdentity();
        try {
            int parentUserId = accounts.userId;
            if (canHaveProfile(parentUserId)) {
                List<UserInfo> users = getUserManager().getUsers(true);
                for (UserInfo user : users) {
                    if (user.isRestricted() && parentUserId == user.restrictedProfileParentId) {
                        removeSharedAccountAsUser(account, user.id, callingUid);
                    }
                }
            }
            return deleted > 0;
        } finally {
            Binder.restoreCallingIdentity(id);
        }
    }

    public void invalidateAuthToken(String accountType, String authToken) {
        int callerUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "invalidateAuthToken: accountType " + accountType + ", caller's uid " + callerUid + ", pid " + Binder.getCallingPid());
        }
        if (accountType == null) {
            throw new IllegalArgumentException("accountType is null");
        }
        if (authToken == null) {
            throw new IllegalArgumentException("authToken is null");
        }
        int userId = UserHandle.getCallingUserId();
        long identityToken = clearCallingIdentity();
        try {
            UserAccounts accounts = getUserAccounts(userId);
            synchronized (accounts.cacheLock) {
                SQLiteDatabase db = accounts.openHelper.getWritableDatabaseUserIsUnlocked();
                db.beginTransaction();
                try {
                    invalidateAuthTokenLocked(accounts, db, accountType, authToken);
                    invalidateCustomTokenLocked(accounts, accountType, authToken);
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    private void invalidateCustomTokenLocked(UserAccounts accounts, String accountType, String authToken) {
        if (authToken == null || accountType == null) {
            return;
        }
        accounts.accountTokenCaches.remove(accountType, authToken);
    }

    private void invalidateAuthTokenLocked(UserAccounts accounts, SQLiteDatabase db, String accountType, String authToken) {
        if (authToken == null || accountType == null) {
            return;
        }
        Cursor cursor = db.rawQuery("SELECT ceDb.authtokens._id, ceDb.accounts.name, ceDb.authtokens.type FROM ceDb.accounts JOIN ceDb.authtokens ON ceDb.accounts._id = ceDb.authtokens.accounts_id WHERE ceDb.authtokens.authtoken = ? AND ceDb.accounts.type = ?", new String[]{authToken, accountType});
        while (cursor.moveToNext()) {
            try {
                long authTokenId = cursor.getLong(0);
                String accountName = cursor.getString(1);
                String authTokenType = cursor.getString(2);
                db.delete(CE_TABLE_AUTHTOKENS, "_id=" + authTokenId, null);
                writeAuthTokenIntoCacheLocked(accounts, db, new Account(accountName, accountType), authTokenType, null);
            } finally {
                cursor.close();
            }
        }
    }

    private void saveCachedToken(UserAccounts accounts, Account account, String callerPkg, byte[] callerSigDigest, String tokenType, String token, long expiryMillis) {
        if (account == null || tokenType == null || callerPkg == null || callerSigDigest == null) {
            return;
        }
        cancelNotification(getSigninRequiredNotificationId(accounts, account).intValue(), UserHandle.of(accounts.userId));
        synchronized (accounts.cacheLock) {
            accounts.accountTokenCaches.put(account, token, tokenType, callerPkg, callerSigDigest, expiryMillis);
        }
    }

    private boolean saveAuthTokenToDatabase(UserAccounts accounts, Account account, String type, String authToken) {
        if (account == null || type == null) {
            return false;
        }
        cancelNotification(getSigninRequiredNotificationId(accounts, account).intValue(), UserHandle.of(accounts.userId));
        synchronized (accounts.cacheLock) {
            SQLiteDatabase db = accounts.openHelper.getWritableDatabaseUserIsUnlocked();
            db.beginTransaction();
            try {
                long accountId = getAccountIdLocked(db, account);
                if (accountId < 0) {
                    return false;
                }
                db.delete(CE_TABLE_AUTHTOKENS, "accounts_id=" + accountId + " AND " + DatabaseHelper.SoundModelContract.KEY_TYPE + "=?", new String[]{type});
                ContentValues values = new ContentValues();
                values.put("accounts_id", Long.valueOf(accountId));
                values.put(DatabaseHelper.SoundModelContract.KEY_TYPE, type);
                values.put(AUTHTOKENS_AUTHTOKEN, authToken);
                if (db.insert(CE_TABLE_AUTHTOKENS, AUTHTOKENS_AUTHTOKEN, values) < 0) {
                    return false;
                }
                db.setTransactionSuccessful();
                writeAuthTokenIntoCacheLocked(accounts, db, account, type, authToken);
                return true;
            } finally {
                db.endTransaction();
            }
        }
    }

    public String peekAuthToken(Account account, String authTokenType) {
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "peekAuthToken: " + account + ", authTokenType " + authTokenType + ", caller's uid " + callingUid + ", pid " + Binder.getCallingPid());
        }
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        if (authTokenType == null) {
            throw new IllegalArgumentException("authTokenType is null");
        }
        int userId = UserHandle.getCallingUserId();
        if (!isAccountManagedByCaller(account.type, callingUid, userId)) {
            String msg = String.format("uid %s cannot peek the authtokens associated with accounts of type: %s", Integer.valueOf(callingUid), account.type);
            throw new SecurityException(msg);
        }
        if (!isLocalUnlockedUser(userId)) {
            Log.w(TAG, "Authtoken not available - user " + userId + " data is locked. callingUid " + callingUid);
            return null;
        }
        long identityToken = clearCallingIdentity();
        try {
            UserAccounts accounts = getUserAccounts(userId);
            return readAuthTokenInternal(accounts, account, authTokenType);
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public void setAuthToken(Account account, String authTokenType, String authToken) {
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "setAuthToken: " + account + ", authTokenType " + authTokenType + ", caller's uid " + callingUid + ", pid " + Binder.getCallingPid());
        }
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        if (authTokenType == null) {
            throw new IllegalArgumentException("authTokenType is null");
        }
        int userId = UserHandle.getCallingUserId();
        if (!isAccountManagedByCaller(account.type, callingUid, userId)) {
            String msg = String.format("uid %s cannot set auth tokens associated with accounts of type: %s", Integer.valueOf(callingUid), account.type);
            throw new SecurityException(msg);
        }
        long identityToken = clearCallingIdentity();
        try {
            UserAccounts accounts = getUserAccounts(userId);
            saveAuthTokenToDatabase(accounts, account, authTokenType, authToken);
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public void setPassword(Account account, String password) {
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "setAuthToken: " + account + ", caller's uid " + callingUid + ", pid " + Binder.getCallingPid());
        }
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        int userId = UserHandle.getCallingUserId();
        if (!isAccountManagedByCaller(account.type, callingUid, userId)) {
            String msg = String.format("uid %s cannot set secrets for accounts of type: %s", Integer.valueOf(callingUid), account.type);
            throw new SecurityException(msg);
        }
        long identityToken = clearCallingIdentity();
        try {
            UserAccounts accounts = getUserAccounts(userId);
            setPasswordInternal(accounts, account, password, callingUid);
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    private void setPasswordInternal(UserAccounts accounts, Account account, String password, int callingUid) {
        String action;
        if (account == null) {
            return;
        }
        synchronized (accounts.cacheLock) {
            SQLiteDatabase db = accounts.openHelper.getWritableDatabaseUserIsUnlocked();
            db.beginTransaction();
            try {
                ContentValues values = new ContentValues();
                values.put(ACCOUNTS_PASSWORD, password);
                long accountId = getAccountIdLocked(db, account);
                if (accountId >= 0) {
                    String[] argsAccountId = {String.valueOf(accountId)};
                    db.update(CE_TABLE_ACCOUNTS, values, "_id=?", argsAccountId);
                    db.delete(CE_TABLE_AUTHTOKENS, "accounts_id=?", argsAccountId);
                    accounts.authTokenCache.remove(account);
                    accounts.accountTokenCaches.remove(account);
                    db.setTransactionSuccessful();
                    if (password == null || password.length() == 0) {
                        action = DebugDbHelper.ACTION_CLEAR_PASSWORD;
                    } else {
                        action = DebugDbHelper.ACTION_SET_PASSWORD;
                    }
                    logRecord(db, action, TABLE_ACCOUNTS, accountId, accounts, callingUid);
                }
                db.endTransaction();
                sendAccountsChangedBroadcast(accounts.userId);
            } catch (Throwable th) {
                db.endTransaction();
                throw th;
            }
        }
    }

    private void sendAccountsChangedBroadcast(int userId) {
        Log.i(TAG, "the accounts changed, sending broadcast of " + ACCOUNTS_CHANGED_INTENT.getAction());
        this.mContext.sendBroadcastAsUser(ACCOUNTS_CHANGED_INTENT, new UserHandle(userId));
    }

    public void clearPassword(Account account) {
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "clearPassword: " + account + ", caller's uid " + callingUid + ", pid " + Binder.getCallingPid());
        }
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        int userId = UserHandle.getCallingUserId();
        if (!isAccountManagedByCaller(account.type, callingUid, userId)) {
            String msg = String.format("uid %s cannot clear passwords for accounts of type: %s", Integer.valueOf(callingUid), account.type);
            throw new SecurityException(msg);
        }
        long identityToken = clearCallingIdentity();
        try {
            UserAccounts accounts = getUserAccounts(userId);
            setPasswordInternal(accounts, account, null, callingUid);
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public void setUserData(Account account, String key, String value) {
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "setUserData: " + account + ", key " + key + ", caller's uid " + callingUid + ", pid " + Binder.getCallingPid());
        }
        if (key == null) {
            throw new IllegalArgumentException("key is null");
        }
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        int userId = UserHandle.getCallingUserId();
        if (!isAccountManagedByCaller(account.type, callingUid, userId)) {
            String msg = String.format("uid %s cannot set user data for accounts of type: %s", Integer.valueOf(callingUid), account.type);
            throw new SecurityException(msg);
        }
        long identityToken = clearCallingIdentity();
        try {
            UserAccounts accounts = getUserAccounts(userId);
            synchronized (accounts.cacheLock) {
                if (accountExistsCacheLocked(accounts, account)) {
                    setUserdataInternalLocked(accounts, account, key, value);
                }
            }
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    private boolean accountExistsCacheLocked(UserAccounts accounts, Account account) {
        if (accounts.accountCache.containsKey(account.type)) {
            for (Account acc : (Account[]) accounts.accountCache.get(account.type)) {
                if (acc.name.equals(account.name)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void setUserdataInternalLocked(UserAccounts accounts, Account account, String key, String value) {
        if (account == null || key == null) {
            return;
        }
        SQLiteDatabase db = accounts.openHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            long accountId = getAccountIdLocked(db, account);
            if (accountId < 0) {
                return;
            }
            long extrasId = getExtrasIdLocked(db, accountId, key);
            if (extrasId >= 0) {
                ContentValues values = new ContentValues();
                values.put("value", value);
                if (1 != db.update(TABLE_EXTRAS, values, "_id=" + extrasId, null)) {
                    return;
                }
            } else if (insertExtraLocked(db, accountId, key, value) < 0) {
                return;
            }
            writeUserDataIntoCacheLocked(accounts, db, account, key, value);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void onResult(IAccountManagerResponse response, Bundle result) {
        if (result == null) {
            Log.e(TAG, "the result is unexpectedly null", new Exception());
        }
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, getClass().getSimpleName() + " calling onResult() on response " + response);
        }
        try {
            response.onResult(result);
        } catch (RemoteException e) {
            if (!Log.isLoggable(TAG, 2)) {
                return;
            }
            Log.v(TAG, "failure while notifying response", e);
        }
    }

    public void getAuthTokenLabel(IAccountManagerResponse response, final String accountType, final String authTokenType) throws RemoteException {
        if (accountType == null) {
            throw new IllegalArgumentException("accountType is null");
        }
        if (authTokenType == null) {
            throw new IllegalArgumentException("authTokenType is null");
        }
        int callingUid = getCallingUid();
        clearCallingIdentity();
        if (callingUid != 1000) {
            throw new SecurityException("can only call from system");
        }
        int userId = UserHandle.getUserId(callingUid);
        long identityToken = clearCallingIdentity();
        try {
            UserAccounts accounts = getUserAccounts(userId);
            new Session(this, accounts, response, accountType, false, false, null, false) {
                @Override
                protected String toDebugString(long now) {
                    return super.toDebugString(now) + ", getAuthTokenLabel, " + accountType + ", authTokenType " + authTokenType;
                }

                @Override
                public void run() throws RemoteException {
                    this.mAuthenticator.getAuthTokenLabel(this, authTokenType);
                }

                @Override
                public void onResult(Bundle result) {
                    Bundle.setDefusable(result, true);
                    if (result != null) {
                        String label = result.getString("authTokenLabelKey");
                        Bundle bundle = new Bundle();
                        bundle.putString("authTokenLabelKey", label);
                        super.onResult(bundle);
                        return;
                    }
                    super.onResult(result);
                }
            }.bind();
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public void getAuthToken(IAccountManagerResponse response, final Account account, final String authTokenType, final boolean notifyOnAuthFailure, boolean expectActivityLaunch, final Bundle loginOptions) {
        String token;
        String authToken;
        Bundle.setDefusable(loginOptions, true);
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "getAuthToken: " + account + ", response " + response + ", authTokenType " + authTokenType + ", notifyOnAuthFailure " + notifyOnAuthFailure + ", expectActivityLaunch " + expectActivityLaunch + ", caller's uid " + Binder.getCallingUid() + ", pid " + Binder.getCallingPid());
        }
        if (response == null) {
            throw new IllegalArgumentException("response is null");
        }
        try {
            if (account == null) {
                Slog.w(TAG, "getAuthToken called with null account");
                response.onError(7, "account is null");
                return;
            }
            if (authTokenType == null) {
                Slog.w(TAG, "getAuthToken called with null authTokenType");
                response.onError(7, "authTokenType is null");
                return;
            }
            int userId = UserHandle.getCallingUserId();
            long ident = Binder.clearCallingIdentity();
            try {
                final UserAccounts accounts = getUserAccounts(userId);
                RegisteredServicesCache.ServiceInfo<AuthenticatorDescription> authenticatorInfo = this.mAuthenticatorCache.getServiceInfo(AuthenticatorDescription.newKey(account.type), accounts.userId);
                final boolean z = authenticatorInfo != null ? ((AuthenticatorDescription) authenticatorInfo.type).customTokens : false;
                final int callerUid = Binder.getCallingUid();
                final boolean zPermissionIsGranted = !z ? permissionIsGranted(account, authTokenType, callerUid, userId) : true;
                final String callerPkg = loginOptions.getString("androidPackageName");
                ident = Binder.clearCallingIdentity();
                try {
                    List<String> callerOwnedPackageNames = Arrays.asList(this.mPackageManager.getPackagesForUid(callerUid));
                    if (callerPkg == null || !callerOwnedPackageNames.contains(callerPkg)) {
                        String msg = String.format("Uid %s is attempting to illegally masquerade as package %s!", Integer.valueOf(callerUid), callerPkg);
                        throw new SecurityException(msg);
                    }
                    loginOptions.putInt("callerUid", callerUid);
                    loginOptions.putInt("callerPid", Binder.getCallingPid());
                    if (notifyOnAuthFailure) {
                        loginOptions.putBoolean("notifyOnAuthFailure", true);
                    }
                    long identityToken = clearCallingIdentity();
                    try {
                        final byte[] callerPkgSigDigest = calculatePackageSignatureDigest(callerPkg);
                        if (!z && zPermissionIsGranted && (authToken = readAuthTokenInternal(accounts, account, authTokenType)) != null) {
                            Bundle result = new Bundle();
                            result.putString(AUTHTOKENS_AUTHTOKEN, authToken);
                            result.putString("authAccount", account.name);
                            result.putString("accountType", account.type);
                            onResult(response, result);
                            return;
                        }
                        if (!z || (token = readCachedTokenInternal(accounts, account, authTokenType, callerPkg, callerPkgSigDigest)) == null) {
                            new Session(this, accounts, response, account.type, expectActivityLaunch, false, account.name, false) {
                                @Override
                                protected String toDebugString(long now) {
                                    if (loginOptions != null) {
                                        loginOptions.keySet();
                                    }
                                    return super.toDebugString(now) + ", getAuthToken, " + account + ", authTokenType " + authTokenType + ", loginOptions " + loginOptions + ", notifyOnAuthFailure " + notifyOnAuthFailure;
                                }

                                @Override
                                public void run() throws RemoteException {
                                    if (!zPermissionIsGranted) {
                                        this.mAuthenticator.getAuthTokenLabel(this, authTokenType);
                                    } else {
                                        this.mAuthenticator.getAuthToken(this, account, authTokenType, loginOptions);
                                    }
                                }

                                @Override
                                public void onResult(Bundle result2) {
                                    Bundle.setDefusable(result2, true);
                                    if (result2 != null) {
                                        if (result2.containsKey("authTokenLabelKey")) {
                                            Intent intent = this.newGrantCredentialsPermissionIntent(account, callerUid, new AccountAuthenticatorResponse((IAccountAuthenticatorResponse) this), authTokenType);
                                            Bundle bundle = new Bundle();
                                            bundle.putParcelable("intent", intent);
                                            onResult(bundle);
                                            return;
                                        }
                                        String authToken2 = result2.getString(AccountManagerService.AUTHTOKENS_AUTHTOKEN);
                                        if (authToken2 != null) {
                                            String name = result2.getString("authAccount");
                                            String type = result2.getString("accountType");
                                            if (TextUtils.isEmpty(type) || TextUtils.isEmpty(name)) {
                                                onError(5, "the type and name should not be empty");
                                                return;
                                            }
                                            Account resultAccount = new Account(name, type);
                                            if (!z) {
                                                this.saveAuthTokenToDatabase(this.mAccounts, resultAccount, authTokenType, authToken2);
                                            }
                                            long expiryMillis = result2.getLong("android.accounts.expiry", 0L);
                                            if (z && expiryMillis > System.currentTimeMillis()) {
                                                this.saveCachedToken(this.mAccounts, account, callerPkg, callerPkgSigDigest, authTokenType, authToken2, expiryMillis);
                                            }
                                        }
                                        Intent intent2 = (Intent) result2.getParcelable("intent");
                                        if (intent2 != null && notifyOnAuthFailure && !z) {
                                            checkKeyIntent(Binder.getCallingUid(), intent2);
                                            this.doNotification(this.mAccounts, account, result2.getString("authFailedMessage"), intent2, accounts.userId);
                                        }
                                    }
                                    super.onResult(result2);
                                }
                            }.bind();
                            return;
                        }
                        if (Log.isLoggable(TAG, 2)) {
                            Log.v(TAG, "getAuthToken: cache hit ofr custom token authenticator.");
                        }
                        Bundle result2 = new Bundle();
                        result2.putString(AUTHTOKENS_AUTHTOKEN, token);
                        result2.putString("authAccount", account.name);
                        result2.putString("accountType", account.type);
                        onResult(response, result2);
                    } finally {
                        restoreCallingIdentity(identityToken);
                    }
                } finally {
                }
            } finally {
            }
        } catch (RemoteException e) {
            Slog.w(TAG, "Failed to report error back to the client." + e);
        }
    }

    private byte[] calculatePackageSignatureDigest(String callerPkg) {
        MessageDigest digester;
        try {
            digester = MessageDigest.getInstance("SHA-256");
            PackageInfo pkgInfo = this.mPackageManager.getPackageInfo(callerPkg, 64);
            for (Signature sig : pkgInfo.signatures) {
                digester.update(sig.toByteArray());
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Could not find packageinfo for: " + callerPkg);
            digester = null;
        } catch (NoSuchAlgorithmException x) {
            Log.wtf(TAG, "SHA-256 should be available", x);
            digester = null;
        }
        if (digester == null) {
            return null;
        }
        return digester.digest();
    }

    private void createNoCredentialsPermissionNotification(Account account, Intent intent, int userId) {
        int uid = intent.getIntExtra("uid", -1);
        String authTokenType = intent.getStringExtra("authTokenType");
        String titleAndSubtitle = this.mContext.getString(R.string.font_family_menu_material, account.name);
        int index = titleAndSubtitle.indexOf(10);
        String title = titleAndSubtitle;
        String subtitle = "";
        if (index > 0) {
            title = titleAndSubtitle.substring(0, index);
            subtitle = titleAndSubtitle.substring(index + 1);
        }
        UserHandle user = new UserHandle(userId);
        Context contextForUser = getContextForUser(user);
        Notification n = new Notification.Builder(contextForUser).setSmallIcon(R.drawable.stat_sys_warning).setWhen(0L).setColor(contextForUser.getColor(R.color.system_accent3_600)).setContentTitle(title).setContentText(subtitle).setContentIntent(PendingIntent.getActivityAsUser(this.mContext, 0, intent, 268435456, null, user)).build();
        installNotification(getCredentialPermissionNotificationId(account, authTokenType, uid).intValue(), n, user);
    }

    private Intent newGrantCredentialsPermissionIntent(Account account, int uid, AccountAuthenticatorResponse response, String authTokenType) {
        Intent intent = new Intent(this.mContext, (Class<?>) GrantCredentialsPermissionActivity.class);
        intent.setFlags(268435456);
        intent.addCategory(String.valueOf(getCredentialPermissionNotificationId(account, authTokenType, uid)));
        intent.putExtra("account", account);
        intent.putExtra("authTokenType", authTokenType);
        intent.putExtra("response", response);
        intent.putExtra("uid", uid);
        return intent;
    }

    private Integer getCredentialPermissionNotificationId(Account account, String authTokenType, int uid) {
        Integer id;
        UserAccounts accounts = getUserAccounts(UserHandle.getUserId(uid));
        synchronized (accounts.credentialsPermissionNotificationIds) {
            Pair<Pair<Account, String>, Integer> key = new Pair<>(new Pair(account, authTokenType), Integer.valueOf(uid));
            id = (Integer) accounts.credentialsPermissionNotificationIds.get(key);
            if (id == null) {
                id = Integer.valueOf(this.mNotificationIds.incrementAndGet());
                accounts.credentialsPermissionNotificationIds.put(key, id);
            }
        }
        return id;
    }

    private Integer getSigninRequiredNotificationId(UserAccounts accounts, Account account) {
        Integer id;
        synchronized (accounts.signinRequiredNotificationIds) {
            id = (Integer) accounts.signinRequiredNotificationIds.get(account);
            if (id == null) {
                id = Integer.valueOf(this.mNotificationIds.incrementAndGet());
                accounts.signinRequiredNotificationIds.put(account, id);
            }
        }
        return id;
    }

    public void addAccount(IAccountManagerResponse response, final String accountType, final String authTokenType, final String[] requiredFeatures, boolean expectActivityLaunch, Bundle optionsIn) {
        Bundle.setDefusable(optionsIn, true);
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "addAccount: accountType " + accountType + ", response " + response + ", authTokenType " + authTokenType + ", requiredFeatures " + stringArrayToString(requiredFeatures) + ", expectActivityLaunch " + expectActivityLaunch + ", caller's uid " + Binder.getCallingUid() + ", pid " + Binder.getCallingPid());
        }
        if (response == null) {
            throw new IllegalArgumentException("response is null");
        }
        if (accountType == null) {
            throw new IllegalArgumentException("accountType is null");
        }
        int uid = Binder.getCallingUid();
        int userId = UserHandle.getUserId(uid);
        if (!canUserModifyAccounts(userId, uid)) {
            try {
                response.onError(100, "User is not allowed to add an account!");
            } catch (RemoteException e) {
            }
            showCantAddAccount(100, userId);
            return;
        }
        if (!canUserModifyAccountsForType(userId, accountType, uid)) {
            try {
                response.onError(101, "User cannot modify accounts of this type (policy).");
            } catch (RemoteException e2) {
            }
            showCantAddAccount(101, userId);
            return;
        }
        int pid = Binder.getCallingPid();
        final Bundle options = optionsIn == null ? new Bundle() : optionsIn;
        options.putInt("callerUid", uid);
        options.putInt("callerPid", pid);
        int usrId = UserHandle.getCallingUserId();
        long identityToken = clearCallingIdentity();
        try {
            UserAccounts accounts = getUserAccounts(usrId);
            logRecordWithUid(accounts, DebugDbHelper.ACTION_CALLED_ACCOUNT_ADD, TABLE_ACCOUNTS, uid);
            new Session(this, accounts, response, accountType, expectActivityLaunch, true, null, false, true) {
                @Override
                public void run() throws RemoteException {
                    this.mAuthenticator.addAccount(this, this.mAccountType, authTokenType, requiredFeatures, options);
                }

                @Override
                protected String toDebugString(long now) {
                    return super.toDebugString(now) + ", addAccount, accountType " + accountType + ", requiredFeatures " + (requiredFeatures != null ? TextUtils.join(",", requiredFeatures) : null);
                }
            }.bind();
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public void addAccountAsUser(IAccountManagerResponse response, final String accountType, final String authTokenType, final String[] requiredFeatures, boolean expectActivityLaunch, Bundle optionsIn, int userId) {
        Bundle.setDefusable(optionsIn, true);
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "addAccount: accountType " + accountType + ", response " + response + ", authTokenType " + authTokenType + ", requiredFeatures " + stringArrayToString(requiredFeatures) + ", expectActivityLaunch " + expectActivityLaunch + ", caller's uid " + Binder.getCallingUid() + ", pid " + Binder.getCallingPid() + ", for user id " + userId);
        }
        if (response == null) {
            throw new IllegalArgumentException("response is null");
        }
        if (accountType == null) {
            throw new IllegalArgumentException("accountType is null");
        }
        if (isCrossUser(callingUid, userId)) {
            throw new SecurityException(String.format("User %s trying to add account for %s", Integer.valueOf(UserHandle.getCallingUserId()), Integer.valueOf(userId)));
        }
        if (!canUserModifyAccounts(userId, callingUid)) {
            try {
                response.onError(100, "User is not allowed to add an account!");
            } catch (RemoteException e) {
            }
            showCantAddAccount(100, userId);
            return;
        }
        if (!canUserModifyAccountsForType(userId, accountType, callingUid)) {
            try {
                response.onError(101, "User cannot modify accounts of this type (policy).");
            } catch (RemoteException e2) {
            }
            showCantAddAccount(101, userId);
            return;
        }
        int pid = Binder.getCallingPid();
        int uid = Binder.getCallingUid();
        final Bundle options = optionsIn == null ? new Bundle() : optionsIn;
        options.putInt("callerUid", uid);
        options.putInt("callerPid", pid);
        long identityToken = clearCallingIdentity();
        try {
            UserAccounts accounts = getUserAccounts(userId);
            logRecordWithUid(accounts, DebugDbHelper.ACTION_CALLED_ACCOUNT_ADD, TABLE_ACCOUNTS, userId);
            new Session(this, accounts, response, accountType, expectActivityLaunch, true, null, false, true) {
                @Override
                public void run() throws RemoteException {
                    this.mAuthenticator.addAccount(this, this.mAccountType, authTokenType, requiredFeatures, options);
                }

                @Override
                protected String toDebugString(long now) {
                    return super.toDebugString(now) + ", addAccount, accountType " + accountType + ", requiredFeatures " + (requiredFeatures != null ? TextUtils.join(",", requiredFeatures) : null);
                }
            }.bind();
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public void startAddAccountSession(IAccountManagerResponse response, final String accountType, final String authTokenType, final String[] requiredFeatures, boolean expectActivityLaunch, Bundle optionsIn) {
        Bundle.setDefusable(optionsIn, true);
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "startAddAccountSession: accountType " + accountType + ", response " + response + ", authTokenType " + authTokenType + ", requiredFeatures " + stringArrayToString(requiredFeatures) + ", expectActivityLaunch " + expectActivityLaunch + ", caller's uid " + Binder.getCallingUid() + ", pid " + Binder.getCallingPid());
        }
        if (response == null) {
            throw new IllegalArgumentException("response is null");
        }
        if (accountType == null) {
            throw new IllegalArgumentException("accountType is null");
        }
        int uid = Binder.getCallingUid();
        if (!isSystemUid(uid)) {
            String msg = String.format("uid %s cannot stat add account session.", Integer.valueOf(uid));
            throw new SecurityException(msg);
        }
        int userId = UserHandle.getUserId(uid);
        if (!canUserModifyAccounts(userId, uid)) {
            try {
                response.onError(100, "User is not allowed to add an account!");
            } catch (RemoteException e) {
            }
            showCantAddAccount(100, userId);
            return;
        }
        if (!canUserModifyAccountsForType(userId, accountType, uid)) {
            try {
                response.onError(101, "User cannot modify accounts of this type (policy).");
            } catch (RemoteException e2) {
            }
            showCantAddAccount(101, userId);
            return;
        }
        int pid = Binder.getCallingPid();
        final Bundle options = optionsIn == null ? new Bundle() : optionsIn;
        options.putInt("callerUid", uid);
        options.putInt("callerPid", pid);
        String callerPkg = optionsIn.getString("androidPackageName");
        boolean isPasswordForwardingAllowed = isPermitted(callerPkg, uid, "android.permission.GET_PASSWORD");
        long identityToken = clearCallingIdentity();
        try {
            UserAccounts accounts = getUserAccounts(userId);
            logRecordWithUid(accounts, DebugDbHelper.ACTION_CALLED_START_ACCOUNT_ADD, TABLE_ACCOUNTS, uid);
            new StartAccountSession(this, accounts, response, accountType, expectActivityLaunch, null, false, true, isPasswordForwardingAllowed) {
                @Override
                public void run() throws RemoteException {
                    this.mAuthenticator.startAddAccountSession(this, this.mAccountType, authTokenType, requiredFeatures, options);
                }

                @Override
                protected String toDebugString(long now) {
                    String requiredFeaturesStr = TextUtils.join(",", requiredFeatures);
                    StringBuilder sbAppend = new StringBuilder().append(super.toDebugString(now)).append(", startAddAccountSession").append(", accountType ").append(accountType).append(", requiredFeatures ");
                    if (requiredFeatures == null) {
                        requiredFeaturesStr = null;
                    }
                    return sbAppend.append(requiredFeaturesStr).toString();
                }
            }.bind();
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    private abstract class StartAccountSession extends Session {
        private final boolean mIsPasswordForwardingAllowed;

        public StartAccountSession(UserAccounts accounts, IAccountManagerResponse response, String accountType, boolean expectActivityLaunch, String accountName, boolean authDetailsRequired, boolean updateLastAuthenticationTime, boolean isPasswordForwardingAllowed) {
            super(accounts, response, accountType, expectActivityLaunch, true, accountName, authDetailsRequired, updateLastAuthenticationTime);
            this.mIsPasswordForwardingAllowed = isPasswordForwardingAllowed;
        }

        @Override
        public void onResult(Bundle result) {
            IAccountManagerResponse response;
            Bundle.setDefusable(result, true);
            this.mNumResults++;
            Intent intent = null;
            if (result != null && (intent = (Intent) result.getParcelable("intent")) != null) {
                checkKeyIntent(Binder.getCallingUid(), intent);
            }
            if (this.mExpectActivityLaunch && result != null && result.containsKey("intent")) {
                response = this.mResponse;
            } else {
                response = getResponseAndClose();
            }
            if (response == null) {
                return;
            }
            if (result == null) {
                if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                    Log.v(AccountManagerService.TAG, getClass().getSimpleName() + " calling onError() on response " + response);
                }
                AccountManagerService.this.sendErrorResponse(response, 5, "null bundle returned");
                return;
            }
            if (result.getInt("errorCode", -1) > 0 && intent == null) {
                AccountManagerService.this.sendErrorResponse(response, result.getInt("errorCode"), result.getString("errorMessage"));
                return;
            }
            if (!this.mIsPasswordForwardingAllowed) {
                result.remove(AccountManagerService.ACCOUNTS_PASSWORD);
            }
            result.remove(AccountManagerService.AUTHTOKENS_AUTHTOKEN);
            if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                Log.v(AccountManagerService.TAG, getClass().getSimpleName() + " calling onResult() on response " + response);
            }
            Bundle sessionBundle = result.getBundle("accountSessionBundle");
            if (sessionBundle != null) {
                String accountType = sessionBundle.getString("accountType");
                if (TextUtils.isEmpty(accountType) || !this.mAccountType.equalsIgnoreCase(accountType)) {
                    Log.w(AccountManagerService.TAG, "Account type in session bundle doesn't match request.");
                }
                sessionBundle.putString("accountType", this.mAccountType);
                try {
                    CryptoHelper cryptoHelper = CryptoHelper.getInstance();
                    Bundle encryptedBundle = cryptoHelper.encryptBundle(sessionBundle);
                    result.putBundle("accountSessionBundle", encryptedBundle);
                } catch (GeneralSecurityException e) {
                    if (Log.isLoggable(AccountManagerService.TAG, 3)) {
                        Log.v(AccountManagerService.TAG, "Failed to encrypt session bundle!", e);
                    }
                    AccountManagerService.this.sendErrorResponse(response, 5, "failed to encrypt session bundle");
                    return;
                }
            }
            AccountManagerService.this.sendResponse(response, result);
        }
    }

    public void finishSessionAsUser(IAccountManagerResponse response, Bundle sessionBundle, boolean expectActivityLaunch, Bundle appInfo, int userId) {
        Bundle.setDefusable(sessionBundle, true);
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "finishSession: response " + response + ", expectActivityLaunch " + expectActivityLaunch + ", caller's uid " + callingUid + ", caller's user id " + UserHandle.getCallingUserId() + ", pid " + Binder.getCallingPid() + ", for user id " + userId);
        }
        if (response == null) {
            throw new IllegalArgumentException("response is null");
        }
        if (sessionBundle == null || sessionBundle.size() == 0) {
            throw new IllegalArgumentException("sessionBundle is empty");
        }
        if (isCrossUser(callingUid, userId)) {
            throw new SecurityException(String.format("User %s trying to finish session for %s without cross user permission", Integer.valueOf(UserHandle.getCallingUserId()), Integer.valueOf(userId)));
        }
        if (!isSystemUid(callingUid)) {
            String msg = String.format("uid %s cannot finish session because it's not system uid.", Integer.valueOf(callingUid));
            throw new SecurityException(msg);
        }
        if (!canUserModifyAccounts(userId, callingUid)) {
            sendErrorResponse(response, 100, "User is not allowed to add an account!");
            showCantAddAccount(100, userId);
            return;
        }
        int pid = Binder.getCallingPid();
        try {
            CryptoHelper cryptoHelper = CryptoHelper.getInstance();
            final Bundle decryptedBundle = cryptoHelper.decryptBundle(sessionBundle);
            if (decryptedBundle == null) {
                sendErrorResponse(response, 8, "failed to decrypt session bundle");
                return;
            }
            final String accountType = decryptedBundle.getString("accountType");
            if (TextUtils.isEmpty(accountType)) {
                sendErrorResponse(response, 7, "accountType is empty");
                return;
            }
            if (appInfo != null) {
                decryptedBundle.putAll(appInfo);
            }
            decryptedBundle.putInt("callerUid", callingUid);
            decryptedBundle.putInt("callerPid", pid);
            if (!canUserModifyAccountsForType(userId, accountType, callingUid)) {
                sendErrorResponse(response, 101, "User cannot modify accounts of this type (policy).");
                showCantAddAccount(101, userId);
                return;
            }
            long identityToken = clearCallingIdentity();
            try {
                UserAccounts accounts = getUserAccounts(userId);
                logRecordWithUid(accounts, DebugDbHelper.ACTION_CALLED_ACCOUNT_SESSION_FINISH, TABLE_ACCOUNTS, callingUid);
                new Session(this, accounts, response, accountType, expectActivityLaunch, true, null, false, true) {
                    @Override
                    public void run() throws RemoteException {
                        this.mAuthenticator.finishSession(this, this.mAccountType, decryptedBundle);
                    }

                    @Override
                    protected String toDebugString(long now) {
                        return super.toDebugString(now) + ", finishSession, accountType " + accountType;
                    }
                }.bind();
            } finally {
                restoreCallingIdentity(identityToken);
            }
        } catch (GeneralSecurityException e) {
            if (Log.isLoggable(TAG, 3)) {
                Log.v(TAG, "Failed to decrypt session bundle!", e);
            }
            sendErrorResponse(response, 8, "failed to decrypt session bundle");
        }
    }

    private void showCantAddAccount(int errorCode, int userId) {
        Intent cantAddAccount = new Intent(this.mContext, (Class<?>) CantAddAccountActivity.class);
        cantAddAccount.putExtra("android.accounts.extra.ERROR_CODE", errorCode);
        cantAddAccount.addFlags(268435456);
        long identityToken = clearCallingIdentity();
        try {
            this.mContext.startActivityAsUser(cantAddAccount, new UserHandle(userId));
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public void confirmCredentialsAsUser(IAccountManagerResponse response, final Account account, final Bundle options, boolean expectActivityLaunch, int userId) {
        Bundle.setDefusable(options, true);
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "confirmCredentials: " + account + ", response " + response + ", expectActivityLaunch " + expectActivityLaunch + ", caller's uid " + callingUid + ", pid " + Binder.getCallingPid());
        }
        if (isCrossUser(callingUid, userId)) {
            throw new SecurityException(String.format("User %s trying to confirm account credentials for %s", Integer.valueOf(UserHandle.getCallingUserId()), Integer.valueOf(userId)));
        }
        if (response == null) {
            throw new IllegalArgumentException("response is null");
        }
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        long identityToken = clearCallingIdentity();
        try {
            UserAccounts accounts = getUserAccounts(userId);
            new Session(this, accounts, response, account.type, expectActivityLaunch, true, account.name, true, true) {
                @Override
                public void run() throws RemoteException {
                    this.mAuthenticator.confirmCredentials(this, account, options);
                }

                @Override
                protected String toDebugString(long now) {
                    return super.toDebugString(now) + ", confirmCredentials, " + account;
                }
            }.bind();
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public void updateCredentials(IAccountManagerResponse response, final Account account, final String authTokenType, boolean expectActivityLaunch, final Bundle loginOptions) {
        Bundle.setDefusable(loginOptions, true);
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "updateCredentials: " + account + ", response " + response + ", authTokenType " + authTokenType + ", expectActivityLaunch " + expectActivityLaunch + ", caller's uid " + Binder.getCallingUid() + ", pid " + Binder.getCallingPid());
        }
        if (response == null) {
            throw new IllegalArgumentException("response is null");
        }
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        int userId = UserHandle.getCallingUserId();
        long identityToken = clearCallingIdentity();
        try {
            UserAccounts accounts = getUserAccounts(userId);
            new Session(this, accounts, response, account.type, expectActivityLaunch, true, account.name, false, true) {
                @Override
                public void run() throws RemoteException {
                    this.mAuthenticator.updateCredentials(this, account, authTokenType, loginOptions);
                }

                @Override
                protected String toDebugString(long now) {
                    if (loginOptions != null) {
                        loginOptions.keySet();
                    }
                    return super.toDebugString(now) + ", updateCredentials, " + account + ", authTokenType " + authTokenType + ", loginOptions " + loginOptions;
                }
            }.bind();
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public void startUpdateCredentialsSession(IAccountManagerResponse response, final Account account, final String authTokenType, boolean expectActivityLaunch, final Bundle loginOptions) {
        Bundle.setDefusable(loginOptions, true);
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "startUpdateCredentialsSession: " + account + ", response " + response + ", authTokenType " + authTokenType + ", expectActivityLaunch " + expectActivityLaunch + ", caller's uid " + Binder.getCallingUid() + ", pid " + Binder.getCallingPid());
        }
        if (response == null) {
            throw new IllegalArgumentException("response is null");
        }
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        int uid = Binder.getCallingUid();
        if (!isSystemUid(uid)) {
            String msg = String.format("uid %s cannot start update credentials session.", Integer.valueOf(uid));
            throw new SecurityException(msg);
        }
        int userId = UserHandle.getCallingUserId();
        String callerPkg = loginOptions.getString("androidPackageName");
        boolean isPasswordForwardingAllowed = isPermitted(callerPkg, uid, "android.permission.GET_PASSWORD");
        long identityToken = clearCallingIdentity();
        try {
            UserAccounts accounts = getUserAccounts(userId);
            new StartAccountSession(this, accounts, response, account.type, expectActivityLaunch, account.name, false, true, isPasswordForwardingAllowed) {
                @Override
                public void run() throws RemoteException {
                    this.mAuthenticator.startUpdateCredentialsSession(this, account, authTokenType, loginOptions);
                }

                @Override
                protected String toDebugString(long now) {
                    if (loginOptions != null) {
                        loginOptions.keySet();
                    }
                    return super.toDebugString(now) + ", startUpdateCredentialsSession, " + account + ", authTokenType " + authTokenType + ", loginOptions " + loginOptions;
                }
            }.bind();
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public void isCredentialsUpdateSuggested(IAccountManagerResponse response, final Account account, final String statusToken) {
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "isCredentialsUpdateSuggested: " + account + ", response " + response + ", caller's uid " + Binder.getCallingUid() + ", pid " + Binder.getCallingPid());
        }
        if (response == null) {
            throw new IllegalArgumentException("response is null");
        }
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        if (TextUtils.isEmpty(statusToken)) {
            throw new IllegalArgumentException("status token is empty");
        }
        int uid = Binder.getCallingUid();
        if (!isSystemUid(uid)) {
            String msg = String.format("uid %s cannot stat add account session.", Integer.valueOf(uid));
            throw new SecurityException(msg);
        }
        int usrId = UserHandle.getCallingUserId();
        long identityToken = clearCallingIdentity();
        try {
            UserAccounts accounts = getUserAccounts(usrId);
            new Session(this, accounts, response, account.type, false, false, account.name, false) {
                @Override
                protected String toDebugString(long now) {
                    return super.toDebugString(now) + ", isCredentialsUpdateSuggested, " + account;
                }

                @Override
                public void run() throws RemoteException {
                    this.mAuthenticator.isCredentialsUpdateSuggested(this, account, statusToken);
                }

                @Override
                public void onResult(Bundle result) {
                    Bundle.setDefusable(result, true);
                    IAccountManagerResponse response2 = getResponseAndClose();
                    if (response2 == null) {
                        return;
                    }
                    if (result == null) {
                        this.sendErrorResponse(response2, 5, "null bundle");
                        return;
                    }
                    if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                        Log.v(AccountManagerService.TAG, getClass().getSimpleName() + " calling onResult() on response " + response2);
                    }
                    if (result.getInt("errorCode", -1) > 0) {
                        this.sendErrorResponse(response2, result.getInt("errorCode"), result.getString("errorMessage"));
                    } else {
                        if (!result.containsKey("booleanResult")) {
                            this.sendErrorResponse(response2, 5, "no result in response");
                            return;
                        }
                        Bundle newResult = new Bundle();
                        newResult.putBoolean("booleanResult", result.getBoolean("booleanResult", false));
                        this.sendResponse(response2, newResult);
                    }
                }
            }.bind();
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public void editProperties(IAccountManagerResponse response, final String accountType, boolean expectActivityLaunch) {
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "editProperties: accountType " + accountType + ", response " + response + ", expectActivityLaunch " + expectActivityLaunch + ", caller's uid " + callingUid + ", pid " + Binder.getCallingPid());
        }
        if (response == null) {
            throw new IllegalArgumentException("response is null");
        }
        if (accountType == null) {
            throw new IllegalArgumentException("accountType is null");
        }
        int userId = UserHandle.getCallingUserId();
        if (!isAccountManagedByCaller(accountType, callingUid, userId) && !isSystemUid(callingUid)) {
            String msg = String.format("uid %s cannot edit authenticator properites for account type: %s", Integer.valueOf(callingUid), accountType);
            throw new SecurityException(msg);
        }
        long identityToken = clearCallingIdentity();
        try {
            UserAccounts accounts = getUserAccounts(userId);
            new Session(this, accounts, response, accountType, expectActivityLaunch, true, null, false) {
                @Override
                public void run() throws RemoteException {
                    this.mAuthenticator.editProperties(this, this.mAccountType);
                }

                @Override
                protected String toDebugString(long now) {
                    return super.toDebugString(now) + ", editProperties, accountType " + accountType;
                }
            }.bind();
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public boolean someUserHasAccount(Account account) {
        if (!UserHandle.isSameApp(1000, Binder.getCallingUid())) {
            throw new SecurityException("Only system can check for accounts across users");
        }
        long token = Binder.clearCallingIdentity();
        try {
            AccountAndUser[] allAccounts = getAllAccounts();
            for (int i = allAccounts.length - 1; i >= 0; i--) {
                if (allAccounts[i].account.equals(account)) {
                    return true;
                }
            }
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private class GetAccountsByTypeAndFeatureSession extends Session {
        private volatile Account[] mAccountsOfType;
        private volatile ArrayList<Account> mAccountsWithFeatures;
        private final int mCallingUid;
        private volatile int mCurrentAccount;
        private final String[] mFeatures;

        public GetAccountsByTypeAndFeatureSession(UserAccounts accounts, IAccountManagerResponse response, String type, String[] features, int callingUid) {
            super(AccountManagerService.this, accounts, response, type, false, true, null, false);
            this.mAccountsOfType = null;
            this.mAccountsWithFeatures = null;
            this.mCurrentAccount = 0;
            this.mCallingUid = callingUid;
            this.mFeatures = features;
        }

        @Override
        public void run() throws RemoteException {
            synchronized (this.mAccounts.cacheLock) {
                this.mAccountsOfType = AccountManagerService.this.getAccountsFromCacheLocked(this.mAccounts, this.mAccountType, this.mCallingUid, null);
            }
            this.mAccountsWithFeatures = new ArrayList<>(this.mAccountsOfType.length);
            this.mCurrentAccount = 0;
            checkAccount();
        }

        public void checkAccount() {
            if (this.mCurrentAccount >= this.mAccountsOfType.length) {
                sendResult();
                return;
            }
            IAccountAuthenticator accountAuthenticator = this.mAuthenticator;
            if (accountAuthenticator == null) {
                if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                    Log.v(AccountManagerService.TAG, "checkAccount: aborting session since we are no longer connected to the authenticator, " + toDebugString());
                }
            } else {
                try {
                    accountAuthenticator.hasFeatures(this, this.mAccountsOfType[this.mCurrentAccount], this.mFeatures);
                } catch (RemoteException e) {
                    onError(1, "remote exception");
                }
            }
        }

        @Override
        public void onResult(Bundle result) {
            Bundle.setDefusable(result, true);
            this.mNumResults++;
            if (result == null) {
                onError(5, "null bundle");
                return;
            }
            if (result.getBoolean("booleanResult", false)) {
                this.mAccountsWithFeatures.add(this.mAccountsOfType[this.mCurrentAccount]);
            }
            this.mCurrentAccount++;
            checkAccount();
        }

        public void sendResult() {
            IAccountManagerResponse response = getResponseAndClose();
            if (response == null) {
                return;
            }
            try {
                Account[] accounts = new Account[this.mAccountsWithFeatures.size()];
                for (int i = 0; i < accounts.length; i++) {
                    accounts[i] = this.mAccountsWithFeatures.get(i);
                }
                if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                    Log.v(AccountManagerService.TAG, getClass().getSimpleName() + " calling onResult() on response " + response);
                }
                Bundle result = new Bundle();
                result.putParcelableArray(AccountManagerService.TABLE_ACCOUNTS, accounts);
                response.onResult(result);
            } catch (RemoteException e) {
                if (!Log.isLoggable(AccountManagerService.TAG, 2)) {
                    return;
                }
                Log.v(AccountManagerService.TAG, "failure while notifying response", e);
            }
        }

        @Override
        protected String toDebugString(long now) {
            return super.toDebugString(now) + ", getAccountsByTypeAndFeatures, " + (this.mFeatures != null ? TextUtils.join(",", this.mFeatures) : null);
        }
    }

    public Account[] getAccounts(int userId, String opPackageName) {
        int callingUid = Binder.getCallingUid();
        List<String> visibleAccountTypes = getTypesVisibleToCaller(callingUid, userId, opPackageName);
        if (visibleAccountTypes.isEmpty()) {
            return new Account[0];
        }
        long identityToken = clearCallingIdentity();
        try {
            UserAccounts accounts = getUserAccounts(userId);
            return getAccountsInternal(accounts, callingUid, null, visibleAccountTypes);
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public AccountAndUser[] getRunningAccounts() {
        try {
            int[] runningUserIds = ActivityManagerNative.getDefault().getRunningUserIds();
            return getAccounts(runningUserIds);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public AccountAndUser[] getAllAccounts() {
        List<UserInfo> users = getUserManager().getUsers(true);
        int[] userIds = new int[users.size()];
        for (int i = 0; i < userIds.length; i++) {
            userIds[i] = users.get(i).id;
        }
        return getAccounts(userIds);
    }

    private AccountAndUser[] getAccounts(int[] userIds) {
        ArrayList<AccountAndUser> runningAccounts = Lists.newArrayList();
        for (int userId : userIds) {
            UserAccounts userAccounts = getUserAccounts(userId);
            if (userAccounts != null) {
                synchronized (userAccounts.cacheLock) {
                    Account[] accounts = getAccountsFromCacheLocked(userAccounts, null, Binder.getCallingUid(), null);
                    for (Account account : accounts) {
                        runningAccounts.add(new AccountAndUser(account, userId));
                    }
                }
            }
        }
        AccountAndUser[] accountsArray = new AccountAndUser[runningAccounts.size()];
        return (AccountAndUser[]) runningAccounts.toArray(accountsArray);
    }

    public Account[] getAccountsAsUser(String type, int userId, String opPackageName) {
        return getAccountsAsUser(type, userId, null, -1, opPackageName);
    }

    private Account[] getAccountsAsUser(String type, int userId, String callingPackage, int packageUid, String opPackageName) {
        int callingUid = Binder.getCallingUid();
        if (userId != UserHandle.getCallingUserId() && callingUid != Process.myUid() && this.mContext.checkCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL") != 0) {
            throw new SecurityException("User " + UserHandle.getCallingUserId() + " trying to get account for " + userId);
        }
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "getAccounts: accountType " + type + ", caller's uid " + Binder.getCallingUid() + ", pid " + Binder.getCallingPid());
        }
        if (packageUid != -1 && UserHandle.isSameApp(callingUid, Process.myUid())) {
            callingUid = packageUid;
            opPackageName = callingPackage;
        }
        List<String> visibleAccountTypes = getTypesVisibleToCaller(callingUid, userId, opPackageName);
        if (visibleAccountTypes.isEmpty() || !(type == null || visibleAccountTypes.contains(type))) {
            return new Account[0];
        }
        if (visibleAccountTypes.contains(type)) {
            visibleAccountTypes = new ArrayList<>();
            visibleAccountTypes.add(type);
        }
        long identityToken = clearCallingIdentity();
        try {
            UserAccounts accounts = getUserAccounts(userId);
            return getAccountsInternal(accounts, callingUid, callingPackage, visibleAccountTypes);
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    private Account[] getAccountsInternal(UserAccounts userAccounts, int callingUid, String callingPackage, List<String> visibleAccountTypes) {
        Account[] result;
        synchronized (userAccounts.cacheLock) {
            ArrayList<Account> visibleAccounts = new ArrayList<>();
            for (String visibleType : visibleAccountTypes) {
                Account[] accountsForType = getAccountsFromCacheLocked(userAccounts, visibleType, callingUid, callingPackage);
                if (accountsForType != null) {
                    visibleAccounts.addAll(Arrays.asList(accountsForType));
                }
            }
            result = new Account[visibleAccounts.size()];
            for (int i = 0; i < visibleAccounts.size(); i++) {
                result[i] = visibleAccounts.get(i);
            }
        }
        return result;
    }

    public void addSharedAccountsFromParentUser(int parentUserId, int userId) {
        checkManageOrCreateUsersPermission("addSharedAccountsFromParentUser");
        Account[] accounts = getAccountsAsUser(null, parentUserId, this.mContext.getOpPackageName());
        for (Account account : accounts) {
            addSharedAccountAsUser(account, userId);
        }
    }

    private boolean addSharedAccountAsUser(Account account, int userId) {
        UserAccounts accounts = getUserAccounts(handleIncomingUser(userId));
        SQLiteDatabase db = accounts.openHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(ACCOUNTS_NAME, account.name);
        values.put(DatabaseHelper.SoundModelContract.KEY_TYPE, account.type);
        db.delete(TABLE_SHARED_ACCOUNTS, "name=? AND type=?", new String[]{account.name, account.type});
        long accountId = db.insert(TABLE_SHARED_ACCOUNTS, ACCOUNTS_NAME, values);
        if (accountId < 0) {
            Log.w(TAG, "insertAccountIntoDatabase: " + account + ", skipping the DB insert failed");
            return false;
        }
        logRecord(db, DebugDbHelper.ACTION_ACCOUNT_ADD, TABLE_SHARED_ACCOUNTS, accountId, accounts);
        return true;
    }

    public boolean renameSharedAccountAsUser(Account account, String newName, int userId) {
        UserAccounts accounts = getUserAccounts(handleIncomingUser(userId));
        SQLiteDatabase db = accounts.openHelper.getWritableDatabase();
        long sharedTableAccountId = getAccountIdFromSharedTable(db, account);
        ContentValues values = new ContentValues();
        values.put(ACCOUNTS_NAME, newName);
        int r = db.update(TABLE_SHARED_ACCOUNTS, values, "name=? AND type=?", new String[]{account.name, account.type});
        if (r > 0) {
            int callingUid = getCallingUid();
            logRecord(db, DebugDbHelper.ACTION_ACCOUNT_RENAME, TABLE_SHARED_ACCOUNTS, sharedTableAccountId, accounts, callingUid);
            renameAccountInternal(accounts, account, newName);
        }
        return r > 0;
    }

    public boolean removeSharedAccountAsUser(Account account, int userId) {
        return removeSharedAccountAsUser(account, userId, getCallingUid());
    }

    private boolean removeSharedAccountAsUser(Account account, int userId, int callingUid) {
        UserAccounts accounts = getUserAccounts(handleIncomingUser(userId));
        SQLiteDatabase db = accounts.openHelper.getWritableDatabase();
        long sharedTableAccountId = getAccountIdFromSharedTable(db, account);
        int r = db.delete(TABLE_SHARED_ACCOUNTS, "name=? AND type=?", new String[]{account.name, account.type});
        if (r > 0) {
            logRecord(db, DebugDbHelper.ACTION_ACCOUNT_REMOVE, TABLE_SHARED_ACCOUNTS, sharedTableAccountId, accounts, callingUid);
            removeAccountInternal(accounts, account, callingUid);
        }
        return r > 0;
    }

    public Account[] getSharedAccountsAsUser(int userId) {
        UserAccounts accounts = getUserAccounts(handleIncomingUser(userId));
        ArrayList<Account> accountList = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = accounts.openHelper.getReadableDatabase().query(TABLE_SHARED_ACCOUNTS, new String[]{ACCOUNTS_NAME, DatabaseHelper.SoundModelContract.KEY_TYPE}, null, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(ACCOUNTS_NAME);
                int typeIndex = cursor.getColumnIndex(DatabaseHelper.SoundModelContract.KEY_TYPE);
                do {
                    accountList.add(new Account(cursor.getString(nameIndex), cursor.getString(typeIndex)));
                } while (cursor.moveToNext());
            }
            Account[] accountArray = new Account[accountList.size()];
            accountList.toArray(accountArray);
            return accountArray;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public Account[] getAccounts(String type, String opPackageName) {
        return getAccountsAsUser(type, UserHandle.getCallingUserId(), opPackageName);
    }

    public Account[] getAccountsForPackage(String packageName, int uid, String opPackageName) {
        int callingUid = Binder.getCallingUid();
        if (!UserHandle.isSameApp(callingUid, Process.myUid())) {
            throw new SecurityException("getAccountsForPackage() called from unauthorized uid " + callingUid + " with uid=" + uid);
        }
        return getAccountsAsUser(null, UserHandle.getCallingUserId(), packageName, uid, opPackageName);
    }

    public Account[] getAccountsByTypeForPackage(String type, String packageName, String opPackageName) {
        try {
            int packageUid = AppGlobals.getPackageManager().getPackageUid(packageName, PackageManagerService.DumpState.DUMP_PREFERRED_XML, UserHandle.getCallingUserId());
            return getAccountsAsUser(type, UserHandle.getCallingUserId(), packageName, packageUid, opPackageName);
        } catch (RemoteException re) {
            Slog.e(TAG, "Couldn't determine the packageUid for " + packageName + re);
            return new Account[0];
        }
    }

    public void getAccountsByFeatures(IAccountManagerResponse response, String type, String[] features, String opPackageName) {
        Account[] accounts;
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "getAccounts: accountType " + type + ", response " + response + ", features " + stringArrayToString(features) + ", caller's uid " + callingUid + ", pid " + Binder.getCallingPid());
        }
        if (response == null) {
            throw new IllegalArgumentException("response is null");
        }
        if (type == null) {
            throw new IllegalArgumentException("accountType is null");
        }
        int userId = UserHandle.getCallingUserId();
        List<String> visibleAccountTypes = getTypesVisibleToCaller(callingUid, userId, opPackageName);
        if (!visibleAccountTypes.contains(type)) {
            Bundle result = new Bundle();
            result.putParcelableArray(TABLE_ACCOUNTS, new Account[0]);
            try {
                response.onResult(result);
                return;
            } catch (RemoteException e) {
                Log.e(TAG, "Cannot respond to caller do to exception.", e);
                return;
            }
        }
        long identityToken = clearCallingIdentity();
        try {
            UserAccounts userAccounts = getUserAccounts(userId);
            if (features != null && features.length != 0) {
                new GetAccountsByTypeAndFeatureSession(userAccounts, response, type, features, callingUid).bind();
                return;
            }
            synchronized (userAccounts.cacheLock) {
                accounts = getAccountsFromCacheLocked(userAccounts, type, callingUid, null);
            }
            Bundle result2 = new Bundle();
            result2.putParcelableArray(TABLE_ACCOUNTS, accounts);
            onResult(response, result2);
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    private long getAccountIdFromSharedTable(SQLiteDatabase db, Account account) {
        Cursor cursor = db.query(TABLE_SHARED_ACCOUNTS, new String[]{"_id"}, "name=? AND type=?", new String[]{account.name, account.type}, null, null, null);
        try {
            if (cursor.moveToNext()) {
                return cursor.getLong(0);
            }
            return -1L;
        } finally {
            cursor.close();
        }
    }

    private long getAccountIdLocked(SQLiteDatabase db, Account account) {
        Cursor cursor = db.query(TABLE_ACCOUNTS, new String[]{"_id"}, "name=? AND type=?", new String[]{account.name, account.type}, null, null, null);
        try {
            if (cursor.moveToNext()) {
                return cursor.getLong(0);
            }
            return -1L;
        } finally {
            cursor.close();
        }
    }

    private long getExtrasIdLocked(SQLiteDatabase db, long accountId, String key) {
        Cursor cursor = db.query(CE_TABLE_EXTRAS, new String[]{"_id"}, "accounts_id=" + accountId + " AND key=?", new String[]{key}, null, null, null);
        try {
            if (cursor.moveToNext()) {
                return cursor.getLong(0);
            }
            return -1L;
        } finally {
            cursor.close();
        }
    }

    private abstract class Session extends IAccountAuthenticatorResponse.Stub implements IBinder.DeathRecipient, ServiceConnection {
        final String mAccountName;
        final String mAccountType;
        protected final UserAccounts mAccounts;
        final boolean mAuthDetailsRequired;
        IAccountAuthenticator mAuthenticator;
        final long mCreationTime;
        final boolean mExpectActivityLaunch;
        private int mNumErrors;
        private int mNumRequestContinued;
        public int mNumResults;
        IAccountManagerResponse mResponse;
        private final boolean mStripAuthTokenFromResult;
        final boolean mUpdateLastAuthenticatedTime;

        public abstract void run() throws RemoteException;

        public Session(AccountManagerService this$0, UserAccounts accounts, IAccountManagerResponse response, String accountType, boolean expectActivityLaunch, boolean stripAuthTokenFromResult, String accountName, boolean authDetailsRequired) {
            this(accounts, response, accountType, expectActivityLaunch, stripAuthTokenFromResult, accountName, authDetailsRequired, false);
        }

        public Session(UserAccounts accounts, IAccountManagerResponse response, String accountType, boolean expectActivityLaunch, boolean stripAuthTokenFromResult, String accountName, boolean authDetailsRequired, boolean updateLastAuthenticatedTime) {
            this.mNumResults = 0;
            this.mNumRequestContinued = 0;
            this.mNumErrors = 0;
            this.mAuthenticator = null;
            if (accountType == null) {
                throw new IllegalArgumentException("accountType is null");
            }
            this.mAccounts = accounts;
            this.mStripAuthTokenFromResult = stripAuthTokenFromResult;
            this.mResponse = response;
            this.mAccountType = accountType;
            this.mExpectActivityLaunch = expectActivityLaunch;
            this.mCreationTime = SystemClock.elapsedRealtime();
            this.mAccountName = accountName;
            this.mAuthDetailsRequired = authDetailsRequired;
            this.mUpdateLastAuthenticatedTime = updateLastAuthenticatedTime;
            synchronized (AccountManagerService.this.mSessions) {
                AccountManagerService.this.mSessions.put(toString(), this);
            }
            if (response == null) {
                return;
            }
            try {
                response.asBinder().linkToDeath(this, 0);
            } catch (RemoteException e) {
                this.mResponse = null;
                binderDied();
            }
        }

        IAccountManagerResponse getResponseAndClose() {
            if (this.mResponse == null) {
                return null;
            }
            IAccountManagerResponse response = this.mResponse;
            close();
            return response;
        }

        protected void checkKeyIntent(int authUid, Intent intent) throws SecurityException {
            intent.setFlags(intent.getFlags() & (-196));
            long bid = Binder.clearCallingIdentity();
            try {
                PackageManager pm = AccountManagerService.this.mContext.getPackageManager();
                ResolveInfo resolveInfo = pm.resolveActivityAsUser(intent, 0, this.mAccounts.userId);
                ActivityInfo targetActivityInfo = resolveInfo.activityInfo;
                int targetUid = targetActivityInfo.applicationInfo.uid;
                if (pm.checkSignatures(authUid, targetUid) == 0) {
                    return;
                }
                String pkgName = targetActivityInfo.packageName;
                String activityName = targetActivityInfo.name;
                throw new SecurityException(String.format("KEY_INTENT resolved to an Activity (%s) in a package (%s) that does not share a signature with the supplying authenticator (%s).", activityName, pkgName, this.mAccountType));
            } finally {
                Binder.restoreCallingIdentity(bid);
            }
        }

        private void close() {
            synchronized (AccountManagerService.this.mSessions) {
                if (AccountManagerService.this.mSessions.remove(toString()) == null) {
                    return;
                }
                if (this.mResponse != null) {
                    this.mResponse.asBinder().unlinkToDeath(this, 0);
                    this.mResponse = null;
                }
                cancelTimeout();
                unbind();
            }
        }

        @Override
        public void binderDied() {
            this.mResponse = null;
            close();
        }

        protected String toDebugString() {
            return toDebugString(SystemClock.elapsedRealtime());
        }

        protected String toDebugString(long now) {
            return "Session: expectLaunch " + this.mExpectActivityLaunch + ", connected " + (this.mAuthenticator != null) + ", stats (" + this.mNumResults + "/" + this.mNumRequestContinued + "/" + this.mNumErrors + "), lifetime " + ((now - this.mCreationTime) / 1000.0d);
        }

        void bind() {
            if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                Log.v(AccountManagerService.TAG, "initiating bind to authenticator type " + this.mAccountType);
            }
            if (bindToAuthenticator(this.mAccountType)) {
                return;
            }
            Log.d(AccountManagerService.TAG, "bind attempt failed for " + toDebugString());
            onError(1, "bind failure");
        }

        private void unbind() {
            if (this.mAuthenticator == null) {
                return;
            }
            this.mAuthenticator = null;
            AccountManagerService.this.mContext.unbindService(this);
        }

        public void cancelTimeout() {
            AccountManagerService.this.mMessageHandler.removeMessages(3, this);
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            this.mAuthenticator = IAccountAuthenticator.Stub.asInterface(service);
            try {
                run();
            } catch (RemoteException e) {
                onError(1, "remote exception");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            this.mAuthenticator = null;
            IAccountManagerResponse response = getResponseAndClose();
            if (response == null) {
                return;
            }
            try {
                response.onError(1, "disconnected");
            } catch (RemoteException e) {
                if (!Log.isLoggable(AccountManagerService.TAG, 2)) {
                    return;
                }
                Log.v(AccountManagerService.TAG, "Session.onServiceDisconnected: caught RemoteException while responding", e);
            }
        }

        public void onTimedOut() {
            IAccountManagerResponse response = getResponseAndClose();
            if (response == null) {
                return;
            }
            try {
                response.onError(1, "timeout");
            } catch (RemoteException e) {
                if (!Log.isLoggable(AccountManagerService.TAG, 2)) {
                    return;
                }
                Log.v(AccountManagerService.TAG, "Session.onTimedOut: caught RemoteException while responding", e);
            }
        }

        public void onResult(Bundle result) {
            IAccountManagerResponse response;
            boolean zContainsKey;
            boolean needUpdate;
            Bundle.setDefusable(result, true);
            this.mNumResults++;
            Intent intent = null;
            if (result != null) {
                boolean isSuccessfulConfirmCreds = result.getBoolean("booleanResult", false);
                if (!result.containsKey("authAccount")) {
                    zContainsKey = false;
                } else {
                    zContainsKey = result.containsKey("accountType");
                }
                if (!this.mUpdateLastAuthenticatedTime) {
                    needUpdate = false;
                } else {
                    needUpdate = !isSuccessfulConfirmCreds ? zContainsKey : true;
                }
                if (needUpdate || this.mAuthDetailsRequired) {
                    boolean accountPresent = AccountManagerService.this.isAccountPresentForCaller(this.mAccountName, this.mAccountType);
                    if (needUpdate && accountPresent) {
                        AccountManagerService.this.updateLastAuthenticatedTime(new Account(this.mAccountName, this.mAccountType));
                    }
                    if (this.mAuthDetailsRequired) {
                        long lastAuthenticatedTime = -1;
                        if (accountPresent) {
                            lastAuthenticatedTime = DatabaseUtils.longForQuery(this.mAccounts.openHelper.getReadableDatabase(), "SELECT last_password_entry_time_millis_epoch FROM accounts WHERE name=? AND type=?", new String[]{this.mAccountName, this.mAccountType});
                        }
                        result.putLong("lastAuthenticatedTime", lastAuthenticatedTime);
                    }
                }
            }
            if (result != null && (intent = (Intent) result.getParcelable("intent")) != null) {
                checkKeyIntent(Binder.getCallingUid(), intent);
            }
            if (result != null && !TextUtils.isEmpty(result.getString(AccountManagerService.AUTHTOKENS_AUTHTOKEN))) {
                String accountName = result.getString("authAccount");
                String accountType = result.getString("accountType");
                if (!TextUtils.isEmpty(accountName) && !TextUtils.isEmpty(accountType)) {
                    Account account = new Account(accountName, accountType);
                    AccountManagerService.this.cancelNotification(AccountManagerService.this.getSigninRequiredNotificationId(this.mAccounts, account).intValue(), new UserHandle(this.mAccounts.userId));
                }
            }
            if (this.mExpectActivityLaunch && result != null && result.containsKey("intent")) {
                response = this.mResponse;
            } else {
                response = getResponseAndClose();
            }
            if (response == null) {
                return;
            }
            try {
                if (result == null) {
                    if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                        Log.v(AccountManagerService.TAG, getClass().getSimpleName() + " calling onError() on response " + response);
                    }
                    response.onError(5, "null bundle returned");
                    return;
                }
                if (this.mStripAuthTokenFromResult) {
                    result.remove(AccountManagerService.AUTHTOKENS_AUTHTOKEN);
                }
                if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                    Log.v(AccountManagerService.TAG, getClass().getSimpleName() + " calling onResult() on response " + response);
                }
                if (result.getInt("errorCode", -1) > 0 && intent == null) {
                    response.onError(result.getInt("errorCode"), result.getString("errorMessage"));
                } else {
                    response.onResult(result);
                }
            } catch (RemoteException e) {
                if (!Log.isLoggable(AccountManagerService.TAG, 2)) {
                    return;
                }
                Log.v(AccountManagerService.TAG, "failure while notifying response", e);
            }
        }

        public void onRequestContinued() {
            this.mNumRequestContinued++;
        }

        public void onError(int errorCode, String errorMessage) {
            this.mNumErrors++;
            IAccountManagerResponse response = getResponseAndClose();
            if (response != null) {
                if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                    Log.v(AccountManagerService.TAG, getClass().getSimpleName() + " calling onError() on response " + response);
                }
                try {
                    response.onError(errorCode, errorMessage);
                    return;
                } catch (RemoteException e) {
                    if (!Log.isLoggable(AccountManagerService.TAG, 2)) {
                        return;
                    }
                    Log.v(AccountManagerService.TAG, "Session.onError: caught RemoteException while responding", e);
                    return;
                }
            }
            if (!Log.isLoggable(AccountManagerService.TAG, 2)) {
                return;
            }
            Log.v(AccountManagerService.TAG, "Session.onError: already closed");
        }

        private boolean bindToAuthenticator(String authenticatorType) {
            RegisteredServicesCache.ServiceInfo<AuthenticatorDescription> serviceInfo = AccountManagerService.this.mAuthenticatorCache.getServiceInfo(AuthenticatorDescription.newKey(authenticatorType), this.mAccounts.userId);
            if (serviceInfo == null) {
                if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                    Log.v(AccountManagerService.TAG, "there is no authenticator for " + authenticatorType + ", bailing out");
                }
                return false;
            }
            if (!AccountManagerService.this.isLocalUnlockedUser(this.mAccounts.userId) && !serviceInfo.componentInfo.directBootAware) {
                Slog.w(AccountManagerService.TAG, "Blocking binding to authenticator " + serviceInfo.componentName + " which isn't encryption aware");
                return false;
            }
            Intent intent = new Intent();
            intent.setAction("android.accounts.AccountAuthenticator");
            intent.setComponent(serviceInfo.componentName);
            if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                Log.v(AccountManagerService.TAG, "performing bindService to " + serviceInfo.componentName);
            }
            if (AccountManagerService.this.mContext.bindServiceAsUser(intent, this, 1, UserHandle.of(this.mAccounts.userId))) {
                return true;
            }
            if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                Log.v(AccountManagerService.TAG, "bindService to " + serviceInfo.componentName + " failed");
            }
            return false;
        }
    }

    private class MessageHandler extends Handler {
        MessageHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 3:
                    Session session = (Session) msg.obj;
                    session.onTimedOut();
                    return;
                case 4:
                    AccountManagerService.this.copyAccountToUser(null, (Account) msg.obj, msg.arg1, msg.arg2);
                    return;
                default:
                    throw new IllegalStateException("unhandled message: " + msg.what);
            }
        }
    }

    String getPreNDatabaseName(int userId) {
        File systemDir = Environment.getDataSystemDirectory();
        File databaseFile = new File(Environment.getUserSystemDirectory(userId), "accounts.db");
        if (userId == 0) {
            File oldFile = new File(systemDir, "accounts.db");
            if (oldFile.exists() && !databaseFile.exists()) {
                File userDir = Environment.getUserSystemDirectory(userId);
                if (!userDir.exists() && !userDir.mkdirs()) {
                    throw new IllegalStateException("User dir cannot be created: " + userDir);
                }
                if (!oldFile.renameTo(databaseFile)) {
                    throw new IllegalStateException("User dir cannot be migrated: " + databaseFile);
                }
            }
        }
        return databaseFile.getPath();
    }

    String getDeDatabaseName(int userId) {
        File databaseFile = new File(Environment.getDataSystemDeDirectory(userId), DE_DATABASE_NAME);
        return databaseFile.getPath();
    }

    String getCeDatabaseName(int userId) {
        File databaseFile = new File(Environment.getDataSystemCeDirectory(userId), CE_DATABASE_NAME);
        return databaseFile.getPath();
    }

    private static class DebugDbHelper {
        private static String TABLE_DEBUG = "debug_table";
        private static String ACTION_TYPE = "action_type";
        private static String TIMESTAMP = "time";
        private static String CALLER_UID = "caller_uid";
        private static String TABLE_NAME = "table_name";
        private static String KEY = "primary_key";
        private static String ACTION_SET_PASSWORD = "action_set_password";
        private static String ACTION_CLEAR_PASSWORD = "action_clear_password";
        private static String ACTION_ACCOUNT_ADD = "action_account_add";
        private static String ACTION_ACCOUNT_REMOVE = "action_account_remove";
        private static String ACTION_ACCOUNT_REMOVE_DE = "action_account_remove_de";
        private static String ACTION_AUTHENTICATOR_REMOVE = "action_authenticator_remove";
        private static String ACTION_ACCOUNT_RENAME = "action_account_rename";
        private static String ACTION_CALLED_ACCOUNT_ADD = "action_called_account_add";
        private static String ACTION_CALLED_ACCOUNT_REMOVE = "action_called_account_remove";
        private static String ACTION_SYNC_DE_CE_ACCOUNTS = "action_sync_de_ce_accounts";
        private static String ACTION_CALLED_START_ACCOUNT_ADD = "action_called_start_account_add";
        private static String ACTION_CALLED_ACCOUNT_SESSION_FINISH = "action_called_account_session_finish";
        private static SimpleDateFormat dateFromat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        private DebugDbHelper() {
        }

        private static void createDebugTable(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + TABLE_DEBUG + " ( _id INTEGER," + ACTION_TYPE + " TEXT NOT NULL, " + TIMESTAMP + " DATETIME," + CALLER_UID + " INTEGER NOT NULL," + TABLE_NAME + " TEXT NOT NULL," + KEY + " INTEGER PRIMARY KEY)");
            db.execSQL("CREATE INDEX timestamp_index ON " + TABLE_DEBUG + " (" + TIMESTAMP + ")");
        }
    }

    private void logRecord(UserAccounts accounts, String action, String tableName) {
        SQLiteDatabase db = accounts.openHelper.getWritableDatabase();
        logRecord(db, action, tableName, -1L, accounts);
    }

    private void logRecordWithUid(UserAccounts accounts, String action, String tableName, int uid) {
        SQLiteDatabase db = accounts.openHelper.getWritableDatabase();
        logRecord(db, action, tableName, -1L, accounts, uid);
    }

    private void logRecord(SQLiteDatabase db, String action, String tableName, long accountId, UserAccounts userAccount) {
        logRecord(db, action, tableName, accountId, userAccount, getCallingUid());
    }

    private void logRecord(SQLiteDatabase db, String action, String tableName, long accountId, UserAccounts userAccount, int callingUid) {
        SQLiteStatement logStatement = userAccount.statementForLogging;
        logStatement.bindLong(1, accountId);
        logStatement.bindString(2, action);
        logStatement.bindString(3, DebugDbHelper.dateFromat.format(new Date()));
        logStatement.bindLong(4, callingUid);
        logStatement.bindString(5, tableName);
        logStatement.bindLong(6, userAccount.debugDbInsertionPoint);
        logStatement.execute();
        logStatement.clearBindings();
        userAccount.debugDbInsertionPoint = (userAccount.debugDbInsertionPoint + 1) % 64;
    }

    private void initializeDebugDbSizeAndCompileSqlStatementForLogging(SQLiteDatabase db, UserAccounts userAccount) {
        int size = (int) getDebugTableRowCount(db);
        if (size >= 64) {
            userAccount.debugDbInsertionPoint = (int) getDebugTableInsertionPoint(db);
        } else {
            userAccount.debugDbInsertionPoint = size;
        }
        compileSqlStatementForLogging(db, userAccount);
    }

    private void compileSqlStatementForLogging(SQLiteDatabase db, UserAccounts userAccount) {
        String sql = "INSERT OR REPLACE INTO " + DebugDbHelper.TABLE_DEBUG + " VALUES (?,?,?,?,?,?)";
        userAccount.statementForLogging = db.compileStatement(sql);
    }

    private long getDebugTableRowCount(SQLiteDatabase db) {
        String queryCountDebugDbRows = "SELECT COUNT(*) FROM " + DebugDbHelper.TABLE_DEBUG;
        return DatabaseUtils.longForQuery(db, queryCountDebugDbRows, null);
    }

    private long getDebugTableInsertionPoint(SQLiteDatabase db) {
        String queryCountDebugDbRows = "SELECT " + DebugDbHelper.KEY + " FROM " + DebugDbHelper.TABLE_DEBUG + " ORDER BY " + DebugDbHelper.TIMESTAMP + "," + DebugDbHelper.KEY + " LIMIT 1";
        return DatabaseUtils.longForQuery(db, queryCountDebugDbRows, null);
    }

    static class PreNDatabaseHelper extends SQLiteOpenHelper {
        private final Context mContext;
        private final int mUserId;

        public PreNDatabaseHelper(Context context, int userId, String preNDatabaseName) {
            super(context, preNDatabaseName, (SQLiteDatabase.CursorFactory) null, 9);
            this.mContext = context;
            this.mUserId = userId;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            throw new IllegalStateException("Legacy database cannot be created - only upgraded!");
        }

        private void createSharedAccountsTable(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE shared_accounts ( _id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, type TEXT NOT NULL, UNIQUE(name,type))");
        }

        private void addLastSuccessfullAuthenticatedTimeColumn(SQLiteDatabase db) {
            db.execSQL("ALTER TABLE accounts ADD COLUMN last_password_entry_time_millis_epoch DEFAULT 0");
        }

        private void addOldAccountNameColumn(SQLiteDatabase db) {
            db.execSQL("ALTER TABLE accounts ADD COLUMN previous_name");
        }

        private void addDebugTable(SQLiteDatabase db) {
            DebugDbHelper.createDebugTable(db);
        }

        private void createAccountsDeletionTrigger(SQLiteDatabase db) {
            db.execSQL(" CREATE TRIGGER accountsDelete DELETE ON accounts BEGIN   DELETE FROM authtokens     WHERE accounts_id=OLD._id ;   DELETE FROM extras     WHERE accounts_id=OLD._id ;   DELETE FROM grants     WHERE accounts_id=OLD._id ; END");
        }

        private void createGrantsTable(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE grants (  accounts_id INTEGER NOT NULL, auth_token_type STRING NOT NULL,  uid INTEGER NOT NULL,  UNIQUE (accounts_id,auth_token_type,uid))");
        }

        private void populateMetaTableWithAuthTypeAndUID(SQLiteDatabase db, Map<String, Integer> authTypeAndUIDMap) {
            for (Map.Entry<String, Integer> entry : authTypeAndUIDMap.entrySet()) {
                ContentValues values = new ContentValues();
                values.put("key", AccountManagerService.META_KEY_FOR_AUTHENTICATOR_UID_FOR_TYPE_PREFIX + entry.getKey());
                values.put("value", entry.getValue());
                db.insert(AccountManagerService.TABLE_META, null, values);
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.e(AccountManagerService.TAG, "upgrade from version " + oldVersion + " to version " + newVersion);
            if (oldVersion == 1) {
                oldVersion++;
            }
            if (oldVersion == 2) {
                createGrantsTable(db);
                db.execSQL("DROP TRIGGER accountsDelete");
                createAccountsDeletionTrigger(db);
                oldVersion++;
            }
            if (oldVersion == 3) {
                db.execSQL("UPDATE accounts SET type = 'com.google' WHERE type == 'com.google.GAIA'");
                oldVersion++;
            }
            if (oldVersion == 4) {
                createSharedAccountsTable(db);
                oldVersion++;
            }
            if (oldVersion == 5) {
                addOldAccountNameColumn(db);
                oldVersion++;
            }
            if (oldVersion == 6) {
                addLastSuccessfullAuthenticatedTimeColumn(db);
                oldVersion++;
            }
            if (oldVersion == 7) {
                addDebugTable(db);
                oldVersion++;
            }
            if (oldVersion == 8) {
                populateMetaTableWithAuthTypeAndUID(db, AccountManagerService.getAuthenticatorTypeAndUIDForUser(this.mContext, this.mUserId));
                oldVersion++;
            }
            if (oldVersion == newVersion) {
                return;
            }
            Log.e(AccountManagerService.TAG, "failed to upgrade version " + oldVersion + " to version " + newVersion);
        }

        @Override
        public void onOpen(SQLiteDatabase db) {
            if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                Log.v(AccountManagerService.TAG, "opened database accounts.db");
            }
        }
    }

    static class DeDatabaseHelper extends SQLiteOpenHelper {
        private volatile boolean mCeAttached;
        private final int mUserId;

        private DeDatabaseHelper(Context context, int userId, String deDatabaseName) {
            super(context, deDatabaseName, (SQLiteDatabase.CursorFactory) null, 1);
            this.mUserId = userId;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.i(AccountManagerService.TAG, "Creating DE database for user " + this.mUserId);
            db.execSQL("CREATE TABLE accounts ( _id INTEGER PRIMARY KEY, name TEXT NOT NULL, type TEXT NOT NULL, previous_name TEXT, last_password_entry_time_millis_epoch INTEGER DEFAULT 0, UNIQUE(name,type))");
            db.execSQL("CREATE TABLE meta ( key TEXT PRIMARY KEY NOT NULL, value TEXT)");
            createGrantsTable(db);
            createSharedAccountsTable(db);
            createAccountsDeletionTrigger(db);
            DebugDbHelper.createDebugTable(db);
        }

        private void createSharedAccountsTable(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE shared_accounts ( _id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, type TEXT NOT NULL, UNIQUE(name,type))");
        }

        private void createAccountsDeletionTrigger(SQLiteDatabase db) {
            db.execSQL(" CREATE TRIGGER accountsDelete DELETE ON accounts BEGIN   DELETE FROM grants     WHERE accounts_id=OLD._id ; END");
        }

        private void createGrantsTable(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE grants (  accounts_id INTEGER NOT NULL, auth_token_type STRING NOT NULL,  uid INTEGER NOT NULL,  UNIQUE (accounts_id,auth_token_type,uid))");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.i(AccountManagerService.TAG, "upgrade from version " + oldVersion + " to version " + newVersion);
            if (oldVersion == newVersion) {
                return;
            }
            Log.e(AccountManagerService.TAG, "failed to upgrade version " + oldVersion + " to version " + newVersion);
        }

        public void attachCeDatabase(File ceDbFile) {
            SQLiteDatabase db = getWritableDatabase();
            db.execSQL("ATTACH DATABASE '" + ceDbFile.getPath() + "' AS ceDb");
            this.mCeAttached = true;
        }

        public boolean isCeDatabaseAttached() {
            return this.mCeAttached;
        }

        public SQLiteDatabase getReadableDatabaseUserIsUnlocked() {
            if (!this.mCeAttached) {
                Log.wtf(AccountManagerService.TAG, "getReadableDatabaseUserIsUnlocked called while user " + this.mUserId + " is still locked. CE database is not yet available.", new Throwable());
            }
            return super.getReadableDatabase();
        }

        public SQLiteDatabase getWritableDatabaseUserIsUnlocked() {
            if (!this.mCeAttached) {
                Log.wtf(AccountManagerService.TAG, "getWritableDatabaseUserIsUnlocked called while user " + this.mUserId + " is still locked. CE database is not yet available.", new Throwable());
            }
            return super.getWritableDatabase();
        }

        @Override
        public void onOpen(SQLiteDatabase db) {
            if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                Log.v(AccountManagerService.TAG, "opened database accounts_de.db");
            }
        }

        private void migratePreNDbToDe(File preNDbFile) {
            Log.i(AccountManagerService.TAG, "Migrate pre-N database to DE preNDbFile=" + preNDbFile);
            SQLiteDatabase db = getWritableDatabase();
            db.execSQL("ATTACH DATABASE '" + preNDbFile.getPath() + "' AS preNDb");
            db.beginTransaction();
            db.execSQL("INSERT INTO accounts(_id,name,type, previous_name, last_password_entry_time_millis_epoch) SELECT _id,name,type, previous_name, last_password_entry_time_millis_epoch FROM preNDb.accounts");
            db.execSQL("INSERT INTO shared_accounts(_id,name,type) SELECT _id,name,type FROM preNDb.shared_accounts");
            db.execSQL("INSERT INTO " + DebugDbHelper.TABLE_DEBUG + "(_id," + DebugDbHelper.ACTION_TYPE + "," + DebugDbHelper.TIMESTAMP + "," + DebugDbHelper.CALLER_UID + "," + DebugDbHelper.TABLE_NAME + "," + DebugDbHelper.KEY + ") SELECT _id," + DebugDbHelper.ACTION_TYPE + "," + DebugDbHelper.TIMESTAMP + "," + DebugDbHelper.CALLER_UID + "," + DebugDbHelper.TABLE_NAME + "," + DebugDbHelper.KEY + " FROM preNDb." + DebugDbHelper.TABLE_DEBUG);
            db.execSQL("INSERT INTO grants(accounts_id,auth_token_type,uid) SELECT accounts_id,auth_token_type,uid FROM preNDb.grants");
            db.execSQL("INSERT INTO meta(key,value) SELECT key,value FROM preNDb.meta");
            db.setTransactionSuccessful();
            db.endTransaction();
            db.execSQL("DETACH DATABASE preNDb");
        }

        static DeDatabaseHelper create(Context context, int userId, File preNDatabaseFile, File deDatabaseFile) {
            boolean newDbExists = deDatabaseFile.exists();
            DeDatabaseHelper deDatabaseHelper = new DeDatabaseHelper(context, userId, deDatabaseFile.getPath());
            if (!newDbExists && preNDatabaseFile.exists()) {
                PreNDatabaseHelper preNDatabaseHelper = new PreNDatabaseHelper(context, userId, preNDatabaseFile.getPath());
                preNDatabaseHelper.getWritableDatabase();
                preNDatabaseHelper.close();
                deDatabaseHelper.migratePreNDbToDe(preNDatabaseFile);
            }
            return deDatabaseHelper;
        }
    }

    static class CeDatabaseHelper extends SQLiteOpenHelper {
        public CeDatabaseHelper(Context context, String ceDatabaseName) {
            super(context, ceDatabaseName, (SQLiteDatabase.CursorFactory) null, 10);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.i(AccountManagerService.TAG, "Creating CE database " + getDatabaseName());
            db.execSQL("CREATE TABLE accounts ( _id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, type TEXT NOT NULL, password TEXT, UNIQUE(name,type))");
            db.execSQL("CREATE TABLE authtokens (  _id INTEGER PRIMARY KEY AUTOINCREMENT,  accounts_id INTEGER NOT NULL, type TEXT NOT NULL,  authtoken TEXT,  UNIQUE (accounts_id,type))");
            db.execSQL("CREATE TABLE extras ( _id INTEGER PRIMARY KEY AUTOINCREMENT, accounts_id INTEGER, key TEXT NOT NULL, value TEXT, UNIQUE(accounts_id,key))");
            createAccountsDeletionTrigger(db);
        }

        private void createAccountsDeletionTrigger(SQLiteDatabase db) {
            db.execSQL(" CREATE TRIGGER accountsDelete DELETE ON accounts BEGIN   DELETE FROM authtokens     WHERE accounts_id=OLD._id ;   DELETE FROM extras     WHERE accounts_id=OLD._id ; END");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.i(AccountManagerService.TAG, "Upgrade CE from version " + oldVersion + " to version " + newVersion);
            if (oldVersion == 9) {
                if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                    Log.v(AccountManagerService.TAG, "onUpgrade upgrading to v10");
                }
                db.execSQL("DROP TABLE IF EXISTS meta");
                db.execSQL("DROP TABLE IF EXISTS shared_accounts");
                db.execSQL("DROP TRIGGER IF EXISTS accountsDelete");
                createAccountsDeletionTrigger(db);
                db.execSQL("DROP TABLE IF EXISTS grants");
                db.execSQL("DROP TABLE IF EXISTS " + DebugDbHelper.TABLE_DEBUG);
                oldVersion++;
            }
            if (oldVersion == newVersion) {
                return;
            }
            Log.e(AccountManagerService.TAG, "failed to upgrade version " + oldVersion + " to version " + newVersion);
        }

        @Override
        public void onOpen(SQLiteDatabase db) {
            if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                Log.v(AccountManagerService.TAG, "opened database accounts_ce.db");
            }
        }

        static String findAccountPasswordByNameAndType(SQLiteDatabase db, String name, String type) {
            Cursor cursor = db.query(AccountManagerService.CE_TABLE_ACCOUNTS, new String[]{AccountManagerService.ACCOUNTS_PASSWORD}, "name=? AND type=?", new String[]{name, type}, null, null, null);
            try {
                if (cursor.moveToNext()) {
                    return cursor.getString(0);
                }
                return null;
            } finally {
                cursor.close();
            }
        }

        static List<Account> findCeAccountsNotInDe(SQLiteDatabase db) {
            Cursor cursor = db.rawQuery("SELECT name,type FROM ceDb.accounts WHERE NOT EXISTS  (SELECT _id FROM accounts WHERE _id=ceDb.accounts._id )", null);
            try {
                List<Account> accounts = new ArrayList<>(cursor.getCount());
                while (cursor.moveToNext()) {
                    String accountName = cursor.getString(0);
                    String accountType = cursor.getString(1);
                    accounts.add(new Account(accountName, accountType));
                }
                return accounts;
            } finally {
                cursor.close();
            }
        }

        static CeDatabaseHelper create(Context context, int userId, File preNDatabaseFile, File ceDatabaseFile) {
            boolean newDbExists = ceDatabaseFile.exists();
            if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                Log.v(AccountManagerService.TAG, "CeDatabaseHelper.create userId=" + userId + " oldDbExists=" + preNDatabaseFile.exists() + " newDbExists=" + newDbExists);
            }
            boolean removeOldDb = false;
            if (!newDbExists && preNDatabaseFile.exists()) {
                removeOldDb = migratePreNDbToCe(preNDatabaseFile, ceDatabaseFile);
            }
            CeDatabaseHelper ceHelper = new CeDatabaseHelper(context, ceDatabaseFile.getPath());
            ceHelper.getWritableDatabase();
            ceHelper.close();
            if (removeOldDb) {
                Slog.i(AccountManagerService.TAG, "Migration complete - removing pre-N db " + preNDatabaseFile);
                if (!SQLiteDatabase.deleteDatabase(preNDatabaseFile)) {
                    Slog.e(AccountManagerService.TAG, "Cannot remove pre-N db " + preNDatabaseFile);
                }
            }
            return ceHelper;
        }

        private static boolean migratePreNDbToCe(File oldDbFile, File ceDbFile) {
            Slog.i(AccountManagerService.TAG, "Moving pre-N DB " + oldDbFile + " to CE " + ceDbFile);
            try {
                FileUtils.copyFileOrThrow(oldDbFile, ceDbFile);
                return true;
            } catch (IOException e) {
                Slog.e(AccountManagerService.TAG, "Cannot copy file to " + ceDbFile + " from " + oldDbFile, e);
                AccountManagerService.deleteDbFileWarnIfFailed(ceDbFile);
                return false;
            }
        }
    }

    public IBinder onBind(Intent intent) {
        return asBinder();
    }

    private static boolean scanArgs(String[] args, String value) {
        if (args != null) {
            for (String arg : args) {
                if (value.equals(arg)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected void dump(FileDescriptor fd, PrintWriter fout, String[] args) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
            fout.println("Permission Denial: can't dump AccountsManager from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " without permission android.permission.DUMP");
            return;
        }
        boolean zScanArgs = !scanArgs(args, "--checkin") ? scanArgs(args, "-c") : true;
        IndentingPrintWriter ipw = new IndentingPrintWriter(fout, "  ");
        List<UserInfo> users = getUserManager().getUsers();
        for (UserInfo user : users) {
            ipw.println("User " + user + META_KEY_DELIMITER);
            ipw.increaseIndent();
            dumpUser(getUserAccounts(user.id), fd, ipw, args, zScanArgs);
            ipw.println();
            ipw.decreaseIndent();
        }
    }

    private void dumpUser(UserAccounts userAccounts, FileDescriptor fd, PrintWriter fout, String[] args, boolean isCheckinRequest) {
        Cursor cursor;
        synchronized (userAccounts.cacheLock) {
            SQLiteDatabase db = userAccounts.openHelper.getReadableDatabase();
            if (isCheckinRequest) {
                cursor = db.query(TABLE_ACCOUNTS, ACCOUNT_TYPE_COUNT_PROJECTION, null, null, DatabaseHelper.SoundModelContract.KEY_TYPE, null, null);
                while (cursor.moveToNext()) {
                    try {
                        fout.println(cursor.getString(0) + "," + cursor.getString(1));
                    } finally {
                        if (cursor != null) {
                        }
                    }
                }
            } else {
                Account[] accounts = getAccountsFromCacheLocked(userAccounts, null, Process.myUid(), null);
                fout.println("Accounts: " + accounts.length);
                for (Account account : accounts) {
                    fout.println("  " + account);
                }
                fout.println();
                cursor = db.query(DebugDbHelper.TABLE_DEBUG, null, null, null, null, null, DebugDbHelper.TIMESTAMP);
                fout.println("AccountId, Action_Type, timestamp, UID, TableName, Key");
                fout.println("Accounts History");
                while (cursor.moveToNext()) {
                    try {
                        fout.println(cursor.getString(0) + "," + cursor.getString(1) + "," + cursor.getString(2) + "," + cursor.getString(3) + "," + cursor.getString(4) + "," + cursor.getString(5));
                    } finally {
                        cursor.close();
                    }
                }
                cursor.close();
                fout.println();
                synchronized (this.mSessions) {
                    long now = SystemClock.elapsedRealtime();
                    fout.println("Active Sessions: " + this.mSessions.size());
                    for (Session session : this.mSessions.values()) {
                        fout.println("  " + session.toDebugString(now));
                    }
                }
                fout.println();
                this.mAuthenticatorCache.dump(fd, fout, args, userAccounts.userId);
            }
        }
    }

    private void doNotification(UserAccounts accounts, Account account, CharSequence message, Intent intent, int userId) {
        long identityToken = clearCallingIdentity();
        try {
            if (Log.isLoggable(TAG, 2)) {
                Log.v(TAG, "doNotification: " + message + " intent:" + intent);
            }
            if (intent.getComponent() == null || !GrantCredentialsPermissionActivity.class.getName().equals(intent.getComponent().getClassName())) {
                Integer notificationId = getSigninRequiredNotificationId(accounts, account);
                intent.addCategory(String.valueOf(notificationId));
                UserHandle user = new UserHandle(userId);
                Context contextForUser = getContextForUser(user);
                String notificationTitleFormat = contextForUser.getText(R.string.accessibility_service_warning_description).toString();
                Notification n = new Notification.Builder(contextForUser).setWhen(0L).setSmallIcon(R.drawable.stat_sys_warning).setColor(contextForUser.getColor(R.color.system_accent3_600)).setContentTitle(String.format(notificationTitleFormat, account.name)).setContentText(message).setContentIntent(PendingIntent.getActivityAsUser(this.mContext, 0, intent, 268435456, null, user)).build();
                installNotification(notificationId.intValue(), n, user);
            } else {
                createNoCredentialsPermissionNotification(account, intent, userId);
            }
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    protected void installNotification(int notificationId, Notification n, UserHandle user) {
        ((NotificationManager) this.mContext.getSystemService("notification")).notifyAsUser(null, notificationId, n, user);
    }

    protected void cancelNotification(int id, UserHandle user) {
        long identityToken = clearCallingIdentity();
        try {
            ((NotificationManager) this.mContext.getSystemService("notification")).cancelAsUser(null, id, user);
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    private boolean isPermitted(String opPackageName, int callingUid, String... permissions) {
        for (String perm : permissions) {
            if (this.mContext.checkCallingOrSelfPermission(perm) == 0) {
                if (Log.isLoggable(TAG, 2)) {
                    Log.v(TAG, "  caller uid " + callingUid + " has " + perm);
                }
                int opCode = AppOpsManager.permissionToOpCode(perm);
                if (opCode == -1 || this.mAppOpsManager.noteOp(opCode, callingUid, opPackageName) == 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private int handleIncomingUser(int userId) {
        try {
            return ActivityManagerNative.getDefault().handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, true, true, "", (String) null);
        } catch (RemoteException e) {
            return userId;
        }
    }

    private boolean isPrivileged(int callingUid) {
        int callingUserId = UserHandle.getUserId(callingUid);
        try {
            PackageManager userPackageManager = this.mContext.createPackageContextAsUser("android", 0, new UserHandle(callingUserId)).getPackageManager();
            String[] packages = userPackageManager.getPackagesForUid(callingUid);
            for (String name : packages) {
                try {
                    PackageInfo packageInfo = userPackageManager.getPackageInfo(name, 0);
                    if (packageInfo != null && (packageInfo.applicationInfo.privateFlags & 8) != 0) {
                        return true;
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    return false;
                }
            }
            return false;
        } catch (PackageManager.NameNotFoundException e2) {
            return false;
        }
    }

    private boolean permissionIsGranted(Account account, String authTokenType, int callerUid, int userId) {
        boolean isPrivileged = isPrivileged(callerUid);
        boolean zIsAccountManagedByCaller = account != null ? isAccountManagedByCaller(account.type, callerUid, userId) : false;
        boolean zHasExplicitlyGrantedPermission = account != null ? hasExplicitlyGrantedPermission(account, authTokenType, callerUid) : false;
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "checkGrantsOrCallingUidAgainstAuthenticator: caller uid " + callerUid + ", " + account + ": is authenticator? " + zIsAccountManagedByCaller + ", has explicit permission? " + zHasExplicitlyGrantedPermission);
        }
        if (zIsAccountManagedByCaller || zHasExplicitlyGrantedPermission) {
            return true;
        }
        return isPrivileged;
    }

    private boolean isAccountVisibleToCaller(String accountType, int callingUid, int userId, String opPackageName) {
        if (accountType == null) {
            return false;
        }
        return getTypesVisibleToCaller(callingUid, userId, opPackageName).contains(accountType);
    }

    private boolean isAccountManagedByCaller(String accountType, int callingUid, int userId) {
        if (accountType == null) {
            return false;
        }
        return getTypesManagedByCaller(callingUid, userId).contains(accountType);
    }

    private List<String> getTypesVisibleToCaller(int callingUid, int userId, String opPackageName) {
        boolean isPermitted = isPermitted(opPackageName, callingUid, "android.permission.GET_ACCOUNTS", "android.permission.GET_ACCOUNTS_PRIVILEGED");
        return getTypesForCaller(callingUid, userId, isPermitted);
    }

    private List<String> getTypesManagedByCaller(int callingUid, int userId) {
        return getTypesForCaller(callingUid, userId, false);
    }

    private List<String> getTypesForCaller(int callingUid, int userId, boolean isOtherwisePermitted) {
        List<String> managedAccountTypes = new ArrayList<>();
        long identityToken = Binder.clearCallingIdentity();
        try {
            Collection<RegisteredServicesCache.ServiceInfo<AuthenticatorDescription>> serviceInfos = this.mAuthenticatorCache.getAllServices(userId);
            Binder.restoreCallingIdentity(identityToken);
            for (RegisteredServicesCache.ServiceInfo<AuthenticatorDescription> serviceInfo : serviceInfos) {
                int sigChk = this.mPackageManager.checkSignatures(serviceInfo.uid, callingUid);
                if (isOtherwisePermitted || sigChk == 0) {
                    managedAccountTypes.add(((AuthenticatorDescription) serviceInfo.type).type);
                }
            }
            return managedAccountTypes;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identityToken);
            throw th;
        }
    }

    private boolean isAccountPresentForCaller(String accountName, String accountType) {
        if (getUserAccountsForCaller().accountCache.containsKey(accountType)) {
            for (Account account : (Account[]) getUserAccountsForCaller().accountCache.get(accountType)) {
                if (account.name.equals(accountName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void checkManageUsersPermission(String message) {
        if (ActivityManager.checkComponentPermission("android.permission.MANAGE_USERS", Binder.getCallingUid(), -1, true) == 0) {
        } else {
            throw new SecurityException("You need MANAGE_USERS permission to: " + message);
        }
    }

    private static void checkManageOrCreateUsersPermission(String message) {
        if (ActivityManager.checkComponentPermission("android.permission.MANAGE_USERS", Binder.getCallingUid(), -1, true) == 0 || ActivityManager.checkComponentPermission("android.permission.CREATE_USERS", Binder.getCallingUid(), -1, true) == 0) {
        } else {
            throw new SecurityException("You need MANAGE_USERS or CREATE_USERS permission to: " + message);
        }
    }

    private boolean hasExplicitlyGrantedPermission(Account account, String authTokenType, int callerUid) {
        if (callerUid == 1000) {
            return true;
        }
        UserAccounts accounts = getUserAccountsForCaller();
        synchronized (accounts.cacheLock) {
            SQLiteDatabase db = accounts.openHelper.getReadableDatabase();
            String[] args = {String.valueOf(callerUid), authTokenType, account.name, account.type};
            boolean permissionGranted = DatabaseUtils.longForQuery(db, COUNT_OF_MATCHING_GRANTS, args) != 0;
            if (!permissionGranted && ActivityManager.isRunningInTestHarness()) {
                Log.d(TAG, "no credentials permission for usage of " + account + ", " + authTokenType + " by uid " + callerUid + " but ignoring since device is in test harness.");
                return true;
            }
            return permissionGranted;
        }
    }

    private boolean isSystemUid(int callingUid) {
        PackageInfo packageInfo;
        long ident = Binder.clearCallingIdentity();
        try {
            String[] packages = this.mPackageManager.getPackagesForUid(callingUid);
            if (packages != null) {
                for (String name : packages) {
                    try {
                        packageInfo = this.mPackageManager.getPackageInfo(name, 0);
                    } catch (PackageManager.NameNotFoundException e) {
                        Log.w(TAG, String.format("Could not find package [%s]", name), e);
                    }
                    if (packageInfo != null && (packageInfo.applicationInfo.flags & 1) != 0) {
                        return true;
                    }
                }
            } else {
                Log.w(TAG, "No known packages with uid " + callingUid);
            }
            return false;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void checkReadAccountsPermitted(int callingUid, String accountType, int userId, String opPackageName) {
        if (isAccountVisibleToCaller(accountType, callingUid, userId, opPackageName)) {
            return;
        }
        String msg = String.format("caller uid %s cannot access %s accounts", Integer.valueOf(callingUid), accountType);
        Log.w(TAG, "  " + msg);
        throw new SecurityException(msg);
    }

    private boolean canUserModifyAccounts(int userId, int callingUid) {
        return isProfileOwner(callingUid) || !getUserManager().getUserRestrictions(new UserHandle(userId)).getBoolean("no_modify_accounts");
    }

    private boolean canUserModifyAccountsForType(int userId, String accountType, int callingUid) {
        if (isProfileOwner(callingUid)) {
            return true;
        }
        DevicePolicyManager dpm = (DevicePolicyManager) this.mContext.getSystemService("device_policy");
        String[] typesArray = dpm.getAccountTypesWithManagementDisabledAsUser(userId);
        if (typesArray == null) {
            return true;
        }
        for (String forbiddenType : typesArray) {
            if (forbiddenType.equals(accountType)) {
                return false;
            }
        }
        return true;
    }

    private boolean isProfileOwner(int uid) {
        DevicePolicyManagerInternal dpmi = (DevicePolicyManagerInternal) LocalServices.getService(DevicePolicyManagerInternal.class);
        if (dpmi != null) {
            return dpmi.isActiveAdminWithPolicy(uid, -1);
        }
        return false;
    }

    public void updateAppPermission(Account account, String authTokenType, int uid, boolean value) throws RemoteException {
        int callingUid = getCallingUid();
        if (callingUid != 1000) {
            throw new SecurityException();
        }
        if (value) {
            grantAppPermission(account, authTokenType, uid);
        } else {
            revokeAppPermission(account, authTokenType, uid);
        }
    }

    private void grantAppPermission(Account account, String authTokenType, int uid) {
        if (account == null || authTokenType == null) {
            Log.e(TAG, "grantAppPermission: called with invalid arguments", new Exception());
            return;
        }
        UserAccounts accounts = getUserAccounts(UserHandle.getUserId(uid));
        synchronized (accounts.cacheLock) {
            SQLiteDatabase db = accounts.openHelper.getWritableDatabase();
            db.beginTransaction();
            try {
                long accountId = getAccountIdLocked(db, account);
                if (accountId >= 0) {
                    ContentValues values = new ContentValues();
                    values.put("accounts_id", Long.valueOf(accountId));
                    values.put(GRANTS_AUTH_TOKEN_TYPE, authTokenType);
                    values.put("uid", Integer.valueOf(uid));
                    db.insert(TABLE_GRANTS, "accounts_id", values);
                    db.setTransactionSuccessful();
                }
                db.endTransaction();
                cancelNotification(getCredentialPermissionNotificationId(account, authTokenType, uid).intValue(), UserHandle.of(accounts.userId));
            } catch (Throwable th) {
                db.endTransaction();
                throw th;
            }
        }
    }

    private void revokeAppPermission(Account account, String authTokenType, int uid) {
        if (account == null || authTokenType == null) {
            Log.e(TAG, "revokeAppPermission: called with invalid arguments", new Exception());
            return;
        }
        UserAccounts accounts = getUserAccounts(UserHandle.getUserId(uid));
        synchronized (accounts.cacheLock) {
            SQLiteDatabase db = accounts.openHelper.getWritableDatabase();
            db.beginTransaction();
            try {
                long accountId = getAccountIdLocked(db, account);
                if (accountId >= 0) {
                    db.delete(TABLE_GRANTS, "accounts_id=? AND auth_token_type=? AND uid=?", new String[]{String.valueOf(accountId), authTokenType, String.valueOf(uid)});
                    db.setTransactionSuccessful();
                }
                db.endTransaction();
                cancelNotification(getCredentialPermissionNotificationId(account, authTokenType, uid).intValue(), new UserHandle(accounts.userId));
            } catch (Throwable th) {
                db.endTransaction();
                throw th;
            }
        }
    }

    private static final String stringArrayToString(String[] value) {
        if (value != null) {
            return "[" + TextUtils.join(",", value) + "]";
        }
        return null;
    }

    private void removeAccountFromCacheLocked(UserAccounts accounts, Account account) {
        Account[] oldAccountsForType = (Account[]) accounts.accountCache.get(account.type);
        if (oldAccountsForType != null) {
            ArrayList<Account> newAccountsList = new ArrayList<>();
            for (Account curAccount : oldAccountsForType) {
                if (!curAccount.equals(account)) {
                    newAccountsList.add(curAccount);
                }
            }
            if (newAccountsList.isEmpty()) {
                accounts.accountCache.remove(account.type);
            } else {
                Account[] newAccountsForType = new Account[newAccountsList.size()];
                accounts.accountCache.put(account.type, (Account[]) newAccountsList.toArray(newAccountsForType));
            }
        }
        accounts.userDataCache.remove(account);
        accounts.authTokenCache.remove(account);
        accounts.previousNameCache.remove(account);
    }

    private void insertAccountIntoCacheLocked(UserAccounts accounts, Account account) {
        Account[] accountsForType = (Account[]) accounts.accountCache.get(account.type);
        int oldLength = accountsForType != null ? accountsForType.length : 0;
        Account[] newAccountsForType = new Account[oldLength + 1];
        if (accountsForType != null) {
            System.arraycopy(accountsForType, 0, newAccountsForType, 0, oldLength);
        }
        newAccountsForType[oldLength] = account;
        accounts.accountCache.put(account.type, newAccountsForType);
    }

    private Account[] filterSharedAccounts(UserAccounts userAccounts, Account[] unfiltered, int callingUid, String callingPackage) {
        UserInfo user;
        if (getUserManager() != null && userAccounts != null && userAccounts.userId >= 0 && callingUid != Process.myUid() && (user = getUserManager().getUserInfo(userAccounts.userId)) != null && user.isRestricted()) {
            String[] packages = this.mPackageManager.getPackagesForUid(callingUid);
            String whiteList = this.mContext.getResources().getString(R.string.PERSOSUBSTATE_RUIM_CORPORATE_ERROR);
            for (String packageName : packages) {
                if (whiteList.contains(";" + packageName + ";")) {
                    return unfiltered;
                }
            }
            ArrayList<Account> allowed = new ArrayList<>();
            Account[] sharedAccounts = getSharedAccountsAsUser(userAccounts.userId);
            if (sharedAccounts == null || sharedAccounts.length == 0) {
                return unfiltered;
            }
            String requiredAccountType = "";
            try {
                if (callingPackage != null) {
                    PackageInfo pi = this.mPackageManager.getPackageInfo(callingPackage, 0);
                    if (pi != null && pi.restrictedAccountType != null) {
                        requiredAccountType = pi.restrictedAccountType;
                    }
                } else {
                    int i = 0;
                    int length = packages.length;
                    while (true) {
                        if (i < length) {
                            String packageName2 = packages[i];
                            PackageInfo pi2 = this.mPackageManager.getPackageInfo(packageName2, 0);
                            if (pi2 == null || pi2.restrictedAccountType == null) {
                                i++;
                            } else {
                                requiredAccountType = pi2.restrictedAccountType;
                                break;
                            }
                        }
                    }
                }
                break;
            } catch (PackageManager.NameNotFoundException e) {
            }
            int i2 = 0;
            int length2 = unfiltered.length;
            while (true) {
                int i3 = i2;
                if (i3 < length2) {
                    Account account = unfiltered[i3];
                    if (account.type.equals(requiredAccountType)) {
                        allowed.add(account);
                    } else {
                        boolean found = false;
                        int i4 = 0;
                        int length3 = sharedAccounts.length;
                        while (true) {
                            if (i4 >= length3) {
                                break;
                            }
                            Account shared = sharedAccounts[i4];
                            if (!shared.equals(account)) {
                                i4++;
                            } else {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            allowed.add(account);
                        }
                    }
                    i2 = i3 + 1;
                } else {
                    Account[] filtered = new Account[allowed.size()];
                    allowed.toArray(filtered);
                    return filtered;
                }
            }
        } else {
            return unfiltered;
        }
    }

    protected Account[] getAccountsFromCacheLocked(UserAccounts userAccounts, String accountType, int callingUid, String callingPackage) {
        if (accountType != null) {
            Account[] accounts = (Account[]) userAccounts.accountCache.get(accountType);
            if (accounts == null) {
                return EMPTY_ACCOUNT_ARRAY;
            }
            return filterSharedAccounts(userAccounts, (Account[]) Arrays.copyOf(accounts, accounts.length), callingUid, callingPackage);
        }
        int totalLength = 0;
        Iterator accounts$iterator = userAccounts.accountCache.values().iterator();
        while (accounts$iterator.hasNext()) {
            totalLength += ((Account[]) accounts$iterator.next()).length;
        }
        if (totalLength == 0) {
            return EMPTY_ACCOUNT_ARRAY;
        }
        Account[] accounts2 = new Account[totalLength];
        int totalLength2 = 0;
        for (Account[] accountsOfType : userAccounts.accountCache.values()) {
            System.arraycopy(accountsOfType, 0, accounts2, totalLength2, accountsOfType.length);
            totalLength2 += accountsOfType.length;
        }
        return filterSharedAccounts(userAccounts, accounts2, callingUid, callingPackage);
    }

    protected void writeUserDataIntoCacheLocked(UserAccounts accounts, SQLiteDatabase db, Account account, String key, String value) {
        HashMap<String, String> userDataForAccount = (HashMap) accounts.userDataCache.get(account);
        if (userDataForAccount == null) {
            userDataForAccount = readUserDataForAccountFromDatabaseLocked(db, account);
            accounts.userDataCache.put(account, userDataForAccount);
        }
        if (value == null) {
            userDataForAccount.remove(key);
        } else {
            userDataForAccount.put(key, value);
        }
    }

    protected String readCachedTokenInternal(UserAccounts accounts, Account account, String tokenType, String callingPackage, byte[] pkgSigDigest) {
        String str;
        synchronized (accounts.cacheLock) {
            str = accounts.accountTokenCaches.get(account, tokenType, callingPackage, pkgSigDigest);
        }
        return str;
    }

    protected void writeAuthTokenIntoCacheLocked(UserAccounts accounts, SQLiteDatabase db, Account account, String key, String value) {
        HashMap<String, String> authTokensForAccount = (HashMap) accounts.authTokenCache.get(account);
        if (authTokensForAccount == null) {
            authTokensForAccount = readAuthTokensForAccountFromDatabaseLocked(db, account);
            accounts.authTokenCache.put(account, authTokensForAccount);
        }
        if (value == null) {
            authTokensForAccount.remove(key);
        } else {
            authTokensForAccount.put(key, value);
        }
    }

    protected String readAuthTokenInternal(UserAccounts accounts, Account account, String authTokenType) {
        String str;
        synchronized (accounts.cacheLock) {
            HashMap<String, String> authTokensForAccount = (HashMap) accounts.authTokenCache.get(account);
            if (authTokensForAccount == null) {
                SQLiteDatabase db = accounts.openHelper.getReadableDatabaseUserIsUnlocked();
                authTokensForAccount = readAuthTokensForAccountFromDatabaseLocked(db, account);
                accounts.authTokenCache.put(account, authTokensForAccount);
            }
            str = authTokensForAccount.get(authTokenType);
        }
        return str;
    }

    protected String readUserDataInternalLocked(UserAccounts accounts, Account account, String key) {
        HashMap<String, String> userDataForAccount = (HashMap) accounts.userDataCache.get(account);
        if (userDataForAccount == null) {
            SQLiteDatabase db = accounts.openHelper.getReadableDatabaseUserIsUnlocked();
            userDataForAccount = readUserDataForAccountFromDatabaseLocked(db, account);
            accounts.userDataCache.put(account, userDataForAccount);
        }
        return userDataForAccount.get(key);
    }

    protected HashMap<String, String> readUserDataForAccountFromDatabaseLocked(SQLiteDatabase db, Account account) {
        HashMap<String, String> userDataForAccount = new HashMap<>();
        Cursor cursor = db.query(CE_TABLE_EXTRAS, COLUMNS_EXTRAS_KEY_AND_VALUE, "accounts_id=(select _id FROM accounts WHERE name=? AND type=?)", new String[]{account.name, account.type}, null, null, null);
        while (cursor.moveToNext()) {
            try {
                String tmpkey = cursor.getString(0);
                String value = cursor.getString(1);
                userDataForAccount.put(tmpkey, value);
            } finally {
                cursor.close();
            }
        }
        return userDataForAccount;
    }

    protected HashMap<String, String> readAuthTokensForAccountFromDatabaseLocked(SQLiteDatabase db, Account account) {
        HashMap<String, String> authTokensForAccount = new HashMap<>();
        Cursor cursor = db.query(CE_TABLE_AUTHTOKENS, COLUMNS_AUTHTOKENS_TYPE_AND_AUTHTOKEN, "accounts_id=(select _id FROM accounts WHERE name=? AND type=?)", new String[]{account.name, account.type}, null, null, null);
        while (cursor.moveToNext()) {
            try {
                String type = cursor.getString(0);
                String authToken = cursor.getString(1);
                authTokensForAccount.put(type, authToken);
            } finally {
                cursor.close();
            }
        }
        return authTokensForAccount;
    }

    private Context getContextForUser(UserHandle user) {
        try {
            return this.mContext.createPackageContextAsUser(this.mContext.getPackageName(), 0, user);
        } catch (PackageManager.NameNotFoundException e) {
            return this.mContext;
        }
    }

    private void sendResponse(IAccountManagerResponse response, Bundle result) {
        try {
            response.onResult(result);
        } catch (RemoteException e) {
            if (!Log.isLoggable(TAG, 2)) {
                return;
            }
            Log.v(TAG, "failure while notifying response", e);
        }
    }

    private void sendErrorResponse(IAccountManagerResponse response, int errorCode, String errorMessage) {
        try {
            response.onError(errorCode, errorMessage);
        } catch (RemoteException e) {
            if (!Log.isLoggable(TAG, 2)) {
                return;
            }
            Log.v(TAG, "failure while notifying response", e);
        }
    }
}
