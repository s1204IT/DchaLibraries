package com.android.internal.telephony.uicc;

import android.R;
import android.content.Context;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.SystemProperties;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.CommandsInterface;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class UiccController extends Handler {
    public static final int APP_FAM_3GPP = 1;
    public static final int APP_FAM_3GPP2 = 2;
    public static final int APP_FAM_IMS = 3;
    private static final boolean DBG = true;
    private static final String DECRYPT_STATE = "trigger_restart_framework";
    private static final int EVENT_GET_ICC_STATUS_DONE = 2;
    private static final int EVENT_ICC_PLUG = 5;
    private static final int EVENT_ICC_PLUG_SIM_PLUGIN = 0;
    private static final int EVENT_ICC_PLUG_SIM_PLUGOUT = 1;
    private static final int EVENT_ICC_PLUG_SIM_TRAY_PLUGIN = 2;
    private static final int EVENT_ICC_PLUG_SIM_TRAY_PLUGOUT = 3;
    private static final int EVENT_ICC_STATUS_CHANGED = 1;
    private static final int EVENT_RADIO_UNAVAILABLE = 3;
    private static final int EVENT_SIM_REFRESH = 4;
    private static final String LOG_TAG = "UiccController";
    private static UiccController mInstance;
    private static final Object mLock = new Object();
    private CommandsInterface[] mCis;
    private Context mContext;
    private UiccCard[] mUiccCards = new UiccCard[TelephonyManager.getDefault().getPhoneCount()];
    protected RegistrantList mIccChangedRegistrants = new RegistrantList();

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
            if (DECRYPT_STATE.equals(SystemProperties.get("vold.decrypt"))) {
                this.mCis[i].registerForAvailable(this, 1, index);
            } else {
                this.mCis[i].registerForOn(this, 1, index);
            }
            this.mCis[i].registerForNotAvailable(this, 3, index);
            this.mCis[i].registerForIccRefresh(this, 4, index);
            this.mCis[i].registerForSimHotPlug(this, 5, index);
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
        UiccCard uiccCard;
        synchronized (mLock) {
            uiccCard = isValidCardIndex(phoneId) ? this.mUiccCards[phoneId] : null;
        }
        return uiccCard;
    }

    public UiccCard[] getUiccCards() {
        UiccCard[] uiccCardArr;
        synchronized (mLock) {
            uiccCardArr = (UiccCard[]) this.mUiccCards.clone();
        }
        return uiccCardArr;
    }

    public IccRecords getIccRecords(int phoneId, int family) {
        IccRecords iccRecords;
        synchronized (mLock) {
            UiccCardApplication app = getUiccCardApplication(phoneId, family);
            iccRecords = app != null ? app.getIccRecords() : null;
        }
        return iccRecords;
    }

    public IccFileHandler getIccFileHandler(int phoneId, int family) {
        IccFileHandler iccFileHandler;
        synchronized (mLock) {
            UiccCardApplication app = getUiccCardApplication(phoneId, family);
            iccFileHandler = app != null ? app.getIccFileHandler() : null;
        }
        return iccFileHandler;
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
                    log("Received EVENT_ICC_STATUS_CHANGED, calling getIccCardStatus");
                    this.mCis[index.intValue()].getIccCardStatus(obtainMessage(2, index));
                    break;
                case 2:
                    log("Received EVENT_GET_ICC_STATUS_DONE");
                    onGetIccCardStatusDone(ar, index);
                    break;
                case 3:
                    log("EVENT_RADIO_UNAVAILABLE, dispose card");
                    if (this.mUiccCards[index.intValue()] != null) {
                        this.mUiccCards[index.intValue()].dispose();
                    }
                    this.mUiccCards[index.intValue()] = null;
                    this.mIccChangedRegistrants.notifyRegistrants(new AsyncResult((Object) null, index, (Throwable) null));
                    break;
                case 4:
                    log("Received EVENT_SIM_REFRESH");
                    onSimRefresh(ar, index);
                    break;
                case 5:
                    log("Received EVENT_ICC_PLUG");
                    onIccPlugEvent((AsyncResult) msg.obj, index);
                    break;
                default:
                    Rlog.e(LOG_TAG, " Unknown Event " + msg.what);
                    break;
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
        UiccCardApplication application;
        synchronized (mLock) {
            if (isValidCardIndex(phoneId)) {
                UiccCard c = this.mUiccCards[phoneId];
                application = c != null ? this.mUiccCards[phoneId].getApplication(family) : null;
            }
        }
        return application;
    }

    private synchronized void onGetIccCardStatusDone(AsyncResult ar, Integer index) {
        if (ar.exception != null) {
            Rlog.e(LOG_TAG, "Error getting ICC status. RIL_REQUEST_GET_ICC_STATUS should never return an error", ar.exception);
        } else if (!isValidCardIndex(index.intValue())) {
            Rlog.e(LOG_TAG, "onGetIccCardStatusDone: invalid index : " + index);
        } else {
            IccCardStatus status = (IccCardStatus) ar.result;
            if (this.mUiccCards[index.intValue()] == null) {
                this.mUiccCards[index.intValue()] = new UiccCard(this.mContext, this.mCis[index.intValue()], status, index.intValue());
            } else {
                this.mUiccCards[index.intValue()].update(this.mContext, this.mCis[index.intValue()], status);
            }
            log("Notifying IccChangedRegistrants");
            this.mIccChangedRegistrants.notifyRegistrants(new AsyncResult((Object) null, index, (Throwable) null));
        }
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
        boolean changed = this.mUiccCards[index.intValue()].resetAppWithAid(resp.aid);
        if (changed) {
            boolean requirePowerOffOnSimRefreshReset = this.mContext.getResources().getBoolean(R.^attr-private.interpolatorX);
            if (requirePowerOffOnSimRefreshReset) {
                this.mCis[index.intValue()].setRadioPower(false, null);
            } else {
                this.mCis[index.intValue()].getIccCardStatus(obtainMessage(2));
            }
            this.mIccChangedRegistrants.notifyRegistrants(new AsyncResult((Object) null, index, (Throwable) null));
        }
    }

    private boolean isValidCardIndex(int index) {
        return index >= 0 && index < this.mUiccCards.length;
    }

    private synchronized void onIccPlugEvent(AsyncResult ar, Integer index) {
        synchronized (this) {
            int[] result = (int[]) ar.result;
            log("Receive Icc Plug Event " + result[0]);
            if (!isValidCardIndex(index.intValue())) {
                Rlog.e(LOG_TAG, "onIccPlugEvent: invalid index : " + index);
            } else if (result[0] == 3 || result[0] == 2) {
                boolean isAdded = false;
                if (result[0] == 2) {
                    isAdded = true;
                }
                if (this.mUiccCards[index.intValue()] != null) {
                    this.mUiccCards[index.intValue()].iccCardPlug(isAdded, true);
                }
                int otherIndex = index.intValue() != 0 ? 0 : 1;
                if (getUiccCard(otherIndex) != null) {
                    getUiccCard(otherIndex).iccCardPlug(isAdded, true);
                }
            }
        }
    }

    private void log(String string) {
        Rlog.d(LOG_TAG, string);
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
    }
}
