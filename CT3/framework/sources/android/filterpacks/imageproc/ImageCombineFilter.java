package android.filterpacks.imageproc;

import android.filterfw.core.Filter;
import android.filterfw.core.FilterContext;
import android.filterfw.core.Frame;
import android.filterfw.core.FrameFormat;
import android.filterfw.core.Program;
import android.filterfw.format.ImageFormat;
import java.lang.reflect.Field;

public abstract class ImageCombineFilter extends Filter {
    protected int mCurrentTarget;
    protected String[] mInputNames;
    protected String mOutputName;
    protected String mParameterName;
    protected Program mProgram;

    protected abstract Program getNativeProgram(FilterContext filterContext);

    protected abstract Program getShaderProgram(FilterContext filterContext);

    public ImageCombineFilter(String name, String[] inputNames, String outputName, String parameterName) {
        super(name);
        this.mCurrentTarget = 0;
        this.mInputNames = inputNames;
        this.mOutputName = outputName;
        this.mParameterName = parameterName;
    }

    @Override
    public void setupPorts() {
        if (this.mParameterName != null) {
            try {
                Field programField = ImageCombineFilter.class.getDeclaredField("mProgram");
                addProgramPort(this.mParameterName, this.mParameterName, programField, Float.TYPE, false);
            } catch (NoSuchFieldException e) {
                throw new RuntimeException("Internal Error: mProgram field not found!");
            }
        }
        for (String inputName : this.mInputNames) {
            addMaskedInputPort(inputName, ImageFormat.create(3));
        }
        addOutputBasedOnInput(this.mOutputName, this.mInputNames[0]);
    }

    @Override
    public FrameFormat getOutputFormat(String portName, FrameFormat inputFormat) {
        return inputFormat;
    }

    private void assertAllInputTargetsMatch() {
        int target = getInputFormat(this.mInputNames[0]).getTarget();
        for (String inputName : this.mInputNames) {
            if (target != getInputFormat(inputName).getTarget()) {
                throw new RuntimeException("Type mismatch of input formats in filter " + this + ". All input frames must have the same target!");
            }
        }
    }

    @Override
    public void process(FilterContext context) throws Throwable {
        Frame[] inputs = new Frame[this.mInputNames.length];
        String[] strArr = this.mInputNames;
        int length = strArr.length;
        int i = 0;
        int i2 = 0;
        while (i < length) {
            String inputName = strArr[i];
            inputs[i2] = pullInput(inputName);
            i++;
            i2++;
        }
        Frame output = context.getFrameManager().newFrame(inputs[0].getFormat());
        updateProgramWithTarget(inputs[0].getFormat().getTarget(), context);
        this.mProgram.process(inputs, output);
        pushOutput(this.mOutputName, output);
        output.release();
    }

    protected void updateProgramWithTarget(int target, FilterContext context) {
        if (target == this.mCurrentTarget) {
            return;
        }
        switch (target) {
            case 2:
                this.mProgram = getNativeProgram(context);
                break;
            case 3:
                this.mProgram = getShaderProgram(context);
                break;
            default:
                this.mProgram = null;
                break;
        }
        if (this.mProgram == null) {
            throw new RuntimeException("Could not create a program for image filter " + this + "!");
        }
        initProgramInputs(this.mProgram, context);
        this.mCurrentTarget = target;
    }
}
