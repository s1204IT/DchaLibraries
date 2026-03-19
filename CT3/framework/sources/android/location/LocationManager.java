package android.location;

import android.app.PendingIntent;
import android.content.Context;
import android.location.GnssMeasurementsEvent;
import android.location.GnssNavigationMessage;
import android.location.GnssNavigationMessageEvent;
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
import android.os.RemoteException;
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
    public static final String MODE_CHANGED_ACTION = "android.location.MODE_CHANGED";
    public static final String NETWORK_PROVIDER = "network";
    public static final String PASSIVE_PROVIDER = "passive";
    public static final String PROVIDERS_CHANGED_ACTION = "android.location.PROVIDERS_CHANGED";
    private static final String TAG = "LocationManager";
    private final Context mContext;
    private final GnssMeasurementCallbackTransport mGnssMeasurementCallbackTransport;
    private final GnssNavigationMessageCallbackTransport mGnssNavigationMessageCallbackTransport;
    private GnssStatus mGnssStatus;
    private final ILocationManager mService;
    private int mTimeToFirstFix;
    private final HashMap<GpsStatus.Listener, GnssStatusListenerTransport> mGpsStatusListeners = new HashMap<>();
    private final HashMap<GpsStatus.NmeaListener, GnssStatusListenerTransport> mGpsNmeaListeners = new HashMap<>();
    private final HashMap<GnssStatusCallback, GnssStatusListenerTransport> mOldGnssStatusListeners = new HashMap<>();
    private final HashMap<GnssStatus.Callback, GnssStatusListenerTransport> mGnssStatusListeners = new HashMap<>();
    private final HashMap<GnssNmeaListener, GnssStatusListenerTransport> mOldGnssNmeaListeners = new HashMap<>();
    private final HashMap<OnNmeaMessageListener, GnssStatusListenerTransport> mGnssNmeaListeners = new HashMap<>();
    private final HashMap<GnssNavigationMessageEvent.Callback, GnssNavigationMessage.Callback> mNavigationMessageBridge = new HashMap<>();
    private HashMap<LocationListener, ListenerTransport> mListeners = new HashMap<>();

    private class ListenerTransport extends ILocationListener.Stub {
        private static final int TYPE_LOCATION_CHANGED = 1;
        private static final int TYPE_PROVIDER_DISABLED = 4;
        private static final int TYPE_PROVIDER_ENABLED = 3;
        private static final int TYPE_STATUS_CHANGED = 2;
        private LocationListener mListener;
        private final Handler mListenerHandler;

        ListenerTransport(LocationListener listener, Looper looper) {
            this.mListener = listener;
            if (looper == null) {
                this.mListenerHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        ListenerTransport.this._handleMessage(msg);
                    }
                };
            } else {
                this.mListenerHandler = new Handler(looper) {
                    @Override
                    public void handleMessage(Message msg) {
                        ListenerTransport.this._handleMessage(msg);
                    }
                };
            }
        }

        @Override
        public void onLocationChanged(Location location) {
            Message msg = Message.obtain();
            msg.what = 1;
            msg.obj = location;
            this.mListenerHandler.sendMessage(msg);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Message msg = Message.obtain();
            msg.what = 2;
            Bundle b = new Bundle();
            b.putString("provider", provider);
            b.putInt("status", status);
            if (extras != null) {
                b.putBundle("extras", extras);
            }
            msg.obj = b;
            this.mListenerHandler.sendMessage(msg);
        }

        @Override
        public void onProviderEnabled(String provider) {
            Message msg = Message.obtain();
            msg.what = 3;
            msg.obj = provider;
            this.mListenerHandler.sendMessage(msg);
        }

        @Override
        public void onProviderDisabled(String provider) {
            Message msg = Message.obtain();
            msg.what = 4;
            msg.obj = provider;
            this.mListenerHandler.sendMessage(msg);
        }

        private void _handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    Location location = new Location((Location) msg.obj);
                    this.mListener.onLocationChanged(location);
                    break;
                case 2:
                    Bundle b = (Bundle) msg.obj;
                    String provider = b.getString("provider");
                    int status = b.getInt("status");
                    Bundle extras = b.getBundle("extras");
                    this.mListener.onStatusChanged(provider, status, extras);
                    break;
                case 3:
                    this.mListener.onProviderEnabled((String) msg.obj);
                    break;
                case 4:
                    this.mListener.onProviderDisabled((String) msg.obj);
                    break;
            }
            try {
                LocationManager.this.mService.locationCallbackFinished(this);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public LocationManager(Context context, ILocationManager service) {
        this.mService = service;
        this.mContext = context;
        this.mGnssMeasurementCallbackTransport = new GnssMeasurementCallbackTransport(this.mContext, this.mService);
        this.mGnssNavigationMessageCallbackTransport = new GnssNavigationMessageCallbackTransport(this.mContext, this.mService);
    }

    private LocationProvider createProvider(String name, ProviderProperties properties) {
        return new LocationProvider(name, properties);
    }

    public List<String> getAllProviders() {
        try {
            List<String> list = this.mService.getAllProviders();
            list.remove(NETWORK_PROVIDER);
            return list;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public List<String> getProviders(boolean enabledOnly) {
        try {
            List<String> list = this.mService.getProviders(null, enabledOnly);
            list.remove(NETWORK_PROVIDER);
            return list;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public LocationProvider getProvider(String name) {
        checkProvider(name);
        try {
            ProviderProperties properties = this.mService.getProviderProperties(name);
            if (properties == null) {
                return null;
            }
            return createProvider(name, properties);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public List<String> getProviders(Criteria criteria, boolean enabledOnly) {
        checkCriteria(criteria);
        try {
            return this.mService.getProviders(criteria, enabledOnly);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public String getBestProvider(Criteria criteria, boolean enabledOnly) {
        checkCriteria(criteria);
        try {
            return this.mService.getBestProvider(criteria, enabledOnly);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void requestLocationUpdates(String provider, long minTime, float minDistance, LocationListener listener) {
        checkProvider(provider);
        checkListener(listener);
        LocationRequest request = LocationRequest.createFromDeprecatedProvider(provider, minTime, minDistance, false);
        requestLocationUpdates(request, listener, (Looper) null, (PendingIntent) null);
    }

    public void requestLocationUpdates(String provider, long minTime, float minDistance, LocationListener listener, Looper looper) {
        checkProvider(provider);
        checkListener(listener);
        LocationRequest request = LocationRequest.createFromDeprecatedProvider(provider, minTime, minDistance, false);
        requestLocationUpdates(request, listener, looper, (PendingIntent) null);
    }

    public void requestLocationUpdates(long minTime, float minDistance, Criteria criteria, LocationListener listener, Looper looper) {
        checkCriteria(criteria);
        checkListener(listener);
        LocationRequest request = LocationRequest.createFromDeprecatedCriteria(criteria, minTime, minDistance, false);
        requestLocationUpdates(request, listener, looper, (PendingIntent) null);
    }

    public void requestLocationUpdates(String provider, long minTime, float minDistance, PendingIntent intent) {
        checkProvider(provider);
        checkPendingIntent(intent);
        LocationRequest request = LocationRequest.createFromDeprecatedProvider(provider, minTime, minDistance, false);
        requestLocationUpdates(request, (LocationListener) null, (Looper) null, intent);
    }

    public void requestLocationUpdates(long minTime, float minDistance, Criteria criteria, PendingIntent intent) {
        checkCriteria(criteria);
        checkPendingIntent(intent);
        LocationRequest request = LocationRequest.createFromDeprecatedCriteria(criteria, minTime, minDistance, false);
        requestLocationUpdates(request, (LocationListener) null, (Looper) null, intent);
    }

    public void requestSingleUpdate(String provider, LocationListener listener, Looper looper) {
        checkProvider(provider);
        checkListener(listener);
        LocationRequest request = LocationRequest.createFromDeprecatedProvider(provider, 0L, 0.0f, true);
        requestLocationUpdates(request, listener, looper, (PendingIntent) null);
    }

    public void requestSingleUpdate(Criteria criteria, LocationListener listener, Looper looper) {
        checkCriteria(criteria);
        checkListener(listener);
        LocationRequest request = LocationRequest.createFromDeprecatedCriteria(criteria, 0L, 0.0f, true);
        requestLocationUpdates(request, listener, looper, (PendingIntent) null);
    }

    public void requestSingleUpdate(String provider, PendingIntent intent) {
        checkProvider(provider);
        checkPendingIntent(intent);
        LocationRequest request = LocationRequest.createFromDeprecatedProvider(provider, 0L, 0.0f, true);
        requestLocationUpdates(request, (LocationListener) null, (Looper) null, intent);
    }

    public void requestSingleUpdate(Criteria criteria, PendingIntent intent) {
        checkCriteria(criteria);
        checkPendingIntent(intent);
        LocationRequest request = LocationRequest.createFromDeprecatedCriteria(criteria, 0L, 0.0f, true);
        requestLocationUpdates(request, (LocationListener) null, (Looper) null, intent);
    }

    public void requestLocationUpdates(LocationRequest request, LocationListener listener, Looper looper) {
        checkListener(listener);
        requestLocationUpdates(request, listener, looper, (PendingIntent) null);
    }

    public void requestLocationUpdates(LocationRequest request, PendingIntent intent) {
        checkPendingIntent(intent);
        requestLocationUpdates(request, (LocationListener) null, (Looper) null, intent);
    }

    private ListenerTransport wrapListener(LocationListener listener, Looper looper) {
        ListenerTransport transport;
        if (listener == null) {
            return null;
        }
        synchronized (this.mListeners) {
            transport = this.mListeners.get(listener);
            if (transport == null) {
                transport = new ListenerTransport(listener, looper);
            }
            this.mListeners.put(listener, transport);
        }
        return transport;
    }

    private void requestLocationUpdates(LocationRequest request, LocationListener listener, Looper looper, PendingIntent intent) {
        String packageName = this.mContext.getPackageName();
        ListenerTransport transport = wrapListener(listener, looper);
        try {
            this.mService.requestLocationUpdates(request, transport, intent, packageName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void removeUpdates(LocationListener listener) {
        ListenerTransport transport;
        checkListener(listener);
        String packageName = this.mContext.getPackageName();
        synchronized (this.mListeners) {
            transport = this.mListeners.remove(listener);
        }
        if (transport == null) {
            return;
        }
        try {
            this.mService.removeUpdates(transport, null, packageName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void removeUpdates(PendingIntent intent) {
        checkPendingIntent(intent);
        String packageName = this.mContext.getPackageName();
        try {
            this.mService.removeUpdates(null, intent, packageName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void addProximityAlert(double latitude, double longitude, float radius, long expiration, PendingIntent intent) {
        checkPendingIntent(intent);
        if (expiration < 0) {
            expiration = Long.MAX_VALUE;
        }
        Geofence fence = Geofence.createCircle(latitude, longitude, radius);
        LocationRequest request = new LocationRequest().setExpireIn(expiration);
        try {
            this.mService.requestGeofence(request, fence, intent, this.mContext.getPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void addGeofence(LocationRequest request, Geofence fence, PendingIntent intent) {
        checkPendingIntent(intent);
        checkGeofence(fence);
        try {
            this.mService.requestGeofence(request, fence, intent, this.mContext.getPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void removeProximityAlert(PendingIntent intent) {
        checkPendingIntent(intent);
        String packageName = this.mContext.getPackageName();
        try {
            this.mService.removeGeofence(null, intent, packageName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void removeGeofence(Geofence fence, PendingIntent intent) {
        checkPendingIntent(intent);
        checkGeofence(fence);
        String packageName = this.mContext.getPackageName();
        try {
            this.mService.removeGeofence(fence, intent, packageName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void removeAllGeofences(PendingIntent intent) {
        checkPendingIntent(intent);
        String packageName = this.mContext.getPackageName();
        try {
            this.mService.removeGeofence(null, intent, packageName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isProviderEnabled(String provider) {
        checkProvider(provider);
        try {
            return this.mService.isProviderEnabled(provider);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Location getLastLocation() {
        String packageName = this.mContext.getPackageName();
        try {
            return this.mService.getLastLocation(null, packageName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Location getLastKnownLocation(String provider) {
        checkProvider(provider);
        String packageName = this.mContext.getPackageName();
        LocationRequest request = LocationRequest.createFromDeprecatedProvider(provider, 0L, 0.0f, true);
        try {
            return this.mService.getLastLocation(request, packageName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void addTestProvider(String name, boolean requiresNetwork, boolean requiresSatellite, boolean requiresCell, boolean hasMonetaryCost, boolean supportsAltitude, boolean supportsSpeed, boolean supportsBearing, int powerRequirement, int accuracy) {
        ProviderProperties properties = new ProviderProperties(requiresNetwork, requiresSatellite, requiresCell, hasMonetaryCost, supportsAltitude, supportsSpeed, supportsBearing, powerRequirement, accuracy);
        if (name.matches(LocationProvider.BAD_CHARS_REGEX)) {
            throw new IllegalArgumentException("provider name contains illegal character: " + name);
        }
        try {
            this.mService.addTestProvider(name, properties, this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void removeTestProvider(String provider) {
        try {
            this.mService.removeTestProvider(provider, this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setTestProviderLocation(String provider, Location loc) {
        if (!loc.isComplete()) {
            IllegalArgumentException e = new IllegalArgumentException("Incomplete location object, missing timestamp or accuracy? " + loc);
            if (this.mContext.getApplicationInfo().targetSdkVersion <= 16) {
                Log.w(TAG, e);
                loc.makeComplete();
            } else {
                throw e;
            }
        }
        try {
            this.mService.setTestProviderLocation(provider, loc, this.mContext.getOpPackageName());
        } catch (RemoteException e2) {
            throw e2.rethrowFromSystemServer();
        }
    }

    public void clearTestProviderLocation(String provider) {
        try {
            this.mService.clearTestProviderLocation(provider, this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setTestProviderEnabled(String provider, boolean enabled) {
        try {
            this.mService.setTestProviderEnabled(provider, enabled, this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void clearTestProviderEnabled(String provider) {
        try {
            this.mService.clearTestProviderEnabled(provider, this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setTestProviderStatus(String provider, int status, Bundle extras, long updateTime) {
        try {
            this.mService.setTestProviderStatus(provider, status, extras, updateTime, this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void clearTestProviderStatus(String provider) {
        try {
            this.mService.clearTestProviderStatus(provider, this.mContext.getOpPackageName());
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
        private final GnssStatusCallback mOldGnssCallback;
        private final GnssNmeaListener mOldGnssNmeaListener;

        private class GnssHandler extends Handler {
            public GnssHandler(Handler handler) {
                super(handler != null ? handler.getLooper() : Looper.myLooper());
            }

            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
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
                    case 1000:
                        synchronized (GnssStatusListenerTransport.this.mNmeaBuffer) {
                            int length = GnssStatusListenerTransport.this.mNmeaBuffer.size();
                            for (int i = 0; i < length; i++) {
                                Nmea nmea = (Nmea) GnssStatusListenerTransport.this.mNmeaBuffer.get(i);
                                GnssStatusListenerTransport.this.mGnssNmeaListener.onNmeaMessage(nmea.mNmea, nmea.mTimestamp);
                            }
                            GnssStatusListenerTransport.this.mNmeaBuffer.clear();
                        }
                        return;
                    default:
                        return;
                }
            }
        }

        private class Nmea {
            String mNmea;
            long mTimestamp;

            Nmea(long timestamp, String nmea) {
                this.mTimestamp = timestamp;
                this.mNmea = nmea;
            }
        }

        GnssStatusListenerTransport(LocationManager this$0, GpsStatus.Listener listener) {
            this(listener, (Handler) null);
        }

        GnssStatusListenerTransport(GpsStatus.Listener listener, Handler handler) {
            this.mGpsListener = listener;
            this.mGnssHandler = new GnssHandler(handler);
            this.mGpsNmeaListener = null;
            this.mNmeaBuffer = null;
            this.mOldGnssCallback = null;
            this.mGnssCallback = new GnssStatus.Callback() {
                @Override
                public void onStarted() {
                    GnssStatusListenerTransport.this.mGpsListener.onGpsStatusChanged(1);
                }

                @Override
                public void onStopped() {
                    GnssStatusListenerTransport.this.mGpsListener.onGpsStatusChanged(2);
                }

                @Override
                public void onFirstFix(int ttff) {
                    GnssStatusListenerTransport.this.mGpsListener.onGpsStatusChanged(3);
                }

                @Override
                public void onSatelliteStatusChanged(GnssStatus status) {
                    GnssStatusListenerTransport.this.mGpsListener.onGpsStatusChanged(4);
                }
            };
            this.mOldGnssNmeaListener = null;
            this.mGnssNmeaListener = null;
        }

        GnssStatusListenerTransport(LocationManager this$0, GpsStatus.NmeaListener listener) {
            this(listener, (Handler) null);
        }

        GnssStatusListenerTransport(GpsStatus.NmeaListener listener, Handler handler) {
            this.mGpsListener = null;
            this.mGnssHandler = new GnssHandler(handler);
            this.mGpsNmeaListener = listener;
            this.mNmeaBuffer = new ArrayList<>();
            this.mOldGnssCallback = null;
            this.mGnssCallback = null;
            this.mOldGnssNmeaListener = null;
            this.mGnssNmeaListener = new OnNmeaMessageListener() {
                @Override
                public void onNmeaMessage(String nmea, long timestamp) {
                    GnssStatusListenerTransport.this.mGpsNmeaListener.onNmeaReceived(timestamp, nmea);
                }
            };
        }

        GnssStatusListenerTransport(LocationManager this$0, GnssStatusCallback callback) {
            this(callback, (Handler) null);
        }

        GnssStatusListenerTransport(GnssStatusCallback callback, Handler handler) {
            this.mOldGnssCallback = callback;
            this.mGnssCallback = new GnssStatus.Callback() {
                @Override
                public void onStarted() {
                    GnssStatusListenerTransport.this.mOldGnssCallback.onStarted();
                }

                @Override
                public void onStopped() {
                    GnssStatusListenerTransport.this.mOldGnssCallback.onStopped();
                }

                @Override
                public void onFirstFix(int ttff) {
                    GnssStatusListenerTransport.this.mOldGnssCallback.onFirstFix(ttff);
                }

                @Override
                public void onSatelliteStatusChanged(GnssStatus status) {
                    GnssStatusListenerTransport.this.mOldGnssCallback.onSatelliteStatusChanged(status);
                }
            };
            this.mGnssHandler = new GnssHandler(handler);
            this.mOldGnssNmeaListener = null;
            this.mGnssNmeaListener = null;
            this.mNmeaBuffer = null;
            this.mGpsListener = null;
            this.mGpsNmeaListener = null;
        }

        GnssStatusListenerTransport(LocationManager this$0, GnssStatus.Callback callback) {
            this(callback, (Handler) null);
        }

        GnssStatusListenerTransport(GnssStatus.Callback callback, Handler handler) {
            this.mOldGnssCallback = null;
            this.mGnssCallback = callback;
            this.mGnssHandler = new GnssHandler(handler);
            this.mOldGnssNmeaListener = null;
            this.mGnssNmeaListener = null;
            this.mNmeaBuffer = null;
            this.mGpsListener = null;
            this.mGpsNmeaListener = null;
        }

        GnssStatusListenerTransport(LocationManager this$0, GnssNmeaListener listener) {
            this(listener, (Handler) null);
        }

        GnssStatusListenerTransport(GnssNmeaListener listener, Handler handler) {
            this.mGnssCallback = null;
            this.mOldGnssCallback = null;
            this.mGnssHandler = new GnssHandler(handler);
            this.mOldGnssNmeaListener = listener;
            this.mGnssNmeaListener = new OnNmeaMessageListener() {
                @Override
                public void onNmeaMessage(String message, long timestamp) {
                    GnssStatusListenerTransport.this.mOldGnssNmeaListener.onNmeaReceived(timestamp, message);
                }
            };
            this.mGpsListener = null;
            this.mGpsNmeaListener = null;
            this.mNmeaBuffer = new ArrayList<>();
        }

        GnssStatusListenerTransport(LocationManager this$0, OnNmeaMessageListener listener) {
            this(listener, (Handler) null);
        }

        GnssStatusListenerTransport(OnNmeaMessageListener listener, Handler handler) {
            this.mOldGnssCallback = null;
            this.mGnssCallback = null;
            this.mGnssHandler = new GnssHandler(handler);
            this.mOldGnssNmeaListener = null;
            this.mGnssNmeaListener = listener;
            this.mGpsListener = null;
            this.mGpsNmeaListener = null;
            this.mNmeaBuffer = new ArrayList<>();
        }

        @Override
        public void onGnssStarted() {
            if (this.mGnssCallback == null) {
                return;
            }
            Message msg = Message.obtain();
            msg.what = 1;
            this.mGnssHandler.sendMessage(msg);
        }

        @Override
        public void onGnssStopped() {
            if (this.mGnssCallback == null) {
                return;
            }
            Message msg = Message.obtain();
            msg.what = 2;
            this.mGnssHandler.sendMessage(msg);
        }

        @Override
        public void onFirstFix(int ttff) {
            if (this.mGnssCallback == null) {
                return;
            }
            LocationManager.this.mTimeToFirstFix = ttff;
            Message msg = Message.obtain();
            msg.what = 3;
            this.mGnssHandler.sendMessage(msg);
        }

        @Override
        public void onSvStatusChanged(int svCount, int[] prnWithFlags, float[] cn0s, float[] elevations, float[] azimuths) {
            if (this.mGnssCallback == null) {
                return;
            }
            LocationManager.this.mGnssStatus = new GnssStatus(svCount, prnWithFlags, cn0s, elevations, azimuths);
            Message msg = Message.obtain();
            msg.what = 4;
            this.mGnssHandler.removeMessages(4);
            this.mGnssHandler.sendMessage(msg);
        }

        @Override
        public void onNmeaReceived(long timestamp, String nmea) {
            if (this.mGnssNmeaListener == null) {
                return;
            }
            synchronized (this.mNmeaBuffer) {
                this.mNmeaBuffer.add(new Nmea(timestamp, nmea));
            }
            Message msg = Message.obtain();
            msg.what = 1000;
            this.mGnssHandler.removeMessages(1000);
            this.mGnssHandler.sendMessage(msg);
        }
    }

    @Deprecated
    public boolean addGpsStatusListener(GpsStatus.Listener listener) {
        if (this.mGpsStatusListeners.get(listener) != null) {
            return true;
        }
        try {
            GnssStatusListenerTransport transport = new GnssStatusListenerTransport(this, listener);
            boolean result = this.mService.registerGnssStatusCallback(transport, this.mContext.getPackageName());
            if (result) {
                this.mGpsStatusListeners.put(listener, transport);
            }
            return result;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public void removeGpsStatusListener(GpsStatus.Listener listener) {
        try {
            GnssStatusListenerTransport transport = this.mGpsStatusListeners.remove(listener);
            if (transport == null) {
                return;
            }
            this.mService.unregisterGnssStatusCallback(transport);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean registerGnssStatusCallback(GnssStatusCallback callback) {
        return registerGnssStatusCallback(callback, (Handler) null);
    }

    public boolean registerGnssStatusCallback(GnssStatusCallback callback, Handler handler) {
        if (this.mOldGnssStatusListeners.get(callback) != null) {
            return true;
        }
        try {
            GnssStatusListenerTransport transport = new GnssStatusListenerTransport(callback, handler);
            boolean result = this.mService.registerGnssStatusCallback(transport, this.mContext.getPackageName());
            if (result) {
                this.mOldGnssStatusListeners.put(callback, transport);
            }
            return result;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void unregisterGnssStatusCallback(GnssStatusCallback callback) {
        try {
            GnssStatusListenerTransport transport = this.mOldGnssStatusListeners.remove(callback);
            if (transport == null) {
                return;
            }
            this.mService.unregisterGnssStatusCallback(transport);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean registerGnssStatusCallback(GnssStatus.Callback callback) {
        return registerGnssStatusCallback(callback, (Handler) null);
    }

    public boolean registerGnssStatusCallback(GnssStatus.Callback callback, Handler handler) {
        if (this.mGnssStatusListeners.get(callback) != null) {
            return true;
        }
        try {
            GnssStatusListenerTransport transport = new GnssStatusListenerTransport(callback, handler);
            boolean result = this.mService.registerGnssStatusCallback(transport, this.mContext.getPackageName());
            if (result) {
                this.mGnssStatusListeners.put(callback, transport);
            }
            return result;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void unregisterGnssStatusCallback(GnssStatus.Callback callback) {
        try {
            GnssStatusListenerTransport transport = this.mGnssStatusListeners.remove(callback);
            if (transport == null) {
                return;
            }
            this.mService.unregisterGnssStatusCallback(transport);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public boolean addNmeaListener(GpsStatus.NmeaListener listener) {
        if (this.mGpsNmeaListeners.get(listener) != null) {
            return true;
        }
        try {
            GnssStatusListenerTransport transport = new GnssStatusListenerTransport(this, listener);
            boolean result = this.mService.registerGnssStatusCallback(transport, this.mContext.getPackageName());
            if (result) {
                this.mGpsNmeaListeners.put(listener, transport);
            }
            return result;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public void removeNmeaListener(GpsStatus.NmeaListener listener) {
        try {
            GnssStatusListenerTransport transport = this.mGpsNmeaListeners.remove(listener);
            if (transport == null) {
                return;
            }
            this.mService.unregisterGnssStatusCallback(transport);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean addNmeaListener(GnssNmeaListener listener) {
        return addNmeaListener(listener, (Handler) null);
    }

    public boolean addNmeaListener(GnssNmeaListener listener, Handler handler) {
        if (this.mGpsNmeaListeners.get(listener) != null) {
            return true;
        }
        try {
            GnssStatusListenerTransport transport = new GnssStatusListenerTransport(listener, handler);
            boolean result = this.mService.registerGnssStatusCallback(transport, this.mContext.getPackageName());
            if (result) {
                this.mOldGnssNmeaListeners.put(listener, transport);
            }
            return result;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void removeNmeaListener(GnssNmeaListener listener) {
        try {
            GnssStatusListenerTransport transport = this.mOldGnssNmeaListeners.remove(listener);
            if (transport == null) {
                return;
            }
            this.mService.unregisterGnssStatusCallback(transport);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean addNmeaListener(OnNmeaMessageListener listener) {
        return addNmeaListener(listener, (Handler) null);
    }

    public boolean addNmeaListener(OnNmeaMessageListener listener, Handler handler) {
        if (this.mGpsNmeaListeners.get(listener) != null) {
            return true;
        }
        try {
            GnssStatusListenerTransport transport = new GnssStatusListenerTransport(listener, handler);
            boolean result = this.mService.registerGnssStatusCallback(transport, this.mContext.getPackageName());
            if (result) {
                this.mGnssNmeaListeners.put(listener, transport);
            }
            return result;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void removeNmeaListener(OnNmeaMessageListener listener) {
        try {
            GnssStatusListenerTransport transport = this.mGnssNmeaListeners.remove(listener);
            if (transport == null) {
                return;
            }
            this.mService.unregisterGnssStatusCallback(transport);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

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

    @Deprecated
    public void removeGpsMeasurementListener(GpsMeasurementsEvent.Listener listener) {
    }

    public void unregisterGnssMeasurementsCallback(GnssMeasurementsEvent.Callback callback) {
        this.mGnssMeasurementCallbackTransport.remove(callback);
    }

    @Deprecated
    public boolean addGpsNavigationMessageListener(GpsNavigationMessageEvent.Listener listener) {
        return false;
    }

    @Deprecated
    public void removeGpsNavigationMessageListener(GpsNavigationMessageEvent.Listener listener) {
    }

    public boolean registerGnssNavigationMessageCallback(GnssNavigationMessageEvent.Callback callback) {
        return registerGnssNavigationMessageCallback(callback, (Handler) null);
    }

    public boolean registerGnssNavigationMessageCallback(final GnssNavigationMessageEvent.Callback callback, Handler handler) {
        GnssNavigationMessage.Callback bridge = new GnssNavigationMessage.Callback() {
            @Override
            public void onGnssNavigationMessageReceived(GnssNavigationMessage message) {
                GnssNavigationMessageEvent event = new GnssNavigationMessageEvent(message);
                callback.onGnssNavigationMessageReceived(event);
            }

            @Override
            public void onStatusChanged(int status) {
                callback.onStatusChanged(status);
            }
        };
        this.mNavigationMessageBridge.put(callback, bridge);
        return this.mGnssNavigationMessageCallbackTransport.add(bridge, handler);
    }

    public void unregisterGnssNavigationMessageCallback(GnssNavigationMessageEvent.Callback callback) {
        this.mGnssNavigationMessageCallbackTransport.remove(this.mNavigationMessageBridge.remove(callback));
    }

    public boolean registerGnssNavigationMessageCallback(GnssNavigationMessage.Callback callback) {
        return registerGnssNavigationMessageCallback(callback, (Handler) null);
    }

    public boolean registerGnssNavigationMessageCallback(GnssNavigationMessage.Callback callback, Handler handler) {
        return this.mGnssNavigationMessageCallbackTransport.add(callback, handler);
    }

    public void unregisterGnssNavigationMessageCallback(GnssNavigationMessage.Callback callback) {
        this.mGnssNavigationMessageCallbackTransport.remove(callback);
    }

    @Deprecated
    public GpsStatus getGpsStatus(GpsStatus status) {
        if (status == null) {
            status = new GpsStatus();
        }
        if (this.mGnssStatus != null) {
            status.setStatus(this.mGnssStatus, this.mTimeToFirstFix);
        }
        return status;
    }

    public int getGnssYearOfHardware() {
        try {
            return this.mService.getGnssYearOfHardware();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean sendExtraCommand(String provider, String command, Bundle extras) {
        try {
            return this.mService.sendExtraCommand(provider, command, extras);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean sendNiResponse(int notifId, int userResponse) {
        try {
            return this.mService.sendNiResponse(notifId, userResponse);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private static void checkProvider(String provider) {
        if (provider != null) {
        } else {
            throw new IllegalArgumentException("invalid provider: " + provider);
        }
    }

    private static void checkCriteria(Criteria criteria) {
        if (criteria != null) {
        } else {
            throw new IllegalArgumentException("invalid criteria: " + criteria);
        }
    }

    private static void checkListener(LocationListener listener) {
        if (listener != null) {
        } else {
            throw new IllegalArgumentException("invalid listener: " + listener);
        }
    }

    private void checkPendingIntent(PendingIntent intent) {
        if (intent == null) {
            throw new IllegalArgumentException("invalid pending intent: " + intent);
        }
        if (intent.isTargetedToPackage()) {
            return;
        }
        IllegalArgumentException e = new IllegalArgumentException("pending intent must be targeted to package");
        if (this.mContext.getApplicationInfo().targetSdkVersion > 16) {
            throw e;
        }
        Log.w(TAG, e);
    }

    private static void checkGeofence(Geofence fence) {
        if (fence != null) {
        } else {
            throw new IllegalArgumentException("invalid geofence: " + fence);
        }
    }
}
