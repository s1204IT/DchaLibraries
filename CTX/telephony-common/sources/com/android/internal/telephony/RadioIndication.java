package com.android.internal.telephony;

import android.hardware.radio.V1_0.ApnTypes;
import android.hardware.radio.V1_0.CdmaCallWaiting;
import android.hardware.radio.V1_0.CdmaInformationRecord;
import android.hardware.radio.V1_0.CdmaInformationRecords;
import android.hardware.radio.V1_0.CdmaLineControlInfoRecord;
import android.hardware.radio.V1_0.CdmaNumberInfoRecord;
import android.hardware.radio.V1_0.CdmaRedirectingNumberInfoRecord;
import android.hardware.radio.V1_0.CdmaSignalInfoRecord;
import android.hardware.radio.V1_0.CdmaSmsMessage;
import android.hardware.radio.V1_0.CdmaT53AudioControlInfoRecord;
import android.hardware.radio.V1_0.CellInfo;
import android.hardware.radio.V1_0.CfData;
import android.hardware.radio.V1_0.LceDataInfo;
import android.hardware.radio.V1_0.PcoDataInfo;
import android.hardware.radio.V1_0.RadioAccessFamily;
import android.hardware.radio.V1_0.SetupDataCallResult;
import android.hardware.radio.V1_0.SignalStrength;
import android.hardware.radio.V1_0.SimRefreshResult;
import android.hardware.radio.V1_0.SsInfoData;
import android.hardware.radio.V1_0.StkCcUnsolSsResult;
import android.hardware.radio.V1_0.SuppSvcNotification;
import android.hardware.radio.V1_1.NetworkScanResult;
import android.hardware.radio.V1_2.IRadioIndication;
import android.hardware.radio.V1_2.PhysicalChannelConfig;
import android.os.AsyncResult;
import android.os.SystemProperties;
import android.telephony.PcoData;
import android.telephony.SmsMessage;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.cdma.CdmaCallWaitingNotification;
import com.android.internal.telephony.cdma.CdmaInformationRecords;
import com.android.internal.telephony.cdma.SmsMessageConverter;
import com.android.internal.telephony.dataconnection.KeepaliveStatus;
import com.android.internal.telephony.gsm.SsData;
import com.android.internal.telephony.gsm.SuppServiceNotification;
import com.android.internal.telephony.uicc.IccRefreshResponse;
import com.android.internal.telephony.uicc.IccUtils;
import com.google.android.mms.pdu.CharacterSets;
import java.util.ArrayList;

public class RadioIndication extends IRadioIndication.Stub {
    public RIL mRil;

    public RadioIndication(RIL ril) {
        this.mRil = ril;
    }

    @Override
    public void radioStateChanged(int i, int i2) {
        this.mRil.processIndication(i);
        CommandsInterface.RadioState radioStateFromInt = getRadioStateFromInt(i2);
        this.mRil.unsljLogMore(1000, "radioStateChanged: " + radioStateFromInt);
        this.mRil.setRadioState(radioStateFromInt);
    }

    @Override
    public void callStateChanged(int i) {
        this.mRil.processIndication(i);
        this.mRil.unsljLog(1001);
        this.mRil.mCallStateRegistrants.notifyRegistrants();
    }

    @Override
    public void networkStateChanged(int i) {
        this.mRil.processIndication(i);
        this.mRil.unsljLog(1002);
        this.mRil.mNetworkStateRegistrants.notifyRegistrants();
    }

    @Override
    public void newSms(int i, ArrayList<Byte> arrayList) {
        this.mRil.processIndication(i);
        byte[] bArrArrayListToPrimitiveArray = RIL.arrayListToPrimitiveArray(arrayList);
        this.mRil.unsljLog(1003);
        this.mRil.writeMetricsNewSms(1, 1);
        SmsMessage smsMessageNewFromCMT = SmsMessage.newFromCMT(bArrArrayListToPrimitiveArray);
        if (this.mRil.mGsmSmsRegistrant != null) {
            this.mRil.mGsmSmsRegistrant.notifyRegistrant(new AsyncResult((Object) null, smsMessageNewFromCMT, (Throwable) null));
        }
    }

    @Override
    public void newSmsStatusReport(int i, ArrayList<Byte> arrayList) {
        this.mRil.processIndication(i);
        byte[] bArrArrayListToPrimitiveArray = RIL.arrayListToPrimitiveArray(arrayList);
        this.mRil.unsljLog(1004);
        if (this.mRil.mSmsStatusRegistrant != null) {
            this.mRil.mSmsStatusRegistrant.notifyRegistrant(new AsyncResult((Object) null, bArrArrayListToPrimitiveArray, (Throwable) null));
        }
    }

