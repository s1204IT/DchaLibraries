package com.android.browser;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class NavTabView extends LinearLayout {
    private View.OnClickListener mClickListener;
    private ImageView mClose;
    private ViewGroup mContent;
    private boolean mHighlighted;
    ImageView mImage;
    private Tab mTab;
    private TextView mTitle;
    private View mTitleBar;

    public NavTabView(Context context) {
        super(context);
        init();
    }

    private void init() {
        LayoutInflater.from(this.mContext).inflate(R.layout.nav_tab_view, this);
        this.mContent = (ViewGroup) findViewById(R.id.main);
        this.mClose = (ImageView) findViewById(R.id.closetab);
        this.mTitle = (TextView) findViewById(R.id.title);
        this.mTitleBar = findViewById(R.id.titlebar);
        this.mImage = (ImageView) findViewById(R.id.tab_view);
    }

    protected boolean isClose(View v) {
        return v == this.mClose;
    }

    protected boolean isTitle(View v) {
        return v == this.mTitleBar;
    }

    protected boolean isWebView(View v) {
        return v == this.mImage;
    }

    private void setTitle() {
        if (this.mTab == null) {
            return;
        }
        if (this.mHighlighted) {
            this.mTitle.setText(this.mTab.getUrl());
        } else {
            String txt = this.mTab.getTitle();
            if (txt == null) {
                txt = this.mTab.getUrl();
            }
            this.mTitle.setText(txt);
        }
        if (this.mTab.isSnapshot()) {
            setTitleIcon(R.drawable.ic_history_holo_dark);
        } else if (this.mTab.isPrivateBrowsingEnabled()) {
            setTitleIcon(R.drawable.ic_incognito_holo_dark);
        } else {
            setTitleIcon(0);
        }
    }

    private void setTitleIcon(int id) {
        if (id == 0) {
            this.mTitle.setPadding(this.mTitle.getCompoundDrawablePadding(), 0, 0, 0);
        } else {
            this.mTitle.setPadding(0, 0, 0, 0);
        }
        this.mTitle.setCompoundDrawablesWithIntrinsicBounds(id, 0, 0, 0);
    }

    protected void setWebView(Tab tab) {
        this.mTab = tab;
        setTitle();
        Bitmap image = tab.getScreenshot();
        if (image == null) {
            return;
        }
        this.mImage.setImageBitmap(image);
        if (tab == null) {
            return;
        }
        this.mImage.setContentDescription(tab.getTitle());
    }

    @Override
    public void setOnClickListener(View.OnClickListener listener) {
        this.mClickListener = listener;
        this.mTitleBar.setOnClickListener(this.mClickListener);
        this.mClose.setOnClickListener(this.mClickListener);
        if (this.mImage == null) {
            return;
        }
        this.mImage.setOnClickListener(this.mClickListener);
    }
}
