package java.nio.charset;

import java.lang.ref.WeakReference;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.util.HashMap;
import java.util.Map;

public class CoderResult {
    static final boolean $assertionsDisabled = false;
    private static final int CR_ERROR_MIN = 2;
    private static final int CR_MALFORMED = 2;
    private static final int CR_OVERFLOW = 1;
    private static final int CR_UNDERFLOW = 0;
    private static final int CR_UNMAPPABLE = 3;
    private final int length;
    private final int type;
    private static final String[] names = {"UNDERFLOW", "OVERFLOW", "MALFORMED", "UNMAPPABLE"};
    public static final CoderResult UNDERFLOW = new CoderResult(0, 0);
    public static final CoderResult OVERFLOW = new CoderResult(1, 0);
    private static Cache malformedCache = new Cache() {
        @Override
        public CoderResult create(int i) {
            return new CoderResult(2, i);
        }
    };
    private static Cache unmappableCache = new Cache() {
        @Override
        public CoderResult create(int i) {
            return new CoderResult(3, i);
        }
    };

    private CoderResult(int i, int i2) {
        this.type = i;
        this.length = i2;
    }

    public String toString() {
        String str = names[this.type];
        if (!isError()) {
            return str;
        }
        return str + "[" + this.length + "]";
    }

    public boolean isUnderflow() {
        if (this.type == 0) {
            return true;
        }
        return $assertionsDisabled;
    }

    public boolean isOverflow() {
        if (this.type == 1) {
            return true;
        }
        return $assertionsDisabled;
    }

    public boolean isError() {
        if (this.type >= 2) {
            return true;
        }
        return $assertionsDisabled;
    }

    public boolean isMalformed() {
        if (this.type == 2) {
            return true;
        }
        return $assertionsDisabled;
    }

    public boolean isUnmappable() {
        if (this.type == 3) {
            return true;
        }
        return $assertionsDisabled;
    }

    public int length() {
        if (!isError()) {
            throw new UnsupportedOperationException();
        }
        return this.length;
    }

    private static abstract class Cache {
        private Map<Integer, WeakReference<CoderResult>> cache;

        protected abstract CoderResult create(int i);

        private Cache() {
            this.cache = null;
        }

        private synchronized CoderResult get(int i) {
            CoderResult coderResultCreate;
            if (i <= 0) {
                throw new IllegalArgumentException("Non-positive length");
            }
            Integer num = new Integer(i);
            coderResultCreate = null;
            if (this.cache == null) {
                this.cache = new HashMap();
            } else {
                WeakReference<CoderResult> weakReference = this.cache.get(num);
                if (weakReference != null) {
                    coderResultCreate = weakReference.get();
                }
            }
            if (coderResultCreate == null) {
                coderResultCreate = create(i);
                this.cache.put(num, new WeakReference<>(coderResultCreate));
            }
            return coderResultCreate;
        }
    }

    public static CoderResult malformedForLength(int i) {
        return malformedCache.get(i);
    }

    public static CoderResult unmappableForLength(int i) {
        return unmappableCache.get(i);
    }

    public void throwException() throws CharacterCodingException {
        switch (this.type) {
            case 0:
                throw new BufferUnderflowException();
            case 1:
                throw new BufferOverflowException();
            case 2:
                throw new MalformedInputException(this.length);
            case 3:
                throw new UnmappableCharacterException(this.length);
            default:
                return;
        }
    }
}
