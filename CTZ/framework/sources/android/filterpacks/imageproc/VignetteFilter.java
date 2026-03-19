package android.filterpacks.imageproc;

import android.app.slice.Slice;
import android.app.slice.SliceItem;
import android.filterfw.core.Filter;
import android.filterfw.core.FilterContext;
import android.filterfw.core.Frame;
import android.filterfw.core.FrameFormat;
import android.filterfw.core.GenerateFieldPort;
import android.filterfw.core.Program;
import android.filterfw.core.ShaderProgram;
import android.filterfw.format.ImageFormat;
import android.hardware.Camera;
import android.os.BatteryManager;

public class VignetteFilter extends Filter {
    private int mHeight;
    private Program mProgram;

    @GenerateFieldPort(hasDefault = true, name = BatteryManager.EXTRA_SCALE)
    private float mScale;
    private final float mShade;
    private final float mSlope;
    private int mTarget;

    @GenerateFieldPort(hasDefault = true, name = "tile_size")
    private int mTileSize;
    private final String mVignetteShader;
    private int mWidth;

    public VignetteFilter(String str) {
        super(str);
        this.mScale = 0.0f;
        this.mTileSize = 640;
        this.mWidth = 0;
        this.mHeight = 0;
        this.mTarget = 0;
        this.mSlope = 20.0f;
        this.mShade = 0.85f;
        this.mVignetteShader = "precision mediump float;\nuniform sampler2D tex_sampler_0;\nuniform float range;\nuniform float inv_max_dist;\nuniform float shade;\nuniform vec2 scale;\nvarying vec2 v_texcoord;\nvoid main() {\n  const float slope = 20.0;\n  vec2 coord = v_texcoord - vec2(0.5, 0.5);\n  float dist = length(coord * scale);\n  float lumen = shade / (1.0 + exp((dist * inv_max_dist - range) * slope)) + (1.0 - shade);\n  vec4 color = texture2D(tex_sampler_0, v_texcoord);\n  gl_FragColor = vec4(color.rgb * lumen, color.a);\n}\n";
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
            ShaderProgram shaderProgram = new ShaderProgram(filterContext, "precision mediump float;\nuniform sampler2D tex_sampler_0;\nuniform float range;\nuniform float inv_max_dist;\nuniform float shade;\nuniform vec2 scale;\nvarying vec2 v_texcoord;\nvoid main() {\n  const float slope = 20.0;\n  vec2 coord = v_texcoord - vec2(0.5, 0.5);\n  float dist = length(coord * scale);\n  float lumen = shade / (1.0 + exp((dist * inv_max_dist - range) * slope)) + (1.0 - shade);\n  vec4 color = texture2D(tex_sampler_0, v_texcoord);\n  gl_FragColor = vec4(color.rgb * lumen, color.a);\n}\n");
            shaderProgram.setMaximumTileSize(this.mTileSize);
            this.mProgram = shaderProgram;
            this.mTarget = i;
            return;
        }
        throw new RuntimeException("Filter Sharpen does not support frames of target " + i + "!");
    }

    private void initParameters() {
        if (this.mProgram != null) {
            float[] fArr = new float[2];
            if (this.mWidth > this.mHeight) {
                fArr[0] = 1.0f;
                fArr[1] = this.mHeight / this.mWidth;
            } else {
                fArr[0] = this.mWidth / this.mHeight;
                fArr[1] = 1.0f;
            }
            float fSqrt = ((float) Math.sqrt((fArr[0] * fArr[0]) + (fArr[1] * fArr[1]))) * 0.5f;
            this.mProgram.setHostValue(BatteryManager.EXTRA_SCALE, fArr);
            this.mProgram.setHostValue("inv_max_dist", Float.valueOf(1.0f / fSqrt));
            this.mProgram.setHostValue(Camera.Parameters.WHITE_BALANCE_SHADE, Float.valueOf(0.85f));
            updateParameters();
        }
    }

    private void updateParameters() {
        this.mProgram.setHostValue(Slice.SUBTYPE_RANGE, Float.valueOf(1.3f - (((float) Math.sqrt(this.mScale)) * 0.7f)));
    }

    @Override
    public void fieldPortValueUpdated(String str, FilterContext filterContext) {
        if (this.mProgram != null) {
            updateParameters();
        }
    }

    @Override
    public void process(FilterContext filterContext) {
        Frame framePullInput = pullInput(SliceItem.FORMAT_IMAGE);
        FrameFormat format = framePullInput.getFormat();
        if (this.mProgram == null || format.getTarget() != this.mTarget) {
            initProgram(filterContext, format.getTarget());
        }
        if (format.getWidth() != this.mWidth || format.getHeight() != this.mHeight) {
            this.mWidth = format.getWidth();
            this.mHeight = format.getHeight();
            initParameters();
        }
        Frame frameNewFrame = filterContext.getFrameManager().newFrame(format);
        this.mProgram.process(framePullInput, frameNewFrame);
        pushOutput(SliceItem.FORMAT_IMAGE, frameNewFrame);
        frameNewFrame.release();
    }
}
