package com.android.server;

import android.content.Context;
import android.content.Intent;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorAdditionalInfo;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.Slog;

public class SensorNotificationService extends SystemService implements SensorEventListener, LocationListener {
    private static final boolean DBG = false;
    private static final long KM_IN_M = 1000;
    private static final long LOCATION_MIN_DISTANCE = 100000;
    private static final long LOCATION_MIN_TIME = 1800000;
    private static final long MILLIS_2010_1_1 = 1262358000000L;
    private static final long MINUTE_IN_MS = 60000;
    private static final String PROPERTY_USE_MOCKED_LOCATION = "sensor.notification.use_mocked";
    private static final String TAG = "SensorNotificationService";
    private Context mContext;
    private long mLocalGeomagneticFieldUpdateTime;
    private LocationManager mLocationManager;
    private Sensor mMetaSensor;
    private SensorManager mSensorManager;

    public SensorNotificationService(Context context) {
        super(context);
        this.mLocalGeomagneticFieldUpdateTime = -1800000L;
        this.mContext = context;
    }

    @Override
    public void onStart() {
        LocalServices.addService(SensorNotificationService.class, this);
    }

    @Override
    public void onBootPhase(int i) {
        if (i == 600) {
            this.mSensorManager = (SensorManager) this.mContext.getSystemService("sensor");
            this.mMetaSensor = this.mSensorManager.getDefaultSensor(32);
            if (this.mMetaSensor != null) {
                this.mSensorManager.registerListener(this, this.mMetaSensor, 0);
            }
        }
        if (i == 1000) {
            this.mLocationManager = (LocationManager) this.mContext.getSystemService("location");
            if (this.mLocationManager != null) {
                this.mLocationManager.requestLocationUpdates("passive", 1800000L, 100000.0f, this);
            }
        }
    }

    private void broadcastDynamicSensorChanged() {
        Intent intent = new Intent("android.intent.action.DYNAMIC_SENSOR_CHANGED");
        intent.setFlags(1073741824);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor == this.mMetaSensor) {
            broadcastDynamicSensorChanged();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if ((location.getLatitude() == 0.0d && location.getLongitude() == 0.0d) || SystemClock.elapsedRealtime() - this.mLocalGeomagneticFieldUpdateTime < 600000) {
            return;
        }
        long jCurrentTimeMillis = System.currentTimeMillis();
        if (useMockedLocation() == location.isFromMockProvider() || jCurrentTimeMillis < MILLIS_2010_1_1) {
            return;
        }
        GeomagneticField geomagneticField = new GeomagneticField((float) location.getLatitude(), (float) location.getLongitude(), (float) location.getAltitude(), jCurrentTimeMillis);
        try {
            SensorAdditionalInfo sensorAdditionalInfoCreateLocalGeomagneticField = SensorAdditionalInfo.createLocalGeomagneticField(geomagneticField.getFieldStrength() / 1000.0f, (float) ((((double) geomagneticField.getDeclination()) * 3.141592653589793d) / 180.0d), (float) ((((double) geomagneticField.getInclination()) * 3.141592653589793d) / 180.0d));
            if (sensorAdditionalInfoCreateLocalGeomagneticField != null) {
                this.mSensorManager.setOperationParameter(sensorAdditionalInfoCreateLocalGeomagneticField);
                this.mLocalGeomagneticFieldUpdateTime = SystemClock.elapsedRealtime();
            }
        } catch (IllegalArgumentException e) {
            Slog.e(TAG, "Invalid local geomagnetic field, ignore.");
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
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

    private boolean useMockedLocation() {
        return "false".equals(System.getProperty(PROPERTY_USE_MOCKED_LOCATION, "false"));
    }
}
