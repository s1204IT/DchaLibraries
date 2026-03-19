package com.mediatek.internal.telephony;

import android.hardware.radio.V1_0.CdmaCallWaiting;
import android.hardware.radio.V1_0.CdmaInformationRecords;
import android.hardware.radio.V1_0.CdmaSignalInfoRecord;
import android.hardware.radio.V1_0.CdmaSmsMessage;
import android.hardware.radio.V1_0.CellInfo;
import android.hardware.radio.V1_0.HardwareConfig;
import android.hardware.radio.V1_0.LceDataInfo;
import android.hardware.radio.V1_0.PcoDataInfo;
import android.hardware.radio.V1_0.RadioCapability;
import android.hardware.radio.V1_0.SetupDataCallResult;
import android.hardware.radio.V1_0.SignalStrength;
import android.hardware.radio.V1_0.SimRefreshResult;
import android.hardware.radio.V1_0.StkCcUnsolSsResult;
import android.hardware.radio.V1_0.SuppSvcNotification;
import android.hardware.radio.V1_1.KeepaliveStatus;
import android.hardware.radio.V1_1.NetworkScanResult;
import android.hardware.radio.V1_2.LinkCapacityEstimate;
import android.hardware.radio.V1_2.PhysicalChannelConfig;
import com.android.internal.telephony.RIL;
import java.util.ArrayList;
import vendor.mediatek.hardware.radio.V3_0.CfuStatusNotification;
import vendor.mediatek.hardware.radio.V3_0.CipherNotification;
import vendor.mediatek.hardware.radio.V3_0.CrssNotification;
import vendor.mediatek.hardware.radio.V3_0.DedicateDataCall;
import vendor.mediatek.hardware.radio.V3_0.EtwsNotification;
import vendor.mediatek.hardware.radio.V3_0.IRadioIndication;
import vendor.mediatek.hardware.radio.V3_0.IncomingCallNotification;
import vendor.mediatek.hardware.radio.V3_0.PcoDataAttachedInfo;
import vendor.mediatek.hardware.radio.V3_0.SignalStrengthWithWcdmaEcio;
import vendor.mediatek.hardware.radio.V3_0.VsimOperationEvent;

public class MtkRadioIndicationBase extends IRadioIndication.Stub {
    MtkRadioIndicationBase(RIL ril) {
    }

    public void radioStateChanged(int i, int i2) {
    }

    public void callStateChanged(int i) {
    }

    public void networkStateChanged(int i) {
    }

    public void newSms(int i, ArrayList<Byte> arrayList) {
    }

    public void newSmsStatusReport(int i, ArrayList<Byte> arrayList) {
    }

    public void newSmsOnSim(int i, int i2) {
    }

    public void onUssd(int i, int i2, String str) {
    }

    public void nitzTimeReceived(int i, String str, long j) {
    }

    public void currentSignalStrength(int i, SignalStrength signalStrength) {
    }

    public void currentLinkCapacityEstimate(int i, LinkCapacityEstimate linkCapacityEstimate) {
    }

    public void currentSignalStrength_1_2(int i, android.hardware.radio.V1_2.SignalStrength signalStrength) {
    }

    public void currentSignalStrengthWithWcdmaEcioInd(int i, SignalStrengthWithWcdmaEcio signalStrengthWithWcdmaEcio) {
    }

    public void currentPhysicalChannelConfigs(int i, ArrayList<PhysicalChannelConfig> arrayList) {
    }

    public void dataCallListChanged(int i, ArrayList<SetupDataCallResult> arrayList) {
    }

    public void suppSvcNotify(int i, SuppSvcNotification suppSvcNotification) {
    }

    public void cfuStatusNotify(int i, CfuStatusNotification cfuStatusNotification) {
    }

    public void incomingCallIndication(int i, IncomingCallNotification incomingCallNotification) {
    }

    public void cipherIndication(int i, CipherNotification cipherNotification) {
    }

    public void crssIndication(int i, CrssNotification crssNotification) {
    }

    public void speechCodecInfoIndication(int i, int i2) {
    }

    public void cdmaCallAccepted(int i) {
    }

    public void eccNumIndication(int i, String str, String str2) {
    }

    public void stkSessionEnd(int i) {
    }

    public void stkProactiveCommand(int i, String str) {
    }

    public void stkEventNotify(int i, String str) {
    }

    public void stkCallSetup(int i, long j) {
    }

    public void simSmsStorageFull(int i) {
    }

    public void simRefresh(int i, SimRefreshResult simRefreshResult) {
    }

    public void callRing(int i, boolean z, CdmaSignalInfoRecord cdmaSignalInfoRecord) {
    }

    public void simStatusChanged(int i) {
    }

    public void cdmaNewSms(int i, CdmaSmsMessage cdmaSmsMessage) {
    }

    public void newBroadcastSms(int i, ArrayList<Byte> arrayList) {
    }

    public void cdmaRuimSmsStorageFull(int i) {
    }

    public void restrictedStateChanged(int i, int i2) {
    }

    public void enterEmergencyCallbackMode(int i) {
    }

    public void cdmaCallWaiting(int i, CdmaCallWaiting cdmaCallWaiting) {
    }

    public void cdmaOtaProvisionStatus(int i, int i2) {
    }

    public void cdmaInfoRec(int i, CdmaInformationRecords cdmaInformationRecords) {
    }

    public void indicateRingbackTone(int i, boolean z) {
    }

    public void resendIncallMute(int i) {
    }

    public void cdmaSubscriptionSourceChanged(int i, int i2) {
    }

    public void cdmaPrlChanged(int i, int i2) {
    }

    public void exitEmergencyCallbackMode(int i) {
    }

    public void rilConnected(int i) {
    }

