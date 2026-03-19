package com.android.server.twilight;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.icu.impl.CalendarAstronomer;
import android.icu.util.Calendar;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.ArrayMap;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.server.SystemService;
import java.util.Objects;

public final class TwilightService extends SystemService implements AlarmManager.OnAlarmListener, Handler.Callback, LocationListener {
    private static final boolean DEBUG = false;
    private static final int MSG_START_LISTENING = 1;
    private static final int MSG_STOP_LISTENING = 2;
    private static final String TAG = "TwilightService";
    protected AlarmManager mAlarmManager;
    private boolean mBootCompleted;
    private final Handler mHandler;
    private boolean mHasListeners;
    protected Location mLastLocation;

    @GuardedBy("mListeners")
    protected TwilightState mLastTwilightState;

    @GuardedBy("mListeners")
    private final ArrayMap<TwilightListener, Handler> mListeners;
    private LocationManager mLocationManager;
    private BroadcastReceiver mTimeChangedReceiver;

    public TwilightService(Context context) {
        super(context);
        this.mListeners = new ArrayMap<>();
        this.mHandler = new Handler(Looper.getMainLooper(), this);
    }

    @Override
    public void onStart() {
        publishLocalService(TwilightManager.class, new TwilightManager() {
            @Override
            public void registerListener(TwilightListener twilightListener, Handler handler) {
                synchronized (TwilightService.this.mListeners) {
                    boolean zIsEmpty = TwilightService.this.mListeners.isEmpty();
                    TwilightService.this.mListeners.put(twilightListener, handler);
                    if (zIsEmpty && !TwilightService.this.mListeners.isEmpty()) {
                        TwilightService.this.mHandler.sendEmptyMessage(1);
                    }
                }
            }

            @Override
            public void unregisterListener(TwilightListener twilightListener) {
                synchronized (TwilightService.this.mListeners) {
                    boolean zIsEmpty = TwilightService.this.mListeners.isEmpty();
                    TwilightService.this.mListeners.remove(twilightListener);
                    if (!zIsEmpty && TwilightService.this.mListeners.isEmpty()) {
                        TwilightService.this.mHandler.sendEmptyMessage(2);
                    }
                }
            }

            @Override
            public TwilightState getLastTwilightState() {
                TwilightState twilightState;
                synchronized (TwilightService.this.mListeners) {
                    twilightState = TwilightService.this.mLastTwilightState;
                }
                return twilightState;
            }
        });
    }

    @Override
    public void onBootPhase(int i) {
        if (i == 1000) {
            Context context = getContext();
            this.mAlarmManager = (AlarmManager) context.getSystemService("alarm");
            this.mLocationManager = (LocationManager) context.getSystemService("location");
            this.mBootCompleted = true;
            if (this.mHasListeners) {
                startListening();
            }
        }
    }

    @Override
    public boolean handleMessage(Message message) {
        switch (message.what) {
            case 1:
                if (!this.mHasListeners) {
                    this.mHasListeners = true;
                    if (this.mBootCompleted) {
                        startListening();
                    }
                }
                return true;
            case 2:
                if (this.mHasListeners) {
                    this.mHasListeners = false;
                    if (this.mBootCompleted) {
                        stopListening();
                    }
                }
                return true;
            default:
                return false;
        }
    }

