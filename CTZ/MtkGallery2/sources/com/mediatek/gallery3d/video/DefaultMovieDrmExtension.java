package com.mediatek.gallery3d.video;

import android.content.Context;
import com.mediatek.gallery3d.video.IMovieDrmExtension;

public class DefaultMovieDrmExtension implements IMovieDrmExtension {
    @Override
    public boolean handleDrmFile(Context context, IMovieItem iMovieItem, IMovieDrmExtension.IMovieDrmCallback iMovieDrmCallback) {
        return false;
    }

    @Override
    public boolean canShare(Context context, IMovieItem iMovieItem) {
        return true;
    }
}
