package com.mediatek.internal.telephony;

import android.content.Intent;
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
import android.hardware.radio.V1_1.NetworkScanResult;
import android.hardware.radio.V1_2.CellInfoGsm;
import android.hardware.radio.V1_2.CellInfoLte;
import android.hardware.radio.V1_2.CellInfoWcdma;
import android.hardware.radio.V1_2.LinkCapacityEstimate;
import android.hardware.radio.V1_2.PhysicalChannelConfig;
import android.os.AsyncResult;
import android.os.Build;
import android.os.Message;
import android.os.Parcelable;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.telephony.SubscriptionManager;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.RIL;
import com.android.internal.telephony.RadioIndication;
import com.android.internal.telephony.uicc.IccUtils;
import com.mediatek.internal.telephony.dataconnection.PcoDataAfterAttached;
import com.mediatek.internal.telephony.gsm.MtkSuppCrssNotification;
import com.mediatek.internal.telephony.ims.MtkDedicateDataCallResponse;
import com.mediatek.internal.telephony.ppl.IPplSmsFilter;
import com.mediatek.internal.telephony.worldphone.WorldMode;
import com.mediatek.telephony.internal.telephony.vsim.ExternalSimManager;
import java.util.ArrayList;
import java.util.Iterator;
import vendor.mediatek.hardware.radio.V3_0.CfuStatusNotification;
import vendor.mediatek.hardware.radio.V3_0.CipherNotification;
import vendor.mediatek.hardware.radio.V3_0.CrssNotification;
import vendor.mediatek.hardware.radio.V3_0.DedicateDataCall;
import vendor.mediatek.hardware.radio.V3_0.EtwsNotification;
import vendor.mediatek.hardware.radio.V3_0.IncomingCallNotification;
import vendor.mediatek.hardware.radio.V3_0.PcoDataAttachedInfo;
import vendor.mediatek.hardware.radio.V3_0.SignalStrengthWithWcdmaEcio;
import vendor.mediatek.hardware.radio.V3_0.VsimOperationEvent;

public class MtkRadioIndication extends MtkRadioIndicationBase {
    private static final boolean ENG = "eng".equals(Build.TYPE);
    private static final String TAG = "MtkRadioInd";
    private MtkRIL mMtkRil;
    RadioIndication mRadioIndication;

    MtkRadioIndication(RIL ril) {
        super(ril);
        this.mRadioIndication = new RadioIndication(ril);
        this.mMtkRil = (MtkRIL) ril;
    }

    @Override
    public void radioStateChanged(int i, int i2) {
        CommandsInterface.RadioState radioState = this.mMtkRil.getRadioState();
        this.mRadioIndication.radioStateChanged(i, i2);
        if (this.mMtkRil.getRadioState() != radioState) {
            Intent intent = new Intent("com.mediatek.intent.action.RADIO_STATE_CHANGED");
            intent.putExtra("radioState", i2);
            intent.putExtra(IPplSmsFilter.KEY_SUB_ID, MtkSubscriptionManager.getSubIdUsingPhoneId(this.mMtkRil.mInstanceId.intValue()));
            this.mMtkRil.mMtkContext.sendBroadcastAsUser(intent, UserHandle.ALL);
            this.mMtkRil.riljLog("Broadcast for RadioStateChanged: state=" + i2);
        }
    }

    @Override
    public void callStateChanged(int i) {
        this.mRadioIndication.callStateChanged(i);
    }

    @Override
    public void networkStateChanged(int i) {
        this.mRadioIndication.networkStateChanged(i);
    }

    @Override
    public void newSms(int i, ArrayList<Byte> arrayList) {
        this.mRadioIndication.newSms(i, arrayList);
    }

    @Override
    public void newSmsStatusReport(int i, ArrayList<Byte> arrayList) {
        this.mRadioIndication.newSmsStatusReport(i, arrayList);
    }

    @Override
    public void newSmsOnSim(int i, int i2) {
        this.mRadioIndication.newSmsOnSim(i, i2);
    }

    @Override
    public void onUssd(int i, int i2, String str) {
        this.mRadioIndication.onUssd(i, i2, str);
    }

    @Override
    public void nitzTimeReceived(int i, String str, long j) {
        this.mRadioIndication.nitzTimeReceived(i, str, j);
    }

    @Override
    public void currentSignalStrength(int i, SignalStrength signalStrength) {
        this.mRadioIndication.currentSignalStrength(i, signalStrength);
    }

    @Override
    public void currentLinkCapacityEstimate(int i, LinkCapacityEstimate linkCapacityEstimate) {
        this.mRadioIndication.currentLinkCapacityEstimate(i, linkCapacityEstimate);
    }

    @Override
    public void currentSignalStrength_1_2(int i, android.hardware.radio.V1_2.SignalStrength signalStrength) {
        this.mRadioIndication.currentSignalStrength_1_2(i, signalStrength);
    }

    @Override
    public void currentPhysicalChannelConfigs(int i, ArrayList<PhysicalChannelConfig> arrayList) {
        this.mRadioIndication.currentPhysicalChannelConfigs(i, arrayList);
    }

    @Override
    public void dataCallListChanged(int i, ArrayList<SetupDataCallResult> arrayList) {
        this.mRadioIndication.dataCallListChanged(i, arrayList);
    }

    @Override
    public void suppSvcNotify(int i, SuppSvcNotification suppSvcNotification) {
        this.mRadioIndication.suppSvcNotify(i, suppSvcNotification);
    }

    @Override
    public void cfuStatusNotify(int i, CfuStatusNotification cfuStatusNotification) {
        this.mMtkRil.processIndication(i);
        int[] iArr = {cfuStatusNotification.status, cfuStatusNotification.lineId};
        this.mMtkRil.unsljLogRet(3070, iArr);
        if (iArr[1] == 1) {
            this.mMtkRil.mCfuReturnValue = iArr;
        }
        if (this.mMtkRil.mCallForwardingInfoRegistrants.size() != 0 && iArr[1] == 1) {
            this.mMtkRil.mCallForwardingInfoRegistrants.notifyRegistrants(new AsyncResult((Object) null, iArr, (Throwable) null));
        }
    }

    @Override
    public void incomingCallIndication(int i, IncomingCallNotification incomingCallNotification) {
        this.mMtkRil.processIndication(i);
        String[] strArr = new String[7];
        strArr[0] = incomingCallNotification.callId;
        strArr[1] = incomingCallNotification.number;
        strArr[2] = incomingCallNotification.type;
        strArr[3] = incomingCallNotification.callMode;
        strArr[4] = incomingCallNotification.seqNo;
        strArr[5] = incomingCallNotification.redirectNumber;
        this.mMtkRil.unsljLogRet(3015, strArr);
        if (this.mMtkRil.mIncomingCallIndicationRegistrant != null) {
            this.mMtkRil.mIncomingCallIndicationRegistrant.notifyRegistrant(new AsyncResult((Object) null, strArr, (Throwable) null));
        }
    }

    @Override
    public void cipherIndication(int i, CipherNotification cipherNotification) {
        this.mMtkRil.processIndication(i);
        String[] strArr = {cipherNotification.simCipherStatus, cipherNotification.sessionStatus, cipherNotification.csStatus, cipherNotification.psStatus};
        this.mMtkRil.unsljLogRet(3024, strArr);
        if (this.mMtkRil.mCipherIndicationRegistrant != null) {
            this.mMtkRil.mCipherIndicationRegistrant.notifyRegistrants(new AsyncResult((Object) null, strArr, (Throwable) null));
        }
    }

