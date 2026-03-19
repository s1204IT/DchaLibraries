package com.android.photos;

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
import com.android.photos.adapters.PhotoThumbnailAdapter;
import com.android.photos.shims.LoaderCompatShim;
import com.android.photos.shims.MediaItemsLoader;
import java.util.ArrayList;

public class PhotoSetFragment extends MultiSelectGridFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private PhotoThumbnailAdapter mAdapter;
    private LoaderCompatShim<Cursor> mLoaderCompatShim;
    private ArrayList<Uri> mSubItemUriTemp = new ArrayList<>(1);

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mAdapter = new PhotoThumbnailAdapter(getActivity());
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
        getGridView().setColumnWidth(MediaItemsLoader.getThumbnailSize());
    }

    @Override
    public void onGridItemClick(GridView gridView, View view, int i, long j) {
        if (this.mLoaderCompatShim == null) {
            return;
        }
        Intent intent = new Intent("android.intent.action.VIEW", this.mLoaderCompatShim.uriForItem((Cursor) getItemAtPosition(i)));
        intent.setClass(getActivity(), com.android.gallery3d.app.GalleryActivity.class);
        startActivity(intent);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        MediaItemsLoader mediaItemsLoader = new MediaItemsLoader(getActivity());
        this.mLoaderCompatShim = mediaItemsLoader;
        this.mAdapter.setDrawableFactory(this.mLoaderCompatShim);
        return mediaItemsLoader;
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
    public int getItemMediaType(Object obj) {
        return ((Cursor) obj).getInt(5);
    }

    @Override
    public int getItemSupportedOperations(Object obj) {
        return ((Cursor) obj).getInt(6);
    }

    @Override
    public ArrayList<Uri> getSubItemUrisForItem(Object obj) {
        this.mSubItemUriTemp.clear();
        this.mSubItemUriTemp.add(this.mLoaderCompatShim.uriForItem((Cursor) obj));
        return this.mSubItemUriTemp;
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
