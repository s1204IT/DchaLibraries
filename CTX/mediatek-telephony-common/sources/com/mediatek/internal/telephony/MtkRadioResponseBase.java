package com.mediatek.internal.telephony;

import android.hardware.radio.V1_0.ActivityStatsInfo;
import android.hardware.radio.V1_0.Call;
import android.hardware.radio.V1_0.CallForwardInfo;
import android.hardware.radio.V1_0.CardStatus;
import android.hardware.radio.V1_0.CarrierRestrictions;
import android.hardware.radio.V1_0.CdmaBroadcastSmsConfigInfo;
import android.hardware.radio.V1_0.CellInfo;
import android.hardware.radio.V1_0.DataRegStateResult;
import android.hardware.radio.V1_0.GsmBroadcastSmsConfigInfo;
import android.hardware.radio.V1_0.HardwareConfig;
import android.hardware.radio.V1_0.IccIoResult;
import android.hardware.radio.V1_0.LastCallFailCauseInfo;
import android.hardware.radio.V1_0.LceDataInfo;
import android.hardware.radio.V1_0.LceStatusInfo;
import android.hardware.radio.V1_0.NeighboringCell;
import android.hardware.radio.V1_0.OperatorInfo;
import android.hardware.radio.V1_0.RadioCapability;
import android.hardware.radio.V1_0.RadioResponseInfo;
import android.hardware.radio.V1_0.SendSmsResult;
import android.hardware.radio.V1_0.SetupDataCallResult;
import android.hardware.radio.V1_0.SignalStrength;
import android.hardware.radio.V1_0.VoiceRegStateResult;
import android.hardware.radio.V1_1.KeepaliveStatus;
import android.os.RemoteException;
import com.android.internal.telephony.RIL;
import java.util.ArrayList;
import vendor.mediatek.hardware.radio.V3_0.CallForwardInfoEx;
import vendor.mediatek.hardware.radio.V3_0.IRadioResponse;
import vendor.mediatek.hardware.radio.V3_0.OperatorInfoWithAct;
import vendor.mediatek.hardware.radio.V3_0.PhbEntryExt;
import vendor.mediatek.hardware.radio.V3_0.PhbEntryStructure;
import vendor.mediatek.hardware.radio.V3_0.PhbMemStorageResponse;
import vendor.mediatek.hardware.radio.V3_0.SignalStrengthWithWcdmaEcio;
import vendor.mediatek.hardware.radio.V3_0.SmsMemStatus;
import vendor.mediatek.hardware.radio.V3_0.SmsParams;
import vendor.mediatek.hardware.radio.V3_0.VsimEvent;

public class MtkRadioResponseBase extends IRadioResponse.Stub {
    public MtkRadioResponseBase(RIL ril) {
    }

    public void acknowledgeRequest(int i) {
    }

    public void getIccCardStatusResponse(RadioResponseInfo radioResponseInfo, CardStatus cardStatus) {
    }

    public void getIccCardStatusResponse_1_2(RadioResponseInfo radioResponseInfo, android.hardware.radio.V1_2.CardStatus cardStatus) {
    }

    public void supplyIccPinForAppResponse(RadioResponseInfo radioResponseInfo, int i) {
    }

    public void supplyIccPukForAppResponse(RadioResponseInfo radioResponseInfo, int i) {
    }

    public void supplyIccPin2ForAppResponse(RadioResponseInfo radioResponseInfo, int i) {
    }

    public void supplyIccPuk2ForAppResponse(RadioResponseInfo radioResponseInfo, int i) {
    }

    public void changeIccPinForAppResponse(RadioResponseInfo radioResponseInfo, int i) {
    }

    public void changeIccPin2ForAppResponse(RadioResponseInfo radioResponseInfo, int i) {
    }

    public void supplyNetworkDepersonalizationResponse(RadioResponseInfo radioResponseInfo, int i) {
    }

    public void dialResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void getIMSIForAppResponse(RadioResponseInfo radioResponseInfo, String str) {
    }

    public void hangupConnectionResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void hangupWaitingOrBackgroundResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void hangupForegroundResumeBackgroundResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void switchWaitingOrHoldingAndActiveResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void conferenceResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void rejectCallResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void getLastCallFailCauseResponse(RadioResponseInfo radioResponseInfo, LastCallFailCauseInfo lastCallFailCauseInfo) {
    }

    public void getSignalStrengthResponse(RadioResponseInfo radioResponseInfo, SignalStrength signalStrength) {
    }

    public void getSignalStrengthResponse_1_2(RadioResponseInfo radioResponseInfo, android.hardware.radio.V1_2.SignalStrength signalStrength) {
    }

