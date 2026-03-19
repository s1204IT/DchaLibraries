package com.android.bluetooth.btservice;

import com.android.bluetooth.BluetoothMetricsProto;
import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class MetricsLogger {
    private static final HashMap<BluetoothMetricsProto.ProfileId, Integer> sProfileConnectionCounts = new HashMap<>();

    public static void logProfileConnectionEvent(BluetoothMetricsProto.ProfileId profileId) {
        synchronized (sProfileConnectionCounts) {
            sProfileConnectionCounts.merge(profileId, 1, new BiFunction() {
                @Override
                public final Object apply(Object obj, Object obj2) {
                    return Integer.valueOf(Integer.sum(((Integer) obj).intValue(), ((Integer) obj2).intValue()));
                }
            });
        }
    }

    public static void dumpProto(final BluetoothMetricsProto.BluetoothLog.Builder builder) {
        synchronized (sProfileConnectionCounts) {
            sProfileConnectionCounts.forEach(new BiConsumer() {
                @Override
                public final void accept(Object obj, Object obj2) {
                    builder.addProfileConnectionStats(BluetoothMetricsProto.ProfileConnectionStats.newBuilder().setProfileId((BluetoothMetricsProto.ProfileId) obj).setNumTimesConnected(((Integer) obj2).intValue()).build());
                }
            });
            sProfileConnectionCounts.clear();
        }
    }
}
