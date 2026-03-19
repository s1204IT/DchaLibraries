package com.android.internal.telephony.uicc;

import android.R;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.format.Time;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.uicc.IccCardApplicationStatus;
import com.mediatek.common.MPlugin;
import com.mediatek.common.telephony.IUiccControllerExt;
import com.mediatek.internal.telephony.RadioManager;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.LinkedList;

public class UiccController extends Handler {

    private static final int[] f34x16bf601e = null;
    public static final int APP_FAM_3GPP = 1;
    public static final int APP_FAM_3GPP2 = 2;
    public static final int APP_FAM_IMS = 3;
    private static final String COMMON_SLOT_PROPERTY = "ro.mtk_sim_hot_swap_common_slot";
    private static final boolean DBG = true;
    private static final String DECRYPT_STATE = "trigger_restart_framework";
    protected static final int EVENT_COMMON_SLOT_NO_CHANGED = 116;
    private static final int EVENT_GET_ICC_STATUS_DONE = 2;
    protected static final int EVENT_GET_ICC_STATUS_DONE_FOR_SIM_MISSING = 106;
    protected static final int EVENT_GET_ICC_STATUS_DONE_FOR_SIM_RECOVERY = 107;
    protected static final int EVENT_HOTSWAP_GET_ICC_STATUS_DONE = 111;
    private static final int EVENT_ICC_STATUS_CHANGED = 1;
    protected static final int EVENT_INVALID_SIM_DETECTED = 114;
    protected static final int EVENT_QUERY_ICCID_DONE_FOR_HOT_SWAP = 108;
    protected static final int EVENT_QUERY_SIM_MISSING = 113;
    protected static final int EVENT_QUERY_SIM_MISSING_STATUS = 104;
    protected static final int EVENT_QUERY_SIM_STATUS_FOR_PLUG_IN = 112;
    protected static final int EVENT_RADIO_AVAILABLE = 100;
    private static final int EVENT_RADIO_UNAVAILABLE = 3;
    protected static final int EVENT_REPOLL_SML_STATE = 115;
    protected static final int EVENT_SIM_MISSING = 103;
    protected static final int EVENT_SIM_PLUG_IN = 110;
    protected static final int EVENT_SIM_PLUG_OUT = 109;
    protected static final int EVENT_SIM_RECOVERY = 105;
    private static final int EVENT_SIM_REFRESH = 4;
    protected static final int EVENT_VIRTUAL_SIM_OFF = 102;
    protected static final int EVENT_VIRTUAL_SIM_ON = 101;
    private static final String LOG_TAG = "UiccController";
    private static final int MAX_PROACTIVE_COMMANDS_TO_LOG = 20;
    private static final int SML_FEATURE_NEED_BROADCAST_INTENT = 1;
    private static final int SML_FEATURE_NO_NEED_BROADCAST_INTENT = 0;
    private static UiccController mInstance;
    private static final Object mLock = new Object();
    private static IUiccControllerExt mUiccControllerExt;
    private CommandsInterface[] mCis;
    private Context mContext;
    private UiccCard[] mUiccCards = new UiccCard[TelephonyManager.getDefault().getPhoneCount()];
    protected RegistrantList mIccChangedRegistrants = new RegistrantList();
    private RegistrantList mRecoveryRegistrants = new RegistrantList();
    private int[] mIsimSessionId = new int[TelephonyManager.getDefault().getPhoneCount()];
    private RegistrantList mApplicationChangedRegistrants = new RegistrantList();
    private int[] UICCCONTROLLER_STRING_NOTIFICATION_SIM_MISSING = {134545522, 134545530, 134545531, 134545532};
    private int[] UICCCONTROLLER_STRING_NOTIFICATION_VIRTUAL_SIM_ON = {134545515, 134545516, 134545517, 134545518};
    private LinkedList<String> mCardLogs = new LinkedList<>();
    private int mBtSlotId = -1;

