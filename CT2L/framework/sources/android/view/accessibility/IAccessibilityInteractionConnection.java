package android.view.accessibility;

import android.graphics.Region;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.view.MagnificationSpec;
import android.view.accessibility.IAccessibilityInteractionConnectionCallback;

public interface IAccessibilityInteractionConnection extends IInterface {
    void findAccessibilityNodeInfoByAccessibilityId(long j, Region region, int i, IAccessibilityInteractionConnectionCallback iAccessibilityInteractionConnectionCallback, int i2, int i3, long j2, MagnificationSpec magnificationSpec) throws RemoteException;

    void findAccessibilityNodeInfosByText(long j, String str, Region region, int i, IAccessibilityInteractionConnectionCallback iAccessibilityInteractionConnectionCallback, int i2, int i3, long j2, MagnificationSpec magnificationSpec) throws RemoteException;

    void findAccessibilityNodeInfosByViewId(long j, String str, Region region, int i, IAccessibilityInteractionConnectionCallback iAccessibilityInteractionConnectionCallback, int i2, int i3, long j2, MagnificationSpec magnificationSpec) throws RemoteException;

    void findFocus(long j, int i, Region region, int i2, IAccessibilityInteractionConnectionCallback iAccessibilityInteractionConnectionCallback, int i3, int i4, long j2, MagnificationSpec magnificationSpec) throws RemoteException;

    void focusSearch(long j, int i, Region region, int i2, IAccessibilityInteractionConnectionCallback iAccessibilityInteractionConnectionCallback, int i3, int i4, long j2, MagnificationSpec magnificationSpec) throws RemoteException;

    void performAccessibilityAction(long j, int i, Bundle bundle, int i2, IAccessibilityInteractionConnectionCallback iAccessibilityInteractionConnectionCallback, int i3, int i4, long j2) throws RemoteException;

