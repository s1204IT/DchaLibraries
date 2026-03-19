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

public interface ISupplicant extends IBase {
    public static final String kInterfaceName = "android.hardware.wifi.supplicant@1.0::ISupplicant";

    @FunctionalInterface
    public interface getInterfaceCallback {
        void onValues(SupplicantStatus supplicantStatus, ISupplicantIface iSupplicantIface);
    }

    @FunctionalInterface
    public interface listInterfacesCallback {
        void onValues(SupplicantStatus supplicantStatus, ArrayList<IfaceInfo> arrayList);
    }

    @Override
    IHwBinder asBinder();

    @Override
    DebugInfo getDebugInfo() throws RemoteException;

    int getDebugLevel() throws RemoteException;

    @Override
    ArrayList<byte[]> getHashChain() throws RemoteException;

    void getInterface(IfaceInfo ifaceInfo, getInterfaceCallback getinterfacecallback) throws RemoteException;

    @Override
    ArrayList<String> interfaceChain() throws RemoteException;

    @Override
    String interfaceDescriptor() throws RemoteException;

    boolean isDebugShowKeysEnabled() throws RemoteException;

    boolean isDebugShowTimestampEnabled() throws RemoteException;

    @Override
    boolean linkToDeath(IHwBinder.DeathRecipient deathRecipient, long j) throws RemoteException;

    void listInterfaces(listInterfacesCallback listinterfacescallback) throws RemoteException;

    @Override
    void notifySyspropsChanged() throws RemoteException;

    @Override
    void ping() throws RemoteException;

    SupplicantStatus registerCallback(ISupplicantCallback iSupplicantCallback) throws RemoteException;

    SupplicantStatus setConcurrencyPriority(int i) throws RemoteException;

    SupplicantStatus setDebugParams(int i, boolean z, boolean z2) throws RemoteException;

    @Override
    void setHALInstrumentation() throws RemoteException;

    @Override
    boolean unlinkToDeath(IHwBinder.DeathRecipient deathRecipient) throws RemoteException;

