package com.android.server.net.watchlist;

import android.util.Log;
import android.util.proto.ProtoOutputStream;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.HexDump;
import com.android.server.net.watchlist.WatchlistReportDbHelper;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

class ReportEncoder {
    private static final int REPORT_VERSION = 1;
    private static final String TAG = "ReportEncoder";
    private static final int WATCHLIST_HASH_SIZE = 32;

    ReportEncoder() {
    }

    static byte[] encodeWatchlistReport(WatchlistConfig watchlistConfig, byte[] bArr, List<String> list, WatchlistReportDbHelper.AggregatedResult aggregatedResult) {
        return serializeReport(watchlistConfig, PrivacyUtils.createDpEncodedReportMap(watchlistConfig.isConfigSecure(), bArr, list, aggregatedResult));
    }

    @VisibleForTesting
    static byte[] serializeReport(WatchlistConfig watchlistConfig, Map<String, Boolean> map) {
        byte[] watchlistConfigHash = watchlistConfig.getWatchlistConfigHash();
        if (watchlistConfigHash == null) {
            Log.e(TAG, "No watchlist hash");
            return null;
        }
        if (watchlistConfigHash.length != 32) {
            Log.e(TAG, "Unexpected hash length");
            return null;
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ProtoOutputStream protoOutputStream = new ProtoOutputStream(byteArrayOutputStream);
        protoOutputStream.write(1120986464257L, 1);
        protoOutputStream.write(1138166333442L, HexDump.toHexString(watchlistConfigHash));
        for (Map.Entry<String, Boolean> entry : map.entrySet()) {
            String key = entry.getKey();
            HexDump.hexStringToByteArray(key);
            boolean zBooleanValue = entry.getValue().booleanValue();
            long jStart = protoOutputStream.start(2246267895811L);
            protoOutputStream.write(1138166333441L, key);
            protoOutputStream.write(1133871366146L, zBooleanValue);
            protoOutputStream.end(jStart);
        }
        protoOutputStream.flush();
        return byteArrayOutputStream.toByteArray();
    }
}
