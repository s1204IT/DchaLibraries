package com.android.settings.intelligence.suggestions.ranking;

import android.content.Context;
import android.service.settings.suggestions.Suggestion;
import com.android.settings.intelligence.overlay.FeatureFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SuggestionRanker {
    private static final Map<String, Double> WEIGHTS = new HashMap<String, Double>() {
        {
            put("is_shown", Double.valueOf(5.05140842519d));
            put("is_dismissed", Double.valueOf(2.29641455171d));
            put("is_clicked", Double.valueOf(-2.98812233623d));
            put("time_from_last_shown", Double.valueOf(5.02807250202d));
            put("time_from_last_dismissed", Double.valueOf(2.49589700842d));
            put("time_from_last_clicked", Double.valueOf(-4.3377039948d));
            put("shown_count", Double.valueOf(-2.35993512546d));
        }
    };
    private final long mMaxSuggestionsDisplayCount;
    private final SuggestionFeaturizer mSuggestionFeaturizer;
    Comparator<Suggestion> suggestionComparator = new Comparator<Suggestion>() {
        @Override
        public int compare(Suggestion suggestion, Suggestion suggestion2) {
            return ((Double) SuggestionRanker.this.mRelevanceMetrics.get(suggestion)).doubleValue() < ((Double) SuggestionRanker.this.mRelevanceMetrics.get(suggestion2)).doubleValue() ? 1 : -1;
        }
    };
    private final Map<Suggestion, Double> mRelevanceMetrics = new HashMap();

    public SuggestionRanker(Context context, SuggestionFeaturizer suggestionFeaturizer) {
        this.mSuggestionFeaturizer = suggestionFeaturizer;
        this.mMaxSuggestionsDisplayCount = FeatureFactory.get(context).experimentFeatureProvider().getMaxSuggestionDisplayCount(context);
    }

    public List<Suggestion> rankRelevantSuggestions(List<Suggestion> list) {
        this.mRelevanceMetrics.clear();
        Map<String, Map<String, Double>> mapFeaturize = this.mSuggestionFeaturizer.featurize(list);
        for (Suggestion suggestion : list) {
            this.mRelevanceMetrics.put(suggestion, Double.valueOf(getRelevanceMetric(mapFeaturize.get(suggestion.getId()))));
        }
        ArrayList arrayList = new ArrayList();
        arrayList.addAll(list);
        Collections.sort(arrayList, this.suggestionComparator);
        if (arrayList.size() < this.mMaxSuggestionsDisplayCount) {
            return arrayList;
        }
        ArrayList arrayList2 = new ArrayList();
        for (int i = 0; i < this.mMaxSuggestionsDisplayCount; i++) {
            arrayList2.add((Suggestion) arrayList.get(i));
        }
        return arrayList2;
    }

    double getRelevanceMetric(Map<String, Double> map) {
        double dDoubleValue = 0.0d;
        if (map == null) {
            return 0.0d;
        }
        for (String str : WEIGHTS.keySet()) {
            dDoubleValue += WEIGHTS.get(str).doubleValue() * map.get(str).doubleValue();
        }
        return dDoubleValue;
    }
}
