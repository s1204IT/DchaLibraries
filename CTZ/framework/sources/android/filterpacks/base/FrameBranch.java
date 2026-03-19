package android.filterpacks.base;

import android.filterfw.core.Filter;
import android.filterfw.core.FilterContext;
import android.filterfw.core.Frame;
import android.filterfw.core.FrameFormat;
import android.filterfw.core.GenerateFinalPort;

public class FrameBranch extends Filter {

    @GenerateFinalPort(hasDefault = true, name = "outputs")
    private int mNumberOfOutputs;

    public FrameBranch(String str) {
        super(str);
        this.mNumberOfOutputs = 2;
    }

    @Override
    public void setupPorts() {
        addInputPort("in");
        for (int i = 0; i < this.mNumberOfOutputs; i++) {
            addOutputBasedOnInput("out" + i, "in");
        }
    }

    @Override
    public FrameFormat getOutputFormat(String str, FrameFormat frameFormat) {
        return frameFormat;
    }

    @Override
    public void process(FilterContext filterContext) {
        Frame framePullInput = pullInput("in");
        for (int i = 0; i < this.mNumberOfOutputs; i++) {
            pushOutput("out" + i, framePullInput);
        }
    }
}
