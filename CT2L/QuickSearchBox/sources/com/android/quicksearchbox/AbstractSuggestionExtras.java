package com.android.quicksearchbox;

public abstract class AbstractSuggestionExtras implements SuggestionExtras {
    private final SuggestionExtras mMore;

    protected abstract String doGetExtra(String str);

    protected AbstractSuggestionExtras(SuggestionExtras more) {
        this.mMore = more;
    }

    @Override
    public String getExtra(String columnName) {
        String extra = doGetExtra(columnName);
        if (extra == null && this.mMore != null) {
            return this.mMore.getExtra(columnName);
        }
        return extra;
    }
}
