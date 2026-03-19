package com.mediatek.internal.telephony.imsphone;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.radio.V1_0.LastCallFailCause;
import android.net.LinkProperties;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.PowerManager;
import android.os.ResultReceiver;
import android.os.SystemProperties;
import android.os.WorkSource;
import android.telephony.CarrierConfigManager;
import android.telephony.CellLocation;
import android.telephony.NetworkScanRequest;
import android.telephony.PhoneNumberUtils;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.ims.ImsCallForwardInfo;
import android.telephony.ims.ImsSsInfo;
import com.android.ims.ImsException;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.CallForwardInfo;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.IccPhoneBookInterfaceManager;
import com.android.internal.telephony.OperatorInfo;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneInternalInterface;
import com.android.internal.telephony.PhoneNotifier;
import com.android.internal.telephony.TelephonyComponentFactory;
import com.android.internal.telephony.imsphone.ImsPhone;
import com.android.internal.telephony.imsphone.ImsPhoneMmiCode;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.UiccController;
import com.mediatek.ims.MtkImsCallForwardInfo;
import com.mediatek.ims.internal.MtkImsManager;
import com.mediatek.internal.telephony.MtkCallForwardInfo;
import com.mediatek.internal.telephony.MtkGsmCdmaPhone;
import com.mediatek.internal.telephony.MtkSuppSrvRequest;
import com.mediatek.internal.telephony.OpTelephonyCustomizationUtils;
import com.mediatek.telephony.MtkTelephonyManagerEx;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import mediatek.telephony.MtkServiceState;

public class MtkImsPhone extends ImsPhone {
    private static final String CFU_TIME_SLOT = "persist.vendor.radio.cfu.timeslot.";
    public static final int EVENT_GET_CALL_FORWARD_TIME_SLOT_DONE = 109;
    public static final int EVENT_SET_CALL_FORWARD_TIME_SLOT_DONE = 110;
    private static final String LOG_TAG = "MtkImsPhone";
    public static final String USSD_DURING_IMS_INCALL = "ussd_during_ims_incall";
    public static final String UT_BUNDLE_KEY_CLIR = "queryClir";
    private String mDialString;
    private boolean mIsBlindAssuredEctSupported;
    private boolean mIsConsultativeEctSupported;
    private boolean mIsDeviceSwitchSupported;
    private boolean mIsDigitsSupported;
    protected BroadcastReceiver mReceiver;
    public boolean mUssiCSFB;

    public enum FeatureType {
        VOLTE_ENHANCED_CONFERENCE,
        VOLTE_CONF_REMOVE_MEMBER,
        VIDEO_RESTRICTION,
        VOLTE_ECT,
        CONSULTATIVE_ECT
    }

    public void activateCellBroadcastSms(int i, Message message) {
        super.activateCellBroadcastSms(i, message);
    }

    public boolean disableDataConnectivity() {
        return super.disableDataConnectivity();
    }

    public void disableLocationUpdates() {
        super.disableLocationUpdates();
    }

    public boolean enableDataConnectivity() {
        return super.enableDataConnectivity();
    }

    public void enableLocationUpdates() {
        super.enableLocationUpdates();
    }

    public List getAllCellInfo(WorkSource workSource) {
        return super.getAllCellInfo(workSource);
    }

    public void getAvailableNetworks(Message message) {
        super.getAvailableNetworks(message);
    }

    public boolean getCallForwardingIndicator() {
        return super.getCallForwardingIndicator();
    }

    public void getCellBroadcastSmsConfig(Message message) {
        super.getCellBroadcastSmsConfig(message);
    }

    public CellLocation getCellLocation(WorkSource workSource) {
        return super.getCellLocation(workSource);
    }

    public List getCurrentDataConnectionList() {
        return super.getCurrentDataConnectionList();
    }

    public PhoneInternalInterface.DataActivityState getDataActivityState() {
        return super.getDataActivityState();
    }

    public PhoneConstants.DataState getDataConnectionState() {
        return super.getDataConnectionState();
    }

    public PhoneConstants.DataState getDataConnectionState(String str) {
        return super.getDataConnectionState(str);
    }

    public boolean getDataRoamingEnabled() {
        return super.getDataRoamingEnabled();
    }

    public String getDeviceId() {
        return super.getDeviceId();
    }

    public String getDeviceSvn() {
        return super.getDeviceSvn();
    }

    public String getEsn() {
        return super.getEsn();
    }

    public String getGroupIdLevel1() {
        return super.getGroupIdLevel1();
    }

    public String getGroupIdLevel2() {
        return super.getGroupIdLevel2();
    }

    public IccCard getIccCard() {
        return super.getIccCard();
    }

    public IccFileHandler getIccFileHandler() {
        return super.getIccFileHandler();
    }

    public IccPhoneBookInterfaceManager getIccPhoneBookInterfaceManager() {
        return super.getIccPhoneBookInterfaceManager();
    }

    public boolean getIccRecordsLoaded() {
        return super.getIccRecordsLoaded();
    }

    public String getIccSerialNumber() {
        return super.getIccSerialNumber();
    }

    public String getImei() {
        return super.getImei();
    }

    public String getLine1AlphaTag() {
        return super.getLine1AlphaTag();
    }

    public String getLine1Number() {
        return super.getLine1Number();
    }

    public LinkProperties getLinkProperties(String str) {
        return super.getLinkProperties(str);
    }

    public String getMeid() {
        return super.getMeid();
    }

    public boolean getMessageWaitingIndicator() {
        return super.getMessageWaitingIndicator();
    }

    public int getPhoneType() {
        return super.getPhoneType();
    }

    public SignalStrength getSignalStrength() {
        return super.getSignalStrength();
    }

