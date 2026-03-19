package com.android.org.conscrypt;

import java.nio.ByteBuffer;

public abstract class AllocatedBuffer {
    public abstract ByteBuffer nioBuffer();

    public abstract AllocatedBuffer release();

    public abstract AllocatedBuffer retain();

    public static AllocatedBuffer wrap(final ByteBuffer byteBuffer) {
        Preconditions.checkNotNull(byteBuffer, "buffer");
        return new AllocatedBuffer() {
            @Override
            public ByteBuffer nioBuffer() {
                return byteBuffer;
            }

            @Override
            public AllocatedBuffer retain() {
                return this;
            }

            @Override
            public AllocatedBuffer release() {
                return this;
            }
        };
    }
}
