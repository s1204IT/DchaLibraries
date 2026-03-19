package org.apache.xml.utils;

public class StringBufferPool {
    private static ObjectPool m_stringBufPool = new ObjectPool(FastStringBuffer.class);

    public static synchronized FastStringBuffer get() {
        return (FastStringBuffer) m_stringBufPool.getInstance();
    }

    public static synchronized void free(FastStringBuffer fastStringBuffer) {
        fastStringBuffer.setLength(0);
        m_stringBufPool.freeInstance(fastStringBuffer);
    }
}
