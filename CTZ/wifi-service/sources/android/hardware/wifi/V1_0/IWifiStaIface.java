package android.hardware.wifi.V1_0;

import android.hardware.wifi.V1_0.IWifiIface;
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
import com.android.server.wifi.WifiLoggerHal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;

public interface IWifiStaIface extends IWifiIface {
    public static final String kInterfaceName = "android.hardware.wifi@1.0::IWifiStaIface";

    @FunctionalInterface
    public interface getApfPacketFilterCapabilitiesCallback {
        void onValues(WifiStatus wifiStatus, StaApfPacketFilterCapabilities staApfPacketFilterCapabilities);
    }

    @FunctionalInterface
    public interface getBackgroundScanCapabilitiesCallback {
        void onValues(WifiStatus wifiStatus, StaBackgroundScanCapabilities staBackgroundScanCapabilities);
    }

    @FunctionalInterface
    public interface getCapabilitiesCallback {
        void onValues(WifiStatus wifiStatus, int i);
    }

    @FunctionalInterface
    public interface getDebugRxPacketFatesCallback {
        void onValues(WifiStatus wifiStatus, ArrayList<WifiDebugRxPacketFateReport> arrayList);
    }

    @FunctionalInterface
    public interface getDebugTxPacketFatesCallback {
        void onValues(WifiStatus wifiStatus, ArrayList<WifiDebugTxPacketFateReport> arrayList);
    }

    @FunctionalInterface
    public interface getLinkLayerStatsCallback {
        void onValues(WifiStatus wifiStatus, StaLinkLayerStats staLinkLayerStats);
    }

    @FunctionalInterface
    public interface getRoamingCapabilitiesCallback {
        void onValues(WifiStatus wifiStatus, StaRoamingCapabilities staRoamingCapabilities);
    }

    @FunctionalInterface
    public interface getValidFrequenciesForBandCallback {
        void onValues(WifiStatus wifiStatus, ArrayList<Integer> arrayList);
    }

    @Override
    IHwBinder asBinder();

    WifiStatus configureRoaming(StaRoamingConfig staRoamingConfig) throws RemoteException;

    WifiStatus disableLinkLayerStatsCollection() throws RemoteException;

    WifiStatus enableLinkLayerStatsCollection(boolean z) throws RemoteException;

    WifiStatus enableNdOffload(boolean z) throws RemoteException;

    void getApfPacketFilterCapabilities(getApfPacketFilterCapabilitiesCallback getapfpacketfiltercapabilitiescallback) throws RemoteException;

    void getBackgroundScanCapabilities(getBackgroundScanCapabilitiesCallback getbackgroundscancapabilitiescallback) throws RemoteException;

    void getCapabilities(getCapabilitiesCallback getcapabilitiescallback) throws RemoteException;

    @Override
    DebugInfo getDebugInfo() throws RemoteException;

    void getDebugRxPacketFates(getDebugRxPacketFatesCallback getdebugrxpacketfatescallback) throws RemoteException;

    void getDebugTxPacketFates(getDebugTxPacketFatesCallback getdebugtxpacketfatescallback) throws RemoteException;

    @Override
    ArrayList<byte[]> getHashChain() throws RemoteException;

    void getLinkLayerStats(getLinkLayerStatsCallback getlinklayerstatscallback) throws RemoteException;

    void getRoamingCapabilities(getRoamingCapabilitiesCallback getroamingcapabilitiescallback) throws RemoteException;

    void getValidFrequenciesForBand(int i, getValidFrequenciesForBandCallback getvalidfrequenciesforbandcallback) throws RemoteException;

    WifiStatus installApfPacketFilter(int i, ArrayList<Byte> arrayList) throws RemoteException;

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

    WifiStatus registerEventCallback(IWifiStaIfaceEventCallback iWifiStaIfaceEventCallback) throws RemoteException;

    @Override
    void setHALInstrumentation() throws RemoteException;

    WifiStatus setRoamingState(byte b) throws RemoteException;

    WifiStatus setScanningMacOui(byte[] bArr) throws RemoteException;

