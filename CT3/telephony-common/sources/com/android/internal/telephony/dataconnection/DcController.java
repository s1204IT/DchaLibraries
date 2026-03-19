package com.android.internal.telephony.dataconnection;

import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.NetworkUtils;
import android.os.AsyncResult;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.telephony.PhoneStateListener;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.DctConstants;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.dataconnection.DataConnection;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.mediatek.internal.telephony.gsm.GsmVTProviderUtil;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;

public class DcController extends StateMachine {
    static final int DATA_CONNECTION_ACTIVE_PH_LINK_DORMANT = 1;
    static final int DATA_CONNECTION_ACTIVE_PH_LINK_INACTIVE = 0;
    static final int DATA_CONNECTION_ACTIVE_PH_LINK_UP = 2;
    static final int DATA_CONNECTION_ACTIVE_UNKNOWN = Integer.MAX_VALUE;
    private static final boolean DBG = true;
    private static final boolean MTK_SRLTE_SUPPORT;
    private static final boolean MTK_SVLTE_SUPPORT;
    private static final String PROP_MTK_CDMA_LTE_MODE = "ro.boot.opt_c2k_lte_mode";
    private static final boolean VDBG = false;
    private HashMap<Integer, DataConnection> mDcListActiveByCid;
    ArrayList<DataConnection> mDcListAll;
    private DcTesterDeactivateAll mDcTesterDeactivateAll;
    private DccDefaultState mDccDefaultState;
    private DcTracker mDct;
    private volatile boolean mExecutingCarrierChange;
    private Phone mPhone;
    private PhoneStateListener mPhoneStateListener;
    TelephonyManager mTelephonyManager;

    static {
        MTK_SVLTE_SUPPORT = SystemProperties.getInt(PROP_MTK_CDMA_LTE_MODE, 0) == 1;
        MTK_SRLTE_SUPPORT = SystemProperties.getInt(PROP_MTK_CDMA_LTE_MODE, 0) == 2;
    }

    private DcController(String name, Phone phone, DcTracker dct, Handler handler) {
        super(name, handler);
        this.mDcListAll = new ArrayList<>();
        this.mDcListActiveByCid = new HashMap<>();
        this.mDccDefaultState = new DccDefaultState(this, null);
        setLogRecSize(300);
        log("E ctor");
        this.mPhone = phone;
        this.mDct = dct;
        addState(this.mDccDefaultState);
        setInitialState(this.mDccDefaultState);
        log("X ctor");
        this.mPhoneStateListener = new PhoneStateListener(handler.getLooper()) {
            public void onCarrierNetworkChange(boolean active) {
                DcController.this.mExecutingCarrierChange = active;
            }
        };
        this.mTelephonyManager = (TelephonyManager) phone.getContext().getSystemService("phone");
        if (this.mTelephonyManager == null) {
            return;
        }
        this.mTelephonyManager.listen(this.mPhoneStateListener, GsmVTProviderUtil.UI_MODE_DESTROY);
    }

    public static DcController makeDcc(Phone phone, DcTracker dct, Handler handler) {
        DcController dcc = new DcController("Dcc", phone, dct, handler);
        dcc.start();
        return dcc;
    }

    void dispose() {
        log("dispose: call quiteNow()");
        if (this.mTelephonyManager != null) {
            this.mTelephonyManager.listen(this.mPhoneStateListener, 0);
        }
        quitNow();
    }

    void addDc(DataConnection dc) {
        this.mDcListAll.add(dc);
    }

    void removeDc(DataConnection dc) {
        this.mDcListActiveByCid.remove(Integer.valueOf(dc.mCid));
        this.mDcListAll.remove(dc);
    }

    public void addActiveDcByCid(DataConnection dc) {
        if (dc.mCid < 0) {
            log("addActiveDcByCid dc.mCid < 0 dc=" + dc);
        }
        this.mDcListActiveByCid.put(Integer.valueOf(dc.mCid), dc);
    }

    void removeActiveDcByCid(DataConnection dc) {
        try {
            DataConnection removedDc = this.mDcListActiveByCid.remove(Integer.valueOf(dc.mCid));
            if (removedDc != null) {
                return;
            }
            log("removeActiveDcByCid removedDc=null dc.mCid=" + dc.mCid);
        } catch (ConcurrentModificationException e) {
            log("concurrentModificationException happened!!");
        }
    }

    boolean isExecutingCarrierChange() {
        return this.mExecutingCarrierChange;
    }

    void getDataCallListForSimLoaded() {
        log("getDataCallList");
        this.mPhone.mCi.getDataCallList(obtainMessage(262159));
    }

    private class DccDefaultState extends State {
        DccDefaultState(DcController this$0, DccDefaultState dccDefaultState) {
            this();
        }

        private DccDefaultState() {
        }

