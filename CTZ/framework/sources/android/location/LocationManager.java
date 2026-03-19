package android.location;

import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.app.PendingIntent;
import android.content.Context;
import android.location.GnssMeasurementsEvent;
import android.location.GnssNavigationMessage;
import android.location.GnssStatus;
import android.location.GpsMeasurementsEvent;
import android.location.GpsNavigationMessageEvent;
import android.location.GpsStatus;
import android.location.IGnssStatusListener;
import android.location.ILocationListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;
import com.android.internal.location.ProviderProperties;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class LocationManager {
    public static final String EXTRA_GPS_ENABLED = "enabled";
    public static final String FUSED_PROVIDER = "fused";
    public static final String GPS_ENABLED_CHANGE_ACTION = "android.location.GPS_ENABLED_CHANGE";
    public static final String GPS_FIX_CHANGE_ACTION = "android.location.GPS_FIX_CHANGE";
    public static final String GPS_PROVIDER = "gps";
    public static final String HIGH_POWER_REQUEST_CHANGE_ACTION = "android.location.HIGH_POWER_REQUEST_CHANGE";
    public static final String KEY_LOCATION_CHANGED = "location";
    public static final String KEY_PROVIDER_ENABLED = "providerEnabled";
    public static final String KEY_PROXIMITY_ENTERING = "entering";
    public static final String KEY_STATUS_CHANGED = "status";
    public static final String METADATA_SETTINGS_FOOTER_STRING = "com.android.settings.location.FOOTER_STRING";
    public static final String MODE_CHANGED_ACTION = "android.location.MODE_CHANGED";
    public static final String MODE_CHANGING_ACTION = "com.android.settings.location.MODE_CHANGING";
    public static final String NETWORK_PROVIDER = "network";
    public static final String PASSIVE_PROVIDER = "passive";
    public static final String PROVIDERS_CHANGED_ACTION = "android.location.PROVIDERS_CHANGED";
    public static final String SETTINGS_FOOTER_DISPLAYED_ACTION = "com.android.settings.location.DISPLAYED_FOOTER";
    public static final String SETTINGS_FOOTER_REMOVED_ACTION = "com.android.settings.location.REMOVED_FOOTER";
    private static final String TAG = "LocationManager";
    private final BatchedLocationCallbackTransport mBatchedLocationCallbackTransport;
    private final Context mContext;
    private final GnssMeasurementCallbackTransport mGnssMeasurementCallbackTransport;
    private final GnssNavigationMessageCallbackTransport mGnssNavigationMessageCallbackTransport;
    private volatile GnssStatus mGnssStatus;
    private final ILocationManager mService;
    private int mTimeToFirstFix;
    private final HashMap<GpsStatus.Listener, GnssStatusListenerTransport> mGpsStatusListeners = new HashMap<>();
    private final HashMap<GpsStatus.NmeaListener, GnssStatusListenerTransport> mGpsNmeaListeners = new HashMap<>();
    private final HashMap<GnssStatus.Callback, GnssStatusListenerTransport> mGnssStatusListeners = new HashMap<>();
    private final HashMap<OnNmeaMessageListener, GnssStatusListenerTransport> mGnssNmeaListeners = new HashMap<>();
    private HashMap<LocationListener, ListenerTransport> mListeners = new HashMap<>();

    private class ListenerTransport extends ILocationListener.Stub {
        private static final int TYPE_LOCATION_CHANGED = 1;
        private static final int TYPE_PROVIDER_DISABLED = 4;
        private static final int TYPE_PROVIDER_ENABLED = 3;
        private static final int TYPE_STATUS_CHANGED = 2;
        private LocationListener mListener;
        private final Handler mListenerHandler;

        ListenerTransport(LocationListener locationListener, Looper looper) {
            this.mListener = locationListener;
            if (looper == null) {
                this.mListenerHandler = new Handler() {
                    @Override
                    public void handleMessage(Message message) {
                        ListenerTransport.this._handleMessage(message);
                    }
                };
            } else {
                this.mListenerHandler = new Handler(looper) {
                    @Override
                    public void handleMessage(Message message) {
                        ListenerTransport.this._handleMessage(message);
                    }
                };
            }
        }

        @Override
        public void onLocationChanged(Location location) {
            Message messageObtain = Message.obtain();
            messageObtain.what = 1;
            messageObtain.obj = location;
            this.mListenerHandler.sendMessage(messageObtain);
        }

        @Override
        public void onStatusChanged(String str, int i, Bundle bundle) {
            Message messageObtain = Message.obtain();
            messageObtain.what = 2;
            Bundle bundle2 = new Bundle();
            bundle2.putString("provider", str);
            bundle2.putInt("status", i);
            if (bundle != null) {
                bundle2.putBundle("extras", bundle);
            }
            messageObtain.obj = bundle2;
            this.mListenerHandler.sendMessage(messageObtain);
        }

        @Override
        public void onProviderEnabled(String str) {
            Message messageObtain = Message.obtain();
            messageObtain.what = 3;
            messageObtain.obj = str;
            this.mListenerHandler.sendMessage(messageObtain);
        }

        @Override
        public void onProviderDisabled(String str) {
            Message messageObtain = Message.obtain();
            messageObtain.what = 4;
            messageObtain.obj = str;
            this.mListenerHandler.sendMessage(messageObtain);
        }

        private void _handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    this.mListener.onLocationChanged(new Location((Location) message.obj));
                    break;
                case 2:
                    Bundle bundle = (Bundle) message.obj;
                    this.mListener.onStatusChanged(bundle.getString("provider"), bundle.getInt("status"), bundle.getBundle("extras"));
                    break;
                case 3:
                    this.mListener.onProviderEnabled((String) message.obj);
                    break;
                case 4:
                    this.mListener.onProviderDisabled((String) message.obj);
                    break;
            }
            try {
                LocationManager.this.mService.locationCallbackFinished(this);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public String[] getBackgroundThrottlingWhitelist() {
        try {
            return this.mService.getBackgroundThrottlingWhitelist();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public LocationManager(Context context, ILocationManager iLocationManager) {
        this.mService = iLocationManager;
        this.mContext = context;
        this.mGnssMeasurementCallbackTransport = new GnssMeasurementCallbackTransport(this.mContext, this.mService);
        this.mGnssNavigationMessageCallbackTransport = new GnssNavigationMessageCallbackTransport(this.mContext, this.mService);
        this.mBatchedLocationCallbackTransport = new BatchedLocationCallbackTransport(this.mContext, this.mService);
    }

    private LocationProvider createProvider(String str, ProviderProperties providerProperties) {
        return new LocationProvider(str, providerProperties);
    }

    public List<String> getAllProviders() {
        try {
            return this.mService.getAllProviders();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public List<String> getProviders(boolean z) {
        try {
            return this.mService.getProviders(null, z);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public LocationProvider getProvider(String str) {
        checkProvider(str);
        try {
            ProviderProperties providerProperties = this.mService.getProviderProperties(str);
            if (providerProperties == null) {
                return null;
            }
            return createProvider(str, providerProperties);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public List<String> getProviders(Criteria criteria, boolean z) {
        checkCriteria(criteria);
        try {
            return this.mService.getProviders(criteria, z);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public String getBestProvider(Criteria criteria, boolean z) {
        checkCriteria(criteria);
        try {
            return this.mService.getBestProvider(criteria, z);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void requestLocationUpdates(String str, long j, float f, LocationListener locationListener) {
        checkProvider(str);
        checkListener(locationListener);
        requestLocationUpdates(LocationRequest.createFromDeprecatedProvider(str, j, f, false), locationListener, (Looper) null, (PendingIntent) null);
    }

    public void requestLocationUpdates(String str, long j, float f, LocationListener locationListener, Looper looper) {
        checkProvider(str);
        checkListener(locationListener);
        requestLocationUpdates(LocationRequest.createFromDeprecatedProvider(str, j, f, false), locationListener, looper, (PendingIntent) null);
    }

    public void requestLocationUpdates(long j, float f, Criteria criteria, LocationListener locationListener, Looper looper) {
        checkCriteria(criteria);
        checkListener(locationListener);
        requestLocationUpdates(LocationRequest.createFromDeprecatedCriteria(criteria, j, f, false), locationListener, looper, (PendingIntent) null);
    }

    public void requestLocationUpdates(String str, long j, float f, PendingIntent pendingIntent) {
        checkProvider(str);
        checkPendingIntent(pendingIntent);
        requestLocationUpdates(LocationRequest.createFromDeprecatedProvider(str, j, f, false), (LocationListener) null, (Looper) null, pendingIntent);
    }

    public void requestLocationUpdates(long j, float f, Criteria criteria, PendingIntent pendingIntent) {
        checkCriteria(criteria);
        checkPendingIntent(pendingIntent);
        requestLocationUpdates(LocationRequest.createFromDeprecatedCriteria(criteria, j, f, false), (LocationListener) null, (Looper) null, pendingIntent);
    }

    public void requestSingleUpdate(String str, LocationListener locationListener, Looper looper) {
        checkProvider(str);
        checkListener(locationListener);
        requestLocationUpdates(LocationRequest.createFromDeprecatedProvider(str, 0L, 0.0f, true), locationListener, looper, (PendingIntent) null);
    }

    public void requestSingleUpdate(Criteria criteria, LocationListener locationListener, Looper looper) {
        checkCriteria(criteria);
        checkListener(locationListener);
        requestLocationUpdates(LocationRequest.createFromDeprecatedCriteria(criteria, 0L, 0.0f, true), locationListener, looper, (PendingIntent) null);
    }

    public void requestSingleUpdate(String str, PendingIntent pendingIntent) {
        checkProvider(str);
        checkPendingIntent(pendingIntent);
        requestLocationUpdates(LocationRequest.createFromDeprecatedProvider(str, 0L, 0.0f, true), (LocationListener) null, (Looper) null, pendingIntent);
    }

    public void requestSingleUpdate(Criteria criteria, PendingIntent pendingIntent) {
        checkCriteria(criteria);
        checkPendingIntent(pendingIntent);
        requestLocationUpdates(LocationRequest.createFromDeprecatedCriteria(criteria, 0L, 0.0f, true), (LocationListener) null, (Looper) null, pendingIntent);
    }

    @SystemApi
    public void requestLocationUpdates(LocationRequest locationRequest, LocationListener locationListener, Looper looper) {
        checkListener(locationListener);
        requestLocationUpdates(locationRequest, locationListener, looper, (PendingIntent) null);
    }

    @SystemApi
    public void requestLocationUpdates(LocationRequest locationRequest, PendingIntent pendingIntent) {
        checkPendingIntent(pendingIntent);
        requestLocationUpdates(locationRequest, (LocationListener) null, (Looper) null, pendingIntent);
    }

    public boolean injectLocation(Location location) {
        try {
            return this.mService.injectLocation(location);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private ListenerTransport wrapListener(LocationListener locationListener, Looper looper) {
        ListenerTransport listenerTransport;
        if (locationListener == null) {
            return null;
        }
        synchronized (this.mListeners) {
            listenerTransport = this.mListeners.get(locationListener);
            if (listenerTransport == null) {
                listenerTransport = new ListenerTransport(locationListener, looper);
            }
            this.mListeners.put(locationListener, listenerTransport);
        }
        return listenerTransport;
    }

    private void requestLocationUpdates(LocationRequest locationRequest, LocationListener locationListener, Looper looper, PendingIntent pendingIntent) {
        String packageName = this.mContext.getPackageName();
        try {
            this.mService.requestLocationUpdates(locationRequest, wrapListener(locationListener, looper), pendingIntent, packageName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void removeUpdates(LocationListener locationListener) {
        ListenerTransport listenerTransportRemove;
        checkListener(locationListener);
        String packageName = this.mContext.getPackageName();
        synchronized (this.mListeners) {
            listenerTransportRemove = this.mListeners.remove(locationListener);
        }
        if (listenerTransportRemove == null) {
            return;
        }
        try {
            this.mService.removeUpdates(listenerTransportRemove, null, packageName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void removeUpdates(PendingIntent pendingIntent) {
        checkPendingIntent(pendingIntent);
        try {
            this.mService.removeUpdates(null, pendingIntent, this.mContext.getPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void addProximityAlert(double d, double d2, float f, long j, PendingIntent pendingIntent) {
        checkPendingIntent(pendingIntent);
        if (j < 0) {
            j = Long.MAX_VALUE;
        }
        Geofence geofenceCreateCircle = Geofence.createCircle(d, d2, f);
        try {
            this.mService.requestGeofence(new LocationRequest().setExpireIn(j), geofenceCreateCircle, pendingIntent, this.mContext.getPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void addGeofence(LocationRequest locationRequest, Geofence geofence, PendingIntent pendingIntent) {
        checkPendingIntent(pendingIntent);
        checkGeofence(geofence);
        try {
            this.mService.requestGeofence(locationRequest, geofence, pendingIntent, this.mContext.getPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void removeProximityAlert(PendingIntent pendingIntent) {
        checkPendingIntent(pendingIntent);
        try {
            this.mService.removeGeofence(null, pendingIntent, this.mContext.getPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void removeGeofence(Geofence geofence, PendingIntent pendingIntent) {
        checkPendingIntent(pendingIntent);
        checkGeofence(geofence);
        try {
            this.mService.removeGeofence(geofence, pendingIntent, this.mContext.getPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void removeAllGeofences(PendingIntent pendingIntent) {
        checkPendingIntent(pendingIntent);
        try {
            this.mService.removeGeofence(null, pendingIntent, this.mContext.getPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isLocationEnabled() {
        return isLocationEnabledForUser(Process.myUserHandle());
    }

    @SystemApi
    public void setLocationEnabledForUser(boolean z, UserHandle userHandle) {
        try {
            this.mService.setLocationEnabledForUser(z, userHandle.getIdentifier());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public boolean isLocationEnabledForUser(UserHandle userHandle) {
        try {
            return this.mService.isLocationEnabledForUser(userHandle.getIdentifier());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isProviderEnabled(String str) {
        return isProviderEnabledForUser(str, Process.myUserHandle());
    }

    @SystemApi
    public boolean isProviderEnabledForUser(String str, UserHandle userHandle) {
        checkProvider(str);
        try {
            return this.mService.isProviderEnabledForUser(str, userHandle.getIdentifier());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public boolean setProviderEnabledForUser(String str, boolean z, UserHandle userHandle) {
        checkProvider(str);
        try {
            return this.mService.setProviderEnabledForUser(str, z, userHandle.getIdentifier());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Location getLastLocation() {
        try {
            return this.mService.getLastLocation(null, this.mContext.getPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Location getLastKnownLocation(String str) {
        checkProvider(str);
        String packageName = this.mContext.getPackageName();
        try {
            return this.mService.getLastLocation(LocationRequest.createFromDeprecatedProvider(str, 0L, 0.0f, true), packageName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void addTestProvider(String str, boolean z, boolean z2, boolean z3, boolean z4, boolean z5, boolean z6, boolean z7, int i, int i2) {
        ProviderProperties providerProperties = new ProviderProperties(z, z2, z3, z4, z5, z6, z7, i, i2);
        if (str.matches(LocationProvider.BAD_CHARS_REGEX)) {
            throw new IllegalArgumentException("provider name contains illegal character: " + str);
        }
        try {
            this.mService.addTestProvider(str, providerProperties, this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void removeTestProvider(String str) {
        try {
            this.mService.removeTestProvider(str, this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setTestProviderLocation(String str, Location location) {
        if (!location.isComplete()) {
            IllegalArgumentException illegalArgumentException = new IllegalArgumentException("Incomplete location object, missing timestamp or accuracy? " + location);
            if (this.mContext.getApplicationInfo().targetSdkVersion <= 16) {
                Log.w(TAG, illegalArgumentException);
                location.makeComplete();
            } else {
                throw illegalArgumentException;
            }
        }
        try {
            this.mService.setTestProviderLocation(str, location, this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void clearTestProviderLocation(String str) {
        try {
            this.mService.clearTestProviderLocation(str, this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setTestProviderEnabled(String str, boolean z) {
        try {
            this.mService.setTestProviderEnabled(str, z, this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void clearTestProviderEnabled(String str) {
        try {
            this.mService.clearTestProviderEnabled(str, this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setTestProviderStatus(String str, int i, Bundle bundle, long j) {
        try {
            this.mService.setTestProviderStatus(str, i, bundle, j, this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void clearTestProviderStatus(String str) {
        try {
            this.mService.clearTestProviderStatus(str, this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private class GnssStatusListenerTransport extends IGnssStatusListener.Stub {
        private static final int NMEA_RECEIVED = 1000;
        private final GnssStatus.Callback mGnssCallback;
        private final Handler mGnssHandler;
        private final OnNmeaMessageListener mGnssNmeaListener;
        private final GpsStatus.Listener mGpsListener;
        private final GpsStatus.NmeaListener mGpsNmeaListener;
        private final ArrayList<Nmea> mNmeaBuffer;

        private class GnssHandler extends Handler {
            public GnssHandler(Handler handler) {
                super(handler != null ? handler.getLooper() : Looper.myLooper());
            }

            @Override
            public void handleMessage(Message message) {
                int i = message.what;
                if (i != 1000) {
                    switch (i) {
                        case 1:
                            GnssStatusListenerTransport.this.mGnssCallback.onStarted();
                            return;
                        case 2:
                            GnssStatusListenerTransport.this.mGnssCallback.onStopped();
                            return;
                        case 3:
                            GnssStatusListenerTransport.this.mGnssCallback.onFirstFix(LocationManager.this.mTimeToFirstFix);
                            return;
                        case 4:
                            GnssStatusListenerTransport.this.mGnssCallback.onSatelliteStatusChanged(LocationManager.this.mGnssStatus);
                            return;
                        default:
                            return;
                    }
                }
                synchronized (GnssStatusListenerTransport.this.mNmeaBuffer) {
                    int size = GnssStatusListenerTransport.this.mNmeaBuffer.size();
                    for (int i2 = 0; i2 < size; i2++) {
                        Nmea nmea = (Nmea) GnssStatusListenerTransport.this.mNmeaBuffer.get(i2);
                        GnssStatusListenerTransport.this.mGnssNmeaListener.onNmeaMessage(nmea.mNmea, nmea.mTimestamp);
                    }
                    GnssStatusListenerTransport.this.mNmeaBuffer.clear();
                }
            }
        }

        private class Nmea {
            String mNmea;
            long mTimestamp;

            Nmea(long j, String str) {
                this.mTimestamp = j;
                this.mNmea = str;
            }
        }

        GnssStatusListenerTransport(LocationManager locationManager, GpsStatus.Listener listener) {
            this(listener, (Handler) null);
        }

        GnssStatusListenerTransport(GpsStatus.Listener listener, Handler handler) {
            this.mGpsListener = listener;
            this.mGnssHandler = new GnssHandler(handler);
            this.mGpsNmeaListener = null;
            this.mNmeaBuffer = null;
            this.mGnssCallback = this.mGpsListener != null ? new GnssStatus.Callback() {
                @Override
                public void onStarted() {
                    GnssStatusListenerTransport.this.mGpsListener.onGpsStatusChanged(1);
                }

                @Override
                public void onStopped() {
                    GnssStatusListenerTransport.this.mGpsListener.onGpsStatusChanged(2);
                }

                @Override
                public void onFirstFix(int i) {
                    GnssStatusListenerTransport.this.mGpsListener.onGpsStatusChanged(3);
                }

                @Override
                public void onSatelliteStatusChanged(GnssStatus gnssStatus) {
                    GnssStatusListenerTransport.this.mGpsListener.onGpsStatusChanged(4);
                }
            } : null;
            this.mGnssNmeaListener = null;
        }

        GnssStatusListenerTransport(LocationManager locationManager, GpsStatus.NmeaListener nmeaListener) {
            this(nmeaListener, (Handler) null);
        }

        GnssStatusListenerTransport(GpsStatus.NmeaListener nmeaListener, Handler handler) {
            this.mGpsListener = null;
            this.mGnssHandler = new GnssHandler(handler);
            this.mGpsNmeaListener = nmeaListener;
            this.mNmeaBuffer = new ArrayList<>();
            this.mGnssCallback = null;
            this.mGnssNmeaListener = this.mGpsNmeaListener != null ? new OnNmeaMessageListener() {
                @Override
                public void onNmeaMessage(String str, long j) {
                    GnssStatusListenerTransport.this.mGpsNmeaListener.onNmeaReceived(j, str);
                }
            } : null;
        }

        GnssStatusListenerTransport(LocationManager locationManager, GnssStatus.Callback callback) {
            this(callback, (Handler) null);
        }

        GnssStatusListenerTransport(GnssStatus.Callback callback, Handler handler) {
            this.mGnssCallback = callback;
            this.mGnssHandler = new GnssHandler(handler);
            this.mGnssNmeaListener = null;
            this.mNmeaBuffer = null;
            this.mGpsListener = null;
            this.mGpsNmeaListener = null;
        }

        GnssStatusListenerTransport(LocationManager locationManager, OnNmeaMessageListener onNmeaMessageListener) {
            this(onNmeaMessageListener, (Handler) null);
        }

        GnssStatusListenerTransport(OnNmeaMessageListener onNmeaMessageListener, Handler handler) {
            this.mGnssCallback = null;
            this.mGnssHandler = new GnssHandler(handler);
            this.mGnssNmeaListener = onNmeaMessageListener;
            this.mGpsListener = null;
            this.mGpsNmeaListener = null;
            this.mNmeaBuffer = new ArrayList<>();
        }

        @Override
        public void onGnssStarted() {
            if (this.mGnssCallback != null) {
                Message messageObtain = Message.obtain();
                messageObtain.what = 1;
                this.mGnssHandler.sendMessage(messageObtain);
            }
        }

        @Override
        public void onGnssStopped() {
            if (this.mGnssCallback != null) {
                Message messageObtain = Message.obtain();
                messageObtain.what = 2;
                this.mGnssHandler.sendMessage(messageObtain);
            }
        }

        @Override
        public void onFirstFix(int i) {
            if (this.mGnssCallback != null) {
                LocationManager.this.mTimeToFirstFix = i;
                Message messageObtain = Message.obtain();
                messageObtain.what = 3;
                this.mGnssHandler.sendMessage(messageObtain);
            }
        }

        @Override
        public void onSvStatusChanged(int i, int[] iArr, float[] fArr, float[] fArr2, float[] fArr3, float[] fArr4) {
            if (this.mGnssCallback != null) {
                LocationManager.this.mGnssStatus = new GnssStatus(i, iArr, fArr, fArr2, fArr3, fArr4);
                Message messageObtain = Message.obtain();
                messageObtain.what = 4;
                this.mGnssHandler.removeMessages(4);
                this.mGnssHandler.sendMessage(messageObtain);
            }
        }

        @Override
        public void onNmeaReceived(long j, String str) {
            if (this.mGnssNmeaListener != null) {
                synchronized (this.mNmeaBuffer) {
                    this.mNmeaBuffer.add(new Nmea(j, str));
                }
                Message messageObtain = Message.obtain();
                messageObtain.what = 1000;
                this.mGnssHandler.removeMessages(1000);
                this.mGnssHandler.sendMessage(messageObtain);
            }
        }
    }

    @Deprecated
    public boolean addGpsStatusListener(GpsStatus.Listener listener) {
        if (this.mGpsStatusListeners.get(listener) != null) {
            return true;
        }
        try {
            GnssStatusListenerTransport gnssStatusListenerTransport = new GnssStatusListenerTransport(this, listener);
            boolean zRegisterGnssStatusCallback = this.mService.registerGnssStatusCallback(gnssStatusListenerTransport, this.mContext.getPackageName());
            if (zRegisterGnssStatusCallback) {
                this.mGpsStatusListeners.put(listener, gnssStatusListenerTransport);
            }
            return zRegisterGnssStatusCallback;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public void removeGpsStatusListener(GpsStatus.Listener listener) {
        try {
            GnssStatusListenerTransport gnssStatusListenerTransportRemove = this.mGpsStatusListeners.remove(listener);
            if (gnssStatusListenerTransportRemove != null) {
                this.mService.unregisterGnssStatusCallback(gnssStatusListenerTransportRemove);
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean registerGnssStatusCallback(GnssStatus.Callback callback) {
        return registerGnssStatusCallback(callback, null);
    }

    public boolean registerGnssStatusCallback(GnssStatus.Callback callback, Handler handler) {
        if (this.mGnssStatusListeners.get(callback) != null) {
            return true;
        }
        try {
            GnssStatusListenerTransport gnssStatusListenerTransport = new GnssStatusListenerTransport(callback, handler);
            boolean zRegisterGnssStatusCallback = this.mService.registerGnssStatusCallback(gnssStatusListenerTransport, this.mContext.getPackageName());
            if (zRegisterGnssStatusCallback) {
                this.mGnssStatusListeners.put(callback, gnssStatusListenerTransport);
            }
            return zRegisterGnssStatusCallback;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void unregisterGnssStatusCallback(GnssStatus.Callback callback) {
        try {
            GnssStatusListenerTransport gnssStatusListenerTransportRemove = this.mGnssStatusListeners.remove(callback);
            if (gnssStatusListenerTransportRemove != null) {
                this.mService.unregisterGnssStatusCallback(gnssStatusListenerTransportRemove);
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public boolean addNmeaListener(GpsStatus.NmeaListener nmeaListener) {
        if (this.mGpsNmeaListeners.get(nmeaListener) != null) {
            return true;
        }
        try {
            GnssStatusListenerTransport gnssStatusListenerTransport = new GnssStatusListenerTransport(this, nmeaListener);
            boolean zRegisterGnssStatusCallback = this.mService.registerGnssStatusCallback(gnssStatusListenerTransport, this.mContext.getPackageName());
            if (zRegisterGnssStatusCallback) {
                this.mGpsNmeaListeners.put(nmeaListener, gnssStatusListenerTransport);
            }
            return zRegisterGnssStatusCallback;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public void removeNmeaListener(GpsStatus.NmeaListener nmeaListener) {
        try {
            GnssStatusListenerTransport gnssStatusListenerTransportRemove = this.mGpsNmeaListeners.remove(nmeaListener);
            if (gnssStatusListenerTransportRemove != null) {
                this.mService.unregisterGnssStatusCallback(gnssStatusListenerTransportRemove);
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean addNmeaListener(OnNmeaMessageListener onNmeaMessageListener) {
        return addNmeaListener(onNmeaMessageListener, null);
    }

    public boolean addNmeaListener(OnNmeaMessageListener onNmeaMessageListener, Handler handler) {
        if (this.mGpsNmeaListeners.get(onNmeaMessageListener) != null) {
            return true;
        }
        try {
            GnssStatusListenerTransport gnssStatusListenerTransport = new GnssStatusListenerTransport(onNmeaMessageListener, handler);
            boolean zRegisterGnssStatusCallback = this.mService.registerGnssStatusCallback(gnssStatusListenerTransport, this.mContext.getPackageName());
            if (zRegisterGnssStatusCallback) {
                this.mGnssNmeaListeners.put(onNmeaMessageListener, gnssStatusListenerTransport);
            }
            return zRegisterGnssStatusCallback;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void removeNmeaListener(OnNmeaMessageListener onNmeaMessageListener) {
        try {
            GnssStatusListenerTransport gnssStatusListenerTransportRemove = this.mGnssNmeaListeners.remove(onNmeaMessageListener);
            if (gnssStatusListenerTransportRemove != null) {
                this.mService.unregisterGnssStatusCallback(gnssStatusListenerTransportRemove);
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    @SuppressLint({"Doclava125"})
    @Deprecated
    public boolean addGpsMeasurementListener(GpsMeasurementsEvent.Listener listener) {
        return false;
    }

    public boolean registerGnssMeasurementsCallback(GnssMeasurementsEvent.Callback callback) {
        return registerGnssMeasurementsCallback(callback, null);
    }

    public boolean registerGnssMeasurementsCallback(GnssMeasurementsEvent.Callback callback, Handler handler) {
        return this.mGnssMeasurementCallbackTransport.add(callback, handler);
    }

    @SystemApi
    @SuppressLint({"Doclava125"})
    @Deprecated
    public void removeGpsMeasurementListener(GpsMeasurementsEvent.Listener listener) {
    }

    public void unregisterGnssMeasurementsCallback(GnssMeasurementsEvent.Callback callback) {
        this.mGnssMeasurementCallbackTransport.remove(callback);
    }

    @SystemApi
    @SuppressLint({"Doclava125"})
    @Deprecated
    public boolean addGpsNavigationMessageListener(GpsNavigationMessageEvent.Listener listener) {
        return false;
    }

    @SystemApi
    @SuppressLint({"Doclava125"})
    @Deprecated
    public void removeGpsNavigationMessageListener(GpsNavigationMessageEvent.Listener listener) {
    }

    public boolean registerGnssNavigationMessageCallback(GnssNavigationMessage.Callback callback) {
        return registerGnssNavigationMessageCallback(callback, null);
    }

    public boolean registerGnssNavigationMessageCallback(GnssNavigationMessage.Callback callback, Handler handler) {
        return this.mGnssNavigationMessageCallbackTransport.add(callback, handler);
    }

    public void unregisterGnssNavigationMessageCallback(GnssNavigationMessage.Callback callback) {
        this.mGnssNavigationMessageCallbackTransport.remove(callback);
    }

    @Deprecated
    public GpsStatus getGpsStatus(GpsStatus gpsStatus) {
        if (gpsStatus == null) {
            gpsStatus = new GpsStatus();
        }
        if (this.mGnssStatus != null) {
            gpsStatus.setStatus(this.mGnssStatus, this.mTimeToFirstFix);
        }
        return gpsStatus;
    }

    public int getGnssYearOfHardware() {
        try {
            return this.mService.getGnssYearOfHardware();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public String getGnssHardwareModelName() {
        try {
            return this.mService.getGnssHardwareModelName();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public int getGnssBatchSize() {
        try {
            return this.mService.getGnssBatchSize(this.mContext.getPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public boolean registerGnssBatchedLocationCallback(long j, boolean z, BatchedLocationCallback batchedLocationCallback, Handler handler) {
        this.mBatchedLocationCallbackTransport.add(batchedLocationCallback, handler);
        try {
            return this.mService.startGnssBatch(j, z, this.mContext.getPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public void flushGnssBatch() {
        try {
            this.mService.flushGnssBatch(this.mContext.getPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public boolean unregisterGnssBatchedLocationCallback(BatchedLocationCallback batchedLocationCallback) {
        this.mBatchedLocationCallbackTransport.remove(batchedLocationCallback);
        try {
            return this.mService.stopGnssBatch();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean sendExtraCommand(String str, String str2, Bundle bundle) {
        try {
            return this.mService.sendExtraCommand(str, str2, bundle);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean sendNiResponse(int i, int i2) {
        try {
            return this.mService.sendNiResponse(i, i2);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private static void checkProvider(String str) {
        if (str == null) {
            throw new IllegalArgumentException("invalid provider: " + str);
        }
    }

    private static void checkCriteria(Criteria criteria) {
        if (criteria == null) {
            throw new IllegalArgumentException("invalid criteria: " + criteria);
        }
    }

    private static void checkListener(LocationListener locationListener) {
        if (locationListener == null) {
            throw new IllegalArgumentException("invalid listener: " + locationListener);
        }
    }

    private void checkPendingIntent(PendingIntent pendingIntent) {
        if (pendingIntent == null) {
            throw new IllegalArgumentException("invalid pending intent: " + pendingIntent);
        }
        if (!pendingIntent.isTargetedToPackage()) {
            IllegalArgumentException illegalArgumentException = new IllegalArgumentException("pending intent must be targeted to package");
            if (this.mContext.getApplicationInfo().targetSdkVersion > 16) {
                throw illegalArgumentException;
            }
            Log.w(TAG, illegalArgumentException);
        }
    }

    private static void checkGeofence(Geofence geofence) {
        if (geofence == null) {
            throw new IllegalArgumentException("invalid geofence: " + geofence);
        }
    }
}
