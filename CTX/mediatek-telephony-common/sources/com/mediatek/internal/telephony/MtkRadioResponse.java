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
import android.os.RemoteException;
import android.os.SystemProperties;
import android.telephony.SubscriptionManager;
import com.android.internal.telephony.RIL;
import com.android.internal.telephony.RILRequest;
import com.android.internal.telephony.RadioResponse;
import com.android.internal.telephony.uicc.IccUtils;
import com.mediatek.internal.telephony.datasub.DataSubConstants;
import com.mediatek.internal.telephony.phb.PBEntry;
import com.mediatek.internal.telephony.phb.PBMemStorage;
import com.mediatek.internal.telephony.phb.PhbEntry;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.TimeZone;
import mediatek.telephony.MtkSmsParameters;
import vendor.mediatek.hardware.radio.V3_0.CallForwardInfoEx;
import vendor.mediatek.hardware.radio.V3_0.OperatorInfoWithAct;
import vendor.mediatek.hardware.radio.V3_0.PhbEntryExt;
import vendor.mediatek.hardware.radio.V3_0.PhbEntryStructure;
import vendor.mediatek.hardware.radio.V3_0.PhbMemStorageResponse;
import vendor.mediatek.hardware.radio.V3_0.SignalStrengthWithWcdmaEcio;
import vendor.mediatek.hardware.radio.V3_0.SmsMemStatus;
import vendor.mediatek.hardware.radio.V3_0.SmsParams;
import vendor.mediatek.hardware.radio.V3_0.VsimEvent;

public class MtkRadioResponse extends MtkRadioResponseBase {
    private static final String TAG = "MtkRadioResp";
    private static final boolean isUserLoad = SystemProperties.get("ro.build.type").equals(DataSubConstants.REASON_MOBILE_DATA_ENABLE_USER);
    MtkMessageBoost mMtkMessageBoost;
    MtkRIL mMtkRil;
    RadioResponse mRadioResponse;

    public MtkRadioResponse(RIL ril) {
        super(ril);
        this.mRadioResponse = new RadioResponse(ril);
        this.mMtkRil = (MtkRIL) ril;
        this.mMtkMessageBoost = MtkMessageBoost.init(this.mMtkRil);
    }

    @Override
    public void acknowledgeRequest(int i) {
        this.mRadioResponse.acknowledgeRequest(i);
    }

    @Override
    public void getIccCardStatusResponse(RadioResponseInfo radioResponseInfo, CardStatus cardStatus) {
        this.mRadioResponse.getIccCardStatusResponse(radioResponseInfo, cardStatus);
    }

    @Override
    public void getIccCardStatusResponse_1_2(RadioResponseInfo radioResponseInfo, android.hardware.radio.V1_2.CardStatus cardStatus) {
        this.mRadioResponse.getIccCardStatusResponse_1_2(radioResponseInfo, cardStatus);
    }

    @Override
    public void supplyIccPinForAppResponse(RadioResponseInfo radioResponseInfo, int i) {
        this.mRadioResponse.supplyIccPinForAppResponse(radioResponseInfo, i);
    }

    @Override
    public void supplyIccPukForAppResponse(RadioResponseInfo radioResponseInfo, int i) {
        this.mRadioResponse.supplyIccPukForAppResponse(radioResponseInfo, i);
    }

    @Override
    public void supplyIccPin2ForAppResponse(RadioResponseInfo radioResponseInfo, int i) {
        this.mRadioResponse.supplyIccPin2ForAppResponse(radioResponseInfo, i);
    }

    @Override
    public void supplyIccPuk2ForAppResponse(RadioResponseInfo radioResponseInfo, int i) {
        this.mRadioResponse.supplyIccPuk2ForAppResponse(radioResponseInfo, i);
    }

    @Override
    public void changeIccPinForAppResponse(RadioResponseInfo radioResponseInfo, int i) {
        this.mRadioResponse.changeIccPinForAppResponse(radioResponseInfo, i);
    }

    @Override
    public void changeIccPin2ForAppResponse(RadioResponseInfo radioResponseInfo, int i) {
        this.mRadioResponse.changeIccPin2ForAppResponse(radioResponseInfo, i);
    }

    @Override
    public void supplyNetworkDepersonalizationResponse(RadioResponseInfo radioResponseInfo, int i) {
        this.mRadioResponse.supplyNetworkDepersonalizationResponse(radioResponseInfo, i);
    }

    @Override
    public void supplyDepersonalizationResponse(RadioResponseInfo radioResponseInfo, int i) {
        this.mRadioResponse.supplyNetworkDepersonalizationResponse(radioResponseInfo, i);
    }

    @Override
    public void getCurrentCallsResponse(RadioResponseInfo radioResponseInfo, ArrayList<Call> arrayList) {
        this.mRadioResponse.getCurrentCallsResponse(radioResponseInfo, arrayList);
    }

    @Override
    public void getCurrentCallsResponse_1_2(RadioResponseInfo radioResponseInfo, ArrayList<android.hardware.radio.V1_2.Call> arrayList) {
        this.mRadioResponse.getCurrentCallsResponse_1_2(radioResponseInfo, arrayList);
    }

