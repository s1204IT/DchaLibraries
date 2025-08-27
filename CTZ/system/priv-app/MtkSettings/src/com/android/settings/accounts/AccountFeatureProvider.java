package com.android.settings.accounts;

import android.accounts.Account;
import android.content.Context;
import android.util.FeatureFlagUtils;

/* loaded from: classes.dex */
public interface AccountFeatureProvider {
    String getAccountType();

    Account[] getAccounts(Context context);

    default boolean isAboutPhoneV2Enabled(Context context) {
        return FeatureFlagUtils.isEnabled(context, "settings_about_phone_v2");
    }
}
