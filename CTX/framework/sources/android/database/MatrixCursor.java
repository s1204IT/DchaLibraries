package android.database;

import java.util.ArrayList;

public class MatrixCursor extends AbstractCursor {
    private final int columnCount;
    private final String[] columnNames;
    private Object[] data;
    private int rowCount;

    public MatrixCursor(String[] strArr, int i) {
        this.rowCount = 0;
        this.columnNames = strArr;
        this.columnCount = strArr.length;
        this.data = new Object[this.columnCount * (i >= 1 ? i : 1)];
    }

    public MatrixCursor(String[] strArr) {
        this(strArr, 16);
    }

    private Object get(int i) {
        if (i < 0 || i >= this.columnCount) {
            throw new CursorIndexOutOfBoundsException("Requested column: " + i + ", # of columns: " + this.columnCount);
        }
        if (this.mPos < 0) {
            throw new CursorIndexOutOfBoundsException("Before first row.");
        }
        if (this.mPos >= this.rowCount) {
            throw new CursorIndexOutOfBoundsException("After last row.");
        }
        return this.data[(this.mPos * this.columnCount) + i];
    }

    public RowBuilder newRow() {
        int i = this.rowCount;
        this.rowCount = i + 1;
        ensureCapacity(this.rowCount * this.columnCount);
        return new RowBuilder(i);
    }

    public void addRow(Object[] objArr) {
        if (objArr.length != this.columnCount) {
            throw new IllegalArgumentException("columnNames.length = " + this.columnCount + ", columnValues.length = " + objArr.length);
        }
        int i = this.rowCount;
        this.rowCount = i + 1;
        int i2 = i * this.columnCount;
        ensureCapacity(this.columnCount + i2);
        System.arraycopy(objArr, 0, this.data, i2, this.columnCount);
    }

    public void addRow(Iterable<?> iterable) {
        int i = this.rowCount * this.columnCount;
        int i2 = this.columnCount + i;
        ensureCapacity(i2);
        if (iterable instanceof ArrayList) {
            addRow((ArrayList) iterable, i);
            return;
        }
        Object[] objArr = this.data;
        for (Object obj : iterable) {
            if (i == i2) {
                throw new IllegalArgumentException("columnValues.size() > columnNames.length");
            }
            objArr[i] = obj;
            i++;
        }
        if (i != i2) {
            throw new IllegalArgumentException("columnValues.size() < columnNames.length");
        }
        this.rowCount++;
    }

    private void addRow(ArrayList<?> arrayList, int i) {
        int size = arrayList.size();
        if (size != this.columnCount) {
            throw new IllegalArgumentException("columnNames.length = " + this.columnCount + ", columnValues.size() = " + size);
        }
        this.rowCount++;
        Object[] objArr = this.data;
        for (int i2 = 0; i2 < size; i2++) {
            objArr[i + i2] = arrayList.get(i2);
        }
    }

    private void ensureCapacity(int i) {
        if (i > this.data.length) {
            Object[] objArr = this.data;
            int length = this.data.length * 2;
            if (length >= i) {
                i = length;
            }
            this.data = new Object[i];
            System.arraycopy(objArr, 0, this.data, 0, objArr.length);
        }
    }

    public class RowBuilder {
        private final int endIndex;
        private int index;
        private final int row;

        RowBuilder(int i) {
            this.row = i;
            this.index = i * MatrixCursor.this.columnCount;
            this.endIndex = this.index + MatrixCursor.this.columnCount;
        }

        public RowBuilder add(Object obj) {
            if (this.index != this.endIndex) {
                Object[] objArr = MatrixCursor.this.data;
                int i = this.index;
                this.index = i + 1;
                objArr[i] = obj;
                return this;
            }
            throw new CursorIndexOutOfBoundsException("No more columns left.");
        }

        public RowBuilder add(String str, Object obj) {
            for (int i = 0; i < MatrixCursor.this.columnNames.length; i++) {
                if (str.equals(MatrixCursor.this.columnNames[i])) {
                    MatrixCursor.this.data[(this.row * MatrixCursor.this.columnCount) + i] = obj;
                }
            }
            return this;
        }
    }

    @Override
    public int getCount() {
        return this.rowCount;
    }

    @Override
    public String[] getColumnNames() {
        return this.columnNames;
    }

    @Override
    public String getString(int i) {
        Object obj = get(i);
        if (obj == null) {
            return null;
        }
        return obj.toString();
    }

    @Override
    public short getShort(int i) {
        Object obj = get(i);
        if (obj == null) {
            return (short) 0;
        }
        return obj instanceof Number ? ((Number) obj).shortValue() : Short.parseShort(obj.toString());
    }

    @Override
    public int getInt(int i) {
        Object obj = get(i);
        if (obj == null) {
            return 0;
        }
        return obj instanceof Number ? ((Number) obj).intValue() : Integer.parseInt(obj.toString());
    }

    @Override
    public long getLong(int i) {
        Object obj = get(i);
        if (obj == null) {
            return 0L;
        }
        return obj instanceof Number ? ((Number) obj).longValue() : Long.parseLong(obj.toString());
    }

    @Override
    public float getFloat(int i) {
        Object obj = get(i);
        if (obj == null) {
            return 0.0f;
        }
        return obj instanceof Number ? ((Number) obj).floatValue() : Float.parseFloat(obj.toString());
    }

    @Override
    public double getDouble(int i) {
        Object obj = get(i);
        if (obj == null) {
            return 0.0d;
        }
        return obj instanceof Number ? ((Number) obj).doubleValue() : Double.parseDouble(obj.toString());
    }

    @Override
    public byte[] getBlob(int i) {
        return (byte[]) get(i);
    }

    @Override
    public int getType(int i) {
        return DatabaseUtils.getTypeOfObject(get(i));
    }

    @Override
    public boolean isNull(int i) {
        return get(i) == null;
    }
}
