package android.filterpacks.imageproc;

import android.filterfw.core.Filter;
import android.filterfw.core.FilterContext;
import android.filterfw.core.Frame;
import android.filterfw.core.FrameFormat;
import android.filterfw.core.Program;
import android.filterfw.format.ImageFormat;

public abstract class ImageCombineFilter extends Filter {
    protected int mCurrentTarget;
    protected String[] mInputNames;
    protected String mOutputName;
    protected String mParameterName;
    protected Program mProgram;

    protected abstract Program getNativeProgram(FilterContext filterContext);

    protected abstract Program getShaderProgram(FilterContext filterContext);

    public ImageCombineFilter(String str, String[] strArr, String str2, String str3) {
        super(str);
        this.mCurrentTarget = 0;
        this.mInputNames = strArr;
        this.mOutputName = str2;
        this.mParameterName = str3;
    }

    @Override
    public void setupPorts() {
        if (this.mParameterName != null) {
            try {
                addProgramPort(this.mParameterName, this.mParameterName, ImageCombineFilter.class.getDeclaredField("mProgram"), Float.TYPE, false);
            } catch (NoSuchFieldException e) {
                throw new RuntimeException("Internal Error: mProgram field not found!");
            }
        }
        for (String str : this.mInputNames) {
            addMaskedInputPort(str, ImageFormat.create(3));
        }
        addOutputBasedOnInput(this.mOutputName, this.mInputNames[0]);
    }

    @Override
    public FrameFormat getOutputFormat(String str, FrameFormat frameFormat) {
        return frameFormat;
    }

    private void assertAllInputTargetsMatch() {
        int target = getInputFormat(this.mInputNames[0]).getTarget();
        for (String str : this.mInputNames) {
            if (target != getInputFormat(str).getTarget()) {
                throw new RuntimeException("Type mismatch of input formats in filter " + this + ". All input frames must have the same target!");
            }
        }
    }

    @Override
    public void process(FilterContext filterContext) {
        Frame[] frameArr = new Frame[this.mInputNames.length];
        String[] strArr = this.mInputNames;
        int length = strArr.length;
        int i = 0;
        int i2 = 0;
        while (i < length) {
            frameArr[i2] = pullInput(strArr[i]);
            i++;
            i2++;
        }
        Frame frameNewFrame = filterContext.getFrameManager().newFrame(frameArr[0].getFormat());
        updateProgramWithTarget(frameArr[0].getFormat().getTarget(), filterContext);
        this.mProgram.process(frameArr, frameNewFrame);
        pushOutput(this.mOutputName, frameNewFrame);
        frameNewFrame.release();
    }

    protected void updateProgramWithTarget(int i, FilterContext filterContext) {
        if (i != this.mCurrentTarget) {
            switch (i) {
                case 2:
                    this.mProgram = getNativeProgram(filterContext);
                    break;
                case 3:
                    this.mProgram = getShaderProgram(filterContext);
                    break;
                default:
                    this.mProgram = null;
                    break;
            }
            if (this.mProgram == null) {
                throw new RuntimeException("Could not create a program for image filter " + this + "!");
            }
            initProgramInputs(this.mProgram, filterContext);
            this.mCurrentTarget = i;
        }
    }
}
