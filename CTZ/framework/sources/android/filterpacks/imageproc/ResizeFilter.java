package android.filterpacks.imageproc;

import android.app.slice.SliceItem;
import android.filterfw.core.Filter;
import android.filterfw.core.FilterContext;
import android.filterfw.core.Frame;
import android.filterfw.core.FrameFormat;
import android.filterfw.core.GLFrame;
import android.filterfw.core.GenerateFieldPort;
import android.filterfw.core.MutableFrameFormat;
import android.filterfw.core.Program;
import android.filterfw.core.ShaderProgram;
import android.filterfw.format.ImageFormat;

public class ResizeFilter extends Filter {

    @GenerateFieldPort(hasDefault = true, name = "generateMipMap")
    private boolean mGenerateMipMap;
    private int mInputChannels;

    @GenerateFieldPort(hasDefault = true, name = "keepAspectRatio")
    private boolean mKeepAspectRatio;
    private FrameFormat mLastFormat;

    @GenerateFieldPort(name = "oheight")
    private int mOHeight;

    @GenerateFieldPort(name = "owidth")
    private int mOWidth;
    private MutableFrameFormat mOutputFormat;
    private Program mProgram;

    public ResizeFilter(String str) {
        super(str);
        this.mKeepAspectRatio = false;
        this.mGenerateMipMap = false;
        this.mLastFormat = null;
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

    protected void createProgram(FilterContext filterContext, FrameFormat frameFormat) {
        if (this.mLastFormat == null || this.mLastFormat.getTarget() != frameFormat.getTarget()) {
            this.mLastFormat = frameFormat;
            switch (frameFormat.getTarget()) {
                case 2:
                    throw new RuntimeException("Native ResizeFilter not implemented yet!");
                case 3:
                    this.mProgram = ShaderProgram.createIdentity(filterContext);
                    return;
                default:
                    throw new RuntimeException("ResizeFilter could not create suitable program!");
            }
        }
    }

    @Override
    public void process(FilterContext filterContext) {
        Frame framePullInput = pullInput(SliceItem.FORMAT_IMAGE);
        createProgram(filterContext, framePullInput.getFormat());
        MutableFrameFormat mutableFrameFormatMutableCopy = framePullInput.getFormat().mutableCopy();
        if (this.mKeepAspectRatio) {
            FrameFormat format = framePullInput.getFormat();
            this.mOHeight = (this.mOWidth * format.getHeight()) / format.getWidth();
        }
        mutableFrameFormatMutableCopy.setDimensions(this.mOWidth, this.mOHeight);
        Frame frameNewFrame = filterContext.getFrameManager().newFrame(mutableFrameFormatMutableCopy);
        if (this.mGenerateMipMap) {
            GLFrame gLFrame = (GLFrame) filterContext.getFrameManager().newFrame(framePullInput.getFormat());
            gLFrame.setTextureParameter(10241, 9985);
            gLFrame.setDataFromFrame(framePullInput);
            gLFrame.generateMipMap();
            this.mProgram.process(gLFrame, frameNewFrame);
            gLFrame.release();
        } else {
            this.mProgram.process(framePullInput, frameNewFrame);
        }
        pushOutput(SliceItem.FORMAT_IMAGE, frameNewFrame);
        frameNewFrame.release();
    }
}
