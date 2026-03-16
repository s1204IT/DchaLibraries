package com.android.settings.accounts;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.SyncAdapterType;
import android.content.SyncInfo;
import android.content.SyncStatusInfo;
import android.content.pm.ProviderInfo;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.Utils;
import com.google.android.collect.Lists;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class AccountSyncSettings extends AccountPreferenceBase {
    private Account mAccount;
    private TextView mErrorInfoView;
    private ImageView mProviderIcon;
    private TextView mProviderId;
    private TextView mUserId;
    private ArrayList<SyncStateSwitchPreference> mSwitches = new ArrayList<>();
    private ArrayList<SyncAdapterType> mInvisibleAdapters = Lists.newArrayList();

    @Override
    public PreferenceScreen addPreferencesForType(String str, PreferenceScreen preferenceScreen) {
        return super.addPreferencesForType(str, preferenceScreen);
    }

    @Override
    public ArrayList getAuthoritiesForAccountType(String str) {
        return super.getAuthoritiesForAccountType(str);
    }

    @Override
    public void updateAuthDescriptions() {
        super.updateAuthDescriptions();
    }

    @Override
    public Dialog onCreateDialog(int id) {
        if (id == 100) {
            Dialog dialog = new AlertDialog.Builder(getActivity()).setTitle(R.string.really_remove_account_title).setMessage(R.string.really_remove_account_message).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).setPositiveButton(R.string.remove_account_label, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog2, int which) {
                    Activity activity = AccountSyncSettings.this.getActivity();
                    AccountManager.get(activity).removeAccountAsUser(AccountSyncSettings.this.mAccount, activity, new AccountManagerCallback<Bundle>() {
                        @Override
                        public void run(AccountManagerFuture<Bundle> future) {
                            if (AccountSyncSettings.this.isResumed()) {
                                boolean failed = true;
                                try {
                                    if (future.getResult().getBoolean("booleanResult")) {
                                        failed = false;
                                    }
                                } catch (AuthenticatorException e) {
                                } catch (OperationCanceledException e2) {
                                } catch (IOException e3) {
                                }
                                if (failed && AccountSyncSettings.this.getActivity() != null && !AccountSyncSettings.this.getActivity().isFinishing()) {
                                    AccountSyncSettings.this.showDialog(101);
                                } else {
                                    AccountSyncSettings.this.finish();
                                }
                            }
                        }
                    }, null, AccountSyncSettings.this.mUserHandle);
                }
            }).create();
            return dialog;
        }
        if (id == 101) {
            Dialog dialog2 = new AlertDialog.Builder(getActivity()).setTitle(R.string.really_remove_account_title).setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null).setMessage(R.string.remove_account_failed).create();
            return dialog2;
        }
        if (id != 102) {
            return null;
        }
        Dialog dialog3 = new AlertDialog.Builder(getActivity()).setTitle(R.string.cant_sync_dialog_title).setMessage(R.string.cant_sync_dialog_message).setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null).create();
        return dialog3;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.account_sync_screen, container, false);
        ListView list = (ListView) view.findViewById(android.R.id.list);
        Utils.prepareCustomPreferencesList(container, view, list, false);
        initializeUi(view);
        return view;
    }

    protected void initializeUi(View rootView) {
        addPreferencesFromResource(R.xml.account_sync_settings);
        this.mErrorInfoView = (TextView) rootView.findViewById(R.id.sync_settings_error_info);
        this.mErrorInfoView.setVisibility(8);
        this.mUserId = (TextView) rootView.findViewById(R.id.user_id);
        this.mProviderId = (TextView) rootView.findViewById(R.id.provider_id);
        this.mProviderIcon = (ImageView) rootView.findViewById(R.id.provider_icon);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Bundle arguments = getArguments();
        if (arguments == null) {
            Log.e("AccountSettings", "No arguments provided when starting intent. ACCOUNT_KEY needed.");
            finish();
            return;
        }
        this.mAccount = (Account) arguments.getParcelable("account");
        if (!accountExists(this.mAccount)) {
            Log.e("AccountSettings", "Account provided does not exist: " + this.mAccount);
            finish();
        } else {
            if (Log.isLoggable("AccountSettings", 2)) {
                Log.v("AccountSettings", "Got account: " + this.mAccount);
            }
            this.mUserId.setText(this.mAccount.name);
            this.mProviderId.setText(this.mAccount.type);
        }
    }

    @Override
    public void onResume() {
        this.mAuthenticatorHelper.listenToAccountUpdates();
        updateAuthDescriptions();
        onAccountsUpdate(UserHandle.getCallingUserHandle());
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        this.mAuthenticatorHelper.stopListeningToAccountUpdates();
    }

    private void addSyncStateSwitch(Account account, String authority) {
        SyncStateSwitchPreference item = new SyncStateSwitchPreference(getActivity(), account, authority);
        item.setPersistent(false);
        ProviderInfo providerInfo = getPackageManager().resolveContentProviderAsUser(authority, 0, this.mUserHandle.getIdentifier());
        if (providerInfo != null) {
            CharSequence providerLabel = providerInfo.loadLabel(getPackageManager());
            if (TextUtils.isEmpty(providerLabel)) {
                Log.e("AccountSettings", "Provider needs a label for authority '" + authority + "'");
                return;
            }
            String title = getString(R.string.sync_item_title, new Object[]{providerLabel});
            item.setTitle(title);
            item.setKey(authority);
            this.mSwitches.add(item);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MenuItem syncNow = menu.add(0, 1, 0, getString(R.string.sync_menu_sync_now)).setIcon(R.drawable.ic_menu_refresh_holo_dark);
        MenuItem syncCancel = menu.add(0, 2, 0, getString(R.string.sync_menu_sync_cancel)).setIcon(android.R.drawable.ic_menu_close_clear_cancel);
        UserManager um = (UserManager) getSystemService("user");
        if (!um.hasUserRestriction("no_modify_accounts", this.mUserHandle)) {
            MenuItem removeAccount = menu.add(0, 3, 0, getString(R.string.remove_account_label)).setIcon(R.drawable.ic_menu_delete);
            removeAccount.setShowAsAction(4);
        }
        syncNow.setShowAsAction(4);
        syncCancel.setShowAsAction(4);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        boolean syncActive = !ContentResolver.getCurrentSyncsAsUser(this.mUserHandle.getIdentifier()).isEmpty();
        menu.findItem(1).setVisible(syncActive ? false : true);
        menu.findItem(2).setVisible(syncActive);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 1:
                startSyncForEnabledProviders();
                return true;
            case 2:
                cancelSyncForEnabledProviders();
                return true;
            case 3:
                showDialog(100);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferences, Preference preference) {
        if (preference instanceof SyncStateSwitchPreference) {
            SyncStateSwitchPreference syncPref = (SyncStateSwitchPreference) preference;
            String authority = syncPref.getAuthority();
            Account account = syncPref.getAccount();
            int userId = this.mUserHandle.getIdentifier();
            boolean syncAutomatically = ContentResolver.getSyncAutomaticallyAsUser(account, authority, userId);
            if (syncPref.isOneTimeSyncMode()) {
                requestOrCancelSync(account, authority, true);
                return true;
            }
            boolean syncOn = syncPref.isChecked();
            if (syncOn == syncAutomatically) {
                return true;
            }
            ContentResolver.setSyncAutomaticallyAsUser(account, authority, syncOn, userId);
            if (ContentResolver.getMasterSyncAutomaticallyAsUser(userId) && syncOn) {
                return true;
            }
            requestOrCancelSync(account, authority, syncOn);
            return true;
        }
        return super.onPreferenceTreeClick(preferences, preference);
    }

    private void startSyncForEnabledProviders() {
        requestOrCancelSyncForEnabledProviders(true);
        Activity activity = getActivity();
        if (activity != null) {
            activity.invalidateOptionsMenu();
        }
    }

    private void cancelSyncForEnabledProviders() {
        requestOrCancelSyncForEnabledProviders(false);
        Activity activity = getActivity();
        if (activity != null) {
            activity.invalidateOptionsMenu();
        }
    }

    private void requestOrCancelSyncForEnabledProviders(boolean startSync) {
        int count = getPreferenceScreen().getPreferenceCount();
        for (int i = 0; i < count; i++) {
            Preference pref = getPreferenceScreen().getPreference(i);
            if (pref instanceof SyncStateSwitchPreference) {
                SyncStateSwitchPreference syncPref = (SyncStateSwitchPreference) pref;
                if (syncPref.isChecked()) {
                    requestOrCancelSync(syncPref.getAccount(), syncPref.getAuthority(), startSync);
                }
            }
        }
        if (this.mAccount != null) {
            for (SyncAdapterType syncAdapter : this.mInvisibleAdapters) {
                requestOrCancelSync(this.mAccount, syncAdapter.authority, startSync);
            }
        }
    }

    private void requestOrCancelSync(Account account, String authority, boolean flag) {
        if (flag) {
            Bundle extras = new Bundle();
            extras.putBoolean("force", true);
            ContentResolver.requestSyncAsUser(account, authority, this.mUserHandle.getIdentifier(), extras);
            return;
        }
        ContentResolver.cancelSyncAsUser(account, authority, this.mUserHandle.getIdentifier());
    }

    private boolean isSyncing(List<SyncInfo> currentSyncs, Account account, String authority) {
        for (SyncInfo syncInfo : currentSyncs) {
            if (syncInfo.account.equals(account) && syncInfo.authority.equals(authority)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onSyncStateUpdated() {
        if (isResumed()) {
            setFeedsState();
            Activity activity = getActivity();
            if (activity != null) {
                activity.invalidateOptionsMenu();
            }
        }
    }

    private void setFeedsState() {
        Date date = new Date();
        int userId = this.mUserHandle.getIdentifier();
        List<SyncInfo> currentSyncs = ContentResolver.getCurrentSyncsAsUser(userId);
        boolean syncIsFailing = false;
        updateAccountSwitches();
        int count = getPreferenceScreen().getPreferenceCount();
        for (int i = 0; i < count; i++) {
            Preference pref = getPreferenceScreen().getPreference(i);
            if (pref instanceof SyncStateSwitchPreference) {
                SyncStateSwitchPreference syncPref = (SyncStateSwitchPreference) pref;
                String authority = syncPref.getAuthority();
                Account account = syncPref.getAccount();
                SyncStatusInfo status = ContentResolver.getSyncStatusAsUser(account, authority, userId);
                boolean syncEnabled = ContentResolver.getSyncAutomaticallyAsUser(account, authority, userId);
                boolean authorityIsPending = status == null ? false : status.pending;
                boolean initialSync = status == null ? false : status.initialize;
                boolean activelySyncing = isSyncing(currentSyncs, account, authority);
                boolean lastSyncFailed = (status == null || status.lastFailureTime == 0 || status.getLastFailureMesgAsInt(0) == 1) ? false : true;
                if (!syncEnabled) {
                    lastSyncFailed = false;
                }
                if (lastSyncFailed && !activelySyncing && !authorityIsPending) {
                    syncIsFailing = true;
                }
                if (Log.isLoggable("AccountSettings", 2)) {
                    Log.d("AccountSettings", "Update sync status: " + account + " " + authority + " active = " + activelySyncing + " pend =" + authorityIsPending);
                }
                long successEndTime = status == null ? 0L : status.lastSuccessTime;
                if (!syncEnabled) {
                    syncPref.setSummary(R.string.sync_disabled);
                } else if (activelySyncing) {
                    syncPref.setSummary(R.string.sync_in_progress);
                } else if (successEndTime != 0) {
                    date.setTime(successEndTime);
                    String timeString = formatSyncDate(date);
                    syncPref.setSummary(getResources().getString(R.string.last_synced, timeString));
                } else {
                    syncPref.setSummary("");
                }
                int syncState = ContentResolver.getIsSyncableAsUser(account, authority, userId);
                syncPref.setActive(activelySyncing && syncState >= 0 && !initialSync);
                syncPref.setPending(authorityIsPending && syncState >= 0 && !initialSync);
                syncPref.setFailed(lastSyncFailed);
                ConnectivityManager connManager = (ConnectivityManager) getSystemService("connectivity");
                boolean masterSyncAutomatically = ContentResolver.getMasterSyncAutomaticallyAsUser(userId);
                boolean backgroundDataEnabled = connManager.getBackgroundDataSetting();
                boolean oneTimeSyncMode = (masterSyncAutomatically && backgroundDataEnabled) ? false : true;
                syncPref.setOneTimeSyncMode(oneTimeSyncMode);
                syncPref.setChecked(oneTimeSyncMode || syncEnabled);
            }
        }
        this.mErrorInfoView.setVisibility(syncIsFailing ? 0 : 8);
    }

    @Override
    public void onAccountsUpdate(UserHandle userHandle) {
        super.onAccountsUpdate(userHandle);
        if (!accountExists(this.mAccount)) {
            finish();
        } else {
            updateAccountSwitches();
            onSyncStateUpdated();
        }
    }

    private boolean accountExists(Account account) {
        if (account == null) {
            return false;
        }
        Account[] accounts = AccountManager.get(getActivity()).getAccountsByTypeAsUser(account.type, this.mUserHandle);
        for (Account account2 : accounts) {
            if (account2.equals(account)) {
                return true;
            }
        }
        return false;
    }

    private void updateAccountSwitches() {
        this.mInvisibleAdapters.clear();
        SyncAdapterType[] syncAdapters = ContentResolver.getSyncAdapterTypesAsUser(this.mUserHandle.getIdentifier());
        ArrayList<String> authorities = new ArrayList<>();
        for (SyncAdapterType sa : syncAdapters) {
            if (sa.accountType.equals(this.mAccount.type)) {
                if (sa.isUserVisible()) {
                    if (Log.isLoggable("AccountSettings", 2)) {
                        Log.d("AccountSettings", "updateAccountSwitches: added authority " + sa.authority + " to accountType " + sa.accountType);
                    }
                    authorities.add(sa.authority);
                } else {
                    this.mInvisibleAdapters.add(sa);
                }
            }
        }
        int n = this.mSwitches.size();
        for (int i = 0; i < n; i++) {
            getPreferenceScreen().removePreference(this.mSwitches.get(i));
        }
        this.mSwitches.clear();
        if (Log.isLoggable("AccountSettings", 2)) {
            Log.d("AccountSettings", "looking for sync adapters that match account " + this.mAccount);
        }
        int m = authorities.size();
        for (int j = 0; j < m; j++) {
            String authority = authorities.get(j);
            int syncState = ContentResolver.getIsSyncableAsUser(this.mAccount, authority, this.mUserHandle.getIdentifier());
            if (Log.isLoggable("AccountSettings", 2)) {
                Log.d("AccountSettings", "  found authority " + authority + " " + syncState);
            }
            if (syncState > 0) {
                addSyncStateSwitch(this.mAccount, authority);
            }
        }
        Collections.sort(this.mSwitches);
        int n2 = this.mSwitches.size();
        for (int i2 = 0; i2 < n2; i2++) {
            getPreferenceScreen().addPreference(this.mSwitches.get(i2));
        }
    }

    @Override
    protected void onAuthDescriptionsUpdated() {
        super.onAuthDescriptionsUpdated();
        getPreferenceScreen().removeAll();
        if (this.mAccount != null) {
            this.mProviderIcon.setImageDrawable(getDrawableForType(this.mAccount.type));
            this.mProviderId.setText(getLabelForType(this.mAccount.type));
        }
        addPreferencesFromResource(R.xml.account_sync_settings);
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_accounts;
    }
}
