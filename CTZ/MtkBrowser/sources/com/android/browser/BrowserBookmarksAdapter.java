package com.android.browser;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.browser.util.ThreadedCursorAdapter;
import com.android.browser.view.BookmarkContainer;

public class BrowserBookmarksAdapter extends ThreadedCursorAdapter<BrowserBookmarksAdapterItem> {
    Context mContext;
    LayoutInflater mInflater;

    public BrowserBookmarksAdapter(Context context) {
        super(context, null);
        this.mInflater = LayoutInflater.from(context);
        this.mContext = context;
    }

    void bindGridView(View view, Context context, BrowserBookmarksAdapterItem browserBookmarksAdapterItem) {
        int dimensionPixelSize = context.getResources().getDimensionPixelSize(2131427367);
        view.setPadding(dimensionPixelSize, view.getPaddingTop(), dimensionPixelSize, view.getPaddingBottom());
        ImageView imageView = (ImageView) view.findViewById(2131558430);
        ((TextView) view.findViewById(2131558424)).setText(browserBookmarksAdapterItem.title);
        if (browserBookmarksAdapterItem.is_folder) {
            imageView.setImageResource(2130837613);
            imageView.setScaleType(ImageView.ScaleType.FIT_END);
            imageView.setBackground(null);
        } else {
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            if (browserBookmarksAdapterItem.thumbnail == null || !browserBookmarksAdapterItem.has_thumbnail) {
                imageView.setImageResource(2130837518);
            } else {
                imageView.setImageDrawable(browserBookmarksAdapterItem.thumbnail);
            }
            imageView.setBackgroundResource(2130837516);
        }
    }

    @Override
    public void bindView(View view, BrowserBookmarksAdapterItem browserBookmarksAdapterItem) {
        BookmarkContainer bookmarkContainer = (BookmarkContainer) view;
        bookmarkContainer.setIgnoreRequestLayout(true);
        bindGridView(view, this.mContext, browserBookmarksAdapterItem);
        bookmarkContainer.setIgnoreRequestLayout(false);
    }

    @Override
    protected long getItemId(Cursor cursor) {
        return cursor.getLong(0);
    }

    @Override
    public BrowserBookmarksAdapterItem getLoadingObject() {
        return new BrowserBookmarksAdapterItem();
    }

    @Override
    public BrowserBookmarksAdapterItem getRowObject(Cursor cursor, BrowserBookmarksAdapterItem browserBookmarksAdapterItem) {
        if (browserBookmarksAdapterItem == null) {
            browserBookmarksAdapterItem = new BrowserBookmarksAdapterItem();
        }
        Bitmap bitmap = BrowserBookmarksPage.getBitmap(cursor, 4, browserBookmarksAdapterItem.thumbnail != null ? browserBookmarksAdapterItem.thumbnail.getBitmap() : null);
        browserBookmarksAdapterItem.has_thumbnail = bitmap != null;
        if (bitmap != null && (browserBookmarksAdapterItem.thumbnail == null || browserBookmarksAdapterItem.thumbnail.getBitmap() != bitmap)) {
            browserBookmarksAdapterItem.thumbnail = new BitmapDrawable(this.mContext.getResources(), bitmap);
        }
        browserBookmarksAdapterItem.is_folder = cursor.getInt(6) != 0;
        browserBookmarksAdapterItem.title = getTitle(cursor);
        browserBookmarksAdapterItem.url = cursor.getString(1);
        return browserBookmarksAdapterItem;
    }

    CharSequence getTitle(Cursor cursor) {
        return cursor.getInt(9) != 4 ? cursor.getString(2) : this.mContext.getText(2131492932);
    }

    @Override
    public View newView(Context context, ViewGroup viewGroup) {
        return this.mInflater.inflate(2130968585, viewGroup, false);
    }
}
