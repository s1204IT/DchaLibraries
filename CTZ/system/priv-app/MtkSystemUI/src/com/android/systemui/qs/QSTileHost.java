package com.android.systemui.qs;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.text.TextUtils;
import android.util.Log;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.plugins.PluginListener;
import com.android.systemui.plugins.PluginManager;
import com.android.systemui.plugins.qs.QSFactory;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.plugins.qs.QSTileView;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.external.CustomTile;
import com.android.systemui.qs.external.TileLifecycleManager;
import com.android.systemui.qs.external.TileServices;
import com.android.systemui.qs.tileimpl.QSFactoryImpl;
import com.android.systemui.statusbar.phone.AutoTileManager;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.phone.StatusBarIconController;
import com.android.systemui.tuner.TunerService;
import com.mediatek.systemui.ext.IQuickSettingsPlugin;
import com.mediatek.systemui.ext.OpSystemUICustomizationFactoryBase;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

/* loaded from: classes.dex */
public class QSTileHost implements PluginListener<QSFactory>, QSHost, TunerService.Tunable {
    private static final boolean DEBUG = Log.isLoggable("QSTileHost", 3);
    private final AutoTileManager mAutoTiles;
    private final Context mContext;
    private int mCurrentUser;
    private final StatusBarIconController mIconController;
    private IQuickSettingsPlugin mQuickSettingsExt;
    private final StatusBar mStatusBar;
    private final LinkedHashMap<String, QSTile> mTiles = new LinkedHashMap<>();
    protected final ArrayList<String> mTileSpecs = new ArrayList<>();
    private final List<QSHost.Callback> mCallbacks = new ArrayList();
    private final ArrayList<QSFactory> mQsFactories = new ArrayList<>();
    private final TileServices mServices = new TileServices(this, (Looper) Dependency.get(Dependency.BG_LOOPER));

    public QSTileHost(Context context, StatusBar statusBar, StatusBarIconController statusBarIconController) {
        this.mQuickSettingsExt = null;
        this.mIconController = statusBarIconController;
        this.mContext = context;
        this.mStatusBar = statusBar;
        this.mQuickSettingsExt = OpSystemUICustomizationFactoryBase.getOpFactory(context).makeQuickSettings(context);
        this.mQsFactories.add(new QSFactoryImpl(this));
        ((PluginManager) Dependency.get(PluginManager.class)).addPluginListener((PluginListener) this, QSFactory.class, true);
        ((TunerService) Dependency.get(TunerService.class)).addTunable(this, "sysui_qs_tiles");
        this.mAutoTiles = new AutoTileManager(context, this);
    }

    public StatusBarIconController getIconController() {
        return this.mIconController;
    }

    /* JADX DEBUG: Method merged with bridge method: onPluginConnected(Lcom/android/systemui/plugins/Plugin;Landroid/content/Context;)V */
    @Override // com.android.systemui.plugins.PluginListener
    public void onPluginConnected(QSFactory qSFactory, Context context) throws Resources.NotFoundException {
        this.mQsFactories.add(0, qSFactory);
        String value = ((TunerService) Dependency.get(TunerService.class)).getValue("sysui_qs_tiles");
        onTuningChanged("sysui_qs_tiles", "");
        onTuningChanged("sysui_qs_tiles", value);
    }

    /* JADX DEBUG: Method merged with bridge method: onPluginDisconnected(Lcom/android/systemui/plugins/Plugin;)V */
    @Override // com.android.systemui.plugins.PluginListener
    public void onPluginDisconnected(QSFactory qSFactory) throws Resources.NotFoundException {
        this.mQsFactories.remove(qSFactory);
        String value = ((TunerService) Dependency.get(TunerService.class)).getValue("sysui_qs_tiles");
        onTuningChanged("sysui_qs_tiles", "");
        onTuningChanged("sysui_qs_tiles", value);
    }

    public void addCallback(QSHost.Callback callback) {
        this.mCallbacks.add(callback);
    }

    public void removeCallback(QSHost.Callback callback) {
        this.mCallbacks.remove(callback);
    }

    public Collection<QSTile> getTiles() {
        return this.mTiles.values();
    }

    @Override // com.android.systemui.qs.QSHost
    public void warn(String str, Throwable th) {
    }

    @Override // com.android.systemui.qs.QSHost
    public void collapsePanels() {
        this.mStatusBar.postAnimateCollapsePanels();
    }

