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

    @Override
    public boolean canCreateView(Suggestion suggestion) {
        return true;
    }

    @Override
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

    @Override
    public View getView(SuggestionCursor suggestionCursor, String str, View view, ViewGroup viewGroup) {
        for (SuggestionViewFactory suggestionViewFactory : this.mFactories) {
            if (suggestionViewFactory.canCreateView(suggestionCursor)) {
                return suggestionViewFactory.getView(suggestionCursor, str, view, viewGroup);
            }
        }
        return this.mDefaultFactory.getView(suggestionCursor, str, view, viewGroup);
    }

    @Override
    public String getViewType(Suggestion suggestion) {
        for (SuggestionViewFactory suggestionViewFactory : this.mFactories) {
            if (suggestionViewFactory.canCreateView(suggestion)) {
                return suggestionViewFactory.getViewType(suggestion);
            }
        }
        return this.mDefaultFactory.getViewType(suggestion);
    }
}
