package android.hardware.wifi.supplicant.V1_0;

import android.hardware.wifi.supplicant.V1_0.ISupplicantIface;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaIfaceCallback;
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
import com.android.server.wifi.hotspot2.anqp.Constants;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;

public interface ISupplicantStaIface extends ISupplicantIface {
    public static final String kInterfaceName = "android.hardware.wifi.supplicant@1.0::ISupplicantStaIface";

    @FunctionalInterface
    public interface addExtRadioWorkCallback {
        void onValues(SupplicantStatus supplicantStatus, int i);
    }

    @FunctionalInterface
    public interface getMacAddressCallback {
        void onValues(SupplicantStatus supplicantStatus, byte[] bArr);
    }

    @FunctionalInterface
    public interface startWpsPinDisplayCallback {
        void onValues(SupplicantStatus supplicantStatus, String str);
    }

    void addExtRadioWork(String str, int i, int i2, addExtRadioWorkCallback addextradioworkcallback) throws RemoteException;

    SupplicantStatus addRxFilter(byte b) throws RemoteException;

    @Override
    IHwBinder asBinder();

    SupplicantStatus cancelWps() throws RemoteException;

    SupplicantStatus disconnect() throws RemoteException;

    SupplicantStatus enableAutoReconnect(boolean z) throws RemoteException;

    @Override
    DebugInfo getDebugInfo() throws RemoteException;

    @Override
    ArrayList<byte[]> getHashChain() throws RemoteException;

    void getMacAddress(getMacAddressCallback getmacaddresscallback) throws RemoteException;

    SupplicantStatus initiateAnqpQuery(byte[] bArr, ArrayList<Short> arrayList, ArrayList<Integer> arrayList2) throws RemoteException;

    SupplicantStatus initiateHs20IconQuery(byte[] bArr, String str) throws RemoteException;

    SupplicantStatus initiateTdlsDiscover(byte[] bArr) throws RemoteException;

    SupplicantStatus initiateTdlsSetup(byte[] bArr) throws RemoteException;

    SupplicantStatus initiateTdlsTeardown(byte[] bArr) throws RemoteException;

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

    SupplicantStatus reassociate() throws RemoteException;

    SupplicantStatus reconnect() throws RemoteException;

    SupplicantStatus registerCallback(ISupplicantStaIfaceCallback iSupplicantStaIfaceCallback) throws RemoteException;

    SupplicantStatus removeExtRadioWork(int i) throws RemoteException;

    SupplicantStatus removeRxFilter(byte b) throws RemoteException;

    SupplicantStatus setBtCoexistenceMode(byte b) throws RemoteException;

    SupplicantStatus setBtCoexistenceScanModeEnabled(boolean z) throws RemoteException;

    SupplicantStatus setCountryCode(byte[] bArr) throws RemoteException;

    SupplicantStatus setExternalSim(boolean z) throws RemoteException;

    @Override
    void setHALInstrumentation() throws RemoteException;

    SupplicantStatus setPowerSave(boolean z) throws RemoteException;

    SupplicantStatus setSuspendModeEnabled(boolean z) throws RemoteException;

    SupplicantStatus startRxFilter() throws RemoteException;

    SupplicantStatus startWpsPbc(byte[] bArr) throws RemoteException;

    void startWpsPinDisplay(byte[] bArr, startWpsPinDisplayCallback startwpspindisplaycallback) throws RemoteException;

    SupplicantStatus startWpsPinKeypad(String str) throws RemoteException;

    SupplicantStatus startWpsRegistrar(byte[] bArr, String str) throws RemoteException;

    SupplicantStatus stopRxFilter() throws RemoteException;

    @Override
    boolean unlinkToDeath(IHwBinder.DeathRecipient deathRecipient) throws RemoteException;

