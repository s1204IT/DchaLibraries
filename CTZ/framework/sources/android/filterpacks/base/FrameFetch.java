package android.filterpacks.base;

import android.filterfw.core.Filter;
import android.filterfw.core.FilterContext;
import android.filterfw.core.Frame;
import android.filterfw.core.FrameFormat;
import android.filterfw.core.GenerateFieldPort;
import android.filterfw.core.GenerateFinalPort;

public class FrameFetch extends Filter {

    @GenerateFinalPort(hasDefault = true, name = "format")
    private FrameFormat mFormat;

    @GenerateFieldPort(name = "key")
    private String mKey;

    @GenerateFieldPort(hasDefault = true, name = "repeatFrame")
    private boolean mRepeatFrame;

    public FrameFetch(String str) {
        super(str);
        this.mRepeatFrame = false;
    }

    @Override
    public void setupPorts() {
        addOutputPort("frame", this.mFormat == null ? FrameFormat.unspecified() : this.mFormat);
    }

    @Override
    public void process(FilterContext filterContext) {
        Frame frameFetchFrame = filterContext.fetchFrame(this.mKey);
        if (frameFetchFrame != null) {
            pushOutput("frame", frameFetchFrame);
            if (!this.mRepeatFrame) {
                closeOutputPort("frame");
                return;
            }
            return;
        }
        delayNextProcess(250);
    }
}
