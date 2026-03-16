package java.nio.charset;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.util.WeakHashMap;

public class CoderResult {
    private static final int TYPE_MALFORMED_INPUT = 3;
    private static final int TYPE_OVERFLOW = 2;
    private static final int TYPE_UNDERFLOW = 1;
    private static final int TYPE_UNMAPPABLE_CHAR = 4;
    private final int length;
    private final int type;
    public static final CoderResult UNDERFLOW = new CoderResult(1, 0);
    public static final CoderResult OVERFLOW = new CoderResult(2, 0);
    private static WeakHashMap<Integer, CoderResult> _malformedErrors = new WeakHashMap<>();
    private static WeakHashMap<Integer, CoderResult> _unmappableErrors = new WeakHashMap<>();

    private CoderResult(int type, int length) {
        this.type = type;
        this.length = length;
    }

    public static synchronized CoderResult malformedForLength(int length) throws IllegalArgumentException {
        CoderResult r;
        if (length > 0) {
            Integer key = Integer.valueOf(length);
            synchronized (_malformedErrors) {
                r = _malformedErrors.get(key);
                if (r == null) {
                    r = new CoderResult(3, length);
                    _malformedErrors.put(key, r);
                }
            }
        } else {
            throw new IllegalArgumentException("length <= 0: " + length);
        }
        return r;
    }

    public static synchronized CoderResult unmappableForLength(int length) throws IllegalArgumentException {
        CoderResult r;
        if (length > 0) {
            Integer key = Integer.valueOf(length);
            synchronized (_unmappableErrors) {
                r = _unmappableErrors.get(key);
                if (r == null) {
                    r = new CoderResult(4, length);
                    _unmappableErrors.put(key, r);
                }
            }
        } else {
            throw new IllegalArgumentException("length <= 0: " + length);
        }
        return r;
    }

    public boolean isUnderflow() {
        return this.type == 1;
    }

    public boolean isError() {
        return this.type == 3 || this.type == 4;
    }

    public boolean isMalformed() {
        return this.type == 3;
    }

    public boolean isOverflow() {
        return this.type == 2;
    }

    public boolean isUnmappable() {
        return this.type == 4;
    }

    public int length() throws UnsupportedOperationException {
        if (this.type == 3 || this.type == 4) {
            return this.length;
        }
        throw new UnsupportedOperationException("length meaningless for " + toString());
    }

    public void throwException() throws CharacterCodingException, BufferOverflowException, BufferUnderflowException {
        switch (this.type) {
            case 1:
                throw new BufferUnderflowException();
            case 2:
                throw new BufferOverflowException();
            case 3:
                throw new MalformedInputException(this.length);
            case 4:
                throw new UnmappableCharacterException(this.length);
            default:
                throw new CharacterCodingException();
        }
    }

    public String toString() {
        String dsc;
        switch (this.type) {
            case 1:
                dsc = "UNDERFLOW error";
                break;
            case 2:
                dsc = "OVERFLOW error";
                break;
            case 3:
                dsc = "Malformed-input error with erroneous input length " + this.length;
                break;
            case 4:
                dsc = "Unmappable-character error with erroneous input length " + this.length;
                break;
            default:
                dsc = "";
                break;
        }
        return getClass().getName() + "[" + dsc + "]";
    }
}
