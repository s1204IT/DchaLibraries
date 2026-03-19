package com.android.server.backup.transport;

import android.content.ComponentName;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.backup.transport.TransportStats;
import com.android.server.job.controllers.JobStatus;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BinaryOperator;

public class TransportStats {
    private final Object mStatsLock = new Object();
    private final Map<ComponentName, Stats> mTransportStats = new HashMap();

    void registerConnectionTime(ComponentName componentName, long j) {
        synchronized (this.mStatsLock) {
            Stats stats = this.mTransportStats.get(componentName);
            if (stats == null) {
                stats = new Stats();
                this.mTransportStats.put(componentName, stats);
            }
            stats.register(j);
        }
    }

    public Stats getStatsForTransport(ComponentName componentName) {
        synchronized (this.mStatsLock) {
            Stats stats = this.mTransportStats.get(componentName);
            if (stats == null) {
                return null;
            }
            return new Stats(stats);
        }
    }

    public void dump(PrintWriter printWriter) {
        synchronized (this.mStatsLock) {
            Optional<Stats> optionalReduce = this.mTransportStats.values().stream().reduce(new BinaryOperator() {
                @Override
                public final Object apply(Object obj, Object obj2) {
                    return TransportStats.Stats.merge((TransportStats.Stats) obj, (TransportStats.Stats) obj2);
                }
            });
            if (optionalReduce.isPresent()) {
                dumpStats(printWriter, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, optionalReduce.get());
            }
            if (!this.mTransportStats.isEmpty()) {
                printWriter.println("Per transport:");
                for (ComponentName componentName : this.mTransportStats.keySet()) {
                    Stats stats = this.mTransportStats.get(componentName);
                    printWriter.println("    " + componentName.flattenToShortString());
                    dumpStats(printWriter, "        ", stats);
                }
            }
        }
    }

    private static void dumpStats(PrintWriter printWriter, String str, Stats stats) {
        printWriter.println(String.format(Locale.US, "%sAverage connection time: %.2f ms", str, Double.valueOf(stats.average)));
        printWriter.println(String.format(Locale.US, "%sMax connection time: %d ms", str, Long.valueOf(stats.max)));
        printWriter.println(String.format(Locale.US, "%sMin connection time: %d ms", str, Long.valueOf(stats.min)));
        printWriter.println(String.format(Locale.US, "%sNumber of connections: %d ", str, Integer.valueOf(stats.n)));
    }

    public static final class Stats {
        public double average;
        public long max;
        public long min;
        public int n;

        public static Stats merge(Stats stats, Stats stats2) {
            return new Stats(stats2.n + stats.n, ((stats.average * ((double) stats.n)) + (stats2.average * ((double) stats2.n))) / ((double) (stats.n + stats2.n)), Math.max(stats.max, stats2.max), Math.min(stats.min, stats2.min));
        }

        public Stats() {
            this.n = 0;
            this.average = 0.0d;
            this.max = 0L;
            this.min = JobStatus.NO_LATEST_RUNTIME;
        }

        private Stats(int i, double d, long j, long j2) {
            this.n = i;
            this.average = d;
            this.max = j;
            this.min = j2;
        }

        private Stats(Stats stats) {
            this(stats.n, stats.average, stats.max, stats.min);
        }

        private void register(long j) {
            this.average = ((this.average * ((double) this.n)) + j) / ((double) (this.n + 1));
            this.n++;
            this.max = Math.max(this.max, j);
            this.min = Math.min(this.min, j);
        }
    }
}
