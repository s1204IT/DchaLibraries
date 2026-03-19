package com.android.location.provider;

import android.location.Address;
import android.location.GeocoderParams;
import android.location.IGeocodeProvider;
import android.os.IBinder;
import java.util.List;

public abstract class GeocodeProvider {
    private IGeocodeProvider.Stub mProvider = new IGeocodeProvider.Stub() {
        public String getFromLocation(double d, double d2, int i, GeocoderParams geocoderParams, List<Address> list) {
            return GeocodeProvider.this.onGetFromLocation(d, d2, i, geocoderParams, list);
        }

        public String getFromLocationName(String str, double d, double d2, double d3, double d4, int i, GeocoderParams geocoderParams, List<Address> list) {
            return GeocodeProvider.this.onGetFromLocationName(str, d, d2, d3, d4, i, geocoderParams, list);
        }
    };

    public abstract String onGetFromLocation(double d, double d2, int i, GeocoderParams geocoderParams, List<Address> list);

    public abstract String onGetFromLocationName(String str, double d, double d2, double d3, double d4, int i, GeocoderParams geocoderParams, List<Address> list);

    public IBinder getBinder() {
        return this.mProvider;
    }
}