        public void enter() {
            DcController.this.mPhone.mCi.registerForRilConnected(DcController.this.getHandler(), 262149, null);
            DcController.this.mPhone.mCi.registerForDataNetworkStateChanged(DcController.this.getHandler(), 262151, null);
            if (!Build.IS_DEBUGGABLE) {
                return;
            }
            DcController.this.mDcTesterDeactivateAll = new DcTesterDeactivateAll(DcController.this.mPhone, DcController.this, DcController.this.getHandler());
        }

        public void exit() {
            if (DcController.this.mPhone != null) {
                DcController.this.mPhone.mCi.unregisterForRilConnected(DcController.this.getHandler());
                DcController.this.mPhone.mCi.unregisterForDataNetworkStateChanged(DcController.this.getHandler());
            }
            if (DcController.this.mDcTesterDeactivateAll == null) {
                return;
            }
            DcController.this.mDcTesterDeactivateAll.dispose();
        }

        public boolean processMessage(Message msg) {
            switch (msg.what) {
                case 262149:
                    AsyncResult ar = (AsyncResult) msg.obj;
                    if (ar.exception == null) {
                        DcController.this.log("DccDefaultState: msg.what=EVENT_RIL_CONNECTED mRilVersion=" + ar.result);
                    } else {
                        DcController.this.log("DccDefaultState: Unexpected exception on EVENT_RIL_CONNECTED");
                    }
                    break;
                case 262151:
                    AsyncResult ar2 = (AsyncResult) msg.obj;
                    if (ar2.exception == null) {
                        onDataStateChanged((ArrayList) ar2.result);
                    } else {
                        DcController.this.log("DccDefaultState: EVENT_DATA_STATE_CHANGED: exception; likely radio not available, ignore");
                    }
                    break;
                case 262159:
                    AsyncResult ar3 = (AsyncResult) msg.obj;
                    if (ar3.exception == null) {
                        onDataStateChanged((ArrayList) ar3.result);
                    } else {
                        DcController.this.log("DccDefaultState: EVENT_DATA_STATE_CHANGED: exception; likely radio not available, ignore");
                    }
                    DcController.this.mDct.sendMessage(DcController.this.obtainMessage(270840));
                    break;
            }
            return true;
        }

