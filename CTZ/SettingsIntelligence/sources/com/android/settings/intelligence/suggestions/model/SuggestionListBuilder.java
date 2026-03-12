package com.android.settings.intelligence.suggestions.model;

import android.service.settings.suggestions.Suggestion;
import android.util.ArraySet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SuggestionListBuilder {
    private boolean isBuilt;
    private Map<SuggestionCategory, List<Suggestion>> mSuggestions = new HashMap();

    public void addSuggestions(SuggestionCategory suggestionCategory, List<Suggestion> list) {
        if (this.isBuilt) {
            throw new IllegalStateException("Already built suggestion list, cannot add new ones");
        }
        this.mSuggestions.put(suggestionCategory, list);
    }

    public List<Suggestion> build() {
        this.isBuilt = true;
        return dedupeSuggestions();
    }

    private List<Suggestion> dedupeSuggestions() {
        ArraySet arraySet = new ArraySet();
        ArrayList arrayList = new ArrayList();
        Iterator<List<Suggestion>> it = this.mSuggestions.values().iterator();
        while (it.hasNext()) {
            arrayList.addAll(it.next());
        }
        for (int size = arrayList.size() - 1; size >= 0; size--) {
            String id = ((Suggestion) arrayList.get(size)).getId();
            if (arraySet.contains(id)) {
                arrayList.remove(size);
            } else {
                arraySet.add(id);
            }
        }
        return arrayList;
    }
}
