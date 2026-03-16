package com.android.server.twilight;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.format.Time;
import com.android.server.SystemService;
import com.android.server.TwilightCalculator;
import java.util.ArrayList;
import java.util.Iterator;
import libcore.util.Objects;

public final class TwilightService extends SystemService {
    static final String ACTION_UPDATE_TWILIGHT_STATE = "com.android.server.action.UPDATE_TWILIGHT_STATE";
    static final boolean DEBUG = false;
    static final String TAG = "TwilightService";
    AlarmManager mAlarmManager;
    private final LocationListener mEmptyLocationListener;
    final ArrayList<TwilightListenerRecord> mListeners;
    LocationHandler mLocationHandler;
    private final LocationListener mLocationListener;
    LocationManager mLocationManager;
    final Object mLock;
    private final TwilightManager mService;
    TwilightState mTwilightState;
    private final BroadcastReceiver mUpdateLocationReceiver;

    public TwilightService(Context context) {
        super(context);
        this.mLock = new Object();
        this.mListeners = new ArrayList<>();
        this.mService = new TwilightManager() {
            @Override
            public TwilightState getCurrentState() {
                TwilightState twilightState;
                synchronized (TwilightService.this.mLock) {
                    twilightState = TwilightService.this.mTwilightState;
                }
                return twilightState;
            }

            @Override
            public void registerListener(TwilightListener listener, Handler handler) {
                synchronized (TwilightService.this.mLock) {
                    TwilightService.this.mListeners.add(new TwilightListenerRecord(listener, handler));
                    if (TwilightService.this.mListeners.size() == 1) {
                        TwilightService.this.mLocationHandler.enableLocationUpdates();
                    }
                }
            }
        };
        this.mUpdateLocationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if ("android.intent.action.AIRPLANE_MODE".equals(intent.getAction()) && !intent.getBooleanExtra("state", TwilightService.DEBUG)) {
                    TwilightService.this.mLocationHandler.requestLocationUpdate();
                } else {
                    TwilightService.this.mLocationHandler.requestTwilightUpdate();
                }
            }
        };
        this.mEmptyLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
            }

            @Override
            public void onProviderDisabled(String provider) {
            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }
        };
        this.mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                TwilightService.this.mLocationHandler.processNewLocation(location);
            }

            @Override
            public void onProviderDisabled(String provider) {
            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }
        };
    }

    @Override
    public void onStart() {
        this.mAlarmManager = (AlarmManager) getContext().getSystemService("alarm");
        this.mLocationManager = (LocationManager) getContext().getSystemService("location");
        this.mLocationHandler = new LocationHandler();
        IntentFilter filter = new IntentFilter("android.intent.action.AIRPLANE_MODE");
        filter.addAction("android.intent.action.TIME_SET");
        filter.addAction("android.intent.action.TIMEZONE_CHANGED");
        filter.addAction(ACTION_UPDATE_TWILIGHT_STATE);
        getContext().registerReceiver(this.mUpdateLocationReceiver, filter);
        publishLocalService(TwilightManager.class, this.mService);
    }

    private static class TwilightListenerRecord implements Runnable {
        private final Handler mHandler;
        private final TwilightListener mListener;

        public TwilightListenerRecord(TwilightListener listener, Handler handler) {
            this.mListener = listener;
            this.mHandler = handler;
        }

        public void postUpdate() {
            this.mHandler.post(this);
        }

        @Override
        public void run() {
            this.mListener.onTwilightStateChanged();
        }
    }

    private void setTwilightState(TwilightState state) {
        synchronized (this.mLock) {
            if (!Objects.equal(this.mTwilightState, state)) {
                this.mTwilightState = state;
                int listenerLen = this.mListeners.size();
                for (int i = 0; i < listenerLen; i++) {
                    this.mListeners.get(i).postUpdate();
                }
            }
        }
    }

    private static boolean hasMoved(Location from, Location to) {
        if (to == null) {
            return DEBUG;
        }
        if (from == null) {
            return true;
        }
        if (to.getElapsedRealtimeNanos() < from.getElapsedRealtimeNanos()) {
            return DEBUG;
        }
        float distance = from.distanceTo(to);
        float totalAccuracy = from.getAccuracy() + to.getAccuracy();
        return distance >= totalAccuracy;
    }

    private final class LocationHandler extends Handler {
        private static final double FACTOR_GMT_OFFSET_LONGITUDE = 0.004166666666666667d;
        private static final float LOCATION_UPDATE_DISTANCE_METER = 20000.0f;
        private static final long LOCATION_UPDATE_ENABLE_INTERVAL_MAX = 900000;
        private static final long LOCATION_UPDATE_ENABLE_INTERVAL_MIN = 5000;
        private static final long LOCATION_UPDATE_MS = 86400000;
        private static final long MIN_LOCATION_UPDATE_MS = 1800000;
        private static final int MSG_DO_TWILIGHT_UPDATE = 4;
        private static final int MSG_ENABLE_LOCATION_UPDATES = 1;
        private static final int MSG_GET_NEW_LOCATION_UPDATE = 2;
        private static final int MSG_PROCESS_NEW_LOCATION = 3;
        private boolean mDidFirstInit;
        private long mLastNetworkRegisterTime;
        private long mLastUpdateInterval;
        private Location mLocation;
        private boolean mNetworkListenerEnabled;
        private boolean mPassiveListenerEnabled;
        private final TwilightCalculator mTwilightCalculator;

        private LocationHandler() {
            this.mLastNetworkRegisterTime = -1800000L;
            this.mTwilightCalculator = new TwilightCalculator();
        }

        public void processNewLocation(Location location) {
            Message msg = obtainMessage(3, location);
            sendMessage(msg);
        }

        public void enableLocationUpdates() {
            sendEmptyMessage(1);
        }

        public void requestLocationUpdate() {
            sendEmptyMessage(2);
        }

        public void requestTwilightUpdate() {
            sendEmptyMessage(4);
        }

        @Override
        public void handleMessage(Message msg) {
            boolean networkLocationEnabled;
            boolean passiveLocationEnabled;
            switch (msg.what) {
                case 1:
                    break;
                case 2:
                    if (this.mNetworkListenerEnabled && this.mLastNetworkRegisterTime + MIN_LOCATION_UPDATE_MS < SystemClock.elapsedRealtime()) {
                        this.mNetworkListenerEnabled = TwilightService.DEBUG;
                        TwilightService.this.mLocationManager.removeUpdates(TwilightService.this.mEmptyLocationListener);
                    } else {
                        return;
                    }
                    break;
                case 3:
                    Location location = (Location) msg.obj;
                    boolean hasMoved = TwilightService.hasMoved(this.mLocation, location);
                    boolean hasBetterAccuracy = (this.mLocation == null || location.getAccuracy() < this.mLocation.getAccuracy()) ? true : TwilightService.DEBUG;
                    if (hasMoved || hasBetterAccuracy) {
                        setLocation(location);
                        return;
                    }
                    return;
                case 4:
                    updateTwilightState();
                    return;
                default:
                    return;
            }
            try {
                networkLocationEnabled = TwilightService.this.mLocationManager.isProviderEnabled("network");
            } catch (Exception e) {
                networkLocationEnabled = TwilightService.DEBUG;
            }
            if (!this.mNetworkListenerEnabled && networkLocationEnabled) {
                this.mNetworkListenerEnabled = true;
                this.mLastNetworkRegisterTime = SystemClock.elapsedRealtime();
                TwilightService.this.mLocationManager.requestLocationUpdates("network", LOCATION_UPDATE_MS, 0.0f, TwilightService.this.mEmptyLocationListener);
                if (!this.mDidFirstInit) {
                    this.mDidFirstInit = true;
                    if (this.mLocation == null) {
                        retrieveLocation();
                    }
                }
            }
            try {
                passiveLocationEnabled = TwilightService.this.mLocationManager.isProviderEnabled("passive");
            } catch (Exception e2) {
                passiveLocationEnabled = TwilightService.DEBUG;
            }
            if (!this.mPassiveListenerEnabled && passiveLocationEnabled) {
                this.mPassiveListenerEnabled = true;
                TwilightService.this.mLocationManager.requestLocationUpdates("passive", 0L, LOCATION_UPDATE_DISTANCE_METER, TwilightService.this.mLocationListener);
            }
            if (!this.mNetworkListenerEnabled || !this.mPassiveListenerEnabled) {
                this.mLastUpdateInterval = (long) (this.mLastUpdateInterval * 1.5d);
                if (this.mLastUpdateInterval == 0) {
                    this.mLastUpdateInterval = LOCATION_UPDATE_ENABLE_INTERVAL_MIN;
                } else if (this.mLastUpdateInterval > LOCATION_UPDATE_ENABLE_INTERVAL_MAX) {
                    this.mLastUpdateInterval = LOCATION_UPDATE_ENABLE_INTERVAL_MAX;
                }
                sendEmptyMessageDelayed(1, this.mLastUpdateInterval);
            }
        }

        private void retrieveLocation() {
            Location location = null;
            Iterator<String> providers = TwilightService.this.mLocationManager.getProviders(new Criteria(), true).iterator();
            while (providers.hasNext()) {
                Location lastKnownLocation = TwilightService.this.mLocationManager.getLastKnownLocation(providers.next());
                if (location == null || (lastKnownLocation != null && location.getElapsedRealtimeNanos() < lastKnownLocation.getElapsedRealtimeNanos())) {
                    location = lastKnownLocation;
                }
            }
            if (location == null) {
                Time currentTime = new Time();
                currentTime.set(System.currentTimeMillis());
                double lngOffset = FACTOR_GMT_OFFSET_LONGITUDE * (currentTime.gmtoff - ((long) (currentTime.isDst > 0 ? 3600 : 0)));
                location = new Location("fake");
                location.setLongitude(lngOffset);
                location.setLatitude(0.0d);
                location.setAccuracy(417000.0f);
                location.setTime(System.currentTimeMillis());
                location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
            }
            setLocation(location);
        }

        private void setLocation(Location location) {
            this.mLocation = location;
            updateTwilightState();
        }

        private void updateTwilightState() {
            long nextUpdate;
            if (this.mLocation == null) {
                TwilightService.this.setTwilightState(null);
                return;
            }
            long now = System.currentTimeMillis();
            this.mTwilightCalculator.calculateTwilight(now - LOCATION_UPDATE_MS, this.mLocation.getLatitude(), this.mLocation.getLongitude());
            long yesterdaySunset = this.mTwilightCalculator.mSunset;
            this.mTwilightCalculator.calculateTwilight(now, this.mLocation.getLatitude(), this.mLocation.getLongitude());
            boolean isNight = this.mTwilightCalculator.mState == 1 ? true : TwilightService.DEBUG;
            long todaySunrise = this.mTwilightCalculator.mSunrise;
            long todaySunset = this.mTwilightCalculator.mSunset;
            this.mTwilightCalculator.calculateTwilight(LOCATION_UPDATE_MS + now, this.mLocation.getLatitude(), this.mLocation.getLongitude());
            long tomorrowSunrise = this.mTwilightCalculator.mSunrise;
            TwilightState state = new TwilightState(isNight, yesterdaySunset, todaySunrise, todaySunset, tomorrowSunrise);
            TwilightService.this.setTwilightState(state);
            if (todaySunrise != -1 && todaySunset != -1) {
                long nextUpdate2 = 0 + 60000;
                if (now > todaySunset) {
                    nextUpdate = nextUpdate2 + tomorrowSunrise;
                } else if (now > todaySunrise) {
                    nextUpdate = nextUpdate2 + todaySunset;
                } else {
                    nextUpdate = nextUpdate2 + todaySunrise;
                }
            } else {
                nextUpdate = now + 43200000;
            }
            Intent updateIntent = new Intent(TwilightService.ACTION_UPDATE_TWILIGHT_STATE);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(TwilightService.this.getContext(), 0, updateIntent, 0);
            TwilightService.this.mAlarmManager.cancel(pendingIntent);
            TwilightService.this.mAlarmManager.setExact(1, nextUpdate, pendingIntent);
        }
    }
}