    @Override
    public void newSmsOnSim(int i, int i2) {
        this.mRil.processIndication(i);
        this.mRil.unsljLog(1005);
        if (this.mRil.mSmsOnSimRegistrant != null) {
            this.mRil.mSmsOnSimRegistrant.notifyRegistrant(new AsyncResult((Object) null, Integer.valueOf(i2), (Throwable) null));
        }
    }

    @Override
    public void onUssd(int i, int i2, String str) {
        this.mRil.processIndication(i);
        this.mRil.unsljLogMore(1006, "" + i2);
        String[] strArr = {"" + i2, str};
        if (this.mRil.mUSSDRegistrant != null) {
            this.mRil.mUSSDRegistrant.notifyRegistrant(new AsyncResult((Object) null, strArr, (Throwable) null));
        }
    }

    @Override
    public void nitzTimeReceived(int i, String str, long j) {
        this.mRil.processIndication(i);
        this.mRil.unsljLogRet(1008, str);
        Object[] objArr = {str, Long.valueOf(j)};
        if (SystemProperties.getBoolean("telephony.test.ignore.nitz", false)) {
            this.mRil.riljLog("ignoring UNSOL_NITZ_TIME_RECEIVED");
            return;
        }
        if (this.mRil.mNITZTimeRegistrant != null) {
            this.mRil.mNITZTimeRegistrant.notifyRegistrant(new AsyncResult((Object) null, objArr, (Throwable) null));
        }
        this.mRil.mLastNITZTimeInfo = objArr;
    }

    @Override
    public void currentSignalStrength(int i, SignalStrength signalStrength) {
        this.mRil.processIndication(i);
        android.telephony.SignalStrength signalStrengthConvertHalSignalStrength = RIL.convertHalSignalStrength(signalStrength);
        if (this.mRil.mSignalStrengthRegistrant != null) {
            this.mRil.mSignalStrengthRegistrant.notifyRegistrant(new AsyncResult((Object) null, signalStrengthConvertHalSignalStrength, (Throwable) null));
        }
    }

    @Override
    public void currentLinkCapacityEstimate(int i, android.hardware.radio.V1_2.LinkCapacityEstimate linkCapacityEstimate) {
        this.mRil.processIndication(i);
        LinkCapacityEstimate linkCapacityEstimateConvertHalLceData = RIL.convertHalLceData(linkCapacityEstimate, this.mRil);
        this.mRil.unsljLogRet(1045, linkCapacityEstimateConvertHalLceData);
        if (this.mRil.mLceInfoRegistrants != null) {
            this.mRil.mLceInfoRegistrants.notifyRegistrants(new AsyncResult((Object) null, linkCapacityEstimateConvertHalLceData, (Throwable) null));
        }
    }

    @Override
    public void currentSignalStrength_1_2(int i, android.hardware.radio.V1_2.SignalStrength signalStrength) {
        this.mRil.processIndication(i);
        android.telephony.SignalStrength signalStrengthConvertHalSignalStrength_1_2 = RIL.convertHalSignalStrength_1_2(signalStrength);
        if (this.mRil.mSignalStrengthRegistrant != null) {
            this.mRil.mSignalStrengthRegistrant.notifyRegistrant(new AsyncResult((Object) null, signalStrengthConvertHalSignalStrength_1_2, (Throwable) null));
        }
    }

    @Override
    public void currentPhysicalChannelConfigs(int i, ArrayList<PhysicalChannelConfig> arrayList) {
        int i2;
        ArrayList arrayList2 = new ArrayList(arrayList.size());
        for (PhysicalChannelConfig physicalChannelConfig : arrayList) {
            switch (physicalChannelConfig.status) {
                case 1:
                    i2 = 1;
                    break;
                case 2:
                    i2 = 2;
                    break;
                default:
                    this.mRil.riljLoge("Unsupported CellConnectionStatus in PhysicalChannelConfig: " + physicalChannelConfig.status);
                    i2 = KeepaliveStatus.INVALID_HANDLE;
                    break;
            }
            arrayList2.add(new android.telephony.PhysicalChannelConfig(i2, physicalChannelConfig.cellBandwidthDownlink));
        }
        this.mRil.unsljLogRet(1052, arrayList2);
        this.mRil.mPhysicalChannelConfigurationRegistrants.notifyRegistrants(new AsyncResult((Object) null, arrayList2, (Throwable) null));
    }

