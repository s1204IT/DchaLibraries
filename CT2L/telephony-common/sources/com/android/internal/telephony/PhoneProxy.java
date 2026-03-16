package com.android.internal.telephony;

import android.R;
import android.app.ActivityManagerNative;
import android.content.Context;
import android.content.Intent;
import android.net.LinkProperties;
import android.net.NetworkCapabilities;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.telephony.CellInfo;
import android.telephony.CellLocation;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionManager;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.cdma.CDMALTEPhone;
import com.android.internal.telephony.cdma.CDMAPhone;
import com.android.internal.telephony.dataconnection.DctController;
import com.android.internal.telephony.gsm.GSMPhone;
import com.android.internal.telephony.imsphone.ImsPhone;
import com.android.internal.telephony.test.SimulatedRadioControl;
import com.android.internal.telephony.uicc.IccCardApplicationStatus;
import com.android.internal.telephony.uicc.IccCardProxy;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.telephony.uicc.IsimRecords;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UsimServiceTable;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.List;

public class PhoneProxy extends Handler implements Phone {
    private static final int EVENT_RADIO_ON = 2;
    private static final int EVENT_REQUEST_VOICE_RADIO_TECH_DONE = 3;
    private static final int EVENT_RIL_CONNECTED = 4;
    private static final int EVENT_SIM_RECORDS_LOADED = 6;
    private static final int EVENT_UPDATE_PHONE_OBJECT = 5;
    private static final int EVENT_VOICE_RADIO_TECH_CHANGED = 1;
    private static final String LOG_TAG = "PhoneProxy";
    public static final Object lockForRadioTechnologyChange = new Object();
    private Phone mActivePhone;
    private CommandsInterface mCommandsInterface;
    private IccCardProxy mIccCardProxy;
    private IccPhoneBookInterfaceManagerProxy mIccPhoneBookInterfaceManagerProxy;
    private IccSmsInterfaceManager mIccSmsInterfaceManager;
    private int mPhoneId;
    private PhoneSubInfoProxy mPhoneSubInfoProxy;
    private boolean mResetModemOnRadioTechnologyChange;
    private int mRilVersion;

    public PhoneProxy(PhoneBase phone) {
        this.mResetModemOnRadioTechnologyChange = false;
        this.mPhoneId = 0;
        this.mActivePhone = phone;
        this.mResetModemOnRadioTechnologyChange = SystemProperties.getBoolean("persist.radio.reset_on_switch", false);
        this.mIccPhoneBookInterfaceManagerProxy = new IccPhoneBookInterfaceManagerProxy(phone.getIccPhoneBookInterfaceManager());
        this.mPhoneSubInfoProxy = new PhoneSubInfoProxy(phone.getPhoneSubInfo());
        this.mCommandsInterface = ((PhoneBase) this.mActivePhone).mCi;
        this.mCommandsInterface.registerForRilConnected(this, 4, null);
        this.mCommandsInterface.registerForOn(this, 2, null);
        this.mCommandsInterface.registerForVoiceRadioTechChanged(this, 1, null);
        this.mPhoneId = phone.getPhoneId();
        this.mIccSmsInterfaceManager = new IccSmsInterfaceManager((PhoneBase) this.mActivePhone);
        this.mIccCardProxy = new IccCardProxy(this.mActivePhone.getContext(), this.mCommandsInterface, this.mActivePhone.getPhoneId());
        if (phone.getPhoneType() == 1) {
            this.mIccCardProxy.setVoiceRadioTech(3);
        } else if (phone.getPhoneType() == 2) {
            this.mIccCardProxy.setVoiceRadioTech(6);
        }
    }

    @Override
    public void handleMessage(Message msg) {
        AsyncResult ar = (AsyncResult) msg.obj;
        switch (msg.what) {
            case 1:
            case 3:
                String what = msg.what == 1 ? "EVENT_VOICE_RADIO_TECH_CHANGED" : "EVENT_REQUEST_VOICE_RADIO_TECH_DONE";
                if (ar.exception == null) {
                    if (ar.result != null && ((int[]) ar.result).length != 0) {
                        int newVoiceTech = ((int[]) ar.result)[0];
                        logd(what + ": newVoiceTech=" + newVoiceTech);
                        phoneObjectUpdater(newVoiceTech);
                    } else {
                        loge(what + ": has no tech!");
                    }
                } else {
                    loge(what + ": exception=" + ar.exception);
                }
                break;
            case 2:
                this.mCommandsInterface.getVoiceRadioTechnology(obtainMessage(3));
                break;
            case 4:
                if (ar.exception == null && ar.result != null) {
                    this.mRilVersion = ((Integer) ar.result).intValue();
                } else {
                    logd("Unexpected exception on EVENT_RIL_CONNECTED");
                    this.mRilVersion = -1;
                }
                break;
            case 5:
                phoneObjectUpdater(msg.arg1);
                break;
            case 6:
                if (!this.mActivePhone.getContext().getResources().getBoolean(R.^attr-private.magnifierHeight)) {
                    this.mCommandsInterface.getVoiceRadioTechnology(obtainMessage(3));
                }
                break;
            default:
                loge("Error! This handler was not registered for this message type. Message: " + msg.what);
                break;
        }
        super.handleMessage(msg);
    }

    private static void logd(String msg) {
        Rlog.d(LOG_TAG, "[PhoneProxy] " + msg);
    }

    private void loge(String msg) {
        Rlog.e(LOG_TAG, "[PhoneProxy] " + msg);
    }

