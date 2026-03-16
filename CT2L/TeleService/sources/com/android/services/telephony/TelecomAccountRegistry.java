package com.android.services.telephony;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.phone.PhoneUtils;
import com.android.phone.R;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

final class TelecomAccountRegistry {
    private static TelecomAccountRegistry sInstance;
    private final Context mContext;
    private final SubscriptionManager mSubscriptionManager;
    private final TelecomManager mTelecomManager;
    private TelephonyConnectionService mTelephonyConnectionService;
    private final TelephonyManager mTelephonyManager;
    private SubscriptionManager.OnSubscriptionsChangedListener mOnSubscriptionsChangedListener = new SubscriptionManager.OnSubscriptionsChangedListener() {
        @Override
        public void onSubscriptionsChanged() {
            TelecomAccountRegistry.this.tearDownAccounts();
            TelecomAccountRegistry.this.setupAccounts();
        }
    };
    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            int newState = serviceState.getState();
            if (newState == 0 && TelecomAccountRegistry.this.mServiceState != newState) {
                TelecomAccountRegistry.this.tearDownAccounts();
                TelecomAccountRegistry.this.setupAccounts();
            }
            TelecomAccountRegistry.this.mServiceState = newState;
        }
    };
    private List<AccountEntry> mAccounts = new LinkedList();
    private int mServiceState = 3;

    private final class AccountEntry {
        private final PhoneAccount mAccount;
        private final PstnIncomingCallNotifier mIncomingCallNotifier;
        private final Phone mPhone;

        AccountEntry(Phone phone, boolean isEmergency, boolean isDummy) {
            this.mPhone = phone;
            this.mAccount = registerPstnPhoneAccount(isEmergency, isDummy);
            Log.d(this, "Registered phoneAccount: %s with handle: %s", this.mAccount, this.mAccount.getAccountHandle());
            this.mIncomingCallNotifier = new PstnIncomingCallNotifier(this.mPhone);
        }

        void teardown() {
            this.mIncomingCallNotifier.teardown();
        }

        private PhoneAccount registerPstnPhoneAccount(boolean isEmergency, boolean isDummyAccount) {
            String slotIdString;
            String label;
            String description;
            String dummyPrefix = isDummyAccount ? "Dummy " : "";
            PhoneAccountHandle phoneAccountHandle = PhoneUtils.makePstnPhoneAccountHandleWithPrefix(this.mPhone, dummyPrefix, isEmergency);
            int subId = this.mPhone.getSubId();
            int color = 0;
            int slotId = -1;
            String line1Number = TelecomAccountRegistry.this.mTelephonyManager.getLine1NumberForSubscriber(subId);
            if (line1Number == null) {
                line1Number = "";
            }
            String subNumber = this.mPhone.getPhoneSubInfo().getLine1Number();
            if (subNumber == null) {
                subNumber = "";
            }
            Bitmap iconBitmap = null;
            if (isEmergency) {
                label = TelecomAccountRegistry.this.mContext.getResources().getString(R.string.sim_label_emergency_calls);
                description = TelecomAccountRegistry.this.mContext.getResources().getString(R.string.sim_description_emergency_calls);
            } else if (TelecomAccountRegistry.this.mTelephonyManager.getPhoneCount() == 1) {
                label = TelecomAccountRegistry.this.mTelephonyManager.getNetworkOperatorName();
                description = label;
            } else {
                CharSequence subDisplayName = null;
                SubscriptionInfo record = TelecomAccountRegistry.this.mSubscriptionManager.getActiveSubscriptionInfo(subId);
                if (record != null) {
                    subDisplayName = record.getDisplayName();
                    slotId = record.getSimSlotIndex();
                    color = record.getIconTint();
                    iconBitmap = record.createIconBitmap(TelecomAccountRegistry.this.mContext);
                }
                if (!SubscriptionManager.isValidSlotId(slotId)) {
                    slotIdString = TelecomAccountRegistry.this.mContext.getResources().getString(R.string.unknown);
                } else {
                    slotIdString = Integer.toString(slotId);
                }
                if (TextUtils.isEmpty(subDisplayName)) {
                    Log.w(this, "Could not get a display name for subid: %d", Integer.valueOf(subId));
                    subDisplayName = TelecomAccountRegistry.this.mContext.getResources().getString(R.string.sim_description_default, slotIdString);
                }
                label = dummyPrefix + ((Object) subDisplayName);
                description = dummyPrefix + TelecomAccountRegistry.this.mContext.getResources().getString(R.string.sim_description_default, slotIdString);
            }
            if (iconBitmap == null) {
                iconBitmap = BitmapFactory.decodeResource(TelecomAccountRegistry.this.mContext.getResources(), R.drawable.ic_multi_sim);
            }
            PhoneAccount account = PhoneAccount.builder(phoneAccountHandle, label).setAddress(Uri.fromParts("tel", line1Number, null)).setSubscriptionAddress(Uri.fromParts("tel", subNumber, null)).setCapabilities(54).setIcon(iconBitmap).setHighlightColor(color).setShortDescription(description).setSupportedUriSchemes(Arrays.asList("tel", "voicemail")).build();
            TelecomAccountRegistry.this.mTelecomManager.registerPhoneAccount(account);
            return account;
        }

        public PhoneAccountHandle getPhoneAccountHandle() {
            if (this.mAccount != null) {
                return this.mAccount.getAccountHandle();
            }
            return null;
        }
    }

    TelecomAccountRegistry(Context context) {
        this.mContext = context;
        this.mTelecomManager = TelecomManager.from(context);
        this.mTelephonyManager = TelephonyManager.from(context);
        this.mSubscriptionManager = SubscriptionManager.from(context);
    }

    static final synchronized TelecomAccountRegistry getInstance(Context context) {
        if (sInstance == null && context != null) {
            sInstance = new TelecomAccountRegistry(context);
        }
        return sInstance;
    }

    void setTelephonyConnectionService(TelephonyConnectionService telephonyConnectionService) {
        this.mTelephonyConnectionService = telephonyConnectionService;
    }

    TelephonyConnectionService getTelephonyConnectionService() {
        return this.mTelephonyConnectionService;
    }

    void setupOnBoot() {
        SubscriptionManager.from(this.mContext).addOnSubscriptionsChangedListener(this.mOnSubscriptionsChangedListener);
        this.mTelephonyManager.listen(this.mPhoneStateListener, 1);
    }

    static PhoneAccountHandle makePstnPhoneAccountHandle(Phone phone) {
        return makePstnPhoneAccountHandleWithPrefix(phone, "", false);
    }

    static PhoneAccountHandle makePstnPhoneAccountHandleWithPrefix(Phone phone, String prefix, boolean isEmergency) {
        ComponentName pstnConnectionServiceName = new ComponentName(phone.getContext(), (Class<?>) TelephonyConnectionService.class);
        String id = isEmergency ? "E" : prefix + String.valueOf(phone.getSubId());
        return new PhoneAccountHandle(pstnConnectionServiceName, id);
    }

    boolean hasAccountEntryForPhoneAccount(PhoneAccountHandle handle) {
        for (AccountEntry entry : this.mAccounts) {
            if (entry.getPhoneAccountHandle().equals(handle)) {
                return true;
            }
        }
        return false;
    }

    private void cleanupPhoneAccounts() {
        ComponentName telephonyComponentName = new ComponentName(this.mContext, (Class<?>) TelephonyConnectionService.class);
        List<PhoneAccountHandle> accountHandles = this.mTelecomManager.getAllPhoneAccountHandles();
        for (PhoneAccountHandle handle : accountHandles) {
            if (telephonyComponentName.equals(handle.getComponentName()) && !hasAccountEntryForPhoneAccount(handle)) {
                Log.d(this, "Unregistering phone account %s.", handle);
                this.mTelecomManager.unregisterPhoneAccount(handle);
            }
        }
    }

    private void setupAccounts() {
        Phone[] phones = PhoneFactory.getPhones();
        Log.d(this, "Found %d phones.  Attempting to register.", Integer.valueOf(phones.length));
        for (Phone phone : phones) {
            long subscriptionId = phone.getSubId();
            Log.d(this, "Phone with subscription id %d", Long.valueOf(subscriptionId));
            if (subscriptionId >= 0) {
                this.mAccounts.add(new AccountEntry(phone, false, false));
            }
        }
        if (this.mAccounts.isEmpty()) {
            this.mAccounts.add(new AccountEntry(PhoneFactory.getDefaultPhone(), true, false));
        }
        cleanupPhoneAccounts();
    }

    private void tearDownAccounts() {
        for (AccountEntry entry : this.mAccounts) {
            entry.teardown();
        }
        this.mAccounts.clear();
    }
}