    @Override
    public void crssIndication(int i, CrssNotification crssNotification) {
        this.mMtkRil.processIndication(i);
        MtkSuppCrssNotification mtkSuppCrssNotification = new MtkSuppCrssNotification();
        mtkSuppCrssNotification.code = crssNotification.code;
        mtkSuppCrssNotification.type = crssNotification.type;
        mtkSuppCrssNotification.alphaid = crssNotification.alphaid;
        mtkSuppCrssNotification.number = crssNotification.number;
        mtkSuppCrssNotification.cli_validity = crssNotification.cli_validity;
        this.mMtkRil.unsljLogRet(3025, mtkSuppCrssNotification);
        if (this.mMtkRil.mCallRelatedSuppSvcRegistrant != null) {
            this.mMtkRil.mCallRelatedSuppSvcRegistrant.notifyRegistrant(new AsyncResult((Object) null, mtkSuppCrssNotification, (Throwable) null));
        }
    }

    @Override
    public void speechCodecInfoIndication(int i, int i2) {
        this.mMtkRil.processIndication(i);
        this.mMtkRil.unsljLogvRet(3027, Integer.valueOf(i2));
        if (this.mMtkRil.mSpeechCodecInfoRegistrant != null) {
            this.mMtkRil.mSpeechCodecInfoRegistrant.notifyRegistrant(new AsyncResult((Object) null, Integer.valueOf(i2), (Throwable) null));
        }
    }

    @Override
    public void cdmaCallAccepted(int i) {
        this.mMtkRil.processIndication(i);
        this.mMtkRil.unsljLogRet(3069, Integer.valueOf(i));
        if (this.mMtkRil.mCdmaCallAcceptedRegistrant != null) {
            this.mMtkRil.mCdmaCallAcceptedRegistrant.notifyRegistrants(new AsyncResult((Object) null, (Object) null, (Throwable) null));
        }
    }

    @Override
    public void eccNumIndication(int i, String str, String str2) {
    }

    @Override
    public void stkSessionEnd(int i) {
        this.mRadioIndication.stkSessionEnd(i);
    }

    @Override
    public void stkProactiveCommand(int i, String str) {
        this.mRadioIndication.stkProactiveCommand(i, str);
    }

    @Override
    public void stkEventNotify(int i, String str) {
        this.mRadioIndication.stkEventNotify(i, str);
    }

    @Override
    public void stkCallSetup(int i, long j) {
        this.mRadioIndication.stkCallSetup(i, j);
    }

    @Override
    public void simSmsStorageFull(int i) {
        this.mRadioIndication.simSmsStorageFull(i);
    }

    @Override
    public void simRefresh(int i, SimRefreshResult simRefreshResult) {
        this.mRadioIndication.simRefresh(i, simRefreshResult);
    }

    @Override
    public void callRing(int i, boolean z, CdmaSignalInfoRecord cdmaSignalInfoRecord) {
        this.mRadioIndication.callRing(i, z, cdmaSignalInfoRecord);
    }

    @Override
    public void simStatusChanged(int i) {
        this.mRadioIndication.simStatusChanged(i);
    }

    @Override
    public void cdmaNewSms(int i, CdmaSmsMessage cdmaSmsMessage) {
        this.mRadioIndication.cdmaNewSms(i, cdmaSmsMessage);
    }

    @Override
    public void newBroadcastSms(int i, ArrayList<Byte> arrayList) {
        this.mRadioIndication.newBroadcastSms(i, arrayList);
    }

    @Override
    public void cdmaRuimSmsStorageFull(int i) {
        this.mRadioIndication.cdmaRuimSmsStorageFull(i);
    }

    @Override
    public void restrictedStateChanged(int i, int i2) {
        this.mRadioIndication.restrictedStateChanged(i, i2);
    }

    @Override
    public void enterEmergencyCallbackMode(int i) {
        this.mRadioIndication.enterEmergencyCallbackMode(i);
    }

    @Override
    public void cdmaCallWaiting(int i, CdmaCallWaiting cdmaCallWaiting) {
        this.mRadioIndication.cdmaCallWaiting(i, cdmaCallWaiting);
    }

    @Override
    public void cdmaOtaProvisionStatus(int i, int i2) {
        this.mRadioIndication.cdmaOtaProvisionStatus(i, i2);
    }

    @Override
    public void cdmaInfoRec(int i, CdmaInformationRecords cdmaInformationRecords) {
        this.mRadioIndication.cdmaInfoRec(i, cdmaInformationRecords);
    }

    @Override
    public void indicateRingbackTone(int i, boolean z) {
        this.mRadioIndication.indicateRingbackTone(i, z);
    }

    @Override
    public void resendIncallMute(int i) {
        this.mRadioIndication.resendIncallMute(i);
    }

    @Override
    public void cdmaSubscriptionSourceChanged(int i, int i2) {
        this.mRadioIndication.cdmaSubscriptionSourceChanged(i, i2);
    }

    @Override
    public void cdmaPrlChanged(int i, int i2) {
        this.mRadioIndication.cdmaPrlChanged(i, i2);
    }

    @Override
    public void exitEmergencyCallbackMode(int i) {
        this.mRadioIndication.exitEmergencyCallbackMode(i);
    }

    @Override
    public void rilConnected(int i) {
        this.mRadioIndication.mRil.processIndication(i);
        this.mMtkRil.unsljLog(1034);
        this.mRadioIndication.mRil.setCdmaSubscriptionSource(this.mRadioIndication.mRil.mCdmaSubscription, (Message) null);
        this.mRadioIndication.mRil.setCellInfoListRate();
        this.mRadioIndication.mRil.notifyRegistrantsRilConnectionChanged(15);
    }

    @Override
    public void voiceRadioTechChanged(int i, int i2) {
        this.mRadioIndication.voiceRadioTechChanged(i, i2);
    }

    @Override
    public void cellInfoList(int i, ArrayList<CellInfo> arrayList) {
        this.mRadioIndication.cellInfoList(i, arrayList);
    }

    @Override
    public void cellInfoList_1_2(int i, ArrayList<android.hardware.radio.V1_2.CellInfo> arrayList) {
        this.mRadioIndication.cellInfoList_1_2(i, arrayList);
    }

    private int getSubId(int i) {
        int[] subId = SubscriptionManager.getSubId(i);
        if (subId != null && subId.length > 0) {
            return subId[0];
        }
        return -1;
    }

    @Override
    public void networkScanResult(int i, NetworkScanResult networkScanResult) {
        this.mRadioIndication.networkScanResult(i, networkScanResult);
    }

