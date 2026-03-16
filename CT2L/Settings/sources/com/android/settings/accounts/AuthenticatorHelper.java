package com.android.settings.accounts;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SyncAdapterType;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import com.google.android.collect.Maps;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public final class AuthenticatorHelper extends BroadcastReceiver {
    private AuthenticatorDescription[] mAuthDescs;
    private final Context mContext;
    private final OnAccountsUpdateListener mListener;
    private boolean mListeningToAccountUpdates;
    private final UserManager mUm;
    private final UserHandle mUserHandle;
    private Map<String, AuthenticatorDescription> mTypeToAuthDescription = new HashMap();
    private ArrayList<String> mEnabledAccountTypes = new ArrayList<>();
    private Map<String, Drawable> mAccTypeIconCache = new HashMap();
    private HashMap<String, ArrayList<String>> mAccountTypeToAuthorities = Maps.newHashMap();

    public interface OnAccountsUpdateListener {
        void onAccountsUpdate(UserHandle userHandle);
    }

    public AuthenticatorHelper(Context context, UserHandle userHandle, UserManager userManager, OnAccountsUpdateListener listener) {
        this.mContext = context;
        this.mUm = userManager;
        this.mUserHandle = userHandle;
        this.mListener = listener;
        onAccountsUpdated(null);
    }

    public String[] getEnabledAccountTypes() {
        return (String[]) this.mEnabledAccountTypes.toArray(new String[this.mEnabledAccountTypes.size()]);
    }

    public void preloadDrawableForType(final Context context, final String accountType) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                AuthenticatorHelper.this.getDrawableForType(context, accountType);
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
    }

    public Drawable getDrawableForType(Context context, String accountType) {
        Drawable icon = null;
        synchronized (this.mAccTypeIconCache) {
            if (this.mAccTypeIconCache.containsKey(accountType)) {
                return this.mAccTypeIconCache.get(accountType);
            }
            if (this.mTypeToAuthDescription.containsKey(accountType)) {
                try {
                    AuthenticatorDescription desc = this.mTypeToAuthDescription.get(accountType);
                    Context authContext = context.createPackageContextAsUser(desc.packageName, 0, this.mUserHandle);
                    icon = this.mContext.getPackageManager().getUserBadgedIcon(authContext.getDrawable(desc.iconId), this.mUserHandle);
                    synchronized (this.mAccTypeIconCache) {
                        this.mAccTypeIconCache.put(accountType, icon);
                    }
                } catch (PackageManager.NameNotFoundException e) {
                } catch (Resources.NotFoundException e2) {
                }
            }
            if (icon == null) {
                icon = context.getPackageManager().getDefaultActivityIcon();
            }
            return icon;
        }
    }

    public CharSequence getLabelForType(Context context, String accountType) {
        if (!this.mTypeToAuthDescription.containsKey(accountType)) {
            return null;
        }
        try {
            AuthenticatorDescription desc = this.mTypeToAuthDescription.get(accountType);
            Context authContext = context.createPackageContextAsUser(desc.packageName, 0, this.mUserHandle);
            CharSequence label = authContext.getResources().getText(desc.labelId);
            return label;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w("AuthenticatorHelper", "No label name for account type " + accountType);
            return null;
        } catch (Resources.NotFoundException e2) {
            Log.w("AuthenticatorHelper", "No label icon for account type " + accountType);
            return null;
        }
    }

    public String getPackageForType(String accountType) {
        if (!this.mTypeToAuthDescription.containsKey(accountType)) {
            return null;
        }
        AuthenticatorDescription desc = this.mTypeToAuthDescription.get(accountType);
        return desc.packageName;
    }

    public int getLabelIdForType(String accountType) {
        if (!this.mTypeToAuthDescription.containsKey(accountType)) {
            return -1;
        }
        AuthenticatorDescription desc = this.mTypeToAuthDescription.get(accountType);
        return desc.labelId;
    }

    public void updateAuthDescriptions(Context context) {
        this.mAuthDescs = AccountManager.get(context).getAuthenticatorTypesAsUser(this.mUserHandle.getIdentifier());
        for (int i = 0; i < this.mAuthDescs.length; i++) {
            this.mTypeToAuthDescription.put(this.mAuthDescs[i].type, this.mAuthDescs[i]);
        }
    }

    public boolean containsAccountType(String accountType) {
        return this.mTypeToAuthDescription.containsKey(accountType);
    }

    public AuthenticatorDescription getAccountTypeDescription(String accountType) {
        return this.mTypeToAuthDescription.get(accountType);
    }

    public boolean hasAccountPreferences(String accountType) {
        AuthenticatorDescription desc;
        return (!containsAccountType(accountType) || (desc = getAccountTypeDescription(accountType)) == null || desc.accountPreferencesId == 0) ? false : true;
    }

    void onAccountsUpdated(Account[] accounts) {
        updateAuthDescriptions(this.mContext);
        if (accounts == null) {
            accounts = AccountManager.get(this.mContext).getAccountsAsUser(this.mUserHandle.getIdentifier());
        }
        this.mEnabledAccountTypes.clear();
        this.mAccTypeIconCache.clear();
        for (Account account : accounts) {
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
        Account[] accounts = AccountManager.get(this.mContext).getAccountsAsUser(this.mUserHandle.getIdentifier());
        onAccountsUpdated(accounts);
    }

    public void listenToAccountUpdates() {
        if (!this.mListeningToAccountUpdates) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.accounts.LOGIN_ACCOUNTS_CHANGED");
            intentFilter.addAction("android.intent.action.DEVICE_STORAGE_OK");
            this.mContext.registerReceiverAsUser(this, this.mUserHandle, intentFilter, null, null);
            this.mListeningToAccountUpdates = true;
        }
    }

    public void stopListeningToAccountUpdates() {
        if (this.mListeningToAccountUpdates) {
            this.mContext.unregisterReceiver(this);
            this.mListeningToAccountUpdates = false;
        }
    }

    public ArrayList<String> getAuthoritiesForAccountType(String type) {
        return this.mAccountTypeToAuthorities.get(type);
    }

    private void buildAccountTypeToAuthoritiesMap() {
        this.mAccountTypeToAuthorities.clear();
        SyncAdapterType[] syncAdapters = ContentResolver.getSyncAdapterTypesAsUser(this.mUserHandle.getIdentifier());
        for (SyncAdapterType sa : syncAdapters) {
            ArrayList<String> authorities = this.mAccountTypeToAuthorities.get(sa.accountType);
            if (authorities == null) {
                authorities = new ArrayList<>();
                this.mAccountTypeToAuthorities.put(sa.accountType, authorities);
            }
            if (Log.isLoggable("AuthenticatorHelper", 2)) {
                Log.d("AuthenticatorHelper", "Added authority " + sa.authority + " to accountType " + sa.accountType);
            }
            authorities.add(sa.authority);
        }
    }
}
