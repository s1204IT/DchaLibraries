package com.android.photos.data;

import android.content.CursorLoader;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import com.android.photos.drawables.DataUriThumbnailDrawable;
import com.android.photos.shims.LoaderCompatShim;
import com.mediatek.gallery3d.video.BookmarkEnhance;
import java.util.ArrayList;

public class PhotoSetLoader extends CursorLoader implements LoaderCompatShim<Cursor> {
    private final ContentObserver mGlobalObserver;
    private static final Uri CONTENT_URI = MediaStore.Files.getContentUri("external");
    public static final String[] PROJECTION = {BookmarkEnhance.COLUMN_ID, BookmarkEnhance.COLUMN_DATA, "width", "height", BookmarkEnhance.COLUMN_ADD_DATE, "media_type", "supported_operations"};
    private static final Uri GLOBAL_CONTENT_URI = Uri.parse("content://media/external/");

    @Override
    protected void onStartLoading() {
        super.onStartLoading();
        getContext().getContentResolver().registerContentObserver(GLOBAL_CONTENT_URI, true, this.mGlobalObserver);
    }

    @Override
    protected void onReset() {
        super.onReset();
        getContext().getContentResolver().unregisterContentObserver(this.mGlobalObserver);
    }

    @Override
    public Drawable drawableForItem(Cursor cursor, Drawable drawable) {
        ?? dataUriThumbnailDrawable;
        if (drawable != null) {
            boolean z = drawable instanceof DataUriThumbnailDrawable;
            dataUriThumbnailDrawable = drawable;
            if (!z) {
                dataUriThumbnailDrawable = new DataUriThumbnailDrawable();
            }
        }
        dataUriThumbnailDrawable.setImage(cursor.getString(1), cursor.getInt(2), cursor.getInt(3));
        return dataUriThumbnailDrawable;
    }

    @Override
    public Uri uriForItem(Cursor cursor) {
        return null;
    }

    @Override
    public ArrayList<Uri> urisForSubItems(Cursor cursor) {
        return null;
    }

    @Override
    public void deleteItemWithPath(Object obj) {
    }

    @Override
    public Object getPathForItem(Cursor cursor) {
        return null;
    }
}
