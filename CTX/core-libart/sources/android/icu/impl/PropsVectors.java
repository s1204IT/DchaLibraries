package android.icu.impl;

import android.icu.impl.Trie;
import android.icu.impl.TrieBuilder;
import java.util.Arrays;
import java.util.Comparator;

public class PropsVectors {
    public static final int ERROR_VALUE_CP = 1114113;
    public static final int FIRST_SPECIAL_CP = 1114112;
    public static final int INITIAL_ROWS = 4096;
    public static final int INITIAL_VALUE_CP = 1114112;
    public static final int MAX_CP = 1114113;
    public static final int MAX_ROWS = 1114114;
    public static final int MEDIUM_ROWS = 65536;
    private int columns;
    private boolean isCompacted;
    private int maxRows;
    private int prevRow;
    private int rows;
    private int[] v;

    public interface CompactHandler {
        void setRowIndexForErrorValue(int i);

        void setRowIndexForInitialValue(int i);

        void setRowIndexForRange(int i, int i2, int i3);

        void startRealValues(int i);
    }

    private boolean areElementsSame(int i, int[] iArr, int i2, int i3) {
        for (int i4 = 0; i4 < i3; i4++) {
            if (this.v[i + i4] != iArr[i2 + i4]) {
                return false;
            }
        }
        return true;
    }

    private int findRow(int i) {
        int i2 = this.prevRow * this.columns;
        int i3 = 0;
        if (i >= this.v[i2]) {
            if (i < this.v[i2 + 1]) {
                return i2;
            }
            int i4 = i2 + this.columns;
            if (i < this.v[i4 + 1]) {
                this.prevRow++;
                return i4;
            }
            int i5 = i4 + this.columns;
            int i6 = i5 + 1;
            if (i < this.v[i6]) {
                this.prevRow += 2;
                return i5;
            }
            if (i - this.v[i6] < 10) {
                this.prevRow += 2;
                do {
                    this.prevRow++;
                    i5 += this.columns;
                } while (i >= this.v[i5 + 1]);
                return i5;
            }
        } else if (i < this.v[1]) {
            this.prevRow = 0;
            return 0;
        }
        int i7 = this.rows;
        while (i3 < i7 - 1) {
            int i8 = (i3 + i7) / 2;
            int i9 = this.columns * i8;
            if (i < this.v[i9]) {
                i7 = i8;
            } else if (i >= this.v[i9 + 1]) {
                i3 = i8;
            } else {
                this.prevRow = i8;
                return i9;
            }
        }
        this.prevRow = i3;
        return i3 * this.columns;
    }

    public PropsVectors(int i) {
        if (i < 1) {
            throw new IllegalArgumentException("numOfColumns need to be no less than 1; but it is " + i);
        }
        this.columns = i + 2;
        this.v = new int[this.columns * 4096];
        this.maxRows = 4096;
        this.rows = 3;
        this.prevRow = 0;
        this.isCompacted = false;
        this.v[0] = 0;
        int i2 = 1114112;
        this.v[1] = 1114112;
        int i3 = this.columns;
        while (i2 <= 1114113) {
            this.v[i3] = i2;
            i2++;
            this.v[i3 + 1] = i2;
            i3 += this.columns;
        }
    }

