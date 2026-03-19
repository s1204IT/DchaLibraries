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

public interface IWifiNanIfaceEventCallback extends IBase {
    public static final String kInterfaceName = "android.hardware.wifi@1.0::IWifiNanIfaceEventCallback";

    @Override
    IHwBinder asBinder();

    void eventClusterEvent(NanClusterEventInd nanClusterEventInd) throws RemoteException;

    void eventDataPathConfirm(NanDataPathConfirmInd nanDataPathConfirmInd) throws RemoteException;

    void eventDataPathRequest(NanDataPathRequestInd nanDataPathRequestInd) throws RemoteException;

    void eventDataPathTerminated(int i) throws RemoteException;

    void eventDisabled(WifiNanStatus wifiNanStatus) throws RemoteException;

    void eventFollowupReceived(NanFollowupReceivedInd nanFollowupReceivedInd) throws RemoteException;

    void eventMatch(NanMatchInd nanMatchInd) throws RemoteException;

    void eventMatchExpired(byte b, int i) throws RemoteException;

    void eventPublishTerminated(byte b, WifiNanStatus wifiNanStatus) throws RemoteException;

    void eventSubscribeTerminated(byte b, WifiNanStatus wifiNanStatus) throws RemoteException;

    void eventTransmitFollowup(short s, WifiNanStatus wifiNanStatus) throws RemoteException;

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

    void notifyCapabilitiesResponse(short s, WifiNanStatus wifiNanStatus, NanCapabilities nanCapabilities) throws RemoteException;

    void notifyConfigResponse(short s, WifiNanStatus wifiNanStatus) throws RemoteException;

    void notifyCreateDataInterfaceResponse(short s, WifiNanStatus wifiNanStatus) throws RemoteException;

    void notifyDeleteDataInterfaceResponse(short s, WifiNanStatus wifiNanStatus) throws RemoteException;

    void notifyDisableResponse(short s, WifiNanStatus wifiNanStatus) throws RemoteException;

    void notifyEnableResponse(short s, WifiNanStatus wifiNanStatus) throws RemoteException;

    void notifyInitiateDataPathResponse(short s, WifiNanStatus wifiNanStatus, int i) throws RemoteException;

    void notifyRespondToDataPathIndicationResponse(short s, WifiNanStatus wifiNanStatus) throws RemoteException;

    void notifyStartPublishResponse(short s, WifiNanStatus wifiNanStatus, byte b) throws RemoteException;

    void notifyStartSubscribeResponse(short s, WifiNanStatus wifiNanStatus, byte b) throws RemoteException;

    void notifyStopPublishResponse(short s, WifiNanStatus wifiNanStatus) throws RemoteException;

    void notifyStopSubscribeResponse(short s, WifiNanStatus wifiNanStatus) throws RemoteException;

    @Override
    void notifySyspropsChanged() throws RemoteException;

    void notifyTerminateDataPathResponse(short s, WifiNanStatus wifiNanStatus) throws RemoteException;

    void notifyTransmitFollowupResponse(short s, WifiNanStatus wifiNanStatus) throws RemoteException;

    @Override
    void ping() throws RemoteException;

    @Override
    void setHALInstrumentation() throws RemoteException;

    @Override
    boolean unlinkToDeath(IHwBinder.DeathRecipient deathRecipient) throws RemoteException;

