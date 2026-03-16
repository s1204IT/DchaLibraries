package android.filterpacks.base;

import android.filterfw.core.Filter;
import android.filterfw.core.FilterContext;
import android.filterfw.core.Frame;
import android.filterfw.core.FrameFormat;
import android.filterfw.core.GenerateFieldPort;
import android.filterfw.core.GenerateFinalPort;
import android.hardware.Camera;
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
        addOutputPort(Camera.Parameters.EFFECT_FRAME, this.mFormat == null ? FrameFormat.unspecified() : this.mFormat);
    }

    @Override
    public void process(FilterContext context) {
        Frame output = context.fetchFrame(this.mKey);
        if (output != null) {
            pushOutput(Camera.Parameters.EFFECT_FRAME, output);
            if (!this.mRepeatFrame) {
                closeOutputPort(Camera.Parameters.EFFECT_FRAME);
                return;
            }
            return;
        }
        delayNextProcess(250);
    }
}
