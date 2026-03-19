package com.android.settingslib.suggestions;

import android.content.Context;
import android.service.settings.suggestions.Suggestion;
import android.util.Log;
import com.android.settingslib.utils.AsyncLoader;
import java.util.List;

public class SuggestionLoader extends AsyncLoader<List<Suggestion>> {
    private final SuggestionController mSuggestionController;

    public SuggestionLoader(Context context, SuggestionController suggestionController) {
        super(context);
        this.mSuggestionController = suggestionController;
    }

    @Override
    protected void onDiscardResult(List<Suggestion> list) {
    }

    @Override
    public List<Suggestion> loadInBackground() {
        List<Suggestion> suggestions = this.mSuggestionController.getSuggestions();
        if (suggestions == null) {
            Log.d("SuggestionLoader", "data is null");
        } else {
            Log.d("SuggestionLoader", "data size " + suggestions.size());
        }
        return suggestions;
    }
}
