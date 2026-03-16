package com.android.musicvis;

import android.os.Handler;
import android.os.SystemClock;
import android.renderscript.Allocation;
import android.renderscript.Matrix4f;
import android.renderscript.Mesh;
import android.renderscript.ProgramFragment;
import android.renderscript.ProgramFragmentFixedFunction;
import android.renderscript.ProgramVertex;
import android.renderscript.ProgramVertexFixedFunction;
import android.renderscript.Sampler;
import android.renderscript.ScriptC;

public class GenericWaveRS extends RenderScriptScene {
    protected AudioCapture mAudioCapture;
    private Mesh mCubeMesh;
    private final Runnable mDrawCube;
    private final Handler mHandler;
    private ProgramVertexFixedFunction.Constants mPVAlloc;
    private ProgramVertex mPVBackground;
    private ProgramFragment mPfBackground;
    protected Allocation mPointAlloc;
    protected float[] mPointData;
    private Sampler mSampler;
    ScriptC_waveform mScript;
    private int mTexId;
    private Allocation mTexture;
    private ScriptField_Vertex mVertexBuffer;
    private boolean mVisible;
    protected int[] mVizData;
    protected WorldState mWorldState;

    protected static class WorldState {
        public int idle;
        public int waveCounter;
        public int width;
        public float yRotation;

        protected WorldState() {
        }
    }

    protected GenericWaveRS(int width, int height, int texid) {
        super(width, height);
        this.mHandler = new Handler();
        this.mDrawCube = new Runnable() {
            @Override
            public void run() {
                GenericWaveRS.this.updateWave();
            }
        };
        this.mWorldState = new WorldState();
        this.mPointData = new float[8192];
        this.mAudioCapture = null;
        this.mVizData = new int[1024];
        this.mTexId = texid;
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
        this.mWorldState.width = width;
        if (this.mPVAlloc != null) {
            Matrix4f proj = new Matrix4f();
            proj.loadProjectionNormalized(this.mWidth, this.mHeight);
            this.mPVAlloc.setProjection(proj);
        }
    }

    @Override
    protected ScriptC createScript() {
        this.mScript = new ScriptC_waveform(this.mRS, this.mResources, R.raw.waveform);
        this.mWorldState.yRotation = 0.0f;
        this.mWorldState.width = this.mWidth;
        updateWorldState();
        ProgramVertexFixedFunction.Builder pvb = new ProgramVertexFixedFunction.Builder(this.mRS);
        this.mPVBackground = pvb.create();
        this.mPVAlloc = new ProgramVertexFixedFunction.Constants(this.mRS);
        this.mPVBackground.bindConstants(this.mPVAlloc);
        Matrix4f proj = new Matrix4f();
        proj.loadProjectionNormalized(this.mWidth, this.mHeight);
        this.mPVAlloc.setProjection(proj);
        this.mScript.set_gPVBackground(this.mPVBackground);
        this.mVertexBuffer = new ScriptField_Vertex(this.mRS, this.mPointData.length / 4);
        Mesh.AllocationBuilder meshBuilder = new Mesh.AllocationBuilder(this.mRS);
        meshBuilder.addVertexAllocation(this.mVertexBuffer.getAllocation());
        meshBuilder.addIndexSetType(Mesh.Primitive.TRIANGLE_STRIP);
        this.mCubeMesh = meshBuilder.create();
        this.mPointAlloc = this.mVertexBuffer.getAllocation();
        this.mScript.bind_gPoints(this.mVertexBuffer);
        this.mScript.set_gPointBuffer(this.mPointAlloc);
        this.mScript.set_gCubeMesh(this.mCubeMesh);
        this.mPointAlloc.copyFromUnchecked(this.mPointData);
        this.mTexture = Allocation.createFromBitmapResource(this.mRS, this.mResources, this.mTexId, Allocation.MipmapControl.MIPMAP_NONE, 2);
        this.mScript.set_gTlinetexture(this.mTexture);
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
        return this.mScript;
    }

    @Override
    public void setOffset(float xOffset, float yOffset, int xPixels, int yPixels) {
        this.mWorldState.yRotation = 4.0f * xOffset * 180.0f;
        updateWorldState();
    }

    @Override
    public void start() {
        super.start();
        this.mVisible = true;
        if (this.mAudioCapture != null) {
            this.mAudioCapture.start();
        }
        SystemClock.sleep(200L);
        updateWave();
    }

    @Override
    public void stop() {
        super.stop();
        this.mVisible = false;
        if (this.mAudioCapture != null) {
            this.mAudioCapture.stop();
        }
        updateWave();
    }

    public void update() {
    }

    void updateWave() {
        this.mHandler.removeCallbacks(this.mDrawCube);
        if (this.mVisible) {
            this.mHandler.postDelayed(this.mDrawCube, 20L);
            update();
            this.mWorldState.waveCounter++;
            updateWorldState();
        }
    }

    protected void updateWorldState() {
        this.mScript.set_gYRotation(this.mWorldState.yRotation);
        this.mScript.set_gIdle(this.mWorldState.idle);
        this.mScript.set_gWaveCounter(this.mWorldState.waveCounter);
        this.mScript.set_gWidth(this.mWorldState.width);
    }
}
