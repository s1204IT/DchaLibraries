package com.android.settings.intelligence.suggestions;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.service.settings.suggestions.Suggestion;
import android.util.ArrayMap;
import android.util.Log;
import com.android.settings.intelligence.overlay.FeatureFactory;
import com.android.settings.intelligence.suggestions.eligibility.CandidateSuggestionFilter;
import com.android.settings.intelligence.suggestions.model.CandidateSuggestion;
import com.android.settings.intelligence.suggestions.model.SuggestionCategory;
import com.android.settings.intelligence.suggestions.model.SuggestionCategoryRegistry;
import com.android.settings.intelligence.suggestions.model.SuggestionListBuilder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

class SuggestionParser {
    private final Map<String, Suggestion> mAddCache = new ArrayMap();
    private final Context mContext;
    private final PackageManager mPackageManager;
    private final SharedPreferences mSharedPrefs;

    public SuggestionParser(Context context) {
        this.mContext = context.getApplicationContext();
        this.mPackageManager = context.getPackageManager();
        this.mSharedPrefs = FeatureFactory.get(this.mContext).suggestionFeatureProvider().getSharedPrefs(this.mContext);
    }

    public List<Suggestion> getSuggestions() {
        SuggestionListBuilder suggestionListBuilder = new SuggestionListBuilder();
        for (SuggestionCategory suggestionCategory : SuggestionCategoryRegistry.CATEGORIES) {
            if (suggestionCategory.isExclusive() && !isExclusiveCategoryExpired(suggestionCategory)) {
                List<Suggestion> suggestions = readSuggestions(suggestionCategory, false);
                if (!suggestions.isEmpty()) {
                    suggestionListBuilder.addSuggestions(suggestionCategory, suggestions);
                    return suggestionListBuilder.build();
                }
            } else {
                suggestionListBuilder.addSuggestions(suggestionCategory, readSuggestions(suggestionCategory, true));
            }
        }
        return suggestionListBuilder.build();
    }

    List<Suggestion> readSuggestions(SuggestionCategory suggestionCategory, boolean z) {
        ArrayList arrayList = new ArrayList();
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory(suggestionCategory.getCategory());
        List<ResolveInfo> listQueryIntentActivities = this.mPackageManager.queryIntentActivities(intent, 128);
        ArrayList arrayList2 = new ArrayList();
        Iterator<ResolveInfo> it = listQueryIntentActivities.iterator();
        while (it.hasNext()) {
            CandidateSuggestion candidateSuggestion = new CandidateSuggestion(this.mContext, it.next(), z);
            if (candidateSuggestion.isEligible()) {
                arrayList2.add(candidateSuggestion);
            }
        }
        for (CandidateSuggestion candidateSuggestion2 : CandidateSuggestionFilter.getInstance().filterCandidates(this.mContext, arrayList2)) {
            String id = candidateSuggestion2.getId();
            Suggestion suggestion = this.mAddCache.get(id);
            if (suggestion == null) {
                suggestion = candidateSuggestion2.toSuggestion();
                this.mAddCache.put(id, suggestion);
            }
            if (!arrayList.contains(suggestion)) {
                arrayList.add(suggestion);
            }
        }
        return arrayList;
    }

    private boolean isExclusiveCategoryExpired(SuggestionCategory suggestionCategory) {
        String str = suggestionCategory.getCategory() + "_setup_time";
        long jCurrentTimeMillis = System.currentTimeMillis();
        if (!this.mSharedPrefs.contains(str)) {
            this.mSharedPrefs.edit().putLong(str, jCurrentTimeMillis).commit();
        }
        if (suggestionCategory.getExclusiveExpireDaysInMillis() < 0) {
            return false;
        }
        long j = jCurrentTimeMillis - this.mSharedPrefs.getLong(str, 0L);
        Log.d("SuggestionParser", "Day " + (j / 86400000) + " for " + suggestionCategory.getCategory());
        return j > suggestionCategory.getExclusiveExpireDaysInMillis();
    }
}
