package com.android.internal.telephony.imsphone;

import android.content.Context;
import android.net.KeepalivePacketData;
import android.net.LinkProperties;
import android.os.Handler;
import android.os.Message;
import android.service.carrier.CarrierIdentifier;
import android.telephony.ImsiEncryptionInfo;
import android.telephony.NetworkScanRequest;
import android.telephony.data.DataProfile;
import com.android.internal.telephony.BaseCommands;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.RadioCapability;
import com.android.internal.telephony.UUSInfo;
import com.android.internal.telephony.cdma.CdmaSmsBroadcastConfigInfo;
import com.android.internal.telephony.gsm.SmsBroadcastConfigInfo;
import java.util.List;

class ImsPhoneCommandInterface extends BaseCommands implements CommandsInterface {
    ImsPhoneCommandInterface(Context context) {
        super(context);
    }

    @Override
    public void setOnNITZTime(Handler handler, int i, Object obj) {
    }

    @Override
    public void getIccCardStatus(Message message) {
    }

    @Override
    public void getIccSlotsStatus(Message message) {
    }

    @Override
    public void setLogicalToPhysicalSlotMapping(int[] iArr, Message message) {
    }

    @Override
    public void supplyIccPin(String str, Message message) {
    }

    @Override
    public void supplyIccPuk(String str, String str2, Message message) {
    }

    @Override
    public void supplyIccPin2(String str, Message message) {
    }

    @Override
    public void supplyIccPuk2(String str, String str2, Message message) {
    }

    @Override
    public void changeIccPin(String str, String str2, Message message) {
    }

    @Override
    public void changeIccPin2(String str, String str2, Message message) {
    }

    @Override
    public void changeBarringPassword(String str, String str2, String str3, Message message) {
    }

    @Override
    public void supplyNetworkDepersonalization(String str, Message message) {
    }

    @Override
    public void getCurrentCalls(Message message) {
    }

    @Override
    @Deprecated
    public void getPDPContextList(Message message) {
    }

    @Override
    public void getDataCallList(Message message) {
    }

    @Override
    public void dial(String str, int i, Message message) {
    }

    @Override
    public void dial(String str, int i, UUSInfo uUSInfo, Message message) {
    }

    @Override
    public void getIMSI(Message message) {
    }

    @Override
    public void getIMSIForApp(String str, Message message) {
    }

    @Override
    public void getIMEI(Message message) {
    }

    @Override
    public void getIMEISV(Message message) {
    }

    @Override
    public void hangupConnection(int i, Message message) {
    }

    @Override
    public void hangupWaitingOrBackground(Message message) {
    }

    @Override
    public void hangupForegroundResumeBackground(Message message) {
    }

    @Override
    public void switchWaitingOrHoldingAndActive(Message message) {
    }

    @Override
    public void conference(Message message) {
    }

    @Override
    public void setPreferredVoicePrivacy(boolean z, Message message) {
    }

    @Override
    public void getPreferredVoicePrivacy(Message message) {
    }

    @Override
    public void separateConnection(int i, Message message) {
    }

    @Override
    public void acceptCall(Message message) {
    }

    @Override
    public void rejectCall(Message message) {
    }

    @Override
    public void explicitCallTransfer(Message message) {
    }

    @Override
    public void getLastCallFailCause(Message message) {
    }

    @Override
    @Deprecated
    public void getLastPdpFailCause(Message message) {
    }

    @Override
    public void getLastDataCallFailCause(Message message) {
    }

    @Override
    public void setMute(boolean z, Message message) {
    }

    @Override
    public void getMute(Message message) {
    }

    @Override
    public void getSignalStrength(Message message) {
    }

    @Override
    public void getVoiceRegistrationState(Message message) {
    }

    @Override
    public void getDataRegistrationState(Message message) {
    }

    @Override
    public void getOperator(Message message) {
    }

    @Override
    public void sendDtmf(char c, Message message) {
    }

    @Override
    public void startDtmf(char c, Message message) {
    }

    @Override
    public void stopDtmf(Message message) {
    }

    @Override
    public void sendBurstDtmf(String str, int i, int i2, Message message) {
    }

    @Override
    public void sendSMS(String str, String str2, Message message) {
    }

    @Override
    public void sendSMSExpectMore(String str, String str2, Message message) {
    }

    @Override
    public void sendCdmaSms(byte[] bArr, Message message) {
    }

    @Override
    public void sendImsGsmSms(String str, String str2, int i, int i2, Message message) {
    }

    @Override
    public void sendImsCdmaSms(byte[] bArr, int i, int i2, Message message) {
    }