    public void getVoiceRegistrationStateResponse(RadioResponseInfo radioResponseInfo, VoiceRegStateResult voiceRegStateResult) {
    }

    public void getVoiceRegistrationStateResponse_1_2(RadioResponseInfo radioResponseInfo, android.hardware.radio.V1_2.VoiceRegStateResult voiceRegStateResult) {
    }

    public void getDataRegistrationStateResponse(RadioResponseInfo radioResponseInfo, DataRegStateResult dataRegStateResult) {
    }

    public void getDataRegistrationStateResponse_1_2(RadioResponseInfo radioResponseInfo, android.hardware.radio.V1_2.DataRegStateResult dataRegStateResult) {
    }

    public void getOperatorResponse(RadioResponseInfo radioResponseInfo, String str, String str2, String str3) {
    }

    public void setRadioPowerResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void sendDtmfResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void sendSmsResponse(RadioResponseInfo radioResponseInfo, SendSmsResult sendSmsResult) {
    }

    public void sendSMSExpectMoreResponse(RadioResponseInfo radioResponseInfo, SendSmsResult sendSmsResult) {
    }

    public void setupDataCallResponse(RadioResponseInfo radioResponseInfo, SetupDataCallResult setupDataCallResult) {
    }

    public void iccIOForAppResponse(RadioResponseInfo radioResponseInfo, IccIoResult iccIoResult) {
    }

    public void sendUssdResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void cancelPendingUssdResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void getClirResponse(RadioResponseInfo radioResponseInfo, int i, int i2) {
    }

    public void setClirResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void getCallForwardStatusResponse(RadioResponseInfo radioResponseInfo, ArrayList<CallForwardInfo> arrayList) {
    }

    public void setCallForwardResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void getCallWaitingResponse(RadioResponseInfo radioResponseInfo, boolean z, int i) {
    }

    public void setCallWaitingResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void acknowledgeLastIncomingGsmSmsResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void acceptCallResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void deactivateDataCallResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void getFacilityLockForAppResponse(RadioResponseInfo radioResponseInfo, int i) {
    }

    public void setFacilityLockForAppResponse(RadioResponseInfo radioResponseInfo, int i) {
    }

    public void setBarringPasswordResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void getNetworkSelectionModeResponse(RadioResponseInfo radioResponseInfo, boolean z) {
    }

    public void setNetworkSelectionModeAutomaticResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void setNetworkSelectionModeManualResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void getAvailableNetworksResponse(RadioResponseInfo radioResponseInfo, ArrayList<OperatorInfo> arrayList) {
    }

    public void startNetworkScanResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void stopNetworkScanResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void startDtmfResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void stopDtmfResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void getBasebandVersionResponse(RadioResponseInfo radioResponseInfo, String str) {
    }

    public void separateConnectionResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void setMuteResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void getMuteResponse(RadioResponseInfo radioResponseInfo, boolean z) {
    }

    public void getClipResponse(RadioResponseInfo radioResponseInfo, int i) {
    }

    public void setClipResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void getColpResponse(RadioResponseInfo radioResponseInfo, int i, int i2) {
    }

    public void getColrResponse(RadioResponseInfo radioResponseInfo, int i) {
    }

    public void sendCnapResponse(RadioResponseInfo radioResponseInfo, int i, int i2) {
    }

    public void setColpResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void setColrResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void queryCallForwardInTimeSlotStatusResponse(RadioResponseInfo radioResponseInfo, ArrayList<CallForwardInfoEx> arrayList) {
    }

    public void setCallForwardInTimeSlotResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void runGbaAuthenticationResponse(RadioResponseInfo radioResponseInfo, ArrayList<String> arrayList) {
    }

    public void getDataCallListResponse(RadioResponseInfo radioResponseInfo, ArrayList<SetupDataCallResult> arrayList) {
    }

    public void sendOemRilRequestRawResponse(RadioResponseInfo radioResponseInfo, ArrayList<Byte> arrayList) {
    }

    public void setSuppServiceNotificationsResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void writeSmsToSimResponse(RadioResponseInfo radioResponseInfo, int i) {
    }

    public void deleteSmsOnSimResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void setBandModeResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void getAvailableBandModesResponse(RadioResponseInfo radioResponseInfo, ArrayList<Integer> arrayList) {
    }

    public void sendEnvelopeResponse(RadioResponseInfo radioResponseInfo, String str) {
    }

    public void sendTerminalResponseToSimResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void handleStkCallSetupRequestFromSimResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void setPdnReuseResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void setOverrideApnResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void setPdnNameReuseResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void explicitCallTransferResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void setPreferredNetworkTypeResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void getPreferredNetworkTypeResponse(RadioResponseInfo radioResponseInfo, int i) {
    }

