package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.ColorDisplayController;
import com.android.systemui.Dependency;
import com.android.systemui.qs.AutoAddTracker;
import com.android.systemui.qs.QSTileHost;
import com.android.systemui.qs.SecureSetting;
import com.android.systemui.statusbar.phone.ManagedProfileController;
import com.android.systemui.statusbar.policy.DataSaverController;
import com.android.systemui.statusbar.policy.HotspotController;

public class AutoTileManager {
    private final AutoAddTracker mAutoTracker;

    @VisibleForTesting
    final ColorDisplayController.Callback mColorDisplayCallback;
    private SecureSetting mColorsSetting;
    private final Context mContext;
    private final DataSaverController.Listener mDataSaverListener;
    private final Handler mHandler;
    private final QSTileHost mHost;
    private final HotspotController.Callback mHotspotCallback;
    private final ManagedProfileController.Callback mProfileCallback;

    public AutoTileManager(Context context, QSTileHost qSTileHost) {
        this(context, new AutoAddTracker(context), qSTileHost, new Handler((Looper) Dependency.get(Dependency.BG_LOOPER)));
    }

    @VisibleForTesting
    AutoTileManager(Context context, AutoAddTracker autoAddTracker, QSTileHost qSTileHost, Handler handler) {
        this.mProfileCallback = new AnonymousClass2();
        this.mDataSaverListener = new AnonymousClass3();
        this.mHotspotCallback = new AnonymousClass4();
        this.mColorDisplayCallback = new AnonymousClass5();
        this.mAutoTracker = autoAddTracker;
        this.mContext = context;
        this.mHost = qSTileHost;
        this.mHandler = handler;
        if (!this.mAutoTracker.isAdded("hotspot")) {
            ((HotspotController) Dependency.get(HotspotController.class)).addCallback(this.mHotspotCallback);
        }
        if (!this.mAutoTracker.isAdded("saver")) {
            ((DataSaverController) Dependency.get(DataSaverController.class)).addCallback(this.mDataSaverListener);
        }
        if (!this.mAutoTracker.isAdded("inversion")) {
            this.mColorsSetting = new AnonymousClass1(this.mContext, this.mHandler, "accessibility_display_inversion_enabled");
            this.mColorsSetting.setListening(true);
        }
        if (!this.mAutoTracker.isAdded("work")) {
            ((ManagedProfileController) Dependency.get(ManagedProfileController.class)).addCallback(this.mProfileCallback);
        }
        if (!this.mAutoTracker.isAdded("night") && ColorDisplayController.isAvailable(this.mContext)) {
            ((ColorDisplayController) Dependency.get(ColorDisplayController.class)).setListener(this.mColorDisplayCallback);
        }
    }

    class AnonymousClass1 extends SecureSetting {
        AnonymousClass1(Context context, Handler handler, String str) {
            super(context, handler, str);
        }

        @Override
        protected void handleValueChanged(int i, boolean z) {
            if (!AutoTileManager.this.mAutoTracker.isAdded("inversion") && i != 0) {
                AutoTileManager.this.mHost.addTile("inversion");
                AutoTileManager.this.mAutoTracker.setTileAdded("inversion");
                AutoTileManager.this.mHandler.post(new Runnable() {
                    @Override
                    public final void run() {
                        AutoTileManager.this.mColorsSetting.setListening(false);
                    }
                });
            }
        }
    }

    class AnonymousClass2 implements ManagedProfileController.Callback {
        AnonymousClass2() {
        }

        @Override
        public void onManagedProfileChanged() {
            if (!AutoTileManager.this.mAutoTracker.isAdded("work") && ((ManagedProfileController) Dependency.get(ManagedProfileController.class)).hasActiveProfile()) {
                AutoTileManager.this.mHost.addTile("work");
                AutoTileManager.this.mAutoTracker.setTileAdded("work");
                AutoTileManager.this.mHandler.post(new Runnable() {
                    @Override
                    public final void run() {
                        ((ManagedProfileController) Dependency.get(ManagedProfileController.class)).removeCallback(AutoTileManager.this.mProfileCallback);
                    }
                });
            }
        }

        @Override
        public void onManagedProfileRemoved() {
        }
    }

    class AnonymousClass3 implements DataSaverController.Listener {
        AnonymousClass3() {
        }

        @Override
        public void onDataSaverChanged(boolean z) {
            if (!AutoTileManager.this.mAutoTracker.isAdded("saver") && z) {
                AutoTileManager.this.mHost.addTile("saver");
                AutoTileManager.this.mAutoTracker.setTileAdded("saver");
                AutoTileManager.this.mHandler.post(new Runnable() {
                    @Override
                    public final void run() {
                        ((DataSaverController) Dependency.get(DataSaverController.class)).removeCallback(AutoTileManager.this.mDataSaverListener);
                    }
                });
            }
        }
    }

    class AnonymousClass4 implements HotspotController.Callback {
        AnonymousClass4() {
        }

        @Override
        public void onHotspotChanged(boolean z, int i) {
            if (!AutoTileManager.this.mAutoTracker.isAdded("hotspot") && z) {
                AutoTileManager.this.mHost.addTile("hotspot");
                AutoTileManager.this.mAutoTracker.setTileAdded("hotspot");
                AutoTileManager.this.mHandler.post(new Runnable() {
                    @Override
                    public final void run() {
                        ((HotspotController) Dependency.get(HotspotController.class)).removeCallback(AutoTileManager.this.mHotspotCallback);
                    }
                });
            }
        }
    }

    class AnonymousClass5 implements ColorDisplayController.Callback {
        AnonymousClass5() {
        }

        public void onActivated(boolean z) {
            if (z) {
                addNightTile();
            }
        }

        public void onAutoModeChanged(int i) {
            if (i == 1 || i == 2) {
                addNightTile();
            }
        }

        private void addNightTile() {
            if (AutoTileManager.this.mAutoTracker.isAdded("night")) {
                return;
            }
            AutoTileManager.this.mHost.addTile("night");
            AutoTileManager.this.mAutoTracker.setTileAdded("night");
            AutoTileManager.this.mHandler.post(new Runnable() {
                @Override
                public final void run() {
                    ((ColorDisplayController) Dependency.get(ColorDisplayController.class)).setListener((ColorDisplayController.Callback) null);
                }
            });
        }
    }
}
