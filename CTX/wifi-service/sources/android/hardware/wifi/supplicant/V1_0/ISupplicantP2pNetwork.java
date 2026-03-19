package android.hardware.wifi.supplicant.V1_0;

import android.hardware.wifi.supplicant.V1_0.ISupplicantNetwork;
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

public interface ISupplicantP2pNetwork extends ISupplicantNetwork {
    public static final String kInterfaceName = "android.hardware.wifi.supplicant@1.0::ISupplicantP2pNetwork";

    @FunctionalInterface
    public interface getBssidCallback {
        void onValues(SupplicantStatus supplicantStatus, byte[] bArr);
    }

    @FunctionalInterface
    public interface getClientListCallback {
        void onValues(SupplicantStatus supplicantStatus, ArrayList<byte[]> arrayList);
    }

    @FunctionalInterface
    public interface getSsidCallback {
        void onValues(SupplicantStatus supplicantStatus, ArrayList<Byte> arrayList);
    }

    @FunctionalInterface
    public interface isCurrentCallback {
        void onValues(SupplicantStatus supplicantStatus, boolean z);
    }

    @FunctionalInterface
    public interface isGoCallback {
        void onValues(SupplicantStatus supplicantStatus, boolean z);
    }

    @FunctionalInterface
    public interface isPersistentCallback {
        void onValues(SupplicantStatus supplicantStatus, boolean z);
    }

    @Override
    IHwBinder asBinder();

    void getBssid(getBssidCallback getbssidcallback) throws RemoteException;

    void getClientList(getClientListCallback getclientlistcallback) throws RemoteException;

    @Override
    DebugInfo getDebugInfo() throws RemoteException;

    @Override
    ArrayList<byte[]> getHashChain() throws RemoteException;

    void getSsid(getSsidCallback getssidcallback) throws RemoteException;

    @Override
    ArrayList<String> interfaceChain() throws RemoteException;

    @Override
    String interfaceDescriptor() throws RemoteException;

    void isCurrent(isCurrentCallback iscurrentcallback) throws RemoteException;

    void isGo(isGoCallback isgocallback) throws RemoteException;

    void isPersistent(isPersistentCallback ispersistentcallback) throws RemoteException;

    @Override
    boolean linkToDeath(IHwBinder.DeathRecipient deathRecipient, long j) throws RemoteException;

    @Override
    void notifySyspropsChanged() throws RemoteException;

    @Override
    void ping() throws RemoteException;

    SupplicantStatus registerCallback(ISupplicantP2pNetworkCallback iSupplicantP2pNetworkCallback) throws RemoteException;

    SupplicantStatus setClientList(ArrayList<byte[]> arrayList) throws RemoteException;

    @Override
    void setHALInstrumentation() throws RemoteException;

    @Override
    boolean unlinkToDeath(IHwBinder.DeathRecipient deathRecipient) throws RemoteException;

