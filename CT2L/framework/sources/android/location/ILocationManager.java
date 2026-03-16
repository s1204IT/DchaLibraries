package android.location;

import android.app.PendingIntent;
import android.location.IGpsMeasurementsListener;
import android.location.IGpsNavigationMessageListener;
import android.location.IGpsStatusListener;
import android.location.ILocationListener;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.android.internal.location.ProviderProperties;
import java.util.ArrayList;
import java.util.List;

public interface ILocationManager extends IInterface {
    boolean addGpsMeasurementsListener(IGpsMeasurementsListener iGpsMeasurementsListener, String str) throws RemoteException;

    boolean addGpsNavigationMessageListener(IGpsNavigationMessageListener iGpsNavigationMessageListener, String str) throws RemoteException;

    boolean addGpsStatusListener(IGpsStatusListener iGpsStatusListener, String str) throws RemoteException;

    void addTestProvider(String str, ProviderProperties providerProperties) throws RemoteException;

    void clearTestProviderEnabled(String str) throws RemoteException;

    void clearTestProviderLocation(String str) throws RemoteException;

    void clearTestProviderStatus(String str) throws RemoteException;

    boolean geocoderIsPresent() throws RemoteException;

    List<String> getAllProviders() throws RemoteException;

    String getBestProvider(Criteria criteria, boolean z) throws RemoteException;

    String getFromLocation(double d, double d2, int i, GeocoderParams geocoderParams, List<Address> list) throws RemoteException;

    String getFromLocationName(String str, double d, double d2, double d3, double d4, int i, GeocoderParams geocoderParams, List<Address> list) throws RemoteException;

    Location getLastLocation(LocationRequest locationRequest, String str) throws RemoteException;

    ProviderProperties getProviderProperties(String str) throws RemoteException;

    List<String> getProviders(Criteria criteria, boolean z) throws RemoteException;

    boolean isProviderEnabled(String str) throws RemoteException;

    void locationCallbackFinished(ILocationListener iLocationListener) throws RemoteException;

    boolean providerMeetsCriteria(String str, Criteria criteria) throws RemoteException;

    void removeGeofence(Geofence geofence, PendingIntent pendingIntent, String str) throws RemoteException;

    void removeGpsMeasurementsListener(IGpsMeasurementsListener iGpsMeasurementsListener) throws RemoteException;

    void removeGpsNavigationMessageListener(IGpsNavigationMessageListener iGpsNavigationMessageListener) throws RemoteException;

    void removeGpsStatusListener(IGpsStatusListener iGpsStatusListener) throws RemoteException;

    void removeTestProvider(String str) throws RemoteException;

    void removeUpdates(ILocationListener iLocationListener, PendingIntent pendingIntent, String str) throws RemoteException;

    void reportLocation(Location location, boolean z) throws RemoteException;

    void requestGeofence(LocationRequest locationRequest, Geofence geofence, PendingIntent pendingIntent, String str) throws RemoteException;

    void requestLocationUpdates(LocationRequest locationRequest, ILocationListener iLocationListener, PendingIntent pendingIntent, String str) throws RemoteException;

    boolean sendExtraCommand(String str, String str2, Bundle bundle) throws RemoteException;

    boolean sendNiResponse(int i, int i2) throws RemoteException;

    void setTestProviderEnabled(String str, boolean z) throws RemoteException;

    void setTestProviderLocation(String str, Location location) throws RemoteException;

    void setTestProviderStatus(String str, int i, Bundle bundle, long j) throws RemoteException;

