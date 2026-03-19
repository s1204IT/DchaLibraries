package android.database;

import java.util.Iterator;

public final class CursorJoiner implements Iterator<Result>, Iterable<Result> {

    private static final int[] f3androiddatabaseCursorJoiner$ResultSwitchesValues = null;

    static final boolean f4assertionsDisabled;
    private int[] mColumnsLeft;
    private int[] mColumnsRight;
    private Result mCompareResult;
    private boolean mCompareResultIsValid;
    private Cursor mCursorLeft;
    private Cursor mCursorRight;
    private String[] mValues;

    private static int[] m536getandroiddatabaseCursorJoiner$ResultSwitchesValues() {
        if (f3androiddatabaseCursorJoiner$ResultSwitchesValues != null) {
            return f3androiddatabaseCursorJoiner$ResultSwitchesValues;
        }
        int[] iArr = new int[Result.valuesCustom().length];
        try {
            iArr[Result.BOTH.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[Result.LEFT.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[Result.RIGHT.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        f3androiddatabaseCursorJoiner$ResultSwitchesValues = iArr;
        return iArr;
    }

    static {
        f4assertionsDisabled = !CursorJoiner.class.desiredAssertionStatus();
    }

    public enum Result {
        RIGHT,
        LEFT,
        BOTH;

        public static Result[] valuesCustom() {
            return values();
        }
    }

    public CursorJoiner(Cursor cursorLeft, String[] columnNamesLeft, Cursor cursorRight, String[] columnNamesRight) {
        if (columnNamesLeft.length != columnNamesRight.length) {
            throw new IllegalArgumentException("you must have the same number of columns on the left and right, " + columnNamesLeft.length + " != " + columnNamesRight.length);
        }
        this.mCursorLeft = cursorLeft;
        this.mCursorRight = cursorRight;
        this.mCursorLeft.moveToFirst();
        this.mCursorRight.moveToFirst();
        this.mCompareResultIsValid = false;
        this.mColumnsLeft = buildColumnIndiciesArray(cursorLeft, columnNamesLeft);
        this.mColumnsRight = buildColumnIndiciesArray(cursorRight, columnNamesRight);
        this.mValues = new String[this.mColumnsLeft.length * 2];
    }

    @Override
    public Iterator<Result> iterator() {
        return this;
    }

    private int[] buildColumnIndiciesArray(Cursor cursor, String[] columnNames) {
        int[] columns = new int[columnNames.length];
        for (int i = 0; i < columnNames.length; i++) {
            columns[i] = cursor.getColumnIndexOrThrow(columnNames[i]);
        }
        return columns;
    }

    @Override
    public boolean hasNext() {
        if (!this.mCompareResultIsValid) {
            return (this.mCursorLeft.isAfterLast() && this.mCursorRight.isAfterLast()) ? false : true;
        }
        switch (m536getandroiddatabaseCursorJoiner$ResultSwitchesValues()[this.mCompareResult.ordinal()]) {
            case 1:
                return (this.mCursorLeft.isLast() && this.mCursorRight.isLast()) ? false : true;
            case 2:
                return (this.mCursorLeft.isLast() && this.mCursorRight.isAfterLast()) ? false : true;
            case 3:
                return (this.mCursorLeft.isAfterLast() && this.mCursorRight.isLast()) ? false : true;
            default:
                throw new IllegalStateException("bad value for mCompareResult, " + this.mCompareResult);
        }
    }

    @Override
    public Result next() {
        if (!hasNext()) {
            throw new IllegalStateException("you must only call next() when hasNext() is true");
        }
        incrementCursors();
        if (!f4assertionsDisabled && !hasNext()) {
            throw new AssertionError();
        }
        boolean hasLeft = !this.mCursorLeft.isAfterLast();
        boolean hasRight = !this.mCursorRight.isAfterLast();
        if (hasLeft && hasRight) {
            populateValues(this.mValues, this.mCursorLeft, this.mColumnsLeft, 0);
            populateValues(this.mValues, this.mCursorRight, this.mColumnsRight, 1);
            switch (compareStrings(this.mValues)) {
                case -1:
                    this.mCompareResult = Result.LEFT;
                    break;
                case 0:
                    this.mCompareResult = Result.BOTH;
                    break;
                case 1:
                    this.mCompareResult = Result.RIGHT;
                    break;
            }
        } else if (hasLeft) {
            this.mCompareResult = Result.LEFT;
        } else {
            if (!f4assertionsDisabled && !hasRight) {
                throw new AssertionError();
            }
            this.mCompareResult = Result.RIGHT;
        }
        this.mCompareResultIsValid = true;
        return this.mCompareResult;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("not implemented");
    }

    private static void populateValues(String[] values, Cursor cursor, int[] columnIndicies, int startingIndex) {
        boolean z = true;
        if (!f4assertionsDisabled) {
            if (startingIndex != 0 && startingIndex != 1) {
                z = false;
            }
            if (!z) {
                throw new AssertionError();
            }
        }
        for (int i = 0; i < columnIndicies.length; i++) {
            values[(i * 2) + startingIndex] = cursor.getString(columnIndicies[i]);
        }
    }

    private void incrementCursors() {
        if (!this.mCompareResultIsValid) {
            return;
        }
        switch (m536getandroiddatabaseCursorJoiner$ResultSwitchesValues()[this.mCompareResult.ordinal()]) {
            case 1:
                this.mCursorLeft.moveToNext();
                this.mCursorRight.moveToNext();
                break;
            case 2:
                this.mCursorLeft.moveToNext();
                break;
            case 3:
                this.mCursorRight.moveToNext();
                break;
        }
        this.mCompareResultIsValid = false;
    }

    private static int compareStrings(String... values) {
        if (values.length % 2 != 0) {
            throw new IllegalArgumentException("you must specify an even number of values");
        }
        for (int index = 0; index < values.length; index += 2) {
            if (values[index] == null) {
                if (values[index + 1] != null) {
                    return -1;
                }
            } else {
                if (values[index + 1] == null) {
                    return 1;
                }
                int comp = values[index].compareTo(values[index + 1]);
                if (comp != 0) {
                    return comp < 0 ? -1 : 1;
                }
            }
        }
        return 0;
    }
}
