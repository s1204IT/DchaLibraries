package com.android.calendar;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ExpandableTextView extends LinearLayout implements View.OnClickListener {
    ImageButton mButton;
    private Drawable mCollapseDrawable;
    private boolean mCollapsed;
    private Drawable mExpandDrawable;
    private int mMaxCollapsedLines;
    private boolean mRelayout;
    TextView mTv;

    public ExpandableTextView(Context context) {
        super(context);
        this.mRelayout = false;
        this.mCollapsed = true;
        this.mMaxCollapsedLines = 8;
        init();
    }

    public ExpandableTextView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet, 0);
        this.mRelayout = false;
        this.mCollapsed = true;
        this.mMaxCollapsedLines = 8;
        init();
    }

    public ExpandableTextView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mRelayout = false;
        this.mCollapsed = true;
        this.mMaxCollapsedLines = 8;
        init();
    }

    void init() {
        this.mMaxCollapsedLines = getResources().getInteger(R.integer.event_info_desc_line_num);
        this.mExpandDrawable = getResources().getDrawable(R.drawable.ic_expand_small_holo_light);
        this.mCollapseDrawable = getResources().getDrawable(R.drawable.ic_collapse_small_holo_light);
    }

    @Override
    public void onClick(View view) {
        if (this.mButton.getVisibility() != 0) {
            return;
        }
        this.mCollapsed = !this.mCollapsed;
        this.mButton.setImageDrawable(this.mCollapsed ? this.mExpandDrawable : this.mCollapseDrawable);
        this.mTv.setMaxLines(this.mCollapsed ? this.mMaxCollapsedLines : Integer.MAX_VALUE);
    }

    @Override
    protected void onMeasure(int i, int i2) {
        if (!this.mRelayout || getVisibility() == 8) {
            super.onMeasure(i, i2);
            return;
        }
        this.mRelayout = false;
        this.mButton.setVisibility(8);
        this.mTv.setMaxLines(Integer.MAX_VALUE);
        super.onMeasure(i, i2);
        if (this.mTv.getLineCount() <= this.mMaxCollapsedLines) {
            return;
        }
        if (this.mCollapsed) {
            this.mTv.setMaxLines(this.mMaxCollapsedLines);
        }
        this.mButton.setVisibility(0);
        super.onMeasure(i, i2);
    }

    private void findViews() {
        this.mTv = (TextView) findViewById(R.id.expandable_text);
        this.mTv.setOnClickListener(this);
        this.mButton = (ImageButton) findViewById(R.id.expand_collapse);
        this.mButton.setOnClickListener(this);
    }

    public void setText(String str) {
        this.mRelayout = true;
        if (this.mTv == null) {
            findViews();
        }
        String strTrim = str.trim();
        this.mTv.setText(strTrim);
        setVisibility(strTrim.length() == 0 ? 8 : 0);
    }

    public CharSequence getText() {
        if (this.mTv == null) {
            return "";
        }
        return this.mTv.getText();
    }
}
