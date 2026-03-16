package com.android.quicksearchbox.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.quicksearchbox.Suggestion;
import com.android.quicksearchbox.SuggestionCursor;
import java.util.Collection;
import java.util.Collections;

public class SuggestionViewInflater implements SuggestionViewFactory {
    private final Context mContext;
    private final int mLayoutId;
    private final Class<?> mViewClass;
    private final String mViewType;

    public SuggestionViewInflater(String viewType, Class<? extends SuggestionView> viewClass, int layoutId, Context context) {
        this.mViewType = viewType;
        this.mViewClass = viewClass;
        this.mLayoutId = layoutId;
        this.mContext = context;
    }

    protected LayoutInflater getInflater() {
        return (LayoutInflater) this.mContext.getSystemService("layout_inflater");
    }

    @Override
    public Collection<String> getSuggestionViewTypes() {
        return Collections.singletonList(this.mViewType);
    }

    @Override
    public View getView(SuggestionCursor suggestion, String userQuery, View convertView, ViewGroup parent) {
        if (convertView == null || !convertView.getClass().equals(this.mViewClass)) {
            int layoutId = this.mLayoutId;
            convertView = getInflater().inflate(layoutId, parent, false);
        }
        if (!(convertView instanceof SuggestionView)) {
            throw new IllegalArgumentException("Not a SuggestionView: " + convertView);
        }
        ((SuggestionView) convertView).bindAsSuggestion(suggestion, userQuery);
        return convertView;
    }

    @Override
    public String getViewType(Suggestion suggestion) {
        return this.mViewType;
    }

    @Override
    public boolean canCreateView(Suggestion suggestion) {
        return true;
    }
}
