package android.os;

import android.os.BatteryStats;
import android.system.OsConstants;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.util.Locale;

class CommonTimeUtils {
    public static final int ERROR = -1;
    public static final int ERROR_BAD_VALUE = -4;
    public static final int ERROR_DEAD_OBJECT = -7;
    public static final int SUCCESS = 0;
    private String mInterfaceDesc;
    private IBinder mRemote;

    public CommonTimeUtils(IBinder remote, String interfaceDesc) {
        this.mRemote = remote;
        this.mInterfaceDesc = interfaceDesc;
    }

    public int transactGetInt(int method_code, int error_ret_val) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(this.mInterfaceDesc);
            this.mRemote.transact(method_code, data, reply, 0);
            int res = reply.readInt();
            int ret_val = res == 0 ? reply.readInt() : error_ret_val;
            return ret_val;
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    public int transactSetInt(int method_code, int val) {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(this.mInterfaceDesc);
            data.writeInt(val);
            this.mRemote.transact(method_code, data, reply, 0);
            return reply.readInt();
        } catch (RemoteException e) {
            return -7;
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    public long transactGetLong(int method_code, long error_ret_val) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(this.mInterfaceDesc);
            this.mRemote.transact(method_code, data, reply, 0);
            int res = reply.readInt();
            long ret_val = res == 0 ? reply.readLong() : error_ret_val;
            return ret_val;
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    public int transactSetLong(int method_code, long val) {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(this.mInterfaceDesc);
            data.writeLong(val);
            this.mRemote.transact(method_code, data, reply, 0);
            return reply.readInt();
        } catch (RemoteException e) {
            return -7;
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    public String transactGetString(int method_code, String error_ret_val) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(this.mInterfaceDesc);
            this.mRemote.transact(method_code, data, reply, 0);
            int res = reply.readInt();
            String ret_val = res == 0 ? reply.readString() : error_ret_val;
            return ret_val;
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    public int transactSetString(int method_code, String val) {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(this.mInterfaceDesc);
            data.writeString(val);
            this.mRemote.transact(method_code, data, reply, 0);
            return reply.readInt();
        } catch (RemoteException e) {
            return -7;
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    public InetSocketAddress transactGetSockaddr(int method_code) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        InetSocketAddress ret_val = null;
        try {
            data.writeInterfaceToken(this.mInterfaceDesc);
            this.mRemote.transact(method_code, data, reply, 0);
            int res = reply.readInt();
            if (res == 0) {
                int port = 0;
                String addrStr = null;
                int type = reply.readInt();
                if (OsConstants.AF_INET == type) {
                    int addr = reply.readInt();
                    port = reply.readInt();
                    addrStr = String.format(Locale.US, "%d.%d.%d.%d", Integer.valueOf((addr >> 24) & 255), Integer.valueOf((addr >> 16) & 255), Integer.valueOf((addr >> 8) & 255), Integer.valueOf(addr & 255));
                } else if (OsConstants.AF_INET6 == type) {
                    int addr1 = reply.readInt();
                    int addr2 = reply.readInt();
                    int addr3 = reply.readInt();
                    int addr4 = reply.readInt();
                    port = reply.readInt();
                    reply.readInt();
                    reply.readInt();
                    addrStr = String.format(Locale.US, "[%04X:%04X:%04X:%04X:%04X:%04X:%04X:%04X]", Integer.valueOf((addr1 >> 16) & 65535), Integer.valueOf(65535 & addr1), Integer.valueOf((addr2 >> 16) & 65535), Integer.valueOf(65535 & addr2), Integer.valueOf((addr3 >> 16) & 65535), Integer.valueOf(65535 & addr3), Integer.valueOf((addr4 >> 16) & 65535), Integer.valueOf(65535 & addr4));
                }
                if (addrStr != null) {
                    ret_val = new InetSocketAddress(addrStr, port);
                }
            }
            return ret_val;
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    public int transactSetSockaddr(int method_code, InetSocketAddress addr) {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(this.mInterfaceDesc);
            if (addr == null) {
                data.writeInt(0);
            } else {
                data.writeInt(1);
                ?? address = addr.getAddress();
                byte[] b = address.getAddress();
                int p = addr.getPort();
                if (address instanceof Inet4Address) {
                    int v4addr = ((b[0] & BatteryStats.HistoryItem.CMD_NULL) << 24) | ((b[1] & BatteryStats.HistoryItem.CMD_NULL) << 16) | ((b[2] & BatteryStats.HistoryItem.CMD_NULL) << 8) | (b[3] & BatteryStats.HistoryItem.CMD_NULL);
                    data.writeInt(OsConstants.AF_INET);
                    data.writeInt(v4addr);
                    data.writeInt(p);
                } else {
                    if (!(address instanceof Inet6Address)) {
                        return -4;
                    }
                    data.writeInt(OsConstants.AF_INET6);
                    for (int i = 0; i < 4; i++) {
                        int aword = ((b[(i * 4) + 0] & BatteryStats.HistoryItem.CMD_NULL) << 24) | ((b[(i * 4) + 1] & BatteryStats.HistoryItem.CMD_NULL) << 16) | ((b[(i * 4) + 2] & BatteryStats.HistoryItem.CMD_NULL) << 8) | (b[(i * 4) + 3] & BatteryStats.HistoryItem.CMD_NULL);
                        data.writeInt(aword);
                    }
                    data.writeInt(p);
                    data.writeInt(0);
                    data.writeInt(address.getScopeId());
                }
            }
            this.mRemote.transact(method_code, data, reply, 0);
            int ret_val = reply.readInt();
            return ret_val;
        } catch (RemoteException e) {
            return -7;
        } finally {
            reply.recycle();
            data.recycle();
        }
    }
}
