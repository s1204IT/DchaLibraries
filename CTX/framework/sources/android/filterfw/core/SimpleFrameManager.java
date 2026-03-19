package android.filterfw.core;

public class SimpleFrameManager extends FrameManager {
    @Override
    public Frame newFrame(FrameFormat frameFormat) {
        return createNewFrame(frameFormat);
    }

    @Override
    public Frame newBoundFrame(FrameFormat frameFormat, int i, long j) {
        if (frameFormat.getTarget() == 3) {
            GLFrame gLFrame = new GLFrame(frameFormat, this, i, j);
            gLFrame.init(getGLEnvironment());
            return gLFrame;
        }
        throw new RuntimeException("Attached frames are not supported for target type: " + FrameFormat.targetToString(frameFormat.getTarget()) + "!");
    }

    private Frame createNewFrame(FrameFormat frameFormat) {
        switch (frameFormat.getTarget()) {
            case 1:
                return new SimpleFrame(frameFormat, this);
            case 2:
                return new NativeFrame(frameFormat, this);
            case 3:
                GLFrame gLFrame = new GLFrame(frameFormat, this);
                gLFrame.init(getGLEnvironment());
                return gLFrame;
            case 4:
                return new VertexFrame(frameFormat, this);
            default:
                throw new RuntimeException("Unsupported frame target type: " + FrameFormat.targetToString(frameFormat.getTarget()) + "!");
        }
    }

    @Override
    public Frame retainFrame(Frame frame) {
        frame.incRefCount();
        return frame;
    }

    @Override
    public Frame releaseFrame(Frame frame) {
        int iDecRefCount = frame.decRefCount();
        if (iDecRefCount == 0 && frame.hasNativeAllocation()) {
            frame.releaseNativeAllocation();
            return null;
        }
        if (iDecRefCount < 0) {
            throw new RuntimeException("Frame reference count dropped below 0!");
        }
        return frame;
    }
}
