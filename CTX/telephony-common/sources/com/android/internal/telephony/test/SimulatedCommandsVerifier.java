package com.android.internal.telephony.test;

import android.net.KeepalivePacketData;
import android.net.LinkProperties;
import android.os.Handler;
import android.os.Message;
import android.service.carrier.CarrierIdentifier;
import android.telephony.ImsiEncryptionInfo;
import android.telephony.NetworkScanRequest;
import android.telephony.data.DataProfile;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.RadioCapability;
import com.android.internal.telephony.UUSInfo;
import com.android.internal.telephony.cdma.CdmaSmsBroadcastConfigInfo;
import com.android.internal.telephony.gsm.SmsBroadcastConfigInfo;
import java.util.List;

public class SimulatedCommandsVerifier implements CommandsInterface {
    private static SimulatedCommandsVerifier sInstance;

    private SimulatedCommandsVerifier() {
    }

    public static SimulatedCommandsVerifier getInstance() {
        if (sInstance == null) {
            sInstance = new SimulatedCommandsVerifier();
        }
        return sInstance;
    }

    @Override
    public CommandsInterface.RadioState getRadioState() {
        return null;
    }

    @Override
    public void getImsRegistrationState(Message message) {
    }

    @Override
    public void registerForRadioStateChanged(Handler handler, int i, Object obj) {
    }

    @Override
    public void unregisterForRadioStateChanged(Handler handler) {
    }

    @Override
    public void registerForVoiceRadioTechChanged(Handler handler, int i, Object obj) {
    }

    @Override
    public void unregisterForVoiceRadioTechChanged(Handler handler) {
    }

    @Override
    public void registerForImsNetworkStateChanged(Handler handler, int i, Object obj) {
    }

    @Override
    public void unregisterForImsNetworkStateChanged(Handler handler) {
    }

    @Override
    public void registerForOn(Handler handler, int i, Object obj) {
    }

    @Override
    public void unregisterForOn(Handler handler) {
    }

    @Override
    public void registerForAvailable(Handler handler, int i, Object obj) {
    }

    @Override
    public void unregisterForAvailable(Handler handler) {
    }

    @Override
    public void registerForNotAvailable(Handler handler, int i, Object obj) {
    }

    @Override
    public void unregisterForNotAvailable(Handler handler) {
    }

    @Override
    public void registerForOffOrNotAvailable(Handler handler, int i, Object obj) {
    }

    @Override
    public void unregisterForOffOrNotAvailable(Handler handler) {
    }

    @Override
    public void registerForIccStatusChanged(Handler handler, int i, Object obj) {
    }

    @Override
    public void unregisterForIccStatusChanged(Handler handler) {
    }

    @Override
    public void registerForIccSlotStatusChanged(Handler handler, int i, Object obj) {
    }

    @Override
    public void unregisterForIccSlotStatusChanged(Handler handler) {
    }

    @Override
    public void registerForCallStateChanged(Handler handler, int i, Object obj) {
    }

    @Override
    public void unregisterForCallStateChanged(Handler handler) {
    }

    @Override
    public void registerForNetworkStateChanged(Handler handler, int i, Object obj) {
    }

    @Override
    public void unregisterForNetworkStateChanged(Handler handler) {
    }

    @Override
    public void registerForDataCallListChanged(Handler handler, int i, Object obj) {
    }

    @Override
    public void unregisterForDataCallListChanged(Handler handler) {
    }

    @Override
    public void registerForInCallVoicePrivacyOn(Handler handler, int i, Object obj) {
    }

    @Override
    public void unregisterForInCallVoicePrivacyOn(Handler handler) {
    }

    @Override
    public void registerForInCallVoicePrivacyOff(Handler handler, int i, Object obj) {
    }

    @Override
    public void unregisterForInCallVoicePrivacyOff(Handler handler) {
    }

    @Override
    public void registerForSrvccStateChanged(Handler handler, int i, Object obj) {
    }

    @Override
    public void unregisterForSrvccStateChanged(Handler handler) {
    }

    @Override
    public void registerForSubscriptionStatusChanged(Handler handler, int i, Object obj) {
    }

    @Override
    public void unregisterForSubscriptionStatusChanged(Handler handler) {
    }