    @Override
    public void dataCallListChanged(int i, ArrayList<SetupDataCallResult> arrayList) {
        this.mRil.processIndication(i);
        this.mRil.unsljLogRet(1010, arrayList);
        this.mRil.mDataCallListChangedRegistrants.notifyRegistrants(new AsyncResult((Object) null, arrayList, (Throwable) null));
    }

    @Override
    public void suppSvcNotify(int i, SuppSvcNotification suppSvcNotification) {
        this.mRil.processIndication(i);
        SuppServiceNotification suppServiceNotification = new SuppServiceNotification();
        suppServiceNotification.notificationType = suppSvcNotification.isMT ? 1 : 0;
        suppServiceNotification.code = suppSvcNotification.code;
        suppServiceNotification.index = suppSvcNotification.index;
        suppServiceNotification.type = suppSvcNotification.type;
        suppServiceNotification.number = suppSvcNotification.number;
        this.mRil.unsljLogRet(1011, suppServiceNotification);
        if (this.mRil.mSsnRegistrant != null) {
            this.mRil.mSsnRegistrant.notifyRegistrant(new AsyncResult((Object) null, suppServiceNotification, (Throwable) null));
        }
    }

    @Override
    public void stkSessionEnd(int i) {
        this.mRil.processIndication(i);
        this.mRil.unsljLog(1012);
        if (this.mRil.mCatSessionEndRegistrant != null) {
            this.mRil.mCatSessionEndRegistrant.notifyRegistrant(new AsyncResult((Object) null, (Object) null, (Throwable) null));
        }
    }

    @Override
    public void stkProactiveCommand(int i, String str) {
        this.mRil.processIndication(i);
        this.mRil.unsljLog(1013);
        if (this.mRil.mCatProCmdRegistrant != null) {
            this.mRil.mCatProCmdRegistrant.notifyRegistrant(new AsyncResult((Object) null, str, (Throwable) null));
        }
    }

    @Override
    public void stkEventNotify(int i, String str) {
        this.mRil.processIndication(i);
        this.mRil.unsljLog(1014);
        if (this.mRil.mCatEventRegistrant != null) {
            this.mRil.mCatEventRegistrant.notifyRegistrant(new AsyncResult((Object) null, str, (Throwable) null));
        }
    }

    @Override
    public void stkCallSetup(int i, long j) {
        this.mRil.processIndication(i);
        this.mRil.unsljLogRet(CharacterSets.UTF_16, Long.valueOf(j));
        if (this.mRil.mCatCallSetUpRegistrant != null) {
            this.mRil.mCatCallSetUpRegistrant.notifyRegistrant(new AsyncResult((Object) null, Long.valueOf(j), (Throwable) null));
        }
    }

    @Override
    public void simSmsStorageFull(int i) {
        this.mRil.processIndication(i);
        this.mRil.unsljLog(1016);
        if (this.mRil.mIccSmsFullRegistrant != null) {
            this.mRil.mIccSmsFullRegistrant.notifyRegistrant();
        }
    }

    @Override
    public void simRefresh(int i, SimRefreshResult simRefreshResult) {
        this.mRil.processIndication(i);
        IccRefreshResponse iccRefreshResponse = new IccRefreshResponse();
        iccRefreshResponse.refreshResult = simRefreshResult.type;
        iccRefreshResponse.efId = simRefreshResult.efId;
        iccRefreshResponse.aid = simRefreshResult.aid;
        this.mRil.unsljLogRet(1017, iccRefreshResponse);
        this.mRil.mIccRefreshRegistrants.notifyRegistrants(new AsyncResult((Object) null, iccRefreshResponse, (Throwable) null));
    }

    @Override
    public void callRing(int i, boolean z, CdmaSignalInfoRecord cdmaSignalInfoRecord) {
        char[] cArr;
        this.mRil.processIndication(i);
        if (!z) {
            cArr = new char[]{cdmaSignalInfoRecord.isPresent ? (char) 1 : (char) 0, (char) cdmaSignalInfoRecord.signalType, (char) cdmaSignalInfoRecord.alertPitch, (char) cdmaSignalInfoRecord.signal};
            this.mRil.writeMetricsCallRing(cArr);
        } else {
            cArr = null;
        }
        this.mRil.unsljLogRet(1018, cArr);
        if (this.mRil.mRingRegistrant != null) {
            this.mRil.mRingRegistrant.notifyRegistrant(new AsyncResult((Object) null, cArr, (Throwable) null));
        }
    }

