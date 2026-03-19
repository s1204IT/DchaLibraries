package vendor.mediatek.hardware.radio.V3_0;

import android.hardware.radio.V1_0.ActivityStatsInfo;
import android.hardware.radio.V1_0.Call;
import android.hardware.radio.V1_0.CallForwardInfo;
import android.hardware.radio.V1_0.CardStatus;
import android.hardware.radio.V1_0.CarrierRestrictions;
import android.hardware.radio.V1_0.CdmaBroadcastSmsConfigInfo;
import android.hardware.radio.V1_0.CellInfo;
import android.hardware.radio.V1_0.DataCallFailCause;
import android.hardware.radio.V1_0.DataRegStateResult;
import android.hardware.radio.V1_0.GsmBroadcastSmsConfigInfo;
import android.hardware.radio.V1_0.HardwareConfig;
import android.hardware.radio.V1_0.IccIoResult;
import android.hardware.radio.V1_0.LastCallFailCauseInfo;
import android.hardware.radio.V1_0.LceDataInfo;
import android.hardware.radio.V1_0.LceStatusInfo;
import android.hardware.radio.V1_0.NeighboringCell;
import android.hardware.radio.V1_0.NvItem;
import android.hardware.radio.V1_0.OperatorInfo;
import android.hardware.radio.V1_0.RadioCapability;
import android.hardware.radio.V1_0.RadioCdmaSmsConst;
import android.hardware.radio.V1_0.RadioResponseInfo;
import android.hardware.radio.V1_0.SendSmsResult;
import android.hardware.radio.V1_0.SetupDataCallResult;
import android.hardware.radio.V1_0.SignalStrength;
import android.hardware.radio.V1_0.VoiceRegStateResult;
import android.hardware.radio.V1_1.KeepaliveStatus;
import android.hidl.base.V1_0.DebugInfo;
import android.hidl.base.V1_0.IBase;
import android.os.HidlSupport;
import android.os.HwBinder;
import android.os.HwBlob;
import android.os.HwParcel;
import android.os.IHwBinder;
import android.os.IHwInterface;
import android.os.RemoteException;
import com.mediatek.android.mms.pdu.MtkCharacterSets;
import com.mediatek.android.mms.pdu.MtkPduHeaders;
import com.mediatek.android.mms.pdu.MtkPduPart;
import com.mediatek.internal.telephony.ppl.PplMessageManager;
import com.mediatek.internal.telephony.worldphone.IWorldPhone;
import com.mediatek.telephony.internal.telephony.vsim.ExternalSimConstants;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;

public interface IImsRadioResponse extends android.hardware.radio.V1_2.IRadioResponse {
    public static final String kInterfaceName = "vendor.mediatek.hardware.radio@3.0::IImsRadioResponse";

    void acknowledgeLastIncomingCdmaSmsExResponse(RadioResponseInfo radioResponseInfo) throws RemoteException;

    void acknowledgeLastIncomingGsmSmsExResponse(RadioResponseInfo radioResponseInfo) throws RemoteException;

    void addImsConferenceCallMemberResponse(RadioResponseInfo radioResponseInfo) throws RemoteException;

    @Override
    IHwBinder asBinder();

    void cancelUssiResponse(RadioResponseInfo radioResponseInfo) throws RemoteException;

    void conferenceDialResponse(RadioResponseInfo radioResponseInfo) throws RemoteException;

    void dialWithSipUriResponse(RadioResponseInfo radioResponseInfo) throws RemoteException;

    void emergencyDialResponse(RadioResponseInfo radioResponseInfo) throws RemoteException;

    void forceReleaseCallResponse(RadioResponseInfo radioResponseInfo) throws RemoteException;

    void getColpResponse(RadioResponseInfo radioResponseInfo, int i, int i2) throws RemoteException;

    void getColrResponse(RadioResponseInfo radioResponseInfo, int i) throws RemoteException;

    @Override
    DebugInfo getDebugInfo() throws RemoteException;

    @Override
    ArrayList<byte[]> getHashChain() throws RemoteException;

    void getImsCfgFeatureValueResponse(RadioResponseInfo radioResponseInfo, int i) throws RemoteException;

    void getImsCfgProvisionValueResponse(RadioResponseInfo radioResponseInfo, String str) throws RemoteException;

    void getImsCfgResourceCapValueResponse(RadioResponseInfo radioResponseInfo, int i) throws RemoteException;

    void getProvisionValueResponse(RadioResponseInfo radioResponseInfo) throws RemoteException;

    void getXcapStatusResponse(RadioResponseInfo radioResponseInfo) throws RemoteException;

    void hangupAllResponse(RadioResponseInfo radioResponseInfo) throws RemoteException;

    void holdCallResponse(RadioResponseInfo radioResponseInfo) throws RemoteException;

    void imsBearerActivationDoneResponse(RadioResponseInfo radioResponseInfo) throws RemoteException;

    void imsBearerDeactivationDoneResponse(RadioResponseInfo radioResponseInfo) throws RemoteException;

    void imsDeregNotificationResponse(RadioResponseInfo radioResponseInfo) throws RemoteException;

    void imsEctCommandResponse(RadioResponseInfo radioResponseInfo) throws RemoteException;

    @Override
    ArrayList<String> interfaceChain() throws RemoteException;

    @Override
    String interfaceDescriptor() throws RemoteException;

    @Override
    boolean linkToDeath(IHwBinder.DeathRecipient deathRecipient, long j) throws RemoteException;

    @Override
    void notifySyspropsChanged() throws RemoteException;

    @Override
    void ping() throws RemoteException;

    void pullCallResponse(RadioResponseInfo radioResponseInfo) throws RemoteException;

    void queryCallForwardInTimeSlotStatusResponse(RadioResponseInfo radioResponseInfo, ArrayList<CallForwardInfoEx> arrayList) throws RemoteException;

    void removeImsConferenceCallMemberResponse(RadioResponseInfo radioResponseInfo) throws RemoteException;

    void resetSuppServResponse(RadioResponseInfo radioResponseInfo) throws RemoteException;

    void resumeCallResponse(RadioResponseInfo radioResponseInfo) throws RemoteException;

    void runGbaAuthenticationResponse(RadioResponseInfo radioResponseInfo, ArrayList<String> arrayList) throws RemoteException;

    void sendImsSmsExResponse(RadioResponseInfo radioResponseInfo, SendSmsResult sendSmsResult) throws RemoteException;

    void sendUssiResponse(RadioResponseInfo radioResponseInfo) throws RemoteException;

    void setCallForwardInTimeSlotResponse(RadioResponseInfo radioResponseInfo) throws RemoteException;

    void setCallIndicationResponse(RadioResponseInfo radioResponseInfo) throws RemoteException;

    void setClipResponse(RadioResponseInfo radioResponseInfo) throws RemoteException;

    void setColpResponse(RadioResponseInfo radioResponseInfo) throws RemoteException;

    void setColrResponse(RadioResponseInfo radioResponseInfo) throws RemoteException;

    void setEccServiceCategoryResponse(RadioResponseInfo radioResponseInfo) throws RemoteException;

    @Override
    void setHALInstrumentation() throws RemoteException;

    void setImsBearerNotificationResponse(RadioResponseInfo radioResponseInfo) throws RemoteException;

    void setImsCallStatusResponse(RadioResponseInfo radioResponseInfo) throws RemoteException;

    void setImsCfgFeatureValueResponse(RadioResponseInfo radioResponseInfo) throws RemoteException;

    void setImsCfgProvisionValueResponse(RadioResponseInfo radioResponseInfo) throws RemoteException;

    void setImsCfgResourceCapValueResponse(RadioResponseInfo radioResponseInfo) throws RemoteException;

    void setImsEnableResponse(RadioResponseInfo radioResponseInfo) throws RemoteException;

    void setImsRegistrationReportResponse(RadioResponseInfo radioResponseInfo) throws RemoteException;

    void setImsRtpReportResponse(RadioResponseInfo radioResponseInfo) throws RemoteException;

    void setImsVideoEnableResponse(RadioResponseInfo radioResponseInfo) throws RemoteException;

    void setImsVoiceEnableResponse(RadioResponseInfo radioResponseInfo) throws RemoteException;

    void setImscfgResponse(RadioResponseInfo radioResponseInfo) throws RemoteException;

    void setModemImsCfgResponse(RadioResponseInfo radioResponseInfo, String str) throws RemoteException;

    void setProvisionValueResponse(RadioResponseInfo radioResponseInfo) throws RemoteException;

    void setRcsUaEnableResponse(RadioResponseInfo radioResponseInfo) throws RemoteException;

    void setViWifiEnableResponse(RadioResponseInfo radioResponseInfo) throws RemoteException;

    void setVilteEnableResponse(RadioResponseInfo radioResponseInfo) throws RemoteException;

    void setVoiceDomainPreferenceResponse(RadioResponseInfo radioResponseInfo) throws RemoteException;

    void setVolteEnableResponse(RadioResponseInfo radioResponseInfo) throws RemoteException;

    void setWfcEnableResponse(RadioResponseInfo radioResponseInfo) throws RemoteException;

    void setWfcProfileResponse(RadioResponseInfo radioResponseInfo) throws RemoteException;

    void setupXcapUserAgentStringResponse(RadioResponseInfo radioResponseInfo) throws RemoteException;

    @Override
    boolean unlinkToDeath(IHwBinder.DeathRecipient deathRecipient) throws RemoteException;

    void updateImsRegistrationStatusResponse(RadioResponseInfo radioResponseInfo) throws RemoteException;

    void videoCallAcceptResponse(RadioResponseInfo radioResponseInfo) throws RemoteException;

    void vtDialResponse(RadioResponseInfo radioResponseInfo) throws RemoteException;

    void vtDialWithSipUriResponse(RadioResponseInfo radioResponseInfo) throws RemoteException;

    static IImsRadioResponse asInterface(IHwBinder iHwBinder) {
        if (iHwBinder == null) {
            return null;
        }
        IHwInterface iHwInterfaceQueryLocalInterface = iHwBinder.queryLocalInterface(kInterfaceName);
        if (iHwInterfaceQueryLocalInterface != null && (iHwInterfaceQueryLocalInterface instanceof IImsRadioResponse)) {
            return (IImsRadioResponse) iHwInterfaceQueryLocalInterface;
        }
        Proxy proxy = new Proxy(iHwBinder);
        try {
            Iterator<String> it = proxy.interfaceChain().iterator();
            while (it.hasNext()) {
                if (it.next().equals(kInterfaceName)) {
                    return proxy;
                }
            }
        } catch (RemoteException e) {
        }
        return null;
    }

    static IImsRadioResponse castFrom(IHwInterface iHwInterface) {
        if (iHwInterface == null) {
            return null;
        }
        return asInterface(iHwInterface.asBinder());
    }

