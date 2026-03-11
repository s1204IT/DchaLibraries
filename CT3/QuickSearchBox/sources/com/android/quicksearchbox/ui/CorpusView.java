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

    public CorpusView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CorpusView(Context context) {
        super(context);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mIcon = (ImageView) findViewById(com.android.quicksearchbox.R.id.source_icon);
        this.mLabel = (TextView) findViewById(com.android.quicksearchbox.R.id.source_label);
    }

    @Override
    @ViewDebug.ExportedProperty
    public boolean isChecked() {
        return this.mChecked;
    }

    @Override
    public void setChecked(boolean checked) {
        if (this.mChecked == checked) {
            return;
        }
        this.mChecked = checked;
        refreshDrawableState();
    }

    @Override
    public void toggle() {
        setChecked(!this.mChecked);
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (isChecked()) {
            mergeDrawableStates(drawableState, CHECKED_STATE_SET);
        }
        return drawableState;
    }
}
