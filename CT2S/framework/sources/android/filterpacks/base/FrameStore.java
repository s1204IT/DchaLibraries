package android.filterpacks.base;

import android.filterfw.core.Filter;
import android.filterfw.core.FilterContext;
import android.filterfw.core.Frame;
import android.filterfw.core.GenerateFieldPort;
import android.hardware.Camera;

public class FrameStore extends Filter {

    @GenerateFieldPort(name = "key")
    private String mKey;

    public FrameStore(String name) {
        super(name);
    }

    @Override
    public void setupPorts() {
        addInputPort(Camera.Parameters.EFFECT_FRAME);
    }

    @Override
    public void process(FilterContext context) {
        Frame input = pullInput(Camera.Parameters.EFFECT_FRAME);
        context.storeFrame(this.mKey, input);
    }
}
