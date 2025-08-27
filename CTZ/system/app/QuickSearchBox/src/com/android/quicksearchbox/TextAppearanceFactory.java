package com.android.quicksearchbox;

import android.content.Context;
import android.text.style.TextAppearanceSpan;

/* loaded from: classes.dex */
public class TextAppearanceFactory {
    private final Context mContext;

    public TextAppearanceFactory(Context context) {
        this.mContext = context;
    }

    public Object[] createSuggestionQueryTextAppearance() {
        return new Object[]{new TextAppearanceSpan(this.mContext, R.style.SuggestionText1_Query)};
    }

    public Object[] createSuggestionSuggestedTextAppearance() {
        return new Object[]{new TextAppearanceSpan(this.mContext, R.style.SuggestionText1_Suggested)};
    }
}
