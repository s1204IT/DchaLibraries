package android.hardware.wifi.V1_0;

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

public interface IWifiRttController extends IBase {
    public static final String kInterfaceName = "android.hardware.wifi@1.0::IWifiRttController";

    @FunctionalInterface
    public interface getBoundIfaceCallback {
        void onValues(WifiStatus wifiStatus, IWifiIface iWifiIface);
    }

    @FunctionalInterface
    public interface getCapabilitiesCallback {
        void onValues(WifiStatus wifiStatus, RttCapabilities rttCapabilities);
    }

    @FunctionalInterface
    public interface getResponderInfoCallback {
        void onValues(WifiStatus wifiStatus, RttResponder rttResponder);
    }

    @Override
    IHwBinder asBinder();

    WifiStatus disableResponder(int i) throws RemoteException;

    WifiStatus enableResponder(int i, WifiChannelInfo wifiChannelInfo, int i2, RttResponder rttResponder) throws RemoteException;

    void getBoundIface(getBoundIfaceCallback getboundifacecallback) throws RemoteException;

    void getCapabilities(getCapabilitiesCallback getcapabilitiescallback) throws RemoteException;

    @Override
    DebugInfo getDebugInfo() throws RemoteException;

    @Override
    ArrayList<byte[]> getHashChain() throws RemoteException;

    void getResponderInfo(getResponderInfoCallback getresponderinfocallback) throws RemoteException;

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

    WifiStatus rangeCancel(int i, ArrayList<byte[]> arrayList) throws RemoteException;

    WifiStatus rangeRequest(int i, ArrayList<RttConfig> arrayList) throws RemoteException;

    WifiStatus registerEventCallback(IWifiRttControllerEventCallback iWifiRttControllerEventCallback) throws RemoteException;

    @Override
    void setHALInstrumentation() throws RemoteException;

    WifiStatus setLci(int i, RttLciInformation rttLciInformation) throws RemoteException;

    WifiStatus setLcr(int i, RttLcrInformation rttLcrInformation) throws RemoteException;

    @Override
    boolean unlinkToDeath(IHwBinder.DeathRecipient deathRecipient) throws RemoteException;

