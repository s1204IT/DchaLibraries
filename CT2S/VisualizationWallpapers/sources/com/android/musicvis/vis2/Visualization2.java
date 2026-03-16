package com.android.musicvis.vis2;

import com.android.musicvis.RenderScriptWallpaper;

public class Visualization2 extends RenderScriptWallpaper<Visualization2RS> {
    @Override
    protected Visualization2RS createScene(int width, int height) {
        return new Visualization2RS(width, height);
    }
}