    @Override
    public void getImsRegistrationState(Message message) {
    }

    @Override
    public void deleteSmsOnSim(int i, Message message) {
    }

    @Override
    public void deleteSmsOnRuim(int i, Message message) {
    }

    @Override
    public void writeSmsToSim(int i, String str, String str2, Message message) {
    }

    @Override
    public void writeSmsToRuim(int i, String str, Message message) {
    }

    @Override
    public void setupDataCall(int i, DataProfile dataProfile, boolean z, boolean z2, int i2, LinkProperties linkProperties, Message message) {
    }

    @Override
    public void deactivateDataCall(int i, int i2, Message message) {
    }

    @Override
    public void setRadioPower(boolean z, Message message) {
    }

    @Override
    public void setSuppServiceNotifications(boolean z, Message message) {
    }

    @Override
    public void acknowledgeLastIncomingGsmSms(boolean z, int i, Message message) {
    }

    @Override
    public void acknowledgeLastIncomingCdmaSms(boolean z, int i, Message message) {
    }

    @Override
    public void acknowledgeIncomingGsmSmsWithPdu(boolean z, String str, Message message) {
    }

    @Override
    public void iccIO(int i, int i2, String str, int i3, int i4, int i5, String str2, String str3, Message message) {
    }

    @Override
    public void iccIOForApp(int i, int i2, String str, int i3, int i4, int i5, String str2, String str3, String str4, Message message) {
    }

    @Override
    public void getCLIR(Message message) {
    }

    @Override
    public void setCLIR(int i, Message message) {
    }

    @Override
    public void queryCallWaiting(int i, Message message) {
    }

    @Override
    public void setCallWaiting(boolean z, int i, Message message) {
    }

    @Override
    public void setNetworkSelectionModeAutomatic(Message message) {
    }

    @Override
    public void setNetworkSelectionModeManual(String str, Message message) {
    }

    @Override
    public void getNetworkSelectionMode(Message message) {
    }

    @Override
    public void getAvailableNetworks(Message message) {
    }

    @Override
    public void startNetworkScan(NetworkScanRequest networkScanRequest, Message message) {
    }

    @Override
    public void stopNetworkScan(Message message) {
    }

    @Override
    public void setCallForward(int i, int i2, int i3, String str, int i4, Message message) {
    }

    @Override
    public void queryCallForwardStatus(int i, int i2, String str, Message message) {
    }

    @Override
    public void queryCLIP(Message message) {
    }

    @Override
    public void getBasebandVersion(Message message) {
    }

    @Override
    public void queryFacilityLock(String str, String str2, int i, Message message) {
    }

    @Override
    public void queryFacilityLockForApp(String str, String str2, int i, String str3, Message message) {
    }

    @Override
    public void setFacilityLock(String str, boolean z, String str2, int i, Message message) {
    }

    @Override
    public void setFacilityLockForApp(String str, boolean z, String str2, int i, String str3, Message message) {
    }

    @Override
    public void sendUSSD(String str, Message message) {
    }

    @Override
    public void cancelPendingUssd(Message message) {
    }

    @Override
    public void resetRadio(Message message) {
    }

    @Override
    public void invokeOemRilRequestRaw(byte[] bArr, Message message) {
    }

    @Override
    public void invokeOemRilRequestStrings(String[] strArr, Message message) {
    }

    @Override
    public void setBandMode(int i, Message message) {
    }

    @Override
    public void queryAvailableBandMode(Message message) {
    }

    @Override
    public void sendTerminalResponse(String str, Message message) {
    }

    @Override
    public void sendEnvelope(String str, Message message) {
    }

    @Override
    public void sendEnvelopeWithStatus(String str, Message message) {
    }

    @Override
    public void handleCallSetupRequestFromSim(boolean z, Message message) {
    }

    @Override
    public void setPreferredNetworkType(int i, Message message) {
    }

    @Override
    public void getPreferredNetworkType(Message message) {
    }

    @Override
    public void setLocationUpdates(boolean z, Message message) {
    }

    @Override
    public void getSmscAddress(Message message) {
    }

    @Override
    public void setSmscAddress(String str, Message message) {
    }

    @Override
    public void reportSmsMemoryStatus(boolean z, Message message) {
    }

    @Override
    public void reportStkServiceIsRunning(Message message) {
    }

    @Override
    public void getCdmaSubscriptionSource(Message message) {
    }

    @Override
    public void getGsmBroadcastConfig(Message message) {
    }

    @Override
    public void setGsmBroadcastConfig(SmsBroadcastConfigInfo[] smsBroadcastConfigInfoArr, Message message) {
    }

