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
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mAdapter = new PhotoThumbnailAdapter(getActivity());
        Bundle arguments = getArguments();
        if (arguments != null) {
            this.mAlbumPath = arguments.getString("AlbumUri", null);
            this.mAlbumTitle = arguments.getString("AlbumTitle", null);
        }
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        getLoaderManager().initLoader(1, null, this);
        return layoutInflater.inflate(R.layout.album_content, viewGroup, false);
    }

    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        getGridView().setColumnWidth(MediaItemsLoader.getThumbnailSize());
    }

    private void updateHeaderView() {
        if (this.mHeaderView == null) {
            this.mHeaderView = LayoutInflater.from(getActivity()).inflate(R.layout.album_header, (ViewGroup) getGridView(), false);
            ((HeaderGridView) getGridView()).addHeaderView(this.mHeaderView, null, false);
            this.mHeaderView.setMinimumHeight(200);
        }
        ImageView imageView = (ImageView) this.mHeaderView.findViewById(R.id.album_header_image);
        TextView textView = (TextView) this.mHeaderView.findViewById(R.id.album_header_title);
        TextView textView2 = (TextView) this.mHeaderView.findViewById(R.id.album_header_subtitle);
        textView.setText(this.mAlbumTitle);
        int count = this.mAdapter.getCount();
        textView2.setText(getActivity().getResources().getQuantityString(R.plurals.number_of_photos, count, Integer.valueOf(count)));
        if (count > 0) {
            imageView.setImageDrawable(this.mLoaderCompatShim.drawableForItem(this.mAdapter.getItem(0), null));
        }
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
        MediaItemsLoader mediaItemsLoader = new MediaItemsLoader(getActivity(), this.mAlbumPath);
        this.mLoaderCompatShim = mediaItemsLoader;
        this.mAdapter.setDrawableFactory(this.mLoaderCompatShim);
        return mediaItemsLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        this.mAdapter.swapCursor(cursor);
        updateHeaderView();
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
