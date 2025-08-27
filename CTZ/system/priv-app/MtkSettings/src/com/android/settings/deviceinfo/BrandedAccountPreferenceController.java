package com.android.settings.deviceinfo;

import android.accounts.Account;
import android.content.Context;
import android.os.Bundle;
import android.os.Process;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.R;
import com.android.settings.accounts.AccountDetailDashboardFragment;
import com.android.settings.accounts.AccountFeatureProvider;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.overlay.FeatureFactory;

/* loaded from: classes.dex */
public class BrandedAccountPreferenceController extends BasePreferenceController {
    private static final String KEY_PREFERENCE_TITLE = "branded_account";
    private final Account[] mAccounts;

    public BrandedAccountPreferenceController(Context context) {
        super(context, KEY_PREFERENCE_TITLE);
        this.mAccounts = FeatureFactory.getFactory(this.mContext).getAccountFeatureProvider().getAccounts(this.mContext);
    }

    @Override // com.android.settings.core.BasePreferenceController
    public int getAvailabilityStatus() {
        if (this.mAccounts != null && this.mAccounts.length > 0) {
            return 0;
        }
        return 3;
    }

    @Override // com.android.settings.core.BasePreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        final AccountFeatureProvider accountFeatureProvider = FeatureFactory.getFactory(this.mContext).getAccountFeatureProvider();
        Preference preferenceFindPreference = preferenceScreen.findPreference(KEY_PREFERENCE_TITLE);
        if (preferenceFindPreference != null && (this.mAccounts == null || this.mAccounts.length == 0)) {
            preferenceScreen.removePreference(preferenceFindPreference);
        } else {
            preferenceFindPreference.setSummary(this.mAccounts[0].name);
            preferenceFindPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() { // from class: com.android.settings.deviceinfo.-$$Lambda$BrandedAccountPreferenceController$rFwl4JPEzufcbKCkFgByL5d4NMI
                @Override // android.support.v7.preference.Preference.OnPreferenceClickListener
                public final boolean onPreferenceClick(Preference preference) {
                    return BrandedAccountPreferenceController.lambda$displayPreference$0(this.f$0, accountFeatureProvider, preference);
                }
            });
        }
    }

    public static /* synthetic */ boolean lambda$displayPreference$0(BrandedAccountPreferenceController brandedAccountPreferenceController, AccountFeatureProvider accountFeatureProvider, Preference preference) {
        Bundle bundle = new Bundle();
        bundle.putParcelable("account", brandedAccountPreferenceController.mAccounts[0]);
        bundle.putParcelable("user_handle", Process.myUserHandle());
        bundle.putString("account_type", accountFeatureProvider.getAccountType());
        new SubSettingLauncher(brandedAccountPreferenceController.mContext).setDestination(AccountDetailDashboardFragment.class.getName()).setTitle(R.string.account_sync_title).setArguments(bundle).setSourceMetricsCategory(40).launch();
        return true;
    }
}
