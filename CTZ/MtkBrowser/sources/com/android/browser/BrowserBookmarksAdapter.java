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

    @Override
    protected long getItemId(Cursor cursor) {
        return cursor.getLong(0);
    }

    @Override
    public View newView(Context context, ViewGroup viewGroup) {
        return this.mInflater.inflate(R.layout.bookmark_thumbnail, viewGroup, false);
    }

    @Override
    public void bindView(View view, BrowserBookmarksAdapterItem browserBookmarksAdapterItem) {
        BookmarkContainer bookmarkContainer = (BookmarkContainer) view;
        bookmarkContainer.setIgnoreRequestLayout(true);
        bindGridView(view, this.mContext, browserBookmarksAdapterItem);
        bookmarkContainer.setIgnoreRequestLayout(false);
    }

    CharSequence getTitle(Cursor cursor) {
        if (cursor.getInt(9) == 4) {
            return this.mContext.getText(R.string.other_bookmarks);
        }
        return cursor.getString(2);
    }

    void bindGridView(View view, Context context, BrowserBookmarksAdapterItem browserBookmarksAdapterItem) {
        int dimensionPixelSize = context.getResources().getDimensionPixelSize(R.dimen.combo_horizontalSpacing);
        view.setPadding(dimensionPixelSize, view.getPaddingTop(), dimensionPixelSize, view.getPaddingBottom());
        ImageView imageView = (ImageView) view.findViewById(R.id.thumb);
        ((TextView) view.findViewById(R.id.label)).setText(browserBookmarksAdapterItem.title);
        if (browserBookmarksAdapterItem.is_folder) {
            imageView.setImageResource(R.drawable.thumb_bookmark_widget_folder_holo);
            imageView.setScaleType(ImageView.ScaleType.FIT_END);
            imageView.setBackground(null);
        } else {
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            if (browserBookmarksAdapterItem.thumbnail == null || !browserBookmarksAdapterItem.has_thumbnail) {
                imageView.setImageResource(R.drawable.browser_thumbnail);
            } else {
                imageView.setImageDrawable(browserBookmarksAdapterItem.thumbnail);
            }
            imageView.setBackgroundResource(R.drawable.border_thumb_bookmarks_widget_holo);
        }
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

    @Override
    public BrowserBookmarksAdapterItem getLoadingObject() {
        return new BrowserBookmarksAdapterItem();
    }
}
