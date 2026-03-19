package android.filterpacks.base;

import android.filterfw.core.Filter;
import android.filterfw.core.FilterContext;

public class NullFilter extends Filter {
    public NullFilter(String str) {
        super(str);
    }

    @Override
    public void setupPorts() {
        addInputPort("frame");
    }

    @Override
    public void process(FilterContext filterContext) {
        pullInput("frame");
    }
}
