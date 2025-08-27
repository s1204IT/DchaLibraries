package com.android.quicksearchbox;

import android.content.ComponentName;

/* loaded from: classes.dex */
public abstract class AbstractSuggestionWrapper implements Suggestion {
    protected abstract Suggestion current();

    @Override // com.android.quicksearchbox.Suggestion
    public String getShortcutId() {
        return current().getShortcutId();
    }

    @Override // com.android.quicksearchbox.Suggestion
    public String getSuggestionFormat() {
        return current().getSuggestionFormat();
    }

    @Override // com.android.quicksearchbox.Suggestion
    public String getSuggestionIcon1() {
        return current().getSuggestionIcon1();
    }

    @Override // com.android.quicksearchbox.Suggestion
    public String getSuggestionIcon2() {
        return current().getSuggestionIcon2();
    }

    @Override // com.android.quicksearchbox.Suggestion
    public String getSuggestionIntentAction() {
        return current().getSuggestionIntentAction();
    }

    @Override // com.android.quicksearchbox.Suggestion
    public ComponentName getSuggestionIntentComponent() {
        return current().getSuggestionIntentComponent();
    }

    @Override // com.android.quicksearchbox.Suggestion
    public String getSuggestionIntentDataString() {
        return current().getSuggestionIntentDataString();
    }

    @Override // com.android.quicksearchbox.Suggestion
    public String getSuggestionIntentExtraData() {
        return current().getSuggestionIntentExtraData();
    }

    @Override // com.android.quicksearchbox.Suggestion
    public String getSuggestionLogType() {
        return current().getSuggestionLogType();
    }

    @Override // com.android.quicksearchbox.Suggestion
    public String getSuggestionQuery() {
        return current().getSuggestionQuery();
    }

    @Override // com.android.quicksearchbox.Suggestion
    public Source getSuggestionSource() {
        return current().getSuggestionSource();
    }

    @Override // com.android.quicksearchbox.Suggestion
    public String getSuggestionText1() {
        return current().getSuggestionText1();
    }

    @Override // com.android.quicksearchbox.Suggestion
    public String getSuggestionText2() {
        return current().getSuggestionText2();
    }

    @Override // com.android.quicksearchbox.Suggestion
    public String getSuggestionText2Url() {
        return current().getSuggestionText2Url();
    }

    @Override // com.android.quicksearchbox.Suggestion
    public boolean isSpinnerWhileRefreshing() {
        return current().isSpinnerWhileRefreshing();
    }

    @Override // com.android.quicksearchbox.Suggestion
    public boolean isSuggestionShortcut() {
        return current().isSuggestionShortcut();
    }

    @Override // com.android.quicksearchbox.Suggestion
    public boolean isWebSearchSuggestion() {
        return current().isWebSearchSuggestion();
    }

    @Override // com.android.quicksearchbox.Suggestion
    public boolean isHistorySuggestion() {
        return current().isHistorySuggestion();
    }

    @Override // com.android.quicksearchbox.Suggestion
    public SuggestionExtras getExtras() {
        return current().getExtras();
    }
}
