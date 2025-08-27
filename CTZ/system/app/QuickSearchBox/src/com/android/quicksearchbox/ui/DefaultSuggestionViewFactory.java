package com.android.quicksearchbox.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import com.android.quicksearchbox.Suggestion;
import com.android.quicksearchbox.SuggestionCursor;
import com.android.quicksearchbox.ui.DefaultSuggestionView;
import com.android.quicksearchbox.ui.WebSearchSuggestionView;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

/* loaded from: classes.dex */
public class DefaultSuggestionViewFactory implements SuggestionViewFactory {
    private final SuggestionViewFactory mDefaultFactory;
    private final LinkedList<SuggestionViewFactory> mFactories = new LinkedList<>();
    private HashSet<String> mViewTypes;

    public DefaultSuggestionViewFactory(Context context) {
        this.mDefaultFactory = new DefaultSuggestionView.Factory(context);
        addFactory(new WebSearchSuggestionView.Factory(context));
    }

    protected final void addFactory(SuggestionViewFactory suggestionViewFactory) {
        this.mFactories.addFirst(suggestionViewFactory);
    }

    @Override // com.android.quicksearchbox.ui.SuggestionViewFactory
    public Collection<String> getSuggestionViewTypes() {
        if (this.mViewTypes == null) {
            this.mViewTypes = new HashSet<>();
            this.mViewTypes.addAll(this.mDefaultFactory.getSuggestionViewTypes());
            Iterator<SuggestionViewFactory> it = this.mFactories.iterator();
            while (it.hasNext()) {
                this.mViewTypes.addAll(it.next().getSuggestionViewTypes());
            }
        }
        return this.mViewTypes;
    }

    @Override // com.android.quicksearchbox.ui.SuggestionViewFactory
    public View getView(SuggestionCursor suggestionCursor, String str, View view, ViewGroup viewGroup) {
        Iterator<SuggestionViewFactory> it = this.mFactories.iterator();
        while (it.hasNext()) {
            SuggestionViewFactory next = it.next();
            if (next.canCreateView(suggestionCursor)) {
                return next.getView(suggestionCursor, str, view, viewGroup);
            }
        }
        return this.mDefaultFactory.getView(suggestionCursor, str, view, viewGroup);
    }

    @Override // com.android.quicksearchbox.ui.SuggestionViewFactory
    public String getViewType(Suggestion suggestion) {
        Iterator<SuggestionViewFactory> it = this.mFactories.iterator();
        while (it.hasNext()) {
            SuggestionViewFactory next = it.next();
            if (next.canCreateView(suggestion)) {
                return next.getViewType(suggestion);
            }
        }
        return this.mDefaultFactory.getViewType(suggestion);
    }

    @Override // com.android.quicksearchbox.ui.SuggestionViewFactory
    public boolean canCreateView(Suggestion suggestion) {
        return true;
    }
}
