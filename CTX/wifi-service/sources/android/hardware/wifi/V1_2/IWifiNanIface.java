package android.hardware.wifi.V1_2;

import android.hardware.wifi.V1_0.IWifiIface;
import android.hardware.wifi.V1_0.NanConfigRequest;
import android.hardware.wifi.V1_0.NanEnableRequest;
import android.hardware.wifi.V1_0.NanInitiateDataPathRequest;
import android.hardware.wifi.V1_0.NanPublishRequest;
import android.hardware.wifi.V1_0.NanRespondToDataPathIndicationRequest;
import android.hardware.wifi.V1_0.NanSubscribeRequest;
import android.hardware.wifi.V1_0.NanTransmitFollowupRequest;
import android.hardware.wifi.V1_0.WifiStatus;
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

public interface IWifiNanIface extends android.hardware.wifi.V1_0.IWifiNanIface {
    public static final String kInterfaceName = "android.hardware.wifi@1.2::IWifiNanIface";

    @Override
    IHwBinder asBinder();

    WifiStatus configRequest_1_2(short s, NanConfigRequest nanConfigRequest, NanConfigRequestSupplemental nanConfigRequestSupplemental) throws RemoteException;

    WifiStatus enableRequest_1_2(short s, NanEnableRequest nanEnableRequest, NanConfigRequestSupplemental nanConfigRequestSupplemental) throws RemoteException;

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

    WifiStatus registerEventCallback_1_2(IWifiNanIfaceEventCallback iWifiNanIfaceEventCallback) throws RemoteException;

    @Override
    void setHALInstrumentation() throws RemoteException;

    @Override
    boolean unlinkToDeath(IHwBinder.DeathRecipient deathRecipient) throws RemoteException;

