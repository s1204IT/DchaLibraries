package android.hardware.wifi.V1_1;

import android.hardware.wifi.V1_0.IWifiApIface;
import android.hardware.wifi.V1_0.IWifiChip;
import android.hardware.wifi.V1_0.IWifiChipEventCallback;
import android.hardware.wifi.V1_0.IWifiIface;
import android.hardware.wifi.V1_0.IWifiNanIface;
import android.hardware.wifi.V1_0.IWifiP2pIface;
import android.hardware.wifi.V1_0.IWifiRttController;
import android.hardware.wifi.V1_0.IWifiStaIface;
import android.hardware.wifi.V1_0.WifiDebugHostWakeReasonStats;
import android.hardware.wifi.V1_0.WifiDebugRingBufferStatus;
import android.hardware.wifi.V1_0.WifiStatus;
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

public interface IWifiChip extends android.hardware.wifi.V1_0.IWifiChip {
    public static final String kInterfaceName = "android.hardware.wifi@1.1::IWifiChip";

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

    WifiStatus resetTxPowerScenario() throws RemoteException;

    WifiStatus selectTxPowerScenario(int i) throws RemoteException;

    @Override
    void setHALInstrumentation() throws RemoteException;

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

    public static final class ChipCapabilityMask {
        public static final int D2AP_RTT = 1024;
        public static final int D2D_RTT = 512;
        public static final int DEBUG_ERROR_ALERTS = 128;
        public static final int DEBUG_HOST_WAKE_REASON_STATS = 64;
        public static final int DEBUG_MEMORY_DRIVER_DUMP = 2;
        public static final int DEBUG_MEMORY_FIRMWARE_DUMP = 1;
        public static final int DEBUG_RING_BUFFER_CONNECT_EVENT = 4;
        public static final int DEBUG_RING_BUFFER_POWER_EVENT = 8;
        public static final int DEBUG_RING_BUFFER_VENDOR_DATA = 32;
        public static final int DEBUG_RING_BUFFER_WAKELOCK_EVENT = 16;
        public static final int SET_TX_POWER_LIMIT = 256;

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
            if (i == 256) {
                return "SET_TX_POWER_LIMIT";
            }
            if (i == 512) {
                return "D2D_RTT";
            }
            if (i == 1024) {
                return "D2AP_RTT";
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
            if ((i & 256) == 256) {
                arrayList.add("SET_TX_POWER_LIMIT");
                i2 |= 256;
            }
            if ((i & 512) == 512) {
                arrayList.add("D2D_RTT");
                i2 |= 512;
            }
            if ((i & 1024) == 1024) {
                arrayList.add("D2AP_RTT");
                i2 |= 1024;
            }
            if (i != i2) {
                arrayList.add("0x" + Integer.toHexString(i & (~i2)));
            }
            return String.join(" | ", arrayList);
        }
    }

    public static final class TxPowerScenario {
        public static final int VOICE_CALL = 0;

        public static final String toString(int i) {
            if (i == 0) {
                return "VOICE_CALL";
            }
            return "0x" + Integer.toHexString(i);
        }

        public static final String dumpBitfield(int i) {
            ArrayList arrayList = new ArrayList();
            arrayList.add("VOICE_CALL");
            if (i != 0) {
                arrayList.add("0x" + Integer.toHexString(i & (-1)));
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
                return "[class or subclass of android.hardware.wifi@1.1::IWifiChip]@Proxy";
            }
        }

        public final boolean equals(Object obj) {
            return HidlSupport.interfacesEqual(this, obj);
        }

        public final int hashCode() {
            return asBinder().hashCode();
        }