    @Override
    public void setGsmBroadcastActivation(boolean z, Message message) {
    }

    @Override
    public void getDeviceIdentity(Message message) {
    }

    @Override
    public void getCDMASubscription(Message message) {
    }

    @Override
    public void setPhoneType(int i) {
    }

    @Override
    public void queryCdmaRoamingPreference(Message message) {
    }

    @Override
    public void setCdmaRoamingPreference(int i, Message message) {
    }

    @Override
    public void setCdmaSubscriptionSource(int i, Message message) {
    }

    @Override
    public void queryTTYMode(Message message) {
    }

    @Override
    public void setTTYMode(int i, Message message) {
    }

    @Override
    public void sendCDMAFeatureCode(String str, Message message) {
    }

    @Override
    public void getCdmaBroadcastConfig(Message message) {
    }

    @Override
    public void setCdmaBroadcastConfig(CdmaSmsBroadcastConfigInfo[] cdmaSmsBroadcastConfigInfoArr, Message message) {
    }

    @Override
    public void setCdmaBroadcastActivation(boolean z, Message message) {
    }

    @Override
    public void exitEmergencyCallbackMode(Message message) {
    }

    @Override
    public void supplyIccPinForApp(String str, String str2, Message message) {
    }

    @Override
    public void supplyIccPukForApp(String str, String str2, String str3, Message message) {
    }

    @Override
    public void supplyIccPin2ForApp(String str, String str2, Message message) {
    }

    @Override
    public void supplyIccPuk2ForApp(String str, String str2, String str3, Message message) {
    }

    @Override
    public void changeIccPinForApp(String str, String str2, String str3, Message message) {
    }

    @Override
    public void changeIccPin2ForApp(String str, String str2, String str3, Message message) {
    }

    @Override
    public void requestIccSimAuthentication(int i, String str, String str2, Message message) {
    }

    @Override
    public void getVoiceRadioTechnology(Message message) {
    }

    @Override
    public void setInitialAttachApn(DataProfile dataProfile, boolean z, Message message) {
    }

    @Override
    public void setDataProfile(DataProfile[] dataProfileArr, boolean z, Message message) {
    }

    @Override
    public void iccOpenLogicalChannel(String str, int i, Message message) {
    }

    @Override
    public void iccCloseLogicalChannel(int i, Message message) {
    }

    @Override
    public void iccTransmitApduLogicalChannel(int i, int i2, int i3, int i4, int i5, int i6, String str, Message message) {
    }

    @Override
    public void iccTransmitApduBasicChannel(int i, int i2, int i3, int i4, int i5, String str, Message message) {
    }

    @Override
    public void nvReadItem(int i, Message message) {
    }

    @Override
    public void nvWriteItem(int i, String str, Message message) {
    }

    @Override
    public void nvWriteCdmaPrl(byte[] bArr, Message message) {
    }

    @Override
    public void nvResetConfig(int i, Message message) {
    }

    @Override
    public void getHardwareConfig(Message message) {
    }

    @Override
    public void requestShutdown(Message message) {
    }

    @Override
    public void setRadioCapability(RadioCapability radioCapability, Message message) {
    }

    @Override
    public void getRadioCapability(Message message) {
    }

    @Override
    public void startLceService(int i, boolean z, Message message) {
    }

    @Override
    public void stopLceService(Message message) {
    }

    @Override
    public void pullLceData(Message message) {
    }

    @Override
    public void getModemActivityInfo(Message message) {
    }

    @Override
    public void setCarrierInfoForImsiEncryption(ImsiEncryptionInfo imsiEncryptionInfo, Message message) {
    }

    @Override
    public void setAllowedCarriers(List<CarrierIdentifier> list, Message message) {
    }

    @Override
    public void getAllowedCarriers(Message message) {
    }

    @Override
    public void sendDeviceState(int i, boolean z, Message message) {
    }

    @Override
    public void setUnsolResponseFilter(int i, Message message) {
    }

    @Override
    public void setSignalStrengthReportingCriteria(int i, int i2, int[] iArr, int i3, Message message) {
    }

    @Override
    public void setLinkCapacityReportingCriteria(int i, int i2, int i3, int[] iArr, int[] iArr2, int i4, Message message) {
    }

    @Override
    public void setSimCardPower(int i, Message message) {
    }

    @Override
    public void startNattKeepalive(int i, KeepalivePacketData keepalivePacketData, int i2, Message message) {
    }

    @Override
    public void stopNattKeepalive(int i, Message message) {
    }
}
