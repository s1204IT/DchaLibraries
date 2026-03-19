package org.gsma.joyn.chat;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.util.List;
import org.gsma.joyn.IJoynServiceRegistrationListener;
import org.gsma.joyn.chat.IChat;
import org.gsma.joyn.chat.IChatListener;
import org.gsma.joyn.chat.IExtendChat;
import org.gsma.joyn.chat.IExtendChatListener;
import org.gsma.joyn.chat.IGroupChat;
import org.gsma.joyn.chat.IGroupChatListener;
import org.gsma.joyn.chat.IGroupChatSyncingListener;
import org.gsma.joyn.chat.INewChatListener;

public interface IChatService extends IInterface {
    void addEventListener(INewChatListener iNewChatListener) throws RemoteException;

    void addServiceRegistrationListener(IJoynServiceRegistrationListener iJoynServiceRegistrationListener) throws RemoteException;

    void blockGroupMessages(String str, boolean z) throws RemoteException;

    IChat getChat(String str) throws RemoteException;

    IExtendChat getChatForSecondaryDevice() throws RemoteException;

    List<IBinder> getChats() throws RemoteException;

    ChatServiceConfiguration getConfiguration() throws RemoteException;

    IExtendChat getExtendChat(String str) throws RemoteException;

    IGroupChat getGroupChat(String str) throws RemoteException;

    List<IBinder> getGroupChats() throws RemoteException;

    int getServiceVersion() throws RemoteException;

    IGroupChat initiateClosedGroupChat(List<String> list, String str, IGroupChatListener iGroupChatListener) throws RemoteException;

    IGroupChat initiateGroupChat(List<String> list, String str, IGroupChatListener iGroupChatListener) throws RemoteException;

    boolean isImCapAlwaysOn() throws RemoteException;

    boolean isServiceRegistered() throws RemoteException;

    IExtendChat openMultipleChat(List<String> list, IExtendChatListener iExtendChatListener) throws RemoteException;

    IExtendChat openSecondaryDeviceChat(IExtendChatListener iExtendChatListener) throws RemoteException;

    IChat openSingleChat(String str, IChatListener iChatListener) throws RemoteException;

    IExtendChat openSingleChatEx(String str, IExtendChatListener iExtendChatListener) throws RemoteException;

    IGroupChat rejoinGroupChat(String str) throws RemoteException;

    IGroupChat rejoinGroupChatId(String str, String str2) throws RemoteException;

    void removeEventListener(INewChatListener iNewChatListener) throws RemoteException;

    void removeServiceRegistrationListener(IJoynServiceRegistrationListener iJoynServiceRegistrationListener) throws RemoteException;

    IGroupChat restartGroupChat(String str) throws RemoteException;

    void syncAllGroupChats(IGroupChatSyncingListener iGroupChatSyncingListener) throws RemoteException;

    void syncGroupChat(String str, IGroupChatSyncingListener iGroupChatSyncingListener) throws RemoteException;

