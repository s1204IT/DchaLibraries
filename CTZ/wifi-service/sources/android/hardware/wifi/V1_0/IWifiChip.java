package android.hardware.wifi.V1_0;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;

public interface IWifiChip extends IBase {
    public static final String kInterfaceName = "android.hardware.wifi@1.0::IWifiChip";

    @FunctionalInterface
    public interface createApIfaceCallback {
        void onValues(WifiStatus wifiStatus, IWifiApIface iWifiApIface);
    }

    @FunctionalInterface
    public interface createNanIfaceCallback {
        void onValues(WifiStatus wifiStatus, IWifiNanIface iWifiNanIface);
    }

    @FunctionalInterface
    public interface createP2pIfaceCallback {
        void onValues(WifiStatus wifiStatus, IWifiP2pIface iWifiP2pIface);
    }

    @FunctionalInterface
    public interface createRttControllerCallback {
        void onValues(WifiStatus wifiStatus, IWifiRttController iWifiRttController);
    }

    @FunctionalInterface
    public interface createStaIfaceCallback {
        void onValues(WifiStatus wifiStatus, IWifiStaIface iWifiStaIface);
    }

    @FunctionalInterface
    public interface getApIfaceCallback {
        void onValues(WifiStatus wifiStatus, IWifiApIface iWifiApIface);
    }

    @FunctionalInterface
    public interface getApIfaceNamesCallback {
        void onValues(WifiStatus wifiStatus, ArrayList<String> arrayList);
    }

    @FunctionalInterface
    public interface getAvailableModesCallback {
        void onValues(WifiStatus wifiStatus, ArrayList<ChipMode> arrayList);
    }

    @FunctionalInterface
    public interface getCapabilitiesCallback {
        void onValues(WifiStatus wifiStatus, int i);
    }

    @FunctionalInterface
    public interface getDebugHostWakeReasonStatsCallback {
        void onValues(WifiStatus wifiStatus, WifiDebugHostWakeReasonStats wifiDebugHostWakeReasonStats);
    }

    @FunctionalInterface
    public interface getDebugRingBuffersStatusCallback {
        void onValues(WifiStatus wifiStatus, ArrayList<WifiDebugRingBufferStatus> arrayList);
    }

    @FunctionalInterface
    public interface getIdCallback {
        void onValues(WifiStatus wifiStatus, int i);
    }

    @FunctionalInterface
    public interface getModeCallback {
        void onValues(WifiStatus wifiStatus, int i);
    }

    @FunctionalInterface
    public interface getNanIfaceCallback {
        void onValues(WifiStatus wifiStatus, IWifiNanIface iWifiNanIface);
    }

    @FunctionalInterface
    public interface getNanIfaceNamesCallback {
        void onValues(WifiStatus wifiStatus, ArrayList<String> arrayList);
    }

    @FunctionalInterface
    public interface getP2pIfaceCallback {
        void onValues(WifiStatus wifiStatus, IWifiP2pIface iWifiP2pIface);
    }

    @FunctionalInterface
    public interface getP2pIfaceNamesCallback {
        void onValues(WifiStatus wifiStatus, ArrayList<String> arrayList);
    }

    @FunctionalInterface
    public interface getStaIfaceCallback {
        void onValues(WifiStatus wifiStatus, IWifiStaIface iWifiStaIface);
    }

    @FunctionalInterface
    public interface getStaIfaceNamesCallback {
        void onValues(WifiStatus wifiStatus, ArrayList<String> arrayList);
    }

    @FunctionalInterface
    public interface requestChipDebugInfoCallback {
        void onValues(WifiStatus wifiStatus, ChipDebugInfo chipDebugInfo);
    }

    @FunctionalInterface
    public interface requestDriverDebugDumpCallback {
        void onValues(WifiStatus wifiStatus, ArrayList<Byte> arrayList);
    }

    @FunctionalInterface
    public interface requestFirmwareDebugDumpCallback {
        void onValues(WifiStatus wifiStatus, ArrayList<Byte> arrayList);
    }

    @Override
    IHwBinder asBinder();

    WifiStatus configureChip(int i) throws RemoteException;

    void createApIface(createApIfaceCallback createapifacecallback) throws RemoteException;

    void createNanIface(createNanIfaceCallback createnanifacecallback) throws RemoteException;

    void createP2pIface(createP2pIfaceCallback createp2pifacecallback) throws RemoteException;

    void createRttController(IWifiIface iWifiIface, createRttControllerCallback createrttcontrollercallback) throws RemoteException;

    void createStaIface(createStaIfaceCallback createstaifacecallback) throws RemoteException;

    WifiStatus enableDebugErrorAlerts(boolean z) throws RemoteException;

    WifiStatus forceDumpToDebugRingBuffer(String str) throws RemoteException;

    void getApIface(String str, getApIfaceCallback getapifacecallback) throws RemoteException;

    void getApIfaceNames(getApIfaceNamesCallback getapifacenamescallback) throws RemoteException;

    void getAvailableModes(getAvailableModesCallback getavailablemodescallback) throws RemoteException;

    void getCapabilities(getCapabilitiesCallback getcapabilitiescallback) throws RemoteException;

    void getDebugHostWakeReasonStats(getDebugHostWakeReasonStatsCallback getdebughostwakereasonstatscallback) throws RemoteException;

    @Override
    DebugInfo getDebugInfo() throws RemoteException;

    void getDebugRingBuffersStatus(getDebugRingBuffersStatusCallback getdebugringbuffersstatuscallback) throws RemoteException;

    @Override
    ArrayList<byte[]> getHashChain() throws RemoteException;

    void getId(getIdCallback getidcallback) throws RemoteException;

    void getMode(getModeCallback getmodecallback) throws RemoteException;

    void getNanIface(String str, getNanIfaceCallback getnanifacecallback) throws RemoteException;

