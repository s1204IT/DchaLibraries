package android.view.textclassifier;

import android.metrics.LogMaker;
import android.util.ArrayMap;
import android.view.textclassifier.TextLinks;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.util.Preconditions;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Function;

@VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
public final class GenerateLinksLogger {
    private static final boolean DEBUG_LOG_ENABLED = false;
    private static final String LOG_TAG = "GenerateLinksLogger";
    private static final String ZERO = "0";
    private final MetricsLogger mMetricsLogger;
    private final Random mRng;
    private final int mSampleRate;

    public GenerateLinksLogger(int i) {
        this.mSampleRate = i;
        this.mRng = new Random(System.nanoTime());
        this.mMetricsLogger = new MetricsLogger();
    }

    @VisibleForTesting
    public GenerateLinksLogger(int i, MetricsLogger metricsLogger) {
        this.mSampleRate = i;
        this.mRng = new Random(System.nanoTime());
        this.mMetricsLogger = metricsLogger;
    }

    public void logGenerateLinks(CharSequence charSequence, TextLinks textLinks, String str, long j) {
        String entity;
        Preconditions.checkNotNull(charSequence);
        Preconditions.checkNotNull(textLinks);
        Preconditions.checkNotNull(str);
        if (!shouldLog()) {
            return;
        }
        LinkifyStats linkifyStats = new LinkifyStats();
        ArrayMap arrayMap = new ArrayMap();
        for (TextLinks.TextLink textLink : textLinks.getLinks()) {
            if (textLink.getEntityCount() != 0 && (entity = textLink.getEntity(0)) != null && !"other".equals(entity) && !"".equals(entity)) {
                linkifyStats.countLink(textLink);
                ((LinkifyStats) arrayMap.computeIfAbsent(entity, new Function() {
                    @Override
                    public final Object apply(Object obj) {
                        return GenerateLinksLogger.lambda$logGenerateLinks$0((String) obj);
                    }
                })).countLink(textLink);
            }
        }
        String string = UUID.randomUUID().toString();
        writeStats(string, str, null, linkifyStats, charSequence, j);
        Iterator it = arrayMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            writeStats(string, str, (String) entry.getKey(), (LinkifyStats) entry.getValue(), charSequence, j);
        }
    }

    static LinkifyStats lambda$logGenerateLinks$0(String str) {
        return new LinkifyStats();
    }

    private boolean shouldLog() {
        return this.mSampleRate <= 1 || this.mRng.nextInt(this.mSampleRate) == 0;
    }

    private void writeStats(String str, String str2, String str3, LinkifyStats linkifyStats, CharSequence charSequence, long j) {
        LogMaker logMakerAddTaggedData = new LogMaker(1313).setPackageName(str2).addTaggedData(1319, str).addTaggedData(1316, Integer.valueOf(linkifyStats.mNumLinks)).addTaggedData(1317, Integer.valueOf(linkifyStats.mNumLinksTextLength)).addTaggedData(1315, Integer.valueOf(charSequence.length())).addTaggedData(1314, Long.valueOf(j));
        if (str3 != null) {
            logMakerAddTaggedData.addTaggedData(1318, str3);
        }
        this.mMetricsLogger.write(logMakerAddTaggedData);
        debugLog(logMakerAddTaggedData);
    }

    private static void debugLog(LogMaker logMaker) {
    }

    private static final class LinkifyStats {
        int mNumLinks;
        int mNumLinksTextLength;

        private LinkifyStats() {
        }

        void countLink(TextLinks.TextLink textLink) {
            this.mNumLinks++;
            this.mNumLinksTextLength += textLink.getEnd() - textLink.getStart();
        }
    }
}
