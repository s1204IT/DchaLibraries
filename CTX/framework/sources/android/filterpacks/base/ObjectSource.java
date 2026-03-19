package android.filterpacks.base;

import android.filterfw.core.Filter;
import android.filterfw.core.FilterContext;
import android.filterfw.core.Frame;
import android.filterfw.core.FrameFormat;
import android.filterfw.core.GenerateFieldPort;
import android.filterfw.core.GenerateFinalPort;
import android.filterfw.format.ObjectFormat;

public class ObjectSource extends Filter {
    private Frame mFrame;

    @GenerateFieldPort(name = "object")
    private Object mObject;

    @GenerateFinalPort(hasDefault = true, name = "format")
    private FrameFormat mOutputFormat;

    @GenerateFieldPort(hasDefault = true, name = "repeatFrame")
    boolean mRepeatFrame;

    public ObjectSource(String str) {
        super(str);
        this.mOutputFormat = FrameFormat.unspecified();
        this.mRepeatFrame = false;
    }

    @Override
    public void setupPorts() {
        addOutputPort("frame", this.mOutputFormat);
    }

    @Override
    public void process(FilterContext filterContext) {
        if (this.mFrame == null) {
            if (this.mObject == null) {
                throw new NullPointerException("ObjectSource producing frame with no object set!");
            }
            this.mFrame = filterContext.getFrameManager().newFrame(ObjectFormat.fromObject(this.mObject, 1));
            this.mFrame.setObjectValue(this.mObject);
            this.mFrame.setTimestamp(-1L);
        }
        pushOutput("frame", this.mFrame);
        if (!this.mRepeatFrame) {
            closeOutputPort("frame");
        }
    }

    @Override
    public void tearDown(FilterContext filterContext) {
        this.mFrame.release();
    }

    @Override
    public void fieldPortValueUpdated(String str, FilterContext filterContext) {
        if (str.equals("object") && this.mFrame != null) {
            this.mFrame.release();
            this.mFrame = null;
        }
    }
}
