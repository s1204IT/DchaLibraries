package dalvik.system.profiler;

import java.util.Arrays;

class PortableThreadSampler implements ThreadSampler {
    private int depth;

    PortableThreadSampler() {
    }

    @Override
    public void setDepth(int depth) {
        this.depth = depth;
    }

    @Override
    public StackTraceElement[] getStackTrace(Thread thread) {
        StackTraceElement[] stackFrames = thread.getStackTrace();
        if (stackFrames.length == 0) {
            return null;
        }
        if (stackFrames.length > this.depth) {
            stackFrames = (StackTraceElement[]) Arrays.copyOfRange(stackFrames, 0, this.depth);
        }
        return stackFrames;
    }
}
