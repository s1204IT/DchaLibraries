package com.android.phone.settings;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Icon;
import android.net.sip.SipManager;
import android.os.Bundle;
import android.os.UserManager;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.telephony.Phone;
import com.android.phone.PhoneUtils;
import com.android.phone.R;
import com.android.phone.SubscriptionInfoHelper;
import com.android.phone.settings.AccountSelectionPreference;
import com.android.services.telephony.sip.SipAccountRegistry;
import com.android.services.telephony.sip.SipPreferences;
import com.android.services.telephony.sip.SipUtil;
import com.mediatek.phone.ext.ExtensionManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class PhoneAccountSettingsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener, AccountSelectionPreference.AccountSelectionListener {
    private static final String LOG_TAG = PhoneAccountSettingsFragment.class.getSimpleName();
    private PreferenceCategory mAccountList;
    private AccountSelectionPreference mDefaultOutgoingAccount;
    private SipPreferences mSipPreferences;
    private SwitchPreference mSipReceiveCallsPreference;
    private SubscriptionManager mSubscriptionManager;
    private TelecomManager mTelecomManager;
    private TelephonyManager mTelephonyManager;
    private ListPreference mUseSipCalling;
    private int mAccountNumber = 0;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(PhoneAccountSettingsFragment.LOG_TAG, "update phone account preference");
            PhoneAccountSettingsFragment.this.updatePhoneAccountPreference();
        }
    };

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mTelecomManager = TelecomManager.from(getActivity());
        this.mTelephonyManager = TelephonyManager.from(getActivity());
        this.mSubscriptionManager = SubscriptionManager.from(getActivity());
        registerPhoneAccountChangeReceiver();
    }

    @Override
    public void onResume() {
        int i;
        super.onResume();
        if (getPreferenceScreen() != null) {
            getPreferenceScreen().removeAll();
        }
        addPreferencesFromResource(R.xml.phone_account_settings);
        this.mAccountList = (PreferenceCategory) getPreferenceScreen().findPreference("phone_accounts_accounts_list_category_key");
        List<PhoneAccountHandle> callingAccounts = getCallingAccounts(false, true);
        String str = LOG_TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("allNonSimAccounts size: ");
        sb.append(callingAccounts != null ? Integer.valueOf(callingAccounts.size()) : null);
        Log.d(str, sb.toString());
        if (shouldShowConnectionServiceList(callingAccounts)) {
            List<PhoneAccountHandle> callingAccounts2 = getCallingAccounts(true, false);
            String str2 = LOG_TAG;
            StringBuilder sb2 = new StringBuilder();
            sb2.append("enabledAccounts size: ");
            sb2.append(callingAccounts2 != null ? Integer.valueOf(callingAccounts2.size()) : null);
            Log.d(str2, sb2.toString());
            initAccountList(callingAccounts2);
            this.mDefaultOutgoingAccount = (AccountSelectionPreference) getPreferenceScreen().findPreference("default_outgoing_account");
            this.mDefaultOutgoingAccount.setListener(this);
            if (callingAccounts2.size() > 1) {
                updateDefaultOutgoingAccountsModel();
            } else {
                this.mAccountList.removePreference(this.mDefaultOutgoingAccount);
            }
            Preference preferenceFindPreference = getPreferenceScreen().findPreference("all_calling_accounts");
            if (callingAccounts.isEmpty() && preferenceFindPreference != null) {
                this.mAccountList.removePreference(preferenceFindPreference);
            }
        } else {
            getPreferenceScreen().removePreference(this.mAccountList);
        }
        if (isPrimaryUser() && SipUtil.isVoipSupported(getActivity())) {
            this.mSipPreferences = new SipPreferences(getActivity());
            this.mUseSipCalling = (ListPreference) getPreferenceScreen().findPreference("use_sip_calling_options_key");
            ListPreference listPreference = this.mUseSipCalling;
            if (!SipManager.isSipWifiOnly(getActivity())) {
                i = R.array.sip_call_options_wifi_only_entries;
            } else {
                i = R.array.sip_call_options_entries;
            }
            listPreference.setEntries(i);
            this.mUseSipCalling.setOnPreferenceChangeListener(this);
            int iFindIndexOfValue = this.mUseSipCalling.findIndexOfValue(this.mSipPreferences.getSipCallOption());
            if (iFindIndexOfValue == -1) {
                this.mSipPreferences.setSipCallOption(getResources().getString(R.string.sip_address_only));
                iFindIndexOfValue = this.mUseSipCalling.findIndexOfValue(this.mSipPreferences.getSipCallOption());
            }
            this.mUseSipCalling.setValueIndex(iFindIndexOfValue);
            this.mUseSipCalling.setSummary(this.mUseSipCalling.getEntry());
            this.mSipReceiveCallsPreference = (SwitchPreference) getPreferenceScreen().findPreference("sip_receive_calls_key");
            this.mSipReceiveCallsPreference.setEnabled(SipUtil.isPhoneIdle(getActivity()));
            this.mSipReceiveCallsPreference.setChecked(this.mSipPreferences.isReceivingCallsEnabled());
            this.mSipReceiveCallsPreference.setOnPreferenceChangeListener(this);
        } else {
            getPreferenceScreen().removePreference(getPreferenceScreen().findPreference("phone_accounts_sip_settings_category_key"));
        }
        if (isPrimaryUser()) {
            ExtensionManager.getCallFeaturesSettingExt().initOtherCallFeaturesSetting(this);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        if (preference == this.mUseSipCalling) {
            String string = obj.toString();
            this.mSipPreferences.setSipCallOption(string);
            this.mUseSipCalling.setValueIndex(this.mUseSipCalling.findIndexOfValue(string));
            this.mUseSipCalling.setSummary(this.mUseSipCalling.getEntry());
            return true;
        }
        if (preference == this.mSipReceiveCallsPreference) {
            final boolean z = !this.mSipReceiveCallsPreference.isChecked();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    PhoneAccountSettingsFragment.this.handleSipReceiveCallsOption(z);
                }
            }).start();
            return true;
        }
        return false;
    }

    @Override
    public boolean onAccountSelected(AccountSelectionPreference accountSelectionPreference, PhoneAccountHandle phoneAccountHandle) {
        if (accountSelectionPreference == this.mDefaultOutgoingAccount) {
            this.mTelecomManager.setUserSelectedOutgoingPhoneAccount(phoneAccountHandle);
            return true;
        }
        return false;
    }

    @Override
    public void onAccountSelectionDialogShow(AccountSelectionPreference accountSelectionPreference) {
        if (accountSelectionPreference == this.mDefaultOutgoingAccount) {
            updateDefaultOutgoingAccountsModel();
        }
    }

    @Override
    public void onAccountChanged(AccountSelectionPreference accountSelectionPreference) {
    }

    private synchronized void handleSipReceiveCallsOption(boolean z) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        this.mSipPreferences.setReceivingCallsEnabled(z);
        SipUtil.useSipToReceiveIncomingCalls(activity, z);
        SipAccountRegistry.getInstance().restartSipService(activity);
    }

    private void updateDefaultOutgoingAccountsModel() {
        this.mDefaultOutgoingAccount.setModel(this.mTelecomManager, getCallingAccounts(true, false), this.mTelecomManager.getUserSelectedOutgoingPhoneAccount(), getString(R.string.phone_accounts_ask_every_time));
    }

    private void initAccountList(List<PhoneAccountHandle> list) {
        SubscriptionInfo activeSubscriptionInfo;
        removeOldAccounts(this.mAccountList);
        boolean zIsMultiSimEnabled = this.mTelephonyManager.isMultiSimEnabled();
        if (!zIsMultiSimEnabled && getCallingAccounts(false, false).isEmpty()) {
            return;
        }
        ArrayList<PhoneAccount> arrayList = new ArrayList();
        Iterator<PhoneAccountHandle> it = list.iterator();
        while (it.hasNext()) {
            PhoneAccount phoneAccount = this.mTelecomManager.getPhoneAccount(it.next());
            if (phoneAccount != null) {
                arrayList.add(phoneAccount);
            }
        }
        Collections.sort(arrayList, new Comparator<PhoneAccount>() {
            @Override
            public int compare(PhoneAccount phoneAccount2, PhoneAccount phoneAccount3) {
                int iCompareTo;
                boolean zHasCapabilities = phoneAccount2.hasCapabilities(4);
                if (zHasCapabilities != phoneAccount3.hasCapabilities(4)) {
                    iCompareTo = zHasCapabilities ? -1 : 1;
                } else {
                    iCompareTo = 0;
                }
                int subIdForPhoneAccount = PhoneAccountSettingsFragment.this.mTelephonyManager.getSubIdForPhoneAccount(phoneAccount2);
                int subIdForPhoneAccount2 = PhoneAccountSettingsFragment.this.mTelephonyManager.getSubIdForPhoneAccount(phoneAccount3);
                if (subIdForPhoneAccount != -1 && subIdForPhoneAccount2 != -1) {
                    SubscriptionManager unused = PhoneAccountSettingsFragment.this.mSubscriptionManager;
                    int slotIndex = SubscriptionManager.getSlotIndex(subIdForPhoneAccount);
                    SubscriptionManager unused2 = PhoneAccountSettingsFragment.this.mSubscriptionManager;
                    iCompareTo = slotIndex < SubscriptionManager.getSlotIndex(subIdForPhoneAccount2) ? -1 : 1;
                }
                if (iCompareTo == 0) {
                    iCompareTo = phoneAccount2.getAccountHandle().getComponentName().getPackageName().compareTo(phoneAccount3.getAccountHandle().getComponentName().getPackageName());
                }
                if (iCompareTo == 0) {
                    iCompareTo = PhoneAccountSettingsFragment.this.nullToEmpty(phoneAccount2.getLabel().toString()).compareTo(PhoneAccountSettingsFragment.this.nullToEmpty(phoneAccount3.getLabel().toString()));
                }
                if (iCompareTo == 0) {
                    return phoneAccount2.hashCode() - phoneAccount3.hashCode();
                }
                return iCompareTo;
            }
        });
        int i = 100;
        for (PhoneAccount phoneAccount2 : arrayList) {
            PhoneAccountHandle accountHandle = phoneAccount2.getAccountHandle();
            Intent intentBuildPhoneAccountConfigureIntent = null;
            if (phoneAccount2.hasCapabilities(4)) {
                if (zIsMultiSimEnabled && (activeSubscriptionInfo = this.mSubscriptionManager.getActiveSubscriptionInfo(this.mTelephonyManager.getSubIdForPhoneAccount(phoneAccount2))) != null) {
                    intentBuildPhoneAccountConfigureIntent = new Intent("android.telecom.action.SHOW_CALL_SETTINGS");
                    intentBuildPhoneAccountConfigureIntent.setFlags(67108864);
                    SubscriptionInfoHelper.addExtrasToIntent(intentBuildPhoneAccountConfigureIntent, activeSubscriptionInfo);
                }
            } else {
                intentBuildPhoneAccountConfigureIntent = buildPhoneAccountConfigureIntent(getActivity(), accountHandle);
            }
            Preference preference = new Preference(getActivity());
            CharSequence label = phoneAccount2.getLabel();
            boolean zHasCapabilities = phoneAccount2.hasCapabilities(4);
            if (TextUtils.isEmpty(label) && zHasCapabilities) {
                label = getString(R.string.phone_accounts_default_account_label);
            }
            preference.setTitle(label);
            this.mAccountNumber++;
            preference.setKey("account" + this.mAccountNumber);
            Icon icon = phoneAccount2.getIcon();
            if (icon != null) {
                preference.setIcon(icon.loadDrawable(getActivity()));
            }
            if (intentBuildPhoneAccountConfigureIntent != null) {
                preference.setIntent(intentBuildPhoneAccountConfigureIntent);
            }
            preference.setOrder(i);
            this.mAccountList.addPreference(preference);
            i++;
        }
    }

    private boolean shouldShowConnectionServiceList(List<PhoneAccountHandle> list) {
        return this.mTelephonyManager.isMultiSimEnabled() || list.size() > 0;
    }

    private List<PhoneAccountHandle> getCallingAccounts(boolean z, boolean z2) {
        PhoneAccountHandle emergencyPhoneAccount = getEmergencyPhoneAccount();
        List<PhoneAccountHandle> callCapablePhoneAccounts = this.mTelecomManager.getCallCapablePhoneAccounts(z2);
        Iterator<PhoneAccountHandle> it = callCapablePhoneAccounts.iterator();
        while (it.hasNext()) {
            PhoneAccountHandle next = it.next();
            if (next.equals(emergencyPhoneAccount)) {
                it.remove();
            } else {
                PhoneAccount phoneAccount = this.mTelecomManager.getPhoneAccount(next);
                if (phoneAccount == null) {
                    it.remove();
                } else if (!z && phoneAccount.hasCapabilities(4)) {
                    it.remove();
                }
            }
        }
        return callCapablePhoneAccounts;
    }

    private String nullToEmpty(String str) {
        return str == null ? "" : str;
    }

    private PhoneAccountHandle getEmergencyPhoneAccount() {
        return PhoneUtils.makePstnPhoneAccountHandleWithPrefix((Phone) null, "", true);
    }

    public static Intent buildPhoneAccountConfigureIntent(Context context, PhoneAccountHandle phoneAccountHandle) {
        Intent intentBuildConfigureIntent = buildConfigureIntent(context, phoneAccountHandle, "android.telecom.action.CONFIGURE_PHONE_ACCOUNT");
        if (intentBuildConfigureIntent == null && (intentBuildConfigureIntent = buildConfigureIntent(context, phoneAccountHandle, "android.telecom.action.CONNECTION_SERVICE_CONFIGURE")) != null) {
            Log.w(LOG_TAG, "Phone account using old configuration intent: " + phoneAccountHandle);
        }
        Log.d(LOG_TAG, "get intent: " + intentBuildConfigureIntent);
        return intentBuildConfigureIntent;
    }

    private static Intent buildConfigureIntent(Context context, PhoneAccountHandle phoneAccountHandle, String str) {
        if (phoneAccountHandle == null || phoneAccountHandle.getComponentName() == null || TextUtils.isEmpty(phoneAccountHandle.getComponentName().getPackageName())) {
            return null;
        }
        Intent intent = new Intent(str);
        intent.setPackage(phoneAccountHandle.getComponentName().getPackageName());
        intent.addCategory("android.intent.category.DEFAULT");
        intent.putExtra("android.telecom.extra.PHONE_ACCOUNT_HANDLE", phoneAccountHandle);
        if (context.getPackageManager().queryIntentActivities(intent, 0).size() == 0) {
            return null;
        }
        return intent;
    }

    private boolean isPrimaryUser() {
        boolean zIsPrimaryUser = ((UserManager) getActivity().getSystemService("user")).isPrimaryUser();
        Log.d(LOG_TAG, "isPrimaryUser:" + zIsPrimaryUser);
        return zIsPrimaryUser;
    }

    private void registerPhoneAccountChangeReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.telecom.action.PHONE_ACCOUNT_REGISTERED");
        intentFilter.addAction("android.telecom.action.PHONE_ACCOUNT_UNREGISTERED");
        getActivity().registerReceiver(this.mReceiver, intentFilter);
    }

    private void unregisterPhoneAccountChangeReceiver() {
        getActivity().unregisterReceiver(this.mReceiver);
    }

    @Override
    public void onDestroy() {
        if (this.mDefaultOutgoingAccount != null) {
            this.mDefaultOutgoingAccount.doDestroy();
        }
        unregisterPhoneAccountChangeReceiver();
        super.onDestroy();
    }

    private void updatePhoneAccountPreference() {
        initAccountList(getCallingAccounts(true, false));
    }

    private void removeOldAccounts(PreferenceCategory preferenceCategory) {
        while (this.mAccountNumber > 0) {
            Preference preferenceFindPreference = preferenceCategory.findPreference("account" + this.mAccountNumber);
            if (preferenceFindPreference != null) {
                preferenceCategory.removePreference(preferenceFindPreference);
            }
            this.mAccountNumber--;
        }
    }
}
