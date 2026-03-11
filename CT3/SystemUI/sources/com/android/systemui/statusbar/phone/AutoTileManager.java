package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.os.Handler;
import com.android.systemui.Prefs;
import com.android.systemui.qs.SecureSetting;
import com.android.systemui.statusbar.phone.ManagedProfileController;
import com.android.systemui.statusbar.policy.DataSaverController;
import com.android.systemui.statusbar.policy.HotspotController;
import com.android.systemui.statusbar.policy.NightModeController;

public class AutoTileManager {
    private SecureSetting mColorsSetting;
    private final Context mContext;
    private final Handler mHandler;
    private final QSTileHost mHost;
    private final NightModeController.Listener mNightModeListener = new NightModeController.Listener() {
        @Override
        public void onNightModeChanged() {
            if (!AutoTileManager.this.mHost.getNightModeController().isEnabled()) {
                return;
            }
            AutoTileManager.this.mHost.addTile("night");
            Prefs.putBoolean(AutoTileManager.this.mContext, "QsNightAdded", true);
            AutoTileManager.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    AutoTileManager.this.mHost.getNightModeController().removeListener(AutoTileManager.this.mNightModeListener);
                }
            });
        }
    };
    private final ManagedProfileController.Callback mProfileCallback = new ManagedProfileController.Callback() {
        @Override
        public void onManagedProfileChanged() {
            if (!AutoTileManager.this.mHost.getManagedProfileController().hasActiveProfile()) {
                return;
            }
            AutoTileManager.this.mHost.addTile("work");
            Prefs.putBoolean(AutoTileManager.this.mContext, "QsWorkAdded", true);
            AutoTileManager.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    AutoTileManager.this.mHost.getManagedProfileController().removeCallback(AutoTileManager.this.mProfileCallback);
                }
            });
        }

        @Override
        public void onManagedProfileRemoved() {
        }
    };
    private final DataSaverController.Listener mDataSaverListener = new DataSaverController.Listener() {
        @Override
        public void onDataSaverChanged(boolean isDataSaving) {
            if (!isDataSaving) {
                return;
            }
            AutoTileManager.this.mHost.addTile("saver");
            Prefs.putBoolean(AutoTileManager.this.mContext, "QsDataSaverAdded", true);
            AutoTileManager.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    AutoTileManager.this.mHost.getNetworkController().getDataSaverController().remListener(AutoTileManager.this.mDataSaverListener);
                }
            });
        }
    };
    private final HotspotController.Callback mHotspotCallback = new HotspotController.Callback() {
        @Override
        public void onHotspotChanged(boolean enabled) {
            if (!enabled) {
                return;
            }
            AutoTileManager.this.mHost.addTile("hotspot");
            Prefs.putBoolean(AutoTileManager.this.mContext, "QsHotspotAdded", true);
            AutoTileManager.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    AutoTileManager.this.mHost.getHotspotController().removeCallback(AutoTileManager.this.mHotspotCallback);
                }
            });
        }
    };

    public AutoTileManager(Context context, QSTileHost host) {
        this.mContext = context;
        this.mHost = host;
        this.mHandler = new Handler(this.mHost.getLooper());
        if (!Prefs.getBoolean(context, "QsHotspotAdded", false)) {
            host.getHotspotController().addCallback(this.mHotspotCallback);
        }
        if (!Prefs.getBoolean(context, "QsDataSaverAdded", false)) {
            host.getNetworkController().getDataSaverController().addListener(this.mDataSaverListener);
        }
        if (!Prefs.getBoolean(context, "QsInvertColorsAdded", false)) {
            this.mColorsSetting = new SecureSetting(this.mContext, this.mHandler, "accessibility_display_inversion_enabled") {
                @Override
                protected void handleValueChanged(int value, boolean observedChange) {
                    if (value == 0) {
                        return;
                    }
                    AutoTileManager.this.mHost.addTile("inversion");
                    Prefs.putBoolean(AutoTileManager.this.mContext, "QsInvertColorsAdded", true);
                    AutoTileManager.this.mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            AutoTileManager.this.mColorsSetting.setListening(false);
                        }
                    });
                }
            };
            this.mColorsSetting.setListening(true);
        }
        if (!Prefs.getBoolean(context, "QsWorkAdded", false)) {
            host.getManagedProfileController().addCallback(this.mProfileCallback);
        }
        if (Prefs.getBoolean(context, "QsNightAdded", false)) {
            return;
        }
        host.getNightModeController().addListener(this.mNightModeListener);
    }

    public void destroy() {
    }
}
