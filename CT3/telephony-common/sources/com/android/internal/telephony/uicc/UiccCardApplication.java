package com.android.internal.telephony.uicc;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.SystemProperties;
import android.telephony.Rlog;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.uicc.IccCardApplicationStatus;
import com.android.internal.telephony.uicc.IccCardStatus;
import com.android.internal.telephony.uicc.IccRecords;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class UiccCardApplication {

    private static final int[] f32x1911c1cf = null;

    private static final int[] f33xb5f5d084 = null;
    public static final int AUTH_CONTEXT_EAP_AKA = 129;
    public static final int AUTH_CONTEXT_EAP_SIM = 128;
    public static final int AUTH_CONTEXT_UNDEFINED = -1;
    public static final int CAT_CORPORATE = 3;
    public static final int CAT_NETOWRK_SUBSET = 1;
    public static final int CAT_NETWOEK = 0;
    public static final int CAT_SERVICE_PROVIDER = 2;
    public static final int CAT_SIM = 4;
    private static final boolean DBG = true;
    private static final int EVENT_CHANGE_FACILITY_FDN_DONE = 5;
    private static final int EVENT_CHANGE_FACILITY_LOCK_DONE = 7;
    private static final int EVENT_CHANGE_NETWORK_LOCK_DONE = 102;
    private static final int EVENT_CHANGE_PIN1_DONE = 2;
    private static final int EVENT_CHANGE_PIN2_DONE = 3;
    private static final int EVENT_PIN1_PUK1_DONE = 1;
    private static final int EVENT_PIN2_PUK2_DONE = 8;
    private static final int EVENT_PUK1_CHANGE_PIN1_DONE = 104;
    private static final int EVENT_PUK2_CHANGE_PIN2_DONE = 105;
    private static final int EVENT_QUERY_FACILITY_FDN_DONE = 4;
    private static final int EVENT_QUERY_FACILITY_LOCK_DONE = 6;
    private static final int EVENT_QUERY_NETWORK_LOCK_DONE = 101;
    private static final int EVENT_RADIO_NOTAVAILABLE = 103;
    private static final int EVENT_RADIO_UNAVAILABLE = 9;
    private static final String LOG_TAG = "UiccCardApplication";
    public static final int OP_ADD = 2;
    public static final int OP_LOCK = 1;
    public static final int OP_PERMANENT_UNLOCK = 4;
    public static final int OP_REMOVE = 3;
    public static final int OP_UNLOCK = 0;
    private String mAid;
    private String mAppLabel;
    private IccCardApplicationStatus.AppState mAppState;
    private IccCardApplicationStatus.AppType mAppType;
    private int mAuthContext;
    private CommandsInterface mCi;
    private Context mContext;
    private boolean mDesiredFdnEnabled;
    private boolean mDesiredPinLocked;
    private boolean mDestroyed;
    private boolean mIccFdnEnabled;
    private IccFileHandler mIccFh;
    private boolean mIccLockEnabled;
    private IccRecords mIccRecords;
    private IccCardApplicationStatus.PersoSubState mPersoSubState;
    private int mPhoneId;
    private boolean mPin1Replaced;
    private IccCardStatus.PinState mPin1State;
    private IccCardStatus.PinState mPin2State;
    private UiccCard mUiccCard;
    static final String[] UICCCARDAPPLICATION_PROPERTY_RIL_UICC_TYPE = {"gsm.ril.uicctype", "gsm.ril.uicctype.2", "gsm.ril.uicctype.3", "gsm.ril.uicctype.4"};
    private static final String[] PROPERTY_PIN1_RETRY = {"gsm.sim.retry.pin1", "gsm.sim.retry.pin1.2", "gsm.sim.retry.pin1.3", "gsm.sim.retry.pin1.4"};
    private static final String[] PROPERTY_PIN2_RETRY = {"gsm.sim.retry.pin2", "gsm.sim.retry.pin2.2", "gsm.sim.retry.pin2.3", "gsm.sim.retry.pin2.4"};
    private final Object mLock = new Object();
    private boolean mIccFdnAvailable = true;
    private RegistrantList mReadyRegistrants = new RegistrantList();
    private RegistrantList mPinLockedRegistrants = new RegistrantList();
    private RegistrantList mNetworkLockedRegistrants = new RegistrantList();
    protected String mIccType = null;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (UiccCardApplication.this.mDestroyed) {
                if (1 == msg.what) {
                    Message response = (Message) ((AsyncResult) msg.obj).userObj;
                    AsyncResult.forMessage(response).exception = CommandException.fromRilErrno(1);
                    UiccCardApplication.this.loge("Received message " + msg + "[" + msg.what + "] while being destroyed. return exception.");
                    response.arg1 = -1;
                    response.sendToTarget();
                }
                UiccCardApplication.this.loge("Received message " + msg + "[" + msg.what + "] while being destroyed. Ignoring.");
                return;
            }
            switch (msg.what) {
                case 1:
                case 2:
                case 3:
                case 8:
                    int attemptsRemaining = -1;
                    AsyncResult ar = (AsyncResult) msg.obj;
                    if (ar.exception != null && ar.result != null) {
                        attemptsRemaining = UiccCardApplication.this.parsePinPukErrorResult(ar);
                    }
                    Message response2 = (Message) ar.userObj;
                    AsyncResult.forMessage(response2).exception = ar.exception;
                    response2.arg1 = attemptsRemaining;
                    response2.sendToTarget();
                    break;
                case 4:
                    UiccCardApplication.this.onQueryFdnEnabled((AsyncResult) msg.obj);
                    break;
                case 5:
                    UiccCardApplication.this.onChangeFdnDone((AsyncResult) msg.obj);
                    break;
                case 6:
                    UiccCardApplication.this.onQueryFacilityLock((AsyncResult) msg.obj);
                    break;
                case 7:
                    UiccCardApplication.this.onChangeFacilityLock((AsyncResult) msg.obj);
                    break;
                case 9:
                    UiccCardApplication.this.log("handleMessage (EVENT_RADIO_UNAVAILABLE)");
                    UiccCardApplication.this.mAppState = IccCardApplicationStatus.AppState.APPSTATE_UNKNOWN;
                    break;
                case 101:
                    UiccCardApplication.this.log("handleMessage (EVENT_QUERY_NETWORK_LOCK)");
                    AsyncResult ar2 = (AsyncResult) msg.obj;
                    if (ar2.exception != null) {
                        Rlog.e(UiccCardApplication.LOG_TAG, "Error query network lock with exception " + ar2.exception);
                    }
                    AsyncResult.forMessage((Message) ar2.userObj, ar2.result, ar2.exception);
                    ((Message) ar2.userObj).sendToTarget();
                    break;
                case 102:
                    UiccCardApplication.this.log("handleMessage (EVENT_CHANGE_NETWORK_LOCK)");
                    AsyncResult ar3 = (AsyncResult) msg.obj;
                    if (ar3.exception != null) {
                        Rlog.e(UiccCardApplication.LOG_TAG, "Error change network lock with exception " + ar3.exception);
                    }
                    AsyncResult.forMessage((Message) ar3.userObj).exception = ar3.exception;
                    ((Message) ar3.userObj).sendToTarget();
                    break;
                case 104:
                    UiccCardApplication.this.log("EVENT_PUK1_CHANGE_PIN1_DONE");
                    int attemptsRemainingPuk = -1;
                    AsyncResult ar4 = (AsyncResult) msg.obj;
                    if (ar4.exception != null && ar4.result != null) {
                        attemptsRemainingPuk = UiccCardApplication.this.parsePinPukErrorResult(ar4);
                    }
                    Message responsePuk = (Message) ar4.userObj;
                    AsyncResult.forMessage(responsePuk).exception = ar4.exception;
                    responsePuk.arg1 = attemptsRemainingPuk;
                    responsePuk.sendToTarget();
                    UiccCardApplication.this.queryPin1State();
                    break;
                case 105:
                    int attemptsRemainingPuk2 = -1;
                    AsyncResult ar5 = (AsyncResult) msg.obj;
                    if (ar5.exception != null && ar5.result != null) {
                        attemptsRemainingPuk2 = UiccCardApplication.this.parsePinPukErrorResult(ar5);
                    }
                    Message responsePuk2 = (Message) ar5.userObj;
                    AsyncResult.forMessage(responsePuk2).exception = ar5.exception;
                    responsePuk2.arg1 = attemptsRemainingPuk2;
                    responsePuk2.sendToTarget();
                    UiccCardApplication.this.queryFdn();
                    break;
                default:
                    UiccCardApplication.this.loge("Unknown Event " + msg.what);
                    break;
            }
        }
    };
    private RegistrantList mFdnChangedRegistrants = new RegistrantList();

    private static int[] m525x31a426ab() {
        if (f32x1911c1cf != null) {
            return f32x1911c1cf;
        }
        int[] iArr = new int[IccCardApplicationStatus.AppType.valuesCustom().length];
        try {
            iArr[IccCardApplicationStatus.AppType.APPTYPE_CSIM.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[IccCardApplicationStatus.AppType.APPTYPE_ISIM.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[IccCardApplicationStatus.AppType.APPTYPE_RUIM.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[IccCardApplicationStatus.AppType.APPTYPE_SIM.ordinal()] = 4;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[IccCardApplicationStatus.AppType.APPTYPE_UNKNOWN.ordinal()] = 12;
        } catch (NoSuchFieldError e5) {
        }
        try {
            iArr[IccCardApplicationStatus.AppType.APPTYPE_USIM.ordinal()] = 5;
        } catch (NoSuchFieldError e6) {
        }
        f32x1911c1cf = iArr;
        return iArr;
    }

    private static int[] m526xc1fb5860() {
        if (f33xb5f5d084 != null) {
            return f33xb5f5d084;
        }
        int[] iArr = new int[IccCardStatus.PinState.valuesCustom().length];
        try {
            iArr[IccCardStatus.PinState.PINSTATE_DISABLED.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[IccCardStatus.PinState.PINSTATE_ENABLED_BLOCKED.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[IccCardStatus.PinState.PINSTATE_ENABLED_NOT_VERIFIED.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[IccCardStatus.PinState.PINSTATE_ENABLED_PERM_BLOCKED.ordinal()] = 4;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[IccCardStatus.PinState.PINSTATE_ENABLED_VERIFIED.ordinal()] = 5;
        } catch (NoSuchFieldError e5) {
        }
        try {
            iArr[IccCardStatus.PinState.PINSTATE_UNKNOWN.ordinal()] = 6;
        } catch (NoSuchFieldError e6) {
        }
        f33xb5f5d084 = iArr;
        return iArr;
    }

    public UiccCardApplication(UiccCard uiccCard, IccCardApplicationStatus as, Context c, CommandsInterface ci) {
        log("Creating UiccApp: " + as);
        this.mUiccCard = uiccCard;
        this.mAppState = as.app_state;
        this.mAppType = as.app_type;
        this.mAuthContext = getAuthContext(this.mAppType);
        this.mPersoSubState = as.perso_substate;
        this.mAid = as.aid;
        this.mAppLabel = as.app_label;
        this.mPin1Replaced = as.pin1_replaced != 0;
        this.mPin1State = as.pin1;
        this.mPin2State = as.pin2;
        this.mContext = c;
        this.mCi = ci;
        this.mPhoneId = this.mUiccCard.getPhoneId();
        this.mIccFh = createIccFileHandler(as.app_type);
        this.mIccRecords = createIccRecords(as.app_type, this.mContext, this.mCi);
        if (this.mAppState == IccCardApplicationStatus.AppState.APPSTATE_READY && this.mAppType != IccCardApplicationStatus.AppType.APPTYPE_ISIM) {
            queryFdn();
            queryPin1State();
        }
        this.mCi.registerForNotAvailable(this.mHandler, 9, null);
    }

    public void update(IccCardApplicationStatus as, Context c, CommandsInterface ci) {
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                loge("Application updated after destroyed! Fix me!");
                return;
            }
            log(this.mAppType + " update. New " + as);
            this.mContext = c;
            this.mCi = ci;
            IccCardApplicationStatus.AppType oldAppType = this.mAppType;
            IccCardApplicationStatus.AppState oldAppState = this.mAppState;
            IccCardApplicationStatus.PersoSubState oldPersoSubState = this.mPersoSubState;
            this.mAppType = as.app_type;
            this.mAuthContext = getAuthContext(this.mAppType);
            this.mAppState = as.app_state;
            this.mPersoSubState = as.perso_substate;
            this.mAid = as.aid;
            this.mAppLabel = as.app_label;
            this.mPin1Replaced = as.pin1_replaced != 0;
            this.mPin1State = as.pin1;
            this.mPin2State = as.pin2;
            if (this.mAppType != oldAppType) {
                if (this.mIccFh != null) {
                    this.mIccFh.dispose();
                }
                if (this.mIccRecords != null) {
                    this.mIccRecords.dispose();
                }
                this.mIccFh = createIccFileHandler(as.app_type);
                this.mIccRecords = createIccRecords(as.app_type, c, ci);
            }
            log("mPersoSubState: " + this.mPersoSubState + " oldPersoSubState: " + oldPersoSubState);
            if (this.mPersoSubState != oldPersoSubState) {
                notifyNetworkLockedRegistrantsIfNeeded(null);
            }
            log("update,  mAppState=" + this.mAppState + "  oldAppState=" + oldAppState);
            if (this.mAppState != oldAppState) {
                log(oldAppType + " changed state: " + oldAppState + " -> " + this.mAppState);
                if (this.mAppState == IccCardApplicationStatus.AppState.APPSTATE_READY && this.mAppType != IccCardApplicationStatus.AppType.APPTYPE_ISIM) {
                    queryFdn();
                    queryPin1State();
                }
                notifyPinLockedRegistrantsIfNeeded(null);
                notifyReadyRegistrantsIfNeeded(null);
            }
        }
    }

    void dispose() {
        synchronized (this.mLock) {
            log(this.mAppType + " being Disposed");
            this.mDestroyed = true;
            if (this.mIccRecords != null) {
                this.mIccRecords.dispose();
            }
            if (this.mIccFh != null) {
                this.mIccFh.dispose();
            }
            this.mIccRecords = null;
            this.mIccFh = null;
            this.mCi.unregisterForNotAvailable(this.mHandler);
        }
    }

    private IccRecords createIccRecords(IccCardApplicationStatus.AppType type, Context c, CommandsInterface ci) {
        log("createIccRecords, AppType = " + type);
        if (type == IccCardApplicationStatus.AppType.APPTYPE_USIM || type == IccCardApplicationStatus.AppType.APPTYPE_SIM) {
            return new SIMRecords(this, c, ci);
        }
        if (type == IccCardApplicationStatus.AppType.APPTYPE_RUIM || type == IccCardApplicationStatus.AppType.APPTYPE_CSIM) {
            return new RuimRecords(this, c, ci);
        }
        if (type == IccCardApplicationStatus.AppType.APPTYPE_ISIM) {
            return new IsimUiccRecords(this, c, ci);
        }
        return null;
    }

    private IccFileHandler createIccFileHandler(IccCardApplicationStatus.AppType type) {
        switch (m525x31a426ab()[type.ordinal()]) {
            case 1:
                return new CsimFileHandler(this, this.mAid, this.mCi);
            case 2:
                return new IsimFileHandler(this, this.mAid, this.mCi);
            case 3:
                return new RuimFileHandler(this, this.mAid, this.mCi);
            case 4:
                return new SIMFileHandler(this, this.mAid, this.mCi);
            case 5:
                return new UsimFileHandler(this, this.mAid, this.mCi);
            default:
                return null;
        }
    }

    public void queryFdn() {
        this.mCi.queryFacilityLockForApp(CommandsInterface.CB_FACILITY_BA_FD, UsimPBMemInfo.STRING_NOT_SET, 7, this.mAid, this.mHandler.obtainMessage(4));
    }

    private void onQueryFdnEnabled(AsyncResult ar) {
        synchronized (this.mLock) {
            if (ar.exception != null) {
                log("Error in querying facility lock:" + ar.exception);
                return;
            }
            int[] result = (int[]) ar.result;
            if (result.length != 0) {
                if (result[0] == 2) {
                    this.mIccFdnEnabled = false;
                    this.mIccFdnAvailable = false;
                } else {
                    this.mIccFdnEnabled = result[0] == 1;
                    this.mIccFdnAvailable = true;
                }
                log("Query facility FDN : FDN service available: " + this.mIccFdnAvailable + " enabled: " + this.mIccFdnEnabled);
            } else {
                loge("Bogus facility lock response");
            }
        }
    }

    private void onChangeFdnDone(AsyncResult ar) {
        synchronized (this.mLock) {
            int attemptsRemaining = -1;
            boolean bNotifyFdnChanged = false;
            if (ar.exception == null) {
                this.mIccFdnEnabled = this.mDesiredFdnEnabled;
                log("EVENT_CHANGE_FACILITY_FDN_DONE: mIccFdnEnabled=" + this.mIccFdnEnabled);
                bNotifyFdnChanged = true;
            } else {
                attemptsRemaining = parsePinPukErrorResult(ar);
                loge("Error change facility fdn with exception " + ar.exception);
            }
            Message response = (Message) ar.userObj;
            response.arg1 = attemptsRemaining;
            AsyncResult.forMessage(response).exception = ar.exception;
            response.sendToTarget();
            if (bNotifyFdnChanged) {
                log("notifyFdnChangedRegistrants");
                notifyFdnChangedRegistrants();
            }
        }
    }

    private void queryPin1State() {
        this.mCi.queryFacilityLockForApp(CommandsInterface.CB_FACILITY_BA_SIM, UsimPBMemInfo.STRING_NOT_SET, 7, this.mAid, this.mHandler.obtainMessage(6));
    }

    private void onQueryFacilityLock(AsyncResult ar) {
        synchronized (this.mLock) {
            if (ar.exception != null) {
                loge("Error in querying facility lock:" + ar.exception);
                return;
            }
            int[] ints = (int[]) ar.result;
            if (ints.length != 0) {
                log("Query facility lock : " + ints[0]);
                this.mIccLockEnabled = ints[0] != 0;
                if (this.mIccLockEnabled) {
                    this.mPinLockedRegistrants.notifyRegistrants();
                }
                switch (m526xc1fb5860()[this.mPin1State.ordinal()]) {
                    case 1:
                        if (this.mIccLockEnabled) {
                            loge("QUERY_FACILITY_LOCK:enabled GET_SIM_STATUS.Pin1:disabled. Fixme");
                        }
                        break;
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                        if (!this.mIccLockEnabled) {
                            loge("QUERY_FACILITY_LOCK:disabled GET_SIM_STATUS.Pin1:enabled. Fixme");
                            break;
                        }
                    default:
                        log("Ignoring: pin1state=" + this.mPin1State);
                        break;
                }
            } else {
                loge("Bogus facility lock response");
            }
        }
    }

    private void onChangeFacilityLock(AsyncResult ar) {
        synchronized (this.mLock) {
            int attemptsRemaining = -1;
            if (ar.exception == null) {
                this.mIccLockEnabled = this.mDesiredPinLocked;
                log("EVENT_CHANGE_FACILITY_LOCK_DONE: mIccLockEnabled= " + this.mIccLockEnabled);
            } else {
                attemptsRemaining = parsePinPukErrorResult(ar);
                loge("Error change facility lock with exception " + ar.exception);
            }
            Message response = (Message) ar.userObj;
            AsyncResult.forMessage(response).exception = ar.exception;
            response.arg1 = attemptsRemaining;
            response.sendToTarget();
        }
    }

    private int parsePinPukErrorResult(AsyncResult ar) {
        int[] result = (int[]) ar.result;
        if (result == null) {
            return -1;
        }
        int length = result.length;
        int attemptsRemaining = -1;
        if (length > 0) {
            attemptsRemaining = result[0];
        }
        log("parsePinPukErrorResult: attemptsRemaining=" + attemptsRemaining);
        return attemptsRemaining;
    }

    public void registerForReady(Handler h, int what, Object obj) {
        synchronized (this.mLock) {
            Registrant r = new Registrant(h, what, obj);
            this.mReadyRegistrants.add(r);
            notifyReadyRegistrantsIfNeeded(r);
        }
    }

    public void unregisterForReady(Handler h) {
        synchronized (this.mLock) {
            this.mReadyRegistrants.remove(h);
        }
    }

    public void registerForLocked(Handler h, int what, Object obj) {
        synchronized (this.mLock) {
            Registrant r = new Registrant(h, what, obj);
            this.mPinLockedRegistrants.add(r);
            notifyPinLockedRegistrantsIfNeeded(r);
        }
    }

    public void unregisterForLocked(Handler h) {
        synchronized (this.mLock) {
            this.mPinLockedRegistrants.remove(h);
        }
    }

    public void registerForNetworkLocked(Handler h, int what, Object obj) {
        synchronized (this.mLock) {
            Registrant r = new Registrant(h, what, obj);
            this.mNetworkLockedRegistrants.add(r);
            notifyNetworkLockedRegistrantsIfNeeded(r);
        }
    }

    public void unregisterForNetworkLocked(Handler h) {
        synchronized (this.mLock) {
            this.mNetworkLockedRegistrants.remove(h);
        }
    }

    private void notifyReadyRegistrantsIfNeeded(Registrant r) {
        if (this.mDestroyed || this.mAppState != IccCardApplicationStatus.AppState.APPSTATE_READY) {
            return;
        }
        if (this.mPin1State == IccCardStatus.PinState.PINSTATE_ENABLED_NOT_VERIFIED || this.mPin1State == IccCardStatus.PinState.PINSTATE_ENABLED_BLOCKED || this.mPin1State == IccCardStatus.PinState.PINSTATE_ENABLED_PERM_BLOCKED) {
            loge("Sanity check failed! APPSTATE is ready while PIN1 is not verified!!!");
        } else if (r == null) {
            log("Notifying registrants: READY");
            this.mReadyRegistrants.notifyRegistrants();
        } else {
            log("Notifying 1 registrant: READY");
            r.notifyRegistrant(new AsyncResult((Object) null, (Object) null, (Throwable) null));
        }
    }

    private void notifyPinLockedRegistrantsIfNeeded(Registrant r) {
        if (this.mDestroyed) {
            return;
        }
        if (this.mAppState != IccCardApplicationStatus.AppState.APPSTATE_PIN && this.mAppState != IccCardApplicationStatus.AppState.APPSTATE_PUK) {
            return;
        }
        if (this.mPin1State == IccCardStatus.PinState.PINSTATE_ENABLED_VERIFIED || this.mPin1State == IccCardStatus.PinState.PINSTATE_DISABLED) {
            loge("Sanity check failed! APPSTATE is locked while PIN1 is not!!!");
        } else if (r == null) {
            log("Notifying registrants: LOCKED");
            this.mPinLockedRegistrants.notifyRegistrants();
        } else {
            log("Notifying 1 registrant: LOCKED");
            r.notifyRegistrant(new AsyncResult((Object) null, (Object) null, (Throwable) null));
        }
    }

    private void notifyNetworkLockedRegistrantsIfNeeded(Registrant r) {
        if (this.mDestroyed || this.mAppState != IccCardApplicationStatus.AppState.APPSTATE_SUBSCRIPTION_PERSO) {
            return;
        }
        if (r == null) {
            log("Notifying registrants: NETWORK_LOCKED");
            this.mNetworkLockedRegistrants.notifyRegistrants();
        } else {
            log("Notifying 1 registrant: NETWORK_LOCED");
            r.notifyRegistrant(new AsyncResult((Object) null, (Object) null, (Throwable) null));
        }
    }

    public IccCardApplicationStatus.AppState getState() {
        IccCardApplicationStatus.AppState appState;
        synchronized (this.mLock) {
            appState = this.mAppState;
        }
        return appState;
    }

    public IccCardApplicationStatus.AppType getType() {
        IccCardApplicationStatus.AppType appType;
        synchronized (this.mLock) {
            appType = this.mAppType;
        }
        return appType;
    }

    public int getAuthContext() {
        int i;
        synchronized (this.mLock) {
            i = this.mAuthContext;
        }
        return i;
    }

    private static int getAuthContext(IccCardApplicationStatus.AppType appType) {
        switch (m525x31a426ab()[appType.ordinal()]) {
            case 2:
            case 5:
                return 129;
            case 3:
            default:
                return -1;
            case 4:
                return 128;
        }
    }

    public IccCardApplicationStatus.PersoSubState getPersoSubState() {
        IccCardApplicationStatus.PersoSubState persoSubState;
        synchronized (this.mLock) {
            persoSubState = this.mPersoSubState;
        }
        return persoSubState;
    }

    public String getAid() {
        String str;
        synchronized (this.mLock) {
            str = this.mAid;
        }
        return str;
    }

    public String getAppLabel() {
        return this.mAppLabel;
    }

    public IccCardStatus.PinState getPin1State() {
        synchronized (this.mLock) {
            if (this.mPin1Replaced) {
                return this.mUiccCard.getUniversalPinState();
            }
            return this.mPin1State;
        }
    }

    public IccFileHandler getIccFileHandler() {
        IccFileHandler iccFileHandler;
        synchronized (this.mLock) {
            iccFileHandler = this.mIccFh;
        }
        return iccFileHandler;
    }

    public IccRecords getIccRecords() {
        IccRecords iccRecords;
        synchronized (this.mLock) {
            iccRecords = this.mIccRecords;
        }
        return iccRecords;
    }

    public void supplyPin(String pin, Message onComplete) {
        synchronized (this.mLock) {
            this.mCi.supplyIccPinForApp(pin, this.mAid, this.mHandler.obtainMessage(1, onComplete));
        }
    }

    public void supplyPuk(String puk, String newPin, Message onComplete) {
        synchronized (this.mLock) {
            log("supplyPuk");
            this.mCi.supplyIccPukForApp(puk, newPin, this.mAid, this.mHandler.obtainMessage(104, onComplete));
        }
    }

    public void supplyPin2(String pin2, Message onComplete) {
        synchronized (this.mLock) {
            this.mCi.supplyIccPin2ForApp(pin2, this.mAid, this.mHandler.obtainMessage(8, onComplete));
        }
    }

    public void supplyPuk2(String puk2, String newPin2, Message onComplete) {
        synchronized (this.mLock) {
            this.mCi.supplyIccPuk2ForApp(puk2, newPin2, this.mAid, this.mHandler.obtainMessage(105, onComplete));
        }
    }

    public void supplyNetworkDepersonalization(String pin, Message onComplete) {
        synchronized (this.mLock) {
            log("supplyNetworkDepersonalization");
            this.mCi.supplyNetworkDepersonalization(pin, onComplete);
        }
    }

    public boolean getIccLockEnabled() {
        return this.mIccLockEnabled;
    }

    public boolean getIccFdnEnabled() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mIccFdnEnabled;
        }
        return z;
    }

    public boolean getIccFdnAvailable() {
        if (this.mIccRecords == null) {
            log("isFdnExist mIccRecords == null");
            return false;
        }
        IccRecords.IccServiceStatus iccSerStatus = this.mIccRecords.getSIMServiceStatus(IccRecords.IccService.FDN);
        log("getIccFdnAvailable status: iccSerStatus");
        return iccSerStatus == IccRecords.IccServiceStatus.ACTIVATED;
    }

    public void setIccLockEnabled(boolean enabled, String password, Message onComplete) {
        synchronized (this.mLock) {
            this.mDesiredPinLocked = enabled;
            this.mCi.setFacilityLockForApp(CommandsInterface.CB_FACILITY_BA_SIM, enabled, password, 7, this.mAid, this.mHandler.obtainMessage(7, onComplete));
        }
    }

    public void setIccFdnEnabled(boolean enabled, String password, Message onComplete) {
        synchronized (this.mLock) {
            this.mDesiredFdnEnabled = enabled;
            this.mCi.setFacilityLockForApp(CommandsInterface.CB_FACILITY_BA_FD, enabled, password, 15, this.mAid, this.mHandler.obtainMessage(5, onComplete));
        }
    }

    public void changeIccLockPassword(String oldPassword, String newPassword, Message onComplete) {
        synchronized (this.mLock) {
            log("changeIccLockPassword");
            this.mCi.changeIccPinForApp(oldPassword, newPassword, this.mAid, this.mHandler.obtainMessage(2, onComplete));
        }
    }

    public void changeIccFdnPassword(String oldPassword, String newPassword, Message onComplete) {
        synchronized (this.mLock) {
            log("changeIccFdnPassword");
            this.mCi.changeIccPin2ForApp(oldPassword, newPassword, this.mAid, this.mHandler.obtainMessage(3, onComplete));
        }
    }

    public boolean getIccPin2Blocked() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mPin2State == IccCardStatus.PinState.PINSTATE_ENABLED_BLOCKED;
        }
        return z;
    }

    public boolean getIccPuk2Blocked() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mPin2State == IccCardStatus.PinState.PINSTATE_ENABLED_PERM_BLOCKED;
        }
        return z;
    }

    public int getPhoneId() {
        return this.mUiccCard.getPhoneId();
    }

    protected UiccCard getUiccCard() {
        return this.mUiccCard;
    }

    private void log(String msg) {
        Rlog.d(LOG_TAG, msg + " (slot " + this.mPhoneId + ")");
    }

    private void loge(String msg) {
        Rlog.e(LOG_TAG, msg + " (slot " + this.mPhoneId + ")");
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("UiccCardApplication: " + this);
        pw.println(" mUiccCard=" + this.mUiccCard);
        pw.println(" mAppState=" + this.mAppState);
        pw.println(" mAppType=" + this.mAppType);
        pw.println(" mPersoSubState=" + this.mPersoSubState);
        pw.println(" mAid=" + this.mAid);
        pw.println(" mAppLabel=" + this.mAppLabel);
        pw.println(" mPin1Replaced=" + this.mPin1Replaced);
        pw.println(" mPin1State=" + this.mPin1State);
        pw.println(" mPin2State=" + this.mPin2State);
        pw.println(" mIccFdnEnabled=" + this.mIccFdnEnabled);
        pw.println(" mDesiredFdnEnabled=" + this.mDesiredFdnEnabled);
        pw.println(" mIccLockEnabled=" + this.mIccLockEnabled);
        pw.println(" mDesiredPinLocked=" + this.mDesiredPinLocked);
        pw.println(" mCi=" + this.mCi);
        pw.println(" mIccRecords=" + this.mIccRecords);
        pw.println(" mIccFh=" + this.mIccFh);
        pw.println(" mDestroyed=" + this.mDestroyed);
        pw.println(" mReadyRegistrants: size=" + this.mReadyRegistrants.size());
        for (int i = 0; i < this.mReadyRegistrants.size(); i++) {
            pw.println("  mReadyRegistrants[" + i + "]=" + ((Registrant) this.mReadyRegistrants.get(i)).getHandler());
        }
        pw.println(" mPinLockedRegistrants: size=" + this.mPinLockedRegistrants.size());
        for (int i2 = 0; i2 < this.mPinLockedRegistrants.size(); i2++) {
            pw.println("  mPinLockedRegistrants[" + i2 + "]=" + ((Registrant) this.mPinLockedRegistrants.get(i2)).getHandler());
        }
        pw.println(" mNetworkLockedRegistrants: size=" + this.mNetworkLockedRegistrants.size());
        for (int i3 = 0; i3 < this.mNetworkLockedRegistrants.size(); i3++) {
            pw.println("  mNetworkLockedRegistrants[" + i3 + "]=" + ((Registrant) this.mNetworkLockedRegistrants.get(i3)).getHandler());
        }
        pw.flush();
    }

    public void registerForFdnChanged(Handler h, int what, Object obj) {
        synchronized (this.mLock) {
            Registrant r = new Registrant(h, what, obj);
            this.mFdnChangedRegistrants.add(r);
        }
    }

    public void unregisterForFdnChanged(Handler h) {
        synchronized (this.mLock) {
            this.mFdnChangedRegistrants.remove(h);
        }
    }

    public int getSlotId() {
        return this.mPhoneId;
    }

    private void notifyFdnChangedRegistrants() {
        if (this.mDestroyed) {
            return;
        }
        this.mFdnChangedRegistrants.notifyRegistrants();
    }

    public String getIccCardType() {
        if (this.mIccType == null || this.mIccType.equals(UsimPBMemInfo.STRING_NOT_SET)) {
            this.mIccType = SystemProperties.get(UICCCARDAPPLICATION_PROPERTY_RIL_UICC_TYPE[this.mPhoneId]);
        }
        log("getIccCardType(): mIccType = " + this.mIccType);
        return this.mIccType;
    }

    public void queryIccNetworkLock(int category, Message onComplete) {
        log("queryIccNetworkLock(): category =  " + category);
        switch (category) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
                this.mCi.queryNetworkLock(category, this.mHandler.obtainMessage(101, onComplete));
                break;
            default:
                Rlog.e(LOG_TAG, "queryIccNetworkLock unknown category = " + category);
                break;
        }
    }

    public void setIccNetworkLockEnabled(int category, int lockop, String password, String data_imsi, String gid1, String gid2, Message onComplete) {
        log("SetIccNetworkEnabled(): category = " + category + " lockop = " + lockop + " password = " + password + " data_imsi = " + data_imsi + " gid1 = " + gid1 + " gid2 = " + gid2);
        switch (lockop) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
                this.mCi.setNetworkLock(category, lockop, password, data_imsi, gid1, gid2, this.mHandler.obtainMessage(102, onComplete));
                break;
            default:
                Rlog.e(LOG_TAG, "SetIccNetworkEnabled unknown operation" + lockop);
                break;
        }
    }
}
