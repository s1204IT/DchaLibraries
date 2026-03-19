package android.accounts;

import android.Manifest;
import android.accounts.IAccountAuthenticator;
import android.content.Context;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import java.util.Arrays;

public abstract class AbstractAccountAuthenticator {
    private static final String KEY_ACCOUNT = "android.accounts.AbstractAccountAuthenticator.KEY_ACCOUNT";
    private static final String KEY_AUTH_TOKEN_TYPE = "android.accounts.AbstractAccountAuthenticato.KEY_AUTH_TOKEN_TYPE";
    public static final String KEY_CUSTOM_TOKEN_EXPIRY = "android.accounts.expiry";
    private static final String KEY_OPTIONS = "android.accounts.AbstractAccountAuthenticator.KEY_OPTIONS";
    private static final String KEY_REQUIRED_FEATURES = "android.accounts.AbstractAccountAuthenticator.KEY_REQUIRED_FEATURES";
    private static final String TAG = "AccountAuthenticator";
    private final Context mContext;
    private Transport mTransport = new Transport();

    public abstract Bundle addAccount(AccountAuthenticatorResponse accountAuthenticatorResponse, String str, String str2, String[] strArr, Bundle bundle) throws NetworkErrorException;

    public abstract Bundle confirmCredentials(AccountAuthenticatorResponse accountAuthenticatorResponse, Account account, Bundle bundle) throws NetworkErrorException;

    public abstract Bundle editProperties(AccountAuthenticatorResponse accountAuthenticatorResponse, String str);

    public abstract Bundle getAuthToken(AccountAuthenticatorResponse accountAuthenticatorResponse, Account account, String str, Bundle bundle) throws NetworkErrorException;

    public abstract String getAuthTokenLabel(String str);

    public abstract Bundle hasFeatures(AccountAuthenticatorResponse accountAuthenticatorResponse, Account account, String[] strArr) throws NetworkErrorException;

    public abstract Bundle updateCredentials(AccountAuthenticatorResponse accountAuthenticatorResponse, Account account, String str, Bundle bundle) throws NetworkErrorException;

    public AbstractAccountAuthenticator(Context context) {
        this.mContext = context;
    }

    private class Transport extends IAccountAuthenticator.Stub {
        private Transport() {
        }

        @Override
        public void addAccount(IAccountAuthenticatorResponse iAccountAuthenticatorResponse, String str, String str2, String[] strArr, Bundle bundle) throws RemoteException {
            if (Log.isLoggable(AbstractAccountAuthenticator.TAG, 2)) {
                StringBuilder sb = new StringBuilder();
                sb.append("addAccount: accountType ");
                sb.append(str);
                sb.append(", authTokenType ");
                sb.append(str2);
                sb.append(", features ");
                sb.append(strArr == null ? "[]" : Arrays.toString(strArr));
                Log.v(AbstractAccountAuthenticator.TAG, sb.toString());
            }
            AbstractAccountAuthenticator.this.checkBinderPermission();
            try {
                Bundle bundleAddAccount = AbstractAccountAuthenticator.this.addAccount(new AccountAuthenticatorResponse(iAccountAuthenticatorResponse), str, str2, strArr, bundle);
                if (Log.isLoggable(AbstractAccountAuthenticator.TAG, 2)) {
                    if (bundleAddAccount != null) {
                        bundleAddAccount.keySet();
                    }
                    Log.v(AbstractAccountAuthenticator.TAG, "addAccount: result " + AccountManager.sanitizeResult(bundleAddAccount));
                }
                if (bundleAddAccount != null) {
                    iAccountAuthenticatorResponse.onResult(bundleAddAccount);
                } else {
                    iAccountAuthenticatorResponse.onError(5, "null bundle returned");
                }
            } catch (Exception e) {
                AbstractAccountAuthenticator.this.handleException(iAccountAuthenticatorResponse, "addAccount", str, e);
            }
        }

