package com.android.settings.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.hardware.display.DisplayManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Process;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.RemoteViews;
import com.android.settings.R;
import com.android.settings.bluetooth.Utils;
import com.android.settingslib.bluetooth.LocalBluetoothAdapter;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.wifi.AccessPoint;

public class SettingsAppWidgetProvider extends AppWidgetProvider {
    private static final StateTracker sBluetoothState;
    private static final StateTracker sLocationState;
    private static SettingsObserver sSettingsObserver;
    private static final StateTracker sSyncState;
    private static final StateTracker sWifiState;
    static final ComponentName THIS_APPWIDGET = new ComponentName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider");
    private static LocalBluetoothAdapter sLocalBluetoothAdapter = null;
    private static final int[] IND_DRAWABLE_OFF = {R.drawable.appwidget_settings_ind_off_l_holo, R.drawable.appwidget_settings_ind_off_c_holo, R.drawable.appwidget_settings_ind_off_r_holo};
    private static final int[] IND_DRAWABLE_MID = {R.drawable.appwidget_settings_ind_mid_l_holo, R.drawable.appwidget_settings_ind_mid_c_holo, R.drawable.appwidget_settings_ind_mid_r_holo};
    private static final int[] IND_DRAWABLE_ON = {R.drawable.appwidget_settings_ind_on_l_holo, R.drawable.appwidget_settings_ind_on_c_holo, R.drawable.appwidget_settings_ind_on_r_holo};

    static {
        sWifiState = new WifiStateTracker();
        sBluetoothState = new BluetoothStateTracker();
        sLocationState = new LocationStateTracker();
        sSyncState = new SyncStateTracker();
    }

    private static abstract class StateTracker {
        private Boolean mActualState;
        private boolean mDeferredStateChangeRequestNeeded;
        private boolean mInTransition;
        private Boolean mIntendedState;

        public abstract int getActualState(Context context);

        public abstract int getButtonDescription();

        public abstract int getButtonId();

        public abstract int getButtonImageId(boolean z);

        public abstract int getContainerId();

        public abstract int getIndicatorId();

        public abstract void onActualStateChange(Context context, Intent intent);

        protected abstract void requestStateChange(Context context, boolean z);

        private StateTracker() {
            this.mInTransition = false;
            this.mActualState = null;
            this.mIntendedState = null;
            this.mDeferredStateChangeRequestNeeded = false;
        }

        public final void toggleState(Context context) {
            int triState = getTriState(context);
            boolean z = false;
            if (triState == 5) {
                if (this.mIntendedState != null) {
                    z = !this.mIntendedState.booleanValue();
                }
            } else {
                switch (triState) {
                    case 0:
                        z = true;
                        break;
                }
            }
            this.mIntendedState = Boolean.valueOf(z);
            if (this.mInTransition) {
                this.mDeferredStateChangeRequestNeeded = true;
            } else {
                this.mInTransition = true;
                requestStateChange(context, z);
            }
        }

        public int getPosition() {
            return 1;
        }

        public final void setImageViewResources(Context context, RemoteViews remoteViews) {
            int containerId = getContainerId();
            int buttonId = getButtonId();
            int indicatorId = getIndicatorId();
            int position = getPosition();
            int triState = getTriState(context);
            if (triState != 5) {
                switch (triState) {
                    case 0:
                        remoteViews.setContentDescription(containerId, getContentDescription(context, R.string.gadget_state_off));
                        remoteViews.setImageViewResource(buttonId, getButtonImageId(false));
                        remoteViews.setImageViewResource(indicatorId, SettingsAppWidgetProvider.IND_DRAWABLE_OFF[position]);
                        break;
                    case 1:
                        remoteViews.setContentDescription(containerId, getContentDescription(context, R.string.gadget_state_on));
                        remoteViews.setImageViewResource(buttonId, getButtonImageId(true));
                        remoteViews.setImageViewResource(indicatorId, SettingsAppWidgetProvider.IND_DRAWABLE_ON[position]);
                        break;
                }
            }
            if (isTurningOn()) {
                remoteViews.setContentDescription(containerId, getContentDescription(context, R.string.gadget_state_turning_on));
                remoteViews.setImageViewResource(buttonId, getButtonImageId(true));
                remoteViews.setImageViewResource(indicatorId, SettingsAppWidgetProvider.IND_DRAWABLE_MID[position]);
            } else {
                remoteViews.setContentDescription(containerId, getContentDescription(context, R.string.gadget_state_turning_off));
                remoteViews.setImageViewResource(buttonId, getButtonImageId(false));
                remoteViews.setImageViewResource(indicatorId, SettingsAppWidgetProvider.IND_DRAWABLE_OFF[position]);
            }
        }