    static IImsRadioResponse getService(String str, boolean z) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, str, z));
    }

    static IImsRadioResponse getService(boolean z) throws RemoteException {
        return getService("default", z);
    }

    static IImsRadioResponse getService(String str) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, str));
    }

    static IImsRadioResponse getService() throws RemoteException {
        return getService("default");
    }

    public static final class Proxy implements IImsRadioResponse {
        private IHwBinder mRemote;

        public Proxy(IHwBinder iHwBinder) {
            this.mRemote = (IHwBinder) Objects.requireNonNull(iHwBinder);
        }

        @Override
        public IHwBinder asBinder() {
            return this.mRemote;
        }

        public String toString() {
            try {
                return interfaceDescriptor() + "@Proxy";
            } catch (RemoteException e) {
                return "[class or subclass of vendor.mediatek.hardware.radio@3.0::IImsRadioResponse]@Proxy";
            }
        }

        public final boolean equals(Object obj) {
            return HidlSupport.interfacesEqual(this, obj);
        }

        public final int hashCode() {
            return asBinder().hashCode();
        }

        @Override
        public void getIccCardStatusResponse(RadioResponseInfo radioResponseInfo, CardStatus cardStatus) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            cardStatus.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(1, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void supplyIccPinForAppResponse(RadioResponseInfo radioResponseInfo, int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(2, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void supplyIccPukForAppResponse(RadioResponseInfo radioResponseInfo, int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(3, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void supplyIccPin2ForAppResponse(RadioResponseInfo radioResponseInfo, int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(4, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void supplyIccPuk2ForAppResponse(RadioResponseInfo radioResponseInfo, int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(5, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void changeIccPinForAppResponse(RadioResponseInfo radioResponseInfo, int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(6, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void changeIccPin2ForAppResponse(RadioResponseInfo radioResponseInfo, int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(7, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void supplyNetworkDepersonalizationResponse(RadioResponseInfo radioResponseInfo, int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(8, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getCurrentCallsResponse(RadioResponseInfo radioResponseInfo, ArrayList<Call> arrayList) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            Call.writeVectorToParcel(hwParcel, arrayList);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(9, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void dialResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(10, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getIMSIForAppResponse(RadioResponseInfo radioResponseInfo, String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(11, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void hangupConnectionResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(12, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void hangupWaitingOrBackgroundResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(13, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void hangupForegroundResumeBackgroundResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(14, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void switchWaitingOrHoldingAndActiveResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(15, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void conferenceResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(16, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void rejectCallResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(17, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getLastCallFailCauseResponse(RadioResponseInfo radioResponseInfo, LastCallFailCauseInfo lastCallFailCauseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            lastCallFailCauseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(18, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getSignalStrengthResponse(RadioResponseInfo radioResponseInfo, SignalStrength signalStrength) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            signalStrength.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(19, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getVoiceRegistrationStateResponse(RadioResponseInfo radioResponseInfo, VoiceRegStateResult voiceRegStateResult) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            voiceRegStateResult.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(20, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getDataRegistrationStateResponse(RadioResponseInfo radioResponseInfo, DataRegStateResult dataRegStateResult) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            dataRegStateResult.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(21, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getOperatorResponse(RadioResponseInfo radioResponseInfo, String str, String str2, String str3) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            hwParcel.writeString(str);
            hwParcel.writeString(str2);
            hwParcel.writeString(str3);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(22, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setRadioPowerResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(23, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void sendDtmfResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(24, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void sendSmsResponse(RadioResponseInfo radioResponseInfo, SendSmsResult sendSmsResult) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            sendSmsResult.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(25, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void sendSMSExpectMoreResponse(RadioResponseInfo radioResponseInfo, SendSmsResult sendSmsResult) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            sendSmsResult.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(26, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setupDataCallResponse(RadioResponseInfo radioResponseInfo, SetupDataCallResult setupDataCallResult) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            setupDataCallResult.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(27, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void iccIOForAppResponse(RadioResponseInfo radioResponseInfo, IccIoResult iccIoResult) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            iccIoResult.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(28, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void sendUssdResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(29, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void cancelPendingUssdResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(30, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getClirResponse(RadioResponseInfo radioResponseInfo, int i, int i2) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            hwParcel.writeInt32(i);
            hwParcel.writeInt32(i2);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(31, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setClirResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(32, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getCallForwardStatusResponse(RadioResponseInfo radioResponseInfo, ArrayList<CallForwardInfo> arrayList) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            CallForwardInfo.writeVectorToParcel(hwParcel, arrayList);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(33, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setCallForwardResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(34, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getCallWaitingResponse(RadioResponseInfo radioResponseInfo, boolean z, int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            hwParcel.writeBool(z);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(35, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setCallWaitingResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(36, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void acknowledgeLastIncomingGsmSmsResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(37, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void acceptCallResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(38, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void deactivateDataCallResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(39, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getFacilityLockForAppResponse(RadioResponseInfo radioResponseInfo, int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(40, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setFacilityLockForAppResponse(RadioResponseInfo radioResponseInfo, int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(41, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setBarringPasswordResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(42, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getNetworkSelectionModeResponse(RadioResponseInfo radioResponseInfo, boolean z) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            hwParcel.writeBool(z);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(43, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setNetworkSelectionModeAutomaticResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(44, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setNetworkSelectionModeManualResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(45, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getAvailableNetworksResponse(RadioResponseInfo radioResponseInfo, ArrayList<OperatorInfo> arrayList) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            OperatorInfo.writeVectorToParcel(hwParcel, arrayList);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(46, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void startDtmfResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(47, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void stopDtmfResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(48, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getBasebandVersionResponse(RadioResponseInfo radioResponseInfo, String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(49, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void separateConnectionResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(50, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setMuteResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(51, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getMuteResponse(RadioResponseInfo radioResponseInfo, boolean z) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            hwParcel.writeBool(z);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(52, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getClipResponse(RadioResponseInfo radioResponseInfo, int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(53, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getDataCallListResponse(RadioResponseInfo radioResponseInfo, ArrayList<SetupDataCallResult> arrayList) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            SetupDataCallResult.writeVectorToParcel(hwParcel, arrayList);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(54, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setSuppServiceNotificationsResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(55, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void writeSmsToSimResponse(RadioResponseInfo radioResponseInfo, int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(56, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void deleteSmsOnSimResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(57, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setBandModeResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(58, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getAvailableBandModesResponse(RadioResponseInfo radioResponseInfo, ArrayList<Integer> arrayList) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            hwParcel.writeInt32Vector(arrayList);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(59, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void sendEnvelopeResponse(RadioResponseInfo radioResponseInfo, String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(60, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void sendTerminalResponseToSimResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(61, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void handleStkCallSetupRequestFromSimResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(62, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void explicitCallTransferResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(63, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setPreferredNetworkTypeResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(64, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getPreferredNetworkTypeResponse(RadioResponseInfo radioResponseInfo, int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(65, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getNeighboringCidsResponse(RadioResponseInfo radioResponseInfo, ArrayList<NeighboringCell> arrayList) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            NeighboringCell.writeVectorToParcel(hwParcel, arrayList);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(66, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setLocationUpdatesResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(67, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setCdmaSubscriptionSourceResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(68, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setCdmaRoamingPreferenceResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(69, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getCdmaRoamingPreferenceResponse(RadioResponseInfo radioResponseInfo, int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(70, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setTTYModeResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(71, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getTTYModeResponse(RadioResponseInfo radioResponseInfo, int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(72, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setPreferredVoicePrivacyResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(73, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getPreferredVoicePrivacyResponse(RadioResponseInfo radioResponseInfo, boolean z) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            hwParcel.writeBool(z);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(74, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void sendCDMAFeatureCodeResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(75, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void sendBurstDtmfResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(76, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void sendCdmaSmsResponse(RadioResponseInfo radioResponseInfo, SendSmsResult sendSmsResult) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            sendSmsResult.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(77, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void acknowledgeLastIncomingCdmaSmsResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(78, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getGsmBroadcastConfigResponse(RadioResponseInfo radioResponseInfo, ArrayList<GsmBroadcastSmsConfigInfo> arrayList) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            GsmBroadcastSmsConfigInfo.writeVectorToParcel(hwParcel, arrayList);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(79, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setGsmBroadcastConfigResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(80, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setGsmBroadcastActivationResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(81, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getCdmaBroadcastConfigResponse(RadioResponseInfo radioResponseInfo, ArrayList<CdmaBroadcastSmsConfigInfo> arrayList) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            CdmaBroadcastSmsConfigInfo.writeVectorToParcel(hwParcel, arrayList);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(82, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setCdmaBroadcastConfigResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(83, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setCdmaBroadcastActivationResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(84, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getCDMASubscriptionResponse(RadioResponseInfo radioResponseInfo, String str, String str2, String str3, String str4, String str5) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            hwParcel.writeString(str);
            hwParcel.writeString(str2);
            hwParcel.writeString(str3);
            hwParcel.writeString(str4);
            hwParcel.writeString(str5);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(85, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void writeSmsToRuimResponse(RadioResponseInfo radioResponseInfo, int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(86, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void deleteSmsOnRuimResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(87, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getDeviceIdentityResponse(RadioResponseInfo radioResponseInfo, String str, String str2, String str3, String str4) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            hwParcel.writeString(str);
            hwParcel.writeString(str2);
            hwParcel.writeString(str3);
            hwParcel.writeString(str4);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(88, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void exitEmergencyCallbackModeResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(89, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getSmscAddressResponse(RadioResponseInfo radioResponseInfo, String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(90, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setSmscAddressResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(91, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void reportSmsMemoryStatusResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(92, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void reportStkServiceIsRunningResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(93, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getCdmaSubscriptionSourceResponse(RadioResponseInfo radioResponseInfo, int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(94, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void requestIsimAuthenticationResponse(RadioResponseInfo radioResponseInfo, String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(95, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void acknowledgeIncomingGsmSmsWithPduResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(96, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void sendEnvelopeWithStatusResponse(RadioResponseInfo radioResponseInfo, IccIoResult iccIoResult) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            iccIoResult.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(97, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getVoiceRadioTechnologyResponse(RadioResponseInfo radioResponseInfo, int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(98, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getCellInfoListResponse(RadioResponseInfo radioResponseInfo, ArrayList<CellInfo> arrayList) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            CellInfo.writeVectorToParcel(hwParcel, arrayList);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(99, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setCellInfoListRateResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(100, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setInitialAttachApnResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(101, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getImsRegistrationStateResponse(RadioResponseInfo radioResponseInfo, boolean z, int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            hwParcel.writeBool(z);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(102, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void sendImsSmsResponse(RadioResponseInfo radioResponseInfo, SendSmsResult sendSmsResult) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            sendSmsResult.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(103, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void iccTransmitApduBasicChannelResponse(RadioResponseInfo radioResponseInfo, IccIoResult iccIoResult) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            iccIoResult.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(MtkCharacterSets.ISO_2022_CN, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void iccOpenLogicalChannelResponse(RadioResponseInfo radioResponseInfo, int i, ArrayList<Byte> arrayList) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            hwParcel.writeInt32(i);
            hwParcel.writeInt8Vector(arrayList);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(MtkCharacterSets.ISO_2022_CN_EXT, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void iccCloseLogicalChannelResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(106, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void iccTransmitApduLogicalChannelResponse(RadioResponseInfo radioResponseInfo, IccIoResult iccIoResult) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            iccIoResult.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(107, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void nvReadItemResponse(RadioResponseInfo radioResponseInfo, String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(108, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void nvWriteItemResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(109, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void nvWriteCdmaPrlResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(110, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void nvResetConfigResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(111, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setUiccSubscriptionResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(112, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setDataAllowedResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(113, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getHardwareConfigResponse(RadioResponseInfo radioResponseInfo, ArrayList<HardwareConfig> arrayList) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HardwareConfig.writeVectorToParcel(hwParcel, arrayList);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(114, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void requestIccSimAuthenticationResponse(RadioResponseInfo radioResponseInfo, IccIoResult iccIoResult) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            iccIoResult.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(DataCallFailCause.EMM_ACCESS_BARRED, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setDataProfileResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(DataCallFailCause.EMERGENCY_IFACE_ONLY, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void requestShutdownResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(DataCallFailCause.IFACE_MISMATCH, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getRadioCapabilityResponse(RadioResponseInfo radioResponseInfo, RadioCapability radioCapability) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            radioCapability.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(DataCallFailCause.COMPANION_IFACE_IN_USE, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setRadioCapabilityResponse(RadioResponseInfo radioResponseInfo, RadioCapability radioCapability) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            radioCapability.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(DataCallFailCause.IP_ADDRESS_MISMATCH, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void startLceServiceResponse(RadioResponseInfo radioResponseInfo, LceStatusInfo lceStatusInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            lceStatusInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(DataCallFailCause.IFACE_AND_POL_FAMILY_MISMATCH, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void stopLceServiceResponse(RadioResponseInfo radioResponseInfo, LceStatusInfo lceStatusInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            lceStatusInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(DataCallFailCause.EMM_ACCESS_BARRED_INFINITE_RETRY, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void pullLceDataResponse(RadioResponseInfo radioResponseInfo, LceDataInfo lceDataInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            lceDataInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(DataCallFailCause.AUTH_FAILURE_ON_EMERGENCY_CALL, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getModemActivityInfoResponse(RadioResponseInfo radioResponseInfo, ActivityStatsInfo activityStatsInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            activityStatsInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(123, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setAllowedCarriersResponse(RadioResponseInfo radioResponseInfo, int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(124, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getAllowedCarriersResponse(RadioResponseInfo radioResponseInfo, boolean z, CarrierRestrictions carrierRestrictions) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            hwParcel.writeBool(z);
            carrierRestrictions.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(125, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void sendDeviceStateResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(126, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setIndicationFilterResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(127, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setSimCardPowerResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(128, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void acknowledgeRequest(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(129, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setCarrierInfoForImsiEncryptionResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_1.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(130, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setSimCardPowerResponse_1_1(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_1.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(RadioCdmaSmsConst.UDH_EO_DATA_SEGMENT_MAX, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void startNetworkScanResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_1.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(132, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void stopNetworkScanResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_1.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(133, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void startKeepaliveResponse(RadioResponseInfo radioResponseInfo, KeepaliveStatus keepaliveStatus) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_1.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            keepaliveStatus.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(RadioCdmaSmsConst.UDH_VAR_PIC_SIZE, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void stopKeepaliveResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_1.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(135, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getCellInfoListResponse_1_2(RadioResponseInfo radioResponseInfo, ArrayList<android.hardware.radio.V1_2.CellInfo> arrayList) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_2.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            android.hardware.radio.V1_2.CellInfo.writeVectorToParcel(hwParcel, arrayList);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(136, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getIccCardStatusResponse_1_2(RadioResponseInfo radioResponseInfo, android.hardware.radio.V1_2.CardStatus cardStatus) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_2.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            cardStatus.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(MtkPduHeaders.STATE_SKIP_RETRYING, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setSignalStrengthReportingCriteriaResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_2.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(138, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setLinkCapacityReportingCriteriaResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_2.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(139, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getCurrentCallsResponse_1_2(RadioResponseInfo radioResponseInfo, ArrayList<android.hardware.radio.V1_2.Call> arrayList) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_2.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            android.hardware.radio.V1_2.Call.writeVectorToParcel(hwParcel, arrayList);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(140, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getSignalStrengthResponse_1_2(RadioResponseInfo radioResponseInfo, android.hardware.radio.V1_2.SignalStrength signalStrength) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_2.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            signalStrength.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(141, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getVoiceRegistrationStateResponse_1_2(RadioResponseInfo radioResponseInfo, android.hardware.radio.V1_2.VoiceRegStateResult voiceRegStateResult) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_2.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            voiceRegStateResult.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(142, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getDataRegistrationStateResponse_1_2(RadioResponseInfo radioResponseInfo, android.hardware.radio.V1_2.DataRegStateResult dataRegStateResult) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_2.IRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            dataRegStateResult.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(143, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void hangupAllResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(144, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setCallIndicationResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(145, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void emergencyDialResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(MtkPduPart.P_DATE, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setEccServiceCategoryResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(147, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void videoCallAcceptResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(148, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void imsEctCommandResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(149, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void holdCallResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(150, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void resumeCallResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(151, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void imsDeregNotificationResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(152, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setImsEnableResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(153, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setVolteEnableResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(154, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setWfcEnableResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(155, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setVilteEnableResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(156, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setViWifiEnableResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(157, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setRcsUaEnableResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(158, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setImsVoiceEnableResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(159, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setImsVideoEnableResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(160, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setImscfgResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(161, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getProvisionValueResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(162, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setProvisionValueResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(163, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void addImsConferenceCallMemberResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(164, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void removeImsConferenceCallMemberResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(165, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setWfcProfileResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(166, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void conferenceDialResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(MtkPduPart.P_TRANSFER_ENCODING, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void vtDialWithSipUriResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(168, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void vtDialResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(169, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void dialWithSipUriResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(170, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void sendUssiResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(171, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void cancelUssiResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(172, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getXcapStatusResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(173, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void resetSuppServResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(174, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setupXcapUserAgentStringResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(175, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void forceReleaseCallResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(176, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void imsBearerActivationDoneResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(177, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void imsBearerDeactivationDoneResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(178, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setImsCallStatusResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(179, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void updateImsRegistrationStatusResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(180, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setImsRtpReportResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(181, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void pullCallResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(182, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setImsRegistrationReportResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(183, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setVoiceDomainPreferenceResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(184, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setClipResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(185, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getColpResponse(RadioResponseInfo radioResponseInfo, int i, int i2) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            hwParcel.writeInt32(i);
            hwParcel.writeInt32(i2);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(186, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getColrResponse(RadioResponseInfo radioResponseInfo, int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(187, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setColpResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(188, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setColrResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(189, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void queryCallForwardInTimeSlotStatusResponse(RadioResponseInfo radioResponseInfo, ArrayList<CallForwardInfoEx> arrayList) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            CallForwardInfoEx.writeVectorToParcel(hwParcel, arrayList);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(190, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setCallForwardInTimeSlotResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(191, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void runGbaAuthenticationResponse(RadioResponseInfo radioResponseInfo, ArrayList<String> arrayList) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            hwParcel.writeStringVector(arrayList);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(192, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setModemImsCfgResponse(RadioResponseInfo radioResponseInfo, String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(193, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void sendImsSmsExResponse(RadioResponseInfo radioResponseInfo, SendSmsResult sendSmsResult) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            sendSmsResult.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(194, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void acknowledgeLastIncomingGsmSmsExResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(195, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void acknowledgeLastIncomingCdmaSmsExResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(196, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setImsBearerNotificationResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(197, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setImsCfgFeatureValueResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(198, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getImsCfgFeatureValueResponse(RadioResponseInfo radioResponseInfo, int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(199, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setImsCfgProvisionValueResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(200, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getImsCfgProvisionValueResponse(RadioResponseInfo radioResponseInfo, String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(201, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setImsCfgResourceCapValueResponse(RadioResponseInfo radioResponseInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(ExternalSimConstants.EVENT_TYPE_RECEIVE_RSIM_AUTH_RSP, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getImsCfgResourceCapValueResponse(RadioResponseInfo radioResponseInfo, int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioResponse.kInterfaceName);
            radioResponseInfo.writeToParcel(hwParcel);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(ExternalSimConstants.EVENT_TYPE_RSIM_AUTH_DONE, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public ArrayList<String> interfaceChain() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(256067662, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readStringVector();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public String interfaceDescriptor() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(256136003, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readString();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public ArrayList<byte[]> getHashChain() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(256398152, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                ArrayList<byte[]> arrayList = new ArrayList<>();
                HwBlob buffer = hwParcel2.readBuffer(16L);
                int int32 = buffer.getInt32(8L);
                HwBlob embeddedBuffer = hwParcel2.readEmbeddedBuffer(int32 * 32, buffer.handle(), 0L, true);
                arrayList.clear();
                for (int i = 0; i < int32; i++) {
                    byte[] bArr = new byte[32];
                    embeddedBuffer.copyToInt8Array(i * 32, bArr, 32);
                    arrayList.add(bArr);
                }
                return arrayList;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setHALInstrumentation() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(256462420, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public boolean linkToDeath(IHwBinder.DeathRecipient deathRecipient, long j) throws RemoteException {
            return this.mRemote.linkToDeath(deathRecipient, j);
        }

        @Override
        public void ping() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(256921159, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public DebugInfo getDebugInfo() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(257049926, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                DebugInfo debugInfo = new DebugInfo();
                debugInfo.readFromParcel(hwParcel2);
                return debugInfo;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void notifySyspropsChanged() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(257120595, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public boolean unlinkToDeath(IHwBinder.DeathRecipient deathRecipient) throws RemoteException {
            return this.mRemote.unlinkToDeath(deathRecipient);
        }
    }

    public static abstract class Stub extends HwBinder implements IImsRadioResponse {
        @Override
        public IHwBinder asBinder() {
            return this;
        }

        @Override
        public final ArrayList<String> interfaceChain() {
            return new ArrayList<>(Arrays.asList(IImsRadioResponse.kInterfaceName, android.hardware.radio.V1_2.IRadioResponse.kInterfaceName, android.hardware.radio.V1_1.IRadioResponse.kInterfaceName, android.hardware.radio.V1_0.IRadioResponse.kInterfaceName, IBase.kInterfaceName));
        }

        @Override
        public final String interfaceDescriptor() {
            return IImsRadioResponse.kInterfaceName;
        }

        @Override
        public final ArrayList<byte[]> getHashChain() {
            return new ArrayList<>(Arrays.asList(new byte[]{73, -32, 61, 48, -32, -101, 87, -96, -90, 119, -81, -40, -34, 65, 10, -74, -87, -94, 56, 31, -95, 70, -57, -76, -116, 70, -99, PplMessageManager.Type.INSTRUCTION_DESCRIPTION2, 74, 60, 54, 69}, new byte[]{-38, -116, 106, -23, -111, -58, -92, -78, -124, -52, 110, 68, 83, 50, -32, 100, -30, -114, -24, -96, -108, -126, -19, 90, -1, -7, -47, 89, -20, 102, -108, -73}, new byte[]{5, -86, 61, -26, 19, 10, -105, -120, -3, -74, -12, -45, -52, 87, -61, -22, -112, -16, 103, -25, 122, 94, 9, -42, -89, 114, -20, 127, 107, -54, 51, -46}, new byte[]{45, -125, 58, -18, -48, -51, 29, 89, 67, 122, -54, 33, PplMessageManager.Type.INSTRUCTION_DESCRIPTION2, -27, -112, -87, 83, -49, 50, -68, -74, 104, 60, -42, 61, 8, -105, 98, -90, 67, -5, 73}, new byte[]{-67, -38, -74, 24, 77, 122, 52, 109, -90, -96, 125, -64, -126, -116, -15, -102, 105, 111, 76, -86, 54, 17, -59, 31, 46, 20, 86, 90, 20, -76, 15, -39}));
        }

        @Override
        public final void setHALInstrumentation() {
        }

        @Override
        public final boolean linkToDeath(IHwBinder.DeathRecipient deathRecipient, long j) {
            return true;
        }

        @Override
        public final void ping() {
        }

        @Override
        public final DebugInfo getDebugInfo() {
            DebugInfo debugInfo = new DebugInfo();
            debugInfo.pid = HidlSupport.getPidIfSharable();
            debugInfo.ptr = 0L;
            debugInfo.arch = 0;
            return debugInfo;
        }

        @Override
        public final void notifySyspropsChanged() {
            HwBinder.enableInstrumentation();
        }

        @Override
        public final boolean unlinkToDeath(IHwBinder.DeathRecipient deathRecipient) {
            return true;
        }

        @Override
        public IHwInterface queryLocalInterface(String str) {
            if (IImsRadioResponse.kInterfaceName.equals(str)) {
                return this;
            }
            return null;
        }

        public void registerAsService(String str) throws RemoteException {
            registerService(str);
        }

        public String toString() {
            return interfaceDescriptor() + "@Stub";
        }

        @Override
        public void onTransact(int i, HwParcel hwParcel, HwParcel hwParcel2, int i2) throws RemoteException {
            switch (i) {
                case 1:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo = new RadioResponseInfo();
                        radioResponseInfo.readFromParcel(hwParcel);
                        CardStatus cardStatus = new CardStatus();
                        cardStatus.readFromParcel(hwParcel);
                        getIccCardStatusResponse(radioResponseInfo, cardStatus);
                    }
                    break;
                case 2:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo2 = new RadioResponseInfo();
                        radioResponseInfo2.readFromParcel(hwParcel);
                        supplyIccPinForAppResponse(radioResponseInfo2, hwParcel.readInt32());
                    }
                    break;
                case 3:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo3 = new RadioResponseInfo();
                        radioResponseInfo3.readFromParcel(hwParcel);
                        supplyIccPukForAppResponse(radioResponseInfo3, hwParcel.readInt32());
                    }
                    break;
                case 4:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo4 = new RadioResponseInfo();
                        radioResponseInfo4.readFromParcel(hwParcel);
                        supplyIccPin2ForAppResponse(radioResponseInfo4, hwParcel.readInt32());
                    }
                    break;
                case 5:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo5 = new RadioResponseInfo();
                        radioResponseInfo5.readFromParcel(hwParcel);
                        supplyIccPuk2ForAppResponse(radioResponseInfo5, hwParcel.readInt32());
                    }
                    break;
                case 6:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo6 = new RadioResponseInfo();
                        radioResponseInfo6.readFromParcel(hwParcel);
                        changeIccPinForAppResponse(radioResponseInfo6, hwParcel.readInt32());
                    }
                    break;
                case 7:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo7 = new RadioResponseInfo();
                        radioResponseInfo7.readFromParcel(hwParcel);
                        changeIccPin2ForAppResponse(radioResponseInfo7, hwParcel.readInt32());
                    }
                    break;
                case 8:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo8 = new RadioResponseInfo();
                        radioResponseInfo8.readFromParcel(hwParcel);
                        supplyNetworkDepersonalizationResponse(radioResponseInfo8, hwParcel.readInt32());
                    }
                    break;
                case 9:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo9 = new RadioResponseInfo();
                        radioResponseInfo9.readFromParcel(hwParcel);
                        getCurrentCallsResponse(radioResponseInfo9, Call.readVectorFromParcel(hwParcel));
                    }
                    break;
                case 10:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo10 = new RadioResponseInfo();
                        radioResponseInfo10.readFromParcel(hwParcel);
                        dialResponse(radioResponseInfo10);
                    }
                    break;
                case 11:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo11 = new RadioResponseInfo();
                        radioResponseInfo11.readFromParcel(hwParcel);
                        getIMSIForAppResponse(radioResponseInfo11, hwParcel.readString());
                    }
                    break;
                case 12:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo12 = new RadioResponseInfo();
                        radioResponseInfo12.readFromParcel(hwParcel);
                        hangupConnectionResponse(radioResponseInfo12);
                    }
                    break;
                case 13:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo13 = new RadioResponseInfo();
                        radioResponseInfo13.readFromParcel(hwParcel);
                        hangupWaitingOrBackgroundResponse(radioResponseInfo13);
                    }
                    break;
                case 14:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo14 = new RadioResponseInfo();
                        radioResponseInfo14.readFromParcel(hwParcel);
                        hangupForegroundResumeBackgroundResponse(radioResponseInfo14);
                    }
                    break;
                case 15:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo15 = new RadioResponseInfo();
                        radioResponseInfo15.readFromParcel(hwParcel);
                        switchWaitingOrHoldingAndActiveResponse(radioResponseInfo15);
                    }
                    break;
                case 16:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo16 = new RadioResponseInfo();
                        radioResponseInfo16.readFromParcel(hwParcel);
                        conferenceResponse(radioResponseInfo16);
                    }
                    break;
                case 17:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo17 = new RadioResponseInfo();
                        radioResponseInfo17.readFromParcel(hwParcel);
                        rejectCallResponse(radioResponseInfo17);
                    }
                    break;
                case 18:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo18 = new RadioResponseInfo();
                        radioResponseInfo18.readFromParcel(hwParcel);
                        LastCallFailCauseInfo lastCallFailCauseInfo = new LastCallFailCauseInfo();
                        lastCallFailCauseInfo.readFromParcel(hwParcel);
                        getLastCallFailCauseResponse(radioResponseInfo18, lastCallFailCauseInfo);
                    }
                    break;
                case 19:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo19 = new RadioResponseInfo();
                        radioResponseInfo19.readFromParcel(hwParcel);
                        SignalStrength signalStrength = new SignalStrength();
                        signalStrength.readFromParcel(hwParcel);
                        getSignalStrengthResponse(radioResponseInfo19, signalStrength);
                    }
                    break;
                case 20:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo20 = new RadioResponseInfo();
                        radioResponseInfo20.readFromParcel(hwParcel);
                        VoiceRegStateResult voiceRegStateResult = new VoiceRegStateResult();
                        voiceRegStateResult.readFromParcel(hwParcel);
                        getVoiceRegistrationStateResponse(radioResponseInfo20, voiceRegStateResult);
                    }
                    break;
                case 21:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo21 = new RadioResponseInfo();
                        radioResponseInfo21.readFromParcel(hwParcel);
                        DataRegStateResult dataRegStateResult = new DataRegStateResult();
                        dataRegStateResult.readFromParcel(hwParcel);
                        getDataRegistrationStateResponse(radioResponseInfo21, dataRegStateResult);
                    }
                    break;
                case 22:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo22 = new RadioResponseInfo();
                        radioResponseInfo22.readFromParcel(hwParcel);
                        getOperatorResponse(radioResponseInfo22, hwParcel.readString(), hwParcel.readString(), hwParcel.readString());
                    }
                    break;
                case 23:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo23 = new RadioResponseInfo();
                        radioResponseInfo23.readFromParcel(hwParcel);
                        setRadioPowerResponse(radioResponseInfo23);
                    }
                    break;
                case 24:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo24 = new RadioResponseInfo();
                        radioResponseInfo24.readFromParcel(hwParcel);
                        sendDtmfResponse(radioResponseInfo24);
                    }
                    break;
                case 25:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo25 = new RadioResponseInfo();
                        radioResponseInfo25.readFromParcel(hwParcel);
                        SendSmsResult sendSmsResult = new SendSmsResult();
                        sendSmsResult.readFromParcel(hwParcel);
                        sendSmsResponse(radioResponseInfo25, sendSmsResult);
                    }
                    break;
                case 26:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo26 = new RadioResponseInfo();
                        radioResponseInfo26.readFromParcel(hwParcel);
                        SendSmsResult sendSmsResult2 = new SendSmsResult();
                        sendSmsResult2.readFromParcel(hwParcel);
                        sendSMSExpectMoreResponse(radioResponseInfo26, sendSmsResult2);
                    }
                    break;
                case 27:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo27 = new RadioResponseInfo();
                        radioResponseInfo27.readFromParcel(hwParcel);
                        SetupDataCallResult setupDataCallResult = new SetupDataCallResult();
                        setupDataCallResult.readFromParcel(hwParcel);
                        setupDataCallResponse(radioResponseInfo27, setupDataCallResult);
                    }
                    break;
                case 28:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo28 = new RadioResponseInfo();
                        radioResponseInfo28.readFromParcel(hwParcel);
                        IccIoResult iccIoResult = new IccIoResult();
                        iccIoResult.readFromParcel(hwParcel);
                        iccIOForAppResponse(radioResponseInfo28, iccIoResult);
                    }
                    break;
                case 29:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo29 = new RadioResponseInfo();
                        radioResponseInfo29.readFromParcel(hwParcel);
                        sendUssdResponse(radioResponseInfo29);
                    }
                    break;
                case 30:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo30 = new RadioResponseInfo();
                        radioResponseInfo30.readFromParcel(hwParcel);
                        cancelPendingUssdResponse(radioResponseInfo30);
                    }
                    break;
                case 31:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo31 = new RadioResponseInfo();
                        radioResponseInfo31.readFromParcel(hwParcel);
                        getClirResponse(radioResponseInfo31, hwParcel.readInt32(), hwParcel.readInt32());
                    }
                    break;
                case 32:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo32 = new RadioResponseInfo();
                        radioResponseInfo32.readFromParcel(hwParcel);
                        setClirResponse(radioResponseInfo32);
                    }
                    break;
                case 33:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo33 = new RadioResponseInfo();
                        radioResponseInfo33.readFromParcel(hwParcel);
                        getCallForwardStatusResponse(radioResponseInfo33, CallForwardInfo.readVectorFromParcel(hwParcel));
                    }
                    break;
                case 34:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo34 = new RadioResponseInfo();
                        radioResponseInfo34.readFromParcel(hwParcel);
                        setCallForwardResponse(radioResponseInfo34);
                    }
                    break;
                case 35:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo35 = new RadioResponseInfo();
                        radioResponseInfo35.readFromParcel(hwParcel);
                        getCallWaitingResponse(radioResponseInfo35, hwParcel.readBool(), hwParcel.readInt32());
                    }
                    break;
                case 36:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo36 = new RadioResponseInfo();
                        radioResponseInfo36.readFromParcel(hwParcel);
                        setCallWaitingResponse(radioResponseInfo36);
                    }
                    break;
                case 37:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo37 = new RadioResponseInfo();
                        radioResponseInfo37.readFromParcel(hwParcel);
                        acknowledgeLastIncomingGsmSmsResponse(radioResponseInfo37);
                    }
                    break;
                case 38:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo38 = new RadioResponseInfo();
                        radioResponseInfo38.readFromParcel(hwParcel);
                        acceptCallResponse(radioResponseInfo38);
                    }
                    break;
                case 39:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo39 = new RadioResponseInfo();
                        radioResponseInfo39.readFromParcel(hwParcel);
                        deactivateDataCallResponse(radioResponseInfo39);
                    }
                    break;
                case 40:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo40 = new RadioResponseInfo();
                        radioResponseInfo40.readFromParcel(hwParcel);
                        getFacilityLockForAppResponse(radioResponseInfo40, hwParcel.readInt32());
                    }
                    break;
                case 41:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo41 = new RadioResponseInfo();
                        radioResponseInfo41.readFromParcel(hwParcel);
                        setFacilityLockForAppResponse(radioResponseInfo41, hwParcel.readInt32());
                    }
                    break;
                case 42:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo42 = new RadioResponseInfo();
                        radioResponseInfo42.readFromParcel(hwParcel);
                        setBarringPasswordResponse(radioResponseInfo42);
                    }
                    break;
                case 43:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo43 = new RadioResponseInfo();
                        radioResponseInfo43.readFromParcel(hwParcel);
                        getNetworkSelectionModeResponse(radioResponseInfo43, hwParcel.readBool());
                    }
                    break;
                case 44:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo44 = new RadioResponseInfo();
                        radioResponseInfo44.readFromParcel(hwParcel);
                        setNetworkSelectionModeAutomaticResponse(radioResponseInfo44);
                    }
                    break;
                case 45:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo45 = new RadioResponseInfo();
                        radioResponseInfo45.readFromParcel(hwParcel);
                        setNetworkSelectionModeManualResponse(radioResponseInfo45);
                    }
                    break;
                case 46:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo46 = new RadioResponseInfo();
                        radioResponseInfo46.readFromParcel(hwParcel);
                        getAvailableNetworksResponse(radioResponseInfo46, OperatorInfo.readVectorFromParcel(hwParcel));
                    }
                    break;
                case 47:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo47 = new RadioResponseInfo();
                        radioResponseInfo47.readFromParcel(hwParcel);
                        startDtmfResponse(radioResponseInfo47);
                    }
                    break;
                case 48:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo48 = new RadioResponseInfo();
                        radioResponseInfo48.readFromParcel(hwParcel);
                        stopDtmfResponse(radioResponseInfo48);
                    }
                    break;
                case 49:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo49 = new RadioResponseInfo();
                        radioResponseInfo49.readFromParcel(hwParcel);
                        getBasebandVersionResponse(radioResponseInfo49, hwParcel.readString());
                    }
                    break;
                case 50:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo50 = new RadioResponseInfo();
                        radioResponseInfo50.readFromParcel(hwParcel);
                        separateConnectionResponse(radioResponseInfo50);
                    }
                    break;
                case 51:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo51 = new RadioResponseInfo();
                        radioResponseInfo51.readFromParcel(hwParcel);
                        setMuteResponse(radioResponseInfo51);
                    }
                    break;
                case 52:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo52 = new RadioResponseInfo();
                        radioResponseInfo52.readFromParcel(hwParcel);
                        getMuteResponse(radioResponseInfo52, hwParcel.readBool());
                    }
                    break;
                case 53:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo53 = new RadioResponseInfo();
                        radioResponseInfo53.readFromParcel(hwParcel);
                        getClipResponse(radioResponseInfo53, hwParcel.readInt32());
                    }
                    break;
                case 54:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo54 = new RadioResponseInfo();
                        radioResponseInfo54.readFromParcel(hwParcel);
                        getDataCallListResponse(radioResponseInfo54, SetupDataCallResult.readVectorFromParcel(hwParcel));
                    }
                    break;
                case 55:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo55 = new RadioResponseInfo();
                        radioResponseInfo55.readFromParcel(hwParcel);
                        setSuppServiceNotificationsResponse(radioResponseInfo55);
                    }
                    break;
                case 56:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo56 = new RadioResponseInfo();
                        radioResponseInfo56.readFromParcel(hwParcel);
                        writeSmsToSimResponse(radioResponseInfo56, hwParcel.readInt32());
                    }
                    break;
                case 57:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo57 = new RadioResponseInfo();
                        radioResponseInfo57.readFromParcel(hwParcel);
                        deleteSmsOnSimResponse(radioResponseInfo57);
                    }
                    break;
                case 58:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo58 = new RadioResponseInfo();
                        radioResponseInfo58.readFromParcel(hwParcel);
                        setBandModeResponse(radioResponseInfo58);
                    }
                    break;
                case 59:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo59 = new RadioResponseInfo();
                        radioResponseInfo59.readFromParcel(hwParcel);
                        getAvailableBandModesResponse(radioResponseInfo59, hwParcel.readInt32Vector());
                    }
                    break;
                case 60:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo60 = new RadioResponseInfo();
                        radioResponseInfo60.readFromParcel(hwParcel);
                        sendEnvelopeResponse(radioResponseInfo60, hwParcel.readString());
                    }
                    break;
                case 61:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo61 = new RadioResponseInfo();
                        radioResponseInfo61.readFromParcel(hwParcel);
                        sendTerminalResponseToSimResponse(radioResponseInfo61);
                    }
                    break;
                case 62:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo62 = new RadioResponseInfo();
                        radioResponseInfo62.readFromParcel(hwParcel);
                        handleStkCallSetupRequestFromSimResponse(radioResponseInfo62);
                    }
                    break;
                case 63:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo63 = new RadioResponseInfo();
                        radioResponseInfo63.readFromParcel(hwParcel);
                        explicitCallTransferResponse(radioResponseInfo63);
                    }
                    break;
                case 64:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo64 = new RadioResponseInfo();
                        radioResponseInfo64.readFromParcel(hwParcel);
                        setPreferredNetworkTypeResponse(radioResponseInfo64);
                    }
                    break;
                case 65:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo65 = new RadioResponseInfo();
                        radioResponseInfo65.readFromParcel(hwParcel);
                        getPreferredNetworkTypeResponse(radioResponseInfo65, hwParcel.readInt32());
                    }
                    break;
                case 66:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo66 = new RadioResponseInfo();
                        radioResponseInfo66.readFromParcel(hwParcel);
                        getNeighboringCidsResponse(radioResponseInfo66, NeighboringCell.readVectorFromParcel(hwParcel));
                    }
                    break;
                case 67:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo67 = new RadioResponseInfo();
                        radioResponseInfo67.readFromParcel(hwParcel);
                        setLocationUpdatesResponse(radioResponseInfo67);
                    }
                    break;
                case 68:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo68 = new RadioResponseInfo();
                        radioResponseInfo68.readFromParcel(hwParcel);
                        setCdmaSubscriptionSourceResponse(radioResponseInfo68);
                    }
                    break;
                case 69:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo69 = new RadioResponseInfo();
                        radioResponseInfo69.readFromParcel(hwParcel);
                        setCdmaRoamingPreferenceResponse(radioResponseInfo69);
                    }
                    break;
                case 70:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo70 = new RadioResponseInfo();
                        radioResponseInfo70.readFromParcel(hwParcel);
                        getCdmaRoamingPreferenceResponse(radioResponseInfo70, hwParcel.readInt32());
                    }
                    break;
                case 71:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo71 = new RadioResponseInfo();
                        radioResponseInfo71.readFromParcel(hwParcel);
                        setTTYModeResponse(radioResponseInfo71);
                    }
                    break;
                case 72:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo72 = new RadioResponseInfo();
                        radioResponseInfo72.readFromParcel(hwParcel);
                        getTTYModeResponse(radioResponseInfo72, hwParcel.readInt32());
                    }
                    break;
                case 73:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo73 = new RadioResponseInfo();
                        radioResponseInfo73.readFromParcel(hwParcel);
                        setPreferredVoicePrivacyResponse(radioResponseInfo73);
                    }
                    break;
                case NvItem.LTE_SCAN_PRIORITY_25:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo74 = new RadioResponseInfo();
                        radioResponseInfo74.readFromParcel(hwParcel);
                        getPreferredVoicePrivacyResponse(radioResponseInfo74, hwParcel.readBool());
                    }
                    break;
                case NvItem.LTE_SCAN_PRIORITY_26:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo75 = new RadioResponseInfo();
                        radioResponseInfo75.readFromParcel(hwParcel);
                        sendCDMAFeatureCodeResponse(radioResponseInfo75);
                    }
                    break;
                case NvItem.LTE_SCAN_PRIORITY_41:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo76 = new RadioResponseInfo();
                        radioResponseInfo76.readFromParcel(hwParcel);
                        sendBurstDtmfResponse(radioResponseInfo76);
                    }
                    break;
                case NvItem.LTE_HIDDEN_BAND_PRIORITY_25:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo77 = new RadioResponseInfo();
                        radioResponseInfo77.readFromParcel(hwParcel);
                        SendSmsResult sendSmsResult3 = new SendSmsResult();
                        sendSmsResult3.readFromParcel(hwParcel);
                        sendCdmaSmsResponse(radioResponseInfo77, sendSmsResult3);
                    }
                    break;
                case NvItem.LTE_HIDDEN_BAND_PRIORITY_26:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo78 = new RadioResponseInfo();
                        radioResponseInfo78.readFromParcel(hwParcel);
                        acknowledgeLastIncomingCdmaSmsResponse(radioResponseInfo78);
                    }
                    break;
                case 79:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo79 = new RadioResponseInfo();
                        radioResponseInfo79.readFromParcel(hwParcel);
                        getGsmBroadcastConfigResponse(radioResponseInfo79, GsmBroadcastSmsConfigInfo.readVectorFromParcel(hwParcel));
                    }
                    break;
                case IWorldPhone.EVENT_SERVICE_STATE_CHANGED_1:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo80 = new RadioResponseInfo();
                        radioResponseInfo80.readFromParcel(hwParcel);
                        setGsmBroadcastConfigResponse(radioResponseInfo80);
                    }
                    break;
                case 81:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo81 = new RadioResponseInfo();
                        radioResponseInfo81.readFromParcel(hwParcel);
                        setGsmBroadcastActivationResponse(radioResponseInfo81);
                    }
                    break;
                case IWorldPhone.EVENT_SERVICE_STATE_CHANGED_3:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo82 = new RadioResponseInfo();
                        radioResponseInfo82.readFromParcel(hwParcel);
                        getCdmaBroadcastConfigResponse(radioResponseInfo82, CdmaBroadcastSmsConfigInfo.readVectorFromParcel(hwParcel));
                    }
                    break;
                case IWorldPhone.EVENT_SERVICE_STATE_CHANGED_4:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo83 = new RadioResponseInfo();
                        radioResponseInfo83.readFromParcel(hwParcel);
                        setCdmaBroadcastConfigResponse(radioResponseInfo83);
                    }
                    break;
                case 84:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo84 = new RadioResponseInfo();
                        radioResponseInfo84.readFromParcel(hwParcel);
                        setCdmaBroadcastActivationResponse(radioResponseInfo84);
                    }
                    break;
                case 85:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo85 = new RadioResponseInfo();
                        radioResponseInfo85.readFromParcel(hwParcel);
                        getCDMASubscriptionResponse(radioResponseInfo85, hwParcel.readString(), hwParcel.readString(), hwParcel.readString(), hwParcel.readString(), hwParcel.readString());
                    }
                    break;
                case 86:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo86 = new RadioResponseInfo();
                        radioResponseInfo86.readFromParcel(hwParcel);
                        writeSmsToRuimResponse(radioResponseInfo86, hwParcel.readInt32());
                    }
                    break;
                case 87:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo87 = new RadioResponseInfo();
                        radioResponseInfo87.readFromParcel(hwParcel);
                        deleteSmsOnRuimResponse(radioResponseInfo87);
                    }
                    break;
                case 88:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo88 = new RadioResponseInfo();
                        radioResponseInfo88.readFromParcel(hwParcel);
                        getDeviceIdentityResponse(radioResponseInfo88, hwParcel.readString(), hwParcel.readString(), hwParcel.readString(), hwParcel.readString());
                    }
                    break;
                case 89:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo89 = new RadioResponseInfo();
                        radioResponseInfo89.readFromParcel(hwParcel);
                        exitEmergencyCallbackModeResponse(radioResponseInfo89);
                    }
                    break;
                case 90:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo90 = new RadioResponseInfo();
                        radioResponseInfo90.readFromParcel(hwParcel);
                        getSmscAddressResponse(radioResponseInfo90, hwParcel.readString());
                    }
                    break;
                case 91:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo91 = new RadioResponseInfo();
                        radioResponseInfo91.readFromParcel(hwParcel);
                        setSmscAddressResponse(radioResponseInfo91);
                    }
                    break;
                case 92:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo92 = new RadioResponseInfo();
                        radioResponseInfo92.readFromParcel(hwParcel);
                        reportSmsMemoryStatusResponse(radioResponseInfo92);
                    }
                    break;
                case 93:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo93 = new RadioResponseInfo();
                        radioResponseInfo93.readFromParcel(hwParcel);
                        reportStkServiceIsRunningResponse(radioResponseInfo93);
                    }
                    break;
                case 94:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo94 = new RadioResponseInfo();
                        radioResponseInfo94.readFromParcel(hwParcel);
                        getCdmaSubscriptionSourceResponse(radioResponseInfo94, hwParcel.readInt32());
                    }
                    break;
                case 95:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo95 = new RadioResponseInfo();
                        radioResponseInfo95.readFromParcel(hwParcel);
                        requestIsimAuthenticationResponse(radioResponseInfo95, hwParcel.readString());
                    }
                    break;
                case 96:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo96 = new RadioResponseInfo();
                        radioResponseInfo96.readFromParcel(hwParcel);
                        acknowledgeIncomingGsmSmsWithPduResponse(radioResponseInfo96);
                    }
                    break;
                case 97:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo97 = new RadioResponseInfo();
                        radioResponseInfo97.readFromParcel(hwParcel);
                        IccIoResult iccIoResult2 = new IccIoResult();
                        iccIoResult2.readFromParcel(hwParcel);
                        sendEnvelopeWithStatusResponse(radioResponseInfo97, iccIoResult2);
                    }
                    break;
                case 98:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo98 = new RadioResponseInfo();
                        radioResponseInfo98.readFromParcel(hwParcel);
                        getVoiceRadioTechnologyResponse(radioResponseInfo98, hwParcel.readInt32());
                    }
                    break;
                case 99:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo99 = new RadioResponseInfo();
                        radioResponseInfo99.readFromParcel(hwParcel);
                        getCellInfoListResponse(radioResponseInfo99, CellInfo.readVectorFromParcel(hwParcel));
                    }
                    break;
                case 100:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo100 = new RadioResponseInfo();
                        radioResponseInfo100.readFromParcel(hwParcel);
                        setCellInfoListRateResponse(radioResponseInfo100);
                    }
                    break;
                case 101:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo101 = new RadioResponseInfo();
                        radioResponseInfo101.readFromParcel(hwParcel);
                        setInitialAttachApnResponse(radioResponseInfo101);
                    }
                    break;
                case 102:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo102 = new RadioResponseInfo();
                        radioResponseInfo102.readFromParcel(hwParcel);
                        getImsRegistrationStateResponse(radioResponseInfo102, hwParcel.readBool(), hwParcel.readInt32());
                    }
                    break;
                case 103:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo103 = new RadioResponseInfo();
                        radioResponseInfo103.readFromParcel(hwParcel);
                        SendSmsResult sendSmsResult4 = new SendSmsResult();
                        sendSmsResult4.readFromParcel(hwParcel);
                        sendImsSmsResponse(radioResponseInfo103, sendSmsResult4);
                    }
                    break;
                case MtkCharacterSets.ISO_2022_CN:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo104 = new RadioResponseInfo();
                        radioResponseInfo104.readFromParcel(hwParcel);
                        IccIoResult iccIoResult3 = new IccIoResult();
                        iccIoResult3.readFromParcel(hwParcel);
                        iccTransmitApduBasicChannelResponse(radioResponseInfo104, iccIoResult3);
                    }
                    break;
                case MtkCharacterSets.ISO_2022_CN_EXT:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo105 = new RadioResponseInfo();
                        radioResponseInfo105.readFromParcel(hwParcel);
                        iccOpenLogicalChannelResponse(radioResponseInfo105, hwParcel.readInt32(), hwParcel.readInt8Vector());
                    }
                    break;
                case 106:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo106 = new RadioResponseInfo();
                        radioResponseInfo106.readFromParcel(hwParcel);
                        iccCloseLogicalChannelResponse(radioResponseInfo106);
                    }
                    break;
                case 107:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo107 = new RadioResponseInfo();
                        radioResponseInfo107.readFromParcel(hwParcel);
                        IccIoResult iccIoResult4 = new IccIoResult();
                        iccIoResult4.readFromParcel(hwParcel);
                        iccTransmitApduLogicalChannelResponse(radioResponseInfo107, iccIoResult4);
                    }
                    break;
                case 108:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo108 = new RadioResponseInfo();
                        radioResponseInfo108.readFromParcel(hwParcel);
                        nvReadItemResponse(radioResponseInfo108, hwParcel.readString());
                    }
                    break;
                case 109:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo109 = new RadioResponseInfo();
                        radioResponseInfo109.readFromParcel(hwParcel);
                        nvWriteItemResponse(radioResponseInfo109);
                    }
                    break;
                case 110:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo110 = new RadioResponseInfo();
                        radioResponseInfo110.readFromParcel(hwParcel);
                        nvWriteCdmaPrlResponse(radioResponseInfo110);
                    }
                    break;
                case 111:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo111 = new RadioResponseInfo();
                        radioResponseInfo111.readFromParcel(hwParcel);
                        nvResetConfigResponse(radioResponseInfo111);
                    }
                    break;
                case 112:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo112 = new RadioResponseInfo();
                        radioResponseInfo112.readFromParcel(hwParcel);
                        setUiccSubscriptionResponse(radioResponseInfo112);
                    }
                    break;
                case 113:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo113 = new RadioResponseInfo();
                        radioResponseInfo113.readFromParcel(hwParcel);
                        setDataAllowedResponse(radioResponseInfo113);
                    }
                    break;
                case 114:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo114 = new RadioResponseInfo();
                        radioResponseInfo114.readFromParcel(hwParcel);
                        getHardwareConfigResponse(radioResponseInfo114, HardwareConfig.readVectorFromParcel(hwParcel));
                    }
                    break;
                case DataCallFailCause.EMM_ACCESS_BARRED:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo115 = new RadioResponseInfo();
                        radioResponseInfo115.readFromParcel(hwParcel);
                        IccIoResult iccIoResult5 = new IccIoResult();
                        iccIoResult5.readFromParcel(hwParcel);
                        requestIccSimAuthenticationResponse(radioResponseInfo115, iccIoResult5);
                    }
                    break;
                case DataCallFailCause.EMERGENCY_IFACE_ONLY:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo116 = new RadioResponseInfo();
                        radioResponseInfo116.readFromParcel(hwParcel);
                        setDataProfileResponse(radioResponseInfo116);
                    }
                    break;
                case DataCallFailCause.IFACE_MISMATCH:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo117 = new RadioResponseInfo();
                        radioResponseInfo117.readFromParcel(hwParcel);
                        requestShutdownResponse(radioResponseInfo117);
                    }
                    break;
                case DataCallFailCause.COMPANION_IFACE_IN_USE:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo118 = new RadioResponseInfo();
                        radioResponseInfo118.readFromParcel(hwParcel);
                        RadioCapability radioCapability = new RadioCapability();
                        radioCapability.readFromParcel(hwParcel);
                        getRadioCapabilityResponse(radioResponseInfo118, radioCapability);
                    }
                    break;
                case DataCallFailCause.IP_ADDRESS_MISMATCH:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo119 = new RadioResponseInfo();
                        radioResponseInfo119.readFromParcel(hwParcel);
                        RadioCapability radioCapability2 = new RadioCapability();
                        radioCapability2.readFromParcel(hwParcel);
                        setRadioCapabilityResponse(radioResponseInfo119, radioCapability2);
                    }
                    break;
                case DataCallFailCause.IFACE_AND_POL_FAMILY_MISMATCH:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo120 = new RadioResponseInfo();
                        radioResponseInfo120.readFromParcel(hwParcel);
                        LceStatusInfo lceStatusInfo = new LceStatusInfo();
                        lceStatusInfo.readFromParcel(hwParcel);
                        startLceServiceResponse(radioResponseInfo120, lceStatusInfo);
                    }
                    break;
                case DataCallFailCause.EMM_ACCESS_BARRED_INFINITE_RETRY:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo121 = new RadioResponseInfo();
                        radioResponseInfo121.readFromParcel(hwParcel);
                        LceStatusInfo lceStatusInfo2 = new LceStatusInfo();
                        lceStatusInfo2.readFromParcel(hwParcel);
                        stopLceServiceResponse(radioResponseInfo121, lceStatusInfo2);
                    }
                    break;
                case DataCallFailCause.AUTH_FAILURE_ON_EMERGENCY_CALL:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo122 = new RadioResponseInfo();
                        radioResponseInfo122.readFromParcel(hwParcel);
                        LceDataInfo lceDataInfo = new LceDataInfo();
                        lceDataInfo.readFromParcel(hwParcel);
                        pullLceDataResponse(radioResponseInfo122, lceDataInfo);
                    }
                    break;
                case 123:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo123 = new RadioResponseInfo();
                        radioResponseInfo123.readFromParcel(hwParcel);
                        ActivityStatsInfo activityStatsInfo = new ActivityStatsInfo();
                        activityStatsInfo.readFromParcel(hwParcel);
                        getModemActivityInfoResponse(radioResponseInfo123, activityStatsInfo);
                    }
                    break;
                case 124:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo124 = new RadioResponseInfo();
                        radioResponseInfo124.readFromParcel(hwParcel);
                        setAllowedCarriersResponse(radioResponseInfo124, hwParcel.readInt32());
                    }
                    break;
                case 125:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo125 = new RadioResponseInfo();
                        radioResponseInfo125.readFromParcel(hwParcel);
                        boolean bool = hwParcel.readBool();
                        CarrierRestrictions carrierRestrictions = new CarrierRestrictions();
                        carrierRestrictions.readFromParcel(hwParcel);
                        getAllowedCarriersResponse(radioResponseInfo125, bool, carrierRestrictions);
                    }
                    break;
                case 126:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo126 = new RadioResponseInfo();
                        radioResponseInfo126.readFromParcel(hwParcel);
                        sendDeviceStateResponse(radioResponseInfo126);
                    }
                    break;
                case 127:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo127 = new RadioResponseInfo();
                        radioResponseInfo127.readFromParcel(hwParcel);
                        setIndicationFilterResponse(radioResponseInfo127);
                    }
                    break;
                case 128:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo128 = new RadioResponseInfo();
                        radioResponseInfo128.readFromParcel(hwParcel);
                        setSimCardPowerResponse(radioResponseInfo128);
                    }
                    break;
                case 129:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioResponse.kInterfaceName);
                        acknowledgeRequest(hwParcel.readInt32());
                    }
                    break;
                case 130:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_1.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo129 = new RadioResponseInfo();
                        radioResponseInfo129.readFromParcel(hwParcel);
                        setCarrierInfoForImsiEncryptionResponse(radioResponseInfo129);
                    }
                    break;
                case RadioCdmaSmsConst.UDH_EO_DATA_SEGMENT_MAX:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_1.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo130 = new RadioResponseInfo();
                        radioResponseInfo130.readFromParcel(hwParcel);
                        setSimCardPowerResponse_1_1(radioResponseInfo130);
                    }
                    break;
                case 132:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_1.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo131 = new RadioResponseInfo();
                        radioResponseInfo131.readFromParcel(hwParcel);
                        startNetworkScanResponse(radioResponseInfo131);
                    }
                    break;
                case 133:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_1.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo132 = new RadioResponseInfo();
                        radioResponseInfo132.readFromParcel(hwParcel);
                        stopNetworkScanResponse(radioResponseInfo132);
                    }
                    break;
                case RadioCdmaSmsConst.UDH_VAR_PIC_SIZE:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_1.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo133 = new RadioResponseInfo();
                        radioResponseInfo133.readFromParcel(hwParcel);
                        KeepaliveStatus keepaliveStatus = new KeepaliveStatus();
                        keepaliveStatus.readFromParcel(hwParcel);
                        startKeepaliveResponse(radioResponseInfo133, keepaliveStatus);
                    }
                    break;
                case 135:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_1.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo134 = new RadioResponseInfo();
                        radioResponseInfo134.readFromParcel(hwParcel);
                        stopKeepaliveResponse(radioResponseInfo134);
                    }
                    break;
                case 136:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_2.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo135 = new RadioResponseInfo();
                        radioResponseInfo135.readFromParcel(hwParcel);
                        getCellInfoListResponse_1_2(radioResponseInfo135, android.hardware.radio.V1_2.CellInfo.readVectorFromParcel(hwParcel));
                    }
                    break;
                case MtkPduHeaders.STATE_SKIP_RETRYING:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_2.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo136 = new RadioResponseInfo();
                        radioResponseInfo136.readFromParcel(hwParcel);
                        android.hardware.radio.V1_2.CardStatus cardStatus2 = new android.hardware.radio.V1_2.CardStatus();
                        cardStatus2.readFromParcel(hwParcel);
                        getIccCardStatusResponse_1_2(radioResponseInfo136, cardStatus2);
                    }
                    break;
                case 138:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_2.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo137 = new RadioResponseInfo();
                        radioResponseInfo137.readFromParcel(hwParcel);
                        setSignalStrengthReportingCriteriaResponse(radioResponseInfo137);
                    }
                    break;
                case 139:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_2.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo138 = new RadioResponseInfo();
                        radioResponseInfo138.readFromParcel(hwParcel);
                        setLinkCapacityReportingCriteriaResponse(radioResponseInfo138);
                    }
                    break;
                case 140:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_2.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo139 = new RadioResponseInfo();
                        radioResponseInfo139.readFromParcel(hwParcel);
                        getCurrentCallsResponse_1_2(radioResponseInfo139, android.hardware.radio.V1_2.Call.readVectorFromParcel(hwParcel));
                    }
                    break;
                case 141:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_2.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo140 = new RadioResponseInfo();
                        radioResponseInfo140.readFromParcel(hwParcel);
                        android.hardware.radio.V1_2.SignalStrength signalStrength2 = new android.hardware.radio.V1_2.SignalStrength();
                        signalStrength2.readFromParcel(hwParcel);
                        getSignalStrengthResponse_1_2(radioResponseInfo140, signalStrength2);
                    }
                    break;
                case 142:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_2.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo141 = new RadioResponseInfo();
                        radioResponseInfo141.readFromParcel(hwParcel);
                        android.hardware.radio.V1_2.VoiceRegStateResult voiceRegStateResult2 = new android.hardware.radio.V1_2.VoiceRegStateResult();
                        voiceRegStateResult2.readFromParcel(hwParcel);
                        getVoiceRegistrationStateResponse_1_2(radioResponseInfo141, voiceRegStateResult2);
                    }
                    break;
                case 143:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_2.IRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo142 = new RadioResponseInfo();
                        radioResponseInfo142.readFromParcel(hwParcel);
                        android.hardware.radio.V1_2.DataRegStateResult dataRegStateResult2 = new android.hardware.radio.V1_2.DataRegStateResult();
                        dataRegStateResult2.readFromParcel(hwParcel);
                        getDataRegistrationStateResponse_1_2(radioResponseInfo142, dataRegStateResult2);
                    }
                    break;
                case 144:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo143 = new RadioResponseInfo();
                        radioResponseInfo143.readFromParcel(hwParcel);
                        hangupAllResponse(radioResponseInfo143);
                    }
                    break;
                case 145:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo144 = new RadioResponseInfo();
                        radioResponseInfo144.readFromParcel(hwParcel);
                        setCallIndicationResponse(radioResponseInfo144);
                    }
                    break;
                case MtkPduPart.P_DATE:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo145 = new RadioResponseInfo();
                        radioResponseInfo145.readFromParcel(hwParcel);
                        emergencyDialResponse(radioResponseInfo145);
                    }
                    break;
                case 147:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo146 = new RadioResponseInfo();
                        radioResponseInfo146.readFromParcel(hwParcel);
                        setEccServiceCategoryResponse(radioResponseInfo146);
                    }
                    break;
                case 148:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo147 = new RadioResponseInfo();
                        radioResponseInfo147.readFromParcel(hwParcel);
                        videoCallAcceptResponse(radioResponseInfo147);
                    }
                    break;
                case 149:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo148 = new RadioResponseInfo();
                        radioResponseInfo148.readFromParcel(hwParcel);
                        imsEctCommandResponse(radioResponseInfo148);
                    }
                    break;
                case 150:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo149 = new RadioResponseInfo();
                        radioResponseInfo149.readFromParcel(hwParcel);
                        holdCallResponse(radioResponseInfo149);
                    }
                    break;
                case 151:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo150 = new RadioResponseInfo();
                        radioResponseInfo150.readFromParcel(hwParcel);
                        resumeCallResponse(radioResponseInfo150);
                    }
                    break;
                case 152:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo151 = new RadioResponseInfo();
                        radioResponseInfo151.readFromParcel(hwParcel);
                        imsDeregNotificationResponse(radioResponseInfo151);
                    }
                    break;
                case 153:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo152 = new RadioResponseInfo();
                        radioResponseInfo152.readFromParcel(hwParcel);
                        setImsEnableResponse(radioResponseInfo152);
                    }
                    break;
                case 154:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo153 = new RadioResponseInfo();
                        radioResponseInfo153.readFromParcel(hwParcel);
                        setVolteEnableResponse(radioResponseInfo153);
                    }
                    break;
                case 155:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo154 = new RadioResponseInfo();
                        radioResponseInfo154.readFromParcel(hwParcel);
                        setWfcEnableResponse(radioResponseInfo154);
                    }
                    break;
                case 156:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo155 = new RadioResponseInfo();
                        radioResponseInfo155.readFromParcel(hwParcel);
                        setVilteEnableResponse(radioResponseInfo155);
                    }
                    break;
                case 157:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo156 = new RadioResponseInfo();
                        radioResponseInfo156.readFromParcel(hwParcel);
                        setViWifiEnableResponse(radioResponseInfo156);
                    }
                    break;
                case 158:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo157 = new RadioResponseInfo();
                        radioResponseInfo157.readFromParcel(hwParcel);
                        setRcsUaEnableResponse(radioResponseInfo157);
                    }
                    break;
                case 159:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo158 = new RadioResponseInfo();
                        radioResponseInfo158.readFromParcel(hwParcel);
                        setImsVoiceEnableResponse(radioResponseInfo158);
                    }
                    break;
                case 160:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo159 = new RadioResponseInfo();
                        radioResponseInfo159.readFromParcel(hwParcel);
                        setImsVideoEnableResponse(radioResponseInfo159);
                    }
                    break;
                case 161:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo160 = new RadioResponseInfo();
                        radioResponseInfo160.readFromParcel(hwParcel);
                        setImscfgResponse(radioResponseInfo160);
                    }
                    break;
                case 162:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo161 = new RadioResponseInfo();
                        radioResponseInfo161.readFromParcel(hwParcel);
                        getProvisionValueResponse(radioResponseInfo161);
                    }
                    break;
                case 163:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo162 = new RadioResponseInfo();
                        radioResponseInfo162.readFromParcel(hwParcel);
                        setProvisionValueResponse(radioResponseInfo162);
                    }
                    break;
                case 164:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo163 = new RadioResponseInfo();
                        radioResponseInfo163.readFromParcel(hwParcel);
                        addImsConferenceCallMemberResponse(radioResponseInfo163);
                    }
                    break;
                case 165:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo164 = new RadioResponseInfo();
                        radioResponseInfo164.readFromParcel(hwParcel);
                        removeImsConferenceCallMemberResponse(radioResponseInfo164);
                    }
                    break;
                case 166:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo165 = new RadioResponseInfo();
                        radioResponseInfo165.readFromParcel(hwParcel);
                        setWfcProfileResponse(radioResponseInfo165);
                    }
                    break;
                case MtkPduPart.P_TRANSFER_ENCODING:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo166 = new RadioResponseInfo();
                        radioResponseInfo166.readFromParcel(hwParcel);
                        conferenceDialResponse(radioResponseInfo166);
                    }
                    break;
                case 168:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo167 = new RadioResponseInfo();
                        radioResponseInfo167.readFromParcel(hwParcel);
                        vtDialWithSipUriResponse(radioResponseInfo167);
                    }
                    break;
                case 169:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo168 = new RadioResponseInfo();
                        radioResponseInfo168.readFromParcel(hwParcel);
                        vtDialResponse(radioResponseInfo168);
                    }
                    break;
                case 170:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo169 = new RadioResponseInfo();
                        radioResponseInfo169.readFromParcel(hwParcel);
                        dialWithSipUriResponse(radioResponseInfo169);
                    }
                    break;
                case 171:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo170 = new RadioResponseInfo();
                        radioResponseInfo170.readFromParcel(hwParcel);
                        sendUssiResponse(radioResponseInfo170);
                    }
                    break;
                case 172:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo171 = new RadioResponseInfo();
                        radioResponseInfo171.readFromParcel(hwParcel);
                        cancelUssiResponse(radioResponseInfo171);
                    }
                    break;
                case 173:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo172 = new RadioResponseInfo();
                        radioResponseInfo172.readFromParcel(hwParcel);
                        getXcapStatusResponse(radioResponseInfo172);
                    }
                    break;
                case 174:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo173 = new RadioResponseInfo();
                        radioResponseInfo173.readFromParcel(hwParcel);
                        resetSuppServResponse(radioResponseInfo173);
                    }
                    break;
                case 175:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo174 = new RadioResponseInfo();
                        radioResponseInfo174.readFromParcel(hwParcel);
                        setupXcapUserAgentStringResponse(radioResponseInfo174);
                    }
                    break;
                case 176:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo175 = new RadioResponseInfo();
                        radioResponseInfo175.readFromParcel(hwParcel);
                        forceReleaseCallResponse(radioResponseInfo175);
                    }
                    break;
                case 177:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo176 = new RadioResponseInfo();
                        radioResponseInfo176.readFromParcel(hwParcel);
                        imsBearerActivationDoneResponse(radioResponseInfo176);
                    }
                    break;
                case 178:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo177 = new RadioResponseInfo();
                        radioResponseInfo177.readFromParcel(hwParcel);
                        imsBearerDeactivationDoneResponse(radioResponseInfo177);
                    }
                    break;
                case 179:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo178 = new RadioResponseInfo();
                        radioResponseInfo178.readFromParcel(hwParcel);
                        setImsCallStatusResponse(radioResponseInfo178);
                    }
                    break;
                case 180:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo179 = new RadioResponseInfo();
                        radioResponseInfo179.readFromParcel(hwParcel);
                        updateImsRegistrationStatusResponse(radioResponseInfo179);
                    }
                    break;
                case 181:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo180 = new RadioResponseInfo();
                        radioResponseInfo180.readFromParcel(hwParcel);
                        setImsRtpReportResponse(radioResponseInfo180);
                    }
                    break;
                case 182:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo181 = new RadioResponseInfo();
                        radioResponseInfo181.readFromParcel(hwParcel);
                        pullCallResponse(radioResponseInfo181);
                    }
                    break;
                case 183:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo182 = new RadioResponseInfo();
                        radioResponseInfo182.readFromParcel(hwParcel);
                        setImsRegistrationReportResponse(radioResponseInfo182);
                    }
                    break;
                case 184:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo183 = new RadioResponseInfo();
                        radioResponseInfo183.readFromParcel(hwParcel);
                        setVoiceDomainPreferenceResponse(radioResponseInfo183);
                    }
                    break;
                case 185:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo184 = new RadioResponseInfo();
                        radioResponseInfo184.readFromParcel(hwParcel);
                        setClipResponse(radioResponseInfo184);
                    }
                    break;
                case 186:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo185 = new RadioResponseInfo();
                        radioResponseInfo185.readFromParcel(hwParcel);
                        getColpResponse(radioResponseInfo185, hwParcel.readInt32(), hwParcel.readInt32());
                    }
                    break;
                case 187:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo186 = new RadioResponseInfo();
                        radioResponseInfo186.readFromParcel(hwParcel);
                        getColrResponse(radioResponseInfo186, hwParcel.readInt32());
                    }
                    break;
                case 188:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo187 = new RadioResponseInfo();
                        radioResponseInfo187.readFromParcel(hwParcel);
                        setColpResponse(radioResponseInfo187);
                    }
                    break;
                case 189:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo188 = new RadioResponseInfo();
                        radioResponseInfo188.readFromParcel(hwParcel);
                        setColrResponse(radioResponseInfo188);
                    }
                    break;
                case 190:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo189 = new RadioResponseInfo();
                        radioResponseInfo189.readFromParcel(hwParcel);
                        queryCallForwardInTimeSlotStatusResponse(radioResponseInfo189, CallForwardInfoEx.readVectorFromParcel(hwParcel));
                    }
                    break;
                case 191:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo190 = new RadioResponseInfo();
                        radioResponseInfo190.readFromParcel(hwParcel);
                        setCallForwardInTimeSlotResponse(radioResponseInfo190);
                    }
                    break;
                case 192:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo191 = new RadioResponseInfo();
                        radioResponseInfo191.readFromParcel(hwParcel);
                        runGbaAuthenticationResponse(radioResponseInfo191, hwParcel.readStringVector());
                    }
                    break;
                case 193:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo192 = new RadioResponseInfo();
                        radioResponseInfo192.readFromParcel(hwParcel);
                        setModemImsCfgResponse(radioResponseInfo192, hwParcel.readString());
                    }
                    break;
                case 194:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo193 = new RadioResponseInfo();
                        radioResponseInfo193.readFromParcel(hwParcel);
                        SendSmsResult sendSmsResult5 = new SendSmsResult();
                        sendSmsResult5.readFromParcel(hwParcel);
                        sendImsSmsExResponse(radioResponseInfo193, sendSmsResult5);
                    }
                    break;
                case 195:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo194 = new RadioResponseInfo();
                        radioResponseInfo194.readFromParcel(hwParcel);
                        acknowledgeLastIncomingGsmSmsExResponse(radioResponseInfo194);
                    }
                    break;
                case 196:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo195 = new RadioResponseInfo();
                        radioResponseInfo195.readFromParcel(hwParcel);
                        acknowledgeLastIncomingCdmaSmsExResponse(radioResponseInfo195);
                    }
                    break;
                case 197:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo196 = new RadioResponseInfo();
                        radioResponseInfo196.readFromParcel(hwParcel);
                        setImsBearerNotificationResponse(radioResponseInfo196);
                    }
                    break;
                case 198:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo197 = new RadioResponseInfo();
                        radioResponseInfo197.readFromParcel(hwParcel);
                        setImsCfgFeatureValueResponse(radioResponseInfo197);
                    }
                    break;
                case 199:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo198 = new RadioResponseInfo();
                        radioResponseInfo198.readFromParcel(hwParcel);
                        getImsCfgFeatureValueResponse(radioResponseInfo198, hwParcel.readInt32());
                    }
                    break;
                case 200:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo199 = new RadioResponseInfo();
                        radioResponseInfo199.readFromParcel(hwParcel);
                        setImsCfgProvisionValueResponse(radioResponseInfo199);
                    }
                    break;
                case 201:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo200 = new RadioResponseInfo();
                        radioResponseInfo200.readFromParcel(hwParcel);
                        getImsCfgProvisionValueResponse(radioResponseInfo200, hwParcel.readString());
                    }
                    break;
                case ExternalSimConstants.EVENT_TYPE_RECEIVE_RSIM_AUTH_RSP:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo201 = new RadioResponseInfo();
                        radioResponseInfo201.readFromParcel(hwParcel);
                        setImsCfgResourceCapValueResponse(radioResponseInfo201);
                    }
                    break;
                case ExternalSimConstants.EVENT_TYPE_RSIM_AUTH_DONE:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioResponse.kInterfaceName);
                        RadioResponseInfo radioResponseInfo202 = new RadioResponseInfo();
                        radioResponseInfo202.readFromParcel(hwParcel);
                        getImsCfgResourceCapValueResponse(radioResponseInfo202, hwParcel.readInt32());
                    }
                    break;
                default:
                    switch (i) {
                        case 256067662:
                            if ((i2 & 1) != 0) {
                                hwParcel2.writeStatus(Integer.MIN_VALUE);
                                hwParcel2.send();
                            } else {
                                hwParcel.enforceInterface(IBase.kInterfaceName);
                                ArrayList<String> arrayListInterfaceChain = interfaceChain();
                                hwParcel2.writeStatus(0);
                                hwParcel2.writeStringVector(arrayListInterfaceChain);
                                hwParcel2.send();
                            }
                            break;
                        case 256131655:
                            if ((i2 & 1) != 0) {
                                hwParcel2.writeStatus(Integer.MIN_VALUE);
                                hwParcel2.send();
                            } else {
                                hwParcel.enforceInterface(IBase.kInterfaceName);
                                hwParcel2.writeStatus(0);
                                hwParcel2.send();
                            }
                            break;
                        case 256136003:
                            if ((i2 & 1) != 0) {
                                hwParcel2.writeStatus(Integer.MIN_VALUE);
                                hwParcel2.send();
                            } else {
                                hwParcel.enforceInterface(IBase.kInterfaceName);
                                String strInterfaceDescriptor = interfaceDescriptor();
                                hwParcel2.writeStatus(0);
                                hwParcel2.writeString(strInterfaceDescriptor);
                                hwParcel2.send();
                            }
                            break;
                        case 256398152:
                            if ((i2 & 1) != 0) {
                                hwParcel2.writeStatus(Integer.MIN_VALUE);
                                hwParcel2.send();
                            } else {
                                hwParcel.enforceInterface(IBase.kInterfaceName);
                                ArrayList<byte[]> hashChain = getHashChain();
                                hwParcel2.writeStatus(0);
                                HwBlob hwBlob = new HwBlob(16);
                                int size = hashChain.size();
                                hwBlob.putInt32(8L, size);
                                hwBlob.putBool(12L, false);
                                HwBlob hwBlob2 = new HwBlob(size * 32);
                                while (i < size) {
                                    hwBlob2.putInt8Array(i * 32, hashChain.get(i));
                                    i++;
                                }
                                hwBlob.putBlob(0L, hwBlob2);
                                hwParcel2.writeBuffer(hwBlob);
                                hwParcel2.send();
                            }
                            break;
                        case 256462420:
                            if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                                hwParcel2.writeStatus(Integer.MIN_VALUE);
                                hwParcel2.send();
                            } else {
                                hwParcel.enforceInterface(IBase.kInterfaceName);
                                setHALInstrumentation();
                            }
                            break;
                        case 256660548:
                            if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                                hwParcel2.writeStatus(Integer.MIN_VALUE);
                                hwParcel2.send();
                            }
                            break;
                        case 256921159:
                            if ((i2 & 1) != 0) {
                                hwParcel2.writeStatus(Integer.MIN_VALUE);
                                hwParcel2.send();
                            } else {
                                hwParcel.enforceInterface(IBase.kInterfaceName);
                                ping();
                                hwParcel2.writeStatus(0);
                                hwParcel2.send();
                            }
                            break;
                        case 257049926:
                            if ((i2 & 1) != 0) {
                                hwParcel2.writeStatus(Integer.MIN_VALUE);
                                hwParcel2.send();
                            } else {
                                hwParcel.enforceInterface(IBase.kInterfaceName);
                                DebugInfo debugInfo = getDebugInfo();
                                hwParcel2.writeStatus(0);
                                debugInfo.writeToParcel(hwParcel2);
                                hwParcel2.send();
                            }
                            break;
                        case 257120595:
                            if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                                hwParcel2.writeStatus(Integer.MIN_VALUE);
                                hwParcel2.send();
                            } else {
                                hwParcel.enforceInterface(IBase.kInterfaceName);
                                notifySyspropsChanged();
                            }
                            break;
                        case 257250372:
                            if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                                hwParcel2.writeStatus(Integer.MIN_VALUE);
                                hwParcel2.send();
                            }
                            break;
                    }
                    break;
            }
        }
    }
}
