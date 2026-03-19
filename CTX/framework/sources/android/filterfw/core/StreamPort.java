package android.filterfw.core;

public class StreamPort extends InputPort {
    private Frame mFrame;
    private boolean mPersistent;

    public StreamPort(Filter filter, String str) {
        super(filter, str);
    }

    @Override
    public void clear() {
        if (this.mFrame != null) {
            this.mFrame.release();
            this.mFrame = null;
        }
    }

    @Override
    public void setFrame(Frame frame) {
        assignFrame(frame, true);
    }

    @Override
    public void pushFrame(Frame frame) {
        assignFrame(frame, false);
    }

    protected synchronized void assignFrame(Frame frame, boolean z) {
        assertPortIsOpen();
        checkFrameType(frame, z);
        if (z) {
            if (this.mFrame != null) {
                this.mFrame.release();
            }
        } else if (this.mFrame != null) {
            throw new RuntimeException("Attempting to push more than one frame on port: " + this + "!");
        }
        this.mFrame = frame.retain();
        this.mFrame.markReadOnly();
        this.mPersistent = z;
    }

    @Override
    public synchronized Frame pullFrame() {
        Frame frame;
        if (this.mFrame == null) {
            throw new RuntimeException("No frame available to pull on port: " + this + "!");
        }
        frame = this.mFrame;
        if (this.mPersistent) {
            this.mFrame.retain();
        } else {
            this.mFrame = null;
        }
        return frame;
    }

    @Override
    public synchronized boolean hasFrame() {
        return this.mFrame != null;
    }

    @Override
    public String toString() {
        return "input " + super.toString();
    }

    @Override
    public synchronized void transfer(FilterContext filterContext) {
        if (this.mFrame != null) {
            checkFrameManager(this.mFrame, filterContext);
        }
    }
}
