package org.gsma.joyn.contacts;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.util.List;

public interface IContactsService extends IInterface {
    List<String> getImBlockedContacts() throws RemoteException;

    List<String> getImBlockedContactsFromLocal() throws RemoteException;

    JoynContact getJoynContact(String str) throws RemoteException;

    List<JoynContact> getJoynContacts() throws RemoteException;

    List<JoynContact> getJoynContactsOnline() throws RemoteException;

    List<JoynContact> getJoynContactsSupporting(String str) throws RemoteException;

    int getRegistrationState(String str) throws RemoteException;

    int getServiceVersion() throws RemoteException;

    String getTimeStampForBlockedContact(String str) throws RemoteException;

    boolean isImBlockedForContact(String str) throws RemoteException;

    boolean isRcsValidNumber(String str) throws RemoteException;

    void loadImBlockedContactsToLocal() throws RemoteException;

    void setFtBlockedForContact(String str, boolean z) throws RemoteException;

    void setImBlockedForContact(String str, boolean z) throws RemoteException;

    public static abstract class Stub extends Binder implements IContactsService {
        private static final String DESCRIPTOR = "org.gsma.joyn.contacts.IContactsService";
        static final int TRANSACTION_getImBlockedContacts = 8;
        static final int TRANSACTION_getImBlockedContactsFromLocal = 6;
        static final int TRANSACTION_getJoynContact = 1;
        static final int TRANSACTION_getJoynContacts = 2;
        static final int TRANSACTION_getJoynContactsOnline = 3;
        static final int TRANSACTION_getJoynContactsSupporting = 4;
        static final int TRANSACTION_getRegistrationState = 13;
        static final int TRANSACTION_getServiceVersion = 5;
        static final int TRANSACTION_getTimeStampForBlockedContact = 9;
        static final int TRANSACTION_isImBlockedForContact = 7;
        static final int TRANSACTION_isRcsValidNumber = 12;
        static final int TRANSACTION_loadImBlockedContactsToLocal = 14;
        static final int TRANSACTION_setFtBlockedForContact = 11;
        static final int TRANSACTION_setImBlockedForContact = 10;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IContactsService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IContactsService)) {
                return (IContactsService) iin;
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
                    String _arg0 = data.readString();
                    JoynContact _result = getJoynContact(_arg0);
                    reply.writeNoException();
                    if (_result != null) {
                        reply.writeInt(1);
                        _result.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    List<JoynContact> _result2 = getJoynContacts();
                    reply.writeNoException();
                    reply.writeTypedList(_result2);
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    List<JoynContact> _result3 = getJoynContactsOnline();
                    reply.writeNoException();
                    reply.writeTypedList(_result3);
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg02 = data.readString();
                    List<JoynContact> _result4 = getJoynContactsSupporting(_arg02);
                    reply.writeNoException();
                    reply.writeTypedList(_result4);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    int _result5 = getServiceVersion();
                    reply.writeNoException();
                    reply.writeInt(_result5);
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    List<String> _result6 = getImBlockedContactsFromLocal();
                    reply.writeNoException();
                    reply.writeStringList(_result6);
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg03 = data.readString();
                    boolean _result7 = isImBlockedForContact(_arg03);
                    reply.writeNoException();
                    reply.writeInt(_result7 ? 1 : 0);
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    List<String> _result8 = getImBlockedContacts();
                    reply.writeNoException();
                    reply.writeStringList(_result8);
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg04 = data.readString();
                    String _result9 = getTimeStampForBlockedContact(_arg04);
                    reply.writeNoException();
                    reply.writeString(_result9);
                    return true;
                case 10:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg05 = data.readString();
                    boolean _arg1 = data.readInt() != 0;
                    setImBlockedForContact(_arg05, _arg1);
                    reply.writeNoException();
                    return true;
                case 11:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg06 = data.readString();
                    boolean _arg12 = data.readInt() != 0;
                    setFtBlockedForContact(_arg06, _arg12);
                    reply.writeNoException();
                    return true;
                case 12:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg07 = data.readString();
                    boolean _result10 = isRcsValidNumber(_arg07);
                    reply.writeNoException();
                    reply.writeInt(_result10 ? 1 : 0);
                    return true;
                case 13:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg08 = data.readString();
                    int _result11 = getRegistrationState(_arg08);
                    reply.writeNoException();
                    reply.writeInt(_result11);
                    return true;
                case 14:
                    data.enforceInterface(DESCRIPTOR);
                    loadImBlockedContactsToLocal();
                    reply.writeNoException();
                    return true;
                case 1598968902:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IContactsService {
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
            public JoynContact getJoynContact(String contactId) throws RemoteException {
                JoynContact joynContactCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(contactId);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        joynContactCreateFromParcel = JoynContact.CREATOR.createFromParcel(_reply);
                    } else {
                        joynContactCreateFromParcel = null;
                    }
                    return joynContactCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public List<JoynContact> getJoynContacts() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    List<JoynContact> _result = _reply.createTypedArrayList(JoynContact.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public List<JoynContact> getJoynContactsOnline() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    List<JoynContact> _result = _reply.createTypedArrayList(JoynContact.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public List<JoynContact> getJoynContactsSupporting(String tag) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(tag);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    List<JoynContact> _result = _reply.createTypedArrayList(JoynContact.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getServiceVersion() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
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
            public List<String> getImBlockedContactsFromLocal() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isImBlockedForContact(String contact) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(contact);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public List<String> getImBlockedContacts() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getTimeStampForBlockedContact(String contact) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(contact);
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
            public void setImBlockedForContact(String contact, boolean flag) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(contact);
                    _data.writeInt(flag ? 1 : 0);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setFtBlockedForContact(String contact, boolean flag) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(contact);
                    _data.writeInt(flag ? 1 : 0);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isRcsValidNumber(String number) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(number);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getRegistrationState(String contact) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(contact);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void loadImBlockedContactsToLocal() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