    private void phoneObjectUpdater(int newVoiceRadioTech) {
        logd("phoneObjectUpdater: newVoiceRadioTech=" + newVoiceRadioTech);
        if (this.mActivePhone != null) {
            if (newVoiceRadioTech == 14 || newVoiceRadioTech == 0) {
                int volteReplacementRat = this.mActivePhone.getContext().getResources().getInteger(R.integer.config_defaultNightMode);
                logd("phoneObjectUpdater: volteReplacementRat=" + volteReplacementRat);
                if (volteReplacementRat != 0) {
                    newVoiceRadioTech = volteReplacementRat;
                }
            }
            if (this.mRilVersion == 6 && getLteOnCdmaMode() == 1) {
                if (this.mActivePhone.getPhoneType() == 2) {
                    logd("phoneObjectUpdater: LTE ON CDMA property is set. Use CDMA Phone newVoiceRadioTech=" + newVoiceRadioTech + " mActivePhone=" + this.mActivePhone.getPhoneName());
                    return;
                } else {
                    logd("phoneObjectUpdater: LTE ON CDMA property is set. Switch to CDMALTEPhone newVoiceRadioTech=" + newVoiceRadioTech + " mActivePhone=" + this.mActivePhone.getPhoneName());
                    newVoiceRadioTech = 6;
                }
            } else {
                boolean matchCdma = ServiceState.isCdma(newVoiceRadioTech);
                boolean matchGsm = ServiceState.isGsm(newVoiceRadioTech);
                if ((matchCdma && this.mActivePhone.getPhoneType() == 2) || (matchGsm && this.mActivePhone.getPhoneType() == 1)) {
                    logd("phoneObjectUpdater: No change ignore, newVoiceRadioTech=" + newVoiceRadioTech + " mActivePhone=" + this.mActivePhone.getPhoneName());
                    return;
                } else if (!matchCdma && !matchGsm) {
                    loge("phoneObjectUpdater: newVoiceRadioTech=" + newVoiceRadioTech + " doesn't match either CDMA or GSM - error! No phone change");
                    return;
                }
            }
        }
        if (newVoiceRadioTech == 0) {
            logd("phoneObjectUpdater: Unknown rat ignore,  newVoiceRadioTech=Unknown. mActivePhone=" + this.mActivePhone.getPhoneName());
            return;
        }
        boolean oldPowerState = false;
        if (this.mResetModemOnRadioTechnologyChange && this.mCommandsInterface.getRadioState().isOn()) {
            oldPowerState = true;
            logd("phoneObjectUpdater: Setting Radio Power to Off");
            this.mCommandsInterface.setRadioPower(false, null);
        }
        deleteAndCreatePhone(newVoiceRadioTech);
        if (this.mResetModemOnRadioTechnologyChange && oldPowerState) {
            logd("phoneObjectUpdater: Resetting Radio");
            this.mCommandsInterface.setRadioPower(oldPowerState, null);
        }
        this.mIccSmsInterfaceManager.updatePhoneObject((PhoneBase) this.mActivePhone);
        this.mIccPhoneBookInterfaceManagerProxy.setmIccPhoneBookInterfaceManager(this.mActivePhone.getIccPhoneBookInterfaceManager());
        this.mPhoneSubInfoProxy.setmPhoneSubInfo(this.mActivePhone.getPhoneSubInfo());
        this.mCommandsInterface = ((PhoneBase) this.mActivePhone).mCi;
        this.mIccCardProxy.setVoiceRadioTech(newVoiceRadioTech);
        Intent intent = new Intent("android.intent.action.RADIO_TECHNOLOGY");
        intent.addFlags(536870912);
        intent.putExtra("phoneName", this.mActivePhone.getPhoneName());
        SubscriptionManager.putPhoneIdAndSubIdExtra(intent, this.mPhoneId);
        ActivityManagerNative.broadcastStickyIntent(intent, (String) null, -1);
        DctController.getInstance().updatePhoneObject(this);
    }

    private void deleteAndCreatePhone(int newVoiceRadioTech) {
        String outgoingPhoneName = "Unknown";
        Phone oldPhone = this.mActivePhone;
        ImsPhone imsPhone = null;
        if (oldPhone != null) {
            outgoingPhoneName = ((PhoneBase) oldPhone).getPhoneName();
            oldPhone.unregisterForSimRecordsLoaded(this);
        }
        logd("Switching Voice Phone : " + outgoingPhoneName + " >>> " + (ServiceState.isGsm(newVoiceRadioTech) ? "GSM" : "CDMA"));
        if (ServiceState.isCdma(newVoiceRadioTech)) {
            this.mActivePhone = PhoneFactory.getCdmaPhone(this.mPhoneId);
        } else if (ServiceState.isGsm(newVoiceRadioTech)) {
            this.mActivePhone = PhoneFactory.getGsmPhone(this.mPhoneId);
        } else {
            loge("deleteAndCreatePhone: newVoiceRadioTech=" + newVoiceRadioTech + " is not CDMA or GSM (error) - aborting!");
            return;
        }
        if (oldPhone != null) {
            imsPhone = oldPhone.relinquishOwnershipOfImsPhone();
        }
        if (this.mActivePhone != null) {
            CallManager.getInstance().registerPhone(this.mActivePhone);
            if (imsPhone != null) {
                this.mActivePhone.acquireOwnershipOfImsPhone(imsPhone);
            }
            this.mActivePhone.registerForSimRecordsLoaded(this, 6, null);
        }
        if (oldPhone != null) {
            CallManager.getInstance().unregisterPhone(oldPhone);
            logd("Disposing old phone..");
            oldPhone.dispose();
        }
    }

    public IccSmsInterfaceManager getIccSmsInterfaceManager() {
        return this.mIccSmsInterfaceManager;
    }

    public PhoneSubInfoProxy getPhoneSubInfoProxy() {
        return this.mPhoneSubInfoProxy;
    }

    public IccPhoneBookInterfaceManagerProxy getIccPhoneBookInterfaceManagerProxy() {
        return this.mIccPhoneBookInterfaceManagerProxy;
    }

    public IccFileHandler getIccFileHandler() {
        return ((PhoneBase) this.mActivePhone).getIccFileHandler();
    }

    @Override
    public void updatePhoneObject(int voiceRadioTech) {
        logd("updatePhoneObject: radioTechnology=" + voiceRadioTech);
        sendMessage(obtainMessage(5, voiceRadioTech, 0, null));
    }

