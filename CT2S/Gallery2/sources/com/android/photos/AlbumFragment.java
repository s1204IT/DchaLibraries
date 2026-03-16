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
import android.widget.ImageView;
import android.widget.TextView;
import com.android.gallery3d.R;
import com.android.photos.adapters.PhotoThumbnailAdapter;
import com.android.photos.shims.LoaderCompatShim;
import com.android.photos.shims.MediaItemsLoader;
import com.android.photos.views.HeaderGridView;
import java.util.ArrayList;

public class AlbumFragment extends MultiSelectGridFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private PhotoThumbnailAdapter mAdapter;
    private String mAlbumPath;
    private String mAlbumTitle;
    private View mHeaderView;
    private LoaderCompatShim<Cursor> mLoaderCompatShim;
    private ArrayList<Uri> mSubItemUriTemp = new ArrayList<>(1);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = getActivity();
        this.mAdapter = new PhotoThumbnailAdapter(context);
        Bundle args = getArguments();
        if (args != null) {
            this.mAlbumPath = args.getString("AlbumUri", null);
            this.mAlbumTitle = args.getString("AlbumTitle", null);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getLoaderManager().initLoader(1, null, this);
        return inflater.inflate(R.layout.album_content, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getGridView().setColumnWidth(MediaItemsLoader.getThumbnailSize());
    }

    private void updateHeaderView() {
        if (this.mHeaderView == null) {
            this.mHeaderView = LayoutInflater.from(getActivity()).inflate(R.layout.album_header, (ViewGroup) getGridView(), false);
            ((HeaderGridView) getGridView()).addHeaderView(this.mHeaderView, null, false);
            this.mHeaderView.setMinimumHeight(200);
        }
        ImageView iv = (ImageView) this.mHeaderView.findViewById(R.id.album_header_image);
        TextView title = (TextView) this.mHeaderView.findViewById(R.id.album_header_title);
        TextView subtitle = (TextView) this.mHeaderView.findViewById(R.id.album_header_subtitle);
        title.setText(this.mAlbumTitle);
        int count = this.mAdapter.getCount();
        subtitle.setText(getActivity().getResources().getQuantityString(R.plurals.number_of_photos, count, Integer.valueOf(count)));
        if (count > 0) {
            iv.setImageDrawable(this.mLoaderCompatShim.drawableForItem(this.mAdapter.getItem(0), null));
        }
    }

    @Override
    public void onGridItemClick(GridView g, View v, int position, long id) {
        if (this.mLoaderCompatShim != null) {
            Cursor item = (Cursor) getItemAtPosition(position);
            Uri uri = this.mLoaderCompatShim.uriForItem(item);
            Intent intent = new Intent("android.intent.action.VIEW", uri);
            intent.setClass(getActivity(), com.android.gallery3d.app.GalleryActivity.class);
            startActivity(intent);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        MediaItemsLoader loader = new MediaItemsLoader(getActivity(), this.mAlbumPath);
        this.mLoaderCompatShim = loader;
        this.mAdapter.setDrawableFactory(this.mLoaderCompatShim);
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        this.mAdapter.swapCursor(data);
        updateHeaderView();
        setAdapter(this.mAdapter);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    @Override
    public int getItemMediaType(Object item) {
        return ((Cursor) item).getInt(5);
    }

    @Override
    public int getItemSupportedOperations(Object item) {
        return ((Cursor) item).getInt(6);
    }

    @Override
    public ArrayList<Uri> getSubItemUrisForItem(Object item) {
        this.mSubItemUriTemp.clear();
        this.mSubItemUriTemp.add(this.mLoaderCompatShim.uriForItem((Cursor) item));
        return this.mSubItemUriTemp;
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
