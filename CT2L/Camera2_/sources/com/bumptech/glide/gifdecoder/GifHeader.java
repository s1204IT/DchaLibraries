package com.bumptech.glide.gifdecoder;

import java.util.ArrayList;
import java.util.List;

public class GifHeader {
    public int bgColor;
    public int bgIndex;
    public GifFrame currentFrame;
    public boolean gctFlag;
    public int gctSize;
    public int height;
    public boolean isTransparent;
    public int loopCount;
    public int pixelAspect;
    public int width;
    public int[] gct = null;
    public int status = 0;
    public int frameCount = 0;
    public List<GifFrame> frames = new ArrayList();
}