    static IWifiNanIface asInterface(IHwBinder iHwBinder) {
        if (iHwBinder == null) {
            return null;
        }
        IHwInterface iHwInterfaceQueryLocalInterface = iHwBinder.queryLocalInterface(kInterfaceName);
        if (iHwInterfaceQueryLocalInterface != null && (iHwInterfaceQueryLocalInterface instanceof IWifiNanIface)) {
            return (IWifiNanIface) iHwInterfaceQueryLocalInterface;
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

    static IWifiNanIface castFrom(IHwInterface iHwInterface) {
        if (iHwInterface == null) {
            return null;
        }
        return asInterface(iHwInterface.asBinder());
    }

    static IWifiNanIface getService(String str, boolean z) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, str, z));
    }

    static IWifiNanIface getService(boolean z) throws RemoteException {
        return getService(HalDeviceManager.HAL_INSTANCE_NAME, z);
    }

    static IWifiNanIface getService(String str) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, str));
    }

    static IWifiNanIface getService() throws RemoteException {
        return getService(HalDeviceManager.HAL_INSTANCE_NAME);
    }

    public static final class Proxy implements IWifiNanIface {
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
                return "[class or subclass of android.hardware.wifi@1.2::IWifiNanIface]@Proxy";
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
        public WifiStatus registerEventCallback(android.hardware.wifi.V1_0.IWifiNanIfaceEventCallback iWifiNanIfaceEventCallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiNanIface.kInterfaceName);
            hwParcel.writeStrongBinder(iWifiNanIfaceEventCallback == null ? null : iWifiNanIfaceEventCallback.asBinder());
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
        public WifiStatus getCapabilitiesRequest(short s) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiNanIface.kInterfaceName);
            hwParcel.writeInt16(s);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(4, hwParcel, hwParcel2, 0);
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
        public WifiStatus enableRequest(short s, NanEnableRequest nanEnableRequest) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiNanIface.kInterfaceName);
            hwParcel.writeInt16(s);
            nanEnableRequest.writeToParcel(hwParcel);
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
        public WifiStatus configRequest(short s, NanConfigRequest nanConfigRequest) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiNanIface.kInterfaceName);
            hwParcel.writeInt16(s);
            nanConfigRequest.writeToParcel(hwParcel);
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
        public WifiStatus disableRequest(short s) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiNanIface.kInterfaceName);
            hwParcel.writeInt16(s);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(7, hwParcel, hwParcel2, 0);
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
        public WifiStatus startPublishRequest(short s, NanPublishRequest nanPublishRequest) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiNanIface.kInterfaceName);
            hwParcel.writeInt16(s);
            nanPublishRequest.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(8, hwParcel, hwParcel2, 0);
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
        public WifiStatus stopPublishRequest(short s, byte b) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiNanIface.kInterfaceName);
            hwParcel.writeInt16(s);
            hwParcel.writeInt8(b);
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
        public WifiStatus startSubscribeRequest(short s, NanSubscribeRequest nanSubscribeRequest) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiNanIface.kInterfaceName);
            hwParcel.writeInt16(s);
            nanSubscribeRequest.writeToParcel(hwParcel);
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
        public WifiStatus stopSubscribeRequest(short s, byte b) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiNanIface.kInterfaceName);
            hwParcel.writeInt16(s);
            hwParcel.writeInt8(b);
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
        public WifiStatus transmitFollowupRequest(short s, NanTransmitFollowupRequest nanTransmitFollowupRequest) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiNanIface.kInterfaceName);
            hwParcel.writeInt16(s);
            nanTransmitFollowupRequest.writeToParcel(hwParcel);
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
        public WifiStatus createDataInterfaceRequest(short s, String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiNanIface.kInterfaceName);
            hwParcel.writeInt16(s);
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
        public WifiStatus deleteDataInterfaceRequest(short s, String str) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiNanIface.kInterfaceName);
            hwParcel.writeInt16(s);
            hwParcel.writeString(str);
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
        public WifiStatus initiateDataPathRequest(short s, NanInitiateDataPathRequest nanInitiateDataPathRequest) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiNanIface.kInterfaceName);
            hwParcel.writeInt16(s);
            nanInitiateDataPathRequest.writeToParcel(hwParcel);
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
        public WifiStatus respondToDataPathIndicationRequest(short s, NanRespondToDataPathIndicationRequest nanRespondToDataPathIndicationRequest) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiNanIface.kInterfaceName);
            hwParcel.writeInt16(s);
            nanRespondToDataPathIndicationRequest.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(16, hwParcel, hwParcel2, 0);
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
        public WifiStatus terminateDataPathRequest(short s, int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(android.hardware.wifi.V1_0.IWifiNanIface.kInterfaceName);
            hwParcel.writeInt16(s);
            hwParcel.writeInt32(i);
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
        public WifiStatus registerEventCallback_1_2(IWifiNanIfaceEventCallback iWifiNanIfaceEventCallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiNanIface.kInterfaceName);
            hwParcel.writeStrongBinder(iWifiNanIfaceEventCallback == null ? null : iWifiNanIfaceEventCallback.asBinder());
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
        public WifiStatus enableRequest_1_2(short s, NanEnableRequest nanEnableRequest, NanConfigRequestSupplemental nanConfigRequestSupplemental) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiNanIface.kInterfaceName);
            hwParcel.writeInt16(s);
            nanEnableRequest.writeToParcel(hwParcel);
            nanConfigRequestSupplemental.writeToParcel(hwParcel);
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
        public WifiStatus configRequest_1_2(short s, NanConfigRequest nanConfigRequest, NanConfigRequestSupplemental nanConfigRequestSupplemental) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiNanIface.kInterfaceName);
            hwParcel.writeInt16(s);
            nanConfigRequest.writeToParcel(hwParcel);
            nanConfigRequestSupplemental.writeToParcel(hwParcel);
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

    public static abstract class Stub extends HwBinder implements IWifiNanIface {
        @Override
        public IHwBinder asBinder() {
            return this;
        }

        @Override
        public final ArrayList<String> interfaceChain() {
            return new ArrayList<>(Arrays.asList(IWifiNanIface.kInterfaceName, android.hardware.wifi.V1_0.IWifiNanIface.kInterfaceName, IWifiIface.kInterfaceName, IBase.kInterfaceName));
        }

        @Override
        public final String interfaceDescriptor() {
            return IWifiNanIface.kInterfaceName;
        }

        @Override
        public final ArrayList<byte[]> getHashChain() {
            return new ArrayList<>(Arrays.asList(new byte[]{-116, 126, -13, 47, -57, -115, 94, -58, -26, -107, 109, -29, 120, 76, -62, -58, -12, 38, 20, -75, 39, 45, 46, 70, 31, 109, 96, 83, 75, -93, -114, -62}, new byte[]{-70, 90, -89, 79, 27, -89, 20, -16, 9, 56, 100, 34, 121, 35, 73, 40, 8, 121, 91, -38, 97, -103, -60, -22, 8, -111, 50, 45, 39, -8, -55, 49}, new byte[]{107, -102, -44, 58, 94, -5, -26, -54, 33, 79, 117, 30, 34, -50, 67, -49, 92, -44, -43, -43, -14, -53, -88, 15, 36, -52, -45, 117, 90, 114, WifiLoggerHal.WIFI_ALERT_REASON_MAX, 28}, new byte[]{-67, -38, -74, 24, 77, 122, 52, 109, -90, -96, 125, -64, -126, -116, -15, -102, 105, 111, 76, -86, 54, 17, -59, 31, 46, 20, 86, 90, 20, -76, 15, -39}));
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
            if (IWifiNanIface.kInterfaceName.equals(str)) {
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
                        hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiNanIface.kInterfaceName);
                        WifiStatus wifiStatusRegisterEventCallback = registerEventCallback(android.hardware.wifi.V1_0.IWifiNanIfaceEventCallback.asInterface(hwParcel.readStrongBinder()));
                        hwParcel2.writeStatus(0);
                        wifiStatusRegisterEventCallback.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 4:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiNanIface.kInterfaceName);
                        WifiStatus capabilitiesRequest = getCapabilitiesRequest(hwParcel.readInt16());
                        hwParcel2.writeStatus(0);
                        capabilitiesRequest.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 5:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiNanIface.kInterfaceName);
                        short int16 = hwParcel.readInt16();
                        NanEnableRequest nanEnableRequest = new NanEnableRequest();
                        nanEnableRequest.readFromParcel(hwParcel);
                        WifiStatus wifiStatusEnableRequest = enableRequest(int16, nanEnableRequest);
                        hwParcel2.writeStatus(0);
                        wifiStatusEnableRequest.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 6:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiNanIface.kInterfaceName);
                        short int162 = hwParcel.readInt16();
                        NanConfigRequest nanConfigRequest = new NanConfigRequest();
                        nanConfigRequest.readFromParcel(hwParcel);
                        WifiStatus wifiStatusConfigRequest = configRequest(int162, nanConfigRequest);
                        hwParcel2.writeStatus(0);
                        wifiStatusConfigRequest.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 7:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiNanIface.kInterfaceName);
                        WifiStatus wifiStatusDisableRequest = disableRequest(hwParcel.readInt16());
                        hwParcel2.writeStatus(0);
                        wifiStatusDisableRequest.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 8:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiNanIface.kInterfaceName);
                        short int163 = hwParcel.readInt16();
                        NanPublishRequest nanPublishRequest = new NanPublishRequest();
                        nanPublishRequest.readFromParcel(hwParcel);
                        WifiStatus wifiStatusStartPublishRequest = startPublishRequest(int163, nanPublishRequest);
                        hwParcel2.writeStatus(0);
                        wifiStatusStartPublishRequest.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 9:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiNanIface.kInterfaceName);
                        WifiStatus wifiStatusStopPublishRequest = stopPublishRequest(hwParcel.readInt16(), hwParcel.readInt8());
                        hwParcel2.writeStatus(0);
                        wifiStatusStopPublishRequest.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 10:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiNanIface.kInterfaceName);
                        short int164 = hwParcel.readInt16();
                        NanSubscribeRequest nanSubscribeRequest = new NanSubscribeRequest();
                        nanSubscribeRequest.readFromParcel(hwParcel);
                        WifiStatus wifiStatusStartSubscribeRequest = startSubscribeRequest(int164, nanSubscribeRequest);
                        hwParcel2.writeStatus(0);
                        wifiStatusStartSubscribeRequest.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 11:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiNanIface.kInterfaceName);
                        WifiStatus wifiStatusStopSubscribeRequest = stopSubscribeRequest(hwParcel.readInt16(), hwParcel.readInt8());
                        hwParcel2.writeStatus(0);
                        wifiStatusStopSubscribeRequest.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 12:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiNanIface.kInterfaceName);
                        short int165 = hwParcel.readInt16();
                        NanTransmitFollowupRequest nanTransmitFollowupRequest = new NanTransmitFollowupRequest();
                        nanTransmitFollowupRequest.readFromParcel(hwParcel);
                        WifiStatus wifiStatusTransmitFollowupRequest = transmitFollowupRequest(int165, nanTransmitFollowupRequest);
                        hwParcel2.writeStatus(0);
                        wifiStatusTransmitFollowupRequest.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 13:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiNanIface.kInterfaceName);
                        WifiStatus wifiStatusCreateDataInterfaceRequest = createDataInterfaceRequest(hwParcel.readInt16(), hwParcel.readString());
                        hwParcel2.writeStatus(0);
                        wifiStatusCreateDataInterfaceRequest.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 14:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiNanIface.kInterfaceName);
                        WifiStatus wifiStatusDeleteDataInterfaceRequest = deleteDataInterfaceRequest(hwParcel.readInt16(), hwParcel.readString());
                        hwParcel2.writeStatus(0);
                        wifiStatusDeleteDataInterfaceRequest.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 15:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiNanIface.kInterfaceName);
                        short int166 = hwParcel.readInt16();
                        NanInitiateDataPathRequest nanInitiateDataPathRequest = new NanInitiateDataPathRequest();
                        nanInitiateDataPathRequest.readFromParcel(hwParcel);
                        WifiStatus wifiStatusInitiateDataPathRequest = initiateDataPathRequest(int166, nanInitiateDataPathRequest);
                        hwParcel2.writeStatus(0);
                        wifiStatusInitiateDataPathRequest.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 16:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiNanIface.kInterfaceName);
                        short int167 = hwParcel.readInt16();
                        NanRespondToDataPathIndicationRequest nanRespondToDataPathIndicationRequest = new NanRespondToDataPathIndicationRequest();
                        nanRespondToDataPathIndicationRequest.readFromParcel(hwParcel);
                        WifiStatus wifiStatusRespondToDataPathIndicationRequest = respondToDataPathIndicationRequest(int167, nanRespondToDataPathIndicationRequest);
                        hwParcel2.writeStatus(0);
                        wifiStatusRespondToDataPathIndicationRequest.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 17:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(android.hardware.wifi.V1_0.IWifiNanIface.kInterfaceName);
                        WifiStatus wifiStatusTerminateDataPathRequest = terminateDataPathRequest(hwParcel.readInt16(), hwParcel.readInt32());
                        hwParcel2.writeStatus(0);
                        wifiStatusTerminateDataPathRequest.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 18:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiNanIface.kInterfaceName);
                        WifiStatus wifiStatusRegisterEventCallback_1_2 = registerEventCallback_1_2(IWifiNanIfaceEventCallback.asInterface(hwParcel.readStrongBinder()));
                        hwParcel2.writeStatus(0);
                        wifiStatusRegisterEventCallback_1_2.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 19:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiNanIface.kInterfaceName);
                        short int168 = hwParcel.readInt16();
                        NanEnableRequest nanEnableRequest2 = new NanEnableRequest();
                        nanEnableRequest2.readFromParcel(hwParcel);
                        NanConfigRequestSupplemental nanConfigRequestSupplemental = new NanConfigRequestSupplemental();
                        nanConfigRequestSupplemental.readFromParcel(hwParcel);
                        WifiStatus wifiStatusEnableRequest_1_2 = enableRequest_1_2(int168, nanEnableRequest2, nanConfigRequestSupplemental);
                        hwParcel2.writeStatus(0);
                        wifiStatusEnableRequest_1_2.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 20:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiNanIface.kInterfaceName);
                        short int169 = hwParcel.readInt16();
                        NanConfigRequest nanConfigRequest2 = new NanConfigRequest();
                        nanConfigRequest2.readFromParcel(hwParcel);
                        NanConfigRequestSupplemental nanConfigRequestSupplemental2 = new NanConfigRequestSupplemental();
                        nanConfigRequestSupplemental2.readFromParcel(hwParcel);
                        WifiStatus wifiStatusConfigRequest_1_2 = configRequest_1_2(int169, nanConfigRequest2, nanConfigRequestSupplemental2);
                        hwParcel2.writeStatus(0);
                        wifiStatusConfigRequest_1_2.writeToParcel(hwParcel2);
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
