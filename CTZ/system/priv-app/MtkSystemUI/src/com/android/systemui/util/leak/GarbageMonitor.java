package com.android.systemui.util.leak;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Debug;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;
import android.util.LongSparseArray;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.SystemUI;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

/* loaded from: classes.dex */
public class GarbageMonitor {
    private static final boolean HEAP_TRACKING_ENABLED;
    private static final boolean LEAK_REPORTING_ENABLED;
    private final ActivityManager mAm;
    private final Context mContext;
    private DumpTruck mDumpTruck;
    private final Handler mHandler;
    private long mHeapLimit;
    private final LeakReporter mLeakReporter;
    private MemoryTile mQSTile;
    private final TrackedGarbage mTrackedGarbage;
    private final LongSparseArray<ProcessMemInfo> mData = new LongSparseArray<>();
    private final ArrayList<Long> mPids = new ArrayList<>();
    private int[] mPidsArray = new int[1];

    static {
        boolean z = false;
        if (Build.IS_DEBUGGABLE && SystemProperties.getBoolean("debug.enable_leak_reporting", false)) {
            z = true;
        }
        LEAK_REPORTING_ENABLED = z;
        HEAP_TRACKING_ENABLED = Build.IS_DEBUGGABLE;
    }

    public GarbageMonitor(Context context, Looper looper, LeakDetector leakDetector, LeakReporter leakReporter) {
        this.mContext = context.getApplicationContext();
        this.mAm = (ActivityManager) context.getSystemService("activity");
        this.mHandler = new BackgroundHeapCheckHandler(looper);
        this.mTrackedGarbage = leakDetector.getTrackedGarbage();
        this.mLeakReporter = leakReporter;
        this.mDumpTruck = new DumpTruck(this.mContext);
        this.mHeapLimit = this.mContext.getResources().getInteger(R.integer.watch_heap_limit);
    }

    public void startLeakMonitor() {
        if (this.mTrackedGarbage == null) {
            return;
        }
        this.mHandler.sendEmptyMessage(1000);
    }

    public void startHeapTracking() {
        startTrackingProcess(Process.myPid(), this.mContext.getPackageName(), System.currentTimeMillis());
        this.mHandler.sendEmptyMessage(3000);
    }

    private boolean gcAndCheckGarbage() {
        if (this.mTrackedGarbage.countOldGarbage() > 5) {
            Runtime.getRuntime().gc();
            return true;
        }
        return false;
    }

    void reinspectGarbageAfterGc() throws IOException {
        int iCountOldGarbage = this.mTrackedGarbage.countOldGarbage();
        if (iCountOldGarbage > 5) {
            this.mLeakReporter.dumpLeak(iCountOldGarbage);
        }
    }

    public ProcessMemInfo getMemInfo(int i) {
        return this.mData.get(i);
    }

    public int[] getTrackedProcesses() {
        return this.mPidsArray;
    }

    public void startTrackingProcess(long j, String str, long j2) {
        synchronized (this.mPids) {
            if (this.mPids.contains(Long.valueOf(j))) {
                return;
            }
            this.mPids.add(Long.valueOf(j));
            updatePidsArrayL();
            this.mData.put(j, new ProcessMemInfo(j, str, j2));
        }
    }

    private void updatePidsArrayL() {
        int size = this.mPids.size();
        this.mPidsArray = new int[size];
        StringBuffer stringBuffer = new StringBuffer("Now tracking processes: ");
        for (int i = 0; i < size; i++) {
            int iIntValue = this.mPids.get(i).intValue();
            this.mPidsArray[i] = iIntValue;
            stringBuffer.append(iIntValue);
            stringBuffer.append(" ");
        }
        Log.v("GarbageMonitor", stringBuffer.toString());
    }