    public void forceCollapsePanels() {
        this.mStatusBar.postAnimateForceCollapsePanels();
    }

    @Override // com.android.systemui.qs.QSHost
    public void openPanels() {
        this.mStatusBar.postAnimateOpenPanels();
    }

    @Override // com.android.systemui.qs.QSHost
    public Context getContext() {
        return this.mContext;
    }

    @Override // com.android.systemui.qs.QSHost
    public TileServices getTileServices() {
        return this.mServices;
    }

    @Override // com.android.systemui.qs.QSHost
    public int indexOf(String str) {
        return this.mTileSpecs.indexOf(str);
    }

    @Override // com.android.systemui.tuner.TunerService.Tunable
    public void onTuningChanged(String str, String str2) throws Resources.NotFoundException {
        boolean z;
        if (!"sysui_qs_tiles".equals(str)) {
            return;
        }
        if (DEBUG) {
            Log.d("QSTileHost", "Recreating tiles");
        }
        if (str2 == null && UserManager.isDeviceInDemoMode(this.mContext)) {
            str2 = this.mContext.getResources().getString(R.string.quick_settings_tiles_retail_mode);
        }
        final List<String> listLoadTileSpecs = loadTileSpecs(this.mContext, str2);
        int currentUser = ActivityManager.getCurrentUser();
        if (listLoadTileSpecs.equals(this.mTileSpecs) && currentUser == this.mCurrentUser) {
            return;
        }
        this.mTiles.entrySet().stream().filter(new Predicate() { // from class: com.android.systemui.qs.-$$Lambda$QSTileHost$yU-sQWBVZ_STOuSxrKNWIDPkScE
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return QSTileHost.lambda$onTuningChanged$1(listLoadTileSpecs, (Map.Entry) obj);
            }
        }).forEach(new Consumer() { // from class: com.android.systemui.qs.-$$Lambda$QSTileHost$3Sm37Dy1JWpLe_hn88f-6UG0Q-M
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                QSTileHost.lambda$onTuningChanged$2((Map.Entry) obj);
            }
        });
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        for (String str3 : listLoadTileSpecs) {
            QSTile qSTile = this.mTiles.get(str3);
            if (qSTile != null && (!((z = qSTile instanceof CustomTile)) || ((CustomTile) qSTile).getUser() == currentUser)) {
                if (qSTile.isAvailable()) {
                    if (DEBUG) {
                        Log.d("QSTileHost", "Adding " + qSTile);
                    }
                    qSTile.removeCallbacks();
                    if (!z && this.mCurrentUser != currentUser) {
                        qSTile.userSwitch(currentUser);
                    }
                    linkedHashMap.put(str3, qSTile);
                } else {
                    qSTile.destroy();
                }
            } else {
                if (DEBUG) {
                    Log.d("QSTileHost", "Creating tile: " + str3);
                }
                try {
                    QSTile qSTileCreateTile = createTile(str3);
                    if (qSTileCreateTile != null) {
                        if (qSTileCreateTile.isAvailable()) {
                            qSTileCreateTile.setTileSpec(str3);
                            linkedHashMap.put(str3, qSTileCreateTile);
                        } else {
                            qSTileCreateTile.destroy();
                        }
                    }
                } catch (Throwable th) {
                    Log.w("QSTileHost", "Error creating tile for spec: " + str3, th);
                }
            }
        }
        this.mCurrentUser = currentUser;
        this.mTileSpecs.clear();
        this.mTileSpecs.addAll(listLoadTileSpecs);
        this.mTiles.clear();
        this.mTiles.putAll(linkedHashMap);
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            this.mCallbacks.get(i).onTilesChanged();
        }
    }

    static /* synthetic */ boolean lambda$onTuningChanged$1(List list, Map.Entry entry) {
        return !list.contains(entry.getKey());
    }

    static /* synthetic */ void lambda$onTuningChanged$2(Map.Entry entry) {
        if (DEBUG) {
            Log.d("QSTileHost", "Destroying tile: " + ((String) entry.getKey()));
        }
        ((QSTile) entry.getValue()).destroy();
    }

    @Override // com.android.systemui.qs.QSHost
    public void removeTile(String str) {
        ArrayList arrayList = new ArrayList(this.mTileSpecs);
        arrayList.remove(str);
        Settings.Secure.putStringForUser(this.mContext.getContentResolver(), "sysui_qs_tiles", TextUtils.join(",", arrayList), ActivityManager.getCurrentUser());
    }

    public void addTile(String str) throws Resources.NotFoundException {
        List<String> listLoadTileSpecs = loadTileSpecs(this.mContext, Settings.Secure.getStringForUser(this.mContext.getContentResolver(), "sysui_qs_tiles", ActivityManager.getCurrentUser()));
        if (listLoadTileSpecs.contains(str)) {
            return;
        }
        listLoadTileSpecs.add(str);
        Settings.Secure.putStringForUser(this.mContext.getContentResolver(), "sysui_qs_tiles", TextUtils.join(",", listLoadTileSpecs), ActivityManager.getCurrentUser());
    }

    public void addTile(ComponentName componentName) {
        ArrayList arrayList = new ArrayList(this.mTileSpecs);
        arrayList.add(0, CustomTile.toSpec(componentName));
        changeTiles(this.mTileSpecs, arrayList);
    }

    public void removeTile(ComponentName componentName) {
        ArrayList arrayList = new ArrayList(this.mTileSpecs);
        arrayList.remove(CustomTile.toSpec(componentName));
        changeTiles(this.mTileSpecs, arrayList);
    }

    public void changeTiles(List<String> list, List<String> list2) {
        int size = list.size();
        list2.size();
        for (int i = 0; i < size; i++) {
            String str = list.get(i);
            if (str.startsWith("custom(") && !list2.contains(str)) {
                ComponentName componentFromSpec = CustomTile.getComponentFromSpec(str);
                TileLifecycleManager tileLifecycleManager = new TileLifecycleManager(new Handler(), this.mContext, this.mServices, new Tile(), new Intent().setComponent(componentFromSpec), new UserHandle(ActivityManager.getCurrentUser()));
                tileLifecycleManager.onStopListening();
                tileLifecycleManager.onTileRemoved();
                TileLifecycleManager.setTileAdded(this.mContext, componentFromSpec, false);
                tileLifecycleManager.flushMessagesAndUnbind();
            }
        }
        if (DEBUG) {
            Log.d("QSTileHost", "saveCurrentTiles " + list2);
        }
        Settings.Secure.putStringForUser(getContext().getContentResolver(), "sysui_qs_tiles", TextUtils.join(",", list2), ActivityManager.getCurrentUser());
    }

    public QSTile createTile(String str) {
        for (int i = 0; i < this.mQsFactories.size(); i++) {
            QSTile qSTileCreateTile = this.mQsFactories.get(i).createTile(str);
            if (qSTileCreateTile != null) {
                return qSTileCreateTile;
            }
        }
        if (this.mQuickSettingsExt != null && this.mQuickSettingsExt.doOperatorSupportTile(str)) {
            return (QSTile) this.mQuickSettingsExt.createTile(this, str);
        }
        return null;
    }

    public QSTileView createTileView(QSTile qSTile, boolean z) {
        for (int i = 0; i < this.mQsFactories.size(); i++) {
            QSTileView qSTileViewCreateTileView = this.mQsFactories.get(i).createTileView(qSTile, z);
            if (qSTileViewCreateTileView != null) {
                return qSTileViewCreateTileView;
            }
        }
        throw new RuntimeException("Default factory didn't create view for " + qSTile.getTileSpec());
    }

    protected List<String> loadTileSpecs(Context context, String str) throws Resources.NotFoundException {
        Resources resources = context.getResources();
        String string = resources.getString(R.string.quick_settings_tiles_default);
        if (this.mQuickSettingsExt != null) {
            string = this.mQuickSettingsExt.customizeQuickSettingsTileOrder(this.mQuickSettingsExt.addOpTileSpecs(string));
        }
        Log.d("QSTileHost", "loadTileSpecs() default tile list: " + string);
        if (str == null) {
            str = resources.getString(R.string.quick_settings_tiles);
            if (DEBUG) {
                Log.d("QSTileHost", "Loaded tile specs from config: " + str);
            }
        } else if (DEBUG) {
            Log.d("QSTileHost", "Loaded tile specs from setting: " + str);
        }
        ArrayList arrayList = new ArrayList();
        boolean z = false;
        for (String str2 : str.split(",")) {
            String strTrim = str2.trim();
            if (!strTrim.isEmpty()) {
                if (strTrim.equals("default")) {
                    if (!z) {
                        arrayList.addAll(Arrays.asList(string.split(",")));
                        z = true;
                    }
                } else {
                    arrayList.add(strTrim);
                }
            }
        }
        return arrayList;
    }
}
