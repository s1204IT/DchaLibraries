package sun.nio.ch;

import java.nio.ByteBuffer;
import java.security.AccessController;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import sun.misc.Cleaner;
import sun.misc.Unsafe;
import sun.misc.VM;
import sun.security.action.GetPropertyAction;

public class Util {
    static final boolean $assertionsDisabled = false;
    private static final int TEMP_BUF_POOL_SIZE = IOUtil.IOV_MAX;
    private static ThreadLocal<BufferCache> bufferCache = new ThreadLocal<BufferCache>() {
        @Override
        protected BufferCache initialValue() {
            return new BufferCache();
        }
    };
    private static Unsafe unsafe = Unsafe.getUnsafe();
    private static int pageSize = -1;
    private static volatile String bugLevel = null;

    private static class BufferCache {
        static final boolean $assertionsDisabled = false;
        private ByteBuffer[] buffers = new ByteBuffer[Util.TEMP_BUF_POOL_SIZE];
        private int count;
        private int start;

        private int next(int i) {
            return (i + 1) % Util.TEMP_BUF_POOL_SIZE;
        }

        BufferCache() {
        }

        ByteBuffer get(int i) {
            ByteBuffer byteBuffer;
            if (this.count == 0) {
                return null;
            }
            ByteBuffer[] byteBufferArr = this.buffers;
            ByteBuffer byteBuffer2 = byteBufferArr[this.start];
            if (byteBuffer2.capacity() < i) {
                int next = this.start;
                do {
                    next = next(next);
                    if (next == this.start || (byteBuffer = byteBufferArr[next]) == null) {
                        byteBuffer = null;
                        break;
                    }
                } while (byteBuffer.capacity() < i);
                if (byteBuffer == null) {
                    return null;
                }
                byteBufferArr[next] = byteBufferArr[this.start];
                byteBuffer2 = byteBuffer;
            }
            byteBufferArr[this.start] = null;
            this.start = next(this.start);
            this.count--;
            byteBuffer2.rewind();
            byteBuffer2.limit(i);
            return byteBuffer2;
        }

        boolean offerFirst(ByteBuffer byteBuffer) {
            if (this.count < Util.TEMP_BUF_POOL_SIZE) {
                this.start = ((this.start + Util.TEMP_BUF_POOL_SIZE) - 1) % Util.TEMP_BUF_POOL_SIZE;
                this.buffers[this.start] = byteBuffer;
                this.count++;
                return true;
            }
            return false;
        }

        boolean offerLast(ByteBuffer byteBuffer) {
            if (this.count < Util.TEMP_BUF_POOL_SIZE) {
                this.buffers[(this.start + this.count) % Util.TEMP_BUF_POOL_SIZE] = byteBuffer;
                this.count++;
                return true;
            }
            return false;
        }

        boolean isEmpty() {
            return this.count == 0;
        }

        ByteBuffer removeFirst() {
            ByteBuffer byteBuffer = this.buffers[this.start];
            this.buffers[this.start] = null;
            this.start = next(this.start);
            this.count--;
            return byteBuffer;
        }
    }

    public static ByteBuffer getTemporaryDirectBuffer(int i) {
        BufferCache bufferCache2 = bufferCache.get();
        ByteBuffer byteBuffer = bufferCache2.get(i);
        if (byteBuffer != null) {
            return byteBuffer;
        }
        if (!bufferCache2.isEmpty()) {
            free(bufferCache2.removeFirst());
        }
        return ByteBuffer.allocateDirect(i);
    }

    public static void releaseTemporaryDirectBuffer(ByteBuffer byteBuffer) {
        offerFirstTemporaryDirectBuffer(byteBuffer);
    }

    static void offerFirstTemporaryDirectBuffer(ByteBuffer byteBuffer) {
        if (!bufferCache.get().offerFirst(byteBuffer)) {
            free(byteBuffer);
        }
    }

    static void offerLastTemporaryDirectBuffer(ByteBuffer byteBuffer) {
        if (!bufferCache.get().offerLast(byteBuffer)) {
            free(byteBuffer);
        }
    }

    private static void free(ByteBuffer byteBuffer) {
        Cleaner cleaner = ((DirectBuffer) byteBuffer).cleaner();
        if (cleaner != null) {
            cleaner.clean();
        }
    }

    static ByteBuffer[] subsequence(ByteBuffer[] byteBufferArr, int i, int i2) {
        if (i == 0 && i2 == byteBufferArr.length) {
            return byteBufferArr;
        }
        ByteBuffer[] byteBufferArr2 = new ByteBuffer[i2];
        for (int i3 = 0; i3 < i2; i3++) {
            byteBufferArr2[i3] = byteBufferArr[i + i3];
        }
        return byteBufferArr2;
    }

    static <E> Set<E> ungrowableSet(final Set<E> set) {
        return new Set<E>() {
            @Override
            public int size() {
                return set.size();
            }

            @Override
            public boolean isEmpty() {
                return set.isEmpty();
            }

            @Override
            public boolean contains(Object obj) {
                return set.contains(obj);
            }

            @Override
            public Object[] toArray() {
                return set.toArray();
            }

            @Override
            public <T> T[] toArray(T[] tArr) {
                return (T[]) set.toArray(tArr);
            }

            public String toString() {
                return set.toString();
            }

            @Override
            public Iterator<E> iterator() {
                return set.iterator();
            }

            @Override
            public boolean equals(Object obj) {
                return set.equals(obj);
            }

            @Override
            public int hashCode() {
                return set.hashCode();
            }

            @Override
            public void clear() {
                set.clear();
            }

            @Override
            public boolean remove(Object obj) {
                return set.remove(obj);
            }

            @Override
            public boolean containsAll(Collection<?> collection) {
                return set.containsAll(collection);
            }

            @Override
            public boolean removeAll(Collection<?> collection) {
                return set.removeAll(collection);
            }

            @Override
            public boolean retainAll(Collection<?> collection) {
                return set.retainAll(collection);
            }

            @Override
            public boolean add(E e) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean addAll(Collection<? extends E> collection) {
                throw new UnsupportedOperationException();
            }
        };
    }

    private static byte _get(long j) {
        return unsafe.getByte(j);
    }

    private static void _put(long j, byte b) {
        unsafe.putByte(j, b);
    }

    static void erase(ByteBuffer byteBuffer) {
        unsafe.setMemory(((DirectBuffer) byteBuffer).address(), byteBuffer.capacity(), (byte) 0);
    }

    static Unsafe unsafe() {
        return unsafe;
    }

    static int pageSize() {
        if (pageSize == -1) {
            pageSize = unsafe().pageSize();
        }
        return pageSize;
    }

    static boolean atBugLevel(String str) {
        if (bugLevel == null) {
            if (!VM.isBooted()) {
                return false;
            }
            String str2 = (String) AccessController.doPrivileged(new GetPropertyAction("sun.nio.ch.bugLevel"));
            if (str2 == null) {
                str2 = "";
            }
            bugLevel = str2;
        }
        return bugLevel.equals(str);
    }
}
