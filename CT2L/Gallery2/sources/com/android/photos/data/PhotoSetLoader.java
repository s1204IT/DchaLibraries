package com.android.photos.data;

import android.content.CursorLoader;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import com.android.photos.drawables.DataUriThumbnailDrawable;
import com.android.photos.shims.LoaderCompatShim;
import java.util.ArrayList;

public class PhotoSetLoader extends CursorLoader implements LoaderCompatShim<Cursor> {
    private final ContentObserver mGlobalObserver;
    private static final Uri CONTENT_URI = MediaStore.Files.getContentUri("external");
    public static final String[] PROJECTION = {"_id", "_data", "width", "height", "date_added", "media_type", "supported_operations"};
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
    public Drawable drawableForItem(Cursor item, Drawable recycle) {
        DataUriThumbnailDrawable drawable;
        if (recycle == null || !(recycle instanceof DataUriThumbnailDrawable)) {
            drawable = new DataUriThumbnailDrawable();
        } else {
            drawable = (DataUriThumbnailDrawable) recycle;
        }
        drawable.setImage(item.getString(1), item.getInt(2), item.getInt(3));
        return drawable;
    }

    @Override
    public Uri uriForItem(Cursor item) {
        return null;
    }

    @Override
    public ArrayList<Uri> urisForSubItems(Cursor item) {
        return null;
    }

    @Override
    public void deleteItemWithPath(Object path) {
    }

    @Override
    public Object getPathForItem(Cursor item) {
        return null;
    }
}
