package com.mediatek.location;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.ContentObserver;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;
import com.mediatek.cta.CtaManagerFactory;
import com.mediatek.internal.R;
import java.util.Calendar;

public class MtkLocationExt {
    private static final boolean DEBUG = true;
    private static final String TAG = "MtkLocationExt";

    public static class GnssLocationProvider {
        private static final int EVENT_GPS_TIME_SYNC_CHANGED = 4;
        private static final String INJECT_NLP_LOC = "com.mediatek.location.INJECT_NLP_LOC";
        private static final int UPDATE_LOCATION = 7;
        private final Context mContext;
        private Handler mGpsHandler;
        private GpsTimeSyncObserver mGpsTimeSyncObserver;
        private Thread mGpsTimerThread;
        private final Handler mHandler;
        private Location mLastLocation;
        private LocationManager mLocationManager;
        private ServiceConnection mServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                Log.d(MtkLocationExt.TAG, "LPPe service onServiceConnected");
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                Log.d(MtkLocationExt.TAG, "LPPe service onServiceDisconnected");
            }
        };
        private boolean mIsGpsTimeSyncRunning = false;
        private LocationListener mPassiveLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if ("gps".equals(location.getProvider())) {
                    GnssLocationProvider.this.doSystemTimeSyncByGps((location.getLatitude() == 0.0d || location.getLongitude() == 0.0d) ? false : MtkLocationExt.DEBUG, location.getTime());
                }
            }

            @Override
            public void onProviderDisabled(String str) {
            }

            @Override
            public void onProviderEnabled(String str) {
            }

            @Override
            public void onStatusChanged(String str, int i, Bundle bundle) {
            }
        };
        private Handler mGpsToastHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                Toast.makeText(GnssLocationProvider.this.mContext, (String) message.obj, 1).show();
            }
        };
        private LocationListener mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                GnssLocationProvider.this.mGpsTimerThread.interrupt();
            }

            @Override
            public void onProviderDisabled(String str) {
            }

            @Override
            public void onProviderEnabled(String str) {
            }

            @Override
            public void onStatusChanged(String str, int i, Bundle bundle) {
            }
        };

        public GnssLocationProvider(Context context, Handler handler) {
            Log.d(MtkLocationExt.TAG, "MtkLocationExt GnssLocationProvider()");
            this.mContext = context;
            this.mHandler = handler;
            registerIntentReceiver();
            Log.d(MtkLocationExt.TAG, "add GPS time sync handler and looper");
            this.mGpsHandler = new MyHandler(this.mHandler.getLooper());
            Context context2 = this.mContext;
            Context context3 = this.mContext;
            this.mLocationManager = (LocationManager) context2.getSystemService("location");
            this.mGpsTimeSyncObserver = new GpsTimeSyncObserver(this.mGpsHandler, 4);
            this.mGpsTimeSyncObserver.observe(this.mContext);
        }

        private void bindLPPeService() {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.mediatek.location.lppe.main", "com.mediatek.location.lppe.main.LPPeServiceWrapper"));
            Log.d(MtkLocationExt.TAG, "binding lppe service bound = " + this.mContext.bindServiceAsUser(intent, this.mServiceConnection, 1073741829, new UserHandle(0)));
        }

        private void bindNlpService() {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.mediatek.nlpservice", "com.mediatek.nlpservice.NlpService"));
            Log.d(MtkLocationExt.TAG, "binding nlp service bound = " + this.mContext.bindServiceAsUser(intent, this.mServiceConnection, 1073741829, new UserHandle(0)));
        }

        private void registerIntentReceiver() {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.BOOT_COMPLETED");
            intentFilter.addAction(INJECT_NLP_LOC);
            this.mContext.registerReceiverAsUser(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if ("android.intent.action.BOOT_COMPLETED".equals(action)) {
                        GnssLocationProvider.this.bindLPPeService();
                        GnssLocationProvider.this.bindNlpService();
                    } else if (GnssLocationProvider.INJECT_NLP_LOC.equals(action)) {
                        GnssLocationProvider.this.injectRefLocation(GnssLocationProvider.this.mLocationManager.getLastKnownLocation("network"));
                    }
                }
            }, UserHandle.ALL, intentFilter, null, this.mHandler);
        }

        private void injectRefLocation(Location location) {
            Log.d(MtkLocationExt.TAG, "injectRefLocation");
            if (location != null) {
                this.mHandler.obtainMessage(7, 0, 0, location).sendToTarget();
            }
        }

        private class MyHandler extends Handler {
            public MyHandler(Looper looper) {
                super(looper);
            }

            @Override
            public void handleMessage(Message message) {
                if (message.what == 4) {
                    boolean gpsTimeSyncState = GnssLocationProvider.this.getGpsTimeSyncState();
                    Log.d(MtkLocationExt.TAG, "GPS Time sync is changed to " + gpsTimeSyncState);
                    GnssLocationProvider.this.onGpsTimeChanged(gpsTimeSyncState);
                }
            }
        }

        private boolean getGpsTimeSyncState() {
            try {
                if (Settings.Global.getInt(this.mContext.getContentResolver(), "auto_time_gps") > 0) {
                    return MtkLocationExt.DEBUG;
                }
                return false;
            } catch (Settings.SettingNotFoundException e) {
                return false;
            }
        }

        private static class GpsTimeSyncObserver extends ContentObserver {
            private Handler mHandler;
            private int mMsg;

            GpsTimeSyncObserver(Handler handler, int i) {
                super(handler);
                this.mHandler = handler;
                this.mMsg = i;
            }

            void observe(Context context) {
                context.getContentResolver().registerContentObserver(Settings.Global.getUriFor("auto_time_gps"), false, this);
            }

            @Override
            public void onChange(boolean z) {
                this.mHandler.obtainMessage(this.mMsg).sendToTarget();
            }
        }

        public void onGpsTimeChanged(boolean z) {
            if (z) {
                startUsingGpsWithTimeout(180000, this.mContext.getString(R.string.gps_time_sync_fail_str));
            } else if (this.mGpsTimerThread != null) {
                this.mGpsTimerThread.interrupt();
            }
            setGpsTimeSyncFlag(z);
        }

        private void setGpsTimeSyncFlag(boolean z) {
            Log.d(MtkLocationExt.TAG, "setGpsTimeSyncFlag: " + z);
            if (z) {
                this.mLocationManager.requestLocationUpdates("passive", 0L, 0.0f, this.mPassiveLocationListener);
            } else {
                this.mLocationManager.removeUpdates(this.mPassiveLocationListener);
            }
        }

        private void doSystemTimeSyncByGps(boolean z, long j) {
            if (z) {
                Log.d(MtkLocationExt.TAG, " ########## Auto-sync time with GPS: timestamp = " + j + " ########## ");
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(j);
                long timeInMillis = calendar.getTimeInMillis();
                if (timeInMillis / 1000 < 2147483647L) {
                    SystemClock.setCurrentTimeMillis(timeInMillis);
                }
                this.mLocationManager.removeUpdates(this.mPassiveLocationListener);
            }
        }

        public void startUsingGpsWithTimeout(final int i, final String str) {
            if (this.mIsGpsTimeSyncRunning) {
                Log.d(MtkLocationExt.TAG, "WARNING: Gps Time Sync is already run");
                return;
            }
            this.mIsGpsTimeSyncRunning = MtkLocationExt.DEBUG;
            Log.d(MtkLocationExt.TAG, "start using GPS for GPS time sync timeout=" + i + " timeoutMsg=" + str);
            this.mLocationManager.requestLocationUpdates("gps", 1000L, 0.0f, this.mLocationListener);
            this.mGpsTimerThread = new Thread() {
                @Override
                public void run() {
                    boolean z;
                    try {
                        Thread.sleep(i);
                        z = true;
                    } catch (InterruptedException e) {
                        z = false;
                    }
                    Log.d(MtkLocationExt.TAG, "isTimeout=" + z);
                    if (z) {
                        Message message = new Message();
                        message.obj = str;
                        GnssLocationProvider.this.mGpsToastHandler.sendMessage(message);
                    }
                    GnssLocationProvider.this.mLocationManager.removeUpdates(GnssLocationProvider.this.mLocationListener);
                    GnssLocationProvider.this.mIsGpsTimeSyncRunning = false;
                }
            };
            this.mGpsTimerThread.start();
        }
    }

    public static class LocationManagerService {
        private final Context mContext;
        private final Handler mHandler;

        public LocationManagerService(Context context, Handler handler) {
            Log.d(MtkLocationExt.TAG, "MtkLocationExt LocationManagerService()");
            this.mContext = context;
            this.mHandler = handler;
        }

        public boolean isCtaFeatureSupport() {
            return CtaManagerFactory.getInstance().makeCtaManager().isCtaSupported();
        }
    }
}
