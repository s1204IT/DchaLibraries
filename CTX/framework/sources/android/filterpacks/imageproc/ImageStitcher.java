package android.filterpacks.imageproc;

import android.app.slice.SliceItem;
import android.filterfw.core.Filter;
import android.filterfw.core.FilterContext;
import android.filterfw.core.Frame;
import android.filterfw.core.FrameFormat;
import android.filterfw.core.GenerateFieldPort;
import android.filterfw.core.MutableFrameFormat;
import android.filterfw.core.Program;
import android.filterfw.core.ShaderProgram;
import android.filterfw.format.ImageFormat;

public class ImageStitcher extends Filter {
    private int mImageHeight;
    private int mImageWidth;
    private int mInputHeight;
    private int mInputWidth;
    private Frame mOutputFrame;

    @GenerateFieldPort(name = "padSize")
    private int mPadSize;
    private Program mProgram;
    private int mSliceHeight;
    private int mSliceIndex;
    private int mSliceWidth;

    @GenerateFieldPort(name = "xSlices")
    private int mXSlices;

    @GenerateFieldPort(name = "ySlices")
    private int mYSlices;

    public ImageStitcher(String str) {
        super(str);
        this.mSliceIndex = 0;
    }

    @Override
    public void setupPorts() {
        addMaskedInputPort(SliceItem.FORMAT_IMAGE, ImageFormat.create(3, 3));
        addOutputBasedOnInput(SliceItem.FORMAT_IMAGE, SliceItem.FORMAT_IMAGE);
    }

    @Override
    public FrameFormat getOutputFormat(String str, FrameFormat frameFormat) {
        return frameFormat;
    }

    private FrameFormat calcOutputFormatForInput(FrameFormat frameFormat) {
        MutableFrameFormat mutableFrameFormatMutableCopy = frameFormat.mutableCopy();
        this.mInputWidth = frameFormat.getWidth();
        this.mInputHeight = frameFormat.getHeight();
        this.mSliceWidth = this.mInputWidth - (this.mPadSize * 2);
        this.mSliceHeight = this.mInputHeight - (2 * this.mPadSize);
        this.mImageWidth = this.mSliceWidth * this.mXSlices;
        this.mImageHeight = this.mSliceHeight * this.mYSlices;
        mutableFrameFormatMutableCopy.setDimensions(this.mImageWidth, this.mImageHeight);
        return mutableFrameFormatMutableCopy;
    }

    @Override
    public void process(FilterContext filterContext) {
        Frame framePullInput = pullInput(SliceItem.FORMAT_IMAGE);
        FrameFormat format = framePullInput.getFormat();
        if (this.mSliceIndex == 0) {
            this.mOutputFrame = filterContext.getFrameManager().newFrame(calcOutputFormatForInput(format));
        } else if (format.getWidth() != this.mInputWidth || format.getHeight() != this.mInputHeight) {
            throw new RuntimeException("Image size should not change.");
        }
        if (this.mProgram == null) {
            this.mProgram = ShaderProgram.createIdentity(filterContext);
        }
        int i = (this.mSliceIndex % this.mXSlices) * this.mSliceWidth;
        int i2 = (this.mSliceIndex / this.mXSlices) * this.mSliceHeight;
        float fMin = Math.min(this.mSliceWidth, this.mImageWidth - i);
        float fMin2 = Math.min(this.mSliceHeight, this.mImageHeight - i2);
        ((ShaderProgram) this.mProgram).setSourceRect(this.mPadSize / this.mInputWidth, this.mPadSize / this.mInputHeight, fMin / this.mInputWidth, fMin2 / this.mInputHeight);
        ((ShaderProgram) this.mProgram).setTargetRect(i / this.mImageWidth, i2 / this.mImageHeight, fMin / this.mImageWidth, fMin2 / this.mImageHeight);
        this.mProgram.process(framePullInput, this.mOutputFrame);
        this.mSliceIndex++;
        if (this.mSliceIndex == this.mXSlices * this.mYSlices) {
            pushOutput(SliceItem.FORMAT_IMAGE, this.mOutputFrame);
            this.mOutputFrame.release();
            this.mSliceIndex = 0;
        }
    }
}
