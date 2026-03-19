package android.hardware.wifi.supplicant.V1_0;

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

public interface ISupplicantP2pIfaceCallback extends IBase {
    public static final String kInterfaceName = "android.hardware.wifi.supplicant@1.0::ISupplicantP2pIfaceCallback";

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

    void onDeviceFound(byte[] bArr, byte[] bArr2, byte[] bArr3, String str, short s, byte b, int i, byte[] bArr4) throws RemoteException;

    void onDeviceLost(byte[] bArr) throws RemoteException;

    void onFindStopped() throws RemoteException;

    void onGoNegotiationCompleted(int i) throws RemoteException;

    void onGoNegotiationRequest(byte[] bArr, short s) throws RemoteException;

    void onGroupFormationFailure(String str) throws RemoteException;

    void onGroupFormationSuccess() throws RemoteException;

    void onGroupRemoved(String str, boolean z) throws RemoteException;

    void onGroupStarted(String str, boolean z, ArrayList<Byte> arrayList, int i, byte[] bArr, String str2, byte[] bArr2, boolean z2) throws RemoteException;

    void onInvitationReceived(byte[] bArr, byte[] bArr2, byte[] bArr3, int i, int i2) throws RemoteException;

    void onInvitationResult(byte[] bArr, int i) throws RemoteException;

    void onNetworkAdded(int i) throws RemoteException;

    void onNetworkRemoved(int i) throws RemoteException;

    void onProvisionDiscoveryCompleted(byte[] bArr, boolean z, byte b, short s, String str) throws RemoteException;

    void onServiceDiscoveryResponse(byte[] bArr, short s, ArrayList<Byte> arrayList) throws RemoteException;

    void onStaAuthorized(byte[] bArr, byte[] bArr2) throws RemoteException;

    void onStaDeauthorized(byte[] bArr, byte[] bArr2) throws RemoteException;

    @Override
    void ping() throws RemoteException;

    @Override
    void setHALInstrumentation() throws RemoteException;

    @Override
    boolean unlinkToDeath(IHwBinder.DeathRecipient deathRecipient) throws RemoteException;

