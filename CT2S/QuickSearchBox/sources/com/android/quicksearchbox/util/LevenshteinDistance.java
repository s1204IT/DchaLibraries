package com.android.quicksearchbox.util;

import java.lang.reflect.Array;

public class LevenshteinDistance {
    private final int[][] mDistanceTable;
    private final int[][] mEditTypeTable;
    private final Token[] mSource;
    private final Token[] mTarget;

    public LevenshteinDistance(Token[] source, Token[] target) {
        int sourceSize = source.length;
        int targetSize = target.length;
        int[][] editTab = (int[][]) Array.newInstance((Class<?>) Integer.TYPE, sourceSize + 1, targetSize + 1);
        int[][] distTab = (int[][]) Array.newInstance((Class<?>) Integer.TYPE, sourceSize + 1, targetSize + 1);
        editTab[0][0] = 3;
        distTab[0][0] = 0;
        for (int i = 1; i <= sourceSize; i++) {
            editTab[i][0] = 0;
            distTab[i][0] = i;
        }
        for (int i2 = 1; i2 <= targetSize; i2++) {
            editTab[0][i2] = 1;
            distTab[0][i2] = i2;
        }
        this.mEditTypeTable = editTab;
        this.mDistanceTable = distTab;
        this.mSource = source;
        this.mTarget = target;
    }

    public int calculate() {
        Token[] src = this.mSource;
        Token[] trg = this.mTarget;
        int sourceLen = src.length;
        int targetLen = trg.length;
        int[][] distTab = this.mDistanceTable;
        int[][] editTab = this.mEditTypeTable;
        for (int s = 1; s <= sourceLen; s++) {
            Token sourceToken = src[s - 1];
            for (int t = 1; t <= targetLen; t++) {
                Token targetToken = trg[t - 1];
                int cost = sourceToken.prefixOf(targetToken) ? 0 : 1;
                int distance = distTab[s - 1][t] + 1;
                int type = 0;
                int d = distTab[s][t - 1];
                if (d + 1 < distance) {
                    distance = d + 1;
                    type = 1;
                }
                int d2 = distTab[s - 1][t - 1];
                if (d2 + cost < distance) {
                    distance = d2 + cost;
                    type = cost == 0 ? 3 : 2;
                }
                distTab[s][t] = distance;
                editTab[s][t] = type;
            }
        }
        return distTab[sourceLen][targetLen];
    }

    public EditOperation[] getTargetOperations() {
        int trgLen = this.mTarget.length;
        EditOperation[] ops = new EditOperation[trgLen];
        int targetPos = trgLen;
        int sourcePos = this.mSource.length;
        int[][] editTab = this.mEditTypeTable;
        while (targetPos > 0) {
            int editType = editTab[sourcePos][targetPos];
            switch (editType) {
                case 0:
                    sourcePos--;
                    break;
                case 1:
                    targetPos--;
                    ops[targetPos] = new EditOperation(editType, sourcePos);
                    break;
                case 2:
                case 3:
                    targetPos--;
                    sourcePos--;
                    ops[targetPos] = new EditOperation(editType, sourcePos);
                    break;
            }
        }
        return ops;
    }

    public static final class EditOperation {
        private final int mPosition;
        private final int mType;

        public EditOperation(int type, int position) {
            this.mType = type;
            this.mPosition = position;
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

        public Token(char[] container, int start, int end) {
            this.mContainer = container;
            this.mStart = start;
            this.mEnd = end;
        }

        @Override
        public int length() {
            return this.mEnd - this.mStart;
        }

        @Override
        public String toString() {
            return subSequence(0, length());
        }

        public boolean prefixOf(Token that) {
            int len = length();
            if (len > that.length()) {
                return false;
            }
            int thisStart = this.mStart;
            int thatStart = that.mStart;
            char[] thisContainer = this.mContainer;
            char[] thatContainer = that.mContainer;
            for (int i = 0; i < len; i++) {
                if (thisContainer[thisStart + i] != thatContainer[thatStart + i]) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public char charAt(int index) {
            return this.mContainer[this.mStart + index];
        }

        @Override
        public String subSequence(int start, int end) {
            return new String(this.mContainer, this.mStart + start, length());
        }
    }
}