    public void setValue(int i, int i2, int i3, int i4, int i5) {
        int i6;
        if (i < 0 || i > i2 || i2 > 1114113 || i3 < 0 || i3 >= this.columns - 2) {
            throw new IllegalArgumentException();
        }
        if (this.isCompacted) {
            throw new IllegalStateException("Shouldn't be called aftercompact()!");
        }
        int i7 = i2 + 1;
        int i8 = i3 + 2;
        int i9 = i4 & i5;
        int iFindRow = findRow(i);
        int iFindRow2 = findRow(i2);
        boolean z = (i == this.v[iFindRow] || i9 == (this.v[iFindRow + i8] & i5)) ? false : true;
        boolean z2 = (i7 == this.v[iFindRow2 + 1] || i9 == (this.v[iFindRow2 + i8] & i5)) ? false : true;
        if (z || z2) {
            int i10 = z ? 1 : 0;
            if (z2) {
                i10++;
            }
            if (this.rows + i10 > this.maxRows) {
                int i11 = this.maxRows;
                int i12 = MAX_ROWS;
                if (i11 >= 65536) {
                    if (this.maxRows >= 1114114) {
                        throw new IndexOutOfBoundsException("MAX_ROWS exceeded! Increase it to a higher valuein the implementation");
                    }
                } else {
                    i12 = 65536;
                }
                int[] iArr = new int[this.columns * i12];
                i6 = i9;
                System.arraycopy(this.v, 0, iArr, 0, this.rows * this.columns);
                this.v = iArr;
                this.maxRows = i12;
            } else {
                i6 = i9;
            }
            int i13 = (this.rows * this.columns) - (this.columns + iFindRow2);
            if (i13 > 0) {
                System.arraycopy(this.v, this.columns + iFindRow2, this.v, ((1 + i10) * this.columns) + iFindRow2, i13);
            }
            this.rows += i10;
            if (z) {
                System.arraycopy(this.v, iFindRow, this.v, this.columns + iFindRow, (iFindRow2 - iFindRow) + this.columns);
                iFindRow2 += this.columns;
                this.v[this.columns + iFindRow] = i;
                this.v[iFindRow + 1] = i;
                iFindRow += this.columns;
            }
            if (z2) {
                System.arraycopy(this.v, iFindRow2, this.v, this.columns + iFindRow2, this.columns);
                this.v[this.columns + iFindRow2] = i7;
                this.v[iFindRow2 + 1] = i7;
            }
        } else {
            i6 = i9;
        }
        this.prevRow = iFindRow2 / this.columns;
        int i14 = iFindRow + i8;
        int i15 = iFindRow2 + i8;
        int i16 = ~i5;
        while (true) {
            this.v[i14] = (this.v[i14] & i16) | i6;
            if (i14 != i15) {
                i14 += this.columns;
            } else {
                return;
            }
        }
    }

    public int getValue(int i, int i2) {
        if (this.isCompacted || i < 0 || i > 1114113 || i2 < 0 || i2 >= this.columns - 2) {
            return 0;
        }
        return this.v[findRow(i) + 2 + i2];
    }

    public int[] getRow(int i) {
        if (this.isCompacted) {
            throw new IllegalStateException("Illegal Invocation of the method after compact()");
        }
        if (i < 0 || i > this.rows) {
            throw new IllegalArgumentException("rowIndex out of bound!");
        }
        int[] iArr = new int[this.columns - 2];
        System.arraycopy(this.v, (i * this.columns) + 2, iArr, 0, this.columns - 2);
        return iArr;
    }

    public int getRowStart(int i) {
        if (this.isCompacted) {
            throw new IllegalStateException("Illegal Invocation of the method after compact()");
        }
        if (i < 0 || i > this.rows) {
            throw new IllegalArgumentException("rowIndex out of bound!");
        }
        return this.v[i * this.columns];
    }

    public int getRowEnd(int i) {
        if (this.isCompacted) {
            throw new IllegalStateException("Illegal Invocation of the method after compact()");
        }
        if (i < 0 || i > this.rows) {
            throw new IllegalArgumentException("rowIndex out of bound!");
        }
        return this.v[(i * this.columns) + 1] - 1;
    }

