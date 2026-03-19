package com.mediatek.internal.telephony.uicc;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.SystemProperties;
import android.telephony.Rlog;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.MccTable;
import com.android.internal.telephony.uicc.IccCardApplicationStatus;
import com.android.internal.telephony.uicc.IccCardStatus;
import com.android.internal.telephony.uicc.InstallCarrierAppTrampolineActivity;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.uicc.UiccProfile;

public class MtkUiccProfile extends UiccProfile {
    protected static final int EVENT_BASE_ID = 100;
    private static final int EVENT_GET_ATR_DONE = 102;
    private static final int EVENT_ICC_FDN_CHANGED = 104;
    private static final int EVENT_OPEN_CHANNEL_WITH_SW_DONE = 103;
    private static final int EVENT_SIM_IO_EX_DONE = 101;
    private static final String ICCID_STRING_FOR_NO_SIM = "N/A";
    private String[] PROPERTY_ICCID_SIM;
    private RegistrantList mFdnChangedRegistrants;
    public final Handler mMtkHandler;
    private IccCardApplicationStatus.PersoSubState mNetworkLockState;
    static final String[] UICCCARD_PROPERTY_RIL_UICC_TYPE = {"vendor.gsm.ril.uicctype", "vendor.gsm.ril.uicctype.2", "vendor.gsm.ril.uicctype.3", "vendor.gsm.ril.uicctype.4"};
    private static final String[] PROPERTY_RIL_FULL_UICC_TYPE = {"vendor.gsm.ril.fulluicctype", "vendor.gsm.ril.fulluicctype.2", "vendor.gsm.ril.fulluicctype.3", "vendor.gsm.ril.fulluicctype.4"};