        private void onDataStateChanged(ArrayList<DataCallResponse> dcsList) {
            DcController.this.lr("onDataStateChanged: dcsList=" + dcsList + " mDcListActiveByCid=" + DcController.this.mDcListActiveByCid);
            HashMap<Integer, DataCallResponse> dataCallResponseListByCid = new HashMap<>();
            for (DataCallResponse dcs : dcsList) {
                dataCallResponseListByCid.put(Integer.valueOf(dcs.cid), dcs);
            }
            ArrayList<DataConnection> dcsToRetry = new ArrayList<>();
            for (DataConnection dc : DcController.this.mDcListActiveByCid.values()) {
                if (dataCallResponseListByCid.get(Integer.valueOf(dc.mCid)) == null) {
                    DcController.this.log("onDataStateChanged: add to retry dc=" + dc);
                    dcsToRetry.add(dc);
                }
            }
            DcController.this.log("onDataStateChanged: dcsToRetry=" + dcsToRetry);
            ArrayList<ApnContext> apnsToCleanup = new ArrayList<>();
            boolean isAnyDataCallDormant = false;
            boolean isAnyDataCallActive = false;
            for (DataCallResponse newState : dcsList) {
                DataConnection dc2 = (DataConnection) DcController.this.mDcListActiveByCid.get(Integer.valueOf(newState.cid));
                if (dc2 == null) {
                    DcController.this.loge("onDataStateChanged: no associated DC yet, ignore");
                    DcController.this.loge("Deactivate unlinked PDP context.");
                    DcController.this.mDct.deactivatePdpByCid(newState.cid);
                } else {
                    if (dc2.mApnContexts.size() == 0) {
                        DcController.this.loge("onDataStateChanged: no connected apns, ignore");
                    } else {
                        DcController.this.log("onDataStateChanged: Found ConnId=" + newState.cid + " newState=" + newState.toString());
                        if (newState.active != 0) {
                            DataConnection.UpdateLinkPropertyResult result = dc2.updateLinkProperty(newState);
                            if (result.oldLp.equals(result.newLp)) {
                                DcController.this.log("onDataStateChanged: no change");
                            } else if (!result.oldLp.isIdenticalInterfaceName(result.newLp)) {
                                apnsToCleanup.addAll(dc2.mApnContexts.keySet());
                                DcController.this.log("onDataStateChanged: interface change, cleanup apns=" + dc2.mApnContexts);
                            } else if (result.oldLp.isIdenticalDnses(result.newLp) && result.oldLp.isIdenticalRoutes(result.newLp) && result.oldLp.isIdenticalHttpProxy(result.newLp) && DcController.this.isIpMatched(result.oldLp, result.newLp)) {
                                DcController.this.log("onDataStateChanged: no changes");
                            } else {
                                LinkProperties.CompareResult<LinkAddress> car = result.oldLp.compareAddresses(result.newLp);
                                DcController.this.log("onDataStateChanged: oldLp=" + result.oldLp + " newLp=" + result.newLp + " car=" + car);
                                boolean needToClean = false;
                                for (LinkAddress added : car.added) {
                                    Iterator removed$iterator = car.removed.iterator();
                                    while (true) {
                                        if (removed$iterator.hasNext()) {
                                            LinkAddress removed = (LinkAddress) removed$iterator.next();
                                            if (NetworkUtils.addressTypeMatches(removed.getAddress(), added.getAddress())) {
                                                needToClean = true;
                                                break;
                                            }
                                        }
                                    }
                                }
                                if ((DcController.MTK_SVLTE_SUPPORT || DcController.MTK_SRLTE_SUPPORT) && DcController.this.mPhone.getPhoneType() == 2) {
                                    DcController.this.log("onDataStateChanged: IRAT set needToClean false");
                                    needToClean = false;
                                }
                                if (needToClean) {
                                    DcController.this.log("onDataStateChanged: addr change, cleanup apns=" + dc2.mApnContexts + " oldLp=" + result.oldLp + " newLp=" + result.newLp);
                                    apnsToCleanup.addAll(dc2.mApnContexts.keySet());
                                } else {
                                    DcController.this.log("onDataStateChanged: simple change");
                                    for (ApnContext apnContext : dc2.mApnContexts.keySet()) {
                                        DcController.this.mPhone.notifyDataConnection("linkPropertiesChanged", apnContext.getApnType());
                                    }
                                }
                            }
                        } else if (DcController.this.mDct.isCleanupRequired.get()) {
                            apnsToCleanup.addAll(dc2.mApnContexts.keySet());
                            DcController.this.mDct.isCleanupRequired.set(false);
                        } else {
                            DcFailCause failCause = DcFailCause.fromInt(newState.status);
                            if (failCause.isRestartRadioFail()) {
                                DcController.this.log("onDataStateChanged: X restart radio, failCause=" + failCause);
                                DcController.this.mDct.sendRestartRadio();
                            } else if (DcController.this.mDct.isPermanentFail(failCause)) {
                                DcController.this.log("onDataStateChanged: inactive, add to cleanup list. failCause=" + failCause);
                                apnsToCleanup.addAll(dc2.mApnContexts.keySet());
                            } else {
                                DcController.this.log("onDataStateChanged: inactive, add to retry list. failCause=" + failCause);
                                dcsToRetry.add(dc2);
                            }
                        }
                    }
                    if (newState.active == 2) {
                        isAnyDataCallActive = true;
                    }
                    if (newState.active == 1) {
                        isAnyDataCallDormant = true;
                    }
                }
            }
            if (!isAnyDataCallDormant || isAnyDataCallActive) {
                DcController.this.log("onDataStateChanged: Data Activity updated to NONE. isAnyDataCallActive = " + isAnyDataCallActive + " isAnyDataCallDormant = " + isAnyDataCallDormant);
                if (isAnyDataCallActive) {
                    DcController.this.mDct.sendStartNetStatPoll(DctConstants.Activity.NONE);
                }
            } else {
                DcController.this.log("onDataStateChanged: Data Activity updated to DORMANT. stopNetStatePoll");
                DcController.this.mDct.sendStopNetStatPoll(DctConstants.Activity.DORMANT);
            }
            DcController.this.lr("onDataStateChanged: dcsToRetry=" + dcsToRetry + " apnsToCleanup=" + apnsToCleanup);
            for (ApnContext apnContext2 : apnsToCleanup) {
                DcController.this.mDct.sendCleanUpConnection(true, apnContext2);
            }
            for (DataConnection dc3 : dcsToRetry) {
                DcController.this.log("onDataStateChanged: send EVENT_LOST_CONNECTION dc.mTag=" + dc3.mTag);
                dc3.sendMessage(262153, dc3.mTag);
            }
        }
    }

    private void lr(String s) {
        logAndAddLogRec(s);
    }

    protected void log(String s) {
        Rlog.d(getName(), s);
    }

    protected void loge(String s) {
        Rlog.e(getName(), s);
    }

    protected String getWhatToString(int what) {
        String info = DataConnection.cmdToString(what);
        if (info == null) {
            return DcAsyncChannel.cmdToString(what);
        }
        return info;
    }

    public String toString() {
        return "mDcListAll=" + this.mDcListAll + " mDcListActiveByCid=" + this.mDcListActiveByCid;
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        super.dump(fd, pw, args);
        pw.println(" mPhone=" + this.mPhone);
        pw.println(" mDcListAll=" + this.mDcListAll);
        pw.println(" mDcListActiveByCid=" + this.mDcListActiveByCid);
    }

    private boolean isIpMatched(LinkProperties oldLp, LinkProperties newLp) {
        if (oldLp.isIdenticalAddresses(newLp)) {
            return true;
        }
        log("isIpMatched: address count is different but matched");
        return newLp.getAddresses().containsAll(oldLp.getAddresses());
    }
}
