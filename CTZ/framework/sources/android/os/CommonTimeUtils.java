package android.os;

import android.system.OsConstants;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Locale;

class CommonTimeUtils {
    public static final int ERROR = -1;
    public static final int ERROR_BAD_VALUE = -4;
    public static final int ERROR_DEAD_OBJECT = -7;
    public static final int SUCCESS = 0;
    private String mInterfaceDesc;
    private IBinder mRemote;

    public CommonTimeUtils(IBinder iBinder, String str) {
        this.mRemote = iBinder;
        this.mInterfaceDesc = str;
    }

    public int transactGetInt(int i, int i2) throws RemoteException {
        Parcel parcelObtain = Parcel.obtain();
        Parcel parcelObtain2 = Parcel.obtain();
        try {
            parcelObtain.writeInterfaceToken(this.mInterfaceDesc);
            this.mRemote.transact(i, parcelObtain, parcelObtain2, 0);
            if (parcelObtain2.readInt() == 0) {
                i2 = parcelObtain2.readInt();
            }
            return i2;
        } finally {
            parcelObtain2.recycle();
            parcelObtain.recycle();
        }
    }

    public int transactSetInt(int i, int i2) {
        Parcel parcelObtain = Parcel.obtain();
        Parcel parcelObtain2 = Parcel.obtain();
        try {
            parcelObtain.writeInterfaceToken(this.mInterfaceDesc);
            parcelObtain.writeInt(i2);
            this.mRemote.transact(i, parcelObtain, parcelObtain2, 0);
            return parcelObtain2.readInt();
        } catch (RemoteException e) {
            return -7;
        } finally {
            parcelObtain2.recycle();
            parcelObtain.recycle();
        }
    }

    public long transactGetLong(int i, long j) throws RemoteException {
        Parcel parcelObtain = Parcel.obtain();
        Parcel parcelObtain2 = Parcel.obtain();
        try {
            parcelObtain.writeInterfaceToken(this.mInterfaceDesc);
            this.mRemote.transact(i, parcelObtain, parcelObtain2, 0);
            if (parcelObtain2.readInt() == 0) {
                j = parcelObtain2.readLong();
            }
            return j;
        } finally {
            parcelObtain2.recycle();
            parcelObtain.recycle();
        }
    }

    public int transactSetLong(int i, long j) {
        Parcel parcelObtain = Parcel.obtain();
        Parcel parcelObtain2 = Parcel.obtain();
        try {
            parcelObtain.writeInterfaceToken(this.mInterfaceDesc);
            parcelObtain.writeLong(j);
            this.mRemote.transact(i, parcelObtain, parcelObtain2, 0);
            return parcelObtain2.readInt();
        } catch (RemoteException e) {
            return -7;
        } finally {
            parcelObtain2.recycle();
            parcelObtain.recycle();
        }
    }

    public String transactGetString(int i, String str) throws RemoteException {
        Parcel parcelObtain = Parcel.obtain();
        Parcel parcelObtain2 = Parcel.obtain();
        try {
            parcelObtain.writeInterfaceToken(this.mInterfaceDesc);
            this.mRemote.transact(i, parcelObtain, parcelObtain2, 0);
            if (parcelObtain2.readInt() == 0) {
                str = parcelObtain2.readString();
            }
            return str;
        } finally {
            parcelObtain2.recycle();
            parcelObtain.recycle();
        }
    }

    public int transactSetString(int i, String str) {
        Parcel parcelObtain = Parcel.obtain();
        Parcel parcelObtain2 = Parcel.obtain();
        try {
            parcelObtain.writeInterfaceToken(this.mInterfaceDesc);
            parcelObtain.writeString(str);
            this.mRemote.transact(i, parcelObtain, parcelObtain2, 0);
            return parcelObtain2.readInt();
        } catch (RemoteException e) {
            return -7;
        } finally {
            parcelObtain2.recycle();
            parcelObtain.recycle();
        }
    }

