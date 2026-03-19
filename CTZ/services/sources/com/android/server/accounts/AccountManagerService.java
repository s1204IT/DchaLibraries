package com.android.server.accounts;

import android.R;
import android.accounts.Account;
import android.accounts.AccountAndUser;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManagerInternal;
import android.accounts.AccountManagerResponse;
import android.accounts.AuthenticatorDescription;
import android.accounts.CantAddAccountActivity;
import android.accounts.ChooseAccountActivity;
import android.accounts.GrantCredentialsPermissionActivity;
import android.accounts.IAccountAuthenticator;
import android.accounts.IAccountAuthenticatorResponse;
import android.accounts.IAccountManager;
import android.accounts.IAccountManagerResponse;
import android.app.ActivityManager;
import android.app.ActivityThread;
import android.app.AppOpsManager;
import android.app.INotificationManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.app.admin.DevicePolicyManagerInternal;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.RegisteredServicesCache;
import android.content.pm.RegisteredServicesCacheListener;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.content.pm.UserInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.os.StrictMode;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.content.PackageMonitor;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.Preconditions;
import com.android.server.LocalServices;
import com.android.server.ServiceThread;
import com.android.server.SystemService;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.net.watchlist.WatchlistLoggingHandler;
import com.android.server.pm.DumpState;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.Settings;
import com.android.server.slice.SliceClientPermissions;
import com.google.android.collect.Lists;
import com.google.android.collect.Sets;
import java.io.File;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

public class AccountManagerService extends IAccountManager.Stub implements RegisteredServicesCacheListener<AuthenticatorDescription> {
    private static final Intent ACCOUNTS_CHANGED_INTENT = new Intent("android.accounts.LOGIN_ACCOUNTS_CHANGED");
    private static final Account[] EMPTY_ACCOUNT_ARRAY;
    private static final int MESSAGE_COPY_SHARED_ACCOUNT = 4;
    private static final int MESSAGE_TIMED_OUT = 3;
    private static final String PRE_N_DATABASE_NAME = "accounts.db";
    private static final int SIGNATURE_CHECK_MATCH = 1;
    private static final int SIGNATURE_CHECK_MISMATCH = 0;
    private static final int SIGNATURE_CHECK_UID_MATCH = 2;
    private static final String TAG = "AccountManagerService";
    private static AtomicReference<AccountManagerService> sThis;
    private final AppOpsManager mAppOpsManager;
    private final IAccountAuthenticatorCache mAuthenticatorCache;
    final Context mContext;
    final MessageHandler mHandler;
    private final Injector mInjector;
    private final PackageManager mPackageManager;
    private UserManager mUserManager;
    private final LinkedHashMap<String, Session> mSessions = new LinkedHashMap<>();
    private final SparseArray<UserAccounts> mUsers = new SparseArray<>();
    private final SparseBooleanArray mLocalUnlockedUsers = new SparseBooleanArray();
    private final SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private CopyOnWriteArrayList<AccountManagerInternal.OnAppPermissionChangeListener> mAppPermissionChangeListeners = new CopyOnWriteArrayList<>();

    public static class Lifecycle extends SystemService {
        private AccountManagerService mService;

        public Lifecycle(Context context) {
            super(context);
        }

        @Override
        public void onStart() {
            this.mService = new AccountManagerService(new Injector(getContext()));
            publishBinderService("account", this.mService);
        }

        @Override
        public void onUnlockUser(int i) {
            this.mService.onUnlockUser(i);
        }

        @Override
        public void onStopUser(int i) {
            Slog.i(AccountManagerService.TAG, "onStopUser " + i);
            this.mService.purgeUserData(i);
        }
    }

    static {
        ACCOUNTS_CHANGED_INTENT.setFlags(83886080);
        sThis = new AtomicReference<>();
        EMPTY_ACCOUNT_ARRAY = new Account[0];
    }

    static class UserAccounts {
        final AccountsDb accountsDb;
        private SQLiteStatement statementForLogging;
        private final int userId;
        private final HashMap<Pair<Pair<Account, String>, Integer>, NotificationId> credentialsPermissionNotificationIds = new HashMap<>();
        private final HashMap<Account, NotificationId> signinRequiredNotificationIds = new HashMap<>();
        final Object cacheLock = new Object();
        final Object dbLock = new Object();
        final HashMap<String, Account[]> accountCache = new LinkedHashMap();
        private final Map<Account, Map<String, String>> userDataCache = new HashMap();
        private final Map<Account, Map<String, String>> authTokenCache = new HashMap();
        private final TokenCache accountTokenCaches = new TokenCache();
        private final Map<Account, Map<String, Integer>> visibilityCache = new HashMap();
        private final Map<String, Map<String, Integer>> mReceiversForType = new HashMap();
        private final HashMap<Account, AtomicReference<String>> previousNameCache = new HashMap<>();
        private int debugDbInsertionPoint = -1;

        UserAccounts(Context context, int i, File file, File file2) {
            this.userId = i;
            synchronized (this.dbLock) {
                synchronized (this.cacheLock) {
                    this.accountsDb = AccountsDb.create(context, i, file, file2);
                }
            }
        }
    }

    public static AccountManagerService getSingleton() {
        return sThis.get();
    }

