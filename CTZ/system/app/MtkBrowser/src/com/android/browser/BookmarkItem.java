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

/* loaded from: classes.dex */
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
        LayoutInflater.from(context).inflate(R.layout.history_item, this);
        this.mTextView = (TextView) findViewById(R.id.title);
        this.mUrlText = (TextView) findViewById(R.id.url);
        this.mImageView = (ImageView) findViewById(R.id.favicon);
        findViewById(R.id.star).setVisibility(8);
    }

    String getName() {
        return this.mTitle;
    }

    String getUrl() {
        return this.mUrl;
    }

    void setFavicon(Bitmap bitmap) {
        if (bitmap != null) {
            this.mImageView.setImageBitmap(bitmap);
        } else {
            this.mImageView.setImageResource(R.drawable.app_web_browser_sm);
        }
    }

    void setFaviconBackground(Drawable drawable) {
        this.mImageView.setBackgroundDrawable(drawable);
    }

    void setName(String str) {
        if (str == null) {
            return;
        }
        this.mTitle = str;
        if (str.length() > 80) {
            str = str.substring(0, 80);
        }
        this.mTextView.setText(str);
    }

    void setUrl(String str) {
        if (str == null) {
            return;
        }
        this.mUrl = str;
        String strStripUrl = UrlUtils.stripUrl(str);
        if (strStripUrl.length() > 80) {
            strStripUrl = strStripUrl.substring(0, 80);
        }
        this.mUrlText.setText(strStripUrl);
    }

    void setEnableScrolling(boolean z) {
        this.mEnableScrolling = z;
        setFocusable(this.mEnableScrolling);
        setFocusableInTouchMode(this.mEnableScrolling);
        requestDisallowInterceptTouchEvent(!this.mEnableScrolling);
        requestLayout();
    }

    @Override // android.widget.HorizontalScrollView, android.view.View
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (this.mEnableScrolling) {
            return super.onTouchEvent(motionEvent);
        }
        return false;
    }

    @Override // android.widget.HorizontalScrollView, android.view.ViewGroup
    protected void measureChild(View view, int i, int i2) {
        if (this.mEnableScrolling) {
            super.measureChild(view, i, i2);
        } else {
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            view.measure(getChildMeasureSpec(i, this.mPaddingLeft + this.mPaddingRight, layoutParams.width), getChildMeasureSpec(i2, this.mPaddingTop + this.mPaddingBottom, layoutParams.height));
        }
    }

    @Override // android.widget.HorizontalScrollView, android.view.ViewGroup
    protected void measureChildWithMargins(View view, int i, int i2, int i3, int i4) {
        if (this.mEnableScrolling) {
            super.measureChildWithMargins(view, i, i2, i3, i4);
        } else {
            ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
            view.measure(getChildMeasureSpec(i, this.mPaddingLeft + this.mPaddingRight + marginLayoutParams.leftMargin + marginLayoutParams.rightMargin + i2, marginLayoutParams.width), getChildMeasureSpec(i3, this.mPaddingTop + this.mPaddingBottom + marginLayoutParams.topMargin + marginLayoutParams.bottomMargin + i4, marginLayoutParams.height));
        }
    }
}