    static ISupplicantP2pNetwork asInterface(IHwBinder iHwBinder) {
        if (iHwBinder == null) {
            return null;
        }
        IHwInterface iHwInterfaceQueryLocalInterface = iHwBinder.queryLocalInterface(kInterfaceName);
        if (iHwInterfaceQueryLocalInterface != null && (iHwInterfaceQueryLocalInterface instanceof ISupplicantP2pNetwork)) {
            return (ISupplicantP2pNetwork) iHwInterfaceQueryLocalInterface;
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

    static ISupplicantP2pNetwork castFrom(IHwInterface iHwInterface) {
        if (iHwInterface == null) {
            return null;
        }
        return asInterface(iHwInterface.asBinder());
    }

    static ISupplicantP2pNetwork getService(String str, boolean z) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, str, z));
    }

    static ISupplicantP2pNetwork getService(boolean z) throws RemoteException {
        return getService(HalDeviceManager.HAL_INSTANCE_NAME, z);
    }

    static ISupplicantP2pNetwork getService(String str) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, str));
    }

    static ISupplicantP2pNetwork getService() throws RemoteException {
        return getService(HalDeviceManager.HAL_INSTANCE_NAME);
    }

    public static final class Proxy implements ISupplicantP2pNetwork {
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
                return "[class or subclass of android.hardware.wifi.supplicant@1.0::ISupplicantP2pNetwork]@Proxy";
            }
        }

        public final boolean equals(Object obj) {
            return HidlSupport.interfacesEqual(this, obj);
        }

        public final int hashCode() {
            return asBinder().hashCode();
        }

        @Override
        public void getId(ISupplicantNetwork.getIdCallback getidcallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(1, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                getidcallback.onValues(supplicantStatus, hwParcel2.readInt32());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getInterfaceName(ISupplicantNetwork.getInterfaceNameCallback getinterfacenamecallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(2, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                getinterfacenamecallback.onValues(supplicantStatus, hwParcel2.readString());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getType(ISupplicantNetwork.getTypeCallback gettypecallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(3, hwParcel, hwParcel2, 0);
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
        public SupplicantStatus registerCallback(ISupplicantP2pNetworkCallback iSupplicantP2pNetworkCallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantP2pNetwork.kInterfaceName);
            hwParcel.writeStrongBinder(iSupplicantP2pNetworkCallback == null ? null : iSupplicantP2pNetworkCallback.asBinder());
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
        public void getSsid(getSsidCallback getssidcallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantP2pNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(5, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                getssidcallback.onValues(supplicantStatus, hwParcel2.readInt8Vector());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void getBssid(getBssidCallback getbssidcallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantP2pNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(6, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                byte[] bArr = new byte[6];
                hwParcel2.readBuffer(6L).copyToInt8Array(0L, bArr, 6);
                getbssidcallback.onValues(supplicantStatus, bArr);
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void isCurrent(isCurrentCallback iscurrentcallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantP2pNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(7, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                iscurrentcallback.onValues(supplicantStatus, hwParcel2.readBool());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void isPersistent(isPersistentCallback ispersistentcallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantP2pNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(8, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                ispersistentcallback.onValues(supplicantStatus, hwParcel2.readBool());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void isGo(isGoCallback isgocallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantP2pNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(9, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                isgocallback.onValues(supplicantStatus, hwParcel2.readBool());
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setClientList(ArrayList<byte[]> arrayList) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantP2pNetwork.kInterfaceName);
            HwBlob hwBlob = new HwBlob(16);
            int size = arrayList.size();
            hwBlob.putInt32(8L, size);
            hwBlob.putBool(12L, false);
            HwBlob hwBlob2 = new HwBlob(size * 6);
            for (int i = 0; i < size; i++) {
                hwBlob2.putInt8Array(i * 6, arrayList.get(i));
            }
            hwBlob.putBlob(0L, hwBlob2);
            hwParcel.writeBuffer(hwBlob);
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
        public void getClientList(getClientListCallback getclientlistcallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicantP2pNetwork.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(11, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                ArrayList<byte[]> arrayList = new ArrayList<>();
                HwBlob buffer = hwParcel2.readBuffer(16L);
                int int32 = buffer.getInt32(8L);
                HwBlob embeddedBuffer = hwParcel2.readEmbeddedBuffer(int32 * 6, buffer.handle(), 0L, true);
                arrayList.clear();
                for (int i = 0; i < int32; i++) {
                    byte[] bArr = new byte[6];
                    embeddedBuffer.copyToInt8Array(i * 6, bArr, 6);
                    arrayList.add(bArr);
                }
                getclientlistcallback.onValues(supplicantStatus, arrayList);
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

    public static abstract class Stub extends HwBinder implements ISupplicantP2pNetwork {
        @Override
        public IHwBinder asBinder() {
            return this;
        }

        @Override
        public final ArrayList<String> interfaceChain() {
            return new ArrayList<>(Arrays.asList(ISupplicantP2pNetwork.kInterfaceName, ISupplicantNetwork.kInterfaceName, IBase.kInterfaceName));
        }

        @Override
        public final String interfaceDescriptor() {
            return ISupplicantP2pNetwork.kInterfaceName;
        }

        @Override
        public final ArrayList<byte[]> getHashChain() {
            return new ArrayList<>(Arrays.asList(new byte[]{86, 18, -113, 116, 86, 5, 113, -74, 119, 125, 89, 69, 63, 53, -58, -77, 86, -109, -18, 55, 126, 42, 35, -56, 7, 112, -119, 6, -110, -113, 9, -34}, new byte[]{-51, -96, 16, 8, -64, 105, 34, -6, 55, -63, 33, 62, -101, -72, 49, -95, 9, -77, 23, 69, 50, -128, 86, 22, -5, 113, 97, -19, -60, 3, -122, 111}, new byte[]{-67, -38, -74, 24, 77, 122, 52, 109, -90, -96, 125, -64, -126, -116, -15, -102, 105, 111, 76, -86, 54, 17, -59, 31, 46, 20, 86, 90, 20, -76, 15, -39}));
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
            if (ISupplicantP2pNetwork.kInterfaceName.equals(str)) {
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
                        hwParcel.enforceInterface(ISupplicantNetwork.kInterfaceName);
                        getId(new ISupplicantNetwork.getIdCallback() {
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
                case 2:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantNetwork.kInterfaceName);
                        getInterfaceName(new ISupplicantNetwork.getInterfaceNameCallback() {
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
                case 3:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantNetwork.kInterfaceName);
                        getType(new ISupplicantNetwork.getTypeCallback() {
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
                case 4:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantP2pNetwork.kInterfaceName);
                        SupplicantStatus supplicantStatusRegisterCallback = registerCallback(ISupplicantP2pNetworkCallback.asInterface(hwParcel.readStrongBinder()));
                        hwParcel2.writeStatus(0);
                        supplicantStatusRegisterCallback.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 5:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantP2pNetwork.kInterfaceName);
                        getSsid(new getSsidCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, ArrayList<Byte> arrayList) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeInt8Vector(arrayList);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 6:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantP2pNetwork.kInterfaceName);
                        getBssid(new getBssidCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, byte[] bArr) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                HwBlob hwBlob = new HwBlob(6);
                                hwBlob.putInt8Array(0L, bArr);
                                hwParcel2.writeBuffer(hwBlob);
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
                        hwParcel.enforceInterface(ISupplicantP2pNetwork.kInterfaceName);
                        isCurrent(new isCurrentCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, boolean z) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeBool(z);
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
                        hwParcel.enforceInterface(ISupplicantP2pNetwork.kInterfaceName);
                        isPersistent(new isPersistentCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, boolean z) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeBool(z);
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
                        hwParcel.enforceInterface(ISupplicantP2pNetwork.kInterfaceName);
                        isGo(new isGoCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, boolean z) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeBool(z);
                                hwParcel2.send();
                            }
                        });
                    }
                    break;
                case 10:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantP2pNetwork.kInterfaceName);
                        ArrayList<byte[]> arrayList = new ArrayList<>();
                        HwBlob buffer = hwParcel.readBuffer(16L);
                        int int32 = buffer.getInt32(8L);
                        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 6, buffer.handle(), 0L, true);
                        arrayList.clear();
                        for (int i3 = 0; i3 < int32; i3++) {
                            byte[] bArr = new byte[6];
                            embeddedBuffer.copyToInt8Array(i3 * 6, bArr, 6);
                            arrayList.add(bArr);
                        }
                        SupplicantStatus clientList = setClientList(arrayList);
                        hwParcel2.writeStatus(0);
                        clientList.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 11:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicantP2pNetwork.kInterfaceName);
                        getClientList(new getClientListCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, ArrayList<byte[]> arrayList2) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                HwBlob hwBlob = new HwBlob(16);
                                int size = arrayList2.size();
                                hwBlob.putInt32(8L, size);
                                hwBlob.putBool(12L, false);
                                HwBlob hwBlob2 = new HwBlob(size * 6);
                                for (int i4 = 0; i4 < size; i4++) {
                                    hwBlob2.putInt8Array(i4 * 6, arrayList2.get(i4));
                                }
                                hwBlob.putBlob(0L, hwBlob2);
                                hwParcel2.writeBuffer(hwBlob);
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