    @Override
    public void simStatusChanged(int i) {
        this.mRil.processIndication(i);
        this.mRil.unsljLog(1019);
        this.mRil.mIccStatusChangedRegistrants.notifyRegistrants();
    }

    @Override
    public void cdmaNewSms(int i, CdmaSmsMessage cdmaSmsMessage) {
        this.mRil.processIndication(i);
        this.mRil.unsljLog(1020);
        this.mRil.writeMetricsNewSms(2, 2);
        SmsMessage smsMessageNewSmsMessageFromCdmaSmsMessage = SmsMessageConverter.newSmsMessageFromCdmaSmsMessage(cdmaSmsMessage);
        if (this.mRil.mCdmaSmsRegistrant != null) {
            this.mRil.mCdmaSmsRegistrant.notifyRegistrant(new AsyncResult((Object) null, smsMessageNewSmsMessageFromCdmaSmsMessage, (Throwable) null));
        }
    }

    @Override
    public void newBroadcastSms(int i, ArrayList<Byte> arrayList) {
        this.mRil.processIndication(i);
        byte[] bArrArrayListToPrimitiveArray = RIL.arrayListToPrimitiveArray(arrayList);
        this.mRil.unsljLogvRet(1021, IccUtils.bytesToHexString(bArrArrayListToPrimitiveArray));
        if (this.mRil.mGsmBroadcastSmsRegistrant != null) {
            this.mRil.mGsmBroadcastSmsRegistrant.notifyRegistrant(new AsyncResult((Object) null, bArrArrayListToPrimitiveArray, (Throwable) null));
        }
    }

    @Override
    public void cdmaRuimSmsStorageFull(int i) {
        this.mRil.processIndication(i);
        this.mRil.unsljLog(1022);
        if (this.mRil.mIccSmsFullRegistrant != null) {
            this.mRil.mIccSmsFullRegistrant.notifyRegistrant();
        }
    }

    @Override
    public void restrictedStateChanged(int i, int i2) {
        this.mRil.processIndication(i);
        this.mRil.unsljLogvRet(ApnTypes.ALL, Integer.valueOf(i2));
        if (this.mRil.mRestrictedStateRegistrant != null) {
            this.mRil.mRestrictedStateRegistrant.notifyRegistrant(new AsyncResult((Object) null, Integer.valueOf(i2), (Throwable) null));
        }
    }

    @Override
    public void enterEmergencyCallbackMode(int i) {
        this.mRil.processIndication(i);
        this.mRil.unsljLog(RadioAccessFamily.HSUPA);
        if (this.mRil.mEmergencyCallbackModeRegistrant != null) {
            this.mRil.mEmergencyCallbackModeRegistrant.notifyRegistrant();
        }
    }

    @Override
    public void cdmaCallWaiting(int i, CdmaCallWaiting cdmaCallWaiting) {
        this.mRil.processIndication(i);
        CdmaCallWaitingNotification cdmaCallWaitingNotification = new CdmaCallWaitingNotification();
        cdmaCallWaitingNotification.number = cdmaCallWaiting.number;
        cdmaCallWaitingNotification.numberPresentation = CdmaCallWaitingNotification.presentationFromCLIP(cdmaCallWaiting.numberPresentation);
        cdmaCallWaitingNotification.name = cdmaCallWaiting.name;
        cdmaCallWaitingNotification.namePresentation = cdmaCallWaitingNotification.numberPresentation;
        cdmaCallWaitingNotification.isPresent = cdmaCallWaiting.signalInfoRecord.isPresent ? 1 : 0;
        cdmaCallWaitingNotification.signalType = cdmaCallWaiting.signalInfoRecord.signalType;
        cdmaCallWaitingNotification.alertPitch = cdmaCallWaiting.signalInfoRecord.alertPitch;
        cdmaCallWaitingNotification.signal = cdmaCallWaiting.signalInfoRecord.signal;
        cdmaCallWaitingNotification.numberType = cdmaCallWaiting.numberType;
        cdmaCallWaitingNotification.numberPlan = cdmaCallWaiting.numberPlan;
        this.mRil.unsljLogRet(1025, cdmaCallWaitingNotification);
        this.mRil.mCallWaitingInfoRegistrants.notifyRegistrants(new AsyncResult((Object) null, cdmaCallWaitingNotification, (Throwable) null));
    }

