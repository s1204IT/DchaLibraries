package android.hardware.location;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IGeofenceHardwareMonitorCallback extends IInterface {
    void onMonitoringSystemChange(GeofenceHardwareMonitorEvent geofenceHardwareMonitorEvent) throws RemoteException;

    public static abstract class Stub extends Binder implements IGeofenceHardwareMonitorCallback {
        private static final String DESCRIPTOR = "android.hardware.location.IGeofenceHardwareMonitorCallback";
        static final int TRANSACTION_onMonitoringSystemChange = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IGeofenceHardwareMonitorCallback asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IGeofenceHardwareMonitorCallback)) {
                return (IGeofenceHardwareMonitorCallback) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            GeofenceHardwareMonitorEvent geofenceHardwareMonitorEventCreateFromParcel;
            if (i != 1) {
                if (i == 1598968902) {
                    parcel2.writeString(DESCRIPTOR);
                    return true;
                }
                return super.onTransact(i, parcel, parcel2, i2);
            }
            parcel.enforceInterface(DESCRIPTOR);
            if (parcel.readInt() != 0) {
                geofenceHardwareMonitorEventCreateFromParcel = GeofenceHardwareMonitorEvent.CREATOR.createFromParcel(parcel);
            } else {
                geofenceHardwareMonitorEventCreateFromParcel = null;
            }
            onMonitoringSystemChange(geofenceHardwareMonitorEventCreateFromParcel);
            return true;
        }

        private static class Proxy implements IGeofenceHardwareMonitorCallback {
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
            public void onMonitoringSystemChange(GeofenceHardwareMonitorEvent geofenceHardwareMonitorEvent) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (geofenceHardwareMonitorEvent != null) {
                        parcelObtain.writeInt(1);
                        geofenceHardwareMonitorEvent.writeToParcel(parcelObtain, 0);
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
