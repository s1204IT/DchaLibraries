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
import java.util.LinkedList;

public class DefaultSuggestionViewFactory implements SuggestionViewFactory {
    private final SuggestionViewFactory mDefaultFactory;
    private final LinkedList<SuggestionViewFactory> mFactories = new LinkedList<>();
    private HashSet<String> mViewTypes;

    public DefaultSuggestionViewFactory(Context context) {
        this.mDefaultFactory = new DefaultSuggestionView.Factory(context);
        addFactory(new WebSearchSuggestionView.Factory(context));
    }

    protected final void addFactory(SuggestionViewFactory factory) {
        this.mFactories.addFirst(factory);
    }

    @Override
    public Collection<String> getSuggestionViewTypes() {
        if (this.mViewTypes == null) {
            this.mViewTypes = new HashSet<>();
            this.mViewTypes.addAll(this.mDefaultFactory.getSuggestionViewTypes());
            for (SuggestionViewFactory factory : this.mFactories) {
                this.mViewTypes.addAll(factory.getSuggestionViewTypes());
            }
        }
        return this.mViewTypes;
    }

    @Override
    public View getView(SuggestionCursor suggestion, String userQuery, View convertView, ViewGroup parent) {
        for (SuggestionViewFactory factory : this.mFactories) {
            if (factory.canCreateView(suggestion)) {
                return factory.getView(suggestion, userQuery, convertView, parent);
            }
        }
        return this.mDefaultFactory.getView(suggestion, userQuery, convertView, parent);
    }

    @Override
    public String getViewType(Suggestion suggestion) {
        for (SuggestionViewFactory factory : this.mFactories) {
            if (factory.canCreateView(suggestion)) {
                return factory.getViewType(suggestion);
            }
        }
        return this.mDefaultFactory.getViewType(suggestion);
    }

    @Override
    public boolean canCreateView(Suggestion suggestion) {
        return true;
    }
}
