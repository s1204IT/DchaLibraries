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

    public MergeCursor(Cursor[] cursors) {
        this.mCursors = cursors;
        this.mCursor = cursors[0];
        for (int i = 0; i < this.mCursors.length; i++) {
            if (this.mCursors[i] != null) {
                this.mCursors[i].registerDataSetObserver(this.mObserver);
            }
        }
    }

    @Override
    public int getCount() {
        int count = 0;
        int length = this.mCursors.length;
        for (int i = 0; i < length; i++) {
            if (this.mCursors[i] != null) {
                count += this.mCursors[i].getCount();
            }
        }
        return count;
    }

    @Override
    public boolean onMove(int oldPosition, int newPosition) {
        this.mCursor = null;
        int cursorStartPos = 0;
        int length = this.mCursors.length;
        int i = 0;
        while (true) {
            if (i >= length) {
                break;
            }
            if (this.mCursors[i] != null) {
                if (newPosition < this.mCursors[i].getCount() + cursorStartPos) {
                    this.mCursor = this.mCursors[i];
                    break;
                }
                cursorStartPos += this.mCursors[i].getCount();
            }
            i++;
        }
        if (this.mCursor != null) {
            boolean ret = this.mCursor.moveToPosition(newPosition - cursorStartPos);
            return ret;
        }
        return false;
    }

    @Override
    public String getString(int column) {
        return this.mCursor.getString(column);
    }

    @Override
    public short getShort(int column) {
        return this.mCursor.getShort(column);
    }

    @Override
    public int getInt(int column) {
        return this.mCursor.getInt(column);
    }

    @Override
    public long getLong(int column) {
        return this.mCursor.getLong(column);
    }

    @Override
    public float getFloat(int column) {
        return this.mCursor.getFloat(column);
    }

    @Override
    public double getDouble(int column) {
        return this.mCursor.getDouble(column);
    }

    @Override
    public int getType(int column) {
        return this.mCursor.getType(column);
    }

    @Override
    public boolean isNull(int column) {
        return this.mCursor.isNull(column);
    }

    @Override
    public byte[] getBlob(int column) {
        return this.mCursor.getBlob(column);
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
    public void registerContentObserver(ContentObserver observer) {
        int length = this.mCursors.length;
        for (int i = 0; i < length; i++) {
            if (this.mCursors[i] != null) {
                this.mCursors[i].registerContentObserver(observer);
            }
        }
    }

    @Override
    public void unregisterContentObserver(ContentObserver observer) {
        int length = this.mCursors.length;
        for (int i = 0; i < length; i++) {
            if (this.mCursors[i] != null) {
                this.mCursors[i].unregisterContentObserver(observer);
            }
        }
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        int length = this.mCursors.length;
        for (int i = 0; i < length; i++) {
            if (this.mCursors[i] != null) {
                this.mCursors[i].registerDataSetObserver(observer);
            }
        }
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        int length = this.mCursors.length;
        for (int i = 0; i < length; i++) {
            if (this.mCursors[i] != null) {
                this.mCursors[i].unregisterDataSetObserver(observer);
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
