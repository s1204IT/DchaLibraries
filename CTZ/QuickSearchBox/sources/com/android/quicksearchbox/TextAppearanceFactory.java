package com.android.quicksearchbox;

import android.content.Context;
import android.text.style.TextAppearanceSpan;

public class TextAppearanceFactory {
    private final Context mContext;

    public TextAppearanceFactory(Context context) {
        this.mContext = context;
    }

    public Object[] createSuggestionQueryTextAppearance() {
        return new Object[]{new TextAppearanceSpan(this.mContext, 2131558407)};
    }

    public Object[] createSuggestionSuggestedTextAppearance() {
        return new Object[]{new TextAppearanceSpan(this.mContext, 2131558408)};
    }
}
