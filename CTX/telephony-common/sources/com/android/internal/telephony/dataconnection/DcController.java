package com.android.internal.telephony.dataconnection;

import android.hardware.radio.V1_2.ScanIntervalRange;
import android.net.INetworkPolicyListener;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.NetworkPolicyManager;
import android.net.NetworkUtils;
import android.os.AsyncResult;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.telephony.PhoneStateListener;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import android.telephony.data.DataCallResponse;
import com.android.internal.telephony.DctConstants;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.TelephonyComponentFactory;
import com.android.internal.telephony.dataconnection.DataConnection;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class DcController extends StateMachine {
    static final int DATA_CONNECTION_ACTIVE_UNKNOWN = Integer.MAX_VALUE;
    protected static final boolean DBG = true;
    protected static final boolean VDBG = false;
    protected final int DATA_CONNECTION_ACTIVE_PH_LINK_DORMANT;
    protected final int DATA_CONNECTION_ACTIVE_PH_LINK_INACTIVE;
    protected final int DATA_CONNECTION_ACTIVE_PH_LINK_UP;
    protected final DataServiceManager mDataServiceManager;
    protected final HashMap<Integer, DataConnection> mDcListActiveByCid;
    protected final ArrayList<DataConnection> mDcListAll;
    private final DcTesterDeactivateAll mDcTesterDeactivateAll;
    protected DccDefaultState mDccDefaultState;
    protected final DcTracker mDct;
    private volatile boolean mExecutingCarrierChange;
    private final INetworkPolicyListener mListener;
    final NetworkPolicyManager mNetworkPolicyManager;
    protected final Phone mPhone;
    protected PhoneStateListener mPhoneStateListener;
    protected final TelephonyManager mTelephonyManager;

    public DcController(String str, Phone phone, DcTracker dcTracker, DataServiceManager dataServiceManager, Handler handler) {
        DcTesterDeactivateAll dcTesterDeactivateAll;
        super(str, handler);
        this.mDcListAll = new ArrayList<>();
        this.mDcListActiveByCid = new HashMap<>();
        this.DATA_CONNECTION_ACTIVE_PH_LINK_INACTIVE = 0;
        this.DATA_CONNECTION_ACTIVE_PH_LINK_DORMANT = 1;
        this.DATA_CONNECTION_ACTIVE_PH_LINK_UP = 2;
        this.mDccDefaultState = new DccDefaultState();
        this.mListener = new NetworkPolicyManager.Listener() {
            public void onSubscriptionOverride(int i, int i2, int i3) {
                HashMap map;
                if (DcController.this.mPhone == null || DcController.this.mPhone.getSubId() != i) {
                    return;
                }
                synchronized (DcController.this.mDcListAll) {
                    map = new HashMap(DcController.this.mDcListActiveByCid);
                }
                Iterator it = map.values().iterator();
                while (it.hasNext()) {
                    ((DataConnection) it.next()).onSubscriptionOverride(i2, i3);
                }
            }
        };
        setLogRecSize(ScanIntervalRange.MAX);
        log("E ctor");
        this.mPhone = phone;
        this.mDct = dcTracker;
        this.mDataServiceManager = dataServiceManager;
        mtkReplaceStates();
        addState(this.mDccDefaultState);
        setInitialState(this.mDccDefaultState);
        log("X ctor");
        this.mPhoneStateListener = new PhoneStateListener(handler.getLooper()) {
            public void onCarrierNetworkChange(boolean z) {
                DcController.this.mExecutingCarrierChange = z;
            }
        };
        this.mTelephonyManager = (TelephonyManager) phone.getContext().getSystemService("phone");
        this.mNetworkPolicyManager = (NetworkPolicyManager) phone.getContext().getSystemService("netpolicy");
        if (Build.IS_DEBUGGABLE) {
            dcTesterDeactivateAll = new DcTesterDeactivateAll(this.mPhone, this, getHandler());
        } else {
            dcTesterDeactivateAll = null;
        }
        this.mDcTesterDeactivateAll = dcTesterDeactivateAll;
        if (this.mTelephonyManager != null) {
            this.mTelephonyManager.listen(this.mPhoneStateListener, 65536);
        }
    }

    public static DcController makeDcc(Phone phone, DcTracker dcTracker, DataServiceManager dataServiceManager, Handler handler) {
        return TelephonyComponentFactory.getInstance().makeDcController("Dcc", phone, dcTracker, dataServiceManager, handler);
    }

    void dispose() {
        log("dispose: call quiteNow()");
        if (this.mTelephonyManager != null) {
            this.mTelephonyManager.listen(this.mPhoneStateListener, 0);
        }
        quitNow();
    }

    public void addDc(DataConnection dataConnection) {
        synchronized (this.mDcListAll) {
            this.mDcListAll.add(dataConnection);
        }
    }

    public void removeDc(DataConnection dataConnection) {
        synchronized (this.mDcListAll) {
            this.mDcListActiveByCid.remove(Integer.valueOf(dataConnection.mCid));
            this.mDcListAll.remove(dataConnection);
        }
    }

    public void addActiveDcByCid(DataConnection dataConnection) {
        if (dataConnection.mCid < 0) {
            log("addActiveDcByCid dc.mCid < 0 dc=" + dataConnection);
        }
        synchronized (this.mDcListAll) {
            this.mDcListActiveByCid.put(Integer.valueOf(dataConnection.mCid), dataConnection);
        }
    }

    public DataConnection getActiveDcByCid(int i) {
        DataConnection dataConnection;
        synchronized (this.mDcListAll) {
            dataConnection = this.mDcListActiveByCid.get(Integer.valueOf(i));
        }
        return dataConnection;
    }

    protected void removeActiveDcByCid(DataConnection dataConnection) {
        synchronized (this.mDcListAll) {
            if (this.mDcListActiveByCid.remove(Integer.valueOf(dataConnection.mCid)) == null) {
                log("removeActiveDcByCid removedDc=null dc=" + dataConnection);
            }
        }
    }

    boolean isExecutingCarrierChange() {
        return this.mExecutingCarrierChange;
    }

    protected class DccDefaultState extends State {
        protected DccDefaultState() {
        }

        public void enter() {
            if (DcController.this.mPhone != null && DcController.this.mDataServiceManager.getTransportType() == 1) {
                DcController.this.mPhone.mCi.registerForRilConnected(DcController.this.getHandler(), DataConnection.EVENT_RIL_CONNECTED, null);
            }
            DcController.this.mDataServiceManager.registerForDataCallListChanged(DcController.this.getHandler(), DataConnection.EVENT_DATA_STATE_CHANGED);
            if (DcController.this.mNetworkPolicyManager != null) {
                DcController.this.mNetworkPolicyManager.registerListener(DcController.this.mListener);
            }
        }

        public void exit() {
            if ((DcController.this.mPhone != null) & (DcController.this.mDataServiceManager.getTransportType() == 1)) {
                DcController.this.mPhone.mCi.unregisterForRilConnected(DcController.this.getHandler());
            }
            DcController.this.mDataServiceManager.unregisterForDataCallListChanged(DcController.this.getHandler());
            if (DcController.this.mDcTesterDeactivateAll != null) {
                DcController.this.mDcTesterDeactivateAll.dispose();
            }
            if (DcController.this.mNetworkPolicyManager != null) {
                DcController.this.mNetworkPolicyManager.unregisterListener(DcController.this.mListener);
            }
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            if (i != 262149) {
                if (i == 262151) {
                    AsyncResult asyncResult = (AsyncResult) message.obj;
                    if (asyncResult.exception == null) {
                        onDataStateChanged((ArrayList) asyncResult.result);
                        return true;
                    }
                    DcController.this.log("DccDefaultState: EVENT_DATA_STATE_CHANGED: exception; likely radio not available, ignore");
                    return true;
                }
                return true;
            }
            AsyncResult asyncResult2 = (AsyncResult) message.obj;
            if (asyncResult2.exception == null) {
                DcController.this.log("DccDefaultState: msg.what=EVENT_RIL_CONNECTED mRilVersion=" + asyncResult2.result);
                return true;
            }
            DcController.this.log("DccDefaultState: Unexpected exception on EVENT_RIL_CONNECTED");
            return true;
        }

        protected void onDataStateChanged(ArrayList<DataCallResponse> arrayList) {
            HashMap map;
            HashMap map2;
            HashMap map3;
            synchronized (DcController.this.mDcListAll) {
                new ArrayList(DcController.this.mDcListAll);
                map = new HashMap(DcController.this.mDcListActiveByCid);
            }
            DcController.this.lr("onDataStateChanged: dcsList=" + arrayList + " dcListActiveByCid=" + map);
            HashMap map4 = new HashMap();
            for (DataCallResponse dataCallResponse : arrayList) {
                map4.put(Integer.valueOf(dataCallResponse.getCallId()), dataCallResponse);
            }
            ArrayList<DataConnection> arrayList2 = new ArrayList();
            for (DataConnection dataConnection : map.values()) {
                if (map4.get(Integer.valueOf(dataConnection.mCid)) == null) {
                    DcController.this.log("onDataStateChanged: add to retry dc=" + dataConnection);
                    arrayList2.add(dataConnection);
                }
            }
            DcController.this.log("onDataStateChanged: dcsToRetry=" + arrayList2);
            ArrayList arrayList3 = new ArrayList();
            boolean z = false;
            boolean z2 = false;
            boolean z3 = false;
            for (DataCallResponse dataCallResponse2 : arrayList) {
                DataConnection dataConnection2 = (DataConnection) map.get(Integer.valueOf(dataCallResponse2.getCallId()));
                if (dataConnection2 == null) {
                    DcController.this.loge("onDataStateChanged: no associated DC yet, ignore");
                } else {
                    if (dataConnection2.mApnContexts.size() == 0) {
                        DcController.this.loge("onDataStateChanged: no connected apns, ignore");
                    } else {
                        DcController.this.log("onDataStateChanged: Found ConnId=" + dataCallResponse2.getCallId() + " newState=" + dataCallResponse2.toString());
                        if (dataCallResponse2.getActive() == 0) {
                            if (DcController.this.mDct.isCleanupRequired.get()) {
                                arrayList3.addAll(dataConnection2.mApnContexts.keySet());
                                DcController.this.mDct.isCleanupRequired.set(z);
                            } else {
                                DcFailCause dcFailCauseFromInt = DcFailCause.fromInt(dataCallResponse2.getStatus());
                                if (dcFailCauseFromInt.isRestartRadioFail(DcController.this.mPhone.getContext(), DcController.this.mPhone.getSubId())) {
                                    DcController.this.log("onDataStateChanged: X restart radio, failCause=" + dcFailCauseFromInt);
                                    DcController.this.mDct.sendRestartRadio();
                                } else if (DcController.this.mDct.isPermanentFailure(dcFailCauseFromInt)) {
                                    DcController.this.log("onDataStateChanged: inactive, add to cleanup list. failCause=" + dcFailCauseFromInt);
                                    arrayList3.addAll(dataConnection2.mApnContexts.keySet());
                                } else {
                                    DcController.this.log("onDataStateChanged: inactive, add to retry list. failCause=" + dcFailCauseFromInt);
                                    arrayList2.add(dataConnection2);
                                }
                            }
                        } else {
                            DataConnection.UpdateLinkPropertyResult updateLinkPropertyResultUpdateLinkProperty = dataConnection2.updateLinkProperty(dataCallResponse2);
                            if (updateLinkPropertyResultUpdateLinkProperty.oldLp.equals(updateLinkPropertyResultUpdateLinkProperty.newLp)) {
                                DcController.this.log("onDataStateChanged: no change");
                            } else {
                                if (updateLinkPropertyResultUpdateLinkProperty.oldLp.isIdenticalInterfaceName(updateLinkPropertyResultUpdateLinkProperty.newLp)) {
                                    if (!updateLinkPropertyResultUpdateLinkProperty.oldLp.isIdenticalDnses(updateLinkPropertyResultUpdateLinkProperty.newLp) || !updateLinkPropertyResultUpdateLinkProperty.oldLp.isIdenticalRoutes(updateLinkPropertyResultUpdateLinkProperty.newLp) || !updateLinkPropertyResultUpdateLinkProperty.oldLp.isIdenticalHttpProxy(updateLinkPropertyResultUpdateLinkProperty.newLp) || !updateLinkPropertyResultUpdateLinkProperty.oldLp.isIdenticalAddresses(updateLinkPropertyResultUpdateLinkProperty.newLp)) {
                                        LinkProperties.CompareResult compareResultCompareAddresses = updateLinkPropertyResultUpdateLinkProperty.oldLp.compareAddresses(updateLinkPropertyResultUpdateLinkProperty.newLp);
                                        DcController.this.log("onDataStateChanged: oldLp=" + updateLinkPropertyResultUpdateLinkProperty.oldLp + " newLp=" + updateLinkPropertyResultUpdateLinkProperty.newLp + " car=" + compareResultCompareAddresses);
                                        boolean z4 = z;
                                        for (LinkAddress linkAddress : compareResultCompareAddresses.added) {
                                            Iterator it = compareResultCompareAddresses.removed.iterator();
                                            while (true) {
                                                if (it.hasNext()) {
                                                    map3 = map;
                                                    if (!NetworkUtils.addressTypeMatches(((LinkAddress) it.next()).getAddress(), linkAddress.getAddress())) {
                                                        map = map3;
                                                    } else {
                                                        z4 = true;
                                                        break;
                                                    }
                                                } else {
                                                    map3 = map;
                                                    break;
                                                }
                                            }
                                            map = map3;
                                        }
                                        map2 = map;
                                        if (z4) {
                                            DcController.this.log("onDataStateChanged: addr change, cleanup apns=" + dataConnection2.mApnContexts + " oldLp=" + updateLinkPropertyResultUpdateLinkProperty.oldLp + " newLp=" + updateLinkPropertyResultUpdateLinkProperty.newLp);
                                            arrayList3.addAll(dataConnection2.mApnContexts.keySet());
                                        } else {
                                            DcController.this.log("onDataStateChanged: simple change");
                                            Iterator<ApnContext> it2 = dataConnection2.mApnContexts.keySet().iterator();
                                            while (it2.hasNext()) {
                                                DcController.this.mPhone.notifyDataConnection("linkPropertiesChanged", it2.next().getApnType());
                                            }
                                        }
                                    } else {
                                        DcController.this.log("onDataStateChanged: no changes");
                                    }
                                } else {
                                    map2 = map;
                                    arrayList3.addAll(dataConnection2.mApnContexts.keySet());
                                    DcController.this.log("onDataStateChanged: interface change, cleanup apns=" + dataConnection2.mApnContexts);
                                }
                                if (dataCallResponse2.getActive() == 2) {
                                    z3 = true;
                                }
                                if (dataCallResponse2.getActive() == 1) {
                                    z2 = true;
                                }
                                map = map2;
                                z = false;
                            }
                        }
                    }
                    map2 = map;
                    if (dataCallResponse2.getActive() == 2) {
                    }
                    if (dataCallResponse2.getActive() == 1) {
                    }
                    map = map2;
                    z = false;
                }
            }
            if (z2 && !z3) {
                DcController.this.log("onDataStateChanged: Data Activity updated to DORMANT. stopNetStatePoll");
                DcController.this.mDct.sendStopNetStatPoll(DctConstants.Activity.DORMANT);
            } else {
                DcController.this.log("onDataStateChanged: Data Activity updated to NONE. isAnyDataCallActive = " + z3 + " isAnyDataCallDormant = " + z2);
                if (z3) {
                    DcController.this.mDct.sendStartNetStatPoll(DctConstants.Activity.NONE);
                }
            }
            DcController.this.lr("onDataStateChanged: dcsToRetry=" + arrayList2 + " apnsToCleanup=" + arrayList3);
            Iterator it3 = arrayList3.iterator();
            while (it3.hasNext()) {
                DcController.this.mDct.sendCleanUpConnection(true, (ApnContext) it3.next());
            }
            for (DataConnection dataConnection3 : arrayList2) {
                DcController.this.log("onDataStateChanged: send EVENT_LOST_CONNECTION dc.mTag=" + dataConnection3.mTag);
                dataConnection3.sendMessage(DataConnection.EVENT_LOST_CONNECTION, dataConnection3.mTag);
            }
        }
    }

    protected void lr(String str) {
        logAndAddLogRec(str);
    }

    protected void log(String str) {
        Rlog.d(getName(), str);
    }

    protected void loge(String str) {
        Rlog.e(getName(), str);
    }

    protected String getWhatToString(int i) {
        String strCmdToString = DataConnection.cmdToString(i);
        if (strCmdToString == null) {
            return DcAsyncChannel.cmdToString(i);
        }
        return strCmdToString;
    }

    public String toString() {
        String str;
        synchronized (this.mDcListAll) {
            str = "mDcListAll=" + this.mDcListAll + " mDcListActiveByCid=" + this.mDcListActiveByCid;
        }
        return str;
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        super.dump(fileDescriptor, printWriter, strArr);
        printWriter.println(" mPhone=" + this.mPhone);
        synchronized (this.mDcListAll) {
            printWriter.println(" mDcListAll=" + this.mDcListAll);
            printWriter.println(" mDcListActiveByCid=" + this.mDcListActiveByCid);
        }
    }

    protected void mtkReplaceStates() {
    }
}