    private void startListening() {
        Slog.d(TAG, "startListening");
        this.mLocationManager.requestLocationUpdates((LocationRequest) null, this, Looper.getMainLooper());
        if (this.mLocationManager.getLastLocation() == null) {
            if (this.mLocationManager.isProviderEnabled("network")) {
                this.mLocationManager.requestSingleUpdate("network", this, Looper.getMainLooper());
            } else if (this.mLocationManager.isProviderEnabled("gps")) {
                this.mLocationManager.requestSingleUpdate("gps", this, Looper.getMainLooper());
            }
        }
        if (this.mTimeChangedReceiver == null) {
            this.mTimeChangedReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Slog.d(TwilightService.TAG, "onReceive: " + intent);
                    TwilightService.this.updateTwilightState();
                }
            };
            IntentFilter intentFilter = new IntentFilter("android.intent.action.TIME_SET");
            intentFilter.addAction("android.intent.action.TIMEZONE_CHANGED");
            getContext().registerReceiver(this.mTimeChangedReceiver, intentFilter);
        }
        updateTwilightState();
    }

    private void stopListening() {
        Slog.d(TAG, "stopListening");
        if (this.mTimeChangedReceiver != null) {
            getContext().unregisterReceiver(this.mTimeChangedReceiver);
            this.mTimeChangedReceiver = null;
        }
        if (this.mLastTwilightState != null) {
            this.mAlarmManager.cancel(this);
        }
        this.mLocationManager.removeUpdates(this);
        this.mLastLocation = null;
    }

    private void updateTwilightState() {
        final TwilightState twilightStateCalculateTwilightState = calculateTwilightState(this.mLastLocation != null ? this.mLastLocation : this.mLocationManager.getLastLocation(), System.currentTimeMillis());
        synchronized (this.mListeners) {
            if (!Objects.equals(this.mLastTwilightState, twilightStateCalculateTwilightState)) {
                this.mLastTwilightState = twilightStateCalculateTwilightState;
                for (int size = this.mListeners.size() - 1; size >= 0; size--) {
                    final TwilightListener twilightListenerKeyAt = this.mListeners.keyAt(size);
                    this.mListeners.valueAt(size).post(new Runnable() {
                        @Override
                        public void run() {
                            twilightListenerKeyAt.onTwilightStateChanged(twilightStateCalculateTwilightState);
                        }
                    });
                }
            }
        }
        if (twilightStateCalculateTwilightState != null) {
            this.mAlarmManager.setExact(1, twilightStateCalculateTwilightState.isNight() ? twilightStateCalculateTwilightState.sunriseTimeMillis() : twilightStateCalculateTwilightState.sunsetTimeMillis(), TAG, this, this.mHandler);
        }
    }

    @Override
    public void onAlarm() {
        Slog.d(TAG, "onAlarm");
        updateTwilightState();
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            if (location.getLongitude() != 0.0d || location.getLatitude() != 0.0d) {
                Slog.d(TAG, "onLocationChanged: provider=" + location.getProvider() + " accuracy=" + location.getAccuracy() + " time=" + location.getTime());
                this.mLastLocation = location;
                updateTwilightState();
            }
        }
    }

    @Override
    public void onStatusChanged(String str, int i, Bundle bundle) {
    }

    @Override
    public void onProviderEnabled(String str) {
    }

    @Override
    public void onProviderDisabled(String str) {
    }

    private static TwilightState calculateTwilightState(Location location, long j) {
        if (location == null) {
            return null;
        }
        CalendarAstronomer calendarAstronomer = new CalendarAstronomer(location.getLongitude(), location.getLatitude());
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(j);
        calendar.set(11, 12);
        calendar.set(12, 0);
        calendar.set(13, 0);
        calendar.set(14, 0);
        calendarAstronomer.setTime(calendar.getTimeInMillis());
        long sunRiseSet = calendarAstronomer.getSunRiseSet(true);
        long sunRiseSet2 = calendarAstronomer.getSunRiseSet(false);
        if (sunRiseSet2 < j) {
            calendar.add(5, 1);
            calendarAstronomer.setTime(calendar.getTimeInMillis());
            sunRiseSet = calendarAstronomer.getSunRiseSet(true);
        } else if (sunRiseSet > j) {
            calendar.add(5, -1);
            calendarAstronomer.setTime(calendar.getTimeInMillis());
            sunRiseSet2 = calendarAstronomer.getSunRiseSet(false);
        }
        return new TwilightState(sunRiseSet, sunRiseSet2);
    }
}
