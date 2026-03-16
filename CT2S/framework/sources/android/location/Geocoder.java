package android.location;

import android.content.Context;
import android.location.ILocationManager;
import android.os.IBinder;
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
        IBinder b = ServiceManager.getService("location");
        ILocationManager lm = ILocationManager.Stub.asInterface(b);
        try {
            return lm.geocoderIsPresent();
        } catch (RemoteException e) {
            Log.e(TAG, "isPresent: got RemoteException", e);
            return false;
        }
    }

    public Geocoder(Context context, Locale locale) {
        if (locale == null) {
            throw new NullPointerException("locale == null");
        }
        this.mParams = new GeocoderParams(context, locale);
        IBinder b = ServiceManager.getService("location");
        this.mService = ILocationManager.Stub.asInterface(b);
    }

    public Geocoder(Context context) {
        this(context, Locale.getDefault());
    }

    public List<Address> getFromLocation(double latitude, double longitude, int maxResults) throws IOException {
        if (latitude < -90.0d || latitude > 90.0d) {
            throw new IllegalArgumentException("latitude == " + latitude);
        }
        if (longitude < -180.0d || longitude > 180.0d) {
            throw new IllegalArgumentException("longitude == " + longitude);
        }
        try {
            List<Address> results = new ArrayList<>();
            String ex = this.mService.getFromLocation(latitude, longitude, maxResults, this.mParams, results);
            if (ex != null) {
                throw new IOException(ex);
            }
            return results;
        } catch (RemoteException e) {
            Log.e(TAG, "getFromLocation: got RemoteException", e);
            return null;
        }
    }

    public List<Address> getFromLocationName(String locationName, int maxResults) throws IOException {
        if (locationName == null) {
            throw new IllegalArgumentException("locationName == null");
        }
        try {
            List<Address> results = new ArrayList<>();
            String ex = this.mService.getFromLocationName(locationName, 0.0d, 0.0d, 0.0d, 0.0d, maxResults, this.mParams, results);
            if (ex != null) {
                throw new IOException(ex);
            }
            return results;
        } catch (RemoteException e) {
            Log.e(TAG, "getFromLocationName: got RemoteException", e);
            return null;
        }
    }

    public List<Address> getFromLocationName(String locationName, int maxResults, double lowerLeftLatitude, double lowerLeftLongitude, double upperRightLatitude, double upperRightLongitude) throws IOException {
        if (locationName == null) {
            throw new IllegalArgumentException("locationName == null");
        }
        if (lowerLeftLatitude < -90.0d || lowerLeftLatitude > 90.0d) {
            throw new IllegalArgumentException("lowerLeftLatitude == " + lowerLeftLatitude);
        }
        if (lowerLeftLongitude < -180.0d || lowerLeftLongitude > 180.0d) {
            throw new IllegalArgumentException("lowerLeftLongitude == " + lowerLeftLongitude);
        }
        if (upperRightLatitude < -90.0d || upperRightLatitude > 90.0d) {
            throw new IllegalArgumentException("upperRightLatitude == " + upperRightLatitude);
        }
        if (upperRightLongitude < -180.0d || upperRightLongitude > 180.0d) {
            throw new IllegalArgumentException("upperRightLongitude == " + upperRightLongitude);
        }
        try {
            ArrayList<Address> result = new ArrayList<>();
            String ex = this.mService.getFromLocationName(locationName, lowerLeftLatitude, lowerLeftLongitude, upperRightLatitude, upperRightLongitude, maxResults, this.mParams, result);
            if (ex != null) {
                throw new IOException(ex);
            }
            return result;
        } catch (RemoteException e) {
            Log.e(TAG, "getFromLocationName: got RemoteException", e);
            return null;
        }
    }
}