    public void voiceRadioTechChanged(int i, int i2) {
    }

    public void cellInfoList(int i, ArrayList<CellInfo> arrayList) {
    }

    public void cellInfoList_1_2(int i, ArrayList<android.hardware.radio.V1_2.CellInfo> arrayList) {
    }

    public void networkScanResult(int i, NetworkScanResult networkScanResult) {
    }

    public void networkScanResult_1_2(int i, android.hardware.radio.V1_2.NetworkScanResult networkScanResult) {
    }

    public void imsNetworkStateChanged(int i) {
    }

    public void subscriptionStatusChanged(int i, boolean z) {
    }

    public void srvccStateNotify(int i, int i2) {
    }

    public void hardwareConfigChanged(int i, ArrayList<HardwareConfig> arrayList) {
    }

    public void radioCapabilityIndication(int i, RadioCapability radioCapability) {
    }

    public void onSupplementaryServiceIndication(int i, StkCcUnsolSsResult stkCcUnsolSsResult) {
    }

    public void stkCallControlAlphaNotify(int i, String str) {
    }

    public void lceData(int i, LceDataInfo lceDataInfo) {
    }

    public void pcoData(int i, PcoDataInfo pcoDataInfo) {
    }

    public void modemReset(int i, String str) {
    }

    @Override
    public void carrierInfoForImsiEncryption(int i) {
    }

    @Override
    public void keepaliveStatus(int i, KeepaliveStatus keepaliveStatus) {
    }

    public void responseCsNetworkStateChangeInd(int i, ArrayList<String> arrayList) {
    }

    public void responsePsNetworkStateChangeInd(int i, ArrayList<Integer> arrayList) {
    }

    public void responseNetworkEventInd(int i, ArrayList<Integer> arrayList) {
    }

    public void responseModulationInfoInd(int i, ArrayList<Integer> arrayList) {
    }

    public void responseInvalidSimInd(int i, ArrayList<String> arrayList) {
    }

    public void responseFemtocellInfo(int i, ArrayList<String> arrayList) {
    }

    public void responseLteNetworkInfo(int i, int i2) {
    }

    public void onMccMncChanged(int i, String str) {
    }

    public void onVirtualSimOn(int i, int i2) {
    }

    public void onVirtualSimOff(int i, int i2) {
    }

    public void onImeiLock(int i) {
    }

    public void onImsiRefreshDone(int i) {
    }

    public void onCardDetectedInd(int i) {
    }

    public void newEtwsInd(int i, EtwsNotification etwsNotification) {
    }

    public void meSmsStorageFullInd(int i) {
    }

    public void smsReadyInd(int i) {
    }

    public void dataAllowedNotification(int i, int i2) {
    }

    public void onPseudoCellInfoInd(int i, ArrayList<Integer> arrayList) {
    }

    public void eMBMSSessionStatusIndication(int i, int i2) {
    }

    public void eMBMSAtInfoIndication(int i, String str) {
    }

    public void plmnChangedIndication(int i, ArrayList<String> arrayList) {
    }

    public void registrationSuspendedIndication(int i, ArrayList<Integer> arrayList) {
    }

    public void gmssRatChangedIndication(int i, ArrayList<Integer> arrayList) {
    }

    public void worldModeChangedIndication(int i, ArrayList<Integer> arrayList) {
    }

    public void resetAttachApnInd(int i) {
    }

    public void mdChangedApnInd(int i, int i2) {
    }

    public void esnMeidChangeInd(int i, String str) {
    }

    public void phbReadyNotification(int i, int i2) {
    }

    public void bipProactiveCommand(int i, String str) {
    }

    public void triggerOtaSP(int i) {
    }

    public void onStkMenuReset(int i) {
    }

    public void onMdDataRetryCountReset(int i) {
    }

    public void onRemoveRestrictEutran(int i) {
    }

    @Override
    public void onPcoStatus(int i, ArrayList<Integer> arrayList) {
    }

    public void onLteAccessStratumStateChanged(int i, ArrayList<Integer> arrayList) {
    }

    public void onSimPlugIn(int i) {
    }

    public void onSimPlugOut(int i) {
    }

    public void onSimMissing(int i, int i2) {
    }

    public void onSimRecovery(int i, int i2) {
    }

    public void smlSlotLockInfoChangedInd(int i, ArrayList<Integer> arrayList) {
    }

    public void onSimTrayPlugIn(int i) {
    }

    public void onSimCommonSlotNoChanged(int i) {
    }

    public void networkInfoInd(int i, ArrayList<String> arrayList) {
    }

    @Override
    public void onSimMeLockEvent(int i) {
    }

    public void pcoDataAfterAttached(int i, PcoDataAttachedInfo pcoDataAttachedInfo) {
    }

    public void confSRVCC(int i, ArrayList<Integer> arrayList) {
    }

    public void volteLteConnectionStatus(int i, ArrayList<Integer> arrayList) {
    }

    public void onVsimEventIndication(int i, VsimOperationEvent vsimOperationEvent) {
    }

    public void dedicatedBearerActivationInd(int i, DedicateDataCall dedicateDataCall) {
    }

    public void dedicatedBearerModificationInd(int i, DedicateDataCall dedicateDataCall) {
    }

    public void dedicatedBearerDeactivationInd(int i, int i2) {
    }

    public void oemHookRaw(int i, ArrayList<Byte> arrayList) {
    }

    public void onTxPowerIndication(int i, ArrayList<Integer> arrayList) {
    }

    public void onTxPowerStatusIndication(int i, ArrayList<Integer> arrayList) {
    }

    public void networkRejectCauseInd(int i, ArrayList<Integer> arrayList) {
    }

    public void dsbpStateChanged(int i, int i2) {
    }
}