    @Override
    public void registerForHardwareConfigChanged(Handler handler, int i, Object obj) {
    }

    @Override
    public void unregisterForHardwareConfigChanged(Handler handler) {
    }

    @Override
    public void setOnNewGsmSms(Handler handler, int i, Object obj) {
    }

    @Override
    public void unSetOnNewGsmSms(Handler handler) {
    }

    @Override
    public void setOnNewCdmaSms(Handler handler, int i, Object obj) {
    }

    @Override
    public void unSetOnNewCdmaSms(Handler handler) {
    }

    @Override
    public void setOnNewGsmBroadcastSms(Handler handler, int i, Object obj) {
    }

    @Override
    public void unSetOnNewGsmBroadcastSms(Handler handler) {
    }

    @Override
    public void setOnSmsOnSim(Handler handler, int i, Object obj) {
    }

    @Override
    public void unSetOnSmsOnSim(Handler handler) {
    }

    @Override
    public void setOnSmsStatus(Handler handler, int i, Object obj) {
    }

    @Override
    public void unSetOnSmsStatus(Handler handler) {
    }

    @Override
    public void setOnNITZTime(Handler handler, int i, Object obj) {
    }

    @Override
    public void unSetOnNITZTime(Handler handler) {
    }

    @Override
    public void setOnUSSD(Handler handler, int i, Object obj) {
    }

    @Override
    public void unSetOnUSSD(Handler handler) {
    }

    @Override
    public void setOnSignalStrengthUpdate(Handler handler, int i, Object obj) {
    }

    @Override
    public void unSetOnSignalStrengthUpdate(Handler handler) {
    }

    @Override
    public void setOnIccSmsFull(Handler handler, int i, Object obj) {
    }

    @Override
    public void unSetOnIccSmsFull(Handler handler) {
    }

    @Override
    public void registerForIccRefresh(Handler handler, int i, Object obj) {
    }

    @Override
    public void unregisterForIccRefresh(Handler handler) {
    }

    @Override
    public void setOnIccRefresh(Handler handler, int i, Object obj) {
    }

    @Override
    public void unsetOnIccRefresh(Handler handler) {
    }

    @Override
    public void setOnCallRing(Handler handler, int i, Object obj) {
    }

    @Override
    public void unSetOnCallRing(Handler handler) {
    }

    @Override
    public void setOnRestrictedStateChanged(Handler handler, int i, Object obj) {
    }

    @Override
    public void unSetOnRestrictedStateChanged(Handler handler) {
    }

    @Override
    public void setOnSuppServiceNotification(Handler handler, int i, Object obj) {
    }

    @Override
    public void unSetOnSuppServiceNotification(Handler handler) {
    }

    @Override
    public void setOnCatSessionEnd(Handler handler, int i, Object obj) {
    }

    @Override
    public void unSetOnCatSessionEnd(Handler handler) {
    }

    @Override
    public void setOnCatProactiveCmd(Handler handler, int i, Object obj) {
    }

    @Override
    public void unSetOnCatProactiveCmd(Handler handler) {
    }

    @Override
    public void setOnCatEvent(Handler handler, int i, Object obj) {
    }

    @Override
    public void unSetOnCatEvent(Handler handler) {
    }

    @Override
    public void setOnCatCallSetUp(Handler handler, int i, Object obj) {
    }

    @Override
    public void unSetOnCatCallSetUp(Handler handler) {
    }

    @Override
    public void setSuppServiceNotifications(boolean z, Message message) {
    }

    @Override
    public void setOnCatCcAlphaNotify(Handler handler, int i, Object obj) {
    }

    @Override
    public void unSetOnCatCcAlphaNotify(Handler handler) {
    }

    @Override
    public void setOnSs(Handler handler, int i, Object obj) {
    }

    @Override
    public void unSetOnSs(Handler handler) {
    }

    @Override
    public void registerForDisplayInfo(Handler handler, int i, Object obj) {
    }

    @Override
    public void unregisterForDisplayInfo(Handler handler) {
    }

    @Override
    public void registerForCallWaitingInfo(Handler handler, int i, Object obj) {
    }