    @Override
    public ServiceState getServiceState() {
        return this.mActivePhone.getServiceState();
    }

    @Override
    public CellLocation getCellLocation() {
        return this.mActivePhone.getCellLocation();
    }

    @Override
    public List<CellInfo> getAllCellInfo() {
        return this.mActivePhone.getAllCellInfo();
    }

    @Override
    public void setCellInfoListRate(int rateInMillis) {
        this.mActivePhone.setCellInfoListRate(rateInMillis);
    }

    @Override
    public PhoneConstants.DataState getDataConnectionState() {
        return this.mActivePhone.getDataConnectionState("default");
    }

    @Override
    public PhoneConstants.DataState getDataConnectionState(String apnType) {
        return this.mActivePhone.getDataConnectionState(apnType);
    }

    @Override
    public Phone.DataActivityState getDataActivityState() {
        return this.mActivePhone.getDataActivityState();
    }

    @Override
    public Context getContext() {
        return this.mActivePhone.getContext();
    }

    @Override
    public void disableDnsCheck(boolean b) {
        this.mActivePhone.disableDnsCheck(b);
    }

    @Override
    public boolean isDnsCheckDisabled() {
        return this.mActivePhone.isDnsCheckDisabled();
    }

    @Override
    public PhoneConstants.State getState() {
        return this.mActivePhone.getState();
    }

    @Override
    public String getPhoneName() {
        return this.mActivePhone.getPhoneName();
    }

    @Override
    public int getPhoneType() {
        return this.mActivePhone.getPhoneType();
    }

    @Override
    public String[] getActiveApnTypes() {
        return this.mActivePhone.getActiveApnTypes();
    }

    @Override
    public boolean hasMatchedTetherApnSetting() {
        return this.mActivePhone.hasMatchedTetherApnSetting();
    }

    @Override
    public String getActiveApnHost(String apnType) {
        return this.mActivePhone.getActiveApnHost(apnType);
    }

    @Override
    public LinkProperties getLinkProperties(String apnType) {
        return this.mActivePhone.getLinkProperties(apnType);
    }

    @Override
    public NetworkCapabilities getNetworkCapabilities(String apnType) {
        return this.mActivePhone.getNetworkCapabilities(apnType);
    }

    @Override
    public SignalStrength getSignalStrength() {
        return this.mActivePhone.getSignalStrength();
    }

    @Override
    public void registerForUnknownConnection(Handler h, int what, Object obj) {
        this.mActivePhone.registerForUnknownConnection(h, what, obj);
    }

    @Override
    public void unregisterForUnknownConnection(Handler h) {
        this.mActivePhone.unregisterForUnknownConnection(h);
    }

    @Override
    public void registerForHandoverStateChanged(Handler h, int what, Object obj) {
        this.mActivePhone.registerForHandoverStateChanged(h, what, obj);
    }

    @Override
    public void unregisterForHandoverStateChanged(Handler h) {
        this.mActivePhone.unregisterForHandoverStateChanged(h);
    }

    @Override
    public void registerForPreciseCallStateChanged(Handler h, int what, Object obj) {
        this.mActivePhone.registerForPreciseCallStateChanged(h, what, obj);
    }

    @Override
    public void unregisterForPreciseCallStateChanged(Handler h) {
        this.mActivePhone.unregisterForPreciseCallStateChanged(h);
    }

    @Override
    public void registerForNewRingingConnection(Handler h, int what, Object obj) {
        this.mActivePhone.registerForNewRingingConnection(h, what, obj);
    }

    @Override
    public void unregisterForNewRingingConnection(Handler h) {
        this.mActivePhone.unregisterForNewRingingConnection(h);
    }

    @Override
    public void registerForIncomingRing(Handler h, int what, Object obj) {
        this.mActivePhone.registerForIncomingRing(h, what, obj);
    }

    @Override
    public void unregisterForIncomingRing(Handler h) {
        this.mActivePhone.unregisterForIncomingRing(h);
    }

    @Override
    public void registerForDisconnect(Handler h, int what, Object obj) {
        this.mActivePhone.registerForDisconnect(h, what, obj);
    }

    @Override
    public void unregisterForDisconnect(Handler h) {
        this.mActivePhone.unregisterForDisconnect(h);
    }

    @Override
    public void registerForMmiInitiate(Handler h, int what, Object obj) {
        this.mActivePhone.registerForMmiInitiate(h, what, obj);
    }

    @Override
    public void unregisterForMmiInitiate(Handler h) {
        this.mActivePhone.unregisterForMmiInitiate(h);
    }

    @Override
    public void registerForMmiComplete(Handler h, int what, Object obj) {
        this.mActivePhone.registerForMmiComplete(h, what, obj);
    }

    @Override
    public void unregisterForMmiComplete(Handler h) {
        this.mActivePhone.unregisterForMmiComplete(h);
    }

    @Override
    public List<? extends MmiCode> getPendingMmiCodes() {
        return this.mActivePhone.getPendingMmiCodes();
    }

    @Override
    public void sendUssdResponse(String ussdMessge) {
        this.mActivePhone.sendUssdResponse(ussdMessge);
    }

    @Override
    public void registerForServiceStateChanged(Handler h, int what, Object obj) {
        this.mActivePhone.registerForServiceStateChanged(h, what, obj);
    }

    @Override
    public void unregisterForServiceStateChanged(Handler h) {
        this.mActivePhone.unregisterForServiceStateChanged(h);
    }

    @Override
    public void registerForSuppServiceNotification(Handler h, int what, Object obj) {
        this.mActivePhone.registerForSuppServiceNotification(h, what, obj);
    }

    @Override
    public void unregisterForSuppServiceNotification(Handler h) {
        this.mActivePhone.unregisterForSuppServiceNotification(h);
    }

    @Override
    public void registerForSuppServiceFailed(Handler h, int what, Object obj) {
        this.mActivePhone.registerForSuppServiceFailed(h, what, obj);
    }

    @Override
    public void unregisterForSuppServiceFailed(Handler h) {
        this.mActivePhone.unregisterForSuppServiceFailed(h);
    }

