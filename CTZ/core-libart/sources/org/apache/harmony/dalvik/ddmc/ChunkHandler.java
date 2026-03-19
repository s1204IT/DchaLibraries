package org.apache.harmony.dalvik.ddmc;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public abstract class ChunkHandler {
    public static final ByteOrder CHUNK_ORDER = ByteOrder.BIG_ENDIAN;
    public static final int CHUNK_FAIL = type("FAIL");

    public abstract void connected();

    public abstract void disconnected();

    public abstract Chunk handleChunk(Chunk chunk);

    public static Chunk createFailChunk(int i, String str) {
        if (str == null) {
            str = "";
        }
        ByteBuffer byteBufferAllocate = ByteBuffer.allocate(8 + (str.length() * 2));
        byteBufferAllocate.order(CHUNK_ORDER);
        byteBufferAllocate.putInt(i);
        byteBufferAllocate.putInt(str.length());
        putString(byteBufferAllocate, str);
        return new Chunk(CHUNK_FAIL, byteBufferAllocate);
    }

    public static ByteBuffer wrapChunk(Chunk chunk) {
        ByteBuffer byteBufferWrap = ByteBuffer.wrap(chunk.data, chunk.offset, chunk.length);
        byteBufferWrap.order(CHUNK_ORDER);
        return byteBufferWrap;
    }

    public static String getString(ByteBuffer byteBuffer, int i) {
        char[] cArr = new char[i];
        for (int i2 = 0; i2 < i; i2++) {
            cArr[i2] = byteBuffer.getChar();
        }
        return new String(cArr);
    }

    public static void putString(ByteBuffer byteBuffer, String str) {
        int length = str.length();
        for (int i = 0; i < length; i++) {
            byteBuffer.putChar(str.charAt(i));
        }
    }

    public static int type(String str) {
        if (str.length() != 4) {
            throw new IllegalArgumentException("Bad type name: " + str);
        }
        int iCharAt = 0;
        for (int i = 0; i < 4; i++) {
            iCharAt = (iCharAt << 8) | (str.charAt(i) & 255);
        }
        return iCharAt;
    }

    public static String name(int i) {
        return new String(new char[]{(char) ((i >> 24) & 255), (char) ((i >> 16) & 255), (char) ((i >> 8) & 255), (char) (i & 255)});
    }
}