    public AccountManagerService(Injector injector) {
        this.mInjector = injector;
        this.mContext = injector.getContext();
        this.mPackageManager = this.mContext.getPackageManager();
        this.mAppOpsManager = (AppOpsManager) this.mContext.getSystemService(AppOpsManager.class);
        this.mHandler = new MessageHandler(injector.getMessageHandlerLooper());
        this.mAuthenticatorCache = this.mInjector.getAccountAuthenticatorCache();
        this.mAuthenticatorCache.setListener(this, null);
        sThis.set(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        intentFilter.addDataScheme(Settings.ATTR_PACKAGE);
        this.mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!intent.getBooleanExtra("android.intent.extra.REPLACING", false)) {
                    final String schemeSpecificPart = intent.getData().getSchemeSpecificPart();
                    AccountManagerService.this.mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            AccountManagerService.this.purgeOldGrantsAll();
                            AccountManagerService.this.removeVisibilityValuesForPackage(schemeSpecificPart);
                        }
                    });
                }
            }
        }, intentFilter);
        injector.addLocalService(new AccountManagerInternalImpl());
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("android.intent.action.USER_REMOVED");
        this.mContext.registerReceiverAsUser(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int intExtra;
                if (!"android.intent.action.USER_REMOVED".equals(intent.getAction()) || (intExtra = intent.getIntExtra("android.intent.extra.user_handle", -1)) < 1) {
                    return;
                }
                Slog.i(AccountManagerService.TAG, "User " + intExtra + " removed");
                AccountManagerService.this.purgeUserData(intExtra);
            }
        }, UserHandle.ALL, intentFilter2, null, null);
        new PackageMonitor() {
            public void onPackageAdded(String str, int i) {
                AccountManagerService.this.cancelAccountAccessRequestNotificationIfNeeded(i, true);
            }

            public void onPackageUpdateFinished(String str, int i) {
                AccountManagerService.this.cancelAccountAccessRequestNotificationIfNeeded(i, true);
            }
        }.register(this.mContext, this.mHandler.getLooper(), UserHandle.ALL, true);
        this.mAppOpsManager.startWatchingMode(62, (String) null, (AppOpsManager.OnOpChangedListener) new AppOpsManager.OnOpChangedInternalListener() {
            public void onOpChanged(int i, String str) {
                try {
                    int packageUidAsUser = AccountManagerService.this.mPackageManager.getPackageUidAsUser(str, ActivityManager.getCurrentUser());
                    if (AccountManagerService.this.mAppOpsManager.checkOpNoThrow(62, packageUidAsUser, str) == 0) {
                        long jClearCallingIdentity = Binder.clearCallingIdentity();
                        try {
                            AccountManagerService.this.cancelAccountAccessRequestNotificationIfNeeded(str, packageUidAsUser, true);
                            Binder.restoreCallingIdentity(jClearCallingIdentity);
                        } catch (Throwable th) {
                            Binder.restoreCallingIdentity(jClearCallingIdentity);
                            throw th;
                        }
                    }
                } catch (PackageManager.NameNotFoundException e) {
                }
            }
        });
        this.mPackageManager.addOnPermissionsChangeListener(new PackageManager.OnPermissionsChangedListener() {
            public final void onPermissionsChanged(int i) {
                AccountManagerService.lambda$new$0(this.f$0, i);
            }
        });
    }

    public static void lambda$new$0(AccountManagerService accountManagerService, int i) {
        String[] packagesForUid = accountManagerService.mPackageManager.getPackagesForUid(i);
        if (packagesForUid != null) {
            int userId = UserHandle.getUserId(i);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                Account[] accountsAsUser = null;
                for (String str : packagesForUid) {
                    if (accountManagerService.mPackageManager.checkPermission("android.permission.GET_ACCOUNTS", str) == 0) {
                        if (accountsAsUser == null) {
                            accountsAsUser = accountManagerService.getAccountsAsUser(null, userId, PackageManagerService.PLATFORM_PACKAGE_NAME);
                            if (ArrayUtils.isEmpty(accountsAsUser)) {
                                return;
                            }
                        }
                        for (Account account : accountsAsUser) {
                            accountManagerService.cancelAccountAccessRequestNotificationIfNeeded(account, i, str, true);
                        }
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
    }

    boolean getBindInstantServiceAllowed(int i) {
        return this.mAuthenticatorCache.getBindInstantServiceAllowed(i);
    }

    void setBindInstantServiceAllowed(int i, boolean z) {
        this.mAuthenticatorCache.setBindInstantServiceAllowed(i, z);
    }

    private void cancelAccountAccessRequestNotificationIfNeeded(int i, boolean z) {
        for (Account account : getAccountsAsUser(null, UserHandle.getUserId(i), PackageManagerService.PLATFORM_PACKAGE_NAME)) {
            cancelAccountAccessRequestNotificationIfNeeded(account, i, z);
        }
    }

    private void cancelAccountAccessRequestNotificationIfNeeded(String str, int i, boolean z) {
        for (Account account : getAccountsAsUser(null, UserHandle.getUserId(i), PackageManagerService.PLATFORM_PACKAGE_NAME)) {
            cancelAccountAccessRequestNotificationIfNeeded(account, i, str, z);
        }
    }

    private void cancelAccountAccessRequestNotificationIfNeeded(Account account, int i, boolean z) {
        String[] packagesForUid = this.mPackageManager.getPackagesForUid(i);
        if (packagesForUid != null) {
            for (String str : packagesForUid) {
                cancelAccountAccessRequestNotificationIfNeeded(account, i, str, z);
            }
        }
    }

    private void cancelAccountAccessRequestNotificationIfNeeded(Account account, int i, String str, boolean z) {
        if (!z || hasAccountAccess(account, str, UserHandle.getUserHandleForUid(i))) {
            cancelNotification(getCredentialPermissionNotificationId(account, "com.android.AccountManager.ACCOUNT_ACCESS_TOKEN_TYPE", i), UserHandle.getUserHandleForUid(i));
        }
    }

    public boolean addAccountExplicitlyWithVisibility(Account account, String str, Bundle bundle, Map map) {
        Bundle.setDefusable(bundle, true);
        int callingUid = Binder.getCallingUid();
        int callingUserId = UserHandle.getCallingUserId();
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "addAccountExplicitly: " + account + ", caller's uid " + callingUid + ", pid " + Binder.getCallingPid());
        }
        Preconditions.checkNotNull(account, "account cannot be null");
        if (!isAccountManagedByCaller(account.type, callingUid, callingUserId)) {
            throw new SecurityException(String.format("uid %s cannot explicitly add accounts of type: %s", Integer.valueOf(callingUid), account.type));
        }
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            return addAccountInternal(getUserAccounts(callingUserId), account, str, bundle, callingUid, map);
        } finally {
            restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public Map<Account, Integer> getAccountsAndVisibilityForPackage(String str, String str2) {
        int callingUid = Binder.getCallingUid();
        int callingUserId = UserHandle.getCallingUserId();
        boolean zIsSameApp = UserHandle.isSameApp(callingUid, 1000);
        List<String> typesForCaller = getTypesForCaller(callingUid, callingUserId, zIsSameApp);
        if ((str2 != null && !typesForCaller.contains(str2)) || (str2 == null && !zIsSameApp)) {
            throw new SecurityException("getAccountsAndVisibilityForPackage() called from unauthorized uid " + callingUid + " with packageName=" + str);
        }
        if (str2 != null) {
            typesForCaller = new ArrayList<>();
            typesForCaller.add(str2);
        }
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            return getAccountsAndVisibilityForPackage(str, typesForCaller, Integer.valueOf(callingUid), getUserAccounts(callingUserId));
        } finally {
            restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private Map<Account, Integer> getAccountsAndVisibilityForPackage(String str, List<String> list, Integer num, UserAccounts userAccounts) {
        if (!packageExistsForUser(str, userAccounts.userId)) {
            Log.d(TAG, "Package not found " + str);
            return new LinkedHashMap();
        }
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        for (String str2 : list) {
            synchronized (userAccounts.dbLock) {
                synchronized (userAccounts.cacheLock) {
                    Account[] accountArr = userAccounts.accountCache.get(str2);
                    if (accountArr != null) {
                        for (Account account : accountArr) {
                            linkedHashMap.put(account, resolveAccountVisibility(account, str, userAccounts));
                        }
                    }
                }
            }
        }
        return filterSharedAccounts(userAccounts, linkedHashMap, num.intValue(), str);
    }

    public Map<String, Integer> getPackagesAndVisibilityForAccount(Account account) {
        Map<String, Integer> packagesAndVisibilityForAccountLocked;
        Preconditions.checkNotNull(account, "account cannot be null");
        int callingUid = Binder.getCallingUid();
        int callingUserId = UserHandle.getCallingUserId();
        if (!isAccountManagedByCaller(account.type, callingUid, callingUserId) && !isSystemUid(callingUid)) {
            throw new SecurityException(String.format("uid %s cannot get secrets for account %s", Integer.valueOf(callingUid), account));
        }
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            UserAccounts userAccounts = getUserAccounts(callingUserId);
            synchronized (userAccounts.dbLock) {
                synchronized (userAccounts.cacheLock) {
                    packagesAndVisibilityForAccountLocked = getPackagesAndVisibilityForAccountLocked(account, userAccounts);
                }
            }
            return packagesAndVisibilityForAccountLocked;
        } finally {
            restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private Map<String, Integer> getPackagesAndVisibilityForAccountLocked(Account account, UserAccounts userAccounts) {
        Map<String, Integer> map = (Map) userAccounts.visibilityCache.get(account);
        if (map == null) {
            Log.d(TAG, "Visibility was not initialized");
            HashMap map2 = new HashMap();
            userAccounts.visibilityCache.put(account, map2);
            return map2;
        }
        return map;
    }

    public int getAccountVisibility(Account account, String str) {
        Preconditions.checkNotNull(account, "account cannot be null");
        Preconditions.checkNotNull(str, "packageName cannot be null");
        int callingUid = Binder.getCallingUid();
        int callingUserId = UserHandle.getCallingUserId();
        if (!isAccountManagedByCaller(account.type, callingUid, callingUserId) && !isSystemUid(callingUid)) {
            throw new SecurityException(String.format("uid %s cannot get secrets for accounts of type: %s", Integer.valueOf(callingUid), account.type));
        }
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            UserAccounts userAccounts = getUserAccounts(callingUserId);
            if ("android:accounts:key_legacy_visible".equals(str)) {
                int accountVisibilityFromCache = getAccountVisibilityFromCache(account, str, userAccounts);
                if (accountVisibilityFromCache != 0) {
                    return accountVisibilityFromCache;
                }
                return 2;
            }
            if (!"android:accounts:key_legacy_not_visible".equals(str)) {
                return resolveAccountVisibility(account, str, userAccounts).intValue();
            }
            int accountVisibilityFromCache2 = getAccountVisibilityFromCache(account, str, userAccounts);
            if (accountVisibilityFromCache2 != 0) {
                return accountVisibilityFromCache2;
            }
            return 4;
        } finally {
            restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private int getAccountVisibilityFromCache(Account account, String str, UserAccounts userAccounts) {
        int iIntValue;
        synchronized (userAccounts.cacheLock) {
            Integer num = getPackagesAndVisibilityForAccountLocked(account, userAccounts).get(str);
            iIntValue = num != null ? num.intValue() : 0;
        }
        return iIntValue;
    }

    private Integer resolveAccountVisibility(Account account, String str, UserAccounts userAccounts) {
        Preconditions.checkNotNull(str, "packageName cannot be null");
        try {
            long jClearCallingIdentity = clearCallingIdentity();
            try {
                int packageUidAsUser = this.mPackageManager.getPackageUidAsUser(str, userAccounts.userId);
                if (!UserHandle.isSameApp(packageUidAsUser, 1000)) {
                    int iCheckPackageSignature = checkPackageSignature(account.type, packageUidAsUser, userAccounts.userId);
                    int accountVisibilityFromCache = 2;
                    if (iCheckPackageSignature == 2) {
                        return 1;
                    }
                    int accountVisibilityFromCache2 = getAccountVisibilityFromCache(account, str, userAccounts);
                    if (accountVisibilityFromCache2 == 0) {
                        boolean zIsPermittedForPackage = isPermittedForPackage(str, packageUidAsUser, userAccounts.userId, "android.permission.GET_ACCOUNTS_PRIVILEGED");
                        if (isProfileOwner(packageUidAsUser)) {
                            return 1;
                        }
                        boolean zIsPreOApplication = isPreOApplication(str);
                        if (iCheckPackageSignature != 0 || ((zIsPreOApplication && checkGetAccountsPermission(str, packageUidAsUser, userAccounts.userId)) || ((checkReadContactsPermission(str, packageUidAsUser, userAccounts.userId) && accountTypeManagesContacts(account.type, userAccounts.userId)) || zIsPermittedForPackage))) {
                            int accountVisibilityFromCache3 = getAccountVisibilityFromCache(account, "android:accounts:key_legacy_visible", userAccounts);
                            if (accountVisibilityFromCache3 != 0) {
                                accountVisibilityFromCache = accountVisibilityFromCache3;
                            }
                        } else {
                            accountVisibilityFromCache = getAccountVisibilityFromCache(account, "android:accounts:key_legacy_not_visible", userAccounts);
                            if (accountVisibilityFromCache == 0) {
                                accountVisibilityFromCache = 4;
                            }
                        }
                        return Integer.valueOf(accountVisibilityFromCache);
                    }
                    return Integer.valueOf(accountVisibilityFromCache2);
                }
                return 1;
            } finally {
                restoreCallingIdentity(jClearCallingIdentity);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(TAG, "Package not found " + e.getMessage());
            return 3;
        }
    }

    private boolean isPreOApplication(String str) {
        try {
            long jClearCallingIdentity = clearCallingIdentity();
            try {
                ApplicationInfo applicationInfo = this.mPackageManager.getApplicationInfo(str, 0);
                if (applicationInfo != null && applicationInfo.targetSdkVersion >= 26) {
                    return false;
                }
                return true;
            } finally {
                restoreCallingIdentity(jClearCallingIdentity);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(TAG, "Package not found " + e.getMessage());
            return true;
        }
    }

    public boolean setAccountVisibility(Account account, String str, int i) {
        Preconditions.checkNotNull(account, "account cannot be null");
        Preconditions.checkNotNull(str, "packageName cannot be null");
        int callingUid = Binder.getCallingUid();
        int callingUserId = UserHandle.getCallingUserId();
        if (!isAccountManagedByCaller(account.type, callingUid, callingUserId) && !isSystemUid(callingUid)) {
            throw new SecurityException(String.format("uid %s cannot get secrets for accounts of type: %s", Integer.valueOf(callingUid), account.type));
        }
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            return setAccountVisibility(account, str, i, true, getUserAccounts(callingUserId));
        } finally {
            restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private boolean isVisible(int i) {
        return i == 1 || i == 2;
    }

    private boolean setAccountVisibility(Account account, String str, int i, boolean z, UserAccounts userAccounts) {
        Map<String, Integer> mapEmptyMap;
        List<String> listEmptyList;
        synchronized (userAccounts.dbLock) {
            synchronized (userAccounts.cacheLock) {
                try {
                    if (z) {
                        if (!isSpecialPackageKey(str)) {
                            if (!packageExistsForUser(str, userAccounts.userId)) {
                                return false;
                            }
                            mapEmptyMap = new HashMap<>();
                            mapEmptyMap.put(str, resolveAccountVisibility(account, str, userAccounts));
                            listEmptyList = new ArrayList<>();
                            if (shouldNotifyPackageOnAccountRemoval(account, str, userAccounts)) {
                                listEmptyList.add(str);
                            }
                        } else {
                            mapEmptyMap = getRequestingPackages(account, userAccounts);
                            listEmptyList = getAccountRemovedReceivers(account, userAccounts);
                        }
                    } else {
                        if (!isSpecialPackageKey(str) && !packageExistsForUser(str, userAccounts.userId)) {
                            return false;
                        }
                        mapEmptyMap = Collections.emptyMap();
                        listEmptyList = Collections.emptyList();
                    }
                    if (!updateAccountVisibilityLocked(account, str, i, userAccounts)) {
                        return false;
                    }
                    if (z) {
                        for (Map.Entry<String, Integer> entry : mapEmptyMap.entrySet()) {
                            if (isVisible(entry.getValue().intValue()) != isVisible(resolveAccountVisibility(account, str, userAccounts).intValue())) {
                                notifyPackage(entry.getKey(), userAccounts);
                            }
                        }
                        Iterator<String> it = listEmptyList.iterator();
                        while (it.hasNext()) {
                            sendAccountRemovedBroadcast(account, it.next(), userAccounts.userId);
                        }
                        sendAccountsChangedBroadcast(userAccounts.userId);
                    }
                    return true;
                } catch (Throwable th) {
                    throw th;
                }
            }
        }
    }

    private boolean updateAccountVisibilityLocked(Account account, String str, int i, UserAccounts userAccounts) {
        long jFindDeAccountId = userAccounts.accountsDb.findDeAccountId(account);
        if (jFindDeAccountId < 0) {
            return false;
        }
        StrictMode.ThreadPolicy threadPolicyAllowThreadDiskWrites = StrictMode.allowThreadDiskWrites();
        try {
            if (!userAccounts.accountsDb.setAccountVisibility(jFindDeAccountId, str, i)) {
                return false;
            }
            StrictMode.setThreadPolicy(threadPolicyAllowThreadDiskWrites);
            getPackagesAndVisibilityForAccountLocked(account, userAccounts).put(str, Integer.valueOf(i));
            return true;
        } finally {
            StrictMode.setThreadPolicy(threadPolicyAllowThreadDiskWrites);
        }
    }

    public void registerAccountListener(String[] strArr, String str) {
        this.mAppOpsManager.checkPackage(Binder.getCallingUid(), str);
        int callingUserId = UserHandle.getCallingUserId();
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            registerAccountListener(strArr, str, getUserAccounts(callingUserId));
        } finally {
            restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void registerAccountListener(String[] strArr, String str, UserAccounts userAccounts) {
        synchronized (userAccounts.mReceiversForType) {
            if (strArr == null) {
                strArr = new String[]{null};
                for (String str2 : strArr) {
                    Map map = (Map) userAccounts.mReceiversForType.get(str2);
                    if (map == null) {
                        map = new HashMap();
                        userAccounts.mReceiversForType.put(str2, map);
                    }
                    Integer num = (Integer) map.get(str);
                    int iIntValue = 1;
                    if (num != null) {
                        iIntValue = 1 + num.intValue();
                    }
                    map.put(str, Integer.valueOf(iIntValue));
                }
            } else {
                while (i < r1) {
                }
            }
        }
    }

    public void unregisterAccountListener(String[] strArr, String str) {
        this.mAppOpsManager.checkPackage(Binder.getCallingUid(), str);
        int callingUserId = UserHandle.getCallingUserId();
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            unregisterAccountListener(strArr, str, getUserAccounts(callingUserId));
        } finally {
            restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void unregisterAccountListener(String[] strArr, String str, UserAccounts userAccounts) {
        synchronized (userAccounts.mReceiversForType) {
            if (strArr == null) {
                strArr = new String[]{null};
            }
            for (String str2 : strArr) {
                Map map = (Map) userAccounts.mReceiversForType.get(str2);
                if (map == null || map.get(str) == null) {
                    throw new IllegalArgumentException("attempt to unregister wrong receiver");
                }
                Integer num = (Integer) map.get(str);
                if (num.intValue() == 1) {
                    map.remove(str);
                } else {
                    map.put(str, Integer.valueOf(num.intValue() - 1));
                }
            }
        }
    }

    private void sendNotificationAccountUpdated(Account account, UserAccounts userAccounts) {
        for (Map.Entry<String, Integer> entry : getRequestingPackages(account, userAccounts).entrySet()) {
            if (entry.getValue().intValue() != 3 && entry.getValue().intValue() != 4) {
                notifyPackage(entry.getKey(), userAccounts);
            }
        }
    }

    private void notifyPackage(String str, UserAccounts userAccounts) {
        Intent intent = new Intent("android.accounts.action.VISIBLE_ACCOUNTS_CHANGED");
        intent.setPackage(str);
        intent.setFlags(1073741824);
        this.mContext.sendBroadcastAsUser(intent, new UserHandle(userAccounts.userId));
    }

    private Map<String, Integer> getRequestingPackages(Account account, UserAccounts userAccounts) {
        HashSet<String> hashSet = new HashSet();
        synchronized (userAccounts.mReceiversForType) {
            for (String str : new String[]{account.type, null}) {
                Map map = (Map) userAccounts.mReceiversForType.get(str);
                if (map != null) {
                    hashSet.addAll(map.keySet());
                }
            }
        }
        HashMap map2 = new HashMap();
        for (String str2 : hashSet) {
            map2.put(str2, resolveAccountVisibility(account, str2, userAccounts));
        }
        return map2;
    }

    private List<String> getAccountRemovedReceivers(Account account, UserAccounts userAccounts) {
        Intent intent = new Intent("android.accounts.action.ACCOUNT_REMOVED");
        intent.setFlags(DumpState.DUMP_SERVICE_PERMISSIONS);
        List listQueryBroadcastReceiversAsUser = this.mPackageManager.queryBroadcastReceiversAsUser(intent, 0, userAccounts.userId);
        ArrayList arrayList = new ArrayList();
        if (listQueryBroadcastReceiversAsUser == null) {
            return arrayList;
        }
        Iterator it = listQueryBroadcastReceiversAsUser.iterator();
        while (it.hasNext()) {
            String str = ((ResolveInfo) it.next()).activityInfo.applicationInfo.packageName;
            int iIntValue = resolveAccountVisibility(account, str, userAccounts).intValue();
            if (iIntValue == 1 || iIntValue == 2) {
                arrayList.add(str);
            }
        }
        return arrayList;
    }

    private boolean shouldNotifyPackageOnAccountRemoval(Account account, String str, UserAccounts userAccounts) {
        int iIntValue = resolveAccountVisibility(account, str, userAccounts).intValue();
        if (iIntValue != 1 && iIntValue != 2) {
            return false;
        }
        Intent intent = new Intent("android.accounts.action.ACCOUNT_REMOVED");
        intent.setFlags(DumpState.DUMP_SERVICE_PERMISSIONS);
        intent.setPackage(str);
        List listQueryBroadcastReceiversAsUser = this.mPackageManager.queryBroadcastReceiversAsUser(intent, 0, userAccounts.userId);
        return listQueryBroadcastReceiversAsUser != null && listQueryBroadcastReceiversAsUser.size() > 0;
    }

    private boolean packageExistsForUser(String str, int i) {
        try {
            long jClearCallingIdentity = clearCallingIdentity();
            try {
                this.mPackageManager.getPackageUidAsUser(str, i);
                return true;
            } finally {
                restoreCallingIdentity(jClearCallingIdentity);
            }
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private boolean isSpecialPackageKey(String str) {
        return "android:accounts:key_legacy_visible".equals(str) || "android:accounts:key_legacy_not_visible".equals(str);
    }

    private void sendAccountsChangedBroadcast(int i) {
        Log.i(TAG, "the accounts changed, sending broadcast of " + ACCOUNTS_CHANGED_INTENT.getAction());
        this.mContext.sendBroadcastAsUser(ACCOUNTS_CHANGED_INTENT, new UserHandle(i));
    }

    private void sendAccountRemovedBroadcast(Account account, String str, int i) {
        Intent intent = new Intent("android.accounts.action.ACCOUNT_REMOVED");
        intent.setFlags(DumpState.DUMP_SERVICE_PERMISSIONS);
        intent.setPackage(str);
        intent.putExtra("authAccount", account.name);
        intent.putExtra("accountType", account.type);
        this.mContext.sendBroadcastAsUser(intent, new UserHandle(i));
    }

    public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
        try {
            return super.onTransact(i, parcel, parcel2, i2);
        } catch (RuntimeException e) {
            if (!(e instanceof SecurityException)) {
                Slog.wtf(TAG, "Account Manager Crash", e);
            }
            throw e;
        }
    }

    private UserManager getUserManager() {
        if (this.mUserManager == null) {
            this.mUserManager = UserManager.get(this.mContext);
        }
        return this.mUserManager;
    }

    public void validateAccounts(int i) {
        validateAccountsInternal(getUserAccounts(i), true);
    }

    private void validateAccountsInternal(UserAccounts userAccounts, boolean z) {
        boolean z2;
        Iterator<Map.Entry<Long, Account>> it;
        boolean z3;
        LinkedHashMap linkedHashMap;
        if (Log.isLoggable(TAG, 3)) {
            Log.d(TAG, "validateAccountsInternal " + userAccounts.userId + " isCeDatabaseAttached=" + userAccounts.accountsDb.isCeDatabaseAttached() + " userLocked=" + this.mLocalUnlockedUsers.get(userAccounts.userId));
        }
        if (z) {
            this.mAuthenticatorCache.invalidateCache(userAccounts.userId);
        }
        HashMap<String, Integer> authenticatorTypeAndUIDForUser = getAuthenticatorTypeAndUIDForUser(this.mAuthenticatorCache, userAccounts.userId);
        boolean zIsLocalUnlockedUser = isLocalUnlockedUser(userAccounts.userId);
        synchronized (userAccounts.dbLock) {
            synchronized (userAccounts.cacheLock) {
                AccountsDb accountsDb = userAccounts.accountsDb;
                Map<String, Integer> mapFindMetaAuthUid = accountsDb.findMetaAuthUid();
                HashSet hashSetNewHashSet = Sets.newHashSet();
                SparseBooleanArray uidsOfInstalledOrUpdatedPackagesAsUser = null;
                for (Map.Entry<String, Integer> entry : mapFindMetaAuthUid.entrySet()) {
                    String key = entry.getKey();
                    int iIntValue = entry.getValue().intValue();
                    Integer num = authenticatorTypeAndUIDForUser.get(key);
                    if (num != null && iIntValue == num.intValue()) {
                        authenticatorTypeAndUIDForUser.remove(key);
                    } else {
                        if (uidsOfInstalledOrUpdatedPackagesAsUser == null) {
                            uidsOfInstalledOrUpdatedPackagesAsUser = getUidsOfInstalledOrUpdatedPackagesAsUser(userAccounts.userId);
                        }
                        if (!uidsOfInstalledOrUpdatedPackagesAsUser.get(iIntValue)) {
                            hashSetNewHashSet.add(key);
                            accountsDb.deleteMetaByAuthTypeAndUid(key, iIntValue);
                        }
                    }
                }
                for (Map.Entry<String, Integer> entry2 : authenticatorTypeAndUIDForUser.entrySet()) {
                    accountsDb.insertOrReplaceMetaAuthTypeAndUid(entry2.getKey(), entry2.getValue().intValue());
                }
                Map<Long, Account> mapFindAllDeAccounts = accountsDb.findAllDeAccounts();
                try {
                    userAccounts.accountCache.clear();
                    LinkedHashMap linkedHashMap2 = new LinkedHashMap();
                    Iterator<Map.Entry<Long, Account>> it2 = mapFindAllDeAccounts.entrySet().iterator();
                    z2 = false;
                    while (it2.hasNext()) {
                        try {
                            Map.Entry<Long, Account> next = it2.next();
                            long jLongValue = next.getKey().longValue();
                            Account value = next.getValue();
                            if (hashSetNewHashSet.contains(value.type)) {
                                Slog.w(TAG, "deleting account " + value.name + " because type " + value.type + "'s registered authenticator no longer exist.");
                                Map<String, Integer> requestingPackages = getRequestingPackages(value, userAccounts);
                                List<String> accountRemovedReceivers = getAccountRemovedReceivers(value, userAccounts);
                                accountsDb.beginTransaction();
                                try {
                                    accountsDb.deleteDeAccount(jLongValue);
                                    if (zIsLocalUnlockedUser) {
                                        accountsDb.deleteCeAccount(jLongValue);
                                    }
                                    accountsDb.setTransactionSuccessful();
                                    try {
                                        it = it2;
                                        z3 = zIsLocalUnlockedUser;
                                        linkedHashMap = linkedHashMap2;
                                        logRecord(AccountsDb.DEBUG_ACTION_AUTHENTICATOR_REMOVE, "accounts", jLongValue, userAccounts);
                                        userAccounts.userDataCache.remove(value);
                                        userAccounts.authTokenCache.remove(value);
                                        userAccounts.accountTokenCaches.remove(value);
                                        userAccounts.visibilityCache.remove(value);
                                        for (Map.Entry<String, Integer> entry3 : requestingPackages.entrySet()) {
                                            if (isVisible(entry3.getValue().intValue())) {
                                                notifyPackage(entry3.getKey(), userAccounts);
                                            }
                                        }
                                        Iterator<String> it3 = accountRemovedReceivers.iterator();
                                        while (it3.hasNext()) {
                                            sendAccountRemovedBroadcast(value, it3.next(), userAccounts.userId);
                                        }
                                        z2 = true;
                                    } catch (Throwable th) {
                                        th = th;
                                        z2 = true;
                                        if (z2) {
                                            sendAccountsChangedBroadcast(userAccounts.userId);
                                        }
                                        throw th;
                                    }
                                } finally {
                                    accountsDb.endTransaction();
                                }
                            } else {
                                it = it2;
                                z3 = zIsLocalUnlockedUser;
                                linkedHashMap = linkedHashMap2;
                                ArrayList arrayList = (ArrayList) linkedHashMap.get(value.type);
                                if (arrayList == null) {
                                    arrayList = new ArrayList();
                                    linkedHashMap.put(value.type, arrayList);
                                }
                                arrayList.add(value.name);
                            }
                            linkedHashMap2 = linkedHashMap;
                            it2 = it;
                            zIsLocalUnlockedUser = z3;
                        } catch (Throwable th2) {
                            th = th2;
                        }
                    }
                    for (Map.Entry entry4 : linkedHashMap2.entrySet()) {
                        String str = (String) entry4.getKey();
                        ArrayList arrayList2 = (ArrayList) entry4.getValue();
                        Account[] accountArr = new Account[arrayList2.size()];
                        for (int i = 0; i < accountArr.length; i++) {
                            accountArr[i] = new Account((String) arrayList2.get(i), str, UUID.randomUUID().toString());
                        }
                        userAccounts.accountCache.put(str, accountArr);
                    }
                    userAccounts.visibilityCache.putAll(accountsDb.findAllVisibilityValues());
                    if (z2) {
                        sendAccountsChangedBroadcast(userAccounts.userId);
                    }
                } catch (Throwable th3) {
                    th = th3;
                    z2 = false;
                }
            }
        }
    }

    private SparseBooleanArray getUidsOfInstalledOrUpdatedPackagesAsUser(int i) {
        List<PackageInfo> installedPackagesAsUser = this.mPackageManager.getInstalledPackagesAsUser(8192, i);
        SparseBooleanArray sparseBooleanArray = new SparseBooleanArray(installedPackagesAsUser.size());
        for (PackageInfo packageInfo : installedPackagesAsUser) {
            if (packageInfo.applicationInfo != null && (packageInfo.applicationInfo.flags & DumpState.DUMP_VOLUMES) != 0) {
                sparseBooleanArray.put(packageInfo.applicationInfo.uid, true);
            }
        }
        return sparseBooleanArray;
    }

    static HashMap<String, Integer> getAuthenticatorTypeAndUIDForUser(Context context, int i) {
        return getAuthenticatorTypeAndUIDForUser(new AccountAuthenticatorCache(context), i);
    }

    private static HashMap<String, Integer> getAuthenticatorTypeAndUIDForUser(IAccountAuthenticatorCache iAccountAuthenticatorCache, int i) {
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        for (RegisteredServicesCache.ServiceInfo<AuthenticatorDescription> serviceInfo : iAccountAuthenticatorCache.getAllServices(i)) {
            linkedHashMap.put(((AuthenticatorDescription) serviceInfo.type).type, Integer.valueOf(serviceInfo.uid));
        }
        return linkedHashMap;
    }

    private UserAccounts getUserAccountsForCaller() {
        return getUserAccounts(UserHandle.getCallingUserId());
    }

    protected UserAccounts getUserAccounts(int i) {
        UserAccounts userAccounts;
        synchronized (this.mUsers) {
            userAccounts = this.mUsers.get(i);
            boolean z = false;
            if (userAccounts == null) {
                UserAccounts userAccounts2 = new UserAccounts(this.mContext, i, new File(this.mInjector.getPreNDatabaseName(i)), new File(this.mInjector.getDeDatabaseName(i)));
                initializeDebugDbSizeAndCompileSqlStatementForLogging(userAccounts2);
                this.mUsers.append(i, userAccounts2);
                purgeOldGrants(userAccounts2);
                z = true;
                userAccounts = userAccounts2;
            }
            if (!userAccounts.accountsDb.isCeDatabaseAttached() && this.mLocalUnlockedUsers.get(i)) {
                Log.i(TAG, "User " + i + " is unlocked - opening CE database");
                synchronized (userAccounts.dbLock) {
                    synchronized (userAccounts.cacheLock) {
                        userAccounts.accountsDb.attachCeDatabase(new File(this.mInjector.getCeDatabaseName(i)));
                    }
                }
                syncDeCeAccountsLocked(userAccounts);
            }
            if (z) {
                validateAccountsInternal(userAccounts, true);
            }
        }
        return userAccounts;
    }

    private void syncDeCeAccountsLocked(UserAccounts userAccounts) {
        Preconditions.checkState(Thread.holdsLock(this.mUsers), "mUsers lock must be held");
        List<Account> listFindCeAccountsNotInDe = userAccounts.accountsDb.findCeAccountsNotInDe();
        if (!listFindCeAccountsNotInDe.isEmpty()) {
            Slog.i(TAG, "Accounts " + listFindCeAccountsNotInDe + " were previously deleted while user " + userAccounts.userId + " was locked. Removing accounts from CE tables");
            logRecord(userAccounts, AccountsDb.DEBUG_ACTION_SYNC_DE_CE_ACCOUNTS, "accounts");
            Iterator<Account> it = listFindCeAccountsNotInDe.iterator();
            while (it.hasNext()) {
                removeAccountInternal(userAccounts, it.next(), 1000);
            }
        }
    }

    private void purgeOldGrantsAll() {
        synchronized (this.mUsers) {
            for (int i = 0; i < this.mUsers.size(); i++) {
                purgeOldGrants(this.mUsers.valueAt(i));
            }
        }
    }

    private void purgeOldGrants(UserAccounts userAccounts) {
        synchronized (userAccounts.dbLock) {
            synchronized (userAccounts.cacheLock) {
                Iterator<Integer> it = userAccounts.accountsDb.findAllUidGrants().iterator();
                while (it.hasNext()) {
                    int iIntValue = it.next().intValue();
                    if (!(this.mPackageManager.getPackagesForUid(iIntValue) != null)) {
                        Log.d(TAG, "deleting grants for UID " + iIntValue + " because its package is no longer installed");
                        userAccounts.accountsDb.deleteGrantsByUid(iIntValue);
                    }
                }
            }
        }
    }

    private void removeVisibilityValuesForPackage(String str) {
        if (isSpecialPackageKey(str)) {
            return;
        }
        synchronized (this.mUsers) {
            int size = this.mUsers.size();
            for (int i = 0; i < size; i++) {
                UserAccounts userAccountsValueAt = this.mUsers.valueAt(i);
                try {
                    this.mPackageManager.getPackageUidAsUser(str, userAccountsValueAt.userId);
                } catch (PackageManager.NameNotFoundException e) {
                    userAccountsValueAt.accountsDb.deleteAccountVisibilityForPackage(str);
                    synchronized (userAccountsValueAt.dbLock) {
                        synchronized (userAccountsValueAt.cacheLock) {
                            Iterator it = userAccountsValueAt.visibilityCache.keySet().iterator();
                            while (it.hasNext()) {
                                getPackagesAndVisibilityForAccountLocked((Account) it.next(), userAccountsValueAt).remove(str);
                            }
                        }
                    }
                }
            }
        }
    }

    private void purgeUserData(int i) {
        UserAccounts userAccounts;
        synchronized (this.mUsers) {
            userAccounts = this.mUsers.get(i);
            this.mUsers.remove(i);
            this.mLocalUnlockedUsers.delete(i);
        }
        if (userAccounts != null) {
            synchronized (userAccounts.dbLock) {
                synchronized (userAccounts.cacheLock) {
                    userAccounts.statementForLogging.close();
                    userAccounts.accountsDb.close();
                }
            }
        }
    }

    @VisibleForTesting
    void onUserUnlocked(Intent intent) {
        onUnlockUser(intent.getIntExtra("android.intent.extra.user_handle", -1));
    }

    void onUnlockUser(final int i) {
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "onUserUnlocked " + i);
        }
        synchronized (this.mUsers) {
            this.mLocalUnlockedUsers.put(i, true);
        }
        if (i < 1) {
            return;
        }
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                this.f$0.syncSharedAccounts(i);
            }
        });
    }

    private void syncSharedAccounts(int i) {
        int i2;
        Account[] sharedAccountsAsUser = getSharedAccountsAsUser(i);
        if (sharedAccountsAsUser == null || sharedAccountsAsUser.length == 0) {
            return;
        }
        Account[] accountsAsUser = getAccountsAsUser(null, i, this.mContext.getOpPackageName());
        if (UserManager.isSplitSystemUser()) {
            i2 = getUserManager().getUserInfo(i).restrictedProfileParentId;
        } else {
            i2 = 0;
        }
        if (i2 < 0) {
            Log.w(TAG, "User " + i + " has shared accounts, but no parent user");
            return;
        }
        for (Account account : sharedAccountsAsUser) {
            if (!ArrayUtils.contains(accountsAsUser, account)) {
                copyAccountToUser(null, account, i2, i);
            }
        }
    }

    public void onServiceChanged(AuthenticatorDescription authenticatorDescription, int i, boolean z) {
        validateAccountsInternal(getUserAccounts(i), false);
    }

    public String getPassword(Account account) {
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "getPassword: " + account + ", caller's uid " + Binder.getCallingUid() + ", pid " + Binder.getCallingPid());
        }
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        int callingUserId = UserHandle.getCallingUserId();
        if (!isAccountManagedByCaller(account.type, callingUid, callingUserId)) {
            throw new SecurityException(String.format("uid %s cannot get secrets for accounts of type: %s", Integer.valueOf(callingUid), account.type));
        }
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            return readPasswordInternal(getUserAccounts(callingUserId), account);
        } finally {
            restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private String readPasswordInternal(UserAccounts userAccounts, Account account) {
        String strFindAccountPasswordByNameAndType;
        if (account != null) {
            if (!isLocalUnlockedUser(userAccounts.userId)) {
                Log.w(TAG, "Password is not available - user " + userAccounts.userId + " data is locked");
                return null;
            }
            synchronized (userAccounts.dbLock) {
                synchronized (userAccounts.cacheLock) {
                    strFindAccountPasswordByNameAndType = userAccounts.accountsDb.findAccountPasswordByNameAndType(account.name, account.type);
                }
            }
            return strFindAccountPasswordByNameAndType;
        }
        return null;
    }

    public String getPreviousName(Account account) {
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "getPreviousName: " + account + ", caller's uid " + Binder.getCallingUid() + ", pid " + Binder.getCallingPid());
        }
        Preconditions.checkNotNull(account, "account cannot be null");
        int callingUserId = UserHandle.getCallingUserId();
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            return readPreviousNameInternal(getUserAccounts(callingUserId), account);
        } finally {
            restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private String readPreviousNameInternal(UserAccounts userAccounts, Account account) {
        if (account == null) {
            return null;
        }
        synchronized (userAccounts.dbLock) {
            synchronized (userAccounts.cacheLock) {
                AtomicReference atomicReference = (AtomicReference) userAccounts.previousNameCache.get(account);
                if (atomicReference == null) {
                    String strFindDeAccountPreviousName = userAccounts.accountsDb.findDeAccountPreviousName(account);
                    userAccounts.previousNameCache.put(account, new AtomicReference(strFindDeAccountPreviousName));
                    return strFindDeAccountPreviousName;
                }
                return (String) atomicReference.get();
            }
        }
    }

    public String getUserData(Account account, String str) {
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, String.format("getUserData( account: %s, key: %s, callerUid: %s, pid: %s", account, str, Integer.valueOf(callingUid), Integer.valueOf(Binder.getCallingPid())));
        }
        Preconditions.checkNotNull(account, "account cannot be null");
        Preconditions.checkNotNull(str, "key cannot be null");
        int callingUserId = UserHandle.getCallingUserId();
        if (!isAccountManagedByCaller(account.type, callingUid, callingUserId)) {
            throw new SecurityException(String.format("uid %s cannot get user data for accounts of type: %s", Integer.valueOf(callingUid), account.type));
        }
        if (!isLocalUnlockedUser(callingUserId)) {
            Log.w(TAG, "User " + callingUserId + " data is locked. callingUid " + callingUid);
            return null;
        }
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            UserAccounts userAccounts = getUserAccounts(callingUserId);
            if (accountExistsCache(userAccounts, account)) {
                return readUserDataInternal(userAccounts, account, str);
            }
            return null;
        } finally {
            restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public AuthenticatorDescription[] getAuthenticatorTypes(int i) {
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "getAuthenticatorTypes: for user id " + i + " caller's uid " + callingUid + ", pid " + Binder.getCallingPid());
        }
        if (isCrossUser(callingUid, i)) {
            throw new SecurityException(String.format("User %s tying to get authenticator types for %s", Integer.valueOf(UserHandle.getCallingUserId()), Integer.valueOf(i)));
        }
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            return getAuthenticatorTypesInternal(i);
        } finally {
            restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private AuthenticatorDescription[] getAuthenticatorTypesInternal(int i) {
        this.mAuthenticatorCache.updateServices(i);
        Collection<RegisteredServicesCache.ServiceInfo<AuthenticatorDescription>> allServices = this.mAuthenticatorCache.getAllServices(i);
        AuthenticatorDescription[] authenticatorDescriptionArr = new AuthenticatorDescription[allServices.size()];
        Iterator<RegisteredServicesCache.ServiceInfo<AuthenticatorDescription>> it = allServices.iterator();
        int i2 = 0;
        while (it.hasNext()) {
            authenticatorDescriptionArr[i2] = (AuthenticatorDescription) it.next().type;
            i2++;
        }
        return authenticatorDescriptionArr;
    }

    private boolean isCrossUser(int i, int i2) {
        return (i2 == UserHandle.getCallingUserId() || i == 1000 || this.mContext.checkCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL") == 0) ? false : true;
    }

    public boolean addAccountExplicitly(Account account, String str, Bundle bundle) {
        return addAccountExplicitlyWithVisibility(account, str, bundle, null);
    }

    public void copyAccountToUser(final IAccountManagerResponse iAccountManagerResponse, final Account account, final int i, int i2) {
        if (isCrossUser(Binder.getCallingUid(), -1)) {
            throw new SecurityException("Calling copyAccountToUser requires android.permission.INTERACT_ACROSS_USERS_FULL");
        }
        UserAccounts userAccounts = getUserAccounts(i);
        final UserAccounts userAccounts2 = getUserAccounts(i2);
        if (userAccounts != null && userAccounts2 != null) {
            Slog.d(TAG, "Copying account " + account.name + " from user " + i + " to user " + i2);
            long jClearCallingIdentity = clearCallingIdentity();
            try {
                new Session(userAccounts, iAccountManagerResponse, account.type, false, false, account.name, false) {
                    @Override
                    protected String toDebugString(long j) {
                        return super.toDebugString(j) + ", getAccountCredentialsForClone, " + account.type;
                    }

                    @Override
                    public void run() throws RemoteException {
                        this.mAuthenticator.getAccountCredentialsForCloning(this, account);
                    }

                    @Override
                    public void onResult(Bundle bundle) {
                        Bundle.setDefusable(bundle, true);
                        if (bundle != null && bundle.getBoolean("booleanResult", false)) {
                            AccountManagerService.this.completeCloningAccount(iAccountManagerResponse, bundle, account, userAccounts2, i);
                        } else {
                            super.onResult(bundle);
                        }
                    }
                }.bind();
                return;
            } finally {
                restoreCallingIdentity(jClearCallingIdentity);
            }
        }
        if (iAccountManagerResponse != null) {
            Bundle bundle = new Bundle();
            bundle.putBoolean("booleanResult", false);
            try {
                iAccountManagerResponse.onResult(bundle);
            } catch (RemoteException e) {
                Slog.w(TAG, "Failed to report error back to the client." + e);
            }
        }
    }

    public boolean accountAuthenticated(Account account) {
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, String.format("accountAuthenticated( account: %s, callerUid: %s)", account, Integer.valueOf(callingUid)));
        }
        Preconditions.checkNotNull(account, "account cannot be null");
        int callingUserId = UserHandle.getCallingUserId();
        if (!isAccountManagedByCaller(account.type, callingUid, callingUserId)) {
            throw new SecurityException(String.format("uid %s cannot notify authentication for accounts of type: %s", Integer.valueOf(callingUid), account.type));
        }
        if (!canUserModifyAccounts(callingUserId, callingUid) || !canUserModifyAccountsForType(callingUserId, account.type, callingUid)) {
            return false;
        }
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            getUserAccounts(callingUserId);
            return updateLastAuthenticatedTime(account);
        } finally {
            restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private boolean updateLastAuthenticatedTime(Account account) {
        boolean zUpdateAccountLastAuthenticatedTime;
        UserAccounts userAccountsForCaller = getUserAccountsForCaller();
        synchronized (userAccountsForCaller.dbLock) {
            synchronized (userAccountsForCaller.cacheLock) {
                zUpdateAccountLastAuthenticatedTime = userAccountsForCaller.accountsDb.updateAccountLastAuthenticatedTime(account);
            }
        }
        return zUpdateAccountLastAuthenticatedTime;
    }

    private void completeCloningAccount(IAccountManagerResponse iAccountManagerResponse, final Bundle bundle, final Account account, UserAccounts userAccounts, final int i) {
        Bundle.setDefusable(bundle, true);
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            new Session(userAccounts, iAccountManagerResponse, account.type, false, false, account.name, false) {
                @Override
                protected String toDebugString(long j) {
                    return super.toDebugString(j) + ", getAccountCredentialsForClone, " + account.type;
                }

                @Override
                public void run() throws RemoteException {
                    for (Account account2 : AccountManagerService.this.getAccounts(i, AccountManagerService.this.mContext.getOpPackageName())) {
                        if (account2.equals(account)) {
                            this.mAuthenticator.addAccountFromCredentials(this, account, bundle);
                            return;
                        }
                    }
                }

                @Override
                public void onResult(Bundle bundle2) {
                    Bundle.setDefusable(bundle2, true);
                    super.onResult(bundle2);
                }

                @Override
                public void onError(int i2, String str) {
                    super.onError(i2, str);
                }
            }.bind();
        } finally {
            restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private boolean addAccountInternal(UserAccounts userAccounts, Account account, String str, Bundle bundle, int i, Map<String, Integer> map) {
        Bundle.setDefusable(bundle, true);
        if (account == null) {
            return false;
        }
        if (!isLocalUnlockedUser(userAccounts.userId)) {
            Log.w(TAG, "Account " + account + " cannot be added - user " + userAccounts.userId + " is locked. callingUid=" + i);
            return false;
        }
        synchronized (userAccounts.dbLock) {
            synchronized (userAccounts.cacheLock) {
                userAccounts.accountsDb.beginTransaction();
                try {
                    if (userAccounts.accountsDb.findCeAccountId(account) >= 0) {
                        Log.w(TAG, "insertAccountIntoDatabase: " + account + ", skipping since the account already exists");
                        return false;
                    }
                    if (userAccounts.accountsDb.findAllDeAccounts().size() > 100) {
                        Log.w(TAG, "insertAccountIntoDatabase: " + account + ", skipping since more than 50 accounts on device exist");
                        return false;
                    }
                    long jInsertCeAccount = userAccounts.accountsDb.insertCeAccount(account, str);
                    if (jInsertCeAccount < 0) {
                        Log.w(TAG, "insertAccountIntoDatabase: " + account + ", skipping the DB insert failed");
                        return false;
                    }
                    if (userAccounts.accountsDb.insertDeAccount(account, jInsertCeAccount) < 0) {
                        Log.w(TAG, "insertAccountIntoDatabase: " + account + ", skipping the DB insert failed");
                        return false;
                    }
                    if (bundle != null) {
                        for (String str2 : bundle.keySet()) {
                            if (userAccounts.accountsDb.insertExtra(jInsertCeAccount, str2, bundle.getString(str2)) < 0) {
                                Log.w(TAG, "insertAccountIntoDatabase: " + account + ", skipping since insertExtra failed for key " + str2);
                                return false;
                            }
                        }
                    }
                    if (map != null) {
                        for (Map.Entry<String, Integer> entry : map.entrySet()) {
                            setAccountVisibility(account, entry.getKey(), entry.getValue().intValue(), false, userAccounts);
                        }
                    }
                    userAccounts.accountsDb.setTransactionSuccessful();
                    logRecord(AccountsDb.DEBUG_ACTION_ACCOUNT_ADD, "accounts", jInsertCeAccount, userAccounts, i);
                    insertAccountIntoCacheLocked(userAccounts, account);
                    if (getUserManager().getUserInfo(userAccounts.userId).canHaveProfile()) {
                        addAccountToLinkedRestrictedUsers(account, userAccounts.userId);
                    }
                    sendNotificationAccountUpdated(account, userAccounts);
                    sendAccountsChangedBroadcast(userAccounts.userId);
                    return true;
                } finally {
                    userAccounts.accountsDb.endTransaction();
                }
            }
        }
    }

    private boolean isLocalUnlockedUser(int i) {
        boolean z;
        synchronized (this.mUsers) {
            z = this.mLocalUnlockedUsers.get(i);
        }
        return z;
    }

    private void addAccountToLinkedRestrictedUsers(Account account, int i) {
        for (UserInfo userInfo : getUserManager().getUsers()) {
            if (userInfo.isRestricted() && i == userInfo.restrictedProfileParentId) {
                addSharedAccountAsUser(account, userInfo.id);
                if (isLocalUnlockedUser(userInfo.id)) {
                    this.mHandler.sendMessage(this.mHandler.obtainMessage(4, i, userInfo.id, account));
                }
            }
        }
    }

    public void hasFeatures(IAccountManagerResponse iAccountManagerResponse, Account account, String[] strArr, String str) {
        int callingUid = Binder.getCallingUid();
        this.mAppOpsManager.checkPackage(callingUid, str);
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "hasFeatures: " + account + ", response " + iAccountManagerResponse + ", features " + Arrays.toString(strArr) + ", caller's uid " + callingUid + ", pid " + Binder.getCallingPid());
        }
        Preconditions.checkArgument(account != null, "account cannot be null");
        Preconditions.checkArgument(iAccountManagerResponse != null, "response cannot be null");
        Preconditions.checkArgument(strArr != null, "features cannot be null");
        int callingUserId = UserHandle.getCallingUserId();
        checkReadAccountsPermitted(callingUid, account.type, callingUserId, str);
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            new TestFeaturesSession(getUserAccounts(callingUserId), iAccountManagerResponse, account, strArr).bind();
        } finally {
            restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private class TestFeaturesSession extends Session {
        private final Account mAccount;
        private final String[] mFeatures;

        public TestFeaturesSession(UserAccounts userAccounts, IAccountManagerResponse iAccountManagerResponse, Account account, String[] strArr) {
            super(AccountManagerService.this, userAccounts, iAccountManagerResponse, account.type, false, true, account.name, false);
            this.mFeatures = strArr;
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
        public void onResult(Bundle bundle) {
            Bundle.setDefusable(bundle, true);
            IAccountManagerResponse responseAndClose = getResponseAndClose();
            if (responseAndClose != null) {
                try {
                    if (bundle != null) {
                        if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                            Log.v(AccountManagerService.TAG, getClass().getSimpleName() + " calling onResult() on response " + responseAndClose);
                        }
                        Bundle bundle2 = new Bundle();
                        bundle2.putBoolean("booleanResult", bundle.getBoolean("booleanResult", false));
                        responseAndClose.onResult(bundle2);
                        return;
                    }
                    responseAndClose.onError(5, "null bundle");
                } catch (RemoteException e) {
                    if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                        Log.v(AccountManagerService.TAG, "failure while notifying response", e);
                    }
                }
            }
        }

        @Override
        protected String toDebugString(long j) {
            StringBuilder sb = new StringBuilder();
            sb.append(super.toDebugString(j));
            sb.append(", hasFeatures, ");
            sb.append(this.mAccount);
            sb.append(", ");
            sb.append(this.mFeatures != null ? TextUtils.join(",", this.mFeatures) : null);
            return sb.toString();
        }
    }

    public void renameAccount(IAccountManagerResponse iAccountManagerResponse, Account account, String str) {
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "renameAccount: " + account + " -> " + str + ", caller's uid " + callingUid + ", pid " + Binder.getCallingPid());
        }
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        int callingUserId = UserHandle.getCallingUserId();
        if (!isAccountManagedByCaller(account.type, callingUid, callingUserId)) {
            throw new SecurityException(String.format("uid %s cannot rename accounts of type: %s", Integer.valueOf(callingUid), account.type));
        }
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            Account accountRenameAccountInternal = renameAccountInternal(getUserAccounts(callingUserId), account, str);
            Bundle bundle = new Bundle();
            bundle.putString("authAccount", accountRenameAccountInternal.name);
            bundle.putString("accountType", accountRenameAccountInternal.type);
            bundle.putString("accountAccessId", accountRenameAccountInternal.getAccessId());
            try {
                iAccountManagerResponse.onResult(bundle);
            } catch (RemoteException e) {
                Log.w(TAG, e.getMessage());
            }
        } finally {
            restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private Account renameAccountInternal(UserAccounts userAccounts, Account account, String str) {
        cancelNotification(getSigninRequiredNotificationId(userAccounts, account), new UserHandle(userAccounts.userId));
        synchronized (userAccounts.credentialsPermissionNotificationIds) {
            for (Pair pair : userAccounts.credentialsPermissionNotificationIds.keySet()) {
                if (account.equals(((Pair) pair.first).first)) {
                    cancelNotification((NotificationId) userAccounts.credentialsPermissionNotificationIds.get(pair), new UserHandle(userAccounts.userId));
                }
            }
        }
        synchronized (userAccounts.dbLock) {
            synchronized (userAccounts.cacheLock) {
                List<String> accountRemovedReceivers = getAccountRemovedReceivers(account, userAccounts);
                userAccounts.accountsDb.beginTransaction();
                Account account2 = new Account(str, account.type);
                try {
                    if (userAccounts.accountsDb.findCeAccountId(account2) >= 0) {
                        Log.e(TAG, "renameAccount failed - account with new name already exists");
                        return null;
                    }
                    long jFindDeAccountId = userAccounts.accountsDb.findDeAccountId(account);
                    if (jFindDeAccountId < 0) {
                        Log.e(TAG, "renameAccount failed - old account does not exist");
                        return null;
                    }
                    userAccounts.accountsDb.renameCeAccount(jFindDeAccountId, str);
                    if (!userAccounts.accountsDb.renameDeAccount(jFindDeAccountId, str, account.name)) {
                        Log.e(TAG, "renameAccount failed");
                        return null;
                    }
                    userAccounts.accountsDb.setTransactionSuccessful();
                    userAccounts.accountsDb.endTransaction();
                    Account accountInsertAccountIntoCacheLocked = insertAccountIntoCacheLocked(userAccounts, account2);
                    Map map = (Map) userAccounts.userDataCache.get(account);
                    Map map2 = (Map) userAccounts.authTokenCache.get(account);
                    Map map3 = (Map) userAccounts.visibilityCache.get(account);
                    removeAccountFromCacheLocked(userAccounts, account);
                    userAccounts.userDataCache.put(accountInsertAccountIntoCacheLocked, map);
                    userAccounts.authTokenCache.put(accountInsertAccountIntoCacheLocked, map2);
                    userAccounts.visibilityCache.put(accountInsertAccountIntoCacheLocked, map3);
                    userAccounts.previousNameCache.put(accountInsertAccountIntoCacheLocked, new AtomicReference(account.name));
                    int i = userAccounts.userId;
                    if (canHaveProfile(i)) {
                        for (UserInfo userInfo : getUserManager().getUsers(true)) {
                            if (userInfo.isRestricted() && userInfo.restrictedProfileParentId == i) {
                                renameSharedAccountAsUser(account, str, userInfo.id);
                            }
                        }
                    }
                    sendNotificationAccountUpdated(accountInsertAccountIntoCacheLocked, userAccounts);
                    sendAccountsChangedBroadcast(userAccounts.userId);
                    Iterator<String> it = accountRemovedReceivers.iterator();
                    while (it.hasNext()) {
                        sendAccountRemovedBroadcast(account, it.next(), userAccounts.userId);
                    }
                    return accountInsertAccountIntoCacheLocked;
                } finally {
                    userAccounts.accountsDb.endTransaction();
                }
            }
        }
    }

    private boolean canHaveProfile(int i) {
        UserInfo userInfo = getUserManager().getUserInfo(i);
        return userInfo != null && userInfo.canHaveProfile();
    }

    public void removeAccount(IAccountManagerResponse iAccountManagerResponse, Account account, boolean z) {
        removeAccountAsUser(iAccountManagerResponse, account, z, UserHandle.getCallingUserId());
    }

    public void removeAccountAsUser(IAccountManagerResponse iAccountManagerResponse, Account account, boolean z, int i) {
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "removeAccount: " + account + ", response " + iAccountManagerResponse + ", caller's uid " + callingUid + ", pid " + Binder.getCallingPid() + ", for user id " + i);
        }
        Preconditions.checkArgument(account != null, "account cannot be null");
        Preconditions.checkArgument(iAccountManagerResponse != null, "response cannot be null");
        if (isCrossUser(callingUid, i)) {
            throw new SecurityException(String.format("User %s tying remove account for %s", Integer.valueOf(UserHandle.getCallingUserId()), Integer.valueOf(i)));
        }
        UserHandle userHandleOf = UserHandle.of(i);
        if (!isAccountManagedByCaller(account.type, callingUid, userHandleOf.getIdentifier()) && !isSystemUid(callingUid) && !isProfileOwner(callingUid)) {
            throw new SecurityException(String.format("uid %s cannot remove accounts of type: %s", Integer.valueOf(callingUid), account.type));
        }
        if (canUserModifyAccounts(i, callingUid)) {
            if (!canUserModifyAccountsForType(i, account.type, callingUid)) {
                try {
                    iAccountManagerResponse.onError(101, "User cannot modify accounts of this type (policy).");
                    return;
                } catch (RemoteException e) {
                    return;
                }
            }
            long jClearCallingIdentity = clearCallingIdentity();
            UserAccounts userAccounts = getUserAccounts(i);
            cancelNotification(getSigninRequiredNotificationId(userAccounts, account), userHandleOf);
            synchronized (userAccounts.credentialsPermissionNotificationIds) {
                for (Pair pair : userAccounts.credentialsPermissionNotificationIds.keySet()) {
                    if (account.equals(((Pair) pair.first).first)) {
                        cancelNotification((NotificationId) userAccounts.credentialsPermissionNotificationIds.get(pair), userHandleOf);
                    }
                }
            }
            logRecord(AccountsDb.DEBUG_ACTION_CALLED_ACCOUNT_REMOVE, "accounts", userAccounts.accountsDb.findDeAccountId(account), userAccounts, callingUid);
            try {
                new RemoveAccountSession(userAccounts, iAccountManagerResponse, account, z).bind();
                return;
            } finally {
                restoreCallingIdentity(jClearCallingIdentity);
            }
        }
        try {
            iAccountManagerResponse.onError(100, "User cannot modify accounts");
        } catch (RemoteException e2) {
        }
    }

    public boolean removeAccountExplicitly(Account account) {
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "removeAccountExplicitly: " + account + ", caller's uid " + callingUid + ", pid " + Binder.getCallingPid());
        }
        int identifier = Binder.getCallingUserHandle().getIdentifier();
        if (account == null) {
            Log.e(TAG, "account is null");
            return false;
        }
        if (!isAccountManagedByCaller(account.type, callingUid, identifier)) {
            throw new SecurityException(String.format("uid %s cannot explicitly remove accounts of type: %s", Integer.valueOf(callingUid), account.type));
        }
        UserAccounts userAccountsForCaller = getUserAccountsForCaller();
        logRecord(AccountsDb.DEBUG_ACTION_CALLED_ACCOUNT_REMOVE, "accounts", userAccountsForCaller.accountsDb.findDeAccountId(account), userAccountsForCaller, callingUid);
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            return removeAccountInternal(userAccountsForCaller, account, callingUid);
        } finally {
            restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private class RemoveAccountSession extends Session {
        final Account mAccount;

        public RemoveAccountSession(UserAccounts userAccounts, IAccountManagerResponse iAccountManagerResponse, Account account, boolean z) {
            super(AccountManagerService.this, userAccounts, iAccountManagerResponse, account.type, z, true, account.name, false);
            this.mAccount = account;
        }

        @Override
        protected String toDebugString(long j) {
            return super.toDebugString(j) + ", removeAccount, account " + this.mAccount;
        }

        @Override
        public void run() throws RemoteException {
            this.mAuthenticator.getAccountRemovalAllowed(this, this.mAccount);
        }

        @Override
        public void onResult(Bundle bundle) {
            Bundle.setDefusable(bundle, true);
            if (bundle != null && bundle.containsKey("booleanResult") && !bundle.containsKey("intent")) {
                if (bundle.getBoolean("booleanResult")) {
                    AccountManagerService.this.removeAccountInternal(this.mAccounts, this.mAccount, getCallingUid());
                }
                IAccountManagerResponse responseAndClose = getResponseAndClose();
                if (responseAndClose != null) {
                    if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                        Log.v(AccountManagerService.TAG, getClass().getSimpleName() + " calling onResult() on response " + responseAndClose);
                    }
                    try {
                        responseAndClose.onResult(bundle);
                    } catch (RemoteException e) {
                        Slog.e(AccountManagerService.TAG, "Error calling onResult()", e);
                    }
                }
            }
            super.onResult(bundle);
        }
    }

    @VisibleForTesting
    protected void removeAccountInternal(Account account) {
        removeAccountInternal(getUserAccountsForCaller(), account, getCallingUid());
    }

    private boolean removeAccountInternal(UserAccounts userAccounts, final Account account, int i) {
        boolean zDeleteDeAccount;
        boolean z;
        boolean zIsLocalUnlockedUser = isLocalUnlockedUser(userAccounts.userId);
        if (!zIsLocalUnlockedUser) {
            Slog.i(TAG, "Removing account " + account + " while user " + userAccounts.userId + " is still locked. CE data will be removed later");
        }
        synchronized (userAccounts.dbLock) {
            synchronized (userAccounts.cacheLock) {
                Map<String, Integer> requestingPackages = getRequestingPackages(account, userAccounts);
                List<String> accountRemovedReceivers = getAccountRemovedReceivers(account, userAccounts);
                userAccounts.accountsDb.beginTransaction();
                try {
                    long jFindDeAccountId = userAccounts.accountsDb.findDeAccountId(account);
                    if (jFindDeAccountId >= 0) {
                        zDeleteDeAccount = userAccounts.accountsDb.deleteDeAccount(jFindDeAccountId);
                    } else {
                        zDeleteDeAccount = false;
                    }
                    z = zDeleteDeAccount;
                    if (zIsLocalUnlockedUser) {
                        long jFindCeAccountId = userAccounts.accountsDb.findCeAccountId(account);
                        if (jFindCeAccountId >= 0) {
                            userAccounts.accountsDb.deleteCeAccount(jFindCeAccountId);
                        }
                    }
                    userAccounts.accountsDb.setTransactionSuccessful();
                    userAccounts.accountsDb.endTransaction();
                    if (z) {
                        removeAccountFromCacheLocked(userAccounts, account);
                        for (Map.Entry<String, Integer> entry : requestingPackages.entrySet()) {
                            if (entry.getValue().intValue() == 1 || entry.getValue().intValue() == 2) {
                                notifyPackage(entry.getKey(), userAccounts);
                            }
                        }
                        sendAccountsChangedBroadcast(userAccounts.userId);
                        Iterator<String> it = accountRemovedReceivers.iterator();
                        while (it.hasNext()) {
                            sendAccountRemovedBroadcast(account, it.next(), userAccounts.userId);
                        }
                        logRecord(zIsLocalUnlockedUser ? AccountsDb.DEBUG_ACTION_ACCOUNT_REMOVE : AccountsDb.DEBUG_ACTION_ACCOUNT_REMOVE_DE, "accounts", jFindDeAccountId, userAccounts);
                    }
                } catch (Throwable th) {
                    userAccounts.accountsDb.endTransaction();
                    throw th;
                }
            }
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            int i2 = userAccounts.userId;
            if (canHaveProfile(i2)) {
                for (UserInfo userInfo : getUserManager().getUsers(true)) {
                    if (userInfo.isRestricted() && i2 == userInfo.restrictedProfileParentId) {
                        removeSharedAccountAsUser(account, userInfo.id, i);
                    }
                }
            }
            if (z) {
                synchronized (userAccounts.credentialsPermissionNotificationIds) {
                    for (Pair pair : userAccounts.credentialsPermissionNotificationIds.keySet()) {
                        if (account.equals(((Pair) pair.first).first) && "com.android.AccountManager.ACCOUNT_ACCESS_TOKEN_TYPE".equals(((Pair) pair.first).second)) {
                            final int iIntValue = ((Integer) pair.second).intValue();
                            this.mHandler.post(new Runnable() {
                                @Override
                                public final void run() {
                                    this.f$0.cancelAccountAccessRequestNotificationIfNeeded(account, iIntValue, false);
                                }
                            });
                        }
                    }
                }
            }
            return z;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void invalidateAuthToken(String str, String str2) {
        int callingUid = Binder.getCallingUid();
        Preconditions.checkNotNull(str, "accountType cannot be null");
        Preconditions.checkNotNull(str2, "authToken cannot be null");
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "invalidateAuthToken: accountType " + str + ", caller's uid " + callingUid + ", pid " + Binder.getCallingPid());
        }
        int callingUserId = UserHandle.getCallingUserId();
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            UserAccounts userAccounts = getUserAccounts(callingUserId);
            synchronized (userAccounts.dbLock) {
                userAccounts.accountsDb.beginTransaction();
                try {
                    List<Pair<Account, String>> listInvalidateAuthTokenLocked = invalidateAuthTokenLocked(userAccounts, str, str2);
                    userAccounts.accountsDb.setTransactionSuccessful();
                    userAccounts.accountsDb.endTransaction();
                    synchronized (userAccounts.cacheLock) {
                        for (Pair<Account, String> pair : listInvalidateAuthTokenLocked) {
                            writeAuthTokenIntoCacheLocked(userAccounts, (Account) pair.first, (String) pair.second, null);
                        }
                        userAccounts.accountTokenCaches.remove(str, str2);
                    }
                } catch (Throwable th) {
                    userAccounts.accountsDb.endTransaction();
                    throw th;
                }
            }
        } finally {
            restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private List<Pair<Account, String>> invalidateAuthTokenLocked(UserAccounts userAccounts, String str, String str2) {
        ArrayList arrayList = new ArrayList();
        Cursor cursorFindAuthtokenForAllAccounts = userAccounts.accountsDb.findAuthtokenForAllAccounts(str, str2);
        while (cursorFindAuthtokenForAllAccounts.moveToNext()) {
            try {
                String string = cursorFindAuthtokenForAllAccounts.getString(0);
                String string2 = cursorFindAuthtokenForAllAccounts.getString(1);
                String string3 = cursorFindAuthtokenForAllAccounts.getString(2);
                userAccounts.accountsDb.deleteAuthToken(string);
                arrayList.add(Pair.create(new Account(string2, str), string3));
            } finally {
                cursorFindAuthtokenForAllAccounts.close();
            }
        }
        return arrayList;
    }

    private void saveCachedToken(UserAccounts userAccounts, Account account, String str, byte[] bArr, String str2, String str3, long j) {
        if (account == null || str2 == null || str == null || bArr == null) {
            return;
        }
        cancelNotification(getSigninRequiredNotificationId(userAccounts, account), UserHandle.of(userAccounts.userId));
        synchronized (userAccounts.cacheLock) {
            userAccounts.accountTokenCaches.put(account, str3, str2, str, bArr, j);
        }
    }

    private boolean saveAuthTokenToDatabase(UserAccounts userAccounts, Account account, String str, String str2) {
        if (account == null || str == null) {
            return false;
        }
        cancelNotification(getSigninRequiredNotificationId(userAccounts, account), UserHandle.of(userAccounts.userId));
        synchronized (userAccounts.dbLock) {
            userAccounts.accountsDb.beginTransaction();
            try {
                long jFindDeAccountId = userAccounts.accountsDb.findDeAccountId(account);
                if (jFindDeAccountId < 0) {
                    return false;
                }
                userAccounts.accountsDb.deleteAuthtokensByAccountIdAndType(jFindDeAccountId, str);
                if (userAccounts.accountsDb.insertAuthToken(jFindDeAccountId, str, str2) < 0) {
                    return false;
                }
                userAccounts.accountsDb.setTransactionSuccessful();
                userAccounts.accountsDb.endTransaction();
                synchronized (userAccounts.cacheLock) {
                    writeAuthTokenIntoCacheLocked(userAccounts, account, str, str2);
                }
                return true;
            } finally {
                userAccounts.accountsDb.endTransaction();
            }
        }
    }

    public String peekAuthToken(Account account, String str) {
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "peekAuthToken: " + account + ", authTokenType " + str + ", caller's uid " + callingUid + ", pid " + Binder.getCallingPid());
        }
        Preconditions.checkNotNull(account, "account cannot be null");
        Preconditions.checkNotNull(str, "authTokenType cannot be null");
        int callingUserId = UserHandle.getCallingUserId();
        if (!isAccountManagedByCaller(account.type, callingUid, callingUserId)) {
            throw new SecurityException(String.format("uid %s cannot peek the authtokens associated with accounts of type: %s", Integer.valueOf(callingUid), account.type));
        }
        if (!isLocalUnlockedUser(callingUserId)) {
            Log.w(TAG, "Authtoken not available - user " + callingUserId + " data is locked. callingUid " + callingUid);
            return null;
        }
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            return readAuthTokenInternal(getUserAccounts(callingUserId), account, str);
        } finally {
            restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void setAuthToken(Account account, String str, String str2) {
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "setAuthToken: " + account + ", authTokenType " + str + ", caller's uid " + callingUid + ", pid " + Binder.getCallingPid());
        }
        Preconditions.checkNotNull(account, "account cannot be null");
        Preconditions.checkNotNull(str, "authTokenType cannot be null");
        int callingUserId = UserHandle.getCallingUserId();
        if (!isAccountManagedByCaller(account.type, callingUid, callingUserId)) {
            throw new SecurityException(String.format("uid %s cannot set auth tokens associated with accounts of type: %s", Integer.valueOf(callingUid), account.type));
        }
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            saveAuthTokenToDatabase(getUserAccounts(callingUserId), account, str, str2);
        } finally {
            restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void setPassword(Account account, String str) {
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "setAuthToken: " + account + ", caller's uid " + callingUid + ", pid " + Binder.getCallingPid());
        }
        Preconditions.checkNotNull(account, "account cannot be null");
        int callingUserId = UserHandle.getCallingUserId();
        if (!isAccountManagedByCaller(account.type, callingUid, callingUserId)) {
            throw new SecurityException(String.format("uid %s cannot set secrets for accounts of type: %s", Integer.valueOf(callingUid), account.type));
        }
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            setPasswordInternal(getUserAccounts(callingUserId), account, str, callingUid);
        } finally {
            restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void setPasswordInternal(UserAccounts userAccounts, Account account, String str, int i) {
        String str2;
        if (account == null) {
            return;
        }
        boolean z = false;
        synchronized (userAccounts.dbLock) {
            synchronized (userAccounts.cacheLock) {
                userAccounts.accountsDb.beginTransaction();
                try {
                    long jFindDeAccountId = userAccounts.accountsDb.findDeAccountId(account);
                    if (jFindDeAccountId >= 0) {
                        userAccounts.accountsDb.updateCeAccountPassword(jFindDeAccountId, str);
                        userAccounts.accountsDb.deleteAuthTokensByAccountId(jFindDeAccountId);
                        userAccounts.authTokenCache.remove(account);
                        userAccounts.accountTokenCaches.remove(account);
                        userAccounts.accountsDb.setTransactionSuccessful();
                        z = true;
                        if (str == null || str.length() == 0) {
                            str2 = AccountsDb.DEBUG_ACTION_CLEAR_PASSWORD;
                        } else {
                            str2 = AccountsDb.DEBUG_ACTION_SET_PASSWORD;
                        }
                        logRecord(str2, "accounts", jFindDeAccountId, userAccounts, i);
                    }
                } finally {
                    userAccounts.accountsDb.endTransaction();
                    if (z) {
                        sendNotificationAccountUpdated(account, userAccounts);
                        sendAccountsChangedBroadcast(userAccounts.userId);
                    }
                }
            }
        }
    }

    public void clearPassword(Account account) {
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "clearPassword: " + account + ", caller's uid " + callingUid + ", pid " + Binder.getCallingPid());
        }
        Preconditions.checkNotNull(account, "account cannot be null");
        int callingUserId = UserHandle.getCallingUserId();
        if (!isAccountManagedByCaller(account.type, callingUid, callingUserId)) {
            throw new SecurityException(String.format("uid %s cannot clear passwords for accounts of type: %s", Integer.valueOf(callingUid), account.type));
        }
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            setPasswordInternal(getUserAccounts(callingUserId), account, null, callingUid);
        } finally {
            restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void setUserData(Account account, String str, String str2) {
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "setUserData: " + account + ", key " + str + ", caller's uid " + callingUid + ", pid " + Binder.getCallingPid());
        }
        if (str == null) {
            throw new IllegalArgumentException("key is null");
        }
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        int callingUserId = UserHandle.getCallingUserId();
        if (!isAccountManagedByCaller(account.type, callingUid, callingUserId)) {
            throw new SecurityException(String.format("uid %s cannot set user data for accounts of type: %s", Integer.valueOf(callingUid), account.type));
        }
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            UserAccounts userAccounts = getUserAccounts(callingUserId);
            if (!accountExistsCache(userAccounts, account)) {
                return;
            }
            setUserdataInternal(userAccounts, account, str, str2);
        } finally {
            restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private boolean accountExistsCache(UserAccounts userAccounts, Account account) {
        synchronized (userAccounts.cacheLock) {
            if (userAccounts.accountCache.containsKey(account.type)) {
                for (Account account2 : userAccounts.accountCache.get(account.type)) {
                    if (account2.name.equals(account.name)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    private void setUserdataInternal(UserAccounts userAccounts, Account account, String str, String str2) {
        synchronized (userAccounts.dbLock) {
            userAccounts.accountsDb.beginTransaction();
            try {
                long jFindDeAccountId = userAccounts.accountsDb.findDeAccountId(account);
                if (jFindDeAccountId < 0) {
                    return;
                }
                long jFindExtrasIdByAccountId = userAccounts.accountsDb.findExtrasIdByAccountId(jFindDeAccountId, str);
                if (jFindExtrasIdByAccountId < 0) {
                    if (userAccounts.accountsDb.insertExtra(jFindDeAccountId, str, str2) < 0) {
                        return;
                    }
                } else if (!userAccounts.accountsDb.updateExtra(jFindExtrasIdByAccountId, str2)) {
                    return;
                }
                userAccounts.accountsDb.setTransactionSuccessful();
                userAccounts.accountsDb.endTransaction();
                synchronized (userAccounts.cacheLock) {
                    writeUserDataIntoCacheLocked(userAccounts, account, str, str2);
                }
            } finally {
                userAccounts.accountsDb.endTransaction();
            }
        }
    }

    private void onResult(IAccountManagerResponse iAccountManagerResponse, Bundle bundle) {
        if (bundle == null) {
            Log.e(TAG, "the result is unexpectedly null", new Exception());
        }
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, getClass().getSimpleName() + " calling onResult() on response " + iAccountManagerResponse);
        }
        try {
            iAccountManagerResponse.onResult(bundle);
        } catch (RemoteException e) {
            if (Log.isLoggable(TAG, 2)) {
                Log.v(TAG, "failure while notifying response", e);
            }
        }
    }

    public void getAuthTokenLabel(IAccountManagerResponse iAccountManagerResponse, final String str, final String str2) throws RemoteException {
        Preconditions.checkArgument(str != null, "accountType cannot be null");
        Preconditions.checkArgument(str2 != null, "authTokenType cannot be null");
        int callingUid = getCallingUid();
        clearCallingIdentity();
        if (UserHandle.getAppId(callingUid) != 1000) {
            throw new SecurityException("can only call from system");
        }
        int userId = UserHandle.getUserId(callingUid);
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            new Session(getUserAccounts(userId), iAccountManagerResponse, str, false, false, null, false) {
                @Override
                protected String toDebugString(long j) {
                    return super.toDebugString(j) + ", getAuthTokenLabel, " + str + ", authTokenType " + str2;
                }

                @Override
                public void run() throws RemoteException {
                    this.mAuthenticator.getAuthTokenLabel(this, str2);
                }

                @Override
                public void onResult(Bundle bundle) {
                    Bundle.setDefusable(bundle, true);
                    if (bundle != null) {
                        String string = bundle.getString("authTokenLabelKey");
                        Bundle bundle2 = new Bundle();
                        bundle2.putString("authTokenLabelKey", string);
                        super.onResult(bundle2);
                        return;
                    }
                    super.onResult(bundle);
                }
            }.bind();
        } finally {
            restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void getAuthToken(IAccountManagerResponse iAccountManagerResponse, final Account account, final String str, final boolean z, boolean z2, final Bundle bundle) throws Throwable {
        boolean z3;
        long j;
        String str2;
        long j2;
        String authTokenInternal;
        Bundle.setDefusable(bundle, true);
        if (Log.isLoggable(TAG, 2)) {
            StringBuilder sb = new StringBuilder();
            sb.append("getAuthToken: ");
            sb.append(account);
            sb.append(", response ");
            sb.append(iAccountManagerResponse);
            sb.append(", authTokenType ");
            sb.append(str);
            sb.append(", notifyOnAuthFailure ");
            sb.append(z);
            sb.append(", expectActivityLaunch ");
            z3 = z2;
            sb.append(z3);
            sb.append(", caller's uid ");
            sb.append(Binder.getCallingUid());
            sb.append(", pid ");
            sb.append(Binder.getCallingPid());
            Log.v(TAG, sb.toString());
        } else {
            z3 = z2;
        }
        Preconditions.checkArgument(iAccountManagerResponse != null, "response cannot be null");
        try {
            if (account == null) {
                Slog.w(TAG, "getAuthToken called with null account");
                iAccountManagerResponse.onError(7, "account is null");
                return;
            }
            if (str == null) {
                Slog.w(TAG, "getAuthToken called with null authTokenType");
                iAccountManagerResponse.onError(7, "authTokenType is null");
                return;
            }
            int callingUserId = UserHandle.getCallingUserId();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                final UserAccounts userAccounts = getUserAccounts(callingUserId);
                RegisteredServicesCache.ServiceInfo<AuthenticatorDescription> serviceInfo = this.mAuthenticatorCache.getServiceInfo(AuthenticatorDescription.newKey(account.type), userAccounts.userId);
                final boolean z4 = serviceInfo != null && ((AuthenticatorDescription) serviceInfo.type).customTokens;
                final int callingUid = Binder.getCallingUid();
                boolean z5 = z4 || permissionIsGranted(account, str, callingUid, callingUserId);
                String string = bundle.getString("androidPackageName");
                jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    List listAsList = Arrays.asList(this.mPackageManager.getPackagesForUid(callingUid));
                    if (string == null || !listAsList.contains(string)) {
                        throw new SecurityException(String.format("Uid %s is attempting to illegally masquerade as package %s!", Integer.valueOf(callingUid), string));
                    }
                    bundle.putInt("callerUid", callingUid);
                    bundle.putInt("callerPid", Binder.getCallingPid());
                    if (z) {
                        bundle.putBoolean("notifyOnAuthFailure", true);
                    }
                    long jClearCallingIdentity2 = clearCallingIdentity();
                    try {
                        final byte[] bArrCalculatePackageSignatureDigest = calculatePackageSignatureDigest(string);
                        if (!z4 && z5 && (authTokenInternal = readAuthTokenInternal(userAccounts, account, str)) != null) {
                            Bundle bundle2 = new Bundle();
                            bundle2.putString("authtoken", authTokenInternal);
                            bundle2.putString("authAccount", account.name);
                            bundle2.putString("accountType", account.type);
                            onResult(iAccountManagerResponse, bundle2);
                            restoreCallingIdentity(jClearCallingIdentity2);
                            return;
                        }
                        if (z4) {
                            str2 = string;
                            try {
                                String cachedTokenInternal = readCachedTokenInternal(userAccounts, account, str, string, bArrCalculatePackageSignatureDigest);
                                if (cachedTokenInternal != null) {
                                    if (Log.isLoggable(TAG, 2)) {
                                        Log.v(TAG, "getAuthToken: cache hit ofr custom token authenticator.");
                                    }
                                    Bundle bundle3 = new Bundle();
                                    bundle3.putString("authtoken", cachedTokenInternal);
                                    bundle3.putString("authAccount", account.name);
                                    bundle3.putString("accountType", account.type);
                                    onResult(iAccountManagerResponse, bundle3);
                                    restoreCallingIdentity(jClearCallingIdentity2);
                                    return;
                                }
                                j2 = jClearCallingIdentity2;
                            } catch (Throwable th) {
                                th = th;
                                j = jClearCallingIdentity2;
                            }
                        } else {
                            str2 = string;
                            j2 = jClearCallingIdentity2;
                        }
                        try {
                            long j3 = j2;
                            final boolean z6 = z5;
                            final String str3 = str2;
                            try {
                                new Session(userAccounts, iAccountManagerResponse, account.type, z3, false, account.name, false) {
                                    @Override
                                    protected String toDebugString(long j4) {
                                        if (bundle != null) {
                                            bundle.keySet();
                                        }
                                        return super.toDebugString(j4) + ", getAuthToken, " + account + ", authTokenType " + str + ", loginOptions " + bundle + ", notifyOnAuthFailure " + z;
                                    }

                                    @Override
                                    public void run() throws RemoteException {
                                        if (!z6) {
                                            this.mAuthenticator.getAuthTokenLabel(this, str);
                                        } else {
                                            this.mAuthenticator.getAuthToken(this, account, str, bundle);
                                        }
                                    }

                                    @Override
                                    public void onResult(Bundle bundle4) {
                                        Bundle.setDefusable(bundle4, true);
                                        if (bundle4 != null) {
                                            if (bundle4.containsKey("authTokenLabelKey")) {
                                                Intent intentNewGrantCredentialsPermissionIntent = AccountManagerService.this.newGrantCredentialsPermissionIntent(account, null, callingUid, new AccountAuthenticatorResponse((IAccountAuthenticatorResponse) this), str, true);
                                                Bundle bundle5 = new Bundle();
                                                bundle5.putParcelable("intent", intentNewGrantCredentialsPermissionIntent);
                                                onResult(bundle5);
                                                return;
                                            }
                                            String string2 = bundle4.getString("authtoken");
                                            if (string2 != null) {
                                                String string3 = bundle4.getString("authAccount");
                                                String string4 = bundle4.getString("accountType");
                                                if (TextUtils.isEmpty(string4) || TextUtils.isEmpty(string3)) {
                                                    onError(5, "the type and name should not be empty");
                                                    return;
                                                }
                                                Account account2 = new Account(string3, string4);
                                                if (!z4) {
                                                    AccountManagerService.this.saveAuthTokenToDatabase(this.mAccounts, account2, str, string2);
                                                }
                                                long j4 = bundle4.getLong("android.accounts.expiry", 0L);
                                                if (z4 && j4 > System.currentTimeMillis()) {
                                                    AccountManagerService.this.saveCachedToken(this.mAccounts, account, str3, bArrCalculatePackageSignatureDigest, str, string2, j4);
                                                }
                                            }
                                            Intent intent = (Intent) bundle4.getParcelable("intent");
                                            if (intent != null && z && !z4) {
                                                if (checkKeyIntent(Binder.getCallingUid(), intent)) {
                                                    AccountManagerService.this.doNotification(this.mAccounts, account, bundle4.getString("authFailedMessage"), intent, PackageManagerService.PLATFORM_PACKAGE_NAME, userAccounts.userId);
                                                } else {
                                                    onError(5, "invalid intent in bundle returned");
                                                    return;
                                                }
                                            }
                                        }
                                        super.onResult(bundle4);
                                    }
                                }.bind();
                                restoreCallingIdentity(j3);
                                return;
                            } catch (Throwable th2) {
                                th = th2;
                                j = j3;
                            }
                        } catch (Throwable th3) {
                            th = th3;
                            j = j2;
                        }
                    } catch (Throwable th4) {
                        th = th4;
                        j = jClearCallingIdentity2;
                    }
                    restoreCallingIdentity(j);
                    throw th;
                } finally {
                }
            } finally {
            }
        } catch (RemoteException e) {
            Slog.w(TAG, "Failed to report error back to the client." + e);
        }
    }

    private byte[] calculatePackageSignatureDigest(String str) {
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
            for (Signature signature : this.mPackageManager.getPackageInfo(str, 64).signatures) {
                messageDigest.update(signature.toByteArray());
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Could not find packageinfo for: " + str);
            messageDigest = null;
        } catch (NoSuchAlgorithmException e2) {
            Log.wtf(TAG, "SHA-256 should be available", e2);
            messageDigest = null;
        }
        if (messageDigest == null) {
            return null;
        }
        return messageDigest.digest();
    }

    private void createNoCredentialsPermissionNotification(Account account, Intent intent, String str, int i) {
        int intExtra = intent.getIntExtra(WatchlistLoggingHandler.WatchlistEventKeys.UID, -1);
        String stringExtra = intent.getStringExtra("authTokenType");
        String string = this.mContext.getString(R.string.httpError, getApplicationLabel(str), account.name);
        int iIndexOf = string.indexOf(10);
        String strSubstring = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        if (iIndexOf > 0) {
            String strSubstring2 = string.substring(0, iIndexOf);
            strSubstring = string.substring(iIndexOf + 1);
            string = strSubstring2;
        }
        UserHandle userHandleOf = UserHandle.of(i);
        Context contextForUser = getContextForUser(userHandleOf);
        installNotification(getCredentialPermissionNotificationId(account, stringExtra, intExtra), new Notification.Builder(contextForUser, SystemNotificationChannels.ACCOUNT).setSmallIcon(R.drawable.stat_sys_warning).setWhen(0L).setColor(contextForUser.getColor(R.color.car_colorPrimary)).setContentTitle(string).setContentText(strSubstring).setContentIntent(PendingIntent.getActivityAsUser(this.mContext, 0, intent, 268435456, null, userHandleOf)).build(), PackageManagerService.PLATFORM_PACKAGE_NAME, userHandleOf.getIdentifier());
    }

    private String getApplicationLabel(String str) {
        try {
            return this.mPackageManager.getApplicationLabel(this.mPackageManager.getApplicationInfo(str, 0)).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return str;
        }
    }

    private Intent newGrantCredentialsPermissionIntent(Account account, String str, int i, AccountAuthenticatorResponse accountAuthenticatorResponse, String str2, boolean z) {
        Intent intent = new Intent(this.mContext, (Class<?>) GrantCredentialsPermissionActivity.class);
        if (z) {
            intent.setFlags(268435456);
        }
        StringBuilder sb = new StringBuilder();
        sb.append(getCredentialPermissionNotificationId(account, str2, i).mTag);
        if (str == null) {
            str = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        }
        sb.append(str);
        intent.addCategory(sb.toString());
        intent.putExtra("account", account);
        intent.putExtra("authTokenType", str2);
        intent.putExtra("response", accountAuthenticatorResponse);
        intent.putExtra(WatchlistLoggingHandler.WatchlistEventKeys.UID, i);
        return intent;
    }

    private NotificationId getCredentialPermissionNotificationId(Account account, String str, int i) {
        NotificationId notificationId;
        UserAccounts userAccounts = getUserAccounts(UserHandle.getUserId(i));
        synchronized (userAccounts.credentialsPermissionNotificationIds) {
            Pair pair = new Pair(new Pair(account, str), Integer.valueOf(i));
            notificationId = (NotificationId) userAccounts.credentialsPermissionNotificationIds.get(pair);
            if (notificationId == null) {
                notificationId = new NotificationId("AccountManagerService:38:" + account.hashCode() + ":" + str.hashCode() + ":" + i, 38);
                userAccounts.credentialsPermissionNotificationIds.put(pair, notificationId);
            }
        }
        return notificationId;
    }

    private NotificationId getSigninRequiredNotificationId(UserAccounts userAccounts, Account account) {
        NotificationId notificationId;
        synchronized (userAccounts.signinRequiredNotificationIds) {
            notificationId = (NotificationId) userAccounts.signinRequiredNotificationIds.get(account);
            if (notificationId == null) {
                NotificationId notificationId2 = new NotificationId("AccountManagerService:37:" + account.hashCode(), 37);
                userAccounts.signinRequiredNotificationIds.put(account, notificationId2);
                notificationId = notificationId2;
            }
        }
        return notificationId;
    }

    public void addAccount(IAccountManagerResponse iAccountManagerResponse, final String str, String str2, final String[] strArr, boolean z, Bundle bundle) throws Throwable {
        final String str3;
        boolean z2;
        long j;
        Bundle bundle2 = bundle;
        Bundle.setDefusable(bundle2, true);
        if (Log.isLoggable(TAG, 2)) {
            StringBuilder sb = new StringBuilder();
            sb.append("addAccount: accountType ");
            sb.append(str);
            sb.append(", response ");
            sb.append(iAccountManagerResponse);
            sb.append(", authTokenType ");
            str3 = str2;
            sb.append(str3);
            sb.append(", requiredFeatures ");
            sb.append(Arrays.toString(strArr));
            sb.append(", expectActivityLaunch ");
            z2 = z;
            sb.append(z2);
            sb.append(", caller's uid ");
            sb.append(Binder.getCallingUid());
            sb.append(", pid ");
            sb.append(Binder.getCallingPid());
            Log.v(TAG, sb.toString());
        } else {
            str3 = str2;
            z2 = z;
        }
        if (iAccountManagerResponse == null) {
            throw new IllegalArgumentException("response is null");
        }
        if (str == null) {
            throw new IllegalArgumentException("accountType is null");
        }
        int callingUid = Binder.getCallingUid();
        int userId = UserHandle.getUserId(callingUid);
        if (!canUserModifyAccounts(userId, callingUid)) {
            try {
                iAccountManagerResponse.onError(100, "User is not allowed to add an account!");
            } catch (RemoteException e) {
            }
            showCantAddAccount(100, userId);
            return;
        }
        if (!canUserModifyAccountsForType(userId, str, callingUid)) {
            try {
                iAccountManagerResponse.onError(101, "User cannot modify accounts of this type (policy).");
            } catch (RemoteException e2) {
            }
            showCantAddAccount(101, userId);
            return;
        }
        int callingPid = Binder.getCallingPid();
        if (bundle2 == null) {
            bundle2 = new Bundle();
        }
        final Bundle bundle3 = bundle2;
        bundle3.putInt("callerUid", callingUid);
        bundle3.putInt("callerPid", callingPid);
        int callingUserId = UserHandle.getCallingUserId();
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            UserAccounts userAccounts = getUserAccounts(callingUserId);
            logRecordWithUid(userAccounts, AccountsDb.DEBUG_ACTION_CALLED_ACCOUNT_ADD, "accounts", callingUid);
            try {
                new Session(userAccounts, iAccountManagerResponse, str, z2, true, null, false, true) {
                    @Override
                    public void run() throws RemoteException {
                        this.mAuthenticator.addAccount(this, this.mAccountType, str3, strArr, bundle3);
                    }

                    @Override
                    protected String toDebugString(long j2) {
                        return super.toDebugString(j2) + ", addAccount, accountType " + str + ", requiredFeatures " + Arrays.toString(strArr);
                    }
                }.bind();
                restoreCallingIdentity(jClearCallingIdentity);
            } catch (Throwable th) {
                th = th;
                j = jClearCallingIdentity;
                restoreCallingIdentity(j);
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
            j = jClearCallingIdentity;
        }
    }

    public void addAccountAsUser(IAccountManagerResponse iAccountManagerResponse, final String str, String str2, final String[] strArr, boolean z, Bundle bundle, int i) throws Throwable {
        final String str3;
        boolean z2;
        long j;
        UserAccounts userAccounts;
        Bundle bundle2 = bundle;
        Bundle.setDefusable(bundle2, true);
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            StringBuilder sb = new StringBuilder();
            sb.append("addAccount: accountType ");
            sb.append(str);
            sb.append(", response ");
            sb.append(iAccountManagerResponse);
            sb.append(", authTokenType ");
            str3 = str2;
            sb.append(str3);
            sb.append(", requiredFeatures ");
            sb.append(Arrays.toString(strArr));
            sb.append(", expectActivityLaunch ");
            z2 = z;
            sb.append(z2);
            sb.append(", caller's uid ");
            sb.append(Binder.getCallingUid());
            sb.append(", pid ");
            sb.append(Binder.getCallingPid());
            sb.append(", for user id ");
            sb.append(i);
            Log.v(TAG, sb.toString());
        } else {
            str3 = str2;
            z2 = z;
        }
        Preconditions.checkArgument(iAccountManagerResponse != null, "response cannot be null");
        Preconditions.checkArgument(str != null, "accountType cannot be null");
        if (isCrossUser(callingUid, i)) {
            throw new SecurityException(String.format("User %s trying to add account for %s", Integer.valueOf(UserHandle.getCallingUserId()), Integer.valueOf(i)));
        }
        if (!canUserModifyAccounts(i, callingUid)) {
            try {
                iAccountManagerResponse.onError(100, "User is not allowed to add an account!");
            } catch (RemoteException e) {
            }
            showCantAddAccount(100, i);
            return;
        }
        if (!canUserModifyAccountsForType(i, str, callingUid)) {
            try {
                iAccountManagerResponse.onError(101, "User cannot modify accounts of this type (policy).");
            } catch (RemoteException e2) {
            }
            showCantAddAccount(101, i);
            return;
        }
        int callingPid = Binder.getCallingPid();
        int callingUid2 = Binder.getCallingUid();
        if (bundle2 == null) {
            bundle2 = new Bundle();
        }
        final Bundle bundle3 = bundle2;
        bundle3.putInt("callerUid", callingUid2);
        bundle3.putInt("callerPid", callingPid);
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            userAccounts = getUserAccounts(i);
            logRecordWithUid(userAccounts, AccountsDb.DEBUG_ACTION_CALLED_ACCOUNT_ADD, "accounts", i);
        } catch (Throwable th) {
            th = th;
            j = jClearCallingIdentity;
        }
        try {
            new Session(userAccounts, iAccountManagerResponse, str, z2, true, null, false, true) {
                @Override
                public void run() throws RemoteException {
                    this.mAuthenticator.addAccount(this, this.mAccountType, str3, strArr, bundle3);
                }

                @Override
                protected String toDebugString(long j2) {
                    String strJoin;
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append(super.toDebugString(j2));
                    sb2.append(", addAccount, accountType ");
                    sb2.append(str);
                    sb2.append(", requiredFeatures ");
                    if (strArr != null) {
                        strJoin = TextUtils.join(",", strArr);
                    } else {
                        strJoin = null;
                    }
                    sb2.append(strJoin);
                    return sb2.toString();
                }
            }.bind();
            restoreCallingIdentity(jClearCallingIdentity);
        } catch (Throwable th2) {
            th = th2;
            j = jClearCallingIdentity;
            restoreCallingIdentity(j);
            throw th;
        }
    }

    public void startAddAccountSession(IAccountManagerResponse iAccountManagerResponse, final String str, String str2, final String[] strArr, boolean z, Bundle bundle) throws Throwable {
        final String str3;
        boolean z2;
        final Bundle bundle2;
        long j;
        Bundle.setDefusable(bundle, true);
        if (Log.isLoggable(TAG, 2)) {
            StringBuilder sb = new StringBuilder();
            sb.append("startAddAccountSession: accountType ");
            sb.append(str);
            sb.append(", response ");
            sb.append(iAccountManagerResponse);
            sb.append(", authTokenType ");
            str3 = str2;
            sb.append(str3);
            sb.append(", requiredFeatures ");
            sb.append(Arrays.toString(strArr));
            sb.append(", expectActivityLaunch ");
            z2 = z;
            sb.append(z2);
            sb.append(", caller's uid ");
            sb.append(Binder.getCallingUid());
            sb.append(", pid ");
            sb.append(Binder.getCallingPid());
            Log.v(TAG, sb.toString());
        } else {
            str3 = str2;
            z2 = z;
        }
        Preconditions.checkArgument(iAccountManagerResponse != null, "response cannot be null");
        Preconditions.checkArgument(str != null, "accountType cannot be null");
        int callingUid = Binder.getCallingUid();
        int userId = UserHandle.getUserId(callingUid);
        if (!canUserModifyAccounts(userId, callingUid)) {
            try {
                iAccountManagerResponse.onError(100, "User is not allowed to add an account!");
            } catch (RemoteException e) {
            }
            showCantAddAccount(100, userId);
            return;
        }
        if (!canUserModifyAccountsForType(userId, str, callingUid)) {
            try {
                iAccountManagerResponse.onError(101, "User cannot modify accounts of this type (policy).");
            } catch (RemoteException e2) {
            }
            showCantAddAccount(101, userId);
            return;
        }
        int callingPid = Binder.getCallingPid();
        if (bundle != null) {
            bundle2 = bundle;
        } else {
            bundle2 = new Bundle();
        }
        bundle2.putInt("callerUid", callingUid);
        bundle2.putInt("callerPid", callingPid);
        boolean zIsPermitted = isPermitted(bundle.getString("androidPackageName"), callingUid, "android.permission.GET_PASSWORD");
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            UserAccounts userAccounts = getUserAccounts(userId);
            logRecordWithUid(userAccounts, AccountsDb.DEBUG_ACTION_CALLED_START_ACCOUNT_ADD, "accounts", callingUid);
            try {
                new StartAccountSession(userAccounts, iAccountManagerResponse, str, z2, null, false, true, zIsPermitted) {
                    @Override
                    public void run() throws RemoteException {
                        this.mAuthenticator.startAddAccountSession(this, this.mAccountType, str3, strArr, bundle2);
                    }

                    @Override
                    protected String toDebugString(long j2) {
                        String strJoin = TextUtils.join(",", strArr);
                        StringBuilder sb2 = new StringBuilder();
                        sb2.append(super.toDebugString(j2));
                        sb2.append(", startAddAccountSession, accountType ");
                        sb2.append(str);
                        sb2.append(", requiredFeatures ");
                        if (strArr == null) {
                            strJoin = null;
                        }
                        sb2.append(strJoin);
                        return sb2.toString();
                    }
                }.bind();
                restoreCallingIdentity(jClearCallingIdentity);
            } catch (Throwable th) {
                th = th;
                j = jClearCallingIdentity;
                restoreCallingIdentity(j);
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
            j = jClearCallingIdentity;
        }
    }

    private abstract class StartAccountSession extends Session {
        private final boolean mIsPasswordForwardingAllowed;

        public StartAccountSession(UserAccounts userAccounts, IAccountManagerResponse iAccountManagerResponse, String str, boolean z, String str2, boolean z2, boolean z3, boolean z4) {
            super(userAccounts, iAccountManagerResponse, str, z, true, str2, z2, z3);
            this.mIsPasswordForwardingAllowed = z4;
        }

        @Override
        public void onResult(Bundle bundle) {
            Intent intent;
            IAccountManagerResponse responseAndClose;
            Bundle.setDefusable(bundle, true);
            this.mNumResults++;
            if (bundle != null) {
                intent = (Intent) bundle.getParcelable("intent");
                if (intent != null && !checkKeyIntent(Binder.getCallingUid(), intent)) {
                    onError(5, "invalid intent in bundle returned");
                    return;
                }
            } else {
                intent = null;
            }
            if (this.mExpectActivityLaunch && bundle != null && bundle.containsKey("intent")) {
                responseAndClose = this.mResponse;
            } else {
                responseAndClose = getResponseAndClose();
            }
            if (responseAndClose == null) {
                return;
            }
            if (bundle == null) {
                if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                    Log.v(AccountManagerService.TAG, getClass().getSimpleName() + " calling onError() on response " + responseAndClose);
                }
                AccountManagerService.this.sendErrorResponse(responseAndClose, 5, "null bundle returned");
                return;
            }
            if (bundle.getInt("errorCode", -1) > 0 && intent == null) {
                AccountManagerService.this.sendErrorResponse(responseAndClose, bundle.getInt("errorCode"), bundle.getString("errorMessage"));
                return;
            }
            if (!this.mIsPasswordForwardingAllowed) {
                bundle.remove("password");
            }
            bundle.remove("authtoken");
            if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                Log.v(AccountManagerService.TAG, getClass().getSimpleName() + " calling onResult() on response " + responseAndClose);
            }
            Bundle bundle2 = bundle.getBundle("accountSessionBundle");
            if (bundle2 != null) {
                String string = bundle2.getString("accountType");
                if (TextUtils.isEmpty(string) || !this.mAccountType.equalsIgnoreCase(string)) {
                    Log.w(AccountManagerService.TAG, "Account type in session bundle doesn't match request.");
                }
                bundle2.putString("accountType", this.mAccountType);
                try {
                    bundle.putBundle("accountSessionBundle", CryptoHelper.getInstance().encryptBundle(bundle2));
                } catch (GeneralSecurityException e) {
                    if (Log.isLoggable(AccountManagerService.TAG, 3)) {
                        Log.v(AccountManagerService.TAG, "Failed to encrypt session bundle!", e);
                    }
                    AccountManagerService.this.sendErrorResponse(responseAndClose, 5, "failed to encrypt session bundle");
                    return;
                }
            }
            AccountManagerService.this.sendResponse(responseAndClose, bundle);
        }
    }

    public void finishSessionAsUser(IAccountManagerResponse iAccountManagerResponse, Bundle bundle, boolean z, Bundle bundle2, int i) {
        boolean z2;
        Bundle.setDefusable(bundle, true);
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            StringBuilder sb = new StringBuilder();
            sb.append("finishSession: response ");
            sb.append(iAccountManagerResponse);
            sb.append(", expectActivityLaunch ");
            z2 = z;
            sb.append(z2);
            sb.append(", caller's uid ");
            sb.append(callingUid);
            sb.append(", caller's user id ");
            sb.append(UserHandle.getCallingUserId());
            sb.append(", pid ");
            sb.append(Binder.getCallingPid());
            sb.append(", for user id ");
            sb.append(i);
            Log.v(TAG, sb.toString());
        } else {
            z2 = z;
        }
        Preconditions.checkArgument(iAccountManagerResponse != null, "response cannot be null");
        if (bundle == null || bundle.size() == 0) {
            throw new IllegalArgumentException("sessionBundle is empty");
        }
        if (isCrossUser(callingUid, i)) {
            throw new SecurityException(String.format("User %s trying to finish session for %s without cross user permission", Integer.valueOf(UserHandle.getCallingUserId()), Integer.valueOf(i)));
        }
        if (!canUserModifyAccounts(i, callingUid)) {
            sendErrorResponse(iAccountManagerResponse, 100, "User is not allowed to add an account!");
            showCantAddAccount(100, i);
            return;
        }
        int callingPid = Binder.getCallingPid();
        try {
            final Bundle bundleDecryptBundle = CryptoHelper.getInstance().decryptBundle(bundle);
            if (bundleDecryptBundle == null) {
                sendErrorResponse(iAccountManagerResponse, 8, "failed to decrypt session bundle");
                return;
            }
            final String string = bundleDecryptBundle.getString("accountType");
            if (TextUtils.isEmpty(string)) {
                sendErrorResponse(iAccountManagerResponse, 7, "accountType is empty");
                return;
            }
            if (bundle2 != null) {
                bundleDecryptBundle.putAll(bundle2);
            }
            bundleDecryptBundle.putInt("callerUid", callingUid);
            bundleDecryptBundle.putInt("callerPid", callingPid);
            if (!canUserModifyAccountsForType(i, string, callingUid)) {
                sendErrorResponse(iAccountManagerResponse, 101, "User cannot modify accounts of this type (policy).");
                showCantAddAccount(101, i);
                return;
            }
            long jClearCallingIdentity = clearCallingIdentity();
            try {
                UserAccounts userAccounts = getUserAccounts(i);
                logRecordWithUid(userAccounts, AccountsDb.DEBUG_ACTION_CALLED_ACCOUNT_SESSION_FINISH, "accounts", callingUid);
                new Session(userAccounts, iAccountManagerResponse, string, z2, true, null, false, true) {
                    @Override
                    public void run() throws RemoteException {
                        this.mAuthenticator.finishSession(this, this.mAccountType, bundleDecryptBundle);
                    }

                    @Override
                    protected String toDebugString(long j) {
                        return super.toDebugString(j) + ", finishSession, accountType " + string;
                    }
                }.bind();
            } finally {
                restoreCallingIdentity(jClearCallingIdentity);
            }
        } catch (GeneralSecurityException e) {
            if (Log.isLoggable(TAG, 3)) {
                Log.v(TAG, "Failed to decrypt session bundle!", e);
            }
            sendErrorResponse(iAccountManagerResponse, 8, "failed to decrypt session bundle");
        }
    }

    private void showCantAddAccount(int i, int i2) {
        Intent intentCreateShowAdminSupportIntent;
        DevicePolicyManagerInternal devicePolicyManagerInternal = (DevicePolicyManagerInternal) LocalServices.getService(DevicePolicyManagerInternal.class);
        if (devicePolicyManagerInternal == null) {
            intentCreateShowAdminSupportIntent = getDefaultCantAddAccountIntent(i);
        } else if (i == 100) {
            intentCreateShowAdminSupportIntent = devicePolicyManagerInternal.createUserRestrictionSupportIntent(i2, "no_modify_accounts");
        } else if (i == 101) {
            intentCreateShowAdminSupportIntent = devicePolicyManagerInternal.createShowAdminSupportIntent(i2, false);
        } else {
            intentCreateShowAdminSupportIntent = null;
        }
        if (intentCreateShowAdminSupportIntent == null) {
            intentCreateShowAdminSupportIntent = getDefaultCantAddAccountIntent(i);
        }
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            this.mContext.startActivityAsUser(intentCreateShowAdminSupportIntent, new UserHandle(i2));
        } finally {
            restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private Intent getDefaultCantAddAccountIntent(int i) {
        Intent intent = new Intent(this.mContext, (Class<?>) CantAddAccountActivity.class);
        intent.putExtra("android.accounts.extra.ERROR_CODE", i);
        intent.addFlags(268435456);
        return intent;
    }

    public void confirmCredentialsAsUser(IAccountManagerResponse iAccountManagerResponse, final Account account, final Bundle bundle, boolean z, int i) {
        boolean z2;
        Bundle.setDefusable(bundle, true);
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            StringBuilder sb = new StringBuilder();
            sb.append("confirmCredentials: ");
            sb.append(account);
            sb.append(", response ");
            sb.append(iAccountManagerResponse);
            sb.append(", expectActivityLaunch ");
            z2 = z;
            sb.append(z2);
            sb.append(", caller's uid ");
            sb.append(callingUid);
            sb.append(", pid ");
            sb.append(Binder.getCallingPid());
            Log.v(TAG, sb.toString());
        } else {
            z2 = z;
        }
        if (isCrossUser(callingUid, i)) {
            throw new SecurityException(String.format("User %s trying to confirm account credentials for %s", Integer.valueOf(UserHandle.getCallingUserId()), Integer.valueOf(i)));
        }
        if (iAccountManagerResponse == null) {
            throw new IllegalArgumentException("response is null");
        }
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            new Session(getUserAccounts(i), iAccountManagerResponse, account.type, z2, true, account.name, true, true) {
                @Override
                public void run() throws RemoteException {
                    this.mAuthenticator.confirmCredentials(this, account, bundle);
                }

                @Override
                protected String toDebugString(long j) {
                    return super.toDebugString(j) + ", confirmCredentials, " + account;
                }
            }.bind();
        } finally {
            restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void updateCredentials(IAccountManagerResponse iAccountManagerResponse, final Account account, String str, boolean z, final Bundle bundle) {
        final String str2;
        boolean z2;
        Bundle.setDefusable(bundle, true);
        if (Log.isLoggable(TAG, 2)) {
            StringBuilder sb = new StringBuilder();
            sb.append("updateCredentials: ");
            sb.append(account);
            sb.append(", response ");
            sb.append(iAccountManagerResponse);
            sb.append(", authTokenType ");
            str2 = str;
            sb.append(str2);
            sb.append(", expectActivityLaunch ");
            z2 = z;
            sb.append(z2);
            sb.append(", caller's uid ");
            sb.append(Binder.getCallingUid());
            sb.append(", pid ");
            sb.append(Binder.getCallingPid());
            Log.v(TAG, sb.toString());
        } else {
            str2 = str;
            z2 = z;
        }
        if (iAccountManagerResponse == null) {
            throw new IllegalArgumentException("response is null");
        }
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        int callingUserId = UserHandle.getCallingUserId();
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            new Session(getUserAccounts(callingUserId), iAccountManagerResponse, account.type, z2, true, account.name, false, true) {
                @Override
                public void run() throws RemoteException {
                    this.mAuthenticator.updateCredentials(this, account, str2, bundle);
                }

                @Override
                protected String toDebugString(long j) {
                    if (bundle != null) {
                        bundle.keySet();
                    }
                    return super.toDebugString(j) + ", updateCredentials, " + account + ", authTokenType " + str2 + ", loginOptions " + bundle;
                }
            }.bind();
        } finally {
            restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void startUpdateCredentialsSession(IAccountManagerResponse iAccountManagerResponse, final Account account, String str, boolean z, final Bundle bundle) {
        final String str2;
        boolean z2;
        Bundle.setDefusable(bundle, true);
        if (Log.isLoggable(TAG, 2)) {
            StringBuilder sb = new StringBuilder();
            sb.append("startUpdateCredentialsSession: ");
            sb.append(account);
            sb.append(", response ");
            sb.append(iAccountManagerResponse);
            sb.append(", authTokenType ");
            str2 = str;
            sb.append(str2);
            sb.append(", expectActivityLaunch ");
            z2 = z;
            sb.append(z2);
            sb.append(", caller's uid ");
            sb.append(Binder.getCallingUid());
            sb.append(", pid ");
            sb.append(Binder.getCallingPid());
            Log.v(TAG, sb.toString());
        } else {
            str2 = str;
            z2 = z;
        }
        if (iAccountManagerResponse == null) {
            throw new IllegalArgumentException("response is null");
        }
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        int callingUid = Binder.getCallingUid();
        int callingUserId = UserHandle.getCallingUserId();
        boolean zIsPermitted = isPermitted(bundle.getString("androidPackageName"), callingUid, "android.permission.GET_PASSWORD");
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            new StartAccountSession(getUserAccounts(callingUserId), iAccountManagerResponse, account.type, z2, account.name, false, true, zIsPermitted) {
                @Override
                public void run() throws RemoteException {
                    this.mAuthenticator.startUpdateCredentialsSession(this, account, str2, bundle);
                }

                @Override
                protected String toDebugString(long j) {
                    if (bundle != null) {
                        bundle.keySet();
                    }
                    return super.toDebugString(j) + ", startUpdateCredentialsSession, " + account + ", authTokenType " + str2 + ", loginOptions " + bundle;
                }
            }.bind();
        } finally {
            restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void isCredentialsUpdateSuggested(IAccountManagerResponse iAccountManagerResponse, final Account account, final String str) {
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "isCredentialsUpdateSuggested: " + account + ", response " + iAccountManagerResponse + ", caller's uid " + Binder.getCallingUid() + ", pid " + Binder.getCallingPid());
        }
        if (iAccountManagerResponse == null) {
            throw new IllegalArgumentException("response is null");
        }
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        if (TextUtils.isEmpty(str)) {
            throw new IllegalArgumentException("status token is empty");
        }
        int callingUserId = UserHandle.getCallingUserId();
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            new Session(getUserAccounts(callingUserId), iAccountManagerResponse, account.type, false, false, account.name, false) {
                @Override
                protected String toDebugString(long j) {
                    return super.toDebugString(j) + ", isCredentialsUpdateSuggested, " + account;
                }

                @Override
                public void run() throws RemoteException {
                    this.mAuthenticator.isCredentialsUpdateSuggested(this, account, str);
                }

                @Override
                public void onResult(Bundle bundle) {
                    Bundle.setDefusable(bundle, true);
                    IAccountManagerResponse responseAndClose = getResponseAndClose();
                    if (responseAndClose == null) {
                        return;
                    }
                    if (bundle == null) {
                        AccountManagerService.this.sendErrorResponse(responseAndClose, 5, "null bundle");
                        return;
                    }
                    if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                        Log.v(AccountManagerService.TAG, getClass().getSimpleName() + " calling onResult() on response " + responseAndClose);
                    }
                    if (bundle.getInt("errorCode", -1) > 0) {
                        AccountManagerService.this.sendErrorResponse(responseAndClose, bundle.getInt("errorCode"), bundle.getString("errorMessage"));
                    } else {
                        if (!bundle.containsKey("booleanResult")) {
                            AccountManagerService.this.sendErrorResponse(responseAndClose, 5, "no result in response");
                            return;
                        }
                        Bundle bundle2 = new Bundle();
                        bundle2.putBoolean("booleanResult", bundle.getBoolean("booleanResult", false));
                        AccountManagerService.this.sendResponse(responseAndClose, bundle2);
                    }
                }
            }.bind();
        } finally {
            restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void editProperties(IAccountManagerResponse iAccountManagerResponse, final String str, boolean z) {
        boolean z2;
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            StringBuilder sb = new StringBuilder();
            sb.append("editProperties: accountType ");
            sb.append(str);
            sb.append(", response ");
            sb.append(iAccountManagerResponse);
            sb.append(", expectActivityLaunch ");
            z2 = z;
            sb.append(z2);
            sb.append(", caller's uid ");
            sb.append(callingUid);
            sb.append(", pid ");
            sb.append(Binder.getCallingPid());
            Log.v(TAG, sb.toString());
        } else {
            z2 = z;
        }
        if (iAccountManagerResponse == null) {
            throw new IllegalArgumentException("response is null");
        }
        if (str == null) {
            throw new IllegalArgumentException("accountType is null");
        }
        int callingUserId = UserHandle.getCallingUserId();
        if (!isAccountManagedByCaller(str, callingUid, callingUserId) && !isSystemUid(callingUid)) {
            throw new SecurityException(String.format("uid %s cannot edit authenticator properites for account type: %s", Integer.valueOf(callingUid), str));
        }
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            new Session(getUserAccounts(callingUserId), iAccountManagerResponse, str, z2, true, null, false) {
                @Override
                public void run() throws RemoteException {
                    this.mAuthenticator.editProperties(this, this.mAccountType);
                }

                @Override
                protected String toDebugString(long j) {
                    return super.toDebugString(j) + ", editProperties, accountType " + str;
                }
            }.bind();
        } finally {
            restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public boolean hasAccountAccess(Account account, String str, UserHandle userHandle) {
        if (UserHandle.getAppId(Binder.getCallingUid()) != 1000) {
            throw new SecurityException("Can be called only by system UID");
        }
        Preconditions.checkNotNull(account, "account cannot be null");
        Preconditions.checkNotNull(str, "packageName cannot be null");
        Preconditions.checkNotNull(userHandle, "userHandle cannot be null");
        int identifier = userHandle.getIdentifier();
        Preconditions.checkArgumentInRange(identifier, 0, Integer.MAX_VALUE, "user must be concrete");
        try {
            return hasAccountAccess(account, str, this.mPackageManager.getPackageUidAsUser(str, identifier));
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(TAG, "Package not found " + e.getMessage());
            return false;
        }
    }

    private String getPackageNameForUid(int i) {
        int i2;
        String[] packagesForUid = this.mPackageManager.getPackagesForUid(i);
        if (ArrayUtils.isEmpty(packagesForUid)) {
            return null;
        }
        String str = packagesForUid[0];
        if (packagesForUid.length == 1) {
            return str;
        }
        int i3 = Integer.MAX_VALUE;
        String str2 = str;
        for (String str3 : packagesForUid) {
            try {
                ApplicationInfo applicationInfo = this.mPackageManager.getApplicationInfo(str3, 0);
                if (applicationInfo != null && (i2 = applicationInfo.targetSdkVersion) < i3) {
                    str2 = str3;
                    i3 = i2;
                }
            } catch (PackageManager.NameNotFoundException e) {
            }
        }
        return str2;
    }

    private boolean hasAccountAccess(Account account, String str, int i) {
        if (str == null && (str = getPackageNameForUid(i)) == null) {
            return false;
        }
        if (permissionIsGranted(account, null, i, UserHandle.getUserId(i))) {
            return true;
        }
        int iIntValue = resolveAccountVisibility(account, str, getUserAccounts(UserHandle.getUserId(i))).intValue();
        return iIntValue == 1 || iIntValue == 2;
    }

    public IntentSender createRequestAccountAccessIntentSenderAsUser(Account account, String str, UserHandle userHandle) {
        if (UserHandle.getAppId(Binder.getCallingUid()) != 1000) {
            throw new SecurityException("Can be called only by system UID");
        }
        Preconditions.checkNotNull(account, "account cannot be null");
        Preconditions.checkNotNull(str, "packageName cannot be null");
        Preconditions.checkNotNull(userHandle, "userHandle cannot be null");
        int identifier = userHandle.getIdentifier();
        Preconditions.checkArgumentInRange(identifier, 0, Integer.MAX_VALUE, "user must be concrete");
        try {
            Intent intentNewRequestAccountAccessIntent = newRequestAccountAccessIntent(account, str, this.mPackageManager.getPackageUidAsUser(str, identifier), null);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                return PendingIntent.getActivityAsUser(this.mContext, 0, intentNewRequestAccountAccessIntent, 1409286144, null, new UserHandle(identifier)).getIntentSender();
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Slog.e(TAG, "Unknown package " + str);
            return null;
        }
    }

    private Intent newRequestAccountAccessIntent(final Account account, String str, final int i, final RemoteCallback remoteCallback) {
        return newGrantCredentialsPermissionIntent(account, str, i, new AccountAuthenticatorResponse((IAccountAuthenticatorResponse) new IAccountAuthenticatorResponse.Stub() {
            public void onResult(Bundle bundle) throws RemoteException {
                handleAuthenticatorResponse(true);
            }

            public void onRequestContinued() {
            }

            public void onError(int i2, String str2) throws RemoteException {
                handleAuthenticatorResponse(false);
            }

            private void handleAuthenticatorResponse(boolean z) throws RemoteException {
                AccountManagerService.this.cancelNotification(AccountManagerService.this.getCredentialPermissionNotificationId(account, "com.android.AccountManager.ACCOUNT_ACCESS_TOKEN_TYPE", i), UserHandle.getUserHandleForUid(i));
                if (remoteCallback != null) {
                    Bundle bundle = new Bundle();
                    bundle.putBoolean("booleanResult", z);
                    remoteCallback.sendResult(bundle);
                }
            }
        }), "com.android.AccountManager.ACCOUNT_ACCESS_TOKEN_TYPE", false);
    }

    public boolean someUserHasAccount(Account account) {
        if (!UserHandle.isSameApp(1000, Binder.getCallingUid())) {
            throw new SecurityException("Only system can check for accounts across users");
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            AccountAndUser[] allAccounts = getAllAccounts();
            for (int length = allAccounts.length - 1; length >= 0; length--) {
                if (allAccounts[length].account.equals(account)) {
                    return true;
                }
            }
            return false;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private class GetAccountsByTypeAndFeatureSession extends Session {
        private volatile Account[] mAccountsOfType;
        private volatile ArrayList<Account> mAccountsWithFeatures;
        private final int mCallingUid;
        private volatile int mCurrentAccount;
        private final String[] mFeatures;
        private final boolean mIncludeManagedNotVisible;
        private final String mPackageName;

        public GetAccountsByTypeAndFeatureSession(UserAccounts userAccounts, IAccountManagerResponse iAccountManagerResponse, String str, String[] strArr, int i, String str2, boolean z) {
            super(AccountManagerService.this, userAccounts, iAccountManagerResponse, str, false, true, null, false);
            this.mAccountsOfType = null;
            this.mAccountsWithFeatures = null;
            this.mCurrentAccount = 0;
            this.mCallingUid = i;
            this.mFeatures = strArr;
            this.mPackageName = str2;
            this.mIncludeManagedNotVisible = z;
        }

        @Override
        public void run() throws RemoteException {
            this.mAccountsOfType = AccountManagerService.this.getAccountsFromCache(this.mAccounts, this.mAccountType, this.mCallingUid, this.mPackageName, this.mIncludeManagedNotVisible);
            this.mAccountsWithFeatures = new ArrayList<>(this.mAccountsOfType.length);
            this.mCurrentAccount = 0;
            checkAccount();
        }

        public void checkAccount() {
            if (this.mCurrentAccount >= this.mAccountsOfType.length) {
                sendResult();
                return;
            }
            IAccountAuthenticator iAccountAuthenticator = this.mAuthenticator;
            if (iAccountAuthenticator == null) {
                if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                    Log.v(AccountManagerService.TAG, "checkAccount: aborting session since we are no longer connected to the authenticator, " + toDebugString());
                    return;
                }
                return;
            }
            try {
                iAccountAuthenticator.hasFeatures(this, this.mAccountsOfType[this.mCurrentAccount], this.mFeatures);
            } catch (RemoteException e) {
                onError(1, "remote exception");
            }
        }

        @Override
        public void onResult(Bundle bundle) {
            Bundle.setDefusable(bundle, true);
            this.mNumResults++;
            if (bundle == null) {
                onError(5, "null bundle");
                return;
            }
            if (bundle.getBoolean("booleanResult", false)) {
                this.mAccountsWithFeatures.add(this.mAccountsOfType[this.mCurrentAccount]);
            }
            this.mCurrentAccount++;
            checkAccount();
        }

        public void sendResult() {
            IAccountManagerResponse responseAndClose = getResponseAndClose();
            if (responseAndClose != null) {
                try {
                    Account[] accountArr = new Account[this.mAccountsWithFeatures.size()];
                    for (int i = 0; i < accountArr.length; i++) {
                        accountArr[i] = this.mAccountsWithFeatures.get(i);
                    }
                    if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                        Log.v(AccountManagerService.TAG, getClass().getSimpleName() + " calling onResult() on response " + responseAndClose);
                    }
                    Bundle bundle = new Bundle();
                    bundle.putParcelableArray("accounts", accountArr);
                    responseAndClose.onResult(bundle);
                } catch (RemoteException e) {
                    if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                        Log.v(AccountManagerService.TAG, "failure while notifying response", e);
                    }
                }
            }
        }

        @Override
        protected String toDebugString(long j) {
            StringBuilder sb = new StringBuilder();
            sb.append(super.toDebugString(j));
            sb.append(", getAccountsByTypeAndFeatures, ");
            sb.append(this.mFeatures != null ? TextUtils.join(",", this.mFeatures) : null);
            return sb.toString();
        }
    }

    public Account[] getAccounts(int i, String str) {
        int callingUid = Binder.getCallingUid();
        this.mAppOpsManager.checkPackage(callingUid, str);
        List<String> typesVisibleToCaller = getTypesVisibleToCaller(callingUid, i, str);
        if (typesVisibleToCaller.isEmpty()) {
            return EMPTY_ACCOUNT_ARRAY;
        }
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            return getAccountsInternal(getUserAccounts(i), callingUid, str, typesVisibleToCaller, false);
        } finally {
            restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public AccountAndUser[] getRunningAccounts() {
        try {
            return getAccounts(ActivityManager.getService().getRunningUserIds());
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public AccountAndUser[] getAllAccounts() {
        List users = getUserManager().getUsers(true);
        int[] iArr = new int[users.size()];
        for (int i = 0; i < iArr.length; i++) {
            iArr[i] = ((UserInfo) users.get(i)).id;
        }
        return getAccounts(iArr);
    }

    private AccountAndUser[] getAccounts(int[] iArr) {
        ArrayList arrayListNewArrayList = Lists.newArrayList();
        for (int i : iArr) {
            UserAccounts userAccounts = getUserAccounts(i);
            if (userAccounts != null) {
                for (Account account : getAccountsFromCache(userAccounts, null, Binder.getCallingUid(), null, false)) {
                    arrayListNewArrayList.add(new AccountAndUser(account, i));
                }
            }
        }
        return (AccountAndUser[]) arrayListNewArrayList.toArray(new AccountAndUser[arrayListNewArrayList.size()]);
    }

    public Account[] getAccountsAsUser(String str, int i, String str2) {
        this.mAppOpsManager.checkPackage(Binder.getCallingUid(), str2);
        return getAccountsAsUserForPackage(str, i, str2, -1, str2, false);
    }

    private Account[] getAccountsAsUserForPackage(String str, int i, String str2, int i2, String str3, boolean z) {
        int callingUid = Binder.getCallingUid();
        if (i != UserHandle.getCallingUserId() && callingUid != 1000 && this.mContext.checkCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL") != 0) {
            throw new SecurityException("User " + UserHandle.getCallingUserId() + " trying to get account for " + i);
        }
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "getAccounts: accountType " + str + ", caller's uid " + Binder.getCallingUid() + ", pid " + Binder.getCallingPid());
        }
        List<String> typesManagedByCaller = getTypesManagedByCaller(callingUid, UserHandle.getUserId(callingUid));
        if (i2 == -1 || (!UserHandle.isSameApp(callingUid, 1000) && (str == null || !typesManagedByCaller.contains(str)))) {
            str2 = str3;
            i2 = callingUid;
        }
        List<String> typesVisibleToCaller = getTypesVisibleToCaller(i2, i, str2);
        if (typesVisibleToCaller.isEmpty() || (str != null && !typesVisibleToCaller.contains(str))) {
            return EMPTY_ACCOUNT_ARRAY;
        }
        if (typesVisibleToCaller.contains(str)) {
            typesVisibleToCaller = new ArrayList<>();
            typesVisibleToCaller.add(str);
        }
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            return getAccountsInternal(getUserAccounts(i), i2, str2, typesVisibleToCaller, z);
        } finally {
            restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private Account[] getAccountsInternal(UserAccounts userAccounts, int i, String str, List<String> list, boolean z) {
        ArrayList arrayList = new ArrayList();
        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            Account[] accountsFromCache = getAccountsFromCache(userAccounts, it.next(), i, str, z);
            if (accountsFromCache != null) {
                arrayList.addAll(Arrays.asList(accountsFromCache));
            }
        }
        Account[] accountArr = new Account[arrayList.size()];
        for (int i2 = 0; i2 < arrayList.size(); i2++) {
            accountArr[i2] = (Account) arrayList.get(i2);
        }
        return accountArr;
    }

    public void addSharedAccountsFromParentUser(int i, int i2, String str) {
        checkManageOrCreateUsersPermission("addSharedAccountsFromParentUser");
        for (Account account : getAccountsAsUser(null, i, str)) {
            addSharedAccountAsUser(account, i2);
        }
    }

    private boolean addSharedAccountAsUser(Account account, int i) {
        UserAccounts userAccounts = getUserAccounts(handleIncomingUser(i));
        userAccounts.accountsDb.deleteSharedAccount(account);
        long jInsertSharedAccount = userAccounts.accountsDb.insertSharedAccount(account);
        if (jInsertSharedAccount < 0) {
            Log.w(TAG, "insertAccountIntoDatabase: " + account + ", skipping the DB insert failed");
            return false;
        }
        logRecord(AccountsDb.DEBUG_ACTION_ACCOUNT_ADD, "shared_accounts", jInsertSharedAccount, userAccounts);
        return true;
    }

    public boolean renameSharedAccountAsUser(Account account, String str, int i) {
        UserAccounts userAccounts = getUserAccounts(handleIncomingUser(i));
        long jFindSharedAccountId = userAccounts.accountsDb.findSharedAccountId(account);
        int iRenameSharedAccount = userAccounts.accountsDb.renameSharedAccount(account, str);
        if (iRenameSharedAccount > 0) {
            logRecord(AccountsDb.DEBUG_ACTION_ACCOUNT_RENAME, "shared_accounts", jFindSharedAccountId, userAccounts, getCallingUid());
            renameAccountInternal(userAccounts, account, str);
        }
        return iRenameSharedAccount > 0;
    }

    public boolean removeSharedAccountAsUser(Account account, int i) {
        return removeSharedAccountAsUser(account, i, getCallingUid());
    }

    private boolean removeSharedAccountAsUser(Account account, int i, int i2) {
        UserAccounts userAccounts = getUserAccounts(handleIncomingUser(i));
        long jFindSharedAccountId = userAccounts.accountsDb.findSharedAccountId(account);
        boolean zDeleteSharedAccount = userAccounts.accountsDb.deleteSharedAccount(account);
        if (zDeleteSharedAccount) {
            logRecord(AccountsDb.DEBUG_ACTION_ACCOUNT_REMOVE, "shared_accounts", jFindSharedAccountId, userAccounts, i2);
            removeAccountInternal(userAccounts, account, i2);
        }
        return zDeleteSharedAccount;
    }

    public Account[] getSharedAccountsAsUser(int i) {
        Account[] accountArr;
        UserAccounts userAccounts = getUserAccounts(handleIncomingUser(i));
        synchronized (userAccounts.dbLock) {
            List<Account> sharedAccounts = userAccounts.accountsDb.getSharedAccounts();
            accountArr = new Account[sharedAccounts.size()];
            sharedAccounts.toArray(accountArr);
        }
        return accountArr;
    }

    public Account[] getAccounts(String str, String str2) {
        return getAccountsAsUser(str, UserHandle.getCallingUserId(), str2);
    }

    public Account[] getAccountsForPackage(String str, int i, String str2) {
        int callingUid = Binder.getCallingUid();
        if (!UserHandle.isSameApp(callingUid, 1000)) {
            throw new SecurityException("getAccountsForPackage() called from unauthorized uid " + callingUid + " with uid=" + i);
        }
        return getAccountsAsUserForPackage(null, UserHandle.getCallingUserId(), str, i, str2, true);
    }

    public Account[] getAccountsByTypeForPackage(String str, String str2, String str3) {
        int callingUid = Binder.getCallingUid();
        int callingUserId = UserHandle.getCallingUserId();
        this.mAppOpsManager.checkPackage(callingUid, str3);
        try {
            int packageUidAsUser = this.mPackageManager.getPackageUidAsUser(str2, callingUserId);
            if (!UserHandle.isSameApp(callingUid, 1000) && str != null && !isAccountManagedByCaller(str, callingUid, callingUserId)) {
                return EMPTY_ACCOUNT_ARRAY;
            }
            if (!UserHandle.isSameApp(callingUid, 1000) && str == null) {
                return getAccountsAsUserForPackage(str, callingUserId, str2, packageUidAsUser, str3, false);
            }
            return getAccountsAsUserForPackage(str, callingUserId, str2, packageUidAsUser, str3, true);
        } catch (PackageManager.NameNotFoundException e) {
            Slog.e(TAG, "Couldn't determine the packageUid for " + str2 + e);
            return EMPTY_ACCOUNT_ARRAY;
        }
    }

    private boolean needToStartChooseAccountActivity(Account[] accountArr, String str) {
        if (accountArr.length < 1) {
            return false;
        }
        return accountArr.length > 1 || resolveAccountVisibility(accountArr[0], str, getUserAccounts(UserHandle.getCallingUserId())).intValue() == 4;
    }

    private void startChooseAccountActivityWithAccounts(IAccountManagerResponse iAccountManagerResponse, Account[] accountArr, String str) {
        Intent intent = new Intent(this.mContext, (Class<?>) ChooseAccountActivity.class);
        intent.putExtra("accounts", accountArr);
        intent.putExtra("accountManagerResponse", (Parcelable) new AccountManagerResponse(iAccountManagerResponse));
        intent.putExtra("androidPackageName", str);
        this.mContext.startActivityAsUser(intent, UserHandle.of(UserHandle.getCallingUserId()));
    }

    private void handleGetAccountsResult(IAccountManagerResponse iAccountManagerResponse, Account[] accountArr, String str) {
        if (needToStartChooseAccountActivity(accountArr, str)) {
            startChooseAccountActivityWithAccounts(iAccountManagerResponse, accountArr, str);
            return;
        }
        if (accountArr.length == 1) {
            Bundle bundle = new Bundle();
            bundle.putString("authAccount", accountArr[0].name);
            bundle.putString("accountType", accountArr[0].type);
            onResult(iAccountManagerResponse, bundle);
            return;
        }
        onResult(iAccountManagerResponse, new Bundle());
    }

    public void getAccountByTypeAndFeatures(final IAccountManagerResponse iAccountManagerResponse, String str, String[] strArr, final String str2) {
        int callingUid = Binder.getCallingUid();
        this.mAppOpsManager.checkPackage(callingUid, str2);
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "getAccount: accountType " + str + ", response " + iAccountManagerResponse + ", features " + Arrays.toString(strArr) + ", caller's uid " + callingUid + ", pid " + Binder.getCallingPid());
        }
        if (iAccountManagerResponse == null) {
            throw new IllegalArgumentException("response is null");
        }
        if (str == null) {
            throw new IllegalArgumentException("accountType is null");
        }
        int callingUserId = UserHandle.getCallingUserId();
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            UserAccounts userAccounts = getUserAccounts(callingUserId);
            if (ArrayUtils.isEmpty(strArr)) {
                handleGetAccountsResult(iAccountManagerResponse, getAccountsFromCache(userAccounts, str, callingUid, str2, true), str2);
            } else {
                new GetAccountsByTypeAndFeatureSession(userAccounts, new IAccountManagerResponse.Stub() {
                    public void onResult(Bundle bundle) throws RemoteException {
                        Parcelable[] parcelableArray = bundle.getParcelableArray("accounts");
                        Account[] accountArr = new Account[parcelableArray.length];
                        for (int i = 0; i < parcelableArray.length; i++) {
                            accountArr[i] = (Account) parcelableArray[i];
                        }
                        AccountManagerService.this.handleGetAccountsResult(iAccountManagerResponse, accountArr, str2);
                    }

                    public void onError(int i, String str3) throws RemoteException {
                    }
                }, str, strArr, callingUid, str2, true).bind();
            }
        } finally {
            restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void getAccountsByFeatures(IAccountManagerResponse iAccountManagerResponse, String str, String[] strArr, String str2) {
        int callingUid = Binder.getCallingUid();
        this.mAppOpsManager.checkPackage(callingUid, str2);
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "getAccounts: accountType " + str + ", response " + iAccountManagerResponse + ", features " + Arrays.toString(strArr) + ", caller's uid " + callingUid + ", pid " + Binder.getCallingPid());
        }
        if (iAccountManagerResponse == null) {
            throw new IllegalArgumentException("response is null");
        }
        if (str == null) {
            throw new IllegalArgumentException("accountType is null");
        }
        int callingUserId = UserHandle.getCallingUserId();
        if (!getTypesVisibleToCaller(callingUid, callingUserId, str2).contains(str)) {
            Bundle bundle = new Bundle();
            bundle.putParcelableArray("accounts", EMPTY_ACCOUNT_ARRAY);
            try {
                iAccountManagerResponse.onResult(bundle);
                return;
            } catch (RemoteException e) {
                Log.e(TAG, "Cannot respond to caller do to exception.", e);
                return;
            }
        }
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            UserAccounts userAccounts = getUserAccounts(callingUserId);
            if (strArr != null && strArr.length != 0) {
                new GetAccountsByTypeAndFeatureSession(userAccounts, iAccountManagerResponse, str, strArr, callingUid, str2, false).bind();
                return;
            }
            Account[] accountsFromCache = getAccountsFromCache(userAccounts, str, callingUid, str2, false);
            Bundle bundle2 = new Bundle();
            bundle2.putParcelableArray("accounts", accountsFromCache);
            onResult(iAccountManagerResponse, bundle2);
        } finally {
            restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void onAccountAccessed(String str) throws RemoteException {
        int callingUid = Binder.getCallingUid();
        if (UserHandle.getAppId(callingUid) == 1000) {
            return;
        }
        int callingUserId = UserHandle.getCallingUserId();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            for (Account account : getAccounts(callingUserId, this.mContext.getOpPackageName())) {
                if (Objects.equals(account.getAccessId(), str) && !hasAccountAccess(account, (String) null, callingUid)) {
                    updateAppPermission(account, "com.android.AccountManager.ACCOUNT_ACCESS_TOKEN_TYPE", callingUid, true);
                }
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void onShellCommand(FileDescriptor fileDescriptor, FileDescriptor fileDescriptor2, FileDescriptor fileDescriptor3, String[] strArr, ShellCallback shellCallback, ResultReceiver resultReceiver) {
        new AccountManagerServiceShellCommand(this).exec(this, fileDescriptor, fileDescriptor2, fileDescriptor3, strArr, shellCallback, resultReceiver);
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

        public Session(AccountManagerService accountManagerService, UserAccounts userAccounts, IAccountManagerResponse iAccountManagerResponse, String str, boolean z, boolean z2, String str2, boolean z3) {
            this(userAccounts, iAccountManagerResponse, str, z, z2, str2, z3, false);
        }

        public Session(UserAccounts userAccounts, IAccountManagerResponse iAccountManagerResponse, String str, boolean z, boolean z2, String str2, boolean z3, boolean z4) {
            this.mNumResults = 0;
            this.mNumRequestContinued = 0;
            this.mNumErrors = 0;
            this.mAuthenticator = null;
            if (str == null) {
                throw new IllegalArgumentException("accountType is null");
            }
            this.mAccounts = userAccounts;
            this.mStripAuthTokenFromResult = z2;
            this.mResponse = iAccountManagerResponse;
            this.mAccountType = str;
            this.mExpectActivityLaunch = z;
            this.mCreationTime = SystemClock.elapsedRealtime();
            this.mAccountName = str2;
            this.mAuthDetailsRequired = z3;
            this.mUpdateLastAuthenticatedTime = z4;
            synchronized (AccountManagerService.this.mSessions) {
                AccountManagerService.this.mSessions.put(toString(), this);
            }
            if (iAccountManagerResponse != null) {
                try {
                    iAccountManagerResponse.asBinder().linkToDeath(this, 0);
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

        protected boolean checkKeyIntent(int i, Intent intent) {
            if (intent.getClipData() == null) {
                intent.setClipData(ClipData.newPlainText(null, null));
            }
            intent.setFlags(intent.getFlags() & (-196));
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                ResolveInfo resolveInfoResolveActivityAsUser = AccountManagerService.this.mContext.getPackageManager().resolveActivityAsUser(intent, 0, this.mAccounts.userId);
                if (resolveInfoResolveActivityAsUser == null) {
                    return false;
                }
                ActivityInfo activityInfo = resolveInfoResolveActivityAsUser.activityInfo;
                int i2 = activityInfo.applicationInfo.uid;
                PackageManagerInternal packageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
                if (isExportedSystemActivity(activityInfo) || packageManagerInternal.hasSignatureCapability(i2, i, 16)) {
                    return true;
                }
                Log.e(AccountManagerService.TAG, String.format("KEY_INTENT resolved to an Activity (%s) in a package (%s) that does not share a signature with the supplying authenticator (%s).", activityInfo.name, activityInfo.packageName, this.mAccountType));
                return false;
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        private boolean isExportedSystemActivity(ActivityInfo activityInfo) {
            String str = activityInfo.name;
            return PackageManagerService.PLATFORM_PACKAGE_NAME.equals(activityInfo.packageName) && (GrantCredentialsPermissionActivity.class.getName().equals(str) || CantAddAccountActivity.class.getName().equals(str));
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

        protected String toDebugString(long j) {
            StringBuilder sb = new StringBuilder();
            sb.append("Session: expectLaunch ");
            sb.append(this.mExpectActivityLaunch);
            sb.append(", connected ");
            sb.append(this.mAuthenticator != null);
            sb.append(", stats (");
            sb.append(this.mNumResults);
            sb.append(SliceClientPermissions.SliceAuthority.DELIMITER);
            sb.append(this.mNumRequestContinued);
            sb.append(SliceClientPermissions.SliceAuthority.DELIMITER);
            sb.append(this.mNumErrors);
            sb.append("), lifetime ");
            sb.append((j - this.mCreationTime) / 1000.0d);
            return sb.toString();
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

        public void cancelTimeout() {
            AccountManagerService.this.mHandler.removeMessages(3, this);
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            this.mAuthenticator = IAccountAuthenticator.Stub.asInterface(iBinder);
            try {
                run();
            } catch (RemoteException e) {
                onError(1, "remote exception");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            this.mAuthenticator = null;
            IAccountManagerResponse responseAndClose = getResponseAndClose();
            if (responseAndClose != null) {
                try {
                    responseAndClose.onError(1, "disconnected");
                } catch (RemoteException e) {
                    if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                        Log.v(AccountManagerService.TAG, "Session.onServiceDisconnected: caught RemoteException while responding", e);
                    }
                }
            }
        }

        public void onTimedOut() {
            IAccountManagerResponse responseAndClose = getResponseAndClose();
            if (responseAndClose != null) {
                try {
                    responseAndClose.onError(1, "timeout");
                } catch (RemoteException e) {
                    if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                        Log.v(AccountManagerService.TAG, "Session.onTimedOut: caught RemoteException while responding", e);
                    }
                }
            }
        }

        public void onResult(Bundle bundle) {
            Intent intent;
            IAccountManagerResponse responseAndClose;
            boolean z = true;
            Bundle.setDefusable(bundle, true);
            this.mNumResults++;
            if (bundle != null) {
                boolean z2 = bundle.getBoolean("booleanResult", false);
                boolean z3 = bundle.containsKey("authAccount") && bundle.containsKey("accountType");
                if (!this.mUpdateLastAuthenticatedTime || (!z2 && !z3)) {
                    z = false;
                }
                if (z || this.mAuthDetailsRequired) {
                    boolean zIsAccountPresentForCaller = AccountManagerService.this.isAccountPresentForCaller(this.mAccountName, this.mAccountType);
                    if (z && zIsAccountPresentForCaller) {
                        AccountManagerService.this.updateLastAuthenticatedTime(new Account(this.mAccountName, this.mAccountType));
                    }
                    if (this.mAuthDetailsRequired) {
                        long jFindAccountLastAuthenticatedTime = -1;
                        if (zIsAccountPresentForCaller) {
                            jFindAccountLastAuthenticatedTime = this.mAccounts.accountsDb.findAccountLastAuthenticatedTime(new Account(this.mAccountName, this.mAccountType));
                        }
                        bundle.putLong("lastAuthenticatedTime", jFindAccountLastAuthenticatedTime);
                    }
                }
            }
            if (bundle != null) {
                intent = (Intent) bundle.getParcelable("intent");
                if (intent != null && !checkKeyIntent(Binder.getCallingUid(), intent)) {
                    onError(5, "invalid intent in bundle returned");
                    return;
                }
            } else {
                intent = null;
            }
            if (bundle != null && !TextUtils.isEmpty(bundle.getString("authtoken"))) {
                String string = bundle.getString("authAccount");
                String string2 = bundle.getString("accountType");
                if (!TextUtils.isEmpty(string) && !TextUtils.isEmpty(string2)) {
                    AccountManagerService.this.cancelNotification(AccountManagerService.this.getSigninRequiredNotificationId(this.mAccounts, new Account(string, string2)), new UserHandle(this.mAccounts.userId));
                }
            }
            if (this.mExpectActivityLaunch && bundle != null && bundle.containsKey("intent")) {
                responseAndClose = this.mResponse;
            } else {
                responseAndClose = getResponseAndClose();
            }
            if (responseAndClose != null) {
                try {
                    if (bundle == null) {
                        if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                            Log.v(AccountManagerService.TAG, getClass().getSimpleName() + " calling onError() on response " + responseAndClose);
                        }
                        responseAndClose.onError(5, "null bundle returned");
                        return;
                    }
                    if (this.mStripAuthTokenFromResult) {
                        bundle.remove("authtoken");
                    }
                    if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                        Log.v(AccountManagerService.TAG, getClass().getSimpleName() + " calling onResult() on response " + responseAndClose);
                    }
                    if (bundle.getInt("errorCode", -1) > 0 && intent == null) {
                        responseAndClose.onError(bundle.getInt("errorCode"), bundle.getString("errorMessage"));
                    } else {
                        responseAndClose.onResult(bundle);
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

        public void onError(int i, String str) {
            this.mNumErrors++;
            IAccountManagerResponse responseAndClose = getResponseAndClose();
            if (responseAndClose != null) {
                if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                    Log.v(AccountManagerService.TAG, getClass().getSimpleName() + " calling onError() on response " + responseAndClose);
                }
                try {
                    responseAndClose.onError(i, str);
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

        private boolean bindToAuthenticator(String str) {
            int i;
            RegisteredServicesCache.ServiceInfo<AuthenticatorDescription> serviceInfo = AccountManagerService.this.mAuthenticatorCache.getServiceInfo(AuthenticatorDescription.newKey(str), this.mAccounts.userId);
            if (serviceInfo != null) {
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
                if (AccountManagerService.this.mAuthenticatorCache.getBindInstantServiceAllowed(this.mAccounts.userId)) {
                    i = 4194305;
                } else {
                    i = 1;
                }
                if (AccountManagerService.this.mContext.bindServiceAsUser(intent, this, i, UserHandle.of(this.mAccounts.userId))) {
                    return true;
                }
                if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                    Log.v(AccountManagerService.TAG, "bindService to " + serviceInfo.componentName + " failed");
                }
                return false;
            }
            if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                Log.v(AccountManagerService.TAG, "there is no authenticator for " + str + ", bailing out");
            }
            return false;
        }
    }

    class MessageHandler extends Handler {
        MessageHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 3:
                    ((Session) message.obj).onTimedOut();
                    return;
                case 4:
                    AccountManagerService.this.copyAccountToUser(null, (Account) message.obj, message.arg1, message.arg2);
                    return;
                default:
                    throw new IllegalStateException("unhandled message: " + message.what);
            }
        }
    }

    private void logRecord(UserAccounts userAccounts, String str, String str2) {
        logRecord(str, str2, -1L, userAccounts);
    }

    private void logRecordWithUid(UserAccounts userAccounts, String str, String str2, int i) {
        logRecord(str, str2, -1L, userAccounts, i);
    }

    private void logRecord(String str, String str2, long j, UserAccounts userAccounts) {
        logRecord(str, str2, j, userAccounts, getCallingUid());
    }

    private void logRecord(String str, String str2, long j, UserAccounts userAccounts, int i) {
        Runnable runnable = new Runnable(str, str2, j, userAccounts, i, userAccounts.debugDbInsertionPoint) {
            private final long accountId;
            private final String action;
            private final int callingUid;
            private final String tableName;
            private final UserAccounts userAccount;
            private final long userDebugDbInsertionPoint;

            {
                this.action = str;
                this.tableName = str2;
                this.accountId = j;
                this.userAccount = userAccounts;
                this.callingUid = i;
                this.userDebugDbInsertionPoint = j;
            }

            @Override
            public void run() {
                SQLiteStatement sQLiteStatement = this.userAccount.statementForLogging;
                sQLiteStatement.bindLong(1, this.accountId);
                sQLiteStatement.bindString(2, this.action);
                sQLiteStatement.bindString(3, AccountManagerService.this.mDateFormat.format(new Date()));
                sQLiteStatement.bindLong(4, this.callingUid);
                sQLiteStatement.bindString(5, this.tableName);
                sQLiteStatement.bindLong(6, this.userDebugDbInsertionPoint);
                try {
                    try {
                        sQLiteStatement.execute();
                    } catch (IllegalStateException e) {
                        Slog.w(AccountManagerService.TAG, "Failed to insert a log record. accountId=" + this.accountId + " action=" + this.action + " tableName=" + this.tableName + " Error: " + e);
                    }
                } finally {
                    sQLiteStatement.clearBindings();
                }
            }
        };
        userAccounts.debugDbInsertionPoint = (userAccounts.debugDbInsertionPoint + 1) % 64;
        this.mHandler.post(runnable);
    }

    private void initializeDebugDbSizeAndCompileSqlStatementForLogging(UserAccounts userAccounts) {
        userAccounts.debugDbInsertionPoint = userAccounts.accountsDb.calculateDebugTableInsertionPoint();
        userAccounts.statementForLogging = userAccounts.accountsDb.compileSqlStatementForLogging();
    }

    public IBinder onBind(Intent intent) {
        return asBinder();
    }

    private static boolean scanArgs(String[] strArr, String str) {
        if (strArr != null) {
            for (String str2 : strArr) {
                if (str.equals(str2)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, printWriter)) {
            boolean z = scanArgs(strArr, "--checkin") || scanArgs(strArr, "-c");
            IndentingPrintWriter indentingPrintWriter = new IndentingPrintWriter(printWriter, "  ");
            for (UserInfo userInfo : getUserManager().getUsers()) {
                indentingPrintWriter.println("User " + userInfo + ":");
                indentingPrintWriter.increaseIndent();
                dumpUser(getUserAccounts(userInfo.id), fileDescriptor, indentingPrintWriter, strArr, z);
                indentingPrintWriter.println();
                indentingPrintWriter.decreaseIndent();
            }
        }
    }

    private void dumpUser(UserAccounts userAccounts, FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr, boolean z) {
        boolean zIsLocalUnlockedUser;
        if (z) {
            synchronized (userAccounts.dbLock) {
                userAccounts.accountsDb.dumpDeAccountsTable(printWriter);
            }
            return;
        }
        Account[] accountsFromCache = getAccountsFromCache(userAccounts, null, 1000, null, false);
        printWriter.println("Accounts: " + accountsFromCache.length);
        for (Account account : accountsFromCache) {
            printWriter.println("  " + account);
        }
        printWriter.println();
        synchronized (userAccounts.dbLock) {
            userAccounts.accountsDb.dumpDebugTable(printWriter);
        }
        printWriter.println();
        synchronized (this.mSessions) {
            long jElapsedRealtime = SystemClock.elapsedRealtime();
            printWriter.println("Active Sessions: " + this.mSessions.size());
            Iterator<Session> it = this.mSessions.values().iterator();
            while (it.hasNext()) {
                printWriter.println("  " + it.next().toDebugString(jElapsedRealtime));
            }
        }
        printWriter.println();
        this.mAuthenticatorCache.dump(fileDescriptor, printWriter, strArr, userAccounts.userId);
        synchronized (this.mUsers) {
            zIsLocalUnlockedUser = isLocalUnlockedUser(userAccounts.userId);
        }
        if (!zIsLocalUnlockedUser) {
            return;
        }
        printWriter.println();
        synchronized (userAccounts.dbLock) {
            Map<Account, Map<String, Integer>> mapFindAllVisibilityValues = userAccounts.accountsDb.findAllVisibilityValues();
            printWriter.println("Account visibility:");
            for (Account account2 : mapFindAllVisibilityValues.keySet()) {
                printWriter.println("  " + account2.name);
                for (Map.Entry<String, Integer> entry : mapFindAllVisibilityValues.get(account2).entrySet()) {
                    printWriter.println("    " + entry.getKey() + ", " + entry.getValue());
                }
            }
        }
    }

    private void doNotification(UserAccounts userAccounts, Account account, CharSequence charSequence, Intent intent, String str, int i) {
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            if (Log.isLoggable(TAG, 2)) {
                Log.v(TAG, "doNotification: " + ((Object) charSequence) + " intent:" + intent);
            }
            if (intent.getComponent() != null && GrantCredentialsPermissionActivity.class.getName().equals(intent.getComponent().getClassName())) {
                createNoCredentialsPermissionNotification(account, intent, str, i);
            } else {
                Context contextForUser = getContextForUser(new UserHandle(i));
                NotificationId signinRequiredNotificationId = getSigninRequiredNotificationId(userAccounts, account);
                intent.addCategory(signinRequiredNotificationId.mTag);
                installNotification(signinRequiredNotificationId, new Notification.Builder(contextForUser, SystemNotificationChannels.ACCOUNT).setWhen(0L).setSmallIcon(R.drawable.stat_sys_warning).setColor(contextForUser.getColor(R.color.car_colorPrimary)).setContentTitle(String.format(contextForUser.getText(R.string.face_error_no_space).toString(), account.name)).setContentText(charSequence).setContentIntent(PendingIntent.getActivityAsUser(this.mContext, 0, intent, 268435456, null, new UserHandle(i))).build(), str, i);
            }
        } finally {
            restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void installNotification(NotificationId notificationId, Notification notification, String str, int i) {
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            try {
                this.mInjector.getNotificationManager().enqueueNotificationWithTag(str, str, notificationId.mTag, notificationId.mId, notification, i);
            } catch (RemoteException e) {
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void cancelNotification(NotificationId notificationId, UserHandle userHandle) {
        cancelNotification(notificationId, this.mContext.getPackageName(), userHandle);
    }

    private void cancelNotification(NotificationId notificationId, String str, UserHandle userHandle) {
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            this.mInjector.getNotificationManager().cancelNotificationWithTag(str, notificationId.mTag, notificationId.mId, userHandle.getIdentifier());
        } catch (RemoteException e) {
        } catch (Throwable th) {
            restoreCallingIdentity(jClearCallingIdentity);
            throw th;
        }
        restoreCallingIdentity(jClearCallingIdentity);
    }

    private boolean isPermittedForPackage(String str, int i, int i2, String... strArr) {
        int iPermissionToOpCode;
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            IPackageManager packageManager = ActivityThread.getPackageManager();
            for (String str2 : strArr) {
                if (packageManager.checkPermission(str2, str, i2) == 0 && ((iPermissionToOpCode = AppOpsManager.permissionToOpCode(str2)) == -1 || this.mAppOpsManager.noteOpNoThrow(iPermissionToOpCode, i, str) == 0)) {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                    return true;
                }
            }
        } catch (RemoteException e) {
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            throw th;
        }
        Binder.restoreCallingIdentity(jClearCallingIdentity);
        return false;
    }

    private boolean isPermitted(String str, int i, String... strArr) {
        for (String str2 : strArr) {
            if (this.mContext.checkCallingOrSelfPermission(str2) == 0) {
                if (Log.isLoggable(TAG, 2)) {
                    Log.v(TAG, "  caller uid " + i + " has " + str2);
                }
                int iPermissionToOpCode = AppOpsManager.permissionToOpCode(str2);
                if (iPermissionToOpCode == -1 || this.mAppOpsManager.noteOpNoThrow(iPermissionToOpCode, i, str) == 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private int handleIncomingUser(int i) {
        try {
            return ActivityManager.getService().handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), i, true, true, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, (String) null);
        } catch (RemoteException e) {
            return i;
        }
    }

    private boolean isPrivileged(int i) {
        PackageInfo packageInfo;
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            String[] packagesForUid = this.mPackageManager.getPackagesForUid(i);
            if (packagesForUid == null) {
                Log.d(TAG, "No packages for callingUid " + i);
                return false;
            }
            for (String str : packagesForUid) {
                try {
                    packageInfo = this.mPackageManager.getPackageInfo(str, 0);
                } catch (PackageManager.NameNotFoundException e) {
                    Log.d(TAG, "Package not found " + e.getMessage());
                }
                if (packageInfo != null && (packageInfo.applicationInfo.privateFlags & 8) != 0) {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                    return true;
                }
            }
            return false;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private boolean permissionIsGranted(Account account, String str, int i, int i2) {
        if (UserHandle.getAppId(i) == 1000) {
            if (Log.isLoggable(TAG, 2)) {
                Log.v(TAG, "Access to " + account + " granted calling uid is system");
            }
            return true;
        }
        if (isPrivileged(i)) {
            if (Log.isLoggable(TAG, 2)) {
                Log.v(TAG, "Access to " + account + " granted calling uid " + i + " privileged");
            }
            return true;
        }
        if (account != null && isAccountManagedByCaller(account.type, i, i2)) {
            if (Log.isLoggable(TAG, 2)) {
                Log.v(TAG, "Access to " + account + " granted calling uid " + i + " manages the account");
            }
            return true;
        }
        if (account != null && hasExplicitlyGrantedPermission(account, str, i)) {
            if (Log.isLoggable(TAG, 2)) {
                Log.v(TAG, "Access to " + account + " granted calling uid " + i + " user granted access");
            }
            return true;
        }
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "Access to " + account + " not granted for uid " + i);
            return false;
        }
        return false;
    }

    private boolean isAccountVisibleToCaller(String str, int i, int i2, String str2) {
        if (str == null) {
            return false;
        }
        return getTypesVisibleToCaller(i, i2, str2).contains(str);
    }

    private boolean checkGetAccountsPermission(String str, int i, int i2) {
        return isPermittedForPackage(str, i, i2, "android.permission.GET_ACCOUNTS", "android.permission.GET_ACCOUNTS_PRIVILEGED");
    }

    private boolean checkReadContactsPermission(String str, int i, int i2) {
        return isPermittedForPackage(str, i, i2, "android.permission.READ_CONTACTS");
    }

    private boolean accountTypeManagesContacts(String str, int i) {
        if (str == null) {
            return false;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            Collection<RegisteredServicesCache.ServiceInfo<AuthenticatorDescription>> allServices = this.mAuthenticatorCache.getAllServices(i);
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            for (RegisteredServicesCache.ServiceInfo<AuthenticatorDescription> serviceInfo : allServices) {
                if (str.equals(((AuthenticatorDescription) serviceInfo.type).type)) {
                    return isPermittedForPackage(((AuthenticatorDescription) serviceInfo.type).packageName, serviceInfo.uid, i, "android.permission.WRITE_CONTACTS");
                }
            }
            return false;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            throw th;
        }
    }

    private int checkPackageSignature(String str, int i, int i2) {
        if (str == null) {
            return 0;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            Collection<RegisteredServicesCache.ServiceInfo<AuthenticatorDescription>> allServices = this.mAuthenticatorCache.getAllServices(i2);
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            PackageManagerInternal packageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
            for (RegisteredServicesCache.ServiceInfo<AuthenticatorDescription> serviceInfo : allServices) {
                if (str.equals(((AuthenticatorDescription) serviceInfo.type).type)) {
                    if (serviceInfo.uid == i) {
                        return 2;
                    }
                    if (packageManagerInternal.hasSignatureCapability(serviceInfo.uid, i, 16)) {
                        return 1;
                    }
                }
            }
            return 0;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            throw th;
        }
    }

    private boolean isAccountManagedByCaller(String str, int i, int i2) {
        if (str == null) {
            return false;
        }
        return getTypesManagedByCaller(i, i2).contains(str);
    }

    private List<String> getTypesVisibleToCaller(int i, int i2, String str) {
        return getTypesForCaller(i, i2, true);
    }

    private List<String> getTypesManagedByCaller(int i, int i2) {
        return getTypesForCaller(i, i2, false);
    }

    private List<String> getTypesForCaller(int i, int i2, boolean z) {
        ArrayList arrayList = new ArrayList();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            Collection<RegisteredServicesCache.ServiceInfo<AuthenticatorDescription>> allServices = this.mAuthenticatorCache.getAllServices(i2);
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            PackageManagerInternal packageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
            for (RegisteredServicesCache.ServiceInfo<AuthenticatorDescription> serviceInfo : allServices) {
                if (z || packageManagerInternal.hasSignatureCapability(serviceInfo.uid, i, 16)) {
                    arrayList.add(((AuthenticatorDescription) serviceInfo.type).type);
                }
            }
            return arrayList;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            throw th;
        }
    }

    private boolean isAccountPresentForCaller(String str, String str2) {
        if (getUserAccountsForCaller().accountCache.containsKey(str2)) {
            for (Account account : getUserAccountsForCaller().accountCache.get(str2)) {
                if (account.name.equals(str)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void checkManageUsersPermission(String str) {
        if (ActivityManager.checkComponentPermission("android.permission.MANAGE_USERS", Binder.getCallingUid(), -1, true) != 0) {
            throw new SecurityException("You need MANAGE_USERS permission to: " + str);
        }
    }

    private static void checkManageOrCreateUsersPermission(String str) {
        if (ActivityManager.checkComponentPermission("android.permission.MANAGE_USERS", Binder.getCallingUid(), -1, true) != 0 && ActivityManager.checkComponentPermission("android.permission.CREATE_USERS", Binder.getCallingUid(), -1, true) != 0) {
            throw new SecurityException("You need MANAGE_USERS or CREATE_USERS permission to: " + str);
        }
    }

    private boolean hasExplicitlyGrantedPermission(Account account, String str, int i) {
        long jFindMatchingGrantsCountAnyToken;
        boolean z;
        if (UserHandle.getAppId(i) == 1000) {
            return true;
        }
        UserAccounts userAccounts = getUserAccounts(UserHandle.getUserId(i));
        synchronized (userAccounts.dbLock) {
            synchronized (userAccounts.cacheLock) {
                try {
                    if (str != null) {
                        jFindMatchingGrantsCountAnyToken = userAccounts.accountsDb.findMatchingGrantsCount(i, str, account);
                    } else {
                        jFindMatchingGrantsCountAnyToken = userAccounts.accountsDb.findMatchingGrantsCountAnyToken(i, account);
                    }
                    if (jFindMatchingGrantsCountAnyToken <= 0) {
                        z = false;
                    } else {
                        z = true;
                    }
                    if (z || !ActivityManager.isRunningInTestHarness()) {
                        return z;
                    }
                    Log.d(TAG, "no credentials permission for usage of " + account + ", " + str + " by uid " + i + " but ignoring since device is in test harness.");
                    return true;
                } catch (Throwable th) {
                    throw th;
                }
            }
        }
    }

    private boolean isSystemUid(int i) {
        PackageInfo packageInfo;
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            String[] packagesForUid = this.mPackageManager.getPackagesForUid(i);
            if (packagesForUid != null) {
                for (String str : packagesForUid) {
                    try {
                        packageInfo = this.mPackageManager.getPackageInfo(str, 0);
                    } catch (PackageManager.NameNotFoundException e) {
                        Log.w(TAG, String.format("Could not find package [%s]", str), e);
                    }
                    if (packageInfo != null && (packageInfo.applicationInfo.flags & 1) != 0) {
                        return true;
                    }
                }
            } else {
                Log.w(TAG, "No known packages with uid " + i);
            }
            return false;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void checkReadAccountsPermitted(int i, String str, int i2, String str2) {
        if (!isAccountVisibleToCaller(str, i, i2, str2)) {
            String str3 = String.format("caller uid %s cannot access %s accounts", Integer.valueOf(i), str);
            Log.w(TAG, "  " + str3);
            throw new SecurityException(str3);
        }
    }

    private boolean canUserModifyAccounts(int i, int i2) {
        return isProfileOwner(i2) || !getUserManager().getUserRestrictions(new UserHandle(i)).getBoolean("no_modify_accounts");
    }

    private boolean canUserModifyAccountsForType(int i, String str, int i2) {
        String[] accountTypesWithManagementDisabledAsUser;
        if (isProfileOwner(i2) || (accountTypesWithManagementDisabledAsUser = ((DevicePolicyManager) this.mContext.getSystemService("device_policy")).getAccountTypesWithManagementDisabledAsUser(i)) == null) {
            return true;
        }
        for (String str2 : accountTypesWithManagementDisabledAsUser) {
            if (str2.equals(str)) {
                return false;
            }
        }
        return true;
    }

    private boolean isProfileOwner(int i) {
        DevicePolicyManagerInternal devicePolicyManagerInternal = (DevicePolicyManagerInternal) LocalServices.getService(DevicePolicyManagerInternal.class);
        return devicePolicyManagerInternal != null && devicePolicyManagerInternal.isActiveAdminWithPolicy(i, -1);
    }

    public void updateAppPermission(Account account, String str, int i, boolean z) throws RemoteException {
        if (UserHandle.getAppId(getCallingUid()) != 1000) {
            throw new SecurityException();
        }
        if (z) {
            grantAppPermission(account, str, i);
        } else {
            revokeAppPermission(account, str, i);
        }
    }

    void grantAppPermission(final Account account, String str, final int i) {
        if (account == null || str == null) {
            Log.e(TAG, "grantAppPermission: called with invalid arguments", new Exception());
            return;
        }
        UserAccounts userAccounts = getUserAccounts(UserHandle.getUserId(i));
        synchronized (userAccounts.dbLock) {
            synchronized (userAccounts.cacheLock) {
                long jFindDeAccountId = userAccounts.accountsDb.findDeAccountId(account);
                if (jFindDeAccountId >= 0) {
                    userAccounts.accountsDb.insertGrant(jFindDeAccountId, str, i);
                }
                cancelNotification(getCredentialPermissionNotificationId(account, str, i), UserHandle.of(userAccounts.userId));
                cancelAccountAccessRequestNotificationIfNeeded(account, i, true);
            }
        }
        for (final AccountManagerInternal.OnAppPermissionChangeListener onAppPermissionChangeListener : this.mAppPermissionChangeListeners) {
            this.mHandler.post(new Runnable() {
                @Override
                public final void run() {
                    onAppPermissionChangeListener.onAppPermissionChanged(account, i);
                }
            });
        }
    }

    private void revokeAppPermission(final Account account, String str, final int i) {
        if (account == null || str == null) {
            Log.e(TAG, "revokeAppPermission: called with invalid arguments", new Exception());
            return;
        }
        UserAccounts userAccounts = getUserAccounts(UserHandle.getUserId(i));
        synchronized (userAccounts.dbLock) {
            synchronized (userAccounts.cacheLock) {
                userAccounts.accountsDb.beginTransaction();
                try {
                    long jFindDeAccountId = userAccounts.accountsDb.findDeAccountId(account);
                    if (jFindDeAccountId >= 0) {
                        userAccounts.accountsDb.deleteGrantsByAccountIdAuthTokenTypeAndUid(jFindDeAccountId, str, i);
                        userAccounts.accountsDb.setTransactionSuccessful();
                    }
                    userAccounts.accountsDb.endTransaction();
                    cancelNotification(getCredentialPermissionNotificationId(account, str, i), UserHandle.of(userAccounts.userId));
                } catch (Throwable th) {
                    userAccounts.accountsDb.endTransaction();
                    throw th;
                }
            }
        }
        for (final AccountManagerInternal.OnAppPermissionChangeListener onAppPermissionChangeListener : this.mAppPermissionChangeListeners) {
            this.mHandler.post(new Runnable() {
                @Override
                public final void run() {
                    onAppPermissionChangeListener.onAppPermissionChanged(account, i);
                }
            });
        }
    }

    private void removeAccountFromCacheLocked(UserAccounts userAccounts, Account account) {
        Account[] accountArr = userAccounts.accountCache.get(account.type);
        if (accountArr != null) {
            ArrayList arrayList = new ArrayList();
            for (Account account2 : accountArr) {
                if (!account2.equals(account)) {
                    arrayList.add(account2);
                }
            }
            if (arrayList.isEmpty()) {
                userAccounts.accountCache.remove(account.type);
            } else {
                userAccounts.accountCache.put(account.type, (Account[]) arrayList.toArray(new Account[arrayList.size()]));
            }
        }
        userAccounts.userDataCache.remove(account);
        userAccounts.authTokenCache.remove(account);
        userAccounts.previousNameCache.remove(account);
        userAccounts.visibilityCache.remove(account);
    }

    private Account insertAccountIntoCacheLocked(UserAccounts userAccounts, Account account) {
        Account[] accountArr = userAccounts.accountCache.get(account.type);
        int length = accountArr != null ? accountArr.length : 0;
        Account[] accountArr2 = new Account[length + 1];
        if (accountArr != null) {
            System.arraycopy(accountArr, 0, accountArr2, 0, length);
        }
        accountArr2[length] = new Account(account, account.getAccessId() != null ? account.getAccessId() : UUID.randomUUID().toString());
        userAccounts.accountCache.put(account.type, accountArr2);
        return accountArr2[length];
    }

    private Account[] filterAccounts(UserAccounts userAccounts, Account[] accountArr, int i, String str, boolean z) {
        String packageNameForUid;
        if (str == null) {
            packageNameForUid = getPackageNameForUid(i);
        } else {
            packageNameForUid = str;
        }
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        for (Account account : accountArr) {
            int iIntValue = resolveAccountVisibility(account, packageNameForUid, userAccounts).intValue();
            if (iIntValue == 1 || iIntValue == 2 || (z && iIntValue == 4)) {
                linkedHashMap.put(account, Integer.valueOf(iIntValue));
            }
        }
        Map<Account, Integer> mapFilterSharedAccounts = filterSharedAccounts(userAccounts, linkedHashMap, i, str);
        return (Account[]) mapFilterSharedAccounts.keySet().toArray(new Account[mapFilterSharedAccounts.size()]);
    }

    private Map<Account, Integer> filterSharedAccounts(UserAccounts userAccounts, Map<Account, Integer> map, int i, String str) {
        UserInfo userInfo;
        boolean z;
        String str2;
        if (getUserManager() != null && userAccounts != null && userAccounts.userId >= 0 && i != 1000 && (userInfo = getUserManager().getUserInfo(userAccounts.userId)) != null && userInfo.isRestricted()) {
            String[] packagesForUid = this.mPackageManager.getPackagesForUid(i);
            if (packagesForUid == null) {
                packagesForUid = new String[0];
            }
            String string = this.mContext.getResources().getString(R.string.accessibility_system_action_dpad_down_label);
            for (String str3 : packagesForUid) {
                if (string.contains(";" + str3 + ";")) {
                    return map;
                }
            }
            Account[] sharedAccountsAsUser = getSharedAccountsAsUser(userAccounts.userId);
            if (ArrayUtils.isEmpty(sharedAccountsAsUser)) {
                return map;
            }
            String str4 = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            try {
            } catch (PackageManager.NameNotFoundException e) {
                Log.d(TAG, "Package not found " + e.getMessage());
            }
            if (str != null) {
                PackageInfo packageInfo = this.mPackageManager.getPackageInfo(str, 0);
                if (packageInfo != null && packageInfo.restrictedAccountType != null) {
                    str2 = packageInfo.restrictedAccountType;
                } else {
                    str2 = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
                }
            } else {
                for (String str5 : packagesForUid) {
                    PackageInfo packageInfo2 = this.mPackageManager.getPackageInfo(str5, 0);
                    if (packageInfo2 != null && packageInfo2.restrictedAccountType != null) {
                        str2 = packageInfo2.restrictedAccountType;
                    }
                }
                LinkedHashMap linkedHashMap = new LinkedHashMap();
                for (Map.Entry<Account, Integer> entry : map.entrySet()) {
                    Account key = entry.getKey();
                    if (key.type.equals(str4)) {
                        linkedHashMap.put(key, entry.getValue());
                    } else {
                        int length = sharedAccountsAsUser.length;
                        int i2 = 0;
                        while (true) {
                            if (i2 < length) {
                                if (!sharedAccountsAsUser[i2].equals(key)) {
                                    i2++;
                                } else {
                                    z = true;
                                    break;
                                }
                            } else {
                                z = false;
                                break;
                            }
                        }
                        if (!z) {
                            linkedHashMap.put(key, entry.getValue());
                        }
                    }
                }
                return linkedHashMap;
            }
            str4 = str2;
            LinkedHashMap linkedHashMap2 = new LinkedHashMap();
            while (r9.hasNext()) {
            }
            return linkedHashMap2;
        }
        return map;
    }

    protected Account[] getAccountsFromCache(UserAccounts userAccounts, String str, int i, String str2, boolean z) {
        Account[] accountArr;
        Preconditions.checkState(!Thread.holdsLock(userAccounts.cacheLock), "Method should not be called with cacheLock");
        if (str != null) {
            synchronized (userAccounts.cacheLock) {
                accountArr = userAccounts.accountCache.get(str);
            }
            if (accountArr == null) {
                return EMPTY_ACCOUNT_ARRAY;
            }
            return filterAccounts(userAccounts, (Account[]) Arrays.copyOf(accountArr, accountArr.length), i, str2, z);
        }
        synchronized (userAccounts.cacheLock) {
            Iterator<Account[]> it = userAccounts.accountCache.values().iterator();
            int length = 0;
            while (it.hasNext()) {
                length += it.next().length;
            }
            if (length == 0) {
                return EMPTY_ACCOUNT_ARRAY;
            }
            Account[] accountArr2 = new Account[length];
            int length2 = 0;
            for (Account[] accountArr3 : userAccounts.accountCache.values()) {
                System.arraycopy(accountArr3, 0, accountArr2, length2, accountArr3.length);
                length2 += accountArr3.length;
            }
            return filterAccounts(userAccounts, accountArr2, i, str2, z);
        }
    }

    protected void writeUserDataIntoCacheLocked(UserAccounts userAccounts, Account account, String str, String str2) {
        Map<String, String> mapFindUserExtrasForAccount = (Map) userAccounts.userDataCache.get(account);
        if (mapFindUserExtrasForAccount == null) {
            mapFindUserExtrasForAccount = userAccounts.accountsDb.findUserExtrasForAccount(account);
            userAccounts.userDataCache.put(account, mapFindUserExtrasForAccount);
        }
        if (str2 == null) {
            mapFindUserExtrasForAccount.remove(str);
        } else {
            mapFindUserExtrasForAccount.put(str, str2);
        }
    }

    protected String readCachedTokenInternal(UserAccounts userAccounts, Account account, String str, String str2, byte[] bArr) {
        String str3;
        synchronized (userAccounts.cacheLock) {
            str3 = userAccounts.accountTokenCaches.get(account, str, str2, bArr);
        }
        return str3;
    }

    protected void writeAuthTokenIntoCacheLocked(UserAccounts userAccounts, Account account, String str, String str2) {
        Map<String, String> mapFindAuthTokensByAccount = (Map) userAccounts.authTokenCache.get(account);
        if (mapFindAuthTokensByAccount == null) {
            mapFindAuthTokensByAccount = userAccounts.accountsDb.findAuthTokensByAccount(account);
            userAccounts.authTokenCache.put(account, mapFindAuthTokensByAccount);
        }
        if (str2 == null) {
            mapFindAuthTokensByAccount.remove(str);
        } else {
            mapFindAuthTokensByAccount.put(str, str2);
        }
    }

    protected String readAuthTokenInternal(UserAccounts userAccounts, Account account, String str) {
        String str2;
        synchronized (userAccounts.cacheLock) {
            Map map = (Map) userAccounts.authTokenCache.get(account);
            if (map != null) {
                return (String) map.get(str);
            }
            synchronized (userAccounts.dbLock) {
                synchronized (userAccounts.cacheLock) {
                    Map<String, String> mapFindAuthTokensByAccount = (Map) userAccounts.authTokenCache.get(account);
                    if (mapFindAuthTokensByAccount == null) {
                        mapFindAuthTokensByAccount = userAccounts.accountsDb.findAuthTokensByAccount(account);
                        userAccounts.authTokenCache.put(account, mapFindAuthTokensByAccount);
                    }
                    str2 = mapFindAuthTokensByAccount.get(str);
                }
            }
            return str2;
        }
    }

    private String readUserDataInternal(UserAccounts userAccounts, Account account, String str) {
        Map<String, String> map;
        Map<String, String> mapFindUserExtrasForAccount;
        synchronized (userAccounts.cacheLock) {
            map = (Map) userAccounts.userDataCache.get(account);
        }
        if (map == null) {
            synchronized (userAccounts.dbLock) {
                synchronized (userAccounts.cacheLock) {
                    mapFindUserExtrasForAccount = (Map) userAccounts.userDataCache.get(account);
                    if (mapFindUserExtrasForAccount == null) {
                        mapFindUserExtrasForAccount = userAccounts.accountsDb.findUserExtrasForAccount(account);
                        userAccounts.userDataCache.put(account, mapFindUserExtrasForAccount);
                    }
                }
            }
            map = mapFindUserExtrasForAccount;
        }
        return map.get(str);
    }

    private Context getContextForUser(UserHandle userHandle) {
        try {
            return this.mContext.createPackageContextAsUser(this.mContext.getPackageName(), 0, userHandle);
        } catch (PackageManager.NameNotFoundException e) {
            return this.mContext;
        }
    }

    private void sendResponse(IAccountManagerResponse iAccountManagerResponse, Bundle bundle) {
        try {
            iAccountManagerResponse.onResult(bundle);
        } catch (RemoteException e) {
            if (Log.isLoggable(TAG, 2)) {
                Log.v(TAG, "failure while notifying response", e);
            }
        }
    }

    private void sendErrorResponse(IAccountManagerResponse iAccountManagerResponse, int i, String str) {
        try {
            iAccountManagerResponse.onError(i, str);
        } catch (RemoteException e) {
            if (Log.isLoggable(TAG, 2)) {
                Log.v(TAG, "failure while notifying response", e);
            }
        }
    }

    private final class AccountManagerInternalImpl extends AccountManagerInternal {

        @GuardedBy("mLock")
        private AccountManagerBackupHelper mBackupHelper;
        private final Object mLock;

        private AccountManagerInternalImpl() {
            this.mLock = new Object();
        }

        public void requestAccountAccess(Account account, String str, int i, RemoteCallback remoteCallback) {
            UserAccounts userAccounts;
            if (account == null) {
                Slog.w(AccountManagerService.TAG, "account cannot be null");
                return;
            }
            if (str == null) {
                Slog.w(AccountManagerService.TAG, "packageName cannot be null");
                return;
            }
            if (i < 0) {
                Slog.w(AccountManagerService.TAG, "user id must be concrete");
                return;
            }
            if (remoteCallback == null) {
                Slog.w(AccountManagerService.TAG, "callback cannot be null");
                return;
            }
            if (AccountManagerService.this.resolveAccountVisibility(account, str, AccountManagerService.this.getUserAccounts(i)).intValue() == 3) {
                Slog.w(AccountManagerService.TAG, "requestAccountAccess: account is hidden");
                return;
            }
            if (!AccountManagerService.this.hasAccountAccess(account, str, new UserHandle(i))) {
                try {
                    int packageUidAsUser = AccountManagerService.this.mPackageManager.getPackageUidAsUser(str, i);
                    Intent intentNewRequestAccountAccessIntent = AccountManagerService.this.newRequestAccountAccessIntent(account, str, packageUidAsUser, remoteCallback);
                    synchronized (AccountManagerService.this.mUsers) {
                        userAccounts = (UserAccounts) AccountManagerService.this.mUsers.get(i);
                    }
                    SystemNotificationChannels.createAccountChannelForPackage(str, packageUidAsUser, AccountManagerService.this.mContext);
                    AccountManagerService.this.doNotification(userAccounts, account, null, intentNewRequestAccountAccessIntent, str, i);
                    return;
                } catch (PackageManager.NameNotFoundException e) {
                    Slog.e(AccountManagerService.TAG, "Unknown package " + str);
                    return;
                }
            }
            Bundle bundle = new Bundle();
            bundle.putBoolean("booleanResult", true);
            remoteCallback.sendResult(bundle);
        }

        public void addOnAppPermissionChangeListener(AccountManagerInternal.OnAppPermissionChangeListener onAppPermissionChangeListener) {
            AccountManagerService.this.mAppPermissionChangeListeners.add(onAppPermissionChangeListener);
        }

        public boolean hasAccountAccess(Account account, int i) {
            return AccountManagerService.this.hasAccountAccess(account, (String) null, i);
        }

        public byte[] backupAccountAccessPermissions(int i) {
            byte[] bArrBackupAccountAccessPermissions;
            synchronized (this.mLock) {
                if (this.mBackupHelper == null) {
                    this.mBackupHelper = new AccountManagerBackupHelper(AccountManagerService.this, this);
                }
                bArrBackupAccountAccessPermissions = this.mBackupHelper.backupAccountAccessPermissions(i);
            }
            return bArrBackupAccountAccessPermissions;
        }

        public void restoreAccountAccessPermissions(byte[] bArr, int i) {
            synchronized (this.mLock) {
                if (this.mBackupHelper == null) {
                    this.mBackupHelper = new AccountManagerBackupHelper(AccountManagerService.this, this);
                }
                this.mBackupHelper.restoreAccountAccessPermissions(bArr, i);
            }
        }
    }

    @VisibleForTesting
    static class Injector {
        private final Context mContext;

        public Injector(Context context) {
            this.mContext = context;
        }

        Looper getMessageHandlerLooper() {
            ServiceThread serviceThread = new ServiceThread(AccountManagerService.TAG, -2, true);
            serviceThread.start();
            return serviceThread.getLooper();
        }

        Context getContext() {
            return this.mContext;
        }

        void addLocalService(AccountManagerInternal accountManagerInternal) {
            LocalServices.addService(AccountManagerInternal.class, accountManagerInternal);
        }

        String getDeDatabaseName(int i) {
            return new File(Environment.getDataSystemDeDirectory(i), "accounts_de.db").getPath();
        }

        String getCeDatabaseName(int i) {
            return new File(Environment.getDataSystemCeDirectory(i), "accounts_ce.db").getPath();
        }

        String getPreNDatabaseName(int i) {
            File dataSystemDirectory = Environment.getDataSystemDirectory();
            File file = new File(Environment.getUserSystemDirectory(i), AccountManagerService.PRE_N_DATABASE_NAME);
            if (i == 0) {
                File file2 = new File(dataSystemDirectory, AccountManagerService.PRE_N_DATABASE_NAME);
                if (file2.exists() && !file.exists()) {
                    File userSystemDirectory = Environment.getUserSystemDirectory(i);
                    if (!userSystemDirectory.exists() && !userSystemDirectory.mkdirs()) {
                        throw new IllegalStateException("User dir cannot be created: " + userSystemDirectory);
                    }
                    if (!file2.renameTo(file)) {
                        throw new IllegalStateException("User dir cannot be migrated: " + file);
                    }
                }
            }
            return file.getPath();
        }

        IAccountAuthenticatorCache getAccountAuthenticatorCache() {
            return new AccountAuthenticatorCache(this.mContext);
        }

        INotificationManager getNotificationManager() {
            return NotificationManager.getService();
        }
    }

    private static class NotificationId {
        private final int mId;
        final String mTag;

        NotificationId(String str, int i) {
            this.mTag = str;
            this.mId = i;
        }
    }
}
