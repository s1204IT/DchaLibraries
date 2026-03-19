package android.hardware.radio.V1_2;

import android.hardware.radio.V1_0.CallForwardInfo;
import android.hardware.radio.V1_0.CarrierRestrictions;
import android.hardware.radio.V1_0.CdmaBroadcastSmsConfigInfo;
import android.hardware.radio.V1_0.CdmaSmsAck;
import android.hardware.radio.V1_0.CdmaSmsMessage;
import android.hardware.radio.V1_0.CdmaSmsWriteArgs;
import android.hardware.radio.V1_0.DataProfileInfo;
import android.hardware.radio.V1_0.Dial;
import android.hardware.radio.V1_0.GsmBroadcastSmsConfigInfo;
import android.hardware.radio.V1_0.GsmSmsMessage;
import android.hardware.radio.V1_0.IccIo;
import android.hardware.radio.V1_0.ImsSmsMessage;
import android.hardware.radio.V1_0.NvWriteItem;
import android.hardware.radio.V1_0.RadioCapability;
import android.hardware.radio.V1_0.SelectUiccSub;
import android.hardware.radio.V1_0.SimApdu;
import android.hardware.radio.V1_0.SmsWriteArgs;
import android.hardware.radio.V1_1.ImsiEncryptionInfo;
import android.hardware.radio.V1_1.KeepaliveRequest;
import android.hidl.base.V1_0.DebugInfo;
import android.hidl.base.V1_0.IBase;
import android.os.HidlSupport;
import android.os.HwBinder;
import android.os.HwBlob;
import android.os.HwParcel;
import android.os.IHwBinder;
import android.os.IHwInterface;
import android.os.RemoteException;
import com.android.internal.telephony.RadioNVItems;
import com.android.internal.telephony.nano.TelephonyProto;
import com.android.internal.telephony.uicc.AnswerToReset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;

public interface IRadio extends android.hardware.radio.V1_1.IRadio {
    public static final String kInterfaceName = "android.hardware.radio@1.2::IRadio";

    @Override
    IHwBinder asBinder();

    void deactivateDataCall_1_2(int i, int i2, int i3) throws RemoteException;

    @Override
    DebugInfo getDebugInfo() throws RemoteException;

    @Override
    ArrayList<byte[]> getHashChain() throws RemoteException;

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

    @Override
    void setHALInstrumentation() throws RemoteException;

    void setIndicationFilter_1_2(int i, int i2) throws RemoteException;

    void setLinkCapacityReportingCriteria(int i, int i2, int i3, int i4, ArrayList<Integer> arrayList, ArrayList<Integer> arrayList2, int i5) throws RemoteException;

    void setSignalStrengthReportingCriteria(int i, int i2, int i3, ArrayList<Integer> arrayList, int i4) throws RemoteException;

    void setupDataCall_1_2(int i, int i2, DataProfileInfo dataProfileInfo, boolean z, boolean z2, boolean z3, int i3, ArrayList<String> arrayList, ArrayList<String> arrayList2) throws RemoteException;

    void startNetworkScan_1_2(int i, NetworkScanRequest networkScanRequest) throws RemoteException;

    @Override
    boolean unlinkToDeath(IHwBinder.DeathRecipient deathRecipient) throws RemoteException;

