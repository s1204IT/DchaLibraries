package com.android.settings.intelligence.suggestions.ranking;

import android.content.Context;
import android.service.settings.suggestions.Suggestion;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SuggestionFeaturizer {
    private final SuggestionEventStore mEventStore;

    public SuggestionFeaturizer(Context context) {
        this.mEventStore = SuggestionEventStore.get(context);
    }

    public Map<String, Map<String, Double>> featurize(List<Suggestion> list) {
        HashMap map = new HashMap();
        Long lValueOf = Long.valueOf(System.currentTimeMillis());
        ArrayList<String> arrayList = new ArrayList(list.size());
        Iterator<Suggestion> it = list.iterator();
        while (it.hasNext()) {
            arrayList.add(it.next().getId());
        }
        for (String str : arrayList) {
            HashMap map2 = new HashMap();
            map.put(str, map2);
            Long lValueOf2 = Long.valueOf(this.mEventStore.readMetric(str, "shown", "last_event_time"));
            Long lValueOf3 = Long.valueOf(this.mEventStore.readMetric(str, "dismissed", "last_event_time"));
            Long lValueOf4 = Long.valueOf(this.mEventStore.readMetric(str, "clicked", "last_event_time"));
            boolean z = false;
            map2.put("is_shown", Double.valueOf(booleanToDouble(lValueOf2.longValue() > 0)));
            map2.put("is_dismissed", Double.valueOf(booleanToDouble(lValueOf3.longValue() > 0)));
            if (lValueOf4.longValue() > 0) {
                z = true;
            }
            map2.put("is_clicked", Double.valueOf(booleanToDouble(z)));
            map2.put("time_from_last_shown", Double.valueOf(normalizedTimeDiff(lValueOf.longValue(), lValueOf2.longValue())));
            map2.put("time_from_last_dismissed", Double.valueOf(normalizedTimeDiff(lValueOf.longValue(), lValueOf3.longValue())));
            map2.put("time_from_last_clicked", Double.valueOf(normalizedTimeDiff(lValueOf.longValue(), lValueOf4.longValue())));
            map2.put("shown_count", Double.valueOf(normalizedCount(this.mEventStore.readMetric(str, "shown", "count"))));
            map2.put("dismissed_count", Double.valueOf(normalizedCount(this.mEventStore.readMetric(str, "dismissed", "count"))));
            map2.put("clicked_count", Double.valueOf(normalizedCount(this.mEventStore.readMetric(str, "clicked", "count"))));
        }
        return map;
    }

    private static double booleanToDouble(boolean z) {
        return z ? 1.0d : 0.0d;
    }

    private static double normalizedTimeDiff(long j, long j2) {
        return Math.min(1.0d, (j - j2) / 2.0E10d);
    }

    private static double normalizedCount(long j) {
        return Math.min(1.0d, j / 500.0d);
    }
}