    public String getSubscriberId() {
        return super.getSubscriberId();
    }

    public String getVoiceMailAlphaTag() {
        return super.getVoiceMailAlphaTag();
    }

    public String getVoiceMailNumber() {
        return super.getVoiceMailNumber();
    }

    public boolean handlePinMmi(String str) {
        return super.handlePinMmi(str);
    }

    public boolean isDataAllowed() {
        return super.isDataAllowed();
    }

    public boolean isDataEnabled() {
        return super.isDataEnabled();
    }

    public boolean isUserDataEnabled() {
        return super.isUserDataEnabled();
    }

    public void migrateFrom(Phone phone) {
        super.migrateFrom(phone);
    }

    public boolean needsOtaServiceProvisioning() {
        return super.needsOtaServiceProvisioning();
    }

    public void notifyCallForwardingIndicator() {
        super.notifyCallForwardingIndicator();
    }

    public void notifyDisconnect(Connection connection) {
        super.notifyDisconnect(connection);
    }

    public void notifyPhoneStateChanged() {
        super.notifyPhoneStateChanged();
    }

    public void notifyPreciseCallStateChanged() {
        super.notifyPreciseCallStateChanged();
    }

    public void notifySuppServiceFailed(PhoneInternalInterface.SuppService suppService) {
        super.notifySuppServiceFailed(suppService);
    }

    public void onTtyModeReceived(int i) {
        super.onTtyModeReceived(i);
    }

    public void registerForOnHoldTone(Handler handler, int i, Object obj) {
        super.registerForOnHoldTone(handler, i, obj);
    }

    public void registerForRingbackTone(Handler handler, int i, Object obj) {
        super.registerForRingbackTone(handler, i, obj);
    }

    public void registerForTtyModeReceived(Handler handler, int i, Object obj) {
        super.registerForTtyModeReceived(handler, i, obj);
    }

    public void selectNetworkManually(OperatorInfo operatorInfo, boolean z, Message message) {
        super.selectNetworkManually(operatorInfo, z, message);
    }

    public void setCellBroadcastSmsConfig(int[] iArr, Message message) {
        super.setCellBroadcastSmsConfig(iArr, message);
    }

    public void setDataRoamingEnabled(boolean z) {
        super.setDataRoamingEnabled(z);
    }

    public boolean setLine1Number(String str, String str2, Message message) {
        return super.setLine1Number(str, str2, message);
    }

    public void setNetworkSelectionModeAutomatic(Message message) {
        super.setNetworkSelectionModeAutomatic(message);
    }

    public void setRadioPower(boolean z) {
        super.setRadioPower(z);
    }

    public void setUserDataEnabled(boolean z) {
        super.setUserDataEnabled(z);
    }

    public void setVoiceMailNumber(String str, String str2, Message message) {
        super.setVoiceMailNumber(str, str2, message);
    }

    public void startNetworkScan(NetworkScanRequest networkScanRequest, Message message) {
        super.startNetworkScan(networkScanRequest, message);
    }

    @VisibleForTesting
    public void startOnHoldTone(Connection connection) {
        super.startOnHoldTone(connection);
    }

    public void startRingbackTone() {
        super.startRingbackTone();
    }

    public void stopNetworkScan(Message message) {
        super.stopNetworkScan(message);
    }

    public void stopOnHoldTone(Connection connection) {
        super.stopOnHoldTone(connection);
    }

    public void stopRingbackTone() {
        super.stopRingbackTone();
    }

    public void unregisterForOnHoldTone(Handler handler) {
        super.unregisterForOnHoldTone(handler);
    }

    public void unregisterForRingbackTone(Handler handler) {
        super.unregisterForRingbackTone(handler);
    }

    public void unregisterForTtyModeReceived(Handler handler) {
        super.unregisterForTtyModeReceived(handler);
    }

    public void updateServiceLocation() {
        super.updateServiceLocation();
    }

    public MtkImsPhone(Context context, PhoneNotifier phoneNotifier, Phone phone) {
        this(context, phoneNotifier, phone, false);
    }

