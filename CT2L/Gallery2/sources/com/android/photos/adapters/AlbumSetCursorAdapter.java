package com.android.photos.adapters;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.gallery3d.R;
import com.android.photos.shims.LoaderCompatShim;

public class AlbumSetCursorAdapter extends CursorAdapter {
    private LoaderCompatShim<Cursor> mDrawableFactory;

    public void setDrawableFactory(LoaderCompatShim<Cursor> factory) {
        this.mDrawableFactory = factory;
    }

    public AlbumSetCursorAdapter(Context context) {
        super(context, (Cursor) null, false);
    }

    @Override
    public void bindView(View v, Context context, Cursor cursor) {
        TextView titleTextView = (TextView) v.findViewById(R.id.album_set_item_title);
        titleTextView.setText(cursor.getString(1));
        TextView countTextView = (TextView) v.findViewById(R.id.album_set_item_count);
        int count = cursor.getInt(7);
        countTextView.setText(context.getResources().getQuantityString(R.plurals.number_of_photos, count, Integer.valueOf(count)));
        ImageView thumbImageView = (ImageView) v.findViewById(R.id.album_set_item_image);
        Drawable recycle = thumbImageView.getDrawable();
        Drawable drawable = this.mDrawableFactory.drawableForItem(cursor, recycle);
        if (recycle != drawable) {
            thumbImageView.setImageDrawable(drawable);
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.album_set_item, parent, false);
    }
}
