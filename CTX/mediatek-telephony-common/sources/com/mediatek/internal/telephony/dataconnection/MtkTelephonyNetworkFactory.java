package com.mediatek.internal.telephony.dataconnection;

import android.content.Context;
import android.net.NetworkCapabilities;
import android.os.Looper;
import com.android.internal.telephony.PhoneSwitcher;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.SubscriptionMonitor;
import com.android.internal.telephony.dataconnection.DcTracker;
import com.android.internal.telephony.dataconnection.TelephonyNetworkFactory;

public class MtkTelephonyNetworkFactory extends TelephonyNetworkFactory {
    private boolean mPdnActivation;

    public MtkTelephonyNetworkFactory(PhoneSwitcher phoneSwitcher, SubscriptionController subscriptionController, SubscriptionMonitor subscriptionMonitor, Looper looper, Context context, int i, DcTracker dcTracker) {
        super(phoneSwitcher, subscriptionController, subscriptionMonitor, looper, context, i, dcTracker);
        this.mPdnActivation = false;
    }

    protected NetworkCapabilities makeNetworkFilter(int i) {
        NetworkCapabilities networkCapabilitiesMakeNetworkFilter = super.makeNetworkFilter(i);
        networkCapabilitiesMakeNetworkFilter.addCapability(27);
        networkCapabilitiesMakeNetworkFilter.addCapability(26);
        networkCapabilitiesMakeNetworkFilter.addCapability(28);
        return networkCapabilitiesMakeNetworkFilter;
    }

    protected boolean ignoreCapabilityCheck(NetworkCapabilities networkCapabilities, boolean z) {
        if (networkCapabilities.hasCapability(4) || networkCapabilities.hasCapability(10)) {
            if (z) {
                if (MtkDcHelper.getInstance().isMultiPsAttachSupport()) {
                    log("ignoreCapabilityCheck() allow IMS/EIMS pdn activation");
                    return true;
                }
                log("ignoreCapabilityCheck() reject IMS/EIMS pdn activation");
                return false;
            }
            log("ignoreCapabilityCheck() ignore IMS/EIMS PDN");
            return true;
        }
        if (!networkCapabilities.hasCapability(26)) {
            return false;
        }
        log("ignoreCapabilityCheck() ignore VSIM PDN");
        return true;
    }
}
