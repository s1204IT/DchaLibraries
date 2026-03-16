package com.android.volley.toolbox;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import com.android.volley.AuthFailureError;

public class AndroidAuthenticator implements Authenticator {
    private final Account mAccount;
    private final String mAuthTokenType;
    private final Context mContext;
    private final boolean mNotifyAuthFailure;

    public AndroidAuthenticator(Context context, Account account, String authTokenType) {
        this(context, account, authTokenType, false);
    }

    public AndroidAuthenticator(Context context, Account account, String authTokenType, boolean notifyAuthFailure) {
        this.mContext = context;
        this.mAccount = account;
        this.mAuthTokenType = authTokenType;
        this.mNotifyAuthFailure = notifyAuthFailure;
    }

    public Account getAccount() {
        return this.mAccount;
    }

    @Override
    public String getAuthToken() throws AuthFailureError {
        AccountManager accountManager = AccountManager.get(this.mContext);
        AccountManagerFuture<Bundle> future = accountManager.getAuthToken(this.mAccount, this.mAuthTokenType, this.mNotifyAuthFailure, null, null);
        try {
            Bundle result = future.getResult();
            String authToken = null;
            if (future.isDone() && !future.isCancelled()) {
                if (result.containsKey("intent")) {
                    Intent intent = (Intent) result.getParcelable("intent");
                    throw new AuthFailureError(intent);
                }
                authToken = result.getString("authtoken");
            }
            if (authToken == null) {
                throw new AuthFailureError("Got null auth token for type: " + this.mAuthTokenType);
            }
            return authToken;
        } catch (Exception e) {
            throw new AuthFailureError("Error while retrieving auth token", e);
        }
    }

    @Override
    public void invalidateAuthToken(String authToken) {
        AccountManager.get(this.mContext).invalidateAuthToken(this.mAccount.type, authToken);
    }
}