        @Override
        public void confirmCredentials(IAccountAuthenticatorResponse iAccountAuthenticatorResponse, Account account, Bundle bundle) throws RemoteException {
            if (Log.isLoggable(AbstractAccountAuthenticator.TAG, 2)) {
                Log.v(AbstractAccountAuthenticator.TAG, "confirmCredentials: " + account);
            }
            AbstractAccountAuthenticator.this.checkBinderPermission();
            try {
                Bundle bundleConfirmCredentials = AbstractAccountAuthenticator.this.confirmCredentials(new AccountAuthenticatorResponse(iAccountAuthenticatorResponse), account, bundle);
                if (Log.isLoggable(AbstractAccountAuthenticator.TAG, 2)) {
                    if (bundleConfirmCredentials != null) {
                        bundleConfirmCredentials.keySet();
                    }
                    Log.v(AbstractAccountAuthenticator.TAG, "confirmCredentials: result " + AccountManager.sanitizeResult(bundleConfirmCredentials));
                }
                if (bundleConfirmCredentials != null) {
                    iAccountAuthenticatorResponse.onResult(bundleConfirmCredentials);
                }
            } catch (Exception e) {
                AbstractAccountAuthenticator.this.handleException(iAccountAuthenticatorResponse, "confirmCredentials", account.toString(), e);
            }
        }

        @Override
        public void getAuthTokenLabel(IAccountAuthenticatorResponse iAccountAuthenticatorResponse, String str) throws RemoteException {
            if (Log.isLoggable(AbstractAccountAuthenticator.TAG, 2)) {
                Log.v(AbstractAccountAuthenticator.TAG, "getAuthTokenLabel: authTokenType " + str);
            }
            AbstractAccountAuthenticator.this.checkBinderPermission();
            try {
                Bundle bundle = new Bundle();
                bundle.putString(AccountManager.KEY_AUTH_TOKEN_LABEL, AbstractAccountAuthenticator.this.getAuthTokenLabel(str));
                if (Log.isLoggable(AbstractAccountAuthenticator.TAG, 2)) {
                    bundle.keySet();
                    Log.v(AbstractAccountAuthenticator.TAG, "getAuthTokenLabel: result " + AccountManager.sanitizeResult(bundle));
                }
                iAccountAuthenticatorResponse.onResult(bundle);
            } catch (Exception e) {
                AbstractAccountAuthenticator.this.handleException(iAccountAuthenticatorResponse, "getAuthTokenLabel", str, e);
            }
        }

        @Override
        public void getAuthToken(IAccountAuthenticatorResponse iAccountAuthenticatorResponse, Account account, String str, Bundle bundle) throws RemoteException {
            if (Log.isLoggable(AbstractAccountAuthenticator.TAG, 2)) {
                Log.v(AbstractAccountAuthenticator.TAG, "getAuthToken: " + account + ", authTokenType " + str);
            }
            AbstractAccountAuthenticator.this.checkBinderPermission();
            try {
                Bundle authToken = AbstractAccountAuthenticator.this.getAuthToken(new AccountAuthenticatorResponse(iAccountAuthenticatorResponse), account, str, bundle);
                if (Log.isLoggable(AbstractAccountAuthenticator.TAG, 2)) {
                    if (authToken != null) {
                        authToken.keySet();
                    }
                    Log.v(AbstractAccountAuthenticator.TAG, "getAuthToken: result " + AccountManager.sanitizeResult(authToken));
                }
                if (authToken != null) {
                    iAccountAuthenticatorResponse.onResult(authToken);
                }
            } catch (Exception e) {
                AbstractAccountAuthenticator.this.handleException(iAccountAuthenticatorResponse, "getAuthToken", account.toString() + "," + str, e);
            }
        }

