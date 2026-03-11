package com.android.systemui.recents;

import android.graphics.Rect;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IRecentsNonSystemUserCallbacks extends IInterface {
    void cancelPreloadingRecents() throws RemoteException;

    void dockTopTask(int i, int i2, int i3, Rect rect) throws RemoteException;

    void hideRecents(boolean z, boolean z2) throws RemoteException;

    void onConfigurationChanged() throws RemoteException;

    void onDraggingInRecents(float f) throws RemoteException;

    void onDraggingInRecentsEnded(float f) throws RemoteException;

    void preloadRecents() throws RemoteException;

    void showRecents(boolean z, boolean z2, boolean z3, boolean z4, boolean z5, int i) throws RemoteException;

    void toggleRecents(int i) throws RemoteException;

    public static abstract class Stub extends Binder implements IRecentsNonSystemUserCallbacks {
        public Stub() {
            attachInterface(this, "com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
        }

        public static IRecentsNonSystemUserCallbacks asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
            if (iin != null && (iin instanceof IRecentsNonSystemUserCallbacks)) {
                return (IRecentsNonSystemUserCallbacks) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            Rect rect;
            switch (code) {
                case 1:
                    data.enforceInterface("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                    preloadRecents();
                    return true;
                case 2:
                    data.enforceInterface("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                    cancelPreloadingRecents();
                    return true;
                case 3:
                    data.enforceInterface("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                    boolean _arg0 = data.readInt() != 0;
                    boolean _arg1 = data.readInt() != 0;
                    boolean _arg2 = data.readInt() != 0;
                    boolean _arg3 = data.readInt() != 0;
                    boolean _arg4 = data.readInt() != 0;
                    int _arg5 = data.readInt();
                    showRecents(_arg0, _arg1, _arg2, _arg3, _arg4, _arg5);
                    return true;
                case 4:
                    data.enforceInterface("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                    boolean _arg02 = data.readInt() != 0;
                    boolean _arg12 = data.readInt() != 0;
                    hideRecents(_arg02, _arg12);
                    return true;
                case 5:
                    data.enforceInterface("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                    int _arg03 = data.readInt();
                    toggleRecents(_arg03);
                    return true;
                case 6:
                    data.enforceInterface("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                    onConfigurationChanged();
                    return true;
                case 7:
                    data.enforceInterface("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                    int _arg04 = data.readInt();
                    int _arg13 = data.readInt();
                    int _arg22 = data.readInt();
                    if (data.readInt() != 0) {
                        rect = (Rect) Rect.CREATOR.createFromParcel(data);
                    } else {
                        rect = null;
                    }
                    dockTopTask(_arg04, _arg13, _arg22, rect);
                    return true;
                case 8:
                    data.enforceInterface("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                    float _arg05 = data.readFloat();
                    onDraggingInRecents(_arg05);
                    return true;
                case 9:
                    data.enforceInterface("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                    float _arg06 = data.readFloat();
                    onDraggingInRecentsEnded(_arg06);
                    return true;
                case 1598968902:
                    reply.writeString("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IRecentsNonSystemUserCallbacks {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override
            public IBinder asBinder() {
                return this.mRemote;
            }

            @Override
            public void preloadRecents() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void cancelPreloadingRecents() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void showRecents(boolean triggeredFromAltTab, boolean draggingInRecents, boolean animate, boolean reloadTasks, boolean fromHome, int recentsGrowTarget) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                    _data.writeInt(triggeredFromAltTab ? 1 : 0);
                    _data.writeInt(draggingInRecents ? 1 : 0);
                    _data.writeInt(animate ? 1 : 0);
                    _data.writeInt(reloadTasks ? 1 : 0);
                    _data.writeInt(fromHome ? 1 : 0);
                    _data.writeInt(recentsGrowTarget);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void hideRecents(boolean triggeredFromAltTab, boolean triggeredFromHomeKey) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                    _data.writeInt(triggeredFromAltTab ? 1 : 0);
                    _data.writeInt(triggeredFromHomeKey ? 1 : 0);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void toggleRecents(int recentsGrowTarget) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                    _data.writeInt(recentsGrowTarget);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onConfigurationChanged() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void dockTopTask(int topTaskId, int dragMode, int stackCreateMode, Rect initialBounds) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                    _data.writeInt(topTaskId);
                    _data.writeInt(dragMode);
                    _data.writeInt(stackCreateMode);
                    if (initialBounds != null) {
                        _data.writeInt(1);
                        initialBounds.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onDraggingInRecents(float distanceFromTop) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                    _data.writeFloat(distanceFromTop);
                    this.mRemote.transact(8, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onDraggingInRecentsEnded(float velocity) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                    _data.writeFloat(velocity);
                    this.mRemote.transact(9, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }
    }
}
