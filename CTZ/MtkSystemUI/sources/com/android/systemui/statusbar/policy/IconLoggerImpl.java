package com.android.systemui.statusbar.policy;

import android.R;
import android.content.Context;
import android.metrics.LogMaker;
import android.os.Handler;
import android.os.Looper;
import android.util.ArraySet;
import com.android.internal.logging.MetricsLogger;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class IconLoggerImpl implements IconLogger {
    protected static long MIN_LOG_INTERVAL = 1000;
    private final Context mContext;
    private final Handler mHandler;
    private final List<String> mIconIndex;
    private final ArraySet<String> mIcons = new ArraySet<>();
    private long mLastLog = System.currentTimeMillis();
    private final Runnable mLog = new Runnable() {
        @Override
        public final void run() {
            this.f$0.doLog();
        }
    };
    private final MetricsLogger mLogger;

    public IconLoggerImpl(Context context, Looper looper, MetricsLogger metricsLogger) {
        this.mContext = context;
        this.mHandler = new Handler(looper);
        this.mLogger = metricsLogger;
        this.mIconIndex = Arrays.asList(this.mContext.getResources().getStringArray(R.array.config_deviceSpecificSystemServices));
        doLog();
    }

    @Override
    public void onIconShown(String str) {
        synchronized (this.mIcons) {
            if (this.mIcons.contains(str)) {
                return;
            }
            this.mIcons.add(str);
            if (!this.mHandler.hasCallbacks(this.mLog)) {
                this.mHandler.postDelayed(this.mLog, MIN_LOG_INTERVAL);
            }
        }
    }

    @Override
    public void onIconHidden(String str) {
        synchronized (this.mIcons) {
            if (this.mIcons.contains(str)) {
                this.mIcons.remove(str);
                if (!this.mHandler.hasCallbacks(this.mLog)) {
                    this.mHandler.postDelayed(this.mLog, MIN_LOG_INTERVAL);
                }
            }
        }
    }

    private void doLog() {
        ArraySet<String> arraySet;
        long jCurrentTimeMillis = System.currentTimeMillis();
        long j = jCurrentTimeMillis - this.mLastLog;
        this.mLastLog = jCurrentTimeMillis;
        synchronized (this.mIcons) {
            arraySet = new ArraySet<>(this.mIcons);
        }
        this.mLogger.write(new LogMaker(1093).setType(4).setLatency(j).addTaggedData(1095, Integer.valueOf(arraySet.size())).addTaggedData(1094, Integer.valueOf(getBitField(arraySet))));
    }

    private int getBitField(ArraySet<String> arraySet) {
        Iterator<String> it = arraySet.iterator();
        int i = 0;
        while (it.hasNext()) {
            int iIndexOf = this.mIconIndex.indexOf(it.next());
            if (iIndexOf >= 0) {
                i |= 1 << iIndexOf;
            }
        }
        return i;
    }
}
