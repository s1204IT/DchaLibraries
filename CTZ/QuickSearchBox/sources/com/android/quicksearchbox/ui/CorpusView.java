package com.android.quicksearchbox.ui;

import android.R;
import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewDebug;
import android.widget.Checkable;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class CorpusView extends RelativeLayout implements Checkable {
    private static final int[] CHECKED_STATE_SET = {R.attr.state_checked};
    private boolean mChecked;
    private ImageView mIcon;
    private TextView mLabel;

    public CorpusView(Context context) {
        super(context);
    }

    public CorpusView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    @ViewDebug.ExportedProperty
    public boolean isChecked() {
        return this.mChecked;
    }

    @Override
    protected int[] onCreateDrawableState(int i) {
        int[] iArrOnCreateDrawableState = super.onCreateDrawableState(i + 1);
        if (isChecked()) {
            mergeDrawableStates(iArrOnCreateDrawableState, CHECKED_STATE_SET);
        }
        return iArrOnCreateDrawableState;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mIcon = (ImageView) findViewById(2131689472);
        this.mLabel = (TextView) findViewById(2131689473);
    }

    @Override
    public void setChecked(boolean z) {
        if (this.mChecked != z) {
            this.mChecked = z;
            refreshDrawableState();
        }
    }

    @Override
    public void toggle() {
        setChecked(!this.mChecked);
    }
}
