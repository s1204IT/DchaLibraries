package android.icu.text;

import android.icu.impl.StandardPlural;
import android.icu.util.Freezable;
import android.icu.util.Output;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Iterator;

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

        void set(StandardPlural standardPlural, StandardPlural standardPlural2, StandardPlural standardPlural3) {
            this.data[(standardPlural.ordinal() * StandardPlural.COUNT) + standardPlural2.ordinal()] = standardPlural3 == null ? (byte) -1 : (byte) standardPlural3.ordinal();
        }

        void setIfNew(StandardPlural standardPlural, StandardPlural standardPlural2, StandardPlural standardPlural3) {
            byte b = this.data[(standardPlural.ordinal() * StandardPlural.COUNT) + standardPlural2.ordinal()];
            if (b >= 0) {
                throw new IllegalArgumentException("Previously set value for <" + standardPlural + ", " + standardPlural2 + ", " + StandardPlural.VALUES.get(b) + ">");
            }
            this.data[(standardPlural.ordinal() * StandardPlural.COUNT) + standardPlural2.ordinal()] = standardPlural3 == null ? (byte) -1 : (byte) standardPlural3.ordinal();
        }

        StandardPlural get(StandardPlural standardPlural, StandardPlural standardPlural2) {
            byte b = this.data[(standardPlural.ordinal() * StandardPlural.COUNT) + standardPlural2.ordinal()];
            if (b < 0) {
                return null;
            }
            return StandardPlural.VALUES.get(b);
        }

        StandardPlural endSame(StandardPlural standardPlural) {
            Iterator<StandardPlural> it = StandardPlural.VALUES.iterator();
            StandardPlural standardPlural2 = null;
            while (it.hasNext()) {
                StandardPlural standardPlural3 = get(it.next(), standardPlural);
                if (standardPlural3 != null) {
                    if (standardPlural2 == null) {
                        standardPlural2 = standardPlural3;
                    } else if (standardPlural2 != standardPlural3) {
                        return null;
                    }
                }
            }
            return standardPlural2;
        }

        StandardPlural startSame(StandardPlural standardPlural, EnumSet<StandardPlural> enumSet, Output<Boolean> output) {
            output.value = false;
            StandardPlural standardPlural2 = null;
            for (StandardPlural standardPlural3 : StandardPlural.VALUES) {
                StandardPlural standardPlural4 = get(standardPlural, standardPlural3);
                if (standardPlural4 != null) {
                    if (standardPlural2 == null) {
                        standardPlural2 = standardPlural4;
                    } else {
                        if (standardPlural2 != standardPlural4) {
                            return null;
                        }
                        if (!enumSet.contains(standardPlural3)) {
                            output.value = true;
                        }
                    }
                }
            }
            return standardPlural2;
        }

        public int hashCode() {
            int i = 0;
            for (int i2 = 0; i2 < this.data.length; i2++) {
                i = (i * 37) + this.data[i2];
            }
            return i;
        }

        public boolean equals(Object obj) {
            return (obj instanceof Matrix) && compareTo((Matrix) obj) == 0;
        }

        @Override
        public int compareTo(Matrix matrix) {
            for (int i = 0; i < this.data.length; i++) {
                int i2 = this.data[i] - matrix.data[i];
                if (i2 != 0) {
                    return i2;
                }
            }
            return 0;
        }

        public Matrix m4clone() {
            Matrix matrix = new Matrix();
            matrix.data = (byte[]) this.data.clone();
            return matrix;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (StandardPlural standardPlural : StandardPlural.values()) {
                for (StandardPlural standardPlural2 : StandardPlural.values()) {
                    StandardPlural standardPlural3 = get(standardPlural, standardPlural2);
                    if (standardPlural3 != null) {
                        sb.append(standardPlural + " & " + standardPlural2 + " → " + standardPlural3 + ";\n");
                    }
                }
            }
            return sb.toString();
        }
    }

    @Deprecated
    public void add(StandardPlural standardPlural, StandardPlural standardPlural2, StandardPlural standardPlural3) {
        if (this.isFrozen) {
            throw new UnsupportedOperationException();
        }
        this.explicit[standardPlural3.ordinal()] = true;
        if (standardPlural == null) {
            for (StandardPlural standardPlural4 : StandardPlural.values()) {
                if (standardPlural2 == null) {
                    for (StandardPlural standardPlural5 : StandardPlural.values()) {
                        this.matrix.setIfNew(standardPlural4, standardPlural5, standardPlural3);
                    }
                } else {
                    this.explicit[standardPlural2.ordinal()] = true;
                    this.matrix.setIfNew(standardPlural4, standardPlural2, standardPlural3);
                }
            }
            return;
        }
        if (standardPlural2 == null) {
            this.explicit[standardPlural.ordinal()] = true;
            for (StandardPlural standardPlural6 : StandardPlural.values()) {
                this.matrix.setIfNew(standardPlural, standardPlural6, standardPlural3);
            }
            return;
        }
        this.explicit[standardPlural.ordinal()] = true;
        this.explicit[standardPlural2.ordinal()] = true;
        this.matrix.setIfNew(standardPlural, standardPlural2, standardPlural3);
    }

    @Deprecated
    public StandardPlural get(StandardPlural standardPlural, StandardPlural standardPlural2) {
        StandardPlural standardPlural3 = this.matrix.get(standardPlural, standardPlural2);
        return standardPlural3 == null ? standardPlural2 : standardPlural3;
    }

    @Deprecated
    public boolean isExplicit(StandardPlural standardPlural, StandardPlural standardPlural2) {
        return this.matrix.get(standardPlural, standardPlural2) != null;
    }

    @Deprecated
    public boolean isExplicitlySet(StandardPlural standardPlural) {
        return this.explicit[standardPlural.ordinal()];
    }

    @Deprecated
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PluralRanges)) {
            return false;
        }
        PluralRanges pluralRanges = (PluralRanges) obj;
        return this.matrix.equals(pluralRanges.matrix) && Arrays.equals(this.explicit, pluralRanges.explicit);
    }

    @Deprecated
    public int hashCode() {
        return this.matrix.hashCode();
    }

    @Override
    @Deprecated
    public int compareTo(PluralRanges pluralRanges) {
        return this.matrix.compareTo(pluralRanges.matrix);
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
        PluralRanges pluralRanges = new PluralRanges();
        pluralRanges.explicit = (boolean[]) this.explicit.clone();
        pluralRanges.matrix = this.matrix.m4clone();
        return pluralRanges;
    }

    @Deprecated
    public String toString() {
        return this.matrix.toString();
    }
}