    static ISupplicantP2pIfaceCallback asInterface(IHwBinder iHwBinder) {
        if (iHwBinder == null) {
            return null;
        }
        IHwInterface iHwInterfaceQueryLocalInterface = iHwBinder.queryLocalInterface(kInterfaceName);
        if (iHwInterfaceQueryLocalInterface != null && (iHwInterfaceQueryLocalInterface instanceof ISupplicantP2pIfaceCallback)) {
            return (ISupplicantP2pIfaceCallback) iHwInterfaceQueryLocalInterface;
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

    static ISupplicantP2pIfaceCallback castFrom(IHwInterface iHwInterface) {
        if (iHwInterface == null) {
            return null;
        }
        return asInterface(iHwInterface.asBinder());
    }

    static ISupplicantP2pIfaceCallback getService(String str, boolean z) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, str, z));
    }

    static ISupplicantP2pIfaceCallback getService(boolean z) throws RemoteException {
        return getService(HalDeviceManager.HAL_INSTANCE_NAME, z);
    }

    static ISupplicantP2pIfaceCallback getService(String str) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, str));
    }

    static ISupplicantP2pIfaceCallback getService() throws RemoteException {
        return getService(HalDeviceManager.HAL_INSTANCE_NAME);
    }

    public static final class WpsDevPasswordId {
        public static final short DEFAULT = 0;
        public static final short MACHINE_SPECIFIED = 2;
        public static final short NFC_CONNECTION_HANDOVER = 7;
        public static final short P2PS_DEFAULT = 8;
        public static final short PUSHBUTTON = 4;
        public static final short REGISTRAR_SPECIFIED = 5;
        public static final short REKEY = 3;
        public static final short USER_SPECIFIED = 1;

        public static final String toString(short s) {
            if (s == 0) {
                return "DEFAULT";
            }
            if (s == 1) {
                return "USER_SPECIFIED";
            }
            if (s == 2) {
                return "MACHINE_SPECIFIED";
            }
            if (s == 3) {
                return "REKEY";
            }
            if (s == 4) {
                return "PUSHBUTTON";
            }
            if (s == 5) {
                return "REGISTRAR_SPECIFIED";
            }
            if (s == 7) {
                return "NFC_CONNECTION_HANDOVER";
            }
            if (s == 8) {
                return "P2PS_DEFAULT";
            }
            return "0x" + Integer.toHexString(Short.toUnsignedInt(s));
        }

        public static final String dumpBitfield(short s) {
            short s2;
            ArrayList arrayList = new ArrayList();
            arrayList.add("DEFAULT");
            if ((s & 1) == 1) {
                arrayList.add("USER_SPECIFIED");
                s2 = (short) 1;
            } else {
                s2 = 0;
            }
            if ((s & 2) == 2) {
                arrayList.add("MACHINE_SPECIFIED");
                s2 = (short) (s2 | 2);
            }
            if ((s & 3) == 3) {
                arrayList.add("REKEY");
                s2 = (short) (s2 | 3);
            }
            if ((s & 4) == 4) {
                arrayList.add("PUSHBUTTON");
                s2 = (short) (s2 | 4);
            }
            if ((s & 5) == 5) {
                arrayList.add("REGISTRAR_SPECIFIED");
                s2 = (short) (s2 | 5);
            }
            if ((s & 7) == 7) {
                arrayList.add("NFC_CONNECTION_HANDOVER");
                s2 = (short) (s2 | 7);
            }
            if ((s & 8) == 8) {
                arrayList.add("P2PS_DEFAULT");
                s2 = (short) (s2 | 8);
            }
            if (s != s2) {
                arrayList.add("0x" + Integer.toHexString(Short.toUnsignedInt((short) (s & (~s2)))));
            }
            return String.join(" | ", arrayList);
        }
    }

    public static final class P2pStatusCode {
        public static final int FAIL_BOTH_GO_INTENT_15 = 9;
        public static final int FAIL_INCOMPATIBLE_PARAMS = 2;
        public static final int FAIL_INCOMPATIBLE_PROV_METHOD = 10;
        public static final int FAIL_INFO_CURRENTLY_UNAVAILABLE = 1;
        public static final int FAIL_INVALID_PARAMS = 4;
        public static final int FAIL_LIMIT_REACHED = 3;
        public static final int FAIL_NO_COMMON_CHANNELS = 7;
        public static final int FAIL_PREV_PROTOCOL_ERROR = 6;
        public static final int FAIL_REJECTED_BY_USER = 11;
        public static final int FAIL_UNABLE_TO_ACCOMMODATE = 5;
        public static final int FAIL_UNKNOWN_GROUP = 8;
        public static final int SUCCESS = 0;
        public static final int SUCCESS_DEFERRED = 12;

        public static final String toString(int i) {
            if (i == 0) {
                return "SUCCESS";
            }
            if (i == 1) {
                return "FAIL_INFO_CURRENTLY_UNAVAILABLE";
            }
            if (i == 2) {
                return "FAIL_INCOMPATIBLE_PARAMS";
            }
            if (i == 3) {
                return "FAIL_LIMIT_REACHED";
            }
            if (i == 4) {
                return "FAIL_INVALID_PARAMS";
            }
            if (i == 5) {
                return "FAIL_UNABLE_TO_ACCOMMODATE";
            }
            if (i == 6) {
                return "FAIL_PREV_PROTOCOL_ERROR";
            }
            if (i == 7) {
                return "FAIL_NO_COMMON_CHANNELS";
            }
            if (i == 8) {
                return "FAIL_UNKNOWN_GROUP";
            }
            if (i == 9) {
                return "FAIL_BOTH_GO_INTENT_15";
            }
            if (i == 10) {
                return "FAIL_INCOMPATIBLE_PROV_METHOD";
            }
            if (i == 11) {
                return "FAIL_REJECTED_BY_USER";
            }
            if (i == 12) {
                return "SUCCESS_DEFERRED";
            }
            return "0x" + Integer.toHexString(i);
        }

        public static final String dumpBitfield(int i) {
            ArrayList arrayList = new ArrayList();
            arrayList.add("SUCCESS");
            int i2 = 1;
            if ((i & 1) == 1) {
                arrayList.add("FAIL_INFO_CURRENTLY_UNAVAILABLE");
            } else {
                i2 = 0;
            }
            if ((i & 2) == 2) {
                arrayList.add("FAIL_INCOMPATIBLE_PARAMS");
                i2 |= 2;
            }
            if ((i & 3) == 3) {
                arrayList.add("FAIL_LIMIT_REACHED");
                i2 |= 3;
            }
            if ((i & 4) == 4) {
                arrayList.add("FAIL_INVALID_PARAMS");
                i2 |= 4;
            }
            if ((i & 5) == 5) {
                arrayList.add("FAIL_UNABLE_TO_ACCOMMODATE");
                i2 |= 5;
            }
            if ((i & 6) == 6) {
                arrayList.add("FAIL_PREV_PROTOCOL_ERROR");
                i2 |= 6;
            }
            if ((i & 7) == 7) {
                arrayList.add("FAIL_NO_COMMON_CHANNELS");
                i2 |= 7;
            }
            if ((i & 8) == 8) {
                arrayList.add("FAIL_UNKNOWN_GROUP");
                i2 |= 8;
            }
            if ((i & 9) == 9) {
                arrayList.add("FAIL_BOTH_GO_INTENT_15");
                i2 |= 9;
            }
            if ((i & 10) == 10) {
                arrayList.add("FAIL_INCOMPATIBLE_PROV_METHOD");
                i2 |= 10;
            }
            if ((i & 11) == 11) {
                arrayList.add("FAIL_REJECTED_BY_USER");
                i2 |= 11;
            }
            if ((i & 12) == 12) {
                arrayList.add("SUCCESS_DEFERRED");
                i2 |= 12;
            }
            if (i != i2) {
                arrayList.add("0x" + Integer.toHexString(i & (~i2)));
            }
            return String.join(" | ", arrayList);
        }
    }

    public static final class P2pProvDiscStatusCode {
        public static final byte INFO_UNAVAILABLE = 4;
        public static final byte REJECTED = 2;
        public static final byte SUCCESS = 0;
        public static final byte TIMEOUT = 1;
        public static final byte TIMEOUT_JOIN = 3;

        public static final String toString(byte b) {
            if (b == 0) {
                return "SUCCESS";
            }
            if (b == 1) {
                return "TIMEOUT";
            }
            if (b == 2) {
                return "REJECTED";
            }
            if (b == 3) {
                return "TIMEOUT_JOIN";
            }
            if (b == 4) {
                return "INFO_UNAVAILABLE";
            }
            return "0x" + Integer.toHexString(Byte.toUnsignedInt(b));
        }

        public static final String dumpBitfield(byte b) {
            byte b2;
            ArrayList arrayList = new ArrayList();
            arrayList.add("SUCCESS");
            if ((b & 1) == 1) {
                arrayList.add("TIMEOUT");
                b2 = (byte) 1;
            } else {
                b2 = 0;
            }
            if ((b & 2) == 2) {
                arrayList.add("REJECTED");
                b2 = (byte) (b2 | 2);
            }
            if ((b & 3) == 3) {
                arrayList.add("TIMEOUT_JOIN");
                b2 = (byte) (b2 | 3);
            }
            if ((b & 4) == 4) {
                arrayList.add("INFO_UNAVAILABLE");
                b2 = (byte) (b2 | 4);
            }
            if (b != b2) {
                arrayList.add("0x" + Integer.toHexString(Byte.toUnsignedInt((byte) (b & (~b2)))));
            }
            return String.join(" | ", arrayList);
        }
    }

    public static final class Proxy implements ISupplicantP2pIfaceCallback {
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
                return "[class or subclass of android.hardware.wifi.supplicant@1.0::ISupplicantP2pIfaceCallback]@Proxy";
            }
        }

        public final boolean equals(Object obj) {
            return HidlSupport.interfacesEqual(this, obj);
        }

        public final int hashCode() {
            return asBinder().hashCode();
        }

        @Override
        public void onNetworkAdded(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantP2pIfaceCallback.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(1, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void onNetworkRemoved(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantP2pIfaceCallback.kInterfaceName);
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
        public void onDeviceFound(byte[] bArr, byte[] bArr2, byte[] bArr3, String str, short s, byte b, int i, byte[] bArr4) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantP2pIfaceCallback.kInterfaceName);
            HwBlob hwBlob = new HwBlob(6);
            hwBlob.putInt8Array(0L, bArr);
            hwParcel.writeBuffer(hwBlob);
            HwBlob hwBlob2 = new HwBlob(6);
            hwBlob2.putInt8Array(0L, bArr2);
            hwParcel.writeBuffer(hwBlob2);
            HwBlob hwBlob3 = new HwBlob(8);
            hwBlob3.putInt8Array(0L, bArr3);
            hwParcel.writeBuffer(hwBlob3);
            hwParcel.writeString(str);
            hwParcel.writeInt16(s);
            hwParcel.writeInt8(b);
            hwParcel.writeInt32(i);
            HwBlob hwBlob4 = new HwBlob(6);
            hwBlob4.putInt8Array(0L, bArr4);
            hwParcel.writeBuffer(hwBlob4);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(3, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void onDeviceLost(byte[] bArr) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantP2pIfaceCallback.kInterfaceName);
            HwBlob hwBlob = new HwBlob(6);
            hwBlob.putInt8Array(0L, bArr);
            hwParcel.writeBuffer(hwBlob);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(4, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void onFindStopped() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantP2pIfaceCallback.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(5, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void onGoNegotiationRequest(byte[] bArr, short s) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantP2pIfaceCallback.kInterfaceName);
            HwBlob hwBlob = new HwBlob(6);
            hwBlob.putInt8Array(0L, bArr);
            hwParcel.writeBuffer(hwBlob);
            hwParcel.writeInt16(s);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(6, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void onGoNegotiationCompleted(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantP2pIfaceCallback.kInterfaceName);
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
        public void onGroupFormationSuccess() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantP2pIfaceCallback.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(8, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void onGroupFormationFailure(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantP2pIfaceCallback.kInterfaceName);
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
        public void onGroupStarted(String str, boolean z, ArrayList<Byte> arrayList, int i, byte[] bArr, String str2, byte[] bArr2, boolean z2) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantP2pIfaceCallback.kInterfaceName);
            hwParcel.writeString(str);
            hwParcel.writeBool(z);
            hwParcel.writeInt8Vector(arrayList);
            hwParcel.writeInt32(i);
            HwBlob hwBlob = new HwBlob(32);
            hwBlob.putInt8Array(0L, bArr);
            hwParcel.writeBuffer(hwBlob);
            hwParcel.writeString(str2);
            HwBlob hwBlob2 = new HwBlob(6);
            hwBlob2.putInt8Array(0L, bArr2);
            hwParcel.writeBuffer(hwBlob2);
            hwParcel.writeBool(z2);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(10, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void onGroupRemoved(String str, boolean z) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantP2pIfaceCallback.kInterfaceName);
            hwParcel.writeString(str);
            hwParcel.writeBool(z);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(11, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void onInvitationReceived(byte[] bArr, byte[] bArr2, byte[] bArr3, int i, int i2) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantP2pIfaceCallback.kInterfaceName);
            HwBlob hwBlob = new HwBlob(6);
            hwBlob.putInt8Array(0L, bArr);
            hwParcel.writeBuffer(hwBlob);
            HwBlob hwBlob2 = new HwBlob(6);
            hwBlob2.putInt8Array(0L, bArr2);
            hwParcel.writeBuffer(hwBlob2);
            HwBlob hwBlob3 = new HwBlob(6);
            hwBlob3.putInt8Array(0L, bArr3);
            hwParcel.writeBuffer(hwBlob3);
            hwParcel.writeInt32(i);
            hwParcel.writeInt32(i2);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(12, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void onInvitationResult(byte[] bArr, int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantP2pIfaceCallback.kInterfaceName);
            HwBlob hwBlob = new HwBlob(6);
            hwBlob.putInt8Array(0L, bArr);
            hwParcel.writeBuffer(hwBlob);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(13, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void onProvisionDiscoveryCompleted(byte[] bArr, boolean z, byte b, short s, String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantP2pIfaceCallback.kInterfaceName);
            HwBlob hwBlob = new HwBlob(6);
            hwBlob.putInt8Array(0L, bArr);
            hwParcel.writeBuffer(hwBlob);
            hwParcel.writeBool(z);
            hwParcel.writeInt8(b);
            hwParcel.writeInt16(s);
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
        public void onServiceDiscoveryResponse(byte[] bArr, short s, ArrayList<Byte> arrayList) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantP2pIfaceCallback.kInterfaceName);
            HwBlob hwBlob = new HwBlob(6);
            hwBlob.putInt8Array(0L, bArr);
            hwParcel.writeBuffer(hwBlob);
            hwParcel.writeInt16(s);
            hwParcel.writeInt8Vector(arrayList);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(15, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void onStaAuthorized(byte[] bArr, byte[] bArr2) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantP2pIfaceCallback.kInterfaceName);
            HwBlob hwBlob = new HwBlob(6);
            hwBlob.putInt8Array(0L, bArr);
            hwParcel.writeBuffer(hwBlob);
            HwBlob hwBlob2 = new HwBlob(6);
            hwBlob2.putInt8Array(0L, bArr2);
            hwParcel.writeBuffer(hwBlob2);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(16, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void onStaDeauthorized(byte[] bArr, byte[] bArr2) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantP2pIfaceCallback.kInterfaceName);
            HwBlob hwBlob = new HwBlob(6);
            hwBlob.putInt8Array(0L, bArr);
            hwParcel.writeBuffer(hwBlob);
            HwBlob hwBlob2 = new HwBlob(6);
            hwBlob2.putInt8Array(0L, bArr2);
            hwParcel.writeBuffer(hwBlob2);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(17, hwParcel, hwParcel2, 1);
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

    public static abstract class Stub extends HwBinder implements ISupplicantP2pIfaceCallback {
        @Override
        public IHwBinder asBinder() {
            return this;
        }

        @Override
        public final ArrayList<String> interfaceChain() {
            return new ArrayList<>(Arrays.asList(ISupplicantP2pIfaceCallback.kInterfaceName, IBase.kInterfaceName));
        }

        @Override
        public final String interfaceDescriptor() {
            return ISupplicantP2pIfaceCallback.kInterfaceName;
        }

        @Override
        public final ArrayList<byte[]> getHashChain() {
            return new ArrayList<>(Arrays.asList(new byte[]{-117, 99, -11, -17, -94, -29, -66, 58, 124, -72, -92, 40, 118, 13, -126, 40, 90, 74, -73, -101, -53, -34, -90, -17, -112, -86, 84, 117, 85, -27, -126, -44}, new byte[]{-67, -38, -74, 24, 77, 122, 52, 109, -90, -96, 125, -64, -126, -116, -15, -102, 105, 111, 76, -86, 54, 17, -59, 31, 46, 20, 86, 90, 20, -76, 15, -39}));
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
            if (ISupplicantP2pIfaceCallback.kInterfaceName.equals(str)) {
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
                        hwParcel.enforceInterface(ISupplicantP2pIfaceCallback.kInterfaceName);
                        onNetworkAdded(hwParcel.readInt32());
                    }
                    break;
                case 2:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantP2pIfaceCallback.kInterfaceName);
                        onNetworkRemoved(hwParcel.readInt32());
                    }
                    break;
                case 3:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantP2pIfaceCallback.kInterfaceName);
                        byte[] bArr = new byte[6];
                        hwParcel.readBuffer(6L).copyToInt8Array(0L, bArr, 6);
                        byte[] bArr2 = new byte[6];
                        hwParcel.readBuffer(6L).copyToInt8Array(0L, bArr2, 6);
                        byte[] bArr3 = new byte[8];
                        hwParcel.readBuffer(8L).copyToInt8Array(0L, bArr3, 8);
                        String string = hwParcel.readString();
                        short int16 = hwParcel.readInt16();
                        byte int8 = hwParcel.readInt8();
                        int int32 = hwParcel.readInt32();
                        byte[] bArr4 = new byte[6];
                        hwParcel.readBuffer(6L).copyToInt8Array(0L, bArr4, 6);
                        onDeviceFound(bArr, bArr2, bArr3, string, int16, int8, int32, bArr4);
                    }
                    break;
                case 4:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantP2pIfaceCallback.kInterfaceName);
                        byte[] bArr5 = new byte[6];
                        hwParcel.readBuffer(6L).copyToInt8Array(0L, bArr5, 6);
                        onDeviceLost(bArr5);
                    }
                    break;
                case 5:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantP2pIfaceCallback.kInterfaceName);
                        onFindStopped();
                    }
                    break;
                case 6:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantP2pIfaceCallback.kInterfaceName);
                        byte[] bArr6 = new byte[6];
                        hwParcel.readBuffer(6L).copyToInt8Array(0L, bArr6, 6);
                        onGoNegotiationRequest(bArr6, hwParcel.readInt16());
                    }
                    break;
                case 7:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantP2pIfaceCallback.kInterfaceName);
                        onGoNegotiationCompleted(hwParcel.readInt32());
                    }
                    break;
                case 8:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantP2pIfaceCallback.kInterfaceName);
                        onGroupFormationSuccess();
                    }
                    break;
                case 9:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantP2pIfaceCallback.kInterfaceName);
                        onGroupFormationFailure(hwParcel.readString());
                    }
                    break;
                case 10:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantP2pIfaceCallback.kInterfaceName);
                        String string2 = hwParcel.readString();
                        boolean bool = hwParcel.readBool();
                        ArrayList<Byte> int8Vector = hwParcel.readInt8Vector();
                        int int322 = hwParcel.readInt32();
                        byte[] bArr7 = new byte[32];
                        hwParcel.readBuffer(32L).copyToInt8Array(0L, bArr7, 32);
                        String string3 = hwParcel.readString();
                        byte[] bArr8 = new byte[6];
                        hwParcel.readBuffer(6L).copyToInt8Array(0L, bArr8, 6);
                        onGroupStarted(string2, bool, int8Vector, int322, bArr7, string3, bArr8, hwParcel.readBool());
                    }
                    break;
                case 11:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantP2pIfaceCallback.kInterfaceName);
                        onGroupRemoved(hwParcel.readString(), hwParcel.readBool());
                    }
                    break;
                case 12:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantP2pIfaceCallback.kInterfaceName);
                        byte[] bArr9 = new byte[6];
                        hwParcel.readBuffer(6L).copyToInt8Array(0L, bArr9, 6);
                        byte[] bArr10 = new byte[6];
                        hwParcel.readBuffer(6L).copyToInt8Array(0L, bArr10, 6);
                        byte[] bArr11 = new byte[6];
                        hwParcel.readBuffer(6L).copyToInt8Array(0L, bArr11, 6);
                        onInvitationReceived(bArr9, bArr10, bArr11, hwParcel.readInt32(), hwParcel.readInt32());
                    }
                    break;
                case 13:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantP2pIfaceCallback.kInterfaceName);
                        byte[] bArr12 = new byte[6];
                        hwParcel.readBuffer(6L).copyToInt8Array(0L, bArr12, 6);
                        onInvitationResult(bArr12, hwParcel.readInt32());
                    }
                    break;
                case 14:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantP2pIfaceCallback.kInterfaceName);
                        byte[] bArr13 = new byte[6];
                        hwParcel.readBuffer(6L).copyToInt8Array(0L, bArr13, 6);
                        onProvisionDiscoveryCompleted(bArr13, hwParcel.readBool(), hwParcel.readInt8(), hwParcel.readInt16(), hwParcel.readString());
                    }
                    break;
                case 15:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantP2pIfaceCallback.kInterfaceName);
                        byte[] bArr14 = new byte[6];
                        hwParcel.readBuffer(6L).copyToInt8Array(0L, bArr14, 6);
                        onServiceDiscoveryResponse(bArr14, hwParcel.readInt16(), hwParcel.readInt8Vector());
                    }
                    break;
                case 16:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantP2pIfaceCallback.kInterfaceName);
                        byte[] bArr15 = new byte[6];
                        hwParcel.readBuffer(6L).copyToInt8Array(0L, bArr15, 6);
                        byte[] bArr16 = new byte[6];
                        hwParcel.readBuffer(6L).copyToInt8Array(0L, bArr16, 6);
                        onStaAuthorized(bArr15, bArr16);
                    }
                    break;
                case 17:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantP2pIfaceCallback.kInterfaceName);
                        byte[] bArr17 = new byte[6];
                        hwParcel.readBuffer(6L).copyToInt8Array(0L, bArr17, 6);
                        byte[] bArr18 = new byte[6];
                        hwParcel.readBuffer(6L).copyToInt8Array(0L, bArr18, 6);
                        onStaDeauthorized(bArr17, bArr18);
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
