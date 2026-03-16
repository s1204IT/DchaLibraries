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
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.RegisteredServicesCache;
import android.content.pm.RegisteredServicesCacheListener;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
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
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.FgThread;
import com.android.server.voiceinteraction.DatabaseHelper;
import com.google.android.collect.Lists;
import com.google.android.collect.Sets;
import java.io.File;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
    private static final String ACCOUNTS_NAME = "name";
    private static final String ACCOUNTS_PASSWORD = "password";
    private static final String ACCOUNTS_PREVIOUS_NAME = "previous_name";
    private static final String ACCOUNTS_TYPE = "type";
    private static final String AUTHTOKENS_ACCOUNTS_ID = "accounts_id";
    private static final String AUTHTOKENS_ID = "_id";
    private static final String AUTHTOKENS_TYPE = "type";
    private static final String COUNT_OF_MATCHING_GRANTS = "SELECT COUNT(*) FROM grants, accounts WHERE accounts_id=_id AND uid=? AND auth_token_type=? AND name=? AND type=?";
    private static final String DATABASE_NAME = "accounts.db";
    private static final int DATABASE_VERSION = 6;
    private static final String EXTRAS_ACCOUNTS_ID = "accounts_id";
    private static final String EXTRAS_ID = "_id";
    private static final String EXTRAS_KEY = "key";
    private static final String EXTRAS_VALUE = "value";
    private static final String GRANTS_ACCOUNTS_ID = "accounts_id";
    private static final String GRANTS_AUTH_TOKEN_TYPE = "auth_token_type";
    private static final String GRANTS_GRANTEE_UID = "uid";
    private static final int MESSAGE_COPY_SHARED_ACCOUNT = 4;
    private static final int MESSAGE_TIMED_OUT = 3;
    private static final String META_KEY = "key";
    private static final String META_VALUE = "value";
    private static final String SELECTION_AUTHTOKENS_BY_ACCOUNT = "accounts_id=(select _id FROM accounts WHERE name=? AND type=?)";
    private static final String SELECTION_USERDATA_BY_ACCOUNT = "accounts_id=(select _id FROM accounts WHERE name=? AND type=?)";
    private static final String TABLE_ACCOUNTS = "accounts";
    private static final String TABLE_AUTHTOKENS = "authtokens";
    private static final String TABLE_EXTRAS = "extras";
    private static final String TABLE_GRANTS = "grants";
    private static final String TABLE_META = "meta";
    private static final String TABLE_SHARED_ACCOUNTS = "shared_accounts";
    private static final String TAG = "AccountManagerService";
    private static final int TIMEOUT_DELAY_MS = 60000;
    private final IAccountAuthenticatorCache mAuthenticatorCache;
    private final Context mContext;
    private final MessageHandler mMessageHandler;
    private final AtomicInteger mNotificationIds;
    private final PackageManager mPackageManager;
    private final LinkedHashMap<String, Session> mSessions;
    private UserManager mUserManager;
    private final SparseArray<UserAccounts> mUsers;
    private static final String ACCOUNTS_TYPE_COUNT = "count(type)";
    private static final String[] ACCOUNT_TYPE_COUNT_PROJECTION = {DatabaseHelper.SoundModelContract.KEY_TYPE, ACCOUNTS_TYPE_COUNT};
    private static final String AUTHTOKENS_AUTHTOKEN = "authtoken";
    private static final String[] COLUMNS_AUTHTOKENS_TYPE_AND_AUTHTOKEN = {DatabaseHelper.SoundModelContract.KEY_TYPE, AUTHTOKENS_AUTHTOKEN};
    private static final String[] COLUMNS_EXTRAS_KEY_AND_VALUE = {"key", "value"};
    private static AtomicReference<AccountManagerService> sThis = new AtomicReference<>();
    private static final Account[] EMPTY_ACCOUNT_ARRAY = new Account[0];
    private static final Intent ACCOUNTS_CHANGED_INTENT = new Intent("android.accounts.LOGIN_ACCOUNTS_CHANGED");

    static {
        ACCOUNTS_CHANGED_INTENT.setFlags(67108864);
    }

    static class UserAccounts {
        private final DatabaseHelper openHelper;
        private final int userId;
        private final HashMap<Pair<Pair<Account, String>, Integer>, Integer> credentialsPermissionNotificationIds = new HashMap<>();
        private final HashMap<Account, Integer> signinRequiredNotificationIds = new HashMap<>();
        private final Object cacheLock = new Object();
        private final HashMap<String, Account[]> accountCache = new LinkedHashMap();
        private final HashMap<Account, HashMap<String, String>> userDataCache = new HashMap<>();
        private final HashMap<Account, HashMap<String, String>> authTokenCache = new HashMap<>();
        private final HashMap<Account, AtomicReference<String>> previousNameCache = new HashMap<>();

        UserAccounts(Context context, int userId) {
            this.userId = userId;
            synchronized (this.cacheLock) {
                this.openHelper = new DatabaseHelper(context, userId);
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
        this.mContext = context;
        this.mPackageManager = packageManager;
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
                if (!intent.getBooleanExtra("android.intent.extra.REPLACING", false)) {
                    AccountManagerService.this.purgeOldGrantsAll();
                }
            }
        }, intentFilter);
        IntentFilter userFilter = new IntentFilter();
        userFilter.addAction("android.intent.action.USER_REMOVED");
        userFilter.addAction("android.intent.action.USER_STARTED");
        this.mContext.registerReceiverAsUser(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                if ("android.intent.action.USER_REMOVED".equals(action)) {
                    AccountManagerService.this.onUserRemoved(intent);
                } else if ("android.intent.action.USER_STARTED".equals(action)) {
                    AccountManagerService.this.onUserStarted(intent);
                }
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

    private UserAccounts initUserLocked(int userId) {
        UserAccounts accounts = this.mUsers.get(userId);
        if (accounts == null) {
            UserAccounts accounts2 = new UserAccounts(this.mContext, userId);
            this.mUsers.append(userId, accounts2);
            purgeOldGrants(accounts2);
            validateAccountsInternal(accounts2, true);
            return accounts2;
        }
        return accounts;
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
            Cursor cursor = db.query(TABLE_GRANTS, new String[]{GRANTS_GRANTEE_UID}, null, null, GRANTS_GRANTEE_UID, null, null);
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

    public void validateAccounts(int userId) {
        UserAccounts accounts = getUserAccounts(userId);
        validateAccountsInternal(accounts, true);
    }

    private void validateAccountsInternal(UserAccounts accounts, boolean invalidateAuthenticatorCache) {
        if (invalidateAuthenticatorCache) {
            this.mAuthenticatorCache.invalidateCache(accounts.userId);
        }
        HashSet hashSetNewHashSet = Sets.newHashSet();
        for (RegisteredServicesCache.ServiceInfo<AuthenticatorDescription> service : this.mAuthenticatorCache.getAllServices(accounts.userId)) {
            hashSetNewHashSet.add(service.type);
        }
        synchronized (accounts.cacheLock) {
            SQLiteDatabase db = accounts.openHelper.getWritableDatabase();
            boolean accountDeleted = false;
            Cursor cursor = db.query(TABLE_ACCOUNTS, new String[]{"_id", DatabaseHelper.SoundModelContract.KEY_TYPE, ACCOUNTS_NAME}, null, null, null, null, "_id");
            try {
                accounts.accountCache.clear();
                HashMap<String, ArrayList<String>> accountNamesByType = new LinkedHashMap<>();
                while (cursor.moveToNext()) {
                    long accountId = cursor.getLong(0);
                    String accountType = cursor.getString(1);
                    String accountName = cursor.getString(2);
                    if (!hashSetNewHashSet.contains(AuthenticatorDescription.newKey(accountType))) {
                        Slog.w(TAG, "deleting account " + accountName + " because type " + accountType + " no longer has a registered authenticator");
                        db.delete(TABLE_ACCOUNTS, "_id=" + accountId, null);
                        accountDeleted = true;
                        Account account = new Account(accountName, accountType);
                        accounts.userDataCache.remove(account);
                        accounts.authTokenCache.remove(account);
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
                    int i = 0;
                    Iterator<String> it = accountNames2.iterator();
                    while (it.hasNext()) {
                        accountsForType[i] = new Account(it.next(), accountType2);
                        i++;
                    }
                    accounts.accountCache.put(accountType2, accountsForType);
                }
            } finally {
                cursor.close();
                if (accountDeleted) {
                    sendAccountsChangedBroadcast(accounts.userId);
                }
            }
        }
    }

    private UserAccounts getUserAccountsForCaller() {
        return getUserAccounts(UserHandle.getCallingUserId());
    }

    protected UserAccounts getUserAccounts(int userId) {
        UserAccounts accounts;
        synchronized (this.mUsers) {
            accounts = this.mUsers.get(userId);
            if (accounts == null) {
                accounts = initUserLocked(userId);
                this.mUsers.append(userId, accounts);
            }
        }
        return accounts;
    }

    private void onUserRemoved(Intent intent) {
        UserAccounts accounts;
        int userId = intent.getIntExtra("android.intent.extra.user_handle", -1);
        if (userId >= 1) {
            synchronized (this.mUsers) {
                accounts = this.mUsers.get(userId);
                this.mUsers.remove(userId);
            }
            if (accounts != null) {
                synchronized (accounts.cacheLock) {
                    accounts.openHelper.close();
                    File dbFile = new File(getDatabaseName(userId));
                    dbFile.delete();
                }
                return;
            }
            File dbFile2 = new File(getDatabaseName(userId));
            dbFile2.delete();
        }
    }

    private void onUserStarted(Intent intent) {
        Account[] sharedAccounts;
        int userId = intent.getIntExtra("android.intent.extra.user_handle", -1);
        if (userId >= 1 && (sharedAccounts = getSharedAccountsAsUser(userId)) != null && sharedAccounts.length != 0) {
            Account[] accounts = getAccountsAsUser(null, userId);
            for (Account sa : sharedAccounts) {
                if (!ArrayUtils.contains(accounts, sa)) {
                    copyAccountToUser(null, sa, 0, userId);
                }
            }
        }
    }

    public void onServiceChanged(AuthenticatorDescription desc, int userId, boolean removed) {
        validateAccountsInternal(getUserAccounts(userId), false);
    }

    public String getPassword(Account account) {
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "getPassword: " + account + ", caller's uid " + Binder.getCallingUid() + ", pid " + Binder.getCallingPid());
        }
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        checkAuthenticateAccountsPermission(account);
        UserAccounts accounts = getUserAccountsForCaller();
        long identityToken = clearCallingIdentity();
        try {
            return readPasswordInternal(accounts, account);
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    private String readPasswordInternal(UserAccounts accounts, Account account) {
        if (account == null) {
            return null;
        }
        synchronized (accounts.cacheLock) {
            SQLiteDatabase db = accounts.openHelper.getReadableDatabase();
            Cursor cursor = db.query(TABLE_ACCOUNTS, new String[]{ACCOUNTS_PASSWORD}, "name=? AND type=?", new String[]{account.name, account.type}, null, null, null);
            try {
                if (!cursor.moveToNext()) {
                    return null;
                }
                return cursor.getString(0);
            } finally {
                cursor.close();
            }
        }
    }

    public String getPreviousName(Account account) {
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "getPreviousName: " + account + ", caller's uid " + Binder.getCallingUid() + ", pid " + Binder.getCallingPid());
        }
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        UserAccounts accounts = getUserAccountsForCaller();
        long identityToken = clearCallingIdentity();
        try {
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
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "getUserData: " + account + ", key " + key + ", caller's uid " + Binder.getCallingUid() + ", pid " + Binder.getCallingPid());
        }
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        if (key == null) {
            throw new IllegalArgumentException("key is null");
        }
        checkAuthenticateAccountsPermission(account);
        UserAccounts accounts = getUserAccountsForCaller();
        long identityToken = clearCallingIdentity();
        try {
            return readUserDataInternal(accounts, account, key);
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public AuthenticatorDescription[] getAuthenticatorTypes(int userId) {
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "getAuthenticatorTypes: for user id " + userId + "caller's uid " + Binder.getCallingUid() + ", pid " + Binder.getCallingPid());
        }
        enforceCrossUserPermission(userId, "User " + UserHandle.getCallingUserId() + " trying get authenticator types for " + userId);
        long identityToken = clearCallingIdentity();
        try {
            Collection<RegisteredServicesCache.ServiceInfo<AuthenticatorDescription>> authenticatorCollection = this.mAuthenticatorCache.getAllServices(userId);
            AuthenticatorDescription[] types = new AuthenticatorDescription[authenticatorCollection.size()];
            int i = 0;
            for (RegisteredServicesCache.ServiceInfo<AuthenticatorDescription> authenticator : authenticatorCollection) {
                types[i] = (AuthenticatorDescription) authenticator.type;
                i++;
            }
            return types;
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    private void enforceCrossUserPermission(int userId, String errorMessage) {
        if (userId != UserHandle.getCallingUserId() && Binder.getCallingUid() != Process.myUid() && this.mContext.checkCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL") != 0) {
            throw new SecurityException(errorMessage);
        }
    }

    public boolean addAccountExplicitly(Account account, String password, Bundle extras) {
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "addAccountExplicitly: " + account + ", caller's uid " + Binder.getCallingUid() + ", pid " + Binder.getCallingPid());
        }
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        checkAuthenticateAccountsPermission(account);
        UserAccounts accounts = getUserAccountsForCaller();
        long identityToken = clearCallingIdentity();
        try {
            return addAccountInternal(accounts, account, password, extras, false);
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public void copyAccountToUser(final IAccountManagerResponse response, final Account account, int userFrom, int userTo) {
        enforceCrossUserPermission(-1, "Calling copyAccountToUser requires android.permission.INTERACT_ACROSS_USERS_FULL");
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
            new Session(fromAccounts, response, account.type, false, false) {
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
                    if (result2 != null && result2.getBoolean("booleanResult", false)) {
                        AccountManagerService.this.completeCloningAccount(response, result2, account, toAccounts);
                    } else {
                        super.onResult(result2);
                    }
                }
            }.bind();
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    private void completeCloningAccount(IAccountManagerResponse response, final Bundle accountCredentials, final Account account, UserAccounts targetUser) {
        long id = clearCallingIdentity();
        try {
            new Session(targetUser, response, account.type, false, false) {
                @Override
                protected String toDebugString(long now) {
                    return super.toDebugString(now) + ", getAccountCredentialsForClone, " + account.type;
                }

                @Override
                public void run() throws RemoteException {
                    UserAccounts owner = AccountManagerService.this.getUserAccounts(0);
                    synchronized (owner.cacheLock) {
                        Account[] arr$ = AccountManagerService.this.getAccounts(0);
                        int len$ = arr$.length;
                        int i$ = 0;
                        while (true) {
                            if (i$ >= len$) {
                                break;
                            }
                            Account acc = arr$[i$];
                            if (!acc.equals(account)) {
                                i$++;
                            } else {
                                this.mAuthenticator.addAccountFromCredentials(this, account, accountCredentials);
                                break;
                            }
                        }
                    }
                }

                @Override
                public void onResult(Bundle result) {
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

    private boolean addAccountInternal(UserAccounts accounts, Account account, String password, Bundle extras, boolean restricted) {
        boolean z;
        if (account == null) {
            return false;
        }
        synchronized (accounts.cacheLock) {
            SQLiteDatabase db = accounts.openHelper.getWritableDatabase();
            db.beginTransaction();
            try {
                long numMatches = DatabaseUtils.longForQuery(db, "select count(*) from accounts WHERE name=? AND type=?", new String[]{account.name, account.type});
                if (numMatches > 0) {
                    Log.w(TAG, "insertAccountIntoDatabase: " + account + ", skipping since the account already exists");
                    z = false;
                    db.endTransaction();
                } else {
                    ContentValues values = new ContentValues();
                    values.put(ACCOUNTS_NAME, account.name);
                    values.put(DatabaseHelper.SoundModelContract.KEY_TYPE, account.type);
                    values.put(ACCOUNTS_PASSWORD, password);
                    long accountId = db.insert(TABLE_ACCOUNTS, ACCOUNTS_NAME, values);
                    if (accountId >= 0) {
                        if (extras != null) {
                            for (String key : extras.keySet()) {
                                String value = extras.getString(key);
                                if (insertExtraLocked(db, accountId, key, value) < 0) {
                                    Log.w(TAG, "insertAccountIntoDatabase: " + account + ", skipping since insertExtra failed for key " + key);
                                    z = false;
                                    db.endTransaction();
                                    break;
                                }
                            }
                            db.setTransactionSuccessful();
                            insertAccountIntoCacheLocked(accounts, account);
                            db.endTransaction();
                            sendAccountsChangedBroadcast(accounts.userId);
                            if (accounts.userId == 0) {
                                addAccountToLimitedUsers(account);
                            }
                            z = true;
                        } else {
                            db.setTransactionSuccessful();
                            insertAccountIntoCacheLocked(accounts, account);
                            db.endTransaction();
                            sendAccountsChangedBroadcast(accounts.userId);
                            if (accounts.userId == 0) {
                            }
                            z = true;
                        }
                    }
                    Log.w(TAG, "insertAccountIntoDatabase: " + account + ", skipping the DB insert failed");
                    z = false;
                    db.endTransaction();
                }
            } catch (Throwable th) {
                db.endTransaction();
                throw th;
            }
        }
        return z;
    }

    private void addAccountToLimitedUsers(Account account) {
        List<UserInfo> users = getUserManager().getUsers();
        for (UserInfo user : users) {
            if (user.isRestricted()) {
                addSharedAccountAsUser(account, user.id);
                try {
                    if (ActivityManagerNative.getDefault().isUserRunning(user.id, false)) {
                        this.mMessageHandler.sendMessage(this.mMessageHandler.obtainMessage(4, 0, user.id, account));
                    }
                } catch (RemoteException e) {
                }
            }
        }
    }

    private long insertExtraLocked(SQLiteDatabase db, long accountId, String key, String value) {
        ContentValues values = new ContentValues();
        values.put("key", key);
        values.put("accounts_id", Long.valueOf(accountId));
        values.put("value", value);
        return db.insert(TABLE_EXTRAS, "key", values);
    }

    public void hasFeatures(IAccountManagerResponse response, Account account, String[] features) {
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "hasFeatures: " + account + ", response " + response + ", features " + stringArrayToString(features) + ", caller's uid " + Binder.getCallingUid() + ", pid " + Binder.getCallingPid());
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
        checkReadAccountsPermission();
        UserAccounts accounts = getUserAccountsForCaller();
        long identityToken = clearCallingIdentity();
        try {
            new TestFeaturesSession(accounts, response, account, features).bind();
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    private class TestFeaturesSession extends Session {
        private final Account mAccount;
        private final String[] mFeatures;

        public TestFeaturesSession(UserAccounts accounts, IAccountManagerResponse response, Account account, String[] features) {
            super(accounts, response, account.type, false, true);
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
            IAccountManagerResponse response = getResponseAndClose();
            if (response != null) {
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
                    if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                        Log.v(AccountManagerService.TAG, "failure while notifying response", e);
                    }
                }
            }
        }

        @Override
        protected String toDebugString(long now) {
            return super.toDebugString(now) + ", hasFeatures, " + this.mAccount + ", " + (this.mFeatures != null ? TextUtils.join(",", this.mFeatures) : null);
        }
    }

    public void renameAccount(IAccountManagerResponse response, Account accountToRename, String newName) {
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "renameAccount: " + accountToRename + " -> " + newName + ", caller's uid " + Binder.getCallingUid() + ", pid " + Binder.getCallingPid());
        }
        if (accountToRename == null) {
            throw new IllegalArgumentException("account is null");
        }
        checkAuthenticateAccountsPermission(accountToRename);
        UserAccounts accounts = getUserAccountsForCaller();
        long identityToken = clearCallingIdentity();
        try {
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
            SQLiteDatabase db = accounts.openHelper.getWritableDatabase();
            db.beginTransaction();
            boolean isSuccessful = false;
            Account renamedAccount = new Account(newName, accountToRename.type);
            try {
                ContentValues values = new ContentValues();
                values.put(ACCOUNTS_NAME, newName);
                values.put(ACCOUNTS_PREVIOUS_NAME, accountToRename.name);
                long accountId = getAccountIdLocked(db, accountToRename);
                if (accountId >= 0) {
                    String[] argsAccountId = {String.valueOf(accountId)};
                    db.update(TABLE_ACCOUNTS, values, "_id=?", argsAccountId);
                    db.setTransactionSuccessful();
                    isSuccessful = true;
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
                    if (accounts.userId == 0) {
                        List<UserInfo> users = this.mUserManager.getUsers(true);
                        for (UserInfo user : users) {
                            if (!user.isPrimary() && user.isRestricted()) {
                                renameSharedAccountAsUser(accountToRename, newName, user.id);
                            }
                        }
                    }
                    sendAccountsChangedBroadcast(accounts.userId);
                }
            } catch (Throwable th) {
                db.endTransaction();
                if (0 != 0) {
                    insertAccountIntoCacheLocked(accounts, renamedAccount);
                    HashMap<String, String> tmpData2 = (HashMap) accounts.userDataCache.get(accountToRename);
                    HashMap<String, String> tmpTokens2 = (HashMap) accounts.authTokenCache.get(accountToRename);
                    removeAccountFromCacheLocked(accounts, accountToRename);
                    accounts.userDataCache.put(renamedAccount, tmpData2);
                    accounts.authTokenCache.put(renamedAccount, tmpTokens2);
                    accounts.previousNameCache.put(renamedAccount, new AtomicReference(accountToRename.name));
                    if (accounts.userId == 0) {
                        List<UserInfo> users2 = this.mUserManager.getUsers(true);
                        for (UserInfo user2 : users2) {
                            if (!user2.isPrimary() && user2.isRestricted()) {
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

    public void removeAccount(IAccountManagerResponse response, Account account, boolean expectActivityLaunch) {
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "removeAccount: " + account + ", response " + response + ", caller's uid " + Binder.getCallingUid() + ", pid " + Binder.getCallingPid());
        }
        if (response == null) {
            throw new IllegalArgumentException("response is null");
        }
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        checkManageAccountsPermission();
        UserHandle user = Binder.getCallingUserHandle();
        UserAccounts accounts = getUserAccountsForCaller();
        int userId = Binder.getCallingUserHandle().getIdentifier();
        if (!canUserModifyAccounts(userId)) {
            try {
                response.onError(6, "User cannot modify accounts");
                return;
            } catch (RemoteException e) {
                return;
            }
        }
        if (!canUserModifyAccountsForType(userId, account.type)) {
            try {
                response.onError(101, "User cannot modify accounts of this type (policy).");
                return;
            } catch (RemoteException e2) {
                return;
            }
        }
        long identityToken = clearCallingIdentity();
        cancelNotification(getSigninRequiredNotificationId(accounts, account).intValue(), user);
        synchronized (accounts.credentialsPermissionNotificationIds) {
            for (Pair<Pair<Account, String>, Integer> pair : accounts.credentialsPermissionNotificationIds.keySet()) {
                if (account.equals(((Pair) pair.first).first)) {
                    int id = ((Integer) accounts.credentialsPermissionNotificationIds.get(pair)).intValue();
                    cancelNotification(id, user);
                }
            }
        }
        try {
            new RemoveAccountSession(accounts, response, account, expectActivityLaunch).bind();
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public void removeAccountAsUser(IAccountManagerResponse response, Account account, boolean expectActivityLaunch, int userId) {
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "removeAccount: " + account + ", response " + response + ", caller's uid " + Binder.getCallingUid() + ", pid " + Binder.getCallingPid() + ", for user id " + userId);
        }
        if (response == null) {
            throw new IllegalArgumentException("response is null");
        }
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        enforceCrossUserPermission(userId, "User " + UserHandle.getCallingUserId() + " trying to remove account for " + userId);
        checkManageAccountsPermission();
        UserAccounts accounts = getUserAccounts(userId);
        if (!canUserModifyAccounts(userId)) {
            try {
                response.onError(100, "User cannot modify accounts");
                return;
            } catch (RemoteException e) {
                return;
            }
        }
        if (!canUserModifyAccountsForType(userId, account.type)) {
            try {
                response.onError(101, "User cannot modify accounts of this type (policy).");
                return;
            } catch (RemoteException e2) {
                return;
            }
        }
        UserHandle user = new UserHandle(userId);
        long identityToken = clearCallingIdentity();
        cancelNotification(getSigninRequiredNotificationId(accounts, account).intValue(), user);
        synchronized (accounts.credentialsPermissionNotificationIds) {
            for (Pair<Pair<Account, String>, Integer> pair : accounts.credentialsPermissionNotificationIds.keySet()) {
                if (account.equals(((Pair) pair.first).first)) {
                    int id = ((Integer) accounts.credentialsPermissionNotificationIds.get(pair)).intValue();
                    cancelNotification(id, user);
                }
            }
        }
        try {
            new RemoveAccountSession(accounts, response, account, expectActivityLaunch).bind();
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public boolean removeAccountExplicitly(Account account) {
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "removeAccountExplicitly: " + account + ", caller's uid " + Binder.getCallingUid() + ", pid " + Binder.getCallingPid());
        }
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        checkAuthenticateAccountsPermission(account);
        UserAccounts accounts = getUserAccountsForCaller();
        int userId = Binder.getCallingUserHandle().getIdentifier();
        if (!canUserModifyAccounts(userId) || !canUserModifyAccountsForType(userId, account.type)) {
            return false;
        }
        long identityToken = clearCallingIdentity();
        try {
            return removeAccountInternal(accounts, account);
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    private class RemoveAccountSession extends Session {
        final Account mAccount;

        public RemoveAccountSession(UserAccounts accounts, IAccountManagerResponse response, Account account, boolean expectActivityLaunch) {
            super(accounts, response, account.type, expectActivityLaunch, true);
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
            if (result != null && result.containsKey("booleanResult") && !result.containsKey("intent")) {
                boolean removalAllowed = result.getBoolean("booleanResult");
                if (removalAllowed) {
                    AccountManagerService.this.removeAccountInternal(this.mAccounts, this.mAccount);
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
        removeAccountInternal(getUserAccountsForCaller(), account);
    }

    private boolean removeAccountInternal(UserAccounts accounts, Account account) {
        int deleted;
        synchronized (accounts.cacheLock) {
            SQLiteDatabase db = accounts.openHelper.getWritableDatabase();
            deleted = db.delete(TABLE_ACCOUNTS, "name=? AND type=?", new String[]{account.name, account.type});
            removeAccountFromCacheLocked(accounts, account);
            sendAccountsChangedBroadcast(accounts.userId);
        }
        if (accounts.userId == 0) {
            long id = Binder.clearCallingIdentity();
            try {
                List<UserInfo> users = this.mUserManager.getUsers(true);
                for (UserInfo user : users) {
                    if (!user.isPrimary() && user.isRestricted()) {
                        removeSharedAccountAsUser(account, user.id);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(id);
            }
        }
        return deleted > 0;
    }

    public void invalidateAuthToken(String accountType, String authToken) {
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "invalidateAuthToken: accountType " + accountType + ", caller's uid " + Binder.getCallingUid() + ", pid " + Binder.getCallingPid());
        }
        if (accountType == null) {
            throw new IllegalArgumentException("accountType is null");
        }
        if (authToken == null) {
            throw new IllegalArgumentException("authToken is null");
        }
        checkManageAccountsOrUseCredentialsPermissions();
        UserAccounts accounts = getUserAccountsForCaller();
        long identityToken = clearCallingIdentity();
        try {
            synchronized (accounts.cacheLock) {
                SQLiteDatabase db = accounts.openHelper.getWritableDatabase();
                db.beginTransaction();
                try {
                    invalidateAuthTokenLocked(accounts, db, accountType, authToken);
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    private void invalidateAuthTokenLocked(UserAccounts accounts, SQLiteDatabase db, String accountType, String authToken) {
        if (authToken != null && accountType != null) {
            Cursor cursor = db.rawQuery("SELECT authtokens._id, accounts.name, authtokens.type FROM accounts JOIN authtokens ON accounts._id = accounts_id WHERE authtoken = ? AND accounts.type = ?", new String[]{authToken, accountType});
            while (cursor.moveToNext()) {
                try {
                    long authTokenId = cursor.getLong(0);
                    String accountName = cursor.getString(1);
                    String authTokenType = cursor.getString(2);
                    db.delete(TABLE_AUTHTOKENS, "_id=" + authTokenId, null);
                    writeAuthTokenIntoCacheLocked(accounts, db, new Account(accountName, accountType), authTokenType, null);
                } finally {
                    cursor.close();
                }
            }
        }
    }

    private boolean saveAuthTokenToDatabase(UserAccounts accounts, Account account, String type, String authToken) {
        if (account == null || type == null) {
            return false;
        }
        cancelNotification(getSigninRequiredNotificationId(accounts, account).intValue(), new UserHandle(accounts.userId));
        synchronized (accounts.cacheLock) {
            SQLiteDatabase db = accounts.openHelper.getWritableDatabase();
            db.beginTransaction();
            try {
                long accountId = getAccountIdLocked(db, account);
                if (accountId < 0) {
                    return false;
                }
                db.delete(TABLE_AUTHTOKENS, "accounts_id=" + accountId + " AND " + DatabaseHelper.SoundModelContract.KEY_TYPE + "=?", new String[]{type});
                ContentValues values = new ContentValues();
                values.put("accounts_id", Long.valueOf(accountId));
                values.put(DatabaseHelper.SoundModelContract.KEY_TYPE, type);
                values.put(AUTHTOKENS_AUTHTOKEN, authToken);
                if (db.insert(TABLE_AUTHTOKENS, AUTHTOKENS_AUTHTOKEN, values) < 0) {
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
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "peekAuthToken: " + account + ", authTokenType " + authTokenType + ", caller's uid " + Binder.getCallingUid() + ", pid " + Binder.getCallingPid());
        }
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        if (authTokenType == null) {
            throw new IllegalArgumentException("authTokenType is null");
        }
        checkAuthenticateAccountsPermission(account);
        UserAccounts accounts = getUserAccountsForCaller();
        long identityToken = clearCallingIdentity();
        try {
            return readAuthTokenInternal(accounts, account, authTokenType);
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public void setAuthToken(Account account, String authTokenType, String authToken) {
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "setAuthToken: " + account + ", authTokenType " + authTokenType + ", caller's uid " + Binder.getCallingUid() + ", pid " + Binder.getCallingPid());
        }
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        if (authTokenType == null) {
            throw new IllegalArgumentException("authTokenType is null");
        }
        checkAuthenticateAccountsPermission(account);
        UserAccounts accounts = getUserAccountsForCaller();
        long identityToken = clearCallingIdentity();
        try {
            saveAuthTokenToDatabase(accounts, account, authTokenType, authToken);
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public void setPassword(Account account, String password) {
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "setAuthToken: " + account + ", caller's uid " + Binder.getCallingUid() + ", pid " + Binder.getCallingPid());
        }
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        checkAuthenticateAccountsPermission(account);
        UserAccounts accounts = getUserAccountsForCaller();
        long identityToken = clearCallingIdentity();
        try {
            setPasswordInternal(accounts, account, password);
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    private void setPasswordInternal(UserAccounts accounts, Account account, String password) {
        if (account != null) {
            synchronized (accounts.cacheLock) {
                SQLiteDatabase db = accounts.openHelper.getWritableDatabase();
                db.beginTransaction();
                try {
                    ContentValues values = new ContentValues();
                    values.put(ACCOUNTS_PASSWORD, password);
                    long accountId = getAccountIdLocked(db, account);
                    if (accountId >= 0) {
                        String[] argsAccountId = {String.valueOf(accountId)};
                        db.update(TABLE_ACCOUNTS, values, "_id=?", argsAccountId);
                        db.delete(TABLE_AUTHTOKENS, "accounts_id=?", argsAccountId);
                        accounts.authTokenCache.remove(account);
                        db.setTransactionSuccessful();
                    }
                    db.endTransaction();
                    sendAccountsChangedBroadcast(accounts.userId);
                } catch (Throwable th) {
                    db.endTransaction();
                    throw th;
                }
            }
        }
    }

    private void sendAccountsChangedBroadcast(int userId) {
        Log.i(TAG, "the accounts changed, sending broadcast of " + ACCOUNTS_CHANGED_INTENT.getAction());
        this.mContext.sendBroadcastAsUser(ACCOUNTS_CHANGED_INTENT, new UserHandle(userId));
    }

    public void clearPassword(Account account) {
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "clearPassword: " + account + ", caller's uid " + Binder.getCallingUid() + ", pid " + Binder.getCallingPid());
        }
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        checkManageAccountsPermission();
        UserAccounts accounts = getUserAccountsForCaller();
        long identityToken = clearCallingIdentity();
        try {
            setPasswordInternal(accounts, account, null);
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public void setUserData(Account account, String key, String value) {
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "setUserData: " + account + ", key " + key + ", caller's uid " + Binder.getCallingUid() + ", pid " + Binder.getCallingPid());
        }
        if (key == null) {
            throw new IllegalArgumentException("key is null");
        }
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        checkAuthenticateAccountsPermission(account);
        UserAccounts accounts = getUserAccountsForCaller();
        long identityToken = clearCallingIdentity();
        try {
            setUserdataInternal(accounts, account, key, value);
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    private void setUserdataInternal(UserAccounts accounts, Account account, String key, String value) {
        if (account == null || key == null) {
            return;
        }
        synchronized (accounts.cacheLock) {
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
            if (Log.isLoggable(TAG, 2)) {
                Log.v(TAG, "failure while notifying response", e);
            }
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
        UserAccounts accounts = getUserAccounts(UserHandle.getUserId(callingUid));
        long identityToken = clearCallingIdentity();
        try {
            new Session(accounts, response, accountType, false, false) {
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

    public void getAuthToken(IAccountManagerResponse response, final Account account, final String authTokenType, final boolean notifyOnAuthFailure, boolean expectActivityLaunch, Bundle loginOptionsIn) {
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
            } else if (authTokenType == null) {
                Slog.w(TAG, "getAuthToken called with null authTokenType");
                response.onError(7, "authTokenType is null");
            } else {
                checkBinderPermission("android.permission.USE_CREDENTIALS");
                final UserAccounts accounts = getUserAccountsForCaller();
                RegisteredServicesCache.ServiceInfo<AuthenticatorDescription> authenticatorInfo = this.mAuthenticatorCache.getServiceInfo(AuthenticatorDescription.newKey(account.type), accounts.userId);
                final boolean customTokens = authenticatorInfo != null && ((AuthenticatorDescription) authenticatorInfo.type).customTokens;
                final int callerUid = Binder.getCallingUid();
                final boolean permissionGranted = customTokens || permissionIsGranted(account, authTokenType, callerUid);
                final Bundle loginOptions = loginOptionsIn == null ? new Bundle() : loginOptionsIn;
                loginOptions.putInt("callerUid", callerUid);
                loginOptions.putInt("callerPid", Binder.getCallingPid());
                if (notifyOnAuthFailure) {
                    loginOptions.putBoolean("notifyOnAuthFailure", true);
                }
                long identityToken = clearCallingIdentity();
                if (!customTokens && permissionGranted) {
                    try {
                        String authToken = readAuthTokenInternal(accounts, account, authTokenType);
                        if (authToken != null) {
                            Bundle result = new Bundle();
                            result.putString(AUTHTOKENS_AUTHTOKEN, authToken);
                            result.putString("authAccount", account.name);
                            result.putString("accountType", account.type);
                            onResult(response, result);
                        }
                    } finally {
                        restoreCallingIdentity(identityToken);
                    }
                } else {
                    new Session(accounts, response, account.type, expectActivityLaunch, false) {
                        @Override
                        protected String toDebugString(long now) {
                            if (loginOptions != null) {
                                loginOptions.keySet();
                            }
                            return super.toDebugString(now) + ", getAuthToken, " + account + ", authTokenType " + authTokenType + ", loginOptions " + loginOptions + ", notifyOnAuthFailure " + notifyOnAuthFailure;
                        }

                        @Override
                        public void run() throws RemoteException {
                            if (!permissionGranted) {
                                this.mAuthenticator.getAuthTokenLabel(this, authTokenType);
                            } else {
                                this.mAuthenticator.getAuthToken(this, account, authTokenType, loginOptions);
                            }
                        }

                        @Override
                        public void onResult(Bundle result2) {
                            if (result2 != null) {
                                if (result2.containsKey("authTokenLabelKey")) {
                                    Intent intent = AccountManagerService.this.newGrantCredentialsPermissionIntent(account, callerUid, new AccountAuthenticatorResponse((IAccountAuthenticatorResponse) this), authTokenType, result2.getString("authTokenLabelKey"));
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
                                    } else if (!customTokens) {
                                        AccountManagerService.this.saveAuthTokenToDatabase(this.mAccounts, new Account(name, type), authTokenType, authToken2);
                                    }
                                }
                                Intent intent2 = (Intent) result2.getParcelable("intent");
                                if (intent2 != null && notifyOnAuthFailure && !customTokens) {
                                    intent2.setFlags(intent2.getFlags() & (-196));
                                    AccountManagerService.this.doNotification(this.mAccounts, account, result2.getString("authFailedMessage"), intent2, accounts.userId);
                                }
                            }
                            super.onResult(result2);
                        }
                    }.bind();
                    restoreCallingIdentity(identityToken);
                }
            }
        } catch (RemoteException e) {
            Slog.w(TAG, "Failed to report error back to the client." + e);
        }
    }

    private void createNoCredentialsPermissionNotification(Account account, Intent intent, int userId) {
        int uid = intent.getIntExtra(GRANTS_GRANTEE_UID, -1);
        String authTokenType = intent.getStringExtra("authTokenType");
        intent.getStringExtra("authTokenLabel");
        Notification n = new Notification(R.drawable.stat_sys_warning, null, 0L);
        String titleAndSubtitle = this.mContext.getString(R.string.keyguard_accessibility_sim_puk_unlock, account.name);
        int index = titleAndSubtitle.indexOf(10);
        CharSequence title = titleAndSubtitle;
        CharSequence subtitle = "";
        if (index > 0) {
            title = titleAndSubtitle.substring(0, index);
            subtitle = titleAndSubtitle.substring(index + 1);
        }
        UserHandle user = new UserHandle(userId);
        Context contextForUser = getContextForUser(user);
        n.color = contextForUser.getResources().getColor(R.color.system_accent3_600);
        n.setLatestEventInfo(contextForUser, title, subtitle, PendingIntent.getActivityAsUser(this.mContext, 0, intent, 268435456, null, user));
        installNotification(getCredentialPermissionNotificationId(account, authTokenType, uid).intValue(), n, user);
    }

    private Intent newGrantCredentialsPermissionIntent(Account account, int uid, AccountAuthenticatorResponse response, String authTokenType, String authTokenLabel) {
        Intent intent = new Intent(this.mContext, (Class<?>) GrantCredentialsPermissionActivity.class);
        intent.setFlags(268435456);
        intent.addCategory(String.valueOf(getCredentialPermissionNotificationId(account, authTokenType, uid)));
        intent.putExtra("account", account);
        intent.putExtra("authTokenType", authTokenType);
        intent.putExtra("response", response);
        intent.putExtra(GRANTS_GRANTEE_UID, uid);
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
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "addAccount: accountType " + accountType + ", response " + response + ", authTokenType " + authTokenType + ", requiredFeatures " + stringArrayToString(requiredFeatures) + ", expectActivityLaunch " + expectActivityLaunch + ", caller's uid " + Binder.getCallingUid() + ", pid " + Binder.getCallingPid());
        }
        if (response == null) {
            throw new IllegalArgumentException("response is null");
        }
        if (accountType == null) {
            throw new IllegalArgumentException("accountType is null");
        }
        checkManageAccountsPermission();
        int userId = Binder.getCallingUserHandle().getIdentifier();
        if (!canUserModifyAccounts(userId)) {
            try {
                response.onError(100, "User is not allowed to add an account!");
            } catch (RemoteException e) {
            }
            showCantAddAccount(100, userId);
            return;
        }
        if (!canUserModifyAccountsForType(userId, accountType)) {
            try {
                response.onError(101, "User cannot modify accounts of this type (policy).");
            } catch (RemoteException e2) {
            }
            showCantAddAccount(101, userId);
            return;
        }
        UserAccounts accounts = getUserAccountsForCaller();
        int pid = Binder.getCallingPid();
        int uid = Binder.getCallingUid();
        final Bundle options = optionsIn == null ? new Bundle() : optionsIn;
        options.putInt("callerUid", uid);
        options.putInt("callerPid", pid);
        long identityToken = clearCallingIdentity();
        try {
            new Session(accounts, response, accountType, expectActivityLaunch, true) {
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
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "addAccount: accountType " + accountType + ", response " + response + ", authTokenType " + authTokenType + ", requiredFeatures " + stringArrayToString(requiredFeatures) + ", expectActivityLaunch " + expectActivityLaunch + ", caller's uid " + Binder.getCallingUid() + ", pid " + Binder.getCallingPid() + ", for user id " + userId);
        }
        if (response == null) {
            throw new IllegalArgumentException("response is null");
        }
        if (accountType == null) {
            throw new IllegalArgumentException("accountType is null");
        }
        checkManageAccountsPermission();
        enforceCrossUserPermission(userId, "User " + UserHandle.getCallingUserId() + " trying to add account for " + userId);
        if (!canUserModifyAccounts(userId)) {
            try {
                response.onError(100, "User is not allowed to add an account!");
            } catch (RemoteException e) {
            }
            showCantAddAccount(100, userId);
            return;
        }
        if (!canUserModifyAccountsForType(userId, accountType)) {
            try {
                response.onError(101, "User cannot modify accounts of this type (policy).");
            } catch (RemoteException e2) {
            }
            showCantAddAccount(101, userId);
            return;
        }
        UserAccounts accounts = getUserAccounts(userId);
        int pid = Binder.getCallingPid();
        int uid = Binder.getCallingUid();
        final Bundle options = optionsIn == null ? new Bundle() : optionsIn;
        options.putInt("callerUid", uid);
        options.putInt("callerPid", pid);
        long identityToken = clearCallingIdentity();
        try {
            new Session(accounts, response, accountType, expectActivityLaunch, true) {
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
        enforceCrossUserPermission(userId, "User " + UserHandle.getCallingUserId() + " trying to confirm account credentials for " + userId);
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "confirmCredentials: " + account + ", response " + response + ", expectActivityLaunch " + expectActivityLaunch + ", caller's uid " + Binder.getCallingUid() + ", pid " + Binder.getCallingPid());
        }
        if (response == null) {
            throw new IllegalArgumentException("response is null");
        }
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        checkManageAccountsPermission();
        UserAccounts accounts = getUserAccounts(userId);
        long identityToken = clearCallingIdentity();
        try {
            new Session(accounts, response, account.type, expectActivityLaunch, true) {
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
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "updateCredentials: " + account + ", response " + response + ", authTokenType " + authTokenType + ", expectActivityLaunch " + expectActivityLaunch + ", caller's uid " + Binder.getCallingUid() + ", pid " + Binder.getCallingPid());
        }
        if (response == null) {
            throw new IllegalArgumentException("response is null");
        }
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        if (authTokenType == null) {
            throw new IllegalArgumentException("authTokenType is null");
        }
        checkManageAccountsPermission();
        UserAccounts accounts = getUserAccountsForCaller();
        long identityToken = clearCallingIdentity();
        try {
            new Session(accounts, response, account.type, expectActivityLaunch, true) {
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

    public void editProperties(IAccountManagerResponse response, final String accountType, boolean expectActivityLaunch) {
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "editProperties: accountType " + accountType + ", response " + response + ", expectActivityLaunch " + expectActivityLaunch + ", caller's uid " + Binder.getCallingUid() + ", pid " + Binder.getCallingPid());
        }
        if (response == null) {
            throw new IllegalArgumentException("response is null");
        }
        if (accountType == null) {
            throw new IllegalArgumentException("accountType is null");
        }
        checkManageAccountsPermission();
        UserAccounts accounts = getUserAccountsForCaller();
        long identityToken = clearCallingIdentity();
        try {
            new Session(accounts, response, accountType, expectActivityLaunch, true) {
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

    private class GetAccountsByTypeAndFeatureSession extends Session {
        private volatile Account[] mAccountsOfType;
        private volatile ArrayList<Account> mAccountsWithFeatures;
        private final int mCallingUid;
        private volatile int mCurrentAccount;
        private final String[] mFeatures;

        public GetAccountsByTypeAndFeatureSession(UserAccounts accounts, IAccountManagerResponse response, String type, String[] features, int callingUid) {
            super(accounts, response, type, false, true);
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
            if (response != null) {
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
                    if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                        Log.v(AccountManagerService.TAG, "failure while notifying response", e);
                    }
                }
            }
        }

        @Override
        protected String toDebugString(long now) {
            return super.toDebugString(now) + ", getAccountsByTypeAndFeatures, " + (this.mFeatures != null ? TextUtils.join(",", this.mFeatures) : null);
        }
    }

    public Account[] getAccounts(int userId) {
        Account[] accountsFromCacheLocked;
        checkReadAccountsPermission();
        UserAccounts accounts = getUserAccounts(userId);
        int callingUid = Binder.getCallingUid();
        long identityToken = clearCallingIdentity();
        try {
            synchronized (accounts.cacheLock) {
                accountsFromCacheLocked = getAccountsFromCacheLocked(accounts, null, callingUid, null);
            }
            return accountsFromCacheLocked;
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
        List<UserInfo> users = getUserManager().getUsers();
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

    public Account[] getAccountsAsUser(String type, int userId) {
        return getAccountsAsUser(type, userId, null, -1);
    }

    private Account[] getAccountsAsUser(String type, int userId, String callingPackage, int packageUid) {
        Account[] accountsFromCacheLocked;
        int callingUid = Binder.getCallingUid();
        if (userId != UserHandle.getCallingUserId() && callingUid != Process.myUid() && this.mContext.checkCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL") != 0) {
            throw new SecurityException("User " + UserHandle.getCallingUserId() + " trying to get account for " + userId);
        }
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "getAccounts: accountType " + type + ", caller's uid " + Binder.getCallingUid() + ", pid " + Binder.getCallingPid());
        }
        if (packageUid != -1 && UserHandle.isSameApp(callingUid, Process.myUid())) {
            callingUid = packageUid;
        }
        checkReadAccountsPermission();
        UserAccounts accounts = getUserAccounts(userId);
        long identityToken = clearCallingIdentity();
        try {
            synchronized (accounts.cacheLock) {
                accountsFromCacheLocked = getAccountsFromCacheLocked(accounts, type, callingUid, callingPackage);
            }
            return accountsFromCacheLocked;
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public boolean addSharedAccountAsUser(Account account, int userId) {
        SQLiteDatabase db = getUserAccounts(handleIncomingUser(userId)).openHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(ACCOUNTS_NAME, account.name);
        values.put(DatabaseHelper.SoundModelContract.KEY_TYPE, account.type);
        db.delete(TABLE_SHARED_ACCOUNTS, "name=? AND type=?", new String[]{account.name, account.type});
        long accountId = db.insert(TABLE_SHARED_ACCOUNTS, ACCOUNTS_NAME, values);
        if (accountId >= 0) {
            return true;
        }
        Log.w(TAG, "insertAccountIntoDatabase: " + account + ", skipping the DB insert failed");
        return false;
    }

    public boolean renameSharedAccountAsUser(Account account, String newName, int userId) {
        UserAccounts accounts = getUserAccounts(handleIncomingUser(userId));
        SQLiteDatabase db = accounts.openHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(ACCOUNTS_NAME, newName);
        values.put(ACCOUNTS_PREVIOUS_NAME, account.name);
        int r = db.update(TABLE_SHARED_ACCOUNTS, values, "name=? AND type=?", new String[]{account.name, account.type});
        if (r > 0) {
            renameAccountInternal(accounts, account, newName);
        }
        return r > 0;
    }

    public boolean removeSharedAccountAsUser(Account account, int userId) {
        UserAccounts accounts = getUserAccounts(handleIncomingUser(userId));
        SQLiteDatabase db = accounts.openHelper.getWritableDatabase();
        int r = db.delete(TABLE_SHARED_ACCOUNTS, "name=? AND type=?", new String[]{account.name, account.type});
        if (r > 0) {
            removeAccountInternal(accounts, account);
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

    public Account[] getAccounts(String type) {
        return getAccountsAsUser(type, UserHandle.getCallingUserId());
    }

    public Account[] getAccountsForPackage(String packageName, int uid) {
        int callingUid = Binder.getCallingUid();
        if (!UserHandle.isSameApp(callingUid, Process.myUid())) {
            throw new SecurityException("getAccountsForPackage() called from unauthorized uid " + callingUid + " with uid=" + uid);
        }
        return getAccountsAsUser(null, UserHandle.getCallingUserId(), packageName, uid);
    }

    public Account[] getAccountsByTypeForPackage(String type, String packageName) {
        checkBinderPermission("android.permission.INTERACT_ACROSS_USERS");
        try {
            int packageUid = AppGlobals.getPackageManager().getPackageUid(packageName, UserHandle.getCallingUserId());
            return getAccountsAsUser(type, UserHandle.getCallingUserId(), packageName, packageUid);
        } catch (RemoteException re) {
            Slog.e(TAG, "Couldn't determine the packageUid for " + packageName + re);
            return new Account[0];
        }
    }

    public void getAccountsByFeatures(IAccountManagerResponse response, String type, String[] features) {
        Account[] accounts;
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "getAccounts: accountType " + type + ", response " + response + ", features " + stringArrayToString(features) + ", caller's uid " + Binder.getCallingUid() + ", pid " + Binder.getCallingPid());
        }
        if (response == null) {
            throw new IllegalArgumentException("response is null");
        }
        if (type == null) {
            throw new IllegalArgumentException("accountType is null");
        }
        checkReadAccountsPermission();
        UserAccounts userAccounts = getUserAccountsForCaller();
        int callingUid = Binder.getCallingUid();
        long identityToken = clearCallingIdentity();
        if (features != null) {
            try {
                if (features.length != 0) {
                    new GetAccountsByTypeAndFeatureSession(userAccounts, response, type, features, callingUid).bind();
                    return;
                }
            } finally {
                restoreCallingIdentity(identityToken);
            }
        }
        synchronized (userAccounts.cacheLock) {
            accounts = getAccountsFromCacheLocked(userAccounts, type, callingUid, null);
        }
        Bundle result = new Bundle();
        result.putParcelableArray(TABLE_ACCOUNTS, accounts);
        onResult(response, result);
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
        Cursor cursor = db.query(TABLE_EXTRAS, new String[]{"_id"}, "accounts_id=" + accountId + " AND key=?", new String[]{key}, null, null, null);
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
        final String mAccountType;
        protected final UserAccounts mAccounts;
        final long mCreationTime;
        final boolean mExpectActivityLaunch;
        IAccountManagerResponse mResponse;
        private final boolean mStripAuthTokenFromResult;
        public int mNumResults = 0;
        private int mNumRequestContinued = 0;
        private int mNumErrors = 0;
        IAccountAuthenticator mAuthenticator = null;

        public abstract void run() throws RemoteException;

        public Session(UserAccounts accounts, IAccountManagerResponse response, String accountType, boolean expectActivityLaunch, boolean stripAuthTokenFromResult) {
            if (accountType == null) {
                throw new IllegalArgumentException("accountType is null");
            }
            this.mAccounts = accounts;
            this.mStripAuthTokenFromResult = stripAuthTokenFromResult;
            this.mResponse = response;
            this.mAccountType = accountType;
            this.mExpectActivityLaunch = expectActivityLaunch;
            this.mCreationTime = SystemClock.elapsedRealtime();
            synchronized (AccountManagerService.this.mSessions) {
                AccountManagerService.this.mSessions.put(toString(), this);
            }
            if (response != null) {
                try {
                    response.asBinder().linkToDeath(this, 0);
                } catch (RemoteException e) {
                    this.mResponse = null;
                    binderDied();
                }
            }
        }

        IAccountManagerResponse getResponseAndClose() {
            if (this.mResponse == null) {
                return null;
            }
            IAccountManagerResponse iAccountManagerResponse = this.mResponse;
            close();
            return iAccountManagerResponse;
        }

        private void close() {
            synchronized (AccountManagerService.this.mSessions) {
                if (AccountManagerService.this.mSessions.remove(toString()) != null) {
                    if (this.mResponse != null) {
                        this.mResponse.asBinder().unlinkToDeath(this, 0);
                        this.mResponse = null;
                    }
                    cancelTimeout();
                    unbind();
                }
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
            if (!bindToAuthenticator(this.mAccountType)) {
                Log.d(AccountManagerService.TAG, "bind attempt failed for " + toDebugString());
                onError(1, "bind failure");
            }
        }

        private void unbind() {
            if (this.mAuthenticator != null) {
                this.mAuthenticator = null;
                AccountManagerService.this.mContext.unbindService(this);
            }
        }

        public void scheduleTimeout() {
            AccountManagerService.this.mMessageHandler.sendMessageDelayed(AccountManagerService.this.mMessageHandler.obtainMessage(3, this), 60000L);
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
            if (response != null) {
                try {
                    response.onError(1, "disconnected");
                } catch (RemoteException e) {
                    if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                        Log.v(AccountManagerService.TAG, "Session.onServiceDisconnected: caught RemoteException while responding", e);
                    }
                }
            }
        }

        public void onTimedOut() {
            IAccountManagerResponse response = getResponseAndClose();
            if (response != null) {
                try {
                    response.onError(1, "timeout");
                } catch (RemoteException e) {
                    if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                        Log.v(AccountManagerService.TAG, "Session.onTimedOut: caught RemoteException while responding", e);
                    }
                }
            }
        }

        public void onResult(Bundle result) {
            IAccountManagerResponse response;
            this.mNumResults++;
            Intent intent = null;
            if (result != null && (intent = (Intent) result.getParcelable("intent")) != null) {
                intent.setFlags(intent.getFlags() & (-196));
                int authenticatorUid = Binder.getCallingUid();
                long bid = Binder.clearCallingIdentity();
                try {
                    PackageManager pm = AccountManagerService.this.mContext.getPackageManager();
                    ResolveInfo resolveInfo = pm.resolveActivityAsUser(intent, 0, this.mAccounts.userId);
                    int targetUid = resolveInfo.activityInfo.applicationInfo.uid;
                    if (pm.checkSignatures(authenticatorUid, targetUid) != 0) {
                        throw new SecurityException("Activity to be started with KEY_INTENT must share Authenticator's signatures");
                    }
                } finally {
                    Binder.restoreCallingIdentity(bid);
                }
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
            if (response != null) {
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
                    if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                        Log.v(AccountManagerService.TAG, "failure while notifying response", e);
                    }
                }
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
                    if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                        Log.v(AccountManagerService.TAG, "Session.onError: caught RemoteException while responding", e);
                        return;
                    }
                    return;
                }
            }
            if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                Log.v(AccountManagerService.TAG, "Session.onError: already closed");
            }
        }

        private boolean bindToAuthenticator(String authenticatorType) {
            RegisteredServicesCache.ServiceInfo<AuthenticatorDescription> authenticatorInfo = AccountManagerService.this.mAuthenticatorCache.getServiceInfo(AuthenticatorDescription.newKey(authenticatorType), this.mAccounts.userId);
            if (authenticatorInfo == null) {
                if (!Log.isLoggable(AccountManagerService.TAG, 2)) {
                    return false;
                }
                Log.v(AccountManagerService.TAG, "there is no authenticator for " + authenticatorType + ", bailing out");
                return false;
            }
            Intent intent = new Intent();
            intent.setAction("android.accounts.AccountAuthenticator");
            intent.setComponent(authenticatorInfo.componentName);
            if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                Log.v(AccountManagerService.TAG, "performing bindService to " + authenticatorInfo.componentName);
            }
            if (AccountManagerService.this.mContext.bindServiceAsUser(intent, this, 1, new UserHandle(this.mAccounts.userId))) {
                return true;
            }
            if (!Log.isLoggable(AccountManagerService.TAG, 2)) {
                return false;
            }
            Log.v(AccountManagerService.TAG, "bindService to " + authenticatorInfo.componentName + " failed");
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

    private static String getDatabaseName(int userId) {
        File systemDir = Environment.getSystemSecureDirectory();
        File databaseFile = new File(Environment.getUserSystemDirectory(userId), DATABASE_NAME);
        if (userId == 0) {
            File oldFile = new File(systemDir, DATABASE_NAME);
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

    static class DatabaseHelper extends SQLiteOpenHelper {
        public DatabaseHelper(Context context, int userId) {
            super(context, AccountManagerService.getDatabaseName(userId), (SQLiteDatabase.CursorFactory) null, 6);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE accounts ( _id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, type TEXT NOT NULL, password TEXT, previous_name TEXT, UNIQUE(name,type))");
            db.execSQL("CREATE TABLE authtokens (  _id INTEGER PRIMARY KEY AUTOINCREMENT,  accounts_id INTEGER NOT NULL, type TEXT NOT NULL,  authtoken TEXT,  UNIQUE (accounts_id,type))");
            createGrantsTable(db);
            db.execSQL("CREATE TABLE extras ( _id INTEGER PRIMARY KEY AUTOINCREMENT, accounts_id INTEGER, key TEXT NOT NULL, value TEXT, UNIQUE(accounts_id,key))");
            db.execSQL("CREATE TABLE meta ( key TEXT PRIMARY KEY NOT NULL, value TEXT)");
            createSharedAccountsTable(db);
            createAccountsDeletionTrigger(db);
        }

        private void createSharedAccountsTable(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE shared_accounts ( _id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, type TEXT NOT NULL, UNIQUE(name,type))");
        }

        private void addOldAccountNameColumn(SQLiteDatabase db) {
            db.execSQL("ALTER TABLE accounts ADD COLUMN previous_name");
        }

        private void createAccountsDeletionTrigger(SQLiteDatabase db) {
            db.execSQL(" CREATE TRIGGER accountsDelete DELETE ON accounts BEGIN   DELETE FROM authtokens     WHERE accounts_id=OLD._id ;   DELETE FROM extras     WHERE accounts_id=OLD._id ;   DELETE FROM grants     WHERE accounts_id=OLD._id ; END");
        }

        private void createGrantsTable(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE grants (  accounts_id INTEGER NOT NULL, auth_token_type STRING NOT NULL,  uid INTEGER NOT NULL,  UNIQUE (accounts_id,auth_token_type,uid))");
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
            if (oldVersion != newVersion) {
                Log.e(AccountManagerService.TAG, "failed to upgrade version " + oldVersion + " to version " + newVersion);
            }
        }

        @Override
        public void onOpen(SQLiteDatabase db) {
            if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                Log.v(AccountManagerService.TAG, "opened database accounts.db");
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
        boolean isCheckinRequest = scanArgs(args, "--checkin") || scanArgs(args, "-c");
        IndentingPrintWriter ipw = new IndentingPrintWriter(fout, "  ");
        List<UserInfo> users = getUserManager().getUsers();
        for (UserInfo user : users) {
            ipw.println("User " + user + ":");
            ipw.increaseIndent();
            dumpUser(getUserAccounts(user.id), fd, ipw, args, isCheckinRequest);
            ipw.println();
            ipw.decreaseIndent();
        }
    }

    private void dumpUser(UserAccounts userAccounts, FileDescriptor fd, PrintWriter fout, String[] args, boolean isCheckinRequest) {
        synchronized (userAccounts.cacheLock) {
            SQLiteDatabase db = userAccounts.openHelper.getReadableDatabase();
            if (isCheckinRequest) {
                Cursor cursor = db.query(TABLE_ACCOUNTS, ACCOUNT_TYPE_COUNT_PROJECTION, null, null, DatabaseHelper.SoundModelContract.KEY_TYPE, null, null);
                while (cursor.moveToNext()) {
                    try {
                        fout.println(cursor.getString(0) + "," + cursor.getString(1));
                    } finally {
                        if (cursor != null) {
                            cursor.close();
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
                Log.v(TAG, "doNotification: " + ((Object) message) + " intent:" + intent);
            }
            if (intent.getComponent() != null && GrantCredentialsPermissionActivity.class.getName().equals(intent.getComponent().getClassName())) {
                createNoCredentialsPermissionNotification(account, intent, userId);
            } else {
                Integer notificationId = getSigninRequiredNotificationId(accounts, account);
                intent.addCategory(String.valueOf(notificationId));
                Notification n = new Notification(R.drawable.stat_sys_warning, null, 0L);
                UserHandle user = new UserHandle(userId);
                Context contextForUser = getContextForUser(user);
                String notificationTitleFormat = contextForUser.getText(R.string.accessibility_autoclick_scroll_left).toString();
                n.color = contextForUser.getResources().getColor(R.color.system_accent3_600);
                n.setLatestEventInfo(contextForUser, String.format(notificationTitleFormat, account.name), message, PendingIntent.getActivityAsUser(this.mContext, 0, intent, 268435456, null, user));
                installNotification(notificationId.intValue(), n, user);
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

    private void checkBinderPermission(String... permissions) {
        int uid = Binder.getCallingUid();
        for (String perm : permissions) {
            if (this.mContext.checkCallingOrSelfPermission(perm) == 0) {
                if (Log.isLoggable(TAG, 2)) {
                    Log.v(TAG, "  caller uid " + uid + " has " + perm);
                    return;
                }
                return;
            }
        }
        String msg = "caller uid " + uid + " lacks any of " + TextUtils.join(",", permissions);
        Log.w(TAG, "  " + msg);
        throw new SecurityException(msg);
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
                    if (packageInfo != null && (packageInfo.applicationInfo.flags & 1073741824) != 0) {
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

    private boolean permissionIsGranted(Account account, String authTokenType, int callerUid) {
        boolean isPrivileged = isPrivileged(callerUid);
        boolean fromAuthenticator = account != null && hasAuthenticatorUid(account.type, callerUid);
        boolean hasExplicitGrants = account != null && hasExplicitlyGrantedPermission(account, authTokenType, callerUid);
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "checkGrantsOrCallingUidAgainstAuthenticator: caller uid " + callerUid + ", " + account + ": is authenticator? " + fromAuthenticator + ", has explicit permission? " + hasExplicitGrants);
        }
        return fromAuthenticator || hasExplicitGrants || isPrivileged;
    }

    private boolean hasAuthenticatorUid(String accountType, int callingUid) {
        int callingUserId = UserHandle.getUserId(callingUid);
        for (RegisteredServicesCache.ServiceInfo<AuthenticatorDescription> serviceInfo : this.mAuthenticatorCache.getAllServices(callingUserId)) {
            if (((AuthenticatorDescription) serviceInfo.type).type.equals(accountType)) {
                return serviceInfo.uid == callingUid || this.mPackageManager.checkSignatures(serviceInfo.uid, callingUid) == 0;
            }
        }
        return false;
    }

    private boolean hasExplicitlyGrantedPermission(Account account, String authTokenType, int callerUid) {
        boolean permissionGranted;
        if (callerUid == 1000) {
            return true;
        }
        UserAccounts accounts = getUserAccountsForCaller();
        synchronized (accounts.cacheLock) {
            SQLiteDatabase db = accounts.openHelper.getReadableDatabase();
            String[] args = {String.valueOf(callerUid), authTokenType, account.name, account.type};
            permissionGranted = DatabaseUtils.longForQuery(db, COUNT_OF_MATCHING_GRANTS, args) != 0;
            if (!permissionGranted && ActivityManager.isRunningInTestHarness()) {
                Log.d(TAG, "no credentials permission for usage of " + account + ", " + authTokenType + " by uid " + callerUid + " but ignoring since device is in test harness.");
                permissionGranted = true;
            }
        }
        return permissionGranted;
    }

    private void checkCallingUidAgainstAuthenticator(Account account) {
        int uid = Binder.getCallingUid();
        if (account == null || !hasAuthenticatorUid(account.type, uid)) {
            String msg = "caller uid " + uid + " is different than the authenticator's uid";
            Log.w(TAG, msg);
            throw new SecurityException(msg);
        }
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "caller uid " + uid + " is the same as the authenticator's uid");
        }
    }

    private void checkAuthenticateAccountsPermission(Account account) {
        checkBinderPermission("android.permission.AUTHENTICATE_ACCOUNTS");
        checkCallingUidAgainstAuthenticator(account);
    }

    private void checkReadAccountsPermission() {
        checkBinderPermission("android.permission.GET_ACCOUNTS");
    }

    private void checkManageAccountsPermission() {
        checkBinderPermission("android.permission.MANAGE_ACCOUNTS");
    }

    private void checkManageAccountsOrUseCredentialsPermissions() {
        checkBinderPermission("android.permission.MANAGE_ACCOUNTS", "android.permission.USE_CREDENTIALS");
    }

    private boolean canUserModifyAccounts(int userId) {
        return !getUserManager().getUserRestrictions(new UserHandle(userId)).getBoolean("no_modify_accounts");
    }

    private boolean canUserModifyAccountsForType(int userId, String accountType) {
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
                    values.put(GRANTS_GRANTEE_UID, Integer.valueOf(uid));
                    db.insert(TABLE_GRANTS, "accounts_id", values);
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
        if (getUserManager() != null && userAccounts != null && userAccounts.userId >= 0 && callingUid != Process.myUid() && (user = this.mUserManager.getUserInfo(userAccounts.userId)) != null && user.isRestricted()) {
            String[] packages = this.mPackageManager.getPackagesForUid(callingUid);
            String whiteList = this.mContext.getResources().getString(R.string.config_defaultNotes);
            for (String packageName : packages) {
                if (whiteList.contains(";" + packageName + ";")) {
                    return unfiltered;
                }
            }
            ArrayList<Account> allowed = new ArrayList<>();
            Account[] sharedAccounts = getSharedAccountsAsUser(userAccounts.userId);
            if (sharedAccounts != null && sharedAccounts.length != 0) {
                String requiredAccountType = "";
                try {
                    if (callingPackage != null) {
                        PackageInfo pi = this.mPackageManager.getPackageInfo(callingPackage, 0);
                        if (pi != null && pi.restrictedAccountType != null) {
                            requiredAccountType = pi.restrictedAccountType;
                        }
                    } else {
                        int len$ = packages.length;
                        int i$ = 0;
                        while (true) {
                            if (i$ < len$) {
                                String packageName2 = packages[i$];
                                PackageInfo pi2 = this.mPackageManager.getPackageInfo(packageName2, 0);
                                if (pi2 == null || pi2.restrictedAccountType == null) {
                                    i$++;
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
                for (Account account : unfiltered) {
                    if (account.type.equals(requiredAccountType)) {
                        allowed.add(account);
                    } else {
                        boolean found = false;
                        int len$2 = sharedAccounts.length;
                        int i$2 = 0;
                        while (true) {
                            if (i$2 >= len$2) {
                                break;
                            }
                            Account shared = sharedAccounts[i$2];
                            if (!shared.equals(account)) {
                                i$2++;
                            } else {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            allowed.add(account);
                        }
                    }
                }
                Account[] filtered = new Account[allowed.size()];
                allowed.toArray(filtered);
                return filtered;
            }
            return unfiltered;
        }
        return unfiltered;
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
        Iterator i$ = userAccounts.accountCache.values().iterator();
        while (i$.hasNext()) {
            totalLength += ((Account[]) i$.next()).length;
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
                SQLiteDatabase db = accounts.openHelper.getReadableDatabase();
                authTokensForAccount = readAuthTokensForAccountFromDatabaseLocked(db, account);
                accounts.authTokenCache.put(account, authTokensForAccount);
            }
            str = authTokensForAccount.get(authTokenType);
        }
        return str;
    }

    protected String readUserDataInternal(UserAccounts accounts, Account account, String key) {
        String str;
        synchronized (accounts.cacheLock) {
            HashMap<String, String> userDataForAccount = (HashMap) accounts.userDataCache.get(account);
            if (userDataForAccount == null) {
                SQLiteDatabase db = accounts.openHelper.getReadableDatabase();
                userDataForAccount = readUserDataForAccountFromDatabaseLocked(db, account);
                accounts.userDataCache.put(account, userDataForAccount);
            }
            str = userDataForAccount.get(key);
        }
        return str;
    }

    protected HashMap<String, String> readUserDataForAccountFromDatabaseLocked(SQLiteDatabase db, Account account) {
        HashMap<String, String> userDataForAccount = new HashMap<>();
        Cursor cursor = db.query(TABLE_EXTRAS, COLUMNS_EXTRAS_KEY_AND_VALUE, "accounts_id=(select _id FROM accounts WHERE name=? AND type=?)", new String[]{account.name, account.type}, null, null, null);
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
        Cursor cursor = db.query(TABLE_AUTHTOKENS, COLUMNS_AUTHTOKENS_TYPE_AND_AUTHTOKEN, "accounts_id=(select _id FROM accounts WHERE name=? AND type=?)", new String[]{account.name, account.type}, null, null, null);
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
}
