package android.util;

import android.os.SystemClock;
import java.util.ArrayList;

public class TimingLogger {
    private boolean mDisabled;
    private String mLabel;
    ArrayList<String> mSplitLabels;
    ArrayList<Long> mSplits;
    private String mTag;

    public TimingLogger(String str, String str2) {
        reset(str, str2);
    }

    public void reset(String str, String str2) {
        this.mTag = str;
        this.mLabel = str2;
        reset();
    }

    public void reset() {
        this.mDisabled = !Log.isLoggable(this.mTag, 2);
        if (this.mDisabled) {
            return;
        }
        if (this.mSplits == null) {
            this.mSplits = new ArrayList<>();
            this.mSplitLabels = new ArrayList<>();
        } else {
            this.mSplits.clear();
            this.mSplitLabels.clear();
        }
        addSplit(null);
    }

    public void addSplit(String str) {
        if (this.mDisabled) {
            return;
        }
        this.mSplits.add(Long.valueOf(SystemClock.elapsedRealtime()));
        this.mSplitLabels.add(str);
    }

    public void dumpToLog() {
        if (this.mDisabled) {
            return;
        }
        Log.d(this.mTag, this.mLabel + ": begin");
        long jLongValue = this.mSplits.get(0).longValue();
        long jLongValue2 = jLongValue;
        for (int i = 1; i < this.mSplits.size(); i++) {
            jLongValue2 = this.mSplits.get(i).longValue();
            String str = this.mSplitLabels.get(i);
            long jLongValue3 = this.mSplits.get(i - 1).longValue();
            Log.d(this.mTag, this.mLabel + ":      " + (jLongValue2 - jLongValue3) + " ms, " + str);
        }
        Log.d(this.mTag, this.mLabel + ": end, " + (jLongValue2 - jLongValue) + " ms");
    }
}