    public static abstract class Stub extends Binder implements ILocationManager {
        private static final String DESCRIPTOR = "android.location.ILocationManager";
        static final int TRANSACTION_addGpsMeasurementsListener = 12;
        static final int TRANSACTION_addGpsNavigationMessageListener = 14;
        static final int TRANSACTION_addGpsStatusListener = 6;
        static final int TRANSACTION_addTestProvider = 22;
        static final int TRANSACTION_clearTestProviderEnabled = 27;
        static final int TRANSACTION_clearTestProviderLocation = 25;
        static final int TRANSACTION_clearTestProviderStatus = 29;
        static final int TRANSACTION_geocoderIsPresent = 8;
        static final int TRANSACTION_getAllProviders = 16;
        static final int TRANSACTION_getBestProvider = 18;
        static final int TRANSACTION_getFromLocation = 9;
        static final int TRANSACTION_getFromLocationName = 10;
        static final int TRANSACTION_getLastLocation = 5;
        static final int TRANSACTION_getProviderProperties = 20;
        static final int TRANSACTION_getProviders = 17;
        static final int TRANSACTION_isProviderEnabled = 21;
        static final int TRANSACTION_locationCallbackFinished = 32;
        static final int TRANSACTION_providerMeetsCriteria = 19;
        static final int TRANSACTION_removeGeofence = 4;
        static final int TRANSACTION_removeGpsMeasurementsListener = 13;
        static final int TRANSACTION_removeGpsNavigationMessageListener = 15;
        static final int TRANSACTION_removeGpsStatusListener = 7;
        static final int TRANSACTION_removeTestProvider = 23;
        static final int TRANSACTION_removeUpdates = 2;
        static final int TRANSACTION_reportLocation = 31;
        static final int TRANSACTION_requestGeofence = 3;
        static final int TRANSACTION_requestLocationUpdates = 1;
        static final int TRANSACTION_sendExtraCommand = 30;
        static final int TRANSACTION_sendNiResponse = 11;
        static final int TRANSACTION_setTestProviderEnabled = 26;
        static final int TRANSACTION_setTestProviderLocation = 24;
        static final int TRANSACTION_setTestProviderStatus = 28;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ILocationManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof ILocationManager)) {
                return (ILocationManager) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            Location _arg0;
            Bundle _arg2;
            Bundle _arg22;
            Location _arg1;
            ProviderProperties _arg12;
            Criteria _arg13;
            Criteria _arg02;
            Criteria _arg03;
            GeocoderParams _arg6;
            GeocoderParams _arg3;
            LocationRequest _arg04;
            Geofence _arg05;
            PendingIntent _arg14;
            LocationRequest _arg06;
            Geofence _arg15;
            PendingIntent _arg23;
            PendingIntent _arg16;
            LocationRequest _arg07;
            PendingIntent _arg24;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg07 = LocationRequest.CREATOR.createFromParcel(data);
                    } else {
                        _arg07 = null;
                    }
                    ILocationListener _arg17 = ILocationListener.Stub.asInterface(data.readStrongBinder());
                    if (data.readInt() != 0) {
                        _arg24 = PendingIntent.CREATOR.createFromParcel(data);
                    } else {
                        _arg24 = null;
                    }
                    String _arg32 = data.readString();
                    requestLocationUpdates(_arg07, _arg17, _arg24, _arg32);
                    reply.writeNoException();
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    ILocationListener _arg08 = ILocationListener.Stub.asInterface(data.readStrongBinder());
                    if (data.readInt() != 0) {
                        _arg16 = PendingIntent.CREATOR.createFromParcel(data);
                    } else {
                        _arg16 = null;
                    }
                    String _arg25 = data.readString();
                    removeUpdates(_arg08, _arg16, _arg25);
                    reply.writeNoException();
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg06 = LocationRequest.CREATOR.createFromParcel(data);
                    } else {
                        _arg06 = null;
                    }
                    if (data.readInt() != 0) {
                        _arg15 = Geofence.CREATOR.createFromParcel(data);
                    } else {
                        _arg15 = null;
                    }
                    if (data.readInt() != 0) {
                        _arg23 = PendingIntent.CREATOR.createFromParcel(data);
                    } else {
                        _arg23 = null;
                    }
                    String _arg33 = data.readString();
                    requestGeofence(_arg06, _arg15, _arg23, _arg33);
                    reply.writeNoException();
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg05 = Geofence.CREATOR.createFromParcel(data);
                    } else {
                        _arg05 = null;
                    }
                    if (data.readInt() != 0) {
                        _arg14 = PendingIntent.CREATOR.createFromParcel(data);
                    } else {
                        _arg14 = null;
                    }
                    String _arg26 = data.readString();
                    removeGeofence(_arg05, _arg14, _arg26);
                    reply.writeNoException();
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg04 = LocationRequest.CREATOR.createFromParcel(data);
                    } else {
                        _arg04 = null;
                    }
                    String _arg18 = data.readString();
                    Location _result = getLastLocation(_arg04, _arg18);
                    reply.writeNoException();
                    if (_result != null) {
                        reply.writeInt(1);
                        _result.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    IGpsStatusListener _arg09 = IGpsStatusListener.Stub.asInterface(data.readStrongBinder());
                    String _arg19 = data.readString();
                    boolean _result2 = addGpsStatusListener(_arg09, _arg19);
                    reply.writeNoException();
                    reply.writeInt(_result2 ? 1 : 0);
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    IGpsStatusListener _arg010 = IGpsStatusListener.Stub.asInterface(data.readStrongBinder());
                    removeGpsStatusListener(_arg010);
                    reply.writeNoException();
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result3 = geocoderIsPresent();
                    reply.writeNoException();
                    reply.writeInt(_result3 ? 1 : 0);
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    double _arg011 = data.readDouble();
                    double _arg110 = data.readDouble();
                    int _arg27 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg3 = GeocoderParams.CREATOR.createFromParcel(data);
                    } else {
                        _arg3 = null;
                    }
                    ArrayList arrayList = new ArrayList();
                    String _result4 = getFromLocation(_arg011, _arg110, _arg27, _arg3, arrayList);
                    reply.writeNoException();
                    reply.writeString(_result4);
                    reply.writeTypedList(arrayList);
                    return true;
                case 10:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg012 = data.readString();
                    double _arg111 = data.readDouble();
                    double _arg28 = data.readDouble();
                    double _arg34 = data.readDouble();
                    double _arg4 = data.readDouble();
                    int _arg5 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg6 = GeocoderParams.CREATOR.createFromParcel(data);
                    } else {
                        _arg6 = null;
                    }
                    ArrayList arrayList2 = new ArrayList();
                    String _result5 = getFromLocationName(_arg012, _arg111, _arg28, _arg34, _arg4, _arg5, _arg6, arrayList2);
                    reply.writeNoException();
                    reply.writeString(_result5);
                    reply.writeTypedList(arrayList2);
                    return true;
                case 11:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg013 = data.readInt();
                    int _arg112 = data.readInt();
                    boolean _result6 = sendNiResponse(_arg013, _arg112);
                    reply.writeNoException();
                    reply.writeInt(_result6 ? 1 : 0);
                    return true;
                case 12:
                    data.enforceInterface(DESCRIPTOR);
                    IGpsMeasurementsListener _arg014 = IGpsMeasurementsListener.Stub.asInterface(data.readStrongBinder());
                    String _arg113 = data.readString();
                    boolean _result7 = addGpsMeasurementsListener(_arg014, _arg113);
                    reply.writeNoException();
                    reply.writeInt(_result7 ? 1 : 0);
                    return true;
                case 13:
                    data.enforceInterface(DESCRIPTOR);
                    IGpsMeasurementsListener _arg015 = IGpsMeasurementsListener.Stub.asInterface(data.readStrongBinder());
                    removeGpsMeasurementsListener(_arg015);
                    reply.writeNoException();
                    return true;
                case 14:
                    data.enforceInterface(DESCRIPTOR);
                    IGpsNavigationMessageListener _arg016 = IGpsNavigationMessageListener.Stub.asInterface(data.readStrongBinder());
                    String _arg114 = data.readString();
                    boolean _result8 = addGpsNavigationMessageListener(_arg016, _arg114);
                    reply.writeNoException();
                    reply.writeInt(_result8 ? 1 : 0);
                    return true;
                case 15:
                    data.enforceInterface(DESCRIPTOR);
                    IGpsNavigationMessageListener _arg017 = IGpsNavigationMessageListener.Stub.asInterface(data.readStrongBinder());
                    removeGpsNavigationMessageListener(_arg017);
                    reply.writeNoException();
                    return true;
                case 16:
                    data.enforceInterface(DESCRIPTOR);
                    List<String> _result9 = getAllProviders();
                    reply.writeNoException();
                    reply.writeStringList(_result9);
                    return true;
                case 17:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg03 = Criteria.CREATOR.createFromParcel(data);
                    } else {
                        _arg03 = null;
                    }
                    boolean _arg115 = data.readInt() != 0;
                    List<String> _result10 = getProviders(_arg03, _arg115);
                    reply.writeNoException();
                    reply.writeStringList(_result10);
                    return true;
                case 18:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg02 = Criteria.CREATOR.createFromParcel(data);
                    } else {
                        _arg02 = null;
                    }
                    boolean _arg116 = data.readInt() != 0;
                    String _result11 = getBestProvider(_arg02, _arg116);
                    reply.writeNoException();
                    reply.writeString(_result11);
                    return true;
                case 19:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg018 = data.readString();
                    if (data.readInt() != 0) {
                        _arg13 = Criteria.CREATOR.createFromParcel(data);
                    } else {
                        _arg13 = null;
                    }
                    boolean _result12 = providerMeetsCriteria(_arg018, _arg13);
                    reply.writeNoException();
                    reply.writeInt(_result12 ? 1 : 0);
                    return true;
                case 20:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg019 = data.readString();
                    ProviderProperties _result13 = getProviderProperties(_arg019);
                    reply.writeNoException();
                    if (_result13 != null) {
                        reply.writeInt(1);
                        _result13.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                case 21:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg020 = data.readString();
                    boolean _result14 = isProviderEnabled(_arg020);
                    reply.writeNoException();
                    reply.writeInt(_result14 ? 1 : 0);
                    return true;
                case 22:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg021 = data.readString();
                    if (data.readInt() != 0) {
                        _arg12 = ProviderProperties.CREATOR.createFromParcel(data);
                    } else {
                        _arg12 = null;
                    }
                    addTestProvider(_arg021, _arg12);
                    reply.writeNoException();
                    return true;
                case 23:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg022 = data.readString();
                    removeTestProvider(_arg022);
                    reply.writeNoException();
                    return true;
                case 24:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg023 = data.readString();
                    if (data.readInt() != 0) {
                        _arg1 = Location.CREATOR.createFromParcel(data);
                    } else {
                        _arg1 = null;
                    }
                    setTestProviderLocation(_arg023, _arg1);
                    reply.writeNoException();
                    return true;
                case 25:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg024 = data.readString();
                    clearTestProviderLocation(_arg024);
                    reply.writeNoException();
                    return true;
                case 26:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg025 = data.readString();
                    boolean _arg117 = data.readInt() != 0;
                    setTestProviderEnabled(_arg025, _arg117);
                    reply.writeNoException();
                    return true;
                case 27:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg026 = data.readString();
                    clearTestProviderEnabled(_arg026);
                    reply.writeNoException();
                    return true;
                case 28:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg027 = data.readString();
                    int _arg118 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg22 = Bundle.CREATOR.createFromParcel(data);
                    } else {
                        _arg22 = null;
                    }
                    long _arg35 = data.readLong();
                    setTestProviderStatus(_arg027, _arg118, _arg22, _arg35);
                    reply.writeNoException();
                    return true;
                case 29:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg028 = data.readString();
                    clearTestProviderStatus(_arg028);
                    reply.writeNoException();
                    return true;
                case 30:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg029 = data.readString();
                    String _arg119 = data.readString();
                    if (data.readInt() != 0) {
                        _arg2 = Bundle.CREATOR.createFromParcel(data);
                    } else {
                        _arg2 = null;
                    }
                    boolean _result15 = sendExtraCommand(_arg029, _arg119, _arg2);
                    reply.writeNoException();
                    reply.writeInt(_result15 ? 1 : 0);
                    if (_arg2 != null) {
                        reply.writeInt(1);
                        _arg2.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                case 31:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg0 = Location.CREATOR.createFromParcel(data);
                    } else {
                        _arg0 = null;
                    }
                    boolean _arg120 = data.readInt() != 0;
                    reportLocation(_arg0, _arg120);
                    reply.writeNoException();
                    return true;
                case 32:
                    data.enforceInterface(DESCRIPTOR);
                    ILocationListener _arg030 = ILocationListener.Stub.asInterface(data.readStrongBinder());
                    locationCallbackFinished(_arg030);
                    reply.writeNoException();
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements ILocationManager {
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
            public void requestLocationUpdates(LocationRequest request, ILocationListener listener, PendingIntent intent, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (request != null) {
                        _data.writeInt(1);
                        request.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    if (intent != null) {
                        _data.writeInt(1);
                        intent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(packageName);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void removeUpdates(ILocationListener listener, PendingIntent intent, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    if (intent != null) {
                        _data.writeInt(1);
                        intent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(packageName);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void requestGeofence(LocationRequest request, Geofence geofence, PendingIntent intent, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (request != null) {
                        _data.writeInt(1);
                        request.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (geofence != null) {
                        _data.writeInt(1);
                        geofence.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (intent != null) {
                        _data.writeInt(1);
                        intent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(packageName);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void removeGeofence(Geofence fence, PendingIntent intent, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (fence != null) {
                        _data.writeInt(1);
                        fence.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (intent != null) {
                        _data.writeInt(1);
                        intent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(packageName);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public Location getLastLocation(LocationRequest request, String packageName) throws RemoteException {
                Location _result;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (request != null) {
                        _data.writeInt(1);
                        request.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(packageName);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = Location.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean addGpsStatusListener(IGpsStatusListener listener, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    _data.writeString(packageName);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void removeGpsStatusListener(IGpsStatusListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean geocoderIsPresent() throws RemoteException {
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

            @Override
            public String getFromLocation(double latitude, double longitude, int maxResults, GeocoderParams params, List<Address> addrs) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeDouble(latitude);
                    _data.writeDouble(longitude);
                    _data.writeInt(maxResults);
                    if (params != null) {
                        _data.writeInt(1);
                        params.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    _reply.readTypedList(addrs, Address.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getFromLocationName(String locationName, double lowerLeftLatitude, double lowerLeftLongitude, double upperRightLatitude, double upperRightLongitude, int maxResults, GeocoderParams params, List<Address> addrs) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(locationName);
                    _data.writeDouble(lowerLeftLatitude);
                    _data.writeDouble(lowerLeftLongitude);
                    _data.writeDouble(upperRightLatitude);
                    _data.writeDouble(upperRightLongitude);
                    _data.writeInt(maxResults);
                    if (params != null) {
                        _data.writeInt(1);
                        params.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    _reply.readTypedList(addrs, Address.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean sendNiResponse(int notifId, int userResponse) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(notifId);
                    _data.writeInt(userResponse);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean addGpsMeasurementsListener(IGpsMeasurementsListener listener, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    _data.writeString(packageName);
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
            public void removeGpsMeasurementsListener(IGpsMeasurementsListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean addGpsNavigationMessageListener(IGpsNavigationMessageListener listener, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    _data.writeString(packageName);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void removeGpsNavigationMessageListener(IGpsNavigationMessageListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public List<String> getAllProviders() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public List<String> getProviders(Criteria criteria, boolean enabledOnly) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (criteria != null) {
                        _data.writeInt(1);
                        criteria.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(enabledOnly ? 1 : 0);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getBestProvider(Criteria criteria, boolean enabledOnly) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (criteria != null) {
                        _data.writeInt(1);
                        criteria.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(enabledOnly ? 1 : 0);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean providerMeetsCriteria(String provider, Criteria criteria) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(provider);
                    if (criteria != null) {
                        _data.writeInt(1);
                        criteria.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public ProviderProperties getProviderProperties(String provider) throws RemoteException {
                ProviderProperties _result;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(provider);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = ProviderProperties.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isProviderEnabled(String provider) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(provider);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void addTestProvider(String name, ProviderProperties properties) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(name);
                    if (properties != null) {
                        _data.writeInt(1);
                        properties.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void removeTestProvider(String provider) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(provider);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setTestProviderLocation(String provider, Location loc) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(provider);
                    if (loc != null) {
                        _data.writeInt(1);
                        loc.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(24, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void clearTestProviderLocation(String provider) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(provider);
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setTestProviderEnabled(String provider, boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(provider);
                    _data.writeInt(enabled ? 1 : 0);
                    this.mRemote.transact(26, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void clearTestProviderEnabled(String provider) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(provider);
                    this.mRemote.transact(27, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setTestProviderStatus(String provider, int status, Bundle extras, long updateTime) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(provider);
                    _data.writeInt(status);
                    if (extras != null) {
                        _data.writeInt(1);
                        extras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeLong(updateTime);
                    this.mRemote.transact(28, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void clearTestProviderStatus(String provider) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(provider);
                    this.mRemote.transact(29, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean sendExtraCommand(String provider, String command, Bundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(provider);
                    _data.writeString(command);
                    if (extras != null) {
                        _data.writeInt(1);
                        extras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(30, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    if (_reply.readInt() != 0) {
                        extras.readFromParcel(_reply);
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void reportLocation(Location location, boolean passive) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (location != null) {
                        _data.writeInt(1);
                        location.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(passive ? 1 : 0);
                    this.mRemote.transact(31, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void locationCallbackFinished(ILocationListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(32, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
