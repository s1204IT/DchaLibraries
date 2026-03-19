package com.mediatek.internal.telephony;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Looper;
import android.os.Message;
import android.telephony.Rlog;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.ITelephonyRegistry;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneSwitcher;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.dataconnection.ApnContext;
import com.android.internal.telephony.dataconnection.DcRequest;
import com.mediatek.internal.telephony.dataconnection.MtkDcHelper;
import com.mediatek.telephony.MtkTelephonyManagerEx;
import java.util.Iterator;
import java.util.List;

public class MtkPhoneSwitcher extends PhoneSwitcher {
    private static final int EVENT_SIMLOCK_INFO_CHANGED = 1000;
    private static final String LOG_TAG = "MtkPhoneSwitcher";
    private static final boolean VDBG = true;
    private static MtkPhoneSwitcher sInstance = null;
    private boolean[] mPhoneStateIsSet;
    private final BroadcastReceiver mSimLockChangedReceiver;

    public MtkPhoneSwitcher(int i, int i2, Context context, SubscriptionController subscriptionController, Looper looper, ITelephonyRegistry iTelephonyRegistry, CommandsInterface[] commandsInterfaceArr, Phone[] phoneArr) {
        super(i, i2, context, subscriptionController, looper, iTelephonyRegistry, commandsInterfaceArr, phoneArr);
        this.mSimLockChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                MtkPhoneSwitcher.this.obtainMessage(1000).sendToTarget();
            }
        };
        sInstance = this;
        this.mPhoneStateIsSet = new boolean[i2];
        for (int i3 = 0; i3 < i2; i3++) {
            this.mPhoneStateIsSet[i3] = false;
        }
        if (MtkTelephonyManagerEx.getDefault() != null) {
            log("getSimLockPolicy:" + MtkTelephonyManagerEx.getDefault().getSimLockPolicy());
        }
        if (getSimLockMode()) {
            this.mContext.registerReceiver(this.mSimLockChangedReceiver, new IntentFilter("com.mediatek.phone.ACTION_SIM_SLOT_LOCK_POLICY_INFORMATION"));
        }
    }

    public void handleMessage(Message message) {
        if (message.what == 1000) {
            StringBuilder sb = new StringBuilder("simLockChange");
            for (int i = 0; i < this.mNumPhones; i++) {
                int shouldServiceCapability = MtkTelephonyManagerEx.getDefault().getShouldServiceCapability(i);
                sb.append(" phone[");
                sb.append(i);
                sb.append("],Capability=");
                sb.append(shouldServiceCapability);
            }
            onEvaluate(true, sb.toString());
            return;
        }
        super.handleMessage(message);
    }

    public static MtkPhoneSwitcher getInstance() {
        return sInstance;
    }

    protected NetworkCapabilities makeNetworkFilter() {
        NetworkCapabilities networkCapabilitiesMakeNetworkFilter = super.makeNetworkFilter();
        networkCapabilitiesMakeNetworkFilter.addCapability(27);
        networkCapabilitiesMakeNetworkFilter.addCapability(28);
        return networkCapabilitiesMakeNetworkFilter;
    }

    protected void deactivate(int i) {
        super.deactivate(i);
        this.mPhoneStateIsSet[i] = true;
    }

    protected void activate(int i) {
        super.activate(i);
        this.mPhoneStateIsSet[i] = true;
    }

    public void resendDataAllowed(int i) {
        log("resendDataAllowed: mPhoneStateIsSet[" + i + "] =" + this.mPhoneStateIsSet[i]);
        if (this.mPhoneStateIsSet[i]) {
            super.resendDataAllowed(i);
        }
    }

    private boolean isEimsAllowed(NetworkRequest networkRequest) {
        if (networkRequest.networkCapabilities.hasCapability(10)) {
            for (int i = 0; i < this.mNumPhones; i++) {
                if (MtkDcHelper.getInstance().isSimInserted(i)) {
                    loge("isAllowEims, sim is not null");
                    return false;
                }
            }
            return true;
        }
        loge("isAllowEims, NetworkRequest not include EIMS capability");
        return false;
    }

    protected void suggestDefaultActivePhone(List<Integer> list) {
        MtkDcHelper mtkDcHelper = MtkDcHelper.getInstance();
        int mainCapabilityPhoneId = RadioCapabilitySwitchUtil.getMainCapabilityPhoneId();
        if (list.isEmpty()) {
            log("newActivePhones is empty");
            if (mtkDcHelper.isSimInserted(mainCapabilityPhoneId) && (!getSimLockMode() || getPsAllowedByPhoneId(mainCapabilityPhoneId))) {
                log("newActivePhones mainCapPhoneId=" + mainCapabilityPhoneId);
                list.add(Integer.valueOf(mainCapabilityPhoneId));
            }
        }
        logv("mPrioritizedDcRequests" + this.mPrioritizedDcRequests.toString());
        if (list.isEmpty()) {
            log("ECC w/o SIM");
            Iterator it = this.mPrioritizedDcRequests.iterator();
            while (it.hasNext()) {
                if (isEimsAllowed(((DcRequest) it.next()).networkRequest)) {
                    log("newActivePhones mainCapPhoneId=" + mainCapabilityPhoneId);
                    list.add(Integer.valueOf(mainCapabilityPhoneId));
                }
            }
        }
    }

    public static boolean acceptRequest(NetworkRequest networkRequest, int i) {
        if (ApnContext.apnIdForNetworkRequest(networkRequest) == -1) {
            log("[acceptRequest] Invalid APN ID request: " + networkRequest);
            return false;
        }
        return true;
    }

    protected int phoneIdForRequest(NetworkRequest networkRequest) {
        int iPhoneIdForRequest = super.phoneIdForRequest(networkRequest);
        if (getSimLockMode() && !getPsAllowedByPhoneId(iPhoneIdForRequest)) {
            return -1;
        }
        return iPhoneIdForRequest;
    }

    private boolean getSimLockMode() {
        int simLockPolicy = MtkTelephonyManagerEx.getDefault().getSimLockPolicy();
        return simLockPolicy == 1 || simLockPolicy == 2 || simLockPolicy == 3 || simLockPolicy == 4 || simLockPolicy == 5 || simLockPolicy == 6 || simLockPolicy == 7;
    }

    private boolean getPsAllowedByPhoneId(int i) {
        int shouldServiceCapability = MtkTelephonyManagerEx.getDefault().getShouldServiceCapability(i);
        switch (MtkTelephonyManagerEx.getDefault().getSimLockPolicy()) {
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
                if (shouldServiceCapability == 0 || shouldServiceCapability == 2) {
                }
                break;
        }
        return true;
    }

    private static void log(String str) {
        Rlog.d(LOG_TAG, str);
    }

    private static void loge(String str) {
        Rlog.e(LOG_TAG, str);
    }

    private static void logv(String str) {
        Rlog.v(LOG_TAG, str);
    }
}
