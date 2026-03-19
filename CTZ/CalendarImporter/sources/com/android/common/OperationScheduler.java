package com.android.common;

import android.content.SharedPreferences;
import android.text.format.Time;
import java.util.Map;
import java.util.TreeMap;

public class OperationScheduler {
    private static final String PREFIX = "OperationScheduler_";
    private final SharedPreferences mStorage;

    public static class Options {
        public long backoffFixedMillis = 0;
        public long backoffIncrementalMillis = 5000;
        public int backoffExponentialMillis = 0;
        public long maxMoratoriumMillis = 86400000;
        public long minTriggerMillis = 0;
        public long periodicIntervalMillis = 0;

        public String toString() {
            if (this.backoffExponentialMillis > 0) {
                return String.format("OperationScheduler.Options[backoff=%.1f+%.1f+%.1f max=%.1f min=%.1f period=%.1f]", Double.valueOf(this.backoffFixedMillis / 1000.0d), Double.valueOf(this.backoffIncrementalMillis / 1000.0d), Double.valueOf(((double) this.backoffExponentialMillis) / 1000.0d), Double.valueOf(this.maxMoratoriumMillis / 1000.0d), Double.valueOf(this.minTriggerMillis / 1000.0d), Double.valueOf(this.periodicIntervalMillis / 1000.0d));
            }
            return String.format("OperationScheduler.Options[backoff=%.1f+%.1f max=%.1f min=%.1f period=%.1f]", Double.valueOf(this.backoffFixedMillis / 1000.0d), Double.valueOf(this.backoffIncrementalMillis / 1000.0d), Double.valueOf(this.maxMoratoriumMillis / 1000.0d), Double.valueOf(this.minTriggerMillis / 1000.0d), Double.valueOf(this.periodicIntervalMillis / 1000.0d));
        }
    }

    public OperationScheduler(SharedPreferences sharedPreferences) {
        this.mStorage = sharedPreferences;
    }

    public static Options parseOptions(String str, Options options) throws IllegalArgumentException {
        for (String str2 : str.split(" +")) {
            if (str2.length() != 0) {
                if (str2.startsWith("backoff=")) {
                    String[] strArrSplit = str2.substring(8).split("\\+");
                    if (strArrSplit.length > 3) {
                        throw new IllegalArgumentException("bad value for backoff: [" + str + "]");
                    }
                    if (strArrSplit.length > 0 && strArrSplit[0].length() > 0) {
                        options.backoffFixedMillis = parseSeconds(strArrSplit[0]);
                    }
                    if (strArrSplit.length > 1 && strArrSplit[1].length() > 0) {
                        options.backoffIncrementalMillis = parseSeconds(strArrSplit[1]);
                    }
                    if (strArrSplit.length > 2 && strArrSplit[2].length() > 0) {
                        options.backoffExponentialMillis = (int) parseSeconds(strArrSplit[2]);
                    }
                } else if (str2.startsWith("max=")) {
                    options.maxMoratoriumMillis = parseSeconds(str2.substring(4));
                } else if (str2.startsWith("min=")) {
                    options.minTriggerMillis = parseSeconds(str2.substring(4));
                } else if (str2.startsWith("period=")) {
                    options.periodicIntervalMillis = parseSeconds(str2.substring(7));
                } else {
                    options.periodicIntervalMillis = parseSeconds(str2);
                }
            }
        }
        return options;
    }

    private static long parseSeconds(String str) throws NumberFormatException {
        return (long) (Float.parseFloat(str) * 1000.0f);
    }

    public long getNextTimeMillis(Options options) {
        if (!this.mStorage.getBoolean("OperationScheduler_enabledState", true) || this.mStorage.getBoolean("OperationScheduler_permanentError", false)) {
            return Long.MAX_VALUE;
        }
        int i = this.mStorage.getInt("OperationScheduler_errorCount", 0);
        long jCurrentTimeMillis = currentTimeMillis();
        long timeBefore = getTimeBefore("OperationScheduler_lastSuccessTimeMillis", jCurrentTimeMillis);
        long timeBefore2 = getTimeBefore("OperationScheduler_lastErrorTimeMillis", jCurrentTimeMillis);
        long jMin = this.mStorage.getLong("OperationScheduler_triggerTimeMillis", Long.MAX_VALUE);
        long timeBefore3 = getTimeBefore("OperationScheduler_moratoriumTimeMillis", getTimeBefore("OperationScheduler_moratoriumSetTimeMillis", jCurrentTimeMillis) + options.maxMoratoriumMillis);
        if (options.periodicIntervalMillis > 0) {
            jMin = Math.min(jMin, options.periodicIntervalMillis + timeBefore);
        }
        long jMax = Math.max(Math.max(jMin, timeBefore3), timeBefore + options.minTriggerMillis);
        if (i > 0) {
            int i2 = i - 1;
            if (i2 > 30) {
                i2 = 30;
            }
            return Math.max(jMax, timeBefore2 + Math.min(options.backoffFixedMillis + (options.backoffIncrementalMillis * ((long) i)) + (((long) options.backoffExponentialMillis) << i2), options.maxMoratoriumMillis));
        }
        return jMax;
    }

