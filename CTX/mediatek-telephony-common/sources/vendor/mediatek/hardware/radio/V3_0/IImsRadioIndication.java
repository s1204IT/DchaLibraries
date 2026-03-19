package vendor.mediatek.hardware.radio.V3_0;

import android.hardware.radio.V1_0.CdmaCallWaiting;
import android.hardware.radio.V1_0.CdmaInformationRecords;
import android.hardware.radio.V1_0.CdmaSignalInfoRecord;
import android.hardware.radio.V1_0.CdmaSmsMessage;
import android.hardware.radio.V1_0.CellInfo;
import android.hardware.radio.V1_0.HardwareConfig;
import android.hardware.radio.V1_0.LceDataInfo;
import android.hardware.radio.V1_0.NvItem;
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
import android.hidl.base.V1_0.DebugInfo;
import android.hidl.base.V1_0.IBase;
import android.os.HidlSupport;
import android.os.HwBinder;
import android.os.HwBlob;
import android.os.HwParcel;
import android.os.IHwBinder;
import android.os.IHwInterface;
import android.os.RemoteException;
import com.mediatek.internal.telephony.cat.BipUtils;
import com.mediatek.internal.telephony.ppl.PplControlData;
import com.mediatek.internal.telephony.worldphone.IWorldPhone;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;

public interface IImsRadioIndication extends android.hardware.radio.V1_2.IRadioIndication {
    public static final String kInterfaceName = "vendor.mediatek.hardware.radio@3.0::IImsRadioIndication";

    @Override
    IHwBinder asBinder();

    void callInfoIndication(int i, ArrayList<String> arrayList) throws RemoteException;

    void callmodChangeIndicator(int i, String str, String str2, String str3, String str4, String str5) throws RemoteException;

    void cdmaNewSmsEx(int i, CdmaSmsMessage cdmaSmsMessage) throws RemoteException;

    void econfResultIndication(int i, String str, String str2, String str3, String str4, String str5, String str6) throws RemoteException;

    void ectIndication(int i, int i2, int i3, int i4) throws RemoteException;

    @Override
    DebugInfo getDebugInfo() throws RemoteException;

    @Override
    ArrayList<byte[]> getHashChain() throws RemoteException;

    void getProvisionDone(int i, String str, String str2) throws RemoteException;

    void imsBearerActivation(int i, int i2, String str) throws RemoteException;

    void imsBearerDeactivation(int i, int i2, String str) throws RemoteException;

    void imsBearerInit(int i) throws RemoteException;

    void imsCfgConfigChanged(int i, int i2, String str, String str2) throws RemoteException;

    void imsCfgConfigLoaded(int i) throws RemoteException;

    void imsCfgDynamicImsSwitchComplete(int i) throws RemoteException;

    void imsCfgFeatureChanged(int i, int i2, int i3, int i4) throws RemoteException;

    void imsConferenceInfoIndication(int i, ArrayList<ImsConfParticipant> arrayList) throws RemoteException;

    void imsDataInfoNotify(int i, String str, String str2, String str3) throws RemoteException;

    void imsDeregDone(int i) throws RemoteException;

    void imsDialogIndication(int i, ArrayList<Dialog> arrayList) throws RemoteException;

    void imsDisableDone(int i) throws RemoteException;

    void imsDisableStart(int i) throws RemoteException;

    void imsEnableDone(int i) throws RemoteException;

    void imsEnableStart(int i) throws RemoteException;

    void imsEventPackageIndication(int i, String str, String str2, String str3, String str4, String str5) throws RemoteException;

    void imsRadioInfoChange(int i, String str, String str2) throws RemoteException;

    void imsRegistrationInfo(int i, int i2, int i3) throws RemoteException;

    void imsRtpInfo(int i, String str, String str2, String str3, String str4, String str5) throws RemoteException;

    void imsSupportEcc(int i, int i2) throws RemoteException;

    void incomingCallIndication(int i, IncomingCallNotification incomingCallNotification) throws RemoteException;

    @Override
    ArrayList<String> interfaceChain() throws RemoteException;

    @Override
    String interfaceDescriptor() throws RemoteException;

    @Override
    boolean linkToDeath(IHwBinder.DeathRecipient deathRecipient, long j) throws RemoteException;

    void lteMessageWaitingIndication(int i, String str, String str2, String str3, String str4, String str5) throws RemoteException;

    void multiImsCount(int i, int i2) throws RemoteException;

    void newSmsEx(int i, ArrayList<Byte> arrayList) throws RemoteException;

    void newSmsStatusReportEx(int i, ArrayList<Byte> arrayList) throws RemoteException;

    @Override
    void notifySyspropsChanged() throws RemoteException;

    void onUssi(int i, String str, String str2, String str3, String str4, String str5, String str6, String str7) throws RemoteException;