    public void getNeighboringCidsResponse(RadioResponseInfo radioResponseInfo, ArrayList<NeighboringCell> arrayList) {
    }

    public void setLocationUpdatesResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void setCdmaSubscriptionSourceResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void setCdmaRoamingPreferenceResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void getCdmaRoamingPreferenceResponse(RadioResponseInfo radioResponseInfo, int i) {
    }

    public void setTTYModeResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void getTTYModeResponse(RadioResponseInfo radioResponseInfo, int i) {
    }

    public void setPreferredVoicePrivacyResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void getPreferredVoicePrivacyResponse(RadioResponseInfo radioResponseInfo, boolean z) {
    }

    public void sendCDMAFeatureCodeResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void sendBurstDtmfResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void sendCdmaSmsResponse(RadioResponseInfo radioResponseInfo, SendSmsResult sendSmsResult) {
    }

    public void acknowledgeLastIncomingCdmaSmsResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void getGsmBroadcastConfigResponse(RadioResponseInfo radioResponseInfo, ArrayList<GsmBroadcastSmsConfigInfo> arrayList) {
    }

    public void setGsmBroadcastConfigResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void setGsmBroadcastActivationResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void getCdmaBroadcastConfigResponse(RadioResponseInfo radioResponseInfo, ArrayList<CdmaBroadcastSmsConfigInfo> arrayList) {
    }

    public void setCdmaBroadcastConfigResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void setCdmaBroadcastActivationResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void getCDMASubscriptionResponse(RadioResponseInfo radioResponseInfo, String str, String str2, String str3, String str4, String str5) {
    }

    public void writeSmsToRuimResponse(RadioResponseInfo radioResponseInfo, int i) {
    }

    public void deleteSmsOnRuimResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void getDeviceIdentityResponse(RadioResponseInfo radioResponseInfo, String str, String str2, String str3, String str4) {
    }

    public void exitEmergencyCallbackModeResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void getSmscAddressResponse(RadioResponseInfo radioResponseInfo, String str) {
    }

    public void setSmscAddressResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void reportSmsMemoryStatusResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void reportStkServiceIsRunningResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void getCdmaSubscriptionSourceResponse(RadioResponseInfo radioResponseInfo, int i) {
    }

    public void requestIsimAuthenticationResponse(RadioResponseInfo radioResponseInfo, String str) {
    }

    public void acknowledgeIncomingGsmSmsWithPduResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void sendEnvelopeWithStatusResponse(RadioResponseInfo radioResponseInfo, IccIoResult iccIoResult) {
    }

    public void getVoiceRadioTechnologyResponse(RadioResponseInfo radioResponseInfo, int i) {
    }

    public void getCellInfoListResponse(RadioResponseInfo radioResponseInfo, ArrayList<CellInfo> arrayList) {
    }

    public void getCellInfoListResponse_1_2(RadioResponseInfo radioResponseInfo, ArrayList<android.hardware.radio.V1_2.CellInfo> arrayList) {
    }

    public void setCellInfoListRateResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void setInitialAttachApnResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void getImsRegistrationStateResponse(RadioResponseInfo radioResponseInfo, boolean z, int i) {
    }

    public void sendImsSmsResponse(RadioResponseInfo radioResponseInfo, SendSmsResult sendSmsResult) {
    }

    public void iccTransmitApduBasicChannelResponse(RadioResponseInfo radioResponseInfo, IccIoResult iccIoResult) {
    }

    public void iccOpenLogicalChannelResponse(RadioResponseInfo radioResponseInfo, int i, ArrayList<Byte> arrayList) {
    }

    public void iccCloseLogicalChannelResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void iccTransmitApduLogicalChannelResponse(RadioResponseInfo radioResponseInfo, IccIoResult iccIoResult) {
    }

    public void nvReadItemResponse(RadioResponseInfo radioResponseInfo, String str) {
    }

    public void nvWriteItemResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void nvWriteCdmaPrlResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void nvResetConfigResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void setUiccSubscriptionResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void setDataAllowedResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void getHardwareConfigResponse(RadioResponseInfo radioResponseInfo, ArrayList<HardwareConfig> arrayList) {
    }

    public void requestIccSimAuthenticationResponse(RadioResponseInfo radioResponseInfo, IccIoResult iccIoResult) {
    }

    public void setDataProfileResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void requestShutdownResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void getRadioCapabilityResponse(RadioResponseInfo radioResponseInfo, RadioCapability radioCapability) {
    }

    public void setRadioCapabilityResponse(RadioResponseInfo radioResponseInfo, RadioCapability radioCapability) {
    }

