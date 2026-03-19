package android.hardware.wifi.supplicant.V1_1;

import android.hardware.wifi.supplicant.V1_0.ISupplicantNetwork;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaIfaceCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetworkCallback;
import android.hardware.wifi.supplicant.V1_0.SupplicantStatus;
import android.hidl.base.V1_0.DebugInfo;
import android.hidl.base.V1_0.IBase;
import android.os.HidlSupport;
import android.os.HwBinder;
import android.os.HwBlob;
import android.os.HwParcel;
import android.os.IHwBinder;
import android.os.IHwInterface;
import android.os.RemoteException;
import com.android.server.wifi.HalDeviceManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;

public interface ISupplicantStaNetwork extends android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork {
    public static final String kInterfaceName = "android.hardware.wifi.supplicant@1.1::ISupplicantStaNetwork";

    @Override
    IHwBinder asBinder();

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

    SupplicantStatus sendNetworkEapIdentityResponse_1_1(ArrayList<Byte> arrayList, ArrayList<Byte> arrayList2) throws RemoteException;

    SupplicantStatus setEapEncryptedImsiIdentity(ArrayList<Byte> arrayList) throws RemoteException;

    @Override
    void setHALInstrumentation() throws RemoteException;

    @Override
    boolean unlinkToDeath(IHwBinder.DeathRecipient deathRecipient) throws RemoteException;