    public InetSocketAddress transactGetSockaddr(int i) throws RemoteException {
        InetSocketAddress inetSocketAddress;
        int i2;
        String str;
        Parcel parcelObtain = Parcel.obtain();
        Parcel parcelObtain2 = Parcel.obtain();
        try {
            parcelObtain.writeInterfaceToken(this.mInterfaceDesc);
            this.mRemote.transact(i, parcelObtain, parcelObtain2, 0);
            if (parcelObtain2.readInt() != 0) {
                inetSocketAddress = null;
            } else {
                int i3 = parcelObtain2.readInt();
                if (OsConstants.AF_INET == i3) {
                    int i4 = parcelObtain2.readInt();
                    int i5 = parcelObtain2.readInt();
                    str = String.format(Locale.US, "%d.%d.%d.%d", Integer.valueOf((i4 >> 24) & 255), Integer.valueOf((i4 >> 16) & 255), Integer.valueOf((i4 >> 8) & 255), Integer.valueOf(i4 & 255));
                    i2 = i5;
                } else if (OsConstants.AF_INET6 == i3) {
                    int i6 = parcelObtain2.readInt();
                    int i7 = parcelObtain2.readInt();
                    int i8 = parcelObtain2.readInt();
                    int i9 = parcelObtain2.readInt();
                    i2 = parcelObtain2.readInt();
                    parcelObtain2.readInt();
                    parcelObtain2.readInt();
                    str = String.format(Locale.US, "[%04X:%04X:%04X:%04X:%04X:%04X:%04X:%04X]", Integer.valueOf((i6 >> 16) & 65535), Integer.valueOf(i6 & 65535), Integer.valueOf((i7 >> 16) & 65535), Integer.valueOf(i7 & 65535), Integer.valueOf((i8 >> 16) & 65535), Integer.valueOf(i8 & 65535), Integer.valueOf((i9 >> 16) & 65535), Integer.valueOf(i9 & 65535));
                } else {
                    i2 = 0;
                    str = null;
                }
                if (str != null) {
                    inetSocketAddress = new InetSocketAddress(str, i2);
                }
            }
            return inetSocketAddress;
        } finally {
            parcelObtain2.recycle();
            parcelObtain.recycle();
        }
    }

    public int transactSetSockaddr(int i, InetSocketAddress inetSocketAddress) {
        int i2;
        Parcel parcelObtain = Parcel.obtain();
        Parcel parcelObtain2 = Parcel.obtain();
        try {
            parcelObtain.writeInterfaceToken(this.mInterfaceDesc);
            if (inetSocketAddress == null) {
                parcelObtain.writeInt(0);
            } else {
                parcelObtain.writeInt(1);
                InetAddress address = inetSocketAddress.getAddress();
                byte[] address2 = address.getAddress();
                int port = inetSocketAddress.getPort();
                if (address instanceof Inet4Address) {
                    int i3 = ((address2[1] & 255) << 16) | ((address2[0] & 255) << 24) | ((address2[2] & 255) << 8) | (address2[3] & 255);
                    parcelObtain.writeInt(OsConstants.AF_INET);
                    parcelObtain.writeInt(i3);
                    parcelObtain.writeInt(port);
                } else if (address instanceof Inet6Address) {
                    Inet6Address inet6Address = (Inet6Address) address;
                    parcelObtain.writeInt(OsConstants.AF_INET6);
                    for (int i4 = 0; i4 < 4; i4++) {
                        int i5 = i4 * 4;
                        parcelObtain.writeInt((address2[i5 + 3] & 255) | ((address2[i5 + 0] & 255) << 24) | ((address2[i5 + 1] & 255) << 16) | ((address2[i5 + 2] & 255) << 8));
                    }
                    parcelObtain.writeInt(port);
                    parcelObtain.writeInt(0);
                    parcelObtain.writeInt(inet6Address.getScopeId());
                } else {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                    return -4;
                }
            }
            this.mRemote.transact(i, parcelObtain, parcelObtain2, 0);
            i2 = parcelObtain2.readInt();
        } catch (RemoteException e) {
            i2 = -7;
        } catch (Throwable th) {
            parcelObtain2.recycle();
            parcelObtain.recycle();
            throw th;
        }
        parcelObtain2.recycle();
        parcelObtain.recycle();
        return i2;
    }
}
