package com.android.settings.accounts;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncAdapterType;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.SubSettingLauncher;
import com.android.settingslib.accounts.AuthenticatorHelper;
import com.android.settingslib.core.AbstractPreferenceController;

public class AccountSyncPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin, AuthenticatorHelper.OnAccountsUpdateListener {
    private Account mAccount;
    private Preference mPreference;
    private UserHandle mUserHandle;

    public AccountSyncPreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!"account_sync".equals(preference.getKey())) {
            return false;
        }
        Bundle bundle = new Bundle();
        bundle.putParcelable("account", this.mAccount);
        bundle.putParcelable("android.intent.extra.USER", this.mUserHandle);
        new SubSettingLauncher(this.mContext).setDestination(AccountSyncSettings.class.getName()).setArguments(bundle).setSourceMetricsCategory(8).setTitle(R.string.account_sync_title).launch();
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return "account_sync";
    }

    @Override
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mPreference = preferenceScreen.findPreference(getPreferenceKey());
    }

    @Override
    public void updateState(Preference preference) {
        updateSummary(preference);
    }

    @Override
    public void onAccountsUpdate(UserHandle userHandle) {
        updateSummary(this.mPreference);
    }

    public void init(Account account, UserHandle userHandle) {
        this.mAccount = account;
        this.mUserHandle = userHandle;
    }

    void updateSummary(Preference preference) {
        int i;
        int i2;
        if (this.mAccount == null) {
            return;
        }
        int identifier = this.mUserHandle.getIdentifier();
        SyncAdapterType[] syncAdapterTypesAsUser = ContentResolver.getSyncAdapterTypesAsUser(identifier);
        if (syncAdapterTypesAsUser != null) {
            i = 0;
            i2 = 0;
            for (SyncAdapterType syncAdapterType : syncAdapterTypesAsUser) {
                if (syncAdapterType.accountType.equals(this.mAccount.type) && syncAdapterType.isUserVisible() && ContentResolver.getIsSyncableAsUser(this.mAccount, syncAdapterType.authority, identifier) > 0) {
                    i2++;
                    boolean syncAutomaticallyAsUser = ContentResolver.getSyncAutomaticallyAsUser(this.mAccount, syncAdapterType.authority, identifier);
                    if ((!ContentResolver.getMasterSyncAutomaticallyAsUser(identifier)) || syncAutomaticallyAsUser) {
                        i++;
                    }
                }
            }
        } else {
            i = 0;
            i2 = 0;
        }
        if (i == 0) {
            preference.setSummary(R.string.account_sync_summary_all_off);
        } else if (i == i2) {
            preference.setSummary(R.string.account_sync_summary_all_on);
        } else {
            preference.setSummary(this.mContext.getString(R.string.account_sync_summary_some_on, Integer.valueOf(i), Integer.valueOf(i2)));
        }
    }
}
