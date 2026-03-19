package com.android.internal.telephony.uicc;

import android.R;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.storage.StorageManager;
import android.telephony.CarrierConfigManager;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import android.util.LocalLog;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.RadioConfig;
import com.android.internal.telephony.SubscriptionInfoUpdater;
import com.android.internal.telephony.TelephonyComponentFactory;
import com.android.internal.telephony.uicc.IccSlotStatus;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

public class UiccController extends Handler {
    public static final int APP_FAM_3GPP = 1;
    public static final int APP_FAM_3GPP2 = 2;
    public static final int APP_FAM_IMS = 3;
    protected static final boolean DBG = true;
    protected static final int EVENT_GET_ICC_STATUS_DONE = 3;
    private static final int EVENT_GET_SLOT_STATUS_DONE = 4;
    protected static final int EVENT_ICC_STATUS_CHANGED = 1;
    protected static final int EVENT_RADIO_AVAILABLE = 6;
    protected static final int EVENT_RADIO_ON = 5;
    private static final int EVENT_RADIO_UNAVAILABLE = 7;
    private static final int EVENT_SIM_REFRESH = 8;
    private static final int EVENT_SLOT_STATUS_CHANGED = 2;
    public static final int INVALID_SLOT_ID = -1;
    private static final String LOG_TAG = "UiccController";
    protected static final boolean VDBG = false;
    private static UiccController mInstance;
    private static ArrayList<IccSlotStatus> sLastSlotStatus;
    protected CommandsInterface[] mCis;
    protected Context mContext;
    private UiccStateChangedLauncher mLauncher;
    private int[] mPhoneIdToSlotId;
    private RadioConfig mRadioConfig;
    private UiccSlot[] mUiccSlots;
    protected static final Object mLock = new Object();
    static LocalLog sLocalLog = new LocalLog(100);
    private boolean mIsSlotStatusSupported = true;
    protected RegistrantList mIccChangedRegistrants = new RegistrantList();

    public static UiccController make(Context context, CommandsInterface[] commandsInterfaceArr) {
        UiccController uiccController;
        synchronized (mLock) {
            if (mInstance != null) {
                throw new RuntimeException("UiccController.make() should only be called once");
            }
            mInstance = TelephonyComponentFactory.getInstance().makeUiccController(context, commandsInterfaceArr);
            uiccController = mInstance;
        }
        return uiccController;
    }

    public UiccController(Context context, CommandsInterface[] commandsInterfaceArr) {
        log("Creating UiccController");
        this.mContext = context;
        this.mCis = commandsInterfaceArr;
        String str = "config_num_physical_slots = " + context.getResources().getInteger(R.integer.config_displayWhiteBalanceIncreaseDebounce);
        log(str);
        sLocalLog.log(str);
        int integer = context.getResources().getInteger(R.integer.config_displayWhiteBalanceIncreaseDebounce);
        this.mUiccSlots = new UiccSlot[integer < this.mCis.length ? this.mCis.length : integer];
        this.mPhoneIdToSlotId = new int[commandsInterfaceArr.length];
        Arrays.fill(this.mPhoneIdToSlotId, -1);
        this.mRadioConfig = RadioConfig.getInstance(this.mContext);
        this.mRadioConfig.registerForSimSlotStatusChanged(this, 2, null);
        for (int i = 0; i < this.mCis.length; i++) {
            this.mCis[i].registerForIccStatusChanged(this, 1, Integer.valueOf(i));
            if (!StorageManager.inCryptKeeperBounce()) {
                this.mCis[i].registerForAvailable(this, 6, Integer.valueOf(i));
            } else {
                this.mCis[i].registerForOn(this, 5, Integer.valueOf(i));
            }
            this.mCis[i].registerForNotAvailable(this, 7, Integer.valueOf(i));
            this.mCis[i].registerForIccRefresh(this, 8, Integer.valueOf(i));
        }
        this.mLauncher = new UiccStateChangedLauncher(context, this);
    }

