package com.android.internal.telephony;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkCapabilities;
import android.net.NetworkFactory;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.LocalLog;
import com.android.internal.telephony.IOnSubscriptionsChangedListener;
import com.android.internal.telephony.dataconnection.DcRequest;
import com.android.internal.util.IndentingPrintWriter;
import com.google.android.mms.pdu.CharacterSets;
import com.mediatek.internal.telephony.RadioCapabilitySwitchUtil;
import com.mediatek.internal.telephony.dataconnection.DataConnectionHelper;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class PhoneSwitcher extends Handler {
    private static final int EVENT_DEFAULT_SUBSCRIPTION_CHANGED = 101;
    private static final int EVENT_EMERGENCY_TOGGLE = 105;
    private static final int EVENT_RELEASE_NETWORK = 104;
    private static final int EVENT_REQUEST_NETWORK = 103;
    private static final int EVENT_RESEND_DATA_ALLOWED = 106;
    private static final int EVENT_SUBSCRIPTION_CHANGED = 102;
    private static final String INVALID_ICCID = "N/A";
    private static final String LOG_TAG = "PhoneSwitcher";
    private static final int MAX_LOCAL_LOG_LINES = 30;
    private static final boolean REQUESTS_CHANGED = true;
    private static final boolean REQUESTS_UNCHANGED = false;
    private static final boolean VDBG = true;
    private String[] PROPERTY_ICCID_SIM;
    private final RegistrantList[] mActivePhoneRegistrants;
    private final CommandsInterface[] mCommandsInterfaces;
    private final Context mContext;
    private final BroadcastReceiver mDefaultDataChangedReceiver;
    private int mDefaultDataSubscription;
    private final LocalLog mLocalLog;
    private int mMaxActivePhones;
    private final int mNumPhones;
    private final PhoneState[] mPhoneStates;
    private final int[] mPhoneSubscriptions;
    private final Phone[] mPhones;
    private final List<DcRequest> mPrioritizedDcRequests;
    private final SubscriptionController mSubscriptionController;
    private final IOnSubscriptionsChangedListener mSubscriptionsChangedListener;

    public PhoneSwitcher(Looper looper) {
        super(looper);
        this.mPrioritizedDcRequests = new ArrayList();
        this.PROPERTY_ICCID_SIM = new String[]{"ril.iccid.sim1", "ril.iccid.sim2", "ril.iccid.sim3", "ril.iccid.sim4"};
        this.mDefaultDataChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Message msg = PhoneSwitcher.this.obtainMessage(101);
                msg.sendToTarget();
            }
        };
        this.mSubscriptionsChangedListener = new IOnSubscriptionsChangedListener.Stub() {
            public void onSubscriptionsChanged() {
                Message msg = PhoneSwitcher.this.obtainMessage(102);
                msg.sendToTarget();
            }
        };
        this.mMaxActivePhones = 0;
        this.mSubscriptionController = null;
        this.mPhoneSubscriptions = null;
        this.mCommandsInterfaces = null;
        this.mContext = null;
        this.mPhoneStates = null;
        this.mPhones = null;
        this.mLocalLog = null;
        this.mActivePhoneRegistrants = null;
        this.mNumPhones = 0;
    }

    public PhoneSwitcher(int maxActivePhones, int numPhones, Context context, SubscriptionController subscriptionController, Looper looper, ITelephonyRegistry tr, CommandsInterface[] cis, Phone[] phones) {
        super(looper);
        this.mPrioritizedDcRequests = new ArrayList();
        this.PROPERTY_ICCID_SIM = new String[]{"ril.iccid.sim1", "ril.iccid.sim2", "ril.iccid.sim3", "ril.iccid.sim4"};
        this.mDefaultDataChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                Message msg = PhoneSwitcher.this.obtainMessage(101);
                msg.sendToTarget();
            }
        };
        this.mSubscriptionsChangedListener = new IOnSubscriptionsChangedListener.Stub() {
            public void onSubscriptionsChanged() {
                Message msg = PhoneSwitcher.this.obtainMessage(102);
                msg.sendToTarget();
            }
        };
        this.mContext = context;
        this.mNumPhones = numPhones;
        this.mPhones = phones;
        this.mPhoneSubscriptions = new int[numPhones];
        this.mMaxActivePhones = maxActivePhones;
        this.mLocalLog = new LocalLog(30);
        this.mSubscriptionController = subscriptionController;
        this.mActivePhoneRegistrants = new RegistrantList[numPhones];
        this.mPhoneStates = new PhoneState[numPhones];
        for (int i = 0; i < numPhones; i++) {
            this.mActivePhoneRegistrants[i] = new RegistrantList();
            this.mPhoneStates[i] = new PhoneState(null);
            if (this.mPhones[i] != null) {
                this.mPhones[i].registerForEmergencyCallToggle(this, 105, null);
            }
        }
        this.mCommandsInterfaces = cis;
        try {
            tr.addOnSubscriptionsChangedListener(LOG_TAG, this.mSubscriptionsChangedListener);
        } catch (RemoteException e) {
        }
        this.mContext.registerReceiver(this.mDefaultDataChangedReceiver, new IntentFilter("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED"));
        NetworkCapabilities netCap = new NetworkCapabilities();
        netCap.addTransportType(0);
        netCap.addCapability(0);
        netCap.addCapability(1);
        netCap.addCapability(2);
        netCap.addCapability(3);
        netCap.addCapability(4);
        netCap.addCapability(5);
        netCap.addCapability(7);
        netCap.addCapability(8);
        netCap.addCapability(9);
        netCap.addCapability(10);
        netCap.addCapability(13);
        netCap.addCapability(12);
        netCap.addCapability(27);
        netCap.setNetworkSpecifier(CharacterSets.MIMENAME_ANY_CHARSET);
        NetworkFactory networkFactory = new PhoneSwitcherNetworkRequestListener(looper, context, netCap, this);
        networkFactory.setScoreFilter(101);
        networkFactory.register();
        log("PhoneSwitcher started");
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case 101:
                onEvaluate(REQUESTS_UNCHANGED, "defaultChanged");
                break;
            case 102:
                onEvaluate(REQUESTS_UNCHANGED, "subChanged");
                break;
            case EVENT_REQUEST_NETWORK:
                onRequestNetwork((NetworkRequest) msg.obj);
                break;
            case 104:
                onReleaseNetwork((NetworkRequest) msg.obj);
                break;
            case 105:
                onEvaluate(true, "emergencyToggle");
                break;
            case 106:
                onResendDataAllowed(msg);
                break;
        }
    }

    private boolean isEmergency() {
        for (Phone p : this.mPhones) {
            if (p != null && (p.isInEcm() || p.isInEmergencyCall())) {
                return true;
            }
        }
        return REQUESTS_UNCHANGED;
    }

    private static class PhoneSwitcherNetworkRequestListener extends NetworkFactory {
        private final PhoneSwitcher mPhoneSwitcher;

        public PhoneSwitcherNetworkRequestListener(Looper l, Context c, NetworkCapabilities nc, PhoneSwitcher ps) {
            super(l, c, "PhoneSwitcherNetworkRequstListener", nc);
            this.mPhoneSwitcher = ps;
        }

        protected void needNetworkFor(NetworkRequest networkRequest, int score) {
            log("needNetworkFor " + networkRequest + ", " + score);
            Message msg = this.mPhoneSwitcher.obtainMessage(PhoneSwitcher.EVENT_REQUEST_NETWORK);
            msg.obj = networkRequest;
            msg.sendToTarget();
        }

        protected void releaseNetworkFor(NetworkRequest networkRequest) {
            log("releaseNetworkFor " + networkRequest);
            Message msg = this.mPhoneSwitcher.obtainMessage(104);
            msg.obj = networkRequest;
            msg.sendToTarget();
        }
    }

    private void onRequestNetwork(NetworkRequest networkRequest) {
        DcRequest dcRequest = new DcRequest(networkRequest, this.mContext);
        if (this.mPrioritizedDcRequests.contains(dcRequest)) {
            return;
        }
        this.mPrioritizedDcRequests.add(dcRequest);
        Collections.sort(this.mPrioritizedDcRequests);
        onEvaluate(true, "netRequest");
    }

    private void onReleaseNetwork(NetworkRequest networkRequest) {
        DcRequest dcRequest = new DcRequest(networkRequest, this.mContext);
        if (!this.mPrioritizedDcRequests.remove(dcRequest)) {
            return;
        }
        onEvaluate(true, "netReleased");
    }

    private void onEvaluate(boolean requestsChanged, String reason) {
        StringBuilder sb = new StringBuilder(reason);
        if (isEmergency()) {
            log("onEvalute aborted due to Emergency");
            return;
        }
        boolean diffDetected = requestsChanged;
        int dataSub = this.mSubscriptionController.getDefaultDataSubId();
        if (dataSub != this.mDefaultDataSubscription) {
            sb.append(" default ").append(this.mDefaultDataSubscription).append("->").append(dataSub);
            this.mDefaultDataSubscription = dataSub;
            DataConnectionHelper.updateDefaultDataIccid(dataSub);
            if (DataConnectionHelper.MTK_SVLTE_SUPPORT) {
                DataConnectionHelper.getInstance().updateMaxActivePhoneSvlte(SubscriptionManager.getPhoneId(this.mDefaultDataSubscription));
            }
            diffDetected = true;
        }
        for (int i = 0; i < this.mNumPhones; i++) {
            int sub = this.mSubscriptionController.getSubIdUsingPhoneId(i);
            if (sub != this.mPhoneSubscriptions[i]) {
                sb.append(" phone[").append(i).append("] ").append(this.mPhoneSubscriptions[i]);
                sb.append("->").append(sub);
                this.mPhoneSubscriptions[i] = sub;
                diffDetected = true;
            }
        }
        if (!diffDetected) {
            return;
        }
        log("evaluating due to " + sb.toString());
        List<Integer> newActivePhones = new ArrayList<>();
        for (DcRequest dcRequest : this.mPrioritizedDcRequests) {
            int phoneIdForRequest = phoneIdForRequest(dcRequest.networkRequest);
            if (phoneIdForRequest == -1) {
                if (isSkipEimsCheck(dcRequest.networkRequest)) {
                    phoneIdForRequest = getMainCapPhoneId();
                } else {
                    continue;
                }
            }
            if (!newActivePhones.contains(Integer.valueOf(phoneIdForRequest))) {
                newActivePhones.add(Integer.valueOf(phoneIdForRequest));
                if (newActivePhones.size() >= this.mMaxActivePhones) {
                    break;
                }
            } else {
                continue;
            }
        }
        DataConnectionHelper dcHelper = DataConnectionHelper.getInstance();
        if (newActivePhones.isEmpty()) {
            log("newActivePhones is empty");
            int mainCapPhoneId = RadioCapabilitySwitchUtil.getMainCapabilityPhoneId();
            if (dcHelper.isSimInserted(mainCapPhoneId)) {
                log("newActivePhones mainCapPhoneId=" + mainCapPhoneId);
                newActivePhones.add(Integer.valueOf(mainCapPhoneId));
            }
        }
        if (DataConnectionHelper.MTK_SVLTE_SUPPORT) {
            dcHelper.updateActivePhonesSvlte(newActivePhones);
        }
        log("default subId = " + this.mDefaultDataSubscription);
        for (int i2 = 0; i2 < this.mNumPhones; i2++) {
            log(" phone[" + i2 + "] using sub[" + this.mPhoneSubscriptions[i2] + "]");
        }
        log(" newActivePhones:");
        for (Integer i3 : newActivePhones) {
            log("  " + i3);
        }
        for (int phoneId = 0; phoneId < this.mNumPhones; phoneId++) {
            if (!newActivePhones.contains(Integer.valueOf(phoneId))) {
                deactivate(phoneId);
            }
        }
        Iterator phoneId$iterator = newActivePhones.iterator();
        while (phoneId$iterator.hasNext()) {
            int phoneId2 = ((Integer) phoneId$iterator.next()).intValue();
            activate(phoneId2);
        }
    }

    private static class PhoneState {
        public volatile boolean active;
        public long lastRequested;

        PhoneState(PhoneState phoneState) {
            this();
        }

        private PhoneState() {
            this.active = PhoneSwitcher.REQUESTS_UNCHANGED;
            this.lastRequested = 0L;
        }
    }

    private void deactivate(int phoneId) {
        PhoneState state = this.mPhoneStates[phoneId];
        if (state.active) {
            state.active = REQUESTS_UNCHANGED;
            log("deactivate " + phoneId);
            state.lastRequested = System.currentTimeMillis();
            this.mCommandsInterfaces[phoneId].setDataAllowed(REQUESTS_UNCHANGED, null);
            this.mActivePhoneRegistrants[phoneId].notifyRegistrants();
        }
    }

    private void activate(int phoneId) {
        PhoneState state = this.mPhoneStates[phoneId];
        if (state.active) {
            return;
        }
        state.active = true;
        log("activate " + phoneId);
        state.lastRequested = System.currentTimeMillis();
        this.mCommandsInterfaces[phoneId].setDataAllowed(true, null);
        this.mActivePhoneRegistrants[phoneId].notifyRegistrants();
    }

    public void resendDataAllowed(int phoneId) {
        log("resendDataAllowed " + phoneId);
        validatePhoneId(phoneId);
        Message msg = obtainMessage(106);
        msg.arg1 = phoneId;
        msg.sendToTarget();
    }

    private void onResendDataAllowed(Message msg) {
        int phoneId = msg.arg1;
        this.mCommandsInterfaces[phoneId].setDataAllowed(this.mPhoneStates[phoneId].active, null);
    }

    private int phoneIdForRequest(NetworkRequest netRequest) {
        int subId;
        String specifier = netRequest.networkCapabilities.getNetworkSpecifier();
        if (TextUtils.isEmpty(specifier)) {
            subId = this.mDefaultDataSubscription;
        } else {
            subId = Integer.parseInt(specifier);
        }
        if (subId == -1) {
            return -1;
        }
        for (int i = 0; i < this.mNumPhones; i++) {
            if (this.mPhoneSubscriptions[i] == subId) {
                int phoneId = i;
                return phoneId;
            }
        }
        return -1;
    }

    public boolean isPhoneActive(int phoneId) {
        validatePhoneId(phoneId);
        return this.mPhoneStates[phoneId].active;
    }

    public void registerForActivePhoneSwitch(int phoneId, Handler h, int what, Object o) {
        validatePhoneId(phoneId);
        Registrant r = new Registrant(h, what, o);
        this.mActivePhoneRegistrants[phoneId].add(r);
        r.notifyRegistrant();
    }

    public void unregisterForActivePhoneSwitch(int phoneId, Handler h) {
        validatePhoneId(phoneId);
        this.mActivePhoneRegistrants[phoneId].remove(h);
    }

    private void validatePhoneId(int phoneId) {
        if (phoneId >= 0 && phoneId < this.mNumPhones) {
        } else {
            throw new IllegalArgumentException("Invalid PhoneId");
        }
    }

    private void log(String l) {
        Rlog.d(LOG_TAG, l);
        this.mLocalLog.log(l);
    }

    private static void logv(String s) {
        Rlog.v(LOG_TAG, "[PhoneSwitcher] " + s);
    }

    private static void logd(String s) {
        Rlog.d(LOG_TAG, "[PhoneSwitcher] " + s);
    }

    private static void logw(String s) {
        Rlog.w(LOG_TAG, "[PhoneSwitcher] " + s);
    }

    private static void loge(String s) {
        Rlog.e(LOG_TAG, "[PhoneSwitcher] " + s);
    }

    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        IndentingPrintWriter pw = new IndentingPrintWriter(writer, "  ");
        pw.println("PhoneSwitcher:");
        Calendar c = Calendar.getInstance();
        for (int i = 0; i < this.mNumPhones; i++) {
            PhoneState ps = this.mPhoneStates[i];
            c.setTimeInMillis(ps.lastRequested);
            pw.println("PhoneId(" + i + ") active=" + ps.active + ", lastRequest=" + (ps.lastRequested == 0 ? "never" : String.format("%tm-%td %tH:%tM:%tS.%tL", c, c, c, c, c, c)));
        }
        pw.increaseIndent();
        this.mLocalLog.dump(fd, pw, args);
        pw.decreaseIndent();
    }

    private boolean isSimInserted(int phoneId) {
        String iccid = SystemProperties.get(this.PROPERTY_ICCID_SIM[phoneId], UsimPBMemInfo.STRING_NOT_SET);
        if (TextUtils.isEmpty(iccid) || INVALID_ICCID.equals(iccid)) {
            return REQUESTS_UNCHANGED;
        }
        return true;
    }

    private boolean isSkipEimsCheck(NetworkRequest networkRequest) {
        if (networkRequest.networkCapabilities.hasCapability(10)) {
            for (int i = 0; i < this.mNumPhones; i++) {
                if (isSimInserted(i)) {
                    logd("isSkipEimsCheck: not without sim case, don't ignore.");
                    return REQUESTS_UNCHANGED;
                }
            }
            return true;
        }
        logd("isSkipEimsCheck: not include EIMS capability");
        return REQUESTS_UNCHANGED;
    }

    private int getMainCapPhoneId() {
        String curr4GSim = SystemProperties.get("persist.radio.simswitch", UsimPBMemInfo.STRING_NOT_SET);
        logd("current 4G Sim = " + curr4GSim);
        if (curr4GSim == null || curr4GSim.equals(UsimPBMemInfo.STRING_NOT_SET)) {
            return -1;
        }
        int curr4GPhoneId = Integer.parseInt(curr4GSim) - 1;
        return curr4GPhoneId;
    }

    public void reRegisterPsNetwork() {
        for (int phoneId = 0; phoneId < this.mPhones.length; phoneId++) {
            deactivate(phoneId);
        }
        onEvaluate(REQUESTS_UNCHANGED, "doRecovery");
    }

    public void setMaxActivePhones(int count) {
        this.mMaxActivePhones = count;
    }

    public int getMaxActivePhonesCount() {
        return this.mMaxActivePhones;
    }

    public void onModeChanged() {
        onEvaluate(true, "modeChanged");
    }
}