    static ISupplicant asInterface(IHwBinder iHwBinder) {
        if (iHwBinder == null) {
            return null;
        }
        IHwInterface iHwInterfaceQueryLocalInterface = iHwBinder.queryLocalInterface(kInterfaceName);
        if (iHwInterfaceQueryLocalInterface != null && (iHwInterfaceQueryLocalInterface instanceof ISupplicant)) {
            return (ISupplicant) iHwInterfaceQueryLocalInterface;
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

    static ISupplicant castFrom(IHwInterface iHwInterface) {
        if (iHwInterface == null) {
            return null;
        }
        return asInterface(iHwInterface.asBinder());
    }

    static ISupplicant getService(String str, boolean z) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, str, z));
    }

    static ISupplicant getService(boolean z) throws RemoteException {
        return getService(HalDeviceManager.HAL_INSTANCE_NAME, z);
    }

    static ISupplicant getService(String str) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, str));
    }

    static ISupplicant getService() throws RemoteException {
        return getService(HalDeviceManager.HAL_INSTANCE_NAME);
    }

    public static final class DebugLevel {
        public static final int DEBUG = 2;
        public static final int ERROR = 5;
        public static final int EXCESSIVE = 0;
        public static final int INFO = 3;
        public static final int MSGDUMP = 1;
        public static final int WARNING = 4;

        public static final String toString(int i) {
            if (i == 0) {
                return "EXCESSIVE";
            }
            if (i == 1) {
                return "MSGDUMP";
            }
            if (i == 2) {
                return "DEBUG";
            }
            if (i == 3) {
                return "INFO";
            }
            if (i == 4) {
                return "WARNING";
            }
            if (i == 5) {
                return "ERROR";
            }
            return "0x" + Integer.toHexString(i);
        }

        public static final String dumpBitfield(int i) {
            ArrayList arrayList = new ArrayList();
            arrayList.add("EXCESSIVE");
            int i2 = 1;
            if ((i & 1) == 1) {
                arrayList.add("MSGDUMP");
            } else {
                i2 = 0;
            }
            if ((i & 2) == 2) {
                arrayList.add("DEBUG");
                i2 |= 2;
            }
            if ((i & 3) == 3) {
                arrayList.add("INFO");
                i2 |= 3;
            }
            if ((i & 4) == 4) {
                arrayList.add("WARNING");
                i2 |= 4;
            }
            if ((i & 5) == 5) {
                arrayList.add("ERROR");
                i2 |= 5;
            }
            if (i != i2) {
                arrayList.add("0x" + Integer.toHexString(i & (~i2)));
            }
            return String.join(" | ", arrayList);
        }
    }

    public static final class IfaceInfo {
        public String name = new String();
        public int type;

        public final boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || obj.getClass() != IfaceInfo.class) {
                return false;
            }
            IfaceInfo ifaceInfo = (IfaceInfo) obj;
            if (this.type == ifaceInfo.type && HidlSupport.deepEquals(this.name, ifaceInfo.name)) {
                return true;
            }
            return false;
        }

        public final int hashCode() {
            return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.type))), Integer.valueOf(HidlSupport.deepHashCode(this.name)));
        }

        public final String toString() {
            return "{.type = " + IfaceType.toString(this.type) + ", .name = " + this.name + "}";
        }

        public final void readFromParcel(HwParcel hwParcel) {
            readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(24L), 0L);
        }

        public static final ArrayList<IfaceInfo> readVectorFromParcel(HwParcel hwParcel) {
            ArrayList<IfaceInfo> arrayList = new ArrayList<>();
            HwBlob buffer = hwParcel.readBuffer(16L);
            int int32 = buffer.getInt32(8L);
            HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 24, buffer.handle(), 0L, true);
            arrayList.clear();
            for (int i = 0; i < int32; i++) {
                IfaceInfo ifaceInfo = new IfaceInfo();
                ifaceInfo.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 24);
                arrayList.add(ifaceInfo);
            }
            return arrayList;
        }

        public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
            this.type = hwBlob.getInt32(j + 0);
            long j2 = j + 8;
            this.name = hwBlob.getString(j2);
            hwParcel.readEmbeddedBuffer(this.name.getBytes().length + 1, hwBlob.handle(), j2 + 0, false);
        }

        public final void writeToParcel(HwParcel hwParcel) {
            HwBlob hwBlob = new HwBlob(24);
            writeEmbeddedToBlob(hwBlob, 0L);
            hwParcel.writeBuffer(hwBlob);
        }

        public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<IfaceInfo> arrayList) {
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
            hwBlob.putInt32(0 + j, this.type);
            hwBlob.putString(j + 8, this.name);
        }
    }

    public static final class Proxy implements ISupplicant {
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
                return "[class or subclass of android.hardware.wifi.supplicant@1.0::ISupplicant]@Proxy";
            }
        }

        public final boolean equals(Object obj) {
            return HidlSupport.interfacesEqual(this, obj);
        }

        public final int hashCode() {
            return asBinder().hashCode();
        }

        @Override
        public void getInterface(IfaceInfo ifaceInfo, getInterfaceCallback getinterfacecallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicant.kInterfaceName);
            ifaceInfo.writeToParcel(hwParcel);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(1, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                getinterfacecallback.onValues(supplicantStatus, ISupplicantIface.asInterface(hwParcel2.readStrongBinder()));
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public void listInterfaces(listInterfacesCallback listinterfacescallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicant.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(2, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                SupplicantStatus supplicantStatus = new SupplicantStatus();
                supplicantStatus.readFromParcel(hwParcel2);
                listinterfacescallback.onValues(supplicantStatus, IfaceInfo.readVectorFromParcel(hwParcel2));
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus registerCallback(ISupplicantCallback iSupplicantCallback) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicant.kInterfaceName);
            hwParcel.writeStrongBinder(iSupplicantCallback == null ? null : iSupplicantCallback.asBinder());
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(3, hwParcel, hwParcel2, 0);
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
        public SupplicantStatus setDebugParams(int i, boolean z, boolean z2) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicant.kInterfaceName);
            hwParcel.writeInt32(i);
            hwParcel.writeBool(z);
            hwParcel.writeBool(z2);
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
        public int getDebugLevel() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicant.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(5, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readInt32();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public boolean isDebugShowTimestampEnabled() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicant.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(6, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readBool();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public boolean isDebugShowKeysEnabled() throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicant.kInterfaceName);
            HwParcel hwParcel2 = new HwParcel();
            try {
                this.mRemote.transact(7, hwParcel, hwParcel2, 0);
                hwParcel2.verifySuccess();
                hwParcel.releaseTemporaryStorage();
                return hwParcel2.readBool();
            } finally {
                hwParcel2.release();
            }
        }

        @Override
        public SupplicantStatus setConcurrencyPriority(int i) throws RemoteException {
            HwParcel hwParcel = new HwParcel();
            hwParcel.writeInterfaceToken(ISupplicant.kInterfaceName);
            hwParcel.writeInt32(i);
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

    public static abstract class Stub extends HwBinder implements ISupplicant {
        @Override
        public IHwBinder asBinder() {
            return this;
        }

        @Override
        public final ArrayList<String> interfaceChain() {
            return new ArrayList<>(Arrays.asList(ISupplicant.kInterfaceName, IBase.kInterfaceName));
        }

        @Override
        public final String interfaceDescriptor() {
            return ISupplicant.kInterfaceName;
        }

        @Override
        public final ArrayList<byte[]> getHashChain() {
            return new ArrayList<>(Arrays.asList(new byte[]{-9, -27, 92, 8, 24, 125, -116, -123, 80, 104, -95, -18, 61, 12, -115, -82, -18, 117, 112, 41, 45, -106, 80, -100, 33, -88, 117, 109, 79, 92, -5, -101}, new byte[]{-67, -38, -74, 24, 77, 122, 52, 109, -90, -96, 125, -64, -126, -116, -15, -102, 105, 111, 76, -86, 54, 17, -59, 31, 46, 20, 86, 90, 20, -76, 15, -39}));
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
            if (ISupplicant.kInterfaceName.equals(str)) {
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
                        hwParcel.enforceInterface(ISupplicant.kInterfaceName);
                        IfaceInfo ifaceInfo = new IfaceInfo();
                        ifaceInfo.readFromParcel(hwParcel);
                        getInterface(ifaceInfo, new getInterfaceCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, ISupplicantIface iSupplicantIface) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                hwParcel2.writeStrongBinder(iSupplicantIface == null ? null : iSupplicantIface.asBinder());
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
                        hwParcel.enforceInterface(ISupplicant.kInterfaceName);
                        listInterfaces(new listInterfacesCallback() {
                            @Override
                            public void onValues(SupplicantStatus supplicantStatus, ArrayList<IfaceInfo> arrayList) {
                                hwParcel2.writeStatus(0);
                                supplicantStatus.writeToParcel(hwParcel2);
                                IfaceInfo.writeVectorToParcel(hwParcel2, arrayList);
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
                        hwParcel.enforceInterface(ISupplicant.kInterfaceName);
                        SupplicantStatus supplicantStatusRegisterCallback = registerCallback(ISupplicantCallback.asInterface(hwParcel.readStrongBinder()));
                        hwParcel2.writeStatus(0);
                        supplicantStatusRegisterCallback.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 4:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicant.kInterfaceName);
                        SupplicantStatus debugParams = setDebugParams(hwParcel.readInt32(), hwParcel.readBool(), hwParcel.readBool());
                        hwParcel2.writeStatus(0);
                        debugParams.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 5:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicant.kInterfaceName);
                        int debugLevel = getDebugLevel();
                        hwParcel2.writeStatus(0);
                        hwParcel2.writeInt32(debugLevel);
                        hwParcel2.send();
                    }
                    break;
                case 6:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicant.kInterfaceName);
                        boolean zIsDebugShowTimestampEnabled = isDebugShowTimestampEnabled();
                        hwParcel2.writeStatus(0);
                        hwParcel2.writeBool(zIsDebugShowTimestampEnabled);
                        hwParcel2.send();
                    }
                    break;
                case 7:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicant.kInterfaceName);
                        boolean zIsDebugShowKeysEnabled = isDebugShowKeysEnabled();
                        hwParcel2.writeStatus(0);
                        hwParcel2.writeBool(zIsDebugShowKeysEnabled);
                        hwParcel2.send();
                    }
                    break;
                case 8:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(Integer.MIN_VALUE);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface(ISupplicant.kInterfaceName);
                        SupplicantStatus concurrencyPriority = setConcurrencyPriority(hwParcel.readInt32());
                        hwParcel2.writeStatus(0);
                        concurrencyPriority.writeToParcel(hwParcel2);
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