        @Override
        public void updateCredentials(IAccountAuthenticatorResponse iAccountAuthenticatorResponse, Account account, String str, Bundle bundle) throws RemoteException {
            if (Log.isLoggable(AbstractAccountAuthenticator.TAG, 2)) {
                Log.v(AbstractAccountAuthenticator.TAG, "updateCredentials: " + account + ", authTokenType " + str);
            }
            AbstractAccountAuthenticator.this.checkBinderPermission();
            try {
                Bundle bundleUpdateCredentials = AbstractAccountAuthenticator.this.updateCredentials(new AccountAuthenticatorResponse(iAccountAuthenticatorResponse), account, str, bundle);
                if (Log.isLoggable(AbstractAccountAuthenticator.TAG, 2)) {
                    if (bundleUpdateCredentials != null) {
                        bundleUpdateCredentials.keySet();
                    }
                    Log.v(AbstractAccountAuthenticator.TAG, "updateCredentials: result " + AccountManager.sanitizeResult(bundleUpdateCredentials));
                }
                if (bundleUpdateCredentials != null) {
                    iAccountAuthenticatorResponse.onResult(bundleUpdateCredentials);
                }
            } catch (Exception e) {
                AbstractAccountAuthenticator.this.handleException(iAccountAuthenticatorResponse, "updateCredentials", account.toString() + "," + str, e);
            }
        }

        @Override
        public void editProperties(IAccountAuthenticatorResponse iAccountAuthenticatorResponse, String str) throws RemoteException {
            AbstractAccountAuthenticator.this.checkBinderPermission();
            try {
                Bundle bundleEditProperties = AbstractAccountAuthenticator.this.editProperties(new AccountAuthenticatorResponse(iAccountAuthenticatorResponse), str);
                if (bundleEditProperties != null) {
                    iAccountAuthenticatorResponse.onResult(bundleEditProperties);
                }
            } catch (Exception e) {
                AbstractAccountAuthenticator.this.handleException(iAccountAuthenticatorResponse, "editProperties", str, e);
            }
        }

        @Override
        public void hasFeatures(IAccountAuthenticatorResponse iAccountAuthenticatorResponse, Account account, String[] strArr) throws RemoteException {
            AbstractAccountAuthenticator.this.checkBinderPermission();
            try {
                Bundle bundleHasFeatures = AbstractAccountAuthenticator.this.hasFeatures(new AccountAuthenticatorResponse(iAccountAuthenticatorResponse), account, strArr);
                if (bundleHasFeatures != null) {
                    iAccountAuthenticatorResponse.onResult(bundleHasFeatures);
                }
            } catch (Exception e) {
                AbstractAccountAuthenticator.this.handleException(iAccountAuthenticatorResponse, "hasFeatures", account.toString(), e);
            }
        }

        @Override
        public void getAccountRemovalAllowed(IAccountAuthenticatorResponse iAccountAuthenticatorResponse, Account account) throws RemoteException {
            AbstractAccountAuthenticator.this.checkBinderPermission();
            try {
                Bundle accountRemovalAllowed = AbstractAccountAuthenticator.this.getAccountRemovalAllowed(new AccountAuthenticatorResponse(iAccountAuthenticatorResponse), account);
                if (accountRemovalAllowed != null) {
                    iAccountAuthenticatorResponse.onResult(accountRemovalAllowed);
                }
            } catch (Exception e) {
                AbstractAccountAuthenticator.this.handleException(iAccountAuthenticatorResponse, "getAccountRemovalAllowed", account.toString(), e);
            }
        }

        @Override
        public void getAccountCredentialsForCloning(IAccountAuthenticatorResponse iAccountAuthenticatorResponse, Account account) throws RemoteException {
            AbstractAccountAuthenticator.this.checkBinderPermission();
            try {
                Bundle accountCredentialsForCloning = AbstractAccountAuthenticator.this.getAccountCredentialsForCloning(new AccountAuthenticatorResponse(iAccountAuthenticatorResponse), account);
                if (accountCredentialsForCloning != null) {
                    iAccountAuthenticatorResponse.onResult(accountCredentialsForCloning);
                }
            } catch (Exception e) {
                AbstractAccountAuthenticator.this.handleException(iAccountAuthenticatorResponse, "getAccountCredentialsForCloning", account.toString(), e);
            }
        }

        @Override
        public void addAccountFromCredentials(IAccountAuthenticatorResponse iAccountAuthenticatorResponse, Account account, Bundle bundle) throws RemoteException {
            AbstractAccountAuthenticator.this.checkBinderPermission();
            try {
                Bundle bundleAddAccountFromCredentials = AbstractAccountAuthenticator.this.addAccountFromCredentials(new AccountAuthenticatorResponse(iAccountAuthenticatorResponse), account, bundle);
                if (bundleAddAccountFromCredentials != null) {
                    iAccountAuthenticatorResponse.onResult(bundleAddAccountFromCredentials);
                }
            } catch (Exception e) {
                AbstractAccountAuthenticator.this.handleException(iAccountAuthenticatorResponse, "addAccountFromCredentials", account.toString(), e);
            }
        }

