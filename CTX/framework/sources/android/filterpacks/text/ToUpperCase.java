package android.filterpacks.text;

import android.filterfw.core.Filter;
import android.filterfw.core.FilterContext;
import android.filterfw.core.Frame;
import android.filterfw.core.FrameFormat;
import android.filterfw.format.ObjectFormat;
import java.util.Locale;

public class ToUpperCase extends Filter {
    private FrameFormat mOutputFormat;

    public ToUpperCase(String str) {
        super(str);
    }

    @Override
    public void setupPorts() {
        this.mOutputFormat = ObjectFormat.fromClass(String.class, 1);
        addMaskedInputPort("mixedcase", this.mOutputFormat);
        addOutputPort("uppercase", this.mOutputFormat);
    }

    @Override
    public void process(FilterContext filterContext) {
        String str = (String) pullInput("mixedcase").getObjectValue();
        Frame frameNewFrame = filterContext.getFrameManager().newFrame(this.mOutputFormat);
        frameNewFrame.setObjectValue(str.toUpperCase(Locale.getDefault()));
        pushOutput("uppercase", frameNewFrame);
    }
}