    public void compact(CompactHandler compactHandler) {
        if (this.isCompacted) {
            return;
        }
        this.isCompacted = true;
        int i = this.columns - 2;
        Integer[] numArr = new Integer[this.rows];
        for (int i2 = 0; i2 < this.rows; i2++) {
            numArr[i2] = Integer.valueOf(this.columns * i2);
        }
        Arrays.sort(numArr, new Comparator<Integer>() {
            @Override
            public int compare(Integer num, Integer num2) {
                int iIntValue = num.intValue();
                int iIntValue2 = num2.intValue();
                int i3 = PropsVectors.this.columns;
                int i4 = 2;
                do {
                    int i5 = iIntValue + i4;
                    int i6 = iIntValue2 + i4;
                    if (PropsVectors.this.v[i5] != PropsVectors.this.v[i6]) {
                        return PropsVectors.this.v[i5] < PropsVectors.this.v[i6] ? -1 : 1;
                    }
                    i4++;
                    if (i4 == PropsVectors.this.columns) {
                        i4 = 0;
                    }
                    i3--;
                } while (i3 > 0);
                return 0;
            }
        });
        int i3 = -i;
        int i4 = i3;
        for (int i5 = 0; i5 < this.rows; i5++) {
            int i6 = this.v[numArr[i5].intValue()];
            if (i4 < 0 || !areElementsSame(numArr[i5].intValue() + 2, this.v, numArr[i5 - 1].intValue() + 2, i)) {
                i4 += i;
            }
            if (i6 == 1114112) {
                compactHandler.setRowIndexForInitialValue(i4);
            } else if (i6 == 1114113) {
                compactHandler.setRowIndexForErrorValue(i4);
            }
        }
        int i7 = i4 + i;
        compactHandler.startRealValues(i7);
        int[] iArr = new int[i7];
        for (int i8 = 0; i8 < this.rows; i8++) {
            int i9 = this.v[numArr[i8].intValue()];
            int i10 = this.v[numArr[i8].intValue() + 1];
            if (i3 < 0 || !areElementsSame(numArr[i8].intValue() + 2, iArr, i3, i)) {
                i3 += i;
                System.arraycopy(this.v, numArr[i8].intValue() + 2, iArr, i3, i);
            }
            if (i9 < 1114112) {
                compactHandler.setRowIndexForRange(i9, i10 - 1, i3);
            }
        }
        this.v = iArr;
        this.rows = (i3 / i) + 1;
    }

    public int[] getCompactedArray() {
        if (!this.isCompacted) {
            throw new IllegalStateException("Illegal Invocation of the method before compact()");
        }
        return this.v;
    }

    public int getCompactedRows() {
        if (!this.isCompacted) {
            throw new IllegalStateException("Illegal Invocation of the method before compact()");
        }
        return this.rows;
    }

    public int getCompactedColumns() {
        if (!this.isCompacted) {
            throw new IllegalStateException("Illegal Invocation of the method before compact()");
        }
        return this.columns - 2;
    }

    public IntTrie compactToTrieWithRowIndexes() {
        PVecToTrieCompactHandler pVecToTrieCompactHandler = new PVecToTrieCompactHandler();
        compact(pVecToTrieCompactHandler);
        return pVecToTrieCompactHandler.builder.serialize(new DefaultGetFoldedValue(pVecToTrieCompactHandler.builder), new DefaultGetFoldingOffset());
    }

    private static class DefaultGetFoldingOffset implements Trie.DataManipulate {
        private DefaultGetFoldingOffset() {
        }

        @Override
        public int getFoldingOffset(int i) {
            return i;
        }
    }

    private static class DefaultGetFoldedValue implements TrieBuilder.DataManipulate {
        private IntTrieBuilder builder;

        public DefaultGetFoldedValue(IntTrieBuilder intTrieBuilder) {
            this.builder = intTrieBuilder;
        }

        @Override
        public int getFoldedValue(int i, int i2) {
            int i3 = this.builder.m_initialValue_;
            int i4 = i + 1024;
            while (i < i4) {
                boolean[] zArr = new boolean[1];
                int value = this.builder.getValue(i, zArr);
                if (zArr[0]) {
                    i += 32;
                } else {
                    if (value != i3) {
                        return i2;
                    }
                    i++;
                }
            }
            return 0;
        }
    }
}
