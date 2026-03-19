package com.android.settings.accounts;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.text.BidiFormatter;
import android.util.ArrayMap;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.AccessiblePreferenceCategory;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.accounts.AuthenticatorHelper;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AccountPreferenceController extends AbstractPreferenceController implements Preference.OnPreferenceClickListener, PreferenceControllerMixin, AuthenticatorHelper.OnAccountsUpdateListener, LifecycleObserver, OnPause, OnResume {
    private int mAccountProfileOrder;
    private String[] mAuthorities;
    private int mAuthoritiesCount;
    private AccountRestrictionHelper mHelper;
    private ManagedProfileBroadcastReceiver mManagedProfileBroadcastReceiver;
    private MetricsFeatureProvider mMetricsFeatureProvider;
    private SettingsPreferenceFragment mParent;
    private Preference mProfileNotAvailablePreference;
    private SparseArray<ProfileData> mProfiles;
    private UserManager mUm;

    public static class ProfileData {
        public ArrayMap<String, AccountTypePreference> accountPreferences = new ArrayMap<>();
        public RestrictedPreference addAccountPreference;
        public AuthenticatorHelper authenticatorHelper;
        public Preference managedProfilePreference;
        public boolean pendingRemoval;
        public PreferenceGroup preferenceGroup;
        public RestrictedPreference removeWorkProfilePreference;
        public UserInfo userInfo;
    }

    public AccountPreferenceController(Context context, SettingsPreferenceFragment settingsPreferenceFragment, String[] strArr) {
        this(context, settingsPreferenceFragment, strArr, new AccountRestrictionHelper(context));
    }

    @VisibleForTesting
    AccountPreferenceController(Context context, SettingsPreferenceFragment settingsPreferenceFragment, String[] strArr, AccountRestrictionHelper accountRestrictionHelper) {
        super(context);
        this.mProfiles = new SparseArray<>();
        this.mManagedProfileBroadcastReceiver = new ManagedProfileBroadcastReceiver();
        this.mAuthoritiesCount = 0;
        this.mAccountProfileOrder = 1;
        this.mUm = (UserManager) context.getSystemService("user");
        this.mAuthorities = strArr;
        this.mParent = settingsPreferenceFragment;
        if (this.mAuthorities != null) {
            this.mAuthoritiesCount = this.mAuthorities.length;
        }
        this.mMetricsFeatureProvider = FeatureFactory.getFactory(this.mContext).getMetricsFeatureProvider();
        this.mHelper = accountRestrictionHelper;
    }

    @Override
    public boolean isAvailable() {
        return !this.mUm.isManagedProfile();
    }

    @Override
    public String getPreferenceKey() {
        return null;
    }

    @Override
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        updateUi();
    }

    @Override
    public void onResume() {
        updateUi();
        this.mManagedProfileBroadcastReceiver.register(this.mContext);
        listenToAccountUpdates();
    }

    @Override
    public void onPause() {
        stopListeningToAccountUpdates();
        this.mManagedProfileBroadcastReceiver.unregister(this.mContext);
    }

    @Override
    public void onAccountsUpdate(UserHandle userHandle) {
        ProfileData profileData = this.mProfiles.get(userHandle.getIdentifier());
        if (profileData != null) {
            updateAccountTypes(profileData);
            return;
        }
        Log.w("AccountPrefController", "Missing Settings screen for: " + userHandle.getIdentifier());
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        int size = this.mProfiles.size();
        for (int i = 0; i < size; i++) {
            ProfileData profileDataValueAt = this.mProfiles.valueAt(i);
            if (preference == profileDataValueAt.addAccountPreference) {
                Intent intent = new Intent("android.settings.ADD_ACCOUNT_SETTINGS");
                intent.putExtra("android.intent.extra.USER", profileDataValueAt.userInfo.getUserHandle());
                intent.putExtra("authorities", this.mAuthorities);
                this.mContext.startActivity(intent);
                return true;
            }
            if (preference == profileDataValueAt.removeWorkProfilePreference) {
                RemoveUserFragment.newInstance(profileDataValueAt.userInfo.id).show(this.mParent.getFragmentManager(), "removeUser");
                return true;
            }
            if (preference == profileDataValueAt.managedProfilePreference) {
                Bundle bundle = new Bundle();
                bundle.putParcelable("android.intent.extra.USER", profileDataValueAt.userInfo.getUserHandle());
                new SubSettingLauncher(this.mContext).setSourceMetricsCategory(this.mParent.getMetricsCategory()).setDestination(ManagedProfileSettings.class.getName()).setTitle(R.string.managed_profile_settings_title).setArguments(bundle).launch();
                return true;
            }
        }
        return false;
    }

    private void updateUi() {
        if (!isAvailable()) {
            Log.e("AccountPrefController", "We should not be showing settings for a managed profile");
            return;
        }
        int size = this.mProfiles.size();
        for (int i = 0; i < size; i++) {
            this.mProfiles.valueAt(i).pendingRemoval = true;
        }
        if (this.mUm.isRestrictedProfile()) {
            updateProfileUi(this.mUm.getUserInfo(UserHandle.myUserId()));
        } else {
            List profiles = this.mUm.getProfiles(UserHandle.myUserId());
            int size2 = profiles.size();
            for (int i2 = 0; i2 < size2; i2++) {
                updateProfileUi((UserInfo) profiles.get(i2));
            }
        }
        cleanUpPreferences();
        int size3 = this.mProfiles.size();
        for (int i3 = 0; i3 < size3; i3++) {
            updateAccountTypes(this.mProfiles.valueAt(i3));
        }
    }

    private void updateProfileUi(UserInfo userInfo) {
        if (this.mParent.getPreferenceManager() == null) {
            return;
        }
        ProfileData profileData = this.mProfiles.get(userInfo.id);
        if (profileData != null) {
            profileData.pendingRemoval = false;
            if (userInfo.isEnabled()) {
                profileData.authenticatorHelper = new AuthenticatorHelper(this.mContext, userInfo.getUserHandle(), this);
                return;
            }
            return;
        }
        Context context = this.mContext;
        ProfileData profileData2 = new ProfileData();
        profileData2.userInfo = userInfo;
        AccessiblePreferenceCategory accessiblePreferenceCategoryCreateAccessiblePreferenceCategory = this.mHelper.createAccessiblePreferenceCategory(this.mParent.getPreferenceManager().getContext());
        int i = this.mAccountProfileOrder;
        this.mAccountProfileOrder = i + 1;
        accessiblePreferenceCategoryCreateAccessiblePreferenceCategory.setOrder(i);
        if (isSingleProfile()) {
            accessiblePreferenceCategoryCreateAccessiblePreferenceCategory.setTitle(context.getString(R.string.account_for_section_header, BidiFormatter.getInstance().unicodeWrap(userInfo.name)));
            accessiblePreferenceCategoryCreateAccessiblePreferenceCategory.setContentDescription(this.mContext.getString(R.string.account_settings));
        } else if (userInfo.isManagedProfile()) {
            accessiblePreferenceCategoryCreateAccessiblePreferenceCategory.setTitle(R.string.category_work);
            String workGroupSummary = getWorkGroupSummary(context, userInfo);
            accessiblePreferenceCategoryCreateAccessiblePreferenceCategory.setSummary(workGroupSummary);
            accessiblePreferenceCategoryCreateAccessiblePreferenceCategory.setContentDescription(this.mContext.getString(R.string.accessibility_category_work, workGroupSummary));
            profileData2.removeWorkProfilePreference = newRemoveWorkProfilePreference();
            this.mHelper.enforceRestrictionOnPreference(profileData2.removeWorkProfilePreference, "no_remove_managed_profile", UserHandle.myUserId());
            profileData2.managedProfilePreference = newManagedProfileSettings();
        } else {
            accessiblePreferenceCategoryCreateAccessiblePreferenceCategory.setTitle(R.string.category_personal);
            accessiblePreferenceCategoryCreateAccessiblePreferenceCategory.setContentDescription(this.mContext.getString(R.string.accessibility_category_personal));
        }
        PreferenceScreen preferenceScreen = this.mParent.getPreferenceScreen();
        if (preferenceScreen != null) {
            preferenceScreen.addPreference(accessiblePreferenceCategoryCreateAccessiblePreferenceCategory);
        }
        profileData2.preferenceGroup = accessiblePreferenceCategoryCreateAccessiblePreferenceCategory;
        if (userInfo.isEnabled()) {
            profileData2.authenticatorHelper = new AuthenticatorHelper(context, userInfo.getUserHandle(), this);
            profileData2.addAccountPreference = newAddAccountPreference();
            this.mHelper.enforceRestrictionOnPreference(profileData2.addAccountPreference, "no_modify_accounts", userInfo.id);
        }
        this.mProfiles.put(userInfo.id, profileData2);
    }

    private RestrictedPreference newAddAccountPreference() {
        RestrictedPreference restrictedPreference = new RestrictedPreference(this.mParent.getPreferenceManager().getContext());
        restrictedPreference.setTitle(R.string.add_account_label);
        restrictedPreference.setIcon(R.drawable.ic_menu_add);
        restrictedPreference.setOnPreferenceClickListener(this);
        restrictedPreference.setOrder(1000);
        return restrictedPreference;
    }

    private RestrictedPreference newRemoveWorkProfilePreference() {
        RestrictedPreference restrictedPreference = new RestrictedPreference(this.mParent.getPreferenceManager().getContext());
        restrictedPreference.setTitle(R.string.remove_managed_profile_label);
        restrictedPreference.setIcon(R.drawable.ic_delete);
        restrictedPreference.setOnPreferenceClickListener(this);
        restrictedPreference.setOrder(1002);
        return restrictedPreference;
    }

    private Preference newManagedProfileSettings() {
        Preference preference = new Preference(this.mParent.getPreferenceManager().getContext());
        preference.setTitle(R.string.managed_profile_settings_title);
        preference.setIcon(R.drawable.ic_settings_24dp);
        preference.setOnPreferenceClickListener(this);
        preference.setOrder(1001);
        return preference;
    }

    private String getWorkGroupSummary(Context context, UserInfo userInfo) {
        PackageManager packageManager = context.getPackageManager();
        ApplicationInfo adminApplicationInfo = Utils.getAdminApplicationInfo(context, userInfo.id);
        if (adminApplicationInfo == null) {
            return null;
        }
        return this.mContext.getString(R.string.managing_admin, packageManager.getApplicationLabel(adminApplicationInfo));
    }

    void cleanUpPreferences() {
        PreferenceScreen preferenceScreen = this.mParent.getPreferenceScreen();
        if (preferenceScreen == null) {
            return;
        }
        for (int size = this.mProfiles.size() - 1; size >= 0; size--) {
            ProfileData profileDataValueAt = this.mProfiles.valueAt(size);
            if (profileDataValueAt.pendingRemoval) {
                preferenceScreen.removePreference(profileDataValueAt.preferenceGroup);
                this.mProfiles.removeAt(size);
            }
        }
    }

    private void listenToAccountUpdates() {
        int size = this.mProfiles.size();
        for (int i = 0; i < size; i++) {
            AuthenticatorHelper authenticatorHelper = this.mProfiles.valueAt(i).authenticatorHelper;
            if (authenticatorHelper != null) {
                authenticatorHelper.listenToAccountUpdates();
            }
        }
    }

    private void stopListeningToAccountUpdates() {
        int size = this.mProfiles.size();
        for (int i = 0; i < size; i++) {
            AuthenticatorHelper authenticatorHelper = this.mProfiles.valueAt(i).authenticatorHelper;
            if (authenticatorHelper != null) {
                authenticatorHelper.stopListeningToAccountUpdates();
            }
        }
    }

    private void updateAccountTypes(ProfileData profileData) {
        if (this.mParent.getPreferenceManager() == null || profileData.preferenceGroup.getPreferenceManager() == null) {
            return;
        }
        if (profileData.userInfo.isEnabled()) {
            ArrayMap<String, AccountTypePreference> arrayMap = new ArrayMap<>(profileData.accountPreferences);
            ArrayList<AccountTypePreference> accountTypePreferences = getAccountTypePreferences(profileData.authenticatorHelper, profileData.userInfo.getUserHandle(), arrayMap);
            int size = accountTypePreferences.size();
            for (int i = 0; i < size; i++) {
                AccountTypePreference accountTypePreference = accountTypePreferences.get(i);
                accountTypePreference.setOrder(i);
                String key = accountTypePreference.getKey();
                if (!profileData.accountPreferences.containsKey(key)) {
                    profileData.preferenceGroup.addPreference(accountTypePreference);
                    profileData.accountPreferences.put(key, accountTypePreference);
                }
            }
            if (profileData.addAccountPreference != null) {
                profileData.preferenceGroup.addPreference(profileData.addAccountPreference);
            }
            for (String str : arrayMap.keySet()) {
                profileData.preferenceGroup.removePreference(profileData.accountPreferences.get(str));
                profileData.accountPreferences.remove(str);
            }
        } else {
            profileData.preferenceGroup.removeAll();
            if (this.mProfileNotAvailablePreference == null) {
                this.mProfileNotAvailablePreference = new Preference(this.mParent.getPreferenceManager().getContext());
            }
            this.mProfileNotAvailablePreference.setEnabled(false);
            this.mProfileNotAvailablePreference.setIcon(R.drawable.empty_icon);
            this.mProfileNotAvailablePreference.setTitle((CharSequence) null);
            this.mProfileNotAvailablePreference.setSummary(R.string.managed_profile_not_available_label);
            profileData.preferenceGroup.addPreference(this.mProfileNotAvailablePreference);
        }
        if (profileData.removeWorkProfilePreference != null) {
            profileData.preferenceGroup.addPreference(profileData.removeWorkProfilePreference);
        }
        if (profileData.managedProfilePreference != null) {
            profileData.preferenceGroup.addPreference(profileData.managedProfilePreference);
        }
    }

    private ArrayList<AccountTypePreference> getAccountTypePreferences(AuthenticatorHelper authenticatorHelper, UserHandle userHandle, ArrayMap<String, AccountTypePreference> arrayMap) {
        CharSequence labelForType;
        String[] strArr;
        String[] strArr2;
        int i;
        int i2;
        Account[] accountArr;
        int i3;
        UserHandle userHandle2 = userHandle;
        String[] enabledAccountTypes = authenticatorHelper.getEnabledAccountTypes();
        ArrayList<AccountTypePreference> arrayList = new ArrayList<>(enabledAccountTypes.length);
        int i4 = 0;
        while (i4 < enabledAccountTypes.length) {
            String str = enabledAccountTypes[i4];
            if (!accountTypeHasAnyRequestedAuthorities(authenticatorHelper, str) || (labelForType = authenticatorHelper.getLabelForType(this.mContext, str)) == null) {
                strArr = enabledAccountTypes;
            } else {
                String packageForType = authenticatorHelper.getPackageForType(str);
                int labelIdForType = authenticatorHelper.getLabelIdForType(str);
                Account[] accountsByTypeAsUser = AccountManager.get(this.mContext).getAccountsByTypeAsUser(str, userHandle2);
                Drawable drawableForType = authenticatorHelper.getDrawableForType(this.mContext, str);
                Context context = this.mParent.getPreferenceManager().getContext();
                int length = accountsByTypeAsUser.length;
                int i5 = 0;
                while (i5 < length) {
                    Account account = accountsByTypeAsUser[i5];
                    AccountTypePreference accountTypePreferenceRemove = arrayMap.remove(AccountTypePreference.buildKey(account));
                    if (accountTypePreferenceRemove != null) {
                        arrayList.add(accountTypePreferenceRemove);
                    } else {
                        if (AccountRestrictionHelper.showAccount(this.mAuthorities, authenticatorHelper.getAuthoritiesForAccountType(account.type))) {
                            Bundle bundle = new Bundle();
                            bundle.putParcelable("account", account);
                            bundle.putParcelable("user_handle", userHandle2);
                            bundle.putString("account_type", str);
                            strArr2 = enabledAccountTypes;
                            bundle.putString("account_label", labelForType.toString());
                            bundle.putInt("account_title_res", labelIdForType);
                            bundle.putParcelable("android.intent.extra.USER", userHandle2);
                            i = i5;
                            i2 = length;
                            accountArr = accountsByTypeAsUser;
                            i3 = labelIdForType;
                            arrayList.add(new AccountTypePreference(context, this.mMetricsFeatureProvider.getMetricsCategory(this.mParent), account, packageForType, labelIdForType, labelForType, AccountDetailDashboardFragment.class.getName(), bundle, drawableForType));
                        }
                        i5 = i + 1;
                        enabledAccountTypes = strArr2;
                        length = i2;
                        accountsByTypeAsUser = accountArr;
                        labelIdForType = i3;
                        userHandle2 = userHandle;
                    }
                    strArr2 = enabledAccountTypes;
                    i = i5;
                    i2 = length;
                    accountArr = accountsByTypeAsUser;
                    i3 = labelIdForType;
                    i5 = i + 1;
                    enabledAccountTypes = strArr2;
                    length = i2;
                    accountsByTypeAsUser = accountArr;
                    labelIdForType = i3;
                    userHandle2 = userHandle;
                }
                strArr = enabledAccountTypes;
                authenticatorHelper.preloadDrawableForType(this.mContext, str);
            }
            i4++;
            enabledAccountTypes = strArr;
            userHandle2 = userHandle;
        }
        Collections.sort(arrayList, new Comparator<AccountTypePreference>() {
            @Override
            public int compare(AccountTypePreference accountTypePreference, AccountTypePreference accountTypePreference2) {
                int iCompareTo = accountTypePreference.getSummary().toString().compareTo(accountTypePreference2.getSummary().toString());
                return iCompareTo != 0 ? iCompareTo : accountTypePreference.getTitle().toString().compareTo(accountTypePreference2.getTitle().toString());
            }
        });
        return arrayList;
    }

    private boolean accountTypeHasAnyRequestedAuthorities(AuthenticatorHelper authenticatorHelper, String str) {
        if (this.mAuthoritiesCount == 0) {
            return true;
        }
        ArrayList<String> authoritiesForAccountType = authenticatorHelper.getAuthoritiesForAccountType(str);
        if (authoritiesForAccountType == null) {
            Log.d("AccountPrefController", "No sync authorities for account type: " + str);
            return false;
        }
        for (int i = 0; i < this.mAuthoritiesCount; i++) {
            if (authoritiesForAccountType.contains(this.mAuthorities[i])) {
                return true;
            }
        }
        return false;
    }

    private boolean isSingleProfile() {
        return this.mUm.isLinkedUser() || this.mUm.getProfiles(UserHandle.myUserId()).size() == 1;
    }

    private class ManagedProfileBroadcastReceiver extends BroadcastReceiver {
        private boolean mListeningToManagedProfileEvents;

        private ManagedProfileBroadcastReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.v("AccountPrefController", "Received broadcast: " + action);
            if (action.equals("android.intent.action.MANAGED_PROFILE_REMOVED") || action.equals("android.intent.action.MANAGED_PROFILE_ADDED")) {
                AccountPreferenceController.this.stopListeningToAccountUpdates();
                AccountPreferenceController.this.updateUi();
                AccountPreferenceController.this.listenToAccountUpdates();
            } else {
                Log.w("AccountPrefController", "Cannot handle received broadcast: " + intent.getAction());
            }
        }

        public void register(Context context) {
            if (!this.mListeningToManagedProfileEvents) {
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction("android.intent.action.MANAGED_PROFILE_REMOVED");
                intentFilter.addAction("android.intent.action.MANAGED_PROFILE_ADDED");
                context.registerReceiver(this, intentFilter);
                this.mListeningToManagedProfileEvents = true;
            }
        }

        public void unregister(Context context) {
            if (this.mListeningToManagedProfileEvents) {
                context.unregisterReceiver(this);
                this.mListeningToManagedProfileEvents = false;
            }
        }
    }
}
