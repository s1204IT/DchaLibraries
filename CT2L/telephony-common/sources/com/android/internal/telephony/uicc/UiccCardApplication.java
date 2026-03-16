package com.android.internal.telephony.uicc;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.Registrant;
import android.os.RegistrantList;
import android.telephony.Rlog;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.uicc.IccCardApplicationStatus;
import com.android.internal.telephony.uicc.IccCardStatus;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class UiccCardApplication {
    public static final int AUTH_CONTEXT_EAP_AKA = 129;
    public static final int AUTH_CONTEXT_EAP_SIM = 128;
    public static final int AUTH_CONTEXT_UNDEFINED = -1;
    private static final boolean DBG = true;
    private static final int EVENT_CHANGE_FACILITY_FDN_DONE = 5;
    private static final int EVENT_CHANGE_FACILITY_LOCK_DONE = 7;
    private static final int EVENT_CHANGE_PIN1_DONE = 2;
    private static final int EVENT_CHANGE_PIN2_DONE = 3;
    private static final int EVENT_PIN1_PUK1_DONE = 1;
    private static final int EVENT_PIN2_PUK2_DONE = 8;
    private static final int EVENT_QUERY_FACILITY_FDN_DONE = 4;
    private static final int EVENT_QUERY_FACILITY_LOCK_DONE = 6;
    private static final int EVENT_RADIO_UNAVAILABLE = 9;
    private static final String LOG_TAG = "UiccCardApplication";
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
    private boolean mPin1Replaced;
    private IccCardStatus.PinState mPin1State;
    private IccCardStatus.PinState mPin2State;
    private UiccCard mUiccCard;
    private final Object mLock = new Object();
    private boolean mIccFdnAvailable = true;
    private RegistrantList mReadyRegistrants = new RegistrantList();
    private RegistrantList mPinLockedRegistrants = new RegistrantList();
    private RegistrantList mNetworkLockedRegistrants = new RegistrantList();
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (UiccCardApplication.this.mDestroyed) {
                UiccCardApplication.this.loge("Received message " + msg + "[" + msg.what + "] while being destroyed. Ignoring.");
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
                    Message response = (Message) ar.userObj;
                    AsyncResult.forMessage(response).exception = ar.exception;
                    response.arg1 = attemptsRemaining;
                    response.sendToTarget();
                    break;
                case 4:
                    AsyncResult ar2 = (AsyncResult) msg.obj;
                    UiccCardApplication.this.onQueryFdnEnabled(ar2);
                    break;
                case 5:
                    AsyncResult ar3 = (AsyncResult) msg.obj;
                    UiccCardApplication.this.onChangeFdnDone(ar3);
                    break;
                case 6:
                    AsyncResult ar4 = (AsyncResult) msg.obj;
                    UiccCardApplication.this.onQueryFacilityLock(ar4);
                    break;
                case 7:
                    AsyncResult ar5 = (AsyncResult) msg.obj;
                    UiccCardApplication.this.onChangeFacilityLock(ar5);
                    break;
                case 9:
                    UiccCardApplication.this.log("handleMessage (EVENT_RADIO_UNAVAILABLE)");
                    UiccCardApplication.this.mAppState = IccCardApplicationStatus.AppState.APPSTATE_UNKNOWN;
                    break;
                default:
                    UiccCardApplication.this.loge("Unknown Event " + msg.what);
                    break;
            }
        }
    };

    UiccCardApplication(UiccCard uiccCard, IccCardApplicationStatus as, Context c, CommandsInterface ci) {
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
        this.mIccFh = createIccFileHandler(as.app_type);
        this.mIccRecords = createIccRecords(as.app_type, this.mContext, this.mCi);
        if (this.mAppState == IccCardApplicationStatus.AppState.APPSTATE_READY) {
            queryFdn();
            queryPin1State();
        }
        this.mCi.registerForNotAvailable(this.mHandler, 9, null);
    }

    void update(IccCardApplicationStatus as, Context c, CommandsInterface ci) {
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
            if (this.mPersoSubState != oldPersoSubState && this.mPersoSubState == IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_SIM_NETWORK) {
                notifyNetworkLockedRegistrantsIfNeeded(null);
            }
            if (this.mAppState == IccCardApplicationStatus.AppState.APPSTATE_READY) {
                queryFdn();
                queryPin1State();
            }
            if (this.mAppState != oldAppState) {
                log(oldAppType + " changed state: " + oldAppState + " -> " + this.mAppState);
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
        switch (type) {
            case APPTYPE_SIM:
                return new SIMFileHandler(this, this.mAid, this.mCi);
            case APPTYPE_RUIM:
                return new RuimFileHandler(this, this.mAid, this.mCi);
            case APPTYPE_USIM:
                return new UsimFileHandler(this, this.mAid, this.mCi);
            case APPTYPE_CSIM:
                return new CsimFileHandler(this, this.mAid, this.mCi);
            case APPTYPE_ISIM:
                return new IsimFileHandler(this, this.mAid, this.mCi);
            default:
                return null;
        }
    }

    void queryFdn() {
        this.mCi.queryFacilityLockForApp(CommandsInterface.CB_FACILITY_BA_FD, "", 7, this.mAid, this.mHandler.obtainMessage(4));
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
            if (ar.exception == null) {
                this.mIccFdnEnabled = this.mDesiredFdnEnabled;
                log("EVENT_CHANGE_FACILITY_FDN_DONE: mIccFdnEnabled=" + this.mIccFdnEnabled);
            } else {
                attemptsRemaining = parsePinPukErrorResult(ar);
                loge("Error change facility fdn with exception " + ar.exception);
            }
            Message response = (Message) ar.userObj;
            response.arg1 = attemptsRemaining;
            AsyncResult.forMessage(response).exception = ar.exception;
            response.sendToTarget();
        }
    }

    private void queryPin1State() {
        this.mCi.queryFacilityLockForApp(CommandsInterface.CB_FACILITY_BA_SIM, "", 7, this.mAid, this.mHandler.obtainMessage(6));
    }

    private void onQueryFacilityLock(AsyncResult ar) {
        synchronized (this.mLock) {
            if (ar.exception != null) {
                log("Error in querying facility lock:" + ar.exception);
                return;
            }
            int[] ints = (int[]) ar.result;
            if (ints.length != 0) {
                log("Query facility lock : " + ints[0]);
                this.mIccLockEnabled = ints[0] != 0;
                if (this.mIccLockEnabled) {
                    this.mPinLockedRegistrants.notifyRegistrants();
                }
                switch (this.mPin1State) {
                    case PINSTATE_DISABLED:
                        if (this.mIccLockEnabled) {
                            loge("QUERY_FACILITY_LOCK:enabled GET_SIM_STATUS.Pin1:disabled. Fixme");
                        }
                        break;
                    case PINSTATE_ENABLED_NOT_VERIFIED:
                    case PINSTATE_ENABLED_VERIFIED:
                    case PINSTATE_ENABLED_BLOCKED:
                    case PINSTATE_ENABLED_PERM_BLOCKED:
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
        if (!this.mDestroyed && this.mAppState == IccCardApplicationStatus.AppState.APPSTATE_READY) {
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
    }

    private void notifyPinLockedRegistrantsIfNeeded(Registrant r) {
        if (!this.mDestroyed) {
            if (this.mAppState == IccCardApplicationStatus.AppState.APPSTATE_PIN || this.mAppState == IccCardApplicationStatus.AppState.APPSTATE_PUK) {
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
        }
    }

    private void notifyNetworkLockedRegistrantsIfNeeded(Registrant r) {
        if (!this.mDestroyed && this.mAppState == IccCardApplicationStatus.AppState.APPSTATE_SUBSCRIPTION_PERSO && this.mPersoSubState == IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_SIM_NETWORK) {
            if (r == null) {
                log("Notifying registrants: NETWORK_LOCKED");
                this.mNetworkLockedRegistrants.notifyRegistrants();
            } else {
                log("Notifying 1 registrant: NETWORK_LOCED");
                r.notifyRegistrant(new AsyncResult((Object) null, (Object) null, (Throwable) null));
            }
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
        switch (appType) {
            case APPTYPE_SIM:
                return 128;
            case APPTYPE_RUIM:
            default:
                return -1;
            case APPTYPE_USIM:
                return 129;
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
        IccCardStatus.PinState universalPinState;
        synchronized (this.mLock) {
            universalPinState = this.mPin1Replaced ? this.mUiccCard.getUniversalPinState() : this.mPin1State;
        }
        return universalPinState;
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
            this.mCi.supplyIccPukForApp(puk, newPin, this.mAid, this.mHandler.obtainMessage(1, onComplete));
        }
    }

    public void supplyPin2(String pin2, Message onComplete) {
        synchronized (this.mLock) {
            this.mCi.supplyIccPin2ForApp(pin2, this.mAid, this.mHandler.obtainMessage(8, onComplete));
        }
    }

    public void supplyPuk2(String puk2, String newPin2, Message onComplete) {
        synchronized (this.mLock) {
            this.mCi.supplyIccPuk2ForApp(puk2, newPin2, this.mAid, this.mHandler.obtainMessage(8, onComplete));
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
        return this.mIccFdnAvailable;
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
        Rlog.d(LOG_TAG, msg);
    }

    private void loge(String msg) {
        Rlog.e(LOG_TAG, msg);
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
}
