package com.android.photos;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import com.android.gallery3d.R;
import com.android.photos.adapters.AlbumSetCursorAdapter;
import com.android.photos.shims.LoaderCompatShim;
import com.android.photos.shims.MediaSetLoader;
import java.util.ArrayList;

public class AlbumSetFragment extends MultiSelectGridFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private AlbumSetCursorAdapter mAdapter;
    private LoaderCompatShim<Cursor> mLoaderCompatShim;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mAdapter = new AlbumSetCursorAdapter(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        View viewOnCreateView = super.onCreateView(layoutInflater, viewGroup, bundle);
        getLoaderManager().initLoader(1, null, this);
        return viewOnCreateView;
    }

    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        getGridView().setColumnWidth(getActivity().getResources().getDimensionPixelSize(R.dimen.album_set_item_width));
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        MediaSetLoader mediaSetLoader = new MediaSetLoader(getActivity());
        this.mAdapter.setDrawableFactory(mediaSetLoader);
        this.mLoaderCompatShim = mediaSetLoader;
        return mediaSetLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        this.mAdapter.swapCursor(cursor);
        setAdapter(this.mAdapter);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    @Override
    public void onGridItemClick(GridView gridView, View view, int i, long j) {
        if (this.mLoaderCompatShim == null) {
            return;
        }
        Cursor cursor = (Cursor) getItemAtPosition(i);
        Activity activity = getActivity();
        Intent intent = new Intent(activity, (Class<?>) AlbumActivity.class);
        intent.putExtra("AlbumUri", this.mLoaderCompatShim.getPathForItem(cursor).toString());
        intent.putExtra("AlbumTitle", cursor.getString(1));
        activity.startActivity(intent);
    }

    @Override
    public int getItemMediaType(Object obj) {
        return 0;
    }

    @Override
    public int getItemSupportedOperations(Object obj) {
        return ((Cursor) obj).getInt(8);
    }

    @Override
    public ArrayList<Uri> getSubItemUrisForItem(Object obj) {
        return this.mLoaderCompatShim.urisForSubItems((Cursor) obj);
    }

    @Override
    public void deleteItemWithPath(Object obj) {
        this.mLoaderCompatShim.deleteItemWithPath(obj);
    }

    @Override
    public Uri getItemUri(Object obj) {
        return this.mLoaderCompatShim.uriForItem((Cursor) obj);
    }

    @Override
    public Object getPathForItem(Object obj) {
        return this.mLoaderCompatShim.getPathForItem((Cursor) obj);
    }
}
