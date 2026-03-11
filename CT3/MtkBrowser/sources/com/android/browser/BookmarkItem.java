package com.android.browser;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.TextView;

class BookmarkItem extends HorizontalScrollView {
    protected boolean mEnableScrolling;
    protected ImageView mImageView;
    protected TextView mTextView;
    protected String mTitle;
    protected String mUrl;
    protected TextView mUrlText;

    BookmarkItem(Context context) {
        super(context);
        this.mEnableScrolling = false;
        setClickable(false);
        setEnableScrolling(false);
        LayoutInflater factory = LayoutInflater.from(context);
        factory.inflate(R.layout.history_item, this);
        this.mTextView = (TextView) findViewById(R.id.title);
        this.mUrlText = (TextView) findViewById(R.id.url);
        this.mImageView = (ImageView) findViewById(R.id.favicon);
        View star = findViewById(R.id.star);
        star.setVisibility(8);
    }

    String getName() {
        return this.mTitle;
    }

    String getUrl() {
        return this.mUrl;
    }

    void setFavicon(Bitmap b) {
        if (b != null) {
            this.mImageView.setImageBitmap(b);
        } else {
            this.mImageView.setImageResource(R.drawable.app_web_browser_sm);
        }
    }

    void setFaviconBackground(Drawable d) {
        this.mImageView.setBackgroundDrawable(d);
    }

    void setName(String name) {
        if (name == null) {
            return;
        }
        this.mTitle = name;
        if (name.length() > 80) {
            name = name.substring(0, 80);
        }
        this.mTextView.setText(name);
    }

    void setUrl(String url) {
        if (url == null) {
            return;
        }
        this.mUrl = url;
        String url2 = UrlUtils.stripUrl(url);
        if (url2.length() > 80) {
            url2 = url2.substring(0, 80);
        }
        this.mUrlText.setText(url2);
    }

    void setEnableScrolling(boolean enable) {
        this.mEnableScrolling = enable;
        setFocusable(this.mEnableScrolling);
        setFocusableInTouchMode(this.mEnableScrolling);
        requestDisallowInterceptTouchEvent(!this.mEnableScrolling);
        requestLayout();
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (this.mEnableScrolling) {
            return super.onTouchEvent(ev);
        }
        return false;
    }

    @Override
    protected void measureChild(View child, int parentWidthMeasureSpec, int parentHeightMeasureSpec) {
        if (this.mEnableScrolling) {
            super.measureChild(child, parentWidthMeasureSpec, parentHeightMeasureSpec);
            return;
        }
        ViewGroup.LayoutParams lp = child.getLayoutParams();
        int childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec, this.mPaddingLeft + this.mPaddingRight, lp.width);
        int childHeightMeasureSpec = getChildMeasureSpec(parentHeightMeasureSpec, this.mPaddingTop + this.mPaddingBottom, lp.height);
        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    @Override
    protected void measureChildWithMargins(View child, int parentWidthMeasureSpec, int widthUsed, int parentHeightMeasureSpec, int heightUsed) {
        if (this.mEnableScrolling) {
            super.measureChildWithMargins(child, parentWidthMeasureSpec, widthUsed, parentHeightMeasureSpec, heightUsed);
            return;
        }
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) child.getLayoutParams();
        int childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec, this.mPaddingLeft + this.mPaddingRight + lp.leftMargin + lp.rightMargin + widthUsed, lp.width);
        int childHeightMeasureSpec = getChildMeasureSpec(parentHeightMeasureSpec, this.mPaddingTop + this.mPaddingBottom + lp.topMargin + lp.bottomMargin + heightUsed, lp.height);
        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }
}
