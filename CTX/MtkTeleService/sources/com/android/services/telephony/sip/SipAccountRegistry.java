package com.android.services.telephony.sip;

import android.content.Context;
import android.net.sip.SipException;
import android.net.sip.SipManager;
import android.net.sip.SipProfile;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.util.Log;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public final class SipAccountRegistry {
    private static final SipAccountRegistry INSTANCE = new SipAccountRegistry();
    private final List<AccountEntry> mAccounts = new CopyOnWriteArrayList();

    private final class AccountEntry {
        private final SipProfile mProfile;

        AccountEntry(SipProfile sipProfile) {
            this.mProfile = sipProfile;
        }

        SipProfile getProfile() {
            return this.mProfile;
        }

        boolean startSipService(SipManager sipManager, Context context, boolean z) {
            try {
                sipManager.close(this.mProfile.getUriString());
                if (z) {
                    sipManager.open(this.mProfile, SipUtil.createIncomingCallPendingIntent(context, this.mProfile.getProfileName()), null);
                    return true;
                }
                sipManager.open(this.mProfile);
                return true;
            } catch (SipException e) {
                SipAccountRegistry.this.log("startSipService, profile: " + this.mProfile.getProfileName() + ", exception: " + e);
                return false;
            }
        }

        boolean stopSipService(SipManager sipManager) {
            try {
                sipManager.close(this.mProfile.getUriString());
                return true;
            } catch (Exception e) {
                SipAccountRegistry.this.log("stopSipService, stop failed for profile: " + this.mProfile.getUriString() + ", exception: " + e);
                return false;
            }
        }
    }

    private SipAccountRegistry() {
    }

    public static SipAccountRegistry getInstance() {
        return INSTANCE;
    }

    public void setup(Context context) {
        verifyAndPurgeInvalidPhoneAccounts(context);
        startSipProfilesAsync(context, (String) null, false);
    }

    void verifyAndPurgeInvalidPhoneAccounts(Context context) {
        TelecomManager telecomManagerFrom = TelecomManager.from(context);
        SipProfileDb sipProfileDb = new SipProfileDb(context);
        for (PhoneAccountHandle phoneAccountHandle : telecomManagerFrom.getPhoneAccountsSupportingScheme("sip")) {
            if (sipProfileDb.retrieveSipProfileFromName(SipUtil.getSipProfileNameFromPhoneAccount(phoneAccountHandle)) == null) {
                log("verifyAndPurgeInvalidPhoneAccounts, deleting account: " + phoneAccountHandle);
                telecomManagerFrom.unregisterPhoneAccount(phoneAccountHandle);
            }
        }
    }

    void startSipService(Context context, String str, boolean z) {
        startSipProfilesAsync(context, str, z);
    }

    public void removeSipProfile(String str) {
        AccountEntry accountEntry = getAccountEntry(str);
        if (accountEntry != null) {
            this.mAccounts.remove(accountEntry);
        }
    }

    void stopSipService(Context context, String str) {
        AccountEntry accountEntry = getAccountEntry(str);
        if (accountEntry != null) {
            accountEntry.stopSipService(SipManager.newInstance(context));
        }
        TelecomManager.from(context).unregisterPhoneAccount(SipUtil.createAccountHandle(context, str));
    }

    public void restartSipService(Context context) {
        startSipProfiles(context, null, false);
    }

    private void startSipProfilesAsync(final Context context, final String str, final boolean z) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                SipAccountRegistry.this.startSipProfiles(context, str, z);
            }
        }).start();
    }

    private void startSipProfiles(Context context, String str, boolean z) {
        boolean zIsReceivingCallsEnabled = new SipPreferences(context).isReceivingCallsEnabled();
        TelecomManager telecomManagerFrom = TelecomManager.from(context);
        SipManager sipManagerNewInstance = SipManager.newInstance(context);
        for (SipProfile sipProfile : new SipProfileDb(context).retrieveSipProfileList()) {
            if (str == null || str.equals(sipProfile.getProfileName())) {
                PhoneAccount phoneAccountCreatePhoneAccount = SipUtil.createPhoneAccount(context, sipProfile);
                telecomManagerFrom.registerPhoneAccount(phoneAccountCreatePhoneAccount);
                if (z) {
                    telecomManagerFrom.enablePhoneAccount(phoneAccountCreatePhoneAccount.getAccountHandle(), true);
                }
                startSipServiceForProfile(sipProfile, sipManagerNewInstance, context, zIsReceivingCallsEnabled);
            }
        }
    }

    private void startSipServiceForProfile(SipProfile sipProfile, SipManager sipManager, Context context, boolean z) {
        removeSipProfile(sipProfile.getUriString());
        AccountEntry accountEntry = new AccountEntry(sipProfile);
        if (accountEntry.startSipService(sipManager, context, z)) {
            this.mAccounts.add(accountEntry);
        }
    }

    private AccountEntry getAccountEntry(String str) {
        for (AccountEntry accountEntry : this.mAccounts) {
            if (Objects.equals(str, accountEntry.getProfile().getProfileName())) {
                return accountEntry;
            }
        }
        return null;
    }

    private void log(String str) {
        Log.d("SIP", "[SipAccountRegistry] " + str);
    }
}
