package jp.co.omronsoft.android.decoemojimanager.interfacedata;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.util.ArrayList;
import java.util.List;

public interface IDecoEmojiManager extends IInterface {
    int aidl_changeDecoEmojiInfo(DecoEmojiModInfo decoEmojiModInfo) throws RemoteException;

    int aidl_changeHistory(String str) throws RemoteException;

    int aidl_checkDecoEmoji(int i, String str) throws RemoteException;

    int aidl_getCategoryList(int i, List<DecoEmojiCategoryInfo> list) throws RemoteException;

    int aidl_getCategoryList_ex(int i, List<DecoEmojiCategoryInfo> list, int i2) throws RemoteException;

    int aidl_getDecoUri(int i, DecoEmojiUriInfo decoEmojiUriInfo) throws RemoteException;

    int aidl_getDecoUriList(int i, int i2, List<String> list) throws RemoteException;

    int aidl_getDecoUriList_ex(int i, int i2, List<String> list, int i3) throws RemoteException;

    int aidl_getHistoryUriList(int i, int i2, List<String> list) throws RemoteException;

    int aidl_getTagInfo(String str, String str2, List<String> list) throws RemoteException;

    int aidl_operateDecoEmoji(String str, int i) throws RemoteException;

    int aidl_resetHistoryCnt(int i) throws RemoteException;

