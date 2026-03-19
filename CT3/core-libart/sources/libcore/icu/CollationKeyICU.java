package libcore.icu;

import java.text.CollationKey;

public final class CollationKeyICU extends CollationKey {
    private final android.icu.text.CollationKey key;

    public CollationKeyICU(String source, android.icu.text.CollationKey key) {
        super(source);
        this.key = key;
    }

    @Override
    public int compareTo(CollationKey other) {
        android.icu.text.CollationKey otherKey;
        if (other instanceof CollationKeyICU) {
            otherKey = ((CollationKeyICU) other).key;
        } else {
            otherKey = new android.icu.text.CollationKey(other.getSourceString(), other.toByteArray());
        }
        return this.key.compareTo(otherKey);
    }

    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        return (object instanceof CollationKey) && compareTo((CollationKey) object) == 0;
    }

    public int hashCode() {
        return this.key.hashCode();
    }

    @Override
    public byte[] toByteArray() {
        return this.key.toByteArray();
    }
}
