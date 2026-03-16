package com.android.musicvis.vis4;

import android.os.Handler;
import android.renderscript.Allocation;
import android.renderscript.Matrix4f;
import android.renderscript.ProgramFragment;
import android.renderscript.ProgramFragmentFixedFunction;
import android.renderscript.ProgramStore;
import android.renderscript.ProgramVertex;
import android.renderscript.ProgramVertexFixedFunction;
import android.renderscript.Sampler;
import android.renderscript.ScriptC;
import com.android.musicvis.AudioCapture;
import com.android.musicvis.R;
import com.android.musicvis.RenderScriptScene;
import java.util.TimeZone;

class Visualization4RS extends RenderScriptScene {
    private AudioCapture mAudioCapture;
    private final Runnable mDrawCube;
    private final Handler mHandler;
    private int mNeedleMass;
    private int mNeedlePos;
    private int mNeedleSpeed;
    private ProgramVertexFixedFunction.Constants mPVAlloc;
    private ProgramVertex mPVBackground;
    private ProgramFragment mPfBackground;
    private ProgramStore mPfsBackground;
    private Sampler mSampler;
    ScriptC_vu mScript;
    private int mSpringForceAtOrigin;
    private Allocation[] mTextures;
    private boolean mVisible;
    private int[] mVizData;
    WorldState mWorldState;

    static class WorldState {
        public float mAngle;
        public int mPeak;

        WorldState() {
        }
    }

    Visualization4RS(int width, int height) {
        super(width, height);
        this.mHandler = new Handler();
        this.mDrawCube = new Runnable() {
            @Override
            public void run() {
                Visualization4RS.this.updateWave();
            }
        };
        this.mNeedlePos = 0;
        this.mNeedleSpeed = 0;
        this.mNeedleMass = 10;
        this.mSpringForceAtOrigin = 200;
        this.mWorldState = new WorldState();
        this.mAudioCapture = null;
        this.mVizData = new int[1024];
        this.mWidth = width;
        this.mHeight = height;
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        if (this.mPVAlloc != null) {
            Matrix4f proj = new Matrix4f();
            proj.loadProjectionNormalized(width, height);
            this.mPVAlloc.setProjection(proj);
        }
    }

