package android.filterpacks.imageproc;

import android.app.slice.SliceItem;
import android.filterfw.core.Filter;
import android.filterfw.core.FilterContext;
import android.filterfw.core.Frame;
import android.filterfw.core.FrameFormat;
import android.filterfw.core.MutableFrameFormat;
import android.filterfw.core.NativeProgram;
import android.filterfw.core.Program;
import android.filterfw.format.ImageFormat;

public class ToRGBFilter extends Filter {
    private int mInputBPP;
    private FrameFormat mLastFormat;
    private Program mProgram;

    public ToRGBFilter(String str) {
        super(str);
        this.mLastFormat = null;
    }

    @Override
    public void setupPorts() {
        MutableFrameFormat mutableFrameFormat = new MutableFrameFormat(2, 2);
        mutableFrameFormat.setDimensionCount(2);
        addMaskedInputPort(SliceItem.FORMAT_IMAGE, mutableFrameFormat);
        addOutputBasedOnInput(SliceItem.FORMAT_IMAGE, SliceItem.FORMAT_IMAGE);
    }

    @Override
    public FrameFormat getOutputFormat(String str, FrameFormat frameFormat) {
        return getConvertedFormat(frameFormat);
    }

    public FrameFormat getConvertedFormat(FrameFormat frameFormat) {
        MutableFrameFormat mutableFrameFormatMutableCopy = frameFormat.mutableCopy();
        mutableFrameFormatMutableCopy.setMetaValue(ImageFormat.COLORSPACE_KEY, 2);
        mutableFrameFormatMutableCopy.setBytesPerSample(3);
        return mutableFrameFormatMutableCopy;
    }

    public void createProgram(FilterContext filterContext, FrameFormat frameFormat) {
        this.mInputBPP = frameFormat.getBytesPerSample();
        if (this.mLastFormat == null || this.mLastFormat.getBytesPerSample() != this.mInputBPP) {
            this.mLastFormat = frameFormat;
            int i = this.mInputBPP;
            if (i == 1) {
                this.mProgram = new NativeProgram("filterpack_imageproc", "gray_to_rgb");
                return;
            }
            if (i == 4) {
                this.mProgram = new NativeProgram("filterpack_imageproc", "rgba_to_rgb");
                return;
            }
            throw new RuntimeException("Unsupported BytesPerPixel: " + this.mInputBPP + "!");
        }
    }

    @Override
    public void process(FilterContext filterContext) {
        Frame framePullInput = pullInput(SliceItem.FORMAT_IMAGE);
        createProgram(filterContext, framePullInput.getFormat());
        Frame frameNewFrame = filterContext.getFrameManager().newFrame(getConvertedFormat(framePullInput.getFormat()));
        this.mProgram.process(framePullInput, frameNewFrame);
        pushOutput(SliceItem.FORMAT_IMAGE, frameNewFrame);
        frameNewFrame.release();
    }
}
