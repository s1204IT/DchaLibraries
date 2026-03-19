package android.location;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.util.ArrayList;
import java.util.List;

public interface IGeocodeProvider extends IInterface {
    String getFromLocation(double d, double d2, int i, GeocoderParams geocoderParams, List<Address> list) throws RemoteException;

    String getFromLocationName(String str, double d, double d2, double d3, double d4, int i, GeocoderParams geocoderParams, List<Address> list) throws RemoteException;

    public static abstract class Stub extends Binder implements IGeocodeProvider {
        private static final String DESCRIPTOR = "android.location.IGeocodeProvider";
        static final int TRANSACTION_getFromLocation = 1;
        static final int TRANSACTION_getFromLocationName = 2;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IGeocodeProvider asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IGeocodeProvider)) {
                return (IGeocodeProvider) iInterfaceQueryLocalInterface;
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
                    double d = parcel.readDouble();
                    double d2 = parcel.readDouble();
                    int i3 = parcel.readInt();
                    GeocoderParams geocoderParamsCreateFromParcel = parcel.readInt() != 0 ? GeocoderParams.CREATOR.createFromParcel(parcel) : null;
                    ArrayList arrayList = new ArrayList();
                    String fromLocation = getFromLocation(d, d2, i3, geocoderParamsCreateFromParcel, arrayList);
                    parcel2.writeNoException();
                    parcel2.writeString(fromLocation);
                    parcel2.writeTypedList(arrayList);
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    String string = parcel.readString();
                    double d3 = parcel.readDouble();
                    double d4 = parcel.readDouble();
                    double d5 = parcel.readDouble();
                    double d6 = parcel.readDouble();
                    int i4 = parcel.readInt();
                    GeocoderParams geocoderParamsCreateFromParcel2 = parcel.readInt() != 0 ? GeocoderParams.CREATOR.createFromParcel(parcel) : null;
                    ArrayList arrayList2 = new ArrayList();
                    String fromLocationName = getFromLocationName(string, d3, d4, d5, d6, i4, geocoderParamsCreateFromParcel2, arrayList2);
                    parcel2.writeNoException();
                    parcel2.writeString(fromLocationName);
                    parcel2.writeTypedList(arrayList2);
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IGeocodeProvider {
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
            public String getFromLocation(double d, double d2, int i, GeocoderParams geocoderParams, List<Address> list) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeDouble(d);
                    parcelObtain.writeDouble(d2);
                    parcelObtain.writeInt(i);
                    if (geocoderParams != null) {
                        parcelObtain.writeInt(1);
                        geocoderParams.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    String string = parcelObtain2.readString();
                    parcelObtain2.readTypedList(list, Address.CREATOR);
                    return string;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getFromLocationName(String str, double d, double d2, double d3, double d4, int i, GeocoderParams geocoderParams, List<Address> list) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeDouble(d);
                    parcelObtain.writeDouble(d2);
                    parcelObtain.writeDouble(d3);
                    parcelObtain.writeDouble(d4);
                    parcelObtain.writeInt(i);
                    if (geocoderParams != null) {
                        parcelObtain.writeInt(1);
                        geocoderParams.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    String string = parcelObtain2.readString();
                    parcelObtain2.readTypedList(list, Address.CREATOR);
                    return string;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }
    }
}