    @Override
    protected ScriptC createScript() {
        this.mScript = new ScriptC_vu(this.mRS, this.mResources, R.raw.vu);
        ProgramVertexFixedFunction.Builder pvb = new ProgramVertexFixedFunction.Builder(this.mRS);
        this.mPVBackground = pvb.create();
        this.mPVAlloc = new ProgramVertexFixedFunction.Constants(this.mRS);
        this.mPVBackground.bindConstants(this.mPVAlloc);
        Matrix4f proj = new Matrix4f();
        proj.loadProjectionNormalized(this.mWidth, this.mHeight);
        this.mPVAlloc.setProjection(proj);
        this.mScript.set_gPVBackground(this.mPVBackground);
        updateWave();
        this.mTextures = new Allocation[6];
        this.mTextures[0] = Allocation.createFromBitmapResource(this.mRS, this.mResources, R.drawable.background, Allocation.MipmapControl.MIPMAP_NONE, 2);
        this.mScript.set_gTvumeter_background(this.mTextures[0]);
        this.mTextures[1] = Allocation.createFromBitmapResource(this.mRS, this.mResources, R.drawable.frame, Allocation.MipmapControl.MIPMAP_NONE, 2);
        this.mScript.set_gTvumeter_frame(this.mTextures[1]);
        this.mTextures[2] = Allocation.createFromBitmapResource(this.mRS, this.mResources, R.drawable.peak_on, Allocation.MipmapControl.MIPMAP_NONE, 2);
        this.mScript.set_gTvumeter_peak_on(this.mTextures[2]);
        this.mTextures[3] = Allocation.createFromBitmapResource(this.mRS, this.mResources, R.drawable.peak_off, Allocation.MipmapControl.MIPMAP_NONE, 2);
        this.mScript.set_gTvumeter_peak_off(this.mTextures[3]);
        this.mTextures[4] = Allocation.createFromBitmapResource(this.mRS, this.mResources, R.drawable.needle, Allocation.MipmapControl.MIPMAP_NONE, 2);
        this.mScript.set_gTvumeter_needle(this.mTextures[4]);
        this.mTextures[5] = Allocation.createFromBitmapResource(this.mRS, this.mResources, R.drawable.black, Allocation.MipmapControl.MIPMAP_NONE, 2);
        this.mScript.set_gTvumeter_black(this.mTextures[5]);
        Sampler.Builder samplerBuilder = new Sampler.Builder(this.mRS);
        samplerBuilder.setMinification(Sampler.Value.LINEAR);
        samplerBuilder.setMagnification(Sampler.Value.LINEAR);
        samplerBuilder.setWrapS(Sampler.Value.WRAP);
        samplerBuilder.setWrapT(Sampler.Value.WRAP);
        this.mSampler = samplerBuilder.create();
        ProgramFragmentFixedFunction.Builder builder = new ProgramFragmentFixedFunction.Builder(this.mRS);
        builder.setTexture(ProgramFragmentFixedFunction.Builder.EnvMode.REPLACE, ProgramFragmentFixedFunction.Builder.Format.RGBA, 0);
        this.mPfBackground = builder.create();
        this.mPfBackground.bindSampler(this.mSampler, 0);
        this.mScript.set_gPFBackground(this.mPfBackground);
        ProgramStore.Builder builder2 = new ProgramStore.Builder(this.mRS);
        builder2.setDepthFunc(ProgramStore.DepthFunc.ALWAYS);
        builder2.setBlendFunc(ProgramStore.BlendSrcFunc.ONE, ProgramStore.BlendDstFunc.ONE_MINUS_SRC_ALPHA);
        builder2.setDitherEnabled(true);
        builder2.setDepthMaskEnabled(false);
        this.mPfsBackground = builder2.create();
        this.mScript.set_gPFSBackground(this.mPfsBackground);
        this.mScript.setTimeZone(TimeZone.getDefault().getID());
        return this.mScript;
    }

    @Override
    public void start() {
        super.start();
        this.mVisible = true;
        if (this.mAudioCapture == null) {
            this.mAudioCapture = new AudioCapture(0, 1024);
        }
        this.mAudioCapture.start();
        updateWave();
    }

    @Override
    public void stop() {
        super.stop();
        this.mVisible = false;
        if (this.mAudioCapture != null) {
            this.mAudioCapture.stop();
            this.mAudioCapture.release();
            this.mAudioCapture = null;
        }
    }

    void updateWave() {
        this.mHandler.removeCallbacks(this.mDrawCube);
        if (this.mVisible) {
            this.mHandler.postDelayed(this.mDrawCube, 20L);
            int len = 0;
            if (this.mAudioCapture != null) {
                this.mVizData = this.mAudioCapture.getFormattedData(512, 1);
                len = this.mVizData.length;
            }
            int volt = 0;
            if (len > 0) {
                for (int i = 0; i < len; i++) {
                    int val = this.mVizData[i];
                    if (val < 0) {
                        val = -val;
                    }
                    volt += val;
                }
                volt /= len;
            }
            int netforce = (volt - (this.mNeedleSpeed * 3)) - (this.mNeedlePos + this.mSpringForceAtOrigin);
            int acceleration = netforce / this.mNeedleMass;
            this.mNeedleSpeed += acceleration;
            this.mNeedlePos += this.mNeedleSpeed;
            if (this.mNeedlePos < 0) {
                this.mNeedlePos = 0;
                this.mNeedleSpeed = 0;
            } else if (this.mNeedlePos > 32767) {
                if (this.mNeedlePos > 33333) {
                    this.mWorldState.mPeak = 10;
                }
                this.mNeedlePos = 32767;
                this.mNeedleSpeed = 0;
            }
            if (this.mWorldState.mPeak > 0) {
                WorldState worldState = this.mWorldState;
                worldState.mPeak--;
            }
            this.mWorldState.mAngle = 131.0f - (this.mNeedlePos / 410.0f);
            this.mScript.set_gAngle(this.mWorldState.mAngle);
            this.mScript.set_gPeak(this.mWorldState.mPeak);
        }
    }
}
