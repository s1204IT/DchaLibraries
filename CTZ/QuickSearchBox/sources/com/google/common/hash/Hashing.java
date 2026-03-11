package com.google.common.hash;

public final class Hashing {
    private static final int GOOD_FAST_HASH_SEED = (int) System.currentTimeMillis();

    static final class ConcatenatedHashFunction extends AbstractCompositeHashFunction {
        private final int bits;

        public boolean equals(Object obj) {
            if (!(obj instanceof ConcatenatedHashFunction)) {
                return false;
            }
            ConcatenatedHashFunction concatenatedHashFunction = (ConcatenatedHashFunction) obj;
            if (this.bits != concatenatedHashFunction.bits || this.functions.length != concatenatedHashFunction.functions.length) {
                return false;
            }
            for (int i = 0; i < this.functions.length; i++) {
                if (!this.functions[i].equals(concatenatedHashFunction.functions[i])) {
                    return false;
                }
            }
            return true;
        }

        public int hashCode() {
            int iHashCode = this.bits;
            for (HashFunction hashFunction : this.functions) {
                iHashCode ^= hashFunction.hashCode();
            }
            return iHashCode;
        }
    }
}