        @Override
        public void startAddAccountSession(IAccountAuthenticatorResponse iAccountAuthenticatorResponse, String str, String str2, String[] strArr, Bundle bundle) throws RemoteException {
            if (Log.isLoggable(AbstractAccountAuthenticator.TAG, 2)) {
                StringBuilder sb = new StringBuilder();
                sb.append("startAddAccountSession: accountType ");
                sb.append(str);
                sb.append(", authTokenType ");
                sb.append(str2);
                sb.append(", features ");
                sb.append(strArr == null ? "[]" : Arrays.toString(strArr));
                Log.v(AbstractAccountAuthenticator.TAG, sb.toString());
            }
            AbstractAccountAuthenticator.this.checkBinderPermission();
            try {
                Bundle bundleStartAddAccountSession = AbstractAccountAuthenticator.this.startAddAccountSession(new AccountAuthenticatorResponse(iAccountAuthenticatorResponse), str, str2, strArr, bundle);
                if (Log.isLoggable(AbstractAccountAuthenticator.TAG, 2)) {
                    if (bundleStartAddAccountSession != null) {
                        bundleStartAddAccountSession.keySet();
                    }
                    Log.v(AbstractAccountAuthenticator.TAG, "startAddAccountSession: result " + AccountManager.sanitizeResult(bundleStartAddAccountSession));
                }
                if (bundleStartAddAccountSession != null) {
                    iAccountAuthenticatorResponse.onResult(bundleStartAddAccountSession);
                }
            } catch (Exception e) {
                AbstractAccountAuthenticator.this.handleException(iAccountAuthenticatorResponse, "startAddAccountSession", str, e);
            }
        }

        @Override
        public void startUpdateCredentialsSession(IAccountAuthenticatorResponse iAccountAuthenticatorResponse, Account account, String str, Bundle bundle) throws RemoteException {
            if (Log.isLoggable(AbstractAccountAuthenticator.TAG, 2)) {
                Log.v(AbstractAccountAuthenticator.TAG, "startUpdateCredentialsSession: " + account + ", authTokenType " + str);
            }
            AbstractAccountAuthenticator.this.checkBinderPermission();
            try {
                Bundle bundleStartUpdateCredentialsSession = AbstractAccountAuthenticator.this.startUpdateCredentialsSession(new AccountAuthenticatorResponse(iAccountAuthenticatorResponse), account, str, bundle);
                if (Log.isLoggable(AbstractAccountAuthenticator.TAG, 2)) {
                    if (bundleStartUpdateCredentialsSession != null) {
                        bundleStartUpdateCredentialsSession.keySet();
                    }
                    Log.v(AbstractAccountAuthenticator.TAG, "startUpdateCredentialsSession: result " + AccountManager.sanitizeResult(bundleStartUpdateCredentialsSession));
                }
                if (bundleStartUpdateCredentialsSession != null) {
                    iAccountAuthenticatorResponse.onResult(bundleStartUpdateCredentialsSession);
                }
            } catch (Exception e) {
                AbstractAccountAuthenticator.this.handleException(iAccountAuthenticatorResponse, "startUpdateCredentialsSession", account.toString() + "," + str, e);
            }
        }

        @Override
        public void finishSession(IAccountAuthenticatorResponse iAccountAuthenticatorResponse, String str, Bundle bundle) throws RemoteException {
            if (Log.isLoggable(AbstractAccountAuthenticator.TAG, 2)) {
                Log.v(AbstractAccountAuthenticator.TAG, "finishSession: accountType " + str);
            }
            AbstractAccountAuthenticator.this.checkBinderPermission();
            try {
                Bundle bundleFinishSession = AbstractAccountAuthenticator.this.finishSession(new AccountAuthenticatorResponse(iAccountAuthenticatorResponse), str, bundle);
                if (bundleFinishSession != null) {
                    bundleFinishSession.keySet();
                }
                if (Log.isLoggable(AbstractAccountAuthenticator.TAG, 2)) {
                    Log.v(AbstractAccountAuthenticator.TAG, "finishSession: result " + AccountManager.sanitizeResult(bundleFinishSession));
                }
                if (bundleFinishSession != null) {
                    iAccountAuthenticatorResponse.onResult(bundleFinishSession);
                }
            } catch (Exception e) {
                AbstractAccountAuthenticator.this.handleException(iAccountAuthenticatorResponse, "finishSession", str, e);
            }
        }