    @Override
    public void cdmaOtaProvisionStatus(int i, int i2) {
        this.mRil.processIndication(i);
        int[] iArr = {i2};
        this.mRil.unsljLogRet(1026, iArr);
        this.mRil.mOtaProvisionRegistrants.notifyRegistrants(new AsyncResult((Object) null, iArr, (Throwable) null));
    }

    @Override
    public void cdmaInfoRec(int i, CdmaInformationRecords cdmaInformationRecords) {
        com.android.internal.telephony.cdma.CdmaInformationRecords cdmaInformationRecords2;
        this.mRil.processIndication(i);
        int size = cdmaInformationRecords.infoRec.size();
        for (int i2 = 0; i2 < size; i2++) {
            CdmaInformationRecord cdmaInformationRecord = cdmaInformationRecords.infoRec.get(i2);
            int i3 = cdmaInformationRecord.name;
            switch (i3) {
                case 0:
                case 7:
                    cdmaInformationRecords2 = new com.android.internal.telephony.cdma.CdmaInformationRecords(new CdmaInformationRecords.CdmaDisplayInfoRec(i3, cdmaInformationRecord.display.get(0).alphaBuf));
                    break;
                case 1:
                case 2:
                case 3:
                    CdmaNumberInfoRecord cdmaNumberInfoRecord = cdmaInformationRecord.number.get(0);
                    cdmaInformationRecords2 = new com.android.internal.telephony.cdma.CdmaInformationRecords(new CdmaInformationRecords.CdmaNumberInfoRec(i3, cdmaNumberInfoRecord.number, cdmaNumberInfoRecord.numberType, cdmaNumberInfoRecord.numberPlan, cdmaNumberInfoRecord.pi, cdmaNumberInfoRecord.si));
                    break;
                case 4:
                    CdmaSignalInfoRecord cdmaSignalInfoRecord = cdmaInformationRecord.signal.get(0);
                    cdmaInformationRecords2 = new com.android.internal.telephony.cdma.CdmaInformationRecords(new CdmaInformationRecords.CdmaSignalInfoRec(cdmaSignalInfoRecord.isPresent ? 1 : 0, cdmaSignalInfoRecord.signalType, cdmaSignalInfoRecord.alertPitch, cdmaSignalInfoRecord.signal));
                    break;
                case 5:
                    CdmaRedirectingNumberInfoRecord cdmaRedirectingNumberInfoRecord = cdmaInformationRecord.redir.get(0);
                    cdmaInformationRecords2 = new com.android.internal.telephony.cdma.CdmaInformationRecords(new CdmaInformationRecords.CdmaRedirectingNumberInfoRec(cdmaRedirectingNumberInfoRecord.redirectingNumber.number, cdmaRedirectingNumberInfoRecord.redirectingNumber.numberType, cdmaRedirectingNumberInfoRecord.redirectingNumber.numberPlan, cdmaRedirectingNumberInfoRecord.redirectingNumber.pi, cdmaRedirectingNumberInfoRecord.redirectingNumber.si, cdmaRedirectingNumberInfoRecord.redirectingReason));
                    break;
                case 6:
                    CdmaLineControlInfoRecord cdmaLineControlInfoRecord = cdmaInformationRecord.lineCtrl.get(0);
                    cdmaInformationRecords2 = new com.android.internal.telephony.cdma.CdmaInformationRecords(new CdmaInformationRecords.CdmaLineControlInfoRec(cdmaLineControlInfoRecord.lineCtrlPolarityIncluded, cdmaLineControlInfoRecord.lineCtrlToggle, cdmaLineControlInfoRecord.lineCtrlReverse, cdmaLineControlInfoRecord.lineCtrlPowerDenial));
                    break;
                case 8:
                    cdmaInformationRecords2 = new com.android.internal.telephony.cdma.CdmaInformationRecords(new CdmaInformationRecords.CdmaT53ClirInfoRec(cdmaInformationRecord.clir.get(0).cause));
                    break;
                case 9:
                default:
                    throw new RuntimeException("RIL_UNSOL_CDMA_INFO_REC: unsupported record. Got " + com.android.internal.telephony.cdma.CdmaInformationRecords.idToString(i3) + " ");
                case 10:
                    CdmaT53AudioControlInfoRecord cdmaT53AudioControlInfoRecord = cdmaInformationRecord.audioCtrl.get(0);
                    cdmaInformationRecords2 = new com.android.internal.telephony.cdma.CdmaInformationRecords(new CdmaInformationRecords.CdmaT53AudioControlInfoRec(cdmaT53AudioControlInfoRecord.upLink, cdmaT53AudioControlInfoRecord.downLink));
                    break;
            }
            this.mRil.unsljLogRet(1027, cdmaInformationRecords2);
            this.mRil.notifyRegistrantsCdmaInfoRec(cdmaInformationRecords2);
        }
    }