    @Override
    public void networkScanResult_1_2(int i, android.hardware.radio.V1_2.NetworkScanResult networkScanResult) {
        String str;
        Iterator<android.hardware.radio.V1_2.CellInfo> it = networkScanResult.networkInfos.iterator();
        while (it.hasNext()) {
            android.hardware.radio.V1_2.CellInfo next = it.next();
            switch (next.cellInfoType) {
                case 1:
                    CellInfoGsm cellInfoGsm = next.gsm.get(0);
                    str = cellInfoGsm.cellIdentityGsm.base.mcc + cellInfoGsm.cellIdentityGsm.base.mnc;
                    int i2 = cellInfoGsm.cellIdentityGsm.base.lac;
                    cellInfoGsm.cellIdentityGsm.operatorNames.alphaLong = this.mMtkRil.lookupOperatorName(getSubId(this.mMtkRil.mInstanceId.intValue()), str, true, i2);
                    cellInfoGsm.cellIdentityGsm.operatorNames.alphaShort = this.mMtkRil.lookupOperatorName(getSubId(this.mMtkRil.mInstanceId.intValue()), str, false, i2);
                    cellInfoGsm.cellIdentityGsm.operatorNames.alphaLong = cellInfoGsm.cellIdentityGsm.operatorNames.alphaLong.concat(" 2G");
                    cellInfoGsm.cellIdentityGsm.operatorNames.alphaShort = cellInfoGsm.cellIdentityGsm.operatorNames.alphaShort.concat(" 2G");
                    this.mMtkRil.riljLog("mccmnc=" + str + ", lac=" + i2 + ", longName=" + cellInfoGsm.cellIdentityGsm.operatorNames.alphaLong + " shortName=" + cellInfoGsm.cellIdentityGsm.operatorNames.alphaShort);
                    break;
                case 2:
                    str = null;
                    break;
                case 3:
                    CellInfoLte cellInfoLte = next.lte.get(0);
                    str = cellInfoLte.cellIdentityLte.base.mcc + cellInfoLte.cellIdentityLte.base.mnc;
                    int i3 = cellInfoLte.cellIdentityLte.base.tac;
                    cellInfoLte.cellIdentityLte.operatorNames.alphaLong = this.mMtkRil.lookupOperatorName(getSubId(this.mMtkRil.mInstanceId.intValue()), str, true, i3);
                    cellInfoLte.cellIdentityLte.operatorNames.alphaShort = this.mMtkRil.lookupOperatorName(getSubId(this.mMtkRil.mInstanceId.intValue()), str, false, i3);
                    cellInfoLte.cellIdentityLte.operatorNames.alphaLong = cellInfoLte.cellIdentityLte.operatorNames.alphaLong.concat(" 4G");
                    cellInfoLte.cellIdentityLte.operatorNames.alphaShort = cellInfoLte.cellIdentityLte.operatorNames.alphaShort.concat(" 4G");
                    this.mMtkRil.riljLog("mccmnc=" + str + ", lac=" + i3 + ", longName=" + cellInfoLte.cellIdentityLte.operatorNames.alphaLong + " shortName=" + cellInfoLte.cellIdentityLte.operatorNames.alphaShort);
                    break;
                case 4:
                    CellInfoWcdma cellInfoWcdma = next.wcdma.get(0);
                    str = cellInfoWcdma.cellIdentityWcdma.base.mcc + cellInfoWcdma.cellIdentityWcdma.base.mnc;
                    int i4 = cellInfoWcdma.cellIdentityWcdma.base.lac;
                    cellInfoWcdma.cellIdentityWcdma.operatorNames.alphaLong = this.mMtkRil.lookupOperatorName(getSubId(this.mMtkRil.mInstanceId.intValue()), str, true, i4);
                    cellInfoWcdma.cellIdentityWcdma.operatorNames.alphaShort = this.mMtkRil.lookupOperatorName(getSubId(this.mMtkRil.mInstanceId.intValue()), str, false, i4);
                    cellInfoWcdma.cellIdentityWcdma.operatorNames.alphaLong = cellInfoWcdma.cellIdentityWcdma.operatorNames.alphaLong.concat(" 3G");
                    cellInfoWcdma.cellIdentityWcdma.operatorNames.alphaShort = cellInfoWcdma.cellIdentityWcdma.operatorNames.alphaShort.concat(" 3G");
                    this.mMtkRil.riljLog("mccmnc=" + str + ", lac=" + i4 + ", longName=" + cellInfoWcdma.cellIdentityWcdma.operatorNames.alphaLong + " shortName=" + cellInfoWcdma.cellIdentityWcdma.operatorNames.alphaShort);
                    break;
                default:
                    throw new RuntimeException("unexpected cellinfotype: " + next.cellInfoType);
            }
            if (this.mMtkRil.hidePLMN(str)) {
                it.remove();
                this.mMtkRil.riljLog("remove this one " + str);
            }
        }
        this.mRadioIndication.networkScanResult_1_2(i, networkScanResult);
    }

    @Override
    public void imsNetworkStateChanged(int i) {
        this.mRadioIndication.imsNetworkStateChanged(i);
    }

    @Override
    public void subscriptionStatusChanged(int i, boolean z) {
        this.mRadioIndication.subscriptionStatusChanged(i, z);
    }

    @Override
    public void srvccStateNotify(int i, int i2) {
        this.mRadioIndication.srvccStateNotify(i, i2);
    }

    @Override
    public void hardwareConfigChanged(int i, ArrayList<HardwareConfig> arrayList) {
        this.mRadioIndication.hardwareConfigChanged(i, arrayList);
    }

    @Override
    public void radioCapabilityIndication(int i, RadioCapability radioCapability) {
        this.mRadioIndication.radioCapabilityIndication(i, radioCapability);
    }

    @Override
    public void onSupplementaryServiceIndication(int i, StkCcUnsolSsResult stkCcUnsolSsResult) {
        this.mRadioIndication.onSupplementaryServiceIndication(i, stkCcUnsolSsResult);
    }

    @Override
    public void stkCallControlAlphaNotify(int i, String str) {
        this.mRadioIndication.stkCallControlAlphaNotify(i, str);
    }

    @Override
    public void lceData(int i, LceDataInfo lceDataInfo) {
        this.mRadioIndication.lceData(i, lceDataInfo);
    }

    @Override
    public void pcoData(int i, PcoDataInfo pcoDataInfo) {
        this.mRadioIndication.pcoData(i, pcoDataInfo);
    }

    @Override
    public void modemReset(int i, String str) {
        this.mRadioIndication.modemReset(i, str);
    }

    @Override
    public void responseCsNetworkStateChangeInd(int i, ArrayList<String> arrayList) {
        this.mMtkRil.processIndication(i);
        this.mMtkRil.riljLog("[UNSL]< UNSOL_RESPONSE_CS_NETWORK_STATE_CHANGED");
        if (this.mMtkRil.mCsNetworkStateRegistrants.size() != 0) {
            this.mMtkRil.mCsNetworkStateRegistrants.notifyRegistrants(new AsyncResult((Object) null, arrayList.toArray(new String[arrayList.size()]), (Throwable) null));
        }
    }

    @Override
    public void responsePsNetworkStateChangeInd(int i, ArrayList<Integer> arrayList) {
        this.mMtkRil.processIndication(i);
        this.mMtkRil.riljLog("[UNSL]< UNSOL_RESPONSE_PS_NETWORK_STATE_CHANGED");
        int[] iArr = new int[arrayList.size()];
        for (int i2 = 0; i2 < arrayList.size(); i2++) {
            iArr[i2] = arrayList.get(i2).intValue();
        }
        if (this.mMtkRil.mPsNetworkStateRegistrants.size() != 0) {
            this.mMtkRil.mPsNetworkStateRegistrants.notifyRegistrants(new AsyncResult((Object) null, iArr, (Throwable) null));
        }
    }

