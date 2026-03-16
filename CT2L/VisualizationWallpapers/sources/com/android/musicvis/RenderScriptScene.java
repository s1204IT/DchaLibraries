package com.android.musicvis;

import android.content.res.Resources;
import android.renderscript.RenderScriptGL;
import android.renderscript.Script;
import android.renderscript.ScriptC;

public abstract class RenderScriptScene {
    protected int mHeight;
    protected boolean mPreview;
    protected RenderScriptGL mRS;
    protected Resources mResources;
    protected ScriptC mScript;
    protected int mWidth;

    protected abstract ScriptC createScript();

    public RenderScriptScene(int width, int height) {
        this.mWidth = width;
        this.mHeight = height;
    }

    public void init(RenderScriptGL rs, Resources res, boolean isPreview) {
        this.mRS = rs;
        this.mResources = res;
        this.mPreview = isPreview;
        this.mScript = createScript();
    }

    public void stop() {
        this.mRS.bindRootScript((Script) null);
    }

    public void start() {
        this.mRS.bindRootScript(this.mScript);
    }

    public void resize(int width, int height) {
        this.mWidth = width;
        this.mHeight = height;
    }

    public void setOffset(float xOffset, float yOffset, int xPixels, int yPixels) {
    }
}