        private final String getContentDescription(Context context, int i) {
            return context.getString(R.string.gadget_state_template, context.getString(getButtonDescription()), context.getString(i));
        }

        protected final void setCurrentState(Context context, int i) {
            boolean z = this.mInTransition;
            switch (i) {
                case 0:
                    this.mInTransition = false;
                    this.mActualState = false;
                    break;
                case 1:
                    this.mInTransition = false;
                    this.mActualState = true;
                    break;
                case 2:
                    this.mInTransition = true;
                    this.mActualState = false;
                    break;
                case 3:
                    this.mInTransition = true;
                    this.mActualState = true;
                    break;
            }
            if (z && !this.mInTransition && this.mDeferredStateChangeRequestNeeded) {
                Log.v("SettingsAppWidgetProvider", "processing deferred state change");
                if (this.mActualState != null && this.mIntendedState != null && this.mIntendedState.equals(this.mActualState)) {
                    Log.v("SettingsAppWidgetProvider", "... but intended state matches, so no changes.");
                } else if (this.mIntendedState != null) {
                    this.mInTransition = true;
                    requestStateChange(context, this.mIntendedState.booleanValue());
                }
                this.mDeferredStateChangeRequestNeeded = false;
            }
        }

        public final boolean isTurningOn() {
            return this.mIntendedState != null && this.mIntendedState.booleanValue();
        }

        public final int getTriState(Context context) {
            if (this.mInTransition) {
                return 5;
            }
            switch (getActualState(context)) {
            }
            return 5;
        }
    }

    private static final class WifiStateTracker extends StateTracker {
        private WifiStateTracker() {
            super();
        }

        @Override
        public int getContainerId() {
            return R.id.btn_wifi;
        }

        @Override
        public int getButtonId() {
            return R.id.img_wifi;
        }

        @Override
        public int getIndicatorId() {
            return R.id.ind_wifi;
        }

        @Override
        public int getButtonDescription() {
            return R.string.gadget_wifi;
        }

        @Override
        public int getButtonImageId(boolean z) {
            return z ? R.drawable.ic_appwidget_settings_wifi_on_holo : R.drawable.ic_appwidget_settings_wifi_off_holo;
        }

        @Override
        public int getPosition() {
            return 0;
        }

        @Override
        public int getActualState(Context context) {
            WifiManager wifiManager = (WifiManager) context.getSystemService("wifi");
            if (wifiManager != null) {
                return wifiStateToFiveState(wifiManager.getWifiState());
            }
            return 4;
        }

        @Override
        protected void requestStateChange(final Context context, final boolean z) {
            final WifiManager wifiManager = (WifiManager) context.getSystemService("wifi");
            if (wifiManager == null) {
                Log.d("SettingsAppWidgetProvider", "No wifiManager.");
            } else {
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    public Void doInBackground(Void... voidArr) {
                        int wifiApState = wifiManager.getWifiApState();
                        if (z && (wifiApState == 12 || wifiApState == 13)) {
                            ((ConnectivityManager) context.getSystemService("connectivity")).stopTethering(0);
                        }
                        wifiManager.setWifiEnabled(z);
                        return null;
                    }
                }.execute(new Void[0]);
            }
        }

