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

    public ExpandableTextView(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
        this.mRelayout = false;
        this.mCollapsed = true;
        this.mMaxCollapsedLines = 8;
        init();
    }

    public ExpandableTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
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
    public void onClick(View v) {
        if (this.mButton.getVisibility() == 0) {
            this.mCollapsed = !this.mCollapsed;
            this.mButton.setImageDrawable(this.mCollapsed ? this.mExpandDrawable : this.mCollapseDrawable);
            this.mTv.setMaxLines(this.mCollapsed ? this.mMaxCollapsedLines : Integer.MAX_VALUE);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (!this.mRelayout || getVisibility() == 8) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        this.mRelayout = false;
        this.mButton.setVisibility(8);
        this.mTv.setMaxLines(Integer.MAX_VALUE);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (this.mTv.getLineCount() > this.mMaxCollapsedLines) {
            if (this.mCollapsed) {
                this.mTv.setMaxLines(this.mMaxCollapsedLines);
            }
            this.mButton.setVisibility(0);
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    private void findViews() {
        this.mTv = (TextView) findViewById(R.id.expandable_text);
        this.mTv.setOnClickListener(this);
        this.mButton = (ImageButton) findViewById(R.id.expand_collapse);
        this.mButton.setOnClickListener(this);
    }

    public void setText(String text) {
        this.mRelayout = true;
        if (this.mTv == null) {
            findViews();
        }
        String trimmedText = text.trim();
        this.mTv.setText(trimmedText);
        setVisibility(trimmedText.length() == 0 ? 8 : 0);
    }

    public CharSequence getText() {
        return this.mTv == null ? "" : this.mTv.getText();
    }
}
