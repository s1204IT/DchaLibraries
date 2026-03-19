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

    public void setDrawableFactory(LoaderCompatShim<Cursor> loaderCompatShim) {
        this.mDrawableFactory = loaderCompatShim;
    }

    public AlbumSetCursorAdapter(Context context) {
        super(context, (Cursor) null, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ((TextView) view.findViewById(R.id.album_set_item_title)).setText(cursor.getString(1));
        TextView textView = (TextView) view.findViewById(R.id.album_set_item_count);
        int i = cursor.getInt(7);
        textView.setText(context.getResources().getQuantityString(R.plurals.number_of_photos, i, Integer.valueOf(i)));
        ImageView imageView = (ImageView) view.findViewById(R.id.album_set_item_image);
        Drawable drawable = imageView.getDrawable();
        Drawable drawableDrawableForItem = this.mDrawableFactory.drawableForItem(cursor, drawable);
        if (drawable != drawableDrawableForItem) {
            imageView.setImageDrawable(drawableDrawableForItem);
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        return LayoutInflater.from(context).inflate(R.layout.album_set_item, viewGroup, false);
    }
}
