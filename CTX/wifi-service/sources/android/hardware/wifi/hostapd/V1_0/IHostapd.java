package android.hardware.wifi.hostapd.V1_0;

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

public interface IHostapd extends IBase {
    public static final String kInterfaceName = "android.hardware.wifi.hostapd@1.0::IHostapd";

    HostapdStatus addAccessPoint(IfaceParams ifaceParams, NetworkParams networkParams) throws RemoteException;

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

    HostapdStatus removeAccessPoint(String str) throws RemoteException;

    @Override
    void setHALInstrumentation() throws RemoteException;

    void terminate() throws RemoteException;

    @Override
    boolean unlinkToDeath(IHwBinder.DeathRecipient deathRecipient) throws RemoteException;

    static IHostapd asInterface(IHwBinder iHwBinder) {
        if (iHwBinder == null) {
            return null;
        }
        IHwInterface iHwInterfaceQueryLocalInterface = iHwBinder.queryLocalInterface(kInterfaceName);
        if (iHwInterfaceQueryLocalInterface != null && (iHwInterfaceQueryLocalInterface instanceof IHostapd)) {
            return (IHostapd) iHwInterfaceQueryLocalInterface;
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

    static IHostapd castFrom(IHwInterface iHwInterface) {
        if (iHwInterface == null) {
            return null;
        }
        return asInterface(iHwInterface.asBinder());
    }

    static IHostapd getService(String str, boolean z) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, str, z));
    }

    static IHostapd getService(boolean z) throws RemoteException {
        return getService(HalDeviceManager.HAL_INSTANCE_NAME, z);
    }

    static IHostapd getService(String str) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, str));
    }

    static IHostapd getService() throws RemoteException {
        return getService(HalDeviceManager.HAL_INSTANCE_NAME);
    }

    public static final class ParamSizeLimits {
        public static final int SSID_MAX_LEN_IN_BYTES = 32;
        public static final int WPA2_PSK_PASSPHRASE_MAX_LEN_IN_BYTES = 63;
        public static final int WPA2_PSK_PASSPHRASE_MIN_LEN_IN_BYTES = 8;

        public static final String toString(int i) {
            if (i == 32) {
                return "SSID_MAX_LEN_IN_BYTES";
            }
            if (i == 8) {
                return "WPA2_PSK_PASSPHRASE_MIN_LEN_IN_BYTES";
            }
            if (i == 63) {
                return "WPA2_PSK_PASSPHRASE_MAX_LEN_IN_BYTES";
            }
            return "0x" + Integer.toHexString(i);
        }

        public static final String dumpBitfield(int i) {
            ArrayList arrayList = new ArrayList();
            int i2 = 32;
            if ((i & 32) == 32) {
                arrayList.add("SSID_MAX_LEN_IN_BYTES");
            } else {
                i2 = 0;
            }
            if ((i & 8) == 8) {
                arrayList.add("WPA2_PSK_PASSPHRASE_MIN_LEN_IN_BYTES");
                i2 |= 8;
            }
            if ((i & 63) == 63) {
                arrayList.add("WPA2_PSK_PASSPHRASE_MAX_LEN_IN_BYTES");
                i2 |= 63;
            }
            if (i != i2) {
                arrayList.add("0x" + Integer.toHexString(i & (~i2)));
            }
            return String.join(" | ", arrayList);
        }
    }

    public static final class EncryptionType {
        public static final int NONE = 0;
        public static final int WPA = 1;
        public static final int WPA2 = 2;

        public static final String toString(int i) {
            if (i == 0) {
                return "NONE";
            }
            if (i == 1) {
                return "WPA";
            }
            if (i == 2) {
                return "WPA2";
            }
            return "0x" + Integer.toHexString(i);
        }

        public static final String dumpBitfield(int i) {
            ArrayList arrayList = new ArrayList();
            arrayList.add("NONE");
            int i2 = 1;
            if ((i & 1) == 1) {
                arrayList.add("WPA");
            } else {
                i2 = 0;
            }
            if ((i & 2) == 2) {
                arrayList.add("WPA2");
                i2 |= 2;
            }
            if (i != i2) {
                arrayList.add("0x" + Integer.toHexString(i & (~i2)));
            }
            return String.join(" | ", arrayList);
        }
    }

    public static final class Band {
        public static final int BAND_2_4_GHZ = 0;
        public static final int BAND_5_GHZ = 1;
        public static final int BAND_ANY = 2;

        public static final String toString(int i) {
            if (i == 0) {
                return "BAND_2_4_GHZ";
            }
            if (i == 1) {
                return "BAND_5_GHZ";
            }
            if (i == 2) {
                return "BAND_ANY";
            }
            return "0x" + Integer.toHexString(i);
        }

        public static final String dumpBitfield(int i) {
            ArrayList arrayList = new ArrayList();
            arrayList.add("BAND_2_4_GHZ");
            int i2 = 1;
            if ((i & 1) == 1) {
                arrayList.add("BAND_5_GHZ");
            } else {
                i2 = 0;
            }
            if ((i & 2) == 2) {
                arrayList.add("BAND_ANY");
                i2 |= 2;
            }
            if (i != i2) {
                arrayList.add("0x" + Integer.toHexString(i & (~i2)));
            }
            return String.join(" | ", arrayList);
        }
    }

    public static final class HwModeParams {
        public boolean enable80211AC;
        public boolean enable80211N;

        public final boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || obj.getClass() != HwModeParams.class) {
                return false;
            }
            HwModeParams hwModeParams = (HwModeParams) obj;
            if (this.enable80211N == hwModeParams.enable80211N && this.enable80211AC == hwModeParams.enable80211AC) {
                return true;
            }
            return false;
        }

        public final int hashCode() {
            return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.enable80211N))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.enable80211AC))));
        }

        public final String toString() {
            return "{.enable80211N = " + this.enable80211N + ", .enable80211AC = " + this.enable80211AC + "}";
        }

        public final void readFromParcel(HwParcel hwParcel) {
            readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(2L), 0L);
        }

        public static final ArrayList<HwModeParams> readVectorFromParcel(HwParcel hwParcel) {
            ArrayList<HwModeParams> arrayList = new ArrayList<>();
            HwBlob buffer = hwParcel.readBuffer(16L);
            int int32 = buffer.getInt32(8L);
            HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 2, buffer.handle(), 0L, true);
            arrayList.clear();
            for (int i = 0; i < int32; i++) {
                HwModeParams hwModeParams = new HwModeParams();
                hwModeParams.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 2);
                arrayList.add(hwModeParams);
            }
            return arrayList;
        }

        public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
            this.enable80211N = hwBlob.getBool(0 + j);
            this.enable80211AC = hwBlob.getBool(j + 1);
        }

        public final void writeToParcel(HwParcel hwParcel) {
            HwBlob hwBlob = new HwBlob(2);
            writeEmbeddedToBlob(hwBlob, 0L);
            hwParcel.writeBuffer(hwBlob);
        }

        public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<HwModeParams> arrayList) {
            HwBlob hwBlob = new HwBlob(16);
            int size = arrayList.size();
            hwBlob.putInt32(8L, size);
            hwBlob.putBool(12L, false);
            HwBlob hwBlob2 = new HwBlob(size * 2);
            for (int i = 0; i < size; i++) {
                arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 2);
            }
            hwBlob.putBlob(0L, hwBlob2);
            hwParcel.writeBuffer(hwBlob);
        }

        public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
            hwBlob.putBool(0 + j, this.enable80211N);
            hwBlob.putBool(j + 1, this.enable80211AC);
        }
    }

    public static final class ChannelParams {
        public boolean acsShouldExcludeDfs;
        public int band;
        public int channel;
        public boolean enableAcs;

        public final boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || obj.getClass() != ChannelParams.class) {
                return false;
            }
            ChannelParams channelParams = (ChannelParams) obj;
            if (this.enableAcs == channelParams.enableAcs && this.acsShouldExcludeDfs == channelParams.acsShouldExcludeDfs && this.channel == channelParams.channel && this.band == channelParams.band) {
                return true;
            }
            return false;
        }

        public final int hashCode() {
            return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.enableAcs))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.acsShouldExcludeDfs))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.channel))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.band))));
        }

        public final String toString() {
            return "{.enableAcs = " + this.enableAcs + ", .acsShouldExcludeDfs = " + this.acsShouldExcludeDfs + ", .channel = " + this.channel + ", .band = " + Band.toString(this.band) + "}";
        }

        public final void readFromParcel(HwParcel hwParcel) {
            readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(12L), 0L);
        }

        public static final ArrayList<ChannelParams> readVectorFromParcel(HwParcel hwParcel) {
            ArrayList<ChannelParams> arrayList = new ArrayList<>();
            HwBlob buffer = hwParcel.readBuffer(16L);
            int int32 = buffer.getInt32(8L);
            HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 12, buffer.handle(), 0L, true);
            arrayList.clear();
            for (int i = 0; i < int32; i++) {
                ChannelParams channelParams = new ChannelParams();
                channelParams.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 12);
                arrayList.add(channelParams);
            }
            return arrayList;
        }

        public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
            this.enableAcs = hwBlob.getBool(0 + j);
            this.acsShouldExcludeDfs = hwBlob.getBool(1 + j);
            this.channel = hwBlob.getInt32(4 + j);
            this.band = hwBlob.getInt32(j + 8);
        }

        public final void writeToParcel(HwParcel hwParcel) {
            HwBlob hwBlob = new HwBlob(12);
            writeEmbeddedToBlob(hwBlob, 0L);
            hwParcel.writeBuffer(hwBlob);
        }

        public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<ChannelParams> arrayList) {
            HwBlob hwBlob = new HwBlob(16);
            int size = arrayList.size();
            hwBlob.putInt32(8L, size);
            hwBlob.putBool(12L, false);
            HwBlob hwBlob2 = new HwBlob(size * 12);
            for (int i = 0; i < size; i++) {
                arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 12);
            }
            hwBlob.putBlob(0L, hwBlob2);
            hwParcel.writeBuffer(hwBlob);
        }

        public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
            hwBlob.putBool(0 + j, this.enableAcs);
            hwBlob.putBool(1 + j, this.acsShouldExcludeDfs);
            hwBlob.putInt32(4 + j, this.channel);
            hwBlob.putInt32(j + 8, this.band);
        }
    }

    public static final class IfaceParams {
        public String ifaceName = new String();
        public final HwModeParams hwModeParams = new HwModeParams();
        public final ChannelParams channelParams = new ChannelParams();

        public final boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || obj.getClass() != IfaceParams.class) {
                return false;
            }
            IfaceParams ifaceParams = (IfaceParams) obj;
            if (HidlSupport.deepEquals(this.ifaceName, ifaceParams.ifaceName) && HidlSupport.deepEquals(this.hwModeParams, ifaceParams.hwModeParams) && HidlSupport.deepEquals(this.channelParams, ifaceParams.channelParams)) {
                return true;
            }
            return false;
        }

        public final int hashCode() {
            return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(this.ifaceName)), Integer.valueOf(HidlSupport.deepHashCode(this.hwModeParams)), Integer.valueOf(HidlSupport.deepHashCode(this.channelParams)));
        }

        public final String toString() {
            return "{.ifaceName = " + this.ifaceName + ", .hwModeParams = " + this.hwModeParams + ", .channelParams = " + this.channelParams + "}";
        }

        public final void readFromParcel(HwParcel hwParcel) {
            readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(32L), 0L);
        }

        public static final ArrayList<IfaceParams> readVectorFromParcel(HwParcel hwParcel) {
            ArrayList<IfaceParams> arrayList = new ArrayList<>();
            HwBlob buffer = hwParcel.readBuffer(16L);
            int int32 = buffer.getInt32(8L);
            HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 32, buffer.handle(), 0L, true);
            arrayList.clear();
            for (int i = 0; i < int32; i++) {
                IfaceParams ifaceParams = new IfaceParams();
                ifaceParams.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 32);
                arrayList.add(ifaceParams);
            }
            return arrayList;
        }

        public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
            long j2 = j + 0;
            this.ifaceName = hwBlob.getString(j2);
            hwParcel.readEmbeddedBuffer(this.ifaceName.getBytes().length + 1, hwBlob.handle(), j2 + 0, false);
            this.hwModeParams.readEmbeddedFromParcel(hwParcel, hwBlob, j + 16);
            this.channelParams.readEmbeddedFromParcel(hwParcel, hwBlob, j + 20);
        }

        public final void writeToParcel(HwParcel hwParcel) {
            HwBlob hwBlob = new HwBlob(32);
            writeEmbeddedToBlob(hwBlob, 0L);
            hwParcel.writeBuffer(hwBlob);
        }

        public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<IfaceParams> arrayList) {
            HwBlob hwBlob = new HwBlob(16);
            int size = arrayList.size();
            hwBlob.putInt32(8L, size);
            hwBlob.putBool(12L, false);
            HwBlob hwBlob2 = new HwBlob(size * 32);
            for (int i = 0; i < size; i++) {
                arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 32);
            }
            hwBlob.putBlob(0L, hwBlob2);
            hwParcel.writeBuffer(hwBlob);
        }

        public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
            hwBlob.putString(0 + j, this.ifaceName);
            this.hwModeParams.writeEmbeddedToBlob(hwBlob, 16 + j);
            this.channelParams.writeEmbeddedToBlob(hwBlob, j + 20);
        }
    }

    public static final class NetworkParams {
        public int encryptionType;
        public boolean isHidden;
        public final ArrayList<Byte> ssid = new ArrayList<>();
        public String pskPassphrase = new String();

        public final boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || obj.getClass() != NetworkParams.class) {
                return false;
            }
            NetworkParams networkParams = (NetworkParams) obj;
            if (HidlSupport.deepEquals(this.ssid, networkParams.ssid) && this.isHidden == networkParams.isHidden && this.encryptionType == networkParams.encryptionType && HidlSupport.deepEquals(this.pskPassphrase, networkParams.pskPassphrase)) {
                return true;
            }
            return false;
        }

        public final int hashCode() {
            return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(this.ssid)), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.isHidden))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.encryptionType))), Integer.valueOf(HidlSupport.deepHashCode(this.pskPassphrase)));
        }

        public final String toString() {
            return "{.ssid = " + this.ssid + ", .isHidden = " + this.isHidden + ", .encryptionType = " + EncryptionType.toString(this.encryptionType) + ", .pskPassphrase = " + this.pskPassphrase + "}";
        }

        public final void readFromParcel(HwParcel hwParcel) {
            readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(40L), 0L);
        }

        public static final ArrayList<NetworkParams> readVectorFromParcel(HwParcel hwParcel) {
            ArrayList<NetworkParams> arrayList = new ArrayList<>();
            HwBlob buffer = hwParcel.readBuffer(16L);
            int int32 = buffer.getInt32(8L);
            HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 40, buffer.handle(), 0L, true);
            arrayList.clear();
            for (int i = 0; i < int32; i++) {
                NetworkParams networkParams = new NetworkParams();
                networkParams.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 40);
                arrayList.add(networkParams);
            }
            return arrayList;
        }

        public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
            long j2 = j + 0;
            int int32 = hwBlob.getInt32(8 + j2);
            HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 1, hwBlob.handle(), j2 + 0, true);
            this.ssid.clear();
            for (int i = 0; i < int32; i++) {
                this.ssid.add(Byte.valueOf(embeddedBuffer.getInt8(i * 1)));
            }
            this.isHidden = hwBlob.getBool(j + 16);
            this.encryptionType = hwBlob.getInt32(j + 20);
            long j3 = j + 24;
            this.pskPassphrase = hwBlob.getString(j3);
            hwParcel.readEmbeddedBuffer(this.pskPassphrase.getBytes().length + 1, hwBlob.handle(), j3 + 0, false);
        }

        public final void writeToParcel(HwParcel hwParcel) {
            HwBlob hwBlob = new HwBlob(40);
            writeEmbeddedToBlob(hwBlob, 0L);
            hwParcel.writeBuffer(hwBlob);
        }

        public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<NetworkParams> arrayList) {
            HwBlob hwBlob = new HwBlob(16);
            int size = arrayList.size();
            hwBlob.putInt32(8L, size);
            hwBlob.putBool(12L, false);
            HwBlob hwBlob2 = new HwBlob(size * 40);
            for (int i = 0; i < size; i++) {
                arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 40);
            }
            hwBlob.putBlob(0L, hwBlob2);
            hwParcel.writeBuffer(hwBlob);
        }

        public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
            int size = this.ssid.size();
            long j2 = j + 0;
            hwBlob.putInt32(8 + j2, size);
            hwBlob.putBool(12 + j2, false);
            HwBlob hwBlob2 = new HwBlob(size * 1);
            for (int i = 0; i < size; i++) {
                hwBlob2.putInt8(i * 1, this.ssid.get(i).byteValue());
            }
            hwBlob.putBlob(j2 + 0, hwBlob2);
            hwBlob.putBool(16 + j, this.isHidden);
            hwBlob.putInt32(20 + j, this.encryptionType);
            hwBlob.putString(j + 24, this.pskPassphrase);
        }
    }

    public static final class Proxy implements IHostapd {
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
                return "[class or subclass of android.hardware.wifi.hostapd@1.0::IHostapd]@Proxy";
            }
        }

        public final boolean equals(Object obj) {
            return HidlSupport.interfacesEqual(this, obj);
        }

        public final int hashCode() {
            return asBinder().hashCode();
        }

        @Override
        public HostapdStatus addAccessPoint(IfaceParams ifaceParams, NetworkParams networkParams) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IHostapd.kInterfaceName);
            ifaceParams.writeToParcel(hwParcel);
            networkParams.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(1, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                HostapdStatus hostapdStatus = new HostapdStatus();
                hostapdStatus.readFromParcel(hwParcel2);
                return hostapdStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public HostapdStatus removeAccessPoint(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IHostapd.kInterfaceName);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(2, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                HostapdStatus hostapdStatus = new HostapdStatus();
                hostapdStatus.readFromParcel(hwParcel2);
                return hostapdStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void terminate() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IHostapd.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(3, hwParcel, hwParcel2, 1);
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

    public static abstract class Stub extends HwBinder implements IHostapd {
        @Override
        public IHwBinder asBinder() {
            return this;
        }

        @Override
        public final ArrayList<String> interfaceChain() {
            return new ArrayList<>(Arrays.asList(IHostapd.kInterfaceName, IBase.kInterfaceName));
        }

        @Override
        public final String interfaceDescriptor() {
            return IHostapd.kInterfaceName;
        }

        @Override
        public final ArrayList<byte[]> getHashChain() {
            return new ArrayList<>(Arrays.asList(new byte[]{-18, 8, 40, 13, -30, 28, -76, 30, 62, -62, 109, 110, -42, 54, -57, 1, -73, -9, 5, 22, -25, 31, -78, 47, 79, -26, 10, 19, -26, 3, -12, 6}, new byte[]{-67, -38, -74, 24, 77, 122, 52, 109, -90, -96, 125, -64, -126, -116, -15, -102, 105, 111, 76, -86, 54, 17, -59, 31, 46, 20, 86, 90, 20, -76, 15, -39}));
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
            if (IHostapd.kInterfaceName.equals(str)) {
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
                        hwParcel.enforceInterface(IHostapd.kInterfaceName);
                        IfaceParams ifaceParams = new IfaceParams();
                        ifaceParams.readFromParcel(hwParcel);
                        NetworkParams networkParams = new NetworkParams();
                        networkParams.readFromParcel(hwParcel);
                        HostapdStatus hostapdStatusAddAccessPoint = addAccessPoint(ifaceParams, networkParams);
                        hwParcel2.writeStatus(0);
                        hostapdStatusAddAccessPoint.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 2:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IHostapd.kInterfaceName);
                        HostapdStatus hostapdStatusRemoveAccessPoint = removeAccessPoint(hwParcel.readString());
                        hwParcel2.writeStatus(0);
                        hostapdStatusRemoveAccessPoint.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 3:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IHostapd.kInterfaceName);
                        terminate();
                    }
                    break;
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
        }
    }
}
