package com.android.musicvis.vis5;

import android.os.Handler;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.Matrix4f;
import android.renderscript.Mesh;
import android.renderscript.ProgramFragment;
import android.renderscript.ProgramFragmentFixedFunction;
import android.renderscript.ProgramRaster;
import android.renderscript.ProgramStore;
import android.renderscript.ProgramVertex;
import android.renderscript.ProgramVertexFixedFunction;
import android.renderscript.Sampler;
import android.renderscript.ScriptC;
import com.android.musicvis.AudioCapture;
import com.android.musicvis.R;
import com.android.musicvis.RenderScriptScene;

class Visualization5RS extends RenderScriptScene {
    private AudioCapture mAudioCapture;
    private Mesh mCubeMesh;
    private final Runnable mDrawCube;
    private final Handler mHandler;
    private short[] mIndexData;
    private Allocation mLineIdxAlloc;
    private int mNeedleMass;
    private int mNeedlePos;
    private int mNeedleSpeed;
    private ProgramVertexFixedFunction.Constants mPVAlloc;
    private ProgramVertex mPVBackground;
    private ProgramFragment mPfBackgroundMip;
    private ProgramFragment mPfBackgroundNoMip;
    private ProgramStore mPfsBackground;
    protected Allocation mPointAlloc;
    protected float[] mPointData;
    private ProgramRaster mPr;
    private Sampler mSamplerMip;
    private Sampler mSamplerNoMip;
    ScriptC_many mScript;
    private int mSpringForceAtOrigin;
    private Allocation[] mTextures;
    private ScriptField_Vertex mVertexBuffer;
    private boolean mVisible;
    private int[] mVizData;
    WorldState mWorldState;

    static class WorldState {
        public float mAngle;
        public int mIdle;
        public int mPeak;
        public float mRotate;
        public float mTilt;
        public int mWaveCounter;

        WorldState() {
        }
    }

    Visualization5RS(int width, int height) {
        super(width, height);
        this.mHandler = new Handler();
        this.mDrawCube = new Runnable() {
            @Override
            public void run() {
                Visualization5RS.this.updateWave();
            }
        };
        this.mNeedlePos = 0;
        this.mNeedleSpeed = 0;
        this.mNeedleMass = 10;
        this.mSpringForceAtOrigin = 200;
        this.mWorldState = new WorldState();
        this.mPointData = new float[2048];
        this.mIndexData = new short[512];
        this.mAudioCapture = null;
        this.mVizData = new int[1024];
        this.mWidth = width;
        this.mHeight = height;
        int outlen = this.mPointData.length / 8;
        int half = outlen / 2;
        for (int i = 0; i < outlen; i++) {
            this.mPointData[i * 8] = i - half;
            this.mPointData[(i * 8) + 2] = 0.0f;
            this.mPointData[(i * 8) + 3] = 0.0f;
            this.mPointData[(i * 8) + 4] = i - half;
            this.mPointData[(i * 8) + 6] = 1.0f;
            this.mPointData[(i * 8) + 7] = 0.0f;
        }
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        if (this.mPVAlloc != null) {
            Matrix4f proj = new Matrix4f();
            proj.loadProjectionNormalized(width, height);
            this.mPVAlloc.setProjection(proj);
        }
        this.mWorldState.mTilt = -20.0f;
    }

    @Override
    public void setOffset(float xOffset, float yOffset, int xPixels, int yPixels) {
        this.mWorldState.mRotate = (xOffset - 0.5f) * 90.0f;
        updateWorldState();
    }

