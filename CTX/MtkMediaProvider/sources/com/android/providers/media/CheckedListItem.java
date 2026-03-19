package com.android.providers.media;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Checkable;
import android.widget.CheckedTextView;
import android.widget.RelativeLayout;

public class CheckedListItem extends RelativeLayout implements Checkable {
    public CheckedListItem(Context context) {
        super(context);
    }

    public CheckedListItem(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public CheckedListItem(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    public CheckedListItem(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
    }

    @Override
    public void setChecked(boolean z) {
        getCheckedTextView().setChecked(z);
    }

    @Override
    public boolean isChecked() {
        return getCheckedTextView().isChecked();
    }

    @Override
    public void toggle() {
        getCheckedTextView().toggle();
    }

    private CheckedTextView getCheckedTextView() {
        return (CheckedTextView) findViewById(R.id.checked_text_view);
    }
}