    static ISupplicantStaIface asInterface(IHwBinder iHwBinder) {
        if (iHwBinder == null) {
            return null;
        }
        IHwInterface iHwInterfaceQueryLocalInterface = iHwBinder.queryLocalInterface(kInterfaceName);
        if (iHwInterfaceQueryLocalInterface != null && (iHwInterfaceQueryLocalInterface instanceof ISupplicantStaIface)) {
            return (ISupplicantStaIface) iHwInterfaceQueryLocalInterface;
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

    static ISupplicantStaIface castFrom(IHwInterface iHwInterface) {
        if (iHwInterface == null) {
            return null;
        }
        return asInterface(iHwInterface.asBinder());
    }

    static ISupplicantStaIface getService(String str, boolean z) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, str, z));
    }

    static ISupplicantStaIface getService(boolean z) throws RemoteException {
        return getService(HalDeviceManager.HAL_INSTANCE_NAME, z);
    }

    static ISupplicantStaIface getService(String str) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, str));
    }

    static ISupplicantStaIface getService() throws RemoteException {
        return getService(HalDeviceManager.HAL_INSTANCE_NAME);
    }

    public static final class AnqpInfoId {
        public static final short ANQP_3GPP_CELLULAR_NETWORK = 264;
        public static final short DOMAIN_NAME = 268;
        public static final short IP_ADDR_TYPE_AVAILABILITY = 262;
        public static final short NAI_REALM = 263;
        public static final short ROAMING_CONSORTIUM = 261;
        public static final short VENUE_NAME = 258;

        public static final String toString(short s) {
            if (s == 258) {
                return "VENUE_NAME";
            }
            if (s == 261) {
                return "ROAMING_CONSORTIUM";
            }
            if (s == 262) {
                return "IP_ADDR_TYPE_AVAILABILITY";
            }
            if (s == 263) {
                return "NAI_REALM";
            }
            if (s == 264) {
                return "ANQP_3GPP_CELLULAR_NETWORK";
            }
            if (s == 268) {
                return "DOMAIN_NAME";
            }
            return "0x" + Integer.toHexString(Short.toUnsignedInt(s));
        }

        public static final String dumpBitfield(short s) {
            short s2;
            ArrayList arrayList = new ArrayList();
            if ((s & VENUE_NAME) == 258) {
                arrayList.add("VENUE_NAME");
                s2 = (short) Constants.ANQP_VENUE_NAME;
            } else {
                s2 = 0;
            }
            if ((s & ROAMING_CONSORTIUM) == 261) {
                arrayList.add("ROAMING_CONSORTIUM");
                s2 = (short) (s2 | ROAMING_CONSORTIUM);
            }
            if ((s & IP_ADDR_TYPE_AVAILABILITY) == 262) {
                arrayList.add("IP_ADDR_TYPE_AVAILABILITY");
                s2 = (short) (s2 | IP_ADDR_TYPE_AVAILABILITY);
            }
            if ((s & NAI_REALM) == 263) {
                arrayList.add("NAI_REALM");
                s2 = (short) (s2 | NAI_REALM);
            }
            if ((s & ANQP_3GPP_CELLULAR_NETWORK) == 264) {
                arrayList.add("ANQP_3GPP_CELLULAR_NETWORK");
                s2 = (short) (s2 | ANQP_3GPP_CELLULAR_NETWORK);
            }
            if ((s & DOMAIN_NAME) == 268) {
                arrayList.add("DOMAIN_NAME");
                s2 = (short) (s2 | DOMAIN_NAME);
            }
            if (s != s2) {
                arrayList.add("0x" + Integer.toHexString(Short.toUnsignedInt((short) (s & (~s2)))));
            }
            return String.join(" | ", arrayList);
        }
    }

    public static final class Hs20AnqpSubtypes {
        public static final int CONNECTION_CAPABILITY = 5;
        public static final int OPERATOR_FRIENDLY_NAME = 3;
        public static final int OSU_PROVIDERS_LIST = 8;
        public static final int WAN_METRICS = 4;

        public static final String toString(int i) {
            if (i == 3) {
                return "OPERATOR_FRIENDLY_NAME";
            }
            if (i == 4) {
                return "WAN_METRICS";
            }
            if (i == 5) {
                return "CONNECTION_CAPABILITY";
            }
            if (i == 8) {
                return "OSU_PROVIDERS_LIST";
            }
            return "0x" + Integer.toHexString(i);
        }

        public static final String dumpBitfield(int i) {
            ArrayList arrayList = new ArrayList();
            int i2 = 3;
            if ((i & 3) == 3) {
                arrayList.add("OPERATOR_FRIENDLY_NAME");
            } else {
                i2 = 0;
            }
            if ((i & 4) == 4) {
                arrayList.add("WAN_METRICS");
                i2 |= 4;
            }
            if ((i & 5) == 5) {
                arrayList.add("CONNECTION_CAPABILITY");
                i2 |= 5;
            }
            if ((i & 8) == 8) {
                arrayList.add("OSU_PROVIDERS_LIST");
                i2 |= 8;
            }
            if (i != i2) {
                arrayList.add("0x" + Integer.toHexString(i & (~i2)));
            }
            return String.join(" | ", arrayList);
        }
    }

    public static final class RxFilterType {
        public static final byte V4_MULTICAST = 0;
        public static final byte V6_MULTICAST = 1;

        public static final String toString(byte b) {
            if (b == 0) {
                return "V4_MULTICAST";
            }
            if (b == 1) {
                return "V6_MULTICAST";
            }
            return "0x" + Integer.toHexString(Byte.toUnsignedInt(b));
        }

        public static final String dumpBitfield(byte b) {
            byte b2;
            ArrayList arrayList = new ArrayList();
            arrayList.add("V4_MULTICAST");
            if ((b & 1) == 1) {
                arrayList.add("V6_MULTICAST");
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

    public static final class BtCoexistenceMode {
        public static final byte DISABLED = 1;
        public static final byte ENABLED = 0;
        public static final byte SENSE = 2;

        public static final String toString(byte b) {
            if (b == 0) {
                return "ENABLED";
            }
            if (b == 1) {
                return "DISABLED";
            }
            if (b == 2) {
                return "SENSE";
            }
            return "0x" + Integer.toHexString(Byte.toUnsignedInt(b));
        }

        public static final String dumpBitfield(byte b) {
            byte b2;
            ArrayList arrayList = new ArrayList();
            arrayList.add("ENABLED");
            if ((b & 1) == 1) {
                arrayList.add("DISABLED");
                b2 = (byte) 1;
            } else {
                b2 = 0;
            }
            if ((b & 2) == 2) {
                arrayList.add("SENSE");
                b2 = (byte) (b2 | 2);
            }
            if (b != b2) {
                arrayList.add("0x" + Integer.toHexString(Byte.toUnsignedInt((byte) (b & (~b2)))));
            }
            return String.join(" | ", arrayList);
        }
    }

    public static final class ExtRadioWorkDefaults {
        public static final int TIMEOUT_IN_SECS = 10;

        public static final String toString(int i) {
            if (i == 10) {
                return "TIMEOUT_IN_SECS";
            }
            return "0x" + Integer.toHexString(i);
        }

        public static final String dumpBitfield(int i) {
            ArrayList arrayList = new ArrayList();
            int i2 = 10;
            if ((i & 10) == 10) {
                arrayList.add("TIMEOUT_IN_SECS");
            } else {
                i2 = 0;
            }
            if (i != i2) {
                arrayList.add("0x" + Integer.toHexString(i & (~i2)));
            }
            return String.join(" | ", arrayList);
        }
    }

    public static final class Proxy implements ISupplicantStaIface {
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
                return "[class or subclass of android.hardware.wifi.supplicant@1.0::ISupplicantStaIface]@Proxy";
            }
        }

        public final boolean equals(Object obj) {
            return HidlSupport.interfacesEqual(this, obj);
        }

        public final int hashCode() {
            return asBinder().hashCode();
        }

        @Override
        public void getName(ISupplicantIface.getNameCallback getnamecallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(1, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                getnamecallback.onValues(supplicantStatus, hwParcel2.readString());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getType(ISupplicantIface.getTypeCallback gettypecallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(2, hwParcel, hwParcel2, 0);
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
        public void addNetwork(ISupplicantIface.addNetworkCallback addnetworkcallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(3, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                addnetworkcallback.onValues(supplicantStatus, ISupplicantNetwork.asInterface(hwParcel2.readStrongBinder()));
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus removeNetwork(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            hwParcel.writeInt32(i);
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
        public void getNetwork(int i, ISupplicantIface.getNetworkCallback getnetworkcallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(5, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                getnetworkcallback.onValues(supplicantStatus, ISupplicantNetwork.asInterface(hwParcel2.readStrongBinder()));
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void listNetworks(ISupplicantIface.listNetworksCallback listnetworkscallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(6, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                listnetworkscallback.onValues(supplicantStatus, hwParcel2.readInt32Vector());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setWpsDeviceName(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            hwParcel.writeString(str);
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
        public SupplicantStatus setWpsDeviceType(byte[] bArr) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            HwBlob hwBlob = new HwBlob(8);
            hwBlob.putInt8Array(0L, bArr);
            hwParcel.writeBuffer(hwBlob);
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
        public SupplicantStatus setWpsManufacturer(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            hwParcel.writeString(str);
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
        public SupplicantStatus setWpsModelName(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            hwParcel.writeString(str);
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
        public SupplicantStatus setWpsModelNumber(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            hwParcel.writeString(str);
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
        public SupplicantStatus setWpsSerialNumber(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            hwParcel.writeString(str);
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
        public SupplicantStatus setWpsConfigMethods(short s) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantIface.kInterfaceName);
            hwParcel.writeInt16(s);
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
        public SupplicantStatus registerCallback(ISupplicantStaIfaceCallback iSupplicantStaIfaceCallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            hwParcel.writeStrongBinder(iSupplicantStaIfaceCallback == null ? null : iSupplicantStaIfaceCallback.asBinder());
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
        public SupplicantStatus reassociate() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
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
        public SupplicantStatus reconnect() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
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
        public SupplicantStatus disconnect() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
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
        public SupplicantStatus setPowerSave(boolean z) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            hwParcel.writeBool(z);
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
        public SupplicantStatus initiateTdlsDiscover(byte[] bArr) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            HwBlob hwBlob = new HwBlob(6);
            hwBlob.putInt8Array(0L, bArr);
            hwParcel.writeBuffer(hwBlob);
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
        public SupplicantStatus initiateTdlsSetup(byte[] bArr) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            HwBlob hwBlob = new HwBlob(6);
            hwBlob.putInt8Array(0L, bArr);
            hwParcel.writeBuffer(hwBlob);
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
        public SupplicantStatus initiateTdlsTeardown(byte[] bArr) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            HwBlob hwBlob = new HwBlob(6);
            hwBlob.putInt8Array(0L, bArr);
            hwParcel.writeBuffer(hwBlob);
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
        public SupplicantStatus initiateAnqpQuery(byte[] bArr, ArrayList<Short> arrayList, ArrayList<Integer> arrayList2) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            HwBlob hwBlob = new HwBlob(6);
            hwBlob.putInt8Array(0L, bArr);
            hwParcel.writeBuffer(hwBlob);
            hwParcel.writeInt16Vector(arrayList);
            hwParcel.writeInt32Vector(arrayList2);
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
        public SupplicantStatus initiateHs20IconQuery(byte[] bArr, String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            HwBlob hwBlob = new HwBlob(6);
            hwBlob.putInt8Array(0L, bArr);
            hwParcel.writeBuffer(hwBlob);
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
        public void getMacAddress(getMacAddressCallback getmacaddresscallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(24, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                byte[] bArr = new byte[6];
                hwParcel2.readBuffer(6L).copyToInt8Array(0L, bArr, 6);
                getmacaddresscallback.onValues(supplicantStatus, bArr);
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus startRxFilter() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
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
        public SupplicantStatus stopRxFilter() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
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
        public SupplicantStatus addRxFilter(byte b) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            hwParcel.writeInt8(b);
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
        public SupplicantStatus removeRxFilter(byte b) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            hwParcel.writeInt8(b);
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
        public SupplicantStatus setBtCoexistenceMode(byte b) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            hwParcel.writeInt8(b);
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
        public SupplicantStatus setBtCoexistenceScanModeEnabled(boolean z) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            hwParcel.writeBool(z);
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
        public SupplicantStatus setSuspendModeEnabled(boolean z) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            hwParcel.writeBool(z);
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
        public SupplicantStatus setCountryCode(byte[] bArr) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            HwBlob hwBlob = new HwBlob(2);
            hwBlob.putInt8Array(0L, bArr);
            hwParcel.writeBuffer(hwBlob);
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
        public SupplicantStatus startWpsRegistrar(byte[] bArr, String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            HwBlob hwBlob = new HwBlob(6);
            hwBlob.putInt8Array(0L, bArr);
            hwParcel.writeBuffer(hwBlob);
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
        public SupplicantStatus startWpsPbc(byte[] bArr) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            HwBlob hwBlob = new HwBlob(6);
            hwBlob.putInt8Array(0L, bArr);
            hwParcel.writeBuffer(hwBlob);
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
        public SupplicantStatus startWpsPinKeypad(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(35, hwParcel, hwParcel2, 0);
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
        public void startWpsPinDisplay(byte[] bArr, startWpsPinDisplayCallback startwpspindisplaycallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            HwBlob hwBlob = new HwBlob(6);
            hwBlob.putInt8Array(0L, bArr);
            hwParcel.writeBuffer(hwBlob);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(36, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                startwpspindisplaycallback.onValues(supplicantStatus, hwParcel2.readString());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus cancelWps() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(37, hwParcel, hwParcel2, 0);
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
        public SupplicantStatus setExternalSim(boolean z) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            hwParcel.writeBool(z);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(38, hwParcel, hwParcel2, 0);
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
        public void addExtRadioWork(String str, int i, int i2, addExtRadioWorkCallback addextradioworkcallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            hwParcel.writeString(str);
            hwParcel.writeInt32(i);
            hwParcel.writeInt32(i2);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(39, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                addextradioworkcallback.onValues(supplicantStatus, hwParcel2.readInt32());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus removeExtRadioWork(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(40, hwParcel, hwParcel2, 0);
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
        public SupplicantStatus enableAutoReconnect(boolean z) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantStaIface.kInterfaceName);
            hwParcel.writeBool(z);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(41, hwParcel, hwParcel2, 0);
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

    public static abstract class Stub extends HwBinder implements ISupplicantStaIface {
        @Override
        public IHwBinder asBinder() {
            return this;
        }

        @Override
        public final ArrayList<String> interfaceChain() {
            return new ArrayList<>(Arrays.asList(ISupplicantStaIface.kInterfaceName, ISupplicantIface.kInterfaceName, IBase.kInterfaceName));
        }

        @Override
        public final String interfaceDescriptor() {
            return ISupplicantStaIface.kInterfaceName;
        }

        @Override
        public final ArrayList<byte[]> getHashChain() {
            return new ArrayList<>(Arrays.asList(new byte[]{119, 82, -31, -34, -109, -86, -11, -2, -45, 112, 17, -62, 25, -84, 36, 112, 105, -10, -81, 50, 11, 8, 16, -38, -87, -123, 16, 88, 74, 16, -25, -76}, new byte[]{53, -70, 123, -51, -15, -113, 36, -88, 102, -89, -27, 66, -107, 72, -16, 103, 104, -69, 32, -94, 87, -9, 91, 16, -93, -105, -60, -40, 37, -17, -124, 56}, new byte[]{-67, -38, -74, 24, 77, 122, 52, 109, -90, -96, 125, -64, -126, -116, -15, -102, 105, 111, 76, -86, 54, 17, -59, 31, 46, 20, 86, 90, 20, -76, 15, -39}));
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
            if (ISupplicantStaIface.kInterfaceName.equals(str)) {
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
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantIface.kInterfaceName);
                        getName(new ISupplicantIface.getNameCallback() {
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
                case 2:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantIface.kInterfaceName);
                        getType(new ISupplicantIface.getTypeCallback() {
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
                case 3:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantIface.kInterfaceName);
                        addNetwork(new ISupplicantIface.addNetworkCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, ISupplicantNetwork iSupplicantNetwork) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeStrongBinder(iSupplicantNetwork == null ? null : iSupplicantNetwork.asBinder());
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
                        hwParcel.enforceInterface(ISupplicantIface.kInterfaceName);
                        SupplicantStatus supplicantStatusRemoveNetwork = removeNetwork(hwParcel.readInt32());
                        hwParcel2.writeStatus(0);
                        supplicantStatusRemoveNetwork.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 5:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantIface.kInterfaceName);
                        getNetwork(hwParcel.readInt32(), new ISupplicantIface.getNetworkCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, ISupplicantNetwork iSupplicantNetwork) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeStrongBinder(iSupplicantNetwork == null ? null : iSupplicantNetwork.asBinder());
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 6:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantIface.kInterfaceName);
                        listNetworks(new ISupplicantIface.listNetworksCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, ArrayList<Integer> arrayList) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeInt32Vector(arrayList);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 7:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantIface.kInterfaceName);
                        SupplicantStatus wpsDeviceName = setWpsDeviceName(hwParcel.readString());
                        hwParcel2.writeStatus(0);
                        wpsDeviceName.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 8:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantIface.kInterfaceName);
                        byte[] bArr = new byte[8];
                        hwParcel.readBuffer(8L).copyToInt8Array(0L, bArr, 8);
                        SupplicantStatus wpsDeviceType = setWpsDeviceType(bArr);
                        hwParcel2.writeStatus(0);
                        wpsDeviceType.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 9:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantIface.kInterfaceName);
                        SupplicantStatus wpsManufacturer = setWpsManufacturer(hwParcel.readString());
                        hwParcel2.writeStatus(0);
                        wpsManufacturer.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 10:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantIface.kInterfaceName);
                        SupplicantStatus wpsModelName = setWpsModelName(hwParcel.readString());
                        hwParcel2.writeStatus(0);
                        wpsModelName.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 11:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantIface.kInterfaceName);
                        SupplicantStatus wpsModelNumber = setWpsModelNumber(hwParcel.readString());
                        hwParcel2.writeStatus(0);
                        wpsModelNumber.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 12:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantIface.kInterfaceName);
                        SupplicantStatus wpsSerialNumber = setWpsSerialNumber(hwParcel.readString());
                        hwParcel2.writeStatus(0);
                        wpsSerialNumber.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 13:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantIface.kInterfaceName);
                        SupplicantStatus wpsConfigMethods = setWpsConfigMethods(hwParcel.readInt16());
                        hwParcel2.writeStatus(0);
                        wpsConfigMethods.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 14:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaIface.kInterfaceName);
                        SupplicantStatus supplicantStatusRegisterCallback = registerCallback(ISupplicantStaIfaceCallback.asInterface(hwParcel.readStrongBinder()));
                        hwParcel2.writeStatus(0);
                        supplicantStatusRegisterCallback.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 15:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaIface.kInterfaceName);
                        SupplicantStatus supplicantStatusReassociate = reassociate();
                        hwParcel2.writeStatus(0);
                        supplicantStatusReassociate.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 16:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaIface.kInterfaceName);
                        SupplicantStatus supplicantStatusReconnect = reconnect();
                        hwParcel2.writeStatus(0);
                        supplicantStatusReconnect.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 17:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaIface.kInterfaceName);
                        SupplicantStatus supplicantStatusDisconnect = disconnect();
                        hwParcel2.writeStatus(0);
                        supplicantStatusDisconnect.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 18:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaIface.kInterfaceName);
                        SupplicantStatus powerSave = setPowerSave(hwParcel.readBool());
                        hwParcel2.writeStatus(0);
                        powerSave.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 19:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaIface.kInterfaceName);
                        byte[] bArr2 = new byte[6];
                        hwParcel.readBuffer(6L).copyToInt8Array(0L, bArr2, 6);
                        SupplicantStatus supplicantStatusInitiateTdlsDiscover = initiateTdlsDiscover(bArr2);
                        hwParcel2.writeStatus(0);
                        supplicantStatusInitiateTdlsDiscover.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 20:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaIface.kInterfaceName);
                        byte[] bArr3 = new byte[6];
                        hwParcel.readBuffer(6L).copyToInt8Array(0L, bArr3, 6);
                        SupplicantStatus supplicantStatusInitiateTdlsSetup = initiateTdlsSetup(bArr3);
                        hwParcel2.writeStatus(0);
                        supplicantStatusInitiateTdlsSetup.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case ISupplicantStaIfaceCallback.ReasonCode.UNSUPPORTED_RSN_IE_VERSION:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaIface.kInterfaceName);
                        byte[] bArr4 = new byte[6];
                        hwParcel.readBuffer(6L).copyToInt8Array(0L, bArr4, 6);
                        SupplicantStatus supplicantStatusInitiateTdlsTeardown = initiateTdlsTeardown(bArr4);
                        hwParcel2.writeStatus(0);
                        supplicantStatusInitiateTdlsTeardown.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 22:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaIface.kInterfaceName);
                        byte[] bArr5 = new byte[6];
                        hwParcel.readBuffer(6L).copyToInt8Array(0L, bArr5, 6);
                        SupplicantStatus supplicantStatusInitiateAnqpQuery = initiateAnqpQuery(bArr5, hwParcel.readInt16Vector(), hwParcel.readInt32Vector());
                        hwParcel2.writeStatus(0);
                        supplicantStatusInitiateAnqpQuery.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 23:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaIface.kInterfaceName);
                        byte[] bArr6 = new byte[6];
                        hwParcel.readBuffer(6L).copyToInt8Array(0L, bArr6, 6);
                        SupplicantStatus supplicantStatusInitiateHs20IconQuery = initiateHs20IconQuery(bArr6, hwParcel.readString());
                        hwParcel2.writeStatus(0);
                        supplicantStatusInitiateHs20IconQuery.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 24:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaIface.kInterfaceName);
                        getMacAddress(new getMacAddressCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, byte[] bArr7) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                HwBlob hwBlob = new HwBlob(6);
                                hwBlob.putInt8Array(0L, bArr7);
                                hwParcel2.writeBuffer(hwBlob);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 25:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaIface.kInterfaceName);
                        SupplicantStatus supplicantStatusStartRxFilter = startRxFilter();
                        hwParcel2.writeStatus(0);
                        supplicantStatusStartRxFilter.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case ISupplicantStaIfaceCallback.ReasonCode.TDLS_TEARDOWN_UNSPECIFIED:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaIface.kInterfaceName);
                        SupplicantStatus supplicantStatusStopRxFilter = stopRxFilter();
                        hwParcel2.writeStatus(0);
                        supplicantStatusStopRxFilter.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 27:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaIface.kInterfaceName);
                        SupplicantStatus supplicantStatusAddRxFilter = addRxFilter(hwParcel.readInt8());
                        hwParcel2.writeStatus(0);
                        supplicantStatusAddRxFilter.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 28:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaIface.kInterfaceName);
                        SupplicantStatus supplicantStatusRemoveRxFilter = removeRxFilter(hwParcel.readInt8());
                        hwParcel2.writeStatus(0);
                        supplicantStatusRemoveRxFilter.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 29:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaIface.kInterfaceName);
                        SupplicantStatus btCoexistenceMode = setBtCoexistenceMode(hwParcel.readInt8());
                        hwParcel2.writeStatus(0);
                        btCoexistenceMode.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 30:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaIface.kInterfaceName);
                        SupplicantStatus btCoexistenceScanModeEnabled = setBtCoexistenceScanModeEnabled(hwParcel.readBool());
                        hwParcel2.writeStatus(0);
                        btCoexistenceScanModeEnabled.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 31:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaIface.kInterfaceName);
                        SupplicantStatus suspendModeEnabled = setSuspendModeEnabled(hwParcel.readBool());
                        hwParcel2.writeStatus(0);
                        suspendModeEnabled.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 32:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaIface.kInterfaceName);
                        byte[] bArr7 = new byte[2];
                        hwParcel.readBuffer(2L).copyToInt8Array(0L, bArr7, 2);
                        SupplicantStatus countryCode = setCountryCode(bArr7);
                        hwParcel2.writeStatus(0);
                        countryCode.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 33:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaIface.kInterfaceName);
                        byte[] bArr8 = new byte[6];
                        hwParcel.readBuffer(6L).copyToInt8Array(0L, bArr8, 6);
                        SupplicantStatus supplicantStatusStartWpsRegistrar = startWpsRegistrar(bArr8, hwParcel.readString());
                        hwParcel2.writeStatus(0);
                        supplicantStatusStartWpsRegistrar.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 34:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaIface.kInterfaceName);
                        byte[] bArr9 = new byte[6];
                        hwParcel.readBuffer(6L).copyToInt8Array(0L, bArr9, 6);
                        SupplicantStatus supplicantStatusStartWpsPbc = startWpsPbc(bArr9);
                        hwParcel2.writeStatus(0);
                        supplicantStatusStartWpsPbc.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 35:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaIface.kInterfaceName);
                        SupplicantStatus supplicantStatusStartWpsPinKeypad = startWpsPinKeypad(hwParcel.readString());
                        hwParcel2.writeStatus(0);
                        supplicantStatusStartWpsPinKeypad.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case ISupplicantStaIfaceCallback.ReasonCode.STA_LEAVING:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaIface.kInterfaceName);
                        byte[] bArr10 = new byte[6];
                        hwParcel.readBuffer(6L).copyToInt8Array(0L, bArr10, 6);
                        startWpsPinDisplay(bArr10, new startWpsPinDisplayCallback() {
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
                case 37:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaIface.kInterfaceName);
                        SupplicantStatus supplicantStatusCancelWps = cancelWps();
                        hwParcel2.writeStatus(0);
                        supplicantStatusCancelWps.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 38:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaIface.kInterfaceName);
                        SupplicantStatus externalSim = setExternalSim(hwParcel.readBool());
                        hwParcel2.writeStatus(0);
                        externalSim.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 39:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaIface.kInterfaceName);
                        addExtRadioWork(hwParcel.readString(), hwParcel.readInt32(), hwParcel.readInt32(), new addExtRadioWorkCallback() {
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
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaIface.kInterfaceName);
                        SupplicantStatus supplicantStatusRemoveExtRadioWork = removeExtRadioWork(hwParcel.readInt32());
                        hwParcel2.writeStatus(0);
                        supplicantStatusRemoveExtRadioWork.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case ISupplicantStaIfaceCallback.StatusCode.GROUP_CIPHER_NOT_VALID:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantStaIface.kInterfaceName);
                        SupplicantStatus supplicantStatusEnableAutoReconnect = enableAutoReconnect(hwParcel.readBool());
                        hwParcel2.writeStatus(0);
                        supplicantStatusEnableAutoReconnect.writeToParcel(hwParcel2);
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
                            if ((i2 & 1) != 0) {
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
                            if ((i2 & 1) != 0) {
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
