package android.filterpacks.imageproc;

import android.app.slice.SliceItem;
import android.filterfw.core.Filter;
import android.filterfw.core.FilterContext;
import android.filterfw.core.Frame;
import android.filterfw.core.FrameFormat;
import android.filterfw.core.GenerateFieldPort;
import android.filterfw.core.Program;
import android.filterfw.core.ShaderProgram;
import android.filterfw.format.ImageFormat;
import android.os.BatteryManager;

public class FisheyeFilter extends Filter {
    private static final String TAG = "FisheyeFilter";
    private static final String mFisheyeShader = "precision mediump float;\nuniform sampler2D tex_sampler_0;\nuniform vec2 scale;\nuniform float alpha;\nuniform float radius2;\nuniform float factor;\nvarying vec2 v_texcoord;\nvoid main() {\n  const float m_pi_2 = 1.570963;\n  const float min_dist = 0.01;\n  vec2 coord = v_texcoord - vec2(0.5, 0.5);\n  float dist = length(coord * scale);\n  dist = max(dist, min_dist);\n  float radian = m_pi_2 - atan(alpha * sqrt(radius2 - dist * dist), dist);\n  float scalar = radian * factor / dist;\n  vec2 new_coord = coord * scalar + vec2(0.5, 0.5);\n  gl_FragColor = texture2D(tex_sampler_0, new_coord);\n}\n";
    private int mHeight;
    private Program mProgram;

    @GenerateFieldPort(hasDefault = true, name = BatteryManager.EXTRA_SCALE)
    private float mScale;
    private int mTarget;

    @GenerateFieldPort(hasDefault = true, name = "tile_size")
    private int mTileSize;
    private int mWidth;

    public FisheyeFilter(String str) {
        super(str);
        this.mScale = 0.0f;
        this.mTileSize = 640;
        this.mWidth = 0;
        this.mHeight = 0;
        this.mTarget = 0;
    }

    @Override
    public void setupPorts() {
        addMaskedInputPort(SliceItem.FORMAT_IMAGE, ImageFormat.create(3));
        addOutputBasedOnInput(SliceItem.FORMAT_IMAGE, SliceItem.FORMAT_IMAGE);
    }

    @Override
    public FrameFormat getOutputFormat(String str, FrameFormat frameFormat) {
        return frameFormat;
    }

    public void initProgram(FilterContext filterContext, int i) {
        if (i == 3) {
            ShaderProgram shaderProgram = new ShaderProgram(filterContext, mFisheyeShader);
            shaderProgram.setMaximumTileSize(this.mTileSize);
            this.mProgram = shaderProgram;
            this.mTarget = i;
            return;
        }
        throw new RuntimeException("Filter FisheyeFilter does not support frames of target " + i + "!");
    }

    @Override
    public void process(FilterContext filterContext) {
        Frame framePullInput = pullInput(SliceItem.FORMAT_IMAGE);
        FrameFormat format = framePullInput.getFormat();
        Frame frameNewFrame = filterContext.getFrameManager().newFrame(format);
        if (this.mProgram == null || format.getTarget() != this.mTarget) {
            initProgram(filterContext, format.getTarget());
        }
        if (format.getWidth() != this.mWidth || format.getHeight() != this.mHeight) {
            updateFrameSize(format.getWidth(), format.getHeight());
        }
        this.mProgram.process(framePullInput, frameNewFrame);
        pushOutput(SliceItem.FORMAT_IMAGE, frameNewFrame);
        frameNewFrame.release();
    }

    @Override
    public void fieldPortValueUpdated(String str, FilterContext filterContext) {
        if (this.mProgram != null) {
            updateProgramParams();
        }
    }

    private void updateFrameSize(int i, int i2) {
        this.mWidth = i;
        this.mHeight = i2;
        updateProgramParams();
    }

    private void updateProgramParams() {
        float[] fArr = new float[2];
        if (this.mWidth > this.mHeight) {
            fArr[0] = 1.0f;
            fArr[1] = this.mHeight / this.mWidth;
        } else {
            fArr[0] = this.mWidth / this.mHeight;
            fArr[1] = 1.0f;
        }
        float f = (this.mScale * 2.0f) + 0.75f;
        float fSqrt = (float) Math.sqrt(0.25f * ((fArr[0] * fArr[0]) + (fArr[1] * fArr[1])));
        float f2 = 1.15f * fSqrt;
        float fAtan = fSqrt / (1.5707964f - ((float) Math.atan((f / fSqrt) * ((float) Math.sqrt(r4 - r2)))));
        this.mProgram.setHostValue(BatteryManager.EXTRA_SCALE, fArr);
        this.mProgram.setHostValue("radius2", Float.valueOf(f2 * f2));
        this.mProgram.setHostValue("factor", Float.valueOf(fAtan));
        this.mProgram.setHostValue("alpha", Float.valueOf(f));
    }
}