    private int getSlotIdFromPhoneId(int i) {
        return this.mPhoneIdToSlotId[i];
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

    public UiccCard getUiccCard(int i) {
        UiccCard uiccCardForPhone;
        synchronized (mLock) {
            uiccCardForPhone = getUiccCardForPhone(i);
        }
        return uiccCardForPhone;
    }

    public UiccCard getUiccCardForSlot(int i) {
        synchronized (mLock) {
            UiccSlot uiccSlot = getUiccSlot(i);
            if (uiccSlot != null) {
                return uiccSlot.getUiccCard();
            }
            return null;
        }
    }

    public UiccCard getUiccCardForPhone(int i) {
        UiccSlot uiccSlotForPhone;
        synchronized (mLock) {
            if (isValidPhoneIndex(i) && (uiccSlotForPhone = getUiccSlotForPhone(i)) != null) {
                return uiccSlotForPhone.getUiccCard();
            }
            return null;
        }
    }

    public UiccProfile getUiccProfileForPhone(int i) {
        synchronized (mLock) {
            if (!isValidPhoneIndex(i)) {
                return null;
            }
            UiccCard uiccCardForPhone = getUiccCardForPhone(i);
            return uiccCardForPhone != null ? uiccCardForPhone.getUiccProfile() : null;
        }
    }

    public UiccSlot[] getUiccSlots() {
        UiccSlot[] uiccSlotArr;
        synchronized (mLock) {
            uiccSlotArr = this.mUiccSlots;
        }
        return uiccSlotArr;
    }

    public void switchSlots(int[] iArr, Message message) {
        this.mRadioConfig.setSimSlotsMapping(iArr, message);
    }

    public UiccSlot getUiccSlot(int i) {
        synchronized (mLock) {
            if (isValidSlotIndex(i)) {
                return this.mUiccSlots[i];
            }
            return null;
        }
    }

    public UiccSlot getUiccSlotForPhone(int i) {
        synchronized (mLock) {
            if (isValidPhoneIndex(i)) {
                int slotIdFromPhoneId = getSlotIdFromPhoneId(i);
                if (isValidSlotIndex(slotIdFromPhoneId)) {
                    return this.mUiccSlots[slotIdFromPhoneId];
                }
            }
            return null;
        }
    }

    public int getUiccSlotForCardId(String str) {
        UiccCard uiccCard;
        synchronized (mLock) {
            for (int i = 0; i < this.mUiccSlots.length; i++) {
                if (this.mUiccSlots[i] != null && (uiccCard = this.mUiccSlots[i].getUiccCard()) != null && str.equals(uiccCard.getCardId())) {
                    return i;
                }
            }
            for (int i2 = 0; i2 < this.mUiccSlots.length; i2++) {
                if (this.mUiccSlots[i2] != null && str.equals(this.mUiccSlots[i2].getIccId())) {
                    return i2;
                }
            }
            return -1;
        }
    }

    public IccRecords getIccRecords(int i, int i2) {
        synchronized (mLock) {
            UiccCardApplication uiccCardApplication = getUiccCardApplication(i, i2);
            if (uiccCardApplication != null) {
                return uiccCardApplication.getIccRecords();
            }
            return null;
        }
    }

    public IccFileHandler getIccFileHandler(int i, int i2) {
        synchronized (mLock) {
            UiccCardApplication uiccCardApplication = getUiccCardApplication(i, i2);
            if (uiccCardApplication != null) {
                return uiccCardApplication.getIccFileHandler();
            }
            return null;
        }
    }

    public void registerForIccChanged(Handler handler, int i, Object obj) {
        synchronized (mLock) {
            Registrant registrant = new Registrant(handler, i, obj);
            this.mIccChangedRegistrants.add(registrant);
            registrant.notifyRegistrant();
        }
    }

    public void unregisterForIccChanged(Handler handler) {
        synchronized (mLock) {
            this.mIccChangedRegistrants.remove(handler);
        }
    }

    @Override
    public void handleMessage(Message message) {
        synchronized (mLock) {
            Integer ciIndex = getCiIndex(message);
            if (ciIndex.intValue() >= 0 && ciIndex.intValue() < this.mCis.length) {
                sLocalLog.log("handleMessage: Received " + message.what + " for phoneId " + ciIndex);
                AsyncResult asyncResult = (AsyncResult) message.obj;
                switch (message.what) {
                    case 1:
                        log("Received EVENT_ICC_STATUS_CHANGED, calling getIccCardStatus");
                        this.mCis[ciIndex.intValue()].getIccCardStatus(obtainMessage(3, ciIndex));
                        break;
                    case 2:
                    case 4:
                        log("Received EVENT_SLOT_STATUS_CHANGED or EVENT_GET_SLOT_STATUS_DONE");
                        onGetSlotStatusDone(asyncResult);
                        break;
                    case 3:
                        log("Received EVENT_GET_ICC_STATUS_DONE");
                        onGetIccCardStatusDone(asyncResult, ciIndex);
                        break;
                    case 5:
                    case 6:
                        log("Received EVENT_RADIO_AVAILABLE/EVENT_RADIO_ON, calling getIccCardStatus");
                        this.mCis[ciIndex.intValue()].getIccCardStatus(obtainMessage(3, ciIndex));
                        if (ciIndex.intValue() == 0) {
                            log("Received EVENT_RADIO_AVAILABLE/EVENT_RADIO_ON for phoneId 0, calling getIccSlotsStatus");
                            this.mRadioConfig.getSimSlotsStatus(obtainMessage(4, ciIndex));
                        }
                        break;
                    case 7:
                        log("EVENT_RADIO_UNAVAILABLE, dispose card");
                        UiccSlot uiccSlotForPhone = getUiccSlotForPhone(ciIndex.intValue());
                        if (uiccSlotForPhone != null) {
                            uiccSlotForPhone.onRadioStateUnavailable();
                        }
                        this.mIccChangedRegistrants.notifyRegistrants(new AsyncResult((Object) null, ciIndex, (Throwable) null));
                        break;
                    case 8:
                        log("Received EVENT_SIM_REFRESH");
                        onSimRefresh(asyncResult, ciIndex);
                        break;
                    default:
                        Rlog.e(LOG_TAG, " Unknown Event " + message.what);
                        break;
                }
                return;
            }
            Rlog.e(LOG_TAG, "Invalid phoneId : " + ciIndex + " received with event " + message.what);
        }
    }

    protected Integer getCiIndex(Message message) {
        Integer num = new Integer(0);
        if (message != null) {
            if (message.obj != null && (message.obj instanceof Integer)) {
                return (Integer) message.obj;
            }
            if (message.obj != null && (message.obj instanceof AsyncResult)) {
                AsyncResult asyncResult = (AsyncResult) message.obj;
                if (asyncResult.userObj != null && (asyncResult.userObj instanceof Integer)) {
                    return (Integer) asyncResult.userObj;
                }
                return num;
            }
            return num;
        }
        return num;
    }

    public UiccCardApplication getUiccCardApplication(int i, int i2) {
        synchronized (mLock) {
            UiccCard uiccCardForPhone = getUiccCardForPhone(i);
            if (uiccCardForPhone != null) {
                return uiccCardForPhone.getApplication(i2);
            }
            return null;
        }
    }

    public static void updateInternalIccState(String str, String str2, int i) {
        SubscriptionInfoUpdater subscriptionInfoUpdater = PhoneFactory.getSubscriptionInfoUpdater();
        if (subscriptionInfoUpdater != null) {
            subscriptionInfoUpdater.updateInternalIccState(str, str2, i);
        } else {
            Rlog.e(LOG_TAG, "subInfoUpdate is null.");
        }
    }

    protected synchronized void onGetIccCardStatusDone(AsyncResult asyncResult, Integer num) {
        if (asyncResult.exception != null) {
            Rlog.e(LOG_TAG, "Error getting ICC status. RIL_REQUEST_GET_ICC_STATUS should never return an error", asyncResult.exception);
            return;
        }
        if (!isValidPhoneIndex(num.intValue())) {
            Rlog.e(LOG_TAG, "onGetIccCardStatusDone: invalid index : " + num);
            return;
        }
        IccCardStatus iccCardStatus = (IccCardStatus) asyncResult.result;
        sLocalLog.log("onGetIccCardStatusDone: phoneId " + num + " IccCardStatus: " + iccCardStatus);
        int iIntValue = iccCardStatus.physicalSlotIndex;
        if (iIntValue == -1) {
            iIntValue = num.intValue();
        }
        this.mPhoneIdToSlotId[num.intValue()] = iIntValue;
        if (this.mUiccSlots[iIntValue] == null) {
            this.mUiccSlots[iIntValue] = new UiccSlot(this.mContext, true);
        }
        this.mUiccSlots[iIntValue].update(this.mCis[num.intValue()], iccCardStatus, num.intValue());
        log("Notifying IccChangedRegistrants");
        this.mIccChangedRegistrants.notifyRegistrants(new AsyncResult((Object) null, num, (Throwable) null));
    }

    private synchronized void onGetSlotStatusDone(AsyncResult asyncResult) {
        if (this.mIsSlotStatusSupported) {
            Throwable th = asyncResult.exception;
            if (th != null) {
                if (!(th instanceof CommandException) || ((CommandException) th).getCommandError() != CommandException.Error.REQUEST_NOT_SUPPORTED) {
                    String str = "Unexpected error getting slot status: " + asyncResult.exception;
                    Rlog.e(LOG_TAG, str);
                    sLocalLog.log(str);
                } else {
                    log("onGetSlotStatusDone: request not supported; marking mIsSlotStatusSupported to false");
                    sLocalLog.log("onGetSlotStatusDone: request not supported; marking mIsSlotStatusSupported to false");
                    this.mIsSlotStatusSupported = false;
                }
                return;
            }
            ArrayList<IccSlotStatus> arrayList = (ArrayList) asyncResult.result;
            if (!slotStatusChanged(arrayList)) {
                log("onGetSlotStatusDone: No change in slot status");
                return;
            }
            sLastSlotStatus = arrayList;
            int i = 0;
            for (int i2 = 0; i2 < arrayList.size(); i2++) {
                IccSlotStatus iccSlotStatus = arrayList.get(i2);
                boolean z = iccSlotStatus.slotState == IccSlotStatus.SlotState.SLOTSTATE_ACTIVE;
                if (z) {
                    i++;
                    if (!isValidPhoneIndex(iccSlotStatus.logicalSlotIndex)) {
                        throw new RuntimeException("Logical slot index " + iccSlotStatus.logicalSlotIndex + " invalid for physical slot " + i2);
                    }
                    this.mPhoneIdToSlotId[iccSlotStatus.logicalSlotIndex] = i2;
                }
                if (this.mUiccSlots[i2] == null) {
                    this.mUiccSlots[i2] = new UiccSlot(this.mContext, z);
                }
                this.mUiccSlots[i2].update(z ? this.mCis[iccSlotStatus.logicalSlotIndex] : null, iccSlotStatus);
            }
            if (i != this.mPhoneIdToSlotId.length) {
                throw new RuntimeException("Number of active slots " + i + " does not match the expected value " + this.mPhoneIdToSlotId.length);
            }
            HashSet hashSet = new HashSet();
            for (int i3 : this.mPhoneIdToSlotId) {
                if (hashSet.contains(Integer.valueOf(i3))) {
                    throw new RuntimeException("slotId " + i3 + " mapped to multiple phoneIds");
                }
                hashSet.add(Integer.valueOf(i3));
            }
            Intent intent = new Intent("android.telephony.action.SIM_SLOT_STATUS_CHANGED");
            intent.addFlags(67108864);
            intent.addFlags(16777216);
            this.mContext.sendBroadcast(intent, "android.permission.READ_PRIVILEGED_PHONE_STATE");
        }
    }

    private boolean slotStatusChanged(ArrayList<IccSlotStatus> arrayList) {
        if (sLastSlotStatus == null || sLastSlotStatus.size() != arrayList.size()) {
            return true;
        }
        Iterator<IccSlotStatus> it = arrayList.iterator();
        while (it.hasNext()) {
            if (!sLastSlotStatus.contains(it.next())) {
                return true;
            }
        }
        return false;
    }

    private void logPhoneIdToSlotIdMapping() {
        log("mPhoneIdToSlotId mapping:");
        for (int i = 0; i < this.mPhoneIdToSlotId.length; i++) {
            log("    phoneId " + i + " slotId " + this.mPhoneIdToSlotId[i]);
        }
    }

    private void onSimRefresh(AsyncResult asyncResult, Integer num) {
        if (asyncResult.exception != null) {
            Rlog.e(LOG_TAG, "onSimRefresh: Sim REFRESH with exception: " + asyncResult.exception);
        }
        if (!isValidPhoneIndex(num.intValue())) {
            Rlog.e(LOG_TAG, "onSimRefresh: invalid index : " + num);
            return;
        }
        IccRefreshResponse iccRefreshResponse = (IccRefreshResponse) asyncResult.result;
        log("onSimRefresh: " + iccRefreshResponse);
        sLocalLog.log("onSimRefresh: " + iccRefreshResponse);
        if (iccRefreshResponse == null) {
            Rlog.e(LOG_TAG, "onSimRefresh: received without input");
            return;
        }
        UiccCard uiccCardForPhone = getUiccCardForPhone(num.intValue());
        if (uiccCardForPhone == null) {
            Rlog.e(LOG_TAG, "onSimRefresh: refresh on null card : " + num);
            return;
        }
        switch (iccRefreshResponse.refreshResult) {
            case 1:
            case 2:
                if (uiccCardForPhone.resetAppWithAid(iccRefreshResponse.aid) && iccRefreshResponse.refreshResult == 2) {
                    ((CarrierConfigManager) this.mContext.getSystemService("carrier_config")).updateConfigForPhoneId(num.intValue(), "UNKNOWN");
                    if (this.mContext.getResources().getBoolean(R.^attr-private.magnifierZoom)) {
                        this.mCis[num.intValue()].setRadioPower(false, null);
                    }
                }
                this.mCis[num.intValue()].getIccCardStatus(obtainMessage(3, num));
                break;
        }
    }

    private boolean isValidPhoneIndex(int i) {
        return i >= 0 && i < TelephonyManager.getDefault().getPhoneCount();
    }

    protected boolean isValidSlotIndex(int i) {
        return i >= 0 && i < this.mUiccSlots.length;
    }

    private void log(String str) {
        Rlog.d(LOG_TAG, str);
    }

    public void addCardLog(String str) {
        sLocalLog.log(str);
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("UiccController: " + this);
        printWriter.println(" mContext=" + this.mContext);
        printWriter.println(" mInstance=" + mInstance);
        printWriter.println(" mIccChangedRegistrants: size=" + this.mIccChangedRegistrants.size());
        for (int i = 0; i < this.mIccChangedRegistrants.size(); i++) {
            printWriter.println("  mIccChangedRegistrants[" + i + "]=" + ((Registrant) this.mIccChangedRegistrants.get(i)).getHandler());
        }
        printWriter.println();
        printWriter.flush();
        printWriter.println(" mUiccSlots: size=" + this.mUiccSlots.length);
        for (int i2 = 0; i2 < this.mUiccSlots.length; i2++) {
            if (this.mUiccSlots[i2] == null) {
                printWriter.println("  mUiccSlots[" + i2 + "]=null");
            } else {
                printWriter.println("  mUiccSlots[" + i2 + "]=" + this.mUiccSlots[i2]);
                this.mUiccSlots[i2].dump(fileDescriptor, printWriter, strArr);
            }
        }
        printWriter.println(" sLocalLog= ");
        sLocalLog.dump(fileDescriptor, printWriter, strArr);
    }
}