    @Override
    public void indicateRingbackTone(int i, boolean z) {
        this.mRil.processIndication(i);
        this.mRil.unsljLogvRet(1029, Boolean.valueOf(z));
        this.mRil.mRingbackToneRegistrants.notifyRegistrants(new AsyncResult((Object) null, Boolean.valueOf(z), (Throwable) null));
    }

    @Override
    public void resendIncallMute(int i) {
        this.mRil.processIndication(i);
        this.mRil.unsljLog(1030);
        this.mRil.mResendIncallMuteRegistrants.notifyRegistrants();
    }

    @Override
    public void cdmaSubscriptionSourceChanged(int i, int i2) {
        this.mRil.processIndication(i);
        int[] iArr = {i2};
        this.mRil.unsljLogRet(1031, iArr);
        this.mRil.mCdmaSubscriptionChangedRegistrants.notifyRegistrants(new AsyncResult((Object) null, iArr, (Throwable) null));
    }

    @Override
    public void cdmaPrlChanged(int i, int i2) {
        this.mRil.processIndication(i);
        int[] iArr = {i2};
        this.mRil.unsljLogRet(1032, iArr);
        this.mRil.mCdmaPrlChangedRegistrants.notifyRegistrants(new AsyncResult((Object) null, iArr, (Throwable) null));
    }

    @Override
    public void exitEmergencyCallbackMode(int i) {
        this.mRil.processIndication(i);
        this.mRil.unsljLog(1033);
        this.mRil.mExitEmergencyCallbackModeRegistrants.notifyRegistrants();
    }

    @Override
    public void rilConnected(int i) {
        this.mRil.processIndication(i);
        this.mRil.unsljLog(1034);
        this.mRil.setRadioPower(false, null);
        this.mRil.setCdmaSubscriptionSource(this.mRil.mCdmaSubscription, null);
        this.mRil.setCellInfoListRate();
        this.mRil.notifyRegistrantsRilConnectionChanged(15);
    }

    @Override
    public void voiceRadioTechChanged(int i, int i2) {
        this.mRil.processIndication(i);
        this.mRil.mNewVoiceTech = i2;
        int[] iArr = {i2};
        this.mRil.unsljLogRet(1035, iArr);
        this.mRil.mVoiceRadioTechChangedRegistrants.notifyRegistrants(new AsyncResult((Object) null, iArr, (Throwable) null));
    }

    @Override
    public void cellInfoList(int i, ArrayList<CellInfo> arrayList) {
        this.mRil.processIndication(i);
        ArrayList<android.telephony.CellInfo> arrayListConvertHalCellInfoList = RIL.convertHalCellInfoList(arrayList);
        this.mRil.unsljLogRet(1036, arrayListConvertHalCellInfoList);
        this.mRil.mRilCellInfoListRegistrants.notifyRegistrants(new AsyncResult((Object) null, arrayListConvertHalCellInfoList, (Throwable) null));
    }

    @Override
    public void cellInfoList_1_2(int i, ArrayList<android.hardware.radio.V1_2.CellInfo> arrayList) {
        this.mRil.processIndication(i);
        ArrayList<android.telephony.CellInfo> arrayListConvertHalCellInfoList_1_2 = RIL.convertHalCellInfoList_1_2(arrayList);
        this.mRil.unsljLogRet(1036, arrayListConvertHalCellInfoList_1_2);
        this.mRil.mRilCellInfoListRegistrants.notifyRegistrants(new AsyncResult((Object) null, arrayListConvertHalCellInfoList_1_2, (Throwable) null));
    }

    @Override
    public void networkScanResult(int i, NetworkScanResult networkScanResult) {
        responseCellInfos(i, networkScanResult);
    }

    @Override
    public void networkScanResult_1_2(int i, android.hardware.radio.V1_2.NetworkScanResult networkScanResult) {
        responseCellInfos_1_2(i, networkScanResult);
    }

    @Override
    public void imsNetworkStateChanged(int i) {
        this.mRil.processIndication(i);
        this.mRil.unsljLog(1037);
        this.mRil.mImsNetworkStateChangedRegistrants.notifyRegistrants();
    }

