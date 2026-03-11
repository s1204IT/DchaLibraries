package com.android.quicksearchbox;

import android.content.ComponentName;

public interface Suggestion {
    SuggestionExtras getExtras();

    String getShortcutId();

    String getSuggestionFormat();

    String getSuggestionIcon1();

    String getSuggestionIcon2();

    String getSuggestionIntentAction();

    ComponentName getSuggestionIntentComponent();

    String getSuggestionIntentDataString();

    String getSuggestionIntentExtraData();

    String getSuggestionLogType();

    String getSuggestionQuery();

    Source getSuggestionSource();

    String getSuggestionText1();

    String getSuggestionText2();

    String getSuggestionText2Url();

    boolean isHistorySuggestion();

    boolean isSpinnerWhileRefreshing();

    boolean isSuggestionShortcut();

    boolean isWebSearchSuggestion();
}