    @Override
    public void registerForInCallVoicePrivacyOn(Handler h, int what, Object obj) {
        this.mActivePhone.registerForInCallVoicePrivacyOn(h, what, obj);
    }

    @Override
    public void unregisterForInCallVoicePrivacyOn(Handler h) {
        this.mActivePhone.unregisterForInCallVoicePrivacyOn(h);
    }

    @Override
    public void registerForInCallVoicePrivacyOff(Handler h, int what, Object obj) {
        this.mActivePhone.registerForInCallVoicePrivacyOff(h, what, obj);
    }

    @Override
    public void unregisterForInCallVoicePrivacyOff(Handler h) {
        this.mActivePhone.unregisterForInCallVoicePrivacyOff(h);
    }

    @Override
    public void registerForCdmaOtaStatusChange(Handler h, int what, Object obj) {
        this.mActivePhone.registerForCdmaOtaStatusChange(h, what, obj);
    }

    @Override
    public void unregisterForCdmaOtaStatusChange(Handler h) {
        this.mActivePhone.unregisterForCdmaOtaStatusChange(h);
    }

    @Override
    public void registerForSubscriptionInfoReady(Handler h, int what, Object obj) {
        this.mActivePhone.registerForSubscriptionInfoReady(h, what, obj);
    }

    @Override
    public void unregisterForSubscriptionInfoReady(Handler h) {
        this.mActivePhone.unregisterForSubscriptionInfoReady(h);
    }

    @Override
    public void registerForEcmTimerReset(Handler h, int what, Object obj) {
        this.mActivePhone.registerForEcmTimerReset(h, what, obj);
    }

    @Override
    public void unregisterForEcmTimerReset(Handler h) {
        this.mActivePhone.unregisterForEcmTimerReset(h);
    }

    @Override
    public void registerForRingbackTone(Handler h, int what, Object obj) {
        this.mActivePhone.registerForRingbackTone(h, what, obj);
    }

    @Override
    public void unregisterForRingbackTone(Handler h) {
        this.mActivePhone.unregisterForRingbackTone(h);
    }

    @Override
    public void registerForOnHoldTone(Handler h, int what, Object obj) {
        this.mActivePhone.registerForOnHoldTone(h, what, obj);
    }

    @Override
    public void unregisterForOnHoldTone(Handler h) {
        this.mActivePhone.unregisterForOnHoldTone(h);
    }

    @Override
    public void registerForResendIncallMute(Handler h, int what, Object obj) {
        this.mActivePhone.registerForResendIncallMute(h, what, obj);
    }

    @Override
    public void unregisterForResendIncallMute(Handler h) {
        this.mActivePhone.unregisterForResendIncallMute(h);
    }

    @Override
    public void registerForSimRecordsLoaded(Handler h, int what, Object obj) {
        this.mActivePhone.registerForSimRecordsLoaded(h, what, obj);
    }

    @Override
    public void unregisterForSimRecordsLoaded(Handler h) {
        this.mActivePhone.unregisterForSimRecordsLoaded(h);
    }

    @Override
    public void registerForTtyModeReceived(Handler h, int what, Object obj) {
        this.mActivePhone.registerForTtyModeReceived(h, what, obj);
    }

    @Override
    public void unregisterForTtyModeReceived(Handler h) {
        this.mActivePhone.unregisterForTtyModeReceived(h);
    }

    @Override
    public boolean getIccRecordsLoaded() {
        return this.mIccCardProxy.getIccRecordsLoaded();
    }

    @Override
    public IccCardApplicationStatus.AppType getCurrentUiccAppType() {
        return this.mActivePhone.getCurrentUiccAppType();
    }

    @Override
    public IccCard getIccCard() {
        return this.mIccCardProxy;
    }

    @Override
    public void acceptCall(int videoState) throws CallStateException {
        this.mActivePhone.acceptCall(videoState);
    }

    @Override
    public void rejectCall() throws CallStateException {
        this.mActivePhone.rejectCall();
    }

    @Override
    public void switchHoldingAndActive() throws CallStateException {
        this.mActivePhone.switchHoldingAndActive();
    }

    @Override
    public boolean canConference() {
        return this.mActivePhone.canConference();
    }

    @Override
    public void conference() throws CallStateException {
        this.mActivePhone.conference();
    }

    @Override
    public void enableEnhancedVoicePrivacy(boolean enable, Message onComplete) {
        this.mActivePhone.enableEnhancedVoicePrivacy(enable, onComplete);
    }

    @Override
    public void getEnhancedVoicePrivacy(Message onComplete) {
        this.mActivePhone.getEnhancedVoicePrivacy(onComplete);
    }

    @Override
    public boolean canTransfer() {
        return this.mActivePhone.canTransfer();
    }

    @Override
    public void explicitCallTransfer() throws CallStateException {
        this.mActivePhone.explicitCallTransfer();
    }

    @Override
    public void clearDisconnected() {
        this.mActivePhone.clearDisconnected();
    }

    @Override
    public Call getForegroundCall() {
        return this.mActivePhone.getForegroundCall();
    }

    @Override
    public Call getBackgroundCall() {
        return this.mActivePhone.getBackgroundCall();
    }

    @Override
    public Call getRingingCall() {
        return this.mActivePhone.getRingingCall();
    }

    @Override
    public Connection dial(String dialString, int videoState) throws CallStateException {
        return this.mActivePhone.dial(dialString, videoState);
    }

    @Override
    public Connection dial(String dialString, UUSInfo uusInfo, int videoState) throws CallStateException {
        return this.mActivePhone.dial(dialString, uusInfo, videoState);
    }

    @Override
    public boolean handlePinMmi(String dialString) {
        return this.mActivePhone.handlePinMmi(dialString);
    }

    @Override
    public boolean handleInCallMmiCommands(String command) throws CallStateException {
        return this.mActivePhone.handleInCallMmiCommands(command);
    }

    @Override
    public void sendDtmf(char c) {
        this.mActivePhone.sendDtmf(c);
    }

