package vendor.mediatek.hardware.lbs.V1_0;

import android.hidl.base.V1_0.DebugInfo;
import android.hidl.base.V1_0.IBase;
import android.os.HidlSupport;
import android.os.HwBinder;
import android.os.HwBlob;
import android.os.HwParcel;
import android.os.IHwBinder;
import android.os.IHwInterface;
import android.os.RemoteException;
import com.android.settingslib.wifi.AccessPoint;
import java.util.ArrayList;
import java.util.Arrays;

public interface ILbsCallback extends IBase {
    IHwBinder asBinder();

    boolean callbackToClient(ArrayList<Byte> arrayList) throws RemoteException;

    public static abstract class Stub extends HwBinder implements ILbsCallback {
        @Override
        public IHwBinder asBinder() {
            return this;
        }

        public final ArrayList<String> interfaceChain() {
            return new ArrayList<>(Arrays.asList("vendor.mediatek.hardware.lbs@1.0::ILbsCallback", "android.hidl.base@1.0::IBase"));
        }

        public final String interfaceDescriptor() {
            return "vendor.mediatek.hardware.lbs@1.0::ILbsCallback";
        }

        public final ArrayList<byte[]> getHashChain() {
            return new ArrayList<>(Arrays.asList(new byte[]{-103, 126, 102, 25, -80, -75, 60, 123, -32, 80, -126, -107, -87, 67, 94, 121, 112, -64, 77, -103, -97, 76, -2, -128, 32, -62, -43, -63, -1, 14, -58, -84}, new byte[]{-67, -38, -74, 24, 77, 122, 52, 109, -90, -96, 125, -64, -126, -116, -15, -102, 105, 111, 76, -86, 54, 17, -59, 31, 46, 20, 86, 90, 20, -76, 15, -39}));
        }

        public final void setHALInstrumentation() {
        }

        public final boolean linkToDeath(IHwBinder.DeathRecipient deathRecipient, long j) {
            return true;
        }

        public final void ping() {
        }

        public final DebugInfo getDebugInfo() {
            DebugInfo debugInfo = new DebugInfo();
            debugInfo.pid = HidlSupport.getPidIfSharable();
            debugInfo.ptr = 0L;
            debugInfo.arch = 0;
            return debugInfo;
        }

        public final void notifySyspropsChanged() {
            HwBinder.enableInstrumentation();
        }

        public final boolean unlinkToDeath(IHwBinder.DeathRecipient deathRecipient) {
            return true;
        }

        public IHwInterface queryLocalInterface(String str) {
            if ("vendor.mediatek.hardware.lbs@1.0::ILbsCallback".equals(str)) {
                return this;
            }
            return null;
        }

        public String toString() {
            return interfaceDescriptor() + "@Stub";
        }

        public void onTransact(int i, HwParcel hwParcel, HwParcel hwParcel2, int i2) throws RemoteException {
            switch (i) {
                case 1:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(AccessPoint.UNREACHABLE_RSSI);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface("vendor.mediatek.hardware.lbs@1.0::ILbsCallback");
                        boolean zCallbackToClient = callbackToClient(hwParcel.readInt8Vector());
                        hwParcel2.writeStatus(0);
                        hwParcel2.writeBool(zCallbackToClient);
                        hwParcel2.send();
                    }
                    break;
                case 256067662:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(AccessPoint.UNREACHABLE_RSSI);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface("android.hidl.base@1.0::IBase");
                        ArrayList<String> arrayListInterfaceChain = interfaceChain();
                        hwParcel2.writeStatus(0);
                        hwParcel2.writeStringVector(arrayListInterfaceChain);
                        hwParcel2.send();
                    }
                    break;
                case 256131655:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(AccessPoint.UNREACHABLE_RSSI);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface("android.hidl.base@1.0::IBase");
                        hwParcel2.writeStatus(0);
                        hwParcel2.send();
                    }
                    break;
                case 256136003:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(AccessPoint.UNREACHABLE_RSSI);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface("android.hidl.base@1.0::IBase");
                        String strInterfaceDescriptor = interfaceDescriptor();
                        hwParcel2.writeStatus(0);
                        hwParcel2.writeString(strInterfaceDescriptor);
                        hwParcel2.send();
                    }
                    break;
                case 256398152:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(AccessPoint.UNREACHABLE_RSSI);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface("android.hidl.base@1.0::IBase");
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
                        hwParcel2.writeStatus(AccessPoint.UNREACHABLE_RSSI);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface("android.hidl.base@1.0::IBase");
                        setHALInstrumentation();
                    }
                    break;
                case 256660548:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(AccessPoint.UNREACHABLE_RSSI);
                        hwParcel2.send();
                    }
                    break;
                case 256921159:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(AccessPoint.UNREACHABLE_RSSI);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface("android.hidl.base@1.0::IBase");
                        ping();
                        hwParcel2.writeStatus(0);
                        hwParcel2.send();
                    }
                    break;
                case 257049926:
                    if ((i2 & 1) != 0) {
                        hwParcel2.writeStatus(AccessPoint.UNREACHABLE_RSSI);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface("android.hidl.base@1.0::IBase");
                        DebugInfo debugInfo = getDebugInfo();
                        hwParcel2.writeStatus(0);
                        debugInfo.writeToParcel(hwParcel2);
                        hwParcel2.send();
                    }
                    break;
                case 257120595:
                    if (((i2 & 1) != 0 ? 1 : 0) != 1) {
                        hwParcel2.writeStatus(AccessPoint.UNREACHABLE_RSSI);
                        hwParcel2.send();
                    } else {
                        hwParcel.enforceInterface("android.hidl.base@1.0::IBase");
                        notifySyspropsChanged();
                    }
                    break;
                case 257250372:
                    if (((i2 & 1) != 0 ? 1 : 0) != 0) {
                        hwParcel2.writeStatus(AccessPoint.UNREACHABLE_RSSI);
                        hwParcel2.send();
                    }
                    break;
            }
        }
    }
}