    public static abstract class Stub extends Binder implements IChatService {
        private static final String DESCRIPTOR = "org.gsma.joyn.chat.IChatService";
        static final int TRANSACTION_addEventListener = 16;
        static final int TRANSACTION_addServiceRegistrationListener = 2;
        static final int TRANSACTION_blockGroupMessages = 24;
        static final int TRANSACTION_getChat = 18;
        static final int TRANSACTION_getChatForSecondaryDevice = 20;
        static final int TRANSACTION_getChats = 21;
        static final int TRANSACTION_getConfiguration = 4;
        static final int TRANSACTION_getExtendChat = 19;
        static final int TRANSACTION_getGroupChat = 23;
        static final int TRANSACTION_getGroupChats = 22;
        static final int TRANSACTION_getServiceVersion = 25;
        static final int TRANSACTION_initiateClosedGroupChat = 10;
        static final int TRANSACTION_initiateGroupChat = 9;
        static final int TRANSACTION_isImCapAlwaysOn = 26;
        static final int TRANSACTION_isServiceRegistered = 1;
        static final int TRANSACTION_openMultipleChat = 8;
        static final int TRANSACTION_openSecondaryDeviceChat = 7;
        static final int TRANSACTION_openSingleChat = 5;
        static final int TRANSACTION_openSingleChatEx = 6;
        static final int TRANSACTION_rejoinGroupChat = 11;
        static final int TRANSACTION_rejoinGroupChatId = 12;
        static final int TRANSACTION_removeEventListener = 17;
        static final int TRANSACTION_removeServiceRegistrationListener = 3;
        static final int TRANSACTION_restartGroupChat = 13;
        static final int TRANSACTION_syncAllGroupChats = 14;
        static final int TRANSACTION_syncGroupChat = 15;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IChatService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IChatService)) {
                return (IChatService) iin;
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
                    boolean _result = isServiceRegistered();
                    reply.writeNoException();
                    reply.writeInt(_result ? 1 : 0);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    IJoynServiceRegistrationListener _arg0 = IJoynServiceRegistrationListener.Stub.asInterface(data.readStrongBinder());
                    addServiceRegistrationListener(_arg0);
                    reply.writeNoException();
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    IJoynServiceRegistrationListener _arg02 = IJoynServiceRegistrationListener.Stub.asInterface(data.readStrongBinder());
                    removeServiceRegistrationListener(_arg02);
                    reply.writeNoException();
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    ChatServiceConfiguration _result2 = getConfiguration();
                    reply.writeNoException();
                    if (_result2 != null) {
                        reply.writeInt(1);
                        _result2.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg03 = data.readString();
                    IChatListener _arg1 = IChatListener.Stub.asInterface(data.readStrongBinder());
                    IChat _result3 = openSingleChat(_arg03, _arg1);
                    reply.writeNoException();
                    reply.writeStrongBinder(_result3 != null ? _result3.asBinder() : null);
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg04 = data.readString();
                    IExtendChatListener _arg12 = IExtendChatListener.Stub.asInterface(data.readStrongBinder());
                    IExtendChat _result4 = openSingleChatEx(_arg04, _arg12);
                    reply.writeNoException();
                    reply.writeStrongBinder(_result4 != null ? _result4.asBinder() : null);
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    IExtendChatListener _arg05 = IExtendChatListener.Stub.asInterface(data.readStrongBinder());
                    IExtendChat _result5 = openSecondaryDeviceChat(_arg05);
                    reply.writeNoException();
                    reply.writeStrongBinder(_result5 != null ? _result5.asBinder() : null);
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    List<String> _arg06 = data.createStringArrayList();
                    IExtendChatListener _arg13 = IExtendChatListener.Stub.asInterface(data.readStrongBinder());
                    IExtendChat _result6 = openMultipleChat(_arg06, _arg13);
                    reply.writeNoException();
                    reply.writeStrongBinder(_result6 != null ? _result6.asBinder() : null);
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    List<String> _arg07 = data.createStringArrayList();
                    String _arg14 = data.readString();
                    IGroupChatListener _arg2 = IGroupChatListener.Stub.asInterface(data.readStrongBinder());
                    IGroupChat _result7 = initiateGroupChat(_arg07, _arg14, _arg2);
                    reply.writeNoException();
                    reply.writeStrongBinder(_result7 != null ? _result7.asBinder() : null);
                    return true;
                case 10:
                    data.enforceInterface(DESCRIPTOR);
                    List<String> _arg08 = data.createStringArrayList();
                    String _arg15 = data.readString();
                    IGroupChatListener _arg22 = IGroupChatListener.Stub.asInterface(data.readStrongBinder());
                    IGroupChat _result8 = initiateClosedGroupChat(_arg08, _arg15, _arg22);
                    reply.writeNoException();
                    reply.writeStrongBinder(_result8 != null ? _result8.asBinder() : null);
                    return true;
                case 11:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg09 = data.readString();
                    IGroupChat _result9 = rejoinGroupChat(_arg09);
                    reply.writeNoException();
                    reply.writeStrongBinder(_result9 != null ? _result9.asBinder() : null);
                    return true;
                case 12:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg010 = data.readString();
                    String _arg16 = data.readString();
                    IGroupChat _result10 = rejoinGroupChatId(_arg010, _arg16);
                    reply.writeNoException();
                    reply.writeStrongBinder(_result10 != null ? _result10.asBinder() : null);
                    return true;
                case 13:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg011 = data.readString();
                    IGroupChat _result11 = restartGroupChat(_arg011);
                    reply.writeNoException();
                    reply.writeStrongBinder(_result11 != null ? _result11.asBinder() : null);
                    return true;
                case 14:
                    data.enforceInterface(DESCRIPTOR);
                    IGroupChatSyncingListener _arg012 = IGroupChatSyncingListener.Stub.asInterface(data.readStrongBinder());
                    syncAllGroupChats(_arg012);
                    reply.writeNoException();
                    return true;
                case 15:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg013 = data.readString();
                    IGroupChatSyncingListener _arg17 = IGroupChatSyncingListener.Stub.asInterface(data.readStrongBinder());
                    syncGroupChat(_arg013, _arg17);
                    reply.writeNoException();
                    return true;
                case 16:
                    data.enforceInterface(DESCRIPTOR);
                    INewChatListener _arg014 = INewChatListener.Stub.asInterface(data.readStrongBinder());
                    addEventListener(_arg014);
                    reply.writeNoException();
                    return true;
                case 17:
                    data.enforceInterface(DESCRIPTOR);
                    INewChatListener _arg015 = INewChatListener.Stub.asInterface(data.readStrongBinder());
                    removeEventListener(_arg015);
                    reply.writeNoException();
                    return true;
                case 18:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg016 = data.readString();
                    IChat _result12 = getChat(_arg016);
                    reply.writeNoException();
                    reply.writeStrongBinder(_result12 != null ? _result12.asBinder() : null);
                    return true;
                case 19:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg017 = data.readString();
                    IExtendChat _result13 = getExtendChat(_arg017);
                    reply.writeNoException();
                    reply.writeStrongBinder(_result13 != null ? _result13.asBinder() : null);
                    return true;
                case 20:
                    data.enforceInterface(DESCRIPTOR);
                    IExtendChat _result14 = getChatForSecondaryDevice();
                    reply.writeNoException();
                    reply.writeStrongBinder(_result14 != null ? _result14.asBinder() : null);
                    return true;
                case 21:
                    data.enforceInterface(DESCRIPTOR);
                    List<IBinder> _result15 = getChats();
                    reply.writeNoException();
                    reply.writeBinderList(_result15);
                    return true;
                case 22:
                    data.enforceInterface(DESCRIPTOR);
                    List<IBinder> _result16 = getGroupChats();
                    reply.writeNoException();
                    reply.writeBinderList(_result16);
                    return true;
                case 23:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg018 = data.readString();
                    IGroupChat _result17 = getGroupChat(_arg018);
                    reply.writeNoException();
                    reply.writeStrongBinder(_result17 != null ? _result17.asBinder() : null);
                    return true;
                case 24:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg019 = data.readString();
                    boolean _arg18 = data.readInt() != 0;
                    blockGroupMessages(_arg019, _arg18);
                    reply.writeNoException();
                    return true;
                case 25:
                    data.enforceInterface(DESCRIPTOR);
                    int _result18 = getServiceVersion();
                    reply.writeNoException();
                    reply.writeInt(_result18);
                    return true;
                case 26:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result19 = isImCapAlwaysOn();
                    reply.writeNoException();
                    reply.writeInt(_result19 ? 1 : 0);
                    return true;
                case 1598968902:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IChatService {
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
            public boolean isServiceRegistered() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void addServiceRegistrationListener(IJoynServiceRegistrationListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void removeServiceRegistrationListener(IJoynServiceRegistrationListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public ChatServiceConfiguration getConfiguration() throws RemoteException {
                ChatServiceConfiguration chatServiceConfigurationCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        chatServiceConfigurationCreateFromParcel = ChatServiceConfiguration.CREATOR.createFromParcel(_reply);
                    } else {
                        chatServiceConfigurationCreateFromParcel = null;
                    }
                    return chatServiceConfigurationCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public IChat openSingleChat(String contact, IChatListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(contact);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    IChat _result = IChat.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public IExtendChat openSingleChatEx(String contact, IExtendChatListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(contact);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    IExtendChat _result = IExtendChat.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public IExtendChat openSecondaryDeviceChat(IExtendChatListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    IExtendChat _result = IExtendChat.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public IExtendChat openMultipleChat(List<String> participants, IExtendChatListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStringList(participants);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    IExtendChat _result = IExtendChat.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public IGroupChat initiateGroupChat(List<String> contacts, String subject, IGroupChatListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStringList(contacts);
                    _data.writeString(subject);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    IGroupChat _result = IGroupChat.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public IGroupChat initiateClosedGroupChat(List<String> contacts, String subject, IGroupChatListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStringList(contacts);
                    _data.writeString(subject);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    IGroupChat _result = IGroupChat.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public IGroupChat rejoinGroupChat(String chatId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(chatId);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    IGroupChat _result = IGroupChat.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public IGroupChat rejoinGroupChatId(String chatId, String rejoinId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(chatId);
                    _data.writeString(rejoinId);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    IGroupChat _result = IGroupChat.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public IGroupChat restartGroupChat(String chatId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(chatId);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                    IGroupChat _result = IGroupChat.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void syncAllGroupChats(IGroupChatSyncingListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void syncGroupChat(String chatId, IGroupChatSyncingListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(chatId);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void addEventListener(INewChatListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void removeEventListener(INewChatListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public IChat getChat(String chatId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(chatId);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                    IChat _result = IChat.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public IExtendChat getExtendChat(String chatId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(chatId);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                    IExtendChat _result = IExtendChat.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public IExtendChat getChatForSecondaryDevice() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                    IExtendChat _result = IExtendChat.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public List<IBinder> getChats() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                    List<IBinder> _result = _reply.createBinderArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public List<IBinder> getGroupChats() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                    List<IBinder> _result = _reply.createBinderArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public IGroupChat getGroupChat(String chatId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(chatId);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                    IGroupChat _result = IGroupChat.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void blockGroupMessages(String chatId, boolean flag) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(chatId);
                    _data.writeInt(flag ? 1 : 0);
                    this.mRemote.transact(24, _data, _reply, 0);
                    _reply.readException();
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
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isImCapAlwaysOn() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(26, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