    @Override
    public void responseNetworkEventInd(int i, ArrayList<Integer> arrayList) {
        this.mRadioIndication.mRil.processIndication(i);
        int[] iArr = new int[arrayList.size()];
        for (int i2 = 0; i2 < arrayList.size(); i2++) {
            iArr[i2] = arrayList.get(i2).intValue();
        }
        this.mMtkRil.unsljLogRet(3018, iArr);
        MtkRIL mtkRIL = (MtkRIL) this.mRadioIndication.mRil;
        if (mtkRIL.mNetworkEventRegistrants.size() != 0) {
            mtkRIL.mNetworkEventRegistrants.notifyRegistrants(new AsyncResult((Object) null, iArr, (Throwable) null));
        }
    }

    @Override
    public void networkRejectCauseInd(int i, ArrayList<Integer> arrayList) {
        this.mRadioIndication.mRil.processIndication(i);
        int[] iArr = new int[arrayList.size()];
        for (int i2 = 0; i2 < arrayList.size(); i2++) {
            iArr[i2] = arrayList.get(i2).intValue();
        }
        this.mMtkRil.unsljLogRet(3109, iArr);
        MtkRIL mtkRIL = (MtkRIL) this.mRadioIndication.mRil;
        if (mtkRIL.mNetworkRejectRegistrants.size() != 0) {
            mtkRIL.mNetworkRejectRegistrants.notifyRegistrants(new AsyncResult((Object) null, iArr, (Throwable) null));
        }
    }

    @Override
    public void responseModulationInfoInd(int i, ArrayList<Integer> arrayList) {
        this.mRadioIndication.mRil.processIndication(i);
        int[] iArr = new int[arrayList.size()];
        for (int i2 = 0; i2 < arrayList.size(); i2++) {
            iArr[i2] = arrayList.get(i2).intValue();
        }
        this.mMtkRil.unsljLogRet(3019, iArr);
        MtkRIL mtkRIL = (MtkRIL) this.mRadioIndication.mRil;
        if (mtkRIL.mModulationRegistrants.size() != 0) {
            mtkRIL.mModulationRegistrants.notifyRegistrants(new AsyncResult((Object) null, iArr, (Throwable) null));
        }
    }

    @Override
    public void responseInvalidSimInd(int i, ArrayList<String> arrayList) {
        this.mRadioIndication.mRil.processIndication(i);
        String[] strArr = (String[]) arrayList.toArray(new String[arrayList.size()]);
        this.mMtkRil.unsljLogRet(3016, strArr);
        MtkRIL mtkRIL = (MtkRIL) this.mRadioIndication.mRil;
        if (mtkRIL.mInvalidSimInfoRegistrant.size() != 0) {
            mtkRIL.mInvalidSimInfoRegistrant.notifyRegistrants(new AsyncResult((Object) null, strArr, (Throwable) null));
        }
    }

    @Override
    public void responseFemtocellInfo(int i, ArrayList<String> arrayList) {
        this.mMtkRil.processIndication(i);
        String[] strArr = (String[]) arrayList.toArray(new String[arrayList.size()]);
        this.mMtkRil.unsljLogRet(3029, strArr);
        if (this.mMtkRil.mFemtoCellInfoRegistrants.size() != 0) {
            this.mMtkRil.mFemtoCellInfoRegistrants.notifyRegistrants(new AsyncResult((Object) null, strArr, (Throwable) null));
        }
    }

    @Override
    public void currentSignalStrengthWithWcdmaEcioInd(int i, SignalStrengthWithWcdmaEcio signalStrengthWithWcdmaEcio) {
        this.mMtkRil.processIndication(i);
        android.telephony.SignalStrength signalStrength = new android.telephony.SignalStrength(signalStrengthWithWcdmaEcio.gsm_signalStrength, signalStrengthWithWcdmaEcio.gsm_bitErrorRate, signalStrengthWithWcdmaEcio.wcdma_rscp, signalStrengthWithWcdmaEcio.wcdma_ecio, signalStrengthWithWcdmaEcio.cdma_dbm, signalStrengthWithWcdmaEcio.cdma_ecio, signalStrengthWithWcdmaEcio.evdo_dbm, signalStrengthWithWcdmaEcio.evdo_ecio, signalStrengthWithWcdmaEcio.evdo_signalNoiseRatio, signalStrengthWithWcdmaEcio.lte_signalStrength, signalStrengthWithWcdmaEcio.lte_rsrp, signalStrengthWithWcdmaEcio.lte_rsrq, signalStrengthWithWcdmaEcio.lte_rssnr, signalStrengthWithWcdmaEcio.lte_cqi, signalStrengthWithWcdmaEcio.tdscdma_rscp);
        this.mMtkRil.unsljLogRet(3097, signalStrength);
        this.mMtkRil.riljLog("currentSignalStrengthWithWcdmaEcioInd SignalStrength=" + signalStrength);
        if (this.mMtkRil.mSignalStrengthWithWcdmaEcioRegistrants.size() != 0) {
            this.mMtkRil.mSignalStrengthWithWcdmaEcioRegistrants.notifyRegistrants(new AsyncResult((Object) null, signalStrength, (Throwable) null));
        }
    }

    @Override
    public void responseLteNetworkInfo(int i, int i2) {
        this.mMtkRil.riljLog("[UNSL]< RIL_UNSOL_LTE_NETWORK_INFO " + i2);
    }

    @Override
    public void onMccMncChanged(int i, String str) {
        this.mMtkRil.processIndication(i);
        this.mMtkRil.unsljLogRet(3096, str);
        if (this.mMtkRil.mMccMncRegistrants.size() != 0) {
            this.mMtkRil.mMccMncRegistrants.notifyRegistrants(new AsyncResult((Object) null, str, (Throwable) null));
        }
    }

    @Override
    public void onVirtualSimOn(int i, int i2) {
        this.mRadioIndication.mRil.processIndication(i);
        if (ENG) {
            this.mRadioIndication.mRil.unsljLog(3005);
        }
        MtkRIL mtkRIL = (MtkRIL) this.mRadioIndication.mRil;
        if (mtkRIL.mVirtualSimOn != null) {
            mtkRIL.mVirtualSimOn.notifyRegistrants(new AsyncResult((Object) null, Integer.valueOf(i2), (Throwable) null));
        }
    }

    @Override
    public void onVirtualSimOff(int i, int i2) {
        this.mRadioIndication.mRil.processIndication(i);
        if (ENG) {
            this.mRadioIndication.mRil.unsljLog(3006);
        }
        MtkRIL mtkRIL = (MtkRIL) this.mRadioIndication.mRil;
        if (mtkRIL.mVirtualSimOff != null) {
            mtkRIL.mVirtualSimOn.notifyRegistrants(new AsyncResult((Object) null, Integer.valueOf(i2), (Throwable) null));
        }
    }

    @Override
    public void onImeiLock(int i) {
        this.mRadioIndication.mRil.processIndication(i);
        if (ENG) {
            this.mRadioIndication.mRil.unsljLog(3007);
        }
        MtkRIL mtkRIL = (MtkRIL) this.mRadioIndication.mRil;
        if (mtkRIL.mImeiLockRegistrant != null) {
            mtkRIL.mImeiLockRegistrant.notifyRegistrants(new AsyncResult((Object) null, (Object) null, (Throwable) null));
        }
    }

