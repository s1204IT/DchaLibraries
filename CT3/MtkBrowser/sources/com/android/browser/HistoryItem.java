package com.android.browser;

import android.content.Context;
import android.widget.CompoundButton;

class HistoryItem extends BookmarkItem implements CompoundButton.OnCheckedChangeListener {
    private CompoundButton mStar;

    HistoryItem(Context context) {
        this(context, true);
    }

    HistoryItem(Context context, boolean showStar) {
        super(context);
        this.mStar = (CompoundButton) findViewById(R.id.star);
        this.mStar.setOnCheckedChangeListener(this);
        if (showStar) {
            this.mStar.setVisibility(0);
        } else {
            this.mStar.setVisibility(8);
        }
    }

    void copyTo(HistoryItem item) {
        item.mTextView.setText(this.mTextView.getText());
        item.mUrlText.setText(this.mUrlText.getText());
        item.setIsBookmark(this.mStar.isChecked());
        item.mImageView.setImageDrawable(this.mImageView.getDrawable());
    }

    boolean isBookmark() {
        return this.mStar.isChecked();
    }

    void setIsBookmark(boolean isBookmark) {
        this.mStar.setOnCheckedChangeListener(null);
        this.mStar.setChecked(isBookmark);
        this.mStar.setOnCheckedChangeListener(this);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            setIsBookmark(false);
            com.android.browser.provider.Browser.saveBookmark(getContext(), getName(), this.mUrl);
        } else {
            Bookmarks.removeFromBookmarks(getContext(), getContext().getContentResolver(), this.mUrl, getName());
        }
    }
}
