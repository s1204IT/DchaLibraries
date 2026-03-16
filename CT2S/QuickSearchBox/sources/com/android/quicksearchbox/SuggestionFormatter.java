package com.android.quicksearchbox;

import android.text.Spannable;

public abstract class SuggestionFormatter {
    private final TextAppearanceFactory mSpanFactory;

    public abstract CharSequence formatSuggestion(String str, String str2);

    protected SuggestionFormatter(TextAppearanceFactory spanFactory) {
        this.mSpanFactory = spanFactory;
    }

    protected void applyQueryTextStyle(Spannable text, int start, int end) {
        if (start != end) {
            setSpans(text, start, end, this.mSpanFactory.createSuggestionQueryTextAppearance());
        }
    }

    protected void applySuggestedTextStyle(Spannable text, int start, int end) {
        if (start != end) {
            setSpans(text, start, end, this.mSpanFactory.createSuggestionSuggestedTextAppearance());
        }
    }

    private void setSpans(Spannable text, int start, int end, Object[] spans) {
        for (Object span : spans) {
            text.setSpan(span, start, end, 0);
        }
    }
}
