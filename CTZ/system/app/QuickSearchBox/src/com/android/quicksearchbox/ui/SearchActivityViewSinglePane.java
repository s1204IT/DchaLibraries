package com.android.quicksearchbox.ui;

import android.content.Context;
import android.util.AttributeSet;

/* loaded from: classes.dex */
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

    @Override // com.android.quicksearchbox.ui.SearchActivityView
    public void onResume() {
        focusQueryTextView();
    }

    @Override // com.android.quicksearchbox.ui.SearchActivityView
    public void considerHidingInputMethod() {
        this.mQueryTextView.hideInputMethod();
    }

    @Override // com.android.quicksearchbox.ui.SearchActivityView
    public void onStop() {
    }
}
