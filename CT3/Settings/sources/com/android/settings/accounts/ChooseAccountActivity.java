package com.android.settings.accounts;

import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncAdapterType;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.util.Log;
import com.android.internal.util.CharSequences;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settingslib.RestrictedLockUtils;
import com.google.android.collect.Maps;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class ChooseAccountActivity extends SettingsPreferenceFragment {
    public HashSet<String> mAccountTypesFilter;
    private PreferenceGroup mAddAccountGroup;
    private AuthenticatorDescription[] mAuthDescs;
    private String[] mAuthorities;
    private UserManager mUm;
    private UserHandle mUserHandle;
    private final ArrayList<ProviderEntry> mProviderList = new ArrayList<>();
    private HashMap<String, ArrayList<String>> mAccountTypeToAuthorities = null;
    private Map<String, AuthenticatorDescription> mTypeToAuthDescription = new HashMap();

    private static class ProviderEntry implements Comparable<ProviderEntry> {
        private final CharSequence name;
        private final String type;

        ProviderEntry(CharSequence providerName, String accountType) {
            this.name = providerName;
            this.type = accountType;
        }

        @Override
        public int compareTo(ProviderEntry another) {
            if (this.name == null) {
                return -1;
            }
            if (another.name == null) {
                return 1;
            }
            return CharSequences.compareToIgnoreCase(this.name, another.name);
        }
    }

    @Override
    protected int getMetricsCategory() {
        return 10;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.add_account_settings);
        this.mAuthorities = getIntent().getStringArrayExtra("authorities");
        String[] accountTypesFilter = getIntent().getStringArrayExtra("account_types");
        if (accountTypesFilter != null) {
            this.mAccountTypesFilter = new HashSet<>();
            for (String accountType : accountTypesFilter) {
                this.mAccountTypesFilter.add(accountType);
            }
        }
        this.mAddAccountGroup = getPreferenceScreen();
        this.mUm = UserManager.get(getContext());
        this.mUserHandle = Utils.getSecureTargetUser(getActivity().getActivityToken(), this.mUm, null, getIntent().getExtras());
        updateAuthDescriptions();
    }

    private void updateAuthDescriptions() {
        this.mAuthDescs = AccountManager.get(getContext()).getAuthenticatorTypesAsUser(this.mUserHandle.getIdentifier());
        for (int i = 0; i < this.mAuthDescs.length; i++) {
            this.mTypeToAuthDescription.put(this.mAuthDescs[i].type, this.mAuthDescs[i]);
        }
        onAuthDescriptionsUpdated();
    }

    private void onAuthDescriptionsUpdated() {
        for (int i = 0; i < this.mAuthDescs.length; i++) {
            String accountType = this.mAuthDescs[i].type;
            CharSequence providerName = getLabelForType(accountType);
            ArrayList<String> accountAuths = getAuthoritiesForAccountType(accountType);
            boolean addAccountPref = true;
            if (this.mAuthorities != null && this.mAuthorities.length > 0 && accountAuths != null) {
                addAccountPref = false;
                int k = 0;
                while (true) {
                    if (k >= this.mAuthorities.length) {
                        break;
                    }
                    if (!accountAuths.contains(this.mAuthorities[k])) {
                        k++;
                    } else {
                        addAccountPref = true;
                        break;
                    }
                }
            }
            if (addAccountPref && this.mAccountTypesFilter != null && !this.mAccountTypesFilter.contains(accountType)) {
                addAccountPref = false;
            }
            if (addAccountPref) {
                this.mProviderList.add(new ProviderEntry(providerName, accountType));
            } else if (Log.isLoggable("ChooseAccountActivity", 2)) {
                Log.v("ChooseAccountActivity", "Skipped pref " + providerName + ": has no authority we need");
            }
        }
        Context context = getPreferenceScreen().getContext();
        if (this.mProviderList.size() == 1) {
            RestrictedLockUtils.EnforcedAdmin admin = RestrictedLockUtils.checkIfAccountManagementDisabled(context, this.mProviderList.get(0).type, this.mUserHandle.getIdentifier());
            if (admin != null) {
                setResult(0, RestrictedLockUtils.getShowAdminSupportDetailsIntent(context, admin));
                finish();
                return;
            } else {
                finishWithAccountType(this.mProviderList.get(0).type);
                return;
            }
        }
        if (this.mProviderList.size() > 0) {
            Collections.sort(this.mProviderList);
            this.mAddAccountGroup.removeAll();
            for (ProviderEntry pref : this.mProviderList) {
                Drawable drawable = getDrawableForType(pref.type);
                ProviderPreference p = new ProviderPreference(getPreferenceScreen().getContext(), pref.type, drawable, pref.name);
                p.checkAccountManagementAndSetDisabled(this.mUserHandle.getIdentifier());
                this.mAddAccountGroup.addPreference(p);
            }
            return;
        }
        if (Log.isLoggable("ChooseAccountActivity", 2)) {
            StringBuilder auths = new StringBuilder();
            for (String a : this.mAuthorities) {
                auths.append(a);
                auths.append(' ');
            }
            Log.v("ChooseAccountActivity", "No providers found for authorities: " + ((Object) auths));
        }
        setResult(0);
        finish();
    }

    public ArrayList<String> getAuthoritiesForAccountType(String type) {
        if (this.mAccountTypeToAuthorities == null) {
            this.mAccountTypeToAuthorities = Maps.newHashMap();
            SyncAdapterType[] syncAdapters = ContentResolver.getSyncAdapterTypesAsUser(this.mUserHandle.getIdentifier());
            for (SyncAdapterType sa : syncAdapters) {
                ArrayList<String> authorities = this.mAccountTypeToAuthorities.get(sa.accountType);
                if (authorities == null) {
                    authorities = new ArrayList<>();
                    this.mAccountTypeToAuthorities.put(sa.accountType, authorities);
                }
                if (Log.isLoggable("ChooseAccountActivity", 2)) {
                    Log.d("ChooseAccountActivity", "added authority " + sa.authority + " to accountType " + sa.accountType);
                }
                authorities.add(sa.authority);
            }
        }
        return this.mAccountTypeToAuthorities.get(type);
    }

    protected Drawable getDrawableForType(String accountType) {
        Drawable icon = null;
        if (this.mTypeToAuthDescription.containsKey(accountType)) {
            try {
                AuthenticatorDescription desc = this.mTypeToAuthDescription.get(accountType);
                Context authContext = getActivity().createPackageContextAsUser(desc.packageName, 0, this.mUserHandle);
                icon = getPackageManager().getUserBadgedIcon(authContext.getDrawable(desc.iconId), this.mUserHandle);
            } catch (PackageManager.NameNotFoundException e) {
                Log.w("ChooseAccountActivity", "No icon name for account type " + accountType);
            } catch (Resources.NotFoundException e2) {
                Log.w("ChooseAccountActivity", "No icon resource for account type " + accountType);
            }
        }
        if (icon != null) {
            return icon;
        }
        return getPackageManager().getDefaultActivityIcon();
    }

    protected CharSequence getLabelForType(String accountType) {
        if (!this.mTypeToAuthDescription.containsKey(accountType)) {
            return null;
        }
        try {
            AuthenticatorDescription desc = this.mTypeToAuthDescription.get(accountType);
            Context authContext = getActivity().createPackageContextAsUser(desc.packageName, 0, this.mUserHandle);
            CharSequence label = authContext.getResources().getText(desc.labelId);
            return label;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w("ChooseAccountActivity", "No label name for account type " + accountType);
            return null;
        } catch (Resources.NotFoundException e2) {
            Log.w("ChooseAccountActivity", "No label resource for account type " + accountType);
            return null;
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference instanceof ProviderPreference) {
            ProviderPreference pref = (ProviderPreference) preference;
            if (Log.isLoggable("ChooseAccountActivity", 2)) {
                Log.v("ChooseAccountActivity", "Attempting to add account of type " + pref.getAccountType());
            }
            finishWithAccountType(pref.getAccountType());
            return true;
        }
        return true;
    }

    private void finishWithAccountType(String accountType) {
        Intent intent = new Intent();
        intent.putExtra("selected_account", accountType);
        intent.putExtra("android.intent.extra.USER", this.mUserHandle);
        setResult(-1, intent);
        finish();
    }
}
