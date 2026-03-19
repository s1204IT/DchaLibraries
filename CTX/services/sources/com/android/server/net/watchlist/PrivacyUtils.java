package com.android.server.net.watchlist;

import android.privacy.DifferentialPrivacyEncoder;
import android.privacy.internal.longitudinalreporting.LongitudinalReportingConfig;
import android.privacy.internal.longitudinalreporting.LongitudinalReportingEncoder;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.net.watchlist.WatchlistReportDbHelper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class PrivacyUtils {
    private static final boolean DEBUG = false;
    private static final String ENCODER_ID_PREFIX = "watchlist_encoder:";
    private static final double PROB_F = 0.469d;
    private static final double PROB_P = 0.28d;
    private static final double PROB_Q = 1.0d;
    private static final String TAG = "PrivacyUtils";

    private PrivacyUtils() {
    }

    @VisibleForTesting
    static DifferentialPrivacyEncoder createInsecureDPEncoderForTest(String str) {
        return LongitudinalReportingEncoder.createInsecureEncoderForTest(createLongitudinalReportingConfig(str));
    }

    @VisibleForTesting
    static DifferentialPrivacyEncoder createSecureDPEncoder(byte[] bArr, String str) {
        return LongitudinalReportingEncoder.createEncoder(createLongitudinalReportingConfig(str), bArr);
    }

    private static LongitudinalReportingConfig createLongitudinalReportingConfig(String str) {
        return new LongitudinalReportingConfig(ENCODER_ID_PREFIX + str, PROB_F, PROB_P, PROB_Q);
    }

    @VisibleForTesting
    static Map<String, Boolean> createDpEncodedReportMap(boolean z, byte[] bArr, List<String> list, WatchlistReportDbHelper.AggregatedResult aggregatedResult) {
        DifferentialPrivacyEncoder differentialPrivacyEncoderCreateInsecureDPEncoderForTest;
        int size = list.size();
        HashMap map = new HashMap(size);
        for (int i = 0; i < size; i++) {
            String str = list.get(i);
            if (z) {
                differentialPrivacyEncoderCreateInsecureDPEncoderForTest = createSecureDPEncoder(bArr, str);
            } else {
                differentialPrivacyEncoderCreateInsecureDPEncoderForTest = createInsecureDPEncoderForTest(str);
            }
            boolean z2 = true;
            if ((differentialPrivacyEncoderCreateInsecureDPEncoderForTest.encodeBoolean(aggregatedResult.appDigestList.contains(str))[0] & 1) != 1) {
                z2 = false;
            }
            map.put(str, Boolean.valueOf(z2));
        }
        return map;
    }
}
