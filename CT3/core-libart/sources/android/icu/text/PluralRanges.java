package android.icu.text;

import android.icu.impl.StandardPlural;
import android.icu.util.Freezable;
import android.icu.util.Output;
import java.util.Arrays;
import java.util.EnumSet;

@Deprecated
public final class PluralRanges implements Freezable<PluralRanges>, Comparable<PluralRanges> {
    private volatile boolean isFrozen;
    private Matrix matrix = new Matrix();
    private boolean[] explicit = new boolean[StandardPlural.COUNT];

    @Deprecated
    public PluralRanges() {
    }

    private static final class Matrix implements Comparable<Matrix>, Cloneable {
        private byte[] data = new byte[StandardPlural.COUNT * StandardPlural.COUNT];

        Matrix() {
            for (int i = 0; i < this.data.length; i++) {
                this.data[i] = -1;
            }
        }

        void set(StandardPlural start, StandardPlural end, StandardPlural result) {
            this.data[end.ordinal() + (start.ordinal() * StandardPlural.COUNT)] = result == null ? (byte) -1 : (byte) result.ordinal();
        }

        void setIfNew(StandardPlural start, StandardPlural end, StandardPlural result) {
            byte old = this.data[(start.ordinal() * StandardPlural.COUNT) + end.ordinal()];
            if (old >= 0) {
                throw new IllegalArgumentException("Previously set value for <" + start + ", " + end + ", " + StandardPlural.VALUES.get(old) + ">");
            }
            this.data[end.ordinal() + (start.ordinal() * StandardPlural.COUNT)] = result == null ? (byte) -1 : (byte) result.ordinal();
        }

        StandardPlural get(StandardPlural start, StandardPlural end) {
            byte result = this.data[(start.ordinal() * StandardPlural.COUNT) + end.ordinal()];
            if (result < 0) {
                return null;
            }
            return StandardPlural.VALUES.get(result);
        }

        StandardPlural endSame(StandardPlural end) {
            StandardPlural first = null;
            for (StandardPlural start : StandardPlural.VALUES) {
                StandardPlural item = get(start, end);
                if (item != null) {
                    if (first == null) {
                        first = item;
                    } else if (first != item) {
                        return null;
                    }
                }
            }
            return first;
        }

        StandardPlural startSame(StandardPlural start, EnumSet<StandardPlural> endDone, Output<Boolean> emit) {
            emit.value = false;
            StandardPlural first = null;
            for (StandardPlural end : StandardPlural.VALUES) {
                StandardPlural item = get(start, end);
                if (item != null) {
                    if (first == null) {
                        first = item;
                    } else {
                        if (first != item) {
                            return null;
                        }
                        if (!endDone.contains(end)) {
                            emit.value = true;
                        }
                    }
                }
            }
            return first;
        }

        public int hashCode() {
            int result = 0;
            for (int i = 0; i < this.data.length; i++) {
                result = (result * 37) + this.data[i];
            }
            return result;
        }

        public boolean equals(Object other) {
            return (other instanceof Matrix) && compareTo((Matrix) other) == 0;
        }

        @Override
        public int compareTo(Matrix o) {
            for (int i = 0; i < this.data.length; i++) {
                int diff = this.data[i] - o.data[i];
                if (diff != 0) {
                    return diff;
                }
            }
            return 0;
        }

        public Matrix m180clone() {
            Matrix result = new Matrix();
            result.data = (byte[]) this.data.clone();
            return result;
        }

        public String toString() {
            StringBuilder result = new StringBuilder();
            for (StandardPlural i : StandardPlural.valuesCustom()) {
                for (StandardPlural j : StandardPlural.valuesCustom()) {
                    StandardPlural x = get(i, j);
                    if (x != null) {
                        result.append(i).append(" & ").append(j).append(" → ").append(x).append(";\n");
                    }
                }
            }
            return result.toString();
        }
    }

    @Deprecated
    public void add(StandardPlural rangeStart, StandardPlural rangeEnd, StandardPlural result) {
        if (this.isFrozen) {
            throw new UnsupportedOperationException();
        }
        this.explicit[result.ordinal()] = true;
        if (rangeStart == null) {
            for (StandardPlural rs : StandardPlural.valuesCustom()) {
                if (rangeEnd == null) {
                    for (StandardPlural re : StandardPlural.valuesCustom()) {
                        this.matrix.setIfNew(rs, re, result);
                    }
                } else {
                    this.explicit[rangeEnd.ordinal()] = true;
                    this.matrix.setIfNew(rs, rangeEnd, result);
                }
            }
            return;
        }
        if (rangeEnd == null) {
            this.explicit[rangeStart.ordinal()] = true;
            for (StandardPlural re2 : StandardPlural.valuesCustom()) {
                this.matrix.setIfNew(rangeStart, re2, result);
            }
            return;
        }
        this.explicit[rangeStart.ordinal()] = true;
        this.explicit[rangeEnd.ordinal()] = true;
        this.matrix.setIfNew(rangeStart, rangeEnd, result);
    }

    @Deprecated
    public StandardPlural get(StandardPlural start, StandardPlural end) {
        StandardPlural result = this.matrix.get(start, end);
        return result == null ? end : result;
    }

    @Deprecated
    public boolean isExplicit(StandardPlural start, StandardPlural end) {
        return this.matrix.get(start, end) != null;
    }

    @Deprecated
    public boolean isExplicitlySet(StandardPlural count) {
        return this.explicit[count.ordinal()];
    }

    @Deprecated
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PluralRanges)) {
            return false;
        }
        PluralRanges otherPR = (PluralRanges) other;
        if (this.matrix.equals(otherPR.matrix)) {
            return Arrays.equals(this.explicit, otherPR.explicit);
        }
        return false;
    }

    @Deprecated
    public int hashCode() {
        return this.matrix.hashCode();
    }

    @Override
    @Deprecated
    public int compareTo(PluralRanges that) {
        return this.matrix.compareTo(that.matrix);
    }

    @Override
    @Deprecated
    public boolean isFrozen() {
        return this.isFrozen;
    }

    @Override
    @Deprecated
    public PluralRanges freeze() {
        this.isFrozen = true;
        return this;
    }

    @Override
    @Deprecated
    public PluralRanges cloneAsThawed() {
        PluralRanges result = new PluralRanges();
        result.explicit = (boolean[]) this.explicit.clone();
        result.matrix = this.matrix.m180clone();
        return result;
    }

    @Deprecated
    public String toString() {
        return this.matrix.toString();
    }
}
