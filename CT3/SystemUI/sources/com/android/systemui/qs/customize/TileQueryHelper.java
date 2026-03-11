package com.android.systemui.qs.customize;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.BenesseExtension;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import com.android.systemui.qs.external.CustomTile;
import com.android.systemui.statusbar.phone.QSTileHost;
import com.mediatek.systemui.PluginManager;
import com.mediatek.systemui.ext.IQuickSettingsPlugin;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TileQueryHelper {
    private final Context mContext;
    private TileStateListener mListener;
    private final ArrayList<TileInfo> mTiles = new ArrayList<>();
    private final ArrayList<String> mSpecs = new ArrayList<>();

    public static class TileInfo {
        public CharSequence appLabel;
        public boolean isSystem;
        public String spec;
        public QSTile.State state;
    }

    public interface TileStateListener {
        void onTilesChanged(List<TileInfo> list);
    }

    public TileQueryHelper(Context context, QSTileHost host) {
        this.mContext = context;
        addSystemTiles(host);
    }

    private void addSystemTiles(final QSTileHost host) {
        String possible = this.mContext.getString(R.string.quick_settings_tiles_default) + ",hotspot,inversion,saver,work,cast,night";
        IQuickSettingsPlugin quickSettingsPlugin = PluginManager.getQuickSettingsPlugin(this.mContext);
        String[] possibleTiles = (quickSettingsPlugin.customizeQuickSettingsTileOrder(possible) + ",hotspot,inversion,saver,work,cast,night").split(",");
        Handler qsHandler = new Handler(host.getLooper());
        final Handler mainHandler = new Handler(Looper.getMainLooper());
        for (final String spec : possibleTiles) {
            final QSTile<?> tile = host.createTile(spec);
            if (tile != null && tile.isAvailable()) {
                tile.setListening(this, true);
                tile.clearState();
                tile.refreshState();
                tile.setListening(this, false);
                qsHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        final QSTile.State state = tile.newTileState();
                        tile.getState().copyTo(state);
                        state.label = tile.getTileLabel();
                        Handler handler = mainHandler;
                        final String str = spec;
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                TileQueryHelper.this.addTile(str, null, state, true);
                                TileQueryHelper.this.mListener.onTilesChanged(TileQueryHelper.this.mTiles);
                            }
                        });
                    }
                });
            }
        }
        qsHandler.post(new Runnable() {
            @Override
            public void run() {
                Handler handler = mainHandler;
                final QSTileHost qSTileHost = host;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        new QueryTilesTask(TileQueryHelper.this, null).execute(qSTileHost.getTiles());
                    }
                });
            }
        });
    }

    public void setListener(TileStateListener listener) {
        this.mListener = listener;
    }

    public void addTile(String spec, CharSequence appLabel, QSTile.State state, boolean isSystem) {
        if (this.mSpecs.contains(spec)) {
            return;
        }
        TileInfo info = new TileInfo();
        info.state = state;
        QSTile.State state2 = info.state;
        String name = Button.class.getName();
        info.state.expandedAccessibilityClassName = name;
        state2.minimalAccessibilityClassName = name;
        info.spec = spec;
        info.appLabel = appLabel;
        info.isSystem = isSystem;
        this.mTiles.add(info);
        this.mSpecs.add(spec);
    }

    public void addTile(String spec, Drawable drawable, CharSequence label, CharSequence appLabel, Context context) {
        QSTile.State state = new QSTile.State();
        state.label = label;
        state.contentDescription = label;
        state.icon = new QSTile.DrawableIcon(drawable);
        addTile(spec, appLabel, state, false);
    }

    private class QueryTilesTask extends AsyncTask<Collection<QSTile<?>>, Void, Collection<TileInfo>> {
        QueryTilesTask(TileQueryHelper this$0, QueryTilesTask queryTilesTask) {
            this();
        }

        private QueryTilesTask() {
        }

        @Override
        public Collection<TileInfo> doInBackground(Collection<QSTile<?>>... params) {
            List<TileInfo> tiles = new ArrayList<>();
            PackageManager pm = TileQueryHelper.this.mContext.getPackageManager();
            List<ResolveInfo> services = pm.queryIntentServicesAsUser(new Intent("android.service.quicksettings.action.QS_TILE"), 0, ActivityManager.getCurrentUser());
            if (BenesseExtension.getDchaState() != 0) {
                services = new ArrayList<>();
            }
            for (ResolveInfo info : services) {
                String packageName = info.serviceInfo.packageName;
                ComponentName componentName = new ComponentName(packageName, info.serviceInfo.name);
                CharSequence appLabel = info.serviceInfo.applicationInfo.loadLabel(pm);
                String spec = CustomTile.toSpec(componentName);
                QSTile.State state = getState(params[0], spec);
                if (state != null) {
                    TileQueryHelper.this.addTile(spec, appLabel, state, false);
                } else if (info.serviceInfo.icon != 0 || info.serviceInfo.applicationInfo.icon != 0) {
                    Drawable icon = info.serviceInfo.loadIcon(pm);
                    if ("android.permission.BIND_QUICK_SETTINGS_TILE".equals(info.serviceInfo.permission) && icon != null) {
                        icon.mutate();
                        icon.setTint(TileQueryHelper.this.mContext.getColor(android.R.color.white));
                        CharSequence label = info.serviceInfo.loadLabel(pm);
                        TileQueryHelper.this.addTile(spec, icon, label != null ? label.toString() : "null", appLabel, TileQueryHelper.this.mContext);
                    }
                }
            }
            return tiles;
        }

        private QSTile.State getState(Collection<QSTile<?>> tiles, String spec) {
            for (QSTile<?> tile : tiles) {
                if (spec.equals(tile.getTileSpec())) {
                    QSTile.State state = tile.newTileState();
                    tile.getState().copyTo(state);
                    return state;
                }
            }
            return null;
        }

        @Override
        public void onPostExecute(Collection<TileInfo> result) {
            TileQueryHelper.this.mTiles.addAll(result);
            TileQueryHelper.this.mListener.onTilesChanged(TileQueryHelper.this.mTiles);
        }
    }
}