        @Override
        public void isCredentialsUpdateSuggested(IAccountAuthenticatorResponse iAccountAuthenticatorResponse, Account account, String str) throws RemoteException {
            AbstractAccountAuthenticator.this.checkBinderPermission();
            try {
                Bundle bundleIsCredentialsUpdateSuggested = AbstractAccountAuthenticator.this.isCredentialsUpdateSuggested(new AccountAuthenticatorResponse(iAccountAuthenticatorResponse), account, str);
                if (bundleIsCredentialsUpdateSuggested != null) {
                    iAccountAuthenticatorResponse.onResult(bundleIsCredentialsUpdateSuggested);
                }
            } catch (Exception e) {
                AbstractAccountAuthenticator.this.handleException(iAccountAuthenticatorResponse, "isCredentialsUpdateSuggested", account.toString(), e);
            }
        }
    }

    private void handleException(IAccountAuthenticatorResponse iAccountAuthenticatorResponse, String str, String str2, Exception exc) throws RemoteException {
        if (exc instanceof NetworkErrorException) {
            if (Log.isLoggable(TAG, 2)) {
                Log.v(TAG, str + "(" + str2 + ")", exc);
            }
            iAccountAuthenticatorResponse.onError(3, exc.getMessage());
            return;
        }
        if (exc instanceof UnsupportedOperationException) {
            if (Log.isLoggable(TAG, 2)) {
                Log.v(TAG, str + "(" + str2 + ")", exc);
            }
            iAccountAuthenticatorResponse.onError(6, str + " not supported");
            return;
        }
        if (exc instanceof IllegalArgumentException) {
            if (Log.isLoggable(TAG, 2)) {
                Log.v(TAG, str + "(" + str2 + ")", exc);
            }
            iAccountAuthenticatorResponse.onError(7, str + " not supported");
            return;
        }
        Log.w(TAG, str + "(" + str2 + ")", exc);
        StringBuilder sb = new StringBuilder();
        sb.append(str);
        sb.append(" failed");
        iAccountAuthenticatorResponse.onError(1, sb.toString());
    }

    private void checkBinderPermission() {
        int callingUid = Binder.getCallingUid();
        if (this.mContext.checkCallingOrSelfPermission(Manifest.permission.ACCOUNT_MANAGER) != 0) {
            throw new SecurityException("caller uid " + callingUid + " lacks " + Manifest.permission.ACCOUNT_MANAGER);
        }
    }

    public final IBinder getIBinder() {
        return this.mTransport.asBinder();
    }

