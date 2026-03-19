package com.mediatek.gallery3d.video;

import com.mediatek.gallery3d.ext.DefaultActivityHooker;

public class MovieHooker extends DefaultActivityHooker {
    private IMoviePlayer mPlayer;

    @Override
    public void setParameter(String str, Object obj) {
        super.setParameter(str, obj);
        if (obj instanceof IMoviePlayer) {
            this.mPlayer = (IMoviePlayer) obj;
            onMoviePlayerChanged(this.mPlayer);
        }
    }

    public IMoviePlayer getPlayer() {
        return this.mPlayer;
    }

    public void onMoviePlayerChanged(IMoviePlayer iMoviePlayer) {
    }
}