    @Override
    public void onImsiRefreshDone(int i) {
        this.mRadioIndication.mRil.processIndication(i);
        if (ENG) {
            this.mRadioIndication.mRil.unsljLog(3008);
        }
        MtkRIL mtkRIL = (MtkRIL) this.mRadioIndication.mRil;
        if (mtkRIL.mImsiRefreshDoneRegistrant != null) {
            mtkRIL.mImsiRefreshDoneRegistrant.notifyRegistrants(new AsyncResult((Object) null, (Object) null, (Throwable) null));
        }
    }

    @Override
    public void onCardDetectedInd(int i) {
    }

    @Override
    public void newEtwsInd(int i, EtwsNotification etwsNotification) {
        this.mRadioIndication.mRil.processIndication(i);
        MtkEtwsNotification mtkEtwsNotification = new MtkEtwsNotification();
        mtkEtwsNotification.messageId = etwsNotification.messageId;
        mtkEtwsNotification.serialNumber = etwsNotification.serialNumber;
        mtkEtwsNotification.warningType = etwsNotification.warningType;
        mtkEtwsNotification.plmnId = etwsNotification.plmnId;
        mtkEtwsNotification.securityInfo = etwsNotification.securityInfo;
        if (ENG) {
            this.mRadioIndication.mRil.unsljLogRet(3010, mtkEtwsNotification);
        }
        MtkRIL mtkRIL = (MtkRIL) this.mRadioIndication.mRil;
        if (mtkRIL.mEtwsNotificationRegistrant != null) {
            mtkRIL.mEtwsNotificationRegistrant.notifyRegistrant(new AsyncResult((Object) null, mtkEtwsNotification, (Throwable) null));
        }
    }

    @Override
    public void meSmsStorageFullInd(int i) {
        this.mRadioIndication.mRil.processIndication(i);
        if (ENG) {
            this.mRadioIndication.mRil.unsljLog(3011);
        }
        MtkRIL mtkRIL = (MtkRIL) this.mRadioIndication.mRil;
        if (mtkRIL.mMeSmsFullRegistrant != null) {
            mtkRIL.mMeSmsFullRegistrant.notifyRegistrant();
        }
    }

    @Override
    public void smsReadyInd(int i) {
        this.mRadioIndication.mRil.processIndication(i);
        if (ENG) {
            this.mRadioIndication.mRil.unsljLog(3012);
        }
        MtkRIL mtkRIL = (MtkRIL) this.mRadioIndication.mRil;
        if (mtkRIL.mSmsReadyRegistrants.size() != 0) {
            mtkRIL.mSmsReadyRegistrants.notifyRegistrants();
            return;
        }
        if (ENG) {
            this.mRadioIndication.mRil.riljLog("Cache sms ready event");
        }
        mtkRIL.mIsSmsReady = true;
    }

    @Override
    public void dataAllowedNotification(int i, int i2) {
        this.mRadioIndication.mRil.processIndication(i);
        int[] iArr = {i2};
        this.mMtkRil.unsljLogMore(3014, i2 == 1 ? "true" : "false");
        if (this.mMtkRil.mDataAllowedRegistrants != null) {
            this.mMtkRil.mDataAllowedRegistrants.notifyRegistrants(new AsyncResult((Object) null, iArr, (Throwable) null));
        }
    }

    @Override
    public void onPseudoCellInfoInd(int i, ArrayList<Integer> arrayList) {
        PseudoCellInfo pseudoCellInfo;
        this.mMtkRil.processIndication(i);
        if (ENG) {
            this.mRadioIndication.mRil.unsljLog(3017);
        }
        int[] iArr = new int[arrayList.size()];
        boolean z = false;
        for (int i2 = 0; i2 < arrayList.size(); i2++) {
            iArr[i2] = arrayList.get(i2).intValue();
        }
        String str = SystemProperties.get(String.format("persist.vendor.radio.apc.mode%d", this.mMtkRil.mInstanceId), "0");
        int iIndexOf = str.indexOf("=");
        if (iIndexOf != -1) {
            String[] strArrSplit = str.substring(iIndexOf + 1).split(",");
            int i3 = Integer.parseInt(strArrSplit[0]);
            if (Integer.parseInt(strArrSplit[1]) == 1) {
                z = true;
            }
            pseudoCellInfo = new PseudoCellInfo(i3, z, Integer.parseInt(strArrSplit[2]), iArr);
        } else {
            pseudoCellInfo = new PseudoCellInfo(0, false, 0, iArr);
        }
        if (this.mMtkRil.mPseudoCellInfoRegistrants != null) {
            this.mMtkRil.mPseudoCellInfoRegistrants.notifyRegistrants(new AsyncResult((Object) null, pseudoCellInfo, (Throwable) null));
        }
        Intent intent = new Intent("com.mediatek.phone.ACTION_APC_INFO_NOTIFY");
        intent.putExtra("phoneId", this.mMtkRil.mInstanceId);
        intent.putExtra("info", (Parcelable) pseudoCellInfo);
        this.mMtkRil.mMtkContext.sendBroadcast(intent);
        this.mMtkRil.riljLog("Broadcast for APC info:cellInfo=" + pseudoCellInfo.toString());
    }

    @Override
    public void eMBMSSessionStatusIndication(int i, int i2) {
        this.mMtkRil.processIndication(i);
        int[] iArr = {i2};
        this.mMtkRil.unsljLogRet(3054, null);
        if (this.mMtkRil.mEmbmsSessionStatusNotificationRegistrant.size() > 0) {
            this.mMtkRil.riljLog("Notify mEmbmsSessionStatusNotificationRegistrant");
            this.mMtkRil.mEmbmsSessionStatusNotificationRegistrant.notifyRegistrants(new AsyncResult((Object) null, iArr, (Throwable) null));
        } else {
            this.mMtkRil.riljLog("No mEmbmsSessionStatusNotificationRegistrant exist");
        }
        Intent intent = new Intent("mediatek.intent.action.EMBMS_SESSION_STATUS_CHANGED");
        intent.putExtra("isActived", i2);
        this.mMtkRil.mMtkContext.sendBroadcast(intent);
    }

    @Override
    public void onSimPlugIn(int i) {
        this.mRadioIndication.mRil.processIndication(i);
        if (ENG) {
            this.mRadioIndication.mRil.unsljLog(3063);
        }
        MtkRIL mtkRIL = (MtkRIL) this.mRadioIndication.mRil;
        if (mtkRIL.mSimPlugIn != null) {
            mtkRIL.mSimPlugIn.notifyRegistrants(new AsyncResult((Object) null, (Object) null, (Throwable) null));
        }
    }

    @Override
    public void onSimPlugOut(int i) {
        this.mRadioIndication.mRil.processIndication(i);
        if (ENG) {
            this.mRadioIndication.mRil.unsljLog(3064);
        }
        MtkRIL mtkRIL = (MtkRIL) this.mRadioIndication.mRil;
        if (mtkRIL.mSimPlugOut != null) {
            mtkRIL.mSimPlugOut.notifyRegistrants(new AsyncResult((Object) null, (Object) null, (Throwable) null));
        }
    }

