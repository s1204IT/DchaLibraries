package com.mediatek.gallerybasic.base;

import com.mediatek.gallerybasic.gl.MGLCanvas;

public abstract class PlayEngine {

    public interface OnFrameAvailableListener {
        void onFrameAvailable(int i);
    }

    public abstract boolean draw(MediaData mediaData, int i, MGLCanvas mGLCanvas, int i2, int i3);

    public abstract int getPlayHeight(int i, MediaData mediaData);

    public abstract int getPlayWidth(int i, MediaData mediaData);

    public abstract void pause();

    public abstract void resume();

    public abstract void setLayerManager(LayerManager layerManager);

    public abstract void setOnFrameAvailableListener(OnFrameAvailableListener onFrameAvailableListener);

    public abstract void updateData(MediaData[] mediaDataArr);

    public boolean isSkipAnimationWhenUpdateSize(int i) {
        return false;
    }
}
