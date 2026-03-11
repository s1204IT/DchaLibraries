package com.android.systemui.statusbar.phone;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import com.android.systemui.qs.external.CustomTile;
import com.android.systemui.qs.external.TileLifecycleManager;
import com.android.systemui.qs.external.TileServices;
import com.android.systemui.qs.tiles.AirplaneModeTile;
import com.android.systemui.qs.tiles.BatteryTile;
import com.android.systemui.qs.tiles.BluetoothTile;
import com.android.systemui.qs.tiles.CastTile;
import com.android.systemui.qs.tiles.CellularTile;
import com.android.systemui.qs.tiles.ColorInversionTile;
import com.android.systemui.qs.tiles.DataSaverTile;
import com.android.systemui.qs.tiles.DndTile;
import com.android.systemui.qs.tiles.FlashlightTile;
import com.android.systemui.qs.tiles.HotspotTile;
import com.android.systemui.qs.tiles.IntentTile;
import com.android.systemui.qs.tiles.LocationTile;
import com.android.systemui.qs.tiles.RotationLockTile;
import com.android.systemui.qs.tiles.UserTile;
import com.android.systemui.qs.tiles.WifiTile;
import com.android.systemui.qs.tiles.WorkModeTile;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.BluetoothController;
import com.android.systemui.statusbar.policy.CastController;
import com.android.systemui.statusbar.policy.FlashlightController;
import com.android.systemui.statusbar.policy.HotspotController;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import com.android.systemui.statusbar.policy.LocationController;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.NextAlarmController;
import com.android.systemui.statusbar.policy.NightModeController;
import com.android.systemui.statusbar.policy.RotationLockController;
import com.android.systemui.statusbar.policy.SecurityController;
import com.android.systemui.statusbar.policy.UserInfoController;
import com.android.systemui.statusbar.policy.UserSwitcherController;
import com.android.systemui.statusbar.policy.ZenModeController;
import com.android.systemui.tuner.NightModeTile;
import com.android.systemui.tuner.TunerService;
import com.mediatek.systemui.PluginManager;
import com.mediatek.systemui.ext.IQuickSettingsPlugin;
import com.mediatek.systemui.qs.tiles.HotKnotTile;
import com.mediatek.systemui.qs.tiles.ext.ApnSettingsTile;
import com.mediatek.systemui.qs.tiles.ext.DualSimSettingsTile;
import com.mediatek.systemui.qs.tiles.ext.MobileDataTile;
import com.mediatek.systemui.qs.tiles.ext.SimDataConnectionTile;
import com.mediatek.systemui.statusbar.policy.HotKnotController;
import com.mediatek.systemui.statusbar.util.SIMHelper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class QSTileHost implements QSTile.Host, TunerService.Tunable {
    private static final boolean DEBUG = Log.isLoggable("QSTileHost", 3);
    private final AutoTileManager mAutoTiles;
    private final BatteryController mBattery;
    private final BluetoothController mBluetooth;
    private final CastController mCast;
    private final Context mContext;
    private int mCurrentUser;
    private final FlashlightController mFlashlight;
    private View mHeader;
    private final HotKnotController mHotKnot;
    private final HotspotController mHotspot;
    private final StatusBarIconController mIconController;
    private final KeyguardMonitor mKeyguard;
    private final LocationController mLocation;
    private final Looper mLooper;
    private final NetworkController mNetwork;
    private final NextAlarmController mNextAlarmController;
    private final NightModeController mNightModeController;
    private final RotationLockController mRotation;
    private final SecurityController mSecurity;
    private final TileServices mServices;
    private final PhoneStatusBar mStatusBar;
    private final UserInfoController mUserInfoController;
    private final UserSwitcherController mUserSwitcherController;
    private final ZenModeController mZen;
    private final LinkedHashMap<String, QSTile<?>> mTiles = new LinkedHashMap<>();
    protected final ArrayList<String> mTileSpecs = new ArrayList<>();
    private final List<QSTile.Host.Callback> mCallbacks = new ArrayList();
    private final ManagedProfileController mProfileController = new ManagedProfileController(this);

    public QSTileHost(Context context, PhoneStatusBar statusBar, BluetoothController bluetooth, LocationController location, RotationLockController rotation, NetworkController network, ZenModeController zen, HotspotController hotspot, CastController cast, FlashlightController flashlight, UserSwitcherController userSwitcher, UserInfoController userInfo, KeyguardMonitor keyguard, SecurityController security, BatteryController battery, StatusBarIconController iconController, NextAlarmController nextAlarmController, HotKnotController hotknot) {
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
        this.mUserInfoController = userInfo;
        this.mKeyguard = keyguard;
        this.mSecurity = security;
        this.mBattery = battery;
        this.mIconController = iconController;
        this.mNextAlarmController = nextAlarmController;
        this.mNightModeController = new NightModeController(this.mContext, true);
        this.mHotKnot = hotknot;
        HandlerThread ht = new HandlerThread(QSTileHost.class.getSimpleName(), 10);
        ht.start();
        this.mLooper = ht.getLooper();
        this.mServices = new TileServices(this, this.mLooper);
        TunerService.get(this.mContext).addTunable(this, "sysui_qs_tiles");
        this.mAutoTiles = new AutoTileManager(context, this);
    }

    public NextAlarmController getNextAlarmController() {
        return this.mNextAlarmController;
    }

    public void setHeaderView(View view) {
        this.mHeader = view;
    }

    public PhoneStatusBar getPhoneStatusBar() {
        return this.mStatusBar;
    }

    public void destroy() {
        this.mAutoTiles.destroy();
        TunerService.get(this.mContext).removeTunable(this);
    }

    public void addCallback(QSTile.Host.Callback callback) {
        this.mCallbacks.add(callback);
    }

    public void removeCallback(QSTile.Host.Callback callback) {
        this.mCallbacks.remove(callback);
    }

    public Collection<QSTile<?>> getTiles() {
        return this.mTiles.values();
    }

    @Override
    public void startActivityDismissingKeyguard(Intent intent) {
        if (intent != null) {
            this.mStatusBar.postStartActivityDismissingKeyguard(intent, 0);
        }
    }

    @Override
    public void startActivityDismissingKeyguard(PendingIntent intent) {
        if (intent != null) {
            this.mStatusBar.postStartActivityDismissingKeyguard(intent);
        }
    }

    @Override
    public void startRunnableDismissingKeyguard(Runnable runnable) {
        this.mStatusBar.postQSRunnableDismissingKeyguard(runnable);
    }

    @Override
    public void warn(String message, Throwable t) {
    }

    public void animateToggleQSExpansion() {
        this.mHeader.callOnClick();
    }

    @Override
    public void collapsePanels() {
        this.mStatusBar.postAnimateCollapsePanels();
    }

    @Override
    public void openPanels() {
        this.mStatusBar.postAnimateOpenPanels();
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
    public ZenModeController getZenModeController() {
        return this.mZen;
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

    @Override
    public UserSwitcherController getUserSwitcherController() {
        return this.mUserSwitcherController;
    }

    @Override
    public UserInfoController getUserInfoController() {
        return this.mUserInfoController;
    }

    @Override
    public BatteryController getBatteryController() {
        return this.mBattery;
    }

    public SecurityController getSecurityController() {
        return this.mSecurity;
    }

    @Override
    public TileServices getTileServices() {
        return this.mServices;
    }

    public StatusBarIconController getIconController() {
        return this.mIconController;
    }

    @Override
    public NightModeController getNightModeController() {
        return this.mNightModeController;
    }

    @Override
    public ManagedProfileController getManagedProfileController() {
        return this.mProfileController;
    }

    @Override
    public HotKnotController getHotKnotController() {
        return this.mHotKnot;
    }

    @Override
    public void onTuningChanged(String key, String newValue) {
        if (!"sysui_qs_tiles".equals(key)) {
            return;
        }
        if (DEBUG) {
            Log.d("QSTileHost", "Recreating tiles");
        }
        List<String> tileSpecs = loadTileSpecs(this.mContext, newValue);
        int currentUser = ActivityManager.getCurrentUser();
        if (tileSpecs.equals(this.mTileSpecs) && currentUser == this.mCurrentUser) {
            return;
        }
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
            QSTile<?> tile2 = this.mTiles.get(tileSpec);
            if (tile2 != null && (!(tile2 instanceof CustomTile) || ((CustomTile) tile2).getUser() == currentUser)) {
                if (DEBUG) {
                    Log.d("QSTileHost", "Adding " + tile2);
                }
                tile2.removeCallbacks();
                newTiles.put(tileSpec, tile2);
            } else {
                if (DEBUG) {
                    Log.d("QSTileHost", "Creating tile: " + tileSpec);
                }
                try {
                    QSTile<?> tile3 = createTile(tileSpec);
                    if (tile3 != null && tile3.isAvailable()) {
                        tile3.setTileSpec(tileSpec);
                        newTiles.put(tileSpec, tile3);
                    }
                } catch (Throwable t) {
                    Log.w("QSTileHost", "Error creating tile for spec: " + tileSpec, t);
                }
            }
        }
        this.mCurrentUser = currentUser;
        this.mTileSpecs.clear();
        this.mTileSpecs.addAll(tileSpecs);
        this.mTiles.clear();
        this.mTiles.putAll(newTiles);
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            this.mCallbacks.get(i).onTilesChanged();
        }
    }

    @Override
    public void removeTile(String tileSpec) {
        ArrayList<String> specs = new ArrayList<>(this.mTileSpecs);
        specs.remove(tileSpec);
        Settings.Secure.putStringForUser(this.mContext.getContentResolver(), "sysui_qs_tiles", TextUtils.join(",", specs), ActivityManager.getCurrentUser());
    }

    public void addTile(String spec) {
        String setting = Settings.Secure.getStringForUser(this.mContext.getContentResolver(), "sysui_qs_tiles", ActivityManager.getCurrentUser());
        List<String> tileSpecs = loadTileSpecs(this.mContext, setting);
        if (tileSpecs.contains(spec)) {
            return;
        }
        tileSpecs.add(spec);
        Settings.Secure.putStringForUser(this.mContext.getContentResolver(), "sysui_qs_tiles", TextUtils.join(",", tileSpecs), ActivityManager.getCurrentUser());
    }

    public void addTile(ComponentName tile) {
        List<String> newSpecs = new ArrayList<>(this.mTileSpecs);
        newSpecs.add(0, CustomTile.toSpec(tile));
        changeTiles(this.mTileSpecs, newSpecs);
    }

    public void removeTile(ComponentName tile) {
        List<String> newSpecs = new ArrayList<>(this.mTileSpecs);
        newSpecs.remove(CustomTile.toSpec(tile));
        changeTiles(this.mTileSpecs, newSpecs);
    }

    public void changeTiles(List<String> previousTiles, List<String> newTiles) {
        int NP = previousTiles.size();
        int NA = newTiles.size();
        for (int i = 0; i < NP; i++) {
            String tileSpec = previousTiles.get(i);
            if (tileSpec.startsWith("custom(") && !newTiles.contains(tileSpec)) {
                ComponentName component = CustomTile.getComponentFromSpec(tileSpec);
                Intent intent = new Intent().setComponent(component);
                TileLifecycleManager lifecycleManager = new TileLifecycleManager(new Handler(), this.mContext, this.mServices, new Tile(component), intent, new UserHandle(ActivityManager.getCurrentUser()));
                lifecycleManager.onStopListening();
                lifecycleManager.onTileRemoved();
                lifecycleManager.flushMessagesAndUnbind();
            }
        }
        for (int i2 = 0; i2 < NA; i2++) {
            String tileSpec2 = newTiles.get(i2);
            if (tileSpec2.startsWith("custom(") && !previousTiles.contains(tileSpec2)) {
                ComponentName component2 = CustomTile.getComponentFromSpec(tileSpec2);
                Intent intent2 = new Intent().setComponent(component2);
                TileLifecycleManager lifecycleManager2 = new TileLifecycleManager(new Handler(), this.mContext, this.mServices, new Tile(component2), intent2, new UserHandle(ActivityManager.getCurrentUser()));
                lifecycleManager2.onTileAdded();
                lifecycleManager2.flushMessagesAndUnbind();
            }
        }
        if (DEBUG) {
            Log.d("QSTileHost", "saveCurrentTiles " + newTiles);
        }
        Settings.Secure.putStringForUser(getContext().getContentResolver(), "sysui_qs_tiles", TextUtils.join(",", newTiles), ActivityManager.getCurrentUser());
    }

    public QSTile<?> createTile(String tileSpec) {
        IQuickSettingsPlugin quickSettingsPlugin = PluginManager.getQuickSettingsPlugin(this.mContext);
        if (tileSpec.equals("wifi")) {
            return new WifiTile(this);
        }
        if (tileSpec.equals("bt")) {
            return new BluetoothTile(this);
        }
        if (tileSpec.equals("cell")) {
            return new CellularTile(this);
        }
        if (tileSpec.equals("dnd")) {
            return new DndTile(this);
        }
        if (tileSpec.equals("inversion")) {
            return new ColorInversionTile(this);
        }
        if (tileSpec.equals("airplane")) {
            return new AirplaneModeTile(this);
        }
        if (tileSpec.equals("work")) {
            return new WorkModeTile(this);
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
        if (tileSpec.equals("user")) {
            return new UserTile(this);
        }
        if (tileSpec.equals("battery")) {
            return new BatteryTile(this);
        }
        if (tileSpec.equals("saver")) {
            return new DataSaverTile(this);
        }
        if (tileSpec.equals("night")) {
            return new NightModeTile(this);
        }
        if (tileSpec.equals("hotknot") && SIMHelper.isMtkHotKnotSupport()) {
            return new HotKnotTile(this);
        }
        if (tileSpec.equals("dataconnection") && !SIMHelper.isWifiOnlyDevice()) {
            return new MobileDataTile(this);
        }
        if (tileSpec.equals("simdataconnection") && !SIMHelper.isWifiOnlyDevice() && quickSettingsPlugin.customizeAddQSTile(new SimDataConnectionTile(this)) != null) {
            return (SimDataConnectionTile) quickSettingsPlugin.customizeAddQSTile(new SimDataConnectionTile(this));
        }
        if (tileSpec.equals("dulsimsettings") && !SIMHelper.isWifiOnlyDevice() && quickSettingsPlugin.customizeAddQSTile(new DualSimSettingsTile(this)) != null) {
            return (DualSimSettingsTile) quickSettingsPlugin.customizeAddQSTile(new DualSimSettingsTile(this));
        }
        if (tileSpec.equals("apnsettings") && !SIMHelper.isWifiOnlyDevice() && quickSettingsPlugin.customizeAddQSTile(new ApnSettingsTile(this)) != null) {
            return (ApnSettingsTile) quickSettingsPlugin.customizeAddQSTile(new ApnSettingsTile(this));
        }
        if (quickSettingsPlugin.doOperatorSupportTile(tileSpec)) {
            return (QSTile) quickSettingsPlugin.createTile(this, tileSpec);
        }
        if (tileSpec.startsWith("intent(")) {
            return IntentTile.create(this, tileSpec);
        }
        if (tileSpec.startsWith("custom(")) {
            return CustomTile.create(this, tileSpec);
        }
        Log.w("QSTileHost", "Bad tile spec: " + tileSpec);
        return null;
    }

    protected List<String> loadTileSpecs(Context context, String tileList) {
        Resources res = context.getResources();
        String defaultTileList = res.getString(R.string.quick_settings_tiles_default);
        IQuickSettingsPlugin quickSettingsPlugin = PluginManager.getQuickSettingsPlugin(this.mContext);
        String defaultTileList2 = quickSettingsPlugin.customizeQuickSettingsTileOrder(quickSettingsPlugin.addOpTileSpecs(defaultTileList));
        Log.d("QSTileHost", "loadTileSpecs() default tile list: " + defaultTileList2);
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
        for (String str : tileList.split(",")) {
            String tile = str.trim();
            if (!tile.isEmpty()) {
                if (tile.equals("default")) {
                    if (!addedDefault) {
                        tiles.addAll(Arrays.asList(defaultTileList2.split(",")));
                        addedDefault = true;
                    }
                } else {
                    tiles.add(tile);
                }
            }
        }
        return tiles;
    }
}