    @Override
    public void startDtmf(char c) {
        this.mActivePhone.startDtmf(c);
    }

    @Override
    public void stopDtmf() {
        this.mActivePhone.stopDtmf();
    }

    @Override
    public void setRadioPower(boolean power) {
        this.mActivePhone.setRadioPower(power);
    }

    @Override
    public boolean getMessageWaitingIndicator() {
        return this.mActivePhone.getMessageWaitingIndicator();
    }

    @Override
    public boolean getCallForwardingIndicator() {
        return this.mActivePhone.getCallForwardingIndicator();
    }

    @Override
    public String getLine1Number() {
        return this.mActivePhone.getLine1Number();
    }

    @Override
    public String getCdmaMin() {
        return this.mActivePhone.getCdmaMin();
    }

    @Override
    public boolean isMinInfoReady() {
        return this.mActivePhone.isMinInfoReady();
    }

    @Override
    public String getCdmaPrlVersion() {
        return this.mActivePhone.getCdmaPrlVersion();
    }

    @Override
    public String getLine1AlphaTag() {
        return this.mActivePhone.getLine1AlphaTag();
    }

    @Override
    public boolean setLine1Number(String alphaTag, String number, Message onComplete) {
        return this.mActivePhone.setLine1Number(alphaTag, number, onComplete);
    }

    @Override
    public String getVoiceMailNumber() {
        return this.mActivePhone.getVoiceMailNumber();
    }

    @Override
    public int getVoiceMessageCount() {
        return this.mActivePhone.getVoiceMessageCount();
    }

    @Override
    public String getVoiceMailAlphaTag() {
        return this.mActivePhone.getVoiceMailAlphaTag();
    }

    @Override
    public void setVoiceMailNumber(String alphaTag, String voiceMailNumber, Message onComplete) {
        this.mActivePhone.setVoiceMailNumber(alphaTag, voiceMailNumber, onComplete);
    }

    @Override
    public void getCallForwardingOption(int commandInterfaceCFReason, Message onComplete) {
        this.mActivePhone.getCallForwardingOption(commandInterfaceCFReason, onComplete);
    }

    @Override
    public void setCallForwardingOption(int commandInterfaceCFReason, int commandInterfaceCFAction, String dialingNumber, int timerSeconds, Message onComplete) {
        this.mActivePhone.setCallForwardingOption(commandInterfaceCFReason, commandInterfaceCFAction, dialingNumber, timerSeconds, onComplete);
    }

    @Override
    public void getOutgoingCallerIdDisplay(Message onComplete) {
        this.mActivePhone.getOutgoingCallerIdDisplay(onComplete);
    }

    @Override
    public void setOutgoingCallerIdDisplay(int commandInterfaceCLIRMode, Message onComplete) {
        this.mActivePhone.setOutgoingCallerIdDisplay(commandInterfaceCLIRMode, onComplete);
    }

    @Override
    public void getCallWaiting(Message onComplete) {
        this.mActivePhone.getCallWaiting(onComplete);
    }

    @Override
    public void setCallWaiting(boolean enable, Message onComplete) {
        this.mActivePhone.setCallWaiting(enable, onComplete);
    }

    @Override
    public void getAvailableNetworks(Message response) {
        this.mActivePhone.getAvailableNetworks(response);
    }

    @Override
    public void setNetworkSelectionModeAutomatic(Message response) {
        this.mActivePhone.setNetworkSelectionModeAutomatic(response);
    }

    @Override
    public void getNetworkSelectionMode(Message response) {
        this.mActivePhone.getNetworkSelectionMode(response);
    }

    @Override
    public void selectNetworkManually(OperatorInfo network, Message response) {
        this.mActivePhone.selectNetworkManually(network, response);
    }

    @Override
    public void setPreferredNetworkType(int networkType, Message response) {
        this.mActivePhone.setPreferredNetworkType(networkType, response);
    }

    @Override
    public void getPreferredNetworkType(Message response) {
        this.mActivePhone.getPreferredNetworkType(response);
    }

    @Override
    public void getNeighboringCids(Message response) {
        this.mActivePhone.getNeighboringCids(response);
    }

    @Override
    public void setOnPostDialCharacter(Handler h, int what, Object obj) {
        this.mActivePhone.setOnPostDialCharacter(h, what, obj);
    }

    @Override
    public void setMute(boolean muted) {
        this.mActivePhone.setMute(muted);
    }

    @Override
    public boolean getMute() {
        return this.mActivePhone.getMute();
    }

    @Override
    public void setEchoSuppressionEnabled() {
        this.mActivePhone.setEchoSuppressionEnabled();
    }

    @Override
    public void invokeOemRilRequestRaw(byte[] data, Message response) {
        this.mActivePhone.invokeOemRilRequestRaw(data, response);
    }

    @Override
    public void invokeOemRilRequestStrings(String[] strings, Message response) {
        this.mActivePhone.invokeOemRilRequestStrings(strings, response);
    }

    @Override
    public void getDataCallList(Message response) {
        this.mActivePhone.getDataCallList(response);
    }

    @Override
    public void updateServiceLocation() {
        this.mActivePhone.updateServiceLocation();
    }

    @Override
    public void enableLocationUpdates() {
        this.mActivePhone.enableLocationUpdates();
    }

    @Override
    public void disableLocationUpdates() {
        this.mActivePhone.disableLocationUpdates();
    }

    @Override
    public void setUnitTestMode(boolean f) {
        this.mActivePhone.setUnitTestMode(f);
    }

    @Override
    public boolean getUnitTestMode() {
        return this.mActivePhone.getUnitTestMode();
    }

    @Override
    public void setBandMode(int bandMode, Message response) {
        this.mActivePhone.setBandMode(bandMode, response);
    }

    @Override
    public void queryAvailableBandMode(Message response) {
        this.mActivePhone.queryAvailableBandMode(response);
    }

    @Override
    public boolean getDataRoamingEnabled() {
        return this.mActivePhone.getDataRoamingEnabled();
    }