    @Override
    public void onSimMissing(int i, int i2) {
        this.mRadioIndication.mRil.processIndication(i);
        if (ENG) {
            this.mRadioIndication.mRil.unsljLog(3065);
        }
        MtkRIL mtkRIL = (MtkRIL) this.mRadioIndication.mRil;
        if (mtkRIL.mSimMissing != null) {
            mtkRIL.mSimMissing.notifyRegistrants(new AsyncResult((Object) null, Integer.valueOf(i2), (Throwable) null));
        }
    }

    @Override
    public void onSimRecovery(int i, int i2) {
        this.mRadioIndication.mRil.processIndication(i);
        if (ENG) {
            this.mRadioIndication.mRil.unsljLog(3066);
        }
        MtkRIL mtkRIL = (MtkRIL) this.mRadioIndication.mRil;
        if (mtkRIL.mSimRecovery != null) {
            mtkRIL.mSimRecovery.notifyRegistrants(new AsyncResult((Object) null, Integer.valueOf(i2), (Throwable) null));
        }
    }

    @Override
    public void smlSlotLockInfoChangedInd(int i, ArrayList<Integer> arrayList) {
        this.mRadioIndication.mRil.processIndication(i);
        if (ENG) {
            this.mRadioIndication.mRil.unsljLog(3115);
        }
        int[] iArr = new int[arrayList.size()];
        for (int i2 = 0; i2 < arrayList.size(); i2++) {
            iArr[i2] = arrayList.get(i2).intValue();
        }
        MtkRIL mtkRIL = (MtkRIL) this.mRadioIndication.mRil;
        mtkRIL.mSmlSlotLockInfo = iArr;
        if (mtkRIL.mSmlSlotLockInfoChanged.size() != 0) {
            mtkRIL.mSmlSlotLockInfoChanged.notifyRegistrants(new AsyncResult((Object) null, iArr, (Throwable) null));
        }
    }

    @Override
    public void eMBMSAtInfoIndication(int i, String str) {
        this.mMtkRil.processIndication(i);
        String str2 = new String(str);
        this.mMtkRil.unsljLogRet(3055, str2);
        if (this.mMtkRil.mEmbmsAtInfoNotificationRegistrant.size() > 0) {
            this.mMtkRil.riljLog("Notify mEmbmsAtInfoNotificationRegistrant");
            this.mMtkRil.mEmbmsAtInfoNotificationRegistrant.notifyRegistrants(new AsyncResult((Object) null, str2, (Throwable) null));
        } else {
            this.mMtkRil.riljLog("No mEmbmsAtInfoNotificationRegistrant exist");
        }
    }

    @Override
    public void onSimTrayPlugIn(int i) {
        this.mRadioIndication.mRil.processIndication(i);
        if (ENG) {
            this.mRadioIndication.mRil.unsljLog(3067);
        }
        MtkRIL mtkRIL = (MtkRIL) this.mRadioIndication.mRil;
        if (mtkRIL.mSimTrayPlugIn != null) {
            mtkRIL.mSimTrayPlugIn.notifyRegistrants(new AsyncResult((Object) null, (Object) null, (Throwable) null));
        }
    }

    @Override
    public void onSimCommonSlotNoChanged(int i) {
        this.mRadioIndication.mRil.processIndication(i);
        if (ENG) {
            this.mRadioIndication.mRil.unsljLog(3068);
        }
        MtkRIL mtkRIL = (MtkRIL) this.mRadioIndication.mRil;
        if (mtkRIL.mSimCommonSlotNoChanged != null) {
            mtkRIL.mSimCommonSlotNoChanged.notifyRegistrants(new AsyncResult((Object) null, (Object) null, (Throwable) null));
        }
    }

    @Override
    public void plmnChangedIndication(int i, ArrayList<String> arrayList) {
        this.mMtkRil.processIndication(i);
        String[] strArr = new String[arrayList.size()];
        for (int i2 = 0; i2 < arrayList.size(); i2++) {
            strArr[i2] = arrayList.get(i2);
        }
        this.mMtkRil.unsljLogRet(3000, strArr);
        synchronized (this.mMtkRil.mWPMonitor) {
            if (this.mMtkRil.mPlmnChangeNotificationRegistrant.size() > 0) {
                this.mMtkRil.riljLog("ECOPS,notify mPlmnChangeNotificationRegistrant");
                this.mMtkRil.mPlmnChangeNotificationRegistrant.notifyRegistrants(new AsyncResult((Object) null, strArr, (Throwable) null));
            } else {
                this.mMtkRil.mEcopsReturnValue = strArr;
            }
        }
    }

    @Override
    public void registrationSuspendedIndication(int i, ArrayList<Integer> arrayList) {
        this.mMtkRil.processIndication(i);
        int[] iArr = new int[arrayList.size()];
        for (int i2 = 0; i2 < arrayList.size(); i2++) {
            iArr[i2] = arrayList.get(i2).intValue();
        }
        this.mMtkRil.unsljLogRet(IMtkConcatenatedSmsFwk.EVENT_DISPATCH_CONCATE_SMS_SEGMENTS, iArr);
        synchronized (this.mMtkRil.mWPMonitor) {
            if (this.mMtkRil.mRegistrationSuspendedRegistrant != null) {
                this.mMtkRil.riljLog("EMSR, notify mRegistrationSuspendedRegistrant");
                this.mMtkRil.mRegistrationSuspendedRegistrant.notifyRegistrant(new AsyncResult((Object) null, iArr, (Throwable) null));
            } else {
                this.mMtkRil.mEmsrReturnValue = iArr;
            }
        }
    }

    @Override
    public void gmssRatChangedIndication(int i, ArrayList<Integer> arrayList) {
        this.mMtkRil.processIndication(i);
        int[] iArr = new int[arrayList.size()];
        for (int i2 = 0; i2 < arrayList.size(); i2++) {
            iArr[i2] = arrayList.get(i2).intValue();
        }
        this.mMtkRil.unsljLogRet(3003, iArr);
        int[] iArr2 = iArr;
        if (this.mMtkRil.mGmssRatChangedRegistrant != null) {
            this.mMtkRil.mGmssRatChangedRegistrant.notifyRegistrants(new AsyncResult((Object) null, iArr2, (Throwable) null));
        }
    }

    @Override
    public void worldModeChangedIndication(int i, ArrayList<Integer> arrayList) {
        boolean zUpdateSwitchingState;
        this.mMtkRil.processIndication(i);
        int[] iArr = new int[arrayList.size()];
        for (int i2 = 0; i2 < arrayList.size(); i2++) {
            iArr[i2] = arrayList.get(i2).intValue();
        }
        this.mMtkRil.unsljLogRet(3022, iArr);
        int i3 = iArr[0];
        if (i3 == 2) {
            zUpdateSwitchingState = WorldMode.resetSwitchingState(i3);
            i3 = 1;
        } else if (i3 == 0) {
            zUpdateSwitchingState = WorldMode.updateSwitchingState(true);
        } else {
            zUpdateSwitchingState = WorldMode.updateSwitchingState(false);
        }
        if (!zUpdateSwitchingState) {
            return;
        }
        Intent intent = new Intent(WorldMode.ACTION_WORLD_MODE_CHANGED);
        intent.putExtra(WorldMode.EXTRA_WORLD_MODE_CHANGE_STATE, Integer.valueOf(i3));
        this.mMtkRil.mMtkContext.sendBroadcast(intent);
        this.mMtkRil.riljLog("Broadcast for WorldModeChanged: state=" + i3);
    }

