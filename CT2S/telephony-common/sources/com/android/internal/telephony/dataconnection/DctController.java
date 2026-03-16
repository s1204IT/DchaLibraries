package com.android.internal.telephony.dataconnection;

import android.content.Context;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkFactory;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.provider.Settings;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.util.SparseArray;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.Dsds;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneBase;
import com.android.internal.telephony.PhoneProxy;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.dataconnection.DcSwitchAsyncChannel;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class DctController extends Handler {
    private static final boolean DBG = true;
    private static final int EVENT_DATA_ATTACHED = 500;
    private static final int EVENT_DATA_DETACHED = 600;
    private static final int EVENT_EXECUTE_ALL_REQUESTS = 102;
    private static final int EVENT_EXECUTE_REQUEST = 101;
    private static final int EVENT_PROCESS_REQUESTS = 100;
    private static final int EVENT_RADIO_AVAILABLE = 800;
    private static final int EVENT_RADIO_NOT_AVAILABLE = 700;
    private static final int EVENT_RELEASE_ALL_REQUESTS = 104;
    private static final int EVENT_RELEASE_REQUEST = 103;
    private static final String LOG_TAG = "DctController";
    private static DctController sDctController;
    private boolean mCanProcessMsg;
    private Context mContext;
    private DcSwitchAsyncChannel[] mDcSwitchAsyncChannel;
    private Handler[] mDcSwitchStateHandler;
    private DcSwitchStateMachine[] mDcSwitchStateMachine;
    private NetworkFactory[] mNetworkFactory;
    private Messenger[] mNetworkFactoryMessenger;
    private NetworkCapabilities[] mNetworkFilter;
    private int mPhoneNum;
    private PhoneProxy[] mPhones;
    private SubscriptionManager mSubMgr;
    private HashMap<Integer, DcSwitchAsyncChannel.RequestInfo> mRequestInfos = new HashMap<>();
    private SubscriptionController mSubController = SubscriptionController.getInstance();
    private SubscriptionManager.OnSubscriptionsChangedListener mOnSubscriptionsChangedListener = new SubscriptionManager.OnSubscriptionsChangedListener() {
        @Override
        public void onSubscriptionsChanged() {
            DctController.this.onSubInfoReady();
        }
    };
    private ContentObserver mObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            DctController.logd("Settings change");
            DctController.this.onSettingsChange();
        }
    };
    private Handler mRspHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what >= DctController.EVENT_RADIO_AVAILABLE) {
                DctController.logd("EVENT_PHONE" + ((msg.what - 800) + 1) + "_RADIO_AVAILABLE.");
                DctController.this.handleRadioAvailable(msg.what - 800);
                return;
            }
            if (msg.what >= DctController.EVENT_RADIO_NOT_AVAILABLE) {
                DctController.logd("EVENT_PHONE" + ((msg.what - 700) + 1) + "_OFF_OR_NOT_AVAILABLE.");
                DctController.this.handleRadioUnavailable(msg.what - 700);
            } else if (msg.what >= DctController.EVENT_DATA_DETACHED) {
                DctController.logd("EVENT_PHONE" + ((msg.what - 600) + 1) + "_DATA_DETACH.");
                DctController.this.mDcSwitchAsyncChannel[msg.what - 600].notifyDataDetached();
            } else if (msg.what >= DctController.EVENT_DATA_ATTACHED) {
                DctController.logd("EVENT_PHONE" + ((msg.what - 500) + 1) + "_DATA_ATTACH.");
                DctController.this.mDcSwitchAsyncChannel[msg.what - 500].notifyDataAttached();
            }
        }
    };

    public void updatePhoneObject(PhoneProxy phone) {
        if (phone == null) {
            loge("updatePhoneObject phone = null");
            return;
        }
        PhoneBase phoneBase = (PhoneBase) phone.getActivePhone();
        if (phoneBase == null) {
            loge("updatePhoneObject phoneBase = null");
            return;
        }
        for (int i = 0; i < this.mPhoneNum; i++) {
            if (this.mPhones[i] == phone) {
                updatePhoneBaseForIndex(i, phoneBase);
                return;
            }
        }
    }

    private void updatePhoneBaseForIndex(int index, PhoneBase phoneBase) {
        logd("updatePhoneBaseForIndex for phone index=" + index);
        phoneBase.getServiceStateTracker().registerForDataConnectionAttached(this.mRspHandler, index + EVENT_DATA_ATTACHED, null);
        phoneBase.getServiceStateTracker().registerForDataConnectionDetached(this.mRspHandler, index + EVENT_DATA_DETACHED, null);
        phoneBase.mCi.registerForNotAvailable(this.mRspHandler, index + EVENT_RADIO_NOT_AVAILABLE, null);
        phoneBase.mCi.registerForAvailable(this.mRspHandler, index + EVENT_RADIO_AVAILABLE, null);
        ConnectivityManager cm = (ConnectivityManager) this.mPhones[index].getContext().getSystemService("connectivity");
        if (this.mNetworkFactoryMessenger != null) {
            logd("unregister TelephonyNetworkFactory for phone index=" + index);
            cm.unregisterNetworkFactory(this.mNetworkFactoryMessenger[index]);
            this.mNetworkFactoryMessenger[index] = null;
            this.mNetworkFactory[index] = null;
            this.mNetworkFilter[index] = null;
        }
        this.mNetworkFilter[index] = new NetworkCapabilities();
        this.mNetworkFilter[index].addTransportType(0);
        this.mNetworkFilter[index].addCapability(0);
        this.mNetworkFilter[index].addCapability(1);
        this.mNetworkFilter[index].addCapability(2);
        this.mNetworkFilter[index].addCapability(3);
        this.mNetworkFilter[index].addCapability(4);
        this.mNetworkFilter[index].addCapability(5);
        this.mNetworkFilter[index].addCapability(7);
        this.mNetworkFilter[index].addCapability(8);
        this.mNetworkFilter[index].addCapability(9);
        this.mNetworkFilter[index].addCapability(10);
        this.mNetworkFilter[index].addCapability(13);
        this.mNetworkFilter[index].addCapability(12);
        this.mNetworkFactory[index] = new TelephonyNetworkFactory(getLooper(), this.mPhones[index].getContext(), "TelephonyNetworkFactory", phoneBase, this.mNetworkFilter[index]);
        this.mNetworkFactory[index].setScoreFilter(50);
        this.mNetworkFactoryMessenger[index] = new Messenger(this.mNetworkFactory[index]);
        cm.registerNetworkFactory(this.mNetworkFactoryMessenger[index], "Telephony");
    }

    void handleRadioAvailable(int phoneId) {
        if (!this.mCanProcessMsg) {
            for (int i = 0; i < this.mPhoneNum; i++) {
                PhoneBase phoneBase = (PhoneBase) this.mPhones[i].getActivePhone();
                if (phoneBase.mCi.getRadioState() == CommandsInterface.RadioState.RADIO_UNAVAILABLE) {
                    return;
                }
            }
            int simId = Dsds.getRilDataAllowSIM();
            if (simId < this.mPhoneNum) {
                this.mDcSwitchAsyncChannel[simId].trsToAttachingStateSync();
            }
            this.mCanProcessMsg = true;
            processRequests();
        }
    }

    void handleRadioUnavailable(int phoneId) {
        if (this.mCanProcessMsg) {
            this.mCanProcessMsg = false;
            int activePhoneId = getDataActivePhoneId();
            if (SubscriptionManager.isValidPhoneId(activePhoneId)) {
                this.mDcSwitchAsyncChannel[activePhoneId].disconnectAllSync();
            }
        }
    }

    public static DctController getInstance() {
        if (sDctController == null) {
            throw new RuntimeException("DctController.getInstance can't be called before makeDCTController()");
        }
        return sDctController;
    }

    public static DctController makeDctController(PhoneProxy[] phones) {
        if (sDctController == null) {
            logd("makeDctController: new DctController phones.length=" + phones.length);
            sDctController = new DctController(phones);
        }
        logd("makeDctController: X sDctController=" + sDctController);
        return sDctController;
    }

    private DctController(PhoneProxy[] phones) {
        logd("DctController(): phones.length=" + phones.length);
        if (phones == null || phones.length == 0) {
            if (phones == null) {
                loge("DctController(phones): UNEXPECTED phones=null, ignore");
                return;
            } else {
                loge("DctController(phones): UNEXPECTED phones.length=0, ignore");
                return;
            }
        }
        this.mCanProcessMsg = false;
        this.mPhoneNum = phones.length;
        this.mPhones = phones;
        this.mDcSwitchStateMachine = new DcSwitchStateMachine[this.mPhoneNum];
        this.mDcSwitchAsyncChannel = new DcSwitchAsyncChannel[this.mPhoneNum];
        this.mDcSwitchStateHandler = new Handler[this.mPhoneNum];
        this.mNetworkFactoryMessenger = new Messenger[this.mPhoneNum];
        this.mNetworkFactory = new NetworkFactory[this.mPhoneNum];
        this.mNetworkFilter = new NetworkCapabilities[this.mPhoneNum];
        for (int i = 0; i < this.mPhoneNum; i++) {
            int phoneId = i;
            this.mDcSwitchStateMachine[i] = new DcSwitchStateMachine(this.mPhones[i], "DcSwitchStateMachine-" + phoneId, phoneId);
            this.mDcSwitchStateMachine[i].start();
            this.mDcSwitchAsyncChannel[i] = new DcSwitchAsyncChannel(this.mDcSwitchStateMachine[i], phoneId);
            this.mDcSwitchStateHandler[i] = new Handler();
            int status = this.mDcSwitchAsyncChannel[i].fullyConnectSync(this.mPhones[i].getContext(), this.mDcSwitchStateHandler[i], this.mDcSwitchStateMachine[i].getHandler());
            if (status == 0) {
                logd("DctController(phones): Connect success: " + i);
            } else {
                loge("DctController(phones): Could not connect to " + i);
            }
            PhoneBase phoneBase = (PhoneBase) this.mPhones[i].getActivePhone();
            updatePhoneBaseForIndex(i, phoneBase);
        }
        this.mContext = this.mPhones[0].getContext();
        this.mSubMgr = SubscriptionManager.from(this.mContext);
        this.mSubMgr.addOnSubscriptionsChangedListener(this.mOnSubscriptionsChangedListener);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("multi_sim_data_call"), false, this.mObserver);
    }

    public void dispose() {
        logd("DctController.dispose");
        for (int i = 0; i < this.mPhoneNum; i++) {
            ConnectivityManager cm = (ConnectivityManager) this.mPhones[i].getContext().getSystemService("connectivity");
            cm.unregisterNetworkFactory(this.mNetworkFactoryMessenger[i]);
            this.mNetworkFactoryMessenger[i] = null;
        }
        this.mSubMgr.removeOnSubscriptionsChangedListener(this.mOnSubscriptionsChangedListener);
        this.mContext.getContentResolver().unregisterContentObserver(this.mObserver);
    }

    @Override
    public void handleMessage(Message msg) {
        logd("handleMessage msg=" + msg);
        switch (msg.what) {
            case 100:
                onProcessRequest();
                break;
            case EVENT_EXECUTE_REQUEST:
                onExecuteRequest((DcSwitchAsyncChannel.RequestInfo) msg.obj);
                break;
            case EVENT_EXECUTE_ALL_REQUESTS:
                onExecuteAllRequests(msg.arg1);
                break;
            case EVENT_RELEASE_REQUEST:
                onReleaseRequest((DcSwitchAsyncChannel.RequestInfo) msg.obj);
                break;
            case EVENT_RELEASE_ALL_REQUESTS:
                onReleaseAllRequests(msg.arg1);
                break;
            default:
                loge("Un-handled message [" + msg.what + "]");
                break;
        }
    }

    private int requestNetwork(NetworkRequest request, int priority) {
        logd("requestNetwork request=" + request + ", priority=" + priority);
        DcSwitchAsyncChannel.RequestInfo requestInfo = new DcSwitchAsyncChannel.RequestInfo(request, priority);
        this.mRequestInfos.put(Integer.valueOf(request.requestId), requestInfo);
        processRequests();
        return 1;
    }

    private int releaseNetwork(NetworkRequest request) {
        DcSwitchAsyncChannel.RequestInfo requestInfo = this.mRequestInfos.get(Integer.valueOf(request.requestId));
        logd("releaseNetwork request=" + request + ", requestInfo=" + requestInfo);
        this.mRequestInfos.remove(Integer.valueOf(request.requestId));
        releaseRequest(requestInfo);
        processRequests();
        return 1;
    }

    void processRequests() {
        if (this.mCanProcessMsg) {
            logd("processRequests");
            sendMessage(obtainMessage(100));
        }
    }

    void executeRequest(DcSwitchAsyncChannel.RequestInfo request) {
        if (this.mCanProcessMsg) {
            logd("executeRequest, request= " + request);
            sendMessage(obtainMessage(EVENT_EXECUTE_REQUEST, request));
        }
    }

    void executeAllRequests(int phoneId) {
        if (this.mCanProcessMsg) {
            logd("executeAllRequests, phone:" + phoneId);
            sendMessage(obtainMessage(EVENT_EXECUTE_ALL_REQUESTS, phoneId, 0));
        }
    }

    void releaseRequest(DcSwitchAsyncChannel.RequestInfo request) {
        logd("releaseRequest, request= " + request);
        sendMessage(obtainMessage(EVENT_RELEASE_REQUEST, request));
    }

    void releaseAllRequests(int phoneId) {
        logd("releaseAllRequests, phone:" + phoneId);
        sendMessage(obtainMessage(EVENT_RELEASE_ALL_REQUESTS, phoneId, 0));
    }

    public int getDataActivePhoneId() {
        for (int i = 0; i < this.mDcSwitchStateMachine.length; i++) {
            if (!this.mDcSwitchAsyncChannel[i].isIdleSync()) {
                int activePhoneId = i;
                return activePhoneId;
            }
        }
        return -1;
    }

    private void onProcessRequest() {
        int phoneId = getTopPriorityRequestPhoneId();
        int activePhoneId = -1;
        int i = 0;
        while (true) {
            if (i >= this.mDcSwitchStateMachine.length) {
                break;
            }
            if (this.mDcSwitchAsyncChannel[i].isIdleSync()) {
                i++;
            } else {
                activePhoneId = i;
                break;
            }
        }
        logd("onProcessRequest phoneId=" + phoneId + ", activePhoneId=" + activePhoneId);
        Dsds.setInitialDataAllowSIM(phoneId);
        if (activePhoneId == -1 || activePhoneId == phoneId) {
            Iterator<Integer> iterator = this.mRequestInfos.keySet().iterator();
            while (iterator.hasNext()) {
                DcSwitchAsyncChannel.RequestInfo requestInfo = this.mRequestInfos.get(iterator.next());
                if (getRequestPhoneId(requestInfo.request) == phoneId && !requestInfo.executed) {
                    this.mDcSwitchAsyncChannel[phoneId].connectSync(requestInfo);
                }
            }
            return;
        }
        this.mDcSwitchAsyncChannel[activePhoneId].disconnectAllSync();
    }

    private void onExecuteRequest(DcSwitchAsyncChannel.RequestInfo requestInfo) {
        logd("onExecuteRequest request=" + requestInfo);
        if (!requestInfo.executed) {
            requestInfo.executed = true;
            String apn = apnForNetworkRequest(requestInfo.request);
            int phoneId = getRequestPhoneId(requestInfo.request);
            PhoneBase phoneBase = (PhoneBase) this.mPhones[phoneId].getActivePhone();
            DcTrackerBase dcTracker = phoneBase.mDcTracker;
            dcTracker.incApnRefCount(apn);
        }
    }

    private void onExecuteAllRequests(int phoneId) {
        logd("onExecuteAllRequests phoneId=" + phoneId);
        Iterator<Integer> iterator = this.mRequestInfos.keySet().iterator();
        while (iterator.hasNext()) {
            DcSwitchAsyncChannel.RequestInfo requestInfo = this.mRequestInfos.get(iterator.next());
            if (getRequestPhoneId(requestInfo.request) == phoneId) {
                onExecuteRequest(requestInfo);
            }
        }
    }

    private void onReleaseRequest(DcSwitchAsyncChannel.RequestInfo requestInfo) {
        logd("onReleaseRequest request=" + requestInfo);
        if (requestInfo != null && requestInfo.executed) {
            String apn = apnForNetworkRequest(requestInfo.request);
            int phoneId = getRequestPhoneId(requestInfo.request);
            PhoneBase phoneBase = (PhoneBase) this.mPhones[phoneId].getActivePhone();
            DcTrackerBase dcTracker = phoneBase.mDcTracker;
            dcTracker.decApnRefCount(apn);
            requestInfo.executed = false;
        }
    }

    private void onReleaseAllRequests(int phoneId) {
        logd("onReleaseAllRequests phoneId=" + phoneId);
        Iterator<Integer> iterator = this.mRequestInfos.keySet().iterator();
        while (iterator.hasNext()) {
            DcSwitchAsyncChannel.RequestInfo requestInfo = this.mRequestInfos.get(iterator.next());
            if (getRequestPhoneId(requestInfo.request) == phoneId) {
                onReleaseRequest(requestInfo);
            }
        }
    }

    private void onSettingsChange() {
        long dataSubId = this.mSubController.getDefaultDataSubId();
        int activePhoneId = -1;
        int i = 0;
        while (true) {
            if (i >= this.mDcSwitchStateMachine.length) {
                break;
            }
            if (this.mDcSwitchAsyncChannel[i].isIdleSync()) {
                i++;
            } else {
                activePhoneId = i;
                break;
            }
        }
        int[] subIds = SubscriptionManager.getSubId(activePhoneId);
        if (subIds == null || subIds.length == 0) {
            loge("onSettingsChange, subIds null or length 0 for activePhoneId " + activePhoneId);
            return;
        }
        logd("onSettingsChange, data sub: " + dataSubId + ", active data sub: " + subIds[0]);
        if (subIds[0] != dataSubId) {
            Iterator<Integer> iterator = this.mRequestInfos.keySet().iterator();
            while (iterator.hasNext()) {
                DcSwitchAsyncChannel.RequestInfo requestInfo = this.mRequestInfos.get(iterator.next());
                String specifier = requestInfo.request.networkCapabilities.getNetworkSpecifier();
                if (specifier == null || specifier.equals("")) {
                    if (requestInfo.executed) {
                        String apn = apnForNetworkRequest(requestInfo.request);
                        logd("[setDataSubId] activePhoneId:" + activePhoneId + ", subId =" + dataSubId);
                        PhoneBase phoneBase = (PhoneBase) this.mPhones[activePhoneId].getActivePhone();
                        DcTrackerBase dcTracker = phoneBase.mDcTracker;
                        dcTracker.decApnRefCount(apn);
                        requestInfo.executed = false;
                    }
                }
            }
        }
        for (int i2 = 0; i2 < this.mPhoneNum; i2++) {
            ((TelephonyNetworkFactory) this.mNetworkFactory[i2]).evalPendingRequest();
        }
        processRequests();
    }

    private int getTopPriorityRequestPhoneId() {
        DcSwitchAsyncChannel.RequestInfo retRequestInfo = null;
        int phoneId = 0;
        int priority = -1;
        for (int i = 0; i < this.mPhoneNum; i++) {
            Iterator<Integer> iterator = this.mRequestInfos.keySet().iterator();
            while (iterator.hasNext()) {
                DcSwitchAsyncChannel.RequestInfo requestInfo = this.mRequestInfos.get(iterator.next());
                logd("selectExecPhone requestInfo = " + requestInfo);
                if (getRequestPhoneId(requestInfo.request) == i && priority < requestInfo.priority) {
                    priority = requestInfo.priority;
                    retRequestInfo = requestInfo;
                }
            }
        }
        if (retRequestInfo != null) {
            phoneId = getRequestPhoneId(retRequestInfo.request);
        }
        logd("getTopPriorityRequestPhoneId = " + phoneId + ", priority = " + priority);
        return phoneId;
    }

    private void onSubInfoReady() {
        logd("onSubInfoReady mPhoneNum=" + this.mPhoneNum);
        for (int i = 0; i < this.mPhoneNum; i++) {
            int subId = this.mPhones[i].getSubId();
            logd("onSubInfoReady handle pending requests subId=" + subId);
            this.mNetworkFilter[i].setNetworkSpecifier(String.valueOf(subId));
            ((TelephonyNetworkFactory) this.mNetworkFactory[i]).evalPendingRequest();
        }
        processRequests();
    }

    private String apnForNetworkRequest(NetworkRequest nr) {
        NetworkCapabilities nc = nr.networkCapabilities;
        if (nc.getTransportTypes().length > 0 && !nc.hasTransport(0)) {
            return null;
        }
        int type = -1;
        String name = null;
        if (nc.hasCapability(12)) {
            error = 0 != 0;
            name = "default";
            type = 0;
        }
        if (nc.hasCapability(0)) {
            if (name != null) {
                error = true;
            }
            name = "mms";
            type = 2;
        }
        if (nc.hasCapability(1)) {
            if (name != null) {
                error = true;
            }
            name = "supl";
            type = 3;
        }
        if (nc.hasCapability(2)) {
            if (name != null) {
                error = true;
            }
            name = "dun";
            type = 4;
        }
        if (nc.hasCapability(3)) {
            if (name != null) {
                error = true;
            }
            name = "fota";
            type = 10;
        }
        if (nc.hasCapability(4)) {
            if (name != null) {
                error = true;
            }
            name = "ims";
            type = 11;
        }
        if (nc.hasCapability(5)) {
            if (name != null) {
                error = true;
            }
            name = "cbs";
            type = 12;
        }
        if (nc.hasCapability(7)) {
            if (name != null) {
                error = true;
            }
            name = "ia";
            type = 14;
        }
        if (nc.hasCapability(8)) {
            if (name != null) {
                error = true;
            }
            name = null;
            loge("RCS APN type not yet supported");
        }
        if (nc.hasCapability(9)) {
            if (name != null) {
                error = true;
            }
            name = null;
            loge("XCAP APN type not yet supported");
        }
        if (nc.hasCapability(10)) {
            if (name != null) {
                error = true;
            }
            name = null;
            loge("EIMS APN type not yet supported");
        }
        if (error) {
            loge("Multiple apn types specified in request - result is unspecified!");
        }
        if (type == -1 || name == null) {
            loge("Unsupported NetworkRequest in Telephony: nr=" + nr);
            return null;
        }
        return name;
    }

    private int getRequestPhoneId(NetworkRequest networkRequest) {
        int subId;
        String specifier = networkRequest.networkCapabilities.getNetworkSpecifier();
        if (specifier == null || specifier.equals("")) {
            subId = this.mSubController.getDefaultDataSubId();
        } else {
            subId = Integer.parseInt(specifier);
        }
        int phoneId = this.mSubController.getPhoneId(subId);
        if (!SubscriptionManager.isValidPhoneId(phoneId)) {
            phoneId = Dsds.defaultSimId(this.mContext).ordinal();
            if (!SubscriptionManager.isValidPhoneId(phoneId)) {
                throw new RuntimeException("Should not happen, no valid phoneId");
            }
        }
        return phoneId;
    }

    private static void logd(String s) {
        Rlog.d(LOG_TAG, s);
    }

    private static void loge(String s) {
        Rlog.e(LOG_TAG, s);
    }

    private class TelephonyNetworkFactory extends NetworkFactory {
        private final SparseArray<NetworkRequest> mPendingReq;
        private Phone mPhone;

        public TelephonyNetworkFactory(Looper l, Context c, String TAG, Phone phone, NetworkCapabilities nc) {
            super(l, c, TAG, nc);
            this.mPendingReq = new SparseArray<>();
            this.mPhone = phone;
            log("NetworkCapabilities: " + nc);
        }

        protected void needNetworkFor(NetworkRequest networkRequest, int score) {
            log("Cellular needs Network for " + networkRequest);
            if (SubscriptionManager.isUsableSubIdValue(this.mPhone.getSubId())) {
                if (DctController.this.getRequestPhoneId(networkRequest) == this.mPhone.getPhoneId()) {
                    DcTrackerBase dcTracker = ((PhoneBase) this.mPhone).mDcTracker;
                    String apn = DctController.this.apnForNetworkRequest(networkRequest);
                    if (dcTracker.isApnSupported(apn)) {
                        DctController.this.requestNetwork(networkRequest, dcTracker.getApnPriority(apn));
                        return;
                    } else {
                        log("Unsupported APN");
                        return;
                    }
                }
                log("Request not send, put to pending");
                this.mPendingReq.put(networkRequest.requestId, networkRequest);
                return;
            }
            log("Sub Info has not been ready, pending request.");
            this.mPendingReq.put(networkRequest.requestId, networkRequest);
        }

        protected void releaseNetworkFor(NetworkRequest networkRequest) {
            log("Cellular releasing Network for " + networkRequest);
            if (SubscriptionManager.isUsableSubIdValue(this.mPhone.getSubId())) {
                if (DctController.this.getRequestPhoneId(networkRequest) == this.mPhone.getPhoneId()) {
                    DcTrackerBase dcTracker = ((PhoneBase) this.mPhone).mDcTracker;
                    String apn = DctController.this.apnForNetworkRequest(networkRequest);
                    if (dcTracker.isApnSupported(apn)) {
                        DctController.this.releaseNetwork(networkRequest);
                        return;
                    } else {
                        log("Unsupported APN");
                        return;
                    }
                }
                log("Request not release");
                return;
            }
            log("Sub Info has not been ready, remove request.");
            this.mPendingReq.remove(networkRequest.requestId);
        }

        protected void log(String s) {
            Rlog.d(DctController.LOG_TAG, "[TNF " + this.mPhone.getSubId() + "]" + s);
        }

        public void evalPendingRequest() {
            log("evalPendingRequest, pending request size is " + this.mPendingReq.size());
            for (int i = 0; i < this.mPendingReq.size(); i++) {
                int key = this.mPendingReq.keyAt(i);
                NetworkRequest request = this.mPendingReq.get(key);
                log("evalPendingRequest: request = " + request);
                this.mPendingReq.remove(request.requestId);
                needNetworkFor(request, 0);
            }
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("DctController:");
        try {
            DcSwitchStateMachine[] arr$ = this.mDcSwitchStateMachine;
            for (DcSwitchStateMachine dssm : arr$) {
                dssm.dump(fd, pw, args);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        pw.flush();
        pw.println("++++++++++++++++++++++++++++++++");
        try {
            for (Map.Entry<Integer, DcSwitchAsyncChannel.RequestInfo> entry : this.mRequestInfos.entrySet()) {
                pw.println("mRequestInfos[" + entry.getKey() + "]=" + entry.getValue());
            }
        } catch (Exception e2) {
            e2.printStackTrace();
        }
        pw.flush();
        pw.println("++++++++++++++++++++++++++++++++");
        pw.flush();
        pw.println("TelephonyNetworkFactories:");
        NetworkFactory[] arr$2 = this.mNetworkFactory;
        for (NetworkFactory tnf : arr$2) {
            pw.println("  " + tnf);
        }
        pw.flush();
        pw.println("++++++++++++++++++++++++++++++++");
        pw.flush();
    }
}
