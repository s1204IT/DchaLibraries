package android.media.session;

import android.content.ComponentName;
import android.media.IRemoteVolumeController;
import android.media.session.IActiveSessionsListener;
import android.media.session.ISession;
import android.media.session.ISessionCallback;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.view.KeyEvent;
import java.util.List;

public interface ISessionManager extends IInterface {
    void addSessionsListener(IActiveSessionsListener iActiveSessionsListener, ComponentName componentName, int i) throws RemoteException;

    ISession createSession(String str, ISessionCallback iSessionCallback, String str2, int i) throws RemoteException;

    void dispatchAdjustVolume(int i, int i2, int i3) throws RemoteException;

    void dispatchMediaKeyEvent(KeyEvent keyEvent, boolean z) throws RemoteException;

    List<IBinder> getSessions(ComponentName componentName, int i) throws RemoteException;

    boolean isGlobalPriorityActive() throws RemoteException;

    void removeSessionsListener(IActiveSessionsListener iActiveSessionsListener) throws RemoteException;

    void setRemoteVolumeController(IRemoteVolumeController iRemoteVolumeController) throws RemoteException;

    public static abstract class Stub extends Binder implements ISessionManager {
        private static final String DESCRIPTOR = "android.media.session.ISessionManager";
        static final int TRANSACTION_addSessionsListener = 5;
        static final int TRANSACTION_createSession = 1;
        static final int TRANSACTION_dispatchAdjustVolume = 4;
        static final int TRANSACTION_dispatchMediaKeyEvent = 3;
        static final int TRANSACTION_getSessions = 2;
        static final int TRANSACTION_isGlobalPriorityActive = 8;
        static final int TRANSACTION_removeSessionsListener = 6;
        static final int TRANSACTION_setRemoteVolumeController = 7;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ISessionManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof ISessionManager)) {
                return (ISessionManager) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            ComponentName componentNameCreateFromParcel;
            KeyEvent keyEventCreateFromParcel;
            ComponentName componentNameCreateFromParcel2;
            switch (i) {
                case 1:
                    parcel.enforceInterface(DESCRIPTOR);
                    ISession iSessionCreateSession = createSession(parcel.readString(), ISessionCallback.Stub.asInterface(parcel.readStrongBinder()), parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeStrongBinder(iSessionCreateSession != null ? iSessionCreateSession.asBinder() : null);
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    if (parcel.readInt() != 0) {
                        componentNameCreateFromParcel2 = ComponentName.CREATOR.createFromParcel(parcel);
                    } else {
                        componentNameCreateFromParcel2 = null;
                    }
                    List<IBinder> sessions = getSessions(componentNameCreateFromParcel2, parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeBinderList(sessions);
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    if (parcel.readInt() != 0) {
                        keyEventCreateFromParcel = KeyEvent.CREATOR.createFromParcel(parcel);
                    } else {
                        keyEventCreateFromParcel = null;
                    }
                    dispatchMediaKeyEvent(keyEventCreateFromParcel, parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 4:
                    parcel.enforceInterface(DESCRIPTOR);
                    dispatchAdjustVolume(parcel.readInt(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 5:
                    parcel.enforceInterface(DESCRIPTOR);
                    IActiveSessionsListener iActiveSessionsListenerAsInterface = IActiveSessionsListener.Stub.asInterface(parcel.readStrongBinder());
                    if (parcel.readInt() != 0) {
                        componentNameCreateFromParcel = ComponentName.CREATOR.createFromParcel(parcel);
                    } else {
                        componentNameCreateFromParcel = null;
                    }
                    addSessionsListener(iActiveSessionsListenerAsInterface, componentNameCreateFromParcel, parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 6:
                    parcel.enforceInterface(DESCRIPTOR);
                    removeSessionsListener(IActiveSessionsListener.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 7:
                    parcel.enforceInterface(DESCRIPTOR);
                    setRemoteVolumeController(IRemoteVolumeController.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 8:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsGlobalPriorityActive = isGlobalPriorityActive();
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsGlobalPriorityActive ? 1 : 0);
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    parcel2.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements ISessionManager {
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
            public ISession createSession(String packageName, ISessionCallback cb, String tag, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeStrongBinder(cb != null ? cb.asBinder() : null);
                    _data.writeString(tag);
                    _data.writeInt(userId);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    ISession _result = ISession.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public List<IBinder> getSessions(ComponentName compName, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (compName != null) {
                        _data.writeInt(1);
                        compName.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userId);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    List<IBinder> _result = _reply.createBinderArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void dispatchMediaKeyEvent(KeyEvent keyEvent, boolean needWakeLock) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (keyEvent != null) {
                        _data.writeInt(1);
                        keyEvent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(needWakeLock ? 1 : 0);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void dispatchAdjustVolume(int suggestedStream, int delta, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(suggestedStream);
                    _data.writeInt(delta);
                    _data.writeInt(flags);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void addSessionsListener(IActiveSessionsListener listener, ComponentName compName, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    if (compName != null) {
                        _data.writeInt(1);
                        compName.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userId);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void removeSessionsListener(IActiveSessionsListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setRemoteVolumeController(IRemoteVolumeController rvc) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(rvc != null ? rvc.asBinder() : null);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isGlobalPriorityActive() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(8, _data, _reply, 0);
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
