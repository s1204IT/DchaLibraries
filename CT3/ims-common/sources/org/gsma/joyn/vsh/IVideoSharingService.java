package org.gsma.joyn.vsh;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.util.List;
import org.gsma.joyn.IJoynServiceRegistrationListener;
import org.gsma.joyn.vsh.INewVideoSharingListener;
import org.gsma.joyn.vsh.IVideoPlayer;
import org.gsma.joyn.vsh.IVideoSharing;
import org.gsma.joyn.vsh.IVideoSharingListener;

public interface IVideoSharingService extends IInterface {
    void addNewVideoSharingListener(INewVideoSharingListener iNewVideoSharingListener) throws RemoteException;

    void addServiceRegistrationListener(IJoynServiceRegistrationListener iJoynServiceRegistrationListener) throws RemoteException;

    VideoSharingServiceConfiguration getConfiguration() throws RemoteException;

    int getServiceVersion() throws RemoteException;

    IVideoSharing getVideoSharing(String str) throws RemoteException;

    List<IBinder> getVideoSharings() throws RemoteException;

    boolean isServiceRegistered() throws RemoteException;

    void removeNewVideoSharingListener(INewVideoSharingListener iNewVideoSharingListener) throws RemoteException;

    void removeServiceRegistrationListener(IJoynServiceRegistrationListener iJoynServiceRegistrationListener) throws RemoteException;

    IVideoSharing shareVideo(String str, IVideoPlayer iVideoPlayer, IVideoSharingListener iVideoSharingListener) throws RemoteException;

    public static abstract class Stub extends Binder implements IVideoSharingService {
        private static final String DESCRIPTOR = "org.gsma.joyn.vsh.IVideoSharingService";
        static final int TRANSACTION_addNewVideoSharingListener = 8;
        static final int TRANSACTION_addServiceRegistrationListener = 2;
        static final int TRANSACTION_getConfiguration = 4;
        static final int TRANSACTION_getServiceVersion = 10;
        static final int TRANSACTION_getVideoSharing = 6;
        static final int TRANSACTION_getVideoSharings = 5;
        static final int TRANSACTION_isServiceRegistered = 1;
        static final int TRANSACTION_removeNewVideoSharingListener = 9;
        static final int TRANSACTION_removeServiceRegistrationListener = 3;
        static final int TRANSACTION_shareVideo = 7;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IVideoSharingService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IVideoSharingService)) {
                return (IVideoSharingService) iin;
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
                    VideoSharingServiceConfiguration _result2 = getConfiguration();
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
                    List<IBinder> _result3 = getVideoSharings();
                    reply.writeNoException();
                    reply.writeBinderList(_result3);
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg03 = data.readString();
                    IVideoSharing _result4 = getVideoSharing(_arg03);
                    reply.writeNoException();
                    reply.writeStrongBinder(_result4 != null ? _result4.asBinder() : null);
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg04 = data.readString();
                    IVideoPlayer _arg1 = IVideoPlayer.Stub.asInterface(data.readStrongBinder());
                    IVideoSharingListener _arg2 = IVideoSharingListener.Stub.asInterface(data.readStrongBinder());
                    IVideoSharing _result5 = shareVideo(_arg04, _arg1, _arg2);
                    reply.writeNoException();
                    reply.writeStrongBinder(_result5 != null ? _result5.asBinder() : null);
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    INewVideoSharingListener _arg05 = INewVideoSharingListener.Stub.asInterface(data.readStrongBinder());
                    addNewVideoSharingListener(_arg05);
                    reply.writeNoException();
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    INewVideoSharingListener _arg06 = INewVideoSharingListener.Stub.asInterface(data.readStrongBinder());
                    removeNewVideoSharingListener(_arg06);
                    reply.writeNoException();
                    return true;
                case 10:
                    data.enforceInterface(DESCRIPTOR);
                    int _result6 = getServiceVersion();
                    reply.writeNoException();
                    reply.writeInt(_result6);
                    return true;
                case 1598968902:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IVideoSharingService {
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
            public VideoSharingServiceConfiguration getConfiguration() throws RemoteException {
                VideoSharingServiceConfiguration videoSharingServiceConfigurationCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        videoSharingServiceConfigurationCreateFromParcel = VideoSharingServiceConfiguration.CREATOR.createFromParcel(_reply);
                    } else {
                        videoSharingServiceConfigurationCreateFromParcel = null;
                    }
                    return videoSharingServiceConfigurationCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public List<IBinder> getVideoSharings() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    List<IBinder> _result = _reply.createBinderArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public IVideoSharing getVideoSharing(String sharingId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(sharingId);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    IVideoSharing _result = IVideoSharing.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public IVideoSharing shareVideo(String contact, IVideoPlayer player, IVideoSharingListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(contact);
                    _data.writeStrongBinder(player != null ? player.asBinder() : null);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    IVideoSharing _result = IVideoSharing.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void addNewVideoSharingListener(INewVideoSharingListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void removeNewVideoSharingListener(INewVideoSharingListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(9, _data, _reply, 0);
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
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
