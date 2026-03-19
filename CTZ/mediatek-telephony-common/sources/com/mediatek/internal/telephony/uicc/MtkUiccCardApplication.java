package com.mediatek.internal.telephony.uicc;

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
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.RuimRecords;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.uicc.UiccProfile;
import com.mediatek.internal.telephony.uicc.IccServiceInfo;

public class MtkUiccCardApplication extends UiccCardApplication {
    public static final int CAT_CORPORATE = 3;
    public static final int CAT_NETOWRK_SUBSET = 1;
    public static final int CAT_NETWOEK = 0;
    public static final int CAT_SERVICE_PROVIDER = 2;
    public static final int CAT_SIM = 4;
    private static final boolean DBG = true;
    private static final int EVENT_CHANGE_NETWORK_LOCK_DONE = 102;
    private static final int EVENT_PUK1_CHANGE_PIN1_DONE = 104;
    private static final int EVENT_PUK2_CHANGE_PIN2_DONE = 105;
    private static final int EVENT_QUERY_NETWORK_LOCK_DONE = 101;
    private static final int EVENT_RADIO_NOTAVAILABLE = 103;
    private static final String LOG_TAG_EX = "MtkUiccCardApp";
    public static final int OP_ADD = 2;
    public static final int OP_LOCK = 1;
    public static final int OP_PERMANENT_UNLOCK = 4;
    public static final int OP_REMOVE = 3;
    public static final int OP_UNLOCK = 0;
    private RegistrantList mFdnChangedRegistrants;
    private Handler mHandlerEx;
    protected String mIccType;
    protected int mPhoneId;
    static final String[] UICCCARDAPPLICATION_PROPERTY_RIL_UICC_TYPE = {"vendor.gsm.ril.uicctype", "vendor.gsm.ril.uicctype.2", "vendor.gsm.ril.uicctype.3", "vendor.gsm.ril.uicctype.4"};
    private static final String[] PROPERTY_PIN1_RETRY = {"vendor.gsm.sim.retry.pin1", "vendor.gsm.sim.retry.pin1.2", "vendor.gsm.sim.retry.pin1.3", "vendor.gsm.sim.retry.pin1.4"};
    private static final String[] PROPERTY_PIN2_RETRY = {"vendor.gsm.sim.retry.pin2", "vendor.gsm.sim.retry.pin2.2", "vendor.gsm.sim.retry.pin2.3", "vendor.gsm.sim.retry.pin2.4"};

