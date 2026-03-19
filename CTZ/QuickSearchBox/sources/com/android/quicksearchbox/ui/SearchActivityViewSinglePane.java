package com.android.quicksearchbox.ui;

import android.content.Context;
import android.util.AttributeSet;

public class SearchActivityViewSinglePane extends SearchActivityView {
    public SearchActivityViewSinglePane(Context context) {
        super(context);
    }

    public SearchActivityViewSinglePane(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public SearchActivityViewSinglePane(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    @Override
    public void onResume() {
        focusQueryTextView();
    }

    @Override
    public void considerHidingInputMethod() {
        this.mQueryTextView.hideInputMethod();
    }

    @Override
    public void onStop() {
    }
}