    void getNanIfaceNames(getNanIfaceNamesCallback getnanifacenamescallback) throws RemoteException;

    void getP2pIface(String str, getP2pIfaceCallback getp2pifacecallback) throws RemoteException;

    void getP2pIfaceNames(getP2pIfaceNamesCallback getp2pifacenamescallback) throws RemoteException;

    void getStaIface(String str, getStaIfaceCallback getstaifacecallback) throws RemoteException;

    void getStaIfaceNames(getStaIfaceNamesCallback getstaifacenamescallback) throws RemoteException;

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

    WifiStatus registerEventCallback(IWifiChipEventCallback iWifiChipEventCallback) throws RemoteException;

    WifiStatus removeApIface(String str) throws RemoteException;

    WifiStatus removeNanIface(String str) throws RemoteException;

    WifiStatus removeP2pIface(String str) throws RemoteException;

    WifiStatus removeStaIface(String str) throws RemoteException;

    void requestChipDebugInfo(requestChipDebugInfoCallback requestchipdebuginfocallback) throws RemoteException;

    void requestDriverDebugDump(requestDriverDebugDumpCallback requestdriverdebugdumpcallback) throws RemoteException;

    void requestFirmwareDebugDump(requestFirmwareDebugDumpCallback requestfirmwaredebugdumpcallback) throws RemoteException;

    @Override
    void setHALInstrumentation() throws RemoteException;

    WifiStatus startLoggingToDebugRingBuffer(String str, int i, int i2, int i3) throws RemoteException;

    WifiStatus stopLoggingToDebugRingBuffer() throws RemoteException;

    @Override
    boolean unlinkToDeath(IHwBinder.DeathRecipient deathRecipient) throws RemoteException;

