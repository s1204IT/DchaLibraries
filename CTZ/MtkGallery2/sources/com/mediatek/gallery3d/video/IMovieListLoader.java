package com.mediatek.gallery3d.video;

import android.content.Context;
import android.content.Intent;

public interface IMovieListLoader {

    public interface LoaderListener {
        void onListLoaded(IMovieList iMovieList);
    }

    void cancelList();

    void fillVideoList(Context context, Intent intent, LoaderListener loaderListener, IMovieItem iMovieItem);
}
