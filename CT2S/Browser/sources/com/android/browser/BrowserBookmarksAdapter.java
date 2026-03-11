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
    protected long getItemId(Cursor c) {
        return c.getLong(0);
    }

    @Override
    public View newView(Context context, ViewGroup parent) {
        return this.mInflater.inflate(R.layout.bookmark_thumbnail, parent, false);
    }

    @Override
    public void bindView(View view, BrowserBookmarksAdapterItem object) {
        BookmarkContainer container = (BookmarkContainer) view;
        container.setIgnoreRequestLayout(true);
        bindGridView(view, this.mContext, object);
        container.setIgnoreRequestLayout(false);
    }

    CharSequence getTitle(Cursor cursor) {
        int type = cursor.getInt(9);
        switch (type) {
            case 4:
                return this.mContext.getText(R.string.other_bookmarks);
            default:
                return cursor.getString(2);
        }
    }

    void bindGridView(View view, Context context, BrowserBookmarksAdapterItem item) {
        int padding = context.getResources().getDimensionPixelSize(R.dimen.combo_horizontalSpacing);
        view.setPadding(padding, view.getPaddingTop(), padding, view.getPaddingBottom());
        ImageView thumb = (ImageView) view.findViewById(R.id.thumb);
        TextView tv = (TextView) view.findViewById(R.id.label);
        tv.setText(item.title);
        if (item.is_folder) {
            thumb.setImageResource(R.drawable.thumb_bookmark_widget_folder_holo);
            thumb.setScaleType(ImageView.ScaleType.FIT_END);
            thumb.setBackground(null);
        } else {
            thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
            if (item.thumbnail == null || !item.has_thumbnail) {
                thumb.setImageResource(R.drawable.browser_thumbnail);
            } else {
                thumb.setImageDrawable(item.thumbnail);
            }
            thumb.setBackgroundResource(R.drawable.border_thumb_bookmarks_widget_holo);
        }
    }

    @Override
    public BrowserBookmarksAdapterItem getRowObject(Cursor c, BrowserBookmarksAdapterItem item) {
        if (item == null) {
            item = new BrowserBookmarksAdapterItem();
        }
        Bitmap thumbnail = BrowserBookmarksPage.getBitmap(c, 4, item.thumbnail != null ? item.thumbnail.getBitmap() : null);
        item.has_thumbnail = thumbnail != null;
        if (thumbnail != null && (item.thumbnail == null || item.thumbnail.getBitmap() != thumbnail)) {
            item.thumbnail = new BitmapDrawable(this.mContext.getResources(), thumbnail);
        }
        item.is_folder = c.getInt(6) != 0;
        item.title = getTitle(c);
        item.url = c.getString(1);
        return item;
    }

    @Override
    public BrowserBookmarksAdapterItem getLoadingObject() {
        BrowserBookmarksAdapterItem item = new BrowserBookmarksAdapterItem();
        return item;
    }
}
