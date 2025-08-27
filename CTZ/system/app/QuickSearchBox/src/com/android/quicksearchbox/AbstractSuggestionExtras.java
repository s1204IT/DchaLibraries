package com.android.quicksearchbox;

/* loaded from: classes.dex */
public abstract class AbstractSuggestionExtras implements SuggestionExtras {
    private final SuggestionExtras mMore;

    protected abstract String doGetExtra(String str);

    protected AbstractSuggestionExtras(SuggestionExtras suggestionExtras) {
        this.mMore = suggestionExtras;
    }

    @Override // com.android.quicksearchbox.SuggestionExtras
    public String getExtra(String str) {
        String strDoGetExtra = doGetExtra(str);
        if (strDoGetExtra == null && this.mMore != null) {
            return this.mMore.getExtra(str);
        }
        return strDoGetExtra;
    }
}