    static IWifiNanIfaceEventCallback asInterface(IHwBinder iHwBinder) {
        if (iHwBinder == null) {
            return null;
        }
        IHwInterface iHwInterfaceQueryLocalInterface = iHwBinder.queryLocalInterface(kInterfaceName);
        if (iHwInterfaceQueryLocalInterface != null && (iHwInterfaceQueryLocalInterface instanceof IWifiNanIfaceEventCallback)) {
            return (IWifiNanIfaceEventCallback) iHwInterfaceQueryLocalInterface;
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

    static IWifiNanIfaceEventCallback castFrom(IHwInterface iHwInterface) {
        if (iHwInterface == null) {
            return null;
        }
        return asInterface(iHwInterface.asBinder());
    }

    static IWifiNanIfaceEventCallback getService(String str, boolean z) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, str, z));
    }

    static IWifiNanIfaceEventCallback getService(boolean z) throws RemoteException {
        return getService(HalDeviceManager.HAL_INSTANCE_NAME, z);
    }

    static IWifiNanIfaceEventCallback getService(String str) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, str));
    }

    static IWifiNanIfaceEventCallback getService() throws RemoteException {
        return getService(HalDeviceManager.HAL_INSTANCE_NAME);
    }

    public static final class Proxy implements IWifiNanIfaceEventCallback {
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
                return "[class or subclass of android.hardware.wifi@1.0::IWifiNanIfaceEventCallback]@Proxy";
            }
        }

        public final boolean equals(Object obj) {
            return HidlSupport.interfacesEqual(this, obj);
        }

        public final int hashCode() {
            return asBinder().hashCode();
        }

        @Override
        public void notifyCapabilitiesResponse(short s, WifiNanStatus wifiNanStatus, NanCapabilities nanCapabilities) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiNanIfaceEventCallback.kInterfaceName);
            hwParcel.writeInt16(s);
            wifiNanStatus.writeToParcel(hwParcel);
            nanCapabilities.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(1, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void notifyEnableResponse(short s, WifiNanStatus wifiNanStatus) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiNanIfaceEventCallback.kInterfaceName);
            hwParcel.writeInt16(s);
            wifiNanStatus.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(2, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void notifyConfigResponse(short s, WifiNanStatus wifiNanStatus) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiNanIfaceEventCallback.kInterfaceName);
            hwParcel.writeInt16(s);
            wifiNanStatus.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(3, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void notifyDisableResponse(short s, WifiNanStatus wifiNanStatus) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiNanIfaceEventCallback.kInterfaceName);
            hwParcel.writeInt16(s);
            wifiNanStatus.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(4, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void notifyStartPublishResponse(short s, WifiNanStatus wifiNanStatus, byte b) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiNanIfaceEventCallback.kInterfaceName);
            hwParcel.writeInt16(s);
            wifiNanStatus.writeToParcel(hwParcel);
            hwParcel.writeInt8(b);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(5, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void notifyStopPublishResponse(short s, WifiNanStatus wifiNanStatus) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiNanIfaceEventCallback.kInterfaceName);
            hwParcel.writeInt16(s);
            wifiNanStatus.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(6, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void notifyStartSubscribeResponse(short s, WifiNanStatus wifiNanStatus, byte b) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiNanIfaceEventCallback.kInterfaceName);
            hwParcel.writeInt16(s);
            wifiNanStatus.writeToParcel(hwParcel);
            hwParcel.writeInt8(b);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(7, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void notifyStopSubscribeResponse(short s, WifiNanStatus wifiNanStatus) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiNanIfaceEventCallback.kInterfaceName);
            hwParcel.writeInt16(s);
            wifiNanStatus.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(8, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void notifyTransmitFollowupResponse(short s, WifiNanStatus wifiNanStatus) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiNanIfaceEventCallback.kInterfaceName);
            hwParcel.writeInt16(s);
            wifiNanStatus.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(9, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void notifyCreateDataInterfaceResponse(short s, WifiNanStatus wifiNanStatus) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiNanIfaceEventCallback.kInterfaceName);
            hwParcel.writeInt16(s);
            wifiNanStatus.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(10, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void notifyDeleteDataInterfaceResponse(short s, WifiNanStatus wifiNanStatus) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiNanIfaceEventCallback.kInterfaceName);
            hwParcel.writeInt16(s);
            wifiNanStatus.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(11, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void notifyInitiateDataPathResponse(short s, WifiNanStatus wifiNanStatus, int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiNanIfaceEventCallback.kInterfaceName);
            hwParcel.writeInt16(s);
            wifiNanStatus.writeToParcel(hwParcel);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(12, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void notifyRespondToDataPathIndicationResponse(short s, WifiNanStatus wifiNanStatus) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiNanIfaceEventCallback.kInterfaceName);
            hwParcel.writeInt16(s);
            wifiNanStatus.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(13, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void notifyTerminateDataPathResponse(short s, WifiNanStatus wifiNanStatus) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiNanIfaceEventCallback.kInterfaceName);
            hwParcel.writeInt16(s);
            wifiNanStatus.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(14, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void eventClusterEvent(NanClusterEventInd nanClusterEventInd) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiNanIfaceEventCallback.kInterfaceName);
            nanClusterEventInd.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(15, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void eventDisabled(WifiNanStatus wifiNanStatus) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiNanIfaceEventCallback.kInterfaceName);
            wifiNanStatus.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(16, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void eventPublishTerminated(byte b, WifiNanStatus wifiNanStatus) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiNanIfaceEventCallback.kInterfaceName);
            hwParcel.writeInt8(b);
            wifiNanStatus.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(17, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void eventSubscribeTerminated(byte b, WifiNanStatus wifiNanStatus) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiNanIfaceEventCallback.kInterfaceName);
            hwParcel.writeInt8(b);
            wifiNanStatus.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(18, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void eventMatch(NanMatchInd nanMatchInd) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiNanIfaceEventCallback.kInterfaceName);
            nanMatchInd.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(19, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void eventMatchExpired(byte b, int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiNanIfaceEventCallback.kInterfaceName);
            hwParcel.writeInt8(b);
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
        public void eventFollowupReceived(NanFollowupReceivedInd nanFollowupReceivedInd) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiNanIfaceEventCallback.kInterfaceName);
            nanFollowupReceivedInd.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(21, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void eventTransmitFollowup(short s, WifiNanStatus wifiNanStatus) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiNanIfaceEventCallback.kInterfaceName);
            hwParcel.writeInt16(s);
            wifiNanStatus.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(22, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void eventDataPathRequest(NanDataPathRequestInd nanDataPathRequestInd) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiNanIfaceEventCallback.kInterfaceName);
            nanDataPathRequestInd.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(23, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void eventDataPathConfirm(NanDataPathConfirmInd nanDataPathConfirmInd) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiNanIfaceEventCallback.kInterfaceName);
            nanDataPathConfirmInd.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(24, hwParcel, hwParcel2, 1);
                hwParcel.releaseTemporaryStorage();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void eventDataPathTerminated(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiNanIfaceEventCallback.kInterfaceName);
            hwParcel.writeInt32(i);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(25, hwParcel, hwParcel2, 1);
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

    public static abstract class Stub extends HwBinder implements IWifiNanIfaceEventCallback {
        @Override
        public IHwBinder asBinder() {
            return this;
        }

        @Override
        public final ArrayList<String> interfaceChain() {
            return new ArrayList<>(Arrays.asList(IWifiNanIfaceEventCallback.kInterfaceName, IBase.kInterfaceName));
        }

        @Override
        public final String interfaceDescriptor() {
            return IWifiNanIfaceEventCallback.kInterfaceName;
        }

        @Override
        public final ArrayList<byte[]> getHashChain() {
            return new ArrayList<>(Arrays.asList(new byte[]{50, 92, -108, -13, -31, -91, 101, -75, 107, -68, 116, -6, -35, -67, 11, -89, -53, -126, 79, 38, 61, -52, -7, -33, -1, 45, -81, 98, -72, 110, -41, 116}, new byte[]{-67, -38, -74, 24, 77, 122, 52, 109, -90, -96, 125, -64, -126, -116, -15, -102, 105, 111, 76, -86, 54, 17, -59, 31, 46, 20, 86, 90, 20, -76, 15, -39}));
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
            if (IWifiNanIfaceEventCallback.kInterfaceName.equals(str)) {
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
                        hwParcel.enforceInterface(IWifiNanIfaceEventCallback.kInterfaceName);
                        short int16 = hwParcel.readInt16();
                        WifiNanStatus wifiNanStatus = new WifiNanStatus();
                        wifiNanStatus.readFromParcel(hwParcel);
                        NanCapabilities nanCapabilities = new NanCapabilities();
                        nanCapabilities.readFromParcel(hwParcel);
                        notifyCapabilitiesResponse(int16, wifiNanStatus, nanCapabilities);
                    }
                    break;
                case 2:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiNanIfaceEventCallback.kInterfaceName);
                        short int162 = hwParcel.readInt16();
                        WifiNanStatus wifiNanStatus2 = new WifiNanStatus();
                        wifiNanStatus2.readFromParcel(hwParcel);
                        notifyEnableResponse(int162, wifiNanStatus2);
                    }
                    break;
                case 3:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiNanIfaceEventCallback.kInterfaceName);
                        short int163 = hwParcel.readInt16();
                        WifiNanStatus wifiNanStatus3 = new WifiNanStatus();
                        wifiNanStatus3.readFromParcel(hwParcel);
                        notifyConfigResponse(int163, wifiNanStatus3);
                    }
                    break;
                case 4:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiNanIfaceEventCallback.kInterfaceName);
                        short int164 = hwParcel.readInt16();
                        WifiNanStatus wifiNanStatus4 = new WifiNanStatus();
                        wifiNanStatus4.readFromParcel(hwParcel);
                        notifyDisableResponse(int164, wifiNanStatus4);
                    }
                    break;
                case 5:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiNanIfaceEventCallback.kInterfaceName);
                        short int165 = hwParcel.readInt16();
                        WifiNanStatus wifiNanStatus5 = new WifiNanStatus();
                        wifiNanStatus5.readFromParcel(hwParcel);
                        notifyStartPublishResponse(int165, wifiNanStatus5, hwParcel.readInt8());
                    }
                    break;
                case 6:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiNanIfaceEventCallback.kInterfaceName);
                        short int166 = hwParcel.readInt16();
                        WifiNanStatus wifiNanStatus6 = new WifiNanStatus();
                        wifiNanStatus6.readFromParcel(hwParcel);
                        notifyStopPublishResponse(int166, wifiNanStatus6);
                    }
                    break;
                case 7:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiNanIfaceEventCallback.kInterfaceName);
                        short int167 = hwParcel.readInt16();
                        WifiNanStatus wifiNanStatus7 = new WifiNanStatus();
                        wifiNanStatus7.readFromParcel(hwParcel);
                        notifyStartSubscribeResponse(int167, wifiNanStatus7, hwParcel.readInt8());
                    }
                    break;
                case 8:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiNanIfaceEventCallback.kInterfaceName);
                        short int168 = hwParcel.readInt16();
                        WifiNanStatus wifiNanStatus8 = new WifiNanStatus();
                        wifiNanStatus8.readFromParcel(hwParcel);
                        notifyStopSubscribeResponse(int168, wifiNanStatus8);
                    }
                    break;
                case 9:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiNanIfaceEventCallback.kInterfaceName);
                        short int169 = hwParcel.readInt16();
                        WifiNanStatus wifiNanStatus9 = new WifiNanStatus();
                        wifiNanStatus9.readFromParcel(hwParcel);
                        notifyTransmitFollowupResponse(int169, wifiNanStatus9);
                    }
                    break;
                case 10:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiNanIfaceEventCallback.kInterfaceName);
                        short int1610 = hwParcel.readInt16();
                        WifiNanStatus wifiNanStatus10 = new WifiNanStatus();
                        wifiNanStatus10.readFromParcel(hwParcel);
                        notifyCreateDataInterfaceResponse(int1610, wifiNanStatus10);
                    }
                    break;
                case 11:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiNanIfaceEventCallback.kInterfaceName);
                        short int1611 = hwParcel.readInt16();
                        WifiNanStatus wifiNanStatus11 = new WifiNanStatus();
                        wifiNanStatus11.readFromParcel(hwParcel);
                        notifyDeleteDataInterfaceResponse(int1611, wifiNanStatus11);
                    }
                    break;
                case 12:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiNanIfaceEventCallback.kInterfaceName);
                        short int1612 = hwParcel.readInt16();
                        WifiNanStatus wifiNanStatus12 = new WifiNanStatus();
                        wifiNanStatus12.readFromParcel(hwParcel);
                        notifyInitiateDataPathResponse(int1612, wifiNanStatus12, hwParcel.readInt32());
                    }
                    break;
                case 13:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiNanIfaceEventCallback.kInterfaceName);
                        short int1613 = hwParcel.readInt16();
                        WifiNanStatus wifiNanStatus13 = new WifiNanStatus();
                        wifiNanStatus13.readFromParcel(hwParcel);
                        notifyRespondToDataPathIndicationResponse(int1613, wifiNanStatus13);
                    }
                    break;
                case 14:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiNanIfaceEventCallback.kInterfaceName);
                        short int1614 = hwParcel.readInt16();
                        WifiNanStatus wifiNanStatus14 = new WifiNanStatus();
                        wifiNanStatus14.readFromParcel(hwParcel);
                        notifyTerminateDataPathResponse(int1614, wifiNanStatus14);
                    }
                    break;
                case 15:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiNanIfaceEventCallback.kInterfaceName);
                        NanClusterEventInd nanClusterEventInd = new NanClusterEventInd();
                        nanClusterEventInd.readFromParcel(hwParcel);
                        eventClusterEvent(nanClusterEventInd);
                    }
                    break;
                case 16:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiNanIfaceEventCallback.kInterfaceName);
                        WifiNanStatus wifiNanStatus15 = new WifiNanStatus();
                        wifiNanStatus15.readFromParcel(hwParcel);
                        eventDisabled(wifiNanStatus15);
                    }
                    break;
                case 17:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiNanIfaceEventCallback.kInterfaceName);
                        byte int8 = hwParcel.readInt8();
                        WifiNanStatus wifiNanStatus16 = new WifiNanStatus();
                        wifiNanStatus16.readFromParcel(hwParcel);
                        eventPublishTerminated(int8, wifiNanStatus16);
                    }
                    break;
                case 18:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiNanIfaceEventCallback.kInterfaceName);
                        byte int82 = hwParcel.readInt8();
                        WifiNanStatus wifiNanStatus17 = new WifiNanStatus();
                        wifiNanStatus17.readFromParcel(hwParcel);
                        eventSubscribeTerminated(int82, wifiNanStatus17);
                    }
                    break;
                case 19:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiNanIfaceEventCallback.kInterfaceName);
                        NanMatchInd nanMatchInd = new NanMatchInd();
                        nanMatchInd.readFromParcel(hwParcel);
                        eventMatch(nanMatchInd);
                    }
                    break;
                case 20:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiNanIfaceEventCallback.kInterfaceName);
                        eventMatchExpired(hwParcel.readInt8(), hwParcel.readInt32());
                    }
                    break;
                case ISupplicantStaIfaceCallback.ReasonCode.UNSUPPORTED_RSN_IE_VERSION:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiNanIfaceEventCallback.kInterfaceName);
                        NanFollowupReceivedInd nanFollowupReceivedInd = new NanFollowupReceivedInd();
                        nanFollowupReceivedInd.readFromParcel(hwParcel);
                        eventFollowupReceived(nanFollowupReceivedInd);
                    }
                    break;
                case 22:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiNanIfaceEventCallback.kInterfaceName);
                        short int1615 = hwParcel.readInt16();
                        WifiNanStatus wifiNanStatus18 = new WifiNanStatus();
                        wifiNanStatus18.readFromParcel(hwParcel);
                        eventTransmitFollowup(int1615, wifiNanStatus18);
                    }
                    break;
                case 23:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiNanIfaceEventCallback.kInterfaceName);
                        NanDataPathRequestInd nanDataPathRequestInd = new NanDataPathRequestInd();
                        nanDataPathRequestInd.readFromParcel(hwParcel);
                        eventDataPathRequest(nanDataPathRequestInd);
                    }
                    break;
                case 24:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiNanIfaceEventCallback.kInterfaceName);
                        NanDataPathConfirmInd nanDataPathConfirmInd = new NanDataPathConfirmInd();
                        nanDataPathConfirmInd.readFromParcel(hwParcel);
                        eventDataPathConfirm(nanDataPathConfirmInd);
                    }
                    break;
                case 25:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiNanIfaceEventCallback.kInterfaceName);
                        eventDataPathTerminated(hwParcel.readInt32());
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