    @Override
    public void resetAttachApnInd(int i) {
        this.mMtkRil.processIndication(i);
        if (ENG) {
            this.mRadioIndication.mRil.unsljLog(3020);
        }
        MtkRIL mtkRIL = (MtkRIL) this.mRadioIndication.mRil;
        if (mtkRIL.mResetAttachApnRegistrants != null) {
            mtkRIL.mResetAttachApnRegistrants.notifyRegistrants(new AsyncResult((Object) null, (Object) null, (Throwable) null));
        }
    }

    @Override
    public void mdChangedApnInd(int i, int i2) {
        this.mMtkRil.processIndication(i);
        if (ENG) {
            this.mRadioIndication.mRil.unsljLog(3021);
        }
        MtkRIL mtkRIL = (MtkRIL) this.mRadioIndication.mRil;
        if (mtkRIL.mAttachApnChangedRegistrants != null) {
            mtkRIL.mAttachApnChangedRegistrants.notifyRegistrants(new AsyncResult((Object) null, Integer.valueOf(i2), (Throwable) null));
        }
    }

    @Override
    public void esnMeidChangeInd(int i, String str) {
        this.mRadioIndication.mRil.processIndication(i);
        if (ENG) {
            this.mRadioIndication.mRil.unsljLog(3023);
        }
        MtkRIL mtkRIL = (MtkRIL) this.mRadioIndication.mRil;
        if (mtkRIL.mCDMACardEsnMeidRegistrant != null) {
            mtkRIL.mCDMACardEsnMeidRegistrant.notifyRegistrant(new AsyncResult((Object) null, str, (Throwable) null));
            return;
        }
        if (ENG) {
            this.mRadioIndication.mRil.riljLog("Cache esnMeidChangeInd");
        }
        mtkRIL.mEspOrMeid = str;
    }

    @Override
    public void phbReadyNotification(int i, int i2) {
        this.mRadioIndication.mRil.processIndication(i);
        int[] iArr = {i2};
        this.mRadioIndication.mRil.unsljLogMore(3028, "phbReadyNotification: " + i2);
        if (((MtkRIL) this.mRadioIndication.mRil).mPhbReadyRegistrants != null) {
            ((MtkRIL) this.mRadioIndication.mRil).mPhbReadyRegistrants.notifyRegistrants(new AsyncResult((Object) null, iArr, (Throwable) null));
        }
    }

    @Override
    public void bipProactiveCommand(int i, String str) {
        this.mMtkRil.processIndication(i);
        if (ENG) {
            this.mRadioIndication.mRil.unsljLog(3057);
        }
        MtkRIL mtkRIL = (MtkRIL) this.mRadioIndication.mRil;
        if (mtkRIL.mBipProCmdRegistrant != null) {
            mtkRIL.mBipProCmdRegistrant.notifyRegistrants(new AsyncResult((Object) null, str, (Throwable) null));
        }
    }

    @Override
    public void triggerOtaSP(int i) {
        this.mMtkRil.invokeOemRilRequestStrings(new String[]{"AT+CDV=*22899", "", "DESTRILD:C2K"}, null);
    }

    @Override
    public void pcoDataAfterAttached(int i, PcoDataAttachedInfo pcoDataAttachedInfo) {
        this.mMtkRil.processIndication(i);
        PcoDataAfterAttached pcoDataAfterAttached = new PcoDataAfterAttached(pcoDataAttachedInfo.cid, pcoDataAttachedInfo.apnName, pcoDataAttachedInfo.bearerProto, pcoDataAttachedInfo.pcoId, RIL.arrayListToPrimitiveArray(pcoDataAttachedInfo.contents));
        this.mMtkRil.unsljLogRet(3053, pcoDataAfterAttached);
        this.mMtkRil.mPcoDataAfterAttachedRegistrants.notifyRegistrants(new AsyncResult((Object) null, pcoDataAfterAttached, (Throwable) null));
    }

    @Override
    public void onStkMenuReset(int i) {
        this.mMtkRil.processIndication(i);
        if (ENG) {
            this.mRadioIndication.mRil.unsljLog(3071);
        }
        MtkRIL mtkRIL = (MtkRIL) this.mRadioIndication.mRil;
        if (mtkRIL.mStkSetupMenuResetRegistrant != null) {
            mtkRIL.mStkSetupMenuResetRegistrant.notifyRegistrants(new AsyncResult((Object) null, (Object) null, (Throwable) null));
        }
    }

    @Override
    public void onLteAccessStratumStateChanged(int i, ArrayList<Integer> arrayList) {
        this.mMtkRil.processIndication(i);
        int[] iArr = new int[arrayList.size()];
        for (int i2 = 0; i2 < arrayList.size(); i2++) {
            iArr[i2] = arrayList.get(i2).intValue();
        }
        this.mMtkRil.unsljLogRet(3062, iArr);
        if (this.mMtkRil.mLteAccessStratumStateRegistrants != null) {
            this.mMtkRil.mLteAccessStratumStateRegistrants.notifyRegistrants(new AsyncResult((Object) null, iArr, (Throwable) null));
        }
    }

    @Override
    public void networkInfoInd(int i, ArrayList<String> arrayList) {
        this.mRadioIndication.mRil.processIndication(i);
        this.mRadioIndication.mRil.unsljLogMore(3030, "networkInfo: " + arrayList);
        String[] strArr = (String[]) arrayList.toArray(new String[arrayList.size()]);
        MtkRIL mtkRIL = (MtkRIL) this.mRadioIndication.mRil;
        if (mtkRIL.mNetworkInfoRegistrant.size() != 0) {
            mtkRIL.mNetworkInfoRegistrant.notifyRegistrants(new AsyncResult((Object) null, strArr, (Throwable) null));
        }
    }

    @Override
    public void onMdDataRetryCountReset(int i) {
        this.mRadioIndication.mRil.processIndication(i);
        if (ENG) {
            this.mRadioIndication.mRil.unsljLog(3059);
        }
        MtkRIL mtkRIL = (MtkRIL) this.mRadioIndication.mRil;
        if (mtkRIL.mMdDataRetryCountResetRegistrants != null) {
            mtkRIL.mMdDataRetryCountResetRegistrants.notifyRegistrants(new AsyncResult((Object) null, (Object) null, (Throwable) null));
        }
    }

    @Override
    public void onRemoveRestrictEutran(int i) {
        this.mRadioIndication.mRil.processIndication(i);
        if (ENG) {
            this.mRadioIndication.mRil.unsljLog(3060);
        }
        MtkRIL mtkRIL = (MtkRIL) this.mRadioIndication.mRil;
        if (mtkRIL.mRemoveRestrictEutranRegistrants != null) {
            mtkRIL.mRemoveRestrictEutranRegistrants.notifyRegistrants(new AsyncResult((Object) null, (Object) null, (Throwable) null));
        }
    }

