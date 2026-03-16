package com.android.calendar;

import android.content.SearchRecentSuggestionsProvider;

public class CalendarRecentSuggestionsProvider extends SearchRecentSuggestionsProvider {
    @Override
    public boolean onCreate() {
        setupSuggestions(Utils.getSearchAuthority(getContext()), 1);
        return super.onCreate();
    }
}
