package sun.nio.cs;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

public class ThreadLocalCoders {
    private static final int CACHE_SIZE = 3;
    private static Cache decoderCache;
    private static Cache encoderCache;

    private static abstract class Cache {
        private ThreadLocal<Object[]> cache = new ThreadLocal<>();
        private final int size;

        abstract Object create(Object obj);

        abstract boolean hasName(Object obj, Object obj2);

        Cache(int i) {
            this.size = i;
        }

        private void moveToFront(Object[] objArr, int i) {
            Object obj = objArr[i];
            while (i > 0) {
                objArr[i] = objArr[i - 1];
                i--;
            }
            objArr[0] = obj;
        }

        Object forName(Object obj) {
            Object[] objArr = this.cache.get();
            if (objArr == null) {
                objArr = new Object[this.size];
                this.cache.set(objArr);
            } else {
                for (int i = 0; i < objArr.length; i++) {
                    Object obj2 = objArr[i];
                    if (obj2 != null && hasName(obj2, obj)) {
                        if (i > 0) {
                            moveToFront(objArr, i);
                        }
                        return obj2;
                    }
                }
            }
            Object objCreate = create(obj);
            objArr[objArr.length - 1] = objCreate;
            moveToFront(objArr, objArr.length - 1);
            return objCreate;
        }
    }

    static {
        int i = 3;
        decoderCache = new Cache(i) {
            static final boolean $assertionsDisabled = false;

            @Override
            boolean hasName(Object obj, Object obj2) {
                if (obj2 instanceof String) {
                    return ((CharsetDecoder) obj).charset().name().equals(obj2);
                }
                if (obj2 instanceof Charset) {
                    return ((CharsetDecoder) obj).charset().equals(obj2);
                }
                return false;
            }

            @Override
            Object create(Object obj) {
                if (obj instanceof String) {
                    return Charset.forName((String) obj).newDecoder();
                }
                if (obj instanceof Charset) {
                    return ((Charset) obj).newDecoder();
                }
                return null;
            }
        };
        encoderCache = new Cache(i) {
            static final boolean $assertionsDisabled = false;

            @Override
            boolean hasName(Object obj, Object obj2) {
                if (obj2 instanceof String) {
                    return ((CharsetEncoder) obj).charset().name().equals(obj2);
                }
                if (obj2 instanceof Charset) {
                    return ((CharsetEncoder) obj).charset().equals(obj2);
                }
                return false;
            }

            @Override
            Object create(Object obj) {
                if (obj instanceof String) {
                    return Charset.forName((String) obj).newEncoder();
                }
                if (obj instanceof Charset) {
                    return ((Charset) obj).newEncoder();
                }
                return null;
            }
        };
    }

    public static CharsetDecoder decoderFor(Object obj) {
        CharsetDecoder charsetDecoder = (CharsetDecoder) decoderCache.forName(obj);
        charsetDecoder.reset();
        return charsetDecoder;
    }

    public static CharsetEncoder encoderFor(Object obj) {
        CharsetEncoder charsetEncoder = (CharsetEncoder) encoderCache.forName(obj);
        charsetEncoder.reset();
        return charsetEncoder;
    }
}