    public long getLastSuccessTimeMillis() {
        return this.mStorage.getLong("OperationScheduler_lastSuccessTimeMillis", 0L);
    }

    public long getLastAttemptTimeMillis() {
        return Math.max(this.mStorage.getLong("OperationScheduler_lastSuccessTimeMillis", 0L), this.mStorage.getLong("OperationScheduler_lastErrorTimeMillis", 0L));
    }

    private long getTimeBefore(String str, long j) {
        long j2 = this.mStorage.getLong(str, 0L);
        if (j2 > j) {
            SharedPreferencesCompat.apply(this.mStorage.edit().putLong(str, j));
            return j;
        }
        return j2;
    }

    public void setTriggerTimeMillis(long j) {
        SharedPreferencesCompat.apply(this.mStorage.edit().putLong("OperationScheduler_triggerTimeMillis", j));
    }

    public void setMoratoriumTimeMillis(long j) {
        SharedPreferencesCompat.apply(this.mStorage.edit().putLong("OperationScheduler_moratoriumTimeMillis", j).putLong("OperationScheduler_moratoriumSetTimeMillis", currentTimeMillis()));
    }

    public boolean setMoratoriumTimeHttp(String str) {
        try {
            setMoratoriumTimeMillis((Long.parseLong(str) * 1000) + currentTimeMillis());
            return true;
        } catch (NumberFormatException e) {
            try {
                setMoratoriumTimeMillis(LegacyHttpDateTime.parse(str));
                return true;
            } catch (IllegalArgumentException e2) {
                return false;
            }
        }
    }

    public void setEnabledState(boolean z) {
        SharedPreferencesCompat.apply(this.mStorage.edit().putBoolean("OperationScheduler_enabledState", z));
    }

    public void onSuccess() {
        resetTransientError();
        resetPermanentError();
        SharedPreferencesCompat.apply(this.mStorage.edit().remove("OperationScheduler_errorCount").remove("OperationScheduler_lastErrorTimeMillis").remove("OperationScheduler_permanentError").remove("OperationScheduler_triggerTimeMillis").putLong("OperationScheduler_lastSuccessTimeMillis", currentTimeMillis()));
    }

    public void onTransientError() {
        SharedPreferences.Editor editorEdit = this.mStorage.edit();
        editorEdit.putLong("OperationScheduler_lastErrorTimeMillis", currentTimeMillis());
        editorEdit.putInt("OperationScheduler_errorCount", this.mStorage.getInt("OperationScheduler_errorCount", 0) + 1);
        SharedPreferencesCompat.apply(editorEdit);
    }

    public void resetTransientError() {
        SharedPreferencesCompat.apply(this.mStorage.edit().remove("OperationScheduler_errorCount"));
    }

    public void onPermanentError() {
        SharedPreferencesCompat.apply(this.mStorage.edit().putBoolean("OperationScheduler_permanentError", true));
    }

    public void resetPermanentError() {
        SharedPreferencesCompat.apply(this.mStorage.edit().remove("OperationScheduler_permanentError"));
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("[OperationScheduler:");
        for (Map.Entry entry : new TreeMap(this.mStorage.getAll()).entrySet()) {
            String str = (String) entry.getKey();
            if (str.startsWith(PREFIX)) {
                if (str.endsWith("TimeMillis")) {
                    Time time = new Time();
                    time.set(((Long) entry.getValue()).longValue());
                    sb.append(" ");
                    sb.append(str.substring(PREFIX.length(), str.length() - 10));
                    sb.append("=");
                    sb.append(time.format("%Y-%m-%d/%H:%M:%S"));
                } else {
                    sb.append(" ");
                    sb.append(str.substring(PREFIX.length()));
                    Object value = entry.getValue();
                    if (value == null) {
                        sb.append("=(null)");
                    } else {
                        sb.append("=");
                        sb.append(value.toString());
                    }
                }
            }
        }
        sb.append("]");
        return sb.toString();
    }

    protected long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
