package com.android.photos;

import android.app.LoaderManager;
import android.content.Context;
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = getActivity();
        this.mAdapter = new AlbumSetCursorAdapter(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);
        getLoaderManager().initLoader(1, null, this);
        return root;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getGridView().setColumnWidth(getActivity().getResources().getDimensionPixelSize(R.dimen.album_set_item_width));
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        MediaSetLoader loader = new MediaSetLoader(getActivity());
        this.mAdapter.setDrawableFactory(loader);
        this.mLoaderCompatShim = loader;
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        this.mAdapter.swapCursor(data);
        setAdapter(this.mAdapter);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    @Override
    public void onGridItemClick(GridView g, View v, int position, long id) {
        if (this.mLoaderCompatShim != null) {
            Cursor item = (Cursor) getItemAtPosition(position);
            Context context = getActivity();
            Intent intent = new Intent(context, (Class<?>) AlbumActivity.class);
            intent.putExtra("AlbumUri", this.mLoaderCompatShim.getPathForItem(item).toString());
            intent.putExtra("AlbumTitle", item.getString(1));
            context.startActivity(intent);
        }
    }

    @Override
    public int getItemMediaType(Object item) {
        return 0;
    }

    @Override
    public int getItemSupportedOperations(Object item) {
        return ((Cursor) item).getInt(8);
    }

    @Override
    public ArrayList<Uri> getSubItemUrisForItem(Object item) {
        return this.mLoaderCompatShim.urisForSubItems((Cursor) item);
    }

    @Override
    public void deleteItemWithPath(Object itemPath) {
        this.mLoaderCompatShim.deleteItemWithPath(itemPath);
    }

    @Override
    public Uri getItemUri(Object item) {
        return this.mLoaderCompatShim.uriForItem((Cursor) item);
    }

    @Override
    public Object getPathForItem(Object item) {
        return this.mLoaderCompatShim.getPathForItem((Cursor) item);
    }
}
