package android.filterpacks.base;

import android.filterfw.core.Filter;
import android.filterfw.core.FilterContext;
import android.filterfw.core.Frame;
import android.filterfw.core.FrameFormat;
import android.filterfw.core.GenerateFieldPort;
import android.filterfw.core.GenerateFinalPort;
import android.filterfw.format.ObjectFormat;
import android.hardware.Camera;
import android.provider.MediaStore;

public class ObjectSource extends Filter {
    private Frame mFrame;

    @GenerateFieldPort(name = "object")
    private Object mObject;

    @GenerateFinalPort(hasDefault = true, name = MediaStore.Files.FileColumns.FORMAT)
    private FrameFormat mOutputFormat;

    @GenerateFieldPort(hasDefault = true, name = "repeatFrame")
    boolean mRepeatFrame;

    public ObjectSource(String name) {
        super(name);
        this.mOutputFormat = FrameFormat.unspecified();
        this.mRepeatFrame = false;
    }

    @Override
    public void setupPorts() {
        addOutputPort(Camera.Parameters.EFFECT_FRAME, this.mOutputFormat);
    }

    @Override
    public void process(FilterContext context) {
        if (this.mFrame == null) {
            if (this.mObject == null) {
                throw new NullPointerException("ObjectSource producing frame with no object set!");
            }
            FrameFormat outputFormat = ObjectFormat.fromObject(this.mObject, 1);
            this.mFrame = context.getFrameManager().newFrame(outputFormat);
            this.mFrame.setObjectValue(this.mObject);
            this.mFrame.setTimestamp(-1L);
        }
        pushOutput(Camera.Parameters.EFFECT_FRAME, this.mFrame);
        if (!this.mRepeatFrame) {
            closeOutputPort(Camera.Parameters.EFFECT_FRAME);
        }
    }

    @Override
    public void tearDown(FilterContext context) {
        this.mFrame.release();
    }

    @Override
    public void fieldPortValueUpdated(String name, FilterContext context) {
        if (name.equals("object") && this.mFrame != null) {
            this.mFrame.release();
            this.mFrame = null;
        }
    }
}
