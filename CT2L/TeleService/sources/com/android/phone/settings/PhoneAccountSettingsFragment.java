package com.android.phone.settings;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.sip.SipManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.phone.R;
import com.android.phone.SubscriptionInfoHelper;
import com.android.phone.settings.AccountSelectionPreference;
import com.android.services.telephony.sip.SipAccountRegistry;
import com.android.services.telephony.sip.SipSharedPreferences;
import com.android.services.telephony.sip.SipUtil;
import java.util.List;

public class PhoneAccountSettingsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener, AccountSelectionPreference.AccountSelectionListener {
    private String LOG_TAG = PhoneAccountSettingsFragment.class.getSimpleName();
    private PreferenceCategory mAccountList;
    private Preference mConfigureCallAssistant;
    private AccountSelectionPreference mDefaultOutgoingAccount;
    private AccountSelectionPreference mSelectCallAssistant;
    private CheckBoxPreference mSipReceiveCallsPreference;
    private SipSharedPreferences mSipSharedPreferences;
    private SubscriptionManager mSubscriptionManager;
    private TelecomManager mTelecomManager;
    private ListPreference mUseSipCalling;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.mTelecomManager = TelecomManager.from(getActivity());
        this.mSubscriptionManager = SubscriptionManager.from(getActivity());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getPreferenceScreen() != null) {
            getPreferenceScreen().removeAll();
        }
        addPreferencesFromResource(R.xml.phone_account_settings);
        TelephonyManager telephonyManager = (TelephonyManager) getActivity().getSystemService("phone");
        this.mAccountList = (PreferenceCategory) getPreferenceScreen().findPreference("phone_accounts_accounts_list_category_key");
        TtyModeListPreference ttyModeListPreference = (TtyModeListPreference) getPreferenceScreen().findPreference(getResources().getString(R.string.tty_mode_key));
        if (telephonyManager.isMultiSimEnabled()) {
            initAccountList();
            ttyModeListPreference.init();
        } else {
            getPreferenceScreen().removePreference(this.mAccountList);
            getPreferenceScreen().removePreference(ttyModeListPreference);
        }
        this.mDefaultOutgoingAccount = (AccountSelectionPreference) getPreferenceScreen().findPreference("default_outgoing_account");
        if (this.mTelecomManager.hasMultipleCallCapableAccounts()) {
            this.mDefaultOutgoingAccount.setListener(this);
            updateDefaultOutgoingAccountsModel();
        } else {
            getPreferenceScreen().removePreference(this.mDefaultOutgoingAccount);
        }
        List<PhoneAccountHandle> simCallManagers = this.mTelecomManager.getSimCallManagers();
        PreferenceCategory callAssistantCategory = (PreferenceCategory) getPreferenceScreen().findPreference("phone_accounts_call_assistant_settings_category_key");
        if (simCallManagers.isEmpty()) {
            getPreferenceScreen().removePreference(callAssistantCategory);
        } else {
            this.mSelectCallAssistant = (AccountSelectionPreference) getPreferenceScreen().findPreference("wifi_calling_call_assistant_preference");
            this.mSelectCallAssistant.setListener(this);
            this.mSelectCallAssistant.setDialogTitle(R.string.wifi_calling_select_call_assistant_summary);
            updateCallAssistantModel();
            this.mConfigureCallAssistant = getPreferenceScreen().findPreference("wifi_calling_configure_call_assistant_preference");
            this.mConfigureCallAssistant.setOnPreferenceClickListener(this);
            updateConfigureCallAssistant();
        }
        if (SipUtil.isVoipSupported(getActivity())) {
            this.mSipSharedPreferences = new SipSharedPreferences(getActivity());
            this.mUseSipCalling = (ListPreference) getPreferenceScreen().findPreference("use_sip_calling_options_key");
            this.mUseSipCalling.setEntries(!SipManager.isSipWifiOnly(getActivity()) ? R.array.sip_call_options_wifi_only_entries : R.array.sip_call_options_entries);
            this.mUseSipCalling.setOnPreferenceChangeListener(this);
            int optionsValueIndex = this.mUseSipCalling.findIndexOfValue(this.mSipSharedPreferences.getSipCallOption());
            if (optionsValueIndex == -1) {
                this.mSipSharedPreferences.setSipCallOption(getResources().getString(R.string.sip_address_only));
                optionsValueIndex = this.mUseSipCalling.findIndexOfValue(this.mSipSharedPreferences.getSipCallOption());
            }
            this.mUseSipCalling.setValueIndex(optionsValueIndex);
            this.mUseSipCalling.setSummary(this.mUseSipCalling.getEntry());
            this.mSipReceiveCallsPreference = (CheckBoxPreference) getPreferenceScreen().findPreference("sip_receive_calls_key");
            this.mSipReceiveCallsPreference.setEnabled(SipUtil.isPhoneIdle(getActivity()));
            this.mSipReceiveCallsPreference.setChecked(this.mSipSharedPreferences.isReceivingCallsEnabled());
            this.mSipReceiveCallsPreference.setOnPreferenceChangeListener(this);
            return;
        }
        getPreferenceScreen().removePreference(getPreferenceScreen().findPreference("phone_accounts_sip_settings_category_key"));
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object objValue) {
        if (pref == this.mUseSipCalling) {
            String option = objValue.toString();
            this.mSipSharedPreferences.setSipCallOption(option);
            this.mUseSipCalling.setValueIndex(this.mUseSipCalling.findIndexOfValue(option));
            this.mUseSipCalling.setSummary(this.mUseSipCalling.getEntry());
            return true;
        }
        if (pref != this.mSipReceiveCallsPreference) {
            return false;
        }
        final boolean isEnabled = this.mSipReceiveCallsPreference.isChecked() ? false : true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                PhoneAccountSettingsFragment.this.handleSipReceiveCallsOption(isEnabled);
            }
        }).start();
        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference pref) {
        if (pref == this.mConfigureCallAssistant) {
            Intent intent = getConfigureCallAssistantIntent();
            if (intent != null) {
                PhoneAccountHandle handle = this.mTelecomManager.getSimCallManager();
                UserHandle userHandle = handle.getUserHandle();
                try {
                    if (userHandle != null) {
                        getActivity().startActivityAsUser(intent, userHandle);
                    } else {
                        startActivity(intent);
                    }
                } catch (ActivityNotFoundException e) {
                    Log.d(this.LOG_TAG, "Could not resolve call assistant configure intent: " + intent);
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean onAccountSelected(AccountSelectionPreference pref, PhoneAccountHandle account) {
        if (pref == this.mDefaultOutgoingAccount) {
            this.mTelecomManager.setUserSelectedOutgoingPhoneAccount(account);
            return true;
        }
        if (pref == this.mSelectCallAssistant) {
            this.mTelecomManager.setSimCallManager(account);
            return true;
        }
        return false;
    }

    @Override
    public void onAccountSelectionDialogShow(AccountSelectionPreference pref) {
        if (pref == this.mDefaultOutgoingAccount) {
            updateDefaultOutgoingAccountsModel();
        } else if (pref == this.mSelectCallAssistant) {
            updateCallAssistantModel();
            updateConfigureCallAssistant();
        }
    }

    @Override
    public void onAccountChanged(AccountSelectionPreference pref) {
        if (pref == this.mSelectCallAssistant) {
            updateConfigureCallAssistant();
        }
    }

    private synchronized void handleSipReceiveCallsOption(boolean isEnabled) {
        Context context = getActivity();
        if (context != null) {
            this.mSipSharedPreferences.setReceivingCallsEnabled(isEnabled);
            SipUtil.useSipToReceiveIncomingCalls(context, isEnabled);
            SipAccountRegistry sipAccountRegistry = SipAccountRegistry.getInstance();
            sipAccountRegistry.restartSipService(context);
        }
    }

    private void updateDefaultOutgoingAccountsModel() {
        this.mDefaultOutgoingAccount.setModel(this.mTelecomManager, this.mTelecomManager.getCallCapablePhoneAccounts(), this.mTelecomManager.getUserSelectedOutgoingPhoneAccount(), getString(R.string.phone_accounts_ask_every_time));
    }

    public void updateCallAssistantModel() {
        this.mSelectCallAssistant.setModel(this.mTelecomManager, this.mTelecomManager.getSimCallManagers(), this.mTelecomManager.getSimCallManager(), getString(R.string.wifi_calling_call_assistant_none));
    }

    private void updateConfigureCallAssistant() {
        boolean shouldShow = false;
        Intent intent = getConfigureCallAssistantIntent();
        if (intent != null && !getActivity().getPackageManager().queryIntentActivities(intent, 0).isEmpty()) {
            shouldShow = true;
        }
        PreferenceCategory callAssistantCategory = (PreferenceCategory) getPreferenceScreen().findPreference("phone_accounts_call_assistant_settings_category_key");
        if (shouldShow) {
            callAssistantCategory.addPreference(this.mConfigureCallAssistant);
        } else {
            callAssistantCategory.removePreference(this.mConfigureCallAssistant);
        }
    }

    private void initAccountList() {
        List<SubscriptionInfo> sil = this.mSubscriptionManager.getActiveSubscriptionInfoList();
        if (sil != null) {
            for (SubscriptionInfo subscription : sil) {
                CharSequence label = subscription.getDisplayName();
                Intent intent = new Intent("android.telecom.action.SHOW_CALL_SETTINGS");
                intent.setFlags(67108864);
                SubscriptionInfoHelper.addExtrasToIntent(intent, subscription);
                Preference accountPreference = new Preference(getActivity());
                accountPreference.setTitle(label);
                accountPreference.setIntent(intent);
                this.mAccountList.addPreference(accountPreference);
            }
        }
    }

    private Intent getConfigureCallAssistantIntent() {
        String packageName;
        PhoneAccountHandle handle = this.mTelecomManager.getSimCallManager();
        if (handle == null || (packageName = handle.getComponentName().getPackageName()) == null) {
            return null;
        }
        return new Intent("android.telecom.action.CONNECTION_SERVICE_CONFIGURE").addCategory("android.intent.category.DEFAULT").setPackage(packageName);
    }
}