    WifiStatus startBackgroundScan(int i, StaBackgroundScanParameters staBackgroundScanParameters) throws RemoteException;

    WifiStatus startDebugPacketFateMonitoring() throws RemoteException;

    WifiStatus startRssiMonitoring(int i, int i2, int i3) throws RemoteException;

    WifiStatus startSendingKeepAlivePackets(int i, ArrayList<Byte> arrayList, short s, byte[] bArr, byte[] bArr2, int i2) throws RemoteException;

    WifiStatus stopBackgroundScan(int i) throws RemoteException;

    WifiStatus stopRssiMonitoring(int i) throws RemoteException;

    WifiStatus stopSendingKeepAlivePackets(int i) throws RemoteException;

    @Override
    boolean unlinkToDeath(IHwBinder.DeathRecipient deathRecipient) throws RemoteException;

    static IWifiStaIface asInterface(IHwBinder iHwBinder) {
        if (iHwBinder == null) {
            return null;
        }
        IHwInterface iHwInterfaceQueryLocalInterface = iHwBinder.queryLocalInterface(kInterfaceName);
        if (iHwInterfaceQueryLocalInterface != null && (iHwInterfaceQueryLocalInterface instanceof IWifiStaIface)) {
            return (IWifiStaIface) iHwInterfaceQueryLocalInterface;
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

    static IWifiStaIface castFrom(IHwInterface iHwInterface) {
        if (iHwInterface == null) {
            return null;
        }
        return asInterface(iHwInterface.asBinder());
    }

    static IWifiStaIface getService(String str, boolean z) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, str, z));
    }

    static IWifiStaIface getService(boolean z) throws RemoteException {
        return getService(HalDeviceManager.HAL_INSTANCE_NAME, z);
    }

    static IWifiStaIface getService(String str) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, str));
    }

    static IWifiStaIface getService() throws RemoteException {
        return getService(HalDeviceManager.HAL_INSTANCE_NAME);
    }

    public static final class StaIfaceCapabilityMask {
        public static final int APF = 1;
        public static final int BACKGROUND_SCAN = 2;
        public static final int CONTROL_ROAMING = 16;
        public static final int DEBUG_PACKET_FATE = 16384;
        public static final int HOTSPOT = 256;
        public static final int KEEP_ALIVE = 8192;
        public static final int LINK_LAYER_STATS = 4;
        public static final int ND_OFFLOAD = 4096;
        public static final int PNO = 512;
        public static final int PROBE_IE_WHITELIST = 32;
        public static final int RSSI_MONITOR = 8;
        public static final int SCAN_RAND = 64;
        public static final int STA_5G = 128;
        public static final int TDLS = 1024;
        public static final int TDLS_OFFCHANNEL = 2048;

        public static final String toString(int i) {
            if (i == 1) {
                return "APF";
            }
            if (i == 2) {
                return "BACKGROUND_SCAN";
            }
            if (i == 4) {
                return "LINK_LAYER_STATS";
            }
            if (i == 8) {
                return "RSSI_MONITOR";
            }
            if (i == 16) {
                return "CONTROL_ROAMING";
            }
            if (i == 32) {
                return "PROBE_IE_WHITELIST";
            }
            if (i == 64) {
                return "SCAN_RAND";
            }
            if (i == 128) {
                return "STA_5G";
            }
            if (i == 256) {
                return "HOTSPOT";
            }
            if (i == 512) {
                return "PNO";
            }
            if (i == 1024) {
                return "TDLS";
            }
            if (i == 2048) {
                return "TDLS_OFFCHANNEL";
            }
            if (i == 4096) {
                return "ND_OFFLOAD";
            }
            if (i == 8192) {
                return "KEEP_ALIVE";
            }
            if (i == 16384) {
                return "DEBUG_PACKET_FATE";
            }
            return "0x" + Integer.toHexString(i);
        }

        public static final String dumpBitfield(int i) {
            ArrayList arrayList = new ArrayList();
            int i2 = 1;
            if ((i & 1) == 1) {
                arrayList.add("APF");
            } else {
                i2 = 0;
            }
            if ((i & 2) == 2) {
                arrayList.add("BACKGROUND_SCAN");
                i2 |= 2;
            }
            if ((i & 4) == 4) {
                arrayList.add("LINK_LAYER_STATS");
                i2 |= 4;
            }
            if ((i & 8) == 8) {
                arrayList.add("RSSI_MONITOR");
                i2 |= 8;
            }
            if ((i & 16) == 16) {
                arrayList.add("CONTROL_ROAMING");
                i2 |= 16;
            }
            if ((i & 32) == 32) {
                arrayList.add("PROBE_IE_WHITELIST");
                i2 |= 32;
            }
            if ((i & 64) == 64) {
                arrayList.add("SCAN_RAND");
                i2 |= 64;
            }
            if ((i & 128) == 128) {
                arrayList.add("STA_5G");
                i2 |= 128;
            }
            if ((i & 256) == 256) {
                arrayList.add("HOTSPOT");
                i2 |= 256;
            }
            if ((i & 512) == 512) {
                arrayList.add("PNO");
                i2 |= 512;
            }
            if ((i & 1024) == 1024) {
                arrayList.add("TDLS");
                i2 |= 1024;
            }
            if ((i & 2048) == 2048) {
                arrayList.add("TDLS_OFFCHANNEL");
                i2 |= 2048;
            }
            if ((i & 4096) == 4096) {
                arrayList.add("ND_OFFLOAD");
                i2 |= 4096;
            }
            if ((i & 8192) == 8192) {
                arrayList.add("KEEP_ALIVE");
                i2 |= 8192;
            }
            if ((i & 16384) == 16384) {
                arrayList.add("DEBUG_PACKET_FATE");
                i2 |= 16384;
            }
            if (i != i2) {
                arrayList.add("0x" + Integer.toHexString(i & (~i2)));
            }
            return String.join(" | ", arrayList);
        }
    }

    public static final class Proxy implements IWifiStaIface {
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
                return "[class or subclass of android.hardware.wifi@1.0::IWifiStaIface]@Proxy";
            }
        }

        public final boolean equals(Object obj) {
            return HidlSupport.interfacesEqual(this, obj);
        }

        public final int hashCode() {
            return asBinder().hashCode();
        }

        @Override
        public void getType(IWifiIface.getTypeCallback gettypecallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiIface.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(1, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                WifiStatus wifiStatus = new WifiStatus();
                wifiStatus.readFromParcel(hwParcel2);
                gettypecallback.onValues(wifiStatus, hwParcel2.readInt32());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getName(IWifiIface.getNameCallback getnamecallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiIface.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(2, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                WifiStatus wifiStatus = new WifiStatus();
                wifiStatus.readFromParcel(hwParcel2);
                getnamecallback.onValues(wifiStatus, hwParcel2.readString());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public WifiStatus registerEventCallback(IWifiStaIfaceEventCallback iWifiStaIfaceEventCallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiStaIface.kInterfaceName);
            hwParcel.writeStrongBinder(iWifiStaIfaceEventCallback == null ? null : iWifiStaIfaceEventCallback.asBinder());
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(3, hwParcel, hwParcel2, 0);
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
            hwParcel.writeInterfaceToken(IWifiStaIface.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(4, hwParcel, hwParcel2, 0);
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
        public void getApfPacketFilterCapabilities(getApfPacketFilterCapabilitiesCallback getapfpacketfiltercapabilitiescallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiStaIface.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(5, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                WifiStatus wifiStatus = new WifiStatus();
                wifiStatus.readFromParcel(hwParcel2);
                StaApfPacketFilterCapabilities staApfPacketFilterCapabilities = new StaApfPacketFilterCapabilities();
                staApfPacketFilterCapabilities.readFromParcel(hwParcel2);
                getapfpacketfiltercapabilitiescallback.onValues(wifiStatus, staApfPacketFilterCapabilities);
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public WifiStatus installApfPacketFilter(int i, ArrayList<Byte> arrayList) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiStaIface.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeInt8Vector(arrayList);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(6, hwParcel, hwParcel2, 0);
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
        public void getBackgroundScanCapabilities(getBackgroundScanCapabilitiesCallback getbackgroundscancapabilitiescallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiStaIface.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(7, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                WifiStatus wifiStatus = new WifiStatus();
                wifiStatus.readFromParcel(hwParcel2);
                StaBackgroundScanCapabilities staBackgroundScanCapabilities = new StaBackgroundScanCapabilities();
                staBackgroundScanCapabilities.readFromParcel(hwParcel2);
                getbackgroundscancapabilitiescallback.onValues(wifiStatus, staBackgroundScanCapabilities);
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getValidFrequenciesForBand(int i, getValidFrequenciesForBandCallback getvalidfrequenciesforbandcallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiStaIface.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(8, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                WifiStatus wifiStatus = new WifiStatus();
                wifiStatus.readFromParcel(hwParcel2);
                getvalidfrequenciesforbandcallback.onValues(wifiStatus, hwParcel2.readInt32Vector());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public WifiStatus startBackgroundScan(int i, StaBackgroundScanParameters staBackgroundScanParameters) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiStaIface.kInterfaceName);
            hwParcel.writeInt32(i);
            staBackgroundScanParameters.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(9, hwParcel, hwParcel2, 0);
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
        public WifiStatus stopBackgroundScan(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiStaIface.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(10, hwParcel, hwParcel2, 0);
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
        public WifiStatus enableLinkLayerStatsCollection(boolean z) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiStaIface.kInterfaceName);
            hwParcel.writeBool(z);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(11, hwParcel, hwParcel2, 0);
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
        public WifiStatus disableLinkLayerStatsCollection() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiStaIface.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(12, hwParcel, hwParcel2, 0);
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
        public void getLinkLayerStats(getLinkLayerStatsCallback getlinklayerstatscallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiStaIface.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(13, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                WifiStatus wifiStatus = new WifiStatus();
                wifiStatus.readFromParcel(hwParcel2);
                StaLinkLayerStats staLinkLayerStats = new StaLinkLayerStats();
                staLinkLayerStats.readFromParcel(hwParcel2);
                getlinklayerstatscallback.onValues(wifiStatus, staLinkLayerStats);
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public WifiStatus startRssiMonitoring(int i, int i2, int i3) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiStaIface.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeInt32(i2);
            hwParcel.writeInt32(i3);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(14, hwParcel, hwParcel2, 0);
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
        public WifiStatus stopRssiMonitoring(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiStaIface.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(15, hwParcel, hwParcel2, 0);
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
        public void getRoamingCapabilities(getRoamingCapabilitiesCallback getroamingcapabilitiescallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiStaIface.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(16, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                WifiStatus wifiStatus = new WifiStatus();
                wifiStatus.readFromParcel(hwParcel2);
                StaRoamingCapabilities staRoamingCapabilities = new StaRoamingCapabilities();
                staRoamingCapabilities.readFromParcel(hwParcel2);
                getroamingcapabilitiescallback.onValues(wifiStatus, staRoamingCapabilities);
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public WifiStatus configureRoaming(StaRoamingConfig staRoamingConfig) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiStaIface.kInterfaceName);
            staRoamingConfig.writeToParcel(hwParcel);
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
        public WifiStatus setRoamingState(byte b) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiStaIface.kInterfaceName);
            hwParcel.writeInt8(b);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(18, hwParcel, hwParcel2, 0);
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
        public WifiStatus enableNdOffload(boolean z) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiStaIface.kInterfaceName);
            hwParcel.writeBool(z);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(19, hwParcel, hwParcel2, 0);
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
        public WifiStatus startSendingKeepAlivePackets(int i, ArrayList<Byte> arrayList, short s, byte[] bArr, byte[] bArr2, int i2) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiStaIface.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeInt8Vector(arrayList);
            hwParcel.writeInt16(s);
            HwBlob hwBlob = new HwBlob(6);
            hwBlob.putInt8Array(0L, bArr);
            hwParcel.writeBuffer(hwBlob);
            HwBlob hwBlob2 = new HwBlob(6);
            hwBlob2.putInt8Array(0L, bArr2);
            hwParcel.writeBuffer(hwBlob2);
            hwParcel.writeInt32(i2);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(20, hwParcel, hwParcel2, 0);
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
        public WifiStatus stopSendingKeepAlivePackets(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiStaIface.kInterfaceName);
            hwParcel.writeInt32(i);
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
        public WifiStatus setScanningMacOui(byte[] bArr) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiStaIface.kInterfaceName);
            HwBlob hwBlob = new HwBlob(3);
            hwBlob.putInt8Array(0L, bArr);
            hwParcel.writeBuffer(hwBlob);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(22, hwParcel, hwParcel2, 0);
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
        public WifiStatus startDebugPacketFateMonitoring() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiStaIface.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(23, hwParcel, hwParcel2, 0);
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
        public void getDebugTxPacketFates(getDebugTxPacketFatesCallback getdebugtxpacketfatescallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiStaIface.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(24, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                WifiStatus wifiStatus = new WifiStatus();
                wifiStatus.readFromParcel(hwParcel2);
                getdebugtxpacketfatescallback.onValues(wifiStatus, WifiDebugTxPacketFateReport.readVectorFromParcel(hwParcel2));
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getDebugRxPacketFates(getDebugRxPacketFatesCallback getdebugrxpacketfatescallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiStaIface.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(25, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                WifiStatus wifiStatus = new WifiStatus();
                wifiStatus.readFromParcel(hwParcel2);
                getdebugrxpacketfatescallback.onValues(wifiStatus, WifiDebugRxPacketFateReport.readVectorFromParcel(hwParcel2));
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

    public static abstract class Stub extends HwBinder implements IWifiStaIface {
        @Override
        public IHwBinder asBinder() {
            return this;
        }

        @Override
        public final ArrayList<String> interfaceChain() {
            return new ArrayList<>(Arrays.asList(IWifiStaIface.kInterfaceName, IWifiIface.kInterfaceName, IBase.kInterfaceName));
        }

        @Override
        public final String interfaceDescriptor() {
            return IWifiStaIface.kInterfaceName;
        }

        @Override
        public final ArrayList<byte[]> getHashChain() {
            return new ArrayList<>(Arrays.asList(new byte[]{59, -128, -109, -45, -98, -15, -31, 14, 67, -59, 83, -118, -5, -11, -1, 110, 57, -72, -40, 22, -114, -69, -31, -103, -115, -103, 62, -119, -30, 95, 20, -91}, new byte[]{107, -102, -44, 58, 94, -5, -26, -54, 33, 79, 117, 30, 34, -50, 67, -49, 92, -44, -43, -43, -14, -53, -88, 15, 36, -52, -45, 117, 90, 114, WifiLoggerHal.WIFI_ALERT_REASON_MAX, 28}, new byte[]{-67, -38, -74, 24, 77, 122, 52, 109, -90, -96, 125, -64, -126, -116, -15, -102, 105, 111, 76, -86, 54, 17, -59, 31, 46, 20, 86, 90, 20, -76, 15, -39}));
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
            if (IWifiStaIface.kInterfaceName.equals(str)) {
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
                        hwParcel.enforceInterface(IWifiIface.kInterfaceName);
                        getType(new IWifiIface.getTypeCallback() {
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
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiIface.kInterfaceName);
                        getName(new IWifiIface.getNameCallback() {
                            @Override
                            public void onValues(WifiStatus wifiStatus, String str) {
                                hwParcel2.writeStatus(0);
                                wifiStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeString(str);
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
                        hwParcel.enforceInterface(IWifiStaIface.kInterfaceName);
                        WifiStatus wifiStatusRegisterEventCallback = registerEventCallback(IWifiStaIfaceEventCallback.asInterface(hwParcel.readStrongBinder()));
                        hwParcel2.writeStatus(0);
                        wifiStatusRegisterEventCallback.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 4:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiStaIface.kInterfaceName);
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
                case 5:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiStaIface.kInterfaceName);
                        getApfPacketFilterCapabilities(new getApfPacketFilterCapabilitiesCallback() {
                            @Override
                            public void onValues(WifiStatus wifiStatus, StaApfPacketFilterCapabilities staApfPacketFilterCapabilities) {
                                hwParcel2.writeStatus(0);
                                wifiStatus.writeToParcel(hwParcel2);
                                staApfPacketFilterCapabilities.writeToParcel(hwParcel2);
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
                        hwParcel.enforceInterface(IWifiStaIface.kInterfaceName);
                        WifiStatus wifiStatusInstallApfPacketFilter = installApfPacketFilter(hwParcel.readInt32(), hwParcel.readInt8Vector());
                        hwParcel2.writeStatus(0);
                        wifiStatusInstallApfPacketFilter.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 7:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiStaIface.kInterfaceName);
                        getBackgroundScanCapabilities(new getBackgroundScanCapabilitiesCallback() {
                            @Override
                            public void onValues(WifiStatus wifiStatus, StaBackgroundScanCapabilities staBackgroundScanCapabilities) {
                                hwParcel2.writeStatus(0);
                                wifiStatus.writeToParcel(hwParcel2);
                                staBackgroundScanCapabilities.writeToParcel(hwParcel2);
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
                        hwParcel.enforceInterface(IWifiStaIface.kInterfaceName);
                        getValidFrequenciesForBand(hwParcel.readInt32(), new getValidFrequenciesForBandCallback() {
                            @Override
                            public void onValues(WifiStatus wifiStatus, ArrayList<Integer> arrayList) {
                                hwParcel2.writeStatus(0);
                                wifiStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeInt32Vector(arrayList);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 9:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiStaIface.kInterfaceName);
                        int int32 = hwParcel.readInt32();
                        StaBackgroundScanParameters staBackgroundScanParameters = new StaBackgroundScanParameters();
                        staBackgroundScanParameters.readFromParcel(hwParcel);
                        WifiStatus wifiStatusStartBackgroundScan = startBackgroundScan(int32, staBackgroundScanParameters);
                        hwParcel2.writeStatus(0);
                        wifiStatusStartBackgroundScan.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 10:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiStaIface.kInterfaceName);
                        WifiStatus wifiStatusStopBackgroundScan = stopBackgroundScan(hwParcel.readInt32());
                        hwParcel2.writeStatus(0);
                        wifiStatusStopBackgroundScan.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 11:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiStaIface.kInterfaceName);
                        WifiStatus wifiStatusEnableLinkLayerStatsCollection = enableLinkLayerStatsCollection(hwParcel.readBool());
                        hwParcel2.writeStatus(0);
                        wifiStatusEnableLinkLayerStatsCollection.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 12:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiStaIface.kInterfaceName);
                        WifiStatus wifiStatusDisableLinkLayerStatsCollection = disableLinkLayerStatsCollection();
                        hwParcel2.writeStatus(0);
                        wifiStatusDisableLinkLayerStatsCollection.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 13:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiStaIface.kInterfaceName);
                        getLinkLayerStats(new getLinkLayerStatsCallback() {
                            @Override
                            public void onValues(WifiStatus wifiStatus, StaLinkLayerStats staLinkLayerStats) {
                                hwParcel2.writeStatus(0);
                                wifiStatus.writeToParcel(hwParcel2);
                                staLinkLayerStats.writeToParcel(hwParcel2);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 14:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiStaIface.kInterfaceName);
                        WifiStatus wifiStatusStartRssiMonitoring = startRssiMonitoring(hwParcel.readInt32(), hwParcel.readInt32(), hwParcel.readInt32());
                        hwParcel2.writeStatus(0);
                        wifiStatusStartRssiMonitoring.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 15:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiStaIface.kInterfaceName);
                        WifiStatus wifiStatusStopRssiMonitoring = stopRssiMonitoring(hwParcel.readInt32());
                        hwParcel2.writeStatus(0);
                        wifiStatusStopRssiMonitoring.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 16:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiStaIface.kInterfaceName);
                        getRoamingCapabilities(new getRoamingCapabilitiesCallback() {
                            @Override
                            public void onValues(WifiStatus wifiStatus, StaRoamingCapabilities staRoamingCapabilities) {
                                hwParcel2.writeStatus(0);
                                wifiStatus.writeToParcel(hwParcel2);
                                staRoamingCapabilities.writeToParcel(hwParcel2);
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
                        hwParcel.enforceInterface(IWifiStaIface.kInterfaceName);
                        StaRoamingConfig staRoamingConfig = new StaRoamingConfig();
                        staRoamingConfig.readFromParcel(hwParcel);
                        WifiStatus wifiStatusConfigureRoaming = configureRoaming(staRoamingConfig);
                        hwParcel2.writeStatus(0);
                        wifiStatusConfigureRoaming.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 18:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiStaIface.kInterfaceName);
                        WifiStatus roamingState = setRoamingState(hwParcel.readInt8());
                        hwParcel2.writeStatus(0);
                        roamingState.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 19:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiStaIface.kInterfaceName);
                        WifiStatus wifiStatusEnableNdOffload = enableNdOffload(hwParcel.readBool());
                        hwParcel2.writeStatus(0);
                        wifiStatusEnableNdOffload.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 20:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiStaIface.kInterfaceName);
                        int int322 = hwParcel.readInt32();
                        ArrayList<Byte> int8Vector = hwParcel.readInt8Vector();
                        short int16 = hwParcel.readInt16();
                        byte[] bArr = new byte[6];
                        hwParcel.readBuffer(6L).copyToInt8Array(0L, bArr, 6);
                        byte[] bArr2 = new byte[6];
                        hwParcel.readBuffer(6L).copyToInt8Array(0L, bArr2, 6);
                        WifiStatus wifiStatusStartSendingKeepAlivePackets = startSendingKeepAlivePackets(int322, int8Vector, int16, bArr, bArr2, hwParcel.readInt32());
                        hwParcel2.writeStatus(0);
                        wifiStatusStartSendingKeepAlivePackets.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case ISupplicantStaIfaceCallback.ReasonCode.UNSUPPORTED_RSN_IE_VERSION:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiStaIface.kInterfaceName);
                        WifiStatus wifiStatusStopSendingKeepAlivePackets = stopSendingKeepAlivePackets(hwParcel.readInt32());
                        hwParcel2.writeStatus(0);
                        wifiStatusStopSendingKeepAlivePackets.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 22:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiStaIface.kInterfaceName);
                        byte[] bArr3 = new byte[3];
                        hwParcel.readBuffer(3L).copyToInt8Array(0L, bArr3, 3);
                        WifiStatus scanningMacOui = setScanningMacOui(bArr3);
                        hwParcel2.writeStatus(0);
                        scanningMacOui.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 23:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiStaIface.kInterfaceName);
                        WifiStatus wifiStatusStartDebugPacketFateMonitoring = startDebugPacketFateMonitoring();
                        hwParcel2.writeStatus(0);
                        wifiStatusStartDebugPacketFateMonitoring.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 24:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiStaIface.kInterfaceName);
                        getDebugTxPacketFates(new getDebugTxPacketFatesCallback() {
                            @Override
                            public void onValues(WifiStatus wifiStatus, ArrayList<WifiDebugTxPacketFateReport> arrayList) {
                                hwParcel2.writeStatus(0);
                                wifiStatus.writeToParcel(hwParcel2);
                                WifiDebugTxPacketFateReport.writeVectorToParcel(hwParcel2, arrayList);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 25:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiStaIface.kInterfaceName);
                        getDebugRxPacketFates(new getDebugRxPacketFatesCallback() {
                            @Override
                            public void onValues(WifiStatus wifiStatus, ArrayList<WifiDebugRxPacketFateReport> arrayList) {
                                hwParcel2.writeStatus(0);
                                wifiStatus.writeToParcel(hwParcel2);
                                WifiDebugRxPacketFateReport.writeVectorToParcel(hwParcel2, arrayList);
                                hwParcel2.send();
                            }
                        });
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