    public MtkUiccProfile(Context context, CommandsInterface commandsInterface, IccCardStatus iccCardStatus, int i, UiccCard uiccCard, Object obj) {
        super(context, commandsInterface, iccCardStatus, i, uiccCard, obj);
        this.mFdnChangedRegistrants = new RegistrantList();
        this.mNetworkLockState = IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_UNKNOWN;
        this.PROPERTY_ICCID_SIM = new String[]{"vendor.ril.iccid.sim1", "vendor.ril.iccid.sim2", "vendor.ril.iccid.sim3", "vendor.ril.iccid.sim4"};
        this.mMtkHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (MtkUiccProfile.this.mDisposed) {
                    MtkUiccProfile.this.loge("handleMessage: Received " + message.what + " after dispose(); ignoring the message");
                }
                MtkUiccProfile.this.log("mHandlerEx Received message " + message + "[" + message.what + "]");
                switch (message.what) {
                    case 101:
                    case 102:
                    case MtkUiccProfile.EVENT_OPEN_CHANNEL_WITH_SW_DONE:
                        AsyncResult asyncResult = (AsyncResult) message.obj;
                        if (asyncResult.exception != null) {
                            MtkUiccProfile.this.loge("Error in SIM access with exception" + asyncResult.exception);
                        }
                        AsyncResult.forMessage((Message) asyncResult.userObj, asyncResult.result, asyncResult.exception);
                        ((Message) asyncResult.userObj).sendToTarget();
                        break;
                    case 104:
                        MtkUiccProfile.this.mFdnChangedRegistrants.notifyRegistrants();
                        break;
                    default:
                        MtkUiccProfile.this.mHandler.handleMessage(message);
                        break;
                }
            }
        };
        log("MtkUiccProfile Creating");
    }

    protected UiccCardApplication makeUiccApplication(UiccProfile uiccProfile, IccCardApplicationStatus iccCardApplicationStatus, Context context, CommandsInterface commandsInterface) {
        return new MtkUiccCardApplication(uiccProfile, iccCardApplicationStatus, context, commandsInterface);
    }

    protected boolean isSupportAllNetworkLockCategory() {
        return true;
    }

    protected void registerCurrAppEvents() {
        super.registerCurrAppEvents();
        if (this.mUiccApplication != null && (this.mUiccApplication instanceof MtkUiccCardApplication)) {
            ((MtkUiccCardApplication) this.mUiccApplication).registerForFdnChanged(this.mMtkHandler, 104, null);
        }
    }

    protected void unregisterCurrAppEvents() {
        super.unregisterCurrAppEvents();
        if (this.mUiccApplication != null && (this.mUiccApplication instanceof MtkUiccCardApplication)) {
            ((MtkUiccCardApplication) this.mUiccApplication).unregisterForFdnChanged(this.mMtkHandler);
        }
    }

    protected void setCurrentAppType(boolean z) {
        boolean z2;
        log("setCurrentAppType");
        synchronized (this.mLock) {
            if (TelephonyManager.getLteOnCdmaModeStatic() != 1) {
                z2 = false;
            } else {
                z2 = true;
            }
            if (z || (z2 && MtkIccUtilsEx.checkCdma3gCard(getPhoneId()) == 0)) {
                this.mCurrentAppType = 1;
            } else {
                this.mCurrentAppType = 2;
            }
        }
    }

    protected void setExternalState(IccCardConstants.State state, boolean z) {
        synchronized (this.mLock) {
            if (!SubscriptionManager.isValidSlotIndex(getPhoneId())) {
                loge("setExternalState: mPhoneId=" + getPhoneId() + " is invalid; Return!!");
                return;
            }
            log("setExternalState(): mExternalState = " + this.mExternalState + " newState =  " + state + " override = " + z);
            if (!z && state == this.mExternalState) {
                if (state == IccCardConstants.State.NETWORK_LOCKED && this.mNetworkLockState != getNetworkPersoType()) {
                    this.mNetworkLockState = getNetworkPersoType();
                    log("NetworkLockState =  " + this.mNetworkLockState);
                } else {
                    log("setExternalState: !override and newstate unchanged from " + state);
                    return;
                }
            }
            this.mExternalState = state;
            if (this.mExternalState == IccCardConstants.State.LOADED && this.mIccRecords != null) {
                String operatorNumeric = this.mIccRecords.getOperatorNumeric();
                log("operator=" + operatorNumeric + " mPhoneId=" + getPhoneId());
                if (!TextUtils.isEmpty(operatorNumeric)) {
                    this.mTelephonyManager.setSimOperatorNumericForPhone(getPhoneId(), operatorNumeric);
                    String strSubstring = operatorNumeric.substring(0, 3);
                    if (strSubstring != null) {
                        this.mTelephonyManager.setSimCountryIsoForPhone(getPhoneId(), MccTable.countryCodeForMcc(Integer.parseInt(strSubstring)));
                    } else {
                        loge("EVENT_RECORDS_LOADED Country code is null");
                    }
                } else {
                    loge("EVENT_RECORDS_LOADED Operator name is null");
                }
            }
            log("setExternalState: set mPhoneId=" + getPhoneId() + " mExternalState=" + this.mExternalState);
            this.mTelephonyManager.setSimStateForPhone(getPhoneId(), getState().toString());
            UiccController.updateInternalIccState(getIccStateIntentString(this.mExternalState), getIccStateReason(this.mExternalState), getPhoneId());
        }
    }

    protected void setExternalState(IccCardConstants.State state) {
        if (state == IccCardConstants.State.PIN_REQUIRED && this.mUiccApplication != null && this.mUiccApplication.getPin1State() == IccCardStatus.PinState.PINSTATE_ENABLED_PERM_BLOCKED) {
            log("setExternalState(): PERM_DISABLED");
            setExternalState(IccCardConstants.State.PERM_DISABLED);
        } else {
            super.setExternalState(state);
        }
    }

    protected String getIccStateReason(IccCardConstants.State state) {
        log("getIccStateReason E");
        if (IccCardConstants.State.NETWORK_LOCKED == state && this.mUiccApplication != null) {
            switch (AnonymousClass2.$SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$PersoSubState[this.mUiccApplication.getPersoSubState().ordinal()]) {
                case 1:
                    return "NETWORK";
                case 2:
                    return "NETWORK_SUBSET";
                case 3:
                    return "CORPORATE";
                case 4:
                    return "SERVICE_PROVIDER";
                case 5:
                    return "SIM";
                default:
                    return null;
            }
        }
        return super.getIccStateReason(state);
    }

    static class AnonymousClass2 {
        static final int[] $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$PersoSubState = new int[IccCardApplicationStatus.PersoSubState.values().length];

        static {
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$PersoSubState[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_SIM_NETWORK.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$PersoSubState[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_SIM_NETWORK_SUBSET.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$PersoSubState[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_SIM_CORPORATE.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$PersoSubState[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_SIM_SERVICE_PROVIDER.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$PersoSubState[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_SIM_SIM.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
        }
    }

    public boolean hasIccCard() {
        String str = SystemProperties.get(this.PROPERTY_ICCID_SIM[getPhoneId()]);
        boolean z = true;
        boolean z2 = (str == null || str.equals("") || str.equals("N/A")) ? false : true;
        if (z2 || this.mUiccCard == null || this.mUiccCard.getCardState() == IccCardStatus.CardState.CARDSTATE_ABSENT) {
            z = z2;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("hasIccCard(): isSimInsert =  ");
        sb.append(z);
        sb.append(" ,CardState = ");
        sb.append(this.mUiccCard != null ? this.mUiccCard.getCardState() : "");
        sb.append(", iccId = ");
        sb.append(SubscriptionInfo.givePrintableIccid(str));
        log(sb.toString());
        return z;
    }

    protected String getSubscriptionDisplayName(int i, Context context) {
        String simOperatorNumeric = this.mTelephonyManager.getSimOperatorNumeric(i);
        String strLookupOperatorNameForDisplayName = MtkSpnOverride.getInstance().lookupOperatorNameForDisplayName(i, simOperatorNumeric, true, context);
        String simOperatorName = this.mTelephonyManager.getSimOperatorName(i);
        log("getSubscriptionDisplayName- simNumeric: " + simOperatorNumeric + ", simMvnoName: " + strLookupOperatorNameForDisplayName + ", simCarrierName: " + simOperatorName);
        return !TextUtils.isEmpty(strLookupOperatorNameForDisplayName) ? strLookupOperatorNameForDisplayName : simOperatorName;
    }

    protected void promptInstallCarrierApp(String str) {
        Intent intent = InstallCarrierAppTrampolineActivity.get(this.mContext, str);
        intent.addFlags(268435456);
        this.mContext.startActivity(intent);
    }

    public void queryIccNetworkLock(int i, Message message) {
        log("queryIccNetworkLock(): category =  " + i);
        synchronized (this.mLock) {
            if (this.mUiccApplication != null) {
                ((MtkUiccCardApplication) this.mUiccApplication).queryIccNetworkLock(i, message);
            } else if (message != null) {
                CommandException commandExceptionFromRilErrno = CommandException.fromRilErrno(1);
                log("Fail to queryIccNetworkLock, hasIccCard = " + hasIccCard());
                AsyncResult.forMessage(message).exception = commandExceptionFromRilErrno;
                message.sendToTarget();
            }
        }
    }

    public void setIccNetworkLockEnabled(int i, int i2, String str, String str2, String str3, String str4, Message message) {
        log("SetIccNetworkEnabled(): category = " + i + " lockop = " + i2 + " password = " + str + " data_imsi = " + str2 + " gid1 = " + str3 + " gid2 = " + str4);
        synchronized (this.mLock) {
            if (this.mUiccApplication != null) {
                ((MtkUiccCardApplication) this.mUiccApplication).setIccNetworkLockEnabled(i, i2, str, str2, str3, str4, message);
            } else if (message != null) {
                CommandException commandExceptionFromRilErrno = CommandException.fromRilErrno(1);
                log("Fail to setIccNetworkLockEnabled, hasIccCard = " + hasIccCard());
                AsyncResult.forMessage(message).exception = commandExceptionFromRilErrno;
                message.sendToTarget();
            }
        }
    }

    public void registerForFdnChanged(Handler handler, int i, Object obj) {
        synchronized (this.mLock) {
            synchronized (this.mLock) {
                Registrant registrant = new Registrant(handler, i, obj);
                this.mFdnChangedRegistrants.add(registrant);
                if (getIccFdnEnabled()) {
                    registrant.notifyRegistrant();
                }
            }
        }
    }

    public void unregisterForFdnChanged(Handler handler) {
        synchronized (this.mLock) {
            this.mFdnChangedRegistrants.remove(handler);
        }
    }

    public IccCardApplicationStatus.PersoSubState getNetworkPersoType() {
        log("getNetworkPersoType E");
        synchronized (this.mLock) {
            if (this.mUiccApplication != null) {
                return this.mUiccApplication.getPersoSubState();
            }
            return IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_UNKNOWN;
        }
    }

    public void repollIccStateForModemSmlChangeFeatrue(boolean z) {
        log("repollIccStateForModemSmlChangeFeatrue, needIntent = " + z);
        synchronized (this.mLock) {
            MtkUiccController mtkUiccController = (MtkUiccController) UiccController.getInstance();
            if (mtkUiccController != null) {
                mtkUiccController.repollIccStateForModemSmlChangeFeatrue(getPhoneId(), z);
            }
        }
    }

    protected Exception covertException(String str) {
        log("Fail to " + str + ", hasIccCard = " + hasIccCard());
        return CommandException.fromRilErrno(1);
    }

    public boolean getIccFdnAvailable() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mUiccApplication != null && this.mUiccApplication.getIccFdnAvailable();
        }
        return z;
    }

    public void iccExchangeSimIOEx(int i, int i2, int i3, int i4, int i5, String str, String str2, String str3, Message message) {
        this.mCi.iccIO(i2, i, str, i3, i4, i5, str2, str3, this.mMtkHandler.obtainMessage(101, message));
    }

    public void iccGetAtr(Message message) {
        this.mCi.getATR(this.mMtkHandler.obtainMessage(102, message));
    }

    public String getIccCardType() {
        return SystemProperties.get(UICCCARD_PROPERTY_RIL_UICC_TYPE[getPhoneId()]);
    }

    public String[] getFullIccCardType() {
        return SystemProperties.get(PROPERTY_RIL_FULL_UICC_TYPE[getPhoneId()]).split(",");
    }

    protected void log(String str) {
        Rlog.d("UiccProfile", str + " (phoneId " + getPhoneId() + ")");
    }

    protected void loge(String str) {
        Rlog.e("UiccProfile", str + " (phoneId " + getPhoneId() + ")");
    }
}