    public void startLceServiceResponse(RadioResponseInfo radioResponseInfo, LceStatusInfo lceStatusInfo) {
    }

    public void stopLceServiceResponse(RadioResponseInfo radioResponseInfo, LceStatusInfo lceStatusInfo) {
    }

    public void pullLceDataResponse(RadioResponseInfo radioResponseInfo, LceDataInfo lceDataInfo) {
    }

    public void getModemActivityInfoResponse(RadioResponseInfo radioResponseInfo, ActivityStatsInfo activityStatsInfo) {
    }

    public void setAllowedCarriersResponse(RadioResponseInfo radioResponseInfo, int i) {
    }

    public void getAllowedCarriersResponse(RadioResponseInfo radioResponseInfo, boolean z, CarrierRestrictions carrierRestrictions) {
    }

    public void sendDeviceStateResponse(RadioResponseInfo radioResponseInfo) {
    }

    @Override
    public void setCarrierInfoForImsiEncryptionResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void setIndicationFilterResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void setSimCardPowerResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void setSignalStrengthReportingCriteriaResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void setLinkCapacityReportingCriteriaResponse(RadioResponseInfo radioResponseInfo) {
    }

    @Override
    public void setSimCardPowerResponse_1_1(RadioResponseInfo radioResponseInfo) {
    }

    @Override
    public void startKeepaliveResponse(RadioResponseInfo radioResponseInfo, KeepaliveStatus keepaliveStatus) {
    }

    @Override
    public void stopKeepaliveResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void setTrmResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void getATRResponse(RadioResponseInfo radioResponseInfo, String str) {
    }

    public void getIccidResponse(RadioResponseInfo radioResponseInfo, String str) {
    }

    public void setSimPowerResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void setNetworkSelectionModeManualWithActResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void getAvailableNetworksWithActResponse(RadioResponseInfo radioResponseInfo, ArrayList<OperatorInfoWithAct> arrayList) {
    }

    public void getSignalStrengthWithWcdmaEcioResponse(RadioResponseInfo radioResponseInfo, SignalStrengthWithWcdmaEcio signalStrengthWithWcdmaEcio) {
    }

    public void cancelAvailableNetworksResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void setModemPowerResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void getSmsParametersResponse(RadioResponseInfo radioResponseInfo, SmsParams smsParams) {
    }

    public void setSmsParametersResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void setEtwsResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void removeCbMsgResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void getSmsMemStatusResponse(RadioResponseInfo radioResponseInfo, SmsMemStatus smsMemStatus) {
    }

    public void setGsmBroadcastLangsResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void getGsmBroadcastLangsResponse(RadioResponseInfo radioResponseInfo, String str) {
    }

    public void getGsmBroadcastActivationRsp(RadioResponseInfo radioResponseInfo, int i) {
    }

    public void setSmsFwkReadyRsp(RadioResponseInfo radioResponseInfo) {
    }

    public void sendEmbmsAtCommandResponse(RadioResponseInfo radioResponseInfo, String str) {
    }

