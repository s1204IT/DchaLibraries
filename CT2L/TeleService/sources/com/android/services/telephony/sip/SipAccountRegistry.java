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

        AccountEntry(SipProfile profile) {
            this.mProfile = profile;
        }

        SipProfile getProfile() {
            return this.mProfile;
        }

        boolean startSipService(SipManager sipManager, Context context, boolean isReceivingCalls) {
            try {
                sipManager.close(this.mProfile.getUriString());
                if (isReceivingCalls) {
                    sipManager.open(this.mProfile, SipUtil.createIncomingCallPendingIntent(context, this.mProfile.getUriString()), null);
                } else {
                    sipManager.open(this.mProfile);
                }
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

    void setup(Context context) {
        startSipProfilesAsync(context, (String) null);
    }

    void startSipService(Context context, String sipUri) {
        startSipProfilesAsync(context, sipUri);
    }

    void removeSipProfile(String sipUri) {
        AccountEntry accountEntry = getAccountEntry(sipUri);
        if (accountEntry != null) {
            this.mAccounts.remove(accountEntry);
        }
    }

    void stopSipService(Context context, String sipUri) {
        AccountEntry accountEntry = getAccountEntry(sipUri);
        if (accountEntry != null) {
            SipManager sipManager = SipManager.newInstance(context);
            accountEntry.stopSipService(sipManager);
        }
        PhoneAccountHandle handle = SipUtil.createAccountHandle(context, sipUri);
        TelecomManager.from(context).unregisterPhoneAccount(handle);
    }

    public void restartSipService(Context context) {
        startSipProfiles(context, null);
    }

    private void startSipProfilesAsync(final Context context, final String sipUri) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                SipAccountRegistry.this.startSipProfiles(context, sipUri);
            }
        }).start();
    }

    private void startSipProfiles(Context context, String sipUri) {
        SipSharedPreferences sipSharedPreferences = new SipSharedPreferences(context);
        boolean isReceivingCalls = sipSharedPreferences.isReceivingCallsEnabled();
        String primaryProfile = sipSharedPreferences.getPrimaryAccount();
        TelecomManager telecomManager = TelecomManager.from(context);
        SipManager sipManager = SipManager.newInstance(context);
        SipProfileDb profileDb = new SipProfileDb(context);
        List<SipProfile> sipProfileList = profileDb.retrieveSipProfileList();
        for (SipProfile profile : sipProfileList) {
            if (sipUri == null || Objects.equals(sipUri, profile.getUriString())) {
                PhoneAccount phoneAccount = SipUtil.createPhoneAccount(context, profile);
                telecomManager.registerPhoneAccount(phoneAccount);
            }
            if (sipUri == null || Objects.equals(sipUri, profile.getUriString())) {
                startSipServiceForProfile(profile, sipManager, context, isReceivingCalls);
            }
        }
        if (primaryProfile != null) {
            sipSharedPreferences.cleanupPrimaryAccountSetting();
        }
    }

    private void startSipServiceForProfile(SipProfile profile, SipManager sipManager, Context context, boolean isReceivingCalls) {
        removeSipProfile(profile.getUriString());
        AccountEntry entry = new AccountEntry(profile);
        if (entry.startSipService(sipManager, context, isReceivingCalls)) {
            this.mAccounts.add(entry);
        }
    }

    private AccountEntry getAccountEntry(String sipUri) {
        for (AccountEntry entry : this.mAccounts) {
            if (Objects.equals(sipUri, entry.getProfile().getUriString())) {
                return entry;
            }
        }
        return null;
    }

    private void log(String message) {
        Log.d("SIP", "[SipAccountRegistry] " + message);
    }
}
