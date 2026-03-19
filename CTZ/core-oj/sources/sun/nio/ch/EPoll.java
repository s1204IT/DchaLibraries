package sun.nio.ch;

import java.io.IOException;
import sun.misc.Unsafe;

class EPoll {
    static final int EPOLLONESHOT = 1073741824;
    static final int EPOLL_CTL_ADD = 1;
    static final int EPOLL_CTL_DEL = 2;
    static final int EPOLL_CTL_MOD = 3;
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    private static final int SIZEOF_EPOLLEVENT = eventSize();
    private static final int OFFSETOF_EVENTS = eventsOffset();
    private static final int OFFSETOF_FD = dataOffset();

    private static native int dataOffset();

    static native int epollCreate() throws IOException;

    static native int epollCtl(int i, int i2, int i3, int i4);

    static native int epollWait(int i, long j, int i2) throws IOException;

    private static native int eventSize();

    private static native int eventsOffset();

    private EPoll() {
    }

    static long allocatePollArray(int i) {
        return unsafe.allocateMemory(i * SIZEOF_EPOLLEVENT);
    }

    static void freePollArray(long j) {
        unsafe.freeMemory(j);
    }

    static long getEvent(long j, int i) {
        return j + ((long) (SIZEOF_EPOLLEVENT * i));
    }

    static int getDescriptor(long j) {
        return unsafe.getInt(j + ((long) OFFSETOF_FD));
    }

    static int getEvents(long j) {
        return unsafe.getInt(j + ((long) OFFSETOF_EVENTS));
    }
}