    @Override
    public void unregisterForCallWaitingInfo(Handler handler) {
    }

    @Override
    public void registerForSignalInfo(Handler handler, int i, Object obj) {
    }

    @Override
    public void unregisterForSignalInfo(Handler handler) {
    }

    @Override
    public void registerForNumberInfo(Handler handler, int i, Object obj) {
    }

    @Override
    public void unregisterForNumberInfo(Handler handler) {
    }

    @Override
    public void registerForRedirectedNumberInfo(Handler handler, int i, Object obj) {
    }

    @Override
    public void unregisterForRedirectedNumberInfo(Handler handler) {
    }

    @Override
    public void registerForLineControlInfo(Handler handler, int i, Object obj) {
    }

    @Override
    public void unregisterForLineControlInfo(Handler handler) {
    }

    @Override
    public void registerFoT53ClirlInfo(Handler handler, int i, Object obj) {
    }

    @Override
    public void unregisterForT53ClirInfo(Handler handler) {
    }

    @Override
    public void registerForT53AudioControlInfo(Handler handler, int i, Object obj) {
    }

    @Override
    public void unregisterForT53AudioControlInfo(Handler handler) {
    }

    @Override
    public void setEmergencyCallbackMode(Handler handler, int i, Object obj) {
    }

    @Override
    public void registerForCdmaOtaProvision(Handler handler, int i, Object obj) {
    }

    @Override
    public void unregisterForCdmaOtaProvision(Handler handler) {
    }

    @Override
    public void registerForRingbackTone(Handler handler, int i, Object obj) {
    }

    @Override
    public void unregisterForRingbackTone(Handler handler) {
    }

    @Override
    public void registerForResendIncallMute(Handler handler, int i, Object obj) {
    }

    @Override
    public void unregisterForResendIncallMute(Handler handler) {
    }

    @Override
    public void registerForCdmaSubscriptionChanged(Handler handler, int i, Object obj) {
    }

    @Override
    public void unregisterForCdmaSubscriptionChanged(Handler handler) {
    }

    @Override
    public void registerForCdmaPrlChanged(Handler handler, int i, Object obj) {
    }

    @Override
    public void unregisterForCdmaPrlChanged(Handler handler) {
    }

    @Override
    public void registerForExitEmergencyCallbackMode(Handler handler, int i, Object obj) {
    }

    @Override
    public void unregisterForExitEmergencyCallbackMode(Handler handler) {
    }

    @Override
    public void registerForRilConnected(Handler handler, int i, Object obj) {
    }

    @Override
    public void unregisterForRilConnected(Handler handler) {
    }

    @Override
    public void supplyIccPin(String str, Message message) {
    }

    @Override
    public void supplyIccPinForApp(String str, String str2, Message message) {
    }

    @Override
    public void supplyIccPuk(String str, String str2, Message message) {
    }

    @Override
    public void supplyIccPukForApp(String str, String str2, String str3, Message message) {
    }

    @Override
    public void supplyIccPin2(String str, Message message) {
    }

    @Override
    public void supplyIccPin2ForApp(String str, String str2, Message message) {
    }

    @Override
    public void supplyIccPuk2(String str, String str2, Message message) {
    }

    @Override
    public void supplyIccPuk2ForApp(String str, String str2, String str3, Message message) {
    }

    @Override
    public void changeIccPin(String str, String str2, Message message) {
    }

    @Override
    public void changeIccPinForApp(String str, String str2, String str3, Message message) {
    }

    @Override
    public void changeIccPin2(String str, String str2, Message message) {
    }