    @Override
    public void setDataRoamingEnabled(boolean enable) {
        this.mActivePhone.setDataRoamingEnabled(enable);
    }

    @Override
    public boolean getDataEnabled() {
        return this.mActivePhone.getDataEnabled();
    }

    @Override
    public void setDataEnabled(boolean enable) {
        this.mActivePhone.setDataEnabled(enable);
    }

    @Override
    public void queryCdmaRoamingPreference(Message response) {
        this.mActivePhone.queryCdmaRoamingPreference(response);
    }

    @Override
    public void setCdmaRoamingPreference(int cdmaRoamingType, Message response) {
        this.mActivePhone.setCdmaRoamingPreference(cdmaRoamingType, response);
    }

    @Override
    public void setCdmaSubscription(int cdmaSubscriptionType, Message response) {
        this.mActivePhone.setCdmaSubscription(cdmaSubscriptionType, response);
    }

    @Override
    public SimulatedRadioControl getSimulatedRadioControl() {
        return this.mActivePhone.getSimulatedRadioControl();
    }

    @Override
    public boolean isDataConnectivityPossible() {
        return this.mActivePhone.isDataConnectivityPossible("default");
    }

    @Override
    public boolean isDataConnectivityPossible(String apnType) {
        return this.mActivePhone.isDataConnectivityPossible(apnType);
    }

    @Override
    public String getDeviceId() {
        return this.mActivePhone.getDeviceId();
    }

    @Override
    public String getDeviceSvn() {
        return this.mActivePhone.getDeviceSvn();
    }

    @Override
    public String getSubscriberId() {
        return this.mActivePhone.getSubscriberId();
    }

    @Override
    public String getGroupIdLevel1() {
        return this.mActivePhone.getGroupIdLevel1();
    }

    @Override
    public String getIccSerialNumber() {
        return this.mActivePhone.getIccSerialNumber();
    }

    @Override
    public String getEsn() {
        return this.mActivePhone.getEsn();
    }

    @Override
    public String getMeid() {
        return this.mActivePhone.getMeid();
    }

    @Override
    public String getMsisdn() {
        return this.mActivePhone.getMsisdn();
    }

    @Override
    public String getImei() {
        return this.mActivePhone.getImei();
    }

    @Override
    public String getNai() {
        return this.mActivePhone.getNai();
    }

    @Override
    public PhoneSubInfo getPhoneSubInfo() {
        return this.mActivePhone.getPhoneSubInfo();
    }

    @Override
    public IccPhoneBookInterfaceManager getIccPhoneBookInterfaceManager() {
        return this.mActivePhone.getIccPhoneBookInterfaceManager();
    }

    @Override
    public void setUiTTYMode(int uiTtyMode, Message onComplete) {
        this.mActivePhone.setUiTTYMode(uiTtyMode, onComplete);
    }

    @Override
    public void setTTYMode(int ttyMode, Message onComplete) {
        this.mActivePhone.setTTYMode(ttyMode, onComplete);
    }

    @Override
    public void queryTTYMode(Message onComplete) {
        this.mActivePhone.queryTTYMode(onComplete);
    }

    @Override
    public void activateCellBroadcastSms(int activate, Message response) {
        this.mActivePhone.activateCellBroadcastSms(activate, response);
    }

    @Override
    public void getCellBroadcastSmsConfig(Message response) {
        this.mActivePhone.getCellBroadcastSmsConfig(response);
    }

    @Override
    public void setCellBroadcastSmsConfig(int[] configValuesArray, Message response) {
        this.mActivePhone.setCellBroadcastSmsConfig(configValuesArray, response);
    }

    @Override
    public void notifyDataActivity() {
        this.mActivePhone.notifyDataActivity();
    }

    @Override
    public void getSmscAddress(Message result) {
        this.mActivePhone.getSmscAddress(result);
    }

    @Override
    public void setSmscAddress(String address, Message result) {
        this.mActivePhone.setSmscAddress(address, result);
    }

    @Override
    public int getCdmaEriIconIndex() {
        return this.mActivePhone.getCdmaEriIconIndex();
    }

    @Override
    public String getCdmaEriText() {
        return this.mActivePhone.getCdmaEriText();
    }

    @Override
    public int getCdmaEriIconMode() {
        return this.mActivePhone.getCdmaEriIconMode();
    }

    public Phone getActivePhone() {
        return this.mActivePhone;
    }

    @Override
    public void sendBurstDtmf(String dtmfString, int on, int off, Message onComplete) {
        this.mActivePhone.sendBurstDtmf(dtmfString, on, off, onComplete);
    }

    @Override
    public void exitEmergencyCallbackMode() {
        this.mActivePhone.exitEmergencyCallbackMode();
    }

    @Override
    public boolean needsOtaServiceProvisioning() {
        return this.mActivePhone.needsOtaServiceProvisioning();
    }

    @Override
    public boolean isOtaSpNumber(String dialStr) {
        return this.mActivePhone.isOtaSpNumber(dialStr);
    }

    @Override
    public void registerForCallWaiting(Handler h, int what, Object obj) {
        this.mActivePhone.registerForCallWaiting(h, what, obj);
    }

    @Override
    public void unregisterForCallWaiting(Handler h) {
        this.mActivePhone.unregisterForCallWaiting(h);
    }

    @Override
    public void registerForSignalInfo(Handler h, int what, Object obj) {
        this.mActivePhone.registerForSignalInfo(h, what, obj);
    }

    @Override
    public void unregisterForSignalInfo(Handler h) {
        this.mActivePhone.unregisterForSignalInfo(h);
    }

    @Override
    public void registerForDisplayInfo(Handler h, int what, Object obj) {
        this.mActivePhone.registerForDisplayInfo(h, what, obj);
    }

    @Override
    public void unregisterForDisplayInfo(Handler h) {
        this.mActivePhone.unregisterForDisplayInfo(h);
    }

    @Override
    public void registerForNumberInfo(Handler h, int what, Object obj) {
        this.mActivePhone.registerForNumberInfo(h, what, obj);
    }

