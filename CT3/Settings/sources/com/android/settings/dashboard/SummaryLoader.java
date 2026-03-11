package com.android.settings.dashboard;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.Tile;
import com.mediatek.settings.dashboard.ExternalSummaryProvider;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class SummaryLoader {
    private final Activity mActivity;
    private DashboardAdapter mAdapter;
    private boolean mListening;
    private final Worker mWorker;
    private boolean mWorkerListening;
    private final ArrayMap<SummaryProvider, ComponentName> mSummaryMap = new ArrayMap<>();
    private final List<Tile> mTiles = new ArrayList();
    private ArraySet<BroadcastReceiver> mReceivers = new ArraySet<>();
    private final Handler mHandler = new Handler();
    private final HandlerThread mWorkerThread = new HandlerThread("SummaryLoader", 10);

    public interface SummaryProvider {
        void setListening(boolean z);
    }

    public interface SummaryProviderFactory {
        SummaryProvider createSummaryProvider(Activity activity, SummaryLoader summaryLoader);
    }

    public SummaryLoader(Activity activity, List<DashboardCategory> categories) {
        this.mWorkerThread.start();
        this.mWorker = new Worker(this.mWorkerThread.getLooper());
        this.mActivity = activity;
        for (int i = 0; i < categories.size(); i++) {
            List<Tile> tiles = categories.get(i).tiles;
            for (int j = 0; j < tiles.size(); j++) {
                Tile tile = tiles.get(j);
                this.mWorker.obtainMessage(1, tile).sendToTarget();
            }
        }
    }

    public void release() {
        this.mWorkerThread.quitSafely();
        setListeningW(false);
    }

    public void setAdapter(DashboardAdapter adapter) {
        this.mAdapter = adapter;
    }

    public void setSummary(SummaryProvider provider, final CharSequence summary) {
        final ComponentName component = this.mSummaryMap.get(provider);
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                Tile tile = SummaryLoader.this.mAdapter.getTile(component);
                if (tile == null) {
                    return;
                }
                tile.summary = summary;
                SummaryLoader.this.mAdapter.notifyChanged(tile);
            }
        });
    }

    public void setListening(boolean listening) {
        if (this.mListening == listening) {
            return;
        }
        this.mListening = listening;
        for (int i = 0; i < this.mReceivers.size(); i++) {
            this.mActivity.unregisterReceiver(this.mReceivers.valueAt(i));
        }
        this.mReceivers.clear();
        this.mWorker.removeMessages(2);
        this.mWorker.obtainMessage(2, listening ? 1 : 0, 0).sendToTarget();
    }

    private SummaryProvider getSummaryProvider(Tile tile) {
        String clsName;
        if (!this.mActivity.getPackageName().equals(tile.intent.getComponent().getPackageName())) {
            return ExternalSummaryProvider.createExternalSummaryProvider(this.mActivity, this, tile);
        }
        Bundle metaData = getMetaData(tile);
        if (metaData == null || (clsName = metaData.getString("com.android.settings.FRAGMENT_CLASS")) == null) {
            return null;
        }
        try {
            Class<?> cls = Class.forName(clsName);
            Field field = cls.getField("SUMMARY_PROVIDER_FACTORY");
            SummaryProviderFactory factory = (SummaryProviderFactory) field.get(null);
            return factory.createSummaryProvider(this.mActivity, this);
        } catch (ClassCastException e) {
            return null;
        } catch (ClassNotFoundException e2) {
            return null;
        } catch (IllegalAccessException e3) {
            return null;
        } catch (NoSuchFieldException e4) {
            return null;
        }
    }

    private Bundle getMetaData(Tile tile) {
        return tile.metaData;
    }

    public void registerReceiver(final BroadcastReceiver receiver, final IntentFilter filter) {
        this.mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!SummaryLoader.this.mListening) {
                    return;
                }
                SummaryLoader.this.mReceivers.add(receiver);
                SummaryLoader.this.mActivity.registerReceiver(receiver, filter);
            }
        });
    }

    public synchronized void setListeningW(boolean listening) {
        if (this.mWorkerListening == listening) {
            return;
        }
        this.mWorkerListening = listening;
        for (SummaryProvider p : this.mSummaryMap.keySet()) {
            try {
                p.setListening(listening);
            } catch (Exception e) {
                Log.d("SummaryLoader", "Problem in setListening", e);
            }
        }
    }

    public synchronized void makeProviderW(Tile tile) {
        SummaryProvider provider = getSummaryProvider(tile);
        if (provider != null) {
            this.mSummaryMap.put(provider, tile.intent.getComponent());
        }
    }

    private class Worker extends Handler {
        public Worker(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DefaultWfcSettingsExt.PAUSE:
                    Tile tile = (Tile) msg.obj;
                    SummaryLoader.this.makeProviderW(tile);
                    break;
                case DefaultWfcSettingsExt.CREATE:
                    boolean listening = msg.arg1 != 0;
                    SummaryLoader.this.setListeningW(listening);
                    break;
            }
        }
    }
}