    static IRadio asInterface(IHwBinder iHwBinder) {
        if (iHwBinder == null) {
            return null;
        }
        IHwInterface iHwInterfaceQueryLocalInterface = iHwBinder.queryLocalInterface(kInterfaceName);
        if (iHwInterfaceQueryLocalInterface != null && (iHwInterfaceQueryLocalInterface instanceof IRadio)) {
            return (IRadio) iHwInterfaceQueryLocalInterface;
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

    static IRadio castFrom(IHwInterface iHwInterface) {
        if (iHwInterface == null) {
            return null;
        }
        return asInterface(iHwInterface.asBinder());
    }

    static IRadio getService(String str, boolean z) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, str, z));
    }

    static IRadio getService(boolean z) throws RemoteException {
        return getService("default", z);
    }

    static IRadio getService(String str) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, str));
    }

    static IRadio getService() throws RemoteException {
        return getService("default");
    }

    public static final class Proxy implements IRadio {
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
                return "[class or subclass of android.hardware.radio@1.2::IRadio]@Proxy";
            }
        }

        public final boolean equals(Object obj) {
            return HidlSupport.interfacesEqual(this, obj);
        }

        public final int hashCode() {
            return asBinder().hashCode();
        }

        @Override
        public void setResponseFunctions(android.hardware.radio.V1_0.IRadioResponse iRadioResponse, android.hardware.radio.V1_0.IRadioIndication iRadioIndication) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeStrongBinder(iRadioResponse == null ? null : iRadioResponse.asBinder());
            hwParcel.writeStrongBinder(iRadioIndication != null ? iRadioIndication.asBinder() : null);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(1, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getIccCardStatus(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
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
        public void supplyIccPinForApp(int i, String str, String str2) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeString(str);
            hwParcel.writeString(str2);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(3, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void supplyIccPukForApp(int i, String str, String str2, String str3) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeString(str);
            hwParcel.writeString(str2);
            hwParcel.writeString(str3);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(4, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void supplyIccPin2ForApp(int i, String str, String str2) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeString(str);
            hwParcel.writeString(str2);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(5, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void supplyIccPuk2ForApp(int i, String str, String str2, String str3) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeString(str);
            hwParcel.writeString(str2);
            hwParcel.writeString(str3);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(6, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void changeIccPinForApp(int i, String str, String str2, String str3) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeString(str);
            hwParcel.writeString(str2);
            hwParcel.writeString(str3);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(7, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void changeIccPin2ForApp(int i, String str, String str2, String str3) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeString(str);
            hwParcel.writeString(str2);
            hwParcel.writeString(str3);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(8, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void supplyNetworkDepersonalization(int i, String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(9, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getCurrentCalls(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(10, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void dial(int i, Dial dial) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            dial.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(11, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getImsiForApp(int i, String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(12, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void hangup(int i, int i2) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeInt32(i2);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(13, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void hangupWaitingOrBackground(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(14, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void hangupForegroundResumeBackground(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(15, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void switchWaitingOrHoldingAndActive(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
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
        public void conference(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(17, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void rejectCall(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(18, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getLastCallFailCause(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
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
        public void getSignalStrength(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(20, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getVoiceRegistrationState(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(21, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getDataRegistrationState(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
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
        public void getOperator(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(23, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setRadioPower(int i, boolean z) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeBool(z);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(24, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void sendDtmf(int i, String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(25, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void sendSms(int i, GsmSmsMessage gsmSmsMessage) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            gsmSmsMessage.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(26, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void sendSMSExpectMore(int i, GsmSmsMessage gsmSmsMessage) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            gsmSmsMessage.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(27, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setupDataCall(int i, int i2, DataProfileInfo dataProfileInfo, boolean z, boolean z2, boolean z3) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeInt32(i2);
            dataProfileInfo.writeToParcel(hwParcel);
            hwParcel.writeBool(z);
            hwParcel.writeBool(z2);
            hwParcel.writeBool(z3);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(28, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void iccIOForApp(int i, IccIo iccIo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            iccIo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(29, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void sendUssd(int i, String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(30, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void cancelPendingUssd(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(31, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getClir(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
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
        public void setClir(int i, int i2) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeInt32(i2);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(33, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getCallForwardStatus(int i, CallForwardInfo callForwardInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            callForwardInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(34, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setCallForward(int i, CallForwardInfo callForwardInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            callForwardInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(35, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getCallWaiting(int i, int i2) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeInt32(i2);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(36, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setCallWaiting(int i, boolean z, int i2) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeBool(z);
            hwParcel.writeInt32(i2);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(37, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void acknowledgeLastIncomingGsmSms(int i, boolean z, int i2) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeBool(z);
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
        public void acceptCall(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(39, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void deactivateDataCall(int i, int i2, boolean z) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeInt32(i2);
            hwParcel.writeBool(z);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(40, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getFacilityLockForApp(int i, String str, String str2, int i2, String str3) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeString(str);
            hwParcel.writeString(str2);
            hwParcel.writeInt32(i2);
            hwParcel.writeString(str3);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(41, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setFacilityLockForApp(int i, String str, boolean z, String str2, int i2, String str3) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeString(str);
            hwParcel.writeBool(z);
            hwParcel.writeString(str2);
            hwParcel.writeInt32(i2);
            hwParcel.writeString(str3);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(42, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setBarringPassword(int i, String str, String str2, String str3) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeString(str);
            hwParcel.writeString(str2);
            hwParcel.writeString(str3);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(43, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getNetworkSelectionMode(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(44, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setNetworkSelectionModeAutomatic(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(45, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setNetworkSelectionModeManual(int i, String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(46, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getAvailableNetworks(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(47, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void startDtmf(int i, String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(48, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void stopDtmf(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(49, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getBasebandVersion(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(50, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void separateConnection(int i, int i2) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeInt32(i2);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(51, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setMute(int i, boolean z) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
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
        public void getMute(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
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
        public void getClip(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(54, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getDataCallList(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(55, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setSuppServiceNotifications(int i, boolean z) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeBool(z);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(56, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void writeSmsToSim(int i, SmsWriteArgs smsWriteArgs) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            smsWriteArgs.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(57, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void deleteSmsOnSim(int i, int i2) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeInt32(i2);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(58, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setBandMode(int i, int i2) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeInt32(i2);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(59, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getAvailableBandModes(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(60, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void sendEnvelope(int i, String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(61, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void sendTerminalResponseToSim(int i, String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(62, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void handleStkCallSetupRequestFromSim(int i, boolean z) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeBool(z);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(63, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void explicitCallTransfer(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(64, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setPreferredNetworkType(int i, int i2) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeInt32(i2);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(65, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getPreferredNetworkType(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(66, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getNeighboringCids(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
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
        public void setLocationUpdates(int i, boolean z) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeBool(z);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(68, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setCdmaSubscriptionSource(int i, int i2) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeInt32(i2);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(69, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setCdmaRoamingPreference(int i, int i2) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeInt32(i2);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(70, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getCdmaRoamingPreference(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(71, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setTTYMode(int i, int i2) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeInt32(i2);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(72, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getTTYMode(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(73, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setPreferredVoicePrivacy(int i, boolean z) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
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
        public void getPreferredVoicePrivacy(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
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
        public void sendCDMAFeatureCode(int i, String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(76, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void sendBurstDtmf(int i, String str, int i2, int i3) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeString(str);
            hwParcel.writeInt32(i2);
            hwParcel.writeInt32(i3);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(77, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void sendCdmaSms(int i, CdmaSmsMessage cdmaSmsMessage) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            cdmaSmsMessage.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(78, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void acknowledgeLastIncomingCdmaSms(int i, CdmaSmsAck cdmaSmsAck) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            cdmaSmsAck.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(79, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getGsmBroadcastConfig(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(80, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setGsmBroadcastConfig(int i, ArrayList<GsmBroadcastSmsConfigInfo> arrayList) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            GsmBroadcastSmsConfigInfo.writeVectorToParcel(hwParcel, arrayList);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(81, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setGsmBroadcastActivation(int i, boolean z) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeBool(z);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(82, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getCdmaBroadcastConfig(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(83, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setCdmaBroadcastConfig(int i, ArrayList<CdmaBroadcastSmsConfigInfo> arrayList) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            CdmaBroadcastSmsConfigInfo.writeVectorToParcel(hwParcel, arrayList);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(84, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setCdmaBroadcastActivation(int i, boolean z) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeBool(z);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(85, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getCDMASubscription(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
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
        public void writeSmsToRuim(int i, CdmaSmsWriteArgs cdmaSmsWriteArgs) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            cdmaSmsWriteArgs.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(87, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void deleteSmsOnRuim(int i, int i2) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeInt32(i2);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(88, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getDeviceIdentity(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(89, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void exitEmergencyCallbackMode(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(90, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getSmscAddress(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(91, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setSmscAddress(int i, String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(92, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void reportSmsMemoryStatus(int i, boolean z) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeBool(z);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(93, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void reportStkServiceIsRunning(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
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
        public void getCdmaSubscriptionSource(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(95, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void requestIsimAuthentication(int i, String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(96, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void acknowledgeIncomingGsmSmsWithPdu(int i, boolean z, String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeBool(z);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(97, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void sendEnvelopeWithStatus(int i, String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(98, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getVoiceRadioTechnology(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(99, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getCellInfoList(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(100, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setCellInfoListRate(int i, int i2) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeInt32(i2);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(101, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setInitialAttachApn(int i, DataProfileInfo dataProfileInfo, boolean z, boolean z2) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            dataProfileInfo.writeToParcel(hwParcel);
            hwParcel.writeBool(z);
            hwParcel.writeBool(z2);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(102, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getImsRegistrationState(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(103, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void sendImsSms(int i, ImsSmsMessage imsSmsMessage) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            imsSmsMessage.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(104, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void iccTransmitApduBasicChannel(int i, SimApdu simApdu) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            simApdu.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(105, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void iccOpenLogicalChannel(int i, String str, int i2) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeString(str);
            hwParcel.writeInt32(i2);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(106, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void iccCloseLogicalChannel(int i, int i2) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeInt32(i2);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(107, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void iccTransmitApduLogicalChannel(int i, SimApdu simApdu) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            simApdu.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(108, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void nvReadItem(int i, int i2) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeInt32(i2);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(109, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void nvWriteItem(int i, NvWriteItem nvWriteItem) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            nvWriteItem.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(110, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void nvWriteCdmaPrl(int i, ArrayList<Byte> arrayList) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeInt8Vector(arrayList);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(111, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void nvResetConfig(int i, int i2) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeInt32(i2);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(112, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setUiccSubscription(int i, SelectUiccSub selectUiccSub) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            selectUiccSub.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(113, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setDataAllowed(int i, boolean z) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeBool(z);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(114, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getHardwareConfig(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(115, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void requestIccSimAuthentication(int i, int i2, String str, String str2) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeInt32(i2);
            hwParcel.writeString(str);
            hwParcel.writeString(str2);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(116, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setDataProfile(int i, ArrayList<DataProfileInfo> arrayList, boolean z) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            DataProfileInfo.writeVectorToParcel(hwParcel, arrayList);
            hwParcel.writeBool(z);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(117, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void requestShutdown(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(118, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getRadioCapability(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(119, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setRadioCapability(int i, RadioCapability radioCapability) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            radioCapability.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(120, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void startLceService(int i, int i2, boolean z) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeInt32(i2);
            hwParcel.writeBool(z);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(121, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void stopLceService(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(122, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void pullLceData(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(123, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getModemActivityInfo(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
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
        public void setAllowedCarriers(int i, boolean z, CarrierRestrictions carrierRestrictions) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
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
        public void getAllowedCarriers(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(126, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void sendDeviceState(int i, int i2, boolean z) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeInt32(i2);
            hwParcel.writeBool(z);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(127, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setIndicationFilter(int i, int i2) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeInt32(i2);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(128, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setSimCardPower(int i, boolean z) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeBool(z);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(129, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void responseAcknowledgement() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_0.IRadio.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(130, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setCarrierInfoForImsiEncryption(int i, ImsiEncryptionInfo imsiEncryptionInfo) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_1.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            imsiEncryptionInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(131, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setSimCardPower_1_1(int i, int i2) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_1.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeInt32(i2);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(132, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void startNetworkScan(int i, android.hardware.radio.V1_1.NetworkScanRequest networkScanRequest) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_1.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            networkScanRequest.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(133, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void stopNetworkScan(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_1.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(134, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void startKeepalive(int i, KeepaliveRequest keepaliveRequest) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_1.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            keepaliveRequest.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(135, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void stopKeepalive(int i, int i2) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.radio.V1_1.IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeInt32(i2);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(136, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void startNetworkScan_1_2(int i, NetworkScanRequest networkScanRequest) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            networkScanRequest.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(137, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setIndicationFilter_1_2(int i, int i2) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeInt32(i2);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(138, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setSignalStrengthReportingCriteria(int i, int i2, int i3, ArrayList<Integer> arrayList, int i4) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeInt32(i2);
            hwParcel.writeInt32(i3);
            hwParcel.writeInt32Vector(arrayList);
            hwParcel.writeInt32(i4);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(139, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setLinkCapacityReportingCriteria(int i, int i2, int i3, int i4, ArrayList<Integer> arrayList, ArrayList<Integer> arrayList2, int i5) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeInt32(i2);
            hwParcel.writeInt32(i3);
            hwParcel.writeInt32(i4);
            hwParcel.writeInt32Vector(arrayList);
            hwParcel.writeInt32Vector(arrayList2);
            hwParcel.writeInt32(i5);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(140, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void setupDataCall_1_2(int i, int i2, DataProfileInfo dataProfileInfo, boolean z, boolean z2, boolean z3, int i3, ArrayList<String> arrayList, ArrayList<String> arrayList2) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeInt32(i2);
            dataProfileInfo.writeToParcel(hwParcel);
            hwParcel.writeBool(z);
            hwParcel.writeBool(z2);
            hwParcel.writeBool(z3);
            hwParcel.writeInt32(i3);
            hwParcel.writeStringVector(arrayList);
            hwParcel.writeStringVector(arrayList2);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(141, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void deactivateDataCall_1_2(int i, int i2, int i3) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IRadio.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeInt32(i2);
            hwParcel.writeInt32(i3);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(142, hwParcel, hwParcel2, 1);
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

    public static abstract class Stub extends HwBinder implements IRadio {
        @Override
        public IHwBinder asBinder() {
            return this;
        }

        @Override
        public final ArrayList<String> interfaceChain() {
            return new ArrayList<>(Arrays.asList(IRadio.kInterfaceName, android.hardware.radio.V1_1.IRadio.kInterfaceName, android.hardware.radio.V1_0.IRadio.kInterfaceName, IBase.kInterfaceName));
        }

        @Override
        public final String interfaceDescriptor() {
            return IRadio.kInterfaceName;
        }

        @Override
        public final ArrayList<byte[]> getHashChain() {
            return new ArrayList<>(Arrays.asList(new byte[]{-85, 19, 44, -103, 10, 98, -16, -84, -93, 88, 113, -64, -110, -62, 47, -71, -56, 93, 71, -114, 34, 18, 78, -10, -92, -48, -94, 48, 45, -89, 106, -97}, new byte[]{-9, -98, -33, 80, -93, 120, -87, -55, -69, 115, 127, -109, -14, 5, -38, -71, 27, 76, 99, -22, 73, 114, 58, -4, 111, -123, 108, 19, AnswerToReset.EUICC_SUPPORTED, 3, -22, -127}, new byte[]{-101, 90, -92, -103, -20, AnswerToReset.DIRECT_CONVENTION, 66, 38, -15, 95, 72, -11, -19, 8, -119, 110, 47, -64, 103, 111, -105, -116, -98, 25, -100, 29, -94, 29, -86, -16, 2, -90}, new byte[]{-67, -38, -74, 24, 77, 122, 52, 109, -90, -96, 125, -64, AnswerToReset.EUICC_SUPPORTED, -116, -15, -102, 105, 111, 76, -86, 54, 17, -59, 31, 46, 20, 86, 90, 20, -76, 15, -39}));
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
            if (IRadio.kInterfaceName.equals(str)) {
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
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        setResponseFunctions(android.hardware.radio.V1_0.IRadioResponse.asInterface(hwParcel.readStrongBinder()), android.hardware.radio.V1_0.IRadioIndication.asInterface(hwParcel.readStrongBinder()));
                        hwParcel2.writeStatus(0);
                        hwParcel2.send();
                    }
                    break;
                case 2:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        getIccCardStatus(hwParcel.readInt32());
                    }
                    break;
                case 3:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        supplyIccPinForApp(hwParcel.readInt32(), hwParcel.readString(), hwParcel.readString());
                    }
                    break;
                case 4:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        supplyIccPukForApp(hwParcel.readInt32(), hwParcel.readString(), hwParcel.readString(), hwParcel.readString());
                    }
                    break;
                case 5:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        supplyIccPin2ForApp(hwParcel.readInt32(), hwParcel.readString(), hwParcel.readString());
                    }
                    break;
                case 6:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        supplyIccPuk2ForApp(hwParcel.readInt32(), hwParcel.readString(), hwParcel.readString(), hwParcel.readString());
                    }
                    break;
                case 7:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        changeIccPinForApp(hwParcel.readInt32(), hwParcel.readString(), hwParcel.readString(), hwParcel.readString());
                    }
                    break;
                case 8:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        changeIccPin2ForApp(hwParcel.readInt32(), hwParcel.readString(), hwParcel.readString(), hwParcel.readString());
                    }
                    break;
                case 9:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        supplyNetworkDepersonalization(hwParcel.readInt32(), hwParcel.readString());
                    }
                    break;
                case 10:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        getCurrentCalls(hwParcel.readInt32());
                    }
                    break;
                case 11:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        int int32 = hwParcel.readInt32();
                        Dial dial = new Dial();
                        dial.readFromParcel(hwParcel);
                        dial(int32, dial);
                    }
                    break;
                case 12:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        getImsiForApp(hwParcel.readInt32(), hwParcel.readString());
                    }
                    break;
                case 13:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        hangup(hwParcel.readInt32(), hwParcel.readInt32());
                    }
                    break;
                case 14:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        hangupWaitingOrBackground(hwParcel.readInt32());
                    }
                    break;
                case 15:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        hangupForegroundResumeBackground(hwParcel.readInt32());
                    }
                    break;
                case 16:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        switchWaitingOrHoldingAndActive(hwParcel.readInt32());
                    }
                    break;
                case 17:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        conference(hwParcel.readInt32());
                    }
                    break;
                case 18:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        rejectCall(hwParcel.readInt32());
                    }
                    break;
                case 19:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        getLastCallFailCause(hwParcel.readInt32());
                    }
                    break;
                case 20:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        getSignalStrength(hwParcel.readInt32());
                    }
                    break;
                case 21:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        getVoiceRegistrationState(hwParcel.readInt32());
                    }
                    break;
                case 22:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        getDataRegistrationState(hwParcel.readInt32());
                    }
                    break;
                case 23:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        getOperator(hwParcel.readInt32());
                    }
                    break;
                case 24:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        setRadioPower(hwParcel.readInt32(), hwParcel.readBool());
                    }
                    break;
                case 25:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        sendDtmf(hwParcel.readInt32(), hwParcel.readString());
                    }
                    break;
                case 26:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        int int322 = hwParcel.readInt32();
                        GsmSmsMessage gsmSmsMessage = new GsmSmsMessage();
                        gsmSmsMessage.readFromParcel(hwParcel);
                        sendSms(int322, gsmSmsMessage);
                    }
                    break;
                case 27:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        int int323 = hwParcel.readInt32();
                        GsmSmsMessage gsmSmsMessage2 = new GsmSmsMessage();
                        gsmSmsMessage2.readFromParcel(hwParcel);
                        sendSMSExpectMore(int323, gsmSmsMessage2);
                    }
                    break;
                case 28:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        int int324 = hwParcel.readInt32();
                        int int325 = hwParcel.readInt32();
                        DataProfileInfo dataProfileInfo = new DataProfileInfo();
                        dataProfileInfo.readFromParcel(hwParcel);
                        setupDataCall(int324, int325, dataProfileInfo, hwParcel.readBool(), hwParcel.readBool(), hwParcel.readBool());
                    }
                    break;
                case 29:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        int int326 = hwParcel.readInt32();
                        IccIo iccIo = new IccIo();
                        iccIo.readFromParcel(hwParcel);
                        iccIOForApp(int326, iccIo);
                    }
                    break;
                case 30:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        sendUssd(hwParcel.readInt32(), hwParcel.readString());
                    }
                    break;
                case 31:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        cancelPendingUssd(hwParcel.readInt32());
                    }
                    break;
                case 32:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        getClir(hwParcel.readInt32());
                    }
                    break;
                case 33:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        setClir(hwParcel.readInt32(), hwParcel.readInt32());
                    }
                    break;
                case 34:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        int int327 = hwParcel.readInt32();
                        CallForwardInfo callForwardInfo = new CallForwardInfo();
                        callForwardInfo.readFromParcel(hwParcel);
                        getCallForwardStatus(int327, callForwardInfo);
                    }
                    break;
                case 35:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        int int328 = hwParcel.readInt32();
                        CallForwardInfo callForwardInfo2 = new CallForwardInfo();
                        callForwardInfo2.readFromParcel(hwParcel);
                        setCallForward(int328, callForwardInfo2);
                    }
                    break;
                case 36:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        getCallWaiting(hwParcel.readInt32(), hwParcel.readInt32());
                    }
                    break;
                case 37:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        setCallWaiting(hwParcel.readInt32(), hwParcel.readBool(), hwParcel.readInt32());
                    }
                    break;
                case 38:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        acknowledgeLastIncomingGsmSms(hwParcel.readInt32(), hwParcel.readBool(), hwParcel.readInt32());
                    }
                    break;
                case 39:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        acceptCall(hwParcel.readInt32());
                    }
                    break;
                case 40:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        deactivateDataCall(hwParcel.readInt32(), hwParcel.readInt32(), hwParcel.readBool());
                    }
                    break;
                case 41:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        getFacilityLockForApp(hwParcel.readInt32(), hwParcel.readString(), hwParcel.readString(), hwParcel.readInt32(), hwParcel.readString());
                    }
                    break;
                case 42:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        setFacilityLockForApp(hwParcel.readInt32(), hwParcel.readString(), hwParcel.readBool(), hwParcel.readString(), hwParcel.readInt32(), hwParcel.readString());
                    }
                    break;
                case 43:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        setBarringPassword(hwParcel.readInt32(), hwParcel.readString(), hwParcel.readString(), hwParcel.readString());
                    }
                    break;
                case 44:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        getNetworkSelectionMode(hwParcel.readInt32());
                    }
                    break;
                case 45:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        setNetworkSelectionModeAutomatic(hwParcel.readInt32());
                    }
                    break;
                case 46:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        setNetworkSelectionModeManual(hwParcel.readInt32(), hwParcel.readString());
                    }
                    break;
                case 47:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        getAvailableNetworks(hwParcel.readInt32());
                    }
                    break;
                case 48:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        startDtmf(hwParcel.readInt32(), hwParcel.readString());
                    }
                    break;
                case 49:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        stopDtmf(hwParcel.readInt32());
                    }
                    break;
                case 50:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        getBasebandVersion(hwParcel.readInt32());
                    }
                    break;
                case 51:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        separateConnection(hwParcel.readInt32(), hwParcel.readInt32());
                    }
                    break;
                case 52:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        setMute(hwParcel.readInt32(), hwParcel.readBool());
                    }
                    break;
                case 53:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        getMute(hwParcel.readInt32());
                    }
                    break;
                case 54:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        getClip(hwParcel.readInt32());
                    }
                    break;
                case 55:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        getDataCallList(hwParcel.readInt32());
                    }
                    break;
                case 56:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        setSuppServiceNotifications(hwParcel.readInt32(), hwParcel.readBool());
                    }
                    break;
                case 57:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        int int329 = hwParcel.readInt32();
                        SmsWriteArgs smsWriteArgs = new SmsWriteArgs();
                        smsWriteArgs.readFromParcel(hwParcel);
                        writeSmsToSim(int329, smsWriteArgs);
                    }
                    break;
                case 58:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        deleteSmsOnSim(hwParcel.readInt32(), hwParcel.readInt32());
                    }
                    break;
                case 59:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        setBandMode(hwParcel.readInt32(), hwParcel.readInt32());
                    }
                    break;
                case 60:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        getAvailableBandModes(hwParcel.readInt32());
                    }
                    break;
                case 61:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        sendEnvelope(hwParcel.readInt32(), hwParcel.readString());
                    }
                    break;
                case 62:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        sendTerminalResponseToSim(hwParcel.readInt32(), hwParcel.readString());
                    }
                    break;
                case 63:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        handleStkCallSetupRequestFromSim(hwParcel.readInt32(), hwParcel.readBool());
                    }
                    break;
                case 64:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        explicitCallTransfer(hwParcel.readInt32());
                    }
                    break;
                case 65:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        setPreferredNetworkType(hwParcel.readInt32(), hwParcel.readInt32());
                    }
                    break;
                case 66:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        getPreferredNetworkType(hwParcel.readInt32());
                    }
                    break;
                case TelephonyProto.RilErrno.RIL_E_INVALID_RESPONSE:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        getNeighboringCids(hwParcel.readInt32());
                    }
                    break;
                case 68:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        setLocationUpdates(hwParcel.readInt32(), hwParcel.readBool());
                    }
                    break;
                case 69:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        setCdmaSubscriptionSource(hwParcel.readInt32(), hwParcel.readInt32());
                    }
                    break;
                case 70:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        setCdmaRoamingPreference(hwParcel.readInt32(), hwParcel.readInt32());
                    }
                    break;
                case 71:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        getCdmaRoamingPreference(hwParcel.readInt32());
                    }
                    break;
                case 72:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        setTTYMode(hwParcel.readInt32(), hwParcel.readInt32());
                    }
                    break;
                case 73:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        getTTYMode(hwParcel.readInt32());
                    }
                    break;
                case 74:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        setPreferredVoicePrivacy(hwParcel.readInt32(), hwParcel.readBool());
                    }
                    break;
                case 75:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        getPreferredVoicePrivacy(hwParcel.readInt32());
                    }
                    break;
                case 76:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        sendCDMAFeatureCode(hwParcel.readInt32(), hwParcel.readString());
                    }
                    break;
                case 77:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        sendBurstDtmf(hwParcel.readInt32(), hwParcel.readString(), hwParcel.readInt32(), hwParcel.readInt32());
                    }
                    break;
                case 78:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        int int3210 = hwParcel.readInt32();
                        CdmaSmsMessage cdmaSmsMessage = new CdmaSmsMessage();
                        cdmaSmsMessage.readFromParcel(hwParcel);
                        sendCdmaSms(int3210, cdmaSmsMessage);
                    }
                    break;
                case 79:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        int int3211 = hwParcel.readInt32();
                        CdmaSmsAck cdmaSmsAck = new CdmaSmsAck();
                        cdmaSmsAck.readFromParcel(hwParcel);
                        acknowledgeLastIncomingCdmaSms(int3211, cdmaSmsAck);
                    }
                    break;
                case RadioNVItems.RIL_NV_LTE_NEXT_SCAN:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        getGsmBroadcastConfig(hwParcel.readInt32());
                    }
                    break;
                case 81:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        setGsmBroadcastConfig(hwParcel.readInt32(), GsmBroadcastSmsConfigInfo.readVectorFromParcel(hwParcel));
                    }
                    break;
                case RadioNVItems.RIL_NV_LTE_BSR_MAX_TIME:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        setGsmBroadcastActivation(hwParcel.readInt32(), hwParcel.readBool());
                    }
                    break;
                case 83:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        getCdmaBroadcastConfig(hwParcel.readInt32());
                    }
                    break;
                case 84:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        setCdmaBroadcastConfig(hwParcel.readInt32(), CdmaBroadcastSmsConfigInfo.readVectorFromParcel(hwParcel));
                    }
                    break;
                case 85:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        setCdmaBroadcastActivation(hwParcel.readInt32(), hwParcel.readBool());
                    }
                    break;
                case 86:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        getCDMASubscription(hwParcel.readInt32());
                    }
                    break;
                case 87:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        int int3212 = hwParcel.readInt32();
                        CdmaSmsWriteArgs cdmaSmsWriteArgs = new CdmaSmsWriteArgs();
                        cdmaSmsWriteArgs.readFromParcel(hwParcel);
                        writeSmsToRuim(int3212, cdmaSmsWriteArgs);
                    }
                    break;
                case 88:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        deleteSmsOnRuim(hwParcel.readInt32(), hwParcel.readInt32());
                    }
                    break;
                case 89:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        getDeviceIdentity(hwParcel.readInt32());
                    }
                    break;
                case 90:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        exitEmergencyCallbackMode(hwParcel.readInt32());
                    }
                    break;
                case 91:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        getSmscAddress(hwParcel.readInt32());
                    }
                    break;
                case 92:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        setSmscAddress(hwParcel.readInt32(), hwParcel.readString());
                    }
                    break;
                case 93:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        reportSmsMemoryStatus(hwParcel.readInt32(), hwParcel.readBool());
                    }
                    break;
                case 94:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        reportStkServiceIsRunning(hwParcel.readInt32());
                    }
                    break;
                case 95:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        getCdmaSubscriptionSource(hwParcel.readInt32());
                    }
                    break;
                case 96:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        requestIsimAuthentication(hwParcel.readInt32(), hwParcel.readString());
                    }
                    break;
                case 97:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        acknowledgeIncomingGsmSmsWithPdu(hwParcel.readInt32(), hwParcel.readBool(), hwParcel.readString());
                    }
                    break;
                case 98:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        sendEnvelopeWithStatus(hwParcel.readInt32(), hwParcel.readString());
                    }
                    break;
                case 99:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        getVoiceRadioTechnology(hwParcel.readInt32());
                    }
                    break;
                case 100:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        getCellInfoList(hwParcel.readInt32());
                    }
                    break;
                case 101:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        setCellInfoListRate(hwParcel.readInt32(), hwParcel.readInt32());
                    }
                    break;
                case 102:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        int int3213 = hwParcel.readInt32();
                        DataProfileInfo dataProfileInfo2 = new DataProfileInfo();
                        dataProfileInfo2.readFromParcel(hwParcel);
                        setInitialAttachApn(int3213, dataProfileInfo2, hwParcel.readBool(), hwParcel.readBool());
                    }
                    break;
                case 103:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        getImsRegistrationState(hwParcel.readInt32());
                    }
                    break;
                case 104:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        int int3214 = hwParcel.readInt32();
                        ImsSmsMessage imsSmsMessage = new ImsSmsMessage();
                        imsSmsMessage.readFromParcel(hwParcel);
                        sendImsSms(int3214, imsSmsMessage);
                    }
                    break;
                case 105:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        int int3215 = hwParcel.readInt32();
                        SimApdu simApdu = new SimApdu();
                        simApdu.readFromParcel(hwParcel);
                        iccTransmitApduBasicChannel(int3215, simApdu);
                    }
                    break;
                case 106:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        iccOpenLogicalChannel(hwParcel.readInt32(), hwParcel.readString(), hwParcel.readInt32());
                    }
                    break;
                case 107:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        iccCloseLogicalChannel(hwParcel.readInt32(), hwParcel.readInt32());
                    }
                    break;
                case 108:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        int int3216 = hwParcel.readInt32();
                        SimApdu simApdu2 = new SimApdu();
                        simApdu2.readFromParcel(hwParcel);
                        iccTransmitApduLogicalChannel(int3216, simApdu2);
                    }
                    break;
                case 109:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        nvReadItem(hwParcel.readInt32(), hwParcel.readInt32());
                    }
                    break;
                case 110:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        int int3217 = hwParcel.readInt32();
                        NvWriteItem nvWriteItem = new NvWriteItem();
                        nvWriteItem.readFromParcel(hwParcel);
                        nvWriteItem(int3217, nvWriteItem);
                    }
                    break;
                case 111:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        nvWriteCdmaPrl(hwParcel.readInt32(), hwParcel.readInt8Vector());
                    }
                    break;
                case 112:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        nvResetConfig(hwParcel.readInt32(), hwParcel.readInt32());
                    }
                    break;
                case 113:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        int int3218 = hwParcel.readInt32();
                        SelectUiccSub selectUiccSub = new SelectUiccSub();
                        selectUiccSub.readFromParcel(hwParcel);
                        setUiccSubscription(int3218, selectUiccSub);
                    }
                    break;
                case 114:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        setDataAllowed(hwParcel.readInt32(), hwParcel.readBool());
                    }
                    break;
                case 115:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        getHardwareConfig(hwParcel.readInt32());
                    }
                    break;
                case 116:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        requestIccSimAuthentication(hwParcel.readInt32(), hwParcel.readInt32(), hwParcel.readString(), hwParcel.readString());
                    }
                    break;
                case 117:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        setDataProfile(hwParcel.readInt32(), DataProfileInfo.readVectorFromParcel(hwParcel), hwParcel.readBool());
                    }
                    break;
                case 118:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        requestShutdown(hwParcel.readInt32());
                    }
                    break;
                case 119:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        getRadioCapability(hwParcel.readInt32());
                    }
                    break;
                case 120:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        int int3219 = hwParcel.readInt32();
                        RadioCapability radioCapability = new RadioCapability();
                        radioCapability.readFromParcel(hwParcel);
                        setRadioCapability(int3219, radioCapability);
                    }
                    break;
                case 121:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        startLceService(hwParcel.readInt32(), hwParcel.readInt32(), hwParcel.readBool());
                    }
                    break;
                case 122:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        stopLceService(hwParcel.readInt32());
                    }
                    break;
                case 123:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        pullLceData(hwParcel.readInt32());
                    }
                    break;
                case 124:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        getModemActivityInfo(hwParcel.readInt32());
                    }
                    break;
                case 125:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        int int3220 = hwParcel.readInt32();
                        boolean bool = hwParcel.readBool();
                        CarrierRestrictions carrierRestrictions = new CarrierRestrictions();
                        carrierRestrictions.readFromParcel(hwParcel);
                        setAllowedCarriers(int3220, bool, carrierRestrictions);
                    }
                    break;
                case 126:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        getAllowedCarriers(hwParcel.readInt32());
                    }
                    break;
                case 127:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        sendDeviceState(hwParcel.readInt32(), hwParcel.readInt32(), hwParcel.readBool());
                    }
                    break;
                case 128:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        setIndicationFilter(hwParcel.readInt32(), hwParcel.readInt32());
                    }
                    break;
                case 129:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        setSimCardPower(hwParcel.readInt32(), hwParcel.readBool());
                    }
                    break;
                case 130:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_0.IRadio.kInterfaceName);
                        responseAcknowledgement();
                    }
                    break;
                case 131:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_1.IRadio.kInterfaceName);
                        int int3221 = hwParcel.readInt32();
                        ImsiEncryptionInfo imsiEncryptionInfo = new ImsiEncryptionInfo();
                        imsiEncryptionInfo.readFromParcel(hwParcel);
                        setCarrierInfoForImsiEncryption(int3221, imsiEncryptionInfo);
                    }
                    break;
                case 132:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_1.IRadio.kInterfaceName);
                        setSimCardPower_1_1(hwParcel.readInt32(), hwParcel.readInt32());
                    }
                    break;
                case 133:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_1.IRadio.kInterfaceName);
                        int int3222 = hwParcel.readInt32();
                        android.hardware.radio.V1_1.NetworkScanRequest networkScanRequest = new android.hardware.radio.V1_1.NetworkScanRequest();
                        networkScanRequest.readFromParcel(hwParcel);
                        startNetworkScan(int3222, networkScanRequest);
                    }
                    break;
                case 134:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_1.IRadio.kInterfaceName);
                        stopNetworkScan(hwParcel.readInt32());
                    }
                    break;
                case 135:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_1.IRadio.kInterfaceName);
                        int int3223 = hwParcel.readInt32();
                        KeepaliveRequest keepaliveRequest = new KeepaliveRequest();
                        keepaliveRequest.readFromParcel(hwParcel);
                        startKeepalive(int3223, keepaliveRequest);
                    }
                    break;
                case 136:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.radio.V1_1.IRadio.kInterfaceName);
                        stopKeepalive(hwParcel.readInt32(), hwParcel.readInt32());
                    }
                    break;
                case 137:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IRadio.kInterfaceName);
                        int int3224 = hwParcel.readInt32();
                        NetworkScanRequest networkScanRequest2 = new NetworkScanRequest();
                        networkScanRequest2.readFromParcel(hwParcel);
                        startNetworkScan_1_2(int3224, networkScanRequest2);
                    }
                    break;
                case 138:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IRadio.kInterfaceName);
                        setIndicationFilter_1_2(hwParcel.readInt32(), hwParcel.readInt32());
                    }
                    break;
                case 139:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IRadio.kInterfaceName);
                        setSignalStrengthReportingCriteria(hwParcel.readInt32(), hwParcel.readInt32(), hwParcel.readInt32(), hwParcel.readInt32Vector(), hwParcel.readInt32());
                    }
                    break;
                case 140:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IRadio.kInterfaceName);
                        setLinkCapacityReportingCriteria(hwParcel.readInt32(), hwParcel.readInt32(), hwParcel.readInt32(), hwParcel.readInt32(), hwParcel.readInt32Vector(), hwParcel.readInt32Vector(), hwParcel.readInt32());
                    }
                    break;
                case 141:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IRadio.kInterfaceName);
                        int int3225 = hwParcel.readInt32();
                        int int3226 = hwParcel.readInt32();
                        DataProfileInfo dataProfileInfo3 = new DataProfileInfo();
                        dataProfileInfo3.readFromParcel(hwParcel);
                        setupDataCall_1_2(int3225, int3226, dataProfileInfo3, hwParcel.readBool(), hwParcel.readBool(), hwParcel.readBool(), hwParcel.readInt32(), hwParcel.readStringVector(), hwParcel.readStringVector());
                    }
                    break;
                case 142:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IRadio.kInterfaceName);
                        deactivateDataCall_1_2(hwParcel.readInt32(), hwParcel.readInt32(), hwParcel.readInt32());
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
