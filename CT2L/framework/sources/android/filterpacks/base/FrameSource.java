package android.filterpacks.base;

import android.filterfw.core.Filter;
import android.filterfw.core.FilterContext;
import android.filterfw.core.Frame;
import android.filterfw.core.FrameFormat;
import android.filterfw.core.GenerateFieldPort;
import android.filterfw.core.GenerateFinalPort;
import android.hardware.Camera;
import android.provider.MediaStore;

public class FrameSource extends Filter {

    @GenerateFinalPort(name = MediaStore.Files.FileColumns.FORMAT)
    private FrameFormat mFormat;

    @GenerateFieldPort(hasDefault = true, name = Camera.Parameters.EFFECT_FRAME)
    private Frame mFrame;

    @GenerateFieldPort(hasDefault = true, name = "repeatFrame")
    private boolean mRepeatFrame;

    public FrameSource(String name) {
        super(name);
        this.mFrame = null;
        this.mRepeatFrame = false;
    }

    @Override
    public void setupPorts() {
        addOutputPort(Camera.Parameters.EFFECT_FRAME, this.mFormat);
    }

    @Override
    public void process(FilterContext context) {
        if (this.mFrame != null) {
            pushOutput(Camera.Parameters.EFFECT_FRAME, this.mFrame);
        }
        if (!this.mRepeatFrame) {
            closeOutputPort(Camera.Parameters.EFFECT_FRAME);
        }
    }
}