    public Bundle getAccountRemovalAllowed(AccountAuthenticatorResponse accountAuthenticatorResponse, Account account) throws NetworkErrorException {
        Bundle bundle = new Bundle();
        bundle.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, true);
        return bundle;
    }

    public Bundle getAccountCredentialsForCloning(final AccountAuthenticatorResponse accountAuthenticatorResponse, Account account) throws NetworkErrorException {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Bundle bundle = new Bundle();
                bundle.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
                accountAuthenticatorResponse.onResult(bundle);
            }
        }).start();
        return null;
    }

    public Bundle addAccountFromCredentials(final AccountAuthenticatorResponse accountAuthenticatorResponse, Account account, Bundle bundle) throws NetworkErrorException {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Bundle bundle2 = new Bundle();
                bundle2.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
                accountAuthenticatorResponse.onResult(bundle2);
            }
        }).start();
        return null;
    }

    public Bundle startAddAccountSession(final AccountAuthenticatorResponse accountAuthenticatorResponse, String str, final String str2, final String[] strArr, final Bundle bundle) throws NetworkErrorException {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Bundle bundle2 = new Bundle();
                bundle2.putString(AbstractAccountAuthenticator.KEY_AUTH_TOKEN_TYPE, str2);
                bundle2.putStringArray(AbstractAccountAuthenticator.KEY_REQUIRED_FEATURES, strArr);
                bundle2.putBundle(AbstractAccountAuthenticator.KEY_OPTIONS, bundle);
                Bundle bundle3 = new Bundle();
                bundle3.putBundle(AccountManager.KEY_ACCOUNT_SESSION_BUNDLE, bundle2);
                accountAuthenticatorResponse.onResult(bundle3);
            }
        }).start();
        return null;
    }

    public Bundle startUpdateCredentialsSession(final AccountAuthenticatorResponse accountAuthenticatorResponse, final Account account, final String str, final Bundle bundle) throws NetworkErrorException {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Bundle bundle2 = new Bundle();
                bundle2.putString(AbstractAccountAuthenticator.KEY_AUTH_TOKEN_TYPE, str);
                bundle2.putParcelable(AbstractAccountAuthenticator.KEY_ACCOUNT, account);
                bundle2.putBundle(AbstractAccountAuthenticator.KEY_OPTIONS, bundle);
                Bundle bundle3 = new Bundle();
                bundle3.putBundle(AccountManager.KEY_ACCOUNT_SESSION_BUNDLE, bundle2);
                accountAuthenticatorResponse.onResult(bundle3);
            }
        }).start();
        return null;
    }

    public Bundle finishSession(AccountAuthenticatorResponse accountAuthenticatorResponse, String str, Bundle bundle) throws NetworkErrorException {
        Bundle bundle2;
        if (TextUtils.isEmpty(str)) {
            Log.e(TAG, "Account type cannot be empty.");
            Bundle bundle3 = new Bundle();
            bundle3.putInt("errorCode", 7);
            bundle3.putString(AccountManager.KEY_ERROR_MESSAGE, "accountType cannot be empty.");
            return bundle3;
        }
        if (bundle == null) {
            Log.e(TAG, "Session bundle cannot be null.");
            Bundle bundle4 = new Bundle();
            bundle4.putInt("errorCode", 7);
            bundle4.putString(AccountManager.KEY_ERROR_MESSAGE, "sessionBundle cannot be null.");
            return bundle4;
        }
        if (!bundle.containsKey(KEY_AUTH_TOKEN_TYPE)) {
            Bundle bundle5 = new Bundle();
            bundle5.putInt("errorCode", 6);
            bundle5.putString(AccountManager.KEY_ERROR_MESSAGE, "Authenticator must override finishSession if startAddAccountSession or startUpdateCredentialsSession is overridden.");
            accountAuthenticatorResponse.onResult(bundle5);
            return bundle5;
        }
        String string = bundle.getString(KEY_AUTH_TOKEN_TYPE);
        Bundle bundle6 = bundle.getBundle(KEY_OPTIONS);
        String[] stringArray = bundle.getStringArray(KEY_REQUIRED_FEATURES);
        Account account = (Account) bundle.getParcelable(KEY_ACCOUNT);
        boolean zContainsKey = bundle.containsKey(KEY_ACCOUNT);
        Bundle bundle7 = new Bundle(bundle);
        bundle7.remove(KEY_AUTH_TOKEN_TYPE);
        bundle7.remove(KEY_REQUIRED_FEATURES);
        bundle7.remove(KEY_OPTIONS);
        bundle7.remove(KEY_ACCOUNT);
        if (bundle6 != null) {
            bundle6.putAll(bundle7);
            bundle2 = bundle6;
        } else {
            bundle2 = bundle7;
        }
        if (zContainsKey) {
            return updateCredentials(accountAuthenticatorResponse, account, string, bundle6);
        }
        return addAccount(accountAuthenticatorResponse, str, string, stringArray, bundle2);
    }

    public Bundle isCredentialsUpdateSuggested(AccountAuthenticatorResponse accountAuthenticatorResponse, Account account, String str) throws NetworkErrorException {
        Bundle bundle = new Bundle();
        bundle.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
        return bundle;
    }
}
