package android.database;

public class MergeCursor extends AbstractCursor {
    private Cursor mCursor;
    private Cursor[] mCursors;
    private DataSetObserver mObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            MergeCursor.this.mPos = -1;
        }

        @Override
        public void onInvalidated() {
            MergeCursor.this.mPos = -1;
        }
    };

    public MergeCursor(Cursor[] cursorArr) {
        this.mCursors = cursorArr;
        this.mCursor = cursorArr[0];
        for (int i = 0; i < this.mCursors.length; i++) {
            if (this.mCursors[i] != null) {
                this.mCursors[i].registerDataSetObserver(this.mObserver);
            }
        }
    }

    @Override
    public int getCount() {
        int length = this.mCursors.length;
        int count = 0;
        for (int i = 0; i < length; i++) {
            if (this.mCursors[i] != null) {
                count += this.mCursors[i].getCount();
            }
        }
        return count;
    }

    @Override
    public boolean onMove(int i, int i2) {
        this.mCursor = null;
        int length = this.mCursors.length;
        int i3 = 0;
        int count = 0;
        while (true) {
            if (i3 >= length) {
                break;
            }
            if (this.mCursors[i3] != null) {
                if (i2 < this.mCursors[i3].getCount() + count) {
                    this.mCursor = this.mCursors[i3];
                    break;
                }
                count += this.mCursors[i3].getCount();
            }
            i3++;
        }
        if (this.mCursor == null) {
            return false;
        }
        return this.mCursor.moveToPosition(i2 - count);
    }

    @Override
    public String getString(int i) {
        return this.mCursor.getString(i);
    }

    @Override
    public short getShort(int i) {
        return this.mCursor.getShort(i);
    }

    @Override
    public int getInt(int i) {
        return this.mCursor.getInt(i);
    }

    @Override
    public long getLong(int i) {
        return this.mCursor.getLong(i);
    }

    @Override
    public float getFloat(int i) {
        return this.mCursor.getFloat(i);
    }

    @Override
    public double getDouble(int i) {
        return this.mCursor.getDouble(i);
    }

    @Override
    public int getType(int i) {
        return this.mCursor.getType(i);
    }

    @Override
    public boolean isNull(int i) {
        return this.mCursor.isNull(i);
    }

    @Override
    public byte[] getBlob(int i) {
        return this.mCursor.getBlob(i);
    }

    @Override
    public String[] getColumnNames() {
        if (this.mCursor != null) {
            return this.mCursor.getColumnNames();
        }
        return new String[0];
    }

    @Override
    public void deactivate() {
        int length = this.mCursors.length;
        for (int i = 0; i < length; i++) {
            if (this.mCursors[i] != null) {
                this.mCursors[i].deactivate();
            }
        }
        super.deactivate();
    }

    @Override
    public void close() {
        int length = this.mCursors.length;
        for (int i = 0; i < length; i++) {
            if (this.mCursors[i] != null) {
                this.mCursors[i].close();
            }
        }
        super.close();
    }

    @Override
    public void registerContentObserver(ContentObserver contentObserver) {
        int length = this.mCursors.length;
        for (int i = 0; i < length; i++) {
            if (this.mCursors[i] != null) {
                this.mCursors[i].registerContentObserver(contentObserver);
            }
        }
    }

    @Override
    public void unregisterContentObserver(ContentObserver contentObserver) {
        int length = this.mCursors.length;
        for (int i = 0; i < length; i++) {
            if (this.mCursors[i] != null) {
                this.mCursors[i].unregisterContentObserver(contentObserver);
            }
        }
    }

    @Override
    public void registerDataSetObserver(DataSetObserver dataSetObserver) {
        int length = this.mCursors.length;
        for (int i = 0; i < length; i++) {
            if (this.mCursors[i] != null) {
                this.mCursors[i].registerDataSetObserver(dataSetObserver);
            }
        }
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver dataSetObserver) {
        int length = this.mCursors.length;
        for (int i = 0; i < length; i++) {
            if (this.mCursors[i] != null) {
                this.mCursors[i].unregisterDataSetObserver(dataSetObserver);
            }
        }
    }

    @Override
    public boolean requery() {
        int length = this.mCursors.length;
        for (int i = 0; i < length; i++) {
            if (this.mCursors[i] != null && !this.mCursors[i].requery()) {
                return false;
            }
        }
        return true;
    }
}
