package android.location;

import android.content.Context;
import android.location.ILocationManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Geocoder {
    private static final String TAG = "Geocoder";
    private GeocoderParams mParams;
    private ILocationManager mService;

    public static boolean isPresent() {
        try {
            return ILocationManager.Stub.asInterface(ServiceManager.getService("location")).geocoderIsPresent();
        } catch (RemoteException | NullPointerException e) {
            Log.e(TAG, "isPresent: got RemoteException", e);
            return false;
        }
    }

    public Geocoder(Context context, Locale locale) {
        if (locale == null) {
            throw new NullPointerException("locale == null");
        }
        this.mParams = new GeocoderParams(context, locale);
        this.mService = ILocationManager.Stub.asInterface(ServiceManager.getService("location"));
    }

    public Geocoder(Context context) {
        this(context, Locale.getDefault());
    }

    public List<Address> getFromLocation(double d, double d2, int i) throws IOException {
        if (d < -90.0d || d > 90.0d) {
            throw new IllegalArgumentException("latitude == " + d);
        }
        if (d2 < -180.0d || d2 > 180.0d) {
            throw new IllegalArgumentException("longitude == " + d2);
        }
        try {
            ArrayList arrayList = new ArrayList();
            String fromLocation = this.mService.getFromLocation(d, d2, i, this.mParams, arrayList);
            if (fromLocation != null) {
                throw new IOException(fromLocation);
            }
            return arrayList;
        } catch (RemoteException e) {
            Log.e(TAG, "getFromLocation: got RemoteException", e);
            return null;
        }
    }

    public List<Address> getFromLocationName(String str, int i) throws IOException {
        if (str == null) {
            throw new IllegalArgumentException("locationName == null");
        }
        try {
            ArrayList arrayList = new ArrayList();
            String fromLocationName = this.mService.getFromLocationName(str, 0.0d, 0.0d, 0.0d, 0.0d, i, this.mParams, arrayList);
            if (fromLocationName != null) {
                throw new IOException(fromLocationName);
            }
            return arrayList;
        } catch (RemoteException e) {
            Log.e(TAG, "getFromLocationName: got RemoteException", e);
            return null;
        }
    }

    public List<Address> getFromLocationName(String str, int i, double d, double d2, double d3, double d4) throws IOException {
        if (str == null) {
            throw new IllegalArgumentException("locationName == null");
        }
        if (d < -90.0d || d > 90.0d) {
            throw new IllegalArgumentException("lowerLeftLatitude == " + d);
        }
        if (d2 < -180.0d || d2 > 180.0d) {
            throw new IllegalArgumentException("lowerLeftLongitude == " + d2);
        }
        if (d3 < -90.0d || d3 > 90.0d) {
            throw new IllegalArgumentException("upperRightLatitude == " + d3);
        }
        if (d4 < -180.0d || d4 > 180.0d) {
            throw new IllegalArgumentException("upperRightLongitude == " + d4);
        }
        try {
            ArrayList arrayList = new ArrayList();
            String fromLocationName = this.mService.getFromLocationName(str, d, d2, d3, d4, i, this.mParams, arrayList);
            if (fromLocationName != null) {
                throw new IOException(fromLocationName);
            }
            return arrayList;
        } catch (RemoteException e) {
            Log.e(TAG, "getFromLocationName: got RemoteException", e);
            return null;
        }
    }
}
