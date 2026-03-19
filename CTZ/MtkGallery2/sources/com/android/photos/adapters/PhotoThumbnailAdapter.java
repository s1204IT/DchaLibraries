package com.android.photos.adapters;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import com.android.gallery3d.R;
import com.android.photos.shims.LoaderCompatShim;
import com.android.photos.views.GalleryThumbnailView$GalleryThumbnailAdapter;

public class PhotoThumbnailAdapter extends CursorAdapter implements GalleryThumbnailView$GalleryThumbnailAdapter {
    private LoaderCompatShim<Cursor> mDrawableFactory;
    private LayoutInflater mInflater;

    public PhotoThumbnailAdapter(Context context) {
        super(context, (Cursor) null, false);
        this.mInflater = LayoutInflater.from(context);
    }

    public void setDrawableFactory(LoaderCompatShim<Cursor> loaderCompatShim) {
        this.mDrawableFactory = loaderCompatShim;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ImageView imageView = (ImageView) view.findViewById(R.id.thumbnail);
        Drawable drawable = imageView.getDrawable();
        Drawable drawableDrawableForItem = this.mDrawableFactory.drawableForItem(cursor, drawable);
        if (drawable != drawableDrawableForItem) {
            imageView.setImageDrawable(drawableDrawableForItem);
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        return this.mInflater.inflate(R.layout.photo_set_item, viewGroup, false);
    }

    @Override
    public Cursor getItem(int i) {
        return (Cursor) super.getItem(i);
    }
}
