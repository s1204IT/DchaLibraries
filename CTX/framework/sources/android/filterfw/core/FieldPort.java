package android.filterfw.core;

import java.lang.reflect.Field;

public class FieldPort extends InputPort {
    protected Field mField;
    protected boolean mHasFrame;
    protected Object mValue;
    protected boolean mValueWaiting;

    public FieldPort(Filter filter, String str, Field field, boolean z) {
        super(filter, str);
        this.mValueWaiting = false;
        this.mField = field;
        this.mHasFrame = z;
    }

    @Override
    public void clear() {
    }

    @Override
    public void pushFrame(Frame frame) {
        setFieldFrame(frame, false);
    }

    @Override
    public void setFrame(Frame frame) {
        setFieldFrame(frame, true);
    }

    @Override
    public Object getTarget() {
        try {
            return this.mField.get(this.mFilter);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    @Override
    public synchronized void transfer(FilterContext filterContext) {
        if (this.mValueWaiting) {
            try {
                this.mField.set(this.mFilter, this.mValue);
                this.mValueWaiting = false;
                if (filterContext != null) {
                    this.mFilter.notifyFieldPortValueUpdated(this.mName, filterContext);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Access to field '" + this.mField.getName() + "' was denied!");
            }
        }
    }

    @Override
    public synchronized Frame pullFrame() {
        throw new RuntimeException("Cannot pull frame on " + this + "!");
    }

    @Override
    public synchronized boolean hasFrame() {
        return this.mHasFrame;
    }

    @Override
    public synchronized boolean acceptsFrame() {
        return !this.mValueWaiting;
    }

    @Override
    public String toString() {
        return "field " + super.toString();
    }

    protected synchronized void setFieldFrame(Frame frame, boolean z) {
        assertPortIsOpen();
        checkFrameType(frame, z);
        Object objectValue = frame.getObjectValue();
        if ((objectValue == null && this.mValue != null) || !objectValue.equals(this.mValue)) {
            this.mValue = objectValue;
            this.mValueWaiting = true;
        }
        this.mHasFrame = true;
    }
}