    static IWifiChip asInterface(IHwBinder iHwBinder) {
        if (iHwBinder == null) {
            return null;
        }
        IHwInterface iHwInterfaceQueryLocalInterface = iHwBinder.queryLocalInterface(kInterfaceName);
        if (iHwInterfaceQueryLocalInterface != null && (iHwInterfaceQueryLocalInterface instanceof IWifiChip)) {
            return (IWifiChip) iHwInterfaceQueryLocalInterface;
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

    static IWifiChip castFrom(IHwInterface iHwInterface) {
        if (iHwInterface == null) {
            return null;
        }
        return asInterface(iHwInterface.asBinder());
    }

    static IWifiChip getService(String str, boolean z) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, str, z));
    }

    static IWifiChip getService(boolean z) throws RemoteException {
        return getService(HalDeviceManager.HAL_INSTANCE_NAME, z);
    }

    static IWifiChip getService(String str) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, str));
    }

    static IWifiChip getService() throws RemoteException {
        return getService(HalDeviceManager.HAL_INSTANCE_NAME);
    }

    public static final class ChipIfaceCombinationLimit {
        public int maxIfaces;
        public final ArrayList<Integer> types = new ArrayList<>();

        public final boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || obj.getClass() != ChipIfaceCombinationLimit.class) {
                return false;
            }
            ChipIfaceCombinationLimit chipIfaceCombinationLimit = (ChipIfaceCombinationLimit) obj;
            if (HidlSupport.deepEquals(this.types, chipIfaceCombinationLimit.types) && this.maxIfaces == chipIfaceCombinationLimit.maxIfaces) {
                return true;
            }
            return false;
        }

        public final int hashCode() {
            return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(this.types)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.maxIfaces))));
        }

        public final String toString() {
            return "{.types = " + this.types + ", .maxIfaces = " + this.maxIfaces + "}";
        }

        public final void readFromParcel(HwParcel hwParcel) {
            readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(24L), 0L);
        }

        public static final ArrayList<ChipIfaceCombinationLimit> readVectorFromParcel(HwParcel hwParcel) {
            ArrayList<ChipIfaceCombinationLimit> arrayList = new ArrayList<>();
            HwBlob buffer = hwParcel.readBuffer(16L);
            int int32 = buffer.getInt32(8L);
            HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 24, buffer.handle(), 0L, true);
            arrayList.clear();
            for (int i = 0; i < int32; i++) {
                ChipIfaceCombinationLimit chipIfaceCombinationLimit = new ChipIfaceCombinationLimit();
                chipIfaceCombinationLimit.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 24);
                arrayList.add(chipIfaceCombinationLimit);
            }
            return arrayList;
        }

        public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
            long j2 = j + 0;
            int int32 = hwBlob.getInt32(8 + j2);
            HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 4, hwBlob.handle(), j2 + 0, true);
            this.types.clear();
            for (int i = 0; i < int32; i++) {
                this.types.add(Integer.valueOf(embeddedBuffer.getInt32(i * 4)));
            }
            this.maxIfaces = hwBlob.getInt32(j + 16);
        }

        public final void writeToParcel(HwParcel hwParcel) {
            HwBlob hwBlob = new HwBlob(24);
            writeEmbeddedToBlob(hwBlob, 0L);
            hwParcel.writeBuffer(hwBlob);
        }

        public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<ChipIfaceCombinationLimit> arrayList) {
            HwBlob hwBlob = new HwBlob(16);
            int size = arrayList.size();
            hwBlob.putInt32(8L, size);
            hwBlob.putBool(12L, false);
            HwBlob hwBlob2 = new HwBlob(size * 24);
            for (int i = 0; i < size; i++) {
                arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 24);
            }
            hwBlob.putBlob(0L, hwBlob2);
            hwParcel.writeBuffer(hwBlob);
        }

        public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
            int size = this.types.size();
            long j2 = j + 0;
            hwBlob.putInt32(8 + j2, size);
            hwBlob.putBool(12 + j2, false);
            HwBlob hwBlob2 = new HwBlob(size * 4);
            for (int i = 0; i < size; i++) {
                hwBlob2.putInt32(i * 4, this.types.get(i).intValue());
            }
            hwBlob.putBlob(j2 + 0, hwBlob2);
            hwBlob.putInt32(j + 16, this.maxIfaces);
        }
    }

    public static final class ChipIfaceCombination {
        public final ArrayList<ChipIfaceCombinationLimit> limits = new ArrayList<>();

        public final boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj != null && obj.getClass() == ChipIfaceCombination.class && HidlSupport.deepEquals(this.limits, ((ChipIfaceCombination) obj).limits)) {
                return true;
            }
            return false;
        }

        public final int hashCode() {
            return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(this.limits)));
        }

        public final String toString() {
            return "{.limits = " + this.limits + "}";
        }

        public final void readFromParcel(HwParcel hwParcel) {
            readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(16L), 0L);
        }

        public static final ArrayList<ChipIfaceCombination> readVectorFromParcel(HwParcel hwParcel) {
            ArrayList<ChipIfaceCombination> arrayList = new ArrayList<>();
            HwBlob buffer = hwParcel.readBuffer(16L);
            int int32 = buffer.getInt32(8L);
            HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 16, buffer.handle(), 0L, true);
            arrayList.clear();
            for (int i = 0; i < int32; i++) {
                ChipIfaceCombination chipIfaceCombination = new ChipIfaceCombination();
                chipIfaceCombination.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 16);
                arrayList.add(chipIfaceCombination);
            }
            return arrayList;
        }

        public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
            long j2 = j + 0;
            int int32 = hwBlob.getInt32(8 + j2);
            HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 24, hwBlob.handle(), j2 + 0, true);
            this.limits.clear();
            for (int i = 0; i < int32; i++) {
                ChipIfaceCombinationLimit chipIfaceCombinationLimit = new ChipIfaceCombinationLimit();
                chipIfaceCombinationLimit.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 24);
                this.limits.add(chipIfaceCombinationLimit);
            }
        }

        public final void writeToParcel(HwParcel hwParcel) {
            HwBlob hwBlob = new HwBlob(16);
            writeEmbeddedToBlob(hwBlob, 0L);
            hwParcel.writeBuffer(hwBlob);
        }

        public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<ChipIfaceCombination> arrayList) {
            HwBlob hwBlob = new HwBlob(16);
            int size = arrayList.size();
            hwBlob.putInt32(8L, size);
            hwBlob.putBool(12L, false);
            HwBlob hwBlob2 = new HwBlob(size * 16);
            for (int i = 0; i < size; i++) {
                arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 16);
            }
            hwBlob.putBlob(0L, hwBlob2);
            hwParcel.writeBuffer(hwBlob);
        }

        public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
            int size = this.limits.size();
            long j2 = j + 0;
            hwBlob.putInt32(8 + j2, size);
            hwBlob.putBool(12 + j2, false);
            HwBlob hwBlob2 = new HwBlob(size * 24);
            for (int i = 0; i < size; i++) {
                this.limits.get(i).writeEmbeddedToBlob(hwBlob2, i * 24);
            }
            hwBlob.putBlob(j2 + 0, hwBlob2);
        }
    }

    public static final class ChipMode {
        public final ArrayList<ChipIfaceCombination> availableCombinations = new ArrayList<>();
        public int id;

        public final boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || obj.getClass() != ChipMode.class) {
                return false;
            }
            ChipMode chipMode = (ChipMode) obj;
            if (this.id == chipMode.id && HidlSupport.deepEquals(this.availableCombinations, chipMode.availableCombinations)) {
                return true;
            }
            return false;
        }

        public final int hashCode() {
            return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.id))), Integer.valueOf(HidlSupport.deepHashCode(this.availableCombinations)));
        }

        public final String toString() {
            return "{.id = " + this.id + ", .availableCombinations = " + this.availableCombinations + "}";
        }

        public final void readFromParcel(HwParcel hwParcel) {
            readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(24L), 0L);
        }

        public static final ArrayList<ChipMode> readVectorFromParcel(HwParcel hwParcel) {
            ArrayList<ChipMode> arrayList = new ArrayList<>();
            HwBlob buffer = hwParcel.readBuffer(16L);
            int int32 = buffer.getInt32(8L);
            HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 24, buffer.handle(), 0L, true);
            arrayList.clear();
            for (int i = 0; i < int32; i++) {
                ChipMode chipMode = new ChipMode();
                chipMode.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 24);
                arrayList.add(chipMode);
            }
            return arrayList;
        }

        public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
            this.id = hwBlob.getInt32(j + 0);
            long j2 = j + 8;
            int int32 = hwBlob.getInt32(8 + j2);
            HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 16, hwBlob.handle(), j2 + 0, true);
            this.availableCombinations.clear();
            for (int i = 0; i < int32; i++) {
                ChipIfaceCombination chipIfaceCombination = new ChipIfaceCombination();
                chipIfaceCombination.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 16);
                this.availableCombinations.add(chipIfaceCombination);
            }
        }

        public final void writeToParcel(HwParcel hwParcel) {
            HwBlob hwBlob = new HwBlob(24);
            writeEmbeddedToBlob(hwBlob, 0L);
            hwParcel.writeBuffer(hwBlob);
        }

        public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<ChipMode> arrayList) {
            HwBlob hwBlob = new HwBlob(16);
            int size = arrayList.size();
            hwBlob.putInt32(8L, size);
            hwBlob.putBool(12L, false);
            HwBlob hwBlob2 = new HwBlob(size * 24);
            for (int i = 0; i < size; i++) {
                arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 24);
            }
            hwBlob.putBlob(0L, hwBlob2);
            hwParcel.writeBuffer(hwBlob);
        }

        public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
            hwBlob.putInt32(j + 0, this.id);
            int size = this.availableCombinations.size();
            long j2 = j + 8;
            hwBlob.putInt32(8 + j2, size);
            hwBlob.putBool(12 + j2, false);
            HwBlob hwBlob2 = new HwBlob(size * 16);
            for (int i = 0; i < size; i++) {
                this.availableCombinations.get(i).writeEmbeddedToBlob(hwBlob2, i * 16);
            }
            hwBlob.putBlob(j2 + 0, hwBlob2);
        }
    }

    public static final class ChipDebugInfo {
        public String driverDescription = new String();
        public String firmwareDescription = new String();

        public final boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || obj.getClass() != ChipDebugInfo.class) {
                return false;
            }
            ChipDebugInfo chipDebugInfo = (ChipDebugInfo) obj;
            if (HidlSupport.deepEquals(this.driverDescription, chipDebugInfo.driverDescription) && HidlSupport.deepEquals(this.firmwareDescription, chipDebugInfo.firmwareDescription)) {
                return true;
            }
            return false;
        }

        public final int hashCode() {
            return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(this.driverDescription)), Integer.valueOf(HidlSupport.deepHashCode(this.firmwareDescription)));
        }

        public final String toString() {
            return "{.driverDescription = " + this.driverDescription + ", .firmwareDescription = " + this.firmwareDescription + "}";
        }

        public final void readFromParcel(HwParcel hwParcel) {
            readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(32L), 0L);
        }

        public static final ArrayList<ChipDebugInfo> readVectorFromParcel(HwParcel hwParcel) {
            ArrayList<ChipDebugInfo> arrayList = new ArrayList<>();
            HwBlob buffer = hwParcel.readBuffer(16L);
            int int32 = buffer.getInt32(8L);
            HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 32, buffer.handle(), 0L, true);
            arrayList.clear();
            for (int i = 0; i < int32; i++) {
                ChipDebugInfo chipDebugInfo = new ChipDebugInfo();
                chipDebugInfo.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 32);
                arrayList.add(chipDebugInfo);
            }
            return arrayList;
        }

        public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
            long j2 = j + 0;
            this.driverDescription = hwBlob.getString(j2);
            hwParcel.readEmbeddedBuffer(this.driverDescription.getBytes().length + 1, hwBlob.handle(), j2 + 0, false);
            long j3 = j + 16;
            this.firmwareDescription = hwBlob.getString(j3);
            hwParcel.readEmbeddedBuffer(this.firmwareDescription.getBytes().length + 1, hwBlob.handle(), j3 + 0, false);
        }

        public final void writeToParcel(HwParcel hwParcel) {
            HwBlob hwBlob = new HwBlob(32);
            writeEmbeddedToBlob(hwBlob, 0L);
            hwParcel.writeBuffer(hwBlob);
        }

        public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<ChipDebugInfo> arrayList) {
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
            hwBlob.putString(0 + j, this.driverDescription);
            hwBlob.putString(j + 16, this.firmwareDescription);
        }
    }

    public static final class ChipCapabilityMask {
        public static final int DEBUG_ERROR_ALERTS = 128;
        public static final int DEBUG_HOST_WAKE_REASON_STATS = 64;
        public static final int DEBUG_MEMORY_DRIVER_DUMP = 2;
        public static final int DEBUG_MEMORY_FIRMWARE_DUMP = 1;
        public static final int DEBUG_RING_BUFFER_CONNECT_EVENT = 4;
        public static final int DEBUG_RING_BUFFER_POWER_EVENT = 8;
        public static final int DEBUG_RING_BUFFER_VENDOR_DATA = 32;
        public static final int DEBUG_RING_BUFFER_WAKELOCK_EVENT = 16;

        public static final String toString(int i) {
            if (i == 1) {
                return "DEBUG_MEMORY_FIRMWARE_DUMP";
            }
            if (i == 2) {
                return "DEBUG_MEMORY_DRIVER_DUMP";
            }
            if (i == 4) {
                return "DEBUG_RING_BUFFER_CONNECT_EVENT";
            }
            if (i == 8) {
                return "DEBUG_RING_BUFFER_POWER_EVENT";
            }
            if (i == 16) {
                return "DEBUG_RING_BUFFER_WAKELOCK_EVENT";
            }
            if (i == 32) {
                return "DEBUG_RING_BUFFER_VENDOR_DATA";
            }
            if (i == 64) {
                return "DEBUG_HOST_WAKE_REASON_STATS";
            }
            if (i == 128) {
                return "DEBUG_ERROR_ALERTS";
            }
            return "0x" + Integer.toHexString(i);
        }

        public static final String dumpBitfield(int i) {
            ArrayList arrayList = new ArrayList();
            int i2 = 1;
            if ((i & 1) == 1) {
                arrayList.add("DEBUG_MEMORY_FIRMWARE_DUMP");
            } else {
                i2 = 0;
            }
            if ((i & 2) == 2) {
                arrayList.add("DEBUG_MEMORY_DRIVER_DUMP");
                i2 |= 2;
            }
            if ((i & 4) == 4) {
                arrayList.add("DEBUG_RING_BUFFER_CONNECT_EVENT");
                i2 |= 4;
            }
            if ((i & 8) == 8) {
                arrayList.add("DEBUG_RING_BUFFER_POWER_EVENT");
                i2 |= 8;
            }
            if ((i & 16) == 16) {
                arrayList.add("DEBUG_RING_BUFFER_WAKELOCK_EVENT");
                i2 |= 16;
            }
            if ((i & 32) == 32) {
                arrayList.add("DEBUG_RING_BUFFER_VENDOR_DATA");
                i2 |= 32;
            }
            if ((i & 64) == 64) {
                arrayList.add("DEBUG_HOST_WAKE_REASON_STATS");
                i2 |= 64;
            }
            if ((i & 128) == 128) {
                arrayList.add("DEBUG_ERROR_ALERTS");
                i2 |= 128;
            }
            if (i != i2) {
                arrayList.add("0x" + Integer.toHexString(i & (~i2)));
            }
            return String.join(" | ", arrayList);
        }
    }

    public static final class Proxy implements IWifiChip {
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
                return "[class or subclass of android.hardware.wifi@1.0::IWifiChip]@Proxy";
            }
        }

        public final boolean equals(Object obj) {
            return HidlSupport.interfacesEqual(this, obj);
        }

        public final int hashCode() {
            return asBinder().hashCode();
        }

        @Override
        public void getId(getIdCallback getidcallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiChip.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(1, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                WifiStatus wifiStatus = new WifiStatus();
                wifiStatus.readFromParcel(hwParcel2);
                getidcallback.onValues(wifiStatus, hwParcel2.readInt32());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public WifiStatus registerEventCallback(IWifiChipEventCallback iWifiChipEventCallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiChip.kInterfaceName);
            hwParcel.writeStrongBinder(iWifiChipEventCallback == null ? null : iWifiChipEventCallback.asBinder());
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(2, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                WifiStatus wifiStatus = new WifiStatus();
                wifiStatus.readFromParcel(hwParcel2);
                return wifiStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getCapabilities(getCapabilitiesCallback getcapabilitiescallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiChip.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(3, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                WifiStatus wifiStatus = new WifiStatus();
                wifiStatus.readFromParcel(hwParcel2);
                getcapabilitiescallback.onValues(wifiStatus, hwParcel2.readInt32());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getAvailableModes(getAvailableModesCallback getavailablemodescallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiChip.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(4, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                WifiStatus wifiStatus = new WifiStatus();
                wifiStatus.readFromParcel(hwParcel2);
                getavailablemodescallback.onValues(wifiStatus, ChipMode.readVectorFromParcel(hwParcel2));
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public WifiStatus configureChip(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiChip.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(5, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                WifiStatus wifiStatus = new WifiStatus();
                wifiStatus.readFromParcel(hwParcel2);
                return wifiStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getMode(getModeCallback getmodecallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiChip.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(6, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                WifiStatus wifiStatus = new WifiStatus();
                wifiStatus.readFromParcel(hwParcel2);
                getmodecallback.onValues(wifiStatus, hwParcel2.readInt32());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void requestChipDebugInfo(requestChipDebugInfoCallback requestchipdebuginfocallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiChip.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(7, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                WifiStatus wifiStatus = new WifiStatus();
                wifiStatus.readFromParcel(hwParcel2);
                ChipDebugInfo chipDebugInfo = new ChipDebugInfo();
                chipDebugInfo.readFromParcel(hwParcel2);
                requestchipdebuginfocallback.onValues(wifiStatus, chipDebugInfo);
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void requestDriverDebugDump(requestDriverDebugDumpCallback requestdriverdebugdumpcallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiChip.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(8, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                WifiStatus wifiStatus = new WifiStatus();
                wifiStatus.readFromParcel(hwParcel2);
                requestdriverdebugdumpcallback.onValues(wifiStatus, hwParcel2.readInt8Vector());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void requestFirmwareDebugDump(requestFirmwareDebugDumpCallback requestfirmwaredebugdumpcallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiChip.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(9, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                WifiStatus wifiStatus = new WifiStatus();
                wifiStatus.readFromParcel(hwParcel2);
                requestfirmwaredebugdumpcallback.onValues(wifiStatus, hwParcel2.readInt8Vector());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void createApIface(createApIfaceCallback createapifacecallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiChip.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(10, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                WifiStatus wifiStatus = new WifiStatus();
                wifiStatus.readFromParcel(hwParcel2);
                createapifacecallback.onValues(wifiStatus, IWifiApIface.asInterface(hwParcel2.readStrongBinder()));
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getApIfaceNames(getApIfaceNamesCallback getapifacenamescallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiChip.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(11, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                WifiStatus wifiStatus = new WifiStatus();
                wifiStatus.readFromParcel(hwParcel2);
                getapifacenamescallback.onValues(wifiStatus, hwParcel2.readStringVector());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getApIface(String str, getApIfaceCallback getapifacecallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiChip.kInterfaceName);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(12, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                WifiStatus wifiStatus = new WifiStatus();
                wifiStatus.readFromParcel(hwParcel2);
                getapifacecallback.onValues(wifiStatus, IWifiApIface.asInterface(hwParcel2.readStrongBinder()));
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public WifiStatus removeApIface(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiChip.kInterfaceName);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(13, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                WifiStatus wifiStatus = new WifiStatus();
                wifiStatus.readFromParcel(hwParcel2);
                return wifiStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void createNanIface(createNanIfaceCallback createnanifacecallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiChip.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(14, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                WifiStatus wifiStatus = new WifiStatus();
                wifiStatus.readFromParcel(hwParcel2);
                createnanifacecallback.onValues(wifiStatus, IWifiNanIface.asInterface(hwParcel2.readStrongBinder()));
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getNanIfaceNames(getNanIfaceNamesCallback getnanifacenamescallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiChip.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(15, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                WifiStatus wifiStatus = new WifiStatus();
                wifiStatus.readFromParcel(hwParcel2);
                getnanifacenamescallback.onValues(wifiStatus, hwParcel2.readStringVector());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getNanIface(String str, getNanIfaceCallback getnanifacecallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiChip.kInterfaceName);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(16, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                WifiStatus wifiStatus = new WifiStatus();
                wifiStatus.readFromParcel(hwParcel2);
                getnanifacecallback.onValues(wifiStatus, IWifiNanIface.asInterface(hwParcel2.readStrongBinder()));
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public WifiStatus removeNanIface(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiChip.kInterfaceName);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(17, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                WifiStatus wifiStatus = new WifiStatus();
                wifiStatus.readFromParcel(hwParcel2);
                return wifiStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void createP2pIface(createP2pIfaceCallback createp2pifacecallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiChip.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(18, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                WifiStatus wifiStatus = new WifiStatus();
                wifiStatus.readFromParcel(hwParcel2);
                createp2pifacecallback.onValues(wifiStatus, IWifiP2pIface.asInterface(hwParcel2.readStrongBinder()));
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getP2pIfaceNames(getP2pIfaceNamesCallback getp2pifacenamescallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiChip.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(19, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                WifiStatus wifiStatus = new WifiStatus();
                wifiStatus.readFromParcel(hwParcel2);
                getp2pifacenamescallback.onValues(wifiStatus, hwParcel2.readStringVector());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getP2pIface(String str, getP2pIfaceCallback getp2pifacecallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiChip.kInterfaceName);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(20, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                WifiStatus wifiStatus = new WifiStatus();
                wifiStatus.readFromParcel(hwParcel2);
                getp2pifacecallback.onValues(wifiStatus, IWifiP2pIface.asInterface(hwParcel2.readStrongBinder()));
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public WifiStatus removeP2pIface(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiChip.kInterfaceName);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(21, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                WifiStatus wifiStatus = new WifiStatus();
                wifiStatus.readFromParcel(hwParcel2);
                return wifiStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void createStaIface(createStaIfaceCallback createstaifacecallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiChip.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(22, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                WifiStatus wifiStatus = new WifiStatus();
                wifiStatus.readFromParcel(hwParcel2);
                createstaifacecallback.onValues(wifiStatus, IWifiStaIface.asInterface(hwParcel2.readStrongBinder()));
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getStaIfaceNames(getStaIfaceNamesCallback getstaifacenamescallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiChip.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(23, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                WifiStatus wifiStatus = new WifiStatus();
                wifiStatus.readFromParcel(hwParcel2);
                getstaifacenamescallback.onValues(wifiStatus, hwParcel2.readStringVector());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getStaIface(String str, getStaIfaceCallback getstaifacecallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiChip.kInterfaceName);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(24, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                WifiStatus wifiStatus = new WifiStatus();
                wifiStatus.readFromParcel(hwParcel2);
                getstaifacecallback.onValues(wifiStatus, IWifiStaIface.asInterface(hwParcel2.readStrongBinder()));
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public WifiStatus removeStaIface(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiChip.kInterfaceName);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(25, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                WifiStatus wifiStatus = new WifiStatus();
                wifiStatus.readFromParcel(hwParcel2);
                return wifiStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void createRttController(IWifiIface iWifiIface, createRttControllerCallback createrttcontrollercallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiChip.kInterfaceName);
            hwParcel.writeStrongBinder(iWifiIface == null ? null : iWifiIface.asBinder());
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(26, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                WifiStatus wifiStatus = new WifiStatus();
                wifiStatus.readFromParcel(hwParcel2);
                createrttcontrollercallback.onValues(wifiStatus, IWifiRttController.asInterface(hwParcel2.readStrongBinder()));
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getDebugRingBuffersStatus(getDebugRingBuffersStatusCallback getdebugringbuffersstatuscallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiChip.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(27, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                WifiStatus wifiStatus = new WifiStatus();
                wifiStatus.readFromParcel(hwParcel2);
                getdebugringbuffersstatuscallback.onValues(wifiStatus, WifiDebugRingBufferStatus.readVectorFromParcel(hwParcel2));
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public WifiStatus startLoggingToDebugRingBuffer(String str, int i, int i2, int i3) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiChip.kInterfaceName);
            hwParcel.writeString(str);
            hwParcel.writeInt32(i);
            hwParcel.writeInt32(i2);
            hwParcel.writeInt32(i3);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(28, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                WifiStatus wifiStatus = new WifiStatus();
                wifiStatus.readFromParcel(hwParcel2);
                return wifiStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public WifiStatus forceDumpToDebugRingBuffer(String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiChip.kInterfaceName);
            hwParcel.writeString(str);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(29, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                WifiStatus wifiStatus = new WifiStatus();
                wifiStatus.readFromParcel(hwParcel2);
                return wifiStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public WifiStatus stopLoggingToDebugRingBuffer() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiChip.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(30, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                WifiStatus wifiStatus = new WifiStatus();
                wifiStatus.readFromParcel(hwParcel2);
                return wifiStatus;
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getDebugHostWakeReasonStats(getDebugHostWakeReasonStatsCallback getdebughostwakereasonstatscallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiChip.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(31, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                WifiStatus wifiStatus = new WifiStatus();
                wifiStatus.readFromParcel(hwParcel2);
                WifiDebugHostWakeReasonStats wifiDebugHostWakeReasonStats = new WifiDebugHostWakeReasonStats();
                wifiDebugHostWakeReasonStats.readFromParcel(hwParcel2);
                getdebughostwakereasonstatscallback.onValues(wifiStatus, wifiDebugHostWakeReasonStats);
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public WifiStatus enableDebugErrorAlerts(boolean z) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiChip.kInterfaceName);
            hwParcel.writeBool(z);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(32, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                WifiStatus wifiStatus = new WifiStatus();
                wifiStatus.readFromParcel(hwParcel2);
                return wifiStatus;
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

    public static abstract class Stub extends HwBinder implements IWifiChip {
        @Override
        public IHwBinder asBinder() {
            return this;
        }

        @Override
        public final ArrayList<String> interfaceChain() {
            return new ArrayList<>(Arrays.asList(IWifiChip.kInterfaceName, IBase.kInterfaceName));
        }

        @Override
        public final String interfaceDescriptor() {
            return IWifiChip.kInterfaceName;
        }

        @Override
        public final ArrayList<byte[]> getHashChain() {
            return new ArrayList<>(Arrays.asList(new byte[]{-13, -18, -52, 72, -99, -21, 76, 116, -119, 47, 89, -21, 122, -37, 118, -112, 99, -67, 92, 53, 74, -63, 50, -74, 38, -91, -12, 43, 54, 61, 54, -68}, new byte[]{-67, -38, -74, 24, 77, 122, 52, 109, -90, -96, 125, -64, -126, -116, -15, -102, 105, 111, 76, -86, 54, 17, -59, 31, 46, 20, 86, 90, 20, -76, 15, -39}));
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
            if (IWifiChip.kInterfaceName.equals(str)) {
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
                        hwParcel.enforceInterface(IWifiChip.kInterfaceName);
                        getId(new getIdCallback() {
                            @Override
                            public void onValues(WifiStatus wifiStatus, int i3) {
                                hwParcel2.writeStatus(0);
                                wifiStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeInt32(i3);
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
                        hwParcel.enforceInterface(IWifiChip.kInterfaceName);
                        WifiStatus wifiStatusRegisterEventCallback = registerEventCallback(IWifiChipEventCallback.asInterface(hwParcel.readStrongBinder()));
                        hwParcel2.writeStatus(0);
                        wifiStatusRegisterEventCallback.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 3:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiChip.kInterfaceName);
                        getCapabilities(new getCapabilitiesCallback() {
                            @Override
                            public void onValues(WifiStatus wifiStatus, int i3) {
                                hwParcel2.writeStatus(0);
                                wifiStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeInt32(i3);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 4:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiChip.kInterfaceName);
                        getAvailableModes(new getAvailableModesCallback() {
                            @Override
                            public void onValues(WifiStatus wifiStatus, ArrayList<ChipMode> arrayList) {
                                hwParcel2.writeStatus(0);
                                wifiStatus.writeToParcel(hwParcel2);
                                ChipMode.writeVectorToParcel(hwParcel2, arrayList);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 5:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiChip.kInterfaceName);
                        WifiStatus wifiStatusConfigureChip = configureChip(hwParcel.readInt32());
                        hwParcel2.writeStatus(0);
                        wifiStatusConfigureChip.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 6:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiChip.kInterfaceName);
                        getMode(new getModeCallback() {
                            @Override
                            public void onValues(WifiStatus wifiStatus, int i3) {
                                hwParcel2.writeStatus(0);
                                wifiStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeInt32(i3);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 7:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiChip.kInterfaceName);
                        requestChipDebugInfo(new requestChipDebugInfoCallback() {
                            @Override
                            public void onValues(WifiStatus wifiStatus, ChipDebugInfo chipDebugInfo) {
                                hwParcel2.writeStatus(0);
                                wifiStatus.writeToParcel(hwParcel2);
                                chipDebugInfo.writeToParcel(hwParcel2);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 8:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiChip.kInterfaceName);
                        requestDriverDebugDump(new requestDriverDebugDumpCallback() {
                            @Override
                            public void onValues(WifiStatus wifiStatus, ArrayList<Byte> arrayList) {
                                hwParcel2.writeStatus(0);
                                wifiStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeInt8Vector(arrayList);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 9:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiChip.kInterfaceName);
                        requestFirmwareDebugDump(new requestFirmwareDebugDumpCallback() {
                            @Override
                            public void onValues(WifiStatus wifiStatus, ArrayList<Byte> arrayList) {
                                hwParcel2.writeStatus(0);
                                wifiStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeInt8Vector(arrayList);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 10:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiChip.kInterfaceName);
                        createApIface(new createApIfaceCallback() {
                            @Override
                            public void onValues(WifiStatus wifiStatus, IWifiApIface iWifiApIface) {
                                hwParcel2.writeStatus(0);
                                wifiStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeStrongBinder(iWifiApIface == null ? null : iWifiApIface.asBinder());
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 11:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiChip.kInterfaceName);
                        getApIfaceNames(new getApIfaceNamesCallback() {
                            @Override
                            public void onValues(WifiStatus wifiStatus, ArrayList<String> arrayList) {
                                hwParcel2.writeStatus(0);
                                wifiStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeStringVector(arrayList);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 12:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiChip.kInterfaceName);
                        getApIface(hwParcel.readString(), new getApIfaceCallback() {
                            @Override
                            public void onValues(WifiStatus wifiStatus, IWifiApIface iWifiApIface) {
                                hwParcel2.writeStatus(0);
                                wifiStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeStrongBinder(iWifiApIface == null ? null : iWifiApIface.asBinder());
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 13:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiChip.kInterfaceName);
                        WifiStatus wifiStatusRemoveApIface = removeApIface(hwParcel.readString());
                        hwParcel2.writeStatus(0);
                        wifiStatusRemoveApIface.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 14:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiChip.kInterfaceName);
                        createNanIface(new createNanIfaceCallback() {
                            @Override
                            public void onValues(WifiStatus wifiStatus, IWifiNanIface iWifiNanIface) {
                                hwParcel2.writeStatus(0);
                                wifiStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeStrongBinder(iWifiNanIface == null ? null : iWifiNanIface.asBinder());
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 15:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiChip.kInterfaceName);
                        getNanIfaceNames(new getNanIfaceNamesCallback() {
                            @Override
                            public void onValues(WifiStatus wifiStatus, ArrayList<String> arrayList) {
                                hwParcel2.writeStatus(0);
                                wifiStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeStringVector(arrayList);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 16:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiChip.kInterfaceName);
                        getNanIface(hwParcel.readString(), new getNanIfaceCallback() {
                            @Override
                            public void onValues(WifiStatus wifiStatus, IWifiNanIface iWifiNanIface) {
                                hwParcel2.writeStatus(0);
                                wifiStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeStrongBinder(iWifiNanIface == null ? null : iWifiNanIface.asBinder());
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 17:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiChip.kInterfaceName);
                        WifiStatus wifiStatusRemoveNanIface = removeNanIface(hwParcel.readString());
                        hwParcel2.writeStatus(0);
                        wifiStatusRemoveNanIface.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 18:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiChip.kInterfaceName);
                        createP2pIface(new createP2pIfaceCallback() {
                            @Override
                            public void onValues(WifiStatus wifiStatus, IWifiP2pIface iWifiP2pIface) {
                                hwParcel2.writeStatus(0);
                                wifiStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeStrongBinder(iWifiP2pIface == null ? null : iWifiP2pIface.asBinder());
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 19:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiChip.kInterfaceName);
                        getP2pIfaceNames(new getP2pIfaceNamesCallback() {
                            @Override
                            public void onValues(WifiStatus wifiStatus, ArrayList<String> arrayList) {
                                hwParcel2.writeStatus(0);
                                wifiStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeStringVector(arrayList);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 20:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiChip.kInterfaceName);
                        getP2pIface(hwParcel.readString(), new getP2pIfaceCallback() {
                            @Override
                            public void onValues(WifiStatus wifiStatus, IWifiP2pIface iWifiP2pIface) {
                                hwParcel2.writeStatus(0);
                                wifiStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeStrongBinder(iWifiP2pIface == null ? null : iWifiP2pIface.asBinder());
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case ISupplicantStaIfaceCallback.ReasonCode.UNSUPPORTED_RSN_IE_VERSION:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiChip.kInterfaceName);
                        WifiStatus wifiStatusRemoveP2pIface = removeP2pIface(hwParcel.readString());
                        hwParcel2.writeStatus(0);
                        wifiStatusRemoveP2pIface.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 22:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiChip.kInterfaceName);
                        createStaIface(new createStaIfaceCallback() {
                            @Override
                            public void onValues(WifiStatus wifiStatus, IWifiStaIface iWifiStaIface) {
                                hwParcel2.writeStatus(0);
                                wifiStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeStrongBinder(iWifiStaIface == null ? null : iWifiStaIface.asBinder());
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 23:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiChip.kInterfaceName);
                        getStaIfaceNames(new getStaIfaceNamesCallback() {
                            @Override
                            public void onValues(WifiStatus wifiStatus, ArrayList<String> arrayList) {
                                hwParcel2.writeStatus(0);
                                wifiStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeStringVector(arrayList);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 24:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiChip.kInterfaceName);
                        getStaIface(hwParcel.readString(), new getStaIfaceCallback() {
                            @Override
                            public void onValues(WifiStatus wifiStatus, IWifiStaIface iWifiStaIface) {
                                hwParcel2.writeStatus(0);
                                wifiStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeStrongBinder(iWifiStaIface == null ? null : iWifiStaIface.asBinder());
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
                        hwParcel.enforceInterface(IWifiChip.kInterfaceName);
                        WifiStatus wifiStatusRemoveStaIface = removeStaIface(hwParcel.readString());
                        hwParcel2.writeStatus(0);
                        wifiStatusRemoveStaIface.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case ISupplicantStaIfaceCallback.ReasonCode.TDLS_TEARDOWN_UNSPECIFIED:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiChip.kInterfaceName);
                        createRttController(IWifiIface.asInterface(hwParcel.readStrongBinder()), new createRttControllerCallback() {
                            @Override
                            public void onValues(WifiStatus wifiStatus, IWifiRttController iWifiRttController) {
                                hwParcel2.writeStatus(0);
                                wifiStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeStrongBinder(iWifiRttController == null ? null : iWifiRttController.asBinder());
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 27:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiChip.kInterfaceName);
                        getDebugRingBuffersStatus(new getDebugRingBuffersStatusCallback() {
                            @Override
                            public void onValues(WifiStatus wifiStatus, ArrayList<WifiDebugRingBufferStatus> arrayList) {
                                hwParcel2.writeStatus(0);
                                wifiStatus.writeToParcel(hwParcel2);
                                WifiDebugRingBufferStatus.writeVectorToParcel(hwParcel2, arrayList);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 28:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiChip.kInterfaceName);
                        WifiStatus wifiStatusStartLoggingToDebugRingBuffer = startLoggingToDebugRingBuffer(hwParcel.readString(), hwParcel.readInt32(), hwParcel.readInt32(), hwParcel.readInt32());
                        hwParcel2.writeStatus(0);
                        wifiStatusStartLoggingToDebugRingBuffer.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 29:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiChip.kInterfaceName);
                        WifiStatus wifiStatusForceDumpToDebugRingBuffer = forceDumpToDebugRingBuffer(hwParcel.readString());
                        hwParcel2.writeStatus(0);
                        wifiStatusForceDumpToDebugRingBuffer.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 30:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiChip.kInterfaceName);
                        WifiStatus wifiStatusStopLoggingToDebugRingBuffer = stopLoggingToDebugRingBuffer();
                        hwParcel2.writeStatus(0);
                        wifiStatusStopLoggingToDebugRingBuffer.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 31:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiChip.kInterfaceName);
                        getDebugHostWakeReasonStats(new getDebugHostWakeReasonStatsCallback() {
                            @Override
                            public void onValues(WifiStatus wifiStatus, WifiDebugHostWakeReasonStats wifiDebugHostWakeReasonStats) {
                                hwParcel2.writeStatus(0);
                                wifiStatus.writeToParcel(hwParcel2);
                                wifiDebugHostWakeReasonStats.writeToParcel(hwParcel2);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 32:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiChip.kInterfaceName);
                        WifiStatus wifiStatusEnableDebugErrorAlerts = enableDebugErrorAlerts(hwParcel.readBool());
                        hwParcel2.writeStatus(0);
                        wifiStatusEnableDebugErrorAlerts.writeToParcel(hwParcel2);
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
