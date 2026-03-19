package sun.nio.fs;

import sun.misc.Unsafe;

class NativeBuffers {
    static final boolean $assertionsDisabled = false;
    private static final int TEMP_BUF_POOL_SIZE = 3;
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    private static ThreadLocal<NativeBuffer[]> threadLocal = new ThreadLocal<>();

    private NativeBuffers() {
    }

    static NativeBuffer allocNativeBuffer(int i) {
        if (i < 2048) {
            i = 2048;
        }
        return new NativeBuffer(i);
    }

    static NativeBuffer getNativeBufferFromCache(int i) {
        NativeBuffer[] nativeBufferArr = threadLocal.get();
        if (nativeBufferArr != null) {
            for (int i2 = 0; i2 < 3; i2++) {
                NativeBuffer nativeBuffer = nativeBufferArr[i2];
                if (nativeBuffer != null && nativeBuffer.size() >= i) {
                    nativeBufferArr[i2] = null;
                    return nativeBuffer;
                }
            }
        }
        return null;
    }

    static NativeBuffer getNativeBuffer(int i) {
        NativeBuffer nativeBufferFromCache = getNativeBufferFromCache(i);
        if (nativeBufferFromCache != null) {
            nativeBufferFromCache.setOwner(null);
            return nativeBufferFromCache;
        }
        return allocNativeBuffer(i);
    }

    static void releaseNativeBuffer(NativeBuffer nativeBuffer) {
        NativeBuffer[] nativeBufferArr = threadLocal.get();
        if (nativeBufferArr == null) {
            NativeBuffer[] nativeBufferArr2 = new NativeBuffer[3];
            nativeBufferArr2[0] = nativeBuffer;
            threadLocal.set(nativeBufferArr2);
            return;
        }
        for (int i = 0; i < 3; i++) {
            if (nativeBufferArr[i] == null) {
                nativeBufferArr[i] = nativeBuffer;
                return;
            }
        }
        for (int i2 = 0; i2 < 3; i2++) {
            NativeBuffer nativeBuffer2 = nativeBufferArr[i2];
            if (nativeBuffer2.size() < nativeBuffer.size()) {
                nativeBuffer2.cleaner().clean();
                nativeBufferArr[i2] = nativeBuffer;
                return;
            }
        }
        nativeBuffer.cleaner().clean();
    }

    static void copyCStringToNativeBuffer(byte[] bArr, NativeBuffer nativeBuffer) {
        long length = bArr.length;
        int i = 0;
        while (true) {
            long j = i;
            if (j >= length) {
                unsafe.putByte(nativeBuffer.address() + length, (byte) 0);
                return;
            } else {
                unsafe.putByte(nativeBuffer.address() + j, bArr[i]);
                i++;
            }
        }
    }

    static NativeBuffer asNativeBuffer(byte[] bArr) {
        NativeBuffer nativeBuffer = getNativeBuffer(bArr.length + 1);
        copyCStringToNativeBuffer(bArr, nativeBuffer);
        return nativeBuffer;
    }
}
