package dalvik.system;

import java.nio.ByteBuffer;

public final class InMemoryDexClassLoader extends BaseDexClassLoader {
    public InMemoryDexClassLoader(ByteBuffer[] byteBufferArr, ClassLoader classLoader) {
        super(byteBufferArr, classLoader);
    }

    public InMemoryDexClassLoader(ByteBuffer byteBuffer, ClassLoader classLoader) {
        this(new ByteBuffer[]{byteBuffer}, classLoader);
    }
}