    static IWifiRttController asInterface(IHwBinder iHwBinder) {
        if (iHwBinder == null) {
            return null;
        }
        IHwInterface iHwInterfaceQueryLocalInterface = iHwBinder.queryLocalInterface(kInterfaceName);
        if (iHwInterfaceQueryLocalInterface != null && (iHwInterfaceQueryLocalInterface instanceof IWifiRttController)) {
            return (IWifiRttController) iHwInterfaceQueryLocalInterface;
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

    static IWifiRttController castFrom(IHwInterface iHwInterface) {
        if (iHwInterface == null) {
            return null;
        }
        return asInterface(iHwInterface.asBinder());
    }

    static IWifiRttController getService(String str, boolean z) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, str, z));
    }

    static IWifiRttController getService(boolean z) throws RemoteException {
        return getService(HalDeviceManager.HAL_INSTANCE_NAME, z);
    }

    static IWifiRttController getService(String str) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, str));
    }

    static IWifiRttController getService() throws RemoteException {
        return getService(HalDeviceManager.HAL_INSTANCE_NAME);
    }

    public static final class Proxy implements IWifiRttController {
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
                return "[class or subclass of android.hardware.wifi@1.0::IWifiRttController]@Proxy";
            }
        }

        public final boolean equals(Object obj) {
            return HidlSupport.interfacesEqual(this, obj);
        }

        public final int hashCode() {
            return asBinder().hashCode();
        }

        @Override
        public void getBoundIface(getBoundIfaceCallback getboundifacecallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiRttController.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(1, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                WifiStatus wifiStatus = new WifiStatus();
                wifiStatus.readFromParcel(hwParcel2);
                getboundifacecallback.onValues(wifiStatus, IWifiIface.asInterface(hwParcel2.readStrongBinder()));
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public WifiStatus registerEventCallback(IWifiRttControllerEventCallback iWifiRttControllerEventCallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiRttController.kInterfaceName);
            hwParcel.writeStrongBinder(iWifiRttControllerEventCallback == null ? null : iWifiRttControllerEventCallback.asBinder());
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
        public WifiStatus rangeRequest(int i, ArrayList<RttConfig> arrayList) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiRttController.kInterfaceName);
            hwParcel.writeInt32(i);
            RttConfig.writeVectorToParcel(hwParcel, arrayList);
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
        public WifiStatus rangeCancel(int i, ArrayList<byte[]> arrayList) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiRttController.kInterfaceName);
            hwParcel.writeInt32(i);
            HwBlob hwBlob = new HwBlob(16);
            int size = arrayList.size();
            hwBlob.putInt32(8L, size);
            hwBlob.putBool(12L, false);
            HwBlob hwBlob2 = new HwBlob(size * 6);
            for (int i2 = 0; i2 < size; i2++) {
                hwBlob2.putInt8Array(i2 * 6, arrayList.get(i2));
            }
            hwBlob.putBlob(0L, hwBlob2);
            hwParcel.writeBuffer(hwBlob);
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
        public void getCapabilities(getCapabilitiesCallback getcapabilitiescallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiRttController.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(5, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                WifiStatus wifiStatus = new WifiStatus();
                wifiStatus.readFromParcel(hwParcel2);
                RttCapabilities rttCapabilities = new RttCapabilities();
                rttCapabilities.readFromParcel(hwParcel2);
                getcapabilitiescallback.onValues(wifiStatus, rttCapabilities);
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public WifiStatus setLci(int i, RttLciInformation rttLciInformation) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiRttController.kInterfaceName);
            hwParcel.writeInt32(i);
            rttLciInformation.writeToParcel(hwParcel);
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
        public WifiStatus setLcr(int i, RttLcrInformation rttLcrInformation) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiRttController.kInterfaceName);
            hwParcel.writeInt32(i);
            rttLcrInformation.writeToParcel(hwParcel);
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
        public void getResponderInfo(getResponderInfoCallback getresponderinfocallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiRttController.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(8, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                WifiStatus wifiStatus = new WifiStatus();
                wifiStatus.readFromParcel(hwParcel2);
                RttResponder rttResponder = new RttResponder();
                rttResponder.readFromParcel(hwParcel2);
                getresponderinfocallback.onValues(wifiStatus, rttResponder);
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public WifiStatus enableResponder(int i, WifiChannelInfo wifiChannelInfo, int i2, RttResponder rttResponder) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiRttController.kInterfaceName);
            hwParcel.writeInt32(i);
            wifiChannelInfo.writeToParcel(hwParcel);
            hwParcel.writeInt32(i2);
            rttResponder.writeToParcel(hwParcel);
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
        public WifiStatus disableResponder(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(IWifiRttController.kInterfaceName);
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

    public static abstract class Stub extends HwBinder implements IWifiRttController {
        @Override
        public IHwBinder asBinder() {
            return this;
        }

        @Override
        public final ArrayList<String> interfaceChain() {
            return new ArrayList<>(Arrays.asList(IWifiRttController.kInterfaceName, IBase.kInterfaceName));
        }

        @Override
        public final String interfaceDescriptor() {
            return IWifiRttController.kInterfaceName;
        }

        @Override
        public final ArrayList<byte[]> getHashChain() {
            return new ArrayList<>(Arrays.asList(new byte[]{118, 110, -105, 101, -11, -55, -57, 89, -78, -89, 99, -62, 40, -125, 83, -5, 93, -17, -13, 56, -100, 44, -62, -113, -127, -41, -100, -109, -105, 4, -50, -117}, new byte[]{-67, -38, -74, 24, 77, 122, 52, 109, -90, -96, 125, -64, -126, -116, -15, -102, 105, 111, 76, -86, 54, 17, -59, 31, 46, 20, 86, 90, 20, -76, 15, -39}));
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
            if (IWifiRttController.kInterfaceName.equals(str)) {
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
                        hwParcel.enforceInterface(IWifiRttController.kInterfaceName);
                        getBoundIface(new getBoundIfaceCallback() {
                            @Override
                            public void onValues(WifiStatus wifiStatus, IWifiIface iWifiIface) {
                                hwParcel2.writeStatus(0);
                                wifiStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeStrongBinder(iWifiIface == null ? null : iWifiIface.asBinder());
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
                        hwParcel.enforceInterface(IWifiRttController.kInterfaceName);
                        WifiStatus wifiStatusRegisterEventCallback = registerEventCallback(IWifiRttControllerEventCallback.asInterface(hwParcel.readStrongBinder()));
                        hwParcel2.writeStatus(0);
                        wifiStatusRegisterEventCallback.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 3:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiRttController.kInterfaceName);
                        WifiStatus wifiStatusRangeRequest = rangeRequest(hwParcel.readInt32(), RttConfig.readVectorFromParcel(hwParcel));
                        hwParcel2.writeStatus(0);
                        wifiStatusRangeRequest.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 4:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiRttController.kInterfaceName);
                        int int32 = hwParcel.readInt32();
                        ArrayList<byte[]> arrayList = new ArrayList<>();
                        HwBlob buffer = hwParcel.readBuffer(16L);
                        int int322 = buffer.getInt32(8L);
                        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int322 * 6, buffer.handle(), 0L, true);
                        arrayList.clear();
                        for (int i3 = 0; i3 < int322; i3++) {
                            byte[] bArr = new byte[6];
                            embeddedBuffer.copyToInt8Array(i3 * 6, bArr, 6);
                            arrayList.add(bArr);
                        }
                        WifiStatus wifiStatusRangeCancel = rangeCancel(int32, arrayList);
                        hwParcel2.writeStatus(0);
                        wifiStatusRangeCancel.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 5:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiRttController.kInterfaceName);
                        getCapabilities(new getCapabilitiesCallback() {
                            @Override
                            public void onValues(WifiStatus wifiStatus, RttCapabilities rttCapabilities) {
                                hwParcel2.writeStatus(0);
                                wifiStatus.writeToParcel(hwParcel2);
                                rttCapabilities.writeToParcel(hwParcel2);
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
                        hwParcel.enforceInterface(IWifiRttController.kInterfaceName);
                        int int323 = hwParcel.readInt32();
                        RttLciInformation rttLciInformation = new RttLciInformation();
                        rttLciInformation.readFromParcel(hwParcel);
                        WifiStatus lci = setLci(int323, rttLciInformation);
                        hwParcel2.writeStatus(0);
                        lci.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 7:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiRttController.kInterfaceName);
                        int int324 = hwParcel.readInt32();
                        RttLcrInformation rttLcrInformation = new RttLcrInformation();
                        rttLcrInformation.readFromParcel(hwParcel);
                        WifiStatus lcr = setLcr(int324, rttLcrInformation);
                        hwParcel2.writeStatus(0);
                        lcr.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 8:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiRttController.kInterfaceName);
                        getResponderInfo(new getResponderInfoCallback() {
                            @Override
                            public void onValues(WifiStatus wifiStatus, RttResponder rttResponder) {
                                hwParcel2.writeStatus(0);
                                wifiStatus.writeToParcel(hwParcel2);
                                rttResponder.writeToParcel(hwParcel2);
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
                        hwParcel.enforceInterface(IWifiRttController.kInterfaceName);
                        int int325 = hwParcel.readInt32();
                        WifiChannelInfo wifiChannelInfo = new WifiChannelInfo();
                        wifiChannelInfo.readFromParcel(hwParcel);
                        int int326 = hwParcel.readInt32();
                        RttResponder rttResponder = new RttResponder();
                        rttResponder.readFromParcel(hwParcel);
                        WifiStatus wifiStatusEnableResponder = enableResponder(int325, wifiChannelInfo, int326, rttResponder);
                        hwParcel2.writeStatus(0);
                        wifiStatusEnableResponder.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 10:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(IWifiRttController.kInterfaceName);
                        WifiStatus wifiStatusDisableResponder = disableResponder(hwParcel.readInt32());
                        hwParcel2.writeStatus(0);
                        wifiStatusDisableResponder.writeToParcel(hwParcel2);
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
