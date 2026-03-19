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

    private boolean areElementsSame(int index1, int[] target, int index2, int length) {
        for (int i = 0; i < length; i++) {
            if (this.v[index1 + i] != target[index2 + i]) {
                return false;
            }
        }
        return true;
    }

    private int findRow(int rangeStart) {
        int index = this.prevRow * this.columns;
        if (rangeStart >= this.v[index]) {
            if (rangeStart < this.v[index + 1]) {
                return index;
            }
            int index2 = index + this.columns;
            if (rangeStart < this.v[index2 + 1]) {
                this.prevRow++;
                return index2;
            }
            int index3 = index2 + this.columns;
            if (rangeStart < this.v[index3 + 1]) {
                this.prevRow += 2;
                return index3;
            }
            if (rangeStart - this.v[index3 + 1] < 10) {
                this.prevRow += 2;
                do {
                    this.prevRow++;
                    index3 += this.columns;
                } while (rangeStart >= this.v[index3 + 1]);
                return index3;
            }
        } else if (rangeStart < this.v[1]) {
            this.prevRow = 0;
            return 0;
        }
        int start = 0;
        int limit = this.rows;
        while (start < limit - 1) {
            int mid = (start + limit) / 2;
            int index4 = this.columns * mid;
            if (rangeStart < this.v[index4]) {
                limit = mid;
            } else {
                if (rangeStart < this.v[index4 + 1]) {
                    this.prevRow = mid;
                    return index4;
                }
                start = mid;
            }
        }
        this.prevRow = start;
        return start * this.columns;
    }

    public PropsVectors(int numOfColumns) {
        if (numOfColumns < 1) {
            throw new IllegalArgumentException("numOfColumns need to be no less than 1; but it is " + numOfColumns);
        }
        this.columns = numOfColumns + 2;
        this.v = new int[this.columns * 4096];
        this.maxRows = 4096;
        this.rows = 3;
        this.prevRow = 0;
        this.isCompacted = false;
        this.v[0] = 0;
        this.v[1] = 1114112;
        int index = this.columns;
        for (int cp = 1114112; cp <= 1114113; cp++) {
            this.v[index] = cp;
            this.v[index + 1] = cp + 1;
            index += this.columns;
        }
    }

    public void setValue(int start, int end, int column, int value, int mask) {
        int newMaxRows;
        if (start < 0 || start > end || end > 1114113 || column < 0 || column >= this.columns - 2) {
            throw new IllegalArgumentException();
        }
        if (this.isCompacted) {
            throw new IllegalStateException("Shouldn't be called aftercompact()!");
        }
        int limit = end + 1;
        int column2 = column + 2;
        int value2 = value & mask;
        int firstRow = findRow(start);
        int lastRow = findRow(end);
        boolean splitFirstRow = (start == this.v[firstRow] || value2 == (this.v[firstRow + column2] & mask)) ? false : true;
        boolean splitLastRow = (limit == this.v[lastRow + 1] || value2 == (this.v[lastRow + column2] & mask)) ? false : true;
        if (splitFirstRow || splitLastRow) {
            int rowsToExpand = 0;
            if (splitFirstRow) {
                rowsToExpand = 1;
            }
            if (splitLastRow) {
                rowsToExpand++;
            }
            if (this.rows + rowsToExpand > this.maxRows) {
                if (this.maxRows < 65536) {
                    newMaxRows = 65536;
                } else if (this.maxRows < 1114114) {
                    newMaxRows = MAX_ROWS;
                } else {
                    throw new IndexOutOfBoundsException("MAX_ROWS exceeded! Increase it to a higher valuein the implementation");
                }
                int[] temp = new int[this.columns * newMaxRows];
                System.arraycopy(this.v, 0, temp, 0, this.rows * this.columns);
                this.v = temp;
                this.maxRows = newMaxRows;
            }
            int count = (this.rows * this.columns) - (this.columns + lastRow);
            if (count > 0) {
                System.arraycopy(this.v, this.columns + lastRow, this.v, ((rowsToExpand + 1) * this.columns) + lastRow, count);
            }
            this.rows += rowsToExpand;
            if (splitFirstRow) {
                System.arraycopy(this.v, firstRow, this.v, this.columns + firstRow, (lastRow - firstRow) + this.columns);
                lastRow += this.columns;
                this.v[this.columns + firstRow] = start;
                this.v[firstRow + 1] = start;
                firstRow += this.columns;
            }
            if (splitLastRow) {
                System.arraycopy(this.v, lastRow, this.v, this.columns + lastRow, this.columns);
                this.v[this.columns + lastRow] = limit;
                this.v[lastRow + 1] = limit;
            }
        }
        this.prevRow = lastRow / this.columns;
        int firstRow2 = firstRow + column2;
        int lastRow2 = lastRow + column2;
        int mask2 = ~mask;
        while (true) {
            this.v[firstRow2] = (this.v[firstRow2] & mask2) | value2;
            if (firstRow2 == lastRow2) {
                return;
            } else {
                firstRow2 += this.columns;
            }
        }
    }

    public int getValue(int c, int column) {
        if (this.isCompacted || c < 0 || c > 1114113 || column < 0 || column >= this.columns - 2) {
            return 0;
        }
        int index = findRow(c);
        return this.v[index + 2 + column];
    }

    public int[] getRow(int rowIndex) {
        if (this.isCompacted) {
            throw new IllegalStateException("Illegal Invocation of the method after compact()");
        }
        if (rowIndex < 0 || rowIndex > this.rows) {
            throw new IllegalArgumentException("rowIndex out of bound!");
        }
        int[] rowToReturn = new int[this.columns - 2];
        System.arraycopy(this.v, (this.columns * rowIndex) + 2, rowToReturn, 0, this.columns - 2);
        return rowToReturn;
    }

    public int getRowStart(int rowIndex) {
        if (this.isCompacted) {
            throw new IllegalStateException("Illegal Invocation of the method after compact()");
        }
        if (rowIndex < 0 || rowIndex > this.rows) {
            throw new IllegalArgumentException("rowIndex out of bound!");
        }
        return this.v[this.columns * rowIndex];
    }

    public int getRowEnd(int rowIndex) {
        if (this.isCompacted) {
            throw new IllegalStateException("Illegal Invocation of the method after compact()");
        }
        if (rowIndex < 0 || rowIndex > this.rows) {
            throw new IllegalArgumentException("rowIndex out of bound!");
        }
        return this.v[(this.columns * rowIndex) + 1] - 1;
    }

    public void compact(CompactHandler compactor) {
        if (this.isCompacted) {
            return;
        }
        this.isCompacted = true;
        int valueColumns = this.columns - 2;
        Integer[] indexArray = new Integer[this.rows];
        for (int i = 0; i < this.rows; i++) {
            indexArray[i] = Integer.valueOf(this.columns * i);
        }
        Arrays.sort(indexArray, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                int indexOfRow1 = o1.intValue();
                int indexOfRow2 = o2.intValue();
                int count = PropsVectors.this.columns;
                int index = 2;
                while (PropsVectors.this.v[indexOfRow1 + index] == PropsVectors.this.v[indexOfRow2 + index]) {
                    index++;
                    if (index == PropsVectors.this.columns) {
                        index = 0;
                    }
                    count--;
                    if (count <= 0) {
                        return 0;
                    }
                }
                return PropsVectors.this.v[indexOfRow1 + index] < PropsVectors.this.v[indexOfRow2 + index] ? -1 : 1;
            }
        });
        int count = -valueColumns;
        for (int i2 = 0; i2 < this.rows; i2++) {
            int start = this.v[indexArray[i2].intValue()];
            if (count < 0 || !areElementsSame(indexArray[i2].intValue() + 2, this.v, indexArray[i2 - 1].intValue() + 2, valueColumns)) {
                count += valueColumns;
            }
            if (start == 1114112) {
                compactor.setRowIndexForInitialValue(count);
            } else if (start == 1114113) {
                compactor.setRowIndexForErrorValue(count);
            }
        }
        int count2 = count + valueColumns;
        compactor.startRealValues(count2);
        int[] temp = new int[count2];
        int count3 = -valueColumns;
        for (int i3 = 0; i3 < this.rows; i3++) {
            int start2 = this.v[indexArray[i3].intValue()];
            int limit = this.v[indexArray[i3].intValue() + 1];
            if (count3 < 0 || !areElementsSame(indexArray[i3].intValue() + 2, temp, count3, valueColumns)) {
                count3 += valueColumns;
                System.arraycopy(this.v, indexArray[i3].intValue() + 2, temp, count3, valueColumns);
            }
            if (start2 < 1114112) {
                compactor.setRowIndexForRange(start2, limit - 1, count3);
            }
        }
        this.v = temp;
        this.rows = (count3 / valueColumns) + 1;
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
        PVecToTrieCompactHandler compactor = new PVecToTrieCompactHandler();
        compact(compactor);
        return compactor.builder.serialize(new DefaultGetFoldedValue(compactor.builder), new DefaultGetFoldingOffset(null));
    }

    private static class DefaultGetFoldingOffset implements Trie.DataManipulate {
        DefaultGetFoldingOffset(DefaultGetFoldingOffset defaultGetFoldingOffset) {
            this();
        }

        private DefaultGetFoldingOffset() {
        }

        @Override
        public int getFoldingOffset(int value) {
            return value;
        }
    }

    private static class DefaultGetFoldedValue implements TrieBuilder.DataManipulate {
        private IntTrieBuilder builder;

        public DefaultGetFoldedValue(IntTrieBuilder inBuilder) {
            this.builder = inBuilder;
        }

        @Override
        public int getFoldedValue(int start, int offset) {
            int initialValue = this.builder.m_initialValue_;
            int limit = start + 1024;
            while (start < limit) {
                boolean[] inBlockZero = new boolean[1];
                int value = this.builder.getValue(start, inBlockZero);
                if (inBlockZero[0]) {
                    start += 32;
                } else {
                    if (value != initialValue) {
                        return offset;
                    }
                    start++;
                }
            }
            return 0;
        }
    }
}
