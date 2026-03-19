package android.filterpacks.base;

import android.app.Instrumentation;
import android.filterfw.core.Filter;
import android.filterfw.core.FilterContext;
import android.filterfw.core.Frame;
import android.filterfw.core.GenerateFieldPort;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class OutputStreamTarget extends Filter {

    @GenerateFieldPort(name = Instrumentation.REPORT_KEY_STREAMRESULT)
    private OutputStream mOutputStream;

    public OutputStreamTarget(String str) {
        super(str);
    }

    @Override
    public void setupPorts() {
        addInputPort("data");
    }

    @Override
    public void process(FilterContext filterContext) {
        ByteBuffer data;
        Frame framePullInput = pullInput("data");
        if (framePullInput.getFormat().getObjectClass() == String.class) {
            data = ByteBuffer.wrap(((String) framePullInput.getObjectValue()).getBytes());
        } else {
            data = framePullInput.getData();
        }
        try {
            this.mOutputStream.write(data.array(), 0, data.limit());
            this.mOutputStream.flush();
        } catch (IOException e) {
            throw new RuntimeException("OutputStreamTarget: Could not write to stream: " + e.getMessage() + "!");
        }
    }
}