    @Override
    public void confSRVCC(int i, ArrayList<Integer> arrayList) {
        this.mMtkRil.processIndication(i);
        this.mMtkRil.unsljLog(3072);
        int[] iArr = new int[arrayList.size()];
        for (int i2 = 0; i2 < arrayList.size(); i2++) {
            iArr[i2] = arrayList.get(i2).intValue();
        }
        this.mMtkRil.mEconfSrvccRegistrants.notifyRegistrants(new AsyncResult((Object) null, iArr, (Throwable) null));
    }

    @Override
    public void volteLteConnectionStatus(int i, ArrayList<Integer> arrayList) {
        this.mMtkRil.processIndication(i);
        this.mMtkRil.unsljLog(3073);
        int[] iArr = new int[arrayList.size()];
        for (int i2 = 0; i2 < arrayList.size(); i2++) {
            iArr[i2] = arrayList.get(i2).intValue();
        }
        if (iArr.length > 2) {
            this.mMtkRil.riljLog("LTE_CONNECTION_STATUS - status: " + iArr[0] + ", reason: " + iArr[1]);
            if ((iArr[0] > 10 && iArr[0] < 20) || (iArr[0] > 30 && iArr[0] < 40)) {
                Intent intent = new Intent("com.lge.ims.action.LTE_CONNECTION_STATUS");
                intent.putExtra("status", iArr);
                intent.addFlags(32);
                this.mMtkRil.mMtkContext.sendBroadcastAsUser(intent, UserHandle.ALL);
            }
        }
    }

    @Override
    public void onVsimEventIndication(int i, VsimOperationEvent vsimOperationEvent) {
        this.mMtkRil.processIndication(i);
        int i2 = vsimOperationEvent.dataLength > 0 ? (vsimOperationEvent.dataLength / 2) + 4 : 0;
        ExternalSimManager.VsimEvent vsimEvent = new ExternalSimManager.VsimEvent(vsimOperationEvent.transactionId, vsimOperationEvent.eventId, i2, 1 << this.mMtkRil.mInstanceId.intValue());
        if (i2 > 0) {
            vsimEvent.putInt(vsimOperationEvent.dataLength / 2);
            vsimEvent.putBytes(IccUtils.hexStringToBytes(vsimOperationEvent.data));
        }
        if (ENG) {
            this.mRadioIndication.mRil.unsljLogRet(3074, vsimEvent.toString());
        }
        if (this.mMtkRil.mVsimIndicationRegistrants != null) {
            this.mMtkRil.mVsimIndicationRegistrants.notifyRegistrants(new AsyncResult((Object) null, vsimEvent, (Throwable) null));
        }
    }

    @Override
    public void dedicatedBearerActivationInd(int i, DedicateDataCall dedicateDataCall) {
        this.mRadioIndication.mRil.processIndication(i);
        this.mMtkRil.unsljLog(3082);
        MtkDedicateDataCallResponse mtkDedicateDataCallResponseConvertDedicatedDataCallResult = this.mMtkRil.convertDedicatedDataCallResult(dedicateDataCall);
        this.mMtkRil.riljLog(mtkDedicateDataCallResponseConvertDedicatedDataCallResult.toString());
        if (this.mMtkRil.mDedicatedBearerActivedRegistrants != null) {
            this.mMtkRil.mDedicatedBearerActivedRegistrants.notifyRegistrants(new AsyncResult((Object) null, mtkDedicateDataCallResponseConvertDedicatedDataCallResult, (Throwable) null));
        }
    }

    @Override
    public void dedicatedBearerModificationInd(int i, DedicateDataCall dedicateDataCall) {
        this.mRadioIndication.mRil.processIndication(i);
        this.mMtkRil.unsljLog(3083);
        MtkDedicateDataCallResponse mtkDedicateDataCallResponseConvertDedicatedDataCallResult = this.mMtkRil.convertDedicatedDataCallResult(dedicateDataCall);
        this.mMtkRil.riljLog(mtkDedicateDataCallResponseConvertDedicatedDataCallResult.toString());
        if (this.mMtkRil.mDedicatedBearerModifiedRegistrants != null) {
            this.mMtkRil.mDedicatedBearerModifiedRegistrants.notifyRegistrants(new AsyncResult((Object) null, mtkDedicateDataCallResponseConvertDedicatedDataCallResult, (Throwable) null));
        }
    }

    @Override
    public void dedicatedBearerDeactivationInd(int i, int i2) {
        this.mRadioIndication.mRil.processIndication(i);
        this.mMtkRil.unsljLog(3084);
        this.mMtkRil.riljLog("dedicatedBearerDeactivationInd, cid: " + i2);
        if (this.mMtkRil.mDedicatedBearerDeactivatedRegistrants != null) {
            this.mMtkRil.mDedicatedBearerDeactivatedRegistrants.notifyRegistrants(new AsyncResult((Object) null, Integer.valueOf(i2), (Throwable) null));
        }
    }

    @Override
    public void oemHookRaw(int i, ArrayList<Byte> arrayList) {
        this.mRadioIndication.mRil.processIndication(i);
        byte[] bArrArrayListToPrimitiveArray = RIL.arrayListToPrimitiveArray(arrayList);
        MtkRIL mtkRIL = (MtkRIL) this.mRadioIndication.mRil;
        mtkRIL.unsljLogvRet(1028, IccUtils.bytesToHexString(bArrArrayListToPrimitiveArray));
        if (mtkRIL.mUnsolOemHookRegistrant != null) {
            mtkRIL.mUnsolOemHookRegistrant.notifyRegistrant(new AsyncResult((Object) null, bArrArrayListToPrimitiveArray, (Throwable) null));
        }
    }

    @Override
    public void onTxPowerIndication(int i, ArrayList<Integer> arrayList) {
        this.mMtkRil.processIndication(i);
        int[] iArr = new int[arrayList.size()];
        for (int i2 = 0; i2 < arrayList.size(); i2++) {
            iArr[i2] = arrayList.get(i2).intValue();
        }
        if (((MtkRIL) this.mRadioIndication.mRil).mTxPowerRegistrant != null) {
            ((MtkRIL) this.mRadioIndication.mRil).mTxPowerRegistrant.notifyRegistrants(new AsyncResult((Object) null, iArr, (Throwable) null));
        }
    }

    @Override
    public void onTxPowerStatusIndication(int i, ArrayList<Integer> arrayList) {
        this.mMtkRil.processIndication(i);
        int[] iArr = new int[arrayList.size()];
        for (int i2 = 0; i2 < arrayList.size(); i2++) {
            iArr[i2] = arrayList.get(i2).intValue();
        }
        if (((MtkRIL) this.mRadioIndication.mRil).mTxPowerStatusRegistrant != null) {
            ((MtkRIL) this.mRadioIndication.mRil).mTxPowerStatusRegistrant.notifyRegistrants(new AsyncResult((Object) null, iArr, (Throwable) null));
        }
    }

    @Override
    public void dsbpStateChanged(int i, int i2) {
        this.mRadioIndication.mRil.processIndication(i);
        if (ENG) {
            this.mRadioIndication.mRil.unsljLog(3114);
        }
        MtkRIL mtkRIL = (MtkRIL) this.mRadioIndication.mRil;
        this.mMtkRil.riljLog("dsbpStateChanged state: " + i2);
        if (mtkRIL.mDsbpStateRegistrant != null) {
            mtkRIL.mDsbpStateRegistrant.notifyRegistrants(new AsyncResult((Object) null, Integer.valueOf(i2), (Throwable) null));
        }
    }
}
