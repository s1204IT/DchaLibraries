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
    protected static final int EVENT_PIN1_PUK1_DONE = 1;
    private static final int EVENT_PIN2_PUK2_DONE = 8;
    protected static final int EVENT_QUERY_FACILITY_FDN_DONE = 4;
    protected static final int EVENT_QUERY_FACILITY_LOCK_DONE = 6;
    private static final int EVENT_RADIO_UNAVAILABLE = 9;
    protected static final String LOG_TAG = "UiccCardApplication";
    protected String mAid;
    protected String mAppLabel;
    protected IccCardApplicationStatus.AppState mAppState;
    protected IccCardApplicationStatus.AppType mAppType;
    protected int mAuthContext;
    protected CommandsInterface mCi;
    protected Context mContext;
    private boolean mDesiredFdnEnabled;
    private boolean mDesiredPinLocked;
    protected boolean mDestroyed;
    protected boolean mIccFdnEnabled;
    protected IccFileHandler mIccFh;
    private boolean mIccLockEnabled;
    protected IccRecords mIccRecords;
    private boolean mIgnoreApp;
    protected IccCardApplicationStatus.PersoSubState mPersoSubState;
    protected boolean mPin1Replaced;
    protected IccCardStatus.PinState mPin1State;
    protected IccCardStatus.PinState mPin2State;
    private UiccProfile mUiccProfile;
    protected final Object mLock = new Object();
    private boolean mIccFdnAvailable = true;
    private RegistrantList mReadyRegistrants = new RegistrantList();
    private RegistrantList mPinLockedRegistrants = new RegistrantList();
    protected RegistrantList mNetworkLockedRegistrants = new RegistrantList();
    protected Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            if (UiccCardApplication.this.mDestroyed) {
                UiccCardApplication.this.loge("Received message " + message + "[" + message.what + "] while being destroyed. Ignoring.");
            }
            switch (message.what) {
                case 1:
                case 2:
                case 3:
                case 8:
                    AsyncResult asyncResult = (AsyncResult) message.obj;
                    int pinPukErrorResult = UiccCardApplication.this.parsePinPukErrorResult(asyncResult);
                    Message message2 = (Message) asyncResult.userObj;
                    AsyncResult.forMessage(message2).exception = asyncResult.exception;
                    message2.arg1 = pinPukErrorResult;
                    message2.sendToTarget();
                    break;
                case 4:
                    UiccCardApplication.this.onQueryFdnEnabled((AsyncResult) message.obj);
                    break;
                case 5:
                    UiccCardApplication.this.onChangeFdnDone((AsyncResult) message.obj);
                    break;
                case 6:
                    UiccCardApplication.this.onQueryFacilityLock((AsyncResult) message.obj);
                    break;
                case 7:
                    UiccCardApplication.this.onChangeFacilityLock((AsyncResult) message.obj);
                    break;
                case 9:
                    UiccCardApplication.this.log("handleMessage (EVENT_RADIO_UNAVAILABLE)");
                    UiccCardApplication.this.mAppState = IccCardApplicationStatus.AppState.APPSTATE_UNKNOWN;
                    break;
                default:
                    UiccCardApplication.this.loge("Unknown Event " + message.what);
                    break;
            }
        }
    };

    public UiccCardApplication(UiccProfile uiccProfile, IccCardApplicationStatus iccCardApplicationStatus, Context context, CommandsInterface commandsInterface) {
        log("Creating UiccApp: " + iccCardApplicationStatus);
        this.mUiccProfile = uiccProfile;
        this.mAppState = iccCardApplicationStatus.app_state;
        this.mAppType = iccCardApplicationStatus.app_type;
        this.mAuthContext = getAuthContext(this.mAppType);
        this.mPersoSubState = iccCardApplicationStatus.perso_substate;
        this.mAid = iccCardApplicationStatus.aid;
        this.mAppLabel = iccCardApplicationStatus.app_label;
        this.mPin1Replaced = iccCardApplicationStatus.pin1_replaced != 0;
        this.mPin1State = iccCardApplicationStatus.pin1;
        this.mPin2State = iccCardApplicationStatus.pin2;
        this.mIgnoreApp = false;
        this.mContext = context;
        this.mCi = commandsInterface;
        this.mIccFh = createIccFileHandler(iccCardApplicationStatus.app_type);
        this.mIccRecords = createIccRecords(iccCardApplicationStatus.app_type, this.mContext, this.mCi);
        if (this.mAppState == IccCardApplicationStatus.AppState.APPSTATE_READY) {
            queryFdn();
            queryPin1State();
        }
        this.mCi.registerForNotAvailable(this.mHandler, 9, null);
    }

    public void update(IccCardApplicationStatus iccCardApplicationStatus, Context context, CommandsInterface commandsInterface) {
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                loge("Application updated after destroyed! Fix me!");
                return;
            }
            log(this.mAppType + " update. New " + iccCardApplicationStatus);
            this.mContext = context;
            this.mCi = commandsInterface;
            IccCardApplicationStatus.AppType appType = this.mAppType;
            IccCardApplicationStatus.AppState appState = this.mAppState;
            IccCardApplicationStatus.PersoSubState persoSubState = this.mPersoSubState;
            this.mAppType = iccCardApplicationStatus.app_type;
            this.mAuthContext = getAuthContext(this.mAppType);
            this.mAppState = iccCardApplicationStatus.app_state;
            this.mPersoSubState = iccCardApplicationStatus.perso_substate;
            this.mAid = iccCardApplicationStatus.aid;
            this.mAppLabel = iccCardApplicationStatus.app_label;
            this.mPin1Replaced = iccCardApplicationStatus.pin1_replaced != 0;
            this.mPin1State = iccCardApplicationStatus.pin1;
            this.mPin2State = iccCardApplicationStatus.pin2;
            if (this.mAppType != appType) {
                if (this.mIccFh != null) {
                    this.mIccFh.dispose();
                }
                if (this.mIccRecords != null) {
                    this.mIccRecords.dispose();
                }
                this.mIccFh = createIccFileHandler(iccCardApplicationStatus.app_type);
                this.mIccRecords = createIccRecords(iccCardApplicationStatus.app_type, context, commandsInterface);
            }
            if (this.mPersoSubState != persoSubState && (this.mPersoSubState == IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_SIM_NETWORK || supportExtendedMeLockCategory())) {
                notifyNetworkLockedRegistrantsIfNeeded(null);
            }
            if (this.mAppState != appState) {
                log(appType + " changed state: " + appState + " -> " + this.mAppState);
                if (this.mAppState == IccCardApplicationStatus.AppState.APPSTATE_READY) {
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

    protected IccRecords createIccRecords(IccCardApplicationStatus.AppType appType, Context context, CommandsInterface commandsInterface) {
        if (appType == IccCardApplicationStatus.AppType.APPTYPE_USIM || appType == IccCardApplicationStatus.AppType.APPTYPE_SIM) {
            return new SIMRecords(this, context, commandsInterface);
        }
        if (appType == IccCardApplicationStatus.AppType.APPTYPE_RUIM || appType == IccCardApplicationStatus.AppType.APPTYPE_CSIM) {
            return new RuimRecords(this, context, commandsInterface);
        }
        if (appType == IccCardApplicationStatus.AppType.APPTYPE_ISIM) {
            return new IsimUiccRecords(this, context, commandsInterface);
        }
        return null;
    }

    protected IccFileHandler createIccFileHandler(IccCardApplicationStatus.AppType appType) {
        switch (appType) {
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

    public void queryFdn() {
        this.mCi.queryFacilityLockForApp(CommandsInterface.CB_FACILITY_BA_FD, "", 7, this.mAid, this.mHandler.obtainMessage(4));
    }

    private void onQueryFdnEnabled(AsyncResult asyncResult) {
        synchronized (this.mLock) {
            if (asyncResult.exception != null) {
                log("Error in querying facility lock:" + asyncResult.exception);
                return;
            }
            int[] iArr = (int[]) asyncResult.result;
            if (iArr.length != 0) {
                if (iArr[0] == 2) {
                    this.mIccFdnEnabled = false;
                    this.mIccFdnAvailable = false;
                } else {
                    this.mIccFdnEnabled = iArr[0] == 1;
                    this.mIccFdnAvailable = true;
                }
                log("Query facility FDN : FDN service available: " + this.mIccFdnAvailable + " enabled: " + this.mIccFdnEnabled);
            } else {
                loge("Bogus facility lock response");
            }
        }
    }

    protected void onChangeFdnDone(AsyncResult asyncResult) {
        synchronized (this.mLock) {
            int pinPukErrorResult = -1;
            if (asyncResult.exception == null) {
                this.mIccFdnEnabled = this.mDesiredFdnEnabled;
                log("EVENT_CHANGE_FACILITY_FDN_DONE: mIccFdnEnabled=" + this.mIccFdnEnabled);
            } else {
                pinPukErrorResult = parsePinPukErrorResult(asyncResult);
                loge("Error change facility fdn with exception " + asyncResult.exception);
            }
            Message message = (Message) asyncResult.userObj;
            message.arg1 = pinPukErrorResult;
            AsyncResult.forMessage(message).exception = asyncResult.exception;
            message.sendToTarget();
        }
    }

    protected void queryPin1State() {
        this.mCi.queryFacilityLockForApp(CommandsInterface.CB_FACILITY_BA_SIM, "", 7, this.mAid, this.mHandler.obtainMessage(6));
    }

    private void onQueryFacilityLock(AsyncResult asyncResult) {
        synchronized (this.mLock) {
            if (asyncResult.exception != null) {
                log("Error in querying facility lock:" + asyncResult.exception);
                return;
            }
            int[] iArr = (int[]) asyncResult.result;
            if (iArr.length != 0) {
                log("Query facility lock : " + iArr[0]);
                this.mIccLockEnabled = iArr[0] != 0;
                if (this.mIccLockEnabled) {
                    notifyPinLockStatus();
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
                        }
                        log("Ignoring: pin1state=" + this.mPin1State);
                        break;
                    default:
                        log("Ignoring: pin1state=" + this.mPin1State);
                        break;
                }
            } else {
                loge("Bogus facility lock response");
            }
        }
    }

    private void onChangeFacilityLock(AsyncResult asyncResult) {
        synchronized (this.mLock) {
            int pinPukErrorResult = -1;
            if (asyncResult.exception == null) {
                this.mIccLockEnabled = this.mDesiredPinLocked;
                log("EVENT_CHANGE_FACILITY_LOCK_DONE: mIccLockEnabled= " + this.mIccLockEnabled);
            } else {
                pinPukErrorResult = parsePinPukErrorResult(asyncResult);
                loge("Error change facility lock with exception " + asyncResult.exception);
            }
            Message message = (Message) asyncResult.userObj;
            AsyncResult.forMessage(message).exception = asyncResult.exception;
            message.arg1 = pinPukErrorResult;
            message.sendToTarget();
        }
    }

    protected int parsePinPukErrorResult(AsyncResult asyncResult) {
        int[] iArr = (int[]) asyncResult.result;
        int i = -1;
        if (iArr == null) {
            return -1;
        }
        if (iArr.length > 0) {
            i = iArr[0];
        }
        log("parsePinPukErrorResult: attemptsRemaining=" + i);
        return i;
    }

    public void registerForReady(Handler handler, int i, Object obj) {
        synchronized (this.mLock) {
            Registrant registrant = new Registrant(handler, i, obj);
            this.mReadyRegistrants.add(registrant);
            notifyReadyRegistrantsIfNeeded(registrant);
        }
    }

    public void unregisterForReady(Handler handler) {
        synchronized (this.mLock) {
            this.mReadyRegistrants.remove(handler);
        }
    }

    protected void registerForLocked(Handler handler, int i, Object obj) {
        synchronized (this.mLock) {
            Registrant registrant = new Registrant(handler, i, obj);
            this.mPinLockedRegistrants.add(registrant);
            notifyPinLockedRegistrantsIfNeeded(registrant);
        }
    }

    protected void unregisterForLocked(Handler handler) {
        synchronized (this.mLock) {
            this.mPinLockedRegistrants.remove(handler);
        }
    }

    protected void registerForNetworkLocked(Handler handler, int i, Object obj) {
        synchronized (this.mLock) {
            Registrant registrant = new Registrant(handler, i, obj);
            this.mNetworkLockedRegistrants.add(registrant);
            notifyNetworkLockedRegistrantsIfNeeded(registrant);
        }
    }

    protected void unregisterForNetworkLocked(Handler handler) {
        synchronized (this.mLock) {
            this.mNetworkLockedRegistrants.remove(handler);
        }
    }

    protected void notifyReadyRegistrantsIfNeeded(Registrant registrant) {
        if (!this.mDestroyed && this.mAppState == IccCardApplicationStatus.AppState.APPSTATE_READY) {
            if (this.mPin1State == IccCardStatus.PinState.PINSTATE_ENABLED_NOT_VERIFIED || this.mPin1State == IccCardStatus.PinState.PINSTATE_ENABLED_BLOCKED || this.mPin1State == IccCardStatus.PinState.PINSTATE_ENABLED_PERM_BLOCKED) {
                loge("Sanity check failed! APPSTATE is ready while PIN1 is not verified!!!");
            } else if (registrant == null) {
                log("Notifying registrants: READY");
                this.mReadyRegistrants.notifyRegistrants();
            } else {
                log("Notifying 1 registrant: READY");
                registrant.notifyRegistrant(new AsyncResult((Object) null, (Object) null, (Throwable) null));
            }
        }
    }

    protected void notifyPinLockedRegistrantsIfNeeded(Registrant registrant) {
        if (this.mDestroyed) {
            return;
        }
        if (this.mAppState == IccCardApplicationStatus.AppState.APPSTATE_PIN || this.mAppState == IccCardApplicationStatus.AppState.APPSTATE_PUK) {
            if (this.mPin1State == IccCardStatus.PinState.PINSTATE_ENABLED_VERIFIED || this.mPin1State == IccCardStatus.PinState.PINSTATE_DISABLED) {
                loge("Sanity check failed! APPSTATE is locked while PIN1 is not!!!");
            } else if (registrant == null) {
                log("Notifying registrants: LOCKED");
                this.mPinLockedRegistrants.notifyRegistrants();
            } else {
                log("Notifying 1 registrant: LOCKED");
                registrant.notifyRegistrant(new AsyncResult((Object) null, (Object) null, (Throwable) null));
            }
        }
    }

    protected void notifyNetworkLockedRegistrantsIfNeeded(Registrant registrant) {
        if (!this.mDestroyed && this.mAppState == IccCardApplicationStatus.AppState.APPSTATE_SUBSCRIPTION_PERSO && this.mPersoSubState == IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_SIM_NETWORK) {
            if (registrant == null) {
                log("Notifying registrants: NETWORK_LOCKED");
                this.mNetworkLockedRegistrants.notifyRegistrants();
            } else {
                log("Notifying 1 registrant: NETWORK_LOCED");
                registrant.notifyRegistrant(new AsyncResult((Object) null, (Object) null, (Throwable) null));
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
        int i = AnonymousClass2.$SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$AppType[appType.ordinal()];
        if (i == 1) {
            return 128;
        }
        if (i == 3) {
            return 129;
        }
        return -1;
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
                return this.mUiccProfile.getUniversalPinState();
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

    public void supplyPin(String str, Message message) {
        synchronized (this.mLock) {
            this.mCi.supplyIccPinForApp(str, this.mAid, this.mHandler.obtainMessage(1, message));
        }
    }

    public void supplyPuk(String str, String str2, Message message) {
        synchronized (this.mLock) {
            this.mCi.supplyIccPukForApp(str, str2, this.mAid, this.mHandler.obtainMessage(1, message));
        }
    }

    public void supplyPin2(String str, Message message) {
        synchronized (this.mLock) {
            this.mCi.supplyIccPin2ForApp(str, this.mAid, this.mHandler.obtainMessage(8, message));
        }
    }

    public void supplyPuk2(String str, String str2, Message message) {
        synchronized (this.mLock) {
            this.mCi.supplyIccPuk2ForApp(str, str2, this.mAid, this.mHandler.obtainMessage(8, message));
        }
    }

    public void supplyNetworkDepersonalization(String str, Message message) {
        synchronized (this.mLock) {
            log("supplyNetworkDepersonalization");
            this.mCi.supplyNetworkDepersonalization(str, message);
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

    public void setIccLockEnabled(boolean z, String str, Message message) {
        synchronized (this.mLock) {
            this.mDesiredPinLocked = z;
            this.mCi.setFacilityLockForApp(CommandsInterface.CB_FACILITY_BA_SIM, z, str, 7, this.mAid, this.mHandler.obtainMessage(7, message));
        }
    }

    public void setIccFdnEnabled(boolean z, String str, Message message) {
        synchronized (this.mLock) {
            this.mDesiredFdnEnabled = z;
            this.mCi.setFacilityLockForApp(CommandsInterface.CB_FACILITY_BA_FD, z, str, 15, this.mAid, this.mHandler.obtainMessage(5, message));
        }
    }

    public void changeIccLockPassword(String str, String str2, Message message) {
        synchronized (this.mLock) {
            log("changeIccLockPassword");
            this.mCi.changeIccPinForApp(str, str2, this.mAid, this.mHandler.obtainMessage(2, message));
        }
    }

    public void changeIccFdnPassword(String str, String str2, Message message) {
        synchronized (this.mLock) {
            log("changeIccFdnPassword");
            this.mCi.changeIccPin2ForApp(str, str2, this.mAid, this.mHandler.obtainMessage(3, message));
        }
    }

    public boolean isReady() {
        synchronized (this.mLock) {
            if (this.mAppState != IccCardApplicationStatus.AppState.APPSTATE_READY) {
                return false;
            }
            if (this.mPin1State != IccCardStatus.PinState.PINSTATE_ENABLED_NOT_VERIFIED && this.mPin1State != IccCardStatus.PinState.PINSTATE_ENABLED_BLOCKED && this.mPin1State != IccCardStatus.PinState.PINSTATE_ENABLED_PERM_BLOCKED) {
                return true;
            }
            loge("Sanity check failed! APPSTATE is ready while PIN1 is not verified!!!");
            return false;
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
        return this.mUiccProfile.getPhoneId();
    }

    public boolean isAppIgnored() {
        return this.mIgnoreApp;
    }

    public void setAppIgnoreState(boolean z) {
        this.mIgnoreApp = z;
    }

    protected UiccProfile getUiccProfile() {
        return this.mUiccProfile;
    }

    protected void log(String str) {
        Rlog.d(LOG_TAG, str);
    }

    protected void loge(String str) {
        Rlog.e(LOG_TAG, str);
    }

    protected boolean supportExtendedMeLockCategory() {
        return false;
    }

    protected void notifyPinLockStatus() {
        this.mPinLockedRegistrants.notifyRegistrants();
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("UiccCardApplication: " + this);
        printWriter.println(" mUiccProfile=" + this.mUiccProfile);
        printWriter.println(" mAppState=" + this.mAppState);
        printWriter.println(" mAppType=" + this.mAppType);
        printWriter.println(" mPersoSubState=" + this.mPersoSubState);
        printWriter.println(" mAid=" + this.mAid);
        printWriter.println(" mAppLabel=" + this.mAppLabel);
        printWriter.println(" mPin1Replaced=" + this.mPin1Replaced);
        printWriter.println(" mPin1State=" + this.mPin1State);
        printWriter.println(" mPin2State=" + this.mPin2State);
        printWriter.println(" mIccFdnEnabled=" + this.mIccFdnEnabled);
        printWriter.println(" mDesiredFdnEnabled=" + this.mDesiredFdnEnabled);
        printWriter.println(" mIccLockEnabled=" + this.mIccLockEnabled);
        printWriter.println(" mDesiredPinLocked=" + this.mDesiredPinLocked);
        printWriter.println(" mCi=" + this.mCi);
        printWriter.println(" mIccRecords=" + this.mIccRecords);
        printWriter.println(" mIccFh=" + this.mIccFh);
        printWriter.println(" mDestroyed=" + this.mDestroyed);
        printWriter.println(" mReadyRegistrants: size=" + this.mReadyRegistrants.size());
        for (int i = 0; i < this.mReadyRegistrants.size(); i++) {
            printWriter.println("  mReadyRegistrants[" + i + "]=" + ((Registrant) this.mReadyRegistrants.get(i)).getHandler());
        }
        printWriter.println(" mPinLockedRegistrants: size=" + this.mPinLockedRegistrants.size());
        for (int i2 = 0; i2 < this.mPinLockedRegistrants.size(); i2++) {
            printWriter.println("  mPinLockedRegistrants[" + i2 + "]=" + ((Registrant) this.mPinLockedRegistrants.get(i2)).getHandler());
        }
        printWriter.println(" mNetworkLockedRegistrants: size=" + this.mNetworkLockedRegistrants.size());
        for (int i3 = 0; i3 < this.mNetworkLockedRegistrants.size(); i3++) {
            printWriter.println("  mNetworkLockedRegistrants[" + i3 + "]=" + ((Registrant) this.mNetworkLockedRegistrants.get(i3)).getHandler());
        }
        printWriter.flush();
    }
}