    @Override
    public void unregisterForNumberInfo(Handler h) {
        this.mActivePhone.unregisterForNumberInfo(h);
    }

    @Override
    public void registerForRedirectedNumberInfo(Handler h, int what, Object obj) {
        this.mActivePhone.registerForRedirectedNumberInfo(h, what, obj);
    }

    @Override
    public void unregisterForRedirectedNumberInfo(Handler h) {
        this.mActivePhone.unregisterForRedirectedNumberInfo(h);
    }

    @Override
    public void registerForLineControlInfo(Handler h, int what, Object obj) {
        this.mActivePhone.registerForLineControlInfo(h, what, obj);
    }

    @Override
    public void unregisterForLineControlInfo(Handler h) {
        this.mActivePhone.unregisterForLineControlInfo(h);
    }

    @Override
    public void registerFoT53ClirlInfo(Handler h, int what, Object obj) {
        this.mActivePhone.registerFoT53ClirlInfo(h, what, obj);
    }

    @Override
    public void unregisterForT53ClirInfo(Handler h) {
        this.mActivePhone.unregisterForT53ClirInfo(h);
    }

    @Override
    public void registerForT53AudioControlInfo(Handler h, int what, Object obj) {
        this.mActivePhone.registerForT53AudioControlInfo(h, what, obj);
    }

    @Override
    public void unregisterForT53AudioControlInfo(Handler h) {
        this.mActivePhone.unregisterForT53AudioControlInfo(h);
    }

    @Override
    public void registerForRadioOffOrNotAvailable(Handler h, int what, Object obj) {
        this.mActivePhone.registerForRadioOffOrNotAvailable(h, what, obj);
    }

    @Override
    public void unregisterForRadioOffOrNotAvailable(Handler h) {
        this.mActivePhone.unregisterForRadioOffOrNotAvailable(h);
    }

    @Override
    public void setOnEcbModeExitResponse(Handler h, int what, Object obj) {
        this.mActivePhone.setOnEcbModeExitResponse(h, what, obj);
    }

    @Override
    public void unsetOnEcbModeExitResponse(Handler h) {
        this.mActivePhone.unsetOnEcbModeExitResponse(h);
    }

    @Override
    public boolean isCspPlmnEnabled() {
        return this.mActivePhone.isCspPlmnEnabled();
    }

    @Override
    public IsimRecords getIsimRecords() {
        return this.mActivePhone.getIsimRecords();
    }

    @Override
    public int getLteOnCdmaMode() {
        return this.mActivePhone.getLteOnCdmaMode();
    }

    @Override
    public void setVoiceMessageWaiting(int line, int countWaiting) {
        this.mActivePhone.setVoiceMessageWaiting(line, countWaiting);
    }

    @Override
    public UsimServiceTable getUsimServiceTable() {
        return this.mActivePhone.getUsimServiceTable();
    }

    @Override
    public UiccCard getUiccCard() {
        return this.mActivePhone.getUiccCard();
    }

    @Override
    public void nvReadItem(int itemID, Message response) {
        this.mActivePhone.nvReadItem(itemID, response);
    }

    @Override
    public void nvWriteItem(int itemID, String itemValue, Message response) {
        this.mActivePhone.nvWriteItem(itemID, itemValue, response);
    }

    @Override
    public void nvWriteCdmaPrl(byte[] preferredRoamingList, Message response) {
        this.mActivePhone.nvWriteCdmaPrl(preferredRoamingList, response);
    }

    @Override
    public void nvResetConfig(int resetType, Message response) {
        this.mActivePhone.nvResetConfig(resetType, response);
    }

    @Override
    public void dispose() {
        if (this.mActivePhone != null) {
            this.mActivePhone.unregisterForSimRecordsLoaded(this);
        }
        this.mCommandsInterface.unregisterForOn(this);
        this.mCommandsInterface.unregisterForVoiceRadioTechChanged(this);
        this.mCommandsInterface.unregisterForRilConnected(this);
    }

    @Override
    public void removeReferences() {
        this.mActivePhone = null;
        this.mCommandsInterface = null;
    }

    public boolean updateCurrentCarrierInProvider() {
        if (this.mActivePhone instanceof CDMALTEPhone) {
            return ((CDMALTEPhone) this.mActivePhone).updateCurrentCarrierInProvider();
        }
        if (this.mActivePhone instanceof GSMPhone) {
            return ((GSMPhone) this.mActivePhone).updateCurrentCarrierInProvider();
        }
        loge("Phone object is not MultiSim. This should not hit!!!!");
        return false;
    }

    public void updateDataConnectionTracker() {
        logd("Updating Data Connection Tracker");
        if (this.mActivePhone instanceof CDMALTEPhone) {
            ((CDMALTEPhone) this.mActivePhone).updateDataConnectionTracker();
        } else if (this.mActivePhone instanceof GSMPhone) {
            ((GSMPhone) this.mActivePhone).updateDataConnectionTracker();
        } else {
            loge("Phone object is not MultiSim. This should not hit!!!!");
        }
    }

    public void setInternalDataEnabled(boolean enable) {
        setInternalDataEnabled(enable, null);
    }

    public boolean setInternalDataEnabledFlag(boolean enable) {
        if (this.mActivePhone instanceof CDMALTEPhone) {
            boolean flag = ((CDMALTEPhone) this.mActivePhone).setInternalDataEnabledFlag(enable);
            return flag;
        }
        if (this.mActivePhone instanceof GSMPhone) {
            boolean flag2 = ((GSMPhone) this.mActivePhone).setInternalDataEnabledFlag(enable);
            return flag2;
        }
        loge("Phone object is not MultiSim. This should not hit!!!!");
        return false;
    }

