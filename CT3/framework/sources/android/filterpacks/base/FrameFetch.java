package android.filterpacks.base;

import android.filterfw.core.Filter;
import android.filterfw.core.FilterContext;
import android.filterfw.core.Frame;
import android.filterfw.core.FrameFormat;
import android.filterfw.core.GenerateFieldPort;
import android.filterfw.core.GenerateFinalPort;
import android.provider.MediaStore;

public class FrameFetch extends Filter {

    @GenerateFinalPort(hasDefault = true, name = MediaStore.Files.FileColumns.FORMAT)
    private FrameFormat mFormat;

    @GenerateFieldPort(name = "key")
    private String mKey;

    @GenerateFieldPort(hasDefault = true, name = "repeatFrame")
    private boolean mRepeatFrame;

    public FrameFetch(String name) {
        super(name);
        this.mRepeatFrame = false;
    }

    @Override
    public void setupPorts() {
        addOutputPort("frame", this.mFormat == null ? FrameFormat.unspecified() : this.mFormat);
    }

    @Override
    public void process(FilterContext context) throws Throwable {
        Frame output = context.fetchFrame(this.mKey);
        if (output != null) {
            pushOutput("frame", output);
            if (this.mRepeatFrame) {
                return;
            }
            closeOutputPort("frame");
            return;
        }
        delayNextProcess(250);
    }
}