    public MtkUiccCardApplication(UiccProfile uiccProfile, IccCardApplicationStatus iccCardApplicationStatus, Context context, CommandsInterface commandsInterface) {
        super(uiccProfile, iccCardApplicationStatus, context, commandsInterface);
        this.mIccType = null;
        this.mHandlerEx = new Handler() {
            @Override
            public void handleMessage(Message message) {
                int pinPukErrorResult = -1;
                if (MtkUiccCardApplication.this.mDestroyed) {
                    if (1 == message.what || 101 == message.what) {
                        Message message2 = (Message) ((AsyncResult) message.obj).userObj;
                        AsyncResult.forMessage(message2).exception = CommandException.fromRilErrno(1);
                        MtkUiccCardApplication.this.mtkLoge("Received message " + message + "[" + message.what + "] while being destroyed. return exception.");
                        message2.arg1 = -1;
                        message2.sendToTarget();
                    }
                    MtkUiccCardApplication.this.mtkLoge("Received message " + message + "[" + message.what + "] while being destroyed. Ignoring.");
                    return;
                }
                switch (message.what) {
                    case 1:
                        AsyncResult asyncResult = (AsyncResult) message.obj;
                        if (asyncResult.exception != null && asyncResult.result != null) {
                            pinPukErrorResult = MtkUiccCardApplication.this.parsePinPukErrorResult(asyncResult);
                        }
                        Message message3 = (Message) asyncResult.userObj;
                        AsyncResult.forMessage(message3).exception = asyncResult.exception;
                        message3.arg1 = pinPukErrorResult;
                        message3.sendToTarget();
                        break;
                    case 101:
                        MtkUiccCardApplication.this.mtkLog("handleMessage (EVENT_QUERY_NETWORK_LOCK)");
                        AsyncResult asyncResult2 = (AsyncResult) message.obj;
                        if (asyncResult2.exception != null) {
                            Rlog.e(MtkUiccCardApplication.LOG_TAG_EX, "Error query network lock with exception " + asyncResult2.exception);
                        }
                        AsyncResult.forMessage((Message) asyncResult2.userObj, asyncResult2.result, asyncResult2.exception);
                        ((Message) asyncResult2.userObj).sendToTarget();
                        break;
                    case 102:
                        MtkUiccCardApplication.this.mtkLog("handleMessage (EVENT_CHANGE_NETWORK_LOCK)");
                        AsyncResult asyncResult3 = (AsyncResult) message.obj;
                        if (asyncResult3.exception != null) {
                            Rlog.e(MtkUiccCardApplication.LOG_TAG_EX, "Error change network lock with exception " + asyncResult3.exception);
                        }
                        AsyncResult.forMessage((Message) asyncResult3.userObj).exception = asyncResult3.exception;
                        ((Message) asyncResult3.userObj).sendToTarget();
                        break;
                    case 104:
                        MtkUiccCardApplication.this.mtkLog("EVENT_PUK1_CHANGE_PIN1_DONE");
                        AsyncResult asyncResult4 = (AsyncResult) message.obj;
                        if (asyncResult4.exception != null && asyncResult4.result != null) {
                            pinPukErrorResult = MtkUiccCardApplication.this.parsePinPukErrorResult(asyncResult4);
                        }
                        Message message4 = (Message) asyncResult4.userObj;
                        AsyncResult.forMessage(message4).exception = asyncResult4.exception;
                        message4.arg1 = pinPukErrorResult;
                        message4.sendToTarget();
                        MtkUiccCardApplication.this.queryPin1State();
                        break;
                    case 105:
                        AsyncResult asyncResult5 = (AsyncResult) message.obj;
                        if (asyncResult5.exception != null && asyncResult5.result != null) {
                            pinPukErrorResult = MtkUiccCardApplication.this.parsePinPukErrorResult(asyncResult5);
                        }
                        Message message5 = (Message) asyncResult5.userObj;
                        AsyncResult.forMessage(message5).exception = asyncResult5.exception;
                        message5.arg1 = pinPukErrorResult;
                        message5.sendToTarget();
                        MtkUiccCardApplication.this.queryFdn();
                        break;
                    default:
                        MtkUiccCardApplication.this.mtkLoge("Unknown Event " + message.what);
                        break;
                }
            }
        };
        this.mFdnChangedRegistrants = new RegistrantList();
        this.mAuthContext = getAuthContextEx(this.mAppType);
        this.mPhoneId = getPhoneId();
    }

