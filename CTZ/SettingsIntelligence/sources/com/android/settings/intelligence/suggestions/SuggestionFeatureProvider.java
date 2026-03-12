package com.android.settings.intelligence.suggestions;

import android.content.Context;
import android.content.SharedPreferences;
import android.service.settings.suggestions.Suggestion;
import com.android.settings.intelligence.suggestions.ranking.SuggestionEventStore;
import com.android.settings.intelligence.suggestions.ranking.SuggestionFeaturizer;
import com.android.settings.intelligence.suggestions.ranking.SuggestionRanker;
import java.util.Iterator;
import java.util.List;

public class SuggestionFeatureProvider {
    private SuggestionRanker mRanker;

    public SharedPreferences getSharedPrefs(Context context) {
        return context.getApplicationContext().getSharedPreferences("suggestions", 0);
    }

    public List<Suggestion> getSuggestions(Context context) {
        List<Suggestion> listRankRelevantSuggestions = getRanker(context).rankRelevantSuggestions(new SuggestionParser(context).getSuggestions());
        SuggestionEventStore suggestionEventStore = SuggestionEventStore.get(context);
        Iterator<Suggestion> it = listRankRelevantSuggestions.iterator();
        while (it.hasNext()) {
            suggestionEventStore.writeEvent(it.next().getId(), "shown");
        }
        return listRankRelevantSuggestions;
    }

    public void markSuggestionDismissed(Context context, String str) {
        getSharedPrefs(context).edit().putBoolean(getDismissKey(str), true).apply();
        SuggestionEventStore.get(context).writeEvent(str, "dismissed");
    }

    public void markSuggestionNotDismissed(Context context, String str) {
        getSharedPrefs(context).edit().putBoolean(getDismissKey(str), false).apply();
    }

    public void markSuggestionLaunched(Context context, String str) {
        SuggestionEventStore.get(context).writeEvent(str, "clicked");
    }

    public boolean isSuggestionDismissed(Context context, String str) {
        return getSharedPrefs(context).getBoolean(getDismissKey(str), false);
    }

    protected SuggestionRanker getRanker(Context context) {
        if (this.mRanker == null) {
            this.mRanker = new SuggestionRanker(context, new SuggestionFeaturizer(context.getApplicationContext()));
        }
        return this.mRanker;
    }

    private static String getDismissKey(String str) {
        return str + "_is_dismissed";
    }
}
