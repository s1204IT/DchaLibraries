package android.filterpacks.text;

import android.filterfw.core.Filter;
import android.filterfw.core.FilterContext;
import android.filterfw.core.Frame;
import android.filterfw.core.FrameFormat;
import android.filterfw.core.GenerateFieldPort;
import android.filterfw.format.ObjectFormat;

public class StringSource extends Filter {
    private FrameFormat mOutputFormat;

    @GenerateFieldPort(name = "stringValue")
    private String mString;

    public StringSource(String str) {
        super(str);
    }

    @Override
    public void setupPorts() {
        this.mOutputFormat = ObjectFormat.fromClass(String.class, 1);
        addOutputPort("string", this.mOutputFormat);
    }

    @Override
    public void process(FilterContext filterContext) {
        Frame frameNewFrame = filterContext.getFrameManager().newFrame(this.mOutputFormat);
        frameNewFrame.setObjectValue(this.mString);
        frameNewFrame.setTimestamp(-1L);
        pushOutput("string", frameNewFrame);
        closeOutputPort("string");
    }
}