    static ISupplicantStaNetwork asInterface(IHwBinder iHwBinder) {
        if (iHwBinder == null) {
            return null;
        }
        IHwInterface iHwInterfaceQueryLocalInterface = iHwBinder.queryLocalInterface(kInterfaceName);
        if (iHwInterfaceQueryLocalInterface != null && (iHwInterfaceQueryLocalInterface instanceof ISupplicantStaNetwork)) {
            return (ISupplicantStaNetwork) iHwInterfaceQueryLocalInterface;
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

    static ISupplicantStaNetwork castFrom(IHwInterface iHwInterface) {
        if (iHwInterface == null) {
            return null;
        }
        return asInterface(iHwInterface.asBinder());
    }

    static ISupplicantStaNetwork getService(String str, boolean z) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, str, z));
    }

    static ISupplicantStaNetwork getService(boolean z) throws RemoteException {
        return getService(HalDeviceManager.HAL_INSTANCE_NAME, z);
    }

    static ISupplicantStaNetwork getService(String str) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, str));
    }

    static ISupplicantStaNetwork getService() throws RemoteException {
        return getService(HalDeviceManager.HAL_INSTANCE_NAME);
    }

    public static final class Proxy implements ISupplicantStaNetwork {
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
                return "[class or subclass of android.hardware.wifi.supplicant@1.1::ISupplicantStaNetwork]@Proxy";
            }
        }

        public final boolean equals(Object obj) {
            return HidlSupport.interfacesEqual(this, obj);
        }

        public final int hashCode() {
            return asBinder().hashCode();
        }

        @Override
        public void getId(ISupplicantNetwork.getIdCallback getidcallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(1, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                getidcallback.onValues(supplicantStatus, hwParcel2.readInt32());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getInterfaceName(ISupplicantNetwork.getInterfaceNameCallback getinterfacenamecallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(2, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                getinterfacenamecallback.onValues(supplicantStatus, hwParcel2.readString());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getType(ISupplicantNetwork.getTypeCallback gettypecallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(3, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                gettypecallback.onValues(supplicantStatus, hwParcel2.readInt32());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus registerCallback(ISupplicantStaNetworkCallback iSupplicantStaNetworkCallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeStrongBinder(iSupplicantStaNetworkCallback == null ? null : iSupplicantStaNetworkCallback.asBinder());
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(4, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setSsid(ArrayList<Byte> arrayList) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeInt8Vector(arrayList);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(5, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setBssid(byte[] bArr) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            HwBlob hwBlob = new HwBlob(6);
            hwBlob.putInt8Array(0L, bArr);
            hwParcel.writeBuffer(hwBlob);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(6, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setScanSsid(boolean z) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeBool(z);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(7, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setKeyMgmt(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(8, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setProto(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(9, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setAuthAlg(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(10, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setGroupCipher(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(11, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setPairwiseCipher(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(12, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setPskPassphrase(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(13, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setPsk(byte[] bArr) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            HwBlob hwBlob = new HwBlob(32);
            hwBlob.putInt8Array(0L, bArr);
            hwParcel.writeBuffer(hwBlob);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(14, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setWepKey(int i, ArrayList<Byte> arrayList) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeInt8Vector(arrayList);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(15, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setWepTxKeyIdx(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(16, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setRequirePmf(boolean z) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeBool(z);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(17, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setEapMethod(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(18, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setEapPhase2Method(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(19, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setEapIdentity(ArrayList<Byte> arrayList) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeInt8Vector(arrayList);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(20, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setEapAnonymousIdentity(ArrayList<Byte> arrayList) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeInt8Vector(arrayList);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(21, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setEapPassword(ArrayList<Byte> arrayList) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeInt8Vector(arrayList);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(22, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setEapCACert(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(23, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setEapCAPath(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(24, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setEapClientCert(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(25, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setEapPrivateKeyId(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(26, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setEapSubjectMatch(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(27, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setEapAltSubjectMatch(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(28, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setEapEngine(boolean z) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeBool(z);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(29, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setEapEngineID(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(30, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setEapDomainSuffixMatch(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(31, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setProactiveKeyCaching(boolean z) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeBool(z);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(32, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setIdStr(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(33, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setUpdateIdentifier(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(34, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getSsid(ISupplicantStaNetwork.getSsidCallback getssidcallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(35, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                getssidcallback.onValues(supplicantStatus, hwParcel2.readInt8Vector());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getBssid(ISupplicantStaNetwork.getBssidCallback getbssidcallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(36, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                byte[] bArr = new byte[6];
                hwParcel2.readBuffer(6L).copyToInt8Array(0L, bArr, 6);
                getbssidcallback.onValues(supplicantStatus, bArr);
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getScanSsid(ISupplicantStaNetwork.getScanSsidCallback getscanssidcallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(37, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                getscanssidcallback.onValues(supplicantStatus, hwParcel2.readBool());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getKeyMgmt(ISupplicantStaNetwork.getKeyMgmtCallback getkeymgmtcallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(38, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                getkeymgmtcallback.onValues(supplicantStatus, hwParcel2.readInt32());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getProto(ISupplicantStaNetwork.getProtoCallback getprotocallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(39, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                getprotocallback.onValues(supplicantStatus, hwParcel2.readInt32());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getAuthAlg(ISupplicantStaNetwork.getAuthAlgCallback getauthalgcallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(40, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                getauthalgcallback.onValues(supplicantStatus, hwParcel2.readInt32());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getGroupCipher(ISupplicantStaNetwork.getGroupCipherCallback getgroupciphercallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(41, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                getgroupciphercallback.onValues(supplicantStatus, hwParcel2.readInt32());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getPairwiseCipher(ISupplicantStaNetwork.getPairwiseCipherCallback getpairwiseciphercallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(42, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                getpairwiseciphercallback.onValues(supplicantStatus, hwParcel2.readInt32());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getPskPassphrase(ISupplicantStaNetwork.getPskPassphraseCallback getpskpassphrasecallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(43, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                getpskpassphrasecallback.onValues(supplicantStatus, hwParcel2.readString());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getPsk(ISupplicantStaNetwork.getPskCallback getpskcallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(44, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                byte[] bArr = new byte[32];
                hwParcel2.readBuffer(32L).copyToInt8Array(0L, bArr, 32);
                getpskcallback.onValues(supplicantStatus, bArr);
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getWepKey(int i, ISupplicantStaNetwork.getWepKeyCallback getwepkeycallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(45, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                getwepkeycallback.onValues(supplicantStatus, hwParcel2.readInt8Vector());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getWepTxKeyIdx(ISupplicantStaNetwork.getWepTxKeyIdxCallback getweptxkeyidxcallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(46, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                getweptxkeyidxcallback.onValues(supplicantStatus, hwParcel2.readInt32());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getRequirePmf(ISupplicantStaNetwork.getRequirePmfCallback getrequirepmfcallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(47, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                getrequirepmfcallback.onValues(supplicantStatus, hwParcel2.readBool());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getEapMethod(ISupplicantStaNetwork.getEapMethodCallback geteapmethodcallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(48, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                geteapmethodcallback.onValues(supplicantStatus, hwParcel2.readInt32());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getEapPhase2Method(ISupplicantStaNetwork.getEapPhase2MethodCallback geteapphase2methodcallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(49, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                geteapphase2methodcallback.onValues(supplicantStatus, hwParcel2.readInt32());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getEapIdentity(ISupplicantStaNetwork.getEapIdentityCallback geteapidentitycallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(50, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                geteapidentitycallback.onValues(supplicantStatus, hwParcel2.readInt8Vector());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getEapAnonymousIdentity(ISupplicantStaNetwork.getEapAnonymousIdentityCallback geteapanonymousidentitycallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(51, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                geteapanonymousidentitycallback.onValues(supplicantStatus, hwParcel2.readInt8Vector());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getEapPassword(ISupplicantStaNetwork.getEapPasswordCallback geteappasswordcallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(52, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                geteappasswordcallback.onValues(supplicantStatus, hwParcel2.readInt8Vector());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getEapCACert(ISupplicantStaNetwork.getEapCACertCallback geteapcacertcallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(53, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                geteapcacertcallback.onValues(supplicantStatus, hwParcel2.readString());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getEapCAPath(ISupplicantStaNetwork.getEapCAPathCallback geteapcapathcallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(54, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                geteapcapathcallback.onValues(supplicantStatus, hwParcel2.readString());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getEapClientCert(ISupplicantStaNetwork.getEapClientCertCallback geteapclientcertcallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(55, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                geteapclientcertcallback.onValues(supplicantStatus, hwParcel2.readString());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getEapPrivateKeyId(ISupplicantStaNetwork.getEapPrivateKeyIdCallback geteapprivatekeyidcallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(56, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                geteapprivatekeyidcallback.onValues(supplicantStatus, hwParcel2.readString());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getEapSubjectMatch(ISupplicantStaNetwork.getEapSubjectMatchCallback geteapsubjectmatchcallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(57, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                geteapsubjectmatchcallback.onValues(supplicantStatus, hwParcel2.readString());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getEapAltSubjectMatch(ISupplicantStaNetwork.getEapAltSubjectMatchCallback geteapaltsubjectmatchcallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(58, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                geteapaltsubjectmatchcallback.onValues(supplicantStatus, hwParcel2.readString());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getEapEngine(ISupplicantStaNetwork.getEapEngineCallback geteapenginecallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(59, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                geteapenginecallback.onValues(supplicantStatus, hwParcel2.readBool());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getEapEngineID(ISupplicantStaNetwork.getEapEngineIDCallback geteapengineidcallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(60, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                geteapengineidcallback.onValues(supplicantStatus, hwParcel2.readString());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getEapDomainSuffixMatch(ISupplicantStaNetwork.getEapDomainSuffixMatchCallback geteapdomainsuffixmatchcallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(61, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                geteapdomainsuffixmatchcallback.onValues(supplicantStatus, hwParcel2.readString());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getIdStr(ISupplicantStaNetwork.getIdStrCallback getidstrcallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(62, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                getidstrcallback.onValues(supplicantStatus, hwParcel2.readString());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getWpsNfcConfigurationToken(ISupplicantStaNetwork.getWpsNfcConfigurationTokenCallback getwpsnfcconfigurationtokencallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(63, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                getwpsnfcconfigurationtokencallback.onValues(supplicantStatus, hwParcel2.readInt8Vector());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus enable(boolean z) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeBool(z);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(64, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus disable() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(65, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus select() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(66, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus sendNetworkEapSimGsmAuthResponse(ArrayList<ISupplicantStaNetwork.NetworkResponseEapSimGsmAuthParams> arrayList) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            ISupplicantStaNetwork.NetworkResponseEapSimGsmAuthParams.writeVectorToParcel(hwParcel, arrayList);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(67, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus sendNetworkEapSimGsmAuthFailure() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(68, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus sendNetworkEapSimUmtsAuthResponse(ISupplicantStaNetwork.NetworkResponseEapSimUmtsAuthParams networkResponseEapSimUmtsAuthParams) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            networkResponseEapSimUmtsAuthParams.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(69, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus sendNetworkEapSimUmtsAutsResponse(byte[] bArr) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            HwBlob hwBlob = new HwBlob(14);
            hwBlob.putInt8Array(0L, bArr);
            hwParcel.writeBuffer(hwBlob);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(70, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus sendNetworkEapSimUmtsAuthFailure() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(71, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus sendNetworkEapIdentityResponse(ArrayList<Byte> arrayList) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeInt8Vector(arrayList);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(72, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setEapEncryptedImsiIdentity(ArrayList<Byte> arrayList) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeInt8Vector(arrayList);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(73, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus sendNetworkEapIdentityResponse_1_1(ArrayList<Byte> arrayList, ArrayList<Byte> arrayList2) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaNetwork.kInterfaceName);
            hwParcel.writeInt8Vector(arrayList);
            hwParcel.writeInt8Vector(arrayList2);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(74, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                return supplicantStatus;
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

    public static abstract class Stub extends HwBinder implements ISupplicantStaNetwork {
        @Override
        public IHwBinder asBinder() {
            return this;
        }

        @Override
        public final ArrayList<String> interfaceChain() {
            return new ArrayList<>(Arrays.asList(ISupplicantStaNetwork.kInterfaceName, android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName, ISupplicantNetwork.kInterfaceName, IBase.kInterfaceName));
        }

        @Override
        public final String interfaceDescriptor() {
            return ISupplicantStaNetwork.kInterfaceName;
        }

        @Override
        public final ArrayList<byte[]> getHashChain() {
            return new ArrayList<>(Arrays.asList(new byte[]{16, -1, 47, -82, 81, 99, 70, -72, 97, 33, 54, -116, -27, 121, 13, 90, -52, -33, -53, 115, -104, 50, 70, -72, 19, -13, -44, -120, -74, 109, -76, 90}, new byte[]{-79, 46, -16, -67, -40, -92, -46, 71, -88, -90, -23, 96, -78, 39, -19, 50, 56, 63, 43, 2, 65, -11, 93, 103, -4, -22, 110, -1, 106, 103, 55, -6}, new byte[]{-51, -96, 16, 8, -64, 105, 34, -6, 55, -63, 33, 62, -101, -72, 49, -95, 9, -77, 23, 69, 50, -128, 86, 22, -5, 113, 97, -19, -60, 3, -122, 111}, new byte[]{-67, -38, -74, 24, 77, 122, 52, 109, -90, -96, 125, -64, -126, -116, -15, -102, 105, 111, 76, -86, 54, 17, -59, 31, 46, 20, 86, 90, 20, -76, 15, -39}));
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
            if (ISupplicantStaNetwork.kInterfaceName.equals(str)) {
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
        public void onTransact(int i, HwParcel hwParcel, final HwParcel hwParcel2, int i2) throws RemoteException {
            switch (i) {
                case 1:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantNetwork.kInterfaceName);
                        getId(new ISupplicantNetwork.getIdCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, int i3) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeInt32(i3);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 2:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantNetwork.kInterfaceName);
                        getInterfaceName(new ISupplicantNetwork.getInterfaceNameCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, String str) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeString(str);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 3:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantNetwork.kInterfaceName);
                        getType(new ISupplicantNetwork.getTypeCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, int i3) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeInt32(i3);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 4:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus supplicantStatusRegisterCallback = registerCallback(ISupplicantStaNetworkCallback.asInterface(hwParcel.readStrongBinder()));
                        hwParcel2.writeStatus(0);
                        supplicantStatusRegisterCallback.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 5:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus ssid = setSsid(hwParcel.readInt8Vector());
                        hwParcel2.writeStatus(0);
                        ssid.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 6:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        byte[] bArr = new byte[6];
                        hwParcel.readBuffer(6L).copyToInt8Array(0L, bArr, 6);
                        SupplicantStatus bssid = setBssid(bArr);
                        hwParcel2.writeStatus(0);
                        bssid.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 7:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus scanSsid = setScanSsid(hwParcel.readBool());
                        hwParcel2.writeStatus(0);
                        scanSsid.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 8:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus keyMgmt = setKeyMgmt(hwParcel.readInt32());
                        hwParcel2.writeStatus(0);
                        keyMgmt.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 9:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus proto = setProto(hwParcel.readInt32());
                        hwParcel2.writeStatus(0);
                        proto.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 10:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus authAlg = setAuthAlg(hwParcel.readInt32());
                        hwParcel2.writeStatus(0);
                        authAlg.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 11:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus groupCipher = setGroupCipher(hwParcel.readInt32());
                        hwParcel2.writeStatus(0);
                        groupCipher.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 12:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus pairwiseCipher = setPairwiseCipher(hwParcel.readInt32());
                        hwParcel2.writeStatus(0);
                        pairwiseCipher.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 13:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus pskPassphrase = setPskPassphrase(hwParcel.readString());
                        hwParcel2.writeStatus(0);
                        pskPassphrase.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 14:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        byte[] bArr2 = new byte[32];
                        hwParcel.readBuffer(32L).copyToInt8Array(0L, bArr2, 32);
                        SupplicantStatus psk = setPsk(bArr2);
                        hwParcel2.writeStatus(0);
                        psk.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 15:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus wepKey = setWepKey(hwParcel.readInt32(), hwParcel.readInt8Vector());
                        hwParcel2.writeStatus(0);
                        wepKey.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 16:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus wepTxKeyIdx = setWepTxKeyIdx(hwParcel.readInt32());
                        hwParcel2.writeStatus(0);
                        wepTxKeyIdx.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 17:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus requirePmf = setRequirePmf(hwParcel.readBool());
                        hwParcel2.writeStatus(0);
                        requirePmf.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 18:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus eapMethod = setEapMethod(hwParcel.readInt32());
                        hwParcel2.writeStatus(0);
                        eapMethod.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 19:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus eapPhase2Method = setEapPhase2Method(hwParcel.readInt32());
                        hwParcel2.writeStatus(0);
                        eapPhase2Method.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 20:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus eapIdentity = setEapIdentity(hwParcel.readInt8Vector());
                        hwParcel2.writeStatus(0);
                        eapIdentity.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case ISupplicantStaIfaceCallback.ReasonCode.UNSUPPORTED_RSN_IE_VERSION:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus eapAnonymousIdentity = setEapAnonymousIdentity(hwParcel.readInt8Vector());
                        hwParcel2.writeStatus(0);
                        eapAnonymousIdentity.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 22:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus eapPassword = setEapPassword(hwParcel.readInt8Vector());
                        hwParcel2.writeStatus(0);
                        eapPassword.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 23:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus eapCACert = setEapCACert(hwParcel.readString());
                        hwParcel2.writeStatus(0);
                        eapCACert.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 24:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus eapCAPath = setEapCAPath(hwParcel.readString());
                        hwParcel2.writeStatus(0);
                        eapCAPath.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 25:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus eapClientCert = setEapClientCert(hwParcel.readString());
                        hwParcel2.writeStatus(0);
                        eapClientCert.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case ISupplicantStaIfaceCallback.ReasonCode.TDLS_TEARDOWN_UNSPECIFIED:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus eapPrivateKeyId = setEapPrivateKeyId(hwParcel.readString());
                        hwParcel2.writeStatus(0);
                        eapPrivateKeyId.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 27:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus eapSubjectMatch = setEapSubjectMatch(hwParcel.readString());
                        hwParcel2.writeStatus(0);
                        eapSubjectMatch.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 28:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus eapAltSubjectMatch = setEapAltSubjectMatch(hwParcel.readString());
                        hwParcel2.writeStatus(0);
                        eapAltSubjectMatch.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 29:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus eapEngine = setEapEngine(hwParcel.readBool());
                        hwParcel2.writeStatus(0);
                        eapEngine.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 30:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus eapEngineID = setEapEngineID(hwParcel.readString());
                        hwParcel2.writeStatus(0);
                        eapEngineID.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 31:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus eapDomainSuffixMatch = setEapDomainSuffixMatch(hwParcel.readString());
                        hwParcel2.writeStatus(0);
                        eapDomainSuffixMatch.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 32:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus proactiveKeyCaching = setProactiveKeyCaching(hwParcel.readBool());
                        hwParcel2.writeStatus(0);
                        proactiveKeyCaching.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 33:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus idStr = setIdStr(hwParcel.readString());
                        hwParcel2.writeStatus(0);
                        idStr.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 34:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus updateIdentifier = setUpdateIdentifier(hwParcel.readInt32());
                        hwParcel2.writeStatus(0);
                        updateIdentifier.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 35:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        getSsid(new ISupplicantStaNetwork.getSsidCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, ArrayList<Byte> arrayList) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeInt8Vector(arrayList);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case ISupplicantStaIfaceCallback.ReasonCode.STA_LEAVING:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        getBssid(new ISupplicantStaNetwork.getBssidCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, byte[] bArr3) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                HwBlob hwBlob = new HwBlob(6);
                                hwBlob.putInt8Array(0L, bArr3);
                                hwParcel2.writeBuffer(hwBlob);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 37:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        getScanSsid(new ISupplicantStaNetwork.getScanSsidCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, boolean z) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeBool(z);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 38:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        getKeyMgmt(new ISupplicantStaNetwork.getKeyMgmtCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, int i3) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeInt32(i3);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 39:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        getProto(new ISupplicantStaNetwork.getProtoCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, int i3) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeInt32(i3);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case ISupplicantStaIfaceCallback.StatusCode.INVALID_IE:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        getAuthAlg(new ISupplicantStaNetwork.getAuthAlgCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, int i3) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeInt32(i3);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case ISupplicantStaIfaceCallback.StatusCode.GROUP_CIPHER_NOT_VALID:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        getGroupCipher(new ISupplicantStaNetwork.getGroupCipherCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, int i3) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeInt32(i3);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 42:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        getPairwiseCipher(new ISupplicantStaNetwork.getPairwiseCipherCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, int i3) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeInt32(i3);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 43:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        getPskPassphrase(new ISupplicantStaNetwork.getPskPassphraseCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, String str) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeString(str);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case ISupplicantStaIfaceCallback.StatusCode.UNSUPPORTED_RSN_IE_VERSION:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        getPsk(new ISupplicantStaNetwork.getPskCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, byte[] bArr3) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                HwBlob hwBlob = new HwBlob(32);
                                hwBlob.putInt8Array(0L, bArr3);
                                hwParcel2.writeBuffer(hwBlob);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 45:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        getWepKey(hwParcel.readInt32(), new ISupplicantStaNetwork.getWepKeyCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, ArrayList<Byte> arrayList) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeInt8Vector(arrayList);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 46:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        getWepTxKeyIdx(new ISupplicantStaNetwork.getWepTxKeyIdxCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, int i3) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeInt32(i3);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 47:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        getRequirePmf(new ISupplicantStaNetwork.getRequirePmfCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, boolean z) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeBool(z);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 48:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        getEapMethod(new ISupplicantStaNetwork.getEapMethodCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, int i3) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeInt32(i3);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 49:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        getEapPhase2Method(new ISupplicantStaNetwork.getEapPhase2MethodCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, int i3) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeInt32(i3);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 50:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        getEapIdentity(new ISupplicantStaNetwork.getEapIdentityCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, ArrayList<Byte> arrayList) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeInt8Vector(arrayList);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 51:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        getEapAnonymousIdentity(new ISupplicantStaNetwork.getEapAnonymousIdentityCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, ArrayList<Byte> arrayList) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeInt8Vector(arrayList);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 52:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        getEapPassword(new ISupplicantStaNetwork.getEapPasswordCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, ArrayList<Byte> arrayList) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeInt8Vector(arrayList);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 53:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        getEapCACert(new ISupplicantStaNetwork.getEapCACertCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, String str) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeString(str);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 54:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        getEapCAPath(new ISupplicantStaNetwork.getEapCAPathCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, String str) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeString(str);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 55:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        getEapClientCert(new ISupplicantStaNetwork.getEapClientCertCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, String str) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeString(str);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 56:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        getEapPrivateKeyId(new ISupplicantStaNetwork.getEapPrivateKeyIdCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, String str) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeString(str);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 57:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        getEapSubjectMatch(new ISupplicantStaNetwork.getEapSubjectMatchCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, String str) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeString(str);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 58:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        getEapAltSubjectMatch(new ISupplicantStaNetwork.getEapAltSubjectMatchCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, String str) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeString(str);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 59:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        getEapEngine(new ISupplicantStaNetwork.getEapEngineCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, boolean z) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeBool(z);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 60:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        getEapEngineID(new ISupplicantStaNetwork.getEapEngineIDCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, String str) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeString(str);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 61:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        getEapDomainSuffixMatch(new ISupplicantStaNetwork.getEapDomainSuffixMatchCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, String str) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeString(str);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 62:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        getIdStr(new ISupplicantStaNetwork.getIdStrCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, String str) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeString(str);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 63:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        getWpsNfcConfigurationToken(new ISupplicantStaNetwork.getWpsNfcConfigurationTokenCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, ArrayList<Byte> arrayList) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeInt8Vector(arrayList);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 64:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus supplicantStatusEnable = enable(hwParcel.readBool());
                        hwParcel2.writeStatus(0);
                        supplicantStatusEnable.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 65:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus supplicantStatusDisable = disable();
                        hwParcel2.writeStatus(0);
                        supplicantStatusDisable.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case ISupplicantStaIfaceCallback.ReasonCode.MESH_CHANNEL_SWITCH_UNSPECIFIED:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus supplicantStatusSelect = select();
                        hwParcel2.writeStatus(0);
                        supplicantStatusSelect.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case ISupplicantStaIfaceCallback.StatusCode.REQ_REFUSED_SSPN:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus supplicantStatusSendNetworkEapSimGsmAuthResponse = sendNetworkEapSimGsmAuthResponse(ISupplicantStaNetwork.NetworkResponseEapSimGsmAuthParams.readVectorFromParcel(hwParcel));
                        hwParcel2.writeStatus(0);
                        supplicantStatusSendNetworkEapSimGsmAuthResponse.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 68:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus supplicantStatusSendNetworkEapSimGsmAuthFailure = sendNetworkEapSimGsmAuthFailure();
                        hwParcel2.writeStatus(0);
                        supplicantStatusSendNetworkEapSimGsmAuthFailure.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 69:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        ISupplicantStaNetwork.NetworkResponseEapSimUmtsAuthParams networkResponseEapSimUmtsAuthParams = new ISupplicantStaNetwork.NetworkResponseEapSimUmtsAuthParams();
                        networkResponseEapSimUmtsAuthParams.readFromParcel(hwParcel);
                        SupplicantStatus supplicantStatusSendNetworkEapSimUmtsAuthResponse = sendNetworkEapSimUmtsAuthResponse(networkResponseEapSimUmtsAuthParams);
                        hwParcel2.writeStatus(0);
                        supplicantStatusSendNetworkEapSimUmtsAuthResponse.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 70:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        byte[] bArr3 = new byte[14];
                        hwParcel.readBuffer(14L).copyToInt8Array(0L, bArr3, 14);
                        SupplicantStatus supplicantStatusSendNetworkEapSimUmtsAutsResponse = sendNetworkEapSimUmtsAutsResponse(bArr3);
                        hwParcel2.writeStatus(0);
                        supplicantStatusSendNetworkEapSimUmtsAutsResponse.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 71:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus supplicantStatusSendNetworkEapSimUmtsAuthFailure = sendNetworkEapSimUmtsAuthFailure();
                        hwParcel2.writeStatus(0);
                        supplicantStatusSendNetworkEapSimUmtsAuthFailure.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case ISupplicantStaIfaceCallback.StatusCode.INVALID_RSNIE:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus supplicantStatusSendNetworkEapIdentityResponse = sendNetworkEapIdentityResponse(hwParcel.readInt8Vector());
                        hwParcel2.writeStatus(0);
                        supplicantStatusSendNetworkEapIdentityResponse.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case ISupplicantStaIfaceCallback.StatusCode.U_APSD_COEX_NOT_SUPPORTED:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus eapEncryptedImsiIdentity = setEapEncryptedImsiIdentity(hwParcel.readInt8Vector());
                        hwParcel2.writeStatus(0);
                        eapEncryptedImsiIdentity.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case ISupplicantStaIfaceCallback.StatusCode.U_APSD_COEX_MODE_NOT_SUPPORTED:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaNetwork.kInterfaceName);
                        SupplicantStatus supplicantStatusSendNetworkEapIdentityResponse_1_1 = sendNetworkEapIdentityResponse_1_1(hwParcel.readInt8Vector(), hwParcel.readInt8Vector());
                        hwParcel2.writeStatus(0);
                        supplicantStatusSendNetworkEapIdentityResponse_1_1.writeToParcel(hwParcel2);
                        hwParcel2.send();
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
