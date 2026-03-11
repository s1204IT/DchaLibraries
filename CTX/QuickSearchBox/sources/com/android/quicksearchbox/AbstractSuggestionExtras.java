package com.android.quicksearchbox;

public abstract class AbstractSuggestionExtras implements SuggestionExtras {
    private final SuggestionExtras mMore;

    protected AbstractSuggestionExtras(SuggestionExtras suggestionExtras) {
        this.mMore = suggestionExtras;
    }

    protected abstract String doGetExtra(String str);

    @Override
    public String getExtra(String str) {
        String strDoGetExtra = doGetExtra(str);
        return (strDoGetExtra != null || this.mMore == null) ? strDoGetExtra : this.mMore.getExtra(str);
    }
}