    private void update() {
        synchronized (this.mPids) {
            Debug.MemoryInfo[] processMemoryInfo = this.mAm.getProcessMemoryInfo(this.mPidsArray);
            int i = 0;
            while (true) {
                if (i >= processMemoryInfo.length) {
                    break;
                }
                Debug.MemoryInfo memoryInfo = processMemoryInfo[i];
                if (i > this.mPids.size()) {
                    Log.e("GarbageMonitor", "update: unknown process info received: " + memoryInfo);
                    break;
                }
                long jIntValue = this.mPids.get(i).intValue();
                ProcessMemInfo processMemInfo = this.mData.get(jIntValue);
                processMemInfo.head = (processMemInfo.head + 1) % processMemInfo.pss.length;
                long[] jArr = processMemInfo.pss;
                int i2 = processMemInfo.head;
                long totalPss = memoryInfo.getTotalPss();
                processMemInfo.currentPss = totalPss;
                jArr[i2] = totalPss;
                long[] jArr2 = processMemInfo.uss;
                int i3 = processMemInfo.head;
                long totalPrivateDirty = memoryInfo.getTotalPrivateDirty();
                processMemInfo.currentUss = totalPrivateDirty;
                jArr2[i3] = totalPrivateDirty;
                if (processMemInfo.currentPss > processMemInfo.max) {
                    processMemInfo.max = processMemInfo.currentPss;
                }
                if (processMemInfo.currentUss > processMemInfo.max) {
                    processMemInfo.max = processMemInfo.currentUss;
                }
                if (processMemInfo.currentPss == 0) {
                    Log.v("GarbageMonitor", "update: pid " + jIntValue + " has pss=0, it probably died");
                    this.mData.remove(jIntValue);
                }
                i++;
            }
            for (int size = this.mPids.size() - 1; size >= 0; size--) {
                if (this.mData.get(this.mPids.get(size).intValue()) == null) {
                    this.mPids.remove(size);
                    updatePidsArrayL();
                }
            }
        }
        if (this.mQSTile != null) {
            this.mQSTile.update();
        }
    }

    private void setTile(MemoryTile memoryTile) {
        this.mQSTile = memoryTile;
        if (memoryTile != null) {
            memoryTile.update();
        }
    }

    private static String formatBytes(long j) {
        String[] strArr = {"B", "K", "M", "G", "T"};
        int i = 0;
        while (i < strArr.length && j >= 1024) {
            j /= 1024;
            i++;
        }
        return j + strArr[i];
    }

    private void dumpHprofAndShare() {
        this.mContext.startActivity(this.mDumpTruck.captureHeaps(getTrackedProcesses()).createShareIntent());
    }

    private static class MemoryIconDrawable extends Drawable {
        final Drawable baseIcon;
        final float dp;
        long limit;
        final Paint paint = new Paint();
        long pss;

        MemoryIconDrawable(Context context) {
            this.baseIcon = context.getDrawable(R.drawable.ic_memory).mutate();
            this.dp = context.getResources().getDisplayMetrics().density;
            this.paint.setColor(QSTileImpl.getColorForState(context, 2));
        }

        public void setPss(long j) {
            if (j != this.pss) {
                this.pss = j;
                invalidateSelf();
            }
        }

        public void setLimit(long j) {
            if (j != this.limit) {
                this.limit = j;
                invalidateSelf();
            }
        }

        @Override // android.graphics.drawable.Drawable
        public void draw(Canvas canvas) {
            this.baseIcon.draw(canvas);
            if (this.limit > 0 && this.pss > 0) {
                float fMin = Math.min(1.0f, this.pss / this.limit);
                Rect bounds = getBounds();
                canvas.translate(bounds.left + (this.dp * 8.0f), bounds.top + (5.0f * this.dp));
                canvas.drawRect(0.0f, this.dp * 14.0f * (1.0f - fMin), (8.0f * this.dp) + 1.0f, (14.0f * this.dp) + 1.0f, this.paint);
            }
        }

        @Override // android.graphics.drawable.Drawable
        public void setBounds(int i, int i2, int i3, int i4) {
            super.setBounds(i, i2, i3, i4);
            this.baseIcon.setBounds(i, i2, i3, i4);
        }