    @Override
    public void dialResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.dialResponse(radioResponseInfo);
    }

    @Override
    public void getIMSIForAppResponse(RadioResponseInfo radioResponseInfo, String str) {
        this.mRadioResponse.getIMSIForAppResponse(radioResponseInfo, str);
    }

    @Override
    public void hangupConnectionResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.hangupConnectionResponse(radioResponseInfo);
    }

    @Override
    public void hangupWaitingOrBackgroundResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.hangupWaitingOrBackgroundResponse(radioResponseInfo);
    }

    @Override
    public void hangupForegroundResumeBackgroundResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.hangupForegroundResumeBackgroundResponse(radioResponseInfo);
    }

    @Override
    public void switchWaitingOrHoldingAndActiveResponse(RadioResponseInfo radioResponseInfo) {
        this.mMtkRil.riljLog("clear mIsSendChldRequest");
        synchronized (this.mMtkRil.mDtmfReqQueue) {
            this.mMtkRil.mDtmfReqQueue.resetSendChldRequest();
        }
        this.mRadioResponse.switchWaitingOrHoldingAndActiveResponse(radioResponseInfo);
    }

    @Override
    public void conferenceResponse(RadioResponseInfo radioResponseInfo) {
        this.mMtkRil.riljLog("clear mIsSendChldRequest");
        synchronized (this.mMtkRil.mDtmfReqQueue) {
            this.mMtkRil.mDtmfReqQueue.resetSendChldRequest();
        }
        this.mRadioResponse.conferenceResponse(radioResponseInfo);
    }

    @Override
    public void rejectCallResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.rejectCallResponse(radioResponseInfo);
    }

    @Override
    public void getLastCallFailCauseResponse(RadioResponseInfo radioResponseInfo, LastCallFailCauseInfo lastCallFailCauseInfo) {
        this.mRadioResponse.getLastCallFailCauseResponse(radioResponseInfo, lastCallFailCauseInfo);
    }

    @Override
    public void getSignalStrengthResponse(RadioResponseInfo radioResponseInfo, SignalStrength signalStrength) {
        this.mRadioResponse.getSignalStrengthResponse(radioResponseInfo, signalStrength);
    }

    @Override
    public void getSignalStrengthResponse_1_2(RadioResponseInfo radioResponseInfo, android.hardware.radio.V1_2.SignalStrength signalStrength) {
        this.mRadioResponse.getSignalStrengthResponse_1_2(radioResponseInfo, signalStrength);
    }

    @Override
    public void getVoiceRegistrationStateResponse(RadioResponseInfo radioResponseInfo, VoiceRegStateResult voiceRegStateResult) {
        this.mRadioResponse.getVoiceRegistrationStateResponse(radioResponseInfo, voiceRegStateResult);
    }

    @Override
    public void getVoiceRegistrationStateResponse_1_2(RadioResponseInfo radioResponseInfo, android.hardware.radio.V1_2.VoiceRegStateResult voiceRegStateResult) {
        this.mRadioResponse.getVoiceRegistrationStateResponse_1_2(radioResponseInfo, voiceRegStateResult);
    }

    @Override
    public void getDataRegistrationStateResponse(RadioResponseInfo radioResponseInfo, DataRegStateResult dataRegStateResult) {
        this.mRadioResponse.getDataRegistrationStateResponse(radioResponseInfo, dataRegStateResult);
    }

    @Override
    public void getDataRegistrationStateResponse_1_2(RadioResponseInfo radioResponseInfo, android.hardware.radio.V1_2.DataRegStateResult dataRegStateResult) {
        this.mRadioResponse.getDataRegistrationStateResponse_1_2(radioResponseInfo, dataRegStateResult);
    }

    @Override
    public void getOperatorResponse(RadioResponseInfo radioResponseInfo, String str, String str2, String str3) {
        this.mRadioResponse.getOperatorResponse(radioResponseInfo, str, str2, str3);
    }

    @Override
    public void setRadioPowerResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.setRadioPowerResponse(radioResponseInfo);
    }

    @Override
    public void sendDtmfResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.sendDtmfResponse(radioResponseInfo);
    }

    @Override
    public void sendSmsResponse(RadioResponseInfo radioResponseInfo, SendSmsResult sendSmsResult) {
        this.mRadioResponse.sendSmsResponse(radioResponseInfo, sendSmsResult);
    }

    @Override
    public void sendSMSExpectMoreResponse(RadioResponseInfo radioResponseInfo, SendSmsResult sendSmsResult) {
        this.mRadioResponse.sendSMSExpectMoreResponse(radioResponseInfo, sendSmsResult);
    }

    @Override
    public void setupDataCallResponse(RadioResponseInfo radioResponseInfo, SetupDataCallResult setupDataCallResult) {
        this.mRadioResponse.setupDataCallResponse(radioResponseInfo, setupDataCallResult);
    }

    @Override
    public void iccIOForAppResponse(RadioResponseInfo radioResponseInfo, IccIoResult iccIoResult) {
        this.mRadioResponse.iccIOForAppResponse(radioResponseInfo, iccIoResult);
    }

    @Override
    public void sendUssdResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.sendUssdResponse(radioResponseInfo);
    }

    @Override
    public void cancelPendingUssdResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.cancelPendingUssdResponse(radioResponseInfo);
    }

    @Override
    public void getClirResponse(RadioResponseInfo radioResponseInfo, int i, int i2) {
        this.mRadioResponse.getClirResponse(radioResponseInfo, i, i2);
    }

    @Override
    public void setClirResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.setClirResponse(radioResponseInfo);
    }

    @Override
    public void getCallForwardStatusResponse(RadioResponseInfo radioResponseInfo, ArrayList<CallForwardInfo> arrayList) {
        this.mRadioResponse.getCallForwardStatusResponse(radioResponseInfo, arrayList);
    }

    @Override
    public void setCallForwardResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.setCallForwardResponse(radioResponseInfo);
    }

    @Override
    public void getCallWaitingResponse(RadioResponseInfo radioResponseInfo, boolean z, int i) {
        this.mRadioResponse.getCallWaitingResponse(radioResponseInfo, z, i);
    }

    @Override
    public void setCallWaitingResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.setCallWaitingResponse(radioResponseInfo);
    }

    @Override
    public void acknowledgeLastIncomingGsmSmsResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.acknowledgeLastIncomingGsmSmsResponse(radioResponseInfo);
    }

    @Override
    public void acceptCallResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.acceptCallResponse(radioResponseInfo);
    }

    @Override
    public void deactivateDataCallResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.deactivateDataCallResponse(radioResponseInfo);
    }

    @Override
    public void getFacilityLockForAppResponse(RadioResponseInfo radioResponseInfo, int i) {
        this.mRadioResponse.getFacilityLockForAppResponse(radioResponseInfo, i);
    }

    @Override
    public void setFacilityLockForAppResponse(RadioResponseInfo radioResponseInfo, int i) {
        this.mRadioResponse.setFacilityLockForAppResponse(radioResponseInfo, i);
    }

    @Override
    public void setBarringPasswordResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.setBarringPasswordResponse(radioResponseInfo);
    }

    @Override
    public void getNetworkSelectionModeResponse(RadioResponseInfo radioResponseInfo, boolean z) {
        this.mRadioResponse.getNetworkSelectionModeResponse(radioResponseInfo, z);
    }

    @Override
    public void setNetworkSelectionModeAutomaticResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.setNetworkSelectionModeAutomaticResponse(radioResponseInfo);
    }

    @Override
    public void setNetworkSelectionModeManualResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.setNetworkSelectionModeManualResponse(radioResponseInfo);
    }

    @Override
    public void getAvailableNetworksResponse(RadioResponseInfo radioResponseInfo, ArrayList<OperatorInfo> arrayList) {
        this.mRadioResponse.getAvailableNetworksResponse(radioResponseInfo, arrayList);
    }

    @Override
    public void startNetworkScanResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.startNetworkScanResponse(radioResponseInfo);
    }

    @Override
    public void stopNetworkScanResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.stopNetworkScanResponse(radioResponseInfo);
    }

    @Override
    public void startDtmfResponse(RadioResponseInfo radioResponseInfo) {
        this.mMtkRil.handleDtmfQueueNext(radioResponseInfo.serial);
        this.mRadioResponse.startDtmfResponse(radioResponseInfo);
    }

    @Override
    public void stopDtmfResponse(RadioResponseInfo radioResponseInfo) {
        this.mMtkRil.handleDtmfQueueNext(radioResponseInfo.serial);
        this.mRadioResponse.stopDtmfResponse(radioResponseInfo);
    }

    @Override
    public void getBasebandVersionResponse(RadioResponseInfo radioResponseInfo, String str) {
        this.mRadioResponse.getBasebandVersionResponse(radioResponseInfo, str);
    }

    @Override
    public void separateConnectionResponse(RadioResponseInfo radioResponseInfo) {
        this.mMtkRil.riljLog("clear mIsSendChldRequest");
        synchronized (this.mMtkRil.mDtmfReqQueue) {
            this.mMtkRil.mDtmfReqQueue.resetSendChldRequest();
        }
        this.mRadioResponse.separateConnectionResponse(radioResponseInfo);
    }

    @Override
    public void setMuteResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.setMuteResponse(radioResponseInfo);
    }

    @Override
    public void getMuteResponse(RadioResponseInfo radioResponseInfo, boolean z) {
        this.mRadioResponse.getMuteResponse(radioResponseInfo, z);
    }

    @Override
    public void getClipResponse(RadioResponseInfo radioResponseInfo, int i) {
        this.mRadioResponse.getClipResponse(radioResponseInfo, i);
    }

    @Override
    public void setClipResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    @Override
    public void getColpResponse(RadioResponseInfo radioResponseInfo, int i, int i2) {
        this.mRadioResponse.responseInts(radioResponseInfo, new int[]{i, i2});
    }

    @Override
    public void getColrResponse(RadioResponseInfo radioResponseInfo, int i) {
        this.mRadioResponse.responseInts(radioResponseInfo, new int[]{i});
    }

    @Override
    public void sendCnapResponse(RadioResponseInfo radioResponseInfo, int i, int i2) {
        this.mRadioResponse.responseInts(radioResponseInfo, new int[]{i, i2});
    }

    @Override
    public void setColpResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    @Override
    public void setColrResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    @Override
    public void queryCallForwardInTimeSlotStatusResponse(RadioResponseInfo radioResponseInfo, ArrayList<CallForwardInfoEx> arrayList) {
        responseCallForwardInfoEx(radioResponseInfo, arrayList);
    }

    @Override
    public void setCallForwardInTimeSlotResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    @Override
    public void runGbaAuthenticationResponse(RadioResponseInfo radioResponseInfo, ArrayList<String> arrayList) {
        RadioResponse radioResponse = this.mRadioResponse;
        RadioResponse.responseStringArrayList(this.mRadioResponse.mRil, radioResponseInfo, arrayList);
    }

    @Override
    public void getDataCallListResponse(RadioResponseInfo radioResponseInfo, ArrayList<SetupDataCallResult> arrayList) {
        this.mRadioResponse.getDataCallListResponse(radioResponseInfo, arrayList);
    }

    @Override
    public void sendOemRilRequestRawResponse(RadioResponseInfo radioResponseInfo, ArrayList<Byte> arrayList) {
    }

    @Override
    public void setSuppServiceNotificationsResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.setSuppServiceNotificationsResponse(radioResponseInfo);
    }

    @Override
    public void writeSmsToSimResponse(RadioResponseInfo radioResponseInfo, int i) {
        this.mRadioResponse.writeSmsToSimResponse(radioResponseInfo, i);
    }

    @Override
    public void deleteSmsOnSimResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.deleteSmsOnSimResponse(radioResponseInfo);
    }

    @Override
    public void setBandModeResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.setBandModeResponse(radioResponseInfo);
    }

    @Override
    public void getAvailableBandModesResponse(RadioResponseInfo radioResponseInfo, ArrayList<Integer> arrayList) {
        this.mRadioResponse.getAvailableBandModesResponse(radioResponseInfo, arrayList);
    }

    @Override
    public void sendEnvelopeResponse(RadioResponseInfo radioResponseInfo, String str) {
        this.mRadioResponse.sendEnvelopeResponse(radioResponseInfo, str);
    }

    @Override
    public void sendTerminalResponseToSimResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.sendTerminalResponseToSimResponse(radioResponseInfo);
    }

    @Override
    public void handleStkCallSetupRequestFromSimResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.handleStkCallSetupRequestFromSimResponse(radioResponseInfo);
    }

    @Override
    public void explicitCallTransferResponse(RadioResponseInfo radioResponseInfo) {
        this.mMtkRil.riljLog("clear mIsSendChldRequest");
        synchronized (this.mMtkRil.mDtmfReqQueue) {
            this.mMtkRil.mDtmfReqQueue.resetSendChldRequest();
        }
        this.mRadioResponse.explicitCallTransferResponse(radioResponseInfo);
    }

    @Override
    public void setPreferredNetworkTypeResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.setPreferredNetworkTypeResponse(radioResponseInfo);
    }

    @Override
    public void getPreferredNetworkTypeResponse(RadioResponseInfo radioResponseInfo, int i) {
        this.mRadioResponse.getPreferredNetworkTypeResponse(radioResponseInfo, i);
    }

    @Override
    public void getNeighboringCidsResponse(RadioResponseInfo radioResponseInfo, ArrayList<NeighboringCell> arrayList) {
        this.mRadioResponse.getNeighboringCidsResponse(radioResponseInfo, arrayList);
    }

    @Override
    public void setLocationUpdatesResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.setLocationUpdatesResponse(radioResponseInfo);
    }

    @Override
    public void setCdmaSubscriptionSourceResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.setCdmaSubscriptionSourceResponse(radioResponseInfo);
    }

    @Override
    public void setCdmaRoamingPreferenceResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.setCdmaRoamingPreferenceResponse(radioResponseInfo);
    }

    @Override
    public void getCdmaRoamingPreferenceResponse(RadioResponseInfo radioResponseInfo, int i) {
        this.mRadioResponse.getCdmaRoamingPreferenceResponse(radioResponseInfo, i);
    }

    @Override
    public void setTTYModeResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.setTTYModeResponse(radioResponseInfo);
    }

    @Override
    public void getTTYModeResponse(RadioResponseInfo radioResponseInfo, int i) {
        this.mRadioResponse.getTTYModeResponse(radioResponseInfo, i);
    }

    @Override
    public void setPreferredVoicePrivacyResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.setPreferredVoicePrivacyResponse(radioResponseInfo);
    }

    @Override
    public void getPreferredVoicePrivacyResponse(RadioResponseInfo radioResponseInfo, boolean z) {
        this.mRadioResponse.getPreferredVoicePrivacyResponse(radioResponseInfo, z);
    }

    @Override
    public void sendCDMAFeatureCodeResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.sendCDMAFeatureCodeResponse(radioResponseInfo);
    }

    @Override
    public void sendBurstDtmfResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.sendBurstDtmfResponse(radioResponseInfo);
    }

    @Override
    public void sendCdmaSmsResponse(RadioResponseInfo radioResponseInfo, SendSmsResult sendSmsResult) {
        this.mRadioResponse.sendCdmaSmsResponse(radioResponseInfo, sendSmsResult);
    }

    @Override
    public void acknowledgeLastIncomingCdmaSmsResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.acknowledgeLastIncomingCdmaSmsResponse(radioResponseInfo);
    }

    @Override
    public void getGsmBroadcastConfigResponse(RadioResponseInfo radioResponseInfo, ArrayList<GsmBroadcastSmsConfigInfo> arrayList) {
        this.mRadioResponse.getGsmBroadcastConfigResponse(radioResponseInfo, arrayList);
    }

    @Override
    public void setGsmBroadcastConfigResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.setGsmBroadcastConfigResponse(radioResponseInfo);
    }

    @Override
    public void setGsmBroadcastActivationResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.setGsmBroadcastActivationResponse(radioResponseInfo);
    }

    @Override
    public void getCdmaBroadcastConfigResponse(RadioResponseInfo radioResponseInfo, ArrayList<CdmaBroadcastSmsConfigInfo> arrayList) {
        this.mRadioResponse.getCdmaBroadcastConfigResponse(radioResponseInfo, arrayList);
    }

    @Override
    public void setCdmaBroadcastConfigResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.setCdmaBroadcastConfigResponse(radioResponseInfo);
    }

    @Override
    public void setCdmaBroadcastActivationResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.setCdmaBroadcastActivationResponse(radioResponseInfo);
    }

    @Override
    public void getCDMASubscriptionResponse(RadioResponseInfo radioResponseInfo, String str, String str2, String str3, String str4, String str5) {
        this.mRadioResponse.getCDMASubscriptionResponse(radioResponseInfo, str, str2, str3, str4, str5);
    }

    @Override
    public void writeSmsToRuimResponse(RadioResponseInfo radioResponseInfo, int i) {
        this.mRadioResponse.writeSmsToRuimResponse(radioResponseInfo, i);
    }

    @Override
    public void deleteSmsOnRuimResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.deleteSmsOnRuimResponse(radioResponseInfo);
    }

    @Override
    public void getDeviceIdentityResponse(RadioResponseInfo radioResponseInfo, String str, String str2, String str3, String str4) {
        this.mRadioResponse.getDeviceIdentityResponse(radioResponseInfo, str, str2, str3, str4);
    }

    @Override
    public void exitEmergencyCallbackModeResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.exitEmergencyCallbackModeResponse(radioResponseInfo);
    }

    @Override
    public void getSmscAddressResponse(RadioResponseInfo radioResponseInfo, String str) {
        this.mRadioResponse.getSmscAddressResponse(radioResponseInfo, str);
    }

    @Override
    public void setSmscAddressResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.setSmscAddressResponse(radioResponseInfo);
    }

    @Override
    public void reportSmsMemoryStatusResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.reportSmsMemoryStatusResponse(radioResponseInfo);
    }

    @Override
    public void reportStkServiceIsRunningResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.reportStkServiceIsRunningResponse(radioResponseInfo);
    }

    @Override
    public void getCdmaSubscriptionSourceResponse(RadioResponseInfo radioResponseInfo, int i) {
        this.mRadioResponse.getCdmaSubscriptionSourceResponse(radioResponseInfo, i);
    }

    @Override
    public void requestIsimAuthenticationResponse(RadioResponseInfo radioResponseInfo, String str) {
        this.mRadioResponse.requestIsimAuthenticationResponse(radioResponseInfo, str);
    }

    @Override
    public void acknowledgeIncomingGsmSmsWithPduResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.acknowledgeIncomingGsmSmsWithPduResponse(radioResponseInfo);
    }

    @Override
    public void sendEnvelopeWithStatusResponse(RadioResponseInfo radioResponseInfo, IccIoResult iccIoResult) {
        this.mRadioResponse.sendEnvelopeWithStatusResponse(radioResponseInfo, iccIoResult);
    }

    @Override
    public void getVoiceRadioTechnologyResponse(RadioResponseInfo radioResponseInfo, int i) {
        this.mRadioResponse.getVoiceRadioTechnologyResponse(radioResponseInfo, i);
    }

    @Override
    public void getCellInfoListResponse(RadioResponseInfo radioResponseInfo, ArrayList<CellInfo> arrayList) {
        this.mRadioResponse.getCellInfoListResponse(radioResponseInfo, arrayList);
    }

    @Override
    public void getCellInfoListResponse_1_2(RadioResponseInfo radioResponseInfo, ArrayList<android.hardware.radio.V1_2.CellInfo> arrayList) {
        this.mRadioResponse.responseCellInfoList_1_2(radioResponseInfo, arrayList);
    }

    @Override
    public void setCellInfoListRateResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.setCellInfoListRateResponse(radioResponseInfo);
    }

    @Override
    public void setInitialAttachApnResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.setInitialAttachApnResponse(radioResponseInfo);
    }

    @Override
    public void getImsRegistrationStateResponse(RadioResponseInfo radioResponseInfo, boolean z, int i) {
        this.mRadioResponse.getImsRegistrationStateResponse(radioResponseInfo, z, i);
    }

    @Override
    public void sendImsSmsResponse(RadioResponseInfo radioResponseInfo, SendSmsResult sendSmsResult) {
        this.mRadioResponse.sendImsSmsResponse(radioResponseInfo, sendSmsResult);
    }

    @Override
    public void iccTransmitApduBasicChannelResponse(RadioResponseInfo radioResponseInfo, IccIoResult iccIoResult) {
        this.mRadioResponse.iccTransmitApduBasicChannelResponse(radioResponseInfo, iccIoResult);
    }

    @Override
    public void iccOpenLogicalChannelResponse(RadioResponseInfo radioResponseInfo, int i, ArrayList<Byte> arrayList) {
        this.mRadioResponse.iccOpenLogicalChannelResponse(radioResponseInfo, i, arrayList);
    }

    @Override
    public void iccCloseLogicalChannelResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.iccCloseLogicalChannelResponse(radioResponseInfo);
    }

    @Override
    public void iccTransmitApduLogicalChannelResponse(RadioResponseInfo radioResponseInfo, IccIoResult iccIoResult) {
        this.mRadioResponse.iccTransmitApduLogicalChannelResponse(radioResponseInfo, iccIoResult);
    }

    @Override
    public void nvReadItemResponse(RadioResponseInfo radioResponseInfo, String str) {
        this.mRadioResponse.nvReadItemResponse(radioResponseInfo, str);
    }

    @Override
    public void nvWriteItemResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.nvWriteItemResponse(radioResponseInfo);
    }

    @Override
    public void nvWriteCdmaPrlResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.nvWriteCdmaPrlResponse(radioResponseInfo);
    }

    @Override
    public void nvResetConfigResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.nvResetConfigResponse(radioResponseInfo);
    }

    @Override
    public void setUiccSubscriptionResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.setUiccSubscriptionResponse(radioResponseInfo);
    }

    @Override
    public void setDataAllowedResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.setDataAllowedResponse(radioResponseInfo);
    }

    @Override
    public void getHardwareConfigResponse(RadioResponseInfo radioResponseInfo, ArrayList<HardwareConfig> arrayList) {
        this.mRadioResponse.getHardwareConfigResponse(radioResponseInfo, arrayList);
    }

    @Override
    public void requestIccSimAuthenticationResponse(RadioResponseInfo radioResponseInfo, IccIoResult iccIoResult) {
        this.mRadioResponse.requestIccSimAuthenticationResponse(radioResponseInfo, iccIoResult);
    }

    @Override
    public void setDataProfileResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.setDataProfileResponse(radioResponseInfo);
    }

    @Override
    public void requestShutdownResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.requestShutdownResponse(radioResponseInfo);
    }

    @Override
    public void getRadioCapabilityResponse(RadioResponseInfo radioResponseInfo, RadioCapability radioCapability) {
        this.mRadioResponse.getRadioCapabilityResponse(radioResponseInfo, radioCapability);
    }

    @Override
    public void setRadioCapabilityResponse(RadioResponseInfo radioResponseInfo, RadioCapability radioCapability) {
        this.mRadioResponse.setRadioCapabilityResponse(radioResponseInfo, radioCapability);
    }

    @Override
    public void startLceServiceResponse(RadioResponseInfo radioResponseInfo, LceStatusInfo lceStatusInfo) {
        this.mRadioResponse.startLceServiceResponse(radioResponseInfo, lceStatusInfo);
    }

    @Override
    public void stopLceServiceResponse(RadioResponseInfo radioResponseInfo, LceStatusInfo lceStatusInfo) {
        this.mRadioResponse.stopLceServiceResponse(radioResponseInfo, lceStatusInfo);
    }

    @Override
    public void pullLceDataResponse(RadioResponseInfo radioResponseInfo, LceDataInfo lceDataInfo) {
        this.mRadioResponse.pullLceDataResponse(radioResponseInfo, lceDataInfo);
    }

    @Override
    public void getModemActivityInfoResponse(RadioResponseInfo radioResponseInfo, ActivityStatsInfo activityStatsInfo) {
        this.mRadioResponse.getModemActivityInfoResponse(radioResponseInfo, activityStatsInfo);
    }

    @Override
    public void setAllowedCarriersResponse(RadioResponseInfo radioResponseInfo, int i) {
        this.mRadioResponse.setAllowedCarriersResponse(radioResponseInfo, i);
    }

    @Override
    public void getAllowedCarriersResponse(RadioResponseInfo radioResponseInfo, boolean z, CarrierRestrictions carrierRestrictions) {
        this.mRadioResponse.getAllowedCarriersResponse(radioResponseInfo, z, carrierRestrictions);
    }

    @Override
    public void sendDeviceStateResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.sendDeviceStateResponse(radioResponseInfo);
    }

    @Override
    public void setIndicationFilterResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.setIndicationFilterResponse(radioResponseInfo);
    }

    @Override
    public void setSimCardPowerResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.setSimCardPowerResponse(radioResponseInfo);
    }

    @Override
    public void setSignalStrengthReportingCriteriaResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    @Override
    public void setLinkCapacityReportingCriteriaResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    @Override
    public void setTrmResponse(RadioResponseInfo radioResponseInfo) {
        RILRequest rILRequestProcessResponse = this.mMtkRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            if (radioResponseInfo.error == 0) {
                RadioResponse radioResponse = this.mRadioResponse;
                RadioResponse.sendMessageResponse(rILRequestProcessResponse.mResult, (Object) null);
            }
            this.mMtkRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, null);
        }
    }

    @Override
    public void getATRResponse(RadioResponseInfo radioResponseInfo, String str) {
        this.mRadioResponse.responseString(radioResponseInfo, str);
    }

    @Override
    public void getIccidResponse(RadioResponseInfo radioResponseInfo, String str) {
        this.mRadioResponse.responseString(radioResponseInfo, str);
    }

    @Override
    public void setSimPowerResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    @Override
    public void setNetworkSelectionModeManualWithActResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.setNetworkSelectionModeManualResponse(radioResponseInfo);
    }

    @Override
    public void getAvailableNetworksWithActResponse(RadioResponseInfo radioResponseInfo, ArrayList<OperatorInfoWithAct> arrayList) {
        responseOperatorInfosWithAct(radioResponseInfo, arrayList);
    }

    @Override
    public void getSignalStrengthWithWcdmaEcioResponse(RadioResponseInfo radioResponseInfo, SignalStrengthWithWcdmaEcio signalStrengthWithWcdmaEcio) {
        responseGetSignalStrengthWithWcdmaEcio(radioResponseInfo, signalStrengthWithWcdmaEcio);
    }

    @Override
    public void cancelAvailableNetworksResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    private int getSubId(int i) {
        int[] subId = SubscriptionManager.getSubId(i);
        if (subId != null && subId.length > 0) {
            return subId[0];
        }
        return -1;
    }

    private void responseOperatorInfosWithAct(RadioResponseInfo radioResponseInfo, ArrayList<OperatorInfoWithAct> arrayList) {
        RILRequest rILRequestProcessResponse = this.mRadioResponse.mRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            ArrayList arrayList2 = null;
            if (radioResponseInfo.error == 0) {
                arrayList2 = new ArrayList();
                for (int i = 0; i < arrayList.size(); i++) {
                    int i2 = -1;
                    this.mMtkRil.riljLog("responseOperatorInfosWithAct: act:" + arrayList.get(i).act);
                    this.mMtkRil.riljLog("responseOperatorInfosWithAct: lac:" + arrayList.get(i).lac);
                    if (arrayList.get(i).lac.length() > 0) {
                        i2 = Integer.parseInt(arrayList.get(i).lac, 16);
                    }
                    arrayList.get(i).base.alphaLong = this.mMtkRil.lookupOperatorName(getSubId(this.mMtkRil.mInstanceId.intValue()), arrayList.get(i).base.operatorNumeric, true, i2);
                    arrayList.get(i).base.alphaShort = this.mMtkRil.lookupOperatorName(getSubId(this.mMtkRil.mInstanceId.intValue()), arrayList.get(i).base.operatorNumeric, false, i2);
                    arrayList.get(i).base.alphaLong = arrayList.get(i).base.alphaLong.concat(" " + arrayList.get(i).act);
                    arrayList.get(i).base.alphaShort = arrayList.get(i).base.alphaShort.concat(" " + arrayList.get(i).act);
                    if (!this.mMtkRil.hidePLMN(arrayList.get(i).base.operatorNumeric)) {
                        String str = arrayList.get(i).base.alphaLong;
                        String str2 = arrayList.get(i).base.alphaShort;
                        String str3 = arrayList.get(i).base.operatorNumeric;
                        RadioResponse radioResponse = this.mRadioResponse;
                        arrayList2.add(new com.android.internal.telephony.OperatorInfo(str, str2, str3, RadioResponse.convertOpertatorInfoToString(arrayList.get(i).base.status)));
                    } else {
                        this.mMtkRil.riljLog("remove this one " + arrayList.get(i).base.operatorNumeric);
                    }
                }
                RadioResponse radioResponse2 = this.mRadioResponse;
                RadioResponse.sendMessageResponse(rILRequestProcessResponse.mResult, arrayList2);
            }
            this.mRadioResponse.mRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, arrayList2);
        }
    }

    private void responseGetSignalStrengthWithWcdmaEcio(RadioResponseInfo radioResponseInfo, SignalStrengthWithWcdmaEcio signalStrengthWithWcdmaEcio) {
        RILRequest rILRequest;
        android.telephony.SignalStrength signalStrength;
        MtkRadioResponse mtkRadioResponse;
        RILRequest rILRequestProcessResponse = this.mRadioResponse.mRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            android.telephony.SignalStrength signalStrength2 = new android.telephony.SignalStrength(signalStrengthWithWcdmaEcio.gsm_signalStrength, signalStrengthWithWcdmaEcio.gsm_bitErrorRate, signalStrengthWithWcdmaEcio.wcdma_rscp, signalStrengthWithWcdmaEcio.wcdma_ecio, signalStrengthWithWcdmaEcio.cdma_dbm, signalStrengthWithWcdmaEcio.cdma_ecio, signalStrengthWithWcdmaEcio.evdo_dbm, signalStrengthWithWcdmaEcio.evdo_ecio, signalStrengthWithWcdmaEcio.evdo_signalNoiseRatio, signalStrengthWithWcdmaEcio.lte_signalStrength, signalStrengthWithWcdmaEcio.lte_rsrp, signalStrengthWithWcdmaEcio.lte_rsrq, signalStrengthWithWcdmaEcio.lte_rssnr, signalStrengthWithWcdmaEcio.lte_cqi, signalStrengthWithWcdmaEcio.tdscdma_rscp);
            if (radioResponseInfo.error == 0) {
                mtkRadioResponse = this;
                RadioResponse radioResponse = mtkRadioResponse.mRadioResponse;
                rILRequest = rILRequestProcessResponse;
                signalStrength = signalStrength2;
                RadioResponse.sendMessageResponse(rILRequest.mResult, signalStrength);
            } else {
                rILRequest = rILRequestProcessResponse;
                signalStrength = signalStrength2;
                mtkRadioResponse = this;
            }
            mtkRadioResponse.mMtkRil.processResponseDone(rILRequest, radioResponseInfo, signalStrength);
        }
    }

    @Override
    public void setModemPowerResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    @Override
    public void getSmsParametersResponse(RadioResponseInfo radioResponseInfo, SmsParams smsParams) {
        responseSmsParams(radioResponseInfo, smsParams);
    }

    private void responseSmsParams(RadioResponseInfo radioResponseInfo, SmsParams smsParams) {
        RILRequest rILRequestProcessResponse = this.mMtkRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            MtkSmsParameters mtkSmsParameters = null;
            if (radioResponseInfo.error == 0) {
                mtkSmsParameters = new MtkSmsParameters(smsParams.format, smsParams.vp, smsParams.pid, smsParams.dcs);
                this.mMtkRil.riljLog("responseSmsParams: from HIDL: " + mtkSmsParameters);
                RadioResponse radioResponse = this.mRadioResponse;
                RadioResponse.sendMessageResponse(rILRequestProcessResponse.mResult, mtkSmsParameters);
            }
            this.mMtkRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, mtkSmsParameters);
        }
    }

    @Override
    public void setSmsParametersResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    @Override
    public void setEtwsResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    @Override
    public void removeCbMsgResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    @Override
    public void getSmsMemStatusResponse(RadioResponseInfo radioResponseInfo, SmsMemStatus smsMemStatus) {
        responseSmsMemStatus(radioResponseInfo, smsMemStatus);
    }

    private void responseSmsMemStatus(RadioResponseInfo radioResponseInfo, SmsMemStatus smsMemStatus) {
        RILRequest rILRequestProcessResponse = this.mMtkRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            MtkIccSmsStorageStatus mtkIccSmsStorageStatus = null;
            if (radioResponseInfo.error == 0) {
                mtkIccSmsStorageStatus = new MtkIccSmsStorageStatus(smsMemStatus.used, smsMemStatus.total);
                this.mMtkRil.riljLog("responseSmsMemStatus: from HIDL: " + mtkIccSmsStorageStatus);
                RadioResponse radioResponse = this.mRadioResponse;
                RadioResponse.sendMessageResponse(rILRequestProcessResponse.mResult, mtkIccSmsStorageStatus);
            }
            this.mMtkRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, mtkIccSmsStorageStatus);
        }
    }

    @Override
    public void setGsmBroadcastLangsResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    @Override
    public void getGsmBroadcastLangsResponse(RadioResponseInfo radioResponseInfo, String str) {
        this.mRadioResponse.responseString(radioResponseInfo, str);
    }

    @Override
    public void getGsmBroadcastActivationRsp(RadioResponseInfo radioResponseInfo, int i) {
        this.mRadioResponse.responseInts(radioResponseInfo, new int[]{i});
    }

    @Override
    public void setSmsFwkReadyRsp(RadioResponseInfo radioResponseInfo) {
        this.mMtkRil.riljLog("setSmsFwkReadyRsp: from HIDL");
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    @Override
    public void sendEmbmsAtCommandResponse(RadioResponseInfo radioResponseInfo, String str) {
        this.mRadioResponse.responseString(radioResponseInfo, str);
    }

    @Override
    public void hangupAllResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    @Override
    public void setCallIndicationResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    @Override
    public void emergencyDialResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    @Override
    public void setEccServiceCategoryResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    @Override
    public void setEccListResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    @Override
    public void setVoicePreferStatusResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    @Override
    public void setEccNumResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    @Override
    public void getEccNumResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    @Override
    public void currentStatusResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    @Override
    public void eccPreferredRatResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    @Override
    public void setApcModeResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    @Override
    public void getApcInfoResponse(RadioResponseInfo radioResponseInfo, ArrayList<Integer> arrayList) {
        RILRequest rILRequestProcessResponse = this.mMtkRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            int[] iArr = new int[arrayList.size()];
            if (radioResponseInfo.error == 0) {
                for (int i = 0; i < arrayList.size(); i++) {
                    iArr[i] = arrayList.get(i).intValue();
                }
                RadioResponse radioResponse = this.mRadioResponse;
                RadioResponse.sendMessageResponse(rILRequestProcessResponse.mResult, iArr);
            }
            this.mMtkRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, iArr);
        }
    }

    @Override
    public void triggerModeSwitchByEccResponse(RadioResponseInfo radioResponseInfo) {
        RILRequest rILRequestProcessResponse = this.mMtkRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            if (radioResponseInfo.error == 0) {
                RadioResponse radioResponse = this.mRadioResponse;
                RadioResponse.sendMessageResponse(rILRequestProcessResponse.mResult, (Object) null);
            }
            this.mMtkRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, null);
        }
    }

    @Override
    public void getSmsRuimMemoryStatusResponse(RadioResponseInfo radioResponseInfo, SmsMemStatus smsMemStatus) {
        RILRequest rILRequestProcessResponse = this.mMtkRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            MtkIccSmsStorageStatus mtkIccSmsStorageStatus = null;
            if (radioResponseInfo.error == 0) {
                mtkIccSmsStorageStatus = new MtkIccSmsStorageStatus(smsMemStatus.used, smsMemStatus.total);
                RadioResponse radioResponse = this.mRadioResponse;
                RadioResponse.sendMessageResponse(rILRequestProcessResponse.mResult, mtkIccSmsStorageStatus);
            }
            this.mMtkRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, mtkIccSmsStorageStatus);
        }
    }

    @Override
    public void setFdModeResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    @Override
    public void setResumeRegistrationResponse(RadioResponseInfo radioResponseInfo) {
        RILRequest rILRequestProcessResponse = this.mMtkRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            RadioResponse radioResponse = this.mRadioResponse;
            RadioResponse.sendMessageResponse(rILRequestProcessResponse.mResult, (Object) null);
        }
        this.mMtkRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, null);
    }

    @Override
    public void storeModemTypeResponse(RadioResponseInfo radioResponseInfo) {
        RILRequest rILRequestProcessResponse = this.mMtkRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            RadioResponse radioResponse = this.mRadioResponse;
            RadioResponse.sendMessageResponse(rILRequestProcessResponse.mResult, (Object) null);
        }
        this.mMtkRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, null);
    }

    @Override
    public void reloadModemTypeResponse(RadioResponseInfo radioResponseInfo) {
        RILRequest rILRequestProcessResponse = this.mMtkRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            RadioResponse radioResponse = this.mRadioResponse;
            RadioResponse.sendMessageResponse(rILRequestProcessResponse.mResult, (Object) null);
        }
        this.mMtkRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, null);
    }

    @Override
    public void handleStkCallSetupRequestFromSimWithResCodeResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    @Override
    public void setPdnReuseResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    @Override
    public void setOverrideApnResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    @Override
    public void setPdnNameReuseResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    @Override
    public void queryPhbStorageInfoResponse(RadioResponseInfo radioResponseInfo, ArrayList<Integer> arrayList) {
        this.mRadioResponse.responseIntArrayList(radioResponseInfo, arrayList);
    }

    @Override
    public void writePhbEntryResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    @Override
    public void readPhbEntryResponse(RadioResponseInfo radioResponseInfo, ArrayList<PhbEntryStructure> arrayList) {
        responsePhbEntries(radioResponseInfo, arrayList);
    }

    private void responsePhbEntries(RadioResponseInfo radioResponseInfo, ArrayList<PhbEntryStructure> arrayList) {
        RILRequest rILRequestProcessResponse = this.mMtkRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            PhbEntry[] phbEntryArr = null;
            if (radioResponseInfo.error == 0) {
                phbEntryArr = new PhbEntry[arrayList.size()];
                for (int i = 0; i < arrayList.size(); i++) {
                    phbEntryArr[i] = new PhbEntry();
                    phbEntryArr[i].type = arrayList.get(i).type;
                    phbEntryArr[i].index = arrayList.get(i).index;
                    phbEntryArr[i].number = arrayList.get(i).number;
                    phbEntryArr[i].ton = arrayList.get(i).ton;
                    phbEntryArr[i].alphaId = arrayList.get(i).alphaId;
                }
                RadioResponse radioResponse = this.mRadioResponse;
                RadioResponse.sendMessageResponse(rILRequestProcessResponse.mResult, phbEntryArr);
            }
            this.mMtkRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, phbEntryArr);
        }
    }

    @Override
    public void queryUPBCapabilityResponse(RadioResponseInfo radioResponseInfo, ArrayList<Integer> arrayList) {
        this.mRadioResponse.responseIntArrayList(radioResponseInfo, arrayList);
    }

    @Override
    public void editUPBEntryResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    @Override
    public void deleteUPBEntryResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    @Override
    public void readUPBGasListResponse(RadioResponseInfo radioResponseInfo, ArrayList<String> arrayList) {
        RadioResponse radioResponse = this.mRadioResponse;
        RadioResponse.responseStringArrayList(this.mRadioResponse.mRil, radioResponseInfo, arrayList);
    }

    @Override
    public void readUPBGrpEntryResponse(RadioResponseInfo radioResponseInfo, ArrayList<Integer> arrayList) {
        this.mRadioResponse.responseIntArrayList(radioResponseInfo, arrayList);
    }

    @Override
    public void writeUPBGrpEntryResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    @Override
    public void getPhoneBookStringsLengthResponse(RadioResponseInfo radioResponseInfo, ArrayList<Integer> arrayList) {
        this.mRadioResponse.responseIntArrayList(radioResponseInfo, arrayList);
    }

    @Override
    public void getPhoneBookMemStorageResponse(RadioResponseInfo radioResponseInfo, PhbMemStorageResponse phbMemStorageResponse) {
        responseGetPhbMemStorage(radioResponseInfo, phbMemStorageResponse);
    }

    private void responseGetPhbMemStorage(RadioResponseInfo radioResponseInfo, PhbMemStorageResponse phbMemStorageResponse) {
        RILRequest rILRequestProcessResponse = this.mMtkRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            PBMemStorage pBMemStorage = new PBMemStorage();
            if (radioResponseInfo.error == 0) {
                pBMemStorage.setStorage(phbMemStorageResponse.storage);
                pBMemStorage.setUsed(phbMemStorageResponse.used);
                pBMemStorage.setTotal(phbMemStorageResponse.total);
                RadioResponse radioResponse = this.mRadioResponse;
                RadioResponse.sendMessageResponse(rILRequestProcessResponse.mResult, pBMemStorage);
            }
            this.mMtkRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, pBMemStorage);
        }
    }

    @Override
    public void setPhoneBookMemStorageResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    @Override
    public void readPhoneBookEntryExtResponse(RadioResponseInfo radioResponseInfo, ArrayList<PhbEntryExt> arrayList) {
        responseReadPhbEntryExt(radioResponseInfo, arrayList);
    }

    private void responseCallForwardInfoEx(RadioResponseInfo radioResponseInfo, ArrayList<CallForwardInfoEx> arrayList) {
        RILRequest rILRequestProcessResponse = this.mMtkRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            MtkCallForwardInfo[] mtkCallForwardInfoArr = new MtkCallForwardInfo[arrayList.size()];
            for (int i = 0; i < arrayList.size(); i++) {
                long[] jArr = new long[2];
                mtkCallForwardInfoArr[i] = new MtkCallForwardInfo();
                mtkCallForwardInfoArr[i].status = arrayList.get(i).status;
                mtkCallForwardInfoArr[i].reason = arrayList.get(i).reason;
                mtkCallForwardInfoArr[i].serviceClass = arrayList.get(i).serviceClass;
                mtkCallForwardInfoArr[i].toa = arrayList.get(i).toa;
                mtkCallForwardInfoArr[i].number = arrayList.get(i).number;
                mtkCallForwardInfoArr[i].timeSeconds = arrayList.get(i).timeSeconds;
                String[] strArr = {arrayList.get(i).timeSlotBegin, arrayList.get(i).timeSlotEnd};
                if (strArr[0] == null || strArr[1] == null) {
                    mtkCallForwardInfoArr[i].timeSlot = null;
                } else {
                    for (int i2 = 0; i2 < 2; i2++) {
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
                        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT+8"));
                        try {
                            jArr[i2] = simpleDateFormat.parse(strArr[i2]).getTime();
                        } catch (ParseException e) {
                            e.printStackTrace();
                            jArr = null;
                        }
                    }
                    mtkCallForwardInfoArr[i].timeSlot = jArr;
                }
            }
            if (radioResponseInfo.error == 0) {
                RadioResponse radioResponse = this.mRadioResponse;
                RadioResponse.sendMessageResponse(rILRequestProcessResponse.mResult, mtkCallForwardInfoArr);
            }
            this.mMtkRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, mtkCallForwardInfoArr);
        }
    }

    private void responseReadPhbEntryExt(RadioResponseInfo radioResponseInfo, ArrayList<PhbEntryExt> arrayList) {
        RILRequest rILRequestProcessResponse = this.mMtkRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            PBEntry[] pBEntryArr = null;
            if (radioResponseInfo.error == 0) {
                pBEntryArr = new PBEntry[arrayList.size()];
                for (int i = 0; i < arrayList.size(); i++) {
                    pBEntryArr[i] = new PBEntry();
                    pBEntryArr[i].setIndex1(arrayList.get(i).type);
                    pBEntryArr[i].setNumber(arrayList.get(i).number);
                    pBEntryArr[i].setType(arrayList.get(i).type);
                    pBEntryArr[i].setText(arrayList.get(i).text);
                    pBEntryArr[i].setHidden(arrayList.get(i).hidden);
                    pBEntryArr[i].setGroup(arrayList.get(i).group);
                    pBEntryArr[i].setAdnumber(arrayList.get(i).adnumber);
                    pBEntryArr[i].setAdtype(arrayList.get(i).adtype);
                    pBEntryArr[i].setSecondtext(arrayList.get(i).secondtext);
                    pBEntryArr[i].setEmail(arrayList.get(i).email);
                }
                RadioResponse radioResponse = this.mRadioResponse;
                RadioResponse.sendMessageResponse(rILRequestProcessResponse.mResult, pBEntryArr);
            }
            this.mMtkRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, pBEntryArr);
        }
    }

    @Override
    public void writePhoneBookEntryExtResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    @Override
    public void queryUPBAvailableResponse(RadioResponseInfo radioResponseInfo, ArrayList<Integer> arrayList) {
        this.mRadioResponse.responseIntArrayList(radioResponseInfo, arrayList);
    }

    @Override
    public void readUPBEmailEntryResponse(RadioResponseInfo radioResponseInfo, String str) {
        RILRequest rILRequestProcessResponse = this.mMtkRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            if (radioResponseInfo.error == 0) {
                RadioResponse radioResponse = this.mRadioResponse;
                RadioResponse.sendMessageResponse(rILRequestProcessResponse.mResult, str);
            }
            if (isUserLoad && radioResponseInfo.error == 0) {
                this.mMtkRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, "xxx@email.com");
            } else {
                this.mMtkRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, str);
            }
        }
    }

    @Override
    public void readUPBSneEntryResponse(RadioResponseInfo radioResponseInfo, String str) {
        this.mRadioResponse.responseString(radioResponseInfo, str);
    }

    @Override
    public void readUPBAnrEntryResponse(RadioResponseInfo radioResponseInfo, ArrayList<PhbEntryStructure> arrayList) {
        responsePhbEntries(radioResponseInfo, arrayList);
    }

    @Override
    public void readUPBAasListResponse(RadioResponseInfo radioResponseInfo, ArrayList<String> arrayList) {
        RadioResponse radioResponse = this.mRadioResponse;
        RadioResponse.responseStringArrayList(this.mRadioResponse.mRil, radioResponseInfo, arrayList);
    }

    @Override
    public void setPhonebookReadyResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    @Override
    public void resetRadioResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    @Override
    public void restartRILDResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    @Override
    public void getFemtocellListResponse(RadioResponseInfo radioResponseInfo, ArrayList<String> arrayList) {
        responseFemtoCellInfos(radioResponseInfo, arrayList);
    }

    @Override
    public void abortFemtocellListResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    @Override
    public void selectFemtocellResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    @Override
    public void queryFemtoCellSystemSelectionModeResponse(RadioResponseInfo radioResponseInfo, int i) {
        this.mRadioResponse.responseInts(radioResponseInfo, new int[]{i});
    }

    @Override
    public void queryNetworkLockResponse(RadioResponseInfo radioResponseInfo, int i, int i2, int i3, int i4, int i5, int i6, int i7) {
        this.mRadioResponse.responseInts(radioResponseInfo, new int[]{i, i2, i3, i4, i5, i6, i7});
    }

    @Override
    public void setNetworkLockResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    @Override
    public void supplyDeviceNetworkDepersonalizationResponse(RadioResponseInfo radioResponseInfo, int i) {
        this.mRadioResponse.responseInts(radioResponseInfo, new int[]{i});
    }

    @Override
    public void setFemtoCellSystemSelectionModeResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    private void responseFemtoCellInfos(RadioResponseInfo radioResponseInfo, ArrayList<String> arrayList) {
        ArrayList arrayList2;
        String str;
        RILRequest rILRequestProcessResponse = this.mRadioResponse.mRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null && radioResponseInfo.error == 0) {
            String[] strArr = new String[arrayList.size()];
            for (int i = 0; i < arrayList.size(); i++) {
                strArr[i] = arrayList.get(i);
            }
            if (strArr.length % 6 != 0) {
                throw new RuntimeException("responseFemtoCellInfos: invalid response. Got " + strArr.length + " strings, expected multible of 6");
            }
            arrayList2 = new ArrayList(strArr.length / 6);
            for (int i2 = 0; i2 < strArr.length; i2 += 6) {
                int i3 = i2 + 1;
                int i4 = 1;
                if (strArr[i3] != null && strArr[i3].startsWith("uCs2")) {
                    this.mMtkRil.riljLog("responseFemtoCellInfos handling UCS2 format name");
                    try {
                        strArr[i2 + 0] = new String(IccUtils.hexStringToBytes(strArr[i3].substring(4)), "UTF-16");
                    } catch (UnsupportedEncodingException e) {
                        this.mMtkRil.riljLog("responseFemtoCellInfos UnsupportedEncodingException");
                    }
                }
                if (strArr[i3] != null && (strArr[i3].equals("") || strArr[i3].equals(strArr[i2 + 0]))) {
                    MtkRIL mtkRIL = this.mMtkRil;
                    StringBuilder sb = new StringBuilder();
                    sb.append("lookup RIL responseFemtoCellInfos() for plmn id= ");
                    int i5 = i2 + 0;
                    sb.append(strArr[i5]);
                    mtkRIL.riljLog(sb.toString());
                    strArr[i3] = this.mMtkRil.lookupOperatorName(getSubId(this.mMtkRil.mInstanceId.intValue()), strArr[i5], true, -1);
                }
                int i6 = i2 + 2;
                if (strArr[i6].equals(MtkGsmCdmaPhone.ACT_TYPE_LTE)) {
                    str = MtkGsmCdmaPhone.LTE_INDICATOR;
                    i4 = 14;
                } else if (strArr[i6].equals(MtkGsmCdmaPhone.ACT_TYPE_UTRAN)) {
                    str = MtkGsmCdmaPhone.UTRAN_INDICATOR;
                    i4 = 3;
                } else {
                    str = "2G";
                }
                int i7 = i4;
                strArr[i3] = strArr[i3].concat(" " + str);
                int i8 = i2 + 5;
                String str2 = new String(IccUtils.hexStringToBytes(strArr[i8]));
                MtkRIL mtkRIL2 = this.mMtkRil;
                StringBuilder sb2 = new StringBuilder();
                sb2.append("FemtoCellInfo(");
                int i9 = i2 + 3;
                sb2.append(strArr[i9]);
                sb2.append(",");
                int i10 = i2 + 4;
                sb2.append(strArr[i10]);
                sb2.append(",");
                sb2.append(strArr[i8]);
                sb2.append(",");
                int i11 = i2 + 0;
                sb2.append(strArr[i11]);
                sb2.append(",");
                sb2.append(strArr[i3]);
                sb2.append(",");
                sb2.append(i7);
                sb2.append(")hnbName=");
                sb2.append(str2);
                mtkRIL2.riljLog(sb2.toString());
                arrayList2.add(new FemtoCellInfo(Integer.parseInt(strArr[i9]), Integer.parseInt(strArr[i10]), str2, strArr[i11], strArr[i3], i7));
            }
            RadioResponse radioResponse = this.mRadioResponse;
            RadioResponse.sendMessageResponse(rILRequestProcessResponse.mResult, arrayList2);
        } else {
            arrayList2 = null;
        }
        this.mMtkRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, arrayList2);
    }

    @Override
    public void setLteAccessStratumReportResponse(RadioResponseInfo radioResponseInfo) {
        RILRequest rILRequestProcessResponse = this.mMtkRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            if (radioResponseInfo.error == 0) {
                RadioResponse radioResponse = this.mRadioResponse;
                RadioResponse.sendMessageResponse(rILRequestProcessResponse.mResult, (Object) null);
            }
            this.mMtkRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, null);
        }
    }

    @Override
    public void setLteUplinkDataTransferResponse(RadioResponseInfo radioResponseInfo) {
        RILRequest rILRequestProcessResponse = this.mMtkRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            if (radioResponseInfo.error == 0) {
                RadioResponse radioResponse = this.mRadioResponse;
                RadioResponse.sendMessageResponse(rILRequestProcessResponse.mResult, (Object) null);
            }
            this.mMtkRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, null);
        }
    }

    @Override
    public void setRxTestConfigResponse(RadioResponseInfo radioResponseInfo, ArrayList<Integer> arrayList) {
        this.mRadioResponse.responseIntArrayList(radioResponseInfo, arrayList);
    }

    @Override
    public void getRxTestResultResponse(RadioResponseInfo radioResponseInfo, ArrayList<Integer> arrayList) {
        this.mRadioResponse.responseIntArrayList(radioResponseInfo, arrayList);
    }

    @Override
    public void getPOLCapabilityResponse(RadioResponseInfo radioResponseInfo, ArrayList<Integer> arrayList) {
        this.mRadioResponse.responseIntArrayList(radioResponseInfo, arrayList);
    }

    @Override
    public void getCurrentPOLListResponse(RadioResponseInfo radioResponseInfo, ArrayList<String> arrayList) {
        ArrayList arrayList2;
        String str;
        String strLookupOperatorName;
        RILRequest rILRequestProcessResponse = this.mMtkRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            if (radioResponseInfo.error == 0) {
                if (arrayList.size() % 4 != 0) {
                    this.mMtkRil.riljLog("RIL_REQUEST_GET_POL_LIST: invalid response. Got " + arrayList.size() + " strings, expected multible of 4");
                    arrayList2 = null;
                } else {
                    arrayList2 = new ArrayList(arrayList.size() / 4);
                    for (int i = 0; i < arrayList.size(); i += 4) {
                        int i2 = arrayList.get(i) != null ? Integer.parseInt(arrayList.get(i)) : 0;
                        int i3 = i + 1;
                        if (arrayList.get(i3) != null) {
                            switch (Integer.parseInt(arrayList.get(i3))) {
                                case 0:
                                case 1:
                                    strLookupOperatorName = arrayList.get(i + 2);
                                    str = null;
                                    break;
                                case 2:
                                    int i4 = i + 2;
                                    if (arrayList.get(i4) != null) {
                                        str = arrayList.get(i4);
                                        strLookupOperatorName = this.mMtkRil.lookupOperatorName(getSubId(this.mMtkRil.mInstanceId.intValue()), str, true, -1);
                                        break;
                                    }
                                default:
                                    str = null;
                                    strLookupOperatorName = null;
                                    break;
                            }
                        }
                        int i5 = i + 3;
                        int i6 = arrayList.get(i5) != null ? Integer.parseInt(arrayList.get(i5)) : 0;
                        if (str != null && !str.equals("?????")) {
                            arrayList2.add(new NetworkInfoWithAcT(strLookupOperatorName, str, i6, i2));
                        }
                    }
                    RadioResponse radioResponse = this.mRadioResponse;
                    RadioResponse.sendMessageResponse(rILRequestProcessResponse.mResult, arrayList2);
                }
            } else {
                arrayList2 = null;
            }
            this.mMtkRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, arrayList2);
        }
    }

    @Override
    public void setPOLEntryResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    @Override
    public void syncDataSettingsToMdResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    @Override
    public void resetMdDataRetryCountResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    @Override
    public void setRemoveRestrictEutranModeResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    @Override
    public void setRoamingEnableResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    @Override
    public void getRoamingEnableResponse(RadioResponseInfo radioResponseInfo, ArrayList<Integer> arrayList) {
        this.mRadioResponse.responseIntArrayList(radioResponseInfo, arrayList);
    }

    @Override
    public void setLteReleaseVersionResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    @Override
    public void getLteReleaseVersionResponse(RadioResponseInfo radioResponseInfo, int i) {
        this.mRadioResponse.responseInts(radioResponseInfo, new int[]{i});
    }

    @Override
    public void vsimNotificationResponse(RadioResponseInfo radioResponseInfo, VsimEvent vsimEvent) {
        RILRequest rILRequestProcessResponse = this.mMtkRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            Integer numValueOf = null;
            if (radioResponseInfo.error == 0) {
                numValueOf = Integer.valueOf(vsimEvent.transactionId);
                RadioResponse radioResponse = this.mRadioResponse;
                RadioResponse.sendMessageResponse(rILRequestProcessResponse.mResult, numValueOf);
            }
            this.mMtkRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, numValueOf);
        }
    }

    @Override
    public void vsimOperationResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    @Override
    public void setWifiEnabledResponse(RadioResponseInfo radioResponseInfo) {
        RILRequest rILRequestProcessResponse = this.mMtkRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            RadioResponse radioResponse = this.mRadioResponse;
            RadioResponse.sendMessageResponse(rILRequestProcessResponse.mResult, (Object) null);
        }
        this.mMtkRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, null);
    }

    @Override
    public void setWifiAssociatedResponse(RadioResponseInfo radioResponseInfo) {
        RILRequest rILRequestProcessResponse = this.mMtkRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            RadioResponse radioResponse = this.mRadioResponse;
            RadioResponse.sendMessageResponse(rILRequestProcessResponse.mResult, (Object) null);
        }
        this.mMtkRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, null);
    }

    @Override
    public void setWifiSignalLevelResponse(RadioResponseInfo radioResponseInfo) {
        RILRequest rILRequestProcessResponse = this.mMtkRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            RadioResponse radioResponse = this.mRadioResponse;
            RadioResponse.sendMessageResponse(rILRequestProcessResponse.mResult, (Object) null);
        }
        this.mMtkRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, null);
    }

    @Override
    public void setWifiIpAddressResponse(RadioResponseInfo radioResponseInfo) {
        RILRequest rILRequestProcessResponse = this.mMtkRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            RadioResponse radioResponse = this.mRadioResponse;
            RadioResponse.sendMessageResponse(rILRequestProcessResponse.mResult, (Object) null);
        }
        this.mMtkRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, null);
    }

    @Override
    public void setLocationInfoResponse(RadioResponseInfo radioResponseInfo) {
        RILRequest rILRequestProcessResponse = this.mMtkRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            RadioResponse radioResponse = this.mRadioResponse;
            RadioResponse.sendMessageResponse(rILRequestProcessResponse.mResult, (Object) null);
        }
        this.mMtkRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, null);
    }

    @Override
    public void setEmergencyAddressIdResponse(RadioResponseInfo radioResponseInfo) {
        RILRequest rILRequestProcessResponse = this.mMtkRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            RadioResponse radioResponse = this.mRadioResponse;
            RadioResponse.sendMessageResponse(rILRequestProcessResponse.mResult, (Object) null);
        }
        this.mMtkRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, null);
    }

    @Override
    public void setNattKeepAliveStatusResponse(RadioResponseInfo radioResponseInfo) {
        RILRequest rILRequestProcessResponse = this.mMtkRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            RadioResponse radioResponse = this.mRadioResponse;
            RadioResponse.sendMessageResponse(rILRequestProcessResponse.mResult, (Object) null);
        }
        this.mMtkRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, null);
    }

    @Override
    public void setWifiPingResultResponse(RadioResponseInfo radioResponseInfo) {
        RILRequest rILRequestProcessResponse = this.mMtkRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            RadioResponse radioResponse = this.mRadioResponse;
            RadioResponse.sendMessageResponse(rILRequestProcessResponse.mResult, (Object) null);
        }
        this.mMtkRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, null);
    }

    @Override
    public void setE911StateResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    @Override
    public void setServiceStateToModemResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    @Override
    public void sendRequestRawResponse(RadioResponseInfo radioResponseInfo, ArrayList<Byte> arrayList) {
        RILRequest rILRequestProcessResponse = this.mMtkRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            byte[] bArrArrayListToPrimitiveArray = null;
            if (radioResponseInfo.error == 0) {
                bArrArrayListToPrimitiveArray = RIL.arrayListToPrimitiveArray(arrayList);
                RadioResponse radioResponse = this.mRadioResponse;
                RadioResponse.sendMessageResponse(rILRequestProcessResponse.mResult, bArrArrayListToPrimitiveArray);
            }
            this.mMtkRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, bArrArrayListToPrimitiveArray);
        }
    }

    public void setTxPowerStatusResponse(RadioResponseInfo radioResponseInfo, ArrayList<Byte> arrayList) {
        RILRequest rILRequestProcessResponse = this.mMtkRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            if (radioResponseInfo.error == 0) {
                RadioResponse radioResponse = this.mRadioResponse;
                RadioResponse.sendMessageResponse(rILRequestProcessResponse.mResult, (Object) null);
            }
            this.mMtkRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, null);
            return;
        }
        this.mMtkRil.riljLog("setTxPowerStatusResponse, rr is null");
    }

    @Override
    public void sendRequestStringsResponse(RadioResponseInfo radioResponseInfo, ArrayList<String> arrayList) {
        RadioResponse radioResponse = this.mRadioResponse;
        RadioResponse.responseStringArrayList(this.mMtkRil, radioResponseInfo, arrayList);
    }

    @Override
    public void dataConnectionAttachResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    @Override
    public void dataConnectionDetachResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    @Override
    public void resetAllConnectionsResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    @Override
    public void reportAirplaneModeResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    @Override
    public void reportSimModeResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    @Override
    public void setSilentRebootResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    @Override
    public void setPropImsHandoverResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    @Override
    public void setOperatorConfigurationResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    @Override
    public void setSuppServPropertyResponse(RadioResponseInfo radioResponseInfo) {
        this.mRadioResponse.responseVoid(radioResponseInfo);
    }

    @Override
    public void getSuppServPropertyResponse(RadioResponseInfo radioResponseInfo, String str) {
        this.mRadioResponse.responseString(radioResponseInfo, str);
    }
}