    @Override
    public void subscriptionStatusChanged(int i, boolean z) {
        this.mRil.processIndication(i);
        int[] iArr = {z ? 1 : 0};
        this.mRil.unsljLogRet(1038, iArr);
        this.mRil.mSubscriptionStatusRegistrants.notifyRegistrants(new AsyncResult((Object) null, iArr, (Throwable) null));
    }

    @Override
    public void srvccStateNotify(int i, int i2) {
        this.mRil.processIndication(i);
        int[] iArr = {i2};
        this.mRil.unsljLogRet(1039, iArr);
        this.mRil.writeMetricsSrvcc(i2);
        this.mRil.mSrvccStateRegistrants.notifyRegistrants(new AsyncResult((Object) null, iArr, (Throwable) null));
    }

    @Override
    public void hardwareConfigChanged(int i, ArrayList<android.hardware.radio.V1_0.HardwareConfig> arrayList) {
        this.mRil.processIndication(i);
        ArrayList<HardwareConfig> arrayListConvertHalHwConfigList = this.mRil.convertHalHwConfigList(arrayList, this.mRil);
        this.mRil.unsljLogRet(1040, arrayListConvertHalHwConfigList);
        this.mRil.mHardwareConfigChangeRegistrants.notifyRegistrants(new AsyncResult((Object) null, arrayListConvertHalHwConfigList, (Throwable) null));
    }

    @Override
    public void radioCapabilityIndication(int i, android.hardware.radio.V1_0.RadioCapability radioCapability) {
        this.mRil.processIndication(i);
        RadioCapability radioCapabilityConvertHalRadioCapability = RIL.convertHalRadioCapability(radioCapability, this.mRil);
        this.mRil.unsljLogRet(1042, radioCapabilityConvertHalRadioCapability);
        this.mRil.mPhoneRadioCapabilityChangedRegistrants.notifyRegistrants(new AsyncResult((Object) null, radioCapabilityConvertHalRadioCapability, (Throwable) null));
    }

    @Override
    public void onSupplementaryServiceIndication(int i, StkCcUnsolSsResult stkCcUnsolSsResult) {
        this.mRil.processIndication(i);
        SsData ssData = new SsData();
        ssData.serviceType = ssData.ServiceTypeFromRILInt(stkCcUnsolSsResult.serviceType);
        ssData.requestType = ssData.RequestTypeFromRILInt(stkCcUnsolSsResult.requestType);
        ssData.teleserviceType = ssData.TeleserviceTypeFromRILInt(stkCcUnsolSsResult.teleserviceType);
        ssData.serviceClass = stkCcUnsolSsResult.serviceClass;
        ssData.result = stkCcUnsolSsResult.result;
        int i2 = 0;
        if (ssData.serviceType.isTypeCF() && ssData.requestType.isTypeInterrogation()) {
            CfData cfData = stkCcUnsolSsResult.cfData.get(0);
            int size = cfData.cfInfo.size();
            ssData.cfInfo = new CallForwardInfo[size];
            while (i2 < size) {
                android.hardware.radio.V1_0.CallForwardInfo callForwardInfo = cfData.cfInfo.get(i2);
                ssData.cfInfo[i2] = new CallForwardInfo();
                ssData.cfInfo[i2].status = callForwardInfo.status;
                ssData.cfInfo[i2].reason = callForwardInfo.reason;
                ssData.cfInfo[i2].serviceClass = callForwardInfo.serviceClass;
                ssData.cfInfo[i2].toa = callForwardInfo.toa;
                ssData.cfInfo[i2].number = callForwardInfo.number;
                ssData.cfInfo[i2].timeSeconds = callForwardInfo.timeSeconds;
                this.mRil.riljLog("[SS Data] CF Info " + i2 + " : " + ssData.cfInfo[i2]);
                i2++;
            }
        } else {
            SsInfoData ssInfoData = stkCcUnsolSsResult.ssInfo.get(0);
            int size2 = ssInfoData.ssInfo.size();
            ssData.ssInfo = new int[size2];
            while (i2 < size2) {
                ssData.ssInfo[i2] = ssInfoData.ssInfo.get(i2).intValue();
                this.mRil.riljLog("[SS Data] SS Info " + i2 + " : " + ssData.ssInfo[i2]);
                i2++;
            }
        }
        this.mRil.unsljLogRet(1043, ssData);
        if (this.mRil.mSsRegistrant != null) {
            this.mRil.mSsRegistrant.notifyRegistrant(new AsyncResult((Object) null, ssData, (Throwable) null));
        }
    }

