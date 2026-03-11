package com.android.settings.accounts;

import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.app.Activity;
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
import com.android.settings.enterprise.EnterprisePrivacyFeatureProvider;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.widget.FooterPreference;
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
    private EnterprisePrivacyFeatureProvider mFeatureProvider;
    private UserManager mUm;
    private UserHandle mUserHandle;
    private FooterPreference mEnterpriseDisclosurePreference = null;
    private final ArrayList<ProviderEntry> mProviderList = new ArrayList<>();
    private HashMap<String, ArrayList<String>> mAccountTypeToAuthorities = null;
    private Map<String, AuthenticatorDescription> mTypeToAuthDescription = new HashMap();

    private static class ProviderEntry implements Comparable<ProviderEntry> {
        private final CharSequence name;
        private final String type;

        ProviderEntry(CharSequence charSequence, String str) {
            this.name = charSequence;
            this.type = str;
        }

        @Override
        public int compareTo(ProviderEntry providerEntry) {
            if (this.name == null) {
                return -1;
            }
            if (providerEntry.name == null) {
                return 1;
            }
            return CharSequences.compareToIgnoreCase(this.name, providerEntry.name);
        }
    }

    @Override
    public int getMetricsCategory() {
        return 10;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Activity activity = getActivity();
        this.mFeatureProvider = FeatureFactory.getFactory(activity).getEnterprisePrivacyFeatureProvider(activity);
        addPreferencesFromResource(R.xml.add_account_settings);
        this.mAuthorities = getIntent().getStringArrayExtra("authorities");
        String[] stringArrayExtra = getIntent().getStringArrayExtra("account_types");
        if (stringArrayExtra != null) {
            this.mAccountTypesFilter = new HashSet<>();
            for (String str : stringArrayExtra) {
                this.mAccountTypesFilter.add(str);
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
        int i = 0;
        while (true) {
            boolean z = true;
            if (i >= this.mAuthDescs.length) {
                break;
            }
            String str = this.mAuthDescs[i].type;
            CharSequence labelForType = getLabelForType(str);
            ArrayList<String> authoritiesForAccountType = getAuthoritiesForAccountType(str);
            if (this.mAuthorities != null && this.mAuthorities.length > 0 && authoritiesForAccountType != null) {
                int i2 = 0;
                while (true) {
                    if (i2 < this.mAuthorities.length) {
                        if (authoritiesForAccountType.contains(this.mAuthorities[i2])) {
                            break;
                        } else {
                            i2++;
                        }
                    } else {
                        z = false;
                        break;
                    }
                }
            }
            if (z && this.mAccountTypesFilter != null && !this.mAccountTypesFilter.contains(str)) {
                z = false;
            }
            if (z) {
                this.mProviderList.add(new ProviderEntry(labelForType, str));
            } else if (Log.isLoggable("ChooseAccountActivity", 2)) {
                Log.v("ChooseAccountActivity", "Skipped pref " + ((Object) labelForType) + ": has no authority we need");
            }
            i++;
        }
        Context context = getPreferenceScreen().getContext();
        if (this.mProviderList.size() == 1) {
            RestrictedLockUtils.EnforcedAdmin enforcedAdminCheckIfAccountManagementDisabled = RestrictedLockUtils.checkIfAccountManagementDisabled(context, this.mProviderList.get(0).type, this.mUserHandle.getIdentifier());
            if (enforcedAdminCheckIfAccountManagementDisabled == null) {
                finishWithAccountType(this.mProviderList.get(0).type);
                return;
            } else {
                setResult(0, RestrictedLockUtils.getShowAdminSupportDetailsIntent(context, enforcedAdminCheckIfAccountManagementDisabled));
                finish();
                return;
            }
        }
        if (this.mProviderList.size() > 0) {
            Collections.sort(this.mProviderList);
            this.mAddAccountGroup.removeAll();
            for (ProviderEntry providerEntry : this.mProviderList) {
                ProviderPreference providerPreference = new ProviderPreference(getPreferenceScreen().getContext(), providerEntry.type, getDrawableForType(providerEntry.type), providerEntry.name);
                providerPreference.checkAccountManagementAndSetDisabled(this.mUserHandle.getIdentifier());
                this.mAddAccountGroup.addPreference(providerPreference);
            }
            addEnterpriseDisclosure();
            return;
        }
        if (Log.isLoggable("ChooseAccountActivity", 2)) {
            StringBuilder sb = new StringBuilder();
            for (String str2 : this.mAuthorities) {
                sb.append(str2);
                sb.append(' ');
            }
            Log.v("ChooseAccountActivity", "No providers found for authorities: " + ((Object) sb));
        }
        setResult(0);
        finish();
    }

    private void addEnterpriseDisclosure() {
        CharSequence deviceOwnerDisclosure = this.mFeatureProvider.getDeviceOwnerDisclosure();
        if (deviceOwnerDisclosure == null) {
            return;
        }
        if (this.mEnterpriseDisclosurePreference == null) {
            this.mEnterpriseDisclosurePreference = this.mFooterPreferenceMixin.createFooterPreference();
            this.mEnterpriseDisclosurePreference.setSelectable(false);
        }
        this.mEnterpriseDisclosurePreference.setTitle(deviceOwnerDisclosure);
        this.mAddAccountGroup.addPreference(this.mEnterpriseDisclosurePreference);
    }

    public ArrayList<String> getAuthoritiesForAccountType(String str) {
        if (this.mAccountTypeToAuthorities == null) {
            this.mAccountTypeToAuthorities = Maps.newHashMap();
            for (SyncAdapterType syncAdapterType : ContentResolver.getSyncAdapterTypesAsUser(this.mUserHandle.getIdentifier())) {
                ArrayList<String> arrayList = this.mAccountTypeToAuthorities.get(syncAdapterType.accountType);
                if (arrayList == null) {
                    arrayList = new ArrayList<>();
                    this.mAccountTypeToAuthorities.put(syncAdapterType.accountType, arrayList);
                }
                if (Log.isLoggable("ChooseAccountActivity", 2)) {
                    Log.d("ChooseAccountActivity", "added authority " + syncAdapterType.authority + " to accountType " + syncAdapterType.accountType);
                }
                arrayList.add(syncAdapterType.authority);
            }
        }
        return this.mAccountTypeToAuthorities.get(str);
    }

    protected Drawable getDrawableForType(String str) {
        Drawable userBadgedIcon;
        if (this.mTypeToAuthDescription.containsKey(str)) {
            try {
                AuthenticatorDescription authenticatorDescription = this.mTypeToAuthDescription.get(str);
                userBadgedIcon = getPackageManager().getUserBadgedIcon(getActivity().createPackageContextAsUser(authenticatorDescription.packageName, 0, this.mUserHandle).getDrawable(authenticatorDescription.iconId), this.mUserHandle);
            } catch (PackageManager.NameNotFoundException e) {
                Log.w("ChooseAccountActivity", "No icon name for account type " + str);
                userBadgedIcon = null;
            } catch (Resources.NotFoundException e2) {
                Log.w("ChooseAccountActivity", "No icon resource for account type " + str);
                userBadgedIcon = null;
            }
        } else {
            userBadgedIcon = null;
        }
        if (userBadgedIcon != null) {
            return userBadgedIcon;
        }
        return getPackageManager().getDefaultActivityIcon();
    }

    protected CharSequence getLabelForType(String str) {
        if (this.mTypeToAuthDescription.containsKey(str)) {
            try {
                AuthenticatorDescription authenticatorDescription = this.mTypeToAuthDescription.get(str);
                return getActivity().createPackageContextAsUser(authenticatorDescription.packageName, 0, this.mUserHandle).getResources().getText(authenticatorDescription.labelId);
            } catch (PackageManager.NameNotFoundException e) {
                Log.w("ChooseAccountActivity", "No label name for account type " + str);
            } catch (Resources.NotFoundException e2) {
                Log.w("ChooseAccountActivity", "No label resource for account type " + str);
            }
        }
        return null;
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference instanceof ProviderPreference) {
            ProviderPreference providerPreference = (ProviderPreference) preference;
            if (Log.isLoggable("ChooseAccountActivity", 2)) {
                Log.v("ChooseAccountActivity", "Attempting to add account of type " + providerPreference.getAccountType());
            }
            finishWithAccountType(providerPreference.getAccountType());
            return true;
        }
        return true;
    }

    private void finishWithAccountType(String str) {
        Intent intent = new Intent();
        intent.putExtra("selected_account", str);
        intent.putExtra("android.intent.extra.USER", this.mUserHandle);
        setResult(-1, intent);
        finish();
    }
}