    public static abstract class Stub extends Binder implements IDecoEmojiManager {
        private static final String DESCRIPTOR = "jp.co.omronsoft.android.decoemojimanager.interfacedata.IDecoEmojiManager";
        static final int TRANSACTION_aidl_changeDecoEmojiInfo = 5;
        static final int TRANSACTION_aidl_changeHistory = 8;
        static final int TRANSACTION_aidl_checkDecoEmoji = 2;
        static final int TRANSACTION_aidl_getCategoryList = 3;
        static final int TRANSACTION_aidl_getCategoryList_ex = 11;
        static final int TRANSACTION_aidl_getDecoUri = 6;
        static final int TRANSACTION_aidl_getDecoUriList = 4;
        static final int TRANSACTION_aidl_getDecoUriList_ex = 12;
        static final int TRANSACTION_aidl_getHistoryUriList = 10;
        static final int TRANSACTION_aidl_getTagInfo = 7;
        static final int TRANSACTION_aidl_operateDecoEmoji = 1;
        static final int TRANSACTION_aidl_resetHistoryCnt = 9;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IDecoEmojiManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IDecoEmojiManager)) {
                return (IDecoEmojiManager) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            DecoEmojiModInfo _arg0;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg02 = data.readString();
                    int _arg1 = data.readInt();
                    int _result = aidl_operateDecoEmoji(_arg02, _arg1);
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg03 = data.readInt();
                    String _arg12 = data.readString();
                    int _result2 = aidl_checkDecoEmoji(_arg03, _arg12);
                    reply.writeNoException();
                    reply.writeInt(_result2);
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg04 = data.readInt();
                    ArrayList arrayList = new ArrayList();
                    int _result3 = aidl_getCategoryList(_arg04, arrayList);
                    reply.writeNoException();
                    reply.writeInt(_result3);
                    reply.writeTypedList(arrayList);
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg05 = data.readInt();
                    int _arg13 = data.readInt();
                    List<String> _arg2 = new ArrayList<>();
                    int _result4 = aidl_getDecoUriList(_arg05, _arg13, _arg2);
                    reply.writeNoException();
                    reply.writeInt(_result4);
                    reply.writeStringList(_arg2);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg0 = DecoEmojiModInfo.CREATOR.createFromParcel(data);
                    } else {
                        _arg0 = null;
                    }
                    int _result5 = aidl_changeDecoEmojiInfo(_arg0);
                    reply.writeNoException();
                    reply.writeInt(_result5);
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg06 = data.readInt();
                    DecoEmojiUriInfo _arg14 = new DecoEmojiUriInfo();
                    int _result6 = aidl_getDecoUri(_arg06, _arg14);
                    reply.writeNoException();
                    reply.writeInt(_result6);
                    if (_arg14 != null) {
                        reply.writeInt(1);
                        _arg14.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg07 = data.readString();
                    String _arg15 = data.readString();
                    List<String> _arg22 = new ArrayList<>();
                    int _result7 = aidl_getTagInfo(_arg07, _arg15, _arg22);
                    reply.writeNoException();
                    reply.writeInt(_result7);
                    reply.writeStringList(_arg22);
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg08 = data.readString();
                    int _result8 = aidl_changeHistory(_arg08);
                    reply.writeNoException();
                    reply.writeInt(_result8);
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg09 = data.readInt();
                    int _result9 = aidl_resetHistoryCnt(_arg09);
                    reply.writeNoException();
                    reply.writeInt(_result9);
                    return true;
                case 10:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg010 = data.readInt();
                    int _arg16 = data.readInt();
                    List<String> _arg23 = new ArrayList<>();
                    int _result10 = aidl_getHistoryUriList(_arg010, _arg16, _arg23);
                    reply.writeNoException();
                    reply.writeInt(_result10);
                    reply.writeStringList(_arg23);
                    return true;
                case 11:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg011 = data.readInt();
                    ArrayList arrayList2 = new ArrayList();
                    int _result11 = aidl_getCategoryList_ex(_arg011, arrayList2, data.readInt());
                    reply.writeNoException();
                    reply.writeInt(_result11);
                    reply.writeTypedList(arrayList2);
                    return true;
                case 12:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg012 = data.readInt();
                    int _arg17 = data.readInt();
                    List<String> _arg24 = new ArrayList<>();
                    int _arg3 = data.readInt();
                    int _result12 = aidl_getDecoUriList_ex(_arg012, _arg17, _arg24, _arg3);
                    reply.writeNoException();
                    reply.writeInt(_result12);
                    reply.writeStringList(_arg24);
                    return true;
                case 1598968902:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IDecoEmojiManager {
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
            public int aidl_operateDecoEmoji(String uri, int flag) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(uri);
                    _data.writeInt(flag);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int aidl_checkDecoEmoji(int decoemoji_id, String ime_info) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(decoemoji_id);
                    _data.writeString(ime_info);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int aidl_getCategoryList(int maxDisplaycnt, List<DecoEmojiCategoryInfo> categoryList) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(maxDisplaycnt);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    _reply.readTypedList(categoryList, DecoEmojiCategoryInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int aidl_getDecoUriList(int categoryId, int pagenum, List<String> uriList) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(categoryId);
                    _data.writeInt(pagenum);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    _reply.readStringList(uriList);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int aidl_changeDecoEmojiInfo(DecoEmojiModInfo modInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (modInfo != null) {
                        _data.writeInt(1);
                        modInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int aidl_getDecoUri(int decoemoji_id, DecoEmojiUriInfo uriInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(decoemoji_id);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    if (_reply.readInt() != 0) {
                        uriInfo.readFromParcel(_reply);
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int aidl_getTagInfo(String uri, String tag_name, List<String> tag_info) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(uri);
                    _data.writeString(tag_name);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    _reply.readStringList(tag_info);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int aidl_changeHistory(String uri) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(uri);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int aidl_resetHistoryCnt(int target) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(target);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int aidl_getHistoryUriList(int maxGetcnt, int emojiType, List<String> uriList) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(maxGetcnt);
                    _data.writeInt(emojiType);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    _reply.readStringList(uriList);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int aidl_getCategoryList_ex(int maxDisplaycnt, List<DecoEmojiCategoryInfo> categoryList, int emojiType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(maxDisplaycnt);
                    _data.writeInt(emojiType);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    _reply.readTypedList(categoryList, DecoEmojiCategoryInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int aidl_getDecoUriList_ex(int categoryId, int pagenum, List<String> uriList, int emojiType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(categoryId);
                    _data.writeInt(pagenum);
                    _data.writeInt(emojiType);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    _reply.readStringList(uriList);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
