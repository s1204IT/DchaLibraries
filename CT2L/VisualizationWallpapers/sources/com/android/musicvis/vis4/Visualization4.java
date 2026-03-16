package com.android.musicvis.vis4;

import com.android.musicvis.RenderScriptWallpaper;

public class Visualization4 extends RenderScriptWallpaper<Visualization4RS> {
    @Override
    protected Visualization4RS createScene(int width, int height) {
        return new Visualization4RS(width, height);
    }
}