    @Override
    protected ScriptC createScript() {
        this.mScript = new ScriptC_many(this.mRS, this.mResources, R.raw.many);
        ProgramVertexFixedFunction.Builder pvb = new ProgramVertexFixedFunction.Builder(this.mRS);
        this.mPVBackground = pvb.create();
        this.mPVAlloc = new ProgramVertexFixedFunction.Constants(this.mRS);
        this.mPVBackground.bindConstants(this.mPVAlloc);
        Matrix4f proj = new Matrix4f();
        proj.loadProjectionNormalized(this.mWidth, this.mHeight);
        this.mPVAlloc.setProjection(proj);
        this.mScript.set_gPVBackground(this.mPVBackground);
        this.mTextures = new Allocation[8];
        this.mTextures[0] = Allocation.createFromBitmapResource(this.mRS, this.mResources, R.drawable.background, Allocation.MipmapControl.MIPMAP_ON_SYNC_TO_TEXTURE, 2);
        this.mScript.set_gTvumeter_background(this.mTextures[0]);
        this.mTextures[1] = Allocation.createFromBitmapResource(this.mRS, this.mResources, R.drawable.frame, Allocation.MipmapControl.MIPMAP_ON_SYNC_TO_TEXTURE, 2);
        this.mScript.set_gTvumeter_frame(this.mTextures[1]);
        this.mTextures[2] = Allocation.createFromBitmapResource(this.mRS, this.mResources, R.drawable.peak_on, Allocation.MipmapControl.MIPMAP_ON_SYNC_TO_TEXTURE, 2);
        this.mScript.set_gTvumeter_peak_on(this.mTextures[2]);
        this.mTextures[3] = Allocation.createFromBitmapResource(this.mRS, this.mResources, R.drawable.peak_off, Allocation.MipmapControl.MIPMAP_ON_SYNC_TO_TEXTURE, 2);
        this.mScript.set_gTvumeter_peak_off(this.mTextures[3]);
        this.mTextures[4] = Allocation.createFromBitmapResource(this.mRS, this.mResources, R.drawable.needle, Allocation.MipmapControl.MIPMAP_ON_SYNC_TO_TEXTURE, 2);
        this.mScript.set_gTvumeter_needle(this.mTextures[4]);
        this.mTextures[5] = Allocation.createFromBitmapResource(this.mRS, this.mResources, R.drawable.black, Allocation.MipmapControl.MIPMAP_ON_SYNC_TO_TEXTURE, 2);
        this.mScript.set_gTvumeter_black(this.mTextures[5]);
        this.mTextures[6] = Allocation.createFromBitmapResource(this.mRS, this.mResources, R.drawable.albumart, Allocation.MipmapControl.MIPMAP_ON_SYNC_TO_TEXTURE, 2);
        this.mScript.set_gTvumeter_album(this.mTextures[6]);
        this.mTextures[7] = Allocation.createFromBitmapResource(this.mRS, this.mResources, R.drawable.fire, Allocation.MipmapControl.MIPMAP_ON_SYNC_TO_TEXTURE, 2);
        this.mScript.set_gTlinetexture(this.mTextures[7]);
        Sampler.Builder builder = new Sampler.Builder(this.mRS);
        builder.setMinification(Sampler.Value.LINEAR);
        builder.setMagnification(Sampler.Value.LINEAR);
        builder.setWrapS(Sampler.Value.WRAP);
        builder.setWrapT(Sampler.Value.WRAP);
        this.mSamplerNoMip = builder.create();
        Sampler.Builder builder2 = new Sampler.Builder(this.mRS);
        builder2.setMinification(Sampler.Value.LINEAR_MIP_LINEAR);
        builder2.setMagnification(Sampler.Value.LINEAR);
        builder2.setWrapS(Sampler.Value.WRAP);
        builder2.setWrapT(Sampler.Value.WRAP);
        this.mSamplerMip = builder2.create();
        ProgramFragmentFixedFunction.Builder builder3 = new ProgramFragmentFixedFunction.Builder(this.mRS);
        builder3.setTexture(ProgramFragmentFixedFunction.Builder.EnvMode.REPLACE, ProgramFragmentFixedFunction.Builder.Format.RGBA, 0);
        this.mPfBackgroundNoMip = builder3.create();
        this.mPfBackgroundNoMip.bindSampler(this.mSamplerNoMip, 0);
        this.mScript.set_gPFBackgroundNoMip(this.mPfBackgroundNoMip);
        ProgramFragmentFixedFunction.Builder builder4 = new ProgramFragmentFixedFunction.Builder(this.mRS);
        builder4.setTexture(ProgramFragmentFixedFunction.Builder.EnvMode.REPLACE, ProgramFragmentFixedFunction.Builder.Format.RGBA, 0);
        this.mPfBackgroundMip = builder4.create();
        this.mPfBackgroundMip.bindSampler(this.mSamplerMip, 0);
        this.mScript.set_gPFBackgroundMip(this.mPfBackgroundMip);
        ProgramRaster.Builder builder5 = new ProgramRaster.Builder(this.mRS);
        builder5.setCullMode(ProgramRaster.CullMode.NONE);
        this.mPr = builder5.create();
        this.mScript.set_gPR(this.mPr);
        ProgramStore.Builder builder6 = new ProgramStore.Builder(this.mRS);
        builder6.setDepthFunc(ProgramStore.DepthFunc.EQUAL);
        builder6.setBlendFunc(ProgramStore.BlendSrcFunc.ONE, ProgramStore.BlendDstFunc.ONE_MINUS_SRC_ALPHA);
        builder6.setDitherEnabled(true);
        builder6.setDepthMaskEnabled(false);
        this.mPfsBackground = builder6.create();
        this.mScript.set_gPFSBackground(this.mPfsBackground);
        this.mVertexBuffer = new ScriptField_Vertex(this.mRS, this.mPointData.length / 4);
        Mesh.AllocationBuilder meshBuilder = new Mesh.AllocationBuilder(this.mRS);
        meshBuilder.addVertexAllocation(this.mVertexBuffer.getAllocation());
        this.mLineIdxAlloc = Allocation.createSized(this.mRS, Element.U16(this.mRS), this.mIndexData.length, 5);
        meshBuilder.addIndexSetAllocation(this.mLineIdxAlloc, Mesh.Primitive.LINE);
        this.mCubeMesh = meshBuilder.create();
        this.mPointAlloc = this.mVertexBuffer.getAllocation();
        this.mScript.bind_gPoints(this.mVertexBuffer);
        this.mScript.set_gPointBuffer(this.mPointAlloc);
        this.mScript.set_gCubeMesh(this.mCubeMesh);
        updateWave();
        for (int i = 0; i < this.mIndexData.length; i++) {
            this.mIndexData[i] = (short) i;
        }
        this.mPointAlloc.copyFromUnchecked(this.mPointData);
        this.mLineIdxAlloc.copyFrom(this.mIndexData);
        this.mLineIdxAlloc.syncAll(1);
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
            if (len == 0) {
                if (this.mWorldState.mIdle == 0) {
                    this.mWorldState.mIdle = 1;
                }
            } else {
                if (this.mWorldState.mIdle != 0) {
                    this.mWorldState.mIdle = 0;
                }
                int outlen = this.mPointData.length / 8;
                int len2 = len / 4;
                if (len2 > outlen) {
                    len2 = outlen;
                }
                for (int i2 = 0; i2 < len2; i2++) {
                    int amp = this.mVizData[i2 * 4] + this.mVizData[(i2 * 4) + 1] + this.mVizData[(i2 * 4) + 2] + this.mVizData[(i2 * 4) + 3];
                    this.mPointData[(i2 * 8) + 1] = amp;
                    this.mPointData[(i2 * 8) + 5] = -amp;
                }
                this.mPointAlloc.copyFromUnchecked(this.mPointData);
                this.mWorldState.mWaveCounter++;
            }
            updateWorldState();
        }
    }

    protected void updateWorldState() {
        this.mScript.set_gAngle(this.mWorldState.mAngle);
        this.mScript.set_gPeak(this.mWorldState.mPeak);
        this.mScript.set_gRotate(this.mWorldState.mRotate);
        this.mScript.set_gTilt(this.mWorldState.mTilt);
        this.mScript.set_gIdle(this.mWorldState.mIdle);
        this.mScript.set_gWaveCounter(this.mWorldState.mWaveCounter);
    }
}
