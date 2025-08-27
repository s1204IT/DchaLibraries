package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.res.Resources;
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

/* loaded from: classes.dex */
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

    /* renamed from: com.android.systemui.statusbar.phone.AutoTileManager$1, reason: invalid class name */
    class AnonymousClass1 extends SecureSetting {
        AnonymousClass1(Context context, Handler handler, String str) {
            super(context, handler, str);
        }

        @Override // com.android.systemui.qs.SecureSetting
        protected void handleValueChanged(int i, boolean z) throws Resources.NotFoundException {
            if (!AutoTileManager.this.mAutoTracker.isAdded("inversion") && i != 0) {
                AutoTileManager.this.mHost.addTile("inversion");
                AutoTileManager.this.mAutoTracker.setTileAdded("inversion");
                AutoTileManager.this.mHandler.post(new Runnable() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$AutoTileManager$1$fkFB83CLnhxsYFtYdorSMjVQp8g
                    @Override // java.lang.Runnable
                    public final void run() {
                        AutoTileManager.this.mColorsSetting.setListening(false);
                    }
                });
            }
        }
    }

    /* renamed from: com.android.systemui.statusbar.phone.AutoTileManager$2, reason: invalid class name */
    class AnonymousClass2 implements ManagedProfileController.Callback {
        AnonymousClass2() {
        }

        @Override // com.android.systemui.statusbar.phone.ManagedProfileController.Callback
        public void onManagedProfileChanged() throws Resources.NotFoundException {
            if (!AutoTileManager.this.mAutoTracker.isAdded("work") && ((ManagedProfileController) Dependency.get(ManagedProfileController.class)).hasActiveProfile()) {
                AutoTileManager.this.mHost.addTile("work");
                AutoTileManager.this.mAutoTracker.setTileAdded("work");
                AutoTileManager.this.mHandler.post(new Runnable() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$AutoTileManager$2$syftIWA2wqUlSOuDU-f8zUuwVzc
                    @Override // java.lang.Runnable
                    public final void run() {
                        ((ManagedProfileController) Dependency.get(ManagedProfileController.class)).removeCallback(AutoTileManager.this.mProfileCallback);
                    }
                });
            }
        }

        @Override // com.android.systemui.statusbar.phone.ManagedProfileController.Callback
        public void onManagedProfileRemoved() {
        }
    }

    /* renamed from: com.android.systemui.statusbar.phone.AutoTileManager$3, reason: invalid class name */
    class AnonymousClass3 implements DataSaverController.Listener {
        AnonymousClass3() {
        }

        @Override // com.android.systemui.statusbar.policy.DataSaverController.Listener
        public void onDataSaverChanged(boolean z) throws Resources.NotFoundException {
            if (!AutoTileManager.this.mAutoTracker.isAdded("saver") && z) {
                AutoTileManager.this.mHost.addTile("saver");
                AutoTileManager.this.mAutoTracker.setTileAdded("saver");
                AutoTileManager.this.mHandler.post(new Runnable() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$AutoTileManager$3$jtlbOv9xqjXTNoW_lFuZ_dYzc1k
                    @Override // java.lang.Runnable
                    public final void run() {
                        ((DataSaverController) Dependency.get(DataSaverController.class)).removeCallback(AutoTileManager.this.mDataSaverListener);
                    }
                });
            }
        }
    }

    /* renamed from: com.android.systemui.statusbar.phone.AutoTileManager$4, reason: invalid class name */
    class AnonymousClass4 implements HotspotController.Callback {
        AnonymousClass4() {
        }

        @Override // com.android.systemui.statusbar.policy.HotspotController.Callback
        public void onHotspotChanged(boolean z, int i) throws Resources.NotFoundException {
            if (!AutoTileManager.this.mAutoTracker.isAdded("hotspot") && z) {
                AutoTileManager.this.mHost.addTile("hotspot");
                AutoTileManager.this.mAutoTracker.setTileAdded("hotspot");
                AutoTileManager.this.mHandler.post(new Runnable() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$AutoTileManager$4$B3sgSxASy9hbK7cekuTaJNclHvY
                    @Override // java.lang.Runnable
                    public final void run() {
                        ((HotspotController) Dependency.get(HotspotController.class)).removeCallback(AutoTileManager.this.mHotspotCallback);
                    }
                });
            }
        }
    }

    /* renamed from: com.android.systemui.statusbar.phone.AutoTileManager$5, reason: invalid class name */
    class AnonymousClass5 implements ColorDisplayController.Callback {
        AnonymousClass5() {
        }

        public void onActivated(boolean z) throws Resources.NotFoundException {
            if (z) {
                addNightTile();
            }
        }

        public void onAutoModeChanged(int i) throws Resources.NotFoundException {
            if (i == 1 || i == 2) {
                addNightTile();
            }
        }

        private void addNightTile() throws Resources.NotFoundException {
            if (AutoTileManager.this.mAutoTracker.isAdded("night")) {
                return;
            }
            AutoTileManager.this.mHost.addTile("night");
            AutoTileManager.this.mAutoTracker.setTileAdded("night");
            AutoTileManager.this.mHandler.post(new Runnable() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$AutoTileManager$5$NT1gu4kQ2W-Iy5c_1PT65axEySA
                @Override // java.lang.Runnable
                public final void run() {
                    ((ColorDisplayController) Dependency.get(ColorDisplayController.class)).setListener((ColorDisplayController.Callback) null);
                }
            });
        }
    }
}
