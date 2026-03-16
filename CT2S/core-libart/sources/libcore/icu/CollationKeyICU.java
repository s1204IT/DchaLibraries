package libcore.icu;

import dalvik.bytecode.Opcodes;
import java.text.CollationKey;

public final class CollationKeyICU extends CollationKey {
    private final byte[] bytes;
    private int hashCode;

    CollationKeyICU(String source, byte[] bytes) {
        super(source);
        this.bytes = bytes;
    }

    @Override
    public int compareTo(CollationKey other) {
        byte[] rhsBytes;
        if (other instanceof CollationKeyICU) {
            rhsBytes = ((CollationKeyICU) other).bytes;
        } else {
            rhsBytes = other.toByteArray();
        }
        if (this.bytes == null || this.bytes.length == 0) {
            return (rhsBytes == null || rhsBytes.length == 0) ? 0 : -1;
        }
        if (rhsBytes == null || rhsBytes.length == 0) {
            return 1;
        }
        int count = Math.min(this.bytes.length, rhsBytes.length);
        for (int i = 0; i < count; i++) {
            int s = this.bytes[i] & Opcodes.OP_CONST_CLASS_JUMBO;
            int t = rhsBytes[i] & Opcodes.OP_CONST_CLASS_JUMBO;
            if (s < t) {
                return -1;
            }
            if (s > t) {
                return 1;
            }
        }
        if (this.bytes.length >= rhsBytes.length) {
            return this.bytes.length > rhsBytes.length ? 1 : 0;
        }
        return -1;
    }

    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        return (object instanceof CollationKey) && compareTo((CollationKey) object) == 0;
    }

    public int hashCode() {
        if (this.hashCode == 0) {
            if (this.bytes != null && this.bytes.length != 0) {
                int len = this.bytes.length;
                int inc = ((len - 32) / 32) + 1;
                for (int i = 0; i < len; i += inc) {
                    this.hashCode = (this.hashCode * 37) + this.bytes[i];
                }
            }
            if (this.hashCode == 0) {
                this.hashCode = 1;
            }
        }
        return this.hashCode;
    }

    @Override
    public byte[] toByteArray() {
        if (this.bytes == null || this.bytes.length == 0) {
            return null;
        }
        return (byte[]) this.bytes.clone();
    }
}
