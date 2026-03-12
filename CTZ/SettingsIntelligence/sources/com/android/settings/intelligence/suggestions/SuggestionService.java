package com.android.settings.intelligence.suggestions;

import android.service.settings.suggestions.Suggestion;
import android.util.Log;
import com.android.settings.intelligence.overlay.FeatureFactory;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SuggestionService extends android.service.settings.suggestions.SuggestionService {
    public List<Suggestion> onGetSuggestions() {
        long jCurrentTimeMillis = System.currentTimeMillis();
        List<Suggestion> suggestions = FeatureFactory.get(this).suggestionFeatureProvider().getSuggestions(this);
        ArrayList arrayList = new ArrayList(suggestions.size());
        Iterator<Suggestion> it = suggestions.iterator();
        while (it.hasNext()) {
            arrayList.add(it.next().getId());
        }
        FeatureFactory.get(this).metricsFeatureProvider(this).logGetSuggestion(arrayList, System.currentTimeMillis() - jCurrentTimeMillis);
        return suggestions;
    }

    public void onSuggestionDismissed(Suggestion suggestion) {
        long jCurrentTimeMillis = System.currentTimeMillis();
        String id = suggestion.getId();
        Log.d("SuggestionService", "dismissing suggestion " + id);
        long jCurrentTimeMillis2 = System.currentTimeMillis();
        FeatureFactory.get(this).suggestionFeatureProvider().markSuggestionDismissed(this, id);
        FeatureFactory.get(this).metricsFeatureProvider(this).logDismissSuggestion(id, jCurrentTimeMillis2 - jCurrentTimeMillis);
    }

    public void onSuggestionLaunched(Suggestion suggestion) {
        long jCurrentTimeMillis = System.currentTimeMillis();
        String id = suggestion.getId();
        Log.d("SuggestionService", "Suggestion is launched" + id);
        long jCurrentTimeMillis2 = System.currentTimeMillis();
        FeatureFactory.get(this).suggestionFeatureProvider().markSuggestionLaunched(this, id);
        FeatureFactory.get(this).metricsFeatureProvider(this).logLaunchSuggestion(id, jCurrentTimeMillis2 - jCurrentTimeMillis);
    }
}
