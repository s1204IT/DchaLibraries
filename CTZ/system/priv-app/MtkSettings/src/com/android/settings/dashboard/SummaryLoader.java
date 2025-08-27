package com.android.settings.dashboard;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.Tile;
import com.android.settingslib.utils.ThreadUtils;
import java.util.Iterator;
import java.util.List;

/* loaded from: classes.dex */
public class SummaryLoader {
    public static final String SUMMARY_PROVIDER_FACTORY = "SUMMARY_PROVIDER_FACTORY";
    private final Activity mActivity;
    private final String mCategoryKey;
    private final DashboardFeatureProvider mDashboardFeatureProvider;
    private boolean mListening;
    private SummaryConsumer mSummaryConsumer;
    private final Worker mWorker;
    private boolean mWorkerListening;
    private final ArrayMap<SummaryProvider, ComponentName> mSummaryProviderMap = new ArrayMap<>();
    private final ArrayMap<String, CharSequence> mSummaryTextMap = new ArrayMap<>();
    private ArraySet<BroadcastReceiver> mReceivers = new ArraySet<>();
    private final HandlerThread mWorkerThread = new HandlerThread("SummaryLoader", 10);

    public interface SummaryConsumer {
        void notifySummaryChanged(Tile tile);
    }

    public interface SummaryProvider {
        void setListening(boolean z);
    }

    public interface SummaryProviderFactory {
        SummaryProvider createSummaryProvider(Activity activity, SummaryLoader summaryLoader);
    }

    public SummaryLoader(Activity activity, String str) {
        this.mDashboardFeatureProvider = FeatureFactory.getFactory(activity).getDashboardFeatureProvider(activity);
        this.mCategoryKey = str;
        this.mWorkerThread.start();
        this.mWorker = new Worker(this.mWorkerThread.getLooper());
        this.mActivity = activity;
    }

    public void release() {
        this.mWorkerThread.quitSafely();
        setListeningW(false);
    }

    public void setSummaryConsumer(SummaryConsumer summaryConsumer) {
        this.mSummaryConsumer = summaryConsumer;
    }

    public void setSummary(SummaryProvider summaryProvider, final CharSequence charSequence) {
        final ComponentName componentName = this.mSummaryProviderMap.get(summaryProvider);
        ThreadUtils.postOnMainThread(new Runnable() { // from class: com.android.settings.dashboard.-$$Lambda$SummaryLoader$EirySW2ETuFFjqqH756jJXvHagg
            @Override // java.lang.Runnable
            public final void run() {
                SummaryLoader.lambda$setSummary$0(this.f$0, componentName, charSequence);
            }
        });
    }

    public static /* synthetic */ void lambda$setSummary$0(SummaryLoader summaryLoader, ComponentName componentName, CharSequence charSequence) {
        Tile tileFromCategory = summaryLoader.getTileFromCategory(summaryLoader.mDashboardFeatureProvider.getTilesForCategory(summaryLoader.mCategoryKey), componentName);
        if (tileFromCategory == null) {
            return;
        }
        summaryLoader.updateSummaryIfNeeded(tileFromCategory, charSequence);
    }

    void updateSummaryIfNeeded(Tile tile, CharSequence charSequence) {
        if (TextUtils.equals(tile.summary, charSequence)) {
            return;
        }
        this.mSummaryTextMap.put(this.mDashboardFeatureProvider.getDashboardKeyForTile(tile), charSequence);
        tile.summary = charSequence;
        if (this.mSummaryConsumer != null) {
            this.mSummaryConsumer.notifySummaryChanged(tile);
        }
    }

    public void setListening(boolean z) {
        if (this.mListening == z) {
            return;
        }
        this.mListening = z;
        for (int i = 0; i < this.mReceivers.size(); i++) {
            this.mActivity.unregisterReceiver(this.mReceivers.valueAt(i));
        }
        this.mReceivers.clear();
        this.mWorker.removeMessages(3);
        if (!z) {
            this.mWorker.obtainMessage(3, 0).sendToTarget();
        } else {
            if (this.mSummaryProviderMap.isEmpty()) {
                if (!this.mWorker.hasMessages(1)) {
                    this.mWorker.sendEmptyMessage(1);
                    return;
                }
                return;
            }
            this.mWorker.obtainMessage(3, 1).sendToTarget();
        }
    }

    private SummaryProvider getSummaryProvider(Tile tile) {
        Bundle metaData;
        String string;
        if (!this.mActivity.getPackageName().equals(tile.intent.getComponent().getPackageName()) || (metaData = getMetaData(tile)) == null || (string = metaData.getString("com.android.settings.FRAGMENT_CLASS")) == null) {
            return null;
        }
        try {
            return ((SummaryProviderFactory) Class.forName(string).getField(SUMMARY_PROVIDER_FACTORY).get(null)).createSummaryProvider(this.mActivity, this);
        } catch (ClassCastException | ClassNotFoundException | IllegalAccessException | NoSuchFieldException e) {
            return null;
        }
    }

    private Bundle getMetaData(Tile tile) {
        return tile.metaData;
    }

    public void updateSummaryToCache(DashboardCategory dashboardCategory) {
        if (dashboardCategory == null) {
            return;
        }
        for (Tile tile : dashboardCategory.getTiles()) {
            String dashboardKeyForTile = this.mDashboardFeatureProvider.getDashboardKeyForTile(tile);
            if (this.mSummaryTextMap.containsKey(dashboardKeyForTile)) {
                tile.summary = this.mSummaryTextMap.get(dashboardKeyForTile);
            }
        }
    }

    private synchronized void setListeningW(boolean z) {
        if (this.mWorkerListening == z) {
            return;
        }
        this.mWorkerListening = z;
        Iterator<SummaryProvider> it = this.mSummaryProviderMap.keySet().iterator();
        while (it.hasNext()) {
            try {
                it.next().setListening(z);
            } catch (Exception e) {
                Log.d("SummaryLoader", "Problem in setListening", e);
            }
        }
    }

    private synchronized void makeProviderW(Tile tile) {
        SummaryProvider summaryProvider = getSummaryProvider(tile);
        if (summaryProvider != null) {
            this.mSummaryProviderMap.put(summaryProvider, tile.intent.getComponent());
        }
    }

    private Tile getTileFromCategory(DashboardCategory dashboardCategory, ComponentName componentName) {
        if (dashboardCategory == null || dashboardCategory.getTilesCount() == 0) {
            return null;
        }
        List<Tile> tiles = dashboardCategory.getTiles();
        int size = tiles.size();
        for (int i = 0; i < size; i++) {
            Tile tile = tiles.get(i);
            if (componentName.equals(tile.intent.getComponent())) {
                return tile;
            }
        }
        return null;
    }

    private class Worker extends Handler {
        public Worker(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    DashboardCategory tilesForCategory = SummaryLoader.this.mDashboardFeatureProvider.getTilesForCategory(SummaryLoader.this.mCategoryKey);
                    if (tilesForCategory != null && tilesForCategory.getTilesCount() != 0) {
                        Iterator<Tile> it = tilesForCategory.getTiles().iterator();
                        while (it.hasNext()) {
                            SummaryLoader.this.makeProviderW(it.next());
                        }
                        SummaryLoader.this.setListeningW(true);
                        break;
                    }
                    break;
                case 2:
                    SummaryLoader.this.makeProviderW((Tile) message.obj);
                    break;
                case 3:
                    SummaryLoader.this.setListeningW(message.obj != null && message.obj.equals(1));
                    break;
            }
        }
    }
}
