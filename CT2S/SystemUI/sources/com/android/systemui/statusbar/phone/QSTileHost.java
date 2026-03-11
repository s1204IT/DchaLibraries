package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import com.android.systemui.qs.tiles.AirplaneModeTile;
import com.android.systemui.qs.tiles.BluetoothTile;
import com.android.systemui.qs.tiles.CastTile;
import com.android.systemui.qs.tiles.CellularTile;
import com.android.systemui.qs.tiles.ColorInversionTile;
import com.android.systemui.qs.tiles.FlashlightTile;
import com.android.systemui.qs.tiles.HotspotTile;
import com.android.systemui.qs.tiles.IntentTile;
import com.android.systemui.qs.tiles.LocationTile;
import com.android.systemui.qs.tiles.RotationLockTile;
import com.android.systemui.qs.tiles.WifiTile;
import com.android.systemui.settings.CurrentUserTracker;
import com.android.systemui.statusbar.policy.BluetoothController;
import com.android.systemui.statusbar.policy.CastController;
import com.android.systemui.statusbar.policy.FlashlightController;
import com.android.systemui.statusbar.policy.HotspotController;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import com.android.systemui.statusbar.policy.LocationController;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.RotationLockController;
import com.android.systemui.statusbar.policy.SecurityController;
import com.android.systemui.statusbar.policy.UserSwitcherController;
import com.android.systemui.statusbar.policy.ZenModeController;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class QSTileHost implements QSTile.Host {
    private static final boolean DEBUG = Log.isLoggable("QSTileHost", 3);
    private final BluetoothController mBluetooth;
    private QSTile.Host.Callback mCallback;
    private final CastController mCast;
    private final Context mContext;
    private final FlashlightController mFlashlight;
    private final HotspotController mHotspot;
    private final KeyguardMonitor mKeyguard;
    private final LocationController mLocation;
    private final Looper mLooper;
    private final NetworkController mNetwork;
    private final RotationLockController mRotation;
    private final SecurityController mSecurity;
    private final PhoneStatusBar mStatusBar;
    private final UserSwitcherController mUserSwitcherController;
    private final CurrentUserTracker mUserTracker;
    private final ZenModeController mZen;
    private final LinkedHashMap<String, QSTile<?>> mTiles = new LinkedHashMap<>();
    private final Observer mObserver = new Observer();

    public QSTileHost(Context context, PhoneStatusBar statusBar, BluetoothController bluetooth, LocationController location, RotationLockController rotation, NetworkController network, ZenModeController zen, HotspotController hotspot, CastController cast, FlashlightController flashlight, UserSwitcherController userSwitcher, KeyguardMonitor keyguard, SecurityController security) {
        this.mContext = context;
        this.mStatusBar = statusBar;
        this.mBluetooth = bluetooth;
        this.mLocation = location;
        this.mRotation = rotation;
        this.mNetwork = network;
        this.mZen = zen;
        this.mHotspot = hotspot;
        this.mCast = cast;
        this.mFlashlight = flashlight;
        this.mUserSwitcherController = userSwitcher;
        this.mKeyguard = keyguard;
        this.mSecurity = security;
        HandlerThread ht = new HandlerThread(QSTileHost.class.getSimpleName(), 10);
        ht.start();
        this.mLooper = ht.getLooper();
        this.mUserTracker = new CurrentUserTracker(this.mContext) {
            @Override
            public void onUserSwitched(int newUserId) {
                QSTileHost.this.recreateTiles();
                for (QSTile<?> tile : QSTileHost.this.mTiles.values()) {
                    tile.userSwitch(newUserId);
                }
                QSTileHost.this.mSecurity.onUserSwitched(newUserId);
                QSTileHost.this.mNetwork.onUserSwitched(newUserId);
                QSTileHost.this.mObserver.register();
            }
        };
        recreateTiles();
        this.mUserTracker.startTracking();
        this.mObserver.register();
    }

    public void setCallback(QSTile.Host.Callback callback) {
        this.mCallback = callback;
    }

    public Collection<QSTile<?>> getTiles() {
        return this.mTiles.values();
    }

    @Override
    public void startSettingsActivity(Intent intent) {
        this.mStatusBar.postStartSettingsActivity(intent, 0);
    }

    @Override
    public void warn(String message, Throwable t) {
    }

    @Override
    public void collapsePanels() {
        this.mStatusBar.postAnimateCollapsePanels();
    }

    @Override
    public Looper getLooper() {
        return this.mLooper;
    }

    @Override
    public Context getContext() {
        return this.mContext;
    }

    @Override
    public BluetoothController getBluetoothController() {
        return this.mBluetooth;
    }

    @Override
    public LocationController getLocationController() {
        return this.mLocation;
    }

    @Override
    public RotationLockController getRotationLockController() {
        return this.mRotation;
    }

    @Override
    public NetworkController getNetworkController() {
        return this.mNetwork;
    }

    @Override
    public HotspotController getHotspotController() {
        return this.mHotspot;
    }

    @Override
    public CastController getCastController() {
        return this.mCast;
    }

    @Override
    public FlashlightController getFlashlightController() {
        return this.mFlashlight;
    }

    @Override
    public KeyguardMonitor getKeyguardMonitor() {
        return this.mKeyguard;
    }

    public UserSwitcherController getUserSwitcherController() {
        return this.mUserSwitcherController;
    }

    public SecurityController getSecurityController() {
        return this.mSecurity;
    }

    public void recreateTiles() {
        if (DEBUG) {
            Log.d("QSTileHost", "Recreating tiles");
        }
        List<String> tileSpecs = loadTileSpecs();
        for (Map.Entry<String, QSTile<?>> tile : this.mTiles.entrySet()) {
            if (!tileSpecs.contains(tile.getKey())) {
                if (DEBUG) {
                    Log.d("QSTileHost", "Destroying tile: " + tile.getKey());
                }
                tile.getValue().destroy();
            }
        }
        LinkedHashMap<String, QSTile<?>> newTiles = new LinkedHashMap<>();
        for (String tileSpec : tileSpecs) {
            if (this.mTiles.containsKey(tileSpec)) {
                newTiles.put(tileSpec, this.mTiles.get(tileSpec));
            } else {
                if (DEBUG) {
                    Log.d("QSTileHost", "Creating tile: " + tileSpec);
                }
                try {
                    newTiles.put(tileSpec, createTile(tileSpec));
                } catch (Throwable t) {
                    Log.w("QSTileHost", "Error creating tile for spec: " + tileSpec, t);
                }
            }
        }
        if (!this.mTiles.equals(newTiles)) {
            this.mTiles.clear();
            this.mTiles.putAll(newTiles);
            if (this.mCallback != null) {
                this.mCallback.onTilesChanged();
            }
        }
    }

    private QSTile<?> createTile(String tileSpec) {
        if (tileSpec.equals("wifi")) {
            return new WifiTile(this);
        }
        if (tileSpec.equals("bt")) {
            return new BluetoothTile(this);
        }
        if (tileSpec.equals("inversion")) {
            return new ColorInversionTile(this);
        }
        if (tileSpec.equals("cell")) {
            return new CellularTile(this);
        }
        if (tileSpec.equals("airplane")) {
            return new AirplaneModeTile(this);
        }
        if (tileSpec.equals("rotation")) {
            return new RotationLockTile(this);
        }
        if (tileSpec.equals("flashlight")) {
            return new FlashlightTile(this);
        }
        if (tileSpec.equals("location")) {
            return new LocationTile(this);
        }
        if (tileSpec.equals("cast")) {
            return new CastTile(this);
        }
        if (tileSpec.equals("hotspot")) {
            return new HotspotTile(this);
        }
        if (tileSpec.startsWith("intent(")) {
            return IntentTile.create(this, tileSpec);
        }
        throw new IllegalArgumentException("Bad tile spec: " + tileSpec);
    }

    private List<String> loadTileSpecs() {
        Resources res = this.mContext.getResources();
        String defaultTileList = res.getString(R.string.quick_settings_tiles_default);
        String tileList = Settings.Secure.getStringForUser(this.mContext.getContentResolver(), "sysui_qs_tiles", this.mUserTracker.getCurrentUserId());
        if (tileList == null) {
            tileList = res.getString(R.string.quick_settings_tiles);
            if (DEBUG) {
                Log.d("QSTileHost", "Loaded tile specs from config: " + tileList);
            }
        } else if (DEBUG) {
            Log.d("QSTileHost", "Loaded tile specs from setting: " + tileList);
        }
        ArrayList<String> tiles = new ArrayList<>();
        boolean addedDefault = false;
        String[] arr$ = tileList.split(",");
        for (String str : arr$) {
            String tile = str.trim();
            if (!tile.isEmpty()) {
                if (tile.equals("default")) {
                    if (!addedDefault) {
                        tiles.addAll(Arrays.asList(defaultTileList.split(",")));
                        addedDefault = true;
                    }
                } else {
                    tiles.add(tile);
                }
            }
        }
        return tiles;
    }

    private class Observer extends ContentObserver {
        private boolean mRegistered;

        public Observer() {
            super(new Handler(Looper.getMainLooper()));
        }

        public void register() {
            if (this.mRegistered) {
                QSTileHost.this.mContext.getContentResolver().unregisterContentObserver(this);
            }
            QSTileHost.this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("sysui_qs_tiles"), false, this, QSTileHost.this.mUserTracker.getCurrentUserId());
            this.mRegistered = true;
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            QSTileHost.this.recreateTiles();
        }
    }
}
