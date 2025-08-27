package com.android.quicksearchbox.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.quicksearchbox.Suggestion;
import com.android.quicksearchbox.SuggestionCursor;
import java.util.Collection;
import java.util.Collections;

/* loaded from: classes.dex */
public class SuggestionViewInflater implements SuggestionViewFactory {
    private final Context mContext;
    private final int mLayoutId;
    private final Class<?> mViewClass;
    private final String mViewType;

    public SuggestionViewInflater(String str, Class<? extends SuggestionView> cls, int i, Context context) {
        this.mViewType = str;
        this.mViewClass = cls;
        this.mLayoutId = i;
        this.mContext = context;
    }

    protected LayoutInflater getInflater() {
        return (LayoutInflater) this.mContext.getSystemService("layout_inflater");
    }

    @Override // com.android.quicksearchbox.ui.SuggestionViewFactory
    public Collection<String> getSuggestionViewTypes() {
        return Collections.singletonList(this.mViewType);
    }

    @Override // com.android.quicksearchbox.ui.SuggestionViewFactory
    public View getView(SuggestionCursor suggestionCursor, String str, View view, ViewGroup viewGroup) {
        if (view == null || !view.getClass().equals(this.mViewClass)) {
            view = getInflater().inflate(this.mLayoutId, viewGroup, false);
        }
        if (!(view instanceof SuggestionView)) {
            throw new IllegalArgumentException("Not a SuggestionView: " + view);
        }
        ((SuggestionView) view).bindAsSuggestion(suggestionCursor, str);
        return view;
    }

    @Override // com.android.quicksearchbox.ui.SuggestionViewFactory
    public String getViewType(Suggestion suggestion) {
        return this.mViewType;
    }

    @Override // com.android.quicksearchbox.ui.SuggestionViewFactory
    public boolean canCreateView(Suggestion suggestion) {
        return true;
    }
}