    @Override
    public void stkCallControlAlphaNotify(int i, String str) {
        this.mRil.processIndication(i);
        this.mRil.unsljLogRet(1044, str);
        if (this.mRil.mCatCcAlphaRegistrant != null) {
            this.mRil.mCatCcAlphaRegistrant.notifyRegistrant(new AsyncResult((Object) null, str, (Throwable) null));
        }
    }

    @Override
    public void lceData(int i, LceDataInfo lceDataInfo) {
        this.mRil.processIndication(i);
        LinkCapacityEstimate linkCapacityEstimateConvertHalLceData = RIL.convertHalLceData(lceDataInfo, this.mRil);
        this.mRil.unsljLogRet(1045, linkCapacityEstimateConvertHalLceData);
        if (this.mRil.mLceInfoRegistrants != null) {
            this.mRil.mLceInfoRegistrants.notifyRegistrants(new AsyncResult((Object) null, linkCapacityEstimateConvertHalLceData, (Throwable) null));
        }
    }

    @Override
    public void pcoData(int i, PcoDataInfo pcoDataInfo) {
        this.mRil.processIndication(i);
        PcoData pcoData = new PcoData(pcoDataInfo.cid, pcoDataInfo.bearerProto, pcoDataInfo.pcoId, RIL.arrayListToPrimitiveArray(pcoDataInfo.contents));
        this.mRil.unsljLogRet(1046, pcoData);
        this.mRil.mPcoDataRegistrants.notifyRegistrants(new AsyncResult((Object) null, pcoData, (Throwable) null));
    }

    @Override
    public void modemReset(int i, String str) {
        this.mRil.processIndication(i);
        this.mRil.unsljLogRet(1047, str);
        this.mRil.writeMetricsModemRestartEvent(str);
        this.mRil.mModemResetRegistrants.notifyRegistrants(new AsyncResult((Object) null, str, (Throwable) null));
    }

    @Override
    public void carrierInfoForImsiEncryption(int i) {
        this.mRil.processIndication(i);
        this.mRil.unsljLogRet(1048, null);
        this.mRil.mCarrierInfoForImsiEncryptionRegistrants.notifyRegistrants(new AsyncResult((Object) null, (Object) null, (Throwable) null));
    }

    @Override
    public void keepaliveStatus(int i, android.hardware.radio.V1_1.KeepaliveStatus keepaliveStatus) {
        this.mRil.processIndication(i);
        this.mRil.unsljLogRet(1051, "handle=" + keepaliveStatus.sessionHandle + " code=" + keepaliveStatus.code);
        this.mRil.mNattKeepaliveStatusRegistrants.notifyRegistrants(new AsyncResult((Object) null, new KeepaliveStatus(keepaliveStatus.sessionHandle, keepaliveStatus.code), (Throwable) null));
    }

    private CommandsInterface.RadioState getRadioStateFromInt(int i) {
        if (i != 10) {
            switch (i) {
                case 0:
                    return CommandsInterface.RadioState.RADIO_OFF;
                case 1:
                    return CommandsInterface.RadioState.RADIO_UNAVAILABLE;
                default:
                    throw new RuntimeException("Unrecognized RadioState: " + i);
            }
        }
        return CommandsInterface.RadioState.RADIO_ON;
    }

    private void responseCellInfos(int i, NetworkScanResult networkScanResult) {
        this.mRil.processIndication(i);
        NetworkScanResult networkScanResult2 = new NetworkScanResult(networkScanResult.status, networkScanResult.error, RIL.convertHalCellInfoList(networkScanResult.networkInfos));
        this.mRil.unsljLogRet(1049, networkScanResult2);
        this.mRil.mRilNetworkScanResultRegistrants.notifyRegistrants(new AsyncResult((Object) null, networkScanResult2, (Throwable) null));
    }

    private void responseCellInfos_1_2(int i, android.hardware.radio.V1_2.NetworkScanResult networkScanResult) {
        this.mRil.processIndication(i);
        NetworkScanResult networkScanResult2 = new NetworkScanResult(networkScanResult.status, networkScanResult.error, RIL.convertHalCellInfoList_1_2(networkScanResult.networkInfos));
        this.mRil.unsljLogRet(1049, networkScanResult2);
        this.mRil.mRilNetworkScanResultRegistrants.notifyRegistrants(new AsyncResult((Object) null, networkScanResult2, (Throwable) null));
    }
}
