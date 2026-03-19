package android.accessibilityservice;

import android.accessibilityservice.IAccessibilityServiceConnection;
import android.graphics.Region;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

public interface IAccessibilityServiceClient extends IInterface {
    void clearAccessibilityCache() throws RemoteException;

    void init(IAccessibilityServiceConnection iAccessibilityServiceConnection, int i, IBinder iBinder) throws RemoteException;

    void onAccessibilityButtonAvailabilityChanged(boolean z) throws RemoteException;

    void onAccessibilityButtonClicked() throws RemoteException;

    void onAccessibilityEvent(AccessibilityEvent accessibilityEvent, boolean z) throws RemoteException;

    void onFingerprintCapturingGesturesChanged(boolean z) throws RemoteException;

    void onFingerprintGesture(int i) throws RemoteException;

    void onGesture(int i) throws RemoteException;

    void onInterrupt() throws RemoteException;

    void onKeyEvent(KeyEvent keyEvent, int i) throws RemoteException;

    void onMagnificationChanged(Region region, float f, float f2, float f3) throws RemoteException;

    void onPerformGestureResult(int i, boolean z) throws RemoteException;

    void onSoftKeyboardShowModeChanged(int i) throws RemoteException;

    public static abstract class Stub extends Binder implements IAccessibilityServiceClient {
        private static final String DESCRIPTOR = "android.accessibilityservice.IAccessibilityServiceClient";
        static final int TRANSACTION_clearAccessibilityCache = 5;
        static final int TRANSACTION_init = 1;
        static final int TRANSACTION_onAccessibilityButtonAvailabilityChanged = 13;
        static final int TRANSACTION_onAccessibilityButtonClicked = 12;
        static final int TRANSACTION_onAccessibilityEvent = 2;
        static final int TRANSACTION_onFingerprintCapturingGesturesChanged = 10;
        static final int TRANSACTION_onFingerprintGesture = 11;
        static final int TRANSACTION_onGesture = 4;
        static final int TRANSACTION_onInterrupt = 3;
        static final int TRANSACTION_onKeyEvent = 6;
        static final int TRANSACTION_onMagnificationChanged = 7;
        static final int TRANSACTION_onPerformGestureResult = 9;
        static final int TRANSACTION_onSoftKeyboardShowModeChanged = 8;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IAccessibilityServiceClient asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IAccessibilityServiceClient)) {
                return (IAccessibilityServiceClient) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i == 1598968902) {
                parcel2.writeString(DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    parcel.enforceInterface(DESCRIPTOR);
                    init(IAccessibilityServiceConnection.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt(), parcel.readStrongBinder());
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    onAccessibilityEvent(parcel.readInt() != 0 ? AccessibilityEvent.CREATOR.createFromParcel(parcel) : null, parcel.readInt() != 0);
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    onInterrupt();
                    return true;
                case 4:
                    parcel.enforceInterface(DESCRIPTOR);
                    onGesture(parcel.readInt());
                    return true;
                case 5:
                    parcel.enforceInterface(DESCRIPTOR);
                    clearAccessibilityCache();
                    return true;
                case 6:
                    parcel.enforceInterface(DESCRIPTOR);
                    onKeyEvent(parcel.readInt() != 0 ? KeyEvent.CREATOR.createFromParcel(parcel) : null, parcel.readInt());
                    return true;
                case 7:
                    parcel.enforceInterface(DESCRIPTOR);
                    onMagnificationChanged(parcel.readInt() != 0 ? Region.CREATOR.createFromParcel(parcel) : null, parcel.readFloat(), parcel.readFloat(), parcel.readFloat());
                    return true;
                case 8:
                    parcel.enforceInterface(DESCRIPTOR);
                    onSoftKeyboardShowModeChanged(parcel.readInt());
                    return true;
                case 9:
                    parcel.enforceInterface(DESCRIPTOR);
                    onPerformGestureResult(parcel.readInt(), parcel.readInt() != 0);
                    return true;
                case 10:
                    parcel.enforceInterface(DESCRIPTOR);
                    onFingerprintCapturingGesturesChanged(parcel.readInt() != 0);
                    return true;
                case 11:
                    parcel.enforceInterface(DESCRIPTOR);
                    onFingerprintGesture(parcel.readInt());
                    return true;
                case 12:
                    parcel.enforceInterface(DESCRIPTOR);
                    onAccessibilityButtonClicked();
                    return true;
                case 13:
                    parcel.enforceInterface(DESCRIPTOR);
                    onAccessibilityButtonAvailabilityChanged(parcel.readInt() != 0);
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IAccessibilityServiceClient {
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            @Override
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            @Override
            public void init(IAccessibilityServiceConnection iAccessibilityServiceConnection, int i, IBinder iBinder) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iAccessibilityServiceConnection != null ? iAccessibilityServiceConnection.asBinder() : null);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeStrongBinder(iBinder);
                    this.mRemote.transact(1, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (accessibilityEvent != null) {
                        parcelObtain.writeInt(1);
                        accessibilityEvent.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(2, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onInterrupt() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(3, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onGesture(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(4, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void clearAccessibilityCache() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(5, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onKeyEvent(KeyEvent keyEvent, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (keyEvent != null) {
                        parcelObtain.writeInt(1);
                        keyEvent.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(6, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onMagnificationChanged(Region region, float f, float f2, float f3) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (region != null) {
                        parcelObtain.writeInt(1);
                        region.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeFloat(f);
                    parcelObtain.writeFloat(f2);
                    parcelObtain.writeFloat(f3);
                    this.mRemote.transact(7, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onSoftKeyboardShowModeChanged(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(8, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onPerformGestureResult(int i, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(9, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onFingerprintCapturingGesturesChanged(boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(10, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onFingerprintGesture(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(11, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onAccessibilityButtonClicked() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(12, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onAccessibilityButtonAvailabilityChanged(boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(13, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }
        }
    }
}
