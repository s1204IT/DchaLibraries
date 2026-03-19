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
import android.filterfw.geometry.Point;
import android.filterfw.geometry.Quad;

public class RotateFilter extends Filter {

    @GenerateFieldPort(name = "angle")
    private int mAngle;
    private int mHeight;
    private int mOutputHeight;
    private int mOutputWidth;
    private Program mProgram;
    private int mTarget;

    @GenerateFieldPort(hasDefault = true, name = "tile_size")
    private int mTileSize;
    private int mWidth;

    public RotateFilter(String str) {
        super(str);
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

    public void initProgram(FilterContext filterContext, int i) {
        if (i == 3) {
            ShaderProgram shaderProgramCreateIdentity = ShaderProgram.createIdentity(filterContext);
            shaderProgramCreateIdentity.setMaximumTileSize(this.mTileSize);
            shaderProgramCreateIdentity.setClearsOutput(true);
            this.mProgram = shaderProgramCreateIdentity;
            this.mTarget = i;
            return;
        }
        throw new RuntimeException("Filter Sharpen does not support frames of target " + i + "!");
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
            this.mOutputWidth = this.mWidth;
            this.mOutputHeight = this.mHeight;
            updateParameters();
        }
        Frame frameNewFrame = filterContext.getFrameManager().newFrame(ImageFormat.create(this.mOutputWidth, this.mOutputHeight, 3, 3));
        this.mProgram.process(framePullInput, frameNewFrame);
        pushOutput(SliceItem.FORMAT_IMAGE, frameNewFrame);
        frameNewFrame.release();
    }

    private void updateParameters() {
        if (this.mAngle % 90 == 0) {
            float f = 0.0f;
            if (this.mAngle % 180 == 0) {
                if (this.mAngle % 360 == 0) {
                    f = 1.0f;
                }
            } else {
                f = (this.mAngle + 90) % 360 != 0 ? 1.0f : -1.0f;
                this.mOutputWidth = this.mHeight;
                this.mOutputHeight = this.mWidth;
                f = f;
                f = 0.0f;
            }
            float f2 = -f;
            float f3 = -f;
            float f4 = (f + f + 1.0f) * 0.5f;
            ((ShaderProgram) this.mProgram).setTargetRegion(new Quad(new Point((f2 + f + 1.0f) * 0.5f, ((f3 - f) + 1.0f) * 0.5f), new Point(f4, ((f - f) + 1.0f) * 0.5f), new Point(((f2 - f) + 1.0f) * 0.5f, (f3 + f + 1.0f) * 0.5f), new Point(0.5f * ((f - f) + 1.0f), f4)));
            return;
        }
        throw new RuntimeException("degree has to be multiply of 90.");
    }
}
