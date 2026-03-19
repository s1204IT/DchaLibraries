package dalvik.system;

import java.io.File;

public final class ZygoteHooks {
    private long token;

    private static native void nativePostForkChild(long j, int i, boolean z, boolean z2, String str);

    private static native long nativePreFork();

    public static native void startZygoteNoThreadCreation();

    public static native void stopZygoteNoThreadCreation();

    public void preFork() {
        Daemons.stop();
        waitUntilAllThreadsStopped();
        this.token = nativePreFork();
    }

    public void postForkChild(int i, boolean z, boolean z2, String str) {
        nativePostForkChild(this.token, i, z, z2, str);
        Math.setRandomSeedInternal(System.currentTimeMillis());
    }

    public void postForkCommon() {
        Daemons.startPostZygoteFork();
    }

    private static void waitUntilAllThreadsStopped() {
        File file = new File("/proc/self/task");
        while (file.list().length > 1) {
            Thread.yield();
        }
    }
}