    void onVolteSubscription(int i, int i2) throws RemoteException;

    void onXui(int i, String str, String str2, String str3) throws RemoteException;

    @Override
    void ping() throws RemoteException;

    @Override
    void setHALInstrumentation() throws RemoteException;

    void sipCallProgressIndicator(int i, String str, String str2, String str3, String str4, String str5, String str6) throws RemoteException;

    void speechCodecInfoIndication(int i, int i2) throws RemoteException;

    @Override
    boolean unlinkToDeath(IHwBinder.DeathRecipient deathRecipient) throws RemoteException;

    void videoCapabilityIndicator(int i, String str, String str2, String str3) throws RemoteException;

    void volteSetting(int i, boolean z) throws RemoteException;

    static IImsRadioIndication asInterface(IHwBinder iHwBinder) {
        if (iHwBinder == null) {
            return null;
        }
        IHwInterface iHwInterfaceQueryLocalInterface = iHwBinder.queryLocalInterface(kInterfaceName);
        if (iHwInterfaceQueryLocalInterface != null && (iHwInterfaceQueryLocalInterface instanceof IImsRadioIndication)) {
            return (IImsRadioIndication) iHwInterfaceQueryLocalInterface;
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

    static IImsRadioIndication castFrom(IHwInterface iHwInterface) {
        if (iHwInterface == null) {
            return null;
        }
        return asInterface(iHwInterface.asBinder());
    }

    static IImsRadioIndication getService(String str, boolean z) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, str, z));
    }

    static IImsRadioIndication getService(boolean z) throws RemoteException {
        return getService("default", z);
    }

    static IImsRadioIndication getService(String str) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, str));
    }

    static IImsRadioIndication getService() throws RemoteException {
        return getService("default");
    }

    public static final class Proxy implements IImsRadioIndication {
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
                return "[class or subclass of vendor.mediatek.hardware.radio@3.0::IImsRadioIndication]@Proxy";
            }
        }

        public final boolean equals(Object obj) {
            return HidlSupport.interfacesEqual(this, obj);
        }

        public final int hashCode() {
            return asBinder().hashCode();
        }

        @Override
        public void radioStateChanged(int i, int i2) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeInt32(i2);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(1, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void callStateChanged(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
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
        public void networkStateChanged(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
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
        public void newSms(int i, ArrayList<Byte> arrayList) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeInt8Vector(arrayList);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(4, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void newSmsStatusReport(int i, ArrayList<Byte> arrayList) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeInt8Vector(arrayList);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(5, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void newSmsOnSim(int i, int i2) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeInt32(i2);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(6, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void onUssd(int i, int i2, String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeInt32(i2);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(7, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void nitzTimeReceived(int i, String str, long j) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeString(str);
            hwParcel.writeInt64(j);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(8, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void currentSignalStrength(int i, SignalStrength signalStrength) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            signalStrength.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(9, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void dataCallListChanged(int i, ArrayList<SetupDataCallResult> arrayList) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            SetupDataCallResult.writeVectorToParcel(hwParcel, arrayList);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(10, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void suppSvcNotify(int i, SuppSvcNotification suppSvcNotification) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            suppSvcNotification.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(11, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void stkSessionEnd(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(12, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void stkProactiveCommand(int i, String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(13, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void stkEventNotify(int i, String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(14, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void stkCallSetup(int i, long j) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeInt64(j);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(15, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void simSmsStorageFull(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(16, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void simRefresh(int i, SimRefreshResult simRefreshResult) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            simRefreshResult.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(17, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void callRing(int i, boolean z, CdmaSignalInfoRecord cdmaSignalInfoRecord) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeBool(z);
            cdmaSignalInfoRecord.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(18, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void simStatusChanged(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(19, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void cdmaNewSms(int i, CdmaSmsMessage cdmaSmsMessage) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            cdmaSmsMessage.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(20, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void newBroadcastSms(int i, ArrayList<Byte> arrayList) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeInt8Vector(arrayList);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(21, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void cdmaRuimSmsStorageFull(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(22, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void restrictedStateChanged(int i, int i2) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeInt32(i2);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(23, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void enterEmergencyCallbackMode(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(24, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void cdmaCallWaiting(int i, CdmaCallWaiting cdmaCallWaiting) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            cdmaCallWaiting.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(25, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void cdmaOtaProvisionStatus(int i, int i2) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeInt32(i2);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(26, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void cdmaInfoRec(int i, CdmaInformationRecords cdmaInformationRecords) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            cdmaInformationRecords.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(27, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void indicateRingbackTone(int i, boolean z) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeBool(z);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(28, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void resendIncallMute(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(29, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void cdmaSubscriptionSourceChanged(int i, int i2) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeInt32(i2);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(30, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void cdmaPrlChanged(int i, int i2) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
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
        public void exitEmergencyCallbackMode(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(32, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void rilConnected(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(33, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void voiceRadioTechChanged(int i, int i2) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeInt32(i2);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(34, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void cellInfoList(int i, ArrayList<CellInfo> arrayList) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            CellInfo.writeVectorToParcel(hwParcel, arrayList);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(35, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void imsNetworkStateChanged(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(36, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void subscriptionStatusChanged(int i, boolean z) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeBool(z);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(37, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void srvccStateNotify(int i, int i2) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeInt32(i2);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(38, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void hardwareConfigChanged(int i, ArrayList<HardwareConfig> arrayList) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            HardwareConfig.writeVectorToParcel(hwParcel, arrayList);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(39, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void radioCapabilityIndication(int i, RadioCapability radioCapability) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            radioCapability.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(40, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void onSupplementaryServiceIndication(int i, StkCcUnsolSsResult stkCcUnsolSsResult) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            stkCcUnsolSsResult.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(41, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void stkCallControlAlphaNotify(int i, String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(42, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void lceData(int i, LceDataInfo lceDataInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            lceDataInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(43, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void pcoData(int i, PcoDataInfo pcoDataInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            pcoDataInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(44, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void modemReset(int i, String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(45, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void carrierInfoForImsiEncryption(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_1.IRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(46, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void networkScanResult(int i, NetworkScanResult networkScanResult) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_1.IRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            networkScanResult.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(47, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void keepaliveStatus(int i, KeepaliveStatus keepaliveStatus) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_1.IRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            keepaliveStatus.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(48, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void networkScanResult_1_2(int i, android.hardware.radio.V1_2.NetworkScanResult networkScanResult) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_2.IRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            networkScanResult.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(49, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void cellInfoList_1_2(int i, ArrayList<android.hardware.radio.V1_2.CellInfo> arrayList) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_2.IRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            android.hardware.radio.V1_2.CellInfo.writeVectorToParcel(hwParcel, arrayList);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(50, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void currentLinkCapacityEstimate(int i, LinkCapacityEstimate linkCapacityEstimate) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_2.IRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            linkCapacityEstimate.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(51, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void currentPhysicalChannelConfigs(int i, ArrayList<PhysicalChannelConfig> arrayList) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_2.IRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            PhysicalChannelConfig.writeVectorToParcel(hwParcel, arrayList);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(52, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void currentSignalStrength_1_2(int i, android.hardware.radio.V1_2.SignalStrength signalStrength) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_2.IRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            signalStrength.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(53, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void incomingCallIndication(int i, IncomingCallNotification incomingCallNotification) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            incomingCallNotification.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(54, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void callInfoIndication(int i, ArrayList<String> arrayList) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeStringVector(arrayList);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(55, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void econfResultIndication(int i, String str, String str2, String str3, String str4, String str5, String str6) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeString(str);
            hwParcel.writeString(str2);
            hwParcel.writeString(str3);
            hwParcel.writeString(str4);
            hwParcel.writeString(str5);
            hwParcel.writeString(str6);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(56, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void sipCallProgressIndicator(int i, String str, String str2, String str3, String str4, String str5, String str6) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeString(str);
            hwParcel.writeString(str2);
            hwParcel.writeString(str3);
            hwParcel.writeString(str4);
            hwParcel.writeString(str5);
            hwParcel.writeString(str6);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(57, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void callmodChangeIndicator(int i, String str, String str2, String str3, String str4, String str5) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeString(str);
            hwParcel.writeString(str2);
            hwParcel.writeString(str3);
            hwParcel.writeString(str4);
            hwParcel.writeString(str5);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(58, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void videoCapabilityIndicator(int i, String str, String str2, String str3) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeString(str);
            hwParcel.writeString(str2);
            hwParcel.writeString(str3);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(59, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void onUssi(int i, String str, String str2, String str3, String str4, String str5, String str6, String str7) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeString(str);
            hwParcel.writeString(str2);
            hwParcel.writeString(str3);
            hwParcel.writeString(str4);
            hwParcel.writeString(str5);
            hwParcel.writeString(str6);
            hwParcel.writeString(str7);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(60, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getProvisionDone(int i, String str, String str2) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeString(str);
            hwParcel.writeString(str2);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(61, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void imsRtpInfo(int i, String str, String str2, String str3, String str4, String str5) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeString(str);
            hwParcel.writeString(str2);
            hwParcel.writeString(str3);
            hwParcel.writeString(str4);
            hwParcel.writeString(str5);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(62, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void onXui(int i, String str, String str2, String str3) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeString(str);
            hwParcel.writeString(str2);
            hwParcel.writeString(str3);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(63, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void onVolteSubscription(int i, int i2) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeInt32(i2);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(64, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void imsEventPackageIndication(int i, String str, String str2, String str3, String str4, String str5) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeString(str);
            hwParcel.writeString(str2);
            hwParcel.writeString(str3);
            hwParcel.writeString(str4);
            hwParcel.writeString(str5);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(65, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void imsRegistrationInfo(int i, int i2, int i3) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeInt32(i2);
            hwParcel.writeInt32(i3);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(66, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void imsEnableDone(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(67, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void imsDisableDone(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(68, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void imsEnableStart(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(69, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void imsDisableStart(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioIndication.kInterfaceName);
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
        public void ectIndication(int i, int i2, int i3, int i4) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeInt32(i2);
            hwParcel.writeInt32(i3);
            hwParcel.writeInt32(i4);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(71, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void volteSetting(int i, boolean z) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeBool(z);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(72, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void imsBearerActivation(int i, int i2, String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeInt32(i2);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(73, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void imsBearerDeactivation(int i, int i2, String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeInt32(i2);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(74, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void imsBearerInit(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(75, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void imsDeregDone(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(76, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void multiImsCount(int i, int i2) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeInt32(i2);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(77, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void imsSupportEcc(int i, int i2) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeInt32(i2);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(78, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void imsRadioInfoChange(int i, String str, String str2) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeString(str);
            hwParcel.writeString(str2);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(79, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void speechCodecInfoIndication(int i, int i2) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeInt32(i2);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(80, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void imsConferenceInfoIndication(int i, ArrayList<ImsConfParticipant> arrayList) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            ImsConfParticipant.writeVectorToParcel(hwParcel, arrayList);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(81, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void lteMessageWaitingIndication(int i, String str, String str2, String str3, String str4, String str5) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeString(str);
            hwParcel.writeString(str2);
            hwParcel.writeString(str3);
            hwParcel.writeString(str4);
            hwParcel.writeString(str5);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(82, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void imsDialogIndication(int i, ArrayList<Dialog> arrayList) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            Dialog.writeVectorToParcel(hwParcel, arrayList);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(83, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void imsCfgDynamicImsSwitchComplete(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(84, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void imsCfgFeatureChanged(int i, int i2, int i3, int i4) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeInt32(i2);
            hwParcel.writeInt32(i3);
            hwParcel.writeInt32(i4);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(85, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void imsCfgConfigChanged(int i, int i2, String str, String str2) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeInt32(i2);
            hwParcel.writeString(str);
            hwParcel.writeString(str2);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(86, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void imsCfgConfigLoaded(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(87, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void imsDataInfoNotify(int i, String str, String str2, String str3) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeString(str);
            hwParcel.writeString(str2);
            hwParcel.writeString(str3);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(88, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void newSmsEx(int i, ArrayList<Byte> arrayList) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeInt8Vector(arrayList);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(89, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void newSmsStatusReportEx(int i, ArrayList<Byte> arrayList) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeInt8Vector(arrayList);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(90, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void cdmaNewSmsEx(int i, CdmaSmsMessage cdmaSmsMessage) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IImsRadioIndication.kInterfaceName);
            hwParcel.writeInt32(i);
            cdmaSmsMessage.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(91, hwParcel, hwParcel2, 1);
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

    public static abstract class Stub extends HwBinder implements IImsRadioIndication {
        @Override
        public IHwBinder asBinder() {
            return this;
        }

        @Override
        public final ArrayList<String> interfaceChain() {
            return new ArrayList<>(Arrays.asList(IImsRadioIndication.kInterfaceName, android.hardware.radio.V1_2.IRadioIndication.kInterfaceName, android.hardware.radio.V1_1.IRadioIndication.kInterfaceName, android.hardware.radio.V1_0.IRadioIndication.kInterfaceName, IBase.kInterfaceName));
        }

        @Override
        public final String interfaceDescriptor() {
            return IImsRadioIndication.kInterfaceName;
        }

        @Override
        public final ArrayList<byte[]> getHashChain() {
            return new ArrayList<>(Arrays.asList(new byte[]{73, 71, -84, -7, -87, -33, 103, 82, -94, -3, 25, -125, 20, 86, 89, -26, -120, -41, -81, -95, -59, 35, 122, 82, -25, -34, 0, 44, -63, -15, -99, -68}, new byte[]{-51, -89, 82, -82, -85, -86, -68, 32, 72, 106, -126, -84, 87, -93, -35, PplControlData.STATUS_WIPE_REQUESTED, 119, -123, -64, 6, 9, 74, 52, -101, -59, -30, 36, -24, -86, 34, -95, 124}, new byte[]{-4, -59, -56, -56, -117, -123, -87, -10, 63, -70, 103, -39, -26, 116, -38, 70, 108, 114, -87, -116, -94, -121, -13, 67, -5, 87, 33, -48, -104, 113, 63, -122}, new byte[]{92, -114, -5, -71, -60, 81, -91, -105, 55, -19, 44, 108, 32, 35, 10, -82, 71, 69, -125, -100, -96, 29, BipUtils.TCP_STATUS_ESTABLISHED, -120, -42, -36, -55, 2, 14, 82, -46, -59}, new byte[]{-67, -38, -74, 24, 77, 122, 52, 109, -90, -96, 125, -64, -126, -116, -15, -102, 105, 111, 76, -86, 54, 17, -59, 31, 46, 20, 86, 90, 20, -76, 15, -39}));
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
            if (IImsRadioIndication.kInterfaceName.equals(str)) {
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
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
                        radioStateChanged(hwParcel.readInt32(), hwParcel.readInt32());
                    }
                    break;
                case 2:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
                        callStateChanged(hwParcel.readInt32());
                    }
                    break;
                case 3:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
                        networkStateChanged(hwParcel.readInt32());
                    }
                    break;
                case 4:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
                        newSms(hwParcel.readInt32(), hwParcel.readInt8Vector());
                    }
                    break;
                case 5:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
                        newSmsStatusReport(hwParcel.readInt32(), hwParcel.readInt8Vector());
                    }
                    break;
                case 6:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
                        newSmsOnSim(hwParcel.readInt32(), hwParcel.readInt32());
                    }
                    break;
                case 7:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
                        onUssd(hwParcel.readInt32(), hwParcel.readInt32(), hwParcel.readString());
                    }
                    break;
                case 8:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
                        nitzTimeReceived(hwParcel.readInt32(), hwParcel.readString(), hwParcel.readInt64());
                    }
                    break;
                case 9:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
                        int int32 = hwParcel.readInt32();
                        SignalStrength signalStrength = new SignalStrength();
                        signalStrength.readFromParcel(hwParcel);
                        currentSignalStrength(int32, signalStrength);
                    }
                    break;
                case 10:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
                        dataCallListChanged(hwParcel.readInt32(), SetupDataCallResult.readVectorFromParcel(hwParcel));
                    }
                    break;
                case 11:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
                        int int322 = hwParcel.readInt32();
                        SuppSvcNotification suppSvcNotification = new SuppSvcNotification();
                        suppSvcNotification.readFromParcel(hwParcel);
                        suppSvcNotify(int322, suppSvcNotification);
                    }
                    break;
                case 12:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
                        stkSessionEnd(hwParcel.readInt32());
                    }
                    break;
                case 13:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
                        stkProactiveCommand(hwParcel.readInt32(), hwParcel.readString());
                    }
                    break;
                case 14:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
                        stkEventNotify(hwParcel.readInt32(), hwParcel.readString());
                    }
                    break;
                case 15:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
                        stkCallSetup(hwParcel.readInt32(), hwParcel.readInt64());
                    }
                    break;
                case 16:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
                        simSmsStorageFull(hwParcel.readInt32());
                    }
                    break;
                case 17:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
                        int int323 = hwParcel.readInt32();
                        SimRefreshResult simRefreshResult = new SimRefreshResult();
                        simRefreshResult.readFromParcel(hwParcel);
                        simRefresh(int323, simRefreshResult);
                    }
                    break;
                case 18:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
                        int int324 = hwParcel.readInt32();
                        boolean bool = hwParcel.readBool();
                        CdmaSignalInfoRecord cdmaSignalInfoRecord = new CdmaSignalInfoRecord();
                        cdmaSignalInfoRecord.readFromParcel(hwParcel);
                        callRing(int324, bool, cdmaSignalInfoRecord);
                    }
                    break;
                case 19:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
                        simStatusChanged(hwParcel.readInt32());
                    }
                    break;
                case 20:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
                        int int325 = hwParcel.readInt32();
                        CdmaSmsMessage cdmaSmsMessage = new CdmaSmsMessage();
                        cdmaSmsMessage.readFromParcel(hwParcel);
                        cdmaNewSms(int325, cdmaSmsMessage);
                    }
                    break;
                case 21:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
                        newBroadcastSms(hwParcel.readInt32(), hwParcel.readInt8Vector());
                    }
                    break;
                case 22:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
                        cdmaRuimSmsStorageFull(hwParcel.readInt32());
                    }
                    break;
                case 23:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
                        restrictedStateChanged(hwParcel.readInt32(), hwParcel.readInt32());
                    }
                    break;
                case 24:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
                        enterEmergencyCallbackMode(hwParcel.readInt32());
                    }
                    break;
                case 25:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
                        int int326 = hwParcel.readInt32();
                        CdmaCallWaiting cdmaCallWaiting = new CdmaCallWaiting();
                        cdmaCallWaiting.readFromParcel(hwParcel);
                        cdmaCallWaiting(int326, cdmaCallWaiting);
                    }
                    break;
                case 26:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
                        cdmaOtaProvisionStatus(hwParcel.readInt32(), hwParcel.readInt32());
                    }
                    break;
                case 27:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
                        int int327 = hwParcel.readInt32();
                        CdmaInformationRecords cdmaInformationRecords = new CdmaInformationRecords();
                        cdmaInformationRecords.readFromParcel(hwParcel);
                        cdmaInfoRec(int327, cdmaInformationRecords);
                    }
                    break;
                case 28:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
                        indicateRingbackTone(hwParcel.readInt32(), hwParcel.readBool());
                    }
                    break;
                case 29:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
                        resendIncallMute(hwParcel.readInt32());
                    }
                    break;
                case 30:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
                        cdmaSubscriptionSourceChanged(hwParcel.readInt32(), hwParcel.readInt32());
                    }
                    break;
                case 31:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
                        cdmaPrlChanged(hwParcel.readInt32(), hwParcel.readInt32());
                    }
                    break;
                case 32:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
                        exitEmergencyCallbackMode(hwParcel.readInt32());
                    }
                    break;
                case 33:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
                        rilConnected(hwParcel.readInt32());
                    }
                    break;
                case 34:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
                        voiceRadioTechChanged(hwParcel.readInt32(), hwParcel.readInt32());
                    }
                    break;
                case 35:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
                        cellInfoList(hwParcel.readInt32(), CellInfo.readVectorFromParcel(hwParcel));
                    }
                    break;
                case 36:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
                        imsNetworkStateChanged(hwParcel.readInt32());
                    }
                    break;
                case 37:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
                        subscriptionStatusChanged(hwParcel.readInt32(), hwParcel.readBool());
                    }
                    break;
                case 38:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
                        srvccStateNotify(hwParcel.readInt32(), hwParcel.readInt32());
                    }
                    break;
                case 39:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
                        hardwareConfigChanged(hwParcel.readInt32(), HardwareConfig.readVectorFromParcel(hwParcel));
                    }
                    break;
                case 40:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
                        int int328 = hwParcel.readInt32();
                        RadioCapability radioCapability = new RadioCapability();
                        radioCapability.readFromParcel(hwParcel);
                        radioCapabilityIndication(int328, radioCapability);
                    }
                    break;
                case 41:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
                        int int329 = hwParcel.readInt32();
                        StkCcUnsolSsResult stkCcUnsolSsResult = new StkCcUnsolSsResult();
                        stkCcUnsolSsResult.readFromParcel(hwParcel);
                        onSupplementaryServiceIndication(int329, stkCcUnsolSsResult);
                    }
                    break;
                case 42:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
                        stkCallControlAlphaNotify(hwParcel.readInt32(), hwParcel.readString());
                    }
                    break;
                case 43:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
                        int int3210 = hwParcel.readInt32();
                        LceDataInfo lceDataInfo = new LceDataInfo();
                        lceDataInfo.readFromParcel(hwParcel);
                        lceData(int3210, lceDataInfo);
                    }
                    break;
                case 44:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
                        int int3211 = hwParcel.readInt32();
                        PcoDataInfo pcoDataInfo = new PcoDataInfo();
                        pcoDataInfo.readFromParcel(hwParcel);
                        pcoData(int3211, pcoDataInfo);
                    }
                    break;
                case 45:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadioIndication.kInterfaceName);
                        modemReset(hwParcel.readInt32(), hwParcel.readString());
                    }
                    break;
                case 46:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_1.IRadioIndication.kInterfaceName);
                        carrierInfoForImsiEncryption(hwParcel.readInt32());
                    }
                    break;
                case 47:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_1.IRadioIndication.kInterfaceName);
                        int int3212 = hwParcel.readInt32();
                        NetworkScanResult networkScanResult = new NetworkScanResult();
                        networkScanResult.readFromParcel(hwParcel);
                        networkScanResult(int3212, networkScanResult);
                    }
                    break;
                case 48:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_1.IRadioIndication.kInterfaceName);
                        int int3213 = hwParcel.readInt32();
                        KeepaliveStatus keepaliveStatus = new KeepaliveStatus();
                        keepaliveStatus.readFromParcel(hwParcel);
                        keepaliveStatus(int3213, keepaliveStatus);
                    }
                    break;
                case 49:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_2.IRadioIndication.kInterfaceName);
                        int int3214 = hwParcel.readInt32();
                        android.hardware.radio.V1_2.NetworkScanResult networkScanResult2 = new android.hardware.radio.V1_2.NetworkScanResult();
                        networkScanResult2.readFromParcel(hwParcel);
                        networkScanResult_1_2(int3214, networkScanResult2);
                    }
                    break;
                case 50:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_2.IRadioIndication.kInterfaceName);
                        cellInfoList_1_2(hwParcel.readInt32(), android.hardware.radio.V1_2.CellInfo.readVectorFromParcel(hwParcel));
                    }
                    break;
                case 51:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_2.IRadioIndication.kInterfaceName);
                        int int3215 = hwParcel.readInt32();
                        LinkCapacityEstimate linkCapacityEstimate = new LinkCapacityEstimate();
                        linkCapacityEstimate.readFromParcel(hwParcel);
                        currentLinkCapacityEstimate(int3215, linkCapacityEstimate);
                    }
                    break;
                case 52:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_2.IRadioIndication.kInterfaceName);
                        currentPhysicalChannelConfigs(hwParcel.readInt32(), PhysicalChannelConfig.readVectorFromParcel(hwParcel));
                    }
                    break;
                case 53:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_2.IRadioIndication.kInterfaceName);
                        int int3216 = hwParcel.readInt32();
                        android.hardware.radio.V1_2.SignalStrength signalStrength2 = new android.hardware.radio.V1_2.SignalStrength();
                        signalStrength2.readFromParcel(hwParcel);
                        currentSignalStrength_1_2(int3216, signalStrength2);
                    }
                    break;
                case 54:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioIndication.kInterfaceName);
                        int int3217 = hwParcel.readInt32();
                        IncomingCallNotification incomingCallNotification = new IncomingCallNotification();
                        incomingCallNotification.readFromParcel(hwParcel);
                        incomingCallIndication(int3217, incomingCallNotification);
                    }
                    break;
                case 55:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioIndication.kInterfaceName);
                        callInfoIndication(hwParcel.readInt32(), hwParcel.readStringVector());
                    }
                    break;
                case 56:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioIndication.kInterfaceName);
                        econfResultIndication(hwParcel.readInt32(), hwParcel.readString(), hwParcel.readString(), hwParcel.readString(), hwParcel.readString(), hwParcel.readString(), hwParcel.readString());
                    }
                    break;
                case 57:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioIndication.kInterfaceName);
                        sipCallProgressIndicator(hwParcel.readInt32(), hwParcel.readString(), hwParcel.readString(), hwParcel.readString(), hwParcel.readString(), hwParcel.readString(), hwParcel.readString());
                    }
                    break;
                case 58:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioIndication.kInterfaceName);
                        callmodChangeIndicator(hwParcel.readInt32(), hwParcel.readString(), hwParcel.readString(), hwParcel.readString(), hwParcel.readString(), hwParcel.readString());
                    }
                    break;
                case 59:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioIndication.kInterfaceName);
                        videoCapabilityIndicator(hwParcel.readInt32(), hwParcel.readString(), hwParcel.readString(), hwParcel.readString());
                    }
                    break;
                case 60:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioIndication.kInterfaceName);
                        onUssi(hwParcel.readInt32(), hwParcel.readString(), hwParcel.readString(), hwParcel.readString(), hwParcel.readString(), hwParcel.readString(), hwParcel.readString(), hwParcel.readString());
                    }
                    break;
                case 61:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioIndication.kInterfaceName);
                        getProvisionDone(hwParcel.readInt32(), hwParcel.readString(), hwParcel.readString());
                    }
                    break;
                case 62:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioIndication.kInterfaceName);
                        imsRtpInfo(hwParcel.readInt32(), hwParcel.readString(), hwParcel.readString(), hwParcel.readString(), hwParcel.readString(), hwParcel.readString());
                    }
                    break;
                case 63:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioIndication.kInterfaceName);
                        onXui(hwParcel.readInt32(), hwParcel.readString(), hwParcel.readString(), hwParcel.readString());
                    }
                    break;
                case 64:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioIndication.kInterfaceName);
                        onVolteSubscription(hwParcel.readInt32(), hwParcel.readInt32());
                    }
                    break;
                case 65:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioIndication.kInterfaceName);
                        imsEventPackageIndication(hwParcel.readInt32(), hwParcel.readString(), hwParcel.readString(), hwParcel.readString(), hwParcel.readString(), hwParcel.readString());
                    }
                    break;
                case 66:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioIndication.kInterfaceName);
                        imsRegistrationInfo(hwParcel.readInt32(), hwParcel.readInt32(), hwParcel.readInt32());
                    }
                    break;
                case 67:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioIndication.kInterfaceName);
                        imsEnableDone(hwParcel.readInt32());
                    }
                    break;
                case 68:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioIndication.kInterfaceName);
                        imsDisableDone(hwParcel.readInt32());
                    }
                    break;
                case 69:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioIndication.kInterfaceName);
                        imsEnableStart(hwParcel.readInt32());
                    }
                    break;
                case 70:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioIndication.kInterfaceName);
                        imsDisableStart(hwParcel.readInt32());
                    }
                    break;
                case 71:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioIndication.kInterfaceName);
                        ectIndication(hwParcel.readInt32(), hwParcel.readInt32(), hwParcel.readInt32(), hwParcel.readInt32());
                    }
                    break;
                case 72:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioIndication.kInterfaceName);
                        volteSetting(hwParcel.readInt32(), hwParcel.readBool());
                    }
                    break;
                case 73:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioIndication.kInterfaceName);
                        imsBearerActivation(hwParcel.readInt32(), hwParcel.readInt32(), hwParcel.readString());
                    }
                    break;
                case NvItem.LTE_SCAN_PRIORITY_25:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioIndication.kInterfaceName);
                        imsBearerDeactivation(hwParcel.readInt32(), hwParcel.readInt32(), hwParcel.readString());
                    }
                    break;
                case NvItem.LTE_SCAN_PRIORITY_26:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioIndication.kInterfaceName);
                        imsBearerInit(hwParcel.readInt32());
                    }
                    break;
                case NvItem.LTE_SCAN_PRIORITY_41:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioIndication.kInterfaceName);
                        imsDeregDone(hwParcel.readInt32());
                    }
                    break;
                case NvItem.LTE_HIDDEN_BAND_PRIORITY_25:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioIndication.kInterfaceName);
                        multiImsCount(hwParcel.readInt32(), hwParcel.readInt32());
                    }
                    break;
                case NvItem.LTE_HIDDEN_BAND_PRIORITY_26:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioIndication.kInterfaceName);
                        imsSupportEcc(hwParcel.readInt32(), hwParcel.readInt32());
                    }
                    break;
                case 79:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioIndication.kInterfaceName);
                        imsRadioInfoChange(hwParcel.readInt32(), hwParcel.readString(), hwParcel.readString());
                    }
                    break;
                case IWorldPhone.EVENT_SERVICE_STATE_CHANGED_1:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioIndication.kInterfaceName);
                        speechCodecInfoIndication(hwParcel.readInt32(), hwParcel.readInt32());
                    }
                    break;
                case 81:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioIndication.kInterfaceName);
                        imsConferenceInfoIndication(hwParcel.readInt32(), ImsConfParticipant.readVectorFromParcel(hwParcel));
                    }
                    break;
                case IWorldPhone.EVENT_SERVICE_STATE_CHANGED_3:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioIndication.kInterfaceName);
                        lteMessageWaitingIndication(hwParcel.readInt32(), hwParcel.readString(), hwParcel.readString(), hwParcel.readString(), hwParcel.readString(), hwParcel.readString());
                    }
                    break;
                case IWorldPhone.EVENT_SERVICE_STATE_CHANGED_4:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioIndication.kInterfaceName);
                        imsDialogIndication(hwParcel.readInt32(), Dialog.readVectorFromParcel(hwParcel));
                    }
                    break;
                case 84:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioIndication.kInterfaceName);
                        imsCfgDynamicImsSwitchComplete(hwParcel.readInt32());
                    }
                    break;
                case 85:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioIndication.kInterfaceName);
                        imsCfgFeatureChanged(hwParcel.readInt32(), hwParcel.readInt32(), hwParcel.readInt32(), hwParcel.readInt32());
                    }
                    break;
                case 86:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioIndication.kInterfaceName);
                        imsCfgConfigChanged(hwParcel.readInt32(), hwParcel.readInt32(), hwParcel.readString(), hwParcel.readString());
                    }
                    break;
                case 87:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioIndication.kInterfaceName);
                        imsCfgConfigLoaded(hwParcel.readInt32());
                    }
                    break;
                case 88:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioIndication.kInterfaceName);
                        imsDataInfoNotify(hwParcel.readInt32(), hwParcel.readString(), hwParcel.readString(), hwParcel.readString());
                    }
                    break;
                case 89:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioIndication.kInterfaceName);
                        newSmsEx(hwParcel.readInt32(), hwParcel.readInt8Vector());
                    }
                    break;
                case 90:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioIndication.kInterfaceName);
                        newSmsStatusReportEx(hwParcel.readInt32(), hwParcel.readInt8Vector());
                    }
                    break;
                case 91:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IImsRadioIndication.kInterfaceName);
                        int int3218 = hwParcel.readInt32();
                        CdmaSmsMessage cdmaSmsMessage2 = new CdmaSmsMessage();
                        cdmaSmsMessage2.readFromParcel(hwParcel);
                        cdmaNewSmsEx(int3218, cdmaSmsMessage2);
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
