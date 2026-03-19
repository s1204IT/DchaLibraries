package android.accounts;

import android.accounts.IAccountManagerResponse;
import android.annotation.SystemApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.res.Resources;
import android.database.SQLException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.R;
import com.google.android.collect.Maps;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class AccountManager {
    public static final String ACCOUNT_ACCESS_TOKEN_TYPE = "com.android.AccountManager.ACCOUNT_ACCESS_TOKEN_TYPE";
    public static final String ACTION_ACCOUNT_REMOVED = "android.accounts.action.ACCOUNT_REMOVED";
    public static final String ACTION_AUTHENTICATOR_INTENT = "android.accounts.AccountAuthenticator";
    public static final String ACTION_VISIBLE_ACCOUNTS_CHANGED = "android.accounts.action.VISIBLE_ACCOUNTS_CHANGED";
    public static final String AUTHENTICATOR_ATTRIBUTES_NAME = "account-authenticator";
    public static final String AUTHENTICATOR_META_DATA_NAME = "android.accounts.AccountAuthenticator";
    public static final int ERROR_CODE_BAD_ARGUMENTS = 7;
    public static final int ERROR_CODE_BAD_AUTHENTICATION = 9;
    public static final int ERROR_CODE_BAD_REQUEST = 8;
    public static final int ERROR_CODE_CANCELED = 4;
    public static final int ERROR_CODE_INVALID_RESPONSE = 5;
    public static final int ERROR_CODE_MANAGEMENT_DISABLED_FOR_ACCOUNT_TYPE = 101;
    public static final int ERROR_CODE_NETWORK_ERROR = 3;
    public static final int ERROR_CODE_REMOTE_EXCEPTION = 1;
    public static final int ERROR_CODE_UNSUPPORTED_OPERATION = 6;
    public static final int ERROR_CODE_USER_RESTRICTED = 100;
    public static final String KEY_ACCOUNTS = "accounts";
    public static final String KEY_ACCOUNT_ACCESS_ID = "accountAccessId";
    public static final String KEY_ACCOUNT_AUTHENTICATOR_RESPONSE = "accountAuthenticatorResponse";
    public static final String KEY_ACCOUNT_MANAGER_RESPONSE = "accountManagerResponse";
    public static final String KEY_ACCOUNT_NAME = "authAccount";
    public static final String KEY_ACCOUNT_SESSION_BUNDLE = "accountSessionBundle";
    public static final String KEY_ACCOUNT_STATUS_TOKEN = "accountStatusToken";
    public static final String KEY_ACCOUNT_TYPE = "accountType";
    public static final String KEY_ANDROID_PACKAGE_NAME = "androidPackageName";
    public static final String KEY_AUTHENTICATOR_TYPES = "authenticator_types";
    public static final String KEY_AUTHTOKEN = "authtoken";
    public static final String KEY_AUTH_FAILED_MESSAGE = "authFailedMessage";
    public static final String KEY_AUTH_TOKEN_LABEL = "authTokenLabelKey";
    public static final String KEY_BOOLEAN_RESULT = "booleanResult";
    public static final String KEY_CALLER_PID = "callerPid";
    public static final String KEY_CALLER_UID = "callerUid";
    public static final String KEY_ERROR_CODE = "errorCode";
    public static final String KEY_ERROR_MESSAGE = "errorMessage";
    public static final String KEY_INTENT = "intent";
    public static final String KEY_LAST_AUTHENTICATED_TIME = "lastAuthenticatedTime";
    public static final String KEY_NOTIFY_ON_FAILURE = "notifyOnAuthFailure";
    public static final String KEY_PASSWORD = "password";
    public static final String KEY_USERDATA = "userdata";
    public static final String LOGIN_ACCOUNTS_CHANGED_ACTION = "android.accounts.LOGIN_ACCOUNTS_CHANGED";
    public static final String PACKAGE_NAME_KEY_LEGACY_NOT_VISIBLE = "android:accounts:key_legacy_not_visible";
    public static final String PACKAGE_NAME_KEY_LEGACY_VISIBLE = "android:accounts:key_legacy_visible";
    private static final String TAG = "AccountManager";
    public static final int VISIBILITY_NOT_VISIBLE = 3;
    public static final int VISIBILITY_UNDEFINED = 0;
    public static final int VISIBILITY_USER_MANAGED_NOT_VISIBLE = 4;
    public static final int VISIBILITY_USER_MANAGED_VISIBLE = 2;
    public static final int VISIBILITY_VISIBLE = 1;
    private final Context mContext;
    private final Handler mMainHandler;
    private final IAccountManager mService;
    private final HashMap<OnAccountsUpdateListener, Handler> mAccountsUpdatedListeners = Maps.newHashMap();
    private final HashMap<OnAccountsUpdateListener, Set<String>> mAccountsUpdatedListenersTypes = Maps.newHashMap();
    private final BroadcastReceiver mAccountsChangedBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Account[] accounts = AccountManager.this.getAccounts();
            synchronized (AccountManager.this.mAccountsUpdatedListeners) {
                for (Map.Entry entry : AccountManager.this.mAccountsUpdatedListeners.entrySet()) {
                    AccountManager.this.postToHandler((Handler) entry.getValue(), (OnAccountsUpdateListener) entry.getKey(), accounts);
                }
            }
        }
    };

    @Retention(RetentionPolicy.SOURCE)
    public @interface AccountVisibility {
    }

    public AccountManager(Context context, IAccountManager iAccountManager) {
        this.mContext = context;
        this.mService = iAccountManager;
        this.mMainHandler = new Handler(this.mContext.getMainLooper());
    }

    public AccountManager(Context context, IAccountManager iAccountManager, Handler handler) {
        this.mContext = context;
        this.mService = iAccountManager;
        this.mMainHandler = handler;
    }

    public static Bundle sanitizeResult(Bundle bundle) {
        if (bundle != null && bundle.containsKey(KEY_AUTHTOKEN) && !TextUtils.isEmpty(bundle.getString(KEY_AUTHTOKEN))) {
            Bundle bundle2 = new Bundle(bundle);
            bundle2.putString(KEY_AUTHTOKEN, "<omitted for logging purposes>");
            return bundle2;
        }
        return bundle;
    }

    public static AccountManager get(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context is null");
        }
        return (AccountManager) context.getSystemService("account");
    }

    public String getPassword(Account account) {
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        try {
            return this.mService.getPassword(account);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public String getUserData(Account account, String str) {
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        if (str == null) {
            throw new IllegalArgumentException("key is null");
        }
        try {
            return this.mService.getUserData(account, str);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public AuthenticatorDescription[] getAuthenticatorTypes() {
        try {
            return this.mService.getAuthenticatorTypes(UserHandle.getCallingUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public AuthenticatorDescription[] getAuthenticatorTypesAsUser(int i) {
        try {
            return this.mService.getAuthenticatorTypes(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Account[] getAccounts() {
        try {
            return this.mService.getAccounts(null, this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Account[] getAccountsAsUser(int i) {
        try {
            return this.mService.getAccountsAsUser(null, i, this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Account[] getAccountsForPackage(String str, int i) {
        try {
            return this.mService.getAccountsForPackage(str, i, this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Account[] getAccountsByTypeForPackage(String str, String str2) {
        try {
            return this.mService.getAccountsByTypeForPackage(str, str2, this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Account[] getAccountsByType(String str) {
        return getAccountsByTypeAsUser(str, this.mContext.getUser());
    }

    public Account[] getAccountsByTypeAsUser(String str, UserHandle userHandle) {
        try {
            return this.mService.getAccountsAsUser(str, userHandle.getIdentifier(), this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void updateAppPermission(Account account, String str, int i, boolean z) {
        try {
            this.mService.updateAppPermission(account, str, i, z);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public AccountManagerFuture<String> getAuthTokenLabel(final String str, final String str2, AccountManagerCallback<String> accountManagerCallback, Handler handler) {
        if (str == null) {
            throw new IllegalArgumentException("accountType is null");
        }
        if (str2 == null) {
            throw new IllegalArgumentException("authTokenType is null");
        }
        return new Future2Task<String>(handler, accountManagerCallback) {
            @Override
            public void doWork() throws RemoteException {
                AccountManager.this.mService.getAuthTokenLabel(this.mResponse, str, str2);
            }

            @Override
            public String bundleToResult(Bundle bundle) throws AuthenticatorException {
                if (!bundle.containsKey(AccountManager.KEY_AUTH_TOKEN_LABEL)) {
                    throw new AuthenticatorException("no result in response");
                }
                return bundle.getString(AccountManager.KEY_AUTH_TOKEN_LABEL);
            }
        }.start();
    }

    public AccountManagerFuture<Boolean> hasFeatures(final Account account, final String[] strArr, AccountManagerCallback<Boolean> accountManagerCallback, Handler handler) {
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        if (strArr == null) {
            throw new IllegalArgumentException("features is null");
        }
        return new Future2Task<Boolean>(handler, accountManagerCallback) {
            @Override
            public void doWork() throws RemoteException {
                AccountManager.this.mService.hasFeatures(this.mResponse, account, strArr, AccountManager.this.mContext.getOpPackageName());
            }

            @Override
            public Boolean bundleToResult(Bundle bundle) throws AuthenticatorException {
                if (!bundle.containsKey(AccountManager.KEY_BOOLEAN_RESULT)) {
                    throw new AuthenticatorException("no result in response");
                }
                return Boolean.valueOf(bundle.getBoolean(AccountManager.KEY_BOOLEAN_RESULT));
            }
        }.start();
    }

    public AccountManagerFuture<Account[]> getAccountsByTypeAndFeatures(final String str, final String[] strArr, AccountManagerCallback<Account[]> accountManagerCallback, Handler handler) {
        if (str == null) {
            throw new IllegalArgumentException("type is null");
        }
        return new Future2Task<Account[]>(handler, accountManagerCallback) {
            @Override
            public void doWork() throws RemoteException {
                AccountManager.this.mService.getAccountsByFeatures(this.mResponse, str, strArr, AccountManager.this.mContext.getOpPackageName());
            }

            @Override
            public Account[] bundleToResult(Bundle bundle) throws AuthenticatorException {
                if (!bundle.containsKey(AccountManager.KEY_ACCOUNTS)) {
                    throw new AuthenticatorException("no result in response");
                }
                Parcelable[] parcelableArray = bundle.getParcelableArray(AccountManager.KEY_ACCOUNTS);
                Account[] accountArr = new Account[parcelableArray.length];
                for (int i = 0; i < parcelableArray.length; i++) {
                    accountArr[i] = (Account) parcelableArray[i];
                }
                return accountArr;
            }
        }.start();
    }

    public boolean addAccountExplicitly(Account account, String str, Bundle bundle) {
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        try {
            return this.mService.addAccountExplicitly(account, str, bundle);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean addAccountExplicitly(Account account, String str, Bundle bundle, Map<String, Integer> map) {
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        try {
            return this.mService.addAccountExplicitlyWithVisibility(account, str, bundle, map);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Map<String, Integer> getPackagesAndVisibilityForAccount(Account account) {
        try {
            if (account == null) {
                throw new IllegalArgumentException("account is null");
            }
            return this.mService.getPackagesAndVisibilityForAccount(account);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Map<Account, Integer> getAccountsAndVisibilityForPackage(String str, String str2) {
        try {
            return this.mService.getAccountsAndVisibilityForPackage(str, str2);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean setAccountVisibility(Account account, String str, int i) {
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        try {
            return this.mService.setAccountVisibility(account, str, i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getAccountVisibility(Account account, String str) {
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        try {
            return this.mService.getAccountVisibility(account, str);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean notifyAccountAuthenticated(Account account) {
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        try {
            return this.mService.accountAuthenticated(account);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public AccountManagerFuture<Account> renameAccount(final Account account, final String str, AccountManagerCallback<Account> accountManagerCallback, Handler handler) {
        if (account == null) {
            throw new IllegalArgumentException("account is null.");
        }
        if (TextUtils.isEmpty(str)) {
            throw new IllegalArgumentException("newName is empty or null.");
        }
        return new Future2Task<Account>(handler, accountManagerCallback) {
            @Override
            public void doWork() throws RemoteException {
                AccountManager.this.mService.renameAccount(this.mResponse, account, str);
            }

            @Override
            public Account bundleToResult(Bundle bundle) throws AuthenticatorException {
                return new Account(bundle.getString(AccountManager.KEY_ACCOUNT_NAME), bundle.getString("accountType"), bundle.getString(AccountManager.KEY_ACCOUNT_ACCESS_ID));
            }
        }.start();
    }

    public String getPreviousName(Account account) {
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        try {
            return this.mService.getPreviousName(account);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public AccountManagerFuture<Boolean> removeAccount(final Account account, AccountManagerCallback<Boolean> accountManagerCallback, Handler handler) {
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        return new Future2Task<Boolean>(handler, accountManagerCallback) {
            @Override
            public void doWork() throws RemoteException {
                AccountManager.this.mService.removeAccount(this.mResponse, account, false);
            }

            @Override
            public Boolean bundleToResult(Bundle bundle) throws AuthenticatorException {
                if (!bundle.containsKey(AccountManager.KEY_BOOLEAN_RESULT)) {
                    throw new AuthenticatorException("no result in response");
                }
                return Boolean.valueOf(bundle.getBoolean(AccountManager.KEY_BOOLEAN_RESULT));
            }
        }.start();
    }

    public AccountManagerFuture<Bundle> removeAccount(final Account account, final Activity activity, AccountManagerCallback<Bundle> accountManagerCallback, Handler handler) {
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        return new AmsTask(activity, handler, accountManagerCallback) {
            @Override
            public void doWork() throws RemoteException {
                AccountManager.this.mService.removeAccount(this.mResponse, account, activity != null);
            }
        }.start();
    }

    @Deprecated
    public AccountManagerFuture<Boolean> removeAccountAsUser(final Account account, AccountManagerCallback<Boolean> accountManagerCallback, Handler handler, final UserHandle userHandle) {
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        if (userHandle == null) {
            throw new IllegalArgumentException("userHandle is null");
        }
        return new Future2Task<Boolean>(handler, accountManagerCallback) {
            @Override
            public void doWork() throws RemoteException {
                AccountManager.this.mService.removeAccountAsUser(this.mResponse, account, false, userHandle.getIdentifier());
            }

            @Override
            public Boolean bundleToResult(Bundle bundle) throws AuthenticatorException {
                if (!bundle.containsKey(AccountManager.KEY_BOOLEAN_RESULT)) {
                    throw new AuthenticatorException("no result in response");
                }
                return Boolean.valueOf(bundle.getBoolean(AccountManager.KEY_BOOLEAN_RESULT));
            }
        }.start();
    }

    public AccountManagerFuture<Bundle> removeAccountAsUser(final Account account, final Activity activity, AccountManagerCallback<Bundle> accountManagerCallback, Handler handler, final UserHandle userHandle) {
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        if (userHandle == null) {
            throw new IllegalArgumentException("userHandle is null");
        }
        return new AmsTask(activity, handler, accountManagerCallback) {
            @Override
            public void doWork() throws RemoteException {
                AccountManager.this.mService.removeAccountAsUser(this.mResponse, account, activity != null, userHandle.getIdentifier());
            }
        }.start();
    }

    public boolean removeAccountExplicitly(Account account) {
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        try {
            return this.mService.removeAccountExplicitly(account);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void invalidateAuthToken(String str, String str2) {
        if (str == null) {
            throw new IllegalArgumentException("accountType is null");
        }
        if (str2 != null) {
            try {
                this.mService.invalidateAuthToken(str, str2);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public String peekAuthToken(Account account, String str) {
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        if (str == null) {
            throw new IllegalArgumentException("authTokenType is null");
        }
        try {
            return this.mService.peekAuthToken(account, str);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setPassword(Account account, String str) {
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        try {
            this.mService.setPassword(account, str);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void clearPassword(Account account) {
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        try {
            this.mService.clearPassword(account);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setUserData(Account account, String str, String str2) {
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        if (str == null) {
            throw new IllegalArgumentException("key is null");
        }
        try {
            this.mService.setUserData(account, str, str2);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setAuthToken(Account account, String str, String str2) {
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        if (str == null) {
            throw new IllegalArgumentException("authTokenType is null");
        }
        try {
            this.mService.setAuthToken(account, str, str2);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public String blockingGetAuthToken(Account account, String str, boolean z) throws OperationCanceledException, IOException, AuthenticatorException {
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        if (str == null) {
            throw new IllegalArgumentException("authTokenType is null");
        }
        Bundle result = getAuthToken(account, str, z, null, null).getResult();
        if (result == null) {
            Log.e(TAG, "blockingGetAuthToken: null was returned from getResult() for " + account + ", authTokenType " + str);
            return null;
        }
        return result.getString(KEY_AUTHTOKEN);
    }

    public AccountManagerFuture<Bundle> getAuthToken(final Account account, final String str, Bundle bundle, Activity activity, AccountManagerCallback<Bundle> accountManagerCallback, Handler handler) {
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        if (str == null) {
            throw new IllegalArgumentException("authTokenType is null");
        }
        final Bundle bundle2 = new Bundle();
        if (bundle != null) {
            bundle2.putAll(bundle);
        }
        bundle2.putString(KEY_ANDROID_PACKAGE_NAME, this.mContext.getPackageName());
        return new AmsTask(activity, handler, accountManagerCallback) {
            @Override
            public void doWork() throws RemoteException {
                AccountManager.this.mService.getAuthToken(this.mResponse, account, str, false, true, bundle2);
            }
        }.start();
    }

    @Deprecated
    public AccountManagerFuture<Bundle> getAuthToken(Account account, String str, boolean z, AccountManagerCallback<Bundle> accountManagerCallback, Handler handler) {
        return getAuthToken(account, str, (Bundle) null, z, accountManagerCallback, handler);
    }

    public AccountManagerFuture<Bundle> getAuthToken(final Account account, final String str, Bundle bundle, final boolean z, AccountManagerCallback<Bundle> accountManagerCallback, Handler handler) {
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        if (str == null) {
            throw new IllegalArgumentException("authTokenType is null");
        }
        final Bundle bundle2 = new Bundle();
        if (bundle != null) {
            bundle2.putAll(bundle);
        }
        bundle2.putString(KEY_ANDROID_PACKAGE_NAME, this.mContext.getPackageName());
        return new AmsTask(null, handler, accountManagerCallback) {
            @Override
            public void doWork() throws RemoteException {
                AccountManager.this.mService.getAuthToken(this.mResponse, account, str, z, false, bundle2);
            }
        }.start();
    }

    public AccountManagerFuture<Bundle> addAccount(final String str, final String str2, final String[] strArr, Bundle bundle, final Activity activity, AccountManagerCallback<Bundle> accountManagerCallback, Handler handler) {
        if (str == null) {
            throw new IllegalArgumentException("accountType is null");
        }
        final Bundle bundle2 = new Bundle();
        if (bundle != null) {
            bundle2.putAll(bundle);
        }
        bundle2.putString(KEY_ANDROID_PACKAGE_NAME, this.mContext.getPackageName());
        return new AmsTask(activity, handler, accountManagerCallback) {
            @Override
            public void doWork() throws RemoteException {
                AccountManager.this.mService.addAccount(this.mResponse, str, str2, strArr, activity != null, bundle2);
            }
        }.start();
    }

    public AccountManagerFuture<Bundle> addAccountAsUser(final String str, final String str2, final String[] strArr, Bundle bundle, final Activity activity, AccountManagerCallback<Bundle> accountManagerCallback, Handler handler, final UserHandle userHandle) {
        if (str == null) {
            throw new IllegalArgumentException("accountType is null");
        }
        if (userHandle == null) {
            throw new IllegalArgumentException("userHandle is null");
        }
        final Bundle bundle2 = new Bundle();
        if (bundle != null) {
            bundle2.putAll(bundle);
        }
        bundle2.putString(KEY_ANDROID_PACKAGE_NAME, this.mContext.getPackageName());
        return new AmsTask(activity, handler, accountManagerCallback) {
            @Override
            public void doWork() throws RemoteException {
                AccountManager.this.mService.addAccountAsUser(this.mResponse, str, str2, strArr, activity != null, bundle2, userHandle.getIdentifier());
            }
        }.start();
    }

    public void addSharedAccountsFromParentUser(UserHandle userHandle, UserHandle userHandle2) {
        try {
            this.mService.addSharedAccountsFromParentUser(userHandle.getIdentifier(), userHandle2.getIdentifier(), this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public AccountManagerFuture<Boolean> copyAccountToUser(final Account account, final UserHandle userHandle, final UserHandle userHandle2, AccountManagerCallback<Boolean> accountManagerCallback, Handler handler) {
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        if (userHandle2 == null || userHandle == null) {
            throw new IllegalArgumentException("fromUser and toUser cannot be null");
        }
        return new Future2Task<Boolean>(handler, accountManagerCallback) {
            @Override
            public void doWork() throws RemoteException {
                AccountManager.this.mService.copyAccountToUser(this.mResponse, account, userHandle.getIdentifier(), userHandle2.getIdentifier());
            }

            @Override
            public Boolean bundleToResult(Bundle bundle) throws AuthenticatorException {
                if (!bundle.containsKey(AccountManager.KEY_BOOLEAN_RESULT)) {
                    throw new AuthenticatorException("no result in response");
                }
                return Boolean.valueOf(bundle.getBoolean(AccountManager.KEY_BOOLEAN_RESULT));
            }
        }.start();
    }

    public boolean removeSharedAccount(Account account, UserHandle userHandle) {
        try {
            return this.mService.removeSharedAccountAsUser(account, userHandle.getIdentifier());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Account[] getSharedAccounts(UserHandle userHandle) {
        try {
            return this.mService.getSharedAccountsAsUser(userHandle.getIdentifier());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public AccountManagerFuture<Bundle> confirmCredentials(Account account, Bundle bundle, Activity activity, AccountManagerCallback<Bundle> accountManagerCallback, Handler handler) {
        return confirmCredentialsAsUser(account, bundle, activity, accountManagerCallback, handler, this.mContext.getUser());
    }

    public AccountManagerFuture<Bundle> confirmCredentialsAsUser(final Account account, final Bundle bundle, final Activity activity, AccountManagerCallback<Bundle> accountManagerCallback, Handler handler, UserHandle userHandle) {
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        final int identifier = userHandle.getIdentifier();
        return new AmsTask(activity, handler, accountManagerCallback) {
            @Override
            public void doWork() throws RemoteException {
                AccountManager.this.mService.confirmCredentialsAsUser(this.mResponse, account, bundle, activity != null, identifier);
            }
        }.start();
    }

    public AccountManagerFuture<Bundle> updateCredentials(final Account account, final String str, final Bundle bundle, final Activity activity, AccountManagerCallback<Bundle> accountManagerCallback, Handler handler) {
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        return new AmsTask(activity, handler, accountManagerCallback) {
            @Override
            public void doWork() throws RemoteException {
                AccountManager.this.mService.updateCredentials(this.mResponse, account, str, activity != null, bundle);
            }
        }.start();
    }

    public AccountManagerFuture<Bundle> editProperties(final String str, final Activity activity, AccountManagerCallback<Bundle> accountManagerCallback, Handler handler) {
        if (str == null) {
            throw new IllegalArgumentException("accountType is null");
        }
        return new AmsTask(activity, handler, accountManagerCallback) {
            @Override
            public void doWork() throws RemoteException {
                AccountManager.this.mService.editProperties(this.mResponse, str, activity != null);
            }
        }.start();
    }

    public boolean someUserHasAccount(Account account) {
        try {
            return this.mService.someUserHasAccount(account);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private void ensureNotOnMainThread() {
        Looper looperMyLooper = Looper.myLooper();
        if (looperMyLooper != null && looperMyLooper == this.mContext.getMainLooper()) {
            IllegalStateException illegalStateException = new IllegalStateException("calling this from your main thread can lead to deadlock");
            Log.e(TAG, "calling this from your main thread can lead to deadlock and/or ANRs", illegalStateException);
            if (this.mContext.getApplicationInfo().targetSdkVersion >= 8) {
                throw illegalStateException;
            }
        }
    }

    private void postToHandler(Handler handler, final AccountManagerCallback<Bundle> accountManagerCallback, final AccountManagerFuture<Bundle> accountManagerFuture) {
        if (handler == null) {
            handler = this.mMainHandler;
        }
        handler.post(new Runnable() {
            @Override
            public void run() {
                accountManagerCallback.run(accountManagerFuture);
            }
        });
    }

    private void postToHandler(Handler handler, final OnAccountsUpdateListener onAccountsUpdateListener, Account[] accountArr) {
        final Account[] accountArr2 = new Account[accountArr.length];
        System.arraycopy(accountArr, 0, accountArr2, 0, accountArr2.length);
        if (handler == null) {
            handler = this.mMainHandler;
        }
        handler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (AccountManager.this.mAccountsUpdatedListeners) {
                    try {
                        if (AccountManager.this.mAccountsUpdatedListeners.containsKey(onAccountsUpdateListener)) {
                            Set set = (Set) AccountManager.this.mAccountsUpdatedListenersTypes.get(onAccountsUpdateListener);
                            if (set != null) {
                                ArrayList arrayList = new ArrayList();
                                for (Account account : accountArr2) {
                                    if (set.contains(account.type)) {
                                        arrayList.add(account);
                                    }
                                }
                                onAccountsUpdateListener.onAccountsUpdated((Account[]) arrayList.toArray(new Account[arrayList.size()]));
                            } else {
                                onAccountsUpdateListener.onAccountsUpdated(accountArr2);
                            }
                        }
                    } catch (SQLException e) {
                        Log.e(AccountManager.TAG, "Can't update accounts", e);
                    }
                }
            }
        });
    }

    private abstract class AmsTask extends FutureTask<Bundle> implements AccountManagerFuture<Bundle> {
        final Activity mActivity;
        final AccountManagerCallback<Bundle> mCallback;
        final Handler mHandler;
        final IAccountManagerResponse mResponse;

        public abstract void doWork() throws RemoteException;

        public AmsTask(Activity activity, Handler handler, AccountManagerCallback<Bundle> accountManagerCallback) {
            super(new Callable<Bundle>() {
                @Override
                public Bundle call() throws Exception {
                    throw new IllegalStateException("this should never be called");
                }
            });
            this.mHandler = handler;
            this.mCallback = accountManagerCallback;
            this.mActivity = activity;
            this.mResponse = new Response();
        }

        public final AccountManagerFuture<Bundle> start() {
            try {
                doWork();
            } catch (RemoteException e) {
                setException(e);
            }
            return this;
        }

        @Override
        protected void set(Bundle bundle) {
            if (bundle == null) {
                Log.e(AccountManager.TAG, "the bundle must not be null", new Exception());
            }
            super.set(bundle);
        }

        private Bundle internalGetResult(Long l, TimeUnit timeUnit) throws OperationCanceledException, IOException, AuthenticatorException {
            if (!isDone()) {
                AccountManager.this.ensureNotOnMainThread();
            }
            try {
                try {
                    return l == null ? get() : get(l.longValue(), timeUnit);
                } catch (InterruptedException e) {
                    cancel(true);
                    throw new OperationCanceledException();
                } catch (CancellationException e2) {
                    throw new OperationCanceledException();
                } catch (ExecutionException e3) {
                    Throwable cause = e3.getCause();
                    if (cause instanceof IOException) {
                        throw ((IOException) cause);
                    }
                    if (cause instanceof UnsupportedOperationException) {
                        throw new AuthenticatorException(cause);
                    }
                    if (cause instanceof AuthenticatorException) {
                        throw ((AuthenticatorException) cause);
                    }
                    if (cause instanceof RuntimeException) {
                        throw ((RuntimeException) cause);
                    }
                    if (cause instanceof Error) {
                        throw ((Error) cause);
                    }
                    throw new IllegalStateException(cause);
                } catch (TimeoutException e4) {
                    cancel(true);
                    throw new OperationCanceledException();
                }
            } finally {
                cancel(true);
            }
        }

        @Override
        public Bundle getResult() throws OperationCanceledException, IOException, AuthenticatorException {
            return internalGetResult(null, null);
        }

        @Override
        public Bundle getResult(long j, TimeUnit timeUnit) throws OperationCanceledException, IOException, AuthenticatorException {
            return internalGetResult(Long.valueOf(j), timeUnit);
        }

        @Override
        protected void done() {
            if (this.mCallback != null) {
                AccountManager.this.postToHandler(this.mHandler, this.mCallback, this);
            }
        }

        private class Response extends IAccountManagerResponse.Stub {
            private Response() {
            }

            @Override
            public void onResult(Bundle bundle) {
                if (bundle == null) {
                    onError(5, "null bundle returned");
                    return;
                }
                Intent intent = (Intent) bundle.getParcelable("intent");
                if (intent != null && AmsTask.this.mActivity != null) {
                    AmsTask.this.mActivity.startActivity(intent);
                } else {
                    if (bundle.getBoolean("retry")) {
                        try {
                            AmsTask.this.doWork();
                            return;
                        } catch (RemoteException e) {
                            throw e.rethrowFromSystemServer();
                        }
                    }
                    AmsTask.this.set(bundle);
                }
            }

            @Override
            public void onError(int i, String str) {
                if (i != 4 && i != 100 && i != 101) {
                    AmsTask.this.setException(AccountManager.this.convertErrorToException(i, str));
                } else {
                    AmsTask.this.cancel(true);
                }
            }
        }
    }

    private abstract class BaseFutureTask<T> extends FutureTask<T> {
        final Handler mHandler;
        public final IAccountManagerResponse mResponse;

        public abstract T bundleToResult(Bundle bundle) throws AuthenticatorException;

        public abstract void doWork() throws RemoteException;

        public BaseFutureTask(Handler handler) {
            super(new Callable<T>() {
                @Override
                public T call() throws Exception {
                    throw new IllegalStateException("this should never be called");
                }
            });
            this.mHandler = handler;
            this.mResponse = new Response();
        }

        protected void postRunnableToHandler(Runnable runnable) {
            (this.mHandler == null ? AccountManager.this.mMainHandler : this.mHandler).post(runnable);
        }

        protected void startTask() {
            try {
                doWork();
            } catch (RemoteException e) {
                setException(e);
            }
        }

        protected class Response extends IAccountManagerResponse.Stub {
            protected Response() {
            }

            @Override
            public void onResult(Bundle bundle) {
                try {
                    Object objBundleToResult = BaseFutureTask.this.bundleToResult(bundle);
                    if (objBundleToResult != null) {
                        BaseFutureTask.this.set(objBundleToResult);
                    }
                } catch (AuthenticatorException | ClassCastException e) {
                    onError(5, "no result in response");
                }
            }

            @Override
            public void onError(int i, String str) {
                if (i != 4 && i != 100 && i != 101) {
                    BaseFutureTask.this.setException(AccountManager.this.convertErrorToException(i, str));
                } else {
                    BaseFutureTask.this.cancel(true);
                }
            }
        }
    }

    private abstract class Future2Task<T> extends BaseFutureTask<T> implements AccountManagerFuture<T> {
        final AccountManagerCallback<T> mCallback;

        public Future2Task(Handler handler, AccountManagerCallback<T> accountManagerCallback) {
            super(handler);
            this.mCallback = accountManagerCallback;
        }

        @Override
        protected void done() {
            if (this.mCallback != null) {
                postRunnableToHandler(new Runnable() {
                    @Override
                    public void run() {
                        Future2Task.this.mCallback.run(Future2Task.this);
                    }
                });
            }
        }

        public Future2Task<T> start() {
            startTask();
            return this;
        }

        private T internalGetResult(Long l, TimeUnit timeUnit) throws OperationCanceledException, IOException, AuthenticatorException {
            if (!isDone()) {
                AccountManager.this.ensureNotOnMainThread();
            }
            try {
                try {
                    return l == null ? (T) get() : (T) get(l.longValue(), timeUnit);
                } catch (InterruptedException e) {
                    cancel(true);
                    throw new OperationCanceledException();
                } catch (CancellationException e2) {
                    cancel(true);
                    throw new OperationCanceledException();
                } catch (ExecutionException e3) {
                    Throwable cause = e3.getCause();
                    if (cause instanceof IOException) {
                        throw ((IOException) cause);
                    }
                    if (cause instanceof UnsupportedOperationException) {
                        throw new AuthenticatorException(cause);
                    }
                    if (cause instanceof AuthenticatorException) {
                        throw ((AuthenticatorException) cause);
                    }
                    if (cause instanceof RuntimeException) {
                        throw ((RuntimeException) cause);
                    }
                    if (cause instanceof Error) {
                        throw ((Error) cause);
                    }
                    throw new IllegalStateException(cause);
                } catch (TimeoutException e4) {
                    cancel(true);
                    throw new OperationCanceledException();
                }
            } finally {
                cancel(true);
            }
        }

        @Override
        public T getResult() throws OperationCanceledException, IOException, AuthenticatorException {
            return internalGetResult(null, null);
        }

        @Override
        public T getResult(long j, TimeUnit timeUnit) throws OperationCanceledException, IOException, AuthenticatorException {
            return internalGetResult(Long.valueOf(j), timeUnit);
        }
    }

    private Exception convertErrorToException(int i, String str) {
        if (i == 3) {
            return new IOException(str);
        }
        if (i == 6) {
            return new UnsupportedOperationException(str);
        }
        if (i == 5) {
            return new AuthenticatorException(str);
        }
        if (i == 7) {
            return new IllegalArgumentException(str);
        }
        return new AuthenticatorException(str);
    }

    private void getAccountByTypeAndFeatures(final String str, final String[] strArr, AccountManagerCallback<Bundle> accountManagerCallback, Handler handler) {
        new AmsTask(null, handler, accountManagerCallback) {
            @Override
            public void doWork() throws RemoteException {
                AccountManager.this.mService.getAccountByTypeAndFeatures(this.mResponse, str, strArr, AccountManager.this.mContext.getOpPackageName());
            }
        }.start();
    }

    private class GetAuthTokenByTypeAndFeaturesTask extends AmsTask implements AccountManagerCallback<Bundle> {
        final String mAccountType;
        final Bundle mAddAccountOptions;
        final String mAuthTokenType;
        final String[] mFeatures;
        volatile AccountManagerFuture<Bundle> mFuture;
        final Bundle mLoginOptions;
        final AccountManagerCallback<Bundle> mMyCallback;
        private volatile int mNumAccounts;

        GetAuthTokenByTypeAndFeaturesTask(String str, String str2, String[] strArr, Activity activity, Bundle bundle, Bundle bundle2, AccountManagerCallback<Bundle> accountManagerCallback, Handler handler) {
            super(activity, handler, accountManagerCallback);
            this.mFuture = null;
            this.mNumAccounts = 0;
            if (str == null) {
                throw new IllegalArgumentException("account type is null");
            }
            this.mAccountType = str;
            this.mAuthTokenType = str2;
            this.mFeatures = strArr;
            this.mAddAccountOptions = bundle;
            this.mLoginOptions = bundle2;
            this.mMyCallback = this;
        }

        @Override
        public void doWork() throws RemoteException {
            AccountManager.this.getAccountByTypeAndFeatures(this.mAccountType, this.mFeatures, new AccountManagerCallback<Bundle>() {
                @Override
                public void run(AccountManagerFuture<Bundle> accountManagerFuture) {
                    try {
                        Bundle result = accountManagerFuture.getResult();
                        String string = result.getString(AccountManager.KEY_ACCOUNT_NAME);
                        String string2 = result.getString("accountType");
                        if (string != null) {
                            GetAuthTokenByTypeAndFeaturesTask.this.mNumAccounts = 1;
                            Account account = new Account(string, string2);
                            if (GetAuthTokenByTypeAndFeaturesTask.this.mActivity == null) {
                                GetAuthTokenByTypeAndFeaturesTask.this.mFuture = AccountManager.this.getAuthToken(account, GetAuthTokenByTypeAndFeaturesTask.this.mAuthTokenType, false, GetAuthTokenByTypeAndFeaturesTask.this.mMyCallback, GetAuthTokenByTypeAndFeaturesTask.this.mHandler);
                                return;
                            } else {
                                GetAuthTokenByTypeAndFeaturesTask.this.mFuture = AccountManager.this.getAuthToken(account, GetAuthTokenByTypeAndFeaturesTask.this.mAuthTokenType, GetAuthTokenByTypeAndFeaturesTask.this.mLoginOptions, GetAuthTokenByTypeAndFeaturesTask.this.mActivity, GetAuthTokenByTypeAndFeaturesTask.this.mMyCallback, GetAuthTokenByTypeAndFeaturesTask.this.mHandler);
                                return;
                            }
                        }
                        if (GetAuthTokenByTypeAndFeaturesTask.this.mActivity != null) {
                            GetAuthTokenByTypeAndFeaturesTask.this.mFuture = AccountManager.this.addAccount(GetAuthTokenByTypeAndFeaturesTask.this.mAccountType, GetAuthTokenByTypeAndFeaturesTask.this.mAuthTokenType, GetAuthTokenByTypeAndFeaturesTask.this.mFeatures, GetAuthTokenByTypeAndFeaturesTask.this.mAddAccountOptions, GetAuthTokenByTypeAndFeaturesTask.this.mActivity, GetAuthTokenByTypeAndFeaturesTask.this.mMyCallback, GetAuthTokenByTypeAndFeaturesTask.this.mHandler);
                            return;
                        }
                        Bundle bundle = new Bundle();
                        bundle.putString(AccountManager.KEY_ACCOUNT_NAME, null);
                        bundle.putString("accountType", null);
                        bundle.putString(AccountManager.KEY_AUTHTOKEN, null);
                        bundle.putBinder(AccountManager.KEY_ACCOUNT_ACCESS_ID, null);
                        try {
                            GetAuthTokenByTypeAndFeaturesTask.this.mResponse.onResult(bundle);
                        } catch (RemoteException e) {
                        }
                    } catch (AuthenticatorException e2) {
                        GetAuthTokenByTypeAndFeaturesTask.this.setException(e2);
                    } catch (OperationCanceledException e3) {
                        GetAuthTokenByTypeAndFeaturesTask.this.setException(e3);
                    } catch (IOException e4) {
                        GetAuthTokenByTypeAndFeaturesTask.this.setException(e4);
                    }
                }
            }, this.mHandler);
        }

        @Override
        public void run(AccountManagerFuture<Bundle> accountManagerFuture) {
            try {
                Bundle result = accountManagerFuture.getResult();
                if (this.mNumAccounts == 0) {
                    String string = result.getString(AccountManager.KEY_ACCOUNT_NAME);
                    String string2 = result.getString("accountType");
                    if (!TextUtils.isEmpty(string) && !TextUtils.isEmpty(string2)) {
                        Account account = new Account(string, string2, result.getString(AccountManager.KEY_ACCOUNT_ACCESS_ID));
                        this.mNumAccounts = 1;
                        AccountManager.this.getAuthToken(account, this.mAuthTokenType, (Bundle) null, this.mActivity, this.mMyCallback, this.mHandler);
                        return;
                    }
                    setException(new AuthenticatorException("account not in result"));
                    return;
                }
                set(result);
            } catch (AuthenticatorException e) {
                setException(e);
            } catch (OperationCanceledException e2) {
                cancel(true);
            } catch (IOException e3) {
                setException(e3);
            }
        }
    }

    public AccountManagerFuture<Bundle> getAuthTokenByFeatures(String str, String str2, String[] strArr, Activity activity, Bundle bundle, Bundle bundle2, AccountManagerCallback<Bundle> accountManagerCallback, Handler handler) {
        if (str == null) {
            throw new IllegalArgumentException("account type is null");
        }
        if (str2 == null) {
            throw new IllegalArgumentException("authTokenType is null");
        }
        GetAuthTokenByTypeAndFeaturesTask getAuthTokenByTypeAndFeaturesTask = new GetAuthTokenByTypeAndFeaturesTask(str, str2, strArr, activity, bundle, bundle2, accountManagerCallback, handler);
        getAuthTokenByTypeAndFeaturesTask.start();
        return getAuthTokenByTypeAndFeaturesTask;
    }

    @Deprecated
    public static Intent newChooseAccountIntent(Account account, ArrayList<Account> arrayList, String[] strArr, boolean z, String str, String str2, String[] strArr2, Bundle bundle) {
        return newChooseAccountIntent(account, arrayList, strArr, str, str2, strArr2, bundle);
    }

    public static Intent newChooseAccountIntent(Account account, List<Account> list, String[] strArr, String str, String str2, String[] strArr2, Bundle bundle) {
        Intent intent = new Intent();
        ComponentName componentNameUnflattenFromString = ComponentName.unflattenFromString(Resources.getSystem().getString(R.string.config_chooseTypeAndAccountActivity));
        intent.setClassName(componentNameUnflattenFromString.getPackageName(), componentNameUnflattenFromString.getClassName());
        intent.putExtra(ChooseTypeAndAccountActivity.EXTRA_ALLOWABLE_ACCOUNTS_ARRAYLIST, list == null ? null : new ArrayList(list));
        intent.putExtra(ChooseTypeAndAccountActivity.EXTRA_ALLOWABLE_ACCOUNT_TYPES_STRING_ARRAY, strArr);
        intent.putExtra(ChooseTypeAndAccountActivity.EXTRA_ADD_ACCOUNT_OPTIONS_BUNDLE, bundle);
        intent.putExtra(ChooseTypeAndAccountActivity.EXTRA_SELECTED_ACCOUNT, account);
        intent.putExtra(ChooseTypeAndAccountActivity.EXTRA_DESCRIPTION_TEXT_OVERRIDE, str);
        intent.putExtra("authTokenType", str2);
        intent.putExtra(ChooseTypeAndAccountActivity.EXTRA_ADD_ACCOUNT_REQUIRED_FEATURES_STRING_ARRAY, strArr2);
        return intent;
    }

    public void addOnAccountsUpdatedListener(OnAccountsUpdateListener onAccountsUpdateListener, Handler handler, boolean z) {
        addOnAccountsUpdatedListener(onAccountsUpdateListener, handler, z, null);
    }

    public void addOnAccountsUpdatedListener(OnAccountsUpdateListener onAccountsUpdateListener, Handler handler, boolean z, String[] strArr) {
        if (onAccountsUpdateListener == null) {
            throw new IllegalArgumentException("the listener is null");
        }
        synchronized (this.mAccountsUpdatedListeners) {
            if (this.mAccountsUpdatedListeners.containsKey(onAccountsUpdateListener)) {
                throw new IllegalStateException("this listener is already added");
            }
            boolean zIsEmpty = this.mAccountsUpdatedListeners.isEmpty();
            this.mAccountsUpdatedListeners.put(onAccountsUpdateListener, handler);
            if (strArr != null) {
                this.mAccountsUpdatedListenersTypes.put(onAccountsUpdateListener, new HashSet(Arrays.asList(strArr)));
            } else {
                this.mAccountsUpdatedListenersTypes.put(onAccountsUpdateListener, null);
            }
            if (zIsEmpty) {
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(ACTION_VISIBLE_ACCOUNTS_CHANGED);
                intentFilter.addAction(Intent.ACTION_DEVICE_STORAGE_OK);
                this.mContext.registerReceiver(this.mAccountsChangedBroadcastReceiver, intentFilter);
            }
            try {
                this.mService.registerAccountListener(strArr, this.mContext.getOpPackageName());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        if (z) {
            postToHandler(handler, onAccountsUpdateListener, getAccounts());
        }
    }

    public void removeOnAccountsUpdatedListener(OnAccountsUpdateListener onAccountsUpdateListener) {
        String[] strArr;
        if (onAccountsUpdateListener == null) {
            throw new IllegalArgumentException("listener is null");
        }
        synchronized (this.mAccountsUpdatedListeners) {
            if (!this.mAccountsUpdatedListeners.containsKey(onAccountsUpdateListener)) {
                Log.e(TAG, "Listener was not previously added");
                return;
            }
            Set<String> set = this.mAccountsUpdatedListenersTypes.get(onAccountsUpdateListener);
            if (set != null) {
                strArr = (String[]) set.toArray(new String[set.size()]);
            } else {
                strArr = null;
            }
            this.mAccountsUpdatedListeners.remove(onAccountsUpdateListener);
            this.mAccountsUpdatedListenersTypes.remove(onAccountsUpdateListener);
            if (this.mAccountsUpdatedListeners.isEmpty()) {
                this.mContext.unregisterReceiver(this.mAccountsChangedBroadcastReceiver);
            }
            try {
                this.mService.unregisterAccountListener(strArr, this.mContext.getOpPackageName());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public AccountManagerFuture<Bundle> startAddAccountSession(final String str, final String str2, final String[] strArr, Bundle bundle, final Activity activity, AccountManagerCallback<Bundle> accountManagerCallback, Handler handler) {
        if (str == null) {
            throw new IllegalArgumentException("accountType is null");
        }
        final Bundle bundle2 = new Bundle();
        if (bundle != null) {
            bundle2.putAll(bundle);
        }
        bundle2.putString(KEY_ANDROID_PACKAGE_NAME, this.mContext.getPackageName());
        return new AmsTask(activity, handler, accountManagerCallback) {
            @Override
            public void doWork() throws RemoteException {
                AccountManager.this.mService.startAddAccountSession(this.mResponse, str, str2, strArr, activity != null, bundle2);
            }
        }.start();
    }

    public AccountManagerFuture<Bundle> startUpdateCredentialsSession(final Account account, final String str, Bundle bundle, final Activity activity, AccountManagerCallback<Bundle> accountManagerCallback, Handler handler) {
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        final Bundle bundle2 = new Bundle();
        if (bundle != null) {
            bundle2.putAll(bundle);
        }
        bundle2.putString(KEY_ANDROID_PACKAGE_NAME, this.mContext.getPackageName());
        return new AmsTask(activity, handler, accountManagerCallback) {
            @Override
            public void doWork() throws RemoteException {
                AccountManager.this.mService.startUpdateCredentialsSession(this.mResponse, account, str, activity != null, bundle2);
            }
        }.start();
    }

    public AccountManagerFuture<Bundle> finishSession(Bundle bundle, Activity activity, AccountManagerCallback<Bundle> accountManagerCallback, Handler handler) {
        return finishSessionAsUser(bundle, activity, this.mContext.getUser(), accountManagerCallback, handler);
    }

    @SystemApi
    public AccountManagerFuture<Bundle> finishSessionAsUser(final Bundle bundle, final Activity activity, final UserHandle userHandle, AccountManagerCallback<Bundle> accountManagerCallback, Handler handler) {
        if (bundle == null) {
            throw new IllegalArgumentException("sessionBundle is null");
        }
        final Bundle bundle2 = new Bundle();
        bundle2.putString(KEY_ANDROID_PACKAGE_NAME, this.mContext.getPackageName());
        return new AmsTask(activity, handler, accountManagerCallback) {
            @Override
            public void doWork() throws RemoteException {
                AccountManager.this.mService.finishSessionAsUser(this.mResponse, bundle, activity != null, bundle2, userHandle.getIdentifier());
            }
        }.start();
    }

    public AccountManagerFuture<Boolean> isCredentialsUpdateSuggested(final Account account, final String str, AccountManagerCallback<Boolean> accountManagerCallback, Handler handler) {
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        if (TextUtils.isEmpty(str)) {
            throw new IllegalArgumentException("status token is empty");
        }
        return new Future2Task<Boolean>(handler, accountManagerCallback) {
            @Override
            public void doWork() throws RemoteException {
                AccountManager.this.mService.isCredentialsUpdateSuggested(this.mResponse, account, str);
            }

            @Override
            public Boolean bundleToResult(Bundle bundle) throws AuthenticatorException {
                if (!bundle.containsKey(AccountManager.KEY_BOOLEAN_RESULT)) {
                    throw new AuthenticatorException("no result in response");
                }
                return Boolean.valueOf(bundle.getBoolean(AccountManager.KEY_BOOLEAN_RESULT));
            }
        }.start();
    }

    public boolean hasAccountAccess(Account account, String str, UserHandle userHandle) {
        try {
            return this.mService.hasAccountAccess(account, str, userHandle);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public IntentSender createRequestAccountAccessIntentSenderAsUser(Account account, String str, UserHandle userHandle) {
        try {
            return this.mService.createRequestAccountAccessIntentSenderAsUser(account, str, userHandle);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
}
