package android.accounts;

import android.accounts.IAccountManagerResponse;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.SQLException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.os.Process;
import android.os.RemoteException;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.R;
import com.google.android.collect.Maps;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class AccountManager {
    public static final String ACTION_AUTHENTICATOR_INTENT = "android.accounts.AccountAuthenticator";
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
    public static final String KEY_ACCOUNT_AUTHENTICATOR_RESPONSE = "accountAuthenticatorResponse";
    public static final String KEY_ACCOUNT_MANAGER_RESPONSE = "accountManagerResponse";
    public static final String KEY_ACCOUNT_NAME = "authAccount";
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
    public static final String KEY_NOTIFY_ON_FAILURE = "notifyOnAuthFailure";
    public static final String KEY_PASSWORD = "password";
    public static final String KEY_USERDATA = "userdata";
    public static final String LOGIN_ACCOUNTS_CHANGED_ACTION = "android.accounts.LOGIN_ACCOUNTS_CHANGED";
    private static final String TAG = "AccountManager";
    private final Context mContext;
    private final Handler mMainHandler;
    private final IAccountManager mService;
    private final HashMap<OnAccountsUpdateListener, Handler> mAccountsUpdatedListeners = Maps.newHashMap();
    private final BroadcastReceiver mAccountsChangedBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Account[] accounts = AccountManager.this.getAccounts();
            synchronized (AccountManager.this.mAccountsUpdatedListeners) {
                for (Map.Entry<OnAccountsUpdateListener, Handler> entry : AccountManager.this.mAccountsUpdatedListeners.entrySet()) {
                    AccountManager.this.postToHandler(entry.getValue(), entry.getKey(), accounts);
                }
            }
        }
    };

    public AccountManager(Context context, IAccountManager service) {
        this.mContext = context;
        this.mService = service;
        this.mMainHandler = new Handler(this.mContext.getMainLooper());
    }

    public AccountManager(Context context, IAccountManager service, Handler handler) {
        this.mContext = context;
        this.mService = service;
        this.mMainHandler = handler;
    }

    public static Bundle sanitizeResult(Bundle result) {
        if (result == null || !result.containsKey(KEY_AUTHTOKEN) || TextUtils.isEmpty(result.getString(KEY_AUTHTOKEN))) {
            return result;
        }
        Bundle newResult = new Bundle(result);
        newResult.putString(KEY_AUTHTOKEN, "<omitted for logging purposes>");
        return newResult;
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
            throw new RuntimeException(e);
        }
    }

    public String getUserData(Account account, String key) {
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        if (key == null) {
            throw new IllegalArgumentException("key is null");
        }
        try {
            return this.mService.getUserData(account, key);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public AuthenticatorDescription[] getAuthenticatorTypes() {
        try {
            return this.mService.getAuthenticatorTypes(UserHandle.getCallingUserId());
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public AuthenticatorDescription[] getAuthenticatorTypesAsUser(int userId) {
        try {
            return this.mService.getAuthenticatorTypes(userId);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public Account[] getAccounts() {
        try {
            return this.mService.getAccounts(null);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public Account[] getAccountsAsUser(int userId) {
        try {
            return this.mService.getAccountsAsUser(null, userId);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public Account[] getAccountsForPackage(String packageName, int uid) {
        try {
            return this.mService.getAccountsForPackage(packageName, uid);
        } catch (RemoteException re) {
            throw new RuntimeException(re);
        }
    }

    public Account[] getAccountsByTypeForPackage(String type, String packageName) {
        try {
            return this.mService.getAccountsByTypeForPackage(type, packageName);
        } catch (RemoteException re) {
            throw new RuntimeException(re);
        }
    }

    public Account[] getAccountsByType(String type) {
        return getAccountsByTypeAsUser(type, Process.myUserHandle());
    }

    public Account[] getAccountsByTypeAsUser(String type, UserHandle userHandle) {
        try {
            return this.mService.getAccountsAsUser(type, userHandle.getIdentifier());
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateAppPermission(Account account, String authTokenType, int uid, boolean value) {
        try {
            this.mService.updateAppPermission(account, authTokenType, uid, value);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public AccountManagerFuture<String> getAuthTokenLabel(final String accountType, final String authTokenType, AccountManagerCallback<String> callback, Handler handler) {
        if (accountType == null) {
            throw new IllegalArgumentException("accountType is null");
        }
        if (authTokenType == null) {
            throw new IllegalArgumentException("authTokenType is null");
        }
        return new Future2Task<String>(handler, callback) {
            @Override
            public void doWork() throws RemoteException {
                AccountManager.this.mService.getAuthTokenLabel(this.mResponse, accountType, authTokenType);
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

    public AccountManagerFuture<Boolean> hasFeatures(final Account account, final String[] features, AccountManagerCallback<Boolean> callback, Handler handler) {
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        if (features == null) {
            throw new IllegalArgumentException("features is null");
        }
        return new Future2Task<Boolean>(handler, callback) {
            @Override
            public void doWork() throws RemoteException {
                AccountManager.this.mService.hasFeatures(this.mResponse, account, features);
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

    public AccountManagerFuture<Account[]> getAccountsByTypeAndFeatures(final String type, final String[] features, AccountManagerCallback<Account[]> callback, Handler handler) {
        if (type == null) {
            throw new IllegalArgumentException("type is null");
        }
        return new Future2Task<Account[]>(handler, callback) {
            @Override
            public void doWork() throws RemoteException {
                AccountManager.this.mService.getAccountsByFeatures(this.mResponse, type, features);
            }

            @Override
            public Account[] bundleToResult(Bundle bundle) throws AuthenticatorException {
                if (!bundle.containsKey(AccountManager.KEY_ACCOUNTS)) {
                    throw new AuthenticatorException("no result in response");
                }
                Parcelable[] parcelables = bundle.getParcelableArray(AccountManager.KEY_ACCOUNTS);
                Account[] descs = new Account[parcelables.length];
                for (int i = 0; i < parcelables.length; i++) {
                    descs[i] = (Account) parcelables[i];
                }
                return descs;
            }
        }.start();
    }

    public boolean addAccountExplicitly(Account account, String password, Bundle userdata) {
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        try {
            return this.mService.addAccountExplicitly(account, password, userdata);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public AccountManagerFuture<Account> renameAccount(final Account account, final String newName, AccountManagerCallback<Account> callback, Handler handler) {
        if (account == null) {
            throw new IllegalArgumentException("account is null.");
        }
        if (TextUtils.isEmpty(newName)) {
            throw new IllegalArgumentException("newName is empty or null.");
        }
        return new Future2Task<Account>(handler, callback) {
            @Override
            public void doWork() throws RemoteException {
                AccountManager.this.mService.renameAccount(this.mResponse, account, newName);
            }

            @Override
            public Account bundleToResult(Bundle bundle) throws AuthenticatorException {
                String name = bundle.getString(AccountManager.KEY_ACCOUNT_NAME);
                String type = bundle.getString("accountType");
                return new Account(name, type);
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
            throw new RuntimeException(e);
        }
    }

    @Deprecated
    public AccountManagerFuture<Boolean> removeAccount(final Account account, AccountManagerCallback<Boolean> callback, Handler handler) {
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        return new Future2Task<Boolean>(handler, callback) {
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

    public AccountManagerFuture<Bundle> removeAccount(final Account account, final Activity activity, AccountManagerCallback<Bundle> callback, Handler handler) {
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        return new AmsTask(activity, handler, callback) {
            @Override
            public void doWork() throws RemoteException {
                AccountManager.this.mService.removeAccount(this.mResponse, account, activity != null);
            }
        }.start();
    }

    @Deprecated
    public AccountManagerFuture<Boolean> removeAccountAsUser(final Account account, AccountManagerCallback<Boolean> callback, Handler handler, final UserHandle userHandle) {
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        if (userHandle == null) {
            throw new IllegalArgumentException("userHandle is null");
        }
        return new Future2Task<Boolean>(handler, callback) {
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

    public AccountManagerFuture<Bundle> removeAccountAsUser(final Account account, final Activity activity, AccountManagerCallback<Bundle> callback, Handler handler, final UserHandle userHandle) {
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        if (userHandle == null) {
            throw new IllegalArgumentException("userHandle is null");
        }
        return new AmsTask(activity, handler, callback) {
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
            throw new RuntimeException(e);
        }
    }

    public void invalidateAuthToken(String accountType, String authToken) {
        if (accountType == null) {
            throw new IllegalArgumentException("accountType is null");
        }
        if (authToken != null) {
            try {
                this.mService.invalidateAuthToken(accountType, authToken);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public String peekAuthToken(Account account, String authTokenType) {
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        if (authTokenType == null) {
            throw new IllegalArgumentException("authTokenType is null");
        }
        try {
            return this.mService.peekAuthToken(account, authTokenType);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void setPassword(Account account, String password) {
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        try {
            this.mService.setPassword(account, password);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void clearPassword(Account account) {
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        try {
            this.mService.clearPassword(account);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void setUserData(Account account, String key, String value) {
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        if (key == null) {
            throw new IllegalArgumentException("key is null");
        }
        try {
            this.mService.setUserData(account, key, value);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void setAuthToken(Account account, String authTokenType, String authToken) {
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        if (authTokenType == null) {
            throw new IllegalArgumentException("authTokenType is null");
        }
        try {
            this.mService.setAuthToken(account, authTokenType, authToken);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public String blockingGetAuthToken(Account account, String authTokenType, boolean notifyAuthFailure) throws OperationCanceledException, IOException, AuthenticatorException {
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        if (authTokenType == null) {
            throw new IllegalArgumentException("authTokenType is null");
        }
        Bundle bundle = getAuthToken(account, authTokenType, notifyAuthFailure, null, null).getResult();
        if (bundle != null) {
            return bundle.getString(KEY_AUTHTOKEN);
        }
        Log.e(TAG, "blockingGetAuthToken: null was returned from getResult() for " + account + ", authTokenType " + authTokenType);
        return null;
    }

    public AccountManagerFuture<Bundle> getAuthToken(final Account account, final String authTokenType, Bundle options, Activity activity, AccountManagerCallback<Bundle> callback, Handler handler) {
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        if (authTokenType == null) {
            throw new IllegalArgumentException("authTokenType is null");
        }
        final Bundle optionsIn = new Bundle();
        if (options != null) {
            optionsIn.putAll(options);
        }
        optionsIn.putString(KEY_ANDROID_PACKAGE_NAME, this.mContext.getPackageName());
        return new AmsTask(activity, handler, callback) {
            @Override
            public void doWork() throws RemoteException {
                AccountManager.this.mService.getAuthToken(this.mResponse, account, authTokenType, false, true, optionsIn);
            }
        }.start();
    }

    @Deprecated
    public AccountManagerFuture<Bundle> getAuthToken(Account account, String authTokenType, boolean notifyAuthFailure, AccountManagerCallback<Bundle> callback, Handler handler) {
        return getAuthToken(account, authTokenType, (Bundle) null, notifyAuthFailure, callback, handler);
    }

    public AccountManagerFuture<Bundle> getAuthToken(final Account account, final String authTokenType, Bundle options, final boolean notifyAuthFailure, AccountManagerCallback<Bundle> callback, Handler handler) {
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        if (authTokenType == null) {
            throw new IllegalArgumentException("authTokenType is null");
        }
        final Bundle optionsIn = new Bundle();
        if (options != null) {
            optionsIn.putAll(options);
        }
        optionsIn.putString(KEY_ANDROID_PACKAGE_NAME, this.mContext.getPackageName());
        return new AmsTask(null, handler, callback) {
            @Override
            public void doWork() throws RemoteException {
                AccountManager.this.mService.getAuthToken(this.mResponse, account, authTokenType, notifyAuthFailure, false, optionsIn);
            }
        }.start();
    }

    public AccountManagerFuture<Bundle> addAccount(final String accountType, final String authTokenType, final String[] requiredFeatures, Bundle addAccountOptions, final Activity activity, AccountManagerCallback<Bundle> callback, Handler handler) {
        if (accountType == null) {
            throw new IllegalArgumentException("accountType is null");
        }
        final Bundle optionsIn = new Bundle();
        if (addAccountOptions != null) {
            optionsIn.putAll(addAccountOptions);
        }
        optionsIn.putString(KEY_ANDROID_PACKAGE_NAME, this.mContext.getPackageName());
        return new AmsTask(activity, handler, callback) {
            @Override
            public void doWork() throws RemoteException {
                AccountManager.this.mService.addAccount(this.mResponse, accountType, authTokenType, requiredFeatures, activity != null, optionsIn);
            }
        }.start();
    }

    public AccountManagerFuture<Bundle> addAccountAsUser(final String accountType, final String authTokenType, final String[] requiredFeatures, Bundle addAccountOptions, final Activity activity, AccountManagerCallback<Bundle> callback, Handler handler, final UserHandle userHandle) {
        if (accountType == null) {
            throw new IllegalArgumentException("accountType is null");
        }
        if (userHandle == null) {
            throw new IllegalArgumentException("userHandle is null");
        }
        final Bundle optionsIn = new Bundle();
        if (addAccountOptions != null) {
            optionsIn.putAll(addAccountOptions);
        }
        optionsIn.putString(KEY_ANDROID_PACKAGE_NAME, this.mContext.getPackageName());
        return new AmsTask(activity, handler, callback) {
            @Override
            public void doWork() throws RemoteException {
                AccountManager.this.mService.addAccountAsUser(this.mResponse, accountType, authTokenType, requiredFeatures, activity != null, optionsIn, userHandle.getIdentifier());
            }
        }.start();
    }

    public boolean addSharedAccount(Account account, UserHandle user) {
        try {
            boolean val = this.mService.addSharedAccountAsUser(account, user.getIdentifier());
            return val;
        } catch (RemoteException re) {
            throw new RuntimeException(re);
        }
    }

    public AccountManagerFuture<Boolean> copyAccountToUser(final Account account, final UserHandle user, AccountManagerCallback<Boolean> callback, Handler handler) {
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        if (user == null) {
            throw new IllegalArgumentException("user is null");
        }
        return new Future2Task<Boolean>(handler, callback) {
            @Override
            public void doWork() throws RemoteException {
                AccountManager.this.mService.copyAccountToUser(this.mResponse, account, 0, user.getIdentifier());
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

    public boolean removeSharedAccount(Account account, UserHandle user) {
        try {
            boolean val = this.mService.removeSharedAccountAsUser(account, user.getIdentifier());
            return val;
        } catch (RemoteException re) {
            throw new RuntimeException(re);
        }
    }

    public Account[] getSharedAccounts(UserHandle user) {
        try {
            return this.mService.getSharedAccountsAsUser(user.getIdentifier());
        } catch (RemoteException re) {
            throw new RuntimeException(re);
        }
    }

    public AccountManagerFuture<Bundle> confirmCredentials(Account account, Bundle options, Activity activity, AccountManagerCallback<Bundle> callback, Handler handler) {
        return confirmCredentialsAsUser(account, options, activity, callback, handler, Process.myUserHandle());
    }

    public AccountManagerFuture<Bundle> confirmCredentialsAsUser(final Account account, final Bundle options, final Activity activity, AccountManagerCallback<Bundle> callback, Handler handler, UserHandle userHandle) {
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        final int userId = userHandle.getIdentifier();
        return new AmsTask(activity, handler, callback) {
            @Override
            public void doWork() throws RemoteException {
                AccountManager.this.mService.confirmCredentialsAsUser(this.mResponse, account, options, activity != null, userId);
            }
        }.start();
    }

    public AccountManagerFuture<Bundle> updateCredentials(final Account account, final String authTokenType, final Bundle options, final Activity activity, AccountManagerCallback<Bundle> callback, Handler handler) {
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        return new AmsTask(activity, handler, callback) {
            @Override
            public void doWork() throws RemoteException {
                AccountManager.this.mService.updateCredentials(this.mResponse, account, authTokenType, activity != null, options);
            }
        }.start();
    }

    public AccountManagerFuture<Bundle> editProperties(final String accountType, final Activity activity, AccountManagerCallback<Bundle> callback, Handler handler) {
        if (accountType == null) {
            throw new IllegalArgumentException("accountType is null");
        }
        return new AmsTask(activity, handler, callback) {
            @Override
            public void doWork() throws RemoteException {
                AccountManager.this.mService.editProperties(this.mResponse, accountType, activity != null);
            }
        }.start();
    }

    private void ensureNotOnMainThread() {
        Looper looper = Looper.myLooper();
        if (looper != null && looper == this.mContext.getMainLooper()) {
            IllegalStateException exception = new IllegalStateException("calling this from your main thread can lead to deadlock");
            Log.e(TAG, "calling this from your main thread can lead to deadlock and/or ANRs", exception);
            if (this.mContext.getApplicationInfo().targetSdkVersion >= 8) {
                throw exception;
            }
        }
    }

    private void postToHandler(Handler handler, final AccountManagerCallback<Bundle> callback, final AccountManagerFuture<Bundle> future) {
        if (handler == null) {
            handler = this.mMainHandler;
        }
        handler.post(new Runnable() {
            @Override
            public void run() {
                callback.run(future);
            }
        });
    }

    private void postToHandler(Handler handler, final OnAccountsUpdateListener listener, Account[] accounts) {
        final Account[] accountsCopy = new Account[accounts.length];
        System.arraycopy(accounts, 0, accountsCopy, 0, accountsCopy.length);
        if (handler == null) {
            handler = this.mMainHandler;
        }
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    listener.onAccountsUpdated(accountsCopy);
                } catch (SQLException e) {
                    Log.e(AccountManager.TAG, "Can't update accounts", e);
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

        public AmsTask(Activity activity, Handler handler, AccountManagerCallback<Bundle> callback) {
            super(new Callable<Bundle>() {
                @Override
                public Bundle call() throws Exception {
                    throw new IllegalStateException("this should never be called");
                }
            });
            this.mHandler = handler;
            this.mCallback = callback;
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

        private Bundle internalGetResult(Long timeout, TimeUnit unit) throws OperationCanceledException, IOException, AuthenticatorException {
            Bundle bundle;
            if (!isDone()) {
                AccountManager.this.ensureNotOnMainThread();
            }
            try {
                try {
                    if (timeout == null) {
                        bundle = get();
                        cancel(true);
                    } else {
                        bundle = get(timeout.longValue(), unit);
                        cancel(true);
                    }
                    return bundle;
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
            } catch (Throwable th) {
                cancel(true);
                throw th;
            }
        }

        @Override
        public Bundle getResult() throws OperationCanceledException, IOException, AuthenticatorException {
            return internalGetResult(null, null);
        }

        @Override
        public Bundle getResult(long timeout, TimeUnit unit) throws OperationCanceledException, IOException, AuthenticatorException {
            return internalGetResult(Long.valueOf(timeout), unit);
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
                Intent intent = (Intent) bundle.getParcelable("intent");
                if (intent != null && AmsTask.this.mActivity != null) {
                    AmsTask.this.mActivity.startActivity(intent);
                } else if (bundle.getBoolean("retry")) {
                    try {
                        AmsTask.this.doWork();
                    } catch (RemoteException e) {
                    }
                } else {
                    AmsTask.this.set(bundle);
                }
            }

            @Override
            public void onError(int code, String message) {
                if (code != 4 && code != 100 && code != 101) {
                    AmsTask.this.setException(AccountManager.this.convertErrorToException(code, message));
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
            Handler handler;
            if (this.mHandler == null) {
                handler = AccountManager.this.mMainHandler;
            } else {
                handler = this.mHandler;
            }
            handler.post(runnable);
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
            public void onError(int code, String message) {
                if (code != 4 && code != 100 && code != 101) {
                    BaseFutureTask.this.setException(AccountManager.this.convertErrorToException(code, message));
                } else {
                    BaseFutureTask.this.cancel(true);
                }
            }
        }
    }

    private abstract class Future2Task<T> extends BaseFutureTask<T> implements AccountManagerFuture<T> {
        final AccountManagerCallback<T> mCallback;

        public Future2Task(Handler handler, AccountManagerCallback<T> callback) {
            super(handler);
            this.mCallback = callback;
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
            T t;
            if (!isDone()) {
                AccountManager.this.ensureNotOnMainThread();
            }
            try {
                try {
                    if (l == null) {
                        t = (T) get();
                        cancel(true);
                    } else {
                        t = (T) get(l.longValue(), timeUnit);
                        cancel(true);
                    }
                    return t;
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
            } catch (Throwable th) {
                cancel(true);
                throw th;
            }
        }

        @Override
        public T getResult() throws OperationCanceledException, IOException, AuthenticatorException {
            return internalGetResult(null, null);
        }

        @Override
        public T getResult(long timeout, TimeUnit unit) throws OperationCanceledException, IOException, AuthenticatorException {
            return internalGetResult(Long.valueOf(timeout), unit);
        }
    }

    private Exception convertErrorToException(int code, String message) {
        if (code == 3) {
            return new IOException(message);
        }
        if (code == 6) {
            return new UnsupportedOperationException(message);
        }
        if (code == 5) {
            return new AuthenticatorException(message);
        }
        if (code == 7) {
            return new IllegalArgumentException(message);
        }
        return new AuthenticatorException(message);
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

        GetAuthTokenByTypeAndFeaturesTask(String accountType, String authTokenType, String[] features, Activity activityForPrompting, Bundle addAccountOptions, Bundle loginOptions, AccountManagerCallback<Bundle> callback, Handler handler) {
            super(activityForPrompting, handler, callback);
            this.mFuture = null;
            this.mNumAccounts = 0;
            if (accountType == null) {
                throw new IllegalArgumentException("account type is null");
            }
            this.mAccountType = accountType;
            this.mAuthTokenType = authTokenType;
            this.mFeatures = features;
            this.mAddAccountOptions = addAccountOptions;
            this.mLoginOptions = loginOptions;
            this.mMyCallback = this;
        }

        @Override
        public void doWork() throws RemoteException {
            AccountManager.this.getAccountsByTypeAndFeatures(this.mAccountType, this.mFeatures, new AccountManagerCallback<Account[]>() {
                @Override
                public void run(AccountManagerFuture<Account[]> future) {
                    try {
                        Account[] accounts = future.getResult();
                        GetAuthTokenByTypeAndFeaturesTask.this.mNumAccounts = accounts.length;
                        if (accounts.length == 0) {
                            if (GetAuthTokenByTypeAndFeaturesTask.this.mActivity != null) {
                                GetAuthTokenByTypeAndFeaturesTask.this.mFuture = AccountManager.this.addAccount(GetAuthTokenByTypeAndFeaturesTask.this.mAccountType, GetAuthTokenByTypeAndFeaturesTask.this.mAuthTokenType, GetAuthTokenByTypeAndFeaturesTask.this.mFeatures, GetAuthTokenByTypeAndFeaturesTask.this.mAddAccountOptions, GetAuthTokenByTypeAndFeaturesTask.this.mActivity, GetAuthTokenByTypeAndFeaturesTask.this.mMyCallback, GetAuthTokenByTypeAndFeaturesTask.this.mHandler);
                                return;
                            }
                            Bundle result = new Bundle();
                            result.putString(AccountManager.KEY_ACCOUNT_NAME, null);
                            result.putString("accountType", null);
                            result.putString(AccountManager.KEY_AUTHTOKEN, null);
                            try {
                                GetAuthTokenByTypeAndFeaturesTask.this.mResponse.onResult(result);
                                return;
                            } catch (RemoteException e) {
                                return;
                            }
                        }
                        if (accounts.length == 1) {
                            if (GetAuthTokenByTypeAndFeaturesTask.this.mActivity == null) {
                                GetAuthTokenByTypeAndFeaturesTask.this.mFuture = AccountManager.this.getAuthToken(accounts[0], GetAuthTokenByTypeAndFeaturesTask.this.mAuthTokenType, false, GetAuthTokenByTypeAndFeaturesTask.this.mMyCallback, GetAuthTokenByTypeAndFeaturesTask.this.mHandler);
                                return;
                            } else {
                                GetAuthTokenByTypeAndFeaturesTask.this.mFuture = AccountManager.this.getAuthToken(accounts[0], GetAuthTokenByTypeAndFeaturesTask.this.mAuthTokenType, GetAuthTokenByTypeAndFeaturesTask.this.mLoginOptions, GetAuthTokenByTypeAndFeaturesTask.this.mActivity, GetAuthTokenByTypeAndFeaturesTask.this.mMyCallback, GetAuthTokenByTypeAndFeaturesTask.this.mHandler);
                                return;
                            }
                        }
                        if (GetAuthTokenByTypeAndFeaturesTask.this.mActivity != null) {
                            IAccountManagerResponse chooseResponse = new IAccountManagerResponse.Stub() {
                                @Override
                                public void onResult(Bundle value) throws RemoteException {
                                    Account account = new Account(value.getString(AccountManager.KEY_ACCOUNT_NAME), value.getString("accountType"));
                                    GetAuthTokenByTypeAndFeaturesTask.this.mFuture = AccountManager.this.getAuthToken(account, GetAuthTokenByTypeAndFeaturesTask.this.mAuthTokenType, GetAuthTokenByTypeAndFeaturesTask.this.mLoginOptions, GetAuthTokenByTypeAndFeaturesTask.this.mActivity, GetAuthTokenByTypeAndFeaturesTask.this.mMyCallback, GetAuthTokenByTypeAndFeaturesTask.this.mHandler);
                                }

                                @Override
                                public void onError(int errorCode, String errorMessage) throws RemoteException {
                                    GetAuthTokenByTypeAndFeaturesTask.this.mResponse.onError(errorCode, errorMessage);
                                }
                            };
                            Intent intent = new Intent();
                            ComponentName componentName = ComponentName.unflattenFromString(Resources.getSystem().getString(R.string.config_chooseAccountActivity));
                            intent.setClassName(componentName.getPackageName(), componentName.getClassName());
                            intent.putExtra(AccountManager.KEY_ACCOUNTS, accounts);
                            intent.putExtra(AccountManager.KEY_ACCOUNT_MANAGER_RESPONSE, new AccountManagerResponse(chooseResponse));
                            GetAuthTokenByTypeAndFeaturesTask.this.mActivity.startActivity(intent);
                            return;
                        }
                        Bundle result2 = new Bundle();
                        result2.putString(AccountManager.KEY_ACCOUNTS, null);
                        try {
                            GetAuthTokenByTypeAndFeaturesTask.this.mResponse.onResult(result2);
                        } catch (RemoteException e2) {
                        }
                    } catch (AuthenticatorException e3) {
                        GetAuthTokenByTypeAndFeaturesTask.this.setException(e3);
                    } catch (OperationCanceledException e4) {
                        GetAuthTokenByTypeAndFeaturesTask.this.setException(e4);
                    } catch (IOException e5) {
                        GetAuthTokenByTypeAndFeaturesTask.this.setException(e5);
                    }
                }
            }, this.mHandler);
        }

        @Override
        public void run(AccountManagerFuture<Bundle> future) {
            try {
                Bundle result = future.getResult();
                if (this.mNumAccounts == 0) {
                    String accountName = result.getString(AccountManager.KEY_ACCOUNT_NAME);
                    String accountType = result.getString("accountType");
                    if (TextUtils.isEmpty(accountName) || TextUtils.isEmpty(accountType)) {
                        setException(new AuthenticatorException("account not in result"));
                    } else {
                        Account account = new Account(accountName, accountType);
                        this.mNumAccounts = 1;
                        AccountManager.this.getAuthToken(account, this.mAuthTokenType, (Bundle) null, this.mActivity, this.mMyCallback, this.mHandler);
                    }
                } else {
                    set(result);
                }
            } catch (AuthenticatorException e) {
                setException(e);
            } catch (OperationCanceledException e2) {
                cancel(true);
            } catch (IOException e3) {
                setException(e3);
            }
        }
    }

    public AccountManagerFuture<Bundle> getAuthTokenByFeatures(String accountType, String authTokenType, String[] features, Activity activity, Bundle addAccountOptions, Bundle getAuthTokenOptions, AccountManagerCallback<Bundle> callback, Handler handler) {
        if (accountType == null) {
            throw new IllegalArgumentException("account type is null");
        }
        if (authTokenType == null) {
            throw new IllegalArgumentException("authTokenType is null");
        }
        GetAuthTokenByTypeAndFeaturesTask task = new GetAuthTokenByTypeAndFeaturesTask(accountType, authTokenType, features, activity, addAccountOptions, getAuthTokenOptions, callback, handler);
        task.start();
        return task;
    }

    public static Intent newChooseAccountIntent(Account selectedAccount, ArrayList<Account> allowableAccounts, String[] allowableAccountTypes, boolean alwaysPromptForAccount, String descriptionOverrideText, String addAccountAuthTokenType, String[] addAccountRequiredFeatures, Bundle addAccountOptions) {
        Intent intent = new Intent();
        ComponentName componentName = ComponentName.unflattenFromString(Resources.getSystem().getString(R.string.config_chooseTypeAndAccountActivity));
        intent.setClassName(componentName.getPackageName(), componentName.getClassName());
        intent.putExtra(ChooseTypeAndAccountActivity.EXTRA_ALLOWABLE_ACCOUNTS_ARRAYLIST, allowableAccounts);
        intent.putExtra(ChooseTypeAndAccountActivity.EXTRA_ALLOWABLE_ACCOUNT_TYPES_STRING_ARRAY, allowableAccountTypes);
        intent.putExtra(ChooseTypeAndAccountActivity.EXTRA_ADD_ACCOUNT_OPTIONS_BUNDLE, addAccountOptions);
        intent.putExtra(ChooseTypeAndAccountActivity.EXTRA_SELECTED_ACCOUNT, selectedAccount);
        intent.putExtra(ChooseTypeAndAccountActivity.EXTRA_ALWAYS_PROMPT_FOR_ACCOUNT, alwaysPromptForAccount);
        intent.putExtra(ChooseTypeAndAccountActivity.EXTRA_DESCRIPTION_TEXT_OVERRIDE, descriptionOverrideText);
        intent.putExtra("authTokenType", addAccountAuthTokenType);
        intent.putExtra(ChooseTypeAndAccountActivity.EXTRA_ADD_ACCOUNT_REQUIRED_FEATURES_STRING_ARRAY, addAccountRequiredFeatures);
        return intent;
    }

    public void addOnAccountsUpdatedListener(OnAccountsUpdateListener listener, Handler handler, boolean updateImmediately) {
        if (listener == null) {
            throw new IllegalArgumentException("the listener is null");
        }
        synchronized (this.mAccountsUpdatedListeners) {
            if (this.mAccountsUpdatedListeners.containsKey(listener)) {
                throw new IllegalStateException("this listener is already added");
            }
            boolean wasEmpty = this.mAccountsUpdatedListeners.isEmpty();
            this.mAccountsUpdatedListeners.put(listener, handler);
            if (wasEmpty) {
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(LOGIN_ACCOUNTS_CHANGED_ACTION);
                intentFilter.addAction(Intent.ACTION_DEVICE_STORAGE_OK);
                this.mContext.registerReceiver(this.mAccountsChangedBroadcastReceiver, intentFilter);
            }
        }
        if (updateImmediately) {
            postToHandler(handler, listener, getAccounts());
        }
    }

    public void removeOnAccountsUpdatedListener(OnAccountsUpdateListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener is null");
        }
        synchronized (this.mAccountsUpdatedListeners) {
            if (!this.mAccountsUpdatedListeners.containsKey(listener)) {
                Log.e(TAG, "Listener was not previously added");
                return;
            }
            this.mAccountsUpdatedListeners.remove(listener);
            if (this.mAccountsUpdatedListeners.isEmpty()) {
                this.mContext.unregisterReceiver(this.mAccountsChangedBroadcastReceiver);
            }
        }
    }
}