        @Override
        public void onActualStateChange(Context context, Intent intent) {
            if (!"android.net.wifi.WIFI_STATE_CHANGED".equals(intent.getAction())) {
                return;
            }
            setCurrentState(context, wifiStateToFiveState(intent.getIntExtra("wifi_state", -1)));
        }

        private static int wifiStateToFiveState(int i) {
            switch (i) {
                case 0:
                    return 3;
                case 1:
                    return 0;
                case 2:
                    return 2;
                case 3:
                    return 1;
                default:
                    return 4;
            }
        }
    }

    private static final class BluetoothStateTracker extends StateTracker {
        private BluetoothStateTracker() {
            super();
        }

        @Override
        public int getContainerId() {
            return R.id.btn_bluetooth;
        }

        @Override
        public int getButtonId() {
            return R.id.img_bluetooth;
        }

        @Override
        public int getIndicatorId() {
            return R.id.ind_bluetooth;
        }

        @Override
        public int getButtonDescription() {
            return R.string.gadget_bluetooth;
        }

        @Override
        public int getButtonImageId(boolean z) {
            return z ? R.drawable.ic_appwidget_settings_bluetooth_on_holo : R.drawable.ic_appwidget_settings_bluetooth_off_holo;
        }

        @Override
        public int getActualState(Context context) {
            if (SettingsAppWidgetProvider.sLocalBluetoothAdapter == null) {
                LocalBluetoothManager localBtManager = Utils.getLocalBtManager(context);
                if (localBtManager != null) {
                    LocalBluetoothAdapter unused = SettingsAppWidgetProvider.sLocalBluetoothAdapter = localBtManager.getBluetoothAdapter();
                    if (SettingsAppWidgetProvider.sLocalBluetoothAdapter == null) {
                        return 4;
                    }
                } else {
                    return 4;
                }
            }
            return bluetoothStateToFiveState(SettingsAppWidgetProvider.sLocalBluetoothAdapter.getBluetoothState());
        }

        @Override
        protected void requestStateChange(Context context, final boolean z) {
            if (SettingsAppWidgetProvider.sLocalBluetoothAdapter == null) {
                Log.d("SettingsAppWidgetProvider", "No LocalBluetoothManager");
            } else {
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    public Void doInBackground(Void... voidArr) {
                        SettingsAppWidgetProvider.sLocalBluetoothAdapter.setBluetoothEnabled(z);
                        return null;
                    }
                }.execute(new Void[0]);
            }
        }

        @Override
        public void onActualStateChange(Context context, Intent intent) {
            if (!"android.bluetooth.adapter.action.STATE_CHANGED".equals(intent.getAction())) {
                return;
            }
            setCurrentState(context, bluetoothStateToFiveState(intent.getIntExtra("android.bluetooth.adapter.extra.STATE", -1)));
        }

        private static int bluetoothStateToFiveState(int i) {
            switch (i) {
                case AccessPoint.Speed.MODERATE:
                    return 0;
                case 11:
                    return 2;
                case 12:
                    return 1;
                case 13:
                    return 3;
                default:
                    return 4;
            }
        }
    }

    private static final class LocationStateTracker extends StateTracker {
        private int mCurrentLocationMode;

        private LocationStateTracker() {
            super();
            this.mCurrentLocationMode = 0;
        }

        @Override
        public int getContainerId() {
            return R.id.btn_location;
        }

        @Override
        public int getButtonId() {
            return R.id.img_location;
        }

        @Override
        public int getIndicatorId() {
            return R.id.ind_location;
        }

        @Override
        public int getButtonDescription() {
            return R.string.gadget_location;
        }

        @Override
        public int getButtonImageId(boolean z) {
            if (z) {
                int i = this.mCurrentLocationMode;
                if (i == 1 || i == 3) {
                    return R.drawable.ic_appwidget_settings_location_on_holo;
                }
                return R.drawable.ic_appwidget_settings_location_saving_holo;
            }
            return R.drawable.ic_appwidget_settings_location_off_holo;
        }

        @Override
        public int getActualState(Context context) {
            this.mCurrentLocationMode = Settings.Secure.getInt(context.getContentResolver(), "location_mode", 0);
            return this.mCurrentLocationMode == 0 ? 0 : 1;
        }

        @Override
        public void onActualStateChange(Context context, Intent intent) {
            setCurrentState(context, getActualState(context));
        }

        @Override
        public void requestStateChange(final Context context, boolean z) {
            context.getContentResolver();
            new AsyncTask<Void, Void, Boolean>() {
                @Override
                public Boolean doInBackground(Void... voidArr) {
                    if (((UserManager) context.getSystemService("user")).hasUserRestriction("no_share_location")) {
                        return Boolean.valueOf(LocationStateTracker.this.getActualState(context) == 1);
                    }
                    LocationManager locationManager = (LocationManager) context.getSystemService("location");
                    locationManager.setLocationEnabledForUser(true ^ locationManager.isLocationEnabled(), Process.myUserHandle());
                    return Boolean.valueOf(locationManager.isLocationEnabled());
                }

                @Override
                public void onPostExecute(Boolean bool) {
                    LocationStateTracker.this.setCurrentState(context, bool.booleanValue() ? 1 : 0);
                    SettingsAppWidgetProvider.updateWidget(context);
                }
            }.execute(new Void[0]);
        }
    }

    private static final class SyncStateTracker extends StateTracker {
        private SyncStateTracker() {
            super();
        }

        @Override
        public int getContainerId() {
            return R.id.btn_sync;
        }

        @Override
        public int getButtonId() {
            return R.id.img_sync;
        }

        @Override
        public int getIndicatorId() {
            return R.id.ind_sync;
        }

        @Override
        public int getButtonDescription() {
            return R.string.gadget_sync;
        }

        @Override
        public int getButtonImageId(boolean z) {
            return z ? R.drawable.ic_appwidget_settings_sync_on_holo : R.drawable.ic_appwidget_settings_sync_off_holo;
        }

        @Override
        public int getActualState(Context context) {
            return ContentResolver.getMasterSyncAutomatically() ? 1 : 0;
        }

        @Override
        public void onActualStateChange(Context context, Intent intent) {
            setCurrentState(context, getActualState(context));
        }

        @Override
        public void requestStateChange(final Context context, final boolean z) {
            final boolean masterSyncAutomatically = ContentResolver.getMasterSyncAutomatically();
            new AsyncTask<Void, Void, Boolean>() {
                @Override
                public Boolean doInBackground(Void... voidArr) {
                    if (z) {
                        if (!masterSyncAutomatically) {
                            ContentResolver.setMasterSyncAutomatically(true);
                        }
                        return true;
                    }
                    if (masterSyncAutomatically) {
                        ContentResolver.setMasterSyncAutomatically(false);
                    }
                    return false;
                }

                @Override
                public void onPostExecute(Boolean bool) {
                    SyncStateTracker.this.setCurrentState(context, bool.booleanValue() ? 1 : 0);
                    SettingsAppWidgetProvider.updateWidget(context);
                }
            }.execute(new Void[0]);
        }
    }

    private static void checkObserver(Context context) {
        if (sSettingsObserver == null) {
            sSettingsObserver = new SettingsObserver(new Handler(), context.getApplicationContext());
            sSettingsObserver.startObserving();
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] iArr) {
        RemoteViews remoteViewsBuildUpdate = buildUpdate(context);
        for (int i : iArr) {
            appWidgetManager.updateAppWidget(i, remoteViewsBuildUpdate);
        }
    }

    @Override
    public void onEnabled(Context context) {
        checkObserver(context);
    }

    @Override
    public void onDisabled(Context context) {
        if (sSettingsObserver != null) {
            sSettingsObserver.stopObserving();
            sSettingsObserver = null;
        }
    }

    static RemoteViews buildUpdate(Context context) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget);
        remoteViews.setOnClickPendingIntent(R.id.btn_wifi, getLaunchPendingIntent(context, 0));
        remoteViews.setOnClickPendingIntent(R.id.btn_brightness, getLaunchPendingIntent(context, 1));
        remoteViews.setOnClickPendingIntent(R.id.btn_sync, getLaunchPendingIntent(context, 2));
        remoteViews.setOnClickPendingIntent(R.id.btn_location, getLaunchPendingIntent(context, 3));
        remoteViews.setOnClickPendingIntent(R.id.btn_bluetooth, getLaunchPendingIntent(context, 4));
        updateButtons(remoteViews, context);
        return remoteViews;
    }

    public static void updateWidget(Context context) {
        AppWidgetManager.getInstance(context).updateAppWidget(THIS_APPWIDGET, buildUpdate(context));
        checkObserver(context);
    }

    private static void updateButtons(RemoteViews remoteViews, Context context) {
        sWifiState.setImageViewResources(context, remoteViews);
        sBluetoothState.setImageViewResources(context, remoteViews);
        sLocationState.setImageViewResources(context, remoteViews);
        sSyncState.setImageViewResources(context, remoteViews);
        if (getBrightnessMode(context)) {
            remoteViews.setContentDescription(R.id.btn_brightness, context.getString(R.string.gadget_brightness_template, context.getString(R.string.gadget_brightness_state_auto)));
            remoteViews.setImageViewResource(R.id.img_brightness, R.drawable.ic_appwidget_settings_brightness_auto_holo);
            remoteViews.setImageViewResource(R.id.ind_brightness, R.drawable.appwidget_settings_ind_on_r_holo);
            return;
        }
        int brightness = getBrightness(context);
        PowerManager powerManager = (PowerManager) context.getSystemService(PowerManager.class);
        int maximumScreenBrightnessSetting = (int) (powerManager.getMaximumScreenBrightnessSetting() * 0.8f);
        int maximumScreenBrightnessSetting2 = (int) (powerManager.getMaximumScreenBrightnessSetting() * 0.3f);
        if (brightness > maximumScreenBrightnessSetting) {
            remoteViews.setContentDescription(R.id.btn_brightness, context.getString(R.string.gadget_brightness_template, context.getString(R.string.gadget_brightness_state_full)));
            remoteViews.setImageViewResource(R.id.img_brightness, R.drawable.ic_appwidget_settings_brightness_full_holo);
        } else if (brightness > maximumScreenBrightnessSetting2) {
            remoteViews.setContentDescription(R.id.btn_brightness, context.getString(R.string.gadget_brightness_template, context.getString(R.string.gadget_brightness_state_half)));
            remoteViews.setImageViewResource(R.id.img_brightness, R.drawable.ic_appwidget_settings_brightness_half_holo);
        } else {
            remoteViews.setContentDescription(R.id.btn_brightness, context.getString(R.string.gadget_brightness_template, context.getString(R.string.gadget_brightness_state_off)));
            remoteViews.setImageViewResource(R.id.img_brightness, R.drawable.ic_appwidget_settings_brightness_off_holo);
        }
        if (brightness > maximumScreenBrightnessSetting2) {
            remoteViews.setImageViewResource(R.id.ind_brightness, R.drawable.appwidget_settings_ind_on_r_holo);
        } else {
            remoteViews.setImageViewResource(R.id.ind_brightness, R.drawable.appwidget_settings_ind_off_r_holo);
        }
    }

    private static PendingIntent getLaunchPendingIntent(Context context, int i) {
        Intent intent = new Intent();
        intent.setClass(context, SettingsAppWidgetProvider.class);
        intent.addCategory("android.intent.category.ALTERNATIVE");
        intent.setData(Uri.parse("custom:" + i));
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();
        if ("android.net.wifi.WIFI_STATE_CHANGED".equals(action)) {
            sWifiState.onActualStateChange(context, intent);
        } else if ("android.bluetooth.adapter.action.STATE_CHANGED".equals(action)) {
            sBluetoothState.onActualStateChange(context, intent);
        } else if ("android.location.MODE_CHANGED".equals(action)) {
            sLocationState.onActualStateChange(context, intent);
        } else if (ContentResolver.ACTION_SYNC_CONN_STATUS_CHANGED.equals(action)) {
            sSyncState.onActualStateChange(context, intent);
        } else if (intent.hasCategory("android.intent.category.ALTERNATIVE")) {
            int i = Integer.parseInt(intent.getData().getSchemeSpecificPart());
            if (i == 0) {
                sWifiState.toggleState(context);
            } else if (i == 1) {
                toggleBrightness(context);
            } else if (i == 2) {
                sSyncState.toggleState(context);
            } else if (i == 3) {
                sLocationState.toggleState(context);
            } else if (i == 4) {
                sBluetoothState.toggleState(context);
            }
        } else {
            return;
        }
        updateWidget(context);
    }

    private static int getBrightness(Context context) {
        try {
            return Settings.System.getInt(context.getContentResolver(), "screen_brightness");
        } catch (Exception e) {
            return 0;
        }
    }

    private static boolean getBrightnessMode(Context context) {
        try {
            if (Settings.System.getInt(context.getContentResolver(), "screen_brightness_mode") != 1) {
                return false;
            }
            return true;
        } catch (Exception e) {
            Log.d("SettingsAppWidgetProvider", "getBrightnessMode: " + e);
            return false;
        }
    }

    private void toggleBrightness(Context context) {
        int i;
        int minimumScreenBrightnessSetting;
        try {
            DisplayManager displayManager = (DisplayManager) context.getSystemService(DisplayManager.class);
            PowerManager powerManager = (PowerManager) context.getSystemService(PowerManager.class);
            ContentResolver contentResolver = context.getContentResolver();
            int i2 = Settings.System.getInt(contentResolver, "screen_brightness");
            if (context.getResources().getBoolean(android.R.^attr-private.borderTop)) {
                i = Settings.System.getInt(contentResolver, "screen_brightness_mode");
            } else {
                i = 0;
            }
            if (i == 1) {
                minimumScreenBrightnessSetting = powerManager.getMinimumScreenBrightnessSetting();
                i = 0;
            } else if (i2 < powerManager.getDefaultScreenBrightnessSetting()) {
                minimumScreenBrightnessSetting = powerManager.getDefaultScreenBrightnessSetting();
            } else if (i2 < powerManager.getMaximumScreenBrightnessSetting()) {
                minimumScreenBrightnessSetting = powerManager.getMaximumScreenBrightnessSetting();
            } else {
                minimumScreenBrightnessSetting = powerManager.getMinimumScreenBrightnessSetting();
                i = 1;
            }
            if (context.getResources().getBoolean(android.R.^attr-private.borderTop)) {
                Settings.System.putInt(context.getContentResolver(), "screen_brightness_mode", i);
            } else {
                i = 0;
            }
            if (i == 0) {
                displayManager.setTemporaryBrightness(minimumScreenBrightnessSetting);
                Settings.System.putInt(contentResolver, "screen_brightness", minimumScreenBrightnessSetting);
            }
        } catch (Settings.SettingNotFoundException e) {
            Log.d("SettingsAppWidgetProvider", "toggleBrightness: " + e);
        }
    }

    private static class SettingsObserver extends ContentObserver {
        private Context mContext;

        SettingsObserver(Handler handler, Context context) {
            super(handler);
            this.mContext = context;
        }

        void startObserving() {
            ContentResolver contentResolver = this.mContext.getContentResolver();
            contentResolver.registerContentObserver(Settings.System.getUriFor("screen_brightness"), false, this);
            contentResolver.registerContentObserver(Settings.System.getUriFor("screen_brightness_mode"), false, this);
            contentResolver.registerContentObserver(Settings.System.getUriFor("screen_auto_brightness_adj"), false, this);
        }

        void stopObserving() {
            this.mContext.getContentResolver().unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean z) {
            SettingsAppWidgetProvider.updateWidget(this.mContext);
        }
    }
}
