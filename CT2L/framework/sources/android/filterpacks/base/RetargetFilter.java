package android.filterpacks.base;

import android.filterfw.core.Filter;
import android.filterfw.core.FilterContext;
import android.filterfw.core.Frame;
import android.filterfw.core.FrameFormat;
import android.filterfw.core.GenerateFinalPort;
import android.filterfw.core.MutableFrameFormat;
import android.hardware.Camera;

public class RetargetFilter extends Filter {
    private MutableFrameFormat mOutputFormat;
    private int mTarget;

    @GenerateFinalPort(hasDefault = false, name = "target")
    private String mTargetString;

    public RetargetFilter(String name) {
        super(name);
        this.mTarget = -1;
    }

    @Override
    public void setupPorts() {
        this.mTarget = FrameFormat.readTargetString(this.mTargetString);
        addInputPort(Camera.Parameters.EFFECT_FRAME);
        addOutputBasedOnInput(Camera.Parameters.EFFECT_FRAME, Camera.Parameters.EFFECT_FRAME);
    }

    @Override
    public FrameFormat getOutputFormat(String portName, FrameFormat inputFormat) {
        MutableFrameFormat retargeted = inputFormat.mutableCopy();
        retargeted.setTarget(this.mTarget);
        return retargeted;
    }

    @Override
    public void process(FilterContext context) {
        Frame input = pullInput(Camera.Parameters.EFFECT_FRAME);
        Frame output = context.getFrameManager().duplicateFrameToTarget(input, this.mTarget);
        pushOutput(Camera.Parameters.EFFECT_FRAME, output);
        output.release();
    }
}
