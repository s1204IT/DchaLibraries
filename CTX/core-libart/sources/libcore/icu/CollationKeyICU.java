package libcore.icu;

import java.text.CollationKey;

public final class CollationKeyICU extends CollationKey {
    private final android.icu.text.CollationKey key;

    public CollationKeyICU(String str, android.icu.text.CollationKey collationKey) {
        super(str);
        this.key = collationKey;
    }

    @Override
    public int compareTo(CollationKey collationKey) {
        android.icu.text.CollationKey collationKey2;
        if (collationKey instanceof CollationKeyICU) {
            collationKey2 = ((CollationKeyICU) collationKey).key;
        } else {
            collationKey2 = new android.icu.text.CollationKey(collationKey.getSourceString(), collationKey.toByteArray());
        }
        return this.key.compareTo(collationKey2);
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        return (obj instanceof CollationKey) && compareTo((CollationKey) obj) == 0;
    }

    public int hashCode() {
        return this.key.hashCode();
    }

    @Override
    public byte[] toByteArray() {
        return this.key.toByteArray();
    }
}
