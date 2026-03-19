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

public class FlipFilter extends Filter {

    @GenerateFieldPort(hasDefault = true, name = Slice.HINT_HORIZONTAL)
    private boolean mHorizontal;
    private Program mProgram;
    private int mTarget;

    @GenerateFieldPort(hasDefault = true, name = "tile_size")
    private int mTileSize;

    @GenerateFieldPort(hasDefault = true, name = "vertical")
    private boolean mVertical;

    public FlipFilter(String str) {
        super(str);
        this.mVertical = false;
        this.mHorizontal = false;
        this.mTileSize = 640;
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
            ShaderProgram shaderProgramCreateIdentity = ShaderProgram.createIdentity(filterContext);
            shaderProgramCreateIdentity.setMaximumTileSize(this.mTileSize);
            this.mProgram = shaderProgramCreateIdentity;
            this.mTarget = i;
            updateParameters();
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
        Frame frameNewFrame = filterContext.getFrameManager().newFrame(format);
        this.mProgram.process(framePullInput, frameNewFrame);
        pushOutput(SliceItem.FORMAT_IMAGE, frameNewFrame);
        frameNewFrame.release();
    }

    private void updateParameters() {
        ((ShaderProgram) this.mProgram).setSourceRect(this.mHorizontal ? 1.0f : 0.0f, this.mVertical ? 1.0f : 0.0f, this.mHorizontal ? -1.0f : 1.0f, this.mVertical ? -1.0f : 1.0f);
    }
}