    public void update(IccCardApplicationStatus iccCardApplicationStatus, Context context, CommandsInterface commandsInterface) {
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                mtkLoge("Application updated after destroyed! Fix me!");
                return;
            }
            mtkLog(this.mAppType + " update. New " + iccCardApplicationStatus);
            this.mContext = context;
            this.mCi = commandsInterface;
            IccCardApplicationStatus.AppType appType = this.mAppType;
            IccCardApplicationStatus.AppState appState = this.mAppState;
            IccCardApplicationStatus.PersoSubState persoSubState = this.mPersoSubState;
            this.mAppType = iccCardApplicationStatus.app_type;
            this.mAuthContext = getAuthContextEx(this.mAppType);
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
            mtkLog("mPersoSubState: " + this.mPersoSubState + " oldPersoSubState: " + persoSubState);
            if (this.mPersoSubState != persoSubState) {
                notifyNetworkLockedRegistrantsIfNeeded(null);
            }
            mtkLog("update,  mAppState=" + this.mAppState + "  oldAppState=" + appState);
            if (this.mAppState != appState) {
                mtkLog(appType + " changed state: " + appState + " -> " + this.mAppState);
                if (this.mAppState == IccCardApplicationStatus.AppState.APPSTATE_READY && this.mAppType != IccCardApplicationStatus.AppType.APPTYPE_ISIM) {
                    queryFdn();
                    queryPin1State();
                }
                notifyPinLockedRegistrantsIfNeeded(null);
                notifyReadyRegistrantsIfNeeded(null);
            } else if (this.mAppState == IccCardApplicationStatus.AppState.APPSTATE_READY && ((this.mAppType == IccCardApplicationStatus.AppType.APPTYPE_SIM && appType == IccCardApplicationStatus.AppType.APPTYPE_RUIM) || (this.mAppType == IccCardApplicationStatus.AppType.APPTYPE_RUIM && appType == IccCardApplicationStatus.AppType.APPTYPE_SIM))) {
                queryFdn();
                queryPin1State();
            }
        }
    }

    protected void notifyNetworkLockedRegistrantsIfNeeded(Registrant registrant) {
        if (!this.mDestroyed && this.mAppState == IccCardApplicationStatus.AppState.APPSTATE_SUBSCRIPTION_PERSO) {
            if (registrant == null) {
                mtkLog("Notifying registrants: NETWORK_LOCKED");
                this.mNetworkLockedRegistrants.notifyRegistrants();
            } else {
                mtkLog("Notifying 1 registrant: NETWORK_LOCED");
                registrant.notifyRegistrant(new AsyncResult((Object) null, (Object) null, (Throwable) null));
            }
        }
    }

    protected IccRecords createIccRecords(IccCardApplicationStatus.AppType appType, Context context, CommandsInterface commandsInterface) {
        mtkLog("UiccCardAppEx createIccRecords, AppType = " + appType);
        if (appType == IccCardApplicationStatus.AppType.APPTYPE_USIM || appType == IccCardApplicationStatus.AppType.APPTYPE_SIM) {
            return new MtkSIMRecords(this, context, commandsInterface);
        }
        if (appType == IccCardApplicationStatus.AppType.APPTYPE_RUIM || appType == IccCardApplicationStatus.AppType.APPTYPE_CSIM) {
            return new MtkRuimRecords(this, context, commandsInterface);
        }
        if (appType == IccCardApplicationStatus.AppType.APPTYPE_ISIM) {
            return new MtkIsimUiccRecords(this, context, commandsInterface);
        }
        return null;
    }

    static class AnonymousClass2 {
        static final int[] $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$AppType = new int[IccCardApplicationStatus.AppType.values().length];

        static {
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$AppType[IccCardApplicationStatus.AppType.APPTYPE_SIM.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$AppType[IccCardApplicationStatus.AppType.APPTYPE_RUIM.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$AppType[IccCardApplicationStatus.AppType.APPTYPE_USIM.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$AppType[IccCardApplicationStatus.AppType.APPTYPE_CSIM.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$AppType[IccCardApplicationStatus.AppType.APPTYPE_ISIM.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
        }
    }

    protected IccFileHandler createIccFileHandler(IccCardApplicationStatus.AppType appType) {
        switch (AnonymousClass2.$SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$AppType[appType.ordinal()]) {
            case 1:
                return new MtkSIMFileHandler(this, this.mAid, this.mCi);
            case 2:
                return new MtkRuimFileHandler(this, this.mAid, this.mCi);
            case 3:
                return new MtkUsimFileHandler(this, this.mAid, this.mCi);
            case 4:
                return new MtkCsimFileHandler(this, this.mAid, this.mCi);
            case 5:
                return new MtkIsimFileHandler(this, this.mAid, this.mCi);
            default:
                return null;
        }
    }

    protected void onChangeFdnDone(AsyncResult asyncResult) {
        super.onChangeFdnDone(asyncResult);
        if (asyncResult.exception == null) {
            mtkLog("notifyFdnChangedRegistrants");
            notifyFdnChangedRegistrants();
        }
    }

    private static int getAuthContextEx(IccCardApplicationStatus.AppType appType) {
        int i = AnonymousClass2.$SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$AppType[appType.ordinal()];
        if (i == 1) {
            return 128;
        }
        if (i == 3 || i == 5) {
            return 129;
        }
        return -1;
    }

    public void supplyPin(String str, Message message) {
        synchronized (this.mLock) {
            this.mCi.supplyIccPinForApp(str, this.mAid, this.mHandlerEx.obtainMessage(1, message));
        }
    }

    public void supplyPuk(String str, String str2, Message message) {
        synchronized (this.mLock) {
            mtkLog("supplyPuk");
            this.mCi.supplyIccPukForApp(str, str2, this.mAid, this.mHandlerEx.obtainMessage(104, message));
        }
    }

    public void supplyPuk2(String str, String str2, Message message) {
        synchronized (this.mLock) {
            this.mCi.supplyIccPuk2ForApp(str, str2, this.mAid, this.mHandlerEx.obtainMessage(105, message));
        }
    }

    public void queryIccNetworkLock(int i, Message message) {
        mtkLog("queryIccNetworkLock(): category =  " + i);
        switch (i) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
                this.mCi.queryNetworkLock(i, this.mHandlerEx.obtainMessage(101, message));
                break;
            default:
                Rlog.e(LOG_TAG_EX, "queryIccNetworkLock unknown category = " + i);
                break;
        }
    }

    public void setIccNetworkLockEnabled(int i, int i2, String str, String str2, String str3, String str4, Message message) {
        mtkLog("SetIccNetworkEnabled(): category = " + i + " lockop = " + i2 + " password = " + str + " data_imsi = " + str2 + " gid1 = " + str3 + " gid2 = " + str4);
        switch (i2) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
                this.mCi.setNetworkLock(i, i2, str, str2, str3, str4, this.mHandlerEx.obtainMessage(102, message));
                break;
            default:
                Rlog.e(LOG_TAG_EX, "SetIccNetworkEnabled unknown operation" + i2);
                break;
        }
    }

    public void registerForFdnChanged(Handler handler, int i, Object obj) {
        synchronized (this.mLock) {
            this.mFdnChangedRegistrants.add(new Registrant(handler, i, obj));
        }
    }

    public void unregisterForFdnChanged(Handler handler) {
        synchronized (this.mLock) {
            this.mFdnChangedRegistrants.remove(handler);
        }
    }

    private void notifyFdnChangedRegistrants() {
        if (this.mDestroyed) {
            return;
        }
        this.mFdnChangedRegistrants.notifyRegistrants();
    }

    public String getIccCardType() {
        if (this.mIccType == null || this.mIccType.equals("")) {
            this.mIccType = SystemProperties.get(UICCCARDAPPLICATION_PROPERTY_RIL_UICC_TYPE[this.mPhoneId]);
        }
        mtkLog("getIccCardType(): mIccType = " + this.mIccType);
        return this.mIccType;
    }

    public void queryFdn() {
        if (getType() == IccCardApplicationStatus.AppType.APPTYPE_ISIM) {
            mtkLog("queryFdn(): do nothing for ISIM.");
        } else {
            this.mCi.queryFacilityLockForApp("FD", "", 7, this.mAid, this.mHandler.obtainMessage(4));
        }
    }

    protected void queryPin1State() {
        if (getType() == IccCardApplicationStatus.AppType.APPTYPE_ISIM) {
            mtkLog("queryPin1State(): do nothing for ISIM.");
        } else {
            this.mCi.queryFacilityLockForApp("SC", "", 7, this.mAid, this.mHandler.obtainMessage(6));
        }
    }

    public boolean getIccFdnAvailable() {
        IccServiceInfo.IccServiceStatus sIMServiceStatus;
        boolean zIsPhbReady;
        if (this.mIccRecords == null) {
            mtkLoge("isFdnExist mIccRecords == null");
            return false;
        }
        IccServiceInfo.IccServiceStatus iccServiceStatus = IccServiceInfo.IccServiceStatus.NOT_EXIST_IN_USIM;
        if (this.mIccRecords instanceof MtkSIMRecords) {
            sIMServiceStatus = this.mIccRecords.getSIMServiceStatus(IccServiceInfo.IccService.FDN);
            zIsPhbReady = this.mIccRecords.isPhbReady();
        } else if (this.mIccRecords instanceof RuimRecords) {
            sIMServiceStatus = this.mIccRecords.getSIMServiceStatus(IccServiceInfo.IccService.FDN);
            zIsPhbReady = this.mIccRecords.isPhbReady();
        } else {
            sIMServiceStatus = IccServiceInfo.IccServiceStatus.NOT_EXIST_IN_USIM;
            zIsPhbReady = false;
        }
        log("getIccFdnAvailable status: iccSerStatus");
        return sIMServiceStatus == IccServiceInfo.IccServiceStatus.ACTIVATED && zIsPhbReady;
    }

    protected void notifyPinLockStatus() {
        notifyPinLockedRegistrantsIfNeeded(null);
    }

    protected void log(String str) {
        Rlog.d("UiccCardApplication", str + " (slot " + this.mPhoneId + ")");
    }

    protected void loge(String str) {
        Rlog.e("UiccCardApplication", str + " (slot " + this.mPhoneId + ")");
    }

    protected void mtkLog(String str) {
        Rlog.d(LOG_TAG_EX, str + " (slot " + this.mPhoneId + ")");
    }

    protected void mtkLoge(String str) {
        Rlog.e(LOG_TAG_EX, str + " (slot " + this.mPhoneId + ")");
    }
}