        @Override
        public void getId(IWifiChip.getIdCallback getidcallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
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
            hwParcel.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
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
        public void getCapabilities(IWifiChip.getCapabilitiesCallback getcapabilitiescallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
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
        public void getAvailableModes(IWifiChip.getAvailableModesCallback getavailablemodescallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(4, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                WifiStatus wifiStatus = new WifiStatus();
                wifiStatus.readFromParcel(hwParcel2);
                getavailablemodescallback.onValues(wifiStatus, IWifiChip.ChipMode.readVectorFromParcel(hwParcel2));
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public WifiStatus configureChip(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
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
        public void getMode(IWifiChip.getModeCallback getmodecallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
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
        public void requestChipDebugInfo(IWifiChip.requestChipDebugInfoCallback requestchipdebuginfocallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(7, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                WifiStatus wifiStatus = new WifiStatus();
                wifiStatus.readFromParcel(hwParcel2);
                IWifiChip.ChipDebugInfo chipDebugInfo = new IWifiChip.ChipDebugInfo();
                chipDebugInfo.readFromParcel(hwParcel2);
                requestchipdebuginfocallback.onValues(wifiStatus, chipDebugInfo);
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void requestDriverDebugDump(IWifiChip.requestDriverDebugDumpCallback requestdriverdebugdumpcallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
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
        public void requestFirmwareDebugDump(IWifiChip.requestFirmwareDebugDumpCallback requestfirmwaredebugdumpcallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
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
        public void createApIface(IWifiChip.createApIfaceCallback createapifacecallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
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
        public void getApIfaceNames(IWifiChip.getApIfaceNamesCallback getapifacenamescallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
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
        public void getApIface(String str, IWifiChip.getApIfaceCallback getapifacecallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
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
            hwParcel.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
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
        public void createNanIface(IWifiChip.createNanIfaceCallback createnanifacecallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
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
        public void getNanIfaceNames(IWifiChip.getNanIfaceNamesCallback getnanifacenamescallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
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
        public void getNanIface(String str, IWifiChip.getNanIfaceCallback getnanifacecallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
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
            hwParcel.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
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
        public void createP2pIface(IWifiChip.createP2pIfaceCallback createp2pifacecallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
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
        public void getP2pIfaceNames(IWifiChip.getP2pIfaceNamesCallback getp2pifacenamescallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
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
        public void getP2pIface(String str, IWifiChip.getP2pIfaceCallback getp2pifacecallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
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
            hwParcel.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
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
        public void createStaIface(IWifiChip.createStaIfaceCallback createstaifacecallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
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
        public void getStaIfaceNames(IWifiChip.getStaIfaceNamesCallback getstaifacenamescallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
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
        public void getStaIface(String str, IWifiChip.getStaIfaceCallback getstaifacecallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
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
            hwParcel.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
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
        public void createRttController(IWifiIface iWifiIface, IWifiChip.createRttControllerCallback createrttcontrollercallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
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
        public void getDebugRingBuffersStatus(IWifiChip.getDebugRingBuffersStatusCallback getdebugringbuffersstatuscallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
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
            hwParcel.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
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
            hwParcel.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
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
            hwParcel.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
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
        public void getDebugHostWakeReasonStats(IWifiChip.getDebugHostWakeReasonStatsCallback getdebughostwakereasonstatscallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
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
            hwParcel.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
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
        public WifiStatus selectTxPowerScenario(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiChip.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(33, hwParcel, hwParcel2, 0);
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
        public WifiStatus resetTxPowerScenario() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiChip.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(34, hwParcel, hwParcel2, 0);
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
            return new ArrayList<>(Arrays.asList(IWifiChip.kInterfaceName, android.hardware.wifi.V1_0.IWifiChip.kInterfaceName, IBase.kInterfaceName));
        }

        @Override
        public final String interfaceDescriptor() {
            return IWifiChip.kInterfaceName;
        }

        @Override
        public final ArrayList<byte[]> getHashChain() {
            return new ArrayList<>(Arrays.asList(new byte[]{-80, 86, -31, -34, -6, -76, 7, 21, -124, 33, 69, -124, 5, 125, 11, -57, 58, 97, 48, -127, -65, 17, 82, 89, 5, 73, 100, -99, 69, -126, -63, 60}, new byte[]{-13, -18, -52, 72, -99, -21, 76, 116, -119, 47, 89, -21, 122, -37, 118, -112, 99, -67, 92, 53, 74, -63, 50, -74, 38, -91, -12, 43, 54, 61, 54, -68}, new byte[]{-67, -38, -74, 24, 77, 122, 52, 109, -90, -96, 125, -64, -126, -116, -15, -102, 105, 111, 76, -86, 54, 17, -59, 31, 46, 20, 86, 90, 20, -76, 15, -39}));
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
                        hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
                        getId(new IWifiChip.getIdCallback() {
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
                        hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
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
                        hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
                        getCapabilities(new IWifiChip.getCapabilitiesCallback() {
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
                        hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
                        getAvailableModes(new IWifiChip.getAvailableModesCallback() {
                            @Override
                            public void onValues(WifiStatus wifiStatus, ArrayList<IWifiChip.ChipMode> arrayList) {
                                hwParcel2.writeStatus(0);
                                wifiStatus.writeToParcel(hwParcel2);
                                IWifiChip.ChipMode.writeVectorToParcel(hwParcel2, arrayList);
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
                        hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
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
                        hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
                        getMode(new IWifiChip.getModeCallback() {
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
                        hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
                        requestChipDebugInfo(new IWifiChip.requestChipDebugInfoCallback() {
                            @Override
                            public void onValues(WifiStatus wifiStatus, IWifiChip.ChipDebugInfo chipDebugInfo) {
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
                        hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
                        requestDriverDebugDump(new IWifiChip.requestDriverDebugDumpCallback() {
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
                        hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
                        requestFirmwareDebugDump(new IWifiChip.requestFirmwareDebugDumpCallback() {
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
                        hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
                        createApIface(new IWifiChip.createApIfaceCallback() {
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
                        hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
                        getApIfaceNames(new IWifiChip.getApIfaceNamesCallback() {
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
                        hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
                        getApIface(hwParcel.readString(), new IWifiChip.getApIfaceCallback() {
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
                        hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
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
                        hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
                        createNanIface(new IWifiChip.createNanIfaceCallback() {
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
                        hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
                        getNanIfaceNames(new IWifiChip.getNanIfaceNamesCallback() {
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
                        hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
                        getNanIface(hwParcel.readString(), new IWifiChip.getNanIfaceCallback() {
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
                        hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
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
                        hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
                        createP2pIface(new IWifiChip.createP2pIfaceCallback() {
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
                        hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
                        getP2pIfaceNames(new IWifiChip.getP2pIfaceNamesCallback() {
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
                        hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
                        getP2pIface(hwParcel.readString(), new IWifiChip.getP2pIfaceCallback() {
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
                        hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
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
                        hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
                        createStaIface(new IWifiChip.createStaIfaceCallback() {
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
                        hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
                        getStaIfaceNames(new IWifiChip.getStaIfaceNamesCallback() {
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
                        hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
                        getStaIface(hwParcel.readString(), new IWifiChip.getStaIfaceCallback() {
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
                        hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
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
                        hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
                        createRttController(IWifiIface.asInterface(hwParcel.readStrongBinder()), new IWifiChip.createRttControllerCallback() {
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
                        hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
                        getDebugRingBuffersStatus(new IWifiChip.getDebugRingBuffersStatusCallback() {
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
                        hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
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
                        hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
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
                        hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
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
                        hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
                        getDebugHostWakeReasonStats(new IWifiChip.getDebugHostWakeReasonStatsCallback() {
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
                        hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiChip.kInterfaceName);
                        WifiStatus wifiStatusEnableDebugErrorAlerts = enableDebugErrorAlerts(hwParcel.readBool());
                        hwParcel2.writeStatus(0);
                        wifiStatusEnableDebugErrorAlerts.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 33:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiChip.kInterfaceName);
                        WifiStatus wifiStatusSelectTxPowerScenario = selectTxPowerScenario(hwParcel.readInt32());
                        hwParcel2.writeStatus(0);
                        wifiStatusSelectTxPowerScenario.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 34:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiChip.kInterfaceName);
                        WifiStatus wifiStatusResetTxPowerScenario = resetTxPowerScenario();
                        hwParcel2.writeStatus(0);
                        wifiStatusResetTxPowerScenario.writeToParcel(hwParcel2);
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
