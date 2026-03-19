package android.filterfw.core;

public class OutputPort extends FilterPort {
    protected InputPort mBasePort;
    protected InputPort mTargetPort;

    public OutputPort(Filter filter, String str) {
        super(filter, str);
    }

    public void connectTo(InputPort inputPort) {
        if (this.mTargetPort != null) {
            throw new RuntimeException(this + " already connected to " + this.mTargetPort + "!");
        }
        this.mTargetPort = inputPort;
        this.mTargetPort.setSourcePort(this);
    }

    public boolean isConnected() {
        return this.mTargetPort != null;
    }

    @Override
    public void open() {
        super.open();
        if (this.mTargetPort != null && !this.mTargetPort.isOpen()) {
            this.mTargetPort.open();
        }
    }

    @Override
    public void close() {
        super.close();
        if (this.mTargetPort != null && this.mTargetPort.isOpen()) {
            this.mTargetPort.close();
        }
    }

    public InputPort getTargetPort() {
        return this.mTargetPort;
    }

    public Filter getTargetFilter() {
        if (this.mTargetPort == null) {
            return null;
        }
        return this.mTargetPort.getFilter();
    }

    public void setBasePort(InputPort inputPort) {
        this.mBasePort = inputPort;
    }

    public InputPort getBasePort() {
        return this.mBasePort;
    }

    @Override
    public boolean filterMustClose() {
        return !isOpen() && isBlocking();
    }

    @Override
    public boolean isReady() {
        return (isOpen() && this.mTargetPort.acceptsFrame()) || !isBlocking();
    }

    @Override
    public void clear() {
        if (this.mTargetPort != null) {
            this.mTargetPort.clear();
        }
    }

    @Override
    public void pushFrame(Frame frame) {
        if (this.mTargetPort == null) {
            throw new RuntimeException("Attempting to push frame on unconnected port: " + this + "!");
        }
        this.mTargetPort.pushFrame(frame);
    }

    @Override
    public void setFrame(Frame frame) {
        assertPortIsOpen();
        if (this.mTargetPort == null) {
            throw new RuntimeException("Attempting to set frame on unconnected port: " + this + "!");
        }
        this.mTargetPort.setFrame(frame);
    }

    @Override
    public Frame pullFrame() {
        throw new RuntimeException("Cannot pull frame on " + this + "!");
    }

    @Override
    public boolean hasFrame() {
        if (this.mTargetPort == null) {
            return false;
        }
        return this.mTargetPort.hasFrame();
    }

    @Override
    public String toString() {
        return "output " + super.toString();
    }
}
