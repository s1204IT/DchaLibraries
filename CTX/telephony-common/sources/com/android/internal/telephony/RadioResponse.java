package com.android.internal.telephony;

import android.hardware.radio.V1_0.ActivityStatsInfo;
import android.hardware.radio.V1_0.AppStatus;
import android.hardware.radio.V1_0.CardStatus;
import android.hardware.radio.V1_0.CarrierRestrictions;
import android.hardware.radio.V1_0.CdmaBroadcastSmsConfigInfo;
import android.hardware.radio.V1_0.CellInfo;
import android.hardware.radio.V1_0.DataRegStateResult;
import android.hardware.radio.V1_0.GsmBroadcastSmsConfigInfo;
import android.hardware.radio.V1_0.IccIoResult;
import android.hardware.radio.V1_0.LastCallFailCauseInfo;
import android.hardware.radio.V1_0.LceDataInfo;
import android.hardware.radio.V1_0.LceStatusInfo;
import android.hardware.radio.V1_0.NeighboringCell;
import android.hardware.radio.V1_0.OperatorInfo;
import android.hardware.radio.V1_0.RadioResponseInfo;
import android.hardware.radio.V1_0.SendSmsResult;
import android.hardware.radio.V1_0.SetupDataCallResult;
import android.hardware.radio.V1_0.SignalStrength;
import android.hardware.radio.V1_0.VoiceRegStateResult;
import android.hardware.radio.V1_1.KeepaliveStatus;
import android.hardware.radio.V1_2.IRadioResponse;
import android.os.AsyncResult;
import android.os.Message;
import android.os.SystemClock;
import android.telephony.ModemActivityInfo;
import android.telephony.NeighboringCellInfo;
import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Base64;
import com.android.internal.telephony.gsm.SmsBroadcastConfigInfo;
import com.android.internal.telephony.uicc.IccCardApplicationStatus;
import com.android.internal.telephony.uicc.IccCardStatus;
import com.android.internal.telephony.uicc.IccUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RadioResponse extends IRadioResponse.Stub {
    private static final int CDMA_BROADCAST_SMS_NO_OF_SERVICE_CATEGORIES = 31;
    private static final int CDMA_BSI_NO_OF_INTS_STRUCT = 3;
    public RIL mRil;

    public RadioResponse(RIL ril) {
        this.mRil = ril;
    }

    public static void sendMessageResponse(Message message, Object obj) {
        if (message != null) {
            AsyncResult.forMessage(message, obj, (Throwable) null);
            message.sendToTarget();
        }
    }

    @Override
    public void acknowledgeRequest(int i) {
        this.mRil.processRequestAck(i);
    }

    @Override
    public void getIccCardStatusResponse(RadioResponseInfo radioResponseInfo, CardStatus cardStatus) {
        responseIccCardStatus(radioResponseInfo, cardStatus);
    }

    @Override
    public void getIccCardStatusResponse_1_2(RadioResponseInfo radioResponseInfo, android.hardware.radio.V1_2.CardStatus cardStatus) {
        responseIccCardStatus_1_2(radioResponseInfo, cardStatus);
    }

    @Override
    public void supplyIccPinForAppResponse(RadioResponseInfo radioResponseInfo, int i) {
        responseInts(radioResponseInfo, i);
    }

    @Override
    public void supplyIccPukForAppResponse(RadioResponseInfo radioResponseInfo, int i) {
        responseInts(radioResponseInfo, i);
    }

    @Override
    public void supplyIccPin2ForAppResponse(RadioResponseInfo radioResponseInfo, int i) {
        responseInts(radioResponseInfo, i);
    }

    @Override
    public void supplyIccPuk2ForAppResponse(RadioResponseInfo radioResponseInfo, int i) {
        responseInts(radioResponseInfo, i);
    }

    @Override
    public void changeIccPinForAppResponse(RadioResponseInfo radioResponseInfo, int i) {
        responseInts(radioResponseInfo, i);
    }

    @Override
    public void changeIccPin2ForAppResponse(RadioResponseInfo radioResponseInfo, int i) {
        responseInts(radioResponseInfo, i);
    }

    @Override
    public void supplyNetworkDepersonalizationResponse(RadioResponseInfo radioResponseInfo, int i) {
        responseInts(radioResponseInfo, i);
    }

    @Override
    public void getCurrentCallsResponse(RadioResponseInfo radioResponseInfo, ArrayList<android.hardware.radio.V1_0.Call> arrayList) {
        responseCurrentCalls(radioResponseInfo, arrayList);
    }

    @Override
    public void getCurrentCallsResponse_1_2(RadioResponseInfo radioResponseInfo, ArrayList<android.hardware.radio.V1_2.Call> arrayList) {
        responseCurrentCalls_1_2(radioResponseInfo, arrayList);
    }

    @Override
    public void dialResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void getIMSIForAppResponse(RadioResponseInfo radioResponseInfo, String str) {
        responseString(radioResponseInfo, str);
    }

    @Override
    public void hangupConnectionResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void hangupWaitingOrBackgroundResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void hangupForegroundResumeBackgroundResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void switchWaitingOrHoldingAndActiveResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void conferenceResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void rejectCallResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void getLastCallFailCauseResponse(RadioResponseInfo radioResponseInfo, LastCallFailCauseInfo lastCallFailCauseInfo) {
        responseLastCallFailCauseInfo(radioResponseInfo, lastCallFailCauseInfo);
    }

    @Override
    public void getSignalStrengthResponse(RadioResponseInfo radioResponseInfo, SignalStrength signalStrength) {
        responseSignalStrength(radioResponseInfo, signalStrength);
    }

    @Override
    public void getSignalStrengthResponse_1_2(RadioResponseInfo radioResponseInfo, android.hardware.radio.V1_2.SignalStrength signalStrength) {
        responseSignalStrength_1_2(radioResponseInfo, signalStrength);
    }

    @Override
    public void getVoiceRegistrationStateResponse(RadioResponseInfo radioResponseInfo, VoiceRegStateResult voiceRegStateResult) {
        RILRequest rILRequestProcessResponse = this.mRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            if (radioResponseInfo.error == 0) {
                sendMessageResponse(rILRequestProcessResponse.mResult, voiceRegStateResult);
            }
            this.mRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, voiceRegStateResult);
        }
    }

    @Override
    public void getVoiceRegistrationStateResponse_1_2(RadioResponseInfo radioResponseInfo, android.hardware.radio.V1_2.VoiceRegStateResult voiceRegStateResult) {
        RILRequest rILRequestProcessResponse = this.mRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            if (radioResponseInfo.error == 0) {
                sendMessageResponse(rILRequestProcessResponse.mResult, voiceRegStateResult);
            }
            this.mRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, voiceRegStateResult);
        }
    }

    @Override
    public void getDataRegistrationStateResponse(RadioResponseInfo radioResponseInfo, DataRegStateResult dataRegStateResult) {
        RILRequest rILRequestProcessResponse = this.mRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            if (radioResponseInfo.error == 0) {
                sendMessageResponse(rILRequestProcessResponse.mResult, dataRegStateResult);
            }
            this.mRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, dataRegStateResult);
        }
    }

    @Override
    public void getDataRegistrationStateResponse_1_2(RadioResponseInfo radioResponseInfo, android.hardware.radio.V1_2.DataRegStateResult dataRegStateResult) {
        RILRequest rILRequestProcessResponse = this.mRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            if (radioResponseInfo.error == 0) {
                sendMessageResponse(rILRequestProcessResponse.mResult, dataRegStateResult);
            }
            this.mRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, dataRegStateResult);
        }
    }

    @Override
    public void getOperatorResponse(RadioResponseInfo radioResponseInfo, String str, String str2, String str3) {
        responseStrings(radioResponseInfo, str, str2, str3);
    }

    @Override
    public void setRadioPowerResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void sendDtmfResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void sendSmsResponse(RadioResponseInfo radioResponseInfo, SendSmsResult sendSmsResult) {
        responseSms(radioResponseInfo, sendSmsResult);
    }

    @Override
    public void sendSMSExpectMoreResponse(RadioResponseInfo radioResponseInfo, SendSmsResult sendSmsResult) {
        responseSms(radioResponseInfo, sendSmsResult);
    }

    @Override
    public void setupDataCallResponse(RadioResponseInfo radioResponseInfo, SetupDataCallResult setupDataCallResult) {
        responseSetupDataCall(radioResponseInfo, setupDataCallResult);
    }

    @Override
    public void iccIOForAppResponse(RadioResponseInfo radioResponseInfo, IccIoResult iccIoResult) {
        responseIccIo(radioResponseInfo, iccIoResult);
    }

    @Override
    public void sendUssdResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void cancelPendingUssdResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void getClirResponse(RadioResponseInfo radioResponseInfo, int i, int i2) {
        responseInts(radioResponseInfo, i, i2);
    }

    @Override
    public void setClirResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void getCallForwardStatusResponse(RadioResponseInfo radioResponseInfo, ArrayList<android.hardware.radio.V1_0.CallForwardInfo> arrayList) {
        responseCallForwardInfo(radioResponseInfo, arrayList);
    }

    @Override
    public void setCallForwardResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void getCallWaitingResponse(RadioResponseInfo radioResponseInfo, boolean z, int i) {
        responseInts(radioResponseInfo, z ? 1 : 0, i);
    }

    @Override
    public void setCallWaitingResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void acknowledgeLastIncomingGsmSmsResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void acceptCallResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void deactivateDataCallResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void getFacilityLockForAppResponse(RadioResponseInfo radioResponseInfo, int i) {
        responseInts(radioResponseInfo, i);
    }

    @Override
    public void setFacilityLockForAppResponse(RadioResponseInfo radioResponseInfo, int i) {
        responseInts(radioResponseInfo, i);
    }

    @Override
    public void setBarringPasswordResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void getNetworkSelectionModeResponse(RadioResponseInfo radioResponseInfo, boolean z) {
        responseInts(radioResponseInfo, z ? 1 : 0);
    }

    @Override
    public void setNetworkSelectionModeAutomaticResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void setNetworkSelectionModeManualResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void getAvailableNetworksResponse(RadioResponseInfo radioResponseInfo, ArrayList<OperatorInfo> arrayList) {
        responseOperatorInfos(radioResponseInfo, arrayList);
    }

    @Override
    public void startNetworkScanResponse(RadioResponseInfo radioResponseInfo) {
        responseScanStatus(radioResponseInfo);
    }

    @Override
    public void stopNetworkScanResponse(RadioResponseInfo radioResponseInfo) {
        responseScanStatus(radioResponseInfo);
    }

    @Override
    public void startDtmfResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void stopDtmfResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void getBasebandVersionResponse(RadioResponseInfo radioResponseInfo, String str) {
        responseString(radioResponseInfo, str);
    }

    @Override
    public void separateConnectionResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void setMuteResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void getMuteResponse(RadioResponseInfo radioResponseInfo, boolean z) {
        responseInts(radioResponseInfo, z ? 1 : 0);
    }

    @Override
    public void getClipResponse(RadioResponseInfo radioResponseInfo, int i) {
        responseInts(radioResponseInfo, i);
    }

    @Override
    public void getDataCallListResponse(RadioResponseInfo radioResponseInfo, ArrayList<SetupDataCallResult> arrayList) {
        responseDataCallList(radioResponseInfo, arrayList);
    }

    public void sendOemRilRequestRawResponse(RadioResponseInfo radioResponseInfo, ArrayList<Byte> arrayList) {
    }

    @Override
    public void setSuppServiceNotificationsResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void writeSmsToSimResponse(RadioResponseInfo radioResponseInfo, int i) {
        responseInts(radioResponseInfo, i);
    }

    @Override
    public void deleteSmsOnSimResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void setBandModeResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void getAvailableBandModesResponse(RadioResponseInfo radioResponseInfo, ArrayList<Integer> arrayList) {
        responseIntArrayList(radioResponseInfo, arrayList);
    }

    @Override
    public void sendEnvelopeResponse(RadioResponseInfo radioResponseInfo, String str) {
        responseString(radioResponseInfo, str);
    }

    @Override
    public void sendTerminalResponseToSimResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void handleStkCallSetupRequestFromSimResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void explicitCallTransferResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void setPreferredNetworkTypeResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void getPreferredNetworkTypeResponse(RadioResponseInfo radioResponseInfo, int i) {
        this.mRil.mPreferredNetworkType = i;
        responseInts(radioResponseInfo, i);
    }

    @Override
    public void getNeighboringCidsResponse(RadioResponseInfo radioResponseInfo, ArrayList<NeighboringCell> arrayList) {
        responseCellList(radioResponseInfo, arrayList);
    }

    @Override
    public void setLocationUpdatesResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void setCdmaSubscriptionSourceResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void setCdmaRoamingPreferenceResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void getCdmaRoamingPreferenceResponse(RadioResponseInfo radioResponseInfo, int i) {
        responseInts(radioResponseInfo, i);
    }

    @Override
    public void setTTYModeResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void getTTYModeResponse(RadioResponseInfo radioResponseInfo, int i) {
        responseInts(radioResponseInfo, i);
    }

    @Override
    public void setPreferredVoicePrivacyResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void getPreferredVoicePrivacyResponse(RadioResponseInfo radioResponseInfo, boolean z) {
        responseInts(radioResponseInfo, z ? 1 : 0);
    }

    @Override
    public void sendCDMAFeatureCodeResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void sendBurstDtmfResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void sendCdmaSmsResponse(RadioResponseInfo radioResponseInfo, SendSmsResult sendSmsResult) {
        responseSms(radioResponseInfo, sendSmsResult);
    }

    @Override
    public void acknowledgeLastIncomingCdmaSmsResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void getGsmBroadcastConfigResponse(RadioResponseInfo radioResponseInfo, ArrayList<GsmBroadcastSmsConfigInfo> arrayList) {
        responseGmsBroadcastConfig(radioResponseInfo, arrayList);
    }

    @Override
    public void setGsmBroadcastConfigResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void setGsmBroadcastActivationResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void getCdmaBroadcastConfigResponse(RadioResponseInfo radioResponseInfo, ArrayList<CdmaBroadcastSmsConfigInfo> arrayList) {
        responseCdmaBroadcastConfig(radioResponseInfo, arrayList);
    }

    @Override
    public void setCdmaBroadcastConfigResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void setCdmaBroadcastActivationResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void getCDMASubscriptionResponse(RadioResponseInfo radioResponseInfo, String str, String str2, String str3, String str4, String str5) {
        responseStrings(radioResponseInfo, str, str2, str3, str4, str5);
    }

    @Override
    public void writeSmsToRuimResponse(RadioResponseInfo radioResponseInfo, int i) {
        responseInts(radioResponseInfo, i);
    }

    @Override
    public void deleteSmsOnRuimResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void getDeviceIdentityResponse(RadioResponseInfo radioResponseInfo, String str, String str2, String str3, String str4) {
        responseStrings(radioResponseInfo, str, str2, str3, str4);
    }

    @Override
    public void exitEmergencyCallbackModeResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void getSmscAddressResponse(RadioResponseInfo radioResponseInfo, String str) {
        responseString(radioResponseInfo, str);
    }

    @Override
    public void setSmscAddressResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void reportSmsMemoryStatusResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void reportStkServiceIsRunningResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void getCdmaSubscriptionSourceResponse(RadioResponseInfo radioResponseInfo, int i) {
        responseInts(radioResponseInfo, i);
    }

    @Override
    public void requestIsimAuthenticationResponse(RadioResponseInfo radioResponseInfo, String str) {
        throw new RuntimeException("Inexplicable response received for requestIsimAuthentication");
    }

    @Override
    public void acknowledgeIncomingGsmSmsWithPduResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void sendEnvelopeWithStatusResponse(RadioResponseInfo radioResponseInfo, IccIoResult iccIoResult) {
        responseIccIo(radioResponseInfo, iccIoResult);
    }

    @Override
    public void getVoiceRadioTechnologyResponse(RadioResponseInfo radioResponseInfo, int i) {
        responseInts(radioResponseInfo, i);
    }

    @Override
    public void getCellInfoListResponse(RadioResponseInfo radioResponseInfo, ArrayList<CellInfo> arrayList) {
        responseCellInfoList(radioResponseInfo, arrayList);
    }

    @Override
    public void getCellInfoListResponse_1_2(RadioResponseInfo radioResponseInfo, ArrayList<android.hardware.radio.V1_2.CellInfo> arrayList) {
        responseCellInfoList_1_2(radioResponseInfo, arrayList);
    }

    @Override
    public void setCellInfoListRateResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void setInitialAttachApnResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void getImsRegistrationStateResponse(RadioResponseInfo radioResponseInfo, boolean z, int i) {
        responseInts(radioResponseInfo, z ? 1 : 0, i);
    }

    @Override
    public void sendImsSmsResponse(RadioResponseInfo radioResponseInfo, SendSmsResult sendSmsResult) {
        responseSms(radioResponseInfo, sendSmsResult);
    }

    @Override
    public void iccTransmitApduBasicChannelResponse(RadioResponseInfo radioResponseInfo, IccIoResult iccIoResult) {
        responseIccIo(radioResponseInfo, iccIoResult);
    }

    @Override
    public void iccOpenLogicalChannelResponse(RadioResponseInfo radioResponseInfo, int i, ArrayList<Byte> arrayList) {
        ArrayList<Integer> arrayList2 = new ArrayList<>();
        arrayList2.add(Integer.valueOf(i));
        for (int i2 = 0; i2 < arrayList.size(); i2++) {
            arrayList2.add(Integer.valueOf(arrayList.get(i2).byteValue()));
        }
        responseIntArrayList(radioResponseInfo, arrayList2);
    }

    @Override
    public void iccCloseLogicalChannelResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void iccTransmitApduLogicalChannelResponse(RadioResponseInfo radioResponseInfo, IccIoResult iccIoResult) {
        responseIccIo(radioResponseInfo, iccIoResult);
    }

    @Override
    public void nvReadItemResponse(RadioResponseInfo radioResponseInfo, String str) {
        responseString(radioResponseInfo, str);
    }

    @Override
    public void nvWriteItemResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void nvWriteCdmaPrlResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void nvResetConfigResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void setUiccSubscriptionResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void setDataAllowedResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void getHardwareConfigResponse(RadioResponseInfo radioResponseInfo, ArrayList<android.hardware.radio.V1_0.HardwareConfig> arrayList) {
        responseHardwareConfig(radioResponseInfo, arrayList);
    }

    @Override
    public void requestIccSimAuthenticationResponse(RadioResponseInfo radioResponseInfo, IccIoResult iccIoResult) {
        responseICC_IOBase64(radioResponseInfo, iccIoResult);
    }

    @Override
    public void setDataProfileResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void requestShutdownResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void getRadioCapabilityResponse(RadioResponseInfo radioResponseInfo, android.hardware.radio.V1_0.RadioCapability radioCapability) {
        RILRequest rILRequestProcessResponse = this.mRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            RadioCapability radioCapabilityConvertHalRadioCapability = RIL.convertHalRadioCapability(radioCapability, this.mRil);
            if (radioResponseInfo.error == 6 || radioResponseInfo.error == 2) {
                radioCapabilityConvertHalRadioCapability = this.mRil.makeStaticRadioCapability();
                radioResponseInfo.error = 0;
            }
            if (radioResponseInfo.error == 0) {
                sendMessageResponse(rILRequestProcessResponse.mResult, radioCapabilityConvertHalRadioCapability);
            }
            this.mRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, radioCapabilityConvertHalRadioCapability);
        }
    }

    @Override
    public void setRadioCapabilityResponse(RadioResponseInfo radioResponseInfo, android.hardware.radio.V1_0.RadioCapability radioCapability) {
        responseRadioCapability(radioResponseInfo, radioCapability);
    }

    @Override
    public void startLceServiceResponse(RadioResponseInfo radioResponseInfo, LceStatusInfo lceStatusInfo) {
        responseLceStatus(radioResponseInfo, lceStatusInfo);
    }

    @Override
    public void stopLceServiceResponse(RadioResponseInfo radioResponseInfo, LceStatusInfo lceStatusInfo) {
        responseLceStatus(radioResponseInfo, lceStatusInfo);
    }

    @Override
    public void pullLceDataResponse(RadioResponseInfo radioResponseInfo, LceDataInfo lceDataInfo) {
        responseLceData(radioResponseInfo, lceDataInfo);
    }

    @Override
    public void getModemActivityInfoResponse(RadioResponseInfo radioResponseInfo, ActivityStatsInfo activityStatsInfo) {
        responseActivityData(radioResponseInfo, activityStatsInfo);
    }

    @Override
    public void setAllowedCarriersResponse(RadioResponseInfo radioResponseInfo, int i) {
        responseInts(radioResponseInfo, i);
    }

    @Override
    public void getAllowedCarriersResponse(RadioResponseInfo radioResponseInfo, boolean z, CarrierRestrictions carrierRestrictions) {
        responseCarrierIdentifiers(radioResponseInfo, z, carrierRestrictions);
    }

    @Override
    public void sendDeviceStateResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void setCarrierInfoForImsiEncryptionResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void setIndicationFilterResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void setSimCardPowerResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void setSignalStrengthReportingCriteriaResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void setLinkCapacityReportingCriteriaResponse(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void setSimCardPowerResponse_1_1(RadioResponseInfo radioResponseInfo) {
        responseVoid(radioResponseInfo);
    }

    @Override
    public void startKeepaliveResponse(RadioResponseInfo radioResponseInfo, KeepaliveStatus keepaliveStatus) {
        com.android.internal.telephony.dataconnection.KeepaliveStatus keepaliveStatus2;
        RILRequest rILRequestProcessResponse = this.mRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse == null) {
            return;
        }
        int i = radioResponseInfo.error;
        if (i == 0) {
            int iConvertHalKeepaliveStatusCode = convertHalKeepaliveStatusCode(keepaliveStatus.code);
            if (iConvertHalKeepaliveStatusCode < 0) {
                keepaliveStatus2 = new com.android.internal.telephony.dataconnection.KeepaliveStatus(1);
            } else {
                keepaliveStatus2 = new com.android.internal.telephony.dataconnection.KeepaliveStatus(keepaliveStatus.sessionHandle, iConvertHalKeepaliveStatusCode);
            }
        } else if (i == 6) {
            keepaliveStatus2 = new com.android.internal.telephony.dataconnection.KeepaliveStatus(1);
            radioResponseInfo.error = 0;
        } else if (i == 42) {
            keepaliveStatus2 = new com.android.internal.telephony.dataconnection.KeepaliveStatus(2);
        } else {
            keepaliveStatus2 = new com.android.internal.telephony.dataconnection.KeepaliveStatus(3);
        }
        sendMessageResponse(rILRequestProcessResponse.mResult, keepaliveStatus2);
        this.mRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, keepaliveStatus2);
    }

    @Override
    public void stopKeepaliveResponse(RadioResponseInfo radioResponseInfo) {
        RILRequest rILRequestProcessResponse = this.mRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null && radioResponseInfo.error == 0) {
            sendMessageResponse(rILRequestProcessResponse.mResult, null);
            this.mRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, null);
        }
    }

    private int convertHalKeepaliveStatusCode(int i) {
        switch (i) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            default:
                this.mRil.riljLog("Invalid Keepalive Status" + i);
                return -1;
        }
    }

    private IccCardStatus convertHalCardStatus(CardStatus cardStatus) {
        IccCardStatus iccCardStatus = new IccCardStatus();
        iccCardStatus.setCardState(cardStatus.cardState);
        iccCardStatus.setUniversalPinState(cardStatus.universalPinState);
        iccCardStatus.mGsmUmtsSubscriptionAppIndex = cardStatus.gsmUmtsSubscriptionAppIndex;
        iccCardStatus.mCdmaSubscriptionAppIndex = cardStatus.cdmaSubscriptionAppIndex;
        iccCardStatus.mImsSubscriptionAppIndex = cardStatus.imsSubscriptionAppIndex;
        int size = cardStatus.applications.size();
        if (size > 8) {
            size = 8;
        }
        iccCardStatus.mApplications = new IccCardApplicationStatus[size];
        for (int i = 0; i < size; i++) {
            AppStatus appStatus = cardStatus.applications.get(i);
            IccCardApplicationStatus iccCardApplicationStatus = new IccCardApplicationStatus();
            iccCardApplicationStatus.app_type = iccCardApplicationStatus.AppTypeFromRILInt(appStatus.appType);
            iccCardApplicationStatus.app_state = iccCardApplicationStatus.AppStateFromRILInt(appStatus.appState);
            iccCardApplicationStatus.perso_substate = iccCardApplicationStatus.PersoSubstateFromRILInt(appStatus.persoSubstate);
            iccCardApplicationStatus.aid = appStatus.aidPtr;
            iccCardApplicationStatus.app_label = appStatus.appLabelPtr;
            iccCardApplicationStatus.pin1_replaced = appStatus.pin1Replaced;
            iccCardApplicationStatus.pin1 = iccCardApplicationStatus.PinStateFromRILInt(appStatus.pin1);
            iccCardApplicationStatus.pin2 = iccCardApplicationStatus.PinStateFromRILInt(appStatus.pin2);
            iccCardStatus.mApplications[i] = iccCardApplicationStatus;
            this.mRil.riljLog("IccCardApplicationStatus " + i + ":" + iccCardApplicationStatus.toString());
        }
        return iccCardStatus;
    }

    private void responseIccCardStatus(RadioResponseInfo radioResponseInfo, CardStatus cardStatus) {
        RILRequest rILRequestProcessResponse = this.mRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            IccCardStatus iccCardStatusConvertHalCardStatus = convertHalCardStatus(cardStatus);
            this.mRil.riljLog("responseIccCardStatus: from HIDL: " + iccCardStatusConvertHalCardStatus);
            if (radioResponseInfo.error == 0) {
                sendMessageResponse(rILRequestProcessResponse.mResult, iccCardStatusConvertHalCardStatus);
            }
            this.mRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, iccCardStatusConvertHalCardStatus);
        }
    }

    private void responseIccCardStatus_1_2(RadioResponseInfo radioResponseInfo, android.hardware.radio.V1_2.CardStatus cardStatus) {
        RILRequest rILRequestProcessResponse = this.mRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            IccCardStatus iccCardStatusConvertHalCardStatus = convertHalCardStatus(cardStatus.base);
            iccCardStatusConvertHalCardStatus.physicalSlotIndex = cardStatus.physicalSlotId;
            iccCardStatusConvertHalCardStatus.atr = cardStatus.atr;
            iccCardStatusConvertHalCardStatus.iccid = cardStatus.iccid;
            this.mRil.riljLog("responseIccCardStatus: from HIDL: " + iccCardStatusConvertHalCardStatus);
            if (radioResponseInfo.error == 0) {
                sendMessageResponse(rILRequestProcessResponse.mResult, iccCardStatusConvertHalCardStatus);
            }
            this.mRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, iccCardStatusConvertHalCardStatus);
        }
    }

    public void responseInts(RadioResponseInfo radioResponseInfo, int... iArr) {
        ArrayList<Integer> arrayList = new ArrayList<>();
        for (int i : iArr) {
            arrayList.add(Integer.valueOf(i));
        }
        responseIntArrayList(radioResponseInfo, arrayList);
    }

    public void responseIntArrayList(RadioResponseInfo radioResponseInfo, ArrayList<Integer> arrayList) {
        RILRequest rILRequestProcessResponse = this.mRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            int[] iArr = new int[arrayList.size()];
            for (int i = 0; i < arrayList.size(); i++) {
                iArr[i] = arrayList.get(i).intValue();
            }
            if (radioResponseInfo.error == 0) {
                sendMessageResponse(rILRequestProcessResponse.mResult, iArr);
            }
            this.mRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, iArr);
        }
    }

    private void responseCurrentCalls(RadioResponseInfo radioResponseInfo, ArrayList<android.hardware.radio.V1_0.Call> arrayList) {
        RILRequest rILRequestProcessResponse = this.mRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            int size = arrayList.size();
            ArrayList arrayList2 = new ArrayList(size);
            for (int i = 0; i < size; i++) {
                DriverCall driverCall = new DriverCall();
                driverCall.state = DriverCall.stateFromCLCC(arrayList.get(i).state);
                driverCall.index = arrayList.get(i).index;
                driverCall.TOA = arrayList.get(i).toa;
                driverCall.isMpty = arrayList.get(i).isMpty;
                driverCall.isMT = arrayList.get(i).isMT;
                driverCall.als = arrayList.get(i).als;
                driverCall.isVoice = arrayList.get(i).isVoice;
                driverCall.isVoicePrivacy = arrayList.get(i).isVoicePrivacy;
                driverCall.number = arrayList.get(i).number;
                driverCall.numberPresentation = DriverCall.presentationFromCLIP(arrayList.get(i).numberPresentation);
                driverCall.name = arrayList.get(i).name;
                driverCall.namePresentation = DriverCall.presentationFromCLIP(arrayList.get(i).namePresentation);
                if (arrayList.get(i).uusInfo.size() == 1) {
                    driverCall.uusInfo = new UUSInfo();
                    driverCall.uusInfo.setType(arrayList.get(i).uusInfo.get(0).uusType);
                    driverCall.uusInfo.setDcs(arrayList.get(i).uusInfo.get(0).uusDcs);
                    if (!TextUtils.isEmpty(arrayList.get(i).uusInfo.get(0).uusData)) {
                        driverCall.uusInfo.setUserData(arrayList.get(i).uusInfo.get(0).uusData.getBytes());
                    } else {
                        this.mRil.riljLog("responseCurrentCalls: uusInfo data is null or empty");
                    }
                    this.mRil.riljLogv(String.format("Incoming UUS : type=%d, dcs=%d, length=%d", Integer.valueOf(driverCall.uusInfo.getType()), Integer.valueOf(driverCall.uusInfo.getDcs()), Integer.valueOf(driverCall.uusInfo.getUserData().length)));
                    this.mRil.riljLogv("Incoming UUS : data (hex): " + IccUtils.bytesToHexString(driverCall.uusInfo.getUserData()));
                } else {
                    this.mRil.riljLogv("Incoming UUS : NOT present!");
                }
                driverCall.number = PhoneNumberUtils.stringFromStringAndTOA(driverCall.number, driverCall.TOA);
                arrayList2.add(driverCall);
                if (driverCall.isVoicePrivacy) {
                    this.mRil.mVoicePrivacyOnRegistrants.notifyRegistrants();
                    this.mRil.riljLog("InCall VoicePrivacy is enabled");
                } else {
                    this.mRil.mVoicePrivacyOffRegistrants.notifyRegistrants();
                    this.mRil.riljLog("InCall VoicePrivacy is disabled");
                }
            }
            Collections.sort(arrayList2);
            if (size == 0 && this.mRil.mTestingEmergencyCall.getAndSet(false) && this.mRil.mEmergencyCallbackModeRegistrant != null) {
                this.mRil.riljLog("responseCurrentCalls: call ended, testing emergency call, notify ECM Registrants");
                this.mRil.mEmergencyCallbackModeRegistrant.notifyRegistrant();
            }
            if (radioResponseInfo.error == 0) {
                sendMessageResponse(rILRequestProcessResponse.mResult, arrayList2);
            }
            this.mRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, arrayList2);
        }
    }

    private void responseCurrentCalls_1_2(RadioResponseInfo radioResponseInfo, ArrayList<android.hardware.radio.V1_2.Call> arrayList) {
        RILRequest rILRequestProcessResponse = this.mRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            int size = arrayList.size();
            ArrayList arrayList2 = new ArrayList(size);
            for (int i = 0; i < size; i++) {
                DriverCall driverCall = new DriverCall();
                driverCall.state = DriverCall.stateFromCLCC(arrayList.get(i).base.state);
                driverCall.index = arrayList.get(i).base.index;
                driverCall.TOA = arrayList.get(i).base.toa;
                driverCall.isMpty = arrayList.get(i).base.isMpty;
                driverCall.isMT = arrayList.get(i).base.isMT;
                driverCall.als = arrayList.get(i).base.als;
                driverCall.isVoice = arrayList.get(i).base.isVoice;
                driverCall.isVoicePrivacy = arrayList.get(i).base.isVoicePrivacy;
                driverCall.number = arrayList.get(i).base.number;
                driverCall.numberPresentation = DriverCall.presentationFromCLIP(arrayList.get(i).base.numberPresentation);
                driverCall.name = arrayList.get(i).base.name;
                driverCall.namePresentation = DriverCall.presentationFromCLIP(arrayList.get(i).base.namePresentation);
                if (arrayList.get(i).base.uusInfo.size() == 1) {
                    driverCall.uusInfo = new UUSInfo();
                    driverCall.uusInfo.setType(arrayList.get(i).base.uusInfo.get(0).uusType);
                    driverCall.uusInfo.setDcs(arrayList.get(i).base.uusInfo.get(0).uusDcs);
                    if (!TextUtils.isEmpty(arrayList.get(i).base.uusInfo.get(0).uusData)) {
                        driverCall.uusInfo.setUserData(arrayList.get(i).base.uusInfo.get(0).uusData.getBytes());
                    } else {
                        this.mRil.riljLog("responseCurrentCalls: uusInfo data is null or empty");
                    }
                    this.mRil.riljLogv(String.format("Incoming UUS : type=%d, dcs=%d, length=%d", Integer.valueOf(driverCall.uusInfo.getType()), Integer.valueOf(driverCall.uusInfo.getDcs()), Integer.valueOf(driverCall.uusInfo.getUserData().length)));
                    this.mRil.riljLogv("Incoming UUS : data (hex): " + IccUtils.bytesToHexString(driverCall.uusInfo.getUserData()));
                } else {
                    this.mRil.riljLogv("Incoming UUS : NOT present!");
                }
                driverCall.number = PhoneNumberUtils.stringFromStringAndTOA(driverCall.number, driverCall.TOA);
                driverCall.audioQuality = arrayList.get(i).audioQuality;
                arrayList2.add(driverCall);
                if (driverCall.isVoicePrivacy) {
                    this.mRil.mVoicePrivacyOnRegistrants.notifyRegistrants();
                    this.mRil.riljLog("InCall VoicePrivacy is enabled");
                } else {
                    this.mRil.mVoicePrivacyOffRegistrants.notifyRegistrants();
                    this.mRil.riljLog("InCall VoicePrivacy is disabled");
                }
            }
            Collections.sort(arrayList2);
            if (size == 0 && this.mRil.mTestingEmergencyCall.getAndSet(false) && this.mRil.mEmergencyCallbackModeRegistrant != null) {
                this.mRil.riljLog("responseCurrentCalls: call ended, testing emergency call, notify ECM Registrants");
                this.mRil.mEmergencyCallbackModeRegistrant.notifyRegistrant();
            }
            if (radioResponseInfo.error == 0) {
                sendMessageResponse(rILRequestProcessResponse.mResult, arrayList2);
            }
            this.mRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, arrayList2);
        }
    }

    public void responseVoid(RadioResponseInfo radioResponseInfo) {
        RILRequest rILRequestProcessResponse = this.mRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            if (radioResponseInfo.error == 0) {
                sendMessageResponse(rILRequestProcessResponse.mResult, null);
            }
            this.mRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, null);
        }
    }

    public void responseString(RadioResponseInfo radioResponseInfo, String str) {
        RILRequest rILRequestProcessResponse = this.mRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            if (radioResponseInfo.error == 0) {
                sendMessageResponse(rILRequestProcessResponse.mResult, str);
            }
            this.mRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, str);
        }
    }

    public void responseStrings(RadioResponseInfo radioResponseInfo, String... strArr) {
        ArrayList arrayList = new ArrayList();
        for (String str : strArr) {
            arrayList.add(str);
        }
        responseStringArrayList(this.mRil, radioResponseInfo, arrayList);
    }

    public static void responseStringArrayList(RIL ril, RadioResponseInfo radioResponseInfo, ArrayList<String> arrayList) {
        RILRequest rILRequestProcessResponse = ril.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            String[] strArr = new String[arrayList.size()];
            for (int i = 0; i < arrayList.size(); i++) {
                strArr[i] = arrayList.get(i);
            }
            if (radioResponseInfo.error == 0) {
                sendMessageResponse(rILRequestProcessResponse.mResult, strArr);
            }
            ril.processResponseDone(rILRequestProcessResponse, radioResponseInfo, strArr);
        }
    }

    private void responseLastCallFailCauseInfo(RadioResponseInfo radioResponseInfo, LastCallFailCauseInfo lastCallFailCauseInfo) {
        RILRequest rILRequestProcessResponse = this.mRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            LastCallFailCause lastCallFailCause = new LastCallFailCause();
            lastCallFailCause.causeCode = lastCallFailCauseInfo.causeCode;
            lastCallFailCause.vendorCause = lastCallFailCauseInfo.vendorCause;
            if (radioResponseInfo.error == 0) {
                sendMessageResponse(rILRequestProcessResponse.mResult, lastCallFailCause);
            }
            this.mRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, lastCallFailCause);
        }
    }

    private void responseSignalStrength(RadioResponseInfo radioResponseInfo, SignalStrength signalStrength) {
        RILRequest rILRequestProcessResponse = this.mRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            android.telephony.SignalStrength signalStrengthConvertHalSignalStrength = RIL.convertHalSignalStrength(signalStrength);
            if (radioResponseInfo.error == 0) {
                sendMessageResponse(rILRequestProcessResponse.mResult, signalStrengthConvertHalSignalStrength);
            }
            this.mRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, signalStrengthConvertHalSignalStrength);
        }
    }

    private void responseSignalStrength_1_2(RadioResponseInfo radioResponseInfo, android.hardware.radio.V1_2.SignalStrength signalStrength) {
        RILRequest rILRequestProcessResponse = this.mRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            android.telephony.SignalStrength signalStrengthConvertHalSignalStrength_1_2 = RIL.convertHalSignalStrength_1_2(signalStrength);
            if (radioResponseInfo.error == 0) {
                sendMessageResponse(rILRequestProcessResponse.mResult, signalStrengthConvertHalSignalStrength_1_2);
            }
            this.mRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, signalStrengthConvertHalSignalStrength_1_2);
        }
    }

    private void responseSms(RadioResponseInfo radioResponseInfo, SendSmsResult sendSmsResult) {
        RILRequest rILRequestProcessResponse = this.mRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            SmsResponse smsResponse = new SmsResponse(sendSmsResult.messageRef, sendSmsResult.ackPDU, sendSmsResult.errorCode);
            if (radioResponseInfo.error == 0) {
                sendMessageResponse(rILRequestProcessResponse.mResult, smsResponse);
            }
            this.mRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, smsResponse);
        }
    }

    private void responseSetupDataCall(RadioResponseInfo radioResponseInfo, SetupDataCallResult setupDataCallResult) {
        RILRequest rILRequestProcessResponse = this.mRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            if (radioResponseInfo.error == 0) {
                sendMessageResponse(rILRequestProcessResponse.mResult, setupDataCallResult);
            }
            this.mRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, setupDataCallResult);
        }
    }

    private void responseIccIo(RadioResponseInfo radioResponseInfo, IccIoResult iccIoResult) {
        RILRequest rILRequestProcessResponse = this.mRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            com.android.internal.telephony.uicc.IccIoResult iccIoResult2 = new com.android.internal.telephony.uicc.IccIoResult(iccIoResult.sw1, iccIoResult.sw2, iccIoResult.simResponse);
            if (radioResponseInfo.error == 0) {
                sendMessageResponse(rILRequestProcessResponse.mResult, iccIoResult2);
            }
            this.mRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, iccIoResult2);
        }
    }

    private void responseCallForwardInfo(RadioResponseInfo radioResponseInfo, ArrayList<android.hardware.radio.V1_0.CallForwardInfo> arrayList) {
        RILRequest rILRequestProcessResponse = this.mRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            CallForwardInfo[] callForwardInfoArr = new CallForwardInfo[arrayList.size()];
            for (int i = 0; i < arrayList.size(); i++) {
                callForwardInfoArr[i] = new CallForwardInfo();
                callForwardInfoArr[i].status = arrayList.get(i).status;
                callForwardInfoArr[i].reason = arrayList.get(i).reason;
                callForwardInfoArr[i].serviceClass = arrayList.get(i).serviceClass;
                callForwardInfoArr[i].toa = arrayList.get(i).toa;
                callForwardInfoArr[i].number = arrayList.get(i).number;
                callForwardInfoArr[i].timeSeconds = arrayList.get(i).timeSeconds;
            }
            if (radioResponseInfo.error == 0) {
                sendMessageResponse(rILRequestProcessResponse.mResult, callForwardInfoArr);
            }
            this.mRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, callForwardInfoArr);
        }
    }

    public static String convertOpertatorInfoToString(int i) {
        if (i == 0) {
            return "unknown";
        }
        if (i == 1) {
            return "available";
        }
        if (i == 2) {
            return "current";
        }
        if (i == 3) {
            return "forbidden";
        }
        return "";
    }

    private void responseOperatorInfos(RadioResponseInfo radioResponseInfo, ArrayList<OperatorInfo> arrayList) {
        RILRequest rILRequestProcessResponse = this.mRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            ArrayList arrayList2 = new ArrayList();
            for (int i = 0; i < arrayList.size(); i++) {
                arrayList2.add(new OperatorInfo(arrayList.get(i).alphaLong, arrayList.get(i).alphaShort, arrayList.get(i).operatorNumeric, convertOpertatorInfoToString(arrayList.get(i).status)));
            }
            if (radioResponseInfo.error == 0) {
                sendMessageResponse(rILRequestProcessResponse.mResult, arrayList2);
            }
            this.mRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, arrayList2);
        }
    }

    private void responseScanStatus(RadioResponseInfo radioResponseInfo) {
        NetworkScanResult networkScanResult;
        RILRequest rILRequestProcessResponse = this.mRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            if (radioResponseInfo.error == 0) {
                networkScanResult = new NetworkScanResult(1, 0, (List) null);
                sendMessageResponse(rILRequestProcessResponse.mResult, networkScanResult);
            } else {
                networkScanResult = null;
            }
            this.mRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, networkScanResult);
        }
    }

    private void responseDataCallList(RadioResponseInfo radioResponseInfo, ArrayList<SetupDataCallResult> arrayList) {
        RILRequest rILRequestProcessResponse = this.mRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            if (radioResponseInfo.error == 0) {
                sendMessageResponse(rILRequestProcessResponse.mResult, arrayList);
            }
            this.mRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, arrayList);
        }
    }

    private void responseCellList(RadioResponseInfo radioResponseInfo, ArrayList<NeighboringCell> arrayList) {
        RILRequest rILRequestProcessResponse = this.mRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            ArrayList arrayList2 = new ArrayList();
            int dataNetworkType = ((TelephonyManager) this.mRil.mContext.getSystemService("phone")).getDataNetworkType(SubscriptionManager.getSubId(this.mRil.mPhoneId.intValue())[0]);
            if (dataNetworkType != 0) {
                for (int i = 0; i < arrayList.size(); i++) {
                    arrayList2.add(new NeighboringCellInfo(arrayList.get(i).rssi, arrayList.get(i).cid, dataNetworkType));
                }
            }
            if (radioResponseInfo.error == 0) {
                sendMessageResponse(rILRequestProcessResponse.mResult, arrayList2);
            }
            this.mRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, arrayList2);
        }
    }

    private void responseGmsBroadcastConfig(RadioResponseInfo radioResponseInfo, ArrayList<GsmBroadcastSmsConfigInfo> arrayList) {
        RILRequest rILRequestProcessResponse = this.mRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            ArrayList arrayList2 = new ArrayList();
            for (int i = 0; i < arrayList.size(); i++) {
                arrayList2.add(new SmsBroadcastConfigInfo(arrayList.get(i).fromServiceId, arrayList.get(i).toServiceId, arrayList.get(i).fromCodeScheme, arrayList.get(i).toCodeScheme, arrayList.get(i).selected));
            }
            if (radioResponseInfo.error == 0) {
                sendMessageResponse(rILRequestProcessResponse.mResult, arrayList2);
            }
            this.mRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, arrayList2);
        }
    }

    private void responseCdmaBroadcastConfig(RadioResponseInfo radioResponseInfo, ArrayList<CdmaBroadcastSmsConfigInfo> arrayList) {
        int[] iArr;
        RILRequest rILRequestProcessResponse = this.mRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            int size = arrayList.size();
            int i = 0;
            int i2 = 1;
            if (size == 0) {
                iArr = new int[94];
                iArr[0] = 31;
                for (int i3 = 1; i3 < 94; i3 += 3) {
                    iArr[i3 + 0] = i3 / 3;
                    iArr[i3 + 1] = 1;
                    iArr[i3 + 2] = 0;
                }
            } else {
                int[] iArr2 = new int[(size * 3) + 1];
                iArr2[0] = size;
                while (i < arrayList.size()) {
                    iArr2[i2] = arrayList.get(i).serviceCategory;
                    iArr2[i2 + 1] = arrayList.get(i).language;
                    iArr2[i2 + 2] = arrayList.get(i).selected ? 1 : 0;
                    i++;
                    i2 += 3;
                }
                iArr = iArr2;
            }
            if (radioResponseInfo.error == 0) {
                sendMessageResponse(rILRequestProcessResponse.mResult, iArr);
            }
            this.mRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, iArr);
        }
    }

    private void responseCellInfoList(RadioResponseInfo radioResponseInfo, ArrayList<CellInfo> arrayList) {
        RILRequest rILRequestProcessResponse = this.mRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            ArrayList<android.telephony.CellInfo> arrayListConvertHalCellInfoList = RIL.convertHalCellInfoList(arrayList);
            if (radioResponseInfo.error == 0) {
                sendMessageResponse(rILRequestProcessResponse.mResult, arrayListConvertHalCellInfoList);
            }
            this.mRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, arrayListConvertHalCellInfoList);
        }
    }

    public void responseCellInfoList_1_2(RadioResponseInfo radioResponseInfo, ArrayList<android.hardware.radio.V1_2.CellInfo> arrayList) {
        RILRequest rILRequestProcessResponse = this.mRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            ArrayList<android.telephony.CellInfo> arrayListConvertHalCellInfoList_1_2 = RIL.convertHalCellInfoList_1_2(arrayList);
            if (radioResponseInfo.error == 0) {
                sendMessageResponse(rILRequestProcessResponse.mResult, arrayListConvertHalCellInfoList_1_2);
            }
            this.mRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, arrayListConvertHalCellInfoList_1_2);
        }
    }

    private void responseActivityData(RadioResponseInfo radioResponseInfo, ActivityStatsInfo activityStatsInfo) {
        ModemActivityInfo modemActivityInfo;
        RILRequest rILRequestProcessResponse = this.mRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            if (radioResponseInfo.error == 0) {
                int i = activityStatsInfo.sleepModeTimeMs;
                int i2 = activityStatsInfo.idleModeTimeMs;
                int[] iArr = new int[5];
                for (int i3 = 0; i3 < 5; i3++) {
                    iArr[i3] = activityStatsInfo.txmModetimeMs[i3];
                }
                modemActivityInfo = new ModemActivityInfo(SystemClock.elapsedRealtime(), i, i2, iArr, activityStatsInfo.rxModeTimeMs, 0);
            } else {
                modemActivityInfo = new ModemActivityInfo(0L, 0, 0, new int[5], 0, 0);
                radioResponseInfo.error = 0;
            }
            sendMessageResponse(rILRequestProcessResponse.mResult, modemActivityInfo);
            this.mRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, modemActivityInfo);
        }
    }

    private void responseHardwareConfig(RadioResponseInfo radioResponseInfo, ArrayList<android.hardware.radio.V1_0.HardwareConfig> arrayList) {
        RILRequest rILRequestProcessResponse = this.mRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            ArrayList<HardwareConfig> arrayListConvertHalHwConfigList = this.mRil.convertHalHwConfigList(arrayList, this.mRil);
            if (radioResponseInfo.error == 0) {
                sendMessageResponse(rILRequestProcessResponse.mResult, arrayListConvertHalHwConfigList);
            }
            this.mRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, arrayListConvertHalHwConfigList);
        }
    }

    private void responseICC_IOBase64(RadioResponseInfo radioResponseInfo, IccIoResult iccIoResult) {
        byte[] bArrDecode;
        RILRequest rILRequestProcessResponse = this.mRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            int i = iccIoResult.sw1;
            int i2 = iccIoResult.sw2;
            if (!iccIoResult.simResponse.equals("")) {
                bArrDecode = Base64.decode(iccIoResult.simResponse, 0);
            } else {
                bArrDecode = (byte[]) null;
            }
            com.android.internal.telephony.uicc.IccIoResult iccIoResult2 = new com.android.internal.telephony.uicc.IccIoResult(i, i2, bArrDecode);
            if (radioResponseInfo.error == 0) {
                sendMessageResponse(rILRequestProcessResponse.mResult, iccIoResult2);
            }
            this.mRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, iccIoResult2);
        }
    }

    private void responseRadioCapability(RadioResponseInfo radioResponseInfo, android.hardware.radio.V1_0.RadioCapability radioCapability) {
        RILRequest rILRequestProcessResponse = this.mRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            RadioCapability radioCapabilityConvertHalRadioCapability = RIL.convertHalRadioCapability(radioCapability, this.mRil);
            if (radioResponseInfo.error == 0) {
                sendMessageResponse(rILRequestProcessResponse.mResult, radioCapabilityConvertHalRadioCapability);
            }
            this.mRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, radioCapabilityConvertHalRadioCapability);
        }
    }

    private void responseLceStatus(RadioResponseInfo radioResponseInfo, LceStatusInfo lceStatusInfo) {
        RILRequest rILRequestProcessResponse = this.mRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            ArrayList arrayList = new ArrayList();
            arrayList.add(Integer.valueOf(lceStatusInfo.lceStatus));
            arrayList.add(Integer.valueOf(Byte.toUnsignedInt(lceStatusInfo.actualIntervalMs)));
            if (radioResponseInfo.error == 0) {
                sendMessageResponse(rILRequestProcessResponse.mResult, arrayList);
            }
            this.mRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, arrayList);
        }
    }

    private void responseLceData(RadioResponseInfo radioResponseInfo, LceDataInfo lceDataInfo) {
        RILRequest rILRequestProcessResponse = this.mRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            LinkCapacityEstimate linkCapacityEstimateConvertHalLceData = RIL.convertHalLceData(lceDataInfo, this.mRil);
            if (radioResponseInfo.error == 0) {
                sendMessageResponse(rILRequestProcessResponse.mResult, linkCapacityEstimateConvertHalLceData);
            }
            this.mRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, linkCapacityEstimateConvertHalLceData);
        }
    }

    private void responseCarrierIdentifiers(RadioResponseInfo radioResponseInfo, boolean z, CarrierRestrictions carrierRestrictions) {
        String str;
        String str2;
        String str3;
        RILRequest rILRequestProcessResponse = this.mRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            ArrayList arrayList = new ArrayList();
            for (int i = 0; i < carrierRestrictions.allowedCarriers.size(); i++) {
                String str4 = carrierRestrictions.allowedCarriers.get(i).mcc;
                String str5 = carrierRestrictions.allowedCarriers.get(i).mnc;
                int i2 = carrierRestrictions.allowedCarriers.get(i).matchType;
                String str6 = carrierRestrictions.allowedCarriers.get(i).matchData;
                String str7 = null;
                if (i2 == 1) {
                    str = str6;
                    str2 = null;
                } else {
                    if (i2 == 2) {
                        str = null;
                        str2 = null;
                        str3 = null;
                        str7 = str6;
                    } else if (i2 == 3) {
                        str2 = str6;
                        str = null;
                        str3 = null;
                    } else if (i2 == 4) {
                        str3 = str6;
                        str = null;
                        str2 = null;
                    } else {
                        str = null;
                        str2 = null;
                    }
                    arrayList.add(new android.service.carrier.CarrierIdentifier(str4, str5, str, str7, str2, str3));
                }
                str3 = str2;
                arrayList.add(new android.service.carrier.CarrierIdentifier(str4, str5, str, str7, str2, str3));
            }
            if (radioResponseInfo.error == 0) {
                sendMessageResponse(rILRequestProcessResponse.mResult, arrayList);
            }
            this.mRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, arrayList);
        }
    }
}
