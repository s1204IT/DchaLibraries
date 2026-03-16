package android.service.carrier;

import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.service.carrier.ICarrierMessagingCallback;
import java.util.List;

public interface ICarrierMessagingService extends IInterface {
    void downloadMms(Uri uri, int i, Uri uri2, ICarrierMessagingCallback iCarrierMessagingCallback) throws RemoteException;

    void filterSms(MessagePdu messagePdu, String str, int i, int i2, ICarrierMessagingCallback iCarrierMessagingCallback) throws RemoteException;

    void sendDataSms(byte[] bArr, int i, String str, int i2, ICarrierMessagingCallback iCarrierMessagingCallback) throws RemoteException;

    void sendMms(Uri uri, int i, Uri uri2, ICarrierMessagingCallback iCarrierMessagingCallback) throws RemoteException;

    void sendMultipartTextSms(List<String> list, int i, String str, ICarrierMessagingCallback iCarrierMessagingCallback) throws RemoteException;

    void sendTextSms(String str, int i, String str2, ICarrierMessagingCallback iCarrierMessagingCallback) throws RemoteException;

    public static abstract class Stub extends Binder implements ICarrierMessagingService {
        private static final String DESCRIPTOR = "android.service.carrier.ICarrierMessagingService";
        static final int TRANSACTION_downloadMms = 6;
        static final int TRANSACTION_filterSms = 1;
        static final int TRANSACTION_sendDataSms = 3;
        static final int TRANSACTION_sendMms = 5;
        static final int TRANSACTION_sendMultipartTextSms = 4;
        static final int TRANSACTION_sendTextSms = 2;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ICarrierMessagingService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof ICarrierMessagingService)) {
                return (ICarrierMessagingService) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            Uri _arg0;
            Uri _arg2;
            Uri _arg02;
            Uri _arg22;
            MessagePdu _arg03;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg03 = MessagePdu.CREATOR.createFromParcel(data);
                    } else {
                        _arg03 = null;
                    }
                    String _arg1 = data.readString();
                    int _arg23 = data.readInt();
                    int _arg3 = data.readInt();
                    ICarrierMessagingCallback _arg4 = ICarrierMessagingCallback.Stub.asInterface(data.readStrongBinder());
                    filterSms(_arg03, _arg1, _arg23, _arg3, _arg4);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg04 = data.readString();
                    int _arg12 = data.readInt();
                    String _arg24 = data.readString();
                    ICarrierMessagingCallback _arg32 = ICarrierMessagingCallback.Stub.asInterface(data.readStrongBinder());
                    sendTextSms(_arg04, _arg12, _arg24, _arg32);
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    byte[] _arg05 = data.createByteArray();
                    int _arg13 = data.readInt();
                    String _arg25 = data.readString();
                    int _arg33 = data.readInt();
                    ICarrierMessagingCallback _arg42 = ICarrierMessagingCallback.Stub.asInterface(data.readStrongBinder());
                    sendDataSms(_arg05, _arg13, _arg25, _arg33, _arg42);
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    List<String> _arg06 = data.createStringArrayList();
                    int _arg14 = data.readInt();
                    String _arg26 = data.readString();
                    ICarrierMessagingCallback _arg34 = ICarrierMessagingCallback.Stub.asInterface(data.readStrongBinder());
                    sendMultipartTextSms(_arg06, _arg14, _arg26, _arg34);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg02 = Uri.CREATOR.createFromParcel(data);
                    } else {
                        _arg02 = null;
                    }
                    int _arg15 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg22 = Uri.CREATOR.createFromParcel(data);
                    } else {
                        _arg22 = null;
                    }
                    ICarrierMessagingCallback _arg35 = ICarrierMessagingCallback.Stub.asInterface(data.readStrongBinder());
                    sendMms(_arg02, _arg15, _arg22, _arg35);
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg0 = Uri.CREATOR.createFromParcel(data);
                    } else {
                        _arg0 = null;
                    }
                    int _arg16 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg2 = Uri.CREATOR.createFromParcel(data);
                    } else {
                        _arg2 = null;
                    }
                    ICarrierMessagingCallback _arg36 = ICarrierMessagingCallback.Stub.asInterface(data.readStrongBinder());
                    downloadMms(_arg0, _arg16, _arg2, _arg36);
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements ICarrierMessagingService {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            @Override
            public void filterSms(MessagePdu pdu, String format, int destPort, int subId, ICarrierMessagingCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (pdu != null) {
                        _data.writeInt(1);
                        pdu.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(format);
                    _data.writeInt(destPort);
                    _data.writeInt(subId);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void sendTextSms(String text, int subId, String destAddress, ICarrierMessagingCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(text);
                    _data.writeInt(subId);
                    _data.writeString(destAddress);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void sendDataSms(byte[] data, int subId, String destAddress, int destPort, ICarrierMessagingCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeByteArray(data);
                    _data.writeInt(subId);
                    _data.writeString(destAddress);
                    _data.writeInt(destPort);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void sendMultipartTextSms(List<String> parts, int subId, String destAddress, ICarrierMessagingCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStringList(parts);
                    _data.writeInt(subId);
                    _data.writeString(destAddress);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void sendMms(Uri pduUri, int subId, Uri location, ICarrierMessagingCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (pduUri != null) {
                        _data.writeInt(1);
                        pduUri.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(subId);
                    if (location != null) {
                        _data.writeInt(1);
                        location.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void downloadMms(Uri pduUri, int subId, Uri location, ICarrierMessagingCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (pduUri != null) {
                        _data.writeInt(1);
                        pduUri.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(subId);
                    if (location != null) {
                        _data.writeInt(1);
                        location.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }
    }
}
