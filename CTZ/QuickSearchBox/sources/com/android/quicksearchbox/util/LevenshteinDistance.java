package com.android.quicksearchbox.util;

import java.lang.reflect.Array;

public class LevenshteinDistance {
    private final int[][] mDistanceTable;
    private final int[][] mEditTypeTable;
    private final Token[] mSource;
    private final Token[] mTarget;

    public LevenshteinDistance(Token[] tokenArr, Token[] tokenArr2) {
        int length = tokenArr.length;
        int length2 = tokenArr2.length;
        int i = length + 1;
        int i2 = length2 + 1;
        int[][] iArr = (int[][]) Array.newInstance((Class<?>) int.class, i, i2);
        int[][] iArr2 = (int[][]) Array.newInstance((Class<?>) int.class, i, i2);
        iArr[0][0] = 3;
        iArr2[0][0] = 0;
        for (int i3 = 1; i3 <= length; i3++) {
            iArr[i3][0] = 0;
            iArr2[i3][0] = i3;
        }
        for (int i4 = 1; i4 <= length2; i4++) {
            iArr[0][i4] = 1;
            iArr2[0][i4] = i4;
        }
        this.mEditTypeTable = iArr;
        this.mDistanceTable = iArr2;
        this.mSource = tokenArr;
        this.mTarget = tokenArr2;
    }

    public int calculate() {
        Token[] tokenArr = this.mSource;
        Token[] tokenArr2 = this.mTarget;
        int length = tokenArr.length;
        int length2 = tokenArr2.length;
        int[][] iArr = this.mDistanceTable;
        int[][] iArr2 = this.mEditTypeTable;
        for (int i = 1; i <= length; i++) {
            int i2 = i - 1;
            Token token = tokenArr[i2];
            for (int i3 = 1; i3 <= length2; i3++) {
                int i4 = i3 - 1;
                int i5 = !token.prefixOf(tokenArr2[i4]) ? 1 : 0;
                int i6 = iArr[i2][i3] + 1;
                int i7 = 0;
                int i8 = iArr[i][i4] + 1;
                if (i8 < i6) {
                    i7 = 1;
                    i6 = i8;
                }
                int i9 = iArr[i2][i4] + i5;
                if (i9 < i6) {
                    i7 = i5 == 0 ? 3 : 2;
                    i6 = i9;
                }
                iArr[i][i3] = i6;
                iArr2[i][i3] = i7;
            }
        }
        return iArr[length][length2];
    }

    public EditOperation[] getTargetOperations() {
        int length = this.mTarget.length;
        EditOperation[] editOperationArr = new EditOperation[length];
        int length2 = this.mSource.length;
        int[][] iArr = this.mEditTypeTable;
        while (length > 0) {
            int i = iArr[length2][length];
            switch (i) {
                case 0:
                    length2--;
                    break;
                case 1:
                    length--;
                    editOperationArr[length] = new EditOperation(i, length2);
                    break;
                case 2:
                case 3:
                    length--;
                    length2--;
                    editOperationArr[length] = new EditOperation(i, length2);
                    break;
            }
        }
        return editOperationArr;
    }

    public static final class EditOperation {
        private final int mPosition;
        private final int mType;

        public EditOperation(int i, int i2) {
            this.mType = i;
            this.mPosition = i2;
        }

        public int getType() {
            return this.mType;
        }

        public int getPosition() {
            return this.mPosition;
        }
    }

    public static final class Token implements CharSequence {
        private final char[] mContainer;
        public final int mEnd;
        public final int mStart;

        public Token(char[] cArr, int i, int i2) {
            this.mContainer = cArr;
            this.mStart = i;
            this.mEnd = i2;
        }

        @Override
        public int length() {
            return this.mEnd - this.mStart;
        }

        @Override
        public String toString() {
            return subSequence(0, length());
        }

        public boolean prefixOf(Token token) {
            int length = length();
            if (length > token.length()) {
                return false;
            }
            int i = this.mStart;
            int i2 = token.mStart;
            char[] cArr = this.mContainer;
            char[] cArr2 = token.mContainer;
            for (int i3 = 0; i3 < length; i3++) {
                if (cArr[i + i3] != cArr2[i2 + i3]) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public char charAt(int i) {
            return this.mContainer[i + this.mStart];
        }

        @Override
        public String subSequence(int i, int i2) {
            return new String(this.mContainer, this.mStart + i, length());
        }
    }
}
