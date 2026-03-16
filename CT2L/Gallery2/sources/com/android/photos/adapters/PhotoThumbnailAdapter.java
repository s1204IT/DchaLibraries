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

    public void setDrawableFactory(LoaderCompatShim<Cursor> factory) {
        this.mDrawableFactory = factory;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ImageView iv = (ImageView) view.findViewById(R.id.thumbnail);
        Drawable recycle = iv.getDrawable();
        Drawable drawable = this.mDrawableFactory.drawableForItem(cursor, recycle);
        if (recycle != drawable) {
            iv.setImageDrawable(drawable);
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = this.mInflater.inflate(R.layout.photo_set_item, parent, false);
        return view;
    }

    @Override
    public Cursor getItem(int position) {
        return (Cursor) super.getItem(position);
    }
}
