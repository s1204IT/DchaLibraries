package android.location;

import android.app.PendingIntent;
import android.content.Context;
import android.location.GpsMeasurementsEvent;
import android.location.GpsNavigationMessageEvent;
import android.location.GpsStatus;
import android.location.IGpsStatusListener;
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
    private final GpsMeasurementListenerTransport mGpsMeasurementListenerTransport;
    private final GpsNavigationMessageListenerTransport mGpsNavigationMessageListenerTransport;
    private final ILocationManager mService;
    private final HashMap<GpsStatus.Listener, GpsStatusListenerTransport> mGpsStatusListeners = new HashMap<>();
    private final HashMap<GpsStatus.NmeaListener, GpsStatusListenerTransport> mNmeaListeners = new HashMap<>();
    private final GpsStatus mGpsStatus = new GpsStatus();
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
                Log.e(LocationManager.TAG, "locationCallbackFinished: RemoteException", e);
            }
        }
    }

    public LocationManager(Context context, ILocationManager service) {
        this.mService = service;
        this.mContext = context;
        this.mGpsMeasurementListenerTransport = new GpsMeasurementListenerTransport(this.mContext, this.mService);
        this.mGpsNavigationMessageListenerTransport = new GpsNavigationMessageListenerTransport(this.mContext, this.mService);
    }

    private LocationProvider createProvider(String name, ProviderProperties properties) {
        return new LocationProvider(name, properties);
    }

    public List<String> getAllProviders() {
        try {
            return this.mService.getAllProviders();
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException", e);
            return null;
        }
    }

    public List<String> getProviders(boolean enabledOnly) {
        try {
            return this.mService.getProviders(null, enabledOnly);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException", e);
            return null;
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
            Log.e(TAG, "RemoteException", e);
            return null;
        }
    }

    public List<String> getProviders(Criteria criteria, boolean enabledOnly) {
        checkCriteria(criteria);
        try {
            return this.mService.getProviders(criteria, enabledOnly);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException", e);
            return null;
        }
    }

    public String getBestProvider(Criteria criteria, boolean enabledOnly) {
        checkCriteria(criteria);
        try {
            return this.mService.getBestProvider(criteria, enabledOnly);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException", e);
            return null;
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
            Log.e(TAG, "RemoteException", e);
        }
    }

    public void removeUpdates(LocationListener listener) {
        ListenerTransport transport;
        checkListener(listener);
        String packageName = this.mContext.getPackageName();
        synchronized (this.mListeners) {
            transport = this.mListeners.remove(listener);
        }
        if (transport != null) {
            try {
                this.mService.removeUpdates(transport, null, packageName);
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException", e);
            }
        }
    }

    public void removeUpdates(PendingIntent intent) {
        checkPendingIntent(intent);
        String packageName = this.mContext.getPackageName();
        try {
            this.mService.removeUpdates(null, intent, packageName);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException", e);
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
            Log.e(TAG, "RemoteException", e);
        }
    }

    public void addGeofence(LocationRequest request, Geofence fence, PendingIntent intent) {
        checkPendingIntent(intent);
        checkGeofence(fence);
        try {
            this.mService.requestGeofence(request, fence, intent, this.mContext.getPackageName());
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException", e);
        }
    }

    public void removeProximityAlert(PendingIntent intent) {
        checkPendingIntent(intent);
        String packageName = this.mContext.getPackageName();
        try {
            this.mService.removeGeofence(null, intent, packageName);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException", e);
        }
    }

    public void removeGeofence(Geofence fence, PendingIntent intent) {
        checkPendingIntent(intent);
        checkGeofence(fence);
        String packageName = this.mContext.getPackageName();
        try {
            this.mService.removeGeofence(fence, intent, packageName);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException", e);
        }
    }

    public void removeAllGeofences(PendingIntent intent) {
        checkPendingIntent(intent);
        String packageName = this.mContext.getPackageName();
        try {
            this.mService.removeGeofence(null, intent, packageName);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException", e);
        }
    }

    public boolean isProviderEnabled(String provider) {
        checkProvider(provider);
        try {
            return this.mService.isProviderEnabled(provider);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException", e);
            return false;
        }
    }

    public Location getLastLocation() {
        String packageName = this.mContext.getPackageName();
        try {
            return this.mService.getLastLocation(null, packageName);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException", e);
            return null;
        }
    }

    public Location getLastKnownLocation(String provider) {
        checkProvider(provider);
        String packageName = this.mContext.getPackageName();
        LocationRequest request = LocationRequest.createFromDeprecatedProvider(provider, 0L, 0.0f, true);
        try {
            return this.mService.getLastLocation(request, packageName);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException", e);
            return null;
        }
    }

    public void addTestProvider(String name, boolean requiresNetwork, boolean requiresSatellite, boolean requiresCell, boolean hasMonetaryCost, boolean supportsAltitude, boolean supportsSpeed, boolean supportsBearing, int powerRequirement, int accuracy) {
        ProviderProperties properties = new ProviderProperties(requiresNetwork, requiresSatellite, requiresCell, hasMonetaryCost, supportsAltitude, supportsSpeed, supportsBearing, powerRequirement, accuracy);
        if (name.matches(LocationProvider.BAD_CHARS_REGEX)) {
            throw new IllegalArgumentException("provider name contains illegal character: " + name);
        }
        try {
            this.mService.addTestProvider(name, properties);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException", e);
        }
    }

    public void removeTestProvider(String provider) {
        try {
            this.mService.removeTestProvider(provider);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException", e);
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
            this.mService.setTestProviderLocation(provider, loc);
        } catch (RemoteException e2) {
            Log.e(TAG, "RemoteException", e2);
        }
    }

    public void clearTestProviderLocation(String provider) {
        try {
            this.mService.clearTestProviderLocation(provider);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException", e);
        }
    }

    public void setTestProviderEnabled(String provider, boolean enabled) {
        try {
            this.mService.setTestProviderEnabled(provider, enabled);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException", e);
        }
    }

    public void clearTestProviderEnabled(String provider) {
        try {
            this.mService.clearTestProviderEnabled(provider);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException", e);
        }
    }

    public void setTestProviderStatus(String provider, int status, Bundle extras, long updateTime) {
        try {
            this.mService.setTestProviderStatus(provider, status, extras, updateTime);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException", e);
        }
    }

    public void clearTestProviderStatus(String provider) {
        try {
            this.mService.clearTestProviderStatus(provider);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException", e);
        }
    }

    private class GpsStatusListenerTransport extends IGpsStatusListener.Stub {
        private static final int NMEA_RECEIVED = 1000;
        private final Handler mGpsHandler;
        private final GpsStatus.Listener mListener;
        private ArrayList<Nmea> mNmeaBuffer;
        private final GpsStatus.NmeaListener mNmeaListener;

        private class Nmea {
            String mNmea;
            long mTimestamp;

            Nmea(long timestamp, String nmea) {
                this.mTimestamp = timestamp;
                this.mNmea = nmea;
            }
        }

        GpsStatusListenerTransport(GpsStatus.Listener listener) {
            this.mGpsHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    if (msg.what != 1000) {
                        synchronized (LocationManager.this.mGpsStatus) {
                            GpsStatusListenerTransport.this.mListener.onGpsStatusChanged(msg.what);
                        }
                        return;
                    }
                    synchronized (GpsStatusListenerTransport.this.mNmeaBuffer) {
                        int length = GpsStatusListenerTransport.this.mNmeaBuffer.size();
                        for (int i = 0; i < length; i++) {
                            Nmea nmea = (Nmea) GpsStatusListenerTransport.this.mNmeaBuffer.get(i);
                            GpsStatusListenerTransport.this.mNmeaListener.onNmeaReceived(nmea.mTimestamp, nmea.mNmea);
                        }
                        GpsStatusListenerTransport.this.mNmeaBuffer.clear();
                    }
                }
            };
            this.mListener = listener;
            this.mNmeaListener = null;
        }

        GpsStatusListenerTransport(GpsStatus.NmeaListener listener) {
            this.mGpsHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    if (msg.what != 1000) {
                        synchronized (LocationManager.this.mGpsStatus) {
                            GpsStatusListenerTransport.this.mListener.onGpsStatusChanged(msg.what);
                        }
                        return;
                    }
                    synchronized (GpsStatusListenerTransport.this.mNmeaBuffer) {
                        int length = GpsStatusListenerTransport.this.mNmeaBuffer.size();
                        for (int i = 0; i < length; i++) {
                            Nmea nmea = (Nmea) GpsStatusListenerTransport.this.mNmeaBuffer.get(i);
                            GpsStatusListenerTransport.this.mNmeaListener.onNmeaReceived(nmea.mTimestamp, nmea.mNmea);
                        }
                        GpsStatusListenerTransport.this.mNmeaBuffer.clear();
                    }
                }
            };
            this.mNmeaListener = listener;
            this.mListener = null;
            this.mNmeaBuffer = new ArrayList<>();
        }

        @Override
        public void onGpsStarted() {
            if (this.mListener != null) {
                Message msg = Message.obtain();
                msg.what = 1;
                this.mGpsHandler.sendMessage(msg);
            }
        }

        @Override
        public void onGpsStopped() {
            if (this.mListener != null) {
                Message msg = Message.obtain();
                msg.what = 2;
                this.mGpsHandler.sendMessage(msg);
            }
        }

        @Override
        public void onFirstFix(int ttff) {
            if (this.mListener != null) {
                LocationManager.this.mGpsStatus.setTimeToFirstFix(ttff);
                Message msg = Message.obtain();
                msg.what = 3;
                this.mGpsHandler.sendMessage(msg);
            }
        }

        @Override
        public void onSvStatusChanged(int svCount, int[] prns, float[] snrs, float[] elevations, float[] azimuths, int ephemerisMask, int almanacMask, int usedInFixMask) {
            if (this.mListener != null) {
                LocationManager.this.mGpsStatus.setStatus(svCount, prns, snrs, elevations, azimuths, ephemerisMask, almanacMask, usedInFixMask);
                Message msg = Message.obtain();
                msg.what = 4;
                this.mGpsHandler.removeMessages(4);
                this.mGpsHandler.sendMessage(msg);
            }
        }

        @Override
        public void onNmeaReceived(long timestamp, String nmea) {
            if (this.mNmeaListener != null) {
                synchronized (this.mNmeaBuffer) {
                    this.mNmeaBuffer.add(new Nmea(timestamp, nmea));
                }
                Message msg = Message.obtain();
                msg.what = 1000;
                this.mGpsHandler.removeMessages(1000);
                this.mGpsHandler.sendMessage(msg);
            }
        }
    }

    public boolean addGpsStatusListener(GpsStatus.Listener listener) {
        if (this.mGpsStatusListeners.get(listener) != null) {
            return true;
        }
        try {
            GpsStatusListenerTransport transport = new GpsStatusListenerTransport(listener);
            boolean result = this.mService.addGpsStatusListener(transport, this.mContext.getPackageName());
            if (result) {
                this.mGpsStatusListeners.put(listener, transport);
                return result;
            }
            return result;
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in registerGpsStatusListener: ", e);
            return false;
        }
    }

    public void removeGpsStatusListener(GpsStatus.Listener listener) {
        try {
            GpsStatusListenerTransport transport = this.mGpsStatusListeners.remove(listener);
            if (transport != null) {
                this.mService.removeGpsStatusListener(transport);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in unregisterGpsStatusListener: ", e);
        }
    }

    public boolean addNmeaListener(GpsStatus.NmeaListener listener) {
        if (this.mNmeaListeners.get(listener) != null) {
            return true;
        }
        try {
            GpsStatusListenerTransport transport = new GpsStatusListenerTransport(listener);
            boolean result = this.mService.addGpsStatusListener(transport, this.mContext.getPackageName());
            if (result) {
                this.mNmeaListeners.put(listener, transport);
                return result;
            }
            return result;
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in registerGpsStatusListener: ", e);
            return false;
        }
    }

    public void removeNmeaListener(GpsStatus.NmeaListener listener) {
        try {
            GpsStatusListenerTransport transport = this.mNmeaListeners.remove(listener);
            if (transport != null) {
                this.mService.removeGpsStatusListener(transport);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in unregisterGpsStatusListener: ", e);
        }
    }

    public boolean addGpsMeasurementListener(GpsMeasurementsEvent.Listener listener) {
        return this.mGpsMeasurementListenerTransport.add(listener);
    }

    public void removeGpsMeasurementListener(GpsMeasurementsEvent.Listener listener) {
        this.mGpsMeasurementListenerTransport.remove(listener);
    }

    public boolean addGpsNavigationMessageListener(GpsNavigationMessageEvent.Listener listener) {
        return this.mGpsNavigationMessageListenerTransport.add(listener);
    }

    public void removeGpsNavigationMessageListener(GpsNavigationMessageEvent.Listener listener) {
        this.mGpsNavigationMessageListenerTransport.remove(listener);
    }

    public GpsStatus getGpsStatus(GpsStatus status) {
        if (status == null) {
            status = new GpsStatus();
        }
        status.setStatus(this.mGpsStatus);
        return status;
    }

    public boolean sendExtraCommand(String provider, String command, Bundle extras) {
        try {
            return this.mService.sendExtraCommand(provider, command, extras);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in sendExtraCommand: ", e);
            return false;
        }
    }

    public boolean sendNiResponse(int notifId, int userResponse) {
        try {
            return this.mService.sendNiResponse(notifId, userResponse);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in sendNiResponse: ", e);
            return false;
        }
    }

    private static void checkProvider(String provider) {
        if (provider == null) {
            throw new IllegalArgumentException("invalid provider: " + provider);
        }
    }

    private static void checkCriteria(Criteria criteria) {
        if (criteria == null) {
            throw new IllegalArgumentException("invalid criteria: " + criteria);
        }
    }

    private static void checkListener(LocationListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("invalid listener: " + listener);
        }
    }

    private void checkPendingIntent(PendingIntent intent) {
        if (intent == null) {
            throw new IllegalArgumentException("invalid pending intent: " + intent);
        }
        if (!intent.isTargetedToPackage()) {
            IllegalArgumentException e = new IllegalArgumentException("pending intent must be targeted to package");
            if (this.mContext.getApplicationInfo().targetSdkVersion > 16) {
                throw e;
            }
            Log.w(TAG, e);
        }
    }

    private static void checkGeofence(Geofence fence) {
        if (fence == null) {
            throw new IllegalArgumentException("invalid geofence: " + fence);
        }
    }
}
