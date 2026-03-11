package com.android.quicksearchbox;

import android.content.ComponentName;

public abstract class AbstractSuggestionWrapper implements Suggestion {
    protected abstract Suggestion current();

    @Override
    public SuggestionExtras getExtras() {
        return current().getExtras();
    }

    @Override
    public String getShortcutId() {
        return current().getShortcutId();
    }

    @Override
    public String getSuggestionFormat() {
        return current().getSuggestionFormat();
    }

    @Override
    public String getSuggestionIcon1() {
        return current().getSuggestionIcon1();
    }

    @Override
    public String getSuggestionIcon2() {
        return current().getSuggestionIcon2();
    }

    @Override
    public String getSuggestionIntentAction() {
        return current().getSuggestionIntentAction();
    }

    @Override
    public ComponentName getSuggestionIntentComponent() {
        return current().getSuggestionIntentComponent();
    }

    @Override
    public String getSuggestionIntentDataString() {
        return current().getSuggestionIntentDataString();
    }

    @Override
    public String getSuggestionIntentExtraData() {
        return current().getSuggestionIntentExtraData();
    }

    @Override
    public String getSuggestionLogType() {
        return current().getSuggestionLogType();
    }

    @Override
    public String getSuggestionQuery() {
        return current().getSuggestionQuery();
    }

    @Override
    public Source getSuggestionSource() {
        return current().getSuggestionSource();
    }

    @Override
    public String getSuggestionText1() {
        return current().getSuggestionText1();
    }

    @Override
    public String getSuggestionText2() {
        return current().getSuggestionText2();
    }

    @Override
    public String getSuggestionText2Url() {
        return current().getSuggestionText2Url();
    }

    @Override
    public boolean isHistorySuggestion() {
        return current().isHistorySuggestion();
    }

    @Override
    public boolean isSpinnerWhileRefreshing() {
        return current().isSpinnerWhileRefreshing();
    }

    @Override
    public boolean isSuggestionShortcut() {
        return current().isSuggestionShortcut();
    }

    @Override
    public boolean isWebSearchSuggestion() {
        return current().isWebSearchSuggestion();
    }
}
