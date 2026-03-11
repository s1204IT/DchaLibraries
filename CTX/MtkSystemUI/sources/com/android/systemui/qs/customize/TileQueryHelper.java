package com.android.systemui.qs.customize;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.ArraySet;
import android.widget.Button;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSTileHost;
import com.android.systemui.qs.external.CustomTile;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.mediatek.systemui.ext.OpSystemUICustomizationFactoryBase;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class TileQueryHelper {
    private final Context mContext;
    private boolean mFinished;
    private final TileStateListener mListener;
    private final ArrayList<TileInfo> mTiles = new ArrayList<>();
    private final ArraySet<String> mSpecs = new ArraySet<>();
    private final Handler mBgHandler = new Handler((Looper) Dependency.get(Dependency.BG_LOOPER));
    private final Handler mMainHandler = (Handler) Dependency.get(Dependency.MAIN_HANDLER);

    public static class TileInfo {
        public boolean isSystem;
        public String spec;
        public QSTile.State state;
    }

    public interface TileStateListener {
        void onTilesChanged(List<TileInfo> list);
    }

    public TileQueryHelper(Context context, TileStateListener tileStateListener) {
        this.mContext = context;
        this.mListener = tileStateListener;
    }

    public void queryTiles(QSTileHost qSTileHost) {
        this.mTiles.clear();
        this.mSpecs.clear();
        this.mFinished = false;
        addStockTiles(qSTileHost);
        addPackageTiles(qSTileHost);
    }

    public boolean isFinished() {
        return this.mFinished;
    }

    private void addStockTiles(QSTileHost qSTileHost) {
        String strCustomizeQuickSettingsTileOrder = OpSystemUICustomizationFactoryBase.getOpFactory(this.mContext).makeQuickSettings(this.mContext).customizeQuickSettingsTileOrder(this.mContext.getString(R.string.quick_settings_tiles_stock));
        ArrayList<String> arrayList = new ArrayList();
        arrayList.addAll(Arrays.asList(strCustomizeQuickSettingsTileOrder.split(",")));
        if (Build.IS_DEBUGGABLE) {
            arrayList.add("dbg:mem");
        }
        final ArrayList arrayList2 = new ArrayList();
        for (String str : arrayList) {
            QSTile qSTileCreateTile = qSTileHost.createTile(str);
            if (qSTileCreateTile != null) {
                if (!qSTileCreateTile.isAvailable()) {
                    qSTileCreateTile.destroy();
                } else {
                    qSTileCreateTile.setListening(this, true);
                    qSTileCreateTile.clearState();
                    qSTileCreateTile.refreshState();
                    qSTileCreateTile.setListening(this, false);
                    qSTileCreateTile.setTileSpec(str);
                    arrayList2.add(qSTileCreateTile);
                }
            }
        }
        this.mBgHandler.post(new Runnable() {
            @Override
            public final void run() {
                TileQueryHelper.lambda$addStockTiles$0(this.f$0, arrayList2);
            }
        });
    }

    public static void lambda$addStockTiles$0(TileQueryHelper tileQueryHelper, ArrayList arrayList) {
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            QSTile qSTile = (QSTile) it.next();
            QSTile.State stateCopy = qSTile.getState().copy();
            stateCopy.label = qSTile.getTileLabel();
            qSTile.destroy();
            tileQueryHelper.addTile(qSTile.getTileSpec(), (CharSequence) null, stateCopy, true);
        }
        tileQueryHelper.notifyTilesChanged(false);
    }

    private void addPackageTiles(final QSTileHost qSTileHost) {
        this.mBgHandler.post(new Runnable() {
            @Override
            public final void run() {
                TileQueryHelper.lambda$addPackageTiles$1(this.f$0, qSTileHost);
            }
        });
    }

    public static void lambda$addPackageTiles$1(TileQueryHelper tileQueryHelper, QSTileHost qSTileHost) {
        Collection<QSTile> tiles = qSTileHost.getTiles();
        PackageManager packageManager = tileQueryHelper.mContext.getPackageManager();
        List<ResolveInfo> listQueryIntentServicesAsUser = packageManager.queryIntentServicesAsUser(new Intent("android.service.quicksettings.action.QS_TILE"), 0, ActivityManager.getCurrentUser());
        String string = tileQueryHelper.mContext.getString(R.string.quick_settings_tiles_stock);
        for (ResolveInfo resolveInfo : listQueryIntentServicesAsUser) {
            ComponentName componentName = new ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name);
            if (!string.contains(componentName.flattenToString())) {
                CharSequence charSequenceLoadLabel = resolveInfo.serviceInfo.applicationInfo.loadLabel(packageManager);
                String spec = CustomTile.toSpec(componentName);
                QSTile.State state = tileQueryHelper.getState(tiles, spec);
                if (state != null) {
                    tileQueryHelper.addTile(spec, charSequenceLoadLabel, state, false);
                } else if (resolveInfo.serviceInfo.icon != 0 || resolveInfo.serviceInfo.applicationInfo.icon != 0) {
                    Drawable drawableLoadIcon = resolveInfo.serviceInfo.loadIcon(packageManager);
                    if ("android.permission.BIND_QUICK_SETTINGS_TILE".equals(resolveInfo.serviceInfo.permission) && drawableLoadIcon != null) {
                        drawableLoadIcon.mutate();
                        drawableLoadIcon.setTint(tileQueryHelper.mContext.getColor(android.R.color.white));
                        CharSequence charSequenceLoadLabel2 = resolveInfo.serviceInfo.loadLabel(packageManager);
                        tileQueryHelper.addTile(spec, drawableLoadIcon, charSequenceLoadLabel2 != null ? charSequenceLoadLabel2.toString() : "null", charSequenceLoadLabel);
                    }
                }
            }
        }
        tileQueryHelper.notifyTilesChanged(true);
    }

    private void notifyTilesChanged(final boolean z) {
        final ArrayList arrayList = new ArrayList(this.mTiles);
        this.mMainHandler.post(new Runnable() {
            @Override
            public final void run() {
                TileQueryHelper.lambda$notifyTilesChanged$2(this.f$0, arrayList, z);
            }
        });
    }

    public static void lambda$notifyTilesChanged$2(TileQueryHelper tileQueryHelper, ArrayList arrayList, boolean z) {
        tileQueryHelper.mListener.onTilesChanged(arrayList);
        tileQueryHelper.mFinished = z;
    }

    private QSTile.State getState(Collection<QSTile> collection, String str) {
        for (QSTile qSTile : collection) {
            if (str.equals(qSTile.getTileSpec())) {
                return qSTile.getState().copy();
            }
        }
        return null;
    }

    private void addTile(String str, CharSequence charSequence, QSTile.State state, boolean z) {
        if (this.mSpecs.contains(str)) {
            return;
        }
        TileInfo tileInfo = new TileInfo();
        tileInfo.state = state;
        tileInfo.state.dualTarget = false;
        tileInfo.state.expandedAccessibilityClassName = Button.class.getName();
        tileInfo.spec = str;
        QSTile.State state2 = tileInfo.state;
        if (z || TextUtils.equals(state.label, charSequence)) {
            charSequence = null;
        }
        state2.secondaryLabel = charSequence;
        tileInfo.isSystem = z;
        this.mTiles.add(tileInfo);
        this.mSpecs.add(str);
    }

    private void addTile(String str, Drawable drawable, CharSequence charSequence, CharSequence charSequence2) {
        QSTile.State state = new QSTile.State();
        state.label = charSequence;
        state.contentDescription = charSequence;
        state.icon = new QSTileImpl.DrawableIcon(drawable);
        addTile(str, charSequence2, state, false);
    }
}