    private static int[] m540x5ed1affa() {
        if (f34x16bf601e != null) {
            return f34x16bf601e;
        }
        int[] iArr = new int[IccCardApplicationStatus.PersoSubState.valuesCustom().length];
        try {
            iArr[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_IN_PROGRESS.ordinal()] = 6;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_READY.ordinal()] = 7;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_RUIM_CORPORATE.ordinal()] = 8;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_RUIM_CORPORATE_PUK.ordinal()] = 9;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_RUIM_HRPD.ordinal()] = 10;
        } catch (NoSuchFieldError e5) {
        }
        try {
            iArr[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_RUIM_HRPD_PUK.ordinal()] = 11;
        } catch (NoSuchFieldError e6) {
        }
        try {
            iArr[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_RUIM_NETWORK1.ordinal()] = 12;
        } catch (NoSuchFieldError e7) {
        }
        try {
            iArr[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_RUIM_NETWORK1_PUK.ordinal()] = 13;
        } catch (NoSuchFieldError e8) {
        }
        try {
            iArr[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_RUIM_NETWORK2.ordinal()] = 14;
        } catch (NoSuchFieldError e9) {
        }
        try {
            iArr[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_RUIM_NETWORK2_PUK.ordinal()] = 15;
        } catch (NoSuchFieldError e10) {
        }
        try {
            iArr[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_RUIM_RUIM.ordinal()] = 16;
        } catch (NoSuchFieldError e11) {
        }
        try {
            iArr[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_RUIM_RUIM_PUK.ordinal()] = 17;
        } catch (NoSuchFieldError e12) {
        }
        try {
            iArr[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_RUIM_SERVICE_PROVIDER.ordinal()] = 18;
        } catch (NoSuchFieldError e13) {
        }
        try {
            iArr[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_RUIM_SERVICE_PROVIDER_PUK.ordinal()] = 19;
        } catch (NoSuchFieldError e14) {
        }
        try {
            iArr[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_SIM_CORPORATE.ordinal()] = 1;
        } catch (NoSuchFieldError e15) {
        }
        try {
            iArr[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_SIM_CORPORATE_PUK.ordinal()] = 20;
        } catch (NoSuchFieldError e16) {
        }
        try {
            iArr[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_SIM_NETWORK.ordinal()] = 2;
        } catch (NoSuchFieldError e17) {
        }
        try {
            iArr[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_SIM_NETWORK_PUK.ordinal()] = 21;
        } catch (NoSuchFieldError e18) {
        }
        try {
            iArr[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_SIM_NETWORK_SUBSET.ordinal()] = 3;
        } catch (NoSuchFieldError e19) {
        }
        try {
            iArr[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_SIM_NETWORK_SUBSET_PUK.ordinal()] = 22;
        } catch (NoSuchFieldError e20) {
        }
        try {
            iArr[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_SIM_SERVICE_PROVIDER.ordinal()] = 4;
        } catch (NoSuchFieldError e21) {
        }
        try {
            iArr[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_SIM_SERVICE_PROVIDER_PUK.ordinal()] = 23;
        } catch (NoSuchFieldError e22) {
        }
        try {
            iArr[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_SIM_SIM.ordinal()] = 5;
        } catch (NoSuchFieldError e23) {
        }
        try {
            iArr[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_SIM_SIM_PUK.ordinal()] = 24;
        } catch (NoSuchFieldError e24) {
        }
        try {
            iArr[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_UNKNOWN.ordinal()] = 25;
        } catch (NoSuchFieldError e25) {
        }
        f34x16bf601e = iArr;
        return iArr;
    }

    public static UiccController make(Context c, CommandsInterface[] ci) {
        UiccController uiccController;
        synchronized (mLock) {
            if (mInstance != null) {
                throw new RuntimeException("MSimUiccController.make() should only be called once");
            }
            mInstance = new UiccController(c, ci);
            uiccController = mInstance;
        }
        return uiccController;
    }

    private UiccController(Context c, CommandsInterface[] ci) {
        log("Creating UiccController");
        this.mContext = c;
        this.mCis = ci;
        for (int i = 0; i < this.mCis.length; i++) {
            Integer index = new Integer(i);
            this.mCis[i].registerForIccStatusChanged(this, 1, index);
            if (SystemProperties.get("ro.crypto.state").equals("unencrypted") || SystemProperties.get("ro.crypto.state").equals("unsupported") || SystemProperties.get("ro.crypto.type").equals("file") || DECRYPT_STATE.equals(SystemProperties.get("vold.decrypt"))) {
                this.mCis[i].registerForAvailable(this, 1, index);
            } else {
                this.mCis[i].registerForOn(this, 1, index);
            }
            this.mCis[i].registerForNotAvailable(this, 3, index);
            this.mCis[i].registerForIccRefresh(this, 4, index);
            this.mCis[i].registerForVirtualSimOn(this, 101, index);
            this.mCis[i].registerForVirtualSimOff(this, 102, index);
            this.mCis[i].registerForSimMissing(this, EVENT_SIM_MISSING, index);
            this.mCis[i].registerForSimRecovery(this, 105, index);
            this.mCis[i].registerForSimPlugOut(this, 109, index);
            this.mCis[i].registerForSimPlugIn(this, 110, index);
            this.mCis[i].registerForCommonSlotNoChanged(this, EVENT_COMMON_SLOT_NO_CHANGED, index);
        }
        try {
            mUiccControllerExt = (IUiccControllerExt) MPlugin.createInstance(IUiccControllerExt.class.getName(), this.mContext);
        } catch (Exception e) {
            Rlog.e(LOG_TAG, "Fail to create plug-in");
            e.printStackTrace();
        }
    }

    public static UiccController getInstance() {
        UiccController uiccController;
        synchronized (mLock) {
            if (mInstance == null) {
                throw new RuntimeException("UiccController.getInstance can't be called before make()");
            }
            uiccController = mInstance;
        }
        return uiccController;
    }

    public UiccCard getUiccCard(int phoneId) {
        synchronized (mLock) {
            if (isValidCardIndex(phoneId)) {
                return this.mUiccCards[phoneId];
            }
            return null;
        }
    }

    public UiccCard[] getUiccCards() {
        UiccCard[] uiccCardArr;
        synchronized (mLock) {
            uiccCardArr = (UiccCard[]) this.mUiccCards.clone();
        }
        return uiccCardArr;
    }

    public UiccCardApplication getUiccCardApplication(int family) {
        return getUiccCardApplication(SubscriptionController.getInstance().getPhoneId(SubscriptionController.getInstance().getDefaultSubId()), family);
    }

    public IccRecords getIccRecords(int phoneId, int family) {
        synchronized (mLock) {
            UiccCardApplication app = getUiccCardApplication(phoneId, family);
            if (app == null) {
                return null;
            }
            return app.getIccRecords();
        }
    }

    public IccFileHandler getIccFileHandler(int phoneId, int family) {
        synchronized (mLock) {
            UiccCardApplication app = getUiccCardApplication(phoneId, family);
            if (app == null) {
                return null;
            }
            return app.getIccFileHandler();
        }
    }

    public int getIccApplicationChannel(int slotId, int family) {
        int index;
        synchronized (mLock) {
            index = 0;
            switch (family) {
                case 3:
                    index = this.mIsimSessionId[slotId];
                    if (index == 0) {
                        index = getUiccCardApplication(slotId, family) == null ? 0 : 1;
                    }
                    break;
                default:
                    log("unknown application");
                    break;
            }
        }
        return index;
    }

    public void registerForIccChanged(Handler h, int what, Object obj) {
        synchronized (mLock) {
            Registrant r = new Registrant(h, what, obj);
            this.mIccChangedRegistrants.add(r);
            r.notifyRegistrant();
        }
    }

    public void unregisterForIccChanged(Handler h) {
        synchronized (mLock) {
            this.mIccChangedRegistrants.remove(h);
        }
    }

    public void registerForIccRecovery(Handler h, int what, Object obj) {
        synchronized (mLock) {
            Registrant r = new Registrant(h, what, obj);
            this.mRecoveryRegistrants.add(r);
            r.notifyRegistrant();
        }
    }

    public void unregisterForIccRecovery(Handler h) {
        synchronized (mLock) {
            this.mRecoveryRegistrants.remove(h);
        }
    }

    @Override
    public void handleMessage(Message msg) {
        synchronized (mLock) {
            Integer index = getCiIndex(msg);
            if (index.intValue() < 0 || index.intValue() >= this.mCis.length) {
                Rlog.e(LOG_TAG, "Invalid index : " + index + " received with event " + msg.what);
                return;
            }
            AsyncResult ar = (AsyncResult) msg.obj;
            switch (msg.what) {
                case 1:
                    log("Received EVENT_ICC_STATUS_CHANGED, calling getIccCardStatus, index: " + index);
                    if (ignoreGetSimStatus()) {
                        log("FlightMode ON, Modem OFF: ignore get sim status");
                    } else {
                        this.mCis[index.intValue()].getIccCardStatus(obtainMessage(2, index));
                    }
                    return;
                case 2:
                    log("Received EVENT_GET_ICC_STATUS_DONE");
                    onGetIccCardStatusDone(ar, index);
                    return;
                case 3:
                    log("EVENT_RADIO_UNAVAILABLE, dispose card");
                    if (this.mUiccCards[index.intValue()] != null) {
                        this.mUiccCards[index.intValue()].dispose();
                    }
                    this.mUiccCards[index.intValue()] = null;
                    this.mIccChangedRegistrants.notifyRegistrants(new AsyncResult((Object) null, index, (Throwable) null));
                    return;
                case 4:
                    log("Received EVENT_SIM_REFRESH");
                    onSimRefresh(ar, index);
                case 101:
                    log("handleMessage (EVENT_VIRTUAL_SIM_ON)");
                    setNotificationVirtual(index.intValue(), 101);
                    SharedPreferences shOn = this.mContext.getSharedPreferences("AutoAnswer", 1);
                    SharedPreferences.Editor editorOn = shOn.edit();
                    editorOn.putBoolean("flag", true);
                    editorOn.commit();
                    return;
                case 102:
                    log("handleMessage (EVENT_VIRTUAL_SIM_OFF)");
                    removeNotificationVirtual(index.intValue(), 101);
                    SharedPreferences shOff = this.mContext.getSharedPreferences("AutoAnswer", 1);
                    SharedPreferences.Editor editorOff = shOff.edit();
                    editorOff.putBoolean("flag", false);
                    editorOff.commit();
                    return;
                case EVENT_SIM_MISSING:
                    log("handleMessage (EVENT_SIM_MISSING)");
                    this.mCis[index.intValue()].getIccCardStatus(obtainMessage(106, index));
                    return;
                case 105:
                    log("handleMessage (EVENT_SIM_RECOVERY)");
                    this.mCis[index.intValue()].getIccCardStatus(obtainMessage(EVENT_GET_ICC_STATUS_DONE_FOR_SIM_RECOVERY, index));
                    this.mRecoveryRegistrants.notifyRegistrants(new AsyncResult((Object) null, index, (Throwable) null));
                    Intent intent = new Intent();
                    intent.setAction("com.android.phone.ACTION_SIM_RECOVERY_DONE");
                    this.mContext.sendBroadcast(intent);
                    return;
                case 106:
                    log("Received EVENT_GET_ICC_STATUS_DONE_FOR_SIM_MISSING");
                    onGetIccCardStatusDone((AsyncResult) msg.obj, index, false);
                    return;
                case EVENT_GET_ICC_STATUS_DONE_FOR_SIM_RECOVERY:
                    log("Received EVENT_GET_ICC_STATUS_DONE_FOR_SIM_RECOVERY");
                    onGetIccCardStatusDone((AsyncResult) msg.obj, index, false);
                    return;
                case EVENT_REPOLL_SML_STATE:
                    log("Received EVENT_REPOLL_SML_STATE");
                    AsyncResult ar2 = (AsyncResult) msg.obj;
                    boolean needIntent = msg.arg1 == 1;
                    onGetIccCardStatusDone(ar2, index, false);
                    if (this.mUiccCards[index.intValue()] != null && needIntent) {
                        UiccCardApplication app = this.mUiccCards[index.intValue()].getApplication(1);
                        if (app == null) {
                            log("UiccCardApplication = null");
                        } else if (app.getState() == IccCardApplicationStatus.AppState.APPSTATE_SUBSCRIPTION_PERSO) {
                            Intent lockIntent = new Intent();
                            if (lockIntent == null) {
                                log("New intent failed");
                                return;
                            }
                            log("Broadcast ACTION_UNLOCK_SIM_LOCK");
                            lockIntent.setAction("mediatek.intent.action.ACTION_UNLOCK_SIM_LOCK");
                            lockIntent.putExtra("ss", "LOCKED");
                            lockIntent.putExtra("reason", parsePersoType(app.getPersoSubState()));
                            SubscriptionManager.putPhoneIdAndSubIdExtra(lockIntent, index.intValue());
                            this.mContext.sendBroadcast(lockIntent);
                        }
                    }
                    return;
                case EVENT_COMMON_SLOT_NO_CHANGED:
                    log("handleMessage (EVENT_COMMON_SLOT_NO_CHANGED)");
                    Intent intentNoChanged = new Intent("com.mediatek.phone.ACTION_COMMON_SLOT_NO_CHANGED");
                    int slotId = index.intValue();
                    SubscriptionManager.putPhoneIdAndSubIdExtra(intentNoChanged, slotId);
                    log("Broadcasting intent ACTION_COMMON_SLOT_NO_CHANGED for mSlotId : " + slotId);
                    this.mContext.sendBroadcast(intentNoChanged);
                    return;
                default:
                    Rlog.e(LOG_TAG, " Unknown Event " + msg.what);
                    return;
            }
        }
    }

    private Integer getCiIndex(Message msg) {
        Integer index = new Integer(0);
        if (msg != null) {
            if (msg.obj != null && (msg.obj instanceof Integer)) {
                return (Integer) msg.obj;
            }
            if (msg.obj != null && (msg.obj instanceof AsyncResult)) {
                AsyncResult ar = (AsyncResult) msg.obj;
                if (ar.userObj != null && (ar.userObj instanceof Integer)) {
                    return (Integer) ar.userObj;
                }
                return index;
            }
            return index;
        }
        return index;
    }

    public UiccCardApplication getUiccCardApplication(int phoneId, int family) {
        synchronized (mLock) {
            if (isValidCardIndex(phoneId)) {
                UiccCard c = this.mUiccCards[phoneId];
                if (c != null) {
                    return this.mUiccCards[phoneId].getApplication(family);
                }
            }
            return null;
        }
    }

    private synchronized void onGetIccCardStatusDone(AsyncResult ar, Integer index) {
        if (ar.exception != null) {
            Rlog.e(LOG_TAG, "Error getting ICC status. RIL_REQUEST_GET_ICC_STATUS should never return an error", ar.exception);
            return;
        }
        if (!isValidCardIndex(index.intValue())) {
            Rlog.e(LOG_TAG, "onGetIccCardStatusDone: invalid index : " + index);
            return;
        }
        log("onGetIccCardStatusDone, index " + index);
        IccCardStatus status = (IccCardStatus) ar.result;
        if (this.mUiccCards[index.intValue()] == null) {
            this.mUiccCards[index.intValue()] = new UiccCard(this.mContext, this.mCis[index.intValue()], status, index.intValue());
        } else {
            this.mUiccCards[index.intValue()].update(this.mContext, this.mCis[index.intValue()], status);
        }
        log("Notifying IccChangedRegistrants");
        this.mIccChangedRegistrants.notifyRegistrants(new AsyncResult((Object) null, index, (Throwable) null));
    }

    private void onSimRefresh(AsyncResult ar, Integer index) {
        if (ar.exception != null) {
            Rlog.e(LOG_TAG, "Sim REFRESH with exception: " + ar.exception);
            return;
        }
        if (!isValidCardIndex(index.intValue())) {
            Rlog.e(LOG_TAG, "onSimRefresh: invalid index : " + index);
            return;
        }
        IccRefreshResponse resp = (IccRefreshResponse) ar.result;
        Rlog.d(LOG_TAG, "onSimRefresh: " + resp);
        if (this.mUiccCards[index.intValue()] == null) {
            Rlog.e(LOG_TAG, "onSimRefresh: refresh on null card : " + index);
            return;
        }
        if (resp.refreshResult != 2 || resp.aid == null) {
            Rlog.d(LOG_TAG, "Ignoring reset: " + resp);
            return;
        }
        Rlog.d(LOG_TAG, "Handling refresh reset: " + resp);
        boolean changed = this.mUiccCards[index.intValue()].resetAppWithAid(resp.aid);
        if (!changed) {
            return;
        }
        this.mContext.getResources().getBoolean(R.^attr-private.layout_maxHeight);
        if (!SystemProperties.get("ro.sim_refresh_reset_by_modem").equals("1")) {
            this.mCis[index.intValue()].resetRadio(null);
        } else {
            this.mCis[index.intValue()].getIccCardStatus(obtainMessage(2));
        }
        this.mIccChangedRegistrants.notifyRegistrants(new AsyncResult((Object) null, index, (Throwable) null));
    }

    private boolean isValidCardIndex(int index) {
        return index >= 0 && index < this.mUiccCards.length;
    }

    private void log(String string) {
        Rlog.d(LOG_TAG, string);
    }

    public void addCardLog(String data) {
        Time t = new Time();
        t.setToNow();
        this.mCardLogs.addLast(t.format("%m-%d %H:%M:%S") + " " + data);
        if (this.mCardLogs.size() <= 20) {
            return;
        }
        this.mCardLogs.removeFirst();
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("UiccController: " + this);
        pw.println(" mContext=" + this.mContext);
        pw.println(" mInstance=" + mInstance);
        pw.println(" mIccChangedRegistrants: size=" + this.mIccChangedRegistrants.size());
        for (int i = 0; i < this.mIccChangedRegistrants.size(); i++) {
            pw.println("  mIccChangedRegistrants[" + i + "]=" + ((Registrant) this.mIccChangedRegistrants.get(i)).getHandler());
        }
        pw.println();
        pw.flush();
        pw.println(" mUiccCards: size=" + this.mUiccCards.length);
        for (int i2 = 0; i2 < this.mUiccCards.length; i2++) {
            if (this.mUiccCards[i2] == null) {
                pw.println("  mUiccCards[" + i2 + "]=null");
            } else {
                pw.println("  mUiccCards[" + i2 + "]=" + this.mUiccCards[i2]);
                this.mUiccCards[i2].dump(fd, pw, args);
            }
        }
        pw.println("mCardLogs: ");
        for (int i3 = 0; i3 < this.mCardLogs.size(); i3++) {
            pw.println("  " + this.mCardLogs.get(i3));
        }
    }

    private synchronized void onGetIccCardStatusDone(AsyncResult ar, Integer index, boolean isUpdate) {
        if (ar.exception != null) {
            Rlog.e(LOG_TAG, "Error getting ICC status. RIL_REQUEST_GET_ICC_STATUS should never return an error", ar.exception);
            return;
        }
        if (!isValidCardIndex(index.intValue())) {
            Rlog.e(LOG_TAG, "onGetIccCardStatusDone: invalid index : " + index);
            return;
        }
        log("onGetIccCardStatusDone, index " + index + "isUpdateSiminfo " + isUpdate);
        IccCardStatus status = (IccCardStatus) ar.result;
        if (this.mUiccCards[index.intValue()] == null) {
            this.mUiccCards[index.intValue()] = new UiccCard(this.mContext, this.mCis[index.intValue()], status, index.intValue(), isUpdate);
        } else {
            this.mUiccCards[index.intValue()].update(this.mContext, this.mCis[index.intValue()], status, isUpdate);
        }
        log("Notifying IccChangedRegistrants");
        if (!SystemProperties.get(COMMON_SLOT_PROPERTY).equals("1")) {
            this.mIccChangedRegistrants.notifyRegistrants(new AsyncResult((Object) null, index, (Throwable) null));
        } else {
            Bundle result = new Bundle();
            result.putInt("Index", index.intValue());
            result.putBoolean("ForceUpdate", isUpdate);
            this.mIccChangedRegistrants.notifyRegistrants(new AsyncResult((Object) null, result, (Throwable) null));
        }
    }

    private void setNotification(int slot, int notifyType) {
        log("setNotification(): notifyType = " + notifyType);
        Notification notification = new Notification();
        notification.when = System.currentTimeMillis();
        notification.flags = 16;
        notification.icon = R.drawable.stat_sys_warning;
        Intent intent = new Intent();
        notification.contentIntent = PendingIntent.getActivity(this.mContext, 0, intent, 134217728);
        String title = mUiccControllerExt.getMissingTitle(this.mContext, slot);
        CharSequence detail = mUiccControllerExt.getMissingDetail(this.mContext);
        notification.tickerText = title;
        notification.setLatestEventInfo(this.mContext, title, detail, notification.contentIntent);
        NotificationManager notificationManager = (NotificationManager) this.mContext.getSystemService("notification");
        notificationManager.notify(notifyType + slot, notification);
    }

    public void disableSimMissingNotification(int slot) {
        NotificationManager notificationManager = (NotificationManager) this.mContext.getSystemService("notification");
        notificationManager.cancel(slot + EVENT_SIM_MISSING);
    }

    private void setNotificationVirtual(int slot, int notifyType) {
        String title;
        log("setNotificationVirtual(): notifyType = " + notifyType);
        Notification notification = new Notification();
        notification.when = System.currentTimeMillis();
        notification.flags = 16;
        notification.icon = R.drawable.stat_sys_warning;
        Intent intent = new Intent();
        notification.contentIntent = PendingIntent.getActivity(this.mContext, 0, intent, 134217728);
        if (TelephonyManager.getDefault().getSimCount() > 1) {
            title = Resources.getSystem().getText(this.UICCCONTROLLER_STRING_NOTIFICATION_VIRTUAL_SIM_ON[slot]).toString();
        } else {
            title = Resources.getSystem().getText(134545519).toString();
        }
        CharSequence detail = this.mContext.getText(134545519).toString();
        notification.tickerText = this.mContext.getText(134545519).toString();
        notification.setLatestEventInfo(this.mContext, title, detail, notification.contentIntent);
        NotificationManager notificationManager = (NotificationManager) this.mContext.getSystemService("notification");
        notificationManager.notify(notifyType + slot, notification);
    }

    private void removeNotificationVirtual(int slot, int notifyType) {
        NotificationManager notificationManager = (NotificationManager) this.mContext.getSystemService("notification");
        notificationManager.cancel(notifyType + slot);
    }

    public int getBtConnectedSimId() {
        log("getBtConnectedSimId, slot " + this.mBtSlotId);
        return this.mBtSlotId;
    }

    public void setBtConnectedSimId(int simId) {
        this.mBtSlotId = simId;
        log("setBtConnectedSimId, slot " + this.mBtSlotId);
    }

    private String parsePersoType(IccCardApplicationStatus.PersoSubState state) {
        log("parsePersoType, state = " + state);
        switch (m540x5ed1affa()[state.ordinal()]) {
            case 1:
                return "CORPORATE";
            case 2:
                return "NETWORK";
            case 3:
                return "NETWORK_SUBSET";
            case 4:
                return "SERVICE_PROVIDER";
            case 5:
                return "SIM";
            default:
                return "UNKNOWN";
        }
    }

    public void repollIccStateForModemSmlChangeFeatrue(int slotId, boolean needIntent) {
        log("repollIccStateForModemSmlChangeFeatrue, needIntent = " + needIntent);
        int arg1 = needIntent ? 1 : 0;
        this.mCis[slotId].getIccCardStatus(obtainMessage(EVENT_REPOLL_SML_STATE, arg1, 0, Integer.valueOf(slotId)));
    }

    public void registerForApplicationChanged(Handler h, int what, Object obj) {
        synchronized (mLock) {
            Registrant r = new Registrant(h, what, obj);
            this.mApplicationChangedRegistrants.add(r);
            r.notifyRegistrant();
        }
    }

    public void unregisterForApplicationChanged(Handler h) {
        synchronized (mLock) {
            this.mApplicationChangedRegistrants.remove(h);
        }
    }

    private boolean ignoreGetSimStatus() {
        int airplaneMode = Settings.Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0);
        log("ignoreGetSimStatus(): airplaneMode - " + airplaneMode);
        if (!RadioManager.isFlightModePowerOffModemEnabled() || airplaneMode != 1) {
            return false;
        }
        log("ignoreGetSimStatus(): return true");
        return true;
    }

    public boolean isAllRadioAvailable() {
        boolean isRadioReady = true;
        for (int i = 0; i < TelephonyManager.getDefault().getPhoneCount(); i++) {
            if (CommandsInterface.RadioState.RADIO_UNAVAILABLE == this.mCis[i].getRadioState()) {
                isRadioReady = false;
            }
        }
        log("isAllRadioAvailable = " + isRadioReady);
        return isRadioReady;
    }

    public void resetRadioForVsim() {
        int allPhoneIdBitMask = (1 << TelephonyManager.getDefault().getPhoneCount()) - 1;
        log("resetRadioForVsim...false");
        RadioManager.getInstance().setModemPower(false, allPhoneIdBitMask);
        log("resetRadioForVsim...true");
        RadioManager.getInstance().setModemPower(true, allPhoneIdBitMask);
    }
}