        @Override // android.graphics.drawable.Drawable
        public int getIntrinsicHeight() {
            return this.baseIcon.getIntrinsicHeight();
        }

        @Override // android.graphics.drawable.Drawable
        public int getIntrinsicWidth() {
            return this.baseIcon.getIntrinsicWidth();
        }

        @Override // android.graphics.drawable.Drawable
        public void setAlpha(int i) {
            this.baseIcon.setAlpha(i);
        }

        @Override // android.graphics.drawable.Drawable
        public void setColorFilter(ColorFilter colorFilter) {
            this.baseIcon.setColorFilter(colorFilter);
            this.paint.setColorFilter(colorFilter);
        }

        @Override // android.graphics.drawable.Drawable
        public void setTint(int i) {
            super.setTint(i);
            this.baseIcon.setTint(i);
        }

        @Override // android.graphics.drawable.Drawable
        public void setTintList(ColorStateList colorStateList) {
            super.setTintList(colorStateList);
            this.baseIcon.setTintList(colorStateList);
        }

        @Override // android.graphics.drawable.Drawable
        public void setTintMode(PorterDuff.Mode mode) {
            super.setTintMode(mode);
            this.baseIcon.setTintMode(mode);
        }

        @Override // android.graphics.drawable.Drawable
        public int getOpacity() {
            return -3;
        }
    }

    private static class MemoryGraphIcon extends QSTile.Icon {
        long limit;
        long pss;

        private MemoryGraphIcon() {
        }

        public void setPss(long j) {
            this.pss = j;
        }

        public void setHeapLimit(long j) {
            this.limit = j;
        }

        @Override // com.android.systemui.plugins.qs.QSTile.Icon
        public Drawable getDrawable(Context context) {
            MemoryIconDrawable memoryIconDrawable = new MemoryIconDrawable(context);
            memoryIconDrawable.setPss(this.pss);
            memoryIconDrawable.setLimit(this.limit);
            return memoryIconDrawable;
        }
    }

    public static class MemoryTile extends QSTileImpl<QSTile.State> {
        private final GarbageMonitor gm;
        private ProcessMemInfo pmi;

        public MemoryTile(QSHost qSHost) {
            super(qSHost);
            this.gm = (GarbageMonitor) Dependency.get(GarbageMonitor.class);
        }

        @Override // com.android.systemui.qs.tileimpl.QSTileImpl
        public QSTile.State newTileState() {
            return new QSTile.State();
        }

        @Override // com.android.systemui.qs.tileimpl.QSTileImpl
        public Intent getLongClickIntent() {
            return new Intent();
        }

        @Override // com.android.systemui.qs.tileimpl.QSTileImpl
        protected void handleClick() {
            getHost().collapsePanels();
            QSTileImpl<TState>.H h = this.mHandler;
            final GarbageMonitor garbageMonitor = this.gm;
            Objects.requireNonNull(garbageMonitor);
            h.post(new Runnable() { // from class: com.android.systemui.util.leak.-$$Lambda$GarbageMonitor$MemoryTile$RY8VPTPHkg6QWMQHYqV3eM4EOMY
                @Override // java.lang.Runnable
                public final void run() {
                    garbageMonitor.dumpHprofAndShare();
                }
            });
        }

        @Override // com.android.systemui.qs.tileimpl.QSTileImpl, com.android.systemui.plugins.qs.QSTile
        public int getMetricsCategory() {
            return 0;
        }

        @Override // com.android.systemui.qs.tileimpl.QSTileImpl
        public void handleSetListening(boolean z) {
            if (this.gm != null) {
                this.gm.setTile(z ? this : null);
            }
            ActivityManager activityManager = (ActivityManager) this.mContext.getSystemService(ActivityManager.class);
            if (z && this.gm.mHeapLimit > 0) {
                activityManager.setWatchHeapLimit(1024 * this.gm.mHeapLimit);
            } else {
                activityManager.clearWatchHeapLimit();
            }
        }

