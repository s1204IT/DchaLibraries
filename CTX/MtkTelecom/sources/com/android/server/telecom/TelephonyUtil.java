package com.android.server.telecom;

import android.content.ComponentName;
import android.content.Context;
import android.net.Uri;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import com.android.internal.annotations.VisibleForTesting;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class TelephonyUtil {
    private static final PhoneAccountHandle DEFAULT_EMERGENCY_PHONE_ACCOUNT_HANDLE = new PhoneAccountHandle(new ComponentName("com.android.phone", "com.android.services.telephony.TelephonyConnectionService"), "E");

    @VisibleForTesting
    public static PhoneAccount getDefaultEmergencyPhoneAccount() {
        return PhoneAccount.builder(DEFAULT_EMERGENCY_PHONE_ACCOUNT_HANDLE, "E").setCapabilities(22).setIsEnabled(true).build();
    }

    public static boolean isPstnComponentName(ComponentName componentName) {
        return new ComponentName("com.android.phone", "com.android.services.telephony.TelephonyConnectionService").equals(componentName);
    }

    public static boolean shouldProcessAsEmergency(Context context, Uri uri) {
        return uri != null && PhoneNumberUtils.isLocalEmergencyNumber(context, uri.getSchemeSpecificPart());
    }

    public static void sortSimPhoneAccounts(Context context, List<PhoneAccount> list) {
        final TelephonyManager telephonyManagerFrom = TelephonyManager.from(context);
        Collections.sort(list, new Comparator<PhoneAccount>() {
            @Override
            public int compare(PhoneAccount phoneAccount, PhoneAccount phoneAccount2) {
                int iCompareTo;
                boolean zHasCapabilities = phoneAccount.hasCapabilities(4);
                if (zHasCapabilities != phoneAccount2.hasCapabilities(4)) {
                    iCompareTo = zHasCapabilities ? -1 : 1;
                } else {
                    iCompareTo = 0;
                }
                int subIdForPhoneAccount = telephonyManagerFrom.getSubIdForPhoneAccount(phoneAccount);
                int subIdForPhoneAccount2 = telephonyManagerFrom.getSubIdForPhoneAccount(phoneAccount2);
                if (subIdForPhoneAccount != -1 && subIdForPhoneAccount2 != -1) {
                    iCompareTo = SubscriptionManager.getSlotIndex(subIdForPhoneAccount) < SubscriptionManager.getSlotIndex(subIdForPhoneAccount2) ? -1 : 1;
                }
                if (iCompareTo == 0) {
                    iCompareTo = phoneAccount.getAccountHandle().getComponentName().getPackageName().compareTo(phoneAccount2.getAccountHandle().getComponentName().getPackageName());
                }
                if (iCompareTo == 0) {
                    iCompareTo = TelephonyUtil.nullToEmpty(phoneAccount.getLabel().toString()).compareTo(TelephonyUtil.nullToEmpty(phoneAccount2.getLabel().toString()));
                }
                if (iCompareTo == 0) {
                    return phoneAccount.hashCode() - phoneAccount2.hashCode();
                }
                return iCompareTo;
            }
        });
    }

    private static String nullToEmpty(String str) {
        return str == null ? "" : str;
    }
}
