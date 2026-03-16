package android.filterpacks.text;

import android.filterfw.core.Filter;
import android.filterfw.core.FilterContext;
import android.filterfw.core.Frame;
import android.filterfw.format.ObjectFormat;
import android.util.Log;

public class StringLogger extends Filter {
    public StringLogger(String name) {
        super(name);
    }

    @Override
    public void setupPorts() {
        addMaskedInputPort("string", ObjectFormat.fromClass(Object.class, 1));
    }

    @Override
    public void process(FilterContext env) {
        Frame input = pullInput("string");
        String inputString = input.getObjectValue().toString();
        Log.i("StringLogger", inputString);
    }
}