        @Override // com.android.systemui.plugins.qs.QSTile
        public CharSequence getTileLabel() {
            return getState().label;
        }

        @Override // com.android.systemui.qs.tileimpl.QSTileImpl
        protected void handleUpdateState(QSTile.State state, Object obj) {
            this.pmi = this.gm.getMemInfo(Process.myPid());
            MemoryGraphIcon memoryGraphIcon = new MemoryGraphIcon();
            memoryGraphIcon.setHeapLimit(this.gm.mHeapLimit);
            if (this.pmi != null) {
                memoryGraphIcon.setPss(this.pmi.currentPss);
                state.label = this.mContext.getString(R.string.heap_dump_tile_name);
                state.secondaryLabel = String.format("pss: %s / %s", GarbageMonitor.formatBytes(this.pmi.currentPss * 1024), GarbageMonitor.formatBytes(this.gm.mHeapLimit * 1024));
            } else {
                memoryGraphIcon.setPss(0L);
                state.label = "Dump SysUI";
                state.secondaryLabel = null;
            }
            state.icon = memoryGraphIcon;
        }

        public void update() {
            refreshState();
        }
    }

    public static class ProcessMemInfo {
        public long currentPss;
        public long currentUss;
        public String name;
        public long pid;
        public long startTime;
        public long[] pss = new long[256];
        public long[] uss = new long[256];
        public long max = 1;
        public int head = 0;

        public ProcessMemInfo(long j, String str, long j2) {
            this.pid = j;
            this.name = str;
            this.startTime = j2;
        }

        public long getUptime() {
            return System.currentTimeMillis() - this.startTime;
        }
    }

    public static class Service extends SystemUI {
        private GarbageMonitor mGarbageMonitor;

        @Override // com.android.systemui.SystemUI
        public void start() {
            boolean z = Settings.Secure.getInt(this.mContext.getContentResolver(), "sysui_force_enable_leak_reporting", 0) != 0;
            this.mGarbageMonitor = (GarbageMonitor) Dependency.get(GarbageMonitor.class);
            if ((GarbageMonitor.LEAK_REPORTING_ENABLED || z) && !GarbageMonitor.doesFileExist("disableleakmonitor")) {
                this.mGarbageMonitor.startLeakMonitor();
            }
            if ((GarbageMonitor.HEAP_TRACKING_ENABLED || z) && !GarbageMonitor.doesFileExist("disableheaptracking")) {
                this.mGarbageMonitor.startHeapTracking();
            }
        }
    }

    private class BackgroundHeapCheckHandler extends Handler {
        BackgroundHeapCheckHandler(Looper looper) {
            super(looper);
            if (Looper.getMainLooper().equals(looper)) {
                throw new RuntimeException("BackgroundHeapCheckHandler may not run on the ui thread");
            }
        }

        @Override // android.os.Handler
        public void handleMessage(Message message) {
            int i = message.what;
            if (i != 1000) {
                if (i == 3000) {
                    GarbageMonitor.this.update();
                    removeMessages(3000);
                    sendEmptyMessageDelayed(3000, 60000L);
                    return;
                }
                return;
            }
            if (GarbageMonitor.this.gcAndCheckGarbage()) {
                final GarbageMonitor garbageMonitor = GarbageMonitor.this;
                postDelayed(new Runnable() { // from class: com.android.systemui.util.leak.-$$Lambda$XMHjUeThvUDRPlJmBo9djG71pM8
                    @Override // java.lang.Runnable
                    public final void run() throws IOException {
                        garbageMonitor.reinspectGarbageAfterGc();
                    }
                }, 100L);
            }
            removeMessages(1000);
            sendEmptyMessageDelayed(1000, 900000L);
        }
    }

    private static boolean doesFileExist(String str) {
        if (str == null || str.length() == 0) {
            return false;
        }
        return new File(Environment.getExternalStorageDirectory(), str).exists();
    }
}
