package com.android.server.telecom.settings;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import com.android.server.telecom.R;
import java.util.Iterator;
import java.util.List;

public class EnableAccountPreferenceFragment extends PreferenceFragment {
    private TelecomManager mTelecomManager;

    private final class AccountSwitchPreference extends SwitchPreference {
        private final PhoneAccount mAccount;

        public AccountSwitchPreference(Context context, PhoneAccount phoneAccount) {
            super(context);
            this.mAccount = phoneAccount;
            setTitle(phoneAccount.getLabel());
            setSummary(phoneAccount.getShortDescription());
            Icon icon = phoneAccount.getIcon();
            if (icon != null) {
                setIcon(icon.loadDrawable(context));
            }
            setChecked(phoneAccount.isEnabled());
        }

        @Override
        protected void onClick() {
            super.onClick();
            EnableAccountPreferenceFragment.this.mTelecomManager.enablePhoneAccount(this.mAccount.getAccountHandle(), isChecked());
        }
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mTelecomManager = TelecomManager.from(getActivity());
    }

    @Override
    public void onResume() {
        boolean z;
        super.onResume();
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        if (preferenceScreen != null) {
            preferenceScreen.removeAll();
        }
        addPreferencesFromResource(R.xml.enable_account_preference);
        PreferenceScreen preferenceScreen2 = getPreferenceScreen();
        List callCapablePhoneAccounts = this.mTelecomManager.getCallCapablePhoneAccounts(true);
        Activity activity = getActivity();
        Iterator it = callCapablePhoneAccounts.iterator();
        while (it.hasNext()) {
            PhoneAccount phoneAccount = this.mTelecomManager.getPhoneAccount((PhoneAccountHandle) it.next());
            if (phoneAccount != null) {
                if ((phoneAccount.getCapabilities() & 4) == 0) {
                    z = false;
                } else {
                    z = true;
                }
                if (!z) {
                    preferenceScreen2.addPreference(new AccountSwitchPreference(activity, phoneAccount));
                }
            }
        }
    }
}
