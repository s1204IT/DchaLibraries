package com.android.quicksearchbox;

import android.text.Spannable;

public abstract class SuggestionFormatter {
    private final TextAppearanceFactory mSpanFactory;

    protected SuggestionFormatter(TextAppearanceFactory textAppearanceFactory) {
        this.mSpanFactory = textAppearanceFactory;
    }

    private void setSpans(Spannable spannable, int i, int i2, Object[] objArr) {
        for (Object obj : objArr) {
            spannable.setSpan(obj, i, i2, 0);
        }
    }

    protected void applyQueryTextStyle(Spannable spannable, int i, int i2) {
        if (i == i2) {
            return;
        }
        setSpans(spannable, i, i2, this.mSpanFactory.createSuggestionQueryTextAppearance());
    }

    protected void applySuggestedTextStyle(Spannable spannable, int i, int i2) {
        if (i == i2) {
            return;
        }
        setSpans(spannable, i, i2, this.mSpanFactory.createSuggestionSuggestedTextAppearance());
    }

    public abstract CharSequence formatSuggestion(String str, String str2);
}
