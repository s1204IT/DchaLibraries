package android.media.session;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.view.KeyEvent;

public interface IOnVolumeKeyLongPressListener extends IInterface {
    void onVolumeKeyLongPress(KeyEvent keyEvent) throws RemoteException;

    public static abstract class Stub extends Binder implements IOnVolumeKeyLongPressListener {
        private static final String DESCRIPTOR = "android.media.session.IOnVolumeKeyLongPressListener";
        static final int TRANSACTION_onVolumeKeyLongPress = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IOnVolumeKeyLongPressListener asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IOnVolumeKeyLongPressListener)) {
                return (IOnVolumeKeyLongPressListener) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            KeyEvent keyEventCreateFromParcel;
            if (i != 1) {
                if (i == 1598968902) {
                    parcel2.writeString(DESCRIPTOR);
                    return true;
                }
                return super.onTransact(i, parcel, parcel2, i2);
            }
            parcel.enforceInterface(DESCRIPTOR);
            if (parcel.readInt() != 0) {
                keyEventCreateFromParcel = KeyEvent.CREATOR.createFromParcel(parcel);
            } else {
                keyEventCreateFromParcel = null;
            }
            onVolumeKeyLongPress(keyEventCreateFromParcel);
            return true;
        }

        private static class Proxy implements IOnVolumeKeyLongPressListener {
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
            public void onVolumeKeyLongPress(KeyEvent keyEvent) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (keyEvent != null) {
                        parcelObtain.writeInt(1);
                        keyEvent.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(1, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }
        }
    }
}
