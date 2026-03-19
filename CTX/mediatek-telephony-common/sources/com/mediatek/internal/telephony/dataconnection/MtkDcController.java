package com.mediatek.internal.telephony.dataconnection;

import android.hardware.radio.V1_0.SetupDataCallResult;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.NetworkUtils;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.telephony.data.DataCallResponse;
import com.android.internal.telephony.DctConstants;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.dataconnection.ApnContext;
import com.android.internal.telephony.dataconnection.CellularDataService;
import com.android.internal.telephony.dataconnection.DataConnection;
import com.android.internal.telephony.dataconnection.DataServiceManager;
import com.android.internal.telephony.dataconnection.DcController;
import com.android.internal.telephony.dataconnection.DcFailCause;
import com.android.internal.telephony.dataconnection.DcTracker;
import com.mediatek.internal.telephony.datasub.DataSubConstants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class MtkDcController extends DcController {
    private static final boolean MTK_SRLTE_SUPPORT;
    private static final boolean MTK_SVLTE_SUPPORT;
    private static final String PROP_MTK_CDMA_LTE_MODE = "ro.boot.opt_c2k_lte_mode";
    private CellularDataService mCellularDataService;

    static {
        MTK_SVLTE_SUPPORT = SystemProperties.getInt(PROP_MTK_CDMA_LTE_MODE, 0) == 1;
        MTK_SRLTE_SUPPORT = SystemProperties.getInt(PROP_MTK_CDMA_LTE_MODE, 0) == 2;
    }

    public MtkDcController(String str, Phone phone, DcTracker dcTracker, DataServiceManager dataServiceManager, Handler handler) {
        super(str, phone, dcTracker, dataServiceManager, handler);
        this.mCellularDataService = new CellularDataService();
    }

    public void removeDc(DataConnection dataConnection) {
        super.removeDc(dataConnection);
        log("removeDc: " + dataConnection);
    }

    public void addActiveDcByCid(DataConnection dataConnection) {
        super.addActiveDcByCid(dataConnection);
        log("addActiveDcByCid: " + dataConnection);
    }

    protected void removeActiveDcByCid(DataConnection dataConnection) {
        super.removeActiveDcByCid(dataConnection);
        log("removeActiveDcByCid: " + dataConnection);
    }

    protected class MtkDccDefaultState extends DcController.DccDefaultState {
        protected MtkDccDefaultState() {
            super(MtkDcController.this);
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            if (i == 262149) {
                AsyncResult asyncResult = (AsyncResult) message.obj;
                if (asyncResult.exception != null) {
                    MtkDcController.this.log("DccDefaultState: Unexpected exception on EVENT_RIL_CONNECTED");
                } else {
                    MtkDcController.this.log("DccDefaultState: msg.what=EVENT_RIL_CONNECTED mRilVersion=" + asyncResult.result);
                    MtkDcController.this.mPhone.mCi.getDataCallList(MtkDcController.this.obtainMessage(262174));
                }
            } else if (i == 262174) {
                AsyncResult asyncResult2 = (AsyncResult) message.obj;
                if (asyncResult2.exception == null) {
                    onDataStateChanged(MtkDcController.this.getDataCallList((ArrayList) asyncResult2.result));
                } else {
                    MtkDcController.this.log("DccDefaultState: EVENT_DATA_STATE_CHANGED: exception; likely radio not available, ignore");
                }
            } else {
                return super.processMessage(message);
            }
            return true;
        }

        protected void onDataStateChanged(ArrayList<DataCallResponse> arrayList) {
            HashMap map;
            HashMap map2;
            HashMap map3;
            synchronized (MtkDcController.this.mDcListAll) {
                new ArrayList(MtkDcController.this.mDcListAll);
                map = new HashMap(MtkDcController.this.mDcListActiveByCid);
            }
            MtkDcController.this.lr("onDataStateChanged: dcsList=" + arrayList + " dcListActiveByCid=" + map);
            HashMap map4 = new HashMap();
            for (DataCallResponse dataCallResponse : arrayList) {
                map4.put(Integer.valueOf(dataCallResponse.getCallId()), dataCallResponse);
            }
            ArrayList<DataConnection> arrayList2 = new ArrayList();
            for (DataConnection dataConnection : map.values()) {
                if (map4.get(Integer.valueOf(dataConnection.mCid)) == null) {
                    MtkDcController.this.log("onDataStateChanged: add to retry dc=" + dataConnection);
                    arrayList2.add(dataConnection);
                }
            }
            MtkDcController.this.log("onDataStateChanged: dcsToRetry=" + arrayList2);
            ArrayList arrayList3 = new ArrayList();
            boolean z = false;
            boolean z2 = false;
            boolean z3 = false;
            for (DataCallResponse dataCallResponse2 : arrayList) {
                int active = Integer.MAX_VALUE;
                DataConnection dataConnection2 = (DataConnection) map.get(Integer.valueOf(dataCallResponse2.getCallId()));
                if (dataConnection2 == null) {
                    MtkDcController.this.loge("onDataStateChanged: no associated DC yet, ignore");
                    MtkDcController.this.loge("Deactivate unlinked PDP context.");
                    ((MtkDcTracker) MtkDcController.this.mDct).deactivatePdpByCid(dataCallResponse2.getCallId());
                } else {
                    if (dataConnection2.mApnContexts.size() == 0) {
                        MtkDcController.this.loge("onDataStateChanged: no connected apns, ignore");
                    } else {
                        MtkDcController.this.log("onDataStateChanged: Found ConnId=" + dataCallResponse2.getCallId() + " newState=" + dataCallResponse2.toString());
                        MtkDataConnection mtkDataConnection = (MtkDataConnection) dataConnection2;
                        mtkDataConnection.setConnectionRat(mtkDataConnection.decodeRat(dataCallResponse2.getActive()), "data call list");
                        active = dataCallResponse2.getActive() % 1000;
                        if (active == 0) {
                            if (MtkDcController.this.mDct.isCleanupRequired.get()) {
                                arrayList3.addAll(dataConnection2.mApnContexts.keySet());
                                MtkDcController.this.mDct.isCleanupRequired.set(z);
                            } else {
                                DcFailCause dcFailCauseFromInt = DcFailCause.fromInt(dataCallResponse2.getStatus());
                                if (!dcFailCauseFromInt.isRestartRadioFail(MtkDcController.this.mPhone.getContext(), MtkDcController.this.mPhone.getSubId())) {
                                    if (MtkDcController.this.mDct.isPermanentFailure(dcFailCauseFromInt)) {
                                        MtkDcController.this.log("onDataStateChanged: inactive, add to cleanup list. failCause=" + dcFailCauseFromInt);
                                        arrayList3.addAll(dataConnection2.mApnContexts.keySet());
                                    } else {
                                        MtkDcController.this.log("onDataStateChanged: inactive, add to retry list. failCause=" + dcFailCauseFromInt);
                                        arrayList2.add(dataConnection2);
                                    }
                                } else {
                                    MtkDcController.this.log("onDataStateChanged: X restart radio, failCause=" + dcFailCauseFromInt);
                                    MtkDcController.this.mDct.sendRestartRadio();
                                }
                            }
                        } else {
                            DataConnection.UpdateLinkPropertyResult updateLinkPropertyResultUpdateLinkProperty = dataConnection2.updateLinkProperty(dataCallResponse2);
                            if (updateLinkPropertyResultUpdateLinkProperty.oldLp.equals(updateLinkPropertyResultUpdateLinkProperty.newLp)) {
                                MtkDcController.this.log("onDataStateChanged: no change");
                            } else {
                                if (updateLinkPropertyResultUpdateLinkProperty.oldLp.isIdenticalInterfaceName(updateLinkPropertyResultUpdateLinkProperty.newLp)) {
                                    if (updateLinkPropertyResultUpdateLinkProperty.oldLp.isIdenticalDnses(updateLinkPropertyResultUpdateLinkProperty.newLp) && updateLinkPropertyResultUpdateLinkProperty.oldLp.isIdenticalRoutes(updateLinkPropertyResultUpdateLinkProperty.newLp) && updateLinkPropertyResultUpdateLinkProperty.oldLp.isIdenticalHttpProxy(updateLinkPropertyResultUpdateLinkProperty.newLp) && MtkDcController.this.isIpMatched(updateLinkPropertyResultUpdateLinkProperty.oldLp, updateLinkPropertyResultUpdateLinkProperty.newLp)) {
                                        MtkDcController.this.log("onDataStateChanged: no changes");
                                    } else {
                                        LinkProperties.CompareResult compareResultCompareAddresses = updateLinkPropertyResultUpdateLinkProperty.oldLp.compareAddresses(updateLinkPropertyResultUpdateLinkProperty.newLp);
                                        MtkDcController.this.log("onDataStateChanged: oldLp=" + updateLinkPropertyResultUpdateLinkProperty.oldLp + " newLp=" + updateLinkPropertyResultUpdateLinkProperty.newLp + " car=" + compareResultCompareAddresses);
                                        boolean z4 = false;
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
                                        if ((MtkDcController.MTK_SVLTE_SUPPORT || MtkDcController.MTK_SRLTE_SUPPORT) && MtkDcController.this.mPhone.getPhoneType() == 2) {
                                            MtkDcController.this.log("onDataStateChanged: IRAT set needToClean false");
                                        } else {
                                            if ("OP07".equals(SystemProperties.get(DataSubConstants.PROPERTY_OPERATOR_OPTR))) {
                                                MtkDcController.this.log("onDataStateChanged: OP07 set needToClean false");
                                            }
                                            if (z4) {
                                                MtkDcController.this.log("onDataStateChanged: simple change");
                                                Iterator it2 = dataConnection2.mApnContexts.keySet().iterator();
                                                while (it2.hasNext()) {
                                                    MtkDcController.this.mPhone.notifyDataConnection("linkPropertiesChanged", ((ApnContext) it2.next()).getApnType());
                                                }
                                            } else {
                                                MtkDcController.this.log("onDataStateChanged: addr change, cleanup apns=" + dataConnection2.mApnContexts + " oldLp=" + updateLinkPropertyResultUpdateLinkProperty.oldLp + " newLp=" + updateLinkPropertyResultUpdateLinkProperty.newLp);
                                                arrayList3.addAll(dataConnection2.mApnContexts.keySet());
                                            }
                                        }
                                        z4 = false;
                                        if (z4) {
                                        }
                                    }
                                } else {
                                    map2 = map;
                                    arrayList3.addAll(dataConnection2.mApnContexts.keySet());
                                    MtkDcController.this.log("onDataStateChanged: interface change, cleanup apns=" + dataConnection2.mApnContexts);
                                }
                                if (active == 2) {
                                    z3 = true;
                                }
                                if (active == 1) {
                                    z2 = true;
                                }
                                map = map2;
                                z = false;
                            }
                        }
                    }
                    map2 = map;
                    if (active == 2) {
                    }
                    if (active == 1) {
                    }
                    map = map2;
                    z = false;
                }
            }
            if (z2 && !z3) {
                MtkDcController.this.log("onDataStateChanged: Data Activity updated to DORMANT. stopNetStatePoll");
                MtkDcController.this.mDct.sendStopNetStatPoll(DctConstants.Activity.DORMANT);
            } else {
                MtkDcController.this.log("onDataStateChanged: Data Activity updated to NONE. isAnyDataCallActive = " + z3 + " isAnyDataCallDormant = " + z2);
                if (z3) {
                    MtkDcController.this.mDct.sendStartNetStatPoll(DctConstants.Activity.NONE);
                }
            }
            MtkDcController.this.lr("onDataStateChanged: dcsToRetry=" + arrayList2 + " apnsToCleanup=" + arrayList3);
            Iterator it3 = arrayList3.iterator();
            while (it3.hasNext()) {
                MtkDcController.this.mDct.sendCleanUpConnection(true, (ApnContext) it3.next());
            }
            for (DataConnection dataConnection3 : arrayList2) {
                MtkDcController.this.log("onDataStateChanged: send EVENT_LOST_CONNECTION dc.mTag=" + dataConnection3.mTag);
                dataConnection3.sendMessage(262153, dataConnection3.mTag);
            }
        }
    }

    private boolean isIpMatched(LinkProperties linkProperties, LinkProperties linkProperties2) {
        if (linkProperties.isIdenticalAddresses(linkProperties2)) {
            return true;
        }
        log("isIpMatched: address count is different but matched");
        return linkProperties2.getAddresses().containsAll(linkProperties.getAddresses());
    }

    protected void mtkReplaceStates() {
        this.mDccDefaultState = new MtkDccDefaultState();
    }

    int getActiveDcCount() {
        return this.mDcListActiveByCid.size();
    }

    private ArrayList<DataCallResponse> getDataCallList(ArrayList<SetupDataCallResult> arrayList) {
        ArrayList<DataCallResponse> arrayList2 = new ArrayList<>();
        Iterator<SetupDataCallResult> it = arrayList.iterator();
        while (it.hasNext()) {
            arrayList2.add(this.mCellularDataService.convertDataCallResult(it.next()));
        }
        return arrayList2;
    }
}