    public static abstract class Stub extends Binder implements IAccessibilityInteractionConnection {
        private static final String DESCRIPTOR = "android.view.accessibility.IAccessibilityInteractionConnection";
        static final int TRANSACTION_findAccessibilityNodeInfoByAccessibilityId = 1;
        static final int TRANSACTION_findAccessibilityNodeInfosByText = 3;
        static final int TRANSACTION_findAccessibilityNodeInfosByViewId = 2;
        static final int TRANSACTION_findFocus = 4;
        static final int TRANSACTION_focusSearch = 5;
        static final int TRANSACTION_performAccessibilityAction = 6;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IAccessibilityInteractionConnection asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IAccessibilityInteractionConnection)) {
                return (IAccessibilityInteractionConnection) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            Bundle _arg2;
            Region _arg22;
            MagnificationSpec _arg8;
            Region _arg23;
            MagnificationSpec _arg82;
            Region _arg24;
            MagnificationSpec _arg83;
            Region _arg25;
            MagnificationSpec _arg84;
            Region _arg1;
            MagnificationSpec _arg7;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    long _arg0 = data.readLong();
                    if (data.readInt() != 0) {
                        _arg1 = Region.CREATOR.createFromParcel(data);
                    } else {
                        _arg1 = null;
                    }
                    int _arg26 = data.readInt();
                    IAccessibilityInteractionConnectionCallback _arg3 = IAccessibilityInteractionConnectionCallback.Stub.asInterface(data.readStrongBinder());
                    int _arg4 = data.readInt();
                    int _arg5 = data.readInt();
                    long _arg6 = data.readLong();
                    if (data.readInt() != 0) {
                        _arg7 = MagnificationSpec.CREATOR.createFromParcel(data);
                    } else {
                        _arg7 = null;
                    }
                    findAccessibilityNodeInfoByAccessibilityId(_arg0, _arg1, _arg26, _arg3, _arg4, _arg5, _arg6, _arg7);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    long _arg02 = data.readLong();
                    String _arg12 = data.readString();
                    if (data.readInt() != 0) {
                        _arg25 = Region.CREATOR.createFromParcel(data);
                    } else {
                        _arg25 = null;
                    }
                    int _arg32 = data.readInt();
                    IAccessibilityInteractionConnectionCallback _arg42 = IAccessibilityInteractionConnectionCallback.Stub.asInterface(data.readStrongBinder());
                    int _arg52 = data.readInt();
                    int _arg62 = data.readInt();
                    long _arg72 = data.readLong();
                    if (data.readInt() != 0) {
                        _arg84 = MagnificationSpec.CREATOR.createFromParcel(data);
                    } else {
                        _arg84 = null;
                    }
                    findAccessibilityNodeInfosByViewId(_arg02, _arg12, _arg25, _arg32, _arg42, _arg52, _arg62, _arg72, _arg84);
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    long _arg03 = data.readLong();
                    String _arg13 = data.readString();
                    if (data.readInt() != 0) {
                        _arg24 = Region.CREATOR.createFromParcel(data);
                    } else {
                        _arg24 = null;
                    }
                    int _arg33 = data.readInt();
                    IAccessibilityInteractionConnectionCallback _arg43 = IAccessibilityInteractionConnectionCallback.Stub.asInterface(data.readStrongBinder());
                    int _arg53 = data.readInt();
                    int _arg63 = data.readInt();
                    long _arg73 = data.readLong();
                    if (data.readInt() != 0) {
                        _arg83 = MagnificationSpec.CREATOR.createFromParcel(data);
                    } else {
                        _arg83 = null;
                    }
                    findAccessibilityNodeInfosByText(_arg03, _arg13, _arg24, _arg33, _arg43, _arg53, _arg63, _arg73, _arg83);
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    long _arg04 = data.readLong();
                    int _arg14 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg23 = Region.CREATOR.createFromParcel(data);
                    } else {
                        _arg23 = null;
                    }
                    int _arg34 = data.readInt();
                    IAccessibilityInteractionConnectionCallback _arg44 = IAccessibilityInteractionConnectionCallback.Stub.asInterface(data.readStrongBinder());
                    int _arg54 = data.readInt();
                    int _arg64 = data.readInt();
                    long _arg74 = data.readLong();
                    if (data.readInt() != 0) {
                        _arg82 = MagnificationSpec.CREATOR.createFromParcel(data);
                    } else {
                        _arg82 = null;
                    }
                    findFocus(_arg04, _arg14, _arg23, _arg34, _arg44, _arg54, _arg64, _arg74, _arg82);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    long _arg05 = data.readLong();
                    int _arg15 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg22 = Region.CREATOR.createFromParcel(data);
                    } else {
                        _arg22 = null;
                    }
                    int _arg35 = data.readInt();
                    IAccessibilityInteractionConnectionCallback _arg45 = IAccessibilityInteractionConnectionCallback.Stub.asInterface(data.readStrongBinder());
                    int _arg55 = data.readInt();
                    int _arg65 = data.readInt();
                    long _arg75 = data.readLong();
                    if (data.readInt() != 0) {
                        _arg8 = MagnificationSpec.CREATOR.createFromParcel(data);
                    } else {
                        _arg8 = null;
                    }
                    focusSearch(_arg05, _arg15, _arg22, _arg35, _arg45, _arg55, _arg65, _arg75, _arg8);
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    long _arg06 = data.readLong();
                    int _arg16 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg2 = Bundle.CREATOR.createFromParcel(data);
                    } else {
                        _arg2 = null;
                    }
                    int _arg36 = data.readInt();
                    IAccessibilityInteractionConnectionCallback _arg46 = IAccessibilityInteractionConnectionCallback.Stub.asInterface(data.readStrongBinder());
                    int _arg56 = data.readInt();
                    int _arg66 = data.readInt();
                    long _arg76 = data.readLong();
                    performAccessibilityAction(_arg06, _arg16, _arg2, _arg36, _arg46, _arg56, _arg66, _arg76);
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IAccessibilityInteractionConnection {
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
            public void findAccessibilityNodeInfoByAccessibilityId(long accessibilityNodeId, Region bounds, int interactionId, IAccessibilityInteractionConnectionCallback callback, int flags, int interrogatingPid, long interrogatingTid, MagnificationSpec spec) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(accessibilityNodeId);
                    if (bounds != null) {
                        _data.writeInt(1);
                        bounds.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(interactionId);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    _data.writeInt(flags);
                    _data.writeInt(interrogatingPid);
                    _data.writeLong(interrogatingTid);
                    if (spec != null) {
                        _data.writeInt(1);
                        spec.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void findAccessibilityNodeInfosByViewId(long accessibilityNodeId, String viewId, Region bounds, int interactionId, IAccessibilityInteractionConnectionCallback callback, int flags, int interrogatingPid, long interrogatingTid, MagnificationSpec spec) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(accessibilityNodeId);
                    _data.writeString(viewId);
                    if (bounds != null) {
                        _data.writeInt(1);
                        bounds.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(interactionId);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    _data.writeInt(flags);
                    _data.writeInt(interrogatingPid);
                    _data.writeLong(interrogatingTid);
                    if (spec != null) {
                        _data.writeInt(1);
                        spec.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void findAccessibilityNodeInfosByText(long accessibilityNodeId, String text, Region bounds, int interactionId, IAccessibilityInteractionConnectionCallback callback, int flags, int interrogatingPid, long interrogatingTid, MagnificationSpec spec) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(accessibilityNodeId);
                    _data.writeString(text);
                    if (bounds != null) {
                        _data.writeInt(1);
                        bounds.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(interactionId);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    _data.writeInt(flags);
                    _data.writeInt(interrogatingPid);
                    _data.writeLong(interrogatingTid);
                    if (spec != null) {
                        _data.writeInt(1);
                        spec.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void findFocus(long accessibilityNodeId, int focusType, Region bounds, int interactionId, IAccessibilityInteractionConnectionCallback callback, int flags, int interrogatingPid, long interrogatingTid, MagnificationSpec spec) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(accessibilityNodeId);
                    _data.writeInt(focusType);
                    if (bounds != null) {
                        _data.writeInt(1);
                        bounds.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(interactionId);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    _data.writeInt(flags);
                    _data.writeInt(interrogatingPid);
                    _data.writeLong(interrogatingTid);
                    if (spec != null) {
                        _data.writeInt(1);
                        spec.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void focusSearch(long accessibilityNodeId, int direction, Region bounds, int interactionId, IAccessibilityInteractionConnectionCallback callback, int flags, int interrogatingPid, long interrogatingTid, MagnificationSpec spec) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(accessibilityNodeId);
                    _data.writeInt(direction);
                    if (bounds != null) {
                        _data.writeInt(1);
                        bounds.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(interactionId);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    _data.writeInt(flags);
                    _data.writeInt(interrogatingPid);
                    _data.writeLong(interrogatingTid);
                    if (spec != null) {
                        _data.writeInt(1);
                        spec.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void performAccessibilityAction(long accessibilityNodeId, int action, Bundle arguments, int interactionId, IAccessibilityInteractionConnectionCallback callback, int flags, int interrogatingPid, long interrogatingTid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(accessibilityNodeId);
                    _data.writeInt(action);
                    if (arguments != null) {
                        _data.writeInt(1);
                        arguments.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(interactionId);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    _data.writeInt(flags);
                    _data.writeInt(interrogatingPid);
                    _data.writeLong(interrogatingTid);
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }
    }
}
