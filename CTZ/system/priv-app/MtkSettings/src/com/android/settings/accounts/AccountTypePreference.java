package com.android.settings.accounts;

import android.accounts.Account;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import com.android.settings.Utils;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.widget.AppPreference;

/* loaded from: classes.dex */
public class AccountTypePreference extends AppPreference implements Preference.OnPreferenceClickListener {
    private final String mFragment;
    private final Bundle mFragmentArguments;
    private final int mMetricsCategory;
    private final CharSequence mSummary;
    private final CharSequence mTitle;
    private final int mTitleResId;
    private final String mTitleResPackageName;

    public AccountTypePreference(Context context, int i, Account account, String str, int i2, CharSequence charSequence, String str2, Bundle bundle, Drawable drawable) {
        super(context);
        this.mTitle = account.name;
        this.mTitleResPackageName = str;
        this.mTitleResId = i2;
        this.mSummary = charSequence;
        this.mFragment = str2;
        this.mFragmentArguments = bundle;
        this.mMetricsCategory = i;
        setKey(buildKey(account));
        setTitle(this.mTitle);
        setSummary(charSequence);
        setIcon(drawable);
        setOnPreferenceClickListener(this);
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceClickListener
    public boolean onPreferenceClick(Preference preference) {
        if (this.mFragment != null) {
            UserManager userManager = (UserManager) getContext().getSystemService("user");
            UserHandle userHandle = (UserHandle) this.mFragmentArguments.getParcelable("android.intent.extra.USER");
            if (userHandle != null && Utils.startQuietModeDialogIfNecessary(getContext(), userManager, userHandle.getIdentifier())) {
                return true;
            }
            if (userHandle != null && Utils.unlockWorkProfileIfNecessary(getContext(), userHandle.getIdentifier())) {
                return true;
            }
            new SubSettingLauncher(getContext()).setDestination(this.mFragment).setArguments(this.mFragmentArguments).setTitle(this.mTitleResPackageName, this.mTitleResId).setSourceMetricsCategory(this.mMetricsCategory).launch();
            return true;
        }
        return false;
    }

    public static String buildKey(Account account) {
        return String.valueOf(account.hashCode());
    }

    @Override // android.support.v7.preference.Preference
    public CharSequence getTitle() {
        return this.mTitle;
    }

    @Override // android.support.v7.preference.Preference
    public CharSequence getSummary() {
        return this.mSummary;
    }
}