    public void setInternalDataEnabled(boolean enable, Message onCompleteMsg) {
        if (this.mActivePhone instanceof CDMALTEPhone) {
            ((CDMALTEPhone) this.mActivePhone).setInternalDataEnabled(enable, onCompleteMsg);
        } else if (this.mActivePhone instanceof GSMPhone) {
            ((GSMPhone) this.mActivePhone).setInternalDataEnabled(enable, onCompleteMsg);
        } else {
            loge("Phone object is not MultiSim. This should not hit!!!!");
        }
    }

    public void registerForAllDataDisconnected(Handler h, int what, Object obj) {
        if (this.mActivePhone instanceof CDMALTEPhone) {
            ((CDMALTEPhone) this.mActivePhone).registerForAllDataDisconnected(h, what, obj);
        } else if (this.mActivePhone instanceof GSMPhone) {
            ((GSMPhone) this.mActivePhone).registerForAllDataDisconnected(h, what, obj);
        } else {
            loge("Phone object is not MultiSim. This should not hit!!!!");
        }
    }

    public void unregisterForAllDataDisconnected(Handler h) {
        if (this.mActivePhone instanceof CDMALTEPhone) {
            ((CDMALTEPhone) this.mActivePhone).unregisterForAllDataDisconnected(h);
        } else if (this.mActivePhone instanceof GSMPhone) {
            ((GSMPhone) this.mActivePhone).unregisterForAllDataDisconnected(h);
        } else {
            loge("Phone object is not MultiSim. This should not hit!!!!");
        }
    }

    @Override
    public int getSubId() {
        return this.mActivePhone.getSubId();
    }

    @Override
    public int getPhoneId() {
        return this.mActivePhone.getPhoneId();
    }

    @Override
    public String[] getPcscfAddress(String apnType) {
        return this.mActivePhone.getPcscfAddress(apnType);
    }

    @Override
    public void setImsRegistrationState(boolean registered) {
        logd("setImsRegistrationState - registered: " + registered);
        this.mActivePhone.setImsRegistrationState(registered);
        if (this.mActivePhone.getPhoneName().equals("GSM")) {
            GSMPhone GP = (GSMPhone) this.mActivePhone;
            GP.getServiceStateTracker().setImsRegistrationState(registered);
        } else if (this.mActivePhone.getPhoneName().equals("CDMA")) {
            CDMAPhone CP = (CDMAPhone) this.mActivePhone;
            CP.getServiceStateTracker().setImsRegistrationState(registered);
        }
    }

    @Override
    public Phone getImsPhone() {
        return this.mActivePhone.getImsPhone();
    }

    @Override
    public ImsPhone relinquishOwnershipOfImsPhone() {
        return null;
    }

    @Override
    public void acquireOwnershipOfImsPhone(ImsPhone imsPhone) {
    }

    @Override
    public int getVoicePhoneServiceState() {
        return this.mActivePhone.getVoicePhoneServiceState();
    }

    @Override
    public boolean setOperatorBrandOverride(String brand) {
        return this.mActivePhone.setOperatorBrandOverride(brand);
    }

    @Override
    public boolean setRoamingOverride(List<String> gsmRoamingList, List<String> gsmNonRoamingList, List<String> cdmaRoamingList, List<String> cdmaNonRoamingList) {
        return this.mActivePhone.setRoamingOverride(gsmRoamingList, gsmNonRoamingList, cdmaRoamingList, cdmaNonRoamingList);
    }

    @Override
    public boolean isRadioAvailable() {
        return this.mCommandsInterface.getRadioState().isAvailable();
    }

    @Override
    public void shutdownRadio() {
        this.mActivePhone.shutdownRadio();
    }

    @Override
    public void setRadioCapability(RadioCapability rc, Message response) {
        this.mActivePhone.setRadioCapability(rc, response);
    }

    @Override
    public int getRadioAccessFamily() {
        return this.mActivePhone.getRadioAccessFamily();
    }

    @Override
    public int getSupportedRadioAccessFamily() {
        return this.mCommandsInterface.getSupportedRadioAccessFamily();
    }

    @Override
    public void registerForRadioCapabilityChanged(Handler h, int what, Object obj) {
        this.mActivePhone.registerForRadioCapabilityChanged(h, what, obj);
    }

    @Override
    public void unregisterForRadioCapabilityChanged(Handler h) {
        this.mActivePhone.unregisterForRadioCapabilityChanged(h);
    }

    public IccCardProxy getPhoneIccCardProxy() {
        return this.mIccCardProxy;
    }

    @Override
    public boolean isImsRegistered() {
        return this.mActivePhone.isImsRegistered();
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        try {
            ((PhoneBase) this.mActivePhone).dump(fd, pw, args);
        } catch (Exception e) {
            e.printStackTrace();
        }
        pw.flush();
        pw.println("++++++++++++++++++++++++++++++++");
        try {
            this.mPhoneSubInfoProxy.dump(fd, pw, args);
        } catch (Exception e2) {
            e2.printStackTrace();
        }
        pw.flush();
        pw.println("++++++++++++++++++++++++++++++++");
        try {
            this.mIccCardProxy.dump(fd, pw, args);
        } catch (Exception e3) {
            e3.printStackTrace();
        }
        pw.flush();
        pw.println("++++++++++++++++++++++++++++++++");
    }

    @Override
    public Connection dialVT(String dialString) throws CallStateException {
        return this.mActivePhone.dialVT(dialString);
    }

    @Override
    public void getSIMLockInfo(Message response) {
        this.mActivePhone.getSIMLockInfo(response);
    }

    @Override
    public void disableDataCall() {
        this.mActivePhone.disableDataCall();
    }

    @Override
    public void enableDataCall() {
        this.mActivePhone.enableDataCall();
    }

    public void suspendDataCallByOtherPhone() {
        ((PhoneBase) this.mActivePhone).suspendDataCallByOtherPhone();
    }

    public void resumeDataCallByOtherPhone() {
        ((PhoneBase) this.mActivePhone).resumeDataCallByOtherPhone();
    }

    @Override
    public void turnOnIms() {
        this.mActivePhone.turnOnIms();
    }

    @Override
    public void turnOffIms() {
        this.mActivePhone.turnOffIms();
    }
}
