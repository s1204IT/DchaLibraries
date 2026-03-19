package com.android.server.location;

import android.content.Context;
import android.location.Address;
import android.location.GeocoderParams;
import android.location.IGeocodeProvider;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import com.android.server.ServiceWatcher;
import java.util.List;

public class GeocoderProxy {
    private static final String SERVICE_ACTION = "com.android.location.service.GeocodeProvider";
    private static final String TAG = "GeocoderProxy";
    private final Context mContext;
    private final ServiceWatcher mServiceWatcher;

    public static GeocoderProxy createAndBind(Context context, int i, int i2, int i3, Handler handler) {
        GeocoderProxy geocoderProxy = new GeocoderProxy(context, i, i2, i3, handler);
        if (geocoderProxy.bind()) {
            return geocoderProxy;
        }
        return null;
    }

    private GeocoderProxy(Context context, int i, int i2, int i3, Handler handler) {
        this.mContext = context;
        this.mServiceWatcher = new ServiceWatcher(this.mContext, TAG, SERVICE_ACTION, i, i2, i3, null, handler);
    }

    private boolean bind() {
        return this.mServiceWatcher.start();
    }

    public String getConnectedPackageName() {
        return this.mServiceWatcher.getBestPackageName();
    }

    public String getFromLocation(final double d, final double d2, final int i, final GeocoderParams geocoderParams, final List<Address> list) {
        final String[] strArr = {"Service not Available"};
        this.mServiceWatcher.runOnBinder(new ServiceWatcher.BinderRunner() {
            @Override
            public void run(IBinder iBinder) {
                try {
                    strArr[0] = IGeocodeProvider.Stub.asInterface(iBinder).getFromLocation(d, d2, i, geocoderParams, list);
                } catch (RemoteException e) {
                    Log.w(GeocoderProxy.TAG, e);
                }
            }
        });
        return strArr[0];
    }

    public String getFromLocationName(final String str, final double d, final double d2, final double d3, final double d4, final int i, final GeocoderParams geocoderParams, final List<Address> list) {
        final String[] strArr = {"Service not Available"};
        this.mServiceWatcher.runOnBinder(new ServiceWatcher.BinderRunner() {
            @Override
            public void run(IBinder iBinder) {
                try {
                    strArr[0] = IGeocodeProvider.Stub.asInterface(iBinder).getFromLocationName(str, d, d2, d3, d4, i, geocoderParams, list);
                } catch (RemoteException e) {
                    Log.w(GeocoderProxy.TAG, e);
                }
            }
        });
        return strArr[0];
    }
}