    @Override
    public void changeIccPin2ForApp(String str, String str2, String str3, Message message) {
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
    public void setRadioPower(boolean z, Message message) {
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
    public void queryCLIP(Message message) {
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
    public void setCallForward(int i, int i2, int i3, String str, int i4, Message message) {
    }

    @Override
    public void queryCallForwardStatus(int i, int i2, String str, Message message) {
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
    public void setBandMode(int i, Message message) {
    }

    @Override
    public void queryAvailableBandMode(Message message) {
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
    public void invokeOemRilRequestRaw(byte[] bArr, Message message) {
    }

    @Override
    public void invokeOemRilRequestStrings(String[] strArr, Message message) {
    }

    @Override
    public void setOnUnsolOemHookRaw(Handler handler, int i, Object obj) {
    }

    @Override
    public void unSetOnUnsolOemHookRaw(Handler handler) {
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
    public void setGsmBroadcastActivation(boolean z, Message message) {
    }

    @Override
    public void setGsmBroadcastConfig(SmsBroadcastConfigInfo[] smsBroadcastConfigInfoArr, Message message) {
    }

    @Override
    public void getGsmBroadcastConfig(Message message) {
    }

    @Override
    public void getDeviceIdentity(Message message) {
    }

    @Override
    public void getCDMASubscription(Message message) {
    }

    @Override
    public void sendCDMAFeatureCode(String str, Message message) {
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
    public void getCdmaSubscriptionSource(Message message) {
    }

    @Override
    public void setTTYMode(int i, Message message) {
    }

    @Override
    public void queryTTYMode(Message message) {
    }

    @Override
    public void setupDataCall(int i, DataProfile dataProfile, boolean z, boolean z2, int i2, LinkProperties linkProperties, Message message) {
    }

    @Override
    public void deactivateDataCall(int i, int i2, Message message) {
    }

    @Override
    public void setCdmaBroadcastActivation(boolean z, Message message) {
    }

    @Override
    public void setCdmaBroadcastConfig(CdmaSmsBroadcastConfigInfo[] cdmaSmsBroadcastConfigInfoArr, Message message) {
    }

    @Override
    public void getCdmaBroadcastConfig(Message message) {
    }

    @Override
    public void exitEmergencyCallbackMode(Message message) {
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
    public int getLteOnCdmaMode() {
        return 0;
    }

    @Override
    public void requestIccSimAuthentication(int i, String str, String str2, Message message) {
    }

    @Override
    public void getVoiceRadioTechnology(Message message) {
    }

    @Override
    public void registerForCellInfoList(Handler handler, int i, Object obj) {
    }

    @Override
    public void unregisterForCellInfoList(Handler handler) {
    }

    @Override
    public void registerForPhysicalChannelConfiguration(Handler handler, int i, Object obj) {
    }

    @Override
    public void unregisterForPhysicalChannelConfiguration(Handler handler) {
    }

    @Override
    public void setInitialAttachApn(DataProfile dataProfile, boolean z, Message message) {
    }

    @Override
    public void setDataProfile(DataProfile[] dataProfileArr, boolean z, Message message) {
    }

    @Override
    public void testingEmergencyCall() {
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
    public int getRilVersion() {
        return 0;
    }

    @Override
    public void setUiccSubscription(int i, int i2, int i3, int i4, Message message) {
    }

    @Override
    public void setDataAllowed(boolean z, Message message) {
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
    public void registerForRadioCapabilityChanged(Handler handler, int i, Object obj) {
    }

    @Override
    public void unregisterForRadioCapabilityChanged(Handler handler) {
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
    public void registerForLceInfo(Handler handler, int i, Object obj) {
    }

    @Override
    public void unregisterForLceInfo(Handler handler) {
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
    public void registerForPcoData(Handler handler, int i, Object obj) {
    }

    @Override
    public void unregisterForPcoData(Handler handler) {
    }

    @Override
    public void registerForModemReset(Handler handler, int i, Object obj) {
    }

    @Override
    public void unregisterForModemReset(Handler handler) {
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
    public void registerForCarrierInfoForImsiEncryption(Handler handler, int i, Object obj) {
    }

    @Override
    public void registerForNetworkScanResult(Handler handler, int i, Object obj) {
    }

    @Override
    public void unregisterForNetworkScanResult(Handler handler) {
    }

    @Override
    public void unregisterForCarrierInfoForImsiEncryption(Handler handler) {
    }

    @Override
    public void registerForNattKeepaliveStatus(Handler handler, int i, Object obj) {
    }

    @Override
    public void unregisterForNattKeepaliveStatus(Handler handler) {
    }

    @Override
    public void startNattKeepalive(int i, KeepalivePacketData keepalivePacketData, int i2, Message message) {
    }

    @Override
    public void stopNattKeepalive(int i, Message message) {
    }
}
