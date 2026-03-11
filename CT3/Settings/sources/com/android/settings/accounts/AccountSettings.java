package com.android.settings.accounts;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.SearchIndexableResource;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.android.settings.AccessiblePreferenceCategory;
import com.android.settings.DimmableIconPreference;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Index;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settings.users.UserDialogs;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.accounts.AuthenticatorHelper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AccountSettings extends SettingsPreferenceFragment implements AuthenticatorHelper.OnAccountsUpdateListener, Preference.OnPreferenceClickListener, Indexable {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean enabled) {
            SearchIndexableResource sir = new SearchIndexableResource(context);
            sir.xmlResId = R.xml.account_settings;
            return Arrays.asList(sir);
        }

        @Override
        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
            List<SearchIndexableRaw> result = new ArrayList<>();
            Resources res = context.getResources();
            String screenTitle = res.getString(R.string.account_settings_title);
            UserManager um = UserManager.get(context);
            List<UserInfo> profiles = um.getProfiles(UserHandle.myUserId());
            int profilesCount = profiles.size();
            for (int i = 0; i < profilesCount; i++) {
                UserInfo userInfo = profiles.get(i);
                if (userInfo.isEnabled()) {
                    if (!RestrictedLockUtils.hasBaseUserRestriction(context, "no_modify_accounts", userInfo.id)) {
                        SearchIndexableRaw data = new SearchIndexableRaw(context);
                        data.title = res.getString(R.string.add_account_label);
                        data.screenTitle = screenTitle;
                        result.add(data);
                    }
                    if (userInfo.isManagedProfile()) {
                        SearchIndexableRaw data2 = new SearchIndexableRaw(context);
                        data2.title = res.getString(R.string.remove_managed_profile_label);
                        data2.screenTitle = screenTitle;
                        result.add(data2);
                        SearchIndexableRaw data3 = new SearchIndexableRaw(context);
                        data3.title = res.getString(R.string.managed_profile_settings_title);
                        data3.screenTitle = screenTitle;
                        result.add(data3);
                    }
                }
            }
            return result;
        }
    };
    private String[] mAuthorities;
    private Preference mProfileNotAvailablePreference;
    private UserManager mUm;
    private SparseArray<ProfileData> mProfiles = new SparseArray<>();
    private ManagedProfileBroadcastReceiver mManagedProfileBroadcastReceiver = new ManagedProfileBroadcastReceiver(this, null);
    private int mAuthoritiesCount = 0;

    private static class ProfileData {
        public DimmableIconPreference addAccountPreference;
        public AuthenticatorHelper authenticatorHelper;
        public Preference managedProfilePreference;
        public PreferenceGroup preferenceGroup;
        public Preference removeWorkProfilePreference;
        public UserInfo userInfo;

        ProfileData(ProfileData profileData) {
            this();
        }

        private ProfileData() {
        }
    }

    @Override
    protected int getMetricsCategory() {
        return 8;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mUm = (UserManager) getSystemService("user");
        this.mProfileNotAvailablePreference = new Preference(getPrefContext());
        this.mAuthorities = getActivity().getIntent().getStringArrayExtra("authorities");
        if (this.mAuthorities != null) {
            this.mAuthoritiesCount = this.mAuthorities.length;
        }
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.account_settings, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        UserHandle currentProfile = Process.myUserHandle();
        if (this.mProfiles.size() == 1) {
            menu.findItem(R.id.account_settings_menu_auto_sync).setVisible(true).setOnMenuItemClickListener(new MasterSyncStateClickListener(currentProfile)).setChecked(ContentResolver.getMasterSyncAutomaticallyAsUser(currentProfile.getIdentifier()));
            menu.findItem(R.id.account_settings_menu_auto_sync_personal).setVisible(false);
            menu.findItem(R.id.account_settings_menu_auto_sync_work).setVisible(false);
        } else {
            if (this.mProfiles.size() > 1) {
                UserHandle managedProfile = this.mProfiles.valueAt(1).userInfo.getUserHandle();
                menu.findItem(R.id.account_settings_menu_auto_sync_personal).setVisible(true).setOnMenuItemClickListener(new MasterSyncStateClickListener(currentProfile)).setChecked(ContentResolver.getMasterSyncAutomaticallyAsUser(currentProfile.getIdentifier()));
                menu.findItem(R.id.account_settings_menu_auto_sync_work).setVisible(true).setOnMenuItemClickListener(new MasterSyncStateClickListener(managedProfile)).setChecked(ContentResolver.getMasterSyncAutomaticallyAsUser(managedProfile.getIdentifier()));
                menu.findItem(R.id.account_settings_menu_auto_sync).setVisible(false);
                return;
            }
            Log.w("AccountSettings", "Method onPrepareOptionsMenu called before mProfiles was initialized");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        cleanUpPreferences();
        updateUi();
        this.mManagedProfileBroadcastReceiver.register(getActivity());
        listenToAccountUpdates();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopListeningToAccountUpdates();
        this.mManagedProfileBroadcastReceiver.unregister(getActivity());
    }

    @Override
    public void onAccountsUpdate(UserHandle userHandle) {
        ProfileData profileData = this.mProfiles.get(userHandle.getIdentifier());
        if (profileData != null) {
            updateAccountTypes(profileData);
        } else {
            Log.w("AccountSettings", "Missing Settings screen for: " + userHandle.getIdentifier());
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        int count = this.mProfiles.size();
        for (int i = 0; i < count; i++) {
            ProfileData profileData = this.mProfiles.valueAt(i);
            if (preference == profileData.addAccountPreference) {
                Intent intent = new Intent("android.settings.ADD_ACCOUNT_SETTINGS");
                intent.putExtra("android.intent.extra.USER", profileData.userInfo.getUserHandle());
                intent.putExtra("authorities", this.mAuthorities);
                startActivity(intent);
                return true;
            }
            if (preference == profileData.removeWorkProfilePreference) {
                final int userId = profileData.userInfo.id;
                UserDialogs.createRemoveDialog(getActivity(), userId, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AccountSettings.this.mUm.removeUser(userId);
                    }
                }).show();
                return true;
            }
            if (preference == profileData.managedProfilePreference) {
                Bundle arguments = new Bundle();
                arguments.putParcelable("android.intent.extra.USER", profileData.userInfo.getUserHandle());
                ((SettingsActivity) getActivity()).startPreferencePanel(ManagedProfileSettings.class.getName(), arguments, R.string.managed_profile_settings_title, null, null, 0);
                return true;
            }
        }
        return false;
    }

    void updateUi() {
        addPreferencesFromResource(R.xml.account_settings);
        if (Utils.isManagedProfile(this.mUm)) {
            Log.e("AccountSettings", "We should not be showing settings for a managed profile");
            finish();
            return;
        }
        PreferenceScreen preferenceScreen = (PreferenceScreen) findPreference("account");
        if (this.mUm.isLinkedUser()) {
            UserInfo userInfo = this.mUm.getUserInfo(UserHandle.myUserId());
            updateProfileUi(userInfo, false, preferenceScreen);
        } else {
            List<UserInfo> profiles = this.mUm.getProfiles(UserHandle.myUserId());
            int profilesCount = profiles.size();
            boolean addCategory = profilesCount > 1;
            for (int i = 0; i < profilesCount; i++) {
                updateProfileUi(profiles.get(i), addCategory, preferenceScreen);
            }
        }
        int profilesCount2 = this.mProfiles.size();
        for (int i2 = 0; i2 < profilesCount2; i2++) {
            ProfileData profileData = this.mProfiles.valueAt(i2);
            if (!profileData.preferenceGroup.equals(preferenceScreen)) {
                preferenceScreen.addPreference(profileData.preferenceGroup);
            }
            updateAccountTypes(profileData);
        }
    }

    private void updateProfileUi(UserInfo userInfo, boolean addCategory, PreferenceScreen parent) {
        Context context = getActivity();
        ProfileData profileData = new ProfileData(null);
        profileData.userInfo = userInfo;
        if (addCategory) {
            profileData.preferenceGroup = new AccessiblePreferenceCategory(getPrefContext());
            if (userInfo.isManagedProfile()) {
                profileData.preferenceGroup.setLayoutResource(R.layout.work_profile_category);
                profileData.preferenceGroup.setTitle(R.string.category_work);
                String workGroupSummary = getWorkGroupSummary(context, userInfo);
                profileData.preferenceGroup.setSummary(workGroupSummary);
                ((AccessiblePreferenceCategory) profileData.preferenceGroup).setContentDescription(getString(R.string.accessibility_category_work, new Object[]{workGroupSummary}));
                profileData.removeWorkProfilePreference = newRemoveWorkProfilePreference(context);
                profileData.managedProfilePreference = newManagedProfileSettings();
            } else {
                profileData.preferenceGroup.setTitle(R.string.category_personal);
                ((AccessiblePreferenceCategory) profileData.preferenceGroup).setContentDescription(getString(R.string.accessibility_category_personal));
            }
            parent.addPreference(profileData.preferenceGroup);
        } else {
            profileData.preferenceGroup = parent;
        }
        if (userInfo.isEnabled()) {
            profileData.authenticatorHelper = new AuthenticatorHelper(context, userInfo.getUserHandle(), this);
            if (!RestrictedLockUtils.hasBaseUserRestriction(context, "no_modify_accounts", userInfo.id)) {
                profileData.addAccountPreference = newAddAccountPreference(context);
                profileData.addAccountPreference.checkRestrictionAndSetDisabled("no_modify_accounts", userInfo.id);
            }
        }
        this.mProfiles.put(userInfo.id, profileData);
        Index.getInstance(getActivity()).updateFromClassNameResource(AccountSettings.class.getName(), true, true);
    }

    private DimmableIconPreference newAddAccountPreference(Context context) {
        DimmableIconPreference preference = new DimmableIconPreference(getPrefContext());
        preference.setTitle(R.string.add_account_label);
        preference.setIcon(R.drawable.ic_menu_add);
        preference.setOnPreferenceClickListener(this);
        preference.setOrder(1000);
        return preference;
    }

    private Preference newRemoveWorkProfilePreference(Context context) {
        Preference preference = new Preference(getPrefContext());
        preference.setTitle(R.string.remove_managed_profile_label);
        preference.setIcon(R.drawable.ic_menu_delete);
        preference.setOnPreferenceClickListener(this);
        preference.setOrder(1002);
        return preference;
    }

    private Preference newManagedProfileSettings() {
        Preference preference = new Preference(getPrefContext());
        preference.setTitle(R.string.managed_profile_settings_title);
        preference.setIcon(R.drawable.ic_sysbar_quicksettings);
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
        CharSequence appLabel = packageManager.getApplicationLabel(adminApplicationInfo);
        return getString(R.string.managing_admin, new Object[]{appLabel});
    }

    public void cleanUpPreferences() {
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        if (preferenceScreen != null) {
            preferenceScreen.removeAll();
        }
        this.mProfiles.clear();
    }

    public void listenToAccountUpdates() {
        int count = this.mProfiles.size();
        for (int i = 0; i < count; i++) {
            AuthenticatorHelper authenticatorHelper = this.mProfiles.valueAt(i).authenticatorHelper;
            if (authenticatorHelper != null) {
                authenticatorHelper.listenToAccountUpdates();
            }
        }
    }

    public void stopListeningToAccountUpdates() {
        int count = this.mProfiles.size();
        for (int i = 0; i < count; i++) {
            AuthenticatorHelper authenticatorHelper = this.mProfiles.valueAt(i).authenticatorHelper;
            if (authenticatorHelper != null) {
                authenticatorHelper.stopListeningToAccountUpdates();
            }
        }
    }

    private void updateAccountTypes(ProfileData profileData) {
        profileData.preferenceGroup.removeAll();
        if (profileData.userInfo.isEnabled()) {
            ArrayList<AccountPreference> preferences = getAccountTypePreferences(profileData.authenticatorHelper, profileData.userInfo.getUserHandle());
            int count = preferences.size();
            for (int i = 0; i < count; i++) {
                profileData.preferenceGroup.addPreference(preferences.get(i));
            }
            if (profileData.addAccountPreference != null) {
                profileData.preferenceGroup.addPreference(profileData.addAccountPreference);
            }
        } else {
            this.mProfileNotAvailablePreference.setEnabled(false);
            this.mProfileNotAvailablePreference.setIcon(R.drawable.empty_icon);
            this.mProfileNotAvailablePreference.setTitle((CharSequence) null);
            this.mProfileNotAvailablePreference.setSummary(R.string.managed_profile_not_available_label);
            profileData.preferenceGroup.addPreference(this.mProfileNotAvailablePreference);
        }
        if (profileData.removeWorkProfilePreference != null) {
            profileData.preferenceGroup.addPreference(profileData.removeWorkProfilePreference);
        }
        if (profileData.managedProfilePreference == null) {
            return;
        }
        profileData.preferenceGroup.addPreference(profileData.managedProfilePreference);
    }

    private ArrayList<AccountPreference> getAccountTypePreferences(AuthenticatorHelper helper, UserHandle userHandle) {
        CharSequence label;
        String[] accountTypes = helper.getEnabledAccountTypes();
        ArrayList<AccountPreference> accountTypePreferences = new ArrayList<>(accountTypes.length);
        for (String accountType : accountTypes) {
            if (accountTypeHasAnyRequestedAuthorities(helper, accountType) && (label = helper.getLabelForType(getActivity(), accountType)) != null) {
                String titleResPackageName = helper.getPackageForType(accountType);
                int titleResId = helper.getLabelIdForType(accountType);
                Account[] accounts = AccountManager.get(getActivity()).getAccountsByTypeAsUser(accountType, userHandle);
                boolean skipToAccount = accounts.length == 1 && !helper.hasAccountPreferences(accountType);
                if (skipToAccount) {
                    Bundle fragmentArguments = new Bundle();
                    fragmentArguments.putParcelable("account", accounts[0]);
                    fragmentArguments.putParcelable("android.intent.extra.USER", userHandle);
                    accountTypePreferences.add(new AccountPreference(getPrefContext(), label, titleResPackageName, titleResId, AccountSyncSettings.class.getName(), fragmentArguments, helper.getDrawableForType(getActivity(), accountType)));
                } else {
                    Bundle fragmentArguments2 = new Bundle();
                    fragmentArguments2.putString("account_type", accountType);
                    fragmentArguments2.putString("account_label", label.toString());
                    fragmentArguments2.putParcelable("android.intent.extra.USER", userHandle);
                    accountTypePreferences.add(new AccountPreference(getPrefContext(), label, titleResPackageName, titleResId, ManageAccountsSettings.class.getName(), fragmentArguments2, helper.getDrawableForType(getActivity(), accountType)));
                }
                helper.preloadDrawableForType(getActivity(), accountType);
            }
        }
        Collections.sort(accountTypePreferences, new Comparator<AccountPreference>() {
            @Override
            public int compare(AccountPreference t1, AccountPreference t2) {
                return t1.mTitle.toString().compareTo(t2.mTitle.toString());
            }
        });
        return accountTypePreferences;
    }

    private boolean accountTypeHasAnyRequestedAuthorities(AuthenticatorHelper helper, String accountType) {
        if (this.mAuthoritiesCount == 0) {
            return true;
        }
        ArrayList<String> authoritiesForType = helper.getAuthoritiesForAccountType(accountType);
        if (authoritiesForType == null) {
            Log.d("AccountSettings", "No sync authorities for account type: " + accountType);
            return false;
        }
        for (int j = 0; j < this.mAuthoritiesCount; j++) {
            if (authoritiesForType.contains(this.mAuthorities[j])) {
                return true;
            }
        }
        return false;
    }

    private class AccountPreference extends Preference implements Preference.OnPreferenceClickListener {
        private final String mFragment;
        private final Bundle mFragmentArguments;
        private final CharSequence mTitle;
        private final int mTitleResId;
        private final String mTitleResPackageName;

        public AccountPreference(Context context, CharSequence title, String titleResPackageName, int titleResId, String fragment, Bundle fragmentArguments, Drawable icon) {
            super(context);
            this.mTitle = title;
            this.mTitleResPackageName = titleResPackageName;
            this.mTitleResId = titleResId;
            this.mFragment = fragment;
            this.mFragmentArguments = fragmentArguments;
            setWidgetLayoutResource(R.layout.account_type_preference);
            setTitle(title);
            setIcon(icon);
            setOnPreferenceClickListener(this);
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            if (this.mFragment == null) {
                return false;
            }
            Utils.startWithFragment(getContext(), this.mFragment, this.mFragmentArguments, (Fragment) null, 0, this.mTitleResPackageName, this.mTitleResId, (CharSequence) null);
            return true;
        }
    }

    private class ManagedProfileBroadcastReceiver extends BroadcastReceiver {
        private boolean listeningToManagedProfileEvents;

        ManagedProfileBroadcastReceiver(AccountSettings this$0, ManagedProfileBroadcastReceiver managedProfileBroadcastReceiver) {
            this();
        }

        private ManagedProfileBroadcastReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.v("AccountSettings", "Received broadcast: " + action);
            if (action.equals("android.intent.action.MANAGED_PROFILE_REMOVED") || action.equals("android.intent.action.MANAGED_PROFILE_ADDED")) {
                AccountSettings.this.stopListeningToAccountUpdates();
                AccountSettings.this.cleanUpPreferences();
                AccountSettings.this.updateUi();
                AccountSettings.this.listenToAccountUpdates();
                AccountSettings.this.getActivity().invalidateOptionsMenu();
                return;
            }
            Log.w("AccountSettings", "Cannot handle received broadcast: " + intent.getAction());
        }

        public void register(Context context) {
            if (this.listeningToManagedProfileEvents) {
                return;
            }
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.MANAGED_PROFILE_REMOVED");
            intentFilter.addAction("android.intent.action.MANAGED_PROFILE_ADDED");
            context.registerReceiver(this, intentFilter);
            this.listeningToManagedProfileEvents = true;
        }

        public void unregister(Context context) {
            if (!this.listeningToManagedProfileEvents) {
                return;
            }
            context.unregisterReceiver(this);
            this.listeningToManagedProfileEvents = false;
        }
    }

    private class MasterSyncStateClickListener implements MenuItem.OnMenuItemClickListener {
        private final UserHandle mUserHandle;

        public MasterSyncStateClickListener(UserHandle userHandle) {
            this.mUserHandle = userHandle;
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            if (ActivityManager.isUserAMonkey()) {
                Log.d("AccountSettings", "ignoring monkey's attempt to flip sync state");
            } else {
                ConfirmAutoSyncChangeFragment.show(AccountSettings.this, !item.isChecked(), this.mUserHandle);
            }
            return true;
        }
    }

    public static class ConfirmAutoSyncChangeFragment extends DialogFragment {
        private boolean mEnabling;
        private int mIdentifier;
        private UserHandle mUserHandle;

        public static void show(AccountSettings parent, boolean enabling, UserHandle userHandle) {
            if (parent.isAdded()) {
                ConfirmAutoSyncChangeFragment dialog = new ConfirmAutoSyncChangeFragment();
                dialog.mEnabling = enabling;
                dialog.mUserHandle = userHandle;
                dialog.mIdentifier = userHandle.getIdentifier();
                dialog.setTargetFragment(parent, 0);
                dialog.show(parent.getFragmentManager(), "confirmAutoSyncChange");
            }
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Context context = getActivity();
            if (savedInstanceState != null) {
                this.mEnabling = savedInstanceState.getBoolean("enabling");
                this.mUserHandle = (UserHandle) savedInstanceState.getParcelable("userHandle");
                this.mIdentifier = savedInstanceState.getInt("identifier");
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            if (!this.mEnabling) {
                builder.setTitle(R.string.data_usage_auto_sync_off_dialog_title);
                builder.setMessage(R.string.data_usage_auto_sync_off_dialog);
            } else {
                builder.setTitle(R.string.data_usage_auto_sync_on_dialog_title);
                builder.setMessage(R.string.data_usage_auto_sync_on_dialog);
            }
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ContentResolver.setMasterSyncAutomaticallyAsUser(ConfirmAutoSyncChangeFragment.this.mEnabling, ConfirmAutoSyncChangeFragment.this.mIdentifier);
                }
            });
            builder.setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null);
            return builder.create();
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putBoolean("enabling", this.mEnabling);
            outState.putParcelable("userHandle", this.mUserHandle);
            outState.putInt("identifier", this.mIdentifier);
        }
    }
}
