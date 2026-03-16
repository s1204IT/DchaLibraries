package android.filterpacks.base;

import android.filterfw.core.Filter;
import android.filterfw.core.FilterContext;
import android.hardware.Camera;

public class NullFilter extends Filter {
    public NullFilter(String name) {
        super(name);
    }

    @Override
    public void setupPorts() {
        addInputPort(Camera.Parameters.EFFECT_FRAME);
    }

    @Override
    public void process(FilterContext context) {
        pullInput(Camera.Parameters.EFFECT_FRAME);
    }
}