    @VisibleForTesting
    public MtkImsPhone(Context context, PhoneNotifier phoneNotifier, Phone phone, boolean z) {
        super(LOG_TAG, context, phoneNotifier, z);
        this.mUssiCSFB = false;
        this.mIsDigitsSupported = MtkTelephonyManagerEx.getDefault().isDigitsSupported();
        this.mSS = new MtkServiceState();
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if (intent.getAction().equals("android.telephony.action.CARRIER_CONFIG_CHANGED") && intent.getIntExtra("subscription", -1) == MtkImsPhone.this.getSubId()) {
                    MtkImsPhone.this.logd("Receive carrierConfig changed: " + MtkImsPhone.this.mPhoneId);
                    MtkImsPhone.this.cacheCarrierConfiguration();
                }
            }
        };
        this.mDefaultPhone = phone;
        this.mSS.setStateOff();
        this.mExternalCallTracker = TelephonyComponentFactory.getInstance().makeImsExternalCallTracker(this);
        this.mCT = TelephonyComponentFactory.getInstance().makeImsPhoneCallTracker(this);
        this.mCT.registerPhoneStateListener(this.mExternalCallTracker);
        this.mExternalCallTracker.setCallPuller(this.mCT);
        this.mPhoneId = this.mDefaultPhone.getPhoneId();
        this.mWakeLock = ((PowerManager) context.getSystemService("power")).newWakeLock(1, LOG_TAG);
        this.mWakeLock.setReferenceCounted(false);
        if (this.mDefaultPhone.getServiceStateTracker() != null) {
            this.mDefaultPhone.getServiceStateTracker().registerForDataRegStateOrRatChanged(this, 52, (Object) null);
        }
        setServiceState(1);
        this.mDefaultPhone.registerForServiceStateChanged(this, 53, (Object) null);
        cacheCarrierConfiguration();
        registerForListenCarrierConfigChanged();
    }

    public void dispose() {
        super.dispose();
        if (this.mContext != null) {
            this.mContext.unregisterReceiver(this.mReceiver);
        }
    }

    protected Connection dialInternal(String str, PhoneInternalInterface.DialArgs dialArgs, ResultReceiver resultReceiver) throws CallStateException {
        String strStripSeparators;
        ImsPhone.ImsDialArgs.Builder builderFrom;
        String strExtractNetworkPortionAlt;
        if (!PhoneNumberUtils.isUriNumber(str)) {
            strStripSeparators = PhoneNumberUtils.stripSeparators(str);
        } else {
            strStripSeparators = str;
        }
        if (handleInCallMmiCommands(strStripSeparators)) {
            return null;
        }
        if (!(dialArgs instanceof ImsPhone.ImsDialArgs)) {
            builderFrom = ImsPhone.ImsDialArgs.Builder.from(dialArgs);
        } else {
            builderFrom = ImsPhone.ImsDialArgs.Builder.from((ImsPhone.ImsDialArgs) dialArgs);
        }
        builderFrom.setClirMode(this.mCT.getClirMode());
        if (this.mDefaultPhone.getPhoneType() == 2) {
            return this.mCT.dial(str, builderFrom.build());
        }
        if (!PhoneNumberUtils.isUriNumber(str)) {
            strExtractNetworkPortionAlt = PhoneNumberUtils.extractNetworkPortionAlt(strStripSeparators);
        } else {
            strExtractNetworkPortionAlt = str;
        }
        MtkImsPhoneMmiCode mtkImsPhoneMmiCodeNewFromDialString = MtkImsPhoneMmiCode.newFromDialString(strExtractNetworkPortionAlt, this);
        boolean zIsEmergencyNumber = PhoneNumberUtils.isEmergencyNumber(getSubId(), str);
        logd("dialInternal: dialing w/ mmi [" + mtkImsPhoneMmiCodeNewFromDialString + "] isEcc: " + zIsEmergencyNumber);
        if (isUssdDuringInCall(mtkImsPhoneMmiCodeNewFromDialString)) {
            logd("USSI during in-call");
        }
        this.mDialString = str;
        if (mtkImsPhoneMmiCodeNewFromDialString == null || zIsEmergencyNumber) {
            return this.mCT.dial(str, builderFrom.build());
        }
        if (mtkImsPhoneMmiCodeNewFromDialString.isTemporaryModeCLIR()) {
            builderFrom.setClirMode(mtkImsPhoneMmiCodeNewFromDialString.getCLIRMode());
            return this.mCT.dial(mtkImsPhoneMmiCodeNewFromDialString.getDialingNumber(), builderFrom.build());
        }
        if (!mtkImsPhoneMmiCodeNewFromDialString.isSupportedOverImsPhone()) {
            logi("dialInternal: USSD not supported by IMS; fallback to CS.");
            this.mDefaultPhone.setCsFallbackStatus(1);
            throw new CallStateException("cs_fallback");
        }
        if (this.mUssiCSFB) {
            logd("USSI CSFB");
            this.mUssiCSFB = false;
            throw new CallStateException("cs_fallback");
        }
        this.mPendingMMIs.add(mtkImsPhoneMmiCodeNewFromDialString);
        this.mMmiRegistrants.notifyRegistrants(new AsyncResult((Object) null, mtkImsPhoneMmiCodeNewFromDialString, (Throwable) null));
        try {
            OpTelephonyCustomizationUtils.getOpFactory(getContext()).makeDigitsUssdManager().setUssdExtra(dialArgs.intentExtras);
            mtkImsPhoneMmiCodeNewFromDialString.processCode();
        } catch (CallStateException e) {
            if ("cs_fallback".equals(e.getMessage())) {
                logi("dialInternal: fallback to GSM required.");
                this.mPendingMMIs.remove(mtkImsPhoneMmiCodeNewFromDialString);
                throw e;
            }
        }
        return null;
    }

    public void explicitCallTransfer(String str, int i) {
        ((MtkImsPhoneCallTracker) this.mCT).unattendedCallTransfer(str, i);
    }

    public void deviceSwitch(String str, String str2) {
        ((MtkImsPhoneCallTracker) this.mCT).deviceSwitch(str, str2);
    }

    public void cancelDeviceSwitch() {
        ((MtkImsPhoneCallTracker) this.mCT).cancelDeviceSwitch();
    }

    private boolean isUssdDuringInCall(MtkImsPhoneMmiCode mtkImsPhoneMmiCode) {
        if (mtkImsPhoneMmiCode == null || !mtkImsPhoneMmiCode.isUssdNumber()) {
            return false;
        }
        return isInCall();
    }

    public void setImsRegistered(boolean z) {
        this.mImsRegistered = z;
        if (this.mImsRegistered) {
            ((NotificationManager) this.mContext.getSystemService("notification")).cancel("wifi_calling", 1);
        }
    }

    protected void onIncomingUSSD(int i, String str) {
        logd("onIncomingUSSD ussdMode=" + i);
        int i2 = 0;
        boolean z = i == 1;
        boolean z2 = (i == 0 || i == 1) ? false : true;
        MtkImsPhoneMmiCode mtkImsPhoneMmiCode = null;
        int size = this.mPendingMMIs.size();
        while (true) {
            if (i2 >= size) {
                break;
            }
            if (!((MtkImsPhoneMmiCode) this.mPendingMMIs.get(i2)).isPendingUSSD()) {
                i2++;
            } else {
                mtkImsPhoneMmiCode = (MtkImsPhoneMmiCode) this.mPendingMMIs.get(i2);
                break;
            }
        }
        if (mtkImsPhoneMmiCode != null) {
            if (z2) {
                this.mUssiCSFB = true;
                mtkImsPhoneMmiCode.onUssdFinishedError();
                return;
            } else {
                mtkImsPhoneMmiCode.onUssdFinished(str, z);
                return;
            }
        }
        if (!z2 && str != null) {
            onNetworkInitiatedUssd(MtkImsPhoneMmiCode.newNetworkInitiatedUssd(str, z, this));
        }
    }

    public Connection dial(List<String> list, int i) throws CallStateException {
        return ((MtkImsPhoneCallTracker) this.mCT).dial(list, i);
    }

    public void hangupAll() throws CallStateException {
        logd("hangupAll");
        ((MtkImsPhoneCallTracker) this.mCT).hangupAll();
    }

    public void onMMIDone(ImsPhoneMmiCode imsPhoneMmiCode) {
        StringBuilder sb = new StringBuilder();
        sb.append("onMMIDone: ");
        sb.append(imsPhoneMmiCode);
        sb.append(", mUssiCSFB=");
        sb.append(this.mUssiCSFB);
        sb.append(", isInCall=");
        sb.append(isInCall() ? "true" : "false");
        logd(sb.toString());
        dumpPendingMmi();
        if (this.mUssiCSFB && !isInCall()) {
            this.mDefaultPhone.sendMessage(this.mDefaultPhone.obtainMessage(MtkGsmCdmaPhone.EVENT_USSI_CSFB, ((MtkImsPhoneMmiCode) imsPhoneMmiCode).getUssdDialString()));
            this.mPendingMMIs.remove(imsPhoneMmiCode);
        } else {
            this.mUssiCSFB = false;
            super.onMMIDone(imsPhoneMmiCode);
        }
    }

    public void removeMmi(ImsPhoneMmiCode imsPhoneMmiCode) {
        logd("removeMmi: " + imsPhoneMmiCode);
        dumpPendingMmi();
        this.mPendingMMIs.remove(imsPhoneMmiCode);
    }

    public void dumpPendingMmi() {
        int size = this.mPendingMMIs.size();
        if (size == 0) {
            logd("dumpPendingMmi: none");
            return;
        }
        for (int i = 0; i < size; i++) {
            logd("dumpPendingMmi: " + this.mPendingMMIs.get(i));
        }
    }

    public void handleMmiCodeCsfb(int i, MtkImsPhoneMmiCode mtkImsPhoneMmiCode) {
        logd("handleMmiCodeCsfb: reason = " + i + ", mDialString = " + this.mDialString + ", mmi=" + mtkImsPhoneMmiCode);
        removeMmi(mtkImsPhoneMmiCode);
        if (i == 61446) {
            this.mDefaultPhone.setCsFallbackStatus(2);
        } else if (i == 61447) {
            this.mDefaultPhone.setCsFallbackStatus(1);
        }
        MtkSuppSrvRequest mtkSuppSrvRequestObtain = MtkSuppSrvRequest.obtain(15, null);
        mtkSuppSrvRequestObtain.mParcel.writeString(this.mDialString);
        this.mDefaultPhone.sendMessage(this.mDefaultPhone.obtainMessage(2001, mtkSuppSrvRequestObtain));
    }

    protected boolean isValidCommandInterfaceCFReason(int i) {
        switch (i) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
                return true;
            default:
                return false;
        }
    }

    protected int getConditionFromCFReason(int i) {
        switch (i) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
                return 4;
            case 5:
                return 5;
            case 6:
                return 6;
            default:
                return -1;
        }
    }

    protected int getCFReasonFromCondition(int i) {
        switch (i) {
        }
        return 3;
    }

    private static class Cf {
        public final boolean mIsCfu;
        public final Message mOnComplete;
        public final int mServiceClass;
        public final String mSetCfNumber;

        public Cf(String str, boolean z, Message message, int i) {
            this.mSetCfNumber = str;
            this.mIsCfu = z;
            this.mOnComplete = message;
            this.mServiceClass = i;
        }
    }

    public void getCallForwardingOption(int i, Message message) {
        logd("getCallForwardingOption reason=" + i);
        if (!isValidCommandInterfaceCFReason(i)) {
            if (message != null) {
                sendErrorResponse(message);
                return;
            }
            return;
        }
        logd("requesting call forwarding query.");
        if (i == 0) {
            TelephonyManager.setTelephonyProperty(this.mDefaultPhone.getPhoneId(), "persist.vendor.radio.cfu.mode", "disabled_cfu_mode");
        }
        try {
            this.mCT.getUtInterface().queryCallForward(getConditionFromCFReason(i), (String) null, obtainMessage(13, message));
        } catch (ImsException e) {
            sendErrorResponse(message, e);
        }
    }

    public void setCallForwardingOption(int i, int i2, String str, int i3, int i4, Message message) {
        String cFPreviousDialNumber;
        logd("setCallForwardingOption action=" + i + ", reason=" + i2 + " serviceClass=" + i3);
        if (!isValidCommandInterfaceCFAction(i) || !isValidCommandInterfaceCFReason(i2)) {
            if (message != null) {
                sendErrorResponse(message);
                return;
            }
            return;
        }
        boolean z = true;
        if ((str != null && !str.isEmpty()) || this.mDefaultPhone == null || this.mDefaultPhone.getPhoneType() != 1 || !(this.mDefaultPhone instanceof MtkGsmCdmaPhone) || !this.mDefaultPhone.isSupportSaveCFNumber() || !isCfEnable(i) || (cFPreviousDialNumber = this.mDefaultPhone.getCFPreviousDialNumber(i2)) == null || cFPreviousDialNumber.isEmpty()) {
            cFPreviousDialNumber = str;
        }
        if (i2 != 0) {
            z = false;
        }
        try {
            this.mCT.getUtInterface().updateCallForward(getActionFromCFAction(i), getConditionFromCFReason(i2), cFPreviousDialNumber, i3, i4, obtainMessage(12, isCfEnable(i) ? 1 : 0, 0, new Cf(cFPreviousDialNumber, z, message, i3)));
        } catch (ImsException e) {
            sendErrorResponse(message, e);
        }
    }

    public void setCallBarring(String str, boolean z, String str2, Message message, int i) {
        int i2;
        logd("setCallBarring facility=" + str + ", lockState=" + z + ", serviceClass = " + i);
        Message messageObtainMessage = obtainMessage(46, message);
        if (z) {
            i2 = 1;
        } else {
            i2 = 0;
        }
        try {
            this.mCT.getUtInterface().updateCallBarring(str2, getCBTypeFromFacility(str), i2, messageObtainMessage, (String[]) null, i);
        } catch (ImsException e) {
            sendErrorResponse(message, e);
        }
    }

    protected void updateRoamingState(boolean z) {
        if (this.mCT.getState() == PhoneConstants.State.IDLE) {
            logd("updateRoamingState now: " + z + ", isWfcEnabled:" + isWifiCallingEnabled());
            if (isWifiCallingEnabled()) {
                logd("ignore due to IWLAN is treated as home");
                return;
            } else {
                this.mRoaming = z;
                MtkImsManager.setWfcMode(this.mContext, MtkImsManager.getWfcMode(this.mContext, z, this.mPhoneId), z, this.mPhoneId);
                return;
            }
        }
        logd("updateRoamingState postponed: " + z);
        this.mCT.registerForVoiceCallEnded(this, 54, (Object) null);
    }

    private static class CfEx {
        final boolean mIsCfu;
        final Message mOnComplete;
        final String mSetCfNumber;
        final long[] mSetTimeSlot;

        CfEx(String str, long[] jArr, boolean z, Message message) {
            this.mSetCfNumber = str;
            this.mSetTimeSlot = jArr;
            this.mIsCfu = z;
            this.mOnComplete = message;
        }
    }

    public void saveTimeSlot(long[] jArr) {
        String str = CFU_TIME_SLOT + this.mPhoneId;
        String str2 = "";
        if (jArr != null && jArr.length == 2) {
            str2 = Long.toString(jArr[0]) + "," + Long.toString(jArr[1]);
        }
        SystemProperties.set(str, str2);
        logd("timeSlotString = " + str2);
    }

    public long[] getTimeSlot() {
        long[] jArr;
        String str = SystemProperties.get(CFU_TIME_SLOT + this.mPhoneId, "");
        if (str != null && !str.equals("")) {
            String[] strArrSplit = str.split(",");
            if (strArrSplit.length == 2) {
                jArr = new long[2];
                for (int i = 0; i < 2; i++) {
                    jArr[i] = Long.parseLong(strArrSplit[i]);
                    Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
                    calendar.setTimeInMillis(jArr[i]);
                    int i2 = calendar.get(11);
                    int i3 = calendar.get(12);
                    Calendar calendar2 = Calendar.getInstance(TimeZone.getDefault());
                    calendar2.set(11, i2);
                    calendar2.set(12, i3);
                    jArr[i] = calendar2.getTimeInMillis();
                }
            }
        } else {
            jArr = null;
        }
        logd("timeSlot = " + Arrays.toString(jArr));
        return jArr;
    }

    public void getCallForwardInTimeSlot(int i, Message message) {
        logd("getCallForwardInTimeSlot reason = " + i);
        if (i != 0) {
            if (message != null) {
                sendErrorResponse(message);
                return;
            }
            return;
        }
        logd("requesting call forwarding in a time slot query.");
        TelephonyManager.setTelephonyProperty(this.mDefaultPhone.getPhoneId(), "persist.vendor.radio.cfu.mode", "disabled_cfu_mode");
        try {
            this.mCT.getUtInterface().queryCallForwardInTimeSlot(getConditionFromCFReason(i), obtainMessage(109, message));
        } catch (ImsException e) {
            sendErrorResponse(message, e);
        }
    }

    public void setCallForwardInTimeSlot(int i, int i2, String str, int i3, long[] jArr, Message message) {
        logd("setCallForwardInTimeSlot action = " + i + ", reason = " + i2);
        if (!isValidCommandInterfaceCFAction(i) || i2 != 0) {
            if (message != null) {
                sendErrorResponse(message);
            }
        } else {
            try {
                this.mCT.getUtInterface().updateCallForwardInTimeSlot(getActionFromCFAction(i), getConditionFromCFReason(i2), str, i3, jArr, obtainMessage(110, i, 0, new CfEx(str, jArr, true, message)));
            } catch (ImsException e) {
                sendErrorResponse(message, e);
            }
        }
    }

    private MtkCallForwardInfo[] handleCfInTimeSlotQueryResult(MtkImsCallForwardInfo[] mtkImsCallForwardInfoArr) {
        MtkCallForwardInfo[] mtkCallForwardInfoArr;
        if (supportMdAutoSetupIms()) {
            if (mtkImsCallForwardInfoArr != null && mtkImsCallForwardInfoArr.length != 0) {
                mtkCallForwardInfoArr = new MtkCallForwardInfo[mtkImsCallForwardInfoArr.length];
            } else {
                mtkCallForwardInfoArr = null;
            }
        } else if (mtkImsCallForwardInfoArr != null) {
            mtkCallForwardInfoArr = new MtkCallForwardInfo[mtkImsCallForwardInfoArr.length];
        }
        IccRecords iccRecords = this.mDefaultPhone.getIccRecords();
        if (mtkImsCallForwardInfoArr == null || mtkImsCallForwardInfoArr.length == 0) {
            if (iccRecords != null) {
                setVoiceCallForwardingFlag(iccRecords, 1, false, null);
            }
        } else {
            int length = mtkImsCallForwardInfoArr.length;
            for (int i = 0; i < length; i++) {
                if (mtkImsCallForwardInfoArr[i].mCondition == 0 && (mtkImsCallForwardInfoArr[i].mServiceClass & 1) != 0 && iccRecords != null) {
                    setVoiceCallForwardingFlag(iccRecords, 1, mtkImsCallForwardInfoArr[i].mStatus == 1, mtkImsCallForwardInfoArr[i].mNumber);
                    saveTimeSlot(mtkImsCallForwardInfoArr[i].mTimeSlot);
                }
                mtkCallForwardInfoArr[i] = getMtkCallForwardInfo(mtkImsCallForwardInfoArr[i]);
            }
        }
        return mtkCallForwardInfoArr;
    }

    private MtkCallForwardInfo getMtkCallForwardInfo(MtkImsCallForwardInfo mtkImsCallForwardInfo) {
        MtkCallForwardInfo mtkCallForwardInfo = new MtkCallForwardInfo();
        mtkCallForwardInfo.status = mtkImsCallForwardInfo.mStatus;
        mtkCallForwardInfo.reason = getCFReasonFromCondition(mtkImsCallForwardInfo.mCondition);
        mtkCallForwardInfo.serviceClass = mtkImsCallForwardInfo.mServiceClass;
        mtkCallForwardInfo.toa = mtkImsCallForwardInfo.mToA;
        mtkCallForwardInfo.number = mtkImsCallForwardInfo.mNumber;
        mtkCallForwardInfo.timeSeconds = mtkImsCallForwardInfo.mTimeSeconds;
        mtkCallForwardInfo.timeSlot = mtkImsCallForwardInfo.mTimeSlot;
        return mtkCallForwardInfo;
    }

    public void sendUssdResponse(String str) {
        logd("sendUssdResponse");
        MtkImsPhoneMmiCode mtkImsPhoneMmiCodeNewFromUssdUserInput = MtkImsPhoneMmiCode.newFromUssdUserInput(str, this);
        this.mPendingMMIs.add(mtkImsPhoneMmiCodeNewFromUssdUserInput);
        logd("sendUssdResponse: " + str + ", mmi=" + mtkImsPhoneMmiCodeNewFromUssdUserInput);
        dumpPendingMmi();
        this.mMmiRegistrants.notifyRegistrants(new AsyncResult((Object) null, mtkImsPhoneMmiCodeNewFromUssdUserInput, (Throwable) null));
        mtkImsPhoneMmiCodeNewFromUssdUserInput.sendUssd(str);
    }

    public void cancelUSSD(Message message) {
        ((MtkImsPhoneCallTracker) this.mCT).cancelUSSD(message);
    }

    protected CommandException getCommandException(int i, String str) {
        logd("getCommandException code= " + i + ", errorString= " + str);
        CommandException.Error error = CommandException.Error.GENERIC_FAILURE;
        switch (i) {
            case LastCallFailCause.FDN_BLOCKED:
                error = CommandException.Error.FDN_CHECK_FAILURE;
                break;
            case 801:
                error = CommandException.Error.REQUEST_NOT_SUPPORTED;
                break;
            case 802:
                error = CommandException.Error.RADIO_NOT_AVAILABLE;
                break;
            case 821:
                error = CommandException.Error.PASSWORD_INCORRECT;
                break;
            case 822:
                error = CommandException.Error.SS_MODIFIED_TO_DIAL;
                break;
            case 823:
                error = CommandException.Error.SS_MODIFIED_TO_USSD;
                break;
            case 824:
                error = CommandException.Error.SS_MODIFIED_TO_SS;
                break;
            case 825:
                error = CommandException.Error.SS_MODIFIED_TO_DIAL_VIDEO;
                break;
            case LastCallFailCause.OEM_CAUSE_6:
                error = CommandException.Error.OPERATION_NOT_ALLOWED;
                break;
            case LastCallFailCause.OEM_CAUSE_7:
                error = CommandException.Error.NO_NETWORK_FOUND;
                break;
            case LastCallFailCause.OEM_CAUSE_9:
                error = CommandException.Error.OEM_ERROR_1;
                break;
            case LastCallFailCause.OEM_CAUSE_10:
                error = CommandException.Error.OEM_ERROR_7;
                break;
        }
        return new CommandException(error, str);
    }

    protected CallForwardInfo getCallForwardInfo(ImsCallForwardInfo imsCallForwardInfo) {
        CallForwardInfo callForwardInfo = new CallForwardInfo();
        callForwardInfo.status = imsCallForwardInfo.mStatus;
        callForwardInfo.reason = getCFReasonFromCondition(imsCallForwardInfo.mCondition);
        callForwardInfo.serviceClass = imsCallForwardInfo.mServiceClass;
        callForwardInfo.toa = imsCallForwardInfo.mToA;
        callForwardInfo.number = imsCallForwardInfo.mNumber;
        callForwardInfo.timeSeconds = imsCallForwardInfo.mTimeSeconds;
        return callForwardInfo;
    }

    public CallForwardInfo[] handleCfQueryResult(ImsCallForwardInfo[] imsCallForwardInfoArr) {
        CallForwardInfo[] callForwardInfoArr;
        if (supportMdAutoSetupIms()) {
            if (imsCallForwardInfoArr != null && imsCallForwardInfoArr.length != 0) {
                callForwardInfoArr = new CallForwardInfo[imsCallForwardInfoArr.length];
            } else {
                callForwardInfoArr = null;
            }
        } else if (imsCallForwardInfoArr != null) {
            callForwardInfoArr = new CallForwardInfo[imsCallForwardInfoArr.length];
        }
        IccRecords iccRecords = this.mDefaultPhone.getIccRecords();
        if (imsCallForwardInfoArr == null || imsCallForwardInfoArr.length == 0) {
            if (iccRecords != null) {
                setVoiceCallForwardingFlag(iccRecords, 1, false, null);
            }
        } else {
            int length = imsCallForwardInfoArr.length;
            for (int i = 0; i < length; i++) {
                if (imsCallForwardInfoArr[i].mCondition == 0 && (imsCallForwardInfoArr[i].mServiceClass & 1) != 0 && iccRecords != null) {
                    setVoiceCallForwardingFlag(iccRecords, 1, imsCallForwardInfoArr[i].mStatus == 1, imsCallForwardInfoArr[i].mNumber);
                }
                callForwardInfoArr[i] = getCallForwardInfo(imsCallForwardInfoArr[i]);
            }
        }
        return callForwardInfoArr;
    }

    protected int[] handleCbQueryResult(ImsSsInfo[] imsSsInfoArr) {
        return new int[]{imsSsInfoArr[0].mStatus};
    }

    public void handleMessage(Message message) {
        ImsException imsException;
        Message message2;
        ImsException imsException2;
        Message message3;
        ImsException imsException3;
        ImsException imsException4;
        AsyncResult asyncResult = (AsyncResult) message.obj;
        logd("handleMessage what=" + message.what);
        switch (message.what) {
            case 12:
                IccRecords iccRecords = this.mDefaultPhone.getIccRecords();
                Cf cf = (Cf) asyncResult.userObj;
                int i = message.arg1;
                int i2 = message.arg2;
                boolean zIsCfEnable = isCfEnable(i);
                if (cf.mIsCfu && asyncResult.exception == null && iccRecords != null) {
                    if (this.mDefaultPhone.queryCFUAgainAfterSet() && i2 == 0) {
                        if (asyncResult.result == null) {
                            logi("arResult is null.");
                        } else {
                            logd("[EVENT_SET_CALL_FORWARD_DONE check cfinfo.");
                        }
                    } else if ((cf.mServiceClass & 1) != 0) {
                        setVoiceCallForwardingFlag(iccRecords, 1, zIsCfEnable, cf.mSetCfNumber);
                    }
                }
                if (this.mDefaultPhone.getPhoneType() == 1 && (this.mDefaultPhone instanceof MtkGsmCdmaPhone) && this.mDefaultPhone.isSupportSaveCFNumber() && asyncResult.exception == null) {
                    if (zIsCfEnable && !this.mDefaultPhone.applyCFSharePreference(i2, cf.mSetCfNumber)) {
                        logd("applySharePreference false.");
                    }
                    if (i == 4) {
                        this.mDefaultPhone.clearCFSharePreference(i2);
                    }
                }
                sendResponse(cf.mOnComplete, null, asyncResult.exception);
                break;
            case 46:
                if (!supportMdAutoSetupIms() && this.mDefaultPhone.isOpTransferXcap404() && asyncResult.exception != null && (asyncResult.exception instanceof ImsException) && (imsException = asyncResult.exception) != null && imsException.getCode() == 61448 && (message2 = (Message) asyncResult.userObj) != null) {
                    AsyncResult.forMessage(message2, (Object) null, new CommandException(CommandException.Error.NO_SUCH_ELEMENT));
                    message2.sendToTarget();
                } else {
                    sendResponse((Message) asyncResult.userObj, null, asyncResult.exception);
                }
                break;
            case 47:
                if (!supportMdAutoSetupIms() && this.mDefaultPhone.isOpTransferXcap404() && asyncResult.exception != null && (asyncResult.exception instanceof ImsException) && (imsException2 = asyncResult.exception) != null && imsException2.getCode() == 61448 && (message3 = (Message) asyncResult.userObj) != null) {
                    AsyncResult.forMessage(message3, (Object) null, new CommandException(CommandException.Error.NO_SUCH_ELEMENT));
                    message3.sendToTarget();
                } else {
                    sendResponse((Message) asyncResult.userObj, asyncResult.exception == null ? handleCbQueryResult((ImsSsInfo[]) asyncResult.result) : null, asyncResult.exception);
                }
                break;
            case 109:
                Object objHandleCfInTimeSlotQueryResult = asyncResult.exception == null ? handleCfInTimeSlotQueryResult((MtkImsCallForwardInfo[]) asyncResult.result) : null;
                if (asyncResult.exception != null && (asyncResult.exception instanceof ImsException) && (imsException3 = asyncResult.exception) != null && imsException3.getCode() == 61446) {
                    this.mDefaultPhone.setCsFallbackStatus(2);
                    Message message4 = (Message) asyncResult.userObj;
                    if (message4 != null) {
                        AsyncResult.forMessage(message4, objHandleCfInTimeSlotQueryResult, new CommandException(CommandException.Error.REQUEST_NOT_SUPPORTED));
                        message4.sendToTarget();
                    }
                }
                sendResponse((Message) asyncResult.userObj, objHandleCfInTimeSlotQueryResult, asyncResult.exception);
                break;
            case 110:
                IccRecords iccRecords2 = this.mDefaultPhone.getIccRecords();
                CfEx cfEx = (CfEx) asyncResult.userObj;
                if (cfEx.mIsCfu && asyncResult.exception == null && iccRecords2 != null) {
                    setVoiceCallForwardingFlag(iccRecords2, 1, isCfEnable(message.arg1), cfEx.mSetCfNumber);
                    saveTimeSlot(cfEx.mSetTimeSlot);
                }
                if (asyncResult.exception != null && (asyncResult.exception instanceof ImsException) && (imsException4 = asyncResult.exception) != null && imsException4.getCode() == 61446) {
                    this.mDefaultPhone.setCsFallbackStatus(2);
                    Message message5 = cfEx.mOnComplete;
                    if (message5 != null) {
                        AsyncResult.forMessage(message5, (Object) null, new CommandException(CommandException.Error.REQUEST_NOT_SUPPORTED));
                        message5.sendToTarget();
                    }
                }
                sendResponse(cfEx.mOnComplete, null, asyncResult.exception);
                break;
            default:
                super.handleMessage(message);
                break;
        }
    }

    @VisibleForTesting
    public void setServiceState(int i) {
        super.setServiceState(i);
        updateIsEmergencyOnly();
    }

    private boolean isSupportImsEcc() {
        return ((MtkImsPhoneCallTracker) this.mCT).isSupportImsEcc();
    }

    public void updateIsEmergencyOnly() {
        ServiceState serviceState = getServiceState();
        logd("updateIsEmergencyOnly() sst: " + serviceState.getState() + " supportImsEcc: " + isSupportImsEcc());
        if (serviceState.getState() == 1 && isSupportImsEcc()) {
            this.mSS.setEmergencyOnly(true);
        } else {
            this.mSS.setEmergencyOnly(false);
        }
    }

    private boolean supportMdAutoSetupIms() {
        if (SystemProperties.get("ro.vendor.md_auto_setup_ims").equals("1")) {
            return true;
        }
        return false;
    }

    private void registerForListenCarrierConfigChanged() {
        if (this.mContext == null) {
            logd("registerForListenCarrierConfigChanged failed");
            return;
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.telephony.action.CARRIER_CONFIG_CHANGED");
        this.mContext.registerReceiver(this.mReceiver, intentFilter);
    }

    private void cacheCarrierConfiguration() {
        int subId = getSubId();
        CarrierConfigManager carrierConfigManager = (CarrierConfigManager) this.mContext.getSystemService("carrier_config");
        if (carrierConfigManager == null) {
            logd("cacheCarrierConfiguration failed: config mgr access failed");
            return;
        }
        PersistableBundle configForSubId = carrierConfigManager.getConfigForSubId(subId);
        if (configForSubId == null) {
            logd("cacheCarrierConfiguration failed: carrier config access failed");
            return;
        }
        this.mIsConsultativeEctSupported = configForSubId.getBoolean("mtk_carrier_consultative_ect_supported");
        this.mIsBlindAssuredEctSupported = configForSubId.getBoolean("mtk_carrier_blind_assured_ect_supported");
        this.mIsDeviceSwitchSupported = configForSubId.getBoolean("mtk_carrier_device_switch_supported");
        logd("cacheCarrierConfiguration, blindAssureEctSupported: " + this.mIsBlindAssuredEctSupported + " deviceSwitchSupported: " + this.mIsDeviceSwitchSupported);
    }

    public boolean isFeatureSupported(FeatureType featureType) {
        if (featureType == FeatureType.VOLTE_ENHANCED_CONFERENCE || featureType == FeatureType.VIDEO_RESTRICTION || featureType == FeatureType.VOLTE_ECT) {
            List listAsList = Arrays.asList("46000", "46002", "46007", "46008");
            IccRecords iccRecords = this.mDefaultPhone.getIccRecords();
            if (iccRecords == null) {
                logd("isFeatureSupported(" + featureType + ") no iccRecords");
                return false;
            }
            String operatorNumeric = iccRecords.getOperatorNumeric();
            if (featureType == FeatureType.VOLTE_ECT) {
                if (this.mIsBlindAssuredEctSupported) {
                    logd("vodafone isFeatureSupported(" + featureType + "): true current mccMnc = " + operatorNumeric);
                    return true;
                }
                if (this.mIsDigitsSupported && this.mIsDeviceSwitchSupported) {
                    logd("Digits device and TMO card, ECT supported: " + operatorNumeric);
                    return true;
                }
            } else {
                boolean zContains = listAsList.contains(operatorNumeric);
                logd("isFeatureSupported(" + featureType + "): ret = " + zContains + " current mccMnc = " + operatorNumeric);
                return zContains;
            }
        } else {
            if (featureType == FeatureType.VOLTE_CONF_REMOVE_MEMBER) {
                return true;
            }
            if (featureType == FeatureType.CONSULTATIVE_ECT) {
                if (this.mIsConsultativeEctSupported) {
                    logd("isFeatureSupported(" + featureType + "): true");
                    return true;
                }
                logd("isFeatureSupported(" + featureType + "): false");
                return false;
            }
        }
        return false;
    }

    public boolean isWifiPdnOutOfService() {
        return ((MtkImsPhoneCallTracker) this.mCT).isWifiPdnOutOfService();
    }

    public void setVoiceCallForwardingFlag(IccRecords iccRecords, int i, boolean z, String str) {
        IccRecords iccRecords2;
        super.setVoiceCallForwardingFlag(iccRecords, i, z, str);
        if (this.mDefaultPhone.getPhoneType() == 2 && (this.mDefaultPhone instanceof MtkGsmCdmaPhone) && this.mDefaultPhone.isGsmSsPrefer()) {
            UiccController uiccController = UiccController.getInstance();
            if (uiccController != null && (iccRecords2 = uiccController.getIccRecords(this.mPhoneId, 1)) != null) {
                iccRecords2.setVoiceCallForwardingFlag(i, z, str);
            }
            this.mDefaultPhone.notifyCallForwardingIndicator();
        }
    }
}
