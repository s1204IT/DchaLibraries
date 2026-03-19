package android.filterfw.core;

import java.lang.reflect.Field;

public class FinalPort extends FieldPort {
    public FinalPort(Filter filter, String str, Field field, boolean z) {
        super(filter, str, field, z);
    }

    @Override
    protected synchronized void setFieldFrame(Frame frame, boolean z) {
        assertPortIsOpen();
        checkFrameType(frame, z);
        if (this.mFilter.getStatus() != 0) {
            throw new RuntimeException("Attempting to modify " + this + "!");
        }
        super.setFieldFrame(frame, z);
        super.transfer(null);
    }

    @Override
    public String toString() {
        return "final " + super.toString();
    }
}
