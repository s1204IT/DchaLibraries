package com.mediatek.gallery3d.video;

import android.content.Context;

public interface IMovieDrmExtension {

    public interface IMovieDrmCallback {
        void onContinue();

        void onStop();
    }

    boolean canShare(Context context, IMovieItem iMovieItem);

    boolean handleDrmFile(Context context, IMovieItem iMovieItem, IMovieDrmCallback iMovieDrmCallback);
}
