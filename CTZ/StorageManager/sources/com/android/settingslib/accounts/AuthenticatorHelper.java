package com.android.settingslib.accounts;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncAdapterType;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public final class AuthenticatorHelper extends BroadcastReceiver {
    private final Map<String, Drawable> mAccTypeIconCache;
    private final HashMap<String, ArrayList<String>> mAccountTypeToAuthorities;
    private final Context mContext;
    private final ArrayList<String> mEnabledAccountTypes;
    private final OnAccountsUpdateListener mListener;
    private boolean mListeningToAccountUpdates;
    private final Map<String, AuthenticatorDescription> mTypeToAuthDescription;
    private final UserHandle mUserHandle;

    public interface OnAccountsUpdateListener {
        void onAccountsUpdate(UserHandle userHandle);
    }

    public void updateAuthDescriptions(Context context) {
        AuthenticatorDescription[] authenticatorTypesAsUser = AccountManager.get(context).getAuthenticatorTypesAsUser(this.mUserHandle.getIdentifier());
        for (int i = 0; i < authenticatorTypesAsUser.length; i++) {
            this.mTypeToAuthDescription.put(authenticatorTypesAsUser[i].type, authenticatorTypesAsUser[i]);
        }
    }

    void onAccountsUpdated(Account[] accountArr) {
        updateAuthDescriptions(this.mContext);
        if (accountArr == null) {
            accountArr = AccountManager.get(this.mContext).getAccountsAsUser(this.mUserHandle.getIdentifier());
        }
        this.mEnabledAccountTypes.clear();
        this.mAccTypeIconCache.clear();
        for (Account account : accountArr) {
            if (!this.mEnabledAccountTypes.contains(account.type)) {
                this.mEnabledAccountTypes.add(account.type);
            }
        }
        buildAccountTypeToAuthoritiesMap();
        if (this.mListeningToAccountUpdates) {
            this.mListener.onAccountsUpdate(this.mUserHandle);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        onAccountsUpdated(AccountManager.get(this.mContext).getAccountsAsUser(this.mUserHandle.getIdentifier()));
    }

    private void buildAccountTypeToAuthoritiesMap() {
        this.mAccountTypeToAuthorities.clear();
        for (SyncAdapterType syncAdapterType : ContentResolver.getSyncAdapterTypesAsUser(this.mUserHandle.getIdentifier())) {
            ArrayList<String> arrayList = this.mAccountTypeToAuthorities.get(syncAdapterType.accountType);
            if (arrayList == null) {
                arrayList = new ArrayList<>();
                this.mAccountTypeToAuthorities.put(syncAdapterType.accountType, arrayList);
            }
            if (Log.isLoggable("AuthenticatorHelper", 2)) {
                Log.v("AuthenticatorHelper", "Added authority " + syncAdapterType.authority + " to accountType " + syncAdapterType.accountType);
            }
            arrayList.add(syncAdapterType.authority);
        }
    }
}
