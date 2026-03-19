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

public interface ISupplicantStaIfaceCallback extends IBase {
    public static final String kInterfaceName = "android.hardware.wifi.supplicant@1.0::ISupplicantStaIfaceCallback";

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

    void onAnqpQueryDone(byte[] bArr, AnqpData anqpData, Hs20AnqpData hs20AnqpData) throws RemoteException;

    void onAssociationRejected(byte[] bArr, int i, boolean z) throws RemoteException;

    void onAuthenticationTimeout(byte[] bArr) throws RemoteException;

    void onBssidChanged(byte b, byte[] bArr) throws RemoteException;

    void onDisconnected(byte[] bArr, boolean z, int i) throws RemoteException;

    void onEapFailure() throws RemoteException;

    void onExtRadioWorkStart(int i) throws RemoteException;

    void onExtRadioWorkTimeout(int i) throws RemoteException;

    void onHs20DeauthImminentNotice(byte[] bArr, int i, int i2, String str) throws RemoteException;

    void onHs20IconQueryDone(byte[] bArr, String str, ArrayList<Byte> arrayList) throws RemoteException;

    void onHs20SubscriptionRemediation(byte[] bArr, byte b, String str) throws RemoteException;

    void onNetworkAdded(int i) throws RemoteException;

    void onNetworkRemoved(int i) throws RemoteException;

    void onStateChanged(int i, byte[] bArr, int i2, ArrayList<Byte> arrayList) throws RemoteException;

    void onWpsEventFail(byte[] bArr, short s, short s2) throws RemoteException;

    void onWpsEventPbcOverlap() throws RemoteException;

    void onWpsEventSuccess() throws RemoteException;

    @Override
    void ping() throws RemoteException;

    @Override
    void setHALInstrumentation() throws RemoteException;

    @Override
    boolean unlinkToDeath(IHwBinder.DeathRecipient deathRecipient) throws RemoteException;

