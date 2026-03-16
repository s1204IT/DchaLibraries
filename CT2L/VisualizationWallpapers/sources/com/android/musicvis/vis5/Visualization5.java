package com.android.musicvis.vis5;

import com.android.musicvis.RenderScriptWallpaper;

public class Visualization5 extends RenderScriptWallpaper<Visualization5RS> {
    @Override
    protected Visualization5RS createScene(int width, int height) {
        return new Visualization5RS(width, height);
    }
}
