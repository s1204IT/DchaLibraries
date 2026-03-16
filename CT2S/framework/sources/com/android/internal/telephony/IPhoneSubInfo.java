package com.android.internal.telephony;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IPhoneSubInfo extends IInterface {
    String getCompleteVoiceMailNumber() throws RemoteException;

    String getCompleteVoiceMailNumberForSubscriber(int i) throws RemoteException;

    String getDeviceId() throws RemoteException;

    String getDeviceIdForPhone(int i) throws RemoteException;

    String getDeviceSvn() throws RemoteException;

    String getDeviceSvnUsingSubId(int i) throws RemoteException;

    String getGroupIdLevel1() throws RemoteException;

    String getGroupIdLevel1ForSubscriber(int i) throws RemoteException;

    String getIccSerialNumber() throws RemoteException;

    String getIccSerialNumberForSubscriber(int i) throws RemoteException;

    String getIccSimChallengeResponse(int i, int i2, String str) throws RemoteException;

    String getImeiForSubscriber(int i) throws RemoteException;

    String getIsimChallengeResponse(String str) throws RemoteException;

    String getIsimDomain() throws RemoteException;

    String getIsimImpi() throws RemoteException;

    String[] getIsimImpu() throws RemoteException;

    String getIsimIst() throws RemoteException;

    String[] getIsimPcscf() throws RemoteException;

    String getLine1AlphaTag() throws RemoteException;

    String getLine1AlphaTagForSubscriber(int i) throws RemoteException;

    String getLine1Number() throws RemoteException;

    String getLine1NumberForSubscriber(int i) throws RemoteException;

    String getMsisdn() throws RemoteException;

    String getMsisdnForSubscriber(int i) throws RemoteException;

    String getNaiForSubscriber(int i) throws RemoteException;

    String getSubscriberId() throws RemoteException;

    String getSubscriberIdForSubscriber(int i) throws RemoteException;

    String getVoiceMailAlphaTag() throws RemoteException;

    String getVoiceMailAlphaTagForSubscriber(int i) throws RemoteException;

    String getVoiceMailNumber() throws RemoteException;

    String getVoiceMailNumberForSubscriber(int i) throws RemoteException;

    public static abstract class Stub extends Binder implements IPhoneSubInfo {
        private static final String DESCRIPTOR = "com.android.internal.telephony.IPhoneSubInfo";
        static final int TRANSACTION_getCompleteVoiceMailNumber = 21;
        static final int TRANSACTION_getCompleteVoiceMailNumberForSubscriber = 22;
        static final int TRANSACTION_getDeviceId = 1;
        static final int TRANSACTION_getDeviceIdForPhone = 3;
        static final int TRANSACTION_getDeviceSvn = 5;
        static final int TRANSACTION_getDeviceSvnUsingSubId = 6;
        static final int TRANSACTION_getGroupIdLevel1 = 9;
        static final int TRANSACTION_getGroupIdLevel1ForSubscriber = 10;
        static final int TRANSACTION_getIccSerialNumber = 11;
        static final int TRANSACTION_getIccSerialNumberForSubscriber = 12;
        static final int TRANSACTION_getIccSimChallengeResponse = 31;
        static final int TRANSACTION_getImeiForSubscriber = 4;
        static final int TRANSACTION_getIsimChallengeResponse = 30;
        static final int TRANSACTION_getIsimDomain = 26;
        static final int TRANSACTION_getIsimImpi = 25;
        static final int TRANSACTION_getIsimImpu = 27;
        static final int TRANSACTION_getIsimIst = 28;
        static final int TRANSACTION_getIsimPcscf = 29;
        static final int TRANSACTION_getLine1AlphaTag = 15;
        static final int TRANSACTION_getLine1AlphaTagForSubscriber = 16;
        static final int TRANSACTION_getLine1Number = 13;
        static final int TRANSACTION_getLine1NumberForSubscriber = 14;
        static final int TRANSACTION_getMsisdn = 17;
        static final int TRANSACTION_getMsisdnForSubscriber = 18;
        static final int TRANSACTION_getNaiForSubscriber = 2;
        static final int TRANSACTION_getSubscriberId = 7;
        static final int TRANSACTION_getSubscriberIdForSubscriber = 8;
        static final int TRANSACTION_getVoiceMailAlphaTag = 23;
        static final int TRANSACTION_getVoiceMailAlphaTagForSubscriber = 24;
        static final int TRANSACTION_getVoiceMailNumber = 19;
        static final int TRANSACTION_getVoiceMailNumberForSubscriber = 20;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IPhoneSubInfo asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IPhoneSubInfo)) {
                return (IPhoneSubInfo) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    String _result = getDeviceId();
                    reply.writeNoException();
                    reply.writeString(_result);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg0 = data.readInt();
                    String _result2 = getNaiForSubscriber(_arg0);
                    reply.writeNoException();
                    reply.writeString(_result2);
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg02 = data.readInt();
                    String _result3 = getDeviceIdForPhone(_arg02);
                    reply.writeNoException();
                    reply.writeString(_result3);
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg03 = data.readInt();
                    String _result4 = getImeiForSubscriber(_arg03);
                    reply.writeNoException();
                    reply.writeString(_result4);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    String _result5 = getDeviceSvn();
                    reply.writeNoException();
                    reply.writeString(_result5);
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg04 = data.readInt();
                    String _result6 = getDeviceSvnUsingSubId(_arg04);
                    reply.writeNoException();
                    reply.writeString(_result6);
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    String _result7 = getSubscriberId();
                    reply.writeNoException();
                    reply.writeString(_result7);
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg05 = data.readInt();
                    String _result8 = getSubscriberIdForSubscriber(_arg05);
                    reply.writeNoException();
                    reply.writeString(_result8);
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    String _result9 = getGroupIdLevel1();
                    reply.writeNoException();
                    reply.writeString(_result9);
                    return true;
                case 10:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg06 = data.readInt();
                    String _result10 = getGroupIdLevel1ForSubscriber(_arg06);
                    reply.writeNoException();
                    reply.writeString(_result10);
                    return true;
                case 11:
                    data.enforceInterface(DESCRIPTOR);
                    String _result11 = getIccSerialNumber();
                    reply.writeNoException();
                    reply.writeString(_result11);
                    return true;
                case 12:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg07 = data.readInt();
                    String _result12 = getIccSerialNumberForSubscriber(_arg07);
                    reply.writeNoException();
                    reply.writeString(_result12);
                    return true;
                case 13:
                    data.enforceInterface(DESCRIPTOR);
                    String _result13 = getLine1Number();
                    reply.writeNoException();
                    reply.writeString(_result13);
                    return true;
                case 14:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg08 = data.readInt();
                    String _result14 = getLine1NumberForSubscriber(_arg08);
                    reply.writeNoException();
                    reply.writeString(_result14);
                    return true;
                case 15:
                    data.enforceInterface(DESCRIPTOR);
                    String _result15 = getLine1AlphaTag();
                    reply.writeNoException();
                    reply.writeString(_result15);
                    return true;
                case 16:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg09 = data.readInt();
                    String _result16 = getLine1AlphaTagForSubscriber(_arg09);
                    reply.writeNoException();
                    reply.writeString(_result16);
                    return true;
                case 17:
                    data.enforceInterface(DESCRIPTOR);
                    String _result17 = getMsisdn();
                    reply.writeNoException();
                    reply.writeString(_result17);
                    return true;
                case 18:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg010 = data.readInt();
                    String _result18 = getMsisdnForSubscriber(_arg010);
                    reply.writeNoException();
                    reply.writeString(_result18);
                    return true;
                case 19:
                    data.enforceInterface(DESCRIPTOR);
                    String _result19 = getVoiceMailNumber();
                    reply.writeNoException();
                    reply.writeString(_result19);
                    return true;
                case 20:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg011 = data.readInt();
                    String _result20 = getVoiceMailNumberForSubscriber(_arg011);
                    reply.writeNoException();
                    reply.writeString(_result20);
                    return true;
                case 21:
                    data.enforceInterface(DESCRIPTOR);
                    String _result21 = getCompleteVoiceMailNumber();
                    reply.writeNoException();
                    reply.writeString(_result21);
                    return true;
                case 22:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg012 = data.readInt();
                    String _result22 = getCompleteVoiceMailNumberForSubscriber(_arg012);
                    reply.writeNoException();
                    reply.writeString(_result22);
                    return true;
                case 23:
                    data.enforceInterface(DESCRIPTOR);
                    String _result23 = getVoiceMailAlphaTag();
                    reply.writeNoException();
                    reply.writeString(_result23);
                    return true;
                case 24:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg013 = data.readInt();
                    String _result24 = getVoiceMailAlphaTagForSubscriber(_arg013);
                    reply.writeNoException();
                    reply.writeString(_result24);
                    return true;
                case 25:
                    data.enforceInterface(DESCRIPTOR);
                    String _result25 = getIsimImpi();
                    reply.writeNoException();
                    reply.writeString(_result25);
                    return true;
                case 26:
                    data.enforceInterface(DESCRIPTOR);
                    String _result26 = getIsimDomain();
                    reply.writeNoException();
                    reply.writeString(_result26);
                    return true;
                case 27:
                    data.enforceInterface(DESCRIPTOR);
                    String[] _result27 = getIsimImpu();
                    reply.writeNoException();
                    reply.writeStringArray(_result27);
                    return true;
                case 28:
                    data.enforceInterface(DESCRIPTOR);
                    String _result28 = getIsimIst();
                    reply.writeNoException();
                    reply.writeString(_result28);
                    return true;
                case 29:
                    data.enforceInterface(DESCRIPTOR);
                    String[] _result29 = getIsimPcscf();
                    reply.writeNoException();
                    reply.writeStringArray(_result29);
                    return true;
                case 30:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg014 = data.readString();
                    String _result30 = getIsimChallengeResponse(_arg014);
                    reply.writeNoException();
                    reply.writeString(_result30);
                    return true;
                case 31:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg015 = data.readInt();
                    int _arg1 = data.readInt();
                    String _arg2 = data.readString();
                    String _result31 = getIccSimChallengeResponse(_arg015, _arg1, _arg2);
                    reply.writeNoException();
                    reply.writeString(_result31);
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IPhoneSubInfo {
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
            public String getDeviceId() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getNaiForSubscriber(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getDeviceIdForPhone(int phoneId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(phoneId);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getImeiForSubscriber(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getDeviceSvn() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getDeviceSvnUsingSubId(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getSubscriberId() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getSubscriberIdForSubscriber(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getGroupIdLevel1() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getGroupIdLevel1ForSubscriber(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getIccSerialNumber() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getIccSerialNumberForSubscriber(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getLine1Number() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getLine1NumberForSubscriber(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getLine1AlphaTag() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getLine1AlphaTagForSubscriber(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getMsisdn() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getMsisdnForSubscriber(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getVoiceMailNumber() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getVoiceMailNumberForSubscriber(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getCompleteVoiceMailNumber() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getCompleteVoiceMailNumberForSubscriber(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getVoiceMailAlphaTag() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getVoiceMailAlphaTagForSubscriber(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(24, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getIsimImpi() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getIsimDomain() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(26, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String[] getIsimImpu() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(27, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getIsimIst() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(28, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String[] getIsimPcscf() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(29, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getIsimChallengeResponse(String nonce) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(nonce);
                    this.mRemote.transact(30, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getIccSimChallengeResponse(int subId, int appType, String data) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(appType);
                    _data.writeString(data);
                    this.mRemote.transact(31, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
