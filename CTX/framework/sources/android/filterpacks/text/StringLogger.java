package android.filterpacks.text;

import android.filterfw.core.Filter;
import android.filterfw.core.FilterContext;
import android.filterfw.format.ObjectFormat;
import android.util.Log;

public class StringLogger extends Filter {
    public StringLogger(String str) {
        super(str);
    }

    @Override
    public void setupPorts() {
        addMaskedInputPort("string", ObjectFormat.fromClass(Object.class, 1));
    }

    @Override
    public void process(FilterContext filterContext) {
        Log.i("StringLogger", pullInput("string").getObjectValue().toString());
    }
}