    public void hangupAllResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
    }

    public void setCallIndicationResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void emergencyDialResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void setEccServiceCategoryResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void setEccListResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void setVoicePreferStatusResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void setEccNumResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void getEccNumResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void currentStatusResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void eccPreferredRatResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void setApcModeResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void getApcInfoResponse(RadioResponseInfo radioResponseInfo, ArrayList<Integer> arrayList) {
    }

    public void triggerModeSwitchByEccResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void getSmsRuimMemoryStatusResponse(RadioResponseInfo radioResponseInfo, SmsMemStatus smsMemStatus) {
    }

    public void setFdModeResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void setResumeRegistrationResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void storeModemTypeResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void reloadModemTypeResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void handleStkCallSetupRequestFromSimWithResCodeResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void queryPhbStorageInfoResponse(RadioResponseInfo radioResponseInfo, ArrayList<Integer> arrayList) {
    }

    public void writePhbEntryResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void readPhbEntryResponse(RadioResponseInfo radioResponseInfo, ArrayList<PhbEntryStructure> arrayList) {
    }

    public void getCurrentCallsResponse(RadioResponseInfo radioResponseInfo, ArrayList<Call> arrayList) {
    }

    public void getCurrentCallsResponse_1_2(RadioResponseInfo radioResponseInfo, ArrayList<android.hardware.radio.V1_2.Call> arrayList) {
    }

    public void queryUPBCapabilityResponse(RadioResponseInfo radioResponseInfo, ArrayList<Integer> arrayList) {
    }

    public void editUPBEntryResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void deleteUPBEntryResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void readUPBGasListResponse(RadioResponseInfo radioResponseInfo, ArrayList<String> arrayList) {
    }

    public void readUPBGrpEntryResponse(RadioResponseInfo radioResponseInfo, ArrayList<Integer> arrayList) {
    }

    public void writeUPBGrpEntryResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void getPhoneBookStringsLengthResponse(RadioResponseInfo radioResponseInfo, ArrayList<Integer> arrayList) {
    }

    public void getPhoneBookMemStorageResponse(RadioResponseInfo radioResponseInfo, PhbMemStorageResponse phbMemStorageResponse) {
    }

    public void setPhoneBookMemStorageResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void readPhoneBookEntryExtResponse(RadioResponseInfo radioResponseInfo, ArrayList<PhbEntryExt> arrayList) {
    }

    public void writePhoneBookEntryExtResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void queryUPBAvailableResponse(RadioResponseInfo radioResponseInfo, ArrayList<Integer> arrayList) {
    }

    public void readUPBEmailEntryResponse(RadioResponseInfo radioResponseInfo, String str) {
    }

    public void readUPBSneEntryResponse(RadioResponseInfo radioResponseInfo, String str) {
    }

    public void readUPBAnrEntryResponse(RadioResponseInfo radioResponseInfo, ArrayList<PhbEntryStructure> arrayList) {
    }

    public void readUPBAasListResponse(RadioResponseInfo radioResponseInfo, ArrayList<String> arrayList) {
    }

    public void setPhonebookReadyResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void resetRadioResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void restartRILDResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void getFemtocellListResponse(RadioResponseInfo radioResponseInfo, ArrayList<String> arrayList) {
    }

    public void abortFemtocellListResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void selectFemtocellResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void queryFemtoCellSystemSelectionModeResponse(RadioResponseInfo radioResponseInfo, int i) {
    }

    public void setFemtoCellSystemSelectionModeResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void syncDataSettingsToMdResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void resetMdDataRetryCountResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void setRemoveRestrictEutranModeResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void setLteAccessStratumReportResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void setLteUplinkDataTransferResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void queryNetworkLockResponse(RadioResponseInfo radioResponseInfo, int i, int i2, int i3, int i4, int i5, int i6, int i7) {
    }

    public void setNetworkLockResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void supplyDepersonalizationResponse(RadioResponseInfo radioResponseInfo, int i) {
    }

    public void supplyDeviceNetworkDepersonalizationResponse(RadioResponseInfo radioResponseInfo, int i) {
    }

    public void setRxTestConfigResponse(RadioResponseInfo radioResponseInfo, ArrayList<Integer> arrayList) {
    }

    public void getRxTestResultResponse(RadioResponseInfo radioResponseInfo, ArrayList<Integer> arrayList) {
    }

    public void getPOLCapabilityResponse(RadioResponseInfo radioResponseInfo, ArrayList<Integer> arrayList) {
    }

    public void getCurrentPOLListResponse(RadioResponseInfo radioResponseInfo, ArrayList<String> arrayList) {
    }

    public void setPOLEntryResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void setRoamingEnableResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void getRoamingEnableResponse(RadioResponseInfo radioResponseInfo, ArrayList<Integer> arrayList) {
    }

    public void setLteReleaseVersionResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void getLteReleaseVersionResponse(RadioResponseInfo radioResponseInfo, int i) {
    }

    public void vsimNotificationResponse(RadioResponseInfo radioResponseInfo, VsimEvent vsimEvent) {
    }

    public void vsimOperationResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void setWifiEnabledResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void setWifiAssociatedResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void setWifiSignalLevelResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void setWifiIpAddressResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void setLocationInfoResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void setEmergencyAddressIdResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void setNattKeepAliveStatusResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void setWifiPingResultResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void setE911StateResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void setServiceStateToModemResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void sendRequestRawResponse(RadioResponseInfo radioResponseInfo, ArrayList<Byte> arrayList) {
    }

    public void sendRequestStringsResponse(RadioResponseInfo radioResponseInfo, ArrayList<String> arrayList) {
    }

    public void dataConnectionAttachResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void dataConnectionDetachResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void resetAllConnectionsResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void reportAirplaneModeResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void reportSimModeResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void setSilentRebootResponse(RadioResponseInfo radioResponseInfo) {
    }

    @Override
    public void setTxPowerStatusResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void setPropImsHandoverResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void setOperatorConfigurationResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void setSuppServPropertyResponse(RadioResponseInfo radioResponseInfo) {
    }

    public void getSuppServPropertyResponse(RadioResponseInfo radioResponseInfo, String str) {
    }
}