    static ISupplicantStaIfaceCallback asInterface(IHwBinder iHwBinder) {
        if (iHwBinder == null) {
            return null;
        }
        IHwInterface iHwInterfaceQueryLocalInterface = iHwBinder.queryLocalInterface(kInterfaceName);
        if (iHwInterfaceQueryLocalInterface != null && (iHwInterfaceQueryLocalInterface instanceof ISupplicantStaIfaceCallback)) {
            return (ISupplicantStaIfaceCallback) iHwInterfaceQueryLocalInterface;
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

    static ISupplicantStaIfaceCallback castFrom(IHwInterface iHwInterface) {
        if (iHwInterface == null) {
            return null;
        }
        return asInterface(iHwInterface.asBinder());
    }

    static ISupplicantStaIfaceCallback getService(String str, boolean z) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, str, z));
    }

    static ISupplicantStaIfaceCallback getService(boolean z) throws RemoteException {
        return getService(HalDeviceManager.HAL_INSTANCE_NAME, z);
    }

    static ISupplicantStaIfaceCallback getService(String str) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, str));
    }

    static ISupplicantStaIfaceCallback getService() throws RemoteException {
        return getService(HalDeviceManager.HAL_INSTANCE_NAME);
    }

    public static final class State {
        public static final int ASSOCIATED = 6;
        public static final int ASSOCIATING = 5;
        public static final int AUTHENTICATING = 4;
        public static final int COMPLETED = 9;
        public static final int DISCONNECTED = 0;
        public static final int FOURWAY_HANDSHAKE = 7;
        public static final int GROUP_HANDSHAKE = 8;
        public static final int IFACE_DISABLED = 1;
        public static final int INACTIVE = 2;
        public static final int SCANNING = 3;

        public static final String toString(int i) {
            if (i == 0) {
                return "DISCONNECTED";
            }
            if (i == 1) {
                return "IFACE_DISABLED";
            }
            if (i == 2) {
                return "INACTIVE";
            }
            if (i == 3) {
                return "SCANNING";
            }
            if (i == 4) {
                return "AUTHENTICATING";
            }
            if (i == 5) {
                return "ASSOCIATING";
            }
            if (i == 6) {
                return "ASSOCIATED";
            }
            if (i == 7) {
                return "FOURWAY_HANDSHAKE";
            }
            if (i == 8) {
                return "GROUP_HANDSHAKE";
            }
            if (i == 9) {
                return "COMPLETED";
            }
            return "0x" + Integer.toHexString(i);
        }

        public static final String dumpBitfield(int i) {
            ArrayList arrayList = new ArrayList();
            arrayList.add("DISCONNECTED");
            int i2 = 1;
            if ((i & 1) == 1) {
                arrayList.add("IFACE_DISABLED");
            } else {
                i2 = 0;
            }
            if ((i & 2) == 2) {
                arrayList.add("INACTIVE");
                i2 |= 2;
            }
            if ((i & 3) == 3) {
                arrayList.add("SCANNING");
                i2 |= 3;
            }
            if ((i & 4) == 4) {
                arrayList.add("AUTHENTICATING");
                i2 |= 4;
            }
            if ((i & 5) == 5) {
                arrayList.add("ASSOCIATING");
                i2 |= 5;
            }
            if ((i & 6) == 6) {
                arrayList.add("ASSOCIATED");
                i2 |= 6;
            }
            if ((i & 7) == 7) {
                arrayList.add("FOURWAY_HANDSHAKE");
                i2 |= 7;
            }
            if ((i & 8) == 8) {
                arrayList.add("GROUP_HANDSHAKE");
                i2 |= 8;
            }
            if ((i & 9) == 9) {
                arrayList.add("COMPLETED");
                i2 |= 9;
            }
            if (i != i2) {
                arrayList.add("0x" + Integer.toHexString(i & (~i2)));
            }
            return String.join(" | ", arrayList);
        }
    }

    public static final class OsuMethod {
        public static final byte OMA_DM = 0;
        public static final byte SOAP_XML_SPP = 1;

        public static final String toString(byte b) {
            if (b == 0) {
                return "OMA_DM";
            }
            if (b == 1) {
                return "SOAP_XML_SPP";
            }
            return "0x" + Integer.toHexString(Byte.toUnsignedInt(b));
        }

        public static final String dumpBitfield(byte b) {
            byte b2;
            ArrayList arrayList = new ArrayList();
            arrayList.add("OMA_DM");
            if ((b & 1) == 1) {
                arrayList.add("SOAP_XML_SPP");
                b2 = (byte) 1;
            } else {
                b2 = 0;
            }
            if (b != b2) {
                arrayList.add("0x" + Integer.toHexString(Byte.toUnsignedInt((byte) (b & (~b2)))));
            }
            return String.join(" | ", arrayList);
        }
    }

    public static final class AnqpData {
        public final ArrayList<Byte> venueName = new ArrayList<>();
        public final ArrayList<Byte> roamingConsortium = new ArrayList<>();
        public final ArrayList<Byte> ipAddrTypeAvailability = new ArrayList<>();
        public final ArrayList<Byte> naiRealm = new ArrayList<>();
        public final ArrayList<Byte> anqp3gppCellularNetwork = new ArrayList<>();
        public final ArrayList<Byte> domainName = new ArrayList<>();

        public final boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || obj.getClass() != AnqpData.class) {
                return false;
            }
            AnqpData anqpData = (AnqpData) obj;
            if (HidlSupport.deepEquals(this.venueName, anqpData.venueName) && HidlSupport.deepEquals(this.roamingConsortium, anqpData.roamingConsortium) && HidlSupport.deepEquals(this.ipAddrTypeAvailability, anqpData.ipAddrTypeAvailability) && HidlSupport.deepEquals(this.naiRealm, anqpData.naiRealm) && HidlSupport.deepEquals(this.anqp3gppCellularNetwork, anqpData.anqp3gppCellularNetwork) && HidlSupport.deepEquals(this.domainName, anqpData.domainName)) {
                return true;
            }
            return false;
        }

        public final int hashCode() {
            return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(this.venueName)), Integer.valueOf(HidlSupport.deepHashCode(this.roamingConsortium)), Integer.valueOf(HidlSupport.deepHashCode(this.ipAddrTypeAvailability)), Integer.valueOf(HidlSupport.deepHashCode(this.naiRealm)), Integer.valueOf(HidlSupport.deepHashCode(this.anqp3gppCellularNetwork)), Integer.valueOf(HidlSupport.deepHashCode(this.domainName)));
        }

        public final String toString() {
            return "{.venueName = " + this.venueName + ", .roamingConsortium = " + this.roamingConsortium + ", .ipAddrTypeAvailability = " + this.ipAddrTypeAvailability + ", .naiRealm = " + this.naiRealm + ", .anqp3gppCellularNetwork = " + this.anqp3gppCellularNetwork + ", .domainName = " + this.domainName + "}";
        }

        public final void readFromParcel(HwParcel hwParcel) {
            readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(96L), 0L);
        }

        public static final ArrayList<AnqpData> readVectorFromParcel(HwParcel hwParcel) {
            ArrayList<AnqpData> arrayList = new ArrayList<>();
            HwBlob buffer = hwParcel.readBuffer(16L);
            int int32 = buffer.getInt32(8L);
            HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 96, buffer.handle(), 0L, true);
            arrayList.clear();
            for (int i = 0; i < int32; i++) {
                AnqpData anqpData = new AnqpData();
                anqpData.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 96);
                arrayList.add(anqpData);
            }
            return arrayList;
        }

        public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
            long j2 = j + 0;
            int int32 = hwBlob.getInt32(j2 + 8);
            HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 1, hwBlob.handle(), j2 + 0, true);
            this.venueName.clear();
            for (int i = 0; i < int32; i++) {
                this.venueName.add(Byte.valueOf(embeddedBuffer.getInt8(i * 1)));
            }
            long j3 = j + 16;
            int int322 = hwBlob.getInt32(j3 + 8);
            HwBlob embeddedBuffer2 = hwParcel.readEmbeddedBuffer(int322 * 1, hwBlob.handle(), j3 + 0, true);
            this.roamingConsortium.clear();
            for (int i2 = 0; i2 < int322; i2++) {
                this.roamingConsortium.add(Byte.valueOf(embeddedBuffer2.getInt8(i2 * 1)));
            }
            long j4 = j + 32;
            int int323 = hwBlob.getInt32(j4 + 8);
            HwBlob embeddedBuffer3 = hwParcel.readEmbeddedBuffer(int323 * 1, hwBlob.handle(), j4 + 0, true);
            this.ipAddrTypeAvailability.clear();
            for (int i3 = 0; i3 < int323; i3++) {
                this.ipAddrTypeAvailability.add(Byte.valueOf(embeddedBuffer3.getInt8(i3 * 1)));
            }
            long j5 = j + 48;
            int int324 = hwBlob.getInt32(j5 + 8);
            HwBlob embeddedBuffer4 = hwParcel.readEmbeddedBuffer(int324 * 1, hwBlob.handle(), j5 + 0, true);
            this.naiRealm.clear();
            for (int i4 = 0; i4 < int324; i4++) {
                this.naiRealm.add(Byte.valueOf(embeddedBuffer4.getInt8(i4 * 1)));
            }
            long j6 = j + 64;
            int int325 = hwBlob.getInt32(j6 + 8);
            HwBlob embeddedBuffer5 = hwParcel.readEmbeddedBuffer(int325 * 1, hwBlob.handle(), j6 + 0, true);
            this.anqp3gppCellularNetwork.clear();
            for (int i5 = 0; i5 < int325; i5++) {
                this.anqp3gppCellularNetwork.add(Byte.valueOf(embeddedBuffer5.getInt8(i5 * 1)));
            }
            long j7 = j + 80;
            int int326 = hwBlob.getInt32(8 + j7);
            HwBlob embeddedBuffer6 = hwParcel.readEmbeddedBuffer(int326 * 1, hwBlob.handle(), j7 + 0, true);
            this.domainName.clear();
            for (int i6 = 0; i6 < int326; i6++) {
                this.domainName.add(Byte.valueOf(embeddedBuffer6.getInt8(i6 * 1)));
            }
        }

        public final void writeToParcel(HwParcel hwParcel) {
            HwBlob hwBlob = new HwBlob(96);
            writeEmbeddedToBlob(hwBlob, 0L);
            hwParcel.writeBuffer(hwBlob);
        }

        public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<AnqpData> arrayList) {
            HwBlob hwBlob = new HwBlob(16);
            int size = arrayList.size();
            hwBlob.putInt32(8L, size);
            hwBlob.putBool(12L, false);
            HwBlob hwBlob2 = new HwBlob(size * 96);
            for (int i = 0; i < size; i++) {
                arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 96);
            }
            hwBlob.putBlob(0L, hwBlob2);
            hwParcel.writeBuffer(hwBlob);
        }

        public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
            int size = this.venueName.size();
            long j2 = j + 0;
            hwBlob.putInt32(j2 + 8, size);
            hwBlob.putBool(j2 + 12, false);
            HwBlob hwBlob2 = new HwBlob(size * 1);
            for (int i = 0; i < size; i++) {
                hwBlob2.putInt8(i * 1, this.venueName.get(i).byteValue());
            }
            hwBlob.putBlob(j2 + 0, hwBlob2);
            int size2 = this.roamingConsortium.size();
            long j3 = j + 16;
            hwBlob.putInt32(j3 + 8, size2);
            hwBlob.putBool(j3 + 12, false);
            HwBlob hwBlob3 = new HwBlob(size2 * 1);
            for (int i2 = 0; i2 < size2; i2++) {
                hwBlob3.putInt8(i2 * 1, this.roamingConsortium.get(i2).byteValue());
            }
            hwBlob.putBlob(j3 + 0, hwBlob3);
            int size3 = this.ipAddrTypeAvailability.size();
            long j4 = j + 32;
            hwBlob.putInt32(j4 + 8, size3);
            hwBlob.putBool(j4 + 12, false);
            HwBlob hwBlob4 = new HwBlob(size3 * 1);
            for (int i3 = 0; i3 < size3; i3++) {
                hwBlob4.putInt8(i3 * 1, this.ipAddrTypeAvailability.get(i3).byteValue());
            }
            hwBlob.putBlob(j4 + 0, hwBlob4);
            int size4 = this.naiRealm.size();
            long j5 = j + 48;
            hwBlob.putInt32(j5 + 8, size4);
            hwBlob.putBool(j5 + 12, false);
            HwBlob hwBlob5 = new HwBlob(size4 * 1);
            for (int i4 = 0; i4 < size4; i4++) {
                hwBlob5.putInt8(i4 * 1, this.naiRealm.get(i4).byteValue());
            }
            hwBlob.putBlob(j5 + 0, hwBlob5);
            int size5 = this.anqp3gppCellularNetwork.size();
            long j6 = j + 64;
            hwBlob.putInt32(j6 + 8, size5);
            hwBlob.putBool(j6 + 12, false);
            HwBlob hwBlob6 = new HwBlob(size5 * 1);
            for (int i5 = 0; i5 < size5; i5++) {
                hwBlob6.putInt8(i5 * 1, this.anqp3gppCellularNetwork.get(i5).byteValue());
            }
            hwBlob.putBlob(j6 + 0, hwBlob6);
            int size6 = this.domainName.size();
            long j7 = j + 80;
            hwBlob.putInt32(8 + j7, size6);
            hwBlob.putBool(j7 + 12, false);
            HwBlob hwBlob7 = new HwBlob(size6 * 1);
            for (int i6 = 0; i6 < size6; i6++) {
                hwBlob7.putInt8(i6 * 1, this.domainName.get(i6).byteValue());
            }
            hwBlob.putBlob(j7 + 0, hwBlob7);
        }
    }

    public static final class Hs20AnqpData {
        public final ArrayList<Byte> operatorFriendlyName = new ArrayList<>();
        public final ArrayList<Byte> wanMetrics = new ArrayList<>();
        public final ArrayList<Byte> connectionCapability = new ArrayList<>();
        public final ArrayList<Byte> osuProvidersList = new ArrayList<>();

        public final boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || obj.getClass() != Hs20AnqpData.class) {
                return false;
            }
            Hs20AnqpData hs20AnqpData = (Hs20AnqpData) obj;
            if (HidlSupport.deepEquals(this.operatorFriendlyName, hs20AnqpData.operatorFriendlyName) && HidlSupport.deepEquals(this.wanMetrics, hs20AnqpData.wanMetrics) && HidlSupport.deepEquals(this.connectionCapability, hs20AnqpData.connectionCapability) && HidlSupport.deepEquals(this.osuProvidersList, hs20AnqpData.osuProvidersList)) {
                return true;
            }
            return false;
        }

        public final int hashCode() {
            return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(this.operatorFriendlyName)), Integer.valueOf(HidlSupport.deepHashCode(this.wanMetrics)), Integer.valueOf(HidlSupport.deepHashCode(this.connectionCapability)), Integer.valueOf(HidlSupport.deepHashCode(this.osuProvidersList)));
        }

        public final String toString() {
            return "{.operatorFriendlyName = " + this.operatorFriendlyName + ", .wanMetrics = " + this.wanMetrics + ", .connectionCapability = " + this.connectionCapability + ", .osuProvidersList = " + this.osuProvidersList + "}";
        }

        public final void readFromParcel(HwParcel hwParcel) {
            readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(64L), 0L);
        }

        public static final ArrayList<Hs20AnqpData> readVectorFromParcel(HwParcel hwParcel) {
            ArrayList<Hs20AnqpData> arrayList = new ArrayList<>();
            HwBlob buffer = hwParcel.readBuffer(16L);
            int int32 = buffer.getInt32(8L);
            HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 64, buffer.handle(), 0L, true);
            arrayList.clear();
            for (int i = 0; i < int32; i++) {
                Hs20AnqpData hs20AnqpData = new Hs20AnqpData();
                hs20AnqpData.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 64);
                arrayList.add(hs20AnqpData);
            }
            return arrayList;
        }

        public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
            long j2 = j + 0;
            int int32 = hwBlob.getInt32(j2 + 8);
            HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 1, hwBlob.handle(), j2 + 0, true);
            this.operatorFriendlyName.clear();
            for (int i = 0; i < int32; i++) {
                this.operatorFriendlyName.add(Byte.valueOf(embeddedBuffer.getInt8(i * 1)));
            }
            long j3 = j + 16;
            int int322 = hwBlob.getInt32(j3 + 8);
            HwBlob embeddedBuffer2 = hwParcel.readEmbeddedBuffer(int322 * 1, hwBlob.handle(), j3 + 0, true);
            this.wanMetrics.clear();
            for (int i2 = 0; i2 < int322; i2++) {
                this.wanMetrics.add(Byte.valueOf(embeddedBuffer2.getInt8(i2 * 1)));
            }
            long j4 = j + 32;
            int int323 = hwBlob.getInt32(j4 + 8);
            HwBlob embeddedBuffer3 = hwParcel.readEmbeddedBuffer(int323 * 1, hwBlob.handle(), j4 + 0, true);
            this.connectionCapability.clear();
            for (int i3 = 0; i3 < int323; i3++) {
                this.connectionCapability.add(Byte.valueOf(embeddedBuffer3.getInt8(i3 * 1)));
            }
            long j5 = j + 48;
            int int324 = hwBlob.getInt32(8 + j5);
            HwBlob embeddedBuffer4 = hwParcel.readEmbeddedBuffer(int324 * 1, hwBlob.handle(), j5 + 0, true);
            this.osuProvidersList.clear();
            for (int i4 = 0; i4 < int324; i4++) {
                this.osuProvidersList.add(Byte.valueOf(embeddedBuffer4.getInt8(i4 * 1)));
            }
        }

        public final void writeToParcel(HwParcel hwParcel) {
            HwBlob hwBlob = new HwBlob(64);
            writeEmbeddedToBlob(hwBlob, 0L);
            hwParcel.writeBuffer(hwBlob);
        }

        public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<Hs20AnqpData> arrayList) {
            HwBlob hwBlob = new HwBlob(16);
            int size = arrayList.size();
            hwBlob.putInt32(8L, size);
            hwBlob.putBool(12L, false);
            HwBlob hwBlob2 = new HwBlob(size * 64);
            for (int i = 0; i < size; i++) {
                arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 64);
            }
            hwBlob.putBlob(0L, hwBlob2);
            hwParcel.writeBuffer(hwBlob);
        }

        public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
            int size = this.operatorFriendlyName.size();
            long j2 = j + 0;
            hwBlob.putInt32(j2 + 8, size);
            hwBlob.putBool(j2 + 12, false);
            HwBlob hwBlob2 = new HwBlob(size * 1);
            for (int i = 0; i < size; i++) {
                hwBlob2.putInt8(i * 1, this.operatorFriendlyName.get(i).byteValue());
            }
            hwBlob.putBlob(j2 + 0, hwBlob2);
            int size2 = this.wanMetrics.size();
            long j3 = j + 16;
            hwBlob.putInt32(j3 + 8, size2);
            hwBlob.putBool(j3 + 12, false);
            HwBlob hwBlob3 = new HwBlob(size2 * 1);
            for (int i2 = 0; i2 < size2; i2++) {
                hwBlob3.putInt8(i2 * 1, this.wanMetrics.get(i2).byteValue());
            }
            hwBlob.putBlob(j3 + 0, hwBlob3);
            int size3 = this.connectionCapability.size();
            long j4 = j + 32;
            hwBlob.putInt32(j4 + 8, size3);
            hwBlob.putBool(j4 + 12, false);
            HwBlob hwBlob4 = new HwBlob(size3 * 1);
            for (int i3 = 0; i3 < size3; i3++) {
                hwBlob4.putInt8(i3 * 1, this.connectionCapability.get(i3).byteValue());
            }
            hwBlob.putBlob(j4 + 0, hwBlob4);
            int size4 = this.osuProvidersList.size();
            long j5 = j + 48;
            hwBlob.putInt32(8 + j5, size4);
            hwBlob.putBool(j5 + 12, false);
            HwBlob hwBlob5 = new HwBlob(size4 * 1);
            for (int i4 = 0; i4 < size4; i4++) {
                hwBlob5.putInt8(i4 * 1, this.osuProvidersList.get(i4).byteValue());
            }
            hwBlob.putBlob(j5 + 0, hwBlob5);
        }
    }

    public static final class WpsConfigError {
        public static final short CHAN_24_NOT_SUPPORTED = 3;
        public static final short CHAN_50_NOT_SUPPORTED = 4;
        public static final short CHAN_60G_NOT_SUPPORTED = 19;
        public static final short DECRYPTION_CRC_FAILURE = 2;
        public static final short DEVICE_BUSY = 14;
        public static final short DEV_PASSWORD_AUTH_FAILURE = 18;
        public static final short FAILED_DHCP_CONFIG = 9;
        public static final short IP_ADDR_CONFLICT = 10;
        public static final short MSG_TIMEOUT = 16;
        public static final short MULTIPLE_PBC_DETECTED = 12;
        public static final short NETWORK_ASSOC_FAILURE = 7;
        public static final short NETWORK_AUTH_FAILURE = 6;
        public static final short NO_CONN_TO_REGISTRAR = 11;
        public static final short NO_DHCP_RESPONSE = 8;
        public static final short NO_ERROR = 0;
        public static final short OOB_IFACE_READ_ERROR = 1;
        public static final short PUBLIC_KEY_HASH_MISMATCH = 20;
        public static final short REG_SESS_TIMEOUT = 17;
        public static final short ROGUE_SUSPECTED = 13;
        public static final short SETUP_LOCKED = 15;
        public static final short SIGNAL_TOO_WEAK = 5;

        public static final String toString(short s) {
            if (s == 0) {
                return "NO_ERROR";
            }
            if (s == 1) {
                return "OOB_IFACE_READ_ERROR";
            }
            if (s == 2) {
                return "DECRYPTION_CRC_FAILURE";
            }
            if (s == 3) {
                return "CHAN_24_NOT_SUPPORTED";
            }
            if (s == 4) {
                return "CHAN_50_NOT_SUPPORTED";
            }
            if (s == 5) {
                return "SIGNAL_TOO_WEAK";
            }
            if (s == 6) {
                return "NETWORK_AUTH_FAILURE";
            }
            if (s == 7) {
                return "NETWORK_ASSOC_FAILURE";
            }
            if (s == 8) {
                return "NO_DHCP_RESPONSE";
            }
            if (s == 9) {
                return "FAILED_DHCP_CONFIG";
            }
            if (s == 10) {
                return "IP_ADDR_CONFLICT";
            }
            if (s == 11) {
                return "NO_CONN_TO_REGISTRAR";
            }
            if (s == 12) {
                return "MULTIPLE_PBC_DETECTED";
            }
            if (s == 13) {
                return "ROGUE_SUSPECTED";
            }
            if (s == 14) {
                return "DEVICE_BUSY";
            }
            if (s == 15) {
                return "SETUP_LOCKED";
            }
            if (s == 16) {
                return "MSG_TIMEOUT";
            }
            if (s == 17) {
                return "REG_SESS_TIMEOUT";
            }
            if (s == 18) {
                return "DEV_PASSWORD_AUTH_FAILURE";
            }
            if (s == 19) {
                return "CHAN_60G_NOT_SUPPORTED";
            }
            if (s == 20) {
                return "PUBLIC_KEY_HASH_MISMATCH";
            }
            return "0x" + Integer.toHexString(Short.toUnsignedInt(s));
        }

        public static final String dumpBitfield(short s) {
            short s2;
            ArrayList arrayList = new ArrayList();
            arrayList.add("NO_ERROR");
            if ((s & 1) == 1) {
                arrayList.add("OOB_IFACE_READ_ERROR");
                s2 = (short) 1;
            } else {
                s2 = 0;
            }
            if ((s & 2) == 2) {
                arrayList.add("DECRYPTION_CRC_FAILURE");
                s2 = (short) (s2 | 2);
            }
            if ((s & 3) == 3) {
                arrayList.add("CHAN_24_NOT_SUPPORTED");
                s2 = (short) (s2 | 3);
            }
            if ((s & 4) == 4) {
                arrayList.add("CHAN_50_NOT_SUPPORTED");
                s2 = (short) (s2 | 4);
            }
            if ((s & 5) == 5) {
                arrayList.add("SIGNAL_TOO_WEAK");
                s2 = (short) (s2 | 5);
            }
            if ((s & 6) == 6) {
                arrayList.add("NETWORK_AUTH_FAILURE");
                s2 = (short) (s2 | 6);
            }
            if ((s & 7) == 7) {
                arrayList.add("NETWORK_ASSOC_FAILURE");
                s2 = (short) (s2 | 7);
            }
            if ((s & 8) == 8) {
                arrayList.add("NO_DHCP_RESPONSE");
                s2 = (short) (s2 | 8);
            }
            if ((s & 9) == 9) {
                arrayList.add("FAILED_DHCP_CONFIG");
                s2 = (short) (s2 | 9);
            }
            if ((s & 10) == 10) {
                arrayList.add("IP_ADDR_CONFLICT");
                s2 = (short) (s2 | 10);
            }
            if ((s & 11) == 11) {
                arrayList.add("NO_CONN_TO_REGISTRAR");
                s2 = (short) (s2 | 11);
            }
            if ((s & 12) == 12) {
                arrayList.add("MULTIPLE_PBC_DETECTED");
                s2 = (short) (s2 | 12);
            }
            if ((s & 13) == 13) {
                arrayList.add("ROGUE_SUSPECTED");
                s2 = (short) (s2 | 13);
            }
            if ((s & 14) == 14) {
                arrayList.add("DEVICE_BUSY");
                s2 = (short) (s2 | 14);
            }
            if ((s & 15) == 15) {
                arrayList.add("SETUP_LOCKED");
                s2 = (short) (s2 | 15);
            }
            if ((s & 16) == 16) {
                arrayList.add("MSG_TIMEOUT");
                s2 = (short) (s2 | 16);
            }
            if ((s & 17) == 17) {
                arrayList.add("REG_SESS_TIMEOUT");
                s2 = (short) (s2 | 17);
            }
            if ((s & 18) == 18) {
                arrayList.add("DEV_PASSWORD_AUTH_FAILURE");
                s2 = (short) (s2 | 18);
            }
            if ((s & 19) == 19) {
                arrayList.add("CHAN_60G_NOT_SUPPORTED");
                s2 = (short) (s2 | 19);
            }
            if ((s & 20) == 20) {
                arrayList.add("PUBLIC_KEY_HASH_MISMATCH");
                s2 = (short) (s2 | 20);
            }
            if (s != s2) {
                arrayList.add("0x" + Integer.toHexString(Short.toUnsignedInt((short) (s & (~s2)))));
            }
            return String.join(" | ", arrayList);
        }
    }

    public static final class WpsErrorIndication {
        public static final short AUTH_FAILURE = 3;
        public static final short NO_ERROR = 0;
        public static final short SECURITY_TKIP_ONLY_PROHIBITED = 1;
        public static final short SECURITY_WEP_PROHIBITED = 2;

        public static final String toString(short s) {
            if (s == 0) {
                return "NO_ERROR";
            }
            if (s == 1) {
                return "SECURITY_TKIP_ONLY_PROHIBITED";
            }
            if (s == 2) {
                return "SECURITY_WEP_PROHIBITED";
            }
            if (s == 3) {
                return "AUTH_FAILURE";
            }
            return "0x" + Integer.toHexString(Short.toUnsignedInt(s));
        }

        public static final String dumpBitfield(short s) {
            short s2;
            ArrayList arrayList = new ArrayList();
            arrayList.add("NO_ERROR");
            if ((s & 1) == 1) {
                arrayList.add("SECURITY_TKIP_ONLY_PROHIBITED");
                s2 = (short) 1;
            } else {
                s2 = 0;
            }
            if ((s & 2) == 2) {
                arrayList.add("SECURITY_WEP_PROHIBITED");
                s2 = (short) (s2 | 2);
            }
            if ((s & 3) == 3) {
                arrayList.add("AUTH_FAILURE");
                s2 = (short) (s2 | 3);
            }
            if (s != s2) {
                arrayList.add("0x" + Integer.toHexString(Short.toUnsignedInt((short) (s & (~s2)))));
            }
            return String.join(" | ", arrayList);
        }
    }

    public static final class StatusCode {
        public static final int ADV_SRV_UNREACHABLE = 65;
        public static final int AKMP_NOT_VALID = 43;
        public static final int ANTI_CLOGGING_TOKEN_REQ = 76;
        public static final int AP_UNABLE_TO_HANDLE_NEW_STA = 17;
        public static final int ASSOC_DENIED_LISTEN_INT_TOO_LARGE = 51;
        public static final int ASSOC_DENIED_NOSHORT = 19;
        public static final int ASSOC_DENIED_NO_HT = 27;
        public static final int ASSOC_DENIED_NO_PCO = 29;
        public static final int ASSOC_DENIED_NO_SHORT_SLOT_TIME = 25;
        public static final int ASSOC_DENIED_NO_VHT = 104;
        public static final int ASSOC_DENIED_RATES = 18;
        public static final int ASSOC_DENIED_UNSPEC = 12;
        public static final int ASSOC_REJECTED_TEMPORARILY = 30;
        public static final int AUTHORIZATION_DEENABLED = 107;
        public static final int AUTH_TIMEOUT = 16;
        public static final int BAD_INTERVAL_WITH_U_APSD_COEX = 75;
        public static final int CANNOT_FIND_ALT_TBTT = 78;
        public static final int CAPS_UNSUPPORTED = 10;
        public static final int CHALLENGE_FAIL = 15;
        public static final int CIPHER_REJECTED_PER_POLICY = 46;
        public static final int DENIED_DUE_TO_SPECTRUM_MANAGEMENT = 103;
        public static final int DENIED_INSUFFICIENT_BANDWIDTH = 33;
        public static final int DENIED_POOR_CHANNEL_CONDITIONS = 34;
        public static final int DENIED_QOS_NOT_SUPPORTED = 35;
        public static final int DENIED_WITH_SUGGESTED_BAND_AND_CHANNEL = 99;
        public static final int DEST_STA_NOT_PRESENT = 49;
        public static final int DEST_STA_NOT_QOS_STA = 50;
        public static final int DIRECT_LINK_NOT_ALLOWED = 48;
        public static final int ENABLEMENT_DENIED = 105;
        public static final int FILS_AUTHENTICATION_FAILURE = 112;
        public static final int FINITE_CYCLIC_GROUP_NOT_SUPPORTED = 77;
        public static final int GAS_ADV_PROTO_NOT_SUPPORTED = 59;
        public static final int GAS_RESP_LARGER_THAN_LIMIT = 63;
        public static final int GAS_RESP_NOT_RECEIVED = 61;
        public static final int GROUP_CIPHER_NOT_VALID = 41;
        public static final int INSUFFICIENT_TCLAS_PROCESSING_RESOURCES = 57;
        public static final int INVALID_FTIE = 55;
        public static final int INVALID_FT_ACTION_FRAME_COUNT = 52;
        public static final int INVALID_IE = 40;
        public static final int INVALID_MDIE = 54;
        public static final int INVALID_PARAMETERS = 38;
        public static final int INVALID_PMKID = 53;
        public static final int INVALID_RSNIE = 72;
        public static final int INVALID_RSN_IE_CAPAB = 45;
        public static final int MAF_LIMIT_EXCEEDED = 101;
        public static final int MCCAOP_RESERVATION_CONFLICT = 100;
        public static final int MCCA_TRACK_LIMIT_EXCEEDED = 102;
        public static final int NOT_IN_SAME_BSS = 7;
        public static final int NOT_SUPPORTED_AUTH_ALG = 13;
        public static final int NO_OUTSTANDING_GAS_REQ = 60;
        public static final int PAIRWISE_CIPHER_NOT_VALID = 42;
        public static final int PENDING_ADMITTING_FST_SESSION = 86;
        public static final int PENDING_GAP_IN_BA_WINDOW = 88;
        public static final int PERFORMING_FST_NOW = 87;
        public static final int PWR_CAPABILITY_NOT_VALID = 23;
        public static final int QUERY_RESP_OUTSTANDING = 95;
        public static final int R0KH_UNREACHABLE = 28;
        public static final int REASSOC_NO_ASSOC = 11;
        public static final int REFUSED_AP_OUT_OF_MEMORY = 93;
        public static final int REFUSED_EXTERNAL_REASON = 92;
        public static final int REJECTED_EMERGENCY_SERVICE_NOT_SUPPORTED = 94;
        public static final int REJECTED_WITH_SUGGESTED_BSS_TRANSITION = 82;
        public static final int REJECTED_WITH_SUGGESTED_CHANGES = 39;
        public static final int REJECT_DSE_BAND = 96;
        public static final int REJECT_NO_WAKEUP_SPECIFIED = 84;
        public static final int REJECT_U_PID_SETTING = 89;
        public static final int REJECT_WITH_SCHEDULE = 83;
        public static final int REQUESTED_TCLAS_NOT_SUPPORTED = 56;
        public static final int REQUEST_DECLINED = 37;
        public static final int REQ_REFUSED_HOME = 64;
        public static final int REQ_REFUSED_SSPN = 67;
        public static final int REQ_REFUSED_UNAUTH_ACCESS = 68;
        public static final int REQ_TCLAS_NOT_SUPPORTED = 80;
        public static final int RESTRICTION_FROM_AUTHORIZED_GDB = 106;
        public static final int ROBUST_MGMT_FRAME_POLICY_VIOLATION = 31;
        public static final int SECURITY_DISABLED = 5;
        public static final int SPEC_MGMT_REQUIRED = 22;
        public static final int STA_TIMED_OUT_WAITING_FOR_GAS_RESP = 62;
        public static final int SUCCESS = 0;
        public static final int SUCCESS_POWER_SAVE_MODE = 85;
        public static final int SUPPORTED_CHANNEL_NOT_VALID = 24;
        public static final int TCLAS_PROCESSING_TERMINATED = 97;
        public static final int TCLAS_RESOURCES_EXCHAUSTED = 81;
        public static final int TDLS_WAKEUP_ALTERNATE = 2;
        public static final int TDLS_WAKEUP_REJECT = 3;
        public static final int TRANSMISSION_FAILURE = 79;
        public static final int TRY_ANOTHER_BSS = 58;
        public static final int TS_NOT_CREATED = 47;
        public static final int TS_SCHEDULE_CONFLICT = 98;
        public static final int UNACCEPTABLE_LIFETIME = 6;
        public static final int UNKNOWN_AUTHENTICATION_SERVER = 113;
        public static final int UNKNOWN_AUTH_TRANSACTION = 14;
        public static final int UNSPECIFIED_FAILURE = 1;
        public static final int UNSPECIFIED_QOS_FAILURE = 32;
        public static final int UNSUPPORTED_RSN_IE_VERSION = 44;
        public static final int U_APSD_COEX_MODE_NOT_SUPPORTED = 74;
        public static final int U_APSD_COEX_NOT_SUPPORTED = 73;

        public static final String toString(int i) {
            if (i == 0) {
                return "SUCCESS";
            }
            if (i == 1) {
                return "UNSPECIFIED_FAILURE";
            }
            if (i == 2) {
                return "TDLS_WAKEUP_ALTERNATE";
            }
            if (i == 3) {
                return "TDLS_WAKEUP_REJECT";
            }
            if (i == 5) {
                return "SECURITY_DISABLED";
            }
            if (i == 6) {
                return "UNACCEPTABLE_LIFETIME";
            }
            if (i == 7) {
                return "NOT_IN_SAME_BSS";
            }
            if (i == 10) {
                return "CAPS_UNSUPPORTED";
            }
            if (i == 11) {
                return "REASSOC_NO_ASSOC";
            }
            if (i == 12) {
                return "ASSOC_DENIED_UNSPEC";
            }
            if (i == 13) {
                return "NOT_SUPPORTED_AUTH_ALG";
            }
            if (i == 14) {
                return "UNKNOWN_AUTH_TRANSACTION";
            }
            if (i == 15) {
                return "CHALLENGE_FAIL";
            }
            if (i == 16) {
                return "AUTH_TIMEOUT";
            }
            if (i == 17) {
                return "AP_UNABLE_TO_HANDLE_NEW_STA";
            }
            if (i == 18) {
                return "ASSOC_DENIED_RATES";
            }
            if (i == 19) {
                return "ASSOC_DENIED_NOSHORT";
            }
            if (i == 22) {
                return "SPEC_MGMT_REQUIRED";
            }
            if (i == 23) {
                return "PWR_CAPABILITY_NOT_VALID";
            }
            if (i == 24) {
                return "SUPPORTED_CHANNEL_NOT_VALID";
            }
            if (i == 25) {
                return "ASSOC_DENIED_NO_SHORT_SLOT_TIME";
            }
            if (i == 27) {
                return "ASSOC_DENIED_NO_HT";
            }
            if (i == 28) {
                return "R0KH_UNREACHABLE";
            }
            if (i == 29) {
                return "ASSOC_DENIED_NO_PCO";
            }
            if (i == 30) {
                return "ASSOC_REJECTED_TEMPORARILY";
            }
            if (i == 31) {
                return "ROBUST_MGMT_FRAME_POLICY_VIOLATION";
            }
            if (i == 32) {
                return "UNSPECIFIED_QOS_FAILURE";
            }
            if (i == 33) {
                return "DENIED_INSUFFICIENT_BANDWIDTH";
            }
            if (i == 34) {
                return "DENIED_POOR_CHANNEL_CONDITIONS";
            }
            if (i == 35) {
                return "DENIED_QOS_NOT_SUPPORTED";
            }
            if (i == 37) {
                return "REQUEST_DECLINED";
            }
            if (i == 38) {
                return "INVALID_PARAMETERS";
            }
            if (i == 39) {
                return "REJECTED_WITH_SUGGESTED_CHANGES";
            }
            if (i == 40) {
                return "INVALID_IE";
            }
            if (i == 41) {
                return "GROUP_CIPHER_NOT_VALID";
            }
            if (i == 42) {
                return "PAIRWISE_CIPHER_NOT_VALID";
            }
            if (i == 43) {
                return "AKMP_NOT_VALID";
            }
            if (i == 44) {
                return "UNSUPPORTED_RSN_IE_VERSION";
            }
            if (i == 45) {
                return "INVALID_RSN_IE_CAPAB";
            }
            if (i == 46) {
                return "CIPHER_REJECTED_PER_POLICY";
            }
            if (i == 47) {
                return "TS_NOT_CREATED";
            }
            if (i == 48) {
                return "DIRECT_LINK_NOT_ALLOWED";
            }
            if (i == 49) {
                return "DEST_STA_NOT_PRESENT";
            }
            if (i == 50) {
                return "DEST_STA_NOT_QOS_STA";
            }
            if (i == 51) {
                return "ASSOC_DENIED_LISTEN_INT_TOO_LARGE";
            }
            if (i == 52) {
                return "INVALID_FT_ACTION_FRAME_COUNT";
            }
            if (i == 53) {
                return "INVALID_PMKID";
            }
            if (i == 54) {
                return "INVALID_MDIE";
            }
            if (i == 55) {
                return "INVALID_FTIE";
            }
            if (i == 56) {
                return "REQUESTED_TCLAS_NOT_SUPPORTED";
            }
            if (i == 57) {
                return "INSUFFICIENT_TCLAS_PROCESSING_RESOURCES";
            }
            if (i == 58) {
                return "TRY_ANOTHER_BSS";
            }
            if (i == 59) {
                return "GAS_ADV_PROTO_NOT_SUPPORTED";
            }
            if (i == 60) {
                return "NO_OUTSTANDING_GAS_REQ";
            }
            if (i == 61) {
                return "GAS_RESP_NOT_RECEIVED";
            }
            if (i == 62) {
                return "STA_TIMED_OUT_WAITING_FOR_GAS_RESP";
            }
            if (i == 63) {
                return "GAS_RESP_LARGER_THAN_LIMIT";
            }
            if (i == 64) {
                return "REQ_REFUSED_HOME";
            }
            if (i == 65) {
                return "ADV_SRV_UNREACHABLE";
            }
            if (i == 67) {
                return "REQ_REFUSED_SSPN";
            }
            if (i == 68) {
                return "REQ_REFUSED_UNAUTH_ACCESS";
            }
            if (i == 72) {
                return "INVALID_RSNIE";
            }
            if (i == 73) {
                return "U_APSD_COEX_NOT_SUPPORTED";
            }
            if (i == 74) {
                return "U_APSD_COEX_MODE_NOT_SUPPORTED";
            }
            if (i == 75) {
                return "BAD_INTERVAL_WITH_U_APSD_COEX";
            }
            if (i == 76) {
                return "ANTI_CLOGGING_TOKEN_REQ";
            }
            if (i == 77) {
                return "FINITE_CYCLIC_GROUP_NOT_SUPPORTED";
            }
            if (i == 78) {
                return "CANNOT_FIND_ALT_TBTT";
            }
            if (i == 79) {
                return "TRANSMISSION_FAILURE";
            }
            if (i == 80) {
                return "REQ_TCLAS_NOT_SUPPORTED";
            }
            if (i == 81) {
                return "TCLAS_RESOURCES_EXCHAUSTED";
            }
            if (i == 82) {
                return "REJECTED_WITH_SUGGESTED_BSS_TRANSITION";
            }
            if (i == 83) {
                return "REJECT_WITH_SCHEDULE";
            }
            if (i == 84) {
                return "REJECT_NO_WAKEUP_SPECIFIED";
            }
            if (i == 85) {
                return "SUCCESS_POWER_SAVE_MODE";
            }
            if (i == 86) {
                return "PENDING_ADMITTING_FST_SESSION";
            }
            if (i == 87) {
                return "PERFORMING_FST_NOW";
            }
            if (i == 88) {
                return "PENDING_GAP_IN_BA_WINDOW";
            }
            if (i == 89) {
                return "REJECT_U_PID_SETTING";
            }
            if (i == 92) {
                return "REFUSED_EXTERNAL_REASON";
            }
            if (i == 93) {
                return "REFUSED_AP_OUT_OF_MEMORY";
            }
            if (i == 94) {
                return "REJECTED_EMERGENCY_SERVICE_NOT_SUPPORTED";
            }
            if (i == 95) {
                return "QUERY_RESP_OUTSTANDING";
            }
            if (i == 96) {
                return "REJECT_DSE_BAND";
            }
            if (i == 97) {
                return "TCLAS_PROCESSING_TERMINATED";
            }
            if (i == 98) {
                return "TS_SCHEDULE_CONFLICT";
            }
            if (i == 99) {
                return "DENIED_WITH_SUGGESTED_BAND_AND_CHANNEL";
            }
            if (i == 100) {
                return "MCCAOP_RESERVATION_CONFLICT";
            }
            if (i == 101) {
                return "MAF_LIMIT_EXCEEDED";
            }
            if (i == 102) {
                return "MCCA_TRACK_LIMIT_EXCEEDED";
            }
            if (i == 103) {
                return "DENIED_DUE_TO_SPECTRUM_MANAGEMENT";
            }
            if (i == 104) {
                return "ASSOC_DENIED_NO_VHT";
            }
            if (i == 105) {
                return "ENABLEMENT_DENIED";
            }
            if (i == 106) {
                return "RESTRICTION_FROM_AUTHORIZED_GDB";
            }
            if (i == 107) {
                return "AUTHORIZATION_DEENABLED";
            }
            if (i == 112) {
                return "FILS_AUTHENTICATION_FAILURE";
            }
            if (i == 113) {
                return "UNKNOWN_AUTHENTICATION_SERVER";
            }
            return "0x" + Integer.toHexString(i);
        }

        public static final String dumpBitfield(int i) {
            ArrayList arrayList = new ArrayList();
            arrayList.add("SUCCESS");
            int i2 = 1;
            if ((i & 1) == 1) {
                arrayList.add("UNSPECIFIED_FAILURE");
            } else {
                i2 = 0;
            }
            if ((i & 2) == 2) {
                arrayList.add("TDLS_WAKEUP_ALTERNATE");
                i2 |= 2;
            }
            if ((i & 3) == 3) {
                arrayList.add("TDLS_WAKEUP_REJECT");
                i2 |= 3;
            }
            if ((i & 5) == 5) {
                arrayList.add("SECURITY_DISABLED");
                i2 |= 5;
            }
            if ((i & 6) == 6) {
                arrayList.add("UNACCEPTABLE_LIFETIME");
                i2 |= 6;
            }
            if ((i & 7) == 7) {
                arrayList.add("NOT_IN_SAME_BSS");
                i2 |= 7;
            }
            if ((i & 10) == 10) {
                arrayList.add("CAPS_UNSUPPORTED");
                i2 |= 10;
            }
            if ((i & 11) == 11) {
                arrayList.add("REASSOC_NO_ASSOC");
                i2 |= 11;
            }
            if ((i & 12) == 12) {
                arrayList.add("ASSOC_DENIED_UNSPEC");
                i2 |= 12;
            }
            if ((i & 13) == 13) {
                arrayList.add("NOT_SUPPORTED_AUTH_ALG");
                i2 |= 13;
            }
            if ((i & 14) == 14) {
                arrayList.add("UNKNOWN_AUTH_TRANSACTION");
                i2 |= 14;
            }
            if ((i & 15) == 15) {
                arrayList.add("CHALLENGE_FAIL");
                i2 |= 15;
            }
            if ((i & 16) == 16) {
                arrayList.add("AUTH_TIMEOUT");
                i2 |= 16;
            }
            if ((i & 17) == 17) {
                arrayList.add("AP_UNABLE_TO_HANDLE_NEW_STA");
                i2 |= 17;
            }
            if ((i & 18) == 18) {
                arrayList.add("ASSOC_DENIED_RATES");
                i2 |= 18;
            }
            if ((i & 19) == 19) {
                arrayList.add("ASSOC_DENIED_NOSHORT");
                i2 |= 19;
            }
            if ((i & 22) == 22) {
                arrayList.add("SPEC_MGMT_REQUIRED");
                i2 |= 22;
            }
            if ((i & 23) == 23) {
                arrayList.add("PWR_CAPABILITY_NOT_VALID");
                i2 |= 23;
            }
            if ((i & 24) == 24) {
                arrayList.add("SUPPORTED_CHANNEL_NOT_VALID");
                i2 |= 24;
            }
            if ((i & 25) == 25) {
                arrayList.add("ASSOC_DENIED_NO_SHORT_SLOT_TIME");
                i2 |= 25;
            }
            if ((i & 27) == 27) {
                arrayList.add("ASSOC_DENIED_NO_HT");
                i2 |= 27;
            }
            if ((i & 28) == 28) {
                arrayList.add("R0KH_UNREACHABLE");
                i2 |= 28;
            }
            if ((i & 29) == 29) {
                arrayList.add("ASSOC_DENIED_NO_PCO");
                i2 |= 29;
            }
            if ((i & 30) == 30) {
                arrayList.add("ASSOC_REJECTED_TEMPORARILY");
                i2 |= 30;
            }
            if ((i & 31) == 31) {
                arrayList.add("ROBUST_MGMT_FRAME_POLICY_VIOLATION");
                i2 |= 31;
            }
            if ((i & 32) == 32) {
                arrayList.add("UNSPECIFIED_QOS_FAILURE");
                i2 |= 32;
            }
            if ((i & 33) == 33) {
                arrayList.add("DENIED_INSUFFICIENT_BANDWIDTH");
                i2 |= 33;
            }
            if ((i & 34) == 34) {
                arrayList.add("DENIED_POOR_CHANNEL_CONDITIONS");
                i2 |= 34;
            }
            if ((i & 35) == 35) {
                arrayList.add("DENIED_QOS_NOT_SUPPORTED");
                i2 |= 35;
            }
            if ((i & 37) == 37) {
                arrayList.add("REQUEST_DECLINED");
                i2 |= 37;
            }
            if ((i & 38) == 38) {
                arrayList.add("INVALID_PARAMETERS");
                i2 |= 38;
            }
            if ((i & 39) == 39) {
                arrayList.add("REJECTED_WITH_SUGGESTED_CHANGES");
                i2 |= 39;
            }
            if ((i & 40) == 40) {
                arrayList.add("INVALID_IE");
                i2 |= 40;
            }
            if ((i & 41) == 41) {
                arrayList.add("GROUP_CIPHER_NOT_VALID");
                i2 |= 41;
            }
            if ((i & 42) == 42) {
                arrayList.add("PAIRWISE_CIPHER_NOT_VALID");
                i2 |= 42;
            }
            if ((i & 43) == 43) {
                arrayList.add("AKMP_NOT_VALID");
                i2 |= 43;
            }
            if ((i & 44) == 44) {
                arrayList.add("UNSUPPORTED_RSN_IE_VERSION");
                i2 |= 44;
            }
            if ((i & 45) == 45) {
                arrayList.add("INVALID_RSN_IE_CAPAB");
                i2 |= 45;
            }
            if ((i & 46) == 46) {
                arrayList.add("CIPHER_REJECTED_PER_POLICY");
                i2 |= 46;
            }
            if ((i & 47) == 47) {
                arrayList.add("TS_NOT_CREATED");
                i2 |= 47;
            }
            if ((i & 48) == 48) {
                arrayList.add("DIRECT_LINK_NOT_ALLOWED");
                i2 |= 48;
            }
            if ((i & 49) == 49) {
                arrayList.add("DEST_STA_NOT_PRESENT");
                i2 |= 49;
            }
            if ((i & 50) == 50) {
                arrayList.add("DEST_STA_NOT_QOS_STA");
                i2 |= 50;
            }
            if ((i & 51) == 51) {
                arrayList.add("ASSOC_DENIED_LISTEN_INT_TOO_LARGE");
                i2 |= 51;
            }
            if ((i & 52) == 52) {
                arrayList.add("INVALID_FT_ACTION_FRAME_COUNT");
                i2 |= 52;
            }
            if ((i & 53) == 53) {
                arrayList.add("INVALID_PMKID");
                i2 |= 53;
            }
            if ((i & 54) == 54) {
                arrayList.add("INVALID_MDIE");
                i2 |= 54;
            }
            if ((i & 55) == 55) {
                arrayList.add("INVALID_FTIE");
                i2 |= 55;
            }
            if ((i & 56) == 56) {
                arrayList.add("REQUESTED_TCLAS_NOT_SUPPORTED");
                i2 |= 56;
            }
            if ((i & 57) == 57) {
                arrayList.add("INSUFFICIENT_TCLAS_PROCESSING_RESOURCES");
                i2 |= 57;
            }
            if ((i & 58) == 58) {
                arrayList.add("TRY_ANOTHER_BSS");
                i2 |= 58;
            }
            if ((i & 59) == 59) {
                arrayList.add("GAS_ADV_PROTO_NOT_SUPPORTED");
                i2 |= 59;
            }
            if ((i & 60) == 60) {
                arrayList.add("NO_OUTSTANDING_GAS_REQ");
                i2 |= 60;
            }
            if ((i & 61) == 61) {
                arrayList.add("GAS_RESP_NOT_RECEIVED");
                i2 |= 61;
            }
            if ((i & 62) == 62) {
                arrayList.add("STA_TIMED_OUT_WAITING_FOR_GAS_RESP");
                i2 |= 62;
            }
            if ((i & 63) == 63) {
                arrayList.add("GAS_RESP_LARGER_THAN_LIMIT");
                i2 |= 63;
            }
            if ((i & 64) == 64) {
                arrayList.add("REQ_REFUSED_HOME");
                i2 |= 64;
            }
            if ((i & 65) == 65) {
                arrayList.add("ADV_SRV_UNREACHABLE");
                i2 |= 65;
            }
            if ((i & 67) == 67) {
                arrayList.add("REQ_REFUSED_SSPN");
                i2 |= 67;
            }
            if ((i & 68) == 68) {
                arrayList.add("REQ_REFUSED_UNAUTH_ACCESS");
                i2 |= 68;
            }
            if ((i & 72) == 72) {
                arrayList.add("INVALID_RSNIE");
                i2 |= 72;
            }
            if ((i & 73) == 73) {
                arrayList.add("U_APSD_COEX_NOT_SUPPORTED");
                i2 |= 73;
            }
            if ((i & 74) == 74) {
                arrayList.add("U_APSD_COEX_MODE_NOT_SUPPORTED");
                i2 |= 74;
            }
            if ((i & 75) == 75) {
                arrayList.add("BAD_INTERVAL_WITH_U_APSD_COEX");
                i2 |= 75;
            }
            if ((i & 76) == 76) {
                arrayList.add("ANTI_CLOGGING_TOKEN_REQ");
                i2 |= 76;
            }
            if ((i & 77) == 77) {
                arrayList.add("FINITE_CYCLIC_GROUP_NOT_SUPPORTED");
                i2 |= 77;
            }
            if ((i & 78) == 78) {
                arrayList.add("CANNOT_FIND_ALT_TBTT");
                i2 |= 78;
            }
            if ((i & 79) == 79) {
                arrayList.add("TRANSMISSION_FAILURE");
                i2 |= 79;
            }
            if ((i & 80) == 80) {
                arrayList.add("REQ_TCLAS_NOT_SUPPORTED");
                i2 |= 80;
            }
            if ((i & 81) == 81) {
                arrayList.add("TCLAS_RESOURCES_EXCHAUSTED");
                i2 |= 81;
            }
            if ((i & 82) == 82) {
                arrayList.add("REJECTED_WITH_SUGGESTED_BSS_TRANSITION");
                i2 |= 82;
            }
            if ((i & 83) == 83) {
                arrayList.add("REJECT_WITH_SCHEDULE");
                i2 |= 83;
            }
            if ((i & 84) == 84) {
                arrayList.add("REJECT_NO_WAKEUP_SPECIFIED");
                i2 |= 84;
            }
            if ((i & 85) == 85) {
                arrayList.add("SUCCESS_POWER_SAVE_MODE");
                i2 |= 85;
            }
            if ((i & 86) == 86) {
                arrayList.add("PENDING_ADMITTING_FST_SESSION");
                i2 |= 86;
            }
            if ((i & 87) == 87) {
                arrayList.add("PERFORMING_FST_NOW");
                i2 |= 87;
            }
            if ((i & 88) == 88) {
                arrayList.add("PENDING_GAP_IN_BA_WINDOW");
                i2 |= 88;
            }
            if ((i & 89) == 89) {
                arrayList.add("REJECT_U_PID_SETTING");
                i2 |= 89;
            }
            if ((i & 92) == 92) {
                arrayList.add("REFUSED_EXTERNAL_REASON");
                i2 |= 92;
            }
            if ((i & 93) == 93) {
                arrayList.add("REFUSED_AP_OUT_OF_MEMORY");
                i2 |= 93;
            }
            if ((i & 94) == 94) {
                arrayList.add("REJECTED_EMERGENCY_SERVICE_NOT_SUPPORTED");
                i2 |= 94;
            }
            if ((i & 95) == 95) {
                arrayList.add("QUERY_RESP_OUTSTANDING");
                i2 |= 95;
            }
            if ((i & 96) == 96) {
                arrayList.add("REJECT_DSE_BAND");
                i2 |= 96;
            }
            if ((i & 97) == 97) {
                arrayList.add("TCLAS_PROCESSING_TERMINATED");
                i2 |= 97;
            }
            if ((i & 98) == 98) {
                arrayList.add("TS_SCHEDULE_CONFLICT");
                i2 |= 98;
            }
            if ((i & 99) == 99) {
                arrayList.add("DENIED_WITH_SUGGESTED_BAND_AND_CHANNEL");
                i2 |= 99;
            }
            if ((i & 100) == 100) {
                arrayList.add("MCCAOP_RESERVATION_CONFLICT");
                i2 |= 100;
            }
            if ((i & MAF_LIMIT_EXCEEDED) == 101) {
                arrayList.add("MAF_LIMIT_EXCEEDED");
                i2 |= MAF_LIMIT_EXCEEDED;
            }
            if ((i & MCCA_TRACK_LIMIT_EXCEEDED) == 102) {
                arrayList.add("MCCA_TRACK_LIMIT_EXCEEDED");
                i2 |= MCCA_TRACK_LIMIT_EXCEEDED;
            }
            if ((i & DENIED_DUE_TO_SPECTRUM_MANAGEMENT) == 103) {
                arrayList.add("DENIED_DUE_TO_SPECTRUM_MANAGEMENT");
                i2 |= DENIED_DUE_TO_SPECTRUM_MANAGEMENT;
            }
            if ((i & ASSOC_DENIED_NO_VHT) == 104) {
                arrayList.add("ASSOC_DENIED_NO_VHT");
                i2 |= ASSOC_DENIED_NO_VHT;
            }
            if ((i & ENABLEMENT_DENIED) == 105) {
                arrayList.add("ENABLEMENT_DENIED");
                i2 |= ENABLEMENT_DENIED;
            }
            if ((i & RESTRICTION_FROM_AUTHORIZED_GDB) == 106) {
                arrayList.add("RESTRICTION_FROM_AUTHORIZED_GDB");
                i2 |= RESTRICTION_FROM_AUTHORIZED_GDB;
            }
            if ((i & AUTHORIZATION_DEENABLED) == 107) {
                arrayList.add("AUTHORIZATION_DEENABLED");
                i2 |= AUTHORIZATION_DEENABLED;
            }
            if ((i & FILS_AUTHENTICATION_FAILURE) == 112) {
                arrayList.add("FILS_AUTHENTICATION_FAILURE");
                i2 |= FILS_AUTHENTICATION_FAILURE;
            }
            if ((i & UNKNOWN_AUTHENTICATION_SERVER) == 113) {
                arrayList.add("UNKNOWN_AUTHENTICATION_SERVER");
                i2 |= UNKNOWN_AUTHENTICATION_SERVER;
            }
            if (i != i2) {
                arrayList.add("0x" + Integer.toHexString(i & (~i2)));
            }
            return String.join(" | ", arrayList);
        }
    }

    public static final class ReasonCode {
        public static final int AKMP_NOT_VALID = 20;
        public static final int AUTHORIZED_ACCESS_LIMIT_REACHED = 46;
        public static final int BAD_CIPHER_OR_AKM = 29;
        public static final int BSS_TRANSITION_DISASSOC = 12;
        public static final int CIPHER_SUITE_REJECTED = 24;
        public static final int CLASS2_FRAME_FROM_NONAUTH_STA = 6;
        public static final int CLASS3_FRAME_FROM_NONASSOC_STA = 7;
        public static final int DEAUTH_LEAVING = 3;
        public static final int DISASSOC_AP_BUSY = 5;
        public static final int DISASSOC_DUE_TO_INACTIVITY = 4;
        public static final int DISASSOC_LOW_ACK = 34;
        public static final int DISASSOC_STA_HAS_LEFT = 8;
        public static final int END_TS_BA_DLS = 37;
        public static final int EXCEEDED_TXOP = 35;
        public static final int EXTERNAL_SERVICE_REQUIREMENTS = 47;
        public static final int FOURWAY_HANDSHAKE_TIMEOUT = 15;
        public static final int GROUP_CIPHER_NOT_VALID = 18;
        public static final int GROUP_KEY_UPDATE_TIMEOUT = 16;
        public static final int IEEE_802_1X_AUTH_FAILED = 23;
        public static final int IE_IN_4WAY_DIFFERS = 17;
        public static final int INVALID_FTE = 51;
        public static final int INVALID_FT_ACTION_FRAME_COUNT = 48;
        public static final int INVALID_IE = 13;
        public static final int INVALID_MDE = 50;
        public static final int INVALID_PMKID = 49;
        public static final int INVALID_RSN_IE_CAPAB = 22;
        public static final int MAC_ADDRESS_ALREADY_EXISTS_IN_MBSS = 64;
        public static final int MESH_CHANNEL_SWITCH_REGULATORY_REQ = 65;
        public static final int MESH_CHANNEL_SWITCH_UNSPECIFIED = 66;
        public static final int MESH_CLOSE_RCVD = 55;
        public static final int MESH_CONFIG_POLICY_VIOLATION = 54;
        public static final int MESH_CONFIRM_TIMEOUT = 57;
        public static final int MESH_INCONSISTENT_PARAMS = 59;
        public static final int MESH_INVALID_GTK = 58;
        public static final int MESH_INVALID_SECURITY_CAP = 60;
        public static final int MESH_MAX_PEERS = 53;
        public static final int MESH_MAX_RETRIES = 56;
        public static final int MESH_PATH_ERROR_DEST_UNREACHABLE = 63;
        public static final int MESH_PATH_ERROR_NO_FORWARDING_INFO = 62;
        public static final int MESH_PATH_ERROR_NO_PROXY_INFO = 61;
        public static final int MESH_PEERING_CANCELLED = 52;
        public static final int MICHAEL_MIC_FAILURE = 14;
        public static final int NOT_AUTHORIZED_THIS_LOCATION = 30;
        public static final int NOT_ENOUGH_BANDWIDTH = 33;
        public static final int NO_SSP_ROAMING_AGREEMENT = 28;
        public static final int PAIRWISE_CIPHER_NOT_VALID = 19;
        public static final int PEERKEY_MISMATCH = 45;
        public static final int PREV_AUTH_NOT_VALID = 2;
        public static final int PWR_CAPABILITY_NOT_VALID = 10;
        public static final int SERVICE_CHANGE_PRECLUDES_TS = 31;
        public static final int SSP_REQUESTED_DISASSOC = 27;
        public static final int STA_LEAVING = 36;
        public static final int STA_REQ_ASSOC_WITHOUT_AUTH = 9;
        public static final int SUPPORTED_CHANNEL_NOT_VALID = 11;
        public static final int TDLS_TEARDOWN_UNREACHABLE = 25;
        public static final int TDLS_TEARDOWN_UNSPECIFIED = 26;
        public static final int TIMEOUT = 39;
        public static final int UNKNOWN_TS_BA = 38;
        public static final int UNSPECIFIED = 1;
        public static final int UNSPECIFIED_QOS_REASON = 32;
        public static final int UNSUPPORTED_RSN_IE_VERSION = 21;

        public static final String toString(int i) {
            if (i == 1) {
                return "UNSPECIFIED";
            }
            if (i == 2) {
                return "PREV_AUTH_NOT_VALID";
            }
            if (i == 3) {
                return "DEAUTH_LEAVING";
            }
            if (i == 4) {
                return "DISASSOC_DUE_TO_INACTIVITY";
            }
            if (i == 5) {
                return "DISASSOC_AP_BUSY";
            }
            if (i == 6) {
                return "CLASS2_FRAME_FROM_NONAUTH_STA";
            }
            if (i == 7) {
                return "CLASS3_FRAME_FROM_NONASSOC_STA";
            }
            if (i == 8) {
                return "DISASSOC_STA_HAS_LEFT";
            }
            if (i == 9) {
                return "STA_REQ_ASSOC_WITHOUT_AUTH";
            }
            if (i == 10) {
                return "PWR_CAPABILITY_NOT_VALID";
            }
            if (i == 11) {
                return "SUPPORTED_CHANNEL_NOT_VALID";
            }
            if (i == 12) {
                return "BSS_TRANSITION_DISASSOC";
            }
            if (i == 13) {
                return "INVALID_IE";
            }
            if (i == 14) {
                return "MICHAEL_MIC_FAILURE";
            }
            if (i == 15) {
                return "FOURWAY_HANDSHAKE_TIMEOUT";
            }
            if (i == 16) {
                return "GROUP_KEY_UPDATE_TIMEOUT";
            }
            if (i == 17) {
                return "IE_IN_4WAY_DIFFERS";
            }
            if (i == 18) {
                return "GROUP_CIPHER_NOT_VALID";
            }
            if (i == 19) {
                return "PAIRWISE_CIPHER_NOT_VALID";
            }
            if (i == 20) {
                return "AKMP_NOT_VALID";
            }
            if (i == 21) {
                return "UNSUPPORTED_RSN_IE_VERSION";
            }
            if (i == 22) {
                return "INVALID_RSN_IE_CAPAB";
            }
            if (i == 23) {
                return "IEEE_802_1X_AUTH_FAILED";
            }
            if (i == 24) {
                return "CIPHER_SUITE_REJECTED";
            }
            if (i == 25) {
                return "TDLS_TEARDOWN_UNREACHABLE";
            }
            if (i == 26) {
                return "TDLS_TEARDOWN_UNSPECIFIED";
            }
            if (i == 27) {
                return "SSP_REQUESTED_DISASSOC";
            }
            if (i == 28) {
                return "NO_SSP_ROAMING_AGREEMENT";
            }
            if (i == 29) {
                return "BAD_CIPHER_OR_AKM";
            }
            if (i == 30) {
                return "NOT_AUTHORIZED_THIS_LOCATION";
            }
            if (i == 31) {
                return "SERVICE_CHANGE_PRECLUDES_TS";
            }
            if (i == 32) {
                return "UNSPECIFIED_QOS_REASON";
            }
            if (i == 33) {
                return "NOT_ENOUGH_BANDWIDTH";
            }
            if (i == 34) {
                return "DISASSOC_LOW_ACK";
            }
            if (i == 35) {
                return "EXCEEDED_TXOP";
            }
            if (i == 36) {
                return "STA_LEAVING";
            }
            if (i == 37) {
                return "END_TS_BA_DLS";
            }
            if (i == 38) {
                return "UNKNOWN_TS_BA";
            }
            if (i == 39) {
                return "TIMEOUT";
            }
            if (i == 45) {
                return "PEERKEY_MISMATCH";
            }
            if (i == 46) {
                return "AUTHORIZED_ACCESS_LIMIT_REACHED";
            }
            if (i == 47) {
                return "EXTERNAL_SERVICE_REQUIREMENTS";
            }
            if (i == 48) {
                return "INVALID_FT_ACTION_FRAME_COUNT";
            }
            if (i == 49) {
                return "INVALID_PMKID";
            }
            if (i == 50) {
                return "INVALID_MDE";
            }
            if (i == 51) {
                return "INVALID_FTE";
            }
            if (i == 52) {
                return "MESH_PEERING_CANCELLED";
            }
            if (i == 53) {
                return "MESH_MAX_PEERS";
            }
            if (i == 54) {
                return "MESH_CONFIG_POLICY_VIOLATION";
            }
            if (i == 55) {
                return "MESH_CLOSE_RCVD";
            }
            if (i == 56) {
                return "MESH_MAX_RETRIES";
            }
            if (i == 57) {
                return "MESH_CONFIRM_TIMEOUT";
            }
            if (i == 58) {
                return "MESH_INVALID_GTK";
            }
            if (i == 59) {
                return "MESH_INCONSISTENT_PARAMS";
            }
            if (i == 60) {
                return "MESH_INVALID_SECURITY_CAP";
            }
            if (i == 61) {
                return "MESH_PATH_ERROR_NO_PROXY_INFO";
            }
            if (i == 62) {
                return "MESH_PATH_ERROR_NO_FORWARDING_INFO";
            }
            if (i == 63) {
                return "MESH_PATH_ERROR_DEST_UNREACHABLE";
            }
            if (i == 64) {
                return "MAC_ADDRESS_ALREADY_EXISTS_IN_MBSS";
            }
            if (i == 65) {
                return "MESH_CHANNEL_SWITCH_REGULATORY_REQ";
            }
            if (i == 66) {
                return "MESH_CHANNEL_SWITCH_UNSPECIFIED";
            }
            return "0x" + Integer.toHexString(i);
        }

        public static final String dumpBitfield(int i) {
            ArrayList arrayList = new ArrayList();
            int i2 = 1;
            if ((i & 1) == 1) {
                arrayList.add("UNSPECIFIED");
            } else {
                i2 = 0;
            }
            if ((i & 2) == 2) {
                arrayList.add("PREV_AUTH_NOT_VALID");
                i2 |= 2;
            }
            if ((i & 3) == 3) {
                arrayList.add("DEAUTH_LEAVING");
                i2 |= 3;
            }
            if ((i & 4) == 4) {
                arrayList.add("DISASSOC_DUE_TO_INACTIVITY");
                i2 |= 4;
            }
            if ((i & 5) == 5) {
                arrayList.add("DISASSOC_AP_BUSY");
                i2 |= 5;
            }
            if ((i & 6) == 6) {
                arrayList.add("CLASS2_FRAME_FROM_NONAUTH_STA");
                i2 |= 6;
            }
            if ((i & 7) == 7) {
                arrayList.add("CLASS3_FRAME_FROM_NONASSOC_STA");
                i2 |= 7;
            }
            if ((i & 8) == 8) {
                arrayList.add("DISASSOC_STA_HAS_LEFT");
                i2 |= 8;
            }
            if ((i & 9) == 9) {
                arrayList.add("STA_REQ_ASSOC_WITHOUT_AUTH");
                i2 |= 9;
            }
            if ((i & 10) == 10) {
                arrayList.add("PWR_CAPABILITY_NOT_VALID");
                i2 |= 10;
            }
            if ((i & 11) == 11) {
                arrayList.add("SUPPORTED_CHANNEL_NOT_VALID");
                i2 |= 11;
            }
            if ((i & 12) == 12) {
                arrayList.add("BSS_TRANSITION_DISASSOC");
                i2 |= 12;
            }
            if ((i & 13) == 13) {
                arrayList.add("INVALID_IE");
                i2 |= 13;
            }
            if ((i & 14) == 14) {
                arrayList.add("MICHAEL_MIC_FAILURE");
                i2 |= 14;
            }
            if ((i & 15) == 15) {
                arrayList.add("FOURWAY_HANDSHAKE_TIMEOUT");
                i2 |= 15;
            }
            if ((i & 16) == 16) {
                arrayList.add("GROUP_KEY_UPDATE_TIMEOUT");
                i2 |= 16;
            }
            if ((i & 17) == 17) {
                arrayList.add("IE_IN_4WAY_DIFFERS");
                i2 |= 17;
            }
            if ((i & 18) == 18) {
                arrayList.add("GROUP_CIPHER_NOT_VALID");
                i2 |= 18;
            }
            if ((i & 19) == 19) {
                arrayList.add("PAIRWISE_CIPHER_NOT_VALID");
                i2 |= 19;
            }
            if ((i & 20) == 20) {
                arrayList.add("AKMP_NOT_VALID");
                i2 |= 20;
            }
            if ((i & 21) == 21) {
                arrayList.add("UNSUPPORTED_RSN_IE_VERSION");
                i2 |= 21;
            }
            if ((i & 22) == 22) {
                arrayList.add("INVALID_RSN_IE_CAPAB");
                i2 |= 22;
            }
            if ((i & 23) == 23) {
                arrayList.add("IEEE_802_1X_AUTH_FAILED");
                i2 |= 23;
            }
            if ((i & 24) == 24) {
                arrayList.add("CIPHER_SUITE_REJECTED");
                i2 |= 24;
            }
            if ((i & 25) == 25) {
                arrayList.add("TDLS_TEARDOWN_UNREACHABLE");
                i2 |= 25;
            }
            if ((i & 26) == 26) {
                arrayList.add("TDLS_TEARDOWN_UNSPECIFIED");
                i2 |= 26;
            }
            if ((i & 27) == 27) {
                arrayList.add("SSP_REQUESTED_DISASSOC");
                i2 |= 27;
            }
            if ((i & 28) == 28) {
                arrayList.add("NO_SSP_ROAMING_AGREEMENT");
                i2 |= 28;
            }
            if ((i & 29) == 29) {
                arrayList.add("BAD_CIPHER_OR_AKM");
                i2 |= 29;
            }
            if ((i & 30) == 30) {
                arrayList.add("NOT_AUTHORIZED_THIS_LOCATION");
                i2 |= 30;
            }
            if ((i & 31) == 31) {
                arrayList.add("SERVICE_CHANGE_PRECLUDES_TS");
                i2 |= 31;
            }
            if ((i & 32) == 32) {
                arrayList.add("UNSPECIFIED_QOS_REASON");
                i2 |= 32;
            }
            if ((i & 33) == 33) {
                arrayList.add("NOT_ENOUGH_BANDWIDTH");
                i2 |= 33;
            }
            if ((i & 34) == 34) {
                arrayList.add("DISASSOC_LOW_ACK");
                i2 |= 34;
            }
            if ((i & 35) == 35) {
                arrayList.add("EXCEEDED_TXOP");
                i2 |= 35;
            }
            if ((i & 36) == 36) {
                arrayList.add("STA_LEAVING");
                i2 |= 36;
            }
            if ((i & 37) == 37) {
                arrayList.add("END_TS_BA_DLS");
                i2 |= 37;
            }
            if ((i & 38) == 38) {
                arrayList.add("UNKNOWN_TS_BA");
                i2 |= 38;
            }
            if ((i & 39) == 39) {
                arrayList.add("TIMEOUT");
                i2 |= 39;
            }
            if ((i & 45) == 45) {
                arrayList.add("PEERKEY_MISMATCH");
                i2 |= 45;
            }
            if ((i & 46) == 46) {
                arrayList.add("AUTHORIZED_ACCESS_LIMIT_REACHED");
                i2 |= 46;
            }
            if ((i & 47) == 47) {
                arrayList.add("EXTERNAL_SERVICE_REQUIREMENTS");
                i2 |= 47;
            }
            if ((i & 48) == 48) {
                arrayList.add("INVALID_FT_ACTION_FRAME_COUNT");
                i2 |= 48;
            }
            if ((i & 49) == 49) {
                arrayList.add("INVALID_PMKID");
                i2 |= 49;
            }
            if ((i & 50) == 50) {
                arrayList.add("INVALID_MDE");
                i2 |= 50;
            }
            if ((i & 51) == 51) {
                arrayList.add("INVALID_FTE");
                i2 |= 51;
            }
            if ((i & 52) == 52) {
                arrayList.add("MESH_PEERING_CANCELLED");
                i2 |= 52;
            }
            if ((i & 53) == 53) {
                arrayList.add("MESH_MAX_PEERS");
                i2 |= 53;
            }
            if ((i & 54) == 54) {
                arrayList.add("MESH_CONFIG_POLICY_VIOLATION");
                i2 |= 54;
            }
            if ((i & 55) == 55) {
                arrayList.add("MESH_CLOSE_RCVD");
                i2 |= 55;
            }
            if ((i & 56) == 56) {
                arrayList.add("MESH_MAX_RETRIES");
                i2 |= 56;
            }
            if ((i & 57) == 57) {
                arrayList.add("MESH_CONFIRM_TIMEOUT");
                i2 |= 57;
            }
            if ((i & 58) == 58) {
                arrayList.add("MESH_INVALID_GTK");
                i2 |= 58;
            }
            if ((i & 59) == 59) {
                arrayList.add("MESH_INCONSISTENT_PARAMS");
                i2 |= 59;
            }
            if ((i & 60) == 60) {
                arrayList.add("MESH_INVALID_SECURITY_CAP");
                i2 |= 60;
            }
            if ((i & 61) == 61) {
                arrayList.add("MESH_PATH_ERROR_NO_PROXY_INFO");
                i2 |= 61;
            }
            if ((i & 62) == 62) {
                arrayList.add("MESH_PATH_ERROR_NO_FORWARDING_INFO");
                i2 |= 62;
            }
            if ((i & 63) == 63) {
                arrayList.add("MESH_PATH_ERROR_DEST_UNREACHABLE");
                i2 |= 63;
            }
            if ((i & 64) == 64) {
                arrayList.add("MAC_ADDRESS_ALREADY_EXISTS_IN_MBSS");
                i2 |= 64;
            }
            if ((i & 65) == 65) {
                arrayList.add("MESH_CHANNEL_SWITCH_REGULATORY_REQ");
                i2 |= 65;
            }
            if ((i & 66) == 66) {
                arrayList.add("MESH_CHANNEL_SWITCH_UNSPECIFIED");
                i2 |= 66;
            }
            if (i != i2) {
                arrayList.add("0x" + Integer.toHexString(i & (~i2)));
            }
            return String.join(" | ", arrayList);
        }
    }

    public static final class BssidChangeReason {
        public static final byte ASSOC_COMPLETE = 1;
        public static final byte ASSOC_START = 0;
        public static final byte DISASSOC = 2;

        public static final String toString(byte b) {
            if (b == 0) {
                return "ASSOC_START";
            }
            if (b == 1) {
                return "ASSOC_COMPLETE";
            }
            if (b == 2) {
                return "DISASSOC";
            }
            return "0x" + Integer.toHexString(Byte.toUnsignedInt(b));
        }

        public static final String dumpBitfield(byte b) {
            byte b2;
            ArrayList arrayList = new ArrayList();
            arrayList.add("ASSOC_START");
            if ((b & 1) == 1) {
                arrayList.add("ASSOC_COMPLETE");
                b2 = (byte) 1;
            } else {
                b2 = 0;
            }
            if ((b & 2) == 2) {
                arrayList.add("DISASSOC");
                b2 = (byte) (b2 | 2);
            }
            if (b != b2) {
                arrayList.add("0x" + Integer.toHexString(Byte.toUnsignedInt((byte) (b & (~b2)))));
            }
            return String.join(" | ", arrayList);
        }
    }

    public static final class Proxy implements ISupplicantStaIfaceCallback {
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
                return "[class or subclass of android.hardware.wifi.supplicant@1.0::ISupplicantStaIfaceCallback]@Proxy";
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
            hwParcel.writeInterfaceToken(ISupplicantStaIfaceCallback.kInterfaceName);
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
            hwParcel.writeInterfaceToken(ISupplicantStaIfaceCallback.kInterfaceName);
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
        public void onStateChanged(int i, byte[] bArr, int i2, ArrayList<Byte> arrayList) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaIfaceCallback.kInterfaceName);
            hwParcel.writeInt32(i);
            HwBlob hwBlob = new HwBlob(6);
            hwBlob.putInt8Array(0L, bArr);
            hwParcel.writeBuffer(hwBlob);
            hwParcel.writeInt32(i2);
            hwParcel.writeInt8Vector(arrayList);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(3, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void onAnqpQueryDone(byte[] bArr, AnqpData anqpData, Hs20AnqpData hs20AnqpData) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaIfaceCallback.kInterfaceName);
            HwBlob hwBlob = new HwBlob(6);
            hwBlob.putInt8Array(0L, bArr);
            hwParcel.writeBuffer(hwBlob);
            anqpData.writeToParcel(hwParcel);
            hs20AnqpData.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(4, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void onHs20IconQueryDone(byte[] bArr, String str, ArrayList<Byte> arrayList) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaIfaceCallback.kInterfaceName);
            HwBlob hwBlob = new HwBlob(6);
            hwBlob.putInt8Array(0L, bArr);
            hwParcel.writeBuffer(hwBlob);
            hwParcel.writeString(str);
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
        public void onHs20SubscriptionRemediation(byte[] bArr, byte b, String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaIfaceCallback.kInterfaceName);
            HwBlob hwBlob = new HwBlob(6);
            hwBlob.putInt8Array(0L, bArr);
            hwParcel.writeBuffer(hwBlob);
            hwParcel.writeInt8(b);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(6, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void onHs20DeauthImminentNotice(byte[] bArr, int i, int i2, String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaIfaceCallback.kInterfaceName);
            HwBlob hwBlob = new HwBlob(6);
            hwBlob.putInt8Array(0L, bArr);
            hwParcel.writeBuffer(hwBlob);
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
        public void onDisconnected(byte[] bArr, boolean z, int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaIfaceCallback.kInterfaceName);
            HwBlob hwBlob = new HwBlob(6);
            hwBlob.putInt8Array(0L, bArr);
            hwParcel.writeBuffer(hwBlob);
            hwParcel.writeBool(z);
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
        public void onAssociationRejected(byte[] bArr, int i, boolean z) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaIfaceCallback.kInterfaceName);
            HwBlob hwBlob = new HwBlob(6);
            hwBlob.putInt8Array(0L, bArr);
            hwParcel.writeBuffer(hwBlob);
            hwParcel.writeInt32(i);
            hwParcel.writeBool(z);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(9, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void onAuthenticationTimeout(byte[] bArr) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaIfaceCallback.kInterfaceName);
            HwBlob hwBlob = new HwBlob(6);
            hwBlob.putInt8Array(0L, bArr);
            hwParcel.writeBuffer(hwBlob);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(10, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void onEapFailure() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaIfaceCallback.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(11, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void onBssidChanged(byte b, byte[] bArr) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaIfaceCallback.kInterfaceName);
            hwParcel.writeInt8(b);
            HwBlob hwBlob = new HwBlob(6);
            hwBlob.putInt8Array(0L, bArr);
            hwParcel.writeBuffer(hwBlob);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(12, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void onWpsEventSuccess() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaIfaceCallback.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(13, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void onWpsEventFail(byte[] bArr, short s, short s2) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaIfaceCallback.kInterfaceName);
            HwBlob hwBlob = new HwBlob(6);
            hwBlob.putInt8Array(0L, bArr);
            hwParcel.writeBuffer(hwBlob);
            hwParcel.writeInt16(s);
            hwParcel.writeInt16(s2);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(14, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void onWpsEventPbcOverlap() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaIfaceCallback.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(15, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void onExtRadioWorkStart(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaIfaceCallback.kInterfaceName);
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
        public void onExtRadioWorkTimeout(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaIfaceCallback.kInterfaceName);
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

    public static abstract class Stub extends HwBinder implements ISupplicantStaIfaceCallback {
        @Override
        public IHwBinder asBinder() {
            return this;
        }

        @Override
        public final ArrayList<String> interfaceChain() {
            return new ArrayList<>(Arrays.asList(ISupplicantStaIfaceCallback.kInterfaceName, IBase.kInterfaceName));
        }

        @Override
        public final String interfaceDescriptor() {
            return ISupplicantStaIfaceCallback.kInterfaceName;
        }

        @Override
        public final ArrayList<byte[]> getHashChain() {
            return new ArrayList<>(Arrays.asList(new byte[]{-41, -127, -56, -41, -25, -77, -2, 92, -54, -116, -10, -31, -40, -128, 110, 119, 9, -126, -82, 83, 88, -57, -127, 110, -43, 27, 15, 14, -62, 114, -25, 13}, new byte[]{-67, -38, -74, 24, 77, 122, 52, 109, -90, -96, 125, -64, -126, -116, -15, -102, 105, 111, 76, -86, 54, 17, -59, 31, 46, 20, 86, 90, 20, -76, 15, -39}));
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
            if (ISupplicantStaIfaceCallback.kInterfaceName.equals(str)) {
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
                        hwParcel.enforceInterface(ISupplicantStaIfaceCallback.kInterfaceName);
                        onNetworkAdded(hwParcel.readInt32());
                    }
                    break;
                case 2:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaIfaceCallback.kInterfaceName);
                        onNetworkRemoved(hwParcel.readInt32());
                    }
                    break;
                case 3:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaIfaceCallback.kInterfaceName);
                        int int32 = hwParcel.readInt32();
                        byte[] bArr = new byte[6];
                        hwParcel.readBuffer(6L).copyToInt8Array(0L, bArr, 6);
                        onStateChanged(int32, bArr, hwParcel.readInt32(), hwParcel.readInt8Vector());
                    }
                    break;
                case 4:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaIfaceCallback.kInterfaceName);
                        byte[] bArr2 = new byte[6];
                        hwParcel.readBuffer(6L).copyToInt8Array(0L, bArr2, 6);
                        AnqpData anqpData = new AnqpData();
                        anqpData.readFromParcel(hwParcel);
                        Hs20AnqpData hs20AnqpData = new Hs20AnqpData();
                        hs20AnqpData.readFromParcel(hwParcel);
                        onAnqpQueryDone(bArr2, anqpData, hs20AnqpData);
                    }
                    break;
                case 5:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaIfaceCallback.kInterfaceName);
                        byte[] bArr3 = new byte[6];
                        hwParcel.readBuffer(6L).copyToInt8Array(0L, bArr3, 6);
                        onHs20IconQueryDone(bArr3, hwParcel.readString(), hwParcel.readInt8Vector());
                    }
                    break;
                case 6:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaIfaceCallback.kInterfaceName);
                        byte[] bArr4 = new byte[6];
                        hwParcel.readBuffer(6L).copyToInt8Array(0L, bArr4, 6);
                        onHs20SubscriptionRemediation(bArr4, hwParcel.readInt8(), hwParcel.readString());
                    }
                    break;
                case 7:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaIfaceCallback.kInterfaceName);
                        byte[] bArr5 = new byte[6];
                        hwParcel.readBuffer(6L).copyToInt8Array(0L, bArr5, 6);
                        onHs20DeauthImminentNotice(bArr5, hwParcel.readInt32(), hwParcel.readInt32(), hwParcel.readString());
                    }
                    break;
                case 8:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaIfaceCallback.kInterfaceName);
                        byte[] bArr6 = new byte[6];
                        hwParcel.readBuffer(6L).copyToInt8Array(0L, bArr6, 6);
                        onDisconnected(bArr6, hwParcel.readBool(), hwParcel.readInt32());
                    }
                    break;
                case 9:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaIfaceCallback.kInterfaceName);
                        byte[] bArr7 = new byte[6];
                        hwParcel.readBuffer(6L).copyToInt8Array(0L, bArr7, 6);
                        onAssociationRejected(bArr7, hwParcel.readInt32(), hwParcel.readBool());
                    }
                    break;
                case 10:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaIfaceCallback.kInterfaceName);
                        byte[] bArr8 = new byte[6];
                        hwParcel.readBuffer(6L).copyToInt8Array(0L, bArr8, 6);
                        onAuthenticationTimeout(bArr8);
                    }
                    break;
                case 11:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaIfaceCallback.kInterfaceName);
                        onEapFailure();
                    }
                    break;
                case 12:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaIfaceCallback.kInterfaceName);
                        byte int8 = hwParcel.readInt8();
                        byte[] bArr9 = new byte[6];
                        hwParcel.readBuffer(6L).copyToInt8Array(0L, bArr9, 6);
                        onBssidChanged(int8, bArr9);
                    }
                    break;
                case 13:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaIfaceCallback.kInterfaceName);
                        onWpsEventSuccess();
                    }
                    break;
                case 14:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaIfaceCallback.kInterfaceName);
                        byte[] bArr10 = new byte[6];
                        hwParcel.readBuffer(6L).copyToInt8Array(0L, bArr10, 6);
                        onWpsEventFail(bArr10, hwParcel.readInt16(), hwParcel.readInt16());
                    }
                    break;
                case 15:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaIfaceCallback.kInterfaceName);
                        onWpsEventPbcOverlap();
                    }
                    break;
                case 16:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaIfaceCallback.kInterfaceName);
                        onExtRadioWorkStart(hwParcel.readInt32());
                    }
                    break;
                case 17:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaIfaceCallback.kInterfaceName);
                        onExtRadioWorkTimeout(hwParcel.readInt32());
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
