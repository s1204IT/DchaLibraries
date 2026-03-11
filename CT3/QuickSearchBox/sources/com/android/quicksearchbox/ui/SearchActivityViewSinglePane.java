package com.android.quicksearchbox.ui;

import android.content.Context;
import android.util.AttributeSet;

public class SearchActivityViewSinglePane extends SearchActivityView {
    public SearchActivityViewSinglePane(Context context) {
        super(context);
    }

    public SearchActivityViewSinglePane(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SearchActivityViewSinglePane(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
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
